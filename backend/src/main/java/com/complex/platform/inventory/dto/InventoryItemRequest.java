package com.complex.platform.inventory.dto;

// 文件版本：V0.02

import jakarta.validation.constraints.PositiveOrZero;

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
        @PositiveOrZero Integer minQuantity,
        String status,
        // V350 归一：物品档案「手工标记为高价值」（业务HTML「或被手工标记为高价值」）。
        // 取代按物料各设一个阈值——那是三套阈值里的一套，与「全平台一个固定金额阈值」直接冲突。
        Boolean manualHighValue,
        // FS-019（BSM L1201）：不可消费物品（设备、器具等）才能借用；不填按可消费。
        Boolean nonConsumable
) {
    public InventoryItemRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds, String merchantId,
            String venueId, String itemName, String category, String unit, Integer minQuantity, String status, Boolean manualHighValue) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, merchantId, venueId, itemName, category, unit, minQuantity, status,
                manualHighValue, null);
    }
}
