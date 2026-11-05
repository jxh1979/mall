package com.complex.platform.contract;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.contract.dto.ContractApprovalFlowRequest;
import com.complex.platform.contract.dto.ContractChangeRequest;
import com.complex.platform.contract.dto.ContractDraftRequest;
import com.complex.platform.contract.dto.ContractResolveDisputeRequest;
import com.complex.platform.contract.dto.ContractSettleRequest;
import com.complex.platform.contract.dto.ContractSubmitRequest;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.dto.ArApPaymentRequest;
import com.complex.platform.governance.chain.ApprovalChainNode;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * HP-C01-R3（BSM L1349「主线与各条平行线都通过才生效；…任一节点驳回则整单退回起草」；PGM L536「主线与各条平行线全部通过合同才生效，
 * 任一节点驳回则整单退回起草…起草人与各审批人都可以查看该单据当前卡在哪一个节点上」；ENUM L255「三类都不得录入即生效——新建未审批不生效…
 * 变更未审批不生效」）：真实 PostgreSQL（Flyway 全量迁移，@Transactional 回滚）上走完整审批链——
 * 组织层级 起草人 D → 经理 M → 总经理 G；三条平行线各自 起点人 E → 该线最终审批人 F（approval_lines.entry_user_id + org_hierarchy）。
 * 提交指定主线最终审批人 G + 三线最终审批人 → supervisor_review_nodes 主线 2 节点 + 三条平行线各 2 节点；逐节点裁决，
 * 终态经 {@link ContractService#onReviewDecided} 回写合同/变更。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class ContractApprovalChainJdbcIntegrationTests {

    /** 平台默认「新建」模板的三条平行线（V494）。 */
    private static final List<String> LINE_CODES = List.of("business_review", "finance_review", "legal_review");

    @Autowired private ContractService contractService;
    @Autowired private ContractRepository repository;
    @Autowired private SupervisorReviewService reviews;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;
    @Autowired private ContractExpiryJob expiryJob;
    @Autowired private FinanceService financeService;
    @Autowired private com.complex.platform.analytics.AnalyticsRepository analytics;

    private String suffix;
    private String projectId;
    private String subject;
    private String drafter;
    private String manager;
    private String gm;
    private final Map<String, String> lineEntry = new LinkedHashMap<>();
    private final Map<String, String> lineFinal = new LinkedHashMap<>();

    @BeforeEach
    void seed() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        projectId = "hpc01_project_" + suffix;
        subject = "be_hpc01_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) "
                + "VALUES (?, 'complex', '审批链项目', 'active', now(), now())", projectId);
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) "
                + "VALUES (?, ?, 'management_company', 'active', now(), now())", subject, "审批链合同主体");
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES (?, ?)", subject, projectId);
        // 主线组织层级：起草人 → 经理 → 总经理。
        drafter = user("drafter");
        manager = user("manager");
        gm = user("gm");
        superior(drafter, manager);
        superior(manager, gm);
        // 三条平行线：起点人 → 该线最终审批人；起点人写进 approval_lines.entry_user_id（事务内改、用例结束回滚）。
        for (String code : LINE_CODES) {
            String entry = user(code + "_entry");
            String fin = user(code + "_final");
            superior(entry, fin);
            jdbc.update("UPDATE approval_lines SET entry_user_id = ? WHERE line_code = ?", entry, code);
            lineEntry.put(code, entry);
            lineFinal.put(code, fin);
        }
        // 起草人的显式角色来源：发起变更 + 维护本项目模板 + 齐单核对 + 登记收付款（requirePermission 逐码校验）。
        String role = "role_hpc01_" + suffix;
        jdbc.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", role, role);
        for (String permission : List.of("contract.change.manage", "contract.approval.flow.manage", "contract.settlement.manage",
                "finance.payment.register")) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, permission);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", drafter, role);
        actAs(drafter);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** 主线 2 节点 + 三平行线各 2 节点；逐级裁决；主线全过而平行线未过仍待确认；最后一个节点通过那一刻才待生效。 */
    @Test
    void contractBecomesPendingEffectiveOnlyAfterMainLineAndEveryParallelLineApprove() {
        ContractRecord submitted = submit(draft("c1"), "s1");
        String contractId = submitted.id();
        String reviewId = submitted.reviewId();
        assertThat(submitted.lifecycleStatus()).isEqualTo("pending_confirmation");
        assertThat(reviewId).isNotNull();
        assertThat(jdbc.queryForObject("SELECT review_id FROM contracts WHERE id = ?", String.class, contractId)).isEqualTo(reviewId);

        List<ApprovalChainNode> nodes = reviews.chain(reviewId);
        assertThat(nodes).hasSize(8);
        assertThat(approvers(nodes, ApprovalChainNode.MAIN_LINE)).containsExactly(manager, gm);
        for (String code : LINE_CODES) {
            assertThat(approvers(nodes, code)).as(code).containsExactly(lineEntry.get(code), lineFinal.get(code));
        }
        assertThat(nodes).extracting(ApprovalChainNode::approverId).doesNotContain(drafter);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM supervisor_review_nodes WHERE review_id = ? AND status = 'pending'",
                Integer.class, reviewId)).isEqualTo(8);

        // 逐级：总经理在经理之前裁决不了自己的节点。
        actAs(gm);
        assertThatThrownBy(() -> reviews.approve(reviewId, approval("approved")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("APPROVAL_NODE_NOT_REACHED"));
        // 主线全过、平行线未过：合同仍待确认、复核单仍待审。
        approve(reviewId, manager);
        approve(reviewId, gm);
        assertThat(contractStatus(contractId)).isEqualTo("pending_confirmation");
        assertThat(reviewStatus(reviewId)).isEqualTo("pending_review");
        // 三条平行线逐节点通过：最后一个节点通过前合同都不动。
        List<String> remaining = new ArrayList<>();
        for (String code : LINE_CODES) {
            remaining.add(lineEntry.get(code));
            remaining.add(lineFinal.get(code));
        }
        for (int i = 0; i < remaining.size(); i++) {
            approve(reviewId, remaining.get(i));
            boolean last = i == remaining.size() - 1;
            assertThat(contractStatus(contractId)).as("第 %d 个平行线节点通过后", i + 1)
                    .isEqualTo(last ? "pending_effective" : "pending_confirmation");
        }
        assertThat(reviewStatus(reviewId)).isEqualTo("approved");
        assertThat(auditCount("contract.review_approved", contractId)).isEqualTo(1);
    }

    /** 任一节点驳回 → 整单 rejected、合同退回起草、其余待审节点作废；退回起草后可重新提交（开新复核单）。 */
    @Test
    void anyNodeRejectionReturnsContractToDraftingAndCancelsRemainingNodes() {
        ContractRecord submitted = submit(draft("c2"), "s2");
        String contractId = submitted.id();
        String reviewId = submitted.reviewId();
        approve(reviewId, manager);
        approve(reviewId, lineEntry.get("finance_review"));
        actAs(lineFinal.get("finance_review"));
        reviews.approve(reviewId, approval("rejected"));

        assertThat(reviewStatus(reviewId)).isEqualTo("rejected");
        assertThat(contractStatus(contractId)).isEqualTo("drafting");
        assertThat(nodeStatusCounts(reviewId))
                .containsEntry("approved", 2).containsEntry("rejected", 1).containsEntry("cancelled", 5)
                .doesNotContainKey("pending");
        assertThat(auditCount("contract.review_rejected", contractId)).isEqualTo(1);
        assertThat(auditCount("contract.review_approved", contractId)).isZero();

        actAs(drafter);
        ContractRecord again = contractService.submit(contractId, submitRequest("s2b"));
        assertThat(again.lifecycleStatus()).isEqualTo("pending_confirmation");
        assertThat(again.reviewId()).isNotNull().isNotEqualTo(reviewId);
        assertThat(reviews.chain(again.reviewId())).hasSize(8);
    }

    /** 起草人不在链上，也不能裁决自己发起的合同复核单。 */
    @Test
    void drafterCannotDecideOwnContractReview() {
        ContractRecord submitted = submit(draft("c3"), "s3");
        assertThatThrownBy(() -> reviews.approve(submitted.reviewId(), approval("approved")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        assertThat(contractStatus(submitted.id())).isEqualTo("pending_confirmation");
        assertThat(nodeStatusCounts(submitted.reviewId())).containsEntry("pending", 8).hasSize(1);
    }

    /**
     * 同项目「变更」模板只含业务审核线且默认最终审批人为总经理时：raiseChange 只给一条业务线即可生成链（主线 2 + 业务线 2），
     * 模板外的线拒绝；整单通过 → 变更 approved、条款应用到合同。
     */
    @Test
    void projectChangeTemplateWithOnlyBusinessLineNeedsThatLineAlone() {
        contractService.saveApprovalFlow(new ContractApprovalFlowRequest(key("tpl"), projectId, "change", "本项目·变更",
                List.of("business_review"), gm, null, null));
        ContractRecord contract = draft("c4");
        // 变更只对生效中的合同发起；生效（activate 绑铺位）不在本条范围，直接落生效态。
        jdbc.update("UPDATE contracts SET lifecycle_status = 'effective' WHERE id = ?", contract.id());

        assertThatThrownBy(() -> contractService.raiseChange(contract.id(), new ContractChangeRequest(key("chx"), "rent_change",
                Map.of("baseRentCent", 120000), null, List.of(line("business_review"), line("finance_review")), "调租")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CONTRACT_APPROVAL_LINE_NOT_IN_TEMPLATE"));

        ContractChangeRecord change = contractService.raiseChange(contract.id(), new ContractChangeRequest(key("ch"), "rent_change",
                Map.of("baseRentCent", 120000), null, List.of(line("business_review")), "调租"));
        assertThat(change.approvalStatus()).isEqualTo("pending");
        List<ApprovalChainNode> nodes = reviews.chain(change.reviewId());
        assertThat(nodes).hasSize(4);
        assertThat(approvers(nodes, ApprovalChainNode.MAIN_LINE)).containsExactly(manager, gm);
        assertThat(approvers(nodes, "business_review")).containsExactly(lineEntry.get("business_review"), lineFinal.get("business_review"));
        assertThat(nodes).extracting(ApprovalChainNode::lineCode).doesNotContain("finance_review", "legal_review");

        for (String approver : List.of(manager, gm, lineEntry.get("business_review"))) {
            approve(change.reviewId(), approver);
            assertThat(repository.findChangeById(change.id()).orElseThrow().approvalStatus()).isEqualTo("pending");
            assertThat(repository.findContractById(contract.id()).orElseThrow().rentTerms()).containsEntry("baseRentCent", 100000);
        }
        approve(change.reviewId(), lineFinal.get("business_review"));
        assertThat(reviewStatus(change.reviewId())).isEqualTo("approved");
        assertThat(repository.findChangeById(change.id()).orElseThrow().approvalStatus()).isEqualTo("approved");
        assertThat(repository.findContractById(contract.id()).orElseThrow().rentTerms()).containsEntry("baseRentCent", 120000);
        assertThat(auditCount("contract.change_approved", change.id())).isEqualTo(1);
    }

    /**
     * HP-C01-R5（BSM L1349「阶段数与审批岗位按综合体后台可配（业务参数，按综合体可配，须同时配置上下限）」；2026-10-18 裁决第 1 条
     * 「主线层级数配上下限……发起时校验：补全后层级数不在上下限内即拒绝」）：主线补全后 2 层（经理→总经理）。
     * 本项目「新建」模板未配置上下限 → 不校验、通过；最多 1 → 超上限拒绝；最少 3 → 低于下限拒绝（提示写清补全后层数与上下限，合同仍草拟中）；
     * 2～3 → 通过。
     */
    @Test
    void submitChecksCompletedMainLineLevelsAgainstTemplateBounds() {
        newTemplate(null, null);
        assertThat(submit(draft("c5a"), "s5a").lifecycleStatus()).isEqualTo("pending_confirmation");

        ContractRecord over = draft("c5b");
        newTemplate(1, 1);
        assertThatThrownBy(() -> submit(over, "s5b"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("CONTRACT_APPROVAL_LEVELS_OUT_OF_RANGE");
                    assertThat(e.getMessage()).contains("补全后为 2 层", "1～1");
                });
        assertThat(contractStatus(over.id())).isEqualTo("drafting");
        newTemplate(3, 5);
        assertThatThrownBy(() -> submit(over, "s5c"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("CONTRACT_APPROVAL_LEVELS_OUT_OF_RANGE");
                    assertThat(e.getMessage()).contains("补全后为 2 层", "3～5");
                });
        assertThat(contractStatus(over.id())).isEqualTo("drafting");
        newTemplate(2, 3);
        ContractRecord ok = submit(over, "s5d");
        assertThat(ok.lifecycleStatus()).isEqualTo("pending_confirmation");
        assertThat(approvers(reviews.chain(ok.reviewId()), ApprovalChainNode.MAIN_LINE)).containsExactly(manager, gm);
    }

    /**
     * HP-C02-R6（2026-10-18 裁决第 2 条「发起退租/提前终止变更时必填「退租日」，期间＝生效月→退租日所在月……齐单核对、缺单判定、
     * 欠费口径都按这个截止月」）：合同 2026-08 起租、退租日 2026-08-20——8 月租金未出账即缺单（不列 9 月）；补出 8 月租金后
     * 9 月账单缺失不算缺单，齐单核对通过。
     */
    @Test
    void moveOutDateMonthBoundsDocumentsCheck() {
        ContractRecord contract = draft("c6");
        jdbc.update("UPDATE contracts SET lifecycle_status = 'effective' WHERE id = ?", contract.id());
        contractService.raiseChange(contract.id(), new ContractChangeRequest(key("ch6"), "move_out",
                Map.of("moveOutDate", "2026-08-20"), gm, LINE_CODES.stream().map(this::line).toList(), "退租"));
        ContractSettlementRecord settlement = repository.listSettlementsByContract(contract.id()).get(0);

        assertThatThrownBy(() -> contractService.checkSettlementDocuments(settlement.id(),
                new ContractResolveDisputeRequest(key("dc6a"), null)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE");
                    assertThat(e.getMessage()).contains("2026-08:rent").doesNotContain("2026-09");
                });
        repository.insertRentBill(new ContractRentBillRecord("crb_hpc01_08_" + suffix, contract.id(), contract.merchantId(),
                projectId, "2026-08", "rent", 100000L, "arap_hpc01_08_" + suffix, Instant.now(), null), LocalDate.parse("2026-08-01"));
        assertThatCode(() -> contractService.checkSettlementDocuments(settlement.id(),
                new ContractResolveDisputeRequest(key("dc6b"), "8 月单据已核对"))).doesNotThrowAnyException();
        assertThat(repository.findSettlementById(settlement.id()).orElseThrow().documentsCheckedBy()).isEqualTo(drafter);
    }

    /**
     * HP-C02（BSM L1352「退租、提前终止、合同到期等各种终止结算，都要经过多级审批……审批全部通过后，系统按算好的结算清单自动落账，不再单设手动确认入口」；
     * ENUM L255「合同审批链按新建、到期终止、变更三类分别配置流程模板……到期不得无审批自动终止」）：到期结算开账即按「到期终止」类模板开复核单
     * （主线 2 节点＋三线各 2 节点），review_id 落结算单；最后一个节点通过前结算单都不动，最后一个节点通过那一刻落账、合同到期
     * （8 月租金未收、无押金实收 → 押金不足抵扣 → 欠费追缴中，第三轮裁决第 4 条「按押金够不够分流」）。
     */
    @Test
    void expirySettlementPostsOnlyWhenExpiryTerminationChainFullyApproves() {
        ContractRecord contract = draft("c7");
        jdbc.update("UPDATE contracts SET lifecycle_status = 'expired', expiry_date = DATE '2026-08-31' WHERE id = ?", contract.id());
        rentBill(contract, "2026-08");
        ContractSettlementRecord opened = contractService.settle(contract.id(), new ContractSettleRequest(key("st7"), "expiry",
                null, gm, LINE_CODES.stream().map(this::line).toList(), "到期结算"));
        String reviewId = opened.reviewId();
        assertThat(reviewId).isNotNull();
        assertThat(jdbc.queryForObject("SELECT review_id FROM contract_settlements WHERE id = ?", String.class, opened.id()))
                .isEqualTo(reviewId);
        assertThat(jdbc.queryForObject("SELECT action_type FROM supervisor_review_decisions WHERE review_id = ?", String.class,
                reviewId)).isEqualTo("contract.expiry_termination");
        contractService.checkSettlementDocuments(opened.id(), new ContractResolveDisputeRequest(key("dc7"), null));
        List<ApprovalChainNode> nodes = reviews.chain(reviewId);
        assertThat(nodes).hasSize(8);
        for (int i = 0; i < nodes.size(); i++) {
            approve(reviewId, nodes.get(i).approverId());
            boolean last = i == nodes.size() - 1;
            assertThat(settlementStatus(opened.id())).as("第 %d 个节点通过后", i + 1)
                    .isEqualTo(last ? "arrears_collecting" : "pending_settlement");
        }
        assertThat(contractStatus(contract.id())).isEqualTo("expired");
        assertThat(auditCount("contract.settlement_completed", opened.id())).isEqualTo(1);
    }

    /**
     * HP-C02（ENUM L255「到期不得无审批自动终止」；BSM L1352「退租对应期间的全部相关单据收齐并核对完成后，才允许退租结算最后审批通过」「有争议进「有争议」
     * 状态挂起、由主管裁决后继续」）：退租变更审批期间合同期满、到期作业跑过 → 合同仍生效；终审照常过闸——齐单的落账终止，商户异议挂起的最后一个节点
     * 409 CONTRACT_SETTLEMENT_DISPUTED、结算单与合同不动。
     */
    @Test
    void terminationApprovedAfterExpiryJobStillPassesGatesAndPosts() {
        ContractChangeRecord ok = moveOutRaisedAndChecked("c8");
        ContractChangeRecord held = moveOutRaisedAndChecked("c9");
        expiryJob.runOnce(LocalDate.parse("2026-09-02"));
        assertThat(contractStatus(ok.contractId())).isEqualTo("effective");
        assertThat(contractStatus(held.contractId())).isEqualTo("effective");

        for (ApprovalChainNode node : reviews.chain(ok.reviewId())) {
            approve(ok.reviewId(), node.approverId());
        }
        assertThat(contractStatus(ok.contractId())).isEqualTo("terminated");
        assertThat(repository.listSettlementsByContract(ok.contractId())).extracting(ContractSettlementRecord::status)
                .containsExactly("arrears_collecting");   // 未收租金、无押金实收 → 押金不足抵扣（第三轮裁决第 4 条）

        ContractSettlementRecord st = repository.listSettlementsByContract(held.contractId()).get(0);
        repository.saveSettlement(st.withDispute("disputed", st.settlementItems(), "商户：违约金多算", null, null, null, Instant.now()));
        List<ApprovalChainNode> nodes = reviews.chain(held.reviewId());
        for (int i = 0; i < nodes.size() - 1; i++) {
            approve(held.reviewId(), nodes.get(i).approverId());
        }
        String lastApprover = nodes.get(nodes.size() - 1).approverId();
        assertThatThrownBy(() -> approve(held.reviewId(), lastApprover))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CONTRACT_SETTLEMENT_DISPUTED"));
        assertThat(settlementStatus(st.id())).isEqualTo("disputed");
        assertThat(contractStatus(held.contractId())).isEqualTo("effective");
    }

    /**
     * 第四轮裁决第 3 条（财务结算 PRD L957「终止结算既有多级审批全部通过后，系统按清单自动落账，使相应应收未付额与押金余额等额减少并可对账」）＋第 1 条
     * 「押金先抵欠费加违约金，剩余才退」真库端到端：押金实收 300000、退租日所在月租金应收 100000 未收、提前终止违约金 50000 → 最后一个节点通过即：
     * 租金与违约金应收以「押金抵扣」（deposit_offset，流水号＝结算单 id）全额销账，deposit_ledger 抵扣行 150000（余额 150000）＋退还行 150000（余额 0），
     * 押金退还应付 150000，押金处置项回写两笔销账 → 押金抵扣中；终审事件重放不重复销账；退还付清 → 已结清。
     */
    @Test
    void depositCoversArrearsAndPenaltyWritesOffLedgerThenRefundSettles() {
        ContractChangeRecord change = terminationWithDeposit("c10", 300000L, "early_termination", Map.of("penaltyAmountCent", 50000));
        approveAll(change.reviewId());
        String sid = repository.listSettlementsByContract(change.contractId()).get(0).id();
        String rentItem = rentItemId(change.contractId());
        String penaltyItem = jdbc.queryForObject(
                "SELECT id FROM ar_ap_items WHERE source_type = 'termination_penalty' AND source_id = ?", String.class, sid);
        assertThat(settlementStatus(sid)).isEqualTo("deposit_offsetting");
        assertThat(itemState(rentItem)).isEqualTo("settled:0");
        assertThat(itemState(penaltyItem)).isEqualTo("settled:0");
        assertThat(offsetPayments(sid)).containsExactly(rentItem + ":100000", penaltyItem + ":50000");
        assertThat(depositLedger(change)).containsExactly("collect:300000:300000", "deduct:150000:150000", "refund:150000:0");
        String refundItem = jdbc.queryForObject(
                "SELECT id FROM ar_ap_items WHERE source_type = 'deposit_refund' AND source_id = ?", String.class, sid);
        assertThat(itemState(refundItem)).isEqualTo("pending_payment:150000");
        assertThat(settlementOffsets(sid)).containsExactly(rentItem + ":100000", penaltyItem + ":50000");

        contractService.onReviewDecided(reviews.findReview(change.reviewId()).orElseThrow());   // 终审事件重放
        assertThat(offsetPayments(sid)).hasSize(2);
        assertThat(depositLedger(change)).hasSize(3);

        actAs(drafter);
        financeService.registerArApPayment(refundItem, payment("p10", 150000L));
        assertThat(settlementStatus(sid)).isEqualTo("settled");
    }

    /**
     * 押金不足（第四轮裁决第 3 条「欠费追缴中」的收齐判定随之改为看台账未收额；第三轮第 4 条「押金不足抵扣的→「欠费追缴中」，差额收齐后→「已结清」」）
     * 真库端到端：押金实收 80000、退租日所在月租金应收 100000 → 被押金覆盖的 80000 以押金抵扣销账、台账剩 20000 未收，抵扣行 80000（余额 0）、
     * 不挂押金退还 → 欠费追缴中（销账回调不把结算单误推进）；部分收 10000 不转，收齐 → 已结清。
     */
    @Test
    void depositShortfallLeavesRemainderUnpaidUntilCollected() {
        ContractChangeRecord change = terminationWithDeposit("c11", 80000L, "move_out", Map.of());
        approveAll(change.reviewId());
        String sid = repository.listSettlementsByContract(change.contractId()).get(0).id();
        String rentItem = rentItemId(change.contractId());
        assertThat(settlementStatus(sid)).isEqualTo("arrears_collecting");
        assertThat(itemState(rentItem)).isEqualTo("partial_paid:20000");
        assertThat(offsetPayments(sid)).containsExactly(rentItem + ":80000");
        assertThat(depositLedger(change)).containsExactly("collect:80000:80000", "deduct:80000:0");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_type = 'deposit_refund' AND source_id = ?",
                Integer.class, sid)).isZero();

        actAs(drafter);
        financeService.registerArApPayment(rentItem, payment("p11a", 10000L));
        assertThat(settlementStatus(sid)).isEqualTo("arrears_collecting");
        financeService.registerArApPayment(rentItem, payment("p11b", 10000L));
        assertThat(settlementStatus(sid)).isEqualTo("settled");
    }

    /**
     * 第四轮裁决第 3 条「终审落账时把被押金覆盖的应收逐笔以「押金抵扣」销账」（财务结算 PRD L957「终止结算既有多级审批全部通过后，系统按清单自动落账……
     * 不另设独立抵扣审批或手动确认入口」；BSM L2023「日结锁定后不能直接补录影响金额，必须上级审批通过后补录」——终止结算的多级审批即该上级审批）：
     * 退租月租金应收的到期日已做日结、台账项被日结锁定 → 最后一个节点照常通过，押金抵扣销账落到该应收（未收额与押金余额等额减少），不因 DAY_CLOSE_LOCKED 整笔回滚。
     */
    @Test
    void depositOffsetWritesOffDayCloseLockedArrearsAtFinalApproval() {
        ContractChangeRecord change = terminationWithDeposit("c12", 300000L, "move_out", Map.of());
        String rentItem = rentItemId(change.contractId());
        String close = "fdc_" + key("c12");
        jdbc.update("INSERT INTO finance_daily_closes(id,business_date,scope_type,scope_id,status,locked_at,created_at)"
                + " VALUES (?, DATE '2026-08-01', 'project', ?, 'locked', now(), now())", close, projectId);
        jdbc.update("UPDATE ar_ap_items SET locked_day_close_id = ? WHERE id = ?", close, rentItem);
        // 第五轮裁决第 1 条「金额归入当期、不改原日结金额」：原日结记录与汇总、原台账项的金额与锁在终审前后不变；抵扣收款流水记在终审当时（当期）。
        jdbc.update("INSERT INTO finance_daily_close_summaries (id, daily_close_id, source_stream, amount_cent, refund_cent, order_count)"
                + " VALUES (?, ?, 'bar', 880000, 1000, 9)", "fdcs_" + key("c12"), close);
        List<String> closeBefore = dailyCloseFacts(close);
        Instant beforeApproval = Instant.now();

        assertThatCode(() -> approveAll(change.reviewId())).doesNotThrowAnyException();
        String sid = repository.listSettlementsByContract(change.contractId()).get(0).id();
        assertThat(settlementStatus(sid)).isEqualTo("deposit_offsetting");
        assertThat(itemState(rentItem)).isEqualTo("settled:0");
        assertThat(offsetPayments(sid)).containsExactly(rentItem + ":100000");
        assertThat(depositLedger(change)).containsExactly("collect:300000:300000", "deduct:100000:200000", "refund:200000:0");
        assertThat(dailyCloseFacts(close)).isEqualTo(closeBefore);
        assertThat(jdbc.queryForObject("SELECT amount_cent || ':' || locked_day_close_id FROM ar_ap_items WHERE id = ?", String.class,
                rentItem)).isEqualTo("100000:" + close);
        assertThat(jdbc.queryForObject("SELECT payment_time FROM ar_ap_payments WHERE payment_method = 'deposit_offset' AND bank_flow_ref = ?",
                java.sql.Timestamp.class, sid).toInstant()).isAfterOrEqualTo(beforeApproval.minusSeconds(1));
    }

    /**
     * 第五轮裁决第 2 条「租金调整生效时，作废自生效月起尚未收款的预排租金应收（留痕），按调整后月租重排剩余租期」＋第 1 条「变更的审批全部通过即视同上级审批，
     * 系统对已锁定应收照常……作废，金额归入当期、不改原日结金额。人工登记收款仍受锁定拦截」（BSM L2023「日结锁定后不能直接补录影响金额，必须上级审批通过后补录」；
     * ENUM L190「日结锁定后的补录须上级审批，且不改动已锁定的日结金额：补录金额归入当期」）——真库、web 端蛇形载荷 base_rent_cent：
     * 合同上月、终审当月、次月各有一张未收租金账单，上月与当月账单的到期日已日结锁定。全链终审 → 当月与次月账单作废（原因含变更 id），同一合同按 150000 重排这两期
     * （重排应收与被作废的原行同合同同期，不撞唯一键）；冲正行不继承原行锁，锁定原行的冲正行业务日期（到期日）取作废当日、未锁原行的冲正行沿用原到期日；
     * 原行金额与锁不动，日结记录与汇总金额不变；上月锁定账单不在重排范围，人工登记收款仍 409 DAY_CLOSE_LOCKED、台账不动。
     */
    @Test
    void rentChangeVoidsDayCloseLockedRentAndRebillsSameContractWithoutTouchingDailyClose() {
        java.time.YearMonth now = java.time.YearMonth.now(com.complex.platform.common.LocalZone.ID);
        LocalDate today = LocalDate.now(com.complex.platform.common.LocalZone.ID);
        ContractRecord contract = draft("c14");
        jdbc.update("UPDATE contracts SET lifecycle_status = 'effective', effective_date = ?, expiry_date = ? WHERE id = ?",
                now.minusMonths(1).atDay(1), now.plusMonths(1).atEndOfMonth(), contract.id());
        Map<java.time.YearMonth, String> items = new LinkedHashMap<>();
        for (java.time.YearMonth ym : List.of(now.minusMonths(1), now, now.plusMonths(1))) {
            rentBill(contract, ym.toString());
            items.put(ym, rentItemId(contract.id(), ym.toString()));
        }
        Map<java.time.YearMonth, String> closes = new LinkedHashMap<>();
        for (java.time.YearMonth ym : List.of(now.minusMonths(1), now)) {
            String close = "fdc_" + key("c14_" + ym);
            jdbc.update("INSERT INTO finance_daily_closes(id,business_date,scope_type,scope_id,status,locked_at,created_at)"
                    + " VALUES (?, ?, 'project', ?, 'locked', now(), now())", close, ym.atDay(1), projectId);
            jdbc.update("INSERT INTO finance_daily_close_summaries (id, daily_close_id, source_stream, amount_cent, refund_cent, order_count)"
                    + " VALUES (?, ?, 'bar', 880000, 1000, 9)", "fdcs_" + key("c14_" + ym), close);
            jdbc.update("UPDATE ar_ap_items SET locked_day_close_id = ? WHERE id = ?", close, items.get(ym));
            closes.put(ym, close);
        }
        List<String> closeBefore = dailyCloseFacts(closes.get(now));

        ContractChangeRecord change = contractService.raiseChange(contract.id(), new ContractChangeRequest(key("rc_c14"), "rent_change",
                Map.of("base_rent_cent", 150000), gm, LINE_CODES.stream().map(this::line).toList(), "租金调整"));
        approveAll(change.reviewId());

        assertThat(repository.findChangeById(change.id()).orElseThrow().approvalStatus()).isEqualTo("approved");
        assertThat(repository.findContractById(contract.id()).orElseThrow().rentTerms()).containsEntry("baseRentCent", 150000)
                .doesNotContainKey("base_rent_cent");
        assertThat(jdbc.queryForList("SELECT period || ':' || amount_cent || ':' || COALESCE(void_reason, 'live') FROM contract_rent_bills"
                + " WHERE contract_id = ? ORDER BY period, voided_at IS NULL", String.class, contract.id())).containsExactly(
                now.minusMonths(1) + ":100000:live",
                now + ":100000:租金调整 " + change.id() + " 生效重排", now + ":150000:live",
                now.plusMonths(1) + ":100000:租金调整 " + change.id() + " 生效重排", now.plusMonths(1) + ":150000:live");
        assertThat(jdbc.queryForObject("SELECT (locked_day_close_id IS NULL) || ':' || due_date || ':' || amount_cent FROM ar_ap_items"
                + " WHERE origin_item_id = ?", String.class, items.get(now))).isEqualTo("true:" + today + ":-100000");
        assertThat(jdbc.queryForObject("SELECT (locked_day_close_id IS NULL) || ':' || due_date || ':' || amount_cent FROM ar_ap_items"
                + " WHERE origin_item_id = ?", String.class, items.get(now.plusMonths(1))))
                .isEqualTo("true:" + now.plusMonths(1).atDay(1) + ":-100000");
        // 「金额归入当期」：锁定原行的冲正行与替代它的重排应收同记作废当日（同日相抵）、重排应收不带锁；未锁期的重排应收与其冲正行同记期首；
        // 已锁定的期首营业日不新增台账行（终审当日恰为 1 日时三行同日，不适用）。
        assertThat(liveRentItemFacts(contract.id(), now)).isEqualTo("true:" + today + ":150000");
        assertThat(liveRentItemFacts(contract.id(), now.plusMonths(1))).isEqualTo("true:" + now.plusMonths(1).atDay(1) + ":150000");
        if (!today.equals(now.atDay(1))) {
            assertThat(jdbc.queryForList("SELECT id FROM ar_ap_items WHERE merchant_id = ? AND due_date = ?", String.class,
                    contract.merchantId(), now.atDay(1))).containsExactly(items.get(now));
        }
        assertThat(jdbc.queryForObject("SELECT status || ':' || amount_cent || ':' || locked_day_close_id FROM ar_ap_items WHERE id = ?",
                String.class, items.get(now))).isEqualTo("pending_collection:100000:" + closes.get(now));
        assertThat(dailyCloseFacts(closes.get(now))).isEqualTo(closeBefore);
        assertThat(repository.sumUnpaidArForContract(contract.id(), null)).isEqualTo(100000L + 150000L + 150000L);

        actAs(drafter);
        assertThatThrownBy(() -> financeService.registerArApPayment(items.get(now.minusMonths(1)), payment("p14", 100000L)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DAY_CLOSE_LOCKED"));
        assertThat(itemState(items.get(now.minusMonths(1)))).isEqualTo("pending_collection:100000");
    }

    /**
     * raiseChange 既有规则「只有生效中的合同才能变更」（409 CONTRACT_NOT_EFFECTIVE）在终审应用时复核；BSM L1350「绑定铺位（防止同一铺位重复出租）」——真库、web 蛇形载荷：
     * 同一生效合同同时挂面积变更与租金调整两张待审变更；面积变更先全链终审（父合同置已变更、派生子合同、作废并重排）→ 租金调整最后一个节点通过即
     * 409 CONTRACT_NOT_EFFECTIVE（终审整笔回滚、由审批人驳回）：父合同仍为已变更、不复活，租金调整仍待审，父子合同账单不新增、不再作废。
     */
    @Test
    void rentChangeFinalApprovalAfterAreaChangeIsRejectedWithoutRevivingParent() {
        java.time.YearMonth now = java.time.YearMonth.now(com.complex.platform.common.LocalZone.ID);
        ContractRecord contract = draft("c15");
        jdbc.update("UPDATE contracts SET lifecycle_status = 'effective', snapshot = CAST(? AS jsonb) WHERE id = ?",
                "{\"depositMonths\": 3}", contract.id());
        rentBill(contract, now.toString());
        ContractChangeRecord area = contractService.raiseChange(contract.id(), new ContractChangeRequest(key("ac_c15"), "area_change",
                Map.of("base_rent_cent", 100000, "deposit_deadline", now.plusMonths(1).atDay(1).toString()), gm,
                LINE_CODES.stream().map(this::line).toList(), "面积变更"));
        ContractChangeRecord rent = contractService.raiseChange(contract.id(), new ContractChangeRequest(key("rc_c15"), "rent_change",
                Map.of("base_rent_cent", 150000), gm, LINE_CODES.stream().map(this::line).toList(), "租金调整"));
        approveAll(area.reviewId());
        assertThat(contractStatus(contract.id())).isEqualTo("amended");
        String billsSql = "SELECT contract_id || ':' || period || ':' || fee_type || ':' || amount_cent || ':' || (voided_at IS NULL)"
                + " FROM contract_rent_bills WHERE merchant_id = ? ORDER BY 1";
        List<String> billsBefore = jdbc.queryForList(billsSql, String.class, contract.merchantId());

        List<ApprovalChainNode> nodes = reviews.chain(rent.reviewId());
        for (ApprovalChainNode node : nodes.subList(0, nodes.size() - 1)) {
            approve(rent.reviewId(), node.approverId());
        }
        assertThatThrownBy(() -> approve(rent.reviewId(), nodes.get(nodes.size() - 1).approverId()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CONTRACT_NOT_EFFECTIVE"));
        assertThat(contractStatus(contract.id())).isEqualTo("amended");
        assertThat(repository.findChangeById(rent.id()).orElseThrow().approvalStatus()).isEqualTo("pending");
        assertThat(jdbc.queryForList(billsSql, String.class, contract.merchantId())).isEqualTo(billsBefore);
    }

    /**
     * 第四轮裁决第 2 条「作废父合同自变更生效月起尚未收款的预排租金/物业费应收（留痕）」；BSM L1353「不将未来应收视为到账，也不覆盖原收款历史」——真库口径：
     * 作废（追加冲正行、原行不动）后的原应收在财务侧不再是有效欠款：分红欠款抵扣候选、账龄分桶、逾期欠款商户榜、逾期应收明细（含 90 天以上）都不计它；
     * 对它登记收款 → 409 AR_AP_ITEM_REVERSED、原行不动。
     */
    @Test
    void voidedRentBillOriginalIsNeitherOutstandingNorPayable() {
        ContractRecord contract = draft("c13");
        rentBill(contract, "2026-08");
        String original = rentItemId(contract.id());
        jdbc.update("UPDATE ar_ap_items SET project_id = ? WHERE id = ?", projectId, original);
        assertThat(repository.voidUnpaidRentBills(List.of(contract.id()), "2026-08", List.of("rent"), "面积/位置变更 cch_c13 生效重排"))
                .hasSize(1);
        LocalDate asOf = LocalDate.parse("2027-01-01");
        String merchant = contract.merchantId();

        assertThat(financeService.listOutstandingReceivables(merchant)).isEmpty();
        assertThat(analytics.arOverdueAgingByMerchant(asOf).values()).allSatisfy(bucket -> assertThat(bucket).doesNotContainKey(merchant));
        assertThat(analytics.listOverdueDebtors(asOf, null, 50, 0, Set.of(projectId))).isEmpty();
        assertThat(analytics.arOpenReceivableDetail(asOf, 50, 0, Set.of(projectId)).rows()).isEmpty();
        assertThat(analytics.arOverdueAging90pDetail(asOf, 50, 0, Set.of(projectId)).rows()).isEmpty();
        assertThatThrownBy(() -> financeService.registerArApPayment(original, payment("p13", 100000L)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("AR_AP_ITEM_REVERSED"));
        assertThat(itemState(original)).isEqualTo("pending_collection:100000");
    }

    // ---- helpers ----

    /**
     * 生效合同（2026-08 起、2026-08-31 到期，合同押金 depositCent、商户押金台账实收同额，本项目等待期 0 日使退还当天可付）发起退租/提前终止
     * （退租日＝到期日）、补出 8 月租金（未收）、齐单核对完成，返回待审变更。
     */
    private ContractChangeRecord terminationWithDeposit(String tag, long depositCent, String type, Map<String, Object> extra) {
        ContractRecord contract = draft(tag);
        jdbc.update("UPDATE contracts SET lifecycle_status = 'effective', expiry_date = DATE '2026-08-31', deposit_cent = ? WHERE id = ?",
                depositCent, contract.id());
        jdbc.update("INSERT INTO deposit_ledger (id, merchant_id, action, amount_cent, balance_after_cent, reason, created_at)"
                + " VALUES (?, ?, 'collect', ?, ?, '押金抵扣测试押金', now() - interval '1 day')",
                "dl_" + key(tag), contract.merchantId(), depositCent, depositCent);
        jdbc.update("INSERT INTO contract_deposit_refund_wait_policy (project_id, wait_days, min_days, max_days) VALUES (?, 0, 0, 60)",
                projectId);
        Map<String, Object> payload = new LinkedHashMap<>(extra);
        payload.put("moveOutDate", "2026-08-31");
        ContractChangeRecord change = contractService.raiseChange(contract.id(), new ContractChangeRequest(key("t_" + tag), type,
                payload, gm, LINE_CODES.stream().map(this::line).toList(), "退租押金抵扣"));
        rentBill(contract, "2026-08");
        ContractSettlementRecord settlement = repository.listSettlementsByContract(contract.id()).get(0);
        contractService.checkSettlementDocuments(settlement.id(), new ContractResolveDisputeRequest(key("dct_" + tag), null));
        return change;
    }

    /**
     * 第十八批 Q 段第 138 条 138-R1 与 138-R5 同批回归（ENUM:257「营业额抽成账单在该账期的薪资月结完成、该账期月结定格时确定的服务费应发数与扣留数定数之后出账，
     * 账单一次出准，不使用预估数」）：酒吧商户「保底与抽成取高」生效合同，在当月卡座服务费尚未月结（此时抽成取数报 409 BAR_REVENUE_PENDING）时发起面积变更
     * 并全链终审 → 终审成功、父合同置已变更，子合同不预排租金；终审不因抽成来源未定整笔回滚。
     */
    @Test
    void higherOfBarAreaChangeApprovesWhileServiceFeeAwaitsMonthClose() {
        java.time.YearMonth now = java.time.YearMonth.now(com.complex.platform.common.LocalZone.ID);
        String merchant = "mch_hpc01_" + suffix;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at)"
                + " VALUES(?,'审批链酒吧','bar',?,'active',now(),now())", merchant, projectId);
        ContractRecord contract = draft("c138");
        jdbc.update("UPDATE contracts SET lifecycle_status = 'effective', snapshot = CAST(? AS jsonb), rent_terms = CAST(? AS jsonb) WHERE id = ?",
                "{\"depositMonths\": 3}", "{\"rentBillingMode\":\"higher_of\",\"baseRentCent\":100000}", contract.id());
        jdbc.update("""
                INSERT INTO turnover_rent_rules
                    (id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent, effective_from, effective_to, status, is_draft)
                VALUES (?, ?, ?, '审批链酒吧抽成规则', 1000, 0, DATE '2020-01-01', NULL, 'active', FALSE)
                """, key("trr_c138"), contract.id(), merchant);
        serviceFeeAwaitingMonthClose(merchant, now.atDay(1));

        ContractChangeRecord area = contractService.raiseChange(contract.id(), new ContractChangeRequest(key("ac_c138"), "area_change",
                Map.of("base_rent_cent", 150000, "deposit_deadline", now.plusMonths(1).atDay(1).toString()), gm,
                LINE_CODES.stream().map(this::line).toList(), "面积变更"));
        assertThatCode(() -> approveAll(area.reviewId())).doesNotThrowAnyException();

        assertThat(contractStatus(contract.id())).isEqualTo("amended");
        ContractRecord child = repository.listChildContracts(contract.id()).get(0);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM contract_rent_bills WHERE contract_id = ? AND fee_type = 'rent'",
                Integer.class, child.id())).isZero();
    }

    /**
     * 第十八批 U 段第 170 条 170-R3（OpenAPI 与代码一致；ContractChangeRequest.change_type 补 location_change，网页合同工作台「经营位置变更」在发）：
     * 起草人按前端请求形状（蛇形键）经真实接口发起经营位置变更，指定主线最终审批人与三条平行线最终审批人 → 200，变更单落库为 location_change、待审。
     */
    @Test
    void locationChangeIsRaisedThroughTheEndpointWithFrontendSnakeCaseBody() throws Exception {
        java.time.YearMonth now = java.time.YearMonth.now(com.complex.platform.common.LocalZone.ID);
        ContractRecord contract = draft("c170");
        jdbc.update("UPDATE contracts SET lifecycle_status = 'effective', snapshot = CAST(? AS jsonb) WHERE id = ?",
                "{\"depositMonths\": 3}", contract.id());
        String lines = LINE_CODES.stream()
                .map(code -> "{\"line_code\":\"" + code + "\",\"final_approver_id\":\"" + lineFinal.get(code) + "\"}")
                .collect(java.util.stream.Collectors.joining(","));
        mockMvc.perform(post("/api/v1/contracts/" + contract.id() + "/changes").with(webSession(drafter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"" + key("lc_c170") + "\",\"change_type\":\"location_change\","
                                + "\"payload\":{\"base_rent_cent\":120000,\"deposit_deadline\":\"" + now.plusMonths(1).atDay(1) + "\"},"
                                + "\"final_approver_id\":\"" + gm + "\",\"parallel_lines\":[" + lines + "],\"reason\":\"经营位置变更\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.change_type").value("location_change"));
        assertThat(jdbc.queryForList("SELECT change_type || '/' || approval_status FROM contract_changes WHERE contract_id = ?",
                String.class, contract.id())).containsExactly("location_change/pending");
    }

    /** 前置态：该酒吧卡座一张未结账单带服务费分项（营业日 businessDate），尚无月结。 */
    private void serviceFeeAwaitingMonthClose(String merchant, LocalDate businessDate) {
        String table = key("bt_c138");
        String session = key("bs_c138");
        String order = key("bo_c138");
        String bill = key("bb_c138");
        jdbc.update("INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,capacity,allowed_pass_free_guest_count,"
                + "min_consumption_cent,currency,status,created_at,updated_at) VALUES(?,?,?,?,'审批链卡座','card_seat',4,0,0,'CNY','active',now(),now())",
                table, merchant, merchant, table);
        jdbc.update("INSERT INTO bar_table_sessions(id,bar_id,merchant_id,business_date,table_id,service_staff_id,status,opened_at,closed_at,"
                + "created_at,updated_at) VALUES(?,?,?,?,?,?,'closed',now(),now(),now(),now())",
                session, merchant, merchant, java.sql.Date.valueOf(businessDate), table, drafter);
        jdbc.update("INSERT INTO bar_orders(id,bar_id,merchant_id,table_id,order_source,status,total_amount_cent,currency,created_at,updated_at)"
                + " VALUES(?,?,?,?,'staff','created',20000,'CNY',now(),now())", order, merchant, merchant, table);
        jdbc.update("INSERT INTO bar_bills(id,bill_no,order_id,bar_id,merchant_id,table_id,subtotal_amount_cent,discount_amount_cent,"
                + "deposit_deduct_amount_cent,payable_amount_cent,paid_amount_cent,currency,payment_status,settlement_status,tip_amount_cent,"
                + "created_at,updated_at) VALUES(?,?,?,?,?,?,20000,0,0,22000,0,'CNY','unpaid','unsettled',0,now(),now())",
                bill, bill, order, merchant, merchant, table);
        jdbc.update("INSERT INTO bar_payroll_bill_components(id,source_bill_id,session_id,source_table_id,project_id,merchant_id,business_date,"
                + "component_type,amount_cent,original_base_cent,source_event_id,source_snapshot,collection_bill_id)"
                + " VALUES(?,?,?,?,?,?,?,'service_fee',2000,20000,?,'[]'::jsonb,?)",
                key("bpc_c138"), bill, session, table, projectId, merchant, java.sql.Date.valueOf(businessDate), key("bpe_c138"), bill);
    }

    private void approveAll(String reviewId) {
        for (ApprovalChainNode node : reviews.chain(reviewId)) {
            approve(reviewId, node.approverId());
        }
    }

    private String rentItemId(String contractId) {
        return jdbc.queryForObject("SELECT ar_ap_item_id FROM contract_rent_bills WHERE contract_id = ?", String.class, contractId);
    }

    private String rentItemId(String contractId, String period) {
        return jdbc.queryForObject("SELECT ar_ap_item_id FROM contract_rent_bills WHERE contract_id = ? AND period = ? AND voided_at IS NULL",
                String.class, contractId, period);
    }

    /** 该期未作废租金账单的台账行「未带锁:到期日:金额」。 */
    private String liveRentItemFacts(String contractId, java.time.YearMonth period) {
        return jdbc.queryForObject("SELECT (a.locked_day_close_id IS NULL) || ':' || a.due_date || ':' || a.amount_cent FROM contract_rent_bills b"
                + " JOIN ar_ap_items a ON a.id = b.ar_ap_item_id WHERE b.contract_id = ? AND b.period = ? AND b.fee_type = 'rent'"
                + " AND b.voided_at IS NULL", String.class, contractId, period.toString());
    }

    /** 日结记录与其汇总金额（经营日:状态:锁定时刻 + 各源 源:金额:退款:单数）。 */
    private List<String> dailyCloseFacts(String closeId) {
        List<String> facts = new ArrayList<>(jdbc.queryForList("SELECT business_date || ':' || status || ':' || locked_at"
                + " FROM finance_daily_closes WHERE id = ?", String.class, closeId));
        facts.addAll(jdbc.queryForList("SELECT source_stream || ':' || amount_cent || ':' || refund_cent || ':' || order_count"
                + " FROM finance_daily_close_summaries WHERE daily_close_id = ? ORDER BY source_stream", String.class, closeId));
        return facts;
    }

    private String itemState(String arApItemId) {
        return jdbc.queryForObject("SELECT status || ':' || unpaid_amount_cent FROM ar_ap_items WHERE id = ?", String.class, arApItemId);
    }

    /** 押金抵扣收款流水（payment_method=deposit_offset、流水号＝结算单 id）：「台账项 id:金额」，按金额降序。 */
    private List<String> offsetPayments(String settlementId) {
        return jdbc.queryForList("SELECT ar_ap_item_id || ':' || amount_cent FROM ar_ap_payments"
                + " WHERE payment_method = 'deposit_offset' AND bank_flow_ref = ? ORDER BY amount_cent DESC", String.class, settlementId);
    }

    private List<String> depositLedger(ContractChangeRecord change) {
        String merchantId = repository.findContractById(change.contractId()).orElseThrow().merchantId();
        return jdbc.queryForList("SELECT action || ':' || amount_cent || ':' || balance_after_cent FROM deposit_ledger"
                + " WHERE merchant_id = ? ORDER BY created_at, id", String.class, merchantId);
    }

    /** 结算清单押金处置项回写的逐笔销账（对账）：「台账项 id:金额」。 */
    @SuppressWarnings("unchecked")
    private List<String> settlementOffsets(String settlementId) {
        Map<String, Object> disposal = repository.findSettlementById(settlementId).orElseThrow().settlementItems().stream()
                .filter(i -> "deposit_disposal".equals(i.get("item"))).findFirst().orElseThrow();
        return ((List<Map<String, Object>>) disposal.get("offsets")).stream()
                .map(o -> o.get("arApItemId") + ":" + ((Number) o.get("amountCent")).longValue()).toList();
    }

    private ArApPaymentRequest payment(String tag, long amountCent) {
        return new ArApPaymentRequest(key(tag), "登记收付款", List.of(), amountCent, "bank_transfer", null, "flow_" + key(tag));
    }

    /** 生效合同（2026-08 起、2026-08-31 到期）发起退租（退租日＝到期日）、补出 8 月租金、齐单核对完成，返回待审退租变更。 */
    private ContractChangeRecord moveOutRaisedAndChecked(String tag) {
        ContractRecord contract = draft(tag);
        jdbc.update("UPDATE contracts SET lifecycle_status = 'effective', expiry_date = DATE '2026-08-31' WHERE id = ?",
                contract.id());
        ContractChangeRecord change = contractService.raiseChange(contract.id(), new ContractChangeRequest(key("mo_" + tag),
                "move_out", Map.of("moveOutDate", "2026-08-31"), gm, LINE_CODES.stream().map(this::line).toList(), "到期退租"));
        rentBill(contract, "2026-08");
        ContractSettlementRecord settlement = repository.listSettlementsByContract(contract.id()).get(0);
        contractService.checkSettlementDocuments(settlement.id(), new ContractResolveDisputeRequest(key("dc_" + tag), null));
        return change;
    }

    private void rentBill(ContractRecord contract, String period) {
        String tag = period + "_" + contract.id().substring(4, 24);   // 单号控制在列宽内
        repository.insertRentBill(new ContractRentBillRecord("crb_" + tag, contract.id(), contract.merchantId(), projectId, period,
                "rent", 100000L, "arap_" + tag, Instant.now(), null), LocalDate.parse(period + "-01"));
    }

    private String settlementStatus(String settlementId) {
        return jdbc.queryForObject("SELECT status FROM contract_settlements WHERE id = ?", String.class, settlementId);
    }

    private ContractRecord draft(String tag) {
        return contractService.createDraft(new ContractDraftRequest(key(tag), "mch_hpc01_" + suffix, subject, null, subject,
                LocalDate.parse("2026-08-01"), LocalDate.parse("2027-07-31"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "none", null, Map.of(), List.of(), List.of(), projectId, "餐饮"));
    }

    private ContractRecord submit(ContractRecord contract, String tag) {
        return contractService.submit(contract.id(), submitRequest(tag));
    }

    /** 主线最终审批人 = 总经理；三条平行线各指定该线最终审批人。 */
    private ContractSubmitRequest submitRequest(String tag) {
        return new ContractSubmitRequest(key(tag), gm, LINE_CODES.stream().map(this::line).toList(), "提交审批");
    }

    /** 本项目「新建」模板：平台默认三线、默认最终审批人空，主线层级数上下限按参数（皆空＝未配置）。 */
    private void newTemplate(Integer minLevels, Integer maxLevels) {
        contractService.saveApprovalFlow(new ContractApprovalFlowRequest(key("tpl_new_" + minLevels + "_" + maxLevels), projectId,
                "new", "本项目·新建", LINE_CODES, null, minLevels, maxLevels));
    }

    private ParallelLine line(String code) {
        return new ParallelLine(code, lineFinal.get(code));
    }

    private void approve(String reviewId, String approver) {
        actAs(approver);
        reviews.approve(reviewId, approval("approved"));
    }

    private static SupervisorReviewApprovalRequest approval(String decision) {
        return new SupervisorReviewApprovalRequest(UUID.randomUUID().toString(), null, List.of(), null, decision,
                "approved".equals(decision) ? "同意" : "条款不合规");
    }

    private static List<String> approvers(List<ApprovalChainNode> nodes, String lineCode) {
        return nodes.stream().filter(n -> lineCode.equals(n.lineCode())).map(ApprovalChainNode::approverId).toList();
    }

    private String contractStatus(String contractId) {
        return jdbc.queryForObject("SELECT lifecycle_status FROM contracts WHERE id = ?", String.class, contractId);
    }

    private String reviewStatus(String reviewId) {
        return jdbc.queryForObject("SELECT review_status FROM supervisor_review_decisions WHERE review_id = ?", String.class, reviewId);
    }

    private Map<String, Integer> nodeStatusCounts(String reviewId) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        jdbc.query("SELECT status, count(*) AS n FROM supervisor_review_nodes WHERE review_id = ? GROUP BY status",
                rs -> { counts.put(rs.getString("status"), rs.getInt("n")); }, reviewId);
        return counts;
    }

    private int auditCount(String eventName, String businessId) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = ? AND business_id = ?",
                Integer.class, eventName, businessId);
    }

    private String user(String name) {
        String id = "usr_hpc01_" + name + "_" + suffix;
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', ?, NULL, 'active', now())", id, id, projectId);
        return id;
    }

    private void superior(String userId, String superiorId) {
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id) VALUES (?, ?)", userId, superiorId);
    }

    /** 后台 Web 终端＋生产过滤器放入的 AccessActor 主体（与 actAs 同一组身份字段）。 */
    private RequestPostProcessor webSession(String userId) {
        AccessActor actor = new AccessActor(userId, userId, "platform_admin", projectId, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private void actAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "platform_admin", projectId, null, "active"), "token", List.of()));
    }

    private String key(String tag) {
        return tag + "_" + suffix;
    }
}
