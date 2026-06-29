package com.complex.platform.inventory.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

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
        @NotNull @Positive Integer quantity,
        Instant expireAt,
        String status
) {
}
