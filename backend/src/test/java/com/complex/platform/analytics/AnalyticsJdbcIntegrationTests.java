package com.complex.platform.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.complex.platform.analytics.AnalyticsRecords.RiskAlertRecord;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class AnalyticsJdbcIntegrationTests {

    @Autowired
    private JdbcAnalyticsRepository repository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesAnalyticsTablesSeedsPermissionsAndCurrentFlywayVersion() {
        assertThat(regclass("analytics_metric_definitions")).isEqualTo("analytics_metric_definitions");
        assertThat(regclass("analytics_dashboard_snapshots")).isEqualTo("analytics_dashboard_snapshots");
        assertThat(regclass("analytics_business_metric_snapshots")).isEqualTo("analytics_business_metric_snapshots");
        assertThat(regclass("analytics_merchant_summaries")).isEqualTo("analytics_merchant_summaries");
        assertThat(regclass("analytics_merchant_health_scores")).isEqualTo("analytics_merchant_health_scores");
        assertThat(regclass("analytics_metric_anomalies")).isEqualTo("analytics_metric_anomalies");
        assertThat(regclass("analytics_risk_rules")).isEqualTo("analytics_risk_rules");
        assertThat(regclass("analytics_risk_alerts")).isEqualTo("analytics_risk_alerts");
        assertThat(regclass("analytics_ai_quality_tasks")).isEqualTo("analytics_ai_quality_tasks");
        assertThat(regclass("analytics_ai_quality_results")).isEqualTo("analytics_ai_quality_results");
        assertThat(regclass("analytics_customer_intents")).isEqualTo("analytics_customer_intents");
        assertThat(regclass("analytics_robot_metrics")).isEqualTo("analytics_robot_metrics");
        assertThat(regclass("analytics_staff_service_scores")).isEqualTo("analytics_staff_service_scores");
        assertThat(currentFlywayVersion()).isEqualTo("054");

        assertThat(permissionCodes()).contains(
                "analytics.dashboard.view",
                "analytics.risk_alert.view",
                "analytics.risk_alert.manage"
        );
        assertThat(platformAdminPermissionCodes()).contains(
                "analytics.dashboard.view",
                "analytics.risk_alert.view",
                "analytics.risk_alert.manage"
        );
    }

    @Test
    void migrationSeedsOneDeterministicRowForEveryReadEndpoint() {
        assertThat(rowCount("analytics_metric_definitions")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_dashboard_snapshots")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_business_metric_snapshots")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_merchant_summaries")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_merchant_health_scores")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_metric_anomalies")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_risk_rules")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_risk_alerts")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_ai_quality_tasks")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_ai_quality_results")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_customer_intents")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_robot_metrics")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_staff_service_scores")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void repositoryListsSeededAnalyticsReadModels() {
        assertThat(repository.listMetricDefinitions())
                .extracting(AnalyticsRecords.MetricDefinitionRecord::metricKey, AnalyticsRecords.MetricDefinitionRecord::unit)
                .contains(tuple("gmv_amount_cents", "cent"));
        assertThat(repository.getP0OverviewDashboard().snapshotKey()).isEqualTo("p0_overview_today");
        assertThat(repository.listBusinessMetricSnapshots())
                .extracting(AnalyticsRecords.BusinessMetricSnapshotRecord::metricKey, AnalyticsRecords.BusinessMetricSnapshotRecord::trendDirection)
                .contains(tuple("gmv_amount_cents", "up"));
        assertThat(repository.listAnalyticsMerchants())
                .extracting(AnalyticsRecords.MerchantSummaryRecord::merchantId, AnalyticsRecords.MerchantSummaryRecord::revenueAmountCents)
                .contains(tuple("merchant_p0", 128800L));
        assertThat(repository.listMerchantHealthScores())
                .extracting(AnalyticsRecords.MerchantHealthScoreRecord::merchantId, AnalyticsRecords.MerchantHealthScoreRecord::scoreLevel)
                .contains(tuple("merchant_p0", "healthy"));
        assertThat(repository.listMetricAnomalies())
                .extracting(AnalyticsRecords.MetricAnomalyRecord::metricKey, AnalyticsRecords.MetricAnomalyRecord::riskLevel)
                .contains(tuple("gmv_amount_cents", "suspicious"));
        assertThat(repository.listRiskRules())
                .extracting(AnalyticsRecords.RiskRuleRecord::ruleId, AnalyticsRecords.RiskRuleRecord::enabled)
                .contains(tuple("rule_p0_gmv_drop", true));
        assertThat(repository.listAiQualityTasks())
                .extracting(AnalyticsRecords.AiQualityTaskRecord::taskId, AnalyticsRecords.AiQualityTaskRecord::processingMode)
                .contains(tuple("ai_task_p0", "manual_review"));
        assertThat(repository.listAiQualityResults())
                .extracting(AnalyticsRecords.AiQualityResultRecord::resultId, AnalyticsRecords.AiQualityResultRecord::riskLevel)
                .contains(tuple("ai_result_p0", "normal"));
        assertThat(repository.listCustomerIntents())
                .extracting(AnalyticsRecords.CustomerIntentRecord::intentId, AnalyticsRecords.CustomerIntentRecord::intentType)
                .contains(tuple("intent_p0", "refund_request"));
        assertThat(repository.listRobotMetrics())
                .extracting(AnalyticsRecords.RobotMetricRecord::metricId, AnalyticsRecords.RobotMetricRecord::successCount)
                .contains(tuple("robot_metric_p0", 9));
        assertThat(repository.listStaffServiceScores())
                .extracting(AnalyticsRecords.StaffServiceScoreRecord::scoreId, AnalyticsRecords.StaffServiceScoreRecord::serviceScore)
                .contains(tuple("staff_score_p0", 95));
        assertThat(repository.listRiskAlerts())
                .extracting(RiskAlertRecord::id, RiskAlertRecord::status)
                .contains(tuple("alert_p0", "open"));
    }

    @Test
    void createListAndResolveRiskAlertPersistsAuditBoundaryFields() {
        Instant createdAt = Instant.parse("2026-06-14T03:00:00Z");
        RiskAlertRecord alert = repository.saveRiskAlert(new RiskAlertRecord(
                "alert_jdbc",
                "rule_p0_gmv_drop",
                "metric_anomaly",
                "critical",
                "merchant",
                "merchant_jdbc",
                "Revenue drop needs review",
                "open",
                "created by integration test",
                List.of("att_create"),
                "analytics-jdbc-create",
                createdAt,
                null,
                createdAt
        ));

        assertThat(alert.id()).isEqualTo("alert_jdbc");
        assertThat(repository.listRiskAlerts())
                .extracting(RiskAlertRecord::id, RiskAlertRecord::idempotencyKey, RiskAlertRecord::operatorNote)
                .contains(tuple("alert_jdbc", "analytics-jdbc-create", "created by integration test"));

        Instant resolvedAt = Instant.parse("2026-06-14T03:10:00Z");
        RiskAlertRecord resolved = repository.updateRiskAlert(new RiskAlertRecord(
                alert.id(),
                alert.ruleId(),
                alert.alertType(),
                alert.riskLevel(),
                alert.objectType(),
                alert.objectId(),
                alert.summary(),
                "resolved",
                "handled by integration test",
                List.of("att_resolve"),
                alert.idempotencyKey(),
                alert.createdAt(),
                resolvedAt,
                resolvedAt
        ));

        assertThat(resolved)
                .extracting(RiskAlertRecord::status, RiskAlertRecord::operatorNote, RiskAlertRecord::evidenceAttachmentIds, RiskAlertRecord::resolvedAt)
                .containsExactly("resolved", "handled by integration test", List.of("att_resolve"), resolvedAt);
        assertThat(repository.findRiskAlertById("alert_jdbc"))
                .get()
                .extracting(RiskAlertRecord::status, RiskAlertRecord::idempotencyKey, RiskAlertRecord::operatorNote, RiskAlertRecord::resolvedAt)
                .containsExactly("resolved", "analytics-jdbc-create", "handled by integration test", resolvedAt);
    }

    private String regclass(String tableName) {
        return jdbcTemplate.queryForObject("""
                SELECT to_regclass(:qualified_name)::text
                """, Map.of("qualified_name", "public." + tableName), String.class);
    }

    private long rowCount(String tableName) {
        String sql = switch (tableName) {
            case "analytics_metric_definitions" -> """
                    SELECT count(*)
                      FROM analytics_metric_definitions
                    """;
            case "analytics_dashboard_snapshots" -> """
                    SELECT count(*)
                      FROM analytics_dashboard_snapshots
                    """;
            case "analytics_business_metric_snapshots" -> """
                    SELECT count(*)
                      FROM analytics_business_metric_snapshots
                    """;
            case "analytics_merchant_summaries" -> """
                    SELECT count(*)
                      FROM analytics_merchant_summaries
                    """;
            case "analytics_merchant_health_scores" -> """
                    SELECT count(*)
                      FROM analytics_merchant_health_scores
                    """;
            case "analytics_metric_anomalies" -> """
                    SELECT count(*)
                      FROM analytics_metric_anomalies
                    """;
            case "analytics_risk_rules" -> """
                    SELECT count(*)
                      FROM analytics_risk_rules
                    """;
            case "analytics_risk_alerts" -> """
                    SELECT count(*)
                      FROM analytics_risk_alerts
                    """;
            case "analytics_ai_quality_tasks" -> """
                    SELECT count(*)
                      FROM analytics_ai_quality_tasks
                    """;
            case "analytics_ai_quality_results" -> """
                    SELECT count(*)
                      FROM analytics_ai_quality_results
                    """;
            case "analytics_customer_intents" -> """
                    SELECT count(*)
                      FROM analytics_customer_intents
                    """;
            case "analytics_robot_metrics" -> """
                    SELECT count(*)
                      FROM analytics_robot_metrics
                    """;
            case "analytics_staff_service_scores" -> """
                    SELECT count(*)
                      FROM analytics_staff_service_scores
                    """;
            default -> throw new IllegalArgumentException("unsupported analytics table: " + tableName);
        };
        return jdbcTemplate.queryForObject(sql, Map.of(), Long.class);
    }

    private List<String> permissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM permissions
                 ORDER BY permission_code
                """, Map.of(), String.class);
    }

    private List<String> platformAdminPermissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM role_permissions
                 WHERE role_id = 'role_platform_admin'
                 ORDER BY permission_code
                """, Map.of(), String.class);
    }

    private String currentFlywayVersion() {
        return jdbcTemplate.queryForObject("""
                SELECT version
                  FROM flyway_schema_history
                 WHERE success = TRUE
                 ORDER BY installed_rank DESC
                 LIMIT 1
                """, Map.of(), String.class);
    }
}
