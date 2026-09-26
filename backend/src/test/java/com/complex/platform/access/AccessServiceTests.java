package com.complex.platform.access;

// 文件版本：V0.21

import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.auth.AuthService;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.access.dto.AccessGrantApplyRequest;
import com.complex.platform.access.dto.AccessGrantReviewRequest;
import com.complex.platform.access.dto.AccessUserRequest;
import com.complex.platform.access.dto.PermissionTemplateApplyRequest;
import com.complex.platform.access.dto.PermissionTemplateRequest;
import com.complex.platform.access.dto.PositionRoleTemplateRequest;
import com.complex.platform.access.dto.RoleAssignmentRequest;
import com.complex.platform.access.dto.RoleRequest;
import com.complex.platform.access.dto.StaffLevelAssignmentRequest;
import com.complex.platform.access.dto.StatusChangeRequest;
import com.complex.platform.access.dto.TemporaryGrantRequest;
import com.complex.platform.access.dto.UserScopeChangeRequest;
import com.complex.platform.access.dto.UserPermissionOverrideRequest;
import com.complex.platform.access.dto.UserPositionAssignmentRequest;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewRepository;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AccessServiceTests {

    private InMemoryAccessRepository accessRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private RecordingCredentialProvisioner credentialProvisioner;
    private AuthorizationService authorizationService;
    private AccessService service;

    @BeforeEach
    void setUp() {
        accessRepository = new InMemoryAccessRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        credentialProvisioner = new RecordingCredentialProvisioner();
        accessRepository.addPermission("ops.production_readiness.view", "medium", false);
        accessRepository.addPermission("ops.production_readiness.check", "high", false);
        accessRepository.addPermission("governance.supervisor_review.view", "medium", false);
        accessRepository.addPermission("governance.supervisor_review.approve", "high", false);
        // M006-001/012：夹具明确逐项授予高危权限和审批角色，不依赖管理员隐式全权。
        accessRepository.roles.put("explicit_admin_grants", new RoleRecord(
                "explicit_admin_grants", "Explicit test grants", null, "global", "active", Instant.now(),
                List.of("ops.production_readiness.check", "governance.supervisor_review.view",
                        "governance.supervisor_review.approve", "finance.ar_ap.manage"), null));
        accessRepository.assignUserRole("usr_admin", "explicit_admin_grants");
        authorizationService = new AuthorizationService(accessRepository);
        service = newAccessService(false, null);
        authenticateAsPlatformAdmin("seed-org");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void copyPermissionTemplateKeepsSnapshotWhenSourceChangesAndWritesAuditEvent() {
        PermissionTemplate source = service.createPermissionTemplate(new PermissionTemplateRequest(
                "create-source",
                "seed",
                List.of(),
                "Source template",
                "temporary",
                null,
                List.of("ops.production_readiness.view"),
                "project",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-15T00:00:00Z"),
                true
        ));

        PermissionTemplate copy = service.copyPermissionTemplate(source.id(), new PermissionTemplateRequest(
                "copy-source",
                "copy",
                List.of(),
                "Copied template",
                "temporary",
                source.id(),
                List.of("ops.production_readiness.view"),
                "project",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-15T00:00:00Z"),
                true
        ));
        service.updatePermissionTemplate(source.id(), new PermissionTemplateRequest(
                "update-source",
                "update",
                List.of(),
                "Source template updated",
                "base",
                null,
                List.of("ops.production_readiness.view"),
                "project",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-15T00:00:00Z"),
                true
        ));

        assertThat(accessRepository.items.get(copy.id()))
                .extracting(PermissionTemplateItem::permissionCode)
                .containsExactly("ops.production_readiness.view");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("permission_template.copied");
    }

    @Test
    void applyPermissionTemplateRequiresApprovalWhenTemplateHasHighRiskPermission() {
        // AUD-011：模板应用必须复用统一授权政策引擎——含高风险(requiresApproval)权限的模板经模板入口批量应用时
        // fail-closed（与角色/岗位/临时授权同口径），不得绕过职责分离/主管复核。
        accessRepository.markRequiresApproval("ops.production_readiness.check", "high");
        service = newAccessService(true, null);
        accessRepository.users.put("usr_hv_apply", new AccessUser(
                "usr_hv_apply", "hv-apply-target", "staff", "seed-org", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_hv_apply", "tpl_hv_apply", "High risk apply", null, "base",
                null, "platform", null, "merge", null, null, false,
                "active", null, null, null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem(
                        "tpl_hv_apply",
                        "ops.production_readiness.check",
                        AccessRepository.Effect.GRANT,
                        "project",
                        false)));

        assertThatThrownBy(() -> service.applyPermissionTemplate("tpl_hv_apply",
                new PermissionTemplateApplyRequest(
                        "apply-hv", "apply", List.of(), "tpl_hv_apply",
                        List.of("usr_hv_apply"), "merge")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(403);
                    assertThat(e.code()).isEqualTo("GRANT_REQUIRES_APPROVAL");
                });
        // 授权未落地：无 applied 事件（说明高风险模板未被批量直发）。
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .doesNotContain("permission_template.applied");
    }

    @Test
    void applyPermissionTemplateAppliesToManyUsersAndWritesAuditEvent() {
        accessRepository.createUser(new AccessUser("usr_1", "one", "staff", "seed-org", null, "active", Instant.now()));
        accessRepository.createUser(new AccessUser("usr_2", "two", "staff", "seed-org", null, "active", Instant.now()));
        PermissionTemplate template = service.createPermissionTemplate(new PermissionTemplateRequest(
                "create-apply",
                "seed",
                List.of(),
                "Apply template",
                "temporary",
                null,
                List.of("ops.production_readiness.check"),
                "project",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-15T00:00:00Z"),
                true
        ));

        PermissionTemplateApplyResult result = service.applyPermissionTemplate(template.id(), new PermissionTemplateApplyRequest(
                "apply-many",
                "apply",
                List.of(),
                template.id(),
                List.of("usr_1", "usr_2"),
                "merge"
        ));

        assertThat(result.applications()).hasSize(2);
        assertThat(result.applications())
                .allSatisfy(application -> {
                    assertThat(application.validFrom()).isEqualTo(Instant.parse("2026-06-14T00:00:00Z"));
                    assertThat(application.validUntil()).isEqualTo(Instant.parse("2026-06-15T00:00:00Z"));
                    assertThat(application.singleUse()).isTrue();
                });
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("permission_template.applied");
    }

    @Test
    void temporaryGrantCreateAndRevokeWriteAuditEvents() {
        accessRepository.createUser(new AccessUser("usr_1", "one", "staff", "seed-org", null, "active", Instant.now()));
        TemporaryGrant grant = service.createTemporaryGrant(new TemporaryGrantRequest(
                "grant-once",
                "grant",
                List.of(),
                "usr_1",
                List.of("ops.production_readiness.check"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-15T00:00:00Z"),
                true,
                "break glass"
        ));

        TemporaryGrant revoked = service.revokeTemporaryGrant(grant.id(), new StatusChangeRequest(
                "revoke-once",
                "revoke",
                "no longer needed",
                List.of()
        ));

        assertThat(revoked.status()).isEqualTo("revoked");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("permission_template.applied", "permission_template.revoked");
    }

    // ── P1: accountType 白名单 ──────────────────────────────────────────────

    @Test
    void createAccessUserWithPlatformAdminAccountTypeThrowsValidationError() {
        assertThatThrownBy(() -> service.createAccessUser(new AccessUserRequest(
                "user-platform-admin-attempt",
                null,
                List.of(),
                "hacker",
                null,
                null,
                "platform_admin",
                "seed-org",
                null,
                "active"
        ))).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.status().value()).isEqualTo(400);
            assertThat(e.code()).isEqualTo("INVALID_ACCOUNT_TYPE");
        });
    }

    @Test
    void createAccessUserWithAllowedAccountTypeSucceeds() {
        for (String type : List.of("outsourced_vendor")) {
            AccessUser user = service.createAccessUser(new AccessUserRequest(
                    "user-type-" + type,
                    null,
                    List.of(),
                    "user_" + type,
                    "password",
                    null,
                    type,
                    "seed-org",
                    Set.of("merchant", "merchant_admin", "sales").contains(type) ? "merchant-1" : null,
                    "active"
            ));
            assertThat(user.accountType()).isEqualTo(type);
        }
    }

    @Test
    void platformAdminWithMerchantAffiliationCanCreateUserForAnyMerchant() {
        // dev/local seed 管理员带 merchantId=seed-merchant;商户越权校验应与组织校验对称地豁免平台管理员,
        // 否则平台管理员无法为任何商户开商户账号(商户专区一期活证据实测被拦)。
        AuthService.UserRecord user = new AuthService.UserRecord(
                "usr_admin_m", "seed-admin-m", "hash", "platform_admin", "seed-org", "seed-merchant", "active",
                Instant.parse("2026-06-14T00:00:00Z")
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, "token", List.of(new SimpleGrantedAuthority("ROLE_P0_SMOKE"))
        ));
        AccessUser created = service.createAccessUser(new AccessUserRequest(
                "user-cross-merchant-by-admin",
                null,
                List.of(),
                "cross_merchant_by_admin",
                "password",
                null,
                "outsourced_vendor",
                "seed-org",
                "merchant-other",
                "active"
        ));
        assertThat(created.merchantId()).isEqualTo("merchant-other");
    }

    @Test
    void createAccessUserRequiresOrganizationScope() {
        assertThatThrownBy(() -> service.createAccessUser(new AccessUserRequest(
                "user-missing-org",
                "manual entry consent evidence",
                List.of("ev_consent_manual_001"),
                "manual_missing_org",
                "change-me-123",
                "13800000000",
                "staff",
                null,
                "merchant-1",
                "active"
        ))).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.status().value()).isEqualTo(400);
            assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
        });
    }

    @Test
    void createAccessUserStoresManualEvidenceAndProvisionsCredential() {
        AccessUser user = service.createAccessUser(new AccessUserRequest(
                "user-manual-evidence",
                "后台手动录入，已取得员工授权",
                List.of("ev_consent_manual_003", "ev_hr_ticket_001"),
                "manual_evidence",
                "change-me-123",
                "13800000002",
                "outsourced_vendor",
                "seed-org",
                "merchant-1",
                "active",
                "consent_staff_manual_003"
        ));

        assertThat(user.phoneHash()).isNull();
        assertThat(user.createdVia()).isEqualTo("manual_admin");
        assertThat(user.consentReference()).isEqualTo("consent_staff_manual_003");
        assertThat(user.evidenceAttachmentIds()).containsExactly("ev_consent_manual_003", "ev_hr_ticket_001");
        assertThat(user.operatorNote()).contains("员工授权");
        assertThat(credentialProvisioner.requests)
                .anySatisfy(request -> {
                    assertThat(request.userId()).isEqualTo(user.id());
                    assertThat(request.password()).isEqualTo("change-me-123");
                    assertThat(request.status()).isEqualTo("active");
                });
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("access.user.changed");
    }

    @Test
    void merchantScopedCreatorCannotCreateUserForAnotherMerchant() {
        authenticateAsMerchantUser("usr_merchant_a", "merchant-a-admin", "org_a", "merchant_a");

        assertThatThrownBy(() -> service.createAccessUser(new AccessUserRequest(
                "user-cross-merchant",
                "manual entry consent evidence",
                List.of("ev_consent_manual_002"),
                "manual_cross_merchant",
                "change-me-123",
                "13800000001",
                "staff",
                "org_a",
                "merchant_b",
                "active"
        ))).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.status().value()).isEqualTo(403);
            assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN");
        });
    }

    // ── P0(R3 修复): 职级与薪资分配 REQ-60/61 ──────────────────────────────

    @Test
    void assignStaffLevelSetsLevelAndDefaultsCurrencyToCny() {
        accessRepository.createUser(new AccessUser("usr_emp1", "emp1", "staff", "seed-org", null, "active", Instant.now()));
        StaffLevelAssignment assignment = service.assignStaffLevel(new StaffLevelAssignmentRequest(
                "k-a-emp1", null, "usr_emp1", "lvl_m10_upper", 1_500_000L, null, "月薪 15000"));
        assertThat(assignment.staffLevelId()).isEqualTo("lvl_m10_upper");
        assertThat(assignment.baseSalaryAmountCent()).isEqualTo(1_500_000L);
        assertThat(assignment.salaryCurrency()).isEqualTo("CNY");
        assertThat(accessRepository.findStaffLevelAssignment("usr_emp1")).isPresent();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("access.staff_level.assigned");
    }

    @Test
    void assignStaffLevelRejectsUnknownLevel() {
        accessRepository.createUser(new AccessUser("usr_emp2", "emp2", "staff", "seed-org", null, "active", Instant.now()));
        assertThatThrownBy(() -> service.assignStaffLevel(new StaffLevelAssignmentRequest(
                "k-a-emp2", null, "usr_emp2", "lvl_does_not_exist", null, null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(400);
                    assertThat(e.code()).isEqualTo("UNKNOWN_STAFF_LEVEL");
                });
    }

    @Test
    void assignStaffLevelRejectsNonCnyCurrency() {
        accessRepository.createUser(new AccessUser("usr_emp3", "emp3", "staff", "seed-org", null, "active", Instant.now()));
        assertThatThrownBy(() -> service.assignStaffLevel(new StaffLevelAssignmentRequest(
                "k-a-emp3", null, "usr_emp3", "lvl_m1_lower", 100L, "USD", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(400);
                    assertThat(e.code()).isEqualTo("UNSUPPORTED_CURRENCY");
                });
    }

    @Test
    void assignStaffLevelRejectsCrossOrganization() {
        accessRepository.users.put("usr_org_b", new AccessUser(
                "usr_org_b", "org-b-user", "staff", "org_b", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        authenticateAsOrgUser("usr_org_a", "org-a-user", "org_a");
        assertThatThrownBy(() -> service.assignStaffLevel(new StaffLevelAssignmentRequest(
                "k-a-cross", null, "usr_org_b", "lvl_m1_lower", null, null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(403);
                    assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN");
                });
    }

    @Test
    void effectivePermissionsForUserRejectsCrossOrganization() {
        accessRepository.users.put("usr_org_b", new AccessUser(
                "usr_org_b", "org-b-user", "staff", "org_b", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        authenticateAsOrgUser("usr_org_a", "org-a-user", "org_a");
        assertThatThrownBy(() -> service.effectivePermissionsForUser("usr_org_b"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(403);
                    assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN");
                });
    }

    @Test
    void changeUserScopeRejectsTargetOutsideActorProject() {
        accessRepository.users.put("usr_org_a", new AccessUser(
                "usr_org_a", "org-a-user", "staff", "org_a", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        authenticateAsOrgUser("usr_org_a", "org-a-user", "org_a");
        assertThatThrownBy(() -> service.changeUserScope("usr_org_a", new UserScopeChangeRequest(
                "scope-cross-target", null, "org_b", null, "ops_clerk", "lvl_m1_lower",
                null, null, null, "move", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(403);
                    assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN");
                });
    }

    @Test
    void listStaffLevelsReturnsSeededLevels() {
        assertThat(service.listStaffLevels().items())
                .extracting(StaffLevelRecord::id)
                .contains("lvl_m1_lower", "lvl_m10_upper");
    }

    @Test
    void assignAndUnassignUserRoleUpdateUserRoles() {
        accessRepository.users.put("usr_role_target", new AccessUser(
                "usr_role_target", "role-target", "staff", "seed-org", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_ops_view", new RoleRecord(
                "role_ops_view", "ops_view", "Ops viewer", "project", "active",
                Instant.parse("2026-06-14T00:00:00Z"),
                List.of("ops.production_readiness.view"),
                null));

        RoleAssignmentRecord assigned = service.assignUserRole(new RoleAssignmentRequest(
                "assign-role", null, "usr_role_target", "role_ops_view"));
        RoleAssignmentRecord unassigned = service.unassignUserRole(new RoleAssignmentRequest(
                "unassign-role", null, "usr_role_target", "role_ops_view"));

        assertThat(assigned).isEqualTo(new RoleAssignmentRecord("usr_role_target", "role_ops_view"));
        assertThat(unassigned).isEqualTo(new RoleAssignmentRecord("usr_role_target", "role_ops_view"));
        assertThat(accessRepository.userRoles.getOrDefault("usr_role_target", Set.of()))
                .doesNotContain("role_ops_view");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("access.role.assigned", "access.role.unassigned");
    }

    // M002-F11（操作员 2026-09-25 裁决，治理地图「权限规则」）：撤销角色只减权，不要求操作人持有该角色里的权限码——
    // 不持有资金码的平台管理员、同组织员工都可撤销；跨组织仍被拒。
    @Test
    void unassignUserRoleDoesNotRequireTheActorToHoldTheRolesCodes() {
        accessRepository.addPermission("finance.dividend.manage", "high", true);
        accessRepository.roles.put("role_money", new RoleRecord(
                "role_money", "money", "Money", "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                List.of("finance.dividend.manage"), null));
        for (String userId : List.of("usr_money_a", "usr_money_b", "usr_money_c")) {
            accessRepository.users.put(userId, new AccessUser(
                    userId, userId, "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
            accessRepository.userRoles.put(userId, new LinkedHashSet<>(Set.of("role_money")));
        }

        service.unassignUserRole(new RoleAssignmentRequest("unassign-money-admin", null, "usr_money_a", "role_money"));
        authenticateAsOrgUser("usr_seed_staff", "seed-staff", "seed-org");
        service.unassignUserRole(new RoleAssignmentRequest("unassign-money-staff", null, "usr_money_b", "role_money"));
        authenticateAsOrgUser("usr_other_staff", "other-staff", "org_b");
        assertThatThrownBy(() -> service.unassignUserRole(new RoleAssignmentRequest("unassign-money-cross", null, "usr_money_c", "role_money")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN"));

        assertThat(accessRepository.userRoles.get("usr_money_a")).doesNotContain("role_money");
        assertThat(accessRepository.userRoles.get("usr_money_b")).doesNotContain("role_money");
        assertThat(accessRepository.userRoles.get("usr_money_c")).contains("role_money");
    }

    @Test
    void assignUserPositionAppliesMappedRoleAndTemplate() {
        accessRepository.users.put("usr_position_target", new AccessUser(
                "usr_position_target", "position-target", "staff", "seed-org", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_ops_check", new RoleRecord(
                "role_ops_check", "ops_check", "Ops checker", "project", "active",
                Instant.parse("2026-06-14T00:00:00Z"),
                List.of("ops.production_readiness.check"),
                null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_check", "tpl_ops_check", "Ops Check", null, "base",
                null, "platform", null, "merge", null, null, false,
                "active", null, null, null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem(
                        "tpl_ops_check",
                        "ops.production_readiness.check",
                        AccessRepository.Effect.GRANT,
                        "project",
                        false)));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "map-ops-manager", null, "ops_manager", "lvl_m1_lower",
                "role_ops_check", "tpl_ops_check", "active"));

        UserPositionAssignmentResult result = service.assignUserPosition(new UserPositionAssignmentRequest(
                "assign-position", null, "usr_position_target", "ops_manager", "lvl_m1_lower",
                null, null, null));

        assertThat(result.roleId()).isEqualTo("role_ops_check");
        assertThat(result.templateId()).isEqualTo("tpl_ops_check");
        assertThat(result.templateApplicationCount()).isEqualTo(1);
        assertThat(accessRepository.positionRoles.get("usr_position_target")).isEqualTo("role_ops_check");
        assertThat(accessRepository.userRoles.getOrDefault("usr_position_target", Set.of())).doesNotContain("role_ops_check");
        assertThat(accessRepository.findUserPositionAssignment("usr_position_target")).get()
                .extracting(UserPositionAssignment::positionCode, UserPositionAssignment::staffLevelId)
                .containsExactly("ops_manager", "lvl_m1_lower");
        assertThat(accessRepository.findStaffLevelAssignment("usr_position_target")).get()
                .extracting(StaffLevelAssignment::staffLevelId)
                .isEqualTo("lvl_m1_lower");
        assertThat(accessRepository.templateApplications)
                .extracting(PermissionTemplateApplication::templateId, PermissionTemplateApplication::targetId)
                .contains(org.assertj.core.groups.Tuple.tuple("tpl_ops_check", "usr_position_target"));
    }

    @Test
    void assignUserRoleRejectsRolePermissionsActorDoesNotHold() {
        accessRepository.users.put("usr_escalation_target", new AccessUser(
                "usr_escalation_target", "escalation-target", "staff", "org_a", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_ops_check", new RoleRecord(
                "role_ops_check", "ops_check", "Ops checker", "project", "active",
                Instant.parse("2026-06-14T00:00:00Z"),
                List.of("ops.production_readiness.check"),
                null));
        authenticateAsOrgUser("usr_org_a_actor", "org-a-actor", "org_a");

        assertThatThrownBy(() -> service.assignUserRole(new RoleAssignmentRequest(
                "assign-escalation", null, "usr_escalation_target", "role_ops_check")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(403);
                    assertThat(e.code()).isEqualTo("GRANT_EXCEEDS_OWN_PERMISSIONS");
                });
    }

    @Test
    void assignUserRoleRequiresApprovalWhenHighRiskGrantFlagIsEnabled() {
        accessRepository.markRequiresApproval("ops.production_readiness.check", "high");
        service = newAccessService(true, null);
        accessRepository.users.put("usr_high_risk_target", new AccessUser(
                "usr_high_risk_target", "high-risk-target", "staff", "seed-org", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_ops_check", new RoleRecord(
                "role_ops_check", "ops_check", "Ops checker", "project", "active",
                Instant.parse("2026-06-14T00:00:00Z"),
                List.of("ops.production_readiness.check"),
                null));

        assertThatThrownBy(() -> service.assignUserRole(new RoleAssignmentRequest(
                "assign-high-risk-direct", null, "usr_high_risk_target", "role_ops_check")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(403);
                    assertThat(e.code()).isEqualTo("GRANT_REQUIRES_APPROVAL");
                });
        assertThat(accessRepository.userRoles.getOrDefault("usr_high_risk_target", Set.of()))
                .doesNotContain("role_ops_check");
    }

    // M006-I9-02（PGM-1.10.1「同一商户、同一项目范围内的普通模板权限随保存立即生效」「属于既有高危权限的，不得因处于岗位模板内而免除授出前审批」；
    // PGM-1.10.1 角色装配：HR 定每个人的层级/职位，系统自动适配）：配置岗位映射与定岗的人不必先持有模板内的高危业务码——
    // 高危部分走挂起单审批，授权人只须持有随保存立即生效的普通码。
    @Test
    void positionMappingAndAssignmentDoNotRequireTheConfigurerToHoldTheHeldHighRiskCodes() {
        accessRepository.markRequiresApproval("ops.production_readiness.check", "high");
        accessRepository.unassignUserRole("usr_admin", "explicit_admin_grants");
        assertThat(authorizationService.effectivePermissions().grants()).doesNotContain("ops.production_readiness.check");
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        reviewRepository.superiors.put("usr_admin", "usr_boss");
        service = newAccessService(true, supervisorReviewService(reviewRepository));
        accessRepository.users.put("usr_position_no_hold_grantor", new AccessUser(
                "usr_position_no_hold_grantor", "position-no-hold-grantor", "staff", "seed-org", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_ops_check_only", new RoleRecord(
                "role_ops_check_only", "ops_check_only", "Ops checker", "project", "active",
                Instant.parse("2026-06-14T00:00:00Z"), List.of("ops.production_readiness.check"), null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_mixed", "tpl_ops_mixed", "Ops Mixed", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_ops_mixed", "ops.production_readiness.view", AccessRepository.Effect.GRANT, "project", false),
                        new PermissionTemplateItem("tpl_ops_mixed", "ops.production_readiness.check", AccessRepository.Effect.GRANT, "project", false)));

        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "map-no-hold-grantor", null, "ops_supervisor", "lvl_m1_lower", "role_ops_check_only", "tpl_ops_mixed", "active"));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "assign-no-hold-grantor", null, "usr_position_no_hold_grantor", "ops_supervisor", "lvl_m1_lower", null, null, null));

        assertThat(authorizationService.effectivePermissionsForUser("usr_position_no_hold_grantor").grants())
                .contains("ops.production_readiness.view").doesNotContain("ops.production_readiness.check");
        AccessGrantRequestRecord hold = accessRepository.grantRequests.values().stream()
                .filter(g -> "position_high_risk_release".equals(g.requestType())).findFirst().orElseThrow();
        assertThat(hold.status()).isEqualTo("pending");
        assertThat(hold.permissionCodes()).containsExactly("ops.production_readiness.check");
    }

    @Test
    void assignUserPositionLandsNormalPartAndHoldsHighRiskPartUntilChainApproves() {
        accessRepository.markRequiresApproval("ops.production_readiness.check", "high");
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        reviewRepository.superiors.put("usr_admin", "usr_boss");
        service = newAccessService(true, supervisorReviewService(reviewRepository));
        accessRepository.users.put("usr_position_high_risk", new AccessUser(
                "usr_position_high_risk", "position-high-risk", "staff", "seed-org", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_ops_check", new RoleRecord(
                "role_ops_check", "ops_check", "Ops checker", "project", "active",
                Instant.parse("2026-06-14T00:00:00Z"),
                List.of("ops.production_readiness.check"),
                null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_check", "tpl_ops_check", "Ops Check", null, "base",
                null, "platform", null, "merge", null, null, false,
                "active", null, null, null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem(
                        "tpl_ops_check",
                        "ops.production_readiness.check",
                        AccessRepository.Effect.GRANT,
                        "project",
                        false)));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "map-high-risk-position", null, "ops_manager", "lvl_m1_lower",
                "role_ops_check", "tpl_ops_check", "active"));

        // M087-F-06（PGM-1.10.1「同一商户、同一项目范围内的普通模板权限随保存立即生效」「既有高危不得因处于岗位模板内而免除审批」）
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "assign-high-risk-position", null, "usr_position_high_risk", "ops_manager", "lvl_m1_lower", null, null, null));
        assertThat(accessRepository.findUserPositionAssignment("usr_position_high_risk")).isPresent();
        assertThat(accessRepository.positionRoles.get("usr_position_high_risk")).isEqualTo("role_ops_check");
        assertThat(authorizationService.effectivePermissionsForUser("usr_position_high_risk").grants())
                .doesNotContain("ops.production_readiness.check"); // 高危部分显式拒绝，待审
        AccessGrantRequestRecord hold = accessRepository.grantRequests.values().stream()
                .filter(g -> "position_high_risk_release".equals(g.requestType())).findFirst().orElseThrow();
        assertThat(hold.status()).isEqualTo("pending");
        assertThat(hold.permissionCodes()).containsExactly("ops.production_readiness.check");
        assertThat(reviewRepository.reviews.get(hold.reviewId()).policyCode()).isEqualTo("permission_grant_l2");

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        AccessGrantRequestRecord released = service.reviewGrant(hold.id(), new AccessGrantReviewRequest(
                "review-high-risk-release", "approve", List.of(), "approve", "ok"));
        assertThat(released.status()).isEqualTo("applied");
        assertThat(authorizationService.effectivePermissionsForUser("usr_position_high_risk").grants())
                .contains("ops.production_readiness.check");
    }

    // M087-A-04/F-M087-04（PGM-1.10.1「保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，
    // 审批通过后方可生效」）：范围变更写路径。
    @Test
    void scopeChangeRevokesOldDerivedGrantsImmediatelyAndNewScopeWaitsForApproval() {
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        reviewRepository.superiors.put("usr_admin", "usr_boss");
        service = newAccessService(true, supervisorReviewService(reviewRepository));
        accessRepository.users.put("usr_mover", new AccessUser(
                "usr_mover", "mover", "staff", "seed-org", "merchant_a", "active", Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_ops_check", new RoleRecord(
                "role_ops_check", "ops_check", "Ops checker", "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                List.of("ops.production_readiness.check"), null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_check", "tpl_ops_check", "Ops Check", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_ops_check", "ops.production_readiness.check", AccessRepository.Effect.GRANT, "project", false)));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "map-mover", null, "ops_clerk", "lvl_m1_lower", "role_ops_check", "tpl_ops_check", "active"));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "assign-mover", null, "usr_mover", "ops_clerk", "lvl_m1_lower", null, null, null));
        assertThat(accessRepository.positionRoles.get("usr_mover")).isEqualTo("role_ops_check");

        AccessGrantRequestRecord pending = service.changeUserScope("usr_mover", new UserScopeChangeRequest(
                "scope-mover", null, "seed-org", "merchant_b", null, null, null, null, null, "调往 B 商户", null));
        assertThat(pending.requestType()).isEqualTo("user_scope_change");
        assertThat(pending.status()).isEqualTo("pending");
        assertThat(pending.merchantId()).isEqualTo("merchant_b");
        assertThat(accessRepository.positionRoles.get("usr_mover")).isNull(); // 旧派生立即失效
        assertThat(accessRepository.users.get("usr_mover").merchantId()).isEqualTo("merchant_b");
        assertThat(reviewRepository.reviews.get(pending.reviewId()).actionType()).isEqualTo("access.scope_change");
        assertThat(reviewRepository.reviews.get(pending.reviewId()).policyCode()).isEqualTo("permission_grant_l1"); // 普通模板也须先审批

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        AccessGrantRequestRecord applied = service.reviewGrant(pending.id(), new AccessGrantReviewRequest(
                "review-mover", "approve", List.of(), "approve", "ok"));
        assertThat(applied.status()).isEqualTo("applied");
        assertThat(accessRepository.positionRoles.get("usr_mover")).isEqualTo("role_ops_check");
        assertThat(accessRepository.findUserPositionAssignment("usr_mover").get().merchantId()).isEqualTo("merchant_b");
    }

    // M087-I2-01（PGM-1.10.1「新范围权限每次均须先审批，审批通过后方可生效；既有高危不得因处于岗位模板内而免除审批」）：
    // 待审期间新范围岗位映射被改动（换角色/模板）→ 审批时 GRANT_SNAPSHOT_CHANGED，不按改宽后的映射落地。
    @Test
    void scopeChangeApprovalRejectsWhenTheNewScopePositionMappingChangedWhilePending() {
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        reviewRepository.superiors.put("usr_admin", "usr_boss");
        service = newAccessService(true, supervisorReviewService(reviewRepository));
        accessRepository.users.put("usr_drift", new AccessUser(
                "usr_drift", "drift", "staff", "seed-org", "merchant_a", "active", Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_ops_check", new RoleRecord(
                "role_ops_check", "ops_check", "Ops checker", "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                List.of("ops.production_readiness.check"), null));
        accessRepository.roles.put("role_wider", new RoleRecord(
                "role_wider", "wider", "Wider", "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                List.of("ops.production_readiness.check", "governance.supervisor_review.approve"), null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_check", "tpl_ops_check", "Ops Check", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_ops_check", "ops.production_readiness.check", AccessRepository.Effect.GRANT, "project", false)));
        PositionRoleTemplate mapping = service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "map-drift", null, "ops_clerk", "lvl_m1_lower", "role_ops_check", "tpl_ops_check", "active"));

        AccessGrantRequestRecord pending = service.changeUserScope("usr_drift", new UserScopeChangeRequest(
                "scope-drift", null, "seed-org", "merchant_b", "ops_clerk", "lvl_m1_lower", null, null, null, "调往 B 商户", null));
        assertThat(pending.status()).isEqualTo("pending");
        // 待审期间把新范围岗位映射改到更宽的角色
        service.updatePositionRoleTemplate(mapping.id(), new PositionRoleTemplateRequest(
                "map-drift-widen", null, "ops_clerk", "lvl_m1_lower", "role_wider", "tpl_ops_check", "active"));

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThatThrownBy(() -> service.reviewGrant(pending.id(), new AccessGrantReviewRequest("review-drift", "approve", List.of(), "approve", "ok")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_SNAPSHOT_CHANGED"));
        assertThat(accessRepository.positionRoles.get("usr_drift")).isNull(); // 未落地任何映射
        assertThat(authorizationService.effectivePermissionsForUser("usr_drift").grants()).doesNotContain("governance.supervisor_review.approve");
    }

    // M087-I5-02（PGM-1.10.1「保存所属商户或发生跨项目变更时……新范围权限每次均须先审批，审批通过后方可生效」）：
    // 跨项目变更由发起人的直接上级（仍在旧项目、非平台管理员）审批通过后必须能落地；授权来自审批链与申请指纹，不按落地人当前组织闸。
    @Test
    void scopeChangeAcrossProjectsLandsWhenTheApproverStaysInTheOldProject() {
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        reviewRepository.superiors.put("usr_admin", "usr_boss");
        service = newAccessService(true, supervisorReviewService(reviewRepository));
        accessRepository.users.put("usr_cross", new AccessUser(
                "usr_cross", "cross", "staff", "seed-org", "merchant_a", "active", Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_ops_check", new RoleRecord(
                "role_ops_check", "ops_check", "Ops checker", "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                List.of("ops.production_readiness.check"), null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_check", "tpl_ops_check", "Ops Check", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_ops_check", "ops.production_readiness.check", AccessRepository.Effect.GRANT, "project", false)));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "map-cross", null, "ops_clerk", "lvl_m1_lower", "role_ops_check", "tpl_ops_check", "active"));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "assign-cross", null, "usr_cross", "ops_clerk", "lvl_m1_lower", null, null, null));

        AccessGrantRequestRecord pending = service.changeUserScope("usr_cross", new UserScopeChangeRequest(
                "scope-cross", null, "org_b", "merchant_b", null, null, null, null, null, "调往 B 项目", null));
        assertThat(pending.projectId()).isEqualTo("org_b");
        assertThat(accessRepository.users.get("usr_cross").organizationId()).isEqualTo("org_b");
        assertThat(accessRepository.positionRoles.get("usr_cross")).isNull(); // 旧派生立即失效

        authenticateAsOrgUser("usr_boss", "boss", "seed-org"); // 链上审批人仍在旧项目、非平台管理员
        AccessGrantRequestRecord applied = service.reviewGrant(pending.id(), new AccessGrantReviewRequest(
                "review-cross", "approve", List.of(), "approve", "ok"));
        assertThat(applied.status()).isEqualTo("applied");
        assertThat(accessRepository.positionRoles.get("usr_cross")).isEqualTo("role_ops_check");
        assertThat(accessRepository.findUserPositionAssignment("usr_cross").get().organizationId()).isEqualTo("org_b");
    }

    // M087-I5-03（PGM-1.10.1「旧岗位或旧范围派生权限立即失效」「显式拒绝优先级最高」）：范围变更时旧范围岗位模板的高危挂起随旧派生一并作废——
    // 显式拒绝删除、挂起单置 rejected；旧挂起单不能再在新范围解除拒绝，新范围高危只能随新范围审批重新授出。
    @Test
    void scopeChangeCancelsOldScopeHighRiskHoldsAndTheirDenyOverrides() {
        accessRepository.markRequiresApproval("ops.production_readiness.check", "high");
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        reviewRepository.superiors.put("usr_admin", "usr_boss");
        service = newAccessService(true, supervisorReviewService(reviewRepository));
        accessRepository.users.put("usr_hold_mover", new AccessUser(
                "usr_hold_mover", "hold-mover", "staff", "seed-org", "merchant_a", "active", Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_ops_check", new RoleRecord(
                "role_ops_check", "ops_check", "Ops checker", "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                List.of("ops.production_readiness.check"), null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_check", "tpl_ops_check", "Ops Check", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_ops_check", "ops.production_readiness.check", AccessRepository.Effect.GRANT, "project", false)));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "map-hold-mover", null, "ops_manager", "lvl_m1_lower", "role_ops_check", "tpl_ops_check", "active"));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "assign-hold-mover", null, "usr_hold_mover", "ops_manager", "lvl_m1_lower", null, null, null));
        AccessGrantRequestRecord hold = accessRepository.grantRequests.values().stream()
                .filter(g -> "position_high_risk_release".equals(g.requestType())).findFirst().orElseThrow();
        assertThat(hold.status()).isEqualTo("pending");
        assertThat(accessRepository.overrides).anyMatch(o -> o.userId().equals("usr_hold_mover") && o.effect() == AccessRepository.Effect.DENY);

        AccessGrantRequestRecord pending = service.changeUserScope("usr_hold_mover", new UserScopeChangeRequest(
                "scope-hold-mover", null, "seed-org", "merchant_b", null, null, null, null, null, "调往 B 商户", null));
        assertThat(pending.status()).isEqualTo("pending");
        assertThat(accessRepository.grantRequests.get(hold.id()).status()).isEqualTo("rejected"); // 旧范围挂起单作废
        assertThat(accessRepository.overrides).noneMatch(o -> o.userId().equals("usr_hold_mover") && o.effect() == AccessRepository.Effect.DENY); // 旧 DENY 清除

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThatThrownBy(() -> service.reviewGrant(hold.id(), new AccessGrantReviewRequest("review-stale-hold", "approve", List.of(), "approve", "ok")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ACCESS_GRANT_REQUEST_NOT_FOUND"));
    }

    // M087-I7-02（PGM-1.10.1「超出新模板范围的增权保持未生效，审批通过后方可生效」）：范围变更时旧范围下的非派生增权
    // （直接装配角色、单独模板授予、临时授权、员工单独授予）随旧派生一并撤销；新范围内的增权须重新申请并审批通过后才生效。
    @Test
    void scopeChangeFreezesExtraGrantsBeyondTheNewTemplate() {
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        reviewRepository.superiors.put("usr_admin", "usr_boss");
        service = newAccessService(true, supervisorReviewService(reviewRepository));
        accessRepository.users.put("usr_extra", new AccessUser(
                "usr_extra", "extra", "staff", "seed-org", "merchant_a", "active", Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_ops_check", new RoleRecord(
                "role_ops_check", "ops_check", "Ops checker", "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                List.of("ops.production_readiness.check"), null));
        accessRepository.roles.put("role_view", new RoleRecord(
                "role_view", "view", "Viewer", "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                List.of("ops.production_readiness.view"), null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_check", "tpl_ops_check", "Ops Check", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_ops_check", "ops.production_readiness.check", AccessRepository.Effect.GRANT, "project", false)));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_extra", "tpl_extra", "Extra", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_extra", "governance.supervisor_review.view", AccessRepository.Effect.GRANT, "project", false)));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "map-extra", null, "ops_clerk", "lvl_m1_lower", "role_ops_check", "tpl_ops_check", "active"));
        accessRepository.userRoles.put("usr_extra", new java.util.LinkedHashSet<>(java.util.Set.of("role_view"))); // 直接装配角色
        accessRepository.applyTemplate("tpl_extra", List.of("usr_extra"), null, null, false, "usr_admin"); // 单独模板授予
        accessRepository.temporaryGrants.put("tg_extra", new TemporaryGrant("tg_extra", "usr_extra", List.of("governance.supervisor_review.approve"),
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2099-01-01T00:00:00Z"), false, false, "active", "r", "usr_admin",
                Instant.parse("2026-06-14T00:00:00Z"), null, null)); // 临时授权
        accessRepository.saveUserPermissionOverrides("usr_extra", List.of("ops.production_readiness.check"), AccessRepository.Effect.GRANT, "员工单独授予", "usr_admin");
        assertThat(authorizationService.effectivePermissionsForUser("usr_extra").grants())
                .contains("ops.production_readiness.view", "governance.supervisor_review.view", "governance.supervisor_review.approve", "ops.production_readiness.check");

        AccessGrantRequestRecord pending = service.changeUserScope("usr_extra", new UserScopeChangeRequest(
                "scope-extra", null, "seed-org", "merchant_b", "ops_clerk", "lvl_m1_lower", null, null, null, "调往 B 商户", null));
        assertThat(pending.status()).isEqualTo("pending");
        assertThat(authorizationService.effectivePermissionsForUser("usr_extra").grants())
                .doesNotContain("ops.production_readiness.view", "governance.supervisor_review.view", "governance.supervisor_review.approve", "ops.production_readiness.check");
        assertThat(accessRepository.userRoles.get("usr_extra")).isEmpty();
        assertThat(accessRepository.temporaryGrants.get("tg_extra").status()).isEqualTo("revoked");
        assertThat(accessRepository.templateApplications).filteredOn(a -> "usr_extra".equals(a.targetId())).isNotEmpty().allMatch(a -> "revoked".equals(a.status()));
        assertThat(accessRepository.overrides).noneMatch(o -> o.userId().equals("usr_extra") && o.effect() == AccessRepository.Effect.GRANT);
    }

    // M006-R6-02（PGM-1.10.1「显式拒绝优先级最高」「既有高危不得因处于岗位模板内而免除授出前审批」）：岗位高危挂起不能经「移除覆盖」无审批解除，
    // 只能经释放单审批释放。M002-F20（技术不变量，依据 F20 裁决原句「可以对同一挂起重新发起释放申请」）：只有挂起行的码直接解除明确 400 拒绝（不再删 0 行假成功，也不再报「须审批」
    // 而无单可批），挂起行保留、不记成功事件。
    @Test
    void removingAHeldOnlyCodeIsRejectedAndOnlyTheReleaseLiftsIt() {
        accessRepository.markRequiresApproval("ops.production_readiness.check", "high");
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        reviewRepository.superiors.put("usr_admin", "usr_boss");
        service = newAccessService(true, supervisorReviewService(reviewRepository));
        accessRepository.users.put("usr_hold_remove", new AccessUser(
                "usr_hold_remove", "hold-remove", "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_ops_check", new RoleRecord(
                "role_ops_check", "ops_check", "Ops checker", "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                List.of("ops.production_readiness.check"), null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_check", "tpl_ops_check", "Ops Check", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_ops_check", "ops.production_readiness.check", AccessRepository.Effect.GRANT, "project", false)));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "map-hold-remove", null, "ops_manager", "lvl_m1_lower", "role_ops_check", "tpl_ops_check", "active"));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "assign-hold-remove", null, "usr_hold_remove", "ops_manager", "lvl_m1_lower", null, null, null));
        assertThat(authorizationService.effectivePermissionsForUser("usr_hold_remove").grants()).doesNotContain("ops.production_readiness.check");

        assertThatThrownBy(() -> service.removeUserPermissionOverrides("usr_hold_remove", new UserPermissionOverrideRequest(
                "remove-hold-deny", null, List.of("ops.production_readiness.check"), "deny", "试图解除挂起")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                    assertThat(e.getMessage()).contains("只有岗位高危挂起").contains("再次提交『岗位高危权限释放』授权申请");
                });
        assertThat(overrideSources("usr_hold_remove", "ops.production_readiness.check", AccessRepository.Effect.DENY))
                .containsExactly(AccessRepository.Source.POSITION_HOLD);
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).doesNotContain("access.user_permission_override.remove");
        assertThat(authorizationService.effectivePermissionsForUser("usr_hold_remove").grants()).doesNotContain("ops.production_readiness.check");

        AccessGrantRequestRecord hold = accessRepository.grantRequests.values().stream()
                .filter(g -> "position_high_risk_release".equals(g.requestType())).findFirst().orElseThrow();
        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThat(service.reviewGrant(hold.id(), new AccessGrantReviewRequest("review-hold-remove", "approve", List.of(), "approve", "ok")).status()).isEqualTo("applied");
        assertThat(authorizationService.effectivePermissionsForUser("usr_hold_remove").grants()).contains("ops.production_readiness.check"); // 只能经挂起单审批释放
    }

    // M006-R6-02（PGM-1.10.1「显式拒绝优先级最高」）：员工已有的独立显式拒绝不被岗位高危挂起复用——不进释放单、作废 / 释放时不被删，
    // 该码在拒绝解除（须审批）前始终不生效。
    @Test
    void preExistingExplicitDenyIsNotReusedByTheHighRiskHold() {
        accessRepository.markRequiresApproval("ops.production_readiness.check", "high");
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        reviewRepository.superiors.put("usr_admin", "usr_boss");
        service = newAccessService(true, supervisorReviewService(reviewRepository));
        accessRepository.users.put("usr_predeny", new AccessUser(
                "usr_predeny", "predeny", "staff", "seed-org", "merchant_a", "active", Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_ops_check", new RoleRecord(
                "role_ops_check", "ops_check", "Ops checker", "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                List.of("ops.production_readiness.check"), null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_check", "tpl_ops_check", "Ops Check", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_ops_check", "ops.production_readiness.check", AccessRepository.Effect.GRANT, "project", false)));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "map-predeny", null, "ops_manager", "lvl_m1_lower", "role_ops_check", "tpl_ops_check", "active"));
        service.setUserPermissionOverrides("usr_predeny", new UserPermissionOverrideRequest(
                "predeny-set", "停权", List.of("ops.production_readiness.check"), "deny", "独立显式拒绝"));

        service.assignUserPosition(new UserPositionAssignmentRequest(
                "assign-predeny", null, "usr_predeny", "ops_manager", "lvl_m1_lower", null, null, null));
        assertThat(accessRepository.grantRequests.values()).noneMatch(g -> "position_high_risk_release".equals(g.requestType())); // 已被独立拒绝的码不开挂起单
        assertThat(overrideSources("usr_predeny", "ops.production_readiness.check", AccessRepository.Effect.DENY))
                .containsExactly(AccessRepository.Source.USER_OVERRIDE); // M002-F02：预拒绝的码只有员工显式行，不写挂起行
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).contains("access.user_position.high_risk_already_denied");
        assertThat(authorizationService.effectivePermissionsForUser("usr_predeny").grants()).doesNotContain("ops.production_readiness.check");

        service.changeUserScope("usr_predeny", new UserScopeChangeRequest(
                "scope-predeny", null, "seed-org", "merchant_b", null, null, null, null, null, "调往 B 商户", null));
        assertThat(accessRepository.overrides).anyMatch(o -> o.userId().equals("usr_predeny") && o.effect() == AccessRepository.Effect.DENY
                && "ops.production_readiness.check".equals(o.permissionCode())); // 独立拒绝不随范围变更作废被删
        assertThat(authorizationService.effectivePermissionsForUser("usr_predeny").grants()).doesNotContain("ops.production_readiness.check");
    }

    // ── M002（PGM「支持员工级完全自定义配置」「显式拒绝优先级最高」「授出前须先审批才生效」；ENUM「最终权限计算」）──────────

    private static final String M002_CODE = "ops.production_readiness.check";

    // M002-F02/F20：待审释放单仍有效（同一岗位映射、授权事实未变）时，同岗再次定岗不重复写挂起行、不另开释放单。
    @Test
    void reassigningTheSamePositionDoesNotDuplicateAPendingHold() {
        m002HighRiskFixture("usr_m002_again", null, "lvl_m1_lower");
        for (String key : List.of("m002-assign-again-1", "m002-assign-again-2")) {
            service.assignUserPosition(new UserPositionAssignmentRequest(
                    key, null, "usr_m002_again", "ops_manager", "lvl_m1_lower", null, null, null));
        }
        assertThat(accessRepository.grantRequests.values())
                .filteredOn(g -> "position_high_risk_release".equals(g.requestType()) && "usr_m002_again".equals(g.targetUserId())
                        && "pending".equals(g.status()))
                .hasSize(1);
        assertThat(overrideSources("usr_m002_again", M002_CODE, AccessRepository.Effect.DENY))
                .containsExactly(AccessRepository.Source.POSITION_HOLD);
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).contains("access.user_position.high_risk_already_denied");
    }

    // M002-F02：挂起期间写下的员工显式拒绝不随挂起单获批被删——释放只删挂起行，重复批准不改变结果。
    @Test
    void explicitDenyAddedWhileHighRiskHoldIsPendingSurvivesReleaseApproval() {
        m002HighRiskFixture("usr_m002_release", null, "lvl_m1_lower");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-assign-release", null, "usr_m002_release", "ops_manager", "lvl_m1_lower", null, null, null));
        AccessGrantRequestRecord hold = m002PendingHold();
        m002ExplicitDeny("usr_m002_release", "m002-deny-release");
        assertThat(overrideSources("usr_m002_release", M002_CODE, AccessRepository.Effect.DENY))
                .containsExactlyInAnyOrder(AccessRepository.Source.USER_OVERRIDE, AccessRepository.Source.POSITION_HOLD);

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThat(service.reviewGrant(hold.id(), m002Approve("m002-review-release")).status()).isEqualTo("applied");
        assertThat(overrideSources("usr_m002_release", M002_CODE, AccessRepository.Effect.DENY))
                .containsExactly(AccessRepository.Source.USER_OVERRIDE);
        EffectivePermissions effective = authorizationService.effectivePermissionsForUser("usr_m002_release");
        assertThat(effective.grants()).doesNotContain(M002_CODE);
        assertThat(effective.denies()).contains(M002_CODE);

        assertThatThrownBy(() -> service.reviewGrant(hold.id(), m002Approve("m002-review-release-again")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ACCESS_GRANT_REQUEST_NOT_FOUND"));
        assertThat(authorizationService.effectivePermissionsForUser("usr_m002_release").grants()).doesNotContain(M002_CODE);
    }

    // M002-F02：挂起期间写下的员工显式拒绝不随范围变更作废挂起被删；新范围审批落地后该码仍不生效。
    @Test
    void explicitDenyAddedWhileHighRiskHoldIsPendingSurvivesScopeChangeCancellation() {
        m002HighRiskFixture("usr_m002_scope", "merchant_a", "lvl_m1_lower");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-assign-scope", null, "usr_m002_scope", "ops_manager", "lvl_m1_lower", null, null, null));
        AccessGrantRequestRecord hold = m002PendingHold();
        m002ExplicitDeny("usr_m002_scope", "m002-deny-scope");

        AccessGrantRequestRecord pending = service.changeUserScope("usr_m002_scope", new UserScopeChangeRequest(
                "m002-scope", null, "seed-org", "merchant_b", null, null, null, null, null, "调往 B 商户", null));
        assertThat(accessRepository.grantRequests.get(hold.id()).status()).isEqualTo("rejected");
        assertThat(overrideSources("usr_m002_scope", M002_CODE, AccessRepository.Effect.DENY))
                .containsExactly(AccessRepository.Source.USER_OVERRIDE);

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThat(service.reviewGrant(pending.id(), m002Approve("m002-review-scope")).status()).isEqualTo("applied");
        assertThat(accessRepository.positionRoles.get("usr_m002_scope")).isEqualTo("role_ops_check");
        assertThat(authorizationService.effectivePermissionsForUser("usr_m002_scope").grants()).doesNotContain(M002_CODE);
    }

    // M002-F02：已定岗员工改职级（assignStaffLevel 走定岗同路径）触发的挂起，释放时同样只删挂起行，挂起后写下的显式拒绝保留。
    @Test
    void staffLevelChangeHoldThenExplicitDenySurvivesRelease() {
        m002HighRiskFixture("usr_m002_level", null, "lvl_m10_upper");
        accessRepository.roles.put("role_ops_view", new RoleRecord(
                "role_ops_view", "ops_view", "Ops viewer", "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                List.of("ops.production_readiness.view"), null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_view", "tpl_ops_view", "Ops View", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_ops_view", "ops.production_readiness.view", AccessRepository.Effect.GRANT, "project", false)));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "m002-map-level-daily", null, "ops_manager", "lvl_m1_lower", "role_ops_view", "tpl_ops_view", "active"));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-assign-level", null, "usr_m002_level", "ops_manager", "lvl_m1_lower", null, null, null));
        assertThat(accessRepository.grantRequests.values()).noneMatch(g -> "position_high_risk_release".equals(g.requestType()));

        service.assignStaffLevel(new StaffLevelAssignmentRequest(
                "m002-level-up", null, "usr_m002_level", "lvl_m10_upper", null, null, null));
        AccessGrantRequestRecord hold = m002PendingHold();
        assertThat(hold.staffLevelId()).isEqualTo("lvl_m10_upper");
        m002ExplicitDeny("usr_m002_level", "m002-deny-level");

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThat(service.reviewGrant(hold.id(), m002Approve("m002-review-level")).status()).isEqualTo("applied");
        assertThat(overrideSources("usr_m002_level", M002_CODE, AccessRepository.Effect.DENY))
                .containsExactly(AccessRepository.Source.USER_OVERRIDE);
        assertThat(authorizationService.effectivePermissionsForUser("usr_m002_level").grants()).doesNotContain(M002_CODE);
    }

    // M002-F01：含须审批权限的员工单独授予——直接覆盖仍拒绝；走授权申请，获批前不写授予、不生效，获批后写员工单独授予并留痕（带申请 id）。
    @Test
    void highRiskUserPermissionGrantLandsOnlyAfterApproval() {
        m002HighRiskFixture("usr_m002_grant", null, "lvl_m1_lower");
        assertThatThrownBy(() -> service.setUserPermissionOverrides("usr_m002_grant", new UserPermissionOverrideRequest(
                "m002-direct-grant", null, List.of(M002_CODE), "grant", "直接单独授予")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_REQUIRES_APPROVAL"));

        AccessGrantRequestRecord pending = service.applyGrant(m002Apply(
                "m002-apply-grant", "user_permission_grant", "usr_m002_grant", List.of(M002_CODE)));
        assertThat(pending.status()).isEqualTo("pending");
        assertThat(pending.requestType()).isEqualTo("user_permission_grant");
        assertThat(pending.reviewId()).isNotNull();
        assertThat(overrideSources("usr_m002_grant", M002_CODE, AccessRepository.Effect.GRANT)).isEmpty();
        assertThat(authorizationService.effectivePermissionsForUser("usr_m002_grant").grants()).doesNotContain(M002_CODE);

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThat(service.reviewGrant(pending.id(), m002Approve("m002-review-grant")).status()).isEqualTo("applied");
        assertThat(overrideSources("usr_m002_grant", M002_CODE, AccessRepository.Effect.GRANT))
                .containsExactly(AccessRepository.Source.USER_OVERRIDE);
        assertThat(authorizationService.effectivePermissionsForUser("usr_m002_grant").grants()).contains(M002_CODE);
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "access.user_permission_override.set".equals(event.eventName()))
                .extracting(event -> event.metadata().get("grant_request_id"))
                .containsExactly(pending.id());
    }

    // M002-F01：受益人与申请人不得自审，驳回后不写授予、不增权。
    @Test
    void userPermissionGrantRejectAndSelfReviewDoNotGrant() {
        m002HighRiskFixture("usr_m002_self", null, "lvl_m1_lower");
        AccessGrantRequestRecord pending = service.applyGrant(m002Apply(
                "m002-apply-self", "user_permission_grant", "usr_m002_self", List.of(M002_CODE)));

        authenticateAsOrgUser("usr_m002_self", "m002-self", "seed-org");
        assertThatThrownBy(() -> service.reviewGrant(pending.id(), m002Approve("m002-review-beneficiary")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("BENEFICIARY_SELF_REVIEW_FORBIDDEN"));
        authenticateAsPlatformAdmin("seed-org"); // 申请人 usr_admin
        assertThatThrownBy(() -> service.reviewGrant(pending.id(), m002Approve("m002-review-requester")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThat(service.reviewGrant(pending.id(), new AccessGrantReviewRequest(
                "m002-review-reject", "reject", List.of(), "reject", "驳回")).status()).isEqualTo("rejected");
        assertThat(overrideSources("usr_m002_self", M002_CODE, AccessRepository.Effect.GRANT)).isEmpty();
        assertThat(authorizationService.effectivePermissionsForUser("usr_m002_self").grants()).doesNotContain(M002_CODE);
    }

    // M002-F01（「显式拒绝优先级最高」）：同码已有员工显式拒绝时，获批的单独授予照写但不生效。
    @Test
    void approvedUserGrantStillLosesToExplicitDeny() {
        m002HighRiskFixture("usr_m002_lose", null, "lvl_m1_lower");
        m002ExplicitDeny("usr_m002_lose", "m002-deny-lose");
        AccessGrantRequestRecord pending = service.applyGrant(m002Apply(
                "m002-apply-lose", "user_permission_grant", "usr_m002_lose", List.of(M002_CODE)));

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThat(service.reviewGrant(pending.id(), m002Approve("m002-review-lose")).status()).isEqualTo("applied");
        assertThat(overrideSources("usr_m002_lose", M002_CODE, AccessRepository.Effect.GRANT))
                .containsExactly(AccessRepository.Source.USER_OVERRIDE);
        EffectivePermissions effective = authorizationService.effectivePermissionsForUser("usr_m002_lose");
        assertThat(effective.grants()).doesNotContain(M002_CODE);
        assertThat(effective.denies()).contains(M002_CODE);
    }

    // M002-F01：待审期间权限定义变化（风险升档）即授权事实指纹变化，批准被拒、不写授予。
    @Test
    void userPermissionGrantSnapshotChangedBlocksApproval() {
        m002HighRiskFixture("usr_m002_drift", null, "lvl_m1_lower");
        AccessGrantRequestRecord pending = service.applyGrant(m002Apply(
                "m002-apply-drift", "user_permission_grant", "usr_m002_drift", List.of(M002_CODE)));
        accessRepository.markRequiresApproval(M002_CODE, "critical");

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThatThrownBy(() -> service.reviewGrant(pending.id(), m002Approve("m002-review-drift")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_SNAPSHOT_CHANGED"));
        assertThat(overrideSources("usr_m002_drift", M002_CODE, AccessRepository.Effect.GRANT)).isEmpty();
        assertThat(accessRepository.grantRequests.get(pending.id()).status()).isEqualTo("pending");
    }

    // M002-F01：两类申请只收须审批权限码（日常码走员工覆盖直接操作）；空码、未知码、跨组织目标、未开放的类型一律拒绝。
    @Test
    void userPermissionOverrideRequestValidation() {
        m002HighRiskFixture("usr_m002_validate", null, "lvl_m1_lower");
        for (List<String> codes : List.of(List.of("ops.production_readiness.view"), List.of(M002_CODE, "ops.production_readiness.view"))) {
            for (String type : List.of("user_permission_grant", "user_permission_deny_removal")) {
                assertThatThrownBy(() -> service.applyGrant(m002Apply(
                        "m002-daily-" + type + "-" + codes.size(), type, "usr_m002_validate", codes)))
                        .isInstanceOfSatisfying(ApiException.class, e -> {
                            assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                            assertThat(e.getMessage()).contains("日常权限");
                        });
            }
        }
        assertThatThrownBy(() -> service.applyGrant(m002Apply(
                "m002-empty", "user_permission_grant", "usr_m002_validate", List.of())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getMessage()).contains("必须填写权限编码列表"));
        assertThatThrownBy(() -> service.applyGrant(m002Apply(
                "m002-unknown", "user_permission_grant", "usr_m002_validate", List.of("m002.unknown.code"))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("UNKNOWN_PERMISSION"));

        accessRepository.users.put("usr_m002_other_org", new AccessUser(
                "usr_m002_other_org", "m002-other-org", "staff", "org_b", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        authenticateAsOrgUser("usr_requester", "requester", "seed-org");
        assertThatThrownBy(() -> service.applyGrant(m002Apply(
                "m002-cross-org", "user_permission_grant", "usr_m002_other_org", List.of(M002_CODE))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN"));
        assertThatThrownBy(() -> service.applyGrant(m002Apply(
                "m002-role-permission", "role_permission", "usr_m002_validate", List.of(M002_CODE))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(accessRepository.grantRequests.values()).isEmpty();
    }

    // M002-F01：解除员工显式拒绝会使须审批权限从既有授予来源生效时，直接移除仍拒绝；走解除申请，获批后才删员工显式拒绝并生效，留痕实际影响行数。
    @Test
    void denyRemovalRequestExposesHighRiskOnlyAfterApproval() {
        m002HighRiskFixture("usr_m002_unblock", null, "lvl_m1_lower");
        m002ExplicitDeny("usr_m002_unblock", "m002-deny-unblock");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-assign-unblock", null, "usr_m002_unblock", "ops_manager", "lvl_m1_lower", null, null, null));
        assertThat(accessRepository.grantRequests.values()).noneMatch(g -> "position_high_risk_release".equals(g.requestType()));
        assertThatThrownBy(() -> service.removeUserPermissionOverrides("usr_m002_unblock", new UserPermissionOverrideRequest(
                "m002-direct-remove", null, List.of(M002_CODE), "deny", "直接解除")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_REQUIRES_APPROVAL"));

        AccessGrantRequestRecord pending = service.applyGrant(m002Apply(
                "m002-apply-unblock", "user_permission_deny_removal", "usr_m002_unblock", List.of(M002_CODE)));
        assertThat(pending.status()).isEqualTo("pending");
        assertThat(authorizationService.effectivePermissionsForUser("usr_m002_unblock").grants()).doesNotContain(M002_CODE);

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThat(service.reviewGrant(pending.id(), m002Approve("m002-review-unblock")).status()).isEqualTo("applied");
        assertThat(overrideSources("usr_m002_unblock", M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
        assertThat(authorizationService.effectivePermissionsForUser("usr_m002_unblock").grants()).contains(M002_CODE);
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "access.user_permission_override.remove".equals(event.eventName())
                        && pending.id().equals(event.metadata().get("grant_request_id")))
                .extracting(event -> event.metadata().get("affected_rows"))
                .containsExactly(1);
    }

    // M002-F01/F02：解除单只删员工显式拒绝，不动岗位高危挂起行——该码仍须挂起单审批释放后才生效。
    @Test
    void denyRemovalNeverReleasesAPositionHold() {
        m002HighRiskFixture("usr_m002_both", null, "lvl_m1_lower");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-assign-both", null, "usr_m002_both", "ops_manager", "lvl_m1_lower", null, null, null));
        AccessGrantRequestRecord hold = m002PendingHold();
        m002ExplicitDeny("usr_m002_both", "m002-deny-both");
        AccessGrantRequestRecord removal = service.applyGrant(m002Apply(
                "m002-apply-both", "user_permission_deny_removal", "usr_m002_both", List.of(M002_CODE)));

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThat(service.reviewGrant(removal.id(), m002Approve("m002-review-removal")).status()).isEqualTo("applied");
        assertThat(overrideSources("usr_m002_both", M002_CODE, AccessRepository.Effect.DENY))
                .containsExactly(AccessRepository.Source.POSITION_HOLD);
        assertThat(authorizationService.effectivePermissionsForUser("usr_m002_both").grants()).doesNotContain(M002_CODE);

        assertThat(service.reviewGrant(hold.id(), m002Approve("m002-review-hold-both")).status()).isEqualTo("applied");
        assertThat(overrideSources("usr_m002_both", M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
        assertThat(authorizationService.effectivePermissionsForUser("usr_m002_both").grants()).contains(M002_CODE);
    }

    // M002-F01：解除单只能针对存在的员工显式拒绝——只被岗位挂起的码、没有任何拒绝的码都不能开解除单。
    @Test
    void denyRemovalRequiresAnExplicitDeny() {
        m002HighRiskFixture("usr_m002_held_only", null, "lvl_m1_lower");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-assign-held-only", null, "usr_m002_held_only", "ops_manager", "lvl_m1_lower", null, null, null));
        assertThat(overrideSources("usr_m002_held_only", M002_CODE, AccessRepository.Effect.DENY))
                .containsExactly(AccessRepository.Source.POSITION_HOLD);
        accessRepository.users.put("usr_m002_no_deny", new AccessUser(
                "usr_m002_no_deny", "m002-no-deny", "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));

        for (String userId : List.of("usr_m002_held_only", "usr_m002_no_deny")) {
            assertThatThrownBy(() -> service.applyGrant(m002Apply(
                    "m002-removal-" + userId, "user_permission_deny_removal", userId, List.of(M002_CODE))))
                    .isInstanceOfSatisfying(ApiException.class, e -> {
                        assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                        assertThat(e.getMessage()).contains("没有可解除的员工显式拒绝");
                    });
        }
        assertThat(accessRepository.grantRequests.values()).noneMatch(g -> "user_permission_deny_removal".equals(g.requestType()));
    }

    // M002-F04（PGM 审批链「逐级审批」「转发不限层级」）：落地不按终审人所属组织判定——其他组织的链上终审人批准后员工单独授予照常落地。
    @Test
    void userPermissionOverrideLandsWhenACrossOrganizationChainApproverFinalizes() {
        m002HighRiskFixture("usr_m002_cross", null, "lvl_m1_lower");
        AccessGrantRequestRecord pending = service.applyGrant(m002Apply(
                "m002-apply-cross", "user_permission_grant", "usr_m002_cross", List.of(M002_CODE)));

        authenticateAsOrgUser("usr_boss", "boss", "org_b");
        assertThat(service.reviewGrant(pending.id(), m002Approve("m002-review-cross")).status()).isEqualTo("applied");
        assertThat(overrideSources("usr_m002_cross", M002_CODE, AccessRepository.Effect.GRANT))
                .containsExactly(AccessRepository.Source.USER_OVERRIDE);
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "access.user_permission_override.set".equals(event.eventName()))
                .extracting(event -> event.metadata().get("grant_request_id"))
                .containsExactly(pending.id());
    }

    // M002-F04：critical 码的授权政策为最高权限（链尾 master 节点）；最高权限末节点终审后，员工单独授予与解除单都照常落地。
    @Test
    void criticalOverrideRequestsLandAfterMasterAuthorityFinalNode() {
        m002HighRiskFixture("usr_m002_top", null, "lvl_m1_lower");
        accessRepository.markRequiresApproval(M002_CODE, "critical");
        accessRepository.users.put("usr_m002_top_grant", new AccessUser(
                "usr_m002_top_grant", "usr_m002_top_grant", "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        m002ExplicitDeny("usr_m002_top", "m002-deny-top");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-assign-top", null, "usr_m002_top", "ops_manager", "lvl_m1_lower", null, null, null));
        List<AccessGrantRequestRecord> pending = List.of(
                service.applyGrant(m002Apply("m002-apply-top-grant", "user_permission_grant", "usr_m002_top_grant", List.of(M002_CODE))),
                service.applyGrant(m002Apply("m002-apply-top-removal", "user_permission_deny_removal", "usr_m002_top", List.of(M002_CODE))));

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        for (AccessGrantRequestRecord request : pending) {
            assertThat(service.reviewGrant(request.id(), m002Approve("m002-review-top-boss-" + request.id())).status()).isEqualTo("pending");
        }
        authenticateAsMasterAuthority();
        for (AccessGrantRequestRecord request : pending) {
            assertThat(service.reviewGrant(request.id(), m002Approve("m002-review-top-master-" + request.id())).status()).isEqualTo("applied");
        }
        assertThat(overrideSources("usr_m002_top_grant", M002_CODE, AccessRepository.Effect.GRANT))
                .containsExactly(AccessRepository.Source.USER_OVERRIDE);
        assertThat(overrideSources("usr_m002_top", M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
        authenticateAsPlatformAdmin("seed-org");
        assertThat(authorizationService.effectivePermissionsForUser("usr_m002_top").grants()).contains(M002_CODE);
    }

    // M002-F04：角色授予由最高权限末节点终审后照常落地。
    @Test
    void userRoleGrantLandsWhenMasterAuthorityFinalizes() {
        m002HighRiskFixture("usr_m002_role_top", null, "lvl_m1_lower");
        accessRepository.markRequiresApproval(M002_CODE, "critical");
        AccessGrantRequestRecord pending = service.applyGrant(new AccessGrantApplyRequest(
                "m002-apply-role-top", "M002", List.of(), "user_role", "usr_m002_role_top", "role_ops_check", null, null, null, null, null,
                null, null, null, false, "角色授予申请"));

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThat(service.reviewGrant(pending.id(), m002Approve("m002-review-role-top-boss")).status()).isEqualTo("pending");
        authenticateAsMasterAuthority();
        assertThat(service.reviewGrant(pending.id(), m002Approve("m002-review-role-top-master")).status()).isEqualTo("applied");
        assertThat(accessRepository.userRoles.get("usr_m002_role_top")).contains("role_ops_check");
    }

    // M002-F04：临时授权由其他组织的链上终审人批准后照常落地。
    @Test
    void temporaryPermissionLandsWhenCrossOrganizationChainApproverFinalizes() {
        m002HighRiskFixture("usr_m002_temp_cross", null, "lvl_m1_lower");
        AccessGrantRequestRecord pending = service.applyGrant(new AccessGrantApplyRequest(
                "m002-apply-temp-cross", "M002", List.of(), "temporary_permission", "usr_m002_temp_cross", null, null, null, null, null, null,
                List.of(M002_CODE), Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2099-01-01T00:00:00Z"), false, "临时授权申请"));

        authenticateAsOrgUser("usr_boss", "boss", "org_b");
        assertThat(service.reviewGrant(pending.id(), m002Approve("m002-review-temp-cross")).status()).isEqualTo("applied");
        assertThat(accessRepository.temporaryGrants.values()).extracting(TemporaryGrant::targetUserId)
                .containsExactly("usr_m002_temp_cross");
    }

    // M002-F04 守护：去掉终审人组织闸后，目标范围由授权事实指纹接管——待审期间目标换组织即 GRANT_SNAPSHOT_CHANGED，不写授予。
    @Test
    void userPermissionGrantApprovalRejectedWhenTheTargetMovesOrganizationWhilePending() {
        m002HighRiskFixture("usr_m002_moved", null, "lvl_m1_lower");
        AccessGrantRequestRecord pending = service.applyGrant(m002Apply(
                "m002-apply-moved", "user_permission_grant", "usr_m002_moved", List.of(M002_CODE)));
        accessRepository.users.put("usr_m002_moved", new AccessUser(
                "usr_m002_moved", "usr_m002_moved", "staff", "org_b", null, "active", Instant.parse("2026-06-14T00:00:00Z")));

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThatThrownBy(() -> service.reviewGrant(pending.id(), m002Approve("m002-review-moved")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_SNAPSHOT_CHANGED"));
        assertThat(overrideSources("usr_m002_moved", M002_CODE, AccessRepository.Effect.GRANT)).isEmpty();
        assertThat(accessRepository.grantRequests.get(pending.id()).status()).isEqualTo("pending");
    }

    // M002-F04 守护：直接写入口与建单仍按操作人 / 申请人范围——其他组织的操作人直接装配角色、直接临时授权、申请角色授予都被拒。
    @Test
    void directWritesAndRoleGrantRequestsStillRequireTheActorsOrganization() {
        m002HighRiskFixture("usr_m002_direct", null, "lvl_m1_lower");
        authenticateAsOrgUser("usr_requester", "requester", "org_b");
        assertThatThrownBy(() -> service.assignUserRole(new RoleAssignmentRequest(
                "m002-direct-role", null, "usr_m002_direct", "role_ops_check")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN"));
        assertThatThrownBy(() -> service.createTemporaryGrant(new TemporaryGrantRequest(
                "m002-direct-temp", null, List.of(), "usr_m002_direct", List.of(M002_CODE), null, Instant.parse("2099-01-01T00:00:00Z"), false, "r")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN"));
        assertThatThrownBy(() -> service.applyGrant(new AccessGrantApplyRequest(
                "m002-direct-apply-role", "M002", List.of(), "user_role", "usr_m002_direct", "role_ops_check", null, null, null, null, null,
                null, null, null, false, "角色授予申请")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN"));
        assertThat(accessRepository.userRoles.getOrDefault("usr_m002_direct", Set.of())).isEmpty();
        assertThat(accessRepository.temporaryGrants.values()).isEmpty();
        assertThat(accessRepository.grantRequests.values()).isEmpty();
    }

    // M002-F05（P0-1）：直接解除显式拒绝会让既有授予来源的日常码生效，操作人本人不持有该码即拒绝、拒绝行保留；持有后可正常解除。
    @Test
    void denyRemovalCannotExposeADailyCodeTheActorDoesNotHold() {
        m002ViewDenyFixture("usr_m002_view_target");
        accessRepository.users.put("usr_m002_view_operator", new AccessUser(
                "usr_m002_view_operator", "usr_m002_view_operator", "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        authenticateAsOrgUser("usr_m002_view_operator", "view-operator", "seed-org");
        assertThatThrownBy(() -> service.removeUserPermissionOverrides("usr_m002_view_target", m002RemoveDeny("m002-view-remove-1", M002_VIEW)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_EXCEEDS_OWN_PERMISSIONS"));
        assertThat(overrideSources("usr_m002_view_target", M002_VIEW, AccessRepository.Effect.DENY))
                .containsExactly(AccessRepository.Source.USER_OVERRIDE);

        accessRepository.userRoles.put("usr_m002_view_operator", new LinkedHashSet<>(Set.of("role_view")));
        service.removeUserPermissionOverrides("usr_m002_view_target", m002RemoveDeny("m002-view-remove-2", M002_VIEW));
        assertThat(overrideSources("usr_m002_view_target", M002_VIEW, AccessRepository.Effect.DENY)).isEmpty();
        authenticateAsPlatformAdmin("seed-org");
        assertThat(authorizationService.effectivePermissionsForUser("usr_m002_view_target").grants()).contains(M002_VIEW);
    }

    // M002-F05：员工不能自行解除上级写给自己的日常码显式拒绝（本人有效集已扣掉该码）。
    @Test
    void denyRemovalCannotSelfUnblockADailyCode() {
        m002ViewDenyFixture("usr_m002_self");
        authenticateAsOrgUser("usr_m002_self", "self", "seed-org");
        assertThatThrownBy(() -> service.removeUserPermissionOverrides("usr_m002_self", m002RemoveDeny("m002-self-remove", M002_VIEW)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_EXCEEDS_OWN_PERMISSIONS"));
        assertThat(overrideSources("usr_m002_self", M002_VIEW, AccessRepository.Effect.DENY))
                .containsExactly(AccessRepository.Source.USER_OVERRIDE);
    }

    // M002-F05：平台管理员目标没有授予行，日常码经通配生效——解除其拒绝同样要求操作人本人持有该码。
    @Test
    void denyRemovalOnPlatformAdminTargetRequiresTheActorToHoldTheCode() {
        accessRepository.users.put("usr_m002_pa", new AccessUser(
                "usr_m002_pa", "usr_m002_pa", "platform_admin", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.saveUserPermissionOverrides("usr_m002_pa", List.of(M002_VIEW), AccessRepository.Effect.DENY, "停权", "usr_admin");
        accessRepository.users.put("usr_m002_pa_operator", new AccessUser(
                "usr_m002_pa_operator", "usr_m002_pa_operator", "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        authenticateAsOrgUser("usr_m002_pa_operator", "pa-operator", "seed-org");
        assertThatThrownBy(() -> service.removeUserPermissionOverrides("usr_m002_pa", m002RemoveDeny("m002-pa-remove", M002_VIEW)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_EXCEEDS_OWN_PERMISSIONS"));
        assertThat(overrideSources("usr_m002_pa", M002_VIEW, AccessRepository.Effect.DENY))
                .containsExactly(AccessRepository.Source.USER_OVERRIDE);
    }

    // M002-F08（操作员 2026-09-25 裁决「本人免审批，他人须审批」）：最高权限账号上的显式拒绝——本人会话直接解除，不走审批、不要求事先持有；
    // 其他人解除须审批码一律 GRANT_REQUIRES_APPROVAL（持码的平台管理员也不例外），须走解除申请、获批后只删显式行；解除日常码须本人持有。
    // 普通目标同组合仍须审批。M002-F19（操作员 2026-09-25 裁决「最高权限账号不能被定岗」）：最高权限账号没有岗位，也就没有岗位高危挂起行，
    // 本人解除后该码不再有任何拒绝（原先固化的「本人解除后仍剩挂起行」按裁决改写）。
    @Test
    void masterAuthorityTargetDenyRemovalIsDirectOnlyForTheMasterSession() {
        m002HighRiskFixture("usr_m002_plain", null, "lvl_m1_lower");
        String master = SessionIssuer.MASTER_USER_ID;
        accessRepository.users.put(master, new AccessUser(master, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        for (String userId : List.of(master, "usr_m002_plain")) {
            accessRepository.userRoles.put(userId, new LinkedHashSet<>(Set.of("role_ops_check"))); // 同码高危授予来源
            accessRepository.saveUserPermissionOverrides(userId, List.of(M002_CODE), AccessRepository.Effect.DENY, "停权", "usr_admin");
        }
        accessRepository.saveUserPermissionOverrides(master, List.of(M002_VIEW), AccessRepository.Effect.DENY, "停权", "usr_admin");
        accessRepository.users.put("usr_m002_pc_staff", new AccessUser(
                "usr_m002_pc_staff", "usr_m002_pc_staff", "staff", "platform-company", null, "active", Instant.parse("2026-06-14T00:00:00Z")));

        assertThatThrownBy(() -> service.removeUserPermissionOverrides(master, m002RemoveDeny("m002-master-admin-direct", M002_CODE)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_REQUIRES_APPROVAL"));
        authenticateAsOrgUser("usr_m002_pc_staff", "pc-staff", "platform-company");
        assertThatThrownBy(() -> service.removeUserPermissionOverrides(master, m002RemoveDeny("m002-master-staff-view", M002_VIEW)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_EXCEEDS_OWN_PERMISSIONS"));
        assertThat(overrideSources(master, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.USER_OVERRIDE);
        assertThat(overrideSources(master, M002_VIEW, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.USER_OVERRIDE);

        authenticateAsMasterAuthority();
        service.removeUserPermissionOverrides(master, m002RemoveDeny("m002-master-self", M002_CODE));
        assertThat(overrideSources(master, M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
        authorizationService.requirePermission(M002_CODE); // 本人解除后不再被拒

        accessRepository.saveUserPermissionOverrides(master, List.of(M002_CODE), AccessRepository.Effect.DENY, "再次停权", "usr_admin");
        authenticateAsPlatformAdmin("seed-org");
        AccessGrantRequestRecord pending = service.applyGrant(m002Apply(
                "m002-master-apply", "user_permission_deny_removal", master, List.of(M002_CODE)));
        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThat(service.reviewGrant(pending.id(), m002Approve("m002-master-review")).status()).isEqualTo("applied");
        assertThat(overrideSources(master, M002_CODE, AccessRepository.Effect.DENY)).isEmpty();

        authenticateAsPlatformAdmin("seed-org");
        assertThatThrownBy(() -> service.removeUserPermissionOverrides("usr_m002_plain", m002RemoveDeny("m002-plain-remove", M002_CODE)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_REQUIRES_APPROVAL"));
        assertThat(overrideSources("usr_m002_plain", M002_CODE, AccessRepository.Effect.DENY))
                .containsExactly(AccessRepository.Source.USER_OVERRIDE);
    }

    // M002-F15：最高权限账号暂被停用时仍按账号身份认作最高权限目标——他人直接解除其须审批码的拒绝仍须审批，拒绝行保留。
    @Test
    void disabledMasterAuthorityAccountIsStillAMasterTargetForDenyRemoval() {
        m002HighRiskFixture("usr_m002_unused_f15", null, "lvl_m1_lower");
        String master = SessionIssuer.MASTER_USER_ID;
        accessRepository.users.put(master, new AccessUser(master, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "disabled", Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.saveUserPermissionOverrides(master, List.of(M002_CODE), AccessRepository.Effect.DENY, "停权", "usr_admin");

        assertThatThrownBy(() -> service.removeUserPermissionOverrides(master, m002RemoveDeny("m002-disabled-master", M002_CODE)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_REQUIRES_APPROVAL"));
        assertThat(overrideSources(master, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.USER_OVERRIDE);
    }

    // M002-F16：权限码列表含空值或空白元素时，建角色与员工覆盖在查库前以 400 VALIDATION_FAILED 拒绝，不留角色、覆盖行与成功事件。
    @Test
    void blankOrNullPermissionCodesAreRejectedBeforeAnyWrite() {
        accessRepository.users.put("usr_m002_blank", new AccessUser(
                "usr_m002_blank", "usr_m002_blank", "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        int roles = accessRepository.roles.size();
        assertThatThrownBy(() -> service.createRole(new RoleRequest(
                "m002-blank-role", null, List.of(), "空码角色", null, java.util.Arrays.asList(M002_VIEW, null), "project")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> service.setUserPermissionOverrides("usr_m002_blank", new UserPermissionOverrideRequest(
                "m002-blank-override", null, List.of("  "), "deny", "空白码")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(accessRepository.roles).hasSize(roles);
        assertThat(accessRepository.overrides).noneMatch(o -> "usr_m002_blank".equals(o.userId()));
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName)
                .doesNotContain("role.changed", "access.user_permission_override.set");
    }

    // M002-F05：直接解除只删员工显式拒绝，岗位高危挂起行保留（经释放单审批解除，或随换岗、模板停用/修改、范围变更作废，M002-F20）。
    @Test
    void directDenyRemovalDeletesOnlyTheExplicitDeny() {
        m002HighRiskFixture("usr_m002_direct_both", null, "lvl_m1_lower");
        accessRepository.saveHighRiskHolds("usr_m002_direct_both", List.of(M002_CODE), "岗位高危挂起", "usr_admin");
        accessRepository.saveUserPermissionOverrides("usr_m002_direct_both", List.of(M002_CODE), AccessRepository.Effect.DENY, "停权", "usr_admin");
        service.removeUserPermissionOverrides("usr_m002_direct_both", m002RemoveDeny("m002-direct-both-remove", M002_CODE));
        assertThat(overrideSources("usr_m002_direct_both", M002_CODE, AccessRepository.Effect.DENY))
                .containsExactly(AccessRepository.Source.POSITION_HOLD);
    }

    // M002-F06（PGM「显式拒绝优先级最高」）：解除单钉住所解除的拒绝行——待审期间该拒绝被重写，批准即 GRANT_SNAPSHOT_CHANGED，新拒绝保留、码不生效。
    @Test
    void denyRemovalApprovalRejectedWhenTheExplicitDenyIsRewrittenWhilePending() {
        m002HighRiskFixture("usr_m002_rewrite", null, "lvl_m1_lower");
        m002ExplicitDeny("usr_m002_rewrite", "m002-deny-rewrite-1");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-assign-rewrite", null, "usr_m002_rewrite", "ops_manager", "lvl_m1_lower", null, null, null));
        AccessGrantRequestRecord pending = service.applyGrant(m002Apply(
                "m002-apply-rewrite", "user_permission_deny_removal", "usr_m002_rewrite", List.of(M002_CODE)));
        m002ExplicitDeny("usr_m002_rewrite", "m002-deny-rewrite-2");

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThatThrownBy(() -> service.reviewGrant(pending.id(), m002Approve("m002-review-rewrite")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_SNAPSHOT_CHANGED"));
        assertThat(overrideSources("usr_m002_rewrite", M002_CODE, AccessRepository.Effect.DENY))
                .containsExactly(AccessRepository.Source.USER_OVERRIDE);
        assertThat(accessRepository.grantRequests.get(pending.id()).status()).isEqualTo("pending");
        authenticateAsPlatformAdmin("seed-org");
        assertThat(authorizationService.effectivePermissionsForUser("usr_m002_rewrite").grants()).doesNotContain(M002_CODE);
    }

    // M002-F06：审批时的比对在锁员工行之前——落地在员工行锁下复核所钉的拒绝行，锁前刚被重写即 GRANT_SNAPSHOT_CHANGED，新拒绝保留。
    @Test
    void denyRemovalLandingRechecksTheExplicitDenyUnderTheTargetLock() {
        m002HighRiskFixture("usr_m002_race", null, "lvl_m1_lower");
        m002ExplicitDeny("usr_m002_race", "m002-deny-race");
        AccessGrantRequestRecord pending = service.applyGrant(m002Apply(
                "m002-apply-race", "user_permission_deny_removal", "usr_m002_race", List.of(M002_CODE)));
        accessRepository.beforeNextUserLock = () -> accessRepository.saveUserPermissionOverrides(
                "usr_m002_race", List.of(M002_CODE), AccessRepository.Effect.DENY, "并发再次停权", "usr_other_admin");

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThatThrownBy(() -> service.reviewGrant(pending.id(), m002Approve("m002-review-race")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_SNAPSHOT_CHANGED"));
        assertThat(accessRepository.findExplicitDenies("usr_m002_race", List.of(M002_CODE)))
                .extracting(AccessRepository.ExplicitDenyRow::reason).containsExactly("并发再次停权");
    }

    // M002-F03（R-001「有效集并入全部权限字典，显式拒绝仍优先」）：最高权限没有预授角色也能建含高危码的角色（不再报超出本人权限）。
    @Test
    void masterAuthorityCreatesARoleWithoutPreAssignedRoles() {
        accessRepository.markRequiresApproval(M002_CODE, "critical");
        authenticateAsMasterAuthority();
        RoleRecord role = service.createRole(new RoleRequest(
                "m002-master-role", "M002", List.of(), "最高权限建角色", null, List.of(M002_CODE, M002_VIEW), "project"));
        assertThat(accessRepository.roles.get(role.id()).permissionCodes()).containsExactly(M002_CODE, M002_VIEW);
    }

    // M002-F03：最高权限的有效集（本人会话与平台管理员查看）= 全部 active 字典码 − 显式拒绝；wildcard 标志保持 false。
    @Test
    void masterAuthorityEffectiveSetIsTheWholeDictionaryMinusExplicitDenies() {
        accessRepository.markRequiresApproval(M002_CODE, "critical");
        accessRepository.users.put(SessionIssuer.MASTER_USER_ID, new AccessUser(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.saveUserPermissionOverrides(SessionIssuer.MASTER_USER_ID, List.of("governance.supervisor_review.view"),
                AccessRepository.Effect.DENY, "停权", "usr_admin");

        EffectivePermissions viewed = authorizationService.effectivePermissionsForUser(SessionIssuer.MASTER_USER_ID);
        assertThat(viewed.grants()).containsExactlyInAnyOrderElementsOf(accessRepository.permissions.keySet().stream()
                .filter(code -> !"governance.supervisor_review.view".equals(code)).toList());
        assertThat(viewed.denies()).containsExactly("governance.supervisor_review.view");
        assertThat(viewed.wildcard()).isFalse();
        authenticateAsMasterAuthority();
        assertThat(authorizationService.effectivePermissions().grants()).containsExactlyElementsOf(viewed.grants());
    }

    // ── M002 第 7 轮（M002-F19～F23；F19、F20 为操作员 2026-09-25 裁决，F21～F23 为技术不变量；治理地图「岗位模板里的高危权限，授出审批被驳回后仍不生效，可以再次发起申请……」
    //    「最高权限账号不设岗位、不套用岗位模板……」）────────────────────────────────────────────────────────────────

    // M002-F19：定岗、范围变更、岗位申请建单以最高权限账号为目标一律 400，不写岗位、挂起行与任何申请单；单独调职级不带岗位派生，
    // 按第 703 行「定岗、换岗」最窄读法不拦（[6]），有岗位时改职级（换到另一张岗位映射）才拒；最高权限会话调须审批的 critical 码照常放行。
    @Test
    void masterAuthorityAccountCannotBeGivenAPositionAndKeepsItsApprovalCodes() {
        m002HighRiskFixture("usr_m002_f19_peer", null, "lvl_m1_lower");
        accessRepository.markRequiresApproval(M002_CODE, "critical");
        String master = SessionIssuer.MASTER_USER_ID;
        accessRepository.users.put(master, new AccessUser(master, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        List<org.assertj.core.api.ThrowableAssert.ThrowingCallable> entries = List.of(
                () -> service.assignUserPosition(new UserPositionAssignmentRequest(
                        "m002-f19-assign", null, master, "ops_manager", "lvl_m1_lower", null, null, null)),
                () -> service.changeUserScope(master, new UserScopeChangeRequest(
                        "m002-f19-scope", null, "seed-org", null, "ops_manager", "lvl_m1_lower", null, null, null, "调入项目", null)),
                () -> service.applyGrant(new AccessGrantApplyRequest(
                        "m002-f19-apply", "M002", List.of(), "user_position", master, null, "ops_manager", "lvl_m1_lower",
                        null, null, null, null, null, null, false, "岗位申请")));
        for (org.assertj.core.api.ThrowableAssert.ThrowingCallable entry : entries) {
            assertThatThrownBy(entry).isInstanceOfSatisfying(ApiException.class, e -> {
                assertThat(e.status().value()).isEqualTo(400);
                assertThat(e.code()).isEqualTo("MASTER_AUTHORITY_NO_POSITION");
            });
        }
        assertThat(accessRepository.userPositionAssignments).doesNotContainKey(master);
        assertThat(accessRepository.staffLevelAssignments).doesNotContainKey(master);
        assertThat(accessRepository.overrides).noneMatch(o -> master.equals(o.userId()));
        assertThat(accessRepository.grantRequests.values()).noneMatch(g -> master.equals(g.targetUserId()));
        assertThat(accessRepository.users.get(master).organizationId()).isEqualTo("platform-company");

        assertThat(service.assignStaffLevel(new StaffLevelAssignmentRequest(
                "m002-f19-level", null, master, "lvl_m10_upper", null, null, null)).staffLevelId()).isEqualTo("lvl_m10_upper");
        assertThat(accessRepository.userPositionAssignments).doesNotContainKey(master);
        accessRepository.saveUserPositionAssignment(new UserPositionAssignment(master, "ops_manager", "lvl_m1_lower", "platform-company",
                null, "seed", Instant.parse("2026-06-14T00:00:00Z"))); // 存量岗位行（V435 前）
        assertThatThrownBy(() -> service.assignStaffLevel(new StaffLevelAssignmentRequest(
                "m002-f19-level-positioned", null, master, "lvl_m10_upper", null, null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MASTER_AUTHORITY_NO_POSITION"));

        authenticateAsMasterAuthority();
        authorizationService.requirePermission(M002_CODE);
    }

    // M002-F20（操作员所选选项原文「可以对同一挂起重新发起释放申请」；驳回→再申请→批准，第二次还能过）：释放单驳回后挂起行保留、码不生效，
    // 同岗重定不替员工自动重开单；对同一挂起提交「岗位高危权限释放」授权申请即再次发起（快照钉当前映射），连续驳回两次仍可再发起，第三次批准后生效；
    // 批准后同岗重定，该码保持生效、不重开单。
    @Test
    void rejectedReleaseIsReappliedForTheSameHoldAndThenLands() {
        String user = "usr_m002_reapply";
        m002HighRiskFixture(user, null, "lvl_m1_lower");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-reapply-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        for (int round = 1; round <= 3; round++) {
            List<AccessGrantRequestRecord> pending = m002PendingReleases(user);
            assertThat(pending).singleElement().satisfies(release -> {
                assertThat(release.positionCode()).isEqualTo("ops_manager");
                assertThat(release.staffLevelId()).isEqualTo("lvl_m1_lower");
                assertThat(release.targetRoleId()).isEqualTo("role_ops_check");
                assertThat(release.templateId()).isEqualTo("tpl_ops_check");
                assertThat(release.permissionCodes()).containsExactly(M002_CODE);
            });
            assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
            authenticateAsOrgUser("usr_boss", "boss", "seed-org");
            if (round == 3) {
                assertThat(service.reviewGrant(pending.get(0).id(), m002Approve("m002-reapply-approve")).status()).isEqualTo("applied");
                break;
            }
            assertThat(service.reviewGrant(pending.get(0).id(), m002Reject("m002-reapply-reject-" + round)).status()).isEqualTo("rejected");
            assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
            assertThat(authorizationService.effectivePermissionsForUser(user).grants()).doesNotContain(M002_CODE);

            authenticateAsPlatformAdmin("seed-org");
            service.assignUserPosition(new UserPositionAssignmentRequest(
                    "m002-reapply-reassign-" + round, null, user, "ops_manager", "lvl_m1_lower", null, null, null));
            assertThat(m002PendingReleases(user)).isEmpty(); // 驳回仍有效：同岗重定不自动重开单
            assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
            AccessGrantRequestRecord reapplied = service.applyGrant(m002Apply(
                    "m002-reapply-" + round, "position_high_risk_release", user, List.of(M002_CODE)));
            assertThat(reapplied.status()).isEqualTo("pending");
            assertThat(reapplied.reviewId()).isNotNull();
        }
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).contains(M002_CODE);
        assertThat(m002Releases(user)).extracting(AccessGrantRequestRecord::status).containsExactly("rejected", "rejected", "applied");

        authenticateAsPlatformAdmin("seed-org");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-reapply-after-approval", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        assertThat(m002Releases(user)).hasSize(3);
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).contains(M002_CODE);
    }

    // M002-F20：再次发起只收该员工现有挂起行里、没有待审释放单覆盖、仍在当前岗位模板高危集合内的码；其余 400，不建单。
    @Test
    void reapplyingARejectedReleaseOnlyAcceptsCurrentUncoveredHolds() {
        String user = "usr_m002_reapply_guard";
        m002HighRiskFixture(user, null, "lvl_m1_lower");
        accessRepository.users.put("usr_m002_reapply_none", new AccessUser(
                "usr_m002_reapply_none", "usr_m002_reapply_none", "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-reapply-guard-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        int releases = m002Releases(user).size();
        for (String target : List.of(user, "usr_m002_reapply_none")) { // 前者已有待审单覆盖，后者没有挂起
            assertThatThrownBy(() -> service.applyGrant(m002Apply("m002-reapply-guard-" + target, "position_high_risk_release", target, List.of(M002_CODE))))
                    .isInstanceOfSatisfying(ApiException.class, e -> {
                        assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                        assertThat(e.getMessage()).contains("没有可再次申请的岗位高危挂起");
                    });
        }
        assertThat(m002Releases(user)).hasSize(releases);
        assertThat(accessRepository.grantRequests.values()).noneMatch(g -> "usr_m002_reapply_none".equals(g.targetUserId()));
    }

    // M002-F20：驳回后跨商户范围变更——已驳回单留下的挂起行随旧派生作废（员工显式拒绝保留）；新范围审批落地后该码生效。
    @Test
    void scopeChangeVoidsTheHoldLeftByARejectedReleaseAndTheNewScopeLands() {
        String user = "usr_m002_rej_scope";
        m002HighRiskFixture(user, "merchant_a", "lvl_m1_lower");
        service.setUserPermissionOverrides(user, new UserPermissionOverrideRequest(
                "m002-rej-scope-deny", "停权", List.of(M002_VIEW), "deny", "员工显式拒绝"));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-rej-scope-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        AccessGrantRequestRecord hold = m002PendingHold();
        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        service.reviewGrant(hold.id(), m002Reject("m002-rej-scope-reject"));

        authenticateAsPlatformAdmin("seed-org");
        AccessGrantRequestRecord scope = service.changeUserScope(user, new UserScopeChangeRequest(
                "m002-rej-scope", null, "seed-org", "merchant_b", null, null, null, null, null, "调往 B 商户", null));
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
        assertThat(overrideSources(user, M002_VIEW, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.USER_OVERRIDE);

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThat(service.reviewGrant(scope.id(), m002Approve("m002-rej-scope-approve")).status()).isEqualTo("applied");
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).contains(M002_CODE).doesNotContain(M002_VIEW);
    }

    // M002-F20：换岗（经改职级）——旧岗位的待审释放单作废、批准返回 404；按新岗位重新挂起开单，批准后生效。
    // 驳回后再换岗同样：已驳回单留下的挂起按新岗位重挂、另开新单。
    @Test
    void changingPositionVoidsTheOldReleaseAndHoldsTheCodeUnderTheNewPosition() {
        String user = "usr_m002_move";
        String rejectedUser = "usr_m002_move_rejected";
        m002HighRiskFixture(user, null, "lvl_m1_lower");
        accessRepository.users.put(rejectedUser, new AccessUser(
                rejectedUser, rejectedUser, "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_ops_check_senior", new RoleRecord(
                "role_ops_check_senior", "ops_check_senior", "Senior checker", "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                List.of(M002_CODE, M002_VIEW), null));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "m002-map-move-senior", null, "ops_manager", "lvl_m10_upper", "role_ops_check_senior", "tpl_ops_check", "active"));
        for (String target : List.of(user, rejectedUser)) {
            service.assignUserPosition(new UserPositionAssignmentRequest(
                    "m002-move-assign-" + target, null, target, "ops_manager", "lvl_m1_lower", null, null, null));
        }
        AccessGrantRequestRecord first = m002PendingReleases(user).get(0);
        AccessGrantRequestRecord rejected = m002PendingReleases(rejectedUser).get(0);
        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        service.reviewGrant(rejected.id(), m002Reject("m002-move-reject"));

        authenticateAsPlatformAdmin("seed-org");
        for (String target : List.of(user, rejectedUser)) {
            service.assignStaffLevel(new StaffLevelAssignmentRequest("m002-move-level-" + target, null, target, "lvl_m10_upper", null, null, null));
            assertThat(m002PendingReleases(target)).singleElement().satisfies(release -> {
                assertThat(release.staffLevelId()).isEqualTo("lvl_m10_upper");
                assertThat(release.targetRoleId()).isEqualTo("role_ops_check_senior");
                assertThat(release.permissionCodes()).containsExactly(M002_CODE);
            });
            assertThat(overrideSources(target, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
            assertThat(authorizationService.effectivePermissionsForUser(target).grants()).contains(M002_VIEW).doesNotContain(M002_CODE);
        }
        assertThat(accessRepository.grantRequests.get(first.id()).status()).isEqualTo("rejected");
        assertThat(m002Reviews.reviews.get(first.reviewId()).reviewStatus()).isEqualTo("rejected"); // M002-F20：作废同时关闭复核单

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThatThrownBy(() -> service.reviewGrant(first.id(), m002Approve("m002-move-stale")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ACCESS_GRANT_REQUEST_NOT_FOUND"));
        for (String target : List.of(user, rejectedUser)) {
            assertThat(service.reviewGrant(m002PendingReleases(target).get(0).id(), m002Approve("m002-move-approve-" + target)).status())
                    .isEqualTo("applied");
            assertThat(authorizationService.effectivePermissionsForUser(target).grants()).contains(M002_CODE, M002_VIEW);
        }
    }

    // M002-F20：驳回后换到不含该码的岗位——挂起行作废、无待审单；再批准含该码的角色申请，该码生效。
    @Test
    void movingToAPositionWithoutTheCodeVoidsTheHoldAndAnApprovedRoleThenLands() {
        String user = "usr_m002_drop";
        m002HighRiskFixture(user, null, "lvl_m1_lower");
        m002ViewMapping("ops_viewer");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-drop-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        service.reviewGrant(m002PendingHold().id(), m002Reject("m002-drop-reject"));

        authenticateAsPlatformAdmin("seed-org");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-drop-move", null, user, "ops_viewer", "lvl_m1_lower", null, null, null));
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
        assertThat(m002PendingReleases(user)).isEmpty();
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).contains(M002_VIEW).doesNotContain(M002_CODE);

        AccessGrantRequestRecord role = service.applyGrant(new AccessGrantApplyRequest(
                "m002-drop-apply-role", "M002", List.of(), "user_role", user, "role_ops_check", null, null, null, null, null,
                null, null, null, false, "角色授予申请"));
        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThat(service.reviewGrant(role.id(), m002Approve("m002-drop-approve-role")).status()).isEqualTo("applied");
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).contains(M002_CODE, M002_VIEW);
    }

    // M002-F20 d（F02/F05/F06 不退化）：同码员工显式拒绝在驳回后同岗重定、模板停用与再启用、换岗、范围变更各路径都保留；
    // 显式拒绝的码不挂起、不开单（M006-R6-02），该码始终不生效。
    @Test
    void explicitDenyOnTheHeldCodeSurvivesEveryVoidingPath() {
        String user = "usr_m002_keep";
        PositionRoleTemplate mapping = m002HighRiskFixture(user, "merchant_a", "lvl_m1_lower");
        m002ViewMapping("ops_viewer");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-keep-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        m002ExplicitDeny(user, "m002-keep-deny");
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY))
                .containsExactlyInAnyOrder(AccessRepository.Source.USER_OVERRIDE, AccessRepository.Source.POSITION_HOLD);
        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        service.reviewGrant(m002PendingHold().id(), m002Reject("m002-keep-reject"));

        authenticateAsPlatformAdmin("seed-org");
        List<Runnable> paths = List.of(
                () -> service.assignUserPosition(new UserPositionAssignmentRequest(
                        "m002-keep-reassign", null, user, "ops_manager", "lvl_m1_lower", null, null, null)),
                () -> service.updatePositionRoleTemplate(mapping.id(), m002Mapping("m002-keep-off", "role_ops_check", "tpl_ops_check", "inactive")),
                () -> service.updatePositionRoleTemplate(mapping.id(), m002Mapping("m002-keep-on", "role_ops_check", "tpl_ops_check", "active")),
                () -> service.assignUserPosition(new UserPositionAssignmentRequest(
                        "m002-keep-move", null, user, "ops_viewer", "lvl_m1_lower", null, null, null)),
                () -> service.changeUserScope(user, new UserScopeChangeRequest(
                        "m002-keep-scope", null, "seed-org", "merchant_b", null, null, null, null, null, "调往 B 商户", null)));
        for (Runnable path : paths) {
            path.run();
            assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.USER_OVERRIDE);
            assertThat(m002PendingReleases(user)).isEmpty();
            assertThat(authorizationService.effectivePermissionsForUser(user).grants()).doesNotContain(M002_CODE);
        }
    }

    // M002-F20：定岗审批落地——整单已审批，先前直接定岗留下的挂起行与待审释放单一并作废，该码生效。
    @Test
    void approvedPositionRequestVoidsTheEarlierHoldAndLands() {
        String user = "usr_m002_landing";
        m002HighRiskFixture(user, null, "lvl_m1_lower");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-landing-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        AccessGrantRequestRecord hold = m002PendingHold();
        AccessGrantRequestRecord request = service.applyGrant(new AccessGrantApplyRequest(
                "m002-landing-apply", "M002", List.of(), "user_position", user, null, "ops_manager", "lvl_m1_lower",
                null, null, null, null, null, null, false, "岗位申请"));

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThat(service.reviewGrant(request.id(), m002Approve("m002-landing-approve")).status()).isEqualTo("applied");
        assertThat(accessRepository.grantRequests.get(hold.id()).status()).isEqualTo("rejected");
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).contains(M002_CODE);
    }

    // M002-F22 ①②（停用→启用→再停用→再启用，第二次还能过）：停用收回派生、作废挂起行与待审释放单；再启用恰开一张待审单；最后批准即生效。
    @Test
    void deactivatingThePositionTemplateVoidsHoldsAndReactivatingReopensExactlyOneRelease() {
        String user = "usr_m002_toggle";
        PositionRoleTemplate mapping = m002HighRiskFixture(user, null, "lvl_m1_lower");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-toggle-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        for (int round = 1; round <= 2; round++) {
            AccessGrantRequestRecord pending = m002PendingReleases(user).get(0);
            service.updatePositionRoleTemplate(mapping.id(), m002Mapping("m002-toggle-off-" + round, "role_ops_check", "tpl_ops_check", "inactive"));
            assertThat(accessRepository.grantRequests.get(pending.id()).status()).isEqualTo("rejected");
            assertThat(m002Reviews.reviews.get(pending.reviewId()).reviewStatus()).isEqualTo("rejected"); // M002-F20：作废同时关闭复核单
            assertThat(m002Reviews.nodes.get(pending.reviewId())).noneMatch(com.complex.platform.governance.chain.ApprovalChainNode::pending);
            assertThat(m002PendingReleases(user)).isEmpty();
            assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
            assertThat(accessRepository.positionRoles.get(user)).isNull();
            EffectivePermissions off = authorizationService.effectivePermissionsForUser(user);
            assertThat(off.grants()).doesNotContain(M002_CODE);
            assertThat(off.denies()).doesNotContain(M002_CODE);

            service.updatePositionRoleTemplate(mapping.id(), m002Mapping("m002-toggle-on-" + round, "role_ops_check", "tpl_ops_check", "active"));
            assertThat(m002PendingReleases(user)).hasSize(1);
            assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
            assertThat(authorizationService.effectivePermissionsForUser(user).grants()).doesNotContain(M002_CODE);
        }
        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThatThrownBy(() -> service.reviewGrant(m002Releases(user).get(0).id(), m002Approve("m002-toggle-stale")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ACCESS_GRANT_REQUEST_NOT_FOUND"));
        assertThat(service.reviewGrant(m002PendingReleases(user).get(0).id(), m002Approve("m002-toggle-approve")).status()).isEqualTo("applied");
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).contains(M002_CODE);
        assertThat(m002Releases(user)).extracting(AccessGrantRequestRecord::status).containsExactly("rejected", "rejected", "applied");
    }

    // M002-F22 回归修正（操作员原文只有「该岗位模板被停用或修改时」，存档、取消存档按基线处理）：存档、取消存档不触发在岗重算——
    // 在岗员工的岗位派生、模板套用、挂起行、待审释放单与复核单原样保留（不收回派生、不重复套用单次模板）。
    @Test
    void archivingAndRestoringThePositionTemplateLeaveAssignedEmployeesUntouched() {
        String user = "usr_m002_archive";
        PositionRoleTemplate mapping = m002HighRiskFixture(user, null, "lvl_m1_lower");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-archive-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        AccessGrantRequestRecord pending = m002PendingHold();
        List<PermissionTemplateApplication> applications = accessRepository.templateApplications.stream()
                .filter(a -> user.equals(a.targetId())).toList();
        for (String action : List.of("archive", "restore")) {
            service.changePositionTemplateLifecycle(mapping.id(), new StatusChangeRequest("m002-lifecycle-" + action, action, "停止或恢复使用", List.of()));
            assertThat(m002PendingReleases(user)).extracting(AccessGrantRequestRecord::id).containsExactly(pending.id());
            assertThat(m002Reviews.reviews.get(pending.reviewId()).reviewStatus()).isEqualTo("pending_review");
            assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
            assertThat(accessRepository.positionRoles.get(user)).isEqualTo("role_ops_check");
            assertThat(accessRepository.templateApplications.stream().filter(a -> user.equals(a.targetId())).toList()).isEqualTo(applications);
        }
    }

    // M002-F22（回归 [0]）：停用分支只处理实际挂在这张映射上的员工——同岗同职级的旧映射停用后再保存或存档，不影响已挂在新映射上的员工的
    // 派生、挂起行、待审释放单与复核单。
    @Test
    void savingOrArchivingAnInactiveOldMappingLeavesEmployeesOnTheActiveReplacementUntouched() {
        String user = "usr_m002_replaced";
        PositionRoleTemplate old = m002HighRiskFixture(user, null, "lvl_m1_lower");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-replaced-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        AccessGrantRequestRecord first = m002PendingHold();
        service.updatePositionRoleTemplate(old.id(), m002Mapping("m002-replaced-off", "role_ops_check", "tpl_ops_check", "inactive"));
        assertThat(accessRepository.grantRequests.get(first.id()).status()).isEqualTo("rejected");
        PositionRoleTemplate replacement = service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "m002-replaced-new", null, "ops_manager", "lvl_m1_lower", "role_ops_check", "tpl_ops_check", "active"));
        AccessGrantRequestRecord current = m002PendingReleases(user).get(0);
        assertThat(accessRepository.positionMappings.get(user)).isEqualTo(replacement.id());

        service.updatePositionRoleTemplate(old.id(), m002Mapping("m002-replaced-edit", "role_ops_check", "tpl_ops_check", "inactive"));
        service.changePositionTemplateLifecycle(old.id(), new StatusChangeRequest("m002-replaced-archive", "archive", "删除旧模板前存档", List.of()));
        assertThat(m002PendingReleases(user)).extracting(AccessGrantRequestRecord::id).containsExactly(current.id());
        assertThat(m002Reviews.reviews.get(current.reviewId()).reviewStatus()).isEqualTo("pending_review");
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
        assertThat(accessRepository.positionRoles.get(user)).isEqualTo("role_ops_check");
        assertThat(accessRepository.positionMappings.get(user)).isEqualTo(replacement.id());
    }

    // M002-F22 ③：模板改映射——旧映射的待审释放单作废、旧挂起行删除，按新模板高危集合重新挂起并开单。
    @Test
    void remappingThePositionTemplateVoidsTheOldReleaseAndHoldsTheNewHighRiskSet() {
        String user = "usr_m002_remap";
        PositionRoleTemplate mapping = m002HighRiskFixture(user, null, "lvl_m1_lower");
        accessRepository.markRequiresApproval(M002_SECOND, "high");
        accessRepository.roles.put("role_ops_second", new RoleRecord(
                "role_ops_second", "ops_second", "Second", "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                List.of(M002_SECOND), null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_second", "tpl_ops_second", "Ops Second", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_ops_second", M002_SECOND, AccessRepository.Effect.GRANT, "project", false)));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-remap-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        AccessGrantRequestRecord old = m002PendingHold();

        service.updatePositionRoleTemplate(mapping.id(), m002Mapping("m002-remap", "role_ops_second", "tpl_ops_second", "active"));
        assertThat(accessRepository.grantRequests.get(old.id()).status()).isEqualTo("rejected");
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
        assertThat(overrideSources(user, M002_SECOND, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
        AccessGrantRequestRecord renewed = m002PendingReleases(user).get(0);
        assertThat(m002PendingReleases(user)).hasSize(1);
        assertThat(renewed.permissionCodes()).containsExactly(M002_SECOND);

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThat(service.reviewGrant(renewed.id(), m002Approve("m002-remap-approve")).status()).isEqualTo("applied");
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).contains(M002_SECOND).doesNotContain(M002_CODE);
    }

    // M002-F20/F22（映射与模板快照都没变）：原样重存岗位映射——待审期间保留原单、不重复开单；驳回后原样重存不自动重开单（驳回仍有效）；
    // 对同一挂起再次申请后再重存仍保留该单；批准生效后再重存，该码保持生效、不重新挂起。
    @Test
    void resavingTheSameMappingKeepsPendingRejectedAndApprovedStates() {
        String user = "usr_m002_resave";
        PositionRoleTemplate mapping = m002HighRiskFixture(user, null, "lvl_m1_lower");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-resave-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        AccessGrantRequestRecord first = m002PendingHold();
        service.updatePositionRoleTemplate(mapping.id(), m002Mapping("m002-resave-1", "role_ops_check", "tpl_ops_check", "active"));
        assertThat(m002PendingReleases(user)).extracting(AccessGrantRequestRecord::id).containsExactly(first.id());

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        service.reviewGrant(first.id(), m002Reject("m002-resave-reject"));
        authenticateAsPlatformAdmin("seed-org");
        service.updatePositionRoleTemplate(mapping.id(), m002Mapping("m002-resave-2", "role_ops_check", "tpl_ops_check", "active"));
        assertThat(m002PendingReleases(user)).isEmpty();
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
        AccessGrantRequestRecord second = service.applyGrant(m002Apply("m002-resave-reapply", "position_high_risk_release", user, List.of(M002_CODE)));
        service.updatePositionRoleTemplate(mapping.id(), m002Mapping("m002-resave-3", "role_ops_check", "tpl_ops_check", "active"));
        assertThat(m002PendingReleases(user)).extracting(AccessGrantRequestRecord::id).containsExactly(second.id());

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThat(service.reviewGrant(second.id(), m002Approve("m002-resave-approve")).status()).isEqualTo("applied");
        authenticateAsPlatformAdmin("seed-org");
        service.updatePositionRoleTemplate(mapping.id(), m002Mapping("m002-resave-4", "role_ops_check", "tpl_ops_check", "active"));
        assertThat(m002PendingReleases(user)).isEmpty();
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).contains(M002_CODE);
        assertThat(m002Releases(user)).hasSize(2);
    }

    // M002-F20（回归 [1]：「原先未生效的……一并作废」只作废未生效的）：已批准生效的岗位高危码，同岗重定后保持生效、不重新挂起、不开新单；
    // 经定岗申请整单审批落地生效的同样保持。
    @Test
    void approvedHighRiskCodesStayEffectiveWhenTheSamePositionIsReassigned() {
        String user = "usr_m002_kept";
        String landed = "usr_m002_kept_landed";
        m002HighRiskFixture(user, null, "lvl_m1_lower");
        accessRepository.users.put(landed, new AccessUser(landed, landed, "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-kept-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        AccessGrantRequestRecord positionRequest = service.applyGrant(new AccessGrantApplyRequest(
                "m002-kept-apply-position", "M002", List.of(), "user_position", landed, null, "ops_manager", "lvl_m1_lower",
                null, null, null, null, null, null, false, "岗位申请"));
        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        service.reviewGrant(m002PendingReleases(user).get(0).id(), m002Approve("m002-kept-approve-release"));
        service.reviewGrant(positionRequest.id(), m002Approve("m002-kept-approve-position"));

        authenticateAsPlatformAdmin("seed-org");
        for (String target : List.of(user, landed)) {
            assertThat(authorizationService.effectivePermissionsForUser(target).grants()).contains(M002_CODE);
            int releases = m002Releases(target).size();
            service.assignUserPosition(new UserPositionAssignmentRequest(
                    "m002-kept-reassign-" + target, null, target, "ops_manager", "lvl_m1_lower", null, null, null));
            assertThat(m002PendingReleases(target)).isEmpty();
            assertThat(m002Releases(target)).hasSize(releases);
            assertThat(overrideSources(target, M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
            assertThat(authorizationService.effectivePermissionsForUser(target).grants()).contains(M002_CODE);
        }
    }

    // M002-F20（治理地图第 703 行「……原先未生效的这类权限及其待审批申请一并作废，按新岗位或新模板重新判断」，只作废未生效的）：
    // 已批准生效的码按（员工、映射、码）判定——员工仍挂在同一张映射上时，所引权限模板内容被修改、映射改角色都不让它重新挂起，
    // 映射新增的高危码挂起并恰开一张单；码先移出映射的须审批集合再加回属新增，重新挂起开单；改职级换到另一张映射按新岗位重新挂起。
    @Test
    void approvedHighRiskCodeStaysEffectiveOnTheSameMappingAndIsRejudgedOnAnotherMapping() {
        String user = "usr_m002_same_mapping";
        PositionRoleTemplate mapping = m002HighRiskFixture(user, null, "lvl_m1_lower");
        accessRepository.markRequiresApproval(M002_SECOND, "high");
        accessRepository.roles.put("role_ops_check_second", new RoleRecord(
                "role_ops_check_second", "ops_check_second", "Checker and second", "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                List.of(M002_CODE, M002_SECOND), null));
        m002ViewMapping("ops_viewer");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-same-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        service.reviewGrant(m002PendingHold().id(), m002Approve("m002-same-approve"));
        authenticateAsPlatformAdmin("seed-org");

        // ① 所引权限模板内容被修改（加一个日常码）：已批准的码保持生效，不挂起、不开新单
        service.updatePermissionTemplate("tpl_ops_check", new PermissionTemplateRequest(
                "m002-same-template-edit", "加日常码", List.of(), "Ops Check", "base", null, List.of(M002_CODE, M002_VIEW), "project",
                null, null, false));
        assertThat(m002Releases(user)).hasSize(1);
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).contains(M002_CODE, M002_VIEW);

        // ② 映射改角色、新增一个高危码：已批准的码保持生效；新增的高危码挂起并恰开一张单
        service.updatePositionRoleTemplate(mapping.id(), m002Mapping("m002-same-remap", "role_ops_check_second", "tpl_ops_check", "active"));
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
        assertThat(overrideSources(user, M002_SECOND, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
        assertThat(m002PendingReleases(user)).singleElement()
                .satisfies(release -> assertThat(release.permissionCodes()).containsExactly(M002_SECOND));
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).contains(M002_CODE).doesNotContain(M002_SECOND);

        // ③ 码先移出映射的须审批集合、再加回：属新增，重新挂起开单（此前的批准不复活）
        service.updatePositionRoleTemplate(mapping.id(), m002Mapping("m002-same-view", "role_ops_view", "tpl_ops_view", "active"));
        assertThat(m002PendingReleases(user)).isEmpty();
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).doesNotContain(M002_CODE, M002_SECOND);
        service.updatePositionRoleTemplate(mapping.id(), m002Mapping("m002-same-back", "role_ops_check", "tpl_ops_check", "active"));
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
        AccessGrantRequestRecord readded = m002PendingHold();
        assertThat(m002PendingReleases(user)).hasSize(1);
        assertThat(readded.permissionCodes()).containsExactly(M002_CODE);
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).doesNotContain(M002_CODE);

        // ④ 批准后改职级、换到同岗位另一职级同样含该码的映射：按新岗位重新挂起、另开新单
        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        service.reviewGrant(readded.id(), m002Approve("m002-same-approve-readded"));
        authenticateAsPlatformAdmin("seed-org");
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).contains(M002_CODE);
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "m002-same-map-upper", null, "ops_manager", "lvl_m10_upper", "role_ops_check", "tpl_ops_check", "active"));
        service.assignStaffLevel(new StaffLevelAssignmentRequest("m002-same-level", null, user, "lvl_m10_upper", null, null, null));
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
        assertThat(m002PendingReleases(user)).singleElement().satisfies(release -> {
            assertThat(release.staffLevelId()).isEqualTo("lvl_m10_upper");
            assertThat(release.permissionCodes()).containsExactly(M002_CODE);
        });
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).doesNotContain(M002_CODE);
    }

    // M002-F20（同上第 703 行「员工换岗……按新岗位……重新判断」）：员工岗位行没挂在这张映射上（映射列为空），
    // 或原映射停用后改挂同岗同职级的新映射——同岗同职级的旧批准不算这张映射的批准，该码按新岗位重新挂起、另开新单。
    @Test
    void approvedHighRiskCodeIsRejudgedWhenTheEmployeeIsNotOnTheSameMapping() {
        String user = "usr_m002_other_mapping";
        PositionRoleTemplate mapping = m002HighRiskFixture(user, null, "lvl_m1_lower");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-other-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        service.reviewGrant(m002PendingHold().id(), m002Approve("m002-other-approve"));
        authenticateAsPlatformAdmin("seed-org");

        accessRepository.positionMappings.remove(user); // 岗位行未记映射
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-other-reassign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
        AccessGrantRequestRecord unlinked = m002PendingHold();
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).doesNotContain(M002_CODE);
        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        service.reviewGrant(unlinked.id(), m002Approve("m002-other-approve-unlinked"));
        authenticateAsPlatformAdmin("seed-org");
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).contains(M002_CODE);

        service.updatePositionRoleTemplate(mapping.id(), m002Mapping("m002-other-off", "role_ops_check", "tpl_ops_check", "inactive"));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "m002-other-replacement", null, "ops_manager", "lvl_m1_lower", "role_ops_check", "tpl_ops_check", "active"));
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
        assertThat(m002PendingReleases(user)).singleElement()
                .satisfies(release -> assertThat(release.permissionCodes()).containsExactly(M002_CODE));
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).doesNotContain(M002_CODE);
    }

    // M002-F20（[0][4][8]；第 703 行只作废「原先未生效的」）：定岗时已有员工显式拒绝的高危码不挂起、不开单，此后经「解除员工显式拒绝」
    // 获批而生效，算已批准生效——所引模板内容被修改、映射原样重存、同岗重定（及其后再改模板）都保持生效、不挂起、不开单，另记 already_approved。
    @Test
    void highRiskCodeLiftedByAnApprovedDenyRemovalStaysEffectiveOnTheSameMapping() {
        String user = "usr_m002_lifted";
        PositionRoleTemplate mapping = m002HighRiskFixture(user, null, "lvl_m1_lower");
        m002ExplicitDeny(user, "m002-lifted-deny");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-lifted-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        AccessGrantRequestRecord lift = service.applyGrant(m002Apply(
                "m002-lifted-apply", "user_permission_deny_removal", user, List.of(M002_CODE)));
        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThat(service.reviewGrant(lift.id(), m002Approve("m002-lifted-approve")).status()).isEqualTo("applied");
        authenticateAsPlatformAdmin("seed-org");
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).contains(M002_CODE);

        List<Runnable> paths = List.of(
                () -> service.updatePermissionTemplate("tpl_ops_check", new PermissionTemplateRequest(
                        "m002-lifted-template", "加日常码", List.of(), "Ops Check", "base", null, List.of(M002_CODE, M002_VIEW), "project",
                        null, null, false)),
                () -> service.updatePositionRoleTemplate(mapping.id(), m002Mapping("m002-lifted-resave", "role_ops_check", "tpl_ops_check", "active")),
                () -> service.assignUserPosition(new UserPositionAssignmentRequest(
                        "m002-lifted-reassign", null, user, "ops_manager", "lvl_m1_lower", null, null, null)),
                () -> service.updatePermissionTemplate("tpl_ops_check", new PermissionTemplateRequest(
                        "m002-lifted-template-2", "去掉日常码", List.of(), "Ops Check", "base", null, List.of(M002_CODE), "project",
                        null, null, false)));
        for (Runnable path : paths) {
            path.run();
            assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
            assertThat(m002Releases(user)).isEmpty();
            assertThat(authorizationService.effectivePermissionsForUser(user).grants()).contains(M002_CODE);
        }
        // M002-F20（[2]）：保持生效的已批准码另记 already_approved，不记进 already_denied
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "access.user_position.high_risk_already_approved".equals(event.eventName())
                        && user.equals(event.businessId()))
                .hasSize(4).allSatisfy(event -> assertThat(event.metadata().get("permission_codes")).isEqualTo(M002_CODE));
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "access.user_position.high_risk_already_denied".equals(event.eventName())
                        && user.equals(event.businessId()))
                .hasSize(1); // 只有首次定岗时显式拒绝那一次
    }

    // M002-F20（[0][4][8]）：解除显式拒绝的批准只在员工挂上当前映射之后才算——挂上当前映射之前落地的解除单不算这张映射的批准。
    // 定岗时该码尚不须审批（不挂起），此后改为须审批；同岗重定时它在映射须审批集合内、没有挂起行，但只有一张定岗前落地的解除单，重新挂起开单。
    @Test
    void denyRemovalApprovedBeforeTheEmployeeWasOnTheMappingDoesNotCount() {
        String user = "usr_m002_stale_lift";
        m002HighRiskFixture("usr_m002_stale_peer", null, "lvl_m1_lower");
        accessRepository.users.put(user, new AccessUser(user, user, "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        String later = "governance.supervisor_review.view"; // 定岗时为日常码、操作人持有，此后改为须审批
        accessRepository.roles.put("role_ops_later", new RoleRecord(
                "role_ops_later", "ops_later", "Later high risk", "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                List.of(later), null));
        m002ViewMapping("ops_viewer");
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "m002-map-stale", null, "ops_stale", "lvl_m1_lower", "role_ops_later", "tpl_ops_view", "active"));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-stale-assign", null, user, "ops_stale", "lvl_m1_lower", null, null, null));
        assertThat(m002Releases(user)).isEmpty();
        Instant before = accessRepository.findUserPositionAssignment(user).orElseThrow().assignedAt().minusSeconds(86400);
        accessRepository.grantRequests.put("agr_m002_stale_lift", new AccessGrantRequestRecord(
                "agr_m002_stale_lift", "user_permission_deny_removal", user, null, null, null, null, null, null, null,
                List.of(later), null, null, false, "applied", null, "定岗前落地的解除单", "usr_admin", "usr_boss", before,
                "seed-org", null, before, before));
        accessRepository.markRequiresApproval(later, "high");

        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-stale-reassign", null, user, "ops_stale", "lvl_m1_lower", null, null, null));
        assertThat(overrideSources(user, later, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
        assertThat(m002PendingReleases(user)).singleElement()
                .satisfies(release -> assertThat(release.permissionCodes()).containsExactly(later));
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).doesNotContain(later);
    }

    // M002-F22（[3][11]，第 703 行「该岗位模板被…修改」）：映射改职级后，原来挂在这张映射上的员工（岗位行仍是旧岗位旧职级）按其当前岗位职级
    // 重新派生：找不到映射即收回岗位派生，未生效的挂起与待审释放单作废、复核单关闭；此后再改该映射所引模板，也不会按映射当前角色与模板
    // 替这名员工删挂起或开单。
    @Test
    void changingTheMappingsStaffLevelRejudgesEmployeesLeftOnIt() {
        String user = "usr_m002_stranded";
        PositionRoleTemplate mapping = m002HighRiskFixture(user, null, "lvl_m1_lower");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-stranded-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        AccessGrantRequestRecord pending = m002PendingHold();

        service.updatePositionRoleTemplate(mapping.id(), new PositionRoleTemplateRequest(
                "m002-stranded-move", null, "ops_manager", "lvl_m10_upper", "role_ops_check", "tpl_ops_check", "active"));
        assertThat(accessRepository.grantRequests.get(pending.id()).status()).isEqualTo("rejected");
        assertThat(m002Reviews.reviews.get(pending.reviewId()).reviewStatus()).isEqualTo("rejected");
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
        assertThat(accessRepository.positionRoles.get(user)).isNull();
        assertThat(accessRepository.positionMappings.get(user)).isNull();
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).doesNotContain(M002_CODE);

        service.updatePermissionTemplate("tpl_ops_check", new PermissionTemplateRequest(
                "m002-stranded-template", "加日常码", List.of(), "Ops Check", "base", null, List.of(M002_CODE, M002_VIEW), "project",
                null, null, false));
        assertThat(m002PendingReleases(user)).isEmpty();
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).doesNotContain(M002_CODE, M002_VIEW);
    }

    // M002-F22（[3]）：映射改职级时，原来挂在这张映射上的员工若在其岗位职级下另有映射（本组织映射），按「模板被修改」改挂过去——
    // 已批准生效、在新映射上仍须审批的码保持生效，不重新挂起、不开单。
    @Test
    void changingTheMappingsStaffLevelKeepsApprovedCodesWhenTheEmployeeMovesToAnotherMapping() {
        String user = "usr_m002_stranded_kept";
        PositionRoleTemplate mapping = m002HighRiskFixture(user, null, "lvl_m1_lower");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-kept-move-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        service.reviewGrant(m002PendingHold().id(), m002Approve("m002-kept-move-approve"));
        authenticateAsPlatformAdmin("seed-org");
        accessRepository.savePositionRoleTemplate(new PositionRoleTemplate("prt_m002_org_kept", "ops_manager", "lvl_m1_lower",
                "role_ops_check", "tpl_ops_check", "seed-org", "active", "usr_admin", Instant.now(), Instant.now()));

        service.updatePositionRoleTemplate(mapping.id(), new PositionRoleTemplateRequest(
                "m002-kept-move", null, "ops_manager", "lvl_m10_upper", "role_ops_check", "tpl_ops_check", "active"));
        assertThat(accessRepository.positionMappings.get(user)).isEqualTo("prt_m002_org_kept");
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
        assertThat(m002Releases(user)).hasSize(1);
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).contains(M002_CODE);
    }

    // M002-F20（[10]，确定性交错）：改模板先按编号锁在岗员工行、再锁模板行；锁后按映射 id 重新取人并逐人复核——列名单之后、锁员工行之前
    // 已换岗提交的员工不按旧映射对账：新岗位的挂起与待审释放单原样保留，该码不因旧模板被修改而免审生效。
    @Test
    void editingATemplateSkipsAnEmployeeWhoMovedAwayBeforeTheirRowWasLocked() {
        String user = "usr_m002_edit_race";
        m002HighRiskFixture(user, null, "lvl_m1_lower");
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_race", "tpl_ops_race", "Ops Race", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_ops_race", M002_CODE, AccessRepository.Effect.GRANT, "project", false),
                        new PermissionTemplateItem("tpl_ops_race", M002_VIEW, AccessRepository.Effect.GRANT, "project", false)));
        m002ViewMapping("ops_viewer");
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "m002-map-race-old", null, "ops_race", "lvl_m1_lower", "role_ops_view", "tpl_ops_race", "active"));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-race-assign", null, user, "ops_race", "lvl_m1_lower", null, null, null));
        AccessGrantRequestRecord old = m002PendingHold();
        accessRepository.lockOrder.clear();
        accessRepository.beforeNextUserLock = () -> service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-race-move", null, user, "ops_manager", "lvl_m1_lower", null, null, null));

        service.updatePermissionTemplate("tpl_ops_race", new PermissionTemplateRequest(
                "m002-race-edit", "去掉高危码", List.of(), "Ops Race", "base", null, List.of(M002_VIEW), "project", null, null, false));
        assertThat(accessRepository.lockOrder.indexOf("edit:tpl_ops_race")).isGreaterThan(accessRepository.lockOrder.lastIndexOf("user:" + user));
        assertThat(accessRepository.grantRequests.get(old.id()).status()).isEqualTo("rejected");
        assertThat(m002PendingReleases(user)).singleElement().satisfies(release -> {
            assertThat(release.positionCode()).isEqualTo("ops_manager");
            assertThat(release.permissionCodes()).containsExactly(M002_CODE);
        });
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).doesNotContain(M002_CODE);
    }

    // M002-F20（[4]，确定性交错）：改模板对账在员工行锁下重读映射当前的角色与模板——循环开头的映射快照里还引用被编辑模板的空映射，
    // 在对账轮到它之前被改指另一模板、随即有人定岗；该员工不按旧快照对账：新挂起行与新释放单保留，码不生效。
    @Test
    void editingATemplateSkipsAMappingRepointedToAnotherTemplateBeforeItsTurn() {
        String user = "usr_m002_repointed";
        m002HighRiskFixture("usr_m002_repointed_peer", null, "lvl_m1_lower");
        accessRepository.users.put(user, new AccessUser(user, user, "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        m002ViewMapping("ops_viewer");
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_shared", "tpl_ops_shared", "Ops Shared", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")), List.of());
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "m002-map-shared-a", null, "ops_shared_a", "lvl_m1_lower", "role_ops_view", "tpl_ops_shared", "active"));
        PositionRoleTemplate empty = service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "m002-map-shared-b", null, "ops_shared_b", "lvl_m1_lower", "role_ops_view", "tpl_ops_shared", "active"));
        // 第一次锁员工行（编辑预锁）时装上第二个钩子；第二次（对账轮到第一张共用映射、补锁时）空映射改指含高危码的模板并随即定岗
        accessRepository.beforeNextUserLock = () -> {
            accessRepository.beforeNextUserLock = () -> {
                service.updatePositionRoleTemplate(empty.id(), new PositionRoleTemplateRequest(
                        "m002-repoint", null, "ops_shared_b", "lvl_m1_lower", "role_ops_view", "tpl_ops_check", "active"));
                service.assignUserPosition(new UserPositionAssignmentRequest(
                        "m002-repointed-assign", null, user, "ops_shared_b", "lvl_m1_lower", null, null, null));
            };
        };

        service.updatePermissionTemplate("tpl_ops_shared", new PermissionTemplateRequest(
                "m002-shared-edit", "加日常码", List.of(), "Ops Shared", "base", null, List.of(M002_VIEW), "project", null, null, false));
        assertThat(accessRepository.positionMappings.get(user)).isEqualTo(empty.id());
        assertThat(m002PendingReleases(user)).singleElement().satisfies(release -> {
            assertThat(release.templateId()).isEqualTo("tpl_ops_check");
            assertThat(release.permissionCodes()).containsExactly(M002_CODE);
        });
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).doesNotContain(M002_CODE);
    }

    // M002-F25（审批落地核对当前映射）：岗位高危释放单钉住发起时员工所挂的岗位映射——员工改挂另一张映射后（岗位、职级、角色、模板都相同），
    // 旧单审批落地报 GRANT_SNAPSHOT_CHANGED，不解除挂起、不改单状态。
    @Test
    void releaseApprovalIsRefusedOnceTheEmployeeIsOnAnotherMapping() {
        String user = "usr_m002_rebound";
        m002HighRiskFixture(user, null, "lvl_m1_lower");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-rebound-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        AccessGrantRequestRecord pending = m002PendingHold();
        accessRepository.positionMappings.put(user, "prt_m002_rebound_other"); // 岗位行改挂另一张映射，其余授权事实不变

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThatThrownBy(() -> service.reviewGrant(pending.id(), m002Approve("m002-rebound-approve")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_SNAPSHOT_CHANGED"));
        assertThat(accessRepository.grantRequests.get(pending.id()).status()).isEqualTo("pending");
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
    }

    // M002-F22（[11]）：改模板对账在员工行锁下逐人复核——岗位行的岗位职级与映射对不上的员工，不拿映射当前角色与模板去算其高危集合：
    // 不删其挂起行、不作废其待审释放单。
    @Test
    void templateEditLeavesAnEmployeeWhosePositionNoLongerMatchesTheMapping() {
        String user = "usr_m002_mismatch";
        m002HighRiskFixture(user, null, "lvl_m1_lower");
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_mismatch", "tpl_ops_mismatch", "Ops Mismatch", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_ops_mismatch", M002_CODE, AccessRepository.Effect.GRANT, "project", false),
                        new PermissionTemplateItem("tpl_ops_mismatch", M002_VIEW, AccessRepository.Effect.GRANT, "project", false)));
        m002ViewMapping("ops_viewer");
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "m002-map-mismatch", null, "ops_mismatch", "lvl_m1_lower", "role_ops_view", "tpl_ops_mismatch", "active"));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-mismatch-assign", null, user, "ops_mismatch", "lvl_m1_lower", null, null, null));
        AccessGrantRequestRecord pending = m002PendingHold();
        UserPositionAssignment row = accessRepository.findUserPositionAssignment(user).orElseThrow();
        accessRepository.saveUserPositionAssignment(new UserPositionAssignment(user, "ops_elsewhere", row.staffLevelId(),
                row.organizationId(), row.merchantId(), row.assignedBy(), row.assignedAt())); // 岗位行与映射对不上

        service.updatePermissionTemplate("tpl_ops_mismatch", new PermissionTemplateRequest(
                "m002-mismatch-edit", "去掉高危码", List.of(), "Ops Mismatch", "base", null, List.of(M002_VIEW), "project", null, null, false));
        assertThat(accessRepository.grantRequests.get(pending.id()).status()).isEqualTo("pending");
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
    }

    // M002-F20/F22（「该岗位模板被…修改」，回归 [6][10]）：修改启用中岗位映射所引权限模板的内容——原待审释放单作废并关闭其复核单，
    // 删掉的高危码的挂起行一并删除，仍在模板里的未生效高危码按新模板重新挂起、恰开一张新单，批准后生效。
    @Test
    void editingThePositionsPermissionTemplateVoidsAndRejudgesUneffectiveHolds() {
        String user = "usr_m002_template_edit";
        m002HighRiskFixture(user, null, "lvl_m1_lower");
        accessRepository.markRequiresApproval(M002_SECOND, "high");
        accessRepository.roles.put("role_ops_view_only", new RoleRecord(
                "role_ops_view_only", "ops_view_only", "Viewer", "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                List.of(M002_VIEW), null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_pair", "tpl_ops_pair", "Ops Pair", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_ops_pair", M002_CODE, AccessRepository.Effect.GRANT, "project", false),
                        new PermissionTemplateItem("tpl_ops_pair", M002_SECOND, AccessRepository.Effect.GRANT, "project", false)));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "m002-map-pair", null, "ops_pair", "lvl_m1_lower", "role_ops_view_only", "tpl_ops_pair", "active"));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-template-edit-assign", null, user, "ops_pair", "lvl_m1_lower", null, null, null));
        AccessGrantRequestRecord old = m002PendingHold();
        assertThat(old.permissionCodes()).containsExactlyInAnyOrder(M002_CODE, M002_SECOND);

        service.updatePermissionTemplate("tpl_ops_pair", new PermissionTemplateRequest(
                "m002-template-edit", "去掉第二个高危码", List.of(), "Ops Pair", "base", null, List.of(M002_CODE), "project", null, null, false));
        assertThat(accessRepository.grantRequests.get(old.id()).status()).isEqualTo("rejected");
        assertThat(m002Reviews.reviews.get(old.reviewId()).reviewStatus()).isEqualTo("rejected");
        assertThat(m002Reviews.nodes.get(old.reviewId())).noneMatch(com.complex.platform.governance.chain.ApprovalChainNode::pending);
        assertThat(overrideSources(user, M002_SECOND, AccessRepository.Effect.DENY)).isEmpty();
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
        AccessGrantRequestRecord renewed = m002PendingReleases(user).get(0);
        assertThat(m002PendingReleases(user)).hasSize(1);
        assertThat(renewed.permissionCodes()).containsExactly(M002_CODE);

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThat(service.reviewGrant(renewed.id(), m002Approve("m002-template-edit-approve")).status()).isEqualTo("applied");
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).contains(M002_CODE).doesNotContain(M002_SECOND);
    }

    // M002-F20 f（回归 [21]：复现假成功前提）：码只有岗位高危挂起、没有任何授予来源时直接解除——基线删 0 行照样 200 并记成功事件；
    // 现在 400，挂起行不动、不记 remove 成功事件。
    @Test
    void removingAHoldWithoutAnyGrantSourceIsRejectedInsteadOfSucceedingSilently() {
        String user = "usr_m002_hold_only";
        m002HighRiskFixture(user, null, "lvl_m1_lower");
        accessRepository.saveHighRiskHolds(user, List.of(M002_CODE), "岗位高危挂起", "usr_admin");
        assertThat(accessRepository.findPermissionEffects(user, Instant.now()))
                .noneMatch(effect -> M002_CODE.equals(effect.permissionCode()) && effect.effect() == AccessRepository.Effect.GRANT);
        assertThatThrownBy(() -> service.removeUserPermissionOverrides(user, m002RemoveDeny("m002-hold-only-remove", M002_CODE)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.POSITION_HOLD);
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).doesNotContain("access.user_permission_override.remove");
    }

    // M002-F21：两类授权申请的权限码列表混有 null 或空白时，在排序、查库与任何写入前 400 VALIDATION_FAILED；不建单、不写指纹、
    // 不建复核单、无成功事件，失败留痕错误码为 VALIDATION_FAILED。
    @Test
    void overrideRequestsRejectNullOrBlankCodesBeforeSorting() {
        String user = "usr_m002_null_codes";
        m002HighRiskFixture(user, null, "lvl_m1_lower");
        int attempts = 0;
        for (String type : List.of("user_permission_grant", "user_permission_deny_removal")) {
            for (List<String> codes : List.of(java.util.Arrays.asList(M002_CODE, null), java.util.Arrays.asList(null, M002_CODE),
                    List.of(M002_CODE, "  "))) {
                String key = "m002-null-codes-" + (++attempts);
                assertThatThrownBy(() -> service.applyGrant(m002Apply(key, type, user, codes)))
                        .isInstanceOfSatisfying(ApiException.class, e -> {
                            assertThat(e.status().value()).isEqualTo(400);
                            assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                        });
            }
        }
        assertThat(accessRepository.grantRequests.values()).isEmpty();
        assertThat(accessRepository.fingerprints).isEmpty();
        assertThat(m002Reviews.reviews).isEmpty(); // 回归 [20]：校验先于建复核单
        assertThat(m002Reviews.nodes).isEmpty();
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).doesNotContain("access.grant_request.applied");
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "access.grant_request.apply_failed".equals(event.eventName()))
                .extracting(event -> event.metadata().get("error_code"))
                .hasSize(6).containsOnly("VALIDATION_FAILED");
    }

    // M002-F23（OpenAPI RoleRequest 契约）：role_name 121 字、缺失或全空白，description 1001 字，data_scope 65 字或不在枚举内，permission_codes 缺失，
    // 均 400 VALIDATION_FAILED，不留角色、无成功事件；120 字（去首尾空白后）与六个合法枚举值可建。
    @Test
    void createRoleEnforcesTheRoleRequestContractBeforeWriting() {
        int roles = accessRepository.roles.size();
        List<RoleRequest> invalid = List.of(
                m002Role("m002-role-name-121", "角".repeat(121), null, List.of(), "project"),
                m002Role("m002-role-name-missing", null, null, List.of(), "project"),
                m002Role("m002-role-name-blank", "   ", null, List.of(), "project"),
                m002Role("m002-role-description", "说明超长", "说".repeat(1001), List.of(), "project"),
                m002Role("m002-role-scope-65", "范围超长", null, List.of(), "s".repeat(65)),
                m002Role("m002-role-scope-unknown", "范围不在枚举", null, List.of(), "tenant"),
                m002Role("m002-role-codes-missing", "缺权限码", null, null, "project"));
        for (RoleRequest request : invalid) {
            assertThatThrownBy(() -> service.createRole(request))
                    .isInstanceOfSatisfying(ApiException.class, e -> {
                        assertThat(e.status().value()).isEqualTo(400);
                        assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                    });
        }
        assertThat(accessRepository.roles).hasSize(roles);
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).doesNotContain("role.changed");
        assertThat(auditEventRepository.events).filteredOn(event -> "role.change_failed".equals(event.eventName()))
                .extracting(event -> event.metadata().get("error_code")).hasSize(invalid.size()).containsOnly("VALIDATION_FAILED");

        RoleRecord longest = service.createRole(m002Role("m002-role-120", "  " + "角".repeat(120) + "  ", "说".repeat(1000), List.of(), null));
        assertThat(accessRepository.roles.get(longest.id()).roleName()).isEqualTo("角".repeat(120));
        assertThat(accessRepository.roles.get(longest.id()).dataScope()).isEqualTo("project");
        for (String scope : List.of("global", "project", "merchant", "bar", "space", "self")) {
            RoleRecord created = service.createRole(m002Role("m002-role-scope-" + scope, "范围角色" + scope, null, List.of(M002_VIEW), scope));
            assertThat(accessRepository.roles.get(created.id()).dataScope()).isEqualTo(scope);
        }
    }

    private static final String M002_SECOND = "ops.production_readiness.approve";

    private RoleRequest m002Role(String key, String name, String description, List<String> codes, String dataScope) {
        return new RoleRequest(key, "M002", List.of(), name, description, codes, dataScope);
    }

    private PositionRoleTemplateRequest m002Mapping(String key, String roleId, String templateId, String status) {
        return new PositionRoleTemplateRequest(key, null, "ops_manager", "lvl_m1_lower", roleId, templateId, status);
    }

    // 只含日常码的岗位映射：position / lvl_m1_lower → role_ops_view（view）+ tpl_ops_view（view）。
    private void m002ViewMapping(String positionCode) {
        accessRepository.roles.put("role_ops_view", new RoleRecord(
                "role_ops_view", "ops_view", "Ops viewer", "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                List.of(M002_VIEW), null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_view", "tpl_ops_view", "Ops View", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_ops_view", M002_VIEW, AccessRepository.Effect.GRANT, "project", false)));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "m002-map-view-" + positionCode, null, positionCode, "lvl_m1_lower", "role_ops_view", "tpl_ops_view", "active"));
    }

    private List<AccessGrantRequestRecord> m002Releases(String userId) {
        return accessRepository.grantRequests.values().stream()
                .filter(g -> "position_high_risk_release".equals(g.requestType()) && userId.equals(g.targetUserId()))
                .toList();
    }

    private List<AccessGrantRequestRecord> m002PendingReleases(String userId) {
        return m002Releases(userId).stream().filter(g -> "pending".equals(g.status())).toList();
    }

    private AccessGrantReviewRequest m002Reject(String idempotencyKey) {
        return new AccessGrantReviewRequest(idempotencyKey, "reject", List.of(), "reject", "驳回");
    }

    // M002：共用装配——check 标为须审批高危；usr_admin（平台管理员、申请人）的直接上级为 usr_boss；
    // 高危岗位映射 ops_manager / highRiskLevel → role_ops_check + tpl_ops_check（沿用上方高危定岗用例的装配）。
    private InMemorySupervisorReviewRepository m002Reviews; // M002-F20：夹具的复核单仓储（断言作废时复核单已关闭）

    private PositionRoleTemplate m002HighRiskFixture(String userId, String merchantId, String highRiskLevel) {
        accessRepository.markRequiresApproval(M002_CODE, "high");
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        m002Reviews = reviewRepository;
        reviewRepository.superiors.put("usr_admin", "usr_boss");
        service = newAccessService(true, supervisorReviewService(reviewRepository));
        accessRepository.users.put(userId, new AccessUser(
                userId, userId, "staff", "seed-org", merchantId, "active", Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_ops_check", new RoleRecord(
                "role_ops_check", "ops_check", "Ops checker", "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                List.of(M002_CODE), null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_check", "tpl_ops_check", "Ops Check", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_ops_check", M002_CODE, AccessRepository.Effect.GRANT, "project", false)));
        return service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "m002-map-" + userId, null, "ops_manager", highRiskLevel, "role_ops_check", "tpl_ops_check", "active"));
    }

    private AccessGrantRequestRecord m002PendingHold() {
        return accessRepository.grantRequests.values().stream()
                .filter(g -> "position_high_risk_release".equals(g.requestType()) && "pending".equals(g.status()))
                .findFirst().orElseThrow();
    }

    private void m002ExplicitDeny(String userId, String idempotencyKey) {
        service.setUserPermissionOverrides(userId, new UserPermissionOverrideRequest(
                idempotencyKey, "停权", List.of(M002_CODE), "deny", "员工显式拒绝"));
    }

    private AccessGrantApplyRequest m002Apply(String idempotencyKey, String requestType, String userId, List<String> codes) {
        return new AccessGrantApplyRequest(idempotencyKey, "M002", List.of(), requestType, userId, null, null, null, null, null, null,
                codes, null, null, null, "员工覆盖授权申请");
    }

    private AccessGrantReviewRequest m002Approve(String idempotencyKey) {
        return new AccessGrantReviewRequest(idempotencyKey, "approve", List.of(), "approve", "ok");
    }

    private static final String M002_VIEW = "ops.production_readiness.view";

    // M002-F05：日常码装配——目标经 role_view 持有 view，并有一条员工显式拒绝。
    private void m002ViewDenyFixture(String targetUserId) {
        accessRepository.users.put(targetUserId, new AccessUser(
                targetUserId, targetUserId, "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_view", new RoleRecord(
                "role_view", "view", "Viewer", "project", "active", Instant.parse("2026-06-14T00:00:00Z"), List.of(M002_VIEW), null));
        accessRepository.userRoles.put(targetUserId, new LinkedHashSet<>(Set.of("role_view")));
        accessRepository.saveUserPermissionOverrides(targetUserId, List.of(M002_VIEW), AccessRepository.Effect.DENY, "停权", "usr_admin");
    }

    private UserPermissionOverrideRequest m002RemoveDeny(String idempotencyKey, String code) {
        return new UserPermissionOverrideRequest(idempotencyKey, null, List.of(code), "deny", "解除显式拒绝");
    }

    private List<AccessRepository.Source> overrideSources(String userId, String code, AccessRepository.Effect effect) {
        return accessRepository.overrides.stream()
                .filter(o -> o.userId().equals(userId) && o.permissionCode().equals(code) && o.effect() == effect)
                .map(AccessRepository.PermissionEffect::source)
                .toList();
    }

    // M006-R6-03（PGM-1.18 后台报错全中文）：残留的英文校验文案改为中文。
    @Test
    void remainingValidationMessagesAreChinese() throws Exception {
        service = newAccessService(true, supervisorReviewService(new InMemorySupervisorReviewRepository()));
        accessRepository.users.put("usr_cn", new AccessUser("usr_cn", "cn", "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_ops_check", new RoleRecord(
                "role_ops_check", "ops_check", "Ops checker", "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                List.of("ops.production_readiness.check"), null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_check", "tpl_ops_check", "Ops Check", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_ops_check", "ops.production_readiness.check", AccessRepository.Effect.GRANT, "project", false)));

        assertThatThrownBy(() -> service.createTemporaryGrant(new TemporaryGrantRequest(
                "tg-cn", null, List.of(), "usr_cn", List.of("ops.production_readiness.view"), Instant.parse("2026-06-14T00:00:00Z"), null, false, "r")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getMessage()).contains("失效时间").doesNotContainPattern("[A-Za-z]"));
        assertThatThrownBy(() -> service.applyGrant(new AccessGrantApplyRequest(
                "ag-cn", null, List.of(), "temporary_permission", "usr_cn", null, null, null, null, null, null,
                List.of("ops.production_readiness.view"), Instant.parse("2026-06-14T00:00:00Z"), null, false, "r", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getMessage()).contains("到期时间").doesNotContainPattern("[A-Za-z]"));
        assertThatThrownBy(() -> service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "map-cn", null, "ops_clerk", "lvl_m1_lower", "role_ops_check", "tpl_ops_check", "bogus")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getMessage()).contains("状态").doesNotContainPattern("[A-Za-z]"));
        java.lang.reflect.Method normalizeType = AccessService.class.getDeclaredMethod("normalizeGrantRequestType", String.class);
        normalizeType.setAccessible(true);
        assertThatThrownBy(() -> normalizeType.invoke(service, "bogus"))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .extracting(t -> ((java.lang.reflect.InvocationTargetException) t).getCause())
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getMessage()).contains("申请类型").doesNotContainPattern("[A-Za-z]"));
    }

    @Test
    void scopeChangeRejectsUnchangedScope() {
        service = newAccessService(true, supervisorReviewService(new InMemorySupervisorReviewRepository()));
        accessRepository.users.put("usr_stay", new AccessUser(
                "usr_stay", "stay", "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        assertThatThrownBy(() -> service.changeUserScope("usr_stay", new UserScopeChangeRequest(
                "scope-stay", null, "seed-org", null, "ops_clerk", "lvl_m1_lower", null, null, null, "x", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SCOPE_UNCHANGED"));
    }


    // M006-016：混合风险中有未配置等级时，不能忽略它并按另一个较低等级放行。
    @Test void grantPolicyRejectsAnyUnclassifiedPermissionInTheRequestedRole() {
        accessRepository.markRequiresApproval("ops.production_readiness.check", "high");
        accessRepository.addPermission("future.permission", "unclassified", true);
        service = newAccessService(true, supervisorReviewService(new InMemorySupervisorReviewRepository()));
        accessRepository.createUser(new AccessUser("target", "target", "staff", "seed-org", null, "active", Instant.now()));
        accessRepository.roles.put("mixed", new RoleRecord("mixed", "mixed", null, "project", "active", Instant.now(),
                List.of("ops.production_readiness.check", "future.permission"), null));
        assertThatThrownBy(() -> service.applyGrant(new AccessGrantApplyRequest(
                "mixed-risk", null, List.of(), "user_role", "target", "mixed",
                null, null, null, null, null, null, null, null, false, "reason")))
                .isInstanceOf(ApiException.class);
        assertThat(accessRepository.grantRequests).isEmpty();
    }

    @Test
    void grantRequestRejectsPermissionsThatDoNotRequireApproval() {
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        service = newAccessService(true, supervisorReviewService(reviewRepository));
        accessRepository.users.put("usr_low_risk_target", new AccessUser(
                "usr_low_risk_target", "low-risk-target", "staff", "seed-org", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_ops_view", new RoleRecord(
                "role_ops_view", "ops_view", "Ops viewer", "project", "active",
                Instant.parse("2026-06-14T00:00:00Z"),
                List.of("ops.production_readiness.view"),
                null));

        assertThatThrownBy(() -> service.applyGrant(new AccessGrantApplyRequest(
                "apply-low-risk", null, List.of(), "user_role", "usr_low_risk_target",
                "role_ops_view", null, null, null, null, null, null,
                null, null, false, "not high risk")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(400);
                    assertThat(e.code()).isEqualTo("GRANT_APPROVAL_NOT_REQUIRED");
                });
        assertThat(reviewRepository.reviews).isEmpty();
    }

    @Test
    void approvedUserRoleGrantRequestUsesSupervisorReviewAndLandsAssignment() {
        accessRepository.markRequiresApproval("ops.production_readiness.check", "high");
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        service = newAccessService(true, supervisorReviewService(reviewRepository));
        accessRepository.users.put("usr_grant_target", new AccessUser(
                "usr_grant_target", "grant-target", "staff", "org_a", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_ops_check", new RoleRecord(
                "role_ops_check", "ops_check", "Ops checker", "project", "active",
                Instant.parse("2026-06-14T00:00:00Z"),
                List.of("ops.production_readiness.check"),
                null));
        accessRepository.saveStaffLevelAssignment(new StaffLevelAssignment(
                "usr_admin", "lvl_m10_upper", null, "CNY", null, "seed-org", null,
                "test", Instant.parse("2026-06-14T00:00:00Z")));
        authenticateAsOrgUser("usr_requester", "requester", "org_a");

        AccessGrantRequestRecord pending = service.applyGrant(new AccessGrantApplyRequest(
                "apply-grant-role", "need checker", List.of(), "user_role",
                "usr_grant_target", "role_ops_check", null, null, null, null, null,
                null, null, null, false, "need checker"));

        assertThat(pending.status()).isEqualTo("pending");
        assertThat(reviewRepository.reviews.get(pending.reviewId()).policyCode()).isEqualTo("permission_grant_l2");

        authenticateAsPlatformAdmin("seed-org");
        AccessGrantRequestRecord applied = service.reviewGrant(pending.id(), new AccessGrantReviewRequest(
                "review-grant-role", "approve", List.of(), "approve", "ok"));

        assertThat(applied.status()).isEqualTo("applied");
        assertThat(accessRepository.userRoles.get("usr_grant_target")).contains("role_ops_check");
    }

    @Test
    void approvedTemporaryGrantExpiresByEffectivePermissionRead() {
        accessRepository.markRequiresApproval("ops.production_readiness.check", "high");
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        service = newAccessService(true, supervisorReviewService(reviewRepository));
        accessRepository.users.put("usr_temp_target", new AccessUser(
                "usr_temp_target", "temp-target", "staff", "org_a", "merchant_a", "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.saveStaffLevelAssignment(new StaffLevelAssignment(
                "usr_admin", "lvl_m10_upper", null, "CNY", null, "seed-org", null,
                "test", Instant.parse("2026-06-14T00:00:00Z")));
        authenticateAsOrgUser("usr_requester", "requester", "org_a");

        AccessGrantRequestRecord pending = service.applyGrant(new AccessGrantApplyRequest(
                "apply-temp-expired", "temporary", List.of(), "temporary_permission",
                "usr_temp_target", null, null, null, null, null, null,
                List.of("ops.production_readiness.check"),
                Instant.parse("2020-01-01T00:00:00Z"),
                Instant.parse("2020-01-02T00:00:00Z"),
                false,
                "break glass"));

        authenticateAsPlatformAdmin("seed-org");
        AccessGrantRequestRecord applied = service.reviewGrant(pending.id(), new AccessGrantReviewRequest(
                "review-temp-expired", "approve", List.of(), "approve", "ok"));

        assertThat(applied.status()).isEqualTo("applied");
        assertThat(accessRepository.temporaryGrants.values()).hasSize(1);
        authenticateAsPlatformAdmin("org_a");
        assertThat(authorizationService.effectivePermissionsForUser("usr_temp_target").grants())
                .doesNotContain("ops.production_readiness.check");
    }

    @Test
    void auditorOutsideTemplatePermissionNeedsApprovedTemporaryGrantBeforeUse() {
        accessRepository.addPermission("finance.ar_ap.view", "medium", false);
        accessRepository.markRequiresApproval("finance.ar_ap.manage", "high");
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        service = newAccessService(true, supervisorReviewService(reviewRepository));
        accessRepository.users.put("usr_auditor_target", new AccessUser(
                "usr_auditor_target", "auditor-target", "staff", "org_a", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_auditor_internal", new RoleRecord(
                "role_auditor_internal", "AUDITOR_INTERNAL", "Internal auditor", "complex", "active",
                Instant.parse("2026-06-14T00:00:00Z"),
                List.of("finance.ar_ap.view"),
                null));
        accessRepository.assignUserRole("usr_auditor_target", "role_auditor_internal");
        accessRepository.saveStaffLevelAssignment(new StaffLevelAssignment(
                "usr_admin", "lvl_m10_upper", null, "CNY", null, "seed-org", null,
                "test", Instant.parse("2026-06-14T00:00:00Z")));

        authenticateAsOrgUser("usr_auditor_target", "auditor-target", "org_a");
        authorizationService.requirePermission("finance.ar_ap.view");
        assertThatThrownBy(() -> authorizationService.requirePermission("finance.ar_ap.manage"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(403);
                    assertThat(e.code()).isEqualTo("PERMISSION_DENIED");
                });

        authenticateAsOrgUser("usr_requester", "requester", "org_a");
        AccessGrantRequestRecord pending = service.applyGrant(new AccessGrantApplyRequest(
                "apply-auditor-temp-manage", "temporary access", List.of(), "temporary_permission",
                "usr_auditor_target", null, null, null, null, null, null,
                List.of("finance.ar_ap.manage"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2099-01-01T00:00:00Z"),
                false,
                "audit exception"));

        authenticateAsPlatformAdmin("seed-org");
        AccessGrantRequestRecord applied = service.reviewGrant(pending.id(), new AccessGrantReviewRequest(
                "review-auditor-temp-manage", "approve", List.of(), "approve", "ok"));

        assertThat(applied.status()).isEqualTo("applied");
        authenticateAsOrgUser("usr_auditor_target", "auditor-target", "org_a");
        authorizationService.requirePermission("finance.ar_ap.manage");
    }

    @Test
    void grantSummaryReportsFrequentTemporaryRequestsByPositionAndUser() {
        Instant now = Instant.now();
        accessRepository.users.put("usr_summary", new AccessUser(
                "usr_summary", "summary", "staff", "seed-org", null, "active", now));
        accessRepository.grantRequests.put("agr_summary_1", temporaryGrantRequest(
                "agr_summary_1", "usr_summary", "cashier", "lvl_m1_lower", "ops.production_readiness.check", now));
        accessRepository.grantRequests.put("agr_summary_2", temporaryGrantRequest(
                "agr_summary_2", "usr_summary", "cashier", "lvl_m1_lower", "ops.production_readiness.check", now));

        accessRepository.policy = accessRepository.policy.replace("\"value\":3", "\"value\":2");
        AccessGrantSummary summary = service.grantSummary(null, null, null);

        assertThat(summary.positionRecommendations())
                .extracting(AccessGrantPositionSummary::positionCode,
                        AccessGrantPositionSummary::staffLevelId,
                        AccessGrantPositionSummary::permissionCode,
                        AccessGrantPositionSummary::requestCount)
                .contains(org.assertj.core.groups.Tuple.tuple(
                        "cashier", "lvl_m1_lower", "ops.production_readiness.check", 2L));
        assertThat(summary.userAnomalies())
                .extracting(AccessGrantUserSummary::userId, AccessGrantUserSummary::requestCount)
                .contains(org.assertj.core.groups.Tuple.tuple("usr_summary", 2L));
        accessRepository.policy = accessRepository.policy.replace("\"value\":2", "\"value\":3");
        AccessGrantSummary higherThreshold = service.grantSummary(null, null, null);
        assertThat(higherThreshold.positionRecommendations()).isEmpty();
        assertThat(higherThreshold.userAnomalies()).isEmpty();
    }

    // ── P1: applyPermissionTemplate 跨组织校验 ─────────────────────────────

    @Test
    void applyPermissionTemplateWithCrossOrgTargetUserThrowsForbidden() {
        PermissionTemplate template = service.createPermissionTemplate(new PermissionTemplateRequest(
                "create-cross-apply",
                "seed",
                List.of(),
                "Cross Apply Template",
                "temporary",
                null,
                List.of("ops.production_readiness.check"),
                "project",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-15T00:00:00Z"),
                false
        ));
        // Create user belonging to a different organization
        accessRepository.users.put("usr_cross_org", new AccessUser(
                "usr_cross_org", "cross-user", "staff", "org_other", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        // Switch to non-platform-admin actor in org_a
        authenticateAsOrgUser("usr_a", "user-a", "org_a");

        assertThatThrownBy(() -> service.applyPermissionTemplate(template.id(),
                new PermissionTemplateApplyRequest(
                        "apply-cross",
                        "apply",
                        List.of(),
                        template.id(),
                        List.of("usr_cross_org"),
                        "merge"
                ))).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.status().value()).isEqualTo(403);
            assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN");
        });
    }

    // ── P1: revokeTemporaryGrant 跨组织校验 ───────────────────────────────

    @Test
    void revokeTemporaryGrantOfOtherOrgUserThrowsForbidden() {
        // Create user in org_b
        accessRepository.users.put("usr_org_b", new AccessUser(
                "usr_org_b", "org-b-user", "staff", "org_b", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        // Platform admin creates a grant for org_b user
        TemporaryGrant grant = service.createTemporaryGrant(new TemporaryGrantRequest(
                "grant-org-b",
                "grant",
                List.of(),
                "usr_org_b",
                List.of("ops.production_readiness.check"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-15T00:00:00Z"),
                false,
                "test"
        ));
        // Switch to org_a user
        authenticateAsOrgUser("usr_org_a", "org-a-user", "org_a");

        assertThatThrownBy(() -> service.revokeTemporaryGrant(grant.id(),
                new StatusChangeRequest("revoke-cross", "revoke", "attack", List.of())
        )).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.status().value()).isEqualTo(403);
            assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN");
        });
    }

    // ── P1: updatePermissionTemplate 跨组织校验 ────────────────────────────

    @Test
    void updatePermissionTemplateOfOtherOrgThrowsForbidden() {
        // Directly put a template owned by org_b into the repository
        accessRepository.templates.put("tpl_org_b_tmpl", new PermissionTemplate(
                "tpl_org_b_tmpl", "tpl_org_b_tmpl", "Org B Template", null, "base",
                null, "organization", "org_b", "merge", null, null, false, "active",
                null, null, null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        accessRepository.items.put("tpl_org_b_tmpl", List.of());
        // Authenticate as org_a user
        authenticateAsOrgUser("usr_org_a", "org-a-user", "org_a");

        assertThatThrownBy(() -> service.updatePermissionTemplate("tpl_org_b_tmpl",
                new PermissionTemplateRequest(
                        "update-cross",
                        "update",
                        List.of(),
                        "Hacked Name",
                        "base",
                        null,
                        List.of(),
                        "project",
                        null,
                        null,
                        false
                ))).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.status().value()).isEqualTo(403);
            assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN");
        });
    }

    // ── P0-1: 发证不得超出授权人自身权限 + apply 模板归属校验 ──────────────

    @Test
    void createTemporaryGrantRejectsGrantingPermissionActorDoesNotHold() {
        // P0-1：非平台管理员不得授予超出自身有效权限的权限码（自我/横向提权防护）。
        // 目标用户同组织（cross-org 校验通过），但授权人本身无该权限 → GRANT_EXCEEDS_OWN_PERMISSIONS。
        accessRepository.users.put("usr_org_a_t", new AccessUser(
                "usr_org_a_t", "org-a-target", "staff", "org_a", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        authenticateAsOrgUser("usr_org_a_actor", "org-a-actor", "org_a");
        assertThatThrownBy(() -> service.createTemporaryGrant(new TemporaryGrantRequest(
                "grant-escalate", "grant", List.of(), "usr_org_a_t",
                List.of("ops.production_readiness.check"),
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-15T00:00:00Z"),
                false, "escalate"))).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.status().value()).isEqualTo(403);
            assertThat(e.code()).isEqualTo("GRANT_EXCEEDS_OWN_PERMISSIONS");
        });
    }

    @Test
    void applyPlatformOwnedTemplateByOrgUserToSameOrgUserIsForbidden() {
        // A1-05：apply 必须校验模板归属——非平台管理员不得套用平台/他组织模板（即便目标用户同组织），
        // 否则可借平台模板把高权限项下发给本组织用户提权。
        PermissionTemplate template = service.createPermissionTemplate(new PermissionTemplateRequest(
                "create-platform-tpl", "seed", List.of(), "Platform Template", "temporary",
                null, List.of("ops.production_readiness.check"), "project",
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-15T00:00:00Z"), false));
        accessRepository.users.put("usr_org_a_same", new AccessUser(
                "usr_org_a_same", "org-a-same", "staff", "org_a", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        authenticateAsOrgUser("usr_org_a_actor2", "org-a-actor2", "org_a");
        assertThatThrownBy(() -> service.applyPermissionTemplate(template.id(),
                new PermissionTemplateApplyRequest("apply-platform", "apply", List.of(),
                        template.id(), List.of("usr_org_a_same"), "merge")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.status().value()).isEqualTo(403);
            assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN");
        });
    }

    // ── M001（审计 20260924T114205Z）：套用的重复目标与真实授予人 ─────────────

    @Test
    void applyPermissionTemplateRejectsDuplicateTargetUserWithoutWriting() {
        // M001-F02：同一次套用请求中同一员工只能出现一次，重复即整单拒绝、不生成任何套用记录（2026-09-24 操作员裁决）。
        accessRepository.createUser(new AccessUser("usr_dup", "dup", "staff", "seed-org", null, "active", Instant.now()));
        accessRepository.createUser(new AccessUser("usr_other", "other", "staff", "seed-org", null, "active", Instant.now()));
        PermissionTemplate template = service.createPermissionTemplate(new PermissionTemplateRequest(
                "create-dup", "seed", List.of(), "Dup template", "temporary", null,
                List.of("ops.production_readiness.check"), "project",
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-15T00:00:00Z"), true));

        assertThatThrownBy(() -> service.applyPermissionTemplate(template.id(), new PermissionTemplateApplyRequest(
                "apply-dup", "apply", List.of(), template.id(), List.of("usr_dup", "usr_other", "usr_dup"), "merge")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(400);
                    assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                    assertThat(e.getMessage()).isEqualTo("同一次套用中员工重复：usr_dup");
                });
        assertThat(accessRepository.templateApplications).isEmpty();
    }

    @Test
    void applyPermissionTemplateRecordsEachActorAsAppliedByAndAuditsApplicationIds() {
        // M001-F04：每条套用记录的授予人是实际操作者；成功审计事件逐条带套用记录 id。
        accessRepository.templates.put("tpl_seed_apply", new PermissionTemplate(
                "tpl_seed_apply", "tpl_seed_apply", "Seed org template", null, "base",
                null, "organization", "seed-org", "merge", null, null, false, "active",
                null, null, null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        accessRepository.items.put("tpl_seed_apply", List.of());
        accessRepository.createUser(new AccessUser("usr_f4_a", "f4a", "staff", "seed-org", null, "active", Instant.now()));
        accessRepository.createUser(new AccessUser("usr_f4_b", "f4b", "staff", "seed-org", null, "active", Instant.now()));
        accessRepository.createUser(new AccessUser("usr_f4_c", "f4c", "staff", "seed-org", null, "active", Instant.now()));

        PermissionTemplateApplyResult byAdmin = service.applyPermissionTemplate("tpl_seed_apply", new PermissionTemplateApplyRequest(
                "apply-f4-admin", "apply", List.of(), "tpl_seed_apply", List.of("usr_f4_a", "usr_f4_b"), "merge"));
        authenticateAsOrgUser("usr_seed_actor", "seed-actor", "seed-org");
        PermissionTemplateApplyResult byOrgUser = service.applyPermissionTemplate("tpl_seed_apply", new PermissionTemplateApplyRequest(
                "apply-f4-org", "apply", List.of(), "tpl_seed_apply", List.of("usr_f4_c"), "merge"));

        assertThat(byAdmin.applications()).extracting(PermissionTemplateApplication::appliedBy)
                .containsExactly("usr_admin", "usr_admin");
        assertThat(byOrgUser.applications()).extracting(PermissionTemplateApplication::appliedBy).containsExactly("usr_seed_actor");
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "permission_template.applied".equals(event.eventName()))
                .extracting(event -> event.metadata().get("application_ids"))
                .containsExactly(
                        byAdmin.applications().stream().map(PermissionTemplateApplication::id).toList(),
                        byOrgUser.applications().stream().map(PermissionTemplateApplication::id).toList());
    }

    // M001-F05（PGM 权限规则「临时授权可设有效期或单次使用」「临时权限……有时间限制、到期自动失效」）：
    // 临时模板必须有到期时间或单次使用，到期时间必须晚于生效时间；非临时模板不受影响。
    @Test
    void temporaryTemplateNeedsExpiryOrSingleUseOnCreateAndUpdate() {
        assertThatThrownBy(() -> service.createPermissionTemplate(new PermissionTemplateRequest(
                "create-unbounded", "seed", List.of(), "Unbounded temporary", "temporary", null,
                List.of("ops.production_readiness.view"), "project", null, null, false)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> service.createPermissionTemplate(new PermissionTemplateRequest(
                "create-reversed", "seed", List.of(), "Reversed window", "temporary", null,
                List.of("ops.production_readiness.view"), "project",
                Instant.parse("2026-06-15T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z"), false)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        PermissionTemplate singleUseOnly = service.createPermissionTemplate(new PermissionTemplateRequest(
                "create-single", "seed", List.of(), "Single use temporary", "temporary", null,
                List.of("ops.production_readiness.view"), "project", null, null, true));
        PermissionTemplate base = service.createPermissionTemplate(new PermissionTemplateRequest(
                "create-base", "seed", List.of(), "Open base", "base", null,
                List.of("ops.production_readiness.view"), "project", null, null, false));

        assertThat(singleUseOnly.singleUse()).isTrue();
        assertThat(base.validUntil()).isNull();
        assertThatThrownBy(() -> service.createPermissionTemplate(new PermissionTemplateRequest(
                "create-zero-window", "seed", List.of(), "Zero window", "temporary", null,
                List.of("ops.production_readiness.view"), "project",
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z"), false)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> service.updatePermissionTemplate(base.id(), new PermissionTemplateRequest(
                "update-unbounded", "seed", List.of(), "Now temporary", "temporary", null,
                List.of("ops.production_readiness.view"), "project", null, null, false)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> service.updatePermissionTemplate(base.id(), new PermissionTemplateRequest(
                "update-reversed", "seed", List.of(), "Now temporary reversed", "temporary", null,
                List.of("ops.production_readiness.view"), "project",
                Instant.parse("2026-06-15T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z"), false)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> service.copyPermissionTemplate(base.id(), new PermissionTemplateRequest(
                "copy-unbounded", "seed", List.of(), "Copied unbounded", "temporary", base.id(),
                List.of("ops.production_readiness.view"), "project", null, null, false)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void updatingTemporaryTemplateWithReversedWindowIsRejectedBeforeReauthorizationRule() {
        // M001-F07：修改时倒置窗口（新截止即使晚于旧截止）先于「延长有效期须重新授权」判定，按 400 拒绝；清空到期仍按原规则 409。
        PermissionTemplate bounded = service.createPermissionTemplate(new PermissionTemplateRequest(
                "create-bounded", "seed", List.of(), "Bounded temporary", "temporary", null,
                List.of("ops.production_readiness.view"), "project",
                Instant.parse("2027-01-01T00:00:00Z"), Instant.parse("2027-01-02T00:00:00Z"), false));

        assertThatThrownBy(() -> service.updatePermissionTemplate(bounded.id(), new PermissionTemplateRequest(
                "update-reversed-later", "seed", List.of(), "Bounded temporary", "temporary", null,
                List.of("ops.production_readiness.view"), "project",
                Instant.parse("2027-01-04T00:00:00Z"), Instant.parse("2027-01-03T00:00:00Z"), false)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(400);
                    assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                });
        assertThatThrownBy(() -> service.updatePermissionTemplate(bounded.id(), new PermissionTemplateRequest(
                "update-zero-later", "seed", List.of(), "Bounded temporary", "temporary", null,
                List.of("ops.production_readiness.view"), "project",
                Instant.parse("2027-01-03T00:00:00Z"), Instant.parse("2027-01-03T00:00:00Z"), false)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(400);
                    assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                });
        assertThatThrownBy(() -> service.updatePermissionTemplate(bounded.id(), new PermissionTemplateRequest(
                "update-clear-expiry", "seed", List.of(), "Bounded temporary", "temporary", null,
                List.of("ops.production_readiness.view"), "project",
                Instant.parse("2027-01-01T00:00:00Z"), null, false)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(409);
                    assertThat(e.code()).isEqualTo("TEMPLATE_REAUTHORIZATION_REQUIRED");
                });
        PermissionTemplate singleUse = service.createPermissionTemplate(new PermissionTemplateRequest(
                "create-single-bounded", "seed", List.of(), "Single use temporary", "temporary", null,
                List.of("ops.production_readiness.view"), "project", null, null, true));
        assertThatThrownBy(() -> service.updatePermissionTemplate(singleUse.id(), new PermissionTemplateRequest(
                "update-drop-single", "seed", List.of(), "Single use temporary", "temporary", null,
                List.of("ops.production_readiness.view"), "project",
                null, Instant.parse("2027-01-02T00:00:00Z"), false)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(409);
                    assertThat(e.code()).isEqualTo("TEMPLATE_REAUTHORIZATION_REQUIRED");
                });
    }

    @Test
    void applyingUnboundedTemporaryTemplateIsRejectedUnlessThisApplicationIsSingleUse() {
        // M001-F05：库里已有的无期限临时模板（绕过入口或历史数据）套用时，本次非单次即拒绝且不写套用记录。
        accessRepository.templates.put("tpl_unbounded_tmp", new PermissionTemplate(
                "tpl_unbounded_tmp", "tpl_unbounded_tmp", "Unbounded temporary", null, "temporary",
                null, "organization", "seed-org", "merge", null, null, false, "active",
                null, null, null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        accessRepository.items.put("tpl_unbounded_tmp", List.of());
        accessRepository.createUser(new AccessUser("usr_f5", "f5", "staff", "seed-org", null, "active", Instant.now()));

        assertThatThrownBy(() -> service.applyPermissionTemplate("tpl_unbounded_tmp", new PermissionTemplateApplyRequest(
                "apply-unbounded", "apply", List.of(), "tpl_unbounded_tmp", List.of("usr_f5"), "merge")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(accessRepository.templateApplications).isEmpty();

        PermissionTemplateApplyResult once = service.applyPermissionTemplate("tpl_unbounded_tmp", new PermissionTemplateApplyRequest(
                "apply-once", "apply", List.of(), "tpl_unbounded_tmp", List.of("usr_f5"), "single_use"));
        assertThat(once.applications()).extracting(PermissionTemplateApplication::singleUse).containsExactly(true);

        accessRepository.templates.put("tpl_single_tmp", new PermissionTemplate(
                "tpl_single_tmp", "tpl_single_tmp", "Single use temporary", null, "temporary",
                null, "organization", "seed-org", "merge", null, null, true, "active",
                null, null, null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        accessRepository.items.put("tpl_single_tmp", List.of());
        PermissionTemplateApplyResult merged = service.applyPermissionTemplate("tpl_single_tmp", new PermissionTemplateApplyRequest(
                "apply-single-merge", "apply", List.of(), "tpl_single_tmp", List.of("usr_f5"), "merge"));
        assertThat(merged.applications()).extracting(PermissionTemplateApplication::singleUse).containsExactly(true);
    }

    @Test
    void positionAssignmentRejectsUnboundedTemporaryTemplateBeforeWriting() {
        // M001-F05：岗位定岗同样是套用；映射到无期限、非单次临时模板时写入前拒绝，岗位、职级与套用记录都不落。
        accessRepository.users.put("usr_position_tmp", new AccessUser(
                "usr_position_tmp", "position-tmp", "staff", "seed-org", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_ops_view", new RoleRecord(
                "role_ops_view", "ops_view", "Ops viewer", "project", "active",
                Instant.parse("2026-06-14T00:00:00Z"),
                List.of("ops.production_readiness.check"),
                null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_position_tmp", "tpl_position_tmp", "Position temporary", null, "temporary",
                null, "platform", null, "merge", null, null, false,
                "active", null, null, null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_position_tmp", "ops.production_readiness.check",
                        AccessRepository.Effect.GRANT, "project", false)));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "map-ops-viewer", null, "ops_viewer", "lvl_m1_lower", "role_ops_view", "tpl_position_tmp", "active"));

        assertThatThrownBy(() -> service.assignUserPosition(new UserPositionAssignmentRequest(
                "assign-position-tmp", null, "usr_position_tmp", "ops_viewer", "lvl_m1_lower", null, null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(accessRepository.findUserPositionAssignment("usr_position_tmp")).isEmpty();
        assertThat(accessRepository.templateApplications).isEmpty();
    }

    // M001-F09（操作员 2026-09-24 裁决，写入 PGM 权限规则）：项目自有模板只能套给本项目员工；平台管理员跨项目使用须先复制，
    // 复制件归平台、只有平台管理员可以修改。
    @Test
    void projectTemplateStaysInItsProjectAndCrossProjectUseGoesThroughAPlatformCopy() {
        accessRepository.templates.put("tpl_proj_b", new PermissionTemplate(
                "tpl_proj_b", "tpl_proj_b", "Project B template", null, "base",
                null, "organization", "org_b", "merge", null, null, false, "active",
                null, null, null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        accessRepository.items.put("tpl_proj_b", List.of());
        accessRepository.users.put("usr_proj_c", new AccessUser("usr_proj_c", "proj-c", "staff", "org_c", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));

        assertThatThrownBy(() -> service.applyPermissionTemplate("tpl_proj_b", new PermissionTemplateApplyRequest(
                "apply-proj-b-to-c", "apply", List.of(), "tpl_proj_b", List.of("usr_proj_c"), "merge")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(403);
                    assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN");
                });
        assertThat(accessRepository.templateApplications).isEmpty();

        PermissionTemplate copy = service.copyPermissionTemplate("tpl_proj_b", new PermissionTemplateRequest(
                "copy-proj-b", "seed", List.of(), "Platform copy", "base", "tpl_proj_b",
                List.of(), "project", null, null, false));
        assertThat(copy.ownerScopeId()).isNull();
        PermissionTemplateApplyResult viaCopy = service.applyPermissionTemplate(copy.id(), new PermissionTemplateApplyRequest(
                "apply-copy-to-c", "apply", List.of(), copy.id(), List.of("usr_proj_c"), "merge"));
        assertThat(viaCopy.applications()).extracting(PermissionTemplateApplication::templateId).containsExactly(copy.id());
        service.updatePermissionTemplate("tpl_proj_b", new PermissionTemplateRequest(
                "update-proj-b", "seed", List.of(), "Project B template", "base", null,
                List.of("ops.production_readiness.check"), "project", null, null, false));
        assertThat(accessRepository.findPermissionEffects("usr_proj_c", Instant.now()))
                .extracting(AccessRepository.PermissionEffect::permissionCode).doesNotContain("ops.production_readiness.check");

        authenticateAsOrgUser("usr_proj_b_editor", "proj-b-editor", "org_b");
        assertThatThrownBy(() -> service.updatePermissionTemplate(copy.id(), new PermissionTemplateRequest(
                "update-copy-by-b", "seed", List.of(), "Platform copy", "base", null,
                List.of(), "project", null, null, false)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN"));
    }

    // M001-F10：按员工归属写入授权的各入口与换项目入口，都在读员工归属前锁定目标员工行（真实锁与并发交错见 AccessJdbcIntegrationTests）。
    // 目标员工不存在：先锁后读时锁已记下再报 USER_NOT_FOUND；先读后锁则一条锁都记不到。
    @Test
    void grantWritesAndScopeChangeLockTheTargetRowsBeforeReadingThem() {
        assertThatThrownBy(() -> service.applyPermissionTemplate("tpl_any", new PermissionTemplateApplyRequest(
                "apply-lock", "apply", List.of(), "tpl_any", List.of("usr_ghost_b", "usr_ghost_a"), "merge")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("USER_NOT_FOUND"));
        assertThat(accessRepository.lockedUsers).containsExactly("usr_ghost_a", "usr_ghost_b");

        List<org.assertj.core.api.ThrowableAssert.ThrowingCallable> writes = List.of(
                () -> service.assignUserRole(new RoleAssignmentRequest("role-lock", "r", "usr_ghost", "role_any")),
                () -> service.unassignUserRole(new RoleAssignmentRequest("unassign-lock", "u", "usr_ghost", "role_any")), // M002-F10
                () -> service.assignUserPosition(new UserPositionAssignmentRequest(
                        "position-lock", "p", "usr_ghost", "ops_clerk", "lvl_m1_lower", null, null, null)),
                () -> service.createTemporaryGrant(new TemporaryGrantRequest("temporary-lock", "t", List.of(), "usr_ghost",
                        List.of("ops.production_readiness.check"), null, Instant.parse("2099-01-01T00:00:00Z"), false, "lock")),
                () -> service.setUserPermissionOverrides("usr_ghost", new UserPermissionOverrideRequest(
                        "override-lock", "o", List.of("ops.production_readiness.check"), "grant", "lock")),
                () -> service.changeUserScope("usr_ghost", new UserScopeChangeRequest(
                        "scope-lock", null, "org_b", null, "ops_clerk", "lvl_m1_lower", null, null, null, "move", null)));
        for (org.assertj.core.api.ThrowableAssert.ThrowingCallable write : writes) {
            accessRepository.lockedUsers.clear();
            assertThatThrownBy(write).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("USER_NOT_FOUND"));
            assertThat(accessRepository.lockedUsers).containsExactly("usr_ghost");
        }
    }

    // M001-F11（PGM 权限规则「权限的授予、收回……全部留痕」）：套用事件记当时的权限码；修改已套用模板时，
    // 每名仍生效的受影响员工各留一条增减记录。
    @Test
    void applySnapshotsCodesAndEditingAnAppliedTemplateIsAuditedPerAffectedEmployee() {
        accessRepository.createUser(new AccessUser("usr_f11_a", "f11a", "staff", "seed-org", null, "active", Instant.now()));
        accessRepository.createUser(new AccessUser("usr_f11_b", "f11b", "staff", "seed-org", null, "active", Instant.now()));
        PermissionTemplate template = service.createPermissionTemplate(new PermissionTemplateRequest(
                "create-f11", "seed", List.of(), "F11 template", "base", null,
                List.of("ops.production_readiness.check"), "project", null, null, false));
        PermissionTemplateApplyResult applied = service.applyPermissionTemplate(template.id(), new PermissionTemplateApplyRequest(
                "apply-f11", "apply", List.of(), template.id(), List.of("usr_f11_a", "usr_f11_b"), "merge"));
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "permission_template.applied".equals(event.eventName()))
                .extracting(event -> event.metadata().get("permission_codes"))
                .containsExactly(List.of("ops.production_readiness.check"));

        service.updatePermissionTemplate(template.id(), new PermissionTemplateRequest(
                "update-f11", "seed", List.of(), "F11 template", "base", null,
                List.of("governance.supervisor_review.view"), "project", null, null, false));

        List<AuditEvent> changed = auditEventRepository.events.stream()
                .filter(event -> "permission_template.effect_changed".equals(event.eventName())).toList();
        assertThat(changed).extracting(AuditEvent::businessId).containsExactlyInAnyOrder("usr_f11_a", "usr_f11_b");
        assertThat(changed).allSatisfy(event -> {
            assertThat(event.metadata()).containsEntry("template_id", template.id())
                    .containsEntry("added_permission_codes", List.of("governance.supervisor_review.view"))
                    .containsEntry("removed_permission_codes", List.of("ops.production_readiness.check"))
                    .containsEntry("operator_id", "usr_admin"); // M001-F14：UserRecord 主体下也记操作人
            assertThat(event.occurredAt()).isNotNull();
            assertThat(applied.applications()).extracting(PermissionTemplateApplication::id).contains((String) event.metadata().get("application_id"));
        });
    }

    // M001-F12/F15（PGM「发生跨项目变更时，旧岗位或旧范围派生权限立即失效」；F10「换项目提交后不存在该员工的旧项目模板授权」）：
    // 项目 A 的空模板套用、空角色不产出权限码，员工调到项目 B 时也按记录撤销；之后给它们加码不能让已调走的员工获得权限。
    @Test
    void scopeChangeRevokesEmptyTemplateApplicationsAndEmptyRoles() {
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        reviewRepository.superiors.put("usr_admin", "usr_boss");
        service = newAccessService(true, supervisorReviewService(reviewRepository));
        Instant at = Instant.parse("2026-06-14T00:00:00Z");
        accessRepository.users.put("usr_empty", new AccessUser("usr_empty", "empty", "staff", "seed-org", "merchant_a", "active", at));
        accessRepository.roles.put("role_ops_check", new RoleRecord(
                "role_ops_check", "ops_check", "Ops checker", "project", "active", at, List.of("ops.production_readiness.check"), null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_check", "tpl_ops_check", "Ops Check", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, at, at),
                List.of(new PermissionTemplateItem("tpl_ops_check", "ops.production_readiness.check", AccessRepository.Effect.GRANT, "project", false)));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "map-empty", null, "ops_clerk", "lvl_m1_lower", "role_ops_check", "tpl_ops_check", "active"));
        accessRepository.roles.put("role_empty", new RoleRecord("role_empty", "empty", "Empty", "project", "active", at, List.of(), null));
        accessRepository.userRoles.put("usr_empty", new java.util.LinkedHashSet<>(java.util.Set.of("role_empty")));
        PermissionTemplate emptyTemplate = new PermissionTemplate(
                "tpl_empty", "tpl_empty", "Empty", null, "base", null, "organization", "seed-org", "merge", null, null, false,
                "active", null, null, null, at, at);
        accessRepository.saveTemplate(emptyTemplate, List.of());
        accessRepository.applyTemplate("tpl_empty", List.of("usr_empty"), null, null, false, "usr_admin");

        service.changeUserScope("usr_empty", new UserScopeChangeRequest(
                "scope-empty", null, "org_b", "merchant_b", "ops_clerk", "lvl_m1_lower", null, null, null, "调往 B 项目", null));

        assertThat(accessRepository.users.get("usr_empty").organizationId()).isEqualTo("org_b");
        assertThat(accessRepository.userRoles.getOrDefault("usr_empty", Set.of())).isEmpty();
        assertThat(accessRepository.templateApplications).filteredOn(a -> "tpl_empty".equals(a.templateId()))
                .isNotEmpty().allMatch(a -> "revoked".equals(a.status()));
        accessRepository.saveTemplate(emptyTemplate, List.of(
                new PermissionTemplateItem("tpl_empty", "governance.supervisor_review.view", AccessRepository.Effect.GRANT, "project", false)));
        accessRepository.roles.put("role_empty", new RoleRecord(
                "role_empty", "empty", "Empty", "project", "active", at, List.of("ops.production_readiness.view"), null));
        assertThat(authorizationService.effectivePermissionsForUser("usr_empty").grants())
                .doesNotContain("governance.supervisor_review.view", "ops.production_readiness.view");
    }

    // M001-F13：修改与套用在读模板之前先取模板锁（真实锁与并发交错见 AccessJdbcIntegrationTests）；模板不存在时锁已记下再报不存在。
    @Test
    void templateEditAndApplyLockTheTemplateBeforeReadingIt() {
        assertThatThrownBy(() -> service.updatePermissionTemplate("tpl_ghost", new PermissionTemplateRequest(
                "edit-ghost", "seed", List.of(), "Ghost", "base", null, List.of(), "project", null, null, false)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("TEMPLATE_NOT_FOUND"));
        assertThat(accessRepository.lockOrder).containsExactly("edit:tpl_ghost");

        accessRepository.lockOrder.clear();
        accessRepository.createUser(new AccessUser("usr_tpl_lock", "tpl-lock", "staff", "seed-org", null, "active", Instant.now()));
        assertThatThrownBy(() -> service.applyPermissionTemplate("tpl_ghost", new PermissionTemplateApplyRequest(
                "apply-ghost", "apply", List.of(), "tpl_ghost", List.of("usr_tpl_lock"), "merge")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PERMISSION_TEMPLATE_NOT_FOUND"));
        assertThat(accessRepository.lockOrder).containsExactly("user:usr_tpl_lock", "apply:tpl_ghost");
    }

    // M001-F10（「按员工编号顺序锁定目标员工行」）：保存岗位映射先取映射表锁，再按编号锁在岗员工，最后才锁映射行——
    // 与定岗「员工行 → 映射行」同序。两人按编号倒序入库，重算原先按人员列表顺序逐个加锁。
    @Test
    void positionTemplateSaveLocksAssignedEmployeesByIdBeforeTheMapping() {
        Instant at = Instant.parse("2026-06-14T00:00:00Z");
        accessRepository.createUser(new AccessUser("usr_map_b", "map-first", "staff", "seed-org", null, "active", at));
        accessRepository.createUser(new AccessUser("usr_map_a", "map-second", "staff", "seed-org", null, "active", at));
        accessRepository.roles.put("role_map", new RoleRecord(
                "role_map", "map", "Map", "project", "active", at, List.of("ops.production_readiness.check"), null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_map", "tpl_map", "Map", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, at, at), List.of());
        PositionRoleTemplate mapping = service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "map-order", null, "ops_clerk", "lvl_m1_lower", "role_map", "tpl_map", "active"));
        for (String userId : List.of("usr_map_b", "usr_map_a")) {
            service.assignUserPosition(new UserPositionAssignmentRequest(
                    "assign-" + userId, null, userId, "ops_clerk", "lvl_m1_lower", null, null, null));
        }

        accessRepository.lockOrder.clear();
        service.updatePositionRoleTemplate(mapping.id(), new PositionRoleTemplateRequest(
                "map-order-save", null, "ops_clerk", "lvl_m1_lower", "role_map", "tpl_map", "active"));

        assertThat(accessRepository.lockOrder).startsWith("prt-table", "user:usr_map_a", "user:usr_map_b", "mapping:" + mapping.id());
    }

    // ── P0: listAccessUsers 只返回本组织用户 ──────────────────────────────

    @Test
    void listAccessUsersFiltersToCurrentOrganization() {
        accessRepository.users.put("usr_org_a_1", new AccessUser("usr_org_a_1", "a1", "staff", "org_a", null, "active", Instant.now()));
        accessRepository.users.put("usr_org_b_1", new AccessUser("usr_org_b_1", "b1", "staff", "org_b", null, "active", Instant.now()));
        authenticateAsOrgUser("usr_req_a", "req-a", "org_a");

        var page = service.listAccessUsers();

        assertThat(page.items()).extracting(AccessUser::id).containsExactly("usr_org_a_1");
        assertThat(page.items()).extracting(AccessUser::id).doesNotContain("usr_org_b_1");
    }

    @Test
    void listAccessUsersPlatformAdminSeesAllOrganizations() {
        accessRepository.users.put("usr_org_a_2", new AccessUser("usr_org_a_2", "a2", "staff", "org_a", null, "active", Instant.now()));
        accessRepository.users.put("usr_org_b_2", new AccessUser("usr_org_b_2", "b2", "staff", "org_b", null, "active", Instant.now()));
        // Already authenticated as platform_admin in @BeforeEach

        var page = service.listAccessUsers();

        assertThat(page.items()).extracting(AccessUser::id).contains("usr_org_a_2", "usr_org_b_2");
    }

    @Test
    void listAccessUsersFiltersToCurrentMerchantWhenMerchantScoped() {
        accessRepository.users.put("usr_merchant_a_1", new AccessUser("usr_merchant_a_1", "ma1", "staff", "org_a", "merchant_a", "active", Instant.now()));
        accessRepository.users.put("usr_merchant_b_1", new AccessUser("usr_merchant_b_1", "mb1", "staff", "org_a", "merchant_b", "active", Instant.now()));
        authenticateAsMerchantUser("usr_req_merchant_a", "merchant-a-admin", "org_a", "merchant_a");

        var page = service.listAccessUsers();

        assertThat(page.items()).extracting(AccessUser::id).containsExactly("usr_merchant_a_1");
        assertThat(page.items()).extracting(AccessUser::id).doesNotContain("usr_merchant_b_1");
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private AccessGrantRequestRecord temporaryGrantRequest(
            String id,
            String userId,
            String positionCode,
            String staffLevelId,
            String permissionCode,
            Instant createdAt
    ) {
        return new AccessGrantRequestRecord(
                id,
                "temporary_permission",
                userId,
                null,
                null,
                positionCode,
                staffLevelId,
                null,
                null,
                null,
                List.of(permissionCode),
                createdAt,
                createdAt.plusSeconds(3600),
                false,
                "applied",
                "review_" + id,
                "summary",
                "usr_requester",
                "usr_admin",
                createdAt,
                null,
                null,
                createdAt,
                createdAt
        );
    }

    // M006-G-01（PGM-1.10.1「员工单独授予 − 显式拒绝」「员工级完全自定义」）：员工级显式拒绝有写路径且立即扣减有效集；
    // 移除后恢复；高危单独授予不得绕过审批。
    @Test
    void userPermissionOverrideDenyLandsRemovesAndHighRiskGrantNeedsApproval() {
        accessRepository.markRequiresApproval("ops.production_readiness.check", "high");
        service = newAccessService(true, supervisorReviewService(new InMemorySupervisorReviewRepository()));
        accessRepository.createUser(new AccessUser("usr_override", "override", "staff", "seed-org", null, "active", Instant.now()));
        accessRepository.roles.put("role_view", new RoleRecord("role_view", "查看", null, "project", "active", Instant.now(),
                List.of("ops.production_readiness.view"), null));
        accessRepository.assignUserRole("usr_override", "role_view");

        EffectivePermissions denied = service.setUserPermissionOverrides("usr_override", new UserPermissionOverrideRequest(
                "override-deny", "停权", List.of("ops.production_readiness.view"), "deny", "临时停权"));
        assertThat(denied.denies()).contains("ops.production_readiness.view");
        assertThat(denied.grants()).doesNotContain("ops.production_readiness.view");
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).contains("access.user_permission_override.set");

        EffectivePermissions restored = service.removeUserPermissionOverrides("usr_override", new UserPermissionOverrideRequest(
                "override-remove", "恢复", List.of("ops.production_readiness.view"), "deny", "恢复"));
        assertThat(restored.denies()).doesNotContain("ops.production_readiness.view");
        assertThat(restored.grants()).contains("ops.production_readiness.view");

        assertThatThrownBy(() -> service.setUserPermissionOverrides("usr_override", new UserPermissionOverrideRequest(
                "override-grant-high", null, List.of("ops.production_readiness.check"), "grant", "越权")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_REQUIRES_APPROVAL"));
    }

    // M007-I3-F02（PGM-1.7.1 涉钱审批出厂默认 1 个工作日）：含资金/分红/退款/调账类权限的授权申请以 access.grant_request.money 进复核链。
    @Test
    void moneyPermissionApplicationsAreMarkedForTheShorterDeadline() {
        accessRepository.markRequiresApproval("finance.dividend.manage", "high");
        accessRepository.markRequiresApproval("ops.production_readiness.check", "high");
        accessRepository.markRequiresApproval("inventory.stock.adjust", "high");
        accessRepository.markRequiresApproval("bar.bill.adjust", "high");
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        service = newAccessService(true, supervisorReviewService(reviewRepository));
        accessRepository.users.put("usr_money_target", new AccessUser(
                "usr_money_target", "money-target", "staff", "org_a", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        authenticateAsOrgUser("usr_requester", "requester", "org_a");

        AccessGrantRequestRecord money = service.applyGrant(new AccessGrantApplyRequest(
                "apply-temp-money", "temporary", List.of(), "temporary_permission", "usr_money_target", null, null, null, null, null, null,
                List.of("finance.dividend.manage"), Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-15T00:00:00Z"), false, "payout"));
        AccessGrantRequestRecord normal = service.applyGrant(new AccessGrantApplyRequest(
                "apply-temp-normal", "temporary", List.of(), "temporary_permission", "usr_money_target", null, null, null, null, null, null,
                List.of("ops.production_readiness.check"), Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-15T00:00:00Z"), false, "check"));
        assertThat(reviewRepository.reviews.get(money.reviewId()).actionType()).isEqualTo("access.grant_request.money");
        assertThat(reviewRepository.reviews.get(normal.reviewId()).actionType()).isEqualTo("access.grant_request");
        // M007-I4-A01：库存调整不是调账（PGM-1.10.1 调账指账单/往来调账），账单调账仍涉钱。
        AccessGrantRequestRecord stock = service.applyGrant(new AccessGrantApplyRequest(
                "apply-temp-stock", "temporary", List.of(), "temporary_permission", "usr_money_target", null, null, null, null, null, null,
                List.of("inventory.stock.adjust"), Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-15T00:00:00Z"), false, "count"));
        AccessGrantRequestRecord bill = service.applyGrant(new AccessGrantApplyRequest(
                "apply-temp-bill", "temporary", List.of(), "temporary_permission", "usr_money_target", null, null, null, null, null, null,
                List.of("bar.bill.adjust"), Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-15T00:00:00Z"), false, "adjust"));
        assertThat(reviewRepository.reviews.get(stock.reviewId()).actionType()).isEqualTo("access.grant_request");
        assertThat(reviewRepository.reviews.get(bill.reviewId()).actionType()).isEqualTo("access.grant_request.money");
    }

    // M007-F-04（PGM-1.10.1「员工可申请临时权限，按层级对应主管审批……到期自动失效」）：日常权限的临时申请也走主管链，
    // 不再以 GRANT_APPROVAL_NOT_REQUIRED 拒绝；审批通过后落为有时限的临时授权。
    @Test
    void dailyTemporaryPermissionApplicationEntersSupervisorChain() {
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        service = newAccessService(true, supervisorReviewService(reviewRepository));
        accessRepository.users.put("usr_daily_target", new AccessUser(
                "usr_daily_target", "daily-target", "staff", "org_a", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.saveStaffLevelAssignment(new StaffLevelAssignment(
                "usr_admin", "lvl_m10_upper", null, "CNY", null, "seed-org", null, "test", Instant.parse("2026-06-14T00:00:00Z")));
        authenticateAsOrgUser("usr_requester", "requester", "org_a");

        AccessGrantRequestRecord pending = service.applyGrant(new AccessGrantApplyRequest(
                "apply-temp-daily", "temporary", List.of(), "temporary_permission",
                "usr_daily_target", null, null, null, null, null, null,
                List.of("ops.production_readiness.view"),
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-15T00:00:00Z"), false, "cover shift"));
        assertThat(pending.status()).isEqualTo("pending");
        assertThat(reviewRepository.reviews.get(pending.reviewId()).policyCode()).isEqualTo("permission_grant_l1");

        authenticateAsPlatformAdmin("seed-org");
        AccessGrantRequestRecord applied = service.reviewGrant(pending.id(), new AccessGrantReviewRequest(
                "review-temp-daily", "approve", List.of(), "approve", "ok"));
        assertThat(applied.status()).isEqualTo("applied");
        assertThat(accessRepository.temporaryGrants.values()).hasSize(1);
    }

    private AccessService newAccessService(boolean grantApprovalEnabled, SupervisorReviewService supervisorReviewService) {
        return new AccessService(
                accessRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                authorizationService,
                credentialProvisioner,
                supervisorReviewService,
                grantApprovalEnabled,
                3,
                3
        );
    }

    private SupervisorReviewService supervisorReviewService(InMemorySupervisorReviewRepository reviewRepository) {
        return new SupervisorReviewService(
                authorizationService,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                reviewRepository,
                new AuditEventService(auditEventRepository), event -> { }
        );
    }

    @Test
    void listGrantRequestsFiltersRowsStampedWithOtherProjects() {
        // 2026-07-18 收尾（多综合体前置）：授权申请行带 project_id 时，项目员工只见本项目行（currentProjectScopeQuietly
        // 谓词范式）；行无 project_id 时沿用目标账号同组织判定；平台管理员全量。
        Instant now = Instant.now();
        accessRepository.users.put("usr_scope_target", new AccessUser(
                "usr_scope_target", "scope-target", "staff", "org_a", null, "active", now));
        accessRepository.saveGrantRequest(grantRequestRow("agr_scope_a", "usr_scope_target", "org_a", now));
        accessRepository.saveGrantRequest(grantRequestRow("agr_scope_b", "usr_scope_target", "org_b", now));
        accessRepository.saveGrantRequest(grantRequestRow("agr_scope_none", "usr_scope_target", null, now));

        authenticateAsOrgUser("usr_scope_viewer", "scope-viewer", "org_a");
        assertThat(service.listGrantRequests(null).items()).extracting(AccessGrantRequestRecord::id)
                .contains("agr_scope_a", "agr_scope_none").doesNotContain("agr_scope_b");

        authenticateAsPlatformAdmin("seed-org");
        assertThat(service.listGrantRequests(null).items()).extracting(AccessGrantRequestRecord::id)
                .contains("agr_scope_a", "agr_scope_b", "agr_scope_none");
    }

    private AccessGrantRequestRecord grantRequestRow(String id, String targetUserId, String projectId, Instant now) {
        return new AccessGrantRequestRecord(id, "temporary_permission", targetUserId, null, null, null, null,
                null, null, null, List.of("ops.production_readiness.view"), null, now.plusSeconds(3600), false,
                "pending", null, "scope test", "usr_requester", null, null, projectId, null, now, now);
    }

    // M031-001（BSM-003；业务地图原文「想恢复点"取消存档"即可」）：存档后恢复即回到可定岗的启用态。
    @Test
    void restoringAnArchivedPositionMappingMakesItAssignableAgain() {
        authenticateAsPlatformAdmin("seed-org");
        PositionRoleTemplate mapping = seedRestoreFixture("usr_restore", "ops_restore", "map-restore");
        service.changePositionTemplateLifecycle(mapping.id(), new StatusChangeRequest("restore-archive", "archive", "停止使用", List.of()));
        assertThatThrownBy(() -> service.assignUserPosition(new UserPositionAssignmentRequest(
                "assign-while-archived", null, "usr_restore", "ops_restore", "lvl_m1_lower", null, null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("POSITION_ROLE_TEMPLATE_NOT_FOUND"));

        PositionRoleTemplate restored = service.changePositionTemplateLifecycle(mapping.id(),
                new StatusChangeRequest("restore-restore", "restore", "恢复使用", List.of()));

        assertThat(restored.status()).isEqualTo("active");
        PositionRoleTemplate stored = accessRepository.findPositionRoleTemplateById(mapping.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo("active");
        assertThat(stored.archivedAt()).isNull();
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "assign-after-restore", null, "usr_restore", "ops_restore", "lvl_m1_lower", null, null, null));
        assertThat(accessRepository.positionRoles.get("usr_restore")).isEqualTo("role_restore");
    }

    // M031-001：同岗位同职级已有启用中的替代映射时，恢复被拒且原映射状态不变。
    @Test
    void restoringIsRejectedWhileAnActiveReplacementMappingExists() {
        authenticateAsPlatformAdmin("seed-org");
        PositionRoleTemplate mapping = seedRestoreFixture("usr_restore_blocked", "ops_restore_blocked", "map-restore-blocked");
        service.changePositionTemplateLifecycle(mapping.id(), new StatusChangeRequest("blocked-archive", "archive", "停止使用", List.of()));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "map-restore-replacement", null, "ops_restore_blocked", "lvl_m1_lower", "role_restore", "tpl_restore", "active"));

        assertThatThrownBy(() -> service.changePositionTemplateLifecycle(mapping.id(),
                new StatusChangeRequest("blocked-restore", "restore", "恢复使用", List.of())))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(409);
                    assertThat(e.code()).isEqualTo("POSITION_TEMPLATE_ACTIVE_EXISTS");
                });

        PositionRoleTemplate unchanged = accessRepository.findPositionRoleTemplateById(mapping.id()).orElseThrow();
        assertThat(unchanged.status()).isEqualTo("inactive");
        assertThat(unchanged.archivedAt()).isNotNull();
    }

    private PositionRoleTemplate seedRestoreFixture(String userId, String positionCode, String idempotencyKey) {
        accessRepository.users.put(userId, new AccessUser(
                userId, userId, "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_restore", new RoleRecord(
                "role_restore", "role_restore", "Ops checker", "project", "active",
                Instant.parse("2026-06-14T00:00:00Z"), List.of("ops.production_readiness.check"), null));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_restore", "tpl_restore", "Ops Check", null, "base",
                null, "platform", null, "merge", null, null, false,
                "active", null, null, null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem(
                        "tpl_restore", "ops.production_readiness.check", AccessRepository.Effect.GRANT, "project", false)));
        return service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                idempotencyKey, null, positionCode, "lvl_m1_lower", "role_restore", "tpl_restore", "active"));
    }

    // M031-002（治理地图「系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效」）：
    // 保存岗位映射后在岗人员按新映射重算、薪资不变；映射停用则收回派生。
    @Test
    void savingAPositionMappingRecomputesAssignedUsersAndDeactivatingRevokesDerivedGrants() {
        authenticateAsPlatformAdmin("seed-org");
        accessRepository.users.put("usr_recompute", new AccessUser(
                "usr_recompute", "recompute", "staff", "seed-org", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        // M036-001：旧角色、新角色、模板各带一个互不相同的码，有效权限集才分得清派生来自哪里。
        for (Map.Entry<String, String> role : Map.of(
                "role_recompute_old", "ops.production_readiness.view",
                "role_recompute_new", "governance.supervisor_review.view").entrySet()) {
            accessRepository.roles.put(role.getKey(), new RoleRecord(
                    role.getKey(), role.getKey(), "Ops checker", "project", "active",
                    Instant.parse("2026-06-14T00:00:00Z"),
                    List.of(role.getValue()),
                    null));
        }
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_recompute", "tpl_recompute", "Ops Check", null, "base",
                null, "platform", null, "merge", null, null, false,
                "active", null, null, null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem(
                        "tpl_recompute",
                        "ops.production_readiness.check",
                        AccessRepository.Effect.GRANT,
                        "project",
                        false)));
        PositionRoleTemplate mapping = service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "map-recompute", null, "ops_recompute", "lvl_m1_lower",
                "role_recompute_old", "tpl_recompute", "active"));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "assign-recompute", null, "usr_recompute", "ops_recompute", "lvl_m1_lower",
                500000L, "CNY", null));
        assertThat(accessRepository.positionRoles.get("usr_recompute")).isEqualTo("role_recompute_old");
        assertThat(authorizationService.effectivePermissionsForUser("usr_recompute").grants())
                .contains("ops.production_readiness.view", "ops.production_readiness.check");

        service.updatePositionRoleTemplate(mapping.id(), new PositionRoleTemplateRequest(
                "map-recompute-change", null, "ops_recompute", "lvl_m1_lower",
                "role_recompute_new", "tpl_recompute", "active"));

        assertThat(accessRepository.positionRoles.get("usr_recompute")).isEqualTo("role_recompute_new");
        assertThat(authorizationService.effectivePermissionsForUser("usr_recompute").grants())
                .contains("governance.supervisor_review.view", "ops.production_readiness.check")
                .doesNotContain("ops.production_readiness.view");
        assertThat(accessRepository.templateApplications).filteredOn(a -> "usr_recompute".equals(a.targetId()))
                .extracting(PermissionTemplateApplication::status).containsExactly("revoked", "active");
        assertThat(accessRepository.findStaffLevelAssignment("usr_recompute")).get()
                .extracting(StaffLevelAssignment::baseSalaryAmountCent)
                .isEqualTo(500000L);

        service.updatePositionRoleTemplate(mapping.id(), new PositionRoleTemplateRequest(
                "map-recompute-off", null, "ops_recompute", "lvl_m1_lower",
                "role_recompute_new", "tpl_recompute", "inactive"));

        assertThat(accessRepository.positionRoles).containsKey("usr_recompute");
        assertThat(accessRepository.positionRoles.get("usr_recompute")).isNull();
        assertThat(authorizationService.effectivePermissionsForUser("usr_recompute").grants())
                .doesNotContain("ops.production_readiness.view", "governance.supervisor_review.view",
                        "ops.production_readiness.check");
        assertThat(accessRepository.templateApplications).filteredOn(a -> "usr_recompute".equals(a.targetId()))
                .extracting(PermissionTemplateApplication::status).containsOnly("revoked");
    }

    // M031-003（GEN-011）：岗位模板生命周期与授权政策两个写口失败也留痕。
    @Test
    void positionTemplateLifecycleAndGrantPolicyFailuresAreAudited() {
        authenticateAsPlatformAdmin("seed-org");
        assertThatThrownBy(() -> service.changePositionTemplateLifecycle("prt_missing",
                new StatusChangeRequest("lifecycle-missing", "archive", "停止使用", List.of())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("POSITION_ROLE_TEMPLATE_NOT_FOUND"));
        assertThatThrownBy(() -> service.updateGrantPolicy(null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "failure".equals(event.eventResult()))
                .extracting(AuditEvent::eventName)
                .contains("access.position_role_template.lifecycle_failed", "access.grant_policy.change_failed");
    }

    private void authenticateAsPlatformAdmin(String organizationId) {
        AuthService.UserRecord user = new AuthService.UserRecord(
                "usr_admin", "seed-admin", "hash", "platform_admin", organizationId, null, "active",
                Instant.parse("2026-06-14T00:00:00Z")
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, "token", List.of(new SimpleGrantedAuthority("ROLE_P0_SMOKE"))
        ));
    }

    // M002：USB 最高权限会话（与 ProductionAuthService 签发的主体同一组身份字段）。
    private void authenticateAsMasterAuthority() {
        AuthService.UserRecord user = new AuthService.UserRecord(
                SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, "hash", SessionIssuer.MASTER_ACCOUNT_TYPE,
                "platform-company", null, "active", Instant.parse("2026-06-14T00:00:00Z")
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, "token", List.of(new SimpleGrantedAuthority("ROLE_P0_SMOKE"))
        ));
    }

    private void authenticateAsOrgUser(String id, String username, String organizationId) {
        AuthService.UserRecord user = new AuthService.UserRecord(
                id, username, "hash", "staff", organizationId, null, "active",
                Instant.parse("2026-06-14T00:00:00Z")
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, "token", List.of(new SimpleGrantedAuthority("ROLE_P0_SMOKE"))
        ));
    }

    private void authenticateAsMerchantUser(String id, String username, String organizationId, String merchantId) {
        AuthService.UserRecord user = new AuthService.UserRecord(
                id, username, "hash", "merchant", organizationId, merchantId, "active",
                Instant.parse("2026-06-14T00:00:00Z")
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, "token", List.of(new SimpleGrantedAuthority("ROLE_P0_SMOKE"))
        ));
    }

    private static final class RecordingCredentialProvisioner implements AccessCredentialProvisioner {
        final List<CredentialProvisionRequest> requests = new ArrayList<>();

        @Override
        public void provisionCredential(String userId, String password, String status, Instant now) {
            requests.add(new CredentialProvisionRequest(userId, password, status, now));
        }
    }

    private record CredentialProvisionRequest(String userId, String password, String status, Instant now) {
    }

    private static final class InMemoryAccessRepository implements AccessRepository {
        final Map<String, PermissionDefinition> permissions = new LinkedHashMap<>();
        String policy = "{\"windowDays\":{\"value\":30,\"min\":1,\"max\":2147483647},\"positionThreshold\":{\"value\":3,\"min\":1,\"max\":2147483647},\"userThreshold\":{\"value\":3,\"min\":1,\"max\":2147483647},\"riskReviews\":{\"low\":{\"approvalLevel\":\"one_level_up\",\"requiredLevel\":1},\"medium\":{\"approvalLevel\":\"one_level_up\",\"requiredLevel\":1},\"high\":{\"approvalLevel\":\"two_levels_up\",\"requiredLevel\":2},\"critical\":{\"approvalLevel\":\"top_authority\",\"requiredLevel\":3}},\"deadlineWorkingDays\":{\"value\":3,\"min\":1,\"max\":30}}";
        final Map<String, String> fingerprints = new LinkedHashMap<>();
        final Map<String, String> positionRoles = new LinkedHashMap<>();
        @Override public void lockGrantAuthorizationFacts() {} // 单线程内存夹具；真实锁在 M006JdbcRegressionTests 验证。
        @Override public Optional<String> findGrantPolicy() { return Optional.of(policy); }
        @Override public void saveGrantFingerprint(String id, String fingerprint) {
            if (fingerprints.putIfAbsent(id, fingerprint) != null) throw new IllegalStateException("fingerprint already set");
        }
        @Override public Optional<String> findGrantFingerprint(String id) { return Optional.ofNullable(fingerprints.get(id)); }
        final Map<String, List<String>> positionApplicationIds = new LinkedHashMap<>();
        final Map<String, String> positionMappings = new LinkedHashMap<>(); // M002-F22：员工实际挂的岗位映射（Jdbc assigned_position_template_id）
        // M036-001：与 JdbcAccessRepository 同语义——先撤上一次岗位派生的模板应用，再记新角色与新应用 id；独立模板应用不受影响。
        @Override public void replacePositionGrants(String userId, String roleId, String mappingId,
                List<String> applicationIds, String actorId, Instant at) {
            revokeTemplateApplications(userId, positionApplicationIds.getOrDefault(userId, List.of()), actorId, at);
            positionRoles.put(userId, roleId);
            positionMappings.put(userId, mappingId);
            positionApplicationIds.put(userId, List.copyOf(applicationIds));
        }
        @Override public Optional<String> findAssignedPositionTemplateId(String userId) {
            return Optional.ofNullable(positionMappings.get(userId));
        }
        // M002-F22：与 JdbcAccessRepository 同语义——只取实际挂在这张映射上的员工。
        @Override public List<String> listUserIdsOnPositionTemplate(PositionRoleTemplate mapping) {
            return positionMappings.entrySet().stream().filter(entry -> mapping.id().equals(entry.getValue())).map(Map.Entry::getKey).toList();
        }
        final Map<String, PermissionTemplate> templates = new LinkedHashMap<>();
        final Map<String, List<PermissionTemplateItem>> items = new LinkedHashMap<>();
        final Map<String, TemporaryGrant> temporaryGrants = new LinkedHashMap<>();
        final Map<String, AccessGrantRequestRecord> grantRequests = new LinkedHashMap<>();
        final Map<String, AccessUser> users = new LinkedHashMap<>();
        final Map<String, RoleRecord> roles = new LinkedHashMap<>();
        final Map<String, Set<String>> userRoles = new LinkedHashMap<>();
        final Map<String, PositionRoleTemplate> positionRoleTemplates = new LinkedHashMap<>();
        final Map<String, UserPositionAssignment> userPositionAssignments = new LinkedHashMap<>();
        final List<PermissionTemplateApplication> templateApplications = new ArrayList<>();
        final Map<String, StaffLevelRecord> staffLevels = new LinkedHashMap<>(Map.of(
                "lvl_m1_lower", new StaffLevelRecord("lvl_m1_lower", "M1", "lower", 1, "M1 薪资大范围", "active", Instant.parse("2026-06-14T00:00:00Z")),
                "lvl_m10_upper", new StaffLevelRecord("lvl_m10_upper", "M10", "upper", 30, "M10 薪资大范围", "active", Instant.parse("2026-06-14T00:00:00Z"))
        ));
        final Map<String, StaffLevelAssignment> staffLevelAssignments = new LinkedHashMap<>();
        final List<PermissionEffect> overrides = new ArrayList<>();
        // M002-F06：覆盖行的原因 / 写入人 / 写入时间（按 sourceId）；每写一次时间递增，对应 JDBC upsert 的 now()。
        final Map<String, ExplicitDenyRow> overrideRows = new LinkedHashMap<>();
        long overrideWrites;
        Runnable beforeNextUserLock; // M002-F06：下一次锁员工行前执行一次（模拟锁前的并发写）

        // M002-F02：与 JdbcAccessRepository 同语义——员工显式行（USER_OVERRIDE）与岗位高危挂起行（POSITION_HOLD）按来源分行，
        // upsert 只覆盖同来源的行；deleteUserPermissionOverrides 删该码该 effect 的全部来源（服务层直接移除拒绝自 M002-F05 起只删显式行）。
        @Override
        public void saveUserPermissionOverrides(String userId, List<String> permissionCodes, Effect effect, String reason, String createdBy) {
            upsertOverrides(userId, permissionCodes, effect, Source.USER_OVERRIDE, reason, createdBy);
        }

        @Override
        public void saveHighRiskHolds(String userId, List<String> permissionCodes, String reason, String createdBy) {
            upsertOverrides(userId, permissionCodes, Effect.DENY, Source.POSITION_HOLD, reason, createdBy);
        }

        @Override
        public List<ExplicitDenyRow> findExplicitDenies(String userId, List<String> permissionCodes) {
            return overrides.stream()
                    .filter(o -> o.userId().equals(userId) && o.effect() == Effect.DENY && o.source() == Source.USER_OVERRIDE
                            && permissionCodes.contains(o.permissionCode()))
                    .map(o -> overrideRows.get(o.sourceId()))
                    .sorted(java.util.Comparator.comparing(ExplicitDenyRow::permissionCode))
                    .toList();
        }

        @Override
        public int deleteUserPermissionOverrides(String userId, List<String> permissionCodes, Effect effect) {
            int before = overrides.size();
            overrides.removeIf(o -> o.userId().equals(userId) && o.effect() == effect && permissionCodes.contains(o.permissionCode()));
            return before - overrides.size();
        }

        @Override
        public int deleteHighRiskHolds(String userId, List<String> permissionCodes) {
            return removeOverrides(userId, permissionCodes, Effect.DENY, Source.POSITION_HOLD);
        }

        @Override
        public int deleteExplicitDenies(String userId, List<String> permissionCodes) {
            return removeOverrides(userId, permissionCodes, Effect.DENY, Source.USER_OVERRIDE);
        }

        private void upsertOverrides(String userId, List<String> permissionCodes, Effect effect, Source source,
                                     String reason, String createdBy) {
            removeOverrides(userId, permissionCodes, effect, source);
            for (String code : permissionCodes) {
                String sourceId = userId + ":" + code + ":" + effect + ":" + source;
                overrides.add(new PermissionEffect(userId, code, effect, source, sourceId, null, null, false, false));
                overrideRows.put(sourceId, new ExplicitDenyRow(code, reason, createdBy, Instant.ofEpochSecond(++overrideWrites)));
            }
        }

        private int removeOverrides(String userId, List<String> permissionCodes, Effect effect, Source source) {
            int before = overrides.size();
            overrides.removeIf(o -> o.userId().equals(userId) && o.effect() == effect && o.source() == source
                    && permissionCodes.contains(o.permissionCode()));
            return before - overrides.size();
        }

        @Override
        public List<StaffLevelRecord> listStaffLevels() {
            return new ArrayList<>(staffLevels.values());
        }

        @Override
        public Optional<StaffLevelRecord> findStaffLevelById(String staffLevelId) {
            return Optional.ofNullable(staffLevels.get(staffLevelId));
        }

        @Override
        public StaffLevelAssignment saveStaffLevelAssignment(StaffLevelAssignment assignment) {
            staffLevelAssignments.put(assignment.userId(), assignment);
            return assignment;
        }

        @Override
        public Optional<StaffLevelAssignment> findStaffLevelAssignment(String userId) {
            return Optional.ofNullable(staffLevelAssignments.get(userId));
        }

        @Override
        public boolean permissionExists(String permissionCode) {
            return permissions.containsKey(permissionCode);
        }

        @Override
        public List<PermissionDefinition> listPermissions() {
            return new ArrayList<>(permissions.values());
        }

        void addPermission(String permissionCode, String riskLevel, boolean requiresApproval) {
            permissions.put(permissionCode, new PermissionDefinition(
                    permissionCode,
                    permissionCode,
                    "ops",
                    riskLevel,
                    requiresApproval,
                    "active"
            ));
        }

        void markRequiresApproval(String permissionCode, String riskLevel) {
            addPermission(permissionCode, riskLevel, true);
        }

        @Override
        public List<PermissionTemplate> listTemplates() {
            return new ArrayList<>(templates.values());
        }

        @Override
        public Optional<PermissionTemplate> findTemplateById(String id) {
            return Optional.ofNullable(templates.get(id));
        }

        @Override
        public PermissionTemplate saveTemplate(PermissionTemplate template, List<PermissionTemplateItem> templateItems) {
            templates.put(template.id(), template);
            items.put(template.id(), new ArrayList<>(templateItems));
            return template;
        }

        final List<String> lockedUsers = new ArrayList<>();
        final List<String> lockOrder = new ArrayList<>(); // M001-F10/F13：员工、模板、映射各类锁的先后

        @Override
        public void lockAccessUsers(List<String> userIds) {
            Runnable hook = beforeNextUserLock;
            beforeNextUserLock = null;
            if (hook != null) {
                hook.run();
            }
            lockedUsers.addAll(userIds);
            userIds.forEach(userId -> lockOrder.add("user:" + userId));
        }

        @Override
        public void lockTemplateForEdit(String templateId) {
            lockOrder.add("edit:" + templateId);
        }

        @Override
        public void lockTemplateForApply(String templateId) {
            lockOrder.add("apply:" + templateId);
        }

        @Override
        public void lockPositionRoleTemplatesForWrite() {
            lockOrder.add("prt-table");
        }

        @Override
        public List<String> findUserRoleIds(String userId) {
            return List.copyOf(userRoles.getOrDefault(userId, Set.of()));
        }

        @Override
        public List<String> findActiveTemplateApplicationIds(String userId) {
            return templateApplications.stream()
                    .filter(application -> userId.equals(application.targetId()) && "user".equals(application.targetType())
                            && "active".equals(application.status()))
                    .map(PermissionTemplateApplication::id)
                    .toList();
        }

        @Override
        public List<PermissionTemplateApplication> findActiveTemplateApplications(String templateId) {
            return templateApplications.stream()
                    .filter(application -> application.templateId().equals(templateId) && "active".equals(application.status())
                            && !application.consumed()
                            && (application.validUntil() == null || application.validUntil().isAfter(Instant.now())))
                    .toList();
        }

        @Override
        public PermissionTemplate copyTemplate(String sourceTemplateId, PermissionTemplate copy) {
            List<PermissionTemplateItem> copiedItems = items.get(sourceTemplateId).stream()
                    .map(item -> new PermissionTemplateItem(copy.id(), item.permissionCode(), item.effect(), item.dataScope(), item.sensitive()))
                    .toList();
            return saveTemplate(copy, copiedItems);
        }

        @Override
        public List<PermissionTemplateItem> findTemplateItems(String templateId) {
            return items.getOrDefault(templateId, List.of());
        }

        @Override
        public AccessUser createUser(AccessUser user) {
            users.put(user.id(), user);
            return user;
        }

        @Override
        public void updateUserScope(String userId, String organizationId, String merchantId, String actorId, Instant changedAt) {
            AccessUser u = users.get(userId);
            if (u != null) {
                users.put(userId, new AccessUser(u.id(), u.username(), u.accountType(), organizationId, merchantId, u.status(),
                        u.createdAt(), u.phoneHash(), u.createdVia(), u.consentReference(), u.evidenceAttachmentIds(), u.operatorNote()));
            }
        }

        @Override
        public List<AccessUser> listUsers() {
            return new ArrayList<>(users.values());
        }

        @Override
        public Optional<AccessUser> findUserById(String userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public List<RoleRecord> listRoles() {
            return new ArrayList<>(roles.values());
        }

        @Override
        public Optional<RoleRecord> findRoleById(String roleId) {
            return Optional.ofNullable(roles.get(roleId));
        }

        @Override
        public RoleRecord createRole(RoleRecord role) {
            roles.put(role.id(), role);
            return role;
        }

        @Override
        public RoleAssignmentRecord assignUserRole(String userId, String roleId) {
            userRoles.computeIfAbsent(userId, ignored -> new LinkedHashSet<>()).add(roleId);
            return new RoleAssignmentRecord(userId, roleId);
        }

        @Override
        public void unassignUserRole(String userId, String roleId) {
            userRoles.computeIfAbsent(userId, ignored -> new LinkedHashSet<>()).remove(roleId);
        }

        @Override
        public List<PositionRoleTemplate> listPositionRoleTemplates(String organizationId) {
            return positionRoleTemplates.values().stream()
                    .filter(mapping -> organizationId == null
                            || mapping.organizationId() == null
                            || organizationId.equals(mapping.organizationId()))
                    .toList();
        }

        @Override
        public Optional<PositionRoleTemplate> findPositionRoleTemplateById(String id) {
            lockOrder.add("mapping:" + id); // 真实实现 FOR UPDATE
            return Optional.ofNullable(positionRoleTemplates.get(id));
        }

        @Override
        public Optional<PositionRoleTemplate> findPositionRoleTemplate(
                String organizationId,
                String positionCode,
                String staffLevelId
        ) {
            Optional<PositionRoleTemplate> own = positionRoleTemplates.values().stream()
                    .filter(mapping -> "active".equals(mapping.status()))
                    .filter(mapping -> positionCode.equals(mapping.positionCode()))
                    .filter(mapping -> staffLevelId.equals(mapping.staffLevelId()))
                    .filter(mapping -> organizationId != null && organizationId.equals(mapping.organizationId()))
                    .findFirst();
            return own.or(() -> positionRoleTemplates.values().stream()
                    .filter(mapping -> "active".equals(mapping.status()))
                    .filter(mapping -> positionCode.equals(mapping.positionCode()))
                    .filter(mapping -> staffLevelId.equals(mapping.staffLevelId()))
                    .filter(mapping -> mapping.organizationId() == null)
                    .findFirst());
        }

        // 与 JdbcAccessRepository 同语义：upsert 不动存档/删除列；生命周期保存恒写 inactive。
        @Override
        public PositionRoleTemplate savePositionRoleTemplate(PositionRoleTemplate mapping) {
            PositionRoleTemplate existing = positionRoleTemplates.get(mapping.id());
            positionRoleTemplates.put(mapping.id(), existing == null ? mapping : new PositionRoleTemplate(
                    mapping.id(), mapping.positionCode(), mapping.staffLevelId(), mapping.roleId(), mapping.templateId(),
                    mapping.organizationId(), mapping.status(), mapping.createdBy(), mapping.createdAt(), mapping.updatedAt(),
                    existing.archivedAt(), existing.deletedAt(), existing.deleteReviewId(), existing.deleteReason()));
            return mapping;
        }

        @Override
        public void savePositionTemplateLifecycle(PositionRoleTemplate mapping, String actorId) {
            positionRoleTemplates.put(mapping.id(), mapping.withLifecycle(mapping.archivedAt(), mapping.deletedAt(),
                    mapping.deleteReviewId(), mapping.deleteReason(), mapping.updatedAt()));
        }

        @Override
        public void deletePositionRoleTemplate(String id) {
            positionRoleTemplates.remove(id);
        }

        @Override
        public UserPositionAssignment saveUserPositionAssignment(UserPositionAssignment assignment) {
            userPositionAssignments.put(assignment.userId(), assignment);
            return assignment;
        }

        @Override
        public Optional<UserPositionAssignment> findUserPositionAssignment(String userId) {
            return Optional.ofNullable(userPositionAssignments.get(userId));
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
            List<PermissionTemplateApplication> applications = targetUserIds.stream()
                    .map(userId -> new PermissionTemplateApplication(
                            "app_" + userId + "_" + (templateApplications.size() + 1),
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
            templateApplications.addAll(applications);
            return applications;
        }

        public int revokeTemplateApplications(String userId, List<String> applicationIds, String revokedBy, Instant revokedAt) {
            int revoked = 0;
            for (int i = 0; i < templateApplications.size(); i++) {
                PermissionTemplateApplication a = templateApplications.get(i);
                if (userId.equals(a.targetId()) && applicationIds.contains(a.id()) && "active".equals(a.status())) {
                    templateApplications.set(i, new PermissionTemplateApplication(a.id(), a.templateId(), a.targetType(), a.targetId(),
                            a.validFrom(), a.validUntil(), a.singleUse(), a.consumed(), "revoked", a.appliedBy(), a.appliedAt(), revokedBy, revokedAt));
                    revoked++;
                }
            }
            return revoked;
        }

        @Override
        public TemporaryGrant createTemporaryGrant(TemporaryGrant temporaryGrant) {
            temporaryGrants.put(temporaryGrant.id(), temporaryGrant);
            return temporaryGrant;
        }

        @Override
        public TemporaryGrant revokeTemporaryGrant(String id, String revokedBy, Instant revokedAt) {
            TemporaryGrant grant = temporaryGrants.get(id);
            TemporaryGrant revoked = new TemporaryGrant(
                    grant.id(),
                    grant.targetUserId(),
                    grant.permissionCodes(),
                    grant.validFrom(),
                    grant.validUntil(),
                    grant.singleUse(),
                    grant.consumed(),
                    "revoked",
                    grant.reason(),
                    grant.grantedBy(),
                    grant.grantedAt(),
                    revokedBy,
                    revokedAt
            );
            temporaryGrants.put(id, revoked);
            return revoked;
        }

        @Override
        public Optional<TemporaryGrant> findTemporaryGrantById(String id) {
            return Optional.ofNullable(temporaryGrants.get(id));
        }

        @Override
        public AccessGrantRequestRecord saveGrantRequest(AccessGrantRequestRecord request) {
            grantRequests.put(request.id(), request);
            return request;
        }

        @Override
        public Optional<AccessGrantRequestRecord> findGrantRequestById(String id) {
            return Optional.ofNullable(grantRequests.get(id));
        }

        @Override
        public List<AccessGrantRequestRecord> listGrantRequests(Instant createdAtFrom) {
            return grantRequests.values().stream()
                    .filter(request -> createdAtFrom == null || !request.createdAt().isBefore(createdAtFrom))
                    .toList();
        }

        @Override
        public List<PermissionEffect> findPermissionEffects(String userId, Instant checkedAt) {
            List<PermissionEffect> effects = new ArrayList<>();
            for (PermissionEffect override : overrides) {
                if (override.userId().equals(userId)) {
                    effects.add(override);
                }
            }
            // M036-001：与 JdbcAccessRepository 同语义——岗位派生角色与直接装配的角色一样以 ROLE 来源并入授予。
            Set<String> roleIds = new LinkedHashSet<>(userRoles.getOrDefault(userId, Set.of()));
            if (positionRoles.get(userId) != null) {
                roleIds.add(positionRoles.get(userId));
            }
            for (String roleId : roleIds) {
                RoleRecord role = roles.get(roleId);
                if (role == null || !"active".equals(role.status())) {
                    continue;
                }
                for (String permissionCode : role.permissionCodes()) {
                    effects.add(new PermissionEffect(
                            userId,
                            permissionCode,
                            Effect.GRANT,
                            Source.ROLE,
                            roleId,
                            null,
                            null,
                            false,
                            false
                    ));
                }
            }
            for (PermissionTemplateApplication application : templateApplications) {
                if (!userId.equals(application.targetId()) || !"active".equals(application.status())) {
                    continue;
                }
                for (PermissionTemplateItem item : findTemplateItems(application.templateId())) {
                    effects.add(new PermissionEffect(
                            userId,
                            item.permissionCode(),
                            item.effect(),
                            Source.TEMPLATE_APPLICATION,
                            application.id(),
                            application.validFrom(),
                            application.validUntil(),
                            application.singleUse(),
                            application.consumed()
                    ));
                }
            }
            for (TemporaryGrant grant : temporaryGrants.values()) {
                if (!userId.equals(grant.targetUserId()) || !"active".equals(grant.status())) {
                    continue;
                }
                for (String permissionCode : grant.permissionCodes()) {
                    effects.add(new PermissionEffect(
                            userId,
                            permissionCode,
                            Effect.GRANT,
                            Source.TEMPORARY_GRANT,
                            grant.id(),
                            grant.validFrom(),
                            grant.validUntil(),
                            grant.singleUse(),
                            grant.consumed()
                    ));
                }
            }
            return effects;
        }

        @Override
        public boolean markSingleUsePermissionConsumed(String sourceId, String permissionCode, String userId, Instant consumedAt) {
            return true;
        }
    }

    private static final class InMemorySupervisorReviewRepository implements SupervisorReviewRepository {
        final Map<String, SupervisorReviewDecisionRecord> reviews = new LinkedHashMap<>();
        // M087：直接上级表与审批链节点（申请人 usr_requester 的直接上级为 usr_admin，链上只有这一个节点）。
        final Map<String, String> superiors = new LinkedHashMap<>(Map.of("usr_requester", "usr_admin"));
        final Map<String, List<com.complex.platform.governance.chain.ApprovalChainNode>> nodes = new LinkedHashMap<>();
        // XM-D029-003：与 JDBC 同样存下排期里的直接受益人，审批时才读得到。
        final Map<String, ReviewSchedule> schedules = new LinkedHashMap<>();

        @Override
        public void scheduleSupervisorReview(String reviewId, String beneficiaryId, Instant dueAt) {
            schedules.put(reviewId, new ReviewSchedule(reviewId, beneficiaryId, dueAt, 0, null, null));
        }

        @Override
        public Optional<ReviewSchedule> findSupervisorReviewSchedule(String reviewId) {
            return Optional.ofNullable(schedules.get(reviewId));
        }

        @Override
        public Optional<String> findDirectSuperior(String userId) {
            return Optional.ofNullable(superiors.get(userId));
        }

        @Override
        public void saveChainNodes(String reviewId, List<com.complex.platform.governance.chain.ApprovalChainNode> chain) {
            nodes.put(reviewId, new ArrayList<>(chain));
        }

        @Override
        public List<com.complex.platform.governance.chain.ApprovalChainNode> listChainNodes(String reviewId) {
            return List.copyOf(nodes.getOrDefault(reviewId, List.of()));
        }

        @Override
        public boolean updateChainNode(com.complex.platform.governance.chain.ApprovalChainNode node) {
            List<com.complex.platform.governance.chain.ApprovalChainNode> chain = nodes.get(node.reviewId());
            if (chain == null) {
                return false;
            }
            for (int i = 0; i < chain.size(); i++) {
                if (chain.get(i).nodeId().equals(node.nodeId())) {
                    if (!chain.get(i).pending()) {
                        return false; // M087-I2-02：只允许改写待审节点（与 JDBC 的 status='pending' 条件一致）
                    }
                    chain.set(i, node);
                    return true;
                }
            }
            return false;
        }

        @Override
        public SupervisorReviewDecisionRecord saveSupervisorReview(SupervisorReviewDecisionRecord record) {
            reviews.put(record.reviewId(), record);
            return record;
        }

        @Override
        public Optional<SupervisorReviewDecisionRecord> findPendingSupervisorReviewById(String reviewId) {
            return Optional.ofNullable(reviews.get(reviewId))
                    .filter(review -> "pending_review".equals(review.reviewStatus()));
        }
    }
}
