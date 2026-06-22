package com.complex.platform.amusement;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AmusementSafetyInspectionRecord(
        String id,
        String projectId,
        String deviceId,
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
        Instant updatedAt,
        // V114 结构化补强：巡检人 + 结构化巡检结论 + 异常标记/说明（沿用增补口径，与既有 overall_result/operator_note 并存）。
        String inspectorId,
        String inspectionResult,
        boolean abnormalFound,
        String abnormalNote
) {
}
