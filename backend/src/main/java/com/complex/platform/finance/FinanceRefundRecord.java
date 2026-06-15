package com.complex.platform.finance;

import java.time.Instant;

public record FinanceRefundRecord(
        String id,
        String targetId,
        long refundAmountCent,
        String reason,
        String refundChannel,
        String status,
        Instant createdAt
) {
}
