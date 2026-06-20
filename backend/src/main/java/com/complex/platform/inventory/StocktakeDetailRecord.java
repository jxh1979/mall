package com.complex.platform.inventory;

import java.util.List;

/**
 * V108 盘点单详情视图（盘点单头 + 差异明细行）。GET /stocktakes/{id} 返回。
 */
public record StocktakeDetailRecord(
        StocktakeRecord stocktake,
        List<InventoryVarianceRecord> variances
) {
}
