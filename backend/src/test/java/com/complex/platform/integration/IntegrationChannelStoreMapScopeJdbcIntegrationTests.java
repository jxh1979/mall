package com.complex.platform.integration;

// 文件版本：V0.01

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第 5 条（PGM:659「所有业务数据默认按项目隔离……数据的项目与商户隔离不提供关闭开关」）：渠道门店映射列表按真实接口读取——
 * 商户账号只见本商户的映射；不挂商户的项目员工只见本项目商户的映射；平台管理员不限。响应按蛇形键断言。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class IntegrationChannelStoreMapScopeJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    private final String projectA = "csm_prj_a_" + suffix;
    private final String projectB = "csm_prj_b_" + suffix;
    private final String merchantA = "csm_mch_a_" + suffix;
    private final String merchantB = "csm_mch_b_" + suffix;
    private final String role = "csm_role_" + suffix;

    @BeforeEach
    void seed() {
        for (String project : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', '映射综合体', 'active', now(), now())",
                    project);
        }
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) VALUES (?, '甲店', 'catering', 'active', now(), now(), ?)",
                merchantA, projectA);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) VALUES (?, '乙店', 'catering', 'active', now(), now(), ?)",
                merchantB, projectB);
        for (String merchant : List.of(merchantA, merchantB)) {
            jdbc.update("""
                    INSERT INTO integration_channel_store_map (id, provider_code, channel, external_store_id, merchant_id, status, created_at, updated_at)
                    VALUES (?, 'mock-provider', 'douyin', ?, ?, 'active', now(), now())""", "csm_" + merchant, "store_" + merchant, merchant);
        }
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '渠道映射查看', 'project', 'active', now())",
                role, role);
        jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'integration.channel_store_map.view')", role);
    }

    @Test
    void merchantAccountSeesOnlyOwnMerchantMaps() throws Exception {
        mockMvc.perform(get("/api/v1/integration/channel-store-maps").with(user("csm_usr_m_" + suffix, projectA, merchantA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].merchant_id", Matchers.hasItem(merchantA)))
                .andExpect(jsonPath("$.data.items[*].merchant_id", Matchers.not(Matchers.hasItem(merchantB))));
    }

    @Test
    void projectStaffSeesOnlyOwnProjectMerchantMaps() throws Exception {
        mockMvc.perform(get("/api/v1/integration/channel-store-maps").with(user("csm_usr_p_" + suffix, projectB, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].merchant_id", Matchers.hasItem(merchantB)))
                .andExpect(jsonPath("$.data.items[*].merchant_id", Matchers.not(Matchers.hasItem(merchantA))));
    }

    private RequestPostProcessor user(String userId, String project, String merchant) {
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) VALUES (?, ?, 'staff', ?, ?, 'active', now())",
                userId, userId, project, merchant);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        AccessActor actor = new AccessActor(userId, userId, "staff", project, merchant, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
