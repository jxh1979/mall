package com.complex.platform.governance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
import com.complex.platform.access.TemporaryGrant;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.facility.SiteEntryReportRecord;
import com.complex.platform.facility.SiteEntryReportRepository;
import com.complex.platform.leasing.LeasingLeadRecord;
import com.complex.platform.leasing.LeasingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 E 段第 19 条 19-R3（联系人手机号明文按行所属项目判；进场报备列表按可触及项目取数）真库＋真实接口＋真实角色勾选回归，
 * 请求体一律按前端蛇形键。
 * 依据：治理地图第 877 行「经单独授权且查看权限有效的人员，以及持授权 U 盘密钥的最高权限本人，可以在其获准访问的商户档案列表和档案操作结果中
 * 查看完整号码」「平台管理员身份本身不自动取得该查看权限」「也不免除项目范围检查和查看留痕」「可以在某一条线索上点“查看完整号码”」；
 * 第 659 行「在所分配的权限内可以查看、处理该角色可查看项目的数据」——明文码须由覆盖该行所属项目的来源授予。
 *
 * <p>不开测试事务（单次来源的消耗要真实提交才看得见）。夹具：项目 A、B 两行（编号带随机后缀），操作人是归属 A 的员工；角色经权限仓储建立并分配
 * （普通角色、只逐个勾 B 的角色）。夹具行按随机后缀隔离、结束时删除；审计事件与消耗日志只追加不删，按后缀隔离留在库里。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class CrossProjectPhonePlaintextJdbcIntegrationTests {

    private static final String PII = "pii.contact_phone.view";
    private static final String PLAIN = "13912345678";
    private static final String MASKED = "139****5678";

    @Autowired private AccessRepository accessRepository;
    @Autowired private PlatformRepository platformRepository;
    @Autowired private LeasingRepository leasingRepository;
    @Autowired private SiteEntryReportRepository siteEntryReportRepository;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_g19r3_a_" + suffix;
    private final String projectB = "prj_g19r3_b_" + suffix;
    private final String projectC = "prj_g19r3_c_" + suffix;
    private boolean masterRowAdded;

    @BeforeEach
    void createProjects() {
        for (String projectId : List.of(projectA, projectB, projectC)) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', ?, 'active', now(), now())", projectId, "G19R3 " + projectId);
        }
    }

    @AfterEach
    void cleanUp() {
        String like = "%" + suffix;
        jdbc.update("DELETE FROM site_entry_reports WHERE id LIKE ?", like);
        jdbc.update("DELETE FROM leasing_leads WHERE id LIKE ?", like);
        jdbc.update("DELETE FROM merchant_projects WHERE merchant_id LIKE ?", like);
        jdbc.update("DELETE FROM merchants WHERE id LIKE ?", like);
        jdbc.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", like);
        jdbc.update("DELETE FROM temporary_permission_grants WHERE subject_id LIKE ?", like);
        jdbc.update("DELETE FROM role_viewable_project_holds WHERE user_id LIKE ?", like);
        jdbc.update("DELETE FROM user_roles WHERE user_id LIKE ?", like);
        jdbc.update("DELETE FROM role_viewable_projects WHERE role_id LIKE ?", like);
        jdbc.update("DELETE FROM role_permissions WHERE role_id LIKE ?", like);
        jdbc.update("DELETE FROM roles WHERE id LIKE ?", like);
        jdbc.update("DELETE FROM access_users WHERE id LIKE ?", like);
        if (masterRowAdded) {
            jdbc.update("DELETE FROM access_users WHERE id = ?", SessionIssuer.MASTER_USER_ID);
        }
        jdbc.update("DELETE FROM projects WHERE id IN (?, ?, ?)", projectA, projectB, projectC);
    }

    // 商户列表：查看码经普通角色与勾 B 的角色都持有、明文码只经普通角色持有——A 商户明文、B 商户掩码；
    // 在勾 B 的角色上也给明文码后 B 也明文。修复前 B 明文（第一处 B 断言即红）。
    @Test
    void merchantListShowsPlaintextOnlyForRowsWhosePlaintextSourceReachesTheirProject() throws Exception {
        String merchantA = merchant("a", projectA);
        String merchantB = merchant("b", projectB);
        String user = userId("list");
        RequestPostProcessor session = staffOfA("list", plainRole("list", "merchant.profile.view", PII),
                tickedBRole("list", "merchant.profile.view"));

        String body = read(session, "/api/v1/platform/merchants");
        assertThat(phone(body, merchantA)).isEqualTo(PLAIN);
        assertThat(phone(body, merchantB)).isEqualTo(MASKED);

        accessRepository.assignUserRole(user, tickedBRole("list_pii", PII));
        body = read(session, "/api/v1/platform/merchants");
        assertThat(phone(body, merchantA)).isEqualTo(PLAIN);
        assertThat(phone(body, merchantB)).isEqualTo(PLAIN);
    }

    // 商户档案操作结果（蛇形请求体 PUT）：改 B 商户返回掩码、改 A 商户返回明文。修复前 B 返回明文（第一处断言即红）。
    @Test
    void merchantWriteResultShowsPlaintextOnlyWhenThePlaintextSourceReachesTheRowProject() throws Exception {
        String merchantA = merchant("a", projectA);
        String merchantB = merchant("b", projectB);
        RequestPostProcessor session = staffOfA("put", plainRole("put", "merchant.profile.manage", PII),
                tickedBRole("put", "merchant.profile.manage"));

        assertThat(data(send(session, put("/api/v1/platform/merchants/" + merchantB), merchantProfile("put-b"))
                .andExpect(status().isOk())).path("contact_phone").asText()).isEqualTo(MASKED);
        assertThat(data(send(session, put("/api/v1/platform/merchants/" + merchantA), merchantProfile("put-a"))
                .andExpect(status().isOk())).path("contact_phone").asText()).isEqualTo(PLAIN);
    }

    // 守护：平台管理员不持明文码——全掩码（平台管理员身份本身不自动取得该查看权限）；最高权限本人持明文码——操作结果整份明文、不按项目判。
    @Test
    void platformAdminWithoutThePlaintextCodeSeesMasksAndTheMasterAuthoritySeesPlaintext() throws Exception {
        String merchantA = merchant("a", projectA);
        String merchantB = merchant("b", projectB);
        RequestPostProcessor admin = account("admin", "platform_admin", null, plainRole("admin", "merchant.profile.view"));
        String body = read(admin, "/api/v1/platform/merchants");
        assertThat(phone(body, merchantA)).isEqualTo(MASKED);
        assertThat(phone(body, merchantB)).isEqualTo(MASKED);

        jdbc.update("UPDATE merchants SET archived_at = now() WHERE id = ?", merchantB);
        assertThat(data(send(master(), post("/api/v1/platform/merchants/" + merchantB + "/delete-request"),
                "{\"idempotency_key\":\"del-b-" + suffix + "\",\"reason\":\"停止经营\"}")
                .andExpect(status().isOk())).path("contact_phone").asText()).isEqualTo(PLAIN);
    }

    // 招商线索「查看完整号码」：线索码与明文码都经勾 B 的角色持有——点 B 的线索 200 明文。修复前 403 CROSS_PROJECT_FORBIDDEN（即红）。
    @Test
    void theLeadFullPhoneOpensForTheProjectsTheRoleReaches() throws Exception {
        String leadB = lead("b", projectB);
        RequestPostProcessor session = staffOfA("lead", tickedBRole("lead", "leasing.lead.manage", PII));

        assertThat(data(mockMvc.perform(get(leadPhoneUrl(leadB)).with(session)).andExpect(status().isOk()))
                .path("contact_phone").asText()).isEqualTo(PLAIN);
    }

    // 招商线索：线索码经普通角色与勾 B 的角色都持有、明文码只经单次授予持有（只到本人项目）——点 B 的线索 403、失败痕一条、
    // 单次来源未消耗；随后点 A 的线索 200 明文并消耗一次。
    @Test
    void aSingleUsePlaintextGrantOfTheOwnProjectDoesNotOpenAnotherProjectsLead() throws Exception {
        String leadA = lead("a", projectA);
        String leadB = lead("b", projectB);
        String user = userId("once");
        RequestPostProcessor session = staffOfA("once", plainRole("once", "leasing.lead.manage"),
                tickedBRole("once", "leasing.lead.manage"));
        singleUsePhoneGrant(user);

        String denied = mockMvc.perform(get(leadPhoneUrl(leadB)).with(session)).andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(new ObjectMapper().readTree(denied).path("detail_code").asText()).isEqualTo("CROSS_PROJECT_FORBIDDEN");
        assertThat(leadPhoneEvents("leasing.lead.contact_phone_view_failed", user, leadB)).isEqualTo(1);
        assertThat(grantConsumed(user)).containsExactly(false);

        assertThat(data(mockMvc.perform(get(leadPhoneUrl(leadA)).with(session)).andExpect(status().isOk()))
                .path("contact_phone").asText()).isEqualTo(PLAIN);
        assertThat(grantConsumed(user)).containsExactly(true);
    }

    // 进场报备：审核码经勾 B 的角色持有时列表出现 B 的报备（修复前只取本人项目 A 的行，B 不出——第一处断言即红）；
    // 明文码只来自普通角色时 B 行电话掩码、A 行明文，B 的详情也掩码。
    @Test
    void theSiteEntryListIncludesReachableProjectsAndMasksPhonesPerRow() throws Exception {
        String reportA = report("a", projectA);
        String reportB = report("b", projectB);
        RequestPostProcessor session = staffOfA("ser", plainRole("ser", "facility.site_entry.review", PII),
                tickedBRole("ser", "facility.site_entry.review"));

        String body = read(session, "/api/v1/facility/site-entry-reports");
        assertThat(ids(body)).contains(reportA, reportB);
        assertThat(phone(body, reportA)).isEqualTo(PLAIN);
        assertThat(phone(body, reportB)).isEqualTo(MASKED);
        assertThat(new ObjectMapper().readTree(read(session, "/api/v1/facility/site-entry-reports/" + reportB))
                .path("data").path("contact_phone").asText()).isEqualTo(MASKED);
    }

    // 18E 组间遗漏（第 20 条连带；治理地图第 659 行「各经营项目的员工和主项目的员工一样，可以修改、存档、删除这份商户主档案」）：
    // 商户 m 主项目 A、经营项目 B。经营项目 B 的员工（查看、管理与明文码都经只勾 B 的角色持有）看 m 的列表行与修改结果为明文；
    // 明文码只经勾 C 的角色持有的 B 员工仍掩码。修复前按主项目 A 判，B 员工也掩码（第一处断言即红）。
    @Test
    void operatingProjectStaffSeePlaintextLikePrimaryProjectStaff() throws Exception {
        String merchantM = merchant("m", projectA);
        jdbc.update("INSERT INTO merchant_projects (merchant_id, project_id) VALUES (?, ?)", merchantM, projectB);
        RequestPostProcessor staffB = account("opb", "staff", projectB,
                tickedBRole("opb", "merchant.profile.view", "merchant.profile.manage", PII));
        assertThat(phone(read(staffB, "/api/v1/platform/merchants"), merchantM)).isEqualTo(PLAIN);
        assertThat(data(send(staffB, put("/api/v1/platform/merchants/" + merchantM), merchantProfile("opb"))
                .andExpect(status().isOk())).path("contact_phone").asText()).isEqualTo(PLAIN);

        RequestPostProcessor staffC = account("opc", "staff", projectB,
                tickedBRole("opc", "merchant.profile.view", "merchant.profile.manage"), role("opc_c", List.of(projectC), PII));
        assertThat(phone(read(staffC, "/api/v1/platform/merchants"), merchantM)).isEqualTo(MASKED);
        assertThat(data(send(staffC, put("/api/v1/platform/merchants/" + merchantM), merchantProfile("opc"))
                .andExpect(status().isOk())).path("contact_phone").asText()).isEqualTo(MASKED);
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private String id(String kind, String tag) {
        return kind + "_g19r3_" + tag + "_" + suffix;
    }

    private String merchant(String tag, String projectId) {
        Instant now = Instant.now();
        platformRepository.saveMerchant(new MerchantRecord(id("mch", tag), id("mch", tag), "other", null, null, null,
                "联系人", PLAIN, null, null, null, null, null, null, null, null, "active", now, now).withProject(projectId));
        return id("mch", tag);
    }

    private String lead(String tag, String projectId) {
        Instant now = Instant.now();
        leasingRepository.saveLead(new LeasingLeadRecord(id("lead", tag), "联系人", PLAIN, "outbound", null, null, null, null, null,
                "pending_follow_up", null, null, projectId, now, now));
        return id("lead", tag);
    }

    private String report(String tag, String projectId) {
        Instant now = Instant.now();
        siteEntryReportRepository.insert(new SiteEntryReportRecord(id("ser", tag), projectId, null, "G19R3 进场报备",
                LocalDate.now(), LocalDate.now(), 1, "联系人", PLAIN, "vendor_g19r3_" + suffix, "submitted", null, null,
                "vendor_g19r3_" + suffix, now, now, List.of(), null, null, null));
        return id("ser", tag);
    }

    private String merchantProfile(String key) {
        return "{\"idempotency_key\":\"" + key + "-" + suffix + "\",\"merchant_name\":\"G19R3 改名 " + key + "\","
                + "\"merchant_type\":\"catering\",\"contact_name\":\"王\",\"contact_phone\":\"" + PLAIN + "\"}";
    }

    private void singleUsePhoneGrant(String user) {
        Instant now = Instant.now();
        accessRepository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), user, List.of(PII),
                now.minusSeconds(3600), now.plusSeconds(86_400), true, false, "active", "G19R3 明文手机号单次授权",
                "seed-admin", now, null, null));
        assertThat(grantConsumed(user)).containsExactly(false);
    }

    private List<Boolean> grantConsumed(String user) {
        return jdbc.queryForList("SELECT consumed FROM temporary_permission_grants WHERE subject_id = ?", Boolean.class, user);
    }

    private Integer leadPhoneEvents(String eventName, String user, String leadId) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = ? AND actor_id = ? AND business_id = ?",
                Integer.class, eventName, user, leadId);
    }

    private String leadPhoneUrl(String leadId) {
        return "/api/v1/leasing/leads/" + leadId + "/contact-phone";
    }

    // ── 角色与会话 ───────────────────────────────────────────────────────────────────────────────

    private String plainRole(String tag, String... codes) {
        return role(tag, List.of(), codes);
    }

    private String tickedBRole(String tag, String... codes) {
        return role(tag + "_b", List.of(projectB), codes);
    }

    private String role(String tag, List<String> ticked, String... codes) {
        String roleId = "role_g19r3_" + tag + "_" + suffix;
        accessRepository.createRole(new RoleRecord(roleId, "G19R3 " + tag + " " + suffix, null, "project", "active", Instant.now(),
                List.of(codes), null, false, ticked));
        return roleId;
    }

    private String userId(String tag) {
        return "usr_g19r3_" + tag + "_" + suffix;
    }

    private RequestPostProcessor staffOfA(String tag, String... roleIds) {
        return account(tag, "staff", projectA, roleIds);
    }

    /** 建一名账号、挂上给定角色，返回以其身份登录的会话（后台 Web 终端＋生产过滤器放入的 AccessActor 主体，四项与账号行一致）。 */
    private RequestPostProcessor account(String tag, String accountType, String organizationId, String... roleIds) {
        String userId = userId(tag);
        accessRepository.createUser(new AccessUser(userId, userId, accountType, organizationId, null, "active", Instant.now()));
        for (String roleId : roleIds) {
            accessRepository.assignUserRole(userId, roleId);
        }
        return session(new AccessActor(userId, userId, accountType, organizationId, null, "active"));
    }

    // 最高权限本人（与 ProductionAuthService 签发的主体同一组身份字段）；库里没有这行时临时补一行、结束删掉。
    private RequestPostProcessor master() {
        masterRowAdded = jdbc.update("""
                INSERT INTO access_users (id, username, account_type, organization_id, status, created_at)
                VALUES (?, ?, ?, 'platform-company', 'active', now()) ON CONFLICT (id) DO NOTHING
                """, SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE) == 1;
        String organizationId = jdbc.queryForObject("SELECT organization_id FROM access_users WHERE id = ?", String.class,
                SessionIssuer.MASTER_USER_ID);
        return session(new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, organizationId, null, "active"));
    }

    private RequestPostProcessor session(AccessActor actor) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    // ── 取数 ─────────────────────────────────────────────────────────────────────────────────────

    private ResultActions send(RequestPostProcessor session, MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.with(session).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String read(RequestPostProcessor session, String url) throws Exception {
        return mockMvc.perform(get(url).with(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private JsonNode data(ResultActions result) throws Exception {
        return new ObjectMapper().readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
    }

    private List<String> ids(String body) throws Exception {
        List<String> ids = new ArrayList<>();
        new ObjectMapper().readTree(body).path("data").path("items").forEach(item -> ids.add(item.path("id").asText()));
        return ids;
    }

    // 列表响应里按编号找夹具行，取第一联系人电话
    private String phone(String body, String rowId) throws Exception {
        for (JsonNode item : new ObjectMapper().readTree(body).path("data").path("items")) {
            if (rowId.equals(item.path("id").asText())) {
                return item.path("contact_phone").asText();
            }
        }
        throw new AssertionError("响应里没有夹具行：" + rowId);
    }
}
