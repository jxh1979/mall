package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.amusement.AmusementBusinessDayTestFixture;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批第 1 条：每家酒吧的营业日版本。ENUM:187「起止时点由后台配置（安全阈值，按每个项目、每家酒吧可配，调整须最高权限审批并留痕，
 * 出厂默认起点为上午 10:00、闭店为次日 07:00，须同时配置上下限）」；BSM:1682「酒吧仍按每家酒吧配置」。请求体按前端 adminClient 蛇形键。
 * V201 按业务域直改的旧入口已删除。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarBusinessDayJdbcIntegrationTests {

    private static final String CONFIGS = "/api/v1/bar-business-day-configs";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private SupervisorReviewService reviews;
    @Autowired private ApplicationContext context;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    private String project;
    private String bar;

    @BeforeEach
    void seed() {
        project = AmusementBusinessDayTestFixture.complex(jdbc);
        bar = "bbd_bar_" + suffix;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at) VALUES(?,'营业日审批酒吧','bar',?,'active',now(),now())",
                bar, project);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        if (context.containsBean("barBusinessDayService")) {
            ReflectionTestUtils.setField(context.getBean("barBusinessDayService"), "clock", Clock.systemUTC());
        }
    }

    /** 前端真实请求形状提交 → 进最高权限审批，待审期间不生效。 */
    @Test
    void submitsSnakeCaseRequestIntoTopAuthorityReview() throws Exception {
        JsonNode data = submit(staff(null, "bar.business_day.manage", "bar.business_day.view"),
                body(11, 9, 12, 7, 5, 8, Instant.now().plus(1, ChronoUnit.DAYS)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString().transform(this::data);
        assertThat(data.path("status").asText()).isEqualTo("pending_review");
        assertThat(data.path("bar_id").asText()).isEqualTo(bar);
        assertThat(data.path("project_id").asText()).isEqualTo(project);
        assertThat(data.path("close_hour").asInt()).isEqualTo(7);
        assertThat(jdbc.queryForObject("SELECT approval_level FROM supervisor_review_decisions WHERE review_id=?", String.class,
                data.path("review_id").asText())).isEqualTo("top_authority");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='bar.business_day.submit_completed' AND business_id=?",
                Integer.class, data.path("id").asText())).isEqualTo(1);
    }

    /** 上级同意后仍待审，非最高权限不能批，最高权限批准后才生效。 */
    @Test
    void onlyTopAuthorityApprovalActivatesVersion() throws Exception {
        String requester = "bbd_req_" + suffix;
        RequestPostProcessor who = staff(requester, "bar.business_day.manage", "bar.business_day.view");
        String superior = AmusementBusinessDayTestFixture.actor(jdbc, project,
                List.of("governance.supervisor_review.approve", "governance.supervisor_review.view"));
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", requester, superior);
        JsonNode data = submit(who, body(11, 9, 12, 7, 5, 8, Instant.now().plus(1, ChronoUnit.DAYS)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString().transform(this::data);
        String review = data.path("review_id").asText();
        AmusementBusinessDayTestFixture.authenticate(superior, project);
        reviews.approve(review, approval("superior"));
        assertThat(statusOf(data)).isEqualTo("pending_review");
        assertThatThrownBy(() -> reviews.approve(review, approval("not-master"))).hasMessageContaining("最高权限");
        AmusementBusinessDayTestFixture.master();
        reviews.approve(review, approval("master"));
        assertThat(statusOf(data)).isEqualTo("approved");
    }

    /** 批准时已过拟生效时点 → 记 expired，不倒追生效。 */
    @Test
    void approvalAfterIntendedEffectExpiresVersion() throws Exception {
        Instant effective = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        JsonNode data = submit(staff(null, "bar.business_day.manage"), body(11, 9, 12, 7, 5, 8, effective))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString().transform(this::data);
        ReflectionTestUtils.setField(context.getBean("barBusinessDayService"), "clock", Clock.fixed(effective, ZoneOffset.UTC));
        AmusementBusinessDayTestFixture.master();
        reviews.approve(data.path("review_id").asText(), approval("late"));
        assertThat(statusOf(data)).isEqualTo("expired");
    }

    /** 起点或闭店越出各自上下限 → 400，不落版本，失败留痕。 */
    @Test
    void hoursOutsideTheirBoundsAreRejected() throws Exception {
        RequestPostProcessor who = staff(null, "bar.business_day.manage");
        int failuresBefore = submitFailures();
        submit(who, body(13, 9, 12, 7, 5, 8, Instant.now().plus(1, ChronoUnit.DAYS)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        submit(who, body(11, 9, 12, 9, 5, 8, Instant.now().plus(1, ChronoUnit.DAYS)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_business_day_configs WHERE bar_id=?", Integer.class, bar)).isZero();
        assertThat(submitFailures() - failuresBefore).isEqualTo(2);
    }

    /** 商户限定授权不能管理酒吧营业日。 */
    @Test
    void merchantScopedActorIsForbidden() throws Exception {
        submit(merchantStaff("bbd_mer_user_" + suffix, "bbd_mer_" + suffix, "bar.business_day.manage"),
                body(11, 9, 12, 7, 5, 8, Instant.now().plus(1, ChronoUnit.DAYS)))
                .andExpect(status().isForbidden());
    }

    /** V201 按业务域直改即生效的旧入口已删除。 */
    @Test
    void legacyDomainCutoffEndpointsAreGone() throws Exception {
        TestSecurityActors.asPlatformAdmin();
        var admin = authentication(SecurityContextHolder.getContext().getAuthentication());
        mockMvc.perform(get("/api/v1/analytics/business-day-cutoffs").with(admin).header("X-Terminal-Type", "web"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/analytics/business-day-cutoffs").with(admin).header("X-Terminal-Type", "web")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"bbd-legacy-" + suffix + "\",\"domain\":\"bar\",\"cutoff_hour\":6}"))
                .andExpect(status().isNotFound());
    }

    private String body(int cutoff, int minCutoff, int maxCutoff, int close, int minClose, int maxClose, Instant effectiveAt) {
        return """
                {"idempotency_key":"bbd-%s-%s","bar_id":"%s","cutoff_hour":%d,"min_cutoff_hour":%d,"max_cutoff_hour":%d,
                 "close_hour":%d,"min_close_hour":%d,"max_close_hour":%d,"effective_at":"%s","reason":"依据已核实的营业计划",
                 "final_approver_id":null,"attachment_ids":[]}
                """.formatted(suffix, UUID.randomUUID(), bar, cutoff, minCutoff, maxCutoff, close, minClose, maxClose, effectiveAt);
    }

    private ResultActions submit(RequestPostProcessor who, String body) throws Exception {
        return mockMvc.perform(post(CONFIGS).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode data(String response) {
        try {
            return json.readTree(response).path("data");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String statusOf(JsonNode data) {
        return jdbc.queryForObject("SELECT status FROM bar_business_day_configs WHERE id=?", String.class, data.path("id").asText());
    }

    private static SupervisorReviewApprovalRequest approval(String key) {
        return new SupervisorReviewApprovalRequest(key + UUID.randomUUID(), "复核", List.of(), null, "approved", "同意");
    }

    /** 员工账号（organization＝本综合体）；merchantId 非空＝商户限定授权。 */
    private RequestPostProcessor staff(String userId, String... permissions) {
        return merchantStaff(userId, null, permissions);
    }

    private int submitFailures() {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='bar.business_day.submit_failed'", Integer.class);
    }

    private RequestPostProcessor merchantStaff(String userId, String merchantId, String... permissions) {
        String id = userId == null ? "bbd_user_" + suffix : userId;
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                id, id, project, merchantId);
        String role = "bbd_role_" + id;
        jdbc.update("INSERT INTO roles(id,role_name,created_at) VALUES(?,?,now())", role, "酒吧营业日测试角色-" + id);
        for (String permission : permissions) {
            jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)", role, permission);
        }
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", id, role);
        AccessActor actor = new AccessActor(id, id, "staff", project, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of())).postProcessRequest(request);
        };
    }
}
