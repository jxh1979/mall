package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 CC 段第 399 条 399-R1（BSM:784「入场、游乐（指定项目/次数/场次）……」；BSM:1033「通票中的酒水、小食和其他权益必须分别绑定实际提供方」；
 * BSM:1754「每项权益都必须绑定实际提供方和审批版本，避免悬空账。」）：游乐权益模板必须指定游乐项目；项目已登记经营商户的，提供方商户必须是该商户；
 * 项目没有登记经营商户的（游乐公司自营）沿用所选提供方。平台管理员按票务策略页同键（蛇形键）经真实入口与授权链建模板；游乐项目列表带出项目经营商户，
 * 后台据此按项目取提供方。修复前不指定项目照样建成、响应与库里都没有指定项目、项目列表不带经营商户：断言红。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class AmusementEntitlementTemplateProjectJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = String.valueOf(System.nanoTime());
    private final String project = "prj_c399_" + suffix;
    /** 项目 P 的经营商户 M1。 */
    private final String operator = "park_c399a_" + suffix;
    /** 另一家游乐商户 M2。 */
    private final String other = "park_c399b_" + suffix;
    /** 项目 P：经营商户为 M1。 */
    private final String ride = "amp_c399p_" + suffix;
    /** 项目 Q：没有登记经营商户（游乐公司自营）。 */
    private final String selfRun = "amp_c399q_" + suffix;
    private final String admin = "usr_c399_admin_" + suffix;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '游乐权益指定项目综合体', 'active', now(), now())", project);
        for (String[] m : List.of(new String[] {operator, "游乐权益指定项目甲乐园"}, new String[] {other, "游乐权益指定项目乙乐园"})) {
            jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                    + " VALUES (?, ?, 'amusement', 'active', ?, now(), now())", m[0], m[1] + suffix, project);
        }
        jdbc.update("INSERT INTO amusement_projects (id, project_name, capacity, open_status, eligibility_rule_json, merchant_id, created_at, updated_at)"
                + " VALUES (?, '指定项目过山车', 10, 'open', '{}'::jsonb, ?, now(), now())", ride, operator);
        jdbc.update("INSERT INTO amusement_projects (id, project_name, capacity, open_status, eligibility_rule_json, merchant_id, created_at, updated_at)"
                + " VALUES (?, '指定项目旋转木马', 10, 'open', '{}'::jsonb, NULL, now(), now())", selfRun);
        String role = "role_" + admin;
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", admin, admin);
        jdbc.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", role, "游乐权益指定项目配置" + suffix);
        for (String permission : List.of("ticket.entitlement.manage", "amusement.session.view")) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, permission);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", admin, role);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM entitlement_templates WHERE merchant_id IN (?, ?)", operator, other);
        jdbc.update("DELETE FROM amusement_projects WHERE id IN (?, ?)", ride, selfRun);
        jdbc.update("UPDATE projects SET status = 'suspended' WHERE id = ?", project);
    }

    /** ① 游乐权益不指定游乐项目：400，库里没有该模板。 */
    @Test
    void amusementTemplateWithoutProjectIsRejected() throws Exception {
        template(operator, null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(templates()).isZero();
    }

    /** ② 项目 P 经营商户为 M1：提供方填 M2 时 400 AMUSEMENT_PROJECT_MERCHANT_MISMATCH 不落库；填 M1 时建成，响应与库里都记 P。 */
    @Test
    void providerMustBeTheProjectsOperatingMerchant() throws Exception {
        template(other, ride).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("AMUSEMENT_PROJECT_MERCHANT_MISMATCH"));
        assertThat(templates()).isZero();
        String body = template(operator, ride).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amusement_project_id").value(ride))
                .andExpect(jsonPath("$.data.merchant_id").value(operator))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JsonPath.read(body, "$.data.id");
        assertThat(jdbc.queryForObject("SELECT amusement_project_id FROM entitlement_templates WHERE id = ?", String.class, id)).isEqualTo(ride);
    }

    /** ③ 项目 Q 没有登记经营商户：提供方照所选 M2 建成。 */
    @Test
    void selfRunProjectKeepsTheChosenProvider() throws Exception {
        template(other, selfRun).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amusement_project_id").value(selfRun))
                .andExpect(jsonPath("$.data.merchant_id").value(other));
    }

    /** ④ 游乐项目列表带出项目经营商户：P 为 M1。 */
    @Test
    void projectListCarriesTheOperatingMerchant() throws Exception {
        String body = mockMvc.perform(get("/api/v1/amusement/projects").with(session())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> merchants = JsonPath.read(body, "$.data.items[?(@.id == '" + ride + "')].merchant_id");
        assertThat(merchants).containsExactly(operator);
    }

    /**
     * 第十八批 CC 段 400-R1（第十八批裁决 83「游乐票的「一次」即一个营业日（场次）：当天首次入场扣一次」）：同一入口建游乐权益模板，库里消耗窗口为按营业日计次
     * （unlimited_in_day）；建入园模板仍逐次扣（per_use）。修复前游乐模板也落 per_use：第一条断言红。
     */
    @Test
    void amusementTemplateCountsByBusinessDayOthersStayPerUse() throws Exception {
        assertThat(consumptionWindow(template(operator, ride))).isEqualTo("unlimited_in_day");
        assertThat(consumptionWindow(template("entry", operator, null))).isEqualTo("per_use");
    }

    /** 按票务策略页「新建权益模板」的请求形状（蛇形键）建游乐权益模板。 */
    private ResultActions template(String merchant, String amusementProject) throws Exception {
        return template("amusement", merchant, amusementProject);
    }

    /** 400-R1：同一请求形状，权益类型由参数给。 */
    private ResultActions template(String type, String merchant, String amusementProject) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/entitlement-templates").with(session()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"idempotency_key": "c399-tpl-%s", "name": "指定项目游乐权益%s", "entitlement_type": "%s",
                         "default_total_times": 2, "default_value_cent": 0, "status": "active", "merchant_id": "%s",
                         "amusement_project_id": %s}
                        """.formatted(UUID.randomUUID(), suffix, type, merchant, amusementProject == null ? "null" : "\"" + amusementProject + "\"")));
    }

    /** 400-R1：建成的模板在库里的消耗窗口。 */
    private String consumptionWindow(ResultActions created) throws Exception {
        String body = created.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JsonPath.read(body, "$.data.id");
        return jdbc.queryForObject("SELECT consumption_window FROM entitlement_templates WHERE id = ?", String.class, id);
    }

    private int templates() {
        return jdbc.queryForObject("SELECT count(*) FROM entitlement_templates WHERE merchant_id IN (?, ?)", Integer.class, operator, other);
    }

    /** 后台 Web 终端 + 生产过滤器放入的 AccessActor 主体（与账号表当前行一致）。 */
    private RequestPostProcessor session() {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(
                    new AccessActor(admin, admin, "platform_admin", "org_1", null, "active"), null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
