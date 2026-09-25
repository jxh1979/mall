package com.complex.platform.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.dto.AccessGrantApplyRequest;
import com.complex.platform.access.dto.AccessGrantReviewRequest;
import com.complex.platform.access.dto.PermissionTemplateApplyRequest;
import com.complex.platform.access.dto.PermissionTemplateRequest;
import com.complex.platform.access.dto.UserPermissionOverrideRequest;
import com.complex.platform.access.dto.UserPositionAssignmentRequest;
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
    @Test
    void moneyAndSensitiveExportPermissionsRequireApproval() {
        assertThat(jdbcTemplate.queryForList("""
                SELECT permission_code FROM permissions
                 WHERE requires_approval = FALSE
                   AND permission_code IN ('finance.dividend.manage', 'finance.ar_ap.export',
                                           'analytics.report.export', 'bar.internal_settlement_price.view')
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

    // M002-F08（操作员 2026-09-25 裁决「本人免审批，他人须审批」）：真库回归——最高权限账号同码有高危授予来源、岗位挂起与员工显式拒绝：
    // 本人会话直接解除成功且只删显式行；他人走解除申请、获批后只删显式行；持码的平台管理员直接解除须审批；普通目标同组合仍须审批。
    @Test
    void masterAuthorityTargetDenyRemovalIsDirectOnlyForTheMasterSessionOnTheRealSchema() {
        M002Fixture f = m002ApprovalFixture();
        String master = SessionIssuer.MASTER_USER_ID;
        jdbcTemplate.update("""
                INSERT INTO access_users (id, username, account_type, organization_id, status, created_at)
                VALUES (?, ?, ?, 'platform-company', 'active', now())
                ON CONFLICT (id) DO UPDATE SET username = EXCLUDED.username, account_type = EXCLUDED.account_type, status = 'active'
                """, master, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE);
        String role = "role_m002_master_" + f.suffix();
        repository.createRole(new RoleRecord(role, "M002 高危角色 " + f.suffix(), null, "project", "active", Instant.now(),
                List.of(f.code()), null));
        for (String user : List.of(master, f.target(), f.admin())) {
            repository.assignUserRole(user, role);
        }
        repository.saveHighRiskHolds(master, List.of(f.code()), "岗位高危挂起", f.admin());
        for (String user : List.of(master, f.target())) {
            repository.saveUserPermissionOverrides(user, List.of(f.code()), AccessRepository.Effect.DENY, "员工显式拒绝", f.admin());
        }
        assertThat(denyOrigins(master, f.code())).containsExactly("explicit", "position_hold");

        m002ActAsMaster(() -> accessService.removeUserPermissionOverrides(master, m002RemoveDeny("m002-j7-self-" + f.suffix(), f.code())));
        assertThat(denyOrigins(master, f.code())).containsExactly("position_hold");

        repository.saveUserPermissionOverrides(master, List.of(f.code()), AccessRepository.Effect.DENY, "再次停权", f.admin());
        m002ActAs(f.admin(), "platform_admin", () -> accessService.applyGrant(new AccessGrantApplyRequest(
                "m002-j7-apply-" + f.suffix(), "M002", List.of(), "user_permission_deny_removal", master, null, null, null, null, null, null,
                List.of(f.code()), null, null, null, "解除最高权限账号显式拒绝")));
        String requestId = m002PendingRequestId(master, "user_permission_deny_removal");
        m002ActAs(f.boss(), "staff", () -> accessService.reviewGrant(requestId, m002Approve("m002-j7-review-" + f.suffix())));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM access_grant_requests WHERE id = ?", String.class, requestId))
                .isEqualTo("applied");
        assertThat(denyOrigins(master, f.code())).containsExactly("position_hold");

        repository.saveUserPermissionOverrides(master, List.of(f.code()), AccessRepository.Effect.DENY, "三次停权", f.admin());
        assertThatThrownBy(() -> m002ActAs(f.admin(), "platform_admin", () -> accessService.removeUserPermissionOverrides(master,
                m002RemoveDeny("m002-j7-admin-" + f.suffix(), f.code()))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_REQUIRES_APPROVAL"));
        assertThat(denyOrigins(master, f.code())).containsExactly("explicit", "position_hold");
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

    // 后台 Web 终端（grace 模式下自报 web 的写请求留痕放行）+ 生产过滤器放入的 AccessActor 主体。
    private org.springframework.test.web.servlet.request.RequestPostProcessor m002Session(AccessActor actor) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null, List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF"))))
                    .postProcessRequest(request);
        };
    }

    // M002-F11/F13（操作员 2026-09-25 裁决，治理地图「权限规则」）：真实接口回归——持 access.role.assign、不持角色高危码的同组织员工
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
        repository.createUser(new AccessUser(f.boss(), f.boss(), "staff", "org", null, "active", Instant.now()));
        repository.createUser(new AccessUser(f.target(), f.target(), "staff", "org", null, "active", Instant.now()));
        jdbcTemplate.update("INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by) VALUES (?, ?, 'main', ?)",
                f.admin(), f.boss(), f.admin());
        return f;
    }

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
        repository.createTemplate(template("tpl_f11_active", "f11-active", null),
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

    // 平台管理员日常桥可授的三个日常码（见 dailyCodesReachThePlatformAdminBridgeAndRiskTiersAreTheFourGovernedOnes），按字母序
    private static final String DAILY_A = "amusement.device.view";
    private static final String DAILY_B = "inventory.stocktake.view";
    private static final String DAILY_C = "inventory.trace_code.view";

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
}
