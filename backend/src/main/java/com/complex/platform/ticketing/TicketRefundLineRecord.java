package com.complex.platform.ticketing;

import java.time.Instant;

public record TicketRefundLineRecord(
        String id,
        String refundId,
        String orderId,
        String entitlementId,
        String entitlementType,
        String lineType,
        long refundAmountCent,
        int remainingTimesBefore,
        int usedTimesBefore,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
