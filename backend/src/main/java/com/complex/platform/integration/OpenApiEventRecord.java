package com.complex.platform.integration;

import java.time.Instant;
import java.util.Map;

public record OpenApiEventRecord(
        String id,
        String endpointKey,
        String providerCode,
        String eventName,
        String idempotencyKey,
        boolean signatureValid,
        String eventStatus,
        Map<String, Object> payloadJson,
        Map<String, Object> resultJson,
        String failureReason,
        Instant receivedAt,
        Instant processedAt
) {
}
