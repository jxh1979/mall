package com.complex.platform.terminal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class MdmJdbcIntegrationTests {

    @Autowired
    private JdbcMdmRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void reset() {
        jdbcTemplate.execute("TRUNCATE mdm_app_whitelist, mdm_site_whitelist, mdm_policy_violations");
    }

    @Test
    void migrationCreatesMdmTablesColumnsSwitchAndPermissions() {
        assertThat(regclass("mdm_app_whitelist")).isEqualTo("mdm_app_whitelist");
        assertThat(regclass("mdm_site_whitelist")).isEqualTo("mdm_site_whitelist");
        assertThat(regclass("mdm_policy_violations")).isEqualTo("mdm_policy_violations");

        assertThat(columnExists("terminal_devices", "management_status")).isTrue();
        assertThat(columnExists("terminal_devices", "policy_status")).isTrue();
        assertThat(columnExists("terminal_devices", "last_policy_sync_at")).isTrue();
        assertThat(columnExists("terminal_devices", "enrolled_at")).isTrue();

        // MDM 开关默认关：显式 enabled=false 行存在。
        Boolean enabled = jdbcTemplate.queryForObject("""
                SELECT enabled FROM terminal_capability_switches
                 WHERE capability_code = 'managed_work_devices'
                   AND terminal_type = '*' AND scope_type = 'global' AND scope_id = 'global'
                """, Boolean.class);
        assertThat(enabled).isFalse();

        assertThat(permissionCodes()).contains(
                "mdm.policy.manage", "mdm.device.manage", "mdm.violation.view");
        assertThat(platformAdminPermissionCodes()).contains(
                "mdm.policy.manage", "mdm.device.manage", "mdm.violation.view");
    }

    @Test
    void appWhitelistRoundTrips() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveAppWhitelist(new MdmAppWhitelistRecord(
                "mdm_app_wecom", "com.tencent.wework", "WeCom", "global", "global", true, "allow", now, now));

        assertThat(repository.listAppWhitelist())
                .extracting(MdmAppWhitelistRecord::packageName)
                .containsExactly("com.tencent.wework");
    }

    @Test
    void managementUpdateProjectsBackOntoTerminalDevices() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        insertDevice("device_mgmt");

        repository.updateDeviceManagement("device_mgmt", "compliant", "applied", now, now);

        assertThat(repository.findDeviceManagement("device_mgmt"))
                .get()
                .extracting(MdmDeviceManagementRecord::managementStatus, MdmDeviceManagementRecord::policyStatus)
                .containsExactly("compliant", "applied");
    }

    @Test
    void violationInsertAndListByDevice() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveViolation(new MdmPolicyViolationRecord(
                "mdm_vio_1", "device_x", "unauthorized_app", "com.evil", now, false, null, null, null, now));

        List<MdmPolicyViolationRecord> rows = repository.listViolations("device_x");
        assertThat(rows).extracting(MdmPolicyViolationRecord::violationType).containsExactly("unauthorized_app");
    }

    @Test
    void checkRejectsInvalidManagementStatusEnum() {
        insertDevice("device_bad_status");
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE terminal_devices SET management_status = 'bogus' WHERE device_id = 'device_bad_status'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkRejectsInvalidViolationTypeEnum() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO mdm_policy_violations (id, device_id, violation_type, occurred_at, handled, created_at)
                VALUES ('mdm_vio_bad', 'device_x', 'bogus_type', now(), false, now())
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void v097SeedsCameraScanAndNativeClientSwitchesDefaultOff() {
        // V097：摄像头扫码（#2）+ 微信小程序/原生 App 客户端（#3）能力开关，显式 seed enabled=false（默认关）。
        Boolean cameraScan = jdbcTemplate.queryForObject("""
                SELECT enabled FROM terminal_capability_switches
                 WHERE capability_code = 'terminal.camera_scan.enabled'
                   AND terminal_type = '*' AND scope_type = 'global' AND scope_id = 'global'
                """, Boolean.class);
        assertThat(cameraScan).isFalse();

        Boolean nativeClient = jdbcTemplate.queryForObject("""
                SELECT enabled FROM terminal_capability_switches
                 WHERE capability_code = 'customer.native_client.enabled'
                   AND terminal_type = '*' AND scope_type = 'global' AND scope_id = 'global'
                """, Boolean.class);
        assertThat(nativeClient).isFalse();
    }

    @Test
    void v098SeedsDeviceKeystoreAndTabletSwitchesDefaultOff() {
        // V098：员工设备 Keystore 凭证绑定（item1）+ 平板终端（②）能力开关，显式 seed enabled=false（默认关）。
        Boolean deviceKeystore = jdbcTemplate.queryForObject("""
                SELECT enabled FROM terminal_capability_switches
                 WHERE capability_code = 'staff.device_keystore.enabled'
                   AND terminal_type = '*' AND scope_type = 'global' AND scope_id = 'global'
                """, Boolean.class);
        assertThat(deviceKeystore).isFalse();

        Boolean tablet = jdbcTemplate.queryForObject("""
                SELECT enabled FROM terminal_capability_switches
                 WHERE capability_code = 'terminal.tablet.enabled'
                   AND terminal_type = '*' AND scope_type = 'global' AND scope_id = 'global'
                """, Boolean.class);
        assertThat(tablet).isFalse();
    }

    @Test
    void v107AddsLifecycleStatusCheckAndDispositionColumns() {
        // V107：terminal_devices.status CHECK + mdm_policy_violations 处置字段。
        assertThat(columnExists("mdm_policy_violations", "disposition")).isTrue();
        assertThat(columnExists("mdm_policy_violations", "blocked_at")).isTrue();

        insertDevice("device_lifecycle");
        // 合法生命周期态可写入。
        jdbcTemplate.update(
                "UPDATE terminal_devices SET status = 'scrapped' WHERE device_id = 'device_lifecycle'");
        // management_status 新增枚举 blocked 可写入。
        jdbcTemplate.update(
                "UPDATE terminal_devices SET management_status = 'blocked' WHERE device_id = 'device_lifecycle'");

        assertThat(permissionCodes()).contains("terminal.device.lifecycle", "mdm.violation.handle");

        // 非法 status 被 CHECK 拒绝。注意：负向断言会令当前事务进入 aborted（25P02），
        // 同一 @Transactional 事务内后续 SQL 会被忽略，故置于本方法所有合法操作之后。
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE terminal_devices SET status = 'bogus_status' WHERE device_id = 'device_lifecycle'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void v107RejectsInvalidDispositionEnum() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveViolation(new MdmPolicyViolationRecord(
                "mdm_vio_disp", "device_x", "other", "x", now, false, null, null, null, now));
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE mdm_policy_violations SET disposition = 'bogus' WHERE id = 'mdm_vio_disp'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkRejectsInvalidWhitelistScopeType() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO mdm_app_whitelist (id, package_name, scope_type, scope_id, enabled, created_at, updated_at)
                VALUES ('mdm_app_bad', 'com.x', 'bogus', 'global', true, now(), now())
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertDevice(String deviceId) {
        jdbcTemplate.update("""
                INSERT INTO terminal_devices (
                    id, device_id, device_code, terminal_type, binding_scope, status, created_at, updated_at
                )
                VALUES (:id, :did, :did, 'staff_work_phone', 'project', 'bound', now(), now())
                ON CONFLICT (device_id) DO NOTHING
                """.replace(":id", "'dev_" + deviceId + "'").replace(":did", "'" + deviceId + "'"));
    }

    private String regclass(String table) {
        return jdbcTemplate.queryForObject("SELECT to_regclass(?)::text", String.class, table);
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                 WHERE table_name = ? AND column_name = ?
                """, Integer.class, table, column);
        return count != null && count > 0;
    }

    private List<String> permissionCodes() {
        return jdbcTemplate.queryForList("SELECT permission_code FROM permissions", String.class);
    }

    private List<String> platformAdminPermissionCodes() {
        return jdbcTemplate.queryForList(
                "SELECT permission_code FROM role_permissions WHERE role_id = 'role_platform_admin'", String.class);
    }
}
