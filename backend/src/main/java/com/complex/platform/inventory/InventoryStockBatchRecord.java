package com.complex.platform.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

public record InventoryStockBatchRecord(
        String id,
        String itemId,
        String locationId,
        String batchNo,
        String supplierId,
        int quantity,
        Instant expireAt,
        String status,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt,
        @JsonIgnore String merchantId
) {
}
