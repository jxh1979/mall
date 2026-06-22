package com.complex.platform.analytics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AuthorizationService;
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
import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.GlobalExceptionHandler;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.config.SecurityConfig;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AnalyticsController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, AnalyticsControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class AnalyticsControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnalyticsService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
        when(service.listAiQualityResults()).thenReturn(new PageResponse<>(List.of(aiQualityResult()), 1, 50, 1));
        when(service.listAiQualityTasks()).thenReturn(new PageResponse<>(List.of(aiQualityTask()), 1, 50, 1));
        when(service.generateLocalLlmAiQualitySuggestion(any(AiQualitySuggestionRequest.class))).thenReturn(aiQualitySuggestion());
        when(service.listCustomerIntents()).thenReturn(new PageResponse<>(List.of(customerIntent()), 1, 50, 1));
        when(service.getP0OverviewDashboard()).thenReturn(dashboard());
        when(authorizationService.currentMerchantScope()).thenReturn(java.util.Optional.empty());
        when(service.overview(any())).thenReturn(overviewResponse());
        when(service.dashboardDomain(anyString(), any())).thenReturn(overviewResponse());
        when(service.groupOverview()).thenReturn(groupOverviewResponse());
        when(service.listBusinessMetricSnapshots()).thenReturn(new PageResponse<>(List.of(businessMetricSnapshot()), 1, 50, 1));
        when(service.listMerchantHealthScores(any())).thenReturn(new PageResponse<>(List.of(merchantHealth()), 1, 50, 1));
        when(service.listAnalyticsMerchants(any())).thenReturn(new PageResponse<>(List.of(merchantSummary()), 1, 50, 1));
        when(service.listMetricAnomalies()).thenReturn(new PageResponse<>(List.of(metricAnomaly()), 1, 50, 1));
        when(service.listMetricDefinitions()).thenReturn(new PageResponse<>(List.of(metricDefinition()), 1, 50, 1));
        when(service.listRiskAlerts()).thenReturn(new PageResponse<>(List.of(riskAlert()), 1, 50, 1));
        when(service.createRiskAlert(any(RiskAlertRequest.class))).thenReturn(riskAlert());
        when(service.resolveRiskAlert(anyString(), any(StatusChangeRequest.class))).thenReturn(resolvedRiskAlert());
        when(service.listRiskRules()).thenReturn(new PageResponse<>(List.of(riskRule()), 1, 50, 1));
        when(service.getRobotAnalytics()).thenReturn(new PageResponse<>(List.of(robotMetric()), 1, 50, 1));
        when(service.listStaffServiceScores()).thenReturn(new PageResponse<>(List.of(staffScore()), 1, 50, 1));
    }

    @ParameterizedTest
    @CsvSource({
            "GET,/api/v1/analytics/ai-quality/results,analytics.risk_alert.view",
            "GET,/api/v1/analytics/ai-quality/tasks,analytics.risk_alert.view",
            "POST,/api/v1/analytics/ai-quality/local-llm-suggestions,analytics.risk_alert.manage",
            "GET,/api/v1/analytics/customer-intents,analytics.risk_alert.view",
            "GET,/api/v1/analytics/dashboards/p0-overview,analytics.dashboard.view",
            "GET,/api/v1/analytics/dashboards/bar,analytics.dashboard.view",
            "GET,/api/v1/analytics/group-overview,analytics.group.view",
            "GET,/api/v1/analytics/business-metric-snapshots,analytics.dashboard.view",
            "GET,/api/v1/analytics/merchant-health,analytics.dashboard.view",
            "GET,/api/v1/analytics/merchants,analytics.dashboard.view",
            "GET,/api/v1/analytics/metric-anomalies,analytics.risk_alert.view",
            "GET,/api/v1/analytics/metric-definitions,analytics.dashboard.view",
            "GET,/api/v1/analytics/risk-alerts,analytics.risk_alert.view",
            "POST,/api/v1/analytics/risk-alerts,analytics.risk_alert.manage",
            "POST,/api/v1/analytics/risk-alerts/alert_1/resolve,analytics.risk_alert.manage",
            "GET,/api/v1/analytics/risk-rules,analytics.risk_alert.view",
            "GET,/api/v1/analytics/robot,analytics.dashboard.view",
            "GET,/api/v1/analytics/staff-service-score,analytics.risk_alert.view"
    })
    void analyticsRoutesUseExactOpenApiPermission(String method, String path, String permission) throws Exception {
        if ("GET".equals(method)) {
            mockMvc.perform(get(path).with(user("analytics").roles("P0_SMOKE")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"));
        } else {
            mockMvc.perform(post(path)
                            .with(user("analytics").roles("P0_SMOKE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyFor(path, true)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"));
        }

        verify(authorizationService).requirePermission(permission);
    }

    @ParameterizedTest
    @CsvSource({
            "/api/v1/analytics/risk-alerts",
            "/api/v1/analytics/risk-alerts/alert_1/resolve",
            "/api/v1/analytics/ai-quality/local-llm-suggestions"
    })
    void writeRoutesRejectMissingIdempotencyKey(String path) throws Exception {
        mockMvc.perform(post(path)
                        .with(user("analytics").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyFor(path, false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("idempotency_key is required"));
    }

    private String bodyFor(String path, boolean withIdempotencyKey) {
        String idempotency = withIdempotencyKey ? "\"idempotency_key\":\"analytics-once\"," : "";
        if (path.contains("/local-llm-suggestions")) {
            return "{" + idempotency + "\"source_type\":\"ticket_call\",\"source_id\":\"call_160\",\"source_text\":\"guest asks for refund after failed service\",\"requested_by\":\"staff_1\"}";
        }
        if (path.endsWith("/resolve")) {
            return "{" + idempotency + "\"action\":\"resolved\",\"reason\":\"handled by operator\",\"evidence_attachment_ids\":[\"att_1\"]}";
        }
        return "{" + idempotency + """
                "rule_id":"rule_1","alert_type":"metric_anomaly","risk_level":"high","object_type":"merchant","object_id":"merchant_1","summary":"Revenue dropped below threshold","operator_note":"created by control desk","evidence_attachment_ids":["att_1"]}
                """;
    }

    private MetricDefinitionRecord metricDefinition() {
        Instant now = now();
        return new MetricDefinitionRecord("gmv_amount_cents", "GMV", "finance", "cent", "sum paid order amount", "finance_manager", "active", now, now);
    }

    private DashboardSnapshotRecord dashboard() {
        return new DashboardSnapshotRecord(
                "p0_overview_today",
                "project_1",
                LocalDate.parse("2026-06-14"),
                128800L,
                23,
                2,
                1,
                Map.of("top_domain", "bar"),
                now()
        );
    }


    private AnalyticsRecords.OverviewResponse overviewResponse() {
        return new AnalyticsRecords.OverviewResponse(
                now(),
                null,
                LocalDate.parse("2026-06-14"),
                Map.of("revenue.ticket", AnalyticsRecords.MetricValue.of(new BigDecimal("128800"), "cent"))
        );
    }

    private AnalyticsRecords.GroupOverviewResponse groupOverviewResponse() {
        return new AnalyticsRecords.GroupOverviewResponse(
                now(),
                LocalDate.parse("2026-06-14"),
                List.of(new AnalyticsRecords.GroupProjectRollup(
                        "project_1",
                        "综合体（默认）",
                        1L,
                        Map.of("revenue.net", AnalyticsRecords.MetricValue.of(new BigDecimal("128800"), "cent"))
                ))
        );
    }

    private BusinessMetricSnapshotRecord businessMetricSnapshot() {
        return new BusinessMetricSnapshotRecord(
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
                now()
        );
    }

    private MerchantSummaryRecord merchantSummary() {
        Instant now = now();
        return new MerchantSummaryRecord("merchant_1", "Blue Bar", "bar", 128800L, 23, 1, "settled", LocalDate.parse("2026-06-14"), now, now);
    }

    private MerchantHealthScoreRecord merchantHealth() {
        Instant now = now();
        return new MerchantHealthScoreRecord("merchant_1", 86, "healthy", 1, 0L, now, "stable", now, now);
    }

    private MetricAnomalyRecord metricAnomaly() {
        return new MetricAnomalyRecord("anomaly_1", "gmv_amount_cents", "finance", "high", new BigDecimal("128800"), new BigDecimal("188800"), "GMV below expected range", "open", now(), null);
    }

    private RiskRuleRecord riskRule() {
        Instant now = now();
        return new RiskRuleRecord("rule_1", "GMV_DROP", "GMV drop", "finance", "high", true, Map.of("threshold", 30), "notify manager", now, now);
    }

    private RiskAlertRecord riskAlert() {
        Instant now = now();
        return new RiskAlertRecord("alert_1", "rule_1", "metric_anomaly", "high", "merchant", "merchant_1", "Revenue dropped below threshold", "open", "created by control desk", List.of("att_1"), "analytics-once", now, null, now);
    }

    private RiskAlertRecord resolvedRiskAlert() {
        Instant now = now();
        return new RiskAlertRecord("alert_1", "rule_1", "metric_anomaly", "high", "merchant", "merchant_1", "Revenue dropped below threshold", "resolved", "handled by operator", List.of("att_1"), "analytics-resolve-once", now, now, now);
    }


    private AiQualitySuggestionResponse aiQualitySuggestion() {
        return new AiQualitySuggestionResponse(
                aiQualityTask(),
                aiQualityResult(),
                "unavailable",
                false,
                true
        );
    }

    private AiQualityTaskRecord aiQualityTask() {
        Instant now = now();
        return new AiQualityTaskRecord("task_1", "ticket_call", "call_1", "manual_review", "none", "none", "completed", "staff_1", now, now);
    }

    private AiQualityResultRecord aiQualityResult() {
        return new AiQualityResultRecord("result_1", "task_1", "ticket_call", "call_1", 92, "normal", new BigDecimal("0.91"), "clear service interaction", 1, now());
    }

    private CustomerIntentRecord customerIntent() {
        return new CustomerIntentRecord("intent_1", "ticket_call", "call_1", "customer_1", "refund_request", new BigDecimal("0.76"), "suspicious", "asks for refund", now());
    }

    private RobotMetricRecord robotMetric() {
        return new RobotMetricRecord("robot_metric_1", "robot_1", "mock_provider", 10, 9, 1, LocalDate.parse("2026-06-14"), "one failed delivery task", now());
    }

    private StaffServiceScoreRecord staffScore() {
        return new StaffServiceScoreRecord("score_1", "staff_1", "Alex", "frontdesk", 95, 0, 12, "normal", LocalDate.parse("2026-06-14"), now());
    }

    private Instant now() {
        return Instant.parse("2026-06-14T00:00:00Z");
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        AnalyticsService analyticsService() {
            return Mockito.mock(AnalyticsService.class);
        }

        @Bean
        AuthorizationService authorizationService() {
            return Mockito.mock(AuthorizationService.class);
        }

        @Bean
        AuthService authService() {
            return Mockito.mock(AuthService.class);
        }
    }
}
