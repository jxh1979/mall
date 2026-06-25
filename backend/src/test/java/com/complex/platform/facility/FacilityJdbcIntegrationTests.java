package com.complex.platform.facility;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.support.MigrationVersions;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
class FacilityJdbcIntegrationTests {

    @Autowired
    private JdbcFacilityRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesFacilityTablesAndSeedsPermissions() {
        assertThat(regclass("facility_equipment")).isEqualTo("facility_equipment");
        assertThat(regclass("facility_work_orders")).isEqualTo("facility_work_orders");
        assertThat(regclass("facility_inspection_templates")).isEqualTo("facility_inspection_templates");
        assertThat(regclass("facility_inspection_tasks")).isEqualTo("facility_inspection_tasks");
        assertThat(regclass("facility_inspection_records")).isEqualTo("facility_inspection_records");
        assertThat(currentFlywayVersion()).isEqualTo(MigrationVersions.latest());
        assertThat(permissionCodes()).contains("facility.equipment.manage", "facility.work_order.manage", "facility.inspection.manage");
        assertThat(platformAdminPermissionCodes()).contains("facility.equipment.manage", "facility.work_order.manage", "facility.inspection.manage");
    }

    @Test
    void repositoryPersistsInspectionRoundTripWithJsonb() {
        Instant now = Instant.parse("2026-06-24T00:00:00Z");
        repository.saveInspectionTemplate(new FacilityInspectionTemplateRecord(
                "fitpl_jdbc", "月检", "hvac", List.of("查滤网", "测温度"), "active", null, null, now, now));
        // JSONB items 读回对拍（writeJson/readJson 双向）。
        assertThat(repository.listInspectionTemplates())
                .filteredOn(t -> t.id().equals("fitpl_jdbc"))
                .singleElement()
                .extracting(FacilityInspectionTemplateRecord::items)
                .isEqualTo(List.of("查滤网", "测温度"));

        // 巡检任务挂设备（FK），自然键防重。
        repository.saveEquipment(new FacilityEquipmentRecord(
                "feq_insp", "E", "hvac", null, null, null, null, null, "active", 30, null, null, null, now, now));
        assertThat(repository.upsertInspectionTask(new FacilityInspectionTaskRecord(
                "fitask_jdbc", "feq_insp", null, now, "pending", now, null, null, null, null, now, now))).isTrue();
        assertThat(repository.upsertInspectionTask(new FacilityInspectionTaskRecord(
                "fitask_jdbc2", "feq_insp", null, now, "pending", now, null, null, null, null, now, now))).isFalse();

        // 巡检记录 checked_items JSONB 写入（cast 不报错即通过）。
        repository.saveInspectionRecord(new FacilityInspectionRecordRecord(
                "firec_jdbc", "fitask_jdbc", "feq_insp", "insp", "pass", "正常", List.of("外观"), null, null, now));
        assertThat(repository.findInspectionTaskById("fitask_jdbc")).isPresent();
    }

    @Test
    void repositoryPersistsEquipmentAndReadsBack() {
        Instant now = Instant.parse("2026-06-24T00:00:00Z");
        repository.saveEquipment(new FacilityEquipmentRecord(
                "eq_jdbc",
                "中央空调JDBC",
                "hvac",
                "sp_jdbc",
                "格力",
                "GX-100",
                LocalDate.parse("2025-01-01"),
                LocalDate.parse("2027-01-01"),
                "active",
                90,
                null,
                null,
                null,
                now,
                now));

        assertThat(repository.findEquipmentById("eq_jdbc"))
                .isPresent()
                .get()
                .extracting(
                        FacilityEquipmentRecord::name,
                        FacilityEquipmentRecord::equipmentType,
                        FacilityEquipmentRecord::status,
                        FacilityEquipmentRecord::maintenanceCycleDays)
                .containsExactly("中央空调JDBC", "hvac", "active", 90);
        assertThat(repository.listEquipment())
                .extracting(FacilityEquipmentRecord::id)
                .contains("eq_jdbc");
    }

    @Test
    void repositoryPersistsWorkOrderAndReadsBack() {
        Instant now = Instant.parse("2026-06-24T00:00:00Z");
        repository.saveWorkOrder(new FacilityWorkOrderRecord(
                "fwo_jdbc",
                null,
                "sp_jdbc",
                "电梯异响",
                "三号梯异响明显",
                "repair",
                "high",
                "open",
                null,
                "u_report",
                now.plus(24, ChronoUnit.HOURS),
                null,
                null,
                null,
                now,
                now));

        assertThat(repository.findWorkOrderById("fwo_jdbc"))
                .isPresent()
                .get()
                .extracting(
                        FacilityWorkOrderRecord::title,
                        FacilityWorkOrderRecord::category,
                        FacilityWorkOrderRecord::priority,
                        FacilityWorkOrderRecord::status,
                        FacilityWorkOrderRecord::slaDueAt)
                .containsExactly("电梯异响", "repair", "high", "open", now.plus(24, ChronoUnit.HOURS));
        assertThat(repository.listWorkOrders())
                .extracting(FacilityWorkOrderRecord::id)
                .contains("fwo_jdbc");
    }

    private String regclass(String tableName) {
        return jdbcTemplate.queryForObject("SELECT to_regclass(?)::text", String.class, tableName);
    }

    private String currentFlywayVersion() {
        return jdbcTemplate.queryForObject("""
                SELECT version
                  FROM flyway_schema_history
                 WHERE success = true
                 ORDER BY installed_rank DESC
                 LIMIT 1
                """, String.class);
    }

    private List<String> permissionCodes() {
        return jdbcTemplate.queryForList("SELECT permission_code FROM permissions", String.class);
    }

    private List<String> platformAdminPermissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM role_permissions
                 WHERE role_id = 'role_platform_admin'
                """, String.class);
    }
}
