package com.complex.platform.bar;

import java.util.List;

public record BarBillSplitRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        Long splitAmountCent,
        String reason,
        String requestedByUserId
) {
}
