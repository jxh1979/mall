package com.complex.platform.bar;

/** 给商品设分类：categoryId 空=清除分类（未分类）。 */
public record BarProductCategoryAssignRequest(
        String categoryId
) {
}
