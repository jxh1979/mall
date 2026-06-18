package com.complex.platform.governance.review;

import java.util.Optional;

public interface SupervisorReviewRepository {

    SupervisorReviewDecisionRecord saveSupervisorReview(SupervisorReviewDecisionRecord record);

    Optional<SupervisorReviewDecisionRecord> findPendingSupervisorReviewById(String reviewId);
}
