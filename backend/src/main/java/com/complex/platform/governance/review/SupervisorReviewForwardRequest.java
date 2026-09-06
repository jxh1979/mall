package com.complex.platform.governance.review;

/** M007-A-02（PGM-1.7.1 转发）：把待复核单逐单转发给指定人选；不得转给申请人本人或直接受益人。 */
public record SupervisorReviewForwardRequest(
        String idempotencyKey,
        String operatorNote,
        String forwardedTo,
        String comment
) {
}
