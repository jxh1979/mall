package com.complex.platform.bar;

/** 目录实体删除审批：decision=approved|rejected；comment 可空。 */
public record BarCatalogDeleteApprovalRequest(String decision, String comment) {
}
