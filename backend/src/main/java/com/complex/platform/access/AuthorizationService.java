package com.complex.platform.access;

// 文件版本：V0.15

import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.CallerScopeResolver;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.complex.platform.auth.SessionIssuer;

@Service
@ConditionalOnBean(AccessRepository.class)
public class AuthorizationService implements CallerScopeResolver {

    // #14 钱包双路径：客人自助主体可自行执行的权限白名单（仅作用于本人账户，由控制器按客人 token 绑定）。
    // 仅限客人端 App/小程序自助场景：储值充值/确认、储值消费。退款/冻结等仍为员工专属。
    private static final java.util.Set<String> CUSTOMER_SELF_SERVICE_PERMISSIONS = java.util.Set.of(
            "customer.stored_value.topup",
            "customer.stored_value.consume");

    private final AccessRepository accessRepository;

    public AuthorizationService(AccessRepository accessRepository) {
        this.accessRepository = accessRepository;
    }

    @Transactional
    public void requirePermission(String permissionCode) {
        requirePermissions(permissionCode);
    }

    /** 同一动作先核验所有权限，再按来源消费一次；任何失败由事务回滚。 */
    @Transactional
    public void requirePermissions(String... permissionCodes) {
        AccessActor actor = currentActor();
        Instant checkedAt = Instant.now();
        List<AccessRepository.PermissionEffect> effects = accessRepository.findPermissionEffects(actor.id(), checkedAt).stream()
                .filter(effect -> isActive(effect, checkedAt)).toList();
        java.util.Map<String, AccessRepository.PermissionEffect> consume = new java.util.LinkedHashMap<>();
        java.util.function.Supplier<java.util.Set<String>> daily = dailyPermissions();
        java.util.function.Supplier<java.util.function.Predicate<AccessRepository.PermissionEffect>> positionDerived =
                positionDerivedSources(actor.id());
        for (String code : new java.util.LinkedHashSet<>(java.util.Arrays.asList(permissionCodes))) {
            AccessRepository.PermissionEffect grant = permissionGrant(actor, code, effects, daily, positionDerived);
            if (grant != null && grant.singleUse()) {
                consume.putIfAbsent(grant.source() + ":" + grant.sourceId(), grant);
            }
        }
        // 岗位派生事实在上面的循环里才按操作人 id 读取：核对放在最后一次读取之后、单次来源消费之前。
        requireIdentityUnchanged(actor);
        for (AccessRepository.PermissionEffect grant : consume.values()) {
            if (!accessRepository.markSingleUsePermissionConsumed(
                    grant.sourceId(), grant.permissionCode(), actor.id(), checkedAt)) {
                deny(grant.permissionCode());
            }
        }
    }

    private AccessRepository.PermissionEffect permissionGrant(
            AccessActor actor, String code, List<AccessRepository.PermissionEffect> effects) {
        return permissionGrant(actor, code, effects, dailyPermissions(), positionDerivedSources(actor.id()));
    }

    private AccessRepository.PermissionEffect permissionGrant(
            AccessActor actor, String code, List<AccessRepository.PermissionEffect> effects,
            java.util.function.Supplier<java.util.Set<String>> daily,
            java.util.function.Supplier<java.util.function.Predicate<AccessRepository.PermissionEffect>> positionDerived) {
        if (!"active".equals(actor.status()) || !knownPermission(code)) {
            deny(code);
        }
        List<AccessRepository.PermissionEffect> matching = effects.stream()
                .filter(effect -> code.equals(effect.permissionCode())).toList();
        // M002-F33：员工显式拒绝压过全部来源；岗位高危挂起只压住岗位派生的那一份授予（见 positionDerivedSources）
        if (matching.stream().anyMatch(AuthorizationService::isExplicitDeny)) {
            deny(code);
        }
        boolean held = matching.stream().anyMatch(AuthorizationService::isPositionHold);
        if (isMasterAuthority(actor)
                || (isPlatformAdminBridge(actor) && daily.get().contains(code))
                || (isCustomerSelfServiceBridge(actor) && CUSTOMER_SELF_SERVICE_PERMISSIONS.contains(code))) {
            return null;
        }
        AccessRepository.PermissionEffect grant = matching.stream()
                .filter(effect -> effect.effect() == AccessRepository.Effect.GRANT)
                .filter(effect -> isExplicitOrDailyGrant(effect, daily))
                .filter(effect -> !held || !positionDerived.get().test(effect))
                .min(Comparator.comparing(AccessRepository.PermissionEffect::sourceId,
                        Comparator.nullsLast(String::compareTo))).orElse(null);
        if (grant == null) {
            deny(code);
        }
        return grant;
    }

