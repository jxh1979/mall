package com.complex.platform.access;

// 文件版本：V0.04

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.dto.AccessGrantApplyRequest;
import com.complex.platform.access.dto.AccessGrantReviewRequest;
import com.complex.platform.access.dto.PermissionTemplateApplyRequest;
import com.complex.platform.access.dto.PermissionTemplateRequest;
import com.complex.platform.access.dto.PositionRoleTemplateRequest;
import com.complex.platform.access.dto.RoleAssignmentRequest;
import com.complex.platform.access.dto.RoleRequest;
import com.complex.platform.access.dto.StaffLevelAssignmentRequest;
import com.complex.platform.access.dto.StatusChangeRequest;
import com.complex.platform.access.dto.UserPermissionOverrideRequest;
import com.complex.platform.access.dto.UserPositionAssignmentRequest;
import com.complex.platform.access.dto.UserScopeChangeRequest;
import com.complex.platform.auth.AuthService;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AccessJdbcIntegrationTests {

    @Autowired
    private JdbcAccessRepository repository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private AccessService accessService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private org.springframework.transaction.interceptor.TransactionAttributeSource transactionAttributeSource;

    @Autowired
    private com.complex.platform.common.idempotency.IdempotencyService idempotencyService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void migrationCreatesPermissionTablesAndSeedsAccessAndOpsPermissions() {
        assertThat(regclass("permissions")).isEqualTo("permissions");
        assertThat(regclass("permission_template_items")).isEqualTo("permission_template_items");
        assertThat(regclass("permission_template_applications")).isEqualTo("permission_template_applications");
        assertThat(regclass("access_users")).isEqualTo("access_users");

        List<String> permissionCodes = repository.listPermissions().stream()
                .map(PermissionDefinition::permissionCode)
                .toList();

        assertThat(permissionCodes)
                .contains(
                        "access.permission_template.manage",
                        "access.permission_template.apply",
                        "access.temporary_grant.create",
                        "ops.production_readiness.manage",
                        "ops.production_readiness.check"
                );
    }

    @Test
    void v181MovesOutsourcedFacilityPermissionsToTimeBoxTemplatesWithoutRuntimeRolePermissions() {
        List<String> permissionCodes = repository.listPermissions().stream()
                .map(PermissionDefinition::permissionCode)
                .toList();

        assertThat(permissionCodes).contains(
                "facility.work_order.view_assigned",
                "facility.work_order.process_assigned",
                "facility.work_order.accept_assigned",
                "facility.equipment.view",
                "facility.inspection.view_assigned",
                "facility.inspection.record_assigned",
                "facility.inspection.template.view",
                "facility.inspection.report_exception_assigned"
        );
        assertThat(jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM role_permissions
                 WHERE role_id = 'role_outsourced_facility'
                 ORDER BY permission_code
                """, String.class)).isEmpty();
        assertThat(jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM role_permissions
                 WHERE role_id = 'role_outsourced_security_cleaning'
                 ORDER BY permission_code
                """, String.class)).isEmpty();
        assertThat(jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM permission_template_items
                 WHERE template_id = 'tpl_outsourced_facility'
                 ORDER BY permission_code
                """, String.class)).containsExactly(
                "facility.equipment.view",
                // HP-F01 V530：维保单位一侧确认故障处置规则，同样只经外包限时模板授予。
                "facility.fault_rule.confirm",
                "facility.work_order.process_assigned",
                "facility.work_order.view_assigned"
        );
        assertThat(jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM permission_template_items
                 WHERE template_id = 'tpl_outsourced_security_cleaning'
                 ORDER BY permission_code
                """, String.class)).containsExactly(
                "facility.inspection.record_assigned",
                "facility.inspection.report_exception_assigned",
                "facility.inspection.template.view",
                "facility.inspection.view_assigned"
        );
        assertThat(jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM permission_template_items
                 WHERE template_id = 'tpl_outsourced_facility'
                   AND permission_code = 'facility.work_order.accept_assigned'
                """, String.class)).isEmpty();
        assertThat(jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM permission_template_items
                 WHERE template_id = 'tpl_outsourced_security_cleaning'
                   AND permission_code LIKE '%.manage'
                """, String.class)).isEmpty();
        assertThat(jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM permission_template_items
                 WHERE template_id = 'tpl_outsourced_security_cleaning'
                 ORDER BY permission_code
                """, String.class)).contains(
                "facility.inspection.record_assigned",
                "facility.inspection.view_assigned"
        );
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM role_permissions
                 WHERE role_id IN ('role_outsourced_facility', 'role_outsourced_security_cleaning')
                   AND permission_code IS NOT NULL
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForList("""
                SELECT position_code
                  FROM position_role_templates
                 WHERE role_id IN ('role_outsourced_facility', 'role_outsourced_security_cleaning')
                 ORDER BY position_code
                """, String.class)).containsExactly("outsourced_facility", "outsourced_security_cleaning");
    }

    @Test
    void v181SeedsConstructionCompletionUploadAndKeepsRuntimeRoleTimeBoxed() {
        assertThat(regclass("site_entry_reports")).isEqualTo("site_entry_reports");
        assertThat(jdbcTemplate.queryForList("""
                SELECT column_name
                  FROM information_schema.columns
                 WHERE table_name = 'site_entry_reports'
                   AND column_name IN (
                        'planned_start_date',
                        'planned_end_date',
                        'vendor_user_id',
                        'completion_attachment_ids',
                        'completion_note',
                        'completion_uploaded_at'
                   )
                 ORDER BY column_name
                """, String.class)).containsExactly(
                "completion_attachment_ids",
                "completion_note",
                "completion_uploaded_at",
                "planned_end_date",
                "planned_start_date",
                "vendor_user_id"
        );

        assertThat(jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM role_permissions
                 WHERE role_id = 'role_outsourced_construction'
                 ORDER BY permission_code
                """, String.class)).isEmpty();
        assertThat(jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM permission_template_items
                 WHERE template_id = 'tpl_outsourced_construction'
                 ORDER BY permission_code
                """, String.class)).containsExactly(
                "facility.site_entry.create_assigned",
                "facility.site_entry.submit_assigned",
                "facility.site_entry.upload_completion_assigned",
                "facility.site_entry.view_assigned"
        );
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM role_permissions
                 WHERE role_id = 'role_outsourced_construction'
                   AND permission_code IN ('facility.site_entry.review', 'facility.site_entry.accept')
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForList("""
                SELECT role_id || ':' || permission_code
                  FROM role_permissions
                 WHERE role_id IN ('role_platform_admin', 'role_ops_manager', 'role_facility_manager')
                   AND permission_code IN ('facility.site_entry.review', 'facility.site_entry.accept')
                 ORDER BY role_id, permission_code
                """, String.class)).containsExactly(
                "role_facility_manager:facility.site_entry.accept",
                "role_facility_manager:facility.site_entry.review",
                "role_ops_manager:facility.site_entry.accept",
                "role_ops_manager:facility.site_entry.review",
                "role_platform_admin:facility.site_entry.accept",
                "role_platform_admin:facility.site_entry.review"
        );
    }

    @Test
    void v180SeedsAuditorRolesTemplatesAsReadOnlyWithoutChangingApprovalFlags() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT data_scope FROM roles WHERE id = 'role_auditor_internal'
                """, String.class)).isEqualTo("complex");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT data_scope FROM roles WHERE id = 'role_auditor_external'
                """, String.class)).isEqualTo("project");

        assertThat(jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM role_permissions
                 WHERE role_id = 'role_auditor_internal'
                 ORDER BY permission_code
                """, String.class)).contains(
                "finance.ar_ap.view",
                "analytics.dashboard.view",
                "amusement.maintenance.read",
                "ticket.order.read"
        );
        assertThat(jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM role_permissions
                 WHERE role_id = 'role_auditor_external'
                 ORDER BY permission_code
                """, String.class)).isEmpty();

        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM role_permissions
                 WHERE role_id IN ('role_auditor_internal', 'role_auditor_external')
                   AND permission_code ~ '\\.(manage|approve|export|create|submit|review|accept|process|resolve|refund|adjust|assign)$'
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM permission_template_items
                 WHERE template_id IN ('tpl_auditor_internal', 'tpl_auditor_external')
                   AND permission_code ~ '\\.(manage|approve|export|create|submit|review|accept|process|resolve|refund|adjust|assign)$'
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM permission_template_items
                 WHERE template_id = 'tpl_auditor_internal'
                   AND data_scope <> 'complex'
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM permission_template_items
                 WHERE template_id = 'tpl_auditor_external'
                   AND data_scope <> 'project'
                """, Integer.class)).isZero();
    }

    @Test
    void auditorReadOnlyAccessAndTemporaryGrantExpiryUseExistingAuthorizationEffects() {
        repository.createUser(new AccessUser(
                "usr_jdbc_auditor_internal",
                "jdbc-auditor-internal",
                "staff",
                "complex_1",
                null,
                "active",
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        repository.assignUserRole("usr_jdbc_auditor_internal", "role_auditor_internal");
        authenticate("usr_jdbc_auditor_internal", "jdbc-auditor-internal", "staff", "complex_1");

        authorizationService.requirePermission("finance.ar_ap.view");
        authorizationService.requirePermission("ticket.order.read");
        assertPermissionDenied("finance.ar_ap.manage");

        repository.createUser(new AccessUser(
                "usr_jdbc_auditor_external",
                "jdbc-auditor-external",
                "staff",
                "project_1",
                null,
                "active",
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        repository.assignUserRole("usr_jdbc_auditor_external", "role_auditor_external");
        authenticate("usr_jdbc_auditor_external", "jdbc-auditor-external", "staff", "project_1");
        assertPermissionDenied("finance.ar_ap.view");

        repository.createTemporaryGrant(new TemporaryGrant(
                "22222222-2222-2222-2222-222222222222",
                "usr_jdbc_auditor_external",
                List.of("finance.ar_ap.view"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2099-01-01T00:00:00Z"),
                false,
                false,
                "active",
                "external audit window",
                "seed-admin",
                Instant.parse("2026-06-14T00:00:00Z"),
                null,
                null
        ));

        authorizationService.requirePermission("finance.ar_ap.view");
        assertPermissionDenied("finance.ar_ap.manage");

        repository.createUser(new AccessUser(
                "usr_jdbc_auditor_external_expired",
                "jdbc-auditor-external-expired",
                "staff",
                "project_1",
                null,
                "active",
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        repository.createTemporaryGrant(new TemporaryGrant(
                "33333333-3333-3333-3333-333333333333",
                "usr_jdbc_auditor_external_expired",
                List.of("finance.ar_ap.view"),
                Instant.parse("2020-01-01T00:00:00Z"),
                Instant.parse("2020-01-02T00:00:00Z"),
                false,
                false,
                "active",
                "expired external audit window",
                "seed-admin",
                Instant.parse("2020-01-01T00:00:00Z"),
                null,
                null
        ));
        authenticate("usr_jdbc_auditor_external_expired", "jdbc-auditor-external-expired", "staff", "project_1");
        assertPermissionDenied("finance.ar_ap.view");
    }

    @Test
    void outsourcedTemporaryGrantExpiryUsesExistingAuthorizationEffects() {
        repository.createUser(new AccessUser(
                "usr_jdbc_outsourced_active",
                "jdbc-outsourced-active",
                "outsourced_vendor",
                "project_1",
                null,
                "active",
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        repository.createTemporaryGrant(new TemporaryGrant(
                "44444444-4444-4444-4444-444444444444",
                "usr_jdbc_outsourced_active",
                List.of("facility.work_order.view_assigned", "facility.equipment.view"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2099-01-01T00:00:00Z"),
                false,
                false,
                "active",
                "outsourced maintenance window",
                "seed-admin",
                Instant.parse("2026-06-14T00:00:00Z"),
                null,
                null
        ));
        authenticate("usr_jdbc_outsourced_active", "jdbc-outsourced-active", "outsourced_vendor", "project_1");

        authorizationService.requirePermission("facility.work_order.view_assigned");
        authorizationService.requirePermission("facility.equipment.view");
        assertPermissionDenied("facility.work_order.accept_assigned");
        assertPermissionDenied("facility.work_order.manage");

        repository.createUser(new AccessUser(
                "usr_jdbc_outsourced_expired",
                "jdbc-outsourced-expired",
                "outsourced_vendor",
                "project_1",
                null,
                "active",
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        repository.createTemporaryGrant(new TemporaryGrant(
                "55555555-5555-5555-5555-555555555555",
                "usr_jdbc_outsourced_expired",
                List.of("facility.work_order.view_assigned"),
                Instant.parse("2020-01-01T00:00:00Z"),
                Instant.parse("2020-01-02T00:00:00Z"),
                false,
                false,
                "active",
                "expired outsourced window",
                "seed-admin",
                Instant.parse("2020-01-01T00:00:00Z"),
                null,
                null
        ));
        authenticate("usr_jdbc_outsourced_expired", "jdbc-outsourced-expired", "outsourced_vendor", "project_1");
        assertPermissionDenied("facility.work_order.view_assigned");
    }

    @Test
    void migrationCreatesStaffLevelTablesAndSeedsThirtyContiguousRanks() {
        assertThat(regclass("staff_levels")).isEqualTo("staff_levels");
        assertThat(regclass("staff_level_assignments")).isEqualTo("staff_level_assignments");

        List<StaffLevelRecord> levels = repository.listStaffLevels();
        assertThat(levels).hasSize(30);
        assertThat(levels).extracting(StaffLevelRecord::levelRank)
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, 30).boxed().toList());
        assertThat(repository.findStaffLevelById("lvl_m1_lower")).get()
                .extracting(StaffLevelRecord::mainLevel, StaffLevelRecord::subLevel, StaffLevelRecord::levelRank)
                .containsExactly("M1", "lower", 1);
        assertThat(repository.findStaffLevelById("lvl_m10_upper")).get()
                .extracting(StaffLevelRecord::levelRank)
                .isEqualTo(30);
    }

    @Test
    void staffLevelAssignmentPersistsLevelAndSalary() {
        repository.createUser(new AccessUser(
                "usr_jdbc_level", "jdbc-level-user", "staff", "org", "merchant", "active",
                Instant.parse("2026-06-14T00:00:00Z")));
        repository.saveStaffLevelAssignment(new StaffLevelAssignment(
                "usr_jdbc_level", "lvl_m5_middle", 800_000L, "CNY", "月薪 8000", "org", "merchant",
                "access-api", Instant.parse("2026-06-14T00:00:00Z")));

        assertThat(repository.findStaffLevelAssignment("usr_jdbc_level")).get()
                .extracting(StaffLevelAssignment::staffLevelId, StaffLevelAssignment::baseSalaryAmountCent,
                        StaffLevelAssignment::salaryCurrency)
                .containsExactly("lvl_m5_middle", 800_000L, "CNY");
    }

    @Test
    void saveTemplatePersistsGrantAndDenyItems() {
        PermissionTemplate template = repository.createTemplate(
                template("tpl_jdbc_items", "jdbc-items", "source"),
                List.of(
                        item("tpl_jdbc_items", "ops.production_readiness.view", AccessRepository.Effect.GRANT),
                        item("tpl_jdbc_items", "ops.production_readiness.manage", AccessRepository.Effect.DENY)
                )
        );

        assertThat(template.id()).isEqualTo("tpl_jdbc_items");
        assertThat(repository.findTemplateItems("tpl_jdbc_items"))
                .extracting(PermissionTemplateItem::permissionCode, PermissionTemplateItem::effect)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("ops.production_readiness.view", AccessRepository.Effect.GRANT),
                        org.assertj.core.groups.Tuple.tuple("ops.production_readiness.manage", AccessRepository.Effect.DENY)
                );
    }

    @Test
    void copyTemplateStoresSnapshotIndependentFromLaterSourceChanges() {
        repository.createTemplate(
                template("tpl_source_snapshot", "source-snapshot", null),
                List.of(item("tpl_source_snapshot", "ops.production_readiness.view", AccessRepository.Effect.GRANT))
        );

        repository.copyTemplate("tpl_source_snapshot", template("tpl_copy_snapshot", "copy-snapshot", "tpl_source_snapshot"));
        repository.saveTemplate(
                template("tpl_source_snapshot", "source-snapshot-updated", null),
                List.of(item("tpl_source_snapshot", "ops.production_readiness.manage", AccessRepository.Effect.GRANT))
        );

        assertThat(repository.findTemplateItems("tpl_copy_snapshot"))
                .extracting(PermissionTemplateItem::permissionCode)
                .containsExactly("ops.production_readiness.view");
    }

    @Test
    void appliedTemporaryTemplateCanBeReadAsActivePermissionEffect() {
        repository.createUser(new AccessUser(
                "usr_jdbc_template",
                "jdbc-template-user",
                "staff",
                "org",
                "merchant",
                "active",
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        repository.createTemplate(
                template("tpl_temporary_active", "temporary-active", null),
                List.of(item("tpl_temporary_active", "ops.production_readiness.check", AccessRepository.Effect.GRANT))
        );

        List<PermissionTemplateApplication> applications = repository.applyTemplate(
                "tpl_temporary_active",
                List.of("usr_jdbc_template"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-15T00:00:00Z"),
                true,
                "seed-admin"
        );

        assertThat(applications).hasSize(1);
        assertThat(repository.findPermissionEffects("usr_jdbc_template", Instant.parse("2026-06-14T12:00:00Z")))
                .extracting(AccessRepository.PermissionEffect::permissionCode, AccessRepository.PermissionEffect::effect)
                .contains(org.assertj.core.groups.Tuple.tuple("ops.production_readiness.check", AccessRepository.Effect.GRANT));
    }

    @Test
    void revokingTemporaryGrantIdRevokesEveryPermissionCodeInGrantGroup() {
        TemporaryGrant grant = new TemporaryGrant(
                "11111111-1111-1111-1111-111111111111",
                "usr_jdbc_group_revoke",
                List.of("ops.production_readiness.view", "ops.production_readiness.check"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-15T00:00:00Z"),
                false,
                false,
                "active",
                "break glass",
                "seed-admin",
                Instant.parse("2026-06-14T00:00:00Z"),
                null,
                null
        );

        repository.createTemporaryGrant(grant);

        assertThat(jdbcTemplate.queryForList("""
                SELECT grant_group_id
                  FROM temporary_permission_grants
                 WHERE subject_id = ?
                 ORDER BY permission_code
                """, String.class, "usr_jdbc_group_revoke"))
                .containsExactly(grant.id(), grant.id());
        assertThat(repository.findPermissionEffects("usr_jdbc_group_revoke", Instant.parse("2026-06-14T12:00:00Z")))
                .extracting(AccessRepository.PermissionEffect::permissionCode)
                .containsExactlyInAnyOrder("ops.production_readiness.view", "ops.production_readiness.check");

        TemporaryGrant revoked = repository.revokeTemporaryGrant(
                grant.id(),
                "seed-admin",
                Instant.parse("2026-06-14T12:30:00Z")
        );

        assertThat(revoked.status()).isEqualTo("revoked");
        assertThat(revoked.permissionCodes())
                .containsExactlyInAnyOrder("ops.production_readiness.view", "ops.production_readiness.check");
        assertThat(jdbcTemplate.queryForList("""
                SELECT status
                  FROM temporary_permission_grants
                 WHERE subject_id = ?
                 ORDER BY permission_code
                """, String.class, "usr_jdbc_group_revoke"))
                .containsOnly("revoked");
        assertThat(repository.findPermissionEffects("usr_jdbc_group_revoke", Instant.parse("2026-06-14T12:31:00Z")))
                .extracting(AccessRepository.PermissionEffect::permissionCode)
                .doesNotContain("ops.production_readiness.view", "ops.production_readiness.check");
    }

    // M006-G-06（PGM-1.10.1「到期、单次消耗、失效全部留痕」）：到期的临时授权须置为 expired 并写用量日志，且幂等。
    @Test
    void lapsedTemporaryGrantIsExpiredAndLoggedWhenEffectsAreRead() {
        repository.createTemporaryGrant(new TemporaryGrant(
                "22222222-2222-2222-2222-222222222222",
                "usr_jdbc_expiry",
                List.of("ops.production_readiness.view"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-15T00:00:00Z"),
                false,
                false,
                "active",
                "break glass",
                "seed-admin",
                Instant.parse("2026-06-14T00:00:00Z"),
                null,
                null
        ));

        assertThat(repository.findPermissionEffects("usr_jdbc_expiry", Instant.parse("2026-06-15T00:00:00Z")))
                .extracting(AccessRepository.PermissionEffect::permissionCode)
                .doesNotContain("ops.production_readiness.view");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM temporary_permission_grants WHERE subject_id = ?", String.class, "usr_jdbc_expiry"))
                .isEqualTo("expired");
        assertThat(jdbcTemplate.queryForList(
                "SELECT result FROM permission_template_usage_logs WHERE user_id = ?", String.class, "usr_jdbc_expiry"))
                .containsExactly("expired");

        repository.findPermissionEffects("usr_jdbc_expiry", Instant.parse("2026-06-16T00:00:00Z"));
        assertThat(jdbcTemplate.queryForList(
                "SELECT result FROM permission_template_usage_logs WHERE user_id = ?", String.class, "usr_jdbc_expiry"))
                .hasSize(1);
    }

    @Test
    void lapsedTemplateApplicationIsExpiredAndLoggedWhenEffectsAreRead() {
        repository.createUser(new AccessUser(
                "usr_jdbc_tpl_expiry",
                "jdbc-tpl-expiry",
                "staff",
                "org",
                "merchant",
                "active",
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        repository.createTemplate(
                template("tpl_temporary_lapsed", "temporary-lapsed", null),
                List.of(item("tpl_temporary_lapsed", "ops.production_readiness.check", AccessRepository.Effect.GRANT))
        );
        String applicationId = repository.applyTemplate(
                "tpl_temporary_lapsed",
                List.of("usr_jdbc_tpl_expiry"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-15T00:00:00Z"),
                false,
                "seed-admin"
        ).get(0).id();

        assertThat(repository.findPermissionEffects("usr_jdbc_tpl_expiry", Instant.parse("2026-06-15T00:00:00Z")))
                .extracting(AccessRepository.PermissionEffect::permissionCode)
                .doesNotContain("ops.production_readiness.check");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM permission_template_applications WHERE id = ?", String.class, applicationId))
                .isEqualTo("expired");
        assertThat(jdbcTemplate.queryForMap(
                "SELECT application_id, result FROM permission_template_usage_logs WHERE user_id = ?", "usr_jdbc_tpl_expiry"))
                .containsEntry("application_id", applicationId)
                .containsEntry("result", "expired");
    }

    // M001-F03（PGM 权限规则「到期自动失效」「到期……全部留痕」）：无人读取该员工权限时，到期作业也要置 expired
    // 并恰写一条到期日志；重复运行不重复记账。
    @Test
    void lapsedTemplateApplicationIsExpiredByJobWithoutAnyEffectsRead() {
        repository.createUser(new AccessUser(
                "usr_jdbc_tpl_job",
                "jdbc-tpl-job",
                "staff",
                "org",
                "merchant",
                "active",
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        repository.createTemplate(
                template("tpl_temporary_job", "temporary-job", null),
                List.of(item("tpl_temporary_job", "ops.production_readiness.check", AccessRepository.Effect.GRANT))
        );
        String applicationId = repository.applyTemplate(
                "tpl_temporary_job",
                List.of("usr_jdbc_tpl_job"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-15T00:00:00Z"),
                false,
                "seed-admin"
        ).get(0).id();
        PermissionExpiryJob job = new PermissionExpiryJob(repository, false);

        assertThat(job.runOnce(Instant.parse("2026-06-16T00:00:00Z"))).isGreaterThanOrEqualTo(1);
        assertThat(job.runOnce(Instant.parse("2026-06-17T00:00:00Z"))).isZero();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM permission_template_applications WHERE id = ?", String.class, applicationId))
                .isEqualTo("expired");
        assertThat(jdbcTemplate.queryForList(
                "SELECT result FROM permission_template_usage_logs WHERE application_id = ?", String.class, applicationId))
                .containsExactly("expired");
    }

    // M032-001（治理地图「全平台审批通用时限」为后台业务参数）：库内现行授权政策行（V389 起带 deadlineWorkingDays）
    // 必须能按服务层同一解析路径读出；保存一份不带该键的政策后，库内该键保持原值。
    @Test
    void storedGrantPolicyParsesAndSavingKeepsDeadlineWorkingDays() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String stored = repository.findGrantPolicy().orElseThrow();
        int days = mapper.readTree(stored).path("deadlineWorkingDays").path("value").asInt();
        assertThat(days).isPositive();

        AccessGrantPolicy current = mapper.readValue(stored, AccessGrantPolicy.class);

        repository.saveGrantPolicy(mapper.writeValueAsString(new AccessGrantPolicy(
                current.windowDays(), current.positionThreshold(), current.userThreshold(), current.riskReviews())));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT (parameters -> 'deadlineWorkingDays' ->> 'value')::int FROM supervisor_review_policies"
                        + " WHERE policy_code = 'access_grant'", Integer.class)).isEqualTo(days);
        assertThat(mapper.readValue(repository.findGrantPolicy().orElseThrow(), AccessGrantPolicy.class).windowDays().value())
                .isEqualTo(current.windowDays().value());
    }

    // M034-001（枚举速查「系统不提供人工放行…任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过」）：
    // 人工放行权限码已停用，且不留任何授予来源。
    @Test
    void manualOverridePermissionIsRetiredAndGrantedToNobody() {
        assertThat(repository.permissionExists("verification.manual_override")).isFalse();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT (SELECT count(*) FROM role_permissions WHERE permission_code = 'verification.manual_override')
                     + (SELECT count(*) FROM permission_template_items WHERE permission_code = 'verification.manual_override')
                     + (SELECT count(*) FROM user_permission_overrides WHERE permission_code = 'verification.manual_override')
                     + (SELECT count(*) FROM temporary_permission_grants
                         WHERE permission_code = 'verification.manual_override' AND status = 'active')
                """, Long.class)).isZero();
    }

    // M006-G-07（PGM-1.10.1「旧派生同步失效」）：已定岗改职级复用定岗的四步写入，两个入口都须声明事务。
    // M035-003：本测试只断言 assignUserPosition 与 assignStaffLevel 两个入口带事务声明，不执行四步写入本身
    // （四步路径的行为测试在 AccessServiceTests）。
    @Test
    void positionAndStaffLevelEntryPointsDeclareATransaction() throws NoSuchMethodException {
        assertThat(transactionAttributeSource.getTransactionAttribute(AccessService.class.getMethod(
                "assignUserPosition", com.complex.platform.access.dto.UserPositionAssignmentRequest.class), AccessService.class))
                .isNotNull();
        assertThat(transactionAttributeSource.getTransactionAttribute(AccessService.class.getMethod(
                "assignStaffLevel", com.complex.platform.access.dto.StaffLevelAssignmentRequest.class), AccessService.class))
                .isNotNull();
    }

    // M006-G-08（PGM-1.18「界面全中文」）：治理页 / 模板页直接上屏的权限名、角色名、模板名不含拉丁字母
    // （permission_code / id 是机读标识，不在此列）。M035-001：治理地图「不得出现任何英文…可以不用中文的只有四样，没有第五样」，
    // 「AI」不是豁免。
    @Test
    void seededDisplayNamesAreChinese() {
        assertThat(jdbcTemplate.queryForList("""
                SELECT permission_code FROM permissions
                 WHERE permission_name ~ '[A-Za-z]'
                 ORDER BY permission_code
                """, String.class)).isEmpty();
        assertThat(jdbcTemplate.queryForList(
                "SELECT id FROM roles WHERE role_name ~ '[A-Za-z]' ORDER BY id", String.class)).isEmpty();
        assertThat(jdbcTemplate.queryForList(
                "SELECT id FROM permission_templates WHERE template_name ~ '[A-Za-z]' ORDER BY id", String.class)).isEmpty();
    }

    // M006-G-01（PGM-1.10.1「员工单独授予 − 显式拒绝」）：员工级覆盖在真实表上幂等覆盖、进入有效权限来源、可移除。
    @Test
    void userPermissionOverridesAreUpsertedReadAsEffectsAndRemovable() {
        repository.createUser(new AccessUser(
                "usr_jdbc_override", "jdbc-override", "staff", "org", "merchant", "active", Instant.parse("2026-06-14T00:00:00Z")));
        repository.saveUserPermissionOverrides("usr_jdbc_override", List.of("ops.production_readiness.view"),
                AccessRepository.Effect.DENY, "停权", "seed-admin");
        repository.saveUserPermissionOverrides("usr_jdbc_override", List.of("ops.production_readiness.view"),
                AccessRepository.Effect.DENY, "再次停权", "seed-admin");

        assertThat(repository.findPermissionEffects("usr_jdbc_override", Instant.parse("2026-06-14T12:00:00Z")))
                .filteredOn(effect -> effect.source() == AccessRepository.Source.USER_OVERRIDE)
                .extracting(AccessRepository.PermissionEffect::permissionCode, AccessRepository.PermissionEffect::effect)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("ops.production_readiness.view", AccessRepository.Effect.DENY));

        assertThat(repository.deleteUserPermissionOverrides("usr_jdbc_override", List.of("ops.production_readiness.view"),
                AccessRepository.Effect.DENY)).isEqualTo(1);
        assertThat(repository.findPermissionEffects("usr_jdbc_override", Instant.parse("2026-06-14T12:00:00Z")))
                .filteredOn(effect -> effect.source() == AccessRepository.Source.USER_OVERRIDE)
                .isEmpty();
    }

    // M007-F-05（PGM-1.10.1 五类权限授出前须先审批：资金/分红发放、敏感经营数据查看/导出）：字典分类不得为「日常」。
    // 18AC 复核修复（PGM:666 第五类「税费规则与税费调整」）：税费申报登记 finance.entity_tax.manage（V616）同列。
    @Test
    void moneyAndSensitiveExportPermissionsRequireApproval() {
        assertThat(jdbcTemplate.queryForList("""
                SELECT permission_code FROM permissions
                 WHERE requires_approval = FALSE
                   AND permission_code IN ('finance.dividend.manage', 'finance.ar_ap.export',
                                           'analytics.report.export', 'bar.internal_settlement_price.view',
                                           'finance.entity_tax.manage')
                 ORDER BY permission_code
                """, String.class)).isEmpty();
    }

    // M087-I3-01（PGM-1.10.1 范围变更与模板内高危先审后生效）：两种新申请类型必须能在权威 schema 落库；未知类型仍被 CHECK 拒绝。
    // M002-F01：员工单独授予、解除员工显式拒绝两类申请同样须能落库（V434）。
    @Test
    void scopeChangeAndHighRiskReleaseRequestTypesAreAcceptedByTheGrantRequestCheck() {
        for (String type : List.of("user_scope_change", "position_high_risk_release", "user_permission_grant", "user_permission_deny_removal")) {
            jdbcTemplate.update("""
                    INSERT INTO access_grant_requests (id, request_type, target_user_id, status, reason, requested_by, created_at, updated_at)
                    VALUES (?, ?, 'usr_m087_target', 'pending', 'M087 schema check', 'usr_m087_admin', now(), now())
                    """, "agr_m087_" + type, type);
            assertThat(jdbcTemplate.queryForObject("SELECT request_type FROM access_grant_requests WHERE id = ?", String.class, "agr_m087_" + type))
                    .isEqualTo(type);
        }
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO access_grant_requests (id, request_type, target_user_id, status, reason, requested_by, created_at, updated_at)
                VALUES ('agr_m087_unknown', 'user_scope_change_unknown', 'usr_m087_target', 'pending', 'x', 'usr_m087_admin', now(), now())
                """)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    // M002-F02（PGM「显式拒绝优先级最高」）：岗位高危挂起行与员工显式拒绝在真实表上分行存放——同码显式拒绝不改写挂起行，
    // 两行都作为拒绝进入有效权限来源；释放挂起只删挂起行，解除显式拒绝只删显式行。
    @Test
    void positionHoldAndExplicitDenyAreSeparateRows() {
        String user = "usr_m002_rows";
        String code = "ops.production_readiness.check";
        repository.createUser(new AccessUser(user, "m002-rows", "staff", "org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        repository.saveUserPermissionOverrides(user, List.of(code), AccessRepository.Effect.GRANT, "员工单独授予", "seed-admin");
        repository.saveHighRiskHolds(user, List.of(code), "岗位高危挂起", "seed-admin");
        repository.saveUserPermissionOverrides(user, List.of(code), AccessRepository.Effect.DENY, "员工显式拒绝", "seed-admin");

        assertThat(denyOrigins(user, code)).containsExactly("explicit", "position_hold");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT reason FROM user_permission_overrides WHERE user_id = ? AND permission_code = ? AND origin = 'position_hold'",
                String.class, user, code)).isEqualTo("岗位高危挂起");
        List<AccessRepository.PermissionEffect> denies = repository.findPermissionEffects(user, Instant.parse("2026-06-14T12:00:00Z")).stream()
                .filter(effect -> effect.effect() == AccessRepository.Effect.DENY && code.equals(effect.permissionCode()))
                .toList();
        assertThat(denies).extracting(AccessRepository.PermissionEffect::source)
                .containsExactlyInAnyOrder(AccessRepository.Source.USER_OVERRIDE, AccessRepository.Source.POSITION_HOLD);
        assertThat(denies).extracting(AccessRepository.PermissionEffect::sourceId).doesNotHaveDuplicates();

        assertThat(repository.deleteHighRiskHolds(user, List.of(code))).isEqualTo(1);
        assertThat(denyOrigins(user, code)).containsExactly("explicit");
        assertThat(repository.deleteExplicitDenies(user, List.of(code))).isEqualTo(1);
        assertThat(denyOrigins(user, code)).isEmpty();
        assertThat(jdbcTemplate.queryForList(
                "SELECT effect || '/' || origin FROM user_permission_overrides WHERE user_id = ? AND permission_code = ?",
                String.class, user, code)).containsExactly("grant/explicit");

        // 仓储方法按码删该 effect 的全部来源行：挂起行与显式行一起删（服务层直接移除拒绝自 M002-F05 起改走 deleteExplicitDenies）
        repository.saveHighRiskHolds(user, List.of(code), "岗位高危挂起", "seed-admin");
        repository.saveUserPermissionOverrides(user, List.of(code), AccessRepository.Effect.DENY, "员工显式拒绝", "seed-admin");
        assertThat(repository.deleteUserPermissionOverrides(user, List.of(code), AccessRepository.Effect.DENY)).isEqualTo(2);
        assertThat(denyOrigins(user, code)).isEmpty();
    }

    // M002-F02：origin CHECK——挂起行只能是拒绝。
    @Test
    void overrideOriginCheckRejectsGrantHold() {
        repository.createUser(new AccessUser(
                "usr_m002_hold_grant", "m002-hold-grant", "staff", "org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO user_permission_overrides (user_id, permission_code, effect, origin)
                VALUES ('usr_m002_hold_grant', 'ops.production_readiness.check', 'grant', 'position_hold')
                """))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                .hasMessageContaining("chk_user_permission_overrides_origin");
    }

    // M002-F02：origin CHECK——只认 explicit / position_hold。
    @Test
    void overrideOriginCheckRejectsUnknownOrigin() {
        repository.createUser(new AccessUser(
                "usr_m002_other_origin", "m002-other-origin", "staff", "org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO user_permission_overrides (user_id, permission_code, effect, origin)
                VALUES ('usr_m002_other_origin', 'ops.production_readiness.check', 'deny', 'other')
                """))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                .hasMessageContaining("chk_user_permission_overrides_origin");
    }

    // M002-F47（治理地图 #audit 第 893、899 行：权限变更留痕要能查到「为什么」）：真库回归——建角色、直接分配、撤销的成功事件
    // audit_events.metadata 记请求提交的 operator_note 原文；同键同请求回放分配不新增事件、不重新写入已撤销的角色。
    @Test
    void roleSuccessEventsKeepTheSubmittedOperatorNoteOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        String daily = "m002.note_test.daily_" + f.suffix();
        jdbcTemplate.update("""
                INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
                VALUES (?, 'M002 留痕测试日常权限', 'ops', 'medium', FALSE, 'active')
                """, daily);
        String[] keys = {"m002-f47-role-" + f.suffix(), "m002-f47-assign-" + f.suffix(), "m002-f47-unassign-" + f.suffix()};
        String[] roleId = new String[1];
        m002ActAs(f.admin(), "platform_admin", () -> roleId[0] = accessService.createRole(new RoleRequest(
                keys[0], "为临时代班建立职责角色", List.of(), "M002 代班角色 " + f.suffix(), null, List.of(daily), "project")).id());
        RoleAssignmentRequest assign = new RoleAssignmentRequest(keys[1], "代替休假员工值班", f.target(), roleId[0]);
        m002ActAs(f.admin(), "platform_admin", () -> accessService.assignUserRole(assign));
        m002ActAs(f.admin(), "platform_admin", () -> accessService.unassignUserRole(
                new RoleAssignmentRequest(keys[2], "代班结束收回权限", f.target(), roleId[0])));
        List<String> expected = List.of("role.changed/为临时代班建立职责角色", "access.role.assigned/代替休假员工值班",
                "access.role.unassigned/代班结束收回权限");
        assertThat(m002NoteEvents(keys)).containsExactlyInAnyOrderElementsOf(expected);

        m002ActAs(f.admin(), "platform_admin", () -> accessService.assignUserRole(assign));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM user_roles WHERE user_id = ? AND role_id = ?",
                Long.class, f.target(), roleId[0])).isZero();
        assertThat(m002NoteEvents(keys)).containsExactlyInAnyOrderElementsOf(expected);
    }

    // M002-F48（F47 验收「各自精确等于请求原文」）：真库回归——建角色、直接分配、撤销的 audit_events.metadata.operator_note
    // 对首尾空格、换行、纯空白均原样保存。
    @Test
    void roleSuccessEventsKeepTheOperatorNoteVerbatimOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        String daily = "m002.raw_note_test.daily_" + f.suffix();
        jdbcTemplate.update("""
                INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
                VALUES (?, 'M002 原文留痕测试日常权限', 'ops', 'medium', FALSE, 'active')
                """, daily);
        List<String> notes = List.of("  首尾空格  ", "换行结尾\n", "\r\n前后换行\r\n", "   ");
        for (int i = 0; i < notes.size(); i++) {
            String note = notes.get(i);
            String[] keys = {"m002-f48-role-" + i + "-" + f.suffix(), "m002-f48-assign-" + i + "-" + f.suffix(),
                    "m002-f48-unassign-" + i + "-" + f.suffix()};
            String roleName = "M002 原文角色 " + i + " " + f.suffix();
            String[] roleId = new String[1];
            m002ActAs(f.admin(), "platform_admin", () -> roleId[0] = accessService.createRole(new RoleRequest(
                    keys[0], note, List.of(), roleName, null, List.of(daily), "project")).id());
            m002ActAs(f.admin(), "platform_admin", () -> accessService.assignUserRole(
                    new RoleAssignmentRequest(keys[1], note, f.target(), roleId[0])));
            m002ActAs(f.admin(), "platform_admin", () -> accessService.unassignUserRole(
                    new RoleAssignmentRequest(keys[2], note, f.target(), roleId[0])));
            assertThat(m002NoteEvents(keys)).as("operator_note=%s", note).containsExactlyInAnyOrder(
                    "role.changed/" + note, "access.role.assigned/" + note, "access.role.unassigned/" + note);
        }
    }

    private List<String> m002NoteEvents(String[] keys) {
        return jdbcTemplate.queryForList("""
                SELECT event_name || '/' || coalesce(metadata ->> 'operator_note', '<无>') FROM audit_events
                 WHERE event_result = 'success' AND idempotency_key IN (?, ?, ?)
                """, String.class, (Object[]) keys);
    }

    // M002-F02：服务级真库回归——定岗挂起 → 员工显式拒绝 → 批准释放单：显式拒绝行仍在，有效权限来源里该码仍是拒绝。
    @Test
    void explicitDenyWrittenWhileAHoldIsPendingSurvivesTheReleaseOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        String role = "role_m002_" + f.suffix();
        String template = "tpl_m002_" + f.suffix();
        String position = "m002_pos_" + f.suffix();
        repository.createRole(new RoleRecord(role, "M002 测试角色 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of(f.code()), null));
        repository.createTemplate(baseTemplate(template), List.of());
        repository.savePositionRoleTemplate(new PositionRoleTemplate("prt_m002_" + f.suffix(), position, "lvl_m1_lower", role, template,
                "org", "active", f.admin(), Instant.now(), Instant.now()));
        m002ActAs(f.admin(), "platform_admin", () -> accessService.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-j4-assign-" + f.suffix(), "M002", f.target(), position, "lvl_m1_lower", null, null, null)));
        String holdId = m002PendingRequestId(f.target(), "position_high_risk_release");
        m002ActAs(f.admin(), "platform_admin", () -> accessService.setUserPermissionOverrides(f.target(), new UserPermissionOverrideRequest(
                "m002-j4-deny-" + f.suffix(), "M002", List.of(f.code()), "deny", "挂起期间员工显式拒绝")));
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("explicit", "position_hold");

        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(holdId, new AccessGrantReviewRequest(
                "m002-j4-review-" + f.suffix(), "M002", List.of(), "approve", "同意")));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM access_grant_requests WHERE id = ?", String.class, holdId))
                .isEqualTo("applied");
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("explicit");
        assertThat(repository.findPermissionEffects(f.target(), Instant.now()))
                .filteredOn(effect -> f.code().equals(effect.permissionCode()) && effect.effect() == AccessRepository.Effect.DENY)
                .extracting(AccessRepository.PermissionEffect::source)
                .containsExactly(AccessRepository.Source.USER_OVERRIDE);
    }

    // M002-F01：服务级真库回归——员工单独授予申请获批后，覆盖表写入 effect=grant、origin=explicit 的行。
    @Test
    void approvedUserPermissionGrantRequestWritesAnExplicitGrantRow() {
        M002Fixture f = m002ApprovalFixture();
        m002ActAs(f.admin(), "platform_admin", () -> accessService.applyGrant(new AccessGrantApplyRequest(
                "m002-j5-apply-" + f.suffix(), "M002", List.of(), "user_permission_grant", f.target(), null, null, null, null, null, null,
                List.of(f.code()), null, null, null, "员工单独授予申请")));
        String requestId = m002PendingRequestId(f.target(), "user_permission_grant");
        assertThat(jdbcTemplate.queryForList("SELECT origin FROM user_permission_overrides WHERE user_id = ?", String.class, f.target()))
                .isEmpty();

        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(requestId, new AccessGrantReviewRequest(
                "m002-j5-review-" + f.suffix(), "M002", List.of(), "approve", "同意")));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM access_grant_requests WHERE id = ?", String.class, requestId))
                .isEqualTo("applied");
        assertThat(jdbcTemplate.queryForList(
                "SELECT effect || '/' || origin FROM user_permission_overrides WHERE user_id = ? AND permission_code = ?",
                String.class, f.target(), f.code())).containsExactly("grant/explicit");
    }

    // M002-F04：真库回归——审批链上的终审人已属其他组织，员工单独授予获批后照常落地（不按终审人组织判定）。
    @Test
    void crossOrganizationChainApproverLandsUserPermissionGrantOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        String requestId = m002ApplyOverride(f, "user_permission_grant", "m002-j1");
        jdbcTemplate.update("UPDATE access_users SET organization_id = 'org_other' WHERE id = ?", f.boss());

        m002ActAs(f.boss(), "staff", "org_other", () -> accessService.reviewGrant(requestId, m002Approve("m002-j1-review-" + f.suffix())));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM access_grant_requests WHERE id = ?", String.class, requestId))
                .isEqualTo("applied");
        assertThat(jdbcTemplate.queryForList(
                "SELECT effect || '/' || origin FROM user_permission_overrides WHERE user_id = ? AND permission_code = ?",
                String.class, f.target(), f.code())).containsExactly("grant/explicit");
    }

    // M002-F04：真库回归——critical 码按库内授权政策走最高权限（链尾 master 节点），最高权限终审后照常落地。
    @Test
    void masterAuthorityFinalNodeLandsCriticalUserPermissionGrantOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        jdbcTemplate.update("UPDATE permissions SET risk_level = 'critical' WHERE permission_code = ?", f.code());
        String requestId = m002ApplyOverride(f, "user_permission_grant", "m002-j2");
        String reviewId = jdbcTemplate.queryForObject("SELECT review_id FROM access_grant_requests WHERE id = ?", String.class, requestId);
        assertThat(jdbcTemplate.queryForList(
                "SELECT approver_kind FROM supervisor_review_nodes WHERE review_id = ? ORDER BY seq", String.class, reviewId))
                .containsExactly("user", "master");

        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(requestId, m002Approve("m002-j2-boss-" + f.suffix())));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM access_grant_requests WHERE id = ?", String.class, requestId))
                .isEqualTo("pending");
        m002ActAsMaster(() -> accessService.reviewGrant(requestId, m002Approve("m002-j2-master-" + f.suffix())));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM access_grant_requests WHERE id = ?", String.class, requestId))
                .isEqualTo("applied");
        assertThat(jdbcTemplate.queryForList(
                "SELECT effect || '/' || origin FROM user_permission_overrides WHERE user_id = ? AND permission_code = ?",
                String.class, f.target(), f.code())).containsExactly("grant/explicit");
    }

    // M002-F05：真库回归——目标经角色持有日常码并有显式拒绝，不持有该码的操作人直接解除被拒，显式拒绝保留。
    @Test
    void dailyDenyRemovalByANonHolderIsRejectedOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        String daily = "m002.override_test.daily_" + f.suffix();
        jdbcTemplate.update("""
                INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
                VALUES (?, 'M002 测试日常权限', 'ops', 'medium', FALSE, 'active')
                """, daily);
        String role = "role_m002_daily_" + f.suffix();
        repository.createRole(new RoleRecord(role, "M002 日常角色 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of(daily), null));
        repository.assignUserRole(f.target(), role);
        m002ActAs(f.admin(), "platform_admin", () -> accessService.setUserPermissionOverrides(f.target(), new UserPermissionOverrideRequest(
                "m002-j3-deny-" + f.suffix(), "M002", List.of(daily), "deny", "员工显式拒绝")));

        assertThatThrownBy(() -> m002ActAs(f.boss(), "staff", () -> accessService.removeUserPermissionOverrides(f.target(),
                new UserPermissionOverrideRequest("m002-j3-remove-" + f.suffix(), "M002", List.of(daily), "deny", "解除"))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_EXCEEDS_OWN_PERMISSIONS"));
        assertThat(denyOrigins(f.target(), daily)).containsExactly("explicit");
    }

    // M002-F06：真库回归——解除单待审期间，另一人以新理由重写同码显式拒绝；批准报 GRANT_SNAPSHOT_CHANGED，新拒绝保留、申请仍待审。
    @Test
    void explicitDenyRewrittenByAnotherOperatorVoidsThePendingRemovalOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        String requestId = m002PendingDenyRemoval(f, "m002-j4");
        m002ActAs(f.boss(), "staff", () -> accessService.setUserPermissionOverrides(f.target(), new UserPermissionOverrideRequest(
                "m002-j4-redeny-" + f.suffix(), "M002", List.of(f.code()), "deny", "再次停权")));

        assertThatThrownBy(() -> m002ActAs(f.boss(), "staff",
                () -> accessService.reviewGrant(requestId, m002Approve("m002-j4-review-" + f.suffix()))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_SNAPSHOT_CHANGED"));
        assertThat(jdbcTemplate.queryForList(
                "SELECT reason || '/' || created_by FROM user_permission_overrides WHERE user_id = ? AND permission_code = ? AND origin = 'explicit'",
                String.class, f.target(), f.code())).containsExactly("再次停权/" + f.boss());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM access_grant_requests WHERE id = ?", String.class, requestId))
                .isEqualTo("pending");
    }

    // M002-F06：真库回归——拒绝行的写入时间也钉入指纹（同人同理由在另一事务重写时只有 created_at 变化）。
    @Test
    void explicitDenyVersionIsPinnedByCreatedAtOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        String requestId = m002PendingDenyRemoval(f, "m002-j5");
        jdbcTemplate.update("""
                UPDATE user_permission_overrides SET created_at = created_at + interval '1 second'
                 WHERE user_id = ? AND permission_code = ? AND origin = 'explicit'
                """, f.target(), f.code());

        assertThatThrownBy(() -> m002ActAs(f.boss(), "staff",
                () -> accessService.reviewGrant(requestId, m002Approve("m002-j5-review-" + f.suffix()))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_SNAPSHOT_CHANGED"));
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("explicit");
    }

    // M002-F03（R-001）：真库回归——USB 最高权限会话的有效集 = permissions 表全部 active 码（含须审批高危码）− 显式拒绝。
    @Test
    void masterAuthorityEffectiveSetIsTheWholeActiveDictionaryOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        EffectivePermissions[] result = new EffectivePermissions[1];
        m002ActAsMaster(() -> result[0] = authorizationService.effectivePermissions());
        List<String> active = jdbcTemplate.queryForList("SELECT permission_code FROM permissions WHERE status = 'active'", String.class);
        assertThat(result[0].grants()).contains(f.code())
                .containsExactlyInAnyOrderElementsOf(active.stream().filter(code -> !result[0].denies().contains(code)).toList());
        assertThat(result[0].wildcard()).isFalse();
    }

    // M002-F08（操作员 2026-09-25 裁决「本人免审批，他人须审批」）：真库回归——最高权限账号同码有高危授予来源与员工显式拒绝：
    // 本人会话直接解除成功且只删显式行；他人走解除申请、获批后只删显式行；持码的平台管理员直接解除须审批；普通目标同组合仍须审批。
    // M002-F19（操作员 2026-09-25 裁决「最高权限账号不能被定岗」）：最高权限账号没有岗位高危挂起行，本人解除后该码不再有任何拒绝
    // （原先固化的「本人解除后仍剩挂起行」按裁决改写）。
    @Test
    void masterAuthorityTargetDenyRemovalIsDirectOnlyForTheMasterSessionOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        String master = m002MasterAccount();
        String role = "role_m002_master_" + f.suffix();
        repository.createRole(new RoleRecord(role, "M002 高危角色 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of(f.code()), null));
        for (String user : List.of(master, f.target(), f.admin())) {
            repository.assignUserRole(user, role);
        }
        for (String user : List.of(master, f.target())) {
            repository.saveUserPermissionOverrides(user, List.of(f.code()), AccessRepository.Effect.DENY, "员工显式拒绝", f.admin());
        }
        assertThat(denyOrigins(master, f.code())).containsExactly("explicit");

        m002ActAsMaster(() -> accessService.removeUserPermissionOverrides(master, m002RemoveDeny("m002-j7-self-" + f.suffix(), f.code())));
        assertThat(denyOrigins(master, f.code())).isEmpty();
        m002ActAsMaster(() -> authorizationService.requirePermission(f.code()));

        repository.saveUserPermissionOverrides(master, List.of(f.code()), AccessRepository.Effect.DENY, "再次停权", f.admin());
        m002ActAs(f.admin(), "platform_admin", () -> accessService.applyGrant(new AccessGrantApplyRequest(
                "m002-j7-apply-" + f.suffix(), "M002", List.of(), "user_permission_deny_removal", master, null, null, null, null, null, null,
                List.of(f.code()), null, null, null, "解除最高权限账号显式拒绝")));
        String requestId = m002PendingRequestId(master, "user_permission_deny_removal");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(requestId, m002Approve("m002-j7-review-" + f.suffix())));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM access_grant_requests WHERE id = ?", String.class, requestId))
                .isEqualTo("applied");
        assertThat(denyOrigins(master, f.code())).isEmpty();

        repository.saveUserPermissionOverrides(master, List.of(f.code()), AccessRepository.Effect.DENY, "三次停权", f.admin());
        assertThatThrownBy(() -> m002ActAs(f.admin(), "platform_admin", () -> accessService.removeUserPermissionOverrides(master,
                m002RemoveDeny("m002-j7-admin-" + f.suffix(), f.code()))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_REQUIRES_APPROVAL"));
        assertThat(denyOrigins(master, f.code())).containsExactly("explicit");
        assertThatThrownBy(() -> m002ActAs(f.admin(), "platform_admin", () -> accessService.removeUserPermissionOverrides(f.target(),
                m002RemoveDeny("m002-j7-plain-" + f.suffix(), f.code()))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_REQUIRES_APPROVAL"));
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("explicit");
    }

    // M002-F09（CODEX-M002-3-R1-01）：真实接口回归——最高权限本人会话把自己的 access.role.assign 设为显式拒绝后，仍可经解除接口直接解除、
    // 只删显式行；其他主体经接口直接解除最高权限账号上须审批码的拒绝仍须审批。
    @Test
    void masterSessionRemovesItsOwnRoleAssignDenyThroughTheEndpoint() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        String master = SessionIssuer.MASTER_USER_ID;
        jdbcTemplate.update("""
                INSERT INTO access_users (id, username, account_type, organization_id, status, created_at)
                VALUES (?, ?, ?, 'platform-company', 'active', now())
                ON CONFLICT (id) DO UPDATE SET username = EXCLUDED.username, account_type = EXCLUDED.account_type, status = 'active'
                """, master, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE);
        var masterSession = m002Session(new AccessActor(master, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                "platform-company", null, "active"));
        mockMvc.perform(post("/api/v1/access/users/" + master + "/permission-overrides").with(masterSession)
                        .contentType(MediaType.APPLICATION_JSON).content(m002DenyJson("m002-j8-deny-" + f.suffix(), "access.role.assign")))
                .andExpect(status().isOk());
        assertThat(denyOrigins(master, "access.role.assign")).containsExactly("explicit");

        mockMvc.perform(post("/api/v1/access/users/" + master + "/permission-overrides/remove").with(masterSession)
                        .contentType(MediaType.APPLICATION_JSON).content(m002DenyJson("m002-j8-remove-" + f.suffix(), "access.role.assign")))
                .andExpect(status().isOk());
        assertThat(denyOrigins(master, "access.role.assign")).isEmpty();

        String role = "role_m002_endpoint_" + f.suffix();
        repository.createRole(new RoleRecord(role, "M002 接口角色 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of(f.code(), "access.role.assign"), null));
        repository.assignUserRole(f.admin(), role);
        repository.saveUserPermissionOverrides(master, List.of(f.code()), AccessRepository.Effect.DENY, "停权", f.admin());
        mockMvc.perform(post("/api/v1/access/users/" + master + "/permission-overrides/remove")
                        .with(m002Session(new AccessActor(f.admin(), f.admin(), "platform_admin", "org", null, "active")))
                        .contentType(MediaType.APPLICATION_JSON).content(m002DenyJson("m002-j8-admin-" + f.suffix(), f.code())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("GRANT_REQUIRES_APPROVAL"));
        assertThat(denyOrigins(master, f.code())).containsExactly("explicit");
    }

    // 第十八批 U 段第 170 条 170-R3（PGM:528「发起单据时只需指定该事项的最终审批人，系统按组织层级自动补全从发起人到该最终审批人之间的全部
    // 直接上级节点」；OpenAPI 与代码一致）：平台管理员按前端蛇形键经 POST /grant-requests 提交员工单独授予申请并带 final_approver_id——
    // 组织层级 发起人 → boss → mid → top，高危授予档位只到上两级（boss、mid），指定 top 后复核链补全到 top 为止。
    @Test
    void grantRequestFinalApproverIdEndsTheReviewChainAtTheNamedApprover() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        String mid = "usr_m002_mid_" + f.suffix();
        String top = "usr_m002_top_" + f.suffix();
        repository.createUser(new AccessUser(mid, mid, "staff", "org", null, "active", Instant.now()));
        repository.createUser(new AccessUser(top, top, "staff", "org", null, "active", Instant.now()));
        jdbcTemplate.update("INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by) VALUES (?, ?, 'main', ?)",
                f.boss(), mid, f.admin());
        jdbcTemplate.update("INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by) VALUES (?, ?, 'main', ?)",
                mid, top, f.admin());

        mockMvc.perform(post("/api/v1/access/grant-requests")
                        .with(m002Session(new AccessActor(f.admin(), f.admin(), "platform_admin", "org", null, "active")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"idempotency_key\": \"r170-grant-%s\", \"request_type\": \"user_permission_grant\", "
                                + "\"target_user_id\": \"%s\", \"permission_codes\": [\"%s\"], \"reason\": \"单独授予\", "
                                + "\"final_approver_id\": \"%s\"}").formatted(f.suffix(), f.target(), f.code(), top)))
                .andExpect(status().isOk());
        String reviewId = jdbcTemplate.queryForObject(
                "SELECT review_id FROM access_grant_requests WHERE target_user_id = ? AND request_type = 'user_permission_grant'",
                String.class, f.target());
        assertThat(jdbcTemplate.queryForList("SELECT approver_id FROM supervisor_review_nodes WHERE review_id = ? ORDER BY seq",
                String.class, reviewId)).containsExactly(f.boss(), mid, top);
    }

    // 后台 Web 终端（grace 模式下自报 web 的写请求留痕放行）+ 生产过滤器放入的 AccessActor 主体。
    private org.springframework.test.web.servlet.request.RequestPostProcessor m002Session(AccessActor actor) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null, List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF"))))
                    .postProcessRequest(request);
        };
    }

    // M002-F11（操作员 2026-09-25 裁决，治理地图「权限规则」）、M002-F13（控制席按长期规则自动批准的修复）：真实接口回归——持 access.role.assign、不持角色高危码的同组织员工
    // 经撤销接口撤销成功；同样持码的跨组织员工被 CROSS_ORG_FORBIDDEN 拒绝、角色保留。
    @Test
    void roleAssignHolderRevokesAHighRiskRoleWithoutHoldingItsCodesThroughTheEndpoint() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        String highRole = "role_m002_high_" + f.suffix();
        String assignRole = "role_m002_assign_" + f.suffix();
        repository.createRole(new RoleRecord(highRole, "M002 高危角色 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of(f.code()), null));
        repository.createRole(new RoleRecord(assignRole, "M002 装配角色 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of("access.role.assign"), null));
        String sameOrg = "usr_m002_revoker_" + f.suffix();
        String otherOrg = "usr_m002_revoker_b_" + f.suffix();
        repository.createUser(new AccessUser(sameOrg, sameOrg, "staff", "org", null, "active", Instant.now()));
        repository.createUser(new AccessUser(otherOrg, otherOrg, "staff", "org_other", null, "active", Instant.now()));
        for (String user : List.of(sameOrg, otherOrg)) {
            repository.assignUserRole(user, assignRole);
            assertThat(repository.findPermissionEffects(user, Instant.now()))
                    .extracting(AccessRepository.PermissionEffect::permissionCode).doesNotContain(f.code());
        }
        String kept = f.boss();
        repository.assignUserRole(f.target(), highRole);
        repository.assignUserRole(kept, highRole);

        mockMvc.perform(post("/api/v1/access/role-assignments/unassign")
                        .with(m002Session(new AccessActor(sameOrg, sameOrg, "staff", "org", null, "active")))
                        .contentType(MediaType.APPLICATION_JSON).content(m002UnassignJson("m002-j9-same-" + f.suffix(), f.target(), highRole)))
                .andExpect(status().isOk());
        assertThat(m002UserRoles(f.target())).doesNotContain(highRole);

        mockMvc.perform(post("/api/v1/access/role-assignments/unassign")
                        .with(m002Session(new AccessActor(otherOrg, otherOrg, "staff", "org_other", null, "active")))
                        .contentType(MediaType.APPLICATION_JSON).content(m002UnassignJson("m002-j9-cross-" + f.suffix(), kept, highRole)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_ORG_FORBIDDEN"));
        assertThat(m002UserRoles(kept)).contains(highRole);
    }

    private String m002UnassignJson(String key, String userId, String roleId) {
        return """
                {"idempotency_key": "%s", "user_id": "%s", "role_id": "%s"}
                """.formatted(key, userId, roleId);
    }

    private List<String> m002UserRoles(String userId) {
        return jdbcTemplate.queryForList("SELECT role_id FROM user_roles WHERE user_id = ?", String.class, userId);
    }

    private String m002DenyJson(String key, String code) {
        return """
                {"idempotency_key": "%s", "permission_codes": ["%s"], "effect": "deny", "reason": "M002"}
                """.formatted(key, code);
    }

    private UserPermissionOverrideRequest m002RemoveDeny(String key, String code) {
        return new UserPermissionOverrideRequest(key, "M002", List.of(code), "deny", "解除显式拒绝");
    }

    private String m002ApplyOverride(M002Fixture f, String requestType, String key) {
        m002ActAs(f.admin(), "platform_admin", () -> accessService.applyGrant(new AccessGrantApplyRequest(
                key + "-apply-" + f.suffix(), "M002", List.of(), requestType, f.target(), null, null, null, null, null, null,
                List.of(f.code()), null, null, null, "员工覆盖授权申请")));
        return m002PendingRequestId(f.target(), requestType);
    }

    private String m002PendingDenyRemoval(M002Fixture f, String key) {
        m002ActAs(f.admin(), "platform_admin", () -> accessService.setUserPermissionOverrides(f.target(), new UserPermissionOverrideRequest(
                key + "-deny-" + f.suffix(), "M002", List.of(f.code()), "deny", "员工显式拒绝")));
        return m002ApplyOverride(f, "user_permission_deny_removal", key);
    }

    private AccessGrantReviewRequest m002Approve(String key) {
        return new AccessGrantReviewRequest(key, "M002", List.of(), "approve", "同意");
    }

    // M002：真库授权申请夹具（随测试事务回滚）——自建一个 high、须审批的测试权限码（不依赖种子风险档）、平台管理员发起人、
    // 其直接上级（审批人，org_hierarchy）与目标员工；授权政策取库内 access_grant（high → 上两级）。
    private M002Fixture m002ApprovalFixture() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        M002Fixture f = new M002Fixture(suffix, "m002.override_test.high_" + suffix,
                "usr_m002_admin_" + suffix, "usr_m002_boss_" + suffix, "usr_m002_target_" + suffix);
        jdbcTemplate.update("""
                INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
                VALUES (?, 'M002 测试高危权限', 'ops', 'high', TRUE, 'active')
                """, f.code());
        repository.createUser(new AccessUser(f.admin(), f.admin(), "platform_admin", "org", null, "active", Instant.now()));
        // M002-F37：发起人按真实平台管理员装配（role_platform_admin 种子含 access.role.assign），才能发起员工单独授予、解除显式拒绝与岗位高危释放申请。
        repository.assignUserRole(f.admin(), "role_platform_admin");
        repository.createUser(new AccessUser(f.boss(), f.boss(), "staff", "org", null, "active", Instant.now()));
        repository.createUser(new AccessUser(f.target(), f.target(), "staff", "org", null, "active", Instant.now()));
        jdbcTemplate.update("INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by) VALUES (?, ?, 'main', ?)",
                f.admin(), f.boss(), f.admin());
        return f;
    }

    private static final String GRANT_REVIEW = "access.grant_request.review";

    private record M002Fixture(String suffix, String code, String admin, String boss, String target) {
    }

    // 以生产过滤器放入的 AccessActor 身份在当前线程执行；异常原样抛出。
    private void m002ActAs(String id, String accountType, Runnable action) {
        m002ActAs(id, accountType, "org", action);
    }

    private void m002ActAs(String id, String accountType, String organizationId, Runnable action) {
        m002ActAs(new AccessActor(id, id, accountType, organizationId, null, "active"), action);
    }

    // USB 最高权限会话（与 ProductionAuthService 签发的主体同一组身份字段）。
    private void m002ActAsMaster(Runnable action) {
        m002ActAs(new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                "platform-company", null, "active"), action);
    }

    private void m002ActAs(AccessActor actor, Runnable action) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                actor, null, List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF"))));
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String m002PendingRequestId(String targetUserId, String requestType) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM access_grant_requests WHERE target_user_id = ? AND request_type = ? AND status = 'pending'",
                String.class, targetUserId, requestType);
    }

    private List<String> denyOrigins(String userId, String code) {
        return jdbcTemplate.queryForList(
                "SELECT origin FROM user_permission_overrides WHERE user_id = ? AND permission_code = ? AND effect = 'deny' ORDER BY origin",
                String.class, userId, code);
    }

    // ── M002 第 7 轮（M002-F19～F23；F19、F20 为操作员 2026-09-25 裁决，F21～F23 为技术不变量）真库与真实接口回归 ─────────────────────────────────

    // M002-F19：真库＋真实接口——平台管理员经接口给最高权限账号定岗 400 MASTER_AUTHORITY_NO_POSITION；范围变更同样 400；单独调职级不带
    // 岗位派生，不拦（[6]）；不写岗位行、挂起行与申请单；最高权限会话调该须审批码照常放行。
    @Test
    void masterAuthorityAccountCannotBePositionedOnTheRealSchemaOrThroughTheEndpoint() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        String master = m002MasterAccount();
        M002Mapping m = m002HighRiskMapping(f);
        String assignRole = "role_m002_f19_" + f.suffix();
        repository.createRole(new RoleRecord(assignRole, "M002 装配角色 F19 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of("access.role.assign"), null));
        repository.assignUserRole(f.admin(), assignRole);

        mockMvc.perform(put("/api/v1/access/users/" + master + "/position")
                        .with(m002Session(new AccessActor(f.admin(), f.admin(), "platform_admin", "org", null, "active")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\": \"m002-j12-http-%s\", \"position_code\": \"%s\", \"staff_level_id\": \"lvl_m1_lower\"}"
                                .formatted(f.suffix(), m.position())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("MASTER_AUTHORITY_NO_POSITION"));
        List<Runnable> entries = List.of(
                () -> accessService.changeUserScope(master, new UserScopeChangeRequest(
                        "m002-j12-scope-" + f.suffix(), "M002", "org", null, m.position(), "lvl_m1_lower", null, null, null, "调入项目", null)));
        for (Runnable entry : entries) {
            assertThatThrownBy(() -> m002ActAs(f.admin(), "platform_admin", entry))
                    .isInstanceOfSatisfying(ApiException.class, e -> {
                        assertThat(e.status().value()).isEqualTo(400);
                        assertThat(e.code()).isEqualTo("MASTER_AUTHORITY_NO_POSITION");
                    });
        }
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM user_position_assignments WHERE user_id = ?", Long.class, master))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM user_permission_overrides WHERE user_id = ? AND origin = 'position_hold'",
                Long.class, master)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM access_grant_requests WHERE target_user_id = ?", Long.class, master))
                .isZero();
        m002ActAs(f.admin(), "platform_admin", () -> accessService.assignStaffLevel(new StaffLevelAssignmentRequest(
                "m002-j12-level-" + f.suffix(), "M002", master, "lvl_m1_lower", null, null, null)));
        assertThat(jdbcTemplate.queryForObject("SELECT staff_level_id FROM staff_level_assignments WHERE user_id = ?", String.class, master))
                .isEqualTo("lvl_m1_lower");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM user_position_assignments WHERE user_id = ?", Long.class, master))
                .isZero();
        m002ActAsMaster(() -> authorizationService.requirePermission(f.code()));
    }

    // M002-F19：真库回归——V435 清掉最高权限账号名下存量岗位（含岗位派生的模板套用）、挂起行，待审的岗位高危释放、定岗与范围变更申请
    // 置 rejected（decided_by system:V435）并关闭其复核单（回归 [11][15]）；负对照（回归 [16][19]）：最高权限的员工显式拒绝、非岗位派生的
    // 模板套用与待审角色申请，最高权限名下已决（已批准、已人工驳回）的同类申请及其复核单与节点（回归 [13]），另一名员工的岗位行、挂起行、
    // 待审释放单、待审定岗申请与复核单都不动；重放第二次不再改动。
    @Test
    void v435ClearsMasterAuthorityPositionHoldsAndPendingReleasesIdempotently() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        String master = m002MasterAccount();
        String template = "tpl_m002_v435_" + f.suffix();
        String application = "pta_m002_v435_" + f.suffix();
        String ownApplication = "pta_m002_v435_own_" + f.suffix();
        String peerApplication = "pta_m002_v435_peer_" + f.suffix();
        String release = "agr_m002_v435_" + f.suffix();
        String peerRelease = "agr_m002_v435_peer_" + f.suffix();
        String positionRequest = "agr_m002_v435_pos_" + f.suffix();
        String scopeRequest = "agr_m002_v435_scope_" + f.suffix();
        String masterRoleRequest = "agr_m002_v435_role_" + f.suffix();
        String peerPositionRequest = "agr_m002_v435_peerpos_" + f.suffix();
        repository.createTemplate(baseTemplate(template), List.of());
        for (String[] row : List.of(new String[] {application, master}, new String[] {ownApplication, master},
                new String[] {peerApplication, f.target()})) {
            jdbcTemplate.update("""
                    INSERT INTO permission_template_applications (id, template_id, target_type, target_id, status, applied_by, applied_at)
                    VALUES (?, ?, 'user', ?, 'active', ?, now())
                    """, row[0], template, row[1], f.admin());
        }
        for (String[] row : List.of(new String[] {master, application}, new String[] {f.target(), peerApplication})) {
            jdbcTemplate.update("""
                    INSERT INTO user_position_assignments (user_id, position_code, staff_level_id, organization_id, assigned_by, assigned_at,
                                                           position_application_ids)
                    VALUES (?, ?, 'lvl_m1_lower', 'platform-company', ?, now(), ARRAY[?]::varchar[])
                    ON CONFLICT (user_id) DO UPDATE SET position_application_ids = EXCLUDED.position_application_ids
                    """, row[0], "m002_v435_" + f.suffix(), f.admin(), row[1]);
        }
        for (String user : List.of(master, f.target())) {
            repository.saveHighRiskHolds(user, List.of(f.code()), "岗位高危挂起", f.admin());
        }
        repository.saveUserPermissionOverrides(master, List.of(f.code()), AccessRepository.Effect.DENY, "员工显式拒绝", f.admin());
        for (String[] row : List.of(new String[] {release, master, "position_high_risk_release"},
                new String[] {peerRelease, f.target(), "position_high_risk_release"}, new String[] {positionRequest, master, "user_position"},
                new String[] {scopeRequest, master, "user_scope_change"}, new String[] {masterRoleRequest, master, "user_role"},
                new String[] {peerPositionRequest, f.target(), "user_position"})) {
            jdbcTemplate.update("""
                    INSERT INTO supervisor_review_decisions (review_id, policy_code, action_type, object_type, object_id, blocked, review_status,
                                                             required_level, evaluated_at)
                    VALUES (?, 'permission_grant_l2', 'access.grant_request', 'access_grant_request', ?, TRUE, 'pending_review', 2, now())
                    """, "rv_" + row[0], row[0]);
            jdbcTemplate.update("""
                    INSERT INTO supervisor_review_nodes (node_id, review_id, seq, approver_kind, approver_id, status)
                    VALUES (?, ?, 1, 'user', ?, 'pending')
                    """, "nd_" + row[0], "rv_" + row[0], f.boss());
            jdbcTemplate.update("""
                    INSERT INTO access_grant_requests (id, request_type, target_user_id, permission_codes, status, review_id, reason, requested_by,
                                                       created_at, updated_at)
                    VALUES (?, ?, ?, ?, 'pending', ?, 'M002 存量待审单', ?, now(), now())
                    """, row[0], row[2], row[1], f.code(), "rv_" + row[0], f.admin());
        }

        // 最高权限名下已决的同类申请：决定时间、决定人固定，重放后逐列不变
        String decided = "2026-09-01 00:00:00+00";
        for (String[] row : List.of(new String[] {"agr_m002_v435_done_pos_" + f.suffix(), "user_position", "applied", "approved"},
                new String[] {"agr_m002_v435_done_rel_" + f.suffix(), "position_high_risk_release", "rejected", "rejected"},
                new String[] {"agr_m002_v435_done_scope_" + f.suffix(), "user_scope_change", "applied", "approved"})) {
            jdbcTemplate.update("""
                    INSERT INTO supervisor_review_decisions (review_id, policy_code, action_type, object_type, object_id, blocked, review_status,
                                                             decision, required_level, evaluated_at)
                    VALUES (?, 'permission_grant_l2', 'access.grant_request', 'access_grant_request', ?, TRUE, ?, ?, 2, CAST(? AS timestamptz))
                    """, "rv_" + row[0], row[0], row[3], row[3], decided);
            jdbcTemplate.update("""
                    INSERT INTO supervisor_review_nodes (node_id, review_id, seq, approver_kind, approver_id, status, decided_by, decided_at)
                    VALUES (?, ?, 1, 'user', ?, ?, ?, CAST(? AS timestamptz))
                    """, "nd_" + row[0], "rv_" + row[0], f.boss(), row[3], f.boss(), decided);
            jdbcTemplate.update("""
                    INSERT INTO access_grant_requests (id, request_type, target_user_id, permission_codes, status, review_id, reason, requested_by,
                                                       decided_by, decided_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, 'M002 已决申请', ?, ?, CAST(? AS timestamptz), CAST(? AS timestamptz), CAST(? AS timestamptz))
                    """, row[0], row[1], master, f.code(), row[2], "rv_" + row[0], f.admin(), f.boss(), decided, decided, decided);
        }
        String decidedFacts = """
                SELECT string_agg(r.id || '|' || r.status || '|' || r.decided_by || '|' || (r.decided_at = CAST(? AS timestamptz))
                                  || '|' || (r.updated_at = CAST(? AS timestamptz)) || '|' || d.review_status || '|' || (d.evaluated_at = CAST(? AS timestamptz))
                                  || '|' || n.status || '|' || n.decided_by || '|' || (n.decided_at = CAST(? AS timestamptz)), ',' ORDER BY r.id)
                  FROM access_grant_requests r
                  JOIN supervisor_review_decisions d ON d.review_id = r.review_id
                  JOIN supervisor_review_nodes n ON n.review_id = r.review_id
                 WHERE r.target_user_id = ? AND r.id LIKE 'agr_m002_v435_done_%'
                """;
        String decidedBefore = jdbcTemplate.queryForObject(decidedFacts, String.class, decided, decided, decided, decided, master);
        assertThat(decidedBefore).contains("|applied|", "|rejected|").doesNotContain("|false");

        for (int replay = 0; replay < 2; replay++) {
            m002ReplayMigration("V435__m002_master_authority_no_position.sql");
            assertThat(jdbcTemplate.queryForObject(decidedFacts, String.class, decided, decided, decided, decided, master))
                    .isEqualTo(decidedBefore);
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM user_position_assignments WHERE user_id = ?", Long.class, master))
                    .isZero();
            assertThat(denyOrigins(master, f.code())).containsExactly("explicit");
            for (String closed : List.of(release, positionRequest, scopeRequest)) {
                assertThat(jdbcTemplate.queryForObject("SELECT status || '/' || decided_by FROM access_grant_requests WHERE id = ?",
                        String.class, closed)).as(closed).isEqualTo("rejected/system:V435");
                assertThat(m002ReviewStatus("rv_" + closed)).as(closed).isEqualTo("rejected");
                assertThat(jdbcTemplate.queryForObject("SELECT status || '/' || decided_by FROM supervisor_review_nodes WHERE review_id = ?",
                        String.class, "rv_" + closed)).as(closed).isEqualTo("cancelled/system:V435");
            }
            assertThat(m002ApplicationStatus(application)).isEqualTo("revoked");
            // 负对照：不是最高权限岗位派生的一律不动
            assertThat(m002ApplicationStatus(ownApplication)).isEqualTo("active");
            assertThat(m002ApplicationStatus(peerApplication)).isEqualTo("active");
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM user_position_assignments WHERE user_id = ?", Long.class, f.target()))
                    .isEqualTo(1);
            assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
            for (String untouched : List.of(peerRelease, masterRoleRequest, peerPositionRequest)) {
                assertThat(m002Status(untouched)).as(untouched).isEqualTo("pending");
                assertThat(m002ReviewStatus("rv_" + untouched)).as(untouched).isEqualTo("pending_review");
                assertThat(m002PendingNodeCount("rv_" + untouched)).as(untouched).isEqualTo(1);
            }
        }
    }

    // M002-F20：真库＋真实接口回归——待审期间同岗重定保留原单（单号不变、总数仍为 1，回归 [18]）；驳回后挂起行保留、有效集没有该码、
    // 直接解除 400、同岗重定不自动重开单；经接口对同一挂起提交「岗位高危权限释放」申请即再次发起（快照钉当前映射），批准后挂起删除、码生效；
    // 此后同岗重定该码保持生效、不开新单（回归 [1]）。
    @Test
    void rejectedReleaseIsReappliedForTheSameHoldOnTheRealSchemaAndThroughTheEndpoint() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        M002Mapping m = m002HighRiskMapping(f);
        m002Assign(f, m.position(), "m002-j10-assign-a");
        String first = m002PendingRequestId(f.target(), "position_high_risk_release");
        m002Assign(f, m.position(), "m002-j10-assign-b");
        assertThat(m002PendingRequestId(f.target(), "position_high_risk_release")).isEqualTo(first);
        assertThat(m002ReleaseCount(f.target())).isEqualTo(1);

        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(first, m002Reject("m002-j10-reject-" + f.suffix())));
        assertThat(m002Status(first)).isEqualTo("rejected");
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
        assertThat(m002EffectiveGrants(f)).doesNotContain(f.code());
        assertThatThrownBy(() -> m002ActAs(f.admin(), "platform_admin", () -> accessService.removeUserPermissionOverrides(f.target(),
                m002RemoveDeny("m002-j10-remove-" + f.suffix(), f.code()))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        m002Assign(f, m.position(), "m002-j10-reassign");
        assertThat(m002PendingReleaseCount(f.target())).isZero();
        assertThat(m002ReleaseCount(f.target())).isEqualTo(1);
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");

        mockMvc.perform(post("/api/v1/access/grant-requests")
                        .with(m002Session(new AccessActor(f.admin(), f.admin(), "platform_admin", "org", null, "active")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"idempotency_key\": \"m002-j10-reapply-%s\", \"request_type\": \"position_high_risk_release\", "
                                + "\"target_user_id\": \"%s\", \"permission_codes\": [\"%s\"], \"reason\": \"驳回后再次发起\"}")
                                .formatted(f.suffix(), f.target(), f.code())))
                .andExpect(status().isOk());
        String second = m002PendingRequestId(f.target(), "position_high_risk_release");
        assertThat(second).isNotEqualTo(first);
        AccessGrantRequestRecord reapplied = repository.findGrantRequestById(second).orElseThrow();
        assertThat(List.of(reapplied.positionCode(), reapplied.staffLevelId(), reapplied.targetRoleId(), reapplied.templateId()))
                .containsExactly(m.position(), "lvl_m1_lower", m.role(), m.template());
        assertThat(reapplied.permissionCodes()).containsExactly(f.code());

        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(second, m002Approve("m002-j10-approve-" + f.suffix())));
        assertThat(m002Status(second)).isEqualTo("applied");
        assertThat(denyOrigins(f.target(), f.code())).isEmpty();
        assertThat(m002EffectiveGrants(f)).contains(f.code());
        m002Assign(f, m.position(), "m002-j10-after-approval");
        assertThat(m002PendingReleaseCount(f.target())).isZero();
        assertThat(m002ReleaseCount(f.target())).isEqualTo(2);
        assertThat(denyOrigins(f.target(), f.code())).isEmpty();
        assertThat(m002EffectiveGrants(f)).contains(f.code());
    }

    // M002-F20：真库回归——驳回后跨项目范围变更，已驳回单留下的挂起行作废；新范围审批落地后无挂起行、该码生效。
    @Test
    void scopeChangeVoidsTheHoldLeftByARejectedReleaseOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        M002Mapping m = m002HighRiskMapping(f);
        m002Assign(f, m.position(), "m002-j15-assign");
        String hold = m002PendingRequestId(f.target(), "position_high_risk_release");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(hold, m002Reject("m002-j15-reject-" + f.suffix())));

        m002ActAs(f.admin(), "platform_admin", () -> accessService.changeUserScope(f.target(), new UserScopeChangeRequest(
                "m002-j15-scope-" + f.suffix(), "M002", "org_m002_other", null, null, null, null, null, null, "调往另一项目", null)));
        assertThat(denyOrigins(f.target(), f.code())).isEmpty();
        String scope = m002PendingRequestId(f.target(), "user_scope_change");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(scope, m002Approve("m002-j15-approve-" + f.suffix())));
        assertThat(m002Status(scope)).isEqualTo("applied");
        assertThat(denyOrigins(f.target(), f.code())).isEmpty();
        assertThat(m002EffectiveGrants(f)).contains(f.code());
    }

    // M002-F22：真库回归——停用岗位模板收回派生、作废挂起行与待审释放单并关闭其复核单（回归 [22]）；再启用恰开一张待审单；
    // 存档、取消存档按基线不重算（回归修正）：派生、挂起行、待审单与复核单原样保留。
    @Test
    void deactivatingThePositionTemplateVoidsHoldsAndClosesReviewsWhileArchivingLeavesThemOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        M002Mapping m = m002HighRiskMapping(f);
        m002Assign(f, m.position(), "m002-j11-assign");
        String first = m002PendingRequestId(f.target(), "position_high_risk_release");
        String firstReview = m002ReviewId(first);

        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePositionRoleTemplate(m.id(),
                m002MappingRequest("m002-j11-off-" + f.suffix(), m, "inactive")));
        assertThat(m002Status(first)).isEqualTo("rejected");
        assertThat(m002ReviewStatus(firstReview)).isEqualTo("rejected");
        assertThat(m002PendingNodeCount(firstReview)).isZero();
        assertThat(m002PendingReleaseCount(f.target())).isZero();
        assertThat(denyOrigins(f.target(), f.code())).isEmpty();
        assertThat(m002AssignedRole(f.target())).isNull();

        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePositionRoleTemplate(m.id(),
                m002MappingRequest("m002-j11-on-" + f.suffix(), m, "active")));
        assertThat(m002PendingReleaseCount(f.target())).isEqualTo(1);
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
        assertThat(m002AssignedRole(f.target())).isEqualTo(m.role());
        String second = m002PendingRequestId(f.target(), "position_high_risk_release");
        String secondReview = m002ReviewId(second);

        for (String action : List.of("archive", "restore")) {
            m002ActAs(f.admin(), "platform_admin", () -> accessService.changePositionTemplateLifecycle(m.id(),
                    new StatusChangeRequest("m002-j11-" + action + "-" + f.suffix(), action, "停止或恢复使用", List.of())));
            assertThat(m002PendingRequestId(f.target(), "position_high_risk_release")).isEqualTo(second);
            assertThat(m002ReviewStatus(secondReview)).isEqualTo("pending_review");
            assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
            assertThat(m002AssignedRole(f.target())).isEqualTo(m.role());
            assertThat(m002AssignedMapping(f.target())).isEqualTo(m.id());
        }
    }

    // M002-F22（回归 [0]，真库验证定向查询口径）：同岗同职级的旧映射停用、新映射启用后员工重算挂到新映射；再保存或存档旧映射，
    // 员工的派生、挂起行、待审释放单与复核单都不动（停用分支按 assigned_position_template_id 取人）。
    @Test
    void savingOrArchivingAnInactiveOldMappingLeavesEmployeesOnTheReplacementOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        M002Mapping m = m002HighRiskMapping(f);
        m002Assign(f, m.position(), "m002-j16-assign");
        String first = m002PendingRequestId(f.target(), "position_high_risk_release");
        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePositionRoleTemplate(m.id(),
                m002MappingRequest("m002-j16-off-" + f.suffix(), m, "inactive")));
        assertThat(m002Status(first)).isEqualTo("rejected");
        PositionRoleTemplate[] replacement = new PositionRoleTemplate[1];
        m002ActAs(f.admin(), "platform_admin", () -> replacement[0] = accessService.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "m002-j16-new-" + f.suffix(), "M002", m.position(), "lvl_m1_lower", m.role(), m.template(), "active")));
        assertThat(m002AssignedMapping(f.target())).isEqualTo(replacement[0].id());
        String current = m002PendingRequestId(f.target(), "position_high_risk_release");
        String currentReview = m002ReviewId(current);

        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePositionRoleTemplate(m.id(),
                m002MappingRequest("m002-j16-edit-" + f.suffix(), m, "inactive")));
        m002ActAs(f.admin(), "platform_admin", () -> accessService.changePositionTemplateLifecycle(m.id(),
                new StatusChangeRequest("m002-j16-archive-" + f.suffix(), "archive", "删除旧模板前存档", List.of())));
        assertThat(m002PendingRequestId(f.target(), "position_high_risk_release")).isEqualTo(current);
        assertThat(m002ReviewStatus(currentReview)).isEqualTo("pending_review");
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
        assertThat(m002AssignedRole(f.target())).isEqualTo(m.role());
        assertThat(m002AssignedMapping(f.target())).isEqualTo(replacement[0].id());
    }

    // M002-F20/F22（「该岗位模板被…修改」，回归 [6][10]，真库）：修改岗位映射所引权限模板的内容——原待审释放单作废、复核单关闭，
    // 删掉的高危码挂起行删除，仍在模板里的高危码重新挂起并恰开一张新单。
    @Test
    void editingThePositionsPermissionTemplateVoidsAndRejudgesHoldsOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        String second = "m002.override_test.high2_" + f.suffix();
        jdbcTemplate.update("""
                INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
                VALUES (?, 'M002 测试高危权限二', 'ops', 'high', TRUE, 'active')
                """, second);
        M002Mapping m = new M002Mapping("prt_m002_t_" + f.suffix(), "m002_tpos_" + f.suffix(), "role_m002_t_" + f.suffix(),
                "tpl_m002_t_" + f.suffix());
        repository.createRole(new RoleRecord(m.role(), "M002 模板岗位角色 " + f.suffix(), null, "project", "active", Instant.now(), List.of(), null));
        repository.createTemplate(baseTemplate(m.template()), grants(m.template(), f.code(), second));
        repository.savePositionRoleTemplate(new PositionRoleTemplate(m.id(), m.position(), "lvl_m1_lower", m.role(), m.template(),
                null, "active", f.admin(), Instant.now(), Instant.now()));
        m002Assign(f, m.position(), "m002-j17-assign");
        String old = m002PendingRequestId(f.target(), "position_high_risk_release");
        String oldReview = m002ReviewId(old);

        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePermissionTemplate(m.template(), new PermissionTemplateRequest(
                "m002-j17-edit-" + f.suffix(), "M002", List.of(), m.template(), "base", null, List.of(f.code()), "project", null, null, false)));
        assertThat(m002Status(old)).isEqualTo("rejected");
        assertThat(m002ReviewStatus(oldReview)).isEqualTo("rejected");
        assertThat(denyOrigins(f.target(), second)).isEmpty();
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
        String renewed = m002PendingRequestId(f.target(), "position_high_risk_release");
        assertThat(repository.findGrantRequestById(renewed).orElseThrow().permissionCodes()).containsExactly(f.code());
    }

    // M002-F20（治理地图第 703 行「……原先未生效的这类权限及其待审批申请一并作废」，只作废未生效的；真库）：已批准生效的码按（员工、映射、码）判定——所引权限模板内容被修改、映射改角色后仍生效，
    // 映射新增的高危码挂起并恰开一张单；改职级换到另一张映射，原已批准的码按新岗位重新挂起、另开新单。
    @Test
    void approvedHighRiskCodeStaysEffectiveOnTheSameMappingAndIsRejudgedOnAnotherMappingOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        String second = "m002.override_test.high2_" + f.suffix();
        String daily = "m002.override_test.daily_" + f.suffix();
        jdbcTemplate.update("""
                INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
                VALUES (?, 'M002 测试高危权限二', 'ops', 'high', TRUE, 'active'), (?, 'M002 测试日常权限', 'ops', 'low', FALSE, 'active')
                """, second, daily);
        String dailyHolder = "role_m002_daily_" + f.suffix();
        repository.createRole(new RoleRecord(dailyHolder, "M002 日常码持有 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of(daily), null));
        repository.assignUserRole(f.admin(), dailyHolder); // 授权人只能授出自己持有的日常码
        M002Mapping m = m002HighRiskMapping(f);
        String secondRole = "role_m002_g2_" + f.suffix();
        repository.createRole(new RoleRecord(secondRole, "M002 岗位角色二 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of(f.code(), second), null));
        m002Assign(f, m.position(), "m002-j22-assign");
        String first = m002PendingRequestId(f.target(), "position_high_risk_release");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(first, m002Approve("m002-j22-approve-" + f.suffix())));
        assertThat(m002EffectiveGrants(f)).contains(f.code());

        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePermissionTemplate(m.template(), new PermissionTemplateRequest(
                "m002-j22-edit-" + f.suffix(), "M002", List.of(), m.template(), "base", null, List.of(daily), "project", null, null, false)));
        assertThat(denyOrigins(f.target(), f.code())).isEmpty();
        assertThat(m002ReleaseCount(f.target())).isEqualTo(1);
        assertThat(m002EffectiveGrants(f)).contains(f.code(), daily);

        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePositionRoleTemplate(m.id(), m002MappingRequest(
                "m002-j22-remap-" + f.suffix(), new M002Mapping(m.id(), m.position(), secondRole, m.template()), "active")));
        assertThat(denyOrigins(f.target(), f.code())).isEmpty();
        assertThat(denyOrigins(f.target(), second)).containsExactly("position_hold");
        String added = m002PendingRequestId(f.target(), "position_high_risk_release");
        assertThat(repository.findGrantRequestById(added).orElseThrow().permissionCodes()).containsExactly(second);
        assertThat(m002EffectiveGrants(f)).contains(f.code()).doesNotContain(second);

        repository.savePositionRoleTemplate(new PositionRoleTemplate("prt_m002_g10_" + f.suffix(), m.position(), "lvl_m10_upper", m.role(),
                m.template(), null, "active", f.admin(), Instant.now(), Instant.now()));
        m002ActAs(f.admin(), "platform_admin", () -> accessService.assignStaffLevel(new StaffLevelAssignmentRequest(
                "m002-j22-level-" + f.suffix(), "M002", f.target(), "lvl_m10_upper", null, null, null)));
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
        assertThat(m002Status(added)).isEqualTo("rejected");
        AccessGrantRequestRecord moved = repository.findGrantRequestById(
                m002PendingRequestId(f.target(), "position_high_risk_release")).orElseThrow();
        assertThat(moved.staffLevelId()).isEqualTo("lvl_m10_upper");
        assertThat(moved.permissionCodes()).containsExactly(f.code());
        assertThat(m002EffectiveGrants(f)).doesNotContain(f.code());
    }

    // M002-F20（同上第 703 行原句，同岗同职级两张映射）：改存全平台映射时，改存前集合只用于挂在这张映射上的员工；派生到同岗同职级本组织映射的员工
    // 按其自己的映射判定，已批准生效的码不重新挂起、不开新单。
    @Test
    void savingTheGlobalMappingKeepsApprovedCodesOfEmployeesOnTheOrganizationMappingOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        String position = "m002_twopos_" + f.suffix();
        String plainRole = "role_m002_tp_" + f.suffix();
        String highRole = "role_m002_th_" + f.suffix();
        String template = "tpl_m002_tt_" + f.suffix();
        repository.createRole(new RoleRecord(plainRole, "M002 全平台普通角色 " + f.suffix(), null, "project", "active", Instant.now(), List.of(), null));
        repository.createRole(new RoleRecord(highRole, "M002 本组织高危角色 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of(f.code()), null));
        repository.createTemplate(baseTemplate(template), List.of());
        M002Mapping global = new M002Mapping("prt_m002_tg_" + f.suffix(), position, plainRole, template);
        String organizationMapping = "prt_m002_to_" + f.suffix();
        repository.savePositionRoleTemplate(new PositionRoleTemplate(global.id(), position, "lvl_m1_lower", plainRole, template,
                null, "active", f.admin(), Instant.now(), Instant.now()));
        repository.savePositionRoleTemplate(new PositionRoleTemplate(organizationMapping, position, "lvl_m1_lower", highRole, template,
                "org", "active", f.admin(), Instant.now(), Instant.now()));
        m002Assign(f, position, "m002-j24-assign");
        assertThat(m002AssignedMapping(f.target())).isEqualTo(organizationMapping);
        String first = m002PendingRequestId(f.target(), "position_high_risk_release");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(first, m002Approve("m002-j24-approve-" + f.suffix())));
        assertThat(m002EffectiveGrants(f)).contains(f.code());

        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePositionRoleTemplate(global.id(),
                m002MappingRequest("m002-j24-resave-global-" + f.suffix(), global, "active")));
        assertThat(m002AssignedMapping(f.target())).isEqualTo(organizationMapping);
        assertThat(denyOrigins(f.target(), f.code())).isEmpty();
        assertThat(m002ReleaseCount(f.target())).isEqualTo(1);
        assertThat(m002EffectiveGrants(f)).contains(f.code());
    }

    // M002-F22（停用只处理实际挂在该映射上的员工；仓库 AGENTS.md 第 8 条「不为存量数据做兼容、退化或回填设计」）：停用映射只按
    // assigned_position_template_id 取人、收回派生；映射列为空、仍有派生角色的同岗同职级存量行不做兜底，也不回填。
    @Test
    void deactivatingAMappingTakesOnlyEmployeesLinkedByMappingIdOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        M002Mapping m = m002HighRiskMapping(f);
        m002Assign(f, m.position(), "m002-j23-assign");
        assertThat(m002AssignedMapping(f.target())).isEqualTo(m.id());
        String unlinked = "usr_m002_unlinked_" + f.suffix();
        m002LegacyPositionRow(f, new String[] {unlinked, m.position(), "org", m.role(), null});

        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePositionRoleTemplate(m.id(),
                m002MappingRequest("m002-j23-off-" + f.suffix(), m, "inactive")));
        assertThat(m002AssignedRole(f.target())).isNull();
        assertThat(m002AssignedMapping(f.target())).isNull();
        assertThat(m002AssignedRole(unlinked)).isEqualTo(m.role());
        assertThat(m002AssignedMapping(unlinked)).isNull();
    }

    // M002-F20（[0][4][8]，真库）：定岗时已有员工显式拒绝的高危码不挂起、不开单；此后经「解除员工显式拒绝」获批而生效，改所引模板内容、
    // 映射原样重存、同岗重定后都保持生效，不挂起、不开单，并记 already_approved。
    @Test
    void highRiskCodeLiftedByAnApprovedDenyRemovalStaysEffectiveOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        String daily = "m002.override_test.daily_" + f.suffix();
        jdbcTemplate.update("""
                INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
                VALUES (?, 'M002 测试日常权限', 'ops', 'low', FALSE, 'active')
                """, daily);
        String dailyHolder = "role_m002_daily_" + f.suffix();
        repository.createRole(new RoleRecord(dailyHolder, "M002 日常码持有 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of(daily), null));
        repository.assignUserRole(f.admin(), dailyHolder); // 授权人只能授出自己持有的日常码
        M002Mapping m = m002HighRiskMapping(f);
        m002ActAs(f.admin(), "platform_admin", () -> accessService.setUserPermissionOverrides(f.target(), new UserPermissionOverrideRequest(
                "m002-j25-deny-" + f.suffix(), "M002", List.of(f.code()), "deny", "员工显式拒绝")));
        m002Assign(f, m.position(), "m002-j25-assign");
        assertThat(m002ReleaseCount(f.target())).isZero();
        String lift = m002ApplyOverride(f, "user_permission_deny_removal", "m002-j25");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(lift, m002Approve("m002-j25-approve-" + f.suffix())));
        assertThat(m002EffectiveGrants(f)).contains(f.code());

        List<Runnable> paths = List.of(
                () -> accessService.updatePermissionTemplate(m.template(), new PermissionTemplateRequest(
                        "m002-j25-edit-" + f.suffix(), "M002", List.of(), m.template(), "base", null, List.of(daily), "project", null, null, false)),
                () -> accessService.updatePositionRoleTemplate(m.id(), m002MappingRequest("m002-j25-resave-" + f.suffix(), m, "active")),
                () -> accessService.assignUserPosition(new UserPositionAssignmentRequest(
                        "m002-j25-reassign-" + f.suffix(), "M002", f.target(), m.position(), "lvl_m1_lower", null, null, null)));
        for (Runnable path : paths) {
            m002ActAs(f.admin(), "platform_admin", path);
            assertThat(denyOrigins(f.target(), f.code())).isEmpty();
            assertThat(m002ReleaseCount(f.target())).isZero();
            assertThat(m002EffectiveGrants(f)).contains(f.code());
        }
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM audit_events WHERE event_name = 'access.user_position.high_risk_already_approved' AND business_id = ?
                """, Long.class, f.target())).isEqualTo(3);
    }

    // M002-F22（[3][11]，真库）：映射改职级后，原来挂在这张映射上的员工（岗位行仍是旧职级）找不到映射——收回岗位派生、清掉映射 id，
    // 待审释放单作废、复核单关闭、挂起行删除。
    @Test
    void changingTheMappingsStaffLevelRejudgesEmployeesLeftOnItOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        M002Mapping m = m002HighRiskMapping(f);
        m002Assign(f, m.position(), "m002-j26-assign");
        String first = m002PendingRequestId(f.target(), "position_high_risk_release");
        String review = m002ReviewId(first);

        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePositionRoleTemplate(m.id(), new PositionRoleTemplateRequest(
                "m002-j26-move-" + f.suffix(), "M002", m.position(), "lvl_m10_upper", m.role(), m.template(), "active")));
        assertThat(m002Status(first)).isEqualTo("rejected");
        assertThat(m002ReviewStatus(review)).isEqualTo("rejected");
        assertThat(m002PendingNodeCount(review)).isZero();
        assertThat(denyOrigins(f.target(), f.code())).isEmpty();
        assertThat(m002AssignedRole(f.target())).isNull();
        assertThat(m002AssignedMapping(f.target())).isNull();
        assertThat(m002EffectiveGrants(f)).doesNotContain(f.code());
    }

    // M002-F20（[10]，真库并发）：员工换岗的事务持员工行未提交时，改其原映射所引模板的编辑在员工行锁上等待（锁序：模板表意向锁 → 员工行
    // → 模板行）；换岗提交后编辑按映射 id 重新取人，员工已不在原映射，跳过对账——新岗位的挂起与待审释放单原样保留，该码不因原模板
    // 去掉该码而免审生效。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void templateEditWaitsForAConcurrentPositionChangeAndLeavesTheNewHoldAlone() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        String oldPosition = "m002_rpos1_" + f.suffix();
        String newPosition = "m002_rpos2_" + f.suffix();
        List<String> roles = List.of("role_m002_r1_" + f.suffix(), "role_m002_r2_" + f.suffix());
        List<String> templates = List.of("tpl_m002_r1_" + f.suffix(), "tpl_m002_r2_" + f.suffix());
        List<String> mappings = List.of("prt_m002_r1_" + f.suffix(), "prt_m002_r2_" + f.suffix());
        ExecutorService editor = Executors.newSingleThreadExecutor();
        try {
            repository.createRole(new RoleRecord(roles.get(0), "M002 并发旧角色 " + f.suffix(), null, "project", "active", Instant.now(),
                    List.of(), null));
            repository.createRole(new RoleRecord(roles.get(1), "M002 并发新角色 " + f.suffix(), null, "project", "active", Instant.now(),
                    List.of(f.code()), null));
            repository.createTemplate(baseTemplate(templates.get(0)), grants(templates.get(0), f.code(), DAILY_A));
            repository.createTemplate(baseTemplate(templates.get(1)), List.of());
            repository.savePositionRoleTemplate(new PositionRoleTemplate(mappings.get(0), oldPosition, "lvl_m1_lower", roles.get(0),
                    templates.get(0), null, "active", f.admin(), Instant.now(), Instant.now()));
            repository.savePositionRoleTemplate(new PositionRoleTemplate(mappings.get(1), newPosition, "lvl_m1_lower", roles.get(1),
                    templates.get(1), null, "active", f.admin(), Instant.now(), Instant.now()));
            m002Assign(f, oldPosition, "m002-j27-assign");
            String oldRelease = m002PendingRequestId(f.target(), "position_high_risk_release");

            Future<Throwable> edit = new TransactionTemplate(transactionManager).execute(status -> {
                m002Assign(f, newPosition, "m002-j27-move");
                Future<Throwable> pending = editor.submit(() -> {
                    try {
                        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePermissionTemplate(templates.get(0),
                                templateRequest("m002-j27-edit-" + f.suffix(), DAILY_A)));
                        return null;
                    } catch (Throwable e) {
                        return e;
                    }
                });
                awaitWaiterOrDone(pending);
                assertThat(pending.isDone()).as("编辑应在员工行锁上等换岗提交").isFalse();
                return pending;
            });
            assertThat(edit.get(30, TimeUnit.SECONDS)).isNull();
            assertThat(repository.findTemplateItems(templates.get(0))).extracting(PermissionTemplateItem::permissionCode).containsExactly(DAILY_A);
            assertThat(m002AssignedMapping(f.target())).isEqualTo(mappings.get(1));
            assertThat(m002Status(oldRelease)).isEqualTo("rejected");
            AccessGrantRequestRecord moved = repository.findGrantRequestById(
                    m002PendingRequestId(f.target(), "position_high_risk_release")).orElseThrow();
            assertThat(moved.positionCode()).isEqualTo(newPosition);
            assertThat(moved.permissionCodes()).containsExactly(f.code());
            assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
            assertThat(m002EffectiveGrants(f)).doesNotContain(f.code());
        } finally {
            editor.shutdownNow();
            m002DeleteCommitted(f, mappings, roles, templates);
        }
    }

    // M002-F25（治理地图第 703 行原句「该岗位模板被停用或修改时，原先未生效的这类权限及其待审批申请一并作废，按新岗位或新模板重新判断」；
    // 真库）：全平台映射 A 与组织专属映射 B 同岗同职级、同高危角色与模板，员工先挂 B 并有待审释放单；把 B 改到另一岗位编码后员工按原岗位
    // 回落到 A——B 的待审单作废、复核单关闭、不能再获批；新挂起只对应一张 A 的待审单（同岗重定不重复开单），批准它之后该码才生效。
    @Test
    void releaseRequestIsBoundToItsMappingWhenTheEmployeeFallsBackToTheGlobalMappingOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        M002Mapping global = m002HighRiskMapping(f);
        String orgMapping = "prt_m002_orgb_" + f.suffix();
        repository.savePositionRoleTemplate(new PositionRoleTemplate(orgMapping, global.position(), "lvl_m1_lower", global.role(),
                global.template(), "org", "active", f.admin(), Instant.now(), Instant.now()));
        m002Assign(f, global.position(), "m002-j28-assign");
        assertThat(m002AssignedMapping(f.target())).isEqualTo(orgMapping);
        String old = m002PendingRequestId(f.target(), "position_high_risk_release");
        String oldReview = m002ReviewId(old);

        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePositionRoleTemplate(orgMapping, new PositionRoleTemplateRequest(
                "m002-j28-move-" + f.suffix(), "M002", "m002_moved_" + f.suffix(), "lvl_m1_lower", global.role(), global.template(), "active")));
        assertThat(m002AssignedMapping(f.target())).isEqualTo(global.id());
        assertThat(m002Status(old)).isEqualTo("rejected");
        assertThat(m002ReviewStatus(oldReview)).isEqualTo("rejected");
        assertThat(m002PendingNodeCount(oldReview)).isZero();
        assertThatThrownBy(() -> m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(old, m002Approve("m002-j28-old-" + f.suffix()))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ACCESS_GRANT_REQUEST_NOT_FOUND"));
        assertThat(m002PendingReleaseCount(f.target())).isEqualTo(1);
        String renewed = m002PendingRequestId(f.target(), "position_high_risk_release");
        assertThat(renewed).isNotEqualTo(old);
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
        assertThat(m002EffectiveGrants(f)).doesNotContain(f.code());

        m002Assign(f, global.position(), "m002-j28-reassign");
        assertThat(m002PendingRequestId(f.target(), "position_high_risk_release")).isEqualTo(renewed);
        assertThat(m002PendingReleaseCount(f.target())).isEqualTo(1);

        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(renewed, m002Approve("m002-j28-new-" + f.suffix())));
        assertThat(m002Status(renewed)).isEqualTo("applied");
        assertThat(denyOrigins(f.target(), f.code())).isEmpty();
        assertThat(m002EffectiveGrants(f)).contains(f.code());
    }

    // M002-F32（依据：治理地图第 656 行「临时授权可设有效期或单次使用」、第 660 行「以下五类权限授出前须先审批才生效」；真库链路）：
    // 岗位映射引用只含一个须审批高危码的单次模板——首次审批 → 使用一次 → 同岗重定、映射原样重存 → 再次操作仍被拒：原岗位派生套用及其消耗状态保留
    // （同一条套用、consumed 仍为真），旧释放单审批留痕不变、不开新单；映射改指另一单次模板即新的授出，该码重新挂起、须新审批后才能再用一次。
    @Test
    void consumedSingleUsePositionGrantStaysConsumedAcrossSamePositionReassignmentOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        String position = "m002_spos_" + f.suffix();
        String role = "role_m002_s_" + f.suffix();
        String firstTemplate = "tpl_m002_s1_" + f.suffix();
        String secondTemplate = "tpl_m002_s2_" + f.suffix();
        String mapping = "prt_m002_s_" + f.suffix();
        repository.createRole(new RoleRecord(role, "M002 单次岗位角色 " + f.suffix(), null, "project", "active", Instant.now(), List.of(), null));
        repository.createTemplate(m002SingleUseTemplate(firstTemplate), grants(firstTemplate, f.code()));
        repository.createTemplate(m002SingleUseTemplate(secondTemplate), grants(secondTemplate, f.code()));
        repository.savePositionRoleTemplate(new PositionRoleTemplate(mapping, position, "lvl_m1_lower", role, firstTemplate, null, "active",
                f.admin(), Instant.now(), Instant.now()));
        M002Mapping m = new M002Mapping(mapping, position, role, firstTemplate);

        m002Assign(f, position, "m002-j32-assign");
        String first = m002PendingRequestId(f.target(), "position_high_risk_release");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(first, m002Approve("m002-j32-approve-" + f.suffix())));
        String application = m002PositionApplication(f.target());
        m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code())); // 使用一次
        assertThatThrownBy(() -> m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code())))
                .isInstanceOf(ApiException.class);

        List<Runnable> unchanged = List.of(
                () -> accessService.assignUserPosition(new UserPositionAssignmentRequest(
                        "m002-j32-reassign-" + f.suffix(), "M002", f.target(), position, "lvl_m1_lower", null, null, null)),
                () -> accessService.updatePositionRoleTemplate(mapping, m002MappingRequest("m002-j32-resave-" + f.suffix(), m, "active")));
        for (Runnable path : unchanged) {
            m002ActAs(f.admin(), "platform_admin", path);
            assertThatThrownBy(() -> m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code())))
                    .as("已消耗的单次高危权限不得因同岗重定或映射原样重存再次可用").isInstanceOf(ApiException.class);
            assertThat(m002PositionApplication(f.target())).isEqualTo(application);
            assertThat(jdbcTemplate.queryForObject("SELECT status || '/' || consumed FROM permission_template_applications WHERE id = ?",
                    String.class, application)).isEqualTo("active/true");
            assertThat(m002Status(first)).isEqualTo("applied");
            assertThat(m002PendingReleaseCount(f.target())).isZero();
        }

        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePositionRoleTemplate(mapping, m002MappingRequest(
                "m002-j32-regrant-" + f.suffix(), new M002Mapping(mapping, position, role, secondTemplate), "active")));
        assertThat(m002PositionApplication(f.target())).isNotEqualTo(application);
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
        String regrant = m002PendingRequestId(f.target(), "position_high_risk_release");
        assertThatThrownBy(() -> m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code())))
                .isInstanceOf(ApiException.class);
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(regrant, m002Approve("m002-j32-regrant-approve-" + f.suffix())));
        m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code()));
        assertThatThrownBy(() -> m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code())))
                .isInstanceOf(ApiException.class);
    }

    // M002-F32（B1；依据：治理地图第 660 行「以下五类权限授出前须先审批才生效」、第 703 行「该岗位模板被……修改时，原先未生效的这类权限……
    // 按新模板重新判断」；真库）：单次模板里的高危码批准后用掉一次，映射改指含同一码的长期模板——已消耗的码属「原先未生效」，不沿用旧批准：
    // 重新挂起并开新单，使用被拒；新单批准后才生效（长期，可多次使用）。
    @Test
    void consumedSingleUseCodeIsRejudgedWhenTheMappingSwitchesToALongTermTemplateOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        String position = "m002_bpos_" + f.suffix();
        String role = "role_m002_b_" + f.suffix();
        String once = "tpl_m002_b1_" + f.suffix();
        String longTerm = "tpl_m002_b3_" + f.suffix();
        String mapping = "prt_m002_b_" + f.suffix();
        repository.createRole(new RoleRecord(role, "M002 B1 岗位角色 " + f.suffix(), null, "project", "active", Instant.now(), List.of(), null));
        repository.createTemplate(m002SingleUseTemplate(once), grants(once, f.code()));
        repository.createTemplate(baseTemplate(longTerm), grants(longTerm, f.code()));
        repository.savePositionRoleTemplate(new PositionRoleTemplate(mapping, position, "lvl_m1_lower", role, once, null, "active",
                f.admin(), Instant.now(), Instant.now()));
        m002Assign(f, position, "m002-j34-assign");
        String first = m002PendingRequestId(f.target(), "position_high_risk_release");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(first, m002Approve("m002-j34-approve-" + f.suffix())));
        m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code()));

        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePositionRoleTemplate(mapping, m002MappingRequest(
                "m002-j34-long-" + f.suffix(), new M002Mapping(mapping, position, role, longTerm), "active")));
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
        String renewed = m002PendingRequestId(f.target(), "position_high_risk_release");
        assertThatThrownBy(() -> m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code())))
                .as("已消耗的单次码不得因映射改指长期模板而免审批生效").isInstanceOf(ApiException.class);
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(renewed, m002Approve("m002-j34-renewed-" + f.suffix())));
        m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code()));
        m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code()));
    }

    // M002-F32（B1，真库）：单次模板里的高危码批准后用掉一次；映射改职级，员工按原岗位职级改挂同岗同职级、引用含同一码长期模板的本组织映射——
    // 同样不沿用旧批准：重新挂起并开新单，使用被拒；批准后才生效。
    @Test
    void consumedSingleUseCodeIsRejudgedWhenTheEmployeeIsMovedToALongTermMappingOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        String position = "m002_cpos_" + f.suffix();
        String role = "role_m002_c_" + f.suffix();
        String once = "tpl_m002_c1_" + f.suffix();
        String longTerm = "tpl_m002_c3_" + f.suffix();
        String mapping = "prt_m002_c_" + f.suffix();
        String orgMapping = "prt_m002_co_" + f.suffix();
        repository.createRole(new RoleRecord(role, "M002 B1 改挂角色 " + f.suffix(), null, "project", "active", Instant.now(), List.of(), null));
        repository.createTemplate(m002SingleUseTemplate(once), grants(once, f.code()));
        repository.createTemplate(baseTemplate(longTerm), grants(longTerm, f.code()));
        repository.savePositionRoleTemplate(new PositionRoleTemplate(mapping, position, "lvl_m1_lower", role, once, null, "active",
                f.admin(), Instant.now(), Instant.now()));
        m002Assign(f, position, "m002-j35-assign");
        String first = m002PendingRequestId(f.target(), "position_high_risk_release");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(first, m002Approve("m002-j35-approve-" + f.suffix())));
        m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code()));
        repository.savePositionRoleTemplate(new PositionRoleTemplate(orgMapping, position, "lvl_m1_lower", role, longTerm, "org", "active",
                f.admin(), Instant.now(), Instant.now()));

        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePositionRoleTemplate(mapping, new PositionRoleTemplateRequest(
                "m002-j35-move-" + f.suffix(), "M002", position, "lvl_m10_upper", role, once, "active")));
        assertThat(m002AssignedMapping(f.target())).isEqualTo(orgMapping);
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
        String renewed = m002PendingRequestId(f.target(), "position_high_risk_release");
        assertThatThrownBy(() -> m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code())))
                .isInstanceOf(ApiException.class);
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(renewed, m002Approve("m002-j35-renewed-" + f.suffix())));
        m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code()));
    }

    // M002-F32（技术推导，真库）：已消耗的单次高危码不经新审批不会复活——改职级换到另一张同样引用单次模板的映射，该码挂起开单、使用被拒；
    // 定岗申请审批落地（整单审批即新的授权）后按新套用可再用一次。
    @Test
    void consumedSingleUseCodeNeedsANewApprovalAfterLevelChangeOrApprovalLandingOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        String position = "m002_dpos_" + f.suffix();
        String role = "role_m002_d_" + f.suffix();
        String lower = "tpl_m002_d1_" + f.suffix();
        String upper = "tpl_m002_d2_" + f.suffix();
        repository.createRole(new RoleRecord(role, "M002 改职级角色 " + f.suffix(), null, "project", "active", Instant.now(), List.of(), null));
        repository.createTemplate(m002SingleUseTemplate(lower), grants(lower, f.code()));
        repository.createTemplate(m002SingleUseTemplate(upper), grants(upper, f.code()));
        repository.savePositionRoleTemplate(new PositionRoleTemplate("prt_m002_d1_" + f.suffix(), position, "lvl_m1_lower", role, lower, null,
                "active", f.admin(), Instant.now(), Instant.now()));
        repository.savePositionRoleTemplate(new PositionRoleTemplate("prt_m002_d2_" + f.suffix(), position, "lvl_m10_upper", role, upper, null,
                "active", f.admin(), Instant.now(), Instant.now()));
        m002Assign(f, position, "m002-j36-assign");
        String first = m002PendingRequestId(f.target(), "position_high_risk_release");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(first, m002Approve("m002-j36-approve-" + f.suffix())));
        m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code()));

        m002ActAs(f.admin(), "platform_admin", () -> accessService.assignStaffLevel(new StaffLevelAssignmentRequest(
                "m002-j36-level-" + f.suffix(), "M002", f.target(), "lvl_m10_upper", null, null, null)));
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
        assertThat(m002PendingReleaseCount(f.target())).isEqualTo(1);
        assertThatThrownBy(() -> m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code())))
                .isInstanceOf(ApiException.class);

        m002ActAs(f.admin(), "platform_admin", () -> accessService.applyGrant(new AccessGrantApplyRequest(
                "m002-j36-apply-" + f.suffix(), "M002", List.of(), "user_position", f.target(), null, position, "lvl_m1_lower",
                null, null, null, null, null, null, false, "定岗申请")));
        String landing = m002PendingRequestId(f.target(), "user_position");
        assertThatThrownBy(() -> m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code())))
                .isInstanceOf(ApiException.class);
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(landing, m002Approve("m002-j36-landing-" + f.suffix())));
        m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code()));
        assertThatThrownBy(() -> m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code())))
                .isInstanceOf(ApiException.class);
    }

    // M002-F32c（依据：治理地图第 656 行「临时授权可设有效期或单次使用」、第 660 行「以下五类权限授出前须先审批才生效」；真库）：
    // 高危码 X 经单次或限时来源批准后尚未用完、未到期；映射改指含 X 的长期模板、只改角色为含 X 的角色、员工被改挂到引用长期模板的映射，
    // 或限时模板到期前改指长期模板——新来源比批准时更宽，旧批准不沿用：X 挂起、开新单、使用被拒；新单批准后才生效（可多次使用）。
    @Test
    void approvalOfASingleUseCodeDoesNotCarryOverWhenTheMappingSwitchesToALongTermTemplateOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        M002Widening w = m002WideningFixture(f, "e", m002SingleUseTemplate("tpl_m002_e1_" + f.suffix()));
        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePositionRoleTemplate(w.mapping(), m002MappingRequest(
                "m002-j37-long-" + f.suffix(), new M002Mapping(w.mapping(), w.position(), w.role(), w.longTerm()), "active")));
        m002ExpectRejudgedThenLongTerm(f, "m002-j37");
    }

    @Test
    void approvalOfASingleUseCodeDoesNotCarryOverWhenOnlyTheRoleChangesToOneGrantingItOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        M002Widening w = m002WideningFixture(f, "g", m002SingleUseTemplate("tpl_m002_g1_" + f.suffix()));
        String wideRole = "role_m002_gw_" + f.suffix();
        repository.createRole(new RoleRecord(wideRole, "M002 含高危码角色 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of(f.code()), null));
        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePositionRoleTemplate(w.mapping(), m002MappingRequest(
                "m002-j38-role-" + f.suffix(), new M002Mapping(w.mapping(), w.position(), wideRole, w.first()), "active")));
        m002ExpectRejudgedThenLongTerm(f, "m002-j38");
    }

    @Test
    void approvalOfASingleUseCodeDoesNotCarryOverWhenTheEmployeeIsMovedToALongTermMappingOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        M002Widening w = m002WideningFixture(f, "h", m002SingleUseTemplate("tpl_m002_h1_" + f.suffix()));
        String orgMapping = "prt_m002_ho_" + f.suffix();
        repository.savePositionRoleTemplate(new PositionRoleTemplate(orgMapping, w.position(), "lvl_m1_lower", w.role(), w.longTerm(), "org",
                "active", f.admin(), Instant.now(), Instant.now()));
        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePositionRoleTemplate(w.mapping(), new PositionRoleTemplateRequest(
                "m002-j39-move-" + f.suffix(), "M002", w.position(), "lvl_m10_upper", w.role(), w.first(), "active")));
        assertThat(m002AssignedMapping(f.target())).isEqualTo(orgMapping);
        m002ExpectRejudgedThenLongTerm(f, "m002-j39");
    }

    @Test
    void approvalOfATimeLimitedCodeDoesNotCarryOverWhenTheMappingSwitchesToALongTermTemplateOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        Instant at = Instant.parse("2026-06-14T00:00:00Z");
        String limited = "tpl_m002_k1_" + f.suffix();
        M002Widening w = m002WideningFixture(f, "k", new PermissionTemplate(limited, limited, limited, "M002 限时模板", "temporary", null,
                "platform", null, "merge", null, Instant.now().plus(java.time.Duration.ofDays(30)), false, "active", "integration test",
                "seed-admin", at, at, at));
        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePositionRoleTemplate(w.mapping(), m002MappingRequest(
                "m002-j40-long-" + f.suffix(), new M002Mapping(w.mapping(), w.position(), w.role(), w.longTerm()), "active")));
        m002ExpectRejudgedThenLongTerm(f, "m002-j40");
    }

    // M002-F32（技术推导，真库）：普通长期模板的同岗重定——保留原岗位派生套用（同一条、仍 active），模板里的日常码照常生效、不挂起。
    @Test
    void longTermPositionTemplateSurvivesSamePositionReassignmentOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        String daily = "m002.override_test.daily_" + f.suffix();
        jdbcTemplate.update("""
                INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
                VALUES (?, 'M002 测试日常权限', 'ops', 'low', FALSE, 'active')
                """, daily);
        String dailyHolder = "role_m002_daily_" + f.suffix();
        repository.createRole(new RoleRecord(dailyHolder, "M002 日常码持有 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of(daily), null));
        repository.assignUserRole(f.admin(), dailyHolder);
        String position = "m002_lpos_" + f.suffix();
        String role = "role_m002_l_" + f.suffix();
        String template = "tpl_m002_l_" + f.suffix();
        repository.createRole(new RoleRecord(role, "M002 长期岗位角色 " + f.suffix(), null, "project", "active", Instant.now(), List.of(), null));
        repository.createTemplate(baseTemplate(template), grants(template, daily));
        repository.savePositionRoleTemplate(new PositionRoleTemplate("prt_m002_l_" + f.suffix(), position, "lvl_m1_lower", role, template,
                null, "active", f.admin(), Instant.now(), Instant.now()));
        m002Assign(f, position, "m002-j33-assign");
        String application = m002PositionApplication(f.target());
        assertThat(m002EffectiveGrants(f)).contains(daily);

        m002Assign(f, position, "m002-j33-reassign");
        assertThat(m002PositionApplication(f.target())).isEqualTo(application);
        assertThat(m002ApplicationStatus(application)).isEqualTo("active");
        assertThat(m002EffectiveGrants(f)).contains(daily);
        assertThat(denyOrigins(f.target(), daily)).isEmpty();
        assertThat(m002ReleaseCount(f.target())).isZero();
    }

    // M002-F33（依据：治理地图第 655 行「最终权限 = 角色权限 + 基础模板 + 有效临时模板 + 员工单独授予 − 显式拒绝；显式拒绝优先级最高」；真库）：
    // 高危码 C 的员工单独授予获批后再定到含 C 的岗位——挂起照写（岗位派生那一份仍待释放单审批），C 仍生效；释放单驳回不删已批准的单独授予；
    // 撤掉单独授予后岗位派生不成为未经审批的第二来源，C 无效。requirePermission 与有效权限查询结论一致。
    @Test
    void approvedIndividualGrantSurvivesAPositionHoldOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        M002Mapping m = m002HighRiskMapping(f);
        String grant = m002ApplyOverride(f, "user_permission_grant", "m002-j42");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(grant, m002Approve("m002-j42-approve-" + f.suffix())));
        m002ExpectEffective(f, f.target(), true);

        m002Assign(f, m.position(), "m002-j42-assign");
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
        m002ExpectEffective(f, f.target(), true);
        String release = m002PendingRequestId(f.target(), "position_high_risk_release");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(release, m002Reject("m002-j42-reject-" + f.suffix())));
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM user_permission_overrides WHERE user_id = ? AND permission_code = ? AND effect = 'grant'
                """, Long.class, f.target(), f.code())).isEqualTo(1);
        m002ExpectEffective(f, f.target(), true);

        m002ActAs(f.admin(), "platform_admin", () -> accessService.removeUserPermissionOverrides(f.target(), new UserPermissionOverrideRequest(
                "m002-j42-remove-" + f.suffix(), "M002", List.of(f.code()), "grant", "撤销单独授予")));
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
        m002ExpectEffective(f, f.target(), false);
    }

    // M002-F33（真库）：先定岗、C 只因挂起而无效（没有其他来源时挂起仍使 C 无效）；再批准 C 的单独授予，C 生效，挂起仍在；
    // 释放单获批后挂起解除，C 继续生效。
    @Test
    void individualGrantApprovedAfterThePositionHoldTakesEffectOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        M002Mapping m = m002HighRiskMapping(f);
        m002Assign(f, m.position(), "m002-j43-assign");
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
        m002ExpectEffective(f, f.target(), false);

        String grant = m002ApplyOverride(f, "user_permission_grant", "m002-j43");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(grant, m002Approve("m002-j43-approve-" + f.suffix())));
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
        m002ExpectEffective(f, f.target(), true);

        String release = m002PendingRequestId(f.target(), "position_high_risk_release");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(release, m002Approve("m002-j43-release-" + f.suffix())));
        assertThat(denyOrigins(f.target(), f.code())).isEmpty();
        m002ExpectEffective(f, f.target(), true);
    }

    // M002-F33（真库）：同码的直接角色绑定、有效临时授权不被岗位挂起压掉；员工显式拒绝压过全部来源（直接角色、临时授权、岗位派生）。
    @Test
    void directRoleAndTemporaryGrantSurviveAPositionHoldButNotAnExplicitDenyOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        M002Mapping m = m002HighRiskMapping(f);
        String directRole = "role_m002_direct_" + f.suffix();
        repository.createRole(new RoleRecord(directRole, "M002 直接角色 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of(f.code()), null));
        repository.assignUserRole(f.target(), directRole);
        m002Assign(f, m.position(), "m002-j44-assign");
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
        m002ExpectEffective(f, f.target(), true);

        String temporary = "usr_m002_temp_" + f.suffix();
        repository.createUser(new AccessUser(temporary, temporary, "staff", "org", null, "active", Instant.now()));
        repository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), temporary, List.of(f.code()),
                Instant.now().minusSeconds(3600), Instant.now().plus(java.time.Duration.ofDays(1)), false, false, "active", "M002 临时授权",
                f.admin(), Instant.now(), null, null));
        m002ActAs(f.admin(), "platform_admin", () -> accessService.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-j44-temp-assign-" + f.suffix(), "M002", temporary, m.position(), "lvl_m1_lower", null, null, null)));
        assertThat(denyOrigins(temporary, f.code())).containsExactly("position_hold");
        m002ExpectEffective(f, temporary, true);

        for (String user : List.of(f.target(), temporary)) {
            m002ActAs(f.admin(), "platform_admin", () -> accessService.setUserPermissionOverrides(user, new UserPermissionOverrideRequest(
                    "m002-j44-deny-" + user, "M002", List.of(f.code()), "deny", "员工显式拒绝")));
            m002ExpectEffective(f, user, false);
        }
    }

    // M002-F20 b（回归 [17]，真库）：驳回后换岗（改职级到同岗位另一职级、同样含该码的映射）——已驳回留下的挂起按新岗位重挂、恰开一张新单
    // （快照是新职级），批准后生效。
    @Test
    void changingPositionAfterRejectionRejudgesTheHoldOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        M002Mapping m = m002HighRiskMapping(f);
        repository.savePositionRoleTemplate(new PositionRoleTemplate("prt_m002_g10_" + f.suffix(), m.position(), "lvl_m10_upper", m.role(),
                m.template(), null, "active", f.admin(), Instant.now(), Instant.now()));
        m002Assign(f, m.position(), "m002-j18-assign");
        String first = m002PendingRequestId(f.target(), "position_high_risk_release");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(first, m002Reject("m002-j18-reject-" + f.suffix())));

        m002ActAs(f.admin(), "platform_admin", () -> accessService.assignStaffLevel(new StaffLevelAssignmentRequest(
                "m002-j18-level-" + f.suffix(), "M002", f.target(), "lvl_m10_upper", null, null, null)));
        String renewed = m002PendingRequestId(f.target(), "position_high_risk_release");
        assertThat(repository.findGrantRequestById(renewed).orElseThrow().staffLevelId()).isEqualTo("lvl_m10_upper");
        assertThat(m002ReleaseCount(f.target())).isEqualTo(2);
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(renewed, m002Approve("m002-j18-approve-" + f.suffix())));
        assertThat(denyOrigins(f.target(), f.code())).isEmpty();
        assertThat(m002EffectiveGrants(f)).contains(f.code());
    }

    // M002-F20 c（回归 [17]，真库）：驳回后换到不含该码的岗位——挂起行删除、无待审单；再批准含该码的角色申请，该码生效。
    @Test
    void movingToAPositionWithoutTheCodeAfterRejectionOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        M002Mapping m = m002HighRiskMapping(f);
        String plainPosition = m002PlainMapping(f);
        m002Assign(f, m.position(), "m002-j19-assign");
        String first = m002PendingRequestId(f.target(), "position_high_risk_release");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(first, m002Reject("m002-j19-reject-" + f.suffix())));

        m002Assign(f, plainPosition, "m002-j19-move");
        assertThat(denyOrigins(f.target(), f.code())).isEmpty();
        assertThat(m002PendingReleaseCount(f.target())).isZero();
        m002ActAs(f.admin(), "platform_admin", () -> accessService.applyGrant(new AccessGrantApplyRequest(
                "m002-j19-role-" + f.suffix(), "M002", List.of(), "user_role", f.target(), m.role(), null, null, null, null, null,
                null, null, null, false, "角色授予申请")));
        String role = m002PendingRequestId(f.target(), "user_role");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(role, m002Approve("m002-j19-approve-" + f.suffix())));
        assertThat(m002EffectiveGrants(f)).contains(f.code());
    }

    // M002-F20 d（回归 [17]，真库）：同码员工显式拒绝在驳回后同岗重定、停用、再启用、换岗、范围变更各路径都保留，挂起行与待审单都不留。
    @Test
    void explicitDenySurvivesEveryVoidingPathOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        M002Mapping m = m002HighRiskMapping(f);
        String plainPosition = m002PlainMapping(f);
        m002Assign(f, m.position(), "m002-j20-assign");
        String first = m002PendingRequestId(f.target(), "position_high_risk_release");
        m002ActAs(f.admin(), "platform_admin", () -> accessService.setUserPermissionOverrides(f.target(), new UserPermissionOverrideRequest(
                "m002-j20-deny-" + f.suffix(), "M002", List.of(f.code()), "deny", "员工显式拒绝")));
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(first, m002Reject("m002-j20-reject-" + f.suffix())));

        List<Runnable> paths = List.of(
                () -> accessService.assignUserPosition(new UserPositionAssignmentRequest(
                        "m002-j20-reassign-" + f.suffix(), "M002", f.target(), m.position(), "lvl_m1_lower", null, null, null)),
                () -> accessService.updatePositionRoleTemplate(m.id(), m002MappingRequest("m002-j20-off-" + f.suffix(), m, "inactive")),
                () -> accessService.updatePositionRoleTemplate(m.id(), m002MappingRequest("m002-j20-on-" + f.suffix(), m, "active")),
                () -> accessService.assignUserPosition(new UserPositionAssignmentRequest(
                        "m002-j20-move-" + f.suffix(), "M002", f.target(), plainPosition, "lvl_m1_lower", null, null, null)),
                () -> accessService.changeUserScope(f.target(), new com.complex.platform.access.dto.UserScopeChangeRequest(
                        "m002-j20-scope-" + f.suffix(), "M002", "org_m002_other", null, null, null, null, null, null, "调往另一项目", null)));
        for (Runnable path : paths) {
            m002ActAs(f.admin(), "platform_admin", path);
            assertThat(denyOrigins(f.target(), f.code())).containsExactly("explicit");
            assertThat(m002PendingReleaseCount(f.target())).isZero();
        }
    }

    // M002-F20 f（回归 [21]，真库）：码只有岗位挂起、没有任何授予来源时直接解除 400，挂起行不动，不写 remove 成功留痕（基线会删 0 行后记成功）。
    @Test
    void removingAHoldWithoutAnyGrantSourceIsRejectedOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        repository.saveHighRiskHolds(f.target(), List.of(f.code()), "岗位高危挂起", f.admin());
        assertThatThrownBy(() -> m002ActAs(f.admin(), "platform_admin", () -> accessService.removeUserPermissionOverrides(f.target(),
                m002RemoveDeny("m002-j21-remove-" + f.suffix(), f.code()))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM audit_events
                 WHERE event_name = 'access.user_permission_override.remove' AND event_result = 'success' AND business_id = ?
                """, Long.class, f.target())).isZero();
    }

    // M002-F21：真实接口回归——两类申请的权限码列表混有 null / 空白时 400 VALIDATION_FAILED（不再落成 500），不建申请单、不建复核单（回归 [20]）。
    @Test
    void overrideRequestsWithNullOrBlankCodesAreRejectedWith400ThroughTheEndpoint() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        var session = m002Session(new AccessActor(f.admin(), f.admin(), "platform_admin", "org", null, "active"));
        String countReviews = "SELECT (SELECT count(*) FROM supervisor_review_decisions) || '/' || (SELECT count(*) FROM supervisor_review_nodes)";
        String reviewsBefore = jdbcTemplate.queryForObject(countReviews, String.class);
        int attempts = 0;
        for (String type : List.of("user_permission_grant", "user_permission_deny_removal")) {
            for (String codes : List.of("\"%1$s\", null", "null, \"%1$s\"", "\"%1$s\", \"  \"")) {
                mockMvc.perform(post("/api/v1/access/grant-requests").with(session).contentType(MediaType.APPLICATION_JSON)
                                .content(("{\"idempotency_key\": \"m002-j13-%s-%d\", \"request_type\": \"%s\", \"target_user_id\": \"%s\", "
                                        + "\"permission_codes\": [%s], \"reason\": \"M002\"}")
                                        .formatted(f.suffix(), ++attempts, type, f.target(), codes.formatted(f.code()))))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
            }
        }
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM access_grant_requests WHERE target_user_id = ?", Long.class, f.target()))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(countReviews, String.class)).isEqualTo(reviewsBefore);
    }

    // M002-F27（技术不变量：幂等键列宽 VARCHAR(128)，OpenAPI 幂等键 maxLength 128）：真实接口建角色——129 字幂等键 400 VALIDATION_FAILED，
    // 发生在幂等预留与业务写入之前（不留角色、不留幂等记录、无任何带该键的事件）；128 字幂等键可正常建。
    @Test
    void idempotencyKeyLongerThan128IsRejectedWith400ThroughTheEndpoint() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        String manageRole = "role_m002_f27_" + f.suffix();
        repository.createRole(new RoleRecord(manageRole, "M002 建角色 F27 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of("access.role.manage"), null));
        repository.assignUserRole(f.admin(), manageRole);
        var session = m002Session(new AccessActor(f.admin(), f.admin(), "platform_admin", "org", null, "active"));
        String body = "{\"idempotency_key\": \"%s\", \"role_name\": \"%s\", \"permission_codes\": [], \"data_scope\": \"project\"}";
        String tooLong = ("k" + f.suffix()).repeat(20).substring(0, 129);
        String name = "M002 幂等键长度 " + f.suffix();
        mockMvc.perform(post("/api/v1/access/roles").with(session).contentType(MediaType.APPLICATION_JSON).content(body.formatted(tooLong, name)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM roles WHERE role_name = ?", Long.class, name)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM idempotency_records WHERE idempotency_key = ?", Long.class, tooLong)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM audit_events WHERE idempotency_key = ?", Long.class, tooLong)).isZero();

        String longest = tooLong.substring(0, 128);
        mockMvc.perform(post("/api/v1/access/roles").with(session).contentType(MediaType.APPLICATION_JSON).content(body.formatted(longest, name)))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM roles WHERE role_name = ?", Long.class, name)).isEqualTo(1);
    }

    // M002-F28（技术不变量：岗位映射 position_code 列宽 VARCHAR(64)；真库）：新建岗位映射岗位编码 120 字、修改为 65 字均 400 VALIDATION_FAILED
    // （不再落成列宽或幂等预留 500），不落行、原映射不变；64 字可保存。
    @Test
    void positionCodeLongerThanTheColumnIsRejectedOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        M002Mapping m = m002HighRiskMapping(f);
        String tooLong = ("q" + f.suffix()).repeat(14).substring(0, 120);
        List<Runnable> entries = List.of(
                () -> accessService.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                        "m002-j29-create-" + f.suffix(), "M002", tooLong, "lvl_m1_lower", m.role(), m.template(), "active")),
                () -> accessService.updatePositionRoleTemplate(m.id(), new PositionRoleTemplateRequest(
                        "m002-j29-update-" + f.suffix(), "M002", tooLong.substring(0, 65), "lvl_m1_lower", m.role(), m.template(), "active")));
        for (Runnable entry : entries) {
            assertThatThrownBy(() -> m002ActAs(f.admin(), "platform_admin", entry))
                    .isInstanceOfSatisfying(ApiException.class, e -> {
                        assertThat(e.status().value()).isEqualTo(400);
                        assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                    });
        }
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM position_role_templates WHERE position_code LIKE ?", Long.class,
                "q" + f.suffix() + "%")).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT position_code FROM position_role_templates WHERE id = ?", String.class, m.id()))
                .isEqualTo(m.position());

        String longest = tooLong.substring(0, 64);
        m002ActAs(f.admin(), "platform_admin", () -> accessService.createPositionRoleTemplate(new PositionRoleTemplateRequest(
                "m002-j29-ok-" + f.suffix(), "M002", longest, "lvl_m1_lower", m.role(), m.template(), "active")));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM position_role_templates WHERE position_code = ?", Long.class, longest))
                .isEqualTo(1);
    }

    // M002-F31（技术不变量：岗位行、申请单 position_code 列宽 VARCHAR(64)；F28 验收「及其他写 position_code 的入口」）：真实接口——定岗、
    // 岗位申请建单、范围变更的岗位编码 65 字与 120 字，都在查映射与任何业务写入之前 400 VALIDATION_FAILED（不再落成找不到映射 404）：
    // 不写岗位行、不建申请单、范围变更不撤销旧权限、不改所属范围、无带这些键的成功事件；64 字的有效映射仍可定岗。
    @Test
    void positionCodeLongerThanTheColumnIsRejectedAtEveryAssignmentEntryThroughTheEndpoints() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        String assignRole = "role_m002_f31_" + f.suffix();
        repository.createRole(new RoleRecord(assignRole, "M002 装配角色 F31 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of("access.role.assign"), null));
        repository.assignUserRole(f.admin(), assignRole);
        String kept = "role_m002_f31_kept_" + f.suffix();
        repository.createRole(new RoleRecord(kept, "M002 保留角色 F31 " + f.suffix(), null, "project", "active", Instant.now(), List.of(), null));
        repository.assignUserRole(f.target(), kept);
        var session = m002Session(new AccessActor(f.admin(), f.admin(), "platform_admin", "org", null, "active"));
        String base = ("w" + f.suffix()).repeat(14);
        int attempt = 0;
        for (String code : List.of(base.substring(0, 65), base.substring(0, 120))) {
            String n = f.suffix() + "-" + (++attempt);
            mockMvc.perform(put("/api/v1/access/users/" + f.target() + "/position").with(session).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idempotency_key\": \"m002-j30-position-%s\", \"position_code\": \"%s\", \"staff_level_id\": \"lvl_m1_lower\"}"
                                    .formatted(n, code)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
            mockMvc.perform(post("/api/v1/access/grant-requests").with(session).contentType(MediaType.APPLICATION_JSON)
                            .content(("{\"idempotency_key\": \"m002-j30-apply-%s\", \"request_type\": \"user_position\", \"target_user_id\": \"%s\", "
                                    + "\"position_code\": \"%s\", \"staff_level_id\": \"lvl_m1_lower\", \"reason\": \"岗位申请\"}").formatted(n, f.target(), code)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
            mockMvc.perform(put("/api/v1/access/users/" + f.target() + "/scope").with(session).contentType(MediaType.APPLICATION_JSON)
                            .content(("{\"idempotency_key\": \"m002-j30-scope-%s\", \"organization_id\": \"org_m002_other\", \"position_code\": \"%s\", "
                                    + "\"staff_level_id\": \"lvl_m1_lower\", \"reason\": \"调往另一项目\"}").formatted(n, code)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        }
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM user_position_assignments WHERE user_id = ?", Long.class, f.target()))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM access_grant_requests WHERE target_user_id = ?", Long.class, f.target()))
                .isZero();
        assertThat(repository.findUserRoleIds(f.target())).contains(kept);
        assertThat(jdbcTemplate.queryForObject("SELECT organization_id FROM access_users WHERE id = ?", String.class, f.target())).isEqualTo("org");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_result = 'success' AND idempotency_key LIKE ?", Long.class,
                "m002-j30-%" + f.suffix() + "%")).isZero();

        String longest = base.substring(0, 64);
        String role = "role_m002_j30_" + f.suffix();
        String template = "tpl_m002_j30_" + f.suffix();
        repository.createRole(new RoleRecord(role, "M002 64 字岗位角色 " + f.suffix(), null, "project", "active", Instant.now(), List.of(), null));
        repository.createTemplate(baseTemplate(template), List.of());
        repository.savePositionRoleTemplate(new PositionRoleTemplate("prt_m002_j30_" + f.suffix(), longest, "lvl_m1_lower", role, template,
                null, "active", f.admin(), Instant.now(), Instant.now()));
        mockMvc.perform(put("/api/v1/access/users/" + f.target() + "/position").with(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\": \"m002-j30-ok-%s\", \"position_code\": \"%s\", \"staff_level_id\": \"lvl_m1_lower\"}"
                                .formatted(f.suffix(), longest)))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject("SELECT position_code FROM user_position_assignments WHERE user_id = ?", String.class, f.target()))
                .isEqualTo(longest);
    }

    // M002-F23：真实接口回归——建角色 role_name 121 字、data_scope 65 字或不在契约枚举内均 400 VALIDATION_FAILED（不再落成列宽 500），
    // 不留角色；120 字名与枚举值 self 可建。
    @Test
    void createRoleRejectsContractViolationsWith400ThroughTheEndpoint() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        String manageRole = "role_m002_f23_" + f.suffix();
        repository.createRole(new RoleRecord(manageRole, "M002 建角色 F23 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of("access.role.manage"), null));
        repository.assignUserRole(f.admin(), manageRole);
        var session = m002Session(new AccessActor(f.admin(), f.admin(), "platform_admin", "org", null, "active"));
        String body = "{\"idempotency_key\": \"%s\", \"role_name\": \"%s\", \"permission_codes\": [], \"data_scope\": \"%s\"}";
        List<String[]> invalid = List.of(
                new String[] {"m002-j14-name-" + f.suffix(), "角".repeat(121), "project"},
                new String[] {"m002-j14-scope-long-" + f.suffix(), "M002 范围超长 " + f.suffix(), "s".repeat(65)},
                new String[] {"m002-j14-scope-enum-" + f.suffix(), "M002 范围不在枚举 " + f.suffix(), "tenant"});
        for (String[] values : invalid) {
            mockMvc.perform(post("/api/v1/access/roles").with(session).contentType(MediaType.APPLICATION_JSON)
                            .content(body.formatted(values[0], values[1], values[2])))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM roles WHERE role_name = ?", Long.class, values[1])).isZero();
        }
        String longest = "角".repeat(112) + f.suffix();
        mockMvc.perform(post("/api/v1/access/roles").with(session).contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted("m002-j14-ok-" + f.suffix(), longest, "self")))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject("SELECT data_scope FROM roles WHERE role_name = ?", String.class, longest)).isEqualTo("self");
    }

    // ── M002 第 10 轮（M002-F34、F37 为操作员 2026-09-30 裁决；F35、F36 为技术不变量）真库与真实接口回归 ─────────────────────────────────

    // M002-F34（操作员 2026-09-30 裁决，治理地图第 703 行「其他人不能给最高权限账号设置显式拒绝。」）：真库＋真实接口——平台管理员、同组织持
    // access.role.assign 的员工经接口给最高权限账号设显式拒绝 403 MASTER_AUTHORITY_DENY_FORBIDDEN，不写拒绝行、留 set_failed；停用中的最高权限
    // 账号作目标同样被拒；最高权限本人会话给自己设、解除照常，第二轮仍可。
    @Test
    void othersCannotSetAnExplicitDenyOnTheMasterAuthorityThroughTheEndpoint() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        String master = m002MasterAccount();
        String assigner = "usr_m002_f34_pc_" + f.suffix();
        String assignRole = "role_m002_f34_" + f.suffix();
        repository.createUser(new AccessUser(assigner, assigner, "staff", "platform-company", null, "active", Instant.now()));
        repository.createRole(new RoleRecord(assignRole, "M002 F34 装配角色 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of("access.role.assign"), null));
        repository.assignUserRole(assigner, assignRole);
        var admin = m002Session(new AccessActor(f.admin(), f.admin(), "platform_admin", "org", null, "active"));
        var staff = m002Session(new AccessActor(assigner, assigner, "staff", "platform-company", null, "active"));

        m002MasterDenyRejected(master, admin, "m002-j34-admin-" + f.suffix());
        m002MasterDenyRejected(master, staff, "m002-j34-staff-" + f.suffix());
        jdbcTemplate.update("UPDATE access_users SET status = 'disabled' WHERE id = ?", master);
        m002MasterDenyRejected(master, admin, "m002-j34-disabled-" + f.suffix());
        jdbcTemplate.update("UPDATE access_users SET status = 'active' WHERE id = ?", master);

        var masterSession = m002Session(new AccessActor(master, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                "platform-company", null, "active"));
        for (int round = 1; round <= 2; round++) {
            mockMvc.perform(post("/api/v1/access/users/" + master + "/permission-overrides").with(masterSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(m002DenyJson("m002-j34-self-deny-" + round + "-" + f.suffix(), "access.role.assign")))
                    .andExpect(status().isOk());
            assertThat(denyOrigins(master, "access.role.assign")).containsExactly("explicit");
            mockMvc.perform(post("/api/v1/access/users/" + master + "/permission-overrides/remove").with(masterSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(m002DenyJson("m002-j34-self-remove-" + round + "-" + f.suffix(), "access.role.assign")))
                    .andExpect(status().isOk());
            assertThat(denyOrigins(master, "access.role.assign")).isEmpty();
        }
    }

    private void m002MasterDenyRejected(String master, org.springframework.test.web.servlet.request.RequestPostProcessor session, String key)
            throws Exception {
        mockMvc.perform(post("/api/v1/access/users/" + master + "/permission-overrides").with(session)
                        .contentType(MediaType.APPLICATION_JSON).content(m002DenyJson(key, "access.role.assign")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("MASTER_AUTHORITY_DENY_FORBIDDEN"));
        assertThat(denyOrigins(master, "access.role.assign")).isEmpty();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT event_name || '/' || business_id || '/' || (metadata ->> 'error_code') FROM audit_events
                 WHERE idempotency_key = ? AND event_result = 'failure'
                """, String.class, key)).isEqualTo("access.user_permission_override.set_failed/" + master + "/MASTER_AUTHORITY_DENY_FORBIDDEN");
    }

    // M002-F35（治理地图第 656 行「权限的授予、收回、到期、单次消耗、失效全部留痕」）：真库——映射停用时挂在该映射上的每名员工各留一条
    // access.user_position.derivation_revoked（员工、映射、原岗位派生角色、被撤销的岗位派生套用、原因 inactive、操作人），有挂起的另有
    // high_risk_voided；再次保存为停用、再启用、原样重存与同岗重定都不再记收回。
    @Test
    void deactivatingAMappingLeavesOneRevocationTrailPerEmployeeOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        M002Mapping m = m002HighRiskMapping(f);
        String second = "usr_m002_f35_" + f.suffix();
        repository.createUser(new AccessUser(second, second, "staff", "org", null, "active", Instant.now()));
        m002Assign(f, m.position(), "m002-j35-assign");
        m002ActAs(f.admin(), "platform_admin", () -> accessService.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-j35-assign-second-" + f.suffix(), "M002", second, m.position(), "lvl_m1_lower", null, null, null)));
        List<String> users = List.of(f.target(), second);
        List<String> applications = users.stream().map(this::m002PositionApplicationIds).toList();

        m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePositionRoleTemplate(m.id(),
                m002MappingRequest("m002-j35-off-" + f.suffix(), m, "inactive")));
        for (int i = 0; i < users.size(); i++) {
            assertThat(m002Revocations(users.get(i)))
                    .containsExactly(String.join("|", users.get(i), m.id(), m.role(), applications.get(i), "inactive", f.admin()));
            assertThat(m002EventCount("access.user_position.high_risk_voided", users.get(i))).isEqualTo(1);
            assertThat(m002ApplicationStatus(applications.get(i))).isEqualTo("revoked");
        }

        for (String step : List.of("off-again", "on", "resave")) {
            m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePositionRoleTemplate(m.id(),
                    m002MappingRequest("m002-j35-" + step + "-" + f.suffix(), m, "off-again".equals(step) ? "inactive" : "active")));
        }
        m002Assign(f, m.position(), "m002-j35-reassign");
        assertThat(m002AssignedRole(f.target())).isEqualTo(m.role());
        for (String user : users) {
            assertThat(m002Revocations(user)).hasSize(1);
        }
    }

    // M002-F35：真库——映射改职级后，原来挂在上面、没有挂起的员工找不到映射：收回岗位派生并留一条原因 no_mapping 的收回记录（基线此时没有任何以该
    // 员工为对象的留痕）；再保存改后的映射不再记。
    @Test
    void lostMappingAfterAStaffLevelChangeLeavesARevocationTrailOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        String position = m002PlainMapping(f);
        String mapping = "prt_m002_p_" + f.suffix();
        String role = "role_m002_p_" + f.suffix();
        String template = "tpl_m002_p_" + f.suffix();
        m002Assign(f, position, "m002-j35b-assign");
        String applications = m002PositionApplicationIds(f.target());

        for (String step : List.of("move", "move-again")) {
            m002ActAs(f.admin(), "platform_admin", () -> accessService.updatePositionRoleTemplate(mapping, new PositionRoleTemplateRequest(
                    "m002-j35b-" + step + "-" + f.suffix(), "M002", position, "lvl_m10_upper", role, template, "active")));
            assertThat(m002Revocations(f.target()))
                    .containsExactly(String.join("|", f.target(), mapping, role, applications, "no_mapping", f.admin()));
        }
        assertThat(m002AssignedRole(f.target())).isNull();
        assertThat(m002AssignedMapping(f.target())).isNull();
        assertThat(m002ApplicationStatus(applications)).isEqualTo("revoked");
    }

    private String m002PositionApplicationIds(String userId) {
        String ids = jdbcTemplate.queryForObject(
                "SELECT array_to_string(position_application_ids, ',') FROM user_position_assignments WHERE user_id = ?", String.class, userId);
        assertThat(ids).as("夹具：%s 须有岗位派生套用", userId).isNotBlank().doesNotContain(",");
        return ids;
    }

    private List<String> m002Revocations(String userId) {
        return jdbcTemplate.queryForList("""
                SELECT concat_ws('|', metadata ->> 'user_id', metadata ->> 'mapping_id', metadata ->> 'role_id', metadata ->> 'application_ids',
                                 metadata ->> 'reason', metadata ->> 'operator_id')
                  FROM audit_events
                 WHERE event_name = 'access.user_position.derivation_revoked' AND event_result = 'success' AND business_id = ?
                 ORDER BY 1
                """, String.class, userId);
    }

    private long m002EventCount(String eventName, String businessId) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = ? AND business_id = ?",
                Long.class, eventName, businessId);
    }

    // M002-F36（契约 RoleAssignmentRequest.user_id / role_id maxLength 64；失败留痕 business_id 列宽 VARCHAR(128)）：真库＋真实接口——分配、撤销角色的
    // user_id 与员工覆盖设置、解除的路径用户编号 129 字（基线写失败留痕超列宽 500、留痕丢失）与 65 字（基线 404）一律 400 VALIDATION_FAILED，不留
    // 带该键的事件与幂等记录；分配、撤销的 role_id 65 字同样 400；64 字不存在的编号仍 404 且失败留痕写入；请求体编号前带 70 个空格（去空白后 64 字、
    // 原串 134 字，基线把原串写进失败留痕超列宽 500）时同样 404，失败留痕记去空白后的编号。
    @Test
    void overlongTargetIdsAreRejectedWith400AndSixtyFourCharacterIdsStill404ThroughTheEndpoints() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        var session = m002Session(new AccessActor(f.admin(), f.admin(), "platform_admin", "org", null, "active"));
        String role = "role_m002_f36_" + f.suffix();
        repository.createRole(new RoleRecord(role, "M002 F36 角色 " + f.suffix(), null, "project", "active", Instant.now(), List.of(), null));
        List<String> kinds = List.of("assign", "unassign", "set", "remove");
        int attempt = 0;
        for (String id : List.of("x".repeat(129), "y".repeat(65))) {
            for (String kind : kinds) {
                String key = "m002-j36-" + kind + "-" + ++attempt + "-" + f.suffix();
                m002IdCall(kind, id, role, key, session)
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
                m002AssertNoTrace(key);
            }
        }
        for (String kind : List.of("assign", "unassign")) {
            String key = "m002-j36-role-" + kind + "-" + f.suffix();
            m002IdCall(kind, f.target(), "r".repeat(65), key, session)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
            m002AssertNoTrace(key);
        }

        String id64 = ("z" + f.suffix()).repeat(8).substring(0, 64);
        java.util.Map<String, String> failed = java.util.Map.of("assign", "access.role.assign_failed", "unassign", "access.role.unassign_failed",
                "set", "access.user_permission_override.set_failed", "remove", "access.user_permission_override.remove_failed");
        String padding = " ".repeat(70);
        for (String kind : kinds) {
            List<String> ids = kind.equals("assign") || kind.equals("unassign") ? List.of(id64, padding + id64) : List.of(id64);
            for (String id : ids) {
                String key = "m002-j36-404-" + kind + "-" + ++attempt + "-" + f.suffix();
                m002IdCall(kind, id, role, key, session)
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.detail_code").value("USER_NOT_FOUND"));
                assertThat(jdbcTemplate.queryForObject(
                        "SELECT event_name || '/' || business_id FROM audit_events WHERE idempotency_key = ? AND event_result = 'failure'",
                        String.class, key)).isEqualTo(failed.get(kind) + "/" + id64);
            }
        }
    }

    private org.springframework.test.web.servlet.ResultActions m002IdCall(String kind, String userId, String roleId, String key,
            org.springframework.test.web.servlet.request.RequestPostProcessor session) throws Exception {
        return switch (kind) {
            case "assign" -> mockMvc.perform(post("/api/v1/access/role-assignments").with(session)
                    .contentType(MediaType.APPLICATION_JSON).content(m002UnassignJson(key, userId, roleId)));
            case "unassign" -> mockMvc.perform(post("/api/v1/access/role-assignments/unassign").with(session)
                    .contentType(MediaType.APPLICATION_JSON).content(m002UnassignJson(key, userId, roleId)));
            case "set" -> mockMvc.perform(post("/api/v1/access/users/{id}/permission-overrides", userId).with(session)
                    .contentType(MediaType.APPLICATION_JSON).content(m002DenyJson(key, "access.role.assign")));
            default -> mockMvc.perform(post("/api/v1/access/users/{id}/permission-overrides/remove", userId).with(session)
                    .contentType(MediaType.APPLICATION_JSON).content(m002DenyJson(key, "access.role.assign")));
        };
    }

    private void m002AssertNoTrace(String key) {
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM audit_events WHERE idempotency_key = ?", Long.class, key)).as(key).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM idempotency_records WHERE idempotency_key = ?", Long.class, key))
                .as(key).isZero();
    }

    // M002-F37（操作员 2026-09-30 裁决，治理地图第 651 行「员工单独授予、解除员工显式拒绝、岗位模板高危权限释放这三类授权申请，只能由具备分配/取消
    // 分配角色权限的人发起。」）：真库＋真实接口——只持 role_staff_self（V394 种子含 access.grant_request.apply）的普通员工对三类申请（内容均有效）
    // POST /grant-requests 一律 403 PERMISSION_DENIED，不建单、不开复核单，第二轮仍被拒；同一员工的临时权限申请仍 200；持 access.role.assign 的
    // 平台管理员三类均 200。
    @Test
    void onlyRoleAssignHoldersInitiateTheThreeRequestTypesThroughTheEndpoint() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        M002Mapping m = m002HighRiskMapping(f);
        m002Assign(f, m.position(), "m002-j37-assign");
        String first = m002PendingRequestId(f.target(), "position_high_risk_release");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(first, m002Reject("m002-j37-reject-" + f.suffix())));
        repository.saveUserPermissionOverrides(f.target(), List.of(f.code()), AccessRepository.Effect.DENY, "员工显式拒绝", f.admin());
        String staff = "usr_m002_f37_staff_" + f.suffix();
        repository.createUser(new AccessUser(staff, staff, "staff", "org", null, "active", Instant.now()));
        repository.assignUserRole(staff, "role_staff_self");
        jdbcTemplate.update("INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by) VALUES (?, ?, 'main', ?)",
                staff, f.boss(), f.admin());
        var staffSession = m002Session(new AccessActor(staff, staff, "staff", "org", null, "active"));
        String countRequests = "SELECT count(*) FROM access_grant_requests WHERE target_user_id = ?";
        String countReviews = "SELECT (SELECT count(*) FROM supervisor_review_decisions) || '/' || (SELECT count(*) FROM supervisor_review_nodes)";
        long requestsBefore = jdbcTemplate.queryForObject(countRequests, Long.class, f.target());
        String reviewsBefore = jdbcTemplate.queryForObject(countReviews, String.class);
        List<String> types = List.of("user_permission_grant", "user_permission_deny_removal", "position_high_risk_release");
        String body = "{\"idempotency_key\": \"%s\", \"request_type\": \"%s\", \"target_user_id\": \"%s\", \"permission_codes\": [\"%s\"], "
                + "\"reason\": \"M002\"}";

        for (int round = 1; round <= 2; round++) {
            for (String type : types) {
                mockMvc.perform(post("/api/v1/access/grant-requests").with(staffSession).contentType(MediaType.APPLICATION_JSON)
                                .content(body.formatted("m002-j37-staff-" + type + "-" + round + "-" + f.suffix(), type, f.target(), f.code())))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
            }
        }
        assertThat(jdbcTemplate.queryForObject(countRequests, Long.class, f.target())).isEqualTo(requestsBefore);
        assertThat(jdbcTemplate.queryForObject(countReviews, String.class)).isEqualTo(reviewsBefore);

        mockMvc.perform(post("/api/v1/access/grant-requests").with(staffSession).contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"idempotency_key\": \"m002-j37-staff-temporary-%s\", \"request_type\": \"temporary_permission\", "
                                + "\"target_user_id\": \"%s\", \"permission_codes\": [\"amusement.device.view\"], "
                                + "\"expires_at\": \"2099-01-01T00:00:00Z\", \"reason\": \"临时顶班\"}").formatted(f.suffix(), staff)))
                .andExpect(status().isOk());
        assertThat(m002PendingRequestId(staff, "temporary_permission")).isNotNull();

        var adminSession = m002Session(new AccessActor(f.admin(), f.admin(), "platform_admin", "org", null, "active"));
        for (String type : types) {
            mockMvc.perform(post("/api/v1/access/grant-requests").with(adminSession).contentType(MediaType.APPLICATION_JSON)
                            .content(body.formatted("m002-j37-admin-" + type + "-" + f.suffix(), type, f.target(), f.code())))
                    .andExpect(status().isOk());
            assertThat(m002PendingRequestId(f.target(), type)).isNotNull();
        }
    }

    // ── M002 第 11 轮（M002-F38；依据治理地图第 653 行「所有业务数据默认按项目隔离」「数据的项目与商户隔离不提供关闭开关」与七类红线④，技术推导）──

    // M002-F38：真库＋真实接口——只持 role_staff_self（含 access.grant_request.apply）的普通员工（无商户）给其他项目的员工 POST /grant-requests
    // 提交岗位申请（user_position，映射含须审批码），403 CROSS_ORG_FORBIDDEN：不建授权单、不开复核单，目标原有岗位、职级、岗位派生角色与映射、
    // 直接角色、模板套用与覆盖行（含挂起）不变。
    @Test
    void positionGrantRequestForATargetInAnotherProjectIsRejectedThroughTheEndpoint() throws Exception {
        m002F38RejectedThroughTheEndpoint(null, "org_m002_f38_other", null, "CROSS_ORG_FORBIDDEN");
    }

    // M002-F38：真库＋真实接口——带商户的普通员工给同项目其他商户的员工提交岗位申请，403 CROSS_MERCHANT_FORBIDDEN，其余同上。
    @Test
    void positionGrantRequestForATargetOfAnotherMerchantInTheSameProjectIsRejectedThroughTheEndpoint() throws Exception {
        m002F38RejectedThroughTheEndpoint("mch_m002_f38_a", "org", "mch_m002_f38_b", "CROSS_MERCHANT_FORBIDDEN");
    }

    private void m002F38RejectedThroughTheEndpoint(String applicantMerchant, String targetOrg, String targetMerchant, String code)
            throws Exception {
        M002Fixture f = m002ApprovalFixture();
        M002Mapping m = m002HighRiskMapping(f);
        String plain = m002PlainMapping(f);
        String target = "usr_m002_f38_target_" + f.suffix();
        String staff = "usr_m002_f38_staff_" + f.suffix();
        String merchant = applicantMerchant == null ? null : applicantMerchant + "_" + f.suffix();
        repository.createUser(new AccessUser(target, target, "staff", targetOrg, targetMerchant == null ? null : targetMerchant + "_" + f.suffix(),
                "active", Instant.now()));
        repository.createUser(new AccessUser(staff, staff, "staff", "org", merchant, "active", Instant.now()));
        repository.assignUserRole(staff, "role_staff_self");
        // 修复前可建单、开复核单（缺陷复现），不因申请人没有上级链而失败
        jdbcTemplate.update("INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by) VALUES (?, ?, 'main', ?)",
                staff, f.boss(), f.admin());
        m002ActAs(f.admin(), "platform_admin", () -> accessService.assignUserPosition(new UserPositionAssignmentRequest(
                "m002-j38-seat-" + f.suffix(), "M002", target, plain, "lvl_m1_lower", null, null, null)));
        List<String> before = m002TargetFacts(target);
        String countReviews = "SELECT (SELECT count(*) FROM supervisor_review_decisions) || '/' || (SELECT count(*) FROM supervisor_review_nodes)";
        String reviewsBefore = jdbcTemplate.queryForObject(countReviews, String.class);

        mockMvc.perform(post("/api/v1/access/grant-requests").with(m002Session(new AccessActor(staff, staff, "staff", "org", merchant, "active")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"idempotency_key\": \"m002-j38-apply-%s\", \"request_type\": \"user_position\", \"target_user_id\": \"%s\", "
                                + "\"position_code\": \"%s\", \"staff_level_id\": \"lvl_m1_lower\", \"reason\": \"岗位申请\"}")
                                .formatted(f.suffix(), target, m.position())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value(code));

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM access_grant_requests WHERE target_user_id = ?", Long.class, target))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(countReviews, String.class)).isEqualTo(reviewsBefore);
        assertThat(m002AssignedRole(target)).isEqualTo("role_m002_p_" + f.suffix());
        assertThat(m002TargetFacts(target)).isEqualTo(before);
    }

    // M002-F38 正向对照：真库＋真实接口——只持 role_staff_self 的同项目普通员工给本项目员工 POST 岗位申请照常 200 建单，其直接上级属另一项目、
    // 作为链上终审人批准后照常落地（落地不按终审人组织判定）；平台管理员给其他项目员工的岗位申请照常 200 建单，获批后落地。
    @Test
    void positionGrantRequestsWithinScopeOrByThePlatformAdminLandWithACrossProjectFinalApproverThroughTheEndpoint() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        M002Mapping m = m002HighRiskMapping(f);
        String staff = "usr_m002_f38_peer_" + f.suffix();
        String superior = "usr_m002_f38_hq_" + f.suffix();
        String otherOrg = "usr_m002_f38_far_" + f.suffix();
        repository.createUser(new AccessUser(staff, staff, "staff", "org", null, "active", Instant.now()));
        repository.createUser(new AccessUser(superior, superior, "staff", "org_m002_f38_hq", null, "active", Instant.now()));
        repository.createUser(new AccessUser(otherOrg, otherOrg, "staff", "org_m002_f38_other", null, "active", Instant.now()));
        repository.assignUserRole(staff, "role_staff_self");
        jdbcTemplate.update("INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by) VALUES (?, ?, 'main', ?)",
                staff, superior, f.admin());
        String body = "{\"idempotency_key\": \"%s\", \"request_type\": \"user_position\", \"target_user_id\": \"%s\", \"position_code\": \"%s\", "
                + "\"staff_level_id\": \"lvl_m1_lower\", \"reason\": \"岗位申请\"}";

        mockMvc.perform(post("/api/v1/access/grant-requests").with(m002Session(new AccessActor(staff, staff, "staff", "org", null, "active")))
                        .contentType(MediaType.APPLICATION_JSON).content(body.formatted("m002-j38-peer-" + f.suffix(), f.target(), m.position())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/access/grant-requests")
                        .with(m002Session(new AccessActor(f.admin(), f.admin(), "platform_admin", "org", null, "active")))
                        .contentType(MediaType.APPLICATION_JSON).content(body.formatted("m002-j38-admin-" + f.suffix(), otherOrg, m.position())))
                .andExpect(status().isOk());
        String peer = m002PendingRequestId(f.target(), "user_position");
        String admin = m002PendingRequestId(otherOrg, "user_position");

        m002ActAs(superior, "staff", "org_m002_f38_hq", () -> accessService.reviewGrant(peer, m002Approve("m002-j38-peer-final-" + f.suffix())));
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(admin, m002Approve("m002-j38-admin-final-" + f.suffix())));
        for (String[] landed : List.of(new String[] {peer, f.target()}, new String[] {admin, otherOrg})) {
            assertThat(m002Status(landed[0])).as(landed[1]).isEqualTo("applied");
            assertThat(jdbcTemplate.queryForObject("SELECT position_code FROM user_position_assignments WHERE user_id = ?", String.class,
                    landed[1])).as(landed[1]).isEqualTo(m.position());
            assertThat(m002AssignedRole(landed[1])).as(landed[1]).isEqualTo(m.role());
        }
        m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code()));
    }

    // ── M002 第 12 轮（M002-F40、M002-F41）真库与真实接口回归 ─────────────────────────────────

    // M002-F40（治理地图第 655 行「显式拒绝优先级最高」，R-001「显式拒绝仍优先」）：真库＋真实接口——链上合法当前节点持主管复核与审批授权申请两码，
    // 但被员工显式拒绝「审核高危授权申请」：授权申请专用入口与通用主管复核入口的通过、驳回都 403 PERMISSION_DENIED（中文提示）；申请、复核单与节点、
    // 目标员工授权事实不变，拒绝行仍在。解除拒绝后：非当前节点、申请人、受益人经通用入口仍被拒；当前节点经通用入口批准落地；同一幂等键经两入口重放
    // 都 200，不重复审批、不重复落地。
    @Test
    void explicitlyDeniedGrantReviewerIsRefusedThroughBothReviewEndpointsOnTheRealSchema() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        String requestId = m002ApplyOverride(f, "user_permission_grant", "m002-j44");
        String reviewId = m002ReviewIdOf(requestId);
        AccessActor boss = m002ChainReviewer(f, f.boss(), "staff");
        repository.saveUserPermissionOverrides(f.boss(), List.of(GRANT_REVIEW), AccessRepository.Effect.DENY, "停审批授权申请", f.admin());
        String facts = m002F40Facts(requestId, f.target());
        assertThat(facts).startsWith("pending||pending_review|").contains("|1/user/pending/|");

        m002F40RefusedThroughBothEndpoints(boss, requestId, reviewId, "m002-j44-denied-" + f.suffix());
        assertThat(m002F40Facts(requestId, f.target())).isEqualTo(facts);
        assertThat(denyOrigins(f.boss(), GRANT_REVIEW)).containsExactly("explicit");

        repository.deleteExplicitDenies(f.boss(), List.of(GRANT_REVIEW));
        String outsider = "usr_m002_j44_out_" + f.suffix();
        repository.createUser(new AccessUser(outsider, outsider, "staff", "org", null, "active", Instant.now()));
        List<String[]> refused = List.of(new String[] {outsider, "staff", "SUPERVISOR_NOT_CURRENT_APPROVER"},
                new String[] {f.admin(), "platform_admin", "SUPERVISOR_SELF_REVIEW_FORBIDDEN"},
                new String[] {f.target(), "staff", "BENEFICIARY_SELF_REVIEW_FORBIDDEN"});
        List<AccessActor> sessions = refused.stream().map(row -> m002ChainReviewer(f, row[0], row[1])).toList();
        String beforeRefused = m002F40Facts(requestId, f.target()); // 受益人也已持两码（审批人角色记入目标事实）
        for (int i = 0; i < refused.size(); i++) {
            String code = refused.get(i)[2];
            m002ChainDecision(sessions.get(i), reviewId, "m002-j44-" + code.toLowerCase() + "-" + f.suffix(), "approved")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value(code));
        }
        assertThat(m002F40Facts(requestId, f.target())).isEqualTo(beforeRefused);

        String key = "m002-j44-approve-" + f.suffix();
        m002ChainDecision(boss, reviewId, key, "approved").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("approved"));
        String landed = m002F40Facts(requestId, f.target());
        assertThat(landed).startsWith("applied|" + f.boss() + "|approved|")
                .contains("|1/user/approved/" + f.boss() + "|", "override/" + f.code() + "/grant/explicit");
        m002ChainDecision(boss, reviewId, key, "approved").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("approved"));
        mockMvc.perform(post("/api/v1/access/grant-requests/" + requestId + "/review").with(m002Session(boss))
                        .contentType(MediaType.APPLICATION_JSON).content(m002F40DecisionJson(key, "approve")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("applied"));
        assertThat(m002F40Facts(requestId, f.target())).isEqualTo(landed);
    }

    // M002-F40：真库＋真实接口——最高权限本人裁决末节点（critical 码，链 user→master）：最高权限账号被显式拒绝「审核高危授权申请」时，两入口的通过、驳回
    // 都 403，末节点、申请与目标授权事实不变，拒绝行仍在；解除拒绝后经通用入口终审落地。
    @Test
    void explicitlyDeniedMasterAuthorityIsRefusedAtTheFinalNodeThroughBothReviewEndpointsOnTheRealSchema() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        String master = m002MasterAccount();
        jdbcTemplate.update("UPDATE permissions SET risk_level = 'critical' WHERE permission_code = ?", f.code());
        String requestId = m002ApplyOverride(f, "user_permission_grant", "m002-j45");
        String reviewId = m002ReviewIdOf(requestId);
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(requestId, m002Approve("m002-j45-boss-" + f.suffix())));
        repository.saveUserPermissionOverrides(master, List.of(GRANT_REVIEW), AccessRepository.Effect.DENY, "停审批授权申请", f.admin());
        String facts = m002F40Facts(requestId, f.target());
        assertThat(facts).startsWith("pending||pending_review|").contains("|1/user/approved/" + f.boss() + ",2/master/pending/|");
        AccessActor masterSession = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");

        m002F40RefusedThroughBothEndpoints(masterSession, requestId, reviewId, "m002-j45-denied-" + f.suffix());
        assertThat(m002F40Facts(requestId, f.target())).isEqualTo(facts);
        assertThat(denyOrigins(master, GRANT_REVIEW)).containsExactly("explicit");

        repository.deleteExplicitDenies(master, List.of(GRANT_REVIEW));
        m002ChainDecision(masterSession, reviewId, "m002-j45-master-" + f.suffix(), "approved").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("approved"));
        assertThat(m002F40Facts(requestId, f.target())).startsWith("applied|" + master + "|approved|")
                .contains(",2/master/approved/" + master + "|", "override/" + f.code() + "/grant/explicit");
    }

    // M002-F40（M006-014「两码先整体核验、同一来源只消耗一次」）：真库＋真实接口——审批人的「审核高危授权申请」只来自两条单次模板套用，「主管复核审批」
    // 来自角色：经授权申请页审批一笔、经通用主管复核入口审批另一笔，两笔都落地；每次审批恰好消耗一条单次来源、各留一条消耗日志，没有二次消耗、
    // 也没有不消耗。修复前通用入口不核验该码，第二笔落地后第二条单次来源仍未消耗。
    @Test
    void singleUseGrantReviewSourceIsConsumedOnceThroughEitherReviewEndpointOnTheRealSchema() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        String second = "m002.override_test.high2_" + f.suffix();
        jdbcTemplate.update("""
                INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
                VALUES (?, 'M002 测试高危权限二', 'ops', 'high', TRUE, 'active')
                """, second);
        String viaOwn = m002ApplyOverride(f, "user_permission_grant", "m002-j46");
        m002ActAs(f.admin(), "platform_admin", () -> accessService.applyGrant(new AccessGrantApplyRequest(
                "m002-j46-apply2-" + f.suffix(), "M002", List.of(), "user_permission_grant", f.target(), null, null, null, null, null, null,
                List.of(second), null, null, null, "员工覆盖授权申请")));
        String viaChain = jdbcTemplate.queryForObject("""
                SELECT id FROM access_grant_requests
                 WHERE target_user_id = ? AND request_type = 'user_permission_grant' AND status = 'pending' AND id <> ?
                """, String.class, f.target(), viaOwn);
        String approveRole = "role_m002_apv_" + f.suffix();
        repository.createRole(new RoleRecord(approveRole, "M002 主管复核审批 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of("governance.supervisor_review.approve"), null));
        repository.assignUserRole(f.boss(), approveRole);
        List<String> sources = new java.util.ArrayList<>();
        for (String n : List.of("a", "b")) {
            String template = "tpl_m002_once_" + n + "_" + f.suffix();
            repository.createTemplate(m002SingleUseTemplate(template), grants(template, GRANT_REVIEW));
            sources.add(repository.applyTemplate(template, List.of(f.boss()), null, null, true, f.admin()).get(0).id());
        }
        AccessActor boss = new AccessActor(f.boss(), f.boss(), "staff", "org", null, "active");
        assertThat(m002SourceUse(sources)).containsExactly("false/0", "false/0");

        mockMvc.perform(post("/api/v1/access/grant-requests/" + viaOwn + "/review").with(m002Session(boss))
                        .contentType(MediaType.APPLICATION_JSON).content(m002F40DecisionJson("m002-j46-own-" + f.suffix(), "approve")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("applied"));
        assertThat(m002SourceUse(sources)).containsExactly("false/0", "true/1");

        m002ChainDecision(boss, m002ReviewIdOf(viaChain), "m002-j46-chain-" + f.suffix(), "approved").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("approved"));
        assertThat(m002SourceUse(sources)).containsExactly("true/1", "true/1");
        assertThat(List.of(m002Status(viaOwn), m002Status(viaChain))).containsExactly("applied", "applied");
        assertThat(jdbcTemplate.queryForList("""
                SELECT permission_code FROM user_permission_overrides
                 WHERE user_id = ? AND effect = 'grant' AND origin = 'explicit' ORDER BY permission_code
                """, String.class, f.target())).containsExactlyInAnyOrder(f.code(), second);
    }

    // M723（治理地图第 656 行 J 句「同一次操作重复提交时，直接返回第一次的结果」）与 M002-F40 入口：审批人只经一条单次模板套用持有
    // 「审核高危授权申请」与「主管复核审批」两码，经通用主管复核入口通过末节点——来源消耗一次（一条消耗日志）、申请落地；同一请求编号、同一正文重放：
    // 复核单已不待审，入口改走通用分支，按重放放行，由节点裁决的幂等记录返回第一次的结果，不再消耗、不再执行。
    // 修复前重放在入口即 403（来源已消耗）。
    @Test
    void replayingAChainApprovalMadeWithASingleUseGrantReturnsTheFirstResultOnTheRealSchema() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        String requestId = m002ApplyOverride(f, "user_permission_grant", "m723-replay");
        String reviewId = m002ReviewIdOf(requestId);
        String template = "tpl_m723_once_" + f.suffix();
        repository.createTemplate(m002SingleUseTemplate(template), grants(template, GRANT_REVIEW, "governance.supervisor_review.approve"));
        List<String> sources = List.of(repository.applyTemplate(template, List.of(f.boss()), null, null, true, f.admin()).get(0).id());
        AccessActor boss = new AccessActor(f.boss(), f.boss(), "staff", "org", null, "active");
        String key = "m723-replay-chain-" + f.suffix();

        m002ChainDecision(boss, reviewId, key, "approved").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("approved"));
        assertThat(m002SourceUse(sources)).containsExactly("true/1");
        String facts = m002F40Facts(requestId, f.target());
        assertThat(facts).startsWith("applied|");

        m002ChainDecision(boss, reviewId, key, "approved").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_id").value(reviewId))
                .andExpect(jsonPath("$.data.review_status").value("approved"));
        assertThat(m002SourceUse(sources)).containsExactly("true/1");
        assertThat(m002F40Facts(requestId, f.target())).isEqualTo(facts);
    }

    // M002-F40（保持原有拒绝顺序）：真库＋真实接口——不持主管复核审批权限的人（只经单次模板套用持「审核高危授权申请」、属另一项目、不在审批链上）对范围外的
    // 授权申请类待审复核单调通用入口：403 PERMISSION_DENIED，与基线相同，不是 SUPERVISOR_REVIEW_SCOPE_FORBIDDEN（不暴露范围外复核单是否存在）；
    // 单次来源未被消耗，申请、复核单与目标授权事实不变。
    @Test
    void callerWithoutSupervisorApproveIsRefusedBeforeTheReviewIsReadOnTheRealSchema() throws Exception {
        M002Fixture f = m002ApprovalFixture();
        String requestId = m002ApplyOverride(f, "user_permission_grant", "m002-j47");
        String reviewId = m002ReviewIdOf(requestId);
        String outsider = "usr_m002_j47_far_" + f.suffix();
        repository.createUser(new AccessUser(outsider, outsider, "staff", "org_m002_j47_far", null, "active", Instant.now()));
        String template = "tpl_m002_once_j47_" + f.suffix();
        repository.createTemplate(m002SingleUseTemplate(template), grants(template, GRANT_REVIEW));
        List<String> sources = List.of(repository.applyTemplate(template, List.of(outsider), null, null, true, f.admin()).get(0).id());
        String facts = m002F40Facts(requestId, f.target());

        m002ChainDecision(new AccessActor(outsider, outsider, "staff", "org_m002_j47_far", null, "active"), reviewId,
                "m002-j47-chain-" + f.suffix(), "approved")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        assertThat(m002SourceUse(sources)).containsExactly("false/0");
        assertThat(m002F40Facts(requestId, f.target())).isEqualTo(facts);
    }

    // M002-F40：各单次来源的「是否已消耗 / 消耗日志条数」，排序后比较（不依赖先消耗哪一条）。
    private List<String> m002SourceUse(List<String> sources) {
        return sources.stream().map(id -> jdbcTemplate.queryForObject("""
                SELECT a.consumed::text || '/' || (SELECT count(*) FROM permission_template_usage_logs l
                                                     WHERE l.application_id = a.id AND l.result = 'consumed')::text
                  FROM permission_template_applications a WHERE a.id = ?
                """, String.class, id)).sorted().toList();
    }

    // M002-F40：给 userId 授「审核高危授权申请」与「主管复核审批」（通用入口控制器查后者），返回其会话主体。
    private AccessActor m002ChainReviewer(M002Fixture f, String userId, String accountType) {
        String role = "role_m002_rva_" + f.suffix();
        if (repository.findRoleById(role).isEmpty()) {
            repository.createRole(new RoleRecord(role, "M002 审批链审批人 " + f.suffix(), null, "project", "active", Instant.now(),
                    List.of(GRANT_REVIEW, "governance.supervisor_review.approve"), null));
        }
        repository.assignUserRole(userId, role);
        return new AccessActor(userId, userId, accountType, "org", null, "active");
    }

    // M002-F40：两入口的通过、驳回都 403 PERMISSION_DENIED，提示为中文「没有操作权限」。
    private void m002F40RefusedThroughBothEndpoints(AccessActor actor, String requestId, String reviewId, String key) throws Exception {
        for (String[] decision : List.of(new String[] {"approve", "approved"}, new String[] {"reject", "rejected"})) {
            mockMvc.perform(post("/api/v1/access/grant-requests/" + requestId + "/review").with(m002Session(actor))
                            .contentType(MediaType.APPLICATION_JSON).content(m002F40DecisionJson(key + "-own-" + decision[0], decision[0])))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("没有操作权限")));
            m002ChainDecision(actor, reviewId, key + "-chain-" + decision[0], decision[1])
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("没有操作权限")));
        }
    }

    // M002-F40：通用主管复核入口 POST /api/v1/platform/supervisor-review/{review_id}/approve（审批链页、主管复核页）。
    private org.springframework.test.web.servlet.ResultActions m002ChainDecision(AccessActor actor, String reviewId, String key, String decision)
            throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + reviewId + "/approve").with(m002Session(actor))
                .contentType(MediaType.APPLICATION_JSON).content(m002F40DecisionJson(key, decision)));
    }

    private String m002F40DecisionJson(String key, String decision) {
        return """
                {"idempotency_key": "%s", "operator_note": "M002", "evidence_attachment_ids": [], "decision": "%s", "comment": "同意"}
                """.formatted(key, decision);
    }

    private String m002ReviewIdOf(String requestId) {
        return jdbcTemplate.queryForObject("SELECT review_id FROM access_grant_requests WHERE id = ?", String.class, requestId);
    }

    // M002-F40：申请（状态、决定人）、复核单（状态、结论）、各节点（序号/类型/状态/决定人）与目标员工授权事实拼成一段文本，比对前后。
    private String m002F40Facts(String requestId, String targetUserId) {
        return jdbcTemplate.queryForObject("""
                SELECT r.status || '|' || coalesce(r.decided_by, '') || '|' || d.review_status || '|' || coalesce(d.decision, '') || '|'
                       || (SELECT string_agg(n.seq::text || '/' || n.approver_kind || '/' || n.status || '/' || coalesce(n.decided_by, ''), ','
                                             ORDER BY n.seq)
                             FROM supervisor_review_nodes n WHERE n.review_id = r.review_id) || '|'
                  FROM access_grant_requests r JOIN supervisor_review_decisions d ON d.review_id = r.review_id
                 WHERE r.id = ?
                """, String.class, requestId) + m002TargetFacts(targetUserId);
    }

    // M002-F41（治理地图「系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效」，R-002）：真库真实并发（数据真实提交，结束后清理）——
    // 换岗事务锁住员工行、改岗未提交时另一事务改职级：改职级在员工行锁上等待，换岗提交后按锁内读到的新岗位重算——岗位编码不被写回旧值，两表职级一致，
    // 派生角色与模板对应新岗位与新职级。修复前改职级先无锁读到旧岗位并据此分支，换岗提交后把岗位写回旧编码。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void staffLevelChangeWaitsForAConcurrentMoveAndKeepsTheNewPositionOnTheRealSchema() throws Exception {
        M002F41 w = m002F41Fixture();
        try {
            m002F41Seat(w, w.oldPosition(), "f41-seat-");
            m002F41Race(w);
            m002F41ExpectSeated(w);
        } finally {
            m002F41Cleanup(w);
        }
    }

    // M002-F41：真库真实并发——员工原本无岗位，定岗事务锁住员工行、未提交时另一事务改职级：改职级在员工行锁上等待，定岗提交后在锁内读到新岗位、
    // 走定岗同路径改到新职级——两表职级一致，派生角色与模板对应新岗位与新职级。修复前改职级无锁读到「无岗位」、只写职级表，两表职级分叉。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void staffLevelChangeOfAnUnseatedEmployeeWaitsForAConcurrentSeatOnTheRealSchema() throws Exception {
        M002F41 w = m002F41Fixture();
        try {
            m002F41Race(w);
            m002F41ExpectSeated(w);
        } finally {
            m002F41Cleanup(w);
        }
    }

    private record M002F41(String suffix, String admin, String user, String oldPosition, String newPosition) {
    }

    private static final List<String> M002_F41_LEVELS = List.of("lvl_m1_lower", "lvl_m10_upper");

    // M002-F41：两个岗位 × 两档职级各一张全局映射（空角色、空模板，每张映射各自的角色与模板，派生结果即可看出员工挂在哪张映射上）。
    private M002F41 m002F41Fixture() {
        String s = UUID.randomUUID().toString().substring(0, 8);
        M002F41 w = new M002F41(s, "usr_m002_f41_admin_" + s, "usr_m002_f41_" + s, "f41o_" + s, "f41n_" + s);
        repository.createUser(new AccessUser(w.admin(), w.admin(), "platform_admin", "org", null, "active", Instant.now()));
        repository.createUser(new AccessUser(w.user(), w.user(), "staff", "org", null, "active", Instant.now()));
        for (String position : List.of(w.oldPosition(), w.newPosition())) {
            for (String level : M002_F41_LEVELS) {
                String id = m002F41Id(position, level);
                repository.createRole(new RoleRecord("role_" + id, "M002 F41 " + id, null, "project", "active", Instant.now(), List.of(), null));
                repository.createTemplate(baseTemplate("tpl_" + id), List.of());
                repository.savePositionRoleTemplate(new PositionRoleTemplate("prt_" + id, position, level, "role_" + id, "tpl_" + id,
                        null, "active", w.admin(), Instant.now(), Instant.now()));
            }
        }
        return w;
    }

    private static String m002F41Id(String position, String level) {
        return position + "_" + level.substring(4, level.indexOf('_', 4));
    }

    private void m002F41Seat(M002F41 w, String position, String key) {
        m002ActAs(w.admin(), "platform_admin", () -> accessService.assignUserPosition(new UserPositionAssignmentRequest(
                key + w.suffix(), "M002", w.user(), position, "lvl_m1_lower", null, null, null)));
    }

    // 主线程事务内把员工定岗 / 换岗到新岗位 m1（持员工行锁、未提交），另一线程同时把该员工改到 m10；等它在锁上等待（或已结束）后提交主事务。
    private void m002F41Race(M002F41 w) throws Exception {
        ExecutorService other = Executors.newSingleThreadExecutor();
        try {
            Future<Throwable> level = new TransactionTemplate(transactionManager).execute(status -> {
                m002F41Seat(w, w.newPosition(), "f41-race-");
                Future<Throwable> pending = other.submit(() -> {
                    try {
                        m002ActAs(w.admin(), "platform_admin", () -> accessService.assignStaffLevel(new StaffLevelAssignmentRequest(
                                "f41-level-" + w.suffix(), "M002", w.user(), "lvl_m10_upper", null, null, null)));
                        return null;
                    } catch (Throwable e) {
                        return e;
                    }
                });
                awaitWaiterOrDone(pending);
                return pending;
            });
            assertThat(level.get(30, TimeUnit.SECONDS)).isNull();
        } finally {
            other.shutdownNow();
        }
    }

    private void m002F41ExpectSeated(M002F41 w) {
        String id = m002F41Id(w.newPosition(), "lvl_m10_upper");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT p.position_code || '/' || p.staff_level_id || '/' || p.assigned_role_id || '/' || p.assigned_position_template_id
                       || '/' || s.staff_level_id
                  FROM user_position_assignments p JOIN staff_level_assignments s ON s.user_id = p.user_id
                 WHERE p.user_id = ?
                """, String.class, w.user()))
                .isEqualTo(w.newPosition() + "/lvl_m10_upper/role_" + id + "/prt_" + id + "/lvl_m10_upper");
        assertThat(jdbcTemplate.queryForList(
                "SELECT template_id FROM permission_template_applications WHERE target_id = ? AND status = 'active'", String.class, w.user()))
                .containsExactly("tpl_" + id);
    }

    private void m002F41Cleanup(M002F41 w) {
        jdbcTemplate.update("DELETE FROM user_position_assignments WHERE user_id = ?", w.user());
        jdbcTemplate.update("DELETE FROM permission_template_applications WHERE target_id = ?", w.user());
        for (String position : List.of(w.oldPosition(), w.newPosition())) {
            for (String level : M002_F41_LEVELS) {
                String id = m002F41Id(position, level);
                jdbcTemplate.update("DELETE FROM position_role_templates WHERE id = ?", "prt_" + id);
                jdbcTemplate.update("DELETE FROM roles WHERE id = ?", "role_" + id);
                jdbcTemplate.update("DELETE FROM permission_templates WHERE id = ?", "tpl_" + id);
            }
        }
        jdbcTemplate.update("DELETE FROM access_users WHERE id IN (?, ?)", w.user(), w.admin());
    }

    // ── M002 第 13 轮（M002-F42；治理地图第 651 行「只能由具备分配/取消分配角色权限的人发起」、第 656 行「临时授权可设有效期或单次使用」，
    // 操作员裁决 M002-F37；同一操作先消耗一项必需权限再重查另一项致误拒为技术推导）真库与真实接口回归 ─────────────────────────────────

    private static final List<String> M002_F42_TYPES = List.of("user_permission_grant", "user_permission_deny_removal", "position_high_risk_release");
    private static final String M002_F37_MESSAGE =
            "员工单独授予、解除员工显式拒绝、岗位模板高危权限释放这三类授权申请，只能由具备分配/取消分配角色权限的人发起";

    // M002-F42：真库＋真实接口＋真实 AuthorizationService——申请人的「申请高危授权」与「分配/取消分配角色」只来自同一条有效单次模板套用（没有其他来源），
    // 三类申请各由一名这样的员工首次提交：都 200 建单，该单次来源恰好消耗一次、一条消耗日志；每类恰好一张待审授权单、对应一张待审主管复核单，
    // 复核单总数只增 3。修复前入口先核验并消耗申请权限的来源，服务层再按有效集查分配角色权限时该来源已被排除，403、来源已用掉。
    @Test
    void singleUseApplyAndRoleAssignSourceFilesEachOfTheThreeRequestTypesOnceThroughTheEndpointOnTheRealSchema() throws Exception {
        M002Fixture f = m002F42ThreeTypeFixture("m002-j42a");
        String countReviews = "SELECT count(*) FROM supervisor_review_decisions";
        long reviewsBefore = jdbcTemplate.queryForObject(countReviews, Long.class);
        int n = 0;
        for (String type : M002_F42_TYPES) {
            M002F42Applicant applicant = m002F42Applicant(f, "a" + (++n), "access.grant_request.apply", "access.role.assign");
            assertThat(m002SourceUse(applicant.sources())).containsExactly("false/0");

            m002F42Apply(f, applicant, type, "m002-j42a-" + n).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.request_type").value(type))
                    .andExpect(jsonPath("$.data.status").value("pending"));
            assertThat(m002SourceUse(applicant.sources())).as(type).containsExactly("true/1");
            assertThat(jdbcTemplate.queryForList("""
                    SELECT r.status || '/' || d.review_status || '/'
                           || (SELECT count(*) FROM supervisor_review_decisions x WHERE x.object_id = r.id)::text
                      FROM access_grant_requests r JOIN supervisor_review_decisions d ON d.review_id = r.review_id
                     WHERE r.requested_by = ? AND r.request_type = ?
                    """, String.class, applicant.id(), type)).as(type).containsExactly("pending/pending_review/1");
        }
        assertThat(jdbcTemplate.queryForObject(countReviews, Long.class)).isEqualTo(reviewsBefore + 3);
    }

    // M002-F42：真库＋真实接口——分配角色权限缺失或被显式拒绝时三类申请仍 403（F37 原提示语），且不消耗任何单次来源：甲只经单次模板持申请权限；
    // 乙的单次模板含两码，但「分配/取消分配角色」有员工显式拒绝。两人对三类申请（另加带大小写与首尾空白的类型，不能绕过）都 403 PERMISSION_DENIED，
    // 第二轮仍被拒；来源始终未消耗、无消耗日志，不建授权单、不开复核单，拒绝行仍在。修复前入口先消耗来源、服务层再拒绝，来源白白用掉。
    @Test
    void missingOrDeniedRoleAssignIsRefusedWithoutConsumingTheSingleUseSourceOnTheRealSchema() throws Exception {
        M002Fixture f = m002F42ThreeTypeFixture("m002-j42b");
        M002F42Applicant lacking = m002F42Applicant(f, "b1", "access.grant_request.apply");
        M002F42Applicant denied = m002F42Applicant(f, "b2", "access.grant_request.apply", "access.role.assign");
        repository.saveUserPermissionOverrides(denied.id(), List.of("access.role.assign"), AccessRepository.Effect.DENY, "停分配角色", f.admin());
        String countRequests = "SELECT count(*) FROM access_grant_requests WHERE target_user_id = ?";
        String countReviews = "SELECT (SELECT count(*) FROM supervisor_review_decisions) || '/' || (SELECT count(*) FROM supervisor_review_nodes)";
        long requestsBefore = jdbcTemplate.queryForObject(countRequests, Long.class, f.target());
        String reviewsBefore = jdbcTemplate.queryForObject(countReviews, String.class);
        List<String> types = new java.util.ArrayList<>(M002_F42_TYPES);
        types.addAll(List.of(" User_Permission_GRANT ", "Position_High_Risk_Release  "));
        int n = 0;
        for (M002F42Applicant applicant : List.of(lacking, denied)) {
            for (int round = 1; round <= 2; round++) {
                for (String type : types) {
                    m002F42Apply(f, applicant, type, "m002-j42b-" + (++n)).andExpect(status().isForbidden())
                            .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"))
                            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(M002_F37_MESSAGE)));
                    assertThat(m002SourceUse(applicant.sources())).as(applicant.id() + " " + type).containsExactly("false/0");
                }
            }
        }
        assertThat(jdbcTemplate.queryForObject(countRequests, Long.class, f.target())).isEqualTo(requestsBefore);
        assertThat(jdbcTemplate.queryForObject(countReviews, String.class)).isEqualTo(reviewsBefore);
        assertThat(denyOrigins(denied.id(), "access.role.assign")).containsExactly("explicit");
    }

    // M002-F42（行为保持）：真库＋真实接口——非三类与不持申请权限者不变：丙只经单次模板持申请权限，提交本人临时权限申请 200，该来源恰好消耗一次
    // （不判分配角色权限）；丁只经单次模板持分配角色权限、不持申请权限，三类申请都 403 PERMISSION_DENIED「没有操作权限」（与基线同一标准拒绝），
    // 其来源不消耗，不建授权单。
    @Test
    void temporaryRequestsAndCallersWithoutApplyKeepTheirBaselineBehaviourOnTheRealSchema() throws Exception {
        M002Fixture f = m002F42ThreeTypeFixture("m002-j42c");
        M002F42Applicant temporary = m002F42Applicant(f, "c1", "access.grant_request.apply");
        mockMvc.perform(post("/api/v1/access/grant-requests").with(m002Session(temporary.actor())).contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"idempotency_key\": \"m002-j42c-temporary-%s\", \"request_type\": \"temporary_permission\", "
                                + "\"target_user_id\": \"%s\", \"permission_codes\": [\"amusement.device.view\"], "
                                + "\"expires_at\": \"2099-01-01T00:00:00Z\", \"reason\": \"临时顶班\"}").formatted(f.suffix(), temporary.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"));
        assertThat(m002SourceUse(temporary.sources())).containsExactly("true/1");
        assertThat(m002PendingRequestId(temporary.id(), "temporary_permission")).isNotNull();

        M002F42Applicant withoutApply = m002F42Applicant(f, "c2", "access.role.assign");
        String countRequests = "SELECT count(*) FROM access_grant_requests WHERE target_user_id = ?";
        long requestsBefore = jdbcTemplate.queryForObject(countRequests, Long.class, f.target());
        int n = 0;
        for (String type : M002_F42_TYPES) {
            m002F42Apply(f, withoutApply, type, "m002-j42c-" + (++n)).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("没有操作权限")));
        }
        assertThat(m002SourceUse(withoutApply.sources())).containsExactly("false/0");
        assertThat(jdbcTemplate.queryForObject(countRequests, Long.class, f.target())).isEqualTo(requestsBefore);
    }

    private record M002F42Applicant(String id, List<String> sources) {
        AccessActor actor() {
            return new AccessActor(id, id, "staff", "org", null, "active");
        }
    }

    // M002-F42：三类申请内容均有效的目标（同 F37 真库用例）：定岗形成岗位高危挂起、首张释放单已驳回（可再次申请释放），另有一条员工显式拒绝。
    private M002Fixture m002F42ThreeTypeFixture(String key) {
        M002Fixture f = m002ApprovalFixture();
        M002Mapping m = m002HighRiskMapping(f);
        m002Assign(f, m.position(), key + "-assign");
        String first = m002PendingRequestId(f.target(), "position_high_risk_release");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(first, m002Reject(key + "-reject-" + f.suffix())));
        repository.saveUserPermissionOverrides(f.target(), List.of(f.code()), AccessRepository.Effect.DENY, "员工显式拒绝", f.admin());
        return f;
    }

    // M002-F42：同项目员工（无角色，上级为夹具审批人），所列权限只来自给它套用的一条单次模板（断言来源唯一）。
    private M002F42Applicant m002F42Applicant(M002Fixture f, String tag, String... codes) {
        String applicant = "usr_m002_f42_" + tag + "_" + f.suffix();
        repository.createUser(new AccessUser(applicant, applicant, "staff", "org", null, "active", Instant.now()));
        jdbcTemplate.update("INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by) VALUES (?, ?, 'main', ?)",
                applicant, f.boss(), f.admin());
        String template = "tpl_m002_f42_" + tag + "_" + f.suffix();
        repository.createTemplate(m002SingleUseTemplate(template), grants(template, codes));
        String source = repository.applyTemplate(template, List.of(applicant), null, null, true, f.admin()).get(0).id();
        assertThat(repository.findPermissionEffects(applicant, Instant.now()))
                .filteredOn(effect -> List.of(codes).contains(effect.permissionCode()))
                .extracting(effect -> effect.permissionCode() + "@" + effect.sourceId())
                .containsExactlyInAnyOrder(java.util.Arrays.stream(codes).map(code -> code + "@" + source).toArray(String[]::new));
        return new M002F42Applicant(applicant, List.of(source));
    }

    private org.springframework.test.web.servlet.ResultActions m002F42Apply(M002Fixture f, M002F42Applicant applicant, String type, String key)
            throws Exception {
        return mockMvc.perform(post("/api/v1/access/grant-requests").with(m002Session(applicant.actor())).contentType(MediaType.APPLICATION_JSON)
                .content(("{\"idempotency_key\": \"%s-%s\", \"request_type\": \"%s\", \"target_user_id\": \"%s\", \"permission_codes\": [\"%s\"], "
                        + "\"reason\": \"M002\"}").formatted(key, f.suffix(), type, f.target(), f.code())));
    }

    // M002-F38：目标员工的岗位行（岗位、职级、派生角色、所挂映射）、职级与薪资行、直接角色、模板套用与覆盖行（含挂起），逐行拼成文本整体比较。
    private List<String> m002TargetFacts(String userId) {
        return jdbcTemplate.queryForList("""
                SELECT 'position/' || position_code || '/' || staff_level_id || '/' || coalesce(assigned_role_id, '') || '/'
                       || coalesce(assigned_position_template_id, '') || '/' || assigned_at
                  FROM user_position_assignments WHERE user_id = ?
                UNION ALL
                SELECT 'level/' || staff_level_id || '/' || coalesce(base_salary_amount_cent::text, '') || '/' || assigned_at
                  FROM staff_level_assignments WHERE user_id = ?
                UNION ALL
                SELECT 'role/' || role_id FROM user_roles WHERE user_id = ?
                UNION ALL
                SELECT 'application/' || id || '/' || template_id || '/' || status FROM permission_template_applications WHERE target_id = ?
                UNION ALL
                SELECT 'override/' || permission_code || '/' || effect || '/' || origin FROM user_permission_overrides WHERE user_id = ?
                ORDER BY 1
                """, String.class, userId, userId, userId, userId, userId);
    }

    // 以平台管理员会话读目标员工的有效权限（有效集查询需要登录主体）。
    private List<String> m002EffectiveGrants(M002Fixture f) {
        List<List<String>> grants = new java.util.ArrayList<>();
        m002ActAs(f.admin(), "platform_admin", () -> grants.add(authorizationService.effectivePermissionsForUser(f.target()).grants()));
        return grants.get(0);
    }

    // M002：只含一个空角色、空模板的岗位映射（不派生任何须审批码），返回岗位编码。
    private String m002PlainMapping(M002Fixture f) {
        String position = "m002_ppos_" + f.suffix();
        String role = "role_m002_p_" + f.suffix();
        String template = "tpl_m002_p_" + f.suffix();
        repository.createRole(new RoleRecord(role, "M002 普通岗位角色 " + f.suffix(), null, "project", "active", Instant.now(), List.of(), null));
        repository.createTemplate(baseTemplate(template), List.of());
        repository.savePositionRoleTemplate(new PositionRoleTemplate("prt_m002_p_" + f.suffix(), position, "lvl_m1_lower", role, template,
                null, "active", f.admin(), Instant.now(), Instant.now()));
        return position;
    }

    private long m002ReleaseCount(String userId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM access_grant_requests WHERE target_user_id = ? AND request_type = 'position_high_risk_release'",
                Long.class, userId);
    }

    private String m002ReviewId(String requestId) {
        return jdbcTemplate.queryForObject("SELECT review_id FROM access_grant_requests WHERE id = ?", String.class, requestId);
    }

    private String m002ReviewStatus(String reviewId) {
        return jdbcTemplate.queryForObject("SELECT review_status FROM supervisor_review_decisions WHERE review_id = ?", String.class, reviewId);
    }

    private long m002PendingNodeCount(String reviewId) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM supervisor_review_nodes WHERE review_id = ? AND status = 'pending'",
                Long.class, reviewId);
    }

    private String m002ApplicationStatus(String applicationId) {
        return jdbcTemplate.queryForObject("SELECT status FROM permission_template_applications WHERE id = ?", String.class, applicationId);
    }

    private String m002AssignedMapping(String userId) {
        return jdbcTemplate.queryForObject("SELECT assigned_position_template_id FROM user_position_assignments WHERE user_id = ?",
                String.class, userId);
    }

    private String m002MasterAccount() {
        jdbcTemplate.update("""
                INSERT INTO access_users (id, username, account_type, organization_id, status, created_at)
                VALUES (?, ?, ?, 'platform-company', 'active', now())
                ON CONFLICT (id) DO UPDATE SET username = EXCLUDED.username, account_type = EXCLUDED.account_type, status = 'active'
                """, SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE);
        return SessionIssuer.MASTER_USER_ID;
    }

    private record M002Mapping(String id, String position, String role, String template) {
    }

    // M002：真库高危岗位映射（组织为空的全局映射，与接口保存口径一致）：m002_gpos_<后缀> / lvl_m1_lower → 含测试高危码的角色 + 空模板。
    private M002Mapping m002HighRiskMapping(M002Fixture f) {
        M002Mapping m = new M002Mapping("prt_m002_g_" + f.suffix(), "m002_gpos_" + f.suffix(), "role_m002_g_" + f.suffix(),
                "tpl_m002_g_" + f.suffix());
        repository.createRole(new RoleRecord(m.role(), "M002 岗位角色 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of(f.code()), null));
        repository.createTemplate(baseTemplate(m.template()), List.of());
        repository.savePositionRoleTemplate(new PositionRoleTemplate(m.id(), m.position(), "lvl_m1_lower", m.role(), m.template(),
                null, "active", f.admin(), Instant.now(), Instant.now()));
        return m;
    }

    private void m002Assign(M002Fixture f, String position, String key) {
        m002ActAs(f.admin(), "platform_admin", () -> accessService.assignUserPosition(new UserPositionAssignmentRequest(
                key + "-" + f.suffix(), "M002", f.target(), position, "lvl_m1_lower", null, null, null)));
    }

    private PositionRoleTemplateRequest m002MappingRequest(String key, M002Mapping m, String status) {
        return new PositionRoleTemplateRequest(key, "M002", m.position(), "lvl_m1_lower", m.role(), m.template(), status);
    }

    private long m002PendingReleaseCount(String userId) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM access_grant_requests
                 WHERE target_user_id = ? AND request_type = 'position_high_risk_release' AND status = 'pending'
                """, Long.class, userId);
    }

    private String m002Status(String requestId) {
        return jdbcTemplate.queryForObject("SELECT status FROM access_grant_requests WHERE id = ?", String.class, requestId);
    }

    private String m002AssignedRole(String userId) {
        return jdbcTemplate.queryForObject("SELECT assigned_role_id FROM user_position_assignments WHERE user_id = ?", String.class, userId);
    }

    private AccessGrantReviewRequest m002Reject(String key) {
        return new AccessGrantReviewRequest(key, "M002", List.of(), "reject", "驳回");
    }

    // 重放迁移真源文件（不是手抄副本）；surefire 的 cwd 是 backend/，迁移文件在测试类路径上。
    private void m002ReplayMigration(String file) throws java.io.IOException {
        try (var in = AccessJdbcIntegrationTests.class.getResourceAsStream("/db/migration/" + file)) {
            assertThat(in).as(file + " must be on classpath").isNotNull();
            jdbcTemplate.execute(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    // 并发用例提交了夹具数据：按外键顺序删掉本用例写入的行（审计事件与主管复核单只追加、不删；岗位、职级、覆盖、上级关系随员工级联删除）。
    private void m002DeleteCommitted(M002Fixture f, List<String> mappings, List<String> roles, List<String> templates) {
        for (String user : List.of(f.target(), f.boss(), f.admin())) {
            for (String sql : List.of(
                    "DELETE FROM access_grant_requests WHERE target_user_id = ?",
                    "DELETE FROM permission_template_applications WHERE target_id = ?",
                    "DELETE FROM access_users WHERE id = ?")) {
                jdbcTemplate.update(sql, user);
            }
        }
        mappings.forEach(id -> jdbcTemplate.update("DELETE FROM position_role_templates WHERE id = ?", id));
        templates.forEach(id -> jdbcTemplate.update("DELETE FROM permission_templates WHERE id = ?", id));
        for (String role : roles) {
            jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id = ?", role);
            jdbcTemplate.update("DELETE FROM roles WHERE id = ?", role);
        }
        jdbcTemplate.update("DELETE FROM permissions WHERE permission_code = ?", f.code());
    }

    // 映射列为空的岗位行：{员工, 岗位, 组织, 派生角色, 已记映射}；员工不存在先建。
    private void m002LegacyPositionRow(M002Fixture f, String[] row) {
        if (repository.findUserById(row[0]).isEmpty()) {
            repository.createUser(new AccessUser(row[0], row[0], "staff", row[2], null, "active", Instant.now()));
        }
        jdbcTemplate.update("""
                INSERT INTO user_position_assignments (user_id, position_code, staff_level_id, organization_id, assigned_by, assigned_at,
                                                       assigned_role_id, assigned_position_template_id)
                VALUES (?, ?, 'lvl_m1_lower', ?, ?, now(), ?, ?)
                """, row[0], row[1], row[2], f.admin(), row[3], row[4]);
    }

    // M087-I7-02（PGM-1.10.1「超出新模板范围的增权保持未生效」）：范围变更冻结单独模板授予的仓储写路径在真实 schema 上可执行且幂等。
    @Test
    void revokeTemplateApplicationsMarksOnlyTheGivenActiveApplicationsRevoked() {
        repository.createUser(new AccessUser(
                "usr_jdbc_tpl_freeze", "jdbc-tpl-freeze", "staff", "org", "merchant", "active", Instant.parse("2026-06-14T00:00:00Z")));
        repository.createTemplate(
                template("tpl_freeze", "freeze", null),
                List.of(item("tpl_freeze", "ops.production_readiness.check", AccessRepository.Effect.GRANT))
        );
        List<String> ids = repository.applyTemplate("tpl_freeze", List.of("usr_jdbc_tpl_freeze"), null, null, false, "seed-admin")
                .stream().map(PermissionTemplateApplication::id).toList();
        assertThat(repository.revokeTemplateApplications("usr_jdbc_tpl_freeze", ids, "seed-admin", Instant.parse("2026-06-16T00:00:00Z"))).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM permission_template_applications WHERE id = ?", String.class, ids.get(0))).isEqualTo("revoked");
        assertThat(repository.revokeTemplateApplications("usr_jdbc_tpl_freeze", ids, "seed-admin", Instant.parse("2026-06-16T00:00:00Z"))).isZero();
        assertThat(repository.findPermissionEffects("usr_jdbc_tpl_freeze", Instant.parse("2026-06-16T00:00:00Z")))
                .extracting(AccessRepository.PermissionEffect::permissionCode).doesNotContain("ops.production_readiness.check");
    }

    // M006-R6-01（PGM-1.10.1「敏感经营数据的查看 / 导出 / 修改」授出前审批、「查看内部结算价……不因平台管理员身份自动获得」；BSM-1.3.4）：
    // 含内部结算价 / 成本价的四个权限码在权威 schema 上须为 requires_approval=TRUE，且不进入平台管理员（非主会话）的日常桥。
    @Test
    void sensitiveBusinessDataPermissionsRequireApprovalAndStayOutOfThePlatformAdminBridge() {
        List<String> sensitive = List.of("bar.combo.view", "bar.combo.manage", "inventory.cost.view", "voucher.internal_price.view");
        assertThat(repository.listPermissions()).filteredOn(p -> sensitive.contains(p.permissionCode()))
                .hasSize(4).allMatch(PermissionDefinition::requiresApproval);
        authenticate("usr_jdbc_admin_bridge", "jdbc-admin-bridge", "platform_admin", "org");
        assertThat(authorizationService.effectivePermissions().grants()).doesNotContainAnyElementsOf(sensitive);
    }

    // M006-I8-01 / F8-01（BSM-1.3.4「拥有系统里全部日常功能权限…定为日常功能的会自动同步给他，不会出现"管理员反而没权限"的情况」）：
    // 字典只允许四个风险档（种子阶段即拒绝"半定类"）；按日常意图种下的只读/作业码与「申请高危授权」经平台管理员日常桥可见、可通过 requirePermission；
    // PGM-1.10.1「员工可申请临时权限」：员工自助角色持有申请权限。
    @Test
    void dailyCodesReachThePlatformAdminBridgeAndRiskTiersAreTheFourGovernedOnes() {
        assertThat(jdbcTemplate.queryForList(
                "SELECT permission_code FROM permissions WHERE risk_level NOT IN ('low', 'medium', 'high', 'critical') ORDER BY 1",
                String.class)).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_constraint WHERE conrelid = 'permissions'::regclass AND conname = 'chk_permissions_risk_level'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList(
                "SELECT permission_code FROM role_permissions WHERE role_id = 'role_staff_self' AND permission_code = 'access.grant_request.apply'",
                String.class)).hasSize(1);
        List<String> daily = List.of("amusement.device.view", "inventory.stocktake.view", "inventory.trace_code.view", "access.grant_request.apply");
        authenticate("usr_jdbc_admin_daily", "jdbc-admin-daily", "platform_admin", "org");
        assertThat(authorizationService.effectivePermissions().grants()).containsAll(daily);
        daily.forEach(authorizationService::requirePermission);
    }

    // M006-I8-02（PGM-1.10.1「以下五类权限授出前须先审批才生效——资金/核销/退款/调账/分红发放类…敏感经营数据的查看/导出/修改…」；
    // BSM-1.3.4「敏感数据查看与导出（…企业微信会话原文、客人与员工个人信息）…不因"平台管理员"这个身份自动获得」）：
    // 落在五类内的高危码须标记授出审批，且不进平台管理员日常桥。
    @Test
    void fiveCategoryHighRiskCodesRequireApprovalBeforeGrant() {
        List<String> sensitive = List.of("analytics.group.view", "customer.voucher.manage", "finance.merchant_expense.manage",
                "pii.contact_phone.view", "wecom.sync");
        assertThat(repository.listPermissions()).filteredOn(p -> sensitive.contains(p.permissionCode()))
                .hasSize(5).allMatch(PermissionDefinition::requiresApproval);
        authenticate("usr_jdbc_admin_bridge2", "jdbc-admin-bridge2", "platform_admin", "org");
        assertThat(authorizationService.effectivePermissions().grants()).doesNotContainAnyElementsOf(sensitive);
    }

    // M006-I9-01（BSM-1.3.4；PGM-1.10.1 角色装配）：真实字典 + role_platform_admin 种子下，平台管理员持有治理写码，不持有资金 / 敏感高危码。
    @Test
    void platformAdminRoleSeedYieldsGovernanceWriteCodesButNotBusinessHighRisk() {
        repository.createUser(new AccessUser(
                "usr_jdbc_admin_gov", "jdbc-admin-gov", "platform_admin", "org", null, "active", Instant.parse("2026-06-14T00:00:00Z")));
        repository.assignUserRole("usr_jdbc_admin_gov", "role_platform_admin");
        authenticate("usr_jdbc_admin_gov", "jdbc-admin-gov", "platform_admin", "org");
        List<String> govern = List.of("access.role.assign", "access.permission_template.manage", "access.staff_level.manage");
        assertThat(authorizationService.effectivePermissions().grants()).containsAll(govern)
                .doesNotContain("finance.dividend.manage", "bar.internal_settlement_price.view", "access.grant_request.review");
        govern.forEach(authorizationService::requirePermission);
    }

    // M001-F11：模板当前仍生效的套用记录——撤销的、已单次消耗的、已过到期时间的不算。
    @Test
    void findActiveTemplateApplicationsSkipsRevokedConsumedAndLapsedOnes() {
        List<String> users = List.of("usr_f11_active", "usr_f11_revoked", "usr_f11_consumed", "usr_f11_lapsed");
        users.forEach(id -> repository.createUser(new AccessUser(
                id, id, "staff", "org", null, "active", Instant.parse("2026-06-14T00:00:00Z"))));
        // M001-F17 起持有人查询按「套用当时截止与模板当前截止中较早的一个」判断：原夹具 template() 的模板窗口止于 2026-06-15（已过去），
        // 四人按统一口径都已到期（有效权限查询与到期作业原本就这样算），故模板改用到期在将来的临时模板，本用例原意不变。
        repository.createTemplate(temporaryTemplate("tpl_f11_active", null, Instant.parse("2099-01-31T00:00:00Z"), true),
                List.of(item("tpl_f11_active", "ops.production_readiness.check", AccessRepository.Effect.GRANT)));
        List<PermissionTemplateApplication> applied = repository.applyTemplate("tpl_f11_active", users, null, null, true, "seed-admin");
        repository.revokeTemplateApplications("usr_f11_revoked", List.of(applied.get(1).id()), "seed-admin", Instant.parse("2026-06-16T00:00:00Z"));
        jdbcTemplate.update("UPDATE permission_template_applications SET consumed = TRUE WHERE id = ?", applied.get(2).id());
        jdbcTemplate.update("UPDATE permission_template_applications SET valid_until = now() - interval '1 hour' WHERE id = ?", applied.get(3).id());

        assertThat(repository.findActiveTemplateApplications("tpl_f11_active"))
                .extracting(PermissionTemplateApplication::targetId).containsExactly("usr_f11_active");
    }

    // M001-F10（PGM #permission「旧岗位或旧范围派生权限立即失效」）：换项目事务锁住员工行并改归属期间，同时对他套用原项目模板的事务
    // 须等锁释放、按新归属被拒；换项目提交后该员工不留旧项目模板授权。真实并发、数据真实提交，结束后清理。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void templateApplyWaitsForAConcurrentScopeChangeAndIsRejectedByTheNewProject() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String userId = "usr_f10_" + suffix;
        String templateId = "tpl_f10_" + suffix;
        String oldProject = "org_f10_old_" + suffix;
        Instant now = Instant.now();
        repository.createUser(new AccessUser(userId, "f10-" + suffix, "staff", oldProject, null, "active", now));
        repository.createTemplate(new PermissionTemplate(templateId, templateId, "F10 template", "f10", "base", null,
                "organization", oldProject, "merge", null, null, false, "active", "f10", "seed-admin", now, now, now), List.of());
        ExecutorService applier = Executors.newSingleThreadExecutor();
        try {
            Future<Throwable> apply = new TransactionTemplate(transactionManager).execute(status -> {
                repository.lockAccessUsers(List.of(userId));
                jdbcTemplate.update("UPDATE access_users SET organization_id = ? WHERE id = ?", "org_f10_new_" + suffix, userId);
                Future<Throwable> pending = applier.submit(() -> {
                    authenticate("usr_f10_actor_" + suffix, "f10-actor-" + suffix, "staff", oldProject);
                    try {
                        accessService.applyPermissionTemplate(templateId, new PermissionTemplateApplyRequest(
                                "f10-apply-" + suffix, "apply", List.of(), templateId, List.of(userId), "merge"));
                        return null;
                    } catch (Throwable e) {
                        return e;
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                });
                awaitWaiterOrDone(pending);
                return pending;
            });
            assertThat(apply.get(30, TimeUnit.SECONDS)).isInstanceOfSatisfying(ApiException.class,
                    e -> assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN"));
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM permission_template_applications WHERE target_id = ? AND status = 'active'",
                    Integer.class, userId)).isZero();
        } finally {
            applier.shutdownNow();
            jdbcTemplate.update("DELETE FROM permission_template_applications WHERE target_id = ?", userId);
            jdbcTemplate.update("DELETE FROM permission_templates WHERE id = ?", templateId);
            jdbcTemplate.update("DELETE FROM access_users WHERE id = ?", userId);
        }
    }

    // M001-F10：换项目（持授权事实锁、锁员工行后改员工表）与审批落地（持授权事实锁、再锁同一员工行）并发时串行完成，不互相死锁。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void scopeChangeAndGrantReviewOnTheSameUserSerializeWithoutDeadlock() throws Exception {
        String userId = "usr_f10_dl_" + UUID.randomUUID().toString().substring(0, 8);
        repository.createUser(new AccessUser(userId, userId, "staff", "org_f10_dl_old", null, "active", Instant.now()));
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        ExecutorService reviewer = Executors.newSingleThreadExecutor();
        try {
            Future<?> review = tx.execute(status -> {
                repository.lockGrantAuthorizationFacts();
                repository.lockAccessUsers(List.of(userId));
                Future<?> pending = reviewer.submit(() -> tx.executeWithoutResult(inner -> {
                    repository.lockGrantAuthorizationFacts();
                    repository.lockAccessUsers(List.of(userId));
                }));
                awaitWaiterOrDone(pending);
                jdbcTemplate.update("UPDATE access_users SET organization_id = ? WHERE id = ?", "org_f10_dl_new", userId);
                return pending;
            });
            review.get(30, TimeUnit.SECONDS);
        } finally {
            reviewer.shutdownNow();
            jdbcTemplate.update("DELETE FROM access_users WHERE id = ?", userId);
        }
    }

    // M001-F12：换项目按员工本身的记录枚举来源——空模板的 active 套用、无权限码的直接角色都在列，撤销过的不在列；
    // 已过到期时间的套用先被有效权限读取置为过期并留痕，也不在列（换项目先读有效权限、后枚举）。
    @Test
    void userGrantSourcesAreEnumeratedFromTheRecordsThemselves() {
        repository.createUser(new AccessUser("usr_f12_src", "usr_f12_src", "staff", "org", null, "active", Instant.now()));
        repository.createTemplate(baseTemplate("tpl_f12_empty"), List.of());
        repository.createTemplate(baseTemplate("tpl_f12_other"), grants("tpl_f12_other", DAILY_A));
        String empty = repository.applyTemplate("tpl_f12_empty", List.of("usr_f12_src"), null, null, false, "seed-admin").get(0).id();
        String revoked = repository.applyTemplate("tpl_f12_other", List.of("usr_f12_src"), null, null, false, "seed-admin").get(0).id();
        repository.revokeTemplateApplications("usr_f12_src", List.of(revoked), "seed-admin", Instant.now());
        String lapsed = repository.applyTemplate("tpl_f12_other", List.of("usr_f12_src"), null,
                Instant.now().minusSeconds(3600), false, "seed-admin").get(0).id();
        repository.createRole(new RoleRecord("role_f12_empty", "role_f12_empty", "F12 empty role", "project", "active", Instant.now(), List.of(), null));
        repository.assignUserRole("usr_f12_src", "role_f12_empty");

        assertThat(repository.findPermissionEffects("usr_f12_src", Instant.now())).isEmpty(); // 空来源不产出权限码，到期的已置过期
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM permission_template_applications WHERE id = ?", String.class, lapsed)).isEqualTo("expired");
        assertThat(repository.findActiveTemplateApplicationIds("usr_f12_src")).containsExactly(empty);
        assertThat(repository.findUserRoleIds("usr_f12_src")).containsExactly("role_f12_empty");
    }

    // M001-F10/F13：新增的锁语句在真实 schema 上可执行并真的持锁（空员工列表不发 SQL）。
    @Test
    void repairLockStatementsHoldTheirLocksOnTheRealSchema() {
        repository.createTemplate(baseTemplate("tpl_lock_sql"), List.of());
        repository.lockAccessUsers(List.of());
        repository.lockPositionRoleTemplatesForWrite();
        repository.lockTemplateForEdit("tpl_lock_sql");
        repository.lockTemplateForApply("tpl_lock_sql");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM pg_locks
                 WHERE pid = pg_backend_pid() AND granted AND mode = 'RowExclusiveLock'
                   AND relation IN ('position_role_templates'::regclass, 'permission_templates'::regclass)
                """, Integer.class)).isEqualTo(2);
    }

    // M001-F13（F11「记录……增加与去掉的权限码」）：两个编辑交错时，后提交者在模板锁内读前态，事件按实际前态记增减。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentTemplateEditsRecordTheRealDelta() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String templateId = "tpl_f13_edit_" + suffix;
        String userId = "usr_f13_edit_" + suffix;
        String editKey = "f13-edit-" + suffix;
        repository.createUser(new AccessUser(userId, userId, "staff", "org", null, "active", Instant.now()));
        repository.createTemplate(baseTemplate(templateId), grants(templateId, DAILY_A));
        repository.applyTemplate(templateId, List.of(userId), null, null, false, "seed-admin");
        ExecutorService editor = Executors.newSingleThreadExecutor();
        try {
            Future<Throwable> edit = new TransactionTemplate(transactionManager).execute(status -> {
                repository.lockTemplateForEdit(templateId);
                Future<Throwable> pending = editor.submit(() -> asPlatformAdmin(suffix, () -> accessService.updatePermissionTemplate(
                        templateId, templateRequest(editKey, DAILY_A, DAILY_C))));
                awaitWaiterOrDone(pending);
                repository.saveTemplate(baseTemplate(templateId), grants(templateId, DAILY_A, DAILY_B));
                return pending;
            });
            assertThat(edit.get(30, TimeUnit.SECONDS)).isNull();
            assertThat(eventCodes("permission_template.effect_changed", editKey, "added_permission_codes")).containsExactly(DAILY_C);
            assertThat(eventCodes("permission_template.effect_changed", editKey, "removed_permission_codes")).containsExactly(DAILY_B);
            // M001-F14：生产同型主体下，逐人事件的操作人与发生时间
            assertThat(jdbcTemplate.queryForList("""
                    SELECT actor_id || '|' || actor_type || '|' || (metadata ->> 'operator_id') || '|' || CAST(occurred_at IS NOT NULL AS text)
                      FROM audit_events WHERE event_name = 'permission_template.effect_changed' AND idempotency_key = ?
                    """, String.class, editKey))
                    .containsExactly("usr_m001_admin_" + suffix + "|platform_admin|usr_m001_admin_" + suffix + "|true");
        } finally {
            editor.shutdownNow();
            deleteTemplateAndUsers(templateId, userId);
        }
    }

    // M001-F13（F11「……回滚则不留」）：修改已套用模板的事务回滚后，逐人事件与条目改动都不留下。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rolledBackTemplateEditLeavesNoEffectEvent() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String templateId = "tpl_f13_rb_" + suffix;
        String userId = "usr_f13_rb_" + suffix;
        String editKey = "f13-rb-" + suffix;
        repository.createUser(new AccessUser(userId, userId, "staff", "org", null, "active", Instant.now()));
        repository.createTemplate(baseTemplate(templateId), grants(templateId, DAILY_A));
        repository.applyTemplate(templateId, List.of(userId), null, null, false, "seed-admin");
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                assertThat(asPlatformAdmin(suffix, () -> accessService.updatePermissionTemplate(
                        templateId, templateRequest(editKey, DAILY_A, DAILY_C)))).isNull();
                // 正向对照：同一事务连接上看得到未提交的逐人事件，回滚后才消失
                assertThat(eventCodes("permission_template.effect_changed", editKey, "added_permission_codes")).containsExactly(DAILY_C);
                status.setRollbackOnly();
            });
            assertThat(eventCodes("permission_template.effect_changed", editKey, "added_permission_codes")).isEmpty();
            assertThat(repository.findTemplateItems(templateId)).extracting(PermissionTemplateItem::permissionCode).containsExactly(DAILY_A);
        } finally {
            deleteTemplateAndUsers(templateId, userId);
        }
    }

    // M001-F13（F11「模板套用成功事件同时记录当时的权限码快照」）：编辑未提交时发起的套用，等编辑提交后按提交后的权限码记快照。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void templateApplyWaitsForAConcurrentEditAndSnapshotsCommittedCodes() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String templateId = "tpl_f13_apply_" + suffix;
        String userId = "usr_f13_apply_" + suffix;
        String applyKey = "f13-apply-" + suffix;
        repository.createUser(new AccessUser(userId, userId, "staff", "org", null, "active", Instant.now()));
        repository.createTemplate(baseTemplate(templateId), grants(templateId, DAILY_A));
        ExecutorService applier = Executors.newSingleThreadExecutor();
        try {
            Future<Throwable> apply = new TransactionTemplate(transactionManager).execute(status -> {
                repository.lockTemplateForEdit(templateId);
                repository.saveTemplate(baseTemplate(templateId), grants(templateId, DAILY_A, DAILY_B));
                Future<Throwable> pending = applier.submit(() -> asPlatformAdmin(suffix, () -> accessService.applyPermissionTemplate(
                        templateId, new PermissionTemplateApplyRequest(applyKey, "apply", List.of(), templateId, List.of(userId), "merge"))));
                awaitWaiterOrDone(pending);
                return pending;
            });
            assertThat(apply.get(30, TimeUnit.SECONDS)).isNull();
            assertThat(eventCodes("permission_template.applied", applyKey, "permission_codes")).containsExactly(DAILY_A, DAILY_B);
        } finally {
            applier.shutdownNow();
            deleteTemplateAndUsers(templateId, userId);
        }
    }

    // ── M001 第 4 轮（M001-F16～F21）：真实库 / 真实接口回归（随测试事务回滚；并发用例真实提交、结束后清理） ──

    // M001-F16（PGM 权限规则「数据的项目与商户隔离不提供关闭开关」；操作员 2026-09-26 更正段）：真实接口——同项目商户 B 的操作人
    // （持 access.permission_template.manage）修改商户 A 员工正在套用的项目模板，403 CROSS_MERCHANT_FORBIDDEN，模板行、条目、套用与有效权限都不变、
    // 不写逐人事件；商户 A 的操作人照常修改并逐人留痕。
    @Test
    void merchantOperatorEditOfATemplateHeldByAnotherMerchantIsRejectedThroughTheEndpoint() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String project = "org_f16_" + suffix;
        String templateId = "tpl_f16_" + suffix;
        String holder = "usr_f16_h_" + suffix;
        String role = "role_f16_manage_" + suffix;
        repository.createRole(new RoleRecord(role, "F16 模板维护 " + suffix, null, "project", "active", Instant.now(),
                List.of("access.permission_template.manage"), null));
        repository.createUser(new AccessUser(holder, holder, "staff", project, "merchant_a_" + suffix, "active", Instant.now()));
        for (String merchant : List.of("a", "b")) {
            String editor = "usr_f16_" + merchant + "_" + suffix;
            repository.createUser(new AccessUser(editor, editor, "merchant_admin", project, "merchant_" + merchant + "_" + suffix, "active", Instant.now()));
            repository.assignUserRole(editor, role);
        }
        repository.createTemplate(new PermissionTemplate(templateId, templateId, templateId, "M001 test template", "base", null,
                "organization", project, "merge", null, null, false, "active", "integration test", "seed-admin",
                Instant.now(), Instant.now(), Instant.now()), grants(templateId, DAILY_A));
        String applicationId = repository.applyTemplate(templateId, List.of(holder), null, null, false, "seed-admin").get(0).id();

        String keyB = "f16-edit-b-" + suffix;
        mockMvc.perform(put("/api/v1/access/permission-templates/" + templateId).with(f16Editor("b", project, suffix))
                        .contentType(MediaType.APPLICATION_JSON).content(templateJson(keyB, "F16 改名 " + suffix, "base", "project")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        assertThat(repository.findTemplateById(templateId).orElseThrow().templateName()).isEqualTo(templateId);
        assertThat(templateCodes(templateId)).containsExactly(DAILY_A);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM permission_template_applications WHERE id = ?",
                String.class, applicationId)).isEqualTo("active");
        assertThat(repository.findPermissionEffects(holder, Instant.now()))
                .extracting(AccessRepository.PermissionEffect::permissionCode).containsExactly(DAILY_A);
        assertThat(eventCount("permission_template.effect_changed", keyB)).isZero();

        String keyA = "f16-edit-a-" + suffix;
        mockMvc.perform(put("/api/v1/access/permission-templates/" + templateId).with(f16Editor("a", project, suffix))
                        .contentType(MediaType.APPLICATION_JSON).content(templateJson(keyA, "F16 改名 " + suffix, "base", "project")))
                .andExpect(status().isOk());
        assertThat(templateCodes(templateId)).isEmpty();
        assertThat(eventCount("permission_template.effect_changed", keyA)).isEqualTo(1);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor f16Editor(String merchant, String project, String suffix) {
        String editor = "usr_f16_" + merchant + "_" + suffix;
        return m002Session(new AccessActor(editor, editor, "merchant_admin", project, "merchant_" + merchant + "_" + suffix, "active"));
    }

    // M001-F16（验收「并发归属变化不能绕过」）：修改入口先按编号锁住在用员工行再读其商户——在用员工的换商户事务锁住员工行、未提交时，
    // 商户 A 的操作人修改须等它提交，按新商户（B）被 403 拒绝，模板条目不变。真实并发、数据真实提交，结束后清理。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void merchantEditWaitsForAConcurrentMerchantChangeOfAHolderAndIsRejected() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String project = "org_f16c_" + suffix;
        String templateId = "tpl_f16c_" + suffix;
        String holder = "usr_f16c_h_" + suffix;
        String editor = "usr_f16c_e_" + suffix;
        Instant now = Instant.now();
        repository.createUser(new AccessUser(holder, holder, "staff", project, "merchant_a_" + suffix, "active", now));
        repository.createUser(new AccessUser(editor, editor, "merchant_admin", project, "merchant_a_" + suffix, "active", now));
        repository.createTemplate(new PermissionTemplate(templateId, templateId, templateId, "M001 test template", "base", null,
                "organization", project, "merge", null, null, false, "active", "integration test", "seed-admin", now, now, now),
                grants(templateId, DAILY_A));
        repository.applyTemplate(templateId, List.of(holder), null, null, false, "seed-admin");
        ExecutorService editing = Executors.newSingleThreadExecutor();
        try {
            Future<Throwable> edit = new TransactionTemplate(transactionManager).execute(status -> {
                repository.lockAccessUsers(List.of(holder));
                jdbcTemplate.update("UPDATE access_users SET merchant_id = ? WHERE id = ?", "merchant_b_" + suffix, holder);
                Future<Throwable> pending = editing.submit(() -> {
                    try {
                        m002ActAs(new AccessActor(editor, editor, "merchant_admin", project, "merchant_a_" + suffix, "active"),
                                () -> accessService.updatePermissionTemplate(templateId, templateRequest("f16c-edit-" + suffix)));
                        return null;
                    } catch (Throwable e) {
                        return e;
                    }
                });
                awaitWaiterOrDone(pending);
                return pending;
            });
            assertThat(edit.get(30, TimeUnit.SECONDS)).isInstanceOfSatisfying(ApiException.class,
                    e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
            assertThat(templateCodes(templateId)).containsExactly(DAILY_A);
        } finally {
            editing.shutdownNow();
            deleteTemplateAndUsers(templateId, holder, editor);
        }
    }

    // M001-F19（操作员确认句 1；裁决说明「复制＝以源模板为初值新建，未填项沿用源模板……源模板不存在仍 404」）、M001-F20：真实接口——
    // 平台管理员复制不存在的模板 404、不建行、不写复制成功事件；前端同形复制请求（只给名称）得到与源模板同类型、同窗口、同单次、同条目的新模板，
    // 响应里的权限码与落库条目一致，套用出去的套用行到期与单次同源模板；请求里填了的项按填的（换权限码、重设生效时间、到期推后、取消单次），
    // 落库与响应一致、套用后的有效权限只有新填的码；类型 copy 400。
    // 界面流程（复制后修改复制件）：已套用的复制件放宽 409；再只给名称复制一份，修改这份没人在用的复制件——推后到期、取消单次、清空到期并改为基础模板——都 200。
    @Test
    void copyOfAMissingSourceIsNotFoundAndTheCopyFollowsTheSourceThroughTheEndpoint() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String admin = m001xAdmin(suffix);
        String missing = "tpl_f19_missing_" + suffix;
        String missingKey = "f19-missing-" + suffix;
        mockMvc.perform(post("/api/v1/access/permission-templates/" + missing + "/copy").with(m001xSession(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(copyJson(missingKey)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("TEMPLATE_NOT_FOUND"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM permission_templates WHERE source_template_id = ?",
                Integer.class, missing)).isZero();
        assertThat(eventCount("permission_template.copied", missingKey)).isZero();

        String source = "tpl_f19_source_" + suffix;
        Instant from = Instant.parse("2099-01-01T00:00:00Z");
        Instant until = Instant.parse("2099-01-31T00:00:00Z");
        repository.createTemplate(temporaryTemplate(source, from, until, true), grants(source, DAILY_A, DAILY_B));
        String copyId = com.jayway.jsonpath.JsonPath.read(mockMvc.perform(post("/api/v1/access/permission-templates/" + source + "/copy")
                        .with(m001xSession(admin)).contentType(MediaType.APPLICATION_JSON).content(copyJson("f19-copy-" + suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.template_type").value("temporary"))
                .andExpect(jsonPath("$.data.single_use").value(true))
                .andExpect(jsonPath("$.data.permission_codes", org.hamcrest.Matchers.containsInAnyOrder(DAILY_A, DAILY_B)))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        assertThat(templateCodes(copyId)).containsExactlyInAnyOrder(DAILY_A, DAILY_B);
        assertThat(jdbcTemplate.queryForList("""
                SELECT template_type || '|' || CAST(valid_from = CAST(? AS timestamptz) AS text) || '|'
                       || CAST(valid_to = CAST(? AS timestamptz) AS text) || '|' || CAST(single_use AS text)
                  FROM permission_templates WHERE source_template_id = ?
                """, String.class, from.toString(), until.toString(), source)).containsExactly("temporary|true|true|true");

        String holder = "usr_f19_" + suffix;
        repository.createUser(new AccessUser(holder, holder, "staff", "org", null, "active", Instant.now()));
        mockMvc.perform(post("/api/v1/access/permission-templates/" + copyId + "/apply").with(m001xSession(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(applyJson("f19-apply-" + suffix, "append", "套用复制件", List.of(), holder)))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForList("""
                SELECT CAST(valid_until = CAST(? AS timestamptz) AS text) || '|' || CAST(single_use AS text)
                  FROM permission_template_applications WHERE template_id = ? AND target_id = ?
                """, String.class, until.toString(), copyId, holder)).containsExactly("true|true");

        Instant later = until.plusSeconds(86400);
        Instant resetFrom = Instant.parse("2099-01-02T00:00:00Z");
        mockMvc.perform(put("/api/v1/access/permission-templates/" + copyId).with(m001xSession(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(windowJson("f19-held-later-" + suffix, "temporary", from, later, true)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TEMPLATE_REAUTHORIZATION_REQUIRED"));
        String unused = com.jayway.jsonpath.JsonPath.read(mockMvc.perform(post("/api/v1/access/permission-templates/" + source + "/copy")
                        .with(m001xSession(admin)).contentType(MediaType.APPLICATION_JSON).content(copyJson("f19-copy-unused-" + suffix)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        for (String body : List.of(windowJson("f19-put-later-" + suffix, "temporary", from, later, true),
                windowJson("f19-put-multi-" + suffix, "temporary", from, later, false),
                windowJson("f19-put-base-" + suffix, "base", null, null, false))) {
            mockMvc.perform(put("/api/v1/access/permission-templates/" + unused).with(m001xSession(admin))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());
        }
        assertThat(jdbcTemplate.queryForList(
                "SELECT template_type || '|' || CAST(valid_from IS NULL AND valid_to IS NULL AND NOT single_use AS text) FROM permission_templates WHERE id = ?",
                String.class, unused)).containsExactly("base|true");
        assertThat(jdbcTemplate.queryForList(
                "SELECT CAST(valid_to = CAST(? AS timestamptz) AND single_use AS text) FROM permission_templates WHERE id IN (?, ?)",
                String.class, until.toString(), source, copyId)).containsExactly("true", "true");

        String resetId = com.jayway.jsonpath.JsonPath.read(mockMvc.perform(post("/api/v1/access/permission-templates/" + source + "/copy")
                        .with(m001xSession(admin)).contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "f19-reset-%s", "template_name": "F19 重设", "operator_note": "后台复制权限模板",
                                 "permission_codes": ["%s"], "valid_from": "%s", "valid_until": "%s", "single_use": false}
                                """.formatted(suffix, DAILY_B, resetFrom, later)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.single_use").value(false))
                .andExpect(jsonPath("$.data.permission_codes", org.hamcrest.Matchers.contains(DAILY_B)))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        assertThat(templateCodes(resetId)).containsExactly(DAILY_B);
        assertThat(jdbcTemplate.queryForList("""
                SELECT template_name FROM permission_templates
                 WHERE id = ? AND valid_from = CAST(? AS timestamptz) AND valid_to = CAST(? AS timestamptz) AND NOT single_use
                """, String.class, resetId, resetFrom.toString(), later.toString())).containsExactly("F19 重设");
        String resetHolder = "usr_f19_reset_" + suffix;
        repository.createUser(new AccessUser(resetHolder, resetHolder, "staff", "org", null, "active", Instant.now()));
        mockMvc.perform(post("/api/v1/access/permission-templates/" + resetId + "/apply").with(m001xSession(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(applyJson("f19-reset-apply-" + suffix, "append", "套用重设件", List.of(), resetHolder)))
                .andExpect(status().isOk());
        assertThat(repository.findPermissionEffects(resetHolder, resetFrom.plusSeconds(60)))
                .extracting(AccessRepository.PermissionEffect::permissionCode).containsExactly(DAILY_B);

        mockMvc.perform(post("/api/v1/access/permission-templates/" + source + "/copy").with(m001xSession(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "f20-type-copy-%s", "template_name": "F20 旧类型", "template_type": "copy"}
                                """.formatted(suffix)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM permission_templates WHERE source_template_id = ?",
                Integer.class, source)).isEqualTo(3);
    }

    // M001-F17/F18（操作员确认句 2「模板已被套用且仍有员工在用时，收窄其有效期或改为单次使用，在用员工立即按收窄后的期限执行，
    // 并逐人记录改前改后的期限和操作人；放宽的，新建模板后另行套用」）：真实库——
    // ① 截止提前到已过去：逐人记改前改后期限与操作人；该套用在同一事务内置为已过期并写一条到期日志，到期作业再跑不重复记；
    //   套用已结束，此后放宽 200、套用不复活；套用行自身截止已过、仍为 active 的套用不挡放宽；
    // ② 另一模板（套用行带生效时间）生效推后、到期提前并改为单次：有效权限窗口为交集（生效取较晚、截止取较早）、单次为真，第二名员工套用窗口更窄、
    //   取套用行一侧；逐人事件记改前改后的生效时间；该员工判权被拒（收窄前通过）；有人在用时放宽 409，模板与套用行都不变；
    //   无人查询时到期作业按新截止（而不是套用行的旧截止）置过期并写到期日志，再跑不重复记；
    // ③ 新增截止：原无截止的基础模板加上已过去的截止，在用员工同样立即到期并逐人留痕。
    @Test
    void narrowingAnAppliedTemplateTakesEffectForHoldersOnTheRealSchema() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String admin = "usr_f17_admin_" + suffix;
        Instant from = Instant.parse("2099-01-01T00:00:00Z");
        Instant laterStart = Instant.parse("2099-01-03T00:00:00Z");
        Instant until = Instant.parse("2099-01-31T00:00:00Z");
        Instant earlier = Instant.parse("2099-01-05T00:00:00Z");
        Instant past = Instant.now().minusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        String lapsing = "tpl_f17_lapsing_" + suffix;
        String narrowed = "tpl_f17_narrowed_" + suffix;
        String holder = "usr_f17_" + suffix;
        String inner = "usr_f17_inner_" + suffix;
        Instant innerFrom = Instant.parse("2099-01-04T00:00:00Z");
        Instant innerUntil = Instant.parse("2099-01-04T12:00:00Z");
        repository.createUser(new AccessUser(holder, holder, "staff", "org", null, "active", Instant.now()));
        repository.createUser(new AccessUser(inner, inner, "staff", "org", null, "active", Instant.now()));
        repository.createTemplate(temporaryTemplate(lapsing, null, until, false), grants(lapsing, DAILY_A));
        repository.createTemplate(temporaryTemplate(narrowed, from, until, false), grants(narrowed, DAILY_A));
        String lapsingApplication = repository.applyTemplate(lapsing, List.of(holder), null, until, false, "seed-admin").get(0).id();
        String narrowedApplication = repository.applyTemplate(narrowed, List.of(holder), from, until, false, "seed-admin").get(0).id();
        String innerApplication = repository.applyTemplate(narrowed, List.of(inner), innerFrom, innerUntil, false, "seed-admin").get(0).id();
        m002ActAs(holder, "staff", () -> authorizationService.requirePermission(DAILY_A)); // 收窄前：lapsing 的套用生效

        String key = "f17-lapse-" + suffix;
        m002ActAs(admin, "platform_admin", () -> accessService.updatePermissionTemplate(lapsing, f17Request(key, null, past, false)));
        assertThat(jdbcTemplate.queryForList("""
                SELECT metadata ->> 'application_id' AS application_id, metadata ->> 'valid_until_before' AS before,
                       metadata ->> 'valid_until_after' AS after, metadata ->> 'operator_id' AS operator
                  FROM audit_events WHERE event_name = 'permission_template.effect_changed' AND idempotency_key = ?
                """, key)).singleElement().satisfies(event -> assertThat(event)
                .containsEntry("application_id", lapsingApplication).containsEntry("before", until.toString())
                .containsEntry("after", past.toString()).containsEntry("operator", admin));
        assertThat(applicationStatus(lapsingApplication)).isEqualTo("expired");
        assertThat(usageResults(lapsingApplication)).containsExactly("expired");
        assertThat(repository.findActiveTemplateApplications(lapsing)).isEmpty();
        repository.expireLapsedTemplateApplications(Instant.now());
        assertThat(usageResults(lapsingApplication)).containsExactly("expired");
        m002ActAs(admin, "platform_admin", () -> accessService.updatePermissionTemplate(lapsing,
                f17Request("f17-widen-after-expiry-" + suffix, null, until, false)));
        assertThat(repository.findTemplateById(lapsing).orElseThrow().validUntil()).isEqualTo(until);
        assertThat(applicationStatus(lapsingApplication)).isEqualTo("expired");
        assertThat(repository.findActiveTemplateApplications(lapsing)).isEmpty();
        String stale = "tpl_f17_stale_" + suffix;
        repository.createTemplate(temporaryTemplate(stale, null, until, false), grants(stale, DAILY_A));
        repository.applyTemplate(stale, List.of(holder), null, past, false, "seed-admin");
        m002ActAs(admin, "platform_admin", () -> accessService.updatePermissionTemplate(stale,
                f17Request("f17-widen-stale-" + suffix, null, until.plusSeconds(86400), false)));
        assertThat(repository.findTemplateById(stale).orElseThrow().validUntil()).isEqualTo(until.plusSeconds(86400));

        String narrowKey = "f17-narrow-" + suffix;
        m002ActAs(admin, "platform_admin", () -> accessService.updatePermissionTemplate(narrowed,
                f17Request(narrowKey, laterStart, earlier, true)));
        assertThat(repository.findPermissionEffects(holder, Instant.now()))
                .filteredOn(effect -> narrowedApplication.equals(effect.sourceId()))
                .singleElement().satisfies(effect -> {
                    assertThat(effect.validFrom()).isEqualTo(laterStart);
                    assertThat(effect.validUntil()).isEqualTo(earlier);
                    assertThat(effect.singleUse()).isTrue();
                });
        assertThat(repository.findPermissionEffects(inner, Instant.now()))
                .filteredOn(effect -> innerApplication.equals(effect.sourceId()))
                .singleElement().satisfies(effect -> {
                    assertThat(effect.validFrom()).isEqualTo(innerFrom);
                    assertThat(effect.validUntil()).isEqualTo(innerUntil);
                    assertThat(effect.singleUse()).isTrue();
                });
        assertThat(jdbcTemplate.queryForList("""
                SELECT metadata ->> 'valid_from_before' AS before, metadata ->> 'valid_from_after' AS after
                  FROM audit_events WHERE event_name = 'permission_template.effect_changed' AND idempotency_key = ?
                   AND metadata ->> 'application_id' = ?
                """, narrowKey, narrowedApplication)).singleElement().satisfies(event -> assertThat(event)
                .containsEntry("before", from.toString()).containsEntry("after", laterStart.toString()));
        m002ActAs(holder, "staff", () -> assertPermissionDenied(DAILY_A)); // 收窄后：lapsing 已到期，narrowed 未到生效时间

        assertThatThrownBy(() -> m002ActAs(admin, "platform_admin", () -> accessService.updatePermissionTemplate(narrowed,
                f17Request("f17-widen-" + suffix, laterStart, until, true))))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("TEMPLATE_REAUTHORIZATION_REQUIRED");
                    assertThat(e.getMessage()).isEqualTo("模板已被套用且仍有员工在用，不能放宽有效期或取消单次使用限制；请新建模板或复制为新模板后另行套用");
                });
        assertThat(repository.findTemplateById(narrowed).orElseThrow().validUntil()).isEqualTo(earlier);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT valid_until = CAST(? AS timestamptz) FROM permission_template_applications WHERE id = ?",
                Boolean.class, until.toString(), narrowedApplication)).isTrue();

        // 无人查询：到期作业在新截止之后、套用行旧截止之前运行，按新截止置过期并写到期日志；再跑不重复记。
        Instant afterNewDeadline = earlier.plusSeconds(1);
        repository.expireLapsedTemplateApplications(afterNewDeadline);
        assertThat(applicationStatus(narrowedApplication)).isEqualTo("expired");
        assertThat(usageResults(narrowedApplication)).containsExactly("expired");
        repository.expireLapsedTemplateApplications(afterNewDeadline);
        assertThat(usageResults(narrowedApplication)).containsExactly("expired");

        String added = "tpl_f17_added_" + suffix;
        repository.createTemplate(baseTemplate(added), grants(added, DAILY_B));
        String addedApplication = repository.applyTemplate(added, List.of(holder), null, null, false, "seed-admin").get(0).id();
        String addKey = "f17-add-deadline-" + suffix;
        m002ActAs(admin, "platform_admin", () -> accessService.updatePermissionTemplate(added, new PermissionTemplateRequest(
                addKey, "M001-F17", List.of(), added, "base", null, List.of(DAILY_B), "project", null, past, false)));
        assertThat(jdbcTemplate.queryForList("""
                SELECT metadata ->> 'valid_until_before' AS before, metadata ->> 'valid_until_after' AS after
                  FROM audit_events WHERE event_name = 'permission_template.effect_changed' AND idempotency_key = ?
                """, addKey)).singleElement().satisfies(event -> assertThat(event)
                .containsEntry("before", "").containsEntry("after", past.toString()));
        assertThat(applicationStatus(addedApplication)).isEqualTo("expired");
        assertThat(usageResults(addedApplication)).containsExactly("expired");
    }

    // M001-F25（句 2）：真实库迁移后的种子基础模板 tpl_outsourced_facility 条目范围为 assignee 与 project 混用（V181）。
    // 套用后修改不填数据范围、只新增截止：保存成功，条目范围不变，在用员工逐人留痕、实际截止变为新截止；明确填 project 仍 409。
    @Test
    void mixedScopeSeedTemplateInUseNarrowsWithoutDataScopeOnTheRealSchema() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String admin = "usr_f25_admin_" + suffix;
        String holder = "usr_f25_" + suffix;
        String template = "tpl_outsourced_facility";
        Instant until = Instant.parse("2099-01-31T00:00:00Z");
        List<java.util.Map<String, Object>> before = jdbcTemplate.queryForList(
                "SELECT permission_code, data_scope FROM permission_template_items WHERE template_id = ? AND effect = 'grant' ORDER BY permission_code",
                template);
        assertThat(before).extracting(row -> row.get("data_scope")).contains("assignee", "project"); // 种子确为混用范围
        List<String> codes = before.stream().map(row -> (String) row.get("permission_code")).toList();
        String name = repository.findTemplateById(template).orElseThrow().templateName();
        repository.createUser(new AccessUser(holder, holder, "staff", "org", null, "active", Instant.now()));
        String application = repository.applyTemplate(template, List.of(holder), null, null, false, "seed-admin").get(0).id();

        assertThatThrownBy(() -> m002ActAs(admin, "platform_admin", () -> accessService.updatePermissionTemplate(template,
                new PermissionTemplateRequest("f25-explicit-" + suffix, "M001-F25", List.of(), name, "base", null, codes, "project",
                        null, until, false))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("TEMPLATE_REAUTHORIZATION_REQUIRED"));

        String key = "f25-narrow-" + suffix;
        m002ActAs(admin, "platform_admin", () -> accessService.updatePermissionTemplate(template,
                new PermissionTemplateRequest(key, "M001-F25", List.of(), name, "base", null, codes, null, null, until, false)));
        assertThat(repository.findTemplateById(template).orElseThrow().validUntil()).isEqualTo(until);
        assertThat(jdbcTemplate.queryForList(
                "SELECT permission_code, data_scope FROM permission_template_items WHERE template_id = ? AND effect = 'grant' ORDER BY permission_code",
                template)).isEqualTo(before);
        assertThat(jdbcTemplate.queryForList("""
                SELECT metadata ->> 'valid_until_before' AS before, metadata ->> 'valid_until_after' AS after
                  FROM audit_events WHERE event_name = 'permission_template.effect_changed' AND idempotency_key = ?
                   AND metadata ->> 'application_id' = ?
                """, key, application)).singleElement().satisfies(event -> assertThat(event)
                .containsEntry("before", "").containsEntry("after", until.toString()));
    }

    private PermissionTemplateRequest f17Request(String key, Instant from, Instant until, boolean singleUse) {
        return new PermissionTemplateRequest(key, "M001-F17", List.of(), "F17 临时模板", "temporary", null,
                List.of(DAILY_A), "project", from, until, singleUse);
    }

    private String applicationStatus(String applicationId) {
        return jdbcTemplate.queryForObject("SELECT status FROM permission_template_applications WHERE id = ?", String.class, applicationId);
    }

    private List<String> usageResults(String applicationId) {
        return jdbcTemplate.queryForList("SELECT result FROM permission_template_usage_logs WHERE application_id = ?",
                String.class, applicationId);
    }

    // M001-F17（验收「并发定岗」；CODEX-M001-4-2「岗位路径……避免锁前 DTO 写入旧期限」）：定岗在取模板共享锁之前读出模板，
    // 与收窄该模板的修改并发时，等修改提交后写下的岗位派生套用行仍是收窄前的旧截止；有效权限按「套用当时窗口 ∩ 模板当前窗口」取交集，
    // 该员工立即按收窄后的截止执行。真实并发、数据真实提交，结束后清理。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void positionAssignmentRacingANarrowingEditStillGetsTheNarrowedWindow() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String templateId = "tpl_f17p_" + suffix;
        String role = "role_f17p_" + suffix;
        String mapping = "prt_f17p_" + suffix;
        String position = "f17p_pos_" + suffix;
        String target = "usr_f17p_" + suffix;
        String admin = "usr_f17p_admin_" + suffix;
        Instant from = Instant.parse("2099-01-01T00:00:00Z");
        Instant until = Instant.parse("2099-01-31T00:00:00Z");
        Instant earlier = Instant.parse("2099-01-05T00:00:00Z");
        Instant now = Instant.now();
        ExecutorService assigning = Executors.newSingleThreadExecutor();
        try {
            repository.createUser(new AccessUser(admin, admin, "platform_admin", "org", null, "active", now));
            repository.assignUserRole(admin, "role_platform_admin");
            repository.createUser(new AccessUser(target, target, "staff", "org", null, "active", now));
            repository.createRole(new RoleRecord(role, "F17 定岗角色 " + suffix, null, "project", "active", now, List.of(), null));
            repository.createTemplate(temporaryTemplate(templateId, from, until, false), grants(templateId, DAILY_A));
            repository.savePositionRoleTemplate(new PositionRoleTemplate(mapping, position, "lvl_m1_lower", role, templateId, null,
                    "active", admin, now, now));
            Future<Throwable> assign = new TransactionTemplate(transactionManager).execute(status -> {
                m002ActAs(admin, "platform_admin", () -> accessService.updatePermissionTemplate(templateId, new PermissionTemplateRequest(
                        "f17p-narrow-" + suffix, "M001-F17", List.of(), "F17 定岗模板", "temporary", null,
                        List.of(DAILY_A), "project", from, earlier, false)));
                Future<Throwable> pending = assigning.submit(() -> {
                    try {
                        m002ActAs(admin, "platform_admin", () -> accessService.assignUserPosition(new UserPositionAssignmentRequest(
                                "f17p-assign-" + suffix, "M001-F17", target, position, "lvl_m1_lower", null, null, null)));
                        return null;
                    } catch (Throwable e) {
                        return e;
                    }
                });
                awaitWaiterOrDone(pending);
                return pending;
            });
            assertThat(assign.get(30, TimeUnit.SECONDS)).isNull();
            String application = jdbcTemplate.queryForObject(
                    "SELECT position_application_ids[1] FROM user_position_assignments WHERE user_id = ?", String.class, target);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT valid_until = CAST(? AS timestamptz) FROM permission_template_applications WHERE id = ?",
                    Boolean.class, until.toString(), application)).as("定岗写下的是锁前读到的旧截止").isTrue();
            assertThat(repository.findPermissionEffects(target, Instant.now()))
                    .filteredOn(effect -> application.equals(effect.sourceId()))
                    .singleElement().satisfies(effect -> assertThat(effect.validUntil()).isEqualTo(earlier));
        } finally {
            assigning.shutdownNow();
            jdbcTemplate.update("DELETE FROM permission_template_applications WHERE template_id = ?", templateId);
            jdbcTemplate.update("DELETE FROM access_users WHERE id IN (?, ?)", target, admin);
            jdbcTemplate.update("DELETE FROM position_role_templates WHERE id = ?", mapping);
            jdbcTemplate.update("DELETE FROM permission_templates WHERE id = ?", templateId);
            jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id = ?", role);
            jdbcTemplate.update("DELETE FROM roles WHERE id = ?", role);
        }
    }

    // M001-F18（验收「失败回滚不保留成功痕迹」）：收窄已套用模板到已过去的截止、事务随后回滚——逐人事件、到期日志与套用状态都不留下，模板窗口不变。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rolledBackNarrowingLeavesNoTraceAndNoExpiry() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String templateId = "tpl_f18_rb_" + suffix;
        String userId = "usr_f18_rb_" + suffix;
        String editKey = "f18-rb-" + suffix;
        Instant until = Instant.parse("2099-01-31T00:00:00Z");
        Instant past = Instant.now().minusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        repository.createUser(new AccessUser(userId, userId, "staff", "org", null, "active", Instant.now()));
        repository.createTemplate(temporaryTemplate(templateId, null, until, false), grants(templateId, DAILY_A));
        String applicationId = repository.applyTemplate(templateId, List.of(userId), null, until, false, "seed-admin").get(0).id();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                assertThat(asPlatformAdmin(suffix, () -> accessService.updatePermissionTemplate(
                        templateId, f17Request(editKey, null, past, false)))).isNull();
                // 正向对照：同一事务连接上看得到未提交的逐人事件与到期，回滚后才消失
                assertThat(eventCount("permission_template.effect_changed", editKey)).isEqualTo(1);
                assertThat(applicationStatus(applicationId)).isEqualTo("expired");
                status.setRollbackOnly();
            });
            assertThat(eventCount("permission_template.effect_changed", editKey)).isZero();
            assertThat(applicationStatus(applicationId)).isEqualTo("active");
            assertThat(usageResults(applicationId)).isEmpty();
            assertThat(repository.findTemplateById(templateId).orElseThrow().validUntil()).isEqualTo(until);
        } finally {
            deleteTemplateAndUsers(templateId, userId);
        }
    }

    // M001-F21（基础技术不变量）：真实接口——原契约里的 preview 与错拼的 single-use 一律 400、不写套用行；single_use 写成单次套用。
    @Test
    void applyWithAModeOutsideTheClosedSetIsRejectedThroughTheEndpoint() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String admin = m001xAdmin(suffix);
        String templateId = "tpl_f21_" + suffix;
        String holder = "usr_f21_" + suffix;
        repository.createTemplate(baseTemplate(templateId), grants(templateId, DAILY_A));
        repository.createUser(new AccessUser(holder, holder, "staff", "org", null, "active", Instant.now()));
        for (String mode : List.of("preview", "replace", "single-use")) {
            mockMvc.perform(post("/api/v1/access/permission-templates/" + templateId + "/apply").with(m001xSession(admin))
                            .contentType(MediaType.APPLICATION_JSON).content(applyJson("f21-" + mode + "-" + suffix, mode, "套用", List.of(), holder)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        }
        assertThat(applicationSingleUse(templateId)).isEmpty();
        mockMvc.perform(post("/api/v1/access/permission-templates/" + templateId + "/apply").with(m001xSession(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(applyJson("f21-once-" + suffix, "single_use", "套用", List.of(), holder)))
                .andExpect(status().isOk());
        assertThat(applicationSingleUse(templateId)).containsExactly(true);
    }

    // M001-F20（操作员 2026-09-25 裁决「合并为同一种」；V436）：完整迁移后五个种子模板（原 role）都是基础模板 base，库里没有 role / copy；
    // 真实接口新建 role、copy 与其他取值的模板 400、不建行。
    @Test
    void templateTypesAreOnlyBaseAndTemporaryOnTheRealSchema() throws Exception {
        assertThat(jdbcTemplate.queryForList("""
                SELECT template_type FROM permission_templates
                 WHERE id IN ('tpl_outsourced_facility', 'tpl_outsourced_security_cleaning', 'tpl_outsourced_construction',
                              'tpl_auditor_internal', 'tpl_auditor_external')
                """, String.class)).hasSize(5).containsOnly("base");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM permission_templates WHERE template_type IN ('role', 'copy')",
                Integer.class)).isZero();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String admin = m001xAdmin(suffix);
        List<String> types = List.of("role", "copy", "admin");
        for (int i = 0; i < types.size(); i++) {
            mockMvc.perform(post("/api/v1/access/permission-templates").with(m001xSession(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(templateJson("f20-" + i + "-" + suffix, "F20 " + suffix, types.get(i), "project", DAILY_A)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        }
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM permission_templates WHERE template_name = ?",
                Integer.class, "F20 " + suffix)).isZero();
    }

    // 持 role_platform_admin（模板维护、套用等治理本职码）的平台管理员，随测试事务回滚。
    private String m001xAdmin(String suffix) {
        String admin = "usr_m001x_admin_" + suffix;
        repository.createUser(new AccessUser(admin, admin, "platform_admin", "org", null, "active", Instant.now()));
        repository.assignUserRole(admin, "role_platform_admin");
        return admin;
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor m001xSession(String admin) {
        return m002Session(new AccessActor(admin, admin, "platform_admin", "org", null, "active"));
    }

    private PermissionTemplate temporaryTemplate(String id, Instant from, Instant until, boolean singleUse) {
        Instant at = Instant.parse("2026-06-14T00:00:00Z");
        return new PermissionTemplate(id, id, id, "M001 test template", "temporary", null, "platform", null, "merge",
                from, until, singleUse, "active", "integration test", "seed-admin", at, at, at);
    }

    private static String jsonArray(java.util.Collection<String> values) {
        return values.stream().map(value -> "\"" + value + "\"").collect(java.util.stream.Collectors.joining(", ", "[", "]"));
    }

    private static String templateJson(String key, String name, String type, String dataScope, String... codes) {
        return """
                {"idempotency_key": "%s", "operator_note": "后台权限模板配置", "template_name": "%s", "template_type": "%s",
                 "data_scope": "%s", "permission_codes": %s, "single_use": false}
                """.formatted(key, name, type, dataScope, jsonArray(java.util.Arrays.asList(codes)));
    }

    // 前端编辑保存同形：回传名称、类型、全部条目码、窗口与单次（不限的窗口为 null）。
    private static String windowJson(String key, String type, Instant from, Instant until, boolean singleUse) {
        return """
                {"idempotency_key": "%s", "operator_note": "后台权限模板配置", "template_name": "F19 复制件", "template_type": "%s",
                 "data_scope": "project", "permission_codes": %s, "valid_from": %s, "valid_until": %s, "single_use": %s}
                """.formatted(key, type, jsonArray(List.of(DAILY_A, DAILY_B)), from == null ? "null" : "\"" + from + "\"",
                until == null ? "null" : "\"" + until + "\"", singleUse);
    }

    // 前端复制按钮同形（M001-F20 起不再带类型）：只给名称与备注。
    private static String copyJson(String key) {
        return """
                {"idempotency_key": "%s", "template_name": "F19 复制件", "operator_note": "后台复制权限模板"}
                """.formatted(key);
    }

    private static String applyJson(String key, String mode, String note, List<String> evidence, String... targets) {
        return """
                {"idempotency_key": "%s", "mode": "%s", "operator_note": "%s", "evidence_attachment_ids": %s, "target_user_ids": %s}
                """.formatted(key, mode, note, jsonArray(evidence), jsonArray(java.util.Arrays.asList(targets)));
    }

    private int eventCount(String eventName, String idempotencyKey) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = ? AND idempotency_key = ?",
                Integer.class, eventName, idempotencyKey);
    }

    private List<Boolean> applicationSingleUse(String templateId) {
        return jdbcTemplate.queryForList("SELECT single_use FROM permission_template_applications WHERE template_id = ?",
                Boolean.class, templateId);
    }

    private List<String> templateCodes(String templateId) {
        return repository.findTemplateItems(templateId).stream().map(PermissionTemplateItem::permissionCode).toList();
    }

    // 平台管理员日常桥可授的三个日常码（见 dailyCodesReachThePlatformAdminBridgeAndRiskTiersAreTheFourGovernedOnes），按字母序
    private static final String DAILY_A = "amusement.device.view";
    private static final String DAILY_B = "inventory.stocktake.view";
    private static final String DAILY_C = "inventory.trace_code.view";

    // M002-F33：requirePermission（员工本人会话）与有效权限查询（平台管理员读）对夹具高危码的结论一致，且等于期望。
    private void m002ExpectEffective(M002Fixture f, String userId, boolean expected) {
        List<List<String>> grants = new java.util.ArrayList<>();
        m002ActAs(f.admin(), "platform_admin", () -> grants.add(authorizationService.effectivePermissionsForUser(userId).grants()));
        boolean allowed;
        try {
            m002ActAs(userId, "staff", () -> authorizationService.requirePermission(f.code()));
            allowed = true;
        } catch (ApiException denied) {
            allowed = false;
        }
        assertThat(grants.get(0).contains(f.code())).as("有效权限查询").isEqualTo(expected);
        assertThat(allowed).as("requirePermission").isEqualTo(expected);
    }

    // M002-F32：只含给定条目的单次临时模板。
    private PermissionTemplate m002SingleUseTemplate(String id) {
        Instant at = Instant.parse("2026-06-14T00:00:00Z");
        return new PermissionTemplate(id, id, id, "M002 单次临时模板", "temporary", null, "platform", null, "merge",
                null, null, true, "active", "integration test", "seed-admin", at, at, at);
    }

    private record M002Widening(String position, String role, String first, String longTerm, String mapping) {
    }

    // M002-F32c：岗位映射（空角色＋给定的单次或限时模板，含夹具高危码）；员工定岗后释放单获批，先不使用。另备一个含同一码的长期模板。
    private M002Widening m002WideningFixture(M002Fixture f, String tag, PermissionTemplate first) {
        M002Widening w = new M002Widening("m002_" + tag + "pos_" + f.suffix(), "role_m002_" + tag + "_" + f.suffix(), first.id(),
                "tpl_m002_" + tag + "3_" + f.suffix(), "prt_m002_" + tag + "_" + f.suffix());
        repository.createRole(new RoleRecord(w.role(), "M002 放宽用例角色 " + tag + f.suffix(), null, "project", "active", Instant.now(),
                List.of(), null));
        repository.createTemplate(first, grants(first.id(), f.code()));
        repository.createTemplate(baseTemplate(w.longTerm()), grants(w.longTerm(), f.code()));
        repository.savePositionRoleTemplate(new PositionRoleTemplate(w.mapping(), w.position(), "lvl_m1_lower", w.role(), w.first(), null,
                "active", f.admin(), Instant.now(), Instant.now()));
        m002Assign(f, w.position(), "m002-" + tag + "-assign");
        String release = m002PendingRequestId(f.target(), "position_high_risk_release");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(release, m002Approve("m002-" + tag + "-approve-" + f.suffix())));
        assertThat(m002EffectiveGrants(f)).contains(f.code());
        return w;
    }

    // M002-F32c：新来源比批准时更宽——X 挂起、只有一张新待审单、使用被拒；新单批准后生效，可多次使用。
    private void m002ExpectRejudgedThenLongTerm(M002Fixture f, String key) {
        assertThat(denyOrigins(f.target(), f.code())).containsExactly("position_hold");
        assertThat(m002PendingReleaseCount(f.target())).isEqualTo(1);
        assertThatThrownBy(() -> m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code())))
                .as("单次或限时来源的旧批准不得放宽成长期").isInstanceOf(ApiException.class);
        String renewed = m002PendingRequestId(f.target(), "position_high_risk_release");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(renewed, m002Approve(key + "-renewed-" + f.suffix())));
        m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code()));
        m002ActAs(f.target(), "staff", () -> authorizationService.requirePermission(f.code()));
    }

    // M002-F32：员工岗位行记下的岗位派生套用（岗位映射只套一个模板，取第一条）。
    private String m002PositionApplication(String userId) {
        return jdbcTemplate.queryForObject("SELECT position_application_ids[1] FROM user_position_assignments WHERE user_id = ?",
                String.class, userId);
    }

    private PermissionTemplate baseTemplate(String id) {
        Instant at = Instant.parse("2026-06-14T00:00:00Z");
        return new PermissionTemplate(id, id, id, "M001 test template", "base", null, "platform", null, "merge",
                null, null, false, "active", "integration test", "seed-admin", at, at, at);
    }

    private List<PermissionTemplateItem> grants(String templateId, String... codes) {
        return java.util.Arrays.stream(codes).map(code -> item(templateId, code, AccessRepository.Effect.GRANT)).toList();
    }

    private PermissionTemplateRequest templateRequest(String idempotencyKey, String... codes) {
        return new PermissionTemplateRequest(idempotencyKey, "M001 test", List.of(), "M001 test template", "base", null,
                List.of(codes), "project", null, null, false);
    }

    // 以平台管理员身份在当前线程执行，返回抛出的异常（成功为 null）。主体用生产过滤器放入的 AccessActor。
    private Throwable asPlatformAdmin(String suffix, Runnable action) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor("usr_m001_admin_" + suffix, "m001-admin-" + suffix, "platform_admin", "org", null, "active"),
                null, List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF"))));
        try {
            action.run();
            return null;
        } catch (Throwable e) {
            return e;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private List<String> eventCodes(String eventName, String idempotencyKey, String field) {
        return jdbcTemplate.queryForList(
                "SELECT jsonb_array_elements_text(metadata -> CAST(? AS text)) FROM audit_events WHERE event_name = ? AND idempotency_key = ? ORDER BY 1",
                String.class, field, eventName, idempotencyKey);
    }

    // 审计事件只追加、不删；套用行与条目随模板级联删除。
    private void deleteTemplateAndUsers(String templateId, String... userIds) {
        jdbcTemplate.update("DELETE FROM permission_template_applications WHERE template_id = ?", templateId);
        jdbcTemplate.update("DELETE FROM permission_templates WHERE id = ?", templateId);
        for (String userId : userIds) {
            jdbcTemplate.update("DELETE FROM access_users WHERE id = ?", userId);
        }
    }

    // 在本事务连接上等到有会话被本事务的锁挡住，或对方不等锁就已结束（修复前的竞态），最多 20 秒。
    private void awaitWaiterOrDone(Future<?> pending) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!pending.isDone() && System.nanoTime() < deadline) {
            Integer waiters = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_locks WHERE NOT granted AND pg_backend_pid() = ANY (pg_blocking_pids(pid))", Integer.class);
            if (waiters != null && waiters > 0) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
        }
    }

    private String regclass(String tableName) {
        return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
    }

    private void authenticate(String id, String username, String accountType, String organizationId) {
        AuthService.UserRecord user = new AuthService.UserRecord(
                id,
                username,
                "hash",
                accountType,
                organizationId,
                null,
                "active",
                Instant.parse("2026-06-14T00:00:00Z")
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                "token",
                List.of(new SimpleGrantedAuthority("ROLE_P0_SMOKE"))
        ));
    }

    private void assertPermissionDenied(String permissionCode) {
        assertThatThrownBy(() -> authorizationService.requirePermission(permissionCode))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("PERMISSION_DENIED"));
    }

    private PermissionTemplate template(String id, String name, String sourceTemplateId) {
        return new PermissionTemplate(
                id,
                id,
                name,
                "test template",
                "temporary",
                sourceTemplateId,
                "project",
                "complex",
                "merge",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-15T00:00:00Z"),
                true,
                "active",
                "integration test",
                "seed-admin",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        );
    }

    private PermissionTemplateItem item(
            String templateId,
            String permissionCode,
            AccessRepository.Effect effect
    ) {
        return new PermissionTemplateItem(templateId, permissionCode, effect, "project", false);
    }

    // XM-D029-004（PGM-041「临时授权可设有效期或单次使用；……单次消耗……全部留痕」）：一组两码的单次临时授权，
    // 用其中一个即整组消耗，两个码都各有一条消耗留痕。
    @Test
    void consumingOneCodeOfASingleUseGrantGroupLogsEveryConsumedCode() {
        TemporaryGrant grant = new TemporaryGrant(
                "22222222-2222-2222-2222-222222222222",
                "usr_jdbc_group_consume",
                List.of("ops.production_readiness.view", "ops.production_readiness.check"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2099-06-15T00:00:00Z"),
                true,
                false,
                "active",
                "single use",
                "seed-admin",
                Instant.parse("2026-06-14T00:00:00Z"),
                null,
                null
        );
        repository.createTemporaryGrant(grant);

        assertThat(repository.markSingleUsePermissionConsumed(grant.id(), "ops.production_readiness.view",
                "usr_jdbc_group_consume", Instant.parse("2026-06-14T12:00:00Z"))).isTrue();

        assertThat(jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM permission_template_usage_logs
                 WHERE user_id = ? AND result = 'consumed'
                 ORDER BY permission_code
                """, String.class, "usr_jdbc_group_consume"))
                .containsExactly("ops.production_readiness.check", "ops.production_readiness.view");
    }

    // ── XM-M001-AF（治理地图第 653 行项目与商户隔离、第 658 行跨项目或换商户后旧范围派生权限立即失效；技术推导）真库回归 ──────────

    // 请求进行中操作人被调到新项目、新项目权限审批落地：旧会话不能用新项目权限过判定（断言 A）；
    // 下一次请求按新范围判定，不用重新登录（对照 C）。断言 B 单列在下一条用例。数据真实提交，结束后清理。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void staleSessionAfterCrossProjectMoveAndNewProjectGrantCannotApplyOldProjectTemplate() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String operator = "usr_af_o_" + suffix;
        String target = "usr_af_t_" + suffix;
        String templateId = "tpl_af_" + suffix;
        String oldProject = "org_af_old_" + suffix;
        String newProject = "org_af_new_" + suffix;
        String code = "access.permission_template.apply";
        Instant now = Instant.now();
        try {
            repository.createUser(new AccessUser(operator, "af-o-" + suffix, "staff", oldProject, null, "active", now));
            repository.createUser(new AccessUser(target, "af-t-" + suffix, "staff", oldProject, null, "active", now));
            repository.createTemplate(new PermissionTemplate(templateId, templateId, "AF template", "af", "base", null,
                    "organization", oldProject, "merge", null, null, false, "active", "af", "seed-admin", now, now, now), List.of());
            // 请求开始时的会话快照
            authenticate(operator, "af-o-" + suffix, "staff", oldProject);
            // 在途期间别人提交了两件事：调岗（与 changeUserScope 同一写入方法）、新项目权限审批落地
            repository.updateUserScope(operator, newProject, null, "seed-admin", Instant.now());
            repository.saveUserPermissionOverrides(operator, List.of(code), AccessRepository.Effect.GRANT, "新项目权限审批落地", "seed-admin");

            // 断言 A：新项目权限不能配旧项目身份使用
            assertPermissionDenied(code);

            // 对照 C：下一次请求（重新认证得到新范围身份）按新范围判定
            authenticate(operator, "af-o-" + suffix, "staff", newProject);
            authorizationService.requirePermission(code);
            assertThatThrownBy(() -> accessService.applyPermissionTemplate(templateId, new PermissionTemplateApplyRequest(
                    "af-apply-new-" + suffix, "apply", List.of(), templateId, List.of(target), "merge")))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN"));
        } finally {
            SecurityContextHolder.clearContext();
            jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key = ?", "af-apply-new-" + suffix);
            deleteTemplateAndUsers(templateId, operator, target); // 员工覆盖行随账号级联删除
        }
    }

    // 断言 B（单列，修复前自己会红）：请求进行中操作人被调到新项目，旧会话按旧项目身份套用旧项目模板——身份不一致被拒、整单回滚。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void staleSessionAfterCrossProjectMoveCannotApplyOldProjectTemplateAndNothingIsWritten() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String operator = "usr_afb_o_" + suffix;
        String target = "usr_afb_t_" + suffix;
        String templateId = "tpl_afb_" + suffix;
        String oldProject = "org_afb_old_" + suffix;
        Instant now = Instant.now();
        try {
            repository.createUser(new AccessUser(operator, "afb-o-" + suffix, "staff", oldProject, null, "active", now));
            repository.createUser(new AccessUser(target, "afb-t-" + suffix, "staff", oldProject, null, "active", now));
            repository.createTemplate(new PermissionTemplate(templateId, templateId, "AF template", "af", "base", null,
                    "organization", oldProject, "merge", null, null, false, "active", "af", "seed-admin", now, now, now), List.of());
            authenticate(operator, "afb-o-" + suffix, "staff", oldProject);
            repository.updateUserScope(operator, "org_afb_new_" + suffix, null, "seed-admin", Instant.now());

            assertThatThrownBy(() -> accessService.applyPermissionTemplate(templateId, new PermissionTemplateApplyRequest(
                    "afb-apply-old-" + suffix, "apply", List.of(), templateId, List.of(target), "merge")))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM permission_template_applications WHERE target_id = ? AND status = 'active'",
                    Integer.class, target)).isZero();
        } finally {
            SecurityContextHolder.clearContext();
            jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key = ?", "afb-apply-old-" + suffix);
            deleteTemplateAndUsers(templateId, operator, target);
        }
    }

    // 请求进行中账号被停用：旧会话（active）不能再过权限判定。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void staleSessionOfDisabledAccountIsRejected() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String operator = "usr_af_d_" + suffix;
        String project = "org_af_d_" + suffix;
        String code = "access.permission_template.apply";
        try {
            repository.createUser(new AccessUser(operator, "af-d-" + suffix, "staff", project, null, "active", Instant.now()));
            repository.saveUserPermissionOverrides(operator, List.of(code), AccessRepository.Effect.GRANT, "员工单独授予", "seed-admin");
            authenticate(operator, "af-d-" + suffix, "staff", project);
            authorizationService.requirePermission(code); // 对照：账号行与会话一致时放行

            jdbcTemplate.update("UPDATE access_users SET status = 'disabled' WHERE id = ?", operator);

            assertPermissionDenied(code);
        } finally {
            SecurityContextHolder.clearContext();
            jdbcTemplate.update("DELETE FROM access_users WHERE id = ?", operator); // 员工覆盖行随账号级联删除
        }
    }

    // M722-F03（M722-F01 验收证据补齐，不改实现）：只改所属商户——项目、账号类型、状态都不变——旧会话同样过不了判定，单次临时授权不被消耗。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void staleSessionAfterMerchantOnlyChangeIsRejectedAndSingleUseGrantIsKept() {
        afAssertStaleSessionKeepsSingleUseGrant("m", (operator, project) ->
                repository.updateUserScope(operator, project, "mch_af_moved", "seed-admin", Instant.now()));
    }

    // M722-F03：只改账号类型。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void staleSessionAfterAccountTypeOnlyChangeIsRejectedAndSingleUseGrantIsKept() {
        afAssertStaleSessionKeepsSingleUseGrant("t", (operator, project) ->
                jdbcTemplate.update("UPDATE access_users SET account_type = 'merchant_admin' WHERE id = ?", operator));
    }

    // 操作人持一份单次临时授权；会话建立后提交一项身份变更（入参：操作人、项目）。数据真实提交，结束后清理。
    private void afAssertStaleSessionKeepsSingleUseGrant(String tag, java.util.function.BiConsumer<String, String> committedChange) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String operator = "usr_af_" + tag + "_" + suffix;
        String project = "org_af_" + tag + "_" + suffix;
        String merchant = "mch_af_" + tag + "_" + suffix;
        String code = "access.permission_template.apply";
        Instant now = Instant.now();
        try {
            repository.createUser(new AccessUser(operator, operator, "staff", project, merchant, "active", now));
            repository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), operator, List.of(code),
                    now.minusSeconds(60), now.plusSeconds(3600), true, false, "active", "single use", "seed-admin", now, null, null));
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    new AccessActor(operator, operator, "staff", project, merchant, "active"), null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF"))));
            // 对照：变更前账号行与会话一致、装配足够（布尔判定不消耗单次来源）
            assertThat(authorizationService.currentActorHasPermission(code)).isTrue();

            committedChange.accept(operator, project);

            afAssertIdentityChangedDenied(() -> authorizationService.requirePermission(code));
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM temporary_permission_grants WHERE subject_id = ? AND status = 'active' AND NOT consumed",
                    Long.class, operator)).isEqualTo(1L);
        } finally {
            SecurityContextHolder.clearContext();
            jdbcTemplate.update("DELETE FROM temporary_permission_grants WHERE subject_id = ?", operator);
            jdbcTemplate.update("DELETE FROM access_users WHERE id = ?", operator);
        }
    }

    private void afAssertIdentityChangedDenied(org.assertj.core.api.ThrowableAssert.ThrowingCallable judgment) {
        assertThatThrownBy(judgment).isInstanceOfSatisfying(ApiException.class, exception -> {
            assertThat(exception.code()).isEqualTo("PERMISSION_DENIED");
            assertThat(exception.getMessage()).contains("账号归属或状态已变更");
        });
    }

    // M722-F03：岗位派生事实是判定过程中才按操作人读取的——跨项目变更恰在这次读取时由另一事务提交（旧岗位派生清空、挂起行作废、
    // 归属改到新项目，与 changeUserScope 同一组仓储写入），旧会话不得凭读取前拿到的旧岗位角色授予过判定。
    // 判定在单独一个事务里走真实仓储，只在读岗位角色处插入「另一事务提交」。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void staleSessionIsRejectedWhenCrossProjectMoveCommitsWhilePositionFactsAreRead() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String operator = "usr_af_p_" + suffix;
        String role = "role_af_p_" + suffix;
        String oldProject = "org_af_p_old_" + suffix;
        String newProject = "org_af_p_new_" + suffix;
        String code = "access.permission_template.apply";
        try {
            repository.createUser(new AccessUser(operator, operator, "staff", oldProject, null, "active", Instant.now()));
            repository.createRole(new RoleRecord(role, "AF 岗位角色 " + suffix, null, "project", "active", Instant.now(), List.of(code), null));
            jdbcTemplate.update("""
                    INSERT INTO user_position_assignments (user_id, position_code, staff_level_id, organization_id, assigned_by, assigned_at,
                                                           assigned_role_id)
                    VALUES (?, ?, 'lvl_m1_lower', ?, 'seed-admin', now(), ?)
                    """, operator, "pos_af_" + suffix, oldProject, role);
            repository.saveHighRiskHolds(operator, List.of(code), "岗位高危挂起", "seed-admin");
            authenticate(operator, operator, "staff", oldProject);
            // 对照：没有在途变更时，岗位角色授予被挂起压住——拒绝来自无权，不是身份核对
            assertThatThrownBy(() -> authorizationService.requirePermission(code))
                    .isInstanceOfSatisfying(ApiException.class, exception -> {
                        assertThat(exception.code()).isEqualTo("PERMISSION_DENIED");
                        assertThat(exception.getMessage()).doesNotContain("账号归属或状态已变更");
                    });

            java.util.concurrent.atomic.AtomicInteger positionReads = new java.util.concurrent.atomic.AtomicInteger();
            TransactionTemplate otherTransaction = new TransactionTemplate(transactionManager);
            otherTransaction.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            AccessRepository hooked = org.mockito.Mockito.mock(AccessRepository.class,
                    org.mockito.AdditionalAnswers.delegatesTo(repository));
            org.mockito.Mockito.doAnswer(invocation -> {
                if (positionReads.getAndIncrement() == 0) {
                    otherTransaction.executeWithoutResult(status -> {
                        Instant at = Instant.now();
                        repository.replacePositionGrants(operator, null, null, List.of(), "seed-admin", at);
                        repository.deleteHighRiskHolds(operator, List.of(code));
                        repository.updateUserScope(operator, newProject, null, "seed-admin", at);
                    });
                }
                return repository.findAssignedPositionRoleId(operator);
            }).when(hooked).findAssignedPositionRoleId(operator);
            AuthorizationService judging = new AuthorizationService(hooked);

            afAssertIdentityChangedDenied(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> judging.requirePermission(code)));
            assertThat(positionReads.get()).isEqualTo(1); // 岗位派生事实确在判定过程中被读取，变更就在那一刻提交
            assertThat(repository.findUserById(operator).orElseThrow().organizationId()).isEqualTo(newProject);
        } finally {
            SecurityContextHolder.clearContext();
            jdbcTemplate.update("DELETE FROM access_users WHERE id = ?", operator); // 岗位行、挂起行随账号级联删除
            jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id = ?", role);
            jdbcTemplate.update("DELETE FROM roles WHERE id = ?", role);
        }
    }

    // ── M722-F05（治理地图第 653、658 行；技术推导）真库回归：一次判定进行中账号归属改走又改回 ──────────────────────────────────
    //    验收②：两次范围变更与中间范围岗位角色的落地都由另一事务真实提交（走 changeUserScope 与审批落地所用的同一组仓储写入），
    //    断言中间范围的岗位角色授予已被读到、改回已提交、中间范围派生已清、原范围的新授权申请仍待审且没有已生效的新授权，requirePermission 拒绝。
    //    交错与断言的助手在 IdentityRoundTripJdbcIntegrationTests（与真实接口回归共用）。修复前两条都红（判定放行）。

    // 验收②「项目往返」：项目 A → 项目 B → 项目 A（都不带商户）。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void staleSessionIsRejectedWhenProjectMovesAwayAndBackWhileCapabilitiesAreRead() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        f05AssertScopeRoundTripDenied("p", suffix, "org_f05_pa_" + suffix, null, "org_f05_pb_" + suffix, null);
    }

    // 验收②「商户往返」：同一项目内商户 A → 商户 B → 商户 A。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void staleSessionIsRejectedWhenMerchantMovesAwayAndBackWhileCapabilitiesAreRead() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String project = "org_f05_m_" + suffix;
        f05AssertScopeRoundTripDenied("m", suffix, project, "mch_f05_ma_" + suffix, project, "mch_f05_mb_" + suffix);
    }

    // 操作人在原范围（岗位角色不含被判权限码）；判定在单独一个事务里走真实服务类＋真实仓储，只在读能力事实处插入另一事务的提交。
    private void f05AssertScopeRoundTripDenied(String tag, String suffix, String projectA, String merchantA,
                                               String projectB, String merchantB) {
        String operator = "usr_f05_" + tag + "_" + suffix;
        String roleA = "role_f05_" + tag + "a_" + suffix;
        String roleB = "role_f05_" + tag + "b_" + suffix;
        String code = "access.permission_template.apply";
        try {
            IdentityRoundTripJdbcIntegrationTests.seedRole(repository, roleA, List.of());
            IdentityRoundTripJdbcIntegrationTests.seedRole(repository, roleB, List.of(code));
            IdentityRoundTripJdbcIntegrationTests.seedStaff(repository, operator, projectA, merchantA, roleA, suffix);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    new AccessActor(operator, operator, "staff", projectA, merchantA, "active"), null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF"))));
            // 对照：没有在途变更时，原范围装配本就不含该码——拒绝来自无权，不是身份核对
            assertThatThrownBy(() -> authorizationService.requirePermission(code))
                    .isInstanceOfSatisfying(ApiException.class, exception -> {
                        assertThat(exception.code()).isEqualTo("PERMISSION_DENIED");
                        assertThat(exception.getMessage()).doesNotContain(IdentityRoundTripJdbcIntegrationTests.IDENTITY_CHANGED);
                    });

            AccessRepository hooked = org.mockito.Mockito.mock(AccessRepository.class,
                    org.mockito.AdditionalAnswers.delegatesTo(repository));
            IdentityRoundTripJdbcIntegrationTests.RoundTrip trip = IdentityRoundTripJdbcIntegrationTests.armScopeRoundTrip(
                    hooked, repository, jdbcTemplate, transactionManager, operator, projectA, merchantA, projectB, merchantB,
                    roleB, suffix);
            AuthorizationService judging = new AuthorizationService(hooked);

            afAssertIdentityChangedDenied(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> judging.requirePermission(code)));
            IdentityRoundTripJdbcIntegrationTests.assertRoundTripCommitted(repository, trip, operator, projectA, merchantA, roleB, code);
        } finally {
            SecurityContextHolder.clearContext();
            IdentityRoundTripJdbcIntegrationTests.cleanup(jdbcTemplate, List.of(operator), List.of(roleA, roleB), suffix);
        }
    }

    // ── M722-F06：最高权限的审批身份判定同样核对账号（真库）────────────────────────────────────────────────────────

    // 与 ProductionAuthService.issueMasterSession 每次 U 盘登录对最高权限账号行执行的 upsert 相同的列与冲突更新子句
    // （该服务只在生产配置下装配，这里用位置参数与 now() 重现）：四项不变也会把账号行改写一遍，行版本随之变化。
    private void f06MasterUsbLoginUpsert() {
        jdbcTemplate.update("""
                INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)
                VALUES (?, ?, ?, 'platform-company', NULL, 'active', now())
                ON CONFLICT (id) DO UPDATE SET
                    username = EXCLUDED.username,
                    account_type = EXCLUDED.account_type,
                    organization_id = COALESCE(access_users.organization_id, 'platform-company'),
                    status = 'active'
                """, SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE);
    }

    // 账号行稳定时最高权限的审批级别是 3；判定开始与末尾两次读账号行之间，本人再次 U 盘登录由另一事务把账号行改写了一遍
    // （四项不变、行版本变）→ 这次判定返回 0。数据真实提交，结束后清理。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void masterSupervisorLevelIsZeroWhenAnotherUsbLoginRewritesTheAccountRowDuringTheJudgment() {
        String master = SessionIssuer.MASTER_USER_ID;
        boolean existed = repository.findUserById(master).isPresent();
        try {
            f06MasterUsbLoginUpsert();
            f06AuthenticateMaster();
            // 对照：改写发生之前稳定为 3（走注入的真实服务；单列的守护见下一条用例）
            assertThat(authorizationService.currentSupervisorLevel()).isEqualTo(3);

            java.util.concurrent.atomic.AtomicInteger identityReads = new java.util.concurrent.atomic.AtomicInteger();
            TransactionTemplate otherTransaction = new TransactionTemplate(transactionManager);
            otherTransaction.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            AccessRepository hooked = org.mockito.Mockito.mock(AccessRepository.class,
                    org.mockito.AdditionalAnswers.delegatesTo(repository));
            org.mockito.Mockito.doAnswer(invocation -> {
                java.util.Optional<AccessRepository.UserIdentity> read = repository.findUserIdentity(master);
                if (identityReads.getAndIncrement() == 0) {
                    otherTransaction.executeWithoutResult(status -> f06MasterUsbLoginUpsert());
                }
                return read;
            }).when(hooked).findUserIdentity(master);
            AuthorizationService judging = new AuthorizationService(hooked);

            Integer level = new TransactionTemplate(transactionManager).execute(status -> judging.currentSupervisorLevel());
            assertThat(level).isZero();
            assertThat(identityReads.get()).isEqualTo(2); // 开始、末尾各读一次；改写落在两次之间
            AccessUser row = repository.findUserById(master).orElseThrow(); // 四项仍与会话相同
            assertThat(List.of(row.accountType(), row.organizationId(), row.status()))
                    .containsExactly(SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", "active");
            assertThat(row.merchantId()).isNull();
        } finally {
            SecurityContextHolder.clearContext();
            if (!existed) {
                jdbcTemplate.update("DELETE FROM access_users WHERE id = ?", master);
            }
        }
    }

    // 守护（修复前后都绿）：最高权限账号行稳定时，真库上的审批级别是 3。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void masterSupervisorLevelIsThreeOnTheRealTableWhenTheAccountRowIsStable() {
        String master = SessionIssuer.MASTER_USER_ID;
        boolean existed = repository.findUserById(master).isPresent();
        try {
            f06MasterUsbLoginUpsert();
            f06AuthenticateMaster();

            assertThat(authorizationService.currentSupervisorLevel()).isEqualTo(3);
        } finally {
            SecurityContextHolder.clearContext();
            if (!existed) {
                jdbcTemplate.update("DELETE FROM access_users WHERE id = ?", master);
            }
        }
    }

    // U 盘最高权限会话（与 ProductionAuthService 签发的主体同一组身份字段）。
    private void f06AuthenticateMaster() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                        "platform-company", null, "active"), null, List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF"))));
    }

    // ── XM-M002-BH（治理地图第 303、528、653 行；技术推导）真库＋真实接口回归 ─────────────────────────────────────────────

    // J1：同商户、另一组织的员工用他人的请求编号同体提交员工覆盖，不拿到他人首次结果，也不再按本人范围去执行——
    // M724（操作员 2026-10-04 裁决「同一提交编号只能提交一次不允许多次提交」）：该编号已有一次成功，其他范围再用即 409
    // IDEMPOTENCY_KEY_USED，进不到动作（修复前进入动作后被 CROSS_ORG_FORBIDDEN 拒绝，且多占一行）；本人重试仍只认一次。
    // 旧记录守卫单列在下一条用例。
    @Test
    void sameMerchantStaffInAnotherOrganizationCannotReplayAPermissionOverrideThroughTheEndpoint() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String merchant = "mch_bh_" + suffix;
        String target = "usr_bh_t_" + suffix;
        String a = "usr_bh_a_" + suffix;
        String b = "usr_bh_b_" + suffix;
        String role = "role_bh_assign_" + suffix;
        repository.createRole(new RoleRecord(role, "BH 装配角色 " + suffix, null, "project", "active", Instant.now(),
                List.of("access.role.assign"), null));
        repository.createUser(new AccessUser(target, target, "staff", "org", merchant, "active", Instant.now()));
        repository.createUser(new AccessUser(a, a, "staff", "org", merchant, "active", Instant.now()));
        repository.createUser(new AccessUser(b, b, "staff", "org_other", merchant, "active", Instant.now()));
        repository.assignUserRole(a, role);
        repository.assignUserRole(b, role);
        var sessionA = m002Session(new AccessActor(a, a, "staff", "org", merchant, "active"));
        var sessionB = m002Session(new AccessActor(b, b, "staff", "org_other", merchant, "active"));
        String key = "bh-j1-" + suffix;
        String overridesOfTarget = "SELECT count(*) FROM user_permission_overrides WHERE user_id = ?";
        var json = new com.fasterxml.jackson.databind.ObjectMapper();

        // 1) A 首次提交成功
        String first = m002IdCall("set", target, null, key, sessionA).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(jdbcTemplate.queryForObject(overridesOfTarget, Long.class, target)).isEqualTo(1L);

        // 2) B 用同一编号、同一请求体：该编号已被用掉，被拒且没进入动作（进入动作会是 403 CROSS_ORG_FORBIDDEN）
        m002IdCall("set", target, null, key, sessionB)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("IDEMPOTENCY_KEY_USED"))
                .andExpect(jsonPath("$.message").value("该提交编号已被使用，本次操作未执行"));

        // 3) A 重试：返回首次结果，不重复生效
        String replay = m002IdCall("set", target, null, key, sessionA).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(json.readTree(first).get("data")).isNotNull();
        assertThat(json.readTree(replay).get("data")).isEqualTo(json.readTree(first).get("data"));
        assertThat(jdbcTemplate.queryForObject(overridesOfTarget, Long.class, target)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE idempotency_key = ? AND event_result = 'success'", Long.class, key)).isEqualTo(1L);

        // 4) 该编号下恰一条幂等记录：A 的那条成功行，行键是不含范围的定长形态（once:＋操作号散列，长 69），B 被拒的那次不占行
        String storedId = com.complex.platform.common.idempotency.IdempotencyService.storedOperationId(
                "userPermissionOverride:set:" + target);
        assertThat(storedId).hasSize(69);
        assertThat(jdbcTemplate.queryForList(
                "SELECT operation_id || '/' || response_code || '/' || length(request_hash) FROM idempotency_records WHERE idempotency_key = ?",
                String.class, key)).containsExactly(storedId + "/OK/128");
    }

    // J1 旧记录守卫（单列，修复前自己会红在文案断言上）：修复前留下的 m:<商户号> 旧记录由既有守卫拦下——不回放、不执行、不改旧行。
    @Test
    void legacyMerchantScopedIdempotencyRecordIsBlockedByTheGuardThroughTheEndpoint() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String merchant = "mch_bh_" + suffix;
        String target = "usr_bh_t_" + suffix;
        String a = "usr_bh_a_" + suffix;
        String role = "role_bh_assign_" + suffix;
        repository.createRole(new RoleRecord(role, "BH 装配角色 " + suffix, null, "project", "active", Instant.now(),
                List.of("access.role.assign"), null));
        repository.createUser(new AccessUser(target, target, "staff", "org", merchant, "active", Instant.now()));
        repository.createUser(new AccessUser(a, a, "staff", "org", merchant, "active", Instant.now()));
        repository.assignUserRole(a, role);
        var sessionA = m002Session(new AccessActor(a, a, "staff", "org", merchant, "active"));
        String oldKey = "bh-j1-old-" + suffix;
        String oldOperationId = "m:" + merchant + ":userPermissionOverride:set:" + target;
        jdbcTemplate.update("""
                INSERT INTO idempotency_records (id, operation_id, idempotency_key, request_hash, response_code, response_body,
                                                 http_status, created_at, expires_at)
                VALUES (?, ?, ?, 'x', 'IN_PROGRESS', NULL, 202, now(), now() + interval '1 day')
                """, UUID.randomUUID(), oldOperationId, oldKey);

        m002IdCall("set", target, null, oldKey, sessionA)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("IDEMPOTENCY_CONFLICT"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("该请求编号存在旧处理记录")));
        assertThat(jdbcTemplate.queryForList(
                "SELECT operation_id || '/' || response_code FROM idempotency_records WHERE idempotency_key = ?", String.class, oldKey))
                .containsExactly(oldOperationId + "/IN_PROGRESS");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM user_permission_overrides WHERE user_id = ?", Long.class, target))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE idempotency_key = ? AND event_result = 'success'", Long.class, oldKey)).isZero();
    }

    // J2：商户主体的长编号（与生产编号形态一致）拼出的幂等操作编号原先超过列宽 VARCHAR(128)、预留即报错；修复后为定长
    // （M724 起行键不含范围：once:＋操作号散列，长 69）。
    // 第 1 步用同一套装配的短编号证明装配是足的（修复前后都成功），第 2 步的失败只可能来自列宽。
    @Test
    void merchantCallerLongOperationIdNoLongerOverflowsTheColumn() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String merchant = "mch_" + UUID.randomUUID();
        String a = "usr_bh_j2_" + suffix;
        String assignRole = "role_bh_j2_" + suffix;
        repository.createRole(new RoleRecord(assignRole, "BH J2 装配角色 " + suffix, null, "project", "active", Instant.now(),
                List.of("access.role.assign"), null));
        repository.createUser(new AccessUser(a, a, "staff", "org", merchant, "active", Instant.now()));
        repository.assignUserRole(a, assignRole);
        var sessionA = m002Session(new AccessActor(a, a, "staff", "org", merchant, "active"));
        String shortTarget = "usr_bh_s_" + suffix;
        String shortRole = "role_bh_s_" + suffix;
        String longTarget = "usr_staff_" + UUID.randomUUID();
        String longRole = "role_" + UUID.randomUUID();
        for (String[] pair : List.of(new String[] {shortTarget, shortRole}, new String[] {longTarget, longRole})) {
            repository.createUser(new AccessUser(pair[0], pair[0], "staff", "org", merchant, "active", Instant.now()));
            repository.createRole(new RoleRecord(pair[1], "BH J2 角色 " + pair[1], null, "project", "active", Instant.now(), List.of(), null));
            repository.assignUserRole(pair[0], pair[1]);
        }
        assertThat(("m:" + merchant + ":unassignUserRole:" + shortTarget + ":" + shortRole).length()).isLessThanOrEqualTo(128);
        assertThat(("m:" + merchant + ":unassignUserRole:" + longTarget + ":" + longRole).length()).isGreaterThan(128);

        // 1) 装配对照：短编号
        m002IdCall("unassign", shortTarget, shortRole, "bh-j2-short-" + suffix, sessionA).andExpect(status().isOk());
        assertThat(m002UserRoles(shortTarget)).doesNotContain(shortRole);

        // 2) 长编号
        String longKey = "bh-j2-long-" + suffix;
        m002IdCall("unassign", longTarget, longRole, longKey, sessionA).andExpect(status().isOk());
        assertThat(m002UserRoles(longTarget)).doesNotContain(longRole);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT operation_id FROM idempotency_records WHERE idempotency_key = ?", String.class, longKey))
                .isEqualTo(com.complex.platform.common.idempotency.IdempotencyService.storedOperationId(
                        "unassignUserRole:" + longTarget + ":" + longRole))
                .hasSize(69);
    }

    // ── M722-F04（M722-F02 验收证据补齐，不改实现）：经生产装配的幂等服务（真实幂等表，作用域由 AuthorizationService 解析）──────────

    // M724（操作员 2026-10-04 裁决「同一商户、不同项目的两名员工用同一个提交编号，现在各执行一次？？？同一提交编号只能提交一次不允许多次提交」）：
    // 同一商户、两个项目、两名在职员工，相同操作号、幂等键、请求体：只有第一人执行；第二人两次提交都 409 IDEMPOTENCY_KEY_USED、
    // 不执行、拿不到第一人的结果；第一人重试返回第一次的结果；该键恰一条成功行。修复前两人各执行一次、该键两条成功行。
    @Test
    void sameMerchantStaffInTwoProjectsOnlyTheFirstCallerExecutesAndTheSecondIsRejectedOnTheRealTable() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String merchant = "mch_bh_k_" + suffix;
        bhAssertOnlyTheFirstCallerExecutesAndTheSecondIsRejected(suffix,
                new AccessActor("usr_bh_ka_" + suffix, "usr_bh_ka_" + suffix, "staff", "org_bh_k1_" + suffix, merchant, "active"),
                new AccessActor("usr_bh_kb_" + suffix, "usr_bh_kb_" + suffix, "staff", "org_bh_k2_" + suffix, merchant, "active"));
    }

    // 同上，另一组：同一项目、两个不同商户的员工，同样只有第一人执行、第二人被拒（修复前两人各执行一次）。
    @Test
    void staffOfDifferentMerchantsOnlyTheFirstCallerExecutesAndTheSecondIsRejectedOnTheRealTable() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        bhAssertOnlyTheFirstCallerExecutesAndTheSecondIsRejected(suffix,
                new AccessActor("usr_bh_ma_" + suffix, "usr_bh_ma_" + suffix, "staff", "org_bh_m_" + suffix, "mch_bh_m1_" + suffix, "active"),
                new AccessActor("usr_bh_mb_" + suffix, "usr_bh_mb_" + suffix, "staff", "org_bh_m_" + suffix, "mch_bh_m2_" + suffix, "active"));
    }

    private void bhAssertOnlyTheFirstCallerExecutesAndTheSecondIsRejected(String suffix, AccessActor first, AccessActor second) {
        for (AccessActor caller : List.of(first, second)) {
            repository.createUser(new AccessUser(caller.id(), caller.username(), caller.accountType(), caller.organizationId(),
                    caller.merchantId(), caller.status(), Instant.now()));
        }
        String operationId = "m722F04:" + suffix;
        String key = "bh-k-" + suffix;
        List<String> executed = new java.util.ArrayList<>();

        assertThat(bhExecuteAs(first, operationId, key, executed)).isEqualTo(first.id());
        for (int attempt = 0; attempt < 2; attempt++) {
            assertThatThrownBy(() -> bhExecuteAs(second, operationId, key, executed))
                    .isInstanceOfSatisfying(ApiException.class, exception -> {
                        assertThat(exception.status().value()).isEqualTo(409);
                        assertThat(exception.code()).isEqualTo("IDEMPOTENCY_KEY_USED");
                        assertThat(exception.getMessage()).isEqualTo("该提交编号已被使用，本次操作未执行");
                    });
        }
        assertThat(bhExecuteAs(first, operationId, key, executed)).isEqualTo(first.id());

        assertThat(executed).containsExactly(first.id());
        assertThat(jdbcTemplate.queryForList(
                "SELECT operation_id || '/' || response_code FROM idempotency_records WHERE idempotency_key = ?", String.class, key))
                .containsExactly(com.complex.platform.common.idempotency.IdempotencyService.storedOperationId(operationId)
                        + "/OK"); // 该键恰一条：第一人的成功行
    }

    // M722-F07（操作员 2026-10-03 裁决「同项目同商户内认一次」）：同一项目、同一商户的两名在职员工用相同操作号、幂等键、请求体——
    // 第二人不再执行、拿到第一次的结果；第一人重试同样；库内该键恰一条记录。
    @Test
    void sameProjectSameMerchantStaffAreCountedOnceAndTheSecondGetsTheFirstResultOnTheRealTable() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String project = "org_bh_s_" + suffix;
        String merchant = "mch_bh_s_" + suffix;
        AccessActor first = new AccessActor("usr_bh_sa_" + suffix, "usr_bh_sa_" + suffix, "staff", project, merchant, "active");
        AccessActor second = new AccessActor("usr_bh_sb_" + suffix, "usr_bh_sb_" + suffix, "staff", project, merchant, "active");
        for (AccessActor caller : List.of(first, second)) {
            repository.createUser(new AccessUser(caller.id(), caller.username(), caller.accountType(), caller.organizationId(),
                    caller.merchantId(), caller.status(), Instant.now()));
        }
        String operationId = "m722F07:" + suffix;
        String key = "bh-s-" + suffix;
        List<String> executed = new java.util.ArrayList<>();

        assertThat(bhExecuteAs(first, operationId, key, executed)).isEqualTo(first.id());
        assertThat(bhExecuteAs(second, operationId, key, executed)).isEqualTo(first.id());
        assertThat(bhExecuteAs(first, operationId, key, executed)).isEqualTo(first.id());

        assertThat(executed).containsExactly(first.id());
        assertThat(jdbcTemplate.queryForList(
                "SELECT operation_id || '/' || response_code FROM idempotency_records WHERE idempotency_key = ?", String.class, key))
                .containsExactly(com.complex.platform.common.idempotency.IdempotencyService.storedOperationId(operationId)
                        + "/OK"); // M724：行键不含范围（once:＋操作号散列，长 69）
    }

    // 以该主体身份提交一次；动作把本人编号记进 executed 并作为结果返回。
    private String bhExecuteAs(AccessActor caller, String operationId, String key, List<String> executed) {
        String[] result = new String[1];
        m002ActAs(caller, () -> result[0] = idempotencyService.execute(operationId, key, java.util.Map.of("effect", "deny"), () -> {
            executed.add(caller.id());
            return caller.id();
        }, String.class));
        return result[0];
    }

    // 不带商户归属的员工，提交范围仍是基线形态 u:<编号长度>:<编号>:<组织>；本人重试只认一次。
    // M724 起行键不含范围（once:＋操作号散列，长 69），范围的 SHA-256 记在请求散列的前 64 位（请求散列共 128 位）。
    @Test
    void nonMerchantCallerKeepsTheBaselineIdempotencyScopeOnTheRealTable() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AccessActor caller = new AccessActor("usr_bh_n_" + suffix, "usr_bh_n_" + suffix, "staff", "org_bh_n_" + suffix, null, "active");
        repository.createUser(new AccessUser(caller.id(), caller.username(), "staff", caller.organizationId(), null, "active", Instant.now()));
        String operationId = "m722F04:" + suffix;
        String key = "bh-n-" + suffix;
        List<String> executed = new java.util.ArrayList<>();

        assertThat(bhExecuteAs(caller, operationId, key, executed)).isEqualTo(caller.id());
        assertThat(bhExecuteAs(caller, operationId, key, executed)).isEqualTo(caller.id());

        assertThat(executed).containsExactly(caller.id());
        String baselineScope = "u:" + caller.id().length() + ":" + caller.id() + ":" + caller.organizationId();
        String expected = com.complex.platform.common.idempotency.IdempotencyService.storedOperationId(operationId);
        assertThat(expected).hasSize(69);
        assertThat(jdbcTemplate.queryForList(
                "SELECT operation_id FROM idempotency_records WHERE idempotency_key = ?", String.class, key)).containsExactly(expected);
        String scopeHash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(
                baselineScope.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT request_hash FROM idempotency_records WHERE idempotency_key = ?", String.class, key))
                .startsWith(scopeHash).hasSize(128);
    }

    // XM-M002-I（操作员 2026-09-25 裁定口径 B「一律只认长期来源」）：真实接口回归——只凭临时授权或单次模板套用持有测试码的员工，经员工单独授予、
    // 解除员工显式拒绝、建角色、配角色、定岗（岗位临时模板）、修改临时模板内容、套用临时模板、临时授权（授给别人或自己、单次或多次）各接口
    // 都被 GRANT_EXCEEDS_OWN_PERMISSIONS 拒绝，换幂等键重试同样；平台管理员只凭临时授权持有非日常码时保存岗位映射同样被拒；
    // 改由角色长期持有后各接口照常成功。
    @Test
    void temporaryOrSingleUseHolderCannotGrantThroughTheEndpoints() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String daily = "xm002i.grant_test.daily_" + suffix;
        String high = "xm002i.grant_test.high_" + suffix; // 非日常码：平台管理员不随身份持有
        jdbcTemplate.update("""
                INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
                VALUES (?, 'XM-M002-I 测试日常权限', 'ops', 'medium', FALSE, 'active'),
                       (?, 'XM-M002-I 测试非日常权限', 'ops', 'high', FALSE, 'active')
                """, daily, high);
        String gateRole = "role_xm002i_gate_" + suffix;
        String dailyRole = "role_xm002i_daily_" + suffix;
        String highRole = "role_xm002i_high_" + suffix;
        String blankRole = "role_xm002i_blank_" + suffix;
        repository.createRole(new RoleRecord(gateRole, "XM-M002-I 门禁角色 " + suffix, null, "project", "active", Instant.now(),
                List.of("access.role.assign", "access.role.manage", "access.temporary_grant.create",
                        "access.permission_template.apply", "access.permission_template.manage"), null));
        repository.createRole(new RoleRecord(dailyRole, "XM-M002-I 日常角色 " + suffix, null, "project", "active", Instant.now(),
                List.of(daily), null));
        repository.createRole(new RoleRecord(highRole, "XM-M002-I 非日常角色 " + suffix, null, "project", "active", Instant.now(),
                List.of(high), null));
        repository.createRole(new RoleRecord(blankRole, "XM-M002-I 空角色 " + suffix, null, "project", "active", Instant.now(),
                List.of(), null));
        String tempHolder = "usr_xm002i_temp_" + suffix;
        String onceHolder = "usr_xm002i_once_" + suffix;
        String target = "usr_xm002i_target_" + suffix;
        String denyTarget = "usr_xm002i_deny_" + suffix;
        String admin = "usr_xm002i_admin_" + suffix;
        for (String user : List.of(tempHolder, onceHolder, target, denyTarget)) {
            repository.createUser(new AccessUser(user, user, "staff", "org", null, "active", Instant.now()));
        }
        repository.createUser(new AccessUser(admin, admin, "platform_admin", "org", null, "active", Instant.now()));
        for (String user : List.of(tempHolder, onceHolder, admin)) {
            repository.assignUserRole(user, gateRole);
        }
        repository.assignUserRole(denyTarget, dailyRole);
        repository.saveUserPermissionOverrides(denyTarget, List.of(daily), AccessRepository.Effect.DENY, "停权", "seed-admin");
        Instant until = Instant.parse("2099-01-01T00:00:00Z");
        repository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), tempHolder, List.of(daily),
                Instant.now().minusSeconds(3600), until, false, false, "active", "临时授权", "seed-admin", Instant.now(), null, null));
        repository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), admin, List.of(high),
                Instant.now().minusSeconds(3600), until, false, false, "active", "临时授权", "seed-admin", Instant.now(), null, null));
        String onceTemplate = "tpl_xm002i_once_" + suffix;
        String tempTemplate = "tpl_xm002i_temp_" + suffix;
        String editTemplate = "tpl_xm002i_edit_" + suffix;
        String blankTemplate = "tpl_xm002i_blank_" + suffix;
        repository.createTemplate(baseTemplate(onceTemplate), grants(onceTemplate, daily));
        repository.createTemplate(xm002iOrgTemporaryTemplate(tempTemplate, until), grants(tempTemplate, daily));
        repository.createTemplate(xm002iOrgTemporaryTemplate(editTemplate, until), List.of());
        repository.createTemplate(baseTemplate(blankTemplate), List.of());
        repository.applyTemplate(onceTemplate, List.of(onceHolder), null, null, true, "seed-admin");
        String position = "xm002i_pos_" + suffix;
        repository.savePositionRoleTemplate(new PositionRoleTemplate("prt_xm002i_" + suffix, position, "lvl_m1_lower", blankRole,
                tempTemplate, "org", "active", "seed-admin", Instant.now(), Instant.now()));

        for (String holder : List.of(tempHolder, onceHolder)) {
            var session = m002Session(new AccessActor(holder, holder, "staff", "org", null, "active"));
            for (String attempt : List.of("a", "b")) { // 换幂等键重试
                String key = "xm002i-" + suffix + "-" + holder.substring(11, 15) + "-" + attempt; // temp / once
                xm002iExpectGrantExceeds(post("/api/v1/access/users/" + target + "/permission-overrides").with(session)
                        .contentType(MediaType.APPLICATION_JSON).content(xm002iOverrideJson(key + "-override", daily)));
                xm002iExpectGrantExceeds(post("/api/v1/access/users/" + denyTarget + "/permission-overrides/remove").with(session)
                        .contentType(MediaType.APPLICATION_JSON).content(m002DenyJson(key + "-undeny", daily)));
                xm002iExpectGrantExceeds(post("/api/v1/access/roles").with(session)
                        .contentType(MediaType.APPLICATION_JSON).content(xm002iRoleJson(key + "-role", "临时依据角色 " + key, daily)));
                xm002iExpectGrantExceeds(post("/api/v1/access/role-assignments").with(session)
                        .contentType(MediaType.APPLICATION_JSON).content(m002UnassignJson(key + "-assign", target, dailyRole)));
                xm002iExpectGrantExceeds(put("/api/v1/access/users/" + target + "/position").with(session)
                        .contentType(MediaType.APPLICATION_JSON).content(xm002iPositionJson(key + "-position", position)));
                xm002iExpectGrantExceeds(put("/api/v1/access/permission-templates/" + editTemplate).with(session)
                        .contentType(MediaType.APPLICATION_JSON).content(xm002iEditJson(key + "-edit", daily, until)));
                for (String grantee : List.of(target, holder)) { // 含授给自己
                    xm002iExpectGrantExceeds(post("/api/v1/access/permission-templates/" + tempTemplate + "/apply").with(session)
                            .contentType(MediaType.APPLICATION_JSON).content(xm002iApplyJson(key + "-apply-" + grantee, grantee)));
                    for (boolean singleUse : List.of(true, false)) {
                        xm002iExpectGrantExceeds(post("/api/v1/access/temporary-grants").with(session).contentType(MediaType.APPLICATION_JSON)
                                .content(xm002iTemporaryJson(key + "-temporary-" + singleUse + "-" + grantee, grantee, daily,
                                        until.minusSeconds(3600), singleUse)));
                    }
                }
            }
        }
        var adminSession = m002Session(new AccessActor(admin, admin, "platform_admin", "org", null, "active"));
        for (String attempt : List.of("a", "b")) {
            xm002iExpectGrantExceeds(post("/api/v1/access/position-role-templates").with(adminSession).contentType(MediaType.APPLICATION_JSON)
                    .content(xm002iMappingJson("xm002i-" + suffix + "-map-" + attempt, position + "_admin", highRole, blankTemplate)));
        }
        assertThat(jdbcTemplate.queryForList("SELECT permission_code FROM user_permission_overrides WHERE user_id = ?", String.class, target))
                .isEmpty();
        assertThat(denyOrigins(denyTarget, daily)).containsExactly("explicit");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM roles WHERE role_name LIKE ?", Integer.class,
                "临时依据角色 xm002i-" + suffix + "%")).isZero();
        assertThat(m002UserRoles(target)).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM user_position_assignments WHERE user_id = ?", Integer.class,
                target)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM permission_template_items WHERE template_id = ?", Integer.class,
                editTemplate)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM permission_template_applications WHERE template_id = ?",
                Integer.class, tempTemplate)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM temporary_permission_grants WHERE reason = 'XM-M002-I' AND subject_id IN (?, ?, ?)",
                Integer.class, target, tempHolder, onceHolder)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM position_role_templates WHERE position_code = ?", Integer.class,
                position + "_admin")).isZero();

        repository.assignUserRole(tempHolder, dailyRole); // 角色＝长期来源
        repository.assignUserRole(admin, highRole);
        var tempSession = m002Session(new AccessActor(tempHolder, tempHolder, "staff", "org", null, "active"));
        String ok = "xm002i-" + suffix + "-ok";
        xm002iExpectOk(post("/api/v1/access/users/" + target + "/permission-overrides").with(tempSession)
                .contentType(MediaType.APPLICATION_JSON).content(xm002iOverrideJson(ok + "-override", daily)));
        xm002iExpectOk(post("/api/v1/access/users/" + denyTarget + "/permission-overrides/remove").with(tempSession)
                .contentType(MediaType.APPLICATION_JSON).content(m002DenyJson(ok + "-undeny", daily)));
        xm002iExpectOk(post("/api/v1/access/roles").with(tempSession).contentType(MediaType.APPLICATION_JSON)
                .content(xm002iRoleJson(ok + "-role", "长期依据角色 " + suffix, daily)));
        xm002iExpectOk(post("/api/v1/access/role-assignments").with(tempSession).contentType(MediaType.APPLICATION_JSON)
                .content(m002UnassignJson(ok + "-assign", target, dailyRole)));
        xm002iExpectOk(put("/api/v1/access/users/" + target + "/position").with(tempSession).contentType(MediaType.APPLICATION_JSON)
                .content(xm002iPositionJson(ok + "-position", position)));
        xm002iExpectOk(put("/api/v1/access/permission-templates/" + editTemplate).with(tempSession).contentType(MediaType.APPLICATION_JSON)
                .content(xm002iEditJson(ok + "-edit", daily, until)));
        xm002iExpectOk(post("/api/v1/access/permission-templates/" + tempTemplate + "/apply").with(tempSession)
                .contentType(MediaType.APPLICATION_JSON).content(xm002iApplyJson(ok + "-apply", target)));
        xm002iExpectOk(post("/api/v1/access/temporary-grants").with(tempSession).contentType(MediaType.APPLICATION_JSON)
                .content(xm002iTemporaryJson(ok + "-temporary", target, daily, until.minusSeconds(3600), false)));
        xm002iExpectOk(post("/api/v1/access/position-role-templates").with(adminSession).contentType(MediaType.APPLICATION_JSON)
                .content(xm002iMappingJson(ok + "-map", position + "_admin", highRole, blankTemplate)));
        assertThat(jdbcTemplate.queryForList(
                "SELECT effect || '/' || origin FROM user_permission_overrides WHERE user_id = ? AND permission_code = ?",
                String.class, target, daily)).containsExactly("grant/explicit");
        assertThat(denyOrigins(denyTarget, daily)).isEmpty();
        assertThat(jdbcTemplate.queryForList("""
                SELECT rp.permission_code FROM roles r JOIN role_permissions rp ON rp.role_id = r.id
                 WHERE r.role_name = ? AND r.organization_id = 'org'
                """, String.class, "长期依据角色 " + suffix)).containsExactly(daily);
        assertThat(m002UserRoles(target)).containsExactly(dailyRole);
        assertThat(jdbcTemplate.queryForList("SELECT position_code FROM user_position_assignments WHERE user_id = ?", String.class, target))
                .containsExactly(position);
        assertThat(jdbcTemplate.queryForList("SELECT permission_code FROM permission_template_items WHERE template_id = ?", String.class,
                editTemplate)).containsExactly(daily);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM permission_template_applications WHERE template_id = ? AND target_id = ? AND status = 'active'",
                Integer.class, tempTemplate, target)).isEqualTo(2); // 定岗派生 + 直接套用
        assertThat(jdbcTemplate.queryForList(
                "SELECT permission_code FROM temporary_permission_grants WHERE reason = 'XM-M002-I' AND subject_id = ?", String.class, target))
                .containsExactly(daily);
        assertThat(jdbcTemplate.queryForList("SELECT role_id FROM position_role_templates WHERE position_code = ?", String.class,
                position + "_admin")).containsExactly(highRole);
    }

    // 本项目（org）自有、到期为 validUntil 的临时模板。
    private PermissionTemplate xm002iOrgTemporaryTemplate(String id, Instant validUntil) {
        Instant at = Instant.parse("2026-06-14T00:00:00Z");
        return new PermissionTemplate(id, id, id, "XM-M002-I test template", "temporary", null, "organization", "org", "merge",
                null, validUntil, false, "active", "integration test", "seed-admin", at, at, at);
    }

    // M723-F12（治理地图第 656 行「临时或单次得到的权限只供本人在有效期内或单次使用，不能再授给他人」；M723-F02 验收「一律 403
    // GRANT_EXCEEDS_OWN_PERMISSIONS」）：须审批的权限码也一样。只凭临时授权、单次临时授权、带失效时间的模板套用持有该码的三名员工，
    // 经员工单独授予接口直接授给同范围的他人——403 GRANT_EXCEEDS_OWN_PERMISSIONS（不是 GRANT_REQUIRES_APPROVAL），换请求编号重试同样；
    // 目标没有任何单独授予，单次来源未消耗、没有消耗留痕。对照：另有长期来源（角色）后改为 GRANT_REQUIRES_APPROVAL（引导去走授权申请）；
    // 从未持有该码的员工照旧 GRANT_REQUIRES_APPROVAL。修复前三类持有人都先被「须审批」拦下，返回 GRANT_REQUIRES_APPROVAL。
    @Test
    void aHolderThroughTemporaryOrSingleUseSourcesCannotDirectlyGrantACodeThatNeedsApproval() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String code = "m723f12.needs_approval_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
                VALUES (?, 'M723-F12 须审批测试权限', 'ops', 'high', TRUE, 'active')
                """, code);
        String gateRole = "role_m723f12_gate_" + suffix;
        String codeRole = "role_m723f12_code_" + suffix;
        repository.createRole(new RoleRecord(gateRole, "M723-F12 门禁角色 " + suffix, null, "project", "active", Instant.now(),
                List.of("access.role.assign"), null));
        repository.createRole(new RoleRecord(codeRole, "M723-F12 持码角色 " + suffix, null, "project", "active", Instant.now(),
                List.of(code), null));
        String tempHolder = "usr_m723f12_temp_" + suffix;
        String onceHolder = "usr_m723f12_once_" + suffix;
        String deadlineHolder = "usr_m723f12_dead_" + suffix;
        String nonHolder = "usr_m723f12_none_" + suffix;
        String target = "usr_m723f12_target_" + suffix;
        for (String user : List.of(tempHolder, onceHolder, deadlineHolder, nonHolder, target)) {
            repository.createUser(new AccessUser(user, user, "staff", "org", null, "active", Instant.now()));
        }
        for (String user : List.of(tempHolder, onceHolder, deadlineHolder, nonHolder)) {
            repository.assignUserRole(user, gateRole);
        }
        Instant until = Instant.parse("2099-01-01T00:00:00Z");
        repository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), tempHolder, List.of(code),
                Instant.now().minusSeconds(3600), until, false, false, "active", "临时授权", "seed-admin", Instant.now(), null, null));
        repository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), onceHolder, List.of(code),
                Instant.now().minusSeconds(3600), until, true, false, "active", "单次临时授权", "seed-admin", Instant.now(), null, null));
        String deadlineTemplate = "tpl_m723f12_" + suffix;
        repository.createTemplate(xm002iOrgTemporaryTemplate(deadlineTemplate, until), grants(deadlineTemplate, code));
        repository.applyTemplate(deadlineTemplate, List.of(deadlineHolder), null, null, false, "seed-admin");
        String url = "/api/v1/access/users/" + target + "/permission-overrides";

        int n = 0;
        for (String holder : List.of(tempHolder, onceHolder, deadlineHolder)) {
            var session = m002Session(new AccessActor(holder, holder, "staff", "org", null, "active"));
            for (String attempt : List.of("a", "b")) { // 换请求编号重试
                xm002iExpectGrantExceeds(post(url).with(session).contentType(MediaType.APPLICATION_JSON)
                        .content(xm002iOverrideJson("m723f12-" + suffix + "-" + (n++) + "-" + attempt, code)));
            }
        }
        assertThat(jdbcTemplate.queryForList("SELECT permission_code FROM user_permission_overrides WHERE user_id = ?", String.class, target))
                .isEmpty();
        assertThat(jdbcTemplate.queryForList("SELECT consumed FROM temporary_permission_grants WHERE subject_id = ?", Boolean.class,
                onceHolder)).containsExactly(false);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM permission_template_usage_logs WHERE user_id IN (?, ?, ?) AND result = 'consumed'", Integer.class,
                tempHolder, onceHolder, deadlineHolder)).isZero();

        // 对照一：另有长期来源（角色）——照旧按「须审批」拒绝，引导去走授权申请
        repository.assignUserRole(tempHolder, codeRole);
        mockMvc.perform(post(url).with(m002Session(new AccessActor(tempHolder, tempHolder, "staff", "org", null, "active")))
                        .contentType(MediaType.APPLICATION_JSON).content(xm002iOverrideJson("m723f12-" + suffix + "-long", code)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("GRANT_REQUIRES_APPROVAL"));
        // 对照二：从未持有该码——照旧按「须审批」拒绝
        mockMvc.perform(post(url).with(m002Session(new AccessActor(nonHolder, nonHolder, "staff", "org", null, "active")))
                        .contentType(MediaType.APPLICATION_JSON).content(xm002iOverrideJson("m723f12-" + suffix + "-none", code)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("GRANT_REQUIRES_APPROVAL"));
        assertThat(jdbcTemplate.queryForList("SELECT permission_code FROM user_permission_overrides WHERE user_id = ?", String.class, target))
                .isEmpty();
    }

    private void xm002iExpectGrantExceeds(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("GRANT_EXCEEDS_OWN_PERMISSIONS"));
    }

    private void xm002iExpectOk(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isOk());
    }

    private String xm002iOverrideJson(String key, String code) {
        return """
                {"idempotency_key": "%s", "permission_codes": ["%s"], "effect": "grant", "reason": "XM-M002-I"}
                """.formatted(key, code);
    }

    private String xm002iRoleJson(String key, String roleName, String code) {
        return """
                {"idempotency_key": "%s", "role_name": "%s", "permission_codes": ["%s"], "data_scope": "project"}
                """.formatted(key, roleName, code);
    }

    private String xm002iTemporaryJson(String key, String target, String code, Instant validUntil, boolean singleUse) {
        return """
                {"idempotency_key": "%s", "target_user_id": "%s", "permission_codes": ["%s"], "valid_until": "%s",
                 "single_use": %s, "reason": "XM-M002-I"}
                """.formatted(key, target, code, validUntil, singleUse);
    }

    private String xm002iPositionJson(String key, String position) {
        return """
                {"idempotency_key": "%s", "position_code": "%s", "staff_level_id": "lvl_m1_lower"}
                """.formatted(key, position);
    }

    private String xm002iEditJson(String key, String code, Instant validUntil) {
        return """
                {"idempotency_key": "%s", "template_type": "temporary", "permission_codes": ["%s"], "data_scope": "project",
                 "valid_until": "%s", "single_use": false}
                """.formatted(key, code, validUntil);
    }

    private String xm002iApplyJson(String key, String target) {
        return """
                {"idempotency_key": "%s", "target_user_ids": ["%s"], "mode": "merge"}
                """.formatted(key, target);
    }

    private String xm002iMappingJson(String key, String position, String roleId, String templateId) {
        return """
                {"idempotency_key": "%s", "position_code": "%s", "staff_level_id": "lvl_m1_lower", "role_id": "%s",
                 "template_id": "%s", "status": "active"}
                """.formatted(key, position, roleId, templateId);
    }

    // ── M736（XM-M001-D 可跨项目角色·批 1 权限治理；操作员 2026-10-01 确认的治理地图第 653 行追加句：「被分配了可跨项目角色的员工
    //    （含经岗位得到），在所分配的权限内可以查看、处理该角色可查看项目的数据。」）真库与真实接口回归 ───────────────────────────────
    //    夹具随测试事务回滚：项目表里三个真实项目（甲、乙、丙）各带一名员工，一名持 role_platform_admin 的平台管理员。
    //    可跨项目角色一律经 POST /api/v1/access/roles 建立（两项勾选走请求体），不直写 roles 表的新列；分配、撤销角色的请求体同形，
    //    沿用 m002UnassignJson。

    private record M736Fixture(String suffix, String admin, String projectA, String projectB, String projectC,
                               String staffA, String staffB, String staffC) {
    }

    private M736Fixture m736Fixture() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        M736Fixture f = new M736Fixture(suffix, "usr_m736_admin_" + suffix, "prj_m736_a_" + suffix, "prj_m736_b_" + suffix,
                "prj_m736_c_" + suffix, "usr_m736_a_" + suffix, "usr_m736_b_" + suffix, "usr_m736_c_" + suffix);
        repository.createUser(new AccessUser(f.admin(), f.admin(), "platform_admin", "org", null, "active", Instant.now()));
        repository.assignUserRole(f.admin(), "role_platform_admin");
        // 建角色、分配角色两个入口码另给一个明确的角色（同 createRoleRejectsContractViolationsWith400ThroughTheEndpoint 的做法）
        String adminRole = "role_m736_admin_" + suffix;
        repository.createRole(new RoleRecord(adminRole, "M736 平台管理员治理 " + suffix, null, "project", "active", Instant.now(),
                List.of("access.role.manage", "access.role.assign"), null));
        repository.assignUserRole(f.admin(), adminRole);
        m736Project(f.projectA(), f.staffA());
        m736Project(f.projectB(), f.staffB());
        m736Project(f.projectC(), f.staffC());
        return f;
    }

    // 项目表里的一个项目，带一名归属该项目的员工。
    private void m736Project(String projectId, String staffId) {
        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                + "VALUES (?, 'complex', ?, 'active', now(), now())", projectId, "M736 " + projectId);
        repository.createUser(new AccessUser(staffId, staffId, "staff", projectId, null, "active", Instant.now()));
    }

    // 主体表里的一个主体（城市项目公司、域主管公司或顶层平台主体），带一名归属该主体的员工；返回员工编号。
    private String m736EntityStaff(M736Fixture f, String tag, String entityRole, String status) {
        String entity = "be_m736_" + tag + "_" + f.suffix();
        String staff = "usr_m736_" + tag + "_" + f.suffix();
        jdbcTemplate.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, now(), now())", entity, "M736 " + tag + " " + f.suffix(), entityRole, status);
        repository.createUser(new AccessUser(staff, staff, "staff", entity, null, "active", Instant.now()));
        return staff;
    }

    private void m736Superior(String userId, String superiorId, String updatedBy) {
        jdbcTemplate.update("INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by) VALUES (?, ?, 'main', ?)",
                userId, superiorId, updatedBy);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor m736Admin(M736Fixture f) {
        return m002Session(new AccessActor(f.admin(), f.admin(), "platform_admin", "org", null, "active"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor m736Staff(String userId, String projectId) {
        return m002Session(new AccessActor(userId, userId, "staff", projectId, null, "active"));
    }

    // 经建角色接口建角色并返回编号。viewAll＝勾「可以查看全部项目」；ticked＝展开后逐个勾选的可查看项目。
    private String m736CreateRole(org.springframework.test.web.servlet.request.RequestPostProcessor session, String key,
                                  List<String> codes, boolean viewAll, List<String> ticked) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(mockMvc.perform(post("/api/v1/access/roles").with(session)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "%s", "operator_note": "M736", "role_name": "M736 %s", "permission_codes": %s,
                                 "data_scope": "project", "view_all_projects": %s, "viewable_project_ids": %s}
                                """.formatted(key, key, jsonArray(codes), viewAll, jsonArray(ticked))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.id");
    }

    // 平台岗位映射：m736_<标记>_<后缀> 岗位 / lvl_m1_lower → 给定角色 + 空模板；返回岗位编码。
    private String m736Mapping(M736Fixture f, String tag, String roleId) {
        String position = "m736_" + tag + "_" + f.suffix();
        String template = "tpl_m736_" + tag + "_" + f.suffix();
        repository.createTemplate(baseTemplate(template), List.of());
        repository.savePositionRoleTemplate(new PositionRoleTemplate("prt_m736_" + tag + "_" + f.suffix(), position, "lvl_m1_lower",
                roleId, template, null, "active", f.admin(), Instant.now(), Instant.now()));
        return position;
    }

    private org.springframework.test.web.servlet.ResultActions m736Unassign(
            org.springframework.test.web.servlet.request.RequestPostProcessor session, String key, String userId, String roleId)
            throws Exception {
        return mockMvc.perform(post("/api/v1/access/role-assignments/unassign").with(session).contentType(MediaType.APPLICATION_JSON)
                .content(m002UnassignJson(key, userId, roleId)));
    }

    private org.springframework.test.web.servlet.ResultActions m736EffectivePermissionsOf(
            org.springframework.test.web.servlet.request.RequestPostProcessor session, String userId) throws Exception {
        return mockMvc.perform(get("/api/v1/access/users/" + userId + "/effective-permissions").with(session));
    }

    // 授权申请单的审批链节点（按顺序）：节点类型/审批人。
    private List<String> m736ChainNodes(String requestId) {
        return jdbcTemplate.queryForList("""
                SELECT n.approver_kind || '/' || coalesce(n.approver_id, '')
                  FROM supervisor_review_nodes n
                  JOIN access_grant_requests g ON g.review_id = n.review_id
                 WHERE g.id = ?
                 ORDER BY n.seq
                """, String.class, requestId);
    }

    private String m736Organization(String userId) {
        return jdbcTemplate.queryForObject("SELECT organization_id FROM access_users WHERE id = ?", String.class, userId);
    }

    private List<String> m736Constraints(String table, String type) {
        return jdbcTemplate.queryForList("""
                SELECT pg_get_constraintdef(oid) FROM pg_constraint
                 WHERE conrelid = CAST(? AS regclass) AND CAST(contype AS text) = ?
                 ORDER BY 1
                """, String.class, "public." + table, type);
    }

    // M736-J1（操作员 2026-09-25「如果有一个员工被分配了可跨项目的角色，那么在他分配的权限内，他是可以看到。全部的项目数据的。」「平台管理员
    // 可直接分配」；2026-10-01 所选选项原文「「全部项目」始终指所有项目，含以后新建的；只有展开后逐个勾选的，才只管勾中的那几个。」）：
    // 真库＋真实接口——平台管理员建含查看账号权限、勾了「可以查看全部项目」的角色并直接分配给甲项目员工；该员工列账号，看得到甲、乙、丙
    // 三个项目的员工，看不到平台管理员账号；之后新建一个项目，不改角色，再列就含新项目的员工。对照：只经普通角色持同一权限的甲项目员工
    // 只列得到本项目。
    @Test
    void viewAllRoleHolderListsUsersOfEveryProjectIncludingLaterOnesThroughTheEndpoint() throws Exception {
        M736Fixture f = m736Fixture();
        String role = m736CreateRole(m736Admin(f), "m736-j1-role-" + f.suffix(), List.of("access.user.view"), true, List.of());
        mockMvc.perform(post("/api/v1/access/role-assignments").with(m736Admin(f)).contentType(MediaType.APPLICATION_JSON)
                        .content(m002UnassignJson("m736-j1-assign-" + f.suffix(), f.staffA(), role)))
                .andExpect(status().isOk());
        String plainRole = "role_m736_plain_" + f.suffix();
        String plainStaff = "usr_m736_plain_" + f.suffix();
        repository.createRole(new RoleRecord(plainRole, "M736 本项目查看 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of("access.user.view"), null));
        repository.createUser(new AccessUser(plainStaff, plainStaff, "staff", f.projectA(), null, "active", Instant.now()));
        repository.assignUserRole(plainStaff, plainRole);
        var holder = m736Staff(f.staffA(), f.projectA());

        mockMvc.perform(get("/api/v1/access/users").with(holder))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", org.hamcrest.Matchers.hasItems(f.staffA(), f.staffB(), f.staffC())))
                .andExpect(jsonPath("$.data.items[*].id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(f.admin()))));
        mockMvc.perform(get("/api/v1/access/users").with(m736Staff(plainStaff, f.projectA())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", org.hamcrest.Matchers.hasItems(plainStaff, f.staffA())))
                .andExpect(jsonPath("$.data.items[*].id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(f.staffB()))));

        String laterProject = "prj_m736_n_" + f.suffix();
        String laterStaff = "usr_m736_n_" + f.suffix();
        m736Project(laterProject, laterStaff);
        mockMvc.perform(get("/api/v1/access/users").with(holder))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", org.hamcrest.Matchers.hasItems(f.staffB(), laterStaff)));
    }

    // M736-J2（操作员 2026-10-01 所选选项原文「项目管理员给本项目建的角色也能勾其他项目；分配这类角色仍按之前的答复先审批。」；2026-09-25
    // 所选选项原文「按授权政策「高」一档走（按现行政策不到最高权限）」；治理地图第 703 行「跨商户、跨项目权限每次均须先审批，审批通过后
    // 方可生效」）：真库＋真实接口——甲项目的角色管理人建一个只含日常权限、只勾了乙项目的角色（200：建角色时勾项目不审批，他自己看不到
    // 乙项目也能建）；他直接分配 → 403 GRANT_REQUIRES_APPROVAL、不落行；改提角色授权申请 → 受理，上两级档、两个上级节点、没有最高权限
    // 节点；第一级批完仍待审、持有人还不能跨，两级批完才落地；持有人能查看乙项目员工的有效权限、给乙项目员工提临时权限申请，对丙项目
    // 都是 403 CROSS_ORG_FORBIDDEN；撤销后再申请，仍要两级审批（第二次）。
    @Test
    void tickedRoleBuiltByAProjectManagerIsGrantedOnlyThroughATwoLevelApprovalThroughTheEndpoint() throws Exception {
        M736Fixture f = m736Fixture();
        String manager = "usr_m736_mgr_" + f.suffix();
        String boss = "usr_m736_boss_" + f.suffix();
        String topBoss = "usr_m736_top_" + f.suffix();
        for (String user : List.of(manager, boss, topBoss)) {
            repository.createUser(new AccessUser(user, user, "staff", f.projectA(), null, "active", Instant.now()));
        }
        String managerRole = "role_m736_mgr_" + f.suffix();
        repository.createRole(new RoleRecord(managerRole, "M736 角色管理人 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of("access.role.manage", "access.role.assign", "access.grant_request.apply", "access.user.view"), null));
        repository.assignUserRole(manager, managerRole);
        m736Superior(manager, boss, f.admin());
        m736Superior(boss, topBoss, f.admin());
        m736Superior(f.staffA(), boss, f.admin());
        var managerSession = m736Staff(manager, f.projectA());
        var holder = m736Staff(f.staffA(), f.projectA());
        String applyBody = "{\"idempotency_key\": \"%s\", \"request_type\": \"user_role\", \"target_user_id\": \"%s\", \"role_id\": \"%s\", "
                + "\"reason\": \"申请分配可跨项目角色\"}";
        String temporaryBody = "{\"idempotency_key\": \"%s\", \"request_type\": \"temporary_permission\", \"target_user_id\": \"%s\", "
                + "\"permission_codes\": [\"" + DAILY_A + "\"], \"expires_at\": \"2099-01-01T00:00:00Z\", \"reason\": \"临时顶班\"}";

        String role = m736CreateRole(managerSession, "m736-j2-role-" + f.suffix(),
                List.of("access.user.view", "access.grant_request.apply"), false, List.of(f.projectB()));
        mockMvc.perform(post("/api/v1/access/role-assignments").with(managerSession).contentType(MediaType.APPLICATION_JSON)
                        .content(m002UnassignJson("m736-j2-direct-" + f.suffix(), f.staffA(), role)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("GRANT_REQUIRES_APPROVAL"));
        assertThat(m002UserRoles(f.staffA())).doesNotContain(role);

        mockMvc.perform(post("/api/v1/access/grant-requests").with(managerSession).contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody.formatted("m736-j2-apply-" + f.suffix(), f.staffA(), role)))
                .andExpect(status().isOk());
        String first = m002PendingRequestId(f.staffA(), "user_role");
        assertThat(jdbcTemplate.queryForObject("SELECT policy_code FROM supervisor_review_decisions WHERE review_id = ?", String.class,
                m002ReviewId(first))).isEqualTo("permission_grant_l2");
        assertThat(m736ChainNodes(first)).containsExactly("user/" + boss, "user/" + topBoss);

        m002ActAs(boss, "staff", f.projectA(), () -> accessService.reviewGrant(first, m002Approve("m736-j2-first-" + f.suffix())));
        assertThat(m002Status(first)).isEqualTo("pending");
        assertThat(m002UserRoles(f.staffA())).doesNotContain(role);
        m736EffectivePermissionsOf(holder, f.staffB()).andExpect(status().isForbidden());
        m002ActAs(topBoss, "staff", f.projectA(), () -> accessService.reviewGrant(first, m002Approve("m736-j2-final-" + f.suffix())));
        assertThat(m002Status(first)).isEqualTo("applied");
        assertThat(m002UserRoles(f.staffA())).contains(role);

        m736EffectivePermissionsOf(holder, f.staffB()).andExpect(status().isOk());
        m736EffectivePermissionsOf(holder, f.staffC())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_ORG_FORBIDDEN"));
        mockMvc.perform(post("/api/v1/access/grant-requests").with(holder).contentType(MediaType.APPLICATION_JSON)
                        .content(temporaryBody.formatted("m736-j2-temp-b-" + f.suffix(), f.staffB())))
                .andExpect(status().isOk());
        assertThat(m002PendingRequestId(f.staffB(), "temporary_permission")).isNotNull();
        mockMvc.perform(post("/api/v1/access/grant-requests").with(holder).contentType(MediaType.APPLICATION_JSON)
                        .content(temporaryBody.formatted("m736-j2-temp-c-" + f.suffix(), f.staffC())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_ORG_FORBIDDEN"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM access_grant_requests WHERE target_user_id = ?", Long.class,
                f.staffC())).isZero();

        m736Unassign(managerSession, "m736-j2-unassign-" + f.suffix(), f.staffA(), role).andExpect(status().isOk());
        m736EffectivePermissionsOf(holder, f.staffB()).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/access/grant-requests").with(managerSession).contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody.formatted("m736-j2-apply-again-" + f.suffix(), f.staffA(), role)))
                .andExpect(status().isOk());
        String second = m002PendingRequestId(f.staffA(), "user_role");
        assertThat(second).isNotEqualTo(first);
        assertThat(m736ChainNodes(second)).containsExactly("user/" + boss, "user/" + topBoss);
        assertThat(m002UserRoles(f.staffA())).doesNotContain(role);
    }

    // M736-J3（操作员 2026-10-01 说明：可跨项目按角色在权限配置里的勾选认定——「只要有这个选项被勾选，那么对应的角色就可以看全部项目了」；
    // AGENTS.md 第 8 条不为存量数据回填）：真库＋真实接口——迁移后种子角色两项都未勾，没有哪个数据范围为全局的存量角色被勾上；数据范围为
    // 全局的平台角色分给甲项目员工，他仍只列得到本项目的账号；同样权限码、勾了「可以查看全部项目」的新角色分给他以后才跨。
    @Test
    void globalDataScopeDoesNotMakeARoleCrossProjectAndSeedRolesStayUntickedOnTheRealSchema() throws Exception {
        M736Fixture f = m736Fixture();
        assertThat(jdbcTemplate.queryForList(
                "SELECT view_all_projects FROM roles WHERE id IN ('role_platform_admin', 'role_staff_self') ORDER BY id", Boolean.class))
                .containsExactly(false, false);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM role_viewable_projects WHERE role_id IN ('role_platform_admin', 'role_staff_self')", Long.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM roles r
                 WHERE r.data_scope = 'global'
                   AND (r.view_all_projects OR EXISTS (SELECT 1 FROM role_viewable_projects v WHERE v.role_id = r.id))
                """, Long.class)).isZero();

        String global = "role_m736_global_" + f.suffix();
        repository.createRole(new RoleRecord(global, "M736 全局数据范围 " + f.suffix(), null, "global", "active", Instant.now(),
                List.of("access.user.view"), null));
        repository.assignUserRole(f.staffA(), global);
        var holder = m736Staff(f.staffA(), f.projectA());
        mockMvc.perform(get("/api/v1/access/users").with(holder))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", org.hamcrest.Matchers.hasItem(f.staffA())))
                .andExpect(jsonPath("$.data.items[*].id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(f.staffB()))));

        String ticked = m736CreateRole(m736Admin(f), "m736-j3-role-" + f.suffix(), List.of("access.user.view"), true, List.of());
        mockMvc.perform(post("/api/v1/access/role-assignments").with(m736Admin(f)).contentType(MediaType.APPLICATION_JSON)
                        .content(m002UnassignJson("m736-j3-assign-" + f.suffix(), f.staffA(), ticked)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/access/users").with(holder))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", org.hamcrest.Matchers.hasItems(f.staffA(), f.staffB(), f.staffC())));
    }

    // M736-J4（操作员 2026-09-25 所选选项原文「持有者只能触及归属在项目表或项目公司下的账号与数据；平台公司员工、平台管理员、最高权限、
    // 外部机构账号一律碰不到」；2026-10-01 所选选项原文「域主管公司也按「项目」对待，持有者可以跨项目处理它们的员工与数据（只算在用的）。」）：
    // 真库＋真实接口——经勾了「可以查看全部项目」的角色持分配角色权限的甲项目员工：乙项目、城市项目公司（在用的、停用的各一）、在用的域主管
    // 公司下的员工，角色撤得掉；停用的域主管公司、顶层平台主体下的员工 403 CROSS_ORG_FORBIDDEN；归属在乙项目的平台管理员账号、外部机构
    // 账号、最高权限账号同样 403，再来一次仍是 403（第二次），角色都还在。
    @Test
    void viewAllRoleHolderReachesProjectsAndProjectCompaniesButNotPlatformStaffOrExcludedAccountsThroughTheEndpoint() throws Exception {
        M736Fixture f = m736Fixture();
        String role = m736CreateRole(m736Admin(f), "m736-j4-role-" + f.suffix(), List.of("access.role.assign"), true, List.of());
        repository.assignUserRole(f.staffA(), role); // 夹具直接落行：角色含须审批码，走接口须经授权申请审批（见 J2），不是本用例要看的
        String kept = "role_m736_kept_" + f.suffix();
        repository.createRole(new RoleRecord(kept, "M736 待撤角色 " + f.suffix(), null, "project", "active", Instant.now(), List.of(), null));
        java.util.Map<String, String> reachable = new java.util.LinkedHashMap<>();
        reachable.put(f.staffB(), "乙项目员工");
        reachable.put(m736EntityStaff(f, "city", "project_company", "active"), "城市项目公司员工");
        reachable.put(m736EntityStaff(f, "cityoff", "project_company", "inactive"), "停用的城市项目公司员工");
        reachable.put(m736EntityStaff(f, "dom", "management_company", "active"), "在用的域主管公司员工");
        java.util.Map<String, String> unreachable = new java.util.LinkedHashMap<>();
        unreachable.put(m736EntityStaff(f, "domoff", "management_company", "inactive"), "停用的域主管公司员工");
        unreachable.put(m736EntityStaff(f, "top", "platform", "active"), "顶层平台主体员工");
        String adminInB = "usr_m736_pa_b_" + f.suffix();
        String vendorInB = "usr_m736_vendor_b_" + f.suffix();
        repository.createUser(new AccessUser(adminInB, adminInB, "platform_admin", f.projectB(), null, "active", Instant.now()));
        repository.createUser(new AccessUser(vendorInB, vendorInB, "outsourced_vendor", f.projectB(), null, "active", Instant.now()));
        String master = m002MasterAccount();
        jdbcTemplate.update("UPDATE access_users SET organization_id = ? WHERE id = ?", f.projectB(), master);
        unreachable.put(adminInB, "归属在乙项目的平台管理员账号");
        unreachable.put(vendorInB, "归属在乙项目的外部机构账号");
        unreachable.put(master, "归属在乙项目的最高权限账号");
        java.util.stream.Stream.concat(reachable.keySet().stream(), unreachable.keySet().stream())
                .forEach(user -> repository.assignUserRole(user, kept));
        var holder = m736Staff(f.staffA(), f.projectA());

        for (var target : reachable.entrySet()) {
            m736Unassign(holder, "m736-j4-" + f.suffix() + "-" + target.getKey(), target.getKey(), kept).andExpect(status().isOk());
            assertThat(m002UserRoles(target.getKey())).as(target.getValue()).doesNotContain(kept);
        }
        for (int round = 1; round <= 2; round++) {
            for (var target : unreachable.entrySet()) {
                m736Unassign(holder, "m736-j4-" + f.suffix() + "-" + round + "-" + target.getKey(), target.getKey(), kept)
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.detail_code").value("CROSS_ORG_FORBIDDEN"));
                assertThat(m002UserRoles(target.getKey())).as(target.getValue()).contains(kept);
            }
        }
    }

    // M736-F07（同上所选选项原文「……外部机构账号一律碰不到」；M001-F16 更正段「修改在用模板等同给在用员工增减权限」）：真库＋真实接口——
    // 乙项目的项目自有模板套给归属在乙项目的外部机构账号；经只勾了乙项目的角色持模板维护权限的甲项目员工修改该模板，403 CROSS_ORG_FORBIDDEN，
    // 模板条目不变。
    @Test
    void crossProjectHolderEditOfATemplateHeldByAnExternalAccountIsRejectedThroughTheEndpoint() throws Exception {
        M736Fixture f = m736Fixture();
        String role = m736CreateRole(m736Admin(f), "m736-f07-role-" + f.suffix(), List.of("access.permission_template.manage"), false,
                List.of(f.projectB()));
        repository.assignUserRole(f.staffA(), role); // 夹具直接落行：角色含须审批码（同 J4）
        String vendorInB = "usr_m736_vendor_b_" + f.suffix();
        repository.createUser(new AccessUser(vendorInB, vendorInB, "outsourced_vendor", f.projectB(), null, "active", Instant.now()));
        String templateId = "tpl_m736_f07_" + f.suffix();
        repository.createTemplate(new PermissionTemplate(templateId, templateId, templateId, "M001 test template", "base", null,
                "organization", f.projectB(), "merge", null, null, false, "active", "integration test", "seed-admin",
                Instant.now(), Instant.now(), Instant.now()), grants(templateId, DAILY_A));
        repository.applyTemplate(templateId, List.of(vendorInB), null, null, false, "seed-admin");

        mockMvc.perform(put("/api/v1/access/permission-templates/" + templateId).with(m736Staff(f.staffA(), f.projectA()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(templateJson("m736-f07-edit-" + f.suffix(), "M736 改名 " + f.suffix(), "base", "project")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_ORG_FORBIDDEN"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(vendorInB)));
        assertThat(templateCodes(templateId)).containsExactly(DAILY_A);
    }

    // M736-J5（操作员 2026-09-25 答「也算」；确认句「（含经岗位得到）」）：真库＋真实接口——岗位映射挂一个只含日常权限、只勾了乙项目的角色，
    // 平台管理员给甲项目员工定岗（不拦）；该员工没有直接分配的角色，经岗位派生的角色看得到乙项目员工的有效权限，丙项目 403 CROSS_ORG_FORBIDDEN。
    @Test
    void positionDerivedTickedRoleReachesTheTickedProjectThroughTheEndpoint() throws Exception {
        M736Fixture f = m736Fixture();
        String role = m736CreateRole(m736Admin(f), "m736-j5-role-" + f.suffix(), List.of("access.user.view"), false, List.of(f.projectB()));
        String position = m736Mapping(f, "j5", role);
        mockMvc.perform(put("/api/v1/access/users/" + f.staffA() + "/position").with(m736Admin(f)).contentType(MediaType.APPLICATION_JSON)
                        .content(xm002iPositionJson("m736-j5-seat-" + f.suffix(), position)))
                .andExpect(status().isOk());
        assertThat(m002AssignedRole(f.staffA())).isEqualTo(role);
        assertThat(m002UserRoles(f.staffA())).isEmpty();
        var holder = m736Staff(f.staffA(), f.projectA());

        m736EffectivePermissionsOf(holder, f.staffB()).andExpect(status().isOk());
        m736EffectivePermissionsOf(holder, f.staffC())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_ORG_FORBIDDEN"));
    }

    // M736-J5（同上；治理地图第 703 行「属于既有高危权限的，不得因处于岗位模板内而免除授出前审批」）：岗位派生的可跨项目角色带须审批码时
    // 先挂起——挂起期间该员工撤不了乙项目员工的角色（码未生效）；释放单获批后才跨到乙项目，丙项目仍 403 CROSS_ORG_FORBIDDEN。
    @Test
    void positionDerivedTickedRoleWithACodeNeedingApprovalReachesOnlyAfterTheReleaseIsApprovedThroughTheEndpoint() throws Exception {
        M736Fixture f = m736Fixture();
        String boss = "usr_m736_boss_" + f.suffix();
        repository.createUser(new AccessUser(boss, boss, "staff", "org", null, "active", Instant.now()));
        m736Superior(f.admin(), boss, f.admin());
        String role = m736CreateRole(m736Admin(f), "m736-j5h-role-" + f.suffix(), List.of("access.role.assign"), false, List.of(f.projectB()));
        String position = m736Mapping(f, "j5h", role);
        String kept = "role_m736_kept_" + f.suffix();
        repository.createRole(new RoleRecord(kept, "M736 待撤角色 " + f.suffix(), null, "project", "active", Instant.now(), List.of(), null));
        repository.assignUserRole(f.staffB(), kept);
        repository.assignUserRole(f.staffC(), kept);
        mockMvc.perform(put("/api/v1/access/users/" + f.staffA() + "/position").with(m736Admin(f)).contentType(MediaType.APPLICATION_JSON)
                        .content(xm002iPositionJson("m736-j5h-seat-" + f.suffix(), position)))
                .andExpect(status().isOk());
        String release = m002PendingRequestId(f.staffA(), "position_high_risk_release");
        var holder = m736Staff(f.staffA(), f.projectA());

        m736Unassign(holder, "m736-j5h-held-" + f.suffix(), f.staffB(), kept)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        assertThat(m002UserRoles(f.staffB())).contains(kept);

        m002ActAs(boss, "staff", () -> accessService.reviewGrant(release, m002Approve("m736-j5h-release-" + f.suffix())));
        assertThat(m002Status(release)).isEqualTo("applied");
        m736Unassign(holder, "m736-j5h-b-" + f.suffix(), f.staffB(), kept).andExpect(status().isOk());
        assertThat(m002UserRoles(f.staffB())).doesNotContain(kept);
        m736Unassign(holder, "m736-j5h-c-" + f.suffix(), f.staffC(), kept)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_ORG_FORBIDDEN"));
        assertThat(m002UserRoles(f.staffC())).contains(kept);
    }

    // M736-J6（操作员 2026-10-03 所选选项原文：「角色能看哪些项目，就能在哪些项目新建记录、把员工改挂过去。」）：真库＋真实接口——分配角色
    // 权限既来自只勾了乙项目的角色、又有本项目来源的甲项目员工，把甲项目员工改挂到乙项目：受理，账号归属改到乙项目，新范围岗位进审批
    // （待审）；改挂到丙项目 403 CROSS_ORG_FORBIDDEN、归属不变、不建单，再来一次仍拒（第二次）；只有本项目来源的甲项目员工改挂到乙项目同样 403。
    @Test
    void holderMovesAnEmployeeIntoATickedProjectButNotIntoAnUntickedOneThroughTheEndpoint() throws Exception {
        M736Fixture f = m736Fixture();
        String boss = "usr_m736_boss_" + f.suffix();
        String plain = "usr_m736_plain_" + f.suffix();
        String moverB = "usr_m736_mover_b_" + f.suffix();
        String moverC = "usr_m736_mover_c_" + f.suffix();
        for (String user : List.of(boss, plain, moverB, moverC)) {
            repository.createUser(new AccessUser(user, user, "staff", f.projectA(), null, "active", Instant.now()));
        }
        m736Superior(f.staffA(), boss, f.admin());
        String local = "role_m736_local_" + f.suffix();
        String empty = "role_m736_empty_" + f.suffix();
        repository.createRole(new RoleRecord(local, "M736 本项目分配角色 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of("access.role.assign"), null));
        repository.createRole(new RoleRecord(empty, "M736 空岗位角色 " + f.suffix(), null, "project", "active", Instant.now(), List.of(), null));
        String cross = m736CreateRole(m736Admin(f), "m736-j6-role-" + f.suffix(), List.of("access.role.assign"), false, List.of(f.projectB()));
        repository.assignUserRole(f.staffA(), local);
        repository.assignUserRole(f.staffA(), cross); // 夹具直接落行，同 J4
        repository.assignUserRole(plain, local);
        String position = m736Mapping(f, "j6", empty);
        var holder = m736Staff(f.staffA(), f.projectA());
        String scopeBody = "{\"idempotency_key\": \"%s\", \"organization_id\": \"%s\", \"position_code\": \"" + position + "\", "
                + "\"staff_level_id\": \"lvl_m1_lower\", \"reason\": \"调往其他项目\"}";

        mockMvc.perform(put("/api/v1/access/users/" + moverB + "/scope").with(holder).contentType(MediaType.APPLICATION_JSON)
                        .content(scopeBody.formatted("m736-j6-b-" + f.suffix(), f.projectB())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.request_type").value("user_scope_change"))
                .andExpect(jsonPath("$.data.status").value("pending"));
        assertThat(m736Organization(moverB)).isEqualTo(f.projectB());

        for (int round = 1; round <= 2; round++) {
            mockMvc.perform(put("/api/v1/access/users/" + moverC + "/scope").with(holder).contentType(MediaType.APPLICATION_JSON)
                            .content(scopeBody.formatted("m736-j6-c-" + round + "-" + f.suffix(), f.projectC())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value("CROSS_ORG_FORBIDDEN"));
        }
        mockMvc.perform(put("/api/v1/access/users/" + moverC + "/scope").with(m736Staff(plain, f.projectA()))
                        .contentType(MediaType.APPLICATION_JSON).content(scopeBody.formatted("m736-j6-plain-" + f.suffix(), f.projectB())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_ORG_FORBIDDEN"));
        assertThat(m736Organization(moverC)).isEqualTo(f.projectA());
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM access_grant_requests WHERE target_user_id = ?", Long.class, moverC))
                .isZero();
    }

    @Test
    void v441MigrationDoesNotBackfillExistingMerchantPrimaryProject() {
        String schema = "v441_test_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute("CREATE SCHEMA " + schema);
        jdbcTemplate.execute("SET LOCAL search_path TO " + schema);
        try {
            jdbcTemplate.execute("CREATE TABLE projects(id VARCHAR(64) PRIMARY KEY)");
            jdbcTemplate.execute("CREATE TABLE merchants(id VARCHAR(64) PRIMARY KEY, project_id VARCHAR(64))");
            jdbcTemplate.execute("CREATE TABLE roles(id VARCHAR(64) PRIMARY KEY)");
            jdbcTemplate.execute("CREATE TABLE access_users(id VARCHAR(64) PRIMARY KEY)");
            jdbcTemplate.execute("CREATE TABLE access_grant_requests(request_type VARCHAR(64))");
            jdbcTemplate.update("INSERT INTO projects VALUES ('原综合体')");
            jdbcTemplate.update("INSERT INTO merchants VALUES ('原商户','原综合体')");
            jdbcTemplate.update("INSERT INTO roles VALUES ('原角色')");
            jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
                org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,
                        new org.springframework.core.io.ClassPathResource(
                                "db/migration/V441__m736_role_viewable_projects_and_merchant_projects.sql"));
                return null;
            });
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM merchant_projects", Long.class)).isZero();
            assertThat(jdbcTemplate.queryForObject("SELECT project_id FROM merchants WHERE id='原商户'", String.class)).isEqualTo("原综合体");
            assertThat(jdbcTemplate.queryForObject("SELECT view_all_projects FROM roles WHERE id='原角色'", Boolean.class)).isFalse();
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM role_viewable_projects", Long.class)).isZero();
        } finally {
            jdbcTemplate.execute("SET LOCAL search_path TO public");
        }
    }

    // M736-J7（任务书 3.1；操作员 2026-10-01 所选选项原文「……并支持一个商户对应多个项目（新增建模）。」；AGENTS.md 第 8 条不回填）：真库——
    // V441 之后 merchant_projects 表在、没有行，主键（商户、项目）、项目索引与两个外键在，外键不带级联；roles.view_all_projects 非空、
    // 默认未勾选；role_viewable_projects 主键（角色、可查看目标），勾选行随角色删除。
    @Test
    void v441DefinesMerchantProjectsAndRoleTicksThatGoWithTheRoleOnTheRealSchema() {
        assertThat(regclass("merchant_projects")).isEqualTo("merchant_projects");
        assertThat(regclass("role_viewable_projects")).isEqualTo("role_viewable_projects");
        assertThat(regclass("idx_merchant_projects_project_id")).isEqualTo("idx_merchant_projects_project_id");
        // 不回填在隔离模式执行原迁移验证；运行中的业务表允许已有真实跨项目绑定。
        assertThat(m736Constraints("merchant_projects", "p")).containsExactly("PRIMARY KEY (merchant_id, project_id)");
        assertThat(m736Constraints("merchant_projects", "f")).containsExactly(
                "FOREIGN KEY (merchant_id) REFERENCES merchants(id)", "FOREIGN KEY (project_id) REFERENCES projects(id)");
        assertThat(m736Constraints("role_viewable_projects", "p")).containsExactly("PRIMARY KEY (role_id, viewable_id)");
        assertThat(m736Constraints("role_viewable_projects", "f")).containsExactly(
                "FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT column_default || '/' || is_nullable FROM information_schema.columns
                 WHERE table_schema = 'public' AND table_name = 'roles' AND column_name = 'view_all_projects'
                """, String.class)).isEqualTo("false/NO");

        String role = "role_m736_cascade_" + UUID.randomUUID().toString().substring(0, 8);
        repository.createRole(new RoleRecord(role, "M736 级联 " + role, null, "project", "active", Instant.now(), List.of(), null));
        jdbcTemplate.update("INSERT INTO role_viewable_projects (role_id, viewable_id) VALUES (?, 'm736_any_project')", role);
        jdbcTemplate.update("DELETE FROM roles WHERE id = ?", role);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM role_viewable_projects WHERE role_id = ?", Long.class, role))
                .isZero();
    }

    // ── M736 第二步（操作员 2026-10-08 确认的治理地图第 703 行追加句：「可跨项目角色分配出去以后，可以直接在原角色上增加或减少可查看的
    //    项目：增加的，所有持有人要各自重新审批通过，才能查看新增的项目；减少的，也要审批通过后才生效，审批期间持有人仍能查看。」）
    //    真库与真实接口回归 ──────────────────────────────────────────────────────────────────────────────────────────────
    //    本组只走接口、既有的服务方法与 SQL，不引用第二步新增的 Java 类型：对只有第一步的主源码也能编译，换上本文件即可红跑——
    //    增减接口不存在（404）、待批表不存在、或指纹不含勾选，各条红在注释写明的那一步。

    // 增减角色可查看项目的接口；fields＝请求体里除提交编号、操作说明以外的蛇形字段（JSON 片段）。
    private org.springframework.test.web.servlet.ResultActions m736ChangeTicks(
            org.springframework.test.web.servlet.request.RequestPostProcessor session, String roleId, String key, String fields)
            throws Exception {
        return mockMvc.perform(put("/api/v1/access/roles/" + roleId + "/viewable-projects").with(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\": \"" + key + "\", \"operator_note\": \"M736\", " + fields + "}"));
    }

    private void m736Assign(M736Fixture f, String key, String userId, String roleId) throws Exception {
        mockMvc.perform(post("/api/v1/access/role-assignments").with(m736Admin(f)).contentType(MediaType.APPLICATION_JSON)
                        .content(m002UnassignJson(key, userId, roleId)))
                .andExpect(status().isOk());
    }

    // 平台管理员的上两级（授权政策「高」一档＝上两级），都在甲项目、都经角色持主管复核审批权限（通用主管复核入口的门禁）。
    // 平台管理员本人也挂这个角色：他去批自己发起的单要走到「发起人不能复核」那道闸；角色另带申请授权权限（他发起重新审批的入口码）。
    // 返回 [第一级, 第二级] 的会话主体。
    private List<AccessActor> m736Approvers(M736Fixture f) {
        String role = "role_m736_rva_" + f.suffix();
        repository.createRole(new RoleRecord(role, "M736 主管复核 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of("governance.supervisor_review.approve", "access.grant_request.apply"), null));
        repository.assignUserRole(f.admin(), role);
        String boss = "usr_m736_boss_" + f.suffix();
        String topBoss = "usr_m736_top_" + f.suffix();
        for (String user : List.of(boss, topBoss)) {
            repository.createUser(new AccessUser(user, user, "staff", f.projectA(), null, "active", Instant.now()));
            repository.assignUserRole(user, role);
        }
        m736Superior(f.admin(), boss, f.admin());
        m736Superior(boss, topBoss, f.admin());
        return List.of(new AccessActor(boss, boss, "staff", f.projectA(), null, "active"),
                new AccessActor(topBoss, topBoss, "staff", f.projectA(), null, "active"));
    }

    // 角色当前逐个勾选的条目（按编号）。
    private List<String> m736Ticks(String roleId) {
        return jdbcTemplate.queryForList("SELECT viewable_id FROM role_viewable_projects WHERE role_id = ? ORDER BY viewable_id",
                String.class, roleId);
    }

    // 角色的待批条目：员工/条目。
    private List<String> m736Holds(String roleId) {
        return jdbcTemplate.queryForList(
                "SELECT user_id || '/' || viewable_id FROM role_viewable_project_holds WHERE role_id = ? ORDER BY user_id, viewable_id",
                String.class, roleId);
    }

    // 某条目上记着的减少复核单号；没有待审的减少为空。
    private String m736RemovalReview(String roleId, String viewableId) {
        return jdbcTemplate.queryForObject("SELECT removal_review_id FROM role_viewable_projects WHERE role_id = ? AND viewable_id = ?",
                String.class, roleId, viewableId);
    }

    // 主管复核单的审批链节点（按顺序）：节点类型/审批人。
    private List<String> m736ReviewNodes(String reviewId) {
        return jdbcTemplate.queryForList(
                "SELECT approver_kind || '/' || coalesce(approver_id, '') FROM supervisor_review_nodes WHERE review_id = ? ORDER BY seq",
                String.class, reviewId);
    }

    // M736-J8（同上追加句「增加的，所有持有人要各自重新审批通过，才能查看新增的项目」；操作员 2026-10-03 所选选项原文「直接在原角色上勾 C；
    // 但张三等所有持有人要各自重新审批通过，才能看 C。」）：真库＋真实接口——角色勾了乙，平台管理员直接分给甲项目的张三、经岗位给李四。
    // 增减接口加丙（200）：两人各一行待批；张三看丙项目员工的有效权限 403、乙项目的照常 200；改之后才分配的王五对丙 200，不多写待批。
    // 平台管理员替张三发起重新审批（角色只含日常权限也受理，「高」一档、两个上级节点），第一级批完待批还在，两级批完——张三对丙 200、
    // 李四仍 403，库里只剩李四那一行；张三直接分配的角色没有多行。修复前红在增减接口那一步（404）；判定不减待批时红在张三对丙的 403。
    @Test
    void addedProjectStaysHiddenFromExistingHoldersUntilEachIsReapprovedThroughTheEndpoint() throws Exception {
        M736Fixture f = m736Fixture();
        List<AccessActor> approvers = m736Approvers(f);
        String lisi = "usr_m736_li_" + f.suffix();
        String wangwu = "usr_m736_wang_" + f.suffix();
        for (String user : List.of(lisi, wangwu)) {
            repository.createUser(new AccessUser(user, user, "staff", f.projectA(), null, "active", Instant.now()));
        }
        String role = m736CreateRole(m736Admin(f), "m736-j8-role-" + f.suffix(), List.of("access.user.view"), false, List.of(f.projectB()));
        m736Assign(f, "m736-j8-assign-" + f.suffix(), f.staffA(), role);
        String position = m736Mapping(f, "j8", role);
        mockMvc.perform(put("/api/v1/access/users/" + lisi + "/position").with(m736Admin(f)).contentType(MediaType.APPLICATION_JSON)
                        .content(xm002iPositionJson("m736-j8-seat-" + f.suffix(), position)))
                .andExpect(status().isOk());
        var zhangsan = m736Staff(f.staffA(), f.projectA());
        var lisiSession = m736Staff(lisi, f.projectA());

        m736ChangeTicks(m736Admin(f), role, "m736-j8-add-" + f.suffix(), "\"add_viewable_project_ids\": [\"" + f.projectC() + "\"]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.view_all_projects").value(false))
                .andExpect(jsonPath("$.data.viewable_project_ids", org.hamcrest.Matchers.containsInAnyOrder(f.projectB(), f.projectC())))
                .andExpect(jsonPath("$.data.pending_removal_ids").isEmpty())
                .andExpect(jsonPath("$.data.pending_reapprovals[*].user_id", org.hamcrest.Matchers.containsInAnyOrder(f.staffA(), lisi)));
        assertThat(m736Ticks(role)).containsExactly(f.projectB(), f.projectC());
        assertThat(m736Holds(role)).containsExactlyInAnyOrder(f.staffA() + "/" + f.projectC(), lisi + "/" + f.projectC());
        m736EffectivePermissionsOf(zhangsan, f.staffC())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_ORG_FORBIDDEN"));
        m736EffectivePermissionsOf(zhangsan, f.staffB()).andExpect(status().isOk());

        m736Assign(f, "m736-j8-assign-later-" + f.suffix(), wangwu, role);
        m736EffectivePermissionsOf(m736Staff(wangwu, f.projectA()), f.staffC()).andExpect(status().isOk());
        assertThat(m736Holds(role)).hasSize(2);

        mockMvc.perform(post("/api/v1/access/grant-requests").with(m736Admin(f)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "%s", "request_type": "role_project_reapproval", "target_user_id": "%s", "role_id": "%s",
                                 "reason": "补审新增项目"}
                                """.formatted("m736-j8-apply-" + f.suffix(), f.staffA(), role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.request_type").value("role_project_reapproval"))
                .andExpect(jsonPath("$.data.status").value("pending"));
        String request = m002PendingRequestId(f.staffA(), "role_project_reapproval");
        assertThat(jdbcTemplate.queryForObject("SELECT policy_code FROM supervisor_review_decisions WHERE review_id = ?", String.class,
                m002ReviewId(request))).isEqualTo("permission_grant_l2");
        assertThat(m736ChainNodes(request)).containsExactly("user/" + approvers.get(0).id(), "user/" + approvers.get(1).id());

        m002ActAs(approvers.get(0), () -> accessService.reviewGrant(request, m002Approve("m736-j8-first-" + f.suffix())));
        assertThat(m002Status(request)).isEqualTo("pending");
        assertThat(m736Holds(role)).hasSize(2);
        m736EffectivePermissionsOf(zhangsan, f.staffC()).andExpect(status().isForbidden());
        m002ActAs(approvers.get(1), () -> accessService.reviewGrant(request, m002Approve("m736-j8-final-" + f.suffix())));
        assertThat(m002Status(request)).isEqualTo("applied");

        assertThat(m736Holds(role)).containsExactly(lisi + "/" + f.projectC());
        m736EffectivePermissionsOf(zhangsan, f.staffC()).andExpect(status().isOk());
        m736EffectivePermissionsOf(lisiSession, f.staffC())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_ORG_FORBIDDEN"));
        m736EffectivePermissionsOf(lisiSession, f.staffB()).andExpect(status().isOk());
        assertThat(m002UserRoles(f.staffA())).containsExactly(role);
    }

    // M736-J9（同一追加句「减少的，也要审批通过后才生效，审批期间持有人仍能查看」；操作员 2026-10-03 所选选项原文「减少也要审批通过后
    // 才生效；审批期间持有人仍能看 C。」）：真库＋真实接口——增减接口减丙（200）：读回仍有丙、丙列为减少待审，持有人对丙仍 200；
    // 建的是一张主管复核单（动作名、对象类型、对象编号＝角色、「高」一档、两个上级节点）。在通用主管复核入口：发起人自己去批 403；
    // 第一级通过后角色不变、持有人对丙仍 200；末级通过的同一请求里丙从角色上去掉、持有人对丙 403、待减标记清空。驳回的变体（减乙）：
    // 角色不变、标记清空、持有人照常看得到乙，同样的减少可以再提。修复前红在增减接口那一步（404）；监听没接上时红在末级通过后的勾选。
    @Test
    void removalTakesEffectOnlyAfterApprovalOnTheGenericReviewEndpoint() throws Exception {
        M736Fixture f = m736Fixture();
        List<AccessActor> approvers = m736Approvers(f);
        AccessActor admin = new AccessActor(f.admin(), f.admin(), "platform_admin", "org", null, "active");
        String role = m736CreateRole(m736Admin(f), "m736-j9-role-" + f.suffix(), List.of("access.user.view"), false,
                List.of(f.projectB(), f.projectC()));
        m736Assign(f, "m736-j9-assign-" + f.suffix(), f.staffA(), role);
        var holder = m736Staff(f.staffA(), f.projectA());
        String removeC = "\"remove_viewable_project_ids\": [\"" + f.projectC() + "\"]";
        String removeB = "\"remove_viewable_project_ids\": [\"" + f.projectB() + "\"]";

        m736ChangeTicks(m736Admin(f), role, "m736-j9-remove-" + f.suffix(), removeC)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewable_project_ids", org.hamcrest.Matchers.containsInAnyOrder(f.projectB(), f.projectC())))
                .andExpect(jsonPath("$.data.pending_removal_ids", org.hamcrest.Matchers.contains(f.projectC())));
        mockMvc.perform(get("/api/v1/access/roles/" + role + "/viewable-projects").with(m736Admin(f)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewable_project_ids", org.hamcrest.Matchers.containsInAnyOrder(f.projectB(), f.projectC())))
                .andExpect(jsonPath("$.data.pending_removal_ids", org.hamcrest.Matchers.contains(f.projectC())));
        String review = m736RemovalReview(role, f.projectC());
        assertThat(jdbcTemplate.queryForObject("""
                SELECT action_type || '|' || object_type || '|' || object_id || '|' || policy_code || '|' || review_status
                  FROM supervisor_review_decisions WHERE review_id = ?
                """, String.class, review))
                .isEqualTo("access.role_viewable_projects.reduce|role_viewable_projects|" + role + "|permission_grant_l2|pending_review");
        assertThat(m736ReviewNodes(review)).containsExactly("user/" + approvers.get(0).id(), "user/" + approvers.get(1).id());
        assertThat(m736Holds(role)).isEmpty();
        m736EffectivePermissionsOf(holder, f.staffC()).andExpect(status().isOk());

        m002ChainDecision(admin, review, "m736-j9-self-" + f.suffix(), "approved")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        m002ChainDecision(approvers.get(0), review, "m736-j9-first-" + f.suffix(), "approved")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("pending_review"));
        assertThat(m736Ticks(role)).containsExactly(f.projectB(), f.projectC());
        assertThat(m736RemovalReview(role, f.projectC())).isEqualTo(review);
        m736EffectivePermissionsOf(holder, f.staffC()).andExpect(status().isOk());

        m002ChainDecision(approvers.get(1), review, "m736-j9-last-" + f.suffix(), "approved")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("approved"));
        assertThat(m736Ticks(role)).containsExactly(f.projectB());
        assertThat(m736RemovalReview(role, f.projectB())).isNull();
        m736EffectivePermissionsOf(holder, f.staffC())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_ORG_FORBIDDEN"));
        m736EffectivePermissionsOf(holder, f.staffB()).andExpect(status().isOk());

        m736ChangeTicks(m736Admin(f), role, "m736-j9-remove-b-" + f.suffix(), removeB).andExpect(status().isOk());
        String rejected = m736RemovalReview(role, f.projectB());
        assertThat(rejected).isNotNull();
        m002ChainDecision(approvers.get(0), rejected, "m736-j9-reject-" + f.suffix(), "rejected")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("rejected"));
        assertThat(m736Ticks(role)).containsExactly(f.projectB());
        assertThat(m736RemovalReview(role, f.projectB())).isNull();
        m736EffectivePermissionsOf(holder, f.staffB()).andExpect(status().isOk());
        m736ChangeTicks(m736Admin(f), role, "m736-j9-remove-b-again-" + f.suffix(), removeB)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pending_removal_ids", org.hamcrest.Matchers.contains(f.projectB())));
    }

    // M736-J10（技术推导，依据同一追加句「增加或减少可查看的项目」：把「可以查看全部项目」勾上按增加办、去掉按减少办，逐个勾选的清单
    // 始终保留）：真库＋真实接口——清单只有乙的角色勾上全部：清单仍是乙，老持有人一行「全部」的待批、仍只到乙；之后才得到角色的人
    // 到全部（含用 SQL 新插的项目）。去掉全部：待审时角色仍勾着全部、新持有人仍到全部；两级批完——角色不再勾全部、清单仍是乙，
    // 新持有人只到乙，「全部」的待批行没了。修复前红在增减接口那一步（404）。
    @Test
    void viewAllAndBackThroughTheEndpoint() throws Exception {
        M736Fixture f = m736Fixture();
        List<AccessActor> approvers = m736Approvers(f);
        String later = "usr_m736_wang_" + f.suffix();
        repository.createUser(new AccessUser(later, later, "staff", f.projectA(), null, "active", Instant.now()));
        String role = m736CreateRole(m736Admin(f), "m736-j10-role-" + f.suffix(), List.of("access.user.view"), false, List.of(f.projectB()));
        m736Assign(f, "m736-j10-assign-" + f.suffix(), f.staffA(), role);
        var oldHolder = m736Staff(f.staffA(), f.projectA());
        var newHolder = m736Staff(later, f.projectA());

        m736ChangeTicks(m736Admin(f), role, "m736-j10-all-" + f.suffix(), "\"add_view_all_projects\": true")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.view_all_projects").value(true))
                .andExpect(jsonPath("$.data.viewable_project_ids", org.hamcrest.Matchers.contains(f.projectB())));
        assertThat(m736Ticks(role)).containsExactly(f.projectB());
        assertThat(m736Holds(role)).containsExactly(f.staffA() + "/*");
        m736Assign(f, "m736-j10-assign-later-" + f.suffix(), later, role);
        assertThat(m736Holds(role)).containsExactly(f.staffA() + "/*");
        String laterProject = "prj_m736_n_" + f.suffix();
        String laterStaff = "usr_m736_n_" + f.suffix();
        m736Project(laterProject, laterStaff);
        m736EffectivePermissionsOf(oldHolder, f.staffB()).andExpect(status().isOk());
        m736EffectivePermissionsOf(oldHolder, f.staffC())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_ORG_FORBIDDEN"));
        m736EffectivePermissionsOf(newHolder, f.staffC()).andExpect(status().isOk());
        m736EffectivePermissionsOf(newHolder, laterStaff).andExpect(status().isOk());

        m736ChangeTicks(m736Admin(f), role, "m736-j10-back-" + f.suffix(), "\"remove_view_all_projects\": true")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.view_all_projects").value(true))
                .andExpect(jsonPath("$.data.pending_removal_ids", org.hamcrest.Matchers.contains("*")));
        String review = jdbcTemplate.queryForObject("SELECT view_all_removal_review_id FROM roles WHERE id = ?", String.class, role);
        assertThat(review).isNotNull();
        m736EffectivePermissionsOf(newHolder, f.staffC()).andExpect(status().isOk());
        m002ChainDecision(approvers.get(0), review, "m736-j10-first-" + f.suffix(), "approved")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("pending_review"));
        m736EffectivePermissionsOf(newHolder, f.staffC()).andExpect(status().isOk());
        m002ChainDecision(approvers.get(1), review, "m736-j10-last-" + f.suffix(), "approved")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("approved"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT CAST(view_all_projects AS text) || '/' || coalesce(view_all_removal_review_id, '') FROM roles WHERE id = ?",
                String.class, role)).isEqualTo("false/");
        assertThat(m736Ticks(role)).containsExactly(f.projectB());
        assertThat(m736Holds(role)).isEmpty();
        m736EffectivePermissionsOf(newHolder, f.staffB()).andExpect(status().isOk());
        m736EffectivePermissionsOf(newHolder, f.staffC())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_ORG_FORBIDDEN"));
        m736EffectivePermissionsOf(oldHolder, f.staffB()).andExpect(status().isOk());
    }

    // M736-J11（技术推导：减少落地只认自己的动作名，并按复核单号匹配待减标记）：真库＋真实接口——角色的减少在待审。另一人（持主管复核
    // 发起权限）经通用评估入口建单：填本模块的动作名 400 UNKNOWN_REVIEW_ACTION（它不在可评估的清单里）；改用清单里的排班发布动作名，
    // 对象类型、对象编号照抄该角色，建出一张复核单并由他的上级批到通过——角色不变、待减标记还是原来那张单的号、真复核单仍待审、
    // 持有人照常看得到丙。修复前红在增减接口那一步（404）；监听不核动作名并按对象编号删时红在勾选那条。
    @Test
    void forgedReviewOnTheSameObjectDoesNotLandARemoval() throws Exception {
        M736Fixture f = m736Fixture();
        m736Approvers(f);
        String forger = "usr_m736_forger_" + f.suffix();
        String forgerBoss = "usr_m736_fboss_" + f.suffix();
        for (String user : List.of(forger, forgerBoss)) {
            repository.createUser(new AccessUser(user, user, "staff", f.projectA(), null, "active", Instant.now()));
        }
        String reviewRole = "role_m736_review_" + f.suffix();
        repository.createRole(new RoleRecord(reviewRole, "M736 主管复核发起与审批 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of("governance.supervisor_review.manage", "governance.supervisor_review.approve"), null));
        repository.assignUserRole(forger, reviewRole);
        repository.assignUserRole(forgerBoss, reviewRole);
        m736Superior(forger, forgerBoss, f.admin());
        AccessActor forgerActor = new AccessActor(forger, forger, "staff", f.projectA(), null, "active");
        AccessActor forgerBossActor = new AccessActor(forgerBoss, forgerBoss, "staff", f.projectA(), null, "active");
        String role = m736CreateRole(m736Admin(f), "m736-j11-role-" + f.suffix(), List.of("access.user.view"), false,
                List.of(f.projectB(), f.projectC()));
        m736Assign(f, "m736-j11-assign-" + f.suffix(), f.staffA(), role);
        m736ChangeTicks(m736Admin(f), role, "m736-j11-remove-" + f.suffix(), "\"remove_viewable_project_ids\": [\"" + f.projectC() + "\"]")
                .andExpect(status().isOk());
        String real = m736RemovalReview(role, f.projectC());
        assertThat(real).isNotNull();
        String evaluateBody = """
                {"idempotency_key": "%s", "operator_note": "M736", "evidence_attachment_ids": [], "action_type": "%s",
                 "object_type": "role_viewable_projects", "object_id": "%s", "amount_cent": 0}
                """;

        mockMvc.perform(post("/api/v1/platform/supervisor-review/evaluate").with(m002Session(forgerActor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluateBody.formatted("m736-j11-own-action-" + f.suffix(), "access.role_viewable_projects.reduce", role)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("UNKNOWN_REVIEW_ACTION"));
        String forged = com.jayway.jsonpath.JsonPath.read(mockMvc.perform(post("/api/v1/platform/supervisor-review/evaluate")
                        .with(m002Session(forgerActor)).contentType(MediaType.APPLICATION_JSON)
                        .content(evaluateBody.formatted("m736-j11-forged-" + f.suffix(), "staff.shift.publish", role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("pending_review"))
                .andExpect(jsonPath("$.data.object_type").value("role_viewable_projects"))
                .andExpect(jsonPath("$.data.object_id").value(role))
                .andReturn().getResponse().getContentAsString(), "$.data.review_id");
        m002ChainDecision(forgerBossActor, forged, "m736-j11-forged-approve-" + f.suffix(), "approved")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("approved"));

        assertThat(m736Ticks(role)).containsExactly(f.projectB(), f.projectC());
        assertThat(m736RemovalReview(role, f.projectC())).isEqualTo(real);
        assertThat(m002ReviewStatus(real)).isEqualTo("pending_review");
        m736EffectivePermissionsOf(m736Staff(f.staffA(), f.projectA()), f.staffC()).andExpect(status().isOk());
    }

    // M736-J12（技术推导：直接分配与岗位派生两条路都不再持有这个角色时，待重新审批的记录随之清除）：真库＋真实接口——张三直接持有，
    // 李四直接分配与岗位派生两条路都持有，加丙后两人各一行待批。经接口取消张三的分配——他这个角色的待批 0 行；取消李四的直接分配——
    // 岗位派生还在，待批还在。另一个角色用 SQL 删掉——它的勾选行与待批行随角色级联删除。修复前红在增减接口那一步（404）；
    // 仓储不清待批时红在取消张三之后那条。
    @Test
    void losingTheRoleDropsTheHoldsOnTheRealSchema() throws Exception {
        M736Fixture f = m736Fixture();
        String lisi = "usr_m736_li_" + f.suffix();
        repository.createUser(new AccessUser(lisi, lisi, "staff", f.projectA(), null, "active", Instant.now()));
        String role = m736CreateRole(m736Admin(f), "m736-j12-role-" + f.suffix(), List.of("access.user.view"), false, List.of(f.projectB()));
        m736Assign(f, "m736-j12-assign-" + f.suffix(), f.staffA(), role);
        m736Assign(f, "m736-j12-assign-li-" + f.suffix(), lisi, role);
        String position = m736Mapping(f, "j12", role);
        mockMvc.perform(put("/api/v1/access/users/" + lisi + "/position").with(m736Admin(f)).contentType(MediaType.APPLICATION_JSON)
                        .content(xm002iPositionJson("m736-j12-seat-" + f.suffix(), position)))
                .andExpect(status().isOk());
        m736ChangeTicks(m736Admin(f), role, "m736-j12-add-" + f.suffix(), "\"add_viewable_project_ids\": [\"" + f.projectC() + "\"]")
                .andExpect(status().isOk());
        assertThat(m736Holds(role)).containsExactlyInAnyOrder(f.staffA() + "/" + f.projectC(), lisi + "/" + f.projectC());

        m736Unassign(m736Admin(f), "m736-j12-unassign-" + f.suffix(), f.staffA(), role).andExpect(status().isOk());
        assertThat(m736Holds(role)).containsExactly(lisi + "/" + f.projectC());
        m736Unassign(m736Admin(f), "m736-j12-unassign-li-" + f.suffix(), lisi, role).andExpect(status().isOk());
        assertThat(m002UserRoles(lisi)).doesNotContain(role);
        assertThat(m002AssignedRole(lisi)).isEqualTo(role);
        assertThat(m736Holds(role)).as("两条路都持有、只取消直接分配").containsExactly(lisi + "/" + f.projectC());

        String cascade = "role_m736_gone_" + f.suffix();
        repository.createRole(new RoleRecord(cascade, "M736 级联 " + cascade, null, "project", "active", Instant.now(), List.of(), null));
        jdbcTemplate.update("INSERT INTO role_viewable_projects (role_id, viewable_id) VALUES (?, ?)", cascade, f.projectB());
        jdbcTemplate.update("INSERT INTO role_viewable_project_holds (user_id, role_id, viewable_id) VALUES (?, ?, ?)",
                f.staffB(), cascade, f.projectB());
        assertThat(m736Holds(cascade)).hasSize(1);
        jdbcTemplate.update("DELETE FROM roles WHERE id = ?", cascade);
        assertThat(m736Ticks(cascade)).isEmpty();
        assertThat(m736Holds(cascade)).isEmpty();
    }

    // M736-J13（任务书第 3 节；AGENTS.md 第 8 条不回填）：真库——V441 之后待批表在，主键（员工、角色、条目），员工、角色两个外键都随删
    // 级联；roles 与 role_viewable_projects 上两个待减标记列在、可空、没有默认值；授权申请类型的检查约束收「重新审批可查看项目」、
    // 仍拒未知类型。修复前（库按只有第一步的 V441 迁移）红在第一条：没有待批表。
    @Test
    void migrationAddsHoldsTableRemovalMarkersAndTheReapprovalType() {
        assertThat(regclass("role_viewable_project_holds")).isEqualTo("role_viewable_project_holds");
        assertThat(m736Constraints("role_viewable_project_holds", "p")).containsExactly("PRIMARY KEY (user_id, role_id, viewable_id)");
        assertThat(m736Constraints("role_viewable_project_holds", "f")).containsExactly(
                "FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE",
                "FOREIGN KEY (user_id) REFERENCES access_users(id) ON DELETE CASCADE");
        assertThat(jdbcTemplate.queryForList("""
                SELECT table_name || '.' || column_name || '/' || coalesce(column_default, '') || '/' || is_nullable
                  FROM information_schema.columns
                 WHERE table_schema = 'public'
                   AND ((table_name = 'roles' AND column_name = 'view_all_removal_review_id')
                     OR (table_name = 'role_viewable_projects' AND column_name = 'removal_review_id'))
                """, String.class)).containsExactlyInAnyOrder(
                "roles.view_all_removal_review_id//YES", "role_viewable_projects.removal_review_id//YES");

        jdbcTemplate.update("""
                INSERT INTO access_grant_requests (id, request_type, target_user_id, status, reason, requested_by, created_at, updated_at)
                VALUES ('agr_m736_reapproval', 'role_project_reapproval', 'usr_m736_target', 'pending', 'M736 schema check', 'usr_m736_admin', now(), now())
                """);
        assertThat(jdbcTemplate.queryForObject("SELECT request_type FROM access_grant_requests WHERE id = 'agr_m736_reapproval'", String.class))
                .isEqualTo("role_project_reapproval");
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO access_grant_requests (id, request_type, target_user_id, status, reason, requested_by, created_at, updated_at)
                VALUES ('agr_m736_unknown', 'm736_unknown_type', 'usr_m736_target', 'pending', 'x', 'usr_m736_admin', now(), now())
                """))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                .hasMessageContaining("chk_access_grant_requests_type");
    }

    // M736-J14（技术推导：角色、岗位、范围变更三类申请所指角色勾了项目的，授权事实指纹含两项勾选——否则按旧勾选批的单会按新勾选落地）：
    // 真库——甲项目的角色管理人对勾了乙的角色提角色申请（待审）；随后用 SQL 给该角色多插一行丙。上两级去点通过——都是 409
    // GRANT_SNAPSHOT_CHANGED，申请仍待审，目标员工没有得到角色。本条在只有第一步的主源码上按断言变红：那时指纹不含勾选，
    // 第一级点通过不抛错（两级批完照旧落地）。
    @Test
    void pendingRoleRequestIsRefusedAfterATickIsAddedOnTheRealSchema() throws Exception {
        M736Fixture f = m736Fixture();
        String manager = "usr_m736_mgr_" + f.suffix();
        String boss = "usr_m736_boss_" + f.suffix();
        String topBoss = "usr_m736_top_" + f.suffix();
        for (String user : List.of(manager, boss, topBoss)) {
            repository.createUser(new AccessUser(user, user, "staff", f.projectA(), null, "active", Instant.now()));
        }
        String managerRole = "role_m736_mgr_" + f.suffix();
        repository.createRole(new RoleRecord(managerRole, "M736 角色管理人 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of("access.role.manage", "access.role.assign", "access.grant_request.apply", "access.user.view"), null));
        repository.assignUserRole(manager, managerRole);
        m736Superior(manager, boss, f.admin());
        m736Superior(boss, topBoss, f.admin());
        var managerSession = m736Staff(manager, f.projectA());
        String role = m736CreateRole(managerSession, "m736-j14-role-" + f.suffix(), List.of("access.user.view"), false, List.of(f.projectB()));
        mockMvc.perform(post("/api/v1/access/grant-requests").with(managerSession).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "%s", "request_type": "user_role", "target_user_id": "%s", "role_id": "%s",
                                 "reason": "申请分配可跨项目角色"}
                                """.formatted("m736-j14-apply-" + f.suffix(), f.staffA(), role)))
                .andExpect(status().isOk());
        String request = m002PendingRequestId(f.staffA(), "user_role");
        assertThat(m736ChainNodes(request)).containsExactly("user/" + boss, "user/" + topBoss);

        jdbcTemplate.update("INSERT INTO role_viewable_projects (role_id, viewable_id) VALUES (?, ?)", role, f.projectC());

        for (String approver : List.of(boss, topBoss)) {
            assertThatThrownBy(() -> m002ActAs(approver, "staff", f.projectA(),
                    () -> accessService.reviewGrant(request, m002Approve("m736-j14-review-" + approver))))
                    .as(approver).isInstanceOfSatisfying(ApiException.class, e -> {
                        assertThat(e.status().value()).isEqualTo(409);
                        assertThat(e.code()).isEqualTo("GRANT_SNAPSHOT_CHANGED");
                    });
        }
        assertThat(m002Status(request)).isEqualTo("pending");
        assertThat(m002UserRoles(f.staffA())).doesNotContain(role);
    }
}
