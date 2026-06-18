package com.complex.platform.governance.review;

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
                new AuthorizationService(null),
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

        assertThat(page.total()).isEqualTo(6);
        assertThat(page.items())
                .extracting(SupervisorReviewPolicyRecord::actionType)
                .contains("manual_release", "bill_waiver", "contract_violation", "base_definition_violation");
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
        TestSecurityActors.asSupervisorLevel(3);
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
        // 发起人无职级（AuthorizationService(null) → rank 0）时不按 rank 计算，回退既有 required_level。
        assertThat(contract.requiredApproverRank()).isNull();

        var baseDef = service.evaluate(new SupervisorReviewEvaluateRequest(
                "review-approval-level-base", "v", List.of(),
                "base_definition_violation", "definition", "d1", 0L, "violation"));
        assertThat(baseDef.approvalLevel()).isEqualTo("top_authority");
    }

    @Test
    void approveRejectsReviewerWhoseLevelRankIsBelowRequiredApproverRank() {
        SupervisorReviewService rankService = rankAwareService(Map.of("usr_init", 19, "usr_low", 4));
        TestSecurityActors.asSupervisor("usr_init", 3);
        var pending = rankService.evaluate(new SupervisorReviewEvaluateRequest(
                "review-rank-reject", "v", List.of(),
                "contract_violation", "contract", "c_rank", 0L, "violation"));
        assertThat(pending.requiredApproverRank()).isEqualTo(25); // two_levels_up from rank 19

        TestSecurityActors.asSupervisor("usr_low", 3); // 主管级 3>=3 通过，但管理职级 rank 4 < 25
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

    /** 构造一个 AuthorizationService 由 mock AccessRepository 提供指定用户的 level_rank 的 service。 */
    private SupervisorReviewService rankAwareService(Map<String, Integer> userRanks) {
        com.complex.platform.access.AccessRepository repo =
                org.mockito.Mockito.mock(com.complex.platform.access.AccessRepository.class);
        userRanks.forEach((userId, rank) -> {
            String levelId = "lvl_" + userId;
            org.mockito.Mockito.when(repo.findStaffLevelAssignment(userId)).thenReturn(java.util.Optional.of(
                    new com.complex.platform.access.StaffLevelAssignment(
                            userId, levelId, null, "CNY", null, null, null, "x",
                            java.time.Instant.parse("2026-06-14T00:00:00Z"))));
            org.mockito.Mockito.when(repo.findStaffLevelById(levelId)).thenReturn(java.util.Optional.of(
                    new com.complex.platform.access.StaffLevelRecord(
                            levelId, "M", "lower", rank, null, "active",
                            java.time.Instant.parse("2026-06-14T00:00:00Z"))));
        });
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
                new AuthorizationService(null),
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

    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String key = record.operationId() + ":" + record.idempotencyKey();
            if (records.containsKey(key)) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(record.operationId() + ":" + record.idempotencyKey(), record);
            return record;
        }
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }
}
