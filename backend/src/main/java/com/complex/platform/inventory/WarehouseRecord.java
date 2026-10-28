package com.complex.platform.inventory;

import java.time.Instant;

/**
 * R5 T6 结构化库房。
 *
 * <p>{@code warehouseType} 六类：{@code main}/{@code bar}/{@code bar_counter}/{@code consumable}/
 * {@code gift}/{@code saved_bottle}。库存余额 {@code inventory_stock_balances.warehouse_id} 挂此库房。
 */
public record WarehouseRecord(
        String id,
        String projectId,
        String merchantId,
        String warehouseType,
        String name,
        String status,
        // 第十八批 C 段第 17 条（BSM 第 1065 行）：吧台是否配有出酒量采集设备（只对 bar_counter）。
        boolean pourDeviceEquipped,
        Instant createdAt,
        Instant updatedAt
) {
}
