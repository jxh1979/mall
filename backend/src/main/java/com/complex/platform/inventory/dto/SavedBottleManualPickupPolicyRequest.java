package com.complex.platform.inventory.dto;

// 文件版本：V0.01

/**
 * FS-016 人工例外取酒消费历史条件配置（BSM L1073「安全阈值，按项目可配，调整须最高权限审批并留痕……须同时配置上下限」）：
 * 三个条件值与上下限须同时填写，任一条件值落在 [min_orders, max_orders] 内。
 */
public record SavedBottleManualPickupPolicyRequest(
        String idempotencyKey,
        String projectId,
        Integer ordersWithin1m,
        Integer ordersWithin3m,
        Integer ordersTotal,
        Integer minOrders,
        Integer maxOrders,
        String reason
) {
}
