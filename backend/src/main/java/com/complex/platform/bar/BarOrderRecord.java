package com.complex.platform.bar;

import java.time.Instant;

public record BarOrderRecord(
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
        // R5 T15：订单类型 standard / gift（0 元赠饮单不计商户收入但进账单/留痕）。
        String orderType,
        // V231 强制口径：订单支付成功时刻。酒吧订单支付走账单结算，结账成功时由结算路径回填此订单 paid_at；未支付为 null。
        Instant paidAt
) {
    /** V231 兼容构造器（16 参，含 orderType 无 paidAt）：paidAt 默认 null。 */
    public BarOrderRecord(
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
            String orderType
    ) {
        this(id, barId, merchantId, tableId, reservationId, customerId, orderSource, status,
                totalAmountCent, currency, createdTerminal, deviceId, salesStaffId, createdAt, updatedAt, orderType, null);
    }

    /** 向后兼容构造：未显式指定 order_type 的既有调用默认 'standard'。 */
    public BarOrderRecord(
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
            Instant updatedAt
    ) {
        this(id, barId, merchantId, tableId, reservationId, customerId, orderSource, status,
                totalAmountCent, currency, createdTerminal, deviceId, salesStaffId, createdAt, updatedAt, "standard", null);
    }
}
