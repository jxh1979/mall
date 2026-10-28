package com.complex.platform.merchantportal;

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
 * 第十八批 E 段 20-R4 真库＋真实接口回归：商户账号看各经营项目里本商户的数据（其余同型读：游乐项目与场次、计数释放记录、积分账期账单）。
 * 依据：治理地图第 659 行「一个商户在两个或更多项目中有经营的，看到的是每一个项目内他经营的商户。」「商户账号持有这类角色也不能跨项目」；
 * XM-M001-D 答复 3 第 10 条所选选项原文「一个商户账号能看到各项目里本商户的数据；要把财务、员工排班等列表对商户账号改为只按商户过滤」。
 *
 * <p>夹具随测试事务回滚：商户 m 主项目 A、经营项目表里追加 B；商户 n 主项目 B。操作人是归属 A、商户 m 的商户账号，
 * 角色经权限仓储直接建立并分配（普通角色、没有勾选项目）。共享库里有别的数据：断言只看本次夹具建的行（编号都以随机后缀结尾）。
 * 请求一律按前端的蛇形键。修复前商户账号只看到 A 的行（列表包含断言即红）；写入口不放开（对 B 场次提前结束、对 B 账单提异议仍 403）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MerchantAccountCrossProjectOtherReadsJdbcIntegrationTests {

    private static final String BILLS = "/api/v1/loyalty/ticket-points-billing/bills";

    @Autowired
    private AccessRepository accessRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.complex.platform.customer.TicketPointsBillingService billing;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_m20r4_a_" + suffix;
    private final String projectB = "prj_m20r4_b_" + suffix;
    private final String merchantM = "mch_m20r4_m_" + suffix;
    private final String merchantN = "mch_m20r4_n_" + suffix;
    private RequestPostProcessor merchantAccount;

    @BeforeEach
    void seed() {
        for (String project : List.of(projectA, projectB)) {
            jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', ?, 'active', now(), now())", project, project);
        }
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, ?, 'amusement', ?, 'active', now(), now())", merchantM, merchantM, projectA);
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, ?, 'amusement', ?, 'active', now(), now())", merchantN, merchantN, projectB);
        jdbcTemplate.update("INSERT INTO merchant_projects (merchant_id, project_id) VALUES (?, ?)", merchantM, projectB);
        String roleId = "role_m20r4_" + suffix;
        accessRepository.createRole(new RoleRecord(roleId, "M20R4 商户账号 " + suffix, null, "project", "active", Instant.now(),
                List.of("amusement.session.view", "amusement.verification.view", "amusement.session.end",
                        "ticket.points.billing.view"),
                null, false, List.of()));
        merchantAccount = account("acct", projectA, merchantM, roleId);
    }

    // 游乐：游乐项目、场次、计数释放记录与场次清场原件／后续处理只按商户；对 B 场次的提前结束不放开。
    @Test
    void amusementReadsFollowMerchantAcrossOperatingProjects() throws Exception {
        String amusementA = amusement("a", merchantM, projectA);
        String amusementB = amusement("b", merchantM, projectB);
        String amusementN = amusement("n", merchantN, projectB);
        String sessionA = session(amusementA, projectA);
        String sessionB = session(amusementB, projectB);
        String sessionN = session(amusementN, projectB);
        String releaseA = release(amusementA, sessionA, projectA);
        String releaseB = release(amusementB, sessionB, projectB);
        release(amusementN, sessionN, projectB);

        // 修复前红：只出 A 的行
        assertThat(mine("/api/v1/amusement/projects", "$.data.items[*].id")).containsExactlyInAnyOrder(amusementA, amusementB);
        assertThat(mine("/api/v1/amusement/sessions", "$.data.items[*].id")).containsExactlyInAnyOrder(sessionA, sessionB);
        assertThat(mine("/api/v1/amusement/count-releases", "$.data[*].id")).containsExactlyInAnyOrder(releaseA, releaseB);
        assertThat(mine("/api/v1/amusement/count-releases?projectId=" + amusementB, "$.data[*].id")).containsExactly(releaseB);
        denied(read(get("/api/v1/amusement/count-releases?projectId=" + amusementN)), "CROSS_MERCHANT_FORBIDDEN");
        read(get("/api/v1/amusement/sessions/" + sessionB + "/endings")).andExpect(status().isOk());
        read(get("/api/v1/amusement/sessions/" + sessionB + "/followups")).andExpect(status().isOk());
        denied(read(get("/api/v1/amusement/sessions/" + sessionN + "/endings")), "CROSS_MERCHANT_FORBIDDEN");

        // 写不放开：对 B 场次提前结束仍按本人项目拒绝
        denied(postJson("/api/v1/amusement/sessions/" + sessionB + "/end", """
                {"idempotency_key": "%s", "clearance_confirmed": true, "cleared_at": "2099-03-17T02:30:00Z", "reason": "M20R4 实际清场"}
                """.formatted(key())), "FORBIDDEN");
    }

    // 积分账期：本商户为卖方的各经营项目账单与其明细、异议、期限调整可读；对 B 账单提异议不放开。
    @Test
    void pointsBillsFollowMerchantAcrossOperatingProjects() throws Exception {
        String rule = billingRule();
        String entity = entity("seller");
        String receiver = entity("receiver");
        String billA = bill("a", rule, entity, receiver, merchantM, projectA);
        String billB = bill("b", rule, entity, receiver, merchantM, projectB);
        String billN = bill("n", rule, entity, receiver, merchantN, projectB);

        // 修复前红：只出 A 的账单；带 B 项目筛选 403
        assertThat(mine(BILLS, "$.data[*].id")).containsExactlyInAnyOrder(billA, billB);
        assertThat(mine(BILLS + "?project_id=" + projectB, "$.data[*].id")).containsExactly(billB);
        read(get(BILLS + "/" + billB + "/items")).andExpect(status().isOk());
        read(get(BILLS + "/" + billB + "/objections")).andExpect(status().isOk());
        read(get(BILLS + "/" + billB + "/schedule-adjustments")).andExpect(status().isOk());
        denied(read(get(BILLS + "/" + billN + "/items")), "FORBIDDEN");

        // 写不放开：对 B 账单提异议仍按本人项目拒绝
        denied(postJson(BILLS + "/" + billB + "/objections", """
                {"idempotency_key": "%s", "reason": "M20R4 金额有误"}
                """.formatted(key())), "FORBIDDEN");
    }

    // 18E 组间遗漏（第 20 条连带）：本商户在经营项目 B 的积分月账单，本商户账号都归属主项目 A——定时任务照样公布（异议期起算）。
    // 修复前要求账号本人项目覆盖账单所在项目，B 的账单一直停在待公布（visible_at 断言即红）。
    @Test
    void pointsBillOfAnOperatingProjectIsPublishedForTheSellerMerchantAccount() {
        String bill = "tpmb_m20r4_pub_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO ticket_points_monthly_bills (id, billing_month, billing_rule_id, seller_entity_id, seller_merchant_id, receiver_entity_id,
                    project_id, amount_cent, created_at)
                VALUES (?, DATE '2097-01-01', ?, ?, ?, ?, ?, 100, now())
                """, bill, billingRule(), entity("pub_seller"), merchantM, entity("pub_receiver"), projectB);
        String calendarCase = "tpcc_m20r4_" + suffix;
        jdbcTemplate.update("INSERT INTO ticket_points_calendar_cases (id, days, source_note, status, created_by, created_at, approved_at) "
                + "VALUES (?, '[]', 'M20R4 本用例给定的工作日日历', 'approved', 'm20r4-review', now(), now())", calendarCase);
        for (java.time.LocalDate d = java.time.LocalDate.parse("2097-02-01"); !d.isAfter(java.time.LocalDate.parse("2097-03-31")); d = d.plusDays(1)) {
            boolean working = d.getDayOfWeek().getValue() <= 5;
            jdbcTemplate.update("INSERT INTO ticket_points_calendar_days (calendar_date, working_day, bank_working_day, case_id) "
                    + "VALUES (?, ?, ?, ?) ON CONFLICT (calendar_date) DO NOTHING", d, working, working, calendarCase);
        }

        billing.finalizeDue(bill, Instant.parse("2097-02-01T00:00:00Z"));

        assertThat(jdbcTemplate.queryForObject("SELECT visible_at IS NOT NULL FROM ticket_points_monthly_bills WHERE id = ?", Boolean.class, bill))
                .isTrue();
    }

    // ── 夹具 ───────────────────────────────────────────────────────────────────────────────

    private static String key() {
        return "m20r4-" + UUID.randomUUID();
    }

    /** 建一名账号、挂上给定角色，返回以其身份登录的会话（后台 Web 终端＋生产过滤器放入的 AccessActor 主体，四项与账号行一致）。 */
    private RequestPostProcessor account(String tag, String organizationId, String merchantId, String... roleIds) {
        String userId = "usr_m20r4_" + tag + "_" + suffix;
        accessRepository.createUser(new AccessUser(userId, userId, "staff", organizationId, merchantId, "active", Instant.now()));
        for (String roleId : roleIds) {
            accessRepository.assignUserRole(userId, roleId);
        }
        AccessActor actor = new AccessActor(userId, userId, "staff", organizationId, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    /** 游乐项目挂在给定项目的本商户空间上（综合体归属＝该空间所在项目，须在商户经营项目内）。 */
    private String amusement(String tag, String merchantId, String projectId) {
        String id = "amp_m20r4_" + tag + "_" + suffix;
        String space = "spc_m20r4_" + tag + "_" + suffix;
        jdbcTemplate.update("INSERT INTO spaces (id, project_id, merchant_id, space_name, space_type, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, 'M20R4 游乐空间', 'amusement', 'active', now(), now())", space, projectId, merchantId);
        jdbcTemplate.update("INSERT INTO amusement_projects (id, project_name, capacity, open_status, merchant_id, space_id, created_at, updated_at) "
                + "VALUES (?, 'M20R4 游乐项目', 10, 'open', ?, ?, now(), now())", id, merchantId, space);
        return id;
    }

    /** 2099-03-17 的一个已锁综合体的场次。 */
    private String session(String amusementId, String complexId) {
        String id = "ses_" + amusementId;
        jdbcTemplate.update("""
                INSERT INTO amusement_sessions (id, project_id, start_time, end_time, capacity, used_capacity, status, created_at, updated_at, complex_id)
                VALUES (?, ?, TIMESTAMPTZ '2099-03-17 10:00:00+08', TIMESTAMPTZ '2099-03-17 11:00:00+08', 10, 0, 'open', now(), now(), ?)
                """, id, amusementId, complexId);
        return id;
    }

    /** 场次结束时系统清空在玩人数的一条释放记录。 */
    private String release(String amusementId, String sessionId, String complexId) {
        String id = "rel_" + amusementId;
        jdbcTemplate.update("""
                INSERT INTO amusement_concurrent_reset_log (id, project_id, scope, business_date, session_id, cleared_rows, cleared_people,
                    boundary_at, reset_at, complex_id)
                VALUES (?, ?, 'session_end', DATE '2099-03-17', ?, 1, 2, TIMESTAMPTZ '2099-03-17 11:00:00+08',
                    TIMESTAMPTZ '2099-03-17 11:00:05+08', ?)
                """, id, amusementId, sessionId, complexId);
        return id;
    }

    /** 2097-01 账期已审批的全国积分账期版本（共享库里已有同月版本时复用）。 */
    private String billingRule() {
        jdbcTemplate.update("""
                INSERT INTO ticket_points_billing_rules (id, version, effective_month, statement_day, objection_working_days, minimum_days,
                    maximum_days, pay_days, status, created_by, created_at, approved_at)
                VALUES (?, ?, DATE '2097-01-01', 1, 10, 1, 30, '{5,15,25}', 'approved', 'm20r4-review', now(), now()) ON CONFLICT DO NOTHING
                """, "tpbr_m20r4_" + suffix, "VM20R4" + suffix);
        return jdbcTemplate.queryForObject("SELECT id FROM ticket_points_billing_rules WHERE effective_month = DATE '2097-01-01'", String.class);
    }

    private String entity(String tag) {
        String id = "be_m20r4_" + tag + "_" + suffix;
        jdbcTemplate.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) "
                + "VALUES (?, ?, 'platform', 'active', now(), now())", id, id);
        return id;
    }

    /** 已公布的 2097-01 积分月账单（卖方商户、所属项目按给定）。 */
    private String bill(String tag, String ruleId, String sellerEntity, String receiver, String merchantId, String projectId) {
        String id = "tpmb_m20r4_" + tag + "_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO ticket_points_monthly_bills (id, billing_month, billing_rule_id, seller_entity_id, seller_merchant_id, receiver_entity_id,
                    project_id, amount_cent, created_at, visible_at, objection_deadline, due_date)
                VALUES (?, DATE '2097-01-01', ?, ?, ?, ?, ?, 100, now(), now(), DATE '2097-02-13', DATE '2097-02-15')
                """, id, ruleId, sellerEntity, merchantId, receiver, projectId);
        return id;
    }

    // ── 请求 ───────────────────────────────────────────────────────────────────────────────

    private ResultActions read(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(merchantAccount));
    }

    /** 以商户账号 GET（须 200），取出该 JSON 路径下属于本次夹具的编号（都以随机后缀结尾）；共享库里别的行不看。 */
    private List<String> mine(String url, String path) throws Exception {
        List<String> ids = JsonPath.read(read(get(url))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), path);
        return ids.stream().filter(id -> id != null && id.endsWith(suffix)).toList();
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(merchantAccount).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static void denied(ResultActions result, String detailCode) throws Exception {
        result.andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value(detailCode));
    }
}
