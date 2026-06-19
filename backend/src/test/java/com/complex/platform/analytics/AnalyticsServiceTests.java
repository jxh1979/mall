package com.complex.platform.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

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
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnalyticsServiceTests {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-14T00:00:00Z"), ZoneOffset.UTC);

    private InMemoryAnalyticsRepository analyticsRepository;
    private InMemoryIdempotencyRepository idempotencyRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private CapturingLocalLlmClient localLlmClient;
    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        analyticsRepository = new InMemoryAnalyticsRepository();
        idempotencyRepository = new InMemoryIdempotencyRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        localLlmClient = new CapturingLocalLlmClient();
        seedReadModels();
        AuditedOperationExecutor operations = new AuditedOperationExecutor(
                new IdempotencyService(idempotencyRepository, new ObjectMapper()),
                new AuditEventService(auditEventRepository)
        );
        service = new AnalyticsService(
                analyticsRepository,
                operations,
                FIXED_CLOCK,
                localLlmClient
        );
    }

    @Test
    void readEndpointsReturnRepositoryDataAndRecordExactOpenApiAuditEvents() {
        assertThat(service.listAiQualityResults().items()).extracting(AiQualityResultRecord::resultId).contains("result_1");
        assertThat(service.listAiQualityTasks().items()).extracting(AiQualityTaskRecord::taskId).contains("task_1");
        assertThat(service.listCustomerIntents().items()).extracting(CustomerIntentRecord::intentId).contains("intent_1");
        assertThat(service.getP0OverviewDashboard().snapshotKey()).isEqualTo("p0_overview_today");
        assertThat(service.listBusinessMetricSnapshots().items()).extracting(BusinessMetricSnapshotRecord::metricKey).contains("gmv_amount_cents");
        assertThat(service.listMerchantHealthScores().items()).extracting(MerchantHealthScoreRecord::merchantId).contains("merchant_1");
        assertThat(service.listAnalyticsMerchants().items()).extracting(MerchantSummaryRecord::merchantId).contains("merchant_1");
        assertThat(service.listMetricAnomalies().items()).extracting(MetricAnomalyRecord::id).contains("anomaly_1");
        assertThat(service.listMetricDefinitions().items()).extracting(MetricDefinitionRecord::metricKey).contains("gmv_amount_cents");
        assertThat(service.listRiskAlerts().items()).extracting(RiskAlertRecord::id).contains("alert_1");
        assertThat(service.listRiskRules().items()).extracting(RiskRuleRecord::ruleId).contains("rule_1");
        assertThat(service.getRobotAnalytics().items()).extracting(RobotMetricRecord::metricId).contains("robot_metric_1");
        assertThat(service.listStaffServiceScores().items()).extracting(StaffServiceScoreRecord::scoreId).contains("score_1");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains(
                        "analytics.ai_quality_result.viewed",
                        "analytics.ai_quality_task.viewed",
                        "analytics.customer_intent.viewed",
                        "analytics.dashboard_viewed",
                        "analytics.business_metric_snapshot.viewed",
                        "analytics.merchant_health.viewed",
                        "analytics.merchant_summary.viewed",
                        "analytics.metric_anomaly.viewed",
                        "analytics.metric_definition.viewed",
                        "risk_alert.viewed",
                        "analytics.risk_rule.viewed",
                        "analytics.robot.viewed",
                        "analytics.staff_service_score.viewed"
                );
    }

    @Test
    void overviewReturnsMetricCodeKeyedMapForPlatformScope() {
        // 平台口径快照（ETL 落地）。
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.ticket", "platform", null), new BigDecimal("128800"), "etl_aggregate", Instant.now(FIXED_CLOCK));
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.bar", "platform", null), new BigDecimal("68800"), "etl_aggregate", Instant.now(FIXED_CLOCK));

        var overview = service.overview(Optional.empty());

        assertThat(overview.scope()).isNull();
        // DTO 契约：键为 metric_code。
        assertThat(overview.metrics()).containsKeys("revenue.ticket", "revenue.bar", "verify.done", "member.new", "risk.pending_review");
        assertThat(overview.metrics().get("revenue.ticket").value()).isEqualByComparingTo("128800");
        assertThat(overview.metrics().get("revenue.bar").value()).isEqualByComparingTo("68800");
    }

    @Test
    void overviewEnforcesMerchantScopeIsolation() {
        // 商户 A 与 B 的酒吧收入分别落快照（行级隔离按 segment_id）。
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.bar", "merchant", "merchant_a"), new BigDecimal("5000"), "etl_aggregate", Instant.now(FIXED_CLOCK));
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.bar", "merchant", "merchant_b"), new BigDecimal("9000"), "etl_aggregate", Instant.now(FIXED_CLOCK));

        var merchantA = service.overview(Optional.of("merchant_a"));

        assertThat(merchantA.scope()).isEqualTo("merchant_a");
        assertThat(merchantA.metrics().get("revenue.bar").value()).isEqualByComparingTo("5000");
        // 跨商户不可见：商户 A 不应看到商户 B 的 9000。
        assertThat(merchantA.metrics().get("revenue.bar").value()).isNotEqualByComparingTo("9000");
    }

    @Test
    void overviewReturnsNullWithoutFabricationForUnreadyAndScopelessMetrics() {
        // 仅落地平台口径 revenue.bar；商户口径无该指标行。
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.bar", "merchant", "merchant_a"), new BigDecimal("5000"), "etl_aggregate", Instant.now(FIXED_CLOCK));

        var merchantA = service.overview(Optional.of("merchant_a"));

        // 数据源未就绪的指标不伪造：value=null + note。
        assertThat(merchantA.metrics().get("supply.received").value()).isNull();
        assertThat(merchantA.metrics().get("supply.received").note()).contains("T6");
        assertThat(merchantA.metrics().get("risk.manual_backfill").value()).isNull();
        assertThat(merchantA.metrics().get("revenue.thirdparty").value()).isNull();
        // 源表无 merchant_id 的指标在商户口径下无快照行 → null（不伪造），带 scope note。
        assertThat(merchantA.metrics().get("revenue.ticket").value()).isNull();
        assertThat(merchantA.metrics().get("revenue.ticket").note()).contains("merchant_id");
    }

    @Test
    void dashboardDomainFiltersByDomainAndIsScopeIsolated() {
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.bar", "merchant", "merchant_a"), new BigDecimal("5000"), "etl_aggregate", Instant.now(FIXED_CLOCK));

        var barDomain = service.dashboardDomain("bar", Optional.of("merchant_a"));

        assertThat(barDomain.metrics()).containsOnlyKeys("revenue.bar");
        assertThat(barDomain.metrics().get("revenue.bar").value()).isEqualByComparingTo("5000");
    }

    @Test
    void repositoryFailureRecordsExactFailureAuditEventAndRethrows() {
        analyticsRepository.failAiQualityResults = true;

        assertThatThrownBy(service::listAiQualityResults)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("analytics store unavailable");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName, AuditEvent::eventResult)
                .containsExactly(tuple("analytics.ai_quality_result.viewed_failed", "failure"));
    }

    @Test
    void createRiskAlertRejectsInvalidRiskLevel() {
        assertThatThrownBy(() -> service.createRiskAlert(riskAlertRequest("bad_risk", "metric_anomaly", "unknown", "merchant", "summary")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");

        assertThat(analyticsRepository.riskAlertSaveCount).isZero();
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).containsExactly("risk_alert.create_failed");
    }

    @Test
    void createRiskAlertRejectsMissingRequiredFields() {
        assertThatThrownBy(() -> service.createRiskAlert(riskAlertRequest("missing_type", " ", "high", "merchant", "summary")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
        assertThatThrownBy(() -> service.createRiskAlert(riskAlertRequest("missing_object", "metric_anomaly", "high", " ", "summary")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
        assertThatThrownBy(() -> service.createRiskAlert(riskAlertRequest("missing_summary", "metric_anomaly", "high", "merchant", " ")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");

        assertThat(analyticsRepository.riskAlertSaveCount).isZero();
    }

    @Test
    void createRiskAlertUsesAnalyticsCreateRiskAlertIdempotencyOperation() {
        RiskAlertRequest request = riskAlertRequest("create-once", "metric_anomaly", "high", "merchant", "Revenue dropped");

        RiskAlertRecord first = service.createRiskAlert(request);
        RiskAlertRecord second = service.createRiskAlert(request);

        assertThat(second).isEqualTo(first);
        assertThat(first)
                .extracting(RiskAlertRecord::riskLevel, RiskAlertRecord::status, RiskAlertRecord::idempotencyKey)
                .containsExactly("high", "open", "create-once");
        assertThat(analyticsRepository.riskAlertSaveCount).isEqualTo(1);
        assertThat(idempotencyRepository.records).containsKey("analytics.create-risk-alert:create-once");
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "risk_alert.created".equals(event.eventName()))
                .hasSize(1);
    }

    @Test
    void resolveRiskAlertRejectsMissingTargetStatusOrReason() {
        assertThatThrownBy(() -> service.resolveRiskAlert("alert_1", new StatusChangeRequest("resolve-missing-action", " ", "handled", List.of())))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
        assertThatThrownBy(() -> service.resolveRiskAlert("alert_1", new StatusChangeRequest("resolve-missing-note", "resolved", " ", List.of())))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void resolveRiskAlertRejectsInvalidActionWithApiExceptionCodeValidationFailed() {
        assertThatThrownBy(() -> service.resolveRiskAlert(
                        "alert_1",
                        new StatusChangeRequest("resolve-invalid-action", "send_wechat", "handled", List.of())
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");

        assertThat(analyticsRepository.riskAlertUpdateCount).isZero();
    }

    @Test
    void resolveRiskAlertRejectsUnknownAlertId() {
        assertThatThrownBy(() -> service.resolveRiskAlert("missing_alert", resolveRequest("resolve-missing")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("RISK_ALERT_NOT_FOUND");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("analytics.risk_alert.resolved_failed");
    }

    @Test
    void resolveRiskAlertRejectsAlreadyResolvedAlert() {
        RiskAlertRecord resolved = resolvedAlert("alert_resolved");
        analyticsRepository.riskAlerts.put(resolved.id(), resolved);

        assertThatThrownBy(() -> service.resolveRiskAlert(resolved.id(), resolveRequest("resolve-twice")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("RISK_ALERT_ALREADY_RESOLVED");
    }

    @Test
    void resolveRiskAlertUsesAnalyticsResolveRiskAlertIdempotencyOperation() {
        StatusChangeRequest request = resolveRequest("resolve-once");

        RiskAlertRecord first = service.resolveRiskAlert("alert_1", request);
        RiskAlertRecord second = service.resolveRiskAlert("alert_1", request);

        assertThat(second).isEqualTo(first);
        assertThat(first.status()).isEqualTo("resolved");
        assertThat(first.operatorNote()).isEqualTo("handled by operator");
        assertThat(first.resolvedAt()).isEqualTo(Instant.now(FIXED_CLOCK));
        assertThat(analyticsRepository.riskAlertUpdateCount).isEqualTo(1);
        assertThat(idempotencyRepository.records).containsKey("analytics.resolve-risk-alert:resolve-once");
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "analytics.risk_alert.resolved".equals(event.eventName()))
                .hasSize(1);
    }


    @Test
    void generateLocalLlmAiQualitySuggestionStoresResultAndRedactsSensitiveInput() {
        localLlmClient.nextSuggestion = LocalLlmSuggestion.completed(
                "local_llm",
                "gemma-local",
                88,
                "suspicious",
                new BigDecimal("0.72"),
                "service tone risk found",
                2
        );

        AiQualitySuggestionResponse response = service.generateLocalLlmAiQualitySuggestion(new AiQualitySuggestionRequest(
                "llm-once",
                "ticket_call",
                "call_160",
                "phone 13812345678 id 110101199003071234 email guest@example.com card 6222021234567890123 complaint text",
                "staff_1"
        ));

        assertThat(response.modelStatus()).isEqualTo("completed");
        assertThat(response.redactionApplied()).isTrue();
        assertThat(response.manualReviewRequired()).isFalse();
        assertThat(localLlmClient.lastSourceText)
                .doesNotContain("13812345678")
                .doesNotContain("110101199003071234")
                .doesNotContain("guest@example.com")
                .doesNotContain("6222021234567890123")
                .contains("[PHONE_REDACTED]")
                .contains("[ID_REDACTED]")
                .contains("[EMAIL_REDACTED]")
                .contains("[CARD_REDACTED]");
        assertThat(response.task().processingMode()).isEqualTo("local_llm_suggestion");
        assertThat(response.task().modelProvider()).isEqualTo("local_llm");
        assertThat(response.result().qualityScore()).isEqualTo(88);
        assertThat(response.result().riskLevel()).isEqualTo("suspicious");
        assertThat(analyticsRepository.aiQualityTaskSaveCount).isEqualTo(1);
        assertThat(analyticsRepository.aiQualityResultSaveCount).isEqualTo(1);
    }

    @Test
    void localLlmSuggestionRedactsInternationalPhonesIbanSecretsAndLongTokens() {
        localLlmClient.nextSuggestion = LocalLlmSuggestion.completed(
                "local_llm", "gemma-local", 80, "normal", new BigDecimal("0.10"), "ok", 0);

        AiQualitySuggestionResponse response = service.generateLocalLlmAiQualitySuggestion(new AiQualitySuggestionRequest(
                "llm-intl-redact",
                "ticket_call",
                "call_intl",
                "intl +14155552671 iban GB82WEST12345698765432 aws AKIAIOSFODNN7EXAMPLE "
                        + "key sk-live_abcdefghijklmnop token deadbeefdeadbeefdeadbeefdeadbeef12345678 ssn 123-45-6789",
                "staff_1"
        ));

        assertThat(response.redactionApplied()).isTrue();
        assertThat(localLlmClient.lastSourceText)
                .doesNotContain("+14155552671")
                .doesNotContain("GB82WEST12345698765432")
                .doesNotContain("AKIAIOSFODNN7EXAMPLE")
                .doesNotContain("sk-live_abcdefghijklmnop")
                .doesNotContain("deadbeefdeadbeefdeadbeefdeadbeef12345678")
                .doesNotContain("123-45-6789")
                .contains("[PHONE_REDACTED]")
                .contains("[SECRET_REDACTED]")
                .contains("[ID_REDACTED]");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("analytics.ai_quality.local_llm_suggestion.created");
    }

    @Test
    void generateLocalLlmAiQualitySuggestionFallsBackToManualReviewWhenModelUnavailable() {
        localLlmClient.nextSuggestion = LocalLlmSuggestion.unavailable("disabled", "local-model");

        AiQualitySuggestionResponse response = service.generateLocalLlmAiQualitySuggestion(new AiQualitySuggestionRequest(
                "llm-unavailable",
                "wecom_message",
                "msg_160",
                "guest asks for refund after failed service",
                "staff_2"
        ));

        assertThat(response.modelStatus()).isEqualTo("unavailable");
        assertThat(response.manualReviewRequired()).isTrue();
        assertThat(response.result().confidenceScore()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(response.result().summary()).contains("manual review");
        assertThat(response.task().status()).isEqualTo("failed");
        assertThat(analyticsRepository.aiQualityTaskSaveCount).isEqualTo(1);
        assertThat(analyticsRepository.aiQualityResultSaveCount).isEqualTo(1);
    }

    @Test
    void generateLocalLlmAiQualitySuggestionNormalizesUntrustedModelOutputAndRequiresManualReview() {
        localLlmClient.nextSuggestion = LocalLlmSuggestion.completed(
                "local_llm",
                "gemma-local",
                128,
                "model_invented_risk",
                new BigDecimal("1.70"),
                "x".repeat(600),
                -7
        );

        AiQualitySuggestionResponse response = service.generateLocalLlmAiQualitySuggestion(new AiQualitySuggestionRequest(
                "llm-dirty-output",
                "ticket_call",
                "call_dirty",
                "guest asks for manual escalation",
                "staff_3"
        ));

        assertThat(response.modelStatus()).isEqualTo("completed");
        assertThat(response.manualReviewRequired()).isTrue();
        assertThat(response.result().qualityScore()).isEqualTo(100);
        assertThat(response.result().riskLevel()).isEqualTo("suspicious");
        assertThat(response.result().confidenceScore()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(response.result().summary()).hasSize(500);
        assertThat(response.result().findingCount()).isZero();
        assertThat(analyticsRepository.aiQualityTaskSaveCount).isEqualTo(1);
        assertThat(analyticsRepository.aiQualityResultSaveCount).isEqualTo(1);
    }

    private com.complex.platform.analytics.AnalyticsRecords.MetricSnapshotKey metricKey(
            String metricCode,
            String segmentType,
            String segmentId
    ) {
        return new com.complex.platform.analytics.AnalyticsRecords.MetricSnapshotKey(
                metricCode, "test", "daily", LocalDate.parse("2026-06-14"), segmentType, segmentId);
    }

    private RiskAlertRequest riskAlertRequest(
            String idempotencyKey,
            String alertType,
            String riskLevel,
            String objectType,
            String summary
    ) {
        return new RiskAlertRequest(
                idempotencyKey,
                "created by control desk",
                List.of("att_1"),
                "rule_1",
                alertType,
                riskLevel,
                objectType,
                "merchant_1",
                summary
        );
    }

    private StatusChangeRequest resolveRequest(String idempotencyKey) {
        return new StatusChangeRequest(idempotencyKey, "resolved", "handled by operator", List.of("att_2"));
    }

    private RiskAlertRecord resolvedAlert(String id) {
        Instant now = Instant.now(FIXED_CLOCK);
        return new RiskAlertRecord(
                id,
                "rule_1",
                "metric_anomaly",
                "high",
                "merchant",
                "merchant_1",
                "Revenue dropped below threshold",
                "resolved",
                "already handled",
                List.of("att_1"),
                "seed-resolved",
                now.minusSeconds(3600),
                now,
                now
        );
    }

    private void seedReadModels() {
        Instant now = Instant.now(FIXED_CLOCK);
        analyticsRepository.metricDefinitions.add(new MetricDefinitionRecord(
                "gmv_amount_cents",
                "GMV",
                "finance",
                "cent",
                "sum paid order amount",
                "finance_manager",
                "active",
                now,
                now
        ));
        analyticsRepository.dashboardSnapshot = new DashboardSnapshotRecord(
                "p0_overview_today",
                "project_1",
                LocalDate.parse("2026-06-14"),
                128800L,
                23,
                2,
                1,
                Map.of("top_domain", "bar"),
                now
        );
        analyticsRepository.businessMetricSnapshots.add(new BusinessMetricSnapshotRecord(
                "metric_snapshot_1",
                "gmv_amount_cents",
                "finance",
                "daily",
                LocalDate.parse("2026-06-14"),
                "project",
                "project_1",
                new BigDecimal("128800"),
                new BigDecimal("118800"),
                new BigDecimal("8.42"),
                "up",
                "manual_baseline",
                new BigDecimal("135000"),
                new BigDecimal("0.70"),
                Map.of("source", "p2_seed"),
                now
        ));
        analyticsRepository.merchantSummaries.add(new MerchantSummaryRecord(
                "merchant_1",
                "Blue Bar",
                "bar",
                128800L,
                23,
                1,
                "settled",
                LocalDate.parse("2026-06-14"),
                now,
                now
        ));
        analyticsRepository.merchantHealthScores.add(new MerchantHealthScoreRecord(
                "merchant_1",
                86,
                "healthy",
                1,
                0L,
                now,
                "stable",
                now,
                now
        ));
        analyticsRepository.metricAnomalies.add(new MetricAnomalyRecord(
                "anomaly_1",
                "gmv_amount_cents",
                "finance",
                "high",
                new BigDecimal("128800"),
                new BigDecimal("188800"),
                "GMV below expected range",
                "open",
                now,
                null
        ));
        analyticsRepository.riskRules.add(new RiskRuleRecord(
                "rule_1",
                "GMV_DROP",
                "GMV drop",
                "finance",
                "high",
                true,
                Map.of("threshold_percent", 30),
                "notify manager",
                now,
                now
        ));
        RiskAlertRecord riskAlert = new RiskAlertRecord(
                "alert_1",
                "rule_1",
                "metric_anomaly",
                "high",
                "merchant",
                "merchant_1",
                "Revenue dropped below threshold",
                "open",
                "created by control desk",
                List.of("att_1"),
                "seed-alert",
                now,
                null,
                now
        );
        analyticsRepository.riskAlerts.put(riskAlert.id(), riskAlert);
        analyticsRepository.aiQualityTasks.add(new AiQualityTaskRecord(
                "task_1",
                "ticket_call",
                "call_1",
                "manual_review",
                "none",
                "none",
                "completed",
                "staff_1",
                now,
                now
        ));
        analyticsRepository.aiQualityResults.add(new AiQualityResultRecord(
                "result_1",
                "task_1",
                "ticket_call",
                "call_1",
                92,
                "normal",
                new BigDecimal("0.91"),
                "clear service interaction",
                1,
                now
        ));
        analyticsRepository.customerIntents.add(new CustomerIntentRecord(
                "intent_1",
                "ticket_call",
                "call_1",
                "customer_1",
                "refund_request",
                new BigDecimal("0.76"),
                "suspicious",
                "asks for refund",
                now
        ));
        analyticsRepository.robotMetrics.add(new RobotMetricRecord(
                "robot_metric_1",
                "robot_1",
                "mock_provider",
                10,
                9,
                1,
                LocalDate.parse("2026-06-14"),
                "one failed delivery task",
                now
        ));
        analyticsRepository.staffServiceScores.add(new StaffServiceScoreRecord(
                "score_1",
                "staff_1",
                "Alex",
                "frontdesk",
                95,
                0,
                12,
                "normal",
                LocalDate.parse("2026-06-14"),
                now
        ));
    }

    private static final class InMemoryAnalyticsRepository implements AnalyticsRepository {
        private final List<MetricDefinitionRecord> metricDefinitions = new ArrayList<>();
        private DashboardSnapshotRecord dashboardSnapshot;
        private final List<BusinessMetricSnapshotRecord> businessMetricSnapshots = new ArrayList<>();
        private final List<MerchantSummaryRecord> merchantSummaries = new ArrayList<>();
        private final List<MerchantHealthScoreRecord> merchantHealthScores = new ArrayList<>();
        private final List<MetricAnomalyRecord> metricAnomalies = new ArrayList<>();
        private final List<RiskRuleRecord> riskRules = new ArrayList<>();
        private final Map<String, RiskAlertRecord> riskAlerts = new LinkedHashMap<>();
        private final List<AiQualityTaskRecord> aiQualityTasks = new ArrayList<>();
        private final List<AiQualityResultRecord> aiQualityResults = new ArrayList<>();
        private final List<CustomerIntentRecord> customerIntents = new ArrayList<>();
        private final List<RobotMetricRecord> robotMetrics = new ArrayList<>();
        private final List<StaffServiceScoreRecord> staffServiceScores = new ArrayList<>();
        private boolean failAiQualityResults;
        private int riskAlertSaveCount;
        private int riskAlertUpdateCount;
        private int aiQualityTaskSaveCount;
        private int aiQualityResultSaveCount;

        @Override
        public List<AiQualityResultRecord> listAiQualityResults() {
            if (failAiQualityResults) {
                throw new IllegalStateException("analytics store unavailable");
            }
            return aiQualityResults;
        }

        @Override
        public List<AiQualityTaskRecord> listAiQualityTasks() {
            return aiQualityTasks;
        }

        @Override
        public AiQualityTaskRecord saveAiQualityTask(AiQualityTaskRecord task) {
            aiQualityTaskSaveCount += 1;
            aiQualityTasks.add(0, task);
            return task;
        }

        @Override
        public AiQualityResultRecord saveAiQualityResult(AiQualityResultRecord result) {
            aiQualityResultSaveCount += 1;
            aiQualityResults.add(0, result);
            return result;
        }

        @Override
        public List<CustomerIntentRecord> listCustomerIntents() {
            return customerIntents;
        }

        @Override
        public DashboardSnapshotRecord getP0OverviewDashboard() {
            return dashboardSnapshot;
        }

        @Override
        public List<BusinessMetricSnapshotRecord> listBusinessMetricSnapshots() {
            return businessMetricSnapshots;
        }

        @Override
        public List<MerchantHealthScoreRecord> listMerchantHealthScores() {
            return merchantHealthScores;
        }

        @Override
        public List<MerchantSummaryRecord> listAnalyticsMerchants() {
            return merchantSummaries;
        }

        @Override
        public List<MetricAnomalyRecord> listMetricAnomalies() {
            return metricAnomalies;
        }

        @Override
        public List<MetricDefinitionRecord> listMetricDefinitions() {
            return metricDefinitions;
        }

        @Override
        public List<RiskAlertRecord> listRiskAlerts() {
            return new ArrayList<>(riskAlerts.values());
        }

        @Override
        public RiskAlertRecord saveRiskAlert(RiskAlertRecord alert) {
            riskAlertSaveCount += 1;
            riskAlerts.put(alert.id(), alert);
            return alert;
        }

        @Override
        public Optional<RiskAlertRecord> findRiskAlertById(String id) {
            return Optional.ofNullable(riskAlerts.get(id));
        }

        @Override
        public RiskAlertRecord updateRiskAlert(RiskAlertRecord alert) {
            riskAlertUpdateCount += 1;
            riskAlerts.put(alert.id(), alert);
            return alert;
        }

        @Override
        public List<RiskRuleRecord> listRiskRules() {
            return riskRules;
        }

        @Override
        public List<RobotMetricRecord> listRobotMetrics() {
            return robotMetrics;
        }

        @Override
        public List<StaffServiceScoreRecord> listStaffServiceScores() {
            return staffServiceScores;
        }

        // --- T12 看板真实聚合 / 行级隔离（内存实现，用 metricSnapshots 模拟 ETL 落地的快照） ---
        // key = scope|metricKey（scope=null 用 "__platform__"），value = actual_value。
        private final Map<String, Map<String, BigDecimal>> metricSnapshots = new LinkedHashMap<>();
        private LocalDate etlBusinessDate;

        @Override
        public Map<String, BigDecimal> readMetricSnapshots(LocalDate businessDate, String scope) {
            String key = scope == null ? "__platform__" : scope;
            return new LinkedHashMap<>(metricSnapshots.getOrDefault(key, Map.of()));
        }

        @Override
        public Optional<LocalDate> latestEtlBusinessDate(String scope) {
            return Optional.ofNullable(etlBusinessDate);
        }

        @Override
        public void upsertMetricSnapshot(
                com.complex.platform.analytics.AnalyticsRecords.MetricSnapshotKey snapshotKey,
                BigDecimal actualValue,
                String projectionMethod,
                Instant createdAt
        ) {
            etlBusinessDate = snapshotKey.businessDate();
            String scope = "merchant".equals(snapshotKey.segmentType()) ? snapshotKey.segmentId() : "__platform__";
            metricSnapshots.computeIfAbsent(scope, ignored -> new LinkedHashMap<>())
                    .put(snapshotKey.metricKey(), actualValue);
        }

        @Override
        public BigDecimal sumTicketRevenueCents(LocalDate businessDate) {
            return BigDecimal.ZERO;
        }

        @Override
        public Map<String, BigDecimal> sumBarRevenueCentsByMerchant(LocalDate businessDate) {
            return Map.of();
        }

        @Override
        public Map<String, BigDecimal> sumTicketBarShareCentsByMerchant(LocalDate businessDate) {
            return Map.of();
        }

        @Override
        public Map<String, BigDecimal> sumTicketAmusementShareCentsByMerchant(LocalDate businessDate) {
            return Map.of();
        }

        @Override
        public BigDecimal countAmusementVerificationSuccess(LocalDate businessDate) {
            return BigDecimal.ZERO;
        }

        @Override
        public Map<String, BigDecimal> countTicketVerificationSuccessByMerchant(LocalDate businessDate) {
            return Map.of();
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
    }



    private static final class CapturingLocalLlmClient implements LocalLlmClient {
        private String lastSourceText;
        private LocalLlmSuggestion nextSuggestion = LocalLlmSuggestion.unavailable("disabled", "local-model");

        @Override
        public LocalLlmSuggestion suggestQuality(String sourceType, String sourceId, String redactedText) {
            lastSourceText = redactedText;
            return nextSuggestion;
        }
    }

    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String key = record.operationId() + ":" + record.idempotencyKey();
            if (records.containsKey(key)) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(record.operationId() + ":" + record.idempotencyKey(), record);
            return record;
        }
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }
}
