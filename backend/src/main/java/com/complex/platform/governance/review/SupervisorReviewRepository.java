package com.complex.platform.governance.review;

import java.time.Instant;
import java.util.Optional;

public interface SupervisorReviewRepository {

    SupervisorReviewDecisionRecord saveSupervisorReview(SupervisorReviewDecisionRecord record);

    Optional<SupervisorReviewDecisionRecord> findPendingSupervisorReviewById(String reviewId);

    /** M007-A-01：同动作、同对象、同发起人且已批准未放行的复核单（供准入闸消费）。 */
    default Optional<SupervisorReviewDecisionRecord> findApprovedUnreleasedSupervisorReview(
            String actionType, String objectType, String objectId, String initiatedBy) {
        return Optional.empty();
    }

    /** M007-A-01：把已批准复核单标记为已放行；只在尚未放行时成功（一次批准只放行一次）。 */
    default boolean markSupervisorReviewReleased(String reviewId, String releasedBy, Instant releasedAt) {
        return false;
    }
}
