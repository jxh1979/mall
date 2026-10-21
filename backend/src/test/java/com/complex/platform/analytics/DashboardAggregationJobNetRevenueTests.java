package com.complex.platform.analytics;

// 文件版本：V0.03

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
    void occupancyRateUsesTotalAreaIncludingUnavailableAndSkipsWhenTotalZero() {
        // 总面积含不可租；可租面积仍单列供空置和能耗使用。
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        repository.occupancy = new AnalyticsRecords.OccupancyAreas(
                new BigDecimal("1000"), new BigDecimal("800"), new BigDecimal("120"),
                new BigDecimal("200"), new BigDecimal("50"), new BigDecimal("1250"));
        repository.expiringContracts = 7;

        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);

        assertThat(repository.upserts.get(DashboardAggregationJob.OCC_RATE + "|platform")).isEqualByComparingTo("0.6400");
        assertThat(repository.upserts.get(DashboardAggregationJob.OCC_TOTAL_AREA + "|platform")).isEqualByComparingTo("1250");
        assertThat(repository.upserts.get(DashboardAggregationJob.OCC_SIGNED_PENDING_AREA + "|platform")).isEqualByComparingTo("120");
        assertThat(repository.upserts.get(DashboardAggregationJob.OCC_PARKING_RATE + "|platform")).isEqualByComparingTo("0.2500");
        assertThat(repository.upserts.get(DashboardAggregationJob.CONTRACT_EXPIRING_90D + "|platform")).isEqualByComparingTo("7");

        CapturingAnalyticsRepository zero = new CapturingAnalyticsRepository();
        new DashboardAggregationJob(zero).refreshSnapshots(BUSINESS_DATE);
        assertThat(zero.upserts).doesNotContainKey(DashboardAggregationJob.OCC_RATE + "|platform");
        assertThat(zero.upserts).doesNotContainKey(DashboardAggregationJob.OCC_PARKING_RATE + "|platform");
    }

    /**
     * HP-P01 R10（BSM「停车场」章「连接经营看板」、BSM「出租」条「车位出租率单独列（月租和员工车位占用计入）」；第十二批操作员裁决补充第 5～7 条）：
     * 五项停车指标按各项目当日事实合计后求比，分母为 0 不产出；车位出租率按车位个数（合同已租＋在职员工登记车辆）÷车位个数。
     */
    @Test
    void parkingMetricsEmittedFromDayFactsAndRentalRateCountsSpaces() {
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        repository.occupancy = new AnalyticsRecords.OccupancyAreas(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("10"), new BigDecimal("4"), BigDecimal.ZERO);
        repository.parkingFacts = Map.of(
                "p1", new AnalyticsRecords.ParkingDayFacts(100, 200, 30, 100, 3, 1, 4, 390, 20_800),
                "p2", new AnalyticsRecords.ParkingDayFacts(50, 50, 10, 50, 1, 0, 1, 30, 200));

        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);

        assertThat(repository.upserts.get("parking.utilization_rate|platform")).isEqualByComparingTo("0.6000");
        assertThat(repository.upserts.get("parking.daily_turnover|platform")).isEqualByComparingTo("0.2667");
        assertThat(repository.upserts.get("parking.revenue|platform")).isEqualByComparingTo("21000");
        assertThat(repository.upserts.get("parking.credit_share|platform")).isEqualByComparingTo("0.2500");
        assertThat(repository.upserts.get("parking.avg_stay_minutes|platform")).isEqualByComparingTo("84.00");
        assertThat(repository.upserts.get(DashboardAggregationJob.OCC_PARKING_RATE + "|platform")).isEqualByComparingTo("0.4000");

        CapturingAnalyticsRepository zero = new CapturingAnalyticsRepository();
        new DashboardAggregationJob(zero).refreshSnapshots(BUSINESS_DATE);
        assertThat(zero.upserts.keySet()).noneMatch(key -> key.startsWith("parking."));
    }

    @Test
    void anomalyDetectionSkipsHolidayDates() {
        // H1(2026-07-02 拍板):节假日暴涨暴跌是正常经营不是异常——经营日落在节假日区间内自动免报。
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        repository.anomalyConfig = new AnalyticsRecords.AnomalyConfig(
                new BigDecimal("0.5"), new BigDecimal("0.8"), true, null, null);
        repository.snapshotsByDate = Map.of(
                BUSINESS_DATE, Map.of("revenue.net", new BigDecimal("1900000")),
                BUSINESS_DATE.minusDays(1), Map.of("revenue.net", new BigDecimal("1000000")));
        repository.withinHoliday = true;

        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);

        assertThat(repository.insertedAnomalies).isEmpty();
    }

    @Test
    void refreshTriggersMerchantSummaryRollup() {
        // M4:每次日刷后重算商户经营汇总(每商户一行最新态)。
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);
        assertThat(repository.refreshedMerchantSummariesFor).isEqualTo(BUSINESS_DATE);
    }

    @Test
    void refreshSyncsDefinitionsAndDetectsDayOverDayAnomalies() {
        // M3(2026-07-02 拍板):环比突变 ±50% 记可疑/±80% 记高;小基数不报(淡季防误报);
        // mtd 类月初天然重置不检测;指标定义由代码目录同步进表(单一事实源投影,异常 FK 前置)。
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        repository.anomalyConfig = new AnalyticsRecords.AnomalyConfig(
                new BigDecimal("0.5"), new BigDecimal("0.8"), true, null, null);
        repository.snapshotsByDate = Map.of(
                BUSINESS_DATE, Map.of(
                        "revenue.net", new BigDecimal("1900000"),
                        "member.new", new BigDecimal("26"),
                        "occupancy.rate", new BigDecimal("0.8"),
                        "ar.collected_mtd", new BigDecimal("5"),
                        "wo.backlog", new BigDecimal("1")),
                BUSINESS_DATE.minusDays(1), Map.of(
                        "revenue.net", new BigDecimal("1000000"),
                        "member.new", new BigDecimal("20"),
                        "occupancy.rate", new BigDecimal("0.5"),
                        "ar.collected_mtd", new BigDecimal("999999"),
                        "wo.backlog", new BigDecimal("4")));

        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);

        assertThat(repository.syncedDefinitions).extracting(AnalyticsRecords.MetricDefinitionSyncRow::metricKey)
                .contains("revenue.net", "occupancy.rate", "wo.backlog");
        assertThat(repository.insertedAnomalies).containsEntry("revenue.net", "high");          // +90%
        assertThat(repository.insertedAnomalies).containsEntry("occupancy.rate", "suspicious"); // +60%
        assertThat(repository.insertedAnomalies).doesNotContainKey("member.new");               // +30% 不到阈值
        assertThat(repository.insertedAnomalies).doesNotContainKey("ar.collected_mtd");         // mtd 排除
        assertThat(repository.insertedAnomalies).doesNotContainKey("wo.backlog");               // 双侧小基数(<10)不报

        CapturingAnalyticsRepository disabled = new CapturingAnalyticsRepository();
        disabled.anomalyConfig = new AnalyticsRecords.AnomalyConfig(
                new BigDecimal("0.5"), new BigDecimal("0.8"), false, null, null);
        disabled.snapshotsByDate = repository.snapshotsByDate;
        new DashboardAggregationJob(disabled).refreshSnapshots(BUSINESS_DATE);
        assertThat(disabled.insertedAnomalies).isEmpty();
    }

    @Test
    void refreshTriggersMonthlyRollupWithMixedAggregationWhitelists() {
        // M1:每次日刷后滚动更新当月 monthly 行;流量类 SUM/存量比率类期末 LAST 两白名单,名单外不做(不伪造)。
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);
        assertThat(repository.rolledUpMonthFirst).isEqualTo(BUSINESS_DATE.withDayOfMonth(1));
        assertThat(repository.rolledUpUpTo).isEqualTo(BUSINESS_DATE);
        assertThat(repository.rolledUpSumKeys).contains("revenue.net", "member.new", "energy.total_electricity");
        assertThat(repository.rolledUpLastKeys).contains("ar.balance", "occupancy.rate", "wo.backlog", "mwo.backlog");
        assertThat(repository.rolledUpSumKeys).doesNotContain("wo.overdue_rate", "wo.response_hours_p50", "mwo.overdue_rate", "mwo.handle_hours_p50");
        assertThat(repository.rolledUpLastKeys).doesNotContain("wo.overdue_rate", "wo.response_hours_p50", "mwo.overdue_rate", "mwo.handle_hours_p50");
    }

    @Test
    void workOrderSlaAndEnergyMetricsFollowRulings() {
        // S4:超时率=超时/处理时限落窗单数(分母0不产出);积压=时点存量。
        // S5:能耗按表计类型分列(单位不同不跨类合计);电耗/㎡分母=可租面积(与出租率同分母,分母0不产出)。
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        repository.workOrderSla = new AnalyticsRecords.WorkOrderSla(
                new BigDecimal("2.5"), new BigDecimal("6"), 4, 1, 7);
        repository.merchantWorkOrderSla = new AnalyticsRecords.WorkOrderSla(
                null, new BigDecimal("5"), 4, 2, 3);
        repository.energyByType = Map.of("electricity", new BigDecimal("100.5"), "water", new BigDecimal("8"));
        repository.abnormalReadings = 3;
        repository.occupancy = new AnalyticsRecords.OccupancyAreas(
                new BigDecimal("201"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("999"));

        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);

        assertThat(repository.upserts.get("wo.response_hours_p50|platform")).isEqualByComparingTo("2.5");
        assertThat(repository.upserts.get("wo.handle_hours_p50|platform")).isEqualByComparingTo("6");
        assertThat(repository.upserts.get("wo.overdue_rate|platform")).isEqualByComparingTo("0.2500");
        assertThat(repository.upserts.get("wo.backlog|platform")).isEqualByComparingTo("7");
        // 清单#13:商户服务工单并入(无响应节点不产出响应P50;处理=提交→关单;超时率=2/4)。
        assertThat(repository.upserts.get("mwo.handle_hours_p50|platform")).isEqualByComparingTo("5");
        assertThat(repository.upserts.get("mwo.overdue_rate|platform")).isEqualByComparingTo("0.5000");
        assertThat(repository.upserts.get("mwo.backlog|platform")).isEqualByComparingTo("3");
        assertThat(repository.upserts).doesNotContainKey("mwo.response_hours_p50|platform");
        assertThat(repository.upserts.get("energy.total_electricity|platform")).isEqualByComparingTo("100.5");
        assertThat(repository.upserts.get("energy.total_water|platform")).isEqualByComparingTo("8");
        assertThat(repository.upserts.get("energy.electricity_per_sqm|platform")).isEqualByComparingTo("0.5000");
        assertThat(repository.upserts.get("energy.abnormal_count|platform")).isEqualByComparingTo("3");

        CapturingAnalyticsRepository zero = new CapturingAnalyticsRepository();
        new DashboardAggregationJob(zero).refreshSnapshots(BUSINESS_DATE);
        assertThat(zero.upserts).doesNotContainKey("wo.overdue_rate|platform");
        assertThat(zero.upserts).doesNotContainKey("mwo.overdue_rate|platform");
        assertThat(zero.upserts).doesNotContainKey("mwo.handle_hours_p50|platform");
        assertThat(zero.upserts).doesNotContainKey("wo.response_hours_p50|platform");
        assertThat(zero.upserts).doesNotContainKey("energy.electricity_per_sqm|platform");
    }

    @Test
    void arAgingBucketsSumToBalanceAndCollectionRateDerives() {
        // S2 口径断言:Σ四桶 == 欠费总额(平台+商户);回款率=月实收/月应收;分母0不产出。
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        repository.arAging = Map.of(
                "a0_30", orderedMap("merchant_a", "10000"),
                "a90p", orderedMap("merchant_a", "3000", "merchant_b", "7000"));
        repository.arCollected = new BigDecimal("60000");
        repository.arReceivableMtd = new BigDecimal("80000");

        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);

        assertThat(repository.upserts.get(DashboardAggregationJob.AR_AGING_0_30 + "|merchant_a")).isEqualByComparingTo("10000");
        assertThat(repository.upserts.get(DashboardAggregationJob.AR_AGING_90P + "|merchant_b")).isEqualByComparingTo("7000");
        assertThat(repository.upserts.get(DashboardAggregationJob.AR_BALANCE + "|merchant_a")).isEqualByComparingTo("13000");
        assertThat(repository.upserts.get(DashboardAggregationJob.AR_BALANCE + "|platform")).isEqualByComparingTo("20000");
        assertThat(repository.upserts.get(DashboardAggregationJob.AR_COLLECTION_RATE_MTD + "|platform")).isEqualByComparingTo("0.7500");

        // 分母 0:不产出回款率(不伪造)。
        CapturingAnalyticsRepository empty = new CapturingAnalyticsRepository();
        new DashboardAggregationJob(empty).refreshSnapshots(BUSINESS_DATE);
        assertThat(empty.upserts).doesNotContainKey(DashboardAggregationJob.AR_COLLECTION_RATE_MTD + "|platform");
    }

    @Test
    void barBusinessDayWindowSplitsBarKeyFromPlatformNetComponent() {
        // S0(2026-07-02 拍板):酒吧经营日 06:00 切、平台汇总基准 04:00——凌晨 04:00–06:00 的酒吧流水
        // 归酒吧域「前一经营日」(revenue.bar 键按 bar 窗),但平台对账归当日(net 分量按 platform 窗)。
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        repository.barRevenueByWindow = Map.of(
                "bar", orderedMap("merchant_a", "60000"),
                "platform", orderedMap("merchant_a", "80000"));
        repository.barRefundByWindow = Map.of();

        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);

        assertThat(repository.upserts.get(DashboardAggregationJob.REVENUE_BAR + "|merchant_a"))
                .isEqualByComparingTo("60000");
        assertThat(repository.upserts.get(netKey("merchant_a"))).isEqualByComparingTo("80000");
        assertThat(repository.upserts.get(netKey("platform"))).isEqualByComparingTo("80000");
    }

    /**
     * ROOT-04-R1（BSM「主要指标」收入「第三方回流收入」单列；r5-aggregation-metrics-draft「第三方回流仅计商户结算口径，不计平台净收入」定稿）：
     * 第三方回流按商户写快照、平台＝商户合计、按商户营业日窗取数；不并入任何商户或平台的净收入；与补录数量一并进月累计（流量类）。
     */
    @Test
    void thirdpartyRevenueSnapshottedPerMerchantButExcludedFromNet() {
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        repository.barRevenue = orderedMap("merchant_a", "80000");
        repository.thirdpartyRevenue = orderedMap("merchant_a", "10000", "merchant_b", "5000");

        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);

        assertThat(repository.upserts.get("revenue.thirdparty|merchant_a")).isEqualByComparingTo("10000");
        assertThat(repository.upserts.get("revenue.thirdparty|merchant_b")).isEqualByComparingTo("5000");
        assertThat(repository.upserts.get("revenue.thirdparty|platform")).isEqualByComparingTo("15000");
        assertThat(repository.thirdpartyWindow).isEqualTo("merchant");
        assertThat(repository.upserts.get(netKey("merchant_a"))).isEqualByComparingTo("80000");
        assertThat(repository.upserts).doesNotContainKey(netKey("merchant_b"));
        assertThat(repository.upserts.get(netKey("platform"))).isEqualByComparingTo("80000");
        assertThat(repository.rolledUpSumKeys).contains("revenue.thirdparty", "risk.manual_backfill");
    }

    @Test
    void platformNetIsTicketPlusBarMinusRefundsAndExcludesThirdParty() {
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        // 自营售票实付 100000，售票退款 10000；
        // 酒吧实收 merchant_a=80000 / merchant_b=20000，酒吧退款 merchant_a=5000；
        // 第三方回流（revenue.thirdparty 单列）不计入 net。
        repository.ticketRevenue = new BigDecimal("100000");
        repository.ticketRefund = new BigDecimal("10000");
        // 2d-3：售票分摊到商户（生产中 paid 订单出票必分摊到 entitlement.merchant）；平台 net 取 Σ商户 net（防漂移）。
        repository.ticketAllocated = orderedMap("merchant_a", "70000", "merchant_b", "30000");
        repository.ticketRefundByMerchant = orderedMap("merchant_a", "7000", "merchant_b", "3000");
        repository.barRevenue = orderedMap("merchant_a", "80000", "merchant_b", "20000");
        repository.barRefund = orderedMap("merchant_a", "5000");

        DashboardAggregationJob job = new DashboardAggregationJob(repository);
        job.refreshSnapshots(BUSINESS_DATE);

        // 商户 net：A=70000+80000−7000−5000=138000；B=30000+20000−3000=47000。
        assertThat(repository.upserts.get(netKey("merchant_a"))).isEqualByComparingTo("138000");
        assertThat(repository.upserts.get(netKey("merchant_b"))).isEqualByComparingTo("47000");
        // 平台 net = Σ商户 net = 138000 + 47000 = 185000（防漂移：平台=Σ商户，售票+酒吧−退款全经商户口径）。
        assertThat(repository.upserts.get(netKey("platform"))).isEqualByComparingTo("185000");
        // 售票退款单列（平台口径）。
        assertThat(repository.upserts.get(DashboardAggregationJob.REVENUE_TICKET_REFUND + "|platform"))
                .isEqualByComparingTo("10000");
    }

    @Test
    void perSqmDerivesNetDividedByOperatingAreaAndSkipsWhenAreaUnset() {
        // T4.2 坪效：revenue.per_sqm = 平台净收入 / 可经营面积；面积未配置则不产出（诚实空缺）。
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        repository.ticketRevenue = new BigDecimal("100000");
        repository.ticketAllocated = orderedMap("merchant_a", "100000"); // 平台 net = Σ商户 net = 100000。

        // 面积未配置 → 不产出 per_sqm。
        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);
        assertThat(repository.upserts).doesNotContainKey(DashboardAggregationJob.REVENUE_PER_SQM + "|platform");

        // 配置面积 1000㎡ → per_sqm = 100000 / 1000 = 100.00（平台 + project 两口径）。
        repository.operatingParams = new OperatingParamsRecord("project_1", new BigDecimal("1000"), "active",
                Instant.parse("2026-06-18T00:00:00Z"), Instant.parse("2026-06-18T00:00:00Z"));
        repository.upserts.clear();
        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);
        assertThat(repository.upserts.get(DashboardAggregationJob.REVENUE_PER_SQM + "|platform")).isEqualByComparingTo("100.00");
        assertThat(repository.upserts.get(DashboardAggregationJob.REVENUE_PER_SQM + "|project:project_1")).isEqualByComparingTo("100.00");
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
    void netIncludesContractRentReceivableAndEmitsRentComponentRow() {
        // P1-C 收入闭环：合同租金/物业/能源应收并入 net，并单列 revenue.contract_rent 展示行。
        CapturingAnalyticsRepository repository = new CapturingAnalyticsRepository();
        repository.barRevenue = orderedMap("merchant_a", "80000");
        // merchant_a 当日确认合同租金应收 30000；merchant_b 仅有租金 50000、无其他经营。
        repository.contractRent = orderedMap("merchant_a", "30000", "merchant_b", "50000");

        new DashboardAggregationJob(repository).refreshSnapshots(BUSINESS_DATE);

        // 商户 net：A=酒吧 80000 + 租金 30000 = 110000；B=租金 50000。
        assertThat(repository.upserts.get(netKey("merchant_a"))).isEqualByComparingTo("110000");
        assertThat(repository.upserts.get(netKey("merchant_b"))).isEqualByComparingTo("50000");
        // 平台 net = Σ商户 = 160000。
        assertThat(repository.upserts.get(netKey("platform"))).isEqualByComparingTo("160000");
        // 单列租金应收展示行：商户 + 平台合计 80000。
        String rent = DashboardAggregationJob.REVENUE_CONTRACT_RENT;
        assertThat(repository.upserts.get(rent + "|merchant_a")).isEqualByComparingTo("30000");
        assertThat(repository.upserts.get(rent + "|platform")).isEqualByComparingTo("80000");
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

        // 2d-3 防漂移：商户 net=可归属 70000；平台 net=Σ商户=70000（direct 出票 30000 无 merchant 归属，
        // 不计入平台，消除"平台≥Σ商户"漂移）。direct 出票本无订单收入，不计入平台口径是正确语义。
        assertThat(repository.upserts.get(netKey("merchant_a"))).isEqualByComparingTo("70000");
        assertThat(repository.upserts.get(netKey("platform"))).isEqualByComparingTo("70000");
    }

    @Test
    void netDeductsRefundOnTheBusinessDayTheRefundOccurred() {
        // 退款冲减当日：聚合按经营日（BusinessDayResolver 窗口）取数，退款随当日 ticketRefund/barRefund 入 net。
        CapturingAnalyticsRepository sameDay = new CapturingAnalyticsRepository();
        sameDay.ticketRevenue = new BigDecimal("50000");
        sameDay.ticketRefund = new BigDecimal("8000");
        // 2d-3：售票分摊到商户，平台=Σ商户。
        sameDay.ticketAllocated = orderedMap("merchant_a", "50000");
        sameDay.ticketRefundByMerchant = orderedMap("merchant_a", "8000");
        sameDay.barRevenue = orderedMap("merchant_a", "0");
        sameDay.barRefund = Map.of();
        new DashboardAggregationJob(sameDay).refreshSnapshots(BUSINESS_DATE);
        // 当日含退款：50000 − 8000 = 42000。
        assertThat(sameDay.upserts.get(netKey("platform"))).isEqualByComparingTo("42000");

        CapturingAnalyticsRepository noRefund = new CapturingAnalyticsRepository();
        noRefund.ticketRevenue = new BigDecimal("50000");
        noRefund.ticketRefund = BigDecimal.ZERO;
        // 2d-3：售票分摊到商户，平台=Σ商户。
        noRefund.ticketAllocated = orderedMap("merchant_a", "50000");
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
        // 2d-3：售票分摊到商户，平台=Σ商户=185000。
        repository.ticketAllocated = orderedMap("merchant_a", "70000", "merchant_b", "30000");
        repository.ticketRefundByMerchant = orderedMap("merchant_a", "7000", "merchant_b", "3000");
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
        Map<String, BigDecimal> thirdpartyRevenue = Map.of();
        String thirdpartyWindow;

        @Override
        public Map<String, BigDecimal> sumThirdpartyRevenueCentsByMerchant(LocalDate businessDate, String windowDomain) {
            this.thirdpartyWindow = windowDomain;
            return thirdpartyRevenue;
        }

        // V220 招商漏斗 fake:默认空漏斗 + 零营业额(比率分母 0 → ETL 不产出比率行)。
        public AnalyticsRecords.LeasingFunnel leasingFunnel(LocalDate businessDate, String windowDomain) {
            return new AnalyticsRecords.LeasingFunnel(0, 0, 0);
        }
        public BigDecimal sumThirdpartyTurnoverCents(LocalDate businessDate, String windowDomain) {
            return BigDecimal.ZERO;
        }

        // V221 归因口径规则:fake 返回空/ null → 触发聚合的 gross+net_deduct 兜底,等价于改造前硬编码口径(net 值不变)。
        public java.util.List<AnalyticsRecords.MerchantRevenueAttributionRule> listRevenueAttributionRules() {
            return java.util.List.of();
        }
        public AnalyticsRecords.MerchantRevenueAttributionRule findDefaultAttributionRule(String revenueSource) {
            return null;
        }
        public AnalyticsRecords.MerchantRevenueAttributionRule upsertRevenueAttributionRule(
                AnalyticsRecords.MerchantRevenueAttributionRule rule, String updatedBy, java.time.Instant now) {
            return rule;
        }

        Map<String, Map<String, BigDecimal>> arAging = Map.of();
        BigDecimal arCollected = BigDecimal.ZERO;
        BigDecimal arReceivableMtd = BigDecimal.ZERO;

        public Map<String, Map<String, BigDecimal>> arOverdueAgingByMerchant(LocalDate asOf) {
            return arAging;
        }

        public BigDecimal arCollectedCentsBetween(java.time.Instant start, java.time.Instant end) {
            return arCollected;
        }

        public BigDecimal arReceivableDueCentsBetween(LocalDate monthStart, LocalDate endExclusive) {
            return arReceivableMtd;
        }

        public java.util.List<AnalyticsRecords.ArDebtorRow> listOverdueDebtors(LocalDate asOf, String bucket, int limit, int offset,
                java.util.Collection<String> projectIds) {
            return java.util.List.of();
        }

        AnalyticsRecords.OccupancyAreas occupancy = new AnalyticsRecords.OccupancyAreas(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        long expiringContracts = 0;

        public AnalyticsRecords.OccupancyAreas occupancyAreas(LocalDate asOf) {
            return occupancy;
        }

        Map<String, AnalyticsRecords.ParkingDayFacts> parkingFacts = Map.of();

        public Map<String, AnalyticsRecords.ParkingDayFacts> parkingDayFacts(LocalDate businessDate) {
            return parkingFacts;
        }

        public long countExpiringContracts(LocalDate asOf, int windowDays) {
            return expiringContracts;
        }

        public java.util.List<AnalyticsRecords.VacantSpaceRow> listVacantSpaces(LocalDate asOf, int limit, int offset,
                java.util.Collection<String> projectIds) {
            return java.util.List.of();
        }

        public java.util.List<AnalyticsRecords.ExpiringContractRow> listExpiringContracts(LocalDate asOf, int windowDays, int limit, int offset,
                java.util.Collection<String> projectIds) {
            return java.util.List.of();
        }

        LocalDate rolledUpMonthFirst;
        LocalDate rolledUpUpTo;
        java.util.List<String> rolledUpSumKeys;
        java.util.List<String> rolledUpLastKeys;

        public void rollupMonthlySnapshots(LocalDate monthFirst, LocalDate upTo,
                java.util.List<String> sumKeys, java.util.List<String> lastKeys, java.time.Instant now) {
            this.rolledUpMonthFirst = monthFirst;
            this.rolledUpUpTo = upTo;
            this.rolledUpSumKeys = sumKeys;
            this.rolledUpLastKeys = lastKeys;
        }

        public Map<String, BigDecimal> readMonthlySnapshots(LocalDate monthFirst) {
            return Map.of();
        }

        AnalyticsRecords.WorkOrderSla workOrderSla = new AnalyticsRecords.WorkOrderSla(null, null, 0, 0, 0);
        AnalyticsRecords.WorkOrderSla merchantWorkOrderSla = new AnalyticsRecords.WorkOrderSla(null, null, 0, 0, 0);
        Map<String, BigDecimal> energyByType = Map.of();
        long abnormalReadings = 0;

        public AnalyticsRecords.WorkOrderSla workOrderSla(java.time.Instant start, java.time.Instant end, java.time.Instant asOf) {
            return workOrderSla;
        }

        public AnalyticsRecords.WorkOrderSla merchantWorkOrderSla(java.time.Instant start, java.time.Instant end, java.time.Instant asOf) {
            return merchantWorkOrderSla;
        }

        public java.util.List<AnalyticsRecords.OverdueWorkOrderRow> listOverdueMerchantWorkOrders(
                java.time.Instant start, java.time.Instant end, java.time.Instant asOf, int limit, int offset,
                java.util.Collection<String> projectIds) {
            return java.util.List.of();
        }

        public java.util.List<AnalyticsRecords.OverdueWorkOrderRow> listOverdueWorkOrders(
                java.time.Instant start, java.time.Instant end, java.time.Instant asOf, int limit, int offset,
                java.util.Collection<String> projectIds) {
            return java.util.List.of();
        }

        public Map<String, BigDecimal> energyConsumptionByType(java.time.Instant start, java.time.Instant end) {
            return energyByType;
        }

        public long countAbnormalReadings(java.time.Instant start, java.time.Instant end) {
            return abnormalReadings;
        }

        public java.util.List<AnalyticsRecords.AbnormalReadingRow> listAbnormalReadings(
                java.time.Instant start, java.time.Instant end, int limit, int offset,
                java.util.Collection<String> projectIds) {
            return java.util.List.of();
        }

        public java.util.List<AnalyticsRecords.BusinessDayCutoffRecord> listBusinessDayCutoffs() {
            return java.util.List.of();
        }

        public AnalyticsRecords.BusinessDayCutoffRecord upsertBusinessDayCutoff(String domain, int cutoffHour, String updatedBy, java.time.Instant now) {
            return new AnalyticsRecords.BusinessDayCutoffRecord(domain, cutoffHour, now, updatedBy);
        }


        Map<String, Map<String, BigDecimal>> barRevenueByWindow;
        Map<String, Map<String, BigDecimal>> barRefundByWindow;

        private final Map<String, BigDecimal> upserts = new LinkedHashMap<>();
        private BigDecimal ticketRevenue = BigDecimal.ZERO;
        private BigDecimal ticketRefund = BigDecimal.ZERO;
        private Map<String, BigDecimal> barRevenue = Map.of();
        private Map<String, BigDecimal> barRefund = Map.of();
        private Map<String, BigDecimal> ticketAllocated = Map.of();
        private Map<String, BigDecimal> ticketRefundByMerchant = Map.of();
        private Map<String, BigDecimal> contractRent = Map.of();
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
        public BigDecimal sumTicketRevenueCents(LocalDate businessDate, String windowDomain) {
            return ticketRevenue;
        }

        @Override
        public BigDecimal sumTicketRefundCents(LocalDate businessDate, String windowDomain) {
            return ticketRefund;
        }

        @Override
        public Map<String, BigDecimal> sumBarRevenueCentsByMerchant(LocalDate businessDate, String windowDomain) {
            if (barRevenueByWindow != null) return barRevenueByWindow.getOrDefault(windowDomain, Map.of());
            return barRevenue;
        }

        @Override
        public Map<String, BigDecimal> sumBarRefundCentsByMerchant(LocalDate businessDate, String windowDomain) {
            if (barRefundByWindow != null) return barRefundByWindow.getOrDefault(windowDomain, Map.of());
            return barRefund;
        }

        @Override
        public Map<String, BigDecimal> sumTicketAllocatedRevenueByMerchant(LocalDate businessDate, String windowDomain) {
            return ticketAllocated;
        }

        @Override
        public Map<String, BigDecimal> sumTicketRefundByMerchant(LocalDate businessDate, String windowDomain) {
            return ticketRefundByMerchant;
        }

        @Override
        public Map<String, BigDecimal> sumContractRentReceivableByMerchant(LocalDate businessDate, String windowDomain) {
            return contractRent;
        }

        // ── 其余聚合源：本测试不关心，返回中性值 ──
        @Override
        public Map<String, BigDecimal> sumTicketBarShareCentsByMerchant(LocalDate businessDate, String windowDomain) {
            return Map.of();
        }

        @Override
        public Map<String, BigDecimal> sumTicketAmusementShareCentsByMerchant(LocalDate businessDate, String windowDomain) {
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
        public Map<Integer, Long> hourlyTrafficCounts(LocalDate businessDate, Optional<String> merchantScope) {
            return Map.of();
        }

        private OperatingParamsRecord operatingParams; // null = 未配置面积

        @Override
        public Optional<OperatingParamsRecord> findOperatingParams(String projectId) {
            return Optional.ofNullable(operatingParams);
        }

        @Override
        public List<OperatingParamsRecord> listOperatingParams() {
            return operatingParams == null ? List.of() : List.of(operatingParams);
        }

        @Override
        public OperatingParamsRecord upsertOperatingParams(OperatingParamsRecord record) {
            this.operatingParams = record;
            return record;
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

        boolean withinHoliday;

        public java.util.List<AnalyticsRecords.HolidayPeriod> listHolidayPeriods(Integer year) {
            return java.util.List.of();
        }

        public AnalyticsRecords.HolidayPeriod upsertHolidayPeriod(String holidayName, int holidayYear,
                LocalDate startDate, LocalDate endDate, String source, java.time.Instant now) {
            return new AnalyticsRecords.HolidayPeriod(holidayName, holidayYear, startDate, endDate, source, now);
        }

        public boolean isWithinHoliday(LocalDate businessDate) {
            return withinHoliday;
        }

        public Map<String, BigDecimal> aggregateRange(LocalDate start, LocalDate end,
                java.util.List<String> sumKeys, java.util.List<String> lastKeys) {
            return Map.of();
        }

        LocalDate refreshedMerchantSummariesFor;

        public void refreshMerchantSummaries(LocalDate businessDate, java.time.Instant now) {
            this.refreshedMerchantSummariesFor = businessDate;
        }

        Map<LocalDate, Map<String, BigDecimal>> snapshotsByDate = Map.of();
        AnalyticsRecords.AnomalyConfig anomalyConfig;
        final java.util.List<AnalyticsRecords.MetricDefinitionSyncRow> syncedDefinitions = new java.util.ArrayList<>();
        final Map<String, String> insertedAnomalies = new LinkedHashMap<>();

        @Override
        public Map<String, BigDecimal> readMetricSnapshots(LocalDate businessDate, String scope) {
            return snapshotsByDate.getOrDefault(businessDate, Map.of());
        }

        public void syncMetricDefinitions(java.util.List<AnalyticsRecords.MetricDefinitionSyncRow> rows, java.time.Instant now) {
            syncedDefinitions.addAll(rows);
        }

        public void insertMetricAnomaly(String id, String metricKey, String domain, String riskLevel,
                BigDecimal observedValue, BigDecimal expectedValue, String summary, java.time.Instant detectedAt) {
            insertedAnomalies.put(metricKey, riskLevel);
        }

        public AnalyticsRecords.AnomalyConfig readAnomalyConfig() {
            return anomalyConfig;
        }

        public java.util.List<AnalyticsRecords.NotificationWindow> listNotificationWindows() {
            return java.util.List.of();
        }

        public AnalyticsRecords.NotificationWindow upsertNotificationWindow(String windowKey, int days,
                String description, String updatedBy, java.time.Instant now) {
            return new AnalyticsRecords.NotificationWindow(windowKey, days, description, updatedBy, now);
        }

        public AnalyticsRecords.AiThresholds readAiThresholds() {
            return new AnalyticsRecords.AiThresholds(
                    new BigDecimal("0.60"), new BigDecimal("0.20"), null, null);
        }

        public AnalyticsRecords.AiThresholds upsertAiThresholds(BigDecimal minConfidence,
                BigDecimal benchmarkDeviation, String updatedBy, java.time.Instant now) {
            return new AnalyticsRecords.AiThresholds(minConfidence, benchmarkDeviation, updatedBy, now);
        }

        public AnalyticsRecords.AnomalyConfig upsertAnomalyConfig(BigDecimal surgePct, BigDecimal highPct,
                boolean enabled, String updatedBy, java.time.Instant now) {
            return new AnalyticsRecords.AnomalyConfig(surgePct, highPct, enabled, updatedBy, now);
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
        public java.util.Optional<MetricAnomalyRecord> findMetricAnomalyById(String id) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<MetricAnomalyRecord> findMetricAnomalyByIdForUpdate(String id) {
            return java.util.Optional.empty();
        }

        @Override
        public MetricAnomalyRecord updateMetricAnomalyDisposition(String id, String status, String note,
                String disposedBy, java.time.Instant disposedAt, String escalatedAlertId,
                java.time.Instant resolvedAt) {
            throw new UnsupportedOperationException("not used in aggregation job tests");
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
        public AnalyticsRecords.BusinessReviewLockRecord saveReviewLock(AnalyticsRecords.BusinessReviewLockRecord lock) {
            return lock;
        }

        @Override
        public List<AnalyticsRecords.BusinessReviewLockRecord> listReviewLocks(String projectScope) {
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
