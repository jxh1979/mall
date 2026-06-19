package com.complex.platform.amusement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class AmusementJdbcIntegrationTests {

    @Autowired
    private JdbcAmusementRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private com.complex.platform.gate.AdmissionFactRepository admissionFactRepository;

    @Test
    void migrationCreatesAmusementTablesAndSeedsPermissions() {
        assertThat(regclass("amusement_safety_templates")).isEqualTo("amusement_safety_templates");
        assertThat(regclass("amusement_projects")).isEqualTo("amusement_projects");
        assertThat(regclass("amusement_project_rules")).isEqualTo("amusement_project_rules");
        assertThat(regclass("amusement_sessions")).isEqualTo("amusement_sessions");
        assertThat(regclass("amusement_safety_inspections")).isEqualTo("amusement_safety_inspections");
        assertThat(regclass("amusement_safety_actions")).isEqualTo("amusement_safety_actions");
        assertThat(regclass("amusement_verifications")).isEqualTo("amusement_verifications");
        assertThat(regclass("amusement_manual_overrides")).isEqualTo("amusement_manual_overrides");
        assertThat(regclass("amusement_device_statuses")).isEqualTo("amusement_device_statuses");
        assertThat(regclass("amusement_queue_entries")).isEqualTo("amusement_queue_entries");
        assertThat(regclass("amusement_reservations")).isEqualTo("amusement_reservations");
        assertThat(regclass("amusement_group_bookings")).isEqualTo("amusement_group_bookings");
        assertThat(currentFlywayVersion()).isEqualTo(com.complex.platform.support.MigrationVersions.latest());

        assertThat(permissionCodes()).contains(
                "amusement.device_status.view",
                "amusement.verification.override",
                "amusement.session.view",
                "amusement.session.manage",
                "amusement.safety_inspection.view",
                "amusement.project.manage",
                "amusement.queue.view",
                "amusement.queue.manage",
                "amusement.reservation.view",
                "amusement.reservation.manage",
                "amusement.group_booking.view",
                "amusement.group_booking.manage",
                "amusement.group_booking.approve",
                "amusement.safety_inspection.create",
                "amusement.safety_inspection.approve_reopen",
                "amusement.safety_inspection.recheck",
                "amusement.safety_inspection.rectify",
                "amusement.safety_inspection.submit",
                "amusement.safety_template.view",
                "amusement.safety_template.manage",
                "amusement.verification.view",
                "amusement.verification.execute"
        );
        assertThat(regclass("amusement_capacity_config")).isEqualTo("amusement_capacity_config");
        assertThat(regclass("amusement_project_entitlements")).isEqualTo("amusement_project_entitlements");
        assertThat(platformAdminPermissionCodes()).contains(
                "amusement.verification.override",
                "amusement.session.manage",
                "amusement.capacity_config.manage",
                "amusement.project_entitlement.manage",
                "amusement.project.manage",
                "amusement.queue.manage",
                "amusement.reservation.manage",
                "amusement.group_booking.manage",
                "amusement.group_booking.approve",
                "amusement.safety_inspection.approve_reopen",
                "amusement.safety_template.manage",
                "amusement.verification.execute"
        );
    }

    @Test
    void saveAndListFlowsPersistSafetySessionVerificationAndReadonlyRecords() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveSafetyTemplate(new AmusementSafetyTemplateRecord(
                "template_jdbc",
                "Daily Opening",
                List.of(Map.of("name", "restraint", "required", true)),
                LocalDate.parse("2026-06-14"),
                "active",
                List.of("att_template"),
                now,
                now,
                null
        ));
        repository.saveProject(new AmusementProjectRecord(
                "project_jdbc",
                "Roller Coaster",
                2,
                "open",
                "template_jdbc",
                Map.of("min_age", 12),
                "point_jdbc",
                now,
                now,
                null
        ));
        repository.saveProjectRule(new AmusementProjectRuleRecord(
                "rule_jdbc",
                "project_jdbc",
                "safety_notice",
                "briefing required",
                "2026-06-14",
                null,
                true,
                "active",
                List.of("att_rule"),
                now,
                now
        ));
        repository.saveSession(new AmusementSessionRecord(
                "session_jdbc",
                "project_jdbc",
                now.plusSeconds(3600),
                now.plusSeconds(7200),
                2,
                0,
                0,
                "open",
                List.of("att_session"),
                now,
                now
        ));
        repository.saveSafetyInspection(new AmusementSafetyInspectionRecord(
                "inspection_jdbc",
                "project_jdbc",
                "template_jdbc",
                List.of(Map.of("name", "restraint", "result", "pass")),
                "pass",
                "submitted",
                "none",
                "not_required",
                now,
                now,
                null,
                null,
                "passed",
                List.of("att_inspection"),
                now,
                now,
                "inspector_jdbc",
                "pass",
                false,
                null
        ));
        repository.saveSafetyAction(new AmusementSafetyActionRecord(
                "action_jdbc",
                "inspection_jdbc",
                "submit",
                "pass",
                "passed",
                List.of("att_inspection"),
                now
        ));
        repository.saveVerification(new AmusementVerificationRecord(
                "verification_jdbc",
                "project_jdbc",
                "session_jdbc",
                "credential_jdbc",
                "device_1",
                "staff_1",
                "success",
                null,
                List.of("att_verification"),
                now
        ));
        repository.saveManualOverride(new AmusementManualOverrideRecord(
                "override_jdbc",
                "project_jdbc",
                "session_jdbc",
                "allow",
                "supervisor approved",
                "approved",
                "manual note",
                List.of("att_override"),
                now
        ));
        repository.saveDeviceStatus(new AmusementDeviceStatusRecord(
                "device_status_jdbc",
                "project_jdbc",
                "device_1",
                "mock_provider",
                "online",
                now,
                Map.of("raw", "ok"),
                now,
                now
        ));
        repository.saveQueueEntry(new AmusementQueueEntryRecord(
                "queue_jdbc",
                "project_jdbc",
                "session_jdbc",
                "customer_1",
                "A001",
                "waiting",
                now,
                now
        ));
        repository.saveReservation(new AmusementReservationRecord(
                "reservation_jdbc",
                "project_jdbc",
                "session_jdbc",
                "customer_1",
                "reserved",
                now,
                now
        ));
        repository.saveGroupBooking(new AmusementGroupBookingRecord(
                "group_booking_jdbc",
                "project_jdbc",
                "session_jdbc",
                "private",
                "Private Event",
                "13900000000",
                8,
                2,
                "pending_approval",
                null,
                "operator note",
                List.of("att_group"),
                null,
                null,
                now,
                now
        ));

        assertThat(repository.listSafetyTemplates())
                .extracting(AmusementSafetyTemplateRecord::id, AmusementSafetyTemplateRecord::templateName)
                .contains(tuple("template_jdbc", "Daily Opening"));
        assertThat(repository.listProjects())
                .extracting(AmusementProjectRecord::id, AmusementProjectRecord::capacity, AmusementProjectRecord::openStatus)
                .contains(tuple("project_jdbc", 2, "open"));
        assertThat(repository.listActiveBlockingProjectRules("project_jdbc"))
                .extracting(AmusementProjectRuleRecord::id, AmusementProjectRuleRecord::ruleType)
                .contains(tuple("rule_jdbc", "safety_notice"));
        assertThat(repository.lockSessionCapacityIfAvailable("session_jdbc", 1)).isTrue();
        assertThat(repository.incrementSessionUsedCapacityIfAvailable("session_jdbc")).isTrue();
        assertThat(repository.incrementSessionUsedCapacityIfAvailable("session_jdbc")).isFalse();
        assertThat(repository.findSessionById("session_jdbc"))
                .get()
                .extracting(AmusementSessionRecord::usedCapacity, AmusementSessionRecord::lockedCapacity)
                .containsExactly(1, 1);
        repository.releaseSessionLockedCapacity("session_jdbc", 1);
        assertThat(repository.findSessionById("session_jdbc"))
                .get()
                .extracting(AmusementSessionRecord::lockedCapacity)
                .isEqualTo(0);
        assertThat(repository.latestSafetyInspectionForProject("project_jdbc"))
                .get()
                .extracting(AmusementSafetyInspectionRecord::id, AmusementSafetyInspectionRecord::reopenStatus)
                .containsExactly("inspection_jdbc", "not_required");
        assertThat(repository.listVerifications())
                .extracting(AmusementVerificationRecord::id, AmusementVerificationRecord::verificationResult)
                .contains(tuple("verification_jdbc", "success"));
        assertThat(repository.listManualOverrides())
                .extracting(AmusementManualOverrideRecord::id, AmusementManualOverrideRecord::status)
                .contains(tuple("override_jdbc", "approved"));
        assertThat(repository.listDeviceStatuses())
                .extracting(AmusementDeviceStatusRecord::deviceId, AmusementDeviceStatusRecord::status)
                .contains(tuple("device_1", "online"));
        assertThat(repository.listQueueEntries())
                .extracting(AmusementQueueEntryRecord::queueNo, AmusementQueueEntryRecord::status)
                .contains(tuple("A001", "waiting"));
        assertThat(repository.findQueueEntryById("queue_jdbc"))
                .get()
                .extracting(AmusementQueueEntryRecord::queueNo, AmusementQueueEntryRecord::status)
                .containsExactly("A001", "waiting");
        assertThat(repository.listReservations())
                .extracting(AmusementReservationRecord::id, AmusementReservationRecord::reservationStatus)
                .contains(tuple("reservation_jdbc", "reserved"));
        assertThat(repository.findReservationById("reservation_jdbc"))
                .get()
                .extracting(AmusementReservationRecord::id, AmusementReservationRecord::reservationStatus)
                .containsExactly("reservation_jdbc", "reserved");
        assertThat(repository.findGroupBookingById("group_booking_jdbc"))
                .get()
                .extracting(AmusementGroupBookingRecord::bookingType, AmusementGroupBookingRecord::bookingStatus)
                .containsExactly("private", "pending_approval");

        // 清单#6：项目↔权益绑定 CRUD（amusement_project_entitlements）。
        repository.saveProjectEntitlement(new AmusementProjectEntitlementRecord(
                "ament_jdbc", "project_jdbc", "amusement_pass", now));
        repository.saveProjectEntitlement(new AmusementProjectEntitlementRecord(
                "ament_jdbc_dup", "project_jdbc", "amusement_pass", now)); // 幂等：同 type 不重复
        assertThat(repository.listProjectEntitlementTypes("project_jdbc")).containsExactly("amusement_pass");
        assertThat(repository.listProjectEntitlements("project_jdbc"))
                .extracting(AmusementProjectEntitlementRecord::entitlementType).containsExactly("amusement_pass");
        assertThat(repository.deleteProjectEntitlement("project_jdbc", "amusement_pass")).isTrue();
        assertThat(repository.listProjectEntitlementTypes("project_jdbc")).isEmpty();

        // 清单#6：点位/设备并发容量配置 CRUD（amusement_capacity_config）。
        repository.saveCapacityConfig(new AmusementCapacityConfigRecord(
                "amcap_jdbc", "project_jdbc", null, "device_1", 3, "", true, now, now));
        assertThat(repository.findCapacityConfigById("amcap_jdbc"))
                .get().extracting(AmusementCapacityConfigRecord::maxConcurrent, AmusementCapacityConfigRecord::enabled)
                .containsExactly(3, true);
        repository.saveCapacityConfig(new AmusementCapacityConfigRecord(
                "amcap_jdbc", "project_jdbc", null, "device_1", 5, "", false, now, now)); // upsert 改值+停用
        assertThat(repository.findCapacityConfigById("amcap_jdbc"))
                .get().extracting(AmusementCapacityConfigRecord::maxConcurrent, AmusementCapacityConfigRecord::enabled)
                .containsExactly(5, false);
        assertThat(repository.listCapacityConfigs("project_jdbc"))
                .extracting(AmusementCapacityConfigRecord::id).contains("amcap_jdbc");
    }

    @Test
    void assignProjectSpaceBackfillDrivesOverSpaceResolutionEndToEnd() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        // 空间归属 merchant_space_8（spaces.merchant_id）。
        jdbcTemplate.update("""
                INSERT INTO spaces (id, project_id, merchant_id, space_name, space_type, polygon_points, status, created_at, updated_at)
                VALUES ('space_amuse_8', NULL, 'merchant_space_8', 'Zone 8', 'amusement_zone', '[]'::jsonb, 'active', NOW(), NOW())
                ON CONFLICT (id) DO NOTHING
                """);
        repository.saveSafetyTemplate(new AmusementSafetyTemplateRecord(
                "template_space_8", "Daily Opening 8",
                List.of(Map.of("name", "restraint", "required", true)),
                LocalDate.parse("2026-06-14"), "active", List.of(), now, now, "merchant_space_8"));
        repository.saveProject(new AmusementProjectRecord(
                "project_space_8", "Space8 Coaster", 2, "open", "template_space_8", Map.of(), "point_space_8", now, now, "merchant_space_8"));
        repository.saveSafetyInspection(new AmusementSafetyInspectionRecord(
                "inspection_space_8", "project_space_8", "template_space_8",
                List.of(Map.of("name", "restraint", "result", "pass")),
                "pass", "submitted", "none", "not_required",
                now, now, null, null, "passed", List.of(), now, now,
                "inspector_space_8", "pass", false, null));

        // 回填前：项目出现在「未回填空间」清单；inspection 无法解析到 space（over_space 保守放行）。
        assertThat(repository.listProjectsWithoutSpace(java.util.Optional.of("merchant_space_8")))
                .extracting(AmusementProjectSpaceBacklogRecord::id)
                .contains("project_space_8");
        assertThat(admissionFactRepository.resolveSpaceId("inspection", "inspection_space_8")).isEmpty();

        // 指派/回填 space_id。
        assertThat(repository.findSpaceMerchant("space_amuse_8")).contains("merchant_space_8");
        assertThat(repository.assignProjectSpace("project_space_8", "space_amuse_8")).isEqualTo(1);

        // 回填后：inspection 经 project_id → amusement_projects.space_id 解析到该 space（over_space 真实生效）。
        assertThat(admissionFactRepository.resolveSpaceId("inspection", "inspection_space_8"))
                .contains("space_amuse_8");
        // 项目不再出现在「未回填空间」清单。
        assertThat(repository.listProjectsWithoutSpace(java.util.Optional.of("merchant_space_8")))
                .extracting(AmusementProjectSpaceBacklogRecord::id)
                .doesNotContain("project_space_8");
    }

    private String regclass(String tableName) {
        return namedParameterJdbcTemplate.queryForObject("""
                SELECT to_regclass(:qualified_name)::text
                """, Map.of("qualified_name", "public." + tableName), String.class);
    }

    private List<String> permissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM permissions
                 ORDER BY permission_code
                """, String.class);
    }

    private List<String> platformAdminPermissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM role_permissions
                 WHERE role_id = 'role_platform_admin'
                 ORDER BY permission_code
                """, String.class);
    }

    private String currentFlywayVersion() {
        return jdbcTemplate.queryForObject("""
                SELECT version
                  FROM flyway_schema_history
                 WHERE success = TRUE
                 ORDER BY installed_rank DESC
                 LIMIT 1
                """, String.class);
    }
}
