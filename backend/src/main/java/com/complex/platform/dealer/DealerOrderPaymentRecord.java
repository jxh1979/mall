package com.complex.platform.dealer;

import java.time.Instant;

/** 经销订单到账记录。method：bank_transfer 对公转账 / platform_online 平台线上支付（ENUM:176）；status：pending_verification / verified / rejected / late。 */
public record DealerOrderPaymentRecord(
        String id,
        String orderId,
        String method,
        long amountCent,
        Instant receivedAt,
        String referenceNo,
        String status,
        long appliedCent,
        String registeredBy,
        String verifiedBy,
        Instant verifiedAt,
        String note,
        Instant createdAt,
        Instant updatedAt
) {
}
