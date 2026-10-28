package com.complex.platform.analytics;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 C 段第 62 条 商户处罚记录（BSM 第 1838 行「由城市项目公司的运营人员填写处罚记录，处罚即扣减健康分，扣分规则由后台配置
 * （业务参数，按项目可配，须同时配置上下限）」；第十八批裁决 8「只配单次扣分上下限（按项目可配），运营登记处罚时自己填扣分」）。
 * 请求按后台网页真实形状（蛇形键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MerchantPenaltyJdbcIntegrationTests {

    private static final String RECORD = "merchant.penalty.record";
    private static final String BOUNDS = "merchant.penalty.bounds.manage";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String projectA = "project_mpa_" + suffix;
    private final String projectB = "project_mpb_" + suffix;
    private final String merchantA = "mer_mpa_" + suffix;
    private StaffRequestFixture staffs;
    private RequestPostProcessor operator;

    @BeforeEach
    void seed() {
        staffs = new StaffRequestFixture(jdbc);
        for (String p : new String[] {projectA, projectB}) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                    + " VALUES (?, 'complex', '处罚记录测试综合体', 'active', now(), now())", p);
        }
        merchant(merchantA, projectA);
        operator = staffs.staff("usr_mp_op_" + suffix, "staff", projectA, null, RECORD, BOUNDS);
    }

    @Test
    void recordPenaltyWithOperatorEnteredPointsWithinBounds() throws Exception {
        Integer healthRows = jdbc.queryForObject("SELECT count(*) FROM analytics_merchant_health_scores", Integer.class);
        saveBounds(operator, projectA, 1, 10).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.min_penalty_points").value(1))
                .andExpect(jsonPath("$.data.max_penalty_points").value(10));
        String id = body(recordPenalty(operator, "k-" + UUID.randomUUID(), projectA, merchantA, 3, "占道经营")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.penalty_points").value(3))
                .andExpect(jsonPath("$.data.merchant_id").value(merchantA))
                .andExpect(jsonPath("$.data.recorded_by").value("usr_mp_op_" + suffix)))
                .path("data").path("id").asText();
        mockMvc.perform(get("/api/v1/merchant-penalties").param("project_id", projectA).with(operator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.min_penalty_points").value(1))
                .andExpect(jsonPath("$.data.max_penalty_points").value(10))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(id))
                .andExpect(jsonPath("$.data.records[0].reason").value("占道经营"));
        assertThat(audit("merchant.penalty_recorded", id)).isEqualTo(1);
        assertThat(audit("merchant.penalty_bounds_updated", projectA)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM merchants WHERE id = ?", String.class, merchantA)).isEqualTo("active");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM analytics_merchant_health_scores", Integer.class)).isEqualTo(healthRows);
    }

    @Test
    void pointsOutsideBoundsRejected() throws Exception {
        saveBounds(operator, projectA, 1, 10).andExpect(status().isOk());
        for (int points : new int[] {0, 11}) {
            recordPenalty(operator, "k-" + UUID.randomUUID(), projectA, merchantA, points, "占道经营")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail_code").value("MERCHANT_PENALTY_POINTS_OUT_OF_BOUNDS"));
        }
        assertThat(rows()).isZero();
        assertThat(audit("merchant.penalty_record_failed", merchantA)).isEqualTo(2);
    }

    @Test
    void boundsNotConfiguredRejected() throws Exception {
        recordPenalty(operator, "k-" + UUID.randomUUID(), projectA, merchantA, 3, "占道经营")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MERCHANT_PENALTY_BOUNDS_NOT_CONFIGURED"));
        mockMvc.perform(get("/api/v1/merchant-penalties").param("project_id", projectA).with(operator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.min_penalty_points").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.records.length()").value(0));
    }

    @Test
    void boundsMustBeSetTogether() throws Exception {
        saveBounds(operator, projectA, 1, null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        saveBounds(operator, projectA, 6, 5).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        saveBounds(operator, projectA, 0, 5).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM merchant_penalty_point_bounds WHERE project_id = ?", Integer.class, projectA))
                .isZero();
    }

    @Test
    void scopeGates() throws Exception {
        saveBounds(operator, projectA, 1, 10).andExpect(status().isOk());
        String outsider = "mer_mpo_" + suffix;
        merchant(outsider, projectB);
        recordPenalty(operator, "k-" + UUID.randomUUID(), projectA, outsider, 3, "占道经营")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("MERCHANT_NOT_IN_PROJECT"));
        RequestPostProcessor otherProject = staffs.staff("usr_mp_b_" + suffix, "staff", projectB, null, RECORD, BOUNDS);
        recordPenalty(otherProject, "k-" + UUID.randomUUID(), projectA, merchantA, 3, "占道经营")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        RequestPostProcessor merchantAccount = staffs.staff("usr_mp_m_" + suffix, "staff", projectA, merchantA, RECORD);
        recordPenalty(merchantAccount, "k-" + UUID.randomUUID(), projectA, merchantA, 3, "占道经营")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        RequestPostProcessor recorderOnly = staffs.staff("usr_mp_r_" + suffix, "staff", projectA, null, RECORD);
        saveBounds(recorderOnly, projectA, 2, 8).andExpect(status().isForbidden());
        assertThat(rows()).isZero();
    }

    @Test
    void reasonRequired() throws Exception {
        saveBounds(operator, projectA, 1, 10).andExpect(status().isOk());
        recordPenalty(operator, "k-" + UUID.randomUUID(), projectA, merchantA, 3, "  ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(rows()).isZero();
    }

    @Test
    void idempotentReplayReturnsFirst() throws Exception {
        saveBounds(operator, projectA, 1, 10).andExpect(status().isOk());
        String key = "k-" + UUID.randomUUID();
        String first = body(recordPenalty(operator, key, projectA, merchantA, 3, "占道经营").andExpect(status().isOk()))
                .path("data").path("id").asText();
        String again = body(recordPenalty(operator, key, projectA, merchantA, 3, "占道经营").andExpect(status().isOk()))
                .path("data").path("id").asText();
        assertThat(again).isEqualTo(first);
        assertThat(rows()).isEqualTo(1);
    }

    // ---------- helpers ----------

    private void merchant(String id, String project) {
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at)"
                + " VALUES (?, '处罚记录测试商户', 'retail', ?, 'active', now(), now())", id, project);
    }

    private ResultActions saveBounds(RequestPostProcessor who, String project, Integer min, Integer max) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "mpb-" + UUID.randomUUID());
        body.put("min_penalty_points", min);
        body.put("max_penalty_points", max);
        return mockMvc.perform(put("/api/v1/merchant-penalty-bounds/" + project).with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions recordPenalty(RequestPostProcessor who, String key, String project, String merchant, int points,
            String reason) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("project_id", project);
        body.put("merchant_id", merchant);
        body.put("penalty_points", points);
        body.put("reason", reason);
        return mockMvc.perform(post("/api/v1/merchant-penalties").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private int rows() {
        return jdbc.queryForObject("SELECT count(*) FROM merchant_penalty_records WHERE project_id = ?", Integer.class, projectA);
    }

    private int audit(String event, String businessId) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = ? AND business_id = ?", Integer.class,
                event, businessId);
    }
}
