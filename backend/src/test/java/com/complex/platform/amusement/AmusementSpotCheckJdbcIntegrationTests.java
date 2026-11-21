package com.complex.platform.amusement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.LocalZone;
import com.complex.platform.support.StaffRequestFixture;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BS 段第 373 条 373-R2（BSM:1300「营业中抽检有最低频次要求，每隔多久至少抽检一次由后台配置（安全阈值，按项目可配，须同时配置上限与下限，
 * 出厂默认每 2 小时一次），达不到最低频次的视为当班未抽检」；DEC-CFG-2026-08-03-01「安全阈值可配但须最高权限审批并留痕」；BSM:576；
 * BSM:1682「营业日从当日上午 10:00 起算」；PGM:528「④看不到别的商户和别的项目的数据」）：抽检间隔按综合体项目、最高权限本人调整；
 * 当班未抽检按每笔成功入场之前最近一次已提交巡检判。真库＋真实 HTTP＋真实授权链、蛇形键；入场行直接插 amusement_verifications（带核销时锁定的
 * 综合体与营业日起点），时刻都落在昨天、营业日起点取昨天 10:00，判定时刻取昨天 23:00。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AmusementSpotCheckJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AmusementSpotCheckService spotChecks;

    private final String digits = String.valueOf(System.nanoTime());
    private final String complexA = "prj_a373a_" + digits;
    private final String complexB = "prj_a373b_" + digits;
    private final String merchantA = "mch_a373a_" + digits;
    private final String merchantB = "mch_a373b_" + digits;
    private final String merchantA2 = "mch_a373a2_" + digits;
    private final String amusementA = "amu_a373a_" + digits;
    private final String amusementB = "amu_a373b_" + digits;
    private final String amusementA2 = "amu_a373a2_" + digits;
    private final LocalDate yesterday = LocalDate.now(LocalZone.ID).minusDays(1);
    private final Instant dayStart = at(10, 0);
    private final Instant t0 = at(12, 0);
    private final Instant judgedAt = at(23, 0);
    private RequestPostProcessor manager;
    private RequestPostProcessor merchantStaffA;
    private RequestPostProcessor companyStaffB;
    private int rows;

    @BeforeEach
    void seed() {
        for (String complex : List.of(complexA, complexB)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex',?,'active',now(),now())",
                    complex, "抽检综合体" + complex);
        }
        Map.of(merchantA, complexA, merchantA2, complexA, merchantB, complexB).forEach((merchant, complex) -> jdbc.update(
                "INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at)"
                        + " VALUES(?,'抽检游乐商户','amusement',?,'active',now(),now())", merchant, complex));
        Map.of(amusementA, merchantA, amusementA2, merchantA2, amusementB, merchantB).forEach((amusement, merchant) -> {
            jdbc.update("INSERT INTO amusement_projects (id, project_name, capacity, open_status, eligibility_rule_json, merchant_id,"
                    + " created_at, updated_at) VALUES (?, '旋转木马', 10, 'open', '{}'::jsonb, ?, now(), now())", amusement, merchant);
            jdbc.update("INSERT INTO amusement_sessions (id, project_id, start_time, end_time, capacity, status, created_at, updated_at)"
                    + " VALUES (?, ?, ?, ?, 100, 'open', now(), now())", "ses_" + amusement, amusement,
                    Timestamp.from(at(9, 0)), Timestamp.from(judgedAt));
        });
        StaffRequestFixture fixture = new StaffRequestFixture(jdbc);
        manager = fixture.staff("usr_a373g_" + digits, "staff", complexA, null,
                "amusement.safety_template.manage", "amusement.safety_inspection.view");
        merchantStaffA = fixture.staff("usr_a373ma_" + digits, "staff", complexA, merchantA, "amusement.safety_inspection.view");
        companyStaffB = fixture.staff("usr_a373cb_" + digits, "staff", complexB, null, "amusement.safety_inspection.view");
    }

    // (1) 没配置的综合体读出厂行：每 120 分钟。
    @Test
    void unconfiguredComplexReadsFactoryInterval() throws Exception {
        mockMvc.perform(get("/api/v1/amusement/spot-check-policies/" + complexA).with(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project_id").value("__global__"))
                .andExpect(jsonPath("$.data.interval_minutes").value(120));
    }

    // (2) 安全阈值：非最高权限 403 且无行；最高权限本人 200 并留痕；缺上下限或间隔越界 400。
    @Test
    void onlyMasterAdjustsInterval() throws Exception {
        save(manager, "m", 90, 60, 180)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("MASTER_APPROVAL_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM amusement_spot_check_policies WHERE project_id=?", Integer.class, complexA)).isZero();

        save(master(), "ok", 90, 60, 180)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project_id").value(complexA))
                .andExpect(jsonPath("$.data.interval_minutes").value(90))
                .andExpect(jsonPath("$.data.min_minutes").value(60))
                .andExpect(jsonPath("$.data.max_minutes").value(180));
        mockMvc.perform(get("/api/v1/amusement/spot-check-policies/" + complexA).with(manager))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.interval_minutes").value(90));
        save(master(), "no-max", 90, 60, null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        save(master(), "out", 200, 60, 180)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='amusement.spot_check_policy.updated'"
                + " AND business_id=?", Integer.class, complexA)).isEqualTo(1);
    }

    // (3) 上次巡检后 119 分钟入场不记、121 分钟入场记一条（重复判不重记）；之后再巡检，从新锚点重新起算。
    @Test
    void admissionPastIntervalIsRecordedOncePerAnchor() {
        inspection(amusementA, t0);
        entry(amusementA, complexA, t0.plusSeconds(119 * 60));
        spotChecks.detectMisses(judgedAt);
        assertThat(misses(amusementA)).isEmpty();

        entry(amusementA, complexA, t0.plusSeconds(121 * 60));
        spotChecks.detectMisses(judgedAt);
        spotChecks.detectMisses(judgedAt);
        assertThat(misses(amusementA)).containsExactly(Map.of("anchor_at", t0, "first_admission_at", t0.plusSeconds(121 * 60),
                "interval_minutes", 120));

        inspection(amusementA, t0.plusSeconds(130 * 60));
        entry(amusementA, complexA, t0.plusSeconds(200 * 60));
        spotChecks.detectMisses(judgedAt);
        assertThat(misses(amusementA)).hasSize(1);
        entry(amusementA, complexA, t0.plusSeconds(251 * 60));
        spotChecks.detectMisses(judgedAt);
        assertThat(misses(amusementA)).hasSize(2)
                .element(1).isEqualTo(Map.of("anchor_at", t0.plusSeconds(130 * 60), "first_admission_at", t0.plusSeconds(251 * 60),
                        "interval_minutes", 120));
    }

    // (4) 超时后补做抽检不抹掉缺口：判定在补做之后才跑，仍按入场当时最近一次巡检记一条。
    @Test
    void lateSpotCheckDoesNotHideMiss() {
        inspection(amusementA, t0);
        entry(amusementA, complexA, t0.plusSeconds(125 * 60));
        inspection(amusementA, t0.plusSeconds(128 * 60));
        spotChecks.detectMisses(judgedAt);
        assertThat(misses(amusementA)).containsExactly(Map.of("anchor_at", t0, "first_admission_at", t0.plusSeconds(125 * 60),
                "interval_minutes", 120));
    }

    // (5) 开门前巡检：从营业日起点起算，10:05 入场不记；之后一直不巡检，12:01 入场记一条（锚点为 07:30 那次）。
    @Test
    void intervalStartsAtBusinessDayStartAfterPreOpenInspection() {
        Instant preOpen = at(7, 30);
        inspection(amusementA, preOpen);
        entry(amusementA, complexA, at(10, 5));
        spotChecks.detectMisses(judgedAt);
        assertThat(misses(amusementA)).isEmpty();
        entry(amusementA, complexA, at(12, 1));
        spotChecks.detectMisses(judgedAt);
        assertThat(misses(amusementA)).containsExactly(Map.of("anchor_at", preOpen, "first_admission_at", at(12, 1),
                "interval_minutes", 120));
    }

    // (6) 间隔按综合体：A 配 60 分钟，A 的项目 61 分钟即记；B（出厂 120 分钟）的项目同时刻入场不记。
    @Test
    void intervalFollowsComplexOfTheAdmission() {
        jdbc.update("INSERT INTO amusement_spot_check_policies(project_id,interval_minutes,min_minutes,max_minutes) VALUES(?,60,30,120)",
                complexA);
        inspection(amusementA, t0);
        inspection(amusementB, t0);
        entry(amusementA, complexA, t0.plusSeconds(61 * 60));
        entry(amusementB, complexB, t0.plusSeconds(61 * 60));
        spotChecks.detectMisses(judgedAt);
        assertThat(misses(amusementA)).containsExactly(Map.of("anchor_at", t0, "first_admission_at", t0.plusSeconds(61 * 60),
                "interval_minutes", 60));
        assertThat(misses(amusementB)).isEmpty();
    }

    // (7) 超间隔后只有出场记录或失败核销：不记。
    @Test
    void exitsAndFailedAdmissionsDoNotCount() {
        inspection(amusementA, t0);
        verification(amusementA, complexA, t0.plusSeconds(150 * 60), "exit", "success");
        verification(amusementA, complexA, t0.plusSeconds(160 * 60), "entry", "failed");
        spotChecks.detectMisses(judgedAt);
        assertThat(misses(amusementA)).isEmpty();
    }

    // (8) 列表：真实 HTTP 回蛇形键；商户账号只见本商户项目（同综合体里别家商户的记录也看不到）；综合体 B 的公司员工看不到综合体 A 的记录。
    @Test
    void missListIsScoped() throws Exception {
        inspection(amusementA, t0);
        inspection(amusementB, t0);
        entry(amusementA, complexA, t0.plusSeconds(121 * 60));
        entry(amusementB, complexB, t0.plusSeconds(121 * 60));
        inspection(amusementA2, t0);
        entry(amusementA2, complexA, t0.plusSeconds(121 * 60));
        spotChecks.detectMisses(judgedAt);
        assertThat(misses(amusementA2)).hasSize(1);

        mockMvc.perform(get("/api/v1/amusement/spot-check-misses").with(merchantStaffA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.project_id=='" + amusementA + "')].anchor_at").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.project_id=='" + amusementA + "')].first_admission_at").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.project_id=='" + amusementA + "')].interval_minutes").value(120))
                .andExpect(jsonPath("$.data[?(@.project_id=='" + amusementA + "')].detected_at").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.project_id=='" + amusementA + "')].project_name").value("旋转木马"))
                .andExpect(jsonPath("$.data[?(@.project_id=='" + amusementB + "')]").isEmpty())
                .andExpect(jsonPath("$.data[?(@.project_id=='" + amusementA2 + "')]").isEmpty());
        mockMvc.perform(get("/api/v1/amusement/spot-check-misses").with(companyStaffB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.project_id=='" + amusementA + "')]").isEmpty())
                .andExpect(jsonPath("$.data[?(@.project_id=='" + amusementB + "')].interval_minutes").value(120));
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private Instant at(int hour, int minute) {
        return yesterday.atTime(LocalTime.of(hour, minute)).atZone(LocalZone.ID).toInstant();
    }

    private ResultActions save(RequestPostProcessor who, String tag, Integer interval, Integer min, Integer max) throws Exception {
        StringBuilder body = new StringBuilder("{\"idempotency_key\":\"a373-spot-" + tag + "-" + digits + "\"");
        if (interval != null) body.append(",\"interval_minutes\":").append(interval);
        if (min != null) body.append(",\"min_minutes\":").append(min);
        if (max != null) body.append(",\"max_minutes\":").append(max);
        return mockMvc.perform(put("/api/v1/amusement/spot-check-policies/" + complexA).with(who)
                .contentType(MediaType.APPLICATION_JSON).content(body.append("}").toString()));
    }

    private RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private void inspection(String amusement, Instant submittedAt) {
        jdbc.update("INSERT INTO amusement_safety_inspections (id, project_id, items_json, overall_result, inspection_status,"
                + " rectification_status, reopen_status, inspected_at, submitted_at, created_at, updated_at)"
                + " VALUES (?, ?, '[{\"item_code\":\"point_1\",\"result\":\"pass\"}]'::jsonb, 'pass', 'submitted', 'not_required',"
                + " 'not_required', ?, ?, now(), now())",
                "insp_a373_" + digits + "_" + (rows++), amusement, Timestamp.from(submittedAt), Timestamp.from(submittedAt));
    }

    private void entry(String amusement, String complex, Instant at) {
        verification(amusement, complex, at, "entry", "success");
    }

    private void verification(String amusement, String complex, Instant at, String direction, String result) {
        jdbc.update("INSERT INTO amusement_verifications (id, project_id, session_id, credential_payload, verification_result, direction,"
                + " complex_id, business_start_at, created_at) VALUES (?, ?, ?, 'spot-check', ?, ?, ?, ?, ?)",
                "ver_a373_" + digits + "_" + (rows++), amusement, "ses_" + amusement, result, direction, complex,
                Timestamp.from(dayStart), Timestamp.from(at));
    }

    private List<Map<String, Object>> misses(String amusement) {
        return jdbc.query("SELECT anchor_at, first_admission_at, interval_minutes FROM amusement_spot_check_misses WHERE project_id=?"
                + " ORDER BY anchor_at", (rs, n) -> Map.<String, Object>of("anchor_at", rs.getTimestamp(1).toInstant(),
                "first_admission_at", rs.getTimestamp(2).toInstant(), "interval_minutes", rs.getInt(3)), amusement);
    }
}
