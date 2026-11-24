package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.amusement.AmusementBusinessDayService;
import com.complex.platform.amusement.AmusementBusinessDayTestFixture;
import com.complex.platform.amusement.AmusementRepository;
import com.complex.platform.amusement.AmusementSessionLifecycleService;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 CF 段第 409 条（BSM:1267「运营容量（软上限）只是经营建议线，超过时仅告警提示、仍可放行。达到运营容量（软上限）时的处置方式可在后台配置为两种之一：
 * 「记录异常事件」（超过软上限即生成异常事件留痕，出厂默认）或「仅提示」（只在现场提示、不生成异常事件）；两种方式都不改变硬安全容量的阻断与自动停运」；
 * BSM:1295；ENUM:145；DEC-R7-02）：409-R1「仅提示」达到软上限不生成异常事件、「记录异常事件」照旧生成，两种方式硬上限都阻断并自动停运；
 * 409-R3 达到运营容量时核销结果带现场提示标记。真库、真实 HTTP、真实授权链与蛇形键（核销请求形状同 AmusementVerifyRoutingJdbcIntegrationTests）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"})
@AutoConfigureMockMvc
class AmusementSoftCapacityDisposalJdbcTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    @Autowired AmusementRepository repository;
    @Autowired AmusementSessionLifecycleService lifecycle;
    @Autowired AmusementBusinessDayService days;
    @Autowired SupervisorReviewService reviews;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mockMvc;
    private AmusementTicketJdbcFixture.Data f;
    private AccessActor gate;
    private AccessActor configurer;
    private int keys;

    @AfterEach void cleanup() {
        try {
            if (f != null) AmusementTicketJdbcFixture.cleanupTicket(jdbc, f);
        } finally { SecurityContextHolder.clearContext(); }
    }

    /** 409-R1：「仅提示」达到软上限放行且不落异常事件、核销结果照样带现场提示（409-R3）；「记录异常事件」落一条中级异常；两种方式达到硬上限都 409 并自动停运。 */
    @Test void warnOnlySkipsSoftIncidentAndBothModesStillStopAtHardCapacity() throws Exception {
        seed(2);
        policy(1, "warn_only").andExpect(status().isOk());
        verify("warn-soft").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"))
                .andExpect(jsonPath("$.data.operating_capacity_reached").value(true));
        assertThat(incidents("medium")).isZero();

        policy(1, "block_hard").andExpect(status().isOk());
        verify("block-soft").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
        assertThat(incidents("medium")).isEqualTo(1);

        verify("block-hard").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("SESSION_CAPACITY_EXHAUSTED"));
        assertThat(openStatus()).isEqualTo("paused");

        jdbc.update("UPDATE amusement_projects SET open_status='open', auto_paused_reason=NULL WHERE id=?", f.project());
        policy(1, "warn_only").andExpect(status().isOk());
        verify("warn-hard").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("SESSION_CAPACITY_EXHAUSTED"));
        assertThat(openStatus()).isEqualTo("paused");
        assertThat(incidents("medium")).isEqualTo(1);
    }

    /** 409-R3：在玩人数未到运营容量时核销结果标 false，达到时标 true（「记录异常事件」方式同样提示）。 */
    @Test void verificationResultFlagsOperatingCapacityReached() throws Exception {
        seed(5);
        policy(2, "block_hard").andExpect(status().isOk());
        verify("first").andExpect(status().isOk()).andExpect(jsonPath("$.data.operating_capacity_reached").value(false));
        verify("second").andExpect(status().isOk()).andExpect(jsonPath("$.data.operating_capacity_reached").value(true));
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private void seed(int capacity) {
        f = AmusementTicketJdbcFixture.seed(jdbc, repository, lifecycle, days, reviews, 10, "per_use", capacity);
        gate = (AccessActor) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String user = AmusementBusinessDayTestFixture.actor(jdbc, f.complex(),
                List.of("amusement.over_capacity_policy.configure", "amusement.over_capacity_policy.view"));
        jdbc.update("UPDATE access_users SET account_type='platform_admin' WHERE id=?", user);
        configurer = new AccessActor(user, user, "platform_admin", f.complex(), null, "active");
    }

    /** 后台「项目与场次」页软上限处置方式表单实发形状。 */
    private ResultActions policy(int operatingCapacity, String mode) throws Exception {
        return mockMvc.perform(put("/api/v1/amusement/projects/" + f.project() + "/over-capacity-policy").with(web(configurer))
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(Map.of(
                        "idempotency_key", "a409-policy-" + UUID.randomUUID(), "operating_capacity", operatingCapacity,
                        "disposal_mode", mode))));
    }

    /** 员工端门票核销屏实发形状（蛇形键、credential_type=qr）。 */
    private ResultActions verify(String tag) throws Exception {
        String body = JSON.writeValueAsString(Map.of("idempotency_key", "a409_" + tag + "_" + (keys++) + "_" + f.code(),
                "credential_type", "qr", "credential_payload", f.code(), "device_id", f.device(), "point_id", f.point(),
                "business_context", Map.of("scenario", "ticket_checkin", "staff_id", "gate")));
        return mockMvc.perform(post("/api/v1/ticketing/verifications").with(web(gate))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int incidents(String severity) {
        return jdbc.queryForObject("SELECT count(*) FROM amusement_incidents WHERE amusement_project_id=? AND incident_type='over_capacity'"
                + " AND severity=?", Integer.class, f.project(), severity);
    }

    private String openStatus() {
        return jdbc.queryForObject("SELECT open_status FROM amusement_projects WHERE id=?", String.class, f.project());
    }

    private static RequestPostProcessor web(AccessActor who) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(who, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
