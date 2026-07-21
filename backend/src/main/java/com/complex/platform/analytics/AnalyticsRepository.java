package com.complex.platform.analytics;

import com.complex.platform.analytics.AnalyticsRecords.AiQualityResultRecord;
import com.complex.platform.analytics.AnalyticsRecords.AiQualityTaskRecord;
import com.complex.platform.analytics.AnalyticsRecords.BusinessMetricSnapshotRecord;
import com.complex.platform.analytics.AnalyticsRecords.BusinessReviewLockRecord;
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

    // BI S0(2026-07-02 拍板):收入类聚合按「窗口域」取经营日窗——展示键用各业务域自己的切分
    // (bar=06:00 等,V201 可配),跨域相加的 revenue.net 分量一律传 "platform"(对齐基准 04:00,防混窗)。


    /** project_id -> business_entity_id（projects ⋈ merchants），供 group-overview groupBy=entity 派生。 */
    default Map<String, String> projectBusinessEntityMap() {
        return Map.of();
    }

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

    java.util.Optional<MetricAnomalyRecord> findMetricAnomalyById(String id);

    /** 处置事务内加行锁读(SELECT ... FOR UPDATE),串行化同一异常的并发处置,避免升级重复建告警产生孤儿。 */
    java.util.Optional<MetricAnomalyRecord> findMetricAnomalyByIdForUpdate(String id);

    MetricAnomalyRecord updateMetricAnomalyDisposition(String id, String status, String note,
            String disposedBy, java.time.Instant disposedAt, String escalatedAlertId,
            java.time.Instant resolvedAt);

    List<MetricDefinitionRecord> listMetricDefinitions();

    List<RiskAlertRecord> listRiskAlerts();

    RiskAlertRecord saveRiskAlert(RiskAlertRecord alert);

    /** V216 经营复盘锁定：写入一条锁定凭据（幂等键唯一）。 */
    BusinessReviewLockRecord saveReviewLock(BusinessReviewLockRecord lock);

    /** V216 经营复盘锁定：按 token 项目 scope 读回已锁定（projectScope=null 见全量，非空仅本项目）。 */
    List<BusinessReviewLockRecord> listReviewLocks(String projectScope);

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
    /**
     * S3:出租面积盘(as-of 时点)。分母口径(2026-07-02 拍板):商铺+摊位(shop/booth)且非人工不可租;
     * 车位(parking)单列不与铺位混算;容器类(楼栋/楼层/区块)与公共区域面积重叠/不出租,不进分母。
     * 已租=有生效合同覆盖当日;已签待交=pending_effective 绑定(不算已租,单列)。
     */
    AnalyticsRecords.OccupancyAreas occupancyAreas(LocalDate asOf);

    /** S3:90 天内到期的生效合同数(到期预警 KPI)。 */
    long countExpiringContracts(LocalDate asOf, int windowDays);

    /** S3 钻取:未出租的铺位明细(shop/booth,含推导租赁态,实时与快照同过滤)。 */
    java.util.List<AnalyticsRecords.VacantSpaceRow> listVacantSpaces(LocalDate asOf, int limit, int offset);

    /** H1:列节假日区间(按年份过滤;year=null 全部,按年份+起始日排序)。 */
    java.util.List<AnalyticsRecords.HolidayPeriod> listHolidayPeriods(Integer year);

    /** H1:录入/修正节假日区间(同名同年唯一,幂等覆盖;source=seed/fetched/manual)。 */
    AnalyticsRecords.HolidayPeriod upsertHolidayPeriod(String holidayName, int holidayYear,
            LocalDate startDate, LocalDate endDate, String source, java.time.Instant now);

    /** H1:某经营日是否落在任一节假日区间内(异常检测免报用——节假日暴涨暴跌是正常经营)。 */
    boolean isWithinHoliday(LocalDate businessDate);

    /** H1:区间聚合(节假日对比用):sumKeys=区间累计,lastKeys=区间末时点;返回 metric_key -> value。 */
    Map<String, BigDecimal> aggregateRange(LocalDate start, LocalDate end,
            java.util.List<String> sumKeys, java.util.List<String> lastKeys);

    /** M4:商户经营汇总真管道——从当日商户维快照(净收入/核销)+风险告警+最新账单状态聚合,
     * 每商户一行最新态(V017 演示种子已清)。口径:收入与看板同源;单量=当日核销;风险=open告警数。 */
    void refreshMerchantSummaries(LocalDate businessDate, java.time.Instant now);

    /** M3:代码指标目录同步进 analytics_metric_definitions(单一事实源投影;异常表 FK 前置)。 */
    void syncMetricDefinitions(java.util.List<AnalyticsRecords.MetricDefinitionSyncRow> rows, java.time.Instant now);

    /** M3:写入一条指标异常(id 幂等,同日同指标只记一次)。 */
    void insertMetricAnomaly(String id, String metricKey, String domain, String riskLevel,
            BigDecimal observedValue, BigDecimal expectedValue, String summary, java.time.Instant detectedAt);

    /** M3:读异常检测阈值配置(default 行;无行返回 null=检测关闭)。 */
    AnalyticsRecords.AnomalyConfig readAnomalyConfig();

    /** V219 提醒窗天数配置:列全部键值。 */
    java.util.List<AnalyticsRecords.NotificationWindow> listNotificationWindows();

    /** V219 提醒窗天数配置:按 key 幂等 upsert,返回落库行。 */
    AnalyticsRecords.NotificationWindow upsertNotificationWindow(String windowKey, int days,
            String description, String updatedBy, java.time.Instant now);

    /** V218 AI/对标阈值(单例 default)。 */
    AnalyticsRecords.AiThresholds readAiThresholds();

    AnalyticsRecords.AiThresholds upsertAiThresholds(java.math.BigDecimal minConfidence,
            java.math.BigDecimal benchmarkDeviation, String updatedBy, java.time.Instant now);

    /** V221 商户级收入归因口径:列全部规则(default + merchant 特例)。 */
    java.util.List<AnalyticsRecords.MerchantRevenueAttributionRule> listRevenueAttributionRules();

    /** V221:按 revenue_source 取 default 口径规则(聚合读)。 */
    AnalyticsRecords.MerchantRevenueAttributionRule findDefaultAttributionRule(String revenueSource);

    /** V221:新增/更新一条归因口径规则(维护接口)。 */
    AnalyticsRecords.MerchantRevenueAttributionRule upsertRevenueAttributionRule(
            AnalyticsRecords.MerchantRevenueAttributionRule rule, String updatedBy, java.time.Instant now);

    /** 结算配置中心批0:把归因规则草案(is_draft=TRUE)复核转正为正式(FALSE)。返回更新行数(0=非草案/不存在)。
     *  默认无操作(供无需此能力的内存测试替身);真实实现见 JdbcAnalyticsRepository。 */
    default int activateAttributionRuleDraft(String id, String updatedBy, java.time.Instant now) {
        return 0;
    }

    /** 差人转正守卫(项5):读该归因规则草案的起草人 updated_by(不存在/非草案返 null);默认无操作,真实实现见 JdbcAnalyticsRepository。 */
    default String attributionRuleDraftUpdatedBy(String id) {
        return null;
    }

    /** V220 招商漏斗:线索总数/已达意向/已签约(按 created_at 落经营日窗口)。 */
    AnalyticsRecords.LeasingFunnel leasingFunnel(java.time.LocalDate businessDate, String windowDomain);

    /** V220 第三方回流营业额(integration_order_syncs 无 merchant_id → 仅平台级;租售比分母)。 */
    java.math.BigDecimal sumThirdpartyTurnoverCents(java.time.LocalDate businessDate, String windowDomain);

    /** M3:更新异常检测阈值配置(经营参数页后台可调)。 */
    AnalyticsRecords.AnomalyConfig upsertAnomalyConfig(BigDecimal surgePct, BigDecimal highPct,
            boolean enabled, String updatedBy, java.time.Instant now);

    /** M1 月度滚动汇总:把当月 daily platform 行聚成 monthly 行(business_date=月首日)。
     * 混合口径(2026-07-02 拍板④):sumKeys=流量类月累计,lastKeys=存量/比率类期末时点;
     * 白名单外的键不做月度行(月度需重算窗口的比率如 P50/超时率不伪造)。幂等(V070 唯一索引)。 */
    void rollupMonthlySnapshots(LocalDate monthFirst, LocalDate upTo,
            java.util.List<String> sumKeys, java.util.List<String> lastKeys, java.time.Instant now);

    /** M1:读某月 monthly platform 快照(metric_key -> actual_value);无行返回空 map。 */
    Map<String, BigDecimal> readMonthlySnapshots(LocalDate monthFirst);

    /** S4 工单时效:响应P50=responded_at 落窗、处理P50=completed_at 落窗且有 assigned_at、
     * 超时分母=handle_due_at 落窗、分子=其中完成晚于时限或未完成且时限已过、积压=asOf 时点未完成存量。 */
    AnalyticsRecords.WorkOrderSla workOrderSla(java.time.Instant start, java.time.Instant end, java.time.Instant asOf);

    /** S4 钻取:超时工单明细(与 wo.overdue_rate 分子同过滤同口径)。 */
    java.util.List<AnalyticsRecords.OverdueWorkOrderRow> listOverdueWorkOrders(
            java.time.Instant start, java.time.Instant end, java.time.Instant asOf, int limit, int offset);

    /** 清单#13 商户服务工单时效(同 facility 窗):流转无响应节点故 responseP50 恒为 null;
     * 处理P50=closed_at 落窗(提交→关单);超时分母=due_at 落窗且非 cancelled、
     * 分子=其中 sla_breached(V120 升级作业在办期间过限标记)或关单晚于时限或未完成且时限已过(实时兜底);
     * 积压=asOf 时点未完成存量(不含 resolved/closed/cancelled)。 */
    AnalyticsRecords.WorkOrderSla merchantWorkOrderSla(java.time.Instant start, java.time.Instant end, java.time.Instant asOf);

    /** 清单#13 钻取:商户服务工单超时明细(与 mwo.overdue_rate 分子同过滤同口径;表无 code 列,code 为 null)。 */
    java.util.List<AnalyticsRecords.OverdueWorkOrderRow> listOverdueMerchantWorkOrders(
            java.time.Instant start, java.time.Instant end, java.time.Instant asOf, int limit, int offset);

    /** S5 能耗:窗内按表计类型合计 consumption(电/水/气/冷热单位不同,不跨类合计)。 */
    Map<String, BigDecimal> energyConsumptionByType(java.time.Instant start, java.time.Instant end);

    /** S5:窗内异常读数计数(abnormal_reason 非空)。 */
    long countAbnormalReadings(java.time.Instant start, java.time.Instant end);

    /** S5 钻取:异常读数明细(与 energy.abnormal_count 同过滤同口径)。 */
    java.util.List<AnalyticsRecords.AbnormalReadingRow> listAbnormalReadings(
            java.time.Instant start, java.time.Instant end, int limit, int offset);

    /** S3 钻取:到期预警合同明细(生效合同按到期日升序,含绑定面积)。 */
    java.util.List<AnalyticsRecords.ExpiringContractRow> listExpiringContracts(LocalDate asOf, int windowDays, int limit, int offset);

    /**
     * S2:应收账龄桶(as-of 时点存量,direction=receivable、未结清、已过应收日)。
     * 返回 bucket -> (merchantId(可空=未挂商户) -> 未清金额);bucket in a0_30/a31_60/a61_90/a90p。
     */
    Map<String, Map<String, BigDecimal>> arOverdueAgingByMerchant(LocalDate asOf);

    /** S2:月窗实收(ar_ap_payments.payment_time 落窗,仅 receivable 项)。 */
    BigDecimal arCollectedCentsBetween(java.time.Instant start, java.time.Instant end);

    /** S2:月窗应收(due_date 落 [monthStart, endExclusive),receivable 全额,不含未来月)。 */
    BigDecimal arReceivableDueCentsBetween(LocalDate monthStart, LocalDate endExclusive);

    /** S2 钻取:欠费商户明细排行(实时,按未清金额降序;bucket 可空=全部)。 */
    java.util.List<AnalyticsRecords.ArDebtorRow> listOverdueDebtors(LocalDate asOf, String bucket, int limit, int offset);

    /** S0:经营日切分配置(V201)——九域清单读取(经营参数页数据源)。 */
    java.util.List<AnalyticsRecords.BusinessDayCutoffRecord> listBusinessDayCutoffs();

    /** S0:改某业务域的日切小时(0-23);后台经营参数页写入,Resolver 60s 缓存内生效。 */
    AnalyticsRecords.BusinessDayCutoffRecord upsertBusinessDayCutoff(String domain, int cutoffHour, String updatedBy, java.time.Instant now);

    BigDecimal sumTicketRevenueCents(LocalDate businessDate, String windowDomain);

    /**
     * RC1②：Σ ticket_refunds.refund_amount_cent——自营售票退款（按 created_at 落经营日窗口，与 T7 日结口径一致）；
     * 本方法为平台口径退款总额（不按商户拆）；商户维退款分量见 sumTicketRefundByMerchant（V220 后直挂 merchant_id）。用于 revenue.net 派生时冲减自营售票收入。
     */
    BigDecimal sumTicketRefundCents(LocalDate businessDate, String windowDomain);

    /** Σ bar_bill_settlements.settlement_amount_cent——酒吧收入；返回 merchantId(可 null) -> 金额。 */
    Map<String, BigDecimal> sumBarRevenueCentsByMerchant(LocalDate businessDate, String windowDomain);

    /**
     * 待确认#2：Σ bar_bill_adjustments.amount_cent（adjustment_type='refund'）——酒吧结账后退款；
     * 按 refunded_at 落经营日窗口，返回 merchantId(可 null) -> 退款金额。退款冲减 net（revenue.bar 不被改写，
     * 单列 revenue.bar_refund 展示并由净收入派生时扣减）；免单 waiver 不计入（已体现在更低结算实收）。
     */
    Map<String, BigDecimal> sumBarRefundCentsByMerchant(LocalDate businessDate, String windowDomain);

    /**
     * 待确认项#1：通票售票收入的"业态拆分展示口径"——按 issued_entitlements.allocated_revenue_cent
     * 汇总当日发放的、业态映射到酒吧的权益分配收入；返回 merchantId(可 null) -> 金额。
     * 仅作展示（revenue.ticket_bar_share），不并入 revenue.net：订单实付全额已在 revenue.ticket 计入，
     * revenue.bar 仍仅取 bar_bill_settlements 现场消费实收，二者口径区分，避免重复计净收入。
     */
    Map<String, BigDecimal> sumTicketBarShareCentsByMerchant(LocalDate businessDate, String windowDomain);

    /**
     * 待确认项#1：通票售票收入业态拆分——映射到游乐的权益分配收入；返回 merchantId(可 null) -> 金额。
     * 仅作展示（revenue.ticket_amusement_share），不并入 revenue.net（同上理由）。
     */
    Map<String, BigDecimal> sumTicketAmusementShareCentsByMerchant(LocalDate businessDate, String windowDomain);

    /**
     * 清单#4：本商户售票分量——Σ issued_entitlements.allocated_revenue_cent 按 merchant_id 汇总（按 created_at 落经营日窗口）。
     * 与 revenue.ticket（ticket_orders 实付全额，平台口径）口径互补：allocateRevenue 保证同一订单 Σallocated==订单实付，
     * 故 Σmerchant(allocated) 等于 Σ有发券订单的实付；direct 出票/无 allocated 的权益（allocated_revenue_cent IS NULL）不计，
     * 保守不归任何商户（避免凭空摊派）。返回 merchantId(可 null) -> 金额，并入商户行级 revenue.net 的售票分量。
     */
    Map<String, BigDecimal> sumTicketAllocatedRevenueByMerchant(LocalDate businessDate, String windowDomain);

    /**
     * 清单#4：本商户售票退款分量——V220 后 ticket_refunds 直挂 merchant_id（受理退款的销售商户 = 原订单 merchant_id），
     * 按 created_at 落经营日窗口，直接 GROUP BY merchant_id 汇总退款额（不再按 allocated 占比反推分摊）。
     * merchant_id 为 null（平台直营单）保守不归任何商户。返回 merchantId -> 退款额，用于商户行级 revenue.net 冲减。
     */
    Map<String, BigDecimal> sumTicketRefundByMerchant(LocalDate businessDate, String windowDomain);

    /**
     * P1-C 收入闭环：合同租金/物业/能源应收（ar_ap_items receivable，source_type contract_rent/property_fee/energy_fee）
     * 按 due_date = businessDate（期首逐期确认，不在生效日堆叠整租期）汇总到 merchant_id。
     * 返回 merchantId -> 金额（合同账单 merchant_id 恒非空），并入商户行级 revenue.net 的租金分量。
     */
    Map<String, BigDecimal> sumContractRentReceivableByMerchant(LocalDate businessDate, String windowDomain);

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

    // ==== KPI 统一明细（指标登记簿，2026-07-18 方案A）：明细行与合计同一 SQL 谓词同源 ====

    /** revenue.ticket 明细：已支付票务订单（经营日窗，按下单时间），合计=Σamount_cent（与 sumTicketRevenueCents 同谓词）。 */
    default AnalyticsRecords.MetricDetailRows ticketRevenueDetail(java.time.Instant start, java.time.Instant end, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** finance.recon_diff_count 明细：对账差异逐笔（start/end 可空=全历史），合计=COUNT(*)。 */
    default AnalyticsRecords.MetricDetailRows reconciliationDifferenceDetail(java.time.Instant start, java.time.Instant end, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** energy.total_electricity（及每平米电耗分子）明细：电表读数（经营日窗），合计=Σconsumption（与 energyConsumptionByType 同谓词）。 */
    default AnalyticsRecords.MetricDetailRows electricityReadingDetail(java.time.Instant start, java.time.Instant end, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** energy.electricity_per_sqm 分母明细：可租空间（商铺/摊位、非停用），合计=Σ面积（与出租率 occupancyAreas.rentable 同谓词）。 */
    default AnalyticsRecords.MetricDetailRows rentableSpaceDetail(int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    // 项目作用域收紧（2026-07-19）：以下带 String projectScope 的明细方法在 scope 非空时加
    //   (:project_scope::text IS NULL OR <scope_col> = :project_scope) 谓词（明细+合计同谓词），
    //   空 scope=平台全量（行为不变）。scope 列见 MetricRegistry.PROJECT_SCOPE_COLUMNS。
    //   各方法配「平台口径便捷重载」（无 projectScope=传 null），供既有平台级调用/一致性测试原样调用。
    /** finance.ar_balance 明细（平台口径便捷重载=空项目作用域）。 */
    default AnalyticsRecords.MetricDetailRows arOpenReceivableDetail(LocalDate asOf, int limit, int offset) {
        return arOpenReceivableDetail(asOf, limit, offset, null);
    }
    /** finance.ar_balance 明细：逾期未清应收（as-of=截至日），合计=Σunpaid_amount_cent（与 arOverdueAgingByMerchant 同谓词）。projectScope 非空按 project_id 过滤。 */
    default AnalyticsRecords.MetricDetailRows arOpenReceivableDetail(LocalDate asOf, int limit, int offset, String projectScope) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    // ==== KPI 统一明细·第二批（酒吧域）====

    /** revenue.bar 明细：已结算账单实收（经营日窗、剔除冲正），合计=Σsettlement_amount_cent（与 sumBarRevenueCentsByMerchant 同谓词）。 */
    default AnalyticsRecords.MetricDetailRows barSettlementRevenueDetail(java.time.Instant start, java.time.Instant end, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** bar.order_count / bar.order_amount 明细：酒吧订单全状态（start/end 可空=全历史，与列表页 listOrders 平台口径同谓词）；sumAmount=true 合计Σtotal_amount_cent，false 合计笔数。 */
    default AnalyticsRecords.MetricDetailRows barOrderDetail(java.time.Instant start, java.time.Instant end, boolean sumAmount, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** bar.open_order_count 明细：当前未完结订单（状态非 paid/cancelled、下单时间早于截至窗末；状态按台账当前值），合计=COUNT(*)。 */
    default AnalyticsRecords.MetricDetailRows barOpenOrderDetail(java.time.Instant asOfEnd, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** bar.saved_bottle_count 明细：存酒登记全状态（start/end 可空=全历史，与存酒页 listSavedBottles 平台口径同谓词），合计=COUNT(*)。 */
    default AnalyticsRecords.MetricDetailRows savedBottleDetail(java.time.Instant start, java.time.Instant end, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** bar.saved_bottle_stored 明细：当前在存（status='stored'、存入时间早于截至窗末），合计=COUNT(*)。 */
    default AnalyticsRecords.MetricDetailRows savedBottleStoredDetail(java.time.Instant asOfEnd, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** bar.saved_bottle_quantity 明细：全部登记行（存入时间早于截至窗末），合计=Σremaining_quantity（台账当前值）。 */
    default AnalyticsRecords.MetricDetailRows savedBottleQuantityDetail(java.time.Instant asOfEnd, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    // ==== KPI 统一明细·第三批（票务域）：谓词逐字同各页面既有取数 ====

    /** ticket.order_count / ticket.paid_order_count 明细：票务订单（start/end 可空=全历史，与 GET /ticketing/orders 平台口径同谓词）；paidOnly=true 加 pay_status='paid'；合计=COUNT(*)。 */
    default AnalyticsRecords.MetricDetailRows ticketOrderDetail(java.time.Instant start, java.time.Instant end, boolean paidOnly, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** ticket.pending_issue_count 明细：当前待出票订单（issue_status<>'issued'、下单时间早于截至窗末；出票状态按台账当前值），合计=COUNT(*)。 */
    default AnalyticsRecords.MetricDetailRows ticketPendingIssueDetail(java.time.Instant asOfEnd, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** amusement.session_count / amusement.session_capacity 明细：游乐场次（start/end 可空=全历史，与 GET /amusement/sessions 平台口径同谓词）；sumCapacity=true 合计Σcapacity，false 合计场次数。 */
    default AnalyticsRecords.MetricDetailRows amusementSessionDetail(java.time.Instant start, java.time.Instant end, boolean sumCapacity, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** amusement.group_booking_count / amusement.group_reserved_capacity 明细：团体/包场预约（start/end 可空=全历史，与 GET /amusement/group-bookings 平台口径同谓词）；sumReserved=true 合计Σreserved_capacity，false 合计笔数。 */
    default AnalyticsRecords.MetricDetailRows amusementGroupBookingDetail(java.time.Instant start, java.time.Instant end, boolean sumReserved, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** amusement.entry_count / amusement.exit_count 明细：游乐核销按方向（entry=direction='entry'或空、含全部结果；exit=direction='exit'；start/end 可空=全历史，与 GET /amusement/verifications 默认无筛选全量同谓词），合计=COUNT(*)。 */
    default AnalyticsRecords.MetricDetailRows amusementVerificationDirectionDetail(java.time.Instant start, java.time.Instant end, boolean entry, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** amusement.inspection_count 明细：安全巡检全状态（start/end 可空=全历史，与 GET /amusement/safety-inspections 平台口径同谓词），合计=COUNT(*)。 */
    default AnalyticsRecords.MetricDetailRows amusementInspectionDetail(java.time.Instant start, java.time.Instant end, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** amusement.inspection_pending_rectification 明细：当前待整改巡检（COALESCE(rectification_status,overall_result)∈待整改白名单、创建时间早于截至窗末；状态按台账当前值），合计=COUNT(*)。 */
    default AnalyticsRecords.MetricDetailRows amusementInspectionPendingRectificationDetail(java.time.Instant asOfEnd, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** finance.ap_balance 明细：应付欠款余额（as-of），direction='payable'，同 arOpenReceivableDetail 结构。 */
    /** finance.ap_balance 明细（平台口径便捷重载=空项目作用域）。 */
    default AnalyticsRecords.MetricDetailRows apOpenPayableDetail(LocalDate asOf, int limit, int offset) {
        return apOpenPayableDetail(asOf, limit, offset, null);
    }
    default AnalyticsRecords.MetricDetailRows apOpenPayableDetail(LocalDate asOf, int limit, int offset, String projectScope) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** finance.ar_balance_aging90p 明细：应收逾期90天以上余额（as-of），应收谓词 + 逾期天数>90。 */
    /** finance.ar_balance_aging90p 明细（平台口径便捷重载=空项目作用域）。 */
    default AnalyticsRecords.MetricDetailRows arOverdueAging90pDetail(LocalDate asOf, int limit, int offset) {
        return arOverdueAging90pDetail(asOf, limit, offset, null);
    }
    default AnalyticsRecords.MetricDetailRows arOverdueAging90pDetail(LocalDate asOf, int limit, int offset, String projectScope) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** settlement.* 明细：通票核销结算流水（transaction_type='verification_settled'）；summaryMode∈{count,amount,tickets}。 */
    /** settlement.* 明细（平台口径便捷重载=空项目作用域）。 */
    default AnalyticsRecords.MetricDetailRows settlementTransactionDetail(java.time.Instant start, java.time.Instant end, String summaryMode, int limit, int offset) {
        return settlementTransactionDetail(start, end, summaryMode, limit, offset, null);
    }
    default AnalyticsRecords.MetricDetailRows settlementTransactionDetail(java.time.Instant start, java.time.Instant end, String summaryMode, int limit, int offset, String projectScope) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    // ==== KPI 统一明细·第五批（人事域）：谓词逐字同各列表端点既有取数 ====

    /** staff.shift_count 明细：排班按 work_date 落 [from,to] 闭区间（同 listShifts 平台口径谓词），合计=COUNT(*)。 */
    /** staff.shift_count 明细（平台口径便捷重载=空项目作用域）。 */
    default AnalyticsRecords.MetricDetailRows staffShiftDetail(LocalDate from, LocalDate to, int limit, int offset) {
        return staffShiftDetail(from, to, limit, offset, null);
    }
    default AnalyticsRecords.MetricDetailRows staffShiftDetail(LocalDate from, LocalDate to, int limit, int offset, String projectScope) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** staff.attendance_count / staff.attendance_late_count 明细：考勤按 punched_at 落自然日窗 [from 00:00, to+1 00:00)（同 listAttendanceOn，不走经营日切）；lateOnly=true 加 judgement='late'；合计=COUNT(*)。 */
    /** staff.attendance_* 明细（平台口径便捷重载=空项目作用域）。 */
    default AnalyticsRecords.MetricDetailRows staffAttendanceDetail(LocalDate from, LocalDate to, boolean lateOnly, int limit, int offset) {
        return staffAttendanceDetail(from, to, lateOnly, limit, offset, null);
    }
    default AnalyticsRecords.MetricDetailRows staffAttendanceDetail(LocalDate from, LocalDate to, boolean lateOnly, int limit, int offset, String projectScope) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** staff.payroll_net_total / staff.payroll_statement_count 明细：薪资单按 month 落 [fromMonth,toMonth] 闭区间（同 listPayrollStatements）；sumTotal=true 合计Σtotal_cent，false 合计份数。 */
    /** staff.payroll_* 明细（平台口径便捷重载=空项目作用域）。 */
    default AnalyticsRecords.MetricDetailRows staffPayrollDetail(LocalDate from, LocalDate to, boolean sumTotal, int limit, int offset) {
        return staffPayrollDetail(from, to, sumTotal, limit, offset, null);
    }
    default AnalyticsRecords.MetricDetailRows staffPayrollDetail(LocalDate from, LocalDate to, boolean sumTotal, int limit, int offset, String projectScope) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** staff.pending_leave_count 明细：待审请假（status='pending'、提交时间早于截至日窗末；状态按台账当前值），合计=COUNT(*)。 */
    /** staff.pending_leave_count 明细（平台口径便捷重载=空项目作用域）。 */
    default AnalyticsRecords.MetricDetailRows staffPendingLeaveDetail(LocalDate asOf, int limit, int offset) {
        return staffPendingLeaveDetail(asOf, limit, offset, null);
    }
    default AnalyticsRecords.MetricDetailRows staffPendingLeaveDetail(LocalDate asOf, int limit, int offset, String projectScope) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** staff.active_headcount 明细：在职员工账号（account_type='staff'、status='active'、建号早于截至日窗末），合计=COUNT(*)。 */
    /** staff.active_headcount 明细（平台口径便捷重载=空项目作用域）。 */
    default AnalyticsRecords.MetricDetailRows staffActiveHeadcountDetail(LocalDate asOf, int limit, int offset) {
        return staffActiveHeadcountDetail(asOf, limit, offset, null);
    }
    default AnalyticsRecords.MetricDetailRows staffActiveHeadcountDetail(LocalDate asOf, int limit, int offset, String projectScope) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** traffic.amusement 明细：成功游乐核销记录（verification_result='success'，两方向），合计=COUNT(*)。谓词同 ETL countAmusementVerificationSuccess。 */
    default AnalyticsRecords.MetricDetailRows amusementVerificationSuccessDetail(java.time.Instant start, java.time.Instant end, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** verify.done 明细：成功票务核销记录（status='success'），合计=COUNT(*)。谓词同 ETL countTicketVerificationSuccessByMerchant 平台合计。 */
    default AnalyticsRecords.MetricDetailRows ticketVerificationSuccessDetail(java.time.Instant start, java.time.Instant end, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** member.new 明细：新增客人账号（建号落窗、status NOT IN ('deregistered','blacklisted')；仅脱敏列），合计=COUNT(*)。谓词同 ETL countNewMembers。 */
    default AnalyticsRecords.MetricDetailRows newMemberDetail(java.time.Instant start, java.time.Instant end, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }

    /** risk.pending_review 明细：待复核主管审批（review_status='pending_review'）+ 未处置风险预警（status='open'），产生时间早于截至日窗末，合计=COUNT(*)。谓词同 ETL countPendingReview。 */
    default AnalyticsRecords.MetricDetailRows pendingReviewDetail(LocalDate asOf, int limit, int offset) {
        return new AnalyticsRecords.MetricDetailRows(java.util.List.of(), 0, BigDecimal.ZERO);
    }
}
