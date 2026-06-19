package com.complex.platform.finance;

import java.time.Instant;

public record FinanceRefundRecord(
        String id,
        String targetId,
        long refundAmountCent,
        String reason,
        String refundChannel,
        // P2#④ 统一退款方向维度：original / cash / third_party / mixed。
        String refundDirection,
        String status,
        Instant createdAt
) {
}
