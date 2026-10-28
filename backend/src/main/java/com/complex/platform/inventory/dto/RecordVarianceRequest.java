package com.complex.platform.inventory.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * V108/V126 录差异行请求。variance_qty = counted_qty − book_qty（带符号），review_status=pending。
 *
 * <p>{@code itemId}/{@code reason} 必填；{@code reason} 取 damage/shortage/overage/supplier_fault。
 * {@code varianceAmountCent} 可显式传入（带符号，分）；缺省时按 {@code unitCostCents}（成本单价，分）估算
 * = round(variance_qty × unitCostCents)。{@code supplierId} 仅 supplier_fault 责任供应商时填。
 * <p>第十八批第 24 条 24-R4：高价值物品（最近一次实际到货价大于或等于阈值、手工标记或阈值缺失）差异不为 0 时
 * {@code photoAttachmentIds}、{@code videoAttachmentIds} 必填（照片＋视频双证据）；低价值不要求。
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
        Long unitCostCents,
        List<String> photoAttachmentIds,
        List<String> videoAttachmentIds
) {
    public RecordVarianceRequest(String idempotencyKey, String itemId, String batchNo, BigDecimal bookQty,
            BigDecimal countedQty, String reason, String supplierId, Long varianceAmountCent, Long unitCostCents) {
        this(idempotencyKey, itemId, batchNo, bookQty, countedQty, reason, supplierId, varianceAmountCent, unitCostCents,
                null, null);
    }
}
