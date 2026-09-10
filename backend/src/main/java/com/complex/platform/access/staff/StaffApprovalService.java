package com.complex.platform.access.staff;

// 文件版本：V0.05

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.dto.StaffShiftBatchResult;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 员工业务复用现有审批链；审批记录与实际发布处于同一事务。 */
@Service
public class StaffApprovalService {
    private final JdbcStaffRepository repository;
    private final SupervisorReviewService reviews;
    private final AuthorizationService authorization;
    private final ObjectMapper json;

    public StaffApprovalService(JdbcStaffRepository repository, SupervisorReviewService reviews,
            AuthorizationService authorization, ObjectMapper json) {
        this.repository = repository;
        this.reviews = reviews;
        this.authorization = authorization;
        this.json = json;
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
        if (!"approved".equals(decision.reviewStatus())) return;
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
