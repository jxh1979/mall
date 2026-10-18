package com.complex.platform.ticketing;

// 文件版本：V0.03

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.amusement.AmusementEntryRequest;
import com.complex.platform.amusement.AmusementService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotentReplayContext;
import com.complex.platform.terminal.MdmService;
import com.complex.platform.ticketing.dto.TerminalSyncTaskItemRequest;
import com.complex.platform.ticketing.dto.TerminalSyncTasksRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

@Service
public class TicketVerificationService {

    // R5 T15：赠饮权益类型——核销成功后触发 ticket.gift_verified，由 bar 域生成 0 元单并扣库存。
    private static final String GIFT_DRINK_ENTITLEMENT_TYPE = "gift_drink";

    // 阶段1：游乐类权益前缀（与 amusement 域 AMUSEMENT_ENTITLEMENT_TYPE_PREFIX 同口径；
    // entitlement_type 以 "amusement" 开头视为游乐类，分流进游乐正路）。
    private static final String AMUSEMENT_ENTITLEMENT_TYPE_PREFIX = "amusement";

    // #172(2026-07-19 拍板)：动态码刷新间隔改为全场统一配置（ticket_qr_refresh_config），发凭证时读它决定刷新窗口，
    // 不再按票种各配（DEFAULT/票种级取值已下线）；缺行时仓库读回退 15。

    // 跨模块闭环②a：核销高风险动作类型（MDM 门控 actionType）。
    private static final String VERIFY_ACTION_TYPE = "ticket.verify";

    private final TicketingRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    // 跨模块闭环②a：MDM 高风险门控（terminal 模块 public 服务）。依赖方向 ticketing→terminal 无环
    // （terminal 不引用 ticketing；finance 已先有 terminal 引用为证）。可为 null=门控未接（旧构造/兼容路径）。
    private final MdmService mdmService;
    // P0 越权修复：员工侧凭证刷新/核销冲正需按调用者商户范围校验权益归属。可为 null=旧构造/兼容路径（不强制，
    // 仅在 Spring 主构造注入后生效；平台管理员 scope 为空=放行，商户用户必须匹配权益 merchant_id）。
    private final AuthorizationService authorizationService;
    // P0-3/X-01 门票二维码 HMAC 签名器：签发时对凭证关键字段签名、核销时校验。可为 null=旧构造/兼容测试
    // （不签名不校验，沿用 signature_value/signature_valid 占位 NULL 语义）；生产由 Spring 注入 @Component 后一律签名+校验。
    private final TicketCredentialSigner credentialSigner;
    // 阶段1（2026-07-05 用户拍板）游乐核销分流：检出游乐类权益 → 直调 AmusementService 走游乐正路
    // （定位项目/当前场次 + 三维容量原子扣减 + 写 amusement_verifications，并在同事务内消费本权益）。
    // 原为单实现、单消费者的分流端口，2026-08-02 收编为 @Lazy 直注，语义不变。
    // 生产由 Spring 注入；游乐必须定位实际项目，无服务或无项目时明确拒绝。
    // @Lazy 破除潜在初始化环（ticketing→amusement→TicketingAmusementEntitlementGateway→TicketingRepository 本无环，
    // @Lazy 仅为稳妥）。
    private final AmusementService amusementService;
    @org.springframework.beans.factory.annotation.Autowired
    TicketAmusementAdmissionService amusementAdmissions;
    // S5：赠送权益内部结算价的唯一解析口径（预扣与兑付同源）。由 repository 直接构造——它无状态、无外部依赖，
    // 各兼容构造点无须逐个补参，且永不为 null（免掉一层空判分支）。
    private final InternalSettlementPriceResolver priceResolver;
    // CG-08b（ENUM「全园故障与补偿通票」）：补偿补发权益核销成功 → 履约锁定到补偿记录（用一次即用尽）。
    // 字段注入免改构造点；旧构造/未注入的单测为 null（不回写）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    CompensationFulfillmentPort compensationFulfillmentPort;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    ParkWideFaultSalesGate parkWideFaultSalesGate;

    // CG-05 核销记录状态闭集（ENUM「核销记录状态」：成功、拒绝（规则不允许）、失败（设备或网络原因）、已冲正、异常）。
    // 落记录的非成功结果按 result_reason 归类：规则不允许 → rejected；设备 / 网络 / 凭证技术原因 → failed。
    // 「异常」（anomaly）的进入条件待 HTML 补句后另开 PR：本类不写 anomaly。manual_overridden 历史保留、不再写入。
    static final String STATUS_SUCCESS = "success";
    static final String STATUS_REJECTED = "rejected";
    static final String STATUS_FAILED = "failed";
    static final String STATUS_ANOMALY = "anomaly";
    /** 规则不允许（票不可用、次数用尽、场次/时段不符、归属不符、项目/场次状态、安全阻断等业务规则）→ rejected。 */
    static final Set<String> RULE_REJECTION_CODES = Set.of(
            "ENTITLEMENT_NOT_ACTIVE", "ENTITLEMENT_NOT_YET_VALID", "ENTITLEMENT_EXPIRED", "ENTITLEMENT_USED_UP", "ENTITLEMENT_USED",
            "TICKET_INSTANCE_REFUND_IN_PROGRESS", "TICKET_TRANSFER_PENDING", "MULTI_PERSON_CROSS_SESSION_FORBIDDEN",
            "ENTITLEMENT_PROVIDER_MISMATCH", "ENTITLEMENT_NOT_AMUSEMENT", "ENTITLEMENT_NOT_FOR_PROJECT",
            "AMUSEMENT_ONLINE_REQUIRED", "AMUSEMENT_PROJECT_UNRESOLVED", "AMUSEMENT_BUSINESS_DAY_REQUIRED",
            "PARK_WIDE_FAULT_VERIFICATION_SUSPENDED", "PROJECT_NOT_OPEN", "SAFETY_BLOCKED", "SESSION_NOT_OPEN", "SESSION_CAPACITY_EXHAUSTED", "SESSION_PROJECT_MISMATCH");
    /** 设备、网络、凭证技术原因（签名无效、窗口过期、凭证不存在/已用、设备未登记/未纳管、归属无法解析）→ failed。 */
    static final Set<String> TECHNICAL_FAILURE_CODES = Set.of(
            "CREDENTIAL_NOT_FOUND", "CREDENTIAL_EXPIRED", "ENTITLEMENT_NOT_FOUND", "DEVICE_NOT_REGISTERED", "DEVICE_NOT_MANAGED",
            "DEVICE_BLOCKED", "DEVICE_NOT_FOUND", "VERIFY_DEVICE_MERCHANT_UNRESOLVED", "ENTITLEMENT_PROVIDER_UNRESOLVED",
            "PROJECT_NOT_FOUND", "TICKET_NOT_FOUND");

