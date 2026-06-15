package com.complex.platform.analytics;

public record AiQualitySuggestionRequest(
        String idempotencyKey,
        String sourceType,
        String sourceId,
        String sourceText,
        String requestedBy
) {
}
