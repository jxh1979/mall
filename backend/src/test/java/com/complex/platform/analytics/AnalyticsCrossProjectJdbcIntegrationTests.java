package com.complex.platform.analytics;

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
import java.time.LocalDate;
import java.time.ZoneOffset;
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

    @Autowired
    private com.complex.platform.common.BusinessDayResolver businessDayResolver;

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

        // 以上三种人都看不到项目列为空的那一行；M737-F07：勾了全部项目的人也一样（只到项目为止），只有平台管理员不按项目列过滤
        RequestPostProcessor all = staffOfA("all", allProjectsRole("all", codes));
        assertThat(vacantSpaces(all)).containsExactlyInAnyOrder(spaceA, spaceB);
        assertThat(debtors(all)).containsExactlyInAnyOrder(debtorA, debtorB);
        RequestPostProcessor admin = platformAdmin(codes);
        assertThat(vacantSpaces(admin)).containsExactlyInAnyOrder(spaceA, spaceB);
        assertThat(debtors(admin)).containsExactlyInAnyOrder(debtorA, debtorB, debtorNone);
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

    // J5（M737-F06 租售比逐商户行）：商户甲（主项目 A）与「无」（主项目为空——V432 不回填历史商户的项目）同一账期各有一笔合同租金（100 分、900 分）。
    // 逐商户行只有平台管理员不过滤；勾了全部项目的人也只出有经营项目的商户，平台合计仍含全部商户。修复前勾全部的人拿到「无」一行（第一处断言即红）。
    @Test
    void rentToSalesRowsForAllProjectsHoldersOnlyShowMerchantsOperatingInProjects() throws Exception {
        String merchantA = merchant("a", projectA);
        String merchantNone = merchant("none", null);
        rent("a", merchantA, 100);
        rent("none", merchantNone, 900);

        RequestPostProcessor all = staffOfA("all", allProjectsRole("all", "contract.view"));
        assertThat(rentToSalesMerchants(all)).containsExactly(merchantA);
        assertThat(platformRentCent(all)).isEqualTo(1000L);

        RequestPostProcessor admin = platformAdmin("contract.view");
        assertThat(rentToSalesMerchants(admin)).containsExactlyInAnyOrder(merchantA, merchantNone);
    }

    // J6（M737-F07 商户服务工单超时明细）：商户甲（主项目 A）与「无」（主项目为空——V432 不回填历史商户的项目）各有一张今天到期、已标超时的服务工单。
    // 勾了全部项目的人只看到有经营项目的商户的工单；平台管理员两张都看到。修复前勾全部的人拿到「无」那张（第一处断言即红）。
    @Test
    void overdueMerchantWorkOrdersForAllProjectsHoldersOnlyShowMerchantsOperatingInProjects() throws Exception {
        String merchantA = merchant("a", projectA);
        String merchantNone = merchant("none", null);
        String woA = overdueMerchantWorkOrder("a", merchantA);
        String woNone = overdueMerchantWorkOrder("none", merchantNone);

        RequestPostProcessor all = staffOfA("all", allProjectsRole("all", "facility.work_order.manage"));
        assertThat(overdueMerchantWorkOrders(all)).containsExactly(woA);

        RequestPostProcessor admin = platformAdmin("facility.work_order.manage");
        assertThat(overdueMerchantWorkOrders(admin)).containsExactlyInAnyOrder(woA, woNone);
    }

    // J7（M737-F08 复盘锁重试）：A 的员工只经角色「可查看 A、B」持有 analytics.review.lock，用提交编号 K 建锁（建在 A），
    // 同一编号立刻重试返回同一把锁。角色的可查看项目减掉 A（减少审批生效后的状态：直接删 role_viewable_projects 那一行）后，
    // 同一编号、同一内容重试被拒，不返回 A 的锁、不新增锁。修复前减掉 A 后的重试拿到 A 的锁（减掉 A 之后第一处断言即红）。
    @Test
    void reviewLockRetryAfterOwnProjectLeavesReachIsDenied() throws Exception {
        String reduced = role("reduced", false, List.of(projectA, projectB), "analytics.review.lock");
        RequestPostProcessor staff = staffOfA("lock", reduced);
        String key = "m737-lock-" + suffix;
        String body = """
                {"idempotency_key": "%s", "domain": "all", "operator_note": "月度复盘", "metric_count": 3}
                """.formatted(key);
        String lockId = JsonPath.read(createReviewLock(staff, body).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        assertThat(reviewLockProject(key)).isEqualTo(projectA);

        // 仍可触及 A：同一编号重试照旧返回第一次的锁，不新增
        String again = JsonPath.read(createReviewLock(staff, body).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        assertThat(again).isEqualTo(lockId);
        assertThat(reviewLockCount(key)).isEqualTo(1);

        // 角色的可查看项目减掉 A 并生效：同一编号重试被拒，不返回 A 的锁，锁数量不变
        jdbcTemplate.update("DELETE FROM role_viewable_projects WHERE role_id = ? AND viewable_id = ?", reduced, projectA);
        createReviewLock(staff, body)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_ORG_FORBIDDEN"));
        assertThat(reviewLockCount(key)).isEqualTo(1);
    }

    // J8（M737-F10 复盘锁的商户、外包账号）：A 的员工先建一把锁；归属 A 的商户账号与外包账号只经「员工单独授予」（不挂角色）持有
    // analytics.review.lock，列表与新建都 403 PLATFORM_SCOPE_REQUIRED、原提示，不建锁。修复前商户账号的列表拿到 200（第一处断言即红）。
    @Test
    void reviewLocksDenyMerchantAndOutsourcedAccountsGrantedThePermission() throws Exception {
        RequestPostProcessor staff = staffOfA("owner", plainRole("owner", "analytics.review.lock"));
        createReviewLock(staff, reviewLockBody("m737-lock-owner-" + suffix)).andExpect(status().isOk());

        assertReviewLocksDenied("mch", grantedReviewLock("mch", "merchant", merchant("lock", projectA)), "商户账号无权查看平台级经营分析");
        assertReviewLocksDenied("vendor", grantedReviewLock("vendor", "outsourced_vendor", null), "外包供应商无权查看平台级数据");
    }

    // ROOT-04-R2（BSM「采购、库管和酒吧店长看采购周期……」；BSM「项目级操作人员只看自己所在项目范围内的 KPI 和明细」；裁决 M01）：
    // A、B 各一次到货，A 的普通员工按前端真实请求只拿到 A 的到货行与按商品及单位的分组。修复前该指标未登记，400（第一处断言即红）。
    @Test
    void receivedDetailByRealQueryShapeOnlyShowsReachableProject() throws Exception {
        java.time.LocalDate day = java.time.LocalDate.of(2098, 3, 10);
        java.time.Instant at = businessDayResolver.startInstant("inventory", day).plusSeconds(3600);
        String receiptA = purchaseReceipt("a", projectA, 12, 10, 2, at);
        purchaseReceipt("b", projectB, 3, 3, 0, at);
        RequestPostProcessor plain = staffOfA("plain", plainRole("plain", "analytics.dashboard.view", "inventory.purchase_request.view"));

        mockMvc.perform(get("/api/v1/analytics/metric-detail?metric=supply.received&from=" + day + "&to=" + day + "&page=1&size=20").with(plain))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_rows").value(1))
                .andExpect(jsonPath("$.data.rows[0].id").value(receiptA))
                .andExpect(jsonPath("$.data.rows[0].projectId").value(projectA))
                .andExpect(jsonPath("$.data.group_columns[0].key").value("itemName"))
                .andExpect(jsonPath("$.data.groups.length()").value(1))
                .andExpect(jsonPath("$.data.groups[0].acceptedQuantity").value(10))
                .andExpect(jsonPath("$.data.groups[0].rejectedQuantity").value(2));
    }

    // ROOT-04-R3（BSM「风险：待复核、异常事件、补录数量、第三方差异。」；裁决 M02「补录提交即计数，状态另列」）：
    // A、B 各一笔锁账后补录，A 的普通员工按前端真实请求只拿到 A 的一笔。修复前该指标未登记，400（第一处断言即红）。
    @Test
    void manualBackfillDetailByRealQueryShapeOnlyShowsReachableProject() throws Exception {
        java.time.LocalDate day = java.time.LocalDate.of(2098, 3, 10);
        java.time.Instant at = businessDayResolver.startInstant("governance", day).plusSeconds(3600);
        String backfillA = lockedBackfill("a", projectA, at);
        lockedBackfill("b", projectB, at);
        RequestPostProcessor plain = staffOfA("plain", plainRole("plain", "analytics.dashboard.view", "governance.supervisor_review.view"));

        mockMvc.perform(get("/api/v1/analytics/metric-detail?metric=risk.manual_backfill&from=" + day + "&to=" + day + "&page=1&size=20").with(plain))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary_value").value(1))
                .andExpect(jsonPath("$.data.rows[0].id").value(backfillA))
                .andExpect(jsonPath("$.data.rows[0].sourceType").value("locked_backfill"))
                .andExpect(jsonPath("$.data.rows[0].status").value("pending"));
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    /** 一次实际到货（V485 purchase_receipts 一行），返回到货记录编号。 */
    private String purchaseReceipt(String tag, String projectId, int arrived, int accepted, int rejected, java.time.Instant at) {
        String s = tag + "_" + suffix;
        jdbcTemplate.update("INSERT INTO inventory_suppliers (id, supplier_name, status, created_at, updated_at) VALUES (?, '供应商', 'active', now(), now())", "sup_" + s);
        jdbcTemplate.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, created_at, updated_at) VALUES (?, '酒', 'beverage', '瓶', 'active', now(), now())", "item_" + s);
        jdbcTemplate.update("INSERT INTO warehouses (id, project_id, warehouse_type, name, status) VALUES (?, ?, 'main', '主仓', 'active')", "wh_" + s, projectId);
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, status, created_at) VALUES (?, ?, 'staff', ?, 'active', now())", "usr_rcv_" + s, "usr_rcv_" + s, projectId);
        jdbcTemplate.update("INSERT INTO inventory_purchase_requests (id, request_no, supplier_id, item_id, quantity, status, project_id, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, 'arrived', ?, now(), now())", "pr_" + s, "pr_" + s, "sup_" + s, "item_" + s, arrived, projectId);
        jdbcTemplate.update("""
                INSERT INTO purchase_receipts (id, purchase_id, project_id, supplier_id, item_id, warehouse_id, location_id,
                        arrived_quantity, accepted_quantity, rejected_quantity, unit_cost_cent, received_at, received_by, evidence_attachment_ids)
                VALUES (?, ?, ?, ?, ?, ?, 'loc_main', ?, ?, ?, 100, ?, ?, '[]'::jsonb)
                """, "prec_" + s, "pr_" + s, projectId, "sup_" + s, "item_" + s, "wh_" + s, arrived, accepted, rejected,
                java.sql.Timestamp.from(at), "usr_rcv_" + s);
        return "prec_" + s;
    }

    /** 一笔待审的锁账后补录（应收挂在给定项目、已锁账），返回补录编号。 */
    private String lockedBackfill(String tag, String projectId, java.time.Instant at) {
        String s = tag + "_" + suffix;
        jdbcTemplate.update("INSERT INTO finance_daily_closes (id, business_date, scope_type, scope_id, force_close, status, locked_at, created_at) "
                + "VALUES (?, DATE '2098-03-10', 'project', ?, FALSE, 'locked', now(), now())", "close_" + s, projectId);
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id, amount_cent, paid_amount_cent,
                        unpaid_amount_cent, currency, due_date, status, created_at, updated_at, project_id, locked_day_close_id)
                VALUES (?, ?, 'receivable', 'cp', 'manual', ?, 100, 0, 100, 'CNY', DATE '2098-03-10', 'pending_collection', now(), now(), ?, ?)
                """, "ar_bf_" + s, "ar_bf_" + s, "ar_bf_" + s, projectId, "close_" + s);
        jdbcTemplate.update("""
                INSERT INTO ar_ap_adjustments (id, ar_ap_item_id, locked_day_close_id, adjustment_amount_cent, approval_status,
                        requested_by, reason, created_at)
                VALUES (?, ?, ?, 30, 'pending', 'usr_bf', '核实少记', ?)
                """, "adj_" + s, "ar_bf_" + s, "close_" + s, java.sql.Timestamp.from(at));
        return "adj_" + s;
    }

    /** 一张今天到期、已标超时的商户服务工单（超时明细取当日窗口）。 */
    private String overdueMerchantWorkOrder(String tag, String merchantId) {
        String id = "mwo_m737_" + tag + "_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO merchant_service_work_orders (id, merchant_id, work_order_type, priority, status, title, description,
                        reported_by_user_id, reported_channel, due_at, created_at, updated_at, sla_breached)
                VALUES (?, ?, 'repair', 'high', 'created', ?, 'M737', 'usr_m737', 'web', now(), now(), now(), TRUE)
                """, id, merchantId, id);
        return id;
    }

    /** 2098-07 账期的一笔合同租金应收（租售比分子）；账期远在未来，库里没有同期的行。 */
    private void rent(String tag, String merchantId, long cent) {
        String id = "ar_m737_rent_" + tag + "_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id, amount_cent, paid_amount_cent,
                        unpaid_amount_cent, currency, due_date, accounting_period, status, created_at, updated_at, merchant_id)
                VALUES (?, ?, 'receivable', ?, 'contract_rent', ?, ?, 0, ?, 'CNY', DATE '2098-07-31', '2098-07', 'pending_collection',
                        now(), now(), ?)
                """, id, id, merchantId, id, cent, cent, merchantId);
    }

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

    private org.springframework.test.web.servlet.ResultActions createReviewLock(RequestPostProcessor session, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/analytics/review-locks").with(session)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String reviewLockProject(String key) {
        return jdbcTemplate.queryForObject("SELECT project_id FROM business_review_locks WHERE idempotency_key = ?", String.class, key);
    }

    private int reviewLockCount(String key) {
        Integer n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM business_review_locks WHERE idempotency_key = ?", Integer.class, key);
        return n == null ? 0 : n;
    }

    private static String reviewLockBody(String key) {
        return """
                {"idempotency_key": "%s", "domain": "all", "operator_note": "月度复盘", "metric_count": 3}
                """.formatted(key);
    }

    /** 归属 A 的账号，只经「员工单独授予」持有 analytics.review.lock（不挂角色）。 */
    private RequestPostProcessor grantedReviewLock(String tag, String accountType, String merchantId) {
        RequestPostProcessor session = account(tag, accountType, projectA, merchantId);
        accessRepository.saveUserPermissionOverrides(userId(tag), List.of("analytics.review.lock"), AccessRepository.Effect.GRANT,
                "M737-F10", userId("owner"));
        return session;
    }

    /** 复盘锁列表与新建都 403 PLATFORM_SCOPE_REQUIRED、原提示，不建锁。 */
    private void assertReviewLocksDenied(String tag, RequestPostProcessor session, String message) throws Exception {
        mockMvc.perform(get("/api/v1/analytics/review-locks").with(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PLATFORM_SCOPE_REQUIRED"))
                .andExpect(jsonPath("$.message").value(message));
        String key = "m737-lock-" + tag + "-" + suffix;
        createReviewLock(session, reviewLockBody(key))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PLATFORM_SCOPE_REQUIRED"))
                .andExpect(jsonPath("$.message").value(message));
        assertThat(reviewLockCount(key)).isZero();
    }

    private List<String> overdueMerchantWorkOrders(RequestPostProcessor session) throws Exception {
        return mine(session, "/api/v1/analytics/work-order-sla/overdue-orders?source=merchant", "$.data.items[*].work_order_id");
    }

    private List<String> rentToSalesMerchants(RequestPostProcessor session) throws Exception {
        return mine(session, "/api/v1/analytics/rent-to-sales?month=2098-07", "$.data.by_merchant[*].key");
    }

    private long platformRentCent(RequestPostProcessor session) throws Exception {
        Number cent = JsonPath.read(mockMvc.perform(get("/api/v1/analytics/rent-to-sales?month=2098-07").with(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.platform.rent_cent");
        return cent.longValue();
    }
}
