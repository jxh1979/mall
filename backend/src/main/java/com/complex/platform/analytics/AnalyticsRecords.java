package com.complex.platform.analytics;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class AnalyticsRecords {

    private AnalyticsRecords() {
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
            Map<String, MetricValue> metrics
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
            Instant resolvedAt
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
}
