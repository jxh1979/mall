package com.complex.platform.ticketing;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.amusement.AmusementExitRequest;
import com.complex.platform.amusement.AmusementBusinessDayService;
import com.complex.platform.amusement.AmusementBusinessDayTestFixture;
import com.complex.platform.amusement.AmusementSessionLifecycleService;
import com.complex.platform.governance.review.SupervisorReviewService;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 真实票务入场到游乐出场的数据库回归：审批营业日、场次锁版、真实父票及操作人范围。
 * 验证单项目人数释放、重复出场、两笔不同请求并发入场和无入场拒绝。
 * 已退休场次容量不再占用；该测试只覆盖既有出场调用，不代表出口自动感应链已交付。
 * 第十八批 AH 段第 219 条补充：同一权益两笔不同请求并发入场各按一次使用扣次（BSM:780「每用一次扣一次」），不再靠 V234 唯一约束拒第二笔。
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

    @Autowired private AmusementBusinessDayService businessDays;
    @Autowired private AmusementSessionLifecycleService lifecycle;
    @Autowired private SupervisorReviewService reviews;
    @Autowired private PlatformTransactionManager transactionManager;

    private final List<String> seededCodes = new ArrayList<>();
    private final List<String> seededProjects = new ArrayList<>();
    private String deviceCode;
    private String complexId;
    private String actorId;

    @AfterEach
    void cleanup() {
        com.complex.platform.support.TestSecurityActors.clear();
        for (String project : seededProjects) {
            jdbcTemplate.update("DELETE FROM amusement_verifications WHERE project_id = ?", project);
            jdbcTemplate.update("DELETE FROM amusement_capacity_snapshot WHERE project_id = ?", project);
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
            jdbcTemplate.update("DELETE FROM ticket_instances WHERE order_id = ?", "order_" + code);
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
        // 第二张真实权益经同一入口入场，确认重复出场不误减其他客人。
        String otherCode = seedAmusementEntitlement(suffix + "other", 1);
        verify(suffix + "other", otherCode, pointId);
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
    void concurrentDoubleEntrySameEntitlementSerializesAndConsumesTwice() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String pointId = "point_dbl_" + suffix;
        String projectId = seedOpenProjectWithSession(suffix, pointId, 5);
        String code = seedAmusementEntitlement(suffix, 2);
        CountDownLatch firstStored = new CountDownLatch(1), commitFirst = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        AtomicInteger secondPid = new AtomicInteger();
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> {
                AmusementBusinessDayTestFixture.authenticate(actorId, complexId);
                try {
                    return new TransactionTemplate(transactionManager).execute(status -> {
                        var result = verify(suffix + "first", code, pointId);
                        firstStored.countDown();
                        try {
                            if (!commitFirst.await(10, TimeUnit.SECONDS)) throw new AssertionError("首笔核销提交未获释放");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new AssertionError(e);
                        }
                        return result;
                    });
                } finally { com.complex.platform.support.TestSecurityActors.clear(); }
            });
            assertThat(firstStored.await(10, TimeUnit.SECONDS)).isTrue();
            var second = pool.submit(() -> {
                AmusementBusinessDayTestFixture.authenticate(actorId, complexId);
                try {
                    return new TransactionTemplate(transactionManager).execute(status -> {
                        secondPid.set(jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Integer.class));
                        secondStarted.countDown();
                        return verify(suffix + "second", code, pointId);
                    });
                } finally { com.complex.platform.support.TestSecurityActors.clear(); }
            });
            assertThat(secondStarted.await(10, TimeUnit.SECONDS)).isTrue();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            boolean waiting = false;
            while (!waiting && System.nanoTime() < deadline) {
                waiting = Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                        SELECT EXISTS(SELECT 1 FROM pg_stat_activity
                         WHERE pid=? AND cardinality(pg_blocking_pids(pid))>0
                           AND query ILIKE '%amusement_projects%')
                        """, Boolean.class, secondPid.get()));
                if (!waiting) Thread.sleep(10);
            }
            assertThat(waiting).as("第二笔真实核销须等待同项目锁").isTrue();
            commitFirst.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS).status()).isEqualTo("success");
            assertThatCode(() -> second.get(10, TimeUnit.SECONDS)).doesNotThrowAnyException();
        } finally {
            commitFirst.countDown();
            pool.shutdownNow();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(activeEntryCount(projectId)).isEqualTo(2);
        assertThat(projectSnapshotOccupied(projectId)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT used_times FROM issued_entitlements WHERE id=?", Integer.class, "ent_" + code)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ticket_verifications WHERE entitlement_id=?", Integer.class, "ent_" + code)).isEqualTo(2);
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

    /** 入场和出场只调整项目在场人数，不恢复已经退休的场次容量口径。 */
    @Test
    void entryAndExitDoNotUseRetiredSessionCapacity() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String pointId = "point_sess_" + suffix;
        String projectId = seedOpenProjectWithSession(suffix, pointId, 5);
        String code = seedAmusementEntitlement(suffix, 2);

        // 入场增加真实项目在场人数，场次旧计数字段保持未占用。
        var entry = verify(suffix, code, pointId);
        assertThat(entry.status()).isEqualTo("success");
        assertThat(projectSnapshotOccupied(projectId)).isEqualTo(1);
        assertThat(sessionUsedCapacity(projectId)).isZero();

        // 出场仅释放原入场事实对应的项目人数。
        amusementService.exitVerification(new AmusementExitRequest(
                "exit-sess-" + suffix, code, pointId, deviceCode, "staff", List.of()));
        assertThat(projectSnapshotOccupied(projectId)).isEqualTo(0);   // 并发在场回收
        assertThat(sessionUsedCapacity(projectId)).isZero();
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

    /** 仅核对已退休场次容量字段未被入出场重新写入。 */
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
        complexId = AmusementBusinessDayTestFixture.complex(jdbcTemplate);
        AmusementBusinessDayTestFixture.bind(jdbcTemplate, projectId, complexId);
        AmusementBusinessDayTestFixture.approve(jdbcTemplate, businessDays, reviews, complexId,
                "Asia/Shanghai", 4, now.minusSeconds(3 * 86400));
        amusementRepository.saveSession(new AmusementSessionRecord(
                "session_amuse_" + suffix, projectId, now.minusSeconds(600), now.plusSeconds(3600),
                0, 0, 0, "open", List.of(), now, now));
        lifecycle.lockSessionWindow(amusementRepository.findSessionById("session_amuse_" + suffix).orElseThrow());
        actorId = AmusementBusinessDayTestFixture.actor(jdbcTemplate, complexId,
                List.of("verification.execute", "amusement.verification.execute"));
        AmusementBusinessDayTestFixture.authenticate(actorId, complexId);
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
                LocalDate.parse("2026-06-18"), "web", null, 0L, null, 0L, 0L, "CNY", "paid", "issued", "created", now, now, null, null, null));
        jdbcTemplate.update("UPDATE ticket_orders SET project_id=? WHERE id=?", complexId, "order_" + code);
        jdbcTemplate.update("""
                INSERT INTO ticket_instances(id,ticket_no,order_id,ticket_type_id,seq_in_order,
                    num_people,total_times,status,created_at,updated_at)
                VALUES(?,?,?,?,1,1,?,'active',?,?)
                """, "parent_" + code, "TN-" + code, "order_" + code, typeId, totalTimes, ts, ts);
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, typeId, "order_" + code, templateId, "amusement_pass",
                "merchant_a", null, null, totalTimes, 0, 5500L, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, null,
                "virtual", null, null, null, null, 0, null, null));
        jdbcTemplate.update("UPDATE issued_entitlements SET parent_ticket_id=?,slot_index=1 WHERE id=?",
                "parent_" + code, "ent_" + code);
        return code;
    }
}
