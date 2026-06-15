package com.complex.platform.finance.dto;

import java.util.List;

public record PaymentIntentRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String businessOrderType,
        String businessOrderId,
        Long amountCent,
        String currency,
        String channel
) {
}
