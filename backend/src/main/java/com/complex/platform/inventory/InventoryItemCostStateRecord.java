package com.complex.platform.inventory;

import java.time.Instant;

/**
 * V296 移动加权成本状态行（商户+物料粒度；物料唯一归属商户，item_id 即主键）。
 *
 * <p>并发出入库对同一物料的更新经 SELECT FOR UPDATE 行锁串行化；hasCostBasis=false 表示
 * 该物料尚无成本基准（存量库存未录进货价），此时流水成本列记 NULL、均价按 0 处理，不编造数字。
 */
public record InventoryItemCostStateRecord(
        String itemId,
        String merchantId,
        long onHandQuantity,
        long totalCostCent,
        long avgUnitCostCent,
        long cumulativeInCostCent,
        long cumulativeOutCostCent,
        String costMethod,
        boolean hasCostBasis,
        Instant updatedAt
) {
}
