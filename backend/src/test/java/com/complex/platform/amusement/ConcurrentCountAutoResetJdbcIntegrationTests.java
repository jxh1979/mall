package com.complex.platform.amusement;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.support.TestSecurityActors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * CG-03 在玩人数的系统自动清零（BSM「出场自动扣减」：「按场清场的场次型项目，散场时由系统按场次结束自动清空；非场次型项目，
 * 营业日结束时由系统自动清零在玩人数」；BSM「在玩人数不允许人工调整」：「计数只能由入场核销、出场自动扣减以及系统按场次结束的
 * 自动清空、按营业日结束的自动清零产生」）。
 *
 * <p>真实 Flyway 迁移库，{@code @Transactional} 回滚；调度作业直调 {@code AmusementConcurrentCountResetJob.runAt}，
 * 追 {@code amusement_capacity_snapshot.occupied_count}、{@code amusement_sessions.status}、清零留痕与系统审计事件。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class ConcurrentCountAutoResetJdbcIntegrationTests {

    @Autowired private AmusementService service;
    @Autowired private BusinessDayResolver businessDayResolver;
    @Autowired private JdbcTemplate jdbcTemplate;

    private AmusementConcurrentCountResetJob job;
    private String suffix;
    private String freeFlowProjectId;
    private String sessionProjectId;
    private String sessionId;
    private Instant now;
    private Instant dayEnd;

    @BeforeEach
    void seed() {
        TestSecurityActors.clear();
        job = new AmusementConcurrentCountResetJob(service, java.time.Clock.systemUTC());
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        now = Instant.now();
        LocalDate currentBusinessDay = businessDayResolver.businessDate("amusement", now);
        dayEnd = businessDayResolver.endInstant("amusement", currentBusinessDay);
        freeFlowProjectId = project("free");
        sessionProjectId = project("sess");
        // 非场次型：项目维 3 人、点位设备维 2 人，均为本营业日内的计数（updated_at = now）。
        snapshot(freeFlowProjectId, "project", null, null, null, 3, now);
        snapshot(freeFlowProjectId, "point_device", null, "pt_" + suffix, "dev_" + suffix, 2, now);
        // 场次型：一场 end_time 已过仍 open 的场次，场次维 4 人、项目维 4 人。
        sessionId = "ses_cg03_" + suffix;
        jdbcTemplate.update("INSERT INTO amusement_sessions (id, project_id, start_time, end_time, capacity, used_capacity, status,"
                + " created_at, updated_at) VALUES (?, ?, ?, ?, 50, 4, 'open', ?, ?)", sessionId, sessionProjectId,
                Timestamp.from(now.minus(3, ChronoUnit.HOURS)), Timestamp.from(now.minus(1, ChronoUnit.HOURS)),
                Timestamp.from(now), Timestamp.from(now));
        snapshot(sessionProjectId, "session", sessionId, null, null, 4, now.minus(2, ChronoUnit.HOURS));
        snapshot(sessionProjectId, "project", null, null, null, 4, now.minus(2, ChronoUnit.HOURS));
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    /** 营业日结束前不清；结束后清零并留痕与审计；重复跑不重复记。 */
    @Test
    void businessDayEndResetsFreeFlowCountsOnceWithAudit() {
        // 营业日结束前（当前时刻）：本营业日的计数不动。
        job.runAt(now);
        assertThat(occupied(freeFlowProjectId, "project")).isEqualTo(3);
        assertThat(occupied(freeFlowProjectId, "point_device")).isEqualTo(2);
        assertThat(resetLogs(freeFlowProjectId, "business_day_end", 0)).isZero();

        // 营业日结束后：清零、留痕、系统审计。
        Instant afterEnd = dayEnd.plus(1, ChronoUnit.MINUTES);
        int[] first = job.runAt(afterEnd);
        assertThat(first[1]).isGreaterThanOrEqualTo(1);
        assertThat(occupied(freeFlowProjectId, "project")).isZero();
        assertThat(occupied(freeFlowProjectId, "point_device")).isZero();
        java.sql.Date endedDay = java.sql.Date.valueOf(businessDayResolver.businessDate("amusement", now));
        List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                "SELECT scope, business_date, cleared_rows, cleared_people, boundary_at FROM amusement_concurrent_reset_log"
                        + " WHERE project_id = ? AND scope = 'business_day_end' AND business_date = ?", freeFlowProjectId, endedDay);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).get("cleared_rows")).isEqualTo(2);
        assertThat(logs.get(0).get("cleared_people")).isEqualTo(5);
        assertThat(((Timestamp) logs.get(0).get("boundary_at")).toInstant()).isEqualTo(dayEnd);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name = ? AND business_id = ?",
                Integer.class, "amusement.concurrent_count.business_day_reset", freeFlowProjectId)).isEqualTo(1);

        // 重复跑：不重复留痕、不重复审计。
        job.runAt(afterEnd.plus(1, ChronoUnit.MINUTES));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM amusement_concurrent_reset_log WHERE project_id = ?"
                + " AND scope = 'business_day_end' AND business_date = ?", Integer.class, freeFlowProjectId, endedDay)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name = ? AND business_id = ?",
                Integer.class, "amusement.concurrent_count.business_day_reset", freeFlowProjectId)).isEqualTo(1);
    }

    /** 场次结束：系统置 closed 并清空该场次与该项目的在玩人数，留痕与审计；重复跑不重复。 */
    @Test
    void sessionEndClosesSessionAndClearsCountsOnce() {
        int[] result = job.runAt(now);
        assertThat(result[0]).isEqualTo(1);
        assertThat(jdbcTemplate.queryForMap("SELECT status FROM amusement_sessions WHERE id = ?", sessionId))
                .containsEntry("status", "closed");
        assertThat(occupiedForSession(sessionId)).isZero();
        assertThat(occupied(sessionProjectId, "project")).isZero();
        assertThat(jdbcTemplate.queryForList("SELECT cleared_rows, cleared_people FROM amusement_concurrent_reset_log"
                + " WHERE session_id = ? AND scope = 'session_end'", sessionId))
                .hasSize(1).first().satisfies(r -> {
                    assertThat(r.get("cleared_rows")).isEqualTo(2);
                    assertThat(r.get("cleared_people")).isEqualTo(8);
                });
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name = ? AND business_id = ?",
                Integer.class, "amusement.concurrent_count.session_end_cleared", sessionId)).isEqualTo(1);
        // 非场次型项目不受场次结束影响（营业日未结束）。
        assertThat(occupied(freeFlowProjectId, "project")).isEqualTo(3);

        int[] again = job.runAt(now.plus(1, ChronoUnit.MINUTES));
        assertThat(again[0]).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM amusement_concurrent_reset_log WHERE session_id = ?",
                Integer.class, sessionId)).isEqualTo(1);
    }

    /** 契约里不存在任何人工修改/清零在玩人数的端点（计数只能由核销、出场扣减与系统清零产生）。 */
    @Test
    void contractHasNoManualConcurrentCountEndpoint() throws Exception {
        Path openapi = Path.of("..", "docs", "openapi", "openapi-p0.json");
        if (!Files.exists(openapi)) {
            openapi = Path.of("docs", "openapi", "openapi-p0.json");
        }
        assertThat(Files.exists(openapi)).as("OpenAPI 契约文件存在").isTrue();
        String contract = Files.readString(openapi);
        assertThat(contract).doesNotContain("concurrent-count/clear").doesNotContain("concurrent-capacity/clear")
                .doesNotContain("clearProjectConcurrentCapacity").doesNotContain("resetConcurrentCount")
                .doesNotContain("concurrent-reset");
    }

    // ---------- helpers ----------

    private String project(String key) {
        String id = "amp_cg03_" + key + "_" + suffix;
        jdbcTemplate.update("INSERT INTO amusement_projects (id, project_name, capacity, open_status, created_at, updated_at)"
                + " VALUES (?, ?, 100, 'open', ?, ?)", id, "自动清零测试项目-" + key, Timestamp.from(now), Timestamp.from(now));
        return id;
    }

    private void snapshot(String projectId, String dimension, String sessionId, String pointId, String deviceId, int occupied,
            Instant updatedAt) {
        jdbcTemplate.update("INSERT INTO amusement_capacity_snapshot (id, project_id, session_id, location_point_id, device_id,"
                + " dimension, capacity_limit, occupied_count, updated_at) VALUES (?, ?, ?, ?, ?, ?, 100, ?, ?)",
                "capsnap_cg03_" + dimension + "_" + UUID.randomUUID(), projectId, sessionId, pointId, deviceId, dimension, occupied,
                Timestamp.from(updatedAt));
    }

    private int occupied(String projectId, String dimension) {
        Integer n = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(occupied_count), 0) FROM amusement_capacity_snapshot"
                + " WHERE project_id = ? AND dimension = ?", Integer.class, projectId, dimension);
        return n == null ? 0 : n;
    }

    private int occupiedForSession(String sessionId) {
        Integer n = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(occupied_count), 0) FROM amusement_capacity_snapshot"
                + " WHERE session_id = ? AND dimension = 'session'", Integer.class, sessionId);
        return n == null ? 0 : n;
    }

    private int resetLogs(String projectId, String scope, int ignored) {
        Integer n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM amusement_concurrent_reset_log WHERE project_id = ?"
                + " AND scope = ? AND cleared_rows > 0", Integer.class, projectId, scope);
        return n == null ? 0 : n;
    }
}
