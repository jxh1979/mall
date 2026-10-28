package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.amusement.AmusementBusinessDayTestFixture;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.inventory.HighValueDecisionService;
import com.complex.platform.support.TestSecurityActors;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批第 24 条：高价值判定阈值。ENUM:164「与后台配置的固定金额阈值比较（安全阈值，全平台统一可配，调整须最高权限审批并留痕，
 * 出厂默认 5000 元，须同时配置上下限），大于或等于阈值即高价值」。全平台只一行；阈值与下限、上限同时必填、理由必填；
 * 提交即进最高权限审批（照酒吧营业日 BarBusinessDayJdbcIntegrationTests），批准后生效、驳回不生效，结算配置的差人复核转正不再适用。
 * 请求体按前端 adminClient 蛇形键。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class HighValueThresholdMasterApprovalJdbcIntegrationTests {

    private static final String ENDPOINT = "/api/v1/settlement/config/high-value-threshold";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SupervisorReviewService reviews;
    @Autowired private HighValueDecisionService decisions;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    private String project;

    @BeforeEach
    void seed() {
        project = AmusementBusinessDayTestFixture.complex(jdbc);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 前端真实请求形状提交 → 全平台一行草案（项目、商户为空）进最高权限审批，待审期间判定仍读出厂 5000 元。 */
    @Test
    void submitsSnakeCaseRequestIntoTopAuthorityReviewAsGlobalDraft() throws Exception {
        String requester = "hvt_req_" + suffix;
        submit(staff(requester), body(600_000, 100_000, 1_000_000, "按近一年到货价复核")).andExpect(status().isOk());

        assertThat(reviewCount()).isEqualTo(1);
        String draft = draftOf(requester);
        assertThat(jdbc.queryForMap("SELECT project_id,merchant_id,high_value_threshold_cent,high_value_threshold_min_cent,high_value_threshold_max_cent FROM high_value_thresholds WHERE id=?", draft))
                .containsEntry("project_id", null).containsEntry("merchant_id", null)
                .containsEntry("high_value_threshold_cent", 600_000L)
                .containsEntry("high_value_threshold_min_cent", 100_000L)
                .containsEntry("high_value_threshold_max_cent", 1_000_000L);
        String review = jdbc.queryForObject("SELECT review_id FROM high_value_thresholds WHERE id=?", String.class, draft);
        assertThat(jdbc.queryForObject("SELECT approval_level FROM supervisor_review_decisions WHERE review_id=?", String.class, review))
                .isEqualTo("top_authority");
        assertThat(thresholdNow()).isEqualTo(500_000L);
    }

    /** 阈值、下限、上限须同时填写且 0 ≤ 下限 ≤ 阈值 ≤ 上限，理由必填；不落草案，失败按 OpenAPI 声明留痕。 */
    @Test
    void thresholdBoundsAndReasonAreAllRequired() throws Exception {
        String requester = "hvt_val_" + suffix;
        RequestPostProcessor who = staff(requester);
        int failuresBefore = failures();
        submit(who, "{\"idempotency_key\":\"hvt-" + UUID.randomUUID() + "\",\"high_value_threshold_cent\":600000,\"reason\":\"缺上下限\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        submit(who, body(50_000, 100_000, 1_000_000, "低于下限"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        submit(who, body(2_000_000, 100_000, 1_000_000, "高于上限"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        submit(who, body(600_000, 100_000, 1_000_000, ""))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM high_value_thresholds WHERE updated_by=?", Integer.class, requester)).isZero();
        assertThat(failures() - failuresBefore).isEqualTo(4);
    }

    /** 另一名结算配置管理人员不能用「复核转正」把阈值草案转正（只能经最高权限审批生效）。 */
    @Test
    void draftCannotBeActivatedByAnotherConfigManager() throws Exception {
        String requester = "hvt_a_" + suffix;
        submit(staff(requester), body(600_000, 100_000, 1_000_000, "按近一年到货价复核")).andExpect(status().isOk());
        String draft = draftOf(requester);

        mockMvc.perform(post("/api/v1/settlement/config/high_value_threshold/" + draft + "/activate").with(staff("hvt_b_" + suffix))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("HIGH_VALUE_THRESHOLD_TOP_AUTHORITY_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT is_draft FROM high_value_thresholds WHERE id=?", Boolean.class, draft)).isTrue();
        assertThat(thresholdNow()).isEqualTo(500_000L);
    }

    /** 已有一条在审批中的阈值草案时再提交 → 409，不覆盖在审草案。 */
    @Test
    void secondSubmissionWhileReviewPendingIsRejected() throws Exception {
        RequestPostProcessor who = staff("hvt_dup_" + suffix);
        submit(who, body(600_000, 100_000, 1_000_000, "第一次提交")).andExpect(status().isOk());

        submit(who, body(700_000, 100_000, 1_000_000, "第二次提交"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("HIGH_VALUE_THRESHOLD_REVIEW_PENDING"));
        assertThat(jdbc.queryForObject("SELECT high_value_threshold_cent FROM high_value_thresholds WHERE is_draft=TRUE AND status='active' AND project_id IS NULL AND merchant_id IS NULL",
                Long.class)).isEqualTo(600_000L);
    }

    /** 上级同意后仍待审，非最高权限不能批；最高权限批准后判定读到新阈值，出厂默认行停用。 */
    @Test
    void onlyTopAuthorityApprovalActivatesThresholdAndRetiresFactorySeed() throws Exception {
        String requester = "hvt_req2_" + suffix;
        RequestPostProcessor who = staff(requester);
        String superior = AmusementBusinessDayTestFixture.actor(jdbc, project,
                List.of("governance.supervisor_review.approve", "governance.supervisor_review.view"));
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", requester, superior);
        submit(who, body(600_000, 100_000, 1_000_000, "按近一年到货价复核")).andExpect(status().isOk());
        assertThat(reviewCount()).isEqualTo(1);
        String review = reviewOf(requester);

        AmusementBusinessDayTestFixture.authenticate(superior, project);
        reviews.approve(review, decision("superior", "approved"));
        assertThat(thresholdNow()).isEqualTo(500_000L);
        AmusementBusinessDayTestFixture.authenticate(superior, project);
        assertThatThrownBy(() -> reviews.approve(review, decision("not-master", "approved"))).hasMessageContaining("最高权限");

        AmusementBusinessDayTestFixture.master();
        reviews.approve(review, decision("master", "approved"));
        assertThat(thresholdNow()).isEqualTo(600_000L);
        assertThat(jdbc.queryForObject("SELECT status FROM high_value_thresholds WHERE id='hvt_default'", String.class)).isEqualTo("inactive");
    }

    /** 最高权限驳回 → 草案下线，判定仍读原阈值。 */
    @Test
    void rejectedThresholdNeverTakesEffect() throws Exception {
        String requester = "hvt_rej_" + suffix;
        submit(staff(requester), body(600_000, 100_000, 1_000_000, "按近一年到货价复核")).andExpect(status().isOk());
        assertThat(reviewCount()).isEqualTo(1);
        String draft = draftOf(requester);

        AmusementBusinessDayTestFixture.master();
        reviews.approve(reviewOf(requester), decision("master", "rejected"));

        assertThat(jdbc.queryForObject("SELECT status FROM high_value_thresholds WHERE id=?", String.class, draft)).isEqualTo("inactive");
        assertThat(thresholdNow()).isEqualTo(500_000L);
    }

    private String body(long cent, long min, long max, String reason) {
        return """
                {"idempotency_key":"hvt-%s","high_value_threshold_cent":%d,"high_value_threshold_min_cent":%d,
                 "high_value_threshold_max_cent":%d,"reason":"%s"}
                """.formatted(UUID.randomUUID(), cent, min, max, reason);
    }

    private ResultActions submit(RequestPostProcessor who, String body) throws Exception {
        return mockMvc.perform(post(ENDPOINT).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int reviewCount() {
        return jdbc.queryForObject("SELECT count(*) FROM supervisor_review_decisions WHERE object_type='high_value_threshold'"
                + " AND object_id IN (SELECT id FROM high_value_thresholds WHERE updated_by LIKE ?)", Integer.class, "hvt_%" + suffix);
    }

    private String draftOf(String requester) {
        return jdbc.queryForObject("SELECT id FROM high_value_thresholds WHERE updated_by=? AND is_draft=TRUE", String.class, requester);
    }

    private String reviewOf(String requester) {
        return jdbc.queryForObject("SELECT review_id FROM supervisor_review_decisions WHERE object_type='high_value_threshold' AND object_id=?",
                String.class, draftOf(requester));
    }

    private Long thresholdNow() {
        var previous = SecurityContextHolder.getContext().getAuthentication();
        try {
            TestSecurityActors.asPlatformAdmin();
            return decisions.decide(null, null).thresholdCent();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }

    private int failures() {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='settlement.config.high_value_thresholds.update_failed'",
                Integer.class);
    }

    private static SupervisorReviewApprovalRequest decision(String key, String decision) {
        return new SupervisorReviewApprovalRequest(key + UUID.randomUUID(), "复核", List.of(), null, decision, "核实阈值依据");
    }

    /** 结算配置管理人员（平台域员工账号，无商户归属）。 */
    private RequestPostProcessor staff(String id) {
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,status,created_at) VALUES(?,?,'staff',?,'active',now())",
                id, id, project);
        String role = "hvt_role_" + id;
        jdbc.update("INSERT INTO roles(id,role_name,created_at) VALUES(?,?,now())", role, "高价值阈值测试角色-" + id);
        for (String permission : List.of("settlement.config.manage", "settlement.config.view")) {
            jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)", role, permission);
        }
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", id, role);
        AccessActor actor = new AccessActor(id, id, "staff", project, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of())).postProcessRequest(request);
        };
    }
}
