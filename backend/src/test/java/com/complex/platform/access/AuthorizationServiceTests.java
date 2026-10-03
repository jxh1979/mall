package com.complex.platform.access;

// 文件版本：V0.06

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.auth.AuthService;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    // ── XM-M001-AF（治理地图第 653 行项目与商户隔离、第 658 行跨项目或换商户后旧范围派生权限立即失效；技术推导）：
    //    按操作人读权限或职级之后核对其账号当前的项目、商户、账号类型与状态；与会话不一致即按不具备处理。
    //    「会话」= authenticate(...) 写入的主体（组织 org、商户 merchant、active）；「库」= repository.users 里的行。 ──

    private static final String AF_CODE = "ops.production_readiness.manage";

    private void afRow(String id, String accountType, String organizationId, String merchantId, String status) {
        repository.users.put(id, new AccessUser(id, id, accountType, organizationId, merchantId, status,
                Instant.parse("2026-06-14T00:00:00Z")));
        repository.rowVersions.merge(id, 1, Integer::sum); // M722-F05：一次 afRow = 账号行被改写一次
    }

    private void afSingleUseGrant(String userId) {
        repository.addEffect(new AccessRepository.PermissionEffect(userId, AF_CODE, AccessRepository.Effect.GRANT,
                AccessRepository.Source.TEMPLATE_APPLICATION, "application_af_once", null, null, true, false));
    }

    private void afAssertStaleSessionDenied() {
        assertThatThrownBy(() -> service.requirePermission(AF_CODE))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(403);
                    assertThat(exception.code()).isEqualTo("PERMISSION_DENIED");
                });
        assertThat(repository.consumedSourceIds).isEmpty();
    }

    @Test
    void requirePermissionRejectsWhenSessionProjectIsStaleAfterScopeChange() {
        authenticate("usr_o", "usr_o", "staff");
        afRow("usr_o", "staff", "org_b", "merchant", "active");
        afSingleUseGrant("usr_o");

        afAssertStaleSessionDenied();
    }

    @Test
    void requirePermissionConsumesSingleUseOnceWhenIdentityMatches() {
        authenticate("usr_o", "usr_o", "staff");
        afRow("usr_o", "staff", "org", "merchant", "active");
        afSingleUseGrant("usr_o");

        service.requirePermission(AF_CODE);

        assertThat(repository.consumedSourceIds).containsExactly("application_af_once");
    }

    @Test
    void requirePermissionRejectsWhenOnlyMerchantChanged() {
        authenticate("usr_o", "usr_o", "staff");
        afRow("usr_o", "staff", "org", "merchant_b", "active");
        afSingleUseGrant("usr_o");

        afAssertStaleSessionDenied();
    }

    @Test
    void requirePermissionRejectsWhenOnlyAccountTypeChanged() {
        authenticate("usr_o", "usr_o", "staff");
        afRow("usr_o", "merchant_admin", "org", "merchant", "active");
        afSingleUseGrant("usr_o");

        afAssertStaleSessionDenied();
    }

    @Test
    void requirePermissionRejectsWhenAccountDisabledMidRequest() {
        authenticate("usr_o", "usr_o", "staff");
        afRow("usr_o", "staff", "org", "merchant", "disabled");
        afSingleUseGrant("usr_o");

        afAssertStaleSessionDenied();
    }

    @Test
    void effectivePermissionsRejectsStaleSession() {
        authenticate("usr_o", "usr_o", "staff");
        afRow("usr_o", "staff", "org_b", "merchant", "active");
        afSingleUseGrant("usr_o");

        assertThatThrownBy(() -> service.effectivePermissions())
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(403);
                    assertThat(exception.code()).isEqualTo("PERMISSION_DENIED");
                });

        afRow("usr_o", "staff", "org", "merchant", "active");
        assertThat(service.effectivePermissions().grants()).contains(AF_CODE);
    }

    @Test
    void currentActorHasPermissionIsFalseForStaleSession() {
        authenticate("usr_o", "usr_o", "staff");
        afRow("usr_o", "staff", "org_b", "merchant", "active");
        afSingleUseGrant("usr_o");

        assertThat(service.currentActorHasPermission(AF_CODE)).isFalse();

        afRow("usr_o", "staff", "org", "merchant", "active");
        assertThat(service.currentActorHasPermission(AF_CODE)).isTrue();
    }

    @Test
    void currentActorLevelRankIsZeroForStaleSession() {
        authenticate("usr_o", "usr_o", "staff");
        afRow("usr_o", "staff", "org_b", "merchant", "active");
        repository.staffLevels.put("usr_o", 12);

        assertThat(service.currentActorLevelRank()).isZero();

        afRow("usr_o", "staff", "org", "merchant", "active");
        assertThat(service.currentActorLevelRank()).isEqualTo(12);
    }

    @Test
    void holdsPermissionViaRoleIsFalseForStaleSelf() {
        authenticate("usr_o", "usr_o", "staff");
        afRow("usr_o", "staff", "org_b", "merchant", "active");
        repository.addEffect(effect("usr_o", AF_CODE, AccessRepository.Effect.GRANT));
        repository.addEffect(effect("usr_t", AF_CODE, AccessRepository.Effect.GRANT));

        assertThat(service.holdsPermissionViaRole("usr_o", AF_CODE)).isFalse();
        // 查他人不核对当前操作人身份，行为不变
        assertThat(service.holdsPermissionViaRole("usr_t", AF_CODE)).isTrue();

        afRow("usr_o", "staff", "org", "merchant", "active");
        assertThat(service.holdsPermissionViaRole("usr_o", AF_CODE)).isTrue();
    }

    // 岗位派生事实是判定过程中才按操作人 id 读取的：换项目恰在这次读取时提交（旧岗位派生被清空、归属改到新项目），
    // 旧会话不得凭「挂起不再压得住」的旧岗位角色授予过判定——核对必须落在最后一次按操作人读取之后。
    private void afArmScopeChangeDuringPositionRead() {
        authenticate("usr_o", "usr_o", "staff");
        afRow("usr_o", "staff", "org", "merchant", "active");
        repository.addEffect(new AccessRepository.PermissionEffect("usr_o", AF_CODE, AccessRepository.Effect.DENY,
                AccessRepository.Source.POSITION_HOLD, "hold_af", null, null, false, false));
        repository.addEffect(new AccessRepository.PermissionEffect("usr_o", AF_CODE, AccessRepository.Effect.GRANT,
                AccessRepository.Source.ROLE, "role_pos", null, null, false, false));
        repository.positionRoles.put("usr_o", "role_pos");
        repository.onPositionRead = () -> {
            repository.positionRoles.remove("usr_o");
            afRow("usr_o", "staff", "org_b", "merchant", "active");
        };
    }

    @Test
    void requirePermissionRejectsWhenScopeChangesWhilePositionFactsAreRead() {
        afArmScopeChangeDuringPositionRead();

        afAssertStaleSessionDenied();
        assertThat(repository.positionRoles).doesNotContainKey("usr_o"); // 岗位派生事实确实被读过（读取时换项目已提交）
    }

    @Test
    void currentActorHasPermissionIsFalseWhenScopeChangesWhilePositionFactsAreRead() {
        afArmScopeChangeDuringPositionRead();

        assertThat(service.currentActorHasPermission(AF_CODE)).isFalse();
        assertThat(repository.positionRoles).doesNotContainKey("usr_o");
    }

    @Test
    void holdsPermissionViaRoleIsFalseWhenScopeChangesWhilePositionFactsAreRead() {
        afArmScopeChangeDuringPositionRead();

        assertThat(service.holdsPermissionViaRole("usr_o", AF_CODE)).isFalse();
        assertThat(repository.positionRoles).doesNotContainKey("usr_o");
    }

    // ── M722-F05（治理地图第 653、658 行；技术推导）：一次判定内账号归属改走又改回——末尾四项与会话相同，但账号行被改写过，
    //    按不具备处理。验收①「服务层覆盖上述各入口的往返时序（中间范围的授予确被读到、改回后四项与会话相同）」。
    //    时序：判定开始读账号行（原范围）→ 读能力事实之前改走、中间范围事实落地 → 能力事实被读到 → 中间范围事实撤销、改回 → 判定末尾。
    //    中间范围：项目往返 = (org_b, merchant)，商户往返 = (org, merchant_b)。修复前每条都红（放行／true／职级原值）。 ──

    private static final String F05_SUPERVISOR = "governance.supervisor_review.approve";

    private static AccessRepository.PermissionEffect f05RoleGrant(String code) {
        return new AccessRepository.PermissionEffect("usr_o", code, AccessRepository.Effect.GRANT,
                AccessRepository.Source.ROLE, "role_pos_b", null, null, false, false);
    }

    private static AccessRepository.PermissionEffect f05SingleUseGrant() {
        return new AccessRepository.PermissionEffect("usr_o", AF_CODE, AccessRepository.Effect.GRANT,
                AccessRepository.Source.TEMPLATE_APPLICATION, "application_f05_once", null, null, true, false);
    }

    // 会话与账号行都在原范围；钩子挂在「按操作人读权限效果」这一次读取的前后。
    private void f05ArmRoundTripAroundEffectsRead(String middleOrganization, String middleMerchant,
                                                  AccessRepository.PermissionEffect grantedInMiddleScope) {
        authenticate("usr_o", "usr_o", "staff");
        afRow("usr_o", "staff", "org", "merchant", "active");
        repository.beforeEffectsRead = () -> {
            afRow("usr_o", "staff", middleOrganization, middleMerchant, "active");
            repository.addEffect(grantedInMiddleScope);
        };
        repository.afterEffectsRead = () -> {
            repository.effects.remove(grantedInMiddleScope);
            afRow("usr_o", "staff", "org", "merchant", "active");
        };
    }

    // 中间范围的授予确被读到；判定后该授予已撤销；改回后四项与会话相同、账号行确被改写过两次。
    private void f05AssertRoundTripHappened(AccessRepository.PermissionEffect grantedInMiddleScope) {
        assertThat(repository.lastEffectsRead).contains(grantedInMiddleScope);
        assertThat(repository.effects).doesNotContain(grantedInMiddleScope);
        f05AssertRowBackToSession();
    }

    private void f05AssertRowBackToSession() {
        AccessUser row = repository.users.get("usr_o");
        assertThat(List.of(row.organizationId(), row.merchantId(), row.accountType(), row.status()))
                .containsExactly("org", "merchant", "staff", "active");
        assertThat(repository.rowVersions.get("usr_o")).isEqualTo(3);
    }

    // requirePermissions：拒绝（403 PERMISSION_DENIED），单次来源不被消耗。
    @Test
    void requirePermissionRejectsScopeRoundTripAndKeepsSingleUseGrant() {
        AccessRepository.PermissionEffect grant = f05SingleUseGrant();
        f05ArmRoundTripAroundEffectsRead("org_b", "merchant", grant);

        afAssertStaleSessionDenied();
        f05AssertRoundTripHappened(grant);
    }

    // requirePermissions：中间范围的岗位角色授予（非单次，审计席复现的形态）；项目往返。
    @Test
    void requirePermissionRejectsProjectRoundTripWithRoleGrant() {
        AccessRepository.PermissionEffect grant = f05RoleGrant(AF_CODE);
        f05ArmRoundTripAroundEffectsRead("org_b", "merchant", grant);

        afAssertStaleSessionDenied();
        f05AssertRoundTripHappened(grant);
    }

    // requirePermissions：同上，商户往返。
    @Test
    void requirePermissionRejectsMerchantRoundTripWithRoleGrant() {
        AccessRepository.PermissionEffect grant = f05RoleGrant(AF_CODE);
        f05ArmRoundTripAroundEffectsRead("org", "merchant_b", grant);

        afAssertStaleSessionDenied();
        f05AssertRoundTripHappened(grant);
    }

    // effectivePermissions：拒绝（403 PERMISSION_DENIED）。
    @Test
    void effectivePermissionsRejectsScopeRoundTrip() {
        AccessRepository.PermissionEffect grant = f05RoleGrant(AF_CODE);
        f05ArmRoundTripAroundEffectsRead("org_b", "merchant", grant);

        assertThatThrownBy(() -> service.effectivePermissions())
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(403);
                    assertThat(exception.code()).isEqualTo("PERMISSION_DENIED");
                });
        f05AssertRoundTripHappened(grant);
    }

    // currentActorHasPermission：返回 false。
    @Test
    void currentActorHasPermissionIsFalseForScopeRoundTrip() {
        AccessRepository.PermissionEffect grant = f05RoleGrant(AF_CODE);
        f05ArmRoundTripAroundEffectsRead("org_b", "merchant", grant);

        assertThat(service.currentActorHasPermission(AF_CODE)).isFalse();
        f05AssertRoundTripHappened(grant);
    }

    // holdsPermissionViaRole 查本人：返回 false；查他人不核对当前操作人身份，行为不变。
    @Test
    void holdsPermissionViaRoleIsFalseForSelfScopeRoundTrip() {
        AccessRepository.PermissionEffect grant = f05RoleGrant(AF_CODE);
        f05ArmRoundTripAroundEffectsRead("org_b", "merchant", grant);
        repository.addEffect(effect("usr_t", AF_CODE, AccessRepository.Effect.GRANT));

        assertThat(service.holdsPermissionViaRole("usr_o", AF_CODE)).isFalse();
        f05AssertRoundTripHappened(grant);
        assertThat(service.holdsPermissionViaRole("usr_t", AF_CODE)).isTrue();
    }

    // currentActorLevelRank：返回 0。往返发生在读职级的前后；中间范围落下的职级确被读到（范围变更不撤职级行，改回后仍在）。
    @Test
    void currentActorLevelRankIsZeroForScopeRoundTrip() {
        authenticate("usr_o", "usr_o", "staff");
        afRow("usr_o", "staff", "org", "merchant", "active");
        repository.beforeLevelRead = () -> {
            afRow("usr_o", "staff", "org_b", "merchant", "active");
            repository.staffLevels.put("usr_o", 12);
        };
        repository.afterLevelRead = () -> afRow("usr_o", "staff", "org", "merchant", "active");

        assertThat(service.currentActorLevelRank()).isZero();
        assertThat(repository.lastLevelRead).isEqualTo(12);
        f05AssertRowBackToSession();
    }

    // currentSupervisorLevel：返回 0。往返发生在读角色事实的前后（职级稳定为 12，身份未变时本应得 2）。
    @Test
    void currentSupervisorLevelIsZeroForScopeRoundTripWhileRoleFactsAreRead() {
        AccessRepository.PermissionEffect grant = f05RoleGrant(F05_SUPERVISOR);
        f05ArmRoundTripAroundEffectsRead("org_b", "merchant", grant);
        repository.staffLevels.put("usr_o", 12);

        assertThat(service.currentSupervisorLevel()).isZero();
        f05AssertRoundTripHappened(grant);
    }

    // currentSupervisorLevel：返回 0。角色稳定持有，往返发生在读职级的前后。
    @Test
    void currentSupervisorLevelIsZeroForScopeRoundTripWhileLevelIsRead() {
        authenticate("usr_o", "usr_o", "staff");
        afRow("usr_o", "staff", "org", "merchant", "active");
        repository.addEffect(effect("usr_o", F05_SUPERVISOR, AccessRepository.Effect.GRANT));
        repository.beforeLevelRead = () -> {
            afRow("usr_o", "staff", "org_b", "merchant", "active");
            repository.staffLevels.put("usr_o", 12);
        };
        repository.afterLevelRead = () -> afRow("usr_o", "staff", "org", "merchant", "active");

        assertThat(service.currentSupervisorLevel()).isZero();
        assertThat(repository.lastLevelRead).isEqualTo(12);
        f05AssertRowBackToSession();
    }

    // 验收「判定开始时账号行与会话不一致即拒绝、不再读能力」。
    @Test
    void requirePermissionRejectsAtStartWithoutReadingCapabilities() {
        authenticate("usr_o", "usr_o", "staff");
        afRow("usr_o", "staff", "org_b", "merchant", "active");
        afSingleUseGrant("usr_o");

        afAssertStaleSessionDenied();
        assertThat(repository.effectsReads).isZero();
    }

    // 同一句验收在其余入口：开始时账号行与会话不一致即按不具备处理，权限效果与职级都没有被读。
    @Test
    void otherEntriesRejectAtStartWithoutReadingCapabilities() {
        authenticate("usr_o", "usr_o", "staff");
        afRow("usr_o", "staff", "org_b", "merchant", "active");
        repository.addEffect(effect("usr_o", AF_CODE, AccessRepository.Effect.GRANT));
        repository.addEffect(effect("usr_o", F05_SUPERVISOR, AccessRepository.Effect.GRANT));
        repository.staffLevels.put("usr_o", 12);

        assertThatThrownBy(() -> service.effectivePermissions())
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(403);
                    assertThat(exception.code()).isEqualTo("PERMISSION_DENIED");
                });
        assertThat(service.currentActorHasPermission(AF_CODE)).isFalse();
        assertThat(service.holdsPermissionViaRole("usr_o", AF_CODE)).isFalse();
        assertThat(service.currentActorLevelRank()).isZero();
        assertThat(service.currentSupervisorLevel()).isZero();
        assertThat(repository.effectsReads).isZero();
        assertThat(repository.lastLevelRead).isNull();
    }

    // 验收「身份全程未变时行为不变」（修复前后都绿）：角色＋职级 12 → 第 2 级。
    @Test
    void currentSupervisorLevelIsUnchangedWhenIdentityIsStable() {
        authenticate("usr_o", "usr_o", "staff");
        afRow("usr_o", "staff", "org", "merchant", "active");
        repository.addEffect(effect("usr_o", F05_SUPERVISOR, AccessRepository.Effect.GRANT));
        repository.staffLevels.put("usr_o", 12);

        assertThat(service.currentSupervisorLevel()).isEqualTo(2);
        // 角色事实与职级合在一个判定窗口内：账号行只在开始、末尾各读一次（两段各自开始、末尾都读会是四次，两段之间的改写会漏掉）
        assertThat(repository.accountRowReads).isEqualTo(2);
    }

    // 对照（修复前后都绿）：客人与系统调度主体不在员工账号表内，不核对，行为不变。
    @Test
    void principalsWithoutAnAccountRowAreNotCheckedAgainstTheAccountTable() {
        repository.addKnownPermission("customer.stored_value.topup");
        afRow("usr_other", "staff", "org_b", null, "active");

        authenticateActor(new AccessActor("cust_x", "cust_x", "customer", null, null, "active"));
        service.requirePermission("customer.stored_value.topup");

        authenticateActor(new AccessActor("system_scheduler", "system_scheduler", "platform_admin", null, null, "active"));
        service.requirePermission("ops.production_readiness.view");
    }

    // ── XM-M002-BH（治理地图第 303、528、653 行；技术推导）：带商户归属的登录主体，幂等作用域同样按操作人与组织隔离。 ──

    // S1：非商户主体的作用域逐字节不变；商户主体按人、按组织分开，不再是共用的 m:<商户号>。
    @Test
    void resolveKeepsNonMerchantScopesAndSeparatesMerchantCallersByPerson() {
        authenticateActor(new AccessActor("usr_a", "usr_a", "platform_admin", "org", null, "active"));
        assertThat(service.resolve()).isEqualTo("u:5:usr_a:org");
        assertThat(service.legacyScope()).isEqualTo("p");

        String master = SessionIssuer.MASTER_USER_ID;
        authenticateActor(new AccessActor(master, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                "platform-company", null, "active"));
        assertThat(service.resolve()).isEqualTo("u:" + master.length() + ":" + master + ":platform-company");
        assertThat(service.legacyScope()).isEqualTo("p");

        authenticateActor(new AccessActor("cust_x", "cust_x", "customer", null, null, "active"));
        assertThat(service.resolve()).isEqualTo("u:6:cust_x:");
        assertThat(service.legacyScope()).isEqualTo("p");

        authenticateActor(new AccessActor("system_scheduler", "system_scheduler", "platform_admin", null, null, "active"));
        assertThat(service.resolve()).isEqualTo("u:16:system_scheduler:");
        assertThat(service.legacyScope()).isEqualTo("p");

        List<AccessActor> merchantCallers = List.of(
                new AccessActor("usr_a", "usr_a", "staff", "O1", "mch_1", "active"),
                new AccessActor("usr_b", "usr_b", "staff", "O2", "mch_1", "active"),
                new AccessActor("usr_c", "usr_c", "staff", "O1", "mch_1", "active"),
                new AccessActor("usr_a", "usr_a", "staff", "O2", "mch_1", "active"));
        Set<String> scopes = new LinkedHashSet<>();
        for (AccessActor caller : merchantCallers) {
            authenticateActor(caller);
            String scope = service.resolve();
            assertThat(scope).isNotEqualTo("m:mch_1").doesNotStartWith("u:");
            assertThat(service.legacyScope()).isEqualTo("m:mch_1");
            scopes.add(scope);
        }
        assertThat(scopes).hasSize(merchantCallers.size());

        SecurityContextHolder.clearContext();
        assertThat(service.resolve()).isEmpty();
        assertThat(service.legacyScope()).isEmpty();

        authenticateActor(new AccessActor(" ", "blank", "staff", "O1", "mch_1", "active"));
        assertThatThrownBy(() -> service.resolve())
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(401);
                    assertThat(exception.code()).isEqualTo("UNAUTHENTICATED");
                });
    }

    // S2：同商户不同组织、同商户同组织的他人用同一请求编号同体提交都各自执行；同一人重试只认一次；
    // 修复前留下的 m:<商户号> 旧记录由既有守卫拦下（不回放、不执行、不改旧行）。
    @Test
    void sameMerchantDifferentOrganizationCannotReplayAndOldMerchantRecordsAreBlocked() {
        InMemoryIdempotencyRepository records = new InMemoryIdempotencyRepository();
        IdempotencyService idempotency = new IdempotencyService(records, new ObjectMapper());
        String operationId = "userPermissionOverride:set:usr_t";
        String legacyOperationId = "m:mch_1:" + operationId;
        java.util.Map<String, String> body = java.util.Map.of("effect", "deny");
        int[] count = {0};
        java.util.function.Supplier<Integer> action = () -> ++count[0];
        AccessActor a = new AccessActor("usr_a", "usr_a", "staff", "O1", "mch_1", "active");
        AccessActor b = new AccessActor("usr_b", "usr_b", "staff", "O2", "mch_1", "active");
        AccessActor a2 = new AccessActor("usr_c", "usr_c", "staff", "O1", "mch_1", "active");

        // 第 0 步：用修复前的口径（resolve 与 legacyScope 都是 m:mch_1）自然写出一条旧记录
        idempotency.setCallerScopeResolver(() -> "m:mch_1");
        assertThat(idempotency.execute(operationId, "k-old", body, action, Integer.class)).isEqualTo(1);
        IdempotencyRecord oldRecord = records.records.get(legacyOperationId + ":k-old");
        assertThat(oldRecord).isNotNull();
        idempotency.setCallerScopeResolver(service);

        authenticateActor(a);
        assertThat(idempotency.execute(operationId, "k", body, action, Integer.class)).isEqualTo(2);
        authenticateActor(b);
        assertThat(idempotency.execute(operationId, "k", body, action, Integer.class)).isEqualTo(3);
        authenticateActor(a);
        assertThat(idempotency.execute(operationId, "k", body, action, Integer.class)).isEqualTo(2);
        assertThat(count[0]).isEqualTo(3);
        authenticateActor(a2);
        assertThat(idempotency.execute(operationId, "k", body, action, Integer.class)).isEqualTo(4);

        authenticateActor(a);
        assertThatThrownBy(() -> idempotency.execute(operationId, "k-old", body, action, Integer.class))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("IDEMPOTENCY_CONFLICT");
                    assertThat(exception.getMessage()).contains("该请求编号存在旧处理记录");
                });
        assertThat(count[0]).isEqualTo(4);
        assertThat(records.records.get(legacyOperationId + ":k-old")).isEqualTo(oldRecord);

        List<String> operationIds = records.records.values().stream().map(IdempotencyRecord::operationId).toList();
        assertThat(operationIds).filteredOn(legacyOperationId::equals).hasSize(1);
        assertThat(operationIds).filteredOn(id -> !legacyOperationId.equals(id)).hasSize(3)
                .allSatisfy(id -> assertThat(id).startsWith("caller:").hasSize(71));
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

        // XM-M001-AF：读岗位派生事实那一刻触发（模拟此刻别人提交了换项目）
        Runnable onPositionRead;

        @Override
        public java.util.Optional<String> findAssignedPositionRoleId(String userId) {
            if (onPositionRead != null) {
                Runnable hook = onPositionRead;
                onPositionRead = null;
                hook.run();
            }
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

        // XM-M001-AF：员工账号行（默认空 = 主体不在员工表内，不核对）；findUserById 默认按 listUsers 查。
        final java.util.Map<String, AccessUser> users = new java.util.LinkedHashMap<>();
        // XM-M001-AF：员工职级（userId → level_rank）
        final java.util.Map<String, Integer> staffLevels = new java.util.LinkedHashMap<>();

        int accountRowReads; // M722-F05：账号行被读了几次（按号读员工、读身份都经这里）

        @Override
        public List<AccessUser> listUsers() {
            accountRowReads++;
            return List.copyOf(users.values());
        }

        // M722-F05：账号行版本（每改写一次加一）；四项由默认实现从 findUserById 派生。
        final java.util.Map<String, Integer> rowVersions = new java.util.LinkedHashMap<>();

        @Override
        public java.util.Optional<UserIdentity> findUserIdentity(String userId) {
            return AccessRepository.super.findUserIdentity(userId).map(identity -> new UserIdentity(
                    identity.organizationId(), identity.merchantId(), identity.accountType(), identity.status(),
                    String.valueOf(rowVersions.get(userId))));
        }

        // M722-F05：按操作人读权限效果、读职级这两处读取的前后各一个一次性钩子（模拟此刻别人提交了变更），以及读到了什么。
        Runnable beforeEffectsRead;
        Runnable afterEffectsRead;
        Runnable beforeLevelRead;
        Runnable afterLevelRead;
        List<PermissionEffect> lastEffectsRead = List.of();
        int effectsReads;
        Integer lastLevelRead;

        private void runOnce(Runnable hook) {
            if (hook != null) {
                hook.run();
            }
        }

        @Override
        public java.util.Optional<StaffLevelAssignment> findStaffLevelAssignment(String userId) {
            Runnable before = beforeLevelRead;
            beforeLevelRead = null;
            runOnce(before);
            lastLevelRead = staffLevels.get(userId);
            java.util.Optional<StaffLevelAssignment> result = java.util.Optional.ofNullable(lastLevelRead).map(rank -> new StaffLevelAssignment(
                    userId, "lvl_" + rank, null, "CNY", null, null, null, "seed", Instant.parse("2026-06-14T00:00:00Z")));
            Runnable after = afterLevelRead;
            afterLevelRead = null;
            runOnce(after);
            return result;
        }

        @Override
        public java.util.Optional<StaffLevelRecord> findStaffLevelById(String staffLevelId) {
            return staffLevels.values().stream().filter(rank -> ("lvl_" + rank).equals(staffLevelId)).findFirst()
                    .map(rank -> new StaffLevelRecord(staffLevelId, "M4", "lower", rank, null, "active",
                            Instant.parse("2026-06-14T00:00:00Z")));
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
            Runnable before = beforeEffectsRead;
            beforeEffectsRead = null;
            runOnce(before);
            lastEffectsRead = readEffects(userId);
            effectsReads++;
            Runnable after = afterEffectsRead;
            afterEffectsRead = null;
            runOnce(after);
            return lastEffectsRead;
        }

        private List<PermissionEffect> readEffects(String userId) {
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
