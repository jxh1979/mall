package com.complex.platform.access;

// 文件版本：V0.04

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.auth.AuthService;
import com.complex.platform.auth.SessionIssuer;
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
        repository.addKnownPermission("merchant.service_work_order.view");
        repository.addKnownPermission("facility.work_order.manage");
        repository.addKnownPermission("facility.work_order.view_assigned");
        repository.addKnownPermission("facility.work_order.process_assigned");
        repository.addKnownPermission("facility.work_order.accept_assigned");
        repository.addKnownPermission("facility.equipment.view");
        repository.addKnownPermission("facility.inspection.manage");
        repository.addKnownPermission("facility.inspection.view_assigned");
        repository.addKnownPermission("facility.inspection.record_assigned");
        repository.addKnownPermission("facility.inspection.template.view");
        repository.addKnownPermission("facility.inspection.report_exception_assigned");
        repository.addKnownPermission("facility.site_entry.upload_completion_assigned");
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
    void outsourcedVendorIsNotPlatformScopeAndRequiresProjectScope() {
        authenticateActor(new AccessActor("usr_vendor_a", "vendor-a", "outsourced_vendor", "proj_1", null, "active"));

        assertThat(service.isOutsourcedVendor(service.currentActor())).isTrue();
        assertThat(service.requireProjectScope(service.currentActor())).isEqualTo("proj_1");
        assertThatThrownBy(service::requirePlatformScope)
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("PLATFORM_SCOPE_REQUIRED"));
        assertThatThrownBy(service::requirePlatformComplianceScope)
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("PLATFORM_COMPLIANCE_SCOPE_REQUIRED"));
    }

    @Test
    void outsourcedFacilityTemporaryGrantAllowsAssignedWorkAndEquipmentReadButNotAccept() {
        authenticateActor(new AccessActor("usr_vendor_a", "vendor-a", "outsourced_vendor", "proj_1", null, "active"));
        repository.addEffect(temporaryGrant("usr_vendor_a", "facility.work_order.view_assigned", "2099-01-01T00:00:00Z"));
        repository.addEffect(temporaryGrant("usr_vendor_a", "facility.work_order.process_assigned", "2099-01-01T00:00:00Z"));
        repository.addEffect(temporaryGrant("usr_vendor_a", "facility.equipment.view", "2099-01-01T00:00:00Z"));

        service.requirePermission("facility.work_order.view_assigned");
        service.requirePermission("facility.work_order.process_assigned");
        service.requirePermission("facility.equipment.view");

        assertPermissionDenied("facility.work_order.accept_assigned");
        assertPermissionDenied("facility.work_order.manage");
        assertPermissionDenied("facility.inspection.record_assigned");
        assertPermissionDenied("merchant.service_work_order.view");
        assertPermissionDenied("ops.production_readiness.view");
    }

    @Test
    void outsourcedTemporaryGrantExpiresAndNoLongerAuthorizes() {
        authenticateActor(new AccessActor("usr_vendor_expired", "vendor-expired", "outsourced_vendor", "proj_1", null, "active"));
        repository.addEffect(temporaryGrant("usr_vendor_expired", "facility.work_order.view_assigned", "2020-01-01T00:00:00Z"));

        assertPermissionDenied("facility.work_order.view_assigned");
    }

    @Test
    void outsourcedSecurityCleaningTemporaryGrantOnlyGrantsAssignedInspectionAndExceptionPermissions() {
        authenticateActor(new AccessActor("usr_vendor_s", "vendor-s", "outsourced_vendor", "proj_1", null, "active"));
        repository.addEffect(temporaryGrant("usr_vendor_s", "facility.inspection.view_assigned", "2099-01-01T00:00:00Z"));
        repository.addEffect(temporaryGrant("usr_vendor_s", "facility.inspection.record_assigned", "2099-01-01T00:00:00Z"));
        repository.addEffect(temporaryGrant("usr_vendor_s", "facility.inspection.template.view", "2099-01-01T00:00:00Z"));
        repository.addEffect(temporaryGrant("usr_vendor_s", "facility.inspection.report_exception_assigned", "2099-01-01T00:00:00Z"));

        service.requirePermission("facility.inspection.view_assigned");
        service.requirePermission("facility.inspection.record_assigned");
        service.requirePermission("facility.inspection.template.view");
        service.requirePermission("facility.inspection.report_exception_assigned");

        assertPermissionDenied("facility.inspection.manage");
        assertPermissionDenied("facility.work_order.view_assigned");
        assertPermissionDenied("merchant.service_work_order.view");
        assertPermissionDenied("ops.production_readiness.view");
    }

    @Test
    void currentMerchantScopeReturnsEmptyForPlatformAdmin() {
        authenticate("usr_admin", "seed-admin", "platform_admin");
        assertThat(service.currentMerchantScope()).isEmpty();
    }

    @Test
    void currentMerchantIdQuietlyReturnsEmptyForPlatformAdminEvenWhenSeedBoundMerchantId() {
        // 审计主线2 根因回归：dev seed 曾给 platform_admin 绑 merchant_id（AuthService seed-merchant）。
        // authenticate 助手正是这种形态（accountType=platform_admin 且 merchantId=merchant）。
        // 数据层过滤（TicketingService#filterByMerchantScope / AdmissionGuard）走 currentMerchantIdQuietly，
        // 平台管理员必须得到 empty=看全平台，绝不能被 seed 的 merchant_id 当成商户过滤成 0。
        authenticate("usr_admin", "seed-admin", "platform_admin");
        assertThat(service.currentMerchantIdQuietly()).isEmpty();
    }

    @Test
    void productionMasterIdentityGetsExistingAuthorityWithoutBypassingPermissionDenials() {
        AccessActor master = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        authenticateActor(master);
        assertThat(service.isMasterAuthority(master)).isTrue();
        service.requirePermission("ops.production_readiness.manage");
        repository.addEffect(effect(master.id(), "ops.production_readiness.manage", AccessRepository.Effect.DENY));
        assertThatThrownBy(() -> service.requirePermission("ops.production_readiness.manage"))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("PERMISSION_DENIED"));
        assertThatThrownBy(() -> service.requirePermission("not.registered"))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("PERMISSION_DENIED"));
    }

    @Test
    void platformAdminAndIncompleteMasterIdentityDoNotBecomeHighestAuthority() {
        assertThat(service.isMasterAuthority(new AccessActor(SessionIssuer.MASTER_USER_ID,
                SessionIssuer.MASTER_USERNAME, "platform_admin", "org", null, "active"))).isFalse();
        assertThat(service.isMasterAuthority(new AccessActor(SessionIssuer.MASTER_USER_ID,
                SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "disabled"))).isFalse();
        assertThat(service.isMasterAuthority(new AccessActor("another-user", SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active"))).isFalse();
        assertThat(service.isMasterAuthority(new AccessActor(SessionIssuer.MASTER_USER_ID, "another-name",
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active"))).isFalse();
    }

    @Test
    void currentSupervisorLevelIsThreeOnlyForMasterAuthority() {
        authenticateActor(new AccessActor(
                SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                "platform-company", null, "active"));
        assertThat(service.currentSupervisorLevel()).isEqualTo(3);
    }

    @Test
    void currentMerchantIdQuietlyReturnsMerchantIdForRealMerchantUser() {
        // 真实商户账号（非 platform_admin）仍按 merchant_id 隔离——保证修复不放开商户隔离。
        authenticate("usr_staff", "ordinary-staff", "staff");
        assertThat(service.currentMerchantIdQuietly()).contains("merchant");
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
    void effectivePermissionsDoesNotGiveAdministratorAnUnboundedWildcard() {
        authenticate("usr_admin", "seed-admin", "platform_admin");
        assertThat(service.effectivePermissions().wildcard()).isFalse();
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

    // M036-002（治理地图「临时授权可设有效期或单次使用」）：生效时间未到的授予不放行，也不进有效权限集。
    @Test
    void grantWhoseValidityHasNotStartedDoesNotAuthorize() {
        authenticate("usr_staff", "staff", "staff");
        repository.addEffect(new AccessRepository.PermissionEffect(
                "usr_staff",
                "ops.production_readiness.manage",
                AccessRepository.Effect.GRANT,
                AccessRepository.Source.TEMPORARY_GRANT,
                "temp_not_started",
                Instant.parse("2099-01-01T00:00:00Z"),
                Instant.parse("2099-01-02T00:00:00Z"),
                false,
                false
        ));

        assertPermissionDenied("ops.production_readiness.manage");
        assertThat(service.effectivePermissions().grants()).doesNotContain("ops.production_readiness.manage");
    }

    // M002-F33（依据：治理地图第 655 行「最终权限 = 角色权限 + 基础模板 + 有效临时模板 + 员工单独授予 − 显式拒绝；显式拒绝优先级最高」；以下为技术推导）：
    // 岗位高危挂起只压住岗位派生来源（岗位派生角色、岗位派生模板套用）；员工单独授予、直接角色（含与岗位角色相同但另行直接绑定的）、
    // 非岗位模板套用、临时授权照常生效；员工显式拒绝压过全部来源。requirePermission 与有效权限查询两条路径结论一致。
    @Test
    void positionHoldSuppressesOnlyPositionDerivedGrants() {
        String code = "ops.production_readiness.manage";
        repository.addKnownPermission(code);
        java.util.Map<String, Boolean> expected = new java.util.LinkedHashMap<>();
        java.util.function.BiFunction<String, AccessRepository.Source, AccessRepository.PermissionEffect> grant = (user, source) ->
                new AccessRepository.PermissionEffect(user, code, AccessRepository.Effect.GRANT, source,
                        source == AccessRepository.Source.ROLE ? "role_pos" : source == AccessRepository.Source.TEMPLATE_APPLICATION
                                ? "pta_pos" : source.name().toLowerCase(), null, null, false, false);
        java.util.function.BiFunction<String, AccessRepository.Source, AccessRepository.PermissionEffect> deny = (user, source) ->
                new AccessRepository.PermissionEffect(user, code, AccessRepository.Effect.DENY, source, user + ":" + source, null, null, false, false);

        repository.addEffect(deny.apply("usr_hold_role", AccessRepository.Source.POSITION_HOLD));
        repository.addEffect(grant.apply("usr_hold_role", AccessRepository.Source.ROLE));
        repository.positionRoles.put("usr_hold_role", "role_pos");
        expected.put("usr_hold_role", false);

        repository.addEffect(deny.apply("usr_hold_template", AccessRepository.Source.POSITION_HOLD));
        repository.addEffect(grant.apply("usr_hold_template", AccessRepository.Source.TEMPLATE_APPLICATION));
        repository.positionApplications.put("usr_hold_template", List.of("pta_pos"));
        expected.put("usr_hold_template", false);

        repository.addEffect(deny.apply("usr_hold_override", AccessRepository.Source.POSITION_HOLD));
        repository.addEffect(grant.apply("usr_hold_override", AccessRepository.Source.ROLE));
        repository.addEffect(grant.apply("usr_hold_override", AccessRepository.Source.USER_OVERRIDE));
        repository.positionRoles.put("usr_hold_override", "role_pos");
        expected.put("usr_hold_override", true);

        repository.addEffect(deny.apply("usr_hold_direct", AccessRepository.Source.POSITION_HOLD));
        repository.addEffect(grant.apply("usr_hold_direct", AccessRepository.Source.ROLE));
        repository.positionRoles.put("usr_hold_direct", "role_pos");
        repository.directRoles.put("usr_hold_direct", List.of("role_pos"));
        expected.put("usr_hold_direct", true);

        repository.addEffect(deny.apply("usr_hold_other_role", AccessRepository.Source.POSITION_HOLD));
        repository.addEffect(grant.apply("usr_hold_other_role", AccessRepository.Source.ROLE));
        repository.positionRoles.put("usr_hold_other_role", "role_other");
        expected.put("usr_hold_other_role", true);

        repository.addEffect(deny.apply("usr_hold_temp", AccessRepository.Source.POSITION_HOLD));
        repository.addEffect(grant.apply("usr_hold_temp", AccessRepository.Source.TEMPORARY_GRANT));
        expected.put("usr_hold_temp", true);

        repository.addEffect(deny.apply("usr_hold_other_template", AccessRepository.Source.POSITION_HOLD));
        repository.addEffect(grant.apply("usr_hold_other_template", AccessRepository.Source.TEMPLATE_APPLICATION));
        repository.positionApplications.put("usr_hold_other_template", List.of("pta_elsewhere"));
        expected.put("usr_hold_other_template", true);

        repository.addEffect(deny.apply("usr_explicit", AccessRepository.Source.USER_OVERRIDE));
        repository.addEffect(grant.apply("usr_explicit", AccessRepository.Source.TEMPORARY_GRANT));
        repository.addEffect(grant.apply("usr_explicit", AccessRepository.Source.ROLE));
        expected.put("usr_explicit", false);

        expected.forEach((user, allowed) -> {
            authenticate(user, user, "staff");
            assertThat(service.effectivePermissions().grants().contains(code)).as(user + " 有效权限查询").isEqualTo(allowed);
            assertThat(service.effectivePermissions().denies().contains(code)).as(user + " 拒绝列表").isEqualTo(!allowed);
            boolean passed;
            try {
                service.requirePermission(code);
                passed = true;
            } catch (ApiException denied) {
                passed = false;
            }
            assertThat(passed).as(user + " requirePermission").isEqualTo(allowed);
        });
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

    private AccessRepository.PermissionEffect temporaryGrant(String userId, String permissionCode, String validUntil) {
        return new AccessRepository.PermissionEffect(
                userId,
                permissionCode,
                AccessRepository.Effect.GRANT,
                AccessRepository.Source.TEMPORARY_GRANT,
                "temp_" + permissionCode,
                Instant.parse("2020-01-01T00:00:00Z"),
                Instant.parse(validUntil),
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

    private void authenticateActor(AccessActor actor) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                actor,
                "token",
                List.of(new SimpleGrantedAuthority("ROLE_P0_SMOKE"))
        ));
    }

    private void assertPermissionDenied(String permissionCode) {
        assertThatThrownBy(() -> service.requirePermission(permissionCode))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("PERMISSION_DENIED"));
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

        // M002-F33：岗位派生角色、岗位派生套用与直接绑定角色（与 JdbcAccessRepository 同语义）
        final java.util.Map<String, String> positionRoles = new java.util.LinkedHashMap<>();
        final java.util.Map<String, List<String>> positionApplications = new java.util.LinkedHashMap<>();
        final java.util.Map<String, List<String>> directRoles = new java.util.LinkedHashMap<>();

        @Override
        public java.util.Optional<String> findAssignedPositionRoleId(String userId) {
            return java.util.Optional.ofNullable(positionRoles.get(userId));
        }

        @Override
        public List<PermissionTemplateApplication> findPositionApplications(String userId) {
            return positionApplications.getOrDefault(userId, List.of()).stream()
                    .map(id -> new PermissionTemplateApplication(id, "tpl_position", "user", userId, null, null, false, false, "active",
                            "seed", Instant.parse("2026-06-14T00:00:00Z"), null, null)).toList();
        }

        @Override
        public List<String> findUserRoleIds(String userId) {
            return directRoles.getOrDefault(userId, List.of());
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
