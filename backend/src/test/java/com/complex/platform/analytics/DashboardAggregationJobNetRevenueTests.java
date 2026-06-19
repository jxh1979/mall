package com.complex.platform.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.analytics.AnalyticsRecords.AiQualityResultRecord;
import com.complex.platform.analytics.AnalyticsRecords.AiQualityTaskRecord;
import com.complex.platform.analytics.AnalyticsRecords.BusinessMetricSnapshotRecord;
import com.complex.platform.analytics.AnalyticsRecords.CustomerIntentRecord;
import com.complex.platform.analytics.AnalyticsRecords.DashboardSnapshotRecord;
import com.complex.platform.analytics.AnalyticsRecords.MerchantHealthScoreRecord;
import com.complex.platform.analytics.AnalyticsRecords.MerchantSummaryRecord;
import com.complex.platform.analytics.AnalyticsRecords.MetricAnomalyRecord;
import com.complex.platform.analytics.AnalyticsRecords.MetricDefinitionRecord;
import com.complex.platform.analytics.AnalyticsRecords.MetricSnapshotKey;
import com.complex.platform.analytics.AnalyticsRecords.RiskAlertRecord;
import com.complex.platform.analytics.AnalyticsRecords.RiskRuleRecord;
import com.complex.platform.analytics.AnalyticsRecords.RobotMetricRecord;
import com.complex.platform.analytics.AnalyticsRecords.StaffServiceScoreRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * RC1②：revenue.net 派生指标聚合测试——验证净收入公式、第三方剔除、退款冲减经营日。
 */
class DashboardAggregationJobNetRevenueTests {

    private static final LocalDate BUSINESS_DATE = LocalDate.parse("2026-06-18");

    private String netKey(String segment) {
        return DashboardAggregationJob.REVENUE_NET + "|" + segment;
    }

    @Test
    void platformNetIsTicketPlusBarMinusRefundsAndExcludesThirdParty() {
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        // 自营售票实付 100000，售票退款 10000；
        // 酒吧实收 merchant_a=80000 / merchant_b=20000，酒吧退款 merchant_a=5000；
        // 第三方回流（revenue.thirdparty 未就绪 / 不参与聚合）即便存在也不计入 net。
        repository.ticketRevenue = new BigDecimal("100000");
        repository.ticketRefund = new BigDecimal("10000");
        repository.barRevenue = orderedMap("merchant_a", "80000", "merchant_b", "20000");
        repository.barRefund = orderedMap("merchant_a", "5000");

        DashboardAggregationJob job = new DashboardAggregationJob(repository);
        job.refreshSnapshots(BUSINESS_DATE);

        // 平台 net = 100000 + (80000+20000) − 10000 − 5000 = 185000。
        assertThat(repository.upserts.get(netKey("platform"))).isEqualByComparingTo("185000");
        // 商户行级 net = 本商户酒吧实收 − 本商户酒吧退款。
        assertThat(repository.upserts.get(netKey("merchant_a"))).isEqualByComparingTo("75000");
        assertThat(repository.upserts.get(netKey("merchant_b"))).isEqualByComparingTo("20000");
        // 售票退款单列（平台口径）。
        assertThat(repository.upserts.get(DashboardAggregationJob.REVENUE_TICKET_REFUND + "|platform"))
                .isEqualByComparingTo("10000");
    }

    @Test
    void merchantNetIncludesTicketAllocatedShareAndRefundShare() {
        // 清单#4：商户行级 net = 本商户售票分量(allocated) + 本商户酒吧实收 − 本商户售票退款分摊 − 本商户酒吧退款。
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        // 平台售票实付 100000、售票退款 12000（平台口径维持现有，不改用 allocated）。
        repository.ticketRevenue = new BigDecimal("100000");
        repository.ticketRefund = new BigDecimal("12000");
        // 售票分量按商户（allocated）：A=70000、B=30000（Σ=100000=订单实付，无 direct 出票差额）。
        repository.ticketAllocated = orderedMap("merchant_a", "70000", "merchant_b", "30000");
        // 售票退款分摊：A=8000、B=4000（Σ=12000=平台售票退款，按 allocated 占比分摊）。
        repository.ticketRefundByMerchant = orderedMap("merchant_a", "8000", "merchant_b", "4000");
        // 酒吧实收 A=80000 / B=20000；酒吧退款 A=5000。
        repository.barRevenue = orderedMap("merchant_a", "80000", "merchant_b", "20000");
        repository.barRefund = orderedMap("merchant_a", "5000");

        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);

