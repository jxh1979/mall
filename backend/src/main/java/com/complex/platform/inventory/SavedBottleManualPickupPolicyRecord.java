package com.complex.platform.inventory;

// 文件版本：V0.02

/**
 * FS-016 人工例外消费历史条件（saved_bottle_manual_pickup_policy，按项目配置；未配置的项目回落 project_id=global 出厂行：
 * 2 / 3 / 5 笔，上下限留空，首次改参时与条件值同填）。
 * BSM L1073：「近 1 个月内至少 N 笔、近 3 个月内至少 N 笔、或历史累计至少 N 笔有效卡座消费订单」，任一满足即可。
 */
public record SavedBottleManualPickupPolicyRecord(
        String projectId,
        int ordersWithin1m,
        int ordersWithin3m,
        int ordersTotal,
        Integer minOrders,
        Integer maxOrders
) {
}
