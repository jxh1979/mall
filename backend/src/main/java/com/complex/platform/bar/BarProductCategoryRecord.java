package com.complex.platform.bar;

import java.time.Instant;

/** 酒吧商品分类（V256 图形化点单 POS 阶段1）：可配置分类，POS 按此分屏列菜。 */
public record BarProductCategoryRecord(
        String id,
        String barId,
        String merchantId,
        String categoryName,
        int sortOrder,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
