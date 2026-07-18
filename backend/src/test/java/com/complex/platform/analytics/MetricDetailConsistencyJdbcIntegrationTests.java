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
 * KPI 统一明细「数字=明细合计」真库一致性（指标登记簿首批 5 指标，2026-07-18 方案A）。
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
        jdbcTemplate.update("""
                INSERT INTO finance_reconciliation_batches (id, batch_no, scope_type, scope_id, business_date, status, created_at)
                VALUES (:id, :id, 'platform', NULL, :day, 'open', now())
                """, new MapSqlParameterSource().addValue("id", id).addValue("day", DAY));
    }

    private void insertReconDifference(String id, String batchId, long amountCent, String status) {
        jdbcTemplate.update("""
                INSERT INTO finance_reconciliation_differences (id, batch_id, difference_type, business_ref,
                        amount_cent, expected_amount_cent, actual_amount_cent, source, status, created_at)
                VALUES (:id, :batch_id, 'amount_mismatch', :id, :amount, 0, :amount, 'system', :status, now())
                """, new MapSqlParameterSource().addValue("id", id).addValue("batch_id", batchId)
                .addValue("amount", amountCent).addValue("status", status));
    }

    private void insertMeter(String id, String meterType) {
        jdbcTemplate.update("""
                INSERT INTO energy_meters (id, name, meter_type, gather_object, read_mode)
                VALUES (:id, :id, :meter_type, 'public_area', 'manual')
                """, Map.of("id", id, "meter_type", meterType));
    }

    private void insertReading(String id, String meterId, String consumption) {
        jdbcTemplate.update("""
                INSERT INTO energy_meter_readings (id, meter_id, period, reading_value, prev_reading_value,
                        consumption, status, abnormal_reason, read_mode, read_at)
                VALUES (:id, :meter_id, :period, :consumption::numeric, 0, :consumption::numeric,
                        'recorded', NULL, 'manual', :read_at::timestamptz)
                """, new MapSqlParameterSource().addValue("id", id).addValue("meter_id", meterId)
                .addValue("consumption", consumption).addValue("period", DAY.toString())
                .addValue("read_at", DAY + "T10:00:00Z"));
    }

    private void insertSpace(String id, String type, String area, String status) {
        jdbcTemplate.update("""
                INSERT INTO spaces (id, space_name, space_type, floor_no, area_square_meter, polygon_points, status, created_at, updated_at)
                VALUES (:id, :id, :type, '1F', :area, '[]'::jsonb, :status, now(), now())
                """, new MapSqlParameterSource().addValue("id", id).addValue("type", type)
                .addValue("area", new BigDecimal(area)).addValue("status", status));
    }

    private void insertArItem(String id, long unpaidCent, LocalDate dueDate) {
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, amount_cent, paid_amount_cent,
                        unpaid_amount_cent, currency, due_date, status, created_at, updated_at, merchant_id)
                VALUES (:id, :id, 'receivable', 'mch_mdc', :amount, 0, :amount, 'CNY', :due, 'open', now(), now(), NULL)
                """, new MapSqlParameterSource().addValue("id", id).addValue("amount", unpaidCent).addValue("due", dueDate));
    }
}
