package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.FinanceRepository;
import com.complex.platform.finance.TurnoverRentRuleRecord;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ArchiveRequest;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * V346（v7 审计 P0-E·缺口二）营业额抽成规则草案闸验收：
 * 「改 → 落草案（不生效·旧值保值）→ 差人复核转正 → 才对后续结算生效」，以及软删下线。
 *
 * <p>⚠️ 走真实 Flyway 迁移库；{@code @Transactional} = 每用例结束自动回滚。
 * <p>差人转正：起草用 {@code DRAFTER}、复核转正用 {@code APPROVER}（二者不同，否则 403）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class TurnoverRentRuleDraftGateJdbcIntegrationTests {

    private static final String DRAFTER = "drafter_trr";
    private static final String APPROVER = "approver_trr";
    private static final String CONTRACT = "ct_trr_draft_gate_it";
    private static final String MERCHANT = "merch_trr_draft_gate_it";
    private static final String ACTIVE_RULE_ID = "trr_it_active";
    private static final String DRAFT_RULE_ID = "trr_it_draft";

    @Autowired
    private FinanceRepository financeRepository;

    @Autowired
    private SettlementConfigWriteService writeService;

    @Autowired
    private JdbcSettlementConfigWriteRepository writeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
        cleanup();
    }

    @AfterEach
    void tearDown() {
        cleanup();
        TestSecurityActors.clear();
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM turnover_rent_rule_tiers WHERE rule_id IN (SELECT id FROM turnover_rent_rules WHERE contract_id = ?)", CONTRACT);
        jdbcTemplate.update("DELETE FROM turnover_rent_rules WHERE contract_id = ?", CONTRACT);
    }

    private TurnoverRentRuleRecord rule(String id, String name, int rateBps) {
        return new TurnoverRentRuleRecord(id, CONTRACT, MERCHANT, name, rateBps, 10000L, null,
                LocalDate.parse("2026-01-01"), null, "active", Instant.now());
    }

    private String statusOf(String id) {
        return jdbcTemplate.queryForObject("SELECT status FROM turnover_rent_rules WHERE id = ?", String.class, id);
    }

    private boolean isDraft(String id) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT is_draft FROM turnover_rent_rules WHERE id = ?", Boolean.class, id));
    }

    /** 落草案：读侧照旧读到<b>旧生效规则</b>（保值），草案对结算零影响，并出现在待复核队列里。 */
    @Test
    void draftDoesNotAffectSettlementAndShowsInPendingQueue() {
        financeRepository.saveTurnoverRentRule(rule(ACTIVE_RULE_ID, "旧生效规则", 300));
        assertThat(financeRepository.findActiveTurnoverRentRuleByContract(CONTRACT))
                .get().extracting(TurnoverRentRuleRecord::id).isEqualTo(ACTIVE_RULE_ID);

        financeRepository.saveTurnoverRentRuleDraft(rule(DRAFT_RULE_ID, "新草案规则", 900), DRAFTER);

        // 草案落库了，但读侧（抽成取数）仍然只认旧生效规则 —— 单人改动不经转正绝不生效。
        assertThat(isDraft(DRAFT_RULE_ID)).isTrue();
        assertThat(financeRepository.findActiveTurnoverRentRuleByContract(CONTRACT))
                .get().extracting(TurnoverRentRuleRecord::id).isEqualTo(ACTIVE_RULE_ID);

        // 草案进配置中心「待复核草案」队列。
        assertThat(writeRepository.listPendingConfigDrafts())
                .extracting(JdbcSettlementConfigWriteRepository.PendingConfigDraft::id,
                        JdbcSettlementConfigWriteRepository.PendingConfigDraft::configGroup)
                .contains(org.assertj.core.groups.Tuple.tuple(DRAFT_RULE_ID, "turnover_rent_rule"));
    }

    /** 差人守卫：起草人不得自行转正。 */
    @Test
    void drafterCannotActivateOwnDraft() {
        financeRepository.saveTurnoverRentRuleDraft(rule(DRAFT_RULE_ID, "新草案规则", 900), DRAFTER);

        assertThatThrownBy(() -> writeService.activateDraft("turnover_rent_rule", DRAFT_RULE_ID, DRAFTER))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SELF_ACTIVATION_FORBIDDEN");

        // 被拒后草案仍是草案，旧口径纹丝不动。
        assertThat(isDraft(DRAFT_RULE_ID)).isTrue();
    }

    /** 复核转正：另一位有权限的同事转正后，旧生效规则软替换为 inactive，新规则对后续结算生效。 */
    @Test
    void approverActivationSwapsActiveRuleAndTakesEffect() {
        financeRepository.saveTurnoverRentRule(rule(ACTIVE_RULE_ID, "旧生效规则", 300));
        financeRepository.saveTurnoverRentRuleDraft(rule(DRAFT_RULE_ID, "新草案规则", 900), DRAFTER);

        writeService.activateDraft("turnover_rent_rule", DRAFT_RULE_ID, APPROVER);

        assertThat(isDraft(DRAFT_RULE_ID)).isFalse();
        // 旧行软替换（留库可追溯，不物理删）。
        assertThat(statusOf(ACTIVE_RULE_ID)).isEqualTo("inactive");
        assertThat(statusOf(DRAFT_RULE_ID)).isEqualTo("active");
        // 抽成取数改读新规则。
        assertThat(financeRepository.findActiveTurnoverRentRuleByContract(CONTRACT))
                .get().extracting(TurnoverRentRuleRecord::id).isEqualTo(DRAFT_RULE_ID);
    }

    /** 软删铁律：规则下线 = status→inactive + archive_reason（差人守卫），下线后该合同无生效抽成规则。 */
    @Test
    void activatedRuleCanBeSoftDeletedWithReason() {
        financeRepository.saveTurnoverRentRuleDraft(rule(DRAFT_RULE_ID, "新草案规则", 900), DRAFTER);
        writeService.activateDraft("turnover_rent_rule", DRAFT_RULE_ID, APPROVER);

        writeService.archive("turnover_rent_rule", DRAFT_RULE_ID,
                new ArchiveRequest("archive-trr-it", "合同终止，抽成规则下线"), DRAFTER);

        assertThat(statusOf(DRAFT_RULE_ID)).isEqualTo("inactive");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT archive_reason FROM turnover_rent_rules WHERE id = ?", String.class, DRAFT_RULE_ID))
                .isEqualTo("合同终止，抽成规则下线");
        assertThat(financeRepository.findActiveTurnoverRentRuleByContract(CONTRACT)).isEmpty();
    }
}
