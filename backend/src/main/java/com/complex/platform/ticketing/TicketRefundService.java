package com.complex.platform.ticketing;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketRefundRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class TicketRefundService {

    // P0 清单#2：退款比例 basis points 分母（10000=100%）。
    private static final long BASIS_POINTS_DENOMINATOR = 10_000L;

    private final TicketingRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    // F-TXN-01 退款跨商户越权修复：退款须按调用者商户范围校验原票/订单归属。可为 null=旧构造/兼容测试路径
    // （不强制）；生产由 Spring 走主构造注入。平台管理员 scope 为空=放行，商户用户必须匹配原单 merchant_id。
    private final AuthorizationService authorizationService;

    @org.springframework.beans.factory.annotation.Autowired
    public TicketRefundService(
            TicketingRepository repository,
            ApplicationEventPublisher eventPublisher,
            AuthorizationService authorizationService) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.authorizationService = authorizationService;
    }

    /**
     * 向后兼容构造：未注入 AuthorizationService 时跨商户守卫为关（=null），退款链路按原行为执行。
     * 供既有单测构造（不改既有用例）。生产环境由 Spring 走上方主构造注入。
     */
    public TicketRefundService(TicketingRepository repository, ApplicationEventPublisher eventPublisher) {
        this(repository, eventPublisher, null);
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
        // F-TXN-03 退款规则联动（用户口径）：多次/多人票一经购买即「限时用完」——核销一张则整票不可退、核销后不可退；
        //   仅「全未核销」可原渠道退（其 pending 积分从未 confirmed，天然不计分）。
        //   判定：本订单任一权益已核销（usedTimes>0，冲正后回滚归零则视为未核销）→ 拒退。
        if (repository.listIssuedEntitlementsByOrderId(targetId).stream()
                .anyMatch(entitlement -> entitlement.usedTimes() > 0)) {
            throw ApiException.conflict("TICKET_ORDER_VERIFIED_NO_REFUND",
                    "ticket order has verified (checked-in) entitlements and is no longer refundable");
        }
        long amountCent = requirePositiveLong(actual.refundAmountCent(), "refund_amount_cent");
        long alreadyRequested = repository.sumPendingRefundAmountByOrderId(targetId);
        if (alreadyRequested + amountCent > order.amountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "refund amount exceeds order amount");
        }
        Instant now = Instant.now();
        TicketRefundRecord refund = repository.saveTicketRefund(new TicketRefundRecord(
                "ticket_refund_" + slug(actual.idempotencyKey()),
                targetId,
                amountCent,
                requireText(actual.reason(), "reason"),
                trimToDefault(actual.refundChannel(), "original"),
                "requested",
                null,
                null,
                now,
                now
        ));
        createRefundLines(refund, order, now);
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
        Instant now = Instant.now();
        // F-TXN-04 并发双拨付防护：退款状态翻转改用 CAS（WHERE status='requested'），并发只一路过闸，
        // 杜绝并发审批同一退款单各发一次 TicketRefundApprovedEvent（双拨付）。原读检+saveTicketRefund 的 TOCTOU 消除。
        int approvedRows = repository.markTicketRefundApproved(current.id(), "staff", now);
        if (approvedRows == 0) {
            throw ApiException.conflict(
                    "TICKET_REFUND_NOT_APPROVABLE", "ticket refund cannot be approved in current status");
        }
        TicketRefundRecord approved = new TicketRefundRecord(
                current.id(), current.targetId(), current.refundAmountCent(), current.reason(), current.refundChannel(),
                "approved", "staff", now, current.createdAt(), now
        );
        repository.listTicketRefundLines(current.id()).forEach(line -> {
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
        // 票务退款打款闭环：①全额退款回补库存（限量票名额返还，与下单即扣对称）②发打款拨付事件（解耦）。
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
        return approved;
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

    private void createRefundLines(TicketRefundRecord refund, TicketOrderRecord order, Instant now) {
        List<IssuedEntitlementRecord> entitlements = repository.listIssuedEntitlementsByOrderId(order.id());
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
