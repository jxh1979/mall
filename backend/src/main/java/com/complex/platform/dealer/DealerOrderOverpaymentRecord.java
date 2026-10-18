package com.complex.platform.dealer;

// 文件版本：V0.01

import java.time.Instant;

/** BSM:1228 超收：status unconfirmed 未确认用途 / prepayment 转同一销售主体预付款 / offset_applied 抵扣指定订单。 */
public record DealerOrderOverpaymentRecord(
        String id,
        String orderId,
        String dealerId,
        String salesEntityId,
        String paymentId,
        long amountCent,
        String status,
        String targetOrderId,
        String confirmedBy,
        Instant confirmedAt,
        String note,
        Instant createdAt,
        long unconfirmedAmountCent,
        long prepaymentBalanceCent,
        long appliedAmountCent,
        java.util.List<DealerOverpaymentAllocationRecord> allocations
) {
}
