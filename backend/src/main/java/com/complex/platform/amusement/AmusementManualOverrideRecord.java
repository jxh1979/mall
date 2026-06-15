package com.complex.platform.amusement;

import java.time.Instant;
import java.util.List;

public record AmusementManualOverrideRecord(
        String id,
        String projectId,
        String sessionId,
        String overrideType,
        String reason,
        String status,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        Instant createdAt
) {
}
