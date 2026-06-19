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

    List<MerchantSummaryRecord> listAnalyticsMerchants();

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

    // ETL 聚合源（从真实业务表读取，按 segment 维度返回 segmentId -> 聚合值；segmentId=null 行表示平台口径）。

    /** Σ ticket_orders.amount_cent（pay_status='paid'）——售票收入；ticket_orders 无 merchant_id，仅平台口径。 */
    BigDecimal sumTicketRevenueCents(LocalDate businessDate);

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

    /** count amusement_verifications(verification_result='success')——游乐核销人次；无 merchant_id，仅平台口径。 */
    BigDecimal countAmusementVerificationSuccess(LocalDate businessDate);

    /** count ticket_verifications(status='success')——核销数；返回 merchantId(可 null) -> 计数。 */
    Map<String, BigDecimal> countTicketVerificationSuccessByMerchant(LocalDate businessDate);

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
}
