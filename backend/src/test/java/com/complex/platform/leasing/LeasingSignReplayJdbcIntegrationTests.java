package com.complex.platform.leasing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
 * 第十八批 BG 段第 261 条 261-B20-R1（真库、真实入口、真实授权链、蛇形键）。依据：PGM:528「⑤同一笔操作重复提交不重复生效（重复点击、网络重发、
 * 渠道重复回调都只算一次）」；PGM:303「同一次操作重复提交时，返回第一次的结果并提示已完成。」。招商签约按意向认出已签约：同一请求编号重发、
 * 或别人换新编号再签同一意向，都返回首次起草的合同并提示已完成，不再建商户与绑定、不重复留痕；首次签约不带已完成标记。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class LeasingSignReplayJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcLeasingRepository leasingRepository;

    private final String s = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "prj_b20_" + s;
    private final String subject = "be_b20_" + s;
    private final String intentId = "intent_b20_" + s;
    private RequestPostProcessor salesA;
    private RequestPostProcessor salesB;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES (?, 'complex', '签约重发回归综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) "
                + "VALUES (?, ?, 'management_company', 'active', now(), now())", subject, "签约重发回归合同主体" + s);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES (?, ?)", subject, project);
        String space = "sp_b20_" + s;
        jdbc.update("INSERT INTO spaces (id, project_id, merchant_id, parent_space_id, space_name, space_type, floor_no, area_square_meter, status, "
                + "created_at, updated_at) VALUES (?, ?, NULL, NULL, ?, 'shop', '1F', 88.0, 'active', now(), now())", space, project, "签约重发回归铺位");
        Instant now = Instant.now();
        String lead = "lead_b20_" + s;
        leasingRepository.saveLead(new LeasingLeadRecord(lead, "回归联系人", "13800002222", "referral",
                "retail", space, null, null, "usr_sales", "negotiating", null, null, project, now, now));
        leasingRepository.saveIntent(new LeasingIntentRecord(intentId, lead, "pending_signing", space,
                null, Map.of(), now, "usr_supervisor", project, now, now));
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        salesA = staffs.staff("usr_b20_a_" + s, "platform_admin", project, null, "leasing.convert");
        salesB = staffs.staff("usr_b20_b_" + s, "platform_admin", project, null, "leasing.convert");
    }

    @Test
    void resignReturnsTheFirstContractAndTracesOnce() throws Exception {
        String body = body("b20-sign-" + s);
        String first = sign(salesA, body).andExpect(status().isOk()).andExpect(jsonPath("$.replayed").doesNotExist())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String contractId = JsonPath.read(first, "$.data.id");
        int merchants = count("merchants WHERE project_id = ?", project);
        int contracts = count("contracts WHERE contract_subject_id = ?", subject);
        assertThat(signedEvents()).isEqualTo(1);

        // 同一请求编号重发
        sign(salesA, body).andExpect(status().isOk()).andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.message").value(ApiResponse.REPLAYED_MESSAGE))
                .andExpect(jsonPath("$.data.id").value(contractId));
        // 另一位招商员换新编号再签同一意向
        sign(salesB, body("b20-sign-again-" + s)).andExpect(status().isOk()).andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.data.id").value(contractId));

        assertThat(signedEvents()).isEqualTo(1);
        assertThat(count("merchants WHERE project_id = ?", project)).isEqualTo(merchants);
        assertThat(count("contracts WHERE contract_subject_id = ?", subject)).isEqualTo(contracts).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT contract_id FROM leasing_intents WHERE id = ?", String.class, intentId)).isEqualTo(contractId);
    }

    private String body(String key) {
        return "{\"idempotency_key\":\"" + key + "\",\"contract_subject_id\":\"" + subject
                + "\",\"effective_date\":\"2026-12-01\",\"expiry_date\":\"2027-11-30\"}";
    }

    private ResultActions sign(RequestPostProcessor who, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/leasing/intents/" + intentId + "/sign").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int signedEvents() {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='leasing.intent.signed' AND business_id=?",
                Integer.class, intentId);
    }

    private int count(String fromWhere, String arg) {
        return jdbc.queryForObject("SELECT count(*) FROM " + fromWhere, Integer.class, arg);
    }
}
