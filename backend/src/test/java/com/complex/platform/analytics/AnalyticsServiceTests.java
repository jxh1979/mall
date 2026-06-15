package com.complex.platform.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.complex.platform.analytics.AnalyticsRecords.AiQualityResultRecord;
import com.complex.platform.analytics.AnalyticsRecords.AiQualityTaskRecord;
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
    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        analyticsRepository = new InMemoryAnalyticsRepository();
        idempotencyRepository = new InMemoryIdempotencyRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        seedReadModels();
        service = new AnalyticsService(
                analyticsRepository,
                new IdempotencyService(idempotencyRepository, new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                FIXED_CLOCK
        );
    }

    @Test
    void readEndpointsReturnRepositoryDataAndRecordExactOpenApiAuditEvents() {
        assertThat(service.listAiQualityResults().items()).extracting(AiQualityResultRecord::resultId).contains("result_1");
        assertThat(service.listAiQualityTasks().items()).extracting(AiQualityTaskRecord::taskId).contains("task_1");
        assertThat(service.listCustomerIntents().items()).extracting(CustomerIntentRecord::intentId).contains("intent_1");
        assertThat(service.getP0OverviewDashboard().snapshotKey()).isEqualTo("p0_overview_today");
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
        public List<CustomerIntentRecord> listCustomerIntents() {
            return customerIntents;
        }

        @Override
        public DashboardSnapshotRecord getP0OverviewDashboard() {
            return dashboardSnapshot;
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
