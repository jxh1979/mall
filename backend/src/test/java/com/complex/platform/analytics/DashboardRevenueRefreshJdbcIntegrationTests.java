package com.complex.platform.analytics;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.analytics.AnalyticsRecords.MetricSnapshotKey;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.RevenueRefreshMarks;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-04-R4 收入类快照的历史营业日重算（BSM 第 1831 行「月度对比：本月对上月、对去年同月……」、第 1833 行「异常提醒：
 * 经营指标环比突变自动报出来……」、第 1839 行「卡片明细对账：……卡片上的数字就是它下面明细的合计……」；ENUM 第 187 行
 * 「全系统的钱账统一按「营业日」归集……」；实施台账 ROOT-04「仍需在本项补齐跨期月结、退费后的原经营日及月度BI快照刷新。
 * 当前计划任务只刷当日，不能用全量历史refreshSnapshots破坏存量类快照」）。
 *
 * <p>真库、随测试事务回滚；日期取很靠后的营业日，库里没有同窗的业务行。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class DashboardRevenueRefreshJdbcIntegrationTests {

    @Autowired private AnalyticsRepository repository;
    @Autowired private BusinessDayResolver businessDayResolver;
    @Autowired private RevenueRefreshMarks marks;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    @Test
    void markedPastDateRefreshesRevenueAndMonthlyWithoutTouchingStockSnapshots() {
        LocalDate today = LocalDate.of(2097, 8, 5);
        LocalDate d = LocalDate.of(2097, 7, 20);
        String merchant = "mch_rr_" + suffix;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at) "
                + "VALUES (?, ?, 'catering', 'active', now(), now())", merchant, merchant);
        // D 的收入在 D 之后才落定：第三方回流（商户营业日 D）与合同租金应收（到期日 D，并入净收入）。
        reflow("late", "catering", merchant, 10_000, d.atTime(11, 0).atZone(LocalZone.ID).toInstant());
        jdbc.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, amount_cent, unpaid_amount_cent,
                        status, due_date, merchant_id, created_at, updated_at)
                VALUES (?, ?, 'receivable', ?, 'contract_rent', 30000, 30000, 'pending_collection', ?, ?, now(), now())
                """, "arap_rr_" + suffix, "ARAP-RR-" + suffix, merchant, d, merchant);
        // 存量类哨兵：库存余额、欠费、出租率、积压不随历史日重算改写；月度期末值同样不动。
        Instant earlier = Instant.parse("2097-07-21T00:00:00Z");
        for (String[] stock : List.of(new String[] {"supply.balance", "inventory"}, new String[] {"ar.balance", "finance"},
                new String[] {"occupancy.rate", "leasing"}, new String[] {"wo.backlog", "facility"})) {
            repository.upsertMetricSnapshot(new MetricSnapshotKey(stock[0], stock[1], "daily", d, "platform", null),
                    BigDecimal.valueOf(999), "etl_aggregate", earlier);
        }
        repository.upsertMetricSnapshot(new MetricSnapshotKey("occupancy.rate", "leasing", "monthly", d.withDayOfMonth(1), "platform", null),
                new BigDecimal("0.5"), "etl_aggregate", earlier);
        jdbc.update("INSERT INTO analytics_revenue_refresh_dates (business_date, requested_at) VALUES (?, now())", d);

        job(today.atTime(12, 0)).scheduledRefresh();

        assertThat(snapshot("revenue.thirdparty", "daily", d)).isEqualByComparingTo("10000");
        assertThat(snapshot("revenue.contract_rent", "daily", d)).isEqualByComparingTo("30000");
        assertThat(snapshot("revenue.net", "daily", d)).isEqualByComparingTo("30000");
        // 月度行按日行重卷（流量类月累计）。
        for (String key : List.of("revenue.net", "revenue.thirdparty")) {
            assertThat(snapshot(key, "monthly", d.withDayOfMonth(1))).isEqualByComparingTo(jdbc.queryForObject("""
                    SELECT SUM(actual_value) FROM analytics_business_metric_snapshots
                     WHERE metric_key = ? AND period_type = 'daily' AND segment_type = 'platform'
                       AND business_date >= ? AND business_date <= ?
                    """, BigDecimal.class, key, d.withDayOfMonth(1), d.withDayOfMonth(d.lengthOfMonth())));
        }
        for (String stock : List.of("supply.balance", "ar.balance", "occupancy.rate", "wo.backlog")) {
            assertThat(snapshot(stock, "daily", d)).as(stock).isEqualByComparingTo("999");
        }
        assertThat(snapshot("occupancy.rate", "monthly", d.withDayOfMonth(1))).isEqualByComparingTo("0.5");
        // 商户汇总是「每商户一行最新态」，只随当日刷新，不被历史日改写。
        assertThat(jdbc.queryForObject("SELECT business_date FROM analytics_merchant_summaries WHERE merchant_id = ?",
                LocalDate.class, merchant)).isEqualTo(today);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM analytics_revenue_refresh_dates WHERE business_date = ?",
                Integer.class, d)).isZero();
    }

    @Test
    void scheduledRefreshKeepsYesterdayTailInBarAndMerchantWindows() {
        LocalDate d = LocalDate.of(2097, 9, 10);
        LocalDate next = d.plusDays(1);
        String bar = "mch_rrbar_" + suffix;
        String catering = "mch_rrcat_" + suffix;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at) "
                + "VALUES (?, ?, 'catering', 'active', now(), now())", catering, catering);
        // 次日 04:30 结清的酒吧账单属酒吧营业日 D（06:00 起算）；次日 09:00 入账的餐饮回流属商户营业日 D（10:00 起算）。
        settledBarBill(bar, 50_000, next.atTime(4, 30).atZone(LocalZone.ID).toInstant());
        reflow("tail", "catering", catering, 6_000, next.atTime(9, 0).atZone(LocalZone.ID).toInstant());

        job(next.atTime(9, 30)).scheduledRefresh();

        assertThat(snapshot("revenue.bar", "daily", d)).isEqualByComparingTo("50000");
        assertThat(snapshot("revenue.thirdparty", "daily", d)).isEqualByComparingTo("6000");

        // 各域昨日窗都已收尾后不再重算昨日。
        repository.upsertMetricSnapshot(new MetricSnapshotKey("revenue.bar", "bar", "daily", d, "platform", null),
                BigDecimal.valueOf(777), "etl_aggregate", Instant.parse("2097-09-11T03:00:00Z"));
        job(next.atTime(11, 0)).scheduledRefresh();
        assertThat(snapshot("revenue.bar", "daily", d)).isEqualByComparingTo("777");
    }

    private DashboardAggregationJob job(java.time.LocalDateTime beijing) {
        DashboardAggregationJob job = new DashboardAggregationJob(repository,
                Clock.fixed(beijing.atZone(LocalZone.ID).toInstant(), LocalZone.ID), businessDayResolver, true);
        job.setRevenueRefreshMarks(marks);
        return job;
    }

    private BigDecimal snapshot(String key, String period, LocalDate date) {
        List<BigDecimal> values = jdbc.queryForList("""
                SELECT actual_value FROM analytics_business_metric_snapshots
                 WHERE metric_key = ? AND period_type = ? AND business_date = ? AND segment_type = 'platform' AND segment_id IS NULL
                """, BigDecimal.class, key, period, date);
        return values.isEmpty() ? null : values.getFirst();
    }

    private void reflow(String tag, String businessType, String merchantId, long cent, Instant at) {
        jdbc.update("""
                INSERT INTO integration_order_syncs (id, provider_code, business_type, sync_type, external_business_id, status,
                        amount_cent, created_at, updated_at, merchant_id)
                VALUES (?, 'pos_rr_test', ?, 'order', ?, 'synced', ?, ?, ?, ?)
                """, "ios_rr_" + tag + "_" + suffix, businessType, "ext_rr_" + tag + "_" + suffix, cent,
                Timestamp.from(at), Timestamp.from(at), merchantId);
    }

    /** 整单足额结清、无服务费与小费的酒吧账单（综合体取自卡座所在空间）。 */
    private void settledBarBill(String merchant, long cent, Instant settledAt) {
        String project = "prj_rr_" + suffix;
        String space = "spc_rr_" + suffix;
        String table = "tbl_rr_" + suffix;
        String order = "ord_rr_" + suffix;
        String bill = "bil_rr_" + suffix;
        Timestamp at = Timestamp.from(settledAt);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','重算综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at) VALUES(?,'重算酒吧','bar',?,'active',now(),now())",
                merchant, project);
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) VALUES(?,?,?,'卡座区','unit','active',now(),now())",
                space, project, merchant);
        jdbc.update("INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,capacity,allowed_pass_free_guest_count,min_consumption_cent,currency,status,created_at,updated_at,space_id) "
                + "VALUES(?,?,?,'T','卡座','card_seat',4,0,0,'CNY','active',now(),now(),?)", table, merchant, merchant, space);
        jdbc.update("INSERT INTO bar_orders(id,bar_id,merchant_id,table_id,order_source,status,total_amount_cent,currency,created_at,updated_at) "
                + "VALUES(?,?,?,?,'staff','created',?,'CNY',?,?)", order, merchant, merchant, table, cent, at, at);
        jdbc.update("INSERT INTO bar_bills(id,bill_no,order_id,bar_id,merchant_id,table_id,subtotal_amount_cent,discount_amount_cent,deposit_deduct_amount_cent,payable_amount_cent,paid_amount_cent,currency,payment_status,settlement_status,tip_amount_cent,created_at,updated_at) "
                + "VALUES(?,?,?,?,?,?,?,0,0,?,?,'CNY','paid','settled',0,?,?)", bill, "B" + suffix, order, merchant, merchant, table, cent, cent, cent, at, at);
        jdbc.update("INSERT INTO bar_bill_settlements(id,bill_id,settle_channel,settlement_amount_cent,merchant_id,settled_at) VALUES(?,?,'aggregate_pay',?,?,?)",
                "pay_rr_" + suffix, bill, cent, merchant, at);
    }
}
