package com.complex.platform.ticketing;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class TicketVerificationService {

    // R5 T15：赠饮权益类型——核销成功后触发 ticket.gift_verified，由 bar 域生成 0 元单并扣库存。
    private static final String GIFT_DRINK_ENTITLEMENT_TYPE = "gift_drink";

    // P1 清单#19：票种未配置 refresh_interval_seconds 时的回退默认刷新窗口（秒），与 V064 默认对齐。
    private static final int DEFAULT_REFRESH_INTERVAL_SECONDS = 60;

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

    @org.springframework.beans.factory.annotation.Autowired
    public TicketVerificationService(
            TicketingRepository repository,
            ApplicationEventPublisher eventPublisher,
            MdmService mdmService,
            AuthorizationService authorizationService,
            TicketCredentialSigner credentialSigner) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.mdmService = mdmService;
        this.authorizationService = authorizationService;
        this.credentialSigner = credentialSigner;
    }

    /**
     * 向后兼容构造：未注入 MdmService/AuthorizationService/TicketCredentialSigner 时相应门控/签名为关（=null），
     * 核销链路按原行为执行。供既有单测构造（不改既有用例）。生产环境由 Spring 走上方主构造注入。
     */
    public TicketVerificationService(
            TicketingRepository repository,
            ApplicationEventPublisher eventPublisher,
            MdmService mdmService,
            AuthorizationService authorizationService) {
        this(repository, eventPublisher, mdmService, authorizationService, null);
    }

    public TicketVerificationService(
            TicketingRepository repository, ApplicationEventPublisher eventPublisher, MdmService mdmService) {
        this(repository, eventPublisher, mdmService, null, null);
    }

    public TicketVerificationService(TicketingRepository repository, ApplicationEventPublisher eventPublisher) {
        this(repository, eventPublisher, null, null, null);
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
                        "CROSS_MERCHANT_FORBIDDEN", "access to another merchant's entitlement is not allowed");
            }
        });
    }

    TicketCredentialResponse refreshCredential(String ticketId, String customerId) {
        IssuedEntitlementRecord entitlement = repository.findIssuedEntitlementById(requireText(ticketId, "ticket_id"))
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "entitlement not found"));
        if (customerId != null) {
            TicketOrderRecord order = repository.findTicketOrderById(entitlement.orderId())
                    .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "ticket order not found"));
            if (!customerId.equals(order.customerId())) {
                throw ApiException.forbidden("CUSTOMER_ORDER_FORBIDDEN", "ticket does not belong to customer");
            }
        } else {
            // P0 越权修复：员工侧（customerId==null）此前完全跳过归属校验，可对任意权益 id 签发动态核销凭证（跨租户 IDOR）。
            // 改为按调用者商户范围校验权益归属。
            enforceMerchantScope(entitlement.merchantId());
        }
        Instant now = Instant.now();
        String token = "cred_" + UUID.randomUUID().toString().replace("-", "");
        String nonce = UUID.randomUUID().toString().replace("-", "");
        // P1 清单#19：刷新窗口接票种级 refresh_interval_seconds（{30,60}），替换硬编码；
        // 票种未配置（为空）则回退默认 60 秒（向后兼容）。window_started_at=now，
        // 截图离线展示的旧 token 超过本窗口即过期被拒。
        int refreshIntervalSeconds = resolveRefreshIntervalSeconds(entitlement);
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
        return toCredentialResponse(saved);
    }

    VerificationPreviewResponse previewVerification(VerificationRequest request) {
        IssuedEntitlementRecord entitlement = entitlementByCredential(request);
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
        return verifyEntitlement(request, "android_terminal_online", null);
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
     * C1/REQ-05：人工放行（manual override）。主管对核销做人工放行决策，留痕为 status=manual_overridden 的核销记录，
     * 对齐权威状态/事件 verification.manual_overridden。放行不自动扣减权益（是否扣减为后续策略）；权限在端点层按主管权限码控制。
     */
    VerificationResponse manualOverride(VerificationRequest request, String reason) {
        TicketCredentialRecord credential = credentialByPayload(request == null ? null : request.credentialPayload()).orElse(null);
        IssuedEntitlementRecord entitlement = credential == null
                ? entitlementByCredential(request)
                : repository.findIssuedEntitlementById(credential.entitlementId())
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "entitlement not found"));
        // P0-2 越权修复：人工放行同样按调用者商户范围校验权益归属，禁止对他商户权益创建放行记录（平台 scope 放行）。
        enforceMerchantScope(entitlement.merchantId());
        VerificationRecord verification = repository.saveVerification(new VerificationRecord(
                "ver_" + UUID.randomUUID().toString().replace("-", ""),
                "V" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT),
                entitlement.id(),
                entitlement.entitlementCode(),
                requireText(request.credentialType(), "credential_type"),
                verificationPayload(request.credentialPayload(), credential),
                requireText(request.deviceId(), "device_id"),
                requireText(request.pointId(), "point_id"),
                "manual_overridden",
                reason == null || reason.isBlank() ? "manual override" : reason.trim(),
                "manual_override",
                null,
                Instant.now(),
                credential == null ? null : credential.nonce(),
                null,
                entitlement.merchantId(),
                null,
                null,
                entitlement.numPeople(),
                null,
                null,
                null
        ));
        return new VerificationResponse(verification.id(), verification.verificationNo(),
                verification.status(), verification.resultReason());
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
                .orElseThrow(() -> ApiException.notFound("VERIFICATION_NOT_FOUND", "verification not found"));
        // P0 越权修复：此前冲正全程不比对核销记录归属，可冲正他商户核销并回补其赠饮库存（跨租户 IDOR）。
        enforceMerchantScope(original.merchantId());
        if (!"success".equals(original.status())) {
            throw ApiException.conflict("VERIFICATION_NOT_REVERSIBLE", "only successful verification can be reversed");
        }
        if (original.reversedAt() != null) {
            throw ApiException.conflict("VERIFICATION_ALREADY_REVERSED", "verification has already been reversed");
        }
        Instant now = Instant.now();
        // F-TXN-04 并发双花修复：先以"原 success 记录 → reversed"的状态 CAS 作为并发唯一闸（markVerificationReversed
        // 仅当 status='success' 且 reversed_at IS NULL 才翻转并返回 1）。并发两路冲正只一路得 1（胜出），另一路得 0 →
        // 抛 ALREADY_REVERSED 立即返回、且【尚未】回滚 used_times，杜绝同一核销被回滚两次（多次票权益虚增）。
        // 必须置于 atomicReverseEntitlement 之前：此前先回滚再翻转，存在并发双方都已回滚 used_times 才到状态翻转的窗口。
        int reversedRows = repository.markVerificationReversed(original.id(), reversedBy, now);
        if (reversedRows == 0) {
            throw ApiException.conflict("VERIFICATION_ALREADY_REVERSED", "verification has already been reversed");
        }
        // 闸已得手（本路独占该核销的冲正权）→ 回滚权益核销次数（used_times-1，曾用满恢复 active）。
        // 原记录此前核销失败则无可回滚 → 拒绝（事务回滚，状态翻转一并撤销）。
        repository.atomicReverseEntitlement(original.entitlementId(), now)
                .orElseThrow(() -> ApiException.conflict("ENTITLEMENT_REVERSE_FAILED", "no used times to reverse for entitlement"));
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
                "verification_reverse",
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
                now
        ));
        // R5 T15 赠饮库存回补联动：若原核销系赠饮权益，发 bar.gift_reversed → bar 域将赠饮单置 reversed 并回补已扣库存。
        // 以原核销 verification_id 关联 bar_gift_redemptions（唯一键），未触发过赠饮兑换则消费者幂等跳过。
        IssuedEntitlementRecord reversedEntitlement = repository.findIssuedEntitlementById(original.entitlementId()).orElse(null);
        boolean giftDrink = reversedEntitlement != null
                && GIFT_DRINK_ENTITLEMENT_TYPE.equals(reversedEntitlement.entitlementType());
        if (giftDrink) {
            eventPublisher.publishEvent(new GiftReversedEvent(original.id()));
        }
        // 发 VerificationReversedEvent，作为 ticketing 域"核销已冲正"权威信号。现有消费者（同步、同事务）：
        //   - customer 域 LoyaltyPointsConsumer（F-TXN-03 裁决#3）：全额冲减该订单已 confirmed 积分（与"核销→confirm"对称，
        //     冲正专用幂等键 (ticket_verification_reversal, verificationId)，不与退款 deduct 冲突）；
        //   - finance 域 VerificationReversedConsumer（P1#19）：落"冲正已通知"审计留痕（无独立财务冲销对象，仅留痕）。
        // 消费者均按各自 source_doc 幂等；下游异常将随同事务回滚本次冲正（与既有 confirm 链同语义）。
        eventPublisher.publishEvent(new VerificationReversedEvent(
                original.id(),
                reversal.id(),
                original.entitlementId(),
                reversedEntitlement == null ? null : reversedEntitlement.orderId(),
                original.merchantId(),
                original.verifiedPeople()
        ));
        // F-TXN-03 按核销张数：冲正回滚积分份额——以冲正后该订单最新 Σused/Σtotal 发积分确认事件，
        // customer 域据此把已确认份额对齐更低的 target（冲正一张即按份回滚 1/Σtotal），取代原"冲正全额 deduct"。
        String reversedOrderId = reversedEntitlement == null ? null : reversedEntitlement.orderId();
        if (reversedOrderId != null && !reversedOrderId.isBlank()) {
            long[] units = sumOrderUnits(reversedOrderId);
            eventPublisher.publishEvent(new PointsConfirmRequestedEvent(reversedOrderId, units[0], units[1]));
        }
        return new VerificationResponse(reversal.id(), reversal.verificationNo(), reversal.status(), reversal.resultReason());
    }

    /**
     * F-TXN-03 按核销张数：聚合该订单全部已发权益的 Σused_times / Σtotal_times（张=use），
     * 作为积分确认事件的 verifiedUnits / totalUnits（核销/冲正后读到的最新值；customer 域据此对齐确认份额）。
     */
    private long[] sumOrderUnits(String orderId) {
        long used = 0L;
        long total = 0L;
        for (IssuedEntitlementRecord e : repository.listIssuedEntitlementsByOrderId(orderId)) {
            used += e.usedTimes();
            total += e.totalTimes();
        }
        return new long[]{used, total};
    }

    private VerificationResponse verifyEntitlement(VerificationRequest request, String sourceType, String clientTaskId) {
        // P1 清单#19：动态二维码核销取凭证不在此处因过窗口抛异常，改由下方按 window_started_at+refresh_interval
        // 真实判定刷新窗口，过窗口落 status=failed/result_reason=window_expired、window_valid=false（截图重放被拒、可追溯）。
        TicketCredentialRecord credential = credentialForVerification(request == null ? null : request.credentialPayload()).orElse(null);
        // P1 清单#19 防重放：已成功核销占用（status!=active，如 consumed）的动态凭证重放必须仍被拒（抛异常），
        // 保持既有「nonce/凭证已用」契约（CREDENTIAL_EXPIRED / "credential is expired"）。此处先于 window 判定拦截，
        // 避免重放被误并入「过刷新窗口」分支而静默落 failed。过窗口（active 但越窗）才记 failed/window_expired。
        if (credential != null && !"active".equals(credential.status())) {
            throw ApiException.conflict("CREDENTIAL_EXPIRED", "credential is expired");
        }
        IssuedEntitlementRecord entitlement = credential == null
                ? entitlementByCredential(request)
                : repository.findIssuedEntitlementById(credential.entitlementId())
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "entitlement not found"));
        // P0-2 越权修复：在线核销主链此前未做商户归属校验，A 商户核销端可凭票号/权益码核销 B 商户客人的票
        // （跨租户烧票 IDOR）。消费权益前按调用者商户范围校验归属（与 refresh/reverse 同范式；平台 scope 放行）。
        enforceMerchantScope(entitlement.merchantId());
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
                    null
            ));
            return new VerificationResponse(expired.id(), expired.verificationNo(), expired.status(), expired.resultReason());
        }
        // P0-3/X-01 二维码防伪签名校验：动态凭证若带签名（signer 已注入签发），核销端按相同规范串重算并常量时间比对。
        // 失配=离线截图重放/凭证字段篡改（如延长有效期）→ 不消费权益，落一条 failed/signature_invalid 核销记录（可追溯）。
        // 非动态凭证（credential==null，直接凭权益码）/存量未签名凭证（signature_value 空、秒级窗口自然淘汰）/
        // signer 未注入（兼容测试）→ signature_valid=null，不拒绝（向后兼容）。
        Boolean signatureValid = credentialSignatureValid(credential);
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
            throw ApiException.conflict("ENTITLEMENT_NOT_ACTIVE", "entitlement is not active");
        }
        if (entitlement.validFrom() != null && now.isBefore(entitlement.validFrom())) {
            throw ApiException.conflict("ENTITLEMENT_NOT_YET_VALID", "entitlement is not yet valid");
        }
        if (entitlement.validTo() != null && now.isAfter(entitlement.validTo())) {
            throw ApiException.conflict("ENTITLEMENT_EXPIRED", "entitlement has expired");
        }
        IssuedEntitlementRecord updated = repository.atomicUseEntitlement(entitlement.id(), now)
                .orElseThrow(() -> ApiException.conflict("ENTITLEMENT_USED_UP", "entitlement has no remaining times"));
        // R5 T5 多人票：本次核销人数 = 票面 num_people（同场次集中核销），不可超 num_people。
        int verifiedPeople = updated.numPeople();
        if (verifiedPeople > updated.numPeople()) {
            throw ApiException.conflict("VERIFIED_PEOPLE_EXCEEDED", "verified people exceeds ticket num_people");
        }
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
                null
        ));
        // P0 清单#2 / F-TXN-03 按核销张数：核销成功 → 发积分确认事件，带该订单最新 Σused/Σtotal；
        // customer 域据此把已确认份额对齐 floor(总额 × Σused / Σtotal)（多次/多人票部分核销按张数比例确认）。
        if (updated.orderId() != null && !updated.orderId().isBlank()) {
            long[] units = sumOrderUnits(updated.orderId());
            eventPublisher.publishEvent(new PointsConfirmRequestedEvent(updated.orderId(), units[0], units[1]));
        }
        if (credential != null) {
            repository.saveTicketCredential(new TicketCredentialRecord(
                    credential.id(),
                    credential.entitlementId(),
                    credential.orderId(),
                    credential.credentialToken(),
                    credential.nonce(),
                    credential.credentialHash(),
                    credential.credentialType(),
                    credential.issuedAt(),
                    credential.expiresAt(),
                    "consumed",
                    credential.refreshedCount(),
                    credential.createdAt(),
                    now,
                    credential.refreshIntervalSeconds(),
                    credential.windowStartedAt(),
                    credential.signatureValue(),
                    credential.signatureVersion()
            ));
        }
        // R5 T15：赠饮权益核销成功 → 发同步事件 ticket.gift_verified，bar 域据此生成 0 元单并扣库存。
        // 同步发布以便测试与同事务；不与本核销主链耦合（主链已照常完成，下游异常不回滚核销由消费者自行兜底）。
        if (GIFT_DRINK_ENTITLEMENT_TYPE.equals(updated.entitlementType())) {
            // R5 已落地#2：赠饮→吧台 SKU 映射来源——权益模板(entitlement_template).bar_item_ref。
            // 已配置则下游对该 item 走 redeem_consume 扣减；为空（未配置）则下游跳过扣减并标注（向后兼容）。
            String barItemRef = repository.findEntitlementTemplateById(updated.entitlementTemplateId())
                    .map(EntitlementTemplateRecord::barItemRef)
                    .orElse(null);
            eventPublisher.publishEvent(new GiftVerifiedEvent(
                    updated.id(),
                    verification.id(),
                    barItemRef,
                    updated.pointId(),
                    null,
                    updated.merchantId()
            ));
        }
        return new VerificationResponse(verification.id(), verification.verificationNo(), verification.status(), verification.resultReason());
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

        if ("offline_verification".equals(taskType)) {
            String entitlementCode = firstPayload(payload, "entitlementCode", "entitlement_code", "credentialPayload", "credential_payload");
            String pointId = firstPayloadOptional(payload, "pointId", "point_id", "gateId", "gate_id", "point");
            VerificationResponse verification = verifyEntitlement(
                    new VerificationRequest(
                            "sync-task-" + clientTaskId,
                            "qr",
                            entitlementCode,
                            deviceCode,
                            pointId == null ? deviceCode : pointId,
                            Map.of("source", "terminal_sync_task")
                    ),
                    "android_terminal_offline",
                    clientTaskId
            );
            verificationId = verification.id();
            verificationNo = verification.verificationNo();
        }

        TerminalSyncTaskRecord saved = repository.saveTerminalSyncTask(new TerminalSyncTaskRecord(
                "sync_" + UUID.randomUUID().toString().replace("-", ""),
                deviceCode,
                staffId,
                clientTaskId,
                taskType,
                "accepted",
                null,
                verificationId,
                verificationNo,
                occurredAt,
                sanitizedSyncPayload(payload),
                Instant.now()
        ));
        return toSyncTaskResult(saved);
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
                    .orElseThrow(() -> ApiException.notFound("CREDENTIAL_NOT_FOUND", "credential not found"));
            return repository.findIssuedEntitlementById(credential.entitlementId())
                    .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "entitlement not found"));
        }
        return repository.findIssuedEntitlementByCode(entitlementCode)
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "entitlement not found"));
    }

    private Optional<TicketCredentialRecord> credentialByPayload(String credentialPayload) {
        String payload = requireText(credentialPayload, "credential_payload");
        if (!payload.startsWith("cred_")) {
            return Optional.empty();
        }
        TicketCredentialRecord credential = repository.findTicketCredentialByToken(payload)
                .orElseThrow(() -> ApiException.notFound("CREDENTIAL_NOT_FOUND", "credential not found"));
        if (!"active".equals(credential.status()) || credential.expiresAt().isBefore(Instant.now())) {
            throw ApiException.conflict("CREDENTIAL_EXPIRED", "credential is expired");
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
                .orElseThrow(() -> ApiException.notFound("CREDENTIAL_NOT_FOUND", "credential not found")));
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
    private Boolean credentialSignatureValid(TicketCredentialRecord credential) {
        if (credential == null || credentialSigner == null) {
            return null;
        }
        String signature = credential.signatureValue();
        if (signature == null || signature.isBlank()) {
            return null;
        }
        String canonical = credentialSigner.canonical(
                credential.credentialToken(),
                credential.entitlementId(),
                credential.nonce(),
                credential.windowStartedAt(),
                credential.expiresAt());
        return credentialSigner.matches(canonical, signature);
    }

    /**
     * P1 清单#19：发凭证时按票种级 refresh_interval_seconds 取刷新窗口（{30,60}），票种未配置则回退默认 60 秒。
     * 票种取自权益的 ticket_type_id。
     */
    private int resolveRefreshIntervalSeconds(IssuedEntitlementRecord entitlement) {
        return repository.findTicketTypeById(entitlement.ticketTypeId())
                .map(TicketTypeRecord::refreshIntervalSeconds)
                .orElse(DEFAULT_REFRESH_INTERVAL_SECONDS);
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

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
