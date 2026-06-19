package com.complex.platform.inventory.dto;

/**
 * R5 T6 采购单通用流转请求（下单 order / 到货 arrive）。仅幂等键 + 留痕备注。
 */
public record PurchaseRequestTransitionRequest(
        String idempotencyKey,
        String note
) {
}
