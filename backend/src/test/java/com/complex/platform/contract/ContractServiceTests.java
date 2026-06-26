package com.complex.platform.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.contract.dto.ContractActivateRequest;
import com.complex.platform.contract.dto.ContractApproveRequest;
import com.complex.platform.contract.dto.ContractChangeApproveRequest;
import com.complex.platform.contract.dto.ContractChangeRequest;
import com.complex.platform.contract.dto.ContractDraftRequest;
import com.complex.platform.contract.dto.ContractResolveDisputeRequest;
import com.complex.platform.contract.dto.ContractSettleRequest;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 合同域 unit 测试（V162 重建）：核心验「分组并行审批引擎」——
 * 阶段内会签（都过才进下一阶段）、阶段间串行、任一驳回退回起草、生效固化快照、退租争议挂起+裁决。无 DB。
 */
class ContractServiceTests {

    private final InMemoryContractRepository repo = new InMemoryContractRepository();
    private final AuthorizationService authz = mock(AuthorizationService.class);
    private ContractService service;

    @BeforeEach
    void setUp() {
        when(authz.currentActor()).thenReturn(
                new AccessActor("usr_admin", "platform_admin_user", "platform_admin", "org_1", null, "active"));
        service = new ContractService(repo, mock(AuditEventService.class), authz);
    }

    private ContractRecord newDraft() {
        return service.createDraft(new ContractDraftRequest("k_draft", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "by_area", 500000L, Map.of("breach", "2x"),
                List.of(Map.of("item", "deposit_offset"), Map.of("item", "unpaid_fee")), List.of(), null));
    }

    private ContractRecord approve(ContractRecord c, String code) {
        return service.approve(c.id(), new ContractApproveRequest("k_" + code, code, "approve", "ok"));
    }

    @Test
    void parallelStages_advanceOnlyWhenStageFullyApprovedThenGmThenPendingEffective() {
        ContractRecord c = newDraft();
        assertThat(c.lifecycleStatus()).isEqualTo("drafting");

        service.submit(c.id(), "k_submit");
        // 阶段① 三人并行会签全部 pending。
        assertThat(repo.listApprovalsByContract(c.id())).hasSize(3)
                .allMatch(a -> a.stageSeq() == 1 && "pending".equals(a.decision()));

        // 业务审过 → 阶段①未全过，仍待确认，未物化阶段②。
        ContractRecord afterBiz = approve(c, "contract.approve.business");
        assertThat(afterBiz.lifecycleStatus()).isEqualTo("pending_confirmation");
        assertThat(repo.listApprovalsByContract(c.id())).noneMatch(a -> a.stageSeq() == 2);

        // 财务审过 → 仍差法务。
        approve(c, "contract.approve.finance");
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("pending_confirmation");

        // 法务审过 → 阶段①全过，物化阶段②（总经理 pending），仍待确认。
        approve(c, "contract.approve.legal");
        assertThat(repo.listApprovalsByContract(c.id())).anyMatch(
                a -> a.stageSeq() == 2 && "contract.approve.gm".equals(a.approverPermissionCode())
                        && "pending".equals(a.decision()));
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("pending_confirmation");

        // 总经理终审 → 全链走完 → 待生效。
        ContractRecord afterGm = approve(c, "contract.approve.gm");
        assertThat(afterGm.lifecycleStatus()).isEqualTo("pending_effective");
    }

    @Test
    void rejectAnySlotSendsContractBackToDraftingAndClearsApprovals() {
        ContractRecord c = newDraft();
        service.submit(c.id(), "k_submit");
        ContractRecord back = service.approve(c.id(),
                new ContractApproveRequest("k_r", "contract.approve.finance", "reject", "条款不合规"));
        assertThat(back.lifecycleStatus()).isEqualTo("drafting");
        assertThat(repo.listApprovalsByContract(c.id())).isEmpty();
    }

