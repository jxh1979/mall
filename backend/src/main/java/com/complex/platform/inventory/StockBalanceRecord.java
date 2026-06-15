package com.complex.platform.inventory;

import java.time.Instant;

public record StockBalanceRecord(
        String id,
        String itemId,
        String locationId,
        int quantity,
        int reservedQuantity,
        int availableQuantity,
        Instant updatedAt
) {
}
