package com.complex.platform.governance.review;

import java.util.List;

public record SupervisorReviewApprovalRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        Integer reviewerLevel,
        String decision,
        String comment
) {
}
