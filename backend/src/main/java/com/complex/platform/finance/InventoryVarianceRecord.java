package com.complex.platform.finance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 库存差异明细（P1#13 / R6-D08）。finance 只读差异并入账，盘点流程由供应链域写入。
 * <p>varianceQty/varianceAmountCent 带符号：负=盘亏/报损，正=盘盈。reason：damage/shortage/overage/supplier_fault。
 * reviewStatus：pending|approved|rejected。postedArApItemId 非空表示已入账（幂等防重）。
 */
public record InventoryVarianceRecord(
        String id,
        String stocktakeId,
        String itemId,
        String batchNo,
        BigDecimal bookQty,
        BigDecimal countedQty,
        BigDecimal varianceQty,
        long varianceAmountCent,
        String reason,
        String supplierId,
        String reviewStatus,
        String postedArApItemId,
        @JsonIgnore String merchantId,
        Instant createdAt
) {
}
