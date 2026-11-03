package com.complex.platform.analytics;

import com.complex.platform.common.Hashing;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import static com.complex.platform.common.Strings.requireText;

/**
 * 综合体经营参数「配置」服务（T4.2 坪效）：平台管理员维护 complex_operating_params（可经营面积），
 * 坪效指标 revenue.per_sqm 据此派生（净收入 / 面积）。
 *
 * <p>刻意与读模型 {@link AnalyticsService} 分离（仿 finance.RefundAuthorizationConfigService 与 classify 引擎分离的
 * 先例）——不动 AnalyticsService 多处测试构造器；配置写=敏感（决定坪效口径分母），upsert 落审计事件
 * {@code operating_params.changed}。幂等：repository 按 project_id ON CONFLICT DO UPDATE。</p>
 */
@Service
// 文件版本：V0.03
public class OperatingParamsService {

    private final AnalyticsRepository repository;
    private final AuditEventService auditEventService;
    private final Clock clock;

    // AUD-016 SSRF 防护：节假日抓取仅放行配置/默认白名单内的可信主机，防止高权限用户借抓取访问内网/云元数据地址。
    // 经配置项 analytics.holiday.fetch.allowed-hosts（env COMPLEX_ANALYTICS_HOLIDAY_FETCH_ALLOWED_HOSTS，逗号分隔）覆盖，
    // 未配置时回退 holiday-cn 官方源与常用镜像。用字段注入以避免改动两处单测复用的构造器签名（AnalyticsServiceTests）。
    private static final java.util.Set<String> DEFAULT_HOLIDAY_FETCH_HOSTS =
            java.util.Set.of("raw.githubusercontent.com", "cdn.jsdelivr.net", "gitee.com");

    @org.springframework.beans.factory.annotation.Value("${analytics.holiday.fetch.allowed-hosts:}")
    private String allowedHolidayFetchHostsCsv;

    @Autowired
    public OperatingParamsService(AnalyticsRepository repository, AuditEventService auditEventService) {
        this(repository, auditEventService, Clock.systemUTC());
    }

