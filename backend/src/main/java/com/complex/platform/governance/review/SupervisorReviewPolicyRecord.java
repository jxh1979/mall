package com.complex.platform.governance.review;

import java.util.List;

public record SupervisorReviewPolicyRecord(
        String policyCode,
        String actionType,
        long minAmountCent,
        int requiredLevel,
        boolean blocksByDefault,
        List<String> requiredEvidenceTypes,
        String status,
        String approvalLevel
) {
}
