package com.complex.platform.finance.dto;

import java.util.List;

/**
 * P1#13 / R6-D08：库存差异经复核入账成 ar_ap_items 的写端点请求。
 * <p>关键约束：金额差(varianceAmountCent)、原因(reason)、责任供应商(supplierId) 由服务层从 inventory_variance
 * 行只读取数，请求体只标识待入账差异行 + 入账对手方（盘亏/报损为内部损益对手方，供应商责任为供应商 AP）。
 */
public record InventoryVariancePostRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String inventoryVarianceId,
        String counterpartyId
) {
}
