package com.complex.platform.analytics;

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
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
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
    private static final Set<String> TERMINAL_ALERT_STATUSES = Set.of("resolved", "dismissed");
    private static final BigDecimal LOCAL_LLM_MIN_CONFIDENCE = new BigDecimal("0.60");
    private static final int MAX_SOURCE_TEXT_CHARS = 12_000;
    private static final int MAX_LLM_INPUT_CHARS = 4_000;

    private static final String MERCHANT_SCOPE_UNAVAILABLE =
            "source table has no merchant_id; merchant-scoped value not available";

    /**
     * T12 看板七类指标目录（与 r5-aggregation-metrics-draft §2 / DashboardAggregationJob 对齐）。
     * ready=false 的指标数据源未就绪，按"不伪造"返回 null + note（采购到货依赖 T6；补录依赖 P1 留痕；第三方回流依赖开关接线）。
     */
    private static final List<MetricCatalogEntry> METRIC_CATALOG = List.of(
            new MetricCatalogEntry(DashboardAggregationJob.REVENUE_TICKET, "finance", "cent", true,
                    "ticket_orders has no merchant_id; merchant-scoped value not available"),
            new MetricCatalogEntry(DashboardAggregationJob.REVENUE_BAR, "bar", "cent", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry(DashboardAggregationJob.REVENUE_BAR_REFUND, "bar", "cent", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry(DashboardAggregationJob.REVENUE_TICKET_REFUND, "finance", "cent", true,
                    "ticket_refunds has no merchant_id; merchant-scoped value not available"),
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
                    "occupancy is platform-scope only (not emitted when rentable area is zero)"),
            new MetricCatalogEntry(DashboardAggregationJob.OCC_RENTABLE_AREA, "leasing", "sqm", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry(DashboardAggregationJob.OCC_LEASED_AREA, "leasing", "sqm", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry(DashboardAggregationJob.OCC_SIGNED_PENDING_AREA, "leasing", "sqm", true, MERCHANT_SCOPE_UNAVAILABLE),
            new MetricCatalogEntry(DashboardAggregationJob.OCC_PARKING_RATE, "leasing", "rate", true,
                    "parking occupancy is platform-scope only (not emitted when parking area is zero)"),
            new MetricCatalogEntry(DashboardAggregationJob.CONTRACT_EXPIRING_90D, "leasing", "count", true, MERCHANT_SCOPE_UNAVAILABLE),
            // S4 工单时效(平台口径,facility 窗):
            new MetricCatalogEntry(DashboardAggregationJob.WO_RESPONSE_HOURS_P50, "facility", "hours", true,
                    "response P50 is platform-scope only (not emitted when no responded orders in window)"),
            new MetricCatalogEntry(DashboardAggregationJob.WO_HANDLE_HOURS_P50, "facility", "hours", true,
                    "handle P50 is platform-scope only (not emitted when no completed orders in window)"),
            new MetricCatalogEntry(DashboardAggregationJob.WO_OVERDUE_RATE, "facility", "rate", true,
                    "overdue rate is platform-scope only (not emitted when no orders due in window)"),
            new MetricCatalogEntry(DashboardAggregationJob.WO_BACKLOG, "facility", "count", true, MERCHANT_SCOPE_UNAVAILABLE),
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
    private static final BigDecimal BENCHMARK_THRESHOLD = new BigDecimal("0.20");

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

    /** S2 钻取:欠费商户明细排行(实时,与账龄快照同过滤同口径;bucket 可空=全部)。 */
    public java.util.List<AnalyticsRecords.ArDebtorRow> listOverdueDebtors(String bucket, int limit, int offset) {
        String normalized = bucket == null || bucket.isBlank() ? null : bucket;
        if (normalized != null && !AR_BUCKETS.contains(normalized)) {
            throw com.complex.platform.common.api.ApiException.badRequest(
                    "VALIDATION_FAILED", "bucket must be one of " + AR_BUCKETS);
        }
        String finalBucket = normalized;
        return readSingle(
                "analytics.ar_debtors.viewed",
                "analytics.ar_debtors.view_failed",
                "finance.ar_ap.view",
                "ar_ap_items",
                () -> repository.listOverdueDebtors(
                        LocalDate.ofInstant(Instant.now(clock), java.time.ZoneOffset.UTC), finalBucket, limit, offset)
        );
    }

    /** S3 钻取:未出租铺位明细(实时,与出租率快照同过滤同口径)。 */
    public java.util.List<AnalyticsRecords.VacantSpaceRow> listVacantSpaces(int limit, int offset) {
        return readSingle(
                "analytics.vacant_spaces.viewed",
                "analytics.vacant_spaces.view_failed",
                "contract.view",
                "spaces",
                () -> repository.listVacantSpaces(
                        LocalDate.ofInstant(Instant.now(clock), java.time.ZoneOffset.UTC), limit, offset)
        );
    }

    /** S3 钻取:到期预警合同明细(窗口天数 1-365)。 */
    public java.util.List<AnalyticsRecords.ExpiringContractRow> listExpiringContracts(int windowDays, int limit, int offset) {
        int days = Math.min(Math.max(windowDays, 1), 365);
        return readSingle(
                "analytics.expiring_contracts.viewed",
                "analytics.expiring_contracts.view_failed",
                "contract.view",
                "contracts",
                () -> repository.listExpiringContracts(
                        LocalDate.ofInstant(Instant.now(clock), java.time.ZoneOffset.UTC), days, limit, offset)
        );
    }

    /** M1 月度对比目录(展示白名单;label=业务名)。聚合口径由 job 月度白名单单一事实源派生,杜绝两处漂移。 */
    private record MonthlyMetricEntry(String key, String label, String domain, String unit) {
    }

    private static final List<MonthlyMetricEntry> MONTHLY_COMPARISON_CATALOG = List.of(
            new MonthlyMetricEntry(DashboardAggregationJob.REVENUE_NET, "净收入", "finance", "cent"),
            new MonthlyMetricEntry(DashboardAggregationJob.REVENUE_TICKET, "售票收入", "ticketing", "cent"),
            new MonthlyMetricEntry(DashboardAggregationJob.REVENUE_BAR, "酒吧收入", "bar", "cent"),
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

    private java.time.YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return java.time.YearMonth.from(LocalDate.ofInstant(Instant.now(clock), java.time.ZoneOffset.UTC));
        }
        try {
            return java.time.YearMonth.parse(month.trim());
        } catch (java.time.format.DateTimeParseException e) {
            throw com.complex.platform.common.api.ApiException.badRequest("VALIDATION_FAILED", "month must be YYYY-MM");
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

    /** S4 钻取:超时工单明细(实时,与 wo.overdue_rate 分子同过滤:处理时限落当日 facility 窗且超时)。 */
    public java.util.List<AnalyticsRecords.OverdueWorkOrderRow> listOverdueWorkOrders(int limit, int offset) {
        return readSingle(
                "analytics.overdue_work_orders.viewed",
                "analytics.overdue_work_orders.view_failed",
                "facility.work_order.manage",
                "facility_work_orders",
                () -> {
                    Instant now = Instant.now(clock);
                    LocalDate businessDate = businessDayResolver.businessDate("facility", now);
                    return repository.listOverdueWorkOrders(
                            businessDayResolver.startInstant("facility", businessDate),
                            businessDayResolver.endInstant("facility", businessDate), now, limit, offset);
                }
        );
    }

    /** S5 钻取:异常读数明细(实时,与 energy.abnormal_count 同过滤:读数落当日 energy 窗)。 */
    public java.util.List<AnalyticsRecords.AbnormalReadingRow> listAbnormalReadings(int limit, int offset) {
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
                            businessDayResolver.endInstant("energy", businessDate), limit, offset);
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
                metrics.put(entry.metricCode(), MetricValue.of(value, entry.unit()));
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

    public GroupOverviewResponse groupOverview(String groupBy) {
        String normalizedGroupBy = normalizeGroupBy(groupBy);
        return readSingle(
                "analytics.group_overview.viewed",
                "analytics.group_overview.view_failed",
                "analytics.group.view",
                "analytics_business_metric_snapshot",
                () -> buildGroupOverview(normalizedGroupBy)
        );
    }

    private GroupOverviewResponse buildGroupOverview(String groupBy) {
        Instant now = Instant.now(clock);
        Optional<LocalDate> etlDate = repository.latestEtlBusinessDate(null);
        LocalDate businessDate = etlDate.orElse(LocalDate.ofInstant(now, java.time.ZoneOffset.UTC));
        boolean byEntity = "entity".equals(groupBy);

        // ⑤ 集团多项目 ETL：优先读 segment_type='project' 维度，按 project_id 产出 per-project rollup（多综合体→多行）。
        // 无 project 口径快照时回退到平台口径单行（向后兼容旧 ETL 与"无快照诚实空态"），不伪造多 project。
        List<String> projectIds = etlDate.isPresent()
                ? repository.listProjectIdsWithEtlSnapshot(businessDate)
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
                        ? MetricValue.of(value, entry.unit(), Map.of("below_benchmark", true))
                        : MetricValue.of(value, entry.unit()));
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
        BigDecimal floorMultiplier = BigDecimal.ONE.subtract(BENCHMARK_THRESHOLD);
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
                metrics.put(entry.metricCode(), MetricValue.of(average, entry.unit()));
            }
        }
        return metrics;
    }

    private String normalizeGroupBy(String groupBy) {
        String value = groupBy == null || groupBy.isBlank()
                ? "project"
                : groupBy.trim().toLowerCase(Locale.ROOT);
        if (!"project".equals(value) && !"entity".equals(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "group_by must be project or entity");
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

    public PageResponse<BusinessMetricSnapshotRecord> listBusinessMetricSnapshots() {
        return readPage(
                "analytics.business_metric_snapshot.viewed",
                "analytics.business_metric_snapshot.view_failed",
                "analytics.dashboard.view",
                "analytics_business_metric_snapshot",
                repository::listBusinessMetricSnapshots
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

    public PageResponse<MetricAnomalyRecord> listMetricAnomalies() {
        return readPage(
                "analytics.metric_anomaly.viewed",
                "analytics.metric_anomaly.viewed_failed",
                "analytics.risk_alert.view",
                "analytics_metric_anomaly",
                repository::listMetricAnomalies
        );
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

    private AiQualitySuggestionResponse generateLocalLlmAiQualitySuggestionInternal(AiQualitySuggestionRequest request) {
        AiQualitySuggestionRequest actual = requireRequest(request);
        String sourceType = requireLimitedText(actual.sourceType(), "source_type", 64);
        String sourceId = requireLimitedText(actual.sourceId(), "source_id", 64);
        String requestedBy = optionalLimitedText(actual.requestedBy(), "requested_by", 64);
        String sourceText = requireLimitedText(actual.sourceText(), "source_text", MAX_SOURCE_TEXT_CHARS);
        AiTextRedactor.RedactedText redacted = AiTextRedactor.redactAndTruncate(sourceText, MAX_LLM_INPUT_CHARS);
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
                || confidenceScore.compareTo(LOCAL_LLM_MIN_CONFIDENCE) < 0
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
                .orElseThrow(() -> ApiException.notFound("RISK_ALERT_NOT_FOUND", "risk alert not found"));
        if (TERMINAL_ALERT_STATUSES.contains(current.status())) {
            throw ApiException.conflict("RISK_ALERT_ALREADY_RESOLVED", "risk alert is already resolved");
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
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
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
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is too long");
        }
        return text;
    }

    private String optionalLimitedText(String value, String fieldName, int maxLength) {
        String text = trimToNull(value);
        if (text != null && text.length() > maxLength) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is too long");
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

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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


    private record MetricCatalogEntry(
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


}
