package com.complex.platform.governance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十批 TK-27（BSM L1058「待清台超过后台配置时限（业务参数，按酒吧可配，出厂默认 15 分钟，须同时配置上下限）未恢复空闲的，系统提醒当班主管」；
 * 控制席技术决定第二轮：按仓库「值与上下限同配」范式——未配置用出厂值 15；配置时值、下限、上限三者同填且值在闭区间，否则 400）。
 * 请求体按前端真实形状（adminClient.mapKeys 蛇形键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MerchantPendingClearAlertJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "pca_prj_" + suffix;
    private final String bar = "pca_bar_" + suffix;
    private RequestPostProcessor manager;

    @BeforeEach
    void seed() {
        String company = "pca_be_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','提醒综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())", company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at) VALUES(?,'提醒酒吧','bar',?,?,?,?,'active',now(),now())",
                bar, company, company, company, project);
        String user = "pca_mgr_" + suffix;
        String role = "role_" + user;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'待清台提醒回归','project','active',now())", role, role);
        for (String permission : List.of("merchant.profile.view", "merchant.profile.manage")) {
            jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)", role, permission);
        }
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,NULL,'active',now())",
                user, user, project);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user, role);
        AccessActor actor = new AccessActor(user, user, "staff", project, null, "active");
        manager = request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    // 未配置：返回出厂值 15（值与上下限皆空）。
    @Test
    void unconfigured_returnsFactoryDefault15() throws Exception {
        mockMvc.perform(get(url()).with(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effective_minutes").value(15))
                .andExpect(jsonPath("$.data.value_minutes").doesNotExist());
    }

    // 值与上下限同填且值在闭区间内（可超出旧的 1～120）：保存成功，生效值即所配值。
    @Test
    void configureWithBounds_bySnakeCaseBody_saved() throws Exception {
        mockMvc.perform(put(url()).with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"pca-" + suffix + "\",\"value_minutes\":150,\"min_minutes\":100,\"max_minutes\":180}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value_minutes").value(150))
                .andExpect(jsonPath("$.data.effective_minutes").value(150));
        Map<String, Object> row = jdbc.queryForMap("SELECT pending_clear_alert_minutes AS v, pending_clear_alert_min_minutes AS lo, "
                + "pending_clear_alert_max_minutes AS hi FROM merchants WHERE id = ?", bar);
        assertThat(row).containsEntry("v", 150).containsEntry("lo", 100).containsEntry("hi", 180);
    }

    // 只填值不填上下限、或值落在上下限外：400，库内保持未配置。
    @Test
    void partialOrOutOfBounds_rejected400() throws Exception {
        mockMvc.perform(put(url()).with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"pca-a-" + suffix + "\",\"value_minutes\":30}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(url()).with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"pca-b-" + suffix + "\",\"value_minutes\":200,\"min_minutes\":10,\"max_minutes\":120}"))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT pending_clear_alert_minutes FROM merchants WHERE id = ?", Integer.class, bar)).isNull();
    }

    private String url() {
        return "/api/v1/platform/merchants/" + bar + "/pending-clear-alert";
    }
}
