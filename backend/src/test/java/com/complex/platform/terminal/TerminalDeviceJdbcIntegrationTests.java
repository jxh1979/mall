package com.complex.platform.terminal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class TerminalDeviceJdbcIntegrationTests {

    @Autowired
    private JdbcTerminalDeviceRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetTerminalDeviceTables() {
        // CASCADE：V159 后 ticket_verifications.device_id 外键引用 terminal_devices，
        // 不级联则 TRUNCATE 报 "cannot truncate a table referenced in a foreign key constraint"。
        jdbcTemplate.execute("TRUNCATE terminal_device_credentials, terminal_device_status_events, terminal_devices CASCADE");
    }

    @Test
    void migrationCreatesTerminalDeviceTablesAndSeedsPermissions() {
        assertThat(regclass("terminal_devices")).isEqualTo("terminal_devices");
        assertThat(regclass("terminal_device_status_events")).isEqualTo("terminal_device_status_events");
        assertThat(regclass("terminal_capability_switches")).isEqualTo("terminal_capability_switches");
        assertThat(regclass("terminal_kiosk_configs")).isEqualTo("terminal_kiosk_configs");
        assertThat(regclass("terminal_kiosk_heartbeats")).isEqualTo("terminal_kiosk_heartbeats");
        assertThat(regclass("terminal_device_credentials")).isEqualTo("terminal_device_credentials");

        assertThat(permissionCodes()).contains(
                "terminal.device.bind",
                "terminal.device.unbind",
                "terminal.switch.view",
                "terminal.switch.manage",
                "evidence.event.create",
                "evidence.event.view"
        );
        assertThat(platformAdminPermissionCodes()).contains(
                "terminal.device.bind",
                "terminal.device.unbind",
                "terminal.switch.view",
                "terminal.switch.manage",
                "evidence.event.create",
                "evidence.event.view"
        );
    }

    @Test
    void deviceBindAndReturnPersistsFormalFields() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.save(new TerminalDeviceRecord(
                "dev_jdbc",
                "P0-ANDROID-JDBC",
                "P0-ANDROID-JDBC",
                "staff_work_phone",
                "space",
                "point_jdbc",
                "mdm_jdbc",
                null,
                null,
                null,
                null,
                "point_jdbc",
                "staff_jdbc",
                "bound",
                now,
                null,
                null,
                null,
                null,
                now,
                now
        ));
        repository.saveStatusEvent(new TerminalDeviceStatusEventRecord(
                "dev_evt_bind",
                "dev_jdbc",
                "P0-ANDROID-JDBC",
                "terminal.device.bound",
                "bound",
                null,
                "staff_jdbc",
                now
        ));

        TerminalDeviceRecord bound = repository.findByDeviceCode("P0-ANDROID-JDBC").orElseThrow();
        repository.save(new TerminalDeviceRecord(
                bound.id(),
                bound.deviceId(),
                bound.deviceCode(),
                bound.terminalType(),
                bound.bindingScope(),
                bound.boundSpaceId(),
                bound.mdmDeviceRef(),
                bound.kioskMode(),
                bound.merchantId(),
                bound.venueId(),
                bound.areaId(),
                bound.locationPointId(),
                bound.staffId(),
                "returned",
                bound.boundAt(),
                now.plusSeconds(60),
                null,
                "shift closed",
                null,
                bound.createdAt(),
                now.plusSeconds(60)
        ));
        repository.saveStatusEvent(new TerminalDeviceStatusEventRecord(
                "dev_evt_return",
                "dev_jdbc",
                "P0-ANDROID-JDBC",
                "terminal.device.returned",
                "returned",
                "shift closed",
                "staff_jdbc",
                now.plusSeconds(60)
        ));

        assertThat(repository.findByDeviceCode("P0-ANDROID-JDBC"))
                .get()
                .extracting(
                        TerminalDeviceRecord::deviceId,
                        TerminalDeviceRecord::terminalType,
                        TerminalDeviceRecord::boundSpaceId,
                        TerminalDeviceRecord::status,
                        TerminalDeviceRecord::returnReason
                )
                .containsExactly("P0-ANDROID-JDBC", "staff_work_phone", "point_jdbc", "returned", "shift closed");
        assertThat(statusEvents())
                .contains(
                        tuple("terminal.device.bound", "bound"),
                        tuple("terminal.device.returned", "returned")
                );
    }

    @Test
    void capabilitySwitchKioskConfigAndHeartbeatPersistFormalFields() {
        Instant now = Instant.parse("2026-06-14T07:00:00Z");
        repository.saveCapabilitySwitch(new TerminalCapabilitySwitchRecord(
                "switch_jdbc",
                "kiosk",
                "ticketing",
                "device",
                "device_kiosk_jdbc",
                true,
                "enabled",
                now,
                now
        ));
        repository.saveKioskConfig(new TerminalKioskConfigRecord(
                "device_kiosk_jdbc",
                "ticketing",
                true,
                "project:p0",
                Map.of("home", "ticketing"),
                now
        ));
        repository.saveKioskHeartbeat(new KioskHeartbeatRecord(
                "device_kiosk_jdbc",
                "online",
                "1.0.0",
                List.of("ticketing"),
                null,
                "ticketing",
                now.plusSeconds(60)
        ));

        assertThat(repository.listCapabilitySwitches())
                .extracting(TerminalCapabilitySwitchRecord::id, TerminalCapabilitySwitchRecord::enabled)
                .contains(tuple("switch_jdbc", true));
        assertThat(repository.findKioskConfigByDeviceId("device_kiosk_jdbc"))
                .get()
                .satisfies(config -> {
                    assertThat(config.kioskMode()).isEqualTo("ticketing");
                    assertThat(config.config()).containsEntry("home", "ticketing");
                });
        assertThat(kioskHeartbeatRows())
                .contains(tuple("device_kiosk_jdbc", "online", "1.0.0"));
    }

    @Test
    void deviceCredentialPersistsSha256HashOnly() {
        Instant now = Instant.parse("2026-06-14T08:00:00Z");
        String token = "plain-device-token";
        repository.save(new TerminalDeviceRecord(
                "dev_credential_jdbc",
                "device_credential_jdbc",
                "device_credential_jdbc",
                "kiosk",
                "project",
                "project_p0",
                "mdm_credential",
                "ticketing",
                null,
                null,
                null,
                "project_p0",
                null,
                "bound",
                now,
                null,
                null,
                null,
                null,
                now,
                now
        ));
        repository.saveDeviceCredential(new DeviceCredentialRecord(
                "device_credential_jdbc",
                sha256(token),
                "active",
                now,
                now
        ));

        assertThat(repository.findDeviceCredentialByDeviceId("device_credential_jdbc"))
                .get()
                .satisfies(credential -> {
                    assertThat(credential.tokenSha256()).isEqualTo(sha256(token));
                    assertThat(credential.tokenSha256()).isNotEqualTo(token);
                    assertThat(credential.status()).isEqualTo("active");
                });
        assertThat(rawCredentialRows())
                .contains(tuple("device_credential_jdbc", sha256(token), "active"));
    }

    private String regclass(String tableName) {
        return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
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

    private List<org.assertj.core.groups.Tuple> statusEvents() {
        return jdbcTemplate.query("""
                SELECT event_type, status
                  FROM terminal_device_status_events
                 ORDER BY created_at, id
                """, (resultSet, rowNumber) -> tuple(
                resultSet.getString("event_type"),
                resultSet.getString("status")
        ));
    }

    private List<org.assertj.core.groups.Tuple> kioskHeartbeatRows() {
        return jdbcTemplate.query("""
                SELECT device_id, status, app_version
                  FROM terminal_kiosk_heartbeats
                 ORDER BY device_id
                """, (resultSet, rowNumber) -> tuple(
                resultSet.getString("device_id"),
                resultSet.getString("status"),
                resultSet.getString("app_version")
        ));
    }

    private List<org.assertj.core.groups.Tuple> rawCredentialRows() {
        return jdbcTemplate.query("""
                SELECT device_id, token_sha256, status
                  FROM terminal_device_credentials
                 ORDER BY device_id
                """, (resultSet, rowNumber) -> tuple(
                resultSet.getString("device_id"),
                resultSet.getString("token_sha256"),
                resultSet.getString("status")
        ));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
