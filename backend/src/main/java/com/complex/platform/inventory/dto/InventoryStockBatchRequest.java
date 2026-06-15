package com.complex.platform.inventory.dto;

import java.time.Instant;
import java.util.List;

public record InventoryStockBatchRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String itemId,
        String locationId,
        String batchNo,
        String supplierId,
        Integer quantity,
        Instant expireAt,
        String status
) {
}
