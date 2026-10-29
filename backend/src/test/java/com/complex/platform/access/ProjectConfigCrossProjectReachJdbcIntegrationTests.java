package com.complex.platform.access;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 G 段第 82 条 82b-R4（按项目配置的本人项目口径改按入口权限码可触及项目，共用 {@link AuthorizationService#requireReachableProject}）
 * 真库＋真实接口＋真实角色勾选回归，请求体一律按前端蛇形键。
 * 依据：治理地图第 659 行「被分配了可跨项目角色的员工（含经岗位得到），在所分配的权限内可以查看、处理该角色可查看项目的数据。」
 * 「逐个勾选的只能查看被选中的项目」；第 528 行「不受第④类「别的项目」限制」。
 *
 * <p>夹具随测试事务回滚：项目 A、B 两行（编号带随机后缀），操作人都是归属 A 的员工；持有人的角色只逐个勾 B，普通员工的角色两项都没勾。
 * 被拒请求的失败留痕、准入闸建的复核单是独立事务，按随机后缀隔离留在库里。修复前持有人对 B 的配置一律 403 CROSS_PROJECT_FORBIDDEN。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class ProjectConfigCrossProjectReachJdbcIntegrationTests {

    @Autowired private AccessRepository accessRepository;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_g82r4_a_" + suffix;
    private final String projectB = "prj_g82r4_b_" + suffix;

    @BeforeEach
    void createProjects() {
        for (String projectId : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', ?, 'active', now(), now())", projectId, "G82R4 " + projectId);
        }
    }

    // ① 未核销票到期提醒：持有人对 B 能读能写；普通员工对 B 403。
    @Test
    void ticketExpiryReminderPolicyFollowsTheRole() throws Exception {
        String[] codes = {"ticket.product.view", "ticket.product.manage"};
        RequestPostProcessor ticked = staffOfA("tk", role("tk", List.of(projectB), codes));
        String url = "/api/v1/ticketing/expiry-reminder-policies/" + projectB;

        send(ticked, put(url), "{\"remind_hours\":24,\"min_hours\":1,\"max_hours\":72,\"idempotency_key\":\"t1-" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));
        mockMvc.perform(get(url).with(ticked)).andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));

        RequestPostProcessor plain = staffOfA("tp", role("tp", List.of(), codes));
        send(plain, put(url), "{\"remind_hours\":24,\"min_hours\":1,\"max_hours\":72,\"idempotency_key\":\"t2-" + suffix + "\"}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        mockMvc.perform(get(url).with(plain))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
    }

    // ② 非积分商户账单异议期：不带 project_id 取本人项目——持有人的本人项目 A 不在该码可触及范围内（该码只来自勾 B 的角色）403，
    // 带 B 200；普通员工不带照旧落 A、带 B 403。
    @Test
    void objectionPeriodDefaultsToOwnProjectOnlyWhenReachable() throws Exception {
        jdbc.update("INSERT INTO settlement_objection_period_policy (project_id, working_days, min_days, max_days, updated_by) "
                + "VALUES (?, 8, 1, 20, 'usr_g82r4')", projectA);
        String url = "/api/v1/settlement/internal-pay-days/objection-period";
        RequestPostProcessor ticked = staffOfA("op", role("op", List.of(projectB), "settlement.config.view"));

        mockMvc.perform(get(url).with(ticked))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        mockMvc.perform(get(url + "?project_id=" + projectB).with(ticked)).andExpect(status().isOk());

        RequestPostProcessor plain = staffOfA("opp", role("opp", List.of(), "settlement.config.view"));
        mockMvc.perform(get(url).with(plain)).andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectA));
        mockMvc.perform(get(url + "?project_id=" + projectB).with(plain))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
    }

    // ③ 酒吧到店前提醒、会员受限时长、全园故障补偿有效期：持有人对 B 能读能写；普通员工对 B 403。
    @Test
    void barCustomerAndAmusementPoliciesFollowTheRole() throws Exception {
        String[] codes = {"bar.reservation.view", "bar.reservation.manage", "customer.account.manage",
                "amusement.park_fault.view", "amusement.compensation_policy.manage"};
        RequestPostProcessor ticked = staffOfA("cfg", role("cfg", List.of(projectB), codes));
        RequestPostProcessor plain = staffOfA("cfgp", role("cfgp", List.of(), codes));
        String bar = "/api/v1/bar/arrival-reminder-policies/" + projectB;
        String restriction = "/api/v1/customer-restriction-settings/" + projectB;
        String compensation = "/api/v1/amusement/compensation-policies/" + projectB;

        send(ticked, put(bar), "{\"remind_minutes\":30,\"min_minutes\":5,\"max_minutes\":60,\"idempotency_key\":\"b1-" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));
        mockMvc.perform(get(bar).with(ticked)).andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));
        send(ticked, put(restriction), "{\"duration_days\":90,\"min_days\":1,\"max_days\":180,\"idempotency_key\":\"r1-" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));
        mockMvc.perform(get(restriction).with(ticked)).andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));
        send(ticked, post(compensation), "{\"validity_days\":365,\"min_days\":30,\"max_days\":730,\"idempotency_key\":\"a1-" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));
        mockMvc.perform(get(compensation).with(ticked)).andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));

        send(plain, put(bar), "{\"remind_minutes\":30,\"min_minutes\":5,\"max_minutes\":60,\"idempotency_key\":\"b2-" + suffix + "\"}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        send(plain, put(restriction), "{\"duration_days\":90,\"min_days\":1,\"max_days\":180,\"idempotency_key\":\"r2-" + suffix + "\"}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        send(plain, post(compensation), "{\"validity_days\":365,\"min_days\":30,\"max_days\":730,\"idempotency_key\":\"a2-" + suffix + "\"}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        mockMvc.perform(get(compensation).with(plain))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
    }

    // ④ 客人端首页内容：持有人能读 B、提交发布 B 时先存待审原文再进准入审批（409 ADMISSION_BLOCKED）；普通员工对 B 403、不留待审原文。
    @Test
    void homeContentFollowsTheRole() throws Exception {
        String url = "/api/v1/home-contents/" + projectB;
        RequestPostProcessor ticked = staffOfA("hc", role("hc", List.of(projectB), "customer.home_content.manage"));

        mockMvc.perform(get(url).with(ticked)).andExpect(status().isOk());
        send(ticked, post(url + "/publish"), "{\"intro\":\"G82R4 介绍\",\"announcement\":\"\",\"idempotency_key\":\"h1-" + suffix + "\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED"));
        assertThat(jdbc.queryForObject("SELECT intro FROM customer_home_content_drafts WHERE project_id=?", String.class, projectB))
                .isEqualTo("G82R4 介绍");

        jdbc.update("DELETE FROM customer_home_content_drafts WHERE project_id=?", projectB);
        RequestPostProcessor plain = staffOfA("hcp", role("hcp", List.of(), "customer.home_content.manage"));
        mockMvc.perform(get(url).with(plain))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        send(plain, post(url + "/publish"), "{\"intro\":\"G82R4 介绍\",\"announcement\":\"\",\"idempotency_key\":\"h2-" + suffix + "\"}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_home_content_drafts WHERE project_id=?", Integer.class, projectB))
                .isZero();
    }

    // ④b 首页内容：审批码只来自本人项目 A 的普通角色、编辑码来自只勾 B 的角色——审批码够不着 B 时回落编辑码，能读 B。
    // 复核修复前红：持有审批码就只按审批码判，读 B 403 CROSS_PROJECT_FORBIDDEN。
    @Test
    void homeContentReadFallsBackToTheManageCodeWhenTheApproveCodeDoesNotReach() throws Exception {
        RequestPostProcessor both = staffOfA("hca", role("hca", List.of(projectB), "customer.home_content.manage"));
        accessRepository.assignUserRole("usr_g82r4_hca_" + suffix, role("hcaa", List.of(), "governance.supervisor_review.approve"));
        mockMvc.perform(get("/api/v1/home-contents/" + projectB).with(both)).andExpect(status().isOk());
    }

    // ⑤ 存酒到期提醒节点：持有人能配 project:B、列表里有 B 行（不出 A 行）；普通员工配 project:B 403、列表只出本人项目 A 行。
    @Test
    void savedBottleReminderPolicyFollowsTheRole() throws Exception {
        jdbc.update("INSERT INTO saved_bottle_reminder_policy (scope_id, reminder_days) VALUES (?, '9')", "project:" + projectA);
        String url = "/api/v1/inventory/saved-bottles/reminder-policy";
        RequestPostProcessor ticked = staffOfA("sb", role("sb", List.of(projectB), "bar.saved_bottle.reminder_policy.manage"));

        send(ticked, put(url), "{\"scope_id\":\"project:" + projectB + "\",\"reminder_days\":[7,1],\"idempotency_key\":\"s1-" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.scope_id").value("project:" + projectB));
        assertThat(myScopes(ticked, url)).containsExactly("project:" + projectB);

        RequestPostProcessor plain = staffOfA("sbp", role("sbp", List.of(), "bar.saved_bottle.reminder_policy.manage"));
        send(plain, put(url), "{\"scope_id\":\"project:" + projectB + "\",\"reminder_days\":[5],\"idempotency_key\":\"s2-" + suffix + "\"}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(myScopes(plain, url)).containsExactly("project:" + projectA);
        assertThat(jdbc.queryForObject("SELECT reminder_days FROM saved_bottle_reminder_policy WHERE scope_id=?", String.class,
                "project:" + projectB)).isEqualTo("7,1");
    }

    // ── 角色、会话与取数 ─────────────────────────────────────────────────────────────────────────

    private String role(String tag, List<String> ticked, String... codes) {
        String roleId = "role_g82r4_" + tag + "_" + suffix;
        accessRepository.createRole(new RoleRecord(roleId, "G82R4 " + tag + " " + suffix, null, "project", "active", Instant.now(),
                List.of(codes), null, false, ticked));
        return roleId;
    }

    /** 建一名归属 A 的员工、挂上给定角色，返回以其身份登录的会话（后台 Web 终端＋生产过滤器放入的 AccessActor 主体）。 */
    private RequestPostProcessor staffOfA(String tag, String roleId) {
        String userId = "usr_g82r4_" + tag + "_" + suffix;
        accessRepository.createUser(new AccessUser(userId, userId, "staff", projectA, null, "active", Instant.now()));
        accessRepository.assignUserRole(userId, roleId);
        AccessActor actor = new AccessActor(userId, userId, "staff", projectA, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private ResultActions send(RequestPostProcessor session, MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.with(session).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** 存酒提醒节点列表里属于本次夹具项目的 scope_id（共享库里别的行不看）。 */
    private List<String> myScopes(RequestPostProcessor session, String url) throws Exception {
        List<String> scopes = JsonPath.read(mockMvc.perform(get(url).with(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.items[*].scope_id");
        return scopes.stream().filter(scope -> scope.endsWith(suffix)).toList();
    }
}
