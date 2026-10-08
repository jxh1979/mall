package com.complex.platform.access;

// 文件版本：V0.29

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
import com.complex.platform.access.dto.RoleViewableProjectsRequest;
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
    private static final String GRANT_REVIEW = "access.grant_request.review";
    private static final String GRANT_REVIEWER_ROLE = "grant_reviewer";

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
        // M002-F37：平台管理员持 access.role.assign（与真库 role_platform_admin 种子同口径），才能发起员工单独授予、解除显式拒绝与岗位高危释放申请。
        accessRepository.roles.put("explicit_admin_grants", new RoleRecord(
                "explicit_admin_grants", "Explicit test grants", null, "global", "active", Instant.now(),
                List.of("ops.production_readiness.check", "governance.supervisor_review.view",
                        "governance.supervisor_review.approve", "finance.ar_ap.manage", "access.role.assign"), null));
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

    // M002-F47（治理地图 #audit 第 893、899 行：权限变更留痕要能查到「为什么」）：建角色、直接分配、撤销、员工覆盖设置与解除的成功事件
    // metadata 记请求提交的 operator_note 原文，未填记空串；同键同请求回放不新增业务写入与成功事件。
    @Test
    void roleAndOverrideSuccessEventsRecordTheSubmittedOperatorNote() {
        accessRepository.users.put("usr_m002_note", new AccessUser(
                "usr_m002_note", "usr_m002_note", "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        RoleRequest create = new RoleRequest("m002-note-role", "为临时代班建立职责角色", List.of(), "代班角色", null, List.of(M002_VIEW), "project");
        String roleId = service.createRole(create).id();
        RoleAssignmentRequest assign = new RoleAssignmentRequest("m002-note-assign", "代替休假员工值班", "usr_m002_note", roleId);
        RoleAssignmentRequest unassign = new RoleAssignmentRequest("m002-note-unassign", "代班结束收回权限", "usr_m002_note", roleId);
        UserPermissionOverrideRequest deny = new UserPermissionOverrideRequest(
                "m002-note-deny", "离岗期间暂停查看", List.of(M002_VIEW), "deny", null);
        UserPermissionOverrideRequest undeny = new UserPermissionOverrideRequest(
                "m002-note-undeny", null, List.of(M002_VIEW), "deny", "返岗恢复");
        service.assignUserRole(assign);
        service.unassignUserRole(unassign);
        service.setUserPermissionOverrides("usr_m002_note", deny);
        service.removeUserPermissionOverrides("usr_m002_note", undeny);
        Map<String, String> expected = Map.of(
                "m002-note-role", "role.changed/为临时代班建立职责角色",
                "m002-note-assign", "access.role.assigned/代替休假员工值班",
                "m002-note-unassign", "access.role.unassigned/代班结束收回权限",
                "m002-note-deny", "access.user_permission_override.set/离岗期间暂停查看",
                "m002-note-undeny", "access.user_permission_override.remove/");
        assertThat(m002NoteEvents(expected.keySet())).containsExactlyInAnyOrderElementsOf(expected.values());

        int roles = accessRepository.roles.size();
        service.createRole(create);
        service.assignUserRole(assign);
        service.unassignUserRole(unassign);
        service.setUserPermissionOverrides("usr_m002_note", deny);
        service.removeUserPermissionOverrides("usr_m002_note", undeny);
        assertThat(accessRepository.roles).hasSize(roles);
        assertThat(accessRepository.userRoles.getOrDefault("usr_m002_note", Set.of())).doesNotContain(roleId);
        assertThat(accessRepository.overrides).noneMatch(o -> "usr_m002_note".equals(o.userId()));
        assertThat(m002NoteEvents(expected.keySet())).containsExactlyInAnyOrderElementsOf(expected.values());
    }

    // M002-F48（F47 验收「各自精确等于请求原文」）：五类成功事件的 operator_note 不去首尾空格与换行、纯空白不变空串，null 记空串。
    @Test
    void roleAndOverrideSuccessEventsKeepTheOperatorNoteVerbatim() {
        accessRepository.users.put("usr_m002_raw", new AccessUser(
                "usr_m002_raw", "usr_m002_raw", "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        List<String> notes = java.util.Arrays.asList("  首尾空格  ", "换行结尾\n", "\r\n前后换行\r\n", "   ", "普通中文说明", null);
        for (int i = 0; i < notes.size(); i++) {
            String note = notes.get(i);
            String roleId = service.createRole(new RoleRequest(
                    "m002-raw-role-" + i, note, List.of(), "原文角色" + i, null, List.of(M002_VIEW), "project")).id();
            service.assignUserRole(new RoleAssignmentRequest("m002-raw-assign-" + i, note, "usr_m002_raw", roleId));
            service.unassignUserRole(new RoleAssignmentRequest("m002-raw-unassign-" + i, note, "usr_m002_raw", roleId));
            service.setUserPermissionOverrides("usr_m002_raw", new UserPermissionOverrideRequest(
                    "m002-raw-deny-" + i, note, List.of(M002_VIEW), "deny", "暂停查看"));
            service.removeUserPermissionOverrides("usr_m002_raw", new UserPermissionOverrideRequest(
                    "m002-raw-undeny-" + i, note, List.of(M002_VIEW), "deny", "恢复查看"));
            String expected = note == null ? "" : note;
            Set<String> keys = Set.of("m002-raw-role-" + i, "m002-raw-assign-" + i, "m002-raw-unassign-" + i,
                    "m002-raw-deny-" + i, "m002-raw-undeny-" + i);
            assertThat(m002NoteEvents(keys)).as("operator_note=%s", note).containsExactlyInAnyOrder(
                    "role.changed/" + expected, "access.role.assigned/" + expected, "access.role.unassigned/" + expected,
                    "access.user_permission_override.set/" + expected, "access.user_permission_override.remove/" + expected);
        }
    }

    private List<String> m002NoteEvents(Set<String> keys) {
        return auditEventRepository.events.stream()
                .filter(event -> "success".equals(event.eventResult()) && keys.contains(event.idempotencyKey()))
                .map(event -> event.eventName() + "/" + event.metadata().get("operator_note"))
                .toList();
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

    // M002-F41（治理地图「系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效」，R-002；确定性交错）：改职级先锁员工行再读岗位——
    // 锁前另一笔换岗已提交（锁前钩子），改职级按锁内读到的新岗位重算：岗位编码不写回旧值，两表职级一致，派生角色对应新岗位与新职级。
    @Test
    void staffLevelChangeRederivesFromThePositionReadUnderTheTargetLock() {
        String user = "usr_m002_f41_move";
        m002F41Fixture(user);
        service.assignUserPosition(new UserPositionAssignmentRequest("m002-f41-seat", null, user, "ops_f41_old", "lvl_m1_lower", null, null, null));
        accessRepository.beforeNextUserLock = () -> service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-f41-move", null, user, "ops_f41_new", "lvl_m1_lower", null, null, null));

        service.assignStaffLevel(new StaffLevelAssignmentRequest("m002-f41-level", null, user, "lvl_m10_upper", null, null, null));
        m002F41ExpectSeated(user);
    }

    // M002-F41（确定性交错）：员工原本无岗位——锁前另一笔定岗已提交，改职级在锁内读到新岗位，走定岗同路径改到新职级：两表职级一致（不只写职级表）。
    @Test
    void staffLevelChangeOfAnUnseatedEmployeeSeesASeatCommittedBeforeItsLock() {
        String user = "usr_m002_f41_seat";
        m002F41Fixture(user);
        accessRepository.beforeNextUserLock = () -> service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-f41-seat-race", null, user, "ops_f41_new", "lvl_m1_lower", null, null, null));

        service.assignStaffLevel(new StaffLevelAssignmentRequest("m002-f41-unseated-level", null, user, "lvl_m10_upper", null, null, null));
        m002F41ExpectSeated(user);
    }

    // M002-F41：岗位 ops_f41_old / ops_f41_new × 两档职级各一张映射（日常码 view 的模板，各映射自己的角色：派生角色即可看出员工挂在哪张映射上）。
    private void m002F41Fixture(String user) {
        accessRepository.users.put(user, new AccessUser(user, user, "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_view", "tpl_ops_view", "Ops View", null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_ops_view", M002_VIEW, AccessRepository.Effect.GRANT, "project", false)));
        for (String position : List.of("ops_f41_old", "ops_f41_new")) {
            for (String level : List.of("lvl_m1_lower", "lvl_m10_upper")) {
                String role = "role_" + position + "_" + level;
                accessRepository.roles.put(role, new RoleRecord(role, role, role, "project", "active", Instant.parse("2026-06-14T00:00:00Z"),
                        List.of(M002_VIEW), null));
                service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                        "m002-f41-map-" + role, null, position, level, role, "tpl_ops_view", "active"));
            }
        }
    }

    private void m002F41ExpectSeated(String user) {
        assertThat(accessRepository.beforeNextUserLock).as("改职级在读岗位前锁员工行（锁前钩子已执行）").isNull();
        assertThat(accessRepository.userPositionAssignments.get(user)).isNotNull().satisfies(position -> {
            assertThat(position.positionCode()).isEqualTo("ops_f41_new");
            assertThat(position.staffLevelId()).isEqualTo("lvl_m10_upper");
        });
        assertThat(accessRepository.staffLevelAssignments.get(user).staffLevelId()).isEqualTo("lvl_m10_upper");
        assertThat(accessRepository.positionRoles.get(user)).isEqualTo("role_ops_f41_new_lvl_m10_upper");
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

    // M002-F40（治理地图第 655 行「显式拒绝优先级最高」，R-001「显式拒绝仍优先」）：授权申请的两个审批入口——授权申请页（AccessController.reviewGrant）
    // 与通用主管复核入口（SupervisorReviewController.approve，授权申请类待审复核单）——同一门禁：两码一次核验。链上合法当前节点持两码却被显式拒绝
    // 「审核高危授权申请」时，两入口的通过与驳回都 403 PERMISSION_DENIED，申请、复核节点、目标授权不变，拒绝行仍在；解除拒绝后非当前节点、受益人、
    // 申请人仍被拒，当前节点经通用入口批准落地；同一幂等键经两入口重放都不重复落地。
    @Test
    void explicitlyDeniedGrantReviewerIsRefusedAtBothReviewEntriesUntilTheDenyIsLifted() {
        String target = "usr_m002_f40";
        m002HighRiskFixture(target, null, "lvl_m1_lower");
        AccessGrantRequestRecord pending = service.applyGrant(m002Apply(
                "m002-f40-apply", "user_permission_grant", target, List.of(M002_CODE)));
        accessRepository.users.put("usr_admin", new AccessUser("usr_admin", "seed-admin", "platform_admin", "seed-org", null, "active",
                Instant.parse("2026-06-14T00:00:00Z"))); // 申请人账号：通用入口按发起人项目判定复核单可见（落地后回读终态）
        m002F40Reviewers("usr_boss");
        accessRepository.saveUserPermissionOverrides("usr_boss", List.of(GRANT_REVIEW), AccessRepository.Effect.DENY, "停审批授权申请", "usr_admin");
        List<com.complex.platform.governance.chain.ApprovalChainNode> nodesBefore = m002Reviews.listChainNodes(pending.reviewId());

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        m002F40Refused(pending, "m002-f40-denied");
        assertThat(m002Reviews.listChainNodes(pending.reviewId())).isEqualTo(nodesBefore);
        assertThat(overrideSources(target, M002_CODE, AccessRepository.Effect.GRANT)).isEmpty();
        assertThat(overrideSources("usr_boss", GRANT_REVIEW, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.USER_OVERRIDE);

        accessRepository.deleteExplicitDenies("usr_boss", List.of(GRANT_REVIEW));
        accessRepository.users.put("usr_m002_f40_other", new AccessUser(
                "usr_m002_f40_other", "usr_m002_f40_other", "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        m002F40Reviewers("usr_m002_f40_other", target, "usr_admin");
        Map<Runnable, String> refused = new LinkedHashMap<>();
        refused.put(() -> authenticateAsOrgUser("usr_m002_f40_other", "m002-f40-other", "seed-org"), "SUPERVISOR_NOT_CURRENT_APPROVER");
        refused.put(() -> authenticateAsOrgUser(target, target, "seed-org"), "BENEFICIARY_SELF_REVIEW_FORBIDDEN");
        refused.put(() -> authenticateAsPlatformAdmin("seed-org"), "SUPERVISOR_SELF_REVIEW_FORBIDDEN"); // 申请人 usr_admin
        refused.forEach((login, code) -> {
            login.run();
            assertThatThrownBy(() -> new AccessController(service, authorizationService).reviewGrant(pending.id(), m002Approve("m002-f40-refused-" + code)))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(code));
        });
        assertThat(accessRepository.grantRequests.get(pending.id()).status()).isEqualTo("pending");

        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        for (int replay = 0; replay < 2; replay++) {
            assertThat(m002ChainController().approve(pending.reviewId(), m002ChainDecision("m002-f40-approve", "approved")).data().reviewStatus())
                    .isEqualTo("approved");
        }
        assertThat(new AccessController(service, authorizationService).reviewGrant(pending.id(),
                new AccessGrantReviewRequest("m002-f40-approve", "M002", List.of(), "approve", "同意")).data().status()).isEqualTo("applied");
        assertThat(accessRepository.grantRequests.get(pending.id()).status()).isEqualTo("applied");
        assertThat(overrideSources(target, M002_CODE, AccessRepository.Effect.GRANT)).containsExactly(AccessRepository.Source.USER_OVERRIDE);
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "access.user_permission_override.set".equals(event.eventName())
                        && pending.id().equals(event.metadata().get("grant_request_id")))
                .hasSize(1);
    }

    // M002-F40：最高权限本人裁决末节点同样受显式拒绝约束——拒绝「审核高危授权申请」后两入口的通过与驳回都 403、末节点与申请不变；解除后经通用入口照常终审落地。
    @Test
    void explicitlyDeniedMasterAuthorityCannotFinalizeAGrantRequestUntilTheDenyIsLifted() {
        String target = "usr_m002_f40_top";
        m002HighRiskFixture(target, null, "lvl_m1_lower");
        accessRepository.markRequiresApproval(M002_CODE, "critical");
        m002F40Reviewers();
        AccessGrantRequestRecord pending = service.applyGrant(m002Apply(
                "m002-f40-top-apply", "user_permission_grant", target, List.of(M002_CODE)));
        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        assertThat(service.reviewGrant(pending.id(), m002Approve("m002-f40-top-boss")).status()).isEqualTo("pending");
        accessRepository.saveUserPermissionOverrides(SessionIssuer.MASTER_USER_ID, List.of(GRANT_REVIEW), AccessRepository.Effect.DENY,
                "停审批授权申请", "usr_admin");
        List<com.complex.platform.governance.chain.ApprovalChainNode> nodesBefore = m002Reviews.listChainNodes(pending.reviewId());
        assertThat(nodesBefore).extracting(node -> node.approverKind() + "/" + node.status()).containsExactly("user/approved", "master/pending");

        authenticateAsMasterAuthority();
        m002F40Refused(pending, "m002-f40-top-denied");
        assertThat(m002Reviews.listChainNodes(pending.reviewId())).isEqualTo(nodesBefore);
        assertThat(overrideSources(target, M002_CODE, AccessRepository.Effect.GRANT)).isEmpty();
        assertThat(overrideSources(SessionIssuer.MASTER_USER_ID, GRANT_REVIEW, AccessRepository.Effect.DENY))
                .containsExactly(AccessRepository.Source.USER_OVERRIDE);

        accessRepository.deleteExplicitDenies(SessionIssuer.MASTER_USER_ID, List.of(GRANT_REVIEW));
        assertThat(m002ChainController().approve(pending.reviewId(), m002ChainDecision("m002-f40-top-master", "approved")).data().reviewStatus())
                .isEqualTo("approved");
        assertThat(accessRepository.grantRequests.get(pending.id()).status()).isEqualTo("applied");
        assertThat(overrideSources(target, M002_CODE, AccessRepository.Effect.GRANT)).containsExactly(AccessRepository.Source.USER_OVERRIDE);
    }

    // M002-F40：字典登记「审核高危授权申请」（与真库 V170 同档），审批人经角色持该码与「主管复核审批」（两入口一次核验这两码）。
    private void m002F40Reviewers(String... userIds) {
        accessRepository.addPermission(GRANT_REVIEW, "critical", true);
        accessRepository.roles.put(GRANT_REVIEWER_ROLE, new RoleRecord(GRANT_REVIEWER_ROLE, "Grant reviewer", null, "global", "active",
                Instant.parse("2026-06-14T00:00:00Z"), List.of(GRANT_REVIEW, "governance.supervisor_review.approve"), null));
        for (String userId : userIds) {
            accessRepository.assignUserRole(userId, GRANT_REVIEWER_ROLE);
        }
    }

    private com.complex.platform.governance.review.SupervisorReviewController m002ChainController() {
        return new com.complex.platform.governance.review.SupervisorReviewController(m002ReviewService, authorizationService, service);
    }

    private com.complex.platform.governance.review.SupervisorReviewApprovalRequest m002ChainDecision(String key, String decision) {
        return new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(key, "M002", List.of(), null, decision, "同意");
    }

    // M002-F40：当前会话经授权申请页与通用主管复核入口的通过、驳回都 403 PERMISSION_DENIED（中文提示），不进审批，申请仍待审。
    private void m002F40Refused(AccessGrantRequestRecord pending, String key) {
        AccessController grantEntry = new AccessController(service, authorizationService);
        List<org.assertj.core.api.ThrowableAssert.ThrowingCallable> attempts = List.of(
                () -> grantEntry.reviewGrant(pending.id(), m002Approve(key + "-own-approve")),
                () -> grantEntry.reviewGrant(pending.id(), m002Reject(key + "-own-reject")),
                () -> m002ChainController().approve(pending.reviewId(), m002ChainDecision(key + "-chain-approve", "approved")),
                () -> m002ChainController().approve(pending.reviewId(), m002ChainDecision(key + "-chain-reject", "rejected")));
        for (org.assertj.core.api.ThrowableAssert.ThrowingCallable attempt : attempts) {
            assertThatThrownBy(attempt).isInstanceOfSatisfying(ApiException.class, e -> {
                assertThat(e.status().value()).isEqualTo(403);
                assertThat(e.code()).isEqualTo("PERMISSION_DENIED");
                assertThat(e.getMessage()).contains("没有操作权限");
            });
        }
        assertThat(accessRepository.grantRequests.get(pending.id()).status()).isEqualTo("pending");
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
        // M002-F37：跨组织与未开放类型的校验针对持分配角色权限的发起人（不持该码的员工在此之前即 403）
        accessRepository.roles.put("role_m002_assign", new RoleRecord("role_m002_assign", "assign", null, "project", "active",
                Instant.parse("2026-06-14T00:00:00Z"), List.of("access.role.assign"), null));
        accessRepository.userRoles.put("usr_requester", new LinkedHashSet<>(Set.of("role_m002_assign")));
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

    // M002-F32（技术推导）：同岗重定时岗位与所引模板都未变——保留原岗位派生套用（同一条、不撤销、不新增）。
    @Test
    void samePositionReassignmentKeepsThePositionTemplateApplication() {
        String user = "usr_m002_keep_application";
        m002HighRiskFixture(user, null, "lvl_m1_lower");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-keep-app-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        List<PermissionTemplateApplication> before = accessRepository.templateApplications.stream()
                .filter(application -> user.equals(application.targetId())).toList();
        assertThat(before).singleElement().extracting(PermissionTemplateApplication::status).isEqualTo("active");

        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-keep-app-reassign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        assertThat(accessRepository.templateApplications.stream().filter(application -> user.equals(application.targetId())).toList())
                .isEqualTo(before);
        assertThat(accessRepository.positionApplicationIds.get(user)).containsExactly(before.get(0).id());
    }

    // M002-F32（登记 1，技术推导）：映射改指单次模板、重新套用时，只有仅来自该单次模板的码不沿用旧批准；角色里也有、已批准并仍由角色生效的码
    // 照常生效，不重新挂起、不开新单。
    @Test
    void reissuingASingleUseTemplateKeepsApprovedCodesThatTheRoleStillGrants() {
        String user = "usr_m002_role_kept";
        PositionRoleTemplate mapping = m002HighRiskFixture(user, null, "lvl_m1_lower");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-role-kept-assign", null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        service.reviewGrant(m002PendingHold().id(), m002Approve("m002-role-kept-approve"));
        authenticateAsPlatformAdmin("seed-org");
        accessRepository.saveTemplate(new PermissionTemplate(
                "tpl_ops_once", "tpl_ops_once", "Ops Once", null, "temporary", null, "platform", null, "merge", null, null, true,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem("tpl_ops_once", M002_CODE, AccessRepository.Effect.GRANT, "project", false)));

        service.updatePositionRoleTemplate(mapping.id(), m002Mapping("m002-role-kept-once", "role_ops_check", "tpl_ops_once", "active"));
        assertThat(overrideSources(user, M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
        assertThat(m002PendingReleases(user)).isEmpty();
        assertThat(authorizationService.effectivePermissionsForUser(user).grants()).contains(M002_CODE);
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

    // M002-F26（技术不变量：信任边界输入先结构校验后查库；F21 验收「在排序、查库、写入前校验」）：两类授权申请、建角色、员工覆盖四个入口的
    // 权限码列表混有 null 或空白时 400 VALIDATION_FAILED，整张列表的结构校验先于任何权限存在性查询（查询 0 次），无新行、无成功事件。
    @Test
    void permissionCodeListsAreStructurallyCheckedBeforeAnyExistenceLookup() {
        String user = "usr_m002_structural";
        m002HighRiskFixture(user, null, "lvl_m1_lower");
        int roles = accessRepository.roles.size();
        int attempt = 0;
        for (List<String> codes : List.of(java.util.Arrays.asList(M002_CODE, null), java.util.Arrays.asList(null, M002_CODE),
                List.of(M002_CODE, "  "))) {
            String n = String.valueOf(++attempt);
            List<org.assertj.core.api.ThrowableAssert.ThrowingCallable> entries = List.of(
                    () -> service.applyGrant(m002Apply("m002-structural-grant-" + n, "user_permission_grant", user, codes)),
                    () -> service.applyGrant(m002Apply("m002-structural-lift-" + n, "user_permission_deny_removal", user, codes)),
                    () -> service.createRole(m002Role("m002-structural-role-" + n, "结构校验角色" + n, null, codes, "project")),
                    () -> service.setUserPermissionOverrides(user, new UserPermissionOverrideRequest(
                            "m002-structural-override-" + n, "停权", codes, "deny", "员工显式拒绝")));
            for (org.assertj.core.api.ThrowableAssert.ThrowingCallable entry : entries) {
                accessRepository.permissionExistsCalls = 0;
                assertThatThrownBy(entry).isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(400);
                    assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                });
                assertThat(accessRepository.permissionExistsCalls).as("结构校验失败时不应查询权限是否存在").isZero();
            }
        }
        assertThat(accessRepository.grantRequests.values()).isEmpty();
        assertThat(accessRepository.roles).hasSize(roles);
        assertThat(accessRepository.overrides).noneMatch(override -> user.equals(override.userId()));
        assertThat(auditEventRepository.events).noneMatch(event -> "success".equals(event.eventResult())
                && event.idempotencyKey() != null && event.idempotencyKey().startsWith("m002-structural-"));
    }

    // M002-F28（技术不变量：岗位映射 position_code 列宽 VARCHAR(64)）：新建或修改岗位映射时岗位编码超过 64 字 400 VALIDATION_FAILED，
    // 不落行、映射不变、无保存成功事件；64 字可保存。
    @Test
    void positionCodeLongerThanTheColumnIsRejectedBeforeSaving() {
        PositionRoleTemplate existing = m002HighRiskFixture("usr_m002_long_code", null, "lvl_m1_lower");
        int mappings = accessRepository.positionRoleTemplates.size();
        String tooLong = "p".repeat(65);
        List<org.assertj.core.api.ThrowableAssert.ThrowingCallable> entries = List.of(
                () -> service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                        "m002-long-create", null, tooLong, "lvl_m1_lower", "role_ops_check", "tpl_ops_check", "active")),
                () -> service.updatePositionRoleTemplate(existing.id(), new PositionRoleTemplateRequest(
                        "m002-long-update", null, tooLong, "lvl_m1_lower", "role_ops_check", "tpl_ops_check", "active")));
        for (org.assertj.core.api.ThrowableAssert.ThrowingCallable entry : entries) {
            assertThatThrownBy(entry).isInstanceOfSatisfying(ApiException.class, e -> {
                assertThat(e.status().value()).isEqualTo(400);
                assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
            });
        }
        assertThat(accessRepository.positionRoleTemplates).hasSize(mappings);
        assertThat(accessRepository.positionRoleTemplates.get(existing.id()).positionCode()).isEqualTo("ops_manager");
        assertThat(auditEventRepository.events).noneMatch(event -> "access.position_role_template.saved".equals(event.eventName())
                && ("m002-long-create".equals(event.idempotencyKey()) || "m002-long-update".equals(event.idempotencyKey())));

        String longest = "p".repeat(64);
        assertThat(service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "m002-long-ok", null, longest, "lvl_m1_lower", "role_ops_check", "tpl_ops_check", "active")).positionCode()).isEqualTo(longest);
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
    private SupervisorReviewService m002ReviewService;

    private PositionRoleTemplate m002HighRiskFixture(String userId, String merchantId, String highRiskLevel) {
        accessRepository.markRequiresApproval(M002_CODE, "high");
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        m002Reviews = reviewRepository;
        reviewRepository.superiors.put("usr_admin", "usr_boss");
        m002ReviewService = supervisorReviewService(reviewRepository); // M002-F40：通用主管复核入口与 reviewGrant 共用同一复核服务（同一幂等存储）
        service = newAccessService(true, m002ReviewService);
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

    // ── M002 第 10 轮（M002-F34、F37 为操作员 2026-09-30 裁决；F35、F36 为技术不变量） ─────────────────────────────────

    // M002-F34（操作员 2026-09-30 裁决，治理地图第 703 行「其他人不能给最高权限账号设置显式拒绝。」）：平台管理员、同组织持 access.role.assign 的员工
    // 给最高权限账号设显式拒绝一律 403，停用中的最高权限账号作目标同样被拒，不写拒绝行、写失败留痕；最高权限本人会话给自己设、解除照常，第二轮仍可；
    // 对其他员工设拒绝不变。
    @Test
    void othersCannotSetAnExplicitDenyOnTheMasterAuthorityAccount() {
        accessRepository.addPermission("access.role.assign", "high", true);
        String master = SessionIssuer.MASTER_USER_ID;
        accessRepository.users.put(master, m002MasterAccount("active"));
        accessRepository.users.put("usr_m002_f34_assigner", new AccessUser("usr_m002_f34_assigner", "usr_m002_f34_assigner", "staff",
                "platform-company", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.roles.put("role_m002_f34_assign", new RoleRecord("role_m002_f34_assign", "assign", null, "project", "active",
                Instant.parse("2026-06-14T00:00:00Z"), List.of("access.role.assign"), null));
        accessRepository.userRoles.put("usr_m002_f34_assigner", new LinkedHashSet<>(Set.of("role_m002_f34_assign")));
        accessRepository.users.put("usr_m002_f34_plain", new AccessUser("usr_m002_f34_plain", "usr_m002_f34_plain", "staff", "seed-org",
                null, "active", Instant.parse("2026-06-14T00:00:00Z")));

        m002MasterDenyRejected(master, "m002-f34-admin"); // setUp 的平台管理员会话
        authenticateAsOrgUser("usr_m002_f34_assigner", "f34-assigner", "platform-company");
        m002MasterDenyRejected(master, "m002-f34-staff");
        accessRepository.users.put(master, m002MasterAccount("disabled"));
        authenticateAsPlatformAdmin("seed-org");
        m002MasterDenyRejected(master, "m002-f34-disabled");
        accessRepository.users.put(master, m002MasterAccount("active"));

        authenticateAsMasterAuthority();
        for (int round = 1; round <= 2; round++) {
            service.setUserPermissionOverrides(master, new UserPermissionOverrideRequest(
                    "m002-f34-self-set-" + round, null, List.of("access.role.assign"), "deny", "本人停权"));
            assertThat(overrideSources(master, "access.role.assign", AccessRepository.Effect.DENY))
                    .containsExactly(AccessRepository.Source.USER_OVERRIDE);
            service.removeUserPermissionOverrides(master, m002RemoveDeny("m002-f34-self-remove-" + round, "access.role.assign"));
            assertThat(overrideSources(master, "access.role.assign", AccessRepository.Effect.DENY)).isEmpty();
        }

        authenticateAsPlatformAdmin("seed-org");
        service.setUserPermissionOverrides("usr_m002_f34_plain", new UserPermissionOverrideRequest(
                "m002-f34-plain", null, List.of("access.role.assign"), "deny", "停权"));
        assertThat(overrideSources("usr_m002_f34_plain", "access.role.assign", AccessRepository.Effect.DENY))
                .containsExactly(AccessRepository.Source.USER_OVERRIDE);
    }

    private AccessUser m002MasterAccount(String status) {
        return new AccessUser(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                "platform-company", null, status, Instant.parse("2026-06-14T00:00:00Z"));
    }

    private void m002MasterDenyRejected(String master, String key) {
        assertThatThrownBy(() -> service.setUserPermissionOverrides(master, new UserPermissionOverrideRequest(
                key, null, List.of("access.role.assign"), "deny", "给最高权限账号设拒绝")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(403);
                    assertThat(e.code()).isEqualTo("MASTER_AUTHORITY_DENY_FORBIDDEN");
                    assertThat(e.getMessage()).contains("其他人不能给最高权限账号设置显式拒绝");
                });
        assertThat(overrideSources(master, "access.role.assign", AccessRepository.Effect.DENY)).isEmpty();
        assertThat(auditEventRepository.events).filteredOn(event -> key.equals(event.idempotencyKey()))
                .extracting(AuditEvent::eventName, AuditEvent::eventResult, AuditEvent::businessId)
                .containsExactly(org.assertj.core.api.Assertions.tuple("access.user_permission_override.set_failed", "failure", master));
    }

    // M002-F35（治理地图第 656 行「权限的授予、收回、到期、单次消耗、失效全部留痕」）：映射停用时挂在该映射上的每名员工各留一条
    // access.user_position.derivation_revoked（员工、映射、原岗位派生角色、被撤销的岗位派生套用、原因 inactive、操作人），有挂起的另有
    // high_risk_voided；再次保存为停用、再启用、原样重存都不再记收回（第二次）；映射改职级后员工找不到映射，逐人记原因 no_mapping。
    @Test
    void revokingPositionDerivationLeavesOneTrailPerEmployee() {
        List<String> users = List.of("usr_m002_f35_a", "usr_m002_f35_b");
        PositionRoleTemplate mapping = m002HighRiskFixture(users.get(0), null, "lvl_m1_lower");
        accessRepository.users.put(users.get(1), new AccessUser(users.get(1), users.get(1), "staff", "seed-org", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        for (String user : users) {
            service.assignUserPosition(new UserPositionAssignmentRequest(
                    "m002-f35-assign-" + user, null, user, "ops_manager", "lvl_m1_lower", null, null, null));
        }
        Map<String, List<String>> applications = m002PositionApplications(users);

        service.updatePositionRoleTemplate(mapping.id(), m002Mapping("m002-f35-off", "role_ops_check", "tpl_ops_check", "inactive"));
        m002AssertRevocations(users, mapping.id(), applications, "inactive");
        assertThat(auditEventRepository.events).filteredOn(event -> "access.user_position.high_risk_voided".equals(event.eventName()))
                .extracting(AuditEvent::businessId).containsAll(users);
        assertThat(users).allSatisfy(user -> assertThat(accessRepository.positionRoles.get(user)).isNull());

        service.updatePositionRoleTemplate(mapping.id(), m002Mapping("m002-f35-off-again", "role_ops_check", "tpl_ops_check", "inactive"));
        service.updatePositionRoleTemplate(mapping.id(), m002Mapping("m002-f35-on", "role_ops_check", "tpl_ops_check", "active"));
        service.updatePositionRoleTemplate(mapping.id(), m002Mapping("m002-f35-resave", "role_ops_check", "tpl_ops_check", "active"));
        assertThat(m002RevocationEvents()).hasSize(users.size());

        applications = m002PositionApplications(users);
        service.updatePositionRoleTemplate(mapping.id(), new PositionRoleTemplateRequest(
                "m002-f35-move", null, "ops_manager", "lvl_m10_upper", "role_ops_check", "tpl_ops_check", "active"));
        m002AssertRevocations(users, mapping.id(), applications, "no_mapping");
        assertThat(m002RevocationEvents()).hasSize(users.size() * 2);
        assertThat(users).allSatisfy(user -> assertThat(accessRepository.positionMappings.get(user)).isNull());
    }

    private Map<String, List<String>> m002PositionApplications(List<String> users) {
        Map<String, List<String>> applications = new LinkedHashMap<>();
        for (String user : users) {
            List<String> ids = accessRepository.findPositionApplications(user).stream()
                    .filter(application -> "active".equals(application.status())).map(PermissionTemplateApplication::id).toList();
            assertThat(ids).as("夹具：%s 须有生效中的岗位派生套用", user).isNotEmpty();
            applications.put(user, ids);
        }
        return applications;
    }

    private List<AuditEvent> m002RevocationEvents() {
        return auditEventRepository.events.stream()
                .filter(event -> "access.user_position.derivation_revoked".equals(event.eventName())).toList();
    }

    private void m002AssertRevocations(List<String> users, String mappingId, Map<String, List<String>> applications, String reason) {
        List<AuditEvent> trail = m002RevocationEvents().stream().filter(event -> reason.equals(event.metadata().get("reason"))).toList();
        assertThat(trail).extracting(AuditEvent::businessId).containsExactlyInAnyOrderElementsOf(users);
        for (AuditEvent event : trail) {
            assertThat(event.eventResult()).isEqualTo("success");
            assertThat(event.businessType()).isEqualTo("user_position_assignment");
            assertThat(event.metadata())
                    .containsEntry("user_id", event.businessId())
                    .containsEntry("mapping_id", mappingId)
                    .containsEntry("role_id", "role_ops_check")
                    .containsEntry("application_ids", String.join(",", applications.get(event.businessId())))
                    .containsEntry("operator_id", "usr_admin");
            assertThat(accessRepository.templateApplications).filteredOn(a -> applications.get(event.businessId()).contains(a.id()))
                    .extracting(PermissionTemplateApplication::status).containsOnly("revoked");
        }
    }

    // M002-F36（契约 RoleAssignmentRequest.user_id / role_id maxLength 64；失败留痕 business_id 列宽 128）：分配、撤销角色的 user_id、role_id 与员工
    // 覆盖设置、解除的路径用户编号去空白后 65 字、129 字一律 400 VALIDATION_FAILED，发生在锁员工行、查库与写失败留痕之前（不锁任何员工行、无任何
    // 带该键的事件）；64 字不存在的编号（含首尾空白）仍 404，失败留痕记去空白后的编号。
    @Test
    void overlongTargetIdsAreRejectedWith400BeforeAnyLookup() {
        accessRepository.lockOrder.clear();
        int attempt = 0;
        for (String id : List.of("u".repeat(65), "u".repeat(129), " " + "u".repeat(65) + " ")) {
            String n = String.valueOf(++attempt);
            Map<String, org.assertj.core.api.ThrowableAssert.ThrowingCallable> entries = new LinkedHashMap<>();
            entries.put("m002-f36-assign-user-" + n, () -> service.assignUserRole(
                    new RoleAssignmentRequest("m002-f36-assign-user-" + n, null, id, "explicit_admin_grants")));
            entries.put("m002-f36-assign-role-" + n, () -> service.assignUserRole(
                    new RoleAssignmentRequest("m002-f36-assign-role-" + n, null, "usr_admin", id)));
            entries.put("m002-f36-unassign-user-" + n, () -> service.unassignUserRole(
                    new RoleAssignmentRequest("m002-f36-unassign-user-" + n, null, id, "explicit_admin_grants")));
            entries.put("m002-f36-unassign-role-" + n, () -> service.unassignUserRole(
                    new RoleAssignmentRequest("m002-f36-unassign-role-" + n, null, "usr_admin", id)));
            entries.put("m002-f36-override-set-" + n, () -> service.setUserPermissionOverrides(id, new UserPermissionOverrideRequest(
                    "m002-f36-override-set-" + n, null, List.of(M002_VIEW), "deny", "停权")));
            entries.put("m002-f36-override-remove-" + n, () -> service.removeUserPermissionOverrides(id, new UserPermissionOverrideRequest(
                    "m002-f36-override-remove-" + n, null, List.of(M002_VIEW), "deny", "解除")));
            entries.forEach((key, entry) -> {
                assertThatThrownBy(entry).as(key).isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(400);
                    assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                    assertThat(e.getMessage()).contains("不能超过 64 个字符");
                });
                assertThat(auditEventRepository.events).as(key).noneMatch(event -> key.equals(event.idempotencyKey()));
            });
        }
        assertThat(accessRepository.lockOrder).isEmpty();

        String id64 = "u".repeat(64);
        attempt = 0;
        for (String id : List.of(id64, "  " + id64 + "  ")) {
            String n = String.valueOf(++attempt);
            Map<String, org.assertj.core.api.ThrowableAssert.ThrowingCallable> entries = new LinkedHashMap<>();
            entries.put("access.role.assign_failed", () -> service.assignUserRole(
                    new RoleAssignmentRequest("m002-f36-404-assign-" + n, null, id, "explicit_admin_grants")));
            entries.put("access.role.unassign_failed", () -> service.unassignUserRole(
                    new RoleAssignmentRequest("m002-f36-404-unassign-" + n, null, id, "explicit_admin_grants")));
            entries.put("access.user_permission_override.set_failed", () -> service.setUserPermissionOverrides(id,
                    new UserPermissionOverrideRequest("m002-f36-404-set-" + n, null, List.of(M002_VIEW), "deny", "停权")));
            entries.put("access.user_permission_override.remove_failed", () -> service.removeUserPermissionOverrides(id,
                    new UserPermissionOverrideRequest("m002-f36-404-remove-" + n, null, List.of(M002_VIEW), "deny", "解除")));
            entries.forEach((eventName, entry) -> {
                int before = auditEventRepository.events.size();
                assertThatThrownBy(entry).as(eventName).isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(404);
                    assertThat(e.code()).isEqualTo("USER_NOT_FOUND");
                });
                assertThat(auditEventRepository.events.subList(before, auditEventRepository.events.size()))
                        .extracting(AuditEvent::eventName, AuditEvent::businessId)
                        .containsExactly(org.assertj.core.api.Assertions.tuple(eventName, id64));
            });
        }
    }

    // M002-F37（操作员 2026-09-30 裁决，治理地图第 651 行「员工单独授予、解除员工显式拒绝、岗位模板高危权限释放这三类授权申请，只能由具备分配/取消
    // 分配角色权限的人发起。」）：无 access.role.assign 的普通员工发起三类申请（内容均有效）一律 403，不建单、不开复核单，第二轮仍被拒；
    // 同一员工的临时权限申请照常进主管链（第 659 行不变）；持码的平台管理员三类照常建单。
    // M002-F42：资格改在入口 AccessController.applyGrant 与申请权限一次核验，本用例按入口断言——持申请权限的员工经入口发起三类申请
    // （类型带大小写与首尾空白同样）在进服务层之前即被拒（F37 原提示语），该幂等键不留任何服务层痕迹；临时权限申请与管理员三类申请均经入口建单。
    @Test
    void onlyRoleAssignHoldersInitiateTheOverrideAndReleaseRequests() {
        String target = "usr_m002_f37_target";
        m002HighRiskFixture(target, null, "lvl_m1_lower");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-f37-assign", null, target, "ops_manager", "lvl_m1_lower", null, null, null));
        authenticateAsOrgUser("usr_boss", "boss", "seed-org");
        service.reviewGrant(m002PendingHold().id(), m002Reject("m002-f37-reject")); // 驳回后同一挂起可再次申请释放
        accessRepository.saveUserPermissionOverrides(target, List.of(M002_CODE), AccessRepository.Effect.DENY, "员工显式拒绝", "usr_admin");
        accessRepository.users.put("usr_requester", new AccessUser(
                "usr_requester", "requester", "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        // M002-F42：入口核验按字典判码——申请权限与真库 V394 同档（low、无需审批），分配角色权限同 2832 行登记；员工经角色持申请权限。
        accessRepository.addPermission("access.grant_request.apply", "low", false);
        accessRepository.addPermission("access.role.assign", "high", true);
        accessRepository.roles.put("role_m002_f37_apply", new RoleRecord("role_m002_f37_apply", "apply", null, "project", "active",
                Instant.parse("2026-06-14T00:00:00Z"), List.of("access.grant_request.apply"), null));
        accessRepository.userRoles.put("usr_requester", new LinkedHashSet<>(Set.of("role_m002_f37_apply")));
        AccessController entry = new AccessController(service, authorizationService);
        int requests = accessRepository.grantRequests.size();
        int reviews = m002Reviews.reviews.size();
        List<String> types = List.of("user_permission_grant", "user_permission_deny_removal", "position_high_risk_release");

        authenticateAsOrgUser("usr_requester", "requester", "seed-org");
        for (int round = 1; round <= 2; round++) {
            for (String type : List.of(types.get(0), types.get(1), types.get(2), "  User_Permission_DENY_Removal ")) {
                String key = "m002-f37-staff-" + type.trim() + "-" + round;
                assertThatThrownBy(() -> entry.applyGrant(m002Apply(key, type, target, List.of(M002_CODE))))
                        .as(key).isInstanceOfSatisfying(ApiException.class, e -> {
                            assertThat(e.status().value()).isEqualTo(403);
                            assertThat(e.code()).isEqualTo("PERMISSION_DENIED");
                            assertThat(e.getMessage()).isEqualTo(
                                    "员工单独授予、解除员工显式拒绝、岗位模板高危权限释放这三类授权申请，只能由具备分配/取消分配角色权限的人发起");
                        });
                assertThat(auditEventRepository.events).as(key).filteredOn(event -> key.equals(event.idempotencyKey())).isEmpty();
            }
        }
        assertThat(accessRepository.grantRequests).hasSize(requests);
        assertThat(m002Reviews.reviews).hasSize(reviews);

        AccessGrantRequestRecord temporary = entry.applyGrant(new AccessGrantApplyRequest(
                "m002-f37-staff-temporary", "M002", List.of(), "temporary_permission", "usr_requester", null, null, null, null, null, null,
                List.of(M002_VIEW), Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-15T00:00:00Z"), false, "临时顶班")).data();
        assertThat(temporary.status()).isEqualTo("pending");

        authenticateAsPlatformAdmin("seed-org");
        for (String type : types) {
            AccessGrantRequestRecord applied = entry.applyGrant(m002Apply("m002-f37-admin-" + type, type, target, List.of(M002_CODE))).data();
            assertThat(applied.status()).as(type).isEqualTo("pending");
            assertThat(applied.requestType()).isEqualTo(type);
        }
    }

    // ── M002 第 11 轮（M002-F38；依据治理地图第 653 行「所有业务数据默认按项目隔离」「数据的项目与商户隔离不提供关闭开关」与七类红线④，技术推导）──

    // M002-F38：普通员工（无商户）给其他项目的员工提交岗位申请（user_position，映射含须审批码），建单前即 403 CROSS_ORG_FORBIDDEN（中文提示）：
    // 不建授权单、不开复核单，写失败留痕；目标原有岗位、职级、岗位派生角色与映射、直接角色、模板套用、覆盖行（含挂起）不变。
    @Test
    void positionGrantRequestForATargetInAnotherProjectIsRejectedBeforeAnyRequestIsCreated() {
        m002F38RejectedOutOfScope("usr_requester", null, "org_b", null, "CROSS_ORG_FORBIDDEN", "不能为其他组织的用户分配权限");
    }

    // M002-F38：带商户的员工给同项目其他商户的员工提交岗位申请，建单前即 403 CROSS_MERCHANT_FORBIDDEN（中文提示），其余同上。
    @Test
    void positionGrantRequestForATargetOfAnotherMerchantInTheSameProjectIsRejectedBeforeAnyRequestIsCreated() {
        m002F38RejectedOutOfScope("usr_m002_f38_merchant_a", "merchant_a", "seed-org", "merchant_b", "CROSS_MERCHANT_FORBIDDEN", "不能操作其他商户的用户");
    }

    private void m002F38RejectedOutOfScope(String applicant, String applicantMerchant, String targetOrg, String targetMerchant, String code,
                                           String message) {
        m002HighRiskFixture("usr_m002_f38_seed", null, "lvl_m1_lower");
        m002ViewMapping("ops_viewer");
        String target = "usr_m002_f38_target";
        accessRepository.users.put(target, new AccessUser(target, target, "staff", targetOrg, targetMerchant, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        m002Reviews.superiors.put(applicant, "usr_boss"); // 修复前可建单、开复核单（缺陷复现），不因申请人没有上级链而失败
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-f38-seat", null, target, "ops_viewer", "lvl_m1_lower", null, null, null));
        List<Object> before = m002TargetFacts(target);
        int requests = accessRepository.grantRequests.size();
        int reviews = m002Reviews.reviews.size();

        if (applicantMerchant == null) {
            authenticateAsOrgUser(applicant, applicant, "seed-org");
        } else {
            authenticateAsMerchantUser(applicant, applicant, "seed-org", applicantMerchant);
        }
        assertThatThrownBy(() -> service.applyGrant(new AccessGrantApplyRequest(
                "m002-f38-apply", "M002", List.of(), "user_position", target, null, "ops_manager", "lvl_m1_lower",
                null, null, null, null, null, null, false, "岗位申请")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(403);
                    assertThat(e.code()).isEqualTo(code);
                    assertThat(e.getMessage()).contains(message);
                });
        assertThat(auditEventRepository.events).filteredOn(event -> "m002-f38-apply".equals(event.idempotencyKey()))
                .extracting(AuditEvent::eventName).containsExactly("access.grant_request.apply_failed");
        assertThat(accessRepository.grantRequests).hasSize(requests);
        assertThat(m002Reviews.reviews).hasSize(reviews);
        assertThat(m002TargetFacts(target)).isEqualTo(before);
        assertThat(accessRepository.userPositionAssignments.get(target).positionCode()).isEqualTo("ops_viewer");
        assertThat(overrideSources(target, M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
    }

    // M002-F38 正向对照：同项目普通员工（无商户）给本项目员工的岗位申请照常建单，由其他组织的链上终审人批准后照常落地（落地不按终审人组织判定）；
    // 平台管理员给其他项目员工的岗位申请照常建单并落地。
    @Test
    void positionGrantRequestsWithinScopeOrByThePlatformAdminStillLandWithACrossOrganizationFinalApprover() {
        String sameOrg = "usr_m002_f38_same";
        String otherOrg = "usr_m002_f38_admin_target";
        m002HighRiskFixture(sameOrg, null, "lvl_m1_lower");
        accessRepository.users.put(otherOrg, new AccessUser(otherOrg, otherOrg, "staff", "org_b", null, "active", Instant.parse("2026-06-14T00:00:00Z")));

        authenticateAsOrgUser("usr_requester", "requester", "seed-org");
        AccessGrantRequestRecord peer = service.applyGrant(new AccessGrantApplyRequest(
                "m002-f38-peer-apply", "M002", List.of(), "user_position", sameOrg, null, "ops_manager", "lvl_m1_lower",
                null, null, null, null, null, null, false, "岗位申请"));
        assertThat(peer.status()).isEqualTo("pending");
        authenticateAsPlatformAdmin("seed-org");
        AccessGrantRequestRecord admin = service.applyGrant(new AccessGrantApplyRequest(
                "m002-f38-admin-apply", "M002", List.of(), "user_position", otherOrg, null, "ops_manager", "lvl_m1_lower",
                null, null, null, null, null, null, false, "岗位申请"));
        assertThat(admin.status()).isEqualTo("pending");

        assertThat(service.reviewGrant(peer.id(), m002Approve("m002-f38-peer-first")).status()).isEqualTo("pending"); // 链首 usr_admin
        authenticateAsOrgUser("usr_boss", "boss", "org_b");
        assertThat(service.reviewGrant(peer.id(), m002Approve("m002-f38-peer-final")).status()).isEqualTo("applied");
        assertThat(service.reviewGrant(admin.id(), m002Approve("m002-f38-admin-final")).status()).isEqualTo("applied");
        authenticateAsPlatformAdmin("seed-org");
        for (String target : List.of(sameOrg, otherOrg)) {
            assertThat(accessRepository.userPositionAssignments.get(target).positionCode()).as(target).isEqualTo("ops_manager");
            assertThat(accessRepository.positionRoles.get(target)).as(target).isEqualTo("role_ops_check");
            assertThat(authorizationService.effectivePermissionsForUser(target).grants()).as(target).contains(M002_CODE);
        }
    }

    // M002-F38：目标员工的岗位行、职级与薪资行、岗位派生角色与所挂映射、直接角色、模板套用与覆盖行（含挂起），用于断言范围拒绝后原样不变。
    private List<Object> m002TargetFacts(String userId) {
        return java.util.Arrays.asList(accessRepository.userPositionAssignments.get(userId), accessRepository.staffLevelAssignments.get(userId),
                accessRepository.positionRoles.get(userId), accessRepository.positionMappings.get(userId),
                Set.copyOf(accessRepository.userRoles.getOrDefault(userId, Set.of())),
                accessRepository.templateApplications.stream().filter(application -> userId.equals(application.targetId())).toList(),
                accessRepository.overrides.stream().filter(override -> userId.equals(override.userId())).toList());
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
        // M001-F17：放宽闸只管仍有员工在用的模板，两个模板先各套给一名员工，409 的前提才成立。
        PermissionTemplate bounded = service.createPermissionTemplate(new PermissionTemplateRequest(
                "create-bounded", "seed", List.of(), "Bounded temporary", "temporary", null,
                List.of("ops.production_readiness.view"), "project",
                Instant.parse("2027-01-01T00:00:00Z"), Instant.parse("2027-01-02T00:00:00Z"), false));
        accessRepository.applyTemplate(bounded.id(), List.of("usr_f07_bounded"),
                Instant.parse("2027-01-01T00:00:00Z"), Instant.parse("2027-01-02T00:00:00Z"), false, "usr_admin");

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
        accessRepository.applyTemplate(singleUse.id(), List.of("usr_f07_single"), null, null, true, "usr_admin");
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
                () -> service.assignStaffLevel(new StaffLevelAssignmentRequest(
                        "level-lock", "l", "usr_ghost", "lvl_m1_lower", null, null, null)), // M002-F41
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

    // ── M001 第 4 轮（M001-F16～F21）：服务层回归 ─────────────────────────────

    // M001-F16（PGM 权限规则「数据的项目与商户隔离不提供关闭开关」；操作员 2026-09-26 更正段「模板修改入口对带商户归属的操作人逐人比对在用员工的商户，
    // 不符即 403」）：在用员工含其他商户员工时，带商户归属的操作人修改被 403 拒绝，模板、条目、套用与逐人留痕都不动；在用员工的员工行在模板行之前锁住；
    // 在用员工全在本商户时照常修改并逐人留痕；无商户归属的项目操作人与平台管理员不受此限；列表、新建、复制不加「商户账号一律拒绝」（句 3 已撤回）。
    @Test
    void merchantOperatorCannotEditATemplateHeldByAnotherMerchantsEmployee() {
        accessRepository.users.put("usr_f16_a1", new AccessUser("usr_f16_a1", "f16-a1", "staff", "seed-org", "merchant_a", "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        seedTemplate("tpl_f16", "seed-org", null, "project", "ops.production_readiness.view");
        accessRepository.applyTemplate("tpl_f16", List.of("usr_f16_a1"), null, null, false, "usr_admin");

        authenticateAsMerchantUser("usr_f16_mb", "f16-mb", "seed-org", "merchant_b");
        accessRepository.lockOrder.clear();
        assertThatThrownBy(() -> service.updatePermissionTemplate("tpl_f16", baseTemplateRequest("f16-edit-b")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(403);
                    assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN");
                });
        assertThat(accessRepository.lockOrder).containsSubsequence("user:usr_f16_a1", "edit:tpl_f16");
        assertThat(accessRepository.templates.get("tpl_f16").templateName()).isEqualTo("tpl_f16");
        assertThat(accessRepository.items.get("tpl_f16")).extracting(PermissionTemplateItem::permissionCode)
                .containsExactly("ops.production_readiness.view");
        assertThat(accessRepository.templateApplications).singleElement()
                .satisfies(application -> assertThat(application.status()).isEqualTo("active"));
        assertThat(accessRepository.findPermissionEffects("usr_f16_a1", Instant.now()))
                .extracting(AccessRepository.PermissionEffect::permissionCode).containsExactly("ops.production_readiness.view");
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).doesNotContain("permission_template.effect_changed");
        assertThat(service.listPermissionTemplates().items()).extracting(PermissionTemplate::id).contains("tpl_f16");
        assertThat(service.createPermissionTemplate(baseTemplateRequest("f16-create-b")).ownerScopeId()).isEqualTo("seed-org");
        assertThat(service.copyPermissionTemplate("tpl_f16", copyRequest("f16-copy-b", null, null)).sourceTemplateId())
                .isEqualTo("tpl_f16");

        authenticateAsMerchantUser("usr_f16_ma", "f16-ma", "seed-org", "merchant_a");
        service.updatePermissionTemplate("tpl_f16", baseTemplateRequest("f16-edit-a"));
        assertThat(accessRepository.items.get("tpl_f16")).isEmpty();
        assertThat(auditEventRepository.events).filteredOn(event -> "permission_template.effect_changed".equals(event.eventName()))
                .extracting(AuditEvent::businessId).containsExactly("usr_f16_a1");

        authenticateAsOrgUser("usr_f16_org", "f16-org", "seed-org");
        assertThat(service.updatePermissionTemplate("tpl_f16", namedRequest("f16-edit-org", "F16 项目操作人改名", "base")).templateName())
                .isEqualTo("F16 项目操作人改名");
        authenticateAsPlatformAdmin("seed-org");
        service.updatePermissionTemplate("tpl_f16", codesRequest("f16-edit-admin", List.of("ops.production_readiness.view")));
        assertThat(accessRepository.items.get("tpl_f16")).extracting(PermissionTemplateItem::permissionCode)
                .containsExactly("ops.production_readiness.view");
    }

    // M001-F19（操作员确认句 1「复制出来的是一个新模板，权限、有效期、单次使用限制等都可以重新设定，按新建模板的规则校验」；
    // 裁决说明「复制＝以源模板为初值新建，未填项沿用源模板……源模板不存在仍 404」）：两类主体复制不存在的源都 404、不建行、不写复制成功事件；
    // 只给名称的复制（前端复制按钮同形）得到与源模板同类型、同窗口、同单次、同权限码的新模板，响应、再次查询与套用后的有效权限一致；
    // 名称不填、类型空白同样沿用源模板；请求里填了的项按填的（换权限码、数据范围、重设生效、延长到期、取消单次），源模板不变，源模板随后修改不影响复制件；
    // 校验与新建一样：类型闭集、临时须有到期或单次（含库里已有的无期限临时源模板只给名称复制）、未知权限码 400。
    @Test
    void copyIsANewTemplateSeededFromTheSourceAndAMissingSourceIsNotFound() {
        int templatesBefore = accessRepository.templates.size();
        assertThatThrownBy(() -> service.copyPermissionTemplate("tpl_f19_missing", copyRequest("f19-missing-admin", null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(404);
                    assertThat(e.code()).isEqualTo("TEMPLATE_NOT_FOUND");
                });
        authenticateAsOrgUser("usr_f19_org", "f19-org", "seed-org");
        assertThatThrownBy(() -> service.copyPermissionTemplate("tpl_f19_missing", copyRequest("f19-missing-org", null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("TEMPLATE_NOT_FOUND"));
        authenticateAsPlatformAdmin("seed-org");
        assertThat(accessRepository.templates).hasSize(templatesBefore);
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).doesNotContain("permission_template.copied");

        Instant from = Instant.parse("2099-01-01T00:00:00Z");
        Instant until = Instant.parse("2099-01-31T00:00:00Z");
        PermissionTemplate source = service.createPermissionTemplate(new PermissionTemplateRequest(
                "f19-source", "seed", List.of(), "F19 临时模板", "temporary", null,
                List.of("ops.production_readiness.view"), "project", from, until, true));
        PermissionTemplate copy = service.copyPermissionTemplate(source.id(), copyRequest("f19-copy", null, null));
        assertThat(copy.templateType()).isEqualTo("temporary");
        assertThat(copy.validFrom()).isEqualTo(from);
        assertThat(copy.validUntil()).isEqualTo(until);
        assertThat(copy.singleUse()).isTrue();
        assertThat(copy.sourceTemplateId()).isEqualTo(source.id());
        assertThat(copy.permissionCodes()).containsExactly("ops.production_readiness.view");
        assertThat(accessRepository.templates.get(copy.id()).validFrom()).isEqualTo(from);
        assertThat(accessRepository.items.get(copy.id())).extracting(PermissionTemplateItem::permissionCode)
                .containsExactly("ops.production_readiness.view");

        accessRepository.createUser(new AccessUser("usr_f19", "f19", "staff", "seed-org", null, "active", Instant.now()));
        PermissionTemplateApplyResult applied = service.applyPermissionTemplate(copy.id(), new PermissionTemplateApplyRequest(
                "f19-apply", "apply", List.of(), copy.id(), List.of("usr_f19"), "append"));
        assertThat(applied.applications()).singleElement().satisfies(application -> {
            assertThat(application.validUntil()).isEqualTo(until);
            assertThat(application.singleUse()).isTrue();
        });
        assertThat(accessRepository.findPermissionEffects("usr_f19", Instant.now()))
                .filteredOn(effect -> applied.applications().get(0).id().equals(effect.sourceId()))
                .extracting(AccessRepository.PermissionEffect::permissionCode).containsExactlyElementsOf(copy.permissionCodes());

        // 填了的按填的：换权限码、数据范围、重设生效时间、延长到期、取消单次；源模板不变。
        Instant resetFrom = Instant.parse("2099-01-02T00:00:00Z");
        Instant later = until.plusSeconds(86400);
        PermissionTemplate reset = service.copyPermissionTemplate(source.id(), new PermissionTemplateRequest(
                "f19-reset", "后台复制权限模板", List.of(), "F19 重设", null, null,
                List.of("governance.supervisor_review.view"), "merchant", resetFrom, later, false));
        assertThat(reset.templateType()).isEqualTo("temporary");
        assertThat(reset.validFrom()).isEqualTo(resetFrom);
        assertThat(reset.validUntil()).isEqualTo(later);
        assertThat(reset.singleUse()).isFalse();
        assertThat(reset.permissionCodes()).containsExactly("governance.supervisor_review.view");
        assertThat(accessRepository.items.get(reset.id())).extracting(item -> item.permissionCode() + "=" + item.dataScope())
                .containsExactly("governance.supervisor_review.view=merchant");
        assertThat(accessRepository.templates.get(source.id())).satisfies(template -> {
            assertThat(template.validFrom()).isEqualTo(from);
            assertThat(template.validUntil()).isEqualTo(until);
            assertThat(template.singleUse()).isTrue();
        });
        PermissionTemplate unnamed = service.copyPermissionTemplate(source.id(), new PermissionTemplateRequest(
                "f19-unnamed", "后台复制权限模板", List.of(), null, " ", null, null, null, null, null, null));
        assertThat(unnamed.templateName()).isEqualTo("F19 临时模板");
        assertThat(unnamed.templateType()).isEqualTo("temporary");
        assertThat(unnamed.validFrom()).isEqualTo(from);

        // 源模板随后修改（换权限码），复制件不变。
        service.updatePermissionTemplate(source.id(), new PermissionTemplateRequest(
                "f19-source-edit", "seed", List.of(), "F19 临时模板", "temporary", null,
                List.of("ops.production_readiness.check"), "project", from, until, true));
        assertThat(accessRepository.items.get(copy.id())).extracting(PermissionTemplateItem::permissionCode)
                .containsExactly("ops.production_readiness.view");

        // 校验与新建完全一样。
        PermissionTemplate baseSource = service.createPermissionTemplate(baseTemplateRequest("f19-base-source"));
        assertThat(service.copyPermissionTemplate(baseSource.id(), copyRequest("f19-base-copy", null, null)).templateType()).isEqualTo("base");
        accessRepository.templates.put("tpl_f19_unbounded", new PermissionTemplate(
                "tpl_f19_unbounded", "tpl_f19_unbounded", "F19 无期限临时", null, "temporary",
                null, "platform", null, "merge", null, null, false, "active",
                null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.items.put("tpl_f19_unbounded", List.of());
        int beforeInvalid = accessRepository.templates.size();
        for (org.assertj.core.api.ThrowableAssert.ThrowingCallable write : List.<org.assertj.core.api.ThrowableAssert.ThrowingCallable>of(
                () -> service.copyPermissionTemplate(baseSource.id(), new PermissionTemplateRequest(
                        "f19-type-copy", "seed", List.of(), "F19", "copy", null, null, null, null, null, null)),
                () -> service.copyPermissionTemplate(baseSource.id(), new PermissionTemplateRequest(
                        "f19-unbounded-temporary", "seed", List.of(), "F19", "temporary", null, null, null, null, null, false)),
                () -> service.copyPermissionTemplate("tpl_f19_unbounded", copyRequest("f19-unbounded-source", null, null)))) {
            assertThatThrownBy(write).isInstanceOfSatisfying(ApiException.class, e -> {
                assertThat(e.status().value()).isEqualTo(400);
                assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
            });
        }
        assertThatThrownBy(() -> service.copyPermissionTemplate(baseSource.id(), new PermissionTemplateRequest(
                "f19-unknown", "seed", List.of(), "F19", null, null, List.of("f19.unknown.view"), null, null, null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(400);
                    assertThat(e.code()).isEqualTo("UNKNOWN_PERMISSION");
                });
        assertThat(accessRepository.templates).hasSize(beforeInvalid);
    }

    // M001-F17/F18（操作员确认句 2「模板已被套用且仍有员工在用时，收窄其有效期或改为单次使用，在用员工立即按收窄后的期限执行，
    // 并逐人记录改前改后的期限和操作人；放宽的，新建模板后另行套用」）：有人在用的模板组合收窄——生效推后、到期提前、改为单次——直接保存，
    // 在用员工的实际窗口立即取「套用当时窗口 ∩ 模板当前窗口」、单次取任一为单次；受影响员工一条 effect_changed 记改前改后的实际期限与操作人，
    // 实际期限没变的员工（套用当时窗口本就在收窄后的窗口内）不写；有人在用时放宽（到期推后、生效提前、取消单次）409，模板与套用行都不变。
    @Test
    void narrowingAnAppliedTemplateTakesEffectForHoldersImmediatelyAndWideningIsRejected() {
        Instant from = Instant.parse("2099-01-01T00:00:00Z");
        Instant until = Instant.parse("2099-01-31T00:00:00Z");
        Instant laterStart = Instant.parse("2099-01-03T00:00:00Z");
        Instant earlier = Instant.parse("2099-01-05T00:00:00Z");
        PermissionTemplate applied = service.createPermissionTemplate(windowRequest("f17-create-applied", from, until, false));
        accessRepository.createUser(new AccessUser("usr_f17", "f17", "staff", "seed-org", null, "active", Instant.now()));
        String applicationId = service.applyPermissionTemplate(applied.id(), new PermissionTemplateApplyRequest(
                "f17-apply", "apply", List.of(), applied.id(), List.of("usr_f17"), "append")).applications().get(0).id();
        // 第二名在用员工（定岗等入口写入的套用行）：套用当时的窗口 01-03～01-04、单次，本就在收窄后的窗口内，实际期限不变、不写事件。
        accessRepository.applyTemplate(applied.id(), List.of("usr_f17_b"), laterStart, earlier.minusSeconds(86400), true, "usr_admin");

        service.updatePermissionTemplate(applied.id(), windowRequest("f17-narrow", laterStart, earlier, true));

        assertThat(accessRepository.findPermissionEffects("usr_f17", Instant.now()))
                .filteredOn(effect -> applicationId.equals(effect.sourceId()))
                .singleElement().satisfies(effect -> {
                    assertThat(effect.validFrom()).isEqualTo(laterStart);
                    assertThat(effect.validUntil()).isEqualTo(earlier);
                    assertThat(effect.singleUse()).isTrue();
                });
        assertThat(auditEventRepository.events).filteredOn(event -> "permission_template.effect_changed".equals(event.eventName()))
                .singleElement().satisfies(event -> {
                    assertThat(event.businessId()).isEqualTo("usr_f17");
                    assertThat(event.metadata())
                            .containsEntry("application_id", applicationId)
                            .containsEntry("valid_from_before", from.toString())
                            .containsEntry("valid_from_after", laterStart.toString())
                            .containsEntry("valid_until_before", until.toString())
                            .containsEntry("valid_until_after", earlier.toString())
                            .containsEntry("single_use_before", false)
                            .containsEntry("single_use_after", true)
                            .containsEntry("added_permission_codes", List.of())
                            .containsEntry("removed_permission_codes", List.of())
                            .containsEntry("operator_id", "usr_admin");
                });

        for (PermissionTemplateRequest widening : List.of(
                windowRequest("f17-later-end", laterStart, until, true),
                windowRequest("f17-earlier-start", from, earlier, true),
                windowRequest("f17-drop-single", laterStart, earlier, false))) {
            assertThatThrownBy(() -> service.updatePermissionTemplate(applied.id(), widening))
                    .isInstanceOfSatisfying(ApiException.class, e -> {
                        assertThat(e.status().value()).isEqualTo(409);
                        assertThat(e.code()).isEqualTo("TEMPLATE_REAUTHORIZATION_REQUIRED");
                        assertThat(e.getMessage()).isEqualTo("模板已被套用且仍有员工在用，不能放宽有效期或取消单次使用限制；请新建模板或复制为新模板后另行套用");
                    });
        }
        assertThat(accessRepository.templates.get(applied.id())).satisfies(template -> {
            assertThat(template.validFrom()).isEqualTo(laterStart);
            assertThat(template.validUntil()).isEqualTo(earlier);
            assertThat(template.singleUse()).isTrue();
        });
        assertThat(accessRepository.templateApplications).filteredOn(application -> applicationId.equals(application.id()))
                .singleElement().satisfies(application -> {
                    assertThat(application.validUntil()).isEqualTo(until);
                    assertThat(application.singleUse()).isFalse();
                    assertThat(application.status()).isEqualTo("active");
                });
        assertThat(auditEventRepository.events).filteredOn(event -> "permission_template.effect_changed".equals(event.eventName())).hasSize(1);
    }

    // M001-F17/F18：纯期限收窄（新增截止）、纯单次收窄各给每名受影响在用员工写一条痕迹；同幂等请求重放不重复写；
    // 新截止已过时，已到期的套用在这次修改里就置为到期（无人查询也不等到期作业），放宽此后不再被已结束的套用挡住、套用也不复活。
    @Test
    void narrowingOnlyTheWindowOrOnlySingleUseRecordsEachHolderOnceAndLapsedHoldersExpireInTheSameEdit() {
        PermissionTemplate open = service.createPermissionTemplate(codesRequest("f18-open", List.of("ops.production_readiness.view")));
        accessRepository.applyTemplate(open.id(), List.of("usr_f18_a", "usr_f18_b"), null, null, false, "usr_admin");
        Instant until = Instant.parse("2099-02-01T00:00:00Z");
        PermissionTemplateRequest addUntil = new PermissionTemplateRequest("f18-add-until", "seed", List.of(), "F18 模板", "base", null,
                List.of("ops.production_readiness.view"), "project", null, until, false);
        service.updatePermissionTemplate(open.id(), addUntil);
        service.updatePermissionTemplate(open.id(), addUntil); // 同幂等键重放：返回原结果，不再写
        assertThat(effectChanged("f18-add-until")).hasSize(2).allSatisfy(event -> assertThat(event.metadata())
                .containsEntry("valid_until_before", "").containsEntry("valid_until_after", until.toString())
                .containsEntry("single_use_before", false).containsEntry("single_use_after", false)
                .containsEntry("operator_id", "usr_admin"));
        assertThat(effectChanged("f18-add-until")).extracting(AuditEvent::businessId).containsExactlyInAnyOrder("usr_f18_a", "usr_f18_b");

        service.updatePermissionTemplate(open.id(), new PermissionTemplateRequest("f18-single", "seed", List.of(), "F18 模板", "base", null,
                List.of("ops.production_readiness.view"), "project", null, until, true));
        assertThat(effectChanged("f18-single")).hasSize(2).allSatisfy(event -> assertThat(event.metadata())
                .containsEntry("single_use_before", false).containsEntry("single_use_after", true)
                .containsEntry("valid_until_before", until.toString()).containsEntry("valid_until_after", until.toString()));
        assertThat(accessRepository.findPermissionEffects("usr_f18_a", Instant.now()))
                .extracting(AccessRepository.PermissionEffect::singleUse).containsOnly(true);

        Instant past = Instant.now().minusSeconds(3600);
        service.updatePermissionTemplate(open.id(), new PermissionTemplateRequest("f18-past", "seed", List.of(), "F18 模板", "base", null,
                List.of("ops.production_readiness.view"), "project", null, past, true));
        assertThat(effectChanged("f18-past")).hasSize(2).allSatisfy(event -> assertThat(event.metadata())
                .containsEntry("valid_until_after", past.toString()));
        assertThat(accessRepository.templateApplications).filteredOn(application -> open.id().equals(application.templateId()))
                .hasSize(2).allSatisfy(application -> assertThat(application.status()).isEqualTo("expired"));
        assertThat(accessRepository.findPermissionEffects("usr_f18_a", Instant.now())).isEmpty();
        service.updatePermissionTemplate(open.id(), codesRequest("f18-widen-after-expiry", List.of("ops.production_readiness.view")));
        assertThat(accessRepository.templates.get(open.id()).validUntil()).isNull();
        assertThat(accessRepository.findPermissionEffects("usr_f18_a", Instant.now())).isEmpty();
    }

    // M001-F17：修改入口的 409 按原因分开写，每条给出的办法都走得通——放宽（模板已被套用且仍有员工在用）：只给名称复制，
    // 复制件与源模板同窗口、同单次，再修改复制件（没人在用，放宽不受限，仍按新建规则校验），源模板与在用员工不变；
    // 数据范围变化：新建或复制时填新的数据范围；含禁止规则：新建模板；未启用：新建或复制（复制不看源模板状态）。
    @Test
    void editConflictsNameTheirReasonAndTheSuggestedRemedyWorks() {
        Instant until = Instant.parse("2099-01-31T00:00:00Z");
        Instant later = until.plusSeconds(86400);
        Instant at = Instant.parse("2026-06-14T00:00:00Z");
        PermissionTemplate bounded = service.createPermissionTemplate(windowRequest("f17r-bounded", null, until, true));
        accessRepository.createUser(new AccessUser("usr_f17r", "f17r", "staff", "seed-org", null, "active", Instant.now()));
        service.applyPermissionTemplate(bounded.id(), new PermissionTemplateApplyRequest(
                "f17r-apply", "apply", List.of(), bounded.id(), List.of("usr_f17r"), "append"));
        assertThatThrownBy(() -> service.updatePermissionTemplate(bounded.id(), windowRequest("f17r-widen", null, later, true)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(409);
                    assertThat(e.getMessage()).isEqualTo("模板已被套用且仍有员工在用，不能放宽有效期或取消单次使用限制；请新建模板或复制为新模板后另行套用");
                });
        PermissionTemplate copy = service.copyPermissionTemplate(bounded.id(), copyRequest("f17r-widen-copy", null, null));
        assertThat(copy.validUntil()).isEqualTo(until);
        assertThat(copy.singleUse()).isTrue();
        assertThat(service.updatePermissionTemplate(copy.id(), windowRequest("f17r-copy-later", null, later, true)).validUntil())
                .isEqualTo(later);
        assertThat(service.updatePermissionTemplate(copy.id(), windowRequest("f17r-copy-multi", null, later, false)).singleUse())
                .isFalse();
        assertThat(service.updatePermissionTemplate(copy.id(), new PermissionTemplateRequest("f17r-copy-base", "seed", List.of(),
                "F17 基础", "base", null, List.of("ops.production_readiness.view"), "project", null, null, false)))
                .satisfies(template -> {
                    assertThat(template.templateType()).isEqualTo("base");
                    assertThat(template.validUntil()).isNull();
                    assertThat(template.singleUse()).isFalse();
                });
        assertThat(accessRepository.templates.get(bounded.id())).satisfies(template -> {
            assertThat(template.validUntil()).isEqualTo(until);
            assertThat(template.singleUse()).isTrue();
        });

        seedTemplate("tpl_f17r_scope", null, null, "complex", "ops.production_readiness.view");
        assertThatThrownBy(() -> service.updatePermissionTemplate("tpl_f17r_scope",
                codesRequest("f17r-scope", List.of("ops.production_readiness.view"))))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(409);
                    assertThat(e.getMessage()).isEqualTo("模板条目的数据范围不能直接修改；请新建模板或复制为新模板后另行套用");
                });
        PermissionTemplate scopeCopy = service.copyPermissionTemplate("tpl_f17r_scope", new PermissionTemplateRequest(
                "f17r-scope-copy", "后台复制权限模板", List.of(), "F17 数据范围", null, null, null, "project", null, null, null));
        assertThat(accessRepository.items.get(scopeCopy.id())).extracting(item -> item.permissionCode() + "=" + item.dataScope())
                .containsExactly("ops.production_readiness.view=project");

        accessRepository.saveTemplate(new PermissionTemplate("tpl_f17r_deny", "tpl_f17r_deny", "F17 禁止规则", null, "base", null,
                "platform", null, "merge", null, null, false, "active", null, null, null, at, at), List.of(
                new PermissionTemplateItem("tpl_f17r_deny", "ops.production_readiness.view", AccessRepository.Effect.GRANT, "project", false),
                new PermissionTemplateItem("tpl_f17r_deny", "ops.production_readiness.check", AccessRepository.Effect.DENY, "project", false)));
        assertThatThrownBy(() -> service.updatePermissionTemplate("tpl_f17r_deny",
                codesRequest("f17r-deny", List.of("ops.production_readiness.view"))))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(409);
                    assertThat(e.getMessage()).isEqualTo("模板含禁止规则，不能直接修改；请新建模板后另行套用");
                });
        assertThat(service.createPermissionTemplate(codesRequest("f17r-deny-new", List.of("ops.production_readiness.view"))).status())
                .isEqualTo("active");
        PermissionTemplate denyCopy = service.copyPermissionTemplate("tpl_f17r_deny", copyRequest("f17r-deny-copy", null, null));
        assertThat(accessRepository.items.get(denyCopy.id())).extracting(PermissionTemplateItem::effect)
                .contains(AccessRepository.Effect.DENY);

        accessRepository.saveTemplate(new PermissionTemplate("tpl_f17r_inactive", "tpl_f17r_inactive", "F17 未启用", null, "base", null,
                "platform", null, "merge", null, null, false, "inactive", null, null, null, at, at), List.of(
                new PermissionTemplateItem("tpl_f17r_inactive", "ops.production_readiness.view", AccessRepository.Effect.GRANT, "project", false)));
        assertThatThrownBy(() -> service.updatePermissionTemplate("tpl_f17r_inactive",
                codesRequest("f17r-inactive", List.of("ops.production_readiness.view"))))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(409);
                    assertThat(e.getMessage()).isEqualTo("模板未启用，不能修改；请新建模板或复制为新模板后另行套用");
                });
        PermissionTemplate revived = service.copyPermissionTemplate("tpl_f17r_inactive", copyRequest("f17r-inactive-copy", null, null));
        assertThat(revived.status()).isEqualTo("active");
        assertThat(accessRepository.items.get(revived.id())).extracting(PermissionTemplateItem::permissionCode)
                .containsExactly("ops.production_readiness.view");
    }

    // M001-F21（基础技术不变量：契约声明为预览的请求不得写入并生效授权；未识别的取值不得静默落为更宽的授权）：套用方式只收
    // append / merge / single_use，其余（原契约里的 preview、replace 与 single_use 的各种错拼、大小写变体）一律 400、不写套用记录，
    // 只留失败痕迹；不传按追加；single_use 按单次生效。
    @Test
    void applyRejectsModesOutsideTheClosedSetWithoutWriting() {
        seedTemplate("tpl_f21", "seed-org", null, "project", "ops.production_readiness.view");
        accessRepository.createUser(new AccessUser("usr_f21", "f21", "staff", "seed-org", null, "active", Instant.now()));
        List<String> modes = List.of("preview", "replace", "single-use", "singleUse", "SINGLE_USE", "once");
        for (String mode : modes) {
            assertThatThrownBy(() -> service.applyPermissionTemplate("tpl_f21", new PermissionTemplateApplyRequest(
                    "f21-invalid-" + mode, "apply", List.of(), "tpl_f21", List.of("usr_f21"), mode)))
                    .isInstanceOfSatisfying(ApiException.class, e -> {
                        assertThat(e.status().value()).isEqualTo(400);
                        assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                    });
        }
        assertThat(accessRepository.templateApplications).isEmpty();
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).doesNotContain("permission_template.applied");
        assertThat(auditEventRepository.events).filteredOn(event -> "permission_template.apply_failed".equals(event.eventName()))
                .hasSize(modes.size());

        PermissionTemplateApplyResult defaulted = service.applyPermissionTemplate("tpl_f21", new PermissionTemplateApplyRequest(
                "f21-default", "apply", List.of(), "tpl_f21", List.of("usr_f21"), null));
        PermissionTemplateApplyResult merged = service.applyPermissionTemplate("tpl_f21", new PermissionTemplateApplyRequest(
                "f21-merge", "apply", List.of(), "tpl_f21", List.of("usr_f21"), "merge"));
        PermissionTemplateApplyResult once = service.applyPermissionTemplate("tpl_f21", new PermissionTemplateApplyRequest(
                "f21-valid-single", "apply", List.of(), "tpl_f21", List.of("usr_f21"), "single_use"));
        assertThat(defaulted.applications()).extracting(PermissionTemplateApplication::singleUse).containsExactly(false);
        assertThat(merged.applications()).extracting(PermissionTemplateApplication::singleUse).containsExactly(false);
        assertThat(once.applications()).extracting(PermissionTemplateApplication::singleUse).containsExactly(true);
    }

    // M001-F20（操作员 2026-09-25 裁决「合并为同一种」：模板只有基础（角色级）与临时两种，原 copy、role 已由 V436 并入）：
    // 类型不是 base / temporary 时，新建、修改、复制一律 400、不落库；前端同形的 base / temporary 照常。
    @Test
    void templateTypeOutsideBaseAndTemporaryIsRejectedOnCreateUpdateAndCopy() {
        PermissionTemplate base = service.createPermissionTemplate(baseTemplateRequest("f20-base"));
        int templatesBefore = accessRepository.templates.size();
        List<org.assertj.core.api.ThrowableAssert.ThrowingCallable> writes = List.of(
                () -> service.createPermissionTemplate(namedRequest("f20-create-role", "F20", "role")),
                () -> service.createPermissionTemplate(namedRequest("f20-create-copy", "F20", "copy")),
                () -> service.createPermissionTemplate(namedRequest("f20-create-unknown", "F20", "unknown")),
                () -> service.updatePermissionTemplate(base.id(), namedRequest("f20-update-copy", "F20", "copy")),
                () -> service.updatePermissionTemplate(base.id(), namedRequest("f20-update-role", "F20", "role")),
                () -> service.copyPermissionTemplate(base.id(), namedRequest("f20-copy-role", "F20", "role")),
                () -> service.copyPermissionTemplate(base.id(), namedRequest("f20-copy-copy", "F20", "copy")));
        for (org.assertj.core.api.ThrowableAssert.ThrowingCallable write : writes) {
            assertThatThrownBy(write).isInstanceOfSatisfying(ApiException.class, e -> {
                assertThat(e.status().value()).isEqualTo(400);
                assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
            });
        }
        assertThat(accessRepository.templates).hasSize(templatesBefore);
        assertThat(accessRepository.templates.get(base.id()).templateType()).isEqualTo("base");
        assertThat(service.createPermissionTemplate(windowRequest("f20-create-temporary", null, Instant.parse("2099-01-31T00:00:00Z"), false))
                .templateType()).isEqualTo("temporary");
    }

    private List<AuditEvent> effectChanged(String idempotencyKey) {
        return auditEventRepository.events.stream()
                .filter(event -> "permission_template.effect_changed".equals(event.eventName()) && idempotencyKey.equals(event.idempotencyKey()))
                .toList();
    }

    private void seedTemplate(String id, String ownerOrganizationId, Instant validUntil, String dataScope, String... codes) {
        Instant at = Instant.parse("2026-06-14T00:00:00Z");
        accessRepository.saveTemplate(new PermissionTemplate(id, id, id, null, "base", null,
                        ownerOrganizationId == null ? "platform" : "organization", ownerOrganizationId, "merge",
                        null, validUntil, false, "active", null, null, null, at, at),
                java.util.Arrays.stream(codes).map(code -> new PermissionTemplateItem(id, code, AccessRepository.Effect.GRANT, dataScope, false))
                        .toList());
    }

    private static PermissionTemplateRequest baseTemplateRequest(String key) {
        return namedRequest(key, "F16 模板", "base");
    }

    private static PermissionTemplateRequest namedRequest(String key, String name, String type) {
        return new PermissionTemplateRequest(key, "seed", List.of(), name, type, null, List.of(), "project", null, null, false);
    }

    // M001-F25（句 2）：条目数据范围不一的在用模板，修改不填数据范围时保留的码沿用原范围、照常收窄（新增截止并改为单次）；
    // 明确填写且与保留条目不同仍 409，模板不变。
    @Test
    void mixedScopeTemplateInUseNarrowsWhenTheRequestLeavesDataScopeEmpty() {
        Instant at = Instant.parse("2026-06-14T00:00:00Z");
        Instant until = Instant.parse("2099-01-31T00:00:00Z");
        List<String> codes = List.of("ops.production_readiness.view", "ops.production_readiness.check");
        accessRepository.saveTemplate(new PermissionTemplate("tpl_f25_mixed", "tpl_f25_mixed", "F25 混用范围", null, "base", null,
                "platform", null, "merge", null, null, false, "active", null, null, null, at, at), List.of(
                new PermissionTemplateItem("tpl_f25_mixed", codes.get(0), AccessRepository.Effect.GRANT, "assignee", false),
                new PermissionTemplateItem("tpl_f25_mixed", codes.get(1), AccessRepository.Effect.GRANT, "project", false)));
        accessRepository.createUser(new AccessUser("usr_f25", "f25", "staff", "seed-org", null, "active", Instant.now()));
        service.applyPermissionTemplate("tpl_f25_mixed", new PermissionTemplateApplyRequest(
                "f25-apply", "apply", List.of(), "tpl_f25_mixed", List.of("usr_f25"), "append"));

        assertThatThrownBy(() -> service.updatePermissionTemplate("tpl_f25_mixed", new PermissionTemplateRequest(
                "f25-explicit", "seed", List.of(), "F25 混用范围", "base", null, codes, "project", null, until, false)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(409);
                    assertThat(e.getMessage()).isEqualTo("模板条目的数据范围不能直接修改；请新建模板或复制为新模板后另行套用");
                });
        assertThat(accessRepository.templates.get("tpl_f25_mixed").validUntil()).isNull();

        assertThat(service.updatePermissionTemplate("tpl_f25_mixed", new PermissionTemplateRequest(
                "f25-narrow", "seed", List.of(), "F25 混用范围", "base", null, codes, null, null, until, true)))
                .satisfies(template -> {
                    assertThat(template.validUntil()).isEqualTo(until);
                    assertThat(template.singleUse()).isTrue();
                });
        assertThat(accessRepository.items.get("tpl_f25_mixed")).extracting(item -> item.permissionCode() + "=" + item.dataScope())
                .containsExactlyInAnyOrder(codes.get(0) + "=assignee", codes.get(1) + "=project");
    }

    private static PermissionTemplateRequest codesRequest(String key, List<String> codes) {
        return new PermissionTemplateRequest(key, "seed", List.of(), "F18 模板", "base", null, codes, "project", null, null, false);
    }

    private static PermissionTemplateRequest windowRequest(String key, Instant from, Instant until, boolean singleUse) {
        return new PermissionTemplateRequest(key, "seed", List.of(), "F17 临时模板", "temporary", null,
                List.of("ops.production_readiness.view"), "project", from, until, singleUse);
    }

    // 前端复制按钮同形：只给名称（M001-F20 起不再带类型），其余不传（null）。
    private static PermissionTemplateRequest copyRequest(String key, Instant validUntil, Boolean singleUse) {
        return new PermissionTemplateRequest(key, "后台复制权限模板", List.of(), "F19 复制件", null, null,
                null, null, null, validUntil, singleUse);
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
        // M002-F32：映射只换了角色、所引模板未变，保留原岗位派生套用（不撤销重建）
        assertThat(accessRepository.templateApplications).filteredOn(a -> "usr_recompute".equals(a.targetId()))
                .extracting(PermissionTemplateApplication::status).containsExactly("active");
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
            // M002-F32：与 Jdbc 同语义——保留下来的原岗位派生套用不撤销
            revokeTemplateApplications(userId, positionApplicationIds.getOrDefault(userId, List.of()).stream()
                    .filter(id -> !applicationIds.contains(id)).toList(), actorId, at);
            positionRoles.put(userId, roleId);
            positionMappings.put(userId, mappingId);
            positionApplicationIds.put(userId, List.copyOf(applicationIds));
            dropHoldsOfRolesNoLongerHeld(userId); // M736 第二步：与 Jdbc 同语义
        }
        @Override public Optional<String> findAssignedPositionTemplateId(String userId) {
            return Optional.ofNullable(positionMappings.get(userId));
        }
        // M002-F32：与 JdbcAccessRepository 同语义——岗位行记下的岗位派生套用（任意状态）与岗位派生角色。
        @Override public List<PermissionTemplateApplication> findPositionApplications(String userId) {
            List<String> ids = positionApplicationIds.getOrDefault(userId, List.of());
            return templateApplications.stream().filter(application -> ids.contains(application.id())).toList();
        }
        @Override public Optional<String> findAssignedPositionRoleId(String userId) {
            return Optional.ofNullable(positionRoles.get(userId));
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

        int permissionExistsCalls; // M002-F26：权限存在性查询次数（断言结构校验先于查库）

        @Override
        public boolean permissionExists(String permissionCode) {
            permissionExistsCalls++;
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
            trace("lockAccessUsers:" + userIds);
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

        // M001-F17：与 JdbcAccessRepository 同口径——实际截止＝套用当时截止与模板当前截止中较早的一个（null 为不限）。
        @Override
        public List<PermissionTemplateApplication> findActiveTemplateApplications(String templateId) {
            return templateApplications.stream()
                    .filter(application -> application.templateId().equals(templateId) && "active".equals(application.status())
                            && !application.consumed())
                    .filter(application -> {
                        Instant until = earlierOf(application.validUntil(), templateWindow(application).validUntil());
                        return until == null || until.isAfter(Instant.now());
                    })
                    .toList();
        }

        // M001-F17：与 JdbcAccessRepository 同口径——active、未消耗、套用行自身截止未过即算，不看模板窗口。
        // （不标 @Override：红版只换测试文件、主代码为基线，接口里还没有这个方法。）
        public boolean hasUnfinishedTemplateApplications(String templateId) {
            return templateApplications.stream().anyMatch(application -> application.templateId().equals(templateId)
                    && "active".equals(application.status()) && !application.consumed()
                    && (application.validUntil() == null || application.validUntil().isAfter(Instant.now())));
        }

        // M001-F17：与 JdbcAccessRepository 同口径——该模板实际截止已到的 active 套用置为 expired（只命中 active，重复处理不重复）。
        public int expireLapsedTemplateApplications(String templateId, Instant checkedAt) {
            int expired = 0;
            for (int i = 0; i < templateApplications.size(); i++) {
                PermissionTemplateApplication a = templateApplications.get(i);
                Instant until = earlierOf(a.validUntil(), templateWindow(a).validUntil());
                if (templateId.equals(a.templateId()) && "active".equals(a.status()) && until != null && !until.isAfter(checkedAt)) {
                    templateApplications.set(i, new PermissionTemplateApplication(a.id(), a.templateId(), a.targetType(), a.targetId(),
                            a.validFrom(), a.validUntil(), a.singleUse(), a.consumed(), "expired", a.appliedBy(), a.appliedAt(),
                            a.revokedBy(), a.revokedAt()));
                    expired++;
                }
            }
            return expired;
        }

        // 套用行对应模板的当前窗口；模板不在夹具里时按不限处理。
        private PermissionTemplate templateWindow(PermissionTemplateApplication application) {
            PermissionTemplate template = templates.get(application.templateId());
            return template != null ? template : new PermissionTemplate(application.templateId(), null, null, null, "base", null,
                    null, null, null, null, null, false, "active", null, null, null, null, null);
        }

        private static Instant laterOf(Instant a, Instant b) {
            return a == null ? b : b == null || a.isAfter(b) ? a : b;
        }

        private static Instant earlierOf(Instant a, Instant b) {
            return a == null ? b : b == null || a.isBefore(b) ? a : b;
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
            trace("findRoleById:" + roleId);
            return Optional.ofNullable(roles.get(roleId));
        }

        @Override
        public RoleRecord createRole(RoleRecord role) {
            roles.put(role.id(), role);
            return role;
        }

        // M736：当时的项目集合（项目表的项目、城市项目公司、在用的域主管公司的编号）；默认空集＝谁也跨不出本人项目。
        final Set<String> projectIds = new LinkedHashSet<>();

        @Override
        public Set<String> findProjectOrganizationIds() {
            return Set.copyOf(projectIds);
        }

        // M736 第二步：逐个勾选的候选清单，与项目集合同一批编号；名称＝「项目 」＋编号（重新审批申请的原因里写条目名称用）。
        @Override
        public List<RoleProjectOption> listRoleProjectOptions() {
            return projectIds.stream().map(id -> new RoleProjectOption(id, "项目 " + id, "project")).toList();
        }

        // ── M736 第二步：以下与 JdbcAccessRepository 同语义 ──
        // 待批条目（员工 → 角色 → 条目，"*"＝「可以查看全部项目」那一勾）；待减标记（角色 → 条目 → 主管复核单号，"*" 同上）。
        final Map<String, Map<String, Set<String>>> viewableHolds = new LinkedHashMap<>();
        final Map<String, Map<String, String>> viewableRemovals = new LinkedHashMap<>();
        // 非空时按先后记下取锁与读操作人能力、读角色、写待批、删勾选行的调用（手写的仓储 spy，只给 M736-S22、S29～S31 用；不并进
        // lockOrder，既有的锁序断言不受影响）。
        List<String> callTrace;

        private void trace(String call) {
            if (callTrace != null) {
                callTrace.add(call);
            }
        }

        @Override
        public void lockRolesForEdit() {
            trace("lockRolesForEdit");
        }

        @Override
        public void lockRoleForEdit(String roleId) {
            trace("lockRoleForEdit:" + roleId);
        }

        @Override
        public void lockRoleForGrant(String roleId) {
            trace("lockRoleForGrant:" + roleId);
        }

        // 直接分配的并上岗位派生的，按编号排序。
        @Override
        public List<String> listRoleHolderIds(String roleId) {
            trace("listRoleHolderIds:" + roleId);
            Set<String> holders = new java.util.TreeSet<>();
            userRoles.forEach((userId, roleIds) -> {
                if (roleIds.contains(roleId)) {
                    holders.add(userId);
                }
            });
            positionRoles.forEach((userId, derived) -> {
                if (roleId.equals(derived)) {
                    holders.add(userId);
                }
            });
            return List.copyOf(holders);
        }

        @Override
        public Map<String, Set<String>> findRoleViewableProjectHolds(String userId) {
            Map<String, Set<String>> byRole = new LinkedHashMap<>();
            viewableHolds.getOrDefault(userId, Map.of()).forEach((roleId, ids) -> {
                if (!ids.isEmpty()) {
                    byRole.put(roleId, new LinkedHashSet<>(ids));
                }
            });
            return byRole;
        }

        @Override
        public Map<String, Set<String>> listRoleViewableProjectHolds(String roleId) {
            Map<String, Set<String>> byUser = new java.util.TreeMap<>();
            viewableHolds.forEach((userId, byRole) -> {
                Set<String> ids = byRole.getOrDefault(roleId, Set.of());
                if (!ids.isEmpty()) {
                    byUser.put(userId, new java.util.TreeSet<>(ids));
                }
            });
            return byUser;
        }

        @Override
        public void addRoleViewableProjectHolds(String userId, String roleId, List<String> viewableIds) {
            trace("addRoleViewableProjectHolds:" + userId);
            if (!viewableIds.isEmpty()) {
                viewableHolds.computeIfAbsent(userId, ignored -> new LinkedHashMap<>())
                        .computeIfAbsent(roleId, ignored -> new LinkedHashSet<>()).addAll(viewableIds);
            }
        }

        @Override
        public int deleteRoleViewableProjectHolds(String userId, String roleId) {
            Set<String> removed = viewableHolds.getOrDefault(userId, new LinkedHashMap<>()).remove(roleId);
            return removed == null ? 0 : removed.size();
        }

        // 只加不删：viewAllProjects 为真才把「可以查看全部项目」置真，清单逐条追加（已有的跳过）。
        @Override
        public void addRoleViewableProjects(String roleId, boolean viewAllProjects, List<String> viewableIds) {
            RoleRecord role = roles.get(roleId);
            List<String> ids = new ArrayList<>(role.viewableProjectIds());
            viewableIds.stream().filter(id -> !ids.contains(id)).forEach(ids::add);
            roles.put(roleId, withTicks(role, role.viewAllProjects() || viewAllProjects, ids));
        }

        private static RoleRecord withTicks(RoleRecord role, boolean viewAllProjects, List<String> viewableProjectIds) {
            return new RoleRecord(role.id(), role.roleName(), role.description(), role.dataScope(), role.status(), role.createdAt(),
                    role.permissionCodes(), role.organizationId(), viewAllProjects, viewableProjectIds);
        }

        @Override
        public Set<String> findRoleViewableProjectRemovals(String roleId) {
            return Set.copyOf(viewableRemovals.getOrDefault(roleId, Map.of()).keySet());
        }

        // 只给还没有标记的条目记单号；清单里不在角色上的条目跳过。角色的勾选不变。
        @Override
        public void markRoleViewableProjectRemoval(String roleId, boolean viewAllProjects, List<String> viewableIds, String reviewId) {
            Map<String, String> marks = viewableRemovals.computeIfAbsent(roleId, ignored -> new LinkedHashMap<>());
            List<String> ticked = roles.get(roleId).viewableProjectIds();
            viewableIds.stream().filter(ticked::contains).forEach(id -> marks.putIfAbsent(id, reviewId));
            if (viewAllProjects) {
                marks.putIfAbsent("*", reviewId);
            }
        }

        // 按复核单号落地：去掉带该单号的条目（清单条目在前，"*" 在最后）与这些条目上所有人的待批；单号对不上的什么都不改。
        @Override
        public List<String> applyRoleViewableProjectRemoval(String roleId, String reviewId) {
            trace("applyRoleViewableProjectRemoval:" + roleId);
            Map<String, String> marks = viewableRemovals.getOrDefault(roleId, new LinkedHashMap<>());
            List<String> removed = new ArrayList<>();
            marks.forEach((id, markedBy) -> {
                if (reviewId.equals(markedBy) && !"*".equals(id)) {
                    removed.add(id);
                }
            });
            boolean all = reviewId.equals(marks.get("*"));
            if (all) {
                removed.add("*");
            }
            if (removed.isEmpty()) {
                return removed;
            }
            RoleRecord role = roles.get(roleId);
            roles.put(roleId, withTicks(role, role.viewAllProjects() && !all,
                    role.viewableProjectIds().stream().filter(id -> !removed.contains(id)).toList()));
            removed.forEach(marks::remove);
            viewableHolds.values().forEach(byRole -> byRole.getOrDefault(roleId, new LinkedHashSet<>()).removeAll(removed));
            return removed;
        }

        @Override
        public void clearRoleViewableProjectRemoval(String roleId, String reviewId) {
            viewableRemovals.getOrDefault(roleId, new LinkedHashMap<>()).values().removeIf(reviewId::equals);
        }

        // 两条路（直接分配、岗位派生）都不再持有的角色，待批条目随之删除；只去掉一条路的不删（Jdbc 在 unassignUserRole、
        // replacePositionGrants 里做同一件事）。
        private void dropHoldsOfRolesNoLongerHeld(String userId) {
            Map<String, Set<String>> byRole = viewableHolds.get(userId);
            if (byRole != null) {
                byRole.keySet().removeIf(roleId -> !userRoles.getOrDefault(userId, Set.of()).contains(roleId)
                        && !roleId.equals(positionRoles.get(userId)));
            }
        }

        @Override
        public RoleAssignmentRecord assignUserRole(String userId, String roleId) {
            userRoles.computeIfAbsent(userId, ignored -> new LinkedHashSet<>()).add(roleId);
            return new RoleAssignmentRecord(userId, roleId);
        }

        @Override
        public void unassignUserRole(String userId, String roleId) {
            userRoles.computeIfAbsent(userId, ignored -> new LinkedHashSet<>()).remove(roleId);
            dropHoldsOfRolesNoLongerHeld(userId); // M736 第二步：与 Jdbc 同语义
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
            trace("findPermissionEffects:" + userId);
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
                // M001-F17：与 JdbcAccessRepository 同口径——窗口取套用当时与模板当前的交集，单次＝任一为单次。
                PermissionTemplate template = templateWindow(application);
                for (PermissionTemplateItem item : findTemplateItems(application.templateId())) {
                    effects.add(new PermissionEffect(
                            userId,
                            item.permissionCode(),
                            item.effect(),
                            Source.TEMPLATE_APPLICATION,
                            application.id(),
                            laterOf(application.validFrom(), template.validFrom()),
                            earlierOf(application.validUntil(), template.validUntil()),
                            application.singleUse() || template.singleUse(),
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

        // M002-F40：与 JDBC 同语义——任意状态的复核单（通用主管复核入口落地后回读终态）。
        @Override
        public Optional<SupervisorReviewDecisionRecord> findSupervisorReviewById(String reviewId) {
            return Optional.ofNullable(reviews.get(reviewId));
        }
    }

    // XM-M002-I（操作员 2026-09-25 裁定口径 B「一律只认长期来源」）：操作人只凭临时授权、单次临时授权、单次模板套用或有到期的模板套用
    // 持有某码时，经员工单独授予、解除员工显式拒绝、建角色、配角色、定岗（岗位角色与岗位模板两部分）、修改模板内容、套用长期 / 临时 / 单次模板
    // （含以单次方式套用、套给自己）、临时授权（授给别人或自己、单次或多次）都被 GRANT_EXCEEDS_OWN_PERMISSIONS 拒绝，换幂等键重试也一样；
    // 改由角色长期持有后各入口照常成功。
    @Test
    void temporaryOrSingleUseHolderCannotGrantTheCodeThroughAnyEntry() {
        String actor = "usr_xm_i_actor";
        String target = "usr_xm_i_target";
        String denyTarget = "usr_xm_i_deny_target";
        for (String userId : List.of(actor, target)) {
            accessRepository.users.put(userId, new AccessUser(
                    userId, userId, "staff", "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        }
        m002ViewDenyFixture(denyTarget); // 目标经 role_view 持有 view 并有员工显式拒绝；role_view 也用作下方配角色、岗位角色
        Instant until = Instant.parse("2099-01-01T00:00:00Z");
        accessRepository.roles.put("role_xm_i_blank", new RoleRecord(
                "role_xm_i_blank", "xm_i_blank", "Blank", "project", "active", Instant.parse("2026-06-14T00:00:00Z"), List.of(), null));
        xmITemplate("tpl_xm_i_base", "base", null, false, M002_VIEW);
        xmITemplate("tpl_xm_i_temp", "temporary", until, false, M002_VIEW);
        xmITemplate("tpl_xm_i_once", "temporary", null, true, M002_VIEW);
        xmITemplate("tpl_xm_i_edit", "temporary", until, false);
        xmITemplate("tpl_xm_i_blank", "base", null, false);
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest( // 平台管理员随身份持有日常码 view
                "xm-i-map-role", null, "ops_manager", "lvl_m1_lower", "role_view", "tpl_xm_i_blank", "active"));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "xm-i-map-temp", null, "ops_supervisor", "lvl_m1_lower", "role_xm_i_blank", "tpl_xm_i_temp", "active"));
        authenticateAsOrgUser(actor, actor, "seed-org");

        int attempt = 0;
        for (Runnable source : xmITemporarySources(actor, M002_VIEW)) {
            accessRepository.temporaryGrants.clear();
            accessRepository.templateApplications.clear();
            source.run();
            assertThat(authorizationService.effectivePermissions().grants()).contains(M002_VIEW); // 修复前即凭此放行
            for (int retry = 0; retry < 2; retry++) { // 换幂等键重试也绕不过
                String key = "xm-i-" + attempt++;
                assertGrantExceeds(() -> service.setUserPermissionOverrides(target, new UserPermissionOverrideRequest(
                        key + "-override", null, List.of(M002_VIEW), "grant", "单独授予")));
                assertGrantExceeds(() -> service.removeUserPermissionOverrides(denyTarget, m002RemoveDeny(key + "-undeny", M002_VIEW)));
                assertGrantExceeds(() -> service.createRole(new RoleRequest(
                        key + "-role", null, List.of(), "临时依据角色" + key, null, List.of(M002_VIEW), "project")));
                assertGrantExceeds(() -> service.assignUserRole(new RoleAssignmentRequest(key + "-assign", null, target, "role_view")));
                for (String position : List.of("ops_manager", "ops_supervisor")) { // 岗位角色 / 岗位临时模板
                    assertGrantExceeds(() -> service.assignUserPosition(new UserPositionAssignmentRequest(
                            key + "-" + position, null, target, position, "lvl_m1_lower", null, null, null)));
                }
                assertGrantExceeds(() -> service.updatePermissionTemplate("tpl_xm_i_edit", xmIEditRequest(key + "-edit", until)));
                for (String grantee : List.of(target, actor)) { // 含授给自己
                    for (String templateId : List.of("tpl_xm_i_base", "tpl_xm_i_temp", "tpl_xm_i_once")) {
                        for (String mode : List.of("merge", "single_use")) {
                            assertGrantExceeds(() -> service.applyPermissionTemplate(templateId, new PermissionTemplateApplyRequest(
                                    key + "-apply-" + templateId + "-" + mode + "-" + grantee, "apply", List.of(), templateId,
                                    List.of(grantee), mode)));
                        }
                    }
                    for (boolean singleUse : List.of(true, false)) {
                        assertGrantExceeds(() -> service.createTemporaryGrant(new TemporaryGrantRequest(
                                key + "-temporary-" + singleUse + "-" + grantee, null, List.of(), grantee, List.of(M002_VIEW), null,
                                until.minusSeconds(3600), singleUse, "临时授出")));
                    }
                }
            }
            assertThat(accessRepository.temporaryGrants.keySet()).allMatch("tg_xm_i"::equals);
            assertThat(accessRepository.templateApplications).allMatch(application -> "tpl_xm_i_src".equals(application.templateId()));
        }
        assertThat(overrideSources(target, M002_VIEW, AccessRepository.Effect.GRANT)).isEmpty();
        assertThat(overrideSources(denyTarget, M002_VIEW, AccessRepository.Effect.DENY)).containsExactly(AccessRepository.Source.USER_OVERRIDE);
        assertThat(accessRepository.roles.values()).noneMatch(role -> role.roleName().startsWith("临时依据角色"));
        assertThat(accessRepository.userRoles.getOrDefault(target, Set.of())).isEmpty();
        assertThat(accessRepository.userPositionAssignments).doesNotContainKey(target);
        assertThat(accessRepository.items.get("tpl_xm_i_edit")).isEmpty();

        accessRepository.temporaryGrants.clear();
        accessRepository.templateApplications.clear();
        accessRepository.userRoles.put(actor, new LinkedHashSet<>(Set.of("role_view"))); // 角色＝长期来源
        service.setUserPermissionOverrides(target, new UserPermissionOverrideRequest(
                "xm-i-ok-override", null, List.of(M002_VIEW), "grant", "单独授予"));
        service.removeUserPermissionOverrides(denyTarget, m002RemoveDeny("xm-i-ok-undeny", M002_VIEW));
        RoleRecord created = service.createRole(new RoleRequest(
                "xm-i-ok-role", null, List.of(), "长期依据角色", null, List.of(M002_VIEW), "project"));
        service.assignUserRole(new RoleAssignmentRequest("xm-i-ok-assign", null, target, "role_view"));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "xm-i-ok-ops_manager", null, target, "ops_manager", "lvl_m1_lower", null, null, null));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "xm-i-ok-ops_supervisor", null, target, "ops_supervisor", "lvl_m1_lower", null, null, null));
        service.updatePermissionTemplate("tpl_xm_i_edit", xmIEditRequest("xm-i-ok-edit", until));
        for (String templateId : List.of("tpl_xm_i_base", "tpl_xm_i_temp", "tpl_xm_i_once")) {
            service.applyPermissionTemplate(templateId, new PermissionTemplateApplyRequest(
                    "xm-i-ok-apply-" + templateId, "apply", List.of(), templateId, List.of(target), "merge"));
        }
        service.createTemporaryGrant(new TemporaryGrantRequest("xm-i-ok-temporary", null, List.of(), target, List.of(M002_VIEW), null,
                until.minusSeconds(3600), false, "临时授出"));
        assertThat(overrideSources(target, M002_VIEW, AccessRepository.Effect.GRANT)).containsExactly(AccessRepository.Source.USER_OVERRIDE);
        assertThat(overrideSources(denyTarget, M002_VIEW, AccessRepository.Effect.DENY)).isEmpty();
        assertThat(accessRepository.roles.get(created.id()).permissionCodes()).containsExactly(M002_VIEW);
        assertThat(accessRepository.userRoles.get(target)).contains("role_view");
        assertThat(accessRepository.positionRoles.get(target)).isEqualTo("role_xm_i_blank");
        assertThat(accessRepository.items.get("tpl_xm_i_edit")).extracting(PermissionTemplateItem::permissionCode).containsExactly(M002_VIEW);
        assertThat(accessRepository.templateApplications).filteredOn(application -> "active".equals(application.status()))
                .extracting(PermissionTemplateApplication::templateId)
                .containsExactlyInAnyOrder("tpl_xm_i_temp", "tpl_xm_i_base", "tpl_xm_i_temp", "tpl_xm_i_once");
        assertThat(accessRepository.temporaryGrants.values()).extracting(TemporaryGrant::targetUserId).containsExactly(target);

        // M723-F02「显式拒绝仍优先」：操作人经角色长期持有该码，但本人该码有员工显式拒绝 → 仍不能授出
        accessRepository.saveUserPermissionOverrides(actor, List.of(M002_VIEW), AccessRepository.Effect.DENY, "停权", "usr_admin");
        assertGrantExceeds(() -> service.createRole(new RoleRequest(
                "xm-i-denied-role", null, List.of(), "被拒依据角色", null, List.of(M002_VIEW), "project")));
        assertThat(accessRepository.roles.values()).noneMatch(role -> role.roleName().startsWith("被拒依据角色"));
    }

    // XM-M002-I（口径 B）：岗位映射保存（只许平台管理员）同样只认长期来源——平台管理员只凭上述四种临时或单次来源持有非日常码时，
    // 保存含该码的岗位角色或岗位临时模板都被拒，换幂等键重试也一样；改由角色长期持有后照常保存。
    @Test
    void positionMappingSaveOnlyCountsThePlatformAdminsLongTermSources() {
        String check = "ops.production_readiness.check"; // 高风险、非日常码：平台管理员不随身份持有
        Instant until = Instant.parse("2099-01-01T00:00:00Z");
        accessRepository.userRoles.remove("usr_admin"); // 去掉夹具给的长期来源 explicit_admin_grants
        accessRepository.roles.put("role_xm_i_check", new RoleRecord(
                "role_xm_i_check", "xm_i_check", "Check", "project", "active", Instant.parse("2026-06-14T00:00:00Z"), List.of(check), null));
        accessRepository.roles.put("role_xm_i_blank", new RoleRecord(
                "role_xm_i_blank", "xm_i_blank", "Blank", "project", "active", Instant.parse("2026-06-14T00:00:00Z"), List.of(), null));
        xmITemplate("tpl_xm_i_temp", "temporary", until, false, check);
        xmITemplate("tpl_xm_i_blank", "base", null, false);

        int attempt = 0;
        for (Runnable source : xmITemporarySources("usr_admin", check)) {
            accessRepository.temporaryGrants.clear();
            accessRepository.templateApplications.clear();
            source.run();
            assertThat(authorizationService.effectivePermissions().grants()).contains(check); // 修复前即凭此放行
            for (int retry = 0; retry < 2; retry++) {
                String key = "xm-i-map-" + attempt++;
                assertGrantExceeds(() -> service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                        key + "-role", null, "ops_manager", "lvl_m1_lower", "role_xm_i_check", "tpl_xm_i_blank", "active")));
                assertGrantExceeds(() -> service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                        key + "-temp", null, "ops_supervisor", "lvl_m1_lower", "role_xm_i_blank", "tpl_xm_i_temp", "active")));
            }
        }
        assertThat(accessRepository.positionRoleTemplates).isEmpty();

        accessRepository.userRoles.put("usr_admin", new LinkedHashSet<>(Set.of("role_xm_i_check"))); // 角色＝长期来源
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "xm-i-map-ok-role", null, "ops_manager", "lvl_m1_lower", "role_xm_i_check", "tpl_xm_i_blank", "active"));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "xm-i-map-ok-temp", null, "ops_supervisor", "lvl_m1_lower", "role_xm_i_blank", "tpl_xm_i_temp", "active"));
        assertThat(accessRepository.positionRoleTemplates.values()).extracting(PositionRoleTemplate::templateId)
                .containsExactlyInAnyOrder("tpl_xm_i_blank", "tpl_xm_i_temp");
    }

    // XM-M002-I：只临时或单次持有 code 的四种来源——临时授权、单次临时授权、单次模板套用、有到期的模板套用（调用方每次先清掉上一种）。
    private List<Runnable> xmITemporarySources(String userId, String code) {
        Instant until = Instant.parse("2099-01-01T00:00:00Z");
        xmITemplate("tpl_xm_i_src", "base", null, false, code);
        return List.of(
                () -> accessRepository.temporaryGrants.put("tg_xm_i", xmITemporaryGrant(userId, code, false, until)),
                () -> accessRepository.temporaryGrants.put("tg_xm_i", xmITemporaryGrant(userId, code, true, until)),
                () -> accessRepository.applyTemplate("tpl_xm_i_src", List.of(userId), null, null, true, "usr_admin"),
                () -> accessRepository.applyTemplate("tpl_xm_i_src", List.of(userId), null, until, false, "usr_admin"));
    }

    private static TemporaryGrant xmITemporaryGrant(String userId, String code, boolean singleUse, Instant until) {
        return new TemporaryGrant("tg_xm_i", userId, List.of(code), Instant.parse("2026-06-14T00:00:00Z"), until, singleUse, false,
                "active", "临时授权", "usr_admin", Instant.parse("2026-06-14T00:00:00Z"), null, null);
    }

    // 本项目（seed-org）自有模板。
    private void xmITemplate(String id, String type, Instant validUntil, boolean singleUse, String... codes) {
        accessRepository.saveTemplate(new PermissionTemplate(
                id, id, id, null, type, null, "organization", "seed-org", "merge", null, validUntil, singleUse, "active", null, null, null,
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                java.util.Arrays.stream(codes)
                        .map(code -> new PermissionTemplateItem(id, code, AccessRepository.Effect.GRANT, "project", false)).toList());
    }

    private static PermissionTemplateRequest xmIEditRequest(String key, Instant validUntil) {
        return new PermissionTemplateRequest(key, null, List.of(), "tpl_xm_i_edit", "temporary", null, List.of(M002_VIEW), "project",
                null, validUntil, false);
    }

    private static void assertGrantExceeds(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("GRANT_EXCEEDS_OWN_PERMISSIONS"));
    }

    // ── M736（XM-M001-D 可跨项目角色·批 1 权限治理；操作员 2026-10-01 确认的治理地图第 653 行追加句：「被分配了可跨项目角色的员工
    //    （含经岗位得到），在所分配的权限内可以查看、处理该角色可查看项目的数据。」）────────────────────────────────────────
    //    夹具：当时的项目集合＝accessRepository.projectIds；可跨项目角色按 10 参构造直接放进 accessRepository.roles；
    //    判定用真实的 AuthorizationService，不打桩。甲、乙、丙项目＝org_a、org_b、org_c。

    private static final String XP_ASSIGN = "access.role.assign";
    private static final String XP_APPLY = "access.grant_request.apply";

    private void xpProjects(String... projectIds) {
        accessRepository.projectIds.addAll(List.of(projectIds));
    }

    // 平台角色（归属为空）。viewAll＝勾了「可以查看全部项目」；ticked＝展开后逐个勾选的项目；两项都没有的是普通角色。
    private void xpRole(String roleId, List<String> codes, boolean viewAll, String... ticked) {
        accessRepository.roles.put(roleId, new RoleRecord(roleId, roleId, null, "project", "active",
                Instant.parse("2026-06-14T00:00:00Z"), codes, null, viewAll, List.of(ticked)));
    }

    private void xpUser(String userId, String organizationId) {
        accessRepository.users.put(userId, new AccessUser(userId, userId, "staff", organizationId, null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
    }

    // 甲项目的普通员工，直接持有给定角色，并以其身份登录（会话与账号行一致）。
    private void xpActAs(String userId, String... roleIds) {
        xpUser(userId, "org_a");
        accessRepository.userRoles.put(userId, new LinkedHashSet<>(List.of(roleIds)));
        authenticateAsOrgUser(userId, userId, "org_a");
    }

    // 归属给定项目的模板（ownerScopeId 为空＝平台模板），带一个日常码。
    private void xpTemplate(String id, String ownerScopeId) {
        accessRepository.saveTemplate(new PermissionTemplate(
                id, id, id, null, "base", null, ownerScopeId == null ? "platform" : "organization", ownerScopeId, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem(id, M002_VIEW, AccessRepository.Effect.GRANT, "project", false)));
    }

    private static PermissionTemplateRequest xpTemplateRequest(String key) {
        return new PermissionTemplateRequest(key, "M736", List.of(), "跨项目模板", "base", null, List.of(M002_VIEW), "project",
                null, null, false);
    }

    private static RoleRequest xpRoleRequest(String key, Boolean viewAll, List<String> ticked) {
        return new RoleRequest(key, "M736", List.of(), "角色" + key, null, List.of(M002_VIEW), "project", viewAll, ticked);
    }

    private static AccessGrantApplyRequest xpRoleApplication(String key, String roleId) {
        return new AccessGrantApplyRequest(key, "M736", List.of(), "user_role", "usr_xp_target", roleId, null, null, null, null, null,
                null, null, null, false, "角色授予申请");
    }

    private static void xpExpect(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, int status, String code, String message) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.status().value()).isEqualTo(status);
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.getMessage()).contains(message);
        });
    }

    // M736-U1（操作员 2026-10-01 说明「权限配置表格当中有一个可以查看全部项目选项……可以点击+打开……逐个独立的项目去选」；同日所选选项原文
    // 「项目管理员给本项目建的角色也能勾其他项目；分配这类角色仍按之前的答复先审批。」；2026-10-03 所选选项原文「项目、城市项目公司、
    // 在用的域主管公司各列为独立条目，勾哪条只管哪条，互不连带。」）：建角色收两项勾选。四种 400 都不建角色、各留一条失败留痕——
    // 两项同时给；清单有空白条目；去重后超过 500 条；有不在项目集合里的条目（顶层平台主体、停用的域主管公司、查不到的编号各一例）。
    // 停用的城市项目公司在项目集合里，可以勾；重复条目去重，恰好 500 条可以；成功留痕带两项。四种校验排在「不持角色里的码」之前；
    // 建角色的人自己看不到所勾项目也能建，角色仍归他本人项目。
    @Test
    void createRoleValidatesAndStoresTheViewableProjectTicks() {
        xpProjects("org_a", "org_b", "be_city_inactive"); // 项目集合：项目表的项目、城市项目公司（含停用的）、在用的域主管公司
        List<String> bulk = java.util.stream.IntStream.range(0, 500).mapToObj(i -> "bulk_" + i).toList();
        accessRepository.projectIds.addAll(bulk);
        int roles = accessRepository.roles.size();

        xpExpect(() -> service.createRole(xpRoleRequest("xp-u1-both", true, List.of("org_b"))),
                400, "VALIDATION_FAILED", "勾了“可以查看全部项目”就不能再逐个勾选项目");
        List<List<String>> blanks = List.of(java.util.Arrays.asList("org_b", " "), java.util.Arrays.asList("org_b", null));
        for (int i = 0; i < blanks.size(); i++) {
            RoleRequest blank = xpRoleRequest("xp-u1-blank-" + i, null, blanks.get(i));
            xpExpect(() -> service.createRole(blank), 400, "VALIDATION_FAILED", "可查看项目清单里有空白条目");
        }
        List<String> tooMany = new ArrayList<>(bulk);
        tooMany.add("org_b");
        xpExpect(() -> service.createRole(xpRoleRequest("xp-u1-501", false, tooMany)),
                400, "VALIDATION_FAILED", "逐个勾选的可查看项目不能超过 500 条");
        for (String outside : List.of("be_platform", "be_domain_inactive", "no_such_id")) {
            assertThatThrownBy(() -> service.createRole(xpRoleRequest("xp-u1-" + outside, false, List.of("org_b", outside))))
                    .isInstanceOfSatisfying(ApiException.class, e -> {
                        assertThat(e.status().value()).isEqualTo(400);
                        assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                        assertThat(e.getMessage()).isEqualTo("可查看项目清单里有无法识别或已停用的条目：" + outside);
                    });
        }
        assertThat(accessRepository.roles).hasSize(roles);
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).doesNotContain("role.changed");
        assertThat(auditEventRepository.events).filteredOn(event -> "role.change_failed".equals(event.eventName()))
                .extracting(event -> event.metadata().get("error_code")).hasSize(7).containsOnly("VALIDATION_FAILED");

        RoleRecord inactiveCity = service.createRole(xpRoleRequest("xp-u1-city", false, List.of("be_city_inactive")));
        assertThat(accessRepository.roles.get(inactiveCity.id()).viewableProjectIds()).containsExactly("be_city_inactive");
        RoleRecord deduplicated = service.createRole(xpRoleRequest("xp-u1-dedupe", null, List.of("org_b", "org_a", "org_b")));
        assertThat(accessRepository.roles.get(deduplicated.id()).viewableProjectIds()).containsExactly("org_b", "org_a");
        assertThat(accessRepository.roles.get(deduplicated.id()).viewAllProjects()).isFalse();
        List<String> fiveHundred = new ArrayList<>(bulk);
        fiveHundred.add("bulk_0");
        assertThat(service.createRole(xpRoleRequest("xp-u1-500", false, fiveHundred)).viewableProjectIds()).hasSize(500);
        RoleRecord all = service.createRole(xpRoleRequest("xp-u1-all", true, null));
        assertThat(accessRepository.roles.get(all.id()).viewAllProjects()).isTrue();
        assertThat(accessRepository.roles.get(all.id()).viewableProjectIds()).isEmpty();
        RoleRecord plain = service.createRole(xpRoleRequest("xp-u1-plain", null, null));
        assertThat(AuthorizationService.isCrossProjectRole(accessRepository.roles.get(all.id()))).isTrue();
        assertThat(AuthorizationService.isCrossProjectRole(accessRepository.roles.get(deduplicated.id()))).isTrue();
        assertThat(AuthorizationService.isCrossProjectRole(accessRepository.roles.get(plain.id()))).isFalse();
        assertThat(auditEventRepository.events).filteredOn(event -> "xp-u1-dedupe".equals(event.idempotencyKey()))
                .singleElement().satisfies(event -> {
                    assertThat(event.eventName()).isEqualTo("role.changed");
                    assertThat(event.metadata().get("view_all_projects")).isEqualTo(false);
                    assertThat(event.metadata().get("viewable_project_ids")).isEqualTo(List.of("org_b", "org_a"));
                });

        xpActAs("usr_xp_creator");
        xpExpect(() -> service.createRole(xpRoleRequest("xp-u1-creator-invalid", false, List.of("no_such_id"))),
                400, "VALIDATION_FAILED", "可查看项目清单里有无法识别或已停用的条目");
        xpExpect(() -> service.createRole(xpRoleRequest("xp-u1-creator-unheld", false, List.of("org_b"))),
                403, "GRANT_EXCEEDS_OWN_PERMISSIONS", M002_VIEW);
        xpRole("role_xp_view", List.of(M002_VIEW), false);
        xpActAs("usr_xp_creator", "role_xp_view");
        assertThat(authorizationService.projectReach(authorizationService.currentActor(), "access.role.manage").covers("org_b"))
                .as("建角色的人自己看不到乙项目").isFalse();
        RoleRecord byCreator = service.createRole(xpRoleRequest("xp-u1-creator", false, List.of("org_b")));
        assertThat(accessRepository.roles.get(byCreator.id()).organizationId()).isEqualTo("org_a");
        assertThat(accessRepository.roles.get(byCreator.id()).viewableProjectIds()).containsExactly("org_b");
    }

    // M736-U3（操作员 2026-09-25「在他分配的权限内」）：分配角色权限只经员工单独授予得到的人，不是经可跨项目角色持有——撤不了乙项目员工的
    // 角色；同一个人再经只勾了乙项目的角色持有就可以，丙项目仍不行；收回这个角色后又不行（第二次）。
    @Test
    void roleAssignHeldOutsideACrossProjectRoleDoesNotReachOtherProjects() {
        xpProjects("org_a", "org_b", "org_c");
        xpRole("role_xp_keep", List.of(M002_VIEW), false);
        xpRole("role_xp_cross", List.of(XP_ASSIGN), false, "org_b");
        for (String target : List.of("usr_xp_b", "usr_xp_c")) {
            xpUser(target, "usr_xp_b".equals(target) ? "org_b" : "org_c");
            accessRepository.userRoles.put(target, new LinkedHashSet<>(Set.of("role_xp_keep")));
        }
        xpActAs("usr_xp_holder");
        accessRepository.saveUserPermissionOverrides("usr_xp_holder", List.of(XP_ASSIGN), AccessRepository.Effect.GRANT, "单独授予", "usr_admin");

        xpExpect(() -> service.unassignUserRole(new RoleAssignmentRequest("xp-u3-override", null, "usr_xp_b", "role_xp_keep")),
                403, "CROSS_ORG_FORBIDDEN", "usr_xp_b");
        assertThat(accessRepository.userRoles.get("usr_xp_b")).contains("role_xp_keep");

        xpActAs("usr_xp_holder", "role_xp_cross");
        service.unassignUserRole(new RoleAssignmentRequest("xp-u3-cross", null, "usr_xp_b", "role_xp_keep"));
        assertThat(accessRepository.userRoles.get("usr_xp_b")).doesNotContain("role_xp_keep");
        xpExpect(() -> service.unassignUserRole(new RoleAssignmentRequest("xp-u3-cross-c", null, "usr_xp_c", "role_xp_keep")),
                403, "CROSS_ORG_FORBIDDEN", "usr_xp_c");
        assertThat(accessRepository.userRoles.get("usr_xp_c")).contains("role_xp_keep");

        accessRepository.userRoles.put("usr_xp_b", new LinkedHashSet<>(Set.of("role_xp_keep")));
        xpActAs("usr_xp_holder");
        xpExpect(() -> service.unassignUserRole(new RoleAssignmentRequest("xp-u3-again", null, "usr_xp_b", "role_xp_keep")),
                403, "CROSS_ORG_FORBIDDEN", "usr_xp_b");
        assertThat(accessRepository.userRoles.get("usr_xp_b")).contains("role_xp_keep");
    }

    // M736-U6（治理地图第 703 行「跨商户、跨项目权限每次均须先审批，审批通过后方可生效」；操作员 2026-10-01 所选选项原文「项目管理员给本项目
    // 建的角色也能勾其他项目；分配这类角色仍按之前的答复先审批。」；2026-09-25「平台管理员可直接分配」，2026-10-01 确认句的选项原文
    // 「只免「跨项目」这一项审批，角色里的高危码照旧先审批。」；定岗一半是技术推导，依据操作员 2026-09-25 答「也算」）：
    // 直接分配——非平台管理员分配可跨项目角色（含只勾了目标员工所在项目的）403 GRANT_REQUIRES_APPROVAL、不落行，换幂等键再来仍 403；
    // 不持角色里的码先得「超出本人权限」；普通角色照旧直接分配；平台管理员分配只含日常权限的这类角色成功，含须审批码的仍被原有的审批闸拦下。
    // 直接定岗——非平台管理员定岗、新派生出这类角色 403，不写岗位行、不套模板；员工已挂在这张映射上时重定不拦；平台管理员定岗不拦；
    // 平台管理员改岗位映射触发的重派生不拦。
    @Test
    void directAssignmentAndPositioningOfACrossProjectRoleNeedApprovalUnlessByThePlatformAdmin() {
        accessRepository.markRequiresApproval(M002_CODE, "high");
        service = newAccessService(true, supervisorReviewService(new InMemorySupervisorReviewRepository()));
        xpProjects("org_a", "org_b");
        xpRole("role_xp_view", List.of(M002_VIEW), false);
        xpRole("role_xp_cross", List.of(M002_VIEW), false, "org_b");
        xpRole("role_xp_same_project", List.of(M002_VIEW), false, "org_a");
        xpRole("role_xp_cross_unheld", List.of("governance.supervisor_review.view"), false, "org_b");
        xpRole("role_xp_cross_high", List.of(M002_CODE), true);
        xpUser("usr_xp_target", "org_a");
        xpUser("usr_xp_seat", "org_a");

        xpActAs("usr_xp_mgr", "role_xp_view");
        for (int round = 1; round <= 2; round++) {
            for (String role : List.of("role_xp_cross", "role_xp_same_project")) {
                String key = "xp-u6-assign-" + role + "-" + round;
                xpExpect(() -> service.assignUserRole(new RoleAssignmentRequest(key, null, "usr_xp_target", role)),
                        403, "GRANT_REQUIRES_APPROVAL", "可跨项目角色须提交授权申请");
            }
        }
        xpExpect(() -> service.assignUserRole(new RoleAssignmentRequest("xp-u6-assign-unheld", null, "usr_xp_target", "role_xp_cross_unheld")),
                403, "GRANT_EXCEEDS_OWN_PERMISSIONS", "governance.supervisor_review.view");
        assertThat(accessRepository.userRoles.getOrDefault("usr_xp_target", Set.of())).isEmpty();
        service.assignUserRole(new RoleAssignmentRequest("xp-u6-assign-plain", null, "usr_xp_target", "role_xp_view"));
        assertThat(accessRepository.userRoles.get("usr_xp_target")).containsExactly("role_xp_view");

        authenticateAsPlatformAdmin("seed-org");
        service.assignUserRole(new RoleAssignmentRequest("xp-u6-admin-assign", null, "usr_xp_target", "role_xp_cross"));
        assertThat(accessRepository.userRoles.get("usr_xp_target")).contains("role_xp_cross");
        xpExpect(() -> service.assignUserRole(new RoleAssignmentRequest("xp-u6-admin-high", null, "usr_xp_target", "role_xp_cross_high")),
                403, "GRANT_REQUIRES_APPROVAL", "以下权限的授予需要审批");
        assertThat(accessRepository.userRoles.get("usr_xp_target")).doesNotContain("role_xp_cross_high");

        xpTemplate("tpl_xp_pos", null);
        PositionRoleTemplate mapping = service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "xp-u6-map", null, "xp_patrol", "lvl_m1_lower", "role_xp_cross", "tpl_xp_pos", "active"));
        xpActAs("usr_xp_mgr", "role_xp_view");
        xpExpect(() -> service.assignUserPosition(new UserPositionAssignmentRequest(
                        "xp-u6-seat-mgr", null, "usr_xp_seat", "xp_patrol", "lvl_m1_lower", null, null, null)),
                403, "GRANT_REQUIRES_APPROVAL", "可跨项目角色须提交授权申请");
        assertThat(accessRepository.userPositionAssignments).doesNotContainKey("usr_xp_seat");
        assertThat(accessRepository.positionRoles).doesNotContainKey("usr_xp_seat");
        assertThat(accessRepository.templateApplications).noneMatch(application -> "usr_xp_seat".equals(application.targetId()));

        authenticateAsPlatformAdmin("seed-org");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "xp-u6-seat-admin", null, "usr_xp_seat", "xp_patrol", "lvl_m1_lower", null, null, null));
        assertThat(accessRepository.positionRoles.get("usr_xp_seat")).isEqualTo("role_xp_cross");

        xpActAs("usr_xp_mgr", "role_xp_view");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "xp-u6-reseat-mgr", null, "usr_xp_seat", "xp_patrol", "lvl_m1_lower", null, null, null)); // 同映射重定，不是新授出
        assertThat(accessRepository.positionRoles.get("usr_xp_seat")).isEqualTo("role_xp_cross");

        authenticateAsPlatformAdmin("seed-org");
        xpRole("role_xp_cross_two", List.of(M002_VIEW), true);
        service.updatePositionRoleTemplate(mapping.id(), new PositionRoleTemplateRequest(
                "xp-u6-map-change", null, "xp_patrol", "lvl_m1_lower", "role_xp_cross_two", "tpl_xp_pos", "active"));
        assertThat(accessRepository.positionRoles.get("usr_xp_seat")).isEqualTo("role_xp_cross_two");
    }

    // M736-U7（操作员 2026-09-25 所选选项原文「按授权政策「高」一档走（按现行政策不到最高权限）」；2026-10-01「分配这类角色仍按之前的答复
    // 先审批」）：非平台管理员申请把可跨项目角色分给员工——只含日常权限也受理，档位取各码档位与「高」一档的较高者：只含日常码、含中档
    // 须审批码都走上两级，含极高档码走最高权限（那一级来自码本身）。对照：没勾项目的角色只含中档须审批码仍走上一级、只含日常码仍是
    // 「请直接分配」；平台管理员申请只含日常权限的这类角色仍是「请直接分配」。岗位申请同一口径。审批通过后落地，不再过直接分配的审批闸。
    // 范围变更到挂这类角色的岗位（技术推导）下限同为「高」一档，平台管理员发起的维持上一级。
    @Test
    void crossProjectRoleRequestsByNonAdminsAreAcceptedWithTheHighTierAsTheFloor() {
        accessRepository.markRequiresApproval("governance.supervisor_review.view", "medium");
        accessRepository.markRequiresApproval(M002_CODE, "critical");
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        service = newAccessService(true, supervisorReviewService(reviewRepository));
        accessRepository.saveStaffLevelAssignment(new StaffLevelAssignment(
                "usr_admin", "lvl_m10_upper", null, "CNY", null, "seed-org", null, "test", Instant.parse("2026-06-14T00:00:00Z")));
        xpProjects("org_a", "org_b");
        xpUser("usr_xp_target", "org_a");
        xpRole("role_xp_daily", List.of(M002_VIEW), false, "org_b");
        xpRole("role_xp_medium", List.of("governance.supervisor_review.view"), false, "org_b");
        xpRole("role_xp_critical", List.of(M002_CODE), true);
        xpRole("role_plain_medium", List.of("governance.supervisor_review.view"), false);
        xpRole("role_plain_daily", List.of(M002_VIEW), false);
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("role_xp_daily", "permission_grant_l2");
        expected.put("role_xp_medium", "permission_grant_l2");
        expected.put("role_xp_critical", "permission_grant_l3");
        expected.put("role_plain_medium", "permission_grant_l1");

        xpActAs("usr_requester");
        Map<String, AccessGrantRequestRecord> pending = new LinkedHashMap<>();
        expected.forEach((role, policy) -> {
            AccessGrantRequestRecord request = service.applyGrant(xpRoleApplication("xp-u7-" + role, role));
            assertThat(request.status()).as(role).isEqualTo("pending");
            assertThat(reviewRepository.reviews.get(request.reviewId()).policyCode()).as(role).isEqualTo(policy);
            pending.put(role, request);
        });
        xpExpect(() -> service.applyGrant(xpRoleApplication("xp-u7-plain-daily", "role_plain_daily")),
                400, "GRANT_APPROVAL_NOT_REQUIRED", "请直接分配");

        authenticateAsPlatformAdmin("seed-org");
        xpExpect(() -> service.applyGrant(xpRoleApplication("xp-u7-admin-daily", "role_xp_daily")),
                400, "GRANT_APPROVAL_NOT_REQUIRED", "请直接分配");
        AccessGrantRequestRecord applied = service.reviewGrant(pending.get("role_xp_daily").id(), m002Approve("xp-u7-review-daily"));
        assertThat(applied.status()).isEqualTo("applied");
        assertThat(accessRepository.userRoles.get("usr_xp_target")).contains("role_xp_daily");

        xpTemplate("tpl_xp_pos", null);
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "xp-u7-map", null, "xp_patrol", "lvl_m1_lower", "role_xp_daily", "tpl_xp_pos", "active"));
        for (String mover : List.of("usr_xp_seat", "usr_xp_move_staff", "usr_xp_move_admin")) {
            xpUser(mover, "org_a");
        }
        authenticateAsOrgUser("usr_requester", "usr_requester", "org_a");
        AccessGrantRequestRecord position = service.applyGrant(new AccessGrantApplyRequest(
                "xp-u7-position", "M736", List.of(), "user_position", "usr_xp_seat", null, "xp_patrol", "lvl_m1_lower",
                null, null, null, null, null, null, false, "岗位申请"));
        assertThat(reviewRepository.reviews.get(position.reviewId()).policyCode()).isEqualTo("permission_grant_l2");

        AccessGrantRequestRecord byStaff = service.changeUserScope("usr_xp_move_staff", new UserScopeChangeRequest(
                "xp-u7-scope-staff", null, "org_a", "merchant_x", "xp_patrol", "lvl_m1_lower", null, null, null, "调到商户", null));
        assertThat(reviewRepository.reviews.get(byStaff.reviewId()).policyCode()).isEqualTo("permission_grant_l2");
        authenticateAsPlatformAdmin("seed-org");
        AccessGrantRequestRecord byAdmin = service.changeUserScope("usr_xp_move_admin", new UserScopeChangeRequest(
                "xp-u7-scope-admin", null, "org_a", "merchant_x", "xp_patrol", "lvl_m1_lower", null, null, null, "调到商户", null));
        assertThat(reviewRepository.reviews.get(byAdmin.reviewId()).policyCode()).isEqualTo("permission_grant_l1");
    }

    // M736-U8（操作员 2026-09-24 裁决 M001-F01「所有可以跨项目的角色都可以跨项目处理」；M001-F09「项目自己的模板只能套给本项目员工；
    // 平台管理员要跨项目用，先复制一份（复制件归平台，只有平台管理员能改）再套」）：经可跨项目角色持模板维护、套用权限的人复制乙项目的
    // 模板——成功，复制件归他本人项目，再复制一次同样（第二次）；丙项目没被勾中，复制——拒；把复制件套给乙项目员工——拒（项目模板只套本项目员工），套给本项目
    // 员工——成功；改平台模板——拒。
    @Test
    void crossProjectHolderCopiesATemplateOfAReachableProjectAndTheCopyStaysWithHisOwnProject() {
        xpProjects("org_a", "org_b", "org_c");
        xpRole("role_xp_tpl", List.of("access.permission_template.manage", "access.permission_template.apply", M002_VIEW),
                false, "org_a", "org_b");
        xpUser("usr_xp_a", "org_a");
        xpUser("usr_xp_b", "org_b");
        xpTemplate("tpl_xp_b", "org_b");
        xpTemplate("tpl_xp_c", "org_c");
        xpTemplate("tpl_xp_platform", null);
        xpActAs("usr_xp_holder", "role_xp_tpl");

        PermissionTemplate copy = service.copyPermissionTemplate("tpl_xp_b", xpTemplateRequest("xp-u8-copy"));
        assertThat(accessRepository.templates.get(copy.id()).ownerScopeId()).isEqualTo("org_a");
        assertThat(accessRepository.templates.get(copy.id()).sourceTemplateId()).isEqualTo("tpl_xp_b");
        PermissionTemplate again = service.copyPermissionTemplate("tpl_xp_b", xpTemplateRequest("xp-u8-copy-again")); // 第二次
        assertThat(again.id()).isNotEqualTo(copy.id());
        assertThat(accessRepository.templates.get(again.id()).ownerScopeId()).isEqualTo("org_a");
        xpExpect(() -> service.copyPermissionTemplate("tpl_xp_c", xpTemplateRequest("xp-u8-copy-c")),
                403, "CROSS_ORG_FORBIDDEN", "不能复制其他组织的模板");

        xpExpect(() -> service.applyPermissionTemplate(copy.id(), new PermissionTemplateApplyRequest(
                        "xp-u8-apply-b", "apply", List.of(), copy.id(), List.of("usr_xp_b"), "merge")),
                403, "CROSS_ORG_FORBIDDEN", "不能应用其他组织的模板");
        assertThat(service.applyPermissionTemplate(copy.id(), new PermissionTemplateApplyRequest(
                "xp-u8-apply-a", "apply", List.of(), copy.id(), List.of("usr_xp_a"), "merge")).applications()).hasSize(1);
        assertThat(accessRepository.templateApplications).extracting(PermissionTemplateApplication::targetId).containsExactly("usr_xp_a");

        xpExpect(() -> service.updatePermissionTemplate("tpl_xp_platform", xpTemplateRequest("xp-u8-edit-platform")),
                403, "CROSS_ORG_FORBIDDEN", "不能修改其他组织的模板");
    }

    // M736-U9（同一追加句的下一句：「……平台公司员工、平台管理员、最高权限、外部机构账号一律碰不到。」；建账号入口只能建外部机构账号）：
    // 持勾了「可以查看全部项目」的角色的人在乙项目下建外部机构账号——仍拒、不建账号，换幂等键再来仍拒；在本人项目建照旧成功。
    @Test
    void crossProjectHolderStillCannotCreateAnExternalAccountInAnotherProject() {
        xpProjects("org_a", "org_b");
        xpRole("role_xp_all", List.of("access.user.manage", XP_ASSIGN), true);
        xpActAs("usr_xp_holder", "role_xp_all");

        for (int round = 1; round <= 2; round++) {
            String key = "xp-u9-b-" + round;
            xpExpect(() -> service.createAccessUser(new AccessUserRequest(key, null, List.of(), "xp_vendor_b", "password", null,
                            "outsourced_vendor", "org_b", null, "active")),
                    403, "CROSS_ORG_FORBIDDEN", "不能在其他组织下创建用户");
        }
        assertThat(accessRepository.users.values()).noneMatch(user -> "xp_vendor_b".equals(user.username()));
        assertThat(service.createAccessUser(new AccessUserRequest("xp-u9-a", null, List.of(), "xp_vendor_a", "password", null,
                "outsourced_vendor", "org_a", null, "active")).organizationId()).isEqualTo("org_a");
    }

    // M736-F07（同一追加句的下一句：「……平台公司员工、平台管理员、最高权限、外部机构账号一律碰不到。」；M001-F16 更正段「修改在用模板
    // 等同给在用员工增减权限」）：乙项目的项目自有模板套给乙项目的外部机构账号；经勾了乙项目的角色持模板维护权限的甲项目员工修改该模板——
    // 拒（403 CROSS_ORG_FORBIDDEN），换幂等键再来仍拒，模板条目、该账号的有效权限都不变，不写逐人留痕。对照：只被乙项目普通员工在用的
    // 模板，他照常能改并逐人留痕；乙项目自己的人改这张外部机构账号在用的本项目模板照常成功。
    @Test
    void crossProjectHolderCannotEditATemplateHeldByAnExternalAccountOfTheTickedProject() {
        xpProjects("org_a", "org_b");
        xpRole("role_xp_tpl", List.of("access.permission_template.manage", M002_VIEW), false, "org_b");
        xpUser("usr_xp_b", "org_b");
        accessRepository.users.put("usr_xp_vendor_b", new AccessUser("usr_xp_vendor_b", "usr_xp_vendor_b", "outsourced_vendor", "org_b",
                null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        xpTemplate("tpl_xp_vendor", "org_b");
        xpTemplate("tpl_xp_staff", "org_b");
        accessRepository.applyTemplate("tpl_xp_vendor", List.of("usr_xp_vendor_b"), null, null, false, "usr_admin");
        accessRepository.applyTemplate("tpl_xp_staff", List.of("usr_xp_b"), null, null, false, "usr_admin");
        xpActAs("usr_xp_holder", "role_xp_tpl");

        for (int round = 1; round <= 2; round++) {
            String key = "xp-f07-vendor-" + round;
            xpExpect(() -> service.updatePermissionTemplate("tpl_xp_vendor", namedRequest(key, "跨项目模板", "base")),
                    403, "CROSS_ORG_FORBIDDEN", "不能为其他组织的用户分配权限：usr_xp_vendor_b");
        }
        assertThat(accessRepository.items.get("tpl_xp_vendor")).extracting(PermissionTemplateItem::permissionCode).containsExactly(M002_VIEW);
        assertThat(accessRepository.findPermissionEffects("usr_xp_vendor_b", Instant.now()))
                .extracting(AccessRepository.PermissionEffect::permissionCode).containsExactly(M002_VIEW);
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).doesNotContain("permission_template.effect_changed");

        service.updatePermissionTemplate("tpl_xp_staff", namedRequest("xp-f07-staff", "跨项目模板", "base"));
        assertThat(accessRepository.items.get("tpl_xp_staff")).isEmpty();
        assertThat(effectChanged("xp-f07-staff")).extracting(AuditEvent::businessId).containsExactly("usr_xp_b");

        authenticateAsOrgUser("usr_xp_b", "usr_xp_b", "org_b");
        service.updatePermissionTemplate("tpl_xp_vendor", namedRequest("xp-f07-local", "跨项目模板", "base"));
        assertThat(accessRepository.items.get("tpl_xp_vendor")).isEmpty();
        assertThat(effectChanged("xp-f07-local")).extracting(AuditEvent::businessId).containsExactly("usr_xp_vendor_b");
    }

    // M736-U10（D-032：平台管理员不受项目隔离约束）：库里有可跨项目角色、项目集合不为空时，平台管理员各入口照旧——全量列角色、模板
    // （含平台模板）与账号，处理任何归属的员工（含不在项目集合里的平台公司员工），改平台模板；算可触及项目时不看项目集合。
    @Test
    void platformAdminEntriesAreUnchangedByCrossProjectRoles() {
        xpProjects("org_a", "org_b");
        xpRole("role_xp_cross", List.of(M002_VIEW), false, "org_b");
        accessRepository.roles.put("role_xp_org_b", new RoleRecord("role_xp_org_b", "role_xp_org_b", null, "project", "active",
                Instant.parse("2026-06-14T00:00:00Z"), List.of(M002_VIEW), "org_b"));
        xpTemplate("tpl_xp_b", "org_b");
        xpTemplate("tpl_xp_platform", null);
        xpUser("usr_xp_b", "org_b");
        xpUser("usr_xp_hq", "platform-company");
        for (String target : List.of("usr_xp_b", "usr_xp_hq")) {
            accessRepository.userRoles.put(target, new LinkedHashSet<>(Set.of("role_xp_cross")));
        }

        AuthorizationService.ProjectReach reach = authorizationService.projectReach(authorizationService.currentActor(), XP_ASSIGN);
        assertThat(reach.platformAdmin()).isTrue();
        assertThat(reach.covers("platform-company")).isTrue();
        assertThat(service.listRoles().items()).extracting(RoleRecord::id)
                .contains("role_xp_cross", "role_xp_org_b", "explicit_admin_grants");
        assertThat(service.listPermissionTemplates().items()).extracting(PermissionTemplate::id).contains("tpl_xp_b", "tpl_xp_platform");
        assertThat(service.listAccessUsers().items()).extracting(AccessUser::id).contains("usr_xp_b", "usr_xp_hq");
        for (String target : List.of("usr_xp_b", "usr_xp_hq")) {
            service.unassignUserRole(new RoleAssignmentRequest("xp-u10-unassign-" + target, null, target, "role_xp_cross"));
            assertThat(accessRepository.userRoles.get(target)).as(target).isEmpty();
            assertThat(service.effectivePermissionsForUser(target).userId()).isEqualTo(target);
        }
        assertThat(service.updatePermissionTemplate("tpl_xp_platform", xpTemplateRequest("xp-u10-edit")).ownerScopeId()).isNull();
    }

    // M736-U11（复核 I-1；治理地图第 651 行「员工单独授予、解除员工显式拒绝、岗位模板高危权限释放这三类授权申请，只能由具备分配/取消分配
    // 角色权限的人发起」，入口对申请权限与分配角色权限一起核验）：三类申请的可触及项目取两码的交集——申请权限来自勾了全部项目的角色、
    // 分配角色权限只来自本项目的普通角色时，对乙项目员工发起三类申请 403 CROSS_ORG_FORBIDDEN，不建单、不开复核单，第二轮仍被拒；同一个人的临时权限
    // 申请只核申请权限，照常受理；分配角色权限也改由勾了乙项目的角色持有后，三类申请过范围闸（员工单独授予建单成功）。
    @Test
    void theThreeRoleAssignInitiatedRequestsNeedBothCodesToReachTheTargetProject() {
        accessRepository.markRequiresApproval(M002_CODE, "high");
        InMemorySupervisorReviewRepository reviewRepository = new InMemorySupervisorReviewRepository();
        service = newAccessService(true, supervisorReviewService(reviewRepository));
        xpProjects("org_a", "org_b");
        xpUser("usr_xp_b", "org_b");
        xpRole("role_xp_apply_all", List.of(XP_APPLY), true);
        xpRole("role_xp_assign_local", List.of(XP_ASSIGN), false);
        xpRole("role_xp_assign_b", List.of(XP_ASSIGN), false, "org_b");
        xpActAs("usr_xp_applicant", "role_xp_apply_all", "role_xp_assign_local");

        for (int round = 1; round <= 2; round++) {
            for (String type : List.of("user_permission_grant", "user_permission_deny_removal", "position_high_risk_release")) {
                String key = "xp-u11-" + type + "-" + round;
                xpExpect(() -> service.applyGrant(m002Apply(key, type, "usr_xp_b", List.of(M002_CODE))),
                        403, "CROSS_ORG_FORBIDDEN", "usr_xp_b");
            }
        }
        assertThat(accessRepository.grantRequests).isEmpty();
        assertThat(reviewRepository.reviews).isEmpty();

        AccessGrantRequestRecord temporary = service.applyGrant(new AccessGrantApplyRequest(
                "xp-u11-temporary", "M736", List.of(), "temporary_permission", "usr_xp_b", null, null, null, null, null, null,
                List.of(M002_VIEW), Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-15T00:00:00Z"), false, "临时顶班"));
        assertThat(temporary.status()).isEqualTo("pending");

        xpActAs("usr_xp_applicant", "role_xp_apply_all", "role_xp_assign_b");
        AccessGrantRequestRecord grant = service.applyGrant(
                m002Apply("xp-u11-both-grant", "user_permission_grant", "usr_xp_b", List.of(M002_CODE)));
        assertThat(grant.status()).isEqualTo("pending");
        assertThat(grant.targetUserId()).isEqualTo("usr_xp_b");
    }

    // M736-U13（M002-F34「其他人不能给最高权限账号设置显式拒绝」；同一追加句的下一句「……最高权限……一律碰不到」）：最高权限账号归属
    // 就在操作人本人项目时，过了范围闸后得 MASTER_AUTHORITY_DENY_FORBIDDEN（两道闸的先后不变）；归属在别的项目时，勾了「可以查看全部
    // 项目」的持有者先被范围闸拒（CROSS_ORG_FORBIDDEN）、不写拒绝行，换幂等键再来仍拒——可跨项目角色碰不到最高权限账号；同项目的普通员工照常能设拒绝。
    @Test
    void crossProjectHolderIsStoppedByTheScopeGateBeforeTheMasterAuthorityDenyRule() {
        accessRepository.addPermission(XP_ASSIGN, "high", true);
        xpProjects("org_a", "org_b");
        xpUser("usr_xp_b", "org_b");
        xpRole("role_xp_all", List.of(XP_ASSIGN), true);
        xpActAs("usr_xp_holder", "role_xp_all");
        String master = SessionIssuer.MASTER_USER_ID;

        accessRepository.users.put(master, new AccessUser(master, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                "org_a", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        xpExpect(() -> service.setUserPermissionOverrides(master, new UserPermissionOverrideRequest(
                        "xp-u13-own-project", null, List.of(XP_ASSIGN), "deny", "给最高权限账号设拒绝")),
                403, "MASTER_AUTHORITY_DENY_FORBIDDEN", "其他人不能给最高权限账号设置显式拒绝");

        accessRepository.users.put(master, new AccessUser(master, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                "org_b", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        for (int round = 1; round <= 2; round++) {
            String key = "xp-u13-other-project-" + round;
            xpExpect(() -> service.setUserPermissionOverrides(master, new UserPermissionOverrideRequest(
                            key, null, List.of(XP_ASSIGN), "deny", "给最高权限账号设拒绝")),
                    403, "CROSS_ORG_FORBIDDEN", master);
        }
        assertThat(overrideSources(master, XP_ASSIGN, AccessRepository.Effect.DENY)).isEmpty();

        service.setUserPermissionOverrides("usr_xp_b", new UserPermissionOverrideRequest(
                "xp-u13-plain", null, List.of(XP_ASSIGN), "deny", "停权"));
        assertThat(overrideSources("usr_xp_b", XP_ASSIGN, AccessRepository.Effect.DENY))
                .containsExactly(AccessRepository.Source.USER_OVERRIDE);
    }

    // ── M736 第二步（操作员 2026-10-08 确认的治理地图第 703 行追加句：「可跨项目角色分配出去以后，可以直接在原角色上增加或减少可查看的
    //    项目：增加的，所有持有人要各自重新审批通过，才能查看新增的项目；减少的，也要审批通过后才生效，审批期间持有人仍能查看。」）──
    //    夹具 xp2Fixture：授出审批开启；真实判定＋内存仓＋真实主管复核服务，复核单到终态的事件同步转给被测服务的监听（生产里由 Spring
    //    同步送达）。甲、乙、丙、丁项目＝org_a、org_b、org_c、org_d。角色管理人 usr_xp2_mgr 在甲项目，经没勾选的普通角色持建角色、
    //    申请授权、分配角色三码；他与平台管理员 usr_admin 的上两级都是 usr_xp2_sup1、usr_xp2_sup2（授权政策「高」一档＝上两级）。
    //    夹具返回时登录的是角色管理人。各条注释里的「拿掉某处即红」是任务书给的变异点。

    private static final String XP_MANAGE = "access.role.manage";
    private InMemorySupervisorReviewRepository xp2Reviews;
    private SupervisorReviewService xp2ReviewService;

    private void xp2Fixture() {
        xp2Reviews = new InMemorySupervisorReviewRepository();
        xp2Reviews.superiors.put("usr_xp2_mgr", "usr_xp2_sup1");
        xp2Reviews.superiors.put("usr_admin", "usr_xp2_sup1");
        xp2Reviews.superiors.put("usr_xp2_sup1", "usr_xp2_sup2");
        xp2ReviewService = new SupervisorReviewService(authorizationService,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()), xp2Reviews,
                new AuditEventService(auditEventRepository), event -> {
                    if (event instanceof SupervisorReviewDecisionRecord decision) {
                        service.onSupervisorReviewDecided(decision);
                    }
                });
        service = newAccessService(true, xp2ReviewService);
        xpProjects("org_a", "org_b", "org_c", "org_d");
        xpUser("usr_xp2_sup1", "org_a");
        xpUser("usr_xp2_sup2", "org_a");
        xpRole("role_xp2_mgr", List.of(XP_MANAGE, XP_APPLY, XP_ASSIGN, M002_VIEW), false);
        xpActAs("usr_xp2_mgr", "role_xp2_mgr");
    }

    // 归属给定项目的角色（平台角色用 xpRole）。
    private void xp2Role(String roleId, String organizationId, List<String> codes, boolean viewAll, String... ticked) {
        accessRepository.roles.put(roleId, new RoleRecord(roleId, roleId, null, "project", "active",
                Instant.parse("2026-06-14T00:00:00Z"), codes, organizationId, viewAll, List.of(ticked)));
    }

    // 给定项目的员工，直接持有给定角色（夹具直接落行，不换登录人）。
    private void xp2Holder(String userId, String organizationId, String roleId) {
        xpUser(userId, organizationId);
        accessRepository.assignUserRole(userId, roleId);
    }

    // 甲项目的商户账号（带商户归属）。
    private void xp2MerchantUser(String userId, String merchantId) {
        accessRepository.users.put(userId, new AccessUser(userId, userId, "merchant", "org_a", merchantId, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
    }

    // 平台模板，只带一个须审批的高危码：挂它的岗位定岗时先挂起、开岗位高危释放单。
    private void xp2HighTemplate(String id) {
        accessRepository.saveTemplate(new PermissionTemplate(
                id, id, id, null, "base", null, "platform", null, "merge", null, null, false,
                "active", null, null, null, Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")),
                List.of(new PermissionTemplateItem(id, M002_CODE, AccessRepository.Effect.GRANT, "project", false)));
    }

    // 以账号行里的归属登录（会话与账号行一致），不动他的角色。
    private void xp2Login(String userId) {
        authenticateAsOrgUser(userId, userId, accessRepository.users.get(userId).organizationId());
    }

    private static RoleViewableProjectsRequest xp2Delta(String key, Boolean addAll, List<String> add, Boolean removeAll, List<String> remove) {
        return new RoleViewableProjectsRequest(key, "M736", addAll, add, removeAll, remove, null);
    }

    private static RoleViewableProjectsRequest xp2Add(String key, String... ids) {
        return xp2Delta(key, null, List.of(ids), null, null);
    }

    private static RoleViewableProjectsRequest xp2Remove(String key, String... ids) {
        return xp2Delta(key, null, null, null, List.of(ids));
    }

    private static AccessGrantApplyRequest xp2Reapproval(String key, String userId, String roleId) {
        return new AccessGrantApplyRequest(key, "M736", List.of(), "role_project_reapproval", userId, roleId, null, null, null, null, null,
                null, null, null, false, "补审新增项目");
    }

    private Set<String> xp2Holds(String userId, String roleId) {
        return accessRepository.findRoleViewableProjectHolds(userId).getOrDefault(roleId, Set.of());
    }

    // 某条目的减少在待审时记着的主管复核单号；没有待审的减少为空。
    private String xp2RemovalReview(String roleId, String viewableId) {
        return accessRepository.viewableRemovals.getOrDefault(roleId, Map.of()).get(viewableId);
    }

    private List<AuditEvent> xp2Events(String eventName) {
        return auditEventRepository.events.stream().filter(event -> eventName.equals(event.eventName())).toList();
    }

    // 以该员工身份登录，按给定权限码取他的可触及项目（真实判定）。
    private AuthorizationService.ProjectReach xp2ReachOf(String userId, String code) {
        xp2Login(userId);
        return authorizationService.projectReach(authorizationService.currentActor(), code);
    }

    // 以审批链上某一级的身份裁决主管复核单（通用主管复核入口调的同一个服务方法）。
    private SupervisorReviewDecisionRecord xp2Decide(String approver, String reviewId, String key, String decision) {
        xp2Login(approver);
        return xp2ReviewService.approve(reviewId, m002ChainDecision(key, decision));
    }

    // 上两级依次批准一张授权申请：第一级批完仍待审；返回末级批完后的申请。
    private AccessGrantRequestRecord xp2ApproveBoth(String requestId, String key) {
        xp2Login("usr_xp2_sup1");
        assertThat(service.reviewGrant(requestId, m002Approve(key + "-1")).status()).isEqualTo("pending");
        xp2Login("usr_xp2_sup2");
        return service.reviewGrant(requestId, m002Approve(key + "-2"));
    }

    // 角色管理人对三名新员工各提一张指向 role_xp2_cross 的待审申请：角色申请、岗位申请、范围变更（新范围的岗位 xp2_patrol 挂该角色）。
    private Map<String, AccessGrantRequestRecord> xp2ThreeRequests(String tag) {
        xp2Login("usr_xp2_mgr");
        Map<String, AccessGrantRequestRecord> requests = new LinkedHashMap<>();
        for (String type : List.of("user_role", "user_position", "user_scope_change")) {
            String target = "usr_" + tag.replace('-', '_') + "_" + type;
            xpUser(target, "org_a");
            requests.put(type, switch (type) {
                case "user_role" -> service.applyGrant(new AccessGrantApplyRequest(tag + "-role", "M736", List.of(), "user_role", target,
                        "role_xp2_cross", null, null, null, null, null, null, null, null, false, "角色申请"));
                case "user_position" -> service.applyGrant(new AccessGrantApplyRequest(tag + "-position", "M736", List.of(), "user_position",
                        target, null, "xp2_patrol", "lvl_m1_lower", null, null, null, null, null, null, false, "岗位申请"));
                default -> service.changeUserScope(target, new UserScopeChangeRequest(tag + "-scope", null, "org_a", "merchant_x",
                        "xp2_patrol", "lvl_m1_lower", null, null, null, "调到商户", null));
            });
        }
        requests.forEach((type, request) -> {
            assertThat(request.status()).as(type).isEqualTo("pending");
            assertThat(request.requestType()).isEqualTo(type);
            assertThat(request.targetRoleId()).as(type).isEqualTo("role_xp2_cross");
        });
        return requests;
    }

    // M736-S1（同上追加句「增加的，所有持有人要各自重新审批通过，才能查看新增的项目」；操作员 2026-10-03 所选选项原文「直接在原角色上勾 C；
    // 但张三等所有持有人要各自重新审批通过，才能看 C。」）：项目角色勾着甲、乙，持有人三个——直接分配的、经岗位得到的、操作人自己。
    // 加丙：角色当场是甲、乙、丙；三人各一条对丙的待批；不建任何主管复核单与授权申请；现状里三人都列为待重新审批；留痕带增加的条目与
    // 三人编号。拿掉写待批的循环即红（待批为空）。
    @Test
    void addingViewableProjectsWritesTheRoleAndHoldsThemForEveryExistingHolder() {
        xp2Fixture();
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_a", "org_b");
        xp2Holder("usr_xp2_direct", "org_a", "role_xp2");
        xpUser("usr_xp2_seat", "org_a");
        accessRepository.positionRoles.put("usr_xp2_seat", "role_xp2");
        accessRepository.assignUserRole("usr_xp2_mgr", "role_xp2");
        List<String> holders = List.of("usr_xp2_direct", "usr_xp2_mgr", "usr_xp2_seat");

        RoleViewableProjectsView view = service.updateRoleViewableProjects("role_xp2", xp2Add("xp2-s1-add", "org_c"));

        assertThat(accessRepository.roles.get("role_xp2").viewableProjectIds()).containsExactly("org_a", "org_b", "org_c");
        assertThat(accessRepository.roles.get("role_xp2").viewAllProjects()).isFalse();
        assertThat(view.viewableProjectIds()).containsExactly("org_a", "org_b", "org_c");
        assertThat(view.pendingRemovalIds()).isEmpty();
        for (String holder : holders) {
            assertThat(accessRepository.findRoleViewableProjectHolds(holder)).as(holder).isEqualTo(Map.of("role_xp2", Set.of("org_c")));
        }
        assertThat(accessRepository.listRoleViewableProjectHolds("role_xp2").keySet()).containsExactlyElementsOf(holders);
        assertThat(view.pendingReapprovals()).extracting(RoleViewableProjectsView.PendingReapproval::userId)
                .containsExactlyElementsOf(holders);
        assertThat(view.pendingReapprovals()).allSatisfy(pending -> {
            assertThat(pending.viewableIds()).containsExactly("org_c");
            assertThat(pending.reapprovalPending()).isFalse();
        });
        assertThat(xp2Reviews.reviews).isEmpty();
        assertThat(accessRepository.grantRequests).isEmpty();
        assertThat(auditEventRepository.events).filteredOn(event -> "xp2-s1-add".equals(event.idempotencyKey()))
                .singleElement().satisfies(event -> {
                    assertThat(event.eventName()).isEqualTo("role.viewable_projects.changed");
                    assertThat(event.businessId()).isEqualTo("role_xp2");
                    assertThat(event.metadata().get("added_view_all_projects")).isEqualTo(false);
                    assertThat(event.metadata().get("added_viewable_ids")).isEqualTo(List.of("org_c"));
                    assertThat(event.metadata().get("removal_review_id")).isEqualTo("");
                    assertThat(event.metadata().get("held_user_ids")).isEqualTo("usr_xp2_direct,usr_xp2_mgr,usr_xp2_seat");
                });
    }

    // M736-S2（技术推导：待批只记改之前已持有的人；改之后才得到角色的人按当时的勾选）：加丙以后平台管理员把角色直接分给第四人——
    // 他没有待批条目，判定看得到丙；改之前已持有的那位仍看不到。在分配时写待批即红。
    @Test
    void holderWhoGetsTheRoleAfterTheChangeHasNoHold() {
        xp2Fixture();
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_a", "org_b");
        xp2Holder("usr_xp2_direct", "org_a", "role_xp2");
        service.updateRoleViewableProjects("role_xp2", xp2Add("xp2-s2-add", "org_c"));
        xpUser("usr_xp2_later", "org_a");

        authenticateAsPlatformAdmin("seed-org");
        service.assignUserRole(new RoleAssignmentRequest("xp2-s2-assign", null, "usr_xp2_later", "role_xp2"));

        assertThat(accessRepository.userRoles.get("usr_xp2_later")).containsExactly("role_xp2");
        assertThat(xp2Holds("usr_xp2_later", "role_xp2")).isEmpty();
        assertThat(accessRepository.listRoleViewableProjectHolds("role_xp2").keySet()).containsExactly("usr_xp2_direct");
        assertThat(xp2ReachOf("usr_xp2_later", M002_VIEW).covers("org_c")).as("改之后才得到角色的人按当时的勾选").isTrue();
        assertThat(xp2ReachOf("usr_xp2_direct", M002_VIEW).covers("org_c")).as("改之前已持有的人").isFalse();
    }

    // M736-S3（技术推导，依据同一追加句「增加或减少可查看的项目」：把「可以查看全部项目」勾上按增加办，逐个勾选的清单始终保留）：
    // 清单是甲、乙，有持有人。勾上全部的同时又带增加清单——400、角色不变、不写待批；只勾上全部——角色「全部」为真、清单仍是甲、乙，
    // 原持有人一条「全部」的待批，他仍只到清单里的项目，不建复核单。勾全部时清空清单即红。
    @Test
    void addingViewAllKeepsTheListAndHoldsTheAllEntry() {
        xp2Fixture();
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_a", "org_b");
        xp2Holder("usr_xp2_direct", "org_a", "role_xp2");

        xpExpect(() -> service.updateRoleViewableProjects("role_xp2", xp2Delta("xp2-s3-both", true, List.of("org_c"), null, null)),
                400, "VALIDATION_FAILED", "勾了“可以查看全部项目”就不能再逐个勾选项目");
        assertThat(accessRepository.roles.get("role_xp2").viewAllProjects()).isFalse();
        assertThat(accessRepository.roles.get("role_xp2").viewableProjectIds()).containsExactly("org_a", "org_b");
        assertThat(accessRepository.viewableHolds).isEmpty();

        RoleViewableProjectsView view = service.updateRoleViewableProjects("role_xp2", xp2Delta("xp2-s3-all", true, null, null, null));

        assertThat(view.viewAllProjects()).isTrue();
        assertThat(view.viewableProjectIds()).containsExactly("org_a", "org_b");
        assertThat(accessRepository.roles.get("role_xp2").viewAllProjects()).isTrue();
        assertThat(accessRepository.roles.get("role_xp2").viewableProjectIds()).containsExactly("org_a", "org_b");
        assertThat(xp2Holds("usr_xp2_direct", "role_xp2")).containsExactly(AuthorizationService.ALL_PROJECTS);
        assertThat(xp2Reviews.reviews).isEmpty();
        AuthorizationService.ProjectReach reach = xp2ReachOf("usr_xp2_direct", M002_VIEW);
        assertThat(List.of(reach.covers("org_b"), reach.covers("org_c"), reach.allProjects())).containsExactly(true, false, false);
    }

    // M736-S4（技术推导：角色勾着全部、这个人又没有「全部」待批的，本来就看得到全部，新条目不用他再审批）：接 S3 的状态，再有一人
    // 在勾上全部以后才得到角色。一次提交去掉全部并加丙：丙当场在清单里；有「全部」待批的人多一条对丙的待批，后来的那人没有；
    // 「全部」仍为真（去掉全部在待审），建了一张复核单。去掉「没有全部待批就不写」的判断即红（后来的人多出待批）。
    @Test
    void addingUnderViewAllHoldsOnlyHoldersStillWaitingForTheAllEntry() {
        xp2Fixture();
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_a", "org_b");
        xp2Holder("usr_xp2_direct", "org_a", "role_xp2");
        service.updateRoleViewableProjects("role_xp2", xp2Delta("xp2-s4-all", true, null, null, null));
        xp2Holder("usr_xp2_later", "org_a", "role_xp2");

        RoleViewableProjectsView view = service.updateRoleViewableProjects("role_xp2",
                xp2Delta("xp2-s4-mixed", null, List.of("org_c"), true, null));

        RoleRecord role = accessRepository.roles.get("role_xp2");
        assertThat(role.viewableProjectIds()).containsExactly("org_a", "org_b", "org_c");
        assertThat(role.viewAllProjects()).as("去掉全部还在待审").isTrue();
        assertThat(xp2Holds("usr_xp2_direct", "role_xp2")).containsExactlyInAnyOrder(AuthorizationService.ALL_PROJECTS, "org_c");
        assertThat(xp2Holds("usr_xp2_later", "role_xp2")).isEmpty();
        assertThat(view.viewAllProjects()).isTrue();
        assertThat(view.pendingRemovalIds()).containsExactly(AuthorizationService.ALL_PROJECTS);
        assertThat(xp2Reviews.reviews).hasSize(1);
    }

    // M736-S5（同一追加句「减少的，也要审批通过后才生效，审批期间持有人仍能查看」；操作员 2026-10-03 所选选项原文「减少也要审批通过后
    // 才生效；审批期间持有人仍能看 C。」；审批一次、档位取「高」一档为技术推导）：角色勾着甲、乙、丙，有持有人，操作人有两级上级。
    // 减丙：读回角色仍是甲、乙、丙，持有人照常看得到丙；只多一张待审的主管复核单——动作名、对象类型、对象编号＝角色、「高」一档、
    // 两个上级节点、没有受益人；现状里丙列为减少待审；不写待批、不建授权申请。提交时就删行即红。
    @Test
    void removingOnlyMarksAndOpensOneReviewAndTheRoleStaysUntouched() {
        xp2Fixture();
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_a", "org_b", "org_c");
        xp2Holder("usr_xp2_direct", "org_a", "role_xp2");

        RoleViewableProjectsView view = service.updateRoleViewableProjects("role_xp2", xp2Remove("xp2-s5-remove", "org_c"));

        assertThat(accessRepository.findRoleById("role_xp2").orElseThrow().viewableProjectIds()).containsExactly("org_a", "org_b", "org_c");
        assertThat(view.viewableProjectIds()).containsExactly("org_a", "org_b", "org_c");
        assertThat(view.pendingRemovalIds()).containsExactly("org_c");
        assertThat(view.pendingReapprovals()).isEmpty();
        assertThat(xp2Reviews.reviews).hasSize(1);
        SupervisorReviewDecisionRecord review = xp2Reviews.reviews.values().iterator().next();
        assertThat(review.actionType()).isEqualTo("access.role_viewable_projects.reduce");
        assertThat(review.objectType()).isEqualTo("role_viewable_projects");
        assertThat(review.objectId()).isEqualTo("role_xp2");
        assertThat(review.policyCode()).isEqualTo("permission_grant_l2");
        assertThat(review.reviewStatus()).isEqualTo("pending_review");
        assertThat(review.initiatedBy()).isEqualTo("usr_xp2_mgr");
        assertThat(xp2Reviews.listChainNodes(review.reviewId())).extracting(node -> node.approverKind() + "/" + node.approverId())
                .containsExactly("user/usr_xp2_sup1", "user/usr_xp2_sup2");
        assertThat(xp2Reviews.schedules.get(review.reviewId()).beneficiaryId()).isNull();
        assertThat(xp2RemovalReview("role_xp2", "org_c")).isEqualTo(review.reviewId());
        assertThat(accessRepository.viewableHolds).isEmpty();
        assertThat(accessRepository.grantRequests).isEmpty();
        assertThat(auditEventRepository.events).filteredOn(event -> "xp2-s5-remove".equals(event.idempotencyKey()))
                .singleElement().satisfies(event -> {
                    assertThat(event.eventName()).isEqualTo("role.viewable_projects.changed");
                    assertThat(event.metadata().get("removal_viewable_ids")).isEqualTo(List.of("org_c"));
                    assertThat(event.metadata().get("removal_review_id")).isEqualTo(review.reviewId());
                });
        assertThat(xp2ReachOf("usr_xp2_direct", M002_VIEW).covers("org_c")).as("审批期间持有人仍能查看").isTrue();
    }

    // M736-S6（同上两句原文）：减少的复核单第一级通过——角色不变、之后才得到角色的人照常看得到丙；末级通过的那一下——丙从角色上去掉、
    // 老持有人对丙的待批随之清掉、谁都看不到丙、留痕带复核单号与去掉的条目。另起一笔减乙：第一级驳回——角色仍有乙、待减标记清空、
    // 不再多留减少的痕；同样的减少可以再提。监听里不判通过与否即红（驳回也删）。
    @Test
    void removalLandsOnlyWhenTheWholeChainApprovesAndRejectionClearsTheMark() {
        xp2Fixture();
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_a", "org_b");
        xp2Holder("usr_xp2_direct", "org_a", "role_xp2");
        service.updateRoleViewableProjects("role_xp2", xp2Add("xp2-s6-add", "org_c"));
        xp2Holder("usr_xp2_later", "org_a", "role_xp2");
        service.updateRoleViewableProjects("role_xp2", xp2Remove("xp2-s6-remove", "org_c"));
        String reviewId = xp2RemovalReview("role_xp2", "org_c");

        assertThat(xp2Decide("usr_xp2_sup1", reviewId, "xp2-s6-first", "approved").reviewStatus()).isEqualTo("pending_review");
        assertThat(accessRepository.roles.get("role_xp2").viewableProjectIds()).containsExactly("org_a", "org_b", "org_c");
        assertThat(xp2Holds("usr_xp2_direct", "role_xp2")).containsExactly("org_c");
        assertThat(xp2ReachOf("usr_xp2_later", M002_VIEW).covers("org_c")).as("链上还有节点待审").isTrue();

        assertThat(xp2Decide("usr_xp2_sup2", reviewId, "xp2-s6-last", "approved").reviewStatus()).isEqualTo("approved");
        assertThat(accessRepository.roles.get("role_xp2").viewableProjectIds()).containsExactly("org_a", "org_b");
        assertThat(accessRepository.findRoleViewableProjectRemovals("role_xp2")).isEmpty();
        assertThat(xp2Holds("usr_xp2_direct", "role_xp2")).isEmpty();
        assertThat(xp2ReachOf("usr_xp2_later", M002_VIEW).covers("org_c")).isFalse();
        assertThat(xp2Events("role.viewable_projects.reduced")).singleElement().satisfies(event -> {
            assertThat(event.businessId()).isEqualTo("role_xp2");
            assertThat(event.metadata().get("review_id")).isEqualTo(reviewId);
            assertThat(event.metadata().get("removed_viewable_ids")).isEqualTo(List.of("org_c"));
        });

        xp2Login("usr_xp2_mgr");
        service.updateRoleViewableProjects("role_xp2", xp2Remove("xp2-s6-remove-b", "org_b"));
        String rejected = xp2RemovalReview("role_xp2", "org_b");
        assertThat(xp2Decide("usr_xp2_sup1", rejected, "xp2-s6-reject", "rejected").reviewStatus()).isEqualTo("rejected");
        assertThat(accessRepository.roles.get("role_xp2").viewableProjectIds()).containsExactly("org_a", "org_b");
        assertThat(accessRepository.findRoleViewableProjectRemovals("role_xp2")).isEmpty();
        assertThat(xp2Events("role.viewable_projects.reduced")).hasSize(1);
        xp2Login("usr_xp2_mgr");
        RoleViewableProjectsView again = service.updateRoleViewableProjects("role_xp2", xp2Remove("xp2-s6-remove-b-again", "org_b"));
        assertThat(again.pendingRemovalIds()).containsExactly("org_b");
        assertThat(xp2RemovalReview("role_xp2", "org_b")).isNotNull().isNotEqualTo(rejected);
    }

    // M736-S7（技术推导：请求体是差量，锁内与角色当前状态核对）：角色勾着甲、乙、丙，丙的减少在待审。再减丙、加已在角色上的甲、
    // 减不在角色上的条目——都是 409 INVALID_STATE，角色、复核单、待批都不变，各留一条失败痕；同一请求里加丁、减乙（都合法）受理。
    // 去掉差量核对即红（不报错）。
    @Test
    void staleOrRepeatedDeltaIsRefused() {
        xp2Fixture();
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_a", "org_b", "org_c");
        xp2Holder("usr_xp2_direct", "org_a", "role_xp2");
        service.updateRoleViewableProjects("role_xp2", xp2Remove("xp2-s7-remove", "org_c"));
        String message = "角色的可查看项目或待审批的减少已变化，请刷新后重试";

        xpExpect(() -> service.updateRoleViewableProjects("role_xp2", xp2Remove("xp2-s7-again", "org_c")), 409, "INVALID_STATE", message);
        xpExpect(() -> service.updateRoleViewableProjects("role_xp2", xp2Add("xp2-s7-present", "org_a")), 409, "INVALID_STATE", message);
        xpExpect(() -> service.updateRoleViewableProjects("role_xp2", xp2Remove("xp2-s7-absent", "org_z")), 409, "INVALID_STATE", message);
        assertThat(accessRepository.roles.get("role_xp2").viewableProjectIds()).containsExactly("org_a", "org_b", "org_c");
        assertThat(accessRepository.findRoleViewableProjectRemovals("role_xp2")).containsExactly("org_c");
        assertThat(xp2Reviews.reviews).hasSize(1);
        assertThat(accessRepository.viewableHolds).isEmpty();
        assertThat(xp2Events("role.viewable_projects.change_failed")).extracting(event -> event.metadata().get("error_code"))
                .containsExactly("INVALID_STATE", "INVALID_STATE", "INVALID_STATE");

        RoleViewableProjectsView accepted = service.updateRoleViewableProjects("role_xp2",
                xp2Delta("xp2-s7-valid", null, List.of("org_d"), null, List.of("org_b")));
        assertThat(accepted.viewableProjectIds()).containsExactly("org_a", "org_b", "org_c", "org_d");
        assertThat(accepted.pendingRemovalIds()).containsExactly("org_b", "org_c");
    }

    // M736-S8（同一追加句：增加、减少各按各的规则）：角色勾着甲、乙、丙。一次提交加丁、减丙——角色此刻是甲、乙、丙、丁；持有人
    // 对丁有待批、对丙照常看得到；只有丙带待减标记；只建一张复核单；留痕一条，带增加、减少的条目、复核单号与写了待批的人。
    @Test
    void mixedChangeAddsAtOnceAndSendsTheReductionForApproval() {
        xp2Fixture();
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_a", "org_b", "org_c");
        xp2Holder("usr_xp2_direct", "org_a", "role_xp2");

        RoleViewableProjectsView view = service.updateRoleViewableProjects("role_xp2",
                xp2Delta("xp2-s8-mixed", null, List.of("org_d"), null, List.of("org_c")));

        assertThat(accessRepository.roles.get("role_xp2").viewableProjectIds()).containsExactly("org_a", "org_b", "org_c", "org_d");
        assertThat(xp2Holds("usr_xp2_direct", "role_xp2")).containsExactly("org_d");
        assertThat(accessRepository.findRoleViewableProjectRemovals("role_xp2")).containsExactly("org_c");
        assertThat(view.pendingRemovalIds()).containsExactly("org_c");
        assertThat(xp2Reviews.reviews).hasSize(1);
        String reviewId = xp2RemovalReview("role_xp2", "org_c");
        assertThat(xp2Reviews.reviews.get(reviewId).actionType()).isEqualTo("access.role_viewable_projects.reduce");
        assertThat(auditEventRepository.events).filteredOn(event -> "xp2-s8-mixed".equals(event.idempotencyKey()))
                .singleElement().satisfies(event -> {
                    assertThat(event.eventName()).isEqualTo("role.viewable_projects.changed");
                    assertThat(event.metadata().get("added_viewable_ids")).isEqualTo(List.of("org_d"));
                    assertThat(event.metadata().get("removal_viewable_ids")).isEqualTo(List.of("org_c"));
                    assertThat(event.metadata().get("removal_review_id")).isEqualTo(reviewId);
                    assertThat(event.metadata().get("held_user_ids")).isEqualTo("usr_xp2_direct");
                });
        AuthorizationService.ProjectReach reach = xp2ReachOf("usr_xp2_direct", M002_VIEW);
        assertThat(List.of(reach.covers("org_c"), reach.covers("org_d"))).containsExactly(true, false);
    }

    // M736-S9（同一追加句与两条选项原文都没有按操作人区分）：平台管理员改平台角色、最高权限会话改本人项目的角色，各做一遍——
    // 加丙：原持有人照写待批；减乙：照样建一张待审的复核单、角色不变（没有「本人发起即生效」）。加一个平台管理员或最高权限直接删行的
    // 分支即红。
    @Test
    void platformAdminAndMasterSessionAreTreatedLikeAnyoneElse() {
        xp2Fixture();
        xpRole("role_xp2_platform", List.of(M002_VIEW), false, "org_a", "org_b");
        xp2Holder("usr_xp2_direct", "org_a", "role_xp2_platform");
        xp2Role("role_xp2_hq", "platform-company", List.of(M002_VIEW), false, "org_a", "org_b");
        xp2Holder("usr_xp2_hq", "platform-company", "role_xp2_hq");
        Map<String, String> holderOf = Map.of("role_xp2_platform", "usr_xp2_direct", "role_xp2_hq", "usr_xp2_hq");
        Map<String, Runnable> sessions = new LinkedHashMap<>();
        sessions.put("role_xp2_platform", () -> authenticateAsPlatformAdmin("seed-org"));
        sessions.put("role_xp2_hq", this::authenticateAsMasterAuthority);

        sessions.forEach((role, login) -> {
            login.run();
            service.updateRoleViewableProjects(role, xp2Add("xp2-s9-add-" + role, "org_c"));
            assertThat(xp2Holds(holderOf.get(role), role)).as(role).containsExactly("org_c");

            int reviews = xp2Reviews.reviews.size();
            RoleViewableProjectsView view = service.updateRoleViewableProjects(role, xp2Remove("xp2-s9-remove-" + role, "org_b"));
            assertThat(accessRepository.roles.get(role).viewableProjectIds()).as(role).containsExactly("org_a", "org_b", "org_c");
            assertThat(view.pendingRemovalIds()).as(role).containsExactly("org_b");
            assertThat(xp2Reviews.reviews).as(role).hasSize(reviews + 1);
            assertThat(xp2Reviews.reviews.get(xp2RemovalReview(role, "org_b")).reviewStatus()).as(role).isEqualTo("pending_review");
        });
    }

    // M736-S10（技术推导，依据操作员 2026-10-01 确认的治理地图第 653 行追加句末句「能配置角色的人都可以设置这一项。」，与权限模板
    // 同口径）：只到甲项目的角色管理人——
    // 本项目的角色可改可读；乙项目的角色、平台角色，改与读都是 403 CROSS_ORG_FORBIDDEN，角色不变。经勾了乙的可跨项目角色持建角色权限的
    // 人——可改可读乙项目的角色，平台角色仍 403；平台角色只有平台管理员能改。去掉范围判断即红（不拦）。
    @Test
    void onlyRolesWithinReachCanBeChangedOrRead() {
        xp2Fixture();
        xp2Role("role_xp2_a", "org_a", List.of(M002_VIEW), false, "org_a");
        xp2Role("role_xp2_b", "org_b", List.of(M002_VIEW), false, "org_b");
        xpRole("role_xp2_platform", List.of(M002_VIEW), false, "org_b");
        xpRole("role_xp2_manage_b", List.of(XP_MANAGE), false, "org_b");

        assertThat(service.updateRoleViewableProjects("role_xp2_a", xp2Add("xp2-s10-own", "org_c")).viewableProjectIds())
                .containsExactly("org_a", "org_c");
        assertThat(service.getRoleViewableProjects("role_xp2_a").viewableProjectIds()).containsExactly("org_a", "org_c");
        for (String outside : List.of("role_xp2_b", "role_xp2_platform")) {
            xpExpect(() -> service.updateRoleViewableProjects(outside, xp2Add("xp2-s10-" + outside, "org_c")),
                    403, "CROSS_ORG_FORBIDDEN", "不能修改其他组织的角色");
            xpExpect(() -> service.getRoleViewableProjects(outside), 403, "CROSS_ORG_FORBIDDEN", "不能查看其他组织的角色");
            assertThat(accessRepository.roles.get(outside).viewableProjectIds()).as(outside).containsExactly("org_b");
        }

        xpActAs("usr_xp2_cross", "role_xp2_manage_b");
        assertThat(service.updateRoleViewableProjects("role_xp2_b", xp2Add("xp2-s10-cross", "org_c")).viewableProjectIds())
                .containsExactly("org_b", "org_c");
        assertThat(service.getRoleViewableProjects("role_xp2_b").viewableProjectIds()).containsExactly("org_b", "org_c");
        xpExpect(() -> service.updateRoleViewableProjects("role_xp2_platform", xp2Add("xp2-s10-cross-platform", "org_c")),
                403, "CROSS_ORG_FORBIDDEN", "不能修改其他组织的角色");

        authenticateAsPlatformAdmin("seed-org");
        assertThat(service.updateRoleViewableProjects("role_xp2_platform", xp2Add("xp2-s10-admin", "org_c")).viewableProjectIds())
                .containsExactly("org_b", "org_c");
    }

    // M736-S11（技术推导：只核这次新增的条目，沿用建角色的校验）：四项都空（或都是空表）——400「可查看项目没有变化」；增加或减少
    // 清单里有空白、空值——400；同一条既加又减——400；新增条目不在项目集合里——400；加完超过 500 条——400；角色不存在——改与读都是 404。
    // 以上都不改角色，改的各留一条失败痕。角色上原有的条目已不在项目集合里、这次没动它——不报错，原条目照留。
    @Test
    void viewableProjectDeltaValidation() {
        xp2Fixture();
        List<String> bulk = java.util.stream.IntStream.range(0, 499).mapToObj(i -> "bulk_" + i).toList();
        accessRepository.projectIds.addAll(bulk);
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_a", "gone_project");

        xpExpect(() -> service.updateRoleViewableProjects("role_xp2", xp2Delta("xp2-s11-empty", null, null, null, null)),
                400, "VALIDATION_FAILED", "可查看项目没有变化");
        xpExpect(() -> service.updateRoleViewableProjects("role_xp2", xp2Delta("xp2-s11-empty-lists", false, List.of(), false, List.of())),
                400, "VALIDATION_FAILED", "可查看项目没有变化");
        xpExpect(() -> service.updateRoleViewableProjects("role_xp2",
                        xp2Delta("xp2-s11-blank-add", null, java.util.Arrays.asList("org_b", " "), null, null)),
                400, "VALIDATION_FAILED", "可查看项目清单里有空白条目");
        xpExpect(() -> service.updateRoleViewableProjects("role_xp2",
                        xp2Delta("xp2-s11-blank-remove", null, null, null, java.util.Arrays.asList("org_a", null))),
                400, "VALIDATION_FAILED", "可查看项目清单里有空白条目");
        xpExpect(() -> service.updateRoleViewableProjects("role_xp2",
                        xp2Delta("xp2-s11-both", null, List.of("org_b"), null, List.of("org_b"))),
                400, "VALIDATION_FAILED", "同一个可查看项目不能既增加又减少");
        xpExpect(() -> service.updateRoleViewableProjects("role_xp2", xp2Add("xp2-s11-unknown", "org_b", "no_such_id")),
                400, "VALIDATION_FAILED", "可查看项目清单里有无法识别或已停用的条目：no_such_id");
        xpExpect(() -> service.updateRoleViewableProjects("role_xp2", xp2Delta("xp2-s11-501", null, bulk, null, null)),
                400, "VALIDATION_FAILED", "逐个勾选的可查看项目不能超过 500 条");
        xpExpect(() -> service.updateRoleViewableProjects("role_missing", xp2Add("xp2-s11-missing", "org_b")),
                404, "ROLE_NOT_FOUND", "role_missing");
        xpExpect(() -> service.getRoleViewableProjects("role_missing"), 404, "ROLE_NOT_FOUND", "role_missing");
        assertThat(accessRepository.roles.get("role_xp2").viewableProjectIds()).containsExactly("org_a", "gone_project");
        assertThat(xp2Events("role.viewable_projects.changed")).isEmpty();
        List<Object> failures = xp2Events("role.viewable_projects.change_failed").stream()
                .map(event -> event.metadata().get("error_code")).toList();
        assertThat(failures).hasSize(8).endsWith("ROLE_NOT_FOUND");
        assertThat(failures.subList(0, 7)).containsOnly("VALIDATION_FAILED");

        RoleViewableProjectsView kept = service.updateRoleViewableProjects("role_xp2", xp2Add("xp2-s11-ok", "org_b"));
        assertThat(kept.viewableProjectIds()).containsExactly("org_a", "gone_project", "org_b");
    }

    // M736-S12（技术推导，非操作员原话：答 13 选项原文没有按操作人留例外；档位借用操作员 2026-09-25 所选选项原文「按授权政策「高」一档走
    // （按现行政策不到最高权限）」）：角色只含日常权限，持有人对丙有待批。角色管理人发起、平台管理员发起重新审批——都受理（不出
    // 「请直接分配」），都是「高」一档、两个上级节点，复核单受益人＝该持有人，申请原因以系统写的那句开头并带条目名称；角色含极高档码——
    // 到最高权限一档。档位下限只在「非平台管理员申请可跨项目角色」时才给即红。
    @Test
    void reapprovalIsAcceptedAtTheHighTierWhoeverInitiates() {
        xp2Fixture();
        accessRepository.markRequiresApproval(M002_CODE, "critical");
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_a", "org_b");
        xp2Role("role_xp2_critical", "org_a", List.of(M002_CODE), false, "org_a", "org_b");
        xp2Holder("usr_xp2_direct", "org_a", "role_xp2");
        accessRepository.assignUserRole("usr_xp2_direct", "role_xp2_critical");
        service.updateRoleViewableProjects("role_xp2", xp2Add("xp2-s12-add", "org_c"));
        service.updateRoleViewableProjects("role_xp2_critical", xp2Add("xp2-s12-add-critical", "org_c"));

        AccessGrantRequestRecord byManager = service.applyGrant(xp2Reapproval("xp2-s12-manager", "usr_xp2_direct", "role_xp2"));
        authenticateAsPlatformAdmin("seed-org");
        AccessGrantRequestRecord byAdmin = service.applyGrant(xp2Reapproval("xp2-s12-admin", "usr_xp2_direct", "role_xp2"));

        for (AccessGrantRequestRecord request : List.of(byManager, byAdmin)) {
            assertThat(request.status()).as(request.requestedBy()).isEqualTo("pending");
            assertThat(request.requestType()).isEqualTo("role_project_reapproval");
            assertThat(request.targetUserId()).isEqualTo("usr_xp2_direct");
            assertThat(request.targetRoleId()).isEqualTo("role_xp2");
            assertThat(request.reason()).startsWith("重新审批可查看项目（共 1 项）：").contains("项目 org_c").endsWith("补审新增项目");
            assertThat(xp2Reviews.reviews.get(request.reviewId()).policyCode()).as(request.requestedBy()).isEqualTo("permission_grant_l2");
            assertThat(xp2Reviews.schedules.get(request.reviewId()).beneficiaryId()).isEqualTo("usr_xp2_direct");
            assertThat(xp2Reviews.listChainNodes(request.reviewId())).extracting(node -> node.approverKind() + "/" + node.approverId())
                    .containsExactly("user/usr_xp2_sup1", "user/usr_xp2_sup2");
        }
        assertThat(List.of(byManager.requestedBy(), byAdmin.requestedBy())).containsExactly("usr_xp2_mgr", "usr_admin");

        AccessGrantRequestRecord critical = service.applyGrant(xp2Reapproval("xp2-s12-critical", "usr_xp2_direct", "role_xp2_critical"));
        assertThat(xp2Reviews.reviews.get(critical.reviewId()).policyCode()).isEqualTo("permission_grant_l3");
        assertThat(xp2Holds("usr_xp2_direct", "role_xp2")).as("建单不解除待批").containsExactly("org_c");
    }

    // M736-S13（技术推导：一张申请一个持有人，只收对这个角色有待批条目的员工；范围照授权申请的现行口径）：改之后才得到角色、没有待批的
    // 员工——400；乙项目的持有人确有待批，只到甲项目的角色管理人替他发起——403 CROSS_ORG_FORBIDDEN。两次都不建申请、不开复核单，
    // 待批不动；平台管理员替乙项目那位发起则受理。
    @Test
    void reapprovalNeedsAHold() {
        xp2Fixture();
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_a", "org_b");
        xpRole("role_xp2_platform", List.of(M002_VIEW), false, "org_a");
        xp2Holder("usr_xp2_b", "org_b", "role_xp2_platform");
        authenticateAsPlatformAdmin("seed-org");
        service.updateRoleViewableProjects("role_xp2_platform", xp2Add("xp2-s13-add", "org_c"));
        xp2Holder("usr_xp2_later", "org_a", "role_xp2");

        xp2Login("usr_xp2_mgr");
        xpExpect(() -> service.applyGrant(xp2Reapproval("xp2-s13-none", "usr_xp2_later", "role_xp2")),
                400, "VALIDATION_FAILED", "该员工对这个角色没有待重新审批的可查看项目");
        xpExpect(() -> service.applyGrant(xp2Reapproval("xp2-s13-outside", "usr_xp2_b", "role_xp2_platform")),
                403, "CROSS_ORG_FORBIDDEN", "usr_xp2_b");
        assertThat(accessRepository.grantRequests).isEmpty();
        assertThat(xp2Reviews.reviews).isEmpty();
        assertThat(xp2Holds("usr_xp2_b", "role_xp2_platform")).containsExactly("org_c");

        authenticateAsPlatformAdmin("seed-org");
        assertThat(service.applyGrant(xp2Reapproval("xp2-s13-admin", "usr_xp2_b", "role_xp2_platform")).status()).isEqualTo("pending");
    }

    // M736-S14（答 13 选项原文「各自重新审批通过」；落地只解除待批为技术推导）：张三经岗位、李四经直接分配持同一角色，都对丙有待批；
    // 张三的岗位模板里有须审批码（有工资、岗位派生套用、岗位高危挂起与待审释放单）。对张三的重新审批第一级通过——待批还在；末级通过——
    // 张三的待批清空、看得到丙，李四的还在、看不到；直接分配的角色一行不多；张三的岗位行、工资、岗位派生角色与映射、模板套用、
    // 覆盖行（含挂起）、待审释放单与批前逐项相同；留痕带员工、申请单号与删掉的行数。落地改走定岗即红（工资被改）。
    @Test
    void approvedReapprovalClearsOnlyThatHoldersHoldsAndTouchesNothingElse() {
        xp2Fixture();
        accessRepository.markRequiresApproval(M002_CODE, "high");
        xpRole("role_xp2_cross", List.of(M002_VIEW), false, "org_b");
        xp2HighTemplate("tpl_xp2_high");
        xpUser("usr_xp2_zhang", "org_a");
        xpUser("usr_xp2_li", "org_a");
        authenticateAsPlatformAdmin("seed-org");
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "xp2-s14-map", null, "xp2_patrol", "lvl_m1_lower", "role_xp2_cross", "tpl_xp2_high", "active"));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "xp2-s14-seat", null, "usr_xp2_zhang", "xp2_patrol", "lvl_m1_lower", 500000L, "CNY", null));
        service.assignUserRole(new RoleAssignmentRequest("xp2-s14-assign", null, "usr_xp2_li", "role_xp2_cross"));
        service.updateRoleViewableProjects("role_xp2_cross", xp2Add("xp2-s14-add", "org_c"));
        assertThat(overrideSources("usr_xp2_zhang", M002_CODE, AccessRepository.Effect.DENY))
                .containsExactly(AccessRepository.Source.POSITION_HOLD);
        List<Object> facts = m002TargetFacts("usr_xp2_zhang");
        List<AccessGrantRequestRecord> releases = m002PendingReleases("usr_xp2_zhang");
        String directRoles = accessRepository.userRoles.toString();

        xp2Login("usr_xp2_mgr");
        AccessGrantRequestRecord pending = service.applyGrant(xp2Reapproval("xp2-s14-apply", "usr_xp2_zhang", "role_xp2_cross"));
        xp2Login("usr_xp2_sup1");
        assertThat(service.reviewGrant(pending.id(), m002Approve("xp2-s14-first")).status()).isEqualTo("pending");
        assertThat(xp2Holds("usr_xp2_zhang", "role_xp2_cross")).as("第一级通过时待批还在").containsExactly("org_c");
        xp2Login("usr_xp2_sup2");
        assertThat(service.reviewGrant(pending.id(), m002Approve("xp2-s14-last")).status()).isEqualTo("applied");

        assertThat(xp2Holds("usr_xp2_zhang", "role_xp2_cross")).isEmpty();
        assertThat(xp2Holds("usr_xp2_li", "role_xp2_cross")).containsExactly("org_c");
        assertThat(accessRepository.userRoles.toString()).isEqualTo(directRoles);
        assertThat(m002TargetFacts("usr_xp2_zhang")).isEqualTo(facts);
        assertThat(m002PendingReleases("usr_xp2_zhang")).hasSize(1).isEqualTo(releases);
        assertThat(accessRepository.staffLevelAssignments.get("usr_xp2_zhang").baseSalaryAmountCent()).isEqualTo(500000L);
        assertThat(xp2ReachOf("usr_xp2_zhang", M002_VIEW).covers("org_c")).isTrue();
        assertThat(xp2ReachOf("usr_xp2_li", M002_VIEW).covers("org_c")).isFalse();
        assertThat(xp2Events("role.viewable_projects.reapproved")).singleElement().satisfies(event -> {
            assertThat(event.businessId()).isEqualTo("role_xp2_cross");
            assertThat(event.metadata().get("user_id")).isEqualTo("usr_xp2_zhang");
            assertThat(event.metadata().get("grant_request_id")).isEqualTo(pending.id());
            assertThat(event.metadata().get("removed_holds")).isEqualTo(1);
        });
    }

    // M736-S15（技术推导：被驳回的仍看不到新增项目，可以再发起；治理地图第 529 行「申请人与直接受益人不得审批」）：目标本人去批——
    // BENEFICIARY_SELF_REVIEW_FORBIDDEN；发起人去批——SUPERVISOR_SELF_REVIEW_FORBIDDEN；上级驳回后待批还在、判定仍看不到丙；
    // 再发起一张，受理。
    @Test
    void rejectedReapprovalKeepsTheHoldAndCanBeRaisedAgain() {
        xp2Fixture();
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_a", "org_b");
        xp2Holder("usr_xp2_direct", "org_a", "role_xp2");
        service.updateRoleViewableProjects("role_xp2", xp2Add("xp2-s15-add", "org_c"));
        AccessGrantRequestRecord pending = service.applyGrant(xp2Reapproval("xp2-s15-apply", "usr_xp2_direct", "role_xp2"));

        xp2Login("usr_xp2_direct");
        xpExpect(() -> service.reviewGrant(pending.id(), m002Approve("xp2-s15-beneficiary")),
                403, "BENEFICIARY_SELF_REVIEW_FORBIDDEN", "授权受益人不能审批自己的授权");
        xp2Login("usr_xp2_mgr");
        xpExpect(() -> service.reviewGrant(pending.id(), m002Approve("xp2-s15-requester")),
                403, "SUPERVISOR_SELF_REVIEW_FORBIDDEN", "申请人不能审批自己的授权");
        assertThat(accessRepository.grantRequests.get(pending.id()).status()).isEqualTo("pending");

        xp2Login("usr_xp2_sup1");
        assertThat(service.reviewGrant(pending.id(), m002Reject("xp2-s15-reject")).status()).isEqualTo("rejected");
        assertThat(xp2Holds("usr_xp2_direct", "role_xp2")).containsExactly("org_c");
        assertThat(xp2ReachOf("usr_xp2_direct", M002_VIEW).covers("org_c")).isFalse();

        xp2Login("usr_xp2_mgr");
        AccessGrantRequestRecord again = service.applyGrant(xp2Reapproval("xp2-s15-again", "usr_xp2_direct", "role_xp2"));
        assertThat(again.status()).isEqualTo("pending");
        assertThat(again.id()).isNotEqualTo(pending.id());
    }

    // M736-S16（技术推导：指纹钉住这个人对这个角色当时的待批条目，批的是这几条、落地删的也只能是这几条）：张三待批 {丙}，申请待审；
    // 再加丁——点通过 409 GRANT_SNAPSHOT_CHANGED，丙、丁两条待批都在。同一人两张待审单（都钉 {丙、丁}）：第一张两级批完、待批清空，
    // 第二张点通过 409。再加一条后提单，张三被取消角色（待批随之清掉）——点通过 409。指纹里不钉待批即红（旧单把丁一起放行）。
    @Test
    void reapprovalCannotBeApprovedAfterTheHoldsChange() {
        xp2Fixture();
        xpProjects("org_e");
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_a", "org_b");
        xp2Holder("usr_xp2_zhang", "org_a", "role_xp2");
        service.updateRoleViewableProjects("role_xp2", xp2Add("xp2-s16-add-c", "org_c"));
        AccessGrantRequestRecord stale = service.applyGrant(xp2Reapproval("xp2-s16-stale", "usr_xp2_zhang", "role_xp2"));
        service.updateRoleViewableProjects("role_xp2", xp2Add("xp2-s16-add-d", "org_d"));

        xp2Login("usr_xp2_sup1");
        xpExpect(() -> service.reviewGrant(stale.id(), m002Approve("xp2-s16-stale-review")), 409, "GRANT_SNAPSHOT_CHANGED", "请重新申请");
        assertThat(xp2Holds("usr_xp2_zhang", "role_xp2")).containsExactlyInAnyOrder("org_c", "org_d");
        assertThat(accessRepository.grantRequests.get(stale.id()).status()).isEqualTo("pending");

        xp2Login("usr_xp2_mgr");
        AccessGrantRequestRecord first = service.applyGrant(xp2Reapproval("xp2-s16-first", "usr_xp2_zhang", "role_xp2"));
        AccessGrantRequestRecord second = service.applyGrant(xp2Reapproval("xp2-s16-second", "usr_xp2_zhang", "role_xp2"));
        assertThat(xp2ApproveBoth(first.id(), "xp2-s16-first-review").status()).isEqualTo("applied");
        assertThat(xp2Holds("usr_xp2_zhang", "role_xp2")).isEmpty();
        xp2Login("usr_xp2_sup1");
        xpExpect(() -> service.reviewGrant(second.id(), m002Approve("xp2-s16-second-review")), 409, "GRANT_SNAPSHOT_CHANGED", "请重新申请");

        xp2Login("usr_xp2_mgr");
        service.updateRoleViewableProjects("role_xp2", xp2Add("xp2-s16-add-e", "org_e"));
        AccessGrantRequestRecord orphan = service.applyGrant(xp2Reapproval("xp2-s16-orphan", "usr_xp2_zhang", "role_xp2"));
        service.unassignUserRole(new RoleAssignmentRequest("xp2-s16-unassign", null, "usr_xp2_zhang", "role_xp2"));
        assertThat(xp2Holds("usr_xp2_zhang", "role_xp2")).isEmpty();
        xp2Login("usr_xp2_sup1");
        xpExpect(() -> service.reviewGrant(orphan.id(), m002Approve("xp2-s16-orphan-review")), 409, "GRANT_SNAPSHOT_CHANGED", "请重新申请");
        assertThat(accessRepository.grantRequests.get(orphan.id()).status()).isEqualTo("pending");
    }

    // M736-S17（技术推导：只有重新审批这一类申请通过才算重新审批通过；得到角色时一律不清待批）：直接持有、经岗位持有的两人都对丙有待批。
    // 以下各做一遍，两人的待批都还在、都仍看不到丙：①对直接持有的再提角色申请并两级批完；②平台管理员对他再直接分配；③经岗位持有的
    // 被平台管理员直接分配同一角色；④直接持有的被平台管理员定到派生同一角色的岗位；⑤角色管理人对经岗位持有的同岗重定；⑥平台管理员
    // 原样重存岗位映射触发重派生。在分配或定岗写入之后清待批即红。
    @Test
    void onlyTheDedicatedReapprovalClearsHolds() {
        xp2Fixture();
        xpRole("role_xp2_cross", List.of(M002_VIEW), false, "org_b");
        xpTemplate("tpl_xp2_pos", null);
        xpUser("usr_xp2_direct", "org_a");
        xpUser("usr_xp2_seat", "org_a");
        authenticateAsPlatformAdmin("seed-org");
        PositionRoleTemplate mapping = service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "xp2-s17-map", null, "xp2_patrol", "lvl_m1_lower", "role_xp2_cross", "tpl_xp2_pos", "active"));
        service.assignUserRole(new RoleAssignmentRequest("xp2-s17-assign", null, "usr_xp2_direct", "role_xp2_cross"));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "xp2-s17-seat", null, "usr_xp2_seat", "xp2_patrol", "lvl_m1_lower", null, null, null));
        service.updateRoleViewableProjects("role_xp2_cross", xp2Add("xp2-s17-add", "org_c"));
        java.util.function.Consumer<String> stillHeld = step -> {
            for (String holder : List.of("usr_xp2_direct", "usr_xp2_seat")) {
                assertThat(xp2Holds(holder, "role_xp2_cross")).as(step + " " + holder).containsExactly("org_c");
                assertThat(xp2ReachOf(holder, M002_VIEW).covers("org_c")).as(step + " " + holder).isFalse();
            }
        };
        stillHeld.accept("改勾选之后");

        xp2Login("usr_xp2_mgr");
        AccessGrantRequestRecord roleRequest = service.applyGrant(new AccessGrantApplyRequest("xp2-s17-role", "M736", List.of(), "user_role",
                "usr_xp2_direct", "role_xp2_cross", null, null, null, null, null, null, null, null, false, "再提角色申请"));
        assertThat(xp2ApproveBoth(roleRequest.id(), "xp2-s17-role-review").status()).isEqualTo("applied");
        stillHeld.accept("①再提角色申请并批完");

        authenticateAsPlatformAdmin("seed-org");
        service.assignUserRole(new RoleAssignmentRequest("xp2-s17-assign-again", null, "usr_xp2_direct", "role_xp2_cross"));
        stillHeld.accept("②平台管理员再直接分配");

        authenticateAsPlatformAdmin("seed-org");
        service.assignUserRole(new RoleAssignmentRequest("xp2-s17-assign-seat", null, "usr_xp2_seat", "role_xp2_cross"));
        stillHeld.accept("③经岗位持有的被直接分配");

        authenticateAsPlatformAdmin("seed-org");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "xp2-s17-seat-direct", null, "usr_xp2_direct", "xp2_patrol", "lvl_m1_lower", null, null, null));
        assertThat(accessRepository.positionRoles.get("usr_xp2_direct")).isEqualTo("role_xp2_cross");
        stillHeld.accept("④直接持有的被定到派生同一角色的岗位");

        xp2Login("usr_xp2_mgr");
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "xp2-s17-reseat", null, "usr_xp2_seat", "xp2_patrol", "lvl_m1_lower", null, null, null));
        stillHeld.accept("⑤同岗重定");

        authenticateAsPlatformAdmin("seed-org");
        service.updatePositionRoleTemplate(mapping.id(), new PositionRoleTemplateRequest(
                "xp2-s17-resave", null, "xp2_patrol", "lvl_m1_lower", "role_xp2_cross", "tpl_xp2_pos", "active"));
        assertThat(accessRepository.positionRoles.get("usr_xp2_seat")).isEqualTo("role_xp2_cross");
        stillHeld.accept("⑥原样重存映射");
    }

    // M736-S18（技术推导：直接分配与岗位派生两条路都不再持有这个角色时，待重新审批的记录随之清除，以后重新得到角色的按当时的勾选）：
    // 五名持有人都对丙有待批。取消分配、范围变更、所挂映射停用、换到派生别的角色的岗位——这四人的待批没了；两条路都持有、只取消直接
    // 分配的那位——还在。平台管理员对被取消的那位再直接分配——没有待批，判定看得到丙；仍带待批的那位看不到。去掉仓储里的清除即红
    // （重新得到后仍带旧待批）。
    @Test
    void losingTheRoleDropsTheHoldsAndRegainingItStartsClean() {
        xp2Fixture();
        xpRole("role_xp2_cross", List.of(M002_VIEW), false, "org_b");
        xpRole("role_xp2_plain", List.of(M002_VIEW), false);
        xpTemplate("tpl_xp2_pos", null);
        authenticateAsPlatformAdmin("seed-org");
        PositionRoleTemplate off = service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "xp2-s18-map-off", null, "xp2_off", "lvl_m1_lower", "role_xp2_cross", "tpl_xp2_pos", "active"));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "xp2-s18-map-keep", null, "xp2_keep", "lvl_m1_lower", "role_xp2_cross", "tpl_xp2_pos", "active"));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "xp2-s18-map-plain", null, "xp2_plain", "lvl_m1_lower", "role_xp2_plain", "tpl_xp2_pos", "active"));
        for (String direct : List.of("usr_xp2_unassign", "usr_xp2_scope", "usr_xp2_both")) {
            xpUser(direct, "org_a");
            service.assignUserRole(new RoleAssignmentRequest("xp2-s18-assign-" + direct, null, direct, "role_xp2_cross"));
        }
        Map<String, String> seats = new LinkedHashMap<>();
        seats.put("usr_xp2_off", "xp2_off");
        seats.put("usr_xp2_move", "xp2_keep");
        seats.put("usr_xp2_both", "xp2_keep");
        seats.forEach((user, position) -> {
            xpUser(user, "org_a");
            service.assignUserPosition(new UserPositionAssignmentRequest(
                    "xp2-s18-seat-" + user, null, user, position, "lvl_m1_lower", null, null, null));
        });
        service.updateRoleViewableProjects("role_xp2_cross", xp2Add("xp2-s18-add", "org_c"));
        assertThat(accessRepository.listRoleViewableProjectHolds("role_xp2_cross").keySet())
                .containsExactly("usr_xp2_both", "usr_xp2_move", "usr_xp2_off", "usr_xp2_scope", "usr_xp2_unassign");

        service.unassignUserRole(new RoleAssignmentRequest("xp2-s18-unassign", null, "usr_xp2_unassign", "role_xp2_cross"));
        service.changeUserScope("usr_xp2_scope", new UserScopeChangeRequest(
                "xp2-s18-scope", null, "org_a", "merchant_x", "xp2_plain", "lvl_m1_lower", null, null, null, "调到商户", null));
        service.updatePositionRoleTemplate(off.id(), new PositionRoleTemplateRequest(
                "xp2-s18-map-off-stop", null, "xp2_off", "lvl_m1_lower", "role_xp2_cross", "tpl_xp2_pos", "inactive"));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "xp2-s18-move", null, "usr_xp2_move", "xp2_plain", "lvl_m1_lower", null, null, null));
        service.unassignUserRole(new RoleAssignmentRequest("xp2-s18-unassign-both", null, "usr_xp2_both", "role_xp2_cross"));

        assertThat(accessRepository.positionRoles.get("usr_xp2_both")).isEqualTo("role_xp2_cross");
        assertThat(accessRepository.listRoleViewableProjectHolds("role_xp2_cross")).isEqualTo(Map.of("usr_xp2_both", Set.of("org_c")));

        service.assignUserRole(new RoleAssignmentRequest("xp2-s18-reassign", null, "usr_xp2_unassign", "role_xp2_cross"));
        assertThat(xp2Holds("usr_xp2_unassign", "role_xp2_cross")).isEmpty();
        assertThat(xp2ReachOf("usr_xp2_unassign", M002_VIEW).covers("org_c")).as("重新得到角色的人按当时的勾选").isTrue();
        assertThat(xp2ReachOf("usr_xp2_both", M002_VIEW).covers("org_c")).as("仍带待批的人").isFalse();
    }

    // M736-S19（技术推导：角色、岗位、范围变更三类申请所指角色勾了项目的，指纹含两项勾选——否则按旧勾选批的单会按新勾选落地）：
    // 指向该角色的待审角色申请、岗位申请、范围变更申请各一张。加丁以后三张点通过都是 409 GRANT_SNAPSHOT_CHANGED，申请仍待审、
    // 角色与岗位都没落到目标身上。另提一组三张：减乙在待审期间角色申请两级批完照常落地；减少整单通过（角色没了乙）以后，
    // 余下两张点通过 409。指纹不追加勾选即红（照旧落地）。
    @Test
    void pendingGrantRequestsCannotBeApprovedAfterTheTicksChange() {
        xp2Fixture();
        xpRole("role_xp2_cross", List.of(M002_VIEW), false, "org_b", "org_c");
        xpTemplate("tpl_xp2_pos", null);
        authenticateAsPlatformAdmin("seed-org");
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "xp2-s19-map", null, "xp2_patrol", "lvl_m1_lower", "role_xp2_cross", "tpl_xp2_pos", "active"));

        Map<String, AccessGrantRequestRecord> beforeAdd = xp2ThreeRequests("xp2-s19-a");
        authenticateAsPlatformAdmin("seed-org");
        service.updateRoleViewableProjects("role_xp2_cross", xp2Add("xp2-s19-add", "org_d"));
        xp2Login("usr_xp2_sup1");
        beforeAdd.forEach((type, request) -> {
            xpExpect(() -> service.reviewGrant(request.id(), m002Approve("xp2-s19-a-review-" + type)),
                    409, "GRANT_SNAPSHOT_CHANGED", "请重新申请");
            assertThat(accessRepository.grantRequests.get(request.id()).status()).as(type).isEqualTo("pending");
            assertThat(accessRepository.userRoles.getOrDefault(request.targetUserId(), Set.of())).as(type).doesNotContain("role_xp2_cross");
            assertThat(accessRepository.positionRoles.get(request.targetUserId())).as(type).isNull();
        });

        Map<String, AccessGrantRequestRecord> beforeRemoval = xp2ThreeRequests("xp2-s19-b");
        authenticateAsPlatformAdmin("seed-org");
        service.updateRoleViewableProjects("role_xp2_cross", xp2Remove("xp2-s19-remove", "org_b"));
        String reviewId = xp2RemovalReview("role_xp2_cross", "org_b");
        AccessGrantRequestRecord landed = xp2ApproveBoth(beforeRemoval.get("user_role").id(), "xp2-s19-b-role");
        assertThat(landed.status()).as("减少待审期间仍可批准").isEqualTo("applied");
        assertThat(accessRepository.userRoles.get(landed.targetUserId())).contains("role_xp2_cross");

        assertThat(xp2Decide("usr_xp2_sup1", reviewId, "xp2-s19-remove-first", "approved").reviewStatus()).isEqualTo("pending_review");
        assertThat(xp2Decide("usr_xp2_sup2", reviewId, "xp2-s19-remove-last", "approved").reviewStatus()).isEqualTo("approved");
        assertThat(accessRepository.roles.get("role_xp2_cross").viewableProjectIds()).containsExactly("org_c", "org_d");
        xp2Login("usr_xp2_sup1");
        for (String type : List.of("user_position", "user_scope_change")) {
            AccessGrantRequestRecord request = beforeRemoval.get(type);
            xpExpect(() -> service.reviewGrant(request.id(), m002Approve("xp2-s19-b-review-" + type)),
                    409, "GRANT_SNAPSHOT_CHANGED", "请重新申请");
            assertThat(accessRepository.grantRequests.get(request.id()).status()).as(type).isEqualTo("pending");
            assertThat(accessRepository.positionRoles.get(request.targetUserId())).as(type).isNull();
        }
    }

    // M736-S20（技术推导：岗位高危释放单获批只删挂起行、不授出角色，指纹不钉勾选；没勾选的角色指纹与原来相同）：挂该角色的在岗员工
    // 有一张待审的岗位高危释放单，另一个没勾选的角色上有一张待审的角色申请。给该角色加丙以后（在岗员工照写待批），两张单都仍可
    // 两级批完落地。把释放单也钉上勾选即红。
    @Test
    void tickChangeDoesNotTouchReleaseRequestsOrRolesWithoutTicks() {
        xp2Fixture();
        accessRepository.markRequiresApproval(M002_CODE, "high");
        xpRole("role_xp2_cross", List.of(M002_VIEW), false, "org_b");
        xpRole("role_xp2_plain_high", List.of(M002_CODE), false);
        xp2HighTemplate("tpl_xp2_high");
        xpUser("usr_xp2_seat", "org_a");
        xpUser("usr_xp2_target", "org_a");
        authenticateAsPlatformAdmin("seed-org");
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "xp2-s20-map", null, "xp2_patrol", "lvl_m1_lower", "role_xp2_cross", "tpl_xp2_high", "active"));
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "xp2-s20-seat", null, "usr_xp2_seat", "xp2_patrol", "lvl_m1_lower", null, null, null));
        assertThat(m002PendingReleases("usr_xp2_seat")).hasSize(1);
        AccessGrantRequestRecord release = m002PendingReleases("usr_xp2_seat").get(0);
        assertThat(release.targetRoleId()).isEqualTo("role_xp2_cross");
        AccessGrantRequestRecord plain = service.applyGrant(new AccessGrantApplyRequest("xp2-s20-plain", "M736", List.of(), "user_role",
                "usr_xp2_target", "role_xp2_plain_high", null, null, null, null, null, null, null, null, false, "角色申请"));

        service.updateRoleViewableProjects("role_xp2_cross", xp2Add("xp2-s20-add", "org_c"));
        assertThat(xp2Holds("usr_xp2_seat", "role_xp2_cross")).containsExactly("org_c");

        assertThat(xp2ApproveBoth(release.id(), "xp2-s20-release").status()).isEqualTo("applied");
        assertThat(overrideSources("usr_xp2_seat", M002_CODE, AccessRepository.Effect.DENY)).isEmpty();
        assertThat(xp2ApproveBoth(plain.id(), "xp2-s20-plain-review").status()).isEqualTo("applied");
        assertThat(accessRepository.userRoles.get("usr_xp2_target")).contains("role_xp2_plain_high");
    }

    // M736-S21（技术推导：监听只认自己的动作名，并按复核单号匹配待减标记）：角色有一笔待审的减少。直接把事件交给监听——别的动作名、
    // 对象同为该角色的已通过事件；本动作名、同一角色、另一个复核单号的已通过事件——角色、待减标记、真复核单都不动，不留减少的痕。
    // 监听改按对象编号删即红。
    @Test
    void reviewDecisionsOfOtherActionsOrOtherReviewsDoNotTouchTheRole() {
        xp2Fixture();
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_a", "org_b", "org_c");
        service.updateRoleViewableProjects("role_xp2", xp2Remove("xp2-s21-remove", "org_c"));
        String reviewId = xp2RemovalReview("role_xp2", "org_c");
        Map<String, String> forged = new LinkedHashMap<>();
        forged.put("review_xp2_other_action", "staff.shift.publish");
        forged.put("review_xp2_other_review", "access.role_viewable_projects.reduce");

        forged.forEach((otherReviewId, actionType) -> {
            service.onSupervisorReviewDecided(new SupervisorReviewDecisionRecord(otherReviewId, "permission_grant_l2", actionType,
                    "role_viewable_projects", "role_xp2", false, "approved", 2, 2, "approved", null, List.of(), Instant.now(),
                    "usr_xp2_mgr", "two_levels_up", null));
            assertThat(accessRepository.roles.get("role_xp2").viewableProjectIds()).as(actionType).containsExactly("org_a", "org_b", "org_c");
            assertThat(xp2RemovalReview("role_xp2", "org_c")).as(actionType).isEqualTo(reviewId);
        });
        assertThat(xp2Events("role.viewable_projects.reduced")).isEmpty();
        assertThat(xp2Reviews.reviews.get(reviewId).reviewStatus()).isEqualTo("pending_review");
    }

    // M736-S22（M001-F10 锁序；技术推导：角色表锁 → 持有人员工行 → 角色行，分配一侧员工行 → 角色行共享锁 → 读角色）：仓储按先后记下
    // 调用。改勾选——按操作人读能力事实全部在第一把锁之前；角色表锁先于任何员工行锁，员工行锁先于角色行锁，读角色在角色行锁之后。
    // 直接分配——员工行锁先于角色行共享锁，读角色在共享锁之后。调换次序即红。
    @Test
    void reachIsComputedBeforeAnyLockAndLocksAreTakenInOrder() {
        xp2Fixture();
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_a", "org_b");
        xp2Holder("usr_xp2_direct", "org_a", "role_xp2");
        xpUser("usr_xp2_new", "org_a");

        accessRepository.callTrace = new ArrayList<>();
        service.updateRoleViewableProjects("role_xp2", xp2Add("xp2-s22-add", "org_c"));
        List<String> change = List.copyOf(accessRepository.callTrace);
        assertThat(change).containsSubsequence("findPermissionEffects:usr_xp2_mgr", "lockRolesForEdit", "listRoleHolderIds:role_xp2",
                "lockAccessUsers:[usr_xp2_direct]", "lockRoleForEdit:role_xp2", "findRoleById:role_xp2");
        assertThat(change.lastIndexOf("findPermissionEffects:usr_xp2_mgr")).as("按操作人读能力事实都在第一把锁之前")
                .isLessThan(change.indexOf("lockRolesForEdit"));
        int firstUserLock = change.indexOf(change.stream().filter(call -> call.startsWith("lockAccessUsers:")).findFirst().orElseThrow());
        assertThat(change.indexOf("lockRolesForEdit")).as("角色表锁先于任何员工行锁").isLessThan(firstUserLock);
        assertThat(firstUserLock).as("员工行锁先于角色行锁").isLessThan(change.indexOf("lockRoleForEdit:role_xp2"));
        assertThat(change.indexOf("findRoleById:role_xp2")).as("读角色在角色行锁之后").isGreaterThan(change.indexOf("lockRoleForEdit:role_xp2"));

        authenticateAsPlatformAdmin("seed-org");
        accessRepository.callTrace = new ArrayList<>();
        service.assignUserRole(new RoleAssignmentRequest("xp2-s22-assign", null, "usr_xp2_new", "role_xp2"));
        List<String> assign = List.copyOf(accessRepository.callTrace);
        assertThat(assign).containsSubsequence("lockAccessUsers:[usr_xp2_new]", "lockRoleForGrant:role_xp2", "findRoleById:role_xp2");
        assertThat(assign.indexOf("lockAccessUsers:[usr_xp2_new]")).as("员工行锁先于角色行共享锁")
                .isLessThan(assign.indexOf("lockRoleForGrant:role_xp2"));
        assertThat(assign.indexOf("findRoleById:role_xp2")).as("读角色在角色行共享锁之后")
                .isGreaterThan(assign.indexOf("lockRoleForGrant:role_xp2"));
    }

    // M736-S23（操作员 2026-09-25 确认句「同一次操作重复提交时，直接返回第一次的结果并提示已完成」）：同一提交编号、同样内容再提交
    // 一次（加丁、减丙）——返回与第一次相同的结果，角色、待批、复核单、留痕都只有一份；同一提交编号换了内容——409，仍只有一份。
    @Test
    void changingViewableProjectsReplaysByIdempotencyKey() {
        xp2Fixture();
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_a", "org_b", "org_c");
        xp2Holder("usr_xp2_direct", "org_a", "role_xp2");
        RoleViewableProjectsRequest request = xp2Delta("xp2-s23", null, List.of("org_d"), null, List.of("org_c"));

        RoleViewableProjectsView first = service.updateRoleViewableProjects("role_xp2", request);
        RoleViewableProjectsView replayed = service.updateRoleViewableProjects("role_xp2", request);

        assertThat(replayed).isEqualTo(first);
        assertThat(first.pendingRemovalIds()).containsExactly("org_c");
        xpExpect(() -> service.updateRoleViewableProjects("role_xp2", xp2Add("xp2-s23", "org_d")),
                409, "IDEMPOTENCY_CONFLICT", "该幂等键已被不同的请求使用");
        assertThat(accessRepository.roles.get("role_xp2").viewableProjectIds()).containsExactly("org_a", "org_b", "org_c", "org_d");
        assertThat(xp2Holds("usr_xp2_direct", "role_xp2")).containsExactly("org_d");
        assertThat(accessRepository.findRoleViewableProjectRemovals("role_xp2")).containsExactly("org_c");
        assertThat(xp2Reviews.reviews).hasSize(1);
        assertThat(xp2Events("role.viewable_projects.changed")).hasSize(1);
    }

    // M736-S24（操作员 2026-10-01 确认的治理地图第 653 行追加句：「……平台公司员工、平台管理员、最高权限、外部机构账号一律碰不到。」）：
    // 乙项目的角色，带待批的持有人里有乙项目的两名普通员工和一个归属乙项目的平台管理员账号；一人的待审重新审批单还批得动，
    // 另一人的因为又加了项目已批不动；乙的减少在待审。平台管理员读——三人都列，前者「已发起」为真、后者为假，减少待审的条目与库里一致；
    // 经勾了乙的可跨项目角色持建角色权限的甲项目员工读——只列两名普通员工；只到甲项目的角色管理人读——403 CROSS_ORG_FORBIDDEN。
    // 去掉持有人的可触及过滤即红（多出那个平台管理员账号）。
    @Test
    void viewListsPendingRemovalsAndHoldersWithinReach() {
        xp2Fixture();
        xp2Role("role_xp2_b", "org_b", List.of(M002_VIEW), false, "org_b");
        xp2Holder("usr_xp2_b1", "org_b", "role_xp2_b");
        xp2Holder("usr_xp2_b2", "org_b", "role_xp2_b");
        accessRepository.users.put("usr_xp2_pa_b", new AccessUser("usr_xp2_pa_b", "usr_xp2_pa_b", "platform_admin", "org_b", null, "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        accessRepository.assignUserRole("usr_xp2_pa_b", "role_xp2_b");
        xpRole("role_xp2_manage_b", List.of(XP_MANAGE), false, "org_b");
        authenticateAsPlatformAdmin("seed-org");
        service.updateRoleViewableProjects("role_xp2_b", xp2Add("xp2-s24-add-c", "org_c"));
        service.applyGrant(xp2Reapproval("xp2-s24-stale", "usr_xp2_b2", "role_xp2_b"));
        service.updateRoleViewableProjects("role_xp2_b", xp2Add("xp2-s24-add-d", "org_d"));
        service.applyGrant(xp2Reapproval("xp2-s24-live", "usr_xp2_b1", "role_xp2_b"));
        service.updateRoleViewableProjects("role_xp2_b", xp2Remove("xp2-s24-remove", "org_b"));

        RoleViewableProjectsView byAdmin = service.getRoleViewableProjects("role_xp2_b");
        assertThat(byAdmin.viewAllProjects()).isFalse();
        assertThat(byAdmin.viewableProjectIds()).containsExactly("org_b", "org_c", "org_d");
        assertThat(byAdmin.pendingRemovalIds()).containsExactly("org_b")
                .containsExactlyInAnyOrderElementsOf(accessRepository.findRoleViewableProjectRemovals("role_xp2_b"));
        assertThat(byAdmin.pendingReapprovals()).extracting(RoleViewableProjectsView.PendingReapproval::userId)
                .containsExactly("usr_xp2_b1", "usr_xp2_b2", "usr_xp2_pa_b");
        assertThat(byAdmin.pendingReapprovals()).extracting(RoleViewableProjectsView.PendingReapproval::reapprovalPending)
                .containsExactly(true, false, false);
        assertThat(byAdmin.pendingReapprovals()).allSatisfy(pending -> assertThat(pending.viewableIds()).containsExactly("org_c", "org_d"));

        xpActAs("usr_xp2_cross", "role_xp2_manage_b");
        RoleViewableProjectsView byHolder = service.getRoleViewableProjects("role_xp2_b");
        assertThat(byHolder.pendingReapprovals()).extracting(RoleViewableProjectsView.PendingReapproval::userId)
                .containsExactly("usr_xp2_b1", "usr_xp2_b2");
        assertThat(byHolder.pendingReapprovals()).extracting(RoleViewableProjectsView.PendingReapproval::reapprovalPending)
                .containsExactly(true, false);
        assertThat(byHolder.pendingRemovalIds()).containsExactly("org_b");

        xp2Login("usr_xp2_mgr");
        xpExpect(() -> service.getRoleViewableProjects("role_xp2_b"), 403, "CROSS_ORG_FORBIDDEN", "不能查看其他组织的角色");
    }

    // M736-S25（技术推导：角色原来一条勾选都没有、第一次勾上项目时已持有它的人，重新审批通过之前这个角色对他仍只到本人项目）：
    // 没勾选的角色第一次勾上丙——老持有人除了对丙的待批另记一条保留标记：他仍到本人项目、到不了丙；现状、申请原因、留痕的条数都不含
    // 这条标记；改之后才得到角色的人没有标记、只到丙；老持有人重新审批通过后标记随待批一起删，他也只到丙（2026-10-01 确认的追加句
    // 「逐个勾选的只能查看被选中的项目」）。不记保留标记即红（老持有人当场丢掉本人项目）。
    @Test
    void firstTickOnAnUntickedRoleKeepsExistingHoldersOnTheirOwnProjectUntilReapproved() {
        xp2Fixture();
        xp2Role("role_xp2_plain", "org_a", List.of(M002_VIEW), false);
        xp2Holder("usr_xp2_direct", "org_a", "role_xp2_plain");

        RoleViewableProjectsView view = service.updateRoleViewableProjects("role_xp2_plain", xp2Add("xp2-s25-add", "org_c"));

        assertThat(xp2Holds("usr_xp2_direct", "role_xp2_plain"))
                .containsExactlyInAnyOrder("org_c", AuthorizationService.HELD_SINCE_UNTICKED);
        assertThat(view.pendingReapprovals()).singleElement()
                .satisfies(row -> assertThat(row.viewableIds()).containsExactly("org_c"));
        AuthorizationService.ProjectReach before = xp2ReachOf("usr_xp2_direct", M002_VIEW);
        assertThat(List.of(before.covers("org_a"), before.covers("org_c"))).as("重新审批通过之前").containsExactly(true, false);
        xp2Holder("usr_xp2_later", "org_a", "role_xp2_plain");
        assertThat(xp2Holds("usr_xp2_later", "role_xp2_plain")).isEmpty();
        AuthorizationService.ProjectReach later = xp2ReachOf("usr_xp2_later", M002_VIEW);
        assertThat(List.of(later.covers("org_a"), later.covers("org_c"))).as("改之后才得到角色的人").containsExactly(false, true);

        xp2Login("usr_xp2_mgr");
        AccessGrantRequestRecord pending = service.applyGrant(xp2Reapproval("xp2-s25-apply", "usr_xp2_direct", "role_xp2_plain"));
        assertThat(pending.reason()).startsWith("重新审批可查看项目（共 1 项）：项目 org_c。");
        assertThat(xp2ApproveBoth(pending.id(), "xp2-s25-approve").status()).isEqualTo("applied");

        assertThat(xp2Holds("usr_xp2_direct", "role_xp2_plain")).isEmpty();
        assertThat(xp2Events("role.viewable_projects.reapproved")).singleElement()
                .satisfies(event -> assertThat(event.metadata().get("removed_holds")).isEqualTo(1));
        AuthorizationService.ProjectReach after = xp2ReachOf("usr_xp2_direct", M002_VIEW);
        assertThat(List.of(after.covers("org_a"), after.covers("org_c"))).as("重新审批通过之后").containsExactly(false, true);
    }

    // M736-S26（操作员 2026-10-03 所选选项原文「减少也要审批通过后才生效；审批期间持有人仍能看 C。」；2026-10-01 确认的追加句「逐个勾选的
    // 只能查看被选中的项目」；一条不剩时什么都不给为技术推导）：角色只勾着乙，张三（本人在甲项目）持有它。一次保存去掉乙、勾上丙：
    // 审批期间张三仍到乙、到不了丙；减少整单通过后角色只剩丙——张三对丙还没重新审批，这个角色对他哪个项目都不给：乙、丙不到，
    // 从没被勾过的本人项目也不到（改之前角色已有勾选，不记保留标记）；改之后才得到角色的人只到丙；张三重新审批通过后同样只到丙。
    // 一条不剩就退回「按没勾选的角色算」即红（张三到了本人项目）。
    @Test
    void replacingEveryTickLeavesAnExistingHolderWithNothingUntilReapproved() {
        xp2Fixture();
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_b");
        xp2Holder("usr_xp2_zhang", "org_a", "role_xp2");

        service.updateRoleViewableProjects("role_xp2", xp2Delta("xp2-s26-swap", null, List.of("org_c"), null, List.of("org_b")));
        assertThat(xp2Holds("usr_xp2_zhang", "role_xp2")).as("改之前角色已有勾选，不记保留标记").containsExactly("org_c");
        AuthorizationService.ProjectReach waiting = xp2ReachOf("usr_xp2_zhang", M002_VIEW);
        assertThat(List.of(waiting.covers("org_a"), waiting.covers("org_b"), waiting.covers("org_c")))
                .as("减少待审期间").containsExactly(false, true, false);

        String reviewId = xp2RemovalReview("role_xp2", "org_b");
        xp2Decide("usr_xp2_sup1", reviewId, "xp2-s26-first", "approved");
        assertThat(xp2Decide("usr_xp2_sup2", reviewId, "xp2-s26-last", "approved").reviewStatus()).isEqualTo("approved");
        assertThat(accessRepository.roles.get("role_xp2").viewableProjectIds()).containsExactly("org_c");
        xp2Holder("usr_xp2_later", "org_a", "role_xp2");

        AuthorizationService.ProjectReach none = xp2ReachOf("usr_xp2_zhang", M002_VIEW);
        assertThat(List.of(none.covers("org_a"), none.covers("org_b"), none.covers("org_c")))
                .as("减少已生效、新增的还没重新审批").containsExactly(false, false, false);
        AuthorizationService.ProjectReach later = xp2ReachOf("usr_xp2_later", M002_VIEW);
        assertThat(List.of(later.covers("org_a"), later.covers("org_c"))).as("改之后才得到角色的人").containsExactly(false, true);

        xp2Login("usr_xp2_mgr");
        AccessGrantRequestRecord pending = service.applyGrant(xp2Reapproval("xp2-s26-apply", "usr_xp2_zhang", "role_xp2"));
        assertThat(xp2ApproveBoth(pending.id(), "xp2-s26-approve").status()).isEqualTo("applied");
        AuthorizationService.ProjectReach approved = xp2ReachOf("usr_xp2_zhang", M002_VIEW);
        assertThat(List.of(approved.covers("org_a"), approved.covers("org_c"))).as("重新审批通过之后").containsExactly(false, true);
    }

    // M736-S27（技术推导：保留标记不是可查看项目）：没勾选的角色第一次勾上丙以后又把丙减掉（审批通过）——角色回到没勾选，老持有人
    // 身上只剩保留标记：现状里不列他，替他发起重新审批 400，他照旧只到本人项目。滤掉保留标记的地方漏一处即红。
    @Test
    void retentionMarkerAloneIsNotAPendingReapproval() {
        xp2Fixture();
        xp2Role("role_xp2_plain", "org_a", List.of(M002_VIEW), false);
        xp2Holder("usr_xp2_direct", "org_a", "role_xp2_plain");
        service.updateRoleViewableProjects("role_xp2_plain", xp2Add("xp2-s27-add", "org_c"));
        service.updateRoleViewableProjects("role_xp2_plain", xp2Remove("xp2-s27-remove", "org_c"));
        String reviewId = xp2RemovalReview("role_xp2_plain", "org_c");
        xp2Decide("usr_xp2_sup1", reviewId, "xp2-s27-first", "approved");
        assertThat(xp2Decide("usr_xp2_sup2", reviewId, "xp2-s27-last", "approved").reviewStatus()).isEqualTo("approved");
        assertThat(accessRepository.roles.get("role_xp2_plain").viewableProjectIds()).isEmpty();
        assertThat(xp2Holds("usr_xp2_direct", "role_xp2_plain")).containsExactly(AuthorizationService.HELD_SINCE_UNTICKED);

        xp2Login("usr_xp2_mgr");
        assertThat(service.getRoleViewableProjects("role_xp2_plain").pendingReapprovals()).isEmpty();
        xpExpect(() -> service.applyGrant(xp2Reapproval("xp2-s27-apply", "usr_xp2_direct", "role_xp2_plain")),
                400, "VALIDATION_FAILED", "该员工对这个角色没有待重新审批的可查看项目");
        assertThat(xp2ReachOf("usr_xp2_direct", M002_VIEW).onlyOwnProject()).contains("org_a");
    }

    // M736-S28（治理地图第 528 行红线④「看不到别的商户和别的项目的数据」；本包读路径的商户半边，同 requireSameOrganization、
    // canViewGrantRequest）：甲项目的角色，带待批的持有人三个——不带商户归属的员工、商户 X 的账号、商户 Y 的账号。不带商户归属的
    // 角色管理人改勾选的返回体与读现状都列出三人；带商户 X 归属、持建角色权限的操作人读现状、改勾选的返回体都只列商户 X 的那一个，
    // 另两人的待批照写、只是不列。去掉名单过滤里的商户半边即红（多出另两人）。
    @Test
    void merchantBoundActorSeesOnlyPendingHoldersOfTheirOwnMerchant() {
        xp2Fixture();
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_a");
        xp2Holder("usr_xp2_plain", "org_a", "role_xp2");
        xp2MerchantUser("usr_xp2_merchant_x", "merchant_x");
        xp2MerchantUser("usr_xp2_merchant_y", "merchant_y");
        accessRepository.assignUserRole("usr_xp2_merchant_x", "role_xp2");
        accessRepository.assignUserRole("usr_xp2_merchant_y", "role_xp2");
        List<String> everyone = List.of("usr_xp2_merchant_x", "usr_xp2_merchant_y", "usr_xp2_plain");

        RoleViewableProjectsView changedByStaff = service.updateRoleViewableProjects("role_xp2", xp2Add("xp2-s28-add-c", "org_c"));
        assertThat(changedByStaff.pendingReapprovals()).extracting(RoleViewableProjectsView.PendingReapproval::userId)
                .as("不带商户归属的项目内操作人：改勾选的返回体").containsExactlyElementsOf(everyone);
        assertThat(service.getRoleViewableProjects("role_xp2").pendingReapprovals())
                .extracting(RoleViewableProjectsView.PendingReapproval::userId)
                .as("不带商户归属的项目内操作人：读现状").containsExactlyElementsOf(everyone);

        xp2MerchantUser("usr_xp2_mx_mgr", "merchant_x");
        accessRepository.userRoles.put("usr_xp2_mx_mgr", new LinkedHashSet<>(List.of("role_xp2_mgr")));
        authenticateAsMerchantUser("usr_xp2_mx_mgr", "usr_xp2_mx_mgr", "org_a", "merchant_x");
        assertThat(service.getRoleViewableProjects("role_xp2").pendingReapprovals())
                .extracting(RoleViewableProjectsView.PendingReapproval::userId)
                .as("带商户归属的操作人：读现状").containsExactly("usr_xp2_merchant_x");
        RoleViewableProjectsView changedByMerchant = service.updateRoleViewableProjects("role_xp2", xp2Add("xp2-s28-add-d", "org_d"));
        assertThat(changedByMerchant.pendingReapprovals()).as("带商户归属的操作人：改勾选的返回体").singleElement().satisfies(row -> {
            assertThat(row.userId()).isEqualTo("usr_xp2_merchant_x");
            assertThat(row.viewableIds()).containsExactly("org_c", "org_d");
        });
        for (String unlisted : List.of("usr_xp2_merchant_y", "usr_xp2_plain")) {
            assertThat(xp2Holds(unlisted, "role_xp2")).as("不列的人待批照写 " + unlisted).containsExactlyInAnyOrder("org_c", "org_d");
        }
    }

    // M736-S29（技术推导：改勾选取到角色行锁之后重列持有人、给新出现的人补锁员工行——并发的直接分配先到时给新持有人补上待批）：
    // 预列持有人之后、取角色行锁之前多出一个持有人（用仓储的锁前钩子落一行直接分配，不起线程）。只看调用先后：角色行锁之后有一次重列，
    // 重列之后补锁的正是新出现的那个人，补锁先于给他写待批。拿掉重列（holders 改用预列的 locked）或拿掉补锁那一行即红。
    @Test
    void holdersAreRelistedAndTheNewcomerIsLockedAfterTheRoleRowLock() {
        xp2Fixture();
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_a", "org_b");
        xp2Holder("usr_xp2_direct", "org_a", "role_xp2");
        xpUser("usr_xp2_late", "org_a");
        accessRepository.beforeNextUserLock = () -> accessRepository.assignUserRole("usr_xp2_late", "role_xp2");

        accessRepository.callTrace = new ArrayList<>();
        service.updateRoleViewableProjects("role_xp2", xp2Add("xp2-s29-add", "org_c"));
        List<String> change = List.copyOf(accessRepository.callTrace);

        assertThat(change).containsSubsequence("listRoleHolderIds:role_xp2", "lockAccessUsers:[usr_xp2_direct]", "lockRoleForEdit:role_xp2",
                "listRoleHolderIds:role_xp2", "lockAccessUsers:[usr_xp2_late]", "addRoleViewableProjectHolds:usr_xp2_late");
        int relist = change.lastIndexOf("listRoleHolderIds:role_xp2");
        int lateLock = change.indexOf("lockAccessUsers:[usr_xp2_late]");
        assertThat(relist).as("角色行锁之后重列持有人").isGreaterThan(change.indexOf("lockRoleForEdit:role_xp2"));
        assertThat(lateLock).as("重列之后补锁新出现的持有人").isGreaterThan(relist);
        assertThat(lateLock).as("补锁先于给他写待批").isLessThan(change.indexOf("addRoleViewableProjectHolds:usr_xp2_late"));
    }

    // M736-S30（技术推导：直接定岗一侧同样在员工行之后、读角色之前取角色行共享锁，与改勾选的角色行锁互斥）：平台管理员把员工定到
    // 派生可跨项目角色的岗位。只看调用先后：员工行锁先于角色行共享锁，读该角色在共享锁之后。拿掉 resolvePositionGrant 里的
    // lockRoleForGrant 即红。
    @Test
    void positionAssignmentTakesTheRoleRowShareLockBeforeReadingTheRole() {
        xp2Fixture();
        xpRole("role_xp2_cross", List.of(M002_VIEW), false, "org_b");
        xpTemplate("tpl_xp2_pos", null);
        xpUser("usr_xp2_seat", "org_a");
        authenticateAsPlatformAdmin("seed-org");
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "xp2-s30-map", null, "xp2_patrol", "lvl_m1_lower", "role_xp2_cross", "tpl_xp2_pos", "active"));

        accessRepository.callTrace = new ArrayList<>();
        service.assignUserPosition(new UserPositionAssignmentRequest(
                "xp2-s30-seat", null, "usr_xp2_seat", "xp2_patrol", "lvl_m1_lower", null, null, null));
        List<String> seat = List.copyOf(accessRepository.callTrace);

        assertThat(seat).containsSubsequence("lockAccessUsers:[usr_xp2_seat]", "lockRoleForGrant:role_xp2_cross", "findRoleById:role_xp2_cross");
        assertThat(seat.indexOf("lockAccessUsers:[usr_xp2_seat]")).as("员工行锁先于角色行共享锁")
                .isLessThan(seat.indexOf("lockRoleForGrant:role_xp2_cross"));
        assertThat(seat.indexOf("findRoleById:role_xp2_cross")).as("读角色在角色行共享锁之后")
                .isGreaterThan(seat.indexOf("lockRoleForGrant:role_xp2_cross"));
    }

    // M736-S31（技术推导：减少落地先取角色表锁、再取角色行锁，然后才删勾选行——与授权申请的审批落地、与改勾选串行）：减丙的复核单
    // 末级通过时监听同步落地。只看调用先后：角色表锁 → 角色行锁 → 删带该单号的勾选行。拿掉监听里两把锁的任何一把即红。
    @Test
    void removalLandingLocksTheRolesTableThenTheRoleRowBeforeDeletingTicks() {
        xp2Fixture();
        xp2Role("role_xp2", "org_a", List.of(M002_VIEW), false, "org_a", "org_b", "org_c");
        service.updateRoleViewableProjects("role_xp2", xp2Remove("xp2-s31-remove", "org_c"));
        String reviewId = xp2RemovalReview("role_xp2", "org_c");
        xp2Decide("usr_xp2_sup1", reviewId, "xp2-s31-first", "approved");

        accessRepository.callTrace = new ArrayList<>();
        xp2Decide("usr_xp2_sup2", reviewId, "xp2-s31-last", "approved");
        List<String> landing = List.copyOf(accessRepository.callTrace);

        assertThat(landing).containsSubsequence("lockRolesForEdit", "lockRoleForEdit:role_xp2", "applyRoleViewableProjectRemoval:role_xp2");
        assertThat(landing.indexOf("lockRolesForEdit")).as("角色表锁先于角色行锁").isLessThan(landing.indexOf("lockRoleForEdit:role_xp2"));
        assertThat(landing.indexOf("lockRoleForEdit:role_xp2")).as("角色行锁先于删勾选行")
                .isLessThan(landing.indexOf("applyRoleViewableProjectRemoval:role_xp2"));
    }
}
