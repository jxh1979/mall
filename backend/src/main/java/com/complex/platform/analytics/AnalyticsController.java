package com.complex.platform.analytics;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

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
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService service;
    private final AuthorizationService authorizationService;

    public AnalyticsController(AnalyticsService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/ai-quality/results")
    public ApiResponse<PageResponse<AiQualityResultRecord>> listAiQualityResults() {
        authorizationService.requirePermission("analytics.risk_alert.view");
        return ApiResponse.ok(service.listAiQualityResults());
    }

    @GetMapping("/ai-quality/tasks")
    public ApiResponse<PageResponse<AiQualityTaskRecord>> listAiQualityTasks() {
        authorizationService.requirePermission("analytics.risk_alert.view");
        return ApiResponse.ok(service.listAiQualityTasks());
    }

    @PostMapping("/ai-quality/local-llm-suggestions")
    public ApiResponse<AiQualitySuggestionResponse> generateLocalLlmAiQualitySuggestion(
            @RequestBody(required = false) AiQualitySuggestionRequest request
    ) {
        authorizationService.requirePermission("analytics.risk_alert.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.generateLocalLlmAiQualitySuggestion(request));
    }

    @GetMapping("/customer-intents")
    public ApiResponse<PageResponse<CustomerIntentRecord>> listCustomerIntents() {
        authorizationService.requirePermission("analytics.risk_alert.view");
        return ApiResponse.ok(service.listCustomerIntents());
    }

    @GetMapping("/dashboards/p0-overview")
    public ApiResponse<DashboardSnapshotRecord> getP0OverviewDashboard() {
        authorizationService.requirePermission("analytics.dashboard.view");
        return ApiResponse.ok(service.getP0OverviewDashboard());
    }


    @GetMapping("/business-metric-snapshots")
    public ApiResponse<PageResponse<BusinessMetricSnapshotRecord>> listBusinessMetricSnapshots() {
        authorizationService.requirePermission("analytics.dashboard.view");
        return ApiResponse.ok(service.listBusinessMetricSnapshots());
    }

    @GetMapping("/merchant-health")
    public ApiResponse<PageResponse<MerchantHealthScoreRecord>> listMerchantHealthScores() {
        authorizationService.requirePermission("analytics.dashboard.view");
        return ApiResponse.ok(service.listMerchantHealthScores());
    }

    @GetMapping("/merchants")
    public ApiResponse<PageResponse<MerchantSummaryRecord>> listAnalyticsMerchants() {
        authorizationService.requirePermission("analytics.dashboard.view");
        return ApiResponse.ok(service.listAnalyticsMerchants());
    }

    @GetMapping("/metric-anomalies")
    public ApiResponse<PageResponse<MetricAnomalyRecord>> listMetricAnomalies() {
        authorizationService.requirePermission("analytics.risk_alert.view");
        return ApiResponse.ok(service.listMetricAnomalies());
    }

    @GetMapping("/metric-definitions")
    public ApiResponse<PageResponse<MetricDefinitionRecord>> listMetricDefinitions() {
        authorizationService.requirePermission("analytics.dashboard.view");
        return ApiResponse.ok(service.listMetricDefinitions());
    }

    @GetMapping("/risk-alerts")
    public ApiResponse<PageResponse<RiskAlertRecord>> listRiskAlerts() {
        authorizationService.requirePermission("analytics.risk_alert.view");
        return ApiResponse.ok(service.listRiskAlerts());
    }

    @PostMapping("/risk-alerts")
    public ApiResponse<RiskAlertRecord> createRiskAlert(@RequestBody(required = false) RiskAlertRequest request) {
        authorizationService.requirePermission("analytics.risk_alert.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createRiskAlert(request));
    }

    @PostMapping("/risk-alerts/{id}/resolve")
    public ApiResponse<RiskAlertRecord> resolveRiskAlert(
            @PathVariable String id,
            @RequestBody(required = false) StatusChangeRequest request
    ) {
        authorizationService.requirePermission("analytics.risk_alert.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.resolveRiskAlert(id, request));
    }

    @GetMapping("/risk-rules")
    public ApiResponse<PageResponse<RiskRuleRecord>> listRiskRules() {
        authorizationService.requirePermission("analytics.risk_alert.view");
        return ApiResponse.ok(service.listRiskRules());
    }

    @GetMapping("/robot")
    public ApiResponse<PageResponse<RobotMetricRecord>> getRobotAnalytics() {
        authorizationService.requirePermission("analytics.dashboard.view");
        return ApiResponse.ok(service.getRobotAnalytics());
    }

    @GetMapping("/staff-service-score")
    public ApiResponse<PageResponse<StaffServiceScoreRecord>> listStaffServiceScores() {
        authorizationService.requirePermission("analytics.risk_alert.view");
        return ApiResponse.ok(service.listStaffServiceScores());
    }
}
