package com.complex.platform.customer;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.customer.dto.BirthdayRuleConfigRequest;
import com.complex.platform.customer.dto.CheckInTierConfigRequest;
import com.complex.platform.customer.dto.MemberLevelConfigRequest;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会员忠诚度运营配置后台管理（会员忠诚度 PRD §3/§6）：会员等级档位/门槛/保级/倍率、连续签到阶梯、生日加倍规则。
 * 查看需 customer.loyalty.view，维护需 customer.loyalty.manage（high, V089）。
 * 底层库/仓储已就绪（{@link CustomerRepository#saveMemberLevel} 等 + V089 表与 CHECK 约束）；本服务只补齐配置写路径。
 *
 * <p>租户隔离与 {@link CustomerPointsRuleAdminService} 同口径：项目级管理员一律绑定其 token scope（覆盖请求体 project_id，
 * 写路径不信任请求体）；平台管理员须显式给 project_id。更新时校验目标记录 project_id 与解析出的 project_id 完全一致，
 * 防跨租户篡改（借 id 改他项目）。</p>
 *
 * <p>ponytail: 列表复用既有 list*（仅 status='active' + 项目匹配/平台缺省），故 status 置为非 active 后会从列表隐去，
 * v1 不做全状态列表（如需再改活跃项即重建同 levelCode）。</p>
 */
@Service
public class LoyaltyConfigAdminService {

    private static final String VIEW = "customer.loyalty.view";
    private static final String MANAGE = "customer.loyalty.manage";
    private static final String BT_LEVEL = "customer_member_level";
    private static final String BT_TIER = "customer_check_in_tier";
    private static final String BT_BIRTHDAY = "customer_birthday_rule";

    // 允许值对齐 V089 DB CHECK，服务层前置挡下非法枚举返回 400，避免裸 DB 约束违例 500。
    private static final Set<String> LEVEL_STATUSES = Set.of("active", "inactive", "archived");
    private static final Set<String> TIER_STATUSES = Set.of("active", "inactive");
    private static final Set<String> BIRTHDAY_STATUSES = Set.of("active", "inactive");
    private static final Set<String> BIRTHDAY_WINDOWS = Set.of("birthday_day", "birthday_month");

    private final CustomerRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;

    public LoyaltyConfigAdminService(
            CustomerRepository repository,
            AuthorizationService authorizationService,
            AuditEventService auditEventService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
    }

    // ---- 会员等级 ----

    public PageResponse<CustomerMemberLevelRecord> listMemberLevels(String requestedProjectId) {
        authorizationService.requirePermission(VIEW);
        List<CustomerMemberLevelRecord> items = repository.listMemberLevels(resolveReadProjectId(requestedProjectId));
        auditEventService.recordSuccess("customer.member_level.listed", VIEW, BT_LEVEL, "list",
                null, null, Map.of("count", items.size()));
        return page(items);
    }

    @Transactional
    public CustomerMemberLevelRecord saveMemberLevel(MemberLevelConfigRequest request) {
        authorizationService.requirePermission(MANAGE);
        try {
            MemberLevelConfigRequest r = require(request);
            String projectId = resolveWriteProjectId(r.projectId());
            Instant now = Instant.now();
            String id = resolveId(r.id(), "member_level_", projectId, this::findLevelInProject);
            CustomerMemberLevelRecord saved = repository.saveMemberLevel(new CustomerMemberLevelRecord(
                    id,
                    projectId,
                    requireText(r.levelCode(), "level_code"),
                    requireText(r.levelName(), "level_name"),
                    (int) requireMin(r.levelRank(), 0, "level_rank"),
                    requireMinLong(r.upgradeThreshold(), 0, "upgrade_threshold"),
                    requireMinLong(r.keepThreshold(), 0, "keep_threshold"),
                    (int) requireMin(r.keepPeriodDays(), 1, "keep_period_days"),
                    (int) requireMin(r.graceDays(), 0, "grace_days"),
                    (int) requireMin(r.pointMultiplierBasisPoints(), 1, "point_multiplier_basis_points"),
                    normalizeStatus(r.status(), LEVEL_STATUSES),
                    now,
                    now));
            auditEventService.recordSuccess("customer.member_level.saved", MANAGE, BT_LEVEL, saved.id(),
                    null, null, Map.of("project_id", projectId, "level_code", saved.levelCode()));
            return saved;
        } catch (ApiException e) {
            auditEventService.recordFailure("customer.member_level.save_failed", MANAGE, BT_LEVEL,
                    request == null ? "unknown" : String.valueOf(request.id()), null, null, e.code());
            throw e;
        }
    }

    // ---- 连续签到阶梯 ----

    public PageResponse<CustomerCheckInTierRecord> listCheckInTiers(String requestedProjectId) {
        authorizationService.requirePermission(VIEW);
        List<CustomerCheckInTierRecord> items = repository.listCheckInTiers(resolveReadProjectId(requestedProjectId));
        auditEventService.recordSuccess("customer.check_in_tier.listed", VIEW, BT_TIER, "list",
                null, null, Map.of("count", items.size()));
        return page(items);
    }

    @Transactional
    public CustomerCheckInTierRecord saveCheckInTier(CheckInTierConfigRequest request) {
        authorizationService.requirePermission(MANAGE);
        try {
            CheckInTierConfigRequest r = require(request);
            String projectId = resolveWriteProjectId(r.projectId());
            Instant now = Instant.now();
            String id = resolveId(r.id(), "check_in_tier_", projectId, this::findTierInProject);
            CustomerCheckInTierRecord saved = repository.saveCheckInTier(new CustomerCheckInTierRecord(
                    id,
                    projectId,
                    (int) requireMin(r.streakDays(), 1, "streak_days"),
                    requireMinLong(r.awardPoints(), 0, "award_points"),
                    normalizeStatus(r.status(), TIER_STATUSES),
                    now,
                    now));
            auditEventService.recordSuccess("customer.check_in_tier.saved", MANAGE, BT_TIER, saved.id(),
                    null, null, Map.of("project_id", projectId, "streak_days", saved.streakDays()));
            return saved;
        } catch (ApiException e) {
            auditEventService.recordFailure("customer.check_in_tier.save_failed", MANAGE, BT_TIER,
                    request == null ? "unknown" : String.valueOf(request.id()), null, null, e.code());
            throw e;
        }
    }

    // ---- 生日加倍规则 ----

    public PageResponse<CustomerBirthdayRuleRecord> listBirthdayRules(String requestedProjectId) {
        authorizationService.requirePermission(VIEW);
        List<CustomerBirthdayRuleRecord> items = repository.listActiveBirthdayRules(resolveReadProjectId(requestedProjectId));
        auditEventService.recordSuccess("customer.birthday_rule.listed", VIEW, BT_BIRTHDAY, "list",
                null, null, Map.of("count", items.size()));
        return page(items);
    }

    @Transactional
    public CustomerBirthdayRuleRecord saveBirthdayRule(BirthdayRuleConfigRequest request) {
        authorizationService.requirePermission(MANAGE);
        try {
            BirthdayRuleConfigRequest r = require(request);
            String projectId = resolveWriteProjectId(r.projectId());
            Instant now = Instant.now();
            String id = resolveId(r.id(), "birthday_rule_", projectId, this::findBirthdayInProject);
            CustomerBirthdayRuleRecord saved = repository.saveBirthdayRule(new CustomerBirthdayRuleRecord(
                    id,
                    projectId,
                    requireText(r.ruleName(), "rule_name"),
                    requireEnum(r.windowType(), BIRTHDAY_WINDOWS, "window_type"),
                    (int) requireMin(r.multiplierBasisPoints(), 10001, "multiplier_basis_points"),
                    normalizeStatus(r.status(), BIRTHDAY_STATUSES),
                    now,
                    now));
            auditEventService.recordSuccess("customer.birthday_rule.saved", MANAGE, BT_BIRTHDAY, saved.id(),
                    null, null, Map.of("project_id", projectId, "window_type", saved.windowType()));
            return saved;
        } catch (ApiException e) {
            auditEventService.recordFailure("customer.birthday_rule.save_failed", MANAGE, BT_BIRTHDAY,
                    request == null ? "unknown" : String.valueOf(request.id()), null, null, e.code());
            throw e;
        }
    }

    // ---- 共用私有工具 ----

    /** 读路径 project_id：项目级管理员强制其 scope；平台管理员用请求参数（可空=看平台缺省行）。 */
    private String resolveReadProjectId(String requestedProjectId) {
        return authorizationService.currentProjectScopeQuietly().orElse(requestedProjectId);
    }

    /** 写路径 project_id：项目级管理员强制绑定 token scope（覆盖请求体）；平台管理员须显式给。 */
    private String resolveWriteProjectId(String requestedProjectId) {
        Optional<String> scope = authorizationService.currentProjectScopeQuietly();
        return scope.orElseGet(() -> requireText(requestedProjectId, "project_id"));
    }

    /**
     * 解析写入 id：空=新增（生成）；非空=更新，须目标记录存在且归属同一 project_id（防借 id 篡改他项目）。
     * ponytail: 复用 status='active' 列表定位，非 active 记录无法定位=视为不存在（v1 已在类注释说明）。
     */
    private String resolveId(String requestedId, String prefix, String projectId,
                             java.util.function.BiPredicate<String, String> existsInProject) {
        if (requestedId == null || requestedId.isBlank()) {
            return prefix + UUID.randomUUID();
        }
        String id = requestedId.trim();
        if (!existsInProject.test(id, projectId)) {
            throw ApiException.notFound("LOYALTY_CONFIG_NOT_FOUND", "config record not found in scope");
        }
        return id;
    }

    private boolean findLevelInProject(String id, String projectId) {
        return repository.listMemberLevels(projectId).stream()
                .anyMatch(x -> id.equals(x.id()) && projectId.equals(x.projectId()));
    }

    private boolean findTierInProject(String id, String projectId) {
        return repository.listCheckInTiers(projectId).stream()
                .anyMatch(x -> id.equals(x.id()) && projectId.equals(x.projectId()));
    }

    private boolean findBirthdayInProject(String id, String projectId) {
        return repository.listActiveBirthdayRules(projectId).stream()
                .anyMatch(x -> id.equals(x.id()) && projectId.equals(x.projectId()));
    }

    private static <T> PageResponse<T> page(List<T> items) {
        return new PageResponse<>(items, 1, Math.max(items.size(), 1), items.size());
    }

    private <T> T require(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return value.trim();
    }

    private long requireMin(Integer value, int min, String field) {
        if (value == null || value < min) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " must be >= " + min);
        }
        return value;
    }

    private long requireMinLong(Long value, long min, String field) {
        if (value == null || value < min) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " must be >= " + min);
        }
        return value;
    }

    private String normalizeStatus(String status, Set<String> allowed) {
        if (status == null || status.isBlank()) {
            return "active";
        }
        return requireEnum(status, allowed, "status");
    }

    private String requireEnum(String value, Set<String> allowed, String field) {
        String v = requireText(value, field);
        if (!allowed.contains(v)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " must be one of " + allowed);
        }
        return v;
    }
}
