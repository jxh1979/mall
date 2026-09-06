package com.complex.platform.governance.review;

// 文件版本：V0.01

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
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import static com.complex.platform.common.Strings.requireText;

@Service
public class SupervisorReviewService {

    private static final String VIEW_PERMISSION = "governance.supervisor_review.view";
    private static final String MANAGE_PERMISSION = "governance.supervisor_review.manage";
    private static final String APPROVE_PERMISSION = "governance.supervisor_review.approve";

    private final AuthorizationService authorizationService;
    private final IdempotencyService idempotencyService;
    private final SupervisorReviewRepository supervisorReviewRepository;
    private final AuditEventService auditEventService;

    // specific_role 策略的"指定角色"以审批人须经角色持有的权限码表达（双轨收口 §24.3）。
    // 默认沿用督导复核审批权限 governance.supervisor_review.approve（须来自角色，非临时授权），
    // 从而把 specific_role 落为"按角色解析"而非旁路。
    private static final String DEFAULT_SPECIFIC_ROLE_PERMISSION = APPROVE_PERMISSION;

    private final List<SupervisorReviewPolicyRecord> policies = List.of(
            new SupervisorReviewPolicyRecord("manual_release_l2", "manual_release", 0, 2, true, List.of("operator_note", "evidence_attachment"), "active", "specific_role", DEFAULT_SPECIFIC_ROLE_PERMISSION),
            new SupervisorReviewPolicyRecord("bill_waiver_l3", "bill_waiver", 1, 3, true, List.of("reason", "evidence_attachment"), "active", "specific_role", DEFAULT_SPECIFIC_ROLE_PERMISSION),
            new SupervisorReviewPolicyRecord("refund_l2", "refund", 1, 2, true, List.of("reason"), "active", "specific_role", DEFAULT_SPECIFIC_ROLE_PERMISSION),
            new SupervisorReviewPolicyRecord("ar_ap_adjustment_l3", "ar_ap_adjustment", 1, 3, true, List.of("reason", "before_after"), "active", "specific_role", DEFAULT_SPECIFIC_ROLE_PERMISSION),
            new SupervisorReviewPolicyRecord("contract_violation_l3", "contract_violation", 0, 3, true, List.of("violation_summary", "contract_snapshot"), "active", "two_levels_up"),
            new SupervisorReviewPolicyRecord("base_definition_violation_l3", "base_definition_violation", 0, 3, true, List.of("violation_summary", "definition_snapshot"), "active", "top_authority"),
            new SupervisorReviewPolicyRecord("permission_grant_l1", "permission_grant_l1", 0, 1, true, List.of("reason"), "active", "one_level_up"),
            new SupervisorReviewPolicyRecord("permission_grant_l2", "permission_grant_l2", 0, 2, true, List.of("reason"), "active", "two_levels_up"),
            new SupervisorReviewPolicyRecord("permission_grant_l3", "permission_grant_l3", 0, 3, true, List.of("reason"), "active", "top_authority")
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
                    // REQ-69 逐级上提 + 双轨收口（§24.3）：按发起人 level_rank 与策略升级类型计算所需审批人 rank。
                    // 发起人无职级配置（rank=0）时不再静默降级为"仅主管级"，而是用安全保守 rank 下限收口。
                    ApprovalLevel approvalLevel = ApprovalLevel.fromCode(policy.approvalLevel());
                    int initiatorRank = authorizationService.currentActorLevelRank();
                    Integer requiredApproverRank = resolveRequiredApproverRank(
                            approvalLevel, initiatorRank, policy.requiredLevel());
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
                        scheduleDeadline(decision.reviewId(), null, actionType, policy.policyCode());
                    }
                    auditEventService.recordSuccess(
                            "governance.supervisor_review.evaluated",
                            MANAGE_PERMISSION,
                            "supervisor_review",
                            decision.reviewId(),
                            null,
                            request.idempotencyKey(),
                            Map.of("action_type", actionType, "blocked", blocked, "required_level", policy.requiredLevel(),
                                    "approval_level", policy.approvalLevel(),
                                    "violation_summary", request.violationSummary() == null ? "" : request.violationSummary())
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
                        throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过（approved）或驳回（rejected）");
                    }
                    // Reviewer level comes from the authenticated principal, never from the request body.
                    int reviewerLevel = authorizationService.currentSupervisorLevel();
                    SupervisorReviewDecisionRecord pending = supervisorReviewRepository
                            .findPendingSupervisorReviewById(id)
                            .orElseThrow(() -> ApiException.notFound("SUPERVISOR_REVIEW_NOT_FOUND", "主管复核单不存在或已不在待复核状态"));
                    if (ApprovalLevel.TOP_AUTHORITY.code().equals(pending.approvalLevel())
                            && !authorizationService.isMasterAuthority(authorizationService.currentActor())) {
                        throw ApiException.forbidden("MASTER_AUTHORITY_REQUIRED", "此项须使用 USB 最高权限会话审批");
                    }
                    String reviewerId = authorizationService.currentActor().id();
                    Optional<SupervisorReviewRepository.ReviewSchedule> schedule =
                            supervisorReviewRepository.findSupervisorReviewSchedule(id);
                    // M007-A-02（PGM-1.7.1「转发给指定人选（不限层级，可为任意人选）」）：被转发人免除级别/职级/角色/范围闸，
                    // 仍受自审与直接受益人闸约束。
                    boolean forwardedReviewer = schedule.map(s -> reviewerId != null && reviewerId.equals(s.forwardedTo())).orElse(false);
                    if (schedule.map(s -> reviewerId != null && reviewerId.equals(s.beneficiaryId())).orElse(false)) {
                        throw ApiException.forbidden("SUPERVISOR_SELF_REVIEW_FORBIDDEN", "直接受益人不能复核与自己相关的申请");
                    }
                    int requiredLevel = pending.requiredLevel();
                    if (!forwardedReviewer && reviewerLevel < requiredLevel) {
                        throw ApiException.forbidden("SUPERVISOR_LEVEL_TOO_LOW", "复核人级别低于要求的复核级别");
                    }
                    // Segregation of duties: the supervisor who initiated the action cannot approve it.
                    if (reviewerId != null && reviewerId.equals(pending.initiatedBy())) {
                        throw ApiException.forbidden(
                                "SUPERVISOR_SELF_REVIEW_FORBIDDEN", "操作发起人不能复核自己发起的操作");
                    }
                    // REQ-69 逐级上提：当本次复核记录有按 rank 计算的所需审批人 rank 时，
                    // 审批人的 level_rank 必须不低于该 rank（在主管级校验之上的管理职级门槛）。
                    if (!forwardedReviewer && pending.requiredApproverRank() != null) {
                        int reviewerRank = authorizationService.currentActorLevelRank();
                        if (reviewerRank < pending.requiredApproverRank()) {
                            throw ApiException.forbidden(
                                    "SUPERVISOR_RANK_TOO_LOW",
                                    "复核人管理级别低于要求的审批级别");
                        }
                    }
                    // 双轨收口的角色闸/范围闸仅在可解析角色与隔离键时强制；不可解析时回退既有 level 闸（向后兼容）。
                    if (!forwardedReviewer && authorizationService.canResolveRoles()) {
                        SupervisorReviewPolicyRecord policy = findPolicyByCode(pending.policyCode());
                        // §24.3 specific_role：rank 闸不适用（requiredApproverRank=null）时不旁路，
                        // 改按角色解析——审批人必须通过其角色（非临时授权）持有指定审批权限。
                        if (pending.requiredApproverRank() == null
                                && policy != null
                                && ApprovalLevel.SPECIFIC_ROLE.code().equals(policy.approvalLevel())
                                && policy.requiredApproverPermission() != null
                                && !authorizationService.holdsPermissionViaRole(reviewerId, policy.requiredApproverPermission())) {
                            throw ApiException.forbidden(
                                    "SUPERVISOR_ROLE_NOT_AUTHORIZED",
                                    "复核人不具备该复核要求的审批角色");
                        }
                        // §24.3 同商户范围闸：发起人有商户隔离键时，审批人须同商户（或平台管理员越权）。
                        Optional<String> initiatorMerchant = authorizationService.merchantIdOf(pending.initiatedBy());
                        if (initiatorMerchant.isPresent()
                                && !authorizationService.isPlatformAdmin(authorizationService.currentActor())) {
                            Optional<String> reviewerMerchant = authorizationService.merchantIdOf(reviewerId);
                            if (reviewerMerchant.isEmpty() || !reviewerMerchant.get().equals(initiatorMerchant.get())) {
                                throw ApiException.forbidden(
                                        "SUPERVISOR_MERCHANT_SCOPE_MISMATCH",
                                        "复核人必须与发起人属于同一商户范围");
                            }
                        }
                    }
                    requireDecisionEvidence(findPolicyByCode(pending.policyCode()), request);
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
                            pending.reason(), // M007-F-11：保留评估时的「为什么」，审批意见进审计留痕
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
                            Map.of("decision", decision, "reviewer_level", reviewerLevel,
                                    "comment", request.comment() == null ? "" : request.comment())
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
        return blockForAdmission(policyCode, actionType, objectType, objectId, approvalLevelCode, requiredLevel, null);
    }

    /** 带直接受益人的阻断（授权申请传被授权对象），受益人不得审批也不得被转发（PGM-1.7.1）。 */
    public SupervisorReviewDecisionRecord blockForAdmission(
            String policyCode,
            String actionType,
            String objectType,
            String objectId,
            String approvalLevelCode,
            int requiredLevel,
            String beneficiaryId
    ) {
        String initiatedBy = authorizationService.currentActor().id();
        ApprovalLevel approvalLevel = ApprovalLevel.fromCode(approvalLevelCode);
        int initiatorRank = authorizationService.currentActorLevelRank();
        Integer requiredApproverRank = resolveRequiredApproverRank(approvalLevel, initiatorRank, requiredLevel);
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
        scheduleDeadline(decision.reviewId(), beneficiaryId, actionType, policyCode);
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
                .orElseThrow(() -> ApiException.badRequest("UNKNOWN_REVIEW_ACTION", "无法识别的复核操作类型"));
    }

    /** 审批时按 policy_code 复原策略（取 specific_role 角色权限）；未知策略返回 {@code null}（按非 specific_role 处理）。 */
    private SupervisorReviewPolicyRecord findPolicyByCode(String policyCode) {
        if (policyCode == null) {
            return null;
        }
        return policies.stream()
                .filter(policy -> policyCode.equals(policy.policyCode()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 双轨收口（§24.3）所需审批人 rank 计算：
     * <ul>
     *   <li>非按 rank（specific_role）→ {@code null}（由角色闸判定，不走 rank）；
     *   <li>按 rank 且发起人职级已知（rank&gt;0）→ 按逐级上提算出的 rank；
     *   <li>按 rank 但发起人无职级配置（rank=0）→ 不静默降级，改用安全保守 rank 下限。
     * </ul>
     */
    private Integer resolveRequiredApproverRank(ApprovalLevel approvalLevel, int initiatorRank, int requiredLevel) {
        if (!approvalLevel.rankBased()) {
            return null;
        }
        if (initiatorRank > 0) {
            return approvalLevel.requiredApproverRank(initiatorRank);
        }
        // 发起人无职级配置：仅在职级可解析（仓储可用）的真实部署里收口为安全保守 rank，
        // 不再静默降级；在不可解析的轻量上下文回退既有 level 闸（rank=null），保持向后兼容。
        if (!authorizationService.canResolveRoles()) {
            return null;
        }
        return approvalLevel.conservativeRequiredApproverRank(requiredLevel);
    }

    private static final int MONEY_DEADLINE_WORKING_DAYS = 1;
    private static final int DEFAULT_DEADLINE_WORKING_DAYS = 3;

    /**
     * M007-A-02（PGM-1.7.1「全平台审批通用时限」）：按策略参数 deadlineWorkingDays（工作日，含上下限）计算到期；
     * 未配置时涉钱 1 个工作日、其余 3 个工作日。工作日 = 周一至周五且不在 holiday_calendar 内。
     */
    private void scheduleDeadline(String reviewId, String beneficiaryId, String actionType, String policyCode) {
        int days = supervisorReviewRepository.findDeadlineWorkingDays(policyCode)
                .orElse(isMoneyAction(actionType) ? MONEY_DEADLINE_WORKING_DAYS : DEFAULT_DEADLINE_WORKING_DAYS);
        supervisorReviewRepository.scheduleSupervisorReview(reviewId, beneficiaryId, addWorkingDays(Instant.now(), Math.max(days, 1)));
    }

    private static boolean isMoneyAction(String actionType) {
        String type = actionType == null ? "" : actionType;
        return type.startsWith("finance.") || List.of("bill_waiver", "refund", "ar_ap_adjustment", "bar.bill_waiver",
                "ticketing.refund_approve", "ticketing.verification_reverse").contains(type);
    }

    // ponytail: 调休上班的周末未建模（holiday_calendar 只记假期），需要时在日历表加 workday 类型即可。
    Instant addWorkingDays(Instant from, int workingDays) {
        java.time.ZonedDateTime at = from.atZone(com.complex.platform.common.LocalZone.ID);
        java.time.LocalDate day = at.toLocalDate();
        java.util.Set<java.time.LocalDate> holidays = new java.util.HashSet<>(
                supervisorReviewRepository.findHolidayDates(day, day.plusDays(workingDays * 3L + 14)));
        int remaining = workingDays;
        while (remaining > 0) {
            day = day.plusDays(1);
            boolean weekend = day.getDayOfWeek() == java.time.DayOfWeek.SATURDAY || day.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
            if (!weekend && !holidays.contains(day)) {
                remaining--;
            }
        }
        return day.atTime(at.toLocalTime()).atZone(com.complex.platform.common.LocalZone.ID).toInstant();
    }

    /**
     * M007-A-02（PGM-1.7.1「超时……自动提醒并升级到该节点的上一级，不得默认通过，也不得默认拒绝……到达最终审批节点后仍超时的，
     * 单据保持待审」）：到期的待复核单升级一级（one_level_up → two_levels_up → top_authority），到顶后只提醒并顺延；
     * 每次升级/提醒留痕。返回处理条数。
     */
    public int escalateOverdue(Instant now) {
        int handled = 0;
        for (SupervisorReviewRepository.PendingReview item : supervisorReviewRepository.listOverdueSupervisorReviews(now, 200)) {
            SupervisorReviewDecisionRecord d = item.decision();
            ApprovalLevel current = d.approvalLevel() == null ? null : ApprovalLevel.fromCode(d.approvalLevel());
            ApprovalLevel next = current == ApprovalLevel.ONE_LEVEL_UP ? ApprovalLevel.TWO_LEVELS_UP
                    : current == ApprovalLevel.TWO_LEVELS_UP ? ApprovalLevel.TOP_AUTHORITY : null;
            int days = supervisorReviewRepository.findDeadlineWorkingDays(d.policyCode())
                    .orElse(isMoneyAction(d.actionType()) ? MONEY_DEADLINE_WORKING_DAYS : DEFAULT_DEADLINE_WORKING_DAYS);
            Instant nextDue = addWorkingDays(now, Math.max(days, 1));
            boolean escalated = next != null;
            String level = escalated ? next.code() : d.approvalLevel();
            Integer rank = escalated
                    ? resolveRequiredApproverRank(next, authorizationService.levelRankOf(d.initiatedBy()), d.requiredLevel())
                    : d.requiredApproverRank();
            if (!supervisorReviewRepository.escalateSupervisorReview(d.reviewId(), level, rank, nextDue, now)) {
                continue;
            }
            handled++;
            auditEventService.recordSuccess(
                    escalated ? "governance.supervisor_review.escalated" : "governance.supervisor_review.reminded",
                    APPROVE_PERMISSION,
                    "supervisor_review",
                    d.reviewId(),
                    null,
                    null,
                    Map.of("action_type", d.actionType(), "from_level", d.approvalLevel() == null ? "" : d.approvalLevel(),
                            "to_level", level == null ? "" : level, "due_at", nextDue.toString(),
                            "escalation_count", item.schedule() == null ? 1 : item.schedule().escalationCount() + 1)
            );
        }
        return handled;
    }

    /** M007-A-02（PGM-1.7.1 转发）：审批人（或已被转发人）把待复核单逐单转发；不得转给申请人本人或直接受益人。 */
    public SupervisorReviewRepository.ReviewSchedule forward(String reviewId, SupervisorReviewForwardRequest request) {
        String id = requireText(reviewId, "review_id");
        requireText(request == null ? null : request.idempotencyKey(), "idempotency_key");
        return idempotencyService.execute(
                "supervisorReviewForward:" + id,
                request.idempotencyKey(),
                request,
                () -> {
                    SupervisorReviewDecisionRecord pending = supervisorReviewRepository
                            .findPendingSupervisorReviewById(id)
                            .orElseThrow(() -> ApiException.notFound("SUPERVISOR_REVIEW_NOT_FOUND", "主管复核单不存在或已不在待复核状态"));
                    String actorId = authorizationService.currentActor().id();
                    Optional<SupervisorReviewRepository.ReviewSchedule> schedule = supervisorReviewRepository.findSupervisorReviewSchedule(id);
                    boolean forwardedReviewer = schedule.map(s -> actorId != null && actorId.equals(s.forwardedTo())).orElse(false);
                    if (ApprovalLevel.TOP_AUTHORITY.code().equals(pending.approvalLevel())
                            && !authorizationService.isMasterAuthority(authorizationService.currentActor())) {
                        throw ApiException.forbidden("MASTER_AUTHORITY_REQUIRED", "此项须由 USB 最高权限本人逐单转发");
                    }
                    if (!forwardedReviewer && authorizationService.currentSupervisorLevel() < pending.requiredLevel()) {
                        throw ApiException.forbidden("SUPERVISOR_LEVEL_TOO_LOW", "复核人级别低于要求的复核级别");
                    }
                    String target = requireText(request.forwardedTo(), "forwarded_to");
                    if (target.equals(pending.initiatedBy())
                            || schedule.map(s -> target.equals(s.beneficiaryId())).orElse(false)) {
                        throw ApiException.forbidden("SUPERVISOR_FORWARD_TARGET_FORBIDDEN", "不得转发给申请人本人或直接受益人");
                    }
                    Instant now = Instant.now();
                    if (!supervisorReviewRepository.forwardSupervisorReview(id, target, actorId, now)) {
                        throw ApiException.conflict("SUPERVISOR_REVIEW_NOT_FOUND", "主管复核单已不在待复核状态");
                    }
                    auditEventService.recordSuccess(
                            "governance.supervisor_review.forwarded",
                            APPROVE_PERMISSION,
                            "supervisor_review",
                            id,
                            null,
                            request.idempotencyKey(),
                            Map.of("forwarded_to", target, "forwarded_by", actorId == null ? "" : actorId,
                                    "comment", request.comment() == null ? "" : request.comment())
                    );
                    return supervisorReviewRepository.findSupervisorReviewSchedule(id)
                            .orElse(new SupervisorReviewRepository.ReviewSchedule(id, null, null, 0, null, target));
                },
                SupervisorReviewRepository.ReviewSchedule.class
        );
    }

    /** M007-A-02：待复核清单（审批人可见卡在哪一节点、到期与升级、转发状态）。 */
    public PageResponse<SupervisorReviewRepository.PendingReview> listPending(int limit) {
        List<SupervisorReviewRepository.PendingReview> items = supervisorReviewRepository.listPendingSupervisorReviews(limit);
        return new PageResponse<>(items, 1, limit, items.size());
    }

    /**
     * M007-A-01（PGM-1.2 第 5 步「放行结果都要记录」、PGM-1.7.1「审批通过才生效」「逐笔放行」）：
     * 准入闸在拦截前调用——同动作、同对象、同发起人且已批准未放行的复核单被消费一次并放行本次动作，
     * 留痕 admission.release_executed；批准消费后再次执行须重新审批。
     */
    public Optional<SupervisorReviewDecisionRecord> consumeApprovedRelease(
            String actionType, String objectType, String objectId, String actorId) {
        String type = objectType == null || objectType.isBlank() ? actionType : objectType;
        String id = objectId == null || objectId.isBlank() ? actionType : objectId;
        Optional<SupervisorReviewDecisionRecord> released = supervisorReviewRepository
                .findApprovedUnreleasedSupervisorReview(actionType, type, id, actorId)
                .filter(record -> supervisorReviewRepository.markSupervisorReviewReleased(record.reviewId(), actorId, Instant.now()));
        released.ifPresent(record -> auditEventService.recordSuccess(
                "admission.release_executed",
                MANAGE_PERMISSION,
                "supervisor_review",
                record.reviewId(),
                null,
                null,
                Map.of("action_type", actionType, "object_type", type, "object_id", id,
                        "released_by", actorId == null ? "" : actorId)
        ));
        return released;
    }

    // M007-F-07（PGM-1.16.2 常见复核：免单、退款、调账不设金额豁免）：三类与违规、人工放行一样恒进复核。
    private boolean isAlwaysBlocked(String actionType) {
        return actionType.contains("violation") || actionType.equals("manual_release")
                || actionType.equals("bill_waiver") || actionType.equals("refund") || actionType.equals("ar_ap_adjustment");
    }

    // M007-F-09（PGM-1.16「谁发起、为什么、谁审批、结果是什么，都要能查到」）：按策略声明的证据类型 fail-closed。
    private void requireDecisionEvidence(SupervisorReviewPolicyRecord policy, SupervisorReviewApprovalRequest request) {
        if (policy == null || policy.requiredEvidenceTypes() == null) {
            return;
        }
        List<String> required = policy.requiredEvidenceTypes();
        boolean hasAttachment = request.evidenceAttachmentIds() != null
                && request.evidenceAttachmentIds().stream().anyMatch(this::textPresent);
        if (required.contains("evidence_attachment") && !hasAttachment) {
            throw ApiException.badRequest("VALIDATION_FAILED", "该策略要求审批附带证据附件");
        }
        if ((required.contains("reason") || required.contains("operator_note"))
                && !textPresent(request.comment()) && !textPresent(request.operatorNote())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "该策略要求填写审批意见");
        }
    }

    private boolean textPresent(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
