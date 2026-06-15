package com.complex.platform.ticketing.dto;

import java.util.Map;

public record TerminalSyncTaskItemRequest(
        String clientTaskId,
        String taskType,
        String occurredAt,
        Map<String, String> payload
) {
}
