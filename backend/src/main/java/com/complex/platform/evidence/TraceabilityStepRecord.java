package com.complex.platform.evidence;

import java.time.Instant;

/** 业务过程步骤记录（留痕中层，REQ-40）。capture_source 用于统计自动留痕率（auto_captured）。 */
public record TraceabilityStepRecord(
        String id,
        String processInstanceId,
        String stepCode,
        String stepName,
        String actorType,
        String actorId,
        String terminalType,
        String deviceId,
        String locationPointId,
        String eventId,
        boolean autoCaptured,
        String captureSource,
        boolean requiresManualReason,
        String result,
        Instant occurredAt
) {
}