        // A: 70000 + 80000 − 8000 − 5000 = 137000。
        assertThat(repository.upserts.get(netKey("merchant_a"))).isEqualByComparingTo("137000");
        // B: 30000 + 20000 − 4000 − 0 = 46000。
        assertThat(repository.upserts.get(netKey("merchant_b"))).isEqualByComparingTo("46000");
        // 平台 net 维持现有口径：100000 + (80000+20000) − 12000 − 5000 = 183000（不改用 allocated）。
        assertThat(repository.upserts.get(netKey("platform"))).isEqualByComparingTo("183000");
        // 一致性（无 direct 出票差额时）：Σ商户 net == 平台 net。
        BigDecimal merchantSum = repository.upserts.get(netKey("merchant_a"))
                .add(repository.upserts.get(netKey("merchant_b")));
        assertThat(merchantSum).isEqualByComparingTo(repository.upserts.get(netKey("platform")));
    }

    @Test
    void merchantNetTicketShareCanCoverMerchantWithoutBarActivity() {
        // 商户只有售票分量、无酒吧活动：net 仍应体现售票分量（行只来自 ticketAllocated 也能产出）。
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        repository.ticketRevenue = new BigDecimal("40000");
        repository.ticketRefund = BigDecimal.ZERO;
        repository.ticketAllocated = orderedMap("merchant_c", "40000");
        repository.barRevenue = Map.of();
        repository.barRefund = Map.of();

        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);

        assertThat(repository.upserts.get(netKey("merchant_c"))).isEqualByComparingTo("40000");
        assertThat(repository.upserts.get(netKey("platform"))).isEqualByComparingTo("40000");
    }

    @Test
    void platformNetIgnoresDirectIssuanceGapButStaysConsistentForAllocatedOrders() {
        // direct 出票（无 allocated）造成平台售票实付 > Σ商户售票分量：平台口径维持全额，差额不归商户（保守）。
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        repository.ticketRevenue = new BigDecimal("100000"); // 含 30000 direct 出票（无 allocated）。
        repository.ticketRefund = BigDecimal.ZERO;
        repository.ticketAllocated = orderedMap("merchant_a", "70000"); // 仅有发券订单进商户分量。
        repository.barRevenue = Map.of();
        repository.barRefund = Map.of();

        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);

        // 商户 net 仅含可归属的 70000；平台 net 维持 100000（差额 30000 是 direct 出票，保守不归商户、不重复）。
        assertThat(repository.upserts.get(netKey("merchant_a"))).isEqualByComparingTo("70000");
        assertThat(repository.upserts.get(netKey("platform"))).isEqualByComparingTo("100000");
    }

    @Test
    void netDeductsRefundOnTheBusinessDayTheRefundOccurred() {
        // 退款冲减当日：聚合按经营日（BusinessDayResolver 窗口）取数，退款随当日 ticketRefund/barRefund 入 net。
        CapturingAnalyticsRepository sameDay = new CapturingAnalyticsRepository();
        sameDay.ticketRevenue = new BigDecimal("50000");
        sameDay.ticketRefund = new BigDecimal("8000");
        sameDay.barRevenue = orderedMap("merchant_a", "0");
        sameDay.barRefund = Map.of();
        new DashboardAggregationJob(sameDay).refreshSnapshots(BUSINESS_DATE);
        // 当日含退款：50000 − 8000 = 42000。
        assertThat(sameDay.upserts.get(netKey("platform"))).isEqualByComparingTo("42000");

        CapturingAnalyticsRepository noRefund = new CapturingAnalyticsRepository();
        noRefund.ticketRevenue = new BigDecimal("50000");
        noRefund.ticketRefund = BigDecimal.ZERO;
        noRefund.barRevenue = orderedMap("merchant_a", "0");
        noRefund.barRefund = Map.of();
        new DashboardAggregationJob(noRefund).refreshSnapshots(BUSINESS_DATE);
        // 无退款日：净收入不被该退款冲减。
        assertThat(noRefund.upserts.get(netKey("platform"))).isEqualByComparingTo("50000");
    }

    @Test
    void netIsNullSafeWhenComponentsMissing() {
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        repository.ticketRevenue = null;
        repository.ticketRefund = null;
        repository.barRevenue = Map.of();
        repository.barRefund = Map.of();

        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);

        // 分量缺失按 0 处理，net=0（不抛 NPE）。
        assertThat(repository.upserts.get(netKey("platform"))).isEqualByComparingTo("0");
    }

    @Test
    void redemptionRateIsVerifyDoneOverIssuedByMerchantAndPlatform() {
        // 清单#18：券核销率 = 核销数/发放数。商户行用本商户口径，平台行用 Σ 口径；分母为 0 置 0（不除零）。
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        // 核销：A=30、B=10；发放：A=120、B=40、C=50（C 当日只发放未核销）。
        repository.verifyDone = orderedMap("merchant_a", "30", "merchant_b", "10");
        repository.ticketIssued = orderedMap("merchant_a", "120", "merchant_b", "40", "merchant_c", "50");

        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);

        String rate = DashboardAggregationJob.REDEMPTION_RATE;
        // A: 30/120 = 0.2500；B: 10/40 = 0.2500；C: 0/50 = 0.0000。
        assertThat(repository.upserts.get(rate + "|merchant_a")).isEqualByComparingTo("0.2500");
        assertThat(repository.upserts.get(rate + "|merchant_b")).isEqualByComparingTo("0.2500");
        assertThat(repository.upserts.get(rate + "|merchant_c")).isEqualByComparingTo("0.0000");
        // 平台 Σ核销=40 / Σ发放=210 = 0.1905（HALF_UP 4 位）。
        assertThat(repository.upserts.get(rate + "|platform")).isEqualByComparingTo("0.1905");
    }

    @Test
    void redemptionRateIsZeroWhenNoIssuance() {
        // 当日无发放：分母为 0，核销率置 0（不伪造、不除零）。
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        repository.verifyDone = orderedMap("merchant_a", "5");
        repository.ticketIssued = Map.of();

        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);

        // merchant_a 仅有核销无发放：分母 0 → 0；平台同理。
        assertThat(repository.upserts.get(DashboardAggregationJob.REDEMPTION_RATE + "|merchant_a"))
                .isEqualByComparingTo("0");
        assertThat(repository.upserts.get(DashboardAggregationJob.REDEMPTION_RATE + "|platform"))
                .isEqualByComparingTo("0");
    }

    @Test
    void etlEmitsProjectSegmentRowsMirroringPlatformTotalsForSingleComplex() {
        // ⑤ 集团多项目 ETL：refreshSnapshots 在 platform/merchant 维度之外，按 project_id 落 segment_type='project' 行。
        // 单综合体默认全部归 DEFAULT_PROJECT_ID，project 口径值 = 平台口径合计（口径与现状一致，不伪造多 project）。
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        repository.ticketRevenue = new BigDecimal("100000");
        repository.ticketRefund = new BigDecimal("10000");
        repository.barRevenue = orderedMap("merchant_a", "80000", "merchant_b", "20000");
        repository.barRefund = orderedMap("merchant_a", "5000");

        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);

        String projectId = DashboardAggregationJob.DEFAULT_PROJECT_ID;
        // project 口径 net == 平台口径 net == 185000（同一综合体合计，不漂移）。
        assertThat(repository.upserts.get(netKey("project:" + projectId)))
                .isEqualByComparingTo(repository.upserts.get(netKey("platform")))
                .isEqualByComparingTo("185000");
        // project 口径同时覆盖售票实付平台合计 100000。
        assertThat(repository.upserts.get(DashboardAggregationJob.REVENUE_TICKET + "|project:" + projectId))
                .isEqualByComparingTo("100000");
    }

    private static Map<String, BigDecimal> orderedMap(String... keyValues) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], new BigDecimal(keyValues[i + 1]));
        }
        return map;
    }

    private static final class CapturingAnalyticsRepository implements AnalyticsRepository {
        private final Map<String, BigDecimal> upserts = new LinkedHashMap<>();
        private BigDecimal ticketRevenue = BigDecimal.ZERO;
        private BigDecimal ticketRefund = BigDecimal.ZERO;
        private Map<String, BigDecimal> barRevenue = Map.of();
        private Map<String, BigDecimal> barRefund = Map.of();
        private Map<String, BigDecimal> ticketAllocated = Map.of();
        private Map<String, BigDecimal> ticketRefundByMerchant = Map.of();
        private Map<String, BigDecimal> verifyDone = Map.of();
        private Map<String, BigDecimal> ticketIssued = Map.of();

        @Override
        public void upsertMetricSnapshot(MetricSnapshotKey key, BigDecimal actualValue, String projectionMethod, Instant createdAt) {
            String segment;
            if ("merchant".equals(key.segmentType())) {
                segment = key.segmentId();
            } else if ("project".equals(key.segmentType())) {
                // ⑤ 集团多项目 ETL：project 口径单独成键，避免与平台口径互相覆盖。
                segment = "project:" + key.segmentId();
            } else {
                segment = "platform";
            }
            upserts.put(key.metricKey() + "|" + segment, actualValue);
        }

        @Override
        public BigDecimal sumTicketRevenueCents(LocalDate businessDate) {
            return ticketRevenue;
        }

        @Override
        public BigDecimal sumTicketRefundCents(LocalDate businessDate) {
            return ticketRefund;
        }

        @Override
        public Map<String, BigDecimal> sumBarRevenueCentsByMerchant(LocalDate businessDate) {
            return barRevenue;
        }

        @Override
        public Map<String, BigDecimal> sumBarRefundCentsByMerchant(LocalDate businessDate) {
            return barRefund;
        }

        @Override
        public Map<String, BigDecimal> sumTicketAllocatedRevenueByMerchant(LocalDate businessDate) {
            return ticketAllocated;
        }

        @Override
        public Map<String, BigDecimal> sumTicketRefundByMerchant(LocalDate businessDate) {
            return ticketRefundByMerchant;
        }

        // ── 其余聚合源：本测试不关心，返回中性值 ──
        @Override
        public Map<String, BigDecimal> sumTicketBarShareCentsByMerchant(LocalDate businessDate) {
            return Map.of();
        }

        @Override
        public Map<String, BigDecimal> sumTicketAmusementShareCentsByMerchant(LocalDate businessDate) {
            return Map.of();
        }

        @Override
        public BigDecimal countAmusementVerificationSuccess(LocalDate businessDate) {
            return BigDecimal.ZERO;
        }

        @Override
        public Map<String, BigDecimal> countTicketVerificationSuccessByMerchant(LocalDate businessDate) {
            return verifyDone;
        }

        @Override
        public Map<String, BigDecimal> countTicketIssuedByMerchant(LocalDate businessDate) {
            return ticketIssued;
        }

        @Override
        public BigDecimal countNewMembers(LocalDate businessDate) {
            return BigDecimal.ZERO;
        }

        @Override
        public Map<String, BigDecimal> sumInventoryBalanceByMerchant() {
            return Map.of();
        }

        @Override
        public Map<String, BigDecimal> countLowStockByMerchant() {
            return Map.of();
        }

        @Override
        public BigDecimal countPendingReview() {
            return BigDecimal.ZERO;
        }

        @Override
        public long countMerchantsWithEtlSnapshot(LocalDate businessDate) {
            return 0L;
        }

        @Override
        public Map<String, BigDecimal> readMetricSnapshots(LocalDate businessDate, String scope) {
            return Map.of();
        }

        @Override
        public Optional<LocalDate> latestEtlBusinessDate(String scope) {
            return Optional.empty();
        }

        // ── 读模型：本测试不使用 ──
        @Override
        public List<AiQualityResultRecord> listAiQualityResults() {
            return List.of();
        }

        @Override
        public List<AiQualityTaskRecord> listAiQualityTasks() {
            return List.of();
        }

        @Override
        public AiQualityTaskRecord saveAiQualityTask(AiQualityTaskRecord task) {
            return task;
        }

        @Override
        public AiQualityResultRecord saveAiQualityResult(AiQualityResultRecord result) {
            return result;
        }

        @Override
        public List<CustomerIntentRecord> listCustomerIntents() {
            return List.of();
        }

        @Override
        public DashboardSnapshotRecord getP0OverviewDashboard() {
            return null;
        }

        @Override
        public List<BusinessMetricSnapshotRecord> listBusinessMetricSnapshots() {
            return List.of();
        }

        @Override
        public List<MerchantHealthScoreRecord> listMerchantHealthScores() {
            return List.of();
        }

        @Override
        public List<MerchantSummaryRecord> listAnalyticsMerchants() {
            return List.of();
        }

        @Override
        public List<MetricAnomalyRecord> listMetricAnomalies() {
            return List.of();
        }

        @Override
        public List<MetricDefinitionRecord> listMetricDefinitions() {
            return List.of();
        }

        @Override
        public List<RiskAlertRecord> listRiskAlerts() {
            return List.of();
        }

        @Override
        public RiskAlertRecord saveRiskAlert(RiskAlertRecord alert) {
            return alert;
        }

        @Override
        public Optional<RiskAlertRecord> findRiskAlertById(String id) {
            return Optional.empty();
        }

        @Override
        public RiskAlertRecord updateRiskAlert(RiskAlertRecord alert) {
            return alert;
        }

        @Override
        public List<RiskRuleRecord> listRiskRules() {
            return List.of();
        }

        @Override
        public List<RobotMetricRecord> listRobotMetrics() {
            return List.of();
        }

        @Override
        public List<StaffServiceScoreRecord> listStaffServiceScores() {
            return List.of();
        }
    }
}
