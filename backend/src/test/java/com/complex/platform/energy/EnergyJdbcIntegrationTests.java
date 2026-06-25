package com.complex.platform.energy;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.support.MigrationVersions;
import java.math.BigDecimal;
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
class EnergyJdbcIntegrationTests {

    @Autowired
    private JdbcEnergyRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesEnergyTablesAndSeedsPermission() {
        assertThat(regclass("energy_meters")).isEqualTo("energy_meters");
        assertThat(regclass("energy_meter_readings")).isEqualTo("energy_meter_readings");
        assertThat(currentFlywayVersion()).isEqualTo(MigrationVersions.latest());
        assertThat(permissionCodes()).contains("facility.energy.manage");
        assertThat(platformAdminPermissionCodes()).contains("facility.energy.manage");
    }

    @Test
    void repositoryPersistsMeterAndReadingsComputingBaseline() {
        Instant now = Instant.parse("2026-06-24T00:00:00Z");
        repository.saveMeter(new EnergyMeterRecord(
                "emtr_jdbc", "电表JDBC", "electricity", "sp_jdbc", "kWh", "active", null, null, now, now));
        repository.saveReading(new EnergyMeterReadingRecord(
                "erdg_jdbc1", "emtr_jdbc", "2026-05", new BigDecimal("100.0000"), null, "u", null, null, now));
        repository.saveReading(new EnergyMeterReadingRecord(
                "erdg_jdbc2", "emtr_jdbc", "2026-06", new BigDecimal("150.5000"), new BigDecimal("50.5000"), "u", null, null, now));

        // 上期表底（period<2026-06 的最近）= 2026-05 的 100。
        assertThat(repository.findLatestReadingValueBefore("emtr_jdbc", "2026-06").orElseThrow())
                .isEqualByComparingTo("100.0000");
        assertThat(repository.findReading("emtr_jdbc", "2026-05")).isPresent();
        assertThat(repository.listReadings())
                .extracting(EnergyMeterReadingRecord::id)
                .contains("erdg_jdbc1", "erdg_jdbc2");
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
