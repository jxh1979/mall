package com.complex.platform.analytics;

// 文件版本：V0.05

import com.complex.platform.analytics.AnalyticsRecords.AiQualityResultRecord;
import com.complex.platform.analytics.AnalyticsRecords.AiQualityTaskRecord;
import com.complex.platform.analytics.AnalyticsRecords.BusinessMetricSnapshotRecord;
import com.complex.platform.analytics.AnalyticsRecords.CustomerIntentRecord;
import com.complex.platform.analytics.AnalyticsRecords.DashboardSnapshotRecord;
import com.complex.platform.analytics.AnalyticsRecords.GroupOverviewResponse;
import com.complex.platform.analytics.AnalyticsRecords.GroupProjectRollup;
import com.complex.platform.analytics.AnalyticsRecords.HourlyTrafficBucket;
import com.complex.platform.analytics.AnalyticsRecords.HourlyTrafficResponse;
import com.complex.platform.analytics.AnalyticsRecords.MerchantHealthScoreRecord;
import com.complex.platform.analytics.AnalyticsRecords.MerchantSummaryRecord;
import com.complex.platform.analytics.AnalyticsRecords.MetricAnomalyRecord;
import com.complex.platform.analytics.AnalyticsRecords.MetricDefinitionRecord;
import com.complex.platform.analytics.AnalyticsRecords.MetricValue;
import com.complex.platform.analytics.AnalyticsRecords.OverviewResponse;
import com.complex.platform.analytics.AnalyticsRecords.RiskAlertRecord;
import com.complex.platform.analytics.AnalyticsRecords.RiskRuleRecord;
import com.complex.platform.analytics.AnalyticsRecords.RobotMetricRecord;
import com.complex.platform.analytics.AnalyticsRecords.StaffServiceScoreRecord;
import com.complex.platform.common.PiiTextRedactor;
import com.complex.platform.common.PiiTextRedactor;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.evidence.RiskAlertService;
import com.complex.platform.evidence.dto.RiskAlertReportRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

