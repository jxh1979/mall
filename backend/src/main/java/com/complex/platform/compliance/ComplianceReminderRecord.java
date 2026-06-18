package com.complex.platform.compliance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.time.LocalDate;

public record ComplianceReminderRecord(
        String id,
        @JsonIgnore String merchantId,
        String sourceType,
        String sourceId,
        String reminderType,
        LocalDate dueDate,
        LocalDate reminderAt,
        String severity,
        String status,
        boolean blockingSuggested,
        String message,
        Instant generatedAt
) {
}
