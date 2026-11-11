package com.complex.platform.amusement;

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
 * 第十八批 AG 段第 255 条 255-R3①（PGM:899「谁发起、谁销售、谁服务、谁第一个处理异常、为什么、谁审批、结果是什么，都要能查到。」；
 * 先例决策台账 18AE 198-R3「服务端操作人一律取登录人」）：游乐安全告知新建、告知确认，事故上报、追加处置、关闭，按蛇形键走真实 HTTP、
 * 真实授权链（平台账号，事故关闭的超空间准入闸对无商户上下文放行），落库的发布人、确认操作人、上报人、处置人、关闭人都是登录人；
 * 请求体另填的 operator_user_id、reported_by_user_id、closed_by_user_id 不采信，不填也不再 400。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AmusementSafetyOperatorJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String digits = String.valueOf(System.nanoTime());
    private final String userId = "usr_255_amu_" + digits;
    private final String projectId = "prj_255_amu_" + digits;

    @Test
    void noticeAndAcknowledgementRecordTheLoggedInOperator() throws Exception {
        RequestPostProcessor ops = ops("amusement.safety_notice.manage", "amusement.waiver.acknowledge");

        String noticeId = id(send(ops, "/api/v1/amusement/safety-notices", """
                {"idempotency_key":"notice-255-%s","project_id":"%s","notice_type":"safety_notice","title":"乘坐须知",
                 "content_version":"v1","content_text":"乘坐前请系好安全带","status":"active","operator_user_id":"someone_else"}
                """.formatted(digits, projectId)));
        send(ops, "/api/v1/amusement/safety-notices/" + noticeId + "/acknowledgements", """
                {"idempotency_key":"ack-255-%s","project_id":"%s","customer_id":"cus_255_%s","visitor_name":"张小游",
                 "signer_name":"张家长","acknowledgement_method":"paper_signature","content_version":"v1"}
                """.formatted(digits, projectId, digits))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT created_by_user_id FROM amusement_safety_notices WHERE id = ?", String.class, noticeId))
                .isEqualTo(userId);
        assertThat(jdbc.queryForList("SELECT operator_user_id FROM amusement_waiver_acknowledgements WHERE notice_id = ?",
                String.class, noticeId)).containsExactly(userId);
    }

    @Test
    void incidentReportActionAndCloseRecordTheLoggedInOperator() throws Exception {
        RequestPostProcessor ops = ops("amusement.incident.create", "amusement.incident.process", "amusement.incident.close");

        String withOthers = id(send(ops, "/api/v1/amusement/incidents", incident("a", ",\"reported_by_user_id\":\"someone_else\"")));
        String withoutKeys = id(send(ops, "/api/v1/amusement/incidents", incident("b", "")));
        action(ops, withOthers, "a", ",\"operator_user_id\":\"someone_else\"").andExpect(status().isOk());
        action(ops, withoutKeys, "b", "").andExpect(status().isOk());
        close(ops, withOthers, "a", ",\"closed_by_user_id\":\"someone_else\"").andExpect(status().isOk());
        close(ops, withoutKeys, "b", "").andExpect(status().isOk());

        assertThat(jdbc.queryForList("SELECT reported_by_user_id FROM amusement_incidents WHERE id IN (?, ?)",
                String.class, withOthers, withoutKeys)).containsExactly(userId, userId);
        assertThat(jdbc.queryForList("SELECT action_type || ':' || operator_user_id FROM amusement_incident_actions WHERE incident_id IN (?, ?)",
                String.class, withOthers, withoutKeys)).containsExactlyInAnyOrder(
                "created:" + userId, "created:" + userId, "status_changed:" + userId, "status_changed:" + userId,
                "closed:" + userId, "closed:" + userId);
    }

    /** 第十八批 AG 段 259-R6（OpenAPI 与代码一致：closeAmusementIncident 的 resolution 进 required）：关闭事故不带 resolution → 400「处理结论未填写」，事故不关。 */
    @Test
    void closeWithoutResolutionIsRejected() throws Exception {
        RequestPostProcessor ops = ops("amusement.incident.create", "amusement.incident.close");
        String incidentId = id(send(ops, "/api/v1/amusement/incidents", incident("c", "")));

        send(ops, "/api/v1/amusement/incidents/" + incidentId + "/close", """
                {"idempotency_key":"close-259-%s"}
                """.formatted(digits))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("处理结论未填写"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM amusement_incident_actions WHERE incident_id = ? AND action_type = 'closed'",
                Integer.class, incidentId)).isZero();
    }

    private RequestPostProcessor ops(String... permissions) {
        return new StaffRequestFixture(jdbc).staff(userId, "platform_admin", null, null, permissions);
    }

    private String incident(String tag, String extra) {
        return """
                {"idempotency_key":"incident-255-%s-%s","project_id":"%s","severity":"low","title":"游客滑倒",
                 "description":"入口处地面湿滑游客滑倒","reported_channel":"staff_app"%s}
                """.formatted(tag, digits, projectId, extra);
    }

    private ResultActions action(RequestPostProcessor who, String incidentId, String tag, String extra) throws Exception {
        return send(who, "/api/v1/amusement/incidents/" + incidentId + "/actions", """
                {"idempotency_key":"action-255-%s-%s","operator_note":"已到场处理","target_status":"processing"%s}
                """.formatted(tag, digits, extra));
    }

    private ResultActions close(RequestPostProcessor who, String incidentId, String tag, String extra) throws Exception {
        return send(who, "/api/v1/amusement/incidents/" + incidentId + "/close", """
                {"idempotency_key":"close-255-%s-%s","resolution":"已处置并回访"%s}
                """.formatted(tag, digits, extra));
    }

    private ResultActions send(RequestPostProcessor who, String path, String body) throws Exception {
        return mockMvc.perform(post(path).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String id(ResultActions created) throws Exception {
        return JsonPath.read(created.andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }
}
