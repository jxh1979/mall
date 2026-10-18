package com.complex.platform.finance;

// 文件版本：V0.04

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.mock;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.dto.TurnoverRentBillRequest;
import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.settlement.SettlementConfigWriteService;
import com.complex.platform.settlement.JdbcSettlementConfigWriteRepository;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ArchiveRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.time.LocalDate;
import java.time.Instant;
import org.springframework.dao.DataAccessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
class FinanceHistoricalRentIntegrationTests {

    @Autowired private FinanceService service;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcFinanceRepository repository;
    @Autowired private SettlementConfigWriteService writeService;
    @Autowired private JdbcSettlementConfigWriteRepository writeRepository;

    @AfterEach
    void clearActor() {
        TestSecurityActors.clear();
    }

    @Test
    void historicalMonthUsesReplacedRuleInsteadOfCurrentActiveRule() {
        rule("rent_history_old", "2026-01-01", null, "inactive", false, null, 10000);
        rule("rent_history_new", "2026-09-01", null, "active", false, null, 20000);

        assertThat(service.computePeriodTurnoverRent("rent_history_merchant", "rent_history_contract", "2026-08"))
                .isEqualTo(10000);
        assertThat(service.computePeriodTurnoverRent("rent_history_merchant", "rent_history_contract", "2026-09"))
                .isEqualTo(20000);
    }

    @Test
    void futureActivatedRuleAndUnapprovedDraftDoNotChangeCurrentMonth() {
        rule("rent_history_old", "2026-01-01", null, "inactive", false, null, 10000);
        rule("rent_history_future", "2026-12-01", null, "active", false, null, 30000);
        rule("rent_history_draft", "2026-08-01", null, "active", true, null, 40000);

        assertThat(service.computePeriodTurnoverRent("rent_history_merchant", "rent_history_contract", "2026-08"))
                .isEqualTo(10000);
    }

