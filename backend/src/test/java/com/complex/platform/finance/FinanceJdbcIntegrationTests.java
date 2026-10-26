package com.complex.platform.finance;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.BusinessDayResolver;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class FinanceJdbcIntegrationTests {

    @Autowired
    private JdbcFinanceRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BusinessDayResolver businessDayResolver;

    @Test
    void dailyCloseLockChecksAcceptNullMerchantScope() {
        // 2026-07-02 现网500根因:平台口径(merchant scope=null)时 :merchant_id 参数无 SQL 类型,
        // PG 报 could not determine data type of parameter → 平台管理员创建 AR/AP 台账全量 500。
        LocalDate date = LocalDate.of(2099, 1, 1);
        assertThat(repository.lockedDailyCloseExistsForDate(date, null)).isFalse();
        int pendingItems = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ar_ap_items WHERE due_date = ? AND locked_day_close_id IS NULL",
                Integer.class, date);
        // 使用真实日结外键，允许其他提交型集成测试已在同日留下收款记录。
        repository.saveDailyClose(new DailyCloseRecord("dc_mz_null_scope", date, "project",
                "project_jdbc", false, "locked", Instant.now(), Instant.now()));
        assertThat(repository.lockArApItemsForDailyClose("dc_mz_null_scope", date, null)).isEqualTo(pendingItems);
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
        // 经营日 2026-06-18，日切 04:00 按所在地（北京）时间读 →
        // 窗口 = 北京 [06-18 04:00, 06-19 04:00) = [2026-06-17T20:00Z, 2026-06-18T20:00Z)。
        LocalDate businessDate = LocalDate.parse("2026-06-18");
        Instant from = Instant.parse("2026-06-17T20:00:00Z");
        Instant to = Instant.parse("2026-06-18T20:00:00Z");

        // 窗口内退款（北京 10:00 = 06-18T02:00Z）计入；窗口外退款（北京 03:00 = 06-17T19:00Z，归前一经营日）不计入。
        insertTicketRefund("tr_in_window", 5000L, Instant.parse("2026-06-18T02:00:00Z"));
        insertTicketRefund("tr_before_cutoff", 9999L, Instant.parse("2026-06-17T19:00:00Z"));

        FinanceRepository.DailyCloseSourceTotals totals =
                repository.aggregateOfficialTicket(businessDate, from, to);

        assertThat(totals.refundCent()).isEqualTo(5000L);
    }

    @Test
    void onlySuccessRefundsCountByEffectiveTimeNotRequestedRejectedFailedCancelled() {
        // AUD-018 门禁：八状态逐一插入，只有 success 且 succeeded_at 落窗冲减收入；其余一律不计。
        LocalDate businessDate = LocalDate.parse("2026-06-20");
        Instant from = Instant.parse("2026-06-19T20:00:00Z");
        Instant to = Instant.parse("2026-06-20T20:00:00Z");
        Instant inWindow = Instant.parse("2026-06-20T10:00:00Z");
        insertTicketRefund("tr_ok", 5000L, inWindow, "success");
        for (String st : java.util.List.of("requested", "review", "approved", "processing", "rejected", "failed", "cancelled")) {
            insertTicketRefund("tr_" + st, 7000L, inWindow, st);
        }
        assertThat(repository.aggregateOfficialTicket(businessDate, from, to).refundCent()).isEqualTo(5000L);
    }

    @Test
    void thirdpartyReflowIsAggregatedSeparatelyAndNotFoldedIntoTicketOrBar() {
        // 财务确认：第三方回流单列，不进 revenue.net（净收入 = 售票 + 酒吧 − 退款，剔除第三方）。
        LocalDate businessDate = LocalDate.parse("2026-06-18");
        Instant from = Instant.parse("2026-06-17T20:00:00Z");
        Instant to = Instant.parse("2026-06-18T20:00:00Z");

        insertMerchant("mch_tp_jdbc");
        insertThirdpartySync("ios_tp_1", "catering", "order", "synced", "mch_tp_jdbc", 120000L,
                Instant.parse("2026-06-18T12:00:00Z"));

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
        Instant from = Instant.parse("2026-06-17T20:00:00Z");
        Instant to = Instant.parse("2026-06-18T20:00:00Z");
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
    void officialAndChannelTicketsAndRefundsAreDisjointAtPlatformScope() {
        LocalDate date = LocalDate.parse("2026-06-22");
        Instant from = Instant.parse("2026-06-21T20:00:00Z");
        Instant to = Instant.parse("2026-06-22T20:00:00Z");
        String type = seedTicketType();
        insertTicketOrder("direct_split", type, "merchant_split", 10000L, date, "admin");
        insertTicketOrder("channel_split", type, "merchant_split", 20000L, date, "channel");
        insertTicketRefundForOrder("direct_refund", "direct_split", "merchant_split", 1000L, from, "success");
        insertTicketRefundForOrder("channel_refund", "channel_split", "merchant_split", 4000L, from, "success");

        assertThat(repository.aggregateOfficialTicket(date, from, to))
                .isEqualTo(new FinanceRepository.DailyCloseSourceTotals(10000L, 1000L, 1));
        assertThat(repository.aggregateChannelTicket(date, from, to))
                .isEqualTo(new FinanceRepository.DailyCloseSourceTotals(20000L, 4000L, 1));
    }

    @Test
    void officialAndChannelTicketsAndRefundsStayDisjointWithinMerchantScope() {
        LocalDate date = LocalDate.parse("2026-06-23");
        Instant from = Instant.parse("2026-06-22T20:00:00Z");
        Instant to = Instant.parse("2026-06-23T20:00:00Z");
        String type = seedTicketType();
        insertTicketOrder("direct_scope", type, "merchant_split", 10000L, date, "miniapp");
        insertTicketOrder("channel_scope", type, "merchant_split", 20000L, date, "channel");
        insertTicketOrder("other_scope", type, "merchant_other", 90000L, date, "admin");
        insertTicketRefundForOrder("direct_scope_refund", "direct_scope", "merchant_split", 1000L, from, "success");
        insertTicketRefundForOrder("channel_scope_refund", "channel_scope", "merchant_split", 4000L, from, "success");
        insertTicketRefundForOrder("other_scope_refund", "other_scope", "merchant_other", 8000L, from, "success");

        assertThat(repository.aggregateOfficialTicket(date, from, to, "merchant", "merchant_split"))
                .isEqualTo(new FinanceRepository.DailyCloseSourceTotals(10000L, 1000L, 1));
        assertThat(repository.aggregateChannelTicket(date, from, to, "merchant", "merchant_split"))
                .isEqualTo(new FinanceRepository.DailyCloseSourceTotals(20000L, 4000L, 1));
    }

    @Test
    void aggregateThirdpartyReflowByMerchantScopeIsZeroNotPlatformTotal() {
        // 第三方回流源无 merchant 维度:商户锁定日结时该源应归零(不属于任何单一商户),而非渗入全平台金额。
        LocalDate businessDate = LocalDate.parse("2026-06-18");
        Instant from = Instant.parse("2026-06-17T20:00:00Z");
        Instant to = Instant.parse("2026-06-18T20:00:00Z");
        insertMerchant("mch_tp_jdbc");
        insertThirdpartySync("ios_scope_1", "catering", "order", "synced", "mch_tp_jdbc", 120000L,
                Instant.parse("2026-06-18T12:00:00Z"));

        // 平台口径能看到 120000;商户口径必须归零。
        assertThat(repository.aggregateThirdpartyReflow(businessDate, from, to).amountCent()).isEqualTo(120000L);
        FinanceRepository.DailyCloseSourceTotals scoped =
                repository.aggregateThirdpartyReflow(businessDate, from, to, "merchant", "merchant_scope_a");
        assertThat(scoped.amountCent()).isZero();
    }

    // ─── OBS-13-07：日结第三方回流源与官方/渠道售票源口径 ─────────────────────
    @Test
    void thirdpartyReflowCountsOnlyAttributedCateringRetailBookedOrders() {
        // BSM L1548「回流订单按该映射认定商户，映射缺失的回流单挂差异，不得自动入账」；L1704 餐饮、零售回流；与看板 revenue.thirdparty 同谓词。
        LocalDate date = LocalDate.parse("2097-06-24");
        seedReflowRows("obs1307a", Instant.parse("2097-06-24T04:00:00Z"));
        assertThat(repository.aggregateThirdpartyReflow(date,
                Instant.parse("2097-06-23T20:00:00Z"), Instant.parse("2097-06-24T20:00:00Z")))
                .isEqualTo(new FinanceRepository.DailyCloseSourceTotals(15000L, 0L, 2));
    }

    @Test
    void channelReflowTicketWithConcreteChannelCodeCountsAsChannelTicketNotOfficial() {
        // BSM L1681 官方 App/小程序与第三方渠道分开汇总：第三方渠道回流出的票记具体渠道码，只进渠道售票源、不进官方售票源。
        LocalDate date = LocalDate.parse("2097-06-25");
        Instant from = Instant.parse("2097-06-24T20:00:00Z");
        Instant to = Instant.parse("2097-06-25T20:00:00Z");
        seedSelfAndChannelTickets("obs1307b", "merchant_obs1307b", date, from);
        FinanceRepository.DailyCloseSourceTotals official = new FinanceRepository.DailyCloseSourceTotals(10000L, 0L, 1);
        FinanceRepository.DailyCloseSourceTotals channel = new FinanceRepository.DailyCloseSourceTotals(25000L, 4000L, 2);
        assertThat(repository.aggregateOfficialTicket(date, from, to)).isEqualTo(official);
        assertThat(repository.aggregateChannelTicket(date, from, to)).isEqualTo(channel);
        assertThat(repository.aggregateOfficialTicket(date, from, to, "merchant", "merchant_obs1307b")).isEqualTo(official);
        assertThat(repository.aggregateChannelTicket(date, from, to, "merchant", "merchant_obs1307b")).isEqualTo(channel);
    }

    @Test
    void dailyCloseSummariesCountGroupBuyTicketOnceAndSkipUnmappedReflow() throws Exception {
        // 前端真实请求形状（蛇形键）：日结六源汇总里团购票只进渠道售票源一次，映射缺失回流与团购票回流不进第三方回流源。
        LocalDate date = LocalDate.parse("2097-06-26");
        Instant inDay = businessDayResolver.start(null, null, date).plusSeconds(3600);
        String prefix = "obs1307e_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        seedReflowRows(prefix, inDay);
        seedSelfAndChannelTickets(prefix, prefix + "_mch_a", date, inDay);
        RequestPostProcessor finance = financeActor("usr_" + prefix);
        String body = mockMvc.perform(post("/api/v1/finance/daily-closes").with(finance)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"" + prefix + "\",\"operator_note\":\"日结\","
                                + "\"business_date\":\"2097-06-26\",\"scope_type\":\"project\","
                                + "\"scope_id\":\"project_" + prefix + "\",\"force_close\":false}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String dailyCloseId = JsonPath.read(body, "$.data.id");
        mockMvc.perform(get("/api/v1/finance/daily-closes/" + dailyCloseId + "/summaries").with(finance))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.source_stream == 'thirdparty_reflow')].amount_cent").value(15000))
                .andExpect(jsonPath("$.data.items[?(@.source_stream == 'thirdparty_reflow')].order_count").value(2))
                .andExpect(jsonPath("$.data.items[?(@.source_stream == 'channel_ticket')].amount_cent").value(25000))
                .andExpect(jsonPath("$.data.items[?(@.source_stream == 'official_ticket')].amount_cent").value(10000));
    }

    /** 两家已归属商户的餐饮/零售已入账回流（10000＋5000），另四条不该入第三方回流源：映射缺失、团购票、退款、失败。 */
    private void seedReflowRows(String prefix, Instant inDay) {
        String merchantA = prefix + "_mch_a";
        String merchantB = prefix + "_mch_b";
        insertMerchant(merchantA);
        insertMerchant(merchantB);
        insertThirdpartySync(prefix + "_c", "catering", "order", "synced", merchantA, 10000L, inDay);
        insertThirdpartySync(prefix + "_r", "retail", "order", "synced", merchantB, 5000L, inDay);
        insertThirdpartySync(prefix + "_unmapped", "catering", "order", "synced", null, 7000L, inDay);
        insertThirdpartySync(prefix + "_ticket", "ticket", "order", "synced", merchantA, 9000L, inDay);
        insertThirdpartySync(prefix + "_refund", "catering", "refund", "synced", merchantA, 2000L, inDay);
        insertThirdpartySync(prefix + "_failed", "catering", "order", "failed", merchantA, 3000L, inDay);
    }

    /** 自营小程序票 10000；第三方渠道回流出的美团票 20000（成功退款 4000）与抖音票 5000。 */
    private void seedSelfAndChannelTickets(String prefix, String merchant, LocalDate date, Instant refundAt) {
        String type = seedTicketType();
        insertTicketOrder(prefix + "_miniapp", type, merchant, 10000L, date, "miniapp");
        insertTicketOrder(prefix + "_meituan", type, merchant, 20000L, date, "meituan");
        insertTicketOrder(prefix + "_douyin", type, merchant, 5000L, date, "douyin");
        insertTicketRefundForOrder(prefix + "_mt_refund", prefix + "_meituan", merchant, 4000L, refundAt, "success");
    }

    private RequestPostProcessor financeActor(String userId) {
        String role = "role_" + userId;
        jdbcTemplate.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at)"
                + " VALUES (?, ?, '日结回归', 'project', 'active', now())", role, role);
        for (String permission : List.of("finance.daily_close.lock", "finance.daily_close.manage")) {
            jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, permission);
        }
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", userId, userId);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        AccessActor actor = new AccessActor(userId, userId, "platform_admin", "org_1", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
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

    @Test
    void depositRefundLedgerRowDecrementsRunningBalance() {
        // #3② 底座：退押金写 deposit_ledger refund 行后运行余额下降（多合同退租按余额消费封顶所依赖）。
        Instant now = Instant.parse("2026-07-06T00:00:00Z");
        repository.saveDepositLedger(new DepositLedgerRecord(
                "dep_collect_jdbc", "mch_refund_jdbc", "collect", 40000L, 40000L, null, "init", now));
        assertThat(repository.currentDepositBalanceForUpdate("mch_refund_jdbc")).isEqualTo(40000L);
        // 退款行关联到刚挂的应付（满足 deposit_ledger_ar_ap_item_id_fkey）：先种真 ar_ap_items 行。
        repository.saveArApItem(projItem("arap_x", "ARAP-X", "mch_refund_jdbc", "proj_x", now));
        repository.saveDepositLedger(new DepositLedgerRecord(
                "dep_refund_jdbc", "mch_refund_jdbc", "refund", 25000L, 15000L, "arap_x", "退租应退押金结算", now));
        assertThat(repository.currentDepositBalance("mch_refund_jdbc")).isEqualTo(15000L);
    }

    private void insertTicketOrder(String id, String ticketTypeId, String merchantId,
                                   long amountCent, LocalDate visitDate, String channel) {
        jdbcTemplate.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, visit_date, sales_channel,
                                           amount_cent, currency, pay_status, issue_status, status,
                                           merchant_id, created_at, updated_at)
                VALUES (?, ?, ?, 1, ?, ?, ?, 'CNY', 'paid', 'issued', 'created', ?, now(), now())
                """, id, "ORD-" + id, ticketTypeId, visitDate, channel, amountCent, merchantId);
    }

    private void insertTicketRefund(String id, long amountCent, Instant effectiveAt) {
        // AUD-018：有效退款=success + succeeded_at；默认插 success 复现"生效落窗即计入"。
        insertTicketRefund(id, amountCent, effectiveAt, "success");
    }

    private void insertTicketRefund(String id, long amountCent, Instant effectiveAt, String status) {
        String orderId = "order_" + id;
        insertTicketOrder(orderId, seedTicketType(), "merchant_refund", amountCent,
                effectiveAt.atZone(java.time.ZoneOffset.UTC).toLocalDate(), "admin");
        insertTicketRefundForOrder(id, orderId, "merchant_refund", amountCent, effectiveAt, status);
    }

    private void insertTicketRefundForOrder(String id, String orderId, String merchantId,
                                            long amountCent, Instant effectiveAt, String status) {
        java.sql.Timestamp ts = java.sql.Timestamp.from(effectiveAt);
        java.sql.Timestamp succeeded = "success".equals(status) ? ts : null;
        jdbcTemplate.update("""
                INSERT INTO ticket_refunds (id, target_id, merchant_id, refund_amount_cent, reason, refund_channel,
                                            status, created_at, updated_at, succeeded_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, orderId, merchantId, amountCent, "test refund", "manual", status,
                ts, ts, succeeded);
    }

    private void insertMerchant(String id) {
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)"
                + " VALUES (?, ?, 'catering', 'active', now(), now()) ON CONFLICT (id) DO NOTHING", id, id);
    }

    private void insertThirdpartySync(String id, String businessType, String syncType, String status,
                                      String merchantId, long amountCent, Instant createdAt) {
        jdbcTemplate.update("""
                INSERT INTO integration_order_syncs (id, provider_code, business_type, sync_type, external_business_id,
                                                     status, amount_cent, merchant_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, "meituan", businessType, syncType, "ext_" + id, status, amountCent, merchantId,
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
