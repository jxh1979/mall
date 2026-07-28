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
        // V350 归一：物品档案「手工标记为高价值」。TRUE = 不论进货价一律按高价值走复核/先审后行。
        // 取代此前的按物料阈值 high_value_threshold_cents（与「全平台一个固定金额阈值」冲突，V350 已删）。
        boolean manualHighValue
) {
}