    @Test
    void manuallyArchivedLatestRuleDoesNotReviveOlderRule() {
        rule("rent_history_old", "2026-01-01", null, "inactive", false, null, 10000);
        rule("rent_history_archived", "2026-08-01", null, "inactive", false, "审批下线", 20000);

        assertThatThrownBy(() -> service.computePeriodTurnoverRent(
                "rent_history_merchant", "rent_history_contract", "2026-09"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("TURNOVER_RENT_RULE_PERIOD_UNRESOLVED");
    }

    @Test
    void partialMonthCoverageAndMidMonthVersionSwitchCannotProduceWholeMonthBill() {
        rule("rent_history_old", "2026-01-01", null, "inactive", false, null, 10000);
        rule("rent_history_midmonth", "2026-08-15", null, "active", false, null, 20000);

        assertThatThrownBy(() -> service.computePeriodTurnoverRent(
                "rent_history_merchant", "rent_history_contract", "2026-08"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("TURNOVER_RENT_RULE_PERIOD_UNRESOLVED");
    }

    @Test
    void expiredRuleDoesNotCoverLaterMonth() {
        rule("rent_history_expired", "2026-01-01", "2026-08-15", "active", false, null, 10000);

        assertThatThrownBy(() -> service.computePeriodTurnoverRent(
                "rent_history_merchant", "rent_history_contract", "2026-08"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("TURNOVER_RENT_RULE_PERIOD_UNRESOLVED");
    }

    @Test
    void manualRuleIdCannotBillBeforeItsEffectiveDate() {
        rule("rent_history_future", "2026-09-01", null, "active", false, null, 20000);

        assertThatThrownBy(() -> manualService().billTurnoverRent(request("rent_history_future", "manual_future")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("TURNOVER_RENT_RULE_PERIOD_UNRESOLVED");
    }

    @Test
    void manualBillingCanUseReplacedRuleForItsHistoricalMonth() {
        rule("rent_history_old", "2026-01-01", null, "inactive", false, null, 10000);
        rule("rent_history_new", "2026-09-01", null, "active", false, null, 20000);

        assertThat(manualService().billTurnoverRent(request("rent_history_old", "manual_history")).amountCent())
                .isEqualTo(10000);
    }

    @Test
    void ruleForAnotherMerchantCannotBeUsedToCalculateRent() {
        rule("rent_history_current", "2026-01-01", null, "active", false, null, 10000);

        assertThatThrownBy(() -> service.computePeriodTurnoverRent(
                "another_merchant", "rent_history_contract", "2026-08"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("TURNOVER_RENT_RULE_PERIOD_UNRESOLVED");
    }

    @Test
    void archivedRuleWithSameStartDateCannotReviveAnOlderApprovedVersion() {
        rule("rent_history_old", "2026-08-01", null, "inactive", false, null, 10000);
        rule("rent_history_archived", "2026-08-01", null, "inactive", false, "审批下线", 20000);

        assertThatThrownBy(() -> service.computePeriodTurnoverRent(
                "rent_history_merchant", "rent_history_contract", "2026-08"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("TURNOVER_RENT_RULE_PERIOD_UNRESOLVED");
    }

    @Test
    void approvedReplacementWithSameStartDateBillsTheLatestApproval() {
        approveRule("rent_history_first", "2026-08-01", 10000);
        approveRule("rent_history_replacement", "2026-08-01", 20000);

        assertThat(service.computePeriodTurnoverRentSnapshot(
                "rent_history_merchant", "rent_history_contract", "2026-08"))
                .isEqualTo(new FinanceService.PeriodTurnoverRent("rent_history_replacement", 20000));
    }

    @Test
    void approvingFutureRulePreservesEarlierSameDateReplacementForHistoricalMonth() {
        approveRule("rent_history_first", "2026-08-01", 10000);
        approveRule("rent_history_replacement", "2026-08-01", 20000);
        long replacementSequence = activationSequence("rent_history_replacement");
        assertThat(replacementSequence).isGreaterThan(activationSequence("rent_history_first"));
        approveRule("rent_history_later", "2026-09-01", 30000);
        assertThat(activationSequence("rent_history_replacement")).isEqualTo(replacementSequence);
        assertThat(activationSequence("rent_history_later")).isGreaterThan(replacementSequence);

        assertThat(service.computePeriodTurnoverRentSnapshot(
                "rent_history_merchant", "rent_history_contract", "2026-08"))
                .isEqualTo(new FinanceService.PeriodTurnoverRent("rent_history_replacement", 20000));
        assertThat(service.computePeriodTurnoverRentSnapshot(
                "rent_history_merchant", "rent_history_contract", "2026-09"))
                .isEqualTo(new FinanceService.PeriodTurnoverRent("rent_history_later", 30000));
    }

    @Test
    void archivingLatestSameDateApprovalDoesNotRevivePriorApproval() {
        approveRule("rent_history_first", "2026-08-01", 10000);
        approveRule("rent_history_replacement", "2026-08-01", 20000);
        long replacementSequence = activationSequence("rent_history_replacement");
        writeService.archive("turnover_rent_rule", "rent_history_replacement",
                new ArchiveRequest("rent-history-archive", "审批后下线"), "rent_history_drafter");
        assertThat(activationSequence("rent_history_replacement")).isEqualTo(replacementSequence);

        assertThatThrownBy(() -> service.computePeriodTurnoverRent(
                "rent_history_merchant", "rent_history_contract", "2026-08"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("TURNOVER_RENT_RULE_PERIOD_UNRESOLVED");
    }

    @Test
    void activationSequenceCannotBeOverwritten() {
        approveRule("rent_history_immutable", "2026-08-01", 10000);

        assertThatThrownBy(() -> jdbc.update("""
                UPDATE turnover_rent_rules SET activation_sequence = activation_sequence + 100000
                 WHERE id = 'rent_history_immutable'
                """))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("转正序号不得修改");
    }

    @Test
    void repeatedActivationDoesNotDeactivateCurrentRuleOrChangeSequence() {
        approveRule("rent_history_first", "2026-08-01", 10000);
        approveRule("rent_history_replacement", "2026-08-01", 20000);
        long firstSequence = activationSequence("rent_history_first");

        assertThat(writeRepository.activateSingleRowDraft(
                JdbcSettlementConfigWriteRepository.SingleRowConfig.TURNOVER_RENT_RULE,
                "rent_history_first", "another_approver", Instant.now())).isZero();

        assertThat(activationSequence("rent_history_first")).isEqualTo(firstSequence);
        assertThat(jdbc.queryForObject("SELECT status FROM turnover_rent_rules WHERE id = ?",
                String.class, "rent_history_replacement")).isEqualTo("active");
        assertThat(service.computePeriodTurnoverRent(
                "rent_history_merchant", "rent_history_contract", "2026-08")).isEqualTo(20000);
    }

    private long activationSequence(String id) {
        return jdbc.queryForObject("SELECT activation_sequence FROM turnover_rent_rules WHERE id = ?",
                Long.class, id);
    }

    private void approveRule(String id, String from, long minAmount) {
        TestSecurityActors.asPlatformAdmin();
        rule(id, from, null, "active", true, null, minAmount);
        jdbc.update("UPDATE turnover_rent_rules SET updated_by = 'rent_history_drafter' WHERE id = ?", id);
        writeService.activateDraft("turnover_rent_rule", id, "rent_history_approver");
    }

    private FinanceService manualService() {
        AuditEventService audit = new AuditEventService(new InMemoryAuditEventRepository());
        AuditedOperationExecutor operations = new AuditedOperationExecutor(
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()), audit);
        return new FinanceService(repository, operations, audit, mock(AuthorizationService.class),
                new BusinessDayResolver(4));
    }

    private TurnoverRentBillRequest request(String ruleId, String key) {
        return new TurnoverRentBillRequest(key, "期间回归", List.of(), ruleId,
                LocalDate.parse("2026-08-01"), LocalDate.parse("2026-08-31"), LocalDate.parse("2026-09-10"));
    }

    private void rule(String id, String from, String to, String status, boolean draft,
            String archiveReason, long minAmount) {
        jdbc.update("""
                INSERT INTO turnover_rent_rules
                    (id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent,
                     effective_from, effective_to, status, is_draft, archive_reason)
                VALUES (?, 'rent_history_contract', 'rent_history_merchant', '历史期间回归规则',
                        1000, ?, ?, ?, ?, ?, ?)
                """, id, minAmount, LocalDate.parse(from), to == null ? null : LocalDate.parse(to),
                status, draft, archiveReason);
    }
}
