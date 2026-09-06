package com.complex.platform.governance.review;

import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
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

class SupervisorReviewServiceTests {

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
                new AuditEventService(auditEventRepository)
        );
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    @Test
    void listPoliciesReturnsBlockingCatalogAndWritesAudit() {
        var page = service.listPolicies();

        assertThat(page.total()).isEqualTo(9);
        assertThat(page.items())
                .extracting(SupervisorReviewPolicyRecord::actionType)
                .contains("manual_release", "bill_waiver", "contract_violation", "base_definition_violation",
                        "permission_grant_l1", "permission_grant_l2", "permission_grant_l3");
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
        var decision = service.evaluate(new SupervisorReviewEvaluateRequest(
                "review-evaluate-1",
                "contract definition mismatch",
                List.of("att_1"),
                "contract_violation",
                "contract",
                "contract_1",
                0L,
                "contract subject differs from operation subject"
        ));

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
        assertThatThrownBy(() -> service.evaluate(new SupervisorReviewEvaluateRequest(
                "review-evaluate-unknown",
                "unknown action",
                List.of(),
                "unsupported_action",
                "bill",
                "bill_1",
                0L,
                null
        )))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("UNKNOWN_REVIEW_ACTION"));
    }

    // P0 red→green: level 1 用户不得通过请求体自报 reviewerLevel=3 绕过 L3 门限
    @Test
    void approveRejectsCallerWithInsufficientSupervisorLevelRegardlessOfRequestBody() {
        TestSecurityActors.asSupervisorLevel(1);
        var pending = service.evaluate(new SupervisorReviewEvaluateRequest(
                "review-p0-red",
                "contract violation",
                List.of(),
                "contract_violation",
                "contract",
                "contract_p0",
                0L,
                "violation"
        ));

        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "approve-p0-red",
                "approve",
                List.of(),
                3,  // 请求体声明 level=3，但实际用户只有 level=1
                "approved",
                "spoofed"
        )))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SUPERVISOR_LEVEL_TOO_LOW"));
    }

    @Test
    void approveRequiresLevelThreeSupervisor() {
        TestSecurityActors.asSupervisorLevel(2);
        var pending = service.evaluate(new SupervisorReviewEvaluateRequest(
                "review-evaluate-level3",
                "contract violation",
                List.of("att_2"),
                "contract_violation",
                "contract",
                "contract_1",
                0L,
                "contract mismatch"
        ));

        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-approve-low",
                "approve",
                List.of("att_2"),
                2,  // 请求体字段保持不变，但服务器忽略它，从安全上下文取 level=2 < required 3
                "approved",
                "ok"
        )))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SUPERVISOR_LEVEL_TOO_LOW"));
    }

    @Test
    void approveRecordsDecisionWhenReviewerLevelIsEnough() {
        TestSecurityActors.asSupervisorLevel(1); // 发起人 L1（rank 3）→ two_levels_up 所需审批人 rank 9
        var pending = service.evaluate(new SupervisorReviewEvaluateRequest(
                "review-evaluate-approve",
                "contract violation",
                List.of("att_3"),
                "contract_violation",
                "contract",
                "contract_2",
                0L,
                "contract mismatch"
        ));

        // A different L3 supervisor approves (segregation of duties)
        TestSecurityActors.asSupervisor("usr_reviewer_a", 3);
        var decision = service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-approve-1",
                "approve",
                List.of("att_3"),
                3,
                "approved",
                "evidence checked"
        ));

        assertThat(decision.reviewId()).isEqualTo(pending.reviewId());
        assertThat(decision.reviewStatus()).isEqualTo("approved");
        assertThat(decision.reviewerLevel()).isEqualTo(3); // 从安全上下文取，不是请求体
        assertThat(decision.evidenceAttachmentIds()).containsExactly("att_3");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("governance.supervisor_review.decided");
    }

    // Segregation of duties: the supervisor who initiated the action cannot approve their own review,
    // even when they hold a sufficient level.
    @Test
    void approveRejectsSelfReviewByInitiator() {
        TestSecurityActors.asSupervisor("usr_initiator_l3", 3);
        var pending = service.evaluate(new SupervisorReviewEvaluateRequest(
                "review-self-review",
                "contract violation",
                List.of(),
                "contract_violation",
                "contract",
                "contract_self",
                0L,
                "violation"
        ));

        // Same supervisor (same user id), sufficient level, tries to approve their own review
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-self-approve",
                "approve",
                List.of(),
                3,
                "approved",
                "self approval attempt")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
    }

    @Test
    void approveRejectsUnknownReviewId() {
        TestSecurityActors.asSupervisorLevel(3);
        assertThatThrownBy(() -> service.approve("review_missing", new SupervisorReviewApprovalRequest(
                "review-approve-missing",
                "approve missing",
                List.of("att_missing"),
                3,
                "approved",
                "should not fabricate approval"
        )))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SUPERVISOR_REVIEW_NOT_FOUND"));
    }

    @Test
    void approveUsesRequiredLevelFromEvaluatedReview() {
        TestSecurityActors.asSupervisorLevel(2);
        var pending = service.evaluate(new SupervisorReviewEvaluateRequest(
                "review-evaluate-refund",
                "refund needs review",
                List.of("att_refund"),
                "refund",
                "ticket_refund",
                "refund_1",
                100L,
                null
        ));

        // A different L2 supervisor approves (segregation of duties)
        TestSecurityActors.asSupervisor("usr_reviewer_b", 2);
        var approved = service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-approve-refund",
                "approve refund",
                List.of("att_refund"),
                2,
                "approved",
                "refund evidence checked"
        ));

        assertThat(pending.requiredLevel()).isEqualTo(2);
        assertThat(approved.requiredLevel()).isEqualTo(2);
        assertThat(approved.reviewerLevel()).isEqualTo(2); // 从安全上下文取
        assertThat(approved.reviewStatus()).isEqualTo("approved");
    }

    // ── R3 修复 P0-2: 审批层级逐级上提 REQ-69 ──────────────────────────────

    @Test
    void evaluateRecordsEscalationApprovalLevelOnDecision() {
        TestSecurityActors.asSupervisorLevel(1);
        var contract = service.evaluate(new SupervisorReviewEvaluateRequest(
                "review-approval-level-contract", "v", List.of(),
                "contract_violation", "contract", "c1", 0L, "violation"));
        assertThat(contract.approvalLevel()).isEqualTo("two_levels_up");
        // 可解析职级的夹具：发起人 L1 → rank 3，two_levels_up → 所需审批人 rank 9。
        assertThat(contract.requiredApproverRank()).isEqualTo(9);

        var baseDef = service.evaluate(new SupervisorReviewEvaluateRequest(
                "review-approval-level-base", "v", List.of(),
                "base_definition_violation", "definition", "d1", 0L, "violation"));
        assertThat(baseDef.approvalLevel()).isEqualTo("top_authority");
    }

    @Test
    void approveRejectsReviewerWhoseLevelRankIsBelowRequiredApproverRank() {
        SupervisorReviewService rankService = rankAwareService(Map.of("usr_init", 19, "usr_low", 22));
        TestSecurityActors.asSupervisor("usr_init", 3);
        var pending = rankService.evaluate(new SupervisorReviewEvaluateRequest(
                "review-rank-reject", "v", List.of(),
                "contract_violation", "contract", "c_rank", 0L, "violation"));
        assertThat(pending.requiredApproverRank()).isEqualTo(25); // two_levels_up from rank 19

        TestSecurityActors.asSupervisor("usr_low", 3); // rank 22 → 主管级 8>=3 通过，但管理职级 rank 22 < 25
        assertThatThrownBy(() -> rankService.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-rank-reject-approve", "approve", List.of(), 3, "approved", "low rank")))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("SUPERVISOR_RANK_TOO_LOW"));
    }

    @Test
    void approveAllowsReviewerWhoseLevelRankMeetsRequiredApproverRank() {
        SupervisorReviewService rankService = rankAwareService(Map.of("usr_init", 19, "usr_high", 28));
        TestSecurityActors.asSupervisor("usr_init", 3);
        var pending = rankService.evaluate(new SupervisorReviewEvaluateRequest(
                "review-rank-allow", "v", List.of(),
                "contract_violation", "contract", "c_rank2", 0L, "violation"));

        TestSecurityActors.asSupervisor("usr_high", 3); // rank 28 >= 25
        var approved = rankService.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-rank-allow-approve", "approve", List.of(), 3, "approved", "high rank"));
        assertThat(approved.reviewStatus()).isEqualTo("approved");
        assertThat(approved.requiredApproverRank()).isEqualTo(25);
    }

    // ── 双轨收口（platform-governance §24.3）：无静默降级 / specific_role 角色解析 / 同商户范围闸 ──

    // §24.3-A 无职级配置不被静默降级：可解析职级的真实部署里，发起人无 staff_level_assignment（rank 0）时
    // 不再回退仅主管级（rank=null），而是收口为安全保守 rank 下限；rank 不足的审批人被 SUPERVISOR_RANK_TOO_LOW 拒。
    @Test
    void evaluateConservativeRankWhenInitiatorHasNoStaffLevelAndRepoResolvable() {
        // 发起人 rank 0 = 无职级配置；审批人 rank 9（主管级 3 达标）但低于保守下限 15。
        com.complex.platform.access.AccessRepository repo =
                levelAwareRepository(Map.of("usr_no_level", 0, "usr_reviewer_no_level", 9), null);
        SupervisorReviewService svc = new SupervisorReviewService(
                new AuthorizationService(repo),
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                reviewRepository,
                new AuditEventService(auditEventRepository));

        TestSecurityActors.asSupervisor("usr_no_level", 3);
        var pending = svc.evaluate(new SupervisorReviewEvaluateRequest(
                "review-conservative", "v", List.of(),
                "contract_violation", "contract", "c_cons", 0L, "violation"));
        // two_levels_up + requiredLevel 3 → conservative floor(max(3,1)*3=9)+2*3 = 15（非 null，未静默降级）。
        assertThat(pending.requiredApproverRank()).isEqualTo(15);

        // 另一个 rank 9（< 15）的审批人被 rank 闸拒，而非旁路放行。
        TestSecurityActors.asSupervisor("usr_reviewer_no_level", 3);
        assertThatThrownBy(() -> svc.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-conservative-approve", "approve", List.of(), 3, "approved", "no rank")))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("SUPERVISOR_RANK_TOO_LOW"));
    }

    // §24.3-B specific_role 按角色解析而非旁路：审批人未经角色持有指定审批权限 → SUPERVISOR_ROLE_NOT_AUTHORIZED；
    // 经角色持有 → 放行。
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
                new AuditEventService(auditEventRepository));

        TestSecurityActors.asSupervisor("usr_init_sr", 2);
        var pending = svc.evaluate(new SupervisorReviewEvaluateRequest(
                "review-sr", null, List.of("att"), "refund", "ticket", "r1", 100L, null));
        assertThat(pending.requiredApproverRank()).isNull(); // specific_role 不按 rank

        // 无角色审批人被拒：现口径下主管级本身由「经角色持有审批权限」推导，不持有即级别 0，在更早的级别闸被拒。
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

    /** 共享 service 用到的审批人夹具 rank：a → 主管级 3、b → 主管级 2（用例断言从上下文取的级别）；其余为顶级 30。 */
    private static final Map<String, Integer> SHARED_RANKS = Map.of(
            "usr_reviewer_a", 9, "usr_reviewer_b", 6, "usr_reviewer_c", 30, "usr_reviewer_idem", 30, "usr_reviewer_decided", 30);

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
        TestSecurityActors.asSupervisor("usr_release_init", 3);
        var pending = service.evaluate(new SupervisorReviewEvaluateRequest(
                "review-evidence-eval", "manual release", List.of("att_eval"),
                "manual_release", "ticket", "t_evidence", 0L, null));
        assertThat(pending.blocked()).isTrue();

        TestSecurityActors.asSupervisor("usr_release_reviewer", 3);
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-evidence-no-attachment", "note", List.of(), 3, "approved", "ok")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-evidence-no-note", null, List.of("att_approve"), 3, "approved", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        var approved = service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "review-evidence-ok", "note", List.of("att_approve"), 3, "approved", "evidence attached"));
        assertThat(approved.reviewStatus()).isEqualTo("approved");
    }

    /** 构造一个 AuthorizationService 由 mock AccessRepository 提供指定用户的 level_rank 的 service。 */
    private SupervisorReviewService rankAwareService(Map<String, Integer> userRanks) {
        com.complex.platform.access.AccessRepository repo = levelAwareRepository(userRanks, null);
        return new SupervisorReviewService(
                new AuthorizationService(repo),
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                reviewRepository,
                new AuditEventService(auditEventRepository));
    }

    // P1-1 red→green: 新服务实例（模拟重启）能从持久化存储中读取已提交的待审记录
    @Test
    void pendingReviewSurvivesServiceRestart() {
        TestSecurityActors.asSupervisorLevel(3);
        var decision = service.evaluate(new SupervisorReviewEvaluateRequest(
                "review-persist-1",
                "contract violation",
                List.of(),
                "contract_violation",
                "contract",
                "contract_persist",
                0L,
                "violation"
        ));

        // 创建新 service 实例（共享同一 reviewRepository，模拟重启后仍能读取）
        SupervisorReviewService newService = new SupervisorReviewService(
                new AuthorizationService(levelAwareRepository(SHARED_RANKS, null)),
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                reviewRepository,  // 共享同一持久化存储
                new AuditEventService(auditEventRepository)
        );

        // A different L3 supervisor approves after restart (segregation of duties)
        TestSecurityActors.asSupervisor("usr_reviewer_c", 3);
        var approved = newService.approve(decision.reviewId(), new SupervisorReviewApprovalRequest(
                "review-persist-approve-1",
                "approve",
                List.of(),
                3,
                "approved",
                "approved after restart"
        ));
        assertThat(approved.reviewStatus()).isEqualTo("approved");
    }

    // P1-2 red→green: 相同幂等键的 evaluate 只持久化一次
    @Test
    void evaluateWithSameIdempotencyKeyReturnsSameReviewAndPersistsOnce() {
        TestSecurityActors.asSupervisorLevel(1);
        var request = new SupervisorReviewEvaluateRequest(
                "review-idem-1",
                "contract violation",
                List.of(),
                "contract_violation",
                "contract",
                "contract_idem",
                0L,
                "violation"
        );
        var first = service.evaluate(request);
        var second = service.evaluate(request);

        assertThat(second.reviewId()).isEqualTo(first.reviewId());
        assertThat(reviewRepository.saveCount).isEqualTo(1); // 只保存一次
    }

    // ── R6 §1.3 督导复核补强：命中/未命中两分支 + 字段断言 ──────────────────

    // §1.3-1 未命中分支：blocksByDefault 策略但金额未达阈值、无 violation、非 always-blocked，
    // 决策为 allowed、不落待审记录。bill_waiver(minAmount=1) 传 amount=0 即落入未命中。
    @Test
    // M007-F-07（PGM-1.16.2 常见复核：免单、退款、调账不设金额豁免）：金额 0、无违规说明的免单也必须进复核。
    void evaluateBlocksBillWaiverEvenAtZeroAmount() {
        TestSecurityActors.asSupervisorLevel(1);
        var decision = service.evaluate(new SupervisorReviewEvaluateRequest(
                "review-autopass-1",
                null,
                List.of(),
                "bill_waiver",
                "bill",
                "bill_autopass",
                0L,    // < minAmount(1) → amountHit=false
                null   // 无 violationSummary → violationHit=false；bill_waiver 非 always-blocked
        ));

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

    // §1.3-1 命中分支（靠 amountHit 命中，区别于既有 contract_violation 的 always-blocked），
    // 补全 SupervisorReviewDecisionRecord 字段落库断言。
    @Test
    void evaluateReturnsReviewRequiredWhenPolicyMatches() {
        TestSecurityActors.asSupervisorLevel(1);
        var decision = service.evaluate(new SupervisorReviewEvaluateRequest(
                "review-match-1",
                null,
                List.of("att_match"),
                "bill_waiver",
                "bill",
                "bill_match",
                500L,  // >= minAmount(1) → amountHit=true，命中策略
                null
        ));

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
        TestSecurityActors.asSupervisorLevel(3);
        var pending = service.evaluate(new SupervisorReviewEvaluateRequest(
                "review-idem-approve-eval",
                "contract violation",
                List.of(),
                "contract_violation",
                "contract",
                "contract_idem_approve",
                0L,
                "violation"
        ));
        int saveCountAfterEvaluate = reviewRepository.saveCount;

        TestSecurityActors.asSupervisor("usr_reviewer_idem", 3);
        var approval = new SupervisorReviewApprovalRequest(
                "approve-idem-key",
                "approve",
                List.of(),
                3,
                "approved",
                "first decision"
        );
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
        TestSecurityActors.asSupervisorLevel(3);
        var pending = service.evaluate(new SupervisorReviewEvaluateRequest(
                "review-decided-eval",
                "contract violation",
                List.of(),
                "contract_violation",
                "contract",
                "contract_decided",
                0L,
                "violation"
        ));

        TestSecurityActors.asSupervisor("usr_reviewer_decided", 3);
        service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "approve-decided-1",
                "approve",
                List.of(),
                3,
                "approved",
                "first and final decision"
        ));

        // 第二次用不同幂等键审批同一已决策记录 → 拒绝
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "approve-decided-2",
                "approve again",
                List.of(),
                3,
                "rejected",
                "attempt to re-decide")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SUPERVISOR_REVIEW_NOT_FOUND"));
    }

    // §1.3-3 越权审批被拒：非主管体系账户（supervisor level=0，如普通商户用户）
    // 不得审批主管复核记录，被 SUPERVISOR_LEVEL_TOO_LOW (403 forbidden) 拒绝。
    @Test
    void approveCrossTenantReviewThrowsForbidden() {
        TestSecurityActors.asSupervisorLevel(3);
        var pending = service.evaluate(new SupervisorReviewEvaluateRequest(
                "review-crosstenant-eval",
                "contract violation",
                List.of(),
                "contract_violation",
                "contract",
                "contract_crosstenant",
                0L,
                "violation"
        ));

        // 一个职权范围之外的账户（非 supervisor_lN → currentSupervisorLevel()=0）尝试审批
        TestSecurityActors.asMerchantUser("m_outsider");
        assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                "approve-crosstenant",
                "approve",
                List.of(),
                3,
                "approved",
                "out-of-scope approval attempt")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SUPERVISOR_LEVEL_TOO_LOW"));
    }

    private static final class InMemorySupervisorReviewRepository implements SupervisorReviewRepository {
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
    }
}
