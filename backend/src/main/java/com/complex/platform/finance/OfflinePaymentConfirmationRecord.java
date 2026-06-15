package com.complex.platform.finance;

import java.time.Instant;
import java.util.List;

public record OfflinePaymentConfirmationRecord(
        String id,
        String paymentId,
        long amountCent,
        String paymentMethod,
        Instant confirmedAt,
        List<String> evidenceAttachmentIds,
        String status,
        Instant createdAt
) {
}