    @Test
    void activateFreezesSnapshotAndRequiresPendingEffective() {
        ContractRecord c = newDraft();
        // 未走审批不能生效。
        assertThatThrownBy(() -> service.activate(c.id(),
                new ContractActivateRequest("k_a", List.of("sp_1"))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_NOT_PENDING_EFFECTIVE"));

        service.submit(c.id(), "k_submit");
        approve(c, "contract.approve.business");
        approve(c, "contract.approve.finance");
        approve(c, "contract.approve.legal");
        approve(c, "contract.approve.gm");

        ContractRecord eff = service.activate(c.id(), new ContractActivateRequest("k_a2", List.of("sp_1")));
        assertThat(eff.lifecycleStatus()).isEqualTo("effective");
        assertThat(eff.snapshot()).isNotNull();
        assertThat(eff.snapshot()).containsKeys("rentTerms", "commissionMode", "energyFeeBasis", "penaltyTerms");
        assertThat(eff.snapshot().get("commissionMode")).isEqualTo("fixed_ratio");
    }

    @Test
    void settleWithDisputeHangsThenResolveSettlesAndTerminates() {
        ContractRecord c = effectiveContract();
        // 带争议 → 挂起。
        ContractSettlementRecord disputed = service.settle(c.id(),
                new ContractSettleRequest("k_s", "move_out", "押金抵扣有异议"));
        assertThat(disputed.status()).isEqualTo("disputed");
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("effective");

        // 主管裁决 → 结清 + 合同终止 + 逐例结算项按 settlement_rules 顺序。
        ContractSettlementRecord settled = service.resolveDispute(disputed.id(),
                new ContractResolveDisputeRequest("k_rd", "押金按约抵扣"));
        assertThat(settled.status()).isEqualTo("settled");
        assertThat(settled.settlementItems()).hasSize(2);
        assertThat(settled.settlementItems().get(0)).containsEntry("order", 1);
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("terminated");
    }

    @Test
    void contractChangeWalksSameParallelChainThenApplies() {
        ContractRecord c = effectiveContract();
        // 发起变更 → 变更 pending + 物化阶段①三个并行槽（changeId 维度，与新签审批不混）。
        ContractChangeRecord change = service.raiseChange(c.id(),
                new ContractChangeRequest("k_ch", "rent_change", Map.of("baseRentCent", 120000)));
        assertThat(change.approvalStatus()).isEqualTo("pending");
        assertThat(repo.listApprovalsByChange(change.id())).hasSize(3)
                .allMatch(a -> a.stageSeq() == 1 && a.changeId() != null);

        // 走与新签同一并行链：阶段①三人会签 → 阶段②总经理。
        approveChange(change.id(), "contract.approve.business");
        approveChange(change.id(), "contract.approve.finance");
        approveChange(change.id(), "contract.approve.legal");
        assertThat(repo.findChangeById(change.id()).orElseThrow().approvalStatus()).isEqualTo("pending");
        ContractChangeRecord applied = approveChange(change.id(), "contract.approve.gm");
        assertThat(applied.approvalStatus()).isEqualTo("approved");
        assertThat(applied.newSnapshot()).isNotNull();
    }

    private ContractChangeRecord approveChange(String changeId, String code) {
        return service.approveChange(changeId, new ContractChangeApproveRequest("k_" + code, code, "approve", "ok"));
    }

    private ContractRecord effectiveContract() {
        ContractRecord c = newDraft();
        service.submit(c.id(), "k_submit");
        approve(c, "contract.approve.business");
        approve(c, "contract.approve.finance");
        approve(c, "contract.approve.legal");
        approve(c, "contract.approve.gm");
        return service.activate(c.id(), new ContractActivateRequest("k_a", List.of("sp_1")));
    }

    /** 内存仓储：种平台默认链（阶段①业务/财务/法务并行 → 阶段②总经理）。 */
    private static final class InMemoryContractRepository implements ContractRepository {
        private final Map<String, ContractRecord> contracts = new LinkedHashMap<>();
        private final Map<String, ContractApprovalRecord> approvals = new LinkedHashMap<>();
        private final Map<String, ContractChangeRecord> changes = new LinkedHashMap<>();
        private final Map<String, ContractSettlementRecord> settlements = new LinkedHashMap<>();
        private final List<SpaceExpiryColorRuleRecord> colorRules = new ArrayList<>();
        private final List<ContractApprovalFlowStepRecord> defaultSteps = List.of(
                new ContractApprovalFlowStepRecord("s1", "caf_default", null, 1, "业务审核", "contract.approve.business", true),
                new ContractApprovalFlowStepRecord("s2", "caf_default", null, 1, "财务审核", "contract.approve.finance", true),
                new ContractApprovalFlowStepRecord("s3", "caf_default", null, 1, "法务审核", "contract.approve.legal", true),
                new ContractApprovalFlowStepRecord("s4", "caf_default", null, 2, "总经理终审", "contract.approve.gm", true));

        @Override
        public ContractRecord saveContract(ContractRecord c) {
            contracts.put(c.id(), c);
            return c;
        }

        @Override
        public List<ContractRecord> listContracts() {
            return List.copyOf(contracts.values());
        }

        @Override
        public Optional<ContractRecord> findContractById(String id) {
            return Optional.ofNullable(contracts.get(id));
        }

        @Override
        public ContractApprovalRecord saveApproval(ContractApprovalRecord a) {
            approvals.put(a.id(), a);
            return a;
        }

        @Override
        public List<ContractApprovalRecord> listApprovalsByContract(String contractId) {
            return approvals.values().stream()
                    .filter(a -> a.contractId().equals(contractId) && a.changeId() == null)
                    .sorted((x, y) -> Integer.compare(x.stageSeq(), y.stageSeq())).toList();
        }

        @Override
        public void clearApprovals(String contractId) {
            approvals.values().removeIf(a -> a.contractId().equals(contractId) && a.changeId() == null);
        }

        @Override
        public List<ContractApprovalRecord> listApprovalsByChange(String changeId) {
            return approvals.values().stream().filter(a -> changeId.equals(a.changeId()))
                    .sorted((x, y) -> Integer.compare(x.stageSeq(), y.stageSeq())).toList();
        }

        @Override
        public void clearApprovalsByChange(String changeId) {
            approvals.values().removeIf(a -> changeId.equals(a.changeId()));
        }

        @Override
        public ContractChangeRecord saveChange(ContractChangeRecord ch) {
            changes.put(ch.id(), ch);
            return ch;
        }

        @Override
        public Optional<ContractChangeRecord> findChangeById(String id) {
            return Optional.ofNullable(changes.get(id));
        }

        @Override
        public List<ContractChangeRecord> listChangesByContract(String contractId) {
            return changes.values().stream().filter(ch -> ch.contractId().equals(contractId)).toList();
        }

        @Override
        public ContractSettlementRecord saveSettlement(ContractSettlementRecord s) {
            settlements.put(s.id(), s);
            return s;
        }

        @Override
        public Optional<ContractSettlementRecord> findSettlementById(String id) {
            return Optional.ofNullable(settlements.get(id));
        }

        @Override
        public List<ContractSettlementRecord> listSettlementsByContract(String contractId) {
            return settlements.values().stream().filter(s -> s.contractId().equals(contractId)).toList();
        }

        @Override
        public List<ContractApprovalFlowStepRecord> listActiveFlowSteps(String projectId) {
            return defaultSteps;
        }

        @Override
        public List<ContractApprovalFlowStepRecord> listFlowStepsByProject(String projectId) {
            return defaultSteps;
        }

        @Override
        public void saveFlowConfig(String projectId, String name, List<ContractApprovalFlowStepRecord> steps) {
            // 测试不覆盖配置写
        }

        @Override
        public SpaceExpiryColorRuleRecord saveColorRule(SpaceExpiryColorRuleRecord rule) {
            colorRules.add(rule);
            return rule;
        }

        @Override
        public List<SpaceExpiryColorRuleRecord> listColorRules(String projectId) {
            return List.copyOf(colorRules);
        }

        @Override
        public void bindContractSpaces(String contractId, List<String> spaceIds, String projectId) {
            // 内存无需绑定
        }

        @Override
        public boolean hasOverlappingEffectiveContractForSpace(
                String spaceId, LocalDate effectiveDate, LocalDate expiryDate, String excludeContractId) {
            return false;
        }

        @Override
        public int markContractsExpired(LocalDate asOf) {
            return 0;
        }

        @Override
        public List<SpaceRentalStatusRecord> listSpaceRentalRaw(String projectId, LocalDate asOf) {
            return List.of();
        }
    }
}
