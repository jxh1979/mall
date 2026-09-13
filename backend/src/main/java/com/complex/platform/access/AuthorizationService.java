package com.complex.platform.access;

// 文件版本：V0.08

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
        for (String code : new java.util.LinkedHashSet<>(java.util.Arrays.asList(permissionCodes))) {
            AccessRepository.PermissionEffect grant = permissionGrant(actor, code, effects, daily);
            if (grant != null && grant.singleUse()) {
                consume.putIfAbsent(grant.source() + ":" + grant.sourceId(), grant);
            }
        }
        for (AccessRepository.PermissionEffect grant : consume.values()) {
            if (!accessRepository.markSingleUsePermissionConsumed(
                    grant.sourceId(), grant.permissionCode(), actor.id(), checkedAt)) {
                deny(grant.permissionCode());
            }
        }
    }

    private AccessRepository.PermissionEffect permissionGrant(
            AccessActor actor, String code, List<AccessRepository.PermissionEffect> effects) {
        return permissionGrant(actor, code, effects, dailyPermissions());
    }

    private AccessRepository.PermissionEffect permissionGrant(
            AccessActor actor, String code, List<AccessRepository.PermissionEffect> effects,
            java.util.function.Supplier<java.util.Set<String>> daily) {
        if (!"active".equals(actor.status()) || !knownPermission(code)) {
            deny(code);
        }
        List<AccessRepository.PermissionEffect> matching = effects.stream()
                .filter(effect -> code.equals(effect.permissionCode())).toList();
        if (matching.stream().anyMatch(effect -> effect.effect() == AccessRepository.Effect.DENY)) {
            deny(code);
        }
        if (isMasterAuthority(actor)
                || (isPlatformAdminBridge(actor) && daily.get().contains(code))
                || (isCustomerSelfServiceBridge(actor) && CUSTOMER_SELF_SERVICE_PERMISSIONS.contains(code))) {
            return null;
        }
        AccessRepository.PermissionEffect grant = matching.stream()
                .filter(effect -> effect.effect() == AccessRepository.Effect.GRANT)
                .filter(effect -> isExplicitOrDailyGrant(effect, daily))
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

    private boolean isExplicitOrDailyGrant(AccessRepository.PermissionEffect effect,
                                           java.util.function.Supplier<java.util.Set<String>> daily) {
        // 默认管理员角色随权限字典播种，不能把其高危条目当成逐项授权；治理本职码除外（M006-I9-01）。
        return effect.source() != AccessRepository.Source.ROLE
                || !"role_platform_admin".equals(effect.sourceId())
                || daily.get().contains(effect.permissionCode())
                || PLATFORM_ADMIN_GOVERNANCE_PERMISSIONS.contains(effect.permissionCode());
    }

    public boolean isMasterAuthority(AccessActor actor) {
        return isPlatformAdminBridge(actor)
                && SessionIssuer.MASTER_USER_ID.equals(actor.id())
                && SessionIssuer.MASTER_USERNAME.equals(actor.username());
    }

    /**
     * 当前 actor 自身的有效权限集（self 端，只读）。不消费单次授权——
     * 单次授权仅在真正执行动作的 {@link #requirePermission} 中消费，避免"看一眼菜单就烧掉"。
     */
    public EffectivePermissions effectivePermissions() {
        AccessActor actor = currentActor();
        return computeEffectivePermissions(actor.id(), isPlatformAdminBridge(actor));
    }

    /** 指定用户的有效权限集（平台管理用；调用方须先校验 {@code access.user.view}）。 */
    public EffectivePermissions effectivePermissionsForUser(String userId) {
        AccessActor actor = currentActor();
        AccessUser user = accessRepository.findUserById(userId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "账号不存在"));
        if (!isMasterAuthority(actor)) {
            String organizationId = requireProjectScope(actor);
            if (!organizationId.equals(user.organizationId())) {
                throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "无权查询其他组织的用户");
            }
            if (actor.merchantId() != null && !actor.merchantId().isBlank()
                    && !actor.merchantId().equals(user.merchantId())) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权查询其他商户的用户");
            }
        }
        Optional<AccessUser> target = Optional.of(user);
        boolean wildcard = target
                .map(account -> "active".equals(account.status())
                        && "platform_admin".equals(normalize(account.accountType())))
                .orElse(false);
        return computeEffectivePermissions(userId, wildcard);
    }

    private EffectivePermissions computeEffectivePermissions(String userId, boolean wildcard) {
        Instant checkedAt = Instant.now();
        java.util.function.Supplier<java.util.Set<String>> daily = dailyPermissions();
        List<AccessRepository.PermissionEffect> active = accessRepository.findPermissionEffects(userId, checkedAt).stream()
                .filter(effect -> isActive(effect, checkedAt))
                .toList();
        List<String> denies = active.stream()
                .filter(effect -> effect.effect() == AccessRepository.Effect.DENY)
                .map(AccessRepository.PermissionEffect::permissionCode)
                .distinct()
                .sorted()
                .toList();
        // 实现 A：grants 预扣 denies（DENY 优先），denies 仍单独返回供前端展示冲突来源。
        List<String> grants = active.stream()
                .filter(effect -> effect.effect() == AccessRepository.Effect.GRANT)
                .filter(effect -> isExplicitOrDailyGrant(effect, daily))
                .map(AccessRepository.PermissionEffect::permissionCode)
                .filter(code -> !denies.contains(code))
                .distinct()
                .sorted()
                .toList();
        java.util.Set<String> effective = new java.util.TreeSet<>(grants);
        if (wildcard) {
            accessRepository.listPermissions().stream()
                    .filter(p -> SessionIssuer.MASTER_USER_ID.equals(userId) || daily.get().contains(p.permissionCode()))
                    .map(PermissionDefinition::permissionCode).forEach(effective::add);
        }
        effective.removeAll(denies);
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

    /** 非商户请求按已有主体及组织隔离；商户与系统请求沿用原范围。 */
    @Override
    public String resolve() {
        String legacy = legacyScope();
        if (!"p".equals(legacy)) {
            return legacy;
        }
        AccessActor actor = currentActor();
        if (actor.id() == null || actor.id().isBlank()) {
            throw ApiException.unauthorized("UNAUTHENTICATED", "无法识别的认证主体");
        }
        return "u:" + actor.id().length() + ":" + actor.id() + ":"
                + java.util.Objects.toString(actor.organizationId(), "");
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
            return isMasterAuthority(actor) ? 30 : levelRankOf(actor.id());
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
            if (active.stream().anyMatch(effect -> effect.effect() == AccessRepository.Effect.DENY)) {
                return false;
            }
            java.util.function.Supplier<java.util.Set<String>> daily = dailyPermissions();
            return active.stream()
                    .anyMatch(effect -> effect.effect() == AccessRepository.Effect.GRANT
                            && effect.source() == AccessRepository.Source.ROLE
                            && isExplicitOrDailyGrant(effect, daily));
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
     * Merchant users get Optional.of(merchantId).
     * Callers without a merchantId throw MERCHANT_SCOPE_REQUIRED.
     * 红线④：不因平台管理员身份整类关掉商户隔离；持密钥最高权限只在逐笔接口放行。
     */
    public Optional<String> currentMerchantScope() {
        AccessActor actor = currentActor();
        String merchantId = actor.merchantId();
        if (merchantId != null && !merchantId.isBlank()) {
            return Optional.of(merchantId);
        }
        throw ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行");
    }

    /**
     * Returns the current caller's merchant id if present, otherwise empty.
     * Unlike {@link #currentMerchantScope()}, this never throws: callers without a merchant
     * yield {@code Optional.empty()}. Degrades to empty on any error.
     * 有商户号就按商户号过滤，不因平台管理员身份改成看全量。
     */
    public Optional<String> currentMerchantIdQuietly() {
        try {
            AccessActor actor = currentActor();
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
     * 项目维度只读 scope：返回当前主体的项目 ID。没有项目归属则拒绝。
     * 红线④：不因平台管理员身份返回空（全量）。
     */
    public Optional<String> currentProjectScopeQuietly() {
        AccessActor actor = currentActor();
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
