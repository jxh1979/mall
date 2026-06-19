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
        String orderType
) {
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
                totalAmountCent, currency, createdTerminal, deviceId, salesStaffId, createdAt, updatedAt, "standard");
    }
}
