package com.complex.platform.bar;

import java.time.Instant;

public record BarOrderRecord(
        String id,
        String barId,
        String tableId,
        String reservationId,
        String customerId,
        String orderSource,
        String status,
        long totalAmountCent,
        String currency,
        String createdTerminal,
        String deviceId,
        String salesStaffId,
        Instant createdAt,
        Instant updatedAt
) {
}
