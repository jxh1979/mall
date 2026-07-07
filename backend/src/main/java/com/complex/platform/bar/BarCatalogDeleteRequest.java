package com.complex.platform.bar;

/** 目录实体（分类/商品/套餐）删除申请：须先存档；reason 记录删除理由供审批人参考。 */
public record BarCatalogDeleteRequest(String reason) {
}
