package com.complex.platform.bar;

/** 目录实体删除审批结果：id + decision(approved|rejected) + 是否已物理删除。 */
public record BarCatalogDeleteDecision(String id, String decision, boolean deleted) {
}
