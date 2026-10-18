package com.complex.platform.analytics;
// 文件版本：V0.03
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.contract.SpaceRentalDerivationSql;
import com.complex.platform.analytics.AnalyticsRecords.AiQualityResultRecord;
import com.complex.platform.analytics.AnalyticsRecords.AiQualityTaskRecord;
import com.complex.platform.analytics.AnalyticsRecords.BusinessMetricSnapshotRecord;
import com.complex.platform.analytics.AnalyticsRecords.CustomerIntentRecord;
import com.complex.platform.analytics.AnalyticsRecords.DashboardSnapshotRecord;
import com.complex.platform.analytics.AnalyticsRecords.MerchantHealthScoreRecord;
import com.complex.platform.analytics.AnalyticsRecords.MerchantSummaryRecord;
import com.complex.platform.analytics.AnalyticsRecords.MetricAnomalyRecord;
import com.complex.platform.analytics.AnalyticsRecords.MetricDefinitionRecord;
import com.complex.platform.analytics.AnalyticsRecords.BusinessReviewLockRecord;
import com.complex.platform.analytics.AnalyticsRecords.RiskAlertRecord;
import com.complex.platform.analytics.AnalyticsRecords.RiskRuleRecord;
import com.complex.platform.analytics.AnalyticsRecords.RobotMetricRecord;
import com.complex.platform.analytics.AnalyticsRecords.StaffServiceScoreRecord;
import com.complex.platform.analytics.AnalyticsRecords.MetricSnapshotKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAnalyticsRepository implements AnalyticsRepository {

    @org.springframework.beans.factory.annotation.Autowired
    private AmusementBusinessDayAnalytics amusementDays;

    @Override
    public AnalyticsRecords.MetricDetailRows amusementComplexDetail(String key, LocalDate from, LocalDate to, Instant now,
            boolean allHistory, int limit, int offset, java.util.Collection<String> scope) {
        return amusementDays.detail(key,from,to,now,allHistory,limit,offset,scope);
    }


    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final String METRIC_ANOMALY_SELECT = """
            SELECT id, metric_key, domain, risk_level, observed_value, expected_value,
                   summary, status, detected_at, resolved_at,
                   disposition_note, disposed_by, disposed_at, escalated_alert_id
              FROM analytics_metric_anomalies
            """;
    private static final String SAVED_BOTTLE_SELECT = """
            SELECT id, bottle_name, initial_quantity, remaining_quantity, unit, status, created_at, expire_at
              FROM saved_bottles
            """;
    // M737「在这些项目经营的商户」只有这一个定义，各处共用（V441 表注释：「商户经营项目＝merchants.project_id（主项目）加本表；
    // 本表只放主项目以外追加的项目」）。参数 :project_ids 非空集合。
    private static final String MERCHANTS_OPERATING_IN_PROJECTS = """
            SELECT m.id FROM merchants m
             WHERE m.project_id IN (:project_ids)
                OR m.id IN (SELECT mp.merchant_id FROM merchant_projects mp WHERE mp.project_id IN (:project_ids))
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final com.complex.platform.common.BusinessDayResolver businessDayResolver;

    JdbcAnalyticsRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            com.complex.platform.common.BusinessDayResolver businessDayResolver) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.businessDayResolver = businessDayResolver;
    }

    @Override
    public Map<String, String> projectBusinessEntityMap() {
        Map<String, String> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT p.id AS project_id, m.business_entity_id AS business_entity_id
                  FROM projects p
                  JOIN merchants m ON m.id = p.merchant_id
                 WHERE m.business_entity_id IS NOT NULL
                 ORDER BY p.id
                """, new MapSqlParameterSource(), (org.springframework.jdbc.core.RowCallbackHandler) resultSet ->
                result.put(resultSet.getString("project_id"), resultSet.getString("business_entity_id"))
        );
        return result;
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
    public List<MerchantHealthScoreRecord> listMerchantHealthScoresByMerchant(String merchantId) {
        // 商户面 scoped 读通道：按 analytics_merchant_health_scores.merchant_id = token scope 过滤（AC-ISO-1，§4）。
        if (merchantId == null) return listMerchantHealthScores();
        return jdbcTemplate.query("""
                SELECT merchant_id, score, score_level, risk_count, overdue_ar_amount_cents,
                       last_reviewed_at, summary, created_at, updated_at
                  FROM analytics_merchant_health_scores
                 WHERE merchant_id = :merchant_id
                 ORDER BY score ASC, merchant_id
                """, Map.of("merchant_id", merchantId), this::mapMerchantHealthScore);
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
    public List<MerchantSummaryRecord> listAnalyticsMerchantsByMerchant(String merchantId) {
        // 商户面 scoped 读通道：按 analytics_merchant_summaries.merchant_id = token scope 过滤（AC-ISO-1，§4）。
        if (merchantId == null) return listAnalyticsMerchants();
        return jdbcTemplate.query("""
                SELECT merchant_id, merchant_name, merchant_type, revenue_amount_cents,
                       order_count, risk_alert_count, settlement_status, business_date,
                       created_at, updated_at
                  FROM analytics_merchant_summaries
                 WHERE merchant_id = :merchant_id
                 ORDER BY business_date DESC, revenue_amount_cents DESC, merchant_id
                """, Map.of("merchant_id", merchantId), this::mapMerchantSummary);
    }

    @Override
    public java.util.Set<String> listMerchantIdsOperatingIn(java.util.Collection<String> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return java.util.Set.of();
        }
        return new java.util.HashSet<>(jdbcTemplate.queryForList(
                MERCHANTS_OPERATING_IN_PROJECTS, Map.of("project_ids", projectIds), String.class));
    }

    @Override
    public List<MetricAnomalyRecord> listMetricAnomalies() {
        return jdbcTemplate.query(METRIC_ANOMALY_SELECT + """
                 ORDER BY detected_at DESC, id
                """, this::mapMetricAnomaly);
    }

    @Override
    public java.util.Optional<MetricAnomalyRecord> findMetricAnomalyById(String id) {
        return jdbcTemplate.query(METRIC_ANOMALY_SELECT + """
                 WHERE id = :id
                """, new MapSqlParameterSource("id", id), this::mapMetricAnomaly)
                .stream().findFirst();
    }

    @Override
    public java.util.Optional<MetricAnomalyRecord> findMetricAnomalyByIdForUpdate(String id) {
        return jdbcTemplate.query(METRIC_ANOMALY_SELECT + """
                 WHERE id = :id
                 FOR UPDATE
                """, new MapSqlParameterSource("id", id), this::mapMetricAnomaly)
                .stream().findFirst();
    }

    @Override
    public MetricAnomalyRecord updateMetricAnomalyDisposition(String id, String status, String note,
            String disposedBy, java.time.Instant disposedAt, String escalatedAlertId,
            java.time.Instant resolvedAt) {
        jdbcTemplate.update("""
                UPDATE analytics_metric_anomalies
                   SET status = :status,
                       disposition_note = :note,
                       disposed_by = :disposed_by,
                       disposed_at = :disposed_at,
                       escalated_alert_id = :escalated_alert_id,
                       resolved_at = :resolved_at
                 WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", status)
                .addValue("note", note)
                .addValue("disposed_by", disposedBy)
                .addValue("disposed_at", timestampWithTimeZone(disposedAt))
                .addValue("escalated_alert_id", escalatedAlertId)
                .addValue("resolved_at", timestampWithTimeZone(resolvedAt)));
        return findMetricAnomalyById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "metric anomaly disappeared after disposition: " + id));
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

    // --- T12 看板真实聚合 / 行级隔离 ---

    @Override
    public Map<String, BigDecimal> readMetricSnapshots(LocalDate businessDate, String scope) {
        String segmentType = scope == null ? "platform" : "merchant";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("business_date", businessDate)
                .addValue("segment_type", segmentType)
                .addValue("segment_id", scope);
        String segmentFilter = scope == null ? "segment_id IS NULL" : "segment_id = :segment_id";
        return keyTotals("""
                SELECT metric_key, actual_value
                  FROM analytics_business_metric_snapshots
                 WHERE business_date = :business_date
                   AND period_type = 'daily'
                   AND segment_type = :segment_type
                   AND %s
                """.formatted(segmentFilter), params);
    }

    @Override
    public Optional<LocalDate> latestEtlBusinessDate(String scope) {
        String segmentType = scope == null ? "platform" : "merchant";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("segment_type", segmentType)
                .addValue("segment_id", scope);
        String segmentFilter = scope == null ? "segment_id IS NULL" : "segment_id = :segment_id";
        LocalDate latest = jdbcTemplate.query("""
                SELECT MAX(business_date) AS latest
                  FROM analytics_business_metric_snapshots
                 WHERE period_type = 'daily'
                   AND projection_method = 'etl_aggregate'
                   AND segment_type = :segment_type
                   AND %s
                """.formatted(segmentFilter), params, resultSet ->
                resultSet.next() ? resultSet.getObject("latest", LocalDate.class) : null
        );
        return Optional.ofNullable(latest);
    }

    @Override
    public Optional<OperatingParamsRecord> findOperatingParams(String projectId) {
        List<OperatingParamsRecord> rows = jdbcTemplate.query("""
                SELECT project_id, operating_area_sqm, status, created_at, updated_at
                  FROM complex_operating_params
                 WHERE project_id = :project_id
                """, Map.of("project_id", projectId), (rs, n) -> mapOperatingParams(rs));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public List<OperatingParamsRecord> listOperatingParams() {
        return jdbcTemplate.query("""
                SELECT project_id, operating_area_sqm, status, created_at, updated_at
                  FROM complex_operating_params
                 ORDER BY project_id
                """, (rs, n) -> mapOperatingParams(rs));
    }

    @Override
    public OperatingParamsRecord upsertOperatingParams(OperatingParamsRecord record) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("project_id", record.projectId())
                .addValue("operating_area_sqm", record.operatingAreaSqm())
                .addValue("status", record.status())
                .addValue("now", timestampWithTimeZone(record.updatedAt()));
        jdbcTemplate.update("""
                INSERT INTO complex_operating_params (project_id, operating_area_sqm, status, created_at, updated_at)
                VALUES (:project_id, :operating_area_sqm, :status, :now, :now)
                ON CONFLICT (project_id) DO UPDATE SET
                    operating_area_sqm = EXCLUDED.operating_area_sqm,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, params);
        return findOperatingParams(record.projectId()).orElse(record);
    }

    private OperatingParamsRecord mapOperatingParams(ResultSet rs) throws SQLException {
        return new OperatingParamsRecord(
                rs.getString("project_id"),
                rs.getBigDecimal("operating_area_sqm"),
                rs.getString("status"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }

    @Override
    public void upsertMetricSnapshot(
            MetricSnapshotKey key,
            BigDecimal actualValue,
            String projectionMethod,
            Instant createdAt
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("snapshot_id", "etl_" + UUID.randomUUID())
                .addValue("metric_key", key.metricKey())
                .addValue("domain", key.domain())
                .addValue("period_type", key.periodType())
                .addValue("business_date", key.businessDate())
                .addValue("segment_type", key.segmentType())
                .addValue("segment_id", key.segmentId())
                .addValue("actual_value", actualValue)
                .addValue("projection_method", projectionMethod)
                .addValue("created_at", timestampWithTimeZone(createdAt));
        // 与 V070 唯一索引 (metric_key, period_type, business_date, segment_type, COALESCE(segment_id,'__platform__')) 对齐。
        jdbcTemplate.update("""
                INSERT INTO analytics_business_metric_snapshots (
                    snapshot_id, metric_key, domain, period_type, business_date, segment_type, segment_id,
                    actual_value, trend_direction, projection_method, payload_json, created_at
                )
                VALUES (
                    :snapshot_id, :metric_key, :domain, :period_type, :business_date, :segment_type, :segment_id,
                    :actual_value, 'unknown', :projection_method, '{}'::jsonb, :created_at
                )
                ON CONFLICT (metric_key, period_type, business_date, segment_type, COALESCE(segment_id, '__platform__'))
                DO UPDATE SET
                    actual_value = EXCLUDED.actual_value,
                    projection_method = EXCLUDED.projection_method,
                    created_at = EXCLUDED.created_at
                """, params);
    }

    @Override
    public BigDecimal sumTicketRevenueCents(LocalDate businessDate, String windowDomain) {
        BigDecimal value = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount_cent), 0)
                  FROM ticket_orders
                 WHERE pay_status = 'paid'
                   AND created_at >= :start
                   AND created_at < :end
                """, dayRange(windowDomain, businessDate), BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    public BigDecimal sumTicketRefundCents(LocalDate businessDate, String windowDomain) {
        // RC1②：自营售票退款，ticket_refunds 以 created_at 落经营日窗口 [start, end)（与 T7 aggregateOfficialTicket 口径一致）。
        BigDecimal value = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(refund_amount_cent), 0)
                  FROM ticket_refunds
                 WHERE status = 'success'
                   AND succeeded_at >= :start
                   AND succeeded_at < :end
                """, dayRange(windowDomain, businessDate), BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.complex.platform.bar.BarRevenueSourceService barRevenueSources;

    private List<String> barRevenueMerchants() {
        return jdbcTemplate.queryForList("SELECT DISTINCT merchant_id FROM bar_bills WHERE merchant_id IS NOT NULL ORDER BY merchant_id",
                new MapSqlParameterSource(), String.class);
    }

    @Override
    public Map<String, BigDecimal> sumBarRevenueCentsByMerchant(LocalDate businessDate, String windowDomain) {
        Instant from = businessDayResolver.startInstant(windowDomain, businessDate);
        Instant to = businessDayResolver.endInstant(windowDomain, businessDate);
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (String merchant : barRevenueMerchants()) {
            long amount = 0L;
            var collections = barRevenueSources.completedCollections(merchant, from, to);
            var withheld = barRevenueSources.withheldServiceFees(merchant, businessDate, businessDate.plusDays(1));
            if (collections.isEmpty() && withheld.isEmpty()) continue;
            for (var row : collections) {
                if (row.goodsRevenueCent() != null) amount = Math.addExact(amount, row.goodsRevenueCent());
            }
            for (var row : withheld) {
                if (row.amountCent() != null) amount = Math.addExact(amount, row.amountCent());
            }
            totals.put(merchant, BigDecimal.valueOf(amount));
        }
        return totals;
    }

    @Override
    public Map<String, BigDecimal> sumBarRefundCentsByMerchant(LocalDate businessDate, String windowDomain) {
        Instant from = businessDayResolver.startInstant(windowDomain, businessDate);
        Instant to = businessDayResolver.endInstant(windowDomain, businessDate);
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (String merchant : barRevenueMerchants()) {
            long amount = 0L;
            var refunds = barRevenueSources.completedGoodsRefunds(merchant, from, to);
            if (refunds.isEmpty()) continue;
            for (var row : refunds) {
                if (row.amountCent() != null) amount = Math.addExact(amount, row.amountCent());
            }
            totals.put(merchant, BigDecimal.valueOf(amount));
        }
        return totals;
    }

    @Override
    public Map<String, BigDecimal> sumTicketBarShareCentsByMerchant(LocalDate businessDate, String windowDomain) {
        // 待确认项#1：当日发放（created_at 落经营日窗口）、业态映射到酒吧的权益分配收入按商户汇总（展示口径）。
        // 业态归属映射：drink/gift_drink/bar_* → 酒吧（与 ticket_amusement_share 互斥，避免重复）。
        return sumAllocatedRevenueByMerchant(businessDate, windowDomain, """
                   AND (entitlement_type IN ('drink', 'gift_drink')
                        OR entitlement_type LIKE 'bar\\_%' ESCAPE '\\')
                """);
    }

    @Override
    public Map<String, BigDecimal> sumTicketAmusementShareCentsByMerchant(LocalDate businessDate, String windowDomain) {
        // 待确认项#1：当日发放、业态映射到游乐的权益分配收入按商户汇总（展示口径）。
        // 业态归属映射：amusement / amusement_* → 游乐。
        return sumAllocatedRevenueByMerchant(businessDate, windowDomain, """
                   AND (entitlement_type = 'amusement'
                        OR entitlement_type LIKE 'amusement\\_%' ESCAPE '\\')
                """);
    }

    private Map<String, BigDecimal> sumAllocatedRevenueByMerchant(LocalDate businessDate, String windowDomain, String entitlementTypeFilter) {
        return keyTotals("""
                SELECT merchant_id, COALESCE(SUM(allocated_revenue_cent), 0) AS total
                  FROM issued_entitlements
                 WHERE allocated_revenue_cent IS NOT NULL
                   AND created_at >= :start
                   AND created_at < :end
                """ + entitlementTypeFilter + """
                 GROUP BY merchant_id
                """, dayRange(windowDomain, businessDate));
    }

    /** 两列聚合查询通用收集：调用方 SELECT 恰好两列（键、值），按列序读取。 */
    private Map<String, BigDecimal> keyTotals(String sql, SqlParameterSource params) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        jdbcTemplate.query(sql, params, (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                result.put(rs.getString(1), rs.getBigDecimal(2)));
        return result;
    }

    @Override
    public Map<String, BigDecimal> sumTicketAllocatedRevenueByMerchant(LocalDate businessDate, String windowDomain) {
        // 清单#4：本商户售票分量——当日发放（created_at 落经营日窗口）权益的 allocated_revenue_cent 按商户汇总。
        // allocated_revenue_cent IS NULL（direct 出票/无订单）不计，保守不归任何商户。
        return keyTotals("""
                SELECT merchant_id, COALESCE(SUM(allocated_revenue_cent), 0) AS total
                  FROM issued_entitlements
                 WHERE allocated_revenue_cent IS NOT NULL
                   AND created_at >= :start
                   AND created_at < :end
                 GROUP BY merchant_id
                """, dayRange(windowDomain, businessDate));
    }

    @Override
    public Map<String, BigDecimal> sumTicketRefundByMerchant(LocalDate businessDate, String windowDomain) {
        // V220：ticket_refunds 已直挂 merchant_id（受理退款的销售商户），直接 GROUP BY 汇总。
        // created_at 落经营日窗口，与平台 sumTicketRefundCents 同口径；merchant_id 为 null（平台直营单）不归任何商户。
        return keyTotals("""
                SELECT merchant_id, COALESCE(SUM(refund_amount_cent), 0) AS total
                  FROM ticket_refunds
                 WHERE merchant_id IS NOT NULL
                   AND status = 'success'
                   AND succeeded_at >= :start
                   AND succeeded_at < :end
                 GROUP BY merchant_id
                """, dayRange(windowDomain, businessDate));
    }

    @Override
    public Map<String, BigDecimal> sumContractRentReceivableByMerchant(LocalDate businessDate, String windowDomain) {
        // P1-C：合同租金/物业/能源应收按 due_date 当日（期首）逐期确认收入，并入商户行级 revenue.net 租金分量。
        // 用 amount_cent（已挂账应收，权责发生制确认），与酒吧/售票的现金口径合并入 net（任务要求接 ar_ap receivable）。
        return keyTotals("""
                SELECT merchant_id, COALESCE(SUM(amount_cent), 0) AS total
                  FROM ar_ap_items
                 WHERE direction = 'receivable'
                   AND source_type IN ('contract_rent', 'contract_property_fee', 'contract_energy_fee')
                   AND due_date = :business_date
                 GROUP BY merchant_id
                """, new MapSqlParameterSource("business_date", businessDate));
    }


    @Override
    public BigDecimal countAmusementVerificationSuccess(LocalDate businessDate) {
        return amusementDays.detail("traffic.amusement",businessDate,businessDate,Instant.now(),false,1,0,null).summaryValue();
    }

    @Override
    public Map<String, BigDecimal> countTicketVerificationSuccessByMerchant(LocalDate businessDate) {
        return keyTotals("""
                SELECT merchant_id, COUNT(*) AS total
                  FROM ticket_verifications
                 WHERE status = 'success'
                   AND created_at >= :start
                   AND created_at < :end
                 GROUP BY merchant_id
                """, dayRange("ticketing", businessDate));
    }

    @Override
    public Map<Integer, Long> hourlyTrafficCounts(LocalDate businessDate, Optional<String> merchantScope) {
        // T4.3：按经营日窗口对成功核销 created_at 分小时桶。
        // ponytail: 按 UTC 小时分桶（确定、与窗口同源）；若要本地时段口径，改 AT TIME ZONE '业务时区'。
        Map<Integer, Long> result = new LinkedHashMap<>();
        MapSqlParameterSource params = dayRange("ticketing", businessDate);
        String sql;
        if (merchantScope.isEmpty()) {
            sql = """
                    SELECT h, COUNT(*) AS total FROM (
                        SELECT date_part('hour', created_at AT TIME ZONE 'UTC')::int AS h
                          FROM ticket_verifications
                         WHERE status = 'success' AND created_at >= :start AND created_at < :end
                    ) t GROUP BY h
                    """;
        } else {
            params.addValue("merchant_id", merchantScope.get());
            sql = """
                    SELECT date_part('hour', created_at AT TIME ZONE 'UTC')::int AS h, COUNT(*) AS total
                      FROM ticket_verifications
                     WHERE status = 'success' AND merchant_id = :merchant_id
                       AND created_at >= :start AND created_at < :end
                     GROUP BY h
                    """;
        }
        jdbcTemplate.query(sql, params, (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                result.put(rs.getInt("h"), rs.getLong("total")));
        if(merchantScope.isEmpty())amusementDays.hourly(businessDate).forEach((hour,count)->result.merge(hour,count,Long::sum));
        return result;
    }

    @Override
    public Map<String, BigDecimal> countTicketIssuedByMerchant(LocalDate businessDate) {
        // 清单#18：券发放数（核销率分母）——issued_entitlements 当日发放（created_at 落经营日窗口）按 merchant 分组计数，
        // 与 countTicketVerificationSuccessByMerchant 同口径（merchant_id 可空：归入平台口径合计）。
        return keyTotals("""
                SELECT merchant_id, COUNT(*) AS total
                  FROM issued_entitlements
                 WHERE created_at >= :start
                   AND created_at < :end
                 GROUP BY merchant_id
                """, dayRange("ticketing", businessDate));
    }

    @Override
    public BigDecimal countNewMembers(LocalDate businessDate) {
        // 仅 COUNT(*)，不读取任何 PII 列；customers.phone 的信封哑密文占位不影响本聚合。
        BigDecimal value = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM customers
                 WHERE created_at >= :start
                   AND created_at < :end
                   AND status NOT IN ('deregistered', 'blacklisted')
                """, dayRange("customer", businessDate), BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    public Map<String, BigDecimal> sumInventoryBalanceByMerchant() {
        return keyTotals("""
                SELECT merchant_id, COALESCE(SUM(quantity), 0) AS total
                  FROM inventory_stock_balances
                 GROUP BY merchant_id
                """, new MapSqlParameterSource());
    }

    @Override
    public Map<String, BigDecimal> countLowStockByMerchant() {
        // 缺货预警（财务确认：绝对值阈值/SKU 可配）：按 SKU 汇总各库位可用量，低于该 SKU 的安全库存阈值则计一个预警。
        // 阈值取 inventory_items.min_quantity（绝对值/SKU），阈值 ≤ 0 视为不预警（与 InventoryService.safety_stock_low 一致）。
        return keyTotals("""
                SELECT i.merchant_id AS merchant_id, COUNT(*) AS total
                  FROM inventory_items i
                  JOIN (
                        SELECT item_id, COALESCE(SUM(available_quantity), 0) AS available
                          FROM inventory_stock_balances
                         GROUP BY item_id
                       ) b ON b.item_id = i.id
                 WHERE i.min_quantity > 0
                   AND b.available < i.min_quantity
                 GROUP BY i.merchant_id
                """, new MapSqlParameterSource());
    }

    @Override
    public BigDecimal countPendingReview() {
        // 待复核主管审批的规范状态值=SupervisorReviewService 写入的 'pending_review'（V050 索引亦以此为准）；
        // 曾误写 'pending' 致主管复核部分恒为 0（总控「待复核」卡漏计），修正与 JdbcPlatformRepository.listPendingReviews 同谓词。
        BigDecimal reviews = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM supervisor_review_decisions WHERE review_status = 'pending_review'
                """, new MapSqlParameterSource(), BigDecimal.class);
        BigDecimal alerts = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM analytics_risk_alerts WHERE status = 'open'
                """, new MapSqlParameterSource(), BigDecimal.class);
        return (reviews == null ? BigDecimal.ZERO : reviews).add(alerts == null ? BigDecimal.ZERO : alerts);
    }

    // --- P2 集团/跨项目汇总（R6-D13）---

    @Override
    public long countMerchantsWithEtlSnapshot(LocalDate businessDate) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT segment_id)
                  FROM analytics_business_metric_snapshots
                 WHERE business_date = :business_date
                   AND period_type = 'daily'
                   AND segment_type = 'merchant'
                   AND projection_method = 'etl_aggregate'
                   AND segment_id IS NOT NULL
                """, new MapSqlParameterSource().addValue("business_date", businessDate), Long.class);
        return count == null ? 0L : count;
    }

    // --- ⑤ 集团多项目 ETL 维度（segment_type='project' 按 project_id 分组）---

    @Override
    public List<String> listProjectIdsWithEtlSnapshot(LocalDate businessDate) {
        return jdbcTemplate.query("""
                SELECT DISTINCT segment_id
                  FROM analytics_business_metric_snapshots
                 WHERE business_date = :business_date
                   AND period_type = 'daily'
                   AND segment_type = 'project'
                   AND projection_method = 'etl_aggregate'
                   AND segment_id IS NOT NULL
                 ORDER BY segment_id
                """, new MapSqlParameterSource().addValue("business_date", businessDate),
                (resultSet, rowNumber) -> resultSet.getString("segment_id"));
    }

    @Override
    public Map<String, BigDecimal> readProjectMetricSnapshots(LocalDate businessDate, String projectId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("business_date", businessDate)
                .addValue("segment_id", projectId);
        return keyTotals("""
                SELECT metric_key, actual_value
                  FROM analytics_business_metric_snapshots
                 WHERE business_date = :business_date
                   AND period_type = 'daily'
                   AND segment_type = 'project'
                   AND segment_id = :segment_id
                """, params);
    }

    @Override
    public long countMerchantsWithEtlSnapshotByProject(LocalDate businessDate, String projectId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT s.segment_id)
                  FROM analytics_business_metric_snapshots s
                  JOIN projects p ON p.merchant_id = s.segment_id
                 WHERE s.business_date = :business_date
                   AND s.period_type = 'daily'
                   AND s.segment_type = 'merchant'
                   AND s.projection_method = 'etl_aggregate'
                   AND p.id = :project_id
                """, new MapSqlParameterSource()
                        .addValue("business_date", businessDate)
                        .addValue("project_id", projectId), Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public AnalyticsRecords.OccupancyAreas occupancyAreas(LocalDate asOf) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("as_of", asOf);
        // CG-09：已租口径与铺位看板共用 SpaceRentalDerivationSql（开立中退租结算单的铺位＝已退租，不计入已租面积）。
        String sql = """
                SELECT
                  COALESCE(SUM(s.area_square_meter) FILTER (WHERE s.space_type IN ('shop','booth')), 0) AS total,
                  COALESCE(SUM(s.area_square_meter) FILTER (WHERE s.space_type IN ('shop','booth')
                      AND s.status <> 'unavailable'), 0) AS rentable,
                  COALESCE(SUM(s.area_square_meter) FILTER (WHERE s.space_type IN ('shop','booth')
                      AND s.status <> 'unavailable' AND __IS_LET__), 0) AS leased,
                  COALESCE(SUM(s.area_square_meter) FILTER (WHERE s.space_type IN ('shop','booth')
                      AND s.status <> 'unavailable' AND EXISTS (
                      SELECT 1 FROM contract_spaces cs2 JOIN contracts c2 ON c2.id = cs2.contract_id
                       WHERE cs2.space_id = s.id AND c2.lifecycle_status = 'pending_effective')), 0) AS signed_pending,
                  COALESCE(SUM(s.area_square_meter) FILTER (WHERE s.space_type = 'parking'
                      AND s.status <> 'unavailable'), 0) AS parking_rentable,
                  COALESCE(SUM(s.area_square_meter) FILTER (WHERE s.space_type = 'parking'
                      AND s.status <> 'unavailable' AND __IS_LET__), 0) AS parking_leased
                  FROM spaces s
                  LEFT JOIN LATERAL (__LATEST__) l ON true
                """.replace("__LATEST__", SpaceRentalDerivationSql.LATEST_CONTRACT_LATERAL)
                .replace("__IS_LET__", SpaceRentalDerivationSql.IS_LET);
        return jdbcTemplate.queryForObject(sql, params, (rs, n) -> new AnalyticsRecords.OccupancyAreas(
                rs.getBigDecimal("rentable"), rs.getBigDecimal("leased"), rs.getBigDecimal("signed_pending"),
                rs.getBigDecimal("parking_rentable"), rs.getBigDecimal("parking_leased"), rs.getBigDecimal("total")));
    }

    @Override
    public long countExpiringContracts(LocalDate asOf, int windowDays) {
        Long value = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM contracts
                 WHERE lifecycle_status = 'effective'
                   AND expiry_date >= :as_of AND expiry_date <= :until
                """, new MapSqlParameterSource()
                .addValue("as_of", asOf).addValue("until", asOf.plusDays(windowDays)), Long.class);
        return value == null ? 0L : value;
    }

    @Override
    public java.util.List<AnalyticsRecords.VacantSpaceRow> listVacantSpaces(LocalDate asOf, int limit, int offset,
            java.util.Collection<String> projectIds) {
        if (projectIds != null && projectIds.isEmpty()) return List.of();
        MapSqlParameterSource params = withProjects(new MapSqlParameterSource()
                .addValue("as_of", asOf)
                .addValue("limit", Math.min(Math.max(limit, 1), 200))
                .addValue("offset", Math.max(offset, 0)), projectIds);
        // CG-09：与铺位看板同口径（SpaceRentalDerivationSql）：已退租 > 洽谈中 > 空置；结算完成即转空置。
        String sql = """
                SELECT s.id, s.space_name, s.space_type, s.floor_no, s.area_square_meter,
                       CASE WHEN COALESCE(__IS_MOVED_OUT__, false) THEN 'moved_out'
                            WHEN EXISTS (SELECT 1 FROM leasing_intents li WHERE li.intended_space_id = s.id) THEN 'negotiating'
                            ELSE 'vacant' END AS rental_status
                  FROM spaces s
                  LEFT JOIN LATERAL (__LATEST__) l ON true
                 WHERE s.space_type IN ('shop','booth')
                   AND s.status <> 'unavailable'
                   AND NOT COALESCE(__IS_LET__, false)
                   AND (:all_projects OR s.project_id IN (:project_ids))
                 ORDER BY s.area_square_meter DESC NULLS LAST, s.id
                 LIMIT :limit OFFSET :offset
                """.replace("__LATEST__", SpaceRentalDerivationSql.LATEST_CONTRACT_LATERAL)
                .replace("__IS_LET__", SpaceRentalDerivationSql.IS_LET)
                .replace("__IS_MOVED_OUT__", SpaceRentalDerivationSql.IS_MOVED_OUT);
        return jdbcTemplate.query(sql, params, (rs, n) -> new AnalyticsRecords.VacantSpaceRow(
                rs.getString("id"), rs.getString("space_name"), rs.getString("space_type"),
                rs.getString("floor_no"), rs.getBigDecimal("area_square_meter"), rs.getString("rental_status")));
    }

    @Override
    public java.util.List<AnalyticsRecords.ExpiringContractRow> listExpiringContracts(LocalDate asOf, int windowDays, int limit, int offset,
            java.util.Collection<String> projectIds) {
        if (projectIds != null && projectIds.isEmpty()) return List.of();
        MapSqlParameterSource params = withProjects(new MapSqlParameterSource()
                .addValue("as_of", asOf)
                .addValue("until", asOf.plusDays(windowDays))
                .addValue("limit", Math.min(Math.max(limit, 1), 200))
                .addValue("offset", Math.max(offset, 0)), projectIds);
        return jdbcTemplate.query("""
                SELECT c.id, c.merchant_id, m.merchant_name, c.expiry_date,
                       (c.expiry_date - :as_of) AS days_left,
                       COALESCE((SELECT SUM(s.area_square_meter) FROM contract_spaces cs
                                   JOIN spaces s ON s.id = cs.space_id WHERE cs.contract_id = c.id), 0) AS bound_area
                  FROM contracts c
                  LEFT JOIN merchants m ON m.id = c.merchant_id
                 WHERE c.lifecycle_status = 'effective'
                   AND c.expiry_date >= :as_of AND c.expiry_date <= :until
                   AND (:all_projects OR c.project_id IN (:project_ids))
                 ORDER BY c.expiry_date, c.id
                 LIMIT :limit OFFSET :offset
                """, params, (rs, n) -> new AnalyticsRecords.ExpiringContractRow(
                rs.getString("id"), rs.getString("merchant_id"), rs.getString("merchant_name"),
                rs.getObject("expiry_date", LocalDate.class), rs.getLong("days_left"), rs.getBigDecimal("bound_area")));
    }

    @Override
    public java.util.List<AnalyticsRecords.HolidayPeriod> listHolidayPeriods(Integer year) {
        return jdbcTemplate.query("""
                SELECT holiday_name, holiday_year, start_date, end_date, source, updated_at
                  FROM holiday_calendar
                 WHERE :year::int IS NULL OR holiday_year = :year
                 ORDER BY holiday_year DESC, start_date
                """, new MapSqlParameterSource().addValue("year", year),
                (rs, n) -> new AnalyticsRecords.HolidayPeriod(
                        rs.getString("holiday_name"), rs.getInt("holiday_year"),
                        rs.getObject("start_date", LocalDate.class), rs.getObject("end_date", LocalDate.class),
                        rs.getString("source"), rs.getTimestamp("updated_at").toInstant()));
    }

    @Override
    public AnalyticsRecords.HolidayPeriod upsertHolidayPeriod(String holidayName, int holidayYear,
            LocalDate startDate, LocalDate endDate, String source, java.time.Instant now) {
        jdbcTemplate.update("""
                INSERT INTO holiday_calendar (id, holiday_name, holiday_year, start_date, end_date, source, updated_at)
                VALUES ('hol_' || gen_random_uuid(), :name, :year, :start_date, :end_date, :source, :now)
                ON CONFLICT (holiday_name, holiday_year) DO UPDATE SET
                    start_date = EXCLUDED.start_date,
                    end_date = EXCLUDED.end_date,
                    source = EXCLUDED.source,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("name", holidayName)
                .addValue("year", holidayYear)
                .addValue("start_date", startDate)
                .addValue("end_date", endDate)
                .addValue("source", source)
                .addValue("now", timestampWithTimeZone(now)));
        return new AnalyticsRecords.HolidayPeriod(holidayName, holidayYear, startDate, endDate, source, now);
    }

    @Override
    public boolean isWithinHoliday(LocalDate businessDate) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM holiday_calendar WHERE :d BETWEEN start_date AND end_date)
                """, new MapSqlParameterSource().addValue("d", businessDate), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public Map<String, BigDecimal> aggregateRange(LocalDate start, LocalDate end,
            java.util.List<String> sumKeys, java.util.List<String> lastKeys) {
        Map<String, BigDecimal> out = new java.util.LinkedHashMap<>();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("start", start).addValue("end", end)
                .addValue("sum_keys", sumKeys).addValue("last_keys", lastKeys);
        if (!sumKeys.isEmpty()) {
            jdbcTemplate.query("""
                    SELECT metric_key, SUM(actual_value) AS v
                      FROM analytics_business_metric_snapshots
                     WHERE period_type = 'daily' AND segment_type = 'platform'
                       AND business_date BETWEEN :start AND :end AND metric_key IN (:sum_keys)
                     GROUP BY metric_key
                    """, params, resultSet -> {
                out.put(resultSet.getString("metric_key"), resultSet.getBigDecimal("v"));
            });
        }
        if (!lastKeys.isEmpty()) {
            jdbcTemplate.query("""
                    SELECT DISTINCT ON (metric_key) metric_key, actual_value AS v
                      FROM analytics_business_metric_snapshots
                     WHERE period_type = 'daily' AND segment_type = 'platform'
                       AND business_date BETWEEN :start AND :end AND metric_key IN (:last_keys)
                     ORDER BY metric_key, business_date DESC
                    """, params, resultSet -> {
                out.put(resultSet.getString("metric_key"), resultSet.getBigDecimal("v"));
            });
        }
        return out;
    }

    @Override
    public void refreshMerchantSummaries(LocalDate businessDate, java.time.Instant now) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("business_date", businessDate)
                .addValue("now", timestampWithTimeZone(now));
        jdbcTemplate.update("""
                INSERT INTO analytics_merchant_summaries (
                    merchant_id, merchant_name, merchant_type, revenue_amount_cents, order_count,
                    risk_alert_count, settlement_status, business_date, created_at, updated_at)
                SELECT m.id, m.merchant_name, m.merchant_type,
                       GREATEST(COALESCE(rev.v, 0), 0)::bigint,
                       COALESCE(vd.v, 0)::int,
                       COALESCE(ra.c, 0)::int,
                       COALESCE(bill.status, 'none'),
                       :business_date, :now, :now
                  FROM merchants m
                  LEFT JOIN (SELECT segment_id, actual_value AS v
                               FROM analytics_business_metric_snapshots
                              WHERE metric_key = 'revenue.net' AND period_type = 'daily'
                                AND segment_type = 'merchant' AND business_date = :business_date) rev
                         ON rev.segment_id = m.id
                  LEFT JOIN (SELECT segment_id, actual_value AS v
                               FROM analytics_business_metric_snapshots
                              WHERE metric_key = 'verify.done' AND period_type = 'daily'
                                AND segment_type = 'merchant' AND business_date = :business_date) vd
                         ON vd.segment_id = m.id
                  LEFT JOIN (SELECT merchant_id, COUNT(*) AS c FROM risk_alerts
                              WHERE status = 'open' AND merchant_id IS NOT NULL GROUP BY merchant_id) ra
                         ON ra.merchant_id = m.id
                  LEFT JOIN LATERAL (SELECT status FROM merchant_bills
                                      WHERE merchant_id = m.id ORDER BY created_at DESC LIMIT 1) bill ON true
                 WHERE m.status = 'active'
                ON CONFLICT (merchant_id) DO UPDATE SET
                    merchant_name = EXCLUDED.merchant_name,
                    merchant_type = EXCLUDED.merchant_type,
                    revenue_amount_cents = EXCLUDED.revenue_amount_cents,
                    order_count = EXCLUDED.order_count,
                    risk_alert_count = EXCLUDED.risk_alert_count,
                    settlement_status = EXCLUDED.settlement_status,
                    business_date = EXCLUDED.business_date,
                    updated_at = EXCLUDED.updated_at
                """, params);
    }

    @Override
    public void syncMetricDefinitions(java.util.List<AnalyticsRecords.MetricDefinitionSyncRow> rows, java.time.Instant now) {
        for (AnalyticsRecords.MetricDefinitionSyncRow row : rows) {
            jdbcTemplate.update("""
                    INSERT INTO analytics_metric_definitions (
                        metric_key, name, domain, unit, calculation_note, owner_role, status, created_at, updated_at)
                    VALUES (:metric_key, :metric_key, :domain, :unit, :note, 'platform', :status, :now, :now)
                    ON CONFLICT (metric_key) DO UPDATE SET
                        domain = EXCLUDED.domain,
                        unit = EXCLUDED.unit,
                        calculation_note = EXCLUDED.calculation_note,
                        status = EXCLUDED.status,
                        updated_at = EXCLUDED.updated_at
                    """, new MapSqlParameterSource()
                    .addValue("metric_key", row.metricKey())
                    .addValue("domain", row.domain())
                    .addValue("unit", row.unit())
                    .addValue("note", row.note())
                    .addValue("status", row.ready() ? "active" : "disabled")
                    .addValue("now", timestampWithTimeZone(now)));
        }
    }

    @Override
    public void insertMetricAnomaly(String id, String metricKey, String domain, String riskLevel,
            BigDecimal observedValue, BigDecimal expectedValue, String summary, java.time.Instant detectedAt) {
        jdbcTemplate.update("""
                INSERT INTO analytics_metric_anomalies (
                    id, metric_key, domain, risk_level, observed_value, expected_value, summary, status, detected_at)
                VALUES (:id, :metric_key, :domain, :risk_level, :observed, :expected, :summary, 'open', :detected_at)
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("metric_key", metricKey)
                .addValue("domain", domain)
                .addValue("risk_level", riskLevel)
                .addValue("observed", observedValue)
                .addValue("expected", expectedValue)
                .addValue("summary", summary)
                .addValue("detected_at", timestampWithTimeZone(detectedAt)));
    }

    @Override
    public AnalyticsRecords.AnomalyConfig readAnomalyConfig() {
        return jdbcTemplate.query("""
                SELECT surge_pct, high_pct, enabled, updated_by, updated_at
                  FROM analytics_anomaly_config
                 WHERE id = 'default'
                """, new MapSqlParameterSource(), (rs, n) -> new AnalyticsRecords.AnomalyConfig(
                rs.getBigDecimal("surge_pct"), rs.getBigDecimal("high_pct"), rs.getBoolean("enabled"),
                rs.getString("updated_by"), rs.getTimestamp("updated_at").toInstant()))
                .stream().findFirst().orElse(null);
    }

    @Override
    public java.util.List<AnalyticsRecords.NotificationWindow> listNotificationWindows() {
        return jdbcTemplate.query("""
                SELECT window_key, days, description, updated_by, updated_at
                  FROM notification_windows ORDER BY window_key
                """, new MapSqlParameterSource(), (rs, n) -> new AnalyticsRecords.NotificationWindow(
                rs.getString("window_key"), rs.getInt("days"), rs.getString("description"),
                rs.getString("updated_by"), rs.getTimestamp("updated_at").toInstant()));
    }

    @Override
    public AnalyticsRecords.NotificationWindow upsertNotificationWindow(String windowKey, int days,
            String description, String updatedBy, java.time.Instant now) {
        jdbcTemplate.update("""
                INSERT INTO notification_windows (window_key, days, description, updated_by, updated_at)
                VALUES (:key, :days, :desc, :updated_by, :now)
                ON CONFLICT (window_key) DO UPDATE SET
                    days = EXCLUDED.days,
                    description = COALESCE(EXCLUDED.description, notification_windows.description),
                    updated_by = EXCLUDED.updated_by, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource().addValue("key", windowKey).addValue("days", days)
                .addValue("desc", description).addValue("updated_by", updatedBy)
                .addValue("now", timestampWithTimeZone(now)));
        return jdbcTemplate.query("""
                SELECT window_key, days, description, updated_by, updated_at
                  FROM notification_windows WHERE window_key = :key
                """, new MapSqlParameterSource().addValue("key", windowKey),
                (rs, n) -> new AnalyticsRecords.NotificationWindow(
                        rs.getString("window_key"), rs.getInt("days"), rs.getString("description"),
                        rs.getString("updated_by"), rs.getTimestamp("updated_at").toInstant()))
                .stream().findFirst().orElse(null);
    }

    @Override
    public AnalyticsRecords.AiThresholds readAiThresholds() {
        return jdbcTemplate.query("""
                SELECT local_llm_min_confidence, benchmark_deviation, updated_by, updated_at
                  FROM analytics_ai_thresholds WHERE id = 'default'
                """, new MapSqlParameterSource(), (rs, n) -> new AnalyticsRecords.AiThresholds(
                rs.getBigDecimal("local_llm_min_confidence"), rs.getBigDecimal("benchmark_deviation"),
                rs.getString("updated_by"), rs.getTimestamp("updated_at").toInstant()))
                .stream().findFirst().orElse(null);
    }

    @Override
    public AnalyticsRecords.AiThresholds upsertAiThresholds(BigDecimal minConfidence, BigDecimal benchmarkDeviation,
            String updatedBy, java.time.Instant now) {
        jdbcTemplate.update("""
                INSERT INTO analytics_ai_thresholds (id, local_llm_min_confidence, benchmark_deviation,
                        updated_by, updated_at)
                VALUES ('default', :conf, :dev, :updated_by, :now)
                ON CONFLICT (id) DO UPDATE SET
                    local_llm_min_confidence = EXCLUDED.local_llm_min_confidence,
                    benchmark_deviation = EXCLUDED.benchmark_deviation,
                    updated_by = EXCLUDED.updated_by, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("conf", minConfidence).addValue("dev", benchmarkDeviation)
                .addValue("updated_by", updatedBy).addValue("now", timestampWithTimeZone(now)));
        return readAiThresholds();
    }

    private static final String MRAR_COLUMNS =
            "id, rule_scope, merchant_id, revenue_source, attribution_mode, share_bps, "
            + "refund_attribution, status, is_draft, updated_by, updated_at";

    private AnalyticsRecords.MerchantRevenueAttributionRule mapAttributionRule(java.sql.ResultSet rs, int n)
            throws java.sql.SQLException {
        return new AnalyticsRecords.MerchantRevenueAttributionRule(
                rs.getString("id"), rs.getString("rule_scope"), rs.getString("merchant_id"),
                rs.getString("revenue_source"), rs.getString("attribution_mode"), rs.getInt("share_bps"),
                rs.getString("refund_attribution"), rs.getString("status"), rs.getBoolean("is_draft"),
                rs.getString("updated_by"),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toInstant());
    }

    @Override
    public java.util.List<AnalyticsRecords.MerchantRevenueAttributionRule> listRevenueAttributionRules() {
        return jdbcTemplate.query("SELECT " + MRAR_COLUMNS
                + " FROM merchant_revenue_attribution_rules ORDER BY rule_scope, revenue_source, merchant_id",
                new MapSqlParameterSource(), this::mapAttributionRule);
    }

    @Override
    public AnalyticsRecords.MerchantRevenueAttributionRule findDefaultAttributionRule(String revenueSource) {
        return jdbcTemplate.query("SELECT " + MRAR_COLUMNS
                + " FROM merchant_revenue_attribution_rules"
                + " WHERE rule_scope = 'default' AND revenue_source = :source AND status = 'active' AND is_draft = FALSE",
                new MapSqlParameterSource("source", revenueSource), this::mapAttributionRule)
                .stream().findFirst().orElse(null);
    }

    @Override
    public AnalyticsRecords.MerchantRevenueAttributionRule upsertRevenueAttributionRule(
            AnalyticsRecords.MerchantRevenueAttributionRule rule, String updatedBy, java.time.Instant now) {
        // 待主会话协调 batch0:Path 2 落草案——ON CONFLICT 命中 V329 索引「同 scope 的草案槽」(is_draft 已入索引),
        // 覆盖草案、不动生效行;转正在 activateAttributionRuleDraft(软替换旧生效 + 翻草案)。scope 两分支各对一部分索引。
        String conflict = "default".equals(rule.ruleScope())
                ? "ON CONFLICT (revenue_source, is_draft) WHERE rule_scope = 'default' AND status = 'active'"
                : "ON CONFLICT (merchant_id, revenue_source, is_draft) WHERE rule_scope = 'merchant' AND status = 'active'";
        String landedId = jdbcTemplate.queryForObject("""
                INSERT INTO merchant_revenue_attribution_rules (
                    id, rule_scope, merchant_id, revenue_source, attribution_mode, share_bps,
                    refund_attribution, status, is_draft, updated_by, updated_at
                ) VALUES (
                    :id, :scope, :merchant_id, :source, :mode, :bps,
                    :refund, :status, :is_draft, :updated_by, :now
                )
                """ + conflict + """
                 DO UPDATE SET
                    attribution_mode = EXCLUDED.attribution_mode,
                    share_bps = EXCLUDED.share_bps,
                    refund_attribution = EXCLUDED.refund_attribution,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = EXCLUDED.updated_at
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("id", rule.id())
                .addValue("scope", rule.ruleScope())
                .addValue("merchant_id", rule.merchantId(), java.sql.Types.VARCHAR)
                .addValue("source", rule.revenueSource())
                .addValue("mode", rule.attributionMode())
                .addValue("bps", rule.shareBps())
                .addValue("refund", rule.refundAttribution())
                .addValue("status", rule.status())
                .addValue("is_draft", rule.isDraft())
                .addValue("updated_by", updatedBy)
                .addValue("now", timestampWithTimeZone(now)), String.class);
        return jdbcTemplate.query("SELECT " + MRAR_COLUMNS
                + " FROM merchant_revenue_attribution_rules WHERE id = :id",
                new MapSqlParameterSource("id", landedId), this::mapAttributionRule)
                .stream().findFirst().orElse(null);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public int activateAttributionRuleDraft(String id, String updatedBy, java.time.Instant now) {
        // 待主会话协调 batch0:Path 2 换行——同 scope 旧生效行软替换 status→inactive(对齐项6,不物理删),再翻草案。
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("id", id).addValue("updated_by", updatedBy).addValue("now", timestampWithTimeZone(now));
        jdbcTemplate.update("""
                UPDATE merchant_revenue_attribution_rules l
                   SET status = 'inactive', updated_by = :updated_by, updated_at = :now
                  FROM merchant_revenue_attribution_rules d
                 WHERE d.id = :id AND d.is_draft = TRUE AND d.status = 'active'
                   AND l.is_draft = FALSE AND l.status = 'active' AND l.id <> d.id
                   AND l.rule_scope = d.rule_scope AND l.revenue_source = d.revenue_source
                   AND l.merchant_id IS NOT DISTINCT FROM d.merchant_id
                """, p);
        return jdbcTemplate.update("""
                UPDATE merchant_revenue_attribution_rules
                   SET is_draft = FALSE, updated_by = :updated_by, updated_at = :now
                 WHERE id = :id AND is_draft = TRUE AND status = 'active'
                """, p);
    }

    @Override
    public String attributionRuleDraftUpdatedBy(String id) {
        return jdbcTemplate.query(
                "SELECT updated_by FROM merchant_revenue_attribution_rules WHERE id = :id AND is_draft = TRUE AND status = 'active'",
                new MapSqlParameterSource("id", id), rs -> rs.next() ? rs.getString(1) : null);
    }

    @Override
    public AnalyticsRecords.LeasingFunnel leasingFunnel(LocalDate businessDate, String windowDomain) {
        // V220 招商漏斗:按线索 created_at 落经营日窗口;已达意向 = 进展到 converted_to_intent 及之后阶段(含 negotiating/signed);
        // 已签约 = signed。lost/on_hold/pending_follow_up/following_up 不计入意向。
        return jdbcTemplate.query("""
                SELECT COUNT(*) AS lead_count,
                       COUNT(*) FILTER (WHERE status IN ('converted_to_intent', 'negotiating', 'signed')) AS intent_count,
                       COUNT(*) FILTER (WHERE status = 'signed') AS signed_count
                  FROM leasing_leads
                 WHERE created_at >= :start AND created_at < :end
                """, dayRange(windowDomain, businessDate), (rs, n) -> new AnalyticsRecords.LeasingFunnel(
                rs.getLong("lead_count"), rs.getLong("intent_count"), rs.getLong("signed_count")))
                .stream().findFirst().orElse(new AnalyticsRecords.LeasingFunnel(0, 0, 0));
    }

    @Override
    public BigDecimal sumThirdpartyTurnoverCents(LocalDate businessDate, String windowDomain) {
        // V220 第三方回流营业额:成功回流订单(sync_type='order', status='synced')金额合计,按 created_at 落窗。
        // integration_order_syncs 无 merchant_id → 仅平台级(租售比分母)。
        Long total = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount_cent), 0)
                  FROM integration_order_syncs
                 WHERE sync_type = 'order' AND status = 'synced'
                   AND created_at >= :start AND created_at < :end
                """, dayRange(windowDomain, businessDate), Long.class);
        return BigDecimal.valueOf(total == null ? 0L : total);
    }

    @Override
    public AnalyticsRecords.RentToSalesAggregate rentToSales(String period) {
        // P1c 店级/业态级租售比取数（2026-07-21 用户拍板 8 口径）。
        // 分子=租金：ar_ap_items 应收 source_type∈(contract_rent 月租, turnover_rent 营业额抽成)，
        //   按 accounting_period=:period（YYYY-MM，两类租金同期键，与分红挂接同口径）归期，仅已归属商户（merchant_id NOT NULL）。
        // 分母=营业额：integration_order_syncs 第三方回流，status='synced'、sync_type∈(order,refund) 实收净额
        //   （口径①含税毛额 ②扣退款；voucher_verification 通票核销不进抽成基数，排除），
        //   经营日窗口 [期首, 次月首) 按业务日切派生（与营业额抽成计费引擎 aggregateMerchantReflowTurnover 同窗）。
        //   商户级净额 GREATEST(…,0) 钳 0（负营业额无业务意义，同抽成口径）。
        // NULL merchant_id 的回流营业额=未归属：不进任何商户/业态，只汇入平台级分母（口径③⑧，诚实标注，绝不塞给某商户）。
        java.time.YearMonth ym = java.time.YearMonth.parse(period);
        java.time.Instant windowStart = businessDayResolver.startInstant(ym.atDay(1));
        java.time.Instant windowEnd = businessDayResolver.startInstant(ym.plusMonths(1).atDay(1));
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("period", period)
                .addValue("window_start", timestampWithTimeZone(windowStart))
                .addValue("window_end", timestampWithTimeZone(windowEnd));

        java.util.List<AnalyticsRecords.RentToSalesMerchantRow> merchants = jdbcTemplate.query("""
                WITH rent AS (
                    SELECT merchant_id, SUM(amount_cent) AS rent_cent
                      FROM ar_ap_items
                     WHERE direction = 'receivable'
                       AND source_type IN ('contract_rent', 'turnover_rent')
                       AND merchant_id IS NOT NULL
                       AND accounting_period = :period
                     GROUP BY merchant_id
                ),
                sales AS (
                    SELECT merchant_id,
                           SUM(CASE sync_type WHEN 'order' THEN amount_cent
                                              WHEN 'refund' THEN -amount_cent ELSE 0 END) AS sales_cent
                      FROM integration_order_syncs
                     WHERE merchant_id IS NOT NULL
                       AND status = 'synced'
                       AND sync_type IN ('order', 'refund')
                       AND created_at >= :window_start AND created_at < :window_end
                     GROUP BY merchant_id
                )
                SELECT m.id AS merchant_id, m.merchant_name, m.merchant_type,
                       COALESCE(r.rent_cent, 0) AS rent_cent,
                       GREATEST(COALESCE(s.sales_cent, 0), 0) AS sales_cent
                  FROM merchants m
                  JOIN (SELECT merchant_id FROM rent UNION SELECT merchant_id FROM sales) ids ON ids.merchant_id = m.id
                  LEFT JOIN rent r ON r.merchant_id = m.id
                  LEFT JOIN sales s ON s.merchant_id = m.id
                 ORDER BY rent_cent DESC, sales_cent DESC, m.id
                """, params, (rs, n) -> new AnalyticsRecords.RentToSalesMerchantRow(
                        rs.getString("merchant_id"), rs.getString("merchant_name"), rs.getString("merchant_type"),
                        rs.getLong("rent_cent"), rs.getLong("sales_cent")));

        Long unattributed = jdbcTemplate.queryForObject("""
                SELECT GREATEST(COALESCE(SUM(CASE sync_type WHEN 'order' THEN amount_cent
                                                            WHEN 'refund' THEN -amount_cent ELSE 0 END), 0), 0)
                  FROM integration_order_syncs
                 WHERE merchant_id IS NULL
                   AND status = 'synced'
                   AND sync_type IN ('order', 'refund')
                   AND created_at >= :window_start AND created_at < :window_end
                """, params, Long.class);

        return new AnalyticsRecords.RentToSalesAggregate(merchants, unattributed == null ? 0L : unattributed);
    }

    @Override
    public java.util.List<AnalyticsRecords.AbandonedEntitlementCell> abandonedEntitlementRevenue(
            LocalDate start, LocalDate end) {
        // 结算B2 放弃权益额外收入：直接读生产每日 02:30 已写好的 unverified_expired 归项目流水，零重算。
        //   金额=权威 unverified_amount_cent（SettlementExpiryJob 按 allocated×(total−used)/total 算好）、笔数=COUNT(*)。
        //   票种维度：JOIN issued_entitlements 取 bar_id（NULL=通票 / 非空=单独门票，V288 票绑酒吧列；
        //     绝不用 exclusive_merchant_id——那是结算专属标记、正交维度）。
        //   权益类型桶：沿用 analytics 现有口径（酒水 drink/gift_drink/bar_、入场 entry、游乐 amusement*、演出 show、其余 other）。
        //   窗口 [start(含), end(不含))：与 rentToSales 同法用 BusinessDayResolver 派生经营日窗，按 business_at（=到期业务时刻，
        //     存量行回退 created_at）归窗。
        java.time.Instant windowStart = businessDayResolver.startInstant(start);
        java.time.Instant windowEnd = businessDayResolver.startInstant(end);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("window_start", timestampWithTimeZone(windowStart))
                .addValue("window_end", timestampWithTimeZone(windowEnd));
        return jdbcTemplate.query("""
                SELECT CASE
                           WHEN st.entitlement_type IN ('drink', 'gift_drink')
                                OR st.entitlement_type LIKE 'bar\\_%' ESCAPE '\\' THEN 'drink'
                           WHEN st.entitlement_type = 'entry' THEN 'entry'
                           WHEN st.entitlement_type = 'amusement'
                                OR st.entitlement_type LIKE 'amusement\\_%' ESCAPE '\\' THEN 'amusement'
                           WHEN st.entitlement_type = 'show' THEN 'show'
                           ELSE 'other'
                       END AS bucket,
                       CASE WHEN ie.bar_id IS NULL THEN 'pass' ELSE 'ticket' END AS ticket_type,
                       COALESCE(SUM(st.unverified_amount_cent), 0) AS amount_cent,
                       COUNT(*) AS cnt
                  FROM settlement_transactions st
                  -- INNER JOIN 安全依赖:issued_entitlements 受软删铁律永不硬删,且 unverified_expired 行由 SettlementExpiryJob
                  --   读该表后写入(entitlement_id=ie.id 恒有匹配)→ 不丢行、不少报。ie.id 是 PK → 一对一、不扇出翻倍。
                  --   (对抗审计 2026-07-21 复核:若将来违反软删铁律清档,孤儿流水会被整条丢弃致静默少报,届时改 LEFT JOIN。)
                  JOIN issued_entitlements ie ON ie.id = st.entitlement_id
                 WHERE st.transaction_type = 'unverified_expired'
                   -- S8:未核销款归属从「项目公司」切为「经营主体」(默认娱乐平台公司/游乐业态归游乐公司,entity);
                   --   'project' 一并保留 = 切换前已落的旧行仍进报表(行是 append-only 事实,不回填不改写)。
                   AND st.beneficiary_type IN ('project', 'entity')
                   AND COALESCE(st.business_at, st.created_at) >= :window_start
                   AND COALESCE(st.business_at, st.created_at) <  :window_end
                 GROUP BY 1, 2
                """, params, (rs, n) -> new AnalyticsRecords.AbandonedEntitlementCell(
                        rs.getString("bucket"), rs.getString("ticket_type"),
                        rs.getLong("amount_cent"), rs.getLong("cnt")));
    }

    @Override
    public AnalyticsRecords.AnomalyConfig upsertAnomalyConfig(BigDecimal surgePct, BigDecimal highPct,
            boolean enabled, String updatedBy, java.time.Instant now) {
        jdbcTemplate.update("""
                INSERT INTO analytics_anomaly_config (id, surge_pct, high_pct, enabled, updated_by, updated_at)
                VALUES ('default', :surge, :high, :enabled, :updated_by, :now)
                ON CONFLICT (id) DO UPDATE SET
                    surge_pct = EXCLUDED.surge_pct,
                    high_pct = EXCLUDED.high_pct,
                    enabled = EXCLUDED.enabled,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("surge", surgePct)
                .addValue("high", highPct)
                .addValue("enabled", enabled)
                .addValue("updated_by", updatedBy)
                .addValue("now", timestampWithTimeZone(now)));
        return readAnomalyConfig();
    }

    @Override
    public void rollupMonthlySnapshots(LocalDate monthFirst, LocalDate upTo,
            java.util.List<String> sumKeys, java.util.List<String> lastKeys, java.time.Instant now) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("month_first", monthFirst)
                .addValue("up_to", upTo)
                .addValue("sum_keys", sumKeys)
                .addValue("last_keys", lastKeys)
                .addValue("now", timestampWithTimeZone(now));
        if (!sumKeys.isEmpty()) {
            jdbcTemplate.update("""
                    INSERT INTO analytics_business_metric_snapshots (
                        snapshot_id, metric_key, domain, period_type, business_date, segment_type, segment_id,
                        actual_value, trend_direction, projection_method, payload_json, created_at)
                    SELECT 'etl_' || gen_random_uuid(), metric_key, MIN(domain), 'monthly', :month_first, 'platform', NULL,
                           SUM(actual_value), 'unknown', 'etl_aggregate', '{}'::jsonb, :now
                      FROM analytics_business_metric_snapshots
                     WHERE period_type = 'daily' AND segment_type = 'platform'
                       AND business_date >= :month_first AND business_date <= :up_to
                       AND metric_key IN (:sum_keys)
                     GROUP BY metric_key
                    ON CONFLICT (metric_key, period_type, business_date, segment_type, COALESCE(segment_id, '__platform__'))
                    DO UPDATE SET actual_value = EXCLUDED.actual_value, created_at = EXCLUDED.created_at
                    """, params);
        }
        if (!lastKeys.isEmpty()) {
            jdbcTemplate.update("""
                    INSERT INTO analytics_business_metric_snapshots (
                        snapshot_id, metric_key, domain, period_type, business_date, segment_type, segment_id,
                        actual_value, trend_direction, projection_method, payload_json, created_at)
                    SELECT 'etl_' || gen_random_uuid(), latest.metric_key, latest.domain, 'monthly', :month_first, 'platform', NULL,
                           latest.actual_value, 'unknown', 'etl_aggregate', '{}'::jsonb, :now
                      FROM (SELECT DISTINCT ON (metric_key) metric_key, domain, actual_value
                              FROM analytics_business_metric_snapshots
                             WHERE period_type = 'daily' AND segment_type = 'platform'
                               AND business_date >= :month_first AND business_date <= :up_to
                               AND metric_key IN (:last_keys)
                             ORDER BY metric_key, business_date DESC) latest
                    ON CONFLICT (metric_key, period_type, business_date, segment_type, COALESCE(segment_id, '__platform__'))
                    DO UPDATE SET actual_value = EXCLUDED.actual_value, created_at = EXCLUDED.created_at
                    """, params);
        }
    }

    @Override
    public Map<String, BigDecimal> readMonthlySnapshots(LocalDate monthFirst) {
        return keyTotals("""
                SELECT metric_key, actual_value
                  FROM analytics_business_metric_snapshots
                 WHERE period_type = 'monthly' AND segment_type = 'platform' AND segment_id IS NULL
                   AND business_date = :month_first
                """, new MapSqlParameterSource().addValue("month_first", monthFirst));
    }

    @Override
    public AnalyticsRecords.WorkOrderSla workOrderSla(java.time.Instant start, java.time.Instant end, java.time.Instant asOf) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("start", timestampWithTimeZone(start))
                .addValue("end", timestampWithTimeZone(end))
                .addValue("as_of", timestampWithTimeZone(asOf));
        return jdbcTemplate.queryForObject("""
                SELECT
                  (SELECT percentile_cont(0.5) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (responded_at - created_at)) / 3600.0)
                     FROM facility_work_orders WHERE responded_at >= :start AND responded_at < :end) AS response_p50,
                  (SELECT percentile_cont(0.5) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (completed_at - assigned_at)) / 3600.0)
                     FROM facility_work_orders WHERE completed_at >= :start AND completed_at < :end AND assigned_at IS NOT NULL) AS handle_p50,
                  (SELECT COUNT(*) FROM facility_work_orders WHERE handle_due_at >= :start AND handle_due_at < :end) AS due_count,
                  (SELECT COUNT(*) FROM facility_work_orders WHERE handle_due_at >= :start AND handle_due_at < :end
                      AND (completed_at > handle_due_at OR (completed_at IS NULL AND handle_due_at < :as_of))) AS overdue_count,
                  (SELECT COUNT(*) FROM facility_work_orders WHERE created_at <= :as_of
                      AND completed_at IS NULL AND status <> 'closed') AS backlog
                """, params, (rs, n) -> new AnalyticsRecords.WorkOrderSla(
                rs.getBigDecimal("response_p50"), rs.getBigDecimal("handle_p50"),
                rs.getLong("due_count"), rs.getLong("overdue_count"), rs.getLong("backlog")));
    }

    @Override
    public java.util.List<AnalyticsRecords.OverdueWorkOrderRow> listOverdueWorkOrders(
            java.time.Instant start, java.time.Instant end, java.time.Instant asOf, int limit, int offset,
            java.util.Collection<String> projectIds) {
        if (projectIds != null && projectIds.isEmpty()) return List.of();
        MapSqlParameterSource params = withProjects(new MapSqlParameterSource()
                .addValue("start", timestampWithTimeZone(start))
                .addValue("end", timestampWithTimeZone(end))
                .addValue("as_of", timestampWithTimeZone(asOf))
                .addValue("limit", Math.min(Math.max(limit, 1), 200))
                .addValue("offset", Math.max(offset, 0)), projectIds);
        return jdbcTemplate.query("""
                SELECT id, code, title, status, priority, assignee_id, handle_due_at, completed_at,
                       ROUND((EXTRACT(EPOCH FROM (COALESCE(completed_at, :as_of) - handle_due_at)) / 3600.0)::numeric, 1) AS overdue_hours
                  FROM facility_work_orders
                 WHERE handle_due_at >= :start AND handle_due_at < :end
                   AND (completed_at > handle_due_at OR (completed_at IS NULL AND handle_due_at < :as_of))
                   AND (:all_projects OR project_id IN (:project_ids))
                 ORDER BY overdue_hours DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, (rs, n) -> {
            java.sql.Timestamp due = rs.getTimestamp("handle_due_at");
            java.sql.Timestamp completed = rs.getTimestamp("completed_at");
            return new AnalyticsRecords.OverdueWorkOrderRow(
                    rs.getString("id"), rs.getString("code"), rs.getString("title"), rs.getString("status"),
                    rs.getString("priority"), rs.getString("assignee_id"),
                    due == null ? null : due.toInstant(), completed == null ? null : completed.toInstant(),
                    rs.getBigDecimal("overdue_hours"));
        });
    }

    @Override
    public AnalyticsRecords.WorkOrderSla merchantWorkOrderSla(java.time.Instant start, java.time.Instant end, java.time.Instant asOf) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("start", timestampWithTimeZone(start))
                .addValue("end", timestampWithTimeZone(end))
                .addValue("as_of", timestampWithTimeZone(asOf));
        return jdbcTemplate.queryForObject("""
                SELECT
                  (SELECT percentile_cont(0.5) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (closed_at - created_at)) / 3600.0)
                     FROM merchant_service_work_orders WHERE closed_at >= :start AND closed_at < :end) AS handle_p50,
                  (SELECT COUNT(*) FROM merchant_service_work_orders
                    WHERE due_at >= :start AND due_at < :end AND status <> 'cancelled') AS due_count,
                  (SELECT COUNT(*) FROM merchant_service_work_orders
                    WHERE due_at >= :start AND due_at < :end AND status <> 'cancelled'
                      AND (sla_breached OR closed_at > due_at
                           OR (closed_at IS NULL AND status NOT IN ('resolved', 'closed') AND due_at < :as_of))) AS overdue_count,
                  (SELECT COUNT(*) FROM merchant_service_work_orders WHERE created_at <= :as_of
                      AND status NOT IN ('resolved', 'closed', 'cancelled')) AS backlog
                """, params, (rs, n) -> new AnalyticsRecords.WorkOrderSla(
                null, rs.getBigDecimal("handle_p50"),
                rs.getLong("due_count"), rs.getLong("overdue_count"), rs.getLong("backlog")));
    }

    @Override
    public java.util.List<AnalyticsRecords.OverdueWorkOrderRow> listOverdueMerchantWorkOrders(
            java.time.Instant start, java.time.Instant end, java.time.Instant asOf, int limit, int offset,
            java.util.Collection<String> projectIds) {
        if (projectIds != null && projectIds.isEmpty()) return List.of();
        MapSqlParameterSource params = withProjects(new MapSqlParameterSource()
                .addValue("start", timestampWithTimeZone(start))
                .addValue("end", timestampWithTimeZone(end))
                .addValue("as_of", timestampWithTimeZone(asOf))
                .addValue("limit", Math.min(Math.max(limit, 1), 200))
                .addValue("offset", Math.max(offset, 0)), projectIds);
        // M737：表里没有项目列，按商户的经营项目过滤（同一条子查询，见 MERCHANTS_OPERATING_IN_PROJECTS）。
        return jdbcTemplate.query("""
                SELECT id, title, status, priority, owner_user_id, due_at, closed_at,
                       ROUND((EXTRACT(EPOCH FROM (COALESCE(closed_at, :as_of) - due_at)) / 3600.0)::numeric, 1) AS overdue_hours
                  FROM merchant_service_work_orders
                 WHERE due_at >= :start AND due_at < :end AND status <> 'cancelled'
                   AND (sla_breached OR closed_at > due_at
                        OR (closed_at IS NULL AND status NOT IN ('resolved', 'closed') AND due_at < :as_of))
                   AND (:all_projects OR merchant_id IN (%s))
                 ORDER BY overdue_hours DESC, id
                 LIMIT :limit OFFSET :offset
                """.formatted(MERCHANTS_OPERATING_IN_PROJECTS), params, (rs, n) -> {
            java.sql.Timestamp due = rs.getTimestamp("due_at");
            java.sql.Timestamp closed = rs.getTimestamp("closed_at");
            return new AnalyticsRecords.OverdueWorkOrderRow(
                    rs.getString("id"), null, rs.getString("title"), rs.getString("status"),
                    rs.getString("priority"), rs.getString("owner_user_id"),
                    due == null ? null : due.toInstant(), closed == null ? null : closed.toInstant(),
                    rs.getBigDecimal("overdue_hours"));
        });
    }

    @Override
    public Map<String, BigDecimal> energyConsumptionByType(java.time.Instant start, java.time.Instant end) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("start", timestampWithTimeZone(start))
                .addValue("end", timestampWithTimeZone(end));
        Map<String, BigDecimal> out = new java.util.LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT m.meter_type, SUM(r.effective_consumption) AS total
                  FROM energy_effective_meter_readings r
                  JOIN energy_meters m ON m.id = r.meter_id
                 WHERE r.status='reviewed' AND r.read_at >= :start AND r.read_at < :end
                 GROUP BY m.meter_type
                 ORDER BY m.meter_type
                """, params, rs -> {
            out.put(rs.getString("meter_type"), rs.getBigDecimal("total"));
        });
        return out;
    }

    @Override
    public long countAbnormalReadings(java.time.Instant start, java.time.Instant end) {
        Long value = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM energy_meter_readings
                 WHERE (abnormal_reason IS NOT NULL OR prev_reading_value IS NULL) AND read_at >= :start AND read_at < :end
                """, new MapSqlParameterSource()
                .addValue("start", timestampWithTimeZone(start))
                .addValue("end", timestampWithTimeZone(end)), Long.class);
        return value == null ? 0L : value;
    }

    @Override
    public java.util.List<AnalyticsRecords.AbnormalReadingRow> listAbnormalReadings(
            java.time.Instant start, java.time.Instant end, int limit, int offset,
            java.util.Collection<String> projectIds) {
        if (projectIds != null && projectIds.isEmpty()) return List.of();
        MapSqlParameterSource params = withProjects(new MapSqlParameterSource()
                .addValue("start", timestampWithTimeZone(start))
                .addValue("end", timestampWithTimeZone(end))
                .addValue("limit", Math.min(Math.max(limit, 1), 200))
                .addValue("offset", Math.max(offset, 0)), projectIds);
        return jdbcTemplate.query("""
                SELECT r.id, r.meter_id, m.name AS meter_name, m.meter_type, r.period,
                       r.consumption, r.abnormal_reason, r.read_at
                  FROM energy_meter_readings r
                  JOIN energy_meters m ON m.id = r.meter_id
                 WHERE (r.abnormal_reason IS NOT NULL OR r.prev_reading_value IS NULL) AND r.read_at >= :start AND r.read_at < :end
                   AND (:all_projects OR m.project_id IN (:project_ids))
                 ORDER BY r.read_at DESC, r.id
                 LIMIT :limit OFFSET :offset
                """, params, (rs, n) -> new AnalyticsRecords.AbnormalReadingRow(
                rs.getString("id"), rs.getString("meter_id"), rs.getString("meter_name"),
                rs.getString("meter_type"), rs.getString("period"), rs.getBigDecimal("consumption"),
                rs.getString("abnormal_reason"), rs.getTimestamp("read_at").toInstant()));
    }

    @Override
    public Map<String, Map<String, BigDecimal>> arOverdueAgingByMerchant(LocalDate asOf) {
        Map<String, Map<String, BigDecimal>> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT CASE WHEN (:as_of - due_date) <= 30 THEN 'a0_30'
                            WHEN (:as_of - due_date) <= 60 THEN 'a31_60'
                            WHEN (:as_of - due_date) <= 90 THEN 'a61_90'
                            ELSE 'a90p' END AS bucket,
                       COALESCE(merchant_id, counterparty_id) AS merchant_key, SUM(unpaid_amount_cent) AS total
                  FROM ar_ap_items
                 WHERE direction = 'receivable'
                   AND status <> 'closed'
                   AND unpaid_amount_cent > 0
                   AND due_date < :as_of
                 GROUP BY 1, COALESCE(merchant_id, counterparty_id)
                """, new MapSqlParameterSource().addValue("as_of", asOf),
                (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                        result.computeIfAbsent(rs.getString("bucket"), k -> new LinkedHashMap<>())
                              .put(rs.getString("merchant_key"), rs.getBigDecimal("total")));
        return result;
    }

    @Override
    public BigDecimal arCollectedCentsBetween(Instant start, Instant end) {
        BigDecimal value = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(p.amount_cent), 0)
                  FROM ar_ap_payments p
                  JOIN ar_ap_items i ON i.id = p.ar_ap_item_id
                 WHERE i.direction = 'receivable'
                   AND p.payment_time >= :start AND p.payment_time < :end
                """, new MapSqlParameterSource()
                .addValue("start", timestampWithTimeZone(start))
                .addValue("end", timestampWithTimeZone(end)), BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    public BigDecimal arReceivableDueCentsBetween(LocalDate monthStart, LocalDate endExclusive) {
        BigDecimal value = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount_cent), 0)
                  FROM ar_ap_items
                 WHERE direction = 'receivable'
                   AND due_date >= :from AND due_date < :to
                """, new MapSqlParameterSource()
                .addValue("from", monthStart).addValue("to", endExclusive), BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    public java.util.List<AnalyticsRecords.ArDebtorRow> listOverdueDebtors(LocalDate asOf, String bucket, int limit, int offset,
            java.util.Collection<String> projectIds) {
        if (projectIds != null && projectIds.isEmpty()) return List.of();
        MapSqlParameterSource params = withProjects(new MapSqlParameterSource()
                .addValue("as_of", asOf)
                .addValue("bucket", bucket, java.sql.Types.VARCHAR)
                .addValue("limit", Math.min(Math.max(limit, 1), 200))
                .addValue("offset", Math.max(offset, 0)), projectIds);
        return jdbcTemplate.query("""
                SELECT COALESCE(i.merchant_id, i.counterparty_id) AS merchant_key, m.merchant_name,
                       SUM(i.unpaid_amount_cent) AS outstanding,
                       MIN(i.due_date) AS oldest_due,
                       (:as_of - MIN(i.due_date)) AS overdue_days,
                       CASE WHEN (:as_of - MIN(i.due_date)) <= 30 THEN 'a0_30'
                            WHEN (:as_of - MIN(i.due_date)) <= 60 THEN 'a31_60'
                            WHEN (:as_of - MIN(i.due_date)) <= 90 THEN 'a61_90'
                            ELSE 'a90p' END AS bucket
                  FROM ar_ap_items i
                  LEFT JOIN merchants m ON m.id = COALESCE(i.merchant_id, i.counterparty_id)
                 WHERE i.direction = 'receivable'
                   AND i.status <> 'closed'
                   AND i.unpaid_amount_cent > 0
                   AND i.due_date < :as_of
                   AND (:all_projects OR i.project_id IN (:project_ids))
                 GROUP BY COALESCE(i.merchant_id, i.counterparty_id), m.merchant_name
                HAVING (:bucket IS NULL
                        OR CASE WHEN (:as_of - MIN(i.due_date)) <= 30 THEN 'a0_30'
                                WHEN (:as_of - MIN(i.due_date)) <= 60 THEN 'a31_60'
                                WHEN (:as_of - MIN(i.due_date)) <= 90 THEN 'a61_90'
                                ELSE 'a90p' END = :bucket)
                 ORDER BY outstanding DESC, merchant_key
                 LIMIT :limit OFFSET :offset
                """, params, (rs, n) -> new AnalyticsRecords.ArDebtorRow(
                rs.getString("merchant_key"),
                rs.getString("merchant_name"),
                rs.getBigDecimal("outstanding"),
                rs.getObject("oldest_due", LocalDate.class),
                rs.getLong("overdue_days"),
                rs.getString("bucket")));
    }

    @Override
    public java.util.List<AnalyticsRecords.BusinessDayCutoffRecord> listBusinessDayCutoffs() {
        return jdbcTemplate.query("""
                SELECT domain, cutoff_hour, updated_at, updated_by
                  FROM business_day_cutoff_config ORDER BY domain
                """, Map.of(), (rs, n) -> new AnalyticsRecords.BusinessDayCutoffRecord(
                rs.getString("domain"), rs.getInt("cutoff_hour"),
                rs.getTimestamp("updated_at").toInstant(), rs.getString("updated_by")));
    }

    @Override
    public AnalyticsRecords.BusinessDayCutoffRecord upsertBusinessDayCutoff(String domain, int cutoffHour, String updatedBy, Instant now) {
        jdbcTemplate.update("""
                INSERT INTO business_day_cutoff_config (domain, cutoff_hour, updated_at, updated_by)
                VALUES (:domain, :cutoff_hour, :now, :updated_by)
                ON CONFLICT (domain) DO UPDATE SET
                    cutoff_hour = EXCLUDED.cutoff_hour, updated_at = EXCLUDED.updated_at, updated_by = EXCLUDED.updated_by
                """, new MapSqlParameterSource()
                .addValue("domain", domain)
                .addValue("cutoff_hour", cutoffHour)
                .addValue("now", timestampWithTimeZone(now))
                .addValue("updated_by", updatedBy, java.sql.Types.VARCHAR));
        return new AnalyticsRecords.BusinessDayCutoffRecord(domain, cutoffHour, now, updatedBy);
    }

    private MapSqlParameterSource dayRange(LocalDate businessDate) {
        return dayRange(null, businessDate);
    }

    private MapSqlParameterSource dayRange(String windowDomain, LocalDate businessDate) {
        // 经营日窗口按窗口域可配日切派生（V201；domain 为空=全局 04:00 基准）：[经营日 00:00+cutoff, 次日 00:00+cutoff)。
        Instant start = businessDayResolver.startInstant(windowDomain, businessDate);
        Instant end = businessDayResolver.endInstant(windowDomain, businessDate);
        return new MapSqlParameterSource()
                .addValue("start", timestampWithTimeZone(start))
                .addValue("end", timestampWithTimeZone(end));
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
                instant(resultSet, "resolved_at"),
                resultSet.getString("disposition_note"),
                resultSet.getString("disposed_by"),
                instant(resultSet, "disposed_at"),
                resultSet.getString("escalated_alert_id")
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

    @Override
    public BusinessReviewLockRecord saveReviewLock(BusinessReviewLockRecord lock) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", lock.id())
                .addValue("project_id", lock.projectId())
                .addValue("domain", lock.domain())
                .addValue("operator_note", lock.operatorNote())
                .addValue("metric_count", lock.metricCount())
                .addValue("locked_by", lock.lockedBy())
                .addValue("locked_at", java.sql.Timestamp.from(lock.lockedAt()))
                .addValue("idempotency_key", lock.idempotencyKey());
        jdbcTemplate.update("""
                INSERT INTO business_review_locks (
                    id, project_id, domain, operator_note, metric_count,
                    locked_by, locked_at, idempotency_key
                )
                VALUES (
                    :id, :project_id, :domain, :operator_note, :metric_count,
                    :locked_by, :locked_at, :idempotency_key
                )
                """, params);
        return lock;
    }

    @Override
    public List<BusinessReviewLockRecord> listReviewLocks(String projectScope) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("project_scope", projectScope);
        return jdbcTemplate.query("""
                SELECT id, project_id, domain, operator_note, metric_count,
                       locked_by, locked_at, idempotency_key
                  FROM business_review_locks
                 WHERE (:project_scope::text IS NULL OR project_id = :project_scope)
                 ORDER BY locked_at DESC, id
                """, params, this::mapReviewLock);
    }

    private BusinessReviewLockRecord mapReviewLock(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BusinessReviewLockRecord(
                resultSet.getString("id"),
                resultSet.getString("project_id"),
                resultSet.getString("domain"),
                resultSet.getString("operator_note"),
                resultSet.getInt("metric_count"),
                resultSet.getString("locked_by"),
                instant(resultSet, "locked_at"),
                resultSet.getString("idempotency_key")
        );
    }

    // ==== KPI 统一明细（指标登记簿，2026-07-18 方案A）====
    // 铁律：每个指标「当前页明细行」与「合计校验值」共用同一 WHERE 谓词 → 数字=明细合计按构造成立。

    @Override
    public AnalyticsRecords.MetricDetailRows ticketRevenueDetail(Instant start, Instant end, int limit, int offset) {
        MapSqlParameterSource params = pagedRangeParams(start, end, limit, offset);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, order_no, quantity, sales_channel, amount_cent, created_at, paid_at
                  FROM ticket_orders
                 WHERE pay_status = 'paid'
                   AND created_at >= :start
                   AND created_at < :end
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, (rs, n) -> {
            java.util.Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getString("id"));
            row.put("orderNo", rs.getString("order_no"));
            row.put("quantity", rs.getInt("quantity"));
            row.put("salesChannel", rs.getString("sales_channel"));
            row.put("amountCent", rs.getLong("amount_cent"));
            row.put("createdAt", instant(rs, "created_at"));
            row.put("paidAt", instant(rs, "paid_at"));
            return row;
        });
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COALESCE(SUM(amount_cent), 0) AS summary
                  FROM ticket_orders
                 WHERE pay_status = 'paid'
                   AND created_at >= :start
                   AND created_at < :end
                """, params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows reconciliationDifferenceDetail(Instant start, Instant end, int limit, int offset) {
        // start/end 可空=全历史（与对账差异页「对账差异」合计口径一致）。
        String window = start == null ? "" : "WHERE created_at >= :start AND created_at < :end";
        MapSqlParameterSource params = start == null
                ? pagedParams(limit, offset)
                : pagedRangeParams(start, end, limit, offset);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, difference_type, amount_cent, source, status, created_at
                  FROM finance_reconciliation_differences
                 %s
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """.formatted(window), params, (rs, n) -> {
            java.util.Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getString("id"));
            row.put("differenceType", rs.getString("difference_type"));
            row.put("amountCent", rs.getLong("amount_cent"));
            row.put("source", rs.getString("source"));
            row.put("status", rs.getString("status"));
            row.put("createdAt", instant(rs, "created_at"));
            return row;
        });
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COUNT(*) AS summary
                  FROM finance_reconciliation_differences
                 %s
                """.formatted(window), params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows electricityReadingDetail(Instant start, Instant end, int limit, int offset) {
        MapSqlParameterSource params = pagedRangeParams(start, end, limit, offset);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT r.id, m.name AS meter_name, r.period, r.effective_consumption AS consumption, r.abnormal_reason, r.read_at
                  FROM energy_effective_meter_readings r
                  JOIN energy_meters m ON m.id = r.meter_id
                 WHERE m.meter_type = 'electricity' AND r.status='reviewed'
                   AND r.read_at >= :start
                   AND r.read_at < :end
                 ORDER BY r.read_at DESC, r.id
                 LIMIT :limit OFFSET :offset
                """, params, (rs, n) -> {
            java.util.Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getString("id"));
            row.put("meterName", rs.getString("meter_name"));
            row.put("period", rs.getString("period"));
            row.put("consumption", rs.getBigDecimal("consumption"));
            row.put("abnormalReason", rs.getString("abnormal_reason"));
            row.put("readAt", instant(rs, "read_at"));
            return row;
        });
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COALESCE(SUM(r.effective_consumption), 0) AS summary
                  FROM energy_effective_meter_readings r
                  JOIN energy_meters m ON m.id = r.meter_id
                 WHERE m.meter_type = 'electricity' AND r.status='reviewed'
                   AND r.read_at >= :start
                   AND r.read_at < :end
                """, params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows rentableSpaceDetail(int limit, int offset) {
        // 能耗沿用可租面积：shop/booth 且非 unavailable；出租率另用含不可租的总面积。
        MapSqlParameterSource params = pagedParams(limit, offset);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, space_name, space_type, floor_no, area_square_meter
                  FROM spaces
                 WHERE space_type IN ('shop','booth')
                   AND status <> 'unavailable'
                 ORDER BY area_square_meter DESC NULLS LAST, id
                 LIMIT :limit OFFSET :offset
                """, params, (rs, n) -> {
            java.util.Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getString("id"));
            row.put("spaceName", rs.getString("space_name"));
            row.put("spaceType", rs.getString("space_type"));
            row.put("floorNo", rs.getString("floor_no"));
            row.put("areaSquareMeter", rs.getBigDecimal("area_square_meter"));
            return row;
        });
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COALESCE(SUM(area_square_meter), 0) AS summary
                  FROM spaces
                 WHERE space_type IN ('shop','booth')
                   AND status <> 'unavailable'
                """, params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows arOpenReceivableDetail(LocalDate asOf, int limit, int offset, java.util.Collection<String> projectScope) {
        if (projectScope != null && projectScope.isEmpty()) {
            return new AnalyticsRecords.MetricDetailRows(List.of(), 0, BigDecimal.ZERO);
        }
        // 与 ETL ar.balance（arOverdueAgingByMerchant）同谓词：应收、未关闭、未收>0、到期日早于截至日。
        // 项目作用域（2026-07-19；M737 起为可触及项目的集合）：scope 非 null 按 project_id 落在集合内过滤（同 FinanceService ar_ap 列表口径），null=平台全量。
        MapSqlParameterSource params = withProjects(pagedParams(limit, offset).addValue("as_of", asOf), projectScope);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, COALESCE(merchant_id, counterparty_id) AS merchant_key, source_type,
                       due_date, unpaid_amount_cent, status
                  FROM ar_ap_items
                 WHERE direction = 'receivable'
                   AND status <> 'closed'
                   AND unpaid_amount_cent > 0
                   AND due_date < :as_of
                   AND (:all_projects OR project_id IN (:project_ids))
                 ORDER BY due_date, id
                 LIMIT :limit OFFSET :offset
                """, params, (rs, n) -> {
            java.util.Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getString("id"));
            row.put("merchantId", rs.getString("merchant_key"));
            row.put("sourceType", rs.getString("source_type"));
            row.put("dueDate", rs.getObject("due_date", LocalDate.class));
            row.put("unpaidAmountCent", rs.getLong("unpaid_amount_cent"));
            row.put("status", rs.getString("status"));
            return row;
        });
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COALESCE(SUM(unpaid_amount_cent), 0) AS summary
                  FROM ar_ap_items
                 WHERE direction = 'receivable'
                   AND status <> 'closed'
                   AND unpaid_amount_cent > 0
                   AND due_date < :as_of
                   AND (:all_projects OR project_id IN (:project_ids))
                """, params));
    }

    // ==== KPI 统一明细·第二批（酒吧域）：谓词逐字同各页面既有取数 ====

    @Override
    public AnalyticsRecords.MetricDetailRows barSettlementRevenueDetail(Instant start, Instant end, int limit, int offset) {
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        LocalDate fromDate = businessDayResolver.businessDate("bar", start);
        LocalDate toDate = businessDayResolver.businessDate("bar", end.minusNanos(1)).plusDays(1);
        for (String merchant : barRevenueMerchants()) {
            for (var source : barRevenueSources.completedCollections(merchant, start, end)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", source.billId());
                row.put("billId", source.billId());
                row.put("merchantId", merchant);
                row.put("sourceType", "整单商品收入");
                row.put("settlementAmountCent", source.goodsRevenueCent());
                row.put("collectedAmountCent", source.collectedCent());
                row.put("serviceFeeCent", source.serviceFeeCent());
                row.put("tipCent", source.tipCent());
                row.put("statusLabel", "confirmed".equals(source.status()) ? "已确认" : "待确认");
                row.put("unresolvedSources", String.join("；", source.unresolvedSources()));
                row.put("settledAt", source.completedAt());
                rows.add(row);
            }
            for (var source : barRevenueSources.withheldServiceFees(merchant, fromDate, toDate)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", source.runId() == null ? "待结:" + source.sourceBillId() : source.runId());
                row.put("billId", source.sourceBillId());
                row.put("merchantId", merchant);
                row.put("sourceType", "月结服务费扣留");
                row.put("settlementAmountCent", source.amountCent());
                row.put("businessDate", source.businessDate());
                row.put("statusLabel", "confirmed".equals(source.status()) ? "已确认" : "待月结");
                row.put("unresolvedSources", String.join("；", source.unresolvedSources()));
                rows.add(row);
            }
        }
        BigDecimal summary = rows.stream().map(row -> (Long) row.get("settlementAmountCent"))
                .filter(java.util.Objects::nonNull).map(BigDecimal::valueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AnalyticsRecords.MetricDetailRows(rows.stream().skip(offset).limit(limit).toList(), rows.size(), summary);
    }

    @Override
    public AnalyticsRecords.MetricDetailRows barOrderDetail(Instant start, Instant end, boolean sumAmount, int limit, int offset) {
        // start/end 可空=全历史（与订单/出品/账单页、销售提成页的全量列表 listOrders 平台口径同谓词：无 WHERE 全状态）。
        String window = start == null ? "" : "WHERE created_at >= :start AND created_at < :end";
        MapSqlParameterSource params = start == null
                ? pagedParams(limit, offset)
                : pagedRangeParams(start, end, limit, offset);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, table_id, merchant_id, status, total_amount_cent, created_at, paid_at
                  FROM bar_orders
                 %s
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """.formatted(window), params, (rs, n) -> barOrderRow(rs));
        String summaryExpr = sumAmount ? "COALESCE(SUM(total_amount_cent), 0)" : "COUNT(*)";
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, %s AS summary
                  FROM bar_orders
                 %s
                """.formatted(summaryExpr, window), params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows barOpenOrderDetail(Instant asOfEnd, int limit, int offset) {
        // 与订单/出品/账单页「未完结」同谓词：status ∉ {paid, cancelled}（状态按台账当前值；截至日仅限制下单时间）。
        MapSqlParameterSource params = pagedParams(limit, offset)
                .addValue("as_of_end", timestampWithTimeZone(asOfEnd));
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, table_id, merchant_id, status, total_amount_cent, created_at, paid_at
                  FROM bar_orders
                 WHERE status NOT IN ('paid', 'cancelled')
                   AND created_at < :as_of_end
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, (rs, n) -> barOrderRow(rs));
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COUNT(*) AS summary
                  FROM bar_orders
                 WHERE status NOT IN ('paid', 'cancelled')
                   AND created_at < :as_of_end
                """, params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows savedBottleDetail(Instant start, Instant end, int limit, int offset) {
        // start/end 可空=全历史（与存酒页全量列表 listSavedBottles 平台口径同谓词：无 WHERE 全状态）。
        String window = start == null ? "" : "WHERE created_at >= :start AND created_at < :end";
        MapSqlParameterSource params = start == null
                ? pagedParams(limit, offset)
                : pagedRangeParams(start, end, limit, offset);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query(SAVED_BOTTLE_SELECT + """
                 %s
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """.formatted(window), params, (rs, n) -> savedBottleRow(rs));
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COUNT(*) AS summary
                  FROM saved_bottles
                 %s
                """.formatted(window), params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows savedBottleStoredDetail(Instant asOfEnd, int limit, int offset) {
        // 与存酒页「在存」同谓词：status='stored'（状态按台账当前值；截至日仅限制存入时间）。
        MapSqlParameterSource params = pagedParams(limit, offset)
                .addValue("as_of_end", timestampWithTimeZone(asOfEnd));
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query(SAVED_BOTTLE_SELECT + """
                 WHERE status = 'stored'
                   AND created_at < :as_of_end
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, (rs, n) -> savedBottleRow(rs));
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COUNT(*) AS summary
                  FROM saved_bottles
                 WHERE status = 'stored'
                   AND created_at < :as_of_end
                """, params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows savedBottleQuantityDetail(Instant asOfEnd, int limit, int offset) {
        // 与存酒页「剩余数量」同口径：全部登记行当前 remaining_quantity 直加（含已领完=0行）。
        MapSqlParameterSource params = pagedParams(limit, offset)
                .addValue("as_of_end", timestampWithTimeZone(asOfEnd));
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query(SAVED_BOTTLE_SELECT + """
                 WHERE created_at < :as_of_end
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, (rs, n) -> savedBottleRow(rs));
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COALESCE(SUM(remaining_quantity), 0) AS summary
                  FROM saved_bottles
                 WHERE created_at < :as_of_end
                """, params));
    }

    // ==== KPI 统一明细·第三批（票务域）：谓词逐字同各页面既有取数 ====

    @Override
    public AnalyticsRecords.MetricDetailRows ticketOrderDetail(Instant start, Instant end, boolean paidOnly, int limit, int offset) {
        // start/end 可空=全历史（与 GET /ticketing/orders 平台口径 listTicketOrders 同谓词：无窗全状态）。
        // paidOnly=true 加 pay_status='paid'（与页面「已支付」卡 orders.filter(payStatus==='paid') 同谓词）。
        String base = paidOnly ? "pay_status = 'paid'" : null;
        String where = composeWhere(base, start != null);
        MapSqlParameterSource params = start == null ? pagedParams(limit, offset) : pagedRangeParams(start, end, limit, offset);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, order_no, ticket_type_id, quantity, sales_channel, amount_cent,
                       pay_status, issue_status, created_at
                  FROM ticket_orders
                 %s
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """.formatted(where), params, (rs, n) -> ticketOrderRow(rs));
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COUNT(*) AS summary
                  FROM ticket_orders
                 %s
                """.formatted(where), params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows ticketPendingIssueDetail(Instant asOfEnd, int limit, int offset) {
        // 与出票/核销追踪页「待出票」同谓词：issue_status<>'issued'（状态按台账当前值；截至日仅限制下单时间）。
        MapSqlParameterSource params = pagedParams(limit, offset).addValue("as_of_end", timestampWithTimeZone(asOfEnd));
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, order_no, ticket_type_id, quantity, sales_channel, amount_cent,
                       pay_status, issue_status, created_at
                  FROM ticket_orders
                 WHERE issue_status <> 'issued'
                   AND created_at < :as_of_end
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, (rs, n) -> ticketOrderRow(rs));
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COUNT(*) AS summary
                  FROM ticket_orders
                 WHERE issue_status <> 'issued'
                   AND created_at < :as_of_end
                """, params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows amusementSessionDetail(Instant start, Instant end, boolean sumCapacity, int limit, int offset) {
        // start/end 可空=全历史（与 GET /amusement/sessions 平台口径 listSessions 同谓词：无窗全状态）。
        String where = composeWhere(null, start != null);
        MapSqlParameterSource params = start == null ? pagedParams(limit, offset) : pagedRangeParams(start, end, limit, offset);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, project_id, start_time, end_time, capacity, used_capacity, status, created_at
                  FROM amusement_sessions
                 %s
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """.formatted(where), params, (rs, n) -> amusementSessionRow(rs));
        String summaryExpr = sumCapacity ? "COALESCE(SUM(capacity), 0)" : "COUNT(*)";
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, %s AS summary
                  FROM amusement_sessions
                 %s
                """.formatted(summaryExpr, where), params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows amusementGroupBookingDetail(Instant start, Instant end, boolean sumReserved, int limit, int offset) {
        // start/end 可空=全历史（与 GET /amusement/group-bookings 平台口径 listGroupBookings 同谓词：无窗全状态）。
        String where = composeWhere(null, start != null);
        MapSqlParameterSource params = start == null ? pagedParams(limit, offset) : pagedRangeParams(start, end, limit, offset);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, project_id, session_id, booking_type, customer_name, party_size,
                       reserved_capacity, booking_status, created_at, paid_at
                  FROM amusement_group_bookings
                 %s
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """.formatted(where), params, (rs, n) -> amusementGroupBookingRow(rs));
        String summaryExpr = sumReserved ? "COALESCE(SUM(reserved_capacity), 0)" : "COUNT(*)";
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, %s AS summary
                  FROM amusement_group_bookings
                 %s
                """.formatted(summaryExpr, where), params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows amusementVerificationDirectionDetail(Instant start, Instant end, boolean entry, int limit, int offset) {
        // 与游乐核销记录页「入场/出场」卡同谓词：entry=(direction='entry' 或空)、exit=direction='exit'（含全部结果，页面不过滤 result）。
        // start/end 可空=全历史（与页面默认无筛选全量列表一致）。
        String base = entry ? "(direction = 'entry' OR direction IS NULL)" : "direction = 'exit'";
        String where = composeWhere(base, start != null);
        MapSqlParameterSource params = start == null ? pagedParams(limit, offset) : pagedRangeParams(start, end, limit, offset);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, project_id, session_id, direction, verification_result, verified_people, created_at
                  FROM amusement_verifications
                 %s
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """.formatted(where), params, (rs, n) -> amusementVerificationRow(rs));
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COUNT(*) AS summary
                  FROM amusement_verifications
                 %s
                """.formatted(where), params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows amusementInspectionDetail(Instant start, Instant end, int limit, int offset) {
        // start/end 可空=全历史（与 GET /amusement/safety-inspections 平台口径 listSafetyInspections 同谓词：无窗全状态）。
        String where = composeWhere(null, start != null);
        MapSqlParameterSource params = start == null ? pagedParams(limit, offset) : pagedRangeParams(start, end, limit, offset);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, project_id, device_id, overall_result, inspection_status,
                       rectification_status, reopen_status, created_at
                  FROM amusement_safety_inspections
                 %s
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """.formatted(where), params, (rs, n) -> amusementSafetyRow(rs));
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COUNT(*) AS summary
                  FROM amusement_safety_inspections
                 %s
                """.formatted(where), params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows amusementInspectionPendingRectificationDetail(Instant asOfEnd, int limit, int offset) {
        // 与安全巡检页「待整改」同谓词：COALESCE(rectification_status,overall_result)∈待整改白名单（状态按台账当前值；截至日仅限制创建时间）。
        MapSqlParameterSource params = pagedParams(limit, offset).addValue("as_of_end", timestampWithTimeZone(asOfEnd));
        String predicate = """
                WHERE COALESCE(rectification_status, overall_result) IN ('pending', 'rectification_pending', 'pending_rectification')
                   AND created_at < :as_of_end
                """;
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, project_id, device_id, overall_result, inspection_status,
                       rectification_status, reopen_status, created_at
                  FROM amusement_safety_inspections
                 %s
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """.formatted(predicate), params, (rs, n) -> amusementSafetyRow(rs));
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COUNT(*) AS summary
                  FROM amusement_safety_inspections
                 %s
                """.formatted(predicate), params));
    }

    // ==== KPI 统一明细·第四批（财务域）：谓词逐字同各页面既有取数 ====

    @Override
    public AnalyticsRecords.MetricDetailRows apOpenPayableDetail(LocalDate asOf, int limit, int offset, java.util.Collection<String> projectScope) {
        return apOpenPayableDetail(asOf, limit, offset, projectScope, null);
    }

    @Override
    public AnalyticsRecords.MetricDetailRows apOpenPayableDetail(LocalDate asOf, int limit, int offset,
            java.util.Collection<String> projectScope, java.util.Set<String> payrollItemIds) {
        if (projectScope != null && projectScope.isEmpty()) {
            return new AnalyticsRecords.MetricDetailRows(List.of(), 0, BigDecimal.ZERO);
        }
        // 应付镜像：与 arOpenReceivableDetail 同结构，方向取 payable（as-of=to，起始日不参与）。scope 非 null 按 project_id 落在集合内过滤。
        MapSqlParameterSource params = withProjects(pagedParams(limit, offset).addValue("as_of", asOf), projectScope)
                .addValue("all_payroll", payrollItemIds == null)
                .addValue("payroll_ids", payrollItemIds == null || payrollItemIds.isEmpty() ? java.util.Set.of("__none__") : payrollItemIds);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, COALESCE(merchant_id, counterparty_id) AS merchant_key, source_type,
                       due_date, unpaid_amount_cent, status
                  FROM ar_ap_items
                 WHERE direction = 'payable'
                   AND status <> 'closed'
                   AND unpaid_amount_cent > 0
                   AND due_date < :as_of
                   AND (:all_projects OR project_id IN (:project_ids))
                   AND (source_type <> 'staff_payroll' OR :all_payroll OR id IN (:payroll_ids))
                 ORDER BY due_date, id
                 LIMIT :limit OFFSET :offset
                """, params, (rs, n) -> arApBalanceRow(rs));
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COALESCE(SUM(unpaid_amount_cent), 0) AS summary
                  FROM ar_ap_items
                 WHERE direction = 'payable'
                   AND status <> 'closed'
                   AND unpaid_amount_cent > 0
                   AND due_date < :as_of
                   AND (:all_projects OR project_id IN (:project_ids))
                   AND (source_type <> 'staff_payroll' OR :all_payroll OR id IN (:payroll_ids))
                """, params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows arOverdueAging90pDetail(LocalDate asOf, int limit, int offset, java.util.Collection<String> projectScope) {
        if (projectScope != null && projectScope.isEmpty()) {
            return new AnalyticsRecords.MetricDetailRows(List.of(), 0, BigDecimal.ZERO);
        }
        // 与 ETL ar.aging_90p（arOverdueAgingByMerchant 最末桶：(:as_of - due_date) > 90）同谓词。scope 非 null 按 project_id 落在集合内过滤。
        MapSqlParameterSource params = withProjects(pagedParams(limit, offset).addValue("as_of", asOf), projectScope);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, COALESCE(merchant_id, counterparty_id) AS merchant_key, source_type,
                       due_date, unpaid_amount_cent, status
                  FROM ar_ap_items
                 WHERE direction = 'receivable'
                   AND status <> 'closed'
                   AND unpaid_amount_cent > 0
                   AND due_date < :as_of
                   AND (:as_of - due_date) > 90
                   AND (:all_projects OR project_id IN (:project_ids))
                 ORDER BY due_date, id
                 LIMIT :limit OFFSET :offset
                """, params, (rs, n) -> arApBalanceRow(rs));
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COALESCE(SUM(unpaid_amount_cent), 0) AS summary
                  FROM ar_ap_items
                 WHERE direction = 'receivable'
                   AND status <> 'closed'
                   AND unpaid_amount_cent > 0
                   AND due_date < :as_of
                   AND (:as_of - due_date) > 90
                   AND (:all_projects OR project_id IN (:project_ids))
                """, params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows settlementTransactionDetail(Instant start, Instant end, String summaryMode, int limit, int offset, java.util.Collection<String> projectScope) {
        if (projectScope != null && projectScope.isEmpty()) {
            return new AnalyticsRecords.MetricDetailRows(List.of(), 0, BigDecimal.ZERO);
        }
        // 与通票结算审阅页 listVerificationSettled 同基础谓词：transaction_type='verification_settled'（默认无商户筛选）。
        // start/end 可空=全历史（与页面默认「共 N 条」一致）；有窗则按 created_at 落窗。
        // 项目作用域（2026-07-19；M737 起为可触及项目的集合）：scope 非 null 按 project_id 落在集合内过滤（settlement_transactions.project_id=综合体，同结算审阅页项目筛选口径），null=平台全量。
        String window = start == null ? "" : "AND created_at >= :start AND created_at < :end";
        String scopePredicate = "AND (:all_projects OR project_id IN (:project_ids))";
        MapSqlParameterSource params = withProjects(start == null
                ? pagedParams(limit, offset)
                : pagedRangeParams(start, end, limit, offset), projectScope);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, transaction_no, project_id, merchant_id, entitlement_type,
                       verified_count, num_people, verified_amount_cent, settlement_status, created_at
                  FROM settlement_transactions
                 WHERE transaction_type = 'verification_settled'
                 %s
                 %s
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """.formatted(window, scopePredicate), params, (rs, n) -> settlementRow(rs));
        String summaryExpr = switch (summaryMode) {
            case "amount" -> "COALESCE(SUM(verified_amount_cent), 0)";
            case "tickets" -> "COALESCE(SUM(verified_count), 0)";
            default -> "COUNT(*)";
        };
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, %s AS summary
                  FROM settlement_transactions
                 WHERE transaction_type = 'verification_settled'
                 %s
                 %s
                """.formatted(summaryExpr, window, scopePredicate), params));
    }

    private java.util.Map<String, Object> arApBalanceRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.util.Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getString("id"));
        row.put("merchantId", rs.getString("merchant_key"));
        row.put("sourceType", rs.getString("source_type"));
        row.put("dueDate", rs.getObject("due_date", LocalDate.class));
        row.put("unpaidAmountCent", rs.getLong("unpaid_amount_cent"));
        row.put("status", rs.getString("status"));
        return row;
    }

    private java.util.Map<String, Object> settlementRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.util.Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getString("id"));
        row.put("transactionNo", rs.getString("transaction_no"));
        row.put("projectId", rs.getString("project_id"));
        row.put("merchantId", rs.getString("merchant_id"));
        row.put("entitlementType", rs.getString("entitlement_type"));
        row.put("verifiedCount", rs.getInt("verified_count"));
        row.put("numPeople", rs.getInt("num_people"));
        row.put("verifiedAmountCent", rs.getLong("verified_amount_cent"));
        row.put("settlementStatus", rs.getString("settlement_status"));
        row.put("createdAt", instant(rs, "created_at"));
        return row;
    }

    /** 组装可选经营日窗谓词：base=附加基础条件（可空）；hasWindow=true 追加 created_at 区间。均空则返回空串（全表）。 */
    private static String composeWhere(String base, boolean hasWindow) {
        String window = hasWindow ? "created_at >= :start AND created_at < :end" : null;
        if (base == null && window == null) {
            return "";
        }
        if (base != null && window != null) {
            return "WHERE " + base + " AND " + window;
        }
        return "WHERE " + (base != null ? base : window);
    }

    private java.util.Map<String, Object> ticketOrderRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.util.Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getString("id"));
        row.put("orderNo", rs.getString("order_no"));
        row.put("ticketTypeId", rs.getString("ticket_type_id"));
        row.put("quantity", rs.getInt("quantity"));
        row.put("salesChannel", rs.getString("sales_channel"));
        row.put("amountCent", rs.getLong("amount_cent"));
        row.put("payStatus", rs.getString("pay_status"));
        row.put("issueStatus", rs.getString("issue_status"));
        row.put("createdAt", instant(rs, "created_at"));
        return row;
    }

    private java.util.Map<String, Object> amusementSessionRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.util.Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getString("id"));
        row.put("projectId", rs.getString("project_id"));
        row.put("startTime", instant(rs, "start_time"));
        row.put("endTime", instant(rs, "end_time"));
        row.put("capacity", rs.getInt("capacity"));
        row.put("usedCapacity", rs.getInt("used_capacity"));
        row.put("status", rs.getString("status"));
        row.put("createdAt", instant(rs, "created_at"));
        return row;
    }

    private java.util.Map<String, Object> amusementGroupBookingRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.util.Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getString("id"));
        row.put("projectId", rs.getString("project_id"));
        row.put("sessionId", rs.getString("session_id"));
        row.put("bookingType", rs.getString("booking_type"));
        row.put("customerName", rs.getString("customer_name"));
        row.put("partySize", rs.getInt("party_size"));
        row.put("reservedCapacity", rs.getInt("reserved_capacity"));
        row.put("bookingStatus", rs.getString("booking_status"));
        row.put("createdAt", instant(rs, "created_at"));
        row.put("paidAt", instant(rs, "paid_at"));
        return row;
    }

    private java.util.Map<String, Object> amusementVerificationRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.util.Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getString("id"));
        row.put("projectId", rs.getString("project_id"));
        row.put("sessionId", rs.getString("session_id"));
        row.put("direction", rs.getString("direction"));
        row.put("verificationResult", rs.getString("verification_result"));
        row.put("verifiedPeople", rs.getInt("verified_people"));
        row.put("createdAt", instant(rs, "created_at"));
        return row;
    }

    private java.util.Map<String, Object> amusementSafetyRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.util.Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getString("id"));
        row.put("projectId", rs.getString("project_id"));
        row.put("deviceId", rs.getString("device_id"));
        row.put("overallResult", rs.getString("overall_result"));
        row.put("inspectionStatus", rs.getString("inspection_status"));
        row.put("rectificationStatus", rs.getString("rectification_status"));
        row.put("reopenStatus", rs.getString("reopen_status"));
        row.put("createdAt", instant(rs, "created_at"));
        return row;
    }

    private java.util.Map<String, Object> barOrderRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.util.Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getString("id"));
        row.put("tableId", rs.getString("table_id"));
        row.put("merchantId", rs.getString("merchant_id"));
        row.put("status", rs.getString("status"));
        row.put("totalAmountCent", rs.getLong("total_amount_cent"));
        row.put("createdAt", instant(rs, "created_at"));
        row.put("paidAt", instant(rs, "paid_at"));
        return row;
    }

    private java.util.Map<String, Object> savedBottleRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.util.Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getString("id"));
        row.put("bottleName", rs.getString("bottle_name"));
        row.put("initialQuantity", rs.getInt("initial_quantity"));
        row.put("remainingQuantity", rs.getInt("remaining_quantity"));
        row.put("unit", rs.getString("unit"));
        row.put("status", rs.getString("status"));
        row.put("createdAt", instant(rs, "created_at"));
        row.put("expireAt", instant(rs, "expire_at"));
        return row;
    }

    /** M737：null＝不过滤（钻取明细只对平台管理员传 null，M737-F07；指标明细里没有项目列的指标对勾了全部项目的人也传 null）；
     *  调用前空集合已提前返回。谓词写作 (:all_projects OR <列> IN (:project_ids))。 */
    private static MapSqlParameterSource withProjects(MapSqlParameterSource params, java.util.Collection<String> projectIds) {
        return params.addValue("all_projects", projectIds == null)
                .addValue("project_ids", projectIds == null ? List.of("") : projectIds);
    }

    private static MapSqlParameterSource pagedParams(int limit, int offset) {
        return new MapSqlParameterSource()
                .addValue("limit", Math.min(Math.max(limit, 1), 200))
                .addValue("offset", Math.max(offset, 0));
    }

    private MapSqlParameterSource pagedRangeParams(Instant start, Instant end, int limit, int offset) {
        // 防御断言：当前登记项凡走带窗查询必有窗（服务层已推导缺省经营日）；防未来登记项误传 null 变成空谓词。
        java.util.Objects.requireNonNull(start, "metric detail window start must not be null");
        java.util.Objects.requireNonNull(end, "metric detail window end must not be null");
        return pagedParams(limit, offset)
                .addValue("start", timestampWithTimeZone(start))
                .addValue("end", timestampWithTimeZone(end));
    }

    private static AnalyticsRecords.MetricDetailRows metricDetailRows(
            java.util.List<java.util.Map<String, Object>> rows, java.util.Map<String, Object> aggregate) {
        Object summary = aggregate.get("summary");
        return new AnalyticsRecords.MetricDetailRows(
                rows,
                ((Number) aggregate.get("total_rows")).longValue(),
                summary == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(summary)));
    }

    // ==== KPI 统一明细·第五批（人事域）：谓词逐字同各列表端点既有取数 ====

    @Override
    public AnalyticsRecords.MetricDetailRows staffShiftDetail(LocalDate from, LocalDate to, int limit, int offset, java.util.Collection<String> projectScope) {
        if (projectScope != null && projectScope.isEmpty()) {
            return new AnalyticsRecords.MetricDetailRows(List.of(), 0, BigDecimal.ZERO);
        }
        // 与 listShifts 同谓词：work_date 落 [from,to] 闭区间 + (null scope=平台全量 / 否则按 project_id 落在集合内过滤，同 listShifts 项目隔离口径)。
        java.util.Objects.requireNonNull(from, "staff shift detail start must not be null");
        java.util.Objects.requireNonNull(to, "staff shift detail end must not be null");
        MapSqlParameterSource params = withProjects(
                pagedParams(limit, offset).addValue("from", from).addValue("to", to), projectScope);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, user_id, work_date, start_at, end_at, note
                  FROM staff_shift_schedules
                 WHERE work_date >= :from AND work_date <= :to
                   AND (:all_projects OR project_id IN (:project_ids))
                 ORDER BY work_date DESC, start_at, user_id
                 LIMIT :limit OFFSET :offset
                """, params, (rs, n) -> staffShiftRow(rs));
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COUNT(*) AS summary
                  FROM staff_shift_schedules
                 WHERE work_date >= :from AND work_date <= :to
                   AND (:all_projects OR project_id IN (:project_ids))
                """, params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows staffAttendanceDetail(LocalDate from, LocalDate to, boolean lateOnly, int limit, int offset, java.util.Collection<String> projectScope) {
        if (projectScope != null && projectScope.isEmpty()) {
            return new AnalyticsRecords.MetricDetailRows(List.of(), 0, BigDecimal.ZERO);
        }
        // 与 listAttendanceOn 同自然日窗：punched_at ∈ [from 00:00, to+1 00:00) UTC（刻意不走经营日切）。lateOnly 加 judgement='late'。
        // 项目作用域：scope 非 null 按 project_id 落在集合内过滤（同 listAttendanceOn 项目隔离口径），null=平台全量。
        java.util.Objects.requireNonNull(from, "staff attendance detail start must not be null");
        java.util.Objects.requireNonNull(to, "staff attendance detail end must not be null");
        String lateClause = lateOnly ? "AND judgement = 'late'" : "";
        MapSqlParameterSource params = withProjects(pagedParams(limit, offset)
                .addValue("day_start", timestampWithTimeZone(from.atStartOfDay(java.time.ZoneOffset.UTC).toInstant()))
                .addValue("day_end", timestampWithTimeZone(to.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant())),
                projectScope);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, user_id, punch_type, punched_at, method, judgement, space_id
                  FROM staff_attendance_records
                 WHERE punched_at >= :day_start AND punched_at < :day_end
                   AND (:all_projects OR project_id IN (:project_ids))
                 %s
                 ORDER BY punched_at DESC, id
                 LIMIT :limit OFFSET :offset
                """.formatted(lateClause), params, (rs, n) -> staffAttendanceRow(rs));
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COUNT(*) AS summary
                  FROM staff_attendance_records
                 WHERE punched_at >= :day_start AND punched_at < :day_end
                   AND (:all_projects OR project_id IN (:project_ids))
                 %s
                """.formatted(lateClause), params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows staffPayrollDetail(LocalDate from, LocalDate to, boolean sumTotal, int limit, int offset, java.util.Collection<String> projectScope) {
        return staffPayrollDetail(from, to, sumTotal, limit, offset, projectScope, null);
    }

    @Override
    public AnalyticsRecords.MetricDetailRows staffPayrollDetail(LocalDate from, LocalDate to, boolean sumTotal, int limit,
            int offset, java.util.Collection<String> projectScope, java.util.Collection<String> statementIds) {
        if (projectScope != null && projectScope.isEmpty()) {
            return new AnalyticsRecords.MetricDetailRows(List.of(), 0, BigDecimal.ZERO);
        }
        // 与 listPayrollStatements 同谓词：month ∈ [fromMonth,toMonth]（YYYY-MM 字典序即时序，不加状态过滤）。
        // 项目作用域：scope 非 null 按 project_id 落在集合内过滤（同 listPayrollStatements 项目隔离口径），null=平台全量。
        java.util.Objects.requireNonNull(from, "staff payroll detail start must not be null");
        java.util.Objects.requireNonNull(to, "staff payroll detail end must not be null");
        java.time.format.DateTimeFormatter ym = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM");
        MapSqlParameterSource params = withProjects(pagedParams(limit, offset)
                .addValue("from_month", from.format(ym))
                .addValue("to_month", to.format(ym)),
                projectScope)
                .addValue("all_statements", statementIds == null)
                .addValue("statement_ids", statementIds == null || statementIds.isEmpty() ? java.util.List.of("__none__") : statementIds);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, user_id, month, base_cent, commission_cent, bonus_cent, penalty_cent, total_cent
                  FROM staff_payroll_statements
                 WHERE month >= :from_month AND month <= :to_month
                   AND (:all_projects OR project_id IN (:project_ids))
                   AND (:all_statements OR id IN (:statement_ids))
                 ORDER BY month DESC, user_id
                 LIMIT :limit OFFSET :offset
                """, params, (rs, n) -> staffPayrollRow(rs));
        String summaryExpr = sumTotal ? "COALESCE(SUM(total_cent), 0)" : "COUNT(*)";
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, %s AS summary
                  FROM staff_payroll_statements
                 WHERE month >= :from_month AND month <= :to_month
                   AND (:all_projects OR project_id IN (:project_ids))
                   AND (:all_statements OR id IN (:statement_ids))
                """.formatted(summaryExpr), params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows staffPendingLeaveDetail(LocalDate asOf, int limit, int offset, java.util.Collection<String> projectScope) {
        if (projectScope != null && projectScope.isEmpty()) {
            return new AnalyticsRecords.MetricDetailRows(List.of(), 0, BigDecimal.ZERO);
        }
        // 与 listLeaves(status='pending') 同谓词；截至日仅限制提交时间早于该自然日窗末（状态按台账当前值）。
        // 项目作用域：scope 非 null 按 project_id 落在集合内过滤（同 listLeaves 项目隔离口径），null=平台全量。
        java.util.Objects.requireNonNull(asOf, "staff pending leave detail asOf must not be null");
        MapSqlParameterSource params = withProjects(pagedParams(limit, offset)
                .addValue("as_of_end", timestampWithTimeZone(asOf.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant())),
                projectScope);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, user_id, leave_type, start_date, end_date, reason, status
                  FROM staff_leave_requests
                 WHERE status = 'pending' AND created_at < :as_of_end
                   AND (:all_projects OR project_id IN (:project_ids))
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, (rs, n) -> staffLeaveRow(rs));
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COUNT(*) AS summary
                  FROM staff_leave_requests
                 WHERE status = 'pending' AND created_at < :as_of_end
                   AND (:all_projects OR project_id IN (:project_ids))
                """, params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows staffActiveHeadcountDetail(LocalDate asOf, int limit, int offset, java.util.Collection<String> projectScope) {
        if (projectScope != null && projectScope.isEmpty()) {
            return new AnalyticsRecords.MetricDetailRows(List.of(), 0, BigDecimal.ZERO);
        }
        // 在职员工=account_type='staff' 且 status='active'（与员工账号页员工类启用账号一致）；截至日仅限制建号时间早于该自然日窗末。
        // 项目作用域：access_users 用 organization_id 作综合体列（同 AccessService.listUsers(organizationId) 项目隔离口径）；null scope=平台全量，否则按 organization_id 落在集合内过滤。
        java.util.Objects.requireNonNull(asOf, "staff headcount detail asOf must not be null");
        MapSqlParameterSource params = withProjects(pagedParams(limit, offset)
                .addValue("as_of_end", timestampWithTimeZone(asOf.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant())),
                projectScope);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, username, account_type, status, created_at
                  FROM access_users
                 WHERE account_type = 'staff' AND status = 'active' AND created_at < :as_of_end
                   AND (:all_projects OR organization_id IN (:project_ids))
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, (rs, n) -> staffHeadcountRow(rs));
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COUNT(*) AS summary
                  FROM access_users
                 WHERE account_type = 'staff' AND status = 'active' AND created_at < :as_of_end
                   AND (:all_projects OR organization_id IN (:project_ids))
                """, params));
    }

    private java.util.Map<String, Object> staffShiftRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.util.Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getString("id"));
        row.put("userId", rs.getString("user_id"));
        row.put("workDate", rs.getObject("work_date", LocalDate.class));
        row.put("startAt", instant(rs, "start_at"));
        row.put("endAt", instant(rs, "end_at"));
        row.put("note", rs.getString("note"));
        return row;
    }

    private java.util.Map<String, Object> staffAttendanceRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.util.Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getString("id"));
        row.put("userId", rs.getString("user_id"));
        row.put("punchType", rs.getString("punch_type"));
        row.put("punchedAt", instant(rs, "punched_at"));
        row.put("method", rs.getString("method"));
        row.put("judgement", rs.getString("judgement"));
        row.put("spaceId", rs.getString("space_id"));
        return row;
    }

    private java.util.Map<String, Object> staffPayrollRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.util.Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getString("id"));
        row.put("userId", rs.getString("user_id"));
        row.put("month", rs.getString("month"));
        row.put("baseCent", rs.getLong("base_cent"));
        row.put("commissionCent", rs.getLong("commission_cent"));
        row.put("bonusCent", rs.getLong("bonus_cent"));
        row.put("penaltyCent", rs.getLong("penalty_cent"));
        row.put("totalCent", rs.getLong("total_cent"));
        return row;
    }

    private java.util.Map<String, Object> staffLeaveRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.util.Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getString("id"));
        row.put("userId", rs.getString("user_id"));
        row.put("leaveType", rs.getString("leave_type"));
        row.put("startDate", rs.getObject("start_date", LocalDate.class));
        row.put("endDate", rs.getObject("end_date", LocalDate.class));
        row.put("reason", rs.getString("reason"));
        row.put("status", rs.getString("status"));
        return row;
    }

    private java.util.Map<String, Object> staffHeadcountRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.util.Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getString("id"));
        row.put("username", rs.getString("username"));
        row.put("accountType", rs.getString("account_type"));
        row.put("status", rs.getString("status"));
        row.put("createdAt", instant(rs, "created_at"));
        return row;
    }

    // ==== KPI 统一明细·第六批（总控驾驶舱剩余卡）：谓词逐字抄总览 ETL 聚合（DashboardAggregationJob）====

    @Override
    public AnalyticsRecords.MetricDetailRows amusementVerificationSuccessDetail(Instant start, Instant end, int limit, int offset) {
        // 谓词同 ETL countAmusementVerificationSuccess：verification_result='success'（入场+出场两方向）。start/end 可空=全历史。
        String where = composeWhere("verification_result = 'success'", start != null);
        MapSqlParameterSource params = start == null ? pagedParams(limit, offset) : pagedRangeParams(start, end, limit, offset);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, project_id, session_id, direction, verification_result, verified_people, created_at
                  FROM amusement_verifications
                 %s
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """.formatted(where), params, (rs, n) -> amusementVerificationRow(rs));
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COUNT(*) AS summary
                  FROM amusement_verifications
                 %s
                """.formatted(where), params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows ticketVerificationSuccessDetail(Instant start, Instant end, int limit, int offset) {
        // 谓词同 ETL countTicketVerificationSuccessByMerchant 平台合计：status='success'。start/end 可空=全历史。
        String where = composeWhere("status = 'success'", start != null);
        MapSqlParameterSource params = start == null ? pagedParams(limit, offset) : pagedRangeParams(start, end, limit, offset);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, verification_no, entitlement_code, status, source_type, created_at
                  FROM ticket_verifications
                 %s
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """.formatted(where), params, (rs, n) -> ticketVerificationRow(rs));
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COUNT(*) AS summary
                  FROM ticket_verifications
                 %s
                """.formatted(where), params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows newMemberDetail(Instant start, Instant end, int limit, int offset) {
        // 谓词同 ETL countNewMembers：建号落窗 + status NOT IN ('deregistered','blacklisted')。仅读脱敏列，不读 phone 明文。start/end 可空=全历史。
        String where = composeWhere("status NOT IN ('deregistered', 'blacklisted')", start != null);
        MapSqlParameterSource params = start == null ? pagedParams(limit, offset) : pagedRangeParams(start, end, limit, offset);
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, masked_phone, status, created_at, last_login_at
                  FROM customers
                 %s
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """.formatted(where), params, (rs, n) -> memberRow(rs));
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COUNT(*) AS summary
                  FROM customers
                 %s
                """.formatted(where), params));
    }

    @Override
    public AnalyticsRecords.MetricDetailRows pendingReviewDetail(LocalDate asOf, int limit, int offset) {
        // 谓词同 ETL countPendingReview（两源并计）：待复核主管审批（review_status='pending_review'）+ 未处置风险预警（status='open'）；
        // 余额口径：仅计入产生时间（评估/建立）早于截至日自然日窗末的条目，状态按台账当前值。
        java.util.Objects.requireNonNull(asOf, "pending review detail asOf must not be null");
        MapSqlParameterSource params = pagedParams(limit, offset)
                .addValue("as_of_end", timestampWithTimeZone(asOf.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()));
        String union = """
                SELECT '主管复核' AS source, review_id AS id, object_type, object_id,
                       policy_code AS detail, review_status AS status, evaluated_at AS created_at
                  FROM supervisor_review_decisions
                 WHERE review_status = 'pending_review' AND evaluated_at < :as_of_end
                UNION ALL
                SELECT '风险预警' AS source, id, object_type, object_id,
                       summary AS detail, status, created_at
                  FROM analytics_risk_alerts
                 WHERE status = 'open' AND created_at < :as_of_end
                """;
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT source, id, object_type, object_id, detail, status, created_at
                  FROM ( %s ) u
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """.formatted(union), params, (rs, n) -> pendingReviewRow(rs));
        return metricDetailRows(rows, jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total_rows, COUNT(*) AS summary
                  FROM ( %s ) u
                """.formatted(union), params));
    }

    private java.util.Map<String, Object> ticketVerificationRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.util.Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getString("id"));
        row.put("verificationNo", rs.getString("verification_no"));
        row.put("entitlementCode", rs.getString("entitlement_code"));
        row.put("status", rs.getString("status"));
        row.put("sourceType", rs.getString("source_type"));
        row.put("createdAt", instant(rs, "created_at"));
        return row;
    }

    private java.util.Map<String, Object> memberRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.util.Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getString("id"));
        row.put("maskedPhone", rs.getString("masked_phone"));
        row.put("status", rs.getString("status"));
        row.put("createdAt", instant(rs, "created_at"));
        row.put("lastLoginAt", instant(rs, "last_login_at"));
        return row;
    }

    private java.util.Map<String, Object> pendingReviewRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.util.Map<String, Object> row = new LinkedHashMap<>();
        row.put("source", rs.getString("source"));
        row.put("id", rs.getString("id"));
        row.put("objectType", rs.getString("object_type"));
        row.put("objectId", rs.getString("object_id"));
        row.put("detail", rs.getString("detail"));
        row.put("status", rs.getString("status"));
        row.put("createdAt", instant(rs, "created_at"));
        return row;
    }
}
