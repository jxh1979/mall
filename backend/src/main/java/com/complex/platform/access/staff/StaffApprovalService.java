package com.complex.platform.access.staff;

// 文件版本：V0.06

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.dto.StaffAdjustmentAppealRequest;
import com.complex.platform.access.staff.dto.StaffEmploymentStatusRequest;
import com.complex.platform.access.staff.dto.StaffPayrollAdjustmentRequest;
import com.complex.platform.access.staff.dto.StaffShiftBatchResult;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 员工业务复用现有审批链；审批记录与实际发布处于同一事务。
 *
 * <p>CG-12（BSM:1478 / BSM:1487 / ENUM:283 / PRD:757、779「须经 HR 与该员工直属领导、向上三级复核后生效」「员工可对扣分提出申诉，
 * 申诉同样报至 HR 与直属领导向上三级裁定，过程与结论均留痕」）：员工状态变更、奖惩入账、员工申诉三类都发起
 * {@code hr_three_levels_up} 档复核（主线＝该员工直属上级链前三级，平行线 hr_review＝人力会签人，须经角色持有 staff.hr.review），
 * 全部通过才写入；申诉链通过＝撤销（冲回原调整，原记录保留），任一节点驳回＝维持。
 */
@Service
public class StaffApprovalService {
    static final String HR_PERMISSION = "staff.hr.review";

    private final JdbcStaffRepository repository;
    private final SupervisorReviewService reviews;
    private final AuthorizationService authorization;
    private final ObjectMapper json;
    private final AuditEventService audit;

    public StaffApprovalService(JdbcStaffRepository repository, SupervisorReviewService reviews,
            AuthorizationService authorization, ObjectMapper json, AuditEventService audit) {
        this.repository = repository;
        this.reviews = reviews;
        this.authorization = authorization;
        this.json = json;
        this.audit = audit;
    }

    /** CG-12：发起员工状态变更复核（不写入 staff_profiles），返回复核单。 */
    SupervisorReviewDecisionRecord submitEmploymentStatus(StaffProfileRecord profile, StaffEmploymentStatusRequest request, String reason) {
        requireHrApprover(request.hrApproverId());
        String before = profile.employmentStatus() == null ? "" : profile.employmentStatus();
        var review = reviews.evaluate(new SupervisorReviewEvaluateRequest(request.idempotencyKey() + ":review", reason,
                List.of(), SupervisorReviewService.STAFF_EMPLOYMENT_STATUS_ACTION, "staff_profile", profile.id(), 0L,
                "员工 " + displayName(profile) + " 状态 " + before + " → " + request.employmentStatus() + "：" + reason,
                null, hrLine(request.hrApproverId()), profile.userId()));
        repository.saveEmploymentStatusRevision(review.reviewId(), profile.id(), profile.userId(), before, request.employmentStatus(),
                reason, authorization.currentActor().id());
        return review;
    }

    /** CG-12：发起奖惩复核（不入账），奖惩记录 id 预分配为复核对象 id，通过后以同一 id 入账。 */
    SupervisorReviewDecisionRecord submitPayrollAdjustment(StaffPayrollAdjustmentRequest request) {
        requireHrApprover(request.hrApproverId());
        String id = "adj_" + UUID.randomUUID();
        String userId = request.userId().trim();
        String reason = request.reason().trim();
        var review = reviews.evaluate(new SupervisorReviewEvaluateRequest(request.idempotencyKey() + ":review", reason,
                List.of(), SupervisorReviewService.STAFF_ADJUSTMENT_ACTION, "staff_payroll_adjustment", id, request.amountCent(),
                ("bonus".equals(request.adjType()) ? "奖励 " : "扣罚 ") + userId + " " + request.month() + " " + request.amountCent() + " 分："
                        + reason, null, hrLine(request.hrApproverId()), userId));
        repository.saveAdjustmentRequest(id, review.reviewId(), userId, request.month(), request.adjType(), request.amountCent(), reason,
                authorization.currentActor().id());
        return review;
    }

