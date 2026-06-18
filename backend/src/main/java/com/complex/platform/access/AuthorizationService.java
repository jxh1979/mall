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
