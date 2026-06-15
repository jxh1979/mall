package com.complex.platform.amusement;

import java.time.Instant;
import java.util.List;

public record AmusementIncidentRecord(
        String id,
        String projectId,
        String amusementProjectId,
        String sessionId,
        String deviceId,
        String customerId,
        String severity,
        String status,
        String title,
        String description,
        String reportedByUserId,
        String reportedChannel,
        Instant occurredAt,
        String resolution,
        Instant closedAt,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt
) {

    public AmusementIncidentRecord transitionTo(String targetStatus, Instant now) {
        return new AmusementIncidentRecord(
                id,
                projectId,
                amusementProjectId,
                sessionId,
                deviceId,
                customerId,
                severity,
                targetStatus,
                title,
                description,
                reportedByUserId,
                reportedChannel,
                occurredAt,
                resolution,
                closedAt,
                evidenceAttachmentIds,
                createdAt,
                now
        );
    }

    public AmusementIncidentRecord close(String resolution, Instant now) {
        return new AmusementIncidentRecord(
                id,
                projectId,
                amusementProjectId,
                sessionId,
                deviceId,
                customerId,
                severity,
                "closed",
                title,
                description,
                reportedByUserId,
                reportedChannel,
                occurredAt,
                resolution,
                now,
                evidenceAttachmentIds,
                createdAt,
                now
        );
    }
}
