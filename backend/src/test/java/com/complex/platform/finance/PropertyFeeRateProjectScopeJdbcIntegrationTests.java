package com.complex.platform.finance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
import com.complex.platform.support.StaffRequestFixture;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BC 段第 368 条 368-R1（真库、真实入口、真实授权链、蛇形键）：PGM:659「所有业务数据默认按项目隔离……数据的项目与商户隔离不提供关闭开关」；
 * PGM:744「前端隐藏只是辅助，后端接口必须独立校验。」。物业费费率的建与查按 finance.property_fee.manage 可触及的项目判
 * （{@link com.complex.platform.access.AuthorizationService#requireReachableProject}）：项目 A 财务建、查 B 的费率 403 CROSS_PROJECT_FORBIDDEN
 * 且建的失败留痕；A 自己 200；可跨项目角色勾中 B 的账号对 B 200；平台管理员不变。修复前 A 财务建、查 B 都 200。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class PropertyFeeRateProjectScopeJdbcIntegrationTests {

    private static final String CODE = "finance.property_fee.manage";
    private static final String RATES = "/api/v1/finance/property-fee/rates";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AccessRepository accessRepository;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    private final String projectA = "r368prj_a_" + s;
    private final String projectB = "r368prj_b_" + s;

    @BeforeEach
    void seed() {
        for (String project : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex',?,'active',now(),now())",
                    project, "物业费费率项目闸回归综合体" + project);
        }
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    /** 项目 A 财务：建 B 的费率 403 且不落行、留失败痕；建 A 200；查 B 403、查 A 200。 */
    @Test
    void projectFinanceOnlyReachesOwnProject() throws Exception {
        RequestPostProcessor financeA = new StaffRequestFixture(jdbc).staff("r368usr_a_" + s, "staff", projectA, null, CODE);

        String key = "r368-b-" + s;
        create(financeA, projectB, key).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM property_fee_rates WHERE project_id = ?", Integer.class, projectB)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'finance_property_fee.rate_set_failed'"
                + " AND idempotency_key = ?", Integer.class, key)).isEqualTo(1);

        create(financeA, projectA, "r368-a-" + s).andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectA));

        mockMvc.perform(get(RATES + "?project_id=" + projectB).with(financeA)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        mockMvc.perform(get(RATES + "?project_id=" + projectA).with(financeA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].project_id").value(projectA));
    }

    /** 可跨项目角色勾中 B 的账号（归属 A）建、查 B 200；平台管理员建、查 B 200。 */
    @Test
    void crossProjectRoleAndPlatformAdminReachProjectB() throws Exception {
        String role = "role_r368_" + s;
        accessRepository.createRole(new RoleRecord(role, "物业费跨项目回归角色" + s, null, "project", "active", Instant.now(),
                List.of(CODE), null, false, List.of(projectB)));
        String userId = "r368usr_t_" + s;
        accessRepository.createUser(new AccessUser(userId, userId, "staff", projectA, null, "active", Instant.now()));
        accessRepository.assignUserRole(userId, role);
        RequestPostProcessor ticked = session(new AccessActor(userId, userId, "staff", projectA, null, "active"));

        create(ticked, projectB, "r368-t-" + s).andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));
        mockMvc.perform(get(RATES + "?project_id=" + projectB).with(ticked)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].project_id").value(projectB));

        RequestPostProcessor admin = new StaffRequestFixture(jdbc).staff("r368adm_" + s, "platform_admin", null, null, CODE);
        create(admin, projectB, "r368-p-" + s).andExpect(status().isOk());
        mockMvc.perform(get(RATES + "?project_id=" + projectB).with(admin)).andExpect(status().isOk());
    }

    private ResultActions create(RequestPostProcessor who, String project, String key) throws Exception {
        return mockMvc.perform(post(RATES).with(who).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + "\",\"project_id\":\"" + project + "\",\"rate_per_square_meter_cent\":500}"));
    }

    private static RequestPostProcessor session(AccessActor actor) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
