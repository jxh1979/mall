package com.complex.platform.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class FinanceJdbcIntegrationTests {

    @Autowired
    private JdbcFinanceRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesFinanceTablesAndSeedsPermissions() {
        assertThat(regclass("ar_ap_items")).isEqualTo("ar_ap_items");
        assertThat(regclass("ar_ap_item_sources")).isEqualTo("ar_ap_item_sources");
        assertThat(regclass("ar_ap_payments")).isEqualTo("ar_ap_payments");
        assertThat(regclass("finance_payment_intents")).isEqualTo("finance_payment_intents");
        assertThat(regclass("finance_offline_payment_confirmations")).isEqualTo("finance_offline_payment_confirmations");
        assertThat(regclass("finance_refunds")).isEqualTo("finance_refunds");
        assertThat(regclass("finance_daily_closes")).isEqualTo("finance_daily_closes");
        assertThat(regclass("finance_reconciliation_batches")).isEqualTo("finance_reconciliation_batches");
        assertThat(regclass("finance_reconciliation_differences")).isEqualTo("finance_reconciliation_differences");

        assertThat(permissionCodes()).contains(
                "finance.ar_ap.view",
                "finance.ar_ap.manage",
                "finance.payment.register",
                "finance.daily_close.lock",
                "finance.payment_intent.create",
                "finance.reconciliation.view",
                "openplatform.reconcile.execute",
                "finance.refund.create"
        );
        assertThat(platformAdminPermissionCodes()).contains(
                "finance.ar_ap.manage",
                "finance.payment.register",
                "finance.daily_close.lock",
                "finance.payment_intent.create",
                "finance.refund.create"
        );
    }

    @Test
    void createAndListArApItemPersistsFormalLedgerFields() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveArApItem(new ArApItemRecord(
                "arap_jdbc",
                "ARAP-JDBC-001",
                "receivable",
                "merchant_jdbc",
                "order",
                "order_jdbc",
                128800L,
                0L,
                128800L,
                "CNY",
                LocalDate.parse("2026-06-30"),
                "2026-06",
                "pending_collection",
                now,
                now
        ));
        repository.saveArApItemSource(new ArApItemSourceRecord(
                "arap_src_jdbc",
                "arap_jdbc",
                "order",
                "order_jdbc",
                128800L,
                now
        ));

        assertThat(repository.listArApItems())
                .extracting(
                        ArApItemRecord::id,
                        ArApItemRecord::direction,
                        ArApItemRecord::counterpartyId,
                        ArApItemRecord::amountCent,
                        ArApItemRecord::unpaidAmountCent,
                        ArApItemRecord::status
                )
                .contains(tuple("arap_jdbc", "receivable", "merchant_jdbc", 128800L, 128800L, "pending_collection"));
    }

    @Test
    void paymentUpdatesPersistedBalances() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveArApItem(new ArApItemRecord(
                "arap_jdbc_payment",
                "ARAP-JDBC-002",
                "receivable",
                "merchant_jdbc",
                "order",
                "order_jdbc_payment",
                128800L,
                0L,
                128800L,
                "CNY",
                LocalDate.parse("2026-06-30"),
                "2026-06",
                "pending_collection",
                now,
                now
        ));
        repository.saveArApPayment(new ArApPaymentRecord(
                "arap_pay_jdbc",
                "arap_jdbc_payment",
                28800L,
                "bank_transfer",
                Instant.parse("2026-06-14T01:00:00Z"),
                "bank_flow_jdbc",
                Instant.parse("2026-06-14T01:00:01Z")
        ));
        repository.updateArApItemLedger(new ArApItemRecord(
                "arap_jdbc_payment",
                "ARAP-JDBC-002",
                "receivable",
                "merchant_jdbc",
                "order",
                "order_jdbc_payment",
                128800L,
                28800L,
                100000L,
                "CNY",
                LocalDate.parse("2026-06-30"),
                "2026-06",
                "partial_paid",
                now,
                Instant.parse("2026-06-14T01:00:01Z")
        ));

        assertThat(repository.findArApItemById("arap_jdbc_payment"))
                .get()
                .extracting(ArApItemRecord::paidAmountCent, ArApItemRecord::unpaidAmountCent, ArApItemRecord::status)
                .containsExactly(28800L, 100000L, "partial_paid");
    }

    @Test
    void reconciliationEndpointsCanListEmptyAndPersistedRows() {
        assertThat(repository.listReconciliationBatches()).isNotNull();
        assertThat(repository.listReconciliationDifferences()).isNotNull();

        repository.saveReconciliationBatch(new ReconciliationBatchRecord(
                "recon_batch_jdbc",
                "RB-JDBC-001",
                "project",
                "project_jdbc",
                LocalDate.parse("2026-06-14"),
                "created",
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        repository.saveReconciliationDifference(new ReconciliationDifferenceRecord(
                "recon_diff_jdbc",
                "recon_batch_jdbc",
                "amount_mismatch",
                "payment_jdbc",
                100L,
                "open",
                Instant.parse("2026-06-14T00:00:00Z")
        ));

        assertThat(repository.listReconciliationBatches())
                .extracting(ReconciliationBatchRecord::batchNo, ReconciliationBatchRecord::status)
                .contains(tuple("RB-JDBC-001", "created"));
        assertThat(repository.listReconciliationDifferences())
                .extracting(ReconciliationDifferenceRecord::differenceType, ReconciliationDifferenceRecord::businessRef)
                .contains(tuple("amount_mismatch", "payment_jdbc"));
    }

    private String regclass(String tableName) {
        return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
    }

    private List<String> permissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM permissions
                 ORDER BY permission_code
                """, String.class);
    }

    private List<String> platformAdminPermissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM role_permissions
                 WHERE role_id = 'role_platform_admin'
                 ORDER BY permission_code
                """, String.class);
    }
}
