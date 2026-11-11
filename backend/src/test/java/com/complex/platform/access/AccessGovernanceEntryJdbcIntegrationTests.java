package com.complex.platform.access;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.governance.review.SupervisorReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AE 段第 201 条 201-R5（控制器入口补进 OpenAPI，只改契约、不改运行时；入口已存在，本回归锁现行为）：
 * 按契约声明的请求体（前端蛇形键）与状态码，真库＋真实接口＋真实角色授权链走两处代表入口——
 * 组织层级（PUT/GET /api/v1/access/org-hierarchy，PGM:528「系统按组织层级自动补全从发起人到该最终审批人之间的全部直接上级节点，逐级审批」）
 * 与授权政策写口（POST /api/v1/access/grant-policy）。
 *
 * <p>夹具随测试事务回滚；被拒请求的失败留痕是独立事务，按带随机后缀的幂等键隔离。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AccessGovernanceEntryJdbcIntegrationTests {

    private static final String HIERARCHY = "/api/v1/access/org-hierarchy";
    private static final String POLICY = "/api/v1/access/grant-policy";

    @Autowired private AccessRepository accessRepository;
    @Autowired private SupervisorReviewRepository reviewRepository;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = Long.toString(System.nanoTime());
    private String project;
    private String managerId;
    /** 角色持 access.role.assign（组织层级）、access.grant_request.view 与 admission.policy.manage（授权政策读写）。 */
    private RequestPostProcessor manager;

    @BeforeEach
    void seed() {
        project = "prj_r201r5_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES (?,'complex',?,'active',now(),now())",
                project, "权限治理入口回归项目" + suffix);
        managerId = user("manager");
        String role = "role_r201r5_" + suffix;
        accessRepository.createRole(new RoleRecord(role, "权限治理入口回归" + suffix, null, "project", "active", Instant.now(),
                List.of("access.role.assign", "access.grant_request.view", "admission.policy.manage"), null, false, List.of()));
        accessRepository.assignUserRole(managerId, role);
        manager = session(new AccessActor(managerId, managerId, "staff", project, null, "active"));
    }

    // ① 组织层级页「设置直接上级」：体 idempotency_key、operator_note、superior_id、line_code → 200，列表读回，记 access.org_hierarchy.superior_set。
    @Test
    void setsDirectSuperiorWithContractBodyAndReadsItBack() throws Exception {
        String employee = user("employee");
        String leader = user("leader");
        String line = "line_r201r5_" + suffix;
        reviewRepository.saveApprovalLine(new SupervisorReviewRepository.ApprovalLine(line, "回归部门" + suffix, 1, leader, "active", null),
                managerId, Instant.now());
        setSuperior(employee, "set", "\"" + leader + "\"", "\"" + line + "\"").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_id").value(employee))
                .andExpect(jsonPath("$.data.superior_id").value(leader))
                .andExpect(jsonPath("$.data.line_code").value(line))
                .andExpect(jsonPath("$.data.updated_by").value(managerId));
        List<Map<String, Object>> rows = JsonPath.read(mockMvc.perform(get(HIERARCHY).param("limit", "500").with(manager))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data");
        assertThat(rows).filteredOn(row -> employee.equals(row.get("user_id"))).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("superior_id", leader).containsEntry("line_code", line));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'access.org_hierarchy.superior_set'"
                + " AND event_result = 'success' AND idempotency_key = ?", Integer.class, key("set"))).isEqualTo(1);
    }

    // ② 拒绝路径：直接上级填本人 → 400 ORG_HIERARCHY_SELF，不写组织层级。
    @Test
    void rejectsSelfAsDirectSuperior() throws Exception {
        String employee = user("self");
        setSuperior(employee, "self", "\"" + employee + "\"", "null").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("ORG_HIERARCHY_SELF"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM org_hierarchy WHERE user_id = ?", Integer.class, employee)).isZero();
    }

    // ③ 授权政策：读回的四键原样带修改理由保存 → 200 并记 access.grant_policy.changed；越界值在请求体解析时 400 REQUEST_BODY_INVALID、不落事件；
    // 缺修改理由 400 VALIDATION_FAILED 并记 access.grant_policy.change_failed。
    @Test
    void grantPolicyWriteFollowsContractBodyAndRejections() throws Exception {
        Map<String, Object> policy = JsonPath.read(mockMvc.perform(get(POLICY).with(manager)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data");
        assertThat(policy).containsOnlyKeys("window_days", "position_threshold", "user_threshold", "risk_reviews");

        @SuppressWarnings("unchecked")
        Map<String, Object> window = new LinkedHashMap<>((Map<String, Object>) policy.get("window_days"));
        Map<String, Object> outOfBounds = new LinkedHashMap<>(policy);
        window.put("value", ((Number) window.get("max")).intValue() + 1);
        outOfBounds.put("window_days", window);
        savePolicy("bounds", outOfBounds, "越界回归").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("REQUEST_BODY_INVALID"));
        assertThat(events("access.grant_policy.change_failed", "bounds")).isZero();

        savePolicy("nonote", policy, null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(events("access.grant_policy.change_failed", "nonote")).isEqualTo(1);

        savePolicy("ok", policy, "回归保存授权政策").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.window_days.value").value(((Map<?, ?>) policy.get("window_days")).get("value")));
        assertThat(events("access.grant_policy.changed", "ok")).isEqualTo(1);
    }

    private ResultActions setSuperior(String userId, String tag, String superiorJson, String lineJson) throws Exception {
        return mockMvc.perform(put(HIERARCHY + "/" + userId).with(manager).contentType(MediaType.APPLICATION_JSON).content("""
                {"idempotency_key": "%s", "operator_note": "后台维护直接上级", "superior_id": %s, "line_code": %s}
                """.formatted(key(tag), superiorJson, lineJson)));
    }

    /** 授权政策页「保存授权政策」：体 idempotency_key、policy、operator_note（null 即不带）。 */
    private ResultActions savePolicy(String tag, Map<String, Object> policy, String operatorNote) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key(tag));
        body.put("policy", policy);
        if (operatorNote != null) {
            body.put("operator_note", operatorNote);
        }
        return mockMvc.perform(post(POLICY).with(manager).contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(body)));
    }

    private int events(String eventName, String tag) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = ? AND idempotency_key = ?",
                Integer.class, eventName, key(tag));
    }

    private String key(String tag) {
        return "r201r5-" + tag + "-" + suffix;
    }

    private String user(String tag) {
        String id = "usr_r201r5_" + tag + "_" + suffix;
        accessRepository.createUser(new AccessUser(id, id, "staff", project, null, "active", Instant.now()));
        return id;
    }

    /** 后台 Web 终端＋生产过滤器放入的 AccessActor 主体（四项与账号行一致）。 */
    private static RequestPostProcessor session(AccessActor actor) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
