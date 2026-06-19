package com.complex.platform.inventory.dto;

import java.util.List;

public record InventoryItemRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String merchantId,
        String venueId,
        String itemName,
        String category,
        String unit,
        Integer minQuantity,
        String status,
        // R5 T6-4：物料高价值阈值（整数分，可空）。null = 不视为高价值/报损/盘点差异不触发复核（向后兼容）。
        Long highValueThresholdCents
) {
}
