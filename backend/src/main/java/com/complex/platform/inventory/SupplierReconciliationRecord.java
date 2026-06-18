package com.complex.platform.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SupplierReconciliationRecord(
        String id,
        String supplierId,
        LocalDate periodStart,
        LocalDate periodEnd,
        long expectedAmountCent,
        long actualAmountCent,
        long differenceAmountCent,
        String status,
        List<String> evidenceAttachmentIds,
        String operatorUserId,
        Instant createdAt,
        Instant updatedAt,
        @JsonIgnore String merchantId
) {
}
