package com.complex.platform.bar;

import java.util.List;

/** 套餐新增/修改入参。id 空=新增；非空=更新（含整体替换 items）。 */
public record BarComboRequest(
        String id,
        String barId,
        String comboName,
        Long priceCent,
        Long internalSettlementPriceCent,
        String imageUrl,
        String status,
        List<Item> items
) {
    /** 套餐组合项：某 SKU + 数量。 */
    public record Item(String productId, Integer quantity) {
    }
}
