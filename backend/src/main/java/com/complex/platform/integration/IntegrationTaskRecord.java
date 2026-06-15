package com.complex.platform.integration;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record IntegrationTaskRecord(
        String id,
        String taskCategory,
        String providerCode,
        String taskType,
        String businessType,
        String externalBusinessId,
        String status,
        int retryCount,
        int maxRetryCount,
        Instant nextRetryAt,
        String operatorUserId,
        String resolutionNote,
        Map<String, Object> payload,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt
) {
}
