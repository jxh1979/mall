package com.complex.platform.governance;

import java.time.Instant;
import java.time.LocalDate;

public record BusinessEntityBindingRecord(
        String id,
        String merchantId,
        String operationEntityId,
        String contractEntityId,
        String settlementEntityId,
        String status,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String changeReason,
        Instant createdAt,
        Instant updatedAt
) {
}
