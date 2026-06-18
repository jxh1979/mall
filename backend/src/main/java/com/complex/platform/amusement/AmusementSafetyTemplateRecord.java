package com.complex.platform.amusement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AmusementSafetyTemplateRecord(
        String id,
        String templateName,
        List<Map<String, Object>> items,
        LocalDate effectiveFrom,
        String status,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt,
        @JsonIgnore
        String merchantId
) {
}
