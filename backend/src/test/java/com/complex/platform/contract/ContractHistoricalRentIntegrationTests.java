package com.complex.platform.contract;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.contract.dto.ContractRentBillRequest;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.FinanceService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
class ContractHistoricalRentIntegrationTests {

    @Autowired private JdbcContractRepository repository;
    @Autowired private FinanceService financeService;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void rentBillLocksHistoricalRuleAndRepeatingPeriodDoesNotRecalculateOriginalBill() {
        contract("turnover_only");
        rule("contract_history_old", "2026-01-01", "inactive", 10000);
        rule("contract_history_new", "2026-09-01", "active", 20000);
        ContractService service = service();

        ContractRentBillRecord bill = service.generateRentBill("contract_history",
                new ContractRentBillRequest("history_bill", "2026-08"));
        assertThat(bill.amountCent()).isEqualTo(10000);
        assertThat(jdbc.queryForObject(
                "SELECT turnover_rule_id FROM contract_rent_bills WHERE id = ?", String.class, bill.id()))
                .isEqualTo("contract_history_old");

        jdbc.update("UPDATE turnover_rent_rules SET archive_reason = '审批下线' WHERE id = 'contract_history_old'");
        assertThat(service.generatePeriodBills("contract_history",
                new ContractRentBillRequest("history_bill_repeat", "2026-08"))).isEmpty();
        assertThat(jdbc.queryForObject(
                "SELECT amount_cent FROM contract_rent_bills WHERE id = ?", Long.class, bill.id())).isEqualTo(10000L);
        assertThat(jdbc.queryForObject(
                "SELECT turnover_rule_id FROM contract_rent_bills WHERE id = ?", String.class, bill.id()))
                .isEqualTo("contract_history_old");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM contract_rent_bills WHERE contract_id = 'contract_history'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void missingTurnoverRuleCannotSilentlyFallBackToFixedRent() {
        contract("sum");

        assertThatThrownBy(() -> service().generateRentBill("contract_history",
                new ContractRentBillRequest("missing_rule_bill", "2026-08")))
                .isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM contract_rent_bills WHERE contract_id = 'contract_history'", Integer.class))
                .isZero();
    }

    @Test
    void fixedRentKeepsWorkingWithoutTurnoverRule() {
        contract("fixed_only");

        ContractRentBillRecord bill = service().generateRentBill("contract_history",
                new ContractRentBillRequest("fixed_rule_bill", "2026-08"));

        assertThat(bill.amountCent()).isEqualTo(5000);
    }

    private ContractService service() {
        return new ContractService(repository, mock(AuditEventService.class), mock(AuthorizationService.class),
                null, null, null, financeService, null, null);
    }

    private void contract(String mode) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        repository.saveContract(new ContractRecord("contract_history", "rent_history_merchant", "subj_1", null, "subj_1",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2027-12-31"),
                Map.of("baseRentCent", 5000, "rentBillingMode", mode),
                "fixed_ratio", "none", 0L, Map.of(), List.of(), null, null, List.of(), "effective",
                "proj_1", null, null, null, now, now));
    }

    private void rule(String id, String from, String status, long minAmount) {
        jdbc.update("""
                INSERT INTO turnover_rent_rules
                    (id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent,
                     effective_from, status, is_draft)
                VALUES (?, 'contract_history', 'rent_history_merchant', '合同期间回归规则', 1000, ?, ?, ?, FALSE)
                """, id, minAmount, LocalDate.parse(from), status);
    }
}
