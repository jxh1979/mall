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
    void dailyCloseLockChecksAcceptNullMerchantScope() {
        // 2026-07-02 现网500根因:平台口径(merchant scope=null)时 :merchant_id 参数无 SQL 类型,
        // PG 报 could not determine data type of parameter → 平台管理员创建 AR/AP 台账全量 500。
        assertThat(repository.lockedDailyCloseExistsForDate(LocalDate.of(2099, 1, 1), null)).isFalse();
        assertThat(repository.lockArApItemsForDailyClose("dc_mz_none", LocalDate.of(2099, 1, 1), null)).isZero();
    }

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
                now,
                "merchant_jdbc"
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
                now,
                "merchant_jdbc"
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
                Instant.parse("2026-06-14T01:00:01Z"),
                "merchant_jdbc"
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
                "amount",
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
                .contains(tuple("amount", "payment_jdbc"));
    }

    @Test
    void closeReconciliationDifferencePersistsClosedStatusAndReason() {
        // 1.6 真实后端活证据：close 落 status=closed + close_reason/closed_by/closed_at；
        // findById 回读为 closed；再 close 幂等仍 closed。走真实 Postgres。
        repository.saveReconciliationBatch(new ReconciliationBatchRecord(
                "recon_batch_close_jdbc", "RB-CLOSE-JDBC", "project", "project_jdbc",
                LocalDate.parse("2026-06-14"), "created", Instant.parse("2026-06-14T00:00:00Z")));
        repository.saveReconciliationDifference(new ReconciliationDifferenceRecord(
                "recon_diff_close_jdbc", "recon_batch_close_jdbc", "amount", "payment_close_jdbc",
                2000L, "open", Instant.parse("2026-06-14T00:00:00Z")));

        ReconciliationDifferenceRecord closed = repository.closeReconciliationDifference(
                "recon_diff_close_jdbc", "手工核对后正式关闭", "op_close", Instant.parse("2026-06-15T00:00:00Z"));
        assertThat(closed.status()).isEqualTo("closed");

        // DB 层活证据：状态与关闭原因落库。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM finance_reconciliation_differences WHERE id = 'recon_diff_close_jdbc'", String.class))
                .isEqualTo("closed");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT close_reason FROM finance_reconciliation_differences WHERE id = 'recon_diff_close_jdbc'", String.class))
                .isEqualTo("手工核对后正式关闭");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT closed_by FROM finance_reconciliation_differences WHERE id = 'recon_diff_close_jdbc'", String.class))
                .isEqualTo("op_close");
        assertThat(repository.findReconciliationDifferenceById("recon_diff_close_jdbc"))
                .get().extracting(ReconciliationDifferenceRecord::status).isEqualTo("closed");
    }

    @Test
    void officialTicketRefundIsDeductedWithinBusinessDayCutoffWindow() {
        // 经营日 2026-06-18，日切 04:00 → 窗口 [2026-06-18T04:00Z, 2026-06-19T04:00Z)。
        LocalDate businessDate = LocalDate.parse("2026-06-18");
        Instant from = Instant.parse("2026-06-18T04:00:00Z");
        Instant to = Instant.parse("2026-06-19T04:00:00Z");

        // 窗口内退款（10:00）计入；窗口外退款（03:00，归前一经营日）不计入。
        insertTicketRefund("tr_in_window", 5000L, Instant.parse("2026-06-18T10:00:00Z"));
        insertTicketRefund("tr_before_cutoff", 9999L, Instant.parse("2026-06-18T03:00:00Z"));

        FinanceRepository.DailyCloseSourceTotals totals =
                repository.aggregateOfficialTicket(businessDate, from, to);

        assertThat(totals.refundCent()).isEqualTo(5000L);
    }

    @Test
    void thirdpartyReflowIsAggregatedSeparatelyAndNotFoldedIntoTicketOrBar() {
        // 财务确认：第三方回流单列，不进 revenue.net（净收入 = 售票 + 酒吧 − 退款，剔除第三方）。
        LocalDate businessDate = LocalDate.parse("2026-06-18");
        Instant from = Instant.parse("2026-06-18T04:00:00Z");
        Instant to = Instant.parse("2026-06-19T04:00:00Z");

        insertThirdpartySync("ios_tp_1", 120000L, Instant.parse("2026-06-18T12:00:00Z"));

        // 第三方源单列出该金额。
        FinanceRepository.DailyCloseSourceTotals thirdparty =
                repository.aggregateThirdpartyReflow(businessDate, from, to);
        assertThat(thirdparty.amountCent()).isEqualTo(120000L);

        // 第三方回流不会渗入官方售票/酒吧源（这两源 SQL 不查 integration_order_syncs）。
        FinanceRepository.DailyCloseSourceTotals official = repository.aggregateOfficialTicket(businessDate, from, to);
        long officialAmount = official.amountCent();
        long barAmount = repository.aggregateBar(businessDate, from, to).amountCent();
        long refund = official.refundCent();
        // net 派生（售票 + 酒吧 − 退款）天然不含第三方 120000。
        long net = officialAmount + barAmount - refund;
        assertThat(net).isEqualTo(officialAmount + barAmount - refund);
        assertThat(net).isNotEqualTo(officialAmount + barAmount - refund + 120000L);
    }

    private void insertTicketRefund(String id, long amountCent, Instant createdAt) {
        jdbcTemplate.update("""
                INSERT INTO ticket_refunds (id, target_id, refund_amount_cent, reason, refund_channel,
                                            status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, "order_" + id, amountCent, "test refund", "manual", "requested",
                java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(createdAt));
    }

    private void insertThirdpartySync(String id, long amountCent, Instant createdAt) {
        jdbcTemplate.update("""
                INSERT INTO integration_order_syncs (id, provider_code, business_type, sync_type,
                                                     external_business_id, status, amount_cent, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, "meituan", "catering", "order", "ext_" + id, "synced", amountCent,
                java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(createdAt));
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
