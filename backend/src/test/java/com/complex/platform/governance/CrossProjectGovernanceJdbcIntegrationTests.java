package com.complex.platform.governance;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
import com.complex.platform.leasing.LeasingBrandRecord;
import com.complex.platform.leasing.LeasingFollowUpRecord;
import com.complex.platform.leasing.LeasingIntentRecord;
import com.complex.platform.leasing.LeasingLeadRecord;
import com.complex.platform.leasing.LeasingRepository;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 E 段第 19 条 19-R2（平台治理各入口与招商按可跨项目角色判）真库＋真实接口＋真实角色勾选回归，请求体一律按前端蛇形键。
 * 依据：治理地图第 659 行「被分配了可跨项目角色的员工（含经岗位得到），在所分配的权限内可以查看、处理该角色可查看项目的数据。」
 * 「逐个勾选的只能查看被选中的项目；无论哪种，各项目中的权限都一样。」「角色能查看哪些项目，持有这类角色的员工就能在哪些项目新建记录」
 * 「商户账号持有这类角色也不能跨项目」；第 528 行「不受第④类「别的项目」限制」。
 *
 * <p>夹具随测试事务回滚：项目 A、B、C 三行（编号带随机后缀），操作人都是归属 A 的员工；角色经权限仓储建立并分配
 * （只逐个勾 B、勾全部、两项都没勾的普通角色）。共享库里有别的数据：断言只看本次夹具建的行（编号都以随机后缀结尾）。
 * 被拒请求的失败留痕是独立事务，按随机后缀隔离留在库里。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class CrossProjectGovernanceJdbcIntegrationTests {

    private static final String[] VIEW_CODES = {"entity.binding.view", "merchant.profile.view", "governance.project.view",
            "governance.space.view", "floor_plan.view", "partner.org.view", "leasing.lead.manage", "leasing.intent.manage",
            "leasing.brand.manage", "customer.loyalty.view"};

    @Autowired private AccessRepository accessRepository;
    @Autowired private PlatformRepository platformRepository;
    @Autowired private LeasingRepository leasingRepository;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_g19_a_" + suffix;
    private final String projectB = "prj_g19_b_" + suffix;
    private final String projectC = "prj_g19_c_" + suffix;

    @BeforeEach
    void createProjects() {
        for (String projectId : List.of(projectA, projectB, projectC)) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', ?, 'active', now(), now())", projectId, "G19 " + projectId);
        }
    }

    // 列表：只逐个勾 B 的人各列表只出 B 的行，不出本人项目 A（该码没有别的来源）与没勾的 C；普通员工照旧只出 A（守护）。
    // 修复前只勾 B 的人拿到的是 A 的行（第一处断言即红）。
    @Test
    void listsOnlyShowRowsOfTheProjectsTheRoleReaches() throws Exception {
        for (String p : List.of("a", "b", "c")) {
            seedProjectRows(p, project(p));
        }
        RequestPostProcessor ticked = staffOfA("ticked", tickedBRole("ticked", VIEW_CODES));

        assertThat(mine(ticked, "/api/v1/platform/merchants", "$.data.items[*].id")).containsExactly(id("mch", "b"));
        assertThat(mine(ticked, "/api/v1/platform/business-entities", "$.data.items[*].id")).containsExactly(id("be", "b"));
        assertThat(mine(ticked, "/api/v1/platform/projects", "$.data.items[*].id")).containsExactly(projectB);
        assertThat(mine(ticked, "/api/v1/platform/spaces", "$.data.items[*].id")).containsExactly(id("sp", "b"));
        assertThat(mine(ticked, "/api/v1/platform/business-entity-bindings", "$.data.items[*].merchant_id")).containsExactly(id("mch", "b"));
        assertThat(mine(ticked, "/api/v1/platform/venue-business-hours", "$.data.items[*].venue_id")).containsExactly(id("sp", "b"));
        assertThat(mine(ticked, "/api/v1/floor-plans", "$.data.items[*].id")).containsExactly(id("fp", "b"));
        assertThat(mine(ticked, "/api/v1/partner/orgs", "$.data.items[*].id")).containsExactly(id("po", "b"));
        assertThat(mine(ticked, "/api/v1/leasing/leads", "$.data.items[*].id")).containsExactly(id("lead", "b"));
        assertThat(mine(ticked, "/api/v1/leasing/leads/follow-up-due", "$.data.items[*].id")).containsExactly(id("lead", "b"));
        assertThat(mine(ticked, "/api/v1/leasing/intents", "$.data.items[*].id")).containsExactly(id("int", "b"));
        assertThat(mine(ticked, "/api/v1/leasing/brands", "$.data.items[*].id")).containsExactly(id("brand", "b"));
        assertThat(mine(ticked, "/api/v1/leasing/follow-ups", "$.data.items[*].id")).containsExactly(id("fu", "b"));
        assertThat(mine(ticked, "/api/v1/leasing/blacklist-exceptions", "$.data.items[*].id")).containsExactly(id("lbe", "b"));
        assertThat(mine(ticked, "/api/v1/loyalty/config/check-in-tiers?project_id=" + projectB, "$.data.items[*].id"))
                .containsExactly(id("tier", "b"));

        // 普通员工（本人项目 A、没有可跨项目角色）：照旧只出 A
        RequestPostProcessor plain = staffOfA("plain", plainRole("plain", VIEW_CODES));
        assertThat(mine(plain, "/api/v1/platform/merchants", "$.data.items[*].id")).containsExactly(id("mch", "a"));
        assertThat(mine(plain, "/api/v1/platform/spaces", "$.data.items[*].id")).containsExactly(id("sp", "a"));
        assertThat(mine(plain, "/api/v1/leasing/leads", "$.data.items[*].id")).containsExactly(id("lead", "a"));
        assertThat(mine(plain, "/api/v1/loyalty/config/check-in-tiers?project_id=" + projectB, "$.data.items[*].id"))
                .containsExactly(id("tier", "a"));
    }

    // 18E 复核：只逐个勾 B 的人，本人项目 A 也不在该码可触及范围内（该码没有别的来源）——签到阶梯不带 project_id、带不可触及的 C
    // 都 403，不回退到 A、不写进 A；普通员工照旧忽略不可触及的 project_id、落本人项目（守护）。修复前不带 project_id 返回 A 的档位（200，第一处断言即红）。
    @Test
    void checkInTiersDoNotFallBackToAnOwnProjectTheRoleDoesNotReach() throws Exception {
        jdbc.update("INSERT INTO customer_check_in_tiers (id, project_id, streak_days, award_points, status) VALUES (?, ?, 3, 10, 'active')",
                id("tier", "a"), projectA);
        RequestPostProcessor ticked = staffOfA("lt", tickedBRole("lt", "customer.loyalty.view", "customer.loyalty.manage"));

        mockMvc.perform(get("/api/v1/loyalty/config/check-in-tiers").with(ticked))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
        mockMvc.perform(get("/api/v1/loyalty/config/check-in-tiers?project_id=" + projectC).with(ticked))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
        send(ticked, post("/api/v1/loyalty/config/check-in-tiers"),
                "{\"project_id\":\"" + projectC + "\",\"streak_days\":7,\"award_points\":10}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
        send(ticked, post("/api/v1/loyalty/config/check-in-tiers"), "{\"streak_days\":8,\"award_points\":10}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_check_in_tiers WHERE project_id IN (?, ?) AND streak_days IN (7, 8)",
                Integer.class, projectA, projectC)).isZero();

        RequestPostProcessor plain = staffOfA("lp", plainRole("lp", "customer.loyalty.view", "customer.loyalty.manage"));
        send(plain, post("/api/v1/loyalty/config/check-in-tiers"),
                "{\"project_id\":\"" + projectC + "\",\"streak_days\":7,\"award_points\":10}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectA));
    }

    // 勾全部：看到与各项目关联的主体；没关联任何项目的主体不出（只到项目为止）。修复前只出本人项目 A 关联的（第一处断言即红）。
    @Test
    void allProjectsHolderSeesEntitiesAssociatedWithAnyProject() throws Exception {
        for (String p : List.of("a", "b", "c")) {
            businessEntity(p, project(p));
        }
        businessEntity("none", null);
        RequestPostProcessor all = staffOfA("all", allProjectsRole("all", "entity.binding.view"));
        assertThat(mine(all, "/api/v1/platform/business-entities", "$.data.items[*].id"))
                .contains(id("be", "a"), id("be", "b"), id("be", "c")).doesNotContain(id("be", "none"));
    }

    // 既有对象写：只勾 B 的人改 B 的商户档案、配置 B 酒吧待清台时限、给 B 场地写营业时间、启用 B 商户的既有主体绑定、存档 B 商户均 200；
    // 对 C 的同样请求 403 且不写库。同一幂等键在角色去掉 B 后重放 403、不返回第一次结果（范围闸在幂等之前）。修复前对 B 一律 403（第一处即红）。
    @Test
    void writesOnExistingObjectsFollowTheRoleAndTheGateRunsBeforeIdempotency() throws Exception {
        for (String p : List.of("b", "c")) {
            seedProjectRows(p, project(p));
        }
        String role = tickedBRole("w", "merchant.profile.manage", "entity.binding.manage", "governance.space.manage");
        RequestPostProcessor ticked = staffOfA("w", role);

        send(ticked, put("/api/v1/platform/merchants/" + id("mch", "b") + "/pending-clear-alert"),
                "{\"idempotency_key\":\"pca-b-" + suffix + "\",\"value_minutes\":20,\"min_minutes\":5,\"max_minutes\":30}")
                .andExpect(status().isOk());
        send(ticked, put("/api/v1/platform/merchants/" + id("mch", "c") + "/pending-clear-alert"),
                "{\"idempotency_key\":\"pca-c-" + suffix + "\",\"value_minutes\":20,\"min_minutes\":5,\"max_minutes\":30}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
        assertThat(jdbc.queryForObject("SELECT pending_clear_alert_minutes FROM merchants WHERE id=?", Integer.class, id("mch", "c"))).isNull();

        send(ticked, put("/api/v1/platform/merchants/" + id("mch", "b")), merchantProfile("upd-b", "B 改名"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.merchant_name").value("B 改名"));
        send(ticked, put("/api/v1/platform/merchants/" + id("mch", "c")), merchantProfile("upd-c", "C 改名"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
        assertThat(jdbc.queryForObject("SELECT merchant_name FROM merchants WHERE id=?", String.class, id("mch", "c"))).isEqualTo(id("mch", "c"));

        send(ticked, post("/api/v1/platform/venue-business-hours"), venueHours("vbh-b", id("sp", "b"))).andExpect(status().isOk());
        send(ticked, post("/api/v1/platform/venue-business-hours"), venueHours("vbh-c", id("sp", "c")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM venue_business_hours WHERE venue_id=? AND day_of_week=2",
                Integer.class, id("sp", "c"))).isZero();

        send(ticked, post("/api/v1/platform/business-entity-bindings/" + bindingOf(id("mch", "b")) + "/change"), bindingChange("bind-b"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("active"));
        send(ticked, post("/api/v1/platform/business-entity-bindings/" + bindingOf(id("mch", "c")) + "/change"), bindingChange("bind-c"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
        assertThat(jdbc.queryForObject("SELECT status FROM business_entity_bindings WHERE merchant_id=?", String.class, id("mch", "c")))
                .isEqualTo("inactive");

        String archiveB = archive("arc-b");
        send(ticked, post("/api/v1/platform/merchants/" + id("mch", "b") + "/archive"), archiveB).andExpect(status().isOk());
        send(ticked, post("/api/v1/platform/merchants/" + id("mch", "c") + "/archive"), archive("arc-c"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
        assertThat(jdbc.queryForObject("SELECT archived_at FROM merchants WHERE id=?", java.sql.Timestamp.class, id("mch", "c"))).isNull();

        // 角色去掉 B：同一幂等键重放被拒，不返回第一次的结果
        jdbc.update("DELETE FROM role_viewable_projects WHERE role_id=? AND viewable_id=?", role, projectB);
        send(ticked, post("/api/v1/platform/merchants/" + id("mch", "b") + "/archive"), archiveB)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
    }

    // 新建（蛇形请求体）：只勾 B 的人在 B 建空间、商户、线索（带 B 的意向铺位不带 project_id，或带 project_id）、品牌、签到阶梯都落 B；
    // 在 C 建空间 403 PROJECT_SCOPE_MISMATCH 不落行。修复前在 B 建空间 403（第一处即红）。
    @Test
    void holderCreatesRecordsInTheProjectsTheRoleReaches() throws Exception {
        String spaceB = space("b", projectB);
        RequestPostProcessor ticked = staffOfA("c", tickedBRole("c", "governance.space.manage", "merchant.profile.manage",
                "leasing.lead.manage", "leasing.brand.manage", "customer.loyalty.manage"));

        send(ticked, post("/api/v1/platform/spaces"), spaceBody("sp-b", "B 新空间 " + suffix, projectB))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));
        send(ticked, post("/api/v1/platform/spaces"), spaceBody("sp-c", "C 新空间 " + suffix, projectC))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM spaces WHERE space_name=?", Integer.class, "C 新空间 " + suffix)).isZero();

        send(ticked, post("/api/v1/platform/merchants"), "{\"idempotency_key\":\"mc-" + suffix + "\",\"merchant_name\":\"B 新商户\","
                + "\"merchant_type\":\"catering\",\"contact_phone\":\"13800000000\",\"project_id\":\"" + projectB + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));

        send(ticked, post("/api/v1/leasing/leads"), "{\"idempotency_key\":\"ll1-" + suffix + "\",\"contact_name\":\"王经理\","
                + "\"source\":\"outbound\",\"intended_space_id\":\"" + spaceB + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));
        send(ticked, post("/api/v1/leasing/leads"), "{\"idempotency_key\":\"ll2-" + suffix + "\",\"contact_name\":\"李经理\","
                + "\"source\":\"outbound\",\"project_id\":\"" + projectB + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));
        send(ticked, post("/api/v1/leasing/brands"), "{\"idempotency_key\":\"lb-" + suffix + "\",\"brand_name\":\"B 新品牌\","
                + "\"brand_type\":\"prospective\",\"business_type\":\"retail\",\"project_id\":\"" + projectB + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));
        assertThat(failureCount("lb-" + suffix)).isZero();
        // 第十八批 I 段第 90 条 90-R2（PGM L738）：在够不着的 C 建品牌 403、不落行，落 leasing.brand.create_failed
        send(ticked, post("/api/v1/leasing/brands"), "{\"idempotency_key\":\"lbc-" + suffix + "\",\"brand_name\":\"C 新品牌 " + suffix
                + "\",\"brand_type\":\"prospective\",\"business_type\":\"retail\",\"project_id\":\"" + projectC + "\"}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM leasing_brands WHERE brand_name=?", Integer.class, "C 新品牌 " + suffix))
                .isZero();
        assertThat(failures("leasing.brand.create_failed", "lbc-" + suffix)).containsExactly(Map.of("actor_id", userId("c"),
                "business_id", "new", "error_code", "CROSS_PROJECT_FORBIDDEN"));
        send(ticked, post("/api/v1/loyalty/config/check-in-tiers"),
                "{\"project_id\":\"" + projectB + "\",\"streak_days\":5,\"award_points\":10}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));
    }

    // 守护＋原有缺陷回归（待开发清单第 79 条）：普通员工新建商户不带 project_id 落本人项目；新建线索与品牌落本人项目、在本人列表里看得到
    // （修复前线索与品牌的 project_id 落空，本项目员工看不到，第一处线索断言即红）。
    @Test
    void plainStaffCreationsLandInOwnProjectAndShowInOwnLists() throws Exception {
        RequestPostProcessor plain = staffOfA("p", plainRole("p", "merchant.profile.manage", "leasing.lead.manage",
                "leasing.brand.manage"));

        String lead = JsonPath.read(send(plain, post("/api/v1/leasing/leads"), "{\"idempotency_key\":\"pl-" + suffix
                + "\",\"contact_name\":\"赵经理\",\"source\":\"referral\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectA))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        assertThat(ids(plain, "/api/v1/leasing/leads")).contains(lead);

        String brand = JsonPath.read(send(plain, post("/api/v1/leasing/brands"), "{\"idempotency_key\":\"pb-" + suffix
                + "\",\"brand_name\":\"本项目品牌\",\"brand_type\":\"intentional\",\"business_type\":\"retail\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectA))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        assertThat(ids(plain, "/api/v1/leasing/brands")).contains(brand);

        send(plain, post("/api/v1/platform/merchants"), "{\"idempotency_key\":\"pm-" + suffix + "\",\"merchant_name\":\"A 新商户\","
                + "\"merchant_type\":\"retail\",\"contact_phone\":\"13800000001\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectA));
    }

    // 第 19 条补充：黑名单例外申请的项目闸在幂等之前——只勾 B 的人为 B 的线索申请（由线索新建意向）200；角色去掉 B 后同一幂等键重放 403，
    // 不返回第一次的结果、不新增放行单。修复前闸在幂等执行体内，重放直接拿到第一次的结果（200，断言即红）。
    @Test
    void blacklistExceptionReplayAfterLosingTheProjectIsForbidden() throws Exception {
        String role = tickedBRole("x", "leasing.brand.blacklist_exception", "leasing.lead.manage");
        RequestPostProcessor applicant = staffOfA("x", role);
        staffOfA("boss", plainRole("boss", "leasing.lead.manage"));
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId("x"), projectA);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId("boss"), projectA);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", userId("x"), userId("boss"));
        String brand = brand("x", projectB);
        String lead = lead("x", projectB, brand, Instant.now());
        String body = "{\"target_type\":\"intent_create\",\"lead_id\":\"" + lead + "\",\"reason\":\"品牌整改完毕，经营方确需继续洽谈\","
                + "\"idempotency_key\":\"lbe-" + suffix + "\"}";

        send(applicant, post("/api/v1/leasing/blacklist-exceptions"), body)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));
        jdbc.update("DELETE FROM role_viewable_projects WHERE role_id=? AND viewable_id=?", role, projectB);
        send(applicant, post("/api/v1/leasing/blacklist-exceptions"), body)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM leasing_blacklist_exceptions WHERE lead_id=?", Integer.class, lead)).isEqualTo(1);
    }

    // 守护：带商户归属的账号挂只勾 B 的角色，平面图仍只按本商户出，看不到 B 的。
    @Test
    void merchantAccountWithTickedRoleStaysOnItsOwnMerchant() throws Exception {
        seedProjectRows("b", projectB);
        String merchantA = merchant("a", projectA);
        RequestPostProcessor merchantAccount = account("m", "staff", projectA, merchantA, tickedBRole("m", "floor_plan.view"));
        assertThat(mine(merchantAccount, "/api/v1/floor-plans", "$.data.items[*].id")).doesNotContain(id("fp", "b"));
    }

    // 第十八批 G 段第 82 条 82a-R1：签约意向按 leasing.convert、拉黑/解黑品牌按 leasing.brand.blacklist 判对象所在项目（治理地图第 528 行
    // 「在该角色所含权限内」、第 659 行「逐个勾选的只能查看被选中的项目」）。列表码经只勾 B 的角色、动作码只来自本项目普通角色的人：
    // 页面列得出 B 的意向和品牌，签约、拉黑都 403 CROSS_PROJECT_FORBIDDEN，不起草合同、不改品牌；动作码也经勾 B 的角色持有的人签、拉黑 B 照常
    // （合同落 B）；本项目普通员工签、拉黑 A 照常（守护）。三个会话都不含 contract.draft.manage：签约起草只按 leasing.convert 核一次
    // （复核修复前红：只勾 B 的四码持有人签 B 被起草闸按 contract.draft.manage 403）。修复前签约 B 返回 200（第一处即红）。
    @Test
    void signAndBlacklistOnAnotherProjectFollowTheActionCode() throws Exception {
        for (String p : List.of("a", "b")) {
            seedProjectRows(p, project(p));
        }
        jdbc.update("UPDATE leasing_intents SET stage='pending_signing' WHERE id IN (?, ?)", id("int", "a"), id("int", "b"));
        RequestPostProcessor mixed = staffOfA("mx", tickedBRole("mx", "leasing.intent.manage", "leasing.brand.manage"),
                plainRole("mxl", "leasing.convert", "leasing.brand.blacklist"));
        assertThat(mine(mixed, "/api/v1/leasing/intents", "$.data.items[*].id")).contains(id("int", "b"));
        assertThat(mine(mixed, "/api/v1/leasing/brands", "$.data.items[*].id")).contains(id("brand", "b"));

        send(mixed, post("/api/v1/leasing/intents/" + id("int", "b") + "/sign"), signBody("mx", "b"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT contract_id FROM leasing_intents WHERE id=?", String.class, id("int", "b"))).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM contracts WHERE merchant_id=?", Integer.class, id("mch", "b"))).isZero();
        // 第十八批 I 段第 90 条 90-R1（PGM L738、L899）：被拒落 OpenAPI 声明的失败事件（独立事务，按本次幂等键查）
        assertThat(failures("leasing.intent.sign_failed", "sign-mx-" + suffix)).containsExactly(Map.of("actor_id", userId("mx"),
                "business_id", id("int", "b"), "error_code", "CROSS_PROJECT_FORBIDDEN"));
        send(mixed, post("/api/v1/leasing/brands/" + id("brand", "b") + "/blacklist"), blacklistBody("mx"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT brand_type FROM leasing_brands WHERE id=?", String.class, id("brand", "b")))
                .isEqualTo("prospective");
        assertThat(failures("leasing.brand.blacklist_change_failed", "bl-mx-" + suffix)).containsExactly(Map.of("actor_id",
                userId("mx"), "business_id", id("brand", "b"), "error_code", "CROSS_PROJECT_FORBIDDEN"));

        RequestPostProcessor full = staffOfA("fx", tickedBRole("fx", "leasing.intent.manage", "leasing.brand.manage",
                "leasing.convert", "leasing.brand.blacklist"));
        send(full, post("/api/v1/leasing/intents/" + id("int", "b") + "/sign"), signBody("fx", "b"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectB));
        send(full, post("/api/v1/leasing/brands/" + id("brand", "b") + "/blacklist"), blacklistBody("fx"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.brand_type").value("blacklist"));

        RequestPostProcessor plain = staffOfA("pl", plainRole("pl", "leasing.intent.manage", "leasing.brand.manage",
                "leasing.convert", "leasing.brand.blacklist"));
        send(plain, post("/api/v1/leasing/intents/" + id("int", "a") + "/sign"), signBody("pl", "a"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.project_id").value(projectA));
        send(plain, post("/api/v1/leasing/brands/" + id("brand", "a") + "/blacklist"), blacklistBody("pl"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.brand_type").value("blacklist"));
        for (String key : List.of("sign-fx-", "bl-fx-", "sign-pl-", "bl-pl-")) {
            assertThat(failureCount(key + suffix)).isZero();
        }
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private String project(String tag) {
        return switch (tag) {
            case "a" -> projectA;
            case "b" -> projectB;
            default -> projectC;
        };
    }

    private String id(String kind, String tag) {
        return kind + "_g19_" + tag + "_" + suffix;
    }

    /** 某项目的一套治理与招商行：商户、关联主体、空间、商户的待生效主体绑定、场地营业时间、平面图、外部机构、线索（10 天未跟进）、意向、品牌、跟进、黑名单例外单、签到阶梯。 */
    private void seedProjectRows(String tag, String projectId) {
        String merchantId = merchant(tag, projectId);
        String entity = businessEntity(tag, projectId);
        String spaceId = space(tag, projectId);
        Instant now = Instant.now();
        platformRepository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(UUID.randomUUID().toString(), merchantId,
                entity, entity, entity, "inactive", LocalDate.now(ZoneOffset.UTC).minusDays(1), null, "G19 夹具", now, now));
        jdbc.update("INSERT INTO venue_business_hours (id, venue_id, day_of_week, open_time, close_time) VALUES (?, ?, 1, '10:00', '22:00')",
                id("vbh", tag), spaceId);
        jdbc.update("""
                INSERT INTO mall_floor_plans (id, project_id, floor_no, plan_name, image_url, image_width, image_height, version_no,
                        status, created_at, updated_at)
                VALUES (?, ?, '1F', ?, 'https://img.example/fp.png', 100, 100, 'v1', 'active', now(), now())
                """, id("fp", tag), projectId, id("fp", tag));
        jdbc.update("INSERT INTO external_partner_orgs (id, project_id, org_name, org_category, created_at, updated_at) "
                + "VALUES (?, ?, ?, 'security', now(), now())", id("po", tag), projectId, id("po", tag));
        String brand = brand(tag, projectId);
        String lead = lead(tag, projectId, brand, now.minus(10, ChronoUnit.DAYS));
        leasingRepository.saveIntent(new LeasingIntentRecord(id("int", tag), lead, "initial_contact", null, null, Map.of(), now, null,
                projectId, now, now));
        leasingRepository.saveFollowUp(new LeasingFollowUpRecord(id("fu", tag), "lead", lead, "phone", "G19 跟进", null, "usr_g19",
                projectId, now));
        jdbc.update("""
                INSERT INTO leasing_blacklist_exceptions (id, project_id, brand_id, target_type, reason, requested_by, review_id, status)
                VALUES (?, ?, ?, 'lead_create', 'G19 夹具', 'usr_g19', ?, 'pending_approval')
                """, id("lbe", tag), projectId, brand, "rv_g19_" + tag + "_" + suffix);
        jdbc.update("INSERT INTO customer_check_in_tiers (id, project_id, streak_days, award_points, status) VALUES (?, ?, 3, 10, 'active')",
                id("tier", tag), projectId);
    }

    private String merchant(String tag, String projectId) {
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                + "VALUES (?, ?, 'catering', 'active', now(), now(), ?)", id("mch", tag), id("mch", tag), projectId);
        return id("mch", tag);
    }

    private String businessEntity(String tag, String projectId) {
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) "
                + "VALUES (?, ?, 'project_company', 'active', now(), now())", id("be", tag), id("be", tag));
        if (projectId != null) {
            jdbc.update("INSERT INTO business_entity_projects (business_entity_id, project_id) VALUES (?, ?)", id("be", tag), projectId);
        }
        return id("be", tag);
    }

    private String space(String tag, String projectId) {
        jdbc.update("""
                INSERT INTO spaces (id, project_id, space_name, space_type, floor_no, area_square_meter, polygon_points,
                        status, created_at, updated_at)
                VALUES (?, ?, ?, 'shop', '1F', 10, '[]'::jsonb, 'active', now(), now())
                """, id("sp", tag), projectId, id("sp", tag));
        return id("sp", tag);
    }

    private String brand(String tag, String projectId) {
        Instant now = Instant.now();
        leasingRepository.saveBrand(new LeasingBrandRecord(id("brand", tag), id("brand", tag), "prospective", "retail", null, null,
                null, projectId, now, now));
        return id("brand", tag);
    }

    private String lead(String tag, String projectId, String brand, Instant createdAt) {
        leasingRepository.saveLead(new LeasingLeadRecord(id("lead", tag), "G19 联系人", null, "outbound", "retail", null, null, brand,
                null, "pending_follow_up", null, null, projectId, createdAt, createdAt));
        return id("lead", tag);
    }

    private String bindingOf(String merchantId) {
        return jdbc.queryForObject("SELECT id::text FROM business_entity_bindings WHERE merchant_id=?", String.class, merchantId);
    }

    private String merchantProfile(String key, String name) {
        return "{\"idempotency_key\":\"" + key + "-" + suffix + "\",\"merchant_name\":\"" + name + "\",\"merchant_type\":\"catering\","
                + "\"contact_name\":\"王\",\"contact_phone\":\"13800000000\"}";
    }

    private String venueHours(String key, String venueId) {
        return "{\"idempotency_key\":\"" + key + "-" + suffix + "\",\"venue_id\":\"" + venueId
                + "\",\"day_of_week\":2,\"open_time\":\"10:00\",\"close_time\":\"22:00\"}";
    }

    private String bindingChange(String key) {
        return "{\"idempotency_key\":\"" + key + "-" + suffix + "\",\"action\":\"active\",\"reason\":\"到期启用\"}";
    }

    private String archive(String key) {
        return "{\"idempotency_key\":\"" + key + "-" + suffix + "\",\"reason\":\"暂停经营\"}";
    }

    private String signBody(String key, String tag) {
        String entity = id("be", tag);
        return "{\"idempotency_key\":\"sign-" + key + "-" + suffix + "\",\"merchant_id\":\"" + id("mch", tag)
                + "\",\"contract_subject_id\":\"" + entity + "\",\"operation_subject_id\":\"" + entity
                + "\",\"settlement_subject_id\":\"" + entity + "\",\"effective_date\":\"2099-01-01\",\"expiry_date\":\"2099-12-31\"}";
    }

    private String blacklistBody(String key) {
        return "{\"idempotency_key\":\"bl-" + key + "-" + suffix + "\",\"blacklist\":true,\"reason\":\"多次违约\"}";
    }

    private String spaceBody(String key, String name, String projectId) {
        return "{\"idempotency_key\":\"" + key + "-" + suffix + "\",\"space_name\":\"" + name + "\",\"space_type\":\"shop\","
                + "\"project_id\":\"" + projectId + "\"}";
    }

    // ── 角色与会话 ───────────────────────────────────────────────────────────────────────────────

    private String plainRole(String tag, String... codes) {
        return role(tag, false, List.of(), codes);
    }

    private String allProjectsRole(String tag, String... codes) {
        return role(tag, true, List.of(), codes);
    }

    private String tickedBRole(String tag, String... codes) {
        return role(tag, false, List.of(projectB), codes);
    }

    private String role(String tag, boolean viewAll, List<String> ticked, String... codes) {
        String roleId = "role_g19_" + tag + "_" + suffix;
        accessRepository.createRole(new RoleRecord(roleId, "G19 " + tag + " " + suffix, null, "project", "active", Instant.now(),
                List.of(codes), null, viewAll, ticked));
        return roleId;
    }

    private String userId(String tag) {
        return "usr_g19_" + tag + "_" + suffix;
    }

    private RequestPostProcessor staffOfA(String tag, String... roleIds) {
        return account(tag, "staff", projectA, null, roleIds);
    }

    /** 建一名账号、挂上给定角色，返回以其身份登录的会话（后台 Web 终端＋生产过滤器放入的 AccessActor 主体，四项与账号行一致）。 */
    private RequestPostProcessor account(String tag, String accountType, String organizationId, String merchantId, String... roleIds) {
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

    // ── 取数 ─────────────────────────────────────────────────────────────────────────────────────

    private ResultActions send(RequestPostProcessor session, MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.with(session).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private List<String> ids(RequestPostProcessor session, String url) throws Exception {
        return JsonPath.read(mockMvc.perform(get(url).with(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.items[*].id");
    }

    /** 失败事件是独立事务写入（AuditEventService.recordFailure）、不随测试回滚：按本次请求唯一的幂等键查。 */
    private List<Map<String, Object>> failures(String event, String key) {
        return jdbc.queryForList("SELECT actor_id, business_id, metadata->>'error_code' AS error_code FROM audit_events "
                + "WHERE event_name=? AND event_result='failure' AND idempotency_key=?", event, key);
    }

    private int failureCount(String key) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_result='failure' AND idempotency_key=?",
                Integer.class, key);
    }

    /** 以给定身份 GET（须 200），取出该 JSON 路径下属于本次夹具的编号（都以随机后缀结尾）；共享库里别的行不看。 */
    private List<String> mine(RequestPostProcessor session, String url, String path) throws Exception {
        List<String> values = JsonPath.read(mockMvc.perform(get(url).with(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), path);
        return values.stream().filter(value -> value != null && value.endsWith(suffix)).toList();
    }
}
