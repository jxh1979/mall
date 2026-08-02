package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.amusement.AmusementExitRequest;
import com.complex.platform.amusement.AmusementManualOverrideRequest;
import com.complex.platform.amusement.AmusementProjectRecord;
import com.complex.platform.amusement.AmusementRepository;
import com.complex.platform.amusement.AmusementSafetyInspectionRecord;
import com.complex.platform.amusement.AmusementSafetyTemplateRecord;
import com.complex.platform.amusement.AmusementService;
import com.complex.platform.amusement.AmusementSessionRecord;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 阶段2（2026-07-05 用户拍板）游乐核销：出场核销（并发容量回收）+ 主管释放 + 并发容量硬化（唯一约束防超扣）
 * 端到端验收（容量/幂等/并发铁律，含证伪）。
 *
 * <p>用例：
 * <ol>
 *   <li>自由进出型入场 +N → 出场 -N：并发在场快照(project 维)回落到 0、入场记录 released_at 落值、写一条 direction=exit。</li>
 *   <li>同一入场重复出场只释放一次（幂等）：第二次不再减容量、不新增 exit、返回既有 exit。</li>
 *   <li>主管释放（release override）：清零项目并发在场快照 + 落审计 override 记录（status=released）。</li>
 *   <li>并发双提交（同权益+同场次两次入场）：唯一约束只让一条成功，另一条被拒（不超扣）。</li>
 *   <li>无未释放入场即出场 → ENTITLEMENT_NO_ACTIVE_ENTRY，不误减、不写脏 exit。</li>
 *   <li>场次/批次型席位不受释放影响：出场与主管释放都只回收并发在场（point_device/project 维），
 *       amusement_sessions.used_capacity 恒不变（木马每轮/密室每批满了等下一场）。</li>
 * </ol>
 * 证伪：注掉出场 -N（decrementConcurrentSnapshot）→ 用例1 快照断言红；去掉部分唯一索引 → 用例4 双入场断言红；
 * 把 clearProjectConcurrentCapacity/releaseEntryConcurrentCapacity 的维度过滤放开到 session 维 → 用例6 断言红。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class AmusementExitReleaseJdbcIntegrationTests {

    @Autowired
    private TicketingService ticketingService;

    @Autowired
    private JdbcTicketingRepository ticketingRepository;

    @Autowired
    private AmusementService amusementService;

    @Autowired
    private AmusementRepository amusementRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<String> seededCodes = new ArrayList<>();
    private final List<String> seededProjects = new ArrayList<>();
    private String deviceCode;

    @AfterEach
    void cleanup() {
        com.complex.platform.support.TestSecurityActors.clear();
        for (String project : seededProjects) {
            jdbcTemplate.update("DELETE FROM amusement_verifications WHERE project_id = ?", project);
            jdbcTemplate.update("DELETE FROM amusement_capacity_snapshot WHERE project_id = ?", project);
            jdbcTemplate.update("DELETE FROM amusement_manual_overrides WHERE project_id = ?", project);
            jdbcTemplate.update("DELETE FROM amusement_safety_actions WHERE inspection_id IN "
                    + "(SELECT id FROM amusement_safety_inspections WHERE project_id = ?)", project);
            jdbcTemplate.update("DELETE FROM amusement_safety_inspections WHERE project_id = ?", project);
            jdbcTemplate.update("DELETE FROM amusement_sessions WHERE project_id = ?", project);
            jdbcTemplate.update("DELETE FROM amusement_projects WHERE id = ?", project);
            jdbcTemplate.update("DELETE FROM amusement_safety_templates WHERE id = ?",
                    project.replace("project_amuse_", "template_amuse_"));
        }
        for (String code : seededCodes) {
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, "DELETE FROM settlement_transactions WHERE entitlement_id = ?", "ent_" + code);
            jdbcTemplate.update("DELETE FROM ticket_verifications WHERE entitlement_id = ?", "ent_" + code);
            jdbcTemplate.update("DELETE FROM issued_entitlements WHERE id = ?", "ent_" + code);
            jdbcTemplate.update("DELETE FROM ticket_orders WHERE id = ?", "order_" + code);
            jdbcTemplate.update("DELETE FROM entitlement_templates WHERE id = ?", "et_" + code);
            jdbcTemplate.update("DELETE FROM ticket_types WHERE id = ?", "tt_" + code);
        }
        if (deviceCode != null) {
            jdbcTemplate.update("DELETE FROM terminal_devices WHERE id = ?", deviceCode);
        }
    }

    @Test
    void freeFlowEntryThenExitReleasesConcurrentCapacity() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String pointId = "point_exit_" + suffix;
        String projectId = seedOpenProjectWithSession(suffix, pointId, 5);
        String code = seedAmusementEntitlement(suffix, 2); // total_times=2：允许出场后再入

        // 入场：project 维并发快照 +1（verified_people 默认 1）。
        var entry = verify(suffix, code, pointId);
        assertThat(entry.status()).isEqualTo("success");
        assertThat(projectSnapshotOccupied(projectId)).isEqualTo(1);   // 入场 +1
        assertThat(activeEntryCount(projectId)).isEqualTo(1);          // 一条未释放入场

        // 出场：同权益 → 定位未释放入场 → project 维并发快照 -1 回落 0、入场 released_at 落值、写 exit 记录。
        var exit = amusementService.exitVerification(new AmusementExitRequest(
                "exit-" + suffix, code, pointId, deviceCode, "staff_" + suffix, List.of()));
        assertThat(exit.direction()).isEqualTo("exit");
        assertThat(projectSnapshotOccupied(projectId)).isEqualTo(0);   // 出场 -1（证伪：注掉 -N 则仍=1，断言红）
        assertThat(activeEntryCount(projectId)).isEqualTo(0);          // 入场已释放
        assertThat(exitCount(projectId)).isEqualTo(1);                 // 一条 direction=exit
    }

    @Test
    void repeatExitReleasesOnlyOnce() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String pointId = "point_idem_" + suffix;
        String projectId = seedOpenProjectWithSession(suffix, pointId, 5);
        String code = seedAmusementEntitlement(suffix, 2);
        verify(suffix, code, pointId);
        // 人为再占 1（模拟另一在场客人），确认重复出场不误减到该客人。
        jdbcTemplate.update("UPDATE amusement_capacity_snapshot SET occupied_count = occupied_count + 1 "
                + "WHERE project_id = ? AND dimension = 'project'", projectId);
        assertThat(projectSnapshotOccupied(projectId)).isEqualTo(2);

        var first = amusementService.exitVerification(new AmusementExitRequest(
                "exit1-" + suffix, code, pointId, deviceCode, "staff", List.of()));
        var second = amusementService.exitVerification(new AmusementExitRequest(
                "exit2-" + suffix, code, pointId, deviceCode, "staff", List.of()));

        assertThat(first.entryVerificationId()).isEqualTo(second.entryVerificationId()); // 幂等：同一入场
        assertThat(projectSnapshotOccupied(projectId)).isEqualTo(1);   // 只 -1 一次（证伪：重复减则=0，断言红）
        assertThat(exitCount(projectId)).isEqualTo(1);                 // 只一条 exit
    }

    @Test
    void supervisorReleaseClearsConcurrentCapacityWithAudit() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String pointId = "point_sup_" + suffix;
        String projectId = seedOpenProjectWithSession(suffix, pointId, 5);
        String code = seedAmusementEntitlement(suffix, 1);
        verify(suffix, code, pointId);
        assertThat(projectSnapshotOccupied(projectId)).isEqualTo(1);

        var override = amusementService.createManualOverride(new AmusementManualOverrideRequest(
                "sup-release-" + suffix, "night clearance", List.of("att_evidence"),
                projectId, null, "release", "漏扫出场堆积，主管异常清场"));

        assertThat(override.overrideType()).isEqualTo("release");
        assertThat(override.status()).isEqualTo("released");           // 审计留痕
        assertThat(projectSnapshotOccupied(projectId)).isEqualTo(0);   // 并发在场清零
    }

    @Test
    void concurrentDoubleEntrySameEntitlementRejectedByUniqueConstraint() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String pointId = "point_dbl_" + suffix;
        String projectId = seedOpenProjectWithSession(suffix, pointId, 5);
        String code = seedAmusementEntitlement(suffix, 2);

        // 第一次入场（正路，成功、留 entitlement_id）。
        var first = verify(suffix, code, pointId);
        assertThat(first.status()).isEqualTo("success");
        String entId = jdbcTemplate.queryForObject(
                "SELECT id FROM issued_entitlements WHERE entitlement_code = ?", String.class, code);
        String sessionId = jdbcTemplate.queryForObject(
                "SELECT session_id FROM amusement_verifications WHERE project_id = ? AND direction = 'entry' LIMIT 1",
                String.class, projectId);

        // 并发第二次提交（不同幂等键→不同 amusement id，绕过 stage1 请求级幂等）同一 (entitlement, session) 的
        // 未释放入场 → 撞 uk_amusement_verifications_active_entry 部分唯一索引被 DB 拒（并发硬化，防超扣）。
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO amusement_verifications (id, project_id, session_id, credential_payload,
                    device_id, staff_id, verification_result, failure_reason, evidence_attachment_ids,
                    created_at, verified_people, direction, released_at, entry_verification_id,
                    entitlement_id, location_point_id)
                VALUES (?, ?, ?, ?, NULL, 'staff', 'success', NULL, '[]'::jsonb, NOW(), 1, 'entry', NULL, NULL, ?, ?)
                """, "verification_dbl2_" + suffix, projectId, sessionId, code, entId, pointId))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        assertThat(activeEntryCount(projectId)).isEqualTo(1);          // 只一条未释放入场（证伪：去掉唯一索引则=2，断言红）
    }

    @Test
    void exitWithoutActiveEntryRejects() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String pointId = "point_noentry_" + suffix;
        String projectId = seedOpenProjectWithSession(suffix, pointId, 5);
        String code = seedAmusementEntitlement(suffix, 1); // 未曾入场

        assertThatThrownBy(() -> amusementService.exitVerification(new AmusementExitRequest(
                "exit-noentry-" + suffix, code, pointId, deviceCode, "staff", List.of())))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("没有进行中的游乐入场");
        assertThat(exitCount(projectId)).isEqualTo(0);                 // 不写脏 exit
    }

    /**
     * 场次/批次型（木马每轮、密室每批）席位不因出场或主管释放回收——满了等下一场。
     *
     * <p>判定依据是【容量落在哪个维度】，不是项目上的类型开关：场次/批次席位记在
     * amusement_sessions.used_capacity（session 维），而出场释放 releaseEntryConcurrentCapacity 与
     * 主管释放 clearProjectConcurrentCapacity 都只动 amusement_capacity_snapshot 的
     * point_device / project 两维 → session 维结构性不可达，无需也不会被释放。
     */
    @Test
    void sessionBatchCapacityUnaffectedByExitAndSupervisorRelease() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String pointId = "point_sess_" + suffix;
        String projectId = seedOpenProjectWithSession(suffix, pointId, 5);
        String code = seedAmusementEntitlement(suffix, 2);

        // 入场：并发在场 +1（自由进出维），同时场次席位 used_capacity +1（场次/批次维）。
        var entry = verify(suffix, code, pointId);
        assertThat(entry.status()).isEqualTo("success");
        assertThat(projectSnapshotOccupied(projectId)).isEqualTo(1);
        assertThat(sessionUsedCapacity(projectId)).isEqualTo(1);

        // 出场：只回收并发在场，场次席位不退（该轮座位已消耗）。
        amusementService.exitVerification(new AmusementExitRequest(
                "exit-sess-" + suffix, code, pointId, deviceCode, "staff", List.of()));
        assertThat(projectSnapshotOccupied(projectId)).isEqualTo(0);   // 并发在场回收
        assertThat(sessionUsedCapacity(projectId)).isEqualTo(1);       // 场次席位不变（证伪：若误减则=0，断言红）

        // 主管释放（异常清场）：同样只清并发在场快照，场次席位仍不受影响。
        amusementService.createManualOverride(new AmusementManualOverrideRequest(
                "sup-sess-" + suffix, "night clearance", List.of(),
                projectId, null, "release", "异常清场核对场次席位不受影响"));
        assertThat(projectSnapshotOccupied(projectId)).isEqualTo(0);
        assertThat(sessionUsedCapacity(projectId)).isEqualTo(1);       // 场次席位仍不变（证伪：若清零则=0，断言红）
    }

    // ===== helpers =====

    private VerificationResponse verify(String suffix, String code, String pointId) {
        return ticketingService.createVerification(new VerificationRequest(
                "idem-" + suffix + "-" + System.nanoTime(), "qr", code, deviceCode, pointId, Map.of()));
    }

    private int projectSnapshotOccupied(String projectId) {
        Integer v = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(occupied_count), 0) FROM amusement_capacity_snapshot "
                        + "WHERE project_id = ? AND dimension = 'project'", Integer.class, projectId);
        return v == null ? 0 : v;
    }

    /** 场次/批次型席位占用（session 维，落在 amusement_sessions.used_capacity）。 */
    private int sessionUsedCapacity(String projectId) {
        Integer v = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(used_capacity), 0) FROM amusement_sessions WHERE project_id = ?",
                Integer.class, projectId);
        return v == null ? 0 : v;
    }

    private int activeEntryCount(String projectId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM amusement_verifications WHERE project_id = ? "
                        + "AND direction = 'entry' AND verification_result = 'success' AND released_at IS NULL",
                Integer.class, projectId);
    }

    private int exitCount(String projectId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM amusement_verifications WHERE project_id = ? AND direction = 'exit'",
                Integer.class, projectId);
    }

    private void seedDevice(String suffix) {
        deviceCode = "device_amuse_" + suffix;
        java.sql.Timestamp ts = java.sql.Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope,
                    status, created_at, updated_at)
                VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', ?, ?)
                """, deviceCode, "dev-" + deviceCode, "DC-" + deviceCode, ts, ts);
    }

    private String seedOpenProjectWithSession(String suffix, String pointId, int capacity) {
        Instant now = Instant.now();
        String projectId = "project_amuse_" + suffix;
        seededProjects.add(projectId);
        String templateId = "template_amuse_" + suffix;
        amusementRepository.saveSafetyTemplate(new AmusementSafetyTemplateRecord(
                templateId, "Daily Opening " + suffix,
                List.of(Map.of("name", "restraint", "required", true)),
                LocalDate.now(), "active", List.of(), now, now, null));
        amusementRepository.saveProject(new AmusementProjectRecord(
                projectId, "Amuse " + suffix, capacity, "open", templateId, Map.of(), pointId, now, now, null));
        amusementRepository.saveSafetyInspection(new AmusementSafetyInspectionRecord(
                "inspection_amuse_" + suffix, projectId, null, templateId,
                List.of(Map.of("name", "restraint", "result", "pass")),
                "pass", "submitted", "none", "not_required",
                now, now, null, null, "passed", List.of(), now, now,
                "inspector_" + suffix, "pass", false, null));
        amusementRepository.saveSession(new AmusementSessionRecord(
                "session_amuse_" + suffix, projectId, now.minusSeconds(600), now.plusSeconds(3600),
                capacity, 0, 0, "open", List.of(), now, now));
        return projectId;
    }

    private String seedAmusementEntitlement(String suffix, int totalTimes) {
        String code = "amuse-code-" + suffix;
        seededCodes.add(code);
        Instant now = Instant.now();
        java.sql.Timestamp ts = java.sql.Timestamp.from(now);
        String typeId = "tt_" + code;
        String templateId = "et_" + code;
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status,
                    created_at, updated_at)
                VALUES (?, 'Amuse Type', 'fixed', FALSE, 'active', ?, ?)
                """, typeId, ts, ts);
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times,
                    default_value_cent, status, entitlement_kind, created_at, updated_at)
                VALUES (?, 'AmusePass', 'amusement_pass', ?, 5500, 'active', 'virtual', ?, ?)
                """, templateId, totalTimes, ts, ts);
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_" + code, "ON-" + code, "cust_" + code, typeId, null, 1,
                LocalDate.parse("2026-06-18"), "web", null, 0L, "CNY", "paid", "issued", "created", now, now));
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, typeId, "order_" + code, templateId, "amusement_pass",
                "merchant_a", null, null, totalTimes, 0, 5500L, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, null,
                "virtual", null));
        return code;
    }
}
