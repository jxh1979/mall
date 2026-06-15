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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAnalyticsRepository implements AnalyticsRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcAnalyticsRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public List<AiQualityResultRecord> listAiQualityResults() {
        return jdbcTemplate.query("""
                SELECT result_id, task_id, source_type, source_id, quality_score, risk_level,
                       confidence_score, summary, finding_count, created_at
                  FROM analytics_ai_quality_results
                 ORDER BY created_at DESC, result_id
                """, this::mapAiQualityResult);
    }

    @Override
    public List<AiQualityTaskRecord> listAiQualityTasks() {
        return jdbcTemplate.query("""
                SELECT task_id, source_type, source_id, processing_mode, model_provider,
                       model_version, status, requested_by, created_at, completed_at
                  FROM analytics_ai_quality_tasks
                 ORDER BY created_at DESC, task_id
                """, this::mapAiQualityTask);
    }

    @Override
    public AiQualityTaskRecord saveAiQualityTask(AiQualityTaskRecord task) {
        jdbcTemplate.update("""
                INSERT INTO analytics_ai_quality_tasks (
                    task_id, source_type, source_id, processing_mode, model_provider,
                    model_version, status, requested_by, created_at, completed_at
                )
                VALUES (
                    :task_id, :source_type, :source_id, :processing_mode, :model_provider,
                    :model_version, :status, :requested_by, :created_at, :completed_at
                )
                ON CONFLICT (task_id) DO UPDATE SET
                    source_type = EXCLUDED.source_type,
                    source_id = EXCLUDED.source_id,
                    processing_mode = EXCLUDED.processing_mode,
                    model_provider = EXCLUDED.model_provider,
                    model_version = EXCLUDED.model_version,
                    status = EXCLUDED.status,
                    requested_by = EXCLUDED.requested_by,
                    completed_at = EXCLUDED.completed_at
                """, aiQualityTaskParameters(task));
        return task;
    }

    @Override
    public AiQualityResultRecord saveAiQualityResult(AiQualityResultRecord result) {
        jdbcTemplate.update("""
                INSERT INTO analytics_ai_quality_results (
                    result_id, task_id, source_type, source_id, quality_score,
                    risk_level, confidence_score, summary, finding_count, created_at
                )
                VALUES (
                    :result_id, :task_id, :source_type, :source_id, :quality_score,
                    :risk_level, :confidence_score, :summary, :finding_count, :created_at
                )
                ON CONFLICT (result_id) DO UPDATE SET
                    task_id = EXCLUDED.task_id,
                    source_type = EXCLUDED.source_type,
                    source_id = EXCLUDED.source_id,
                    quality_score = EXCLUDED.quality_score,
                    risk_level = EXCLUDED.risk_level,
                    confidence_score = EXCLUDED.confidence_score,
                    summary = EXCLUDED.summary,
                    finding_count = EXCLUDED.finding_count
                """, aiQualityResultParameters(result));
        return result;
    }

    @Override
    public List<CustomerIntentRecord> listCustomerIntents() {
        return jdbcTemplate.query("""
                SELECT intent_id, source_type, source_id, customer_ref, intent_type,
                       confidence_score, risk_level, summary, created_at
                  FROM analytics_customer_intents
                 ORDER BY created_at DESC, intent_id
                """, this::mapCustomerIntent);
    }

    @Override
    public DashboardSnapshotRecord getP0OverviewDashboard() {
        return jdbcTemplate.query("""
                SELECT snapshot_key, project_id, business_date, revenue_amount_cents,
                       order_count, risk_alert_count, critical_risk_count, payload_json, created_at
                  FROM analytics_dashboard_snapshots
                 ORDER BY business_date DESC, created_at DESC, snapshot_key
                 LIMIT 1
                """, this::mapDashboardSnapshot).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("P0 overview dashboard snapshot is not seeded"));
    }


    @Override
    public List<BusinessMetricSnapshotRecord> listBusinessMetricSnapshots() {
        return jdbcTemplate.query("""
                SELECT snapshot_id, metric_key, domain, period_type, business_date, segment_type,
                       segment_id, actual_value, baseline_value, variance_rate, trend_direction,
                       projection_method, projected_value, confidence_score, payload_json, created_at
                  FROM analytics_business_metric_snapshots
                 ORDER BY business_date DESC, domain, metric_key, segment_type, segment_id NULLS FIRST
                """, this::mapBusinessMetricSnapshot);
    }

    @Override
    public List<MerchantHealthScoreRecord> listMerchantHealthScores() {
        return jdbcTemplate.query("""
                SELECT merchant_id, score, score_level, risk_count, overdue_ar_amount_cents,
                       last_reviewed_at, summary, created_at, updated_at
                  FROM analytics_merchant_health_scores
                 ORDER BY score ASC, merchant_id
                """, this::mapMerchantHealthScore);
    }

    @Override
    public List<MerchantSummaryRecord> listAnalyticsMerchants() {
        return jdbcTemplate.query("""
                SELECT merchant_id, merchant_name, merchant_type, revenue_amount_cents,
                       order_count, risk_alert_count, settlement_status, business_date,
                       created_at, updated_at
                  FROM analytics_merchant_summaries
                 ORDER BY business_date DESC, revenue_amount_cents DESC, merchant_id
                """, this::mapMerchantSummary);
    }

    @Override
    public List<MetricAnomalyRecord> listMetricAnomalies() {
        return jdbcTemplate.query("""
                SELECT id, metric_key, domain, risk_level, observed_value, expected_value,
                       summary, status, detected_at, resolved_at
                  FROM analytics_metric_anomalies
                 ORDER BY detected_at DESC, id
                """, this::mapMetricAnomaly);
    }

    @Override
    public List<MetricDefinitionRecord> listMetricDefinitions() {
        return jdbcTemplate.query("""
                SELECT metric_key, name, domain, unit, calculation_note, owner_role,
                       status, created_at, updated_at
                  FROM analytics_metric_definitions
                 ORDER BY domain, metric_key
                """, this::mapMetricDefinition);
    }

    @Override
    public List<RiskAlertRecord> listRiskAlerts() {
        return jdbcTemplate.query("""
                SELECT id, rule_id, alert_type, risk_level, object_type, object_id, summary,
                       status, operator_note, evidence_attachment_ids_json, idempotency_key,
                       created_at, resolved_at, updated_at
                  FROM analytics_risk_alerts
                 ORDER BY created_at DESC, id
                """, this::mapRiskAlert);
    }

    @Override
    public RiskAlertRecord saveRiskAlert(RiskAlertRecord alert) {
        jdbcTemplate.update("""
                INSERT INTO analytics_risk_alerts (
                    id, rule_id, alert_type, risk_level, object_type, object_id, summary,
                    status, operator_note, evidence_attachment_ids_json, idempotency_key,
                    created_at, resolved_at, updated_at
                )
                VALUES (
                    :id, :rule_id, :alert_type, :risk_level, :object_type, :object_id, :summary,
                    :status, :operator_note, CAST(:evidence_attachment_ids_json AS jsonb), :idempotency_key,
                    :created_at, :resolved_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    rule_id = EXCLUDED.rule_id,
                    alert_type = EXCLUDED.alert_type,
                    risk_level = EXCLUDED.risk_level,
                    object_type = EXCLUDED.object_type,
                    object_id = EXCLUDED.object_id,
                    summary = EXCLUDED.summary,
                    status = EXCLUDED.status,
                    operator_note = EXCLUDED.operator_note,
                    evidence_attachment_ids_json = EXCLUDED.evidence_attachment_ids_json,
                    idempotency_key = EXCLUDED.idempotency_key,
                    resolved_at = EXCLUDED.resolved_at,
                    updated_at = EXCLUDED.updated_at
                """, riskAlertParameters(alert));
        return alert;
    }

    @Override
    public Optional<RiskAlertRecord> findRiskAlertById(String id) {
        return jdbcTemplate.query("""
                SELECT id, rule_id, alert_type, risk_level, object_type, object_id, summary,
                       status, operator_note, evidence_attachment_ids_json, idempotency_key,
                       created_at, resolved_at, updated_at
                  FROM analytics_risk_alerts
                 WHERE id = :id
                """, Map.of("id", id), this::mapRiskAlert).stream().findFirst();
    }

    @Override
    public RiskAlertRecord updateRiskAlert(RiskAlertRecord alert) {
        jdbcTemplate.update("""
                UPDATE analytics_risk_alerts
                   SET rule_id = :rule_id,
                       alert_type = :alert_type,
                       risk_level = :risk_level,
                       object_type = :object_type,
                       object_id = :object_id,
                       summary = :summary,
                       status = :status,
                       operator_note = :operator_note,
                       evidence_attachment_ids_json = CAST(:evidence_attachment_ids_json AS jsonb),
                       idempotency_key = :idempotency_key,
                       resolved_at = :resolved_at,
                       updated_at = :updated_at
                 WHERE id = :id
                """, riskAlertParameters(alert));
        return alert;
    }

    @Override
    public List<RiskRuleRecord> listRiskRules() {
        return jdbcTemplate.query("""
                SELECT rule_id, rule_code, rule_name, domain, risk_level, enabled,
                       rule_config_json, action_note, created_at, updated_at
                  FROM analytics_risk_rules
                 ORDER BY enabled DESC, domain, rule_code
                """, this::mapRiskRule);
    }

    @Override
    public List<RobotMetricRecord> listRobotMetrics() {
        return jdbcTemplate.query("""
                SELECT metric_id, robot_id, provider_code, task_count, success_count,
                       failure_count, business_date, summary, created_at
                  FROM analytics_robot_metrics
                 ORDER BY business_date DESC, robot_id
                """, this::mapRobotMetric);
    }

    @Override
    public List<StaffServiceScoreRecord> listStaffServiceScores() {
        return jdbcTemplate.query("""
                SELECT score_id, staff_id, staff_name, role_code, service_score,
                       complaint_count, positive_event_count, risk_level, business_date, created_at
                  FROM analytics_staff_service_scores
                 ORDER BY business_date DESC, service_score ASC, staff_id
                """, this::mapStaffServiceScore);
    }

    private MapSqlParameterSource aiQualityTaskParameters(AiQualityTaskRecord task) {
        return new MapSqlParameterSource()
                .addValue("task_id", task.taskId())
                .addValue("source_type", task.sourceType())
                .addValue("source_id", task.sourceId())
                .addValue("processing_mode", task.processingMode())
                .addValue("model_provider", task.modelProvider())
                .addValue("model_version", task.modelVersion())
                .addValue("status", task.status())
                .addValue("requested_by", task.requestedBy())
                .addValue("created_at", timestampWithTimeZone(task.createdAt()))
                .addValue("completed_at", timestampWithTimeZone(task.completedAt()));
    }

    private MapSqlParameterSource aiQualityResultParameters(AiQualityResultRecord result) {
        return new MapSqlParameterSource()
                .addValue("result_id", result.resultId())
                .addValue("task_id", result.taskId())
                .addValue("source_type", result.sourceType())
                .addValue("source_id", result.sourceId())
                .addValue("quality_score", result.qualityScore())
                .addValue("risk_level", result.riskLevel())
                .addValue("confidence_score", result.confidenceScore())
                .addValue("summary", result.summary())
                .addValue("finding_count", result.findingCount())
                .addValue("created_at", timestampWithTimeZone(result.createdAt()));
    }

    private MapSqlParameterSource riskAlertParameters(RiskAlertRecord alert) {
        return new MapSqlParameterSource()
                .addValue("id", alert.id())
                .addValue("rule_id", alert.ruleId())
                .addValue("alert_type", alert.alertType())
                .addValue("risk_level", alert.riskLevel())
                .addValue("object_type", alert.objectType())
                .addValue("object_id", alert.objectId())
                .addValue("summary", alert.summary())
                .addValue("status", alert.status())
                .addValue("operator_note", alert.operatorNote())
                .addValue("evidence_attachment_ids_json", writeJson(alert.evidenceAttachmentIds()))
                .addValue("idempotency_key", alert.idempotencyKey())
                .addValue("created_at", timestampWithTimeZone(alert.createdAt()))
                .addValue("resolved_at", timestampWithTimeZone(alert.resolvedAt()))
                .addValue("updated_at", timestampWithTimeZone(alert.updatedAt()));
    }

    private MetricDefinitionRecord mapMetricDefinition(ResultSet resultSet, int rowNumber) throws SQLException {
        return new MetricDefinitionRecord(
                resultSet.getString("metric_key"),
                resultSet.getString("name"),
                resultSet.getString("domain"),
                resultSet.getString("unit"),
                resultSet.getString("calculation_note"),
                resultSet.getString("owner_role"),
                resultSet.getString("status"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    private DashboardSnapshotRecord mapDashboardSnapshot(ResultSet resultSet, int rowNumber) throws SQLException {
        return new DashboardSnapshotRecord(
                resultSet.getString("snapshot_key"),
                resultSet.getString("project_id"),
                resultSet.getObject("business_date", java.time.LocalDate.class),
                resultSet.getLong("revenue_amount_cents"),
                resultSet.getInt("order_count"),
                resultSet.getInt("risk_alert_count"),
                resultSet.getInt("critical_risk_count"),
                readJson(resultSet.getString("payload_json"), MAP_TYPE),
                instant(resultSet, "created_at")
        );
    }


    private BusinessMetricSnapshotRecord mapBusinessMetricSnapshot(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BusinessMetricSnapshotRecord(
                resultSet.getString("snapshot_id"),
                resultSet.getString("metric_key"),
                resultSet.getString("domain"),
                resultSet.getString("period_type"),
                resultSet.getObject("business_date", java.time.LocalDate.class),
                resultSet.getString("segment_type"),
                resultSet.getString("segment_id"),
                resultSet.getBigDecimal("actual_value"),
                resultSet.getBigDecimal("baseline_value"),
                resultSet.getBigDecimal("variance_rate"),
                resultSet.getString("trend_direction"),
                resultSet.getString("projection_method"),
                resultSet.getBigDecimal("projected_value"),
                resultSet.getBigDecimal("confidence_score"),
                readJson(resultSet.getString("payload_json"), MAP_TYPE),
                instant(resultSet, "created_at")
        );
    }

    private MerchantSummaryRecord mapMerchantSummary(ResultSet resultSet, int rowNumber) throws SQLException {
        return new MerchantSummaryRecord(
                resultSet.getString("merchant_id"),
                resultSet.getString("merchant_name"),
                resultSet.getString("merchant_type"),
                resultSet.getLong("revenue_amount_cents"),
                resultSet.getInt("order_count"),
                resultSet.getInt("risk_alert_count"),
                resultSet.getString("settlement_status"),
                resultSet.getObject("business_date", java.time.LocalDate.class),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    private MerchantHealthScoreRecord mapMerchantHealthScore(ResultSet resultSet, int rowNumber) throws SQLException {
        return new MerchantHealthScoreRecord(
                resultSet.getString("merchant_id"),
                resultSet.getInt("score"),
                resultSet.getString("score_level"),
                resultSet.getInt("risk_count"),
                resultSet.getLong("overdue_ar_amount_cents"),
                instant(resultSet, "last_reviewed_at"),
                resultSet.getString("summary"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    private MetricAnomalyRecord mapMetricAnomaly(ResultSet resultSet, int rowNumber) throws SQLException {
        return new MetricAnomalyRecord(
                resultSet.getString("id"),
                resultSet.getString("metric_key"),
                resultSet.getString("domain"),
                resultSet.getString("risk_level"),
                resultSet.getBigDecimal("observed_value"),
                resultSet.getBigDecimal("expected_value"),
                resultSet.getString("summary"),
                resultSet.getString("status"),
                instant(resultSet, "detected_at"),
                instant(resultSet, "resolved_at")
        );
    }

    private RiskRuleRecord mapRiskRule(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RiskRuleRecord(
                resultSet.getString("rule_id"),
                resultSet.getString("rule_code"),
                resultSet.getString("rule_name"),
                resultSet.getString("domain"),
                resultSet.getString("risk_level"),
                resultSet.getBoolean("enabled"),
                readJson(resultSet.getString("rule_config_json"), MAP_TYPE),
                resultSet.getString("action_note"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }

    private RiskAlertRecord mapRiskAlert(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RiskAlertRecord(
                resultSet.getString("id"),
                resultSet.getString("rule_id"),
                resultSet.getString("alert_type"),
                resultSet.getString("risk_level"),
                resultSet.getString("object_type"),
                resultSet.getString("object_id"),
                resultSet.getString("summary"),
                resultSet.getString("status"),
                resultSet.getString("operator_note"),
                readJson(resultSet.getString("evidence_attachment_ids_json"), STRING_LIST_TYPE),
                resultSet.getString("idempotency_key"),
                instant(resultSet, "created_at"),
                instant(resultSet, "resolved_at"),
                instant(resultSet, "updated_at")
        );
    }

    private AiQualityTaskRecord mapAiQualityTask(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AiQualityTaskRecord(
                resultSet.getString("task_id"),
                resultSet.getString("source_type"),
                resultSet.getString("source_id"),
                resultSet.getString("processing_mode"),
                resultSet.getString("model_provider"),
                resultSet.getString("model_version"),
                resultSet.getString("status"),
                resultSet.getString("requested_by"),
                instant(resultSet, "created_at"),
                instant(resultSet, "completed_at")
        );
    }

    private AiQualityResultRecord mapAiQualityResult(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AiQualityResultRecord(
                resultSet.getString("result_id"),
                resultSet.getString("task_id"),
                resultSet.getString("source_type"),
                resultSet.getString("source_id"),
                resultSet.getInt("quality_score"),
                resultSet.getString("risk_level"),
                resultSet.getBigDecimal("confidence_score"),
                resultSet.getString("summary"),
                resultSet.getInt("finding_count"),
                instant(resultSet, "created_at")
        );
    }

    private CustomerIntentRecord mapCustomerIntent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CustomerIntentRecord(
                resultSet.getString("intent_id"),
                resultSet.getString("source_type"),
                resultSet.getString("source_id"),
                resultSet.getString("customer_ref"),
                resultSet.getString("intent_type"),
                resultSet.getBigDecimal("confidence_score"),
                resultSet.getString("risk_level"),
                resultSet.getString("summary"),
                instant(resultSet, "created_at")
        );
    }

    private RobotMetricRecord mapRobotMetric(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RobotMetricRecord(
                resultSet.getString("metric_id"),
                resultSet.getString("robot_id"),
                resultSet.getString("provider_code"),
                resultSet.getInt("task_count"),
                resultSet.getInt("success_count"),
                resultSet.getInt("failure_count"),
                resultSet.getObject("business_date", java.time.LocalDate.class),
                resultSet.getString("summary"),
                instant(resultSet, "created_at")
        );
    }

    private StaffServiceScoreRecord mapStaffServiceScore(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StaffServiceScoreRecord(
                resultSet.getString("score_id"),
                resultSet.getString("staff_id"),
                resultSet.getString("staff_name"),
                resultSet.getString("role_code"),
                resultSet.getInt("service_score"),
                resultSet.getInt("complaint_count"),
                resultSet.getInt("positive_event_count"),
                resultSet.getString("risk_level"),
                resultSet.getObject("business_date", java.time.LocalDate.class),
                instant(resultSet, "created_at")
        );
    }

    private Instant instant(ResultSet resultSet, String column) throws SQLException {
        java.sql.Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private OffsetDateTime timestampWithTimeZone(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("analytics payload is not serializable", exception);
        }
    }

    private <T> T readJson(String value, TypeReference<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("analytics payload is unreadable", exception);
        }
    }
}
