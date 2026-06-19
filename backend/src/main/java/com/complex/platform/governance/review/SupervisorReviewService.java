package com.complex.platform.governance.review;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SupervisorReviewService {

    private static final String VIEW_PERMISSION = "governance.supervisor_review.view";
    private static final String MANAGE_PERMISSION = "governance.supervisor_review.manage";
    private static final String APPROVE_PERMISSION = "governance.supervisor_review.approve";

    private final AuthorizationService authorizationService;
    private final IdempotencyService idempotencyService;
    private final SupervisorReviewRepository supervisorReviewRepository;
    private final AuditEventService auditEventService;

    private final List<SupervisorReviewPolicyRecord> policies = List.of(
            new SupervisorReviewPolicyRecord("manual_release_l2", "manual_release", 0, 2, true, List.of("operator_note", "evidence_attachment"), "active", "specific_role"),
            new SupervisorReviewPolicyRecord("bill_waiver_l3", "bill_waiver", 1, 3, true, List.of("reason", "evidence_attachment"), "active", "specific_role"),
            new SupervisorReviewPolicyRecord("refund_l2", "refund", 1, 2, true, List.of("reason"), "active", "specific_role"),
            new SupervisorReviewPolicyRecord("ar_ap_adjustment_l3", "ar_ap_adjustment", 1, 3, true, List.of("reason", "before_after"), "active", "specific_role"),
            new SupervisorReviewPolicyRecord("contract_violation_l3", "contract_violation", 0, 3, true, List.of("violation_summary", "contract_snapshot"), "active", "two_levels_up"),
            new SupervisorReviewPolicyRecord("base_definition_violation_l3", "base_definition_violation", 0, 3, true, List.of("violation_summary", "definition_snapshot"), "active", "top_authority")
    );

    public SupervisorReviewService(
            AuthorizationService authorizationService,
            IdempotencyService idempotencyService,
            SupervisorReviewRepository supervisorReviewRepository,
            AuditEventService auditEventService
    ) {
        this.authorizationService = authorizationService;
        this.idempotencyService = idempotencyService;
        this.supervisorReviewRepository = supervisorReviewRepository;
        this.auditEventService = auditEventService;
    }

    public PageResponse<SupervisorReviewPolicyRecord> listPolicies() {
        auditEventService.recordSuccess(
                "governance.supervisor_review.policies_viewed",
                VIEW_PERMISSION,
                "supervisor_review_policy",
                "policy_catalog",
                null,
                null,
                Map.of("policy_count", policies.size())
        );
        return new PageResponse<>(policies, 1, 50, policies.size());
    }

    public SupervisorReviewDecisionRecord evaluate(SupervisorReviewEvaluateRequest request) {
        requireText(request == null ? null : request.idempotencyKey(), "idempotency_key");
        return idempotencyService.execute(
                "supervisorReviewEvaluate",
                request.idempotencyKey(),
                request,
                () -> {
                    String actionType = normalize(requireText(request.actionType(), "action_type"));
                    SupervisorReviewPolicyRecord policy = findPolicy(actionType);
                    long amountCent = request.amountCent() == null ? 0 : request.amountCent();
                    boolean amountHit = amountCent >= policy.minAmountCent();
                    boolean violationHit = textPresent(request.violationSummary());
                    boolean blocked = policy.blocksByDefault() && (amountHit || violationHit || isAlwaysBlocked(actionType));
                    String status = blocked ? "pending_review" : "allowed";
                    // The operator who triggered the reviewed action is recorded so the approver
                    // can never be the same person (segregation of duties / no self-approval).
                    String initiatedBy = authorizationService.currentActor().id();
                    // REQ-69 逐级上提：按发起人 level_rank 与策略升级类型计算所需审批人 rank。
                    // 仅当升级类型按 rank 且发起人职级已知（rank>0）时生效；否则回退既有 required_level（主管级）。
                    ApprovalLevel approvalLevel = ApprovalLevel.fromCode(policy.approvalLevel());
                    int initiatorRank = authorizationService.currentActorLevelRank();
                    Integer requiredApproverRank = (approvalLevel.rankBased() && initiatorRank > 0)
                            ? approvalLevel.requiredApproverRank(initiatorRank)
                            : null;
                    SupervisorReviewDecisionRecord decision = new SupervisorReviewDecisionRecord(
                            "review_" + UUID.randomUUID(),
                            policy.policyCode(),
                            actionType,
                            requireText(request.objectType(), "object_type"),
                            requireText(request.objectId(), "object_id"),
                            blocked,
                            status,
                            policy.requiredLevel(),
                            null,
                            blocked ? "blocked" : "allowed",
                            request.violationSummary(),
                            request.evidenceAttachmentIds() == null ? List.of() : List.copyOf(request.evidenceAttachmentIds()),
                            Instant.now(),
                            initiatedBy,
                            policy.approvalLevel(),
                            requiredApproverRank
                    );
                    if (blocked) {
                        supervisorReviewRepository.saveSupervisorReview(decision);
                    }
                    auditEventService.recordSuccess(
                            "governance.supervisor_review.evaluated",
                            MANAGE_PERMISSION,
                            "supervisor_review",
                            decision.reviewId(),
                            null,
                            request.idempotencyKey(),
                            Map.of("action_type", actionType, "blocked", blocked, "required_level", policy.requiredLevel(),
                                    "approval_level", policy.approvalLevel())
                    );
                    return decision;
                },
                SupervisorReviewDecisionRecord.class
        );
    }

    public SupervisorReviewDecisionRecord approve(String reviewId, SupervisorReviewApprovalRequest request) {
        String id = requireText(reviewId, "review_id");
        requireText(request == null ? null : request.idempotencyKey(), "idempotency_key");
        return idempotencyService.execute(
                "supervisorReviewApprove:" + id,
                request.idempotencyKey(),
                request,
                () -> {
                    String decision = normalize(requireText(request.decision(), "decision"));
                    if (!List.of("approved", "rejected").contains(decision)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "decision must be approved or rejected");
                    }
                    // Reviewer level comes from the authenticated principal, never from the request body.
                    int reviewerLevel = authorizationService.currentSupervisorLevel();
                    SupervisorReviewDecisionRecord pending = supervisorReviewRepository
                            .findPendingSupervisorReviewById(id)
                            .orElseThrow(() -> ApiException.notFound("SUPERVISOR_REVIEW_NOT_FOUND", "supervisor review is not pending or does not exist"));
                    int requiredLevel = pending.requiredLevel();
                    if (reviewerLevel < requiredLevel) {
                        throw ApiException.forbidden("SUPERVISOR_LEVEL_TOO_LOW", "reviewer level is lower than required level");
                    }
                    // Segregation of duties: the supervisor who initiated the action cannot approve it.
                    String reviewerId = authorizationService.currentActor().id();
                    if (reviewerId != null && reviewerId.equals(pending.initiatedBy())) {
                        throw ApiException.forbidden(
                                "SUPERVISOR_SELF_REVIEW_FORBIDDEN", "the initiator of an action cannot approve its own review");
                    }
                    // REQ-69 逐级上提：当本次复核记录有按 rank 计算的所需审批人 rank 时，
                    // 审批人的 level_rank 必须不低于该 rank（在主管级校验之上的管理职级门槛）。
                    if (pending.requiredApproverRank() != null) {
                        int reviewerRank = authorizationService.levelRankOf(reviewerId);
                        if (reviewerRank < pending.requiredApproverRank()) {
                            throw ApiException.forbidden(
                                    "SUPERVISOR_RANK_TOO_LOW",
                                    "reviewer management level rank is lower than the required approver rank");
                        }
                    }
                    SupervisorReviewDecisionRecord result = new SupervisorReviewDecisionRecord(
                            id,
                            pending.policyCode(),
                            pending.actionType(),
                            pending.objectType(),
                            pending.objectId(),
                            false,
                            decision,
                            requiredLevel,
                            reviewerLevel,
                            decision,
                            request.comment(),
                            request.evidenceAttachmentIds() == null ? List.of() : List.copyOf(request.evidenceAttachmentIds()),
                            Instant.now(),
                            pending.initiatedBy(),
                            pending.approvalLevel(),
                            pending.requiredApproverRank()
                    );
                    supervisorReviewRepository.saveSupervisorReview(result);
                    auditEventService.recordSuccess(
                            "governance.supervisor_review.decided",
                            APPROVE_PERMISSION,
                            "supervisor_review",
                            id,
                            null,
                            request.idempotencyKey(),
                            Map.of("decision", decision, "reviewer_level", reviewerLevel)
                    );
                    // R5 T3 事件名收口：准入复核放行/驳回额外发 admission.released（替代零命中的 contract.released）。
                    auditEventService.recordSuccess(
                            "approved".equals(decision) ? "admission.released" : "admission.escalated",
                            APPROVE_PERMISSION,
                            "supervisor_review",
                            id,
                            null,
                            request.idempotencyKey(),
                            Map.of("decision", decision, "action_type", pending.actionType())
                    );
                    return result;
                },
                SupervisorReviewDecisionRecord.class
        );
    }

    /**
     * R5 T3 准入闸 block 分支：为命中 block 策略的业务动作建立 pending 复核（走 supervisor_review），
     * 按 approval_level + 发起人 level_rank 计算所需审批人 rank（与 {@link #evaluate} 同一逻辑），
     * 发 {@code admission.blocked} 事件，返回带 reviewId 的决策。放行沿用 {@link #approve}（发 admission.released）。
     *
     * @param policyCode    supervisor_review_policies.policy_code
     * @param actionType    业务动作（admissionAction）
     * @param objectType    准入对象类型
     * @param objectId      准入对象 id（可空）
     * @param approvalLevelCode 升级类型 one_level_up/two_levels_up/top_authority/specific_role
     * @param requiredLevel 主管复核所需 level
     */
    public SupervisorReviewDecisionRecord blockForAdmission(
            String policyCode,
            String actionType,
            String objectType,
            String objectId,
            String approvalLevelCode,
            int requiredLevel
    ) {
        String initiatedBy = authorizationService.currentActor().id();
        ApprovalLevel approvalLevel = ApprovalLevel.fromCode(approvalLevelCode);
        int initiatorRank = authorizationService.currentActorLevelRank();
        Integer requiredApproverRank = (approvalLevel.rankBased() && initiatorRank > 0)
                ? approvalLevel.requiredApproverRank(initiatorRank)
                : null;
        SupervisorReviewDecisionRecord decision = new SupervisorReviewDecisionRecord(
                "review_" + UUID.randomUUID(),
                policyCode,
                actionType,
                objectType == null || objectType.isBlank() ? actionType : objectType,
                objectId == null || objectId.isBlank() ? actionType : objectId,
                true,
                "pending_review",
                requiredLevel,
                null,
                "blocked",
                null,
                List.of(),
                Instant.now(),
                initiatedBy,
                approvalLevelCode,
                requiredApproverRank
        );
        supervisorReviewRepository.saveSupervisorReview(decision);
        auditEventService.recordSuccess(
                "admission.blocked",
                MANAGE_PERMISSION,
                "supervisor_review",
                decision.reviewId(),
                null,
                null,
                Map.of("action_type", actionType, "object_type", objectType == null ? "" : objectType,
                        "approval_level", approvalLevelCode, "required_level", requiredLevel)
        );
        return decision;
    }

    private SupervisorReviewPolicyRecord findPolicy(String actionType) {
        return policies.stream()
                .filter(policy -> policy.actionType().equals(actionType))
                .min(Comparator.comparing(SupervisorReviewPolicyRecord::policyCode))
                .orElseThrow(() -> ApiException.badRequest("UNKNOWN_REVIEW_ACTION", "unknown review action type"));
    }

    private boolean isAlwaysBlocked(String actionType) {
        return actionType.contains("violation") || actionType.equals("manual_release");
    }

    private boolean textPresent(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return value.trim();
    }
}