    /**
     * 日常权限码集合（active、无需审批、low/medium）。M006-035：一次授权判定或有效集计算内只读一次权限字典，
     * 由调用方持有本 Supplier 复用；首次 get() 才查库，普通员工路径不触发。
     */
    private java.util.function.Supplier<java.util.Set<String>> dailyPermissions() {
        return new java.util.function.Supplier<>() {
            private java.util.Set<String> cached;

            @Override
            public java.util.Set<String> get() {
                if (cached == null) {
                    cached = accessRepository.listPermissions().stream()
                            .filter(p -> "active".equals(p.status()) && !p.requiresApproval()
                                    && java.util.Set.of("low", "medium").contains(normalize(p.riskLevel())))
                            .map(PermissionDefinition::permissionCode)
                            .collect(java.util.stream.Collectors.toSet());
                }
                return cached;
            }
        };
    }

    /**
     * M006-I9-01：HTML 指派给平台管理员的权限治理本职（PGM-1.10.1 角色装配「平台管理员配置各岗位权限模板，并定职位/层级对应哪个模板」；
     * BSM-1.3.4「拥有系统里全部日常功能权限…不会出现"管理员反而没权限"」）。这些码本身是「提权」类（授给他人须审批），
     * 但管理员自己做本职不受授出审批限制：随默认管理员角色（role_platform_admin）生效。资金 / 敏感数据 / 删除 / 最终业务审批
     * （如 access.grant_request.review）不在此列，仍须逐项授予并留痕。
     */
    static final java.util.Set<String> PLATFORM_ADMIN_GOVERNANCE_PERMISSIONS = java.util.Set.of(
            "access.role.assign", "access.role.manage",
            "access.permission_template.manage", "access.permission_template.apply", "access.permission_template.revoke",
            "access.staff_level.manage", "access.user.manage",
            "access.temporary_grant.create", "access.temporary_grant.revoke");

    private static boolean isExplicitDeny(AccessRepository.PermissionEffect effect) {
        return effect.effect() == AccessRepository.Effect.DENY && effect.source() != AccessRepository.Source.POSITION_HOLD;
    }

    private static boolean isPositionHold(AccessRepository.PermissionEffect effect) {
        return effect.effect() == AccessRepository.Effect.DENY && effect.source() == AccessRepository.Source.POSITION_HOLD;
    }

    /**
     * M002-F33（依据：治理地图第 655 行「最终权限 = 角色权限 + 基础模板 + 有效临时模板 + 员工单独授予 − 显式拒绝；显式拒绝优先级最高」；
     * 以下为技术推导）：岗位高危挂起不是显式拒绝，只压住岗位派生的那一份授予——员工岗位行记下的岗位派生角色（未另行直接绑定）与岗位派生
     * 模板套用。员工单独授予、直接角色绑定、非岗位模板套用与临时授权照常生效。首次用到才查库（只在该码有挂起时），一次判定内复用。
     */
    private java.util.function.Supplier<java.util.function.Predicate<AccessRepository.PermissionEffect>> positionDerivedSources(String userId) {
        java.util.concurrent.atomic.AtomicReference<java.util.function.Predicate<AccessRepository.PermissionEffect>> memo =
                new java.util.concurrent.atomic.AtomicReference<>();
        return () -> {
            if (memo.get() == null) {
                String positionRole = accessRepository.findAssignedPositionRoleId(userId).orElse(null);
                java.util.Set<String> positionApplications = accessRepository.findPositionApplications(userId).stream()
                        .map(PermissionTemplateApplication::id).collect(java.util.stream.Collectors.toSet());
                java.util.Set<String> directRoles = positionRole == null ? java.util.Set.of() : directRoleIds(userId);
                memo.set(effect -> switch (effect.source()) {
                    case ROLE -> effect.sourceId() != null && effect.sourceId().equals(positionRole)
                            && !directRoles.contains(effect.sourceId());
                    case TEMPLATE_APPLICATION -> positionApplications.contains(effect.sourceId());
                    default -> false;
                });
            }
            return memo.get();
        };
    }

