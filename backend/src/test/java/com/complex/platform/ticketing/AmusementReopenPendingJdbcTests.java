package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 第十八批 CF 段第 405 条 405-R1（BSM:1300「检修后必须主管确认才能恢复营业（禁止巡检员单人闭环）」；BSM:1270「主管确认检修通过后，设施才能恢复营业。」；
 * BSM:1298「当日无合格巡检或有未复检阻断项则拒绝核销」）：不合格巡检复检通过、主管还没批准恢复营业时，开放资格、核销与硬超容自动恢复都按未复检阻断项拦；
 * 主管批准后放行。真库、真实 HTTP、真实授权链与蛇形键（核销请求形状同 AmusementVerifyRoutingJdbcIntegrationTests）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"})
@AutoConfigureMockMvc
class AmusementReopenPendingJdbcTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    @Autowired AmusementService amusement;
    @Autowired AmusementRepository repository;
    @Autowired AmusementSessionLifecycleService lifecycle;
    @Autowired AmusementBusinessDayService days;
    @Autowired SupervisorReviewService reviews;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mockMvc;
    private AmusementTicketJdbcFixture.Data f;
    private AccessActor gate;
    private AccessActor inspector;
    private AccessActor supervisor;

    @AfterEach void cleanup() {
        try {
            if (f != null) AmusementTicketJdbcFixture.cleanupTicket(jdbc, f);
        } finally { SecurityContextHolder.clearContext(); }
    }

    /** 复检通过、未批准：开放资格 allowed=false 带 SAFETY_REOPEN_PENDING，核销 409 SAFETY_BLOCKED；主管批准后放行、核销成功。 */
    @Test void recheckedPassStaysBlockedUntilSupervisorApprovesReopen() throws Exception {
        seed(5);
        String inspection = failRectifyAndRecheckPass("a");
        eligibility().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(false))
                .andExpect(jsonPath("$.data.blocked_reasons[?(@ == 'SAFETY_REOPEN_PENDING')]").exists());
        verify("pending").andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("SAFETY_BLOCKED"));

        approveReopen(inspection).andExpect(status().isOk());
        eligibility().andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(true));
        verify("approved").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
    }

    /** 硬超容自动停运的项目：复检通过但未批准时人数回落也不自动恢复，主管批准后恢复开放。 */
    @Test void hardCapacityPauseIsNotLiftedBeforeReopenApproval() throws Exception {
        seed(5);
        verify("fill").andExpect(status().isOk());
        String inspection = failRectifyAndRecheckPass("b");
        jdbc.update("UPDATE amusement_projects SET open_status='paused', auto_paused_reason='hard_capacity' WHERE id=?", f.project());

        amusement.reassessHardCapacityRecovery(Instant.now());
        assertThat(openStatus()).isEqualTo("paused");

        approveReopen(inspection).andExpect(status().isOk());
        amusement.reassessHardCapacityRecovery(Instant.now());
        assertThat(openStatus()).isEqualTo("open");
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private void seed(int capacity) {
        f = AmusementTicketJdbcFixture.seed(jdbc, repository, lifecycle, days, reviews, 10, "per_use", capacity);
        gate = (AccessActor) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        inspector = staff(List.of("amusement.safety_inspection.create", "amusement.safety_inspection.submit",
                "amusement.safety_inspection.rectify", "amusement.safety_inspection.recheck", "amusement.safety_inspection.view",
                "evidence.attachment.create"));
        supervisor = staff(List.of("amusement.safety_inspection.approve_reopen", "amusement.safety_inspection.view"));
    }

    /** 不合格巡检 → 提交 → 提交整改 → 复检通过（附证据），返回巡检编号。 */
    private String failRectifyAndRecheckPass(String tag) throws Exception {
        String evidence = upload();
        String created = as(inspector, "/api/v1/amusement/safety-inspections", Map.of("idempotency_key", key("create-" + tag),
                "project_id", f.project(), "overall_result", "fail",
                "items", List.of(Map.of("item_code", "guardrail", "item_name", "护栏", "result", "fail", "severity", "high"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JsonPath.read(created, "$.data.id");
        String base = "/api/v1/amusement/safety-inspections/" + id;
        as(inspector, base + "/submit", Map.of("idempotency_key", key("submit-" + tag), "comment", "护栏松动")).andExpect(status().isOk());
        as(inspector, base + "/rectify", Map.of("idempotency_key", key("rectify-" + tag), "comment", "已紧固护栏")).andExpect(status().isOk());
        as(inspector, base + "/recheck", Map.of("idempotency_key", key("recheck-" + tag), "action_result", "pass",
                "comment", "复检通过", "attachment_ids", List.of(evidence)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.reopen_status").value("pending_approval"));
        return id;
    }

    private ResultActions approveReopen(String inspection) throws Exception {
        return as(supervisor, "/api/v1/amusement/safety-inspections/" + inspection + "/approve-reopen",
                Map.of("idempotency_key", key("approve"), "reason", "主管现场确认整改到位"));
    }

    private ResultActions eligibility() throws Exception {
        return mockMvc.perform(get("/api/v1/amusement/projects/" + f.project() + "/open-eligibility").with(web(inspector)));
    }

    /** 员工端门票核销屏实发形状（蛇形键、credential_type=qr）。 */
    private ResultActions verify(String tag) throws Exception {
        String body = JSON.writeValueAsString(Map.of("idempotency_key", "a405_" + tag + "_" + f.code(), "credential_type", "qr",
                "credential_payload", f.code(), "device_id", f.device(), "point_id", f.point(),
                "business_context", Map.of("scenario", "ticket_checkin", "staff_id", "gate")));
        return mockMvc.perform(post("/api/v1/ticketing/verifications").with(web(gate))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String upload() throws Exception {
        String body = mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", "recheck.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                        .param("business_type", "amusement_safety_inspection").param("attachment_role", "photo").with(web(inspector)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data.id");
    }

    private ResultActions as(AccessActor who, String path, Map<String, ?> body) throws Exception {
        return mockMvc.perform(post(path).with(web(who)).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor web(AccessActor who) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(who, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    /** 本综合体公司员工（不挂商户，账号类型同夹具核销操作人；中文角色名、后缀纯数字，见 AmusementBusinessDayTestFixture.actor）。 */
    private AccessActor staff(List<String> permissions) {
        String user = AmusementBusinessDayTestFixture.actor(jdbc, f.complex(), permissions);
        jdbc.update("UPDATE access_users SET account_type='platform_admin' WHERE id=?", user);
        return new AccessActor(user, user, "platform_admin", f.complex(), null, "active");
    }

    private String openStatus() {
        return jdbc.queryForObject("SELECT open_status FROM amusement_projects WHERE id=?", String.class, f.project());
    }

    private static String key(String tag) {
        return "a405-" + tag + "-" + UUID.randomUUID();
    }
}
