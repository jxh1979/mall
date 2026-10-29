package com.complex.platform.access;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
 * 18F（第 81 条 81-R1）真库＋真实接口回归：把账号或员工档案挂到「商户＋项目」的三个写入口核商户在该项目经营。
 * 依据：裁决 fresh-M474 所选选项原文「只有商户的主项目或已批准的经营项目里，才能挂这个商户的员工；否则拒绝。」；
 * 治理地图第 580 行「在商户档案上标注为自营的商户视同公司自有，其员工按公司自有员工路径办理。」
 *
 * <p>夹具随测试事务回滚：项目 A、B；经营主体公司 E；商户 m 主项目 A、不在 B 经营；E 下两家自营商户 s1、s2（无主项目）。
 * 一个空岗位映射（角色、模板都不含权限码），改挂时的新岗位用它。操作人是挂普通角色的员工，会话主体与账号行一致；
 * 被拒绝的请求留下的失败留痕是独立事务，按幂等键查。请求体一律按前端的蛇形键。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MerchantStaffAttachProjectJdbcIntegrationTests {

    private static final String LEVEL = "lvl_m1_lower";

    @Autowired
    private AccessRepository accessRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_m81_a_" + suffix;
    private final String projectB = "prj_m81_b_" + suffix;
    private final String company = "be_m81_e_" + suffix;
    private final String merchantM = "mch_m81_m_" + suffix;
    private final String selfOp1 = "mch_m81_s1_" + suffix;
    private final String selfOp2 = "mch_m81_s2_" + suffix;
    private final String position = "pos_m81_" + suffix;

    @BeforeEach
    void seed() {
        for (String project : List.of(projectA, projectB)) {
            jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', ?, 'active', now(), now())", project, project);
        }
        jdbcTemplate.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) "
                + "VALUES (?, ?, 'project_company', 'active', now(), now())", company, company);
        merchant(merchantM, projectA, null);
        merchant(selfOp1, null, "self_operated");
        merchant(selfOp2, null, "self_operated");
        String role = "role_m81_pos_" + suffix;
        String template = "tpl_m81_pos_" + suffix;
        accessRepository.createRole(new RoleRecord(role, "M81 岗位角色 " + suffix, null, "project", "active", Instant.now(), List.of(), null));
        Instant at = Instant.parse("2026-06-14T00:00:00Z");
        accessRepository.createTemplate(new PermissionTemplate(template, template, template, "M81 岗位模板", "base", null, "platform", null,
                "merge", null, null, false, "active", "integration test", "seed-admin", at, at, at), List.of());
        accessRepository.savePositionRoleTemplate(new PositionRoleTemplate("prt_m81_" + suffix, position, LEVEL, role, template,
                null, "active", "seed-admin", Instant.now(), Instant.now()));
    }

    // ① ② 改挂：商户 m 不在 B 经营 → 持分配权限、能到 B 的人与平台管理员都 403；归属、岗位不变，不建申请单，留失败痕。
    // 追加经营项目 (m, B) 后同样的请求受理，账号归属变为 (B, m)。修复前第一处断言即红（受理 200）。
    @Test
    void scopeChangeIntoAProjectTheMerchantDoesNotOperateInIsRejected() throws Exception {
        String target = positioned("tgt", projectB, null);
        RequestPostProcessor operator = withBoss("op", projectB, "access.role.assign");
        String key = key();

        denied(scope(operator, target, key, projectB, merchantM));
        assertThat(scopeOf(target)).containsEntry("organization_id", projectB).containsEntry("merchant_id", null);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM access_grant_requests WHERE target_user_id = ?", Integer.class, target))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT position_code FROM user_position_assignments WHERE user_id = ?", String.class, target))
                .isEqualTo(position);
        assertThat(failures("access.user_scope.change_failed", key)).isEqualTo(1);

        denied(scope(admin("adm"), target, key(), projectB, merchantM));
        assertThat(scopeOf(target)).containsEntry("merchant_id", null);

        jdbcTemplate.update("INSERT INTO merchant_projects (merchant_id, project_id) VALUES (?, ?)", merchantM, projectB);
        scope(operator, target, key(), projectB, merchantM).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"));
        assertThat(scopeOf(target)).containsEntry("organization_id", projectB).containsEntry("merchant_id", merchantM);
    }

    // ③ 员工登记：登记到 m 不经营的 B → 403，不落档案，留 staff.register_failed（结果码 MERCHANT_NOT_IN_PROJECT）；登记到主项目 A 受理。
    @Test
    void registrationIntoAProjectTheMerchantDoesNotOperateInIsRejected() throws Exception {
        String key = key();
        denied(register(account("regb", projectB, null, role("regb", "staff.profile.submit")), key, projectB, "M81 登记B"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM staff_profiles WHERE merchant_id = ?", Integer.class, merchantM))
                .isZero();
        assertThat(failures("staff.register_failed", key)).isEqualTo(1);

        String created = com.jayway.jsonpath.JsonPath.read(register(account("rega", projectA, null, role("rega", "staff.profile.submit")),
                key(), projectA, "M81 登记A").andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8), "$.data.id");
        assertThat(jdbcTemplate.queryForMap("SELECT project_id, merchant_id FROM staff_profiles WHERE id = ?", created))
                .containsEntry("project_id", projectA).containsEntry("merchant_id", merchantM);
    }

    // ④ 外包建号：挂到 m 不经营的 B → 403，不建账号。
    @Test
    void outsourcedAccountUnderAProjectTheMerchantDoesNotOperateInIsRejected() throws Exception {
        String username = "m81_vendor_" + suffix;
        denied(postJson(account("mgr", projectB, null, role("mgr", "access.user.manage")), "/api/v1/access/users", """
                {"idempotency_key": "%s", "account_type": "outsourced_vendor", "organization_id": "%s", "merchant_id": "%s",
                 "username": "%s", "password": "M81-Test-Pass-2026"}
                """.formatted(key(), projectB, merchantM, username)));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM access_users WHERE username = ?", Integer.class, username)).isZero();
    }

    // ⑤ 守护：自营商户员工（归属为所属公司 E）改挂到同公司另一自营商户、归属不变，不被本闸拦（自营商户没有主项目）。
    @Test
    void selfOperatedMerchantStaffMayMoveWithinTheCompany() throws Exception {
        String target = positioned("self", company, selfOp1);
        RequestPostProcessor operator = withBoss("selfop", company, "access.role.assign");
        scope(operator, target, key(), company, selfOp2).andExpect(status().isOk());
        assertThat(scopeOf(target)).containsEntry("organization_id", company).containsEntry("merchant_id", selfOp2);
    }

    // ⑥ 自营商户 s3（主项目 A，经营主体公司 E）：员工可挂在 E（PGM:580 公司自有路径的归属）或经营项目 A；挂到 s3 不经营的 B → 403，归属不变。
    // 修复前第一处断言即红（自营一律放行，受理 200）。
    @Test
    void selfOperatedMerchantStaffAttachOnlyToItsCompanyOrOperatingProjects() throws Exception {
        String selfOp3 = "mch_m81_s3_" + suffix;
        merchant(selfOp3, projectA, "self_operated");
        String inB = positioned("s3b_t", projectB, null);
        denied(scope(withBoss("s3b", projectB, "access.role.assign"), inB, key(), projectB, selfOp3));
        assertThat(scopeOf(inB)).containsEntry("organization_id", projectB).containsEntry("merchant_id", null);

        String inCompany = positioned("s3e_t", company, selfOp1);
        scope(withBoss("s3e", company, "access.role.assign"), inCompany, key(), company, selfOp3).andExpect(status().isOk());
        assertThat(scopeOf(inCompany)).containsEntry("organization_id", company).containsEntry("merchant_id", selfOp3);

        String inA = positioned("s3a_t", projectA, null);
        scope(withBoss("s3a", projectA, "access.role.assign"), inA, key(), projectA, selfOp3).andExpect(status().isOk());
        assertThat(scopeOf(inA)).containsEntry("organization_id", projectA).containsEntry("merchant_id", selfOp3);
    }

    // ── 夹具 ────────────────────────────────────────────────────────────────────────────────

    private void merchant(String id, String projectId, String cooperationForm) {
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, business_entity_id, project_id, cooperation_form, status, "
                + "created_at, updated_at) VALUES (?, ?, 'retail', ?, ?, ?, 'active', now(), now())", id, id, company, projectId, cooperationForm);
    }

    private String userId(String tag) {
        return "usr_m81_" + tag + "_" + suffix;
    }

    private static String key() {
        return "m81-" + UUID.randomUUID();
    }

    private String role(String tag, String... codes) {
        String roleId = "role_m81_" + tag + "_" + suffix;
        accessRepository.createRole(new RoleRecord(roleId, "M81 " + tag + " " + suffix, null, "project", "active", Instant.now(),
                List.of(codes), null));
        return roleId;
    }

    /** 建一名员工账号、挂上给定角色，返回以其身份登录的会话（后台 Web 终端＋生产过滤器放入的 AccessActor 主体，与账号行一致）。 */
    private RequestPostProcessor account(String tag, String organizationId, String merchantId, String... roleIds) {
        return session(tag, "staff", organizationId, merchantId, roleIds);
    }

    private RequestPostProcessor session(String tag, String accountType, String organizationId, String merchantId, String... roleIds) {
        String id = userId(tag);
        accessRepository.createUser(new AccessUser(id, id, accountType, organizationId, merchantId, "active", Instant.now()));
        for (String roleId : roleIds) {
            accessRepository.assignUserRole(id, roleId);
        }
        AccessActor actor = new AccessActor(id, id, accountType, organizationId, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private RequestPostProcessor admin(String tag) {
        return session(tag, "platform_admin", "org", null, "role_platform_admin");
    }

    /** 挂给定权限普通角色的员工，并有一名直接上级（改挂建的授权申请沿上级链送审）。 */
    private RequestPostProcessor withBoss(String tag, String organizationId, String code) {
        RequestPostProcessor operator = account(tag, organizationId, null, role(tag, code));
        account(tag + "_boss", organizationId, null);
        jdbcTemplate.update("INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by) VALUES (?, ?, 'main', ?)",
                userId(tag), userId(tag + "_boss"), userId(tag));
        return operator;
    }

    /** 一名已定岗（空岗位映射）的员工，返回账号编号。 */
    private String positioned(String tag, String organizationId, String merchantId) throws Exception {
        account(tag, organizationId, merchantId);
        mockMvc.perform(put("/api/v1/access/users/" + userId(tag) + "/position").with(admin(tag + "_adm"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "%s", "position_code": "%s", "staff_level_id": "%s"}
                                """.formatted(key(), position, LEVEL)))
                .andExpect(status().isOk());
        return userId(tag);
    }

    // ── 请求与取数 ──────────────────────────────────────────────────────────────────────────────

    private ResultActions postJson(RequestPostProcessor session, String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(session).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions scope(RequestPostProcessor session, String target, String key, String organizationId, String merchantId)
            throws Exception {
        return mockMvc.perform(put("/api/v1/access/users/" + target + "/scope").with(session).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"idempotency_key": "%s", "organization_id": "%s", "merchant_id": "%s", "position_code": "%s",
                         "staff_level_id": "%s", "reason": "M81 改挂"}
                        """.formatted(key, organizationId, merchantId, position, LEVEL)));
    }

    private ResultActions register(RequestPostProcessor session, String key, String projectId, String name) throws Exception {
        return postJson(session, "/api/v1/staff/registrations", """
                {"idempotency_key": "%s", "project_id": "%s", "merchant_id": "%s", "name": "%s"}
                """.formatted(key, projectId, merchantM, name));
    }

    private static void denied(ResultActions result) throws Exception {
        result.andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("MERCHANT_NOT_IN_PROJECT"));
    }

    private Map<String, Object> scopeOf(String userId) {
        return jdbcTemplate.queryForMap("SELECT organization_id, merchant_id FROM access_users WHERE id = ?", userId);
    }

    /** 该幂等键下结果码为 MERCHANT_NOT_IN_PROJECT 的失败留痕条数。 */
    private Integer failures(String eventName, String key) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = ? AND event_result = 'failure' "
                + "AND idempotency_key = ? AND metadata->>'error_code' = 'MERCHANT_NOT_IN_PROJECT'", Integer.class, eventName, key);
    }
}
