package com.complex.platform.analytics;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.analytics.AnalyticsRecords.MetricDetailRows;
import com.complex.platform.amusement.AmusementBusinessDayService;
import com.complex.platform.amusement.AmusementBusinessDayTestFixture;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * KPI 统一明细「数字=明细合计」真库一致性（指标登记簿首批 5 指标 + 第二批酒吧域 7 指标，2026-07-18 方案A）。
 * 铁律验证：每个指标的合计校验值（summaryValue）与逐页明细行按同一谓词同源 —— 分页翻完后
 * 明细行求和/计数必须恒等于合计；且与既有看板 ETL 聚合查询（sumTicketRevenueCents /
 * energyConsumptionByType / arOverdueAgingByMerchant / occupancyAreas）同口径。
 * 共享 dev 库有真实数据 → 断言一律用「基线差值」口径（照 AnalyticsJdbcIntegrationTests 惯例）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class MetricDetailConsistencyJdbcIntegrationTests {

    private static final LocalDate DAY = LocalDate.parse("2026-06-20");

    /**
     * 可复跑范式：种子 id 全部经 sid() 带每次运行的随机后缀——共享 complex_test 上历史失败可能留下残留行
     * （事务回滚不保证在所有 runner 配置下发生），固定 id/唯一键的种子会撞残留必炸。基线差值断言本就容忍库内既有数据。
     */
    private final String run = java.util.UUID.randomUUID().toString().substring(0, 8);

    private String sid(String stem) {
        return stem + "_" + run;
    }

    @Autowired
    private JdbcAnalyticsRepository repository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private com.complex.platform.common.BusinessDayResolver businessDayResolver;

    @Autowired
    private AmusementBusinessDayService amusementBusinessDays;

    @Autowired
    private SupervisorReviewService supervisorReviews;

    private String amusementComplex;

    @Test
    void ticketRevenueSummaryEqualsPagedDetailSumAndEtlAggregate() {
        Instant start = businessDayResolver.startInstant("ticketing", DAY);
        Instant end = businessDayResolver.endInstant("ticketing", DAY);
        BigDecimal baseline = repository.ticketRevenueDetail(start, end, 1, 0).summaryValue();

        insertTicketOrder("to_mdc_1", 10000, "paid");
        insertTicketOrder("to_mdc_2", 2500, "paid");
        insertTicketOrder("to_mdc_unpaid", 999, "pending"); // 未支付 → 不进口径

        MetricDetailRows first = repository.ticketRevenueDetail(start, end, 200, 0);
        assertThat(first.summaryValue().subtract(baseline)).isEqualByComparingTo("12500");
        // 数字=明细合计：翻完所有页,Σ amountCent == summaryValue。
        assertThat(sumLong(collectAllPages((limit, offset) -> repository.ticketRevenueDetail(start, end, limit, offset)), "amountCent"))
                .isEqualByComparingTo(first.summaryValue());
        // 与看板 ETL 聚合同源同口径。
        assertThat(repository.sumTicketRevenueCents(DAY, "ticketing")).isEqualByComparingTo(first.summaryValue());
    }

    @Test
    void reconciliationDifferenceCountEqualsPagedDetailCount() {
        MetricDetailRows baseline = repository.reconciliationDifferenceDetail(null, null, 1, 0);

        insertReconBatch("rb_mdc_1");
        insertReconDifference("rd_mdc_1", "rb_mdc_1", 300, "open");
        insertReconDifference("rd_mdc_2", "rb_mdc_1", -150, "closed"); // 已关闭也计数(与对账差异页合计一致)

        MetricDetailRows after = repository.reconciliationDifferenceDetail(null, null, 1, 0);
        assertThat(after.summaryValue().subtract(baseline.summaryValue())).isEqualByComparingTo("2");
        // 可加(计数)口径:合计=总行数=翻页明细行数。
        assertThat(after.summaryValue()).isEqualByComparingTo(BigDecimal.valueOf(after.totalRows()));
        assertThat(collectAllPages((limit, offset) -> repository.reconciliationDifferenceDetail(null, null, limit, offset)))
                .hasSize((int) after.totalRows());
    }

    @Test
    void electricitySummaryEqualsPagedDetailSumAndEtlAggregate() {
        Instant start = businessDayResolver.startInstant("energy", DAY);
        Instant end = businessDayResolver.endInstant("energy", DAY);
        BigDecimal baseline = repository.electricityReadingDetail(start, end, 1, 0).summaryValue();

        insertMeter("em_mdc_e", "electricity");
        insertMeter("em_mdc_w", "water");
        insertReading("er_mdc_1", "em_mdc_e", "700.5", "0", DAY + "T10:00:00Z");
        insertReading("er_mdc_2", "em_mdc_e", "300", "700.5", DAY + "T11:00:00Z");
        insertReading("er_mdc_w", "em_mdc_w", "88", "0", DAY + "T10:00:00Z"); // 水表 → 不进电量口径

        MetricDetailRows after = repository.electricityReadingDetail(start, end, 200, 0);
        assertThat(after.summaryValue().subtract(baseline)).isEqualByComparingTo("1000.5");
        assertThat(sumDecimal(collectAllPages((limit, offset) -> repository.electricityReadingDetail(start, end, limit, offset)), "consumption"))
                .isEqualByComparingTo(after.summaryValue());
        BigDecimal etl = repository.energyConsumptionByType(start, end).getOrDefault("electricity", BigDecimal.ZERO);
        assertThat(etl).isEqualByComparingTo(after.summaryValue());
    }

    @Test
    void energyRentableAreaDenominatorEqualsPagedDetailSumAndExcludesUnavailable() {
        BigDecimal baseline = repository.rentableSpaceDetail(1, 0).summaryValue();

        insertSpace("sp_mdc_shop", "shop", "120", "active");
        insertSpace("sp_mdc_booth", "booth", "30", "active");
        insertSpace("sp_mdc_off", "shop", "999", "unavailable"); // 停用 → 不进分母
        insertSpace("sp_mdc_pub", "public_area", "500", "active"); // 公共区域 → 不进分母

        MetricDetailRows after = repository.rentableSpaceDetail(200, 0);
        assertThat(after.summaryValue().subtract(baseline)).isEqualByComparingTo("150");
        assertThat(sumDecimal(collectAllPages((limit, offset) -> repository.rentableSpaceDetail(limit, offset)), "areaSquareMeter"))
                .isEqualByComparingTo(after.summaryValue());
        // 能源仍用可租面积，不改为出租率使用的总铺位面积。
        assertThat(repository.occupancyAreas(DAY).rentableArea()).isEqualByComparingTo(after.summaryValue());
    }

    @Test
    void arBalanceSummaryEqualsPagedDetailSumAndEtlAgingTotal() {
        BigDecimal baseline = repository.arOpenReceivableDetail(DAY, 1, 0).summaryValue();

        insertArItem("ar_mdc_1", 70000, DAY.minusDays(10)); // 逾期未清 → 进口径
        insertArItem("ar_mdc_2", 5000, DAY.minusDays(45));
        insertArItem("ar_mdc_future", 8888, DAY.plusDays(5)); // 未到期 → 不进口径

        MetricDetailRows after = repository.arOpenReceivableDetail(DAY, 200, 0);
        assertThat(after.summaryValue().subtract(baseline)).isEqualByComparingTo("75000");
        assertThat(sumLong(collectAllPages((limit, offset) -> repository.arOpenReceivableDetail(DAY, limit, offset)), "unpaidAmountCent"))
                .isEqualByComparingTo(after.summaryValue());
        // 与看板 ETL ar.balance(四桶账龄合计)同谓词:Σ全部桶Σ全部商户 == summaryValue。
        BigDecimal agingTotal = repository.arOverdueAgingByMerchant(DAY).values().stream()
                .flatMap(bucket -> bucket.values().stream())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(agingTotal).isEqualByComparingTo(after.summaryValue());
    }

    // ==== 第二批（酒吧域）：谓词逐字同各页面既有取数 ====

    @Test
    void barRevenueSummaryEqualsPagedDetailSumAndEtlAggregate() {
        Instant start = businessDayResolver.startInstant("bar", DAY);
        Instant end = businessDayResolver.endInstant("bar", DAY);
        BigDecimal baseline = repository.barSettlementRevenueDetail(start, end, 1, 0).summaryValue();

        insertBarTable("bt_mdc_r");
        insertBarOrder("bor_mdc_r", "bt_mdc_r", 10000, "paid");
        insertBarBill("bb_mdc_r", "bor_mdc_r", "bt_mdc_r", 10000);
        insertSpace("space_mdc_bar", "shop", "20", "active");
        jdbcTemplate.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES (:id,'complex','收入测试综合体','active',now(),now())",
                new MapSqlParameterSource().addValue("id",sid("project_mdc_bar")));
        jdbcTemplate.update("UPDATE spaces SET project_id=:project WHERE id=:id",
                new MapSqlParameterSource().addValue("project",sid("project_mdc_bar")).addValue("id",sid("space_mdc_bar")));
        jdbcTemplate.update("UPDATE bar_tables SET space_id=:space WHERE id=:id",
                new MapSqlParameterSource().addValue("space",sid("space_mdc_bar")).addValue("id",sid("bt_mdc_r")));
        jdbcTemplate.update("UPDATE bar_bills SET merchant_id='mch_mdc_bar' WHERE id=:id",
                new MapSqlParameterSource().addValue("id",sid("bb_mdc_r")));
        insertBarSettlement("bbs_mdc_1", "bb_mdc_r", 8800, false);
        insertBarSettlement("bbs_mdc_2", "bb_mdc_r", 1200, false);
        insertBarSettlement("bbs_mdc_rev", "bb_mdc_r", 999, true); // 已冲正 → 不进口径

        MetricDetailRows after = repository.barSettlementRevenueDetail(start, end, 200, 0);
        assertThat(after.summaryValue().subtract(baseline)).isEqualByComparingTo("10000");
        assertThat(sumLong(collectAllPages((limit, offset) -> repository.barSettlementRevenueDetail(start, end, limit, offset)), "settlementAmountCent"))
                .isEqualByComparingTo(after.summaryValue());
        // 与看板 ETL revenue.bar（sumBarRevenueCentsByMerchant，Σ全部商户）同谓词同源。
        BigDecimal etl = repository.sumBarRevenueCentsByMerchant(DAY, "bar").values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(etl).isEqualByComparingTo(after.summaryValue());
    }

    @Test
    void barOrderCountAndAmountEqualPagedDetailAndPageListPredicate() {
        MetricDetailRows countBaseline = repository.barOrderDetail(null, null, false, 1, 0);
        MetricDetailRows amountBaseline = repository.barOrderDetail(null, null, true, 1, 0);

        insertBarTable("bt_mdc_o");
        insertBarOrder("bor_mdc_a", "bt_mdc_o", 12000, "ordered");
        insertBarOrder("bor_mdc_b", "bt_mdc_o", 6600, "paid");     // 已支付也计数计金额（页面全量列表口径）
        insertBarOrder("bor_mdc_c", "bt_mdc_o", 0, "cancelled");   // 已取消也计数

        MetricDetailRows count = repository.barOrderDetail(null, null, false, 1, 0);
        assertThat(count.summaryValue().subtract(countBaseline.summaryValue())).isEqualByComparingTo("3");
        assertThat(count.summaryValue()).isEqualByComparingTo(BigDecimal.valueOf(count.totalRows()));

        MetricDetailRows amount = repository.barOrderDetail(null, null, true, 200, 0);
        assertThat(amount.summaryValue().subtract(amountBaseline.summaryValue())).isEqualByComparingTo("18600");
        assertThat(sumLong(collectAllPages((limit, offset) -> repository.barOrderDetail(null, null, true, limit, offset)), "totalAmountCent"))
                .isEqualByComparingTo(amount.summaryValue());
        // 与页面旧谓词交叉：订单/出品/账单页读 listOrders 平台口径=bar_orders 全表无过滤。
        Long tableCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bar_orders", Map.of(), Long.class);
        assertThat(count.totalRows()).isEqualTo(tableCount);
    }

    @Test
    void barOpenOrderCountExcludesPaidAndCancelled() {
        Instant asOfEnd = businessDayResolver.endInstant("bar", DAY);
        BigDecimal baseline = repository.barOpenOrderDetail(asOfEnd, 1, 0).summaryValue();

        insertBarTable("bt_mdc_p");
        insertBarOrder("bor_mdc_open1", "bt_mdc_p", 3000, "ordered");
        insertBarOrder("bor_mdc_open2", "bt_mdc_p", 4000, "billed");
        insertBarOrder("bor_mdc_paid", "bt_mdc_p", 5000, "paid");       // 已支付 → 不进口径
        insertBarOrder("bor_mdc_cancel", "bt_mdc_p", 100, "cancelled"); // 已取消 → 不进口径

        MetricDetailRows after = repository.barOpenOrderDetail(asOfEnd, 200, 0);
        assertThat(after.summaryValue().subtract(baseline)).isEqualByComparingTo("2");
        assertThat(after.summaryValue()).isEqualByComparingTo(BigDecimal.valueOf(after.totalRows()));
        assertThat(collectAllPages((limit, offset) -> repository.barOpenOrderDetail(asOfEnd, limit, offset)))
                .hasSize((int) after.totalRows());
    }

    @Test
    void savedBottleCountCountsAllStatuses() {
        MetricDetailRows baseline = repository.savedBottleDetail(null, null, 1, 0);

        insertSavedBottle("sb_mdc_1", 6, 6, "stored");
        insertSavedBottle("sb_mdc_2", 4, 0, "claimed");  // 已领完也计登记数（页面全量列表口径）
        insertSavedBottle("sb_mdc_3", 2, 2, "expired");

        MetricDetailRows after = repository.savedBottleDetail(null, null, 1, 0);
        assertThat(after.summaryValue().subtract(baseline.summaryValue())).isEqualByComparingTo("3");
        assertThat(after.summaryValue()).isEqualByComparingTo(BigDecimal.valueOf(after.totalRows()));
    }

    @Test
    void savedBottleStoredBalanceCountsOnlyStoredRows() {
        Instant asOfEnd = businessDayResolver.endInstant("bar", DAY);
        BigDecimal baseline = repository.savedBottleStoredDetail(asOfEnd, 1, 0).summaryValue();

        insertSavedBottle("sb_mdc_s1", 6, 6, "stored");
        insertSavedBottle("sb_mdc_s2", 3, 3, "stored");
        insertSavedBottle("sb_mdc_c1", 4, 0, "claimed"); // 非在存 → 不进口径

        MetricDetailRows after = repository.savedBottleStoredDetail(asOfEnd, 200, 0);
        assertThat(after.summaryValue().subtract(baseline)).isEqualByComparingTo("2");
        assertThat(collectAllPages((limit, offset) -> repository.savedBottleStoredDetail(asOfEnd, limit, offset)))
                .hasSize((int) after.totalRows());
    }

    @Test
    void savedBottleQuantitySummaryEqualsPagedDetailSumOfRemainingAndPagePredicate() {
        // as-of 取当前经营日窗末：与全表 Σremaining_quantity 交叉时须覆盖库内既有全部行（历史 DAY 会漏掉其后真实数据）。
        Instant asOfEnd = businessDayResolver.endInstant("bar", businessDayResolver.businessDate("bar", Instant.now()));
        BigDecimal baseline = repository.savedBottleQuantityDetail(asOfEnd, 1, 0).summaryValue();

        insertSavedBottle("sb_mdc_q1", 6, 5, "stored");
        insertSavedBottle("sb_mdc_q2", 4, 0, "claimed"); // 已领完=0 行进明细，不影响合计
        insertSavedBottle("sb_mdc_q3", 3, 2, "stored");

        MetricDetailRows after = repository.savedBottleQuantityDetail(asOfEnd, 200, 0);
        assertThat(after.summaryValue().subtract(baseline)).isEqualByComparingTo("7");
        assertThat(sumLong(collectAllPages((limit, offset) -> repository.savedBottleQuantityDetail(asOfEnd, limit, offset)), "remainingQuantity"))
                .isEqualByComparingTo(after.summaryValue());
        // 与存酒页旧谓词交叉：全量列表 Σ当前剩余数量（saved_bottles 全表）。
        java.math.BigDecimal tableSum = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(remaining_quantity), 0) FROM saved_bottles", Map.of(), java.math.BigDecimal.class);
        assertThat(tableSum).isEqualByComparingTo(after.summaryValue());
    }

    // ==== 第三批（票务域）一致性：summaryValue == 逐页明细合计，且与页面旧谓词交叉 ====

    @Test
    void ticketOrderCountSummaryEqualsPagedDetailCount() {
        BigDecimal baseline = repository.ticketOrderDetail(null, null, false, 1, 0).summaryValue();
        insertTicketOrderIssue("to_cnt_1", "paid", "issued");
        insertTicketOrderIssue("to_cnt_2", "pending", "pending");

        MetricDetailRows after = repository.ticketOrderDetail(null, null, false, 200, 0);
        assertThat(after.summaryValue().subtract(baseline)).isEqualByComparingTo("2");
        // 数字=明细合计：翻完所有页,行数 == summaryValue（计数类）。
        assertThat(collectAllPages((limit, offset) -> repository.ticketOrderDetail(null, null, false, limit, offset)))
                .hasSize((int) after.totalRows());
        assertThat(BigDecimal.valueOf(after.totalRows())).isEqualByComparingTo(after.summaryValue());
    }

    @Test
    void ticketPaidOrderCountCountsOnlyPaidAndCrossesPagePredicate() {
        BigDecimal baseline = repository.ticketOrderDetail(null, null, true, 1, 0).summaryValue();
        insertTicketOrderIssue("to_paid_1", "paid", "issued");
        insertTicketOrderIssue("to_paid_2", "paid", "pending");
        insertTicketOrderIssue("to_paid_unpaid", "pending", "pending"); // 未支付 → 不进口径

        MetricDetailRows after = repository.ticketOrderDetail(null, null, true, 200, 0);
        assertThat(after.summaryValue().subtract(baseline)).isEqualByComparingTo("2");
        // 与出票追踪页「已支付」旧谓词交叉：ticket_orders WHERE pay_status='paid'。
        BigDecimal tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticket_orders WHERE pay_status = 'paid'", Map.of(), BigDecimal.class);
        assertThat(tableCount).isEqualByComparingTo(after.summaryValue());
    }

    @Test
    void ticketPendingIssueBalanceCountsNonIssuedAsOfEnd() {
        Instant asOfEnd = businessDayResolver.endInstant("ticketing", businessDayResolver.businessDate("ticketing", Instant.now()));
        BigDecimal baseline = repository.ticketPendingIssueDetail(asOfEnd, 1, 0).summaryValue();
        insertTicketOrderIssue("to_pend_1", "paid", "pending");
        insertTicketOrderIssue("to_pend_2", "pending", "pending");
        insertTicketOrderIssue("to_pend_done", "paid", "issued"); // 已出票 → 不进口径

        MetricDetailRows after = repository.ticketPendingIssueDetail(asOfEnd, 200, 0);
        assertThat(after.summaryValue().subtract(baseline)).isEqualByComparingTo("2");
        assertThat(collectAllPages((limit, offset) -> repository.ticketPendingIssueDetail(asOfEnd, limit, offset)))
                .hasSize((int) after.totalRows());
    }

    @Test
    void amusementSessionCountAndCapacitySummaryEqualsPagedDetail() {
        insertAmusementProject("proj_sess");
        BigDecimal baseCount = amusementDetail("amusement.session_count", 1, 0).summaryValue();
        insertAmusementSession("ses_1", "proj_sess", 100, "scheduled");
        insertAmusementSession("ses_2", "proj_sess", 50, "open");

        MetricDetailRows count = amusementDetail("amusement.session_count", 200, 0);
        assertThat(count.summaryValue().subtract(baseCount)).isEqualByComparingTo("2");
        MetricDetailRows cap = amusementDetail("amusement.session_capacity", 200, 0);
        assertThat(cap.summaryValue()).isEqualByComparingTo("500");
        // 数字=明细合计：翻完所有页,Σcapacity == summaryValue。
        assertThat(sumLong(collectAllPages((limit, offset) -> amusementDetail("amusement.session_capacity", limit, offset)), "capacity"))
                .isEqualByComparingTo(cap.summaryValue());
    }

    @Test
    void amusementGroupBookingCountAndReservedSummaryEqualsPagedDetail() {
        insertAmusementProject("proj_grp");
        insertAmusementSession("ses_grp", "proj_grp", 200, "scheduled");
        BigDecimal baseCount = amusementDetail("amusement.group_booking_count", 1, 0).summaryValue();
        BigDecimal baseReserved = amusementDetail("amusement.group_reserved_capacity", 1, 0).summaryValue();
        insertAmusementGroupBooking("gb_1", "proj_grp", "ses_grp", 20, "approved");
        insertAmusementGroupBooking("gb_2", "proj_grp", "ses_grp", 30, "pending_approval");

        MetricDetailRows count = amusementDetail("amusement.group_booking_count", 200, 0);
        assertThat(count.summaryValue().subtract(baseCount)).isEqualByComparingTo("2");
        MetricDetailRows reserved = amusementDetail("amusement.group_reserved_capacity", 200, 0);
        assertThat(reserved.summaryValue().subtract(baseReserved)).isEqualByComparingTo("20");
        assertThat(sumLong(collectAllPages((limit, offset) -> amusementDetail("amusement.group_reserved_capacity", limit, offset)), "reservedCapacity"))
                .isEqualByComparingTo(reserved.summaryValue());
    }

    @Test
    void amusementEntryExitCountsSplitByDirection() {
        insertAmusementProject("proj_ver");
        insertAmusementSession("ses_ver", "proj_ver", 300, "open");
        BigDecimal baseEntry = amusementDetail("amusement.entry_count", 1, 0).summaryValue();
        BigDecimal baseExit = amusementDetail("amusement.exit_count", 1, 0).summaryValue();
        insertAmusementVerification("av_e1", "proj_ver", "ses_ver", "entry");
        insertAmusementVerification("av_e2", "proj_ver", "ses_ver", "entry");
        insertAmusementVerification("av_x1", "proj_ver", "ses_ver", "exit");

        MetricDetailRows entry = amusementDetail("amusement.entry_count", 200, 0);
        assertThat(entry.summaryValue().subtract(baseEntry)).isEqualByComparingTo("2");
        MetricDetailRows exit = amusementDetail("amusement.exit_count", 200, 0);
        assertThat(exit.summaryValue().subtract(baseExit)).isEqualByComparingTo("1");
    }

    @Test
    void amusementInspectionCountAndPendingRectificationSummaryEqualsPagedDetail() {
        insertAmusementProject("proj_insp");
        insertAmusementSafetyTemplate("tpl_insp");
        BigDecimal baseCount = amusementDetail("amusement.inspection_count", 1, 0).summaryValue();
        BigDecimal basePending = amusementDetail("amusement.inspection_pending_rectification", 1, 0).summaryValue();
        insertAmusementSafetyInspection("insp_1", "proj_insp", "tpl_insp", "pass", "pending_rectification");
        insertAmusementSafetyInspection("insp_2", "proj_insp", "tpl_insp", "pass", "resolved"); // 已整改 → 不进待整改口径

        MetricDetailRows count = amusementDetail("amusement.inspection_count", 200, 0);
        assertThat(count.summaryValue().subtract(baseCount)).isEqualByComparingTo("2");
        MetricDetailRows pending = amusementDetail("amusement.inspection_pending_rectification", 200, 0);
        assertThat(pending.summaryValue().subtract(basePending)).isEqualByComparingTo("1");
        assertThat(collectAllPages((limit, offset) -> amusementDetail("amusement.inspection_pending_rectification", limit, offset)))
                .hasSize((int) pending.totalRows());
    }

    // ---- helpers ----

    private interface Pager {
        MetricDetailRows page(int limit, int offset);
    }

    private List<Map<String, Object>> collectAllPages(Pager pager) {
        List<Map<String, Object>> all = new ArrayList<>();
        int offset = 0;
        MetricDetailRows page;
        do {
            page = pager.page(200, offset);
            all.addAll(page.rows());
            offset += 200;
        } while (!page.rows().isEmpty() && offset < page.totalRows());
        return all;
    }

    private static BigDecimal sumLong(List<Map<String, Object>> rows, String key) {
        return BigDecimal.valueOf(rows.stream().map(row -> (Number) row.get(key))
                .filter(java.util.Objects::nonNull).mapToLong(Number::longValue).sum());
    }

    private static BigDecimal sumDecimal(List<Map<String, Object>> rows, String key) {
        return rows.stream().map(row -> (BigDecimal) row.get(key)).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ==== 第四批（财务域）：谓词逐字同各页面既有取数（JDBC 一致性，按环境跑）====

    @Test
    void apBalanceSummaryEqualsPagedDetailSumOfPayableUnpaid() {
        BigDecimal baseline = repository.apOpenPayableDetail(DAY, 1, 0).summaryValue();
        insertApItem("ap_mdc_1", 40000, DAY.minusDays(10));   // 逾期未清应付 → 进口径
        insertApItem("ap_mdc_future", 9999, DAY.plusDays(5)); // 未到期 → 不进口径

        MetricDetailRows after = repository.apOpenPayableDetail(DAY, 200, 0);
        assertThat(after.summaryValue().subtract(baseline)).isEqualByComparingTo("40000");
        assertThat(sumLong(collectAllPages((limit, offset) -> repository.apOpenPayableDetail(DAY, limit, offset)), "unpaidAmountCent"))
                .isEqualByComparingTo(after.summaryValue());
    }

    @Test
    void arAging90pSummaryEqualsEtlAging90pBucket() {
        BigDecimal baseline = repository.arOverdueAging90pDetail(DAY, 1, 0).summaryValue();
        insertArItem("ar_ag_over", 55000, DAY.minusDays(120)); // 逾期120天 → 进坏账预警区
        insertArItem("ar_ag_under", 3000, DAY.minusDays(45));  // 逾期45天 → 不进坏账预警区

        MetricDetailRows after = repository.arOverdueAging90pDetail(DAY, 200, 0);
        assertThat(after.summaryValue().subtract(baseline)).isEqualByComparingTo("55000");
        // 与 ETL ar.aging_90p（最末桶合计）同谓词。
        BigDecimal etl90p = repository.arOverdueAgingByMerchant(DAY).getOrDefault("a90p", java.util.Map.of()).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(etl90p).isEqualByComparingTo(after.summaryValue());
    }

    @Test
    void settlementCountAmountTicketsEqualPagedDetailAndPagePredicate() {
        BigDecimal baseCount = repository.settlementTransactionDetail(null, null, "count", 1, 0).summaryValue();
        BigDecimal baseAmount = repository.settlementTransactionDetail(null, null, "amount", 1, 0).summaryValue();
        BigDecimal baseTickets = repository.settlementTransactionDetail(null, null, "tickets", 1, 0).summaryValue();
        insertSettlementTxn("st_mdc_1", 20000, 2);
        insertSettlementTxn("st_mdc_2", 5000, 3);

        MetricDetailRows count = repository.settlementTransactionDetail(null, null, "count", 200, 0);
        assertThat(count.summaryValue().subtract(baseCount)).isEqualByComparingTo("2");
        // 数字=明细合计（计数类）：totalRows == summaryValue。
        assertThat(BigDecimal.valueOf(count.totalRows())).isEqualByComparingTo(count.summaryValue());
        assertThat(repository.settlementTransactionDetail(null, null, "amount", 200, 0).summaryValue().subtract(baseAmount))
                .isEqualByComparingTo("25000");
        assertThat(repository.settlementTransactionDetail(null, null, "tickets", 200, 0).summaryValue().subtract(baseTickets))
                .isEqualByComparingTo("5");
        // 与结算审阅页 listVerificationSettled 基础谓词交叉：transaction_type='verification_settled'。
        BigDecimal tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM settlement_transactions WHERE transaction_type = 'verification_settled'", Map.of(), BigDecimal.class);
        assertThat(tableCount).isEqualByComparingTo(count.summaryValue());
    }

    private void insertApItem(String id, long unpaidCent, LocalDate dueDate) {
        id = sid(id);
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, amount_cent, paid_amount_cent,
                        unpaid_amount_cent, currency, due_date, status, created_at, updated_at, merchant_id)
                VALUES (:id, :id, 'payable', 'sup_mdc', :amount, 0, :amount, 'CNY', :due, 'open', now(), now(), NULL)
                """, new MapSqlParameterSource().addValue("id", id).addValue("amount", unpaidCent).addValue("due", dueDate));
    }

    private void insertSettlementTxn(String id, long verifiedAmountCent, int verifiedCount) {
        id = sid(id);
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                        entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                        verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                        settlement_status, entitlement_valid_to, created_at, updated_at)
                VALUES (:id, :id, 'verification_settled', :id, 'ticket', :vc, :vc, :vc, :amount,
                        :amount, 0, 'merchant', 'mch_mdc_st', 'proj_mdc_st', 'draft', :at, :at, :at)
                """, new MapSqlParameterSource().addValue("id", id).addValue("vc", verifiedCount)
                .addValue("amount", verifiedAmountCent).addValue("at", dayTs()));
    }

    private void insertTicketOrder(String id, long amountCent, String payStatus) {
        id = sid(id);
        Instant at = DAY.atTime(10, 0).toInstant(java.time.ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(at);
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)
                VALUES ('tt_mdc', 'MDC Pass', 'weekday_weekend_holiday', FALSE, 'published', :at, :at)
                ON CONFLICT (id) DO NOTHING
                """, Map.of("at", ts));
        jdbcTemplate.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent,
                    pay_status, issue_status, status, created_at, updated_at)
                VALUES (:id, :id, 'tt_mdc', 1, 'online', :amount, :pay_status, 'issued', 'created', :at, :at)
                """, Map.of("id", id, "amount", amountCent, "pay_status", payStatus, "at", ts));
    }

    private void insertReconBatch(String id) {
        // scope_id 列 NOT NULL：照仓内真实 scope 口径（scope_type ∈ DAILY_CLOSE_SCOPES，单综合体合成 project_1）。
        jdbcTemplate.update("""
                INSERT INTO finance_reconciliation_batches (id, batch_no, scope_type, scope_id, business_date, status, created_at)
                VALUES (:id, :id, 'project', 'project_1', :day, 'open', now())
                """, new MapSqlParameterSource().addValue("id", sid(id)).addValue("day", DAY));
    }

    private void insertReconDifference(String id, String batchId, long amountCent, String status) {
        id = sid(id);
        batchId = sid(batchId);
        jdbcTemplate.update("""
                INSERT INTO finance_reconciliation_differences (id, batch_id, difference_type, business_ref,
                        amount_cent, expected_amount_cent, actual_amount_cent, source, status, created_at)
                -- difference_type 走 V105 收紧后的白名单(amount/count/status/missing);amount_mismatch 是 V069 遗留值已被清
                VALUES (:id, :batch_id, 'amount', :id, :amount, 0, :amount, 'system', :status, now())
                """, new MapSqlParameterSource().addValue("id", id).addValue("batch_id", batchId)
                .addValue("amount", amountCent).addValue("status", status));
    }

    private void insertMeter(String id, String meterType) {
        id = sid(id);
        jdbcTemplate.update("""
                INSERT INTO energy_meters (id, name, meter_type, gather_object, read_mode)
                VALUES (:id, :id, :meter_type, 'public_area', 'manual')
                """, Map.of("id", id, "meter_type", meterType));
    }

    private void insertReading(String id, String meterId, String consumption, String previous, String readAt) {
        // 同一表按真实计量时点分段，只有已审核用量进入汇总和钻取。
        String period = java.time.YearMonth.from(DAY).toString();
        id = sid(id);
        jdbcTemplate.update("""
                INSERT INTO energy_meter_readings (id, meter_id, period, reading_value, prev_reading_value,
                        consumption, status, abnormal_reason, read_mode, read_at)
                VALUES (:id, :meter_id, :period, :previous::numeric+:consumption::numeric, :previous::numeric, :consumption::numeric,
                        'reviewed', NULL, 'manual', :read_at::timestamptz)
                """, new MapSqlParameterSource().addValue("id", id).addValue("meter_id", sid(meterId))
                .addValue("consumption", consumption).addValue("period", period)
                .addValue("previous", previous).addValue("read_at", readAt));
    }

    private void insertSpace(String id, String type, String area, String status) {
        id = sid(id);
        jdbcTemplate.update("""
                INSERT INTO spaces (id, space_name, space_type, floor_no, area_square_meter, polygon_points, status, created_at, updated_at)
                VALUES (:id, :id, :type, '1F', :area, '[]'::jsonb, :status, now(), now())
                """, new MapSqlParameterSource().addValue("id", id).addValue("type", type)
                .addValue("area", new BigDecimal(area)).addValue("status", status));
    }

    private void insertArItem(String id, long unpaidCent, LocalDate dueDate) {
        id = sid(id);
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, amount_cent, paid_amount_cent,
                        unpaid_amount_cent, currency, due_date, status, created_at, updated_at, merchant_id)
                VALUES (:id, :id, 'receivable', 'mch_mdc', :amount, 0, :amount, 'CNY', :due, 'open', now(), now(), NULL)
                """, new MapSqlParameterSource().addValue("id", id).addValue("amount", unpaidCent).addValue("due", dueDate));
    }

    // ---- 第二批（酒吧域）种子：均落 DAY 10:00Z（酒吧经营日窗内，日切 04:00/06:00 皆覆盖） ----

    private static java.sql.Timestamp dayTs() {
        return java.sql.Timestamp.from(DAY.atTime(10, 0).toInstant(java.time.ZoneOffset.UTC));
    }

    private void insertBarTable(String id) {
        id = sid(id);
        jdbcTemplate.update("""
                INSERT INTO bar_tables (id, bar_id, table_code, table_name, table_type, status, created_at, updated_at)
                VALUES (:id, 'bar-main', :id, :id, 'booth', 'available', :at, :at)
                """, new MapSqlParameterSource().addValue("id", id).addValue("at", dayTs()));
    }

    private void insertBarOrder(String id, String tableId, long amountCent, String status) {
        jdbcTemplate.update("""
                INSERT INTO bar_orders (id, bar_id, table_id, order_source, status, total_amount_cent,
                        currency, created_at, updated_at, order_type)
                VALUES (:id, 'bar-main', :table_id, 'staff', :status, :amount, 'CNY', :at, :at, 'standard')
                """, new MapSqlParameterSource().addValue("id", sid(id)).addValue("table_id", sid(tableId))
                .addValue("status", status).addValue("amount", amountCent).addValue("at", dayTs()));
    }

    private void insertBarBill(String id, String orderId, String tableId, long amountCent) {
        jdbcTemplate.update("""
                INSERT INTO bar_bills (id, bill_no, order_id, bar_id, table_id, subtotal_amount_cent,
                        payable_amount_cent, paid_amount_cent, currency, payment_status, settlement_status, created_at, updated_at)
                VALUES (:id, :id, :order_id, 'bar-main', :table_id, :amount, :amount, :amount, 'CNY', 'paid', 'settled', :at, :at)
                """, new MapSqlParameterSource().addValue("id", sid(id)).addValue("order_id", sid(orderId))
                .addValue("table_id", sid(tableId)).addValue("amount", amountCent).addValue("at", dayTs()));
    }

    private void insertBarSettlement(String id, String billId, long amountCent, boolean reversed) {
        jdbcTemplate.update("""
                INSERT INTO bar_bill_settlements (id, bill_id, settle_channel, settlement_amount_cent,
                        merchant_id, settled_at, reversed)
                VALUES (:id, :bill_id, 'cash', :amount, 'mch_mdc_bar', :at, :reversed)
                """, new MapSqlParameterSource().addValue("id", sid(id)).addValue("bill_id", sid(billId))
                .addValue("amount", amountCent).addValue("at", dayTs()).addValue("reversed", reversed));
    }

    private void insertSavedBottle(String id, int initialQuantity, int remainingQuantity, String status) {
        // FK item_id → inventory_items；saved_bottles CHECK：initial>0、remaining>=0、status ∈ 白名单（V012）。
        jdbcTemplate.update("""
                INSERT INTO inventory_items (id, item_name, item_type, unit, status, created_at, updated_at)
                VALUES ('ii_mdc', 'MDC 威士忌', 'liquor', '瓶', 'active', now(), now())
                ON CONFLICT (id) DO NOTHING
                """, Map.of());
        jdbcTemplate.update("""
                INSERT INTO saved_bottles (id, customer_id, bar_project_id, item_id, bottle_name,
                        initial_quantity, remaining_quantity, unit, status, created_at, updated_at)
                VALUES (:id, 'cus_mdc', 'bar-main', 'ii_mdc', 'MDC 威士忌', :initial, :remaining, '瓶', :status, :at, :at)
                """, new MapSqlParameterSource().addValue("id", sid(id)).addValue("initial", initialQuantity)
                .addValue("remaining", remainingQuantity).addValue("status", status).addValue("at", dayTs()));
    }

    // ---- 第三批（票务域）种子：均落 DAY 10:00Z（票务/游乐经营日窗内，日切 04:00 覆盖） ----

    private void insertTicketOrderIssue(String id, String payStatus, String issueStatus) {
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)
                VALUES ('tt_mdc', 'MDC Pass', 'weekday_weekend_holiday', FALSE, 'published', :at, :at)
                ON CONFLICT (id) DO NOTHING
                """, Map.of("at", dayTs()));
        jdbcTemplate.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent,
                    pay_status, issue_status, status, created_at, updated_at)
                VALUES (:id, :id, 'tt_mdc', 1, 'online', 1000, :pay_status, :issue_status, 'created', :at, :at)
                """, new MapSqlParameterSource().addValue("id", sid(id)).addValue("pay_status", payStatus)
                .addValue("issue_status", issueStatus).addValue("at", dayTs()));
    }

    private MetricDetailRows amusementDetail(String key, int limit, int offset) {
        return repository.amusementComplexDetail(key, DAY, DAY, DAY.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                false, limit, offset, List.of(amusementComplex));
    }

    private void insertAmusementProject(String id) {
        if (amusementComplex == null) {
            amusementComplex = AmusementBusinessDayTestFixture.complex(jdbcTemplate.getJdbcTemplate());
            AmusementBusinessDayTestFixture.approve(jdbcTemplate.getJdbcTemplate(), amusementBusinessDays, supervisorReviews,
                    amusementComplex, "Asia/Shanghai", 4, DAY.minusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
        }
        jdbcTemplate.update("""
                INSERT INTO amusement_projects (id, project_name, capacity, open_status, eligibility_rule_json, created_at, updated_at)
                VALUES (:id, :id, 500, 'open', '{}'::jsonb, :at, :at)
                """, new MapSqlParameterSource().addValue("id", sid(id)).addValue("at", dayTs()));
        AmusementBusinessDayTestFixture.bind(jdbcTemplate.getJdbcTemplate(), sid(id), amusementComplex);
    }

    private void insertAmusementSession(String id, String projectId, int capacity, String status) {
        jdbcTemplate.update("""
                INSERT INTO amusement_sessions (id, project_id, start_time, end_time, capacity, used_capacity, status, created_at, updated_at)
                VALUES (:id, :project_id, :start, :end, :capacity, 0, :status, :at, :at)
                """, new MapSqlParameterSource().addValue("id", sid(id)).addValue("project_id", sid(projectId))
                .addValue("start", dayTs()).addValue("end", java.sql.Timestamp.from(DAY.atTime(12, 0).toInstant(java.time.ZoneOffset.UTC)))
                .addValue("capacity", capacity).addValue("status", status).addValue("at", dayTs()));
    }

    private void insertAmusementGroupBooking(String id, String projectId, String sessionId, int reservedCapacity, String status) {
        jdbcTemplate.update("""
                INSERT INTO amusement_group_bookings (id, project_id, session_id, booking_type, customer_name,
                        party_size, reserved_capacity, booking_status, created_at, updated_at)
                VALUES (:id, :project_id, :session_id, 'group', 'MDC 团客', :party, :reserved, :status, :at, :at)
                """, new MapSqlParameterSource().addValue("id", sid(id)).addValue("project_id", sid(projectId))
                .addValue("session_id", sid(sessionId)).addValue("party", reservedCapacity).addValue("reserved", reservedCapacity)
                .addValue("status", status).addValue("at", dayTs()));
    }

    private void insertAmusementVerification(String id, String projectId, String sessionId, String direction) {
        jdbcTemplate.update("""
                INSERT INTO amusement_verifications (id, project_id, session_id, credential_payload,
                        verification_result, verified_people, direction, created_at)
                VALUES (:id, :project_id, :session_id, :id, 'success', 1, :direction, :at)
                """, new MapSqlParameterSource().addValue("id", sid(id)).addValue("project_id", sid(projectId))
                .addValue("session_id", sid(sessionId)).addValue("direction", direction).addValue("at", dayTs()));
    }

    private void insertAmusementSafetyTemplate(String id) {
        jdbcTemplate.update("""
                INSERT INTO amusement_safety_templates (id, template_name, items_json, status, created_at, updated_at)
                VALUES (:id, :id, '[{"item":"check"}]'::jsonb, 'active', :at, :at)
                """, new MapSqlParameterSource().addValue("id", sid(id)).addValue("at", dayTs()));
    }

    private void insertAmusementSafetyInspection(String id, String projectId, String templateId, String overallResult, String rectificationStatus) {
        jdbcTemplate.update("""
                INSERT INTO amusement_safety_inspections (id, project_id, template_id, items_json, overall_result,
                        inspection_status, rectification_status, reopen_status, inspected_at, created_at, updated_at)
                VALUES (:id, :project_id, :template_id, '[{"item":"check"}]'::jsonb, :overall,
                        'submitted', :rectification, 'none', :at, :at, :at)
                """, new MapSqlParameterSource().addValue("id", sid(id)).addValue("project_id", sid(projectId))
                .addValue("template_id", sid(templateId)).addValue("overall", overallResult)
                .addValue("rectification", rectificationStatus).addValue("at", dayTs()));
    }

    // ==== 第五批（人事域）：数字=明细合计 真库一致性（基线差值口径） ====

    @Test
    void staffShiftCountEqualsPagedDetailCountOverWorkDateRange() {
        LocalDate day = LocalDate.parse("2026-06-21");
        BigDecimal baseline = repository.staffShiftDetail(day, day, 1, 0).summaryValue();
        insertShift(sid("shift_a"), day);
        insertShift(sid("shift_b"), day);
        MetricDetailRows rows = repository.staffShiftDetail(day, day, 200, 0);
        assertThat(rows.summaryValue().subtract(baseline)).isEqualByComparingTo("2");
        assertThat(collectAllPages((limit, offset) -> repository.staffShiftDetail(day, day, limit, offset)))
                .hasSize(rows.summaryValue().intValueExact());
    }

    @Test
    void staffAttendanceCountAndLateSplitByJudgement() {
        LocalDate day = LocalDate.parse("2026-06-22");
        BigDecimal baseAll = repository.staffAttendanceDetail(day, day, false, 1, 0).summaryValue();
        BigDecimal baseLate = repository.staffAttendanceDetail(day, day, true, 1, 0).summaryValue();
        insertAttendance(sid("att_normal"), day, "normal");
        insertAttendance(sid("att_late1"), day, "late");
        insertAttendance(sid("att_late2"), day, "late");
        assertThat(repository.staffAttendanceDetail(day, day, false, 1, 0).summaryValue().subtract(baseAll))
                .isEqualByComparingTo("3");
        assertThat(repository.staffAttendanceDetail(day, day, true, 1, 0).summaryValue().subtract(baseLate))
                .isEqualByComparingTo("2");
        assertThat(collectAllPages((limit, offset) -> repository.staffAttendanceDetail(day, day, true, limit, offset)))
                .allSatisfy(r -> assertThat(r.get("judgement")).isEqualTo("late"));
    }

    @Test
    void staffPayrollTotalAndCountEqualPagedDetailOverMonthRange() {
        LocalDate monthDay = LocalDate.parse("2026-06-01");
        BigDecimal baseTotal = repository.staffPayrollDetail(monthDay, monthDay, true, 1, 0).summaryValue();
        BigDecimal baseCount = repository.staffPayrollDetail(monthDay, monthDay, false, 1, 0).summaryValue();
        insertPayrollStatement(sid("pay_a"), "2026-06", 120000);
        insertPayrollStatement(sid("pay_b"), "2026-06", 80000);
        assertThat(repository.staffPayrollDetail(monthDay, monthDay, true, 1, 0).summaryValue().subtract(baseTotal))
                .isEqualByComparingTo("200000");
        assertThat(repository.staffPayrollDetail(monthDay, monthDay, false, 1, 0).summaryValue().subtract(baseCount))
                .isEqualByComparingTo("2");
        assertThat(sumLong(collectAllPages((limit, offset) -> repository.staffPayrollDetail(monthDay, monthDay, true, limit, offset)), "totalCent"))
                .isEqualByComparingTo(repository.staffPayrollDetail(monthDay, monthDay, true, 200, 0).summaryValue());
    }

    @Test
    void staffPendingLeaveCountCountsOnlyPendingAsOfEnd() {
        LocalDate asOf = LocalDate.now(java.time.ZoneOffset.UTC).plusDays(1);
        BigDecimal baseline = repository.staffPendingLeaveDetail(asOf, 1, 0).summaryValue();
        insertLeave(sid("lv_pending"), "pending");
        insertLeave(sid("lv_approved"), "approved");
        MetricDetailRows rows = repository.staffPendingLeaveDetail(asOf, 200, 0);
        assertThat(rows.summaryValue().subtract(baseline)).isEqualByComparingTo("1");
        assertThat(collectAllPages((limit, offset) -> repository.staffPendingLeaveDetail(asOf, limit, offset)))
                .allSatisfy(r -> assertThat(r.get("status")).isEqualTo("pending"));
    }

    @Test
    void staffActiveHeadcountCountsOnlyActiveStaffAccounts() {
        LocalDate asOf = LocalDate.now(java.time.ZoneOffset.UTC).plusDays(1);
        BigDecimal baseline = repository.staffActiveHeadcountDetail(asOf, 1, 0).summaryValue();
        insertUser(sid("u_staff_active"), "staff", "active");
        insertUser(sid("u_staff_disabled"), "staff", "disabled");
        insertUser(sid("u_vendor_active"), "outsourced_vendor", "active");
        MetricDetailRows rows = repository.staffActiveHeadcountDetail(asOf, 200, 0);
        assertThat(rows.summaryValue().subtract(baseline)).isEqualByComparingTo("1");
        assertThat(collectAllPages((limit, offset) -> repository.staffActiveHeadcountDetail(asOf, limit, offset)))
                .allSatisfy(r -> {
                    assertThat(r.get("accountType")).isEqualTo("staff");
                    assertThat(r.get("status")).isEqualTo("active");
                });
    }

    // ---- 第六批（总控驾驶舱剩余四卡）：谓词逐字抄总览 ETL 聚合（DashboardAggregationJob） ----

    @Test
    void amusementVerificationSuccessSummaryEqualsPagedDetailCountAndEtlAggregate() {
        insertAmusementProject("proj_tav");
        BigDecimal baseline = amusementDetail("traffic.amusement", 1, 0).summaryValue();
        BigDecimal globalBaseline = repository.countAmusementVerificationSuccess(DAY);
        insertAmusementSession("ses_tav", "proj_tav", 300, "open");
        insertAmusementVerification("tav_e1", "proj_tav", "ses_tav", "entry");
        insertAmusementVerification("tav_e2", "proj_tav", "ses_tav", "entry");
        insertAmusementVerification("tav_x1", "proj_tav", "ses_tav", "exit"); // 出场也计入（两方向成功核销）

        MetricDetailRows rows = amusementDetail("traffic.amusement", 200, 0);
        assertThat(rows.summaryValue().subtract(baseline)).isEqualByComparingTo("3");
        assertThat(collectAllPages((limit, offset) -> amusementDetail("traffic.amusement", limit, offset)))
                .hasSize((int) rows.totalRows());
        assertThat(repository.countAmusementVerificationSuccess(DAY).subtract(globalBaseline))
                .isEqualByComparingTo(rows.summaryValue().subtract(baseline));
    }

    @Test
    void ticketVerificationSuccessSummaryEqualsPagedDetailCountAndEtlPlatformTotal() {
        // 票务核销依赖 issued_entitlements/terminal_devices 多级外键——不新种子，用共享库存量数据的「同源一致」断言：
        // 明细行数==summaryValue==ETL 平台合计（status='success' 同谓词），证登记项明细与总览 ETL 同口径。
        Instant start = businessDayResolver.startInstant("ticketing", DAY);
        Instant end = businessDayResolver.endInstant("ticketing", DAY);
        MetricDetailRows rows = repository.ticketVerificationSuccessDetail(start, end, 200, 0);
        assertThat(collectAllPages((limit, offset) -> repository.ticketVerificationSuccessDetail(start, end, limit, offset)))
                .hasSize((int) rows.totalRows());
        BigDecimal etlPlatformTotal = repository.countTicketVerificationSuccessByMerchant(DAY).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(rows.summaryValue()).isEqualByComparingTo(etlPlatformTotal);
    }

    @Test
    void newMemberSummaryEqualsPagedDetailCountAndEtlAggregate() {
        Instant start = businessDayResolver.startInstant("customer", DAY);
        Instant end = businessDayResolver.endInstant("customer", DAY);
        BigDecimal baseline = repository.newMemberDetail(start, end, 1, 0).summaryValue();
        insertMember(sid("cu_active"), "active");
        insertMember(sid("cu_dereg"), "deregistered"); // 注销 → 不进口径
        insertMember(sid("cu_black"), "blacklisted");   // 拉黑 → 不进口径

        MetricDetailRows rows = repository.newMemberDetail(start, end, 200, 0);
        assertThat(rows.summaryValue().subtract(baseline)).isEqualByComparingTo("1");
        assertThat(collectAllPages((limit, offset) -> repository.newMemberDetail(start, end, limit, offset)))
                .hasSize((int) rows.totalRows());
        // 与总览 ETL 聚合同源同口径。
        assertThat(repository.countNewMembers(DAY)).isEqualByComparingTo(rows.summaryValue());
    }

    @Test
    void pendingReviewSummaryEqualsPagedDetailCountAndEtlAggregate() {
        LocalDate asOf = LocalDate.now(java.time.ZoneOffset.UTC).plusDays(1);
        BigDecimal baseline = repository.pendingReviewDetail(asOf, 1, 0).summaryValue();
        BigDecimal etlBaseline = repository.countPendingReview();
        insertSupervisorReview(sid("srd_pending"), "pending_review");
        insertSupervisorReview(sid("srd_allowed"), "allowed"); // 非待复核 → 不进口径
        insertRiskAlert(sid("ra_open"), "open");
        insertRiskAlert(sid("ra_resolved"), "resolved");        // 已处置 → 不进口径

        MetricDetailRows rows = repository.pendingReviewDetail(asOf, 200, 0);
        assertThat(rows.summaryValue().subtract(baseline)).isEqualByComparingTo("2"); // 1 待复核 + 1 未处置预警
        // 明细与 ETL 同谓词同步增（ETL countPendingReview 无 as-of 过滤，delta 口径下与明细一致）。
        assertThat(repository.countPendingReview().subtract(etlBaseline)).isEqualByComparingTo("2");
        assertThat(collectAllPages((limit, offset) -> repository.pendingReviewDetail(asOf, limit, offset)))
                .hasSize((int) rows.totalRows());
    }

    private void insertMember(String id, String status) {
        // 与 AnalyticsJdbcIntegrationTests.insertCustomer 同范式：phone 走 enc 占位（V188 起为密文列），仅脱敏列可读。
        jdbcTemplate.update("""
                INSERT INTO customers (id, phone, masked_phone, status, created_at, updated_at)
                VALUES (:id, 'enc:v1:'||:id, '138****0000', :status, :at, :at)
                """, new MapSqlParameterSource().addValue("id", id).addValue("status", status).addValue("at", dayTs()));
    }

    private void insertSupervisorReview(String id, String reviewStatus) {
        // 规范待复核状态=SupervisorReviewService 写入的 'pending_review'（与 countPendingReview 修正后同谓词）。
        jdbcTemplate.update("""
                INSERT INTO supervisor_review_decisions
                    (review_id, policy_code, action_type, object_type, object_id, blocked, review_status, required_level, evaluated_at)
                VALUES (:id, 'pol_mdc', 'high_risk', 'merchant', :id, TRUE, :status, 5, now())
                """, new MapSqlParameterSource().addValue("id", id).addValue("status", reviewStatus));
    }

    private void insertRiskAlert(String id, String status) {
        jdbcTemplate.update("""
                INSERT INTO analytics_risk_alerts
                    (id, alert_type, risk_level, object_type, object_id, summary, status, created_at, updated_at)
                VALUES (:id, 'anomaly', 'high', 'order', :id, 'MDC 测试预警', :status, now(), now())
                """, new MapSqlParameterSource().addValue("id", id).addValue("status", status));
    }

    private void insertShift(String id, LocalDate workDate) {
        jdbcTemplate.update("""
                INSERT INTO staff_shift_schedules (id, user_id, work_date, start_at, end_at, created_at, updated_at)
                VALUES (:id, :user_id, :work_date, :start_at, :end_at, now(), now())
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("user_id", id)
                .addValue("work_date", workDate)
                .addValue("start_at", java.sql.Timestamp.from(workDate.atTime(9, 0).toInstant(java.time.ZoneOffset.UTC)))
                .addValue("end_at", java.sql.Timestamp.from(workDate.atTime(18, 0).toInstant(java.time.ZoneOffset.UTC))));
    }

    private void insertAttendance(String id, LocalDate day, String judgement) {
        jdbcTemplate.update("""
                INSERT INTO staff_attendance_records (id, user_id, punch_type, punched_at, method, judgement, created_at)
                VALUES (:id, :user_id, 'check_in', :punched_at, 'nfc', :judgement, now())
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("user_id", id)
                .addValue("punched_at", java.sql.Timestamp.from(day.atTime(10, 0).toInstant(java.time.ZoneOffset.UTC)))
                .addValue("judgement", judgement));
    }

    private void insertPayrollStatement(String id, String month, long totalCent) {
        jdbcTemplate.update("""
                INSERT INTO staff_payroll_statements
                    (id, user_id, month, base_cent, commission_cent, bonus_cent, penalty_cent,
                     leave_deduction_cent, tax_cent, total_cent, status, closed_by, closed_at)
                VALUES (:id, :user_id, :month, 0, 0, 0, 0, 0, 0, :total, 'closed', 'tester', now())
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("user_id", id)
                .addValue("month", month)
                .addValue("total", totalCent));
    }

    private void insertLeave(String id, String status) {
        jdbcTemplate.update("""
                INSERT INTO staff_leave_requests
                    (id, user_id, leave_type, start_date, end_date, reason, status, created_at, updated_at)
                VALUES (:id, :user_id, 'personal', DATE '2026-06-20', DATE '2026-06-21', '测试请假', :status, now(), now())
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("user_id", id)
                .addValue("status", status));
    }

    private void insertUser(String id, String accountType, String status) {
        jdbcTemplate.update("""
                INSERT INTO access_users (id, username, account_type, status, created_at)
                VALUES (:id, :username, :account_type, :status, now())
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("username", id)
                .addValue("account_type", accountType)
                .addValue("status", status));
    }
}
