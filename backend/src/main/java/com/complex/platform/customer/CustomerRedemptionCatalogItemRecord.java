package com.complex.platform.customer;

import java.time.Instant;

public record CustomerRedemptionCatalogItemRecord(
        String id,
        String itemName,
        String itemType,
        long pointsCost,
        String inventoryItemId,
        String stockLocationId,
        int stockQuantity,
        String status,
        Instant availableFrom,
        Instant availableUntil,
        Instant createdAt,
        Instant updatedAt
) {
}
