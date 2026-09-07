package com.complex.platform.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class AccessJdbcIntegrationTests {

    @Autowired
    private JdbcAccessRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthorizationService authorizationService;

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

    // M006-G-07（PGM-1.10.1「旧派生同步失效」）：已定岗改职级复用定岗的四步写入（职级、定岗、套模板、替换派生），
    // 入口必须与 assignUserPosition 一样由 Spring 事务整体包住，否则套模板已提交、替换派生失败时新旧派生并存。
    @Test
    void staffLevelChangeRunsInOneTransactionLikePositionAssignment() throws NoSuchMethodException {
        assertThat(transactionAttributeSource.getTransactionAttribute(AccessService.class.getMethod(
                "assignUserPosition", com.complex.platform.access.dto.UserPositionAssignmentRequest.class), AccessService.class))
                .isNotNull();
        assertThat(transactionAttributeSource.getTransactionAttribute(AccessService.class.getMethod(
                "assignStaffLevel", com.complex.platform.access.dto.StaffLevelAssignmentRequest.class), AccessService.class))
                .isNotNull();
    }

    // M006-G-08（PGM-1.18「界面全中文」）：治理页 / 模板页直接上屏的权限名、角色名、模板名不含拉丁字母
    // （HTML 正文自身使用的「AI」除外；permission_code / id 是机读标识，不在此列）。
    @Test
    void seededDisplayNamesAreChinese() {
        assertThat(jdbcTemplate.queryForList("""
                SELECT permission_code FROM permissions
                 WHERE replace(permission_name, 'AI', '') ~ '[A-Za-z]'
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
    @Test
    void scopeChangeAndHighRiskReleaseRequestTypesAreAcceptedByTheGrantRequestCheck() {
        for (String type : List.of("user_scope_change", "position_high_risk_release")) {
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
}
