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

    // ─── P1-A：财务补项目级隔离(与储值同范式) FAILING FIRST ─────────────────────
    @Test
    void listArApItemsByProjectExcludesOtherProjectsRows() {
        // 项目 A 的财务角色只应看到本项目应收应付；项目 B 的行必须被 project_id 谓词挡在外面。
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveArApItem(projItem("arap_projA", "ARAP-PROJA", "merchant_a", "project_A", now));
        repository.saveArApItem(projItem("arap_projB", "ARAP-PROJB", "merchant_b", "project_B", now));

        // 项目 A scope(merchantId=null, projectId=project_A) 的真分页列表只含 project_A 行。
        var page = repository.listArApItemsPaged(null, "project_A", null, 1, 50);
        assertThat(page.items()).extracting(ArApItemRecord::id)
                .contains("arap_projA")
                .doesNotContain("arap_projB");
    }

    @Test
    void listArApItemsByProjectRoundTripsProjectIdColumn() {
        // project_id 列往返：saveArApItem 写入、mapArApItem 读回，非丢字段。
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveArApItem(projItem("arap_proj_rt", "ARAP-PROJ-RT", "merchant_a", "project_A", now));
        assertThat(repository.findArApItemById("arap_proj_rt"))
                .get().extracting(ArApItemRecord::projectId).isEqualTo("project_A");
    }

    // ─── P2-A：日结六源汇总必须按 scope 取数 FAILING FIRST ──────────────────────
    @Test
    void aggregateOfficialTicketByMerchantScopeSumsOnlyThatMerchant() {
        LocalDate businessDate = LocalDate.parse("2026-06-18");
        Instant from = Instant.parse("2026-06-18T04:00:00Z");
        Instant to = Instant.parse("2026-06-19T04:00:00Z");
        String tt = seedTicketType();
        insertTicketOrder("to_m_a", tt, "merchant_scope_a", 30000L, businessDate, "admin");
        insertTicketOrder("to_m_b", tt, "merchant_scope_b", 70000L, businessDate, "admin");

        // 商户 A 锁定日结:官方售票源金额应=A的30000,而非全平台100000。
        FinanceRepository.DailyCloseSourceTotals scoped =
                repository.aggregateOfficialTicket(businessDate, from, to, "merchant", "merchant_scope_a");
        assertThat(scoped.amountCent()).isEqualTo(30000L);
        assertThat(scoped.orderCount()).isEqualTo(1);
    }

    @Test
    void aggregateThirdpartyReflowByMerchantScopeIsZeroNotPlatformTotal() {
        // 第三方回流源无 merchant 维度:商户锁定日结时该源应归零(不属于任何单一商户),而非渗入全平台金额。
        LocalDate businessDate = LocalDate.parse("2026-06-18");
        Instant from = Instant.parse("2026-06-18T04:00:00Z");
        Instant to = Instant.parse("2026-06-19T04:00:00Z");
        insertThirdpartySync("ios_scope_1", 120000L, Instant.parse("2026-06-18T12:00:00Z"));

        // 平台口径能看到 120000;商户口径必须归零。
        assertThat(repository.aggregateThirdpartyReflow(businessDate, from, to).amountCent()).isEqualTo(120000L);
        FinanceRepository.DailyCloseSourceTotals scoped =
                repository.aggregateThirdpartyReflow(businessDate, from, to, "merchant", "merchant_scope_a");
        assertThat(scoped.amountCent()).isZero();
    }

    private ArApItemRecord projItem(String id, String no, String merchant, String project, Instant now) {
        return new ArApItemRecord(id, no, "receivable", merchant, "order", "src_" + id,
                10000L, 0L, 10000L, "CNY", LocalDate.parse("2026-06-30"), "2026-06",
                "pending_collection", now, now, merchant, project);
    }

    private String seedTicketType() {
        String id = "tt_scope_test";
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required,
                                          status, created_at, updated_at, sold_quantity)
                VALUES (?, ?, 'fixed', false, 'active', now(), now(), 0)
                ON CONFLICT (id) DO NOTHING
                """, id, "scope-test-type");
        return id;
    }

    private void insertTicketOrder(String id, String ticketTypeId, String merchantId,
                                   long amountCent, LocalDate visitDate, String channel) {
        jdbcTemplate.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, visit_date, sales_channel,
                                           amount_cent, currency, pay_status, issue_status, status,
                                           merchant_id, created_at, updated_at)
                VALUES (?, ?, ?, 1, ?, ?, ?, 'CNY', 'paid', 'issued', 'active', ?, now(), now())
                """, id, "ORD-" + id, ticketTypeId, visitDate, channel, amountCent, merchantId);
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
