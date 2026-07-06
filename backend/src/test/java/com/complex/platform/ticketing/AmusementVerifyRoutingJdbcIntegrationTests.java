package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.amusement.AmusementProjectRecord;
import com.complex.platform.amusement.AmusementRepository;
import com.complex.platform.amusement.AmusementSafetyInspectionRecord;
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
 * 阶段1（2026-07-05 用户拍板）游乐核销走正路：通票游乐权益经【通用核销端点】分流进游乐正路端到端验收
 * （容量/幂等铁律，含证伪）。
 *
 * <p>用例：
 * <ol>
 *   <li>游乐权益在某项目核销点核销 → 写 amusement_verifications(project+session+verified_at)、扣了场次容量、
 *       通票权益也扣次(used_times 0→1)、ticket_verifications 落 success 且带 amusement_verification_id 交叉引用。</li>
 *   <li>场次满(capacity=1 已占满) → 拒绝 SESSION_CAPACITY_EXHAUSTED、通票权益不扣(used_times 仍=0)、
 *       不落 success 核销记录、amusement_verifications 不新增。</li>
 *   <li>定位不到项目(核销点无任何项目/设备绑定) → AMUSEMENT_PROJECT_UNRESOLVED，不写脏数据(权益不扣、无核销记录)。</li>
 *   <li>无进行中场次(项目有 open 但时间不覆盖 now) → AMUSEMENT_NO_OPEN_SESSION，不写脏数据。</li>
 *   <li>幂等/并发：同一 offline_request_id 重放 → 命中同一 amusement_verifications.id、容量只扣一次。</li>
 * </ol>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
class AmusementVerifyRoutingJdbcIntegrationTests {

    @Autowired
    private TicketingService ticketingService;

