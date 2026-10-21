package com.complex.platform.analytics;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.LocalZone;
import com.complex.platform.support.TestSecurityActors;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-04-R1 第三方回流收入（BSM「经营看板」章「主要指标」：「收入：售票收入、酒吧收入、第三方回流收入、退款、净收入。」；
 * BSM「回流订单按该映射认定商户，映射缺失的回流单挂差异，不得自动入账」；ENUM「回流结果状态」的「已入账口径」；
 * r5-aggregation-metrics-draft「第三方回流仅计商户结算口径，不计平台净收入」定稿）：
 * 只计餐饮/零售已入账、已落商户的回流订单，按回流落库时刻落商户营业日（10:00 起算）；渠道团购票已本地出票计入售票收入、不再计；
 * 不并入净收入；卡片数字＝明细合计。
 *
 * <p>真库、随测试事务回滚；日期取很靠后的营业日，库里没有同窗的回流行。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class ThirdpartyRevenueMetricJdbcIntegrationTests {

    private static final LocalDate D = LocalDate.of(2097, 5, 12);

    @Autowired private AnalyticsService service;
    @Autowired private AnalyticsRepository repository;
    @Autowired private BusinessDayResolver businessDayResolver;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String merchantA = "mch_tp_a_" + suffix;
    private final String merchantB = "mch_tp_b_" + suffix;

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
        for (String id : java.util.List.of(merchantA, merchantB)) {
            jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at) "
                    + "VALUES (?, ?, 'catering', 'active', now(), now())", id, id);
        }
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    @Test
    void thirdpartyRevenueCountsBookedCateringRetailOrdersOnlyAndNeverEntersNet() {
        DashboardAggregationJob job = new DashboardAggregationJob(repository, businessDayResolver, false);
        job.refreshSnapshots(D);
        BigDecimal netBefore = service.overview(Optional.empty(), D).metrics().get("revenue.net").value();

        Instant inDay = D.atTime(11, 0).atZone(LocalZone.ID).toInstant();
        Instant beforeCutoff = D.atTime(9, 30).atZone(LocalZone.ID).toInstant(); // 商户营业日 10:00 起算 → 属前一营业日
        reflow("c1", "catering", "order", "synced", merchantA, 10_000, inDay);
        reflow("r1", "retail", "order", "synced", merchantB, 5_000, inDay);
        reflow("nomap", "catering", "order", "synced", null, 7_000, inDay);          // 映射缺失：挂差异，不入账
        reflow("tkt", "ticket", "order", "synced", merchantA, 9_000, inDay);          // 渠道团购票：已计入售票收入
        reflow("rf", "catering", "refund", "synced", merchantA, 2_000, inDay);        // 退款回流不是已入账订单
        reflow("fail", "catering", "order", "failed", merchantA, 3_000, inDay);       // 未入账
        reflow("prev", "catering", "order", "synced", merchantA, 4_000, beforeCutoff); // 前一营业日
        job.refreshSnapshots(D);

        assertThat(service.overview(Optional.empty(), D).metrics().get("revenue.thirdparty").value())
                .isEqualByComparingTo("15000");
        assertThat(service.overview(Optional.of(merchantA), D).metrics().get("revenue.thirdparty").value())
                .isEqualByComparingTo("10000");
        assertThat(service.overview(Optional.of(merchantB), D).metrics().get("revenue.thirdparty").value())
                .isEqualByComparingTo("5000");
        // 不并入净收入：插入七行回流前后平台净收入不变。
        assertThat(service.overview(Optional.empty(), D).metrics().get("revenue.net").value())
                .isEqualByComparingTo(netBefore);

        // 卡片数字＝明细合计：明细只出两行已入账餐饮/零售回流。
        AnalyticsRecords.MetricDetailResponse detail = service.metricDetail("revenue.thirdparty", null, D, D, 1, 20);
        assertThat(detail.summaryValue()).isEqualByComparingTo("15000");
        assertThat(detail.totalRows()).isEqualTo(2);
        assertThat(detail.rows()).extracting(row -> row.get("externalBusinessId"))
                .containsExactlyInAnyOrder("ext_c1_" + suffix, "ext_r1_" + suffix);
    }

    private void reflow(String tag, String businessType, String syncType, String status, String merchantId, long cent, Instant at) {
        jdbc.update("""
                INSERT INTO integration_order_syncs (id, provider_code, business_type, sync_type, external_business_id, status,
                        amount_cent, created_at, updated_at, merchant_id)
                VALUES (?, 'pos_tp_test', ?, ?, ?, ?, ?, ?, ?, ?)
                """, "ios_tp_" + tag + "_" + suffix, businessType, syncType, "ext_" + tag + "_" + suffix, status, cent,
                Timestamp.from(at), Timestamp.from(at), merchantId);
    }
}