    OperatingParamsService(AnalyticsRepository repository, AuditEventService auditEventService, Clock clock) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.clock = clock;
    }

    public PageResponse<OperatingParamsRecord> listOperatingParams() {
        List<OperatingParamsRecord> items = repository.listOperatingParams();
        return new PageResponse<>(items, 1, 50, items.size());
    }

    public OperatingParamsRecord upsertOperatingParams(OperatingParamsRequest request) {
        OperatingParamsRequest actual = requireRequest(request);
        String projectId = requireText(actual.projectId(), "project_id");
        BigDecimal area = requirePositive(actual.operatingAreaSqm(), "operating_area_sqm");
        String status = trimToDefault(actual.status(), "active");
        Instant now = Instant.now(clock);
        OperatingParamsRecord saved = repository.upsertOperatingParams(
                new OperatingParamsRecord(projectId, area, status, now, now));
        auditEventService.recordSuccess(
                "operating_params.changed", "analytics.operating_params.manage",
                "complex_operating_params", projectId, null, actual.idempotencyKey(),
                Map.of("project_id", projectId, "operating_area_sqm", area));
        return saved;
    }

    /** M3:读异常检测阈值(经营参数页展示)。 */
    public AnalyticsRecords.AnomalyConfig getAnomalyConfig() {
        return repository.readAnomalyConfig();
    }

    /** M3(2026-07-02 拍板):环比突变阈值后台可调——可疑线/高风险线,0<可疑<=高;审计留痕。 */
    public AnalyticsRecords.AnomalyConfig updateAnomalyConfig(AnomalyConfigRequest request) {
        AnomalyConfigRequest actual = requireRequest(request);
        java.math.BigDecimal surge = actual.surgePct();
        java.math.BigDecimal high = actual.highPct();
        if (surge == null || high == null || surge.signum() <= 0 || high.compareTo(surge) < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "激增阈值（surge_pct）必须大于 0，且高位阈值（high_pct）不小于激增阈值");
        }
        boolean enabled = actual.enabled() == null || actual.enabled();
        AnalyticsRecords.AnomalyConfig saved = repository.upsertAnomalyConfig(
                surge, high, enabled, null, Instant.now(clock));
        auditEventService.recordSuccess(
                "analytics.anomaly_config.updated", "analytics.operating_params.manage",
                "analytics_anomaly_config", "default", null, actual.idempotencyKey(),
                java.util.Map.of("surge_pct", surge.toPlainString(), "high_pct", high.toPlainString(),
                        "enabled", enabled));
        return saved;
    }

    /** V219:列提醒窗天数配置(经营参数页展示)。 */
    public java.util.List<AnalyticsRecords.NotificationWindow> listNotificationWindows() {
        return repository.listNotificationWindows();
    }

    /**
     * V219:维护按天计的运营参数(提醒类:续约/存酒临期/招商跟进;留存类:V353 人脸照留存);审计留痕。
     * V353 起上界 3650 天(对齐 wecom_retention_policies 的 1..3650)——留存类键管的是个人信息销毁时点，
     * 没有上界等于允许"事实上永久留存"，与 PIPL 最短必要期冲突；提醒类键本就远小于 3650，行为不变。
     */
    public AnalyticsRecords.NotificationWindow upsertNotificationWindow(NotificationWindowRequest request) {
        NotificationWindowRequest actual = requireRequest(request);
        String key = requireText(actual.windowKey(), "window_key");
        Integer days = actual.days();
        if (days == null || days <= 0 || days > 3650) {
            throw ApiException.badRequest("VALIDATION_FAILED", "天数必须在 1 到 3650 之间");
        }
        AnalyticsRecords.NotificationWindow saved = repository.upsertNotificationWindow(
                key, days, actual.description(), null, Instant.now(clock));
        auditEventService.recordSuccess(
                "analytics.notification_windows.updated", "platform.notification_window.manage",
                "notification_windows", key, null, actual.idempotencyKey(),
                java.util.Map.of("window_key", key, "days", days));
        return saved;
    }

    /** V218:读 AI/对标阈值(经营参数页展示)。 */
    public AnalyticsRecords.AiThresholds getAiThresholds() {
        return repository.readAiThresholds();
    }

    /** V218:维护 AI建议采纳置信度 + 对标偏离告警线(均∈[0,1]);审计留痕。 */
    public AnalyticsRecords.AiThresholds updateAiThresholds(AiThresholdsRequest request) {
        AiThresholdsRequest actual = requireRequest(request);
        java.math.BigDecimal conf = actual.localLlmMinConfidence();
        java.math.BigDecimal dev = actual.benchmarkDeviation();
        if (conf == null || dev == null
                || conf.signum() < 0 || conf.compareTo(java.math.BigDecimal.ONE) > 0
                || dev.signum() < 0 || dev.compareTo(java.math.BigDecimal.ONE) > 0) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "本地大模型最低置信度与基准偏差必须在 0 到 1 之间");
        }
        AnalyticsRecords.AiThresholds saved = repository.upsertAiThresholds(conf, dev, null, Instant.now(clock));
        auditEventService.recordSuccess(
                "analytics.ai_thresholds.updated", "analytics.operating_params.manage",
                "analytics_ai_thresholds", "default", null, actual.idempotencyKey(),
                java.util.Map.of("local_llm_min_confidence", conf.toPlainString(),
                        "benchmark_deviation", dev.toPlainString()));
        return saved;
    }

    /** V221:列全部商户收入归因口径规则(default + merchant 特例)。 */
    public java.util.List<AnalyticsRecords.MerchantRevenueAttributionRule> listRevenueAttributionRules() {
        return repository.listRevenueAttributionRules();
    }

    /** V221:新增/更新一条收入归因口径规则(经营参数页维护);校验枚举/bps/scope,审计留痕。
     *  待主会话协调 batch0:升 Path 2——落草案行 id 确定性生成(不复用请求 id,避免与生效行撞 PK);updated_by=起草人(差人守卫依据)。 */
    public AnalyticsRecords.MerchantRevenueAttributionRule upsertRevenueAttributionRule(
            RevenueAttributionRuleRequest request, String actorId) {
        RevenueAttributionRuleRequest actual = requireRequest(request);
        requireText(actual.id(), "id"); // 保留 API 契约必填校验;Path 2 行 id 下方确定性生成,请求 id 不再作行标识
        String scope = requireOneOf(actual.ruleScope(), "rule_scope", "default", "merchant");
        String source = requireOneOf(actual.revenueSource(), "revenue_source",
                "ticket", "bar", "contract_rent", "thirdparty");
        String mode = requireOneOf(actual.attributionMode(), "attribution_mode", "gross", "share_bps", "exclude");
        String refund = requireOneOf(actual.refundAttribution(), "refund_attribution", "net_deduct", "ignore");
        String status = requireOneOf(actual.status() == null ? "active" : actual.status(),
                "status", "active", "inactive");
        String merchantId = actual.merchantId();
        if ("merchant".equals(scope) == (merchantId == null || merchantId.isBlank())) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "商户级规则必须指定商户，默认规则不能指定商户");
        }
        int bps = actual.shareBps() == null ? 10000 : actual.shareBps();
        if (bps < 0 || bps > 10000) {
            throw ApiException.badRequest("VALIDATION_FAILED", "分账比例（share_bps）必须在 0 到 10000 基点之间");
        }
        // 结算配置中心批0:改动一律先落草案(is_draft=TRUE),不允许经维护端点自助直接生效;
        // 转正须走 SettlementConfigController 复核端点(settlement.config.manage,与编辑权限分离)。
        boolean isDraft = true;
        String merchant = "default".equals(scope) ? null : merchantId;
        // Path 2 草案行 id 确定性 = scope|source|merchant|idempotencyKey(同请求幂等,跨编辑轮相异→不撞 PK;草案槽唯一由部分索引保证)。
        String rowId = "mrar_d_" + Hashing.idempotencyCode(
                scope + "|" + source + "|" + (merchant == null ? "" : merchant) + "|" + actual.idempotencyKey());
        AnalyticsRecords.MerchantRevenueAttributionRule saved = repository.upsertRevenueAttributionRule(
                new AnalyticsRecords.MerchantRevenueAttributionRule(
                        rowId, scope, merchant, source, mode, bps, refund, status, isDraft, null, null),
                actorId, Instant.now(clock));
        auditEventService.recordSuccess(
                "analytics.revenue_attribution_rule.updated", "analytics.operating_params.manage",
                "merchant_revenue_attribution_rules", saved == null ? rowId : saved.id(), null, actual.idempotencyKey(),
                java.util.Map.of("rule_scope", scope, "revenue_source", source,
                        "attribution_mode", mode, "share_bps", String.valueOf(bps),
                        "refund_attribution", refund, "is_draft", String.valueOf(isDraft)));
        return saved;
    }

    /**
     * 结算配置中心批0:把一条收入归因规则草案(is_draft=TRUE)复核转正为正式生效口径(FALSE)。
     * 差人守卫(项5):执行人 = 草案起草人 → 403;前态守卫:仅草案态可转正(行数0=不存在/已是正式)。
     */
    public void activateRevenueAttributionRuleDraft(String id, String actorId) {
        String ruleId = requireText(id, "id");
        String drafter = repository.attributionRuleDraftUpdatedBy(ruleId);
        if (drafter != null && drafter.equals(actorId)) {
            throw ApiException.forbidden("SELF_ACTIVATION_FORBIDDEN",
                    "不能自行转正自己起草的改动,须另一高权限人复核批准");
        }
        int rows = repository.activateAttributionRuleDraft(ruleId, actorId, Instant.now(clock));
        if (rows == 0) {
            throw ApiException.badRequest("DRAFT_NOT_ACTIVATABLE", "规则不存在或非草案态,无法转正");
        }
        auditEventService.recordSuccess(
                "settlement.config.draft.activated", "settlement.config.manage",
                "merchant_revenue_attribution_rules", ruleId, null, null,
                java.util.Map.of("config_group", "revenue_attribution"));
    }

    private String requireOneOf(String value, String field, String... allowed) {
        for (String a : allowed) {
            if (a.equals(value)) {
                return value;
            }
        }
        throw ApiException.badRequest("VALIDATION_FAILED",
                field + " 必须为以下之一：" + String.join(", ", allowed));
    }

    /** H1:列节假日日历(经营参数页维护区)。 */
    public java.util.List<AnalyticsRecords.HolidayPeriod> listHolidayPeriods(Integer year) {
        return repository.listHolidayPeriods(year);
    }

    /** H1:手动录入/修正节假日区间(每年按国务院公布;同名同年幂等覆盖;审计留痕)。 */
    public AnalyticsRecords.HolidayPeriod upsertHolidayPeriod(HolidayPeriodRequest request) {
        HolidayPeriodRequest actual = requireRequest(request);
        String name = requireText(actual.holidayName(), "holiday_name");
        Integer year = actual.holidayYear();
        if (year == null || year < 2000 || year > 2100) {
            throw ApiException.badRequest("VALIDATION_FAILED", "节假日年份必须在 2000 到 2100 之间");
        }
        java.time.LocalDate start = actual.startDate();
        java.time.LocalDate end = actual.endDate();
        if (start == null || end == null || end.isBefore(start)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "开始日期或结束日期不正确");
        }
        AnalyticsRecords.HolidayPeriod saved = repository.upsertHolidayPeriod(
                name, year, start, end, "manual", Instant.now(clock));
        auditEventService.recordSuccess(
                "analytics.holiday_calendar.updated", "analytics.operating_params.manage",
                "holiday_calendar", name + "-" + year, null, actual.idempotencyKey(),
                java.util.Map.of("holiday_name", name, "holiday_year", year,
                        "start_date", start.toString(), "end_date", end.toString()));
        return saved;
    }

    /** H1(2026-07-02 拍板):从 URL 自动抓节假日日历(holiday-cn JSON 格式 {days:[{name,date,isOffDay}]});
     * 农历+调休不可程序推算,抓取失败(如内网不通外网)不动既有数据,提示手动录入兜底。 */
    public java.util.Map<String, Object> fetchHolidayCalendar(HolidayFetchRequest request) {
        HolidayFetchRequest actual = requireRequest(request);
        String url = requireText(actual.url(), "url");
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "链接必须为 http(s) 地址");
        }
        requireTrustedHolidayHost(url);
        String body;
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(5)).build();
            java.net.http.HttpResponse<String> response = client.send(
                    java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                            .timeout(java.time.Duration.ofSeconds(10)).GET().build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        "节假日数据源返回 HTTP " + response.statusCode() + "，现有日历未受影响");
            }
            body = response.body();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "节假日数据源不可达（" + e.getClass().getSimpleName() + "），现有日历未受影响，可改用手工录入");
        }
        java.util.Map<String, int[]> merged = new java.util.LinkedHashMap<>();
        java.util.Map<String, java.util.List<java.time.LocalDate>> byName = new java.util.LinkedHashMap<>();
        try {
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
            com.fasterxml.jackson.databind.JsonNode days = root.get("days");
            if (days == null || !days.isArray()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "数据格式不支持：应为含 days[] 的 holiday-cn JSON");
            }
            for (com.fasterxml.jackson.databind.JsonNode day : days) {
                if (!day.path("isOffDay").asBoolean(false)) {
                    continue;
                }
                String name = day.path("name").asText("");
                String date = day.path("date").asText("");
                if (name.isEmpty() || date.isEmpty()) {
                    continue;
                }
                byName.computeIfAbsent(name, k -> new java.util.ArrayList<>()).add(java.time.LocalDate.parse(date));
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.badRequest("VALIDATION_FAILED", "节假日数据源解析失败，现有日历未受影响");
        }
        if (byName.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "数据源中未找到休息日，现有日历未受影响");
        }
        Instant now = Instant.now(clock);
        int imported = 0;
        for (java.util.Map.Entry<String, java.util.List<java.time.LocalDate>> entry : byName.entrySet()) {
            java.util.List<java.time.LocalDate> dates = entry.getValue();
            java.time.LocalDate start = java.util.Collections.min(dates);
            java.time.LocalDate end = java.util.Collections.max(dates);
            // 同名假期取 min~max 区间(调休夹缝上班日对区间聚合影响可忽略,注释口径)。
            repository.upsertHolidayPeriod(entry.getKey(), start.getYear(), start, end, "fetched", now);
            imported++;
        }
        auditEventService.recordSuccess(
                "analytics.holiday_calendar.fetched", "analytics.operating_params.manage",
                "holiday_calendar", url, null, actual.idempotencyKey(),
                java.util.Map.of("url", url, "imported", imported));
        return java.util.Map.of("imported", imported, "holidays", byName.keySet());
    }

    /** AUD-016 SSRF 防护：仅放行配置/默认白名单内的抓取主机（精确 host 匹配，拒绝内网/元数据地址/域名混淆如 a.com@evil.com）。 */
    private void requireTrustedHolidayHost(String url) {
        String host;
        try {
            host = java.net.URI.create(url).getHost();
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("VALIDATION_FAILED", "链接格式不正确");
        }
        if (host == null || !allowedHolidayFetchHosts().contains(host.toLowerCase(java.util.Locale.ROOT))) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "节假日数据源域名不在受信白名单内");
        }
    }

    private java.util.Set<String> allowedHolidayFetchHosts() {
        String csv = allowedHolidayFetchHostsCsv;
        if (csv == null || csv.isBlank()) {
            return DEFAULT_HOLIDAY_FETCH_HOSTS;
        }
        java.util.Set<String> hosts = new java.util.LinkedHashSet<>();
        for (String part : csv.split(",")) {
            String trimmed = part.trim().toLowerCase(java.util.Locale.ROOT);
            if (!trimmed.isEmpty()) {
                hosts.add(trimmed);
            }
        }
        return hosts.isEmpty() ? DEFAULT_HOLIDAY_FETCH_HOSTS : hosts;
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        if (value.signum() <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 必须大于 0");
        }
        return value;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }
}
