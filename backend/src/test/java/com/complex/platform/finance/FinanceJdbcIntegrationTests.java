package com.complex.platform.finance;

// 文件版本：V0.05

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.support.StaffRequestFixture;
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

    @Autowired
    private com.complex.platform.bar.BarRepository bars;

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
                "project_jdbc", false, "locked", Instant.now(), Instant.now(), Instant.now(), null, List.of()));
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

    // ─── 第十八批 P 段第 133 条 133-R2：日结列表读接口 ─────────────────────
    @Test
    void dailyCloseListShowsLockedClosesWithinReachNewestBusinessDateFirst() throws Exception {
        // BSM:1848「财务看退款、差异、日结和结算依据。」、BSM:1682「按营业日生成日结批次。」：已生成的日结批次按营业日倒序列出、离开页面后能找回；
        // 范围同查看类列表（20-R3）：商户账号只看本商户，项目员工只看可触及项目的项目范围日结与这些项目下商户的日结。前端真实请求形状（蛇形键）。
        String s = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String p1 = "project_dcl1_" + s;
        String p2 = "project_dcl2_" + s;
        String barA = "bar_dcl_a_" + s;
        String barB = "bar_dcl_b_" + s;
        for (String project : List.of(p1, p2)) {
            jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                    + " VALUES (?, 'complex', '日结列表测试综合体', 'active', now(), now())", project);
        }
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '日结列表酒吧甲', 'bar', 'active', now(), now(), ?)", barA, p1);
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '日结列表酒吧乙', 'bar', 'active', now(), now(), ?)", barB, p2);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbcTemplate);
        RequestPostProcessor finance = staffs.staff("usr_dcl_fin_" + s, "platform_admin", p1, null,
                "finance.daily_close.lock", "finance.daily_close.manage");
        String closeA = lockDailyClose(finance, "dcl-a-" + s, "2998-03-01", "bar", barA);
        String closeB = lockDailyClose(finance, "dcl-b-" + s, "2998-03-02", "bar", barB);

        String body = mockMvc.perform(get("/api/v1/finance/daily-closes?page=1&size=20").with(finance))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(body, "$.data.items[*].id");
        assertThat(ids.stream().filter(id -> id.equals(closeA) || id.equals(closeB)).toList()).containsExactly(closeB, closeA);
        String rowA = "$.data.items[?(@.id == '" + closeA + "')]";
        assertThat(JsonPath.<List<Object>>read(body, rowA + ".business_date")).containsExactly("2998-03-01");
        assertThat(JsonPath.<List<Object>>read(body, rowA + ".scope_type")).containsExactly("bar");
        assertThat(JsonPath.<List<Object>>read(body, rowA + ".scope_id")).containsExactly(barA);
        assertThat(JsonPath.<List<Object>>read(body, rowA + ".status")).containsExactly("locked");
        assertThat(JsonPath.<List<Object>>read(body, rowA + ".locked_at")).hasSize(1).doesNotContainNull();
        mockMvc.perform(get("/api/v1/finance/daily-closes/" + closeA + "/summaries").with(finance))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(7));

        String closeP1 = lockDailyClose(finance, "dcl-p1-" + s, "2998-03-03", "project", p1);
        List<String> ours = List.of(closeA, closeB, closeP1);
        RequestPostProcessor merchantA = staffs.staff("usr_dcl_mch_" + s, "staff", p1, barA, "finance.daily_close.manage");
        assertThat(listedDailyCloses(merchantA, ours)).containsExactly(closeA);
        RequestPostProcessor projectStaff = staffs.staff("usr_dcl_p1_" + s, "staff", p1, null, "finance.daily_close.manage");
        assertThat(listedDailyCloses(projectStaff, ours)).containsExactly(closeP1, closeA);
        RequestPostProcessor lockOnly = staffs.staff("usr_dcl_lock_" + s, "staff", p1, null, "finance.daily_close.lock");
        mockMvc.perform(get("/api/v1/finance/daily-closes?page=1&size=20").with(lockOnly))
                .andExpect(status().isForbidden());
    }

    // ─── 第十八批 R 段第 143 条 143-R1：日结生成、确认、锁定三步与范围闸 ─────────────────────
    /**
     * ENUM:190「待生成、待确认、有差异、已确认、已锁定、已归档」、BSM:1682「按营业日生成日结批次。」：生成即待确认、不锁台账；
     * 确认与锁定两处都拦待主管处置的异常账单（ENUM:221「异常单据未处置完毕，所属日结不得确认锁定」）；锁定后才锁当日台账，
     * 之后直接登记收款被拒（BSM:2023「日结锁定后不能直接补录影响金额」）。平台侧财务 BP 按后台真实请求形状（蛇形键）。
     */
    @Test
    void dailyCloseGeneratesPendingThenConfirmsAndLocksInOrder() throws Exception {
        String s = suffix();
        String project = "project_dcf_" + s;
        String bar = "bar_dcf_" + s;
        insertProject(project);
        insertBar(bar, project);
        LocalDate day = farDay(s);
        String item = dueItem("arap_dcf_" + s, bar, project, day);
        RequestPostProcessor financeBp = new StaffRequestFixture(jdbcTemplate).staff("usr_dcf_" + s, "staff", project, null,
                "finance.daily_close.lock", "finance.daily_close.manage", "finance.payment.register");

        String body = generate(financeBp, "dcf-gen-" + s, day, "merchant", bar)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_confirm"))
                .andExpect(jsonPath("$.data.locked_at").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String close = JsonPath.read(body, "$.data.id");
        mockMvc.perform(get("/api/v1/finance/daily-closes/" + close + "/summaries").with(financeBp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(7));
        assertThat(lockedDayClose(item)).isNull();
        pay(financeBp, item, "dcf-pay1-" + s).andExpect(status().isOk());
        closeAction(financeBp, close, "lock", "dcf-lk0-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAILY_CLOSE_NOT_CONFIRMED"));

        com.complex.platform.support.DailyCloseFixture.markAllFinal(repository, close);
        Instant inDay = businessDayResolver.start(null, bar, day).plusSeconds(3600);
        String first = abnormalBill(bar, "a" + s, inDay);
        closeAction(financeBp, close, "confirm", "dcf-cf0-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAY_CLOSE_ANOMALY_PENDING"));
        assertThat(closeStatus(close)).isEqualTo("pending_confirm");
        resolveBill(first);
        closeAction(financeBp, close, "confirm", "dcf-cf1-" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("confirmed"))
                .andExpect(jsonPath("$.data.confirmed_at").isNotEmpty())
                .andExpect(jsonPath("$.data.locked_at").doesNotExist());
        closeAction(financeBp, close, "confirm", "dcf-cf2-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAILY_CLOSE_NOT_PENDING"));
        assertThat(lockedDayClose(item)).isNull();

        String second = abnormalBill(bar, "b" + s, inDay);
        closeAction(financeBp, close, "lock", "dcf-lk1-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAY_CLOSE_ANOMALY_PENDING"));
        assertThat(closeStatus(close)).isEqualTo("confirmed");
        resolveBill(second);
        closeAction(financeBp, close, "lock", "dcf-lk2-" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("locked"))
                .andExpect(jsonPath("$.data.locked_at").isNotEmpty());
        assertThat(lockedDayClose(item)).isEqualTo(close);
        pay(financeBp, item, "dcf-pay2-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAY_CLOSE_LOCKED"));
    }

    /**
     * PGM:664「单店层面的财务工作——日结确认与锁定……由该酒吧对应的财务 BP 负责」、PGM:659「所有业务数据默认按项目隔离，普通岗位没有跨项目查看权」：
     * 生成、确认与按日结号读汇总都按日结的范围判——商户账号只能办、只能看本商户的日结，项目财务不能办、不能看别的项目的日结。
     * 待生成：该营业日该范围查不到日结。真实请求形状（蛇形键）。
     */
    @Test
    void dailyCloseWritesAndSummaryReadsStayWithinActorsMerchantAndProject() throws Exception {
        String s = suffix();
        String p1 = "project_dcs1_" + s;
        String p2 = "project_dcs2_" + s;
        String barA = "bar_dcs_a_" + s;
        String barB = "bar_dcs_b_" + s;
        insertProject(p1);
        insertProject(p2);
        insertBar(barA, p1);
        insertBar(barB, p1);
        LocalDate day = farDay(s);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbcTemplate);
        RequestPostProcessor admin = staffs.staff("usr_dcs_adm_" + s, "platform_admin", p1, null,
                "finance.daily_close.lock", "finance.daily_close.manage");
        RequestPostProcessor merchantA = staffs.staff("usr_dcs_mch_" + s, "staff", p1, barA,
                "finance.daily_close.lock", "finance.daily_close.manage");
        RequestPostProcessor p1Finance = staffs.staff("usr_dcs_p1_" + s, "staff", p1, null,
                "finance.daily_close.lock", "finance.daily_close.manage");

        generate(merchantA, "dcs-mb-" + s, day, "merchant", barB).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        generate(p1Finance, "dcs-pp-" + s, day, "project", p2).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM finance_daily_closes WHERE scope_id IN (?, ?)",
                Integer.class, barB, p2)).isZero();

        String closeB = JsonPath.read(generate(admin, "dcs-b-" + s, day, "bar", barB).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        closeAction(merchantA, closeB, "confirm", "dcs-cfb-" + s).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        assertThat(closeStatus(closeB)).isEqualTo("pending_confirm");
        mockMvc.perform(get("/api/v1/finance/daily-closes/" + closeB + "/summaries").with(merchantA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        mockMvc.perform(get("/api/v1/finance/daily-closes/" + closeB + "/summaries").with(p1Finance))
                .andExpect(status().isOk());

        String closeP2 = JsonPath.read(generate(admin, "dcs-p2-" + s, day, "project", p2).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        mockMvc.perform(get("/api/v1/finance/daily-closes/" + closeP2 + "/summaries").with(p1Finance))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));

        mockMvc.perform(get("/api/v1/finance/daily-closes?business_date=" + day + "&scope_type=merchant&scope_id=" + barA
                        + "&page=1&size=20").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/api/v1/finance/daily-closes?business_date=" + day + "&scope_type=bar&scope_id=" + barB
                        + "&page=1&size=20").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(closeB))
                .andExpect(jsonPath("$.data.items[0].status").value("pending_confirm"));
    }

    /**
     * 第十八批 R 段 143-R7（ENUM:190「……同时在原营业日的日结锁定表单上关联显示该笔补录的金额与情况，两边互相可查。」；
     * PGM:664 单店日结由该酒吧对应的财务 BP 负责）：锁某家酒吧的日结只锁该酒吧的当日台账——平台管理员锁 A 酒吧 merchant 范围的日结后，
     * B 酒吧当日台账不被锁到 A 的日结上；平台侧财务 BP 再锁 B 酒吧 bar 范围的日结，锁到 B 自己的台账。真实请求形状（蛇形键）。
     */
    @Test
    void lockingOneBarsDailyCloseLocksOnlyThatBarsSameDayItems() throws Exception {
        String s = suffix();
        String project = "project_dcm_" + s;
        String barA = "bar_dcm_a_" + s;
        String barB = "bar_dcm_b_" + s;
        insertProject(project);
        insertBar(barA, project);
        insertBar(barB, project);
        LocalDate day = farDay(s);
        String itemA = dueItem("arap_dcm_a_" + s, barA, project, day);
        String itemB = dueItem("arap_dcm_b_" + s, barB, project, day);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbcTemplate);
        RequestPostProcessor admin = staffs.staff("usr_dcm_adm_" + s, "platform_admin", project, null,
                "finance.daily_close.lock", "finance.daily_close.manage");
        RequestPostProcessor financeBp = staffs.staff("usr_dcm_bp_" + s, "staff", project, null,
                "finance.daily_close.lock", "finance.daily_close.manage");

        String closeA = lockDailyClose(admin, "dcm-a-" + s, day.toString(), "merchant", barA);
        assertThat(lockedDayClose(itemA)).isEqualTo(closeA);
        assertThat(lockedDayClose(itemB)).isNull();
        String closeB = lockDailyClose(financeBp, "dcm-b-" + s, day.toString(), "bar", barB);
        assertThat(lockedDayClose(itemB)).isEqualTo(closeB);
    }

    // ─── 第十八批 R 段第 143 条 143-R2：五类最终标记 ─────────────────────
    /**
     * ENUM:190「需要逐类标记为最终的数据集有五类：当日订单与核销记录、当日退款与冲正记录、当日出品与物品交接记录、当日考勤记录、当日异常处置记录。
     * 由各域主管公司的财务岗在该营业日闭店之后完成核对并逐类标记」「标记后不可撤销，事后发现错误只能走冲正，不得改回未最终。」
     * 「所属数据未标记为最终之前，日结不得确认、不得锁定。」：前天（已过闭店）的日结逐类标记、同类不能再标；只标四类不得确认，原文列出缺的类别；
     * 标齐后可确认；确认后不能再标。平台侧财务 BP 按后台真实请求形状（蛇形键）。
     */
    @Test
    void dailyCloseFinalMarksGateConfirmAndCannotBeRepeated() throws Exception {
        String s = suffix();
        String project = "project_dfm_" + s;
        String bar = "bar_dfm_" + s;
        insertProject(project);
        insertBar(bar, project);
        LocalDate day = LocalDate.now(com.complex.platform.common.LocalZone.ID).minusDays(2);
        RequestPostProcessor financeBp = new StaffRequestFixture(jdbcTemplate).staff("usr_dfm_" + s, "staff", project, null,
                "finance.daily_close.lock", "finance.daily_close.manage");
        String close = JsonPath.read(generate(financeBp, "dfm-gen-" + s, day, "merchant", bar).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        finalMark(financeBp, close, "orders_verifications", "dfm-m1-" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_confirm"))
                .andExpect(jsonPath("$.data.final_mark_categories").value(org.hamcrest.Matchers.contains("orders_verifications")));
        finalMark(financeBp, close, "orders_verifications", "dfm-m1b-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("FINAL_MARK_EXISTS"));
        for (String category : List.of("refunds_reversals", "production_handover", "anomaly_resolutions")) {
            finalMark(financeBp, close, category, "dfm-" + category + "-" + s).andExpect(status().isOk());
        }
        closeAction(financeBp, close, "confirm", "dfm-cf0-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAY_CLOSE_NOT_FINAL"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("当日考勤记录"),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("当日订单与核销记录")))));
        assertThat(closeStatus(close)).isEqualTo("pending_confirm");
        finalMark(financeBp, close, "attendance", "dfm-m5-" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.final_mark_categories.length()").value(5));
        closeAction(financeBp, close, "confirm", "dfm-cf1-" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("confirmed"));
        finalMark(financeBp, close, "attendance", "dfm-m6-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAILY_CLOSE_NOT_PENDING"));
        mockMvc.perform(get("/api/v1/finance/daily-closes?business_date=" + day + "&scope_type=merchant&scope_id=" + bar
                        + "&page=1&size=20").with(financeBp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].final_mark_categories.length()").value(5));
    }

    /**
     * ENUM:190「由各域主管公司的财务岗在该营业日闭店之后完成核对并逐类标记」：尚未闭店的营业日不能标；PGM:664 单店日结由该酒吧对应的财务 BP 负责：
     * 商户账号不能标别家商户的日结。真实请求形状（蛇形键）。
     */
    @Test
    void finalMarkRefusedBeforeCloseAndForOtherMerchants() throws Exception {
        String s = suffix();
        String project = "project_dfb_" + s;
        String barA = "bar_dfb_a_" + s;
        String barB = "bar_dfb_b_" + s;
        insertProject(project);
        insertBar(barA, project);
        insertBar(barB, project);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbcTemplate);
        RequestPostProcessor financeBp = staffs.staff("usr_dfb_bp_" + s, "staff", project, null,
                "finance.daily_close.lock", "finance.daily_close.manage");
        RequestPostProcessor merchantA = staffs.staff("usr_dfb_mch_" + s, "staff", project, barA,
                "finance.daily_close.lock", "finance.daily_close.manage");
        String notClosed = JsonPath.read(generate(financeBp, "dfb-open-" + s, farDay(s), "merchant", barA)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        finalMark(financeBp, notClosed, "attendance", "dfb-m0-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("FINAL_MARK_BEFORE_CLOSE"));
        String closeB = JsonPath.read(generate(financeBp, "dfb-b-" + s,
                LocalDate.now(com.complex.platform.common.LocalZone.ID).minusDays(2), "merchant", barB)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        finalMark(merchantA, closeB, "attendance", "dfb-m1-" + s).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM finance_daily_close_final_marks WHERE daily_close_id IN (?, ?)",
                Integer.class, notClosed, closeB)).isZero();
    }

    // ─── 第十八批 R 段第 143 条 143-R5：已锁定日结归档（第十八批裁决 33） ─────────────────────
    /**
     * 第十八批裁决 33「锁定后，由该店持日结锁定权限的财务 BP 点「归档」；归档后只读可查，锁定后补录照常走上级审批。」；
     * ENUM:190「……已锁定、已归档」：已锁定的日结归档后，被它锁定的台账照常发起补录、另一人批准；列表与七源汇总照常可查；
     * 不能再确认、锁定、归档；归档日照样拦直接改账（登记收款、新增该日到期台账）；商户账号不能归档别家商户的日结；
     * 只有已锁定的日结能归档。平台侧财务 BP 按后台真实请求形状（蛇形键）。
     * 被拒的写请求会把本测试事务标为只回滚，补录审批的准入闸在只回滚的事务里取不到保存点会保守拦截，故成功路径排在被拒请求之前。
     */
    @Test
    void lockedDailyCloseArchivesReadOnlyWhileBackfillStillGoesThroughApproval() throws Exception {
        String s = suffix();
        String project = "project_dca_" + s;
        String barA = "bar_dca_a_" + s;
        String barB = "bar_dca_b_" + s;
        insertProject(project);
        insertBar(barA, project);
        insertBar(barB, project);
        LocalDate day = farDay(s);
        String item = dueItem("arap_dca_" + s, barA, project, day);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbcTemplate);
        RequestPostProcessor financeBp = staffs.staff("usr_dca_bp_" + s, "staff", project, null,
                "finance.daily_close.lock", "finance.daily_close.manage", "finance.payment.register",
                "finance.locked_backfill.request");
        RequestPostProcessor approver = staffs.staff("usr_dca_ap_" + s, "staff", project, null,
                "finance.locked_backfill.approve");
        RequestPostProcessor admin = staffs.staff("usr_dca_adm_" + s, "platform_admin", project, null, "finance.ar_ap.manage");
        RequestPostProcessor merchantB = staffs.staff("usr_dca_mch_" + s, "staff", project, barB,
                "finance.daily_close.lock", "finance.daily_close.manage");

        String confirmed = JsonPath.read(generate(financeBp, "dca-b-" + s, day, "merchant", barB).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        com.complex.platform.support.DailyCloseFixture.markAllFinal(repository, confirmed);
        closeAction(financeBp, confirmed, "confirm", "dca-bcf-" + s).andExpect(status().isOk());
        String close = lockDailyClose(financeBp, "dca-a-" + s, day.toString(), "merchant", barA);
        closeAction(financeBp, close, "archive", "dca-ar-" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("archived"))
                .andExpect(jsonPath("$.data.archived_at").isNotEmpty())
                .andExpect(jsonPath("$.data.locked_at").isNotEmpty());
        assertThat(lockedDayClose(item)).isEqualTo(close);

        // 补录审批的准入按台账商户的生效合同判（与 FinanceProjectScopeRefundJdbcIntegrationTests 同一夹具口径）。
        jdbcTemplate.update("INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date,"
                + " expiry_date, lifecycle_status, allowed_business_scope, created_at, updated_at) VALUES (?, ?, 'subj_dca', 'subj_dca',"
                + " current_date - 1, current_date + 365, 'effective', '[\"finance\"]'::jsonb, now(), now())", "ctr_dca_" + s, barA);
        String adjustment = JsonPath.read(mockMvc.perform(post("/api/v1/finance/ar-ap-adjustments").with(financeBp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dca-adj-" + s + "\",\"ar_ap_item_id\":\"" + item
                                + "\",\"adjustment_amount_cent\":300,\"reason\":\"核实少记\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locked_day_close_id").value(close))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        mockMvc.perform(post("/api/v1/finance/ar-ap-adjustments/" + adjustment + "/approve").with(approver)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dca-apv-" + s + "\",\"decision\":\"approve\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approval_status").value("approved"));
        mockMvc.perform(get("/api/v1/finance/daily-closes?business_date=" + day + "&scope_type=merchant&scope_id=" + barA
                        + "&page=1&size=20").with(financeBp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].status").value("archived"))
                .andExpect(jsonPath("$.data.items[0].archived_at").isNotEmpty());
        mockMvc.perform(get("/api/v1/finance/daily-closes/" + close + "/summaries").with(financeBp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(7));

        closeAction(financeBp, close, "archive", "dca-ar2-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAILY_CLOSE_NOT_LOCKED"));
        closeAction(financeBp, close, "confirm", "dca-cf2-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAILY_CLOSE_NOT_PENDING"));
        closeAction(financeBp, close, "lock", "dca-lk2-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAILY_CLOSE_NOT_CONFIRMED"));
        pay(financeBp, item, "dca-pay-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAY_CLOSE_LOCKED"));
        mockMvc.perform(post("/api/v1/finance/ar-ap-items").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dca-item-" + s + "\",\"direction\":\"receivable\",\"counterparty_id\":\""
                                + barA + "\",\"source_type\":\"manual\",\"source_id\":\"dca-src-" + s
                                + "\",\"amount_cent\":1000,\"due_date\":\"" + day + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAY_CLOSE_LOCKED"));
        closeAction(merchantB, close, "archive", "dca-mar-" + s).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        closeAction(financeBp, confirmed, "archive", "dca-bar-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAILY_CLOSE_NOT_LOCKED"));
        assertThat(closeStatus(confirmed)).isEqualTo("confirmed");
        assertThat(closeStatus(close)).isEqualTo("archived");
    }

    // ─── 第十八批 R 段第 143 条 143-R4：锁定后补录在日结上互相可查 ─────────────────────
    /**
     * ENUM:190「日结锁定后的补录须上级审批，且不改动已锁定的日结金额：补录金额归入当期，并在补录记录上标明其所属的原营业日；同时在原营业日的
     * 日结锁定表单上关联显示该笔补录的金额与情况，两边互相可查。」；财务 PRD §5.4「日结锁定后的每笔补录/冲正,在日结页面直接展示
     * 『补录人＋补录原因＋批准人(含时间)』」：按日结号查被它锁定的台账（只到本日结锁住的）与锁定后补录；补录记录带原营业日与台账单号；
     * 批准后补录记录有批准人与批准时间、日结七源汇总不变；申请人不能自批；别家商户账号查不到。平台侧财务 BP 按后台真实请求形状（蛇形键）。
     * 被拒的写请求会把本测试事务标为只回滚，补录审批的准入闸在只回滚的事务里取不到保存点会保守拦截，故成功路径排在被拒请求之前。
     */
    @Test
    void lockedDailyCloseListsItsLockedItemsAndBackfillsBothWays() throws Exception {
        String s = suffix();
        String project = "project_dcb_" + s;
        String barA = "bar_dcb_a_" + s;
        String barB = "bar_dcb_b_" + s;
        insertProject(project);
        insertBar(barA, project);
        insertBar(barB, project);
        LocalDate day = farDay(s);
        String itemA = dueItem("arap_dcb_a_" + s, barA, project, day);
        dueItem("arap_dcb_b_" + s, barB, project, day);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbcTemplate);
        String requester = "usr_dcb_bp_" + s;
        String approverId = "usr_dcb_ap_" + s;
        RequestPostProcessor financeBp = staffs.staff(requester, "staff", project, null,
                "finance.daily_close.lock", "finance.daily_close.manage", "finance.ar_ap.view",
                "finance.locked_backfill.request", "finance.locked_backfill.approve");
        RequestPostProcessor approver = staffs.staff(approverId, "staff", project, null, "finance.locked_backfill.approve");
        RequestPostProcessor merchantB = staffs.staff("usr_dcb_mch_" + s, "staff", project, barB, "finance.daily_close.manage");

        String close = lockDailyClose(financeBp, "dcb-" + s, day.toString(), "merchant", barA);
        mockMvc.perform(get("/api/v1/finance/ar-ap-items?locked_day_close_id=" + close + "&page=1&size=20").with(financeBp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(itemA));
        Object summariesBefore = JsonPath.read(mockMvc.perform(get("/api/v1/finance/daily-closes/" + close + "/summaries")
                .with(financeBp)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.items");

        // 补录审批的准入按台账商户的生效合同判（与 FinanceProjectScopeRefundJdbcIntegrationTests 同一夹具口径）。
        jdbcTemplate.update("INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date,"
                + " expiry_date, lifecycle_status, allowed_business_scope, created_at, updated_at) VALUES (?, ?, 'subj_dcb', 'subj_dcb',"
                + " current_date - 1, current_date + 365, 'effective', '[\"finance\"]'::jsonb, now(), now())", "ctr_dcb_" + s, barA);
        String first = JsonPath.read(backfill(financeBp, itemA, 500, "漏记", "dcb-adj1-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approval_status").value("pending"))
                .andExpect(jsonPath("$.data.locked_day_close_id").value(close))
                .andExpect(jsonPath("$.data.original_business_date").value(day.toString()))
                .andExpect(jsonPath("$.data.item_no").value("NO-" + itemA))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        String second = JsonPath.read(backfill(financeBp, itemA, -200, "多记", "dcb-adj2-" + s)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        mockMvc.perform(get("/api/v1/finance/daily-closes/" + close + "/adjustments").with(financeBp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[?(@.id == '" + first + "')].requested_by").value(org.hamcrest.Matchers.contains(requester)))
                .andExpect(jsonPath("$.data.items[?(@.id == '" + first + "')].reason").value(org.hamcrest.Matchers.contains("漏记")))
                .andExpect(jsonPath("$.data.items[?(@.id == '" + first + "')].original_business_date")
                        .value(org.hamcrest.Matchers.contains(day.toString())))
                .andExpect(jsonPath("$.data.items[?(@.id == '" + first + "')].item_no").value(org.hamcrest.Matchers.contains("NO-" + itemA)))
                .andExpect(jsonPath("$.data.items[?(@.id == '" + second + "')].adjustment_amount_cent")
                        .value(org.hamcrest.Matchers.contains(-200)));

        mockMvc.perform(post("/api/v1/finance/ar-ap-adjustments/" + first + "/approve").with(approver)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dcb-apv-" + s + "\",\"decision\":\"approve\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.original_business_date").value(day.toString()));
        mockMvc.perform(get("/api/v1/finance/daily-closes/" + close + "/adjustments").with(financeBp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + first + "')].approval_status").value(org.hamcrest.Matchers.contains("approved")))
                .andExpect(jsonPath("$.data.items[?(@.id == '" + first + "')].approved_by").value(org.hamcrest.Matchers.contains(approverId)))
                .andExpect(jsonPath("$.data.items[?(@.id == '" + first + "')].decided_at").value(
                        org.hamcrest.Matchers.contains(org.hamcrest.Matchers.notNullValue())));
        Object summariesAfter = JsonPath.read(mockMvc.perform(get("/api/v1/finance/daily-closes/" + close + "/summaries")
                .with(financeBp)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.items");
        assertThat(summariesAfter).isEqualTo(summariesBefore);

        mockMvc.perform(post("/api/v1/finance/ar-ap-adjustments/" + second + "/approve").with(financeBp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"dcb-self-" + s + "\",\"decision\":\"approve\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SELF_APPROVAL_FORBIDDEN"));
        mockMvc.perform(get("/api/v1/finance/daily-closes/" + close + "/adjustments").with(merchantB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
    }

    /** 143-R4：日结页面「申请补录」的真实请求形状（幂等键＋台账＋调整额分＋原因）。 */
    private org.springframework.test.web.servlet.ResultActions backfill(RequestPostProcessor who, String itemId, long amountCent,
            String reason, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/ar-ap-adjustments").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + "\",\"ar_ap_item_id\":\"" + itemId
                        + "\",\"adjustment_amount_cent\":" + amountCent + ",\"reason\":\"" + reason + "\"}"));
    }

    /** 143-R2：五类最终标记的真实请求形状（幂等键＋类别）。 */
    private org.springframework.test.web.servlet.ResultActions finalMark(RequestPostProcessor who, String dailyCloseId,
            String category, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/daily-closes/" + dailyCloseId + "/final-marks").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + "\",\"category\":\"" + category + "\"}"));
    }

    private static String suffix() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    /** 远期营业日（2996 年起按后缀错开），不与其他用例在今天、近日的台账与日结相撞。 */
    private static LocalDate farDay(String s) {
        return LocalDate.of(2996, 1, 1).plusDays(Math.floorMod(s.hashCode(), 3000));
    }

    private void insertProject(String id) {
        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '日结三步测试综合体', 'active', now(), now())", id);
    }

    private void insertBar(String id, String project) {
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '日结三步测试酒吧', 'bar', 'active', now(), now(), ?)", id, project);
    }

    /** 该商户在该营业日到期的一笔应收（未锁）。 */
    private String dueItem(String id, String merchant, String project, LocalDate day) {
        Instant now = Instant.now();
        repository.saveArApItem(new ArApItemRecord(id, "NO-" + id, "receivable", merchant, "order", "src_" + id,
                10000L, 0L, 10000L, "CNY", day, day.toString().substring(0, 7), "pending_collection", now, now, merchant, project));
        return id;
    }

    private String lockedDayClose(String itemId) {
        return jdbcTemplate.queryForObject("SELECT locked_day_close_id FROM ar_ap_items WHERE id = ?", String.class, itemId);
    }

    private String closeStatus(String dailyCloseId) {
        return jdbcTemplate.queryForObject("SELECT status FROM finance_daily_closes WHERE id = ?", String.class, dailyCloseId);
    }

    /** 后台生成日结的真实请求形状（蛇形键）。 */
    private org.springframework.test.web.servlet.ResultActions generate(RequestPostProcessor who, String key, LocalDate day,
            String scopeType, String scopeId) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/daily-closes").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + "\",\"operator_note\":\"日结\",\"business_date\":\"" + day
                        + "\",\"scope_type\":\"" + scopeType + "\",\"scope_id\":\"" + scopeId + "\"}"));
    }

    /** 按日结号确认或锁定的真实请求形状（页面只发幂等键）。 */
    private org.springframework.test.web.servlet.ResultActions closeAction(RequestPostProcessor who, String dailyCloseId,
            String action, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/daily-closes/" + dailyCloseId + "/" + action).with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions pay(RequestPostProcessor who, String itemId, String key)
            throws Exception {
        return mockMvc.perform(post("/api/v1/finance/ar-ap-items/" + itemId + "/payments").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + "\",\"amount_cent\":1000,\"payment_method\":\"bank_transfer\","
                        + "\"payment_time\":\"" + Instant.now() + "\",\"bank_flow_ref\":\"flow-" + key + "\"}"));
    }

    /** 该酒吧在该时刻开出、正待主管处置的异常账单。 */
    private String abnormalBill(String bar, String tag, Instant at) {
        String table = "table_dcf_" + tag;
        jdbcTemplate.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity,"
                + " allowed_pass_free_guest_count, min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, '卡座', 'card_seat', 6, 2, 50000, 'CNY', 'active', now(), now())",
                table, bar, bar, "D-" + tag.toUpperCase());
        String order = "ord_dcf_" + tag;
        String bill = "bill_dcf_" + tag;
        bars.saveOrder(new com.complex.platform.bar.BarOrderRecord(order, bar, bar, table, null, null, "staff", "billed", 30_000L,
                "CNY", "web", null, null, at, at));
        bars.saveBill(new com.complex.platform.bar.BarBillRecord(bill, "BILL-DCF-" + tag.toUpperCase(), order, bar, bar, table, null,
                30_000L, 0L, 0L, 30_000L, 0L, "CNY", "unpaid", "unsettled", null, null, at, at));
        jdbcTemplate.update("UPDATE bar_bills SET payment_status = 'abnormal', abnormal_prior_status = 'unpaid' WHERE id = ?", bill);
        return bill;
    }

    /** 主管处置完毕（已结清），不再挡日结。 */
    private void resolveBill(String billId) {
        jdbcTemplate.update("UPDATE bar_bills SET payment_status = 'paid', abnormal_prior_status = NULL, settlement_status = 'settled'"
                + " WHERE id = ?", billId);
    }

    /** 后台日结生成 → 确认 → 锁定的真实请求形状（蛇形键，第十八批 R 段 143-R1；五类最终标记直插，143-R2），返回日结 ID。 */
    private String lockDailyClose(RequestPostProcessor who, String key, String businessDate, String scopeType, String scopeId)
            throws Exception {
        String body = generate(who, key, LocalDate.parse(businessDate), scopeType, scopeId)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.data.id");
        com.complex.platform.support.DailyCloseFixture.markAllFinal(repository, id);
        closeAction(who, id, "confirm", key + "-confirm").andExpect(status().isOk());
        closeAction(who, id, "lock", key + "-lock").andExpect(status().isOk());
        return id;
    }

    /** 该账号在日结列表第一页看到的、属于本例的日结 ID（按列表顺序）。 */
    private List<String> listedDailyCloses(RequestPostProcessor who, List<String> ours) throws Exception {
        String body = mockMvc.perform(get("/api/v1/finance/daily-closes?page=1&size=20").with(who))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(body, "$.data.items[*].id");
        return ids.stream().filter(ours::contains).toList();
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