    /** CG-12：员工对已生效扣罚提出申诉，同链裁定。 */
    StaffAdjustmentAppealRecord submitAdjustmentAppeal(JdbcStaffRepository.AdjustmentRow adjustment, StaffAdjustmentAppealRequest request,
            String employeeId) {
        requireHrApprover(request.hrApproverId());
        String id = "apl_" + UUID.randomUUID();
        String reason = request.reason().trim();
        var review = reviews.evaluate(new SupervisorReviewEvaluateRequest(request.idempotencyKey() + ":review", reason,
                List.of(), SupervisorReviewService.STAFF_ADJUSTMENT_APPEAL_ACTION, "staff_payroll_adjustment_appeal", id, adjustment.amountCent(),
                "员工申诉扣罚 " + adjustment.id() + "（" + adjustment.month() + "，" + adjustment.amountCent() + " 分，" + adjustment.reason() + "）："
                        + reason, null, hrLine(request.hrApproverId()), employeeId));
        var record = new StaffAdjustmentAppealRecord(id, adjustment.id(), employeeId, adjustment.month(), adjustment.adjType(),
                adjustment.amountCent(), reason, review.reviewId(), "pending", null, employeeId, Instant.now(), null);
        repository.saveAppeal(record);
        audit.recordSuccess("staff.payroll.adjustment_appeal_submitted", "staff.payroll.view_mine", "staff_payroll_adjustment_appeal", id,
                null, request.idempotencyKey(), Map.of("adjustment_id", adjustment.id(), "user_id", employeeId, "review_id", review.reviewId(),
                        "reason", reason));
        return record;
    }

    /** 人力会签人必填，且须经角色持有 staff.hr.review（PGM:658 HR 岗位）；员工本人充当 HR 节点由链引擎按受益人剔除（APPROVAL_LINE_EMPTY）。 */
    private void requireHrApprover(String hrApproverId) {
        if (hrApproverId == null || hrApproverId.isBlank()) {
            throw ApiException.badRequest("HR_APPROVER_REQUIRED", "涉及员工的人力操作须指定 人力会签人");
        }
        if (authorization.canResolveRoles() && !authorization.holdsPermissionViaRole(hrApproverId.trim(), HR_PERMISSION)) {
            throw ApiException.badRequest("HR_APPROVER_LACKS_ROLE", "指定的 人力会签人未经角色持有人力会签权限");
        }
    }

    private static List<SupervisorReviewEvaluateRequest.ParallelLine> hrLine(String hrApproverId) {
        return List.of(new SupervisorReviewEvaluateRequest.ParallelLine(SupervisorReviewService.HR_LINE, hrApproverId.trim()));
    }

    private static String displayName(StaffProfileRecord profile) {
        return profile.name() == null || profile.name().isBlank() ? profile.userId() : profile.name();
    }

    StaffShiftBatchResult submitShifts(List<StaffShiftRecord> drafts, String key) {
        String actor = authorization.currentActor().id();
        String proposal = drafts.stream().map(draft -> {
            var before = repository.findShiftById(draft.id()).orElse(null);
            String name = repository.findProfileByUserId(draft.userId()).map(StaffProfileRecord::name).orElse(draft.userId());
            return name + "：" + (before == null ? "新增" : before.startAt() + " 至 " + before.endAt())
                    + " → " + draft.startAt() + " 至 " + draft.endAt()
                    + "；点位：" + String.join("、", draft.pointSpaceIds())
                    + "；缘由：" + (draft.note() == null ? "新排班" : draft.note());
        }).collect(java.util.stream.Collectors.joining("\n"));
        var review = reviews.evaluate(new SupervisorReviewEvaluateRequest(key + ":review", "排班发布审批",
                List.of(), "staff.shift.publish", "staff_shift_batch", "batch_" + UUID.randomUUID(),
                0L, proposal, null, List.of()));
        int created = 0;
        for (var draft : drafts) {
            var before = repository.findShiftById(draft.id()).orElse(null);
            if (before == null) created++;
            repository.saveShiftRevision(draft.id(), review.reviewId(), encode(before), encode(draft), actor);
        }
        return new StaffShiftBatchResult(created, drafts.size() - created,
                drafts.stream().map(row -> row.awaitingReview(review.reviewId())).toList(),
                review.reviewId(), "pending_review");
    }

