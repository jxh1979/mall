package com.complex.platform.customer;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.customer.dto.CustomerPointsRuleRequest;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;

/**
 * 后台积分规则引擎管理（REQ-12, P0-6）：CRUD 积分获取/有效期/过期/等级阈值配置。
 * 权限：查看 customer.points_rule.view（medium）/ 维护 customer.points_rule.manage（high，V269）。
 * 租户隔离：project_id 为综合体级 direct scope。项目级管理员只见/只写本项目（服务层按 token scope 覆盖
 * 请求体 project_id，遵循 AC-ISO-2 写路径不信任请求体）；平台管理员可跨项目、创建时须显式给 project_id。
 */
@Service
public class CustomerPointsRuleAdminService {

    private static final String VIEW = "customer.points_rule.view";
    private static final String MANAGE = "customer.points_rule.manage";
    private static final String BUSINESS_TYPE = "customer_points_rule";
    // 允许值对齐 V060 DB CHECK 约束（chk_cpr_*），在服务层先挡下非法枚举返回 400，避免裸 DB 约束违例 500。
    private static final Set<String> EARN_METHODS = Set.of("consume_amount", "check_in", "activity");
    private static final Set<String> VALIDITY_TYPES = Set.of("permanent", "fixed_days", "natural_year");
    private static final Set<String> EXPIRE_MODES = Set.of("fifo_batch", "whole_account_yearly");
    private static final Set<String> STATUSES = Set.of("draft", "active", "paused", "expired");

    private final CustomerRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;

    public CustomerPointsRuleAdminService(
            CustomerRepository repository,
            AuthorizationService authorizationService,
            AuditEventService auditEventService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
    }

    /** 列积分规则：项目级管理员只见本项目，平台管理员见全部。 */
    public PageResponse<CustomerPointsRuleRecord> list() {
        authorizationService.requirePermission(VIEW);
        Optional<String> scope = authorizationService.currentProjectScopeQuietly();
        List<CustomerPointsRuleRecord> items = scope
                .map(repository::listPointsRulesByProject)
                .orElseGet(repository::listPointsRules);
        auditEventService.recordSuccess("customer.points_rule.listed", VIEW, BUSINESS_TYPE, "list",
                null, null, Map.of("count", items.size(), "scoped", scope.isPresent()));
        return new PageResponse<>(items, 1, Math.max(items.size(), 1), items.size());
    }

    /** 取单条规则：项目级管理员越域访问按 404 处理（不泄漏他项目存在性）。 */
    public CustomerPointsRuleRecord get(String id) {
        authorizationService.requirePermission(VIEW);
        CustomerPointsRuleRecord rule = loadInScope(id);
        auditEventService.recordSuccess("customer.points_rule.viewed", VIEW, BUSINESS_TYPE, rule.id(),
                null, null, Map.of());
        return rule;
    }

    @Transactional
    public CustomerPointsRuleRecord create(CustomerPointsRuleRequest request) {
        authorizationService.requirePermission(MANAGE);
        try {
            CustomerPointsRuleRequest r = require(request);
            String projectId = resolveWriteProjectId(r.projectId());
            Instant now = Instant.now();
            CustomerPointsRuleRecord saved = repository.savePointsRule(new CustomerPointsRuleRecord(
                    "points_rule_" + UUID.randomUUID(),
                    projectId,
                    requireText(r.ruleName(), "rule_name"),
                    requireEnum(r.earnMethod(), EARN_METHODS, "earn_method"),
                    requireNonNegative(r.earnRatio(), "earn_ratio"),
                    r.levelThreshold() == null ? "{}" : r.levelThreshold(),
                    requireEnum(r.validityType(), VALIDITY_TYPES, "validity_type"),
                    r.validityDays(),
                    optionalEnum(r.expireMode(), EXPIRE_MODES, "expire_mode"),
                    r.clearDay(),
                    normalizeStatus(r.status()),
                    now,
                    now));
            auditEventService.recordSuccess("customer.points_rule.created", MANAGE, BUSINESS_TYPE, saved.id(),
                    null, null, Map.of("project_id", projectId));
            return saved;
        } catch (ApiException e) {
            auditEventService.recordFailure("customer.points_rule.create_failed", MANAGE, BUSINESS_TYPE, "unknown",
                    null, null, e.code());
            throw e;
        }
    }

