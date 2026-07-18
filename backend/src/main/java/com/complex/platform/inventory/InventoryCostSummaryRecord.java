package com.complex.platform.inventory;

/**
 * V296 期间出库成本汇总（将来分红引擎的成本侧取数点）。
 *
 * <p>窗口 = [period_start 经营日起, period_end+1 经营日起)（平台全局经营日切分，与财务日结/跨域汇总同口径）；
 * 出库行 = 仅源库位的流水（销售领用/盘亏/报损/积分兑换扣减等），调拨与入库不计入。
 * noCostBasisCount = 窗口内成本列为 NULL（无成本基准）的出库笔数，提示合计存在低估。
 */
public record InventoryCostSummaryRecord(
        String merchantId,
        String periodStart,
        String periodEnd,
        long outboundTotalCostCent,
        long outboundMovementCount,
        long noCostBasisCount
) {
}
