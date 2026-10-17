package com.complex.platform.dealer;

import java.time.Instant;

/** E-13 退款（原路退回）。status：pending_request / pending_approval / approved / rejected / refunded；reviewId 为既有退款审批复核单。 */
public record DealerOrderRefundRecord(
        String id,
        String orderId,
        String paymentId,
        long amountCent,
        String status,
        String reviewId,
        String requestedBy,
        String executedBy,
        Instant executedAt,
        String referenceNo,
        String reason,
        Instant createdAt,
        Instant updatedAt
) {
}
