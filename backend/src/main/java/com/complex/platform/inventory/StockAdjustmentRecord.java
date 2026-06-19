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
        @JsonIgnore String merchantId,
        // R5 T6-4：调整单价（整数分，可空）。高价值判定 = quantity × unitCostCents ≥ 物料阈值；approve 时复算/留痕。
        Long unitCostCents
) {
}
