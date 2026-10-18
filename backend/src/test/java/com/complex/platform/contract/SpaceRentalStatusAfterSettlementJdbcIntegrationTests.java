package com.complex.platform.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.access.AccessActor;
import com.complex.platform.analytics.AnalyticsRecords;
import com.complex.platform.analytics.AnalyticsRepository;
import com.complex.platform.common.LocalZone;
import com.complex.platform.contract.dto.ContractResolveDisputeRequest;
import com.complex.platform.contract.dto.ContractSettleRequest;
import com.complex.platform.support.TestSecurityActors;
import java.math.BigDecimal;
import java.time.LocalDate;
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
 * <p>走真实 Flyway 迁移库，{@code @Transactional} 每用例回滚；结算单开立 / 结清走正路 {@code ContractService.settle} /
 * {@code completeSettlement}，追 {@code contract_settlements.status}、{@code contracts.lifecycle_status} 列，
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

    private String suffix;
    private String projectId;
    private LocalDate today;
    private BigDecimal leasedBaseline;

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

    /** 结算单开立（合同仍 effective）→ 已退租；出租面积不再计入；结算 settled → 空置、合同终止。 */
    @Test
    void settlementOpenedTurnsMovedOutAndSettledTurnsVacant() {
        String spaceId = space("sp_a", "100");
        String contractId = contract("ct_a", spaceId, "effective", today.minusDays(30), today.plusDays(300));
        assertThat(rentalStatus(spaceId)).isEqualTo("let");
        assertThat(analyticsRepository.occupancyAreas(today).leasedArea().subtract(leasedBaseline))
                .isEqualByComparingTo("100");

        ContractSettlementRecord opened = contractService.settle(contractId,
                new ContractSettleRequest("cg09-open-" + suffix, "move_out", null));
        assertThat(settlementStatus(opened.id())).isEqualTo("pending_settlement");
        assertThat(lifecycle(contractId)).isEqualTo("effective");
        assertThat(rentalStatus(spaceId)).isEqualTo("moved_out");
        assertThat(vacantRow(spaceId)).isNotNull();
        assertThat(vacantRow(spaceId).rentalStatus()).isEqualTo("moved_out");
        assertThat(analyticsRepository.occupancyAreas(today).leasedArea().subtract(leasedBaseline))
                .isEqualByComparingTo("0");

        contractService.completeSettlement(opened.id(),
                new ContractResolveDisputeRequest("cg09-settle-" + suffix, null));
        assertThat(settlementStatus(opened.id())).isEqualTo("settled");
        assertThat(lifecycle(contractId)).isEqualTo("terminated");
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
                new ContractSettleRequest("cg09-exp-open-" + suffix, "expiry", null));
        assertThat(settlementStatus(opened.id())).isEqualTo("pending_settlement");
        assertThat(rentalStatus(spaceId)).isEqualTo("moved_out");
        assertThat(vacantRow(spaceId).rentalStatus()).isEqualTo("moved_out");

        contractService.completeSettlement(opened.id(),
                new ContractResolveDisputeRequest("cg09-exp-settle-" + suffix, null));
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
        contractService.settle(contractId, new ContractSettleRequest("cg09-unav-" + suffix, "early_termination", null));
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
                new ContractSettleRequest("cg09-neg-open-" + suffix, "move_out", null));
        contractService.completeSettlement(opened.id(), new ContractResolveDisputeRequest("cg09-neg-settle-" + suffix, null));
        intent(settledSpace);
        assertThat(rentalStatus(settledSpace)).isEqualTo("negotiating");
        assertThat(vacantRow(settledSpace).rentalStatus()).isEqualTo("negotiating");

        String openSpace = space("sp_d2", "70");
        String openContract = contract("ct_d2", openSpace, "effective", today.minusDays(30), today.plusDays(300));
        contractService.settle(openContract, new ContractSettleRequest("cg09-neg-open2-" + suffix, "move_out", null));
        intent(openSpace);
        assertThat(rentalStatus(openSpace)).isEqualTo("moved_out");
        assertThat(vacantRow(openSpace).rentalStatus()).isEqualTo("moved_out");
    }

    // ---------- helpers ----------

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
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        AccessActor actor = new AccessActor(userId, "u_cg09_" + suffix, "platform_admin", "org_1", null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }
}
