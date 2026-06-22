package com.complex.platform.amusement;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AmusementSafetyInspectionRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String projectId,
        // 设备业务码（可空=项目级巡检；与核销 deviceId 同口径）。
        String deviceId,
        String templateId,
        List<Map<String, Object>> items,
        String overallResult,
        Instant inspectedAt,
        // V114 结构化补强：巡检人 + 异常说明（可空；inspectionResult 派生自 overallResult，无需单独传）。
        String inspectorId,
        String abnormalNote
) {
}
