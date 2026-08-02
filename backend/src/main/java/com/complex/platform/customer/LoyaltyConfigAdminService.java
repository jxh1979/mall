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
import static com.complex.platform.common.Strings.requireText;

/**
 * 会员忠诚度运营配置后台管理（会员忠诚度 PRD §3/§6）：会员等级档位/门槛/保级/倍率、连续签到阶梯、生日加倍规则。
 * 查看需 customer.loyalty.view，维护需 customer.loyalty.manage（high, V089）。
 * 底层库/仓储已就绪（{@link CustomerRepository#saveMemberLevel} 等 + V089 表与 CHECK 约束）；本服务只补齐配置写路径。
 *
 * <p>租户隔离与 {@link CustomerPointsRuleAdminService} 同口径：项目级管理员一律绑定其 token scope（覆盖请求体 project_id，
 * 写路径不信任请求体）；平台管理员须显式给 project_id。更新时校验目标记录 project_id 与解析出的 project_id 完全一致，
 * 防跨租户篡改（借 id 改他项目）。写路径 project_id 恒非空（token scope 或必填请求参数），故按 project 严格过滤成立。</p>
 *
 * <p>GET 列表仍仅 status='active'（+ 项目匹配/平台缺省），停用/归档项从列表隐去；但更新路径（POST 带 id）
 * 用状态无关的 list*ByProject 定位，故停用/归档项仍可按 id 恢复为 active（停用可逆，非单向门）。新增同键
 * （同 project 重复 level_code/streak_days/rule_name，含已停用/归档项）在服务层预检返回 409，兜底再由全局
 * DuplicateKeyException 处理器归一 409，杜绝裸 uq_* 约束违例落 500。</p>
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
            String levelCode = requireText(r.levelCode(), "level_code");
            if (isCreate(r.id())
                    && repository.listMemberLevelsByProject(projectId).stream()
                            .anyMatch(x -> levelCode.equals(x.levelCode()))) {
                throw ApiException.conflict("LOYALTY_CONFIG_DUPLICATE",
                        "该会员等级编码在本项目已存在");
            }
            Instant now = Instant.now();
            String id = resolveId(r.id(), "member_level_", projectId, this::findLevelInProject);
            CustomerMemberLevelRecord saved = repository.saveMemberLevel(new CustomerMemberLevelRecord(
                    id,
                    projectId,
                    levelCode,
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
            int streakDays = (int) requireMin(r.streakDays(), 1, "streak_days");
            if (isCreate(r.id())
                    && repository.listCheckInTiersByProject(projectId).stream()
                            .anyMatch(x -> x.streakDays() == streakDays)) {
                throw ApiException.conflict("LOYALTY_CONFIG_DUPLICATE",
                        "该连续签到档位在本项目已存在");
            }
            Instant now = Instant.now();
            String id = resolveId(r.id(), "check_in_tier_", projectId, this::findTierInProject);
            CustomerCheckInTierRecord saved = repository.saveCheckInTier(new CustomerCheckInTierRecord(
                    id,
                    projectId,
                    streakDays,
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
            String ruleName = requireText(r.ruleName(), "rule_name");
            if (isCreate(r.id())
                    && repository.listBirthdayRulesByProject(projectId).stream()
                            .anyMatch(x -> ruleName.equals(x.ruleName()))) {
                throw ApiException.conflict("LOYALTY_CONFIG_DUPLICATE",
                        "该生日规则名称在本项目已存在");
            }
            Instant now = Instant.now();
            String id = resolveId(r.id(), "birthday_rule_", projectId, this::findBirthdayInProject);
            CustomerBirthdayRuleRecord saved = repository.saveBirthdayRule(new CustomerBirthdayRuleRecord(
                    id,
                    projectId,
                    ruleName,
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

    /** id 空=新增；非空=更新。 */
    private boolean isCreate(String requestedId) {
        return requestedId == null || requestedId.isBlank();
    }

    /**
     * 解析写入 id：空=新增（生成）；非空=更新，须目标记录存在且归属同一 project_id（防借 id 篡改他项目）。
     * 用状态无关的 list*ByProject 定位，故 inactive/archived 记录仍可被更新（改回 active），停用不再是单向门。
     */
    private String resolveId(String requestedId, String prefix, String projectId,
                             java.util.function.BiPredicate<String, String> existsInProject) {
        if (requestedId == null || requestedId.isBlank()) {
            return prefix + UUID.randomUUID();
        }
        String id = requestedId.trim();
        if (!existsInProject.test(id, projectId)) {
            throw ApiException.notFound("LOYALTY_CONFIG_NOT_FOUND", "当前范围内未找到该配置记录");
        }
        return id;
    }

    private boolean findLevelInProject(String id, String projectId) {
        return repository.listMemberLevelsByProject(projectId).stream()
                .anyMatch(x -> id.equals(x.id()) && projectId.equals(x.projectId()));
    }

    private boolean findTierInProject(String id, String projectId) {
        return repository.listCheckInTiersByProject(projectId).stream()
                .anyMatch(x -> id.equals(x.id()) && projectId.equals(x.projectId()));
    }

    private boolean findBirthdayInProject(String id, String projectId) {
        return repository.listBirthdayRulesByProject(projectId).stream()
                .anyMatch(x -> id.equals(x.id()) && projectId.equals(x.projectId()));
    }

    private static <T> PageResponse<T> page(List<T> items) {
        return new PageResponse<>(items, 1, Math.max(items.size(), 1), items.size());
    }

    private <T> T require(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private long requireMin(Integer value, int min, String field) {
        if (value == null || value < min) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 必须大于等于 " + min);
        }
        return value;
    }

    private long requireMinLong(Long value, long min, String field) {
        if (value == null || value < min) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 必须大于等于 " + min);
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
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 必须为以下之一：" + allowed);
        }
        return v;
    }
}
