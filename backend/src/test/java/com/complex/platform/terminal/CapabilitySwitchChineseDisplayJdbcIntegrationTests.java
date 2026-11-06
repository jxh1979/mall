package com.complex.platform.terminal;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 W 段第 194 条（真库、真实入口与授权链，请求按 adminClient 蛇形键）。依据 PGM:287「……展示给用户的一切文字，包括……字段名、状态、
 * 枚举取值……必须全部为中文……可以不用中文的只有四样，没有第五样：单据号、设备编号、二维码内容、系统内部标识符。」
 * 194-R1：开关列表每行带能力中文名 capability_name（取权限中文名，非权限类能力为空）。
 * 194-R3：终端开关矩阵「说明」列显示的种子说明（V071、V088、V093、V096～V098、V146、V177、V198、V545、V554、V595、V600）由 V605 改为中文。
 * 194-R4：后台切开关时本次操作说明以 operator_note 提交，写进该行说明（reason）。
 * 194 同根（控制席并入）：不带 operator_note 切开关时，该行原说明不被写成空值。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class CapabilitySwitchChineseDisplayJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private String userId;
    private String suffix;

    @BeforeEach
    void seedAdmin() {
        suffix = String.valueOf(System.nanoTime());
        userId = "usr_b43_194_" + suffix;
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', NULL, NULL, 'active', now())", userId, userId);
        String role = "role_b43_194_" + suffix;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at)"
                + " VALUES (?, ?, '终端开关中文显示回归', 'platform', 'active', now())", role, "终端开关中文显示测试角色" + suffix);
        jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'terminal.switch.view'), (?, 'terminal.switch.manage')",
                role, role);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
    }

    @Test
    void listCarriesPermissionChineseNameForPermissionCapabilities() throws Exception {
        String body = mockMvc.perform(get("/api/v1/terminals/capability-switches").with(admin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Map<String, JsonNode> globalRows = new HashMap<>();
        for (JsonNode row : json.readTree(body).path("data").path("items")) {
            if ("*".equals(row.path("terminal_type").asText()) && "global".equals(row.path("scope_type").asText())) {
                globalRows.put(row.path("capability_code").asText(), row);
            }
        }
        for (String code : List.of("bar.reservation.create", "bar.bill.hang")) {
            assertThat(globalRows).containsKey(code);
            String name = globalRows.get(code).path("capability_name").asText("");
            assertThat(name).as(code).isNotBlank().doesNotContainPattern("[A-Za-z]");
            assertThat(name).isEqualTo(jdbc.queryForObject(
                    "SELECT permission_name FROM permissions WHERE permission_code = ?", String.class, code));
        }
        assertThat(globalRows).containsKey("sms_notification");
        assertThat(globalRows.get("sms_notification").path("capability_name").isNull()
                || globalRows.get("sms_notification").path("capability_name").isMissingNode()).isTrue();
    }

    @Test
    void toggleWritesOperatorNoteIntoReasonAndUpdateResponseKeepsItsFields() throws Exception {
        String id = "tcs_b43_194_" + suffix;
        String device = "B43-194-" + suffix;
        jdbc.update("INSERT INTO terminal_capability_switches (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason,"
                + " created_at, updated_at) VALUES (?, 'kiosk', 'ticketing', 'device', ?, FALSE, '关闭本机售票', now(), now())", id, device);
        // TerminalSwitchMatrixPage.toggle：api.put(…/{id}, { enabled, operatorNote })，adminClient 转蛇形键并带幂等键。
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("enabled", true);
        request.put("operator_note", "后台打开：触摸屏一体机 / 售票能力");
        request.put("idempotency_key", "b43-194-" + suffix);
        mockMvc.perform(put("/api/v1/terminals/capability-switches/" + id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.terminal_type").value("kiosk"))
                .andExpect(jsonPath("$.data.capability_code").value("ticketing"))
                .andExpect(jsonPath("$.data.scope_type").value("device"))
                .andExpect(jsonPath("$.data.scope_id").value(device))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.reason").value("后台打开：触摸屏一体机 / 售票能力"));
        assertThat(jdbc.queryForObject("SELECT reason FROM terminal_capability_switches WHERE id = ?", String.class, id))
                .isEqualTo("后台打开：触摸屏一体机 / 售票能力");
        // 同一幂等键重放：按记录类型回读第一次的结果（记录多了能力中文名组件后仍能回读）。
        mockMvc.perform(put("/api/v1/terminals/capability-switches/" + id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.reason").value("后台打开：触摸屏一体机 / 售票能力"));
    }

    /**
     * 194 同根（控制席并入）：按短信开关页、第三方开关页修复前的请求形状（不带 operator_note）切开关，该行原说明不变。
     * 修复前既有行按 trimToNull(operatorNote) 覆盖说明：说明断言红。
     */
    @Test
    void toggleWithoutOperatorNoteKeepsExistingReason() throws Exception {
        String id = "tcs_b43_l1_" + suffix;
        String device = "B43-L1-" + suffix;
        jdbc.update("INSERT INTO terminal_capability_switches (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason,"
                + " created_at, updated_at) VALUES (?, 'kiosk', 'ticketing', 'device', ?, FALSE, '关闭本机售票', now(), now())", id, device);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("enabled", true);
        request.put("terminal_type", "kiosk");
        request.put("capability_code", "ticketing");
        request.put("scope_id", device);
        request.put("idempotency_key", "b43-l1-" + suffix);
        mockMvc.perform(put("/api/v1/terminals/capability-switches/" + id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.reason").value("关闭本机售票"));
        assertThat(jdbc.queryForObject("SELECT reason FROM terminal_capability_switches WHERE id = ?", String.class, id))
                .isEqualTo("关闭本机售票");
    }

    /** 种子行自迁移写入后没被切过（created_at = updated_at）时，说明里除 HTML 自身的终端名「客人 App」「商户 App」「Web 前端」外不得有英文。 */
    @Test
    void seededSwitchDescriptionsAreChinese() {
        List<Map<String, Object>> english = jdbc.queryForList("""
                SELECT id, reason FROM terminal_capability_switches
                 WHERE created_at = updated_at
                   AND replace(replace(replace(reason, '客人 App', ''), '商户 App', ''), 'Web 前端', '') ~ '[A-Za-z]'
                 ORDER BY id
                """);
        assertThat(english).isEmpty();
        assertThat(jdbc.queryForObject("SELECT reason FROM terminal_capability_switches WHERE id = 'tcs_web_mod_overview'", String.class))
                .isEqualTo("网页后台·总览模块");
    }

    private RequestPostProcessor admin() {
        AccessActor actor = new AccessActor(userId, userId, "platform_admin", null, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of())).postProcessRequest(request);
        };
    }
}
