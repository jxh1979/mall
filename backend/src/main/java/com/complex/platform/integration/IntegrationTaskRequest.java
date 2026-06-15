package com.complex.platform.integration;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record IntegrationTaskRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String providerCode,
        String taskType,
        String businessType,
        String externalBusinessId,
        String status,
        Integer maxRetryCount,
        Instant nextRetryAt,
        Map<String, Object> payload
) {
}
