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
        Instant updatedAt
) {
}
