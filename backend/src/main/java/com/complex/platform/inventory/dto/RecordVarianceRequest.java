package com.complex.platform.inventory.dto;

import java.math.BigDecimal;

/**
 * V108/V126 录差异行请求。variance_qty = counted_qty − book_qty（带符号），review_status=pending。
 *
 * <p>{@code itemId}/{@code reason} 必填；{@code reason} 取 damage/shortage/overage/supplier_fault。
 * {@code varianceAmountCent} 可显式传入（带符号，分）；缺省时按 {@code unitCostCents}（成本单价，分）估算
 * = round(variance_qty × unitCostCents)。{@code supplierId} 仅 supplier_fault 责任供应商时填。
 */
public record RecordVarianceRequest(
        String idempotencyKey,
        String itemId,
        String batchNo,
        BigDecimal bookQty,
        BigDecimal countedQty,
        String reason,
        String supplierId,
        Long varianceAmountCent,
        Long unitCostCents
) {
}
