package com.complex.platform.finance;

import java.time.Instant;

public record PaymentIntentRecord(
        String id,
        String businessOrderType,
        String businessOrderId,
        long amountCent,
        String currency,
        String channel,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
