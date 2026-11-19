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
        String initiatedBy,
        String approvalLevel,
        Integer requiredApproverRank
) {

    /** 第十八批 BA 段第 353 条 353-R1：只换凭证的同值记录（中间节点并入审批人所附凭证时用）。 */
    public SupervisorReviewDecisionRecord withEvidenceAttachmentIds(List<String> ids) {
        return new SupervisorReviewDecisionRecord(reviewId, policyCode, actionType, objectType, objectId, blocked, reviewStatus,
                requiredLevel, reviewerLevel, decision, reason, ids, evaluatedAt, initiatedBy, approvalLevel, requiredApproverRank);
    }
}
