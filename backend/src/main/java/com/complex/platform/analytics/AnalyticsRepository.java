package com.complex.platform.analytics;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AnalyticsRepository {

    List<AiQualityResultRecord> listAiQualityResults();

    List<AiQualityTaskRecord> listAiQualityTasks();

    AiQualityTaskRecord saveAiQualityTask(AiQualityTaskRecord task);

    AiQualityResultRecord saveAiQualityResult(AiQualityResultRecord result);

    List<CustomerIntentRecord> listCustomerIntents();

    DashboardSnapshotRecord getP0OverviewDashboard();

    List<BusinessMetricSnapshotRecord> listBusinessMetricSnapshots();

    List<MerchantHealthScoreRecord> listMerchantHealthScores();

    /**
     * 按商户 scope 列出商户健康分（analytics_merchant_health_scores.merchant_id = token scope）。
     * 商户面数据：商户只能看到本商户的健康分。默认在内存过滤，JdbcAnalyticsRepository 用 SQL 谓词下推。
     * 隔离闸口（AC-ISO-1）：提供 merchant_id 行级 scoped 读通道。
     */
    default List<MerchantHealthScoreRecord> listMerchantHealthScoresByMerchant(String merchantId) {
        if (merchantId == null) return listMerchantHealthScores();
        return listMerchantHealthScores().stream()
                .filter(s -> merchantId.equals(s.merchantId()))
                .toList();
    }

    List<MerchantSummaryRecord> listAnalyticsMerchants();

    /**
     * 按商户 scope 列出商户经营摘要（analytics_merchant_summaries.merchant_id = token scope）。
     * 商户面数据：商户只能看到本商户的经营摘要。默认在内存过滤，JdbcAnalyticsRepository 用 SQL 谓词下推。
     * 隔离闸口（AC-ISO-1）：提供 merchant_id 行级 scoped 读通道。
     */
    default List<MerchantSummaryRecord> listAnalyticsMerchantsByMerchant(String merchantId) {
        if (merchantId == null) return listAnalyticsMerchants();
        return listAnalyticsMerchants().stream()
                .filter(s -> merchantId.equals(s.merchantId()))
                .toList();
    }

    List<MetricAnomalyRecord> listMetricAnomalies();

    List<MetricDefinitionRecord> listMetricDefinitions();

    List<RiskAlertRecord> listRiskAlerts();

    RiskAlertRecord saveRiskAlert(RiskAlertRecord alert);

    Optional<RiskAlertRecord> findRiskAlertById(String id);

    RiskAlertRecord updateRiskAlert(RiskAlertRecord alert);

    List<RiskRuleRecord> listRiskRules();

    List<RobotMetricRecord> listRobotMetrics();

    List<StaffServiceScoreRecord> listStaffServiceScores();

    // --- T12 看板真实聚合 / 行级隔离 ---

    /**
     * 读取某经营日、某 scope（null=平台口径，非空=商户口径）下的指标快照。
     * 返回 metric_key -> actual_value。商户口径只返回 segment_type='merchant' 且 segment_id=scope 的行；
     * 平台口径只返回 segment_type='platform' 的行。
     */
    Map<String, BigDecimal> readMetricSnapshots(LocalDate businessDate, String scope);

    /** 最近一个有 ETL 快照的经营日（无则返回 empty）。 */
    Optional<LocalDate> latestEtlBusinessDate(String scope);

    /** ETL upsert 一个指标快照（按 V070 唯一键幂等）。 */
    void upsertMetricSnapshot(MetricSnapshotKey key, BigDecimal actualValue, String projectionMethod, Instant createdAt);

    // T4.2 坪效：综合体经营参数（complex_operating_params，V160）——可经营面积主数据，坪效 revenue.per_sqm 据此派生。

    /** 读某综合体经营参数（含可经营面积）；无配置返回 empty（坪效该日诚实空缺）。 */
    Optional<OperatingParamsRecord> findOperatingParams(String projectId);

    /** 列出全部综合体经营参数（后台配置页）。 */
    List<OperatingParamsRecord> listOperatingParams();

    /** 按 project_id upsert 经营参数（ON CONFLICT DO UPDATE 幂等）。 */
    OperatingParamsRecord upsertOperatingParams(OperatingParamsRecord record);

    // ETL 聚合源（从真实业务表读取，按 segment 维度返回 segmentId -> 聚合值；segmentId=null 行表示平台口径）。

    /** Σ ticket_orders.amount_cent（pay_status='paid'）——售票收入；ticket_orders 无 merchant_id，仅平台口径。 */
    BigDecimal sumTicketRevenueCents(LocalDate businessDate);

    /**
     * RC1②：Σ ticket_refunds.refund_amount_cent——自营售票退款（按 created_at 落经营日窗口，与 T7 日结口径一致）；
     * ticket_refunds 无 merchant_id，仅平台口径。用于 revenue.net 派生时冲减自营售票收入。
     */
    BigDecimal sumTicketRefundCents(LocalDate businessDate);

    /** Σ bar_bill_settlements.settlement_amount_cent——酒吧收入；返回 merchantId(可 null) -> 金额。 */
    Map<String, BigDecimal> sumBarRevenueCentsByMerchant(LocalDate businessDate);

    /**
     * 待确认#2：Σ bar_bill_adjustments.amount_cent（adjustment_type='refund'）——酒吧结账后退款；
     * 按 refunded_at 落经营日窗口，返回 merchantId(可 null) -> 退款金额。退款冲减 net（revenue.bar 不被改写，
     * 单列 revenue.bar_refund 展示并由净收入派生时扣减）；免单 waiver 不计入（已体现在更低结算实收）。
     */
    Map<String, BigDecimal> sumBarRefundCentsByMerchant(LocalDate businessDate);

    /**
     * 待确认项#1：通票售票收入的"业态拆分展示口径"——按 issued_entitlements.allocated_revenue_cent
     * 汇总当日发放的、业态映射到酒吧的权益分配收入；返回 merchantId(可 null) -> 金额。
     * 仅作展示（revenue.ticket_bar_share），不并入 revenue.net：订单实付全额已在 revenue.ticket 计入，
     * revenue.bar 仍仅取 bar_bill_settlements 现场消费实收，二者口径区分，避免重复计净收入。
     */
    Map<String, BigDecimal> sumTicketBarShareCentsByMerchant(LocalDate businessDate);

    /**
     * 待确认项#1：通票售票收入业态拆分——映射到游乐的权益分配收入；返回 merchantId(可 null) -> 金额。
     * 仅作展示（revenue.ticket_amusement_share），不并入 revenue.net（同上理由）。
     */
    Map<String, BigDecimal> sumTicketAmusementShareCentsByMerchant(LocalDate businessDate);

    /**
     * 清单#4：本商户售票分量——Σ issued_entitlements.allocated_revenue_cent 按 merchant_id 汇总（按 created_at 落经营日窗口）。
     * 与 revenue.ticket（ticket_orders 实付全额，平台口径）口径互补：allocateRevenue 保证同一订单 Σallocated==订单实付，
     * 故 Σmerchant(allocated) 等于 Σ有发券订单的实付；direct 出票/无 allocated 的权益（allocated_revenue_cent IS NULL）不计，
     * 保守不归任何商户（避免凭空摊派）。返回 merchantId(可 null) -> 金额，并入商户行级 revenue.net 的售票分量。
     */
    Map<String, BigDecimal> sumTicketAllocatedRevenueByMerchant(LocalDate businessDate);

    /**
     * 清单#4：本商户售票退款分量——按 ticket_refunds（按 created_at 落经营日窗口，与平台 sumTicketRefundCents 同口径同population）
     * 关联订单各权益的 allocated_revenue_cent 占比，把每笔退款分摊到 merchant_id。
     * 口径：单笔退款 → 该订单 Σallocated 为基数 → 各 merchant 的 allocated 占比 × 退款额（最大余数补尾，Σ分摊==退款额）。
     * 订单无 allocated 权益（基数为 0）的退款保守不归任何商户（与售票分量口径一致，避免摊派）。
     * 返回 merchantId(可 null) -> 退款分摊额，用于商户行级 revenue.net 冲减。
     */
    Map<String, BigDecimal> sumTicketRefundByMerchant(LocalDate businessDate);

    /** count amusement_verifications(verification_result='success')——游乐核销人次；无 merchant_id，仅平台口径。 */
    BigDecimal countAmusementVerificationSuccess(LocalDate businessDate);

    /** count ticket_verifications(status='success')——核销数；返回 merchantId(可 null) -> 计数。 */
    Map<String, BigDecimal> countTicketVerificationSuccessByMerchant(LocalDate businessDate);

    /**
     * T4.3 分时段客流：成功核销按经营日窗口内 created_at 的小时（0~23）分桶计数。
     * 平台口径（scope 空）合并 ticket_verifications + amusement_verifications；
     * 商户口径仅本商户 ticket_verifications（amusement_verifications 无 merchant_id，不并入）。
     * 返回 hour(0~23) -> 计数（稀疏，缺的小时由上层补 0）。
     */
    Map<Integer, Long> hourlyTrafficCounts(LocalDate businessDate, Optional<String> merchantScope);

    /**
     * 清单#18：券发放数——count issued_entitlements(created_at 落经营日窗口)，作为券核销率分母。
     * 与核销数 countTicketVerificationSuccessByMerchant 同 merchant 维度，便于派生 redemption_rate=核销数/发放数。
     * 返回 merchantId(可 null) -> 计数。
     */
    Map<String, BigDecimal> countTicketIssuedByMerchant(LocalDate businessDate);

    /** count customers(created_at 当日)——新增会员；无 merchant_id，仅平台口径。 */
    BigDecimal countNewMembers(LocalDate businessDate);

    /** Σ inventory_stock_balances.quantity——库存余额（截至当日的存量快照）；返回 merchantId(可 null) -> 数量。 */
    Map<String, BigDecimal> sumInventoryBalanceByMerchant();

    /**
     * 缺货预警计数（按 SKU 绝对值阈值，财务确认口径）：count(SKU 现存量 &lt; 该 SKU 的安全库存阈值)。
     * 阈值取 inventory_items.min_quantity（绝对值/SKU 可配），阈值 ≤ 0 视为不预警。返回 merchantId(可 null) -> 数量。
     */
    Map<String, BigDecimal> countLowStockByMerchant();

    /** count(supervisor_review_decisions pending) + count(analytics_risk_alerts open)——待复核；无 merchant_id，仅平台口径。 */
    BigDecimal countPendingReview();

    // --- P2 集团/跨项目汇总（R6-D13）---

    /**
     * 某经营日有 ETL 商户口径快照（segment_type='merchant', projection_method='etl_aggregate'）的去重商户数。
     * 作为集团总览"商户数"指标的真实来源（当日活跃于看板聚合的商户），无快照返回 0，不伪造。
     */
    long countMerchantsWithEtlSnapshot(LocalDate businessDate);

    // --- ⑤ 集团多项目 ETL 维度（segment_type='project' 按 project_id 分组聚合）---

    /**
     * 列出某经营日有 ETL project 口径快照（segment_type='project', projection_method='etl_aggregate'）的去重 project_id。
     * 集团总览据此产出 per-project rollup 行；单综合体 → 单 project_id，多综合体 → 多行真实 rollup。
     * 无 project 口径快照返回空（调用方回退到平台口径单行，向后兼容），不伪造。
     */
    default List<String> listProjectIdsWithEtlSnapshot(LocalDate businessDate) {
        return List.of();
    }

    /**
     * 读取某经营日、某 project_id 的 ETL project 口径指标快照（segment_type='project', segment_id=projectId）。
     * 返回 metric_key -> actual_value。与 {@link #readMetricSnapshots} 同口径，仅 segment 维度不同。
     */
    default Map<String, BigDecimal> readProjectMetricSnapshots(LocalDate businessDate, String projectId) {
        return Map.of();
    }

    /**
     * 某经营日、某 project_id 下有 ETL 商户口径快照的去重商户数（segment_type='merchant'，按 project 归属）。
     * 单综合体默认全部商户归该综合体 → 等于 countMerchantsWithEtlSnapshot；多综合体按 project 归属拆分。
     */
    default long countMerchantsWithEtlSnapshotByProject(LocalDate businessDate, String projectId) {
        return countMerchantsWithEtlSnapshot(businessDate);
    }
}
