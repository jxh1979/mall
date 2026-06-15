package com.complex.platform.bar;

import java.time.Instant;

public record BarProductRecord(
        String id,
        String barId,
        String productName,
        String productType,
        long priceCent,
        String currency,
        String inventoryItemId,
        String stockLocationId,
        boolean stockDeductionRequired,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
