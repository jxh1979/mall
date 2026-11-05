package com.complex.platform.bar;

// 文件版本：V0.02

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
        List<BarOrderItemRecord> items,
        // 第十八批 T 段第 157 条 157-R2：台位类型（只有包厢 vip 能挂起，第十八批裁决 37）与本单挂起状态、挂起金额（未挂起为空）。
        String tableType,
        String suspensionStatus,
        Long suspensionAmountCent,
        // 第十八批 T 段第 157 条 157-R3：本单挂起记录全文（未挂起为空），后台显示坏账固化的未收售价、库存成本与赔偿登记。
        BarOrderSuspensionService.Suspension suspension,
        // 第十八批 T 段第 157 条 157-R5 ⑥：本单账单（或挂起改指的主单）关台未付清待办的升级时间（未升级为空）；已升级的，后台对能进订单页的用户放开转坏账按钮，由后端按排班鉴权。
        Instant unpaidCloseEscalatedAt
) {
    static BarOrderView of(BarOrderRecord order, List<BarBillSummary> bills, List<BarOrderItemRecord> items) {
        return of(order, bills, items, null, null, null);
    }

    static BarOrderView of(BarOrderRecord order, List<BarBillSummary> bills, List<BarOrderItemRecord> items, String tableType,
            BarOrderSuspensionService.Suspension suspension, Instant unpaidCloseEscalatedAt) {
        return new BarOrderView(
                order.id(), order.barId(), order.merchantId(), order.tableId(), order.reservationId(),
                order.customerId(), order.orderSource(), order.status(), order.totalAmountCent(), order.currency(),
                order.createdTerminal(), order.deviceId(), order.salesStaffId(), order.createdAt(), order.updatedAt(),
                order.orderType(), order.paidAt(), bills, items, tableType,
                suspension == null ? null : suspension.status(), suspension == null ? null : suspension.amountCent(), suspension,
                unpaidCloseEscalatedAt);
    }
}
