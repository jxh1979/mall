package com.complex.platform.terminal;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * OBS-13-04 不可关闭的短信（真库）。依据：BSM 第 1598 行「管理员可在后台自行决定哪些提醒发短信。但下列三类不在可关闭范围内，不得按类或按条关闭：
 * 涉及商户应收应付金额发生变化的通知、涉及客人金额发生变化的通知（储值消费扣款、储值充值到账、消费扣款、定金没收、定金原路退回）、存酒到期提醒」；
 * 第三方接入 PRD §11A.4「尝试按类或按条关闭一律拒绝并给出中文报错」。请求按后台短信通知开关页的真实形状（adminClient 转蛇形键并带幂等键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class SmsMandatorySwitchJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private String userId;

    @BeforeEach
    void seedAdmin() {
        userId = "usr_sms_mand_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', NULL, NULL, 'active', now())", userId, userId);
        String role = "role_" + userId;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at)"
                + " VALUES (?, ?, '不可关闭短信回归', 'platform', 'active', now())", role, "不可关闭短信测试角色" + System.nanoTime());
        jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'terminal.switch.manage')", role);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
    }

    @AfterEach
    void restoreSwitches() {
        jdbc.update("UPDATE terminal_capability_switches SET enabled = TRUE WHERE id IN"
                + " ('tcs_sms_notify_stored_value_topup', 'tcs_sms_notify_merchant_fee_rent', 'tcs_sms_notify_stored_wine_expired')");
    }

    @Test
    void closingMandatorySmsSwitchIsRejectedWithChineseMessageAndFailureAudit() throws Exception {
        String key = "sms-mand-" + UUID.randomUUID();
        toggle("tcs_sms_notify_stored_value_topup", "sms.notify.stored_value.topup", key)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("SMS_NOTIFICATION_NOT_CLOSABLE"))
                .andExpect(jsonPath("$.message").value("该短信提醒属于不可关闭的通知，不能关闭"));
        assertThat(enabled("tcs_sms_notify_stored_value_topup")).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'terminal_switch.change_failed'"
                + " AND idempotency_key = ?", Integer.class, key)).isEqualTo(1);

        toggle("tcs_sms_notify_merchant_fee_rent", "sms.notify.merchant_fee.rent", "sms-mand-" + UUID.randomUUID())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("SMS_NOTIFICATION_NOT_CLOSABLE"));
        assertThat(enabled("tcs_sms_notify_merchant_fee_rent")).isTrue();
    }

    @Test
    void closableSmsSwitchStillCloses() throws Exception {
        toggle("tcs_sms_notify_stored_wine_expired", "sms.notify.stored_wine.expired", "sms-mand-" + UUID.randomUUID())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
        assertThat(enabled("tcs_sms_notify_stored_wine_expired")).isFalse();
    }

    /** SmsNotificationSwitchesPage.toggle：api.put(…/{id}, { enabled, terminalType, capabilityCode, scopeId })。 */
    private ResultActions toggle(String id, String capabilityCode, String key) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", false);
        body.put("terminal_type", "*");
        body.put("capability_code", capabilityCode);
        body.put("scope_id", "global");
        body.put("idempotency_key", key);
        AccessActor actor = new AccessActor(userId, userId, "platform_admin", null, null, "active");
        return mockMvc.perform(put("/api/v1/terminals/capability-switches/" + id)
                .with(request -> {
                    request.addHeader("X-Terminal-Type", "web");
                    return authentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of()))
                            .postProcessRequest(request);
                })
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private boolean enabled(String id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT enabled FROM terminal_capability_switches WHERE id = ?", Boolean.class, id));
    }
}
