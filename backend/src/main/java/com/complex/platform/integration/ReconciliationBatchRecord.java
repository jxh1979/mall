package com.complex.platform.integration;

import java.time.Instant;
import java.time.LocalDate;

public record ReconciliationBatchRecord(
        String id,
        String providerCode,
        String businessType,
        LocalDate periodStart,
        LocalDate periodEnd,
        String status,
        int expectedCount,
        int actualCount,
        int differenceCount,
        long totalDifferenceCent,
        Instant createdAt,
        Instant updatedAt
) {
}
