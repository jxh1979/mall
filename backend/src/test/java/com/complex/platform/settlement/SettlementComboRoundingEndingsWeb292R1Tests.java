package com.complex.platform.settlement;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.complex.platform.ticketing.ComboPriceToleranceConfigPort;
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
 * 第十八批 AT 段 292-R1（BSM:754「尾数落在后台配置的允许尾数集合内——业务参数，全平台统一可配，出厂默认 8、9、0」）：
 * 结算配置页「通票分账」全平台默认里提交允许尾数，经另一位管理人员复核转正后，总览显示、票务凑整建议按它算（只认全平台默认行）；
 * 每项须为 0～9、至少一个；不填为出厂 8、9、0。真实 HTTP、真实授权链、蛇形键，草案与转正随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SettlementComboRoundingEndingsWeb292R1Tests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private ComboPriceToleranceConfigPort roundingPort;
    @Autowired private com.complex.platform.common.BusinessDayResolver businessDays;
    @Autowired private SettlementConfigScheduledActivationService activation;

    private final String s = String.valueOf(System.nanoTime());
    private String project;
    private RequestPostProcessor drafter;
    private RequestPostProcessor reviewer;

    @BeforeEach
    void seed() {
        project = "prj_b66end_" + s;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '凑整尾数综合体', 'active', now(), now())", project);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        drafter = staffs.staff("usr_b66end_a_" + s, "staff", project, null, "settlement.config.manage", "settlement.config.view");
        reviewer = staffs.staff("usr_b66end_b_" + s, "staff", project, null, "settlement.config.manage", "settlement.config.view");
    }

    @Test
    void platformDefaultEndings_activated_showInOverview_andDriveRoundingForAnyProject() throws Exception {
        save(List.of(5), "a").andExpect(status().isOk());
        activateDraft();

        assertThat(overviewValue()).isEqualTo("5");
        assertThat(roundingPort.findComboRounding().allowedEndings()).isEqualTo("5");
    }

    @Test
    void endingsOmitted_factoryEightNineZero() throws Exception {
        save(null, "b").andExpect(status().isOk());
        activateDraft();

        assertThat(overviewValue()).isEqualTo("0、8、9");
    }

    @Test
    void endingsOutOfRangeOrEmpty_400() throws Exception {
        save(List.of(10), "c").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        save(List.of(), "d").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    private ResultActions save(List<Integer> endings, String key) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "b66end-" + key + "-" + s);
        // 第十八批 AW 段 293-R1、339-R1：不再有套票折扣归属；误差与上下限同填。
        body.put("rounding_tolerance_cent", 500);
        body.put("rounding_tolerance_min_cent", 0);
        body.put("rounding_tolerance_max_cent", 1000);
        // 第十八批 BQ 段第 371 条 371-R1：生效起始日期必填（全平台一行按平台默认营业日，次日）。
        body.put("effective_from", businessDays.businessDate(null, null, java.time.Instant.now()).plusDays(1).toString());
        if (endings != null) {
            body.put("rounding_allowed_endings", endings);
        }
        return mockMvc.perform(post("/api/v1/settlement/config/combo-discount").with(drafter)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private void activateDraft() throws Exception {
        String draftId = jdbc.queryForObject("SELECT id FROM ticket_combo_discount_configs"
                + " WHERE project_id IS NULL AND is_draft = TRUE AND status = 'active'", String.class);
        mockMvc.perform(post("/api/v1/settlement/config/combo_discount/" + draftId + "/activate").with(reviewer))
                .andExpect(status().isOk());
        activation.activateDue(java.time.Instant.now().plus(java.time.Duration.ofDays(1)));   // 371-R1：批准后待生效，到次日转正
    }

    private String overviewValue() throws Exception {
        String body = mockMvc.perform(get("/api/v1/settlement/config/overview").with(reviewer))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> values = JsonPath.read(body, "$.data.platform_defaults[?(@.key == 'rounding_allowed_endings')].value");
        assertThat(values).hasSize(1);
        return values.get(0);
    }
}
