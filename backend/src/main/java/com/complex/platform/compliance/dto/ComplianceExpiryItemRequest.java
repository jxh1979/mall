package com.complex.platform.compliance.dto;

import java.time.LocalDate;
import java.util.List;

public record ComplianceExpiryItemRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String subjectType,
        String subjectId,
        String merchantId,
        String itemType,
        String itemName,
        String documentNo,
        String issuingAuthority,
        LocalDate validFrom,
        LocalDate validTo,
        List<Integer> reminderDays,
        String expiryAction,
        String status,
        String ownerUserId,
        String sourceType,
        boolean humanConfirmed
) {
}
