package com.complex.platform.access;

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
        service.createPermissionTemplate(new PermissionTemplateRequest(
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

        PermissionTemplate copy = service.copyPermissionTemplate("tpl_create_source", new PermissionTemplateRequest(
                "copy-source",
                "copy",
                List.of(),
                "Copied template",
                "temporary",
                "tpl_create_source",
                List.of("ops.production_readiness.view"),
                "project",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-15T00:00:00Z"),
                true
        ));
        service.updatePermissionTemplate("tpl_create_source", new PermissionTemplateRequest(
                "update-source",
                "update",
                List.of(),
                "Source template updated",
                "temporary",
                null,
                List.of("ops.production_readiness.check"),
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
    void applyPermissionTemplateAppliesToManyUsersAndWritesAuditEvent() {
        service.createPermissionTemplate(new PermissionTemplateRequest(
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

        PermissionTemplateApplyResult result = service.applyPermissionTemplate("tpl_create_apply", new PermissionTemplateApplyRequest(
                "apply-many",
                "apply",
                List.of(),
                "tpl_create_apply",
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
        for (String type : List.of("staff", "merchant", "supervisor", "sales", "merchant_admin")) {
            AccessUser user = service.createAccessUser(new AccessUserRequest(
                    "user-type-" + type,
                    null,
                    List.of(),
                    "user_" + type,
                    null,
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
                "sales",
                "seed-org",
                "merchant-1",
                "active",
                "consent_staff_manual_003"
        ));

        assertThat(user.phoneHash()).hasSize(64).doesNotContain("13800000002");
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
        service.createAccessUser(new AccessUserRequest(
                "k-u-emp1", null, List.of(), "emp1", null, null, "staff", "seed-org", null, "active"));
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
        service.createAccessUser(new AccessUserRequest(
                "k-u-emp2", null, List.of(), "emp2", null, null, "staff", "seed-org", null, "active"));
        assertThatThrownBy(() -> service.assignStaffLevel(new StaffLevelAssignmentRequest(
                "k-a-emp2", null, "usr_emp2", "lvl_does_not_exist", null, null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(400);
                    assertThat(e.code()).isEqualTo("UNKNOWN_STAFF_LEVEL");
                });
    }

    @Test
    void assignStaffLevelRejectsNonCnyCurrency() {
        service.createAccessUser(new AccessUserRequest(
                "k-u-emp3", null, List.of(), "emp3", null, null, "staff", "seed-org", null, "active"));
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
        assertThat(accessRepository.userRoles.get("usr_position_target")).contains("role_ops_check");
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

    @Test
    void assignUserPositionRequiresApprovalWhenMappedRoleOrTemplateIsHighRisk() {
        accessRepository.markRequiresApproval("ops.production_readiness.check", "high");
        service = newAccessService(true, null);
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

        assertThatThrownBy(() -> service.assignUserPosition(new UserPositionAssignmentRequest(
                "assign-high-risk-position", null, "usr_position_high_risk", "ops_manager", "lvl_m1_lower",
                null, null, null))).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.status().value()).isEqualTo(403);
            assertThat(e.code()).isEqualTo("GRANT_REQUIRES_APPROVAL");
        });
        assertThat(accessRepository.findUserPositionAssignment("usr_position_high_risk")).isEmpty();
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
        assertThat(authorizationService.effectivePermissionsForUser("usr_temp_target").grants())
                .doesNotContain("ops.production_readiness.check");
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

        AccessGrantSummary summary = service.grantSummary(30, 2, 2);

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
    }

    // ── P1: applyPermissionTemplate 跨组织校验 ─────────────────────────────

    @Test
    void applyPermissionTemplateWithCrossOrgTargetUserThrowsForbidden() {
        service.createPermissionTemplate(new PermissionTemplateRequest(
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

        assertThatThrownBy(() -> service.applyPermissionTemplate("tpl_create_cross_apply",
                new PermissionTemplateApplyRequest(
                        "apply-cross",
                        "apply",
                        List.of(),
                        "tpl_create_cross_apply",
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
        service.createPermissionTemplate(new PermissionTemplateRequest(
                "create-platform-tpl", "seed", List.of(), "Platform Template", "temporary",
                null, List.of("ops.production_readiness.check"), "project",
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-15T00:00:00Z"), false));
        accessRepository.users.put("usr_org_a_same", new AccessUser(
                "usr_org_a_same", "org-a-same", "staff", "org_a", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        authenticateAsOrgUser("usr_org_a_actor2", "org-a-actor2", "org_a");
        assertThatThrownBy(() -> service.applyPermissionTemplate("tpl_create_platform_tpl",
                new PermissionTemplateApplyRequest("apply-platform", "apply", List.of(),
                        "tpl_create_platform_tpl", List.of("usr_org_a_same"), "merge")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.status().value()).isEqualTo(403);
            assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN");
        });
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
                new AuditEventService(auditEventRepository)
        );
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

        @Override
        public PositionRoleTemplate savePositionRoleTemplate(PositionRoleTemplate mapping) {
            positionRoleTemplates.put(mapping.id(), mapping);
            return mapping;
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
            templateApplications.addAll(applications);
            return applications;
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
            for (String roleId : userRoles.getOrDefault(userId, Set.of())) {
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

    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String key = record.operationId() + ":" + record.idempotencyKey();
            if (records.containsKey(key)) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(record.operationId() + ":" + record.idempotencyKey(), record);
            return record;
        }
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }
}
