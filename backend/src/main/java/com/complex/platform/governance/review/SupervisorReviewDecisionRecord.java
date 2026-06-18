package com.complex.platform.governance.review;

import java.time.Instant;
import java.util.List;

public record SupervisorReviewDecisionRecord(
        String reviewId,
        String policyCode,
        String actionType,
        String objectType,
        String objectId,
        boolean blocked,
        String reviewStatus,
        int requiredLevel,
        Integer reviewerLevel,
        String decision,
        String reason,
        List<String> evidenceAttachmentIds,
        Instant evaluatedAt,
        String initiatedBy
) {
}
