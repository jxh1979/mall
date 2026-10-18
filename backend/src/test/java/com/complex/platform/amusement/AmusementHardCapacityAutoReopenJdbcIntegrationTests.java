package com.complex.platform.amusement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.amusement.dto.AmusementIncidentCreateRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * CG-02 硬安全容量自动停运后自动恢复开放（BSM「容量分两道」：「因达到硬安全容量而自动停运的项目，在玩人数回落到硬安全容量以下后
 * 由系统自动恢复开放」；BSM「容量控制」/ ENUM「容量双阈值」同句）。
 *
 * <p>走真实 Flyway 迁移库（V443 amusement_projects.auto_paused_reason）。本类【非事务】：并发两路出场用例要让两条
 * exitVerification 各自真提交；每用例唯一后缀隔离，末尾按后缀清理（amusement_* 随项目级联删除；audit_events 只增不删）。
 * 入场/出场都走正路（createVerification / exitVerification，真实权益 + 容量快照），逐步追 amusement_projects.open_status、
 * auto_paused_reason 与 amusement_capacity_snapshot.occupied_count 列。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class AmusementHardCapacityAutoReopenJdbcIntegrationTests {

    @Autowired private AmusementService amusementService;
    @Autowired private AmusementSafetyService safetyService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String suffix;
    private String projectId;
    private String sessionId;
    private String templateId;
    private String userId;
    private String roleId;

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
        if (suffix == null) {
            return;
        }
        // amusement_sessions / verifications / capacity_snapshot / inspections / incidents 均 ON DELETE CASCADE 到项目；
        // risk_alerts/risk_alert_actions 为 append-only（V140），随机 id 留痕不清。
        jdbcTemplate.update("DELETE FROM amusement_projects WHERE id = ?", projectId);
        jdbcTemplate.update("DELETE FROM amusement_safety_templates WHERE id = ?", templateId);
        jdbcTemplate.update("DELETE FROM issued_entitlements WHERE order_id = ?", "order_cg02_" + suffix);
        jdbcTemplate.update("DELETE FROM ticket_orders WHERE id = ?", "order_cg02_" + suffix);
        jdbcTemplate.update("DELETE FROM entitlement_templates WHERE id = ?", "et_cg02_" + suffix);
        jdbcTemplate.update("DELETE FROM ticket_types WHERE id = ?", "tt_cg02_" + suffix);
        jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", "%" + suffix + "%");
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id = ?", roleId);
        jdbcTemplate.update("DELETE FROM roles WHERE id = ?", roleId);
        jdbcTemplate.update("DELETE FROM access_users WHERE id = ?", userId);
    }

    /** 入场到硬上限 → paused（原因 hard_capacity）；一人出场后在玩人数 < 硬上限 → 系统自动恢复 open 并留痕。 */
    @Test
    void hardCapacityPauseIsLiftedBySystemWhenInPlayDropsBelowLimit() {
        seed(1, 2);
        enter(1);
        assertThatThrownBy(() -> enter(2))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("SESSION_CAPACITY_EXHAUSTED"));
        assertThat(projectRow()).containsEntry("open_status", "paused")
                .containsEntry("auto_paused_reason", "hard_capacity");
        assertThat(inPlayCount()).isEqualTo(1);

        exit(1);

        assertThat(inPlayCount()).isEqualTo(0);
        assertThat(projectRow()).containsEntry("open_status", "open")
                .containsEntry("auto_paused_reason", null);
        assertThat(autoReopenEvents()).isEqualTo(1);
    }

    /** 高危事故停运（safety_incident）不被出场翻回：事故停运不是硬超容。 */
    @Test
    void safetyIncidentPauseIsNotLiftedByExit() {
        seed(5, 1);
        enter(1);
        safetyService.createIncident(new AmusementIncidentCreateRequest(
                "inc_" + suffix, null, List.of(), projectId, projectId, sessionId, null, null,
                "high", "高危安全事故", "测试：高危事故自动停运", "usr_reporter", "staff", Instant.now()));
        assertThat(projectRow()).containsEntry("open_status", "paused")
                .containsEntry("auto_paused_reason", "safety_incident");

        exit(1);

        assertThat(inPlayCount()).isEqualTo(0);
        assertThat(projectRow()).containsEntry("open_status", "paused")
                .containsEntry("auto_paused_reason", "safety_incident");
        assertThat(autoReopenEvents()).isEqualTo(0);
    }

    /** 硬超容停运期间发生高危事故 → 原因升级为 safety_incident，人数回落后也不自动恢复。 */
    @Test
    void safetyIncidentDuringHardCapacityPauseBlocksAutoReopen() {
        seed(1, 2);
        enter(1);
        assertThatThrownBy(() -> enter(2)).isInstanceOf(ApiException.class);
        assertThat(projectRow()).containsEntry("auto_paused_reason", "hard_capacity");
        safetyService.createIncident(new AmusementIncidentCreateRequest(
                "inc_" + suffix, null, List.of(), projectId, projectId, sessionId, null, null,
                "critical", "停运期间严重事故", "测试：硬超容停运期间发生事故", "usr_reporter", "staff", Instant.now()));
        assertThat(projectRow()).containsEntry("open_status", "paused")
                .containsEntry("auto_paused_reason", "safety_incident");

        exit(1);

        assertThat(projectRow()).containsEntry("open_status", "paused")
                .containsEntry("auto_paused_reason", "safety_incident");
        assertThat(autoReopenEvents()).isEqualTo(0);
    }

    /** 人工 maintenance / closed / 人工 paused（无系统原因）均不被出场翻回。 */
    @Test
    void manualStatesAreNotLiftedByExit() {
        for (String manual : List.of("maintenance", "closed", "paused")) {
            seed(5, 1);
            enter(1);
            jdbcTemplate.update("UPDATE amusement_projects SET open_status = ?, auto_paused_reason = NULL WHERE id = ?",
                    manual, projectId);

            exit(1);

            assertThat(inPlayCount()).isEqualTo(0);
            assertThat(projectRow()).containsEntry("open_status", manual)
                    .containsEntry("auto_paused_reason", null);
            assertThat(autoReopenEvents()).isEqualTo(0);
            tearDown();
        }
    }

    /** 并发两路出场：两路都真实释放，但项目只翻一次 open、只留一条自动恢复痕。 */
    @Test
    void concurrentExitsReopenExactlyOnce() throws Exception {
        seed(2, 3);
        enter(1);
        enter(2);
        assertThatThrownBy(() -> enter(3)).isInstanceOf(ApiException.class);
        assertThat(projectRow()).containsEntry("open_status", "paused")
                .containsEntry("auto_paused_reason", "hard_capacity");
        assertThat(inPlayCount()).isEqualTo(2);

        String actorUser = userId;
        runBoth(() -> { authenticate(actorUser); exit(1); return null; },
                () -> { authenticate(actorUser); exit(2); return null; });

        assertThat(inPlayCount()).isEqualTo(0);
        assertThat(projectRow()).containsEntry("open_status", "open")
                .containsEntry("auto_paused_reason", null);
        assertThat(autoReopenEvents()).isEqualTo(1);
    }

    // ---- 正路驱动 ----

    private void enter(int n) {
        amusementService.createVerification(new VerificationRequest(
                "in_" + n + "_" + suffix, "entitlement_code", code(n), null, null,
                Map.of("project_id", projectId, "session_id", sessionId, "staff_id", "usr_gate")));
    }

    private void exit(int n) {
        amusementService.exitVerification(new AmusementExitRequest(
                "out_" + n + "_" + suffix, code(n), null, null, "usr_gate", List.of()));
    }

    private String code(int n) {
        return "code_cg02_" + suffix + "_" + n;
    }

    private Map<String, Object> projectRow() {
        return jdbcTemplate.queryForMap(
                "SELECT open_status, auto_paused_reason FROM amusement_projects WHERE id = ?", projectId);
    }

    private int inPlayCount() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT occupied_count FROM amusement_capacity_snapshot
                 WHERE project_id = ? AND dimension = 'project'
                   AND COALESCE(session_id, '') = '' AND COALESCE(location_point_id, '') = ''
                   AND COALESCE(device_id, '') = ''
                """, Integer.class, projectId);
        return count == null ? -1 : count;
    }

    private int autoReopenEvents() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_name = 'amusement.project.auto_reopened' AND business_id = ?",
                Integer.class, projectId);
        return count == null ? 0 : count;
    }

    private void runBoth(Callable<?> a, Callable<?> b) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> fa = pool.submit(() -> { barrier.await(); return a.call(); });
            Future<?> fb = pool.submit(() -> { barrier.await(); return b.call(); });
            fa.get();
            fb.get();
        } finally {
            pool.shutdownNow();
        }
    }

    // ---- 种子 ----

    /**
     * @param safetyCapacity 项目硬安全容量（amusement_projects.capacity，项目维快照上限）
     * @param entitlements   预发的游乐权益数（每张 1 人、1 次）
     */
    private void seed(int safetyCapacity, int entitlements) {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        projectId = "amu_cg02_" + suffix;
        sessionId = "sess_cg02_" + suffix;
        templateId = "tpl_cg02_" + suffix;
        jdbcTemplate.update("INSERT INTO amusement_safety_templates (id, template_name, items_json, status, created_at, updated_at) "
                + "VALUES (?, '硬超容测试模板', '[{\"name\":\"restraint\"}]'::jsonb, 'active', now(), now())", templateId);
        jdbcTemplate.update("INSERT INTO amusement_projects (id, project_name, capacity, open_status, safety_template_id, "
                + "eligibility_rule_json, created_at, updated_at) VALUES (?, '硬超容测试项目', ?, 'open', ?, '{}'::jsonb, now(), now())",
                projectId, safetyCapacity, templateId);
        // 准入闸：最近一次巡检 pass + submitted 才放行核销（evaluateOpenEligibility）。
        jdbcTemplate.update("INSERT INTO amusement_safety_inspections (id, project_id, template_id, items_json, overall_result, "
                + "inspection_status, rectification_status, reopen_status, inspected_at, submitted_at, created_at, updated_at) "
                + "VALUES (?, ?, ?, '[{\"name\":\"restraint\",\"result\":\"pass\"}]'::jsonb, 'pass', 'submitted', 'not_required', "
                + "'not_required', now(), now(), now(), now())", "insp_cg02_" + suffix, projectId, templateId);
        // 场次容量放大，确保耗尽的是项目硬安全容量维（project_safety），不是场次维。
        jdbcTemplate.update("INSERT INTO amusement_sessions (id, project_id, start_time, end_time, capacity, used_capacity, status, "
                + "created_at, updated_at) VALUES (?, ?, now() - interval '1 hour', now() + interval '1 hour', 100, 0, 'open', now(), now())",
                sessionId, projectId);
        // 真实票务权益（游乐类 amusement_*，1 人 1 次），经 TicketingAmusementEntitlementGateway 按权益码识别。
        jdbcTemplate.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at) "
                + "VALUES (?, '硬超容测试票', 'fixed', 'active', now(), now())", "tt_cg02_" + suffix);
        jdbcTemplate.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, "
                + "status, created_at, updated_at) VALUES (?, '硬超容测试权益', 'amusement_pass', 1, 0, 'active', now(), now())",
                "et_cg02_" + suffix);
        jdbcTemplate.update("INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, pay_status, "
                + "issue_status, status, created_at, updated_at) VALUES (?, ?, ?, ?, 'web', 0, 'paid', 'issued', 'created', now(), now())",
                "order_cg02_" + suffix, "ONO_CG02_" + suffix, "tt_cg02_" + suffix, entitlements);
        for (int i = 1; i <= entitlements; i++) {
            jdbcTemplate.update("INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id, entitlement_code, "
                    + "entitlement_type, total_times, used_times, status, valid_from, valid_to, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, 'amusement_pass', 1, 0, 'active', now() - interval '1 day', now() + interval '1 day', now(), now())",
                    "ie_cg02_" + suffix + "_" + i, "tt_cg02_" + suffix, "order_cg02_" + suffix, "et_cg02_" + suffix, code(i));
        }
        // 核销/出场/事故上报都走 requirePermission：amusement.verification.execute 为高危码，须给操作人显式角色来源。
        userId = "usr_cg02_" + suffix;
        roleId = "role_cg02_" + suffix;
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) "
                + "VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", userId, "u_cg02_" + suffix);
        // 角色名全中文+数字：治理页上屏名称不得含拉丁字母（AccessJdbcIntegrationTests.seededDisplayNamesAreChinese 全库核查）。
        jdbcTemplate.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId,
                "硬超容测试角色" + System.nanoTime());
        for (String code : List.of("amusement.verification.execute", "amusement.incident.create")) {
            jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        authenticate(userId);
    }

    private void authenticate(String user) {
        AccessActor actor = new AccessActor(user, "u_cg02_" + suffix, "platform_admin", "org_1", null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }
}
