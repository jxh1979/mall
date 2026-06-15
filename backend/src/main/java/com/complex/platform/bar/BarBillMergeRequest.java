package com.complex.platform.bar;

import java.util.List;

public record BarBillMergeRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String mergedBillId,
        String reason,
        String requestedByUserId
) {
}
