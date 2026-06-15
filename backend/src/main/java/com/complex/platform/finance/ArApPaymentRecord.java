package com.complex.platform.finance;

import java.time.Instant;

public record ArApPaymentRecord(
        String id,
        String arApItemId,
        long amountCent,
        String paymentMethod,
        Instant paymentTime,
        String bankFlowRef,
        Instant createdAt
) {
}
