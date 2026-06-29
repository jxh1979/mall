package com.complex.platform.access;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class AccessJdbcIntegrationTests {

    @Autowired
    private JdbcAccessRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void v178SeedsOutsourcedVendorFacilityRolesWithoutManagePermissions() {
        List<String> permissionCodes = repository.listPermissions().stream()
                .map(PermissionDefinition::permissionCode)
                .toList();

        assertThat(permissionCodes).contains(
                "facility.work_order.view_assigned",
                "facility.work_order.process_assigned",
                "facility.work_order.accept_assigned",
                "facility.inspection.view_assigned",
                "facility.inspection.record_assigned"
        );
        assertThat(jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM role_permissions
                 WHERE role_id = 'role_outsourced_facility'
                 ORDER BY permission_code
                """, String.class)).containsExactly(
                "facility.work_order.accept_assigned",
                "facility.work_order.process_assigned",
                "facility.work_order.view_assigned"
        );
        assertThat(jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM role_permissions
                 WHERE role_id = 'role_outsourced_security_cleaning'
                 ORDER BY permission_code
                """, String.class)).containsExactly(
                "facility.inspection.record_assigned",
                "facility.inspection.view_assigned"
        );
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM role_permissions
                 WHERE role_id IN ('role_outsourced_facility', 'role_outsourced_security_cleaning')
                   AND permission_code LIKE '%.manage'
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForList("""
                SELECT position_code
                  FROM position_role_templates
                 WHERE role_id IN ('role_outsourced_facility', 'role_outsourced_security_cleaning')
                 ORDER BY position_code
                """, String.class)).containsExactly("outsourced_facility", "outsourced_security_cleaning");
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
        PermissionTemplate template = repository.saveTemplate(
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
        repository.saveTemplate(
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
        repository.saveTemplate(
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

    private String regclass(String tableName) {
        return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
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
