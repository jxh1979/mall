package com.complex.platform.analytics;

// 文件版本：V0.02

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class AnalyticsRecords {

    private AnalyticsRecords() {
    }

    public record OccupancyAreas(
            java.math.BigDecimal rentableArea,
            java.math.BigDecimal leasedArea,
            java.math.BigDecimal signedPendingArea,
            // HP-P01（第十二批操作员裁决补充第 5 条）：车位按个数计——车位个数（不可租除外）与已租个数（合同已租车位＋已登记且在职的员工车辆）。
            java.math.BigDecimal parkingSpaces,
            java.math.BigDecimal parkingRentedSpaces,
            java.math.BigDecimal totalArea
    ) {
    }

    /**
     * HP-P01 停车看板单项目一个营业日的事实（BSM「停车场」章「连接经营看板」；第十二批操作员裁决补充第 6、7 条）：
     * 车位占用回传的在场合计与车位合计、当日最近一次回传的车位数、入场车次、离场车次（其中临停、临停用了消费抵免）、离场停留分钟合计、
     * 停车收入（临停停车费实收＋当期车位月租账单金额）。
     */
    public record ParkingDayFacts(long snapshotOccupied, long snapshotSpaces, long entries, long latestSpaces, long temporaryExits,
            long creditExits, long exits, long stayMinutes, long revenueCent) {
    }

    public record VacantSpaceRow(
            String spaceId,
            String spaceName,
            String spaceType,
            String floorNo,
            java.math.BigDecimal areaSqm,
            String rentalStatus
    ) {
    }

    public record ExpiringContractRow(
            String contractId,
            String merchantId,
            String merchantName,
            LocalDate expiryDate,
            long daysLeft,
            java.math.BigDecimal boundArea
    ) {
    }

    public record ArDebtorRow(
            String merchantId,
            String merchantName,
            java.math.BigDecimal outstandingCent,
            LocalDate oldestDueDate,
            long overdueDays,
            String bucket
    ) {
    }

    public record BusinessDayCutoffRecord(
            String domain,
            int cutoffHour,
            java.time.Instant updatedAt,
            String updatedBy
    ) {
    }

    public record MetricDefinitionRecord(
            String metricKey,
            String name,
            String domain,
            String unit,
            String calculationNote,
            String ownerRole,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record DashboardSnapshotRecord(
            String snapshotKey,
            String projectId,
            LocalDate businessDate,
            long revenueAmountCents,
            int orderCount,
            int riskAlertCount,
            int criticalRiskCount,
            Map<String, Object> payload,
            Instant createdAt
    ) {
    }

    /**
     * T12 看板总览/下钻响应：以 metric_code 为权威键的指标 map。
     * scope=null 表示平台口径（全量）；scope=merchantId 表示商户口径（仅自身）。
     * metrics 的 value 可能为 null（数据源未就绪，按"不伪造"返回 null + note）。
     */
    public record OverviewResponse(
            Instant asOf,
            String scope,
            LocalDate businessDate,
            Map<String, MetricValue> metrics
    ) {
    }

    /** 单个指标值：value 可空（数据未就绪）；note 携带口径/未就绪说明；dimension 可承载下钻明细。 */
    public record MetricValue(
            BigDecimal value,
            String unit,
            String note,
            Map<String, Object> dimension
    ) {
        public static MetricValue of(BigDecimal value, String unit) {
            return new MetricValue(value, unit, null, null);
        }

        public static MetricValue of(BigDecimal value, String unit, Map<String, Object> dimension) {
            return new MetricValue(value, unit, null, dimension);
        }

        public static MetricValue unavailable(String note) {
            return new MetricValue(null, null, note, null);
        }
    }

    /**
     * P2 集团/跨项目汇总响应（R6-D13）：按 project 维 rollup 关键指标（收入/客流/核销/风险/商户数）。
     * 当前单综合体——projects 列表通常仅一行；endpoint 真实读 ETL 平台快照按 project 归集，不伪造多 project。
     * asOf 为生成时刻；businessDate 为所读 ETL 经营日；projects 为各 project 的指标 rollup。
     */
    public record GroupOverviewResponse(
            Instant asOf,
            LocalDate businessDate,
            String groupBy,
            Map<String, MetricValue> groupAverages,
            List<GroupProjectRollup> projects
    ) {
    }

    /**
     * 单个 project 的指标 rollup。metrics 以 metric_code 为权威键，value 可空（数据源未就绪 → null + note，不伪造）。
     * merchantCount 为该 project 当日有 ETL 商户口径快照的去重商户数（真实计数，无快照则为 0）。
     */
    public record GroupProjectRollup(
            String projectId,
            String projectName,
            long merchantCount,
            Map<String, MetricValue> metrics,
            List<String> belowBenchmarkMetrics
    ) {
    }

    /**
     * T4.3 分时段客流：按经营日窗口（BusinessDayResolver）对成功核销 created_at 小时分桶。
     * buckets 恒为 0~23 共 24 桶（无数据补 0）。平台口径合并票务+游乐核销；商户口径仅本商户票务核销。
     */
    public record HourlyTrafficResponse(
            LocalDate businessDate,
            List<HourlyTrafficBucket> buckets
    ) {
    }

    /** 单个小时桶：hour 为 0~23，count 为该小时成功核销数。 */
    public record HourlyTrafficBucket(int hour, long count) {
    }

    /** ETL upsert 的快照唯一键（与 V070 唯一索引对齐）。 */
    public record MetricSnapshotKey(
            String metricKey,
            String domain,
            String periodType,
            LocalDate businessDate,
            String segmentType,
            String segmentId
    ) {
    }


    public record BusinessMetricSnapshotRecord(
            String snapshotId,
            String metricKey,
            String domain,
            String periodType,
            LocalDate businessDate,
            String segmentType,
            String segmentId,
            BigDecimal actualValue,
            BigDecimal baselineValue,
            BigDecimal varianceRate,
            String trendDirection,
            String projectionMethod,
            BigDecimal projectedValue,
            BigDecimal confidenceScore,
            Map<String, Object> payload,
            Instant createdAt
    ) {
    }

    public record MerchantSummaryRecord(
            String merchantId,
            String merchantName,
            String merchantType,
            long revenueAmountCents,
            int orderCount,
            int riskAlertCount,
            String settlementStatus,
            LocalDate businessDate,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record MerchantHealthScoreRecord(
            String merchantId,
            int score,
            String scoreLevel,
            int riskCount,
            long overdueArAmountCents,
            Instant lastReviewedAt,
            String summary,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record MetricAnomalyRecord(
            String id,
            String metricKey,
            String domain,
            String riskLevel,
            BigDecimal observedValue,
            BigDecimal expectedValue,
            String summary,
            String status,
            Instant detectedAt,
            Instant resolvedAt,
            String dispositionNote,
            String disposedBy,
            Instant disposedAt,
            String escalatedAlertId
    ) {
    }

    public record RiskRuleRecord(
            String ruleId,
            String ruleCode,
            String ruleName,
            String domain,
            String riskLevel,
            boolean enabled,
            Map<String, Object> ruleConfig,
            String actionNote,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record RiskAlertRecord(
            String id,
            String ruleId,
            String alertType,
            String riskLevel,
            String objectType,
            String objectId,
            String summary,
            String status,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String idempotencyKey,
            Instant createdAt,
            Instant resolvedAt,
            Instant updatedAt
    ) {
    }

    public record AiQualityTaskRecord(
            String taskId,
            String sourceType,
            String sourceId,
            String processingMode,
            String modelProvider,
            String modelVersion,
            String status,
            String requestedBy,
            Instant createdAt,
            Instant completedAt
    ) {
    }

    public record AiQualityResultRecord(
            String resultId,
            String taskId,
            String sourceType,
            String sourceId,
            int qualityScore,
            String riskLevel,
            BigDecimal confidenceScore,
            String summary,
            int findingCount,
            Instant createdAt
    ) {
    }

    /** H1 节假日区间(法定假期按国务院公布,URL 抓取或手动录入;同名跨年对比)。 */
    public record HolidayPeriod(
            String holidayName,
            int holidayYear,
            LocalDate startDate,
            LocalDate endDate,
            String source,
            Instant updatedAt
    ) {
    }

    /** H1 节假日对比行:总额+日均双列(2026-07-02 拍板,天数不同看日均才公平);存量类=假期末时点。 */
    public record HolidayComparisonRow(
            String metricKey,
            String label,
            String unit,
            String aggregation,
            BigDecimal currentTotal,
            BigDecimal currentDailyAvg,
            BigDecimal previousTotal,
            BigDecimal previousDailyAvg,
            BigDecimal yoyPct
    ) {
    }

    /** H1 节假日对比响应(含两侧区间与天数,便于页面标注)。 */
    public record HolidayComparisonResponse(
            String holidayName,
            int currentYear,
            LocalDate currentStart,
            LocalDate currentEnd,
            Integer previousYear,
            LocalDate previousStart,
            LocalDate previousEnd,
            java.util.List<HolidayComparisonRow> rows
    ) {
    }

    /** V219 提醒窗天数配置行(键值:window_key -> days)。 */
    public record NotificationWindow(
            String windowKey,
            int days,
            String description,
            String updatedBy,
            Instant updatedAt
    ) {
    }

    /** V218 AI/对标阈值配置(单例 default):AI建议采纳最低置信度 + 对标偏离告警线。 */
    public record AiThresholds(
            BigDecimal localLlmMinConfidence,
            BigDecimal benchmarkDeviation,
            String updatedBy,
            Instant updatedAt
    ) {
    }

    /** V221 商户级收入归因口径(可配置):某收入来源如何计入商户 net。种子=复现现有硬编码口径。 */
    public record MerchantRevenueAttributionRule(
            String id,
            String ruleScope,          // default / merchant
            String merchantId,         // rule_scope=merchant 时的商户;default 时 null
            String revenueSource,      // ticket / bar / contract_rent / thirdparty
            String attributionMode,    // gross / share_bps / exclude
            int shareBps,              // share_bps 模式下缩放万分比[0,10000]
            String refundAttribution,  // net_deduct / ignore
            String status,             // active / inactive
            boolean isDraft,
            String updatedBy,
            Instant updatedAt
    ) {
    }

    /** V220 招商漏斗计数(经营日窗口, 按线索 created_at 归属):总线索/已达意向/已签约。比率由 ETL 派生。 */
    public record LeasingFunnel(long leadCount, long intentCount, long signedCount) {
    }

    /** M3 异常检测阈值配置(环比突变,2026-07-02 拍板:阈值后台可调)。 */
    public record AnomalyConfig(
            BigDecimal surgePct,
            BigDecimal highPct,
            boolean enabled,
            String updatedBy,
            Instant updatedAt
    ) {
    }

    /** M3 指标定义同步行(代码目录=单一事实源,表=可查询投影)。 */
    public record MetricDefinitionSyncRow(
            String metricKey,
            String domain,
            String unit,
            boolean ready,
            String note
    ) {
    }

    /** M1 月度对比行(环比/同比;缺数据=null,前端标「积累中」,不伪造)。 */
    public record MonthlyComparisonRow(
            String metricKey,
            String label,
            String domain,
            String unit,
            String aggregation,
            BigDecimal current,
            BigDecimal prevMonth,
            BigDecimal momPct,
            BigDecimal yoySameMonth,
            BigDecimal yoyPct
    ) {
    }

    /** M1 月度对比响应。 */
    public record MonthlyComparisonResponse(
            String month,
            java.util.List<MonthlyComparisonRow> rows
    ) {
    }

    /** P1c 租售比店级取数原始行（repo→service）：某商户当期租金分子 + 第三方回流营业额分母（均分）。 */
    public record RentToSalesMerchantRow(
            String merchantId,
            String merchantName,
            String merchantType,
            long rentCent,
            long salesCent
    ) {
    }

    /** P1c 租售比取数聚合包：已归属商户行 + 未归属第三方营业额（merchant_id IS NULL，只进平台级分母）。 */
    public record RentToSalesAggregate(
            java.util.List<RentToSalesMerchantRow> merchants,
            long unattributedSalesCent
    ) {
    }

    /** P1c 租售比一档一行（店级/业态级/平台级通用）：ratioBps=租金*10000/营业额；营业额<=0→null（诚实标注 note）。 */
    public record RentToSalesRow(
            String key,
            String label,
            long rentCent,
            long salesCent,
            Long ratioBps,
            String note
    ) {
    }

    /** P1c 租售比响应：店级 / 业态级 / 平台级三档 + 未归属营业额（诚实标注，绝不塞给某商户）。 */
    public record RentToSalesResponse(
            String period,
            java.util.List<RentToSalesRow> byMerchant,
            java.util.List<RentToSalesRow> byMerchantType,
            RentToSalesRow platform,
            long unattributedSalesCent,
            String note
    ) {
    }

    /** 结算B2 放弃权益额外收入取数原始格（repo→service）：某权益类型桶 × 票种 的未核销归项目金额与笔数。
     *  bucket∈{drink,entry,amusement,show,other}；ticketType∈{pass(通票 bar_id IS NULL), ticket(单独门票 bar_id 非空)}。 */
    public record AbandonedEntitlementCell(
            String bucket,
            String ticketType,
            long amountCent,
            long count
    ) {
    }

    /** 结算B2 报表一格：金额（分）+ 笔数。 */
    public record AbandonedEntitlementAmount(
            long amountCent,
            long count
    ) {
    }

    /** 结算B2 报表一行：某权益类型 × {通票, 单独门票, 行合计}。 */
    public record AbandonedEntitlementRow(
            String bucket,
            String label,
            AbandonedEntitlementAmount pass,
            AbandonedEntitlementAmount ticket,
            AbandonedEntitlementAmount rowTotal
    ) {
    }

    /** 结算B2 放弃权益额外收入交叉表响应：行=权益类型、列=通票/单独门票，附列合计与总计。
     *  数据源=settlement_transactions(transaction_type=unverified_expired, beneficiary_type=project) 归项目流水，
     *  金额取权威 unverified_amount_cent（SettlementExpiryJob 已按 allocated×(total−used)/total 算好），零重算。 */
    public record AbandonedEntitlementRevenueResponse(
            String start,
            String end,
            java.util.List<AbandonedEntitlementRow> rows,
            AbandonedEntitlementAmount passTotal,
            AbandonedEntitlementAmount ticketTotal,
            AbandonedEntitlementAmount grandTotal,
            String note
    ) {
    }

    /** S4 工单时效聚合(P50 小时/超时分子分母/时点积压)。 */
    public record WorkOrderSla(
            BigDecimal responseP50Hours,
            BigDecimal handleP50Hours,
            long dueCount,
            long overdueCount,
            long backlog
    ) {
    }

    /** S4 钻取:超时工单明细行(与 wo.overdue_rate 分子同过滤)。 */
    public record OverdueWorkOrderRow(
            String workOrderId,
            String code,
            String title,
            String status,
            String priority,
            String assigneeId,
            Instant handleDueAt,
            Instant completedAt,
            BigDecimal overdueHours
    ) {
    }

    /** S5 钻取:异常读数明细行(与 energy.abnormal_count 同过滤)。 */
    public record AbnormalReadingRow(
            String readingId,
            String meterId,
            String meterName,
            String meterType,
            String period,
            BigDecimal consumption,
            String abnormalReason,
            Instant readAt
    ) {
    }

    public record CustomerIntentRecord(
            String intentId,
            String sourceType,
            String sourceId,
            String customerRef,
            String intentType,
            BigDecimal confidenceScore,
            String riskLevel,
            String summary,
            Instant createdAt
    ) {
    }

    public record RobotMetricRecord(
            String metricId,
            String robotId,
            String providerCode,
            int taskCount,
            int successCount,
            int failureCount,
            LocalDate businessDate,
            String summary,
            Instant createdAt
    ) {
    }

    public record StaffServiceScoreRecord(
            String scoreId,
            String staffId,
            String staffName,
            String roleCode,
            int serviceScore,
            int complaintCount,
            int positiveEventCount,
            String riskLevel,
            LocalDate businessDate,
            Instant createdAt
    ) {
    }

    /** 经营复盘锁定凭据（V216）：project_id 按 token scope 落库，locked_by=操作人，供刷新/换人/审计追责查回。 */
    public record BusinessReviewLockRecord(
            String id,
            String projectId,
            String domain,
            String operatorNote,
            int metricCount,
            String lockedBy,
            Instant lockedAt,
            String idempotencyKey
    ) {
    }

    /** KPI 统一明细（指标登记簿）：仓储层返回 当前页明细行 + 总行数 + 合计校验值（同一谓词同源）。 */
    public record MetricDetailRows(
            java.util.List<java.util.Map<String, Object>> rows,
            long totalRows,
            BigDecimal summaryValue
    ) {
    }

    /**
     * KPI 统一明细端点响应：分页明细行 + 合计校验值 + 口径说明 + 数据截至时间。
     * 比率类：part=numerator|denominator（两页签），numeratorValue/denominatorValue 恒返回；
     * summaryValue=当前口径合计（可加=区间求和；余额=区间末值；比率=分子/分母，分母 0 → null + summaryNote）。
     */
    public record MetricDetailResponse(
            String metricKey,
            String name,
            String valueType,
            String unit,
            String caliber,
            String source,
            String part,
            LocalDate from,
            LocalDate to,
            Instant dataAsOf,
            BigDecimal summaryValue,
            BigDecimal numeratorValue,
            BigDecimal denominatorValue,
            String summaryNote,
            java.util.List<MetricRegistry.MetricColumn> columns,
            java.util.List<java.util.Map<String, Object>> rows,
            int page,
            int size,
            long totalRows,
            String traceLinkPageId,
            String traceLinkColumnKey
    ) {
    }
}
