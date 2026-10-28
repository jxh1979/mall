package com.complex.platform.customer;

// 文件版本：V0.03

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
 * 会员忠诚度运营配置后台管理（会员忠诚度 PRD §3/§6）：会员等级档位/门槛/倍率、连续签到阶梯、生日加倍规则。
 * 查看需 customer.loyalty.view，维护需 customer.loyalty.manage（high, V089）。
 * 底层库/仓储已就绪（{@link CustomerRepository#saveMemberLevel} 等 + V089 表与 CHECK 约束）；本服务只补齐配置写路径。
 *
 * <p>第十八批第 12 条：会员等级档位（BSM L820「客人在全国任一项目的消费均计入」、L821「全国平台统一规定」，ENUM L70/L72，
 * DEC-R3-2026-08-04-35）与生日加倍规则（BSM L831 / ENUM L87「全国平台统一规定，不按项目分别配置」，DEC-R8-97-13）全平台一套：
 * 不带 project_id，写入须有全国范围授权（平台管理员或勾了「可以查看全部项目」的角色，{@link AuthorizationService#allowsPlatformScope}）。</p>
 *
 * <p>连续签到阶梯仍按综合体：与 {@link CustomerPointsRuleAdminService} 同口径，项目级管理员一律绑定其 token scope（覆盖请求体 project_id，
 * 写路径不信任请求体）；平台管理员须显式给 project_id。更新时校验目标记录 project_id 与解析出的 project_id 完全一致，
 * 防跨租户篡改（借 id 改他项目）。</p>
 *
 * <p>GET 列表仍仅 status='active'，停用/归档项从列表隐去；但更新路径（POST 带 id）用状态无关的列表定位，
 * 故停用/归档项仍可按 id 恢复为 active（停用可逆，非单向门）。新增同键（重复 level_code / rule_name，同 project 重复 streak_days，
 * 含已停用/归档项）在服务层预检返回 409，兜底再由全局 DuplicateKeyException 处理器归一 409，杜绝裸 uq_* 约束违例落 500。</p>
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

    public PageResponse<CustomerMemberLevelRecord> listMemberLevels() {
        authorizationService.requirePermission(VIEW);
        List<CustomerMemberLevelRecord> items = repository.listMemberLevels();
        auditEventService.recordSuccess("customer.member_level.listed", VIEW, BT_LEVEL, "list",
                null, null, Map.of("count", items.size()));
        return page(items);
    }

    @Transactional
    public CustomerMemberLevelRecord saveMemberLevel(MemberLevelConfigRequest request) {
        authorizationService.requirePermission(MANAGE);
        try {
            requireNationwide("会员等级全平台统一，须有全国范围授权");
            MemberLevelConfigRequest r = require(request);
            String levelCode = requireText(r.levelCode(), "level_code");
            List<CustomerMemberLevelRecord> all = repository.listAllMemberLevels();
            if (isCreate(r.id()) && all.stream().anyMatch(x -> levelCode.equals(x.levelCode()))) {
                throw ApiException.conflict("LOYALTY_CONFIG_DUPLICATE", "该会员等级编码已存在");
            }
            Instant now = Instant.now();
            String id = resolveId(r.id(), "member_level_", x -> all.stream().anyMatch(level -> x.equals(level.id())));
            CustomerMemberLevelRecord saved = repository.saveMemberLevel(new CustomerMemberLevelRecord(
                    id,
                    levelCode,
                    requireText(r.levelName(), "level_name"),
                    (int) requireMin(r.levelRank(), 0, "level_rank"),
                    requireMinLong(r.upgradeThreshold(), 0, "upgrade_threshold"),
                    (int) requireMin(r.pointMultiplierBasisPoints(), 1, "point_multiplier_basis_points"),
                    normalizeStatus(r.status(), LEVEL_STATUSES),
                    now,
                    now));
            auditEventService.recordSuccess("customer.member_level.saved", MANAGE, BT_LEVEL, saved.id(),
                    null, null, Map.of("level_code", saved.levelCode()));
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
            String id = resolveId(r.id(), "check_in_tier_", x -> findTierInProject(x, projectId));
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

    public PageResponse<CustomerBirthdayRuleRecord> listBirthdayRules() {
        authorizationService.requirePermission(VIEW);
        List<CustomerBirthdayRuleRecord> items = repository.listActiveBirthdayRules();
        auditEventService.recordSuccess("customer.birthday_rule.listed", VIEW, BT_BIRTHDAY, "list",
                null, null, Map.of("count", items.size()));
        return page(items);
    }

    @Transactional
    public CustomerBirthdayRuleRecord saveBirthdayRule(BirthdayRuleConfigRequest request) {
        authorizationService.requirePermission(MANAGE);
        try {
            requireNationwide("生日加倍规则全平台统一，须有全国范围授权");
            BirthdayRuleConfigRequest r = require(request);
            String ruleName = requireText(r.ruleName(), "rule_name");
            List<CustomerBirthdayRuleRecord> all = repository.listAllBirthdayRules();
            if (isCreate(r.id()) && all.stream().anyMatch(x -> ruleName.equals(x.ruleName()))) {
                throw ApiException.conflict("LOYALTY_CONFIG_DUPLICATE", "该生日规则名称已存在");
            }
            Instant now = Instant.now();
            String id = resolveId(r.id(), "birthday_rule_", x -> all.stream().anyMatch(rule -> x.equals(rule.id())));
            CustomerBirthdayRuleRecord saved = repository.saveBirthdayRule(new CustomerBirthdayRuleRecord(
                    id,
                    ruleName,
                    requireEnum(r.windowType(), BIRTHDAY_WINDOWS, "window_type"),
                    (int) requireMin(r.multiplierBasisPoints(), 10001, "multiplier_basis_points"),
                    normalizeStatus(r.status(), BIRTHDAY_STATUSES),
                    now,
                    now));
            auditEventService.recordSuccess("customer.birthday_rule.saved", MANAGE, BT_BIRTHDAY, saved.id(),
                    null, null, Map.of("window_type", saved.windowType()));
            return saved;
        } catch (ApiException e) {
            auditEventService.recordFailure("customer.birthday_rule.save_failed", MANAGE, BT_BIRTHDAY,
                    request == null ? "unknown" : String.valueOf(request.id()), null, null, e.code());
            throw e;
        }
    }

    // ---- 共用私有工具 ----

    /**
     * 读路径 project_id：平台管理员用请求参数（可空=看平台缺省行）；其余带了且在 customer.loyalty.view 可触及范围内的用它
     * （18E（第 19 条 19-R2），治理地图第 659 行），否则本人项目；本人项目也不在该码可触及范围内的 403（见 reachableOrOwn）。
     */
    private String resolveReadProjectId(String requestedProjectId) {
        return authorizationService.currentProjectScopeQuietly()
                .map(own -> reachableOrOwn(requestedProjectId, VIEW, own))
                .orElse(requestedProjectId);
    }

    private String reachableOrOwn(String requestedProjectId, String code, String own) {
        String requested = requestedProjectId == null || requestedProjectId.isBlank() ? null : requestedProjectId.trim();
        var reach = authorizationService.projectReach(authorizationService.currentActor(), code);
        if (requested != null && !requested.equals(own) && reach.covers(requested)) {
            return requested;
        }
        // 18E 复核：该码只经勾选别的项目的角色持有时本人项目也碰不到（治理地图第 659 行「逐个勾选的只能查看被选中的项目」），不回退本人项目。
        if (!reach.covers(own)) {
            throw ApiException.forbidden("PROJECT_SCOPE_MISMATCH", "无权访问该项目的数据");
        }
        return own;
    }

    /** 全平台一套的配置（会员等级、生日加倍规则）：须有全国范围授权；只到本人项目或逐个勾选项目的账号不能写。 */
    private void requireNationwide(String message) {
        if (!authorizationService.allowsPlatformScope(authorizationService.currentActor(), MANAGE)) {
            throw ApiException.forbidden("FORBIDDEN", message);
        }
    }

    /** 写路径 project_id：平台管理员须显式给；其余带了且在 customer.loyalty.manage 可触及范围内的用它（18E（第 19 条 19-R2）），否则本人项目；本人项目也不可触及的 403。 */
    private String resolveWriteProjectId(String requestedProjectId) {
        Optional<String> scope = authorizationService.currentProjectScopeQuietly();
        return scope.map(own -> reachableOrOwn(requestedProjectId, MANAGE, own))
                .orElseGet(() -> requireText(requestedProjectId, "project_id"));
    }

    /** id 空=新增；非空=更新。 */
    private boolean isCreate(String requestedId) {
        return requestedId == null || requestedId.isBlank();
    }

    /**
     * 解析写入 id：空=新增（生成）；非空=更新，须目标记录在当前范围内存在（签到阶梯须归属同一 project_id，防借 id 篡改他项目）。
     * 用状态无关的列表定位，故 inactive/archived 记录仍可被更新（改回 active），停用不再是单向门。
     */
    private String resolveId(String requestedId, String prefix, java.util.function.Predicate<String> exists) {
        if (requestedId == null || requestedId.isBlank()) {
            return prefix + UUID.randomUUID();
        }
        String id = requestedId.trim();
        if (!exists.test(id)) {
            throw ApiException.notFound("LOYALTY_CONFIG_NOT_FOUND", "当前范围内未找到该配置记录");
        }
        return id;
    }

    private boolean findTierInProject(String id, String projectId) {
        return repository.listCheckInTiersByProject(projectId).stream()
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
