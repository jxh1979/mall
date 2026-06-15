package com.complex.platform.integration;

import java.time.LocalDate;
import java.util.List;

public record ReconciliationBatchRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String providerCode,
        String businessType,
        LocalDate periodStart,
        LocalDate periodEnd,
        Integer expectedCount,
        Integer actualCount,
        Integer differenceCount,
        Long totalDifferenceCent
) {
}
