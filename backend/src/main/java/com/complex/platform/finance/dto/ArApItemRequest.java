package com.complex.platform.finance.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public record ArApItemRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String direction,
        String counterpartyId,
        String sourceType,
        String sourceId,
        @NotNull @Positive Long amountCent,
        LocalDate dueDate,
        String accountingPeriod
) {
}
