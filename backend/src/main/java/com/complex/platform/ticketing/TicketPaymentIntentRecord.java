package com.complex.platform.ticketing;

import java.time.Instant;

public record TicketPaymentIntentRecord(
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
