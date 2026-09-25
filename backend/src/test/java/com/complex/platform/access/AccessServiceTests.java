package com.complex.platform.access;

// 文件版本：V0.15

import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.auth.AuthService;
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

    // M006-R6-02（PGM-1.10.1「显式拒绝优先级最高」「既有高危不得因处于岗位模板内而免除授出前审批」）：岗位高危挂起写下的显式拒绝
    // 不能经「移除覆盖」无审批解除——移除会使须审批的权限从既有 GRANT 来源生效，等同授出，须经挂起单审批释放。
    @Test
    void removingTheHighRiskHoldDenyRequiresApproval() {
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
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_REQUIRES_APPROVAL"));
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

    // M002-F02：挂起行算作已拒绝——同岗再次定岗不重复写挂起行、不另开释放单。
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

    // M002-F01：落地时校验终审人与目标同组织（与角色、临时授权落地同口径）——跨组织终审人落地被拒，不写授予，申请不变为已生效。
    @Test
    void userPermissionOverrideLandingRejectsCrossOrganizationApprover() {
        m002HighRiskFixture("usr_m002_cross", null, "lvl_m1_lower");
        AccessGrantRequestRecord pending = service.applyGrant(m002Apply(
                "m002-apply-cross", "user_permission_grant", "usr_m002_cross", List.of(M002_CODE)));

        authenticateAsOrgUser("usr_boss", "boss", "org_b");
        assertThatThrownBy(() -> service.reviewGrant(pending.id(), m002Approve("m002-review-cross")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN"));
        assertThat(overrideSources("usr_m002_cross", M002_CODE, AccessRepository.Effect.GRANT)).isEmpty();
        assertThat(accessRepository.grantRequests.get(pending.id()).status()).isEqualTo("pending");
    }

    // M002：共用装配——check 标为须审批高危；usr_admin（平台管理员、申请人）的直接上级为 usr_boss；
    // 高危岗位映射 ops_manager / highRiskLevel → role_ops_check + tpl_ops_check（沿用上方高危定岗用例的装配）。
    private void m002HighRiskFixture(String userId, String merchantId, String highRiskLevel) {
        accessRepository.markRequiresApproval(M002_CODE, "high");
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
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
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
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
        // M036-001：与 JdbcAccessRepository 同语义——先撤上一次岗位派生的模板应用，再记新角色与新应用 id；独立模板应用不受影响。
        @Override public void replacePositionGrants(String userId, String roleId, String mappingId,
                List<String> applicationIds, String actorId, Instant at) {
            revokeTemplateApplications(userId, positionApplicationIds.getOrDefault(userId, List.of()), actorId, at);
            positionRoles.put(userId, roleId);
            positionApplicationIds.put(userId, List.copyOf(applicationIds));
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

        // M002-F02：与 JdbcAccessRepository 同语义——员工显式行（USER_OVERRIDE）与岗位高危挂起行（POSITION_HOLD）按来源分行，
        // upsert 只覆盖同来源的行；deleteUserPermissionOverrides 删该码该 effect 的全部来源。
        @Override
        public void saveUserPermissionOverrides(String userId, List<String> permissionCodes, Effect effect, String reason, String createdBy) {
            upsertOverrides(userId, permissionCodes, effect, Source.USER_OVERRIDE);
        }

        @Override
        public void saveHighRiskHolds(String userId, List<String> permissionCodes, String reason, String createdBy) {
            upsertOverrides(userId, permissionCodes, Effect.DENY, Source.POSITION_HOLD);
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

        private void upsertOverrides(String userId, List<String> permissionCodes, Effect effect, Source source) {
            removeOverrides(userId, permissionCodes, effect, source);
            for (String code : permissionCodes) {
                overrides.add(new PermissionEffect(userId, code, effect, source,
                        userId + ":" + code + ":" + effect + ":" + source, null, null, false, false));
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
