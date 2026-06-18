package com.complex.platform.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthorizationServiceTests {

    private InMemoryAccessRepository repository;
    private AuthorizationService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAccessRepository();
        service = new AuthorizationService(repository);
        repository.addKnownPermission("ops.production_readiness.manage");
        repository.addKnownPermission("ops.production_readiness.view");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requirePermissionReturnsForbiddenPermissionDeniedWhenUserLacksPermission() {
        authenticate("usr_staff", "staff", "staff");

        assertThatThrownBy(() -> service.requirePermission("ops.production_readiness.manage"))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(403);
                    assertThat(exception.code()).isEqualTo("PERMISSION_DENIED");
                });
    }

    @Test
    void denyWinsWhenGrantAndDenyExistForSamePermission() {
        authenticate("usr_staff", "staff", "staff");
        repository.addEffect(effect("usr_staff", "ops.production_readiness.manage", AccessRepository.Effect.GRANT));
        repository.addEffect(effect("usr_staff", "ops.production_readiness.manage", AccessRepository.Effect.DENY));

        assertThatThrownBy(() -> service.requirePermission("ops.production_readiness.manage"))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(403);
                    assertThat(exception.code()).isEqualTo("PERMISSION_DENIED");
                });
    }

    @Test
    void expiredTemporaryTemplateGrantDoesNotAuthorize() {
        authenticate("usr_staff", "staff", "staff");
        repository.addEffect(new AccessRepository.PermissionEffect(
                "usr_staff",
                "ops.production_readiness.manage",
                AccessRepository.Effect.GRANT,
                AccessRepository.Source.TEMPLATE_APPLICATION,
                "application_expired",
                Instant.parse("2026-06-13T00:00:00Z"),
                Instant.parse("2026-06-13T01:00:00Z"),
                false,
                false
        ));

        assertThatThrownBy(() -> service.requirePermission("ops.production_readiness.manage"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("PERMISSION_DENIED"));
    }

    @Test
    void singleUseTemplateGrantIsConsumedAfterFirstSuccessfulAuthorization() {
        authenticate("usr_staff", "staff", "staff");
        repository.addEffect(new AccessRepository.PermissionEffect(
                "usr_staff",
                "ops.production_readiness.manage",
                AccessRepository.Effect.GRANT,
                AccessRepository.Source.TEMPLATE_APPLICATION,
                "application_once",
                Instant.parse("2026-06-13T00:00:00Z"),
                null,
                true,
                false
        ));

        service.requirePermission("ops.production_readiness.manage");

        assertThat(repository.consumedSourceIds).containsExactly("application_once");
        assertThatThrownBy(() -> service.requirePermission("ops.production_readiness.manage"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("PERMISSION_DENIED"));
    }

    @Test
    void localSeedBridgeAuthorizesPlatformAdminButNotArbitraryStaffWithSmokeRole() {
        authenticate("usr_admin", "seed-admin", "platform_admin");
        service.requirePermission("ops.production_readiness.view");

        authenticate("usr_staff", "ordinary-staff", "staff");
        assertThatThrownBy(() -> service.requirePermission("ops.production_readiness.view"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("PERMISSION_DENIED"));
    }

    @Test
    void singleUsePermissionConcurrentConsumerIsDeniedWhenAtomicUpdateReturnsZeroRows() {
        authenticate("usr_staff", "staff", "staff");
        repository.addEffect(new AccessRepository.PermissionEffect(
                "usr_staff",
                "ops.production_readiness.manage",
                AccessRepository.Effect.GRANT,
                AccessRepository.Source.TEMPLATE_APPLICATION,
                "application_toctou",
                null,
                null,
                true,
                false
        ));
        // Simulate: another concurrent thread already consumed this grant at DB level
        // (AND consumed=FALSE guard would return 0 rows for the second caller)
        repository.simulateAlreadyConsumedAtDb("application_toctou");

        // Second caller sees unconsumed (stale read), but atomic update returns false
        assertThatThrownBy(() -> service.requirePermission("ops.production_readiness.manage"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("PERMISSION_DENIED"));
    }

    @Test
    void isPlatformAdminReturnsTrueForPlatformAdminActor() {
        authenticate("usr_admin", "seed-admin", "platform_admin");
        assertThat(service.isPlatformAdmin(service.currentActor())).isTrue();
    }

    @Test
    void isPlatformAdminReturnsFalseForStaffActor() {
        authenticate("usr_staff", "ordinary-staff", "staff");
        assertThat(service.isPlatformAdmin(service.currentActor())).isFalse();
    }

    @Test
    void currentMerchantScopeReturnsEmptyForPlatformAdmin() {
        authenticate("usr_admin", "seed-admin", "platform_admin");
        assertThat(service.currentMerchantScope()).isEmpty();
    }

    @Test
    void currentMerchantScopeReturnsMerchantIdForMerchantUser() {
        // The authenticate helper sets merchantId = "merchant" in UserRecord
        authenticate("usr_staff", "ordinary-staff", "staff");
        assertThat(service.currentMerchantScope()).contains("merchant");
    }

    @Test
    void currentMerchantScopeThrowsForbiddenWhenMerchantIdIsAbsent() {
        AuthService.UserRecord user = new AuthService.UserRecord(
                "usr_no_merchant", "no-merchant", "hash", "staff", "org", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, "token", List.of(new SimpleGrantedAuthority("ROLE_P0_SMOKE"))
        ));

        assertThatThrownBy(() -> service.currentMerchantScope())
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(403);
                    assertThat(exception.code()).isEqualTo("MERCHANT_SCOPE_REQUIRED");
                });
    }

    @Test
    void effectivePermissionsRemovesDenyCoveredCodesFromGrantsAndListsDenies() {
        authenticate("usr_staff", "staff", "staff");
        repository.addEffect(effect("usr_staff", "ticket.order.view", AccessRepository.Effect.GRANT));
        repository.addEffect(effect("usr_staff", "finance.refund.create", AccessRepository.Effect.GRANT));
        repository.addEffect(effect("usr_staff", "finance.refund.create", AccessRepository.Effect.DENY));

        EffectivePermissions result = service.effectivePermissions();

        assertThat(result.userId()).isEqualTo("usr_staff");
        assertThat(result.grants()).containsExactly("ticket.order.view");
        assertThat(result.denies()).containsExactly("finance.refund.create");
        assertThat(result.wildcard()).isFalse();
        assertThat(result.computedAt()).isNotNull();
    }

    @Test
    void effectivePermissionsMarksWildcardForActivePlatformAdmin() {
        authenticate("usr_admin", "seed-admin", "platform_admin");
        assertThat(service.effectivePermissions().wildcard()).isTrue();
    }

    @Test
    void effectivePermissionsQueryDoesNotConsumeSingleUseGrant() {
        authenticate("usr_staff", "staff", "staff");
        repository.addEffect(new AccessRepository.PermissionEffect(
                "usr_staff",
                "ops.production_readiness.manage",
                AccessRepository.Effect.GRANT,
                AccessRepository.Source.TEMPLATE_APPLICATION,
                "application_once",
                Instant.parse("2026-06-13T00:00:00Z"),
                null,
                true,
                false
        ));

        EffectivePermissions first = service.effectivePermissions();
        EffectivePermissions second = service.effectivePermissions();

        assertThat(first.grants()).contains("ops.production_readiness.manage");
        assertThat(second.grants()).contains("ops.production_readiness.manage");
        assertThat(repository.consumedSourceIds).isEmpty();
    }

    @Test
    void effectivePermissionsExcludesExpiredGrant() {
        authenticate("usr_staff", "staff", "staff");
        repository.addEffect(new AccessRepository.PermissionEffect(
                "usr_staff",
                "ops.production_readiness.view",
                AccessRepository.Effect.GRANT,
                AccessRepository.Source.TEMPLATE_APPLICATION,
                "application_expired",
                Instant.parse("2026-06-13T00:00:00Z"),
                Instant.parse("2026-06-13T01:00:00Z"),
                false,
                false
        ));

        assertThat(service.effectivePermissions().grants()).doesNotContain("ops.production_readiness.view");
    }

    private AccessRepository.PermissionEffect effect(
            String userId,
            String permissionCode,
            AccessRepository.Effect effect
    ) {
        return new AccessRepository.PermissionEffect(
                userId,
                permissionCode,
                effect,
                AccessRepository.Source.ROLE,
                "role_p0",
                null,
                null,
                false,
                false
        );
    }

    private void authenticate(String id, String username, String accountType) {
        AuthService.UserRecord user = new AuthService.UserRecord(
                id,
                username,
                "hash",
                accountType,
                "org",
                "merchant",
                "active",
                Instant.parse("2026-06-14T00:00:00Z")
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                "token",
                List.of(new SimpleGrantedAuthority("ROLE_P0_SMOKE"))
        ));
    }

    private static final class InMemoryAccessRepository implements AccessRepository {
        private final Set<String> knownPermissions = new LinkedHashSet<>();
        private final List<PermissionEffect> effects = new ArrayList<>();
        final List<String> consumedSourceIds = new ArrayList<>();
        // Tracks grants that were already consumed at the "DB level" (simulates AND consumed=FALSE returning 0 rows)
        private final Set<String> alreadyConsumedAtDb = new java.util.LinkedHashSet<>();

        void addKnownPermission(String permissionCode) {
            knownPermissions.add(permissionCode);
        }

        void addEffect(PermissionEffect effect) {
            effects.add(effect);
        }

        @Override
        public boolean permissionExists(String permissionCode) {
            return knownPermissions.contains(permissionCode);
        }

        @Override
        public List<PermissionDefinition> listPermissions() {
            return knownPermissions.stream()
                    .map(permissionCode -> new PermissionDefinition(
                            permissionCode,
                            permissionCode,
                            "ops",
                            "medium",
                            false,
                            "active"
                    ))
                    .toList();
        }

        @Override
        public List<PermissionTemplate> listTemplates() {
            return List.of();
        }

        @Override
        public PermissionTemplate saveTemplate(PermissionTemplate template, List<PermissionTemplateItem> items) {
            return template;
        }

        @Override
        public PermissionTemplate copyTemplate(String sourceTemplateId, PermissionTemplate copy) {
            return copy;
        }

        @Override
        public List<PermissionTemplateItem> findTemplateItems(String templateId) {
            return List.of();
        }

        @Override
        public AccessUser createUser(AccessUser user) {
            return user;
        }

        @Override
        public List<AccessUser> listUsers() {
            return List.of();
        }

        @Override
        public List<RoleRecord> listRoles() {
            return List.of();
        }

        @Override
        public RoleRecord createRole(RoleRecord role) {
            return role;
        }

        @Override
        public List<PermissionTemplateApplication> applyTemplate(
                String templateId,
                List<String> targetUserIds,
                Instant validFrom,
                Instant validUntil,
                boolean singleUse,
                String appliedBy
        ) {
            return targetUserIds.stream()
                    .map(userId -> new PermissionTemplateApplication(
                            "app_" + userId,
                            templateId,
                            "user",
                            userId,
                            validFrom,
                            validUntil,
                            singleUse,
                            false,
                            "active",
                            appliedBy,
                            Instant.parse("2026-06-14T00:00:00Z"),
                            null,
                            null
                    ))
                    .toList();
        }

        @Override
        public TemporaryGrant createTemporaryGrant(TemporaryGrant temporaryGrant) {
            return temporaryGrant;
        }

        @Override
        public TemporaryGrant revokeTemporaryGrant(String id, String revokedBy, Instant revokedAt) {
            return new TemporaryGrant(
                    id,
                    "",
                    List.of(),
                    null,
                    null,
                    false,
                    false,
                    "revoked",
                    null,
                    null,
                    null,
                    revokedBy,
                    revokedAt
            );
        }

        @Override
        public List<PermissionEffect> findPermissionEffects(String userId, Instant checkedAt) {
            return effects.stream()
                    .filter(effect -> effect.userId().equals(userId))
                    .map(effect -> consumedSourceIds.contains(effect.sourceId())
                            ? new PermissionEffect(
                            effect.userId(),
                            effect.permissionCode(),
                            effect.effect(),
                            effect.source(),
                            effect.sourceId(),
                            effect.validFrom(),
                            effect.validUntil(),
                            effect.singleUse(),
                            true
                    )
                            : effect)
                    .toList();
        }

        void simulateAlreadyConsumedAtDb(String sourceId) {
            alreadyConsumedAtDb.add(sourceId);
        }

        @Override
        public boolean markSingleUsePermissionConsumed(
                String sourceId,
                String permissionCode,
                String userId,
                Instant consumedAt
        ) {
            if (alreadyConsumedAtDb.contains(sourceId)) {
                return false;
            }
            consumedSourceIds.add(sourceId);
            alreadyConsumedAtDb.add(sourceId);
            return true;
        }
    }
}
