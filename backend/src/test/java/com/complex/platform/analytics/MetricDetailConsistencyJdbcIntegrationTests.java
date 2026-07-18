package com.complex.platform.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.analytics.AnalyticsRecords.MetricDetailRows;
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
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
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
        insertReading("er_mdc_1", "em_mdc_e", "700.5");
        insertReading("er_mdc_2", "em_mdc_e", "300");
        insertReading("er_mdc_w", "em_mdc_w", "88"); // 水表 → 不进电量口径

        MetricDetailRows after = repository.electricityReadingDetail(start, end, 200, 0);
        assertThat(after.summaryValue().subtract(baseline)).isEqualByComparingTo("1000.5");
        assertThat(sumDecimal(collectAllPages((limit, offset) -> repository.electricityReadingDetail(start, end, limit, offset)), "consumption"))
                .isEqualByComparingTo(after.summaryValue());
        BigDecimal etl = repository.energyConsumptionByType(start, end).getOrDefault("electricity", BigDecimal.ZERO);
        assertThat(etl).isEqualByComparingTo(after.summaryValue());
    }

    @Test
    void rentableAreaDenominatorEqualsPagedDetailSumAndOccupancyDenominator() {
        BigDecimal baseline = repository.rentableSpaceDetail(1, 0).summaryValue();

        insertSpace("sp_mdc_shop", "shop", "120", "active");
        insertSpace("sp_mdc_booth", "booth", "30", "active");
        insertSpace("sp_mdc_off", "shop", "999", "unavailable"); // 停用 → 不进分母
        insertSpace("sp_mdc_pub", "public_area", "500", "active"); // 公共区域 → 不进分母

        MetricDetailRows after = repository.rentableSpaceDetail(200, 0);
        assertThat(after.summaryValue().subtract(baseline)).isEqualByComparingTo("150");
        assertThat(sumDecimal(collectAllPages((limit, offset) -> repository.rentableSpaceDetail(limit, offset)), "areaSquareMeter"))
                .isEqualByComparingTo(after.summaryValue());
        // 与出租率同分母(occupancyAreas.rentable)。
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
        return BigDecimal.valueOf(rows.stream().mapToLong(row -> ((Number) row.get(key)).longValue()).sum());
    }

    private static BigDecimal sumDecimal(List<Map<String, Object>> rows, String key) {
        return rows.stream().map(row -> (BigDecimal) row.get(key)).reduce(BigDecimal.ZERO, BigDecimal::add);
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

    private void insertReading(String id, String meterId, String consumption) {
        // uq(meter_id, period) 且 period VARCHAR(16)：period=「run(8)/序号」——同表计多行（序号不同）、
        // 跨运行（run 不同）均不撞唯一键；此前同表计两行同 period 首跑即炸。
        String period = run + "/" + id.substring(id.lastIndexOf('_') + 1);
        id = sid(id);
        jdbcTemplate.update("""
                INSERT INTO energy_meter_readings (id, meter_id, period, reading_value, prev_reading_value,
                        consumption, status, abnormal_reason, read_mode, read_at)
                VALUES (:id, :meter_id, :period, :consumption::numeric, 0, :consumption::numeric,
                        'recorded', NULL, 'manual', :read_at::timestamptz)
                """, new MapSqlParameterSource().addValue("id", id).addValue("meter_id", sid(meterId))
                .addValue("consumption", consumption).addValue("period", period)
                .addValue("read_at", DAY + "T10:00:00Z"));
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
}
