package com.complex.platform.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.contract.dto.ContractActivateRequest;
import com.complex.platform.contract.dto.ContractApproveRequest;
import com.complex.platform.contract.dto.ContractAttachmentRequest;
import com.complex.platform.contract.dto.ContractChangeApproveRequest;
import com.complex.platform.contract.dto.ContractChangeRequest;
import com.complex.platform.contract.dto.ContractDraftRequest;
import com.complex.platform.contract.dto.ContractRentBillRequest;
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
    private RecordingTerminationPort terminationPort;

    @BeforeEach
    void setUp() {
        when(authz.currentActor()).thenReturn(
                new AccessActor("usr_admin", "platform_admin_user", "platform_admin", "org_1", null, "active"));
        terminationPort = new RecordingTerminationPort();
        service = new ContractService(repo, mock(AuditEventService.class), authz, null, null, terminationPort);
    }

    // 3.13：草稿带意向空间——被生效合同占用即起草拒(409)；未占用则预绑定；提交审批时被占同样拒。
    @Test
    void draftWithOccupiedSpaceIsRejectedEarly() {
        repo.occupiedSpaces.add("space_a");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createDraft(new ContractDraftRequest(
                "k_sp1", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of(),
                null, null, null, null, null, List.of(), null, "餐饮", List.of("space_a"))))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo("CONTRACT_SPACE_CONFLICT"));
    }

    @Test
    void draftWithFreeSpacePreBindsAndSubmitReChecksConflict() {
        ContractRecord draft = service.createDraft(new ContractDraftRequest(
                "k_sp2", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of(),
                null, null, null, null, null, List.of(), null, "餐饮", List.of("space_b")));
        org.assertj.core.api.Assertions.assertThat(repo.listContractSpaceIds(draft.id())).containsExactly("space_b");

        // 审批期间空间被别的生效合同占用 → 提交审批被拒（审批人不该审注定冲突的单）。
        repo.occupiedSpaces.add("space_b");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.submit(draft.id(), "k_sp2_submit"))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo("CONTRACT_SPACE_CONFLICT"));
    }

    private ContractRecord newDraft() {
        return service.createDraft(new ContractDraftRequest("k_draft", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "by_area", 500000L, Map.of("breach", "2x"),
                List.of(Map.of("item", "deposit_offset"), Map.of("item", "unpaid_fee")), List.of(), null, "餐饮"));
    }

    private ContractRecord approve(ContractRecord c, String code) {
        return service.approve(c.id(), new ContractApproveRequest("k_" + code, code, "approve", "ok"));
    }

    @Test
    void createDraftIsIdempotentBySameKey() {
        // 1.5 幂等：同 idempotencyKey 重复提交返回同一草稿、不重复建。
        ContractRecord first = newDraft();
        ContractRecord second = newDraft();
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(repo.listContracts()).extracting(ContractRecord::id).containsExactly(first.id());
    }

    @Test
    void createDraftDistinctKeysCreateDistinctContracts() {
        ContractRecord a = newDraft();
        ContractRecord b = service.createDraft(new ContractDraftRequest("k_draft_2", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "by_area", 500000L, Map.of("breach", "2x"),
                List.of(Map.of("item", "deposit_offset")), List.of(), null, "餐饮"));
        assertThat(b.id()).isNotEqualTo(a.id());
        assertThat(repo.listContracts()).hasSize(2);
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
    void settleDisputeResolveToPendingThenCompleteTerminates() {
        ContractRecord c = effectiveContract();
        // 带争议 → 挂起（有争议），合同仍生效。
        ContractSettlementRecord disputed = service.settle(c.id(),
                new ContractSettleRequest("k_s", "move_out", "押金抵扣有异议"));
        assertThat(disputed.status()).isEqualTo("disputed");
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("effective");

        // 主管裁决 → 回到待结算（逐例项备齐），合同仍生效。
        ContractSettlementRecord resumed = service.resolveDispute(disputed.id(),
                new ContractResolveDisputeRequest("k_rd", "押金按约抵扣"));
        assertThat(resumed.status()).isEqualTo("pending_settlement");
        assertThat(resumed.settlementItems()).hasSize(2);
        assertThat(resumed.settlementItems().get(0)).containsEntry("order", 1);
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("effective");

        // 结清 → 已结清 + 合同终止。
        ContractSettlementRecord settled = service.completeSettlement(resumed.id(),
                new ContractResolveDisputeRequest("k_cs", null));
        assertThat(settled.status()).isEqualTo("settled");
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("terminated");
    }

    @Test
    void settleNoDisputeOpensPendingThenCompleteSettles() {
        ContractRecord c = effectiveContract();
        ContractSettlementRecord opened = service.settle(c.id(), new ContractSettleRequest("k_s2", "expiry", null));
        assertThat(opened.status()).isEqualTo("pending_settlement");
        assertThat(opened.settlementItems()).hasSize(2);
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("effective");
        ContractSettlementRecord settled = service.completeSettlement(opened.id(),
                new ContractResolveDisputeRequest("k_cs2", null));
        assertThat(settled.status()).isEqualTo("settled");
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("expired");
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

    @Test
    void activateSchedulesWholeTermRentBillsIdempotently() {
        // P1-A：生效即按整租期(2026-07..2027-06=12期)幂等排租金AR（ContractRentMath base=100000，无免租/递增）。
        ContractRecord c = effectiveContract();   // rentTerms baseRentCent=100000, 生效 2026-07-01 到期 2027-06-30
        List<ContractRentBillRecord> rent = repo.rentBills.stream()
                .filter(b -> b.contractId().equals(c.id()) && b.feeType().equals("rent")).toList();
        assertThat(rent).hasSize(12);
        assertThat(rent).allMatch(b -> b.amountCent() == 100000L);
        assertThat(rent).extracting(ContractRentBillRecord::period)
                .contains("2026-07", "2026-12", "2027-06");
        // 整租期已预排 → 手工再排任一期幂等冲突。
        assertThatThrownBy(() -> service.generateRentBill(c.id(), new ContractRentBillRequest("k_rb2", "2026-08")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_RENT_ALREADY_BILLED"));
    }

    @Test
    void draftStoresBusinessScopeAndAttachmentsMergeDedup() {
        ContractRecord c = newDraft();
        assertThat(c.rentTerms()).containsEntry("businessScope", "餐饮");   // 缺口3 经营范围入 rent_terms
        ContractRecord a1 = service.addAttachments(c.id(),
                new ContractAttachmentRequest("k_a1", List.of("scan_1", "scan_2")));
        assertThat(a1.attachmentIds()).containsExactly("scan_1", "scan_2");
        ContractRecord a2 = service.addAttachments(c.id(),
                new ContractAttachmentRequest("k_a2", List.of("scan_2", "scan_3")));   // 去重合并
        assertThat(a2.attachmentIds()).containsExactly("scan_1", "scan_2", "scan_3");
    }

    @Test
    void activateBooksRentPropertyForEachPeriodAndLeavesEnergyToEnergyDomain() {
        // 清单 P1-3 方案B:即便 by_contract 包干固定能源费,合同域生效也只排 rent+property(12 期 × 2 = 24 条);
        // 能源费统一由能耗抄表域 settleEnergyFee 出账,消除合同域×能耗域对同一户同期双挂账。
        ContractRecord d = service.createDraft(new ContractDraftRequest("k_pf", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"),
                Map.of("baseRentCent", 100000, "propertyFeeCent", 20000, "energyFeeCent", 5000),
                "fixed_ratio", "by_contract", 500000L, Map.of(), List.of(), List.of(), null, null));
        service.submit(d.id(), "k_sub");
        approve(d, "contract.approve.business");
        approve(d, "contract.approve.finance");
        approve(d, "contract.approve.legal");
        approve(d, "contract.approve.gm");
        service.activate(d.id(), new ContractActivateRequest("k_act", List.of("sp_pf")));
        List<ContractRentBillRecord> all = repo.rentBills.stream()
                .filter(b -> b.contractId().equals(d.id())).toList();
        assertThat(all).hasSize(24);
        List<ContractRentBillRecord> aug = all.stream().filter(b -> b.period().equals("2026-08")).toList();
        assertThat(aug).extracting(ContractRentBillRecord::feeType)
                .containsExactlyInAnyOrder("rent", "property_fee");
        assertThat(aug).extracting(ContractRentBillRecord::feeType).doesNotContain("energy_fee");
        assertThat(service.generatePeriodBills(d.id(), new ContractRentBillRequest("k_pb", "2026-08"))).isEmpty();
    }

    @Test
    void contractRejectsFixedEnergyFeeWhenBasisIsMetered() {
        // 清单 P1-3 固定额校验:按面积/按用量的合同不许再填固定能源费金额(下单时拦住,防脏数据)。
        assertThatThrownBy(() -> service.createDraft(new ContractDraftRequest("k_bad", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"),
                Map.of("baseRentCent", 100000, "energyFeeCent", 5000),
                "fixed_ratio", "by_usage", 500000L, Map.of(), List.of(), List.of(), null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void settlementItemsCarryRealAmountsAndPhasesAdvanceToSettled() {
        ContractRecord c = effectiveContract();   // deposit 500000；rules [deposit_offset, unpaid_fee]
        ContractSettlementRecord opened = service.settle(c.id(), new ContractSettleRequest("k_s3", "move_out", null));
        assertThat(opened.status()).isEqualTo("pending_settlement");
        // 缺口2 真实金额：保证金抵扣=500000；欠费=0(内存无未付AR)
        assertThat(opened.settlementItems().get(0)).containsEntry("amountCent", 500000L);
        // 缺口6 逐相推进：待结算→保证金抵扣中→欠费追缴中→已结清
        assertThat(service.advanceSettlement(opened.id(), new ContractResolveDisputeRequest("k_a1", null)).status())
                .isEqualTo("deposit_offsetting");
        assertThat(service.advanceSettlement(opened.id(), new ContractResolveDisputeRequest("k_a2", null)).status())
                .isEqualTo("arrears_collecting");
        ContractSettlementRecord done = service.advanceSettlement(opened.id(),
                new ContractResolveDisputeRequest("k_a3", null));
        assertThat(done.status()).isEqualTo("settled");
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("terminated");
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
    // ============ 合同经营范围（rent_terms.allowedCategories，2026-07-18） ============
    @Test
    void draftValidatesAndStoresAllowedCategories() {
        ContractRecord c = service.createDraft(new ContractDraftRequest("k_scope1", "mch_1", "subj_1", null, "subj_1",
                LocalDate.now().minusDays(1), LocalDate.now().plusYears(1), Map.of(),
                null, null, null, null, null, List.of(), null, "酒吧经营", null, List.of("beer", "snack")));
        assertThat(c.rentTerms()).containsEntry("allowedCategories", List.of("beer", "snack"));
        // 词表外取值（发明新枚举）→ 拒。
        assertThatThrownBy(() -> service.createDraft(new ContractDraftRequest("k_scope2", "mch_1", "subj_1", null,
                "subj_1", LocalDate.now().minusDays(1), LocalDate.now().plusYears(1), Map.of(),
                null, null, null, null, null, List.of(), null, null, null, List.of("metaverse"))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void draftRejectsVocabularyBypassEmbeddedInRentTerms() {
        // I-1: 直接把词表外 allowedCategories 塞进 rent_terms 整 Map（绕过专用字段）→ 同样拒；
        // 合法内嵌值（含客户端蛇形键）则归一化入库。
        assertThatThrownBy(() -> service.createDraft(new ContractDraftRequest("k_embed_bad", "mch_1", "subj_1",
                null, "subj_1", LocalDate.now().minusDays(1), LocalDate.now().plusYears(1),
                Map.of("allowedCategories", List.of("任意串")),
                null, null, null, null, null, List.of(), null, null, null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        ContractRecord ok = service.createDraft(new ContractDraftRequest("k_embed_ok", "mch_1", "subj_1",
                null, "subj_1", LocalDate.now().minusDays(1), LocalDate.now().plusYears(1),
                Map.of("allowed_categories", List.of("beer")),
                null, null, null, null, null, List.of(), null, null, null, null));
        assertThat(ok.rentTerms()).containsEntry("allowedCategories", List.of("beer"));
        assertThat(ok.rentTerms()).doesNotContainKey("allowed_categories");
    }

    @Test
    void businessScopeChangeUpdatesAllowedCategoriesAndGuardEnforcesAfterApproval() {
        // 变更后生效：走既有 business_scope_change 变更链，终审通过 payload 并入 rent_terms，拦截即读新范围。
        ContractRecord c = service.createDraft(new ContractDraftRequest("k_scope3", "mch_scope", "subj_1", null,
                "subj_1", LocalDate.now().minusDays(1), LocalDate.now().plusYears(1),
                Map.of("baseRentCent", 100000), "fixed_ratio", "by_area", null, null, null, List.of(), null,
                "酒吧经营", null, List.of("beer")));
        service.submit(c.id(), "k_scope3_submit");
        approve(c, "contract.approve.business");
        approve(c, "contract.approve.finance");
        approve(c, "contract.approve.legal");
        approve(c, "contract.approve.gm");
        service.activate(c.id(), new ContractActivateRequest("k_scope3_act", List.of("sp_scope")));

        ContractScopeGuard guard = new ContractScopeGuard(repo);
        // 变更前：只许 beer，卖 spirit 拦。
        assertThatThrownBy(() -> guard.requireBarSaleAllowed("mch_scope", List.of("spirit")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_SCOPE_VIOLATION"));
        // 发起经营范围变更（web 客户端把嵌套键蛇形化为 allowed_categories → 后端归一化）：改为 spirit+beer。
        ContractChangeRecord change = service.raiseChange(c.id(), new ContractChangeRequest(
                "k_scope3_ch", "business_scope_change", Map.of("allowed_categories", List.of("spirit", "beer"))));
        // 审批未全过 → 旧范围仍生效。
        approveChange(change.id(), "contract.approve.business");
        assertThatThrownBy(() -> guard.requireBarSaleAllowed("mch_scope", List.of("spirit")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_SCOPE_VIOLATION"));
        approveChange(change.id(), "contract.approve.finance");
        approveChange(change.id(), "contract.approve.legal");
        approveChange(change.id(), "contract.approve.gm");
        // 终审通过 → 新范围立即生效。
        assertThatCode(() -> guard.requireBarSaleAllowed("mch_scope", List.of("spirit")))
                .doesNotThrowAnyException();
        // 变更链里发明新枚举同样拒。
        assertThatThrownBy(() -> service.raiseChange(c.id(), new ContractChangeRequest(
                "k_scope3_bad", "business_scope_change", Map.of("allowed_categories", List.of("metaverse")))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    // ============ 退租结算落财务台账（move_out/early_termination 终审通过自动结算） ============
    private static final class RecordingTerminationPort
            implements com.complex.platform.finance.TerminationSettlementPort {
        Long depositRefundCent;
        Long penaltyCent;
        int depositCalls;
        int penaltyCalls;
        Long depositBalance;   // null=不封顶(返回固化额)；非 null=按此台账余额封顶

        @Override
        public long createDepositRefundPayable(String merchantId, long netRefundCent, String settlementId) {
            long eff = depositBalance == null ? netRefundCent : Math.min(netRefundCent, Math.max(0L, depositBalance));
            if (depositBalance != null) {
                depositBalance -= eff;   // 模拟 deposit_ledger refund 行扣减余额
            }
            depositRefundCent = eff;
            depositCalls++;
            return eff;
        }

        @Override
        public String createTerminationPenaltyReceivable(String merchantId, long penaltyCent, String settlementId) {
            this.penaltyCent = penaltyCent;
            penaltyCalls++;
            return "arap_pen_" + settlementId;
        }

        @Override
        public long effectiveRefundableDeposit(String merchantId, long contractDepositCent) {
            return depositBalance == null ? contractDepositCent : Math.min(contractDepositCent, depositBalance);
        }
    }

    private ContractRecord effectiveContractRecord(String id, long depositCent, Map<String, Object> penaltyTerms) {
        Instant now = Instant.now();
        return repo.saveContract(new ContractRecord(id, "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "by_area", depositCent, penaltyTerms, List.of(), Map.of(), null, List.of(),
                "effective", "proj_1", null, null, null, now, now));
    }

    private void approveTerminationChain(String changeId) {
        approveChange(changeId, "contract.approve.business");
        approveChange(changeId, "contract.approve.finance");
        approveChange(changeId, "contract.approve.legal");
        approveChange(changeId, "contract.approve.gm");
    }

    /** 镜像 ContractService.deterministicSuffix：SHA-256 前 48 hex。 */
    private static String sha48(String key) {
        try {
            byte[] d = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(d).substring(0, 48);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void moveOutTerminationAutoSettlesPostsDepositRefundAndTerminates() {
        ContractRecord c = effectiveContractRecord("ctr_t1", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(),
                new ContractChangeRequest("k_t1", "move_out", Map.of()));
        approveTerminationChain(ch.id());
        assertThat(terminationPort.depositCalls).isEqualTo(1);
        assertThat(terminationPort.depositRefundCent).isEqualTo(500000L);
        assertThat(terminationPort.penaltyCalls).isZero();
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("terminated");
        List<ContractSettlementRecord> sts = repo.listSettlementsByContract(c.id());
        assertThat(sts).hasSize(1);
        assertThat(sts.get(0).status()).isEqualTo("settled");
        assertThat(sts.get(0).triggerType()).isEqualTo("move_out");
    }

    @Test
    void earlyTerminationUsesContractPenaltyTermsWhenPresent() {
        ContractRecord c = effectiveContractRecord("ctr_t2", 0L, Map.of("amountCent", 200000));
        ContractChangeRecord ch = service.raiseChange(c.id(),
                new ContractChangeRequest("k_t2", "early_termination", Map.of("penaltyAmountCent", 999999)));
        approveTerminationChain(ch.id());
        assertThat(terminationPort.penaltyCalls).isEqualTo(1);
        assertThat(terminationPort.penaltyCent).isEqualTo(200000L);
    }

    @Test
    void earlyTerminationFallsBackToManualPenaltyFromPayload() {
        ContractRecord c = effectiveContractRecord("ctr_t3", 0L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(),
                new ContractChangeRequest("k_t3", "early_termination",
                        Map.of("penaltyAmountCent", 150000, "penaltyReason", "提前退租违约")));
        approveTerminationChain(ch.id());
        assertThat(terminationPort.penaltyCalls).isEqualTo(1);
        assertThat(terminationPort.penaltyCent).isEqualTo(150000L);
        ContractSettlementRecord st = repo.listSettlementsByContract(c.id()).get(0);
        Map<String, Object> pen = st.settlementItems().stream()
                .filter(i -> "termination_penalty".equals(i.get("item"))).findFirst().orElseThrow();
        assertThat(pen).containsEntry("reason", "提前退租违约").containsEntry("arApItemId", "arap_pen_" + st.id());
    }

    @Test
    void earlyTerminationParsesStringPenaltyAmount() {
        // S3：人工录入金额被序列化成字符串时不静默变 0。
        ContractRecord c = effectiveContractRecord("ctr_t3s", 0L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(),
                new ContractChangeRequest("k_t3s", "early_termination", Map.of("penaltyAmountCent", "120000")));
        approveTerminationChain(ch.id());
        assertThat(terminationPort.penaltyCalls).isEqualTo(1);
        assertThat(terminationPort.penaltyCent).isEqualTo(120000L);
    }

    @Test
    void unpaidReceivableOffsetsDepositNetPositivePayable() {
        repo.unpaidAr = 300000L;
        ContractRecord c = effectiveContractRecord("ctr_t4", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(),
                new ContractChangeRequest("k_t4", "move_out", Map.of()));
        approveTerminationChain(ch.id());
        assertThat(terminationPort.depositRefundCent).isEqualTo(200000L);
    }

    @Test
    void unpaidExceedsDepositPostsNoPayableMerchantStillOwes() {
        repo.unpaidAr = 800000L;
        ContractRecord c = effectiveContractRecord("ctr_t5", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(),
                new ContractChangeRequest("k_t5", "move_out", Map.of()));
        approveTerminationChain(ch.id());
        assertThat(terminationPort.depositCalls).isZero();
        ContractSettlementRecord st = repo.listSettlementsByContract(c.id()).get(0);
        Map<String, Object> dep = st.settlementItems().stream()
                .filter(i -> "deposit_disposal".equals(i.get("item"))).findFirst().orElseThrow();
        assertThat(dep).containsEntry("merchantStillOwesCent", 300000L).containsEntry("status", "listed_only");
    }

    @Test
    void depositRefundCappedByLedgerBalance() {
        // I3：台账实收余额 300000 < 固化 500000 → 只退 300000（封顶）。
        terminationPort.depositBalance = 300000L;
        ContractRecord c = effectiveContractRecord("ctr_t9", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(),
                new ContractChangeRequest("k_t9", "move_out", Map.of()));
        approveTerminationChain(ch.id());
        assertThat(terminationPort.depositRefundCent).isEqualTo(300000L);
        ContractSettlementRecord st = repo.listSettlementsByContract(c.id()).get(0);
        Map<String, Object> dep = st.settlementItems().stream()
                .filter(i -> "deposit_disposal".equals(i.get("item"))).findFirst().orElseThrow();
        assertThat(dep).containsEntry("depositCent", 300000L);
    }

    @Test
    void twoContractsSameMerchantCapByLedgerBalanceNoOverRefund() {
        // #3② 多合同同商户押金超退根治：退押金扣减台账余额，第二个合同按已减余额封顶。
        terminationPort.depositBalance = 4000L;   // 商户押金台账实收余额
        ContractRecord c1 = effectiveContractRecord("ctr_mc1", 2000L, Map.of());
        ContractRecord c2 = effectiveContractRecord("ctr_mc2", 3000L, Map.of());
        ContractChangeRecord ch1 = service.raiseChange(c1.id(),
                new ContractChangeRequest("k_mc1", "move_out", Map.of()));
        ContractChangeRecord ch2 = service.raiseChange(c2.id(),
                new ContractChangeRequest("k_mc2", "move_out", Map.of()));
        approveTerminationChain(ch1.id());
        long refund1 = terminationPort.depositRefundCent;
        approveTerminationChain(ch2.id());
        long refund2 = terminationPort.depositRefundCent;
        assertThat(refund1).isEqualTo(2000L);              // 固化 2000 ≤ 余额 4000
        assertThat(refund2).isEqualTo(2000L);              // 固化 3000 但余额只剩 2000 → 封顶 2000
        assertThat(refund1 + refund2).isEqualTo(4000L);    // 共退=实交，不超退
        assertThat(terminationPort.depositBalance).isZero();
    }

    @Test
    void twoTerminationChangesSettleOnlyOnceNoDoubleDepositRefund() {
        // I2：同合同两个退租变更并存，A 先审终止合同，B 审批完成但结算被守卫跳过 → 押金只退一次。
        ContractRecord c = effectiveContractRecord("ctr_t8", 500000L, Map.of());
        ContractChangeRecord a = service.raiseChange(c.id(),
                new ContractChangeRequest("k_t8a", "move_out", Map.of()));
        ContractChangeRecord b = service.raiseChange(c.id(),
                new ContractChangeRequest("k_t8b", "early_termination", Map.of("penaltyAmountCent", 100000)));
        approveTerminationChain(a.id());
        approveTerminationChain(b.id());
        assertThat(terminationPort.depositCalls).isEqualTo(1);
        assertThat(terminationPort.penaltyCalls).isZero();
        assertThat(repo.listSettlementsByContract(c.id())).hasSize(1);
    }

    @Test
    void terminationSettlementIsIdempotentAgainstReplay() {
        ContractRecord c = effectiveContractRecord("ctr_t6", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(),
                new ContractChangeRequest("k_t6", "move_out", Map.of()));
        String settlementId = "cst_" + sha48(ch.id());
        repo.saveSettlement(new ContractSettlementRecord(settlementId, c.id(), "move_out", "settled",
                List.of(), null, null, null, "proj_1", Instant.now(), Instant.now()));
        approveTerminationChain(ch.id());
        assertThat(terminationPort.depositCalls).isZero();
        assertThat(terminationPort.penaltyCalls).isZero();
    }

    @Test
    void rejectedTerminationChangeDoesNotSettle() {
        ContractRecord c = effectiveContractRecord("ctr_t7", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(),
                new ContractChangeRequest("k_t7", "early_termination", Map.of("penaltyAmountCent", 100000)));
        service.approveChange(ch.id(),
                new ContractChangeApproveRequest("k_t7_r", "contract.approve.business", "reject", "no"));
        assertThat(terminationPort.depositCalls).isZero();
        assertThat(terminationPort.penaltyCalls).isZero();
        assertThat(repo.listSettlementsByContract(c.id())).isEmpty();
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("effective");
    }

    private static final class InMemoryContractRepository implements ContractRepository {
        // 3.13 测试基建：草稿预绑定存储 + 可控"空间已被生效合同占用"集合。
        final java.util.Map<String, java.util.Set<String>> boundSpaces = new java.util.HashMap<>();
        final java.util.Set<String> occupiedSpaces = new java.util.HashSet<>();
        // 退租结算测试：可控未清应收合计。
        long unpaidAr = 0L;

        private final Map<String, ContractRecord> contracts = new LinkedHashMap<>();
        private final Map<String, ContractApprovalRecord> approvals = new LinkedHashMap<>();
        private final Map<String, ContractChangeRecord> changes = new LinkedHashMap<>();
        private final Map<String, ContractSettlementRecord> settlements = new LinkedHashMap<>();
        private final List<SpaceExpiryColorRuleRecord> colorRules = new ArrayList<>();
        private final List<ContractRentBillRecord> rentBills = new ArrayList<>();
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
            boundSpaces.computeIfAbsent(contractId, k -> new java.util.LinkedHashSet<>()).addAll(spaceIds);
        }

        @Override
        public List<String> listContractSpaceIds(String contractId) {
            return List.copyOf(boundSpaces.getOrDefault(contractId, java.util.Set.of()));
        }

        @Override
        public boolean hasOverlappingEffectiveContractForSpace(
                String spaceId, LocalDate effectiveDate, LocalDate expiryDate, String excludeContractId) {
            return occupiedSpaces.contains(spaceId);
        }

        @Override
        public int markContractsExpired(LocalDate asOf) {
            return 0;
        }

        @Override
        public List<ContractRecord> listEffectiveContractsExpiringBetween(
                LocalDate fromExclusive,
                LocalDate toInclusive
        ) {
            return contracts.values().stream()
                    .filter(c -> "effective".equals(c.lifecycleStatus()))
                    .filter(c -> c.expiryDate().isAfter(fromExclusive) && !c.expiryDate().isAfter(toInclusive))
                    .toList();
        }

        @Override
        public List<SpaceRentalStatusRecord> listSpaceRentalRaw(String projectId, LocalDate asOf) {
            return List.of();
        }

        @Override
        public boolean rentBillExists(String contractId, String period, String feeType) {
            return rentBills.stream().anyMatch(b -> b.contractId().equals(contractId) && b.period().equals(period)
                    && b.feeType().equals(feeType));
        }

        @Override
        public ContractRentBillRecord insertRentBill(ContractRentBillRecord bill, LocalDate dueDate) {
            rentBills.add(bill);
            return bill;
        }

        @Override
        public long sumUnpaidArForContract(String contractId) {
            return unpaidAr;
        }
    }
}
