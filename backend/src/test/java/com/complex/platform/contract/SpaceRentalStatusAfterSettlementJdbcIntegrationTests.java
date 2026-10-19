package com.complex.platform.contract;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.access.AccessActor;
import com.complex.platform.analytics.AnalyticsRecords;
import com.complex.platform.analytics.AnalyticsRepository;
import com.complex.platform.common.LocalZone;
import com.complex.platform.contract.dto.ContractResolveDisputeRequest;
import com.complex.platform.contract.dto.ContractSettleRequest;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.dto.ArApPaymentRequest;
import com.complex.platform.governance.chain.ApprovalChainNode;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.support.TestSecurityActors;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * CG-09 退租结算完成即转空置（PGM「合同运营细则」：「铺位租赁状态随意向与合同自动联动并在平面图着色（洽谈中、已租、即将到期等），
 * 退租结算完成即转空置，不可租人工标注」；BSM「铺位看板」/ ENUM「铺位租赁状态」：「空置、洽谈中、已租、即将到期、已退租、不可租」）。
 *
 * <p>走真实 Flyway 迁移库，{@code @Transactional} 每用例回滚；结算单开立 / 结清走正路 {@code ContractService.settle} → 齐单核对 →
 * 「到期终止」类复核单逐节点通过（HP-C02：终审回写即落账，不设手动结清入口；退租/提前终止由变更终审落账，故结清用例统一走 expiry），
 * 追 {@code contract_settlements.status}、{@code contracts.lifecycle_status} 列，
 * 再断言铺位看板（{@code listSpaceRentalRaw}）与经营看板（{@code listVacantSpaces} / {@code occupancyAreas}）两条推导同口径。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class SpaceRentalStatusAfterSettlementJdbcIntegrationTests {

    @Autowired private ContractService contractService;
    @Autowired private JdbcContractRepository contractRepository;
    @Autowired private AnalyticsRepository analyticsRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private SupervisorReviewService reviews;
    @Autowired private FinanceService financeService;

    private String suffix;
    private String projectId;
    private LocalDate today;
    private BigDecimal leasedBaseline;
    private String actorId;
    private String gm;
    private final List<ParallelLine> lines = new ArrayList<>();

    @BeforeEach
    void seed() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        projectId = "prj_cg09_" + suffix;
        today = LocalDate.now(LocalZone.ID);
        leasedBaseline = analyticsRepository.occupancyAreas(today).leasedArea();
        authenticateWithSettlementPermission();
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    /** 结算单开立（合同仍 effective）→ 已退租；出租面积不再计入；结算 settled → 空置、合同到期。 */
    @Test
    void settlementOpenedTurnsMovedOutAndSettledTurnsVacant() {
        String spaceId = space("sp_a", "100");
        String contractId = contract("ct_a", spaceId, "effective", today.minusDays(30), today.plusDays(300));
        assertThat(rentalStatus(spaceId)).isEqualTo("let");
        assertThat(analyticsRepository.occupancyAreas(today).leasedArea().subtract(leasedBaseline))
                .isEqualByComparingTo("100");

        ContractSettlementRecord opened = contractService.settle(contractId,
                settleRequest("cg09-open-" + suffix, "expiry"));
        assertThat(settlementStatus(opened.id())).isEqualTo("pending_settlement");
        assertThat(lifecycle(contractId)).isEqualTo("effective");
        assertThat(rentalStatus(spaceId)).isEqualTo("moved_out");
        assertThat(vacantRow(spaceId)).isNotNull();
        assertThat(vacantRow(spaceId).rentalStatus()).isEqualTo("moved_out");
        assertThat(analyticsRepository.occupancyAreas(today).leasedArea().subtract(leasedBaseline))
                .isEqualByComparingTo("0");

        contractService.checkSettlementDocuments(opened.id(), new ContractResolveDisputeRequest("cg09-docs-" + suffix, null));
        approveAll(opened.reviewId());
        assertThat(settlementStatus(opened.id())).isEqualTo("settled");
        assertThat(lifecycle(contractId)).isEqualTo("expired");
        assertThat(rentalStatus(spaceId)).isEqualTo("vacant");
        assertThat(vacantRow(spaceId).rentalStatus()).isEqualTo("vacant");
    }

    /** 到期合同：结算未完（含尚未开立结算单）→ 已退租；到期结算 settled → 空置。 */
    @Test
    void expiredContractStaysMovedOutUntilSettlementSettled() {
        String spaceId = space("sp_b", "80");
        String contractId = contract("ct_b", spaceId, "expired", today.minusDays(400), today.minusDays(1));
        assertThat(rentalStatus(spaceId)).isEqualTo("moved_out");

        ContractSettlementRecord opened = contractService.settle(contractId,
                settleRequest("cg09-exp-open-" + suffix, "expiry"));
        assertThat(settlementStatus(opened.id())).isEqualTo("pending_settlement");
        assertThat(rentalStatus(spaceId)).isEqualTo("moved_out");
        assertThat(vacantRow(spaceId).rentalStatus()).isEqualTo("moved_out");

        contractService.checkSettlementDocuments(opened.id(), new ContractResolveDisputeRequest("cg09-exp-docs-" + suffix, null));
        approveAll(opened.reviewId());
        assertThat(settlementStatus(opened.id())).isEqualTo("settled");
        assertThat(lifecycle(contractId)).isEqualTo("expired");
        assertThat(rentalStatus(spaceId)).isEqualTo("vacant");
        assertThat(vacantRow(spaceId).rentalStatus()).isEqualTo("vacant");
    }

    /** 人工标注不可租优先于一切推导；不可租不进空置明细。 */
    @Test
    void manualUnavailableWinsOverSettlementDerivation() {
        String spaceId = space("sp_c", "50");
        String contractId = contract("ct_c", spaceId, "effective", today.minusDays(10), today.plusDays(100));
        contractService.settle(contractId, settleRequest("cg09-unav-" + suffix, "early_termination"));
        jdbcTemplate.update("UPDATE spaces SET status = 'unavailable' WHERE id = ?", spaceId);
        assertThat(rentalStatus(spaceId)).isEqualTo("unavailable");
        assertThat(vacantRow(spaceId)).isNull();
    }

    /** 洽谈中优先于空置（结算已完成的铺位有招商意向 → 洽谈中）；已退租优先于洽谈中（结算未完的铺位有意向仍 → 已退租）。 */
    @Test
    void negotiatingBeatsVacantButMovedOutBeatsNegotiating() {
        String settledSpace = space("sp_d1", "60");
        String settledContract = contract("ct_d1", settledSpace, "effective", today.minusDays(30), today.plusDays(300));
        ContractSettlementRecord opened = contractService.settle(settledContract,
                settleRequest("cg09-neg-open-" + suffix, "expiry"));
        contractService.checkSettlementDocuments(opened.id(), new ContractResolveDisputeRequest("cg09-neg-docs-" + suffix, null));
        approveAll(opened.reviewId());
        intent(settledSpace);
        assertThat(rentalStatus(settledSpace)).isEqualTo("negotiating");
        assertThat(vacantRow(settledSpace).rentalStatus()).isEqualTo("negotiating");

        String openSpace = space("sp_d2", "70");
        String openContract = contract("ct_d2", openSpace, "effective", today.minusDays(30), today.plusDays(300));
        contractService.settle(openContract, settleRequest("cg09-neg-open2-" + suffix, "move_out"));
        intent(openSpace);
        assertThat(rentalStatus(openSpace)).isEqualTo("moved_out");
        assertThat(vacantRow(openSpace).rentalStatus()).isEqualTo("moved_out");
    }

    /**
     * 第三轮裁决第 4 条「押金足以抵扣全部欠费、有余额要退的→「押金抵扣中」，押金退还付清后→「已结清」」＋控制席约定「铺位转空置、合同终止等现有时点
     * 保持在终审落账时，不随新状态推迟」：真库真接线——终审落账即押金抵扣中、合同到期、铺位空置；押金退还应付经财务登记付清
     * （settleArApItem 经端口回调合同域）→ 已结清。等待期配 0 日使退款当天可付。
     */
    @Test
    void depositOffsettingTurnsVacantAtPostingAndRefundPaidSettles() {
        String spaceId = space("sp_e", "90");
        String contractId = contract("ct_e", spaceId, "effective", today.minusDays(30), today.plusDays(300));
        jdbcTemplate.update("UPDATE contracts SET deposit_cent = 300000 WHERE id = ?", contractId);
        jdbcTemplate.update("INSERT INTO deposit_ledger (id, merchant_id, action, amount_cent, balance_after_cent, reason, created_at)"
                + " VALUES (?, ?, 'collect', 300000, 300000, '退租转空置测试押金', now())", "dl_cg09_" + suffix, "mch_cg09_" + suffix);
        jdbcTemplate.update("INSERT INTO contract_deposit_refund_wait_policy (project_id, wait_days, min_days, max_days)"
                + " VALUES (?, 0, 0, 60)", projectId);
        ContractSettlementRecord opened = contractService.settle(contractId, settleRequest("cg09-dep-open-" + suffix, "expiry"));
        contractService.checkSettlementDocuments(opened.id(), new ContractResolveDisputeRequest("cg09-dep-docs-" + suffix, null));
        approveAll(opened.reviewId());
        assertThat(settlementStatus(opened.id())).isEqualTo("deposit_offsetting");
        assertThat(lifecycle(contractId)).isEqualTo("expired");
        assertThat(rentalStatus(spaceId)).isEqualTo("vacant");
        assertThat(vacantRow(spaceId).rentalStatus()).isEqualTo("vacant");

        String payableId = jdbcTemplate.queryForObject(
                "SELECT id FROM ar_ap_items WHERE source_type = 'deposit_refund' AND source_id = ?", String.class, opened.id());
        financeService.registerArApPayment(payableId, new ArApPaymentRequest("cg09-dep-pay-" + suffix, "退押金", List.of(),
                300000L, "bank_transfer", null, "flow_cg09_" + suffix));
        assertThat(settlementStatus(opened.id())).isEqualTo("settled");
        assertThat(rentalStatus(spaceId)).isEqualTo("vacant");
    }

    // ---------- helpers ----------

    /** 结算开账请求：到期结算按「到期终止」类模板开复核单（主线最终审批人 gm＋三线各自最终审批人）；退租/提前终止不用审批入参。 */
    private ContractSettleRequest settleRequest(String key, String trigger) {
        return new ContractSettleRequest(key, trigger, null, gm, lines, null);
    }

    /** 「到期终止」复核单逐节点通过（终审回写即落账），完事切回结算操作人。 */
    private void approveAll(String reviewId) {
        for (ApprovalChainNode node : reviews.chain(reviewId)) {
            actAs(node.approverId());
            reviews.approve(reviewId, new SupervisorReviewApprovalRequest(UUID.randomUUID().toString(), null, List.of(), null,
                    "approved", "同意"));
        }
        actAs(actorId);
    }

    private String approver(String name) {
        String id = "usr_cg09_" + name + "_" + suffix;
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", id, id);
        return id;
    }

    private void actAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "platform_admin", "org_1", null, "active"), "token", List.of()));
    }

    private String rentalStatus(String spaceId) {
        return contractRepository.listSpaceRentalRaw(projectId, today).stream()
                .filter(r -> r.spaceId().equals(spaceId)).findFirst().orElseThrow().rentalStatus();
    }

    private AnalyticsRecords.VacantSpaceRow vacantRow(String spaceId) {
        return analyticsRepository.listVacantSpaces(today, 200, 0, List.of(projectId)).stream()
                .filter(r -> r.spaceId().equals(spaceId)).findFirst().orElse(null);
    }

    private String settlementStatus(String settlementId) {
        return jdbcTemplate.queryForObject("SELECT status FROM contract_settlements WHERE id = ?", String.class, settlementId);
    }

    private String lifecycle(String contractId) {
        return jdbcTemplate.queryForObject("SELECT lifecycle_status FROM contracts WHERE id = ?", String.class, contractId);
    }

    private String space(String key, String area) {
        String id = key + "_cg09_" + suffix;
        jdbcTemplate.update("INSERT INTO spaces (id, space_name, space_type, floor_no, area_square_meter, polygon_points, status, "
                + "project_id, created_at, updated_at) VALUES (?, ?, 'shop', '1F', ?, '[]'::jsonb, 'active', ?, now(), now())",
                id, "退租转空置测试铺位" + key, new BigDecimal(area), projectId);
        return id;
    }

    private String contract(String key, String spaceId, String lifecycle, LocalDate effective, LocalDate expiry) {
        String id = key + "_cg09_" + suffix;
        jdbcTemplate.update("INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, "
                + "expiry_date, lifecycle_status, project_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, now(), now())",
                id, "mch_cg09_" + suffix, "subj_cg09_" + suffix, "subj_cg09_" + suffix, effective, expiry, lifecycle, projectId);
        jdbcTemplate.update("INSERT INTO contract_spaces (contract_id, space_id) VALUES (?, ?)", id, spaceId);
        return id;
    }

    private void intent(String spaceId) {
        String leadId = "lead_cg09_" + spaceId;
        jdbcTemplate.update("INSERT INTO leasing_leads (id, contact_name, source, status, intended_space_id, project_id, created_at, updated_at) "
                + "VALUES (?, '退租转空置测试线索', 'outbound', 'negotiating', ?, ?, now(), now())", leadId, spaceId, projectId);
        jdbcTemplate.update("INSERT INTO leasing_intents (id, lead_id, stage, intended_space_id, project_id, created_at, updated_at) "
                + "VALUES (?, ?, 'negotiation', ?, ?, now(), now())", "intent_cg09_" + spaceId, leadId, spaceId, projectId);
    }

    private void authenticateWithSettlementPermission() {
        String userId = "usr_cg09_" + suffix;
        String roleId = "role_cg09_" + suffix;
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", userId, "u_cg09_" + suffix);
        jdbcTemplate.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId,
                "退租转空置测试角色" + System.nanoTime());
        jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'contract.settlement.manage')", roleId);
        jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'finance.payment.register')", roleId);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        // 到期终止审批链：结算操作人 → 总经理；三条平行线各自 起点人 → 该线最终审批人（事务内改 approval_lines，用例结束回滚）。
        gm = approver("gm");
        jdbcTemplate.update("INSERT INTO org_hierarchy (user_id, superior_id) VALUES (?, ?)", userId, gm);
        for (String code : List.of("business_review", "finance_review", "legal_review")) {
            String entry = approver(code + "_entry");
            String fin = approver(code + "_final");
            jdbcTemplate.update("INSERT INTO org_hierarchy (user_id, superior_id) VALUES (?, ?)", entry, fin);
            jdbcTemplate.update("UPDATE approval_lines SET entry_user_id = ? WHERE line_code = ?", entry, code);
            lines.add(new ParallelLine(code, fin));
        }
        actorId = userId;
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, "u_cg09_" + suffix, "platform_admin", "org_1", null, "active"), "token", List.of()));
    }
}
