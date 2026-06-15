package com.complex.platform.finance.dto;

import java.time.Instant;
import java.util.List;

public record OfflinePaymentConfirmRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String paymentId,
        Long amountCent,
        String paymentMethod,
        Instant confirmedAt
) {
}