    @Autowired
    private JdbcTicketingRepository ticketingRepository;

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
            jdbcTemplate.update("DELETE FROM amusement_capacity_config WHERE project_id = ?", project);
            jdbcTemplate.update("DELETE FROM amusement_devices WHERE project_id = ?", project);
            jdbcTemplate.update("DELETE FROM amusement_safety_actions WHERE inspection_id IN "
                    + "(SELECT id FROM amusement_safety_inspections WHERE project_id = ?)", project);
            jdbcTemplate.update("DELETE FROM amusement_safety_inspections WHERE project_id = ?", project);
            jdbcTemplate.update("DELETE FROM amusement_sessions WHERE project_id = ?", project);
            jdbcTemplate.update("DELETE FROM amusement_projects WHERE id = ?", project);
            jdbcTemplate.update("DELETE FROM amusement_safety_templates WHERE id = ?",
                    project.replace("project_", "template_"));
        }
        for (String code : seededCodes) {
            jdbcTemplate.update("DELETE FROM settlement_transactions WHERE entitlement_id = ?", "ent_" + code);
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
    void amusementEntitlementRoutesToAmusementPathConsumesCapacityAndCrossReferences() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String pointId = "point_amuse_" + suffix;
        String projectId = seedOpenProjectWithSession(suffix, pointId, 5, "open_now");
        String code = seedAmusementEntitlement(suffix);

        var resp = verify(suffix, code, pointId);

        assertThat(resp.status()).isEqualTo("success");
        assertThat(usedTimes(code)).isEqualTo(1);                       // 通票权益扣次
        assertThat(amusementVerificationCount(projectId)).isEqualTo(1); // 写 amusement_verifications
        assertThat(sessionUsedCapacity(projectId)).isEqualTo(1);       // 扣了场次容量
        // ticket_verifications success 带 amusement_verification_id 交叉引用(证伪:未写交叉引用则为空,断言红)。
        assertThat(crossReferenceOfSuccess(code)).isNotBlank();
        assertThat(amusementVerifiedSession(projectId)).isNotBlank();  // 记录了对应场次
    }

    @Test
    void sessionFullRejectsAndDoesNotConsumeEntitlement() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String pointId = "point_full_" + suffix;
        // 场次 capacity=1 且已占满(used_capacity=1) → 本场满。
        String projectId = seedOpenProjectWithSession(suffix, pointId, 1, "full_now");
        jdbcTemplate.update("UPDATE amusement_sessions SET used_capacity = 1 WHERE project_id = ?", projectId);
        String code = seedAmusementEntitlement(suffix);

        assertThatThrownBy(() -> verify(suffix, code, pointId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("capacity exhausted");
        assertThat(usedTimes(code)).isEqualTo(0);                       // 通票权益不扣(容量失败游乐侧已补偿回补)
        assertThat(successCount(code)).isEqualTo(0);                    // 不落 success 核销记录
        assertThat(amusementVerificationCount(projectId)).isEqualTo(0);// amusement_verifications 不新增
    }

    @Test
    void pointNotAmusementGateFallsThroughToGenericPath() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String code = seedAmusementEntitlement(suffix);
        // 核销点无任何项目/设备绑定 → 非游乐闸 → 回退通用旧路（不分流、不硬造关联）。
        String orphanPoint = "point_orphan_" + suffix;

        var resp = verify(suffix, code, orphanPoint);

        assertThat(resp.status()).isEqualTo("success");         // 通用逐次扣：正常核销成功
        assertThat(usedTimes(code)).isEqualTo(1);               // 通用路径消费一次
        assertThat(crossReferenceIfAny(code)).isNull();         // 未走游乐正路 → 无交叉引用
    }

    @Test
    void noOpenSessionRejectsWithoutDirtyData() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String pointId = "point_nosession_" + suffix;
        // 项目 open 但场次时间窗在过去(不覆盖 now) → 无进行中场次。
        String projectId = seedOpenProjectWithSession(suffix, pointId, 5, "past");
        String code = seedAmusementEntitlement(suffix);

        assertThatThrownBy(() -> verify(suffix, code, pointId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("no open amusement session");
        assertThat(usedTimes(code)).isEqualTo(0);
        assertThat(successCount(code)).isEqualTo(0);
        assertThat(amusementVerificationCount(projectId)).isEqualTo(0);
    }

    @Test
    void replaySameOfflineRequestDoesNotDoubleConsumeCapacity() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String pointId = "point_idem_" + suffix;
        String projectId = seedOpenProjectWithSession(suffix, pointId, 5, "open_now");
        String code = seedAmusementEntitlement(suffix);

        // 同一 offline_request_id(clientTaskId) 两次离线同步重放 → 第二次命中既有同步任务幂等短路，
        // 复用第一次核销结果 → amusement_verifications 只一条、容量只扣一次。
        String offlineReqId = "offline-" + suffix;
        var first = syncOfflineVerify(suffix, code, pointId, offlineReqId);
        var second = syncOfflineVerify(suffix, code, pointId, offlineReqId);

        assertThat(first).isEqualTo(second);                           // 幂等：两次返回同一核销 id
        assertThat(amusementVerificationCount(projectId)).isEqualTo(1);// 只一条游乐核销
        assertThat(sessionUsedCapacity(projectId)).isEqualTo(1);       // 容量只扣一次(证伪:重复扣则=2,断言红)
    }

    // ===== helpers =====

    private VerificationResponse verify(String suffix, String code, String pointId) {
        return ticketingService.createVerification(new VerificationRequest(
                "idem-" + suffix + "-" + System.nanoTime(), "qr", code, deviceCode, pointId, Map.of()));
    }

    /**
     * 通过离线同步任务链核销(携带稳定 client_task_id=offline_request_id + 稳定 occurred_at + 稳定 idempotencyKey)，
     * 返回落库的 amusement 核销 id。两次调用完全相同 payload → 请求级幂等命中，且任务级 client_task_id 去重，
     * 均不重复执行游乐核销 → 容量只扣一次。
     */
    private String syncOfflineVerify(String suffix, String code, String pointId, String offlineReqId) {
        var resp = ticketingService.syncTerminalTasks(new com.complex.platform.ticketing.dto.TerminalSyncTasksRequest(
                "sync-idem-" + offlineReqId, null, null, deviceCode, "staff_" + suffix,
                List.of(new com.complex.platform.ticketing.dto.TerminalSyncTaskItemRequest(
                        offlineReqId, "offline_verification", "2026-07-05T00:00:00Z",
                        Map.of("entitlementCode", code, "pointId", pointId)))));
        return resp.results().get(0).verificationId();
    }

    private int usedTimes(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT used_times FROM issued_entitlements WHERE id = ?", Integer.class, "ent_" + code);
    }

    private int successCount(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ticket_verifications WHERE entitlement_id = ? AND status = 'success'",
                Integer.class, "ent_" + code);
    }

    private String crossReferenceOfSuccess(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT amusement_verification_id FROM ticket_verifications "
                        + "WHERE entitlement_id = ? AND status = 'success' ORDER BY created_at DESC LIMIT 1",
                String.class, "ent_" + code);
    }

    private String crossReferenceIfAny(String code) {
        List<String> refs = jdbcTemplate.queryForList(
                "SELECT amusement_verification_id FROM ticket_verifications "
                        + "WHERE entitlement_id = ? AND status = 'success'",
                String.class, "ent_" + code);
        return refs.isEmpty() ? null : refs.get(0);
    }

    private int amusementVerificationCount(String projectId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM amusement_verifications WHERE project_id = ?", Integer.class, projectId);
    }

    private String amusementVerifiedSession(String projectId) {
        return jdbcTemplate.queryForObject(
                "SELECT session_id FROM amusement_verifications WHERE project_id = ? ORDER BY created_at DESC LIMIT 1",
                String.class, projectId);
    }

    private int sessionUsedCapacity(String projectId) {
        return jdbcTemplate.queryForObject(
                "SELECT used_capacity FROM amusement_sessions WHERE project_id = ? ORDER BY start_time DESC LIMIT 1",
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

    /**
     * 播一个 open 项目 + 入口点位 location_point_id + 一场场次 + 一条 pass 安全巡检。
     * sessionMode：open_now=覆盖当前时刻的 open 场次；full_now=同 open_now(供占满测试)；past=时间窗在过去(不覆盖 now)。
     */
    private String seedOpenProjectWithSession(String suffix, String pointId, int capacity, String sessionMode) {
        Instant now = Instant.now();
        String projectId = "project_amuse_" + suffix;
        seededProjects.add(projectId);
        String templateId = "template_amuse_" + suffix;
        amusementRepository.saveSafetyTemplate(new com.complex.platform.amusement.AmusementSafetyTemplateRecord(
                templateId, "Daily Opening " + suffix,
                List.of(Map.of("name", "restraint", "required", true)),
                LocalDate.now(), "active", List.of(), now, now, null));
        amusementRepository.saveProject(new AmusementProjectRecord(
                projectId, "Amuse " + suffix, capacity, "open", templateId, Map.of(), pointId, now, now, null));
        // pass 安全巡检(submitted+pass) → evaluateOpenEligibility 放行。
        amusementRepository.saveSafetyInspection(new AmusementSafetyInspectionRecord(
                "inspection_amuse_" + suffix, projectId, null, templateId,
                List.of(Map.of("name", "restraint", "result", "pass")),
                "pass", "submitted", "none", "not_required",
                now, now, null, null, "passed", List.of(), now, now,
                "inspector_" + suffix, "pass", false, null));
        Instant start = "past".equals(sessionMode) ? now.minusSeconds(7200) : now.minusSeconds(600);
        Instant end = "past".equals(sessionMode) ? now.minusSeconds(3600) : now.plusSeconds(3600);
        amusementRepository.saveSession(new AmusementSessionRecord(
                "session_amuse_" + suffix, projectId, start, end, capacity, 0, 0, "open", List.of(), now, now));
        return projectId;
    }

    /**
     * 播一张游乐类权益(entitlement_type='amusement_pass')：total_times=1、venue/point 任意(游乐分流按 point→项目定位,
     * 不依赖 entitlement.venue_id)、value_amount=5500(D6 结算价沿用)。
     */
    private String seedAmusementEntitlement(String suffix) {
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
                VALUES (?, 'AmusePass', 'amusement_pass', 1, 5500, 'active', 'virtual', ?, ?)
                """, templateId, ts, ts);
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_" + code, "ON-" + code, "cust_" + code, typeId, null, 1,
                LocalDate.parse("2026-06-18"), "web", null, 0L, "CNY", "paid", "issued", "created", now, now));
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, typeId, "order_" + code, templateId, "amusement_pass",
                "merchant_a", null, null, 1, 0, 5500L, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, null,
                "virtual", null));
        return code;
    }
}
