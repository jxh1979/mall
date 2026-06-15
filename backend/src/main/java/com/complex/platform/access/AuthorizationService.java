package com.complex.platform.access;

import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(AccessRepository.class)
public class AuthorizationService {

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
            accessRepository.markSingleUsePermissionConsumed(grant.sourceId(), grant.permissionCode(), actor.id(), checkedAt);
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

    private boolean knownPermission(String permissionCode) {
        try {
            return accessRepository.permissionExists(permissionCode);
        } catch (RuntimeException exception) {
            return false;
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
