package com.complex.platform.finance;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.dto.RefundAuthorityLimitRequest;
import com.complex.platform.finance.dto.RefundAutoRuleRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import static com.complex.platform.common.Strings.requireText;

/**
 * 退款分级授权「配置」服务（Stage C，审计 A2 闭环）：平台管理员维护 refund_authority_limits / refund_auto_rules
 * 主数据（员工自助退款额度 / 自动放行规则），替代手工改库；配好后打开功能标志即三域分级授权生效。
 *
 * <p>刻意与 {@link RefundAuthorizationService}（运行时 classify 引擎）分离：引擎被三域退款链注入、被多处单测
 * {@code new RefundAuthorizationService(repo)}，保持零额外依赖最稳；配置写需 {@link AuditEventService} 留痕，
 * 若塞进引擎构造会连带破坏既有单测。
 *
 * <p>配置写=安全敏感动作（决定谁能退多少、哪些自动放行），故 upsert 落审计事件 {@code refund_authority.changed}。
 * 幂等：repository 按自然键 ON CONFLICT DO UPDATE；id 由自然键确定性派生（首次 INSERT 用、冲突保留原行）。
 */
@Service
public class RefundAuthorizationConfigService {

    private final RefundAuthorizationRepository repository;
    private final AuditEventService auditEventService;

    public RefundAuthorizationConfigService(
            RefundAuthorizationRepository repository, AuditEventService auditEventService) {
        this.repository = repository;
        this.auditEventService = auditEventService;
    }

    public PageResponse<RefundAuthorityLimitRecord> listLimits(String projectId) {
        List<RefundAuthorityLimitRecord> items = repository.listLimits(trimToNullProject(projectId));
        return new PageResponse<>(items, 1, 50, items.size());
    }

    public RefundAuthorityLimitRecord upsertLimit(RefundAuthorityLimitRequest request) {
        RefundAuthorityLimitRequest actual = requireRequest(request);
        String subjectType = requireText(actual.subjectType(), "subject_type");
        String subjectId = requireText(actual.subjectId(), "subject_id");
        String refundDomain = requireText(actual.refundDomain(), "refund_domain");
        long limitAmountCent = requireNonNegative(actual.limitAmountCent(), "limit_amount_cent");
        String status = trimToDefault(actual.status(), "active");
        // B1d② id 含 project 维度，使不同综合体（及全局）同 subject/domain 的额度互不覆盖。
        String projectId = trimToNullProject(actual.projectId());
        RefundAuthorityLimitRecord saved = repository.saveLimit(new RefundAuthorityLimitRecord(
                "refund_limit_" + slug(subjectType + "_" + subjectId + "_" + refundDomain
                        + "_" + (projectId == null ? "global" : projectId)),
                subjectType, subjectId, refundDomain, limitAmountCent, projectId, status, null, null));
        auditEventService.recordSuccess(
                "refund_authority.changed", "refund.authority.manage", "refund_authority_limit", saved.id(),
                null, actual.idempotencyKey(),
                Map.of("subject_type", subjectType, "subject_id", subjectId, "refund_domain", refundDomain,
                        "limit_amount_cent", limitAmountCent));
        return saved;
    }

    public PageResponse<RefundAutoRuleRecord> listRules(String projectId) {
        List<RefundAutoRuleRecord> items = repository.listRules(trimToNullProject(projectId));
        return new PageResponse<>(items, 1, 50, items.size());
    }

    public RefundAutoRuleRecord upsertRule(RefundAutoRuleRequest request) {
        RefundAutoRuleRequest actual = requireRequest(request);
        String refundDomain = requireText(actual.refundDomain(), "refund_domain");
        String ruleType = requireText(actual.ruleType(), "rule_type");
        boolean enabled = actual.enabled() != null && actual.enabled();
        String status = trimToDefault(actual.status(), "active");
        // B1d② id 含 project 维度，使不同综合体（及全局）同 domain/rule_type 的规则互不覆盖。
        String projectId = trimToNullProject(actual.projectId());
        RefundAutoRuleRecord saved = repository.saveRule(new RefundAutoRuleRecord(
                "refund_rule_" + slug(refundDomain + "_" + ruleType
                        + "_" + (projectId == null ? "global" : projectId)),
                refundDomain, ruleType, enabled, projectId, status, null, null));
        auditEventService.recordSuccess(
                "refund_authority.changed", "refund.authority.manage", "refund_auto_rule", saved.id(),
                null, actual.idempotencyKey(),
                Map.of("refund_domain", refundDomain, "rule_type", ruleType, "enabled", enabled));
        return saved;
    }

    // ── 校验/工具（ApiRequests 仅有 requireIdempotencyKey，其余内联） ──
    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private long requireNonNegative(Long value, String field) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        if (value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " must be non-negative");
        }
        return value;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    /** 空白 project_id 归一为 null（=全局/平台默认），与 DB NULL 语义对齐。 */
    private String trimToNullProject(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String slug(String value) {
        String s = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return s.length() > 80 ? s.substring(0, 80) : s;
    }
}
