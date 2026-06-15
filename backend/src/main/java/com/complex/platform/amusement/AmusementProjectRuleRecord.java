package com.complex.platform.amusement;

import java.time.Instant;
import java.util.List;

public record AmusementProjectRuleRecord(
        String id,
        String projectId,
        String ruleType,
        String ruleValue,
        String effectiveFrom,
        String effectiveTo,
        boolean blocking,
        String status,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt
) {
}
