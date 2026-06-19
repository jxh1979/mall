package com.complex.platform.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
        Instant updatedAt,
        @JsonIgnore String merchantId,
        // R5 T6-4：高价值阈值（整数分，可空）。null = 该物料不视为高价值/不触发报损/盘点差异复核（向后兼容）。
        Long highValueThresholdCents
) {
}
