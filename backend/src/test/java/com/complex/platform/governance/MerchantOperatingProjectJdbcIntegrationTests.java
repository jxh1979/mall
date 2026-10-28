package com.complex.platform.governance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 E 段第 20 条（20-R1、20-R2）真库＋真实接口＋真实授权链回归，请求体一律按前端蛇形键。
 * 依据：治理地图第 659 行「一个商户要在别的项目经营的，由能管理商户档案的人在商户档案里加经营项目，要经过审批才生效。
 * 商户在多个项目经营的，各经营项目的员工和主项目的员工一样，可以修改、存档、删除这份商户主档案。」；第 528 行「发起单据时只需指定该事项的
 * 最终审批人，系统按组织层级自动补全从发起人到该最终审批人之间的全部直接上级节点，逐级审批」；第 529 行「申请人与直接受益人不得审批与自己相关的申请」。
 *
 * <p>夹具随测试事务回滚：项目 A、B、C（编号带随机后缀），商户主项目都是 A；员工都持普通角色（只到本人项目）。
 * 被拒请求的失败留痕是独立事务，按随机后缀隔离留在库里。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MerchantOperatingProjectJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformRepository platformRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_g20_a_" + suffix;
    private final String projectB = "prj_g20_b_" + suffix;
    private final String projectC = "prj_g20_c_" + suffix;
    private RequestPostProcessor mgr;
    private RequestPostProcessor mid;
    private RequestPostProcessor boss;
    private RequestPostProcessor staffB;
    private RequestPostProcessor staffC;

    @BeforeEach
    void seed() {
        for (String projectId : List.of(projectA, projectB, projectC)) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', ?, 'active', now(), now())", projectId, "G20 " + projectId);
        }
        String full = role("full", "'merchant.profile.view','merchant.profile.manage','entity.binding.view',"
                + "'governance.supervisor_review.view','governance.supervisor_review.approve'");
        mgr = account("mgr", projectA, null, full);
        mid = account("mid", projectA, null, full);
        boss = account("boss", projectA, null, full);
        staffB = account("sb", projectB, null, full);
        staffC = account("sc", projectC, null, full);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("mgr"), user("mid"));
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("mid"), user("boss"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 20-R2：商户 m 主项目 A、加了经营项目 B 后，B 项目员工与 A 项目员工一样查看、修改、存档、取消存档、申请删除；最高权限批准删除不因
     * 经营项目行被当成业务引用而 409；不在 m 经营项目里的 C 项目员工 403。修复前 B 员工改档案 403（第一处即红）。
     */
    @Test
    void operatingProjectStaffManageTheMerchantProfileLikePrimaryProjectStaff() throws Exception {
        String m = merchant("m");
        addOperatingProject(m, projectB);

        send(staffB, put("/api/v1/platform/merchants/" + m), profile("upd-b", "B 项目改名"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchant_name").value("B 项目改名"))
                .andExpect(jsonPath("$.data.project_id").value(projectA));
        assertThat(mine(staffB, "/api/v1/platform/merchants?includeArchived=false")).contains(m);
        send(mgr, put("/api/v1/platform/merchants/" + m), profile("upd-a", "A 项目改名")).andExpect(status().isOk());
        send(staffC, put("/api/v1/platform/merchants/" + m), profile("upd-c", "C 项目改名"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
        assertThat(mine(staffC, "/api/v1/platform/merchants?includeArchived=false")).doesNotContain(m);

        send(staffB, post("/api/v1/platform/merchants/" + m + "/archive"), archive("arc1")).andExpect(status().isOk());
        send(staffB, post("/api/v1/platform/merchants/" + m + "/unarchive"), archive("unarc")).andExpect(status().isOk());
        send(staffB, post("/api/v1/platform/merchants/" + m + "/archive"), archive("arc2")).andExpect(status().isOk());
        send(staffB, post("/api/v1/platform/merchants/" + m + "/delete-request"),
                "{\"idempotency_key\":\"del-" + suffix + "\",\"reason\":\"该商户退出经营\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.delete_status").value("pending"));
        send(master(), post("/api/v1/platform/merchants/" + m + "/delete-approve"),
                "{\"idempotency_key\":\"dap-" + suffix + "\",\"decision\":\"approved\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.deleted").value(true));
        assertThat(jdbc.queryForObject("SELECT delete_status FROM merchants WHERE id=?", String.class, m)).isEqualTo("approved");
    }

    /** 20-R2：B 项目员工看得到 m 的主体绑定；m 不再在 B 经营后，B 员工改档案 403、列表不再有 m。 */
    @Test
    void operatingProjectStaffSeeBindingsAndLoseAccessOnceNoLongerOperating() throws Exception {
        String m = merchant("n");
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) "
                + "VALUES (?, ?, 'project_company', 'active', now(), now())", "be_g20_" + suffix, "be_g20_" + suffix);
        jdbc.update("INSERT INTO business_entity_projects (business_entity_id, project_id) VALUES (?, ?)", "be_g20_" + suffix, projectA);
        Instant now = Instant.now();
        String binding = UUID.randomUUID().toString();
        platformRepository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(binding, m, "be_g20_" + suffix,
                "be_g20_" + suffix, "be_g20_" + suffix, "inactive", LocalDate.now(ZoneOffset.UTC).plusDays(1), null, "G20 夹具", now, now));
        jdbc.update("INSERT INTO merchant_projects (merchant_id, project_id) VALUES (?, ?)", m, projectB);

        List<String> bindings = JsonPath.read(mockMvc.perform(get("/api/v1/platform/business-entity-bindings").with(staffB))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.items[*].id");
        assertThat(bindings).contains(binding);

        jdbc.update("DELETE FROM merchant_projects WHERE merchant_id=?", m);
        send(staffB, put("/api/v1/platform/merchants/" + m), profile("upd-gone", "不再经营"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
        assertThat(mine(staffB, "/api/v1/platform/merchants?includeArchived=false")).doesNotContain(m);
    }

    /**
     * 20-R1：A 项目持 merchant.profile.manage 的员工给主项目 A 的商户申请加经营项目 B，指定最终审批人（上级的上级），进统一审批；
     * 本人不能批，链上逐级批准后才写入 merchant_projects，申请记最终审批人与时间并留痕；查看口返回经营项目、申请记录与可申请项目。
     * 修复前没有该接口（第一处即红：期望 200 实得 404）。
     */
    @Test
    void addingAnOperatingProjectTakesEffectOnlyAfterChainApproval() throws Exception {
        String m = merchant("r");
        String requestId = idOf(apply(mgr, m, projectB, "r1", user("boss")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_approval"))
                .andExpect(jsonPath("$.data.merchant_id").value(m))
                .andExpect(jsonPath("$.data.project_id").value(projectB))
                .andExpect(jsonPath("$.data.requested_by").value(user("mgr"))));
        assertThat(operating(m, projectB)).isZero();
        String review = reviewOf(requestId);
        assertThat(jdbc.queryForObject("SELECT action_type FROM supervisor_review_decisions WHERE review_id=?", String.class, review))
                .isEqualTo("merchant.project.add");

        approve(mgr, review, "approved").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        approve(mid, review, "approved").andExpect(status().isOk());
        assertThat(operating(m, projectB)).isZero();
        approve(boss, review, "approved").andExpect(status().isOk());

        assertThat(operating(m, projectB)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM merchant_project_requests WHERE id=?", String.class, requestId)).isEqualTo("approved");
        assertThat(jdbc.queryForObject("SELECT decided_by FROM merchant_project_requests WHERE id=?", String.class, requestId)).isEqualTo(user("boss"));
        assertThat(jdbc.queryForObject("SELECT decided_at FROM merchant_project_requests WHERE id=?", java.sql.Timestamp.class, requestId)).isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='merchant.operating_project.approved' AND business_id=?",
                Integer.class, requestId)).isEqualTo(1);

        String view = mockMvc.perform(get("/api/v1/platform/merchants/" + m + "/operating-projects").with(staffB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primary_project_id").value(projectA))
                .andExpect(jsonPath("$.data.primary_project_name").value("G20 " + projectA))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<List<String>>read(view, "$.data.operating_projects[*].project_id")).containsExactly(projectB);
        assertThat(JsonPath.<List<String>>read(view, "$.data.requests[*].id")).containsExactly(requestId);
        List<String> options = JsonPath.read(view, "$.data.project_options[*].project_id");
        assertThat(options).contains(projectC).doesNotContain(projectA, projectB);
    }

    /** 20-R1：驳回不写经营项目、可重新申请；待审期间同项目再申请、已在经营的项目（含主项目）再申请都 409；同幂等键重放只一张申请一张复核单。 */
    @Test
    void rejectedRequestLeavesNoOperatingProjectAndDuplicatesAreRefused() throws Exception {
        String m = merchant("j");
        String first = idOf(apply(mgr, m, projectB, "j1").andExpect(status().isOk()));
        apply(mgr, m, projectB, "j1").andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(first));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM merchant_project_requests WHERE merchant_id=?", Integer.class, m)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM supervisor_review_decisions WHERE object_id=?", Integer.class, first)).isEqualTo(1);
        apply(mgr, m, projectB, "j2").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MERCHANT_PROJECT_REQUEST_PENDING"));
        apply(mgr, m, projectA, "j3").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MERCHANT_PROJECT_ALREADY_OPERATING"));

        approve(mid, reviewOf(first), "rejected").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM merchant_project_requests WHERE id=?", String.class, first)).isEqualTo("rejected");
        assertThat(operating(m, projectB)).isZero();

        String second = idOf(apply(mgr, m, projectB, "j4").andExpect(status().isOk()));
        approve(mid, reviewOf(second), "approved").andExpect(status().isOk());
        assertThat(operating(m, projectB)).isEqualTo(1);
        apply(mgr, m, projectB, "j5").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MERCHANT_PROJECT_ALREADY_OPERATING"));
    }

    /** 20-R1：不持 merchant.profile.manage 403；商户不在本人项目经营的 C 项目员工 403 且不落申请、记失败留痕；商户账号 403。 */
    @Test
    void onlyProfileManagersReachingTheMerchantMayApply() throws Exception {
        String m = merchant("x");
        RequestPostProcessor viewer = account("vw", projectA, null, role("vw", "'merchant.profile.view'"));
        RequestPostProcessor merchantAccount = account("ma", projectA, m, role("ma", "'merchant.profile.view','merchant.profile.manage'"));

        apply(viewer, m, projectB, "x1").andExpect(status().isForbidden());
        apply(staffC, m, projectB, "x2").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='merchant.operating_project.request_failed' "
                + "AND actor_id=?", Integer.class, user("sc"))).isEqualTo(1);
        apply(merchantAccount, m, projectB, "x3").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PLATFORM_SCOPE_REQUIRED"));
        mockMvc.perform(get("/api/v1/platform/merchants/" + m + "/operating-projects").with(staffC))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM merchant_project_requests WHERE merchant_id=?", Integer.class, m)).isZero();
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    /** 商户加经营项目：走真实流程——A 项目员工申请、直接上级批准（20-R1）。 */
    private void addOperatingProject(String merchantId, String projectId) throws Exception {
        String requestId = idOf(apply(mgr, merchantId, projectId, "add-" + projectId).andExpect(status().isOk()));
        approve(mid, reviewOf(requestId), "approved").andExpect(status().isOk());
        assertThat(operating(merchantId, projectId)).isEqualTo(1);
    }

    private ResultActions apply(RequestPostProcessor who, String merchantId, String projectId, String key) throws Exception {
        return apply(who, merchantId, projectId, key, null);
    }

    /** 前端提交形状：项目、理由、最终审批人（不指定＝直接上级审批）。 */
    private ResultActions apply(RequestPostProcessor who, String merchantId, String projectId, String key, String finalApprover)
            throws Exception {
        return send(who, post("/api/v1/platform/merchants/" + merchantId + "/operating-projects"),
                "{\"idempotency_key\":\"mpr-" + key + "-" + suffix + "\",\"project_id\":\"" + projectId
                        + "\",\"reason\":\"该商户在别的项目开店经营\""
                        + (finalApprover == null ? "" : ",\"final_approver_id\":\"" + finalApprover + "\"") + "}");
    }

    private ResultActions approve(RequestPostProcessor who, String review, String decision) throws Exception {
        return send(who, post("/api/v1/platform/supervisor-review/" + review + "/approve"), "{\"decision\":\"" + decision
                + "\",\"comment\":\"已核对\",\"idempotency_key\":\"ap_" + UUID.randomUUID() + "\"}");
    }

    private String reviewOf(String requestId) {
        return jdbc.queryForObject("SELECT review_id FROM merchant_project_requests WHERE id=?", String.class, requestId);
    }

    private int operating(String merchantId, String projectId) {
        return jdbc.queryForObject("SELECT count(*) FROM merchant_projects WHERE merchant_id=? AND project_id=?", Integer.class,
                merchantId, projectId);
    }

    private String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    private String merchant(String tag) {
        String id = "mch_g20_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                + "VALUES (?, ?, 'catering', 'active', now(), now(), ?)", id, "G20 商户 " + tag, projectA);
        return id;
    }

    private String profile(String key, String name) {
        return "{\"idempotency_key\":\"" + key + "-" + suffix + "\",\"merchant_name\":\"" + name + "\",\"merchant_type\":\"catering\","
                + "\"contact_name\":\"王\",\"contact_phone\":\"13800000000\"}";
    }

    private String archive(String key) {
        return "{\"idempotency_key\":\"" + key + "-" + suffix + "\",\"reason\":\"暂停经营\"}";
    }

    private ResultActions send(RequestPostProcessor who, MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** 以给定身份 GET 商户列表（须 200），只取本次夹具的商户编号。 */
    private List<String> mine(RequestPostProcessor who, String url) throws Exception {
        List<String> ids = JsonPath.read(mockMvc.perform(get(url).with(who)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.items[*].id");
        return ids.stream().filter(id -> id.endsWith(suffix)).toList();
    }

    private String role(String tag, String permissions) {
        String role = "role_g20_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'经营项目回归','project','active',now())", role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN(" + permissions + ")", role);
        return role;
    }

    private RequestPostProcessor account(String tag, String project, String merchantId, String role) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, project, merchantId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        return session(new AccessActor(userId, userId, "staff", project, merchantId, "active"));
    }

    /** 持授权 U 盘密钥的最高权限本人（生产会话形状：master_authority，所属公司 platform-company）。 */
    private RequestPostProcessor master() {
        return session(new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                "platform-company", null, "active"));
    }

    private RequestPostProcessor session(AccessActor actor) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String user(String tag) {
        return "usr_g20_" + tag + "_" + suffix;
    }
}
