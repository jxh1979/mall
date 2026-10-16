package com.complex.platform.analytics;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.analytics.AnalyticsRecords.AiQualityResultRecord;
import com.complex.platform.analytics.AnalyticsRecords.AiQualityTaskRecord;
import com.complex.platform.analytics.AnalyticsRecords.BusinessMetricSnapshotRecord;
import com.complex.platform.analytics.AnalyticsRecords.CustomerIntentRecord;
import com.complex.platform.analytics.AnalyticsRecords.GroupOverviewResponse;
import com.complex.platform.analytics.AnalyticsRecords.HourlyTrafficResponse;
import com.complex.platform.analytics.AnalyticsRecords.MerchantHealthScoreRecord;
import com.complex.platform.analytics.AnalyticsRecords.MerchantSummaryRecord;
import com.complex.platform.analytics.AnalyticsRecords.MetricAnomalyRecord;
import com.complex.platform.analytics.AnalyticsRecords.MetricDefinitionRecord;
import com.complex.platform.analytics.AnalyticsRecords.OverviewResponse;
import com.complex.platform.analytics.AnalyticsRecords.RiskAlertRecord;
import com.complex.platform.analytics.AnalyticsRecords.RiskRuleRecord;
import com.complex.platform.analytics.AnalyticsRecords.RobotMetricRecord;
import com.complex.platform.analytics.AnalyticsRecords.StaffServiceScoreRecord;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEvent;
import java.util.List;
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
    private final OperatingParamsService operatingParamsService;

    public AnalyticsController(AnalyticsService service, AuthorizationService authorizationService,
            OperatingParamsService operatingParamsService) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.operatingParamsService = operatingParamsService;
    }

    /** M737 全平台口径：商户、外包照旧由 requirePlatformScope 拒绝；其余须是平台管理员或经勾了「可以查看全部项目」的角色持有该码。 */
    private void requireWholePlatform(String permissionCode) {
        authorizationService.requirePlatformScope();
        if (!authorizationService.allowsPlatformScope(authorizationService.currentActor(), permissionCode)) {
            throw ApiException.forbidden("PLATFORM_SCOPE_REQUIRED",
                    "该数据为全平台口径，只有平台管理员和勾选了「可以查看全部项目」的人可以查看、处理");
        }
    }

    /** M737 平台口径看板的范围：不带商户归属且能看全平台合计 → 空（平台口径）；否则照旧 currentMerchantScope()。 */
    private java.util.Optional<String> dashboardScope() {
        var actor = authorizationService.currentActor();
        boolean merchantScoped = actor.merchantId() != null && !actor.merchantId().isBlank();
        if (!merchantScoped && authorizationService.allowsPlatformScope(actor, "analytics.dashboard.view")) {
            return java.util.Optional.empty();
        }
        return authorizationService.currentMerchantScope();
    }

    /** M737 按可触及项目取数：先 requirePlatformScope()（商户、外包照旧拒绝），再按入口权限码算可触及项目（多个码取交集）。 */
    private AuthorizationService.ProjectReach reach(String... permissionCodes) {
        authorizationService.requirePlatformScope();
        return authorizationService.projectReach(authorizationService.currentActor(), permissionCodes);
    }

    /** M737 传给服务层的项目集合：平台管理员 → null（不过滤）；其余（含勾了全部项目的人，M737-F07：操作员 2026-10-16「统一按『只到项目为止』」，
     *  治理地图「只能触及归属在项目或项目公司下……的账号与数据」）→ 可触及项目（可能为空集＝一行不出；项目为空的行不出）。 */
    private static java.util.Set<String> projectIds(AuthorizationService.ProjectReach reach) {
        return reach.platformAdmin() ? null : reach.organizationIds();
    }

    /** M737 商户类分析：不是平台管理员、不带商户归属、不是外包账号的员工按可触及项目出在其中经营的商户；其余三种人照旧走 currentMerchantScope()。 */
    private boolean projectStaff(com.complex.platform.access.AccessActor actor) {
        boolean merchantScoped = actor.merchantId() != null && !actor.merchantId().isBlank();
        return !authorizationService.isPlatformAdmin(actor) && !merchantScoped && !authorizationService.isOutsourcedVendor(actor);
    }

    @GetMapping("/ai-quality/results")
    public ApiResponse<PageResponse<AiQualityResultRecord>> listAiQualityResults() {
        authorizationService.requirePermission("analytics.risk_alert.view");
        requireWholePlatform("analytics.risk_alert.view");
        return ApiResponse.ok(service.listAiQualityResults());
    }

    @GetMapping("/ai-quality/tasks")
    public ApiResponse<PageResponse<AiQualityTaskRecord>> listAiQualityTasks() {
        authorizationService.requirePermission("analytics.risk_alert.view");
        requireWholePlatform("analytics.risk_alert.view");
        return ApiResponse.ok(service.listAiQualityTasks());
    }

    @PostMapping("/ai-quality/local-llm-suggestions")
    public ApiResponse<AiQualitySuggestionResponse> generateLocalLlmAiQualitySuggestion(
            @RequestBody(required = false) @jakarta.validation.Valid AiQualitySuggestionRequest request
    ) {
        authorizationService.requirePermission("analytics.risk_alert.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        requireWholePlatform("analytics.risk_alert.manage");
        return ApiResponse.ok(service.generateLocalLlmAiQualitySuggestion(request));
    }

    @GetMapping("/customer-intents")
    public ApiResponse<PageResponse<CustomerIntentRecord>> listCustomerIntents() {
        authorizationService.requirePermission("analytics.risk_alert.view");
        requireWholePlatform("analytics.risk_alert.view");
        return ApiResponse.ok(service.listCustomerIntents());
    }

    @GetMapping("/dashboards/p0-overview")
    public ApiResponse<OverviewResponse> getP0OverviewDashboard(
            @org.springframework.web.bind.annotation.RequestParam(value = "from", required = false) String from,
            @org.springframework.web.bind.annotation.RequestParam(value = "to", required = false) String to) {
        authorizationService.requirePermission("analytics.dashboard.view");
        // T4.1 修日期「假筛选」：前端发 from/to，以 to（退化用 from）为「看哪个经营日」的 as-of 日；
        // 空/非法 → 退回最新 ETL 日。区间跨日求和对余额/比率类指标会算错，待口径确定后再做，当前为单日 as-of。
        java.time.LocalDate asOf = parseOverviewDate(to);
        if (asOf == null) {
            asOf = parseOverviewDate(from);
        }
        return ApiResponse.ok(service.overview(dashboardScope(), asOf));
    }

    private static java.time.LocalDate parseOverviewDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(value.trim());
        } catch (java.time.format.DateTimeParseException e) {
            return null; // 非法日期忽略，退回最新经营日。
        }
    }

    @GetMapping("/dashboards/{domain}")
    public ApiResponse<OverviewResponse> getDomainDashboard(@PathVariable String domain) {
        authorizationService.requirePermission("analytics.dashboard.view");
        return ApiResponse.ok(service.dashboardDomain(domain, dashboardScope()));
    }


    @GetMapping("/traffic-hourly")
    public ApiResponse<HourlyTrafficResponse> getTrafficHourly(
            @org.springframework.web.bind.annotation.RequestParam(value = "date", required = false) String date) {
        authorizationService.requirePermission("analytics.dashboard.view");
        // T4.3 分时段客流：date 为空 → 退回最新经营日（与总览日期口径一致）；scope 隔离（商户仅自身票务核销）。
        return ApiResponse.ok(service.trafficHourly(
                dashboardScope(), parseOverviewDate(date)));
    }

    @GetMapping("/group-overview")
    public ApiResponse<GroupOverviewResponse> getGroupOverview(
            @org.springframework.web.bind.annotation.RequestParam(value = "groupBy", required = false) String groupBy) {
        authorizationService.requirePermission("analytics.group.view");
        // 集团总览为平台级跨商户汇总：fail-closed 拒绝 merchant-scoped 调用方（与其它平台级分析端点纵深防御一致）；
        // M737：其余按 analytics.group.view 可触及的项目出行。
        return ApiResponse.ok(service.groupOverview(groupBy, reach("analytics.group.view")));
    }

    @GetMapping("/occupancy/vacant-spaces")
    public ApiResponse<PageResponse<AnalyticsRecords.VacantSpaceRow>> listVacantSpaces(
            @org.springframework.web.bind.annotation.RequestParam(value = "limit", required = false, defaultValue = "200") int limit,
            @org.springframework.web.bind.annotation.RequestParam(value = "offset", required = false, defaultValue = "0") int offset) {
        authorizationService.requirePermission("contract.view");
        // M737：商户、外包照旧被拒；其余只出项目列落在 contract.view 可触及项目内的行（平台管理员、勾了全部项目的不过滤）。
        java.util.List<AnalyticsRecords.VacantSpaceRow> items =
                service.listVacantSpaces(limit, offset, projectIds(reach("contract.view")));
        return ApiResponse.ok(new PageResponse<>(items, 1, Math.min(Math.max(limit, 1), 200), items.size()));
    }

    @GetMapping("/occupancy/expiring-contracts")
    public ApiResponse<PageResponse<AnalyticsRecords.ExpiringContractRow>> listExpiringContracts(
            @org.springframework.web.bind.annotation.RequestParam(value = "window_days", required = false, defaultValue = "90") int windowDays,
            @org.springframework.web.bind.annotation.RequestParam(value = "limit", required = false, defaultValue = "200") int limit,
            @org.springframework.web.bind.annotation.RequestParam(value = "offset", required = false, defaultValue = "0") int offset) {
        authorizationService.requirePermission("contract.view");
        java.util.List<AnalyticsRecords.ExpiringContractRow> items =
                service.listExpiringContracts(windowDays, limit, offset, projectIds(reach("contract.view")));
        return ApiResponse.ok(new PageResponse<>(items, 1, Math.min(Math.max(limit, 1), 200), items.size()));
    }

    @GetMapping("/monthly-comparison")
    public ApiResponse<AnalyticsRecords.MonthlyComparisonResponse> monthlyComparison(
            @org.springframework.web.bind.annotation.RequestParam(value = "month", required = false) String month) {
        authorizationService.requirePermission("analytics.dashboard.view");
        requireWholePlatform("analytics.dashboard.view");
        return ApiResponse.ok(service.monthlyComparison(month));
    }

    @GetMapping("/reports/monthly-export")
    @com.complex.platform.access.ConsumesSingleUseGrant("analytics.report.export") // M723：导出导一次扣一次（操作员 2026-10-03）
    public ApiResponse<AnalyticsRecords.MonthlyComparisonResponse> exportMonthlyReport(
            @org.springframework.web.bind.annotation.RequestParam(value = "month", required = false) String month) {
        // 治理口径(V196 范式):导出与查看分权——看板 view 可看,导出须独立高危权限,不直授审计角色。
        authorizationService.requirePermission("analytics.report.export");
        // M737（操作员 2026-10-08 所选选项原文：「范围按他「能不能看全平台合计」判，导出授权只管「能不能导」。」「看不了全平台合计的人，
        // 有导出授权也导不了（被拒、不扣次数）。」）：范围按查看经营分析看板的权限判；被拒时单次消耗随请求事务回滚。
        requireWholePlatform("analytics.dashboard.view");
        return ApiResponse.ok(service.exportMonthlyReport(month));
    }

    @GetMapping("/rent-to-sales")
    public ApiResponse<AnalyticsRecords.RentToSalesResponse> rentToSales(
            @org.springframework.web.bind.annotation.RequestParam(value = "month", required = false) String month) {
        // P1c 店级/业态级租售比：租金(月租+营业额抽成) / 第三方回流营业额；店级/业态级/平台级三档，
        // 平台级分母含未归属营业额（诚实标注）。跨商户平台级聚合→ fail-closed 拒绝商户级调用方；
        // M737：其余按 contract.view 可触及的项目出在其中经营的商户，平台合计只给能看全平台的人。
        authorizationService.requirePermission("contract.view");
        return ApiResponse.ok(service.rentToSales(month, reach("contract.view")));
    }

    @GetMapping("/abandoned-entitlement-revenue")
    public ApiResponse<AnalyticsRecords.AbandonedEntitlementRevenueResponse> abandonedEntitlementRevenue(
            @org.springframework.web.bind.annotation.RequestParam(value = "start", required = false) String start,
            @org.springframework.web.bind.annotation.RequestParam(value = "end", required = false) String end) {
        // 结算B2 放弃权益额外收入交叉表（票种×权益类型）：只读、复用分析只读权限（不新造权限点）；
        // 数据源为归项目流水（project_id 恒 NULL）→ 平台级跨商户聚合，fail-closed 拒绝商户级调用方。
        authorizationService.requirePermission("analytics.dashboard.view");
        requireWholePlatform("analytics.dashboard.view");
        java.time.LocalDate startDate = parseOverviewDate(start);
        java.time.LocalDate endDate = parseOverviewDate(end);
        if (startDate == null || endDate == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "start 与 end 为必填的 YYYY-MM-DD 日期");
        }
        if (!startDate.isBefore(endDate)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "start 必须早于 end（start 含、end 不含）");
        }
        return ApiResponse.ok(service.abandonedEntitlementRevenue(startDate, endDate));
    }

    @GetMapping("/work-order-sla/overdue-orders")
    public ApiResponse<PageResponse<AnalyticsRecords.OverdueWorkOrderRow>> listOverdueWorkOrders(
            @org.springframework.web.bind.annotation.RequestParam(value = "source", required = false, defaultValue = "facility") String source,
            @org.springframework.web.bind.annotation.RequestParam(value = "limit", required = false, defaultValue = "200") int limit,
            @org.springframework.web.bind.annotation.RequestParam(value = "offset", required = false, defaultValue = "0") int offset) {
        authorizationService.requirePermission("facility.work_order.manage");
        java.util.List<AnalyticsRecords.OverdueWorkOrderRow> items =
                service.listOverdueWorkOrders(source, limit, offset, projectIds(reach("facility.work_order.manage")));
        return ApiResponse.ok(new PageResponse<>(items, 1, Math.min(Math.max(limit, 1), 200), items.size()));
    }

    @GetMapping("/energy/abnormal-readings")
    public ApiResponse<PageResponse<AnalyticsRecords.AbnormalReadingRow>> listAbnormalReadings(
            @org.springframework.web.bind.annotation.RequestParam(value = "limit", required = false, defaultValue = "200") int limit,
            @org.springframework.web.bind.annotation.RequestParam(value = "offset", required = false, defaultValue = "0") int offset) {
        authorizationService.requirePermission("energy.reading.manage");
        java.util.List<AnalyticsRecords.AbnormalReadingRow> items =
                service.listAbnormalReadings(limit, offset, projectIds(reach("energy.reading.manage")));
        return ApiResponse.ok(new PageResponse<>(items, 1, Math.min(Math.max(limit, 1), 200), items.size()));
    }

    @GetMapping("/finance-receivables/debtors")
    public ApiResponse<PageResponse<AnalyticsRecords.ArDebtorRow>> listOverdueDebtors(
            @org.springframework.web.bind.annotation.RequestParam(value = "bucket", required = false) String bucket,
            @org.springframework.web.bind.annotation.RequestParam(value = "limit", required = false, defaultValue = "200") int limit,
            @org.springframework.web.bind.annotation.RequestParam(value = "offset", required = false, defaultValue = "0") int offset) {
        authorizationService.requirePermission("finance.ar_ap.view");
        java.util.List<AnalyticsRecords.ArDebtorRow> items =
                service.listOverdueDebtors(bucket, limit, offset, projectIds(reach("finance.ar_ap.view")));
        return ApiResponse.ok(new PageResponse<>(items, 1, Math.min(Math.max(limit, 1), 200), items.size()));
    }

    @GetMapping("/business-day-cutoffs")
    public ApiResponse<PageResponse<AnalyticsRecords.BusinessDayCutoffRecord>> listBusinessDayCutoffs() {
        authorizationService.requirePermission("analytics.dashboard.view");
        authorizationService.requirePlatformScope();
        java.util.List<AnalyticsRecords.BusinessDayCutoffRecord> items = operatingParamsService.listBusinessDayCutoffs();
        return ApiResponse.ok(new PageResponse<>(items, 1, 50, items.size()));
    }

    @PostMapping("/business-day-cutoffs")
    public ApiResponse<AnalyticsRecords.BusinessDayCutoffRecord> upsertBusinessDayCutoff(
            @RequestBody @jakarta.validation.Valid BusinessDayCutoffRequest request) {
        authorizationService.requirePermission("analytics.operating_params.manage");
        authorizationService.requirePlatformScope();
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(operatingParamsService.upsertBusinessDayCutoff(request));
    }

    @GetMapping("/holiday-calendar")
    public ApiResponse<PageResponse<AnalyticsRecords.HolidayPeriod>> listHolidayPeriods(
            @org.springframework.web.bind.annotation.RequestParam(value = "year", required = false) Integer year) {
        authorizationService.requirePermission("analytics.dashboard.view");
        authorizationService.requirePlatformScope();
        java.util.List<AnalyticsRecords.HolidayPeriod> items = operatingParamsService.listHolidayPeriods(year);
        return ApiResponse.ok(new PageResponse<>(items, 1, 100, items.size()));
    }

    @PostMapping("/holiday-calendar")
    public ApiResponse<AnalyticsRecords.HolidayPeriod> upsertHolidayPeriod(
            @RequestBody @jakarta.validation.Valid HolidayPeriodRequest request) {
        authorizationService.requirePermission("analytics.operating_params.manage");
        authorizationService.requirePlatformScope();
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(operatingParamsService.upsertHolidayPeriod(request));
    }

    @PostMapping("/holiday-calendar/fetch")
    public ApiResponse<java.util.Map<String, Object>> fetchHolidayCalendar(
            @RequestBody @jakarta.validation.Valid HolidayFetchRequest request) {
        authorizationService.requirePermission("analytics.operating_params.manage");
        authorizationService.requirePlatformScope();
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(operatingParamsService.fetchHolidayCalendar(request));
    }

    @GetMapping("/holiday-comparison")
    public ApiResponse<AnalyticsRecords.HolidayComparisonResponse> holidayComparison(
            @org.springframework.web.bind.annotation.RequestParam("holiday") String holiday,
            @org.springframework.web.bind.annotation.RequestParam("year") int year) {
        authorizationService.requirePermission("analytics.dashboard.view");
        requireWholePlatform("analytics.dashboard.view");
        return ApiResponse.ok(service.holidayComparison(holiday, year));
    }

    @GetMapping("/anomaly-config")
    public ApiResponse<AnalyticsRecords.AnomalyConfig> getAnomalyConfig() {
        authorizationService.requirePermission("analytics.dashboard.view");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(operatingParamsService.getAnomalyConfig());
    }

    @PostMapping("/anomaly-config")
    public ApiResponse<AnalyticsRecords.AnomalyConfig> updateAnomalyConfig(
            @RequestBody @jakarta.validation.Valid AnomalyConfigRequest request) {
        authorizationService.requirePermission("analytics.operating_params.manage");
        authorizationService.requirePlatformScope();
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(operatingParamsService.updateAnomalyConfig(request));
    }

    @GetMapping("/notification-windows")
    public ApiResponse<java.util.List<AnalyticsRecords.NotificationWindow>> listNotificationWindows() {
        authorizationService.requirePermission("analytics.dashboard.view");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(operatingParamsService.listNotificationWindows());
    }

    @PostMapping("/notification-windows")
    public ApiResponse<AnalyticsRecords.NotificationWindow> upsertNotificationWindow(
            @RequestBody @jakarta.validation.Valid NotificationWindowRequest request) {
        authorizationService.requirePermission("platform.notification_window.manage");
        authorizationService.requirePlatformScope();
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(operatingParamsService.upsertNotificationWindow(request));
    }

    @GetMapping("/ai-thresholds")
    public ApiResponse<AnalyticsRecords.AiThresholds> getAiThresholds() {
        authorizationService.requirePermission("analytics.dashboard.view");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(operatingParamsService.getAiThresholds());
    }

    @PostMapping("/ai-thresholds")
    public ApiResponse<AnalyticsRecords.AiThresholds> updateAiThresholds(
            @RequestBody @jakarta.validation.Valid AiThresholdsRequest request) {
        authorizationService.requirePermission("analytics.operating_params.manage");
        authorizationService.requirePlatformScope();
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(operatingParamsService.updateAiThresholds(request));
    }

    @GetMapping("/revenue-attribution-rules")
    public ApiResponse<java.util.List<AnalyticsRecords.MerchantRevenueAttributionRule>> listRevenueAttributionRules() {
        authorizationService.requirePermission("analytics.dashboard.view");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(operatingParamsService.listRevenueAttributionRules());
    }

    @PostMapping("/revenue-attribution-rules")
    public ApiResponse<AnalyticsRecords.MerchantRevenueAttributionRule> upsertRevenueAttributionRule(
            @RequestBody @jakarta.validation.Valid RevenueAttributionRuleRequest request) {
        authorizationService.requirePermission("analytics.operating_params.manage");
        authorizationService.requirePlatformScope();
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(operatingParamsService.upsertRevenueAttributionRule(
                request, authorizationService.currentActor().id()));
    }

    @GetMapping("/operating-params")
    public ApiResponse<PageResponse<OperatingParamsRecord>> listOperatingParams() {
        authorizationService.requirePermission("analytics.dashboard.view");
        // 综合体经营参数=平台级主数据：fail-closed 拒绝 merchant-scoped 调用方（与其它平台级分析端点一致）。
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(operatingParamsService.listOperatingParams());
    }

    @PostMapping("/operating-params")
    public ApiResponse<OperatingParamsRecord> upsertOperatingParams(
            @RequestBody(required = false) @jakarta.validation.Valid OperatingParamsRequest request) {
        authorizationService.requirePermission("analytics.operating_params.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(operatingParamsService.upsertOperatingParams(request));
    }

    @GetMapping("/business-metric-snapshots")
    public ApiResponse<PageResponse<BusinessMetricSnapshotRecord>> listBusinessMetricSnapshots() {
        authorizationService.requirePermission("analytics.dashboard.view");
        // M737：商户、外包照旧被拒；其余按 analytics.dashboard.view 可触及的项目逐行留（平台段只给能看全平台的人）。
        return ApiResponse.ok(service.listBusinessMetricSnapshots(reach("analytics.dashboard.view")));
    }

    @GetMapping("/merchant-health")
    public ApiResponse<PageResponse<MerchantHealthScoreRecord>> listMerchantHealthScores() {
        authorizationService.requirePermission("analytics.dashboard.view");
        var actor = authorizationService.currentActor();
        if (projectStaff(actor)) {
            return ApiResponse.ok(service.listMerchantHealthScoresInProjects(
                    authorizationService.projectReach(actor, "analytics.dashboard.view").organizationIds()));
        }
        return ApiResponse.ok(service.listMerchantHealthScores(authorizationService.currentMerchantScope()));
    }

    @GetMapping("/merchants")
    public ApiResponse<PageResponse<MerchantSummaryRecord>> listAnalyticsMerchants() {
        authorizationService.requirePermission("analytics.dashboard.view");
        var actor = authorizationService.currentActor();
        if (projectStaff(actor)) {
            return ApiResponse.ok(service.listAnalyticsMerchantsInProjects(
                    authorizationService.projectReach(actor, "analytics.dashboard.view").organizationIds()));
        }
        return ApiResponse.ok(service.listAnalyticsMerchants(authorizationService.currentMerchantScope()));
    }

    @GetMapping("/metric-detail")
    public ApiResponse<AnalyticsRecords.MetricDetailResponse> getMetricDetail(
            @org.springframework.web.bind.annotation.RequestParam("metric") String metric,
            @org.springframework.web.bind.annotation.RequestParam(value = "part", required = false) String part,
            @org.springframework.web.bind.annotation.RequestParam(value = "project_id", required = false) String projectId,
            @org.springframework.web.bind.annotation.RequestParam(value = "merchant_id", required = false) String merchantId,
            @org.springframework.web.bind.annotation.RequestParam(value = "from", required = false) String from,
            @org.springframework.web.bind.annotation.RequestParam(value = "to", required = false) String to,
            @org.springframework.web.bind.annotation.RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @org.springframework.web.bind.annotation.RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        // KPI 统一明细（指标登记簿，2026-07-18 方案A）：契约宽权限（analytics.dashboard.view）+
        // 按登记项权限二次校验，双重 fail-closed。
        authorizationService.requirePermission("analytics.dashboard.view");
        MetricRegistry.MetricDefinition definition = MetricRegistry.require(metric);
        authorizationService.requirePermission(definition.permission());
        // requirePlatformScope 拦商户级/外包供应商（放行项目级）；M737：可触及项目按两个权限码取交集，项目作用域在下方由它派生。
        AuthorizationService.ProjectReach reach = reach("analytics.dashboard.view", definition.permission());
        if (merchantId != null && !merchantId.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "登记指标不支持商户维度（merchant_id）过滤");
        }
        if (projectId != null && !projectId.isBlank()) {
            // 作用域由 token 派生（防越权：不接受客户端指定项目），项目级只能看自己可查看的项目。
            throw ApiException.badRequest("VALIDATION_FAILED", "作用域由登录身份派生，不接受客户端指定项目维度（project_id）过滤");
        }
        // 项目作用域收紧（2026-07-19 用户拍板；M737 改按可跨项目角色判定）：平台管理员→null=看平台全量（不变）；
        // 勾了全部项目的人看没有项目列的指标→null=平台全量（治理地图「KPI 可见性分层」：「全平台合计只给平台管理员和勾选了「可以查看全部项目」的人看」）；
        // 其余一律传可触及项目的集合：登记了项目列的指标按它过滤——勾了全部项目的人也过滤（治理地图「权限规则」：
        // 「平台公司员工、平台管理员、最高权限、外部机构账号一律碰不到」，人事类指标的来源表里有这些账号）；
        // platform_only 指标遇非 null → 服务层 fail-closed 403。
        java.util.Set<String> projectScope =
                reach.platformAdmin() || (reach.allProjects() && !MetricRegistry.isProjectScoped(definition))
                        ? null : reach.organizationIds();
        return ApiResponse.ok(service.metricDetail(metric, part, parseOverviewDate(from), parseOverviewDate(to), page, size, projectScope));
    }

    @GetMapping("/metric-anomalies")
    public ApiResponse<PageResponse<MetricAnomalyRecord>> listMetricAnomalies() {
        authorizationService.requirePermission("analytics.risk_alert.view");
        requireWholePlatform("analytics.risk_alert.view");
        return ApiResponse.ok(service.listMetricAnomalies());
    }

    @PostMapping("/metric-anomalies/{id}/dispose")
    public ApiResponse<MetricAnomalyRecord> disposeMetricAnomaly(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid AnomalyDispositionRequest request
    ) {
        authorizationService.requirePermission("analytics.metric_anomaly.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        requireWholePlatform("analytics.metric_anomaly.manage");
        return ApiResponse.ok(service.disposeMetricAnomaly(id, request));
    }

    @GetMapping("/metric-anomalies/{id}/timeline")
    public ApiResponse<List<AuditEvent>> metricAnomalyTimeline(@PathVariable String id) {
        // 处置历史时间线(append-only,不可改):与列表同权限(analytics.risk_alert.view)、平台作用域。
        authorizationService.requirePermission("analytics.risk_alert.view");
        requireWholePlatform("analytics.risk_alert.view");
        return ApiResponse.ok(service.listMetricAnomalyTimeline(id));
    }

    @GetMapping("/metric-definitions")
    public ApiResponse<PageResponse<MetricDefinitionRecord>> listMetricDefinitions() {
        authorizationService.requirePermission("analytics.dashboard.view");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(service.listMetricDefinitions());
    }

    @GetMapping("/risk-alerts")
    public ApiResponse<PageResponse<RiskAlertRecord>> listRiskAlerts() {
        authorizationService.requirePermission("analytics.risk_alert.view");
        requireWholePlatform("analytics.risk_alert.view");
        return ApiResponse.ok(service.listRiskAlerts());
    }

    @PostMapping("/risk-alerts")
    public ApiResponse<RiskAlertRecord> createRiskAlert(@RequestBody(required = false) @jakarta.validation.Valid RiskAlertRequest request) {
        authorizationService.requirePermission("analytics.risk_alert.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        requireWholePlatform("analytics.risk_alert.manage");
        return ApiResponse.ok(service.createRiskAlert(request));
    }

    @PostMapping("/risk-alerts/{id}/resolve")
    public ApiResponse<RiskAlertRecord> resolveRiskAlert(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid StatusChangeRequest request
    ) {
        authorizationService.requirePermission("analytics.risk_alert.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        requireWholePlatform("analytics.risk_alert.manage");
        return ApiResponse.ok(service.resolveRiskAlert(id, request));
    }

    @GetMapping("/review-locks")
    public ApiResponse<PageResponse<AnalyticsRecords.BusinessReviewLockRecord>> listReviewLocks() {
        // 复盘锁定=项目级隔离：仅校验写权限，scope 由 token 派生（service 层过滤）；
        // 不加 requirePlatformScope（那会拒绝合法的项目主管只看/只锁本综合体）。
        authorizationService.requirePermission("analytics.review.lock");
        return ApiResponse.ok(service.listReviewLocks());
    }

    @PostMapping("/review-locks")
    public ApiResponse<AnalyticsRecords.BusinessReviewLockRecord> createReviewLock(
            @RequestBody(required = false) @jakarta.validation.Valid BusinessReviewLockRequest request) {
        authorizationService.requirePermission("analytics.review.lock");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createReviewLock(request));
    }

    @GetMapping("/risk-rules")
    public ApiResponse<PageResponse<RiskRuleRecord>> listRiskRules() {
        authorizationService.requirePermission("analytics.risk_alert.view");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(service.listRiskRules());
    }

    @GetMapping("/robot")
    public ApiResponse<PageResponse<RobotMetricRecord>> getRobotAnalytics() {
        authorizationService.requirePermission("analytics.dashboard.view");
        requireWholePlatform("analytics.dashboard.view");
        return ApiResponse.ok(service.getRobotAnalytics());
    }

    @GetMapping("/staff-service-score")
    public ApiResponse<PageResponse<StaffServiceScoreRecord>> listStaffServiceScores() {
        authorizationService.requirePermission("analytics.risk_alert.view");
        requireWholePlatform("analytics.risk_alert.view");
        return ApiResponse.ok(service.listStaffServiceScores());
    }
}
