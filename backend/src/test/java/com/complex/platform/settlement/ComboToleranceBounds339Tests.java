package com.complex.platform.settlement;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * 第十八批 AW 段 339-R1（BSM:754「允许误差可配（业务参数，全平台统一可配，出厂默认 5 元，须同时配置上下限）」；ENUM:193 同句）：
 * 通票凑整参数只有全平台一行，误差与上下限同填、0 ≤ 下限 ≤ 误差 ≤ 上限；293-R1（BSM:754「与折扣一并分摊到各域，由各域主管公司按份额
 * 各自承担或取得」）：不再有套票折扣归属，请求体不带它、总览不再显示它。结算配置页真实请求（蛇形键）、真实授权链，草案与转正随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class ComboToleranceBounds339Tests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private com.complex.platform.common.BusinessDayResolver businessDays;
    @Autowired private SettlementConfigScheduledActivationService activation;

    private final String s = String.valueOf(System.nanoTime());
    private String project;
    private RequestPostProcessor drafter;
    private RequestPostProcessor reviewer;

    @BeforeEach
    void seed() {
        project = "prj_c339_" + s;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '凑整误差综合体', 'active', now(), now())", project);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        drafter = staffs.staff("usr_c339_a_" + s, "staff", project, null, "settlement.config.manage", "settlement.config.view");
        reviewer = staffs.staff("usr_c339_b_" + s, "staff", project, null, "settlement.config.manage", "settlement.config.view");
    }

    @Test
    void projectScopedRoundingParameters_400() throws Exception {
        save(body("a0", 500L, 0L, 1000L)).andExpect(status().isOk());
        Map<String, Object> body = body("a", 500L, 0L, 1000L);
        body.put("project_id", project);
        save(body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    @Test
    void toleranceWithoutBounds_400() throws Exception {
        save(body("b0", 500L, 0L, 1000L)).andExpect(status().isOk());
        save(body("b", 500L, null, null)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    @Test
    void toleranceOutsideItsBounds_400() throws Exception {
        save(body("c0", 500L, 500L, 500L)).andExpect(status().isOk());
        save(body("c", 500L, 600L, 1000L)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        save(body("d", 500L, 0L, 400L)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    @Test
    void boundedTolerance_activated_showsBoundsAndNoDiscountBearerInOverview() throws Exception {
        save(body("e", 300L, 100L, 800L)).andExpect(status().isOk());
        String draftId = jdbc.queryForObject("SELECT id FROM ticket_combo_discount_configs"
                + " WHERE project_id IS NULL AND is_draft = TRUE AND status = 'active'", String.class);
        mockMvc.perform(post("/api/v1/settlement/config/combo_discount/" + draftId + "/activate").with(reviewer))
                .andExpect(status().isOk());
        activation.activateDue(java.time.Instant.now().plus(java.time.Duration.ofDays(1)));   // 371-R1：批准后待生效，到次日转正

        String overview = mockMvc.perform(get("/api/v1/settlement/config/overview").with(reviewer))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> tolerance = JsonPath.read(overview, "$.data.platform_defaults[?(@.key == 'rounding_tolerance_cent')].value");
        assertThat(tolerance).containsExactly("3 元（下限 1 元～上限 8 元）");
        List<String> bearer = JsonPath.read(overview, "$.data.platform_defaults[?(@.key == 'combo_discount_bearer')].value");
        assertThat(bearer).as("不再有套票折扣归属").isEmpty();
    }

    private Map<String, Object> body(String key, Long tolerance, Long min, Long max) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "c339-" + key + "-" + s);
        // 第十八批 BQ 段第 371 条 371-R1：生效起始日期必填（全平台一行按平台默认营业日，次日）。
        body.put("effective_from", businessDays.businessDate(null, null, java.time.Instant.now()).plusDays(1).toString());
        body.put("rounding_tolerance_cent", tolerance);
        if (min != null) {
            body.put("rounding_tolerance_min_cent", min);
        }
        if (max != null) {
            body.put("rounding_tolerance_max_cent", max);
        }
        return body;
    }

    private ResultActions save(Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/v1/settlement/config/combo-discount").with(drafter)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
}
