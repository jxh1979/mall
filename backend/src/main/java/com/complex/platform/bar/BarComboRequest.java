package com.complex.platform.bar;

import java.util.List;

/** 套餐新增/修改入参。id 空=新增；非空=更新（含整体替换 items）。 */
public record BarComboRequest(
        String id,
        String barId,
        String comboName,
        Long priceCent,
        // ⚠️ 批10/E(V380)起本字段已封写:内部结算价唯一可编辑处 = 内部结算 SKU 目录;携带非空值一律 400。
        Long internalSettlementPriceCent,
        String imageUrl,
        String status,
        List<Item> items,
        // 批10/E(V380)：内部结算 SKU 目录引用(可空;更新时留空 = 保留既有引用不动)。
        //   条目绑定商户必须 = 本套餐所属酒吧(bar_id)。
        String catalogItemCode
) {
    /** 向后兼容构造器（批10/E 目录引用前的 8 字段签名）：既有调用方零改动，引用置空。 */
    public BarComboRequest(String id, String barId, String comboName, Long priceCent,
            Long internalSettlementPriceCent, String imageUrl, String status, List<Item> items) {
        this(id, barId, comboName, priceCent, internalSettlementPriceCent, imageUrl, status, items, null);
    }

    /** 套餐组合项：某 SKU + 数量。 */
    public record Item(String productId, Integer quantity) {
    }
}
