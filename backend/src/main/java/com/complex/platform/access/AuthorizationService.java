package com.complex.platform.access;

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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(AccessRepository.class)
public class AuthorizationService implements CallerScopeResolver {

    private final AccessRepository accessRepository;

    public AuthorizationService(AccessRepository accessRepository) {
        this.accessRepository = accessRepository;
    }

    public void requirePermission(String permissionCode) {
        AccessActor actor = currentActor();
        Instant checkedAt = Instant.now();
        if (!knownPermission(permissionCode)) {
            deny(permissionCode);
        }
        if (isPlatformAdminBridge(actor)) {
            return;
        }

        List<AccessRepository.PermissionEffect> effects = accessRepository.findPermissionEffects(actor.id(), checkedAt).stream()
                .filter(effect -> permissionCode.equals(effect.permissionCode()))
                .filter(effect -> isActive(effect, checkedAt))
                .toList();

        if (effects.stream().anyMatch(effect -> effect.effect() == AccessRepository.Effect.DENY)) {
            deny(permissionCode);
        }

        AccessRepository.PermissionEffect grant = effects.stream()
                .filter(effect -> effect.effect() == AccessRepository.Effect.GRANT)
                .min(Comparator.comparing(AccessRepository.PermissionEffect::sourceId, Comparator.nullsLast(String::compareTo)))
                .orElse(null);
        if (grant == null) {
            deny(permissionCode);
        }

        if (grant.singleUse()) {
            boolean consumed = accessRepository.markSingleUsePermissionConsumed(
                    grant.sourceId(), grant.permissionCode(), actor.id(), checkedAt);
            if (!consumed) {
                // Another concurrent request already consumed this single-use grant.
                deny(permissionCode);
            }
        }
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
        boolean wildcard = accessRepository.findUserById(userId)
                .map(user -> "active".equals(user.status())
                        && "platform_admin".equals(normalize(user.accountType())))
                .orElse(false);
        return computeEffectivePermissions(userId, wildcard);
    }

    private EffectivePermissions computeEffectivePermissions(String userId, boolean wildcard) {
        Instant checkedAt = Instant.now();
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
                .map(AccessRepository.PermissionEffect::permissionCode)
                .filter(code -> !denies.contains(code))
                .distinct()
                .sorted()
                .toList();
        return new EffectivePermissions(userId, grants, denies, wildcard, checkedAt);
    }

    public AccessActor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw ApiException.unauthorized("UNAUTHENTICATED", "authentication is required");
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
        if (principal instanceof UserDetails userDetails) {
            return new AccessActor(authentication.getName(), userDetails.getUsername(), "staff", null, null, "active");
        }
        return new AccessActor(authentication.getName(), authentication.getName(), "staff", null, null, "active");
    }

    /**
     * Returns the caller scope token for idempotency key isolation.
     * "m:{merchantId}" for merchant users, "p" for platform admins/staff.
     * Returns "" on any error so IdempotencyService falls back to unscoped (safe degradation).
     */
    @Override
    public String resolve() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return "";
            }
            Object principal = auth.getPrincipal();
            String merchantId = null;
            if (principal instanceof AccessActor actor) {
                merchantId = actor.merchantId();
            } else if (principal instanceof AuthService.UserRecord user) {
                merchantId = user.merchantId();
            }
            if (merchantId != null && !merchantId.isBlank()) {
                return "m:" + merchantId;
            }
            return "p";
        } catch (Exception e) {
            return "";
        }
    }

    private boolean knownPermission(String permissionCode) {
        try {
            return accessRepository.permissionExists(permissionCode);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean isPlatformAdmin(AccessActor actor) {
        return isPlatformAdminBridge(actor);
    }

    /**
     * Returns the verified supervisor level of the current caller from their account type.
     * Platform admins always get level 3. Staff accounts with accountType "supervisor_lN" get level N.
     * All other accounts return 0, which is insufficient for any supervisor review policy.
     */
    public int currentSupervisorLevel() {
        AccessActor actor = currentActor();
        if (isPlatformAdmin(actor)) {
            return 3;
        }
        String accountType = actor.accountType() == null ? "" : actor.accountType();
        if (accountType.startsWith("supervisor_l")) {
            try {
                return Integer.parseInt(accountType.substring("supervisor_l".length()));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    /**
     * Returns the current caller's management level_rank (1..30, see staff_levels), or 0 if unknown.
     * Degrades to 0 on null repository or any lookup error so callers can fall back to level-based checks.
     */
    public int currentActorLevelRank() {
        try {
            return levelRankOf(currentActor().id());
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
     * {@link AccessRepository.Source#ROLE} 且在有效期内才算"角色赋予"。平台管理员桥（wildcard）视为持有。
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
            return active.stream()
                    .anyMatch(effect -> effect.effect() == AccessRepository.Effect.GRANT
                            && effect.source() == AccessRepository.Source.ROLE);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * 返回指定用户的隔离键 merchant_id（用于同商户范围闸），无则空。绝不抛错（查询异常→空）。
     * 与 {@link #currentMerchantIdQuietly()} 对称，但面向"任意用户"（如审批记录的发起人）。
     */
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
     * Platform admins get Optional.empty() (no filter, sees everything).
     * Merchant users get Optional.of(merchantId).
     * Staff without a merchantId throw MERCHANT_SCOPE_REQUIRED.
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
        throw ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "a merchant scope is required for this operation");
    }

    /**
     * Returns the current caller's merchant id if present, otherwise empty.
     * Unlike {@link #currentMerchantScope()}, this never throws: platform admins and staff
     * without a merchant simply yield {@code Optional.empty()}. Degrades to empty on any error.
     * Used by admission-gate fact checks that are scoped to the caller's merchant.
     */
    public Optional<String> currentMerchantIdQuietly() {
        try {
            String merchantId = currentActor().merchantId();
            if (merchantId != null && !merchantId.isBlank()) {
                return Optional.of(merchantId);
            }
            return Optional.empty();
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private boolean isPlatformAdminBridge(AccessActor actor) {
        return "active".equals(actor.status())
                && "platform_admin".equals(normalize(actor.accountType()));
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
        throw ApiException.forbidden("PERMISSION_DENIED", "permission denied: " + permissionCode);
    }
}
