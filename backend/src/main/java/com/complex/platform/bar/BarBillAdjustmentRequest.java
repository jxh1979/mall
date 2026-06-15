package com.complex.platform.bar;

import java.util.List;

public record BarBillAdjustmentRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String adjustmentType,
        Long amountCent,
        String reason,
        String requestedByUserId
) {
}
