package com.complex.platform.amusement.dto;

import java.util.List;

public record AmusementIncidentActionRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String targetStatus,
        String operatorUserId
) {
}
