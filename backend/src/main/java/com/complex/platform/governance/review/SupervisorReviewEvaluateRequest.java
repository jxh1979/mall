package com.complex.platform.governance.review;

import java.util.List;

public record SupervisorReviewEvaluateRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String actionType,
        String objectType,
        String objectId,
        Long amountCent,
        String violationSummary
) {
}
