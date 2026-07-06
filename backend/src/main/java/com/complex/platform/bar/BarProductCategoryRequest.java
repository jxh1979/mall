package com.complex.platform.bar;

/** 分类新增/修改入参。id 空=新增；非空=更新该分类。 */
public record BarProductCategoryRequest(
        String id,
        String barId,
        String categoryName,
        Integer sortOrder,
        String status
) {
}
