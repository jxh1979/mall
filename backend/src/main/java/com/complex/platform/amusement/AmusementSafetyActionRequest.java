package com.complex.platform.amusement;

import java.util.List;

public record AmusementSafetyActionRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String actionResult,
        String comment,
        List<String> attachmentIds
) {
}
