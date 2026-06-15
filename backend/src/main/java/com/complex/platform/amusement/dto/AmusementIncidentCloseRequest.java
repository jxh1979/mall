package com.complex.platform.amusement.dto;

import java.util.List;

public record AmusementIncidentCloseRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String resolution,
        String closedByUserId
) {
}
