package com.complex.platform.ticketing;

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
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 向后兼容构造：未注入分级授权引擎（功能标志关时不需要）。供既有单测构造。
     */
    public TicketRefundService(
            TicketingRepository repository,
            ApplicationEventPublisher eventPublisher,
            AuthorizationService authorizationService) {
        this(repository, eventPublisher, authorizationService, null);
    }

    /**
     * 向后兼容构造：未注入 AuthorizationService 时跨商户守卫为关（=null），退款链路按原行为执行。
     * 供既有单测构造（不改既有用例）。生产环境由 Spring 走上方主构造注入。
     */
    public TicketRefundService(TicketingRepository repository, ApplicationEventPublisher eventPublisher) {
        this(repository, eventPublisher, null, null);
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
        authorizationService.currentMerchantScope().ifPresent(scope -> {
            if (!scope.equals(merchantId)) {
                throw ApiException.forbidden(
                        "CROSS_MERCHANT_FORBIDDEN", "access to another merchant's ticket order is not allowed");
            }
        });
    }

    TicketRefundRecord requestRefund(TicketRefundRequest request) {
        TicketRefundRequest actual = requireRequest(request);
        String targetId = requireText(actual.targetId(), "target_id");
        TicketOrderRecord order = repository.findTicketOrderById(targetId)
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "ticket order not found"));
        // F-TXN-01 越权修复：发起退款前按调用者商户范围校验原单归属，防商户A退商户B的票（跨租户 IDOR）。
        enforceMerchantScope(order.merchantId());
        // 仅已支付订单可退款：堵"未支付/已取消订单凭空发起退款"（无真实收款却触发退款冻结/积分冲减链）。
        if (!"paid".equals(order.payStatus())) {
            throw ApiException.conflict("TICKET_ORDER_NOT_REFUNDABLE", "only paid ticket orders can be refunded");
        }
        // AUD-006 退款单位=整张票（无兼容期）：旧入参 entitlement_ids 传入非空即拒。
        if (actual.entitlementIds() != null && !actual.entitlementIds().isEmpty()) {
            throw ApiException.badRequest("REFUND_UNIT_IS_TICKET",
                    "refund unit is a whole ticket; select tickets by ticket_instance_ids, not entitlement_ids");
        }
        // 规则2 可退配置：票种默认不可退；不可退则拒申请退（客人不能申请退）。
        TicketTypeRecord ticketType = repository.findTicketTypeById(order.ticketTypeId()).orElse(null);
        if (ticketType == null || !ticketType.refundable()) {
            throw ApiException.conflict("TICKET_TYPE_NOT_REFUNDABLE",
                    "this ticket type is not refundable");
        }
        List<TicketInstanceRecord> allInstances = repository.listTicketInstancesByOrderId(targetId);
        if (allInstances.isEmpty()) {
            throw ApiException.conflict("TICKET_ORDER_NOT_REFUNDABLE",
                    "ticket order has no ticket instances to refund");
        }
        Map<String, Long> shareByInstance = perInstanceShares(order, allInstances);
        Map<String, List<IssuedEntitlementRecord>> entByInstance =
                groupByParent(repository.listIssuedEntitlementsByOrderId(targetId));
        // 解析要退的父票实例：不传/空=该单全部"完全未核销"的实例；传=校验属本单、去重、整票连坐可退。
        List<TicketInstanceRecord> targets =
                resolveRefundInstances(allInstances, entByInstance, actual.ticketInstanceIds());
        if (targets.isEmpty()) {
            throw ApiException.conflict("TICKET_INSTANCE_VERIFIED_NO_REFUND",
                    "no fully-unused ticket instance in this order is refundable");
        }
        // 金额=服务端算定的所选实例分摊额合计；客户端不定价，若带 refund_amount_cent 必须相等。
        long amountCent = targets.stream().mapToLong(ti -> shareByInstance.getOrDefault(ti.id(), 0L)).sum();
        if (amountCent <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "selected ticket instances have zero refundable amount; void the ticket instead of refunding");
        }
        if (actual.refundAmountCent() != null && actual.refundAmountCent() != amountCent) {
            throw ApiException.badRequest("REFUND_AMOUNT_MISMATCH",
                    "refund_amount_cent must equal the server-computed whole-ticket amount");
        }
        long alreadyRequested = repository.sumPendingRefundAmountByOrderId(targetId);
        if (alreadyRequested + amountCent > order.amountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "refund amount exceeds order amount");
        }
        Instant now = Instant.now();
        // 申请即冻结：逐实例 CAS active→refund_pending；并发抢先者胜，命中 0 行=该票在途/已用/已退 → 整笔回滚拒。
        for (TicketInstanceRecord ti : targets) {
            if (repository.markTicketInstanceRefundPending(ti.id(), now) == 0) {
                throw ApiException.conflict("TICKET_INSTANCE_REFUND_IN_PROGRESS",
                        "ticket instance " + ti.id() + " cannot be frozen for refund in its current status");
            }
        }
        String requestedBy = currentActorId();
        // Stage B 分级授权（功能标志 refund.tiered_authorization.enabled，默认关）：判定本笔退款能否当场自助完成。
        //   额度内/规则既定 → 发起人当场自助自动退；超额/无额度 → 留待二人复核。关闭/未注入 → 一律 requested 等人工审批。
        boolean selfComplete = tieredRefundEnabled && refundAuthorizationService != null
                && refundAuthorizationService.classify("ticket", amountCent, requestedBy, null)
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
                order.merchantId()   // V220：退款受理商户=原销售订单 merchant_id（平台直营单为 null）
        ));
        createRefundLines(refund, order, targets, entByInstance, shareByInstance, now);
        if (selfComplete) {
            finalizeApprovedRefund(refund, now, true);
        }
        return refund;
    }

    TicketRefundRecord approveRefund(String refundId, StatusChangeRequest request) {
        StatusChangeRequest actual = requireRequest(request);
        requireText(actual.reason(), "reason");
        TicketRefundRecord current = repository.findTicketRefundById(requireText(refundId, "refund_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_REFUND_NOT_FOUND", "ticket refund not found"));
        // F-TXN-01 越权修复：审批退款（冻结权益/积分冲减/库存回补皆为写）前，按调用者商户范围校验原单归属，
        // 防商户A审批通过商户B的退款。原单缺失时不做归属判定（交由下方既有 null 容错路径）。
        repository.findTicketOrderById(current.targetId())
                .ifPresent(order -> enforceMerchantScope(order.merchantId()));
        // Stage B 职责分离（SoD，功能标志开时）：复核档退款的审批人不得为发起人（approver≠requestedBy，SELF_APPROVAL_FORBIDDEN）。
        String approver = tieredRefundEnabled ? currentActorId() : null;
        if (tieredRefundEnabled && approver != null && approver.equals(current.requestedBy())) {
            throw ApiException.forbidden("SELF_APPROVAL_FORBIDDEN",
                    "the requester of a refund cannot approve it; a different approver is required");
        }
        String approvedBy = approver != null ? approver : "staff";
        Instant now = Instant.now();
        // F-TXN-04 并发双拨付防护：退款状态翻转改用 CAS（WHERE status='requested'），并发只一路过闸。
        int approvedRows = repository.markTicketRefundApproved(current.id(), approvedBy, now);
        if (approvedRows == 0) {
            throw ApiException.conflict(
                    "TICKET_REFUND_NOT_APPROVABLE", "ticket refund cannot be approved in current status");
        }
        TicketRefundRecord approved = new TicketRefundRecord(
                current.id(), current.targetId(), current.refundAmountCent(), current.reason(), current.refundChannel(),
                "approved", current.requestedBy(), approvedBy, now, current.createdAt(), now, current.merchantId()
        );
        finalizeApprovedRefund(approved, now, true);
        return approved;
    }

    /**
     * 方案2 复核驳回：退款单 requested→rejected + 行 rejected + 父票 refund_pending→active 解冻（恢复可用）。
     * SoD 同 approve（发起人不得驳回自己，防自发自撤洗留痕）；不打款、不冲积分、不回补库存。
     */
    TicketRefundRecord rejectRefund(String refundId, StatusChangeRequest request) {
        StatusChangeRequest actual = requireRequest(request);
        requireText(actual.reason(), "reason");
        TicketRefundRecord current = repository.findTicketRefundById(requireText(refundId, "refund_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_REFUND_NOT_FOUND", "ticket refund not found"));
        repository.findTicketOrderById(current.targetId())
                .ifPresent(order -> enforceMerchantScope(order.merchantId()));
        String reviewer = tieredRefundEnabled ? currentActorId() : null;
        if (tieredRefundEnabled && reviewer != null && reviewer.equals(current.requestedBy())) {
            throw ApiException.forbidden("SELF_APPROVAL_FORBIDDEN",
                    "the requester of a refund cannot reject it; a different reviewer is required");
        }
        String decidedBy = reviewer != null ? reviewer : "staff";
        Instant now = Instant.now();
        int rejectedRows = repository.markTicketRefundRejected(current.id(), decidedBy, now);
        if (rejectedRows == 0) {
            throw ApiException.conflict(
                    "TICKET_REFUND_NOT_REJECTABLE", "ticket refund cannot be rejected in current status");
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
        return new TicketRefundRecord(
                current.id(), current.targetId(), current.refundAmountCent(), current.reason(), current.refundChannel(),
                "rejected", current.requestedBy(), decidedBy, now, current.createdAt(), now, current.merchantId());
    }

    /**
     * 退款审批生效后的统一兑付副作用（人工审批通过 / 额度内自助完成 / 到期自动退 共用）：
     * 退款行置 approved + 强 CAS 冻结对应权益（active∧used=0→refunded，失配整笔回滚）+ 父票 refund_pending→refunded
     * + 限量票按本笔退成功实例数回补名额（退一张放一个）+ 发打款事件；deductPoints=true 时按比例发积分冲减。
     */
    private void finalizeApprovedRefund(TicketRefundRecord approved, Instant now, boolean deductPoints) {
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
                if (frozen == 0) {
                    throw ApiException.conflict("TICKET_INSTANCE_VERIFIED_NO_REFUND",
                            "ticket entitlement was verified after the refund request; refund rolled back");
                }
                String parent = entToParent.get(line.entitlementId());
                if (parent != null) {
                    refundedInstances.add(parent);
                }
            }
        });
        // 父票 refund_pending→refunded（终态，CAS）。
        for (String instanceId : refundedInstances) {
            repository.markTicketInstanceRefunded(instanceId, now);
        }
        // P0 清单#2：退款生效 → 按退款比例发积分冲减事件（到期自动退全未核销无已确认积分口径 → 不发）。
        if (deductPoints) {
            publishPointsDeductRequested(approved);
        }
        if (order != null) {
            // 限量票名额回补：退一张放一个可售名额（退成功的父票实例数），与下单即扣对称；无限量票 GREATEST(0,..) 兜底无害。
            if (!refundedInstances.isEmpty()) {
                repository.releaseTicketStock(order.ticketTypeId(), refundedInstances.size());
            }
            // 打款拨付信号：金额已落 ticket_refunds，此事件只承载实际打款，不重复入账。
            eventPublisher.publishEvent(new TicketRefundApprovedEvent(
                    order.id(), approved.id(), approved.refundAmountCent(), approved.refundChannel()));
        }
    }

    /**
     * 方案2 门票到期整票自动退款（系统发起，无 auth 上下文）：对该订单**完全未核销**的父票实例整张原渠道直批退款，
     * 金额=这些实例出票时定格分摊额合计；任一权益已核销的实例（部分使用）不在此退（由调用方作废，规则2口径）。
     * 直批生效（冻结权益 active→refunded、父票 refund_pending→refunded、按实例数回补名额、发打款事件）；
     * 全未核销无已确认积分 → 不发 points deduct；跳过商户闸（系统动作）。
     * 非 paid / 0 元 / 无完全未核销实例 / 已有退款在途或完成 → 返回 null。{@code @Transactional} 保多写原子。
     */
    @Transactional
    public TicketRefundRecord autoRefundExpiredWholeTickets(TicketOrderRecord order, Instant now) {
        if (order == null || !"paid".equals(order.payStatus()) || order.amountCent() <= 0) {
            return null;
        }
        if (repository.sumApprovedRefundAmountByOrderId(order.id()) > 0
                || repository.sumPendingRefundAmountByOrderId(order.id()) > 0) {
            return null; // 已有退款（手动/自动）→ 幂等跳过。
        }
        List<TicketInstanceRecord> allInstances = repository.listTicketInstancesByOrderId(order.id());
        if (allInstances.isEmpty()) {
            return null;
        }
        Map<String, List<IssuedEntitlementRecord>> entByInstance =
                groupByParent(repository.listIssuedEntitlementsByOrderId(order.id()));
        List<TicketInstanceRecord> targets = allInstances.stream()
                .filter(ti -> isFullyUnusedActive(ti, entByInstance.get(ti.id())))
                .toList();
        if (targets.isEmpty()) {
            return null; // 无完全未核销实例（全部部分使用/已退）→ 交由调用方作废。
        }
        Map<String, Long> shareByInstance = perInstanceShares(order, allInstances);
        long amountCent = targets.stream().mapToLong(ti -> shareByInstance.getOrDefault(ti.id(), 0L)).sum();
        if (amountCent <= 0) {
            return null;
        }
        // 申请即冻结（系统直批同样先置 refund_pending，复用 finalize 的 refund_pending→refunded CAS）。
        for (TicketInstanceRecord ti : targets) {
            if (repository.markTicketInstanceRefundPending(ti.id(), now) == 0) {
                return null; // 并发抢先 → 让下轮/退款链处理。
            }
        }
        TicketRefundRecord refund = repository.saveTicketRefund(new TicketRefundRecord(
                "ticket_refund_auto_expiry_" + slug(order.id()),
                order.id(),
                amountCent,
                "ticket expired unused — system auto refund (whole ticket)",
                "original",
                "approved",
                "system",
                "system",
                now,
                now,
                now,
                order.merchantId()));   // V220：自动退款受理商户=原订单 merchant_id
        createRefundLines(refund, order, targets, entByInstance, shareByInstance, now);
        finalizeApprovedRefund(refund, now, false);
        return refund;
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
                    throw ApiException.badRequest("VALIDATION_FAILED", "ticket_instance_ids contains a duplicate id");
                }
                TicketInstanceRecord ti = byId.get(id);
                if (ti == null) {
                    throw ApiException.badRequest("TICKET_INSTANCE_NOT_IN_ORDER",
                            "ticket_instance_ids contains an id that does not belong to this order");
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
        if ("refund_pending".equals(ti.status()) || "transferring".equals(ti.status())) {
            throw ApiException.conflict("TICKET_INSTANCE_REFUND_IN_PROGRESS",
                    "ticket instance " + ti.id() + " is already frozen (refund in progress or transferring)");
        }
        if (!"active".equals(ti.status()) || ti.firstUsedAt() != null) {
            throw ApiException.conflict("TICKET_INSTANCE_VERIFIED_NO_REFUND",
                    "ticket instance " + ti.id() + " has been verified or is not refundable; whole ticket is not refundable");
        }
        if (entitlements == null || entitlements.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "ticket instance " + ti.id() + " has no entitlements to refund");
        }
        for (IssuedEntitlementRecord e : entitlements) {
            if (e.usedTimes() > 0 || !"active".equals(e.status())) {
                throw ApiException.conflict("TICKET_INSTANCE_VERIFIED_NO_REFUND",
                        "ticket instance " + ti.id() + " has a verified or frozen entitlement; whole ticket is not refundable");
            }
        }
    }

    private void publishPointsDeductRequested(TicketRefundRecord refund) {
        TicketOrderRecord order = repository.findTicketOrderById(refund.targetId()).orElse(null);
        if (order == null || order.amountCent() <= 0) {
            return; // 无原单/原单金额非正：无法按比例冲减。
        }
        // 退款比例（basis points）= 退款额 / 原单额 × 10000，封顶 100%。
        long ratioBp = Math.min(BASIS_POINTS_DENOMINATOR,
                refund.refundAmountCent() * BASIS_POINTS_DENOMINATOR / order.amountCent());
        if (ratioBp <= 0) {
            return;
        }
        eventPublisher.publishEvent(new PointsDeductRequestedEvent(order.id(), refund.id(), ratioBp));
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
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
