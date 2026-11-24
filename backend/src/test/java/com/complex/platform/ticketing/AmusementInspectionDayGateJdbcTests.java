package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.amusement.AmusementBusinessDayService;
import com.complex.platform.amusement.AmusementBusinessDayTestFixture;
import com.complex.platform.amusement.AmusementRepository;
import com.complex.platform.amusement.AmusementService;
import com.complex.platform.amusement.AmusementSessionLifecycleService;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 CF 段第 406 条 406-R1（PGM:528「②当天没有做过合格巡检的设施不得开放」；BSM:1298「当日无合格巡检或有未复检阻断项则拒绝核销」；
 * 第十八批裁决 84「从上一个闭店时点（出厂 07:00）起做完并合格的巡检算这一天，与扫码走点「一轮」同一条界线」）：开放判定看当日有没有已提交的合格巡检、
 * 有没有任何未复检或待批准的阻断项，不再只看历史最新一条。夹具综合体营业日 10:00 起、07:00 闭店（Asia/Shanghai）。
 * 真库、真实 HTTP、真实授权链与蛇形键（核销请求形状同 AmusementVerifyRoutingJdbcIntegrationTests）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"})
@AutoConfigureMockMvc
class AmusementInspectionDayGateJdbcTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    @Autowired AmusementService amusement;
    @Autowired AmusementRepository repository;
    @Autowired AmusementSessionLifecycleService lifecycle;
    @Autowired AmusementBusinessDayService days;
    @Autowired SupervisorReviewService reviews;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mockMvc;
    private AmusementTicketJdbcFixture.Data f;
    private AccessActor gate;
    private AccessActor viewer;

    @AfterEach void cleanup() {
        try {
            if (f != null) AmusementTicketJdbcFixture.cleanupTicket(jdbc, f);
        } finally { SecurityContextHolder.clearContext(); }
    }

    /**
     * 唯一的合格巡检做于本轮起点（上一个闭店时点）前一分钟：开放资格只报 MISSING_SAFETY_INSPECTION、零设备档案也拒核销；
     * 闭店时段（起点后一分钟，不晚于当前）做完并提交的合格巡检算这一天，开放资格放行、核销成功。
     */
    @Test void onlyInspectionsSinceLastCloseCountForTheDay() throws Exception {
        seed(5);
        Instant start = dayStart();
        inspectedAt(start.minusSeconds(60));
        eligibility().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(false))
                .andExpect(jsonPath("$.data.blocked_reasons.length()").value(1))
                .andExpect(jsonPath("$.data.blocked_reasons[0]").value("MISSING_SAFETY_INSPECTION"));
        verify("stale").andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("SAFETY_BLOCKED"));

        inspectedAt(inRound(start));
        eligibility().andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(true));
        verify("today").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
    }

    /** 更早一条不合格、还没复检的巡检，加上当日一条合格巡检：仍拦，原因 SAFETY_REOPEN_PENDING。 */
    @Test void olderUnrecheckedFailureStillBlocksAfterTodaysPass() throws Exception {
        seed(5);
        jdbc.update("""
            INSERT INTO amusement_safety_inspections(id,project_id,template_id,items_json,overall_result,inspection_status,
              rectification_status,reopen_status,inspected_at,submitted_at,created_at,updated_at)
            VALUES(?,?,?,'[{"name":"restraint","result":"fail"}]'::jsonb,'fail','submitted','pending','pending',
              now()-interval '1 day',now()-interval '1 day',now()-interval '1 day',now()-interval '1 day')
            """, "a406_fail_" + f.key(), f.project(), f.safety());
        eligibility().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(false))
                .andExpect(jsonPath("$.data.blocked_reasons[?(@ == 'SAFETY_REOPEN_PENDING')]").exists());
        verify("blocked").andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("SAFETY_BLOCKED"));
    }

    /** 硬超容自动停运的项目只有前一天的合格巡检：人数回落后系统也不自动恢复开放。 */
    @Test void hardCapacityRecoveryNeedsTodaysPass() throws Exception {
        seed(5);
        verify("fill").andExpect(status().isOk());
        inspectedAt(dayStart().minusSeconds(60));
        jdbc.update("UPDATE amusement_projects SET open_status='paused', auto_paused_reason='hard_capacity' WHERE id=?", f.project());
        amusement.reassessHardCapacityRecovery(Instant.now());
        assertThat(jdbc.queryForObject("SELECT open_status FROM amusement_projects WHERE id=?", String.class, f.project()))
                .isEqualTo("paused");
    }

    /**
     * 收尾复核修正（设计判断第 14 项「合格时点有复检取复检时刻」；BSM:1298「当日无合格巡检或有未复检阻断项则拒绝核销」）：上一轮营业中抽检不合格、
     * 本轮内复检通过并经主管批准恢复营业的已提交巡检算这一天（夹具那条合格巡检移到本轮起点前），开放资格放行。
     */
    @Test void recheckPassedInRoundAndApprovedCountsForTheDay() throws Exception {
        seed(5);
        Instant start = dayStart();
        inspectedAt(start.minusSeconds(60));
        Timestamp spotCheck = Timestamp.from(start.minusSeconds(9 * 3600));
        Timestamp recheck = Timestamp.from(inRound(start));
        jdbc.update("""
            INSERT INTO amusement_safety_inspections(id,project_id,template_id,items_json,overall_result,inspection_status,
              rectification_status,reopen_status,inspected_at,submitted_at,rechecked_at,approved_at,created_at,updated_at)
            VALUES(?,?,?,'[{"name":"restraint","result":"fail"}]'::jsonb,'pass','submitted','rechecked','approved',?,?,?,?,?,?)
            """, "a406_recheck_" + f.key(), f.project(), f.safety(), spotCheck, spotCheck, recheck, recheck, spotCheck, recheck);
        eligibility().andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(true));
    }

    /** 收尾复核修正（BSM:1298）：本轮只有一条合格但还没提交的巡检（夹具那条移到本轮起点前）：只报 SAFETY_INSPECTION_NOT_SUBMITTED。 */
    @Test void onlyUnsubmittedPassInRoundReportsNotSubmitted() throws Exception {
        seed(5);
        Instant start = dayStart();
        inspectedAt(start.minusSeconds(60));
        Timestamp at = Timestamp.from(inRound(start));
        jdbc.update("""
            INSERT INTO amusement_safety_inspections(id,project_id,template_id,items_json,overall_result,inspection_status,
              rectification_status,reopen_status,inspected_at,created_at,updated_at)
            VALUES(?,?,?,'[{"name":"restraint","result":"pass"}]'::jsonb,'pass','created','not_required','not_required',?,?,?)
            """, "a406_draft_" + f.key(), f.project(), f.safety(), at, at, at);
        eligibility().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(false))
                .andExpect(jsonPath("$.data.blocked_reasons.length()").value(1))
                .andExpect(jsonPath("$.data.blocked_reasons[0]").value("SAFETY_INSPECTION_NOT_SUBMITTED"));
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private void seed(int capacity) {
        f = AmusementTicketJdbcFixture.seed(jdbc, repository, lifecycle, days, reviews, 10, "per_use", capacity);
        gate = (AccessActor) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String user = AmusementBusinessDayTestFixture.actor(jdbc, f.complex(), List.of("amusement.safety_inspection.view"));
        jdbc.update("UPDATE access_users SET account_type='platform_admin' WHERE id=?", user);
        viewer = new AccessActor(user, user, "platform_admin", f.complex(), null, "active");
    }

    /** 本轮起点＝上一个闭店时点（夹具综合体 07:00 闭店）。 */
    private static Instant dayStart() {
        Instant now = Instant.now();
        Instant close = LocalDate.now(ZONE).atTime(7, 0).atZone(ZONE).toInstant();
        return now.isBefore(close) ? close.minusSeconds(86400) : close;
    }

    /** 本轮内一刻：起点后一分钟，晚于当前时取当前（北京时间 07:00～07:01 跑时不落在将来）。 */
    private static Instant inRound(Instant start) {
        Instant at = start.plusSeconds(60);
        Instant now = Instant.now();
        return at.isAfter(now) ? now : at;
    }

    /** 夹具那条已提交合格巡检的巡检、提交时刻改到 at。 */
    private void inspectedAt(Instant at) {
        jdbc.update("UPDATE amusement_safety_inspections SET inspected_at=?, submitted_at=? WHERE project_id=?",
                Timestamp.from(at), Timestamp.from(at), f.project());
    }

    private ResultActions eligibility() throws Exception {
        return mockMvc.perform(get("/api/v1/amusement/projects/" + f.project() + "/open-eligibility").with(web(viewer)));
    }

    /** 员工端门票核销屏实发形状（蛇形键、credential_type=qr）。 */
    private ResultActions verify(String tag) throws Exception {
        String body = JSON.writeValueAsString(Map.of("idempotency_key", "a406_" + tag + "_" + f.code(), "credential_type", "qr",
                "credential_payload", f.code(), "device_id", f.device(), "point_id", f.point(),
                "business_context", Map.of("scenario", "ticket_checkin", "staff_id", "gate")));
        return mockMvc.perform(post("/api/v1/ticketing/verifications").with(web(gate))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static RequestPostProcessor web(AccessActor who) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(who, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
