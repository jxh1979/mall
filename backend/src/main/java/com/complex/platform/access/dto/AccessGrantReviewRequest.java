package com.complex.platform.access.dto;

import java.util.List;

public record AccessGrantReviewRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String decision,
        String comment
) {
}
