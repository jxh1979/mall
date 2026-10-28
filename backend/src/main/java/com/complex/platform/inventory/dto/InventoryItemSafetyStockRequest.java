package com.complex.platform.inventory.dto;

// 文件版本：V0.01

/**
 * 第十八批第 33 条 33-R4：修改已有物料的安全库存（inventory_items.min_quantity）。第十八批裁决 5 的采购建议量
 * 「安全库存＋近 7 天销量－当前可用库存－在途量」取物料安全库存；minQuantity 必填、不得为负。只改安全库存，不开放别的物料字段。
 */
public record InventoryItemSafetyStockRequest(
        String idempotencyKey,
        Integer minQuantity
) {
}
