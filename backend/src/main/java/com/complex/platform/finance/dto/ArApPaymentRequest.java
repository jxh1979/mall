package com.complex.platform.finance.dto;

import java.time.Instant;
import java.util.List;

public record ArApPaymentRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        Long amountCent,
        String paymentMethod,
        Instant paymentTime,
        String bankFlowRef
) {
}
