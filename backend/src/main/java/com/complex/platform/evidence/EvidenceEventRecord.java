package com.complex.platform.evidence;

import java.time.Instant;
import java.util.Map;

public record EvidenceEventRecord(
        String id,
        String deviceCode,
        String staffId,
        String eventType,
        String eventCategory,
        String result,
        String summary,
        Map<String, Object> payload,
        Instant occurredAt,
        Instant receivedAt
) {
}
