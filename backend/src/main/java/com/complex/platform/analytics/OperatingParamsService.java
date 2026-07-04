package com.complex.platform.analytics;

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

/**
 * 综合体经营参数「配置」服务（T4.2 坪效）：平台管理员维护 complex_operating_params（可经营面积），
 * 坪效指标 revenue.per_sqm 据此派生（净收入 / 面积）。
 *
 * <p>刻意与读模型 {@link AnalyticsService} 分离（仿 finance.RefundAuthorizationConfigService 与 classify 引擎分离的
 * 先例）——不动 AnalyticsService 多处测试构造器；配置写=敏感（决定坪效口径分母），upsert 落审计事件
 * {@code operating_params.changed}。幂等：repository 按 project_id ON CONFLICT DO UPDATE。</p>
 */
@Service
public class OperatingParamsService {

    private final AnalyticsRepository repository;
    private final AuditEventService auditEventService;
    private final Clock clock;

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

    /** S0:经营日切分配置读取(九域,经营参数页数据源)。 */
    public java.util.List<AnalyticsRecords.BusinessDayCutoffRecord> listBusinessDayCutoffs() {
        return repository.listBusinessDayCutoffs();
    }

    private static final java.util.Set<String> CUTOFF_DOMAINS = java.util.Set.of(
            "platform", "finance", "ticketing", "amusement", "customer", "inventory", "governance", "bar", "merchant");

    /**
     * S0(2026-07-02 拍板):改某业务域的日切小时——各业务的「一天」后台可配(酒吧6点/零售餐饮10点)。
     * platform 行是跨域汇总与财务日结的对齐基准,允许改但影响全场对账口径;审计留痕。
     * BusinessDayResolver 60s 缓存,改动一分钟内生效。
     */
    public AnalyticsRecords.BusinessDayCutoffRecord upsertBusinessDayCutoff(BusinessDayCutoffRequest request) {
        BusinessDayCutoffRequest actual = requireRequest(request);
        String domain = requireText(actual.domain(), "domain");
        if (!CUTOFF_DOMAINS.contains(domain)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "domain must be one of " + CUTOFF_DOMAINS);
        }
        Integer hour = actual.cutoffHour();
        if (hour == null || hour < 0 || hour > 23) {
            throw ApiException.badRequest("VALIDATION_FAILED", "cutoff_hour must be between 0 and 23");
        }
        AnalyticsRecords.BusinessDayCutoffRecord saved =
                repository.upsertBusinessDayCutoff(domain, hour, null, Instant.now(clock));
        auditEventService.recordSuccess(
                "analytics.business_day_cutoff.updated", "analytics.operating_params.manage",
                "business_day_cutoff_config", domain, null, actual.idempotencyKey(),
                java.util.Map.of("domain", domain, "cutoff_hour", hour));
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
            throw ApiException.badRequest("VALIDATION_FAILED", "surge_pct must be > 0 and high_pct >= surge_pct");
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

    /** V219:维护提醒窗天数(续约/存酒临期/招商跟进等);days>0;审计留痕。 */
    public AnalyticsRecords.NotificationWindow upsertNotificationWindow(NotificationWindowRequest request) {
        NotificationWindowRequest actual = requireRequest(request);
        String key = requireText(actual.windowKey(), "window_key");
        Integer days = actual.days();
        if (days == null || days <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "days must be > 0");
        }
        AnalyticsRecords.NotificationWindow saved = repository.upsertNotificationWindow(
                key, days, actual.description(), null, Instant.now(clock));
        auditEventService.recordSuccess(
                "analytics.notification_window.updated", "platform.notification_window.manage",
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
                    "local_llm_min_confidence and benchmark_deviation must be in [0,1]");
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

    /** V221:新增/更新一条收入归因口径规则(经营参数页维护);校验枚举/bps/scope,审计留痕。 */
    public AnalyticsRecords.MerchantRevenueAttributionRule upsertRevenueAttributionRule(
            RevenueAttributionRuleRequest request) {
        RevenueAttributionRuleRequest actual = requireRequest(request);
        String id = requireText(actual.id(), "id");
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
                    "merchant_id is required for rule_scope=merchant and must be empty for rule_scope=default");
        }
        int bps = actual.shareBps() == null ? 10000 : actual.shareBps();
        if (bps < 0 || bps > 10000) {
            throw ApiException.badRequest("VALIDATION_FAILED", "share_bps must be in [0,10000]");
        }
        boolean isDraft = actual.isDraft() == null || actual.isDraft();
        AnalyticsRecords.MerchantRevenueAttributionRule saved = repository.upsertRevenueAttributionRule(
                new AnalyticsRecords.MerchantRevenueAttributionRule(
                        id, scope, "default".equals(scope) ? null : merchantId, source, mode, bps,
                        refund, status, isDraft, null, null),
                null, Instant.now(clock));
        auditEventService.recordSuccess(
                "analytics.revenue_attribution_rule.updated", "analytics.operating_params.manage",
                "merchant_revenue_attribution_rules", id, null, actual.idempotencyKey(),
                java.util.Map.of("rule_scope", scope, "revenue_source", source,
                        "attribution_mode", mode, "share_bps", String.valueOf(bps),
                        "refund_attribution", refund, "is_draft", String.valueOf(isDraft)));
        return saved;
    }

    private String requireOneOf(String value, String field, String... allowed) {
        for (String a : allowed) {
            if (a.equals(value)) {
                return value;
            }
        }
        throw ApiException.badRequest("VALIDATION_FAILED",
                field + " must be one of " + String.join(", ", allowed));
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
            throw ApiException.badRequest("VALIDATION_FAILED", "holiday_year must be between 2000 and 2100");
        }
        java.time.LocalDate start = actual.startDate();
        java.time.LocalDate end = actual.endDate();
        if (start == null || end == null || end.isBefore(start)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "start_date/end_date invalid");
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
            throw ApiException.badRequest("VALIDATION_FAILED", "url must be http(s)");
        }
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
                        "holiday source returned HTTP " + response.statusCode() + "; existing calendar untouched");
            }
            body = response.body();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "holiday source unreachable (" + e.getClass().getSimpleName() + "); existing calendar untouched, use manual entry");
        }
        java.util.Map<String, int[]> merged = new java.util.LinkedHashMap<>();
        java.util.Map<String, java.util.List<java.time.LocalDate>> byName = new java.util.LinkedHashMap<>();
        try {
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
            com.fasterxml.jackson.databind.JsonNode days = root.get("days");
            if (days == null || !days.isArray()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "unsupported format: expecting holiday-cn JSON with days[]");
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
            throw ApiException.badRequest("VALIDATION_FAILED", "holiday source parse failed; existing calendar untouched");
        }
        if (byName.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "no off-days found in source; existing calendar untouched");
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

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireText(String value, String field) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return trimmed;
    }

    private BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        if (value.signum() <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " must be positive");
        }
        return value;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }
}
