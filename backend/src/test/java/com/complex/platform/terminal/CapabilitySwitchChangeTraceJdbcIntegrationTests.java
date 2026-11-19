package com.complex.platform.terminal;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BA 段第 270 条 270-R1（PGM:739「开关配置的每一次变更——包括模块级开关——都必须记录变更人、变更时间、变更前后的取值与影响范围。」；
 * PGM:742「任何能力在后台没有显式开关配置行时一律默认关闭（opt-in）」）：后台切能力开关、开关灰度试点，成功事件元数据记变更前后的取值与影响范围
 * （端、能力、范围类型、范围）。真库、真实入口与授权链，请求按 adminClient 蛇形键；夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class CapabilitySwitchChangeTraceJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String suffix = String.valueOf(System.nanoTime());
    private final String userId = "usr_b73_270_" + suffix;

    @BeforeEach
    void seedAdmin() {
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', NULL, NULL, 'active', now())", userId, userId);
        String role = "role_b73_270_" + suffix;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at)"
                + " VALUES (?, ?, '开关变更留痕回归', 'platform', 'active', now())", role, "开关变更留痕测试角色" + suffix);
        jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'terminal.switch.view'), (?, 'terminal.switch.manage')",
                role, role);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
    }

    /** 终端开关矩阵页切开关（TerminalSwitchMatrixPage.toggle：{ enabled, operatorNote } 带幂等键）：关一条开着的行、再打开，各记变更前后的取值。 */
    @Test
    void toggleRecordsPreviousAndNewValueWithScope() throws Exception {
        String id = "tcs_b73_270_" + suffix;
        String device = "B73-270-" + suffix;
        jdbc.update("INSERT INTO terminal_capability_switches (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason,"
                + " created_at, updated_at) VALUES (?, 'kiosk', 'ticketing', 'device', ?, TRUE, '本机售票', now(), now())", id, device);

        toggle(id, Map.of("idempotency_key", "b73-270-off-" + suffix, "enabled", false, "operator_note", "试运行关闭"));
        assertThat(changes(id)).containsExactly("true→false");
        assertThat(jdbc.queryForMap("""
                SELECT metadata->>'terminal_type' AS terminal_type, metadata->>'capability_code' AS capability_code,
                       metadata->>'scope_type' AS scope_type, metadata->>'scope_id' AS scope_id, actor_id
                  FROM audit_events WHERE event_name = 'terminal_switch.changed' AND business_id = ?
                """, id)).containsEntry("terminal_type", "kiosk").containsEntry("capability_code", "ticketing")
                .containsEntry("scope_type", "device").containsEntry("scope_id", device).containsEntry("actor_id", userId);

        toggle(id, Map.of("idempotency_key", "b73-270-on-" + suffix, "enabled", true, "operator_note", "试运行打开"));
        assertThat(changes(id)).containsExactlyInAnyOrder("true→false", "false→true");
    }

    /** 设备页一体机售票开关（无开关行时按端、能力、设备号新建）：缺行即默认关闭，变更前取值记关闭。 */
    @Test
    void creatingSwitchRowRecordsPreviousAsClosed() throws Exception {
        String id = "tcs_b73_270n_" + suffix;
        toggle(id, Map.of("idempotency_key", "b73-270-new-" + suffix, "enabled", true, "terminal_type", "kiosk",
                "capability_code", "ticketing", "scope_id", "B73-270N-" + suffix));
        assertThat(changes(id)).containsExactly("false→true");
    }

    /** 终端开关矩阵页开、关灰度试点（PGM:736 灰度是开关最细的一档）：结束试点的事件记变更前为开启。 */
    @Test
    void canaryRolloutChangeRecordsPreviousActive() throws Exception {
        String capability = "b73_270_canary_" + suffix;
        rollout(Map.of("terminal_type", "kiosk", "capability_code", capability, "active", true, "reason", "灰度试点开始"));
        rollout(Map.of("terminal_type", "kiosk", "capability_code", capability, "active", false, "reason", "灰度试点结束"));
        assertThat(jdbc.queryForList("""
                SELECT (metadata->>'previous_active') || '→' || (metadata->>'active') FROM audit_events
                 WHERE event_name = 'terminal_canary.rollout_changed' AND metadata->>'capability' = ?
                """, String.class, capability)).containsExactlyInAnyOrder("false→true", "true→false");
    }

    private void toggle(String id, Map<String, Object> body) throws Exception {
        mockMvc.perform(put("/api/v1/terminals/capability-switches/" + id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    private void rollout(Map<String, Object> body) throws Exception {
        mockMvc.perform(put("/api/v1/terminals/canary-rollouts").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    /** 该开关行各次成功变更的「变更前→变更后」。 */
    private List<String> changes(String id) {
        return jdbc.queryForList("""
                SELECT (metadata->>'previous_enabled') || '→' || (metadata->>'enabled') FROM audit_events
                 WHERE event_name = 'terminal_switch.changed' AND business_id = ?
                """, String.class, id);
    }

    private RequestPostProcessor admin() {
        AccessActor actor = new AccessActor(userId, userId, "platform_admin", null, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of())).postProcessRequest(request);
        };
    }
}
