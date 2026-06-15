package com.complex.platform.finance;

import java.time.Instant;

public record ReconciliationDifferenceRecord(
        String id,
        String batchId,
        String differenceType,
        String businessRef,
        long amountCent,
        String status,
        Instant createdAt
) {
}
