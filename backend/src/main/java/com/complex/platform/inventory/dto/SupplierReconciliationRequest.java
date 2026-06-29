package com.complex.platform.inventory.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.util.List;

public record SupplierReconciliationRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String supplierId,
        LocalDate periodStart,
        LocalDate periodEnd,
        @NotNull @PositiveOrZero Long expectedAmountCent,
        @NotNull @PositiveOrZero Long actualAmountCent,
        String status,
        String operatorUserId
) {
}
