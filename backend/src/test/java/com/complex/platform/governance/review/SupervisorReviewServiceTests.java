// 文件版本：V0.03
package com.complex.platform.governance.review;

import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.chain.ApprovalChainNode;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * M087 起：审批人身份由组织层级节点链决定（PGM-1.7.1 逐级审批），档位只作节点数下限；specific_role 策略自 R-003 起同样走直接上级链
 * （策略级别作节点数下限，链尾角色节点 / 指定最终审批人）。
 * 夹具：{@link InMemorySupervisorReviewRepository#superiors} 为直接上级表（org_hierarchy），各用例按需配置。
 */
class SupervisorReviewServiceTests {
    // U09-005/U12-003：记录服务发布的终态事件，供用例断言。
    final java.util.List<Object> publishedEvents = new java.util.ArrayList<>();

    private InMemoryAuditEventRepository auditEventRepository;
    private InMemorySupervisorReviewRepository reviewRepository;
    private SupervisorReviewService service;

    @BeforeEach
    void setUp() {
        auditEventRepository = new InMemoryAuditEventRepository();
        reviewRepository = new InMemorySupervisorReviewRepository();
        service = new SupervisorReviewService(
                new AuthorizationService(levelAwareRepository(SHARED_RANKS, null)),
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                reviewRepository,
                new AuditEventService(auditEventRepository), publishedEvents::add
        );
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    @Test
    void listPoliciesReturnsBlockingCatalogAndWritesAudit() {
        var page = service.listPolicies();

        assertThat(page.total()).isEqualTo(12); // 含排班发布和普通扣除上限审批。
        assertThat(page.items())
                .extracting(SupervisorReviewPolicyRecord::actionType)
                .contains("manual_release", "bill_waiver", "contract_violation", "base_definition_violation",
                        "permission_grant_l1", "permission_grant_l2", "permission_grant_l3",
                        "staff.shift.publish", "staff.payroll.penalty_cap.adjust");
        assertThat(page.items())
                .filteredOn(policy -> "contract_violation".equals(policy.actionType()))
                .first()
                .satisfies(policy -> {
                    assertThat(policy.requiredLevel()).isEqualTo(3);
                    assertThat(policy.blocksByDefault()).isTrue();
                });
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("governance.supervisor_review.policies_viewed");
    }

    @Test
    void evaluateBlocksContractViolationByDefault() {
        TestSecurityActors.asSupervisorLevel(1);
        var decision = service.evaluate(evaluate("review-evaluate-1", "contract definition mismatch", List.of("att_1"),
                "contract_violation", "contract", "contract_1", 0L, "contract subject differs from operation subject"));

        assertThat(decision.blocked()).isTrue();
        assertThat(decision.reviewStatus()).isEqualTo("pending_review");
        assertThat(decision.requiredLevel()).isEqualTo(3);
        assertThat(decision.evidenceAttachmentIds()).containsExactly("att_1");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("governance.supervisor_review.evaluated");
    }

    @Test
    void evaluateRejectsUnknownActionType() {
        assertThatThrownBy(() -> service.evaluate(evaluate("review-evaluate-unknown", "unknown action", List.of(),
                "unsupported_action", "bill", "bill_1", 0L, null)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("UNKNOWN_REVIEW_ACTION"));
    }

    // M087-A-02（PGM-1.7.1 逐级审批）：不在链上的人无论请求体自报什么级别都不能裁决。
    @Test
    void approveRejectsCallerOutsideTheChainRegardlessOfRequestBody() {
        chainL1();
        TestSecurityActors.asSupervisorLevel(1);
        var pending = service.evaluate(evaluate("review-p0-red", "contract violation", List.of(),
                "contract_violation", "contract", "contract_p0", 0L, "violation"));

        TestSecurityActors.asSupervisor("usr_outsider", 3);
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "approve-p0-red", "approve", List.of(), 3, "approved", "spoofed")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SUPERVISOR_NOT_CURRENT_APPROVER"));
    }

    // M087-A-02：链上靠后的节点审批人在轮到之前不能裁决（不能跳过中间直接上级）。
    @Test
    void approveRejectsLaterNodeApproverBeforeTheEarlierNodeIsDecided() {
        chainL1();
        TestSecurityActors.asSupervisorLevel(1);
        var pending = service.evaluate(evaluate("review-evaluate-level3", "contract violation", List.of("att_2"),
                "contract_violation", "contract", "contract_1", 0L, "contract mismatch"));

        TestSecurityActors.asSupervisor("usr_reviewer_c", 3);
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-approve-skip", "approve", List.of("att_2"), 3, "approved", "skip")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("APPROVAL_NODE_NOT_REACHED"));
    }

    // M087-A-02（PGM-1.7.1「逐级审批」「主线……全部通过才生效」）：两级链逐节点通过，最终节点通过后单据才 approved。
    @Test
    void approveAdvancesNodeByNodeUntilTheFinalApproverDecides() {
        chainL1();
        TestSecurityActors.asSupervisorLevel(1);
        var pending = service.evaluate(evaluate("review-evaluate-approve", "contract violation", List.of("att_3"),
                "contract_violation", "contract", "contract_2", 0L, "contract mismatch"));
        assertThat(reviewRepository.listChainNodes(pending.reviewId())).extracting(ApprovalChainNode::approverId)
                .containsExactly("usr_reviewer_a", "usr_reviewer_c");

        TestSecurityActors.asSupervisor("usr_reviewer_a", 3);
        var first = service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-approve-1", "approve", List.of("att_3"), 3, "approved", "evidence checked"));
        assertThat(first.reviewStatus()).isEqualTo("pending_review");
        assertThat(reviewRepository.listChainNodes(pending.reviewId())).extracting(ApprovalChainNode::status)
                .containsExactly("approved", "pending");

        TestSecurityActors.asSupervisor("usr_reviewer_c", 3);
        var decision = service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-approve-2", "approve", List.of("att_3"), 3, "approved", "final"));
        assertThat(decision.reviewId()).isEqualTo(pending.reviewId());
        assertThat(decision.reviewStatus()).isEqualTo("approved");
        assertThat(decision.reviewerLevel()).isEqualTo(2); // M032：非密钥主体主管级封顶上两级；usr_reviewer_c rank 30 → 2，不是请求体的 3
        assertThat(decision.evidenceAttachmentIds()).containsExactly("att_3");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("governance.supervisor_review.chain_generated", "governance.supervisor_review.node_decided",
                        "governance.supervisor_review.decided");
    }

    // M087-A-02（PGM-1.7.1「任一节点驳回则整单退回起草人」）：中间节点驳回即整单 rejected，其余节点作废。
    @Test
    void rejectionAtAnyNodeReturnsTheWholeTicket() {
        chainL1();
        TestSecurityActors.asSupervisorLevel(1);
        var pending = service.evaluate(evaluate("review-reject-chain", "v", List.of(),
                "contract_violation", "contract", "contract_reject", 0L, "violation"));

        TestSecurityActors.asSupervisor("usr_reviewer_a", 3);
        var rejected = service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-reject-node1", "reject", List.of(), 3, "rejected", "not acceptable"));
        assertThat(rejected.reviewStatus()).isEqualTo("rejected");
        assertThat(reviewRepository.listChainNodes(pending.reviewId())).extracting(ApprovalChainNode::status)
                .containsExactly("rejected", "cancelled");
        TestSecurityActors.asSupervisor("usr_reviewer_c", 3);
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-after-reject", "approve", List.of(), 3, "approved", "late")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SUPERVISOR_REVIEW_NOT_FOUND"));
    }

    // M087-A-03（PGM-1.7.1「需要平行线审批的另行指定该条线的最终审批人……主线与各条平行线全部通过……才生效」）。
    @Test
    void parallelLineMustPassBeforeTheTicketTakesEffect() {
        chainL1();
        reviewRepository.superiors.put("usr_fin_entry", "usr_fin_lead");
        reviewRepository.lines.put("finance", new SupervisorReviewRepository.ApprovalLine("finance", "财务线", 1, "usr_fin_entry", "active"));
        TestSecurityActors.asSupervisorLevel(1);
        var pending = service.evaluate(new SupervisorReviewEvaluateRequest("review-parallel", "v", List.of(),
                "contract_violation", "contract", "contract_parallel", 0L, "violation", "usr_reviewer_c",
                List.of(new SupervisorReviewEvaluateRequest.ParallelLine("finance", "usr_fin_lead"))));
        assertThat(reviewRepository.listChainNodes(pending.reviewId())).extracting(ApprovalChainNode::lineCode, ApprovalChainNode::approverId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("main", "usr_reviewer_a"), org.assertj.core.groups.Tuple.tuple("main", "usr_reviewer_c"),
                        org.assertj.core.groups.Tuple.tuple("finance", "usr_fin_entry"), org.assertj.core.groups.Tuple.tuple("finance", "usr_fin_lead"));

        TestSecurityActors.asSupervisor("usr_reviewer_a", 3);
        service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("p-main-1", "ok", List.of(), 3, "approved", "ok"));
        TestSecurityActors.asSupervisor("usr_reviewer_c", 3);
        assertThat(service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("p-main-2", "ok", List.of(), 3, "approved", "ok"))
                .reviewStatus()).isEqualTo("pending_review"); // 主线全通过，财务线未通过 → 不生效
        TestSecurityActors.asSupervisor("usr_fin_entry", 1);
        service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("p-fin-1", "ok", List.of(), 1, "approved", "ok"));
        TestSecurityActors.asSupervisor("usr_fin_lead", 3);
        assertThat(service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("p-fin-2", "ok", List.of(), 3, "approved", "ok"))
                .reviewStatus()).isEqualTo("approved");
    }

    // Segregation of duties: the supervisor who initiated the action cannot approve their own review,
    // even when they hold a sufficient level.
    @Test
    void approveRejectsSelfReviewByInitiator() {
        TestSecurityActors.asSupervisor("usr_initiator_l3", 3);
        var pending = service.evaluate(evaluate("review-self-review", "contract violation", List.of(),
                "contract_violation", "contract", "contract_self", 0L, "violation"));

        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-self-approve", "approve", List.of(), 3, "approved", "self approval attempt")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
    }

    @Test
    void approveRejectsUnknownReviewId() {
        TestSecurityActors.asSupervisorLevel(3);
        assertThatThrownBy(() -> service.approve("review_missing", new SupervisorReviewApprovalRequest(
                "review-approve-missing", "approve missing", List.of("att_missing"), 3, "approved", "should not fabricate approval")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SUPERVISOR_REVIEW_NOT_FOUND"));
    }

    @Test
    void approveUsesRequiredLevelFromEvaluatedReview() {
        reviewRepository.superiors.put("usr_supervisor_l2", "usr_reviewer_b"); // R-003：角色审批事项也走组织链
        TestSecurityActors.asSupervisorLevel(2);
        var pending = service.evaluate(evaluate("review-evaluate-refund", "refund needs review", List.of("att_refund"),
                "refund", "ticket_refund", "refund_1", 100L, null, "usr_payee"));

        // R-003：specific_role 策略 = 直接上级节点 + 链尾按角色解析的节点；主管级别闸沿用于角色节点。
        TestSecurityActors.asSupervisor("usr_reviewer_b", 2);
        service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-approve-refund", "approve refund", List.of("att_refund"), 2, "approved", "superior checked"));
        var approved = service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-approve-refund-role", "approve refund", List.of("att_refund"), 2, "approved", "refund evidence checked"));

        assertThat(pending.requiredLevel()).isEqualTo(2);
        assertThat(approved.requiredLevel()).isEqualTo(2);
        assertThat(approved.reviewerLevel()).isEqualTo(2); // 从安全上下文取
        assertThat(approved.reviewStatus()).isEqualTo("approved");
    }

    // ── 档位（REQ-69）自 M087 起只作节点数下限并留痕 ──────────────────────────

    @Test
    void evaluateRecordsEscalationApprovalLevelOnDecision() {
        TestSecurityActors.asSupervisorLevel(1);
        var contract = service.evaluate(evaluate("review-approval-level-contract", "v", List.of(),
                "contract_violation", "contract", "c1", 0L, "violation"));
        assertThat(contract.approvalLevel()).isEqualTo("two_levels_up");
        // 可解析职级的夹具：发起人 L1 → rank 3，two_levels_up → 所需审批人 rank 9（留痕）。
        assertThat(contract.requiredApproverRank()).isEqualTo(9);

        var baseDef = service.evaluate(evaluate("review-approval-level-base", "v", List.of(),
                "base_definition_violation", "definition", "d1", 0L, "violation"));
        assertThat(baseDef.approvalLevel()).isEqualTo("top_authority");
    }

    // M087-A-02（PGM-1.10.1「敏感动作可要求上一级、上两级或最高权限审批」为下限）：指定的最终审批人不得低于档位下限。
    @Test
    void evaluateRejectsDesignatedFinalApproverBelowThePolicyMinimum() {
        reviewRepository.superiors.put("usr_init", "usr_low");
        reviewRepository.superiors.put("usr_low", "usr_high");
        TestSecurityActors.asSupervisor("usr_init", 3);
        assertThatThrownBy(() -> service.evaluate(new SupervisorReviewEvaluateRequest("review-rank-reject", "v", List.of(),
                "contract_violation", "contract", "c_rank", 0L, "violation", "usr_low", List.of())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FINAL_APPROVER_BELOW_POLICY_MINIMUM"));
        assertThatThrownBy(() -> service.evaluate(new SupervisorReviewEvaluateRequest("review-rank-stranger", "v", List.of(),
                "contract_violation", "contract", "c_rank_s", 0L, "violation", "usr_stranger", List.of())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FINAL_APPROVER_NOT_IN_CHAIN"));
    }

    // M087-A-02（PGM-1.7.1「发起单据时只需指定该事项的最终审批人，系统按组织层级自动补全……全部直接上级节点」）。
    @Test
    void evaluateWithDesignatedFinalApproverBuildsTheChainThroughDirectSuperiors() {
        reviewRepository.superiors.put("usr_init", "usr_mid");
        reviewRepository.superiors.put("usr_mid", "usr_high");
        reviewRepository.superiors.put("usr_high", "usr_ceo");
        TestSecurityActors.asSupervisor("usr_init", 3);
        var pending = service.evaluate(new SupervisorReviewEvaluateRequest("review-rank-allow", "v", List.of(),
                "contract_violation", "contract", "c_rank2", 0L, "violation", "usr_high", List.of()));
        assertThat(reviewRepository.listChainNodes(pending.reviewId())).extracting(ApprovalChainNode::approverId)
                .containsExactly("usr_mid", "usr_high"); // 起草人指定 usr_high 为最终审批人：usr_ceo 不进链

        TestSecurityActors.asSupervisor("usr_mid", 3);
        service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("review-rank-allow-1", "approve", List.of(), 3, "approved", "mid"));
        TestSecurityActors.asSupervisor("usr_high", 3);
        var approved = service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("review-rank-allow-2", "approve", List.of(), 3, "approved", "high"));
        assertThat(approved.reviewStatus()).isEqualTo("approved");
    }

    // ── 双轨收口（platform-governance §24.3）：无静默降级 / specific_role 角色解析 / 同商户范围闸 ──

    // §24.3-A 无职级配置不被静默降级：保守 rank 下限仍留痕；M087：无组织层级 → 链未生成，任何人（含高职级）都不能裁决（fail-closed）。
    @Test
    void evaluateWithoutOrgHierarchyLeavesTheChainUnresolvedAndNobodyCanApprove() {
        com.complex.platform.access.AccessRepository repo =
                levelAwareRepository(Map.of("usr_no_level", 0, "usr_reviewer_no_level", 9), null);
        SupervisorReviewService svc = new SupervisorReviewService(
                new AuthorizationService(repo),
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                reviewRepository,
                new AuditEventService(auditEventRepository), publishedEvents::add);

        TestSecurityActors.asSupervisor("usr_no_level", 3);
        var pending = svc.evaluate(evaluate("review-conservative", "v", List.of(),
                "contract_violation", "contract", "c_cons", 0L, "violation"));
        // two_levels_up + requiredLevel 3 → conservative floor(max(3,1)*3=9)+2*3 = 15（非 null，未静默降级）。
        assertThat(pending.requiredApproverRank()).isEqualTo(15);
        assertThat(reviewRepository.listChainNodes(pending.reviewId())).isEmpty();
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).contains("governance.supervisor_review.chain_unresolved");

        TestSecurityActors.asSupervisor("usr_reviewer_no_level", 3);
        assertThatThrownBy(() -> svc.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-conservative-approve", "approve", List.of(), 3, "approved", "no rank")))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("APPROVAL_CHAIN_UNRESOLVED"));
    }

    // §24.3-B specific_role 按角色解析而非旁路：审批人未经角色持有指定审批权限 → 被拒；经角色持有 → 放行。
    @Test
    void approveSpecificRoleResolvesByRoleInsteadOfBypass() {
        String approvePerm = "governance.supervisor_review.approve";
        // 授权审批人 usr_role_ok 经 ROLE 来源持有审批权限（rank 6 → 主管级 2）；usr_role_missing 不持有。
        com.complex.platform.access.AccessRepository repo = levelAwareRepository(
                Map.of("usr_init_sr", 6, "usr_role_ok", 6, "usr_role_missing", 6),
                java.util.Set.of("usr_init_sr", "usr_role_ok"));
        assertThat(approvePerm).isEqualTo("governance.supervisor_review.approve");
        SupervisorReviewService svc = new SupervisorReviewService(
                new AuthorizationService(repo),
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                reviewRepository,
                new AuditEventService(auditEventRepository), publishedEvents::add);

        reviewRepository.superiors.put("usr_init_sr", "usr_sr_sup"); // R-003：角色审批事项也走组织链
        TestSecurityActors.asSupervisor("usr_init_sr", 2);
        var pending = svc.evaluate(evaluate("review-sr", null, List.of("att"), "refund", "ticket", "r1", 100L, null, "usr_payee"));
        assertThat(pending.requiredApproverRank()).isNull(); // specific_role 不按 rank
        assertThat(reviewRepository.listChainNodes(pending.reviewId())).extracting(ApprovalChainNode::approverKind).containsExactly("user", "role");
        TestSecurityActors.asSupervisor("usr_sr_sup", 2);
        svc.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("review-sr-sup", "approve", List.of("att"), 2, "approved", "superior")); // 直接上级节点

        // 无角色审批人被拒：现口径下主管级本身由「经角色持有审批权限」推导，不持有即级别 0，在级别闸被拒。
        TestSecurityActors.asSupervisor("usr_role_missing", 2);
        assertThatThrownBy(() -> svc.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-sr-deny", "approve", List.of("att"), 2, "approved", "no role")))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("SUPERVISOR_LEVEL_TOO_LOW"));

        // 经角色持有审批权限的审批人放行。
        TestSecurityActors.asSupervisor("usr_role_ok", 2);
        var approved = svc.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-sr-allow", "approve", List.of("att"), 2, "approved", "role ok"));
        assertThat(approved.reviewStatus()).isEqualTo("approved");
    }

    // R-003「退款等角色审批也走组织链」（PGM-1.7.1「只需指定该事项的最终审批人，系统按组织层级自动补全全部直接上级节点」）：
    // 未指定最终审批人：直接上级 → 链尾角色节点；指定最终审批人：补全到该人、该人须经角色持有审批权限（发起即 fail-closed）；无组织层级则链不生成。
    @Test
    void specificRolePoliciesFollowTheOrgChainBeforeTheRoleGate() {
        com.complex.platform.access.AccessRepository repo = levelAwareRepository(
                Map.of("usr_rf_init", 6, "usr_rf_sup", 9, "usr_rf_boss", 12, "usr_rf_norole", 12),
                java.util.Set.of("usr_rf_init", "usr_rf_sup", "usr_rf_boss"));
        SupervisorReviewService svc = new SupervisorReviewService(
                new AuthorizationService(repo),
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                reviewRepository,
                new AuditEventService(auditEventRepository), publishedEvents::add);
        reviewRepository.superiors.put("usr_rf_init", "usr_rf_sup");
        reviewRepository.superiors.put("usr_rf_sup", "usr_rf_boss");
        reviewRepository.superiors.put("usr_rf_boss", "usr_rf_norole");

        TestSecurityActors.asSupervisor("usr_rf_init", 2);
        var undesignated = svc.evaluate(evaluate("review-rf-1", null, List.of("att"), "refund", "ticket", "rf_1", 100L, null, "usr_payee"));
        assertThat(reviewRepository.listChainNodes(undesignated.reviewId()))
                .extracting(ApprovalChainNode::approverKind, ApprovalChainNode::approverId)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("user", "usr_rf_sup"), org.assertj.core.groups.Tuple.tuple("user", "usr_rf_boss"),
                        org.assertj.core.groups.Tuple.tuple("role", null)); // I7-01：refund_l2 → 两级直接上级（策略级别作下限）+ 链尾角色节点
        TestSecurityActors.asSupervisor("usr_rf_boss", 3);
        assertThatThrownBy(() -> svc.approve(undesignated.reviewId(), new SupervisorReviewApprovalRequest(
                "review-rf-1-early", "n", List.of("att"), 3, "approved", "skip superior")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("APPROVAL_NODE_NOT_REACHED")); // 不得跳过直接上级
        TestSecurityActors.asSupervisor("usr_rf_sup", 3);
        assertThat(svc.approve(undesignated.reviewId(), new SupervisorReviewApprovalRequest(
                "review-rf-1-sup", "n", List.of("att"), 3, "approved", "sup")).reviewStatus()).isEqualTo("pending_review");
        TestSecurityActors.asSupervisor("usr_rf_boss", 3);
        assertThat(svc.approve(undesignated.reviewId(), new SupervisorReviewApprovalRequest(
                "review-rf-1-boss", "n", List.of("att"), 3, "approved", "boss")).reviewStatus()).isEqualTo("pending_review");
        assertThat(svc.approve(undesignated.reviewId(), new SupervisorReviewApprovalRequest(
                "review-rf-1-role", "n", List.of("att"), 3, "approved", "role")).reviewStatus()).isEqualTo("approved");

        TestSecurityActors.asSupervisor("usr_rf_init", 2);
        var designated = svc.evaluate(new SupervisorReviewEvaluateRequest("review-rf-2", null, List.of("att"), "refund", "ticket", "rf_2", 100L, null, "usr_rf_boss", List.of(), "usr_payee"));
        assertThat(reviewRepository.listChainNodes(designated.reviewId())).extracting(ApprovalChainNode::approverId)
                .containsExactly("usr_rf_sup", "usr_rf_boss"); // 指定最终审批人：补全到该人，无独立角色节点
        assertThatThrownBy(() -> svc.evaluate(new SupervisorReviewEvaluateRequest("review-rf-3", null, List.of("att"), "refund", "ticket", "rf_3", 100L, null, "usr_rf_norole", List.of(), "usr_payee")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FINAL_APPROVER_LACKS_ROLE"));

        reviewRepository.superiors.clear();
        TestSecurityActors.asSupervisor("usr_rf_orphan", 2);
        var orphan = svc.evaluate(evaluate("review-rf-4", null, List.of("att"), "refund", "ticket", "rf_4", 100L, null, "usr_payee"));
        assertThat(reviewRepository.listChainNodes(orphan.reviewId())).isEmpty(); // 无组织层级：链不生成（fail-closed）
    }

    // M007-R6-02（PGM-1.10.1「所有业务数据默认按项目隔离……不提供关闭开关」、PGM-1.7.1「④看不到别的商户和别的项目的数据」）：
    // 待复核清单按当前操作人范围过滤——普通主管只见发起人属本项目的单据或自己是当前节点审批人的单据；平台管理员跨范围查看留痕。
    @Test
    void pendingListIsScopedToTheActorsProjectUnlessCurrentApproverOrPlatformAdmin() {
        com.complex.platform.access.AccessRepository repo = levelAwareRepository(Map.of("usr_init_a", 3, "usr_init_b", 3), null);
        org.mockito.Mockito.when(repo.findUserById("usr_init_a")).thenReturn(Optional.of(new com.complex.platform.access.AccessUser(
                "usr_init_a", "a", "staff", "org_1", null, "active", java.time.Instant.parse("2026-06-14T00:00:00Z"))));
        org.mockito.Mockito.when(repo.findUserById("usr_init_b")).thenReturn(Optional.of(new com.complex.platform.access.AccessUser(
                "usr_init_b", "b", "staff", "org_2", null, "active", java.time.Instant.parse("2026-06-14T00:00:00Z"))));
        SupervisorReviewService svc = new SupervisorReviewService(
                new AuthorizationService(repo),
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                reviewRepository,
                new AuditEventService(auditEventRepository), publishedEvents::add);
        reviewRepository.superiors.put("usr_init_a", "usr_sup_a");
        reviewRepository.superiors.put("usr_init_b", "usr_sup_b");
        TestSecurityActors.asSupervisor("usr_init_a", 1);
        var a = svc.evaluate(evaluate("scope-a", "v", List.of(), "contract_violation", "contract", "c_scope_a", 0L, "violation"));
        TestSecurityActors.asSupervisor("usr_init_b", 1);
        var b = svc.evaluate(evaluate("scope-b", "v", List.of(), "contract_violation", "contract", "c_scope_b", 0L, "violation"));

        TestSecurityActors.asSupervisor("usr_viewer_org1", 1); // org_1 的普通主管：只见本项目发起的单据
        assertThat(svc.listPending(200).items()).extracting(p -> p.decision().reviewId()).containsExactly(a.reviewId());
        TestSecurityActors.asSupervisor("usr_sup_b", 1); // org_1 账号，但是 b 单当前节点审批人：可见
        assertThat(svc.listPending(200).items()).extracting(p -> p.decision().reviewId()).containsExactlyInAnyOrder(a.reviewId(), b.reviewId());
        TestSecurityActors.asPlatformAdmin("usr_admin_all");
        assertThat(svc.listPending(200).items()).extracting(p -> p.decision().reviewId()).containsExactly(a.reviewId());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org_1", null, "active"),
                        "token", List.of()));
        assertThat(svc.listPending(200).items()).extracting(p -> p.decision().reviewId()).containsExactlyInAnyOrder(a.reviewId(), b.reviewId());
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).contains("governance.supervisor_review.pending_listed_cross_scope");
    }

    // M007-R6-01（PGM-1.10.1「税费规则与税费调整须经财务审核、税务审核、法务审核，再由老板最终确认后生效」）：策略固定要求平行线的事项
    // 不能经无法指定平行线的准入阻断路径发起——与 evaluate 同一 fail-closed 口径。
    @Test
    void admissionBlockRefusesPoliciesThatRequireParallelLines() {
        TestSecurityActors.asSupervisor("usr_tax_init", 3);
        assertThatThrownBy(() -> service.blockForAdmission("tax_rule_adjustment_l3", "finance.tax_rule.adjust", "tax_rule", "t_block", "top_authority", 3, null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("APPROVAL_LINE_FINAL_APPROVER_REQUIRED"));
    }

    /**
     * M007-A-04：现口径下主管级 = 经角色持有 governance.supervisor_review.approve + 职级 rank 折算（(rank+2)/3）。
     * 夹具用 mock 仓储：ranks 里显式给出的用户按其 rank（0 = 无职级配置），其余用户按当前安全上下文主体的账号类型
     * supervisor_lN 折算 rank 3N；roleHolders 为 null 时全部用户经 ROLE 来源持有审批权限，否则只有集合内的用户持有。
     */
    private static com.complex.platform.access.AccessRepository levelAwareRepository(
            Map<String, Integer> ranks, java.util.Set<String> roleHolders) {
        com.complex.platform.access.AccessRepository repo =
                org.mockito.Mockito.mock(com.complex.platform.access.AccessRepository.class);
        org.mockito.Mockito.when(repo.findPermissionEffects(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    String userId = invocation.getArgument(0);
                    if (roleHolders != null && !roleHolders.contains(userId)) {
                        return List.of();
                    }
                    return List.of(new com.complex.platform.access.AccessRepository.PermissionEffect(
                            userId, "governance.supervisor_review.approve",
                            com.complex.platform.access.AccessRepository.Effect.GRANT,
                            com.complex.platform.access.AccessRepository.Source.ROLE,
                            "role_supervisor", null, null, false, false));
                });
        org.mockito.Mockito.when(repo.findStaffLevelAssignment(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    String userId = invocation.getArgument(0);
                    int rank = ranks.containsKey(userId) ? ranks.get(userId) : rankFromCurrentActor(userId);
                    if (rank <= 0) {
                        return Optional.empty();
                    }
                    return Optional.of(new com.complex.platform.access.StaffLevelAssignment(
                            userId, "lvl_rank_" + rank, null, "CNY", null, null, null, "x",
                            java.time.Instant.parse("2026-06-14T00:00:00Z")));
                });
        org.mockito.Mockito.when(repo.findStaffLevelById(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    String levelId = invocation.getArgument(0);
                    int rank = Integer.parseInt(levelId.substring("lvl_rank_".length()));
                    return Optional.of(new com.complex.platform.access.StaffLevelRecord(
                            levelId, "M", "lower", rank, null, "active", java.time.Instant.parse("2026-06-14T00:00:00Z")));
                });
        return repo;
    }


    // ── U12-003（U09-004 测试面）：第 9 段新增的员工域策略必须有可失败用例 ──────────────────────────

    /** 处罚上限调整固定三条平行线（hr/finance/legal），发起时缺任一线最终审批人即拒（fail-closed）。 */
    @Test
    void penaltyCapAdjustRequiresFinalApproverOnEachDepartmentLine() {
        chainL1();
        TestSecurityActors.asSupervisorLevel(1);
        var missingHr = new SupervisorReviewEvaluateRequest("u12-cap-missing", "n", List.of(), "staff.payroll.penalty_cap.adjust",
                "staff_policy", "cap_u12_1", 0L, null, null,
                List.of(new SupervisorReviewEvaluateRequest.ParallelLine("finance_review", "usr_fin_final"),
                        new SupervisorReviewEvaluateRequest.ParallelLine("legal_review", "usr_legal_final")));
        assertThatThrownBy(() -> service.evaluate(missingHr))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("APPROVAL_LINE_FINAL_APPROVER_REQUIRED"));
    }

    /** 三审未齐时顶层（master）先批被拒 DEPARTMENT_REVIEWS_PENDING；三线齐后 master 确认，整单终态经注入的发布器发出。 */
    @Test
    void penaltyCapAdjustNeedsDepartmentReviewsBeforeMasterAndPublishesTerminalDecision() {
        chainL1();
        for (String code : List.of("hr_review", "finance_review", "legal_review")) {
            // 与 V418 种子一致：hr_review 线无起点人，线上唯一节点＝发起时指定的最终审批人。
            reviewRepository.lines.put(code, new SupervisorReviewRepository.ApprovalLine(code, code, 1, null, "active"));
        }
        TestSecurityActors.asSupervisorLevel(1);
        var lines = List.of(new SupervisorReviewEvaluateRequest.ParallelLine("hr_review", "usr_hr_review_final"),
                new SupervisorReviewEvaluateRequest.ParallelLine("finance_review", "usr_finance_review_final"),
                new SupervisorReviewEvaluateRequest.ParallelLine("legal_review", "usr_legal_review_final"));
        var pending = service.evaluate(new SupervisorReviewEvaluateRequest("u12-cap", "n", List.of(), "staff.payroll.penalty_cap.adjust",
                "staff_policy", "cap_u12_2", 0L, null, null, lines));
        assertThat(pending.reviewStatus()).isEqualTo("pending_review");
        var nodes = reviewRepository.listChainNodes(pending.reviewId());
        assertThat(nodes).extracting(ApprovalChainNode::lineCode).contains("hr_review", "finance_review", "legal_review", ApprovalChainNode.MAIN_LINE);
        var master = nodes.stream().filter(n -> ApprovalChainNode.KIND_MASTER.equals(n.approverKind())).findFirst().orElseThrow();
        assertThat(master.lineCode()).isEqualTo(ApprovalChainNode.MAIN_LINE);
        // 主线上的直接上级节点先逐级通过，master 成为当前节点后仍须等三条部门线（DEPARTMENT_REVIEWS_PENDING）。
        for (var node : nodes.stream().filter(n -> ApprovalChainNode.MAIN_LINE.equals(n.lineCode()) && !ApprovalChainNode.KIND_MASTER.equals(n.approverKind())).toList()) {
            TestSecurityActors.asSupervisor(node.approverId(), 10);
            service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("u12-cap-main-" + node.seq(), "n", List.of("att"), null, "approved", "主线逐级"));
        }
        publishedEvents.clear();
        asMasterAuthority();
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("u12-cap-master-early", "n", List.of("att"), null, "approved", "确认")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEPARTMENT_REVIEWS_PENDING"));
        assertThat(publishedEvents).isEmpty();
        for (var line : lines) {
            TestSecurityActors.asSupervisor(line.finalApproverId(), 3);
            service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("u12-cap-" + line.lineCode(), "n", List.of("att"), null, "approved", "部门审核"));
        }
        asMasterAuthority();
        var done = service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("u12-cap-master", "n", List.of("att"), null, "approved", "确认"));
        assertThat(done.reviewStatus()).isEqualTo("approved");
        assertThat(publishedEvents).extracting(e -> ((SupervisorReviewDecisionRecord) e).reviewId()).contains(pending.reviewId());
    }

    /** 排班发布走 one_level_up：直接上级是唯一审批节点，发起人不能自批，上级批准即 approved 并发布终态事件。 */
    @Test
    void shiftPublishFollowsOneLevelUpChainAndPublishesApproval() {
        chainL1();
        TestSecurityActors.asSupervisorLevel(1);
        var pending = service.evaluate(evaluate("u12-shift", "排班", List.of(), "staff.shift.publish", "staff_shift", "shift_u12", 0L, null));
        assertThat(pending.reviewStatus()).isEqualTo("pending_review");
        var nodes = reviewRepository.listChainNodes(pending.reviewId());
        assertThat(nodes).hasSize(1);
        assertThat(nodes.getFirst().approverId()).isEqualTo("usr_reviewer_a");
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("u12-shift-self", "n", List.of(), null, "approved", "自批")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        publishedEvents.clear();
        TestSecurityActors.asSupervisor("usr_reviewer_a", 3);
        var approved = service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("u12-shift-sup", "n", List.of(), null, "approved", "上一级批准"));
        assertThat(approved.reviewStatus()).isEqualTo("approved");
        assertThat(publishedEvents).extracting(e -> ((SupervisorReviewDecisionRecord) e).reviewId()).containsExactly(pending.reviewId());
    }


    /** 顶层平台最高权限主会话（USB master）：id 与 username 同时匹配 SessionIssuer 常量才被识别为 master。 */
    private static void asMasterAuthority() {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", null, null, "active"),
                        "test", List.of()));
    }

    /** 共享 service 用到的审批人夹具 rank：a → 主管级 3、b → 主管级 2（用例断言从上下文取的级别）；其余为顶级 30。 */
    private static final Map<String, Integer> SHARED_RANKS = Map.of(
            "usr_reviewer_a", 9, "usr_reviewer_b", 6, "usr_reviewer_c", 30, "usr_reviewer_idem", 30, "usr_reviewer_decided", 30);

    /** 常用组织层级：L1 主管 → usr_reviewer_a → usr_reviewer_c（组织顶端）。 */
    private void chainL1() {
        reviewRepository.superiors.put("usr_supervisor_l1", "usr_reviewer_a");
        reviewRepository.superiors.put("usr_reviewer_a", "usr_reviewer_c");
    }

    private static SupervisorReviewEvaluateRequest evaluate(String key, String note, List<String> attachments, String actionType,
                                                            String objectType, String objectId, Long amountCent, String violation) {
        return evaluate(key, note, attachments, actionType, objectType, objectId, amountCent, violation, null);
    }

    private static SupervisorReviewEvaluateRequest evaluate(String key, String note, List<String> attachments, String actionType,
                                                            String objectType, String objectId, Long amountCent, String violation,
                                                            String beneficiaryId) {
        return new SupervisorReviewEvaluateRequest(key, note, attachments, actionType, objectType, objectId, amountCent, violation,
                null, List.of(), beneficiaryId);
    }

    private static int rankFromCurrentActor(String userId) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.complex.platform.access.AccessActor actor
                && userId.equals(actor.id())) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("supervisor_l(\\d+)").matcher(actor.accountType());
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1)) * 3;
            }
        }
        return 0;
    }

    // M007-F-09（PGM-1.16「谁发起、为什么、谁审批、结果是什么，都要能查到」）：策略声明的证据类型在审批时 fail-closed。
    @Test
    void approveRequiresPolicyDeclaredEvidence() {
        reviewRepository.superiors.put("usr_release_init", "usr_release_reviewer"); // R-003：角色审批事项也走组织链
        TestSecurityActors.asSupervisor("usr_release_init", 3);
        var pending = service.evaluate(evaluate("review-evidence-eval", "manual release", List.of("att_eval"),
                "manual_release", "ticket", "t_evidence", 0L, null));
        assertThat(pending.blocked()).isTrue();

        TestSecurityActors.asSupervisor("usr_release_reviewer", 3);
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-evidence-no-attachment", "note", List.of(), 3, "approved", "ok")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-evidence-no-note", null, List.of("att_approve"), 3, "approved", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-evidence-ok", "note", List.of("att_approve"), 3, "approved", "evidence attached"));
        var approved = service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-evidence-ok-role", "note", List.of("att_approve"), 3, "approved", "role node")); // R-003：链尾角色节点
        assertThat(approved.reviewStatus()).isEqualTo("approved");
    }

    // M007-A-01（PGM-1.2 第 5 步「放行结果都要记录」）：批准后同动作同对象同发起人可消费一次并留痕 admission.release_executed，再次消费为空。
    @Test
    void approvedReviewIsConsumedForReleaseExactlyOnce() {
        reviewRepository.superiors.put("usr_waiver_init", "usr_reviewer_c"); // R-003：角色审批事项也走组织链
        TestSecurityActors.asSupervisor("usr_waiver_init", 1);
        var pending = service.evaluate(evaluate("review-release-eval", "waiver", List.of("att"), "bill_waiver", "bill", "bill_release", 0L, null, "usr_payee"));
        assertThat(service.consumeApprovedRelease("bill_waiver", "bill", "bill_release", "usr_waiver_init")).isEmpty();

        TestSecurityActors.asSupervisor("usr_reviewer_c", 3);
        service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-release-approve", "note", List.of("att"), 3, "approved", "ok"));
        service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-release-approve-role", "note", List.of("att"), 3, "approved", "role node")); // R-003：链尾角色节点

        assertThat(service.consumeApprovedRelease("bill_waiver", "bill", "bill_release", "usr_waiver_init"))
                .map(SupervisorReviewDecisionRecord::reviewId).contains(pending.reviewId());
        assertThat(service.consumeApprovedRelease("bill_waiver", "bill", "bill_release", "usr_waiver_init")).isEmpty();
        assertThat(service.consumeApprovedRelease("bill_waiver", "bill", "bill_release", "usr_other")).isEmpty();
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).contains("admission.release_executed");
    }

    // M007-A-02（PGM-1.7.1「不得转发给申请人本人，也不得转发给直接受益人」「可为任意人选」）：
    // 转发人选校验；被转发人不受级别闸约束即可裁决；受益人不得自审。
    @Test
    void forwardedReviewerMayDecideButNeverInitiatorOrBeneficiary() {
        reviewRepository.superiors.put("usr_fwd_init", "usr_reviewer_c"); // R-003：角色审批事项也走组织链
        TestSecurityActors.asSupervisor("usr_fwd_init", 1);
        var pending = service.blockForAdmission("bill_waiver_l3", "bill_waiver", "bill", "bill_fwd",
                "specific_role", 3, "usr_benef");
        assertThat(reviewRepository.schedules.get(pending.reviewId()).beneficiaryId()).isEqualTo("usr_benef");

        TestSecurityActors.asSupervisor("usr_reviewer_c", 3);
        assertThatThrownBy(() -> service.forward(pending.reviewId(), new SupervisorReviewForwardRequest(
                "fwd-init", null, "usr_fwd_init", "to initiator")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SUPERVISOR_FORWARD_TARGET_FORBIDDEN"));
        assertThatThrownBy(() -> service.forward(pending.reviewId(), new SupervisorReviewForwardRequest(
                "fwd-benef", null, "usr_benef", "to beneficiary")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SUPERVISOR_FORWARD_TARGET_FORBIDDEN"));
        var forwarded = service.forward(pending.reviewId(), new SupervisorReviewForwardRequest(
                "fwd-ok", null, "usr_anyone", "please handle"));
        assertThat(forwarded.forwardedTo()).isEqualTo("usr_anyone");
        assertThat(reviewRepository.listChainNodes(pending.reviewId()).get(0).forwardedTo()).isEqualTo("usr_anyone");

        // 受益人自审被拒（即使主管级达标）。
        TestSecurityActors.asSupervisor("usr_benef", 3);
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "approve-benef", "note", List.of("att"), 3, "approved", "self")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));

        // 被转发人（无主管级）可裁决。
        TestSecurityActors.asSupervisor("usr_anyone", 0);
        var forwardedDecision = service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "approve-fwd", "note", List.of("att"), 0, "approved", "handled by forwarded reviewer"));
        assertThat(forwardedDecision.reviewStatus()).isEqualTo("pending_review"); // R-003：链尾角色节点仍待审
        TestSecurityActors.asSupervisor("usr_reviewer_c", 3);
        var approved = service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "approve-fwd-role", "note", List.of("att"), 3, "approved", "role node"));
        assertThat(approved.reviewStatus()).isEqualTo("approved");
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName)
                .contains("governance.supervisor_review.forwarded");
    }

    // M087-F-05（PGM-1.7.1「超时……自动提醒并升级到该节点的上一级，不得默认通过，也不得默认拒绝；到达最终节点仍超时保持待审」）：
    // 当前节点审批人升为其直接上级；到组织顶端后只提醒；档位不再跳档。
    // M087-I5-04（PGM-1.7.1「升级到达该单据的最终审批节点、不再有上一级可升后仍然超时的，单据保持待审」）：
    // 指定最终审批人的节点是本线终点——超时只催办，不把其组织上级（链外）升级进来。
    @Test
    void overdueFinalApprovalNodeOnlyRemindsAndNeverEscalatesPastTheDesignatedFinalApprover() {
        reviewRepository.superiors.put("usr_init", "usr_mid");
        reviewRepository.superiors.put("usr_mid", "usr_high");
        reviewRepository.superiors.put("usr_high", "usr_ceo");
        TestSecurityActors.asSupervisor("usr_init", 3);
        var pending = service.evaluate(new SupervisorReviewEvaluateRequest("review-final-overdue", "v", List.of(),
                "contract_violation", "contract", "c_final_overdue", 0L, "violation", "usr_high", List.of()));
        TestSecurityActors.asSupervisor("usr_mid", 3);
        service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("review-final-overdue-1", "approve", List.of(), 3, "approved", "mid"));

        java.time.Instant later = reviewRepository.schedules.get(pending.reviewId()).dueAt().plusSeconds(60);
        assertThat(service.escalateOverdue(later)).isEqualTo(1);
        var finalNode = reviewRepository.listChainNodes(pending.reviewId()).get(1);
        assertThat(finalNode.approverId()).isEqualTo("usr_high"); // 链外的 usr_ceo 不进链
        assertThat(finalNode.escalationCount()).isZero();
        assertThat(reviewRepository.reviews.get(pending.reviewId()).reviewStatus()).isEqualTo("pending_review");
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).contains("governance.supervisor_review.reminded");
    }

    @Test
    void overdueNodeEscalatesToTheApproversDirectSuperiorThenOnlyReminds() {
        chainL1();
        TestSecurityActors.asSupervisorLevel(1);
        var pending = service.evaluate(evaluate("review-overdue", "v", List.of(), "contract_violation", "contract", "c_overdue", 0L, "violation"));
        var schedule = reviewRepository.schedules.get(pending.reviewId());
        assertThat(schedule.dueAt()).isAfter(java.time.Instant.now());
        java.time.Instant later = schedule.dueAt().plusSeconds(60);

        assertThat(service.escalateOverdue(later)).isEqualTo(1);
        var escalated = reviewRepository.reviews.get(pending.reviewId());
        assertThat(escalated.reviewStatus()).isEqualTo("pending_review");
        assertThat(escalated.approvalLevel()).isEqualTo("two_levels_up"); // 档位留痕不变，升级发生在节点
        var node1 = reviewRepository.listChainNodes(pending.reviewId()).get(0);
        assertThat(node1.approverId()).isEqualTo("usr_reviewer_c");
        assertThat(node1.escalationCount()).isEqualTo(1);
        assertThat(reviewRepository.schedules.get(pending.reviewId()).escalationCount()).isEqualTo(1);
        assertThat(reviewRepository.schedules.get(pending.reviewId()).dueAt()).isAfter(later);

        java.time.Instant muchLater = reviewRepository.schedules.get(pending.reviewId()).dueAt().plusSeconds(60);
        assertThat(service.escalateOverdue(muchLater)).isEqualTo(1);
        assertThat(reviewRepository.reviews.get(pending.reviewId()).reviewStatus()).isEqualTo("pending_review");
        assertThat(reviewRepository.listChainNodes(pending.reviewId()).get(0).approverId()).isEqualTo("usr_reviewer_c"); // 顶端：只提醒
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName)
                .contains("governance.supervisor_review.escalated", "governance.supervisor_review.reminded");
    }

    // M007-I4-F01 口径沿用（PGM-1.7.1「明确要求由最高权限审批的事项，转发由最高权限本人逐单作出」）：节点升级到上级时清除
    // 该节点上低层级作出的转发；被转发人不再能裁决，升级后的审批人逐节点裁决。
    @Test
    void forwardMadeOnANodeDoesNotSurviveThatNodesEscalation() {
        reviewRepository.superiors.put("usr_esc_init", "usr_reviewer_a");
        reviewRepository.superiors.put("usr_reviewer_a", "usr_reviewer_c");
        TestSecurityActors.asSupervisor("usr_esc_init", 1);
        var pending = service.blockForAdmission("permission_grant_l2", "access.grant_request.money", "access_grant_request",
                "agr_esc", "two_levels_up", 2, "usr_esc_benef");
        TestSecurityActors.asSupervisor("usr_reviewer_a", 3);
        assertThat(service.forward(pending.reviewId(), new SupervisorReviewForwardRequest(
                "esc-forward-below-top", null, "usr_anyone", "handle")).forwardedTo()).isEqualTo("usr_anyone");

        java.time.Instant later = reviewRepository.schedules.get(pending.reviewId()).dueAt().plusSeconds(60);
        assertThat(service.escalateOverdue(later)).isEqualTo(1);
        assertThat(reviewRepository.listChainNodes(pending.reviewId()).get(0).forwardedTo()).isNull();
        assertThat(reviewRepository.schedules.get(pending.reviewId()).forwardedTo()).isNull();

        TestSecurityActors.asSupervisor("usr_anyone", 0);
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "esc-approve-stale-forwardee", "note", List.of("att"), 0, "approved", "stale forward")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SUPERVISOR_NOT_CURRENT_APPROVER"));
        assertThat(reviewRepository.reviews.get(pending.reviewId()).reviewStatus()).isEqualTo("pending_review");

        TestSecurityActors.asSupervisor("usr_reviewer_c", 3);
        service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("esc-approve-node1", "note", List.of("att"), 3, "approved", "ok"));
        assertThat(service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("esc-approve-node2", "note", List.of("att"), 3, "approved", "ok"))
                .reviewStatus()).isEqualTo("pending_review");
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org_1", null, "active"),
                        "token", List.of()));
        assertThat(service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("esc-approve-master", "note", List.of("att"), 3, "approved", "ok"))
                .reviewStatus()).isEqualTo("approved");
    }

    // M007-I3-F01（PGM-1.7.1「明确要求由最高权限审批的事项，转发由最高权限本人逐单作出」「被转发人同时获得审批权限」）：
    // 主会话节点只能由 USB 主会话裁决；主会话转发后被转发人可裁决，申请人/受益人仍不得。
    @Test
    void topAuthorityReviewForwardedByMasterCanBeDecidedByForwardee() {
        TestSecurityActors.asSupervisor("usr_top_init", 1);
        var pending = service.blockForAdmission("base_definition_violation_l3", "base_definition_violation", "definition",
                "d_top", "top_authority", 3, "usr_top_benef");
        assertThat(reviewRepository.listChainNodes(pending.reviewId())).extracting(ApprovalChainNode::approverKind).containsExactly("master");

        TestSecurityActors.asSupervisor("usr_reviewer_c", 3);
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "top-approve-non-master", "note", List.of("att"), 3, "approved", "not master")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MASTER_AUTHORITY_REQUIRED"));
        assertThatThrownBy(() -> service.forward(pending.reviewId(), new SupervisorReviewForwardRequest(
                "top-forward-non-master", null, "usr_anyone", "x")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MASTER_AUTHORITY_REQUIRED"));

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org_1", null, "active"),
                        "token", List.of()));
        assertThat(service.forward(pending.reviewId(), new SupervisorReviewForwardRequest(
                "top-forward-master", null, "usr_anyone", "handle")).forwardedTo()).isEqualTo("usr_anyone");

        TestSecurityActors.asSupervisor("usr_anyone", 0);
        var approved = service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "top-approve-forwardee", "note", List.of("att"), 0, "approved", "handled"));
        assertThat(approved.reviewStatus()).isEqualTo("approved");
    }

    // M087-A-02 + M007-I3-F01：最高权限事项的主线 = 发起人直接上级链 + 主会话末节点；上级全部通过后主会话才裁决。
    @Test
    void topAuthorityChainRunsThroughDirectSuperiorsBeforeTheMasterNode() {
        chainL1();
        TestSecurityActors.asSupervisorLevel(1);
        var pending = service.evaluate(evaluate("review-top-chain", "v", List.of(), "base_definition_violation", "definition", "d_chain", 0L, "violation"));
        assertThat(reviewRepository.listChainNodes(pending.reviewId())).extracting(ApprovalChainNode::approverKind)
                .containsExactly("user", "user", "master");
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org_1", null, "active"),
                        "token", List.of()));
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "top-chain-master-early", "note", List.of("att"), 0, "approved", "too early")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SUPERVISOR_NOT_CURRENT_APPROVER"));
    }

    // M007-I3-F02（PGM-1.7.1 涉钱审批出厂默认 1 个工作日，本条适用于临时权限申请与高危授权）：授权申请按涉钱标记排期。
    @Test
    void moneyGrantApplicationsGetTheShorterDeadline() {
        TestSecurityActors.asSupervisorLevel(1);
        var money = service.blockForAdmission("permission_grant_l2", "access.grant_request.money", "access_grant_request",
                "agr_money", "two_levels_up", 2, "usr_m");
        var normal = service.blockForAdmission("permission_grant_l2", "access.grant_request", "access_grant_request",
                "agr_normal", "two_levels_up", 2, "usr_n");
        assertThat(reviewRepository.schedules.get(normal.reviewId()).dueAt())
                .isAfter(reviewRepository.schedules.get(money.reviewId()).dueAt());
    }

    // M007-F-11（PGM-1.16「为什么、谁审批、结果是什么，都要能查到」）：审批不得覆盖评估时的违规说明。
    @Test
    void approvalKeepsEvaluationReasonAndRecordsComment() {
        reviewRepository.superiors.put("usr_supervisor_l1", "usr_reviewer_c");
        TestSecurityActors.asSupervisorLevel(1);
        var pending = service.evaluate(evaluate("review-reason", "v", List.of(), "contract_violation", "contract", "c_reason", 0L, "合同主体与经营主体不一致"));
        TestSecurityActors.asSupervisor("usr_reviewer_c", 3);
        var approved = service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-reason-approve", "note", List.of("att"), 3, "approved", "证据齐备，同意"));
        assertThat(approved.reason()).isEqualTo("合同主体与经营主体不一致");
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).contains("governance.supervisor_review.decided");
    }

    // M007-A-02（PGM-1.7.1 涉钱审批出厂默认 1 个工作日、其余 3 个工作日）：到期按工作日计算。
    @Test
    void deadlineIsShorterForMoneyActions() {
        TestSecurityActors.asSupervisorLevel(1);
        var money = service.blockForAdmission("bar_bill_waiver_block", "bar.bill_waiver", "bar_bill", "b_money",
                "one_level_up", 2, "usr_bar_payee");
        var normal = service.blockForAdmission("amusement_reopen_block", "amusement.reopen", "amusement", "a_normal",
                "two_levels_up", 3, null);
        var moneyDue = reviewRepository.schedules.get(money.reviewId()).dueAt();
        var normalDue = reviewRepository.schedules.get(normal.reviewId()).dueAt();
        assertThat(moneyDue).isAfter(java.time.Instant.now());
        assertThat(normalDue).isAfter(moneyDue);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org_1", null, "active"),
                        "token", List.of())); // 红线④：只有密钥主体可整批看待审
        assertThat(service.listPending(10).items()).extracting(item -> item.decision().reviewId())
                .contains(money.reviewId(), normal.reviewId());
    }

    // P1-1 red→green: 新服务实例（模拟重启）能从持久化存储中读取已提交的待审记录
    @Test
    void pendingReviewSurvivesServiceRestart() {
        reviewRepository.superiors.put("usr_supervisor_l3", "usr_reviewer_c");
        TestSecurityActors.asSupervisorLevel(3);
        var decision = service.evaluate(evaluate("review-persist-1", "contract violation", List.of(),
                "contract_violation", "contract", "contract_persist", 0L, "violation"));

        // 创建新 service 实例（共享同一 reviewRepository，模拟重启后仍能读取）
        SupervisorReviewService newService = new SupervisorReviewService(
                new AuthorizationService(levelAwareRepository(SHARED_RANKS, null)),
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                reviewRepository,  // 共享同一持久化存储
                new AuditEventService(auditEventRepository), publishedEvents::add
        );

        TestSecurityActors.asSupervisor("usr_reviewer_c", 3);
        var approved = newService.approve(decision.reviewId(), new SupervisorReviewApprovalRequest(
                "review-persist-approve-1", "approve", List.of(), 3, "approved", "approved after restart"));
        assertThat(approved.reviewStatus()).isEqualTo("approved");
    }

    // P1-2 red→green: 相同幂等键的 evaluate 只持久化一次
    @Test
    void evaluateWithSameIdempotencyKeyReturnsSameReviewAndPersistsOnce() {
        TestSecurityActors.asSupervisorLevel(1);
        var request = evaluate("review-idem-1", "contract violation", List.of(), "contract_violation", "contract", "contract_idem", 0L, "violation");
        var first = service.evaluate(request);
        var second = service.evaluate(request);

        assertThat(second.reviewId()).isEqualTo(first.reviewId());
        assertThat(reviewRepository.saveCount).isEqualTo(1); // 只保存一次
    }

    // ── R6 §1.3 督导复核补强：命中/未命中两分支 + 字段断言 ──────────────────

    @Test
    // M007-F-07（PGM-1.16.2 常见复核：免单、退款、调账不设金额豁免）：金额 0、无违规说明的免单也必须进复核。
    void evaluateBlocksBillWaiverEvenAtZeroAmount() {
        TestSecurityActors.asSupervisorLevel(1);
        var decision = service.evaluate(evaluate("review-autopass-1", null, List.of(), "bill_waiver", "bill", "bill_autopass", 0L, null, "usr_payee"));

        assertThat(decision.blocked()).isTrue();
        assertThat(decision.reviewStatus()).isEqualTo("pending_review");
        assertThat(decision.decision()).isEqualTo("blocked");
        assertThat(decision.policyCode()).isEqualTo("bill_waiver_l3");
        assertThat(decision.actionType()).isEqualTo("bill_waiver");
        assertThat(decision.objectType()).isEqualTo("bill");
        assertThat(decision.objectId()).isEqualTo("bill_autopass");
        assertThat(decision.reviewId()).startsWith("review_");
        assertThat(decision.evaluatedAt()).isNotNull();
        // 常见复核恒落待审记录
        assertThat(reviewRepository.saveCount).isEqualTo(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("governance.supervisor_review.evaluated");
    }

    @Test
    void evaluateReturnsReviewRequiredWhenPolicyMatches() {
        TestSecurityActors.asSupervisorLevel(1);
        var decision = service.evaluate(evaluate("review-match-1", null, List.of("att_match"), "bill_waiver", "bill", "bill_match", 500L, null, "usr_payee"));

        assertThat(decision.blocked()).isTrue();
        assertThat(decision.reviewStatus()).isEqualTo("pending_review");
        assertThat(decision.decision()).isEqualTo("blocked");
        assertThat(decision.policyCode()).isEqualTo("bill_waiver_l3");
        assertThat(decision.requiredLevel()).isEqualTo(3);
        assertThat(decision.actionType()).isEqualTo("bill_waiver");
        assertThat(decision.objectType()).isEqualTo("bill");
        assertThat(decision.objectId()).isEqualTo("bill_match");
        assertThat(decision.evidenceAttachmentIds()).containsExactly("att_match");
        assertThat(decision.approvalLevel()).isEqualTo("specific_role");
        // 命中落待审记录
        assertThat(reviewRepository.saveCount).isEqualTo(1);
    }

    // ── R6 §1.3 督导复核补强：approve 幂等 / 重复审批拒绝 / 越权 ───────────────

    // §1.3-2 approve 幂等：相同幂等键的第二次审批返回首次结果，且只持久化一次决策。
    @Test
    void approveIsIdempotentForSameIdempotencyKey() {
        reviewRepository.superiors.put("usr_supervisor_l3", "usr_reviewer_idem");
        TestSecurityActors.asSupervisorLevel(3);
        var pending = service.evaluate(evaluate("review-idem-approve-eval", "contract violation", List.of(),
                "contract_violation", "contract", "contract_idem_approve", 0L, "violation"));
        int saveCountAfterEvaluate = reviewRepository.saveCount;

        TestSecurityActors.asSupervisor("usr_reviewer_idem", 3);
        var approval = new SupervisorReviewApprovalRequest("approve-idem-key", "approve", List.of(), 3, "approved", "first decision");
        var first = service.approve(pending.reviewId(), approval);
        var second = service.approve(pending.reviewId(), approval);

        assertThat(second.reviewId()).isEqualTo(first.reviewId());
        assertThat(second.reviewStatus()).isEqualTo("approved");
        assertThat(second.decision()).isEqualTo("approved");
        // 幂等命中：只有第一次真正写入决策（evaluate 落待审 + 第一次 approve 落决策 = +1）
        assertThat(reviewRepository.saveCount).isEqualTo(saveCountAfterEvaluate + 1);
    }

    // §1.3-2 对已决策项的重复审批（不同幂等键）被拒：记录已非 pending_review，
    // findPendingSupervisorReviewById 过滤后抛 SUPERVISOR_REVIEW_NOT_FOUND（真实错误码）。
    @Test
    void approveSecondTimeOnDecidedReviewThrowsConflict() {
        reviewRepository.superiors.put("usr_supervisor_l3", "usr_reviewer_decided");
        TestSecurityActors.asSupervisorLevel(3);
        var pending = service.evaluate(evaluate("review-decided-eval", "contract violation", List.of(),
                "contract_violation", "contract", "contract_decided", 0L, "violation"));

        TestSecurityActors.asSupervisor("usr_reviewer_decided", 3);
        service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "approve-decided-1", "approve", List.of(), 3, "approved", "first and final decision"));

        // 第二次用不同幂等键审批同一已决策记录 → 拒绝
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "approve-decided-2", "approve again", List.of(), 3, "rejected", "attempt to re-decide")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SUPERVISOR_REVIEW_NOT_FOUND"));
    }

    // §1.3-3 越权审批被拒：非主管体系账户（如普通商户用户）不在链上，不得审批主管复核记录。
    @Test
    void approveCrossTenantReviewThrowsForbidden() {
        reviewRepository.superiors.put("usr_supervisor_l3", "usr_reviewer_c");
        TestSecurityActors.asSupervisorLevel(3);
        var pending = service.evaluate(evaluate("review-crosstenant-eval", "contract violation", List.of(),
                "contract_violation", "contract", "contract_crosstenant", 0L, "violation"));

        TestSecurityActors.asMerchantUser("m_outsider");
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "approve-crosstenant", "approve", List.of(), 3, "approved", "out-of-scope approval attempt")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SUPERVISOR_NOT_CURRENT_APPROVER"));
    }

    // M087-A-03（PGM-1.10.1「税费规则与税费调整须经财务审核、税务审核、法务审核，再由顶层平台公司的老板最终确认后生效」）：
    // 税费调整固定三条平行线 + 主线到主会话；发起时必须逐线指定最终审批人。
    @Test
    void taxRuleAdjustmentRequiresThreeParallelLinesAndTopAuthority() {
        chainL1();
        for (String code : List.of("finance_review", "tax_review", "legal_review")) {
            reviewRepository.lines.put(code, new SupervisorReviewRepository.ApprovalLine(code, code, 1, null, "active"));
        }
        TestSecurityActors.asSupervisorLevel(1);
        assertThatThrownBy(() -> service.evaluate(evaluate("tax-no-lines", "v", List.of(), "finance.tax_rule.adjust", "tax_rule", "t1", 0L, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("APPROVAL_LINE_FINAL_APPROVER_REQUIRED"));
        var pending = service.evaluate(new SupervisorReviewEvaluateRequest("tax-lines", "v", List.of(), "finance.tax_rule.adjust", "tax_rule", "t2", 0L, null, null,
                List.of(new SupervisorReviewEvaluateRequest.ParallelLine("finance_review", "usr_fin"),
                        new SupervisorReviewEvaluateRequest.ParallelLine("tax_review", "usr_tax"),
                        new SupervisorReviewEvaluateRequest.ParallelLine("legal_review", "usr_legal"))));
        assertThat(pending.blocked()).isTrue();
        assertThat(pending.approvalLevel()).isEqualTo("top_authority");
        assertThat(reviewRepository.listChainNodes(pending.reviewId())).extracting(ApprovalChainNode::lineCode)
                .containsExactly("main", "main", "main", "finance_review", "tax_review", "legal_review");
        assertThat(reviewRepository.listChainNodes(pending.reviewId()).get(2).approverKind()).isEqualTo("master");
        assertThat(reviewRepository.schedules.get(pending.reviewId()).dueAt())
                .isBefore(java.time.Instant.now().plus(java.time.Duration.ofDays(4))); // finance.* 涉钱：1 个工作日
    }

    // M087：组织层级补齐后可为「链未生成」的待审单重新生成主线（fail-closed 的恢复路径）。
    @Test
    void unresolvedChainCanBeRegeneratedOnceTheHierarchyIsMaintained() {
        TestSecurityActors.asSupervisorLevel(1);
        var pending = service.evaluate(evaluate("regen", "v", List.of(), "contract_violation", "contract", "c_regen", 0L, "violation"));
        assertThat(reviewRepository.listChainNodes(pending.reviewId())).isEmpty();
        assertThatThrownBy(() -> service.regenerateChain(pending.reviewId()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("APPROVAL_CHAIN_UNRESOLVED"));
        chainL1();
        assertThat(service.regenerateChain(pending.reviewId())).extracting(ApprovalChainNode::approverId)
                .containsExactly("usr_reviewer_a", "usr_reviewer_c");
        assertThat(service.regenerateChain(pending.reviewId())).hasSize(2); // 已有节点不重生成
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).contains("governance.supervisor_review.chain_regenerated");
    }

    // M087-I2-02（PGM-1.7.1「任一人审批即完成该节点」「任一节点驳回则整单退回起草人」）：同一节点两名裁决人（原审批人与被转发人）
    // 先到先得，后到的相反结论被拒，节点与整单终态一致。
    @Test
    void secondDecisionOnAnAlreadyDecidedNodeIsRejectedAndTheFirstOutcomeStands() {
        reviewRepository.superiors.put("usr_supervisor_l1", "usr_reviewer_c");
        TestSecurityActors.asSupervisorLevel(1);
        var pending = service.evaluate(evaluate("review-cas", "v", List.of(), "contract_violation", "contract", "c_cas", 0L, "violation"));
        TestSecurityActors.asSupervisor("usr_reviewer_c", 3);
        service.forward(pending.reviewId(), new SupervisorReviewForwardRequest("cas-forward", null, "usr_anyone", "handle"));
        var rejected = service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("cas-reject", "note", List.of(), 3, "rejected", "no"));
        assertThat(rejected.reviewStatus()).isEqualTo("rejected");

        // 被转发人随后提交「通过」：单据已终态 → 不可再裁决；节点仍为已驳回。
        TestSecurityActors.asSupervisor("usr_anyone", 0);
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("cas-approve-late", "note", List.of(), 0, "approved", "yes")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SUPERVISOR_REVIEW_NOT_FOUND"));
        assertThat(reviewRepository.listChainNodes(pending.reviewId())).extracting(ApprovalChainNode::status).containsExactly("rejected");

        // 节点级 CAS：仓储不允许改写已裁决节点（并发双写的后到者拿不到更新行）。
        ApprovalChainNode decided = reviewRepository.listChainNodes(pending.reviewId()).get(0);
        assertThat(reviewRepository.updateChainNode(decided.withStatus("approved", "usr_anyone", java.time.Instant.now()))).isFalse();
        assertThat(reviewRepository.listChainNodes(pending.reviewId()).get(0).status()).isEqualTo("rejected");
    }

    // M087-I2-02：整单终态由节点表归约——任一节点 rejected 即整单 rejected，即使本次请求体是 approved。
    @Test
    void ticketOutcomeIsDerivedFromTheNodeTableNotFromTheRequestBody() {
        chainL1();
        reviewRepository.superiors.put("usr_fin_entry", "usr_fin_lead");
        reviewRepository.lines.put("finance", new SupervisorReviewRepository.ApprovalLine("finance", "财务线", 1, "usr_fin_entry", "active"));
        TestSecurityActors.asSupervisorLevel(1);
        var pending = service.evaluate(new SupervisorReviewEvaluateRequest("review-outcome", "v", List.of(), "contract_violation", "contract", "c_outcome", 0L, "violation", "usr_reviewer_c",
                List.of(new SupervisorReviewEvaluateRequest.ParallelLine("finance", "usr_fin_lead"))));
        // 财务线起点人驳回（整单退回）后，主线审批人再提交通过：单据已 rejected，不得再裁决。
        TestSecurityActors.asSupervisor("usr_fin_entry", 1);
        assertThat(service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("o-fin-reject", "note", List.of(), 1, "rejected", "no")).reviewStatus()).isEqualTo("rejected");
        assertThat(reviewRepository.listChainNodes(pending.reviewId())).extracting(ApprovalChainNode::status)
                .containsExactly("cancelled", "cancelled", "rejected", "cancelled");
        TestSecurityActors.asSupervisor("usr_reviewer_a", 3);
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("o-main-late", "note", List.of(), 3, "approved", "yes")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SUPERVISOR_REVIEW_NOT_FOUND"));
    }

    static final class InMemorySupervisorReviewRepository implements SupervisorReviewRepository {
        private final Map<String, SupervisorReviewDecisionRecord> reviews = new LinkedHashMap<>();
        int saveCount = 0;

        @Override
        public SupervisorReviewDecisionRecord saveSupervisorReview(SupervisorReviewDecisionRecord record) {
            reviews.put(record.reviewId(), record);
            saveCount++;
            return record;
        }

        @Override
        public Optional<SupervisorReviewDecisionRecord> findPendingSupervisorReviewById(String reviewId) {
            return Optional.ofNullable(reviews.get(reviewId))
                    .filter(r -> "pending_review".equals(r.reviewStatus()));
        }

        final Map<String, String> released = new LinkedHashMap<>();

        @Override
        public Optional<SupervisorReviewDecisionRecord> findApprovedUnreleasedSupervisorReview(
                String actionType, String objectType, String objectId, String initiatedBy) {
            return reviews.values().stream()
                    .filter(r -> "approved".equals(r.reviewStatus()) && !released.containsKey(r.reviewId()))
                    .filter(r -> actionType.equals(r.actionType()) && objectType.equals(r.objectType())
                            && objectId.equals(r.objectId()) && java.util.Objects.equals(initiatedBy, r.initiatedBy()))
                    .findFirst();
        }

        @Override
        public boolean markSupervisorReviewReleased(String reviewId, String releasedBy, java.time.Instant releasedAt) {
            return released.putIfAbsent(reviewId, releasedBy) == null;
        }

        final Map<String, ReviewSchedule> schedules = new LinkedHashMap<>();

        @Override
        public void scheduleSupervisorReview(String reviewId, String beneficiaryId, java.time.Instant dueAt) {
            schedules.put(reviewId, new ReviewSchedule(reviewId, beneficiaryId, dueAt, 0, null, null));
        }

        @Override
        public Optional<ReviewSchedule> findSupervisorReviewSchedule(String reviewId) {
            return Optional.ofNullable(schedules.get(reviewId));
        }

        @Override
        public boolean forwardSupervisorReview(String reviewId, String forwardedTo, String forwardedBy, java.time.Instant forwardedAt) {
            ReviewSchedule current = schedules.get(reviewId);
            if (current == null || findPendingSupervisorReviewById(reviewId).isEmpty()) {
                return false;
            }
            schedules.put(reviewId, new ReviewSchedule(reviewId, current.beneficiaryId(), current.dueAt(),
                    current.escalationCount(), current.escalatedAt(), forwardedTo));
            return true;
        }

        @Override
        public boolean escalateSupervisorReview(String reviewId, String approvalLevel, Integer requiredApproverRank,
                                                java.time.Instant dueAt, java.time.Instant escalatedAt) {
            SupervisorReviewDecisionRecord d = reviews.get(reviewId);
            ReviewSchedule current = schedules.get(reviewId);
            if (d == null || current == null || !"pending_review".equals(d.reviewStatus())) {
                return false;
            }
            reviews.put(reviewId, new SupervisorReviewDecisionRecord(d.reviewId(), d.policyCode(), d.actionType(), d.objectType(),
                    d.objectId(), d.blocked(), d.reviewStatus(), d.requiredLevel(), d.reviewerLevel(), d.decision(), d.reason(),
                    d.evidenceAttachmentIds(), d.evaluatedAt(), d.initiatedBy(), approvalLevel, requiredApproverRank));
            schedules.put(reviewId, new ReviewSchedule(reviewId, current.beneficiaryId(), dueAt,
                    current.escalationCount() + 1, escalatedAt, current.forwardedTo()));
            return true;
        }

        @Override
        public List<PendingReview> listPendingSupervisorReviews(int limit) {
            return reviews.values().stream()
                    .filter(r -> "pending_review".equals(r.reviewStatus()))
                    .map(r -> new PendingReview(r, schedules.get(r.reviewId())))
                    .limit(limit).toList();
        }

        @Override
        public List<PendingReview> listOverdueSupervisorReviews(java.time.Instant now, int limit) {
            return listPendingSupervisorReviews(Integer.MAX_VALUE).stream()
                    .filter(p -> p.schedule() != null && p.schedule().dueAt() != null && !p.schedule().dueAt().isAfter(now))
                    .limit(limit).toList();
        }

        // ── M087：组织层级（直接上级）、平行线设置、审批链节点 ──
        final Map<String, String> superiors = new LinkedHashMap<>();
        final Map<String, ApprovalLine> lines = new LinkedHashMap<>();
        final Map<String, List<ApprovalChainNode>> nodes = new LinkedHashMap<>();

        @Override
        public Optional<String> findDirectSuperior(String userId) {
            return Optional.ofNullable(superiors.get(userId));
        }

        @Override
        public Optional<ApprovalLine> findApprovalLine(String lineCode) {
            return Optional.ofNullable(lines.get(lineCode));
        }

        @Override
        public void saveChainNodes(String reviewId, List<ApprovalChainNode> chain) {
            nodes.put(reviewId, new ArrayList<>(chain));
        }

        @Override
        public List<ApprovalChainNode> listChainNodes(String reviewId) {
            return List.copyOf(nodes.getOrDefault(reviewId, List.of()));
        }

        @Override
        public boolean updateChainNode(ApprovalChainNode node) {
            List<ApprovalChainNode> chain = nodes.get(node.reviewId());
            if (chain == null) {
                return false;
            }
            for (int i = 0; i < chain.size(); i++) {
                if (chain.get(i).nodeId().equals(node.nodeId())) {
                    if (!chain.get(i).pending()) {
                        return false; // M087-I2-02：只允许改写待审节点（与 JDBC 的 status='pending' 条件一致）
                    }
                    chain.set(i, node);
                    return true;
                }
            }
            return false;
        }
    }
}
