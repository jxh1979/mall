package com.complex.platform.amusement.dto;

import java.time.Instant;
import java.util.List;

public record AmusementIncidentCreateRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String projectId,
        String amusementProjectId,
        String sessionId,
        String deviceId,
        String customerId,
        String severity,
        String title,
        String description,
        String reportedByUserId,
        String reportedChannel,
        Instant occurredAt
) {
}
