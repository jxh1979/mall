package com.complex.platform.inventory.dto;

// 文件版本：V0.03

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
        // 第十八批第 24 条（ENUM:164「或由库房主管以上岗位手工标记为高价值……一线库管无此权限」）：建物料不再带「手工标记高价值」，
        // 标记与取消走 POST /api/v1/inventory/items/{id}/high-value-mark（InventoryItemHighValueMarkRequest）。
        // FS-019（BSM L1201）：不可消费物品（设备、器具等）才能借用；不填按可消费。
        Boolean nonConsumable
) {
}