    SupervisorReviewDecisionRecord submitPenaltyCap(StaffNumericSetting value,
            com.complex.platform.access.staff.dto.StaffPenaltyCapRequest request) {
        var before = repository.numericSetting("payroll_penalty_cap_bps", "global");
        var review = reviews.evaluate(new SupervisorReviewEvaluateRequest(request.idempotencyKey()+":review", request.reason(),
                List.of(), "staff.payroll.penalty_cap.adjust", "staff_penalty_cap", "cap_"+UUID.randomUUID(),
                0L, "普通扣除上限由 " + before.value() + " 调整为 " + value.value() + "（万分比）：" + request.reason(),
                null, request.parallelLines()));
        repository.savePenaltyCapRevision(review.reviewId(), before, value, authorization.currentActor().id());
        return review;
    }

    @EventListener
    @Transactional
    public void afterDecision(SupervisorReviewDecisionRecord decision) {
        if (SupervisorReviewService.STAFF_ADJUSTMENT_APPEAL_ACTION.equals(decision.actionType())) {
            decideAppeal(decision);
            return;
        }
        if (!"approved".equals(decision.reviewStatus())) return;
        if (SupervisorReviewService.STAFF_EMPLOYMENT_STATUS_ACTION.equals(decision.actionType())) {
            applyEmploymentStatus(decision.reviewId());
            return;
        }
        if (SupervisorReviewService.STAFF_ADJUSTMENT_ACTION.equals(decision.actionType())) {
            applyAdjustment(decision.reviewId());
            return;
        }
        if ("staff.payroll.penalty_cap.adjust".equals(decision.actionType())) {
            repository.applyPenaltyCapRevision(decision.reviewId());
            return;
        }
        if (!"staff.shift.publish".equals(decision.actionType())) return;
        for (var revision : repository.shiftRevisions(decision.reviewId(), null, true)) {
            if (revision.appliedAt() == null) {
                repository.publishShiftRevision(revision, decode(revision.beforeValue()), decode(revision.afterValue()));
            }
        }
    }

    /** CG-12：全部通过 → 写入员工状态（departed 即「生效即转离职」）；复核期间状态已被别的已生效复核改动则拒绝，须重新发起。 */
    private void applyEmploymentStatus(String reviewId) {
        var row = repository.lockPendingEmploymentStatusRevision(reviewId).orElse(null);
        if (row == null) return;
        String profileId = row.get("profile_id").toString();
        String before = row.get("before_status").toString();
        String after = row.get("after_status").toString();
        StaffProfileRecord profile = repository.findProfileById(profileId)
                .orElseThrow(() -> ApiException.notFound("STAFF_PROFILE_NOT_FOUND", "员工档案不存在"));
        String current = profile.employmentStatus() == null ? "" : profile.employmentStatus();
        if (!current.equals(before)) {
            throw ApiException.conflict("EMPLOYMENT_STATUS_CHANGED", "员工状态在复核期间已被其他已生效复核变更，请重新发起");
        }
        repository.saveProfile(profile.withEmploymentStatus(after, Instant.now()));
        repository.markEmploymentStatusRevisionApplied(reviewId);
        audit.recordSuccess("staff.profile.employment_status_changed", "staff.profile.review", "staff_profile", profileId, null, null,
                Map.of("before", before, "after", after, "reason", String.valueOf(row.get("reason")), "review_id", reviewId,
                        "initiated_by", String.valueOf(row.get("initiated_by"))));
    }

