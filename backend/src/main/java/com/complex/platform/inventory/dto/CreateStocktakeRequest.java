package com.complex.platform.inventory.dto;

/**
 * V108/V126 盘点建单请求。建盘点单（status=counting），幂等。
 *
 * <p>{@code warehouseId} 必填；{@code businessDate} 盘点经营日（缺省取当日）；{@code remark} 备注可空。
 * merchantId 不由请求体传入，强制取自认证上下文（归属隔离）。
 */
public record CreateStocktakeRequest(
        String idempotencyKey,
        String warehouseId,
        java.time.LocalDate businessDate,
        String remark
) {
}
