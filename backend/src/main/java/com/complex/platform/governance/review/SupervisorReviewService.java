package com.complex.platform.governance.review;

// 文件版本：V0.24

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.common.idempotency.IdempotentReplayContext;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.WorkingDayCalendar;
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
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private com.complex.platform.access.staff.StaffHrRoutingService staffHrRouting;
    // U09-005：事件发布器必填注入——批准后的员工落地（U05 StaffApprovalService.afterDecision）依赖该事件，不得静默不发。
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    // specific_role 策略的"指定角色"以审批人须经角色持有的权限码表达（双轨收口 §24.3）。
    // 默认沿用督导复核审批权限 governance.supervisor_review.approve（须来自角色，非临时授权），
    // 从而把 specific_role 落为"按角色解析"而非旁路。
    private static final String DEFAULT_SPECIFIC_ROLE_PERMISSION = APPROVE_PERMISSION;

    // M087-A-03（PGM-1.10.1「税费规则与税费调整须经财务审核、税务审核、法务审核，再由顶层平台公司的老板最终确认后生效」）：
    // 税费调整固定三条平行线 + 主线到最高权限（老板最终确认）；发起时须为每条线指定最终审批人。
    static final String TAX_RULE_ADJUST_ACTION = "finance.tax_rule.adjust";

    // CG-12（BSM:1478 / BSM:1487 / ENUM:283 / PRD:757、779）：涉及员工的人力操作——员工状态变更、奖惩、员工申诉——
    // 固定走「HR 会签（平行线 hr_review）＋该员工直属领导向上三级（主线）」档，全部通过才生效。
    public static final String STAFF_EMPLOYMENT_STATUS_ACTION = "staff.profile.employment_status.change";
    public static final String STAFF_ADJUSTMENT_ACTION = "staff.payroll.adjustment.add";
    public static final String STAFF_ADJUSTMENT_APPEAL_ACTION = "staff.payroll.adjustment.appeal";
    public static final String HR_LINE = "hr_review";

    private final List<SupervisorReviewPolicyRecord> policies = List.of(
            new SupervisorReviewPolicyRecord("staff_payroll_scheme", "staff.payroll.scheme.change", 0, 3, true,
                    List.of("reason"), "active", ApprovalLevel.HR_THREE_LEVELS_UP.code(), null, List.of(HR_LINE, "finance_review")),
            new SupervisorReviewPolicyRecord("staff_payroll_period", "staff.payroll.period.confirm", 0, 3, true,
                    List.of("reason"), "active", ApprovalLevel.HR_THREE_LEVELS_UP.code(), null, List.of(HR_LINE, "finance_review")),
            new SupervisorReviewPolicyRecord("staff_performance_deduct", "staff.performance.points.deduct", 0, 3, true,
                    List.of("reason"), "active", ApprovalLevel.HR_THREE_LEVELS_UP.code(), null, List.of(HR_LINE)),
            new SupervisorReviewPolicyRecord("staff_performance_appeal", "staff.performance.points.appeal", 0, 3, true,
                    List.of("reason"), "active", ApprovalLevel.HR_THREE_LEVELS_UP.code(), null, List.of(HR_LINE)),
            new SupervisorReviewPolicyRecord("staff_payroll_calendar", "staff.payroll.calendar.publish", 0, 1, true,
                    List.of("reason"), "active", "one_level_up", null, List.of("finance_review")),
            new SupervisorReviewPolicyRecord("staff_performance_rule", "staff.performance.rule.publish", 0, 1, true,
                    List.of("reason"), "active", "specific_role", "staff.performance.rule.approve"),
            new SupervisorReviewPolicyRecord("bar_payroll_rule", "bar.payroll_rule.adjust", 0, 1, true,
                    List.of("reason"), "active", "specific_role", "bar.payroll.approve"),
            new SupervisorReviewPolicyRecord("amusement_business_day", "amusement.business_day.configure", 0, 3, true,
                    List.of("reason", "before_after"), "active", "top_authority"),
            new SupervisorReviewPolicyRecord("bar_business_day", "bar.business_day.configure", 0, 3, true,
                    List.of("reason", "before_after"), "active", "top_authority"),
            new SupervisorReviewPolicyRecord("inventory_purchase_request", "inventory.purchase_request", 0, 1, true,
                    List.of("reason"), "active", "specific_role", "inventory.purchase.approve"),
            new SupervisorReviewPolicyRecord("inventory_purchase_terminate", "inventory.purchase_terminate", 0, 1, true,
                    List.of("reason"), "active", "specific_role", "inventory.purchase.approve"),
            new SupervisorReviewPolicyRecord("inventory_supply_contract", "inventory.supply_contract", 0, 1, true,
                    List.of("reason"), "active", "specific_role", "inventory.purchase.approve",
                    List.of("business_review", "finance_review", "legal_review")),
            new SupervisorReviewPolicyRecord("inventory_purchase_return", "inventory.purchase_return", 0, 1, true,
                    List.of("reason"), "active", "specific_role", "inventory.purchase.approve", List.of("finance_review")),
            new SupervisorReviewPolicyRecord("inventory_purchase_prepayment_resolution", "inventory.purchase_prepayment_resolution", 0, 1, true,
                    List.of("reason"), "active", "specific_role", "inventory.purchase.approve", List.of("finance_review")),
            // HP-C01（BSM L1349「合同的新建、到期终止、变更分别使用各自配置的审批流程模板，三类均遵守同一全平台审批模型」／PGM L536「三类各走各自的审批流程模板，模板分别可配」）：
            // 三类合同事项各一条恒阻断策略；平行线由合同模板（contract_approval_flows.required_line_codes）决定、策略不固定线，
            // 主线档位 one_level_up 只作下限——发起时指定最终审批人（默认模板配置的总经理）即沿直接上级链补全到该人。
            new SupervisorReviewPolicyRecord("contract_new", "contract.new", 0, 1, true,
                    List.of("reason"), "active", "one_level_up", null, List.of()),
            new SupervisorReviewPolicyRecord("contract_expiry_termination", "contract.expiry_termination", 0, 1, true,
                    List.of("reason"), "active", "one_level_up", null, List.of()),
            new SupervisorReviewPolicyRecord("contract_change", "contract.change", 0, 1, true,
                    List.of("reason"), "active", "one_level_up", null, List.of()),
            new SupervisorReviewPolicyRecord("energy_reading_anomaly", "energy.reading_anomaly_confirm", 0, 1, true,
                    List.of("reason"), "active", "specific_role", "energy.reading.approve"),
            new SupervisorReviewPolicyRecord("energy_review_settings", "energy.review_settings", 0, 1, true,
                    List.of("reason"), "active", "one_level_up"),
            new SupervisorReviewPolicyRecord("dealer_delivery_resolution", "dealer.delivery_resolution", 0, 1, true,
                    List.of("reason"), "active", "specific_role", "dealer.delivery_resolution.approve", List.of("finance_review")),
            new SupervisorReviewPolicyRecord("inventory_quarantine_disposition", "inventory.quarantine_disposition", 0, 1, true,
                    List.of("reason"), "active", "specific_role", "inventory.quarantine.approve", List.of("finance_review")),
            // FS-011（BSM L1212「未出库数量可经审批取消」）：门店要货已提交、未出库的取消，发起人上级审批。
            new SupervisorReviewPolicyRecord("inventory_transfer_order_cancel", "inventory.transfer_order.cancel", 0, 1, true,
                    List.of("reason"), "active", "one_level_up"),
            // FS-019（BSM L1201「不可消费借用物无法归还时……由借出所有权方与借入保管方确认原因、数量和实物处置，并经主管及财务复核」）：
            // 双方确认后，确认人上级主管复核；通过后进财务复核。
            new SupervisorReviewPolicyRecord("inventory_transfer_unreturnable", "inventory.transfer_order.unreturnable", 0, 1, true,
                    List.of("reason"), "active", "one_level_up"),
            // HP-F03（BSM L1408「楼宇设备报废由设备所有权主体确认，沿现有审批流程办理，记录批准人、处置方式及实际处置凭证」）：
            // 恒阻断；发起人沿直接上级链到其指定的最终审批人（设备台账仅平台／项目物业方可写，所有权主体即该方）。
            new SupervisorReviewPolicyRecord("facility_equipment_scrap", "facility.equipment.scrap", 0, 1, true,
                    List.of("reason"), "active", "one_level_up"),
            // HP-P01（BSM L1438「免费放行和减免须经物业主管审批」）：恒阻断；最终审批人须经角色持有停车减免审批权限（物业主管）。
            new SupervisorReviewPolicyRecord("parking_release_waive", "parking.release.waive", 0, 1, true,
                    List.of("reason"), "active", "specific_role", "parking.release.approve"),
            new SupervisorReviewPolicyRecord("ticket_reward_rule", "ticket.reward.rule", 0, 1, true,
                    List.of("reason"), "active", "specific_role", "ticket.reward.rule.approve"),
            new SupervisorReviewPolicyRecord("ticket_reward_national", "ticket.reward.national", 0, 1, true,
                    List.of("reason"), "active", "specific_role", "ticket.reward.national.approve"),
            new SupervisorReviewPolicyRecord("ticket_reward_sales_contract", "ticket.reward.sales_contract", 0, 1, true,
                    List.of("reason"), "active", "specific_role", "ticket.reward.sales_contract.approve",
                    List.of("business_review", "finance_review", "legal_review")),
            new SupervisorReviewPolicyRecord("staff_shift_publish", "staff.shift.publish", 0, 1, true, List.of("reason"), "active", "one_level_up"),
            new SupervisorReviewPolicyRecord("staff_attendance_receipt_review", "staff.attendance.receipt.review", 0, 1, true, List.of("reason"), "active", "one_level_up"),
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
                    null, List.of("finance_review", "tax_review", "legal_review")),
            new SupervisorReviewPolicyRecord("staff_employment_status_hr3", STAFF_EMPLOYMENT_STATUS_ACTION, 0, 3, true, List.of("reason"), "active",
                    ApprovalLevel.HR_THREE_LEVELS_UP.code(), null, List.of(HR_LINE)),
            new SupervisorReviewPolicyRecord("staff_payroll_adjustment_hr3", STAFF_ADJUSTMENT_ACTION, 0, 3, true, List.of("reason"), "active",
                    ApprovalLevel.HR_THREE_LEVELS_UP.code(), null, List.of(HR_LINE)),
            new SupervisorReviewPolicyRecord("staff_payroll_adjustment_appeal_hr3", STAFF_ADJUSTMENT_APPEAL_ACTION, 0, 3, true, List.of("reason"), "active",
                    ApprovalLevel.HR_THREE_LEVELS_UP.code(), null, List.of(HR_LINE))
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

    // XM-ONESHOT-RETURN B01：重放核验要确认用掉放行单的那次请求已成功完成（幂等记录）。经 setter 可选注入（同 AuthorizationService），
    // 单测 new 构造不传则不认重放（照旧拦截）。
    private IdempotencyRepository idempotencyRepository;

    @Autowired(required = false)
    public void setIdempotencyRepository(IdempotencyRepository idempotencyRepository) {
        this.idempotencyRepository = idempotencyRepository;
    }

    // 第十六批第 29 条：审批通用时限的工作日按已审批的逐日工作日历判定（与积分月结、普通内部结算共用）；单测 new 构造不传则只按周末＋假期表。
    private WorkingDayCalendar workingDayCalendar;

    @Autowired(required = false)
    public void setWorkingDayCalendar(WorkingDayCalendar workingDayCalendar) {
        this.workingDayCalendar = workingDayCalendar;
    }

    // M724-F16：准入闸切面过闸期间绑在本线程上的「本次受闸调用的请求内容摘要」
    private static final ThreadLocal<String> GATED_REQUEST_CONTENT = new ThreadLocal<>();

    /** 绑定（null 即清除）并返回原值；切面在 finally 里用返回值还原。 */
    public static String bindGatedRequestContent(String contentDigest) {
        String outer = GATED_REQUEST_CONTENT.get();
        if (contentDigest == null) {
            GATED_REQUEST_CONTENT.remove();
        } else {
            GATED_REQUEST_CONTENT.set(contentDigest);
        }
        return outer;
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
                    boolean amountHit = amountCent >= policy.minAmountCent() || amountCent < 0;
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
            if (HR_LINE.equals(code) && ApprovalLevel.HR_THREE_LEVELS_UP.code().equals(policy.approvalLevel())) continue;
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
                    if (requiresBeneficiary(pending.actionType()) && schedule.map(s -> !textPresent(s.beneficiaryId())).orElse(true)) {
                        throw ApiException.forbidden("BENEFICIARY_REQUIRED", "授权事项必须登记直接受益人（被授权人）");
                    }
                    // PGM-1.7.1「申请人与直接受益人不得审批与自己相关的申请，转发不构成例外」。
                    if (schedule.map(s -> reviewerId != null && reviewerId.equals(s.beneficiaryId())).orElse(false)) {
                        throw ApiException.forbidden("SUPERVISOR_SELF_REVIEW_FORBIDDEN", "直接受益人不能复核与自己相关的申请");
                    }
                    if (reviewerId != null && (reviewerId.equals(pending.initiatedBy()) || isOriginalApplicant(pending, reviewerId))) {
                        throw ApiException.forbidden(
                                "SUPERVISOR_SELF_REVIEW_FORBIDDEN", "操作发起人不能复核自己发起的操作");
                    }
                    List<ApprovalChainNode> nodes = supervisorReviewRepository.listChainNodes(id);
                    if (nodes.isEmpty()) {
                        throw ApiException.forbidden("APPROVAL_CHAIN_UNRESOLVED",
                                "审批链未生成；请补齐组织层级、人事会签配置及有效授权后重新生成审批链");
                    }
                    ApprovalChainNode node = resolveDecidableNode(pending, nodes, reviewerId, reviewerLevel);
                    if (ApprovalLevel.HR_THREE_LEVELS_UP.code().equals(pending.approvalLevel())
                            && (staffHrRouting == null || !staffHrRouting.canReviewEmployee(reviewerId,
                                    schedule.map(SupervisorReviewRepository.ReviewSchedule::beneficiaryId).orElse(null),
                                    HR_LINE.equals(node.lineCode()) ? "staff.hr.review" : APPROVE_PERMISSION))) {
                        throw ApiException.forbidden("HR_APPROVER_SCOPE_INVALID", "人事审批人的有效角色或员工管理范围已不满足要求");
                    }
                    if (ApprovalChainNode.KIND_MASTER.equals(node.approverKind())
                            && nodes.stream().anyMatch(n -> !ApprovalChainNode.MAIN_LINE.equals(n.lineCode())
                                    && !ApprovalChainNode.APPROVED.equals(n.status()))) {
                        throw ApiException.conflict("DEPARTMENT_REVIEWS_PENDING", "须先完成各部门平行线审核，再由顶层平台最终确认");
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
            // §24.3 同商户范围闸：发起人有商户隔离键时，审批人须同商户（平台管理员越权，D-032；USB 最高权限逐笔越权）。
            Optional<String> initiatorMerchant = authorizationService.merchantIdOf(pending.initiatedBy());
            if (initiatorMerchant.isPresent()
                    && !authorizationService.isPlatformAdmin(authorizationService.currentActor())
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
     *
     * <p>M724-F16（操作员 2026-10-07 裁决「批准只能被『被拦下的那次提交的同样内容』用掉，内容不同的另一笔照常拦截送审」）：
     * 准入闸过闸期间建的复核单，建单后记下被拦下那次提交的请求内容摘要（不含提交编号），与建单同事务提交；不在过闸期间建的
     * （授权申请等）没有摘要。
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
        String blockedContent = GATED_REQUEST_CONTENT.get();
        if (blockedContent != null) {
            supervisorReviewRepository.bindSupervisorReviewToBlockedContent(decision.reviewId(), blockedContent);
        }
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

    /**
     * M002-F20（XMX-10 / XM-M002-N）：复核单所审的业务对象已被作废（如岗位高危释放单随换岗、模板停用或修改作废）时，由作废方在同一事务内
     * 关闭该复核单：待审节点置 CANCELLED，复核单置 rejected 并留痕，从审批人待办与超时升级中消失。复核单不存在或已不在待审直接返回。
     */
    public void cancelPendingReview(String reviewId, String actorId, String reason) {
        if (reviewId == null || reviewId.isBlank()) {
            return;
        }
        supervisorReviewRepository.lockSupervisorReview(reviewId);
        SupervisorReviewDecisionRecord pending = supervisorReviewRepository.findPendingSupervisorReviewById(reviewId).orElse(null);
        if (pending == null) {
            return;
        }
        Instant now = Instant.now();
        List<String> cancelledNodes = new ArrayList<>();
        for (ApprovalChainNode node : supervisorReviewRepository.listChainNodes(reviewId)) {
            if (node.pending() && supervisorReviewRepository.updateChainNode(node.withStatus(ApprovalChainNode.CANCELLED, actorId, now))) {
                cancelledNodes.add(node.nodeId());
            }
        }
        supervisorReviewRepository.saveSupervisorReview(new SupervisorReviewDecisionRecord(
                reviewId, pending.policyCode(), pending.actionType(), pending.objectType(), pending.objectId(), false,
                "rejected", pending.requiredLevel(), pending.reviewerLevel(), "rejected", pending.reason(),
                pending.evidenceAttachmentIds(), now, pending.initiatedBy(), pending.approvalLevel(), pending.requiredApproverRank()));
        auditEventService.recordSuccess("governance.supervisor_review.cancelled", APPROVE_PERMISSION, "supervisor_review", reviewId,
                null, null, Map.of("reason", reason == null ? "" : reason, "action_type", pending.actionType(),
                        "object_type", pending.objectType() == null ? "" : pending.objectType(),
                        "object_id", pending.objectId() == null ? "" : pending.objectId(),
                        "cancelled_nodes", String.join(",", cancelledNodes), "operator_id", actorId == null ? "" : actorId));
    }

    /** M087：复核单的审批链节点（主线 + 平行线，按线与序号）。 */
    public List<ApprovalChainNode> chain(String reviewId) {
        String id = requireText(reviewId, "review_id");
        SupervisorReviewDecisionRecord record = supervisorReviewRepository.findSupervisorReviewById(id)
                .orElseThrow(() -> ApiException.notFound("SUPERVISOR_REVIEW_NOT_FOUND", "主管复核单不存在"));
        requireVisibleReview(record);
        return supervisorReviewRepository.listChainNodes(id);
    }

    /**
     * M087：组织层级补齐后为「链未生成」的待审单重新生成主线；人事自动会签一并重解析，其余平行线须重新发起。
     * 已有节点的单据不重生成（返回现有节点）。
     */
    @Transactional
    public List<ApprovalChainNode> regenerateChain(String reviewId) {
        String id = requireText(reviewId, "review_id");
        supervisorReviewRepository.lockSupervisorReview(id);
        SupervisorReviewDecisionRecord pending = supervisorReviewRepository
                .findPendingSupervisorReviewById(id)
                .orElseThrow(() -> ApiException.notFound("SUPERVISOR_REVIEW_NOT_FOUND", "主管复核单不存在或已不在待复核状态"));
        List<ApprovalChainNode> existing = supervisorReviewRepository.listChainNodes(id);
        requireVisibleReview(pending);
        if (!existing.isEmpty()) {
            return existing;
        }
        SupervisorReviewPolicyRecord policy = findPolicyByCode(pending.policyCode());
        if (policy != null && policy.parallelLineCodes().stream().anyMatch(code ->
                !HR_LINE.equals(code) || !ApprovalLevel.HR_THREE_LEVELS_UP.code().equals(policy.approvalLevel()))) {
            throw ApiException.conflict("APPROVAL_LINE_FINAL_APPROVER_REQUIRED",
                    "本事项含平行审批线，组织层级补齐后须重新发起并指定各线最终审批人");
        }
        String beneficiaryId = supervisorReviewRepository.findSupervisorReviewSchedule(id)
                .map(SupervisorReviewRepository.ReviewSchedule::beneficiaryId).orElse(null);
        List<ApprovalChainNode> nodes = generateChain(id, pending.initiatedBy(), beneficiaryId,
                ApprovalLevel.fromCode(pending.approvalLevel()), null, List.of(), pending.requiredLevel());
        if (nodes.isEmpty()) {
            throw ApiException.conflict("APPROVAL_CHAIN_UNRESOLVED", "组织层级、人事会签配置或有效授权尚未补齐，审批链无法生成");
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
        List<ApprovalChainNode> nodes;
        if (level == ApprovalLevel.HR_THREE_LEVELS_UP) {
            var hr = staffHrRouting == null ? Optional.<String>empty() : staffHrRouting.resolve(beneficiaryId, initiatedBy);
            nodes = List.of();
            if (hr.isPresent()) {
                try {
                    List<ApprovalChainService.ParallelLine> resolvedLines = new ArrayList<>();
                    resolvedLines.add(new ApprovalChainService.ParallelLine(HR_LINE, hr.get()));
                    lines.stream().filter(line -> !HR_LINE.equals(line.lineCode())).forEach(resolvedLines::add);
                    nodes = chainService.generate(reviewId, initiatedBy, beneficiaryId, level, null,
                            resolvedLines, requiredLevel);
                    if (nodes.stream().anyMatch(n -> HR_LINE.equals(n.lineCode())
                            ? !staffHrRouting.eligible(n.approverId(), beneficiaryId, initiatedBy)
                            : !staffHrRouting.canReviewEmployee(n.approverId(), beneficiaryId, APPROVE_PERMISSION))) {
                        nodes = List.of();
                    }
                } catch (ApiException exception) {
                    if (!java.util.Set.of("APPROVAL_LINE_NOT_CONFIGURED", "APPROVAL_LINE_EMPTY", "FINAL_APPROVER_NOT_IN_CHAIN")
                            .contains(exception.code())) throw exception;
                    nodes = List.of();
                }
            }
        } else {
            nodes = chainService.generate(reviewId, initiatedBy, beneficiaryId, level, finalApproverId, lines, requiredLevel);
        }
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

    /** 评估入口不得自报授出档位；带平行线的事项不得被名称改写成无平行线的最高权限档。 */
    private SupervisorReviewPolicyRecord resolvePolicy(String actionType, Long amountCent) {
        if ("permission_grant_l1".equals(actionType) || "permission_grant_l2".equals(actionType)) {
            throw ApiException.badRequest("GRANT_LEVEL_NOT_SELECTABLE", "授出档位不由入口自报，资金分红须最高权限，其它授出由准入闸按事项落档");
        }
        SupervisorReviewPolicyRecord original = policies.stream()
                .filter(policy -> policy.actionType().equals(actionType))
                .min(Comparator.comparing(SupervisorReviewPolicyRecord::policyCode))
                .orElse(null);
        if (original != null && !original.parallelLineCodes().isEmpty()) {
            return original;
        }
        if (requiresTopAuthorityGrant(actionType, amountCent)) {
            SupervisorReviewPolicyRecord top = findPolicyByCode("permission_grant_l3");
            if (top != null) {
                return top;
            }
        }
        return findPolicy(actionType);
    }

    /** XM-D029-005（PGM-041「持股分红……由最高权限审批」；其余高危授权按可配政策与指定最终审批人的审批链）：只有分红类升最高权限档。 */
    private boolean requiresTopAuthorityGrant(String actionType, Long amountCent) {
        String type = actionType == null ? "" : actionType;
        return type.contains("dividend");
    }

    /**
     * XM-D029-003（治理地图第 529 行「申请人与直接受益人不得审批」）：只对授权类强制登记直接受益人——受益人是被授权的员工账号，
     * 与审批人、转发目标同一编号体系，比较真实生效。免单、退款等准入路径的收款人是客人或往来方，与员工账号不同编号体系，
     * 不在此强制（缺员工与客人／往来方的身份关联，另登记 XM）。
     */
    private boolean requiresBeneficiary(String actionType) {
        String type = actionType == null ? "" : actionType;
        return type.contains("grant") || type.contains("authorization")
                || type.equals("access.grant_request.money") || type.equals("access.scope_change.money")
                || policies.stream().anyMatch(policy -> type.equals(policy.actionType())
                        && ApprovalLevel.HR_THREE_LEVELS_UP.code().equals(policy.approvalLevel()));
    }

    private boolean isOriginalApplicant(SupervisorReviewDecisionRecord review, String userId) {
        if (!"supply_contract".equals(review.objectType()) && !"inventory_purchase_request".equals(review.objectType())) return false;
        String original = supervisorReviewRepository.findOriginalApplicant(review.objectType(), review.objectId())
                .orElseThrow(() -> ApiException.conflict("SUPERVISOR_ORIGINAL_APPLICANT_REQUIRED", "须核实原申请人后再办理审批"));
        return original.equals(userId);
    }

    private void requireBeneficiaryIfNeeded(String actionType, String beneficiaryId) {
        if (requiresBeneficiary(actionType) && !textPresent(beneficiaryId)) {
            throw ApiException.badRequest("BENEFICIARY_REQUIRED", "授权事项必须登记直接受益人（被授权人）");
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
     * 未配置时涉钱 1 个工作日、其余 3 个工作日。工作日按已审批的逐日工作日历判定（计入法定调休上班日、不计法定假日），
     * 该日未审批的按周一至周五且不在 holiday_calendar 内。
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
        return type.startsWith("finance.") || type.startsWith("staff.payroll.") || type.startsWith("ticket.reward.")
                || type.equals("dealer.delivery_resolution") || type.equals("inventory.quarantine_disposition") || type.endsWith(".money")
                || List.of("bill_waiver", "refund", "ar_ap_adjustment", "bar.bill_waiver",
                "ticketing.refund_approve", "ticketing.verification_reverse").contains(type);
    }

    // 第十六批第 29 条（PGM L529「出厂默认 3 个工作日，涉钱审批出厂默认 1 个工作日」）：逐日先看已审批的工作日历
    // （finance.WorkingDayCalendar，ticket_points_calendar_days.working_day，含调休上班的周末）；该日未审批的仍按周末＋假期表。
    Instant addWorkingDays(Instant from, int workingDays) {
        java.time.ZonedDateTime at = from.atZone(com.complex.platform.common.LocalZone.ID);
        java.time.LocalDate day = at.toLocalDate();
        java.util.Set<java.time.LocalDate> holidays = new java.util.HashSet<>(
                supervisorReviewRepository.findHolidayDates(day, day.plusDays(workingDays * 3L + 14)));
        int remaining = workingDays;
        while (remaining > 0) {
            day = day.plusDays(1);
            java.time.LocalDate d = day;
            Optional<WorkingDayCalendar.Day> approved = workingDayCalendar == null ? Optional.empty() : workingDayCalendar.find(d);
            boolean working = approved.map(WorkingDayCalendar.Day::workingDay)
                    .orElseGet(() -> d.getDayOfWeek() != java.time.DayOfWeek.SATURDAY
                            && d.getDayOfWeek() != java.time.DayOfWeek.SUNDAY && !holidays.contains(d));
            if (working) {
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
                if (ApprovalLevel.HR_THREE_LEVELS_UP.code().equals(d.approvalLevel())) {
                    // 人事链边界按生成时已确定的三个组织层级保留，重复超时也不能引入第四级。
                    superior = superior.filter(candidate -> chain.stream().anyMatch(n ->
                            n.lineCode().equals(node.lineCode()) && n.seq() > node.seq() && candidate.equals(n.approverId())))
                            .filter(candidate -> staffHrRouting != null && staffHrRouting.canReviewEmployee(candidate, beneficiaryId,
                                    HR_LINE.equals(node.lineCode()) ? "staff.hr.review" : APPROVE_PERMISSION));
                }
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
                    supervisorReviewRepository.lockSupervisorReview(id);
                    SupervisorReviewDecisionRecord pending = supervisorReviewRepository
                            .findPendingSupervisorReviewById(id)
                            .orElseThrow(() -> ApiException.notFound("SUPERVISOR_REVIEW_NOT_FOUND", "主管复核单不存在或已不在待复核状态"));
                    String actorId = authorizationService.currentActor().id();
                    if (isOriginalApplicant(pending, actorId)) {
                        throw ApiException.forbidden("SUPERVISOR_SELF_REVIEW_FORBIDDEN", "原申请人不能转交自己申请的审批节点");
                    }
                    Optional<SupervisorReviewRepository.ReviewSchedule> schedule = supervisorReviewRepository.findSupervisorReviewSchedule(id);
                    if (requiresBeneficiary(pending.actionType()) && schedule.map(s -> !textPresent(s.beneficiaryId())).orElse(true)) {
                        throw ApiException.forbidden("BENEFICIARY_REQUIRED", "授权事项必须登记直接受益人（被授权人）");
                    }
                    List<ApprovalChainNode> nodes = supervisorReviewRepository.listChainNodes(id);
                    if (nodes.isEmpty()) {
                        throw ApiException.forbidden("APPROVAL_CHAIN_UNRESOLVED", "组织层级未维护，审批链未生成，无法转发");
                    }
                    ApprovalChainNode node = resolveForwardableNode(pending, nodes, actorId);
                    String target = requireText(request.forwardedTo(), "forwarded_to");
                    if (target.equals(pending.initiatedBy()) || isOriginalApplicant(pending, target)
                            || schedule.map(s -> target.equals(s.beneficiaryId())).orElse(false)) {
                        throw ApiException.forbidden("SUPERVISOR_FORWARD_TARGET_FORBIDDEN", "不得转发给申请人本人或直接受益人");
                    }
                    if (ApprovalLevel.HR_THREE_LEVELS_UP.code().equals(pending.approvalLevel())
                            && (staffHrRouting == null || !staffHrRouting.canReviewEmployee(target,
                                    schedule.map(SupervisorReviewRepository.ReviewSchedule::beneficiaryId).orElse(null),
                                    HR_LINE.equals(node.lineCode()) ? "staff.hr.review" : APPROVE_PERMISSION))) {
                        throw ApiException.forbidden("HR_APPROVER_SCOPE_INVALID", "被转交人须具备对应审批角色及员工组织、商户和项目范围");
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
    public SupervisorReviewDecisionRecord readReview(String reviewId) {
        try {
            authorizationService.requirePermission(VIEW_PERMISSION);
            SupervisorReviewDecisionRecord record = findReview(reviewId)
                    .orElseThrow(() -> ApiException.notFound("SUPERVISOR_REVIEW_NOT_FOUND", "主管复核单不存在"));
            auditEventService.recordSuccess("governance.supervisor_review.original_viewed", VIEW_PERMISSION,
                    "supervisor_review", record.reviewId(), null, null, Map.of("review_status", record.reviewStatus()));
            return record;
        } catch (RuntimeException failure) {
            auditEventService.recordFailure("governance.supervisor_review.original_view_failed", VIEW_PERMISSION,
                    "supervisor_review", reviewId, null, null,
                    failure instanceof ApiException api ? api.code() : failure.getClass().getSimpleName());
            throw failure;
        }
    }

    public Optional<SupervisorReviewDecisionRecord> findReview(String reviewId) {
        Optional<SupervisorReviewDecisionRecord> found = supervisorReviewRepository.findSupervisorReviewById(requireText(reviewId, "review_id"));
        found.ifPresent(this::requireVisibleReview);
        return found;
    }

    /**
     * M007-R6-02（PGM-1.10.1「所有业务数据默认按项目隔离……不提供关闭开关」、PGM-1.7.1「④看不到别的商户和别的项目的数据」）：
     * 待复核清单按当前操作人范围过滤——平台管理员跨范围查看留痕（D-032）；其余主体只看到发起人属本项目（有商户隔离键时同商户）的单据，
     * 或自己是当前节点审批人 / 被转发人 / 发起人的单据。持密钥最高权限也按此过滤，不整表（红线④逐笔开放：按编号读取见 requireVisibleReview）。
     */
    public PageResponse<SupervisorReviewRepository.PendingReview> listPending(int limit) {
        AccessActor actor = authorizationService.currentActor();
        List<SupervisorReviewRepository.PendingReview> all = supervisorReviewRepository.listPendingSupervisorReviews(limit);
        List<SupervisorReviewRepository.PendingReview> items;
        if (authorizationService.isPlatformAdmin(actor)) {
            items = all;
            auditEventService.recordSuccess("governance.supervisor_review.pending_listed_cross_scope", VIEW_PERMISSION, "supervisor_review", "pending", null, null,
                    Map.of("actor_id", actor.id(), "count", items.size()));
        } else {
            String projectId = scopeProjectOf(actor);
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
        if (actor.id().equals(initiator)) {
            return true;
        }
        boolean sameProject = authorizationService.organizationIdOf(initiator).map(projectId::equals).orElse(false);
        Optional<String> initiatorMerchant = authorizationService.merchantIdOf(initiator);
        boolean sameMerchant = merchantId.isEmpty()
                ? initiatorMerchant.isEmpty()
                : initiatorMerchant.map(merchantId::equals).orElse(false);
        return sameProject && sameMerchant;
    }

    private void requireVisibleReview(SupervisorReviewDecisionRecord record) {
        AccessActor actor = authorizationService.currentActor();
        if (authorizationService.isPlatformAdmin(actor)) {
            return;
        }
        String projectId = scopeProjectOf(actor);
        String merchantId = actor.merchantId() == null ? "" : actor.merchantId().trim();
        SupervisorReviewRepository.PendingReview item = new SupervisorReviewRepository.PendingReview(
                record, supervisorReviewRepository.findSupervisorReviewSchedule(record.reviewId()).orElse(null));
        if (!visibleTo(actor, projectId, merchantId, item)) {
            if (authorizationService.isMasterAuthority(actor)) {
                // XM-D029-002（红线④「持授权 U 盘密钥的最高权限可以逐笔开放，每次只对当前这一笔生效并留痕」）：按编号逐笔放行并留痕。
                auditEventService.recordSuccess("governance.supervisor_review.viewed_cross_scope", VIEW_PERMISSION, "supervisor_review",
                        record.reviewId(), null, null, Map.of("actor_id", actor.id(), "review_id", record.reviewId()));
                return;
            }
            throw ApiException.forbidden("SUPERVISOR_REVIEW_SCOPE_FORBIDDEN", "看不到别的商户或别的项目的复核单");
        }
    }

    /** 可见范围的项目：最高权限可无项目号（只见本人为当前节点、被转发人或发起人的单），其余主体无项目号即拒。 */
    private String scopeProjectOf(AccessActor actor) {
        if (authorizationService.isMasterAuthority(actor)) {
            return actor.organizationId() == null ? "" : actor.organizationId().trim();
        }
        return authorizationService.requireProjectScope(actor);
    }

    /**
     * M007-A-01（PGM-1.2 第 5 步「放行结果都要记录」、PGM-1.7.1「审批通过才生效」「逐笔放行」）：
     * 准入闸在拦截前调用——同动作、同对象、同发起人且已批准未放行的复核单被消费一次并放行本次动作，
     * 留痕 admission.release_executed；批准消费后再次执行须重新审批。
     *
     * <p>XM-ONESHOT-RETURN B01（治理地图第 303 行及操作员 2026-09-25 确认的追加句「只有这次操作成功完成、返回结果确认后才算用掉」）：
     * 消费并入调用方事务（准入闸切面把闸口与业务方法包进同一事务），业务失败回滚时放行单与留痕一起回滚、仍可再放行一次。
     * 取得到本次请求的幂等键与请求摘要时一并记到复核单上，并把本请求记为用掉了单次来源（幂等层据此把成功记录记到这次请求名下），
     * 供 {@link #admitReplayOfReleasingRequest} 认出重放。
     *
     * <p>M724-F16（治理地图第 528 行「每次只能对当前这一笔放行、不得整类关闭校验」；操作员 2026-10-07 裁决「批准只能被『被拦下的
     * 那次提交的同样内容』用掉，内容不同的另一笔照常拦截送审」）：除同动作、同对象、同发起人外，还须本次提交的请求内容摘要与复核单上
     * 记的被拦下那次提交的摘要相同才消费；没有本次提交的内容摘要、或复核单上没有摘要的，一律不消费。
     */
    public Optional<SupervisorReviewDecisionRecord> consumeApprovedRelease(
            String actionType, String objectType, String objectId, String actorId) {
        String content = GATED_REQUEST_CONTENT.get();
        if (content == null) {
            return Optional.empty(); // 没有本次提交的内容摘要：不能用掉任何放行单
        }
        String type = objectType == null || objectType.isBlank() ? actionType : objectType;
        String id = objectId == null || objectId.isBlank() ? actionType : objectId;
        IdempotentReplayContext request = IdempotentReplayContext.current();
        boolean recordRequest = request != null && request.identified();
        Optional<SupervisorReviewDecisionRecord> released = supervisorReviewRepository
                .findApprovedUnreleasedSupervisorReview(actionType, type, id, actorId, content)
                .filter(record -> supervisorReviewRepository.markSupervisorReviewReleased(record.reviewId(), actorId, Instant.now(),
                        recordRequest ? request.idempotencyKey() : null, recordRequest ? request.requestDigest() : null));
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
        if (released.isPresent() && request != null) {
            // M723 起上下文按来源记消耗；放行单的来源键与权限来源键（来源类型:来源号）不同名，不会被权限核验当成已消耗的授权
            request.markConsumed(actorId, "admission_release:" + released.get().reviewId());
        }
        return released;
    }

    /**
     * XM-ONESHOT-RETURN B01（治理地图第 303 行「同一个操作被重复提交，系统只认一次」及操作员 2026-09-25 确认的追加句
     * 「同一次操作重复提交时，返回第一次的结果」）：本请求是否正是「用掉这张放行单的那次成功请求」的重放。三项同时成立才认：
     * ① 同一幂等键、同一请求摘要（方法 + 路径 + 查询串）；② 同动作、同对象的已批准复核单记着正是这次请求放行用掉的；
     * ③ 放行人那次请求的幂等记录已成功完成且记在放行人名下（第一次失败时放行与幂等记录一起回滚，没有可认的记录）。
     * 认出后本请求记为「只能重放」：幂等层只返回第一次的结果、不再执行；准入闸据此放行，不再用掉别的放行单、不新建待审复核单。
     * 取不到幂等键或请求摘要、不在控制器调用里、未注入幂等仓（单测）时一律不认，按原口径拦截。
     * 认出前本请求已新用掉一张单次授权的，结果照常返回并提示已完成，请求事务切面把这次消耗整笔回滚、那张单次授权不算用掉
     * （M723-F04；第 303 行追加句「不扣减」）。
     *
     * <p>M724-F03（治理地图第 656 行「同一次操作重复提交时，直接返回第一次的结果并提示已完成，不再重复执行、也不再扣减」；操作员裁决
     * 「同一项目、同一商户的员工用同一个提交编号，只算一次、返回第一次的结果」）：认重放不限定操作人——同项目同商户的同事用同一提交编号
     * 重提同一请求，同样在闸口放行、拿到第一次的结果，不再被拦下另建一张待审复核单。
     * 安全性：「只能重放」意味着幂等层只返回第一次的结果、绝不执行；能不能读到第一次的结果由幂等层按提交范围判——同范围返回第一次的
     * 结果，其他范围 409 {@code IDEMPOTENCY_KEY_USED}、不给结果（见 {@code common.idempotency.IdempotencyService} 的重放分支）；
     * 闸口之后的权限核验照常。
     */
    public boolean admitReplayOfReleasingRequest(String actionType, String objectType, String objectId, String actorId) {
        IdempotentReplayContext request = IdempotentReplayContext.current();
        if (request == null || !request.identified() || actorId == null || idempotencyRepository == null) {
            return false;
        }
        String type = objectType == null || objectType.isBlank() ? actionType : objectType;
        String id = objectId == null || objectId.isBlank() ? actionType : objectId;
        Optional<String> releasedBy = supervisorReviewRepository.findReleaserOfSupervisorReviewReleasedToRequest(
                actionType, type, id, request.idempotencyKey(), request.requestDigest());
        if (releasedBy.isEmpty() || !idempotencyRepository.hasCompletedSingleUseRequest(
                releasedBy.get(), request.idempotencyKey(), request.requestDigest())) {
            return false;
        }
        request.markReplayOnly();
        return true;
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
                || type.contains("penalty") || type.contains("payroll")
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
