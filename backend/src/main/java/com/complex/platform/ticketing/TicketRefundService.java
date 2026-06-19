package com.complex.platform.ticketing;

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

    public TicketRefundService(TicketingRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    TicketRefundRecord requestRefund(TicketRefundRequest request) {
        TicketRefundRequest actual = requireRequest(request);
        String targetId = requireText(actual.targetId(), "target_id");
        TicketOrderRecord order = repository.findTicketOrderById(targetId)
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "ticket order not found"));
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
        if (!"requested".equals(current.status())) {
            throw ApiException.conflict(
                    "TICKET_REFUND_NOT_APPROVABLE", "ticket refund cannot be approved in current status");
        }
        Instant now = Instant.now();
        TicketRefundRecord approved = repository.saveTicketRefund(new TicketRefundRecord(
                current.id(), current.targetId(), current.refundAmountCent(), current.reason(), current.refundChannel(),
                "approved", "staff", now, current.createdAt(), now
        ));
        repository.listTicketRefundLines(current.id()).forEach(line -> repository.saveTicketRefundLine(new TicketRefundLineRecord(
                line.id(), line.refundId(), line.orderId(), line.entitlementId(), line.entitlementType(), line.lineType(),
                line.refundAmountCent(), line.remainingTimesBefore(), line.usedTimesBefore(), "approved", line.createdAt(), now
        )));
        // P0 清单#2：退款审批通过（生效）→ 发积分冲减事件，customer 域按退款比例冲减原单已 confirmed 积分。
        // 幂等键 source_doc=ticket_refund/refundId；无会员/未入账由 LoyaltyService 侧 noop。
        publishPointsDeductRequested(approved);
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
