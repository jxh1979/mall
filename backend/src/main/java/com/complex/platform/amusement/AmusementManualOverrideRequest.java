package com.complex.platform.amusement;

import java.util.List;

public record AmusementManualOverrideRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String projectId,
        String sessionId,
        String overrideType,
        String reason
) {
}
