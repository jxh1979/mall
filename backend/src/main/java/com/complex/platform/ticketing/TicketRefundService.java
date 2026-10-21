package com.complex.platform.ticketing;

// 文件版本：V0.03

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.RefundAuthorizationService;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketRefundRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import static com.complex.platform.common.Strings.requireText;

/**
 * 方案2 整票退款（AUD-006）+ 规则2 可退配置。退款单位 = 父票实例（ticket_instances），不再受理单条权益/自由金额：
 * <ul>
 *   <li>金额由服务端按每票出票时定格的实付分摊额算定（复现方案1 出票均分），客户端不定价；</li>
 *   <li>票种 refundable=false → 拒申请退（默认不可退）；</li>
 *   <li>整票连坐：父票任一权益已核销/已冻结 → 整张不可退；</li>
 *   <li>申请即冻结：父票 active→refund_pending，复核驳回恢复 active；</li>
 *   <li>限量票名额回补：退成功的父票实例数即释放同数可售名额（退一张放一个）。</li>
 * </ul>
 */
@Service
public class TicketRefundService {

    // P0 清单#2：退款比例 basis points 分母（10000=100%）。
    private static final long BASIS_POINTS_DENOMINATOR = 10_000L;

    /** TK-05（BSM:804）：五种情形之一（票种规则明确允许、活动或场次取消、平台或履约方无法提供服务、重复扣款、法律法规要求）。 */
    static final String REASON_TICKET_TYPE_RULE = "ticket_type_rule";
    /** 五种情形之外：只能由持授权 U 盘密钥的最高权限逐笔放行（仍限完全未核销）。 */
    static final String REASON_MASTER_OVERRIDE = "master_override";
    /** 通票安全例外：只经安全事件审批入口生成（TicketSafetyExceptionService）。 */
    static final String REASON_SAFETY_EXCEPTION = "safety_exception";
    private static final Set<String> ORDINARY_REASON_CODES = Set.of(REASON_TICKET_TYPE_RULE, "event_cancelled",
            "service_unavailable", "duplicate_charge", "legal_requirement", REASON_MASTER_OVERRIDE);

    private final TicketingRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    // F-TXN-01 退款跨商户越权修复：退款须按调用者商户范围校验原票/订单归属。可为 null=旧构造/兼容测试路径
    // （不强制）；生产由 Spring 走主构造注入。平台管理员 scope 为空=放行，商户用户必须匹配原单 merchant_id。
    private final AuthorizationService authorizationService;
    // Stage B（审计 A2）：退款分级授权引擎（finance 域 public 服务）。可为 null=旧构造/兼容测试；生产由 Spring 注入。
    private final RefundAuthorizationService refundAuthorizationService;

    // 功能标志 refund.tiered_authorization.enabled，默认关。开启后退款走分级授权（额度内自助自动退 / 超额留二人复核 + SoD、
    // 真实发起人/审批人留痕）；关闭则维持现状退款行为（审批人记字面量 "staff"、无 SoD、无自助自动）。
    // 用字段注入（非构造）以免改动既有多处构造点；包私有便于单测直接置位（仿 AmusementService.enforceDeviceRegistry）。
    @org.springframework.beans.factory.annotation.Value("${refund.tiered_authorization.enabled:false}")
    boolean tieredRefundEnabled;

    // TK-20-R3（CG-06a 第 12 条「票务……消费获准退款均回原卡」）：储值支付订单获准退款时回冲原消费卡。字段注入 + @Lazy 免构造环
    // （customer.StoredValueService 已依赖 ticketing）；可为 null = 旧构造/兼容测试（不回卡，沿既有拨付路径）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    com.complex.platform.customer.StoredValueService storedValueService;

