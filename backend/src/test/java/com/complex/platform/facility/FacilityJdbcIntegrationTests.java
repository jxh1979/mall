package com.complex.platform.facility;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.support.MigrationVersions;
import java.time.Instant;
import java.time.LocalDate;
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
    void migrationCreatesFacilityTableAndSeedsPermission() {
        assertThat(regclass("facility_equipment")).isEqualTo("facility_equipment");
        assertThat(currentFlywayVersion()).isEqualTo(MigrationVersions.latest());
        assertThat(permissionCodes()).contains("facility.equipment.manage");
        assertThat(platformAdminPermissionCodes()).contains("facility.equipment.manage");
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
