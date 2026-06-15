package com.complex.platform.inventory;

import java.time.Instant;

public record InventoryItemRecord(
        String id,
        String skuCode,
        String itemName,
        String itemType,
        String unit,
        String barcode,
        boolean traceRequired,
        int minQuantity,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
