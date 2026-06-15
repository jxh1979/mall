package com.complex.platform.customer;

import java.time.Instant;

public record CustomerPointsRedemptionRecord(
        String id,
        String customerId,
        String catalogItemId,
        String itemName,
        int quantity,
        long pointsCost,
        long totalPoints,
        String inventoryItemId,
        String stockLocationId,
        int stockQuantity,
        String sourceTerminal,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
