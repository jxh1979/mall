package com.complex.platform.amusement;

import java.time.Instant;
import java.util.List;

public record AmusementIncidentActionRecord(
        String id,
        String incidentId,
        String actionType,
        String fromStatus,
        String toStatus,
        String operatorUserId,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        Instant createdAt
) {
}
