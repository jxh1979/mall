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
