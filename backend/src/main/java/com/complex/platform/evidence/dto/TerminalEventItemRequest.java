package com.complex.platform.evidence.dto;

import java.time.Instant;
import java.util.Map;

public record TerminalEventItemRequest(
        String eventType,
        String eventCategory,
        String result,
        String summary,
        Map<String, Object> payload,
        Instant occurredAt
) {
}
