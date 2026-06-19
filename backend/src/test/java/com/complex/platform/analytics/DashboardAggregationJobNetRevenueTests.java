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

        @Override
        public void upsertMetricSnapshot(MetricSnapshotKey key, BigDecimal actualValue, String projectionMethod, Instant createdAt) {
            String segment = "merchant".equals(key.segmentType()) ? key.segmentId() : "platform";
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
            return Map.of();
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
