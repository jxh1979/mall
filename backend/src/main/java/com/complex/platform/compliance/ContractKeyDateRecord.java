package com.complex.platform.compliance;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ContractKeyDateRecord(
        String id,
        String contractId,
        String merchantId,
        String dateType,
        String title,
        LocalDate dueDate,
        List<Integer> reminderDays,
        String status,
        String ownerUserId,
        String sourceType,
        boolean humanConfirmed,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt
) {
}
