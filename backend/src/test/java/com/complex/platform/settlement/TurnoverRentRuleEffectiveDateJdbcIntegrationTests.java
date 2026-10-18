package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.FinanceService;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class TurnoverRentRuleEffectiveDateJdbcIntegrationTests {

    private static final String CONTRACT = "rent_effective_contract";
    private static final String MERCHANT = "rent_effective_merchant";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcSettlementConfigWriteRepository repository;
    @Autowired private FinanceService financeService;

    @Test
    void changeApprovedOnJune16StartsJuly1AndLeavesJuneOnOriginalRule() {
        initialRule();
        draft("rent_effective_change", "2026-06-16", null, 20000);

        activate("rent_effective_change", "2026-06-16T02:00:00Z");

        assertThat(effectiveFrom("rent_effective_change")).isEqualTo(LocalDate.parse("2026-07-01"));
        assertThat(financeService.computePeriodTurnoverRentSnapshot(MERCHANT, CONTRACT, "2026-06"))
                .isEqualTo(new FinanceService.PeriodTurnoverRent("rent_effective_initial", 10000));
        assertThat(financeService.computePeriodTurnoverRentSnapshot(MERCHANT, CONTRACT, "2026-07"))
                .isEqualTo(new FinanceService.PeriodTurnoverRent("rent_effective_change", 20000));
        assertThat(effectiveFrom("rent_effective_initial")).isEqualTo(LocalDate.parse("2026-01-01"));
    }

    @Test
    void initialRulePreservesItsContractStartDate() {
        draft("rent_effective_initial", "2026-06-16", null, 10000);

        activate("rent_effective_initial", "2026-06-10T02:00:00Z");

        assertThat(effectiveFrom("rent_effective_initial")).isEqualTo(LocalDate.parse("2026-06-16"));
    }

    @Test
    void changeUsesApprovalMonthEvenWhenDraftRequestsLaterStart() {
        initialRule();
        draft("rent_effective_change", "2026-09-01", null, 20000);

        activate("rent_effective_change", "2026-06-16T02:00:00Z");

        assertThat(effectiveFrom("rent_effective_change")).isEqualTo(LocalDate.parse("2026-07-01"));
    }

    @ParameterizedTest
    @CsvSource({
            "2026-06-30T15:59:59Z,2026-07-01",
            "2026-06-30T16:00:00Z,2026-08-01",
            "2026-12-31T16:00:00Z,2027-02-01"
    })
    void changeUsesLocalCalendarMonthInsteadOfUtcOrBusinessDayCutoff(String approvedAt, String expectedStart) {
        initialRule();
        draft("rent_effective_change", "2026-06-16", null, 20000);

        activate("rent_effective_change", approvedAt);

        assertThat(effectiveFrom("rent_effective_change")).isEqualTo(LocalDate.parse(expectedStart));
    }

    @Test
    void expiredEndDateRejectsChangeWithoutReplacingOriginalOrAssigningSequence() {
        initialRule();
        Long originalSequence = jdbc.queryForObject(
                "SELECT activation_sequence FROM turnover_rent_rules WHERE id = 'rent_effective_initial'", Long.class);
        draft("rent_effective_change", "2026-06-16", "2026-06-30", 20000);

        assertThatThrownBy(() -> activate("rent_effective_change", "2026-06-16T02:00:00Z"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("VALIDATION_FAILED");

        assertThat(jdbc.queryForObject(
                "SELECT status FROM turnover_rent_rules WHERE id = 'rent_effective_initial'", String.class))
                .isEqualTo("active");
        assertThat(jdbc.queryForObject(
                "SELECT activation_sequence FROM turnover_rent_rules WHERE id = 'rent_effective_initial'", Long.class))
                .isEqualTo(originalSequence);
        assertThat(jdbc.queryForObject(
                "SELECT is_draft FROM turnover_rent_rules WHERE id = 'rent_effective_change'", Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT activation_sequence FROM turnover_rent_rules WHERE id = 'rent_effective_change'", Long.class)).isNull();
        assertThat(effectiveFrom("rent_effective_change")).isEqualTo(LocalDate.parse("2026-06-16"));
    }

    @Test
    void archivedApprovedRuleStillMakesLaterRuleAChange() {
        initialRule();
        jdbc.update("""
                UPDATE turnover_rent_rules SET status = 'inactive', archive_reason = '原版下线'
                 WHERE id = 'rent_effective_initial'
                """);
        draft("rent_effective_change", "2026-06-16", null, 20000);

        activate("rent_effective_change", "2026-06-16T02:00:00Z");

        assertThat(effectiveFrom("rent_effective_change")).isEqualTo(LocalDate.parse("2026-07-01"));
    }

    @Test
    void approvedRuleForAnotherMerchantDoesNotDelayInitialRule() {
        initialRule();
        jdbc.update("UPDATE turnover_rent_rules SET merchant_id = 'another_effective_merchant' WHERE id = 'rent_effective_initial'");
        draft("rent_effective_change", "2026-06-16", null, 20000);

        activate("rent_effective_change", "2026-06-10T02:00:00Z");

        assertThat(effectiveFrom("rent_effective_change")).isEqualTo(LocalDate.parse("2026-06-16"));
    }

    private void initialRule() {
        draft("rent_effective_initial", "2026-01-01", null, 10000);
        activate("rent_effective_initial", "2025-12-16T02:00:00Z");
    }

    private void draft(String id, String from, String to, long minAmount) {
        jdbc.update("""
                INSERT INTO turnover_rent_rules
                    (id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent,
                     effective_from, effective_to, status, is_draft, updated_by, created_at)
                VALUES (?, ?, ?, '次月生效回归', 1000, ?, ?, ?, 'active', TRUE,
                        'effective_drafter', TIMESTAMPTZ '2025-01-01 00:00:00+08')
                """, id, CONTRACT, MERCHANT, minAmount, LocalDate.parse(from),
                to == null ? null : LocalDate.parse(to));
    }

    private void activate(String id, String approvedAt) {
        assertThat(repository.activateSingleRowDraft(
                JdbcSettlementConfigWriteRepository.SingleRowConfig.TURNOVER_RENT_RULE,
                id, "effective_approver", Instant.parse(approvedAt))).isEqualTo(1);
    }

    private LocalDate effectiveFrom(String id) {
        return jdbc.queryForObject("SELECT effective_from FROM turnover_rent_rules WHERE id = ?",
                LocalDate.class, id);
    }
}