    // 不支持直接角色查询的仓储按「没有另行直接绑定」处理（挂起照常压住岗位派生角色）。
    private java.util.Set<String> directRoleIds(String userId) {
        try {
            return new java.util.HashSet<>(accessRepository.findUserRoleIds(userId));
        } catch (UnsupportedOperationException unsupported) {
            return java.util.Set.of();
        }
    }

    private boolean isExplicitOrDailyGrant(AccessRepository.PermissionEffect effect,
                                           java.util.function.Supplier<java.util.Set<String>> daily) {
        // 默认管理员角色随权限字典播种，不能把其高危条目当成逐项授权；治理本职码除外（M006-I9-01）。
        return effect.source() != AccessRepository.Source.ROLE
                || !"role_platform_admin".equals(effect.sourceId())
                || daily.get().contains(effect.permissionCode())
                || PLATFORM_ADMIN_GOVERNANCE_PERMISSIONS.contains(effect.permissionCode());
    }

    public boolean isMasterAuthority(AccessActor actor) {
        return "active".equals(actor.status())
                && SessionIssuer.MASTER_ACCOUNT_TYPE.equals(normalize(actor.accountType()))
                && SessionIssuer.MASTER_USER_ID.equals(actor.id())
                && SessionIssuer.MASTER_USERNAME.equals(actor.username());
    }

    /**
     * 当前 actor 自身的有效权限集（self 端，只读）。不消费单次授权——
     * 单次授权仅在真正执行动作的 {@link #requirePermission} 中消费，避免"看一眼菜单就烧掉"。
     */
    public EffectivePermissions effectivePermissions() {
        AccessActor actor = currentActor();
        EffectivePermissions result = computeEffectivePermissions(actor.id(), isPlatformAdminBridge(actor), isMasterAuthority(actor));
        requireIdentityUnchanged(actor);
        return result;
    }

