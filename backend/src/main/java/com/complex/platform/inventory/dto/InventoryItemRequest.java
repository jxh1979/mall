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
        String status
) {
}
