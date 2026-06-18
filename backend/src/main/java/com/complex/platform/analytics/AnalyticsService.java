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
import com.complex.platform.analytics.AnalyticsRecords.RiskAlertRecord;
import com.complex.platform.analytics.AnalyticsRecords.RiskRuleRecord;
import com.complex.platform.analytics.AnalyticsRecords.RobotMetricRecord;
import com.complex.platform.analytics.AnalyticsRecords.StaffServiceScoreRecord;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final Set<String> RISK_LEVELS = Set.of("normal", "suspicious", "high", "critical");
    private static final Set<String> RISK_ALERT_RESOLVE_ACTIONS = Set.of("investigating", "resolved", "dismissed");
    private static final Set<String> TERMINAL_ALERT_STATUSES = Set.of("resolved", "dismissed");
    private static final BigDecimal LOCAL_LLM_MIN_CONFIDENCE = new BigDecimal("0.60");
    private static final int MAX_SOURCE_TEXT_CHARS = 12_000;
    private static final int MAX_LLM_INPUT_CHARS = 4_000;

    private final AnalyticsRepository repository;
    private final AuditedOperationExecutor operations;
    private final Clock clock;
    private final LocalLlmClient localLlmClient;

    @Autowired
    public AnalyticsService(
            AnalyticsRepository repository,
            AuditedOperationExecutor operations,
            LocalLlmClient localLlmClient
    ) {
        this(repository, operations, Clock.systemUTC(), localLlmClient);
    }

    AnalyticsService(
            AnalyticsRepository repository,
            AuditedOperationExecutor operations,
            Clock clock,
            LocalLlmClient localLlmClient
    ) {
        this.repository = repository;
        this.operations = operations;
        this.clock = clock;
        this.localLlmClient = localLlmClient;
    }

    public PageResponse<AiQualityResultRecord> listAiQualityResults() {
        return readPage(
                "analytics.ai_quality_result.viewed",
                "analytics.ai_quality_result.viewed_failed",
                "analytics.risk_alert.view",
                "analytics_ai_quality_result",
                repository::listAiQualityResults
        );
    }

    public PageResponse<AiQualityTaskRecord> listAiQualityTasks() {
        return readPage(
                "analytics.ai_quality_task.viewed",
                "analytics.ai_quality_task.viewed_failed",
                "analytics.risk_alert.view",
                "analytics_ai_quality_task",
                repository::listAiQualityTasks
        );
    }

    @Transactional
    public AiQualitySuggestionResponse generateLocalLlmAiQualitySuggestion(AiQualitySuggestionRequest request) {
        return executeWrite(
                "analytics.local-llm-ai-quality-suggestion",
                request == null ? null : request.idempotencyKey(),
                request,
                AiQualitySuggestionResponse.class,
                "analytics.ai_quality.local_llm_suggestion.created",
                "analytics.ai_quality.local_llm_suggestion.failed",
                "analytics.risk_alert.manage",
                "analytics_ai_quality_task",
                () -> generateLocalLlmAiQualitySuggestionInternal(request)
        );
    }

    public PageResponse<CustomerIntentRecord> listCustomerIntents() {
        return readPage(
                "analytics.customer_intent.viewed",
                "analytics.customer_intent.viewed_failed",
                "analytics.risk_alert.view",
                "analytics_customer_intent",
                repository::listCustomerIntents
        );
    }

    public DashboardSnapshotRecord getP0OverviewDashboard() {
        return readSingle(
                "analytics.dashboard_viewed",
                "analytics.dashboard_view_failed",
                "analytics.dashboard.view",
                "analytics_dashboard_snapshot",
                repository::getP0OverviewDashboard
        );
    }


    public PageResponse<BusinessMetricSnapshotRecord> listBusinessMetricSnapshots() {
        return readPage(
                "analytics.business_metric_snapshot.viewed",
                "analytics.business_metric_snapshot.view_failed",
                "analytics.dashboard.view",
                "analytics_business_metric_snapshot",
                repository::listBusinessMetricSnapshots
        );
    }

    public PageResponse<MerchantHealthScoreRecord> listMerchantHealthScores() {
        return readPage(
                "analytics.merchant_health.viewed",
                "analytics.merchant_health.viewed_failed",
                "analytics.dashboard.view",
                "analytics_merchant_health_score",
                repository::listMerchantHealthScores
        );
    }

    public PageResponse<MerchantSummaryRecord> listAnalyticsMerchants() {
        return readPage(
                "analytics.merchant_summary.viewed",
                "analytics.merchant_summary.viewed_failed",
                "analytics.dashboard.view",
                "analytics_merchant_summary",
                repository::listAnalyticsMerchants
        );
    }

    public PageResponse<MetricAnomalyRecord> listMetricAnomalies() {
        return readPage(
                "analytics.metric_anomaly.viewed",
                "analytics.metric_anomaly.viewed_failed",
                "analytics.risk_alert.view",
                "analytics_metric_anomaly",
                repository::listMetricAnomalies
        );
    }

    public PageResponse<MetricDefinitionRecord> listMetricDefinitions() {
        return readPage(
                "analytics.metric_definition.viewed",
                "analytics.metric_definition.viewed_failed",
                "analytics.dashboard.view",
                "analytics_metric_definition",
                repository::listMetricDefinitions
        );
    }

    public PageResponse<RiskAlertRecord> listRiskAlerts() {
        return readPage(
                "risk_alert.viewed",
                "risk_alert.view_failed",
                "analytics.risk_alert.view",
                "analytics_risk_alert",
                repository::listRiskAlerts
        );
    }

    @Transactional
    public RiskAlertRecord createRiskAlert(RiskAlertRequest request) {
        return executeWrite(
                "analytics.create-risk-alert",
                request == null ? null : request.idempotencyKey(),
                request,
                RiskAlertRecord.class,
                "risk_alert.created",
                "risk_alert.create_failed",
                "analytics.risk_alert.manage",
                "analytics_risk_alert",
                () -> createRiskAlertInternal(request)
        );
    }

    @Transactional
    public RiskAlertRecord resolveRiskAlert(String id, StatusChangeRequest request) {
        String alertId = requireText(id, "id");
        return executeWrite(
                "analytics.resolve-risk-alert",
                request == null ? null : request.idempotencyKey(),
                request,
                RiskAlertRecord.class,
                "analytics.risk_alert.resolved",
                "analytics.risk_alert.resolved_failed",
                "analytics.risk_alert.manage",
                "analytics_risk_alert",
                () -> resolveRiskAlertInternal(alertId, request)
        );
    }

    public PageResponse<RiskRuleRecord> listRiskRules() {
        return readPage(
                "analytics.risk_rule.viewed",
                "analytics.risk_rule.viewed_failed",
                "analytics.risk_alert.view",
                "analytics_risk_rule",
                repository::listRiskRules
        );
    }

    public PageResponse<RobotMetricRecord> getRobotAnalytics() {
        return readPage(
                "analytics.robot.viewed",
                "analytics.robot.viewed_failed",
                "analytics.dashboard.view",
                "analytics_robot_metric",
                repository::listRobotMetrics
        );
    }

    public PageResponse<StaffServiceScoreRecord> listStaffServiceScores() {
        return readPage(
                "analytics.staff_service_score.viewed",
                "analytics.staff_service_score.viewed_failed",
                "analytics.risk_alert.view",
                "analytics_staff_service_score",
                repository::listStaffServiceScores
        );
    }

    private AiQualitySuggestionResponse generateLocalLlmAiQualitySuggestionInternal(AiQualitySuggestionRequest request) {
        AiQualitySuggestionRequest actual = requireRequest(request);
        String sourceType = requireLimitedText(actual.sourceType(), "source_type", 64);
        String sourceId = requireLimitedText(actual.sourceId(), "source_id", 64);
        String requestedBy = optionalLimitedText(actual.requestedBy(), "requested_by", 64);
        String sourceText = requireLimitedText(actual.sourceText(), "source_text", MAX_SOURCE_TEXT_CHARS);
        AiTextRedactor.RedactedText redacted = AiTextRedactor.redactAndTruncate(sourceText, MAX_LLM_INPUT_CHARS);
        LocalLlmSuggestion suggestion = localLlmClient.suggestQuality(sourceType, sourceId, redacted.value());
        int qualityScore = clampScore(suggestion.qualityScore());
        BigDecimal confidenceScore = clampConfidence(suggestion.confidenceScore());
        NormalizedRiskLevel riskLevel = normalizeSuggestionRiskLevel(suggestion.riskLevel());
        NormalizedText summary = normalizeSuggestionSummary(suggestion.summary());
        int findingCount = Math.max(0, suggestion.findingCount());
        boolean modelOutputNormalized = qualityScore != suggestion.qualityScore()
                || suggestion.confidenceScore() == null
                || confidenceScore.compareTo(suggestion.confidenceScore()) != 0
                || riskLevel.adjusted()
                || summary.adjusted()
                || findingCount != suggestion.findingCount();
        boolean manualReviewRequired = !"completed".equals(suggestion.modelStatus())
                || confidenceScore.compareTo(LOCAL_LLM_MIN_CONFIDENCE) < 0
                || modelOutputNormalized;
        Instant now = Instant.now(clock);
        String taskStatus = "completed".equals(suggestion.modelStatus()) ? "completed" : "failed";
        AiQualityTaskRecord task = repository.saveAiQualityTask(new AiQualityTaskRecord(
                UUID.randomUUID().toString(),
                sourceType,
                sourceId,
                "local_llm_suggestion",
                normalizeText(suggestion.modelProvider(), "local_llm"),
                normalizeText(suggestion.modelVersion(), "local-model"),
                taskStatus,
                requestedBy,
                now,
                now
        ));
        AiQualityResultRecord result = repository.saveAiQualityResult(new AiQualityResultRecord(
                UUID.randomUUID().toString(),
                task.taskId(),
                sourceType,
                sourceId,
                qualityScore,
                riskLevel.value(),
                confidenceScore,
                summary.value(),
                findingCount,
                now
        ));
        return new AiQualitySuggestionResponse(
                task,
                result,
                suggestion.modelStatus(),
                redacted.redactionApplied(),
                manualReviewRequired
        );
    }

    private RiskAlertRecord createRiskAlertInternal(RiskAlertRequest request) {
        RiskAlertRequest actual = requireRequest(request);
        String riskLevel = requireRiskLevel(actual.riskLevel());
        Instant now = Instant.now(clock);
        return repository.saveRiskAlert(new RiskAlertRecord(
                UUID.randomUUID().toString(),
                trimToNull(actual.ruleId()),
                requireText(actual.alertType(), "alert_type"),
                riskLevel,
                requireText(actual.objectType(), "object_type"),
                trimToNull(actual.objectId()),
                requireText(actual.summary(), "summary"),
                "open",
                trimToNull(actual.operatorNote()),
                actual.evidenceAttachmentIds() == null ? List.of() : List.copyOf(actual.evidenceAttachmentIds()),
                actual.idempotencyKey(),
                now,
                null,
                now
        ));
    }

    private RiskAlertRecord resolveRiskAlertInternal(String alertId, StatusChangeRequest request) {
        StatusChangeRequest actual = requireRequest(request);
        String targetStatus = requireRiskAlertResolveAction(actual.action());
        String reason = requireText(actual.reason(), "reason");
        RiskAlertRecord current = repository.findRiskAlertById(alertId)
                .orElseThrow(() -> ApiException.notFound("RISK_ALERT_NOT_FOUND", "risk alert not found"));
        if (TERMINAL_ALERT_STATUSES.contains(current.status())) {
            throw ApiException.conflict("RISK_ALERT_ALREADY_RESOLVED", "risk alert is already resolved");
        }
        Instant now = Instant.now(clock);
        return repository.updateRiskAlert(new RiskAlertRecord(
                current.id(),
                current.ruleId(),
                current.alertType(),
                current.riskLevel(),
                current.objectType(),
                current.objectId(),
                current.summary(),
                targetStatus,
                reason,
                actual.evidenceAttachmentIds() == null ? current.evidenceAttachmentIds() : List.copyOf(actual.evidenceAttachmentIds()),
                current.idempotencyKey(),
                current.createdAt(),
                now,
                now
        ));
    }

    private <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<List<T>> action
    ) {
        return operations.readPage(
                AuditedOperation.of(successEvent, failureEvent, permissionCode, businessType),
                action
        );
    }

    private <T> T readSingle(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<T> action
    ) {
        return operations.readSingle(
                AuditedOperation.of(successEvent, failureEvent, permissionCode, businessType),
                action,
                this::businessId
        );
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<T> action
    ) {
        return operations.executeWrite(
                operationId,
                idempotencyKey,
                request,
                responseType,
                AuditedOperation.of(successEvent, failureEvent, permissionCode, businessType),
                action,
                this::businessId,
                Map.of()
        );
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireRiskLevel(String value) {
        String riskLevel = requireText(value, "risk_level");
        if (!RISK_LEVELS.contains(riskLevel)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "risk_level is invalid");
        }
        return riskLevel;
    }

    private String requireRiskAlertResolveAction(String value) {
        String action = requireText(value, "action");
        if (!RISK_ALERT_RESOLVE_ACTIONS.contains(action)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "action is invalid");
        }
        return action;
    }

    private String requireLimitedText(String value, String fieldName, int maxLength) {
        String text = requireText(value, fieldName);
        if (text.length() > maxLength) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is too long");
        }
        return text;
    }

    private String optionalLimitedText(String value, String fieldName, int maxLength) {
        String text = trimToNull(value);
        if (text != null && text.length() > maxLength) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is too long");
        }
        return text;
    }

    private int clampScore(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private NormalizedRiskLevel normalizeSuggestionRiskLevel(String value) {
        String raw = trimToNull(value);
        if (raw == null) {
            return new NormalizedRiskLevel("suspicious", true);
        }
        String normalized = raw.toLowerCase(Locale.ROOT);
        if (!RISK_LEVELS.contains(normalized)) {
            return new NormalizedRiskLevel("suspicious", true);
        }
        return new NormalizedRiskLevel(normalized, !normalized.equals(raw));
    }

    private NormalizedText normalizeSuggestionSummary(String value) {
        String summary = trimToNull(value);
        if (summary == null) {
            return new NormalizedText("local LLM response requires manual review", true);
        }
        if (summary.length() > 500) {
            return new NormalizedText(summary.substring(0, 500), true);
        }
        return new NormalizedText(summary, false);
    }

    private BigDecimal clampConfidence(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return value;
    }

    private String normalizeText(String value, String fallback) {
        String text = trimToNull(value);
        return text == null ? fallback : text;
    }

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String businessId(Object value) {
        if (value instanceof RiskAlertRecord record) {
            return record.id();
        }
        if (value instanceof DashboardSnapshotRecord record) {
            return record.snapshotKey();
        }
        if (value instanceof AiQualitySuggestionResponse response) {
            return response.task().taskId();
        }
        return null;
    }


    private record NormalizedRiskLevel(String value, boolean adjusted) {
    }

    private record NormalizedText(String value, boolean adjusted) {
    }
}
