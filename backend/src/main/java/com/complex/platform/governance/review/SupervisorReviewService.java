package com.complex.platform.governance.review;

// 文件版本：V0.10

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.chain.ApprovalChainNode;
import com.complex.platform.governance.chain.ApprovalChainService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    // M087：审批链引擎（组织层级逐级节点链）。纯领域对象，随服务构造，不单独注入。
    private final ApprovalChainService chainService;
    // U09-005：事件发布器必填注入——批准后的员工落地（U05 StaffApprovalService.afterDecision）依赖该事件，不得静默不发。
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    // specific_role 策略的"指定角色"以审批人须经角色持有的权限码表达（双轨收口 §24.3）。
    // 默认沿用督导复核审批权限 governance.supervisor_review.approve（须来自角色，非临时授权），
    // 从而把 specific_role 落为"按角色解析"而非旁路。
    private static final String DEFAULT_SPECIFIC_ROLE_PERMISSION = APPROVE_PERMISSION;

    // M087-A-03（PGM-1.10.1「税费规则与税费调整须经财务审核、税务审核、法务审核，再由顶层平台公司的老板最终确认后生效」）：
    // 税费调整固定三条平行线 + 主线到最高权限（老板最终确认）；发起时须为每条线指定最终审批人。
    static final String TAX_RULE_ADJUST_ACTION = "finance.tax_rule.adjust";

    private final List<SupervisorReviewPolicyRecord> policies = List.of(
            new SupervisorReviewPolicyRecord("staff_shift_publish", "staff.shift.publish", 0, 1, true, List.of("reason"), "active", "one_level_up"),
            new SupervisorReviewPolicyRecord("staff_penalty_cap_adjust", "staff.payroll.penalty_cap.adjust", 0, 3, true,
                    List.of("reason", "before_after"), "active", "top_authority", null,
                    List.of("hr_review", "finance_review", "legal_review")),
            new SupervisorReviewPolicyRecord("manual_release_l2", "manual_release", 0, 2, true, List.of("operator_note", "evidence_attachment"), "active", "specific_role", DEFAULT_SPECIFIC_ROLE_PERMISSION),
            new SupervisorReviewPolicyRecord("bill_waiver_l3", "bill_waiver", 1, 3, true, List.of("reason", "evidence_attachment"), "active", "specific_role", DEFAULT_SPECIFIC_ROLE_PERMISSION),
            new SupervisorReviewPolicyRecord("refund_l2", "refund", 1, 2, true, List.of("reason"), "active", "specific_role", DEFAULT_SPECIFIC_ROLE_PERMISSION),
            new SupervisorReviewPolicyRecord("ar_ap_adjustment_l3", "ar_ap_adjustment", 1, 3, true, List.of("reason", "before_after"), "active", "specific_role", DEFAULT_SPECIFIC_ROLE_PERMISSION),
            new SupervisorReviewPolicyRecord("contract_violation_l3", "contract_violation", 0, 3, true, List.of("violation_summary", "contract_snapshot"), "active", "two_levels_up"),
            new SupervisorReviewPolicyRecord("base_definition_violation_l3", "base_definition_violation", 0, 3, true, List.of("violation_summary", "definition_snapshot"), "active", "top_authority"),
            new SupervisorReviewPolicyRecord("permission_grant_l1", "permission_grant_l1", 0, 1, true, List.of("reason"), "active", "one_level_up"),
            new SupervisorReviewPolicyRecord("permission_grant_l2", "permission_grant_l2", 0, 2, true, List.of("reason"), "active", "two_levels_up"),
            new SupervisorReviewPolicyRecord("permission_grant_l3", "permission_grant_l3", 0, 3, true, List.of("reason"), "active", "top_authority"),
            new SupervisorReviewPolicyRecord("tax_rule_adjustment_l3", TAX_RULE_ADJUST_ACTION, 0, 3, true, List.of("reason", "before_after"), "active", "top_authority",
                    null, List.of("finance_review", "tax_review", "legal_review"))
    );

    public SupervisorReviewService(
            AuthorizationService authorizationService,
            IdempotencyService idempotencyService,
            SupervisorReviewRepository supervisorReviewRepository,
            AuditEventService auditEventService,
            org.springframework.context.ApplicationEventPublisher eventPublisher
    ) {
        this.authorizationService = authorizationService;
        this.idempotencyService = idempotencyService;
        this.supervisorReviewRepository = supervisorReviewRepository;
        this.auditEventService = auditEventService;
        this.eventPublisher = java.util.Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.chainService = new ApprovalChainService(supervisorReviewRepository, authorizationService);
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

    @Transactional
    public SupervisorReviewDecisionRecord evaluate(SupervisorReviewEvaluateRequest request) {
        requireText(request == null ? null : request.idempotencyKey(), "idempotency_key");
        return idempotencyService.execute(
                "supervisorReviewEvaluate",
                request.idempotencyKey(),
                request,
                () -> {
                    String actionType = normalize(requireText(request.actionType(), "action_type"));
                    SupervisorReviewPolicyRecord policy = resolvePolicy(actionType, request.amountCent());
                    List<ApprovalChainService.ParallelLine> lines = parallelLines(request.parallelLines());
                    requireDefaultLines(policy, lines);
                    long amountCent = request.amountCent() == null ? 0 : request.amountCent();
                    boolean amountHit = amountCent >= policy.minAmountCent();
                    boolean violationHit = textPresent(request.violationSummary());
                    boolean blocked = policy.blocksByDefault() && (amountHit || violationHit || isAlwaysBlocked(actionType));
                    String status = blocked ? "pending_review" : "allowed";
                    // The operator who triggered the reviewed action is recorded so the approver
                    // can never be the same person (segregation of duties / no self-approval).
                    String initiatedBy = authorizationService.currentActor().id();
                    // REQ-69 逐级上提 + 双轨收口（§24.3）：按发起人 level_rank 与策略升级类型计算所需审批人 rank（留痕；M087 起
                    // 审批人身份由组织层级节点决定，rank 只作为档位下限记录）。
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
                        String beneficiaryId = textPresent(request.beneficiaryId()) ? request.beneficiaryId().trim() : null;
                        requireBeneficiaryIfNeeded(actionType, beneficiaryId);
                        requireDesignatedRoleHolder(policy, request.finalApproverId()); // R-003 fail-closed
                        supervisorReviewRepository.saveSupervisorReview(decision);
                        scheduleDeadline(decision.reviewId(), beneficiaryId, actionType, policy.policyCode());
                        generateChain(decision.reviewId(), initiatedBy, beneficiaryId, approvalLevel, request.finalApproverId(), lines, policy.requiredLevel());
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

    /** 策略固定要求的平行线（如税费三审）必须由起草人逐线指定最终审批人，否则拒绝发起（fail-closed）。 */
    private static void requireDefaultLines(SupervisorReviewPolicyRecord policy, List<ApprovalChainService.ParallelLine> lines) {
        for (String code : policy.parallelLineCodes()) {
            boolean given = lines.stream().anyMatch(line -> code.equals(line.lineCode()) && line.finalApproverId() != null
                    && !line.finalApproverId().isBlank());
            if (!given) {
                throw ApiException.badRequest("APPROVAL_LINE_FINAL_APPROVER_REQUIRED", "本事项须为平行审批线指定最终审批人：" + code);
            }
        }
    }

    /**
     * M087-A-02（PGM-1.7.1「逐级审批」「任一节点驳回则整单退回起草人」「主线与各条平行线全部通过……才生效」）：
     * 一次 approve = 裁决当前节点。全部节点通过才把复核单置为 approved；任一节点驳回即整单 rejected（退回起草），其余待审节点作废；
     * 中间节点通过时复核单保持 pending_review 并原样返回。
     */
    @Transactional
    public SupervisorReviewDecisionRecord approve(String reviewId, SupervisorReviewApprovalRequest request) {
        String id = requireText(reviewId, "review_id");
        requireText(request == null ? null : request.idempotencyKey(), "idempotency_key");
        return idempotencyService.execute(
                "supervisorReviewApprove:" + id,
                request.idempotencyKey(),
                request,
                () -> {
                    supervisorReviewRepository.lockSupervisorReview(id); // M087-I5-06：同一复核单的节点裁决串行化，整单终态归约不受并发末节点影响
                    String decision = normalize(requireText(request.decision(), "decision"));
                    if (!List.of("approved", "rejected").contains(decision)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过（approved）或驳回（rejected）");
                    }
                    // Reviewer level comes from the authenticated principal, never from the request body.
                    int reviewerLevel = authorizationService.currentSupervisorLevel();
                    SupervisorReviewDecisionRecord pending = supervisorReviewRepository
                            .findPendingSupervisorReviewById(id)
                            .orElseThrow(() -> ApiException.notFound("SUPERVISOR_REVIEW_NOT_FOUND", "主管复核单不存在或已不在待复核状态"));
                    String reviewerId = authorizationService.currentActor().id();
                    Optional<SupervisorReviewRepository.ReviewSchedule> schedule =
                            supervisorReviewRepository.findSupervisorReviewSchedule(id);
                    // PGM-1.7.1「申请人与直接受益人不得审批与自己相关的申请，转发不构成例外」。
                    if (schedule.map(s -> reviewerId != null && reviewerId.equals(s.beneficiaryId())).orElse(false)) {
                        throw ApiException.forbidden("SUPERVISOR_SELF_REVIEW_FORBIDDEN", "直接受益人不能复核与自己相关的申请");
                    }
                    if (reviewerId != null && reviewerId.equals(pending.initiatedBy())) {
                        throw ApiException.forbidden(
                                "SUPERVISOR_SELF_REVIEW_FORBIDDEN", "操作发起人不能复核自己发起的操作");
                    }
                    List<ApprovalChainNode> nodes = supervisorReviewRepository.listChainNodes(id);
                    if (nodes.isEmpty()) {
                        throw ApiException.forbidden("APPROVAL_CHAIN_UNRESOLVED",
                                "组织层级未维护，审批链未生成；请平台管理员维护发起人的直接上级后重新生成审批链");
                    }
                    ApprovalChainNode node = resolveDecidableNode(pending, nodes, reviewerId, reviewerLevel);
                    if ("staff.payroll.penalty_cap.adjust".equals(pending.actionType())
                            && ApprovalChainNode.KIND_MASTER.equals(node.approverKind())
                            && nodes.stream().anyMatch(n -> !ApprovalChainNode.MAIN_LINE.equals(n.lineCode())
                                    && !ApprovalChainNode.APPROVED.equals(n.status()))) {
                        throw ApiException.conflict("DEPARTMENT_REVIEWS_PENDING", "须先完成人力、财务、法务三审，再由顶层平台最终确认");
                    }
                    requireDecisionEvidence(findPolicyByCode(pending.policyCode()), request);
                    Instant now = Instant.now();
                    // M087-I2-02（PGM-1.7.1「任一人审批即完成该节点」）：节点裁决为 status='pending' 的比较并交换，先到先得；后到者被拒。
                    if (!supervisorReviewRepository.updateChainNode(node.withStatus(decision, reviewerId, now))) {
                        throw ApiException.conflict("APPROVAL_NODE_ALREADY_DECIDED", "该审批节点已被裁决，本次结论未生效");
                    }
                    auditEventService.recordSuccess(
                            "governance.supervisor_review.node_decided",
                            APPROVE_PERMISSION,
                            "supervisor_review",
                            id,
                            null,
                            request.idempotencyKey(),
                            Map.of("node_id", node.nodeId(), "line_code", node.lineCode(), "seq", node.seq(),
                                    "approver_kind", node.approverKind(), "decision", decision, "reviewer_level", reviewerLevel,
                                    "comment", request.comment() == null ? "" : request.comment())
                    );
                    // M087-I2-02：整单终态由节点表归约，不用请求体里的结论：任一节点 rejected → 整单 rejected（退回起草）；
                    // 全部 approved → approved；否则保持待审。
                    List<ApprovalChainNode> after = supervisorReviewRepository.listChainNodes(id);
                    boolean anyRejected = after.stream().anyMatch(n -> ApprovalChainNode.REJECTED.equals(n.status()));
                    if (!anyRejected && !ApprovalChainService.allApproved(after)) {
                        return pending; // 链上仍有节点待审：单据保持待审（PGM-1.7.1 逐级审批）
                    }
                    String outcome = anyRejected ? "rejected" : "approved";
                    if (anyRejected) {
                        for (ApprovalChainNode other : after) {
                            if (other.pending()) {
                                supervisorReviewRepository.updateChainNode(other.withStatus(ApprovalChainNode.CANCELLED, reviewerId, now));
                            }
                        }
                    }
                    int requiredLevel = pending.requiredLevel();
                    SupervisorReviewDecisionRecord result = new SupervisorReviewDecisionRecord(
                            id,
                            pending.policyCode(),
                            pending.actionType(),
                            pending.objectType(),
                            pending.objectId(),
                            false,
                            outcome,
                            requiredLevel,
                            reviewerLevel,
                            outcome,
                            pending.reason(), // M007-F-11：保留评估时的「为什么」，审批意见进审计留痕
                            request.evidenceAttachmentIds() == null ? List.of() : List.copyOf(request.evidenceAttachmentIds()),
                            now,
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
                            Map.of("decision", outcome, "node_decision", decision, "reviewer_level", reviewerLevel,
                                    "comment", request.comment() == null ? "" : request.comment())
                    );
                    // R5 T3 事件名收口：准入复核放行/驳回额外发 admission.released（替代零命中的 contract.released）。
                    auditEventService.recordSuccess(
                            "approved".equals(outcome) ? "admission.released" : "admission.escalated",
                            APPROVE_PERMISSION,
                            "supervisor_review",
                            id,
                            null,
                            request.idempotencyKey(),
                            Map.of("decision", outcome, "action_type", pending.actionType())
                    );
                    eventPublisher.publishEvent(result);
                    return result;
                },
                SupervisorReviewDecisionRecord.class
        );
    }

    /**
     * 当前操作人可裁决的当前节点：user 节点须是该节点审批人（或被转发人）；master 节点须是 USB 主会话（或主会话转发的被转发人，
     * M007-I3-F01）；role 节点沿用 M007 的级别 / 角色 / 同商户闸。都不满足时按原因给出错误码。
     */
    private ApprovalChainNode resolveDecidableNode(SupervisorReviewDecisionRecord pending, List<ApprovalChainNode> nodes,
                                                   String reviewerId, int reviewerLevel) {
        List<ApprovalChainNode> current = ApprovalChainService.currentNodes(nodes);
        boolean master = authorizationService.isMasterAuthority(authorizationService.currentActor());
        for (ApprovalChainNode node : current) {
            // M007-A-02（PGM-1.7.1「被转发人同时获得审批权限」）：被转发人免除节点身份闸，仍受自审与直接受益人闸约束（上方已判）。
            if (reviewerId != null && reviewerId.equals(node.forwardedTo())) {
                return node;
            }
            switch (node.approverKind()) {
                case ApprovalChainNode.KIND_USER -> {
                    if (reviewerId != null && reviewerId.equals(node.approverId())) {
                        if (isFinalNodeOfRolePolicy(pending, nodes, node)) {
                            // R-003：角色审批事项走组织链后角色闸作为链尾资格——指定最终审批人裁决末节点时须经角色持有审批权限。
                            requireRoleNodeGates(pending, reviewerId, reviewerLevel);
                        }
                        return node;
                    }
                }
                case ApprovalChainNode.KIND_MASTER -> {
                    if (master) {
                        return node;
                    }
                }
                case ApprovalChainNode.KIND_ROLE -> {
                    requireRoleNodeGates(pending, reviewerId, reviewerLevel);
                    return node;
                }
                default -> {
                }
            }
        }
        if (current.stream().anyMatch(node -> ApprovalChainNode.KIND_MASTER.equals(node.approverKind()))) {
            throw ApiException.forbidden("MASTER_AUTHORITY_REQUIRED", "此项须使用 USB 最高权限会话审批");
        }
        boolean laterNode = nodes.stream().anyMatch(node -> node.pending() && reviewerId != null && reviewerId.equals(node.approverId()));
        if (laterNode) {
            throw ApiException.forbidden("APPROVAL_NODE_NOT_REACHED", "尚未轮到你所在的审批节点");
        }
        throw ApiException.forbidden("SUPERVISOR_NOT_CURRENT_APPROVER", "你不是该单据当前节点的审批人");
    }

    /** R-003：specific_role 策略主线的最后一个 user 节点（指定最终审批人）承担角色闸；未指定时链尾是独立的 role 节点。 */
    private boolean isFinalNodeOfRolePolicy(SupervisorReviewDecisionRecord pending, List<ApprovalChainNode> nodes, ApprovalChainNode node) {
        SupervisorReviewPolicyRecord policy = findPolicyByCode(pending.policyCode());
        return policy != null && ApprovalLevel.SPECIFIC_ROLE.code().equals(policy.approvalLevel())
                && ApprovalChainNode.MAIN_LINE.equals(node.lineCode())
                && nodes.stream().noneMatch(n -> ApprovalChainNode.MAIN_LINE.equals(n.lineCode()) && n.seq() > node.seq());
    }

    /** R-003：指定最终审批人裁决角色审批事项的末节点须经角色持有该事项的审批权限；发起时即 fail-closed。 */
    private void requireDesignatedRoleHolder(SupervisorReviewPolicyRecord policy, String finalApproverId) {
        if (policy == null || finalApproverId == null || finalApproverId.isBlank()
                || !ApprovalLevel.SPECIFIC_ROLE.code().equals(policy.approvalLevel())
                || policy.requiredApproverPermission() == null || !authorizationService.canResolveRoles()) {
            return;
        }
        if (!authorizationService.holdsPermissionViaRole(finalApproverId.trim(), policy.requiredApproverPermission())) {
            throw ApiException.badRequest("FINAL_APPROVER_LACKS_ROLE", "指定的最终审批人未经角色持有该事项要求的审批权限");
        }
    }

    /** specific_role 节点：沿用 M007 的主管级别闸、角色闸与同商户范围闸（不满足即抛原错误码）。 */
    private void requireRoleNodeGates(SupervisorReviewDecisionRecord pending, String reviewerId, int reviewerLevel) {
        if (reviewerLevel < roleNodeLevelGate(pending.requiredLevel())) {
            throw ApiException.forbidden("SUPERVISOR_LEVEL_TOO_LOW", "复核人级别低于要求的复核级别");
        }
        // 双轨收口的角色闸/范围闸仅在可解析角色与隔离键时强制；不可解析时回退既有 level 闸（向后兼容）。
        if (authorizationService.canResolveRoles()) {
            SupervisorReviewPolicyRecord policy = findPolicyByCode(pending.policyCode());
            // §24.3 specific_role：审批人必须通过其角色（非临时授权）持有指定审批权限。
            if (policy != null
                    && policy.requiredApproverPermission() != null
                    && !authorizationService.holdsPermissionViaRole(reviewerId, policy.requiredApproverPermission())) {
                throw ApiException.forbidden(
                        "SUPERVISOR_ROLE_NOT_AUTHORIZED",
                        "复核人不具备该复核要求的审批角色");
            }
            // §24.3 同商户范围闸：发起人有商户隔离键时，审批人须同商户（仅 USB 最高权限可逐笔越权）。
            Optional<String> initiatorMerchant = authorizationService.merchantIdOf(pending.initiatedBy());
            if (initiatorMerchant.isPresent()
                    && !authorizationService.isMasterAuthority(authorizationService.currentActor())) {
                Optional<String> reviewerMerchant = authorizationService.merchantIdOf(reviewerId);
                if (reviewerMerchant.isEmpty() || !reviewerMerchant.get().equals(initiatorMerchant.get())) {
                    throw ApiException.forbidden(
                            "SUPERVISOR_MERCHANT_SCOPE_MISMATCH",
                            "复核人必须与发起人属于同一商户范围");
                }
            }
        }
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
    @Transactional
    public SupervisorReviewDecisionRecord blockForAdmission(
            String policyCode,
            String actionType,
            String objectType,
            String objectId,
            String approvalLevelCode,
            int requiredLevel
    ) {
        return blockForAdmission(policyCode, actionType, objectType, objectId, approvalLevelCode, requiredLevel, null, null);
    }

    /** 带直接受益人的阻断（授权申请传被授权对象），受益人不得审批也不得被转发（PGM-1.7.1）。 */
    @Transactional
    public SupervisorReviewDecisionRecord blockForAdmission(
            String policyCode,
            String actionType,
            String objectType,
            String objectId,
            String approvalLevelCode,
            int requiredLevel,
            String beneficiaryId
    ) {
        return blockForAdmission(policyCode, actionType, objectType, objectId, approvalLevelCode, requiredLevel, beneficiaryId, null);
    }

    /**
     * M087（PGM-1.7.1「发起单据时只需指定该事项的最终审批人」）：带起草人指定的主线最终审批人的阻断；为空时按档位下限沿发起人
     * 直接上级链生成主线。
     */
    @Transactional
    public SupervisorReviewDecisionRecord blockForAdmission(
            String policyCode,
            String actionType,
            String objectType,
            String objectId,
            String approvalLevelCode,
            int requiredLevel,
            String beneficiaryId,
            String finalApproverId
    ) {
        String initiatedBy = authorizationService.currentActor().id();
        if (requiresTopAuthorityGrant(actionType, null)) {
            SupervisorReviewPolicyRecord top = findPolicyByCode("permission_grant_l3");
            if (top != null) {
                policyCode = top.policyCode();
                approvalLevelCode = top.approvalLevel();
                requiredLevel = top.requiredLevel();
            }
        }
        requireBeneficiaryIfNeeded(actionType, beneficiaryId);
        requireDesignatedRoleHolder(findPolicyByCode(policyCode), finalApproverId); // R-003 fail-closed
        SupervisorReviewPolicyRecord blockPolicy = findPolicyByCode(policyCode);
        if (blockPolicy != null) {
            requireDefaultLines(blockPolicy, List.of()); // M007-R6-01：策略固定要求平行线（税费三审）的事项不能经无平行线的准入阻断路径发起（fail-closed）
        }
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
        generateChain(decision.reviewId(), initiatedBy, beneficiaryId, approvalLevel, finalApproverId, List.of(), requiredLevel);
        auditEventService.recordSuccess(
                "admission.blocked",
                MANAGE_PERMISSION,
                "supervisor_review",
                decision.reviewId(),
                null,
                null,
                Map.of("action_type", actionType, "object_type", objectType == null ? "" : objectType,
                        "approval_level", approvalLevelCode, "required_level", requiredLevel,
                        "final_approver_id", finalApproverId == null ? "" : finalApproverId)
        );
        return decision;
    }

    /** M087：复核单的审批链节点（主线 + 平行线，按线与序号）。 */
    public List<ApprovalChainNode> chain(String reviewId) {
        return supervisorReviewRepository.listChainNodes(requireText(reviewId, "review_id"));
    }

    /**
     * M087：组织层级补齐后为「链未生成」的待审单重新生成主线（fail-closed 的恢复路径；平行线须重新发起）。
     * 已有节点的单据不重生成（返回现有节点）。
     */
    @Transactional
    public List<ApprovalChainNode> regenerateChain(String reviewId) {
        String id = requireText(reviewId, "review_id");
        SupervisorReviewDecisionRecord pending = supervisorReviewRepository
                .findPendingSupervisorReviewById(id)
                .orElseThrow(() -> ApiException.notFound("SUPERVISOR_REVIEW_NOT_FOUND", "主管复核单不存在或已不在待复核状态"));
        List<ApprovalChainNode> existing = supervisorReviewRepository.listChainNodes(id);
        if (!existing.isEmpty()) {
            return existing;
        }
        String beneficiaryId = supervisorReviewRepository.findSupervisorReviewSchedule(id)
                .map(SupervisorReviewRepository.ReviewSchedule::beneficiaryId).orElse(null);
        List<ApprovalChainNode> nodes = generateChain(id, pending.initiatedBy(), beneficiaryId,
                ApprovalLevel.fromCode(pending.approvalLevel()), null, List.of(), pending.requiredLevel());
        if (nodes.isEmpty()) {
            throw ApiException.conflict("APPROVAL_CHAIN_UNRESOLVED", "发起人的直接上级仍未维护，审批链无法生成");
        }
        auditEventService.recordSuccess(
                "governance.supervisor_review.chain_regenerated",
                MANAGE_PERMISSION,
                "supervisor_review",
                id,
                null,
                null,
                Map.of("node_count", nodes.size(), "regenerated_by", authorizationService.currentActor().id())
        );
        return nodes;
    }

    private List<ApprovalChainNode> generateChain(String reviewId, String initiatedBy, String beneficiaryId, ApprovalLevel level,
                                                  String finalApproverId, List<ApprovalChainService.ParallelLine> lines, int requiredLevel) {
        List<ApprovalChainNode> nodes = chainService.generate(reviewId, initiatedBy, beneficiaryId, level, finalApproverId, lines, requiredLevel);
        if (nodes.isEmpty()) {
            // fail-closed：组织层级未维护，链无法生成；单据留痕待审，无人可裁决，层级补齐后重生成。
            auditEventService.recordSuccess(
                    "governance.supervisor_review.chain_unresolved",
                    MANAGE_PERMISSION,
                    "supervisor_review",
                    reviewId,
                    null,
                    null,
                    Map.of("initiated_by", initiatedBy == null ? "" : initiatedBy, "approval_level", level.code())
            );
            return nodes;
        }
        supervisorReviewRepository.saveChainNodes(reviewId, nodes);
        auditEventService.recordSuccess(
                "governance.supervisor_review.chain_generated",
                MANAGE_PERMISSION,
                "supervisor_review",
                reviewId,
                null,
                null,
                Map.of("node_count", nodes.size(), "approval_level", level.code(),
                        "lines", nodes.stream().map(ApprovalChainNode::lineCode).distinct().collect(Collectors.joining(",")),
                        "approvers", nodes.stream().map(n -> n.approverId() == null ? n.approverKind() : n.approverId()).collect(Collectors.joining(",")))
        );
        return nodes;
    }

    private static List<ApprovalChainService.ParallelLine> parallelLines(List<SupervisorReviewEvaluateRequest.ParallelLine> lines) {
        if (lines == null) {
            return List.of();
        }
        return lines.stream().map(line -> new ApprovalChainService.ParallelLine(line.lineCode(), line.finalApproverId())).toList();
    }

    private SupervisorReviewPolicyRecord findPolicy(String actionType) {
        return policies.stream()
                .filter(policy -> policy.actionType().equals(actionType))
                .min(Comparator.comparing(SupervisorReviewPolicyRecord::policyCode))
                .orElseThrow(() -> ApiException.badRequest("UNKNOWN_REVIEW_ACTION", "无法识别的复核操作类型"));
    }

    /** 资金、分红、扣款与带金额的授出不由入口自报上一级/上两级，一律走最高权限档。 */
    private SupervisorReviewPolicyRecord resolvePolicy(String actionType, Long amountCent) {
        if (requiresTopAuthorityGrant(actionType, amountCent)) {
            SupervisorReviewPolicyRecord top = findPolicyByCode("permission_grant_l3");
            if (top != null) {
                return top;
            }
        }
        return findPolicy(actionType);
    }

    private boolean requiresTopAuthorityGrant(String actionType, Long amountCent) {
        String type = actionType == null ? "" : actionType;
        if (type.endsWith(".money") || type.contains("debit") || type.contains("deduction")
                || type.contains("dividend") || type.contains("authorization")) {
            return true;
        }
        return type.contains("grant") && amountCent != null && amountCent > 0;
    }

    private boolean requiresBeneficiary(String actionType) {
        String type = actionType == null ? "" : actionType;
        return type.contains("grant") || type.contains("authorization") || type.contains("refund")
                || type.contains("waiver") || type.contains("debit") || type.contains("deduction")
                || type.endsWith(".money") || type.equals("ar_ap_adjustment");
    }

    private void requireBeneficiaryIfNeeded(String actionType, String beneficiaryId) {
        if (requiresBeneficiary(actionType) && !textPresent(beneficiaryId)) {
            throw ApiException.badRequest("BENEFICIARY_REQUIRED", "退款、免单、授权与资金事项必须登记直接受益人");
        }
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
     * 双轨收口（§24.3）所需审批人 rank 计算（M087 起仅作档位下限留痕）：
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

    // M007-I3-F02：授权申请含资金/分红/退款/调账类权限时，AccessService 以 access.grant_request.money 作为动作名进链；
    // M087：范围变更同理（access.scope_change.money）——凡以 .money 结尾的授权动作名均按涉钱时限。
    static final String MONEY_GRANT_REQUEST_ACTION = "access.grant_request.money";

    private static boolean isMoneyAction(String actionType) {
        String type = actionType == null ? "" : actionType;
        // U09-002：员工扣除上限是薪资安全阈值（BSM L1458、BSM-001 涉钱参数），与税费同按涉钱 1 个工作日。
        return type.startsWith("finance.") || type.startsWith("staff.payroll.") || type.endsWith(".money")
                || List.of("bill_waiver", "refund", "ar_ap_adjustment", "bar.bill_waiver",
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
     * M087-F-05（PGM-1.7.1「超时……自动提醒并升级到该节点的上一级，不得默认通过，也不得默认拒绝……到达最终审批节点后仍超时的，
     * 单据保持待审」）：到期单据的每条线当前节点升级到该节点审批人的直接上级；无上级（组织顶端）、主会话节点与角色节点只提醒并顺延。
     * M007-I4-F01 口径沿用：节点升级时清除该节点上低层级作出的转发（先清后升，fail-closed）。每次升级/提醒留痕。返回处理条数。
     */
    @Transactional
    public int escalateOverdue(Instant now) {
        int handled = 0;
        for (SupervisorReviewRepository.PendingReview item : supervisorReviewRepository.listOverdueSupervisorReviews(now, 200)) {
            SupervisorReviewDecisionRecord d = item.decision();
            int days = supervisorReviewRepository.findDeadlineWorkingDays(d.policyCode())
                    .orElse(isMoneyAction(d.actionType()) ? MONEY_DEADLINE_WORKING_DAYS : DEFAULT_DEADLINE_WORKING_DAYS);
            Instant nextDue = addWorkingDays(now, Math.max(days, 1));
            String beneficiaryId = item.schedule() == null ? null : item.schedule().beneficiaryId();
            List<String> escalatedNodes = new ArrayList<>();
            List<String> remindedNodes = new ArrayList<>();
            List<ApprovalChainNode> chain = supervisorReviewRepository.listChainNodes(d.reviewId());
            for (ApprovalChainNode node : ApprovalChainService.currentNodes(chain)) {
                // M087-I5-04（PGM-1.7.1「升级到达该单据的最终审批节点、不再有上一级可升后仍然超时的，单据保持待审」）：
                // 本线最后一个节点（指定最终审批人 / 顶端）是该单据的最终审批节点，只催办，不把链外的人升级进来。
                boolean finalNode = chain.stream().noneMatch(n -> n.lineCode().equals(node.lineCode()) && n.seq() > node.seq());
                Optional<String> superior = !finalNode && ApprovalChainNode.KIND_USER.equals(node.approverKind()) && node.approverId() != null
                        ? supervisorReviewRepository.findDirectSuperior(node.approverId())
                        : Optional.empty();
                if (superior.isPresent() && !superior.get().equals(d.initiatedBy()) && !superior.get().equals(beneficiaryId)) {
                    supervisorReviewRepository.updateChainNode(
                            node.withApprover(superior.get(), node.escalationCount() + 1).withForward(null, null));
                    escalatedNodes.add(node.nodeId() + "->" + superior.get());
                } else {
                    remindedNodes.add(node.nodeId());
                }
            }
            boolean escalated = !escalatedNodes.isEmpty();
            if (escalated) {
                supervisorReviewRepository.forwardSupervisorReview(d.reviewId(), null, null, null);
            }
            if (!supervisorReviewRepository.escalateSupervisorReview(d.reviewId(), d.approvalLevel(), d.requiredApproverRank(), nextDue, now)) {
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
                    Map.of("action_type", d.actionType(), "approval_level", d.approvalLevel() == null ? "" : d.approvalLevel(),
                            "escalated_nodes", String.join(",", escalatedNodes), "reminded_nodes", String.join(",", remindedNodes),
                            "due_at", nextDue.toString(),
                            "escalation_count", item.schedule() == null ? 1 : item.schedule().escalationCount() + 1)
            );
        }
        return handled;
    }

    /**
     * M007-A-02（PGM-1.7.1 转发）：当前节点的审批人（或已被转发人）把该节点逐单转发；不得转给申请人本人或直接受益人。
     * M007-I3-F01：主会话节点只能由 USB 最高权限本人转发。转发同时镜像到单据级 forwarded_to（待复核清单展示）。
     */
    @Transactional
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
                    List<ApprovalChainNode> nodes = supervisorReviewRepository.listChainNodes(id);
                    if (nodes.isEmpty()) {
                        throw ApiException.forbidden("APPROVAL_CHAIN_UNRESOLVED", "组织层级未维护，审批链未生成，无法转发");
                    }
                    ApprovalChainNode node = resolveForwardableNode(pending, nodes, actorId);
                    String target = requireText(request.forwardedTo(), "forwarded_to");
                    if (target.equals(pending.initiatedBy())
                            || schedule.map(s -> target.equals(s.beneficiaryId())).orElse(false)) {
                        throw ApiException.forbidden("SUPERVISOR_FORWARD_TARGET_FORBIDDEN", "不得转发给申请人本人或直接受益人");
                    }
                    if (ApprovalChainNode.KIND_ROLE.equals(node.approverKind())
                            && node.approverId() != null && !node.approverId().isBlank()
                            && !authorizationService.holdsPermissionViaRole(target, node.approverId())
                            && !authorizationService.holdsPermissionViaRole(target, APPROVE_PERMISSION)) {
                        throw ApiException.forbidden("SUPERVISOR_ROLE_REQUIRED", "职级不能替代该事项的审批权限点");
                    }
                    Instant now = Instant.now();
                    if (!supervisorReviewRepository.updateChainNode(node.withForward(target, actorId))) {
                        throw ApiException.conflict("SUPERVISOR_REVIEW_NOT_FOUND", "审批节点已不在待裁决状态");
                    }
                    supervisorReviewRepository.forwardSupervisorReview(id, target, actorId, now);
                    auditEventService.recordSuccess(
                            "governance.supervisor_review.forwarded",
                            APPROVE_PERMISSION,
                            "supervisor_review",
                            id,
                            null,
                            request.idempotencyKey(),
                            Map.of("node_id", node.nodeId(), "line_code", node.lineCode(), "seq", node.seq(),
                                    "forwarded_to", target, "forwarded_by", actorId == null ? "" : actorId,
                                    "comment", request.comment() == null ? "" : request.comment())
                    );
                    return supervisorReviewRepository.findSupervisorReviewSchedule(id)
                            .orElse(new SupervisorReviewRepository.ReviewSchedule(id, null, null, 0, null, target));
                },
                SupervisorReviewRepository.ReviewSchedule.class
        );
    }

    private ApprovalChainNode resolveForwardableNode(SupervisorReviewDecisionRecord pending, List<ApprovalChainNode> nodes, String actorId) {
        List<ApprovalChainNode> current = ApprovalChainService.currentNodes(nodes);
        boolean master = authorizationService.isMasterAuthority(authorizationService.currentActor());
        for (ApprovalChainNode node : current) {
            switch (node.approverKind()) {
                case ApprovalChainNode.KIND_MASTER -> {
                    // PGM-1.7.1「明确要求由最高权限审批的事项，转发由最高权限本人逐单作出」：被转发人不得再转发。
                    if (master) {
                        return node;
                    }
                }
                case ApprovalChainNode.KIND_USER -> {
                    if (actorId != null && (actorId.equals(node.approverId()) || actorId.equals(node.forwardedTo()))) {
                        return node;
                    }
                }
                case ApprovalChainNode.KIND_ROLE -> {
                    if (actorId != null && actorId.equals(node.forwardedTo())) {
                        return node;
                    }
                    if (authorizationService.currentSupervisorLevel() < roleNodeLevelGate(pending.requiredLevel())) {
                        throw ApiException.forbidden("SUPERVISOR_LEVEL_TOO_LOW", "复核人级别低于要求的复核级别");
                    }
                    String roleKey = node.approverId();
                    if (roleKey != null && !roleKey.isBlank()
                            && !authorizationService.holdsPermissionViaRole(actorId, roleKey)
                            && !authorizationService.holdsPermissionViaRole(actorId, APPROVE_PERMISSION)) {
                        throw ApiException.forbidden("SUPERVISOR_ROLE_REQUIRED", "职级不能替代该事项的审批权限点");
                    }
                    return node;
                }
                default -> {
                }
            }
        }
        if (current.stream().anyMatch(node -> ApprovalChainNode.KIND_MASTER.equals(node.approverKind()))) {
            throw ApiException.forbidden("MASTER_AUTHORITY_REQUIRED", "此项须由 USB 最高权限本人逐单转发");
        }
        throw ApiException.forbidden("SUPERVISOR_NOT_CURRENT_APPROVER", "你不是该单据当前节点的审批人，不能转发");
    }

    /** M007-A-02：待复核清单（审批人可见卡在哪一节点、到期与升级、转发状态）。 */
    /** M087-I5-01：按编号读取复核单（任意状态），供审批链页把授权申请类复核单的裁决汇入授权申请落地路径。 */
    public Optional<SupervisorReviewDecisionRecord> findReview(String reviewId) {
        return supervisorReviewRepository.findSupervisorReviewById(requireText(reviewId, "review_id"));
    }

    /**
     * M007-R6-02（PGM-1.10.1「所有业务数据默认按项目隔离……不提供关闭开关」、PGM-1.7.1「④看不到别的商户和别的项目的数据」）：
     * 待复核清单按当前操作人范围过滤——非密钥主体只看到发起人属本项目（有商户隔离键时同商户）的单据，或自己是当前节点审批人 / 被转发人
     * 的单据；持密钥最高权限跨范围查看留痕（红线④逐笔开放）。
     */
    public PageResponse<SupervisorReviewRepository.PendingReview> listPending(int limit) {
        AccessActor actor = authorizationService.currentActor();
        List<SupervisorReviewRepository.PendingReview> all = supervisorReviewRepository.listPendingSupervisorReviews(limit);
        List<SupervisorReviewRepository.PendingReview> items;
        if (authorizationService.isMasterAuthority(actor)) {
            items = all;
            auditEventService.recordSuccess("governance.supervisor_review.pending_listed_cross_scope", VIEW_PERMISSION, "supervisor_review", "pending", null, null,
                    Map.of("actor_id", actor.id(), "count", items.size()));
        } else {
            String projectId = authorizationService.requireProjectScope(actor);
            String merchantId = actor.merchantId() == null ? "" : actor.merchantId().trim();
            // ponytail: filters the <= limit rows already fetched; move the scope join into the query if pending volume grows
            items = all.stream().filter(item -> visibleTo(actor, projectId, merchantId, item)).toList();
        }
        return new PageResponse<>(items, 1, limit, items.size());
    }

    private boolean visibleTo(AccessActor actor, String projectId, String merchantId, SupervisorReviewRepository.PendingReview item) {
        String reviewId = item.decision().reviewId();
        boolean currentApprover = ApprovalChainService.currentNodes(supervisorReviewRepository.listChainNodes(reviewId)).stream()
                .anyMatch(node -> actor.id().equals(node.approverId()) || actor.id().equals(node.forwardedTo()));
        if (currentApprover || (item.schedule() != null && actor.id().equals(item.schedule().forwardedTo()))) {
            return true;
        }
        String initiator = item.decision().initiatedBy();
        boolean sameProject = authorizationService.organizationIdOf(initiator).map(projectId::equals).orElse(false);
        boolean sameMerchant = merchantId.isEmpty() || authorizationService.merchantIdOf(initiator).map(merchantId::equals).orElse(false);
        return sameProject && sameMerchant;
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

    /**
     * 角色节点的职级闸最高按上两级。第 3 级只给 USB 主会话（M032），角色事项用角色权限点而不是密钥身份。
     */
    private static int roleNodeLevelGate(int requiredLevel) {
        return Math.min(Math.max(requiredLevel, 0), 2);
    }

    // M007-F-07（PGM-1.16.2 常见复核：免单、退款、调账不设金额豁免）：三类与违规、人工放行一样恒进复核；
    // M087-A-03：税费规则与税费调整恒进三审 + 老板确认链。
    private boolean isAlwaysBlocked(String actionType) {
        String type = actionType == null ? "" : actionType;
        return type.contains("violation") || type.equals("manual_release")
                || type.equals("bill_waiver") || type.equals("refund") || type.equals("ar_ap_adjustment")
                || type.equals(TAX_RULE_ADJUST_ACTION)
                || type.contains("grant") || type.contains("authorization")
                || type.contains("debit") || type.contains("deduction")
                || type.endsWith(".money");
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
