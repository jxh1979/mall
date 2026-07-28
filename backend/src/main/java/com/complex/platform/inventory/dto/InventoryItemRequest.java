package com.complex.platform.inventory.dto;
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
        Boolean manualHighValue
) {
}
