package com.complex.platform.compliance.dto;

import java.time.LocalDate;

public record ComplianceReminderScanRequest(
        String idempotencyKey,
        String operatorNote,
        LocalDate scanDate,
        Integer defaultLookaheadDays,
        String operatorUserId
) {
}
