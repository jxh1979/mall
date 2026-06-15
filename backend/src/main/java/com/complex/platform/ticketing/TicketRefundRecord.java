package com.complex.platform.ticketing;

import java.time.Instant;

public record TicketRefundRecord(
        String id,
        String targetId,
        long refundAmountCent,
        String reason,
        String refundChannel,
        String status,
        String approvedBy,
        Instant approvedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
