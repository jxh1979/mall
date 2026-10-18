package com.complex.platform.finance;

// 文件版本：V0.01

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
        Instant createdAt,
        String sourceType,
        String merchantId,
        String projectId,
        String requestedBy,
        String originalPaymentRef,
        String gatewayProvider,
        String gatewayRef,
        Instant updatedAt,
        Instant succeededAt
) {
    public FinanceRefundRecord(String id, String targetId, long refundAmountCent, String reason,
            String refundChannel, String refundDirection, String status, Instant createdAt) {
        this(id, targetId, refundAmountCent, reason, refundChannel, refundDirection, status, createdAt,
                "ar_ap_payment", null, null, null, null, null, null, createdAt, null);
    }
}