@Service
public class AnalyticsService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final Set<String> RISK_LEVELS = Set.of("normal", "suspicious", "high", "critical");
    // analytics 本域风险级 → 统一表 RISK_LEVELS 映射。本域取值（经 requireRiskLevel 校验）已与统一枚举同名，
    // 直通即可；额外覆盖历史/其它字段可能出现的别名（medium/low/warning），未命中按 suspicious 兜底——
    // 避免 report 抛 VALIDATION_FAILED 被 catch 静默吞 = 丢数据。
    private static final Map<String, String> RISK_LEVEL_TO_UNIFIED = Map.of(
            "normal", "normal",
            "suspicious", "suspicious",
            "high", "high",
            "critical", "critical",
            "medium", "suspicious",
            "low", "normal",
            "warning", "suspicious");
    private static final Set<String> RISK_ALERT_RESOLVE_ACTIONS = Set.of("investigating", "resolved", "dismissed");
    // 指标异常处置动作与终态(状态机纯函数 anomalyDispositionTarget 使用)。
    static final Set<String> ANOMALY_DISPOSITION_ACTIONS = Set.of("acknowledge", "resolve", "false_positive", "escalate");
    static final Set<String> ANOMALY_TERMINAL_STATUSES = Set.of("resolved", "false_positive");
    private static final Set<String> TERMINAL_ALERT_STATUSES = Set.of("resolved", "dismissed");
    // V218 阈值可配置化:LOCAL_LLM_MIN_CONFIDENCE 迁入 analytics_ai_thresholds,从 repository 读(默认 0.60 兜底)。
    private static final int MAX_SOURCE_TEXT_CHARS = 12_000;
    private static final int MAX_LLM_INPUT_CHARS = 4_000;

    private static final String MERCHANT_SCOPE_UNAVAILABLE =
            "source table has no merchant_id; merchant-scoped value not available";
    private static final String PARKING_NOTE =
            "parking metrics are platform-scope only (parking tables have no merchant_id; not emitted when denominator is zero)";

    /**
     * T12 看板七类指标目录（与 r5-aggregation-metrics-draft §2 / DashboardAggregationJob 对齐）。
     * ready=false 的指标数据源未就绪，按"不伪造"返回 null + note（采购到货依赖 T6；补录依赖 P1 留痕；第三方回流依赖开关接线）。
     */
    static final List<MetricCatalogEntry> METRIC_CATALOG = List.of(
            new MetricCatalogEntry(DashboardAggregationJob.REVENUE_TICKET, "finance", "cent", true,
                    "revenue.ticket is platform-only paid-in-full; merchant ticket share is in revenue.net (V135 merchant_id / allocated)"),
            new MetricCatalogEntry(DashboardAggregationJob.REVENUE_BAR, "bar", "cent", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry(DashboardAggregationJob.REVENUE_BAR_REFUND, "bar", "cent", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry(DashboardAggregationJob.REVENUE_TICKET_REFUND, "finance", "cent", true,
                    "revenue.ticket_refund is platform-only total refund; per-merchant refund share is in revenue.net (V220 ticket_refunds.merchant_id)"),
            // RC1② + 清单#4：派生净收入（平台口径 = 售票实付+酒吧−退款；
            // 商户行级 = 售票分量(allocated)+酒吧实收−售票退款分摊−酒吧退款；第三方回流不计）。
            new MetricCatalogEntry(DashboardAggregationJob.REVENUE_NET, "finance", "cent", true,
                    "no revenue.net snapshot for merchant on business date"),
            // T4.2 坪效（派生：净收入/可经营面积）。面积未配置或商户口径无快照 → 诚实空缺 null + note。
            new MetricCatalogEntry(DashboardAggregationJob.REVENUE_PER_SQM, "finance", "cent_per_sqm", true,
                    "operating area not configured (set via /analytics/operating-params) or no snapshot for business date"),
            new MetricCatalogEntry(DashboardAggregationJob.TRAFFIC_AMUSEMENT, "amusement", "count", true,
                    "amusement_verifications has no merchant_id; merchant-scoped value not available"),
            new MetricCatalogEntry(DashboardAggregationJob.VERIFY_DONE, "ticketing", "count", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry(DashboardAggregationJob.MEMBER_NEW, "customer", "count", true,
                    "customers has no merchant_id; merchant-scoped value not available"),
            new MetricCatalogEntry(DashboardAggregationJob.SUPPLY_BALANCE, "inventory", "count", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry(DashboardAggregationJob.SUPPLY_LOW_STOCK, "inventory", "count", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry(DashboardAggregationJob.RISK_PENDING_REVIEW, "governance", "count", true,
                    "supervisor_review_decisions has no merchant_id; merchant-scoped value not available"),
            // S2 财务应收(地产 BI MVP,期末时点/月累计口径,2026-07-02 拍板):
            new MetricCatalogEntry(DashboardAggregationJob.AR_BALANCE, "finance", "cent", true,
                    "no ar snapshot for merchant on business date"),
            new MetricCatalogEntry(DashboardAggregationJob.AR_AGING_0_30, "finance", "cent", true,
                    "no ar snapshot for merchant on business date"),
            new MetricCatalogEntry(DashboardAggregationJob.AR_AGING_31_60, "finance", "cent", true,
                    "no ar snapshot for merchant on business date"),
            new MetricCatalogEntry(DashboardAggregationJob.AR_AGING_61_90, "finance", "cent", true,
                    "no ar snapshot for merchant on business date"),
            new MetricCatalogEntry(DashboardAggregationJob.AR_AGING_90P, "finance", "cent", true,
                    "no ar snapshot for merchant on business date"),
            new MetricCatalogEntry(DashboardAggregationJob.AR_COLLECTED_MTD, "finance", "cent", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry(DashboardAggregationJob.AR_RECEIVABLE_MTD, "finance", "cent", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry(DashboardAggregationJob.AR_COLLECTION_RATE_MTD, "finance", "rate", true,
                    "collection rate is platform-scope only (no snapshot when month receivable is zero)"),
            // S3 出租率/去化(平台口径,期末时点):
            new MetricCatalogEntry(DashboardAggregationJob.OCC_RATE, "leasing", "rate", true,
                    "出租率按已租面积除以全部铺位面积，含不可租；总面积为零时不产出"),
            new MetricCatalogEntry(DashboardAggregationJob.OCC_RENTABLE_AREA, "leasing", "sqm", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry(DashboardAggregationJob.OCC_TOTAL_AREA, "leasing", "sqm", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry(DashboardAggregationJob.OCC_LEASED_AREA, "leasing", "sqm", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry(DashboardAggregationJob.OCC_SIGNED_PENDING_AREA, "leasing", "sqm", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry(DashboardAggregationJob.OCC_PARKING_RATE, "leasing", "rate", true,
                    "parking rental rate counts spaces (contract-let spaces + registered active staff vehicles); not emitted when no parking spaces"),
            // HP-P01 停车场五项（与车位出租率并列为地产指标，平台口径）：
            new MetricCatalogEntry(DashboardAggregationJob.PARKING_UTILIZATION_RATE, "leasing", "rate", true, PARKING_NOTE),
            new MetricCatalogEntry(DashboardAggregationJob.PARKING_DAILY_TURNOVER, "leasing", "rate", true, PARKING_NOTE),
            new MetricCatalogEntry(DashboardAggregationJob.PARKING_REVENUE, "leasing", "cent", true, PARKING_NOTE),
            new MetricCatalogEntry(DashboardAggregationJob.PARKING_CREDIT_SHARE, "leasing", "rate", true, PARKING_NOTE),
            new MetricCatalogEntry(DashboardAggregationJob.PARKING_AVG_STAY_MINUTES, "leasing", "minutes", true, PARKING_NOTE),
            new MetricCatalogEntry(DashboardAggregationJob.CONTRACT_EXPIRING_90D, "leasing", "count", true, MERCHANT_SCOPE_UNAVAILABLE),
            // V220 招商漏斗/租售比(平台口径;integration_order_syncs 无 merchant_id → rent_to_sales 仅平台级):
            new MetricCatalogEntry(DashboardAggregationJob.LEASING_LEAD_COUNT, "leasing", "count", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry(DashboardAggregationJob.LEASING_INTENT_CONVERSION_RATE, "leasing", "rate", true,
                    "lead-to-intent conversion is platform-scope only (not emitted when no leads in window)"),
            new MetricCatalogEntry(DashboardAggregationJob.LEASING_SIGNED_CONVERSION_RATE, "leasing", "rate", true,
                    "intent-to-signed conversion is platform-scope only (not emitted when no intents in window)"),
            new MetricCatalogEntry(DashboardAggregationJob.LEASING_RENT_TO_SALES, "finance", "rate", true,
                    "rent-to-sales is platform-scope only (third-party turnover has no merchant_id; not emitted when turnover is zero)"),
            // S4 工单时效(平台口径,facility 窗):
            new MetricCatalogEntry(DashboardAggregationJob.WO_RESPONSE_HOURS_P50, "facility", "hours", true,
                    "response P50 is platform-scope only (not emitted when no responded orders in window)"),
            new MetricCatalogEntry(DashboardAggregationJob.WO_HANDLE_HOURS_P50, "facility", "hours", true,
                    "handle P50 is platform-scope only (not emitted when no completed orders in window)"),
            new MetricCatalogEntry(DashboardAggregationJob.WO_OVERDUE_RATE, "facility", "rate", true,
                    "overdue rate is platform-scope only (not emitted when no orders due in window)"),
            new MetricCatalogEntry(DashboardAggregationJob.WO_BACKLOG, "facility", "count", true, MERCHANT_SCOPE_UNAVAILABLE),
            // 清单#13 商户服务工单时效(平台口径,facility 窗;流转无响应节点故无响应 P50):
            new MetricCatalogEntry(DashboardAggregationJob.MWO_HANDLE_HOURS_P50, "facility", "hours", true,
                    "merchant work-order handle P50 is platform-scope only (submit to close; not emitted when no closed orders in window)"),
            new MetricCatalogEntry(DashboardAggregationJob.MWO_OVERDUE_RATE, "facility", "rate", true,
                    "overdue per V120 sla_breached plus realtime fallback; cancelled excluded; not emitted when no orders due in window"),
            new MetricCatalogEntry(DashboardAggregationJob.MWO_BACKLOG, "facility", "count", true, MERCHANT_SCOPE_UNAVAILABLE),
            // S5 能耗(平台口径,energy 窗;按表计类型分列,单位不同不跨类合计):
            new MetricCatalogEntry("energy.total_electricity", "energy", "kwh", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry("energy.total_water", "energy", "ton", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry("energy.total_gas", "energy", "m3", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry("energy.total_heat_cold", "energy", "unit", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry(DashboardAggregationJob.ENERGY_ELECTRICITY_PER_SQM, "energy", "kwh_per_sqm", true,
                    "per-sqm uses rentable area as denominator (not emitted when rentable area is zero)"),
            new MetricCatalogEntry(DashboardAggregationJob.ENERGY_ABNORMAL_COUNT, "energy", "count", true, MERCHANT_SCOPE_UNAVAILABLE),
            // 数据源未就绪（不伪造）：
            new MetricCatalogEntry("revenue.thirdparty", "finance", "cent", false,
                    "third-party reflow depends on integration capability switch wiring (not yet aggregated)"),
            new MetricCatalogEntry("supply.received", "inventory", "count", false,
                    "purchase receipt depends on T6 procurement cycle (data source not ready)"),
            new MetricCatalogEntry("risk.manual_backfill", "governance", "count", false,
                    "manual backfill count depends on P1 audit-trail wiring (data source not ready)")
    );

    /**
     * P2 集团总览（R6-D13）：当前单综合体——平台口径 ETL 快照即该综合体（project）的跨商户汇总。
     * 用 V037 既有合成 project 标识，仅作 rollup 行 key/展示名，不假装多 project。
     */
    private static final String GROUP_PROJECT_ID = "project_1";
    private static final String GROUP_PROJECT_NAME = "综合体（默认）";
    // V218 阈值可配置化:BENCHMARK_THRESHOLD 迁入 analytics_ai_thresholds,从 repository 读(默认 0.20 兜底)。

    /**
     * 集团总览汇总指标目录：收入（净收入/售票/酒吧）/客流/核销/风险，来源均为 DashboardAggregationJob 平台口径 ETL 快照。
     * 商户数另由 countMerchantsWithEtlSnapshot 真实计数（非快照指标），不在此目录。
     */
    private static final List<GroupMetricEntry> GROUP_METRIC_CATALOG = List.of(
            new GroupMetricEntry(DashboardAggregationJob.REVENUE_NET, "cent"),
            new GroupMetricEntry(DashboardAggregationJob.REVENUE_TICKET, "cent"),
            new GroupMetricEntry(DashboardAggregationJob.REVENUE_BAR, "cent"),
            new GroupMetricEntry(DashboardAggregationJob.TRAFFIC_AMUSEMENT, "count"),
            new GroupMetricEntry(DashboardAggregationJob.VERIFY_DONE, "count"),
            new GroupMetricEntry(DashboardAggregationJob.RISK_PENDING_REVIEW, "count")
    );

    private final AnalyticsRepository repository;

    // S4/S5 明细窗:与 ETL 同源的经营日窗(未配置域回落 4 点基准)。setter 注入保持既有构造器族不变(单测用默认)。
    private com.complex.platform.common.BusinessDayResolver businessDayResolver =
            new com.complex.platform.common.BusinessDayResolver(4);
    private final AuditedOperationExecutor operations;
    private final Clock clock;
    private final LocalLlmClient localLlmClient;
    // P1：evidence 统一异常责任链服务。analytics 风险写入除落孤岛 analytics_risk_alerts 外，
    // 再双写统一 risk_alerts（source_domain='analytics'），复刻 Wave 1 Wecom 先例（fail-safe、孤岛表仍权威、统一表汇聚镜像）。
    // 可为 null（向后兼容/现有单测用旧构造器）；接入为 fail-safe，不阻塞创建主链。
    private final RiskAlertService riskAlertService;

    // V216 经营复盘锁定：项目 scope 与操作人由 token 派生（AC-ISO-2，不取请求体）。
    // setter 注入保持既有 4 个构造器族不变（现有单测用旧构造器，authorizationService=null 时按平台全量兜底）。
    private com.complex.platform.access.AuthorizationService authorizationService;

    // 第六批：两者在控制器切片测试（AnalyticsControllerTests，AnalyticsService 为 Mockito 桩）里无 bean，
    // 与上方 authorizationService 同用 required=false，调用点已按 null 兜底。
    @Autowired(required = false)
    private com.complex.platform.finance.FinancePayrollService financePayroll;
    @Autowired(required = false)
    private com.complex.platform.access.staff.StaffPayrollService staffPayroll;

    // M737：平台管理员的可触及范围（一切）。各「平台全量便捷重载」与未注入判定服务时的兜底共用。
    private static final com.complex.platform.access.AuthorizationService.ProjectReach PLATFORM_ADMIN_REACH =
            new com.complex.platform.access.AuthorizationService.ProjectReach(true, true, null, true, Set.of());

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setAuthorizationService(com.complex.platform.access.AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Autowired(required = false)
    void setBusinessDayResolver(com.complex.platform.common.BusinessDayResolver businessDayResolver) {
        this.businessDayResolver = businessDayResolver;
    }

    @Autowired
    public AnalyticsService(
            AnalyticsRepository repository,
            AuditedOperationExecutor operations,
            LocalLlmClient localLlmClient,
            RiskAlertService riskAlertService
    ) {
        this(repository, operations, Clock.systemUTC(), localLlmClient, riskAlertService);
    }

    // 向后兼容旧构造器：未注入统一责任链服务（riskAlertService=null），双写短路、仅孤岛表权威。
    public AnalyticsService(
            AnalyticsRepository repository,
            AuditedOperationExecutor operations,
            LocalLlmClient localLlmClient
    ) {
        this(repository, operations, Clock.systemUTC(), localLlmClient, null);
    }

    // 向后兼容旧构造器（带固定 Clock，现有单测用）：riskAlertService=null，双写短路。
    AnalyticsService(
            AnalyticsRepository repository,
            AuditedOperationExecutor operations,
            Clock clock,
            LocalLlmClient localLlmClient
    ) {
        this(repository, operations, clock, localLlmClient, null);
    }

    AnalyticsService(
            AnalyticsRepository repository,
            AuditedOperationExecutor operations,
            Clock clock,
            LocalLlmClient localLlmClient,
            RiskAlertService riskAlertService
    ) {
        this.repository = repository;
        this.operations = operations;
        this.clock = clock;
        this.localLlmClient = localLlmClient;
        this.riskAlertService = riskAlertService;
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


    /**
     * T12 经营总览：以 metric_code 为权威键返回指标 map，经 scope 行级隔离。
     * scope=Optional.empty() → 平台口径（全量）；scope=Optional.of(merchantId) → 仅本商户。
     * 数据源未就绪的指标返回 value=null + note（不伪造）。
     */
    public OverviewResponse overview(Optional<String> scope) {
        return overview(scope, null);
    }

    /**
     * T4.1 修日期「假筛选」：按指定经营日 as-of 读快照（此前永远取最新 ETL 日、忽略前端日期）。
     * businessDate 为空 → 退回最新 ETL 经营日（默认行为不变）。区间跨日聚合待口径确定后再做。
     */
    public OverviewResponse overview(Optional<String> scope, java.time.LocalDate businessDate) {
        return readSingle(
                "analytics.dashboard_viewed",
                "analytics.dashboard_view_failed",
                "analytics.dashboard.view",
                "analytics_dashboard_snapshot",
                () -> buildOverview(scope, null, businessDate)
        );
    }

    private static final java.util.Set<String> AR_BUCKETS =
            java.util.Set.of("a0_30", "a31_60", "a61_90", "a90p");

    // M737：以下钻取明细方法带末参 projectIds（控制器按入口权限码算出的可触及项目）——null=不过滤（只有平台管理员；勾了全部项目的人自 M737-F07 起传可触及项目），
    //   非 null 只出项目列落在集合内的行，空集合一行不出。原签名保留为「平台全量便捷重载」（传 null）。
    /** S2 钻取:欠费商户明细排行(平台全量便捷重载=不按项目过滤)。 */
    public java.util.List<AnalyticsRecords.ArDebtorRow> listOverdueDebtors(String bucket, int limit, int offset) {
        return listOverdueDebtors(bucket, limit, offset, null);
    }

    /** S2 钻取:欠费商户明细排行(实时,与账龄快照同过滤同口径;bucket 可空=全部)。 */
    public java.util.List<AnalyticsRecords.ArDebtorRow> listOverdueDebtors(String bucket, int limit, int offset,
            java.util.Collection<String> projectIds) {
        String normalized = bucket == null || bucket.isBlank() ? null : bucket;
        if (normalized != null && !AR_BUCKETS.contains(normalized)) {
            throw com.complex.platform.common.api.ApiException.badRequest(
                    "VALIDATION_FAILED", "账龄分组必须为以下之一：" + AR_BUCKETS);
        }
        String finalBucket = normalized;
        return readSingle(
                "analytics.ar_debtors.viewed",
                "analytics.ar_debtors.view_failed",
                "finance.ar_ap.view",
                "ar_ap_items",
                () -> repository.listOverdueDebtors(
                        LocalDate.ofInstant(Instant.now(clock), java.time.ZoneOffset.UTC), finalBucket, limit, offset, projectIds)
        );
    }

    /** S3 钻取:未出租铺位明细(平台全量便捷重载=不按项目过滤)。 */
    public java.util.List<AnalyticsRecords.VacantSpaceRow> listVacantSpaces(int limit, int offset) {
        return listVacantSpaces(limit, offset, null);
    }

    /** S3 钻取:未出租铺位明细(实时,与出租率快照同过滤同口径)。 */
    public java.util.List<AnalyticsRecords.VacantSpaceRow> listVacantSpaces(int limit, int offset,
            java.util.Collection<String> projectIds) {
        return readSingle(
                "analytics.vacant_spaces.viewed",
                "analytics.vacant_spaces.view_failed",
                "contract.view",
                "spaces",
                () -> repository.listVacantSpaces(
                        LocalDate.ofInstant(Instant.now(clock), java.time.ZoneOffset.UTC), limit, offset, projectIds)
        );
    }

    /** S3 钻取:到期预警合同明细(平台全量便捷重载=不按项目过滤)。 */
    public java.util.List<AnalyticsRecords.ExpiringContractRow> listExpiringContracts(int windowDays, int limit, int offset) {
        return listExpiringContracts(windowDays, limit, offset, null);
    }

    /** S3 钻取:到期预警合同明细(窗口天数 1-365)。 */
    public java.util.List<AnalyticsRecords.ExpiringContractRow> listExpiringContracts(int windowDays, int limit, int offset,
            java.util.Collection<String> projectIds) {
        int days = Math.min(Math.max(windowDays, 1), 365);
        return readSingle(
                "analytics.expiring_contracts.viewed",
                "analytics.expiring_contracts.view_failed",
                "contract.view",
                "contracts",
                () -> repository.listExpiringContracts(
                        LocalDate.ofInstant(Instant.now(clock), java.time.ZoneOffset.UTC), days, limit, offset, projectIds)
        );
    }

    /** M1 月度对比目录(展示白名单;label=业务名)。聚合口径由 job 月度白名单单一事实源派生,杜绝两处漂移。 */
    private record MonthlyMetricEntry(String key, String label, String domain, String unit) {
    }

    private static final List<MonthlyMetricEntry> MONTHLY_COMPARISON_CATALOG = List.of(
            new MonthlyMetricEntry(DashboardAggregationJob.REVENUE_NET, "净收入（含酒吧已确认部分）", "finance", "cent"),
            new MonthlyMetricEntry(DashboardAggregationJob.REVENUE_TICKET, "售票收入", "ticketing", "cent"),
            new MonthlyMetricEntry(DashboardAggregationJob.REVENUE_BAR, "酒吧收入（已确认部分）", "bar", "cent"),
            new MonthlyMetricEntry("revenue.contract_rent", "合同租金挂账", "finance", "cent"),
            new MonthlyMetricEntry("member.new", "新增会员", "customer", "count"),
            new MonthlyMetricEntry("verify.done", "核销次数", "ticketing", "count"),
            new MonthlyMetricEntry("traffic.amusement", "游乐人次", "amusement", "count"),
            new MonthlyMetricEntry("energy.total_electricity", "用电量", "energy", "kwh"),
            new MonthlyMetricEntry("energy.total_water", "用水量", "energy", "ton"),
            new MonthlyMetricEntry("energy.total_gas", "用气量", "energy", "m3"),
            new MonthlyMetricEntry("energy.total_heat_cold", "冷热量", "energy", "unit"),
            new MonthlyMetricEntry(DashboardAggregationJob.AR_BALANCE, "欠费余额", "finance", "cent"),
            new MonthlyMetricEntry(DashboardAggregationJob.AR_AGING_90P, "90天以上欠费", "finance", "cent"),
            new MonthlyMetricEntry(DashboardAggregationJob.AR_COLLECTION_RATE_MTD, "当月回款率", "finance", "rate"),
            new MonthlyMetricEntry(DashboardAggregationJob.OCC_RATE, "出租率", "leasing", "rate"),
            new MonthlyMetricEntry(DashboardAggregationJob.OCC_PARKING_RATE, "车位出租率", "leasing", "rate"),
            new MonthlyMetricEntry(DashboardAggregationJob.PARKING_REVENUE, "停车收入", "leasing", "cent"),
            new MonthlyMetricEntry(DashboardAggregationJob.OCC_SIGNED_PENDING_AREA, "已签待交面积", "leasing", "sqm"),
            new MonthlyMetricEntry(DashboardAggregationJob.CONTRACT_EXPIRING_90D, "90天内到期合同", "leasing", "count"),
            new MonthlyMetricEntry(DashboardAggregationJob.WO_BACKLOG, "工单积压", "facility", "count"));

    /** M1 月度对比:环比=上月 monthly 行、同比=去年同月;缺行=null(前端标「积累中」),分母0不出百分比(不伪造)。 */
    public AnalyticsRecords.MonthlyComparisonResponse monthlyComparison(String month) {
        java.time.YearMonth yearMonth = parseMonth(month);
        return readSingle(
                "analytics.monthly_comparison.viewed",
                "analytics.monthly_comparison.view_failed",
                "analytics.dashboard.view",
                "analytics_business_metric_snapshots",
                () -> buildMonthlyComparison(yearMonth));
    }

    /** M2 导出:与对比中心同一份组装逻辑(图表=导出,单一口径);权限独立分权(V206,V196 范式)。 */
    public AnalyticsRecords.MonthlyComparisonResponse exportMonthlyReport(String month) {
        java.time.YearMonth yearMonth = parseMonth(month);
        return readSingle(
                "analytics.monthly_report.exported",
                "analytics.monthly_report.export_failed",
                "analytics.report.export",
                "analytics_business_metric_snapshots",
                () -> buildMonthlyComparison(yearMonth));
    }

    /** P1c 店级/业态级租售比（2026-07-21 拍板）：租金(月租+营业额抽成)/第三方回流营业额；三档+未归属标注。
     *  权限 contract.view（与出租看板同口径受众）；平台级跨商户汇总在控制器 requirePlatformScope。
     *  平台全量便捷重载（按平台管理员的可触及范围），只给现有单测用。控制器走带 reach 的版本。 */
    public AnalyticsRecords.RentToSalesResponse rentToSales(String month) {
        return rentToSales(month, PLATFORM_ADMIN_REACH);
    }

    /**
     * M737（业务地图「谁会看」：「项目级操作人员只看自己所在项目范围内的 KPI 和明细，看不到别的项目。」治理地图「KPI 可见性分层」：
     * 「全平台合计只给平台管理员和勾选了「可以查看全部项目」的人看」）：reach 由控制器按 contract.view 算好——平台管理员与勾了全部项目的人
     * 看平台合计与未归属营业额；逐商户行只有平台管理员看全部，其余（含勾了全部项目的人，M737-F06）只出在可触及项目内经营的商户行与由这些行
     * 并出的业态行；不能看全平台的人不出平台合计与未归属营业额。
     */
    public AnalyticsRecords.RentToSalesResponse rentToSales(
            String month, com.complex.platform.access.AuthorizationService.ProjectReach reach) {
        String period = parseMonth(month).toString();
        return readSingle(
                "analytics.rent_to_sales.viewed",
                "analytics.rent_to_sales.view_failed",
                "contract.view",
                "ar_ap_items",
                () -> buildRentToSales(period, reach));
    }

    private AnalyticsRecords.RentToSalesResponse buildRentToSales(
            String period, com.complex.platform.access.AuthorizationService.ProjectReach reach) {
        AnalyticsRecords.RentToSalesAggregate agg = repository.rentToSales(period);
        boolean whole = reach.platformAdmin() || reach.allProjects();
        // M737：逐商户行只有平台管理员不过滤；其余（含勾了全部项目的人，M737-F06：治理地图「只能触及归属在项目或项目公司下……的账号与数据」）
        // 只留在可触及项目内经营的商户，店级、业态级都只用留下的行。平台合计仍按全部商户算。
        Set<String> operating = reach.platformAdmin() ? Set.of() : repository.listMerchantIdsOperatingIn(reach.organizationIds());
        java.util.List<AnalyticsRecords.RentToSalesMerchantRow> merchants = reach.platformAdmin() ? agg.merchants()
                : agg.merchants().stream().filter(m -> operating.contains(m.merchantId())).toList();
        // 店级（口径⑦ 商户即店）：每商户一行；无第三方营业额归属→比率 null 诚实标（NULL 分母只进平台级）。
        java.util.List<AnalyticsRecords.RentToSalesRow> byMerchant = merchants.stream()
                .map(m -> rentToSalesRow(m.merchantId(), m.merchantName(), m.rentCent(), m.salesCent()))
                .toList();
        // 业态级（口径⑥ 复用 merchant_type，不新增维度）：按 merchant_type 合并租金/营业额。
        Map<String, long[]> byType = new LinkedHashMap<>();
        for (AnalyticsRecords.RentToSalesMerchantRow m : merchants) {
            long[] acc = byType.computeIfAbsent(m.merchantType() == null ? "" : m.merchantType(), k -> new long[2]);
            acc[0] += m.rentCent();
            acc[1] += m.salesCent();
        }
        java.util.List<AnalyticsRecords.RentToSalesRow> byMerchantType = byType.entrySet().stream()
                .map(e -> rentToSalesRow(e.getKey(), e.getKey(), e.getValue()[0], e.getValue()[1]))
                .sorted((a, b) -> Long.compare(b.rentCent(), a.rentCent()))
                .toList();
        if (!whole) {
            // M737：平台合计与未归属营业额是全平台口径，不给没勾全部项目的人。
            return new AnalyticsRecords.RentToSalesResponse(period, byMerchant, byMerchantType, null, 0L,
                    "仅含您可查看项目内经营的商户，不含平台合计与未归属营业额");
        }
        // 平台级（口径③⑧）：分子=全部已归属租金；分母=全部已归属营业额 + 未归属营业额。
        long totalRent = agg.merchants().stream().mapToLong(AnalyticsRecords.RentToSalesMerchantRow::rentCent).sum();
        long attributedSales = agg.merchants().stream().mapToLong(AnalyticsRecords.RentToSalesMerchantRow::salesCent).sum();
        AnalyticsRecords.RentToSalesRow platform =
                rentToSalesRow("__platform__", "平台合计", totalRent, attributedSales + agg.unattributedSalesCent());
        String note = agg.unattributedSalesCent() > 0
                ? "含未归属第三方营业额（无门店号 / 未维护渠道映射）：仅计入平台级分母，不参与任何店级或业态抽成"
                : "本期第三方回流营业额均已归属到店";
        return new AnalyticsRecords.RentToSalesResponse(
                period, byMerchant, byMerchantType, platform, agg.unattributedSalesCent(), note);
    }

    /** 租售比 = 租金 / 营业额（基点，1 基点=0.01%）；营业额<=0 时比率不可计→null 并标注（诚实不伪造）。 */
    private static AnalyticsRecords.RentToSalesRow rentToSalesRow(String key, String label, long rentCent, long salesCent) {
        Long ratioBps = salesCent > 0
                ? BigDecimal.valueOf(rentCent).multiply(BigDecimal.valueOf(10000L))
                        .divide(BigDecimal.valueOf(salesCent), 0, java.math.RoundingMode.HALF_UP).longValueExact()
                : null;
        String note = salesCent > 0 ? null : "无第三方营业额归属，租售比不可计（仅计入平台级分母）";
        return new AnalyticsRecords.RentToSalesRow(key, label, rentCent, salesCent, ratioBps, note);
    }

    /** 结算B2 放弃权益额外收入（客人买票但已过期未核销的权益，其未核销部分归项目公司的额外收入）：
     *  按 票种（通票/单独门票）× 权益类型（酒水/入场/游乐/演出/其他）交叉呈现。只读、零迁移。
     *  权限 analytics.dashboard.view（复用分析只读权限，不新造权限点）；平台级跨商户聚合在控制器 requirePlatformScope。 */
    public AnalyticsRecords.AbandonedEntitlementRevenueResponse abandonedEntitlementRevenue(
            java.time.LocalDate start, java.time.LocalDate end) {
        return readSingle(
                "analytics.abandoned_entitlement_revenue.viewed",
                "analytics.abandoned_entitlement_revenue.view_failed",
                "analytics.dashboard.view",
                "settlement_transactions",
                () -> buildAbandonedEntitlementRevenue(start, end));
    }

    // 五桶固定行序：空桶=0，诚实呈现每一类权益（含无数据的类）。{机读桶键, 中文标签}
    private static final java.util.List<String[]> ABANDONED_BUCKETS = java.util.List.of(
            new String[]{"drink", "酒水"},
            new String[]{"entry", "入场"},
            new String[]{"amusement", "游乐"},
            new String[]{"show", "演出"},
            new String[]{"other", "其他"});

    private AnalyticsRecords.AbandonedEntitlementRevenueResponse buildAbandonedEntitlementRevenue(
            java.time.LocalDate start, java.time.LocalDate end) {
        Map<String, long[]> pass = new LinkedHashMap<>();    // bucket -> [amountCent, count]（通票列 bar_id IS NULL）
        Map<String, long[]> ticket = new LinkedHashMap<>();  // bucket -> [amountCent, count]（单独门票列 bar_id 非空）
        for (String[] b : ABANDONED_BUCKETS) {
            pass.put(b[0], new long[2]);
            ticket.put(b[0], new long[2]);
        }
        for (AnalyticsRecords.AbandonedEntitlementCell cell : repository.abandonedEntitlementRevenue(start, end)) {
            Map<String, long[]> col = "ticket".equals(cell.ticketType()) ? ticket : pass;
            long[] acc = col.get(cell.bucket());
            if (acc == null) {
                continue; // 防御：SQL 桶闭合于五值，未知桶不应发生，忽略不污染报表。
            }
            acc[0] += cell.amountCent();
            acc[1] += cell.count();
        }
        java.util.List<AnalyticsRecords.AbandonedEntitlementRow> rows = new java.util.ArrayList<>();
        long passAmt = 0;
        long passCnt = 0;
        long tktAmt = 0;
        long tktCnt = 0;
        for (String[] b : ABANDONED_BUCKETS) {
            long[] p = pass.get(b[0]);
            long[] t = ticket.get(b[0]);
            passAmt += p[0];
            passCnt += p[1];
            tktAmt += t[0];
            tktCnt += t[1];
            rows.add(new AnalyticsRecords.AbandonedEntitlementRow(
                    b[0], b[1],
                    new AnalyticsRecords.AbandonedEntitlementAmount(p[0], p[1]),
                    new AnalyticsRecords.AbandonedEntitlementAmount(t[0], t[1]),
                    new AnalyticsRecords.AbandonedEntitlementAmount(p[0] + t[0], p[1] + t[1])));
        }
        String note = "数据源=settlement_transactions 未核销到期归项目流水（每日 02:30 生成）；金额取权威 unverified_amount_cent；"
                + "票种按 issued_entitlements.bar_id 判定（空=通票 / 非空=单独门票）；"
                + "未做项目维度过滤（归项目流水 project_id 为 NULL，按全局+日期口径）。";
        return new AnalyticsRecords.AbandonedEntitlementRevenueResponse(
                start.toString(), end.toString(), rows,
                new AnalyticsRecords.AbandonedEntitlementAmount(passAmt, passCnt),
                new AnalyticsRecords.AbandonedEntitlementAmount(tktAmt, tktCnt),
                new AnalyticsRecords.AbandonedEntitlementAmount(passAmt + tktAmt, passCnt + tktCnt),
                note);
    }

    /** H1(2026-07-02 拍板):同名节假日今年 vs 去年——流量=区间累计+日均双列(天数不同看日均才公平,
     * 同比按日均比);存量=假期末时点(同比按值比);去年无同名区间→previous 侧 null(积累中,不伪造)。 */
    public AnalyticsRecords.HolidayComparisonResponse holidayComparison(String holidayName, int year) {
        String name = holidayName == null ? "" : holidayName.trim();
        if (name.isEmpty()) {
            throw com.complex.platform.common.api.ApiException.badRequest("VALIDATION_FAILED", "holiday is required");
        }
        return readSingle(
                "analytics.holiday_comparison.viewed",
                "analytics.holiday_comparison.view_failed",
                "analytics.dashboard.view",
                "holiday_calendar",
                () -> buildHolidayComparison(name, year));
    }

    private AnalyticsRecords.HolidayComparisonResponse buildHolidayComparison(String name, int year) {
        java.util.List<AnalyticsRecords.HolidayPeriod> all = repository.listHolidayPeriods(null);
        AnalyticsRecords.HolidayPeriod current = all.stream()
                .filter(h -> h.holidayName().equals(name) && h.holidayYear() == year)
                .findFirst()
                .orElseThrow(() -> com.complex.platform.common.api.ApiException.badRequest(
                        "VALIDATION_FAILED", "节假日期间不存在：" + name + " " + year));
        AnalyticsRecords.HolidayPeriod previous = all.stream()
                .filter(h -> h.holidayName().equals(name) && h.holidayYear() == year - 1)
                .findFirst().orElse(null);

        Map<String, BigDecimal> cur = repository.aggregateRange(current.startDate(), current.endDate(),
                DashboardAggregationJob.MONTHLY_SUM_KEYS, DashboardAggregationJob.MONTHLY_LAST_KEYS);
        Map<String, BigDecimal> prev = previous == null ? Map.of()
                : repository.aggregateRange(previous.startDate(), previous.endDate(),
                        DashboardAggregationJob.MONTHLY_SUM_KEYS, DashboardAggregationJob.MONTHLY_LAST_KEYS);
        long curDays = java.time.temporal.ChronoUnit.DAYS.between(current.startDate(), current.endDate()) + 1;
        Long prevDays = previous == null ? null
                : java.time.temporal.ChronoUnit.DAYS.between(previous.startDate(), previous.endDate()) + 1;

        java.util.List<AnalyticsRecords.HolidayComparisonRow> rows = new java.util.ArrayList<>();
        for (MonthlyMetricEntry entry : MONTHLY_COMPARISON_CATALOG) {
            boolean isSum = DashboardAggregationJob.MONTHLY_SUM_KEYS.contains(entry.key());
            BigDecimal curTotal = cur.get(entry.key());
            BigDecimal prevTotal = prev.get(entry.key());
            BigDecimal curAvg = isSum && curTotal != null
                    ? curTotal.divide(BigDecimal.valueOf(curDays), 2, java.math.RoundingMode.HALF_UP) : null;
            BigDecimal prevAvg = isSum && prevTotal != null && prevDays != null
                    ? prevTotal.divide(BigDecimal.valueOf(prevDays), 2, java.math.RoundingMode.HALF_UP) : null;
            BigDecimal yoy = isSum ? pctChange(curAvg, prevAvg) : pctChange(curTotal, prevTotal);
            rows.add(new AnalyticsRecords.HolidayComparisonRow(entry.key(), entry.label(), entry.unit(),
                    isSum ? "monthly_sum" : "period_end", curTotal, curAvg, prevTotal, prevAvg, yoy));
        }
        return new AnalyticsRecords.HolidayComparisonResponse(
                name, year, current.startDate(), current.endDate(),
                previous == null ? null : previous.holidayYear(),
                previous == null ? null : previous.startDate(),
                previous == null ? null : previous.endDate(),
                rows);
    }

    private java.time.YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return java.time.YearMonth.from(LocalDate.ofInstant(Instant.now(clock), java.time.ZoneOffset.UTC));
        }
        try {
            return java.time.YearMonth.parse(month.trim());
        } catch (java.time.format.DateTimeParseException e) {
            throw com.complex.platform.common.api.ApiException.badRequest("VALIDATION_FAILED", "月份格式不正确，应为 YYYY-MM");
        }
    }

    private AnalyticsRecords.MonthlyComparisonResponse buildMonthlyComparison(java.time.YearMonth yearMonth) {
        Map<String, BigDecimal> current = repository.readMonthlySnapshots(yearMonth.atDay(1));
        Map<String, BigDecimal> prevMonth = repository.readMonthlySnapshots(yearMonth.minusMonths(1).atDay(1));
        Map<String, BigDecimal> yoyMonth = repository.readMonthlySnapshots(yearMonth.minusYears(1).atDay(1));
        List<AnalyticsRecords.MonthlyComparisonRow> rows = new java.util.ArrayList<>();
        for (MonthlyMetricEntry entry : MONTHLY_COMPARISON_CATALOG) {
            BigDecimal cur = current.get(entry.key());
            BigDecimal prev = prevMonth.get(entry.key());
            BigDecimal yoy = yoyMonth.get(entry.key());
            String aggregation = DashboardAggregationJob.MONTHLY_SUM_KEYS.contains(entry.key())
                    ? "monthly_sum" : "period_end";
            rows.add(new AnalyticsRecords.MonthlyComparisonRow(entry.key(), entry.label(), entry.domain(),
                    entry.unit(), aggregation, cur, prev, pctChange(cur, prev), yoy, pctChange(cur, yoy)));
        }
        return new AnalyticsRecords.MonthlyComparisonResponse(yearMonth.toString(), rows);
    }

    private BigDecimal pctChange(BigDecimal current, BigDecimal base) {
        if (current == null || base == null || base.signum() == 0) {
            return null;
        }
        return current.subtract(base).divide(base.abs(), 4, java.math.RoundingMode.HALF_UP);
    }

    /** S4 钻取:超时工单明细(平台全量便捷重载=不按项目过滤)。 */
    public java.util.List<AnalyticsRecords.OverdueWorkOrderRow> listOverdueWorkOrders(String source, int limit, int offset) {
        return listOverdueWorkOrders(source, limit, offset, null);
    }

    /** S4 钻取:超时工单明细(实时,与超时率分子同过滤:时限落当日 facility 窗且超时;
     * 清单#13:source=merchant 查商户服务工单,同窗同口径)。
     * M737：projectIds 非 null 时设施工单按工单的项目列、商户工单按商户的经营项目过滤。 */
    public java.util.List<AnalyticsRecords.OverdueWorkOrderRow> listOverdueWorkOrders(String source, int limit, int offset,
            java.util.Collection<String> projectIds) {
        boolean merchant = "merchant".equals(source);
        if (!merchant && !"facility".equals(source)) {
            throw com.complex.platform.common.api.ApiException.badRequest("VALIDATION_FAILED", "来源必须为设施（facility）或商户（merchant）");
        }
        return readSingle(
                "analytics.overdue_work_orders.viewed",
                "analytics.overdue_work_orders.view_failed",
                "facility.work_order.manage",
                "facility_work_orders",
                () -> {
                    Instant now = Instant.now(clock);
                    LocalDate businessDate = businessDayResolver.businessDate("facility", now);
                    java.time.Instant windowStart = businessDayResolver.startInstant("facility", businessDate);
                    java.time.Instant windowEnd = businessDayResolver.endInstant("facility", businessDate);
                    return merchant
                            ? repository.listOverdueMerchantWorkOrders(windowStart, windowEnd, now, limit, offset, projectIds)
                            : repository.listOverdueWorkOrders(windowStart, windowEnd, now, limit, offset, projectIds);
                }
        );
    }

    /** S5 钻取:异常读数明细(平台全量便捷重载=不按项目过滤)。 */
    public java.util.List<AnalyticsRecords.AbnormalReadingRow> listAbnormalReadings(int limit, int offset) {
        return listAbnormalReadings(limit, offset, null);
    }

    /** S5 钻取:异常读数明细(实时,与 energy.abnormal_count 同过滤:读数落当日 energy 窗)。 */
    public java.util.List<AnalyticsRecords.AbnormalReadingRow> listAbnormalReadings(int limit, int offset,
            java.util.Collection<String> projectIds) {
        return readSingle(
                "analytics.abnormal_readings.viewed",
                "analytics.abnormal_readings.view_failed",
                "energy.reading.manage",
                "energy_meter_readings",
                () -> {
                    Instant now = Instant.now(clock);
                    LocalDate businessDate = businessDayResolver.businessDate("energy", now);
                    return repository.listAbnormalReadings(
                            businessDayResolver.startInstant("energy", businessDate),
                            businessDayResolver.endInstant("energy", businessDate), limit, offset, projectIds);
                }
        );
    }

    /** T12 下钻：按域返回该域指标 map，同样经 scope 隔离。 */
    public OverviewResponse dashboardDomain(String domain, Optional<String> scope) {
        String normalizedDomain = requireText(domain, "domain");
        return readSingle(
                "analytics.dashboard_domain.viewed",
                "analytics.dashboard_domain.view_failed",
                "analytics.dashboard.view",
                "analytics_dashboard_snapshot",
                () -> buildOverview(scope, normalizedDomain, null)
        );
    }

    /**
     * T4.3 分时段客流：按经营日窗口对成功核销小时分桶，恒返回 0~23 共 24 桶（无数据补 0）。
     * 日期解析与 {@link #overview(Optional, LocalDate)} 一致：指定日优先，否则退回最新 ETL 经营日。
     */
    public HourlyTrafficResponse trafficHourly(Optional<String> scope, LocalDate requestedDate) {
        return readSingle(
                "analytics.traffic_hourly.viewed",
                "analytics.traffic_hourly.view_failed",
                "analytics.dashboard.view",
                "analytics_dashboard_snapshot",
                () -> buildTrafficHourly(scope, requestedDate)
        );
    }

    private HourlyTrafficResponse buildTrafficHourly(Optional<String> scope, LocalDate requestedDate) {
        String scopeValue = scope.orElse(null);
        Instant now = Instant.now(clock);
        LocalDate businessDate = requestedDate != null ? requestedDate
                : repository.latestEtlBusinessDate(scopeValue)
                .or(() -> repository.latestEtlBusinessDate(null))
                .orElse(LocalDate.ofInstant(now, java.time.ZoneOffset.UTC));
        Map<Integer, Long> counts = repository.hourlyTrafficCounts(businessDate, scope);
        List<HourlyTrafficBucket> buckets = new java.util.ArrayList<>(24);
        for (int hour = 0; hour < 24; hour++) {
            buckets.add(new HourlyTrafficBucket(hour, counts.getOrDefault(hour, 0L)));
        }
        return new HourlyTrafficResponse(businessDate, List.copyOf(buckets));
    }

    private static final String BAR_CONFIRMED_REVENUE_NOTE =
            "酒吧部分仅含已确认商品收入与月结扣留；待收、待月结及待核退款未计入，不作为最终结算依据。";

    private static MetricValue confirmedRevenueMetric(String key, BigDecimal value, String unit, Map<String, Object> dimension) {
        String note = Set.of("revenue.bar", "revenue.bar_refund", "revenue.net", "revenue.per_sqm").contains(key)
                ? BAR_CONFIRMED_REVENUE_NOTE : null;
        return new MetricValue(value, unit, note, dimension);
    }

    private OverviewResponse buildOverview(Optional<String> scope, String domainFilter, LocalDate requestedDate) {
        String scopeValue = scope.orElse(null);
        Instant now = Instant.now(clock);
        // T4.1：指定经营日优先（前端日期筛选生效）；未指定退回最新 ETL 日。
        LocalDate businessDate = requestedDate != null ? requestedDate
                : repository.latestEtlBusinessDate(scopeValue)
                .or(() -> repository.latestEtlBusinessDate(null))
                .orElse(LocalDate.ofInstant(now, java.time.ZoneOffset.UTC));
        Map<String, BigDecimal> snapshots = repository.readMetricSnapshots(businessDate, scopeValue);

        Map<String, MetricValue> metrics = new LinkedHashMap<>();
        for (MetricCatalogEntry entry : METRIC_CATALOG) {
            if (domainFilter != null && !domainFilter.equals(entry.domain())) {
                continue;
            }
            if (!entry.ready()) {
                metrics.put(entry.metricCode(), MetricValue.unavailable(entry.note()));
                continue;
            }
            BigDecimal value = snapshots.get(entry.metricCode());
            if (value == null) {
                // 商户口径下源表无 merchant_id 维度的指标无快照行 → 不伪造，返回 null + note。
                metrics.put(entry.metricCode(), MetricValue.unavailable(
                        scopeValue == null ? "no snapshot for business date" : entry.scopeNote()));
            } else {
                metrics.put(entry.metricCode(), confirmedRevenueMetric(entry.metricCode(), value, entry.unit(), null));
            }
        }
        return new OverviewResponse(now, scopeValue, businessDate, metrics);
    }

    /**
     * P2 集团/跨项目汇总（R6-D13）：按 project 维 rollup 关键指标（收入/客流/核销/风险/商户数）。
     * 当前为单综合体——平台口径 ETL 快照即该 project 的跨商户汇总，故返回单个 project rollup（不伪造多 project）。
     * 真实聚合：复用 readMetricSnapshots(date, null) 平台快照 + countMerchantsWithEtlSnapshot 真实商户数；
     * 无 ETL 快照时 metrics 诚实置 null + note，merchantCount=0。
     */
    public GroupOverviewResponse groupOverview() {
        return groupOverview(null);
    }

    /** 平台全量便捷重载（按平台管理员的可触及范围），只给现有单测用。控制器走带 reach 的版本。 */
    public GroupOverviewResponse groupOverview(String groupBy) {
        return groupOverview(groupBy, PLATFORM_ADMIN_REACH);
    }

    /**
     * M737（治理地图「集团下钻细则（已确认）」：「集团视图的跨综合体查看按是否持有可跨项目角色判定（见「权限规则」）」；
     * 「KPI 可见性分层」：「全平台合计只给平台管理员和勾选了「可以查看全部项目」的人看；只逐个勾选了项目的人，
     * 只能分别查看被选中项目各自的数据。」）：reach 由控制器按 analytics.group.view 算好——平台管理员与勾了全部项目的人
     * 看全部项目行与平台口径回退行；其余只出可触及项目的行，没有可见行时 projects 为空、不出回退行。
     */
    public GroupOverviewResponse groupOverview(
            String groupBy, com.complex.platform.access.AuthorizationService.ProjectReach reach) {
        String normalizedGroupBy = normalizeGroupBy(groupBy);
        return readSingle(
                "analytics.group_overview.viewed",
                "analytics.group_overview.view_failed",
                "analytics.group.view",
                "analytics_business_metric_snapshot",
                () -> buildGroupOverview(normalizedGroupBy, reach)
        );
    }

    private GroupOverviewResponse buildGroupOverview(
            String groupBy, com.complex.platform.access.AuthorizationService.ProjectReach reach) {
        Instant now = Instant.now(clock);
        Optional<LocalDate> etlDate = repository.latestEtlBusinessDate(null);
        LocalDate businessDate = etlDate.orElse(LocalDate.ofInstant(now, java.time.ZoneOffset.UTC));
        boolean byEntity = "entity".equals(groupBy);
        boolean whole = reach.platformAdmin() || reach.allProjects();

        // ⑤ 集团多项目 ETL：优先读 segment_type='project' 维度，按 project_id 产出 per-project rollup（多综合体→多行）。
        // 无 project 口径快照时回退到平台口径单行（向后兼容旧 ETL 与"无快照诚实空态"），不伪造多 project。
        // M737：非全平台口径的人只留可触及项目的行，后面的合并、平均、低于基准标记都只按留下的行算。
        List<String> projectIds = etlDate.isPresent()
                ? repository.listProjectIdsWithEtlSnapshot(businessDate).stream()
                        .filter(projectId -> whole || reach.covers(projectId)).toList()
                : List.of();

        LinkedHashMap<String, RawGroupUnit> units = new LinkedHashMap<>();
        if (!projectIds.isEmpty()) {
            Map<String, String> projectToEntity = byEntity ? repository.projectBusinessEntityMap() : Map.of();
            for (String projectId : projectIds) {
                Map<String, BigDecimal> snapshots = repository.readProjectMetricSnapshots(businessDate, projectId);
                long merchantCount = repository.countMerchantsWithEtlSnapshotByProject(businessDate, projectId);
                String unitId = byEntity ? entityId(projectToEntity.get(projectId)) : projectId;
                RawGroupUnit unit = units.computeIfAbsent(
                        unitId,
                        ignored -> new RawGroupUnit(byEntity ? unitId : projectDisplayName(projectId)));
                unit.merchantCount += merchantCount;
                snapshots.forEach((metric, value) -> unit.snapshots.merge(metric, value, BigDecimal::add));
            }
        }

        if (units.isEmpty()) {
            if (!whole) {
                // M737：回退行是全平台合计，不给没勾全部项目的人。
                return new GroupOverviewResponse(now, businessDate, groupBy, Map.of(), List.of());
            }
            // 回退：平台口径单 project rollup（单综合体合成 project 标识，口径与现状一致）。
            Map<String, BigDecimal> snapshots = etlDate.isPresent()
                    ? repository.readMetricSnapshots(businessDate, null)
                    : Map.of();
            Map<String, MetricValue> metrics = buildGroupMetrics(snapshots, etlDate.isPresent());
            long merchantCount = etlDate.isPresent() ? repository.countMerchantsWithEtlSnapshot(businessDate) : 0L;
            GroupProjectRollup rollup = new GroupProjectRollup(
                    GROUP_PROJECT_ID, GROUP_PROJECT_NAME, merchantCount, metrics, List.of());
            return new GroupOverviewResponse(now, businessDate, groupBy, Map.of(), List.of(rollup));
        }

        Map<String, BigDecimal> averages = groupAverages(units);
        List<GroupProjectRollup> rollups = new java.util.ArrayList<>();
        for (Map.Entry<String, RawGroupUnit> item : units.entrySet()) {
            RawGroupUnit unit = item.getValue();
            Set<String> below = belowBenchmarkMetrics(unit.snapshots, averages);
            rollups.add(new GroupProjectRollup(
                    item.getKey(),
                    unit.displayName,
                    unit.merchantCount,
                    buildGroupMetrics(unit.snapshots, true, below),
                    List.copyOf(below)));
        }
        return new GroupOverviewResponse(
                now,
                businessDate,
                groupBy,
                buildGroupAverageMetrics(averages),
                List.copyOf(rollups));
    }

    /** 按集团指标目录从快照构造 metrics；无快照按"不伪造"返回 null + note。 */
    private Map<String, MetricValue> buildGroupMetrics(Map<String, BigDecimal> snapshots, boolean hasEtl) {
        return buildGroupMetrics(snapshots, hasEtl, Set.of());
    }

    private Map<String, MetricValue> buildGroupMetrics(
            Map<String, BigDecimal> snapshots,
            boolean hasEtl,
            Set<String> belowBenchmarkMetrics) {
        Map<String, MetricValue> metrics = new LinkedHashMap<>();
        for (GroupMetricEntry entry : GROUP_METRIC_CATALOG) {
            BigDecimal value = snapshots.get(entry.metricCode());
            if (value == null) {
                metrics.put(entry.metricCode(), MetricValue.unavailable(
                        hasEtl
                                ? "no etl snapshot for metric on business date"
                                : "no etl snapshot available yet"));
            } else {
                metrics.put(entry.metricCode(), belowBenchmarkMetrics.contains(entry.metricCode())
                        ? confirmedRevenueMetric(entry.metricCode(), value, entry.unit(), Map.of("below_benchmark", true))
                        : confirmedRevenueMetric(entry.metricCode(), value, entry.unit(), null));
            }
        }
        return metrics;
    }

    private Map<String, BigDecimal> groupAverages(Map<String, RawGroupUnit> units) {
        Map<String, BigDecimal> averages = new LinkedHashMap<>();
        for (GroupMetricEntry entry : GROUP_METRIC_CATALOG) {
            List<BigDecimal> values = units.values().stream()
                    .map(unit -> unit.snapshots.get(entry.metricCode()))
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (!values.isEmpty()) {
                BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                averages.put(entry.metricCode(), sum.divide(
                        BigDecimal.valueOf(values.size()), 4, java.math.RoundingMode.HALF_UP));
            }
        }
        return averages;
    }

    private Set<String> belowBenchmarkMetrics(Map<String, BigDecimal> snapshots, Map<String, BigDecimal> averages) {
        java.util.LinkedHashSet<String> below = new java.util.LinkedHashSet<>();
        BigDecimal floorMultiplier = BigDecimal.ONE.subtract(benchmarkDeviation());
        for (GroupMetricEntry entry : GROUP_METRIC_CATALOG) {
            BigDecimal value = snapshots.get(entry.metricCode());
            BigDecimal average = averages.get(entry.metricCode());
            if (value != null && average != null && average.signum() > 0
                    && value.compareTo(average.multiply(floorMultiplier)) < 0) {
                below.add(entry.metricCode());
            }
        }
        return below;
    }

    private Map<String, MetricValue> buildGroupAverageMetrics(Map<String, BigDecimal> averages) {
        Map<String, MetricValue> metrics = new LinkedHashMap<>();
        for (GroupMetricEntry entry : GROUP_METRIC_CATALOG) {
            BigDecimal average = averages.get(entry.metricCode());
            if (average != null) {
                metrics.put(entry.metricCode(), confirmedRevenueMetric(entry.metricCode(), average, entry.unit(), null));
            }
        }
        return metrics;
    }

    private String normalizeGroupBy(String groupBy) {
        String value = groupBy == null || groupBy.isBlank()
                ? "project"
                : groupBy.trim().toLowerCase(Locale.ROOT);
        if (!"project".equals(value) && !"entity".equals(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "分组维度必须为项目（project）或主体（entity）");
        }
        return value;
    }

    private String entityId(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "entity_unassigned" : trimmed;
    }

    /** 单综合体默认 project 用既有展示名；其余 project_id 暂以 id 作展示名（多综合体接入主数据后替换）。 */
    private String projectDisplayName(String projectId) {
        return GROUP_PROJECT_ID.equals(projectId) ? GROUP_PROJECT_NAME : projectId;
    }

    private static final class RawGroupUnit {
        private final String displayName;
        private long merchantCount;
        private final Map<String, BigDecimal> snapshots = new LinkedHashMap<>();

        private RawGroupUnit(String displayName) {
            this.displayName = displayName;
        }
    }

    /** 平台全量便捷重载（按平台管理员的可触及范围），只给现有单测用。控制器走带 reach 的版本。 */
    public PageResponse<BusinessMetricSnapshotRecord> listBusinessMetricSnapshots() {
        return listBusinessMetricSnapshots(PLATFORM_ADMIN_REACH);
    }

    /**
     * M737（治理地图「KPI 可见性分层」：「全平台合计只给平台管理员和勾选了「可以查看全部项目」的人看；只逐个勾选了项目的人，
     * 只能分别查看被选中项目各自的数据。」）：reach 由控制器按 analytics.dashboard.view 算好——平台管理员看全表；其余按段类型逐行留：
     * 平台段只给勾了全部项目的人，项目段按可触及项目，商户段按在可触及项目经营的商户，别的段不出。
     */
    public PageResponse<BusinessMetricSnapshotRecord> listBusinessMetricSnapshots(
            com.complex.platform.access.AuthorizationService.ProjectReach reach) {
        return readPage(
                "analytics.business_metric_snapshot.viewed",
                "analytics.business_metric_snapshot.view_failed",
                "analytics.dashboard.view",
                "analytics_business_metric_snapshot",
                () -> {
                    List<BusinessMetricSnapshotRecord> snapshots = repository.listBusinessMetricSnapshots();
                    if (reach.platformAdmin()) {
                        return snapshots;
                    }
                    Set<String> merchantIds = repository.listMerchantIdsOperatingIn(reach.organizationIds());
                    // ponytail: 全表取出后在内存里筛（本来就不分页）；表大了改成按段类型下推到 SQL。
                    return snapshots.stream().filter(snapshot -> {
                        String segmentType = snapshot.segmentType();
                        if (DashboardAggregationJob.SEGMENT_PLATFORM.equals(segmentType)) {
                            return reach.allProjects();
                        }
                        if (DashboardAggregationJob.SEGMENT_PROJECT.equals(segmentType)) {
                            return reach.allProjects() || reach.covers(snapshot.segmentId());
                        }
                        return DashboardAggregationJob.SEGMENT_MERCHANT.equals(segmentType)
                                && snapshot.segmentId() != null && merchantIds.contains(snapshot.segmentId());
                    }).toList();
                }
        );
    }

    public PageResponse<MerchantHealthScoreRecord> listMerchantHealthScores(Optional<String> scope) {
        // 行级隔离：merchant-scoped 调用方仅见自身健康分；平台口径（scope 空）见全量。
        return readPage(
                "analytics.merchant_health.viewed",
                "analytics.merchant_health.viewed_failed",
                "analytics.dashboard.view",
                "analytics_merchant_health_score",
                () -> scope.map(repository::listMerchantHealthScoresByMerchant)
                        .orElseGet(repository::listMerchantHealthScores)
        );
    }

    public PageResponse<MerchantSummaryRecord> listAnalyticsMerchants(Optional<String> scope) {
        // 行级隔离：merchant-scoped 调用方仅见自身经营摘要；平台口径见全量。
        return readPage(
                "analytics.merchant_summary.viewed",
                "analytics.merchant_summary.viewed_failed",
                "analytics.dashboard.view",
                "analytics_merchant_summary",
                () -> scope.map(repository::listAnalyticsMerchantsByMerchant)
                        .orElseGet(repository::listAnalyticsMerchants)
        );
    }

    /**
     * M737（业务地图「谁会看」：「项目级操作人员只看自己所在项目范围内的 KPI 和明细，看不到别的项目。」）：
     * 不带商户归属的员工按可触及项目看商户健康分——只出在这些项目经营的商户。
     */
    public PageResponse<MerchantHealthScoreRecord> listMerchantHealthScoresInProjects(java.util.Collection<String> projectIds) {
        return readPage(
                "analytics.merchant_health.viewed",
                "analytics.merchant_health.viewed_failed",
                "analytics.dashboard.view",
                "analytics_merchant_health_score",
                () -> {
                    Set<String> merchantIds = repository.listMerchantIdsOperatingIn(projectIds);
                    // ponytail: 全量取出后在内存里按商户筛；商户多到成为负担时改成 SQL 里按经营商户子查询下推。
                    return repository.listMerchantHealthScores().stream()
                            .filter(score -> merchantIds.contains(score.merchantId())).toList();
                }
        );
    }

    /** M737：同上，商户经营摘要。 */
    public PageResponse<MerchantSummaryRecord> listAnalyticsMerchantsInProjects(java.util.Collection<String> projectIds) {
        return readPage(
                "analytics.merchant_summary.viewed",
                "analytics.merchant_summary.viewed_failed",
                "analytics.dashboard.view",
                "analytics_merchant_summary",
                () -> {
                    Set<String> merchantIds = repository.listMerchantIdsOperatingIn(projectIds);
                    // ponytail: 同上，全量取出后在内存里筛。
                    return repository.listAnalyticsMerchants().stream()
                            .filter(summary -> merchantIds.contains(summary.merchantId())).toList();
                }
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

    /**
     * 指标异常处置状态机(纯函数,便于单测):校验 action 合法、当前态可迁移,返回目标状态。
     * open(未处置) -> acknowledged/resolved/false_positive/escalated;
     * acknowledged(已阅) -> resolved/false_positive/escalated;
     * escalated(已转告警) -> resolved/false_positive;
     * resolved/false_positive 为终态,重复处置抛 409。
     */
    static String anomalyDispositionTarget(String currentStatus, String action) {
        if (!ANOMALY_DISPOSITION_ACTIONS.contains(action)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "action is invalid");
        }
        if (ANOMALY_TERMINAL_STATUSES.contains(currentStatus)) {
            throw ApiException.conflict("METRIC_ANOMALY_ALREADY_DISPOSED", "该指标异常已处置完毕");
        }
        return switch (action) {
            case "acknowledge" -> {
                if (!"open".equals(currentStatus)) {
                    throw ApiException.conflict("METRIC_ANOMALY_INVALID_TRANSITION",
                            "只有待处理的指标异常才能确认");
                }
                yield "acknowledged";
            }
            case "escalate" -> {
                if (!"open".equals(currentStatus) && !"acknowledged".equals(currentStatus)) {
                    throw ApiException.conflict("METRIC_ANOMALY_INVALID_TRANSITION",
                            "指标异常当前状态不可升级：" + currentStatus);
                }
                yield "escalated";
            }
            case "resolve" -> "resolved";
            case "false_positive" -> "false_positive";
            default -> throw ApiException.badRequest("VALIDATION_FAILED", "action is invalid");
        };
    }

    @Transactional
    public MetricAnomalyRecord disposeMetricAnomaly(String id, AnomalyDispositionRequest request) {
        String anomalyId = requireText(id, "id");
        return executeWrite(
                "analytics.dispose-metric-anomaly",
                request == null ? null : request.idempotencyKey(),
                request,
                MetricAnomalyRecord.class,
                "analytics.metric_anomaly.disposed",
                "analytics.metric_anomaly.disposed_failed",
                "analytics.metric_anomaly.manage",
                "analytics_metric_anomaly",
                () -> disposeMetricAnomalyInternal(anomalyId, request)
        );
    }

    /**
     * 异常处置历史时间线(append-only):按发生时间升序返回该异常的每次处置动作(who/when/from->to/说明/生成的告警号),
     * 历史不可改(读 audit_events 的 disposition_recorded 事件)。供闭环回溯"这条异常经历了什么、谁处理的、去哪了"。
     */
    public List<AuditEvent> listMetricAnomalyTimeline(String id) {
        String anomalyId = requireText(id, "id");
        return operations.readSingle(
                AuditedOperation.of(
                        "analytics.metric_anomaly.timeline_viewed",
                        "analytics.metric_anomaly.timeline_viewed_failed",
                        "analytics.risk_alert.view",
                        "analytics_metric_anomaly"),
                () -> operations.readObjectTimeline(
                        "analytics_metric_anomaly", anomalyId, "analytics.metric_anomaly.disposition_recorded"),
                result -> anomalyId);
    }

    private MetricAnomalyRecord disposeMetricAnomalyInternal(String anomalyId, AnomalyDispositionRequest request) {
        AnomalyDispositionRequest actual = requireRequest(request);
        String action = requireText(actual.action(), "action");
        // 行锁读:并发处置串行化,第二个处置在第一个提交后读到新状态,升级/终态守卫在建告警前拦截,杜绝孤儿告警。
        MetricAnomalyRecord current = repository.findMetricAnomalyByIdForUpdate(anomalyId)
                .orElseThrow(() -> ApiException.notFound("METRIC_ANOMALY_NOT_FOUND", "指标异常不存在或已被删除"));
        String targetStatus = anomalyDispositionTarget(current.status(), action);
        // acknowledge 说明可选;其余处置动作必须留说明(审计留痕/升级依据)。
        String note = "acknowledge".equals(action)
                ? optionalLimitedText(actual.note(), "note", 1000)
                : requireLimitedText(actual.note(), "note", 1000);
        Instant now = Instant.now(clock);
        String escalatedAlertId = current.escalatedAlertId();
        Instant resolvedAt = current.resolvedAt();
        if ("escalate".equals(action)) {
            // 转告警:在分析域风险告警工作台落一条可跟进的告警,回挂其 id 形成闭环(object=该异常)。
            RiskAlertRecord alert = createRiskAlertInternal(new RiskAlertRequest(
                    null, note, List.of(), null, "metric_anomaly", current.riskLevel(),
                    "metric_anomaly", current.id(), "指标异常升级:" + current.summary()));
            escalatedAlertId = alert.id();
        }
        if ("resolved".equals(targetStatus) || "false_positive".equals(targetStatus)) {
            resolvedAt = now;
        }
        MetricAnomalyRecord updated = repository.updateMetricAnomalyDisposition(
                anomalyId, targetStatus, note, reviewLockActorId(), now, escalatedAlertId, resolvedAt);
        // 处置历史留痕:每个动作追加一条不可改的时间线记录(who/when 由审计上下文自动填充,from->to/说明/生成的告警号),
        // 供 /timeline 回溯完整处置历史。复用 audit_events(只增不改),与本次更新同事务原子落库;幂等重放不重复追加。
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("action", action);
        detail.put("from_status", current.status());
        detail.put("to_status", targetStatus);
        if (note != null) {
            detail.put("note", note);
        }
        if ("escalate".equals(action) && escalatedAlertId != null) {
            detail.put("escalated_alert_id", escalatedAlertId);
        }
        operations.recordDetail("analytics.metric_anomaly.disposition_recorded",
                "analytics.metric_anomaly.manage", "analytics_metric_anomaly", anomalyId, detail);
        return updated;
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

    private static final BigDecimal DEFAULT_LLM_MIN_CONFIDENCE = new BigDecimal("0.60");
    private static final BigDecimal DEFAULT_BENCHMARK_DEVIATION = new BigDecimal("0.20");

    /** V218:AI建议采纳最低置信度(配置读,缺配置兜底默认,行为不变)。 */
    private BigDecimal localLlmMinConfidence() {
        AnalyticsRecords.AiThresholds t = repository.readAiThresholds();
        return t == null || t.localLlmMinConfidence() == null ? DEFAULT_LLM_MIN_CONFIDENCE : t.localLlmMinConfidence();
    }

    /** V218:对标偏离告警线(配置读,缺配置兜底默认,行为不变)。 */
    private BigDecimal benchmarkDeviation() {
        AnalyticsRecords.AiThresholds t = repository.readAiThresholds();
        return t == null || t.benchmarkDeviation() == null ? DEFAULT_BENCHMARK_DEVIATION : t.benchmarkDeviation();
    }

    private AiQualitySuggestionResponse generateLocalLlmAiQualitySuggestionInternal(AiQualitySuggestionRequest request) {
        AiQualitySuggestionRequest actual = requireRequest(request);
        String sourceType = requireLimitedText(actual.sourceType(), "source_type", 64);
        String sourceId = requireLimitedText(actual.sourceId(), "source_id", 64);
        String requestedBy = optionalLimitedText(actual.requestedBy(), "requested_by", 64);
        String sourceText = requireLimitedText(actual.sourceText(), "source_text", MAX_SOURCE_TEXT_CHARS);
        PiiTextRedactor.Result redacted = PiiTextRedactor.redactAndTruncate(sourceText, MAX_LLM_INPUT_CHARS);
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
                || confidenceScore.compareTo(localLlmMinConfidence()) < 0
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

    public PageResponse<AnalyticsRecords.BusinessReviewLockRecord> listReviewLocks() {
        return readPage(
                "analytics.review_lock.viewed",
                "analytics.review_lock.viewed_failed",
                "analytics.review.lock",
                "analytics_business_review_lock",
                () -> {
                    var reach = reviewLockReach();
                    if (reach.platformAdmin()) {
                        return repository.listReviewLocks(null);
                    }
                    Optional<String> ownProject = reach.onlyOwnProject();
                    if (ownProject.isPresent()) {
                        return repository.listReviewLocks(ownProject.get());
                    }
                    // M737：持有可跨项目角色的人看可触及项目的锁；项目为空的锁不出。
                    // ponytail: 全量取出后在内存里筛；锁多到成为负担时改成 SQL 里 project_id IN (...) 下推。
                    return repository.listReviewLocks(null).stream()
                            .filter(lock -> reach.covers(lock.projectId())).toList();
                }
        );
    }

    @Transactional
    public AnalyticsRecords.BusinessReviewLockRecord createReviewLock(BusinessReviewLockRequest request) {
        // M737：可触及范围在进入写操作之前算好。M737-F08：本人所属项目不在可触及范围内的，在进入幂等与写操作之前就拒绝——
        // 用原提交编号重试（角色的可查看项目已减掉本人项目且审批生效）也不返回第一次的结果、不建锁；仍可触及的照旧返回第一次的结果。
        var reach = reviewLockReach();
        if (!reach.platformAdmin() && !reach.covers(reach.ownProject())) {
            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "本人所属项目不在该权限可查看的项目内，不能锁定经营复盘");
        }
        return executeWrite(
                "analytics.lock-business-review",
                request == null ? null : request.idempotencyKey(),
                request,
                AnalyticsRecords.BusinessReviewLockRecord.class,
                "analytics.review.locked",
                "analytics.review.lock_failed",
                "analytics.review.lock",
                "analytics_business_review_lock",
                () -> createReviewLockInternal(request, reach)
        );
    }

    private AnalyticsRecords.BusinessReviewLockRecord createReviewLockInternal(
            BusinessReviewLockRequest request, com.complex.platform.access.AuthorizationService.ProjectReach reach) {
        BusinessReviewLockRequest actual = requireRequest(request);
        String domain = requireText(actual.domain(), "domain");
        String operatorNote = requireText(actual.operatorNote(), "operator_note");
        int metricCount = actual.metricCount() == null ? 0 : Math.max(0, actual.metricCount());
        // project_id 与 locked_by 由 token 派生，绝不取请求体（AC-ISO-2）。锁仍建在本人项目（平台管理员落 NULL）；
        // 本人项目可触及与否已在 createReviewLock 进入写操作之前判过（M737-F08）。
        String projectScope = reach.platformAdmin() ? null : reach.ownProject();
        String lockedBy = reviewLockActorId();
        return repository.saveReviewLock(new AnalyticsRecords.BusinessReviewLockRecord(
                UUID.randomUUID().toString(),
                projectScope,
                domain,
                operatorNote,
                metricCount,
                lockedBy,
                Instant.now(clock),
                actual.idempotencyKey()
        ));
    }

    /**
     * token 派生可触及项目（M737：按 analytics.review.lock 算，持有可跨项目角色的人看得到角色可查看项目的锁）：
     * 平台管理员见全量并落 NULL；其余按可触及项目看，锁建在本人项目（V214 范式）。未注入判定服务（现有单测）按平台管理员兜底。
     */
    private com.complex.platform.access.AuthorizationService.ProjectReach reviewLockReach() {
        return authorizationService == null
                ? PLATFORM_ADMIN_REACH
                : authorizationService.projectReach(authorizationService.currentActor(), "analytics.review.lock");
    }

    private String reviewLockActorId() {
        if (authorizationService == null) {
            return "system";
        }
        var actor = authorizationService.currentActor();
        return actor == null || actor.id() == null ? "system" : actor.id();
    }

    private RiskAlertRecord createRiskAlertInternal(RiskAlertRequest request) {
        RiskAlertRequest actual = requireRequest(request);
        String riskLevel = requireRiskLevel(actual.riskLevel());
        Instant now = Instant.now(clock);
        RiskAlertRecord saved = repository.saveRiskAlert(new RiskAlertRecord(
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
        // P1：落孤岛表 analytics_risk_alerts（权威）后，双写统一 risk_alerts（汇聚镜像，fail-safe）。
        reportRiskAlertToUnified(saved);
        return saved;
    }

    /**
     * P1：analytics 风险预警双写统一异常责任链 risk_alerts（evidence/RiskAlertService.report）。
     *
     * <p>source_domain='analytics'，alertType 沿用本域风险类型，risk_level 经映射（本域枚举与统一枚举同名直通，
     * 未知值兜底 suspicious），source_ref=该 risk alert 的 id，幂等键稳定唯一（id + ":unified"），重复不重复上报。
     *
     * <p>fail-safe：未注入服务（旧构造器/现有单测）或上报异常都不阻塞创建主链——
     * 孤岛 analytics_risk_alerts 仍是命中落地的权威，统一表为汇聚镜像。
     */
    private void reportRiskAlertToUnified(RiskAlertRecord alert) {
        if (riskAlertService == null) {
            return;
        }
        // 未知风险级映射不到统一枚举则按 suspicious 兜底（避免 report 抛 VALIDATION_FAILED 静默丢数据）。
        String unifiedLevel = RISK_LEVEL_TO_UNIFIED.getOrDefault(alert.riskLevel(), "suspicious");
        try {
            riskAlertService.report(new RiskAlertReportRequest(
                    alert.id() + ":unified",
                    "analytics",
                    alert.id(),
                    alert.alertType(),
                    unifiedLevel,
                    "Analytics risk: " + alert.alertType() + " on " + alert.objectType(),
                    null,
                    "alert_type=" + alert.alertType() + "; object_type=" + alert.objectType()
                            + "; object_id=" + alert.objectId() + "; rule_id=" + alert.ruleId(),
                    List.of()));
        } catch (RuntimeException exception) {
            // 统一表上报失败不影响创建主链（命中已落孤岛 analytics_risk_alerts）。fail-safe 静默吞，不阻塞。
        }
    }

    private RiskAlertRecord resolveRiskAlertInternal(String alertId, StatusChangeRequest request) {
        StatusChangeRequest actual = requireRequest(request);
        String targetStatus = requireRiskAlertResolveAction(actual.action());
        String reason = requireText(actual.reason(), "reason");
        RiskAlertRecord current = repository.findRiskAlertById(alertId)
                .orElseThrow(() -> ApiException.notFound("RISK_ALERT_NOT_FOUND", "风险预警不存在或已被删除"));
        if (TERMINAL_ALERT_STATUSES.contains(current.status())) {
            throw ApiException.conflict("RISK_ALERT_ALREADY_RESOLVED", "该风险预警已处理完毕");
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
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
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
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 长度超出限制");
        }
        return text;
    }

    private String optionalLimitedText(String value, String fieldName, int maxLength) {
        String text = trimToNull(value);
        if (text != null && text.length() > maxLength) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 长度超出限制");
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

    private String businessId(Object value) {
        if (value instanceof RiskAlertRecord record) {
            return record.id();
        }
        if (value instanceof DashboardSnapshotRecord record) {
            return record.snapshotKey();
        }
        if (value instanceof OverviewResponse response) {
            return response.scope() == null ? "platform" : response.scope();
        }
        if (value instanceof GroupOverviewResponse) {
            return "group";
        }
        if (value instanceof AiQualitySuggestionResponse response) {
            return response.task().taskId();
        }
        return null;
    }


    record MetricCatalogEntry(
            String metricCode,
            String domain,
            String unit,
            boolean ready,
            String scopeNote
    ) {
        String note() {
            return scopeNote;
        }
    }

    /** P2 集团总览汇总指标条目（metric_code + 单位），来源为平台口径 ETL 快照。 */
    private record GroupMetricEntry(String metricCode, String unit) {
    }

    private record NormalizedRiskLevel(String value, boolean adjusted) {
    }

    private record NormalizedText(String value, boolean adjusted) {
    }

    // ==== KPI 统一明细（指标登记簿，2026-07-18 方案A）====

    /**
     * KPI 统一明细：按登记项返回分页明细 + 合计校验值 + 口径说明 + 数据截至时间。
     * 口径铁律：可加=区间求和；比率=分子/分母（part 两页签）；余额=区间末值（取 to，from 不参与）。
     * 权限：控制器已做「契约宽权限 + 登记项权限」双重 fail-closed 校验；审计事件按登记项权限落账。
     */
    /** 平台口径（null 项目作用域=平台管理员看全量）便捷重载。项目级调用须走带 projectScope 的 7 参版本。 */
    public AnalyticsRecords.MetricDetailResponse metricDetail(
            String metricKey, String part, LocalDate from, LocalDate to, int page, int size) {
        return metricDetail(metricKey, part, from, to, page, size, null);
    }

    /**
     * KPI 统一明细（项目作用域收紧，2026-07-19 用户拍板）。
     * projectScope：调用方可查看的项目（M737：controller 按两个权限码可触及项目的交集派生，绝不取请求体）；
     *   null=看全量（平台管理员；勾了全部项目的人看没有项目列的指标）；非 null=按登记项 scope 列过滤只见集合内的项目（空集合一行不出）。
     * platform_only 指标（登记簿未声明 scope 列）遇非 null projectScope → fail-closed 403（服务层纵深防御）。
     */
    public AnalyticsRecords.MetricDetailResponse metricDetail(
            String metricKey, String part, LocalDate from, LocalDate to, int page, int size,
            java.util.Collection<String> projectScope) {
        MetricRegistry.MetricDefinition definition = MetricRegistry.require(metricKey);
        return readSingle(
                "analytics.metric_detail.viewed",
                "analytics.metric_detail.view_failed",
                definition.permission(),
                "analytics_metric_detail",
                () -> buildMetricDetail(definition, part, from, to, page, size, projectScope));
    }

    private AnalyticsRecords.MetricDetailResponse buildMetricDetail(
            MetricRegistry.MetricDefinition definition, String part, LocalDate from, LocalDate to, int page, int size,
            java.util.Collection<String> projectScope) {
        // 项目作用域收紧（fail-closed 纵深防御）：平台口径指标（无综合体维度）拒绝项目级下钻——
        // 只有平台管理者能看平台口径聚合；项目级操作者只能看本项目内容（controller 已同判，此处防绕过服务层直调）。
        if (projectScope != null && !MetricRegistry.isProjectScoped(definition)) {
            throw ApiException.forbidden("PLATFORM_METRIC_PROJECT_FORBIDDEN",
                    "该指标为平台口径，项目级不可下钻（仅平台管理者可见平台聚合）");
        }
        // 平台口径指标（非项目作用域）不注入项目谓词：传 null 给带 scope 的仓储方法保持平台全量。
        java.util.Collection<String> scope = MetricRegistry.isProjectScoped(definition) ? projectScope : null;
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "起始日期不能晚于结束日期");
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 200);
        int offset = (safePage - 1) * safeSize;
        boolean isRatio = MetricRegistry.TYPE_RATIO.equals(definition.valueType());
        String activePart = null;
        if (isRatio) {
            activePart = part == null || part.isBlank() ? "numerator" : part.trim();
            if (!"numerator".equals(activePart) && !"denominator".equals(activePart)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "part 仅支持 numerator/denominator（比率类两页签）");
            }
        } else if (part != null && !part.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "part 仅比率类指标可用（分子/分母两页签）");
        }

        Instant now = Instant.now(clock);
        if ("amusement".equals(definition.windowDomain())) {
            AnalyticsRecords.MetricDetailRows detail=repository.amusementComplexDetail(definition.metricKey(),from,to,now,
                    definition.rangeFilterOptional() && !MetricRegistry.TYPE_BALANCE.equals(definition.valueType()),safeSize,offset,scope);
            String note="amusement.session_capacity".equals(definition.metricKey())
                    ? "项目硬安全容量按当前唯一游乐项目合计，日期不改变当前配置值"
                    : "日期按各综合体所在地已批准的游乐营业日版本分桶；已锁定历史记录保留原归属";
            return assembleMetricDetail(definition,null,from,to,now,detail.summaryValue(),null,null,
                    appendProjectScopeNote(note,scope),definition.columns(),detail,safePage,safeSize,true);
        }
        LocalDate currentBusinessDay = businessDayResolver.businessDate(definition.windowDomain(), now);

        if (MetricRegistry.TYPE_BALANCE.equals(definition.valueType())) {
            // 余额类=区间末值：as-of=to（缺省=当前经营日），from 不参与。
            LocalDate asOf = to != null ? to : currentBusinessDay;
            AnalyticsRecords.MetricDetailRows detail = switch (definition.metricKey()) {
                case "finance.ar_balance" -> repository.arOpenReceivableDetail(asOf, safeSize, offset, scope);
                // 第四批（财务域）余额类：应付余额=应收镜像；坏账预警区=应收逾期90天以上（同 as-of 口径）。
                case "finance.ap_balance" -> repository.apOpenPayableDetail(asOf, safeSize, offset, scope,
                        financePayroll == null ? Set.of() : financePayroll.visiblePayableIds(com.complex.platform.finance.FinancePayrollService.VIEW));
                case "finance.ar_balance_aging90p" -> repository.arOverdueAging90pDetail(asOf, safeSize, offset, scope);
                // 第二批（酒吧域）余额类：状态/剩余数量按台账当前值，截至日仅限制下单/存入时间早于该经营日窗末。
                case "bar.open_order_count" -> repository.barOpenOrderDetail(
                        businessDayResolver.endInstant(definition.windowDomain(), asOf), safeSize, offset);
                case "bar.saved_bottle_stored" -> repository.savedBottleStoredDetail(
                        businessDayResolver.endInstant(definition.windowDomain(), asOf), safeSize, offset);
                case "bar.saved_bottle_quantity" -> repository.savedBottleQuantityDetail(
                        businessDayResolver.endInstant(definition.windowDomain(), asOf), safeSize, offset);
                // 第三批（票务域）余额类：状态按台账当前值，截至日仅限制下单/创建时间早于该经营日窗末。
                case "ticket.pending_issue_count" -> repository.ticketPendingIssueDetail(
                        businessDayResolver.endInstant(definition.windowDomain(), asOf), safeSize, offset);
                case "amusement.inspection_pending_rectification" -> repository.amusementInspectionPendingRectificationDetail(
                        businessDayResolver.endInstant(definition.windowDomain(), asOf), safeSize, offset);
                // 第五批（人事域）余额类：待审请假/在职员工数按台账当前值，截至日仅限制提交/建号时间早于该自然日窗末。
                case "staff.pending_leave_count" -> repository.staffPendingLeaveDetail(asOf, safeSize, offset, scope);
                case "staff.active_headcount" -> repository.staffActiveHeadcountDetail(asOf, safeSize, offset, scope);
                // 第六批（总控驾驶舱）余额类：待复核=待复核主管审批+未处置风险预警合并；截至日仅限制产生时间早于该自然日窗末。
                case "risk.pending_review" -> repository.pendingReviewDetail(asOf, safeSize, offset);
                default -> throw ApiException.badRequest("VALIDATION_FAILED",
                        "指标已登记但明细查询未接线：" + definition.metricKey());
            };
            String note = appendProjectScopeNote(
                    from != null ? "余额类指标取区间末值，起始日期不参与计算" : null, scope);
            return assembleMetricDetail(definition, null, null, asOf, now, detail.summaryValue(), null, null, note,
                    definition.columns(), detail, safePage, safeSize, true);
        }

        // 可加/比率分子的时间窗：经营日窗 [start(from), end(to))；缺省=当前经营日（登记 rangeFilterOptional=true 则缺省全历史）。
        LocalDate effectiveFrom = from != null ? from : (to != null ? to : (definition.rangeFilterOptional() ? null : currentBusinessDay));
        LocalDate effectiveTo = to != null ? to : (from != null ? from : (definition.rangeFilterOptional() ? null : currentBusinessDay));
        Instant windowStart = effectiveFrom == null ? null : businessDayResolver.startInstant(definition.windowDomain(), effectiveFrom);
        Instant windowEnd = effectiveTo == null ? null : businessDayResolver.endInstant(definition.windowDomain(), effectiveTo);

        if (isRatio) {
            // 首批唯一比率指标：energy.electricity_per_sqm。分子=区间用电量；分母=可租面积（区间末时点）。
            boolean numeratorActive = "numerator".equals(activePart);
            AnalyticsRecords.MetricDetailRows numerator =
                    repository.electricityReadingDetail(windowStart, windowEnd, numeratorActive ? safeSize : 1, numeratorActive ? offset : 0);
            AnalyticsRecords.MetricDetailRows denominator =
                    repository.rentableSpaceDetail(numeratorActive ? 1 : safeSize, numeratorActive ? 0 : offset);
            BigDecimal ratio = denominator.summaryValue().signum() > 0
                    ? numerator.summaryValue().divide(denominator.summaryValue(), 4, java.math.RoundingMode.HALF_UP)
                    : null;
            String note = ratio == null ? "分母（可租面积）为 0，比率不产出（不伪造）" : null;
            return assembleMetricDetail(definition, activePart, effectiveFrom, effectiveTo, now, ratio,
                    numerator.summaryValue(), denominator.summaryValue(), note,
                    numeratorActive ? definition.columns() : definition.denominatorColumns(),
                    numeratorActive ? numerator : denominator, safePage, safeSize, numeratorActive);
        }

        AnalyticsRecords.MetricDetailRows detail = switch (definition.metricKey()) {
            case "revenue.ticket" -> repository.ticketRevenueDetail(windowStart, windowEnd, safeSize, offset);
            case "finance.recon_diff_count" -> repository.reconciliationDifferenceDetail(windowStart, windowEnd, safeSize, offset);
            // 第二批（酒吧域）可加类：谓词逐字同各页面既有取数（见 MetricRegistry 口径说明）。
            case "revenue.bar" -> repository.barSettlementRevenueDetail(windowStart, windowEnd, safeSize, offset);
            case "bar.order_count" -> repository.barOrderDetail(windowStart, windowEnd, false, safeSize, offset);
            case "bar.order_amount" -> repository.barOrderDetail(windowStart, windowEnd, true, safeSize, offset);
            case "bar.saved_bottle_count" -> repository.savedBottleDetail(windowStart, windowEnd, safeSize, offset);
            case "energy.total_electricity" -> repository.electricityReadingDetail(windowStart, windowEnd, safeSize, offset);
            // 第三批（票务域）可加类：谓词逐字同各页面既有取数（见 MetricRegistry 口径说明）。
            case "ticket.order_count" -> repository.ticketOrderDetail(windowStart, windowEnd, false, safeSize, offset);
            case "ticket.paid_order_count" -> repository.ticketOrderDetail(windowStart, windowEnd, true, safeSize, offset);
            case "amusement.session_count" -> repository.amusementSessionDetail(windowStart, windowEnd, false, safeSize, offset);
            case "amusement.session_capacity" -> repository.amusementSessionDetail(windowStart, windowEnd, true, safeSize, offset);
            case "amusement.group_booking_count" -> repository.amusementGroupBookingDetail(windowStart, windowEnd, false, safeSize, offset);
            case "amusement.group_reserved_capacity" -> repository.amusementGroupBookingDetail(windowStart, windowEnd, true, safeSize, offset);
            case "amusement.entry_count" -> repository.amusementVerificationDirectionDetail(windowStart, windowEnd, true, safeSize, offset);
            case "amusement.exit_count" -> repository.amusementVerificationDirectionDetail(windowStart, windowEnd, false, safeSize, offset);
            case "amusement.inspection_count" -> repository.amusementInspectionDetail(windowStart, windowEnd, safeSize, offset);
            // 第四批（财务域）可加类：通票核销结算流水，谓词同结算审阅页 listVerificationSettled 基础谓词。
            case "settlement.transaction_count" -> repository.settlementTransactionDetail(windowStart, windowEnd, "count", safeSize, offset, scope);
            case "settlement.verified_amount" -> repository.settlementTransactionDetail(windowStart, windowEnd, "amount", safeSize, offset, scope);
            case "settlement.verified_count" -> repository.settlementTransactionDetail(windowStart, windowEnd, "tickets", safeSize, offset, scope);
            // 第五批（人事域）可加类：谓词逐字同各列表端点。班次按 work_date 落日区间、考勤按 punched_at 自然日窗、薪资单按 month 字符串区间——
            // 均用经营日区间的 LocalDate 端点（effectiveFrom/effectiveTo），由仓储自建各自的日期/自然日窗谓词（见 MetricRegistry 口径说明）。
            case "staff.shift_count" -> repository.staffShiftDetail(effectiveFrom, effectiveTo, safeSize, offset, scope);
            case "staff.attendance_count" -> repository.staffAttendanceDetail(effectiveFrom, effectiveTo, false, safeSize, offset, scope);
            case "staff.attendance_late_count" -> repository.staffAttendanceDetail(effectiveFrom, effectiveTo, true, safeSize, offset, scope);
            case "staff.payroll_net_total" -> repository.staffPayrollDetail(effectiveFrom, effectiveTo, true, safeSize, offset, scope,
                    staffPayroll == null ? List.of() : staffPayroll.visibleStatementIds());
            case "staff.payroll_statement_count" -> repository.staffPayrollDetail(effectiveFrom, effectiveTo, false, safeSize, offset, scope,
                    staffPayroll == null ? List.of() : staffPayroll.visibleStatementIds());
            // 第六批（总控驾驶舱）可加类：谓词逐字抄总览 ETL 聚合（DashboardAggregationJob），单日 as-of 时窗口与 ETL dayRange 同。
            case "traffic.amusement" -> repository.amusementVerificationSuccessDetail(windowStart, windowEnd, safeSize, offset);
            case "verify.done" -> repository.ticketVerificationSuccessDetail(windowStart, windowEnd, safeSize, offset);
            case "member.new" -> repository.newMemberDetail(windowStart, windowEnd, safeSize, offset);
            default -> throw ApiException.badRequest("VALIDATION_FAILED",
                    "指标已登记但明细查询未接线：" + definition.metricKey());
        };
        return assembleMetricDetail(definition, null, effectiveFrom, effectiveTo, now, detail.summaryValue(),
                null, null, appendProjectScopeNote("revenue.bar".equals(definition.metricKey()) ? BAR_CONFIRMED_REVENUE_NOTE : null, scope),
                definition.columns(), detail, safePage, safeSize, true);
    }

    /** 项目级作用域口径说明（全中文）：非 null scope 时在明细页顶部标注数字已限定为调用方可查看的项目。 */
    private static String appendProjectScopeNote(String base, java.util.Collection<String> scope) {
        if (scope == null) {
            return base;
        }
        String scoped = "本明细已限定为您可查看的项目，非平台全量";
        return base == null || base.isBlank() ? scoped : base + "；" + scoped;
    }

    private AnalyticsRecords.MetricDetailResponse assembleMetricDetail(
            MetricRegistry.MetricDefinition definition, String part, LocalDate from, LocalDate to, Instant dataAsOf,
            BigDecimal summaryValue, BigDecimal numeratorValue, BigDecimal denominatorValue, String summaryNote,
            List<MetricRegistry.MetricColumn> columns, AnalyticsRecords.MetricDetailRows detail,
            int page, int size, boolean rowTraceLinkable) {
        return new AnalyticsRecords.MetricDetailResponse(
                definition.metricKey(),
                definition.name(),
                definition.valueType(),
                definition.unit(),
                definition.caliber(),
                definition.source(),
                part,
                from,
                to,
                dataAsOf,
                summaryValue,
                numeratorValue,
                denominatorValue,
                summaryNote,
                columns,
                detail.rows(),
                page,
                size,
                detail.totalRows(),
                rowTraceLinkable ? definition.traceLinkPageId() : null,
                rowTraceLinkable ? definition.traceLinkColumnKey() : null);
    }

}
