package com.complex.platform.governance.review;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

    /** M007-A-02（PGM-1.7.1 审批时限）：复核单排期——直接受益人、到期时刻、升级次数/时刻、转发人。 */
    record ReviewSchedule(String reviewId, String beneficiaryId, Instant dueAt, int escalationCount,
                          Instant escalatedAt, String forwardedTo) {
    }

    /** M007-A-02：待复核清单条目（决策 + 排期），供审批人查看卡在哪一节点、是否超时、是否已转发。 */
    record PendingReview(SupervisorReviewDecisionRecord decision, ReviewSchedule schedule) {
    }

    default void scheduleSupervisorReview(String reviewId, String beneficiaryId, Instant dueAt) {
    }

    default Optional<ReviewSchedule> findSupervisorReviewSchedule(String reviewId) {
        return Optional.empty();
    }

    default boolean forwardSupervisorReview(String reviewId, String forwardedTo, String forwardedBy, Instant forwardedAt) {
        return false;
    }

    default boolean escalateSupervisorReview(String reviewId, String approvalLevel, Integer requiredApproverRank,
                                             Instant dueAt, Instant escalatedAt) {
        return false;
    }

    default List<PendingReview> listPendingSupervisorReviews(int limit) {
        return List.of();
    }

    default List<PendingReview> listOverdueSupervisorReviews(Instant now, int limit) {
        return List.of();
    }

    /** 策略参数 parameters.deadlineWorkingDays.value（工作日），未配置为空。 */
    default Optional<Integer> findDeadlineWorkingDays(String policyCode) {
        return Optional.empty();
    }

    /** holiday_calendar 中落在 [from, to] 的全部日期（含调休假期），用于按工作日计算到期。 */
    default List<LocalDate> findHolidayDates(LocalDate from, LocalDate to) {
        return List.of();
    }
}
