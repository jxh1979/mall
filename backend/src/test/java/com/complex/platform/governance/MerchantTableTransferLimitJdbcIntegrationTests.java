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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AB 段第 190 条 190-R1（BSM:1079「同一会话的转台次数不得超过后台配置的上限（业务参数，按酒吧可配，出厂默认 10 次，须同时配置上下限）」；
 * 决策台账 DEC-R6-19 同句）：酒吧（商户）转台次数上限，范式同 TK-27 待清台提醒时限——未配置按出厂 10 次；
 * 配置时值、下限、上限三者同填且 0≤下限≤值≤上限，否则 400。请求体按前端真实形状（adminClient.mapKeys 蛇形键），走真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MerchantTableTransferLimitJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String digits = String.valueOf(System.nanoTime());
    private final String project = "ttl_prj_" + suffix;
    private final String bar = "ttl_bar_" + suffix;
    private RequestPostProcessor manager;
    private RequestPostProcessor viewer;

    @BeforeEach
    void seed() {
        String company = "ttl_be_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','转台综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())", company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at) VALUES(?,'转台酒吧','bar',?,?,?,?,'active',now(),now())",
                bar, company, company, company, project);
        manager = actor("ttl_mgr_" + suffix, "转台上限管理" + digits, List.of("merchant.profile.view", "merchant.profile.manage"));
        viewer = actor("ttl_view_" + suffix, "转台上限查看" + digits, List.of("merchant.profile.view"));
    }

    // 未配置：值与上下限皆空，生效值按出厂 10 次。
    @Test
    void unconfigured_returnsFactoryDefault10() throws Exception {
        mockMvc.perform(get(url()).with(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value_count").doesNotExist())
                .andExpect(jsonPath("$.data.effective_count").value(10));
    }

    // 值与上下限同填且值在闭区间内：保存后回读 3/1/5，生效值即所配值。
    @Test
    void configureWithBounds_bySnakeCaseBody_readBack() throws Exception {
        configure("{\"idempotency_key\":\"ttl-" + suffix + "\",\"value_count\":3,\"min_count\":1,\"max_count\":5}")
                .andExpect(status().isOk());
        mockMvc.perform(get(url()).with(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value_count").value(3))
                .andExpect(jsonPath("$.data.min_count").value(1))
                .andExpect(jsonPath("$.data.max_count").value(5))
                .andExpect(jsonPath("$.data.effective_count").value(3));
        Map<String, Object> row = jdbc.queryForMap("SELECT table_transfer_limit AS v, table_transfer_limit_min AS lo, "
                + "table_transfer_limit_max AS hi FROM merchants WHERE id = ?", bar);
        assertThat(row).containsEntry("v", 3).containsEntry("lo", 1).containsEntry("hi", 5);
    }

    // 缺下限、值大于上限、下限为负：各 400 VALIDATION_FAILED，库内保持未配置。
    @Test
    void partialOutOfBoundsOrNegativeMin_rejected400_unchanged() throws Exception {
        configure("{\"idempotency_key\":\"ttl-a-" + suffix + "\",\"value_count\":3,\"max_count\":5}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        configure("{\"idempotency_key\":\"ttl-b-" + suffix + "\",\"value_count\":6,\"min_count\":1,\"max_count\":5}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        configure("{\"idempotency_key\":\"ttl-c-" + suffix + "\",\"value_count\":3,\"min_count\":-1,\"max_count\":5}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        Map<String, Object> row = jdbc.queryForMap("SELECT table_transfer_limit AS v, table_transfer_limit_min AS lo, "
                + "table_transfer_limit_max AS hi FROM merchants WHERE id = ?", bar);
        assertThat(row.values()).containsOnlyNulls();
    }

    // 只有商户档案查看权限：配置被拒 403，库内不变。
    @Test
    void withoutProfileManage_forbidden403() throws Exception {
        mockMvc.perform(put(url()).with(viewer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"ttl-v-" + suffix + "\",\"value_count\":3,\"min_count\":1,\"max_count\":5}"))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT table_transfer_limit FROM merchants WHERE id = ?", Integer.class, bar)).isNull();
    }

    private RequestPostProcessor actor(String user, String roleName, List<String> permissions) {
        String role = "role_" + user;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'转台次数上限回归','project','active',now())", role, roleName);
        for (String permission : permissions) {
            jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)", role, permission);
        }
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,NULL,'active',now())",
                user, user, project);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user, role);
        AccessActor actor = new AccessActor(user, user, "staff", project, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private ResultActions configure(String body) throws Exception {
        return mockMvc.perform(put(url()).with(manager).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String url() {
        return "/api/v1/platform/merchants/" + bar + "/table-transfer-limit";
    }
}
