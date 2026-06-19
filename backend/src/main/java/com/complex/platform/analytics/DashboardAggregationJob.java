package com.complex.platform.analytics;

import com.complex.platform.analytics.AnalyticsRecords.MetricSnapshotKey;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * T12 经营看板 ETL（实现 B：@Scheduled 刷新快照）。
 *
 * <p>聚合逻辑放在可直接调用的 {@link #refreshSnapshots(LocalDate)}，@Scheduled 仅薄包装调用它——
 * 便于单测直接调方法对拍真实业务表，不依赖定时触发；定时任务通过 @ConditionalOnProperty 默认关闭，
 * 测试期不会乱跑（需显式开 analytics.dashboard-etl.scheduled.enabled=true 才注册定时刷新）。
 *
 * <p>按 r5-aggregation-metrics-draft §2 七类指标从真实业务表聚合，upsert 到
 * analytics_business_metric_snapshots（metric_key + period_type + business_date + segment_type + segment_id）。
 * 平台口径 segment_type='platform'、segment_id=NULL；商户口径 segment_type='merchant'、segment_id=merchant_id。
 * 源表无 merchant_id 的指标只产出平台口径行，商户登录读不到（按"不伪造"返回 null）。
 */
@Component
public class DashboardAggregationJob {

    // 七类 metric_code（与聚合草案 §2 对齐）。
    static final String REVENUE_TICKET = "revenue.ticket";      // 收入
    static final String REVENUE_BAR = "revenue.bar";            // 收入
    static final String REVENUE_BAR_REFUND = "revenue.bar_refund"; // 收入·酒吧退款（冲减 net，待确认#2）
    // 待确认项#1：通票售票收入的业态拆分展示口径（不并入 net，仅展示）。
    static final String REVENUE_TICKET_BAR_SHARE = "revenue.ticket_bar_share";             // 收入·通票酒吧分摊
    static final String REVENUE_TICKET_AMUSEMENT_SHARE = "revenue.ticket_amusement_share"; // 收入·通票游乐分摊
    static final String TRAFFIC_AMUSEMENT = "traffic.amusement"; // 客流
    static final String VERIFY_DONE = "verify.done";            // 核销
    static final String MEMBER_NEW = "member.new";              // 会员
    static final String SUPPLY_BALANCE = "supply.balance";      // 供应链
    static final String SUPPLY_LOW_STOCK = "supply.low_stock";  // 供应链·缺货预警
    static final String RISK_PENDING_REVIEW = "risk.pending_review"; // 风险

    static final String SEGMENT_PLATFORM = "platform";
    static final String SEGMENT_MERCHANT = "merchant";
    static final String PERIOD_DAILY = "daily";
    static final String PROJECTION_ETL = "etl_aggregate";

    private final AnalyticsRepository repository;
    private final Clock clock;

    @Autowired
    public DashboardAggregationJob(AnalyticsRepository repository) {
        this(repository, Clock.systemUTC());
    }

    DashboardAggregationJob(AnalyticsRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** @Scheduled 薄包装：默认关闭，仅在 analytics.dashboard-etl.scheduled.enabled=true 时注册。 */
    @ConditionalOnProperty(name = "analytics.dashboard-etl.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${analytics.dashboard-etl.scheduled.fixed-delay-ms:300000}")
    public void scheduledRefresh() {
        refreshSnapshots(today());
    }

    /** 直接可调用的聚合入口：聚合 businessDate 当日七类指标并 upsert 快照。 */
    public void refreshSnapshots(LocalDate businessDate) {
        Instant now = Instant.now(clock);

        // 收入·售票：ticket_orders 无 merchant_id → 仅平台口径。
        upsertPlatform(REVENUE_TICKET, "finance", businessDate, repository.sumTicketRevenueCents(businessDate), now);

        // 收入·酒吧：bar_bill_settlements 有 merchant_id → 平台 + 商户口径。
        // 硬约束：revenue.bar 仅取现场消费实收（bar_bill_settlements），不含通票售票分摊，保证净收入口径不重复。
        upsertByMerchant(REVENUE_BAR, "bar", businessDate, repository.sumBarRevenueCentsByMerchant(businessDate), now);

        // 收入·酒吧退款（待确认#2）：bar_bill_adjustments(adjustment_type='refund') 按 refunded_at 落经营日窗口，
        // 单列 revenue.bar_refund（不改写 revenue.bar 实收口径），net 派生时扣减；免单 waiver 不计入（已在更低结算实收体现）。
        upsertByMerchant(REVENUE_BAR_REFUND, "bar", businessDate, repository.sumBarRefundCentsByMerchant(businessDate), now);

        // 待确认项#1：通票售票收入业态拆分展示（issued_entitlements.allocated_revenue_cent 按业态/商户汇总）。
        // 仅作展示口径，不并入 revenue.net：订单实付全额已在 revenue.ticket 计入，这里只是把"售票确认收入"
        // 按酒吧/游乐业态拆出展示行，与 revenue.bar（消费实收）口径区分，避免重复计净收入。
        upsertByMerchant(REVENUE_TICKET_BAR_SHARE, "ticketing", businessDate,
                repository.sumTicketBarShareCentsByMerchant(businessDate), now);
        upsertByMerchant(REVENUE_TICKET_AMUSEMENT_SHARE, "ticketing", businessDate,
                repository.sumTicketAmusementShareCentsByMerchant(businessDate), now);

        // 客流·游乐：amusement_verifications 无 merchant_id → 仅平台口径。
        upsertPlatform(TRAFFIC_AMUSEMENT, "amusement", businessDate, repository.countAmusementVerificationSuccess(businessDate), now);

        // 核销：ticket_verifications 有 merchant_id → 平台 + 商户口径。
        upsertByMerchant(VERIFY_DONE, "ticketing", businessDate, repository.countTicketVerificationSuccessByMerchant(businessDate), now);

        // 会员·新增：customers 无 merchant_id → 仅平台口径。
        upsertPlatform(MEMBER_NEW, "customer", businessDate, repository.countNewMembers(businessDate), now);

        // 供应链·库存余额：inventory_stock_balances 有 merchant_id → 平台 + 商户口径（存量快照，与经营日无关，落当日）。
        upsertByMerchant(SUPPLY_BALANCE, "inventory", businessDate, repository.sumInventoryBalanceByMerchant(), now);

        // 供应链·缺货预警：按 SKU 绝对值阈值（inventory_items.min_quantity）count(可用量<阈值)，阈值≤0 不预警（财务确认口径）。
        upsertByMerchant(SUPPLY_LOW_STOCK, "inventory", businessDate, repository.countLowStockByMerchant(), now);

        // 风险·待复核：supervisor_review_decisions / analytics_risk_alerts 无 merchant_id → 仅平台口径。
        upsertPlatform(RISK_PENDING_REVIEW, "governance", businessDate, repository.countPendingReview(), now);
    }

    private void upsertPlatform(String metricKey, String domain, LocalDate businessDate, BigDecimal value, Instant now) {
        repository.upsertMetricSnapshot(
                new MetricSnapshotKey(metricKey, domain, PERIOD_DAILY, businessDate, SEGMENT_PLATFORM, null),
                value == null ? BigDecimal.ZERO : value,
                PROJECTION_ETL,
                now
        );
    }

    private void upsertByMerchant(
            String metricKey,
            String domain,
            LocalDate businessDate,
            Map<String, BigDecimal> byMerchant,
            Instant now
    ) {
        BigDecimal platformTotal = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : byMerchant.entrySet()) {
            BigDecimal value = entry.getValue() == null ? BigDecimal.ZERO : entry.getValue();
            platformTotal = platformTotal.add(value);
            String merchantId = entry.getKey();
            if (merchantId != null && !merchantId.isBlank()) {
                repository.upsertMetricSnapshot(
                        new MetricSnapshotKey(metricKey, domain, PERIOD_DAILY, businessDate, SEGMENT_MERCHANT, merchantId),
                        value,
                        PROJECTION_ETL,
                        now
                );
            }
        }
        // 平台口径 = 全商户合计（含 merchant_id 为 NULL 的行也并入平台总量）。
        repository.upsertMetricSnapshot(
                new MetricSnapshotKey(metricKey, domain, PERIOD_DAILY, businessDate, SEGMENT_PLATFORM, null),
                platformTotal,
                PROJECTION_ETL,
                now
        );
    }

    private LocalDate today() {
        return LocalDate.ofInstant(Instant.now(clock), ZoneOffset.UTC);
    }
}