    /** CG-12：全部通过 → 奖惩入账。 */
    private void applyAdjustment(String reviewId) {
        var row = repository.lockPendingAdjustmentRequest(reviewId).orElse(null);
        if (row == null) return;
        String id = row.get("id").toString();
        String userId = row.get("user_id").toString();
        repository.insertPayrollAdjustment(id, userId, row.get("month").toString(), row.get("adj_type").toString(),
                ((Number) row.get("amount_cent")).longValue(), row.get("reason").toString(), row.get("initiated_by").toString(), Instant.now());
        repository.markAdjustmentRequestApplied(reviewId);
        audit.recordSuccess("staff.payroll.adjustment_added", "staff.payroll.manage", "staff_payroll_adjustment", id, null, null,
                Map.of("user_id", userId, "month", row.get("month").toString(), "adj_type", row.get("adj_type").toString(),
                        "amount_cent", ((Number) row.get("amount_cent")).longValue(), "reason", row.get("reason").toString(),
                        "review_id", reviewId));
    }

    /** CG-12：申诉裁定——链全部通过＝撤销（冲回原调整，原记录保留）；任一节点驳回＝维持。过程（节点裁决）与结论均留痕。 */
    private void decideAppeal(SupervisorReviewDecisionRecord decision) {
        var appeal = repository.lockPendingAppealByReview(decision.reviewId()).orElse(null);
        if (appeal == null) return;
        Instant now = Instant.now();
        String decidedBy = authorization.currentActor() == null ? null : authorization.currentActor().id();
        if ("approved".equals(decision.reviewStatus())) {
            String reversalId = "adj_" + UUID.randomUUID();
            repository.insertPayrollAdjustment(reversalId, appeal.userId(), appeal.month(), "penalty_reversal", appeal.amountCent(),
                    "申诉撤销冲回：" + appeal.adjustmentId(), decidedBy, now, appeal.adjustmentId());
            repository.decideAppeal(appeal.id(), "revoked", reversalId, now);
            audit.recordSuccess("staff.payroll.adjustment_reversed", "staff.payroll.manage", "staff_payroll_adjustment", reversalId, null, null,
                    Map.of("reversal_of", appeal.adjustmentId(), "appeal_id", appeal.id(), "amount_cent", appeal.amountCent(),
                            "user_id", appeal.userId(), "month", appeal.month()));
            audit.recordSuccess("staff.payroll.adjustment_appeal_decided", "staff.payroll.manage", "staff_payroll_adjustment_appeal", appeal.id(),
                    null, null, Map.of("conclusion", "revoked", "review_id", decision.reviewId(), "reversal_adjustment_id", reversalId));
        } else if ("rejected".equals(decision.reviewStatus())) {
            repository.decideAppeal(appeal.id(), "upheld", null, now);
            audit.recordSuccess("staff.payroll.adjustment_appeal_decided", "staff.payroll.manage", "staff_payroll_adjustment_appeal", appeal.id(),
                    null, null, Map.of("conclusion", "upheld", "review_id", decision.reviewId()));
        }
    }

    public record History(String revisionId, String reviewId, StaffShiftRecord before, StaffShiftRecord after,
            java.time.Instant appliedAt) {}

    List<History> history(String shiftId) {
        return repository.shiftRevisions(null, shiftId, false).stream()
                .map(row -> new History(row.id(), row.reviewId(), decode(row.beforeValue()), decode(row.afterValue()), row.appliedAt())).toList();
    }

    private String encode(StaffShiftRecord record) {
        if (record == null) return null;
        try { return json.writeValueAsString(record); }
        catch (java.io.IOException e) { throw ApiException.conflict("SHIFT_SNAPSHOT_FAILED", "排班记录保存失败"); }
    }

    private StaffShiftRecord decode(String value) {
        if (value == null) return null;
        try { return json.readValue(value, StaffShiftRecord.class); }
        catch (java.io.IOException e) { throw ApiException.conflict("SHIFT_SNAPSHOT_FAILED", "排班历史记录读取失败"); }
    }
}
