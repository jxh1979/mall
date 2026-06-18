package com.complex.platform.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

public record StockAdjustmentRecord(
        String id,
        String itemId,
        String locationId,
        String adjustmentType,
        int quantity,
        String reason,
        List<String> evidenceAttachmentIds,
        String status,
        Instant createdAt,
        @JsonIgnore String merchantId
) {
}
