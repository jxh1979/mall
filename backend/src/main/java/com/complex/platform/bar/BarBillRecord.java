package com.complex.platform.bar;

import java.time.Instant;

public record BarBillRecord(
        String id,
        String billNo,
        String orderId,
        String barId,
        String merchantId,
        String tableId,
        String reservationId,
        long subtotalAmountCent,
        long discountAmountCent,
        long depositDeductAmountCent,
        long payableAmountCent,
        long paidAmountCent,
        String currency,
        String paymentStatus,
        String settlementStatus,
        String salesStaffId,
        String salesRoleType,
        Instant createdAt,
        Instant updatedAt,
        // V091 反结：状态机 + 责任人/时点/原因/次数。
        boolean reopened,
        String reopenedByUserId,
        Instant reopenedAt,
        String reopenReason,
        int reopenCount,
        // V091 小费：进账单 payable 与结算（代收口径）。
        long tipAmountCent,
        // V231 强制口径：账单支付/结账成功时刻（payment_status→paid/partial_paid 时写入）；未结账为 null。与 createdAt（开单时间）严格区分。
        Instant paidAt
) {

    /** V231 兼容构造器（26 参，无 paidAt）：paidAt 默认 null（既有非结算调用方零改动）。 */
    public BarBillRecord(
            String id,
            String billNo,
            String orderId,
            String barId,
            String merchantId,
            String tableId,
            String reservationId,
            long subtotalAmountCent,
            long discountAmountCent,
            long depositDeductAmountCent,
            long payableAmountCent,
            long paidAmountCent,
            String currency,
            String paymentStatus,
            String settlementStatus,
            String salesStaffId,
            String salesRoleType,
            Instant createdAt,
            Instant updatedAt,
            boolean reopened,
            String reopenedByUserId,
            Instant reopenedAt,
            String reopenReason,
            int reopenCount,
            long tipAmountCent
    ) {
        this(id, billNo, orderId, barId, merchantId, tableId, reservationId,
                subtotalAmountCent, discountAmountCent, depositDeductAmountCent,
                payableAmountCent, paidAmountCent, currency, paymentStatus, settlementStatus,
                salesStaffId, salesRoleType, createdAt, updatedAt,
                reopened, reopenedByUserId, reopenedAt, reopenReason, reopenCount, tipAmountCent, null);
    }

    /** 向后兼容构造器（V091 反结/小费前签名）：既有调用方零改动，新字段默认未反结、小费为 0、paidAt 为 null。 */
    public BarBillRecord(
            String id,
            String billNo,
            String orderId,
            String barId,
            String merchantId,
            String tableId,
            String reservationId,
            long subtotalAmountCent,
            long discountAmountCent,
            long depositDeductAmountCent,
            long payableAmountCent,
            long paidAmountCent,
            String currency,
            String paymentStatus,
            String settlementStatus,
            String salesStaffId,
            String salesRoleType,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, billNo, orderId, barId, merchantId, tableId, reservationId,
                subtotalAmountCent, discountAmountCent, depositDeductAmountCent,
                payableAmountCent, paidAmountCent, currency, paymentStatus, settlementStatus,
                salesStaffId, salesRoleType, createdAt, updatedAt,
                false, null, null, null, 0, 0L, null);
    }
}
