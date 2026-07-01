package com.complex.platform.ticketing;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.RefundAuthorizationService;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketRefundRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // F-TXN-03 按票实例判退（2026-06-29 裁决）：解析本次要退的票实例——
        //   传 entitlement_ids → 仅退所选票（已核销票按票实例拒退 TICKET_ENTITLEMENT_VERIFIED_NO_REFUND）；
        //   不传 → 退该订单全部未核销票（已核销票不连坐、自动跳过）；订单全核销则无可退（同码拒退）。
        List<IssuedEntitlementRecord> allEntitlements = repository.listIssuedEntitlementsByOrderId(targetId);
        List<IssuedEntitlementRecord> refundTargets =
                resolveRefundTargets(allEntitlements, actual.entitlementIds());
        long amountCent = requirePositiveLong(actual.refundAmountCent(), "refund_amount_cent");
        long alreadyRequested = repository.sumPendingRefundAmountByOrderId(targetId);
        if (alreadyRequested + amountCent > order.amountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "refund amount exceeds order amount");
        }
        // 1.1 份额封顶（F-TXN-03）：只退部分未核销票时，退款额不得超过所选票的可退价值之和，
        // 单张票不能退整单额（镜像 autoRefundExpiredUnusedTickets 的 perTicketShare 口径）；
        // 恰退订单全部票 → 沿用上方整单封顶（规避按张均摊整除舍入）。
        if (!refundTargets.isEmpty() && refundTargets.size() < allEntitlements.size()) {
            long refundableCeiling = refundTargets.stream()
                    .mapToLong(e -> perTicketShare(order, e, allEntitlements.size()))
                    .sum();
            if (amountCent > refundableCeiling) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        "refund amount exceeds refundable ticket value");
            }
        }
        Instant now = Instant.now();
        String requestedBy = currentActorId();
        // Stage B 分级授权（功能标志 refund.tiered_authorization.enabled，默认关）：判定本笔退款能否当场自助完成。
        //   额度内(WITHIN_LIMIT)/规则既定(RULE_AUTO) → 发起人当场自助自动退；超额/无额度(REVIEW_REQUIRED) → 留待二人复核。
        //   手动发起无规则上下文(autoRuleType=null)，故此路径只会落「额度自助」或「复核」；到期未用规则自动退另走
        //   autoRefundExpiredUnusedTickets。关闭功能标志或引擎未注入 → 维持现状：一律建 requested、等人工 approveRefund。
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
                now
        ));
        createRefundLines(refund, order, refundTargets, now);
        if (selfComplete) {
            finalizeApprovedRefund(refund, now);
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
        // Stage B 职责分离（SoD，功能标志开时）：复核档退款的审批人不得为发起人——仿 FinanceService.decideAdjustment
        //   （approver≠requestedBy，SELF_APPROVAL_FORBIDDEN）。关闭功能标志 → 维持现状：审批人记字面量 "staff"、不做 SoD。
        String approver = tieredRefundEnabled ? currentActorId() : null;
        if (tieredRefundEnabled && approver != null && approver.equals(current.requestedBy())) {
            throw ApiException.forbidden("SELF_APPROVAL_FORBIDDEN",
                    "the requester of a refund cannot approve it; a different approver is required");
        }
        String approvedBy = approver != null ? approver : "staff";
        Instant now = Instant.now();
        // F-TXN-04 并发双拨付防护：退款状态翻转改用 CAS（WHERE status='requested'），并发只一路过闸，
        // 杜绝并发审批同一退款单各发一次 TicketRefundApprovedEvent（双拨付）。原读检+saveTicketRefund 的 TOCTOU 消除。
        int approvedRows = repository.markTicketRefundApproved(current.id(), approvedBy, now);
        if (approvedRows == 0) {
            throw ApiException.conflict(
                    "TICKET_REFUND_NOT_APPROVABLE", "ticket refund cannot be approved in current status");
        }
        TicketRefundRecord approved = new TicketRefundRecord(
                current.id(), current.targetId(), current.refundAmountCent(), current.reason(), current.refundChannel(),
                "approved", current.requestedBy(), approvedBy, now, current.createdAt(), now
        );
        finalizeApprovedRefund(approved, now);
        return approved;
    }

    /**
     * 退款审批生效后的统一兑付副作用（approveRefund 人工审批通过 与 requestRefund 额度内自助完成 共用）：
     * 退款行置 approved + 冻结对应权益（active→refunded，防退款后照常入场）+ 按比例发积分冲减 + 累计全额时回补库存 + 发打款拨付事件。
     * 不含 SoD/CAS（由各入口先行把关）。
     */
    private void finalizeApprovedRefund(TicketRefundRecord approved, Instant now) {
        repository.listTicketRefundLines(approved.id()).forEach(line -> {
            repository.saveTicketRefundLine(new TicketRefundLineRecord(
                    line.id(), line.refundId(), line.orderId(), line.entitlementId(), line.entitlementType(), line.lineType(),
                    line.refundAmountCent(), line.remainingTimesBefore(), line.usedTimesBefore(), "approved", line.createdAt(), now
            ));
            // 2a-3 退款冻结：退款审批生效 → 冻结对应权益（active→refunded），使其不可再核销（防"退款后照常入场"双重损失）。
            if (line.entitlementId() != null && !line.entitlementId().isBlank()) {
                repository.freezeEntitlementForRefund(line.entitlementId(), now);
            }
        });
        // P0 清单#2：退款审批通过（生效）→ 发积分冲减事件，customer 域按退款比例冲减原单已 confirmed 积分。
        // 幂等键 source_doc=ticket_refund/refundId；无会员/未入账由 LoyaltyService 侧 noop。
        publishPointsDeductRequested(approved);
        // 票务退款打款闭环：①累计全额退款回补库存（限量票名额返还，与下单即扣对称）②发打款拨付事件（解耦）。
        TicketOrderRecord order = repository.findTicketOrderById(approved.targetId()).orElse(null);
        if (order != null) {
            // 累计全额退款（含本笔的已审批退款合计 ≥ 原单额）视为整单取消 → 回补该票种库存 order.quantity。
            // 用"恰好跨过阈值"判定（priorApproved < 全额 且 累计 ≥ 全额）保证多次部分退款累计达全额时**恰回补一次**，
            // 既修复"单笔判定致分次退款永不回补"，又避免重复回补（幂等，无需额外标记）。
            long cumulativeApproved = repository.sumApprovedRefundAmountByOrderId(order.id());
            long priorApproved = cumulativeApproved - approved.refundAmountCent();
            if (order.amountCent() > 0 && order.quantity() > 0
                    && priorApproved < order.amountCent() && cumulativeApproved >= order.amountCent()) {
                repository.releaseTicketStock(order.ticketTypeId(), order.quantity());
            }
            // 打款拨付信号：金额已落 ticket_refunds（net 按业务表扣减，C1），此事件只承载实际打款，不重复入账。
            eventPublisher.publishEvent(new TicketRefundApprovedEvent(
                    order.id(), approved.id(), approved.refundAmountCent(), approved.refundChannel()));
        }
    }

    /**
     * F-TXN-03 门票到期自动退款（系统发起，无 auth 上下文）：全未核销的已付到期订单整单原渠道直批退款。
     * 与手动退款生效效果一致（退款记录 + 行直批 approved、冻结权益 active→refunded、全额回补库存、发打款事件），
     * 但**跳过商户范围闸**（系统动作）；全未核销 → 无已确认积分可冲减，不发 points deduct。
     * 幂等：非 paid / 0 元 / 有核销 / 已有退款（pending/approved）→ 跳过返回 null（由调用方对到期未用权益另作作废）。
     * {@code @Transactional} 保多写原子（失败回滚，权益保持 active、下轮重试）。
     */
    /**
     * F-TXN-03 门票到期按票实例自动退款（系统发起，无 auth 上下文，2026-06-29 裁决）：对该订单**到期未核销**的票
     * 逐张原渠道自动退款，金额=这些票的份额合计（恰覆盖订单全部票则=整单额，避免整除舍入）；
     * **不再因同单其他票已核销而连坐作废**。直批生效：退款行 approved、冻结对应票(active→refunded)、
     * 累计达全额时回补一次库存、发打款事件；全未核销无已确认积分，故不发 points deduct。
     * 非 paid / 0 元 / 无未核销票 / 已有退款在途或完成 → 返回 null（由调用方对到期票作废）。{@code @Transactional} 保多写原子。
     */
    @Transactional
    public TicketRefundRecord autoRefundExpiredUnusedTickets(
            TicketOrderRecord order, List<IssuedEntitlementRecord> expiredUnusedTickets, Instant now) {
        if (order == null || !"paid".equals(order.payStatus()) || order.amountCent() <= 0) {
            return null;
        }
        List<IssuedEntitlementRecord> refundable = expiredUnusedTickets.stream()
                .filter(e -> e.usedTimes() == 0).toList();
        if (refundable.isEmpty()) {
            return null; // 无未核销票可退（防御；调用方应已过滤）。
        }
        if (repository.sumApprovedRefundAmountByOrderId(order.id()) > 0
                || repository.sumPendingRefundAmountByOrderId(order.id()) > 0) {
            return null; // 已有退款（手动/自动）→ 幂等跳过。
        }
        // 退款额：到期未核销票的份额合计；恰覆盖订单全部票则取整单额（规避按张均摊的整除舍入误差）。
        List<IssuedEntitlementRecord> all = repository.listIssuedEntitlementsByOrderId(order.id());
        long amountCent = refundable.size() == all.size()
                ? order.amountCent()
                : Math.min(order.amountCent(),
                        refundable.stream().mapToLong(e -> perTicketShare(order, e, all.size())).sum());
        if (amountCent <= 0) {
            return null;
        }
        TicketRefundRecord refund = repository.saveTicketRefund(new TicketRefundRecord(
                "ticket_refund_auto_expiry_" + slug(order.id()),
                order.id(),
                amountCent,
                "ticket entitlements expired unused — system auto refund (per-ticket)",
                "original",
                "approved",
                "system",
                "system",
                now,
                now,
                now));
        createRefundLines(refund, order, refundable, now);
        // 直批生效（镜像 finalizeApprovedRefund 的兑付副作用；系统动作跳过商户闸；未核销无已确认积分故不冲减）：
        repository.listTicketRefundLines(refund.id()).forEach(line -> {
            repository.saveTicketRefundLine(new TicketRefundLineRecord(
                    line.id(), line.refundId(), line.orderId(), line.entitlementId(), line.entitlementType(),
                    line.lineType(), line.refundAmountCent(), line.remainingTimesBefore(), line.usedTimesBefore(),
                    "approved", line.createdAt(), now));
            if (line.entitlementId() != null && !line.entitlementId().isBlank()) {
                repository.freezeEntitlementForRefund(line.entitlementId(), now);
            }
        });
        // 库存回补：累计退款恰达整单额时回补一次（与手动 finalizeApprovedRefund 一致，分批不重复回补）。
        long cumulativeApproved = repository.sumApprovedRefundAmountByOrderId(order.id());
        long priorApproved = cumulativeApproved - refund.refundAmountCent();
        if (order.quantity() > 0 && priorApproved < order.amountCent() && cumulativeApproved >= order.amountCent()) {
            repository.releaseTicketStock(order.ticketTypeId(), order.quantity());
        }
        eventPublisher.publishEvent(new TicketRefundApprovedEvent(
                order.id(), refund.id(), refund.refundAmountCent(), refund.refundChannel()));
        return refund;
    }

    /** 单张票退款份额：优先票面价 value_amount_cent，缺省则按订单额 / 票数均摊（至少 1 分）。 */
    private long perTicketShare(TicketOrderRecord order, IssuedEntitlementRecord entitlement, int entitlementCount) {
        return entitlement.valueAmountCent() != null && entitlement.valueAmountCent() > 0
                ? entitlement.valueAmountCent()
                : Math.max(1L, order.amountCent() / Math.max(1, entitlementCount));
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
     * 按票实例解析本次退款目标（F-TXN-03，2026-06-29 裁决）：
     * 传 entitlement_ids → 校验每张属于本单且未核销（已核销按票实例拒退 TICKET_ENTITLEMENT_VERIFIED_NO_REFUND）；
     * 不传 → 该订单全部未核销票（已核销票不连坐、自动跳过）；订单全核销=无可退（同码拒退）。
     * 订单尚未出票（无权益）→ 返回空，下游落 order 级退款行（沿用旧行为）。
     */
    private List<IssuedEntitlementRecord> resolveRefundTargets(
            List<IssuedEntitlementRecord> all, List<String> entitlementIds) {
        List<IssuedEntitlementRecord> targets = new java.util.ArrayList<>();
        if (entitlementIds != null && !entitlementIds.isEmpty()) {
            java.util.Map<String, IssuedEntitlementRecord> byId = new java.util.LinkedHashMap<>();
            for (IssuedEntitlementRecord e : all) {
                byId.put(e.id(), e);
            }
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (String id : entitlementIds) {
                if (!seen.add(id)) {
                    throw ApiException.badRequest("VALIDATION_FAILED",
                            "entitlement_ids contains a duplicate id");
                }
                IssuedEntitlementRecord e = byId.get(id);
                if (e == null) {
                    throw ApiException.badRequest("VALIDATION_FAILED",
                            "entitlement_ids contains an id that does not belong to this order");
                }
                if (e.usedTimes() > 0) {
                    throw ApiException.conflict("TICKET_ENTITLEMENT_VERIFIED_NO_REFUND",
                            "ticket entitlement has been verified (checked-in) and is no longer refundable");
                }
                targets.add(e);
            }
            return targets;
        }
        for (IssuedEntitlementRecord e : all) {
            if (e.usedTimes() == 0) {
                targets.add(e);
            }
        }
        if (!all.isEmpty() && targets.isEmpty()) {
            throw ApiException.conflict("TICKET_ENTITLEMENT_VERIFIED_NO_REFUND",
                    "all ticket entitlements in this order are verified; nothing is refundable");
        }
        return targets;
    }

    private void createRefundLines(TicketRefundRecord refund, TicketOrderRecord order,
            List<IssuedEntitlementRecord> entitlements, Instant now) {
        if (entitlements.isEmpty()) {
            repository.saveTicketRefundLine(new TicketRefundLineRecord(
                    refundLineId(refund.id(), 0), refund.id(), order.id(), null, null, "order",
                    refund.refundAmountCent(), 0, 0, "requested", now, now
            ));
            return;
        }

        long remaining = refund.refundAmountCent();
        for (int index = 0; index < entitlements.size() && remaining > 0; index++) {
            IssuedEntitlementRecord entitlement = entitlements.get(index);
            boolean lastLine = index == entitlements.size() - 1;
            long suggestedAmount = entitlement.valueAmountCent() == null || entitlement.valueAmountCent() <= 0
                    ? Math.max(1L, order.amountCent() / Math.max(1, entitlements.size()))
                    : entitlement.valueAmountCent();
            long lineAmount = lastLine ? remaining : Math.min(suggestedAmount, remaining);
            if (lineAmount <= 0) {
                continue;
            }
            remaining -= lineAmount;
            repository.saveTicketRefundLine(new TicketRefundLineRecord(
                    refundLineId(refund.id(), index), refund.id(), order.id(), entitlement.id(), entitlement.entitlementType(),
                    "entitlement", lineAmount, entitlement.remainingTimes(), entitlement.usedTimes(), "requested", now, now
            ));
        }
        if (remaining > 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "refund amount cannot be allocated to entitlement lines");
        }
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

    private long requirePositiveLong(Long value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        if (value <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be positive");
        }
        return value;
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
