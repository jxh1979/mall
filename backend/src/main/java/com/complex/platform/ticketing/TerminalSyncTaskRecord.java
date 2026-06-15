package com.complex.platform.ticketing;

import java.time.Instant;
import java.util.Map;

public record TerminalSyncTaskRecord(
        String id,
        String deviceCode,
        String staffId,
        String clientTaskId,
        String taskType,
        String status,
        String reason,
        String verificationId,
        String verificationNo,
        String occurredAt,
        Map<String, String> payload,
        Instant createdAt
) {
}