    /** 错误码 → 记录状态；未归类（如越权、参数错误）返回 null，调用方维持原异常（PR 待裁决单列）。 */
    static String outcomeStatusForCode(String code) {
        if (code == null) {
            return null;
        }
        if (RULE_REJECTION_CODES.contains(code)) {
            return STATUS_REJECTED;
        }
        if (TECHNICAL_FAILURE_CODES.contains(code)) {
            return STATUS_FAILED;
        }
        return null;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TicketVerificationService(
            TicketingRepository repository,
            ApplicationEventPublisher eventPublisher,
            MdmService mdmService,
            AuthorizationService authorizationService,
            TicketCredentialSigner credentialSigner,
            @org.springframework.context.annotation.Lazy AmusementService amusementService) {
        this.priceResolver = new InternalSettlementPriceResolver(repository);
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.mdmService = mdmService;
        this.authorizationService = authorizationService;
        this.credentialSigner = credentialSigner;
        this.amusementService = amusementService;
    }

    /**
     * P0 越权修复统一范式：取出记录后按调用者商户范围校验归属。
     * 平台管理员 scope 为空 → 放行（可见全部）；商户用户 → 必须等于记录 merchant_id，否则 CROSS_MERCHANT_FORBIDDEN。
     * authorizationService 为 null（旧构造/兼容测试）时不强制。
     */
    private void enforceMerchantScope(String merchantId) {
        if (authorizationService == null) {
            return;
        }
        authorizationService.currentMerchantScope().ifPresent(scope -> {
            if (!scope.equals(merchantId)) {
                throw ApiException.forbidden(
                        "CROSS_MERCHANT_FORBIDDEN", "无权操作其他商户的权益");
            }
        });
    }

    /**
     * 健壮性修复：核销落库前校验 device_id 已注册（terminal_devices.id）。未注册设备写 ticket_verifications
     * 会违反 FK fk_ticket_verifications_device 使事务中止，连带失败留痕(audit)也在已中止事务里级联失败，
     * 返回脏 500 掩盖原始业务错。此处先抛干净业务错 DEVICE_NOT_REGISTERED(400)，避免 FK 违反污染事务。
     */
    private void requireRegisteredDevice(VerificationRequest request) {
        String deviceId = requireText(request == null ? null : request.deviceId(), "device_id");
        if (!repository.terminalDeviceExists(deviceId)) {
            throw ApiException.badRequest(
                    "DEVICE_NOT_REGISTERED", "设备未登记：" + deviceId);
        }
    }

    TicketCredentialResponse refreshCredential(String ticketId, String customerId) {
        IssuedEntitlementRecord entitlement = repository.findIssuedEntitlementById(requireText(ticketId, "ticket_id"))
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "权益不存在或已被删除"));
        if (customerId != null) {
            // AUD-077 取码授权按"当前持有人"(slot holder),非订单归属:转出后原持票人取码 403、受赠人可取。
            // 无 slot holder(存量/直发未分组)回退订单归属校验;新发放 holder 初始=购票人,故未转赠票行为不变。
            // 复核修正(AUD-012):共享锁读持有人,取码(读 holder→插新凭证)与 claim(改 holder+撤销凭证)串行化,
            // 否则 claim 的撤销可能扫不到并发在飞的新凭证=转出后原持有人留下一个有效动态码。
            String holder = entitlement.parentTicketId() == null ? null
                    : repository.findSlotHolderForShare(entitlement.parentTicketId(), entitlement.slotIndex())
                            .map(TicketSlotHolderRecord::holderCustomerId).orElse(null);
            if (holder != null) {
                if (!customerId.equals(holder)) {
                    throw ApiException.forbidden("CUSTOMER_ORDER_FORBIDDEN", "该票不属于当前客人");
                }
            } else {
                TicketOrderRecord order = repository.findTicketOrderById(entitlement.orderId())
                        .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "票务订单不存在或已被删除"));
                if (!customerId.equals(order.customerId())) {
                    throw ApiException.forbidden("CUSTOMER_ORDER_FORBIDDEN", "该票不属于当前客人");
                }
            }
        } else {
            // P0 越权修复：员工侧（customerId==null）此前完全跳过归属校验，可对任意权益 id 签发动态核销凭证（跨租户 IDOR）。
            // 改为按调用者商户范围校验权益归属。
            enforceMerchantScope(entitlement.merchantId());
        }
        Instant now = Instant.now();
        String token = "cred_" + UUID.randomUUID().toString().replace("-", "");
        String nonce = UUID.randomUUID().toString().replace("-", "");
        // #172(2026-07-19 拍板)：刷新窗口读全场统一配置（ticket_qr_refresh_config），不再按票种各配一套；
        // 取值以 5 秒为单位、最长 60 秒、默认 15 秒。window_started_at=now，
        // 截图离线展示的旧 token 超过本窗口即过期被拒。
        int refreshIntervalSeconds = repository.findQrRefreshIntervalSeconds();
        Instant expiresAt = now.plusSeconds(refreshIntervalSeconds);
        // P0-3/X-01 二维码防伪签名：对凭证关键字段（token/权益/nonce/窗口锚点/有效期）计算 HMAC-SHA256，
        // 写入 signature_value/signature_version。signer 未注入的兼容测试路径下为 null（沿用占位 NULL 语义）。
        String signatureValue = credentialSigner == null
                ? null
                : credentialSigner.sign(credentialSigner.canonical(token, entitlement.id(), nonce, now, expiresAt));
        String signatureVersion = credentialSigner == null ? null : credentialSigner.version();
        TicketCredentialRecord saved = repository.saveTicketCredential(new TicketCredentialRecord(
                "credential_" + UUID.randomUUID().toString().replace("-", ""),
                entitlement.id(),
                entitlement.orderId(),
                token,
                nonce,
                hash(token),
                "dynamic_qr",
                now,
                expiresAt,
                "active",
                1,
                now,
                now,
                refreshIntervalSeconds,
                now,
                signatureValue,
                signatureVersion
        ));
        eventPublisher.publishEvent(new TicketRewardFacts.CredentialPresented(saved.id(), customerId));
        return toCredentialResponse(saved);
    }

    VerificationPreviewResponse previewVerification(VerificationRequest request) {
        IssuedEntitlementRecord entitlement = entitlementByCredential(request);
        if(isAmusementEntitlement(entitlement)) {
            requireAmusementActorMerchantScope(entitlement.merchantId());
            if(amusementService==null)
                throw ApiException.conflict("AMUSEMENT_PROJECT_UNRESOLVED","游乐项目无法定位");
            Instant at=Instant.now();
            var window=amusementService.requireEntryWindow(request.pointId(),request.deviceId(),at);
            boolean allowed=amusementAdmissions.preview(entitlement.id(),window,at);
            return new VerificationPreviewResponse(allowed,allowed?"ok":"not_allowed",
                    TicketEntitlementMapper.toOfflineEntitlementResponse(entitlement));
        }
        // P0-2 越权修复：预览同样会泄露他商户权益明细（剩余次数/人数/有效期），按调用者商户范围校验归属（平台 scope 放行）。
        enforceMerchantScope(entitlement.merchantId());
        Instant now = Instant.now();
        boolean withinValidity = (entitlement.validFrom() == null || !now.isBefore(entitlement.validFrom()))
                && (entitlement.validTo() == null || !now.isAfter(entitlement.validTo()));
        boolean allowed = entitlement.remainingTimes() > 0
                && "active".equals(entitlement.status())
                && withinValidity;
        return new VerificationPreviewResponse(
                allowed,
                allowed ? "ok" : "not_allowed",
                TicketEntitlementMapper.toOfflineEntitlementResponse(entitlement)
        );
    }

    VerificationResponse createVerification(VerificationRequest request) {
        VerificationResponse response = verifyEntitlement(request, "android_terminal_online", null);
        if ("failed".equals(response.status())) {
            // M723-F08（第 656 行「操作被拒绝或失败的不算用掉」）：在线核销以正常返回报告失败（过刷新窗口、签名失配，
            // verifyEntitlement 里落 failed 核销记录的两支），本次请求记为业务失败，请求事务切面据此撤回这次请求新用掉的
            // 单次核销授权（失败核销记录照常提交，响应形状不变）。只在在线核销入口标记：终端离线同步（createSyncTask）
            // 同样调 verifyEntitlement，但同步批次受理即这次同步操作成功，其单次授权照常算用掉。
            IdempotentReplayContext.markBusinessFailed();
        }
        return response;
    }

    SyncTasksResponse syncTerminalTasks(TerminalSyncTasksRequest request) {
        String deviceCode = requireText(request.deviceCode(), "device_code");
        List<TerminalSyncTaskItemRequest> tasks = request.tasks() == null ? List.of() : request.tasks();
        if (tasks.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "tasks is required");
        }
        String staffId = trimToNull(request.staffId());
        List<SyncTaskResult> results = tasks.stream()
                .map(task -> syncTask(deviceCode, staffId, task))
                .toList();
        return new SyncTasksResponse(Instant.now().toString(), results);
    }

    /**
     * R5 T5 核销冲正：对 status=success 的核销记录做冲正——回滚 issued_entitlements.used_times、
     * 原记录置 reversed/reversed_by/reversed_at、新建一条 status=reversed 的核销记录 reverses_verification_id=原id。
     * 不存在/非 success/已冲正 → 拒绝（幂等/重复冲正拒绝）。reason 必填、权限 ticket.verify.reverse、幂等键在端点层。
     */
    VerificationResponse reverseVerification(String verificationId, String reason, String reversedBy) {
        String id = requireText(verificationId, "verification_id");
        String reverseReason = requireText(reason, "reason");
        VerificationRecord original = repository.findVerificationById(id)
                .orElseThrow(() -> ApiException.notFound("VERIFICATION_NOT_FOUND", "核销记录不存在或已被删除"));
        // P0 越权修复：此前冲正全程不比对核销记录归属，可冲正他商户核销并回补其赠饮库存（跨租户 IDOR）。
        if ("amusement_online".equals(original.sourceType())) {
            amusementAdmissions.lockForReversal(original);
        } else {
            enforceMerchantScope(original.merchantId());
        }
        if (!"success".equals(original.status())) {
            throw ApiException.conflict("VERIFICATION_NOT_REVERSIBLE", "只有核销成功的记录才能冲正");
        }
        if (original.reversedAt() != null) {
            throw ApiException.conflict("VERIFICATION_ALREADY_REVERSED", "该核销记录已冲正，请勿重复操作");
        }
        Instant now = Instant.now();
        // F-TXN-04 并发双花修复：先以"原 success 记录 → reversed"的状态 CAS 作为并发唯一闸（markVerificationReversed
        // 仅当 status='success' 且 reversed_at IS NULL 才翻转并返回 1）。并发两路冲正只一路得 1（胜出），另一路得 0 →
        // 抛 ALREADY_REVERSED 立即返回、且【尚未】回滚 used_times，杜绝同一核销被回滚两次（多次票权益虚增）。
        // 必须置于 atomicReverseEntitlement 之前：此前先回滚再翻转，存在并发双方都已回滚 used_times 才到状态翻转的窗口。
        int reversedRows = repository.markVerificationReversed(original.id(), reversedBy, now);
        if (reversedRows == 0) {
            throw ApiException.conflict("VERIFICATION_ALREADY_REVERSED", "该核销记录已冲正，请勿重复操作");
        }
        // 闸已得手（本路独占该核销的冲正权）→ 回滚权益核销次数（used_times-1，曾用满恢复 active）。
        // 原记录此前核销失败则无可回滚 → 拒绝（事务回滚，状态翻转一并撤销）。
        if (!DAY_REPLAY_REASON.equals(original.resultReason())) {
            repository.atomicReverseEntitlement(original.entitlementId(), now)
                    .orElseThrow(() -> ApiException.conflict("ENTITLEMENT_REVERSE_FAILED", "该权益没有已使用次数可冲正"));
            if ("amusement_online".equals(original.sourceType()))
                amusementAdmissions.reverseFirstActivation(original,now);
        }
        // 新建一条 status=reversed 的核销记录，reverses_verification_id 指向原记录，可追溯。
        VerificationRecord reversal = repository.saveVerification(new VerificationRecord(
                "ver_" + UUID.randomUUID().toString().replace("-", ""),
                "V" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT),
                original.entitlementId(),
                original.entitlementCode(),
                original.credentialType(),
                original.credentialPayload(),
                original.deviceId(),
                original.pointId(),
                "reversed",
                reverseReason,
                "amusement_online".equals(original.sourceType()) ? "amusement_online_reversal" : "verification_reverse",
                null,
                now,
                original.nonce(),
                original.staffId(),
                original.merchantId(),
                null,
                null,
                original.verifiedPeople(),
                original.id(),
                reversedBy,
                now,
                // 冲正核销记录不落结算价（冲正对账走原 success 记录的 settlement_price 反向）。
                null
        ));
        // R5 T15 赠饮库存回补联动：若原核销系赠饮权益，发 bar.gift_reversed → bar 域将赠饮单置 reversed 并回补已扣库存。
        // 以原核销 verification_id 关联 bar_gift_redemptions（唯一键），未触发过赠饮兑换则消费者幂等跳过。
        IssuedEntitlementRecord reversedEntitlement = repository.findIssuedEntitlementById(original.entitlementId()).orElse(null);
        boolean giftDrink = reversedEntitlement != null
                && GIFT_DRINK_ENTITLEMENT_TYPE.equals(reversedEntitlement.entitlementType());
        if (giftDrink) {
            // S5：把原核销当时固化的内部结算价带下去，冲正按「当时那个价」逐分对冲，
            // 不再回头重查目录（业务 HTML §内部结算价目录「已发生的不可改」）。
            eventPublisher.publishEvent(new GiftReversedEvent(original.id(), original.settlementPriceCent()));
        }
        // 发 VerificationReversedEvent，作为 ticketing 域"核销已冲正"权威信号。现有消费者（同步、同事务）：
        //   - customer 域 LoyaltyPointsConsumer（F-TXN-03 裁决#3）：全额冲减该订单已 confirmed 积分（与"核销→confirm"对称，
        //     冲正专用幂等键 (ticket_verification_reversal, verificationId)，不与退款 deduct 冲突）；
        //   - finance 域 VerificationReversedConsumer（P1#19）：落"冲正已通知"审计留痕（无独立财务冲销对象，仅留痕）。
        // 消费者均按各自 source_doc 幂等；下游异常将随同事务回滚本次冲正（与既有 confirm 链同语义）。
        if (!DAY_REPLAY_REASON.equals(original.resultReason())) {
        eventPublisher.publishEvent(new VerificationReversedEvent(
                original.id(),
                reversal.id(),
                original.entitlementId(),
                reversedEntitlement == null ? null : reversedEntitlement.orderId(),
                original.merchantId(),
                original.verifiedPeople()
        ));
        eventPublisher.publishEvent(new TicketRewardFacts.Reversed(original.id()));
        }
        return new VerificationResponse(reversal.id(), reversal.verificationNo(), reversal.status(), reversal.resultReason());
    }

    /**
     * F-TXN-03 按核销张数：聚合该订单全部已发权益的 Σused_times / Σtotal_times（张=use），
     * 作为积分确认事件的 verifiedUnits / totalUnits（核销/冲正后读到的最新值；customer 域据此对齐确认份额）。
     */
    /** CG-08b：补偿履约只记到补偿记录，不改原核销/原结算，不产生积分。 */
    private void recordCompensationFulfillment(IssuedEntitlementRecord entitlement, VerificationRecord verification,
            Instant now, Long settlementPriceCent) {
        if (compensationFulfillmentPort != null) {
            compensationFulfillmentPort.recordFulfillment(entitlement.compensationItemId(), verification.id(), now,
                    settlementPriceCent);
        }
    }

    /**
     * AUD-066 多人票同场次核销守卫（用户口径）：同一父票的各人份额必须在同一场次内核销——
     * 同场次内不同人可不同时间分别入场（不要求同一时刻），但不能跨场次/日期。
     * 无父票（存量/直发手造）或单人票（父票 num_people&lt;=1）不约束。
     * 场次键：优先取 venue 当前 open 场次；无场次配置的票回退自然日（"不能跨日期"的机械保证）。
     * ponytail：读父票首场次键→比对（读在消费前，写用 COALESCE 写一次）；并发极端下可能双放行两场次，
     *   频度极低、可接受上限；若需强一致改为父票行加锁。
     */
    private void enforceMultiPersonSameSession(IssuedEntitlementRecord entitlement, Instant now) {
        String parentId = entitlement.parentTicketId();
        if (parentId == null) {
            return;
        }
        TicketInstanceRecord parent = repository.findTicketInstanceById(parentId).orElse(null);
        if (parent == null || parent.numPeople() <= 1) {
            return;
        }
        String firstKey = parent.firstSessionKey();
        if (firstKey != null && !firstKey.equals(sessionKeyFor(entitlement.venueId(), now))) {
            throw ApiException.conflict("MULTI_PERSON_CROSS_SESSION_FORBIDDEN",
                    "多人票各人份额必须在同一场次内核销，不能跨场次/日期");
        }
    }

    /** AUD-066 场次键：venue 当前 open 场次 id；无场次配置回退自然日（系统默认时区）。 */
    private String sessionKeyFor(String venueId, Instant now) {
        return repository.resolveOpenSessionForVenue(venueId, now)
                .orElse(now.atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString());
    }

    /** AUD-066：首次成功核销定格父票 first_used_at 与首场次键（写一次，COALESCE）。无父票跳过。 */
    private void markTicketInstanceFirstUse(IssuedEntitlementRecord entitlement, Instant now) {
        String parentId = entitlement.parentTicketId();
        if (parentId == null) {
            return;
        }
        // AUD 复核 P2-6：原子抢占父票首场次键——CAS 命中 0 行=同父票已被别的场次抢占（多人票各人份额并发首刷跨场次），
        // 抛错回滚本次消费（本方法在 TicketingService.createVerification 的 @Transactional 事务内，回滚 atomicUseEntitlement）。
        // 与 enforceMultiPersonSameSession 的读检查构成「读快拒 + 写原子闸」双闸，封死并发窗口。
        if (repository.markTicketInstanceFirstUse(parentId, sessionKeyFor(entitlement.venueId(), now), now) == 0) {
            throw ApiException.conflict("MULTI_PERSON_CROSS_SESSION_FORBIDDEN",
                    "多人票各人份额必须在同一场次内核销，不能跨场次/日期");
        }
    }

    /**
     * 方案2 申请即冻结守卫：父票非 active（退款申请中/已退/转赠中）→ 拒核销，防冻结中的票照常入场。
     * 无父票（存量/直发权益 parent 为空）跳过。
     */
    private void requireParentTicketActive(IssuedEntitlementRecord entitlement) {
        String parentId = entitlement.parentTicketId();
        if (parentId == null) {
            return;
        }
        repository.findTicketInstanceById(parentId).ifPresent(parent -> {
            if (!"active".equals(parent.status())) {
                throw ApiException.conflict("TICKET_INSTANCE_REFUND_IN_PROGRESS",
                        "该票已被冻结（退款中、已退款或转赠中），不能核销");
            }
        });
    }

    /**
     * 方案3 转赠冻结守卫:该权益所属 slot 有 pending 转赠 → 拒核销(响亮,非静默)。
     * 无父票/未分组(parentTicketId 空)跳过。与 atomicUseEntitlement 的 NOT EXISTS 兜底双闸,覆盖游乐 gateway 等直连消费路径。
     */
    private void requireSlotNotTransferring(IssuedEntitlementRecord entitlement) {
        if (entitlement.parentTicketId() == null) {
            return;
        }
        if (repository.existsPendingTransferForSlot(entitlement.parentTicketId(), entitlement.slotIndex())) {
            throw ApiException.conflict("TICKET_TRANSFER_PENDING", "该票转赠确认中，暂不可用");
        }
    }

    private VerificationResponse verifyEntitlement(VerificationRequest request, String sourceType, String clientTaskId) {
        // 健壮性修复：先校验核销设备已注册，未注册直接干净 400，杜绝 FK 违反中止事务后的脏 500。
        requireRegisteredDevice(request);
        // P1 清单#19：动态二维码核销取凭证不在此处因过窗口抛异常，改由下方按 window_started_at+refresh_interval
        // 真实判定刷新窗口，过窗口落 status=failed/result_reason=window_expired、window_valid=false（截图重放被拒、可追溯）。
        TicketCredentialRecord credential = credentialForVerification(request == null ? null : request.credentialPayload()).orElse(null);
        IssuedEntitlementRecord entitlement = credential == null
                ? entitlementByCredential(request)
                : repository.findIssuedEntitlementById(credential.entitlementId())
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "权益不存在或已被删除"));
        // 两入口以同一请求原件幂等；已完成的游乐请求可返回原事实，新入场仍由事务内回调验证动态凭证。
        if (isAmusementEntitlement(entitlement)) {
            requireAmusementSourceScope(entitlement);
            requireManagedDeviceForVerification(request);
            return verifyAmusementEntitlement(request, sourceType, Instant.now());
        }
        if (credential != null && !"active".equals(credential.status())) {
            throw ApiException.conflict("CREDENTIAL_EXPIRED", "核销凭证已过期，请刷新后重试");
        }
        // P0-2 越权修复：在线核销主链此前未做商户归属校验，A 商户核销端可凭票号/权益码核销 B 商户客人的票
        // （跨租户烧票 IDOR）。消费权益前按调用者商户范围校验归属（与 refresh/reverse 同范式；平台 scope 放行）。
        enforceMerchantScope(entitlement.merchantId());
        // S5 K16：业务不变量——赠送权益只能在提供它的那家商户核销。读快拒，置于消费权益/落任何记录之前。
        enforceProviderMerchantMatch(entitlement, request);
        Instant now = Instant.now();
        // P1 清单#19：动态二维码 window_valid 真实计算——非动态凭证（直接凭权益码）无刷新窗口概念，window_valid=null。
        Boolean windowValid = credential == null ? null : isCredentialWindowValid(credential, now);
        if (Boolean.FALSE.equals(windowValid)) {
            // 过窗口：不消费权益，落一条 failed/window_expired 核销记录（事后核对与离线截图重放拒绝留痕）。
            VerificationRecord expired = repository.saveVerification(new VerificationRecord(
                    "ver_" + UUID.randomUUID().toString().replace("-", ""),
                    "V" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT),
                    entitlement.id(),
                    entitlement.entitlementCode(),
                    requireText(request.credentialType(), "credential_type"),
                    verificationPayload(request.credentialPayload(), credential),
                    requireText(request.deviceId(), "device_id"),
                    requireText(request.pointId(), "point_id"),
                    "failed",
                    "window_expired",
                    sourceType,
                    clientTaskId,
                    now,
                    credential.nonce(),
                    null,
                    entitlement.merchantId(),
                    Boolean.FALSE,
                    null,
                    entitlement.numPeople(),
                    null,
                    null,
                    null,
                    // 失败核销(过窗口)不消费权益、不落结算价。
                    null
            ));
            return new VerificationResponse(expired.id(), expired.verificationNo(), expired.status(), expired.resultReason());
        }
        // P0-3/X-01 二维码防伪签名校验：动态凭证若带签名（signer 已注入签发），核销端按相同规范串重算并常量时间比对。
        // 失配=离线截图重放/凭证字段篡改（如延长有效期）→ 不消费权益，落一条 failed/signature_invalid 核销记录（可追溯）。
        // 非动态凭证（credential==null，直接凭权益码）/signer 未注入（兼容测试）→ signature_valid=null，不校验。
        // B2⑦：signer 已注入（生产）下动态凭证无签名（存量未签名码/被剥离签名）→ signature_valid=false 强制拒绝。
        Boolean signatureValid = credentialSignatureValid(credential, request.credentialPayload());
        if (Boolean.FALSE.equals(signatureValid)) {
            VerificationRecord invalid = repository.saveVerification(new VerificationRecord(
                    "ver_" + UUID.randomUUID().toString().replace("-", ""),
                    "V" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT),
                    entitlement.id(),
                    entitlement.entitlementCode(),
                    requireText(request.credentialType(), "credential_type"),
                    verificationPayload(request.credentialPayload(), credential),
                    requireText(request.deviceId(), "device_id"),
                    requireText(request.pointId(), "point_id"),
                    "failed",
                    "signature_invalid",
                    sourceType,
                    clientTaskId,
                    now,
                    credential.nonce(),
                    null,
                    entitlement.merchantId(),
                    windowValid,
                    Boolean.FALSE,
                    entitlement.numPeople(),
                    null,
                    null,
                    null,
                    // 失败核销(签名失配)不消费权益、不落结算价。
                    null
            ));
            return new VerificationResponse(invalid.id(), invalid.verificationNo(), invalid.status(), invalid.resultReason());
        }
        // 跨模块闭环②a：核销=高风险动作，过窗口判定通过、真正消费权益前接入 MDM 门控。
        // 核销主链确实携带操作设备标识（VerificationRequest.deviceId，在线=设备 deviceId，离线同步=设备码），
        // 故此条链路【已强制门控】：开关开+未纳管/blocked/lost 设备硬拒绝（DEVICE_NOT_MANAGED/DEVICE_BLOCKED）。
        // 置于扣减前，确保被拒时不消费权益、不落 success 核销记录。
        requireManagedDeviceForVerification(request);
        if (!"active".equals(entitlement.status())) {
            throw ApiException.conflict("ENTITLEMENT_NOT_ACTIVE", "该权益未生效");
        }
        // 方案2 申请即冻结：父票非 active（退款申请中 refund_pending / 已退 refunded / 转赠中 transferring）→ 拒核销。
        // 与方案3 转赠冻结共用同一守卫；单人票/无父票不约束（parent 为空跳过）。
        requireParentTicketActive(entitlement);
        // 方案3 转赠冻结：该 slot 有 pending 转赠 → 拒核销(响亮非静默)。与 atomicUseEntitlement NOT EXISTS 双闸。
        requireSlotNotTransferring(entitlement);
        if (entitlement.validFrom() != null && now.isBefore(entitlement.validFrom())) {
            throw ApiException.conflict("ENTITLEMENT_NOT_YET_VALID", "该权益尚未到生效时间");
        }
        if (entitlement.validTo() != null && now.isAfter(entitlement.validTo())) {
            throw ApiException.conflict("ENTITLEMENT_EXPIRED", "该权益已过期");
        }
        // AUD-066 多人票同场次核销守卫（用户口径）：同一父票的各人份额必须在同一场次内核销——
        // 同场次内不同人可不同时间分别入场，但不能跨场次/日期。单人票/无父票不约束。
        enforceMultiPersonSameSession(entitlement, now);
        // 结算重构批3-2(PRD §6.2)：入场资格「场次规则」——仅 consumption_window=per_session 的权益走场次判定；
        // 其余(NULL/per_use/per_day/unlimited_in_day)零改动、走下方现状逐次扣。若返回非 null 即已在本方法内落 success 记录并返回。
        if (SESSION_WINDOW.equals(entitlement.consumptionWindow())) {
            VerificationResponse sessionResponse =
                    verifyPerSessionEntry(entitlement, request, credential, sourceType, clientTaskId, windowValid, signatureValid, now);
            if (sessionResponse != null) {
                return sessionResponse; // 放行不消耗(本场次二次入场) → 直接返回；首入场则 fall-through 走下方正常消耗。
            }
        }
        // 非游乐既有 unlimited_in_day 口径保留；游乐已在上方按真实综合体营业日分流。
        // 其余零改动。返回非 null=当天已激活放行不消耗(短路)；null=当天首刷/剩余为 0 fall-through 走下方正常消耗
        // (剩余为 0 时 atomicUseEntitlement 失败 → ENTITLEMENT_USED_UP,即单人票跨日作废/多次票天天各扣尽后作废)。
        if (DAY_WINDOW.equals(entitlement.consumptionWindow())) {
            VerificationResponse dayResponse =
                    verifyUnlimitedInDay(entitlement, request, credential, sourceType, clientTaskId, windowValid, signatureValid, now);
            if (dayResponse != null) {
                return dayResponse; // 当天已激活/并发抢输 → 放行不消耗直接返回；当天首刷则 fall-through 正常消耗。
            }
        }
        IssuedEntitlementRecord updated = repository.atomicUseEntitlement(entitlement.id(), now)
                .orElseThrow(() -> ApiException.conflict("ENTITLEMENT_USED_UP", "该权益剩余次数已用完"));
        // AUD-018 消费后复检(新语句=新快照):发起转赠持 FOR UPDATE 锁时,本 UPDATE 解锁续跑的
        // NOT EXISTS 可能仍用旧快照看不见刚提交的 pending → 此处复检,发现在途转赠即抛错回滚本次消费。
        requireSlotNotTransferring(updated);
        // 发放已 fan-out 成单人权益（TicketOrderService：每张 num_people=1），每次核销一人；
        // 故本次核销人数恒 = 该权益 num_people(=1)，落 verified_people 记录。
        // （批4：原「verifiedPeople > num_people」是 fan-out 前的恒假死检查，已移除，核销语义不变。）
        int verifiedPeople = updated.numPeople();
        // AUD-066：首次成功核销定格父票 first_used_at + 首场次键（方案2 整票退款闸门、方案3 绑定人锚点）。
        markTicketInstanceFirstUse(updated, now);
        // 结算重构批3-1(PRD §6.1)：核销成功 → 判定本次结算价并写入核销记录 settlement_price_cent（供对账/by_entitlement_price 分账）。
        Long settlementPriceCent = resolveSettlementPrice(updated);
        VerificationRecord verification = repository.saveVerification(new VerificationRecord(
                "ver_" + UUID.randomUUID().toString().replace("-", ""),
                "V" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT),
                updated.id(),
                updated.entitlementCode(),
                requireText(request.credentialType(), "credential_type"),
                verificationPayload(request.credentialPayload(), credential),
                requireText(request.deviceId(), "device_id"),
                requireText(request.pointId(), "point_id"),
                "success",
                null,
                sourceType,
                clientTaskId,
                now,
                credential == null ? null : credential.nonce(),
                null,
                updated.merchantId(),
                // P1 清单#19：window_valid 真实计算结果（动态二维码=窗口内 true；非动态凭证=null），不再恒 TRUE。
                windowValid,
                // P0-3/X-01：签名校验结果（带签名动态凭证=校验通过 true；无签名/非动态凭证/兼容路径=null）。
                signatureValid,
                verifiedPeople,
                null,
                null,
                null,
                settlementPriceCent
        ));
        // P0 清单#2 / F-TXN-03 按核销张数：核销成功 → 发积分确认事件，带该订单最新 Σused/Σtotal；
        // customer 域据此把已确认份额对齐 floor(总额 × Σused / Σtotal)（多次/多人票部分核销按张数比例确认）。
        // CG-08b：补偿补发权益的使用不重复产生原人份/次份的积分与成长值（不发积分确认事件），履约锁定到补偿记录。
        if (updated.compensationItemId() != null) {
            recordCompensationFulfillment(updated, verification, now, settlementPriceCent);
        } else if (updated.orderId() != null && !updated.orderId().isBlank()) {
            eventPublisher.publishEvent(new TicketRewardFacts.Used(verification.id(), credential == null ? null : credential.id()));
        }
        // 结算重构批１：核销成功 → 发 settlement.settled，settlement 域 AFTER_COMMIT 双写 settlement_transactions
        // （核销结算逐笔流水,观察期与旧 allocated 快照口径对账）。异步、不属核销主链——下游失败不回滚核销。
        eventPublisher.publishEvent(new SettlementSettledEvent(
                verification.id(), // AUD-012：本次核销记录 id → 不可变增量结算幂等键
                now,               // AUD-012：核销业务时刻 → account 月窗聚合基准
                updated.id(),
                updated.orderId(),
                updated.ticketTypeId(),
                updated.entitlementType(),
                updated.merchantId(),
                updated.totalTimes(),
                updated.usedTimes(),
                updated.numPeople(),
                updated.allocatedRevenueCent(),
                updated.validTo(),
                // 批3-4：把该权益专属商户带入结算事件，双写时定格进 settlement_transactions（空=非专属分池）。
                updated.exclusiveMerchantId(),
                // 餐饮结算(用户2026-07-06)：本次核销结算价随事件带下游；专属餐饮权益按此价×核销数直付餐厅(内部结算价)，非专属仍按 allocated。
                settlementPriceCent,
                // D6：本权益满额份额随事件带下游，消费者算折扣增量；为空=无折扣。
                updated.componentFullPriceCent(),
                // S2：本次核销所属项目公司（综合体）scope，由核销人 token 派生（不取请求体）。
                //   下游用它解析综合服务费费率规则的项目维度 + 落 settlement_transactions.project_id——
                //   原先消费者两处各写死 null，后台配的项目级费率特例永远命中不了。
                //   平台级操作者返回空 → null → 平台默认口径（与提成费率 / 高价值阈值同一取数范式）。
                authorizationService == null ? null : authorizationService.currentProjectScopeQuietly().orElse(null),
                // S3：本次核销的权益属于哪一张票 —— 门票综合服务费「一张票只收一道」的判定锚点。
                updated.parentTicketId()
        ));
        if (credential != null) {
            // AUD-012 凭证消费 CAS(仅 active→consumed):0 行=该凭证已被并发撤销(转赠领取撤销原持有人旧码)
            // 或已消费 → 响亮拒绝并回滚本次核销(本方法在 @Transactional 事务内,success 记录/扣减一并回滚)。
            if (repository.markTicketCredentialConsumed(credential.id(), now) == 0) {
                throw ApiException.conflict("CREDENTIAL_EXPIRED", "核销凭证已过期，请刷新后重试");
            }
        }
        // R5 T15：赠饮权益核销成功 → 发同步事件 ticket.gift_verified，bar 域据此生成 0 元单并扣库存。
        // AUD-015：同步发布且与本核销同事务——赠饮履约缺配置/缺库存时消费者抛异常，会回滚整笔核销(fail-closed)、
        // 票权益保留可用；严禁在消费者里 try/catch 吞异常放行（否则权益已消费却无出品/无扣库/无结算，重犯 AUD-015）。
        if (GIFT_DRINK_ENTITLEMENT_TYPE.equals(updated.entitlementType())) {
            // S5：itemRef 语义定死 = bar_products.id（建模板时已校验存在且属本提供方）。
            String barItemRef = repository.findEntitlementTemplateById(updated.entitlementTemplateId())
                    .map(EntitlementTemplateRecord::barItemRef)
                    .orElse(null);
            eventPublisher.publishEvent(new GiftVerifiedEvent(
                    updated.id(),
                    verification.id(),
                    barItemRef,
                    // S5：出品吧台 = 提供方商户（K16 已保证本次核销就发生在这家）。此前误用 point_id，
                    // 而赠饮权益的 point_id 多为空 → 赠饮履约恒报「无吧台归属」。
                    providerMerchantId(updated),
                    null,
                    updated.merchantId(),
                    // S5：本次内部结算价随事件带下游，兑付与预扣同源、不再由 bar 域另查一遍（口径不可能漂）。
                    settlementPriceCent,
                    // S6：成本承担方随事件带下游——商户让利型不挂商户应付（HTML:737「不与商户结算」），
                    //   履约（0 元单/出品/扣库）照常。取自票面 binding 配置（发放来源的同一份配置）。
                    costBearer(updated)
            ));
        }
        return new VerificationResponse(verification.id(), verification.verificationNo(), verification.status(), verification.resultReason());
    }

    /** 阶段1：游乐类权益判定——entitlement_type 以 "amusement" 开头（与 amusement 域同口径）。 */
    private boolean isAmusementEntitlement(IssuedEntitlementRecord entitlement) {
        String type = entitlement.entitlementType();
        return type != null && type.trim().toLowerCase(Locale.ROOT).startsWith(AMUSEMENT_ENTITLEMENT_TYPE_PREFIX);
    }

    private VerificationResponse verifyAmusementEntitlement(
            VerificationRequest request, String sourceType, Instant now) {
        if(amusementService==null)
            throw ApiException.conflict("AMUSEMENT_PROJECT_UNRESOLVED","游乐核销服务不可用，不能转用通用核销");
        if("android_terminal_offline".equals(sourceType))
            throw ApiException.conflict("AMUSEMENT_ONLINE_REQUIRED","游乐权益必须在线核销，不接受离线补核销");
        String key=requireText(request.idempotencyKey(),"idempotency_key");
        var admission=amusementService.verifyAmusementEntry(new AmusementEntryRequest(
                requireText(request.credentialPayload(),"credential_payload"),
                request.pointId(),request.deviceId(),trimToNull(textFromBusinessContext(request,"staff_id","staffId")),key,now))
            .orElseThrow(()->ApiException.conflict("AMUSEMENT_PROJECT_UNRESOLVED",
                    "核销点或设备未关联真实游乐项目，不能核销游乐权益"));
        VerificationRecord fact=repository.findVerificationById(ticketIdForAmusement(admission.id()))
            .orElseThrow(()->ApiException.conflict("AMUSEMENT_ADMISSION_FACT_REQUIRED","游乐入场未生成对应票务核销事实"));
        return new VerificationResponse(fact.id(),fact.verificationNo(),fact.status(),fact.resultReason());
    }


    /** 游乐已经完成的入场原件，两个入口共用；不再次消费权益。 */
    VerificationRecord recordAmusementAdmission(
            com.complex.platform.amusement.AmusementVerificationRecord admission,
            com.complex.platform.amusement.AmusementBusinessDayService.Window window,
            com.complex.platform.amusement.AmusementEntitlementGateway.Consumption consumption) {
        String ticketId=ticketIdForAmusement(admission.id());
        Optional<VerificationRecord> existing=repository.findVerificationById(ticketId);
        if(existing.isPresent()) {
            if(!admission.id().equals(existing.get().amusementVerificationId()))
                throw ApiException.conflict("AMUSEMENT_ADMISSION_FACT_MISMATCH","入场原件与票务事实不一致");
            return existing.get();
        }
        Instant now=admission.createdAt();
        IssuedEntitlementRecord consumed=repository.findIssuedEntitlementById(admission.entitlementId())
                .orElseThrow(()->ApiException.notFound("ENTITLEMENT_NOT_FOUND","权益不存在"));
        requireAmusementConsumption(consumed,now);
        TicketCredentialRecord credential=credentialForVerification(admission.credentialPayload()).orElse(null);
        Boolean windowValid=credential==null?null:isCredentialWindowValid(credential,now);
        Boolean signatureValid=credentialSignatureValid(credential, admission.credentialPayload());
        if(credential!=null && (!"active".equals(credential.status()) || !consumed.id().equals(credential.entitlementId()) || !Boolean.TRUE.equals(windowValid)
                || now.isAfter(credential.expiresAt())))
            throw ApiException.conflict("CREDENTIAL_EXPIRED","核销凭证已过期或不属于本次权益，请刷新后重试");
        if(Boolean.FALSE.equals(signatureValid))
            throw ApiException.conflict("CREDENTIAL_SIGNATURE_INVALID","核销凭证签名无效");
        String terminalDeviceId=amusementAdmissions.terminalDeviceId(admission);
        requireManagedDeviceForVerification(new VerificationRequest(admission.id(),"qr",admission.credentialPayload(),
                terminalDeviceId,admission.locationPointId(),Map.of()));
        if(!DAY_WINDOW.equals(consumed.consumptionWindow())) {
            markTicketInstanceFirstUse(consumed,now);
        } else if(consumed.parentTicketId()!=null) {
            var parent=repository.findTicketInstanceById(consumed.parentTicketId()).orElseThrow();
            // 已有大楼检票场次不被场内游乐改写；纯游乐首入场使用真实营业日锚点。
            String key=parent.firstSessionKey()!=null?parent.firstSessionKey():
                    "amusement:"+window.complexId()+":"+window.businessDate();
            if(repository.markTicketInstanceFirstUse(parent.id(),key,now)==0)
                throw ApiException.conflict("MULTI_PERSON_CROSS_SESSION_FORBIDDEN","多人票不允许跨场次使用");
        }
        boolean dayReplay=consumption==com.complex.platform.amusement.AmusementEntitlementGateway.Consumption.DAY_REPLAY;
        Long settlementPriceCent=dayReplay?null:consumed.valueAmountCent();
        VerificationRecord verification=repository.saveVerification(new VerificationRecord(
                ticketId,"VA"+hash(admission.id()).substring(0,48),consumed.id(),consumed.entitlementCode(),
                credential==null?"entitlement_code":"dynamic_qr",verificationPayload(admission.credentialPayload(),credential),
                terminalDeviceId,admission.locationPointId(),"success",dayReplay?DAY_REPLAY_REASON:"amusement_verified",
                "amusement_online",null,now,credential==null?null:credential.nonce(),admission.staffId(),
                consumed.merchantId(),windowValid,signatureValid,admission.verifiedPeople(),null,null,null,
                settlementPriceCent,admission.id()));
        if(!dayReplay) {
            if(consumed.compensationItemId()!=null)
                recordCompensationFulfillment(consumed,verification,now,settlementPriceCent);
            else if(consumed.orderId()!=null && !consumed.orderId().isBlank())
                eventPublisher.publishEvent(new TicketRewardFacts.Used(verification.id(),credential==null?null:credential.id()));
            eventPublisher.publishEvent(new SettlementSettledEvent(
                verification.id(),now,consumed.id(),consumed.orderId(),consumed.ticketTypeId(),
                consumed.entitlementType(),consumed.merchantId(),consumed.totalTimes(),consumed.usedTimes(),
                consumed.numPeople(),consumed.allocatedRevenueCent(),consumed.validTo(),consumed.exclusiveMerchantId(),
                settlementPriceCent,consumed.componentFullPriceCent(),window.complexId(),consumed.parentTicketId()));
        }
        if(credential!=null && repository.markTicketCredentialConsumed(credential.id(),now)==0)
            throw ApiException.conflict("CREDENTIAL_EXPIRED","核销凭证已过期，请刷新后重试");
        return verification;
    }

    void requireAmusementConsumption(IssuedEntitlementRecord entitlement,Instant now) {
        if(!isAmusementEntitlement(entitlement))
            throw ApiException.conflict("ENTITLEMENT_NOT_AMUSEMENT","该权益不是游乐类权益");
        requireAmusementActorMerchantScope(entitlement.merchantId());
        if(!"active".equals(entitlement.status()))
            throw ApiException.conflict("ENTITLEMENT_NOT_ACTIVE","该权益未生效");
        requireParentTicketActive(entitlement);
        requireSlotNotTransferring(entitlement);
        if(!DAY_WINDOW.equals(entitlement.consumptionWindow())) enforceMultiPersonSameSession(entitlement,now);
        if(entitlement.validFrom()!=null && now.isBefore(entitlement.validFrom()))
            throw ApiException.conflict("ENTITLEMENT_NOT_YET_VALID","该权益尚未到生效时间");
        if(entitlement.validTo()!=null && now.isAfter(entitlement.validTo()))
            throw ApiException.conflict("ENTITLEMENT_EXPIRED","该权益已过期");
    }

    /** 外层幂等也须复核当前来源范围；只读凭证原件，不重做其时效或已消费判定。 */
    void requireAmusementRequestScope(VerificationRequest request) {
        String payload=trimToNull(request==null?null:request.credentialPayload());
        if(payload==null) return;
        Optional<IssuedEntitlementRecord> entitlement=payload.startsWith("cred_")
                ? repository.findTicketCredentialByToken(payload)
                    .flatMap(credential->repository.findIssuedEntitlementById(credential.entitlementId()))
                : repository.findIssuedEntitlementByCode(payload);
        entitlement.filter(this::isAmusementEntitlement).ifPresent(this::requireAmusementSourceScope);
    }

    /** 冲正重放只核原成功入场的实时范围，不要求原件仍处于未冲正状态。 */
    void requireAmusementReversalScope(String verificationId) {
        String id=trimToNull(verificationId);
        if(id==null) return;
        repository.findVerificationById(id)
                .filter(original->"amusement_online".equals(original.sourceType()))
                .ifPresent(original -> amusementAdmissions.requireReversalScope(original));
    }

    private void requireAmusementSourceScope(IssuedEntitlementRecord entitlement) {
        String project=entitlement.orderId()==null?null:repository.findTicketOrderById(entitlement.orderId())
                .map(TicketOrderRecord::projectId).orElse(null);
        if(project==null || project.isBlank())
            throw ApiException.conflict("TICKET_PROJECT_REQUIRED","原游乐票订单缺少真实综合体，不能核销或写入核销结果");
        if(authorizationService!=null) {
            authorizationService.requirePermission("verification.execute");
            if(!authorizationService.projectReach(authorizationService.currentActor(),"verification.execute").covers(project))
                throw ApiException.forbidden("FORBIDDEN","无权核销或写入该综合体门票的核销结果");
        }
        requireAmusementActorMerchantScope(entitlement.merchantId());
    }

    private void requireAmusementActorMerchantScope(String merchantId) {
        if(authorizationService==null) return;
        String limitedMerchant=authorizationService.currentActor().merchantId();
        if(limitedMerchant!=null && !limitedMerchant.equals(merchantId))
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN","无权操作其他商户的权益");
    }

    private String ticketIdForAmusement(String admissionId) {
        return "ver_amu_"+hash(admissionId).substring(0,56);
    }


    /** 阶段1：从核销请求 businessContext 取文本值（游乐分流需要的 staff_id 等），无则返回 null。 */
    private String textFromBusinessContext(VerificationRequest request, String... keys) {
        Map<String, Object> ctx = request == null ? null : request.businessContext();
        if (ctx == null) {
            return null;
        }
        for (String key : keys) {
            Object value = ctx.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return null;
    }

    // 结算重构批3-1(PRD §6.1)：结算类别常量。physical 的判定已收进 InternalSettlementPriceResolver（S5 统一口径）。
    private static final String KIND_VIRTUAL = "virtual";

    // 结算重构批3-2(PRD §6.2)：入场资格消耗单位——按场次核销(入过场当场次持续进出不重复消耗、过场作废)。
    private static final String SESSION_WINDOW = "per_session";

    // 结算重构批3-3(PRD §6.3)：游乐消耗单位——按自然日激活(每自然日首刷消耗一次、当天无限、跨日新扣一次)。
    private static final String DAY_WINDOW = "unlimited_in_day";
    private static final String DAY_REPLAY_REASON = "day_replay";

    /**
     * 结算重构批3-2(PRD §6.2)：入场资格「场次规则」核销判定（consumption_window=per_session）。
     * <ul>
     *   <li>非营业时段（无当前 open 场次）→ 抛 {@code VENUE_NOT_IN_SESSION}。</li>
     *   <li>本场次已入过场 → 放行【不消耗】：落一条 success/session_reentry 记录(不 atomicUseEntitlement、不落结算价、
     *       不发积分/结算事件)，返回该 response（调用方据此短路返回）。</li>
     *   <li>曾在别的场次入过场（过了首入场场次）→ 抛 {@code ENTRY_PASS_PAST_SESSION_VOID}。</li>
     *   <li>本场次首入（且从未入过任何场次）→ 建入场记录(唯一键防并发重复)，返回 null → 调用方 fall-through 走
     *       正常消耗一次(atomicUseEntitlement + 批3-1 settlement_price + 积分/结算事件)。</li>
     * </ul>
     * ponytail：惰性开场——核销时按营业时间表判当前是否营业并取/开当日场次，不做后台定时开歇场 job。
     * 若日后要精确歇业关场（把过场作废落到墙钟歇业时刻而非"下一场次开启前不再判过场"），再加定时 job 置 closed。
     */
    private VerificationResponse verifyPerSessionEntry(
            IssuedEntitlementRecord entitlement,
            com.complex.platform.ticketing.dto.VerificationRequest request,
            TicketCredentialRecord credential,
            String sourceType,
            String clientTaskId,
            Boolean windowValid,
            Boolean signatureValid,
            Instant now) {
        String sessionId = repository.resolveOpenSessionForVenue(entitlement.venueId(), now)
                .orElseThrow(() -> ApiException.conflict(
                        "VENUE_NOT_IN_SESSION", "场馆当前不在营业场次内"));
        // 本场次已入过场 → 放行不消耗（持续进出）。
        if (repository.hasEntitlementEntryInSession(entitlement.id(), sessionId)) {
            VerificationRecord reentry = repository.saveVerification(sessionVerificationRecord(
                    entitlement, request, credential, sourceType, clientTaskId, windowValid, signatureValid, now,
                    "session_reentry"));
            return new VerificationResponse(reentry.id(), reentry.verificationNo(), reentry.status(), reentry.resultReason());
        }
        // 曾在别的场次入过场 → 过了首入场场次，本场次作废拒绝。
        if (repository.hasEntitlementEntryInOtherSession(entitlement.id(), sessionId)) {
            throw ApiException.conflict(
                    "ENTRY_PASS_PAST_SESSION_VOID", "入场凭证已失效：已在上一场次入场");
        }
        // 本场次首入（且从未入过任何场次）→ 建入场记录（并发唯一键：若并发另一路抢先插入本场次记录，本路得 false=改判二次入场放行）。
        if (!repository.recordEntitlementEntry(entitlement.id(), sessionId, now)) {
            VerificationRecord reentry = repository.saveVerification(sessionVerificationRecord(
                    entitlement, request, credential, sourceType, clientTaskId, windowValid, signatureValid, now,
                    "session_reentry"));
            return new VerificationResponse(reentry.id(), reentry.verificationNo(), reentry.status(), reentry.resultReason());
        }
        return null; // 首入 → 调用方 fall-through 正常消耗一次。
    }

    /** 批3-2：场次入场"放行不消耗"的 success 核销记录（settlement_price 为 null，不落新结算收入）。 */
    private VerificationRecord sessionVerificationRecord(
            IssuedEntitlementRecord entitlement,
            com.complex.platform.ticketing.dto.VerificationRequest request,
            TicketCredentialRecord credential,
            String sourceType,
            String clientTaskId,
            Boolean windowValid,
            Boolean signatureValid,
            Instant now,
            String resultReason) {
        return new VerificationRecord(
                "ver_" + UUID.randomUUID().toString().replace("-", ""),
                "V" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT),
                entitlement.id(),
                entitlement.entitlementCode(),
                requireText(request.credentialType(), "credential_type"),
                verificationPayload(request.credentialPayload(), credential),
                requireText(request.deviceId(), "device_id"),
                requireText(request.pointId(), "point_id"),
                "success",
                resultReason,
                sourceType,
                clientTaskId,
                now,
                credential == null ? null : credential.nonce(),
                null,
                entitlement.merchantId(),
                windowValid,
                signatureValid,
                entitlement.numPeople(),
                null,
                null,
                null,
                // 放行不消耗：不落结算价（本场次首入已在正常消耗路径落过一次，二次进出不再重复计收入）。
                null);
    }

    /**
     * 结算重构批3-3(PRD §6.3)：游乐「当天无限」核销判定(consumption_window=unlimited_in_day)。
     * 口径(2026-07-04 用户拍板)：每次核销激活「一个整天无限」——每自然日首刷消耗一次(扣次+落当天结算价+积分/结算事件);
     * 同一权益当天再刷判「已激活」放行、不重复扣次不重复计费;跨自然日=新的一天再消耗一次(从剩余次数再扣)。
     * <ul>
     *   <li>当天已激活(day_activations 有本权益本 usage_date 记录) → 放行【不消耗】：落 success/day_replay 记录
     *       (不 atomicUseEntitlement、不落结算价、不发积分/结算事件),返回 response(调用方短路)。</li>
     *   <li>当天未激活且剩余 > 0 → INSERT ON CONFLICT DO NOTHING：真插(rows>0)→ 返回 null → 调用方 fall-through 正常消耗一次;
     *       并发抢输(rows=0)→ 放行不消耗(落 day_replay)。</li>
     *   <li>当天未激活且剩余 = 0 → 不插激活(不留孤儿放行位)、返回 null → fall-through → atomicUseEntitlement 失败
     *       → ENTITLEMENT_USED_UP。单人票 total=1 次日即此路(跨日作废);多次票 total=N 天天各扣一次、扣尽后此路。</li>
     * </ul>
     * ponytail：不做跨日作废报错——跨日=新自然日再消耗,单人靠 total=1 自然 used_up、多次靠 total=N 保留,复用现有次数模型。
     * 自然日用系统默认时区落墙钟本地日(与批3-2 一致);若日后按各 venue 独立时区,在此换算即可,接口不变。
     */
    private VerificationResponse verifyUnlimitedInDay(
            IssuedEntitlementRecord entitlement,
            com.complex.platform.ticketing.dto.VerificationRequest request,
            TicketCredentialRecord credential,
            String sourceType,
            String clientTaskId,
            Boolean windowValid,
            Boolean signatureValid,
            Instant now) {
        java.time.LocalDate usageDate = now.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        // 当天已激活 → 放行不消耗。
        if (repository.hasEntitlementDayActivation(entitlement.id(), usageDate)) {
            return saveDayReplay(entitlement, request, credential, sourceType, clientTaskId, windowValid, signatureValid, now);
        }
        // 当天未激活但剩余为 0 → 不插激活(避免落孤儿放行位使已尽权益当天再被放行),fall-through 让 atomicUseEntitlement 报 used_up。
        if (entitlement.remainingTimes() <= 0) {
            return null;
        }
        // 当天首刷:唯一键(entitlement_id,usage_date)防并发。真插=本自然日首刷 → fall-through 正常消耗一次;
        // 并发另一路抢先插=本路 false → 放行不消耗(与"当天已激活"同)。
        if (!repository.recordEntitlementDayActivation(entitlement.id(), usageDate, now)) {
            return saveDayReplay(entitlement, request, credential, sourceType, clientTaskId, windowValid, signatureValid, now);
        }
        return null; // 当天首刷 → 调用方 fall-through 正常消耗一次。
    }

    /** 批3-3：当天无限"放行不消耗"的 success 核销记录(settlement_price 为 null,不落新结算收入)。 */
    private VerificationResponse saveDayReplay(
            IssuedEntitlementRecord entitlement,
            com.complex.platform.ticketing.dto.VerificationRequest request,
            TicketCredentialRecord credential,
            String sourceType,
            String clientTaskId,
            Boolean windowValid,
            Boolean signatureValid,
            Instant now) {
        VerificationRecord replay = repository.saveVerification(sessionVerificationRecord(
                entitlement, request, credential, sourceType, clientTaskId, windowValid, signatureValid, now,
                DAY_REPLAY_REASON));
        return new VerificationResponse(replay.id(), replay.verificationNo(), replay.status(), replay.resultReason());
    }

    /**
     * S5：该权益的提供方商户——发放时定格的值，票面上写着「这杯酒是哪家吧请的」。
     * 单店票出票会把酒吧写进 bar_id，通票走模板归属商户；两者按 AUD-076/绑定校验本就一致，取先有的那个。
     */
    private static String providerMerchantId(IssuedEntitlementRecord entitlement) {
        return entitlement.barId() != null ? entitlement.barId() : entitlement.merchantId();
    }

    /**
     * S6：该权益的成本承担方——查它所属票种上同模板的 binding 配置（{@code ticket_type_entitlements.cost_bearer}）。
     * 查不到 binding（direct 出票等无票面来源）回退 platform_funded（=改造前唯一行为）。
     * ponytail: 读当下票面配置而非发放时快照——运营中途改承担方会影响未兑付赠品的结算口径，
     * 需要「已发放不追溯」时把 cost_bearer 复制进 issued_entitlements 即可。
     */
    private String costBearer(IssuedEntitlementRecord entitlement) {
        if (entitlement.ticketTypeId() == null) {
            return TicketTypeEntitlementRecord.COST_BEARER_PLATFORM_FUNDED;
        }
        for (TicketTypeEntitlementRecord binding : repository.listTicketTypeEntitlements(entitlement.ticketTypeId())) {
            if (binding.entitlementTemplateId().equals(entitlement.entitlementTemplateId())) {
                return binding.costBearer();
            }
        }
        return TicketTypeEntitlementRecord.COST_BEARER_PLATFORM_FUNDED;
    }

    /**
     * S5 K16 跨提供方核销守卫（业务不变量，<b>不是</b>权限）：
     * 「A 酒吧的赠酒只能在 A 酒吧核销，不能拿到其他酒吧核销」（业务 HTML §通票赠酒按酒吧分配）。
     *
     * <p>为什么必须与角色无关：这条是给酒吧店长看的账——B 吧的机器核掉了 A 吧的赠酒，月底 A 吧的成本
     * 记在 B 吧头上，两家都对不平。平台管理员用平台账号核也一样错，所以平台 scope 同样受约束
     * （{@code enforceMerchantScope} 对空 scope 放行，那是权限层，管不了这件事）。
     *
     * <p>核销上下文 = 核销设备绑定的商户。设备没绑商户就说不清这杯酒是在哪家核的 → 一律拒绝（fail-closed），
     * 让运营先去把设备归属配好，而不是先记一笔说不清的账。离线同步旁路复用本方法
     * （device_code 已在 {@code createSyncTask} 里解析成设备 id），无须另写。
     */
    private void enforceProviderMerchantMatch(IssuedEntitlementRecord entitlement, VerificationRequest request) {
        if (!InternalSettlementPriceResolver.providerRedeemed(
                entitlement.entitlementType(), entitlement.entitlementKind(), entitlement.exclusiveMerchantId())) {
            return; // 入场/游乐等非「到提供方兑付」类权益不受此约束。
        }
        String provider = providerMerchantId(entitlement);
        if (provider == null) {
            throw ApiException.conflict("ENTITLEMENT_PROVIDER_UNRESOLVED",
                    "该赠送权益未绑定提供方商户，无法核销");
        }
        String context = repository
                .findTerminalDeviceMerchantId(trimToNull(request == null ? null : request.deviceId()))
                .orElse(null);
        if (context == null) {
            throw ApiException.conflict("VERIFY_DEVICE_MERCHANT_UNRESOLVED",
                    "核销设备未绑定商户，无法判定本次核销发生在哪家门店");
        }
        if (!provider.equals(context)) {
            throw ApiException.conflict("ENTITLEMENT_PROVIDER_MISMATCH",
                    "该权益只能在发放它的商户核销");
        }
    }

    /**
     * 核销时判定该次结算价（分）。S5 起分两条口径，<b>各按各的性质取值，绝不互相回退</b>：
     * <ul>
     *   <li><b>提供方兑付类（逐项赠送权益）</b>：成本一律走 {@link InternalSettlementPriceResolver}
     *       （提供方商品内部结算价 → 模板签约内部结算价）；取不到就 <b>fail-closed</b> 拒绝核销，
     *       不再静默回退票面价——业务 HTML 明写「票面原价与内部结算价两者不可混用」，回退会让预扣基数
     *       悄悄用成售价，账面平、钱全错。</li>
     *   <li><b>其余（入场/游乐等）</b>：票面价即分账基准、本就不是成本口径，维持现状（虚拟取签约单价、
     *       否则回退 value_amount_cent），逐分不变。</li>
     * </ul>
     */
    private Long resolveSettlementPrice(IssuedEntitlementRecord entitlement) {
        if (InternalSettlementPriceResolver.providerRedeemed(
                entitlement.entitlementType(), entitlement.entitlementKind(), entitlement.exclusiveMerchantId())) {
            // 批10/E:两支取价统一收口目录——商品支经商品的目录引用,餐饮/零售支经模板的目录引用;
            // 核销时固化的即「当时的目录生效价」(改价转正只影响此后核销,已核销流水一分不动)。
            EntitlementTemplateRecord template = repository
                    .findEntitlementTemplateById(entitlement.entitlementTemplateId()).orElse(null);
            return priceResolver.require(
                    template == null ? null : template.barItemRef(),
                    providerMerchantId(entitlement),
                    template == null ? null : template.catalogItemCode());
        }
        if (KIND_VIRTUAL.equals(entitlement.entitlementKind()) && entitlement.agreementPriceCent() != null) {
            return entitlement.agreementPriceCent();
        }
        return entitlement.valueAmountCent();
    }

    /**
     * 跨模块闭环②a：核销前置 MDM 高风险门控。
     * device_id 在核销主链为必填（在线设备 deviceId / 离线同步设备码），故正常路径必有设备标识。
     * mdmService 未注入（兼容构造，mdmService=null）或 device 缺失时安全跳过（不伪造假值），其余情形委托
     * {@link MdmService#requireManagedForHighRiskAction}：开关开+未纳管硬拒绝；blocked/lost 设备无论开关均硬拒绝。
     */
    private void requireManagedDeviceForVerification(VerificationRequest request) {
        if (mdmService == null) {
            return;
        }
        String deviceId = trimToNull(request == null ? null : request.deviceId());
        if (deviceId == null) {
            return;
        }
        mdmService.requireManagedForHighRiskAction(deviceId, VERIFY_ACTION_TYPE);
    }

    private SyncTaskResult syncTask(String deviceCode, String staffId, TerminalSyncTaskItemRequest task) {
        String clientTaskId = requireText(task.clientTaskId(), "client_task_id");
        return repository.findTerminalSyncTask(deviceCode, clientTaskId)
                .map(this::toSyncTaskResult)
                .orElseGet(() -> createSyncTask(deviceCode, staffId, task));
    }

    private SyncTaskResult createSyncTask(String deviceCode, String staffId, TerminalSyncTaskItemRequest task) {
        String taskType = requireText(task.taskType(), "task_type");
        String clientTaskId = requireText(task.clientTaskId(), "client_task_id");
        String occurredAt = requireText(task.occurredAt(), "occurred_at");
        Map<String, String> payload = task.payload() == null ? Map.of() : task.payload();
        String verificationId = null;
        String verificationNo = null;
        String taskStatus = "accepted";
        String taskReason = null;

        if ("offline_verification".equals(taskType)) {
            String entitlementCode = firstPayload(payload, "entitlementCode", "entitlement_code", "credentialPayload", "credential_payload");
            String pointId = firstPayloadOptional(payload, "pointId", "point_id", "gateId", "gate_id", "point");
            // 同步请求携带 device_code(≠ 设备 PK id),须解析成真 id 才能落 ticket_verifications.device_id(FK);
            // 查不到=设备未注册,抛干净业务错(避免下游 FK 违反污染事务)。
            String resolvedDeviceId = repository.resolveTerminalDeviceId(deviceCode)
                    .orElseThrow(() -> ApiException.badRequest(
                            "DEVICE_NOT_REGISTERED", "设备未登记：" + deviceCode));
            VerificationRequest verifyRequest = new VerificationRequest(
                    "sync-task-" + clientTaskId,
                    "qr",
                    entitlementCode,
                    resolvedDeviceId,
                    pointId == null ? resolvedDeviceId : pointId,
                    Map.of("source", "terminal_sync_task")
            );
            try {
                VerificationResponse verification = verifyEntitlement(verifyRequest, "android_terminal_offline", clientTaskId);
                verificationId = verification.id();
                verificationNo = verification.verificationNo();
            } catch (ApiException outcome) {
                // CG-05：终端离线已发生的核销尝试，其结果按闭集落记录——规则不允许 → rejected，技术原因 → failed；
                // 本任务结果同态，同批其他任务照常处理（不再整批抛错）。未归类的错误码维持原异常。
                String status = outcomeStatusForCode(outcome.code());
                if (status == null) {
                    throw outcome;
                }
                taskStatus = status;
                taskReason = outcome.code().toLowerCase(Locale.ROOT);
                VerificationRecord recorded = recordOfflineOutcome(verifyRequest, status, taskReason, staffId, clientTaskId);
                if (recorded != null) {
                    verificationId = recorded.id();
                    verificationNo = recorded.verificationNo();
                }
            }
        }

        TerminalSyncTaskRecord saved = repository.saveTerminalSyncTask(new TerminalSyncTaskRecord(
                "sync_" + UUID.randomUUID().toString().replace("-", ""),
                deviceCode,
                staffId,
                clientTaskId,
                taskType,
                taskStatus,
                taskReason,
                verificationId,
                verificationNo,
                occurredAt,
                sanitizedSyncPayload(payload),
                Instant.now()
        ));
        return toSyncTaskResult(saved);
    }

    /**
     * CG-05：离线同步中被拒绝 / 失败的核销尝试落一条非成功核销记录（不消费权益、不落结算价）；
     * 权益无法定位（凭证不存在等）时无记录可落，返回 null（任务结果仍带状态与原因）。
     */
    private VerificationRecord recordOfflineOutcome(VerificationRequest request, String status, String reason,
            String staffId, String clientTaskId) {
        IssuedEntitlementRecord entitlement;
        try {
            entitlement = entitlementByCredential(request);
        } catch (ApiException unresolved) {
            return null;
        }
        if(isAmusementEntitlement(entitlement)) requireAmusementSourceScope(entitlement);
        return repository.saveVerification(new VerificationRecord(
                "ver_" + UUID.randomUUID().toString().replace("-", ""),
                "V" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT),
                entitlement.id(),
                entitlement.entitlementCode(),
                "qr",
                verificationPayload(request.credentialPayload(), null),
                request.deviceId(),
                request.pointId(),
                status,
                reason,
                "android_terminal_offline",
                clientTaskId,
                Instant.now(),
                null,
                staffId,
                entitlement.merchantId(),
                null,
                null,
                entitlement.numPeople(),
                null,
                null,
                null,
                null
        ));
    }

    private SyncTaskResult toSyncTaskResult(TerminalSyncTaskRecord task) {
        return new SyncTaskResult(
                task.clientTaskId(),
                task.status(),
                task.reason(),
                task.verificationId(),
                task.verificationNo(),
                null
        );
    }

    private IssuedEntitlementRecord entitlementByCredential(VerificationRequest request) {
        String entitlementCode = requireText(request == null ? null : request.credentialPayload(), "credential_payload");
        if (entitlementCode.startsWith("cred_")) {
            TicketCredentialRecord credential = credentialByPayload(entitlementCode)
                    .orElseThrow(() -> ApiException.notFound("CREDENTIAL_NOT_FOUND", "核销凭证不存在或已失效"));
            return repository.findIssuedEntitlementById(credential.entitlementId())
                    .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "权益不存在或已被删除"));
        }
        return repository.findIssuedEntitlementByCode(entitlementCode)
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "权益不存在或已被删除"));
    }

    private Optional<TicketCredentialRecord> credentialByPayload(String credentialPayload) {
        String payload = requireText(credentialPayload, "credential_payload");
        if (!payload.startsWith("cred_")) {
            return Optional.empty();
        }
        TicketCredentialRecord credential = repository.findTicketCredentialByToken(payload)
                .orElseThrow(() -> ApiException.notFound("CREDENTIAL_NOT_FOUND", "核销凭证不存在或已失效"));
        if (!"active".equals(credential.status()) || credential.expiresAt().isBefore(Instant.now())) {
            throw ApiException.conflict("CREDENTIAL_EXPIRED", "核销凭证已过期，请刷新后重试");
        }
        return Optional.of(credential);
    }

    /**
     * P1 清单#19：核销专用动态凭证解析——与 {@link #credentialByPayload} 不同，过窗口/已消费不在此抛异常，
     * 而是返回凭证本体，交由 {@code verifyEntitlement} 用 {@link #isCredentialWindowValid} 真实判定 window_valid，
     * 过窗口落 status=failed/result_reason=window_expired 可追溯。非动态凭证（不以 cred_ 开头）返回空。
     */
    private Optional<TicketCredentialRecord> credentialForVerification(String credentialPayload) {
        String payload = requireText(credentialPayload, "credential_payload");
        if (!payload.startsWith("cred_")) {
            return Optional.empty();
        }
        return Optional.of(repository.findTicketCredentialByToken(payload)
                .orElseThrow(() -> ApiException.notFound("CREDENTIAL_NOT_FOUND", "核销凭证不存在或已失效")));
    }

    /**
     * P1 清单#19：二维码刷新窗口真实判定。窗口 = [window_started_at, window_started_at + refresh_interval_seconds]；
     * 凭证须 active 且当前时间未越过窗口末端（含等于末端为有效）。越窗口（如离线截图重放旧 token）即 false。
     */
    private boolean isCredentialWindowValid(TicketCredentialRecord credential, Instant now) {
        if (!"active".equals(credential.status())) {
            return false;
        }
        Instant windowStart = credential.windowStartedAt() != null
                ? credential.windowStartedAt()
                : credential.issuedAt();
        Instant windowEnd = windowStart.plusSeconds(credential.refreshIntervalSeconds());
        return !now.isAfter(windowEnd);
    }

    /**
     * P0-3/X-01 校验动态凭证 HMAC 签名。返回 null=不适用/不强制（非动态凭证、存量未签名凭证、或 signer 未注入的兼容测试）；
     * 否则按签发时相同的规范串重算并常量时间比对，返回 true/false。返回 false 时核销端落 failed/signature_invalid 拒绝。
     */
    private Boolean credentialSignatureValid(TicketCredentialRecord credential, String presentedToken) {
        if (credential == null || credentialSigner == null) {
            return null;
        }
        String signature = credential.signatureValue();
        if (signature == null || signature.isBlank()) {
            // B2⑦ 门票 HMAC 对存量码强制：signer 已注入=生产签发一律带防伪签名；动态凭证却无签名
            // =存量未签名码（早已过秒级窗口）或被剥离签名的伪造码 → 拒绝核销（不再放行向后兼容）。
            return Boolean.FALSE;
        }
        // 数据库只保存token散列；用已匹配该散列的本次出示原文验签，不恢复持久化明文。
        String canonical = credentialSigner.canonical(
                presentedToken,
                credential.entitlementId(),
                credential.nonce(),
                credential.windowStartedAt(),
                credential.expiresAt());
        return credentialSigner.matches(canonical, signature);
    }

    private String verificationPayload(String credentialPayload, TicketCredentialRecord credential) {
        String payload = requireText(credentialPayload, "credential_payload");
        if (credential == null) {
            return payload;
        }
        return "sha256:" + hash(payload);
    }

    private Map<String, String> sanitizedSyncPayload(Map<String, String> payload) {
        return TerminalSyncPayloadSanitizer.sanitize(payload, this::hash);
    }

    private TicketCredentialResponse toCredentialResponse(TicketCredentialRecord credential) {
        return new TicketCredentialResponse(
                credential.id(),
                credential.entitlementId(),
                credential.orderId(),
                credential.credentialToken(),
                credential.credentialType(),
                credential.issuedAt(),
                credential.expiresAt(),
                credential.refreshedCount()
        );
    }

    private String firstPayload(Map<String, String> payload, String... keys) {
        String value = firstPayloadOptional(payload, keys);
        return requireText(value, keys[0]);
    }

    private String firstPayloadOptional(Map<String, String> payload, String... keys) {
        for (String key : keys) {
            String value = payload.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("credential hashing unavailable", exception);
        }
    }
}
