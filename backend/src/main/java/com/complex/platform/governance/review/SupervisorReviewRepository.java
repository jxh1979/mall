package com.complex.platform.governance.review;

// 文件版本：V0.01

import com.complex.platform.governance.chain.ApprovalChainNode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SupervisorReviewRepository {

    SupervisorReviewDecisionRecord saveSupervisorReview(SupervisorReviewDecisionRecord record);

    Optional<SupervisorReviewDecisionRecord> findPendingSupervisorReviewById(String reviewId);

    /** M087-I5-01：任意状态的复核单（审批链页裁决授权申请类复核单后回读终态）。默认只回退到待复核查找。 */
    default Optional<SupervisorReviewDecisionRecord> findSupervisorReviewById(String reviewId) {
        return findPendingSupervisorReviewById(reviewId);
    }

    /**
     * M007-A-01：同动作、同对象、同发起人且已批准未放行的复核单（供准入闸消费）。
     * M724-F16（操作员 2026-10-07 裁决「批准只能被『被拦下的那次提交的同样内容』用掉」）：还须复核单上记的被拦下那次提交的请求内容摘要
     * 与本次相同；没有摘要的复核单不命中。
     */
    default Optional<SupervisorReviewDecisionRecord> findApprovedUnreleasedSupervisorReview(
            String actionType, String objectType, String objectId, String initiatedBy, String blockedContentDigest) {
        return Optional.empty();
    }

    /** M724-F16：在准入闸建的复核单上记下被拦下那次提交的请求内容摘要（SHA-256 十六进制 64 位，不含提交编号）。默认不记。 */
    default void bindSupervisorReviewToBlockedContent(String reviewId, String contentDigest) {
    }

    /** M007-A-01：把已批准复核单标记为已放行；只在尚未放行时成功（一次批准只放行一次）。 */
    default boolean markSupervisorReviewReleased(String reviewId, String releasedBy, Instant releasedAt) {
        return false;
    }

    /**
     * XM-ONESHOT-RETURN B01：放行时一并记下用掉它的那次请求（幂等键、请求摘要，取不到为空）。
     * 默认退回不记请求信息的写法（内存仓只实现了它，其重放照旧拦截）。
     */
    default boolean markSupervisorReviewReleased(
            String reviewId, String releasedBy, Instant releasedAt, String idempotencyKey, String requestDigest) {
        return markSupervisorReviewReleased(reviewId, releasedBy, releasedAt);
    }

    /**
     * XM-ONESHOT-RETURN B01、M724-F03：同动作、同对象的已批准复核单里，正是被这次请求（同幂等键、同请求摘要）放行用掉的那一张的放行人；
     * 没有这样的复核单为空。不限定当前操作人（同项目同商户的同事重复提交同样要认出）。默认为空（内存仓不记请求信息，其重放照旧拦截）。
     */
    default Optional<String> findReleaserOfSupervisorReviewReleasedToRequest(String actionType, String objectType, String objectId,
            String idempotencyKey, String requestDigest) {
        return Optional.empty();
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

    // ── M087 审批链引擎（PGM-1.7.1 / PGM-1.10.1）：组织层级、平行线设置、复核单节点 ──

    /** M087-A-03：平行审批线设置——该线最低起点层级（level_rank）与起点人（approval_lines）。 */
    record ApprovalLine(String lineCode, String lineName, int minStartRank, String entryUserId, String status, String hrFinalApproverId) {
        public ApprovalLine(String lineCode, String lineName, int minStartRank, String entryUserId, String status) {
            this(lineCode, lineName, minStartRank, entryUserId, status, null);
        }
    }

    /** 员工所属部门的 HR 负责人配置；未配置时不产生审批节点。 */
    default Optional<String> findDepartmentHrApprover(String userId) {
        return Optional.empty();
    }

    /** M087-A-01：直接上级（org_hierarchy）；未维护为空。 */
    default Optional<String> findDirectSuperior(String userId) {
        return Optional.empty();
    }

    default Optional<ApprovalLine> findApprovalLine(String lineCode) {
        return Optional.empty();
    }

    /** 审批线的全部绑定员工，用于核对整条线的影响范围，不受页面条数限制。 */
    default List<String> findApprovalLineUserIds(String lineCode) {
        return listOrgHierarchy(Integer.MAX_VALUE).stream().filter(row -> lineCode.equals(row.lineCode()))
                .map(OrgSuperior::userId).toList();
    }

    /** M087-A-02：复核单生成时一次性写入全部节点。 */
    default void saveChainNodes(String reviewId, List<ApprovalChainNode> nodes) {
    }

    default List<ApprovalChainNode> listChainNodes(String reviewId) {
        return List.of();
    }

    /** 更新单个节点（裁决 / 转发 / 升级）；节点不存在返回 false。 */
    default boolean updateChainNode(ApprovalChainNode node) {
        return false;
    }

    /** M087-I5-06：同一复核单的节点裁决串行化（JDBC 实现为复核单行锁）；默认不加锁。 */
    default void lockSupervisorReview(String reviewId) {
    }

    /** M087-A-01：组织层级一行（org_hierarchy）。 */
    record OrgSuperior(String userId, String superiorId, String lineCode, String updatedBy, Instant updatedAt) {
    }

    default void saveDirectSuperior(String userId, String superiorId, String lineCode, String updatedBy, Instant updatedAt) {
    }

    default List<OrgSuperior> listOrgHierarchy(int limit) {
        return List.of();
    }

    default void saveApprovalLine(ApprovalLine line, String updatedBy, Instant updatedAt) {
    }

    default List<ApprovalLine> listApprovalLines() {
        return List.of();
    }
}
