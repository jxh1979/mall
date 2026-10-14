package com.complex.platform.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
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
 * M737（可跨项目角色·批 3 集团视图与分析）真库＋真实接口回归：真实角色勾选 → 真实判定 → 真实接口 → 真实 SQL。
 * 依据：治理地图「权限规则」「被分配了可跨项目角色的员工（含经岗位得到），在所分配的权限内可以查看、处理该角色可查看项目的数据。」
 * 「勾选全部项目的可以查看全部项目（含以后新建的项目），逐个勾选的只能查看被选中的项目」；业务地图「谁会看」
 * 「项目级操作人员只看自己所在项目范围内的 KPI 和明细，看不到别的项目。」；治理地图「KPI 可见性分层」
 * 「全平台合计只给平台管理员和勾选了「可以查看全部项目」的人看；只逐个勾选了项目的人，只能分别查看被选中项目各自的数据。」
 *
 * <p>夹具随测试事务回滚：项目表里两个项目 A、B（编号带每次运行的随机后缀），操作人都是归属 A 的员工；
 * 角色直接经权限仓储建立并分配（勾了「可以查看全部项目」、只逐个勾了 B、两项都没勾的普通角色三种）。
 * 会话主体的归属、商户、账号类型、状态与账号行一致。共享库里有别的数据：断言只看本次夹具建的行（编号都以随机后缀结尾）。
 * 被拒绝的请求留下的失败留痕是独立事务，按随机后缀隔离留在库里。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AnalyticsCrossProjectJdbcIntegrationTests {

    @Autowired
    private AccessRepository accessRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_m737_a_" + suffix;
    private final String projectB = "prj_m737_b_" + suffix;
    // 在职员工数是余额类指标：截至日取明天，夹具里刚建的账号都落在截至日窗末之前
    private final String headcountUrl = "/api/v1/analytics/metric-detail?metric=staff.active_headcount&size=200&to="
            + LocalDate.now(ZoneOffset.UTC).plusDays(1);

    @BeforeEach
    void createProjects() {
        for (String projectId : List.of(projectA, projectB)) {
            jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', ?, 'active', now(), now())", projectId, "M737 " + projectId);
        }
    }

    // J1（2.5 项目明细按可触及项目下推）：A、B 各一个空置铺位与一笔逾期应收，另有一笔项目列为空的逾期应收。
    // 应收行的 project_id 是用例手工写进去的——现行合同类出账不带这一列；这条钉的是过滤谓词，不代表页面建出来的数据。
    // 修复前 A 的普通员工两个列表都拿到 A、B 两个项目的行（第一处断言即红）。
    @Test
    void drillDownListsOnlyShowRowsInReachableProjects() throws Exception {
        String spaceA = vacantSpace("a", projectA);
        String spaceB = vacantSpace("b", projectB);
        String debtorA = overdueReceivable("a", projectA);
        String debtorB = overdueReceivable("b", projectB);
        String debtorNone = overdueReceivable("none", null);
        String[] codes = {"contract.view", "finance.ar_ap.view"};

        // A 的普通员工：只有 A 的
        RequestPostProcessor plain = staffOfA("plain", plainRole("plain", codes));
        assertThat(vacantSpaces(plain)).containsExactly(spaceA);
        assertThat(debtors(plain)).containsExactly(debtorA);

        // 角色勾 B 且权限另有普通来源：A、B
        RequestPostProcessor both = staffOfA("both", tickedBRole("both_t", codes), plainRole("both_p", codes));
        assertThat(vacantSpaces(both)).containsExactlyInAnyOrder(spaceA, spaceB);
        assertThat(debtors(both)).containsExactlyInAnyOrder(debtorA, debtorB);

        // 只勾 B：只有 B（本人项目 A 没被勾、又没有其他来源）
        RequestPostProcessor ticked = staffOfA("ticked", tickedBRole("ticked", codes));
        assertThat(vacantSpaces(ticked)).containsExactly(spaceB);
        assertThat(debtors(ticked)).containsExactly(debtorB);

        // 以上三种人都看不到项目列为空的那一行；勾全部的人与平台管理员不按项目列过滤，A、B 和项目列为空的那一行都出
        for (RequestPostProcessor whole : List.of(staffOfA("all", allProjectsRole("all", codes)), platformAdmin(codes))) {
            assertThat(vacantSpaces(whole)).containsExactlyInAnyOrder(spaceA, spaceB);
            assertThat(debtors(whole)).containsExactlyInAnyOrder(debtorA, debtorB, debtorNone);
        }
    }

    // J2（2.8 指标明细，登记了项目列的指标）：在职员工数——夹具里的员工账号就是数据。A 的在职员工是下面四名操作人，B 一名，
    // 另有一名平台公司员工（归属不在项目集合里）。可触及项目按「查看经营分析看板」与登记项权限「查看账号」两个码取交集。
    // 修复前只勾 B 的人看到的是本人项目 A 的四名（第一处断言即红）。
    @Test
    void projectScopedMetricDetailFollowsTheProjectsBothCodesReach() throws Exception {
        String[] codes = {"analytics.dashboard.view", "access.user.view"};
        RequestPostProcessor ticked = staffOfA("ticked", tickedBRole("ticked", codes));
        RequestPostProcessor mixed = staffOfA("mixed",
                tickedBRole("mixed_t", "analytics.dashboard.view"), plainRole("mixed_p", "access.user.view"));
        RequestPostProcessor plain = staffOfA("plain", plainRole("plain", codes));
        RequestPostProcessor all = staffOfA("all", allProjectsRole("all", codes));
        account("b", "staff", projectB, null);
        account("out", "staff", "platform-company", null);
        List<String> staffOfProjectA = List.of(userId("ticked"), userId("mixed"), userId("plain"), userId("all"));

        // 两个码都经只勾 B 的角色持有：合计＝B 的在职员工数
        mockMvc.perform(get(headcountUrl).with(ticked))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary_value").value(1))
                .andExpect(jsonPath("$.data.total_rows").value(1))
                .andExpect(jsonPath("$.data.rows[0].id").value(userId("b")));
        // 看板码来自勾 B 的角色、查看账号码只来自普通角色：两个码都覆盖到的项目一个没有，0 行
        mockMvc.perform(get(headcountUrl).with(mixed))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary_value").value(0))
                .andExpect(jsonPath("$.data.total_rows").value(0))
                .andExpect(jsonPath("$.data.rows").isEmpty());
        // 普通员工：本人项目 A 的（守护）
        assertThat(headcount(plain)).containsExactlyInAnyOrderElementsOf(staffOfProjectA);
        // 勾全部：A、B 两边的员工都有；平台公司员工没有（治理地图「权限规则」：「平台公司员工、平台管理员、最高权限、外部机构账号一律碰不到」）
        assertThat(headcount(all)).containsAll(staffOfProjectA).contains(userId("b")).doesNotContain(userId("out"));
    }

    // J2（2.8 指标明细，没有项目列的指标）：酒吧订单数，登记项权限「查看酒吧订单」。两个码都经勾全部的角色持有的人看平台全量
    // （修复前 403，第一处断言即红）；逐个勾选者、普通员工、两个码里只有一个经勾全部的角色持有的人仍被拒（守护）。
    @Test
    void platformOnlyMetricDetailOpensOnlyToHoldersOfBothCodesThroughAllProjectsRoles() throws Exception {
        String url = "/api/v1/analytics/metric-detail?metric=bar.order_count";
        String[] codes = {"analytics.dashboard.view", "bar.order.view"};

        mockMvc.perform(get(url).with(staffOfA("all", allProjectsRole("all", codes))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metric_key").value("bar.order_count"));

        for (RequestPostProcessor denied : List.of(
                staffOfA("ticked", tickedBRole("ticked", codes)),
                staffOfA("plain", plainRole("plain", codes)),
                staffOfA("half", allProjectsRole("half_a", "analytics.dashboard.view"), plainRole("half_p", "bar.order.view")))) {
            mockMvc.perform(get(url).with(denied))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value("PLATFORM_METRIC_PROJECT_FORBIDDEN"));
        }
    }

    // J3（2.7 商户类分析按经营项目）：商户甲（主项目 A）、乙（主项目 B）、丙（主项目为空），各有健康分与经营汇总行。
    // 经营项目＝主项目加 merchant_projects 追加的项目（V441 表注释：「商户经营项目＝merchants.project_id（主项目）加本表」）。
    // 修复前不带商户归属的员工调这两个入口是 403（第一处即红）。
    @Test
    void merchantAnalyticsOnlyShowMerchantsOperatingInReachableProjects() throws Exception {
        String merchantA = merchant("a", projectA);
        String merchantB = merchant("b", projectB);
        String merchantNone = merchant("none", null);
        String code = "analytics.dashboard.view";
        // M737-F05：甲签了一份项目为空的合同（合同工作台起草不提交「所属综合体」，正常录入的合同都是这样），照常按经营项目出
        jdbcTemplate.update("""
                INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,
                        lifecycle_status, created_at, updated_at)
                VALUES (?, ?, 'subj_m737', 'subj_m737', DATE '2099-01-01', DATE '2099-12-31', 'drafting', now(), now())
                """, "ctr_m737_a_" + suffix, merchantA);

        // A 的普通员工：只有在 A 经营的商户
        RequestPostProcessor plain = staffOfA("plain", plainRole("plain", code));
        assertThat(merchantHealth(plain)).containsExactly(merchantA);
        assertThat(merchantSummaries(plain)).containsExactly(merchantA);

        // 只勾 B：只有在 B 经营的商户
        RequestPostProcessor ticked = staffOfA("ticked", tickedBRole("ticked", code));
        assertThat(merchantHealth(ticked)).containsExactly(merchantB);
        assertThat(merchantSummaries(ticked)).containsExactly(merchantB);

        // 勾全部：项目集合内经营的商户；主项目为空的商户不出
        RequestPostProcessor all = staffOfA("all", allProjectsRole("all", code));
        assertThat(merchantHealth(all)).containsExactlyInAnyOrder(merchantA, merchantB);
        assertThat(merchantSummaries(all)).containsExactlyInAnyOrder(merchantA, merchantB);

        // 商户甲追加经营项目 B 后，只勾 B 的人也看得到它
        jdbcTemplate.update("INSERT INTO merchant_projects (merchant_id, project_id) VALUES (?, ?)", merchantA, projectB);
        assertThat(merchantHealth(ticked)).containsExactlyInAnyOrder(merchantA, merchantB);
        assertThat(merchantSummaries(ticked)).containsExactlyInAnyOrder(merchantA, merchantB);

        // 带商户归属的账号：照旧只看本商户（守护）——归属 A、商户是丙，按项目出的话会是甲
        RequestPostProcessor merchantAccount = account("mch", "staff", projectA, merchantNone, plainRole("mch", code));
        assertThat(merchantHealth(merchantAccount)).containsExactly(merchantNone);
        assertThat(merchantSummaries(merchantAccount)).containsExactly(merchantNone);

        // 平台管理员：全部商户，含主项目为空的
        RequestPostProcessor admin = platformAdmin(code);
        assertThat(merchantHealth(admin)).containsExactlyInAnyOrder(merchantA, merchantB, merchantNone);
        assertThat(merchantSummaries(admin)).containsExactlyInAnyOrder(merchantA, merchantB, merchantNone);
    }

    // J4（2.6 指标快照逐段留）：很靠后的同一经营日有平台段、项目段 A 与 B、商户段甲（主项目 A）与乙（主项目 B）。
    // 修复前 A 的普通员工拿到全表，含平台段与 B 的两段（第一处断言即红）。
    @Test
    void metricSnapshotsOnlyShowReachableSegments() throws Exception {
        String merchantA = merchant("a", projectA);
        String merchantB = merchant("b", projectB);
        String platform = snapshot("pf", "platform", null);
        String ofProjectA = snapshot("pa", "project", projectA);
        String ofProjectB = snapshot("pb", "project", projectB);
        String ofMerchantA = snapshot("ma", "merchant", merchantA);
        String ofMerchantB = snapshot("mb", "merchant", merchantB);
        String code = "analytics.dashboard.view";

        // A 的普通员工：只有项目段 A 与在 A 经营的商户段，平台段不出
        assertThat(snapshots(staffOfA("plain", plainRole("plain", code)))).containsExactlyInAnyOrder(ofProjectA, ofMerchantA);
        // 勾全部：含平台段
        assertThat(snapshots(staffOfA("all", allProjectsRole("all", code))))
                .containsExactlyInAnyOrder(platform, ofProjectA, ofProjectB, ofMerchantA, ofMerchantB);
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private String userId(String tag) {
        return "usr_m737_" + tag + "_" + suffix;
    }

    /** 建一名账号、挂上给定角色，返回以其身份登录的会话（后台 Web 终端＋生产过滤器放入的 AccessActor 主体，四项与账号行一致）。 */
    private RequestPostProcessor account(String tag, String accountType, String organizationId, String merchantId,
                                         String... roleIds) {
        String userId = userId(tag);
        accessRepository.createUser(new AccessUser(userId, userId, accountType, organizationId, merchantId, "active", Instant.now()));
        for (String roleId : roleIds) {
            accessRepository.assignUserRole(userId, roleId);
        }
        AccessActor actor = new AccessActor(userId, userId, accountType, organizationId, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    /** 归属项目 A、不带商户归属的员工。 */
    private RequestPostProcessor staffOfA(String tag, String... roleIds) {
        return account(tag, "staff", projectA, null, roleIds);
    }

    /** 平台管理员；入口权限码另给一个明确的角色。 */
    private RequestPostProcessor platformAdmin(String... codes) {
        return account("admin", "platform_admin", "org", null, plainRole("admin", codes));
    }

    /** 普通角色：两项勾选都没有。 */
    private String plainRole(String tag, String... codes) {
        return role(tag, false, List.of(), codes);
    }

    /** 勾了「可以查看全部项目」的角色。 */
    private String allProjectsRole(String tag, String... codes) {
        return role(tag, true, List.of(), codes);
    }

    /** 只逐个勾选了项目 B 的角色。 */
    private String tickedBRole(String tag, String... codes) {
        return role(tag, false, List.of(projectB), codes);
    }

    private String role(String tag, boolean viewAll, List<String> ticked, String... codes) {
        String roleId = "role_m737_" + tag + "_" + suffix;
        accessRepository.createRole(new RoleRecord(roleId, "M737 " + tag + " " + suffix, null, "project", "active", Instant.now(),
                List.of(codes), null, viewAll, ticked));
        return roleId;
    }

    // 空置铺位：没有生效合同的商铺。面积取极大值——铺位列表按面积降序、最多 200 行，不过滤的人（勾全部、平台管理员）也要看得到夹具行。
    private String vacantSpace(String tag, String projectId) {
        String id = "sp_m737_" + tag + "_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO spaces (id, project_id, space_name, space_type, floor_no, area_square_meter, polygon_points,
                        status, created_at, updated_at)
                VALUES (?, ?, ?, 'shop', '1F', 90000000, '[]'::jsonb, 'active', now(), now())
                """, id, projectId, id);
        return id;
    }

    // 一笔逾期未清应收，返回欠费商户编号（欠费列表按商户汇总，每笔用各自的商户编号）。金额取极大值，理由同上（按未清金额降序、最多 200 行）。
    private String overdueReceivable(String tag, String projectId) {
        String id = "ar_m737_" + tag + "_" + suffix;
        String merchantId = "mch_m737_ar_" + tag + "_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, amount_cent, paid_amount_cent,
                        unpaid_amount_cent, currency, due_date, status, created_at, updated_at, merchant_id, project_id)
                VALUES (?, ?, 'receivable', ?, 9000000000000, 0, 9000000000000, 'CNY', current_date - 10, 'open',
                        now(), now(), ?, ?)
                """, id, id, merchantId, merchantId, projectId);
        return merchantId;
    }

    /** 一个商户（主项目可为空），带一行健康分、一行经营汇总。 */
    private String merchant(String tag, String projectId) {
        String id = "mch_m737_" + tag + "_" + suffix;
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                + "VALUES (?, ?, 'catering', 'active', now(), now(), ?)", id, id, projectId);
        jdbcTemplate.update("""
                INSERT INTO analytics_merchant_health_scores (merchant_id, score, score_level, risk_count,
                        overdue_ar_amount_cents, last_reviewed_at, summary, created_at, updated_at)
                VALUES (?, 80, 'healthy', 0, 0, now(), 'M737', now(), now())
                """, id);
        jdbcTemplate.update("""
                INSERT INTO analytics_merchant_summaries (merchant_id, merchant_name, merchant_type, revenue_amount_cents,
                        order_count, risk_alert_count, settlement_status, business_date, created_at, updated_at)
                VALUES (?, ?, 'catering', 100, 1, 0, 'settled', current_date, now(), now())
                """, id, id);
        return id;
    }

    /** 2099-12-31 的一行指标快照（指标键带随机后缀，不与库里的行撞唯一键），返回快照编号。 */
    private String snapshot(String tag, String segmentType, String segmentId) {
        String id = "snap_m737_" + tag + "_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO analytics_business_metric_snapshots (snapshot_id, metric_key, domain, period_type, business_date,
                        segment_type, segment_id, actual_value, trend_direction, projection_method, created_at)
                VALUES (?, ?, 'analytics', 'daily', DATE '2099-12-31', ?, ?, 1, 'flat', 'etl_aggregate', now())
                """, id, "m737_" + suffix, segmentType, segmentId);
        return id;
    }

    // ── 取数 ─────────────────────────────────────────────────────────────────────────────────────

    /** 以给定身份 GET（须 200），取出该 JSON 路径下属于本次夹具的编号（都以随机后缀结尾）；共享库里别的行不看。 */
    private List<String> mine(RequestPostProcessor session, String url, String path) throws Exception {
        List<String> ids = JsonPath.read(mockMvc.perform(get(url).with(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), path);
        return ids.stream().filter(id -> id != null && id.endsWith(suffix)).toList();
    }

    private List<String> vacantSpaces(RequestPostProcessor session) throws Exception {
        return mine(session, "/api/v1/analytics/occupancy/vacant-spaces", "$.data.items[*].space_id");
    }

    private List<String> debtors(RequestPostProcessor session) throws Exception {
        return mine(session, "/api/v1/analytics/finance-receivables/debtors", "$.data.items[*].merchant_id");
    }

    private List<String> headcount(RequestPostProcessor session) throws Exception {
        return mine(session, headcountUrl, "$.data.rows[*].id");
    }

    private List<String> merchantHealth(RequestPostProcessor session) throws Exception {
        return mine(session, "/api/v1/analytics/merchant-health", "$.data.items[*].merchant_id");
    }

    private List<String> merchantSummaries(RequestPostProcessor session) throws Exception {
        return mine(session, "/api/v1/analytics/merchants", "$.data.items[*].merchant_id");
    }

    private List<String> snapshots(RequestPostProcessor session) throws Exception {
        return mine(session, "/api/v1/analytics/business-metric-snapshots", "$.data.items[*].snapshot_id");
    }
}
