package com.complex.platform.analytics;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

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
import org.springframework.stereotype.Repository;

@Repository
class JdbcAnalyticsRepository implements AnalyticsRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

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

    // P2 多综合体 project_id scoped 读通道：按真实 project_id 列做行级隔离（AC-ISO-1，§4）。既有查询不变。
    public List<DashboardSnapshotRecord> listDashboardSnapshotsByProject(String projectId) {
        return jdbcTemplate.query("""
                SELECT snapshot_key, project_id, business_date, revenue_amount_cents,
                       order_count, risk_alert_count, critical_risk_count, payload_json, created_at
                  FROM analytics_dashboard_snapshots
                 WHERE project_id = :project_id
                 ORDER BY business_date DESC, created_at DESC, snapshot_key
                """, Map.of("project_id", projectId), this::mapDashboardSnapshot);
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

    // --- T12 看板真实聚合 / 行级隔离 ---

    @Override
    public Map<String, BigDecimal> readMetricSnapshots(LocalDate businessDate, String scope) {
        String segmentType = scope == null ? "platform" : "merchant";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("business_date", businessDate)
                .addValue("segment_type", segmentType)
                .addValue("segment_id", scope);
        String segmentFilter = scope == null ? "segment_id IS NULL" : "segment_id = :segment_id";
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT metric_key, actual_value
                  FROM analytics_business_metric_snapshots
                 WHERE business_date = :business_date
                   AND period_type = 'daily'
                   AND segment_type = :segment_type
                   AND %s
                """.formatted(segmentFilter), params, (org.springframework.jdbc.core.RowCallbackHandler) resultSet ->
                result.put(resultSet.getString("metric_key"), resultSet.getBigDecimal("actual_value"))
        );
        return result;
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
                 WHERE created_at >= :start
                   AND created_at < :end
                """, dayRange(windowDomain, businessDate), BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    public Map<String, BigDecimal> sumBarRevenueCentsByMerchant(LocalDate businessDate, String windowDomain) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT merchant_id, COALESCE(SUM(settlement_amount_cent), 0) AS total
                  FROM bar_bill_settlements
                 WHERE settled_at >= :start
                   AND settled_at < :end
                   AND reversed = FALSE
                 GROUP BY merchant_id
                """, dayRange(windowDomain, businessDate), (org.springframework.jdbc.core.RowCallbackHandler) resultSet ->
                result.put(resultSet.getString("merchant_id"), resultSet.getBigDecimal("total"))
        );
        return result;
    }

    @Override
    public Map<String, BigDecimal> sumBarRefundCentsByMerchant(LocalDate businessDate, String windowDomain) {
        // 待确认#2：酒吧结账后退款（adjustment_type='refund'）按 refunded_at 落经营日窗口，经 bar_bills 取 merchant_id。
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT b.merchant_id AS merchant_id, COALESCE(SUM(a.amount_cent), 0) AS total
                  FROM bar_bill_adjustments a
                  JOIN bar_bills b ON b.id = a.bill_id
                 WHERE a.adjustment_type = 'refund'
                   AND a.refunded_at >= :start
                   AND a.refunded_at < :end
                 GROUP BY b.merchant_id
                """, dayRange(windowDomain, businessDate), (org.springframework.jdbc.core.RowCallbackHandler) resultSet ->
                result.put(resultSet.getString("merchant_id"), resultSet.getBigDecimal("total"))
        );
        return result;
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
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT merchant_id, COALESCE(SUM(allocated_revenue_cent), 0) AS total
                  FROM issued_entitlements
                 WHERE allocated_revenue_cent IS NOT NULL
                   AND created_at >= :start
                   AND created_at < :end
                """ + entitlementTypeFilter + """
                 GROUP BY merchant_id
                """, dayRange(windowDomain, businessDate), (org.springframework.jdbc.core.RowCallbackHandler) resultSet ->
                result.put(resultSet.getString("merchant_id"), resultSet.getBigDecimal("total"))
        );
        return result;
    }

    @Override
    public Map<String, BigDecimal> sumTicketAllocatedRevenueByMerchant(LocalDate businessDate, String windowDomain) {
        // 清单#4：本商户售票分量——当日发放（created_at 落经营日窗口）权益的 allocated_revenue_cent 按商户汇总。
        // allocated_revenue_cent IS NULL（direct 出票/无订单）不计，保守不归任何商户。
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT merchant_id, COALESCE(SUM(allocated_revenue_cent), 0) AS total
                  FROM issued_entitlements
                 WHERE allocated_revenue_cent IS NOT NULL
                   AND created_at >= :start
                   AND created_at < :end
                 GROUP BY merchant_id
                """, dayRange(windowDomain, businessDate), (org.springframework.jdbc.core.RowCallbackHandler) resultSet ->
                result.put(resultSet.getString("merchant_id"), resultSet.getBigDecimal("total"))
        );
        return result;
    }

    @Override
    public Map<String, BigDecimal> sumTicketRefundByMerchant(LocalDate businessDate, String windowDomain) {
        // 清单#4：本商户售票退款分量——按 ticket_refunds（created_at 落经营日窗口，与平台 sumTicketRefundCents 同口径）
        // 关联订单各权益 allocated 占比把退款分摊到 merchant。逐笔退款用"最大余数补尾"保证 Σ分摊==退款额、分到分不丢分。
        //
        // SQL 取明细行：每笔退款 × 该订单各 merchant 的 allocated 小计 + 该订单 allocated 总额（基数）。
        // 订单无 allocated 权益（基数为 0）→ 不产生明细行 → 该退款保守不归任何商户（与售票分量口径一致）。
        List<RefundMerchantShareRow> rows = jdbcTemplate.query("""
                SELECT r.id AS refund_id,
                       r.refund_amount_cent AS refund_amount,
                       e.merchant_id AS merchant_id,
                       SUM(e.allocated_revenue_cent) AS merchant_allocated,
                       SUM(SUM(e.allocated_revenue_cent)) OVER (PARTITION BY r.id) AS order_total_allocated
                  FROM ticket_refunds r
                  JOIN issued_entitlements e ON e.order_id = r.target_id
                 WHERE e.allocated_revenue_cent IS NOT NULL
                   AND r.created_at >= :start
                   AND r.created_at < :end
                 GROUP BY r.id, r.refund_amount_cent, e.merchant_id
                 ORDER BY r.id, e.merchant_id
                """, dayRange(windowDomain, businessDate), (resultSet, rowNum) -> new RefundMerchantShareRow(
                resultSet.getString("refund_id"),
                resultSet.getLong("refund_amount"),
                resultSet.getString("merchant_id"),
                resultSet.getLong("merchant_allocated"),
                resultSet.getLong("order_total_allocated")
        ));
        return distributeRefundsByMerchant(rows);
    }

    @Override
    public Map<String, BigDecimal> sumContractRentReceivableByMerchant(LocalDate businessDate, String windowDomain) {
        // P1-C：合同租金/物业/能源应收按 due_date 当日（期首）逐期确认收入，并入商户行级 revenue.net 租金分量。
        // 用 amount_cent（已挂账应收，权责发生制确认），与酒吧/售票的现金口径合并入 net（任务要求接 ar_ap receivable）。
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT merchant_id, COALESCE(SUM(amount_cent), 0) AS total
                  FROM ar_ap_items
                 WHERE direction = 'receivable'
                   AND source_type IN ('contract_rent', 'contract_property_fee', 'contract_energy_fee')
                   AND due_date = :business_date
                 GROUP BY merchant_id
                """, new MapSqlParameterSource("business_date", businessDate),
                (org.springframework.jdbc.core.RowCallbackHandler) resultSet ->
                result.put(resultSet.getString("merchant_id"), resultSet.getBigDecimal("total")));
        return result;
    }

    private record RefundMerchantShareRow(
            String refundId, long refundAmount, String merchantId, long merchantAllocated, long orderTotalAllocated) {
    }

    /**
     * 逐笔退款按各商户 allocated 占比分摊（最大余数补尾，Σ分摊恰=退款额），再跨退款累加到 merchant。
     * 基数 orderTotalAllocated<=0 的退款行不会出现（SQL 已过滤），故无需特殊处理。
     */
    private Map<String, BigDecimal> distributeRefundsByMerchant(List<RefundMerchantShareRow> rows) {
        Map<String, java.util.List<RefundMerchantShareRow>> byRefund = new LinkedHashMap<>();
        for (RefundMerchantShareRow row : rows) {
            byRefund.computeIfAbsent(row.refundId(), key -> new java.util.ArrayList<>()).add(row);
        }
        Map<String, Long> totals = new LinkedHashMap<>();
        for (java.util.List<RefundMerchantShareRow> group : byRefund.values()) {
            long base = group.get(0).orderTotalAllocated();
            long refundAmount = group.get(0).refundAmount();
            if (base <= 0) {
                continue;
            }
            long assigned = 0;
            // floor 分配 + 余数排序补尾，保证 Σ恰=refundAmount。
            long[] floors = new long[group.size()];
            double[] remainders = new double[group.size()];
            for (int i = 0; i < group.size(); i++) {
                long product = refundAmount * group.get(i).merchantAllocated();
                floors[i] = product / base;
                remainders[i] = (double) (product % base) / base;
                assigned += floors[i];
            }
            long leftover = refundAmount - assigned;
            Integer[] order = new Integer[group.size()];
            for (int i = 0; i < order.length; i++) {
                order[i] = i;
            }
            java.util.Arrays.sort(order, (a, b) -> Double.compare(remainders[b], remainders[a]));
            for (int i = 0; i < group.size(); i++) {
                long share = floors[i] + (rankOf(order, i) < leftover ? 1L : 0L);
                if (share == 0L) {
                    continue;
                }
                totals.merge(group.get(i).merchantId(), share, Long::sum);
            }
        }
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        totals.forEach((merchantId, value) -> result.put(merchantId, BigDecimal.valueOf(value)));
        return result;
    }

    private int rankOf(Integer[] order, int index) {
        for (int rank = 0; rank < order.length; rank++) {
            if (order[rank] == index) {
                return rank;
            }
        }
        return order.length;
    }

    @Override
    public BigDecimal countAmusementVerificationSuccess(LocalDate businessDate) {
        BigDecimal value = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM amusement_verifications
                 WHERE verification_result = 'success'
                   AND created_at >= :start
                   AND created_at < :end
                """, dayRange("amusement", businessDate), BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    public Map<String, BigDecimal> countTicketVerificationSuccessByMerchant(LocalDate businessDate) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT merchant_id, COUNT(*) AS total
                  FROM ticket_verifications
                 WHERE status = 'success'
                   AND created_at >= :start
                   AND created_at < :end
                 GROUP BY merchant_id
                """, dayRange("ticketing", businessDate), (org.springframework.jdbc.core.RowCallbackHandler) resultSet ->
                result.put(resultSet.getString("merchant_id"), resultSet.getBigDecimal("total"))
        );
        return result;
    }

    @Override
    public Map<Integer, Long> hourlyTrafficCounts(LocalDate businessDate, Optional<String> merchantScope) {
        // T4.3：按经营日窗口对成功核销 created_at 分小时桶。
        // ponytail: 按 UTC 小时分桶（确定、与窗口同源）；若要本地时段口径，改 AT TIME ZONE '业务时区'。
        Map<Integer, Long> result = new LinkedHashMap<>();
        MapSqlParameterSource params = dayRange("amusement", businessDate);
        String sql;
        if (merchantScope.isEmpty()) {
            sql = """
                    SELECT h, COUNT(*) AS total FROM (
                        SELECT date_part('hour', created_at AT TIME ZONE 'UTC')::int AS h
                          FROM ticket_verifications
                         WHERE status = 'success' AND created_at >= :start AND created_at < :end
                        UNION ALL
                        SELECT date_part('hour', created_at AT TIME ZONE 'UTC')::int AS h
                          FROM amusement_verifications
                         WHERE verification_result = 'success' AND created_at >= :start AND created_at < :end
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
        return result;
    }

    @Override
    public Map<String, BigDecimal> countTicketIssuedByMerchant(LocalDate businessDate) {
        // 清单#18：券发放数（核销率分母）——issued_entitlements 当日发放（created_at 落经营日窗口）按 merchant 分组计数，
        // 与 countTicketVerificationSuccessByMerchant 同口径（merchant_id 可空：归入平台口径合计）。
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT merchant_id, COUNT(*) AS total
                  FROM issued_entitlements
                 WHERE created_at >= :start
                   AND created_at < :end
                 GROUP BY merchant_id
                """, dayRange("ticketing", businessDate), (org.springframework.jdbc.core.RowCallbackHandler) resultSet ->
                result.put(resultSet.getString("merchant_id"), resultSet.getBigDecimal("total"))
        );
        return result;
    }

    @Override
    public BigDecimal countNewMembers(LocalDate businessDate) {
        BigDecimal value = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM customers
                 WHERE created_at >= :start
                   AND created_at < :end
                """, dayRange("customer", businessDate), BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    public Map<String, BigDecimal> sumInventoryBalanceByMerchant() {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT merchant_id, COALESCE(SUM(quantity), 0) AS total
                  FROM inventory_stock_balances
                 GROUP BY merchant_id
                """, new MapSqlParameterSource(), (org.springframework.jdbc.core.RowCallbackHandler) resultSet ->
                result.put(resultSet.getString("merchant_id"), resultSet.getBigDecimal("total"))
        );
        return result;
    }

    @Override
    public Map<String, BigDecimal> countLowStockByMerchant() {
        // 缺货预警（财务确认：绝对值阈值/SKU 可配）：按 SKU 汇总各库位可用量，低于该 SKU 的安全库存阈值则计一个预警。
        // 阈值取 inventory_items.min_quantity（绝对值/SKU），阈值 ≤ 0 视为不预警（与 InventoryService.safety_stock_low 一致）。
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
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
                """, new MapSqlParameterSource(), (org.springframework.jdbc.core.RowCallbackHandler) resultSet ->
                result.put(resultSet.getString("merchant_id"), resultSet.getBigDecimal("total"))
        );
        return result;
    }

    @Override
    public BigDecimal countPendingReview() {
        BigDecimal reviews = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM supervisor_review_decisions WHERE review_status = 'pending'
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
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT metric_key, actual_value
                  FROM analytics_business_metric_snapshots
                 WHERE business_date = :business_date
                   AND period_type = 'daily'
                   AND segment_type = 'project'
                   AND segment_id = :segment_id
                """, params, (org.springframework.jdbc.core.RowCallbackHandler) resultSet ->
                result.put(resultSet.getString("metric_key"), resultSet.getBigDecimal("actual_value"))
        );
        return result;
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
