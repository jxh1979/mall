package com.complex.platform.amusement;

import java.time.Instant;
import java.util.Map;

public record AmusementProjectRecord(
        String id,
        String projectName,
        int capacity,
        String openStatus,
        String safetyTemplateId,
        Map<String, Object> eligibilityRule,
        Instant createdAt,
        Instant updatedAt
) {
}
