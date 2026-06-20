package com.complex.platform.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * V108 库存差异明细（库存域写入侧）。与 finance 包内同名只读投影对应同一表 inventory_variance，
 * 本类承载库存域建单/录差异/复核写路径；finance 侧只读差异并入账（review_status='approved' 后）。
 *
 * <p>varianceQty/varianceAmountCent 带符号：负=盘亏/报损，正=盘盈。reason：damage/shortage/overage/supplier_fault。
 * reviewStatus：pending|approved|rejected。reviewedBy/reviewedAt 复核留痕。postedArApItemId 非空表示已入账。
 * merchantId 经 stocktake 归属（隔离用），随明细冗余便于范围查询与审计。
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
        String reviewedBy,
        Instant reviewedAt,
        String postedArApItemId,
        @JsonIgnore String merchantId,
        Instant createdAt,
        Instant updatedAt
) {
}
