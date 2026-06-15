package com.complex.platform.amusement;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AmusementSafetyInspectionRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String projectId,
        String templateId,
        List<Map<String, Object>> items,
        String overallResult,
        Instant inspectedAt
) {
}
