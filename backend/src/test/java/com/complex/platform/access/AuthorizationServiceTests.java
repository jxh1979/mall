package com.complex.platform.access;

// 文件版本：V0.11

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.auth.AuthService;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.common.idempotency.IdempotentReplayContext;
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

    // M723-F09（治理地图第 656 行「操作被拒绝或失败的不算用掉」）：控制器请求上下文里本次要消耗单次来源（写请求，或带仍扣码的查看请求
    // 如导出、商户列表——同根）、当前没有实际事务时——请求进入时不持有、判定期间才被授出，消耗无法随请求结果回滚——不消耗、按不具备处理：
    // 403 PERMISSION_DENIED，来源仍未消耗。对照：没有请求上下文（服务层直调、测试）照常消耗。修复前第一处就消耗了（consumedSourceIds 非空、不抛 403）。
    @Test
    void aSingleUseGrantFoundDuringAWriteRequestWithoutATransactionIsNotConsumed() {
        authenticate("usr_staff", "staff", "staff");
        repository.addEffect(new AccessRepository.PermissionEffect(
                "usr_staff",
                "ops.production_readiness.manage",
                AccessRepository.Effect.GRANT,
                AccessRepository.Source.TEMPLATE_APPLICATION,
                "application_granted_mid_request",
                null,
                null,
                true,
                false
        ));
        IdempotentReplayContext previous = IdempotentReplayContext.enter("key-f09", "digest-f09");
        try {
            assertThatThrownBy(() -> service.requirePermission("ops.production_readiness.manage"))
                    .isInstanceOfSatisfying(ApiException.class, exception -> {
                        assertThat(exception.status().value()).isEqualTo(403);
                        assertThat(exception.code()).isEqualTo("PERMISSION_DENIED");
                    });
        } finally {
            IdempotentReplayContext.restore(previous);
        }
        assertThat(repository.consumedSourceIds).isEmpty();

        // 同根：带仍扣码的查看请求（导出一类，上下文只读但该码在 consumedOnRead 内）无事务时同样不消耗
        IdempotentReplayContext previousRead = IdempotentReplayContext.enter(
                "key-f09-read", "digest-f09-read", true, java.util.Set.of("ops.production_readiness.manage"));
        try {
            assertThatThrownBy(() -> service.requirePermission("ops.production_readiness.manage"))
                    .isInstanceOfSatisfying(ApiException.class, exception -> {
                        assertThat(exception.status().value()).isEqualTo(403);
                        assertThat(exception.code()).isEqualTo("PERMISSION_DENIED");
                    });
        } finally {
            IdempotentReplayContext.restore(previousRead);
        }
        assertThat(repository.consumedSourceIds).isEmpty();

        service.requirePermission("ops.production_readiness.manage");
        assertThat(repository.consumedSourceIds).containsExactly("application_granted_mid_request");
    }

    // M723-F09 同根：请求事务切面没有给本请求开请求事务（进入时不持有单次来源，上下文记了「无请求事务」）时，即使核验发生在业务层
    // 自己的事务里（当前有实际事务），判定期间才被授出的单次来源同样不消耗、按不具备处理——那个事务的提交不受切面的重放回滚与失败撤回
    // 约束。对照：同样有实际事务、上下文没有记「无请求事务」（切面的请求事务，或进入前已有的外层事务）照常消耗。
    // 修复前第一处就消耗了（闸只看有没有实际事务）。
    @Test
    void aSingleUseGrantFoundInsideAServiceTransactionOfARequestWithoutARequestTransactionIsNotConsumed() {
        authenticate("usr_staff", "staff", "staff");
        repository.addEffect(new AccessRepository.PermissionEffect(
                "usr_staff",
                "ops.production_readiness.manage",
                AccessRepository.Effect.GRANT,
                AccessRepository.Source.TEMPLATE_APPLICATION,
                "application_granted_mid_service_tx",
                null,
                null,
                true,
                false
        ));
        org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            IdempotentReplayContext previous = IdempotentReplayContext.enter("key-f09-svc", "digest-f09-svc");
            try {
                IdempotentReplayContext.markWithoutRequestTransaction();
                assertThatThrownBy(() -> service.requirePermission("ops.production_readiness.manage"))
                        .isInstanceOfSatisfying(ApiException.class, exception -> {
                            assertThat(exception.status().value()).isEqualTo(403);
                            assertThat(exception.code()).isEqualTo("PERMISSION_DENIED");
                        });
            } finally {
                IdempotentReplayContext.restore(previous);
            }
            assertThat(repository.consumedSourceIds).isEmpty();

            IdempotentReplayContext previousInTransaction = IdempotentReplayContext.enter("key-f09-svc-2", "digest-f09-svc-2");
            try {
                service.requirePermission("ops.production_readiness.manage");
            } finally {
                IdempotentReplayContext.restore(previousInTransaction);
            }
            assertThat(repository.consumedSourceIds).containsExactly("application_granted_mid_service_tx");
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(false);
        }
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
        // M723：授出依据集同样在读权限前后核身份
        assertThatThrownBy(() -> service.grantablePermissions())
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.code()).isEqualTo("PERMISSION_DENIED"));

        afRow("usr_o", "staff", "org", "merchant", "active");
        assertThat(service.effectivePermissions().grants()).contains(AF_CODE);
        assertThat(service.grantablePermissions().grants()).doesNotContain(AF_CODE); // 单次来源不作授出依据
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

    // grantablePermissions（M723 新增入口）：拒绝（403 PERMISSION_DENIED）。中间范围的授予用角色来源——单次来源本就不作授出依据。
    @Test
    void grantablePermissionsRejectsScopeRoundTrip() {
        AccessRepository.PermissionEffect grant = f05RoleGrant(AF_CODE);
        f05ArmRoundTripAroundEffectsRead("org_b", "merchant", grant);

        assertThatThrownBy(() -> service.grantablePermissions())
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

    // ── M722-F06：currentSupervisorLevel 的最高权限分支同样走判定窗口（没有账号行时仍是 3，见 currentSupervisorLevelIsThreeOnlyForMasterAuthority）──

    private void f06MasterSession() {
        authenticateActor(new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active"));
    }

    // 最高权限账号行被写一次（再次 U 盘登录的 upsert 即使四项不变也会改写该行）。
    private void f06MasterRow(String organizationId) {
        repository.users.put(SessionIssuer.MASTER_USER_ID, new AccessUser(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, organizationId, null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        repository.rowVersions.merge(SessionIssuer.MASTER_USER_ID, 1, Integer::sum);
    }

    // 有账号行、四项与行版本全程未变：仍是 3（修复前后都绿）。
    @Test
    void currentSupervisorLevelIsThreeForMasterAuthorityWithAStableAccountRow() {
        f06MasterSession();
        f06MasterRow("platform-company");

        assertThat(service.currentSupervisorLevel()).isEqualTo(3);
    }

    // 无仓储的轻量上下文：不核对，仍是 3（修复前后都绿）。
    @Test
    void currentSupervisorLevelIsThreeForMasterAuthorityWithoutARepository() {
        f06MasterSession();

        assertThat(new AuthorizationService(null).currentSupervisorLevel()).isEqualTo(3);
    }

    // 开始时账号行与会话不一致：0，且不读权限效果、不读职级。
    @Test
    void currentSupervisorLevelIsZeroForMasterAuthorityWhenTheAccountRowDiffersAtStart() {
        f06MasterSession();
        f06MasterRow("org_b");
        repository.staffLevels.put(SessionIssuer.MASTER_USER_ID, 12);

        assertThat(service.currentSupervisorLevel()).isZero();
        assertThat(repository.effectsReads).isZero();
        assertThat(repository.lastLevelRead).isNull();
    }

    // 开始与末尾两次读账号行之间该行被改写、四项不变（本人再次 U 盘登录）：0。
    @Test
    void currentSupervisorLevelIsZeroForMasterAuthorityWhenTheAccountRowIsRewrittenDuringTheJudgment() {
        f06MasterSession();
        f06MasterRow("platform-company");
        repository.afterIdentityRead = () -> f06MasterRow("platform-company");

        assertThat(service.currentSupervisorLevel()).isZero();
        assertThat(repository.rowVersions.get(SessionIssuer.MASTER_USER_ID)).isEqualTo(2); // 改写确在判定内发生
        assertThat(repository.users.get(SessionIssuer.MASTER_USER_ID).organizationId()).isEqualTo("platform-company"); // 四项仍与会话相同
        assertThat(repository.afterIdentityRead).isNull();
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

    // ── XM-M002-BH、M722-F07（治理地图第 303、528、653 行；操作员 2026-10-03 裁决「同项目同商户内认一次」）：
    //    带商户归属的登录主体，幂等作用域按所属项目＋所属商户隔离。 ──

    // S1：非商户主体的作用域逐字节不变；商户主体按所属项目＋所属商户分开（同项目同商户的两人同一个作用域），不再是共用的 m:<商户号>。
    @Test
    void resolveKeepsNonMerchantScopesAndSeparatesMerchantCallersByProjectAndMerchant() {
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
                new AccessActor("usr_a", "usr_a", "staff", "O2", "mch_1", "active"),
                new AccessActor("usr_d", "usr_d", "staff", "O1", "mch_2", "active"));
        List<String> scopes = new java.util.ArrayList<>();
        for (AccessActor caller : merchantCallers) {
            authenticateActor(caller);
            String scope = service.resolve();
            assertThat(scope).isNotEqualTo("m:mch_1").isNotEqualTo("m:mch_2").doesNotStartWith("u:");
            assertThat(service.legacyScope()).isEqualTo("m:" + caller.merchantId());
            scopes.add(scope);
        }
        // 同项目同商户的两人同一个作用域；项目不同或商户不同即分开。形态：mo:<项目长度>:<项目>:<商户>，不含账号编号
        assertThat(scopes.get(0)).isEqualTo("mo:2:O1:mch_1");
        assertThat(scopes.get(1)).isEqualTo("mo:2:O2:mch_1");
        assertThat(scopes.get(4)).isEqualTo("mo:2:O1:mch_2");
        assertThat(scopes.get(2)).isEqualTo(scopes.get(0));
        assertThat(scopes.get(3)).isEqualTo(scopes.get(1));
        assertThat(new LinkedHashSet<>(scopes)).hasSize(3);

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

    // S2：同商户不同项目的人用同一请求编号同体提交——M724（操作员 2026-10-04 裁决「同一提交编号只能提交一次不允许多次提交」）：
    // 被拒（409 IDEMPOTENCY_KEY_USED），不执行、拿不到别人的结果（修复前各自执行）；同项目同商户的他人只认一次、拿到第一次的结果；
    // 同一人重试只认一次；修复前留下的 m:<商户号> 旧记录由既有守卫拦下（不回放、不执行、不改旧行）。
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

        // 第 0 步：直接预置一条修复前形态（m:<商户号>:<操作号>）的旧记录——M724 起行键不含范围，解析器已写不出这种形态
        IdempotencyRecord oldRecord = new IdempotencyRecord(java.util.UUID.randomUUID(), legacyOperationId, "k-old", "x",
                "OK", "1", 200, Instant.now(), Instant.now().plusSeconds(86_400));
        records.records.put(legacyOperationId + ":k-old", oldRecord);
        idempotency.setCallerScopeResolver(service);

        authenticateActor(a);
        assertThat(idempotency.execute(operationId, "k", body, action, Integer.class)).isEqualTo(1);
        authenticateActor(b); // 同商户、另一项目：该编号已被用掉，被拒、不执行
        assertThatThrownBy(() -> idempotency.execute(operationId, "k", body, action, Integer.class))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(409);
                    assertThat(exception.code()).isEqualTo("IDEMPOTENCY_KEY_USED");
                });
        authenticateActor(a);
        assertThat(idempotency.execute(operationId, "k", body, action, Integer.class)).isEqualTo(1);
        assertThat(count[0]).isEqualTo(1);
        authenticateActor(a2); // 与 a 同项目同商户：不再执行，拿到 a 第一次的结果
        assertThat(idempotency.execute(operationId, "k", body, action, Integer.class)).isEqualTo(1);
        assertThat(count[0]).isEqualTo(1);

        authenticateActor(a);
        assertThatThrownBy(() -> idempotency.execute(operationId, "k-old", body, action, Integer.class))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("IDEMPOTENCY_CONFLICT");
                    assertThat(exception.getMessage()).contains("该请求编号存在旧处理记录");
                });
        assertThat(count[0]).isEqualTo(1);
        assertThat(records.records.get(legacyOperationId + ":k-old")).isEqualTo(oldRecord);

        // 该编号（k）恰一行：行键不含范围（once:＋操作号散列，长 69），请求散列 128 位（范围散列＋请求体散列）
        List<String> operationIds = records.records.values().stream().map(IdempotencyRecord::operationId).toList();
        assertThat(operationIds).filteredOn(legacyOperationId::equals).hasSize(1);
        assertThat(operationIds).filteredOn(id -> !legacyOperationId.equals(id))
                .containsExactly(IdempotencyService.storedOperationId(operationId))
                .allSatisfy(id -> assertThat(id).hasSize(69));
        assertThat(records.records.get(IdempotencyService.storedOperationId(operationId) + ":k").requestHash()).hasSize(128);
    }

    // ── M736（XM-M001-D 可跨项目角色·批 1 权限治理；操作员 2026-10-01 确认的治理地图第 653 行追加句：「被分配了可跨项目角色的员工
    //    （含经岗位得到），在所分配的权限内可以查看、处理该角色可查看项目的数据。」）：统一判定 projectReach 及其派生判断。
    //    真实判定＋内存仓，不给判定打桩。操作人＝xpActor(...) 写入的主体（staff、本人项目 org_a、无商户、active）；角色按编号放在
    //    repository.roles，「持有」用 ROLE 来源、来源号＝角色编号的授予表示；当时的项目集合＝repository.projectIds。 ──

    private static final String XP_ASSIGN = "access.role.assign";
    private static final String XP_APPLY = "access.grant_request.apply";

    private AccessActor xpActor(String id) {
        AccessActor actor = new AccessActor(id, id, "staff", "org_a", null, "active");
        authenticateActor(actor);
        return actor;
    }

    // viewAll＝勾了「可以查看全部项目」；ticked＝展开后逐个勾选的项目；两项都没有的是普通角色。
    private void xpRole(String roleId, String status, boolean viewAll, String... ticked) {
        repository.roles.put(roleId, new RoleRecord(roleId, roleId, null, "project", status,
                Instant.parse("2026-06-14T00:00:00Z"), List.of(XP_ASSIGN), null, viewAll, List.of(ticked)));
    }

    private AccessRepository.PermissionEffect xpGrant(String userId, String code, AccessRepository.Source source, String sourceId) {
        AccessRepository.PermissionEffect grant = new AccessRepository.PermissionEffect(userId, code, AccessRepository.Effect.GRANT,
                source, sourceId, null, null, false, false);
        repository.addEffect(grant);
        return grant;
    }

    private static AccessRepository.PermissionEffect xpDeny(AccessRepository.Source source) {
        return new AccessRepository.PermissionEffect("usr_xp", XP_ASSIGN, AccessRepository.Effect.DENY, source, "deny_" + source,
                null, null, false, false);
    }

    // M736-U2（同一追加句：「逐个勾选的只能查看被选中的项目」；操作员 2026-10-01 说明「没被选中的对应的角色不能看」）：权限码只来自
    // 只勾了乙项目的角色——乙项目可、丙项目拒，本人项目没被勾中也拒；再经普通角色持该码，本人项目与乙项目都可；撤掉普通角色，
    // 本人项目又拒（第二次）；角色同时勾本人项目与乙项目，两个都可；之后新建的项目没被勾中，拒。
    @Test
    void tickedRoleReachesOnlyTheTickedProjectsAndTheOwnProjectOnlyWithAnotherSource() {
        repository.projectIds.addAll(List.of("org_a", "org_b", "org_c"));
        AccessActor actor = xpActor("usr_xp");
        xpRole("role_xp_cross", "active", false, "org_b");
        xpGrant("usr_xp", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_cross");

        AuthorizationService.ProjectReach ticked = service.projectReach(actor, XP_ASSIGN);
        assertThat(ticked.covers("org_b")).isTrue();
        assertThat(ticked.covers("org_c")).isFalse();
        assertThat(ticked.covers("org_a")).as("本人项目没被勾中，又没有别的来源").isFalse();
        assertThat(ticked.onlyOwnProject()).isEmpty();
        assertThat(service.canReachProject(actor, "org_b", XP_ASSIGN)).isTrue();
        assertThat(service.canReachProject(actor, "org_c", XP_ASSIGN)).isFalse();

        xpRole("role_xp_plain", "active", false);
        AccessRepository.PermissionEffect plain = xpGrant("usr_xp", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_plain");
        AuthorizationService.ProjectReach withPlain = service.projectReach(actor, XP_ASSIGN);
        assertThat(List.of(withPlain.covers("org_a"), withPlain.covers("org_b"), withPlain.covers("org_c")))
                .containsExactly(true, true, false);

        repository.effects.remove(plain);
        assertThat(service.projectReach(actor, XP_ASSIGN).covers("org_a")).isFalse();

        xpRole("role_xp_cross", "active", false, "org_a", "org_b");
        repository.projectIds.add("org_n");
        AuthorizationService.ProjectReach both = service.projectReach(actor, XP_ASSIGN);
        assertThat(List.of(both.covers("org_a"), both.covers("org_b"), both.covers("org_n"))).containsExactly(true, true, false);
    }

    // M736-U3（操作员 2026-09-25「在他分配的权限内」；治理地图第 528 行追加句「在该角色所含权限内」）：范围按权限码判——可跨项目角色
    // 只带分配角色权限，按申请权限判时不跨；同一个人经员工单独授予、模板套用、临时授权持有的码也只到本人项目。
    @Test
    void reachIsJudgedPerPermissionCodeAndOnlyThroughCrossProjectRoles() {
        repository.projectIds.addAll(List.of("org_a", "org_b"));
        AccessActor actor = xpActor("usr_xp");
        xpRole("role_xp_all", "active", true);
        xpGrant("usr_xp", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_all");
        AccessUser inB = new AccessUser("usr_b", "usr_b", "staff", "org_b", null, "active", Instant.parse("2026-06-14T00:00:00Z"));

        assertThat(service.canReachUser(actor, inB, XP_ASSIGN)).isTrue();
        assertThat(service.projectReach(actor, XP_APPLY).onlyOwnProject()).contains("org_a");
        assertThat(service.canReachUser(actor, inB, XP_APPLY)).as("角色里没有的码").isFalse();

        for (AccessRepository.Source source : List.of(AccessRepository.Source.USER_OVERRIDE,
                AccessRepository.Source.TEMPLATE_APPLICATION, AccessRepository.Source.TEMPORARY_GRANT)) {
            AccessRepository.PermissionEffect own = xpGrant("usr_xp", XP_APPLY, source, "src_" + source);
            assertThat(service.projectReach(actor, XP_APPLY).onlyOwnProject()).as(source.name()).contains("org_a");
            assertThat(service.canReachUser(actor, inB, XP_APPLY)).as(source.name()).isFalse();
            repository.effects.remove(own);
        }
    }

    // M736-U4（治理地图第 655 行「显式拒绝优先级最高」；M002-F33 岗位高危挂起只压岗位派生的那一份）：该码被员工显式拒绝——不跨，解除后
    // 恢复（第二次）；角色停用——不跨；岗位高危挂起中，岗位派生的可跨项目角色不计，另行直接分配的同一角色照常计，挂起解除后恢复。
    @Test
    void explicitDenyDisabledRoleAndPositionHoldDoNotCountTowardsTheReach() {
        repository.projectIds.addAll(List.of("org_a", "org_b"));
        AccessActor actor = xpActor("usr_xp");
        xpRole("role_xp_cross", "active", false, "org_b");
        xpGrant("usr_xp", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_cross");
        assertThat(service.projectReach(actor, XP_ASSIGN).covers("org_b")).isTrue();

        AccessRepository.PermissionEffect deny = xpDeny(AccessRepository.Source.USER_OVERRIDE);
        repository.addEffect(deny);
        assertThat(service.projectReach(actor, XP_ASSIGN).covers("org_b")).as("显式拒绝").isFalse();
        repository.effects.remove(deny);
        assertThat(service.projectReach(actor, XP_ASSIGN).covers("org_b")).isTrue();

        xpRole("role_xp_cross", "inactive", false, "org_b");
        assertThat(service.projectReach(actor, XP_ASSIGN).covers("org_b")).as("角色停用").isFalse();
        xpRole("role_xp_cross", "active", false, "org_b");

        AccessRepository.PermissionEffect hold = xpDeny(AccessRepository.Source.POSITION_HOLD);
        repository.addEffect(hold);
        repository.positionRoles.put("usr_xp", "role_xp_cross");
        assertThat(service.projectReach(actor, XP_ASSIGN).covers("org_b")).as("挂起中的岗位派生来源").isFalse();
        repository.directRoles.put("usr_xp", List.of("role_xp_cross"));
        assertThat(service.projectReach(actor, XP_ASSIGN).covers("org_b")).as("另行直接分配的同一角色").isTrue();
        repository.directRoles.remove("usr_xp");
        repository.effects.remove(hold);
        assertThat(service.projectReach(actor, XP_ASSIGN).covers("org_b")).as("挂起解除后").isTrue();
    }

    // M736-U5（操作员 2026-09-25「商户账号。是不能跨项目的。」；治理地图第 675 行外包账号「也不能跨出本综合体」）：带商户归属的账号、
    // 外包账号持勾了「可以查看全部项目」的角色仍只到本人项目，也不算看得到全平台合计；同一角色分给同项目的普通员工则跨。
    @Test
    void merchantAndOutsourcedAccountsStayInTheirOwnProject() {
        repository.projectIds.addAll(List.of("org_a", "org_b"));
        xpRole("role_xp_all", "active", true);
        AccessUser inB = new AccessUser("usr_b", "usr_b", "staff", "org_b", null, "active", Instant.parse("2026-06-14T00:00:00Z"));
        java.util.Map<String, AccessActor> holders = new java.util.LinkedHashMap<>();
        holders.put("商户账号", new AccessActor("usr_xp_merchant", "usr_xp_merchant", "merchant_admin", "org_a", "merchant_a", "active"));
        holders.put("带商户归属的员工", new AccessActor("usr_xp_mstaff", "usr_xp_mstaff", "staff", "org_a", "merchant_a", "active"));
        holders.put("外包账号", new AccessActor("usr_xp_vendor", "usr_xp_vendor", "outsourced_vendor", "org_a", null, "active"));
        holders.forEach((label, holder) -> {
            xpGrant(holder.id(), XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_all");
            authenticateActor(holder);
            AuthorizationService.ProjectReach reach = service.projectReach(holder, XP_ASSIGN);
            assertThat(reach.onlyOwnProject()).as(label).contains("org_a");
            assertThat(service.canReachUser(reach, inB)).as(label).isFalse();
            assertThat(service.allowsPlatformScope(holder, XP_ASSIGN)).as(label).isFalse();
        });

        AccessActor staff = xpActor("usr_xp");
        xpGrant("usr_xp", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_all");
        assertThat(service.canReachUser(staff, inB, XP_ASSIGN)).isTrue();
    }

    // M736-U11（复核 I-1；治理地图第 651 行三类申请「只能由具备分配/取消分配角色权限的人发起」，入口两码一起核）：入口要求多个码时
    // 取各码范围的交集——申请权限来自勾了全部项目的角色、分配角色权限只来自本项目的普通角色，两码一起只到本人项目；两码都来自
    // 可跨项目角色时取勾选的交集。
    @Test
    void reachForSeveralCodesIsTheIntersectionOfEachCodesReach() {
        repository.projectIds.addAll(List.of("org_a", "org_b", "org_c"));
        AccessActor actor = xpActor("usr_xp");
        xpRole("role_xp_all", "active", true);
        xpRole("role_xp_plain", "active", false);
        xpGrant("usr_xp", XP_APPLY, AccessRepository.Source.ROLE, "role_xp_all");
        AccessRepository.PermissionEffect plain = xpGrant("usr_xp", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_plain");

        assertThat(service.projectReach(actor, XP_APPLY).covers("org_b")).isTrue();
        AuthorizationService.ProjectReach both = service.projectReach(actor, XP_APPLY, XP_ASSIGN);
        assertThat(List.of(both.covers("org_a"), both.covers("org_b"), both.covers("org_c"))).containsExactly(true, false, false);
        assertThat(both.onlyOwnProject()).contains("org_a");
        assertThat(both.allProjects()).isFalse();

        repository.effects.remove(plain);
        xpRole("role_xp_b", "active", false, "org_b");
        xpGrant("usr_xp", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_b");
        AuthorizationService.ProjectReach ticked = service.projectReach(actor, XP_APPLY, XP_ASSIGN);
        assertThat(List.of(ticked.covers("org_a"), ticked.covers("org_b"), ticked.covers("org_c"))).containsExactly(false, true, false);
    }

    // M736-U15（操作员 2026-10-03 所选选项原文：「全平台合计只给平台管理员和勾了「全部项目」的人看。李四只能分别看 A、B 各自的数据。」；
    // 2026-10-01 所选选项原文：「「全部项目」始终指所有项目，含以后新建的；只有展开后逐个勾选的，才只管勾中的那几个。」）：
    // 平台管理员——真，且不读能力事实；经勾了全部的角色持该码——真，之后新建的项目自动在范围内，角色里没有的码——假；
    // 只逐个勾选的——假（哪怕把现有项目全勾上），新建的项目不在范围内；普通员工——假。
    @Test
    void platformScopeIsOnlyForPlatformAdminsAndViewAllHolders() {
        repository.projectIds.addAll(List.of("org_a", "org_b"));
        xpRole("role_xp_all", "active", true);
        xpRole("role_xp_ticked", "active", false, "org_a", "org_b");

        AccessActor admin = new AccessActor("usr_admin", "usr_admin", "platform_admin", "org", null, "active");
        authenticateActor(admin);
        assertThat(service.allowsPlatformScope(admin, XP_ASSIGN)).isTrue();
        assertThat(service.projectReach(admin, XP_ASSIGN).covers("platform-company")).isTrue();
        assertThat(repository.effectsReads).as("平台管理员不读能力事实").isZero();

        AccessActor all = xpActor("usr_xp_all");
        xpGrant("usr_xp_all", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_all");
        assertThat(service.allowsPlatformScope(all, XP_ASSIGN)).isTrue();
        assertThat(service.allowsPlatformScope(all, XP_APPLY)).as("角色里没有的码").isFalse();
        repository.projectIds.add("org_n");
        assertThat(service.projectReach(all, XP_ASSIGN).covers("org_n")).as("以后新建的项目").isTrue();

        AccessActor ticked = xpActor("usr_xp_ticked");
        xpGrant("usr_xp_ticked", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_ticked");
        assertThat(service.allowsPlatformScope(ticked, XP_ASSIGN)).isFalse();
        assertThat(service.projectReach(ticked, XP_ASSIGN).covers("org_b")).isTrue();
        assertThat(service.projectReach(ticked, XP_ASSIGN).covers("org_n")).isFalse();

        assertThat(service.allowsPlatformScope(xpActor("usr_xp_plain"), XP_ASSIGN)).isFalse();
    }

    // M736-U16（M722-F05）：算可触及项目要按操作人读能力事实，同样走判定窗口——读取期间操作人的项目被改走又改回（账号行被改写），
    // 403 PERMISSION_DENIED「账号归属或状态已变更，本次操作未执行」；开始时账号行就与会话不一致的，不读能力事实即拒绝。
    // 对照：身份全程未变时照常算出范围。
    @Test
    void projectReachRejectsWhenTheActorsAccountRowChangesDuringTheJudgment() {
        repository.projectIds.addAll(List.of("org_a", "org_b"));
        AccessActor actor = xpActor("usr_xp");
        xpRole("role_xp_cross", "active", false, "org_b");
        xpGrant("usr_xp", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_cross");
        afRow("usr_xp", "staff", "org_a", null, "active");
        assertThat(service.projectReach(actor, XP_ASSIGN).covers("org_b")).isTrue();

        repository.beforeEffectsRead = () -> afRow("usr_xp", "staff", "org_b", null, "active");
        repository.afterEffectsRead = () -> afRow("usr_xp", "staff", "org_a", null, "active");
        assertThatThrownBy(() -> service.projectReach(actor, XP_ASSIGN))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(403);
                    assertThat(exception.code()).isEqualTo("PERMISSION_DENIED");
                    assertThat(exception.getMessage()).contains("账号归属或状态已变更，本次操作未执行");
                });
        assertThat(repository.users.get("usr_xp").organizationId()).isEqualTo("org_a"); // 改回后四项与会话相同，只是账号行被改写过
        assertThat(repository.rowVersions.get("usr_xp")).isEqualTo(3);

        afRow("usr_xp", "staff", "org_b", null, "active");
        int reads = repository.effectsReads;
        assertThatThrownBy(() -> service.projectReach(actor, XP_ASSIGN))
                .isInstanceOfSatisfying(ApiException.class, exception -> assertThat(exception.code()).isEqualTo("PERMISSION_DENIED"));
        assertThat(repository.effectsReads).isEqualTo(reads);
    }

    // ── M736 第二步（操作员 2026-10-03 所选选项原文：「直接在原角色上勾 C；但张三等所有持有人要各自重新审批通过，才能看 C。」）：
    //    角色分出去以后又加上的可查看项目，改之前已持有该角色的人在重新审批通过之前不算——判定按（员工，角色）减掉他的待批条目
    //    （以下各条里「减掉以后一条不剩时这个角色什么都不给、只有带保留标记的人按没勾选的角色算」「全部那一勾待批时按留着的清单算」
    //    是技术推导）。
    //    待批条目放在 repository.viewableHolds（员工 → 角色 → 条目），「可以查看全部项目」那一勾的待批记作 XP_ALL。
    //    本组只用既有接口与本文件内存仓自己的字段，对只有第一步的主源码也能编译：换上本文件即可红跑，红在各条注明的断言上。 ──

    private static final String XP_ALL = "*"; // ＝AuthorizationService.ALL_PROJECTS；写字面量是为了对只有第一步的主源码也能编译
    // ＝AuthorizationService.HELD_SINCE_UNTICKED：保留标记（角色原来一条勾选都没有、第一次勾上项目时已持有它的人各记一条），同样写字面量
    private static final String XP_SINCE_UNTICKED = "-";

    private void xpHold(String userId, String roleId, String... viewableIds) {
        repository.viewableHolds.computeIfAbsent(userId, ignored -> new java.util.LinkedHashMap<>()).put(roleId, Set.of(viewableIds));
    }

    // M736-Z1（同上选项原文）：角色勾了乙、丙，某人对丙有待批——乙照常可达、丙不可达；同一角色的另一位持有人（没有待批）丙可达；
    // 待批删掉（重新审批通过）以后丙可达。修复前判定不看待批，红在「待重新审批的项目」那条。
    @Test
    void heldProjectIsOutOfReachUntilReapprovedWhileOtherTicksStay() {
        repository.projectIds.addAll(List.of("org_a", "org_b", "org_c"));
        xpRole("role_xp_cross", "active", false, "org_b", "org_c");
        xpGrant("usr_xp", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_cross");
        xpGrant("usr_xp_other", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_cross");
        xpHold("usr_xp", "role_xp_cross", "org_c");

        AuthorizationService.ProjectReach held = service.projectReach(xpActor("usr_xp"), XP_ASSIGN);
        assertThat(held.covers("org_b")).isTrue();
        assertThat(held.covers("org_c")).as("待重新审批的项目").isFalse();
        assertThat(service.canReachProject(xpActor("usr_xp"), "org_c", XP_ASSIGN)).isFalse();
        assertThat(service.projectReach(xpActor("usr_xp_other"), XP_ASSIGN).covers("org_c")).as("同一角色的另一位持有人").isTrue();

        repository.viewableHolds.remove("usr_xp");
        assertThat(service.projectReach(xpActor("usr_xp"), XP_ASSIGN).covers("org_c")).as("重新审批通过后").isTrue();
    }

    // M736-Z2（技术推导，依据同上选项原文与 2026-10-08 确认句「增加的，所有持有人要各自重新审批通过，才能查看新增的项目」）：
    // 角色勾了「可以查看全部项目」、清单里留着乙，某人对「全部」那一勾有待批——只到乙，本人项目没被勾中也不到，不算看得到全平台合计；
    // 待批去掉后看全部（含之后才加进项目集合的），算看得到全平台合计。修复前红在第一条 containsExactly（丙可达）。
    @Test
    void heldAllEntryFallsBackToTheTickedList() {
        repository.projectIds.addAll(List.of("org_a", "org_b", "org_c"));
        xpRole("role_xp_all", "active", true, "org_b");
        AccessActor actor = xpActor("usr_xp");
        xpGrant("usr_xp", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_all");
        xpHold("usr_xp", "role_xp_all", XP_ALL);

        AuthorizationService.ProjectReach held = service.projectReach(actor, XP_ASSIGN);
        assertThat(List.of(held.covers("org_a"), held.covers("org_b"), held.covers("org_c"))).containsExactly(false, true, false);
        assertThat(held.allProjects()).isFalse();
        assertThat(service.allowsPlatformScope(actor, XP_ASSIGN)).isFalse();

        repository.viewableHolds.remove("usr_xp");
        repository.projectIds.add("org_n");
        AuthorizationService.ProjectReach all = service.projectReach(actor, XP_ASSIGN);
        assertThat(List.of(all.covers("org_c"), all.covers("org_n"))).containsExactly(true, true);
        assertThat(service.allowsPlatformScope(actor, XP_ASSIGN)).isTrue();
    }

    // M736-Z3（技术推导：否则第一次勾上项目的角色会让老持有人丢掉本人项目）：原来没勾选的角色第一次勾上丙，老持有人对丙有待批、
    // 并带着那一次记下的保留标记——这个角色对他仍按没勾选的角色算：只到本人项目、到不了丙；他同一权限码另有勾了乙的可跨项目角色时，
    // 本人项目与乙都可达、丙仍不可达。修复前红在 onlyOwnProject 那条（本人项目丢了、丙可达）。
    @Test
    void roleWhoseOnlyTicksAreHeldCountsAsOrdinaryForThatHolder() {
        repository.projectIds.addAll(List.of("org_a", "org_b", "org_c"));
        AccessActor actor = xpActor("usr_xp");
        xpRole("role_xp_first", "active", false, "org_c");
        xpGrant("usr_xp", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_first");
        xpHold("usr_xp", "role_xp_first", "org_c", XP_SINCE_UNTICKED);

        AuthorizationService.ProjectReach own = service.projectReach(actor, XP_ASSIGN);
        assertThat(own.onlyOwnProject()).contains("org_a");
        assertThat(own.covers("org_c")).isFalse();

        xpRole("role_xp_b", "active", false, "org_b");
        xpGrant("usr_xp", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_b");
        AuthorizationService.ProjectReach both = service.projectReach(actor, XP_ASSIGN);
        assertThat(List.of(both.covers("org_a"), both.covers("org_b"), both.covers("org_c"))).containsExactly(true, true, false);
    }

    // M736-Z4（操作员 2026-10-01 确认的治理地图第 653 行追加句「被分配了可跨项目角色的员工（含经岗位得到）」）：直接分配的与经岗位
    // 得到的，同样的待批得到同样的范围。修复前红在第一条 containsExactly（丙可达）。
    @Test
    void holdsApplyAlikeToDirectAndPositionDerivedHolders() {
        repository.projectIds.addAll(List.of("org_a", "org_b", "org_c"));
        xpRole("role_xp_cross", "active", false, "org_b", "org_c");
        xpGrant("usr_xp_direct", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_cross");
        repository.directRoles.put("usr_xp_direct", List.of("role_xp_cross"));
        xpGrant("usr_xp_seat", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_cross");
        repository.positionRoles.put("usr_xp_seat", "role_xp_cross");
        xpHold("usr_xp_direct", "role_xp_cross", "org_c");
        xpHold("usr_xp_seat", "role_xp_cross", "org_c");

        AuthorizationService.ProjectReach direct = service.projectReach(xpActor("usr_xp_direct"), XP_ASSIGN);
        AuthorizationService.ProjectReach seat = service.projectReach(xpActor("usr_xp_seat"), XP_ASSIGN);
        assertThat(List.of(direct.covers("org_b"), direct.covers("org_c"))).containsExactly(true, false);
        assertThat(seat).as("经岗位得到的与直接分配的同样").isEqualTo(direct);
    }

    // M736-Z5（待批按「员工，角色」记，不按员工记）：两个角色都勾了丙，只对其中一个有待批——只持那一个时丙不可达，再持另一个时丙可达。
    // 修复前红在第一条（丙当场可达）；实现成按员工减时红在第二条。
    @Test
    void holdOnOneRoleDoesNotMaskTheSameProjectTickedByAnotherRole() {
        repository.projectIds.addAll(List.of("org_a", "org_b", "org_c"));
        AccessActor actor = xpActor("usr_xp");
        xpRole("role_xp_held", "active", false, "org_c");
        xpRole("role_xp_free", "active", false, "org_c");
        xpGrant("usr_xp", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_held");
        xpHold("usr_xp", "role_xp_held", "org_c");
        assertThat(service.projectReach(actor, XP_ASSIGN).covers("org_c")).isFalse();

        xpGrant("usr_xp", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_free");
        assertThat(service.projectReach(actor, XP_ASSIGN).covers("org_c")).as("另一个角色勾中的同一项目").isTrue();
    }

    // M736-Z6（待批只在碰到可跨项目角色时读、一次判定最多读一次）：只持普通角色的员工、平台管理员、带商户归属的账号各判一次——
    // 读 0 次；持两个可跨项目角色、入口核两个权限码的人判一次——读 1 次。修复前红在最后一条（读 0 次）。
    @Test
    void holdsAreReadAtMostOnceAndOnlyForCrossProjectRoles() {
        repository.projectIds.addAll(List.of("org_a", "org_b"));
        xpRole("role_xp_plain", "active", false);
        xpRole("role_xp_one", "active", false, "org_b");
        xpRole("role_xp_two", "active", true);

        xpGrant("usr_xp_plain", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_plain");
        service.projectReach(xpActor("usr_xp_plain"), XP_ASSIGN, XP_APPLY);
        AccessActor admin = new AccessActor("usr_admin", "usr_admin", "platform_admin", "org", null, "active");
        authenticateActor(admin);
        service.projectReach(admin, XP_ASSIGN, XP_APPLY);
        AccessActor merchant = new AccessActor("usr_xp_merchant", "usr_xp_merchant", "merchant_admin", "org_a", "merchant_a", "active");
        xpGrant("usr_xp_merchant", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_two");
        authenticateActor(merchant);
        service.projectReach(merchant, XP_ASSIGN, XP_APPLY);
        assertThat(repository.holdsReads).as("普通员工、平台管理员、商户账号").isZero();

        for (String role : List.of("role_xp_one", "role_xp_two")) {
            xpGrant("usr_xp", XP_ASSIGN, AccessRepository.Source.ROLE, role);
            xpGrant("usr_xp", XP_APPLY, AccessRepository.Source.ROLE, role);
        }
        AuthorizationService.ProjectReach reach = service.projectReach(xpActor("usr_xp"), XP_ASSIGN, XP_APPLY);
        assertThat(reach.allProjects()).isTrue();
        assertThat(repository.holdsReads).as("两个可跨项目角色、两个入口权限码").isEqualTo(1);
    }

    // M736-Z7（技术推导，依据操作员 2026-10-03 所选选项原文「减少也要审批通过后才生效；审批期间持有人仍能看 C。」与 2026-10-01 确认的
    // 追加句「逐个勾选的只能查看被选中的项目」）：角色原来勾着别的项目、后来换成只勾丙（原来那条的减少已审批通过），老持有人对丙有待批、
    // 不带保留标记——这个角色的权限对他哪个项目都不到：丙不到，从没被勾过的本人项目也不到；同一权限码另有普通角色时只到本人项目；
    // 待批删掉（重新审批通过）以后只到丙。与 Z3 只差那条保留标记。一条不剩就退回「按没勾选的角色算」时红在第一条（本人项目可达）。
    @Test
    void roleWhoseTicksWereAllReplacedGivesNothingUntilReapproved() {
        repository.projectIds.addAll(List.of("org_a", "org_b", "org_c"));
        AccessActor actor = xpActor("usr_xp");
        xpRole("role_xp_swapped", "active", false, "org_c");
        xpGrant("usr_xp", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_swapped");
        xpHold("usr_xp", "role_xp_swapped", "org_c");

        AuthorizationService.ProjectReach none = service.projectReach(actor, XP_ASSIGN);
        assertThat(List.of(none.covers("org_a"), none.covers("org_b"), none.covers("org_c"))).containsExactly(false, false, false);
        assertThat(none.onlyOwnProject()).isEmpty();
        assertThat(service.canReachProject(actor, "org_a", XP_ASSIGN)).as("从没被勾过的本人项目").isFalse();

        xpRole("role_xp_plain", "active", false);
        AccessRepository.PermissionEffect plain = xpGrant("usr_xp", XP_ASSIGN, AccessRepository.Source.ROLE, "role_xp_plain");
        assertThat(service.projectReach(actor, XP_ASSIGN).onlyOwnProject()).as("另有普通角色").contains("org_a");
        repository.effects.remove(plain);

        repository.viewableHolds.remove("usr_xp");
        AuthorizationService.ProjectReach approved = service.projectReach(actor, XP_ASSIGN);
        assertThat(List.of(approved.covers("org_a"), approved.covers("org_c"))).as("重新审批通过后").containsExactly(false, true);
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

        Runnable afterIdentityRead; // M722-F06：读账号行之后的一次性钩子（模拟此刻别人把账号行改写了一遍）

        @Override
        public java.util.Optional<UserIdentity> findUserIdentity(String userId) {
            java.util.Optional<UserIdentity> result = AccessRepository.super.findUserIdentity(userId).map(identity -> new UserIdentity(
                    identity.organizationId(), identity.merchantId(), identity.accountType(), identity.status(),
                    String.valueOf(rowVersions.get(userId))));
            Runnable after = afterIdentityRead;
            afterIdentityRead = null;
            if (after != null) {
                after.run();
            }
            return result;
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

        // M736：按编号取角色（带「可以查看全部项目」与逐个勾选的项目）与当时的项目集合；都为空时与原先一样查不到角色、谁也跨不出本人项目。
        final java.util.Map<String, RoleRecord> roles = new java.util.LinkedHashMap<>();
        final Set<String> projectIds = new LinkedHashSet<>();

        @Override
        public java.util.Optional<RoleRecord> findRoleById(String roleId) {
            return java.util.Optional.ofNullable(roles.get(roleId));
        }

        @Override
        public Set<String> findProjectOrganizationIds() {
            return Set.copyOf(projectIds);
        }

        // M736 第二步：待批条目（员工 → 角色 → 条目）与读了几次；与 JdbcAccessRepository 同语义——按员工取他的全部待批，没有则空。
        // （不标 @Override：红版只换测试文件、主代码为只有第一步的基线，接口里还没有这个方法。）
        final java.util.Map<String, java.util.Map<String, Set<String>>> viewableHolds = new java.util.LinkedHashMap<>();
        int holdsReads;

        public java.util.Map<String, Set<String>> findRoleViewableProjectHolds(String userId) {
            holdsReads++;
            return viewableHolds.getOrDefault(userId, java.util.Map.of());
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