    /** 指定用户的有效权限集（平台管理用；调用方须先校验 {@code access.user.view}）。 */
    public EffectivePermissions effectivePermissionsForUser(String userId) {
        AccessActor actor = currentActor();
        AccessUser user = accessRepository.findUserById(userId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "账号不存在"));
        if (!isPlatformAdmin(actor) && !isMasterAuthority(actor)) {
            String organizationId = requireProjectScope(actor);
            if (!organizationId.equals(user.organizationId())) {
                throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "无权查询其他组织的用户");
            }
            if (actor.merchantId() != null && !actor.merchantId().isBlank()
                    && !actor.merchantId().equals(user.merchantId())) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权查询其他商户的用户");
            }
        }
        AccessActor subject = new AccessActor(user.id(), user.username(), user.accountType(), user.organizationId(),
                user.merchantId(), user.status());
        return computeEffectivePermissions(userId, isPlatformAdminBridge(subject), isMasterAuthority(subject));
    }

    private EffectivePermissions computeEffectivePermissions(String userId, boolean platformAdmin, boolean master) {
        Instant checkedAt = Instant.now();
        java.util.function.Supplier<java.util.Set<String>> daily = dailyPermissions();
        List<AccessRepository.PermissionEffect> active = accessRepository.findPermissionEffects(userId, checkedAt).stream()
                .filter(effect -> isActive(effect, checkedAt))
                .toList();
        // M002-F33：员工显式拒绝压过全部来源；岗位高危挂起只压住岗位派生的那一份授予，与 permissionGrant 同口径。
        java.util.Set<String> explicitDenies = active.stream().filter(AuthorizationService::isExplicitDeny)
                .map(AccessRepository.PermissionEffect::permissionCode).collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> held = active.stream().filter(AuthorizationService::isPositionHold)
                .map(AccessRepository.PermissionEffect::permissionCode).collect(java.util.stream.Collectors.toSet());
        java.util.function.Supplier<java.util.function.Predicate<AccessRepository.PermissionEffect>> positionDerived =
                positionDerivedSources(userId);
        // 实现 A：grants 预扣显式拒绝（DENY 优先）与被挂起压住的岗位派生授予。
        List<String> grants = active.stream()
                .filter(effect -> effect.effect() == AccessRepository.Effect.GRANT)
                .filter(effect -> isExplicitOrDailyGrant(effect, daily))
                .filter(effect -> !held.contains(effect.permissionCode()) || !positionDerived.get().test(effect))
                .map(AccessRepository.PermissionEffect::permissionCode)
                .filter(code -> !explicitDenies.contains(code))
                .distinct()
                .sorted()
                .toList();
        java.util.Set<String> effective = new java.util.TreeSet<>(grants);
        // M002-F03（R-001「有效集并入全部权限字典，显式拒绝仍优先」）：最高权限并入全部 active 码，与 permissionGrant 对最高权限的放行同口径；
        // 平台管理员只并入日常码。
        if (platformAdmin || master) {
            accessRepository.listPermissions().stream()
                    .filter(p -> master || daily.get().contains(p.permissionCode()))
                    .map(PermissionDefinition::permissionCode).forEach(effective::add);
        }
        effective.removeAll(explicitDenies);
        // denies 仍单独返回供前端展示（前端以 denies 压过 grants）：员工显式拒绝，加上被挂起压住、又没有其他来源的码。
        List<String> denies = java.util.stream.Stream.concat(explicitDenies.stream(),
                        held.stream().filter(code -> !effective.contains(code)))
                .distinct().sorted().toList();
        return new EffectivePermissions(userId, List.copyOf(effective), denies, false, checkedAt);
    }

    public AccessActor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw ApiException.unauthorized("UNAUTHENTICATED", "需要登录后操作");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AccessActor actor) {
            return actor;
        }
        if (principal instanceof AuthService.UserRecord user) {
            return new AccessActor(
                    user.id(),
                    user.username(),
                    user.accountType(),
                    user.organizationId(),
                    user.merchantId(),
                    user.status()
            );
        }
        throw ApiException.unauthorized("UNAUTHENTICATED", "无法识别的认证主体");
    }

    // XM-M001-AF（治理地图第 653 行项目与商户隔离、第 658 行跨项目或换商户后旧范围派生权限立即失效；技术推导）：
    // 先按 id 读能力事实（有效权限、职级）、后核身份——会话身份与员工表当前行不一致（在途调岗、换商户、停用）即按不具备处理。
    // 新范围权限与职级必在换项目提交之后才落地，故读到新能力时必能读到新归属；不加锁、不需同一快照。
    private void requireIdentityUnchanged(AccessActor actor) {
        if (accessRepository == null) {
            return; // 与 canResolveRoles() 同口径：无仓储的轻量上下文
        }
        Optional<AccessUser> row;
        try {
            row = accessRepository.findUserById(actor.id());
        } catch (UnsupportedOperationException unsupported) {
            return; // 与 directRoleIds 同口径：不支持按号读员工的仓储
        }
        if (row.isEmpty()) {
            return; // 主体不来自员工表（客人、调度主体）：没有可被换项目/停用改写的员工事实
        }
        AccessUser user = row.get();
        if (!java.util.Objects.equals(actor.organizationId(), user.organizationId())
                || !java.util.Objects.equals(actor.merchantId(), user.merchantId())
                || !java.util.Objects.equals(actor.accountType(), user.accountType())
                || !java.util.Objects.equals(actor.status(), user.status())) {
            throw ApiException.forbidden("PERMISSION_DENIED", "账号归属或状态已变更，本次操作未执行");
        }
    }

    private void requireIdentityUnchangedIfSelf(String userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return;
        }
        AccessActor self;
        try {
            self = currentActor();
        } catch (ApiException unrecognized) {
            return; // 非员工主体（如 MockMvc user()），保持原行为
        }
        if (userId.equals(self.id())) {
            requireIdentityUnchanged(self);
        }
    }

    /** 已登录请求按操作人与组织隔离，带商户归属的再加商户；安全上下文无认证时沿用原范围（空）。 */
    @Override
    public String resolve() {
        String legacy = legacyScope();
        if (legacy.isEmpty()) {
            return legacy; // 无认证：原样 ""
        }
        AccessActor actor = currentActor();
        if (actor.id() == null || actor.id().isBlank()) {
            throw ApiException.unauthorized("UNAUTHENTICATED", "无法识别的认证主体");
        }
        String id = actor.id();
        String org = java.util.Objects.toString(actor.organizationId(), "");
        if ("p".equals(legacy)) {
            return "u:" + id.length() + ":" + id + ":" + org; // 非商户主体：与 #415 形态逐字节相同
        }
        // XM-M002-BH（治理地图第 303、528、653 行；技术推导）：带商户归属的主体同样按人隔离，不再与同商户他人共用幂等记录。
        return "um:" + id.length() + ":" + id + ":" + org.length() + ":" + org + ":" + actor.merchantId();
    }

    @Override
    public String legacyScope() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "";
        }
        AccessActor actor = currentActor();
        String merchantId = actor.merchantId();
        return merchantId == null || merchantId.isBlank() ? "p" : "m:" + merchantId;
    }

    private boolean knownPermission(String permissionCode) {
        try {
            return accessRepository.permissionExists(permissionCode);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** 当前主体是否持有某权限(布尔·不抛错):用于按权限脱敏字段(如内部结算价)。 */
    public boolean currentActorHasPermission(String permissionCode) {
        try {
            Instant checkedAt = Instant.now();
            AccessActor actor = currentActor();
            List<AccessRepository.PermissionEffect> effects = accessRepository.findPermissionEffects(actor.id(), checkedAt).stream()
                    .filter(effect -> isActive(effect, checkedAt)).toList();
            permissionGrant(actor, permissionCode, effects);
            requireIdentityUnchanged(actor); // 在最后一次按操作人 id 读取（岗位派生事实）之后
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean isPlatformAdmin(AccessActor actor) {
        return isPlatformAdminBridge(actor);
    }

    public boolean isOutsourcedVendor(AccessActor actor) {
        return actor != null
                && "active".equals(actor.status())
                && "outsourced_vendor".equals(normalize(actor.accountType()));
    }

    public String requireProjectScope(AccessActor actor) {
        String projectId = actor == null ? null : actor.organizationId();
        if (projectId == null || projectId.isBlank()) {
            throw ApiException.forbidden("PROJECT_SCOPE_REQUIRED", "此操作必须在项目范围内进行");
        }
        return projectId;
    }

    public void enforceOutsourcedAssigneeScope(AccessActor actor, String assigneeId, String projectId) {
        if (!isOutsourcedVendor(actor)) {
            return;
        }
        String scopeProjectId = requireProjectScope(actor);
        if (!actor.id().equals(assigneeId) || !scopeProjectId.equals(projectId)) {
            throw ApiException.forbidden("FORBIDDEN", "所分配的资源不在当前权限范围内");
        }
    }

    /** 审批身份须由明确角色权限及实际管理职级共同证明；USB 主授权独立识别。 */
    public int currentSupervisorLevel() {
        AccessActor actor = currentActor();
        if (isMasterAuthority(actor)) {
            return 3;
        }
        if (!"active".equals(actor.status())
                || !holdsPermissionViaRole(actor.id(), "governance.supervisor_review.approve")) {
            return 0;
        }
        int rank = currentActorLevelRank();
        int derived = rank > 0 ? (rank + 2) / 3 : 0;
        // 最高权限（第 3 级）只给持密钥主体；职级只映射到上一级/上两级。
        return Math.min(derived, 2);
    }

    /**
     * Returns the current caller's management level_rank (1..30, see staff_levels), or 0 if unknown.
     * Degrades to 0 on null repository or any lookup error so callers can fall back to level-based checks.
     */
    public int currentActorLevelRank() {
        try {
            AccessActor actor = currentActor();
            int rank = isMasterAuthority(actor) ? 30 : levelRankOf(actor.id());
            requireIdentityUnchanged(actor);
            return rank;
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    /** Returns the management level_rank (1..30) of the given user, or 0 if no staff level is assigned/unknown. */
    public int levelRankOf(String userId) {
        if (userId == null || accessRepository == null) {
            return 0;
        }
        try {
            return accessRepository.findStaffLevelAssignment(userId)
                    .flatMap(assignment -> accessRepository.findStaffLevelById(assignment.staffLevelId()))
                    .map(StaffLevelRecord::levelRank)
                    .orElse(0);
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    /**
     * 是否具备角色/隔离键解析能力（仓储可用）。双轨收口的 specific_role 角色闸与同商户范围闸仅在可解析时强制，
     * 在无仓储的轻量上下文（如部分单测/未接入环境）下回退既有 level 闸，避免误伤合法流程。
     */
    public boolean canResolveRoles() {
        return accessRepository != null;
    }

    /**
     * 角色解析（双轨收口 specific_role，platform-governance §24.3）：判断指定用户是否通过其角色
     * （ROLE 来源，非临时授权/模板单发）持有某权限码，从而解析"指定角色"审批闸而非旁路。
     *
     * <p>语义对齐 {@link #computeEffectivePermissions}：DENY 优先（命中 DENY 即不持有）；GRANT 仅当来源为
     * {@link AccessRepository.Source#ROLE} 且在有效期内才算"角色赋予"。不因平台管理员身份补齐审批角色。
     * 仓储为空或查询异常时安全保守地返回 {@code false}（不放行）。
     */
    public boolean holdsPermissionViaRole(String userId, String permissionCode) {
        if (userId == null || permissionCode == null || accessRepository == null) {
            return false;
        }
        try {
            Instant checkedAt = Instant.now();
            List<AccessRepository.PermissionEffect> active = accessRepository.findPermissionEffects(userId, checkedAt).stream()
                    .filter(effect -> permissionCode.equals(effect.permissionCode()))
                    .filter(effect -> isActive(effect, checkedAt))
                    .toList();
            if (active.stream().anyMatch(AuthorizationService::isExplicitDeny)) {
                return false;
            }
            boolean held = active.stream().anyMatch(AuthorizationService::isPositionHold);
            java.util.function.Supplier<java.util.function.Predicate<AccessRepository.PermissionEffect>> positionDerived =
                    positionDerivedSources(userId);
            java.util.function.Supplier<java.util.Set<String>> daily = dailyPermissions();
            boolean holds = active.stream()
                    .anyMatch(effect -> effect.effect() == AccessRepository.Effect.GRANT
                            && effect.source() == AccessRepository.Source.ROLE
                            && isExplicitOrDailyGrant(effect, daily)
                            && (!held || !positionDerived.get().test(effect)));
            requireIdentityUnchangedIfSelf(userId); // 在最后一次按该员工 id 读取（岗位派生事实）之后
            return holds;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * 返回指定用户的隔离键 merchant_id（用于同商户范围闸），无则空。绝不抛错（查询异常→空）。
     * 与 {@link #currentMerchantIdQuietly()} 对称，但面向"任意用户"（如审批记录的发起人）。
     */
    /** M007-R6-02：指定用户所属项目（organization_id），用于待复核清单的项目隔离；无则空，绝不抛错。 */
    public Optional<String> organizationIdOf(String userId) {
        if (userId == null || accessRepository == null) {
            return Optional.empty();
        }
        try {
            return accessRepository.findUserById(userId).map(AccessUser::organizationId).filter(id -> id != null && !id.isBlank());
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public Optional<String> merchantIdOf(String userId) {
        if (userId == null || accessRepository == null) {
            return Optional.empty();
        }
        try {
            return accessRepository.findUserById(userId)
                    .map(AccessUser::merchantId)
                    .filter(merchantId -> merchantId != null && !merchantId.isBlank());
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    /**
     * Returns the merchant scope of the current caller.
     * Platform admins get Optional.empty() (no filter, sees everything; D-032：红线④不约束平台管理员).
     * Merchant users get Optional.of(merchantId).
     * Other callers without a merchantId throw MERCHANT_SCOPE_REQUIRED.
     */
    public Optional<String> currentMerchantScope() {
        AccessActor actor = currentActor();
        if (isPlatformAdminBridge(actor)) {
            return Optional.empty();
        }
        String merchantId = actor.merchantId();
        if (merchantId != null && !merchantId.isBlank()) {
            return Optional.of(merchantId);
        }
        throw ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行");
    }

    /**
     * Returns the current caller's merchant id if present, otherwise empty.
     * Unlike {@link #currentMerchantScope()}, this never throws: platform admins and callers without a merchant
     * yield {@code Optional.empty()}. Degrades to empty on any error.
     * 平台管理员即便被种子误绑 merchant_id，也按平台视角看全量（D-032）。
     */
    public Optional<String> currentMerchantIdQuietly() {
        try {
            AccessActor actor = currentActor();
            if (isPlatformAdminBridge(actor)) {
                return Optional.empty();
            }
            String merchantId = actor.merchantId();
            if (merchantId != null && !merchantId.isBlank()) {
                return Optional.of(merchantId);
            }
            return Optional.empty();
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    /**
     * 平台合规口径守卫：企微会话/留痕/风控等平台级合规数据按 projectId 归属，不按 merchant 切片。
     * 此方法 fail-closed 拒绝任何 merchant-scoped 调用方（持 merchantId 的商户用户），
     * 把"治理层确保 wecom.* 不授商户角色"落到代码层防御——即使权限被错误下放给商户角色，
     * 商户用户也无法越权调阅他项目的合规会话。平台管理员与无 merchantId 的平台合规员放行。
     */
    public void requirePlatformComplianceScope() {
        AccessActor actor = currentActor();
        if (isPlatformAdminBridge(actor)) {
            return;
        }
        if (isOutsourcedVendor(actor)) {
            throw ApiException.forbidden(
                    "PLATFORM_COMPLIANCE_SCOPE_REQUIRED",
                    "外包供应商无权查看平台合规数据");
        }
        String merchantId = actor.merchantId();
        if (merchantId != null && !merchantId.isBlank()) {
            throw ApiException.forbidden(
                    "PLATFORM_COMPLIANCE_SCOPE_REQUIRED",
                    "商户账号无权查看平台合规数据");
        }
    }

    /**
     * 平台口径守卫：平台级聚合分析（无 merchant 维度的指标定义/风控规则/风险告警/员工评分/机器人/
     * AI 质检/客户意图等）不向商户切片，fail-closed 拒绝 merchant-scoped 调用方，
     * 防"权限被下放给商户角色即可拉取全平台分析数据"的越权读。平台管理员/无 merchantId 平台员放行。
     */
    public void requirePlatformScope() {
        AccessActor actor = currentActor();
        if (isPlatformAdminBridge(actor)) {
            return;
        }
        if (isOutsourcedVendor(actor)) {
            throw ApiException.forbidden(
                    "PLATFORM_SCOPE_REQUIRED",
                    "外包供应商无权查看平台级数据");
        }
        String merchantId = actor.merchantId();
        if (merchantId != null && !merchantId.isBlank()) {
            throw ApiException.forbidden(
                    "PLATFORM_SCOPE_REQUIRED",
                    "商户账号无权查看平台级经营分析");
        }
    }

    /**
     * 项目维度只读 scope：平台管理员返回空＝全量（D-032）；其余主体返回其项目 ID，没有项目归属则拒绝。
     */
    public Optional<String> currentProjectScopeQuietly() {
        AccessActor actor = currentActor();
        if (isPlatformAdminBridge(actor)) {
            return Optional.empty();
        }
        return Optional.of(requireProjectScope(actor));
    }

    private boolean isPlatformAdminBridge(AccessActor actor) {
        return "active".equals(actor.status())
                && "platform_admin".equals(normalize(actor.accountType()));
    }

    /** #14：客人自助主体（由客人 token 过滤器写入）。仅对 {@link #CUSTOMER_SELF_SERVICE_PERMISSIONS} 放行。 */
    private boolean isCustomerSelfServiceBridge(AccessActor actor) {
        return "active".equals(actor.status())
                && "customer".equals(normalize(actor.accountType()));
    }

    private boolean isActive(AccessRepository.PermissionEffect effect, Instant checkedAt) {
        if (effect.consumed()) {
            return false;
        }
        if (effect.validFrom() != null && checkedAt.isBefore(effect.validFrom())) {
            return false;
        }
        return effect.validUntil() == null || checkedAt.isBefore(effect.validUntil());
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void deny(String permissionCode) {
        throw ApiException.forbidden("PERMISSION_DENIED", "没有操作权限：" + permissionCode);
    }
}
