package com.complex.platform.compliance.dto;

import java.time.LocalDate;
import java.util.List;

public record ContractKeyDateRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String contractId,
        String merchantId,
        String dateType,
        String title,
        LocalDate dueDate,
        List<Integer> reminderDays,
        String status,
        String ownerUserId,
        String sourceType,
        boolean humanConfirmed
) {
}
