package com.complex.platform.bar;

import java.time.Instant;

public record BarTableRecord(
        String id,
        String barId,
        String merchantId,
        String tableCode,
        String tableName,
        String tableType,
        int capacity,
        int allowedPassFreeGuestCount,
        long minConsumptionCent,
        String currency,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
