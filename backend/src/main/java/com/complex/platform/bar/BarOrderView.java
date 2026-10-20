package com.complex.platform.bar;

// 文件版本：V0.01

import java.time.Instant;
import java.util.List;

/**
 * 订单列表读模型：订单本体字段（与 BarOrderRecord 同名同序，SNAKE_CASE 序列化后前端读取形状不变）
 * 加该单账单摘要 bills（created_at 倒序，最新在前）。修复前 bills 是后端从不返回的幽灵字段。
 */
public record BarOrderView(
        String id,
        String barId,
        String merchantId,
        String tableId,
        String reservationId,
        String customerId,
        String orderSource,
        String status,
        long totalAmountCent,
        String currency,
        String createdTerminal,
        String deviceId,
        String salesStaffId,
        Instant createdAt,
        Instant updatedAt,
        String orderType,
        Instant paidAt,
        List<BarBillSummary> bills,
        // 第十批 TK-26-R2（BSM L1025「分别标注已上菜与未上菜」）：明细随单带出，后台按明细勾选送达。
        List<BarOrderItemRecord> items
) {
    static BarOrderView of(BarOrderRecord order, List<BarBillSummary> bills, List<BarOrderItemRecord> items) {
        return new BarOrderView(
                order.id(), order.barId(), order.merchantId(), order.tableId(), order.reservationId(),
                order.customerId(), order.orderSource(), order.status(), order.totalAmountCent(), order.currency(),
                order.createdTerminal(), order.deviceId(), order.salesStaffId(), order.createdAt(), order.updatedAt(),
                order.orderType(), order.paidAt(), bills, items);
    }
}
