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
