package com.complex.platform.bar;

/** 套餐明细行（V257）：套餐包含某 SKU 及数量。 */
public record BarComboItemRecord(
        String id,
        String comboId,
        String productId,
        String productName,
        int quantity
) {
}
