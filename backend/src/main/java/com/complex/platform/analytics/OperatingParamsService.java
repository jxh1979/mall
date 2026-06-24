package com.complex.platform.analytics;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
