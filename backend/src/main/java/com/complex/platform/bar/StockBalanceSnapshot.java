package com.complex.platform.bar;

import java.time.Instant;

public record StockBalanceSnapshot(
        String id,
        String itemId,
        String locationId,
        int quantity,
        int reservedQuantity,
        int availableQuantity,
        Instant updatedAt
) {
}
