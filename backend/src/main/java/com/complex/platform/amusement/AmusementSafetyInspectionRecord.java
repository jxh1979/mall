package com.complex.platform.amusement;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AmusementSafetyInspectionRecord(
        String id,
        String projectId,
        String templateId,
        List<Map<String, Object>> items,
        String overallResult,
        String inspectionStatus,
        String rectificationStatus,
        String reopenStatus,
        Instant inspectedAt,
        Instant submittedAt,
        Instant recheckedAt,
        Instant approvedAt,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt
) {
}
