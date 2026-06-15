package com.complex.platform.finance.dto;

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
        Long amountCent,
        LocalDate dueDate,
        String accountingPeriod
) {
}
