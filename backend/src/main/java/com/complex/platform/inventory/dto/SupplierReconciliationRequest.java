package com.complex.platform.inventory.dto;

import java.time.LocalDate;
import java.util.List;

public record SupplierReconciliationRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String supplierId,
        LocalDate periodStart,
        LocalDate periodEnd,
        Long expectedAmountCent,
        Long actualAmountCent,
        String status,
        String operatorUserId
) {
}
