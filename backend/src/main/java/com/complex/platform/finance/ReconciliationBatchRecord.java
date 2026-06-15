package com.complex.platform.finance;

import java.time.Instant;
import java.time.LocalDate;

public record ReconciliationBatchRecord(
        String id,
        String batchNo,
        String scopeType,
        String scopeId,
        LocalDate businessDate,
        String status,
        Instant createdAt
) {
}
