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
