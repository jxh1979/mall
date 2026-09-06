package com.complex.platform.governance.chain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.StaffLevelAssignment;
import com.complex.platform.access.StaffLevelRecord;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.governance.review.ApprovalLevel;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

// M087（PGM-1.7.1「发起单据时只需指定该事项的最终审批人，系统按组织层级自动补全从发起人到该最终审批人之间的全部直接上级节点」
// 「需要平行线审批的另行指定该条线的最终审批人……每个部门或业务线须预先设定其平行审批的最低起点层级」）：链生成规则。
class ApprovalChainServiceTests {

    private final HierarchyRepository repository = new HierarchyRepository();
    private final ApprovalChainService chain = new ApprovalChainService(repository,
            new AuthorizationService(rankRepository(Map.of("usr_fin_entry", 3, "usr_fin_mid", 6, "usr_fin_lead", 12))));

    @Test
    void mainLineFollowsDirectSuperiorsUpToTheDesignatedFinalApprover() {
        repository.superiors.putAll(Map.of("usr_a", "usr_b", "usr_b", "usr_c", "usr_c", "usr_d"));
        var nodes = chain.generate("r1", "usr_a", null, ApprovalLevel.TWO_LEVELS_UP, "usr_d", List.of());
        assertThat(nodes).extracting(ApprovalChainNode::approverId).containsExactly("usr_b", "usr_c", "usr_d");
        assertThat(nodes).extracting(ApprovalChainNode::seq).containsExactly(1, 2, 3);
        assertThat(nodes).allMatch(node -> ApprovalChainNode.MAIN_LINE.equals(node.lineCode()) && node.pending());
    }

    @Test
    void policyTierIsOnlyTheLowerBoundForNodeCountWhenNoFinalApproverIsDesignated() {
        repository.superiors.putAll(Map.of("usr_a", "usr_b", "usr_b", "usr_c", "usr_c", "usr_d"));
        assertThat(chain.generate("r2", "usr_a", null, ApprovalLevel.ONE_LEVEL_UP, null, List.of()))
                .extracting(ApprovalChainNode::approverId).containsExactly("usr_b");
        assertThat(chain.generate("r3", "usr_a", null, ApprovalLevel.TWO_LEVELS_UP, null, List.of()))
                .extracting(ApprovalChainNode::approverId).containsExactly("usr_b", "usr_c");
        var top = chain.generate("r4", "usr_a", null, ApprovalLevel.TOP_AUTHORITY, null, List.of());
        assertThat(top).extracting(ApprovalChainNode::approverKind).containsExactly("user", "user", "user", "master");
    }

    @Test
    void designatedFinalApproverMustBeOnTheChainAndNotBelowThePolicyMinimum() {
        repository.superiors.putAll(Map.of("usr_a", "usr_b", "usr_b", "usr_c"));
        assertThatThrownBy(() -> chain.generate("r5", "usr_a", null, ApprovalLevel.ONE_LEVEL_UP, "usr_stranger", List.of()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FINAL_APPROVER_NOT_IN_CHAIN"));
        assertThatThrownBy(() -> chain.generate("r6", "usr_a", null, ApprovalLevel.TWO_LEVELS_UP, "usr_b", List.of()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FINAL_APPROVER_BELOW_POLICY_MINIMUM"));
    }

    @Test
    void initiatorAndBeneficiaryNeverBecomeNodesAndMissingHierarchyLeavesTheChainEmpty() {
        repository.superiors.putAll(Map.of("usr_a", "usr_benef", "usr_benef", "usr_c"));
        assertThat(chain.generate("r7", "usr_a", "usr_benef", ApprovalLevel.TWO_LEVELS_UP, null, List.of()))
                .extracting(ApprovalChainNode::approverId).containsExactly("usr_c");
        assertThat(chain.generate("r8", "usr_orphan", null, ApprovalLevel.ONE_LEVEL_UP, null, List.of())).isEmpty();
        assertThat(chain.generate("r9", "usr_x", null, ApprovalLevel.SPECIFIC_ROLE, null, List.of()))
                .extracting(ApprovalChainNode::approverKind).containsExactly("role");
    }

    @Test
    void parallelLineFillsFromItsEntryPersonUpToItsFinalApproverSkippingBelowMinimumStartRank() {
        repository.superiors.putAll(Map.of("usr_a", "usr_b", "usr_fin_entry", "usr_fin_mid", "usr_fin_mid", "usr_fin_lead"));
        repository.lines.put("finance", new SupervisorReviewRepository.ApprovalLine("finance", "财务线", 6, "usr_fin_entry", "active"));
        var nodes = chain.generate("r10", "usr_a", null, ApprovalLevel.ONE_LEVEL_UP, null,
                List.of(new ApprovalChainService.ParallelLine("finance", "usr_fin_lead")));
        assertThat(nodes.stream().filter(n -> "finance".equals(n.lineCode())).map(ApprovalChainNode::approverId))
                .containsExactly("usr_fin_mid", "usr_fin_lead"); // usr_fin_entry rank 3 < 最低起点 6，不生成
        assertThatThrownBy(() -> chain.generate("r11", "usr_a", null, ApprovalLevel.ONE_LEVEL_UP, null,
                List.of(new ApprovalChainService.ParallelLine("legal", "usr_fin_lead"))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("APPROVAL_LINE_NOT_CONFIGURED"));
    }

    @Test
    void cyclesInTheHierarchyStopAtTheFirstRepeat() {
        repository.superiors.putAll(Map.of("usr_a", "usr_b", "usr_b", "usr_a"));
        assertThat(chain.generate("r12", "usr_a", null, ApprovalLevel.TOP_AUTHORITY, null, List.of()))
                .extracting(ApprovalChainNode::approverKind).containsExactly("user", "master");
    }

    private static AccessRepository rankRepository(Map<String, Integer> ranks) {
        AccessRepository repo = Mockito.mock(AccessRepository.class);
        Mockito.when(repo.findStaffLevelAssignment(ArgumentMatchers.anyString())).thenAnswer(invocation -> {
            String userId = invocation.getArgument(0);
            Integer rank = ranks.get(userId);
            return rank == null ? Optional.empty() : Optional.of(new StaffLevelAssignment(
                    userId, "lvl_rank_" + rank, null, "CNY", null, null, null, "x", Instant.parse("2026-06-14T00:00:00Z")));
        });
        Mockito.when(repo.findStaffLevelById(ArgumentMatchers.anyString())).thenAnswer(invocation -> {
            String levelId = invocation.getArgument(0);
            int rank = Integer.parseInt(levelId.substring("lvl_rank_".length()));
            return Optional.of(new StaffLevelRecord(levelId, "M", "lower", rank, null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        });
        return repo;
    }

    private static final class HierarchyRepository implements SupervisorReviewRepository {
        final Map<String, String> superiors = new LinkedHashMap<>();
        final Map<String, ApprovalLine> lines = new LinkedHashMap<>();

        @Override
        public SupervisorReviewDecisionRecord saveSupervisorReview(SupervisorReviewDecisionRecord record) {
            return record;
        }

        @Override
        public Optional<SupervisorReviewDecisionRecord> findPendingSupervisorReviewById(String reviewId) {
            return Optional.empty();
        }

        @Override
        public Optional<String> findDirectSuperior(String userId) {
            return Optional.ofNullable(superiors.get(userId));
        }

        @Override
        public Optional<ApprovalLine> findApprovalLine(String lineCode) {
            return Optional.ofNullable(lines.get(lineCode));
        }
    }
}
