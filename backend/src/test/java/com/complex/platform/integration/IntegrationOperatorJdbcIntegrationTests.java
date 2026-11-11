package com.complex.platform.integration;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
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
 * 第十八批 AG 段第 255 条 255-R3②③（PGM:899「谁发起、谁销售、谁服务、谁第一个处理异常、为什么、谁审批、结果是什么，都要能查到。」；
 * 先例决策台账 18AE 198-R3「服务端操作人一律取登录人」）与第 260 条（第十八批裁决 49）：按蛇形键走真实 HTTP、真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class IntegrationOperatorJdbcIntegrationTests {

    private static final String BASE = "/api/v1/integration";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String digits = String.valueOf(System.nanoTime());
    private final String userId = "usr_255_int_" + digits;
    private final String registrantId = "usr_255_reg_" + digits;

    /** 255-R3③：补偿任务处置人取登录人；请求体另填的 operator_user_id 不采信，不填也不再 400。 */
    @Test
    void compensationTaskResolveRecordsTheLoggedInOperator() throws Exception {
        RequestPostProcessor ops = ops("integration.compensation.manage");
        String withOther = createCompensationTask(ops, "a");
        String withoutKey = createCompensationTask(ops, "b");

        resolveTask(ops, withOther, "a", ",\"operator_user_id\":\"someone_else\"").andExpect(status().isOk());
        resolveTask(ops, withoutKey, "b", "").andExpect(status().isOk());

        assertThat(jdbc.queryForList("SELECT operator_user_id FROM integration_compensation_tasks WHERE id IN (?, ?)",
                String.class, withOther, withoutKey)).containsExactly(userId, userId);
    }

    /** 255-R3②：对账差异处置人落登录人（此前落请求体）；登记人自审仍 403，且不带 operator_user_id 时报的是自审而不是缺字段。 */
    @Test
    void reconciliationDifferenceResolveRecordsTheLoggedInOperator() throws Exception {
        String batchId = "irb_255_" + digits;
        String differenceId = "ird_255_" + digits;
        jdbc.update("INSERT INTO integration_reconciliation_batches (id, provider_code, business_type, period_start, period_end, status,"
                + " created_at, updated_at) VALUES (?, 'mock-255', 'catering', DATE '2026-06-14', DATE '2026-06-14', 'open', now(), now())",
                batchId);
        jdbc.update("INSERT INTO integration_reconciliation_differences (id, batch_id, provider_code, business_type, external_business_id,"
                + " difference_type, status, amount_source, registered_by_user_id, created_at, updated_at)"
                + " VALUES (?, ?, 'mock-255', 'catering', ?, 'amount_mismatch', 'needs_review', 'manual_registration', ?, now(), now())",
                differenceId, batchId, "ext-255-" + digits, registrantId);
        RequestPostProcessor registrant = new StaffRequestFixture(jdbc).staff(registrantId, "staff", null, null,
                "integration.reconciliation.resolve");

        resolveDifference(registrant, differenceId, "self", "")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SELF_REVIEW_FORBIDDEN"));
        resolveDifference(ops("integration.reconciliation.resolve"), differenceId, "ok", ",\"operator_user_id\":\"someone_else\"")
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT assigned_to_user_id FROM integration_reconciliation_differences WHERE id = ?",
                String.class, differenceId)).isEqualTo(userId);
    }

    /** 第 260 条（第十八批裁决 49）：回流登记不带外部渠道订单号也能登记，投影里不放空值键。 */
    @Test
    void orderSyncWithoutExternalChannelOrderIdIsRegistered() throws Exception {
        String externalBusinessId = "ext-260-" + digits;
        mockMvc.perform(post(BASE + "/order-syncs").with(ops("integration.order_sync.manage"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key":"order-sync-260-%s","provider_code":"provider-260-%s","business_type":"catering",
                                 "sync_type":"order","external_business_id":"%s","status":"received","amount_cent":12800}
                                """.formatted(digits, digits, externalBusinessId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.external_business_id").value(externalBusinessId));

        assertThat(jdbc.queryForMap("""
                SELECT external_channel_order_id,
                       provider_payload_json->'standard_model'->'external_channel_order_id' IS NULL AS key_absent,
                       provider_payload_json->'standard_model'->>'external_business_id' AS projected
                  FROM integration_order_syncs WHERE external_business_id = ?
                """, externalBusinessId))
                .containsEntry("external_channel_order_id", null)
                .containsEntry("key_absent", true)
                .containsEntry("projected", externalBusinessId);
    }

    /** 第十八批 AG 段 259-R6（OpenAPI 与代码一致：createOrderSync 的 sync_type、status 进 required；第十八批裁决 49）：回流登记不带 sync_type → 400「同步类型未填写」，不落回流记录。 */
    @Test
    void orderSyncWithoutSyncTypeIsRejected() throws Exception {
        String externalBusinessId = "ext-259-" + digits;
        mockMvc.perform(post(BASE + "/order-syncs").with(ops("integration.order_sync.manage"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key":"order-sync-259-%s","provider_code":"provider-259-%s","business_type":"catering",
                                 "external_business_id":"%s","status":"received","amount_cent":12800}
                                """.formatted(digits, digits, externalBusinessId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("同步类型未填写"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM integration_order_syncs WHERE external_business_id = ?", Integer.class,
                externalBusinessId)).isZero();
    }

    private RequestPostProcessor ops(String... permissions) {
        return new StaffRequestFixture(jdbc).staff(userId, "staff", null, null, permissions);
    }

    private String createCompensationTask(RequestPostProcessor who, String tag) throws Exception {
        return JsonPath.read(mockMvc.perform(post(BASE + "/compensation-tasks").with(who).contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key":"compensation-255-%s-%s","provider_code":"mock-255","task_type":"manual_adjustment",
                         "business_type":"catering","external_business_id":"ext-255-%s-%s","status":"open"}
                        """.formatted(tag, digits, tag, digits)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    private ResultActions resolveTask(RequestPostProcessor who, String taskId, String tag, String extra) throws Exception {
        return mockMvc.perform(post(BASE + "/compensation-tasks/" + taskId + "/resolve").with(who)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key":"compensation-resolve-255-%s-%s","status":"resolved","resolution_note":"已人工补账"%s}
                        """.formatted(tag, digits, extra)));
    }

    private ResultActions resolveDifference(RequestPostProcessor who, String differenceId, String tag, String extra) throws Exception {
        return mockMvc.perform(post(BASE + "/reconciliation/differences/" + differenceId + "/resolve").with(who)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key":"difference-resolve-255-%s-%s","status":"resolved","resolution_note":"复核通过"%s}
                        """.formatted(tag, digits, extra)));
    }
}
