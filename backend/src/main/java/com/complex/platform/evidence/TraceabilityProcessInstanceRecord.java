package com.complex.platform.evidence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/** 业务过程实例（留痕三层模型顶层，REQ-34）。merchant_id/project_id 为隔离键，由 token 注入。 */
public record TraceabilityProcessInstanceRecord(
        String id,
        String processType,
        String businessObjectType,
        String businessObjectId,
        String projectId,
        @JsonIgnore String merchantId,
        String venueId,
        String areaId,
        String locationPointId,
        String startedEventId,
        String currentStepCode,
        String status,
        Instant startedAt,
        Instant completedAt
) {
}