    @org.springframework.beans.factory.annotation.Autowired
    public TicketRefundService(
            TicketingRepository repository,
            ApplicationEventPublisher eventPublisher,
            AuthorizationService authorizationService,
            RefundAuthorizationService refundAuthorizationService) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.authorizationService = authorizationService;
        this.refundAuthorizationService = refundAuthorizationService;
    }

    /** 当前操作者 id；无 AuthorizationService（旧构造/兼容测试）时返回 null（→ classify 不享额度自助、落复核最安全）。 */
    private String currentActorId() {
        return authorizationService == null ? null : authorizationService.currentActor().id();
    }

    /**
     * P0 越权修复统一范式（对齐 TicketVerificationService.enforceMerchantScope）：取出原单后按调用者商户范围校验归属。
     * 平台管理员 scope 为空 → 放行；商户用户 → 必须等于原单 merchant_id，否则 CROSS_MERCHANT_FORBIDDEN。
     * authorizationService 为 null（旧构造/兼容测试）时不强制。
     */
    private void enforceMerchantScope(String merchantId) {
        if (authorizationService == null) {
            return;
        }
        // TK-05-R1：持授权 U 盘密钥的最高权限逐笔放行须能审批任一商户的退款（最高权限账号不挂商户）。
        if (authorizationService.isMasterAuthority(authorizationService.currentActor())) {
            return;
        }
        authorizationService.currentMerchantScope().ifPresent(scope -> {
            if (!scope.equals(merchantId)) {
                throw ApiException.forbidden(
                        "CROSS_MERCHANT_FORBIDDEN", "无权查看其他商户的票务订单");
            }
        });
    }

    TicketRefundRecord requestRefund(TicketRefundRequest request) {
        TicketRefundRequest actual = requireRequest(request);
        String targetId = requireText(actual.targetId(), "target_id");
        TicketOrderRecord order = repository.findTicketOrderById(targetId)
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "票务订单不存在或已被删除"));
        // F-TXN-01 越权修复：发起退款前按调用者商户范围校验原单归属，防商户A退商户B的票（跨租户 IDOR）。
        enforceMerchantScope(order.merchantId());
        // 仅已支付订单可退款：堵"未支付/已取消订单凭空发起退款"（无真实收款却触发退款冻结/积分冲减链）。
        if (!"paid".equals(order.payStatus())) {
            throw ApiException.conflict("TICKET_ORDER_NOT_REFUNDABLE", "只有已支付的票务订单才能退款");
        }
        // AUD-006 退款单位=整张票（无兼容期）：旧入参 entitlement_ids 传入非空即拒。
        if (actual.entitlementIds() != null && !actual.entitlementIds().isEmpty()) {
            throw ApiException.badRequest("REFUND_UNIT_IS_TICKET",
                    "退款以整张票为单位，请按票实例选择，不能按权益选择");
        }
        // TK-05-R1（BSM:804 五种情形 + 最高权限逐笔放行；BSM:785 票种开关只管「票种规则明确允许」）：理由码闭集必填，
        // 通票安全例外不走本入口；票种开关只约束 ticket_type_rule，其余理由不看开关；每笔退款都须写明责任主体（BSM:806）。
        String reasonCode = trimToDefault(actual.reasonCode(), "");
        if (!ORDINARY_REASON_CODES.contains(reasonCode)) {
            throw ApiException.badRequest("REFUND_REASON_CODE_REQUIRED",
                    "退款理由须为五种情形之一（票种规则明确允许、活动或场次取消、平台或履约方无法提供服务、重复扣款、法律法规要求）"
                            + "或最高权限逐笔放行；通票安全例外请走安全事件审批");
        }
        TicketTypeRecord ticketType = repository.findTicketTypeById(order.ticketTypeId()).orElse(null);
        if (REASON_TICKET_TYPE_RULE.equals(reasonCode) && (ticketType == null || !ticketType.refundable())) {
            throw ApiException.conflict("TICKET_TYPE_NOT_REFUNDABLE",
                    "该票种不支持退款");
        }
        String responsibleEntityId = trimToDefault(actual.responsibleEntityId(), "");
        if (responsibleEntityId.isEmpty() || !repository.isActiveBusinessEntity(responsibleEntityId)) {
            throw ApiException.badRequest("REFUND_RESPONSIBLE_ENTITY_REQUIRED",
                    "请填写退款责任主体（主体主数据中在用的法律主体）");
        }
        List<TicketInstanceRecord> allInstances = repository.listTicketInstancesByOrderId(targetId);
        if (allInstances.isEmpty()) {
            throw ApiException.conflict("TICKET_ORDER_NOT_REFUNDABLE",
                    "该票务订单没有可退款的票");
        }
        Map<String, Long> shareByInstance = perInstanceShares(order, allInstances);
        Map<String, List<IssuedEntitlementRecord>> entByInstance =
                groupByParent(repository.listIssuedEntitlementsByOrderId(targetId));
        // 解析要退的父票实例：不传/空=该单全部"完全未核销"的实例；传=校验属本单、去重、整票连坐可退。
        List<TicketInstanceRecord> targets =
                resolveRefundInstances(allInstances, entByInstance, actual.ticketInstanceIds());
        if (targets.isEmpty()) {
            throw ApiException.conflict("TICKET_INSTANCE_VERIFIED_NO_REFUND",
                    "该订单没有完全未使用且可退款的票");
        }
        // 金额=服务端算定的所选实例分摊额合计；客户端不定价，若带 refund_amount_cent 必须相等。
        long amountCent = targets.stream().mapToLong(ti -> shareByInstance.getOrDefault(ti.id(), 0L)).sum();
        if (amountCent <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "所选票的可退金额为 0，请作废票而不是退款");
        }
        if (actual.refundAmountCent() != null && actual.refundAmountCent() != amountCent) {
            throw ApiException.badRequest("REFUND_AMOUNT_MISMATCH",
                    "退款金额必须等于系统计算的整票金额");
        }
        long alreadyRequested = repository.sumPendingRefundAmountByOrderId(targetId);
        if (alreadyRequested + amountCent > order.amountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "退款金额超过订单金额");
        }
        Instant now = Instant.now();
        // 申请即冻结：逐实例 CAS active→refund_pending；并发抢先者胜，命中 0 行=该票在途/已用/已退 → 整笔回滚拒。
        for (TicketInstanceRecord ti : targets) {
            if (repository.markTicketInstanceRefundPending(ti.id(), now) == 0) {
                throw ApiException.conflict("TICKET_INSTANCE_REFUND_IN_PROGRESS",
                        "票 " + ti.id() + " 当前状态不能冻结退款");
            }
        }
        String requestedBy = currentActorId();
        // Stage B 分级授权（功能标志 refund.tiered_authorization.enabled，默认关）：判定本笔退款能否当场自助完成。
        //   额度内/规则既定 → 发起人当场自助自动退；超额/无额度 → 留待二人复核。关闭/未注入 → 一律 requested 等人工审批。
        // AUD-008：分级授权按综合体隔离——projectId 取操作者授权 scope（票单不自带 project；scope 由安全 token 得出，
        //   非请求体），无 scope（平台管理员/无组织）→ null=仅命中全局配置，绝不跨综合体串额度。
        String refundProjectId = authorizationService == null
                ? null : authorizationService.currentProjectScopeQuietly().orElse(null);
        // 最高权限逐笔放行：不享额度内自助，必须由最高权限审批。
        boolean selfComplete = !REASON_MASTER_OVERRIDE.equals(reasonCode) && tieredRefundEnabled && refundAuthorizationService != null
                && refundAuthorizationService.classify("ticket", amountCent, requestedBy, null, refundProjectId)
                        != RefundAuthorizationService.RefundTier.REVIEW_REQUIRED;
        TicketRefundRecord refund = repository.saveTicketRefund(new TicketRefundRecord(
                "ticket_refund_" + slug(actual.idempotencyKey()),
                targetId,
                amountCent,
                requireText(actual.reason(), "reason"),
                trimToDefault(actual.refundChannel(), "original"),
                selfComplete ? "approved" : "requested",
                requestedBy,
                selfComplete ? requestedBy : null,   // 额度内自助：审批人=发起人本人（非 SoD 场景，SoD 仅约束复核档）
                selfComplete ? now : null,
                now,
                now,
                order.merchantId(),   // V220：退款受理商户=原销售订单 merchant_id（平台直营单为 null）
                reasonCode,
                responsibleEntityId,
                ticketType != null && ticketType.refundable()
        ));
        createRefundLines(refund, order, targets, entByInstance, shareByInstance, now);
        if (selfComplete) {
            return finalizeApprovedRefund(refund, now, false); // 额度内自助：停在 approved，等财务确认拨付
        }
        return refund;
    }

    TicketRefundRecord approveRefund(String refundId, StatusChangeRequest request) {
        StatusChangeRequest actual = requireRequest(request);
        requireText(actual.reason(), "reason");
        TicketRefundRecord current = repository.findTicketRefundById(requireText(refundId, "refund_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_REFUND_NOT_FOUND", "票务退款不存在或已被删除"));
        // F-TXN-01 越权修复：审批退款（冻结权益/积分冲减/库存回补皆为写）前，按调用者商户范围校验原单归属，
        // 防商户A审批通过商户B的退款。原单缺失时不做归属判定（交由下方既有 null 容错路径）。
        repository.findTicketOrderById(current.targetId())
                .ifPresent(order -> enforceMerchantScope(order.merchantId()));
        // AUD-007 职责分离（SoD）+ 真实审批人留痕：不受功能标志控制——一律以真实操作者为审批人、且审批人不得为发起人
        //   （approver≠requestedBy，SELF_APPROVAL_FORBIDDEN）；不再退化为字面量 "staff"（那样自审自批且不可归责）。
        String approver = currentActorId();
        if (approver != null && approver.equals(current.requestedBy())) {
            throw ApiException.forbidden("SELF_APPROVAL_FORBIDDEN",
                    "退款申请人不能自行审批，须由其他审批人处理");
        }
        String approvedBy = approver;
        // TK-05-R1（BSM:804「五种情形之外……只能由持授权 U 盘密钥的最高权限逐笔放行，每次只对当前这一笔生效」）。
        if (REASON_MASTER_OVERRIDE.equals(current.reasonCode()) && (authorizationService == null
                || !authorizationService.isMasterAuthority(authorizationService.currentActor()))) {
            throw ApiException.forbidden("MASTER_AUTHORITY_REQUIRED",
                    "五种情形之外的退款只能由持授权 U 盘密钥的最高权限逐笔放行");
        }
        Instant now = Instant.now();
        // F-TXN-04 并发双拨付防护：退款状态翻转改用 CAS（WHERE status='requested'），并发只一路过闸。
        int approvedRows = repository.markTicketRefundApproved(current.id(), approvedBy, now);
        if (approvedRows == 0) {
            throw ApiException.conflict(
                    "TICKET_REFUND_NOT_APPROVABLE", "票务退款当前状态不可批准");
        }
        TicketRefundRecord approved = current.withStatus("approved", approvedBy, now, now);
        return finalizeApprovedRefund(approved, now, false); // 审批通过=已发起兑付，停在 approved；财务打款后 confirm-payout 才 success
    }

    /**
     * 方案2 复核驳回：退款单 requested→rejected + 行 rejected + 父票 refund_pending→active 解冻（恢复可用）。
     * SoD 同 approve（发起人不得驳回自己，防自发自撤洗留痕）；不打款、不冲积分、不回补库存。
     */
    TicketRefundRecord rejectRefund(String refundId, StatusChangeRequest request) {
        StatusChangeRequest actual = requireRequest(request);
        requireText(actual.reason(), "reason");
        TicketRefundRecord current = repository.findTicketRefundById(requireText(refundId, "refund_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_REFUND_NOT_FOUND", "票务退款不存在或已被删除"));
        repository.findTicketOrderById(current.targetId())
                .ifPresent(order -> enforceMerchantScope(order.merchantId()));
        // AUD-007 职责分离（SoD）+ 真实复核人留痕：不受功能标志控制——一律真实操作者、复核人不得为发起人；不退化为 "staff"。
        String reviewer = currentActorId();
        if (reviewer != null && reviewer.equals(current.requestedBy())) {
            throw ApiException.forbidden("SELF_APPROVAL_FORBIDDEN",
                    "退款申请人不能自行驳回，须由其他审核人处理");
        }
        String decidedBy = reviewer;
        Instant now = Instant.now();
        int rejectedRows = repository.markTicketRefundRejected(current.id(), decidedBy, now);
        if (rejectedRows == 0) {
            throw ApiException.conflict(
                    "TICKET_REFUND_NOT_REJECTABLE", "票务退款当前状态不可驳回");
        }
        // 行 requested→rejected + 收集本单实例解冻。
        Map<String, String> entToParent = entToParentMap(current.targetId());
        LinkedHashSet<String> instanceIds = new LinkedHashSet<>();
        repository.listTicketRefundLines(current.id()).forEach(line -> {
            repository.saveTicketRefundLine(new TicketRefundLineRecord(
                    line.id(), line.refundId(), line.orderId(), line.entitlementId(), line.entitlementType(),
                    line.lineType(), line.refundAmountCent(), line.remainingTimesBefore(), line.usedTimesBefore(),
                    "rejected", line.createdAt(), now));
            if (line.entitlementId() != null) {
                String parent = entToParent.get(line.entitlementId());
                if (parent != null) {
                    instanceIds.add(parent);
                }
            }
        });
        for (String instanceId : instanceIds) {
            repository.restoreTicketInstanceActive(instanceId, now);
        }
        return current.withStatus("rejected", decidedBy, now, now);
    }

    /**
     * 财务实际打款后确认拨付：approved→success（终态）。依据 business-enum-quick-reference-v0.1.html
     * 「退款单状态」行：退款以“成功”为最终完成态，只有“成功”才计入“已退金额”；以及
     * business-system-map-for-operations-v0.1.html「退款授权与留痕」行：系统完整记录“原路退回结果”
     * （本方法的 reason 写入审计事件 ticket_refund.payout_confirmed 即为该结果留痕）。
     * markTicketRefundSucceeded 本身是 WHERE status='approved' 的 CAS，天然幂等；命中 0 行=当前状态不可确认。
     */
    TicketRefundRecord confirmPayout(String refundId, StatusChangeRequest request) {
        StatusChangeRequest actual = requireRequest(request);
        requireText(actual.reason(), "reason");
        TicketRefundRecord current = repository.findTicketRefundById(requireText(refundId, "refund_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_REFUND_NOT_FOUND", "票务退款不存在或已被删除"));
        repository.findTicketOrderById(current.targetId())
                .ifPresent(order -> enforceMerchantScope(order.merchantId()));
        Instant now = Instant.now();
        if (repository.markTicketRefundSucceeded(current.id(), now) == 0) {
            throw ApiException.conflict("TICKET_REFUND_NOT_PAYOUT_CONFIRMABLE",
                    "只有“已通过”的退款才能确认拨付，当前状态不可确认");
        }
        eventPublisher.publishEvent(new TicketRewardFacts.Refunded(current.id()));
        return current.withStatus("success", current.approvedBy(), current.approvedAt(), now);
    }

    /**
     * 退款审批生效后的统一兑付副作用（人工审批通过 / 额度内自助完成 共用）：
     * 退款行置 approved + 强 CAS 冻结对应权益（active∧used=0→refunded，失配整笔回滚）+ 父票 refund_pending→refunded
     * + 限量票按本笔退成功实例数回补名额（退一张放一个）+ 发打款事件。
     * safetyException=true（TK-05-R2 通票安全例外，已核销也退）：已用权益与核销记录不改不删、只冻结未用权益；
     * 多人票只退该人份时父票仍为 active（他人照常使用），不翻 refunded、不回补名额。
     */
    private TicketRefundRecord finalizeApprovedRefund(TicketRefundRecord approved, Instant now, boolean safetyException) {
        TicketOrderRecord order = repository.findTicketOrderById(approved.targetId()).orElse(null);
        Map<String, String> entToParent = entToParentMap(approved.targetId());
        LinkedHashSet<String> refundedInstances = new LinkedHashSet<>();
        repository.listTicketRefundLines(approved.id()).forEach(line -> {
            repository.saveTicketRefundLine(new TicketRefundLineRecord(
                    line.id(), line.refundId(), line.orderId(), line.entitlementId(), line.entitlementType(), line.lineType(),
                    line.refundAmountCent(), line.remainingTimesBefore(), line.usedTimesBefore(), "approved", line.createdAt(), now
            ));
            // 2a-3 退款冻结（强 CAS）：active∧used_times=0 → refunded。命中 0 行=申请→审批空窗内被核销 → 整笔回滚拒退。
            if (line.entitlementId() != null && !line.entitlementId().isBlank()) {
                int frozen = repository.freezeEntitlementForRefund(line.entitlementId(), now);
                if (frozen == 0 && !safetyException) {
                    throw ApiException.conflict("TICKET_INSTANCE_VERIFIED_NO_REFUND",
                            "退款申请后该票权益已被核销，退款已回退");
                }
                String parent = entToParent.get(line.entitlementId());
                if (parent != null) {
                    refundedInstances.add(parent);
                }
            }
        });
        // 父票 refund_pending→refunded（终态，CAS）。安全例外只把整张退的单人票翻转（多人票该人份退款时父票仍 active，命中 0 行）。
        int refundedCount = 0;
        for (String instanceId : refundedInstances) {
            refundedCount += repository.markTicketInstanceRefunded(instanceId, now);
        }
        int releasedCount = safetyException ? refundedCount : refundedInstances.size();
        // P0 清单#2：退款生效 → 按退款比例发积分冲减事件（到期自动退全未核销无已确认积分口径 → 不发）。
        // 审批不冲回奖励；只有实际退款成功处理对应原人次。
        if (order != null) {
            // 限量票名额回补：退一张放一个可售名额（退成功的父票实例数），与下单即扣对称；无限量票 GREATEST(0,..) 兜底无害。
            if (releasedCount > 0) {
                repository.releaseTicketStock(order.ticketTypeId(), releasedCount);
            }
            // TK-20-R3（CG-06a 第 12 条「票务……消费获准退款均回原卡，固定先回原消费尚未回冲的赠送金，再回本金；不扩大原退款资格」）：
            // 储值支付的订单（支付意图 channel=stored_value、id=原消费流水）获准即回冲原卡，回冲到账即为「原路退回结果」，
            // 退款单直接 approved→success，不再等财务 confirm-payout；资格/金额沿上面既有审批算定，不改。非储值订单路径不变。
            TicketPaymentIntentRecord storedValueIntent = storedValueService == null ? null
                    : repository.findSucceededTicketPaymentIntentByOrder(order.id())
                            .filter(intent -> "stored_value".equals(intent.channel())).orElse(null);
            if (storedValueIntent != null) {
                storedValueService.refundConsumption(storedValueIntent.id(), approved.refundAmountCent(),
                        "票务退款 " + approved.id(), "ticketrev_" + approved.id());
                repository.markTicketRefundSucceeded(approved.id(), now);
                eventPublisher.publishEvent(new TicketRewardFacts.Refunded(approved.id()));
                return approved.withStatus("success", approved.approvedBy(), approved.approvedAt(), now);
            }
            // 打款拨付信号：金额已落 ticket_refunds，此事件只承载实际打款，不重复入账。
            eventPublisher.publishEvent(new TicketRefundApprovedEvent(
                    order.id(), approved.id(), approved.refundAmountCent(), approved.refundChannel()));
        }
        // 业务权威口径（business-enum-quick-reference-v0.1.html「退款单状态」行）：退款以“成功”为最终完成态，
        //   只有“成功”的退款才计入“已退金额”；“已通过”只表示已批准并已发起不可逆兑付。
        //   故退款到此停在 approved（等兑付），由财务实际打完款后调 confirm-payout 确认才 approved→success。
        //   此前在本方法内联置 success：钱一分没出账，当天经营净收入就已被扣（财务/分析按 status='success' 扣减）。
        return approved;
    }

    /**
     * TK-05-R2 通票安全例外退款（BSM:804「其他非多次通票仍可免除其通票费用，即使已核销也予以退款」；2026-10-21 操作员裁决补充第 5 条
     * 「只退出事客人那一人份的票款（按该票人均分摊额），同票其他人照常使用」）：由安全事件审批通过后调用，直接生成已批准的退款单。
     * 金额＝该票出票分摊额按人数均分后该人份的份额（尾差沿 allocateEvenly，前几人份多 1 分）；单人票即整张。
     * 单人票整张冻结并退；多人票只退该人份权益（未用的冻结，已用的与核销记录保留），父票与他人份不动。
     */
    TicketRefundRecord createSafetyExceptionRefund(String refundId, TicketInstanceRecord ticket, int slotIndex,
            String requestedBy, String approvedBy, String reason, String responsibleEntityId, Instant now) {
        TicketOrderRecord order = repository.findTicketOrderById(ticket.orderId())
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "票务订单不存在或已被删除"));
        List<TicketInstanceRecord> allInstances = repository.listTicketInstancesByOrderId(order.id());
        long ticketShare = perInstanceShares(order, allInstances).getOrDefault(ticket.id(), 0L);
        int people = Math.max(1, ticket.numPeople());
        long amountCent = allocateEvenly(ticketShare, people)[slotIndex - 1];
        List<IssuedEntitlementRecord> slotEntitlements = groupByParent(repository.listIssuedEntitlementsByOrderId(order.id()))
                .getOrDefault(ticket.id(), List.of()).stream()
                .filter(e -> e.slotIndex() == slotIndex && e.compensationItemId() == null)
                .toList();
        if (amountCent <= 0 || slotEntitlements.isEmpty()) {
            throw ApiException.conflict("TICKET_SAFETY_REFUND_NOTHING", "该人份没有可退的票款");
        }
        if (people == 1 && repository.markTicketInstanceRefundPending(ticket.id(), now) == 0) {
            throw ApiException.conflict("TICKET_INSTANCE_REFUND_IN_PROGRESS", "票 " + ticket.id() + " 当前状态不能退款");
        }
        TicketTypeRecord ticketType = repository.findTicketTypeById(order.ticketTypeId()).orElse(null);
        TicketRefundRecord refund = repository.saveTicketRefund(new TicketRefundRecord(
                refundId, order.id(), amountCent, reason, "original", "approved", requestedBy, approvedBy, now, now, now,
                order.merchantId(), REASON_SAFETY_EXCEPTION, responsibleEntityId, ticketType != null && ticketType.refundable()));
        createRefundLines(refund, order, List.of(ticket), Map.of(ticket.id(), slotEntitlements), Map.of(ticket.id(), amountCent), now);
        return finalizeApprovedRefund(refund, now, true);
    }

    /** 每票出票时定格的实付分摊额：复现方案1 出票均分（allocateEvenly 按实例数分订单实付，seq 定序，分到分不丢分）。 */
    private Map<String, Long> perInstanceShares(TicketOrderRecord order, List<TicketInstanceRecord> instances) {
        List<TicketInstanceRecord> sorted = new ArrayList<>(instances);
        sorted.sort(Comparator.comparingInt(TicketInstanceRecord::seqInOrder)
                .thenComparing(TicketInstanceRecord::id));
        long[] shares = allocateEvenly(order.amountCent(), sorted.size());
        Map<String, Long> out = new LinkedHashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            out.put(sorted.get(i).id(), shares[i]);
        }
        return out;
    }

    /** 整数均分（base + 前 remainder 张各多 1 分），合计恒等于总额，与 TicketOrderService 出票均分一致。 */
    private long[] allocateEvenly(long total, int count) {
        long[] out = new long[count];
        if (count <= 0) {
            return out;
        }
        long base = total / count;
        long remainder = total - base * count;
        for (int i = 0; i < count; i++) {
            out[i] = base + (i < remainder ? 1 : 0);
        }
        return out;
    }

    private Map<String, List<IssuedEntitlementRecord>> groupByParent(List<IssuedEntitlementRecord> entitlements) {
        Map<String, List<IssuedEntitlementRecord>> out = new LinkedHashMap<>();
        for (IssuedEntitlementRecord e : entitlements) {
            if (e.parentTicketId() == null) {
                continue;
            }
            out.computeIfAbsent(e.parentTicketId(), k -> new ArrayList<>()).add(e);
        }
        return out;
    }

    private Map<String, String> entToParentMap(String orderId) {
        Map<String, String> out = new HashMap<>();
        for (IssuedEntitlementRecord e : repository.listIssuedEntitlementsByOrderId(orderId)) {
            if (e.parentTicketId() != null) {
                out.put(e.id(), e.parentTicketId());
            }
        }
        return out;
    }

    /** 父票实例是否「完全未核销且 active」：状态 active、未首用、其全部权益 used=0 且 active。无权益=不可退。 */
    private boolean isFullyUnusedActive(TicketInstanceRecord ti, List<IssuedEntitlementRecord> entitlements) {
        if (ti == null || !"active".equals(ti.status()) || ti.firstUsedAt() != null) {
            return false;
        }
        if (repository.existsPendingTransferForParent(ti.id())) {
            return false; // AUD-077 转赠在途 → 不整票自动退(到期路径先作废在途转赠再退)。
        }
        if (entitlements == null || entitlements.isEmpty()) {
            return false;
        }
        for (IssuedEntitlementRecord e : entitlements) {
            if (e.usedTimes() > 0 || !"active".equals(e.status())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 解析要退的父票实例：
     * 传 ticket_instance_ids → 校验属本单、去重、整票连坐可退（任一权益已核销/冻结或已首用则整张拒）；
     * 不传/空 → 该单全部"完全未核销 active"的实例（已核销/已退实例不连坐、自动跳过）。
     */
    private List<TicketInstanceRecord> resolveRefundInstances(
            List<TicketInstanceRecord> allInstances,
            Map<String, List<IssuedEntitlementRecord>> entByInstance,
            List<String> requestedIds) {
        List<TicketInstanceRecord> targets = new ArrayList<>();
        if (requestedIds != null && !requestedIds.isEmpty()) {
            Map<String, TicketInstanceRecord> byId = new LinkedHashMap<>();
            for (TicketInstanceRecord ti : allInstances) {
                byId.put(ti.id(), ti);
            }
            Set<String> seen = new HashSet<>();
            for (String id : requestedIds) {
                if (!seen.add(id)) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "所选票实例存在重复");
                }
                TicketInstanceRecord ti = byId.get(id);
                if (ti == null) {
                    throw ApiException.badRequest("TICKET_INSTANCE_NOT_IN_ORDER",
                            "所选票实例包含不属于此订单的票");
                }
                requireInstanceRefundable(ti, entByInstance.get(id));
                targets.add(ti);
            }
            return targets;
        }
        for (TicketInstanceRecord ti : allInstances) {
            if (isFullyUnusedActive(ti, entByInstance.get(ti.id()))) {
                targets.add(ti);
            }
        }
        return targets;
    }

    /** 整票连坐可退性校验（显式选票时）：任一不满足即整笔拒（不做部分放行）。 */
    private void requireInstanceRefundable(TicketInstanceRecord ti, List<IssuedEntitlementRecord> entitlements) {
        if (repository.existsPendingTransferForParent(ti.id())) {
            throw ApiException.conflict("TICKET_TRANSFER_PENDING_NO_REFUND",
                    "该票有转赠在等待领取，请先撤回转赠再退款");
        }
        if ("refund_pending".equals(ti.status()) || "transferring".equals(ti.status())) {
            throw ApiException.conflict("TICKET_INSTANCE_REFUND_IN_PROGRESS",
                    "票 " + ti.id() + " 已被冻结（退款中或转赠中）");
        }
        if (!"active".equals(ti.status()) || ti.firstUsedAt() != null) {
            throw ApiException.conflict("TICKET_INSTANCE_VERIFIED_NO_REFUND",
                    "票 " + ti.id() + " 已核销或不可退款，整票不可退");
        }
        if (entitlements == null || entitlements.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "票 " + ti.id() + " 没有可退款的权益");
        }
        for (IssuedEntitlementRecord e : entitlements) {
            if (e.usedTimes() > 0 || !"active".equals(e.status())) {
                throw ApiException.conflict("TICKET_INSTANCE_VERIFIED_NO_REFUND",
                        "票 " + ti.id() + " 存在已核销或已冻结的权益，整票不可退");
            }
        }
    }

    /**
     * 建退款行（按实例分组，明细仍逐权益一行）：每实例分摊额在其权益间按 allocated_revenue/票面价权重再分，末条补尾，
     * 合计恒等于实例分摊额、全部合计恒等于退款单额。
     */
    private void createRefundLines(TicketRefundRecord refund, TicketOrderRecord order,
            List<TicketInstanceRecord> targets, Map<String, List<IssuedEntitlementRecord>> entByInstance,
            Map<String, Long> shareByInstance, Instant now) {
        int index = 0;
        for (TicketInstanceRecord ti : targets) {
            List<IssuedEntitlementRecord> ents = entByInstance.getOrDefault(ti.id(), List.of());
            long share = shareByInstance.getOrDefault(ti.id(), 0L);
            long[] lineAmounts = allocateInstanceShare(share, ents);
            for (int i = 0; i < ents.size(); i++) {
                IssuedEntitlementRecord e = ents.get(i);
                repository.saveTicketRefundLine(new TicketRefundLineRecord(
                        refundLineId(refund.id(), index++), refund.id(), order.id(), e.id(), e.entitlementType(),
                        "entitlement", lineAmounts[i], e.remainingTimes(), e.usedTimes(), "requested", now, now
                ));
            }
        }
    }

    /** 把某实例的分摊额按各权益 allocated_revenue（缺省票面价、再缺省等权）加权分，末条补尾使合计恒等于 share。 */
    private long[] allocateInstanceShare(long share, List<IssuedEntitlementRecord> entitlements) {
        int n = entitlements.size();
        long[] out = new long[n];
        if (n == 0) {
            return out;
        }
        long[] weights = new long[n];
        long totalWeight = 0;
        for (int i = 0; i < n; i++) {
            IssuedEntitlementRecord e = entitlements.get(i);
            long w = e.allocatedRevenueCent() != null && e.allocatedRevenueCent() > 0
                    ? e.allocatedRevenueCent()
                    : (e.valueAmountCent() != null && e.valueAmountCent() > 0 ? e.valueAmountCent() : 1L);
            weights[i] = w;
            totalWeight += w;
        }
        long assigned = 0;
        for (int i = 0; i < n - 1; i++) {
            out[i] = share * weights[i] / totalWeight;
            assigned += out[i];
        }
        out[n - 1] = share - assigned; // 末条补尾（floor 合计 ≤ share → 余数 ≥ 0）。
        return out;
    }

    private String refundLineId(String refundId, int index) {
        return "refund_line_" + slug(refundId) + "_" + index;
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private String slug(String value) {
        String slug = trimToDefault(value, UUID.randomUUID().toString())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String fallback = UUID.randomUUID().toString().replace("-", "");
        String resolved = slug.isEmpty() ? fallback : slug;
        return resolved.length() > 40 ? resolved.substring(0, 40) : resolved;
    }
}