    @Transactional
    public CustomerPointsRuleRecord update(String id, CustomerPointsRuleRequest request) {
        authorizationService.requirePermission(MANAGE);
        try {
            CustomerPointsRuleRequest r = require(request);
            CustomerPointsRuleRecord existing = loadInScope(id);
            CustomerPointsRuleRecord saved = repository.savePointsRule(new CustomerPointsRuleRecord(
                    existing.id(),
                    existing.projectId(),
                    r.ruleName() == null ? existing.ruleName() : r.ruleName(),
                    r.earnMethod() == null ? existing.earnMethod() : requireEnum(r.earnMethod(), EARN_METHODS, "earn_method"),
                    r.earnRatio() == null ? existing.earnRatio() : requireNonNegative(r.earnRatio(), "earn_ratio"),
                    r.levelThreshold() == null ? existing.levelThreshold() : r.levelThreshold(),
                    r.validityType() == null ? existing.validityType() : requireEnum(r.validityType(), VALIDITY_TYPES, "validity_type"),
                    r.validityDays() == null ? existing.validityDays() : r.validityDays(),
                    r.expireMode() == null ? existing.expireMode() : optionalEnum(r.expireMode(), EXPIRE_MODES, "expire_mode"),
                    r.clearDay() == null ? existing.clearDay() : r.clearDay(),
                    r.status() == null ? existing.status() : normalizeStatus(r.status()),
                    existing.createdAt(),
                    Instant.now()));
            auditEventService.recordSuccess("customer.points_rule.updated", MANAGE, BUSINESS_TYPE, saved.id(),
                    null, null, Map.of());
            return saved;
        } catch (ApiException e) {
            auditEventService.recordFailure("customer.points_rule.update_failed", MANAGE, BUSINESS_TYPE, id,
                    null, null, e.code());
            throw e;
        }
    }

    /** 加载并强制租户 scope：项目级管理员访问他项目规则一律 404（越域按不存在处理）。 */
    private CustomerPointsRuleRecord loadInScope(String id) {
        CustomerPointsRuleRecord rule = repository.findPointsRuleById(requireText(id, "id"))
                .orElseThrow(() -> ApiException.notFound("POINTS_RULE_NOT_FOUND", "points rule not found"));
        authorizationService.currentProjectScopeQuietly().ifPresent(scope -> {
            if (!scope.equals(rule.projectId())) {
                throw ApiException.notFound("POINTS_RULE_NOT_FOUND", "points rule not found");
            }
        });
        return rule;
    }

    /** 写路径 project_id：项目级管理员强制绑定 token scope（覆盖请求体）；平台管理员须显式给。 */
    private String resolveWriteProjectId(String requestedProjectId) {
        Optional<String> scope = authorizationService.currentProjectScopeQuietly();
        if (scope.isPresent()) {
            return scope.get();
        }
        return requireText(requestedProjectId, "project_id");
    }

    private CustomerPointsRuleRequest require(CustomerPointsRuleRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private long requireNonNegative(Long value, String field) {
        if (value == null || value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " must be non-negative");
        }
        return value;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "active";
        }
        return requireEnum(status, STATUSES, "status");
    }

    /** 必填枚举校验：空或不在允许集合即 400（对齐 V060 DB CHECK，前置挡下避免裸约束违例）。 */
    private String requireEnum(String value, Set<String> allowed, String field) {
        String v = requireText(value, field);
        if (!allowed.contains(v)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    field + " must be one of " + allowed);
        }
        return v;
    }

    /** 选填枚举校验：null/空按 null 落库；非空须在允许集合内。 */
    private String optionalEnum(String value, Set<String> allowed, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireEnum(value, allowed, field);
    }
}
