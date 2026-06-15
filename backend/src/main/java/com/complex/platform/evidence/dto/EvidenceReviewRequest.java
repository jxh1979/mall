package com.complex.platform.evidence.dto;

import java.util.List;

public record EvidenceReviewRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String businessType,
        String businessId,
        String reviewResult,
        String comment
) {
}
