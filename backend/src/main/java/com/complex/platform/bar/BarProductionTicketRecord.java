package com.complex.platform.bar;

import java.time.Instant;

public record BarProductionTicketRecord(
        String id,
        String orderId,
        String barId,
        String merchantId,
        String ticketStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
