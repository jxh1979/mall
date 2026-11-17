package com.complex.platform.ops.productionreadiness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "complex.smoke.admin.username=p0_jdbc_test_admin",
        "complex.smoke.admin.password=p0_jdbc_test_password"
})
@Transactional
class ProductionReadinessJdbcIntegrationTests {

    @Autowired
    private ProductionReadinessService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void resetReadinessTables() {
        jdbcTemplate.execute("""
                TRUNCATE production_readiness_check_blockers,
                         production_readiness_checks,
                         idempotency_records,
                         audit_events,
                         production_readiness_items
                """);
    }

    @Test
    void listItemsSeedsDefaultsAndWritesAuditEventThroughPostgres() {
        var response = service.listItems();

        assertThat(response.items()).hasSize(12);
        assertThat(response.total()).isEqualTo(12);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM production_readiness_items",
                Long.class
        )).isEqualTo(12);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_name = 'production_readiness.viewed'",
                Long.class
        )).isEqualTo(1);
    }

    @Test
    void authenticatedListItemsApiReturnsOkAndDefaultTotalThroughPostgres() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login_name": "p0_jdbc_test_admin",
                                  "password": "p0_jdbc_test_password"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String accessToken = objectMapper.readTree(loginResponse)
                .path("data")
                .path("access_token")
                .asText();

        mockMvc.perform(get("/api/v1/ops/production-readiness/items")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.total").value(12));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM production_readiness_items",
                Long.class
        )).isEqualTo(12);
    }

    // 第十八批 AN 段第 231 条 231-R1（PGM:287「……菜单、页面标题、字段名……必须全部为中文……」；契约 ProductionReadinessItemUpdateRequest
    // required：idempotency_key、status、owner_department、owner_person、target_date、blocking_scope、fallback_mode）：
    // 默认上线准备项的名称与责任部门是中文；按后台页面「标记已验证」同形的蛇形请求体（status=passed，四项从该行原样带回）经真实入口保存并留痕。
    @Test
    void defaultItemsAreChineseAndPageShapedPassedUpdateIsSavedThroughPostgres() throws Exception {
        RequestPostProcessor opsStaff = readinessManager(String.valueOf(System.nanoTime()));
        String listResponse = mockMvc.perform(get("/api/v1/ops/production-readiness/items").with(opsStaff))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode items = objectMapper.readTree(listResponse).path("data").path("items");
        assertThat(items).hasSize(12);
        JsonNode drill = null;
        for (JsonNode item : items) {
            assertThat(item.path("item_name").asText()).doesNotContainPattern("[A-Za-z]");
            assertThat(item.path("owner_department").asText()).doesNotContainPattern("[A-Za-z]");
            if ("nas-restore-drill".equals(item.path("id").asText())) {
                drill = item;
            }
        }
        assertThat(drill).isNotNull();

        mockMvc.perform(put("/api/v1/ops/production-readiness/items/nas-restore-drill").with(opsStaff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "idempotency_key", "ops-readiness-231-" + System.nanoTime(),
                                "status", "passed",
                                "evidence_link", "evidence://nas/restore-drill/202611",
                                "owner_person", "运维值班负责人",
                                "owner_department", drill.path("owner_department").asText(),
                                "target_date", drill.path("target_date").asText(),
                                "blocking_scope", drill.path("blocking_scope").asText(),
                                "fallback_mode", drill.path("fallback_mode").asText()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("passed"));

        assertThat(jdbcTemplate.queryForMap(
                "SELECT status, evidence_link, owner_person FROM production_readiness_items WHERE id = 'nas-restore-drill'"))
                .containsEntry("status", "passed")
                .containsEntry("evidence_link", "evidence://nas/restore-drill/202611")
                .containsEntry("owner_person", "运维值班负责人");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_name = 'production_readiness.updated' AND business_id = 'nas-restore-drill'",
                Long.class
        )).isEqualTo(1);
    }

    /** 平台公司员工经一个普通角色持有上线准备查看与管理权限（后台 Web 终端）；管理权限是需审批的高风险码，平台管理员不默认持有。 */
    private RequestPostProcessor readinessManager(String suffix) {
        String role = "role_ops_readiness_" + suffix;
        jdbcTemplate.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'第231条','project','active',now())",
                role, "上线准备管理" + suffix);
        for (String code : List.of("ops.production_readiness.view", "ops.production_readiness.manage")) {
            jdbcTemplate.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)", role, code);
        }
        String user = "usr_ops_readiness_" + suffix;
        jdbcTemplate.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff','org',NULL,'active',now())",
                user, user);
        jdbcTemplate.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user, role);
        AccessActor actor = new AccessActor(user, user, "staff", "org", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
