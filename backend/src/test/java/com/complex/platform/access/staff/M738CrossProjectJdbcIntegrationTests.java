package com.complex.platform.access.staff;

// 文件版本：V0.04

import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 E 段 19-R1（移植 M738：可跨项目角色·人事与审批可见性＋服务单）真库＋真实接口回归：真实角色勾选 → 真实判定 → 真实接口 → 真实 SQL。
 * 依据：治理地图「权限规则」「被分配了可跨项目角色的员工（含经岗位得到），在所分配的权限内可以查看、处理该角色可查看项目的数据。」
 * 「逐个勾选的只能查看被选中的项目」「角色能查看哪些项目，持有这类角色的员工就能在哪些项目新建记录」；
 * 2026-10-16 裁决所选选项原文一「角色勾了哪个项目，就看、管登记在该项目或改挂到该项目的员工。」、原文二「两个接口先核原班次的员工在不在范围内」。
 *
 * <p>判定口径：持有人凭可跨项目角色的部分按员工档案的登记项目（staff_profiles.project_id）；本人那一份（该码另有非可跨项目来源）
 * 与非持有人照旧按所属公司（company_id）。所以改挂到 B 的 mB 必须出现在只勾 B 的持有人视图里、不出现在只勾 A 的持有人视图里，
 * A 公司的非持有人照旧看得到 mB。
 *
 * <p>夹具随测试事务回滚：主体表里三家城市项目公司 A、B、C（在项目集合内）与一家平台公司 P（不在项目集合内），编号带每次运行的随机后缀；
 * 公司自有员工按登记项目建（登记项目＝所属公司＝该主体），mB 是从 A 改挂到 B 之后的样子（登记项目 B、所属公司仍是 A）。
 * 操作人默认是归属 A、不建档案的员工；角色直接经权限仓储建立并分配（勾全部、只勾 A、只勾 B、两项都没勾的普通角色）。
 * 会话主体的归属、商户、账号类型、状态与账号行一致。共享库里有别的数据：断言只看本次夹具建的行（编号都以随机后缀结尾）。
 * 被拒绝的请求留下的失败留痕是独立事务，按随机后缀隔离留在库里。请求体一律按前端的蛇形键。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class M738CrossProjectJdbcIntegrationTests {

    @Autowired
    private AccessRepository accessRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String entityA = "be_m738_a_" + suffix;
    private final String entityB = "be_m738_b_" + suffix;
    private final String entityC = "be_m738_c_" + suffix;
    private final String entityP = "be_m738_p_" + suffix;
    // J4 的三个人事列表：2099-03-17 的排班与考勤、待批请假（薪资单列表已改走完整薪资的可见单据，不在本条）
    private static final List<String> STAFF_LISTS = List.of(
            "/api/v1/staff/shifts?date=2099-03-17", "/api/v1/staff/attendance?date=2099-03-17",
            "/api/v1/staff/leaves?status=pending");

    @BeforeEach
    void createEntities() {
        for (String[] row : List.of(new String[] {entityA, "project_company"}, new String[] {entityB, "project_company"},
                new String[] {entityC, "project_company"}, new String[] {entityP, "platform"})) {
            jdbcTemplate.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) "
                    + "VALUES (?, ?, ?, 'active', now(), now())", row[0], "M738 " + row[0], row[1]);
        }
    }

    // J1（2.2 列表）：码 staff.profile.view。修复前只勾 B 的人拿到的是本公司 A 的 eA、mB（第一处断言即红）。
    @Test
    void profileListFollowsRegisteredProjectForCrossProjectRoles() throws Exception {
        String code = "staff.profile.view";
        employee("ea", entityA);
        employee("eb", entityB);
        employee("ec", entityC);
        employee("ep", entityP);
        RequestPostProcessor moved = movedToB("mb", tickedBRole("mb", code));
        String eA = profileId("ea");
        String eB = profileId("eb");
        String eC = profileId("ec");
        String mB = profileId("mb");

        // 修复前红：只勾 B 的人拿到 eA、mB（按本公司 A）
        assertThat(profiles(staffOfA("ticked", tickedBRole("ticked", code)))).containsExactlyInAnyOrder(eB, mB);
        // 非持有人（普通角色）：照旧按所属公司 A，含改挂到 B 的 mB（守护，基线同）
        assertThat(profiles(staffOfA("plain", plainRole("plain", code)))).containsExactlyInAnyOrder(eA, mB);
        // 只勾 A：改挂到 B 的 mB 不出（修复前有 mB）
        assertThat(profiles(staffOfA("tickeda", tickedARole("tickeda", code)))).containsExactly(eA);
        // 勾 B 且权限另有普通来源：本公司照旧，加上登记在 B 的
        assertThat(profiles(staffOfA("both", tickedBRole("both_t", code), plainRole("both_p", code))))
                .containsExactlyInAnyOrder(eA, mB, eB);
        // 勾全部：登记项目在项目集合内的；登记在平台公司 P 的 eP 不出
        assertThat(profiles(staffOfA("all", allProjectsRole("all", code)))).containsExactlyInAnyOrder(eA, eB, eC, mB);

        // 带商户归属的账号挂勾 B 的角色：照旧只看本商户的档案（守护）
        String merchant = merchant("m", entityA);
        profile("em", entityA, entityA, merchant);
        assertThat(profiles(account("mch", "staff", entityA, merchant, tickedBRole("mch", code))))
                .containsExactly(profileId("em"));

        // mB 本人作操作人、只勾 B：本人档案所属公司 A 不因此带进来（守护本人那一份按 ownBySource）——修复前有 eA
        assertThat(profiles(moved)).containsExactlyInAnyOrder(eB, mB);
    }

    // J2（2.2 写）：码 staff.profile.submit、staff.profile.review。修复前只勾 A 的人按本公司 A 存档了改挂到 B 的 mB（第一处断言即红）。
    // 任职状态变更已改为发起复核且先核直属领导（CG-12），这里用销售岗（同码 staff.profile.review）验写入范围。
    @Test
    void profileWritesAndRegistrationReviewFollowRegisteredProject() throws Exception {
        String[] codes = {"staff.profile.submit", "staff.profile.review"};
        employee("ea", entityA);
        employee("eb", entityB);
        employee("ep", entityP);
        RequestPostProcessor moved = movedToB("mb", tickedBRole("mb", codes));
        String eA = profileId("ea");
        String eB = profileId("eb");
        String mB = profileId("mb");
        RequestPostProcessor ticked = staffOfA("ticked", tickedBRole("ticked", codes));

        // 修复前红：只勾 A 的人存档 mB 得到 200
        denied(archive(staffOfA("tickeda", tickedARole("tickeda", codes)), mB), "CROSS_ORG_FORBIDDEN");
        assertThat(archivedAt(mB)).isNull();
        archive(ticked, mB).andExpect(status().isOk());
        archive(ticked, eB).andExpect(status().isOk());
        assertThat(archivedAt(eB)).isNotNull();
        denied(archive(ticked, eA), "CROSS_ORG_FORBIDDEN");
        assertThat(archivedAt(eA)).isNull();
        salesPosition(ticked, eB).andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject("SELECT is_sales_position FROM staff_profiles WHERE id = ?", Boolean.class, eB)).isTrue();
        denied(salesPosition(ticked, eA), "CROSS_ORG_FORBIDDEN");
        // 勾全部：平台公司 P 不在项目集合里
        denied(archive(staffOfA("all", allProjectsRole("all", codes)), profileId("ep")), "CROSS_ORG_FORBIDDEN");
        // mB 本人作操作人、只勾 B：本人档案所属公司 A 不带进来——修复前 200
        denied(archive(moved, eA), "CROSS_ORG_FORBIDDEN");
        assertThat(archivedAt(eA)).isNull();

        // 注册审批：两条「已提交」注册，登记项目＝所属公司＝A／B；用驳回（不依赖登录凭证服务，闸与批准相同）
        String rgA = registration("rga", entityA);
        String rgB = registration("rgb", entityB);
        // 非持有人照旧只批本公司（守护）
        denied(reject(staffOfA("plain", plainRole("plain", "staff.profile.review")), rgB), "CROSS_ORG_FORBIDDEN");
        // 修复前 403 CROSS_ORG_FORBIDDEN
        reject(ticked, rgB).andExpect(status().isOk());
        assertThat(registrationStatus(rgB)).isEqualTo("rejected");
        denied(reject(ticked, rgA), "CROSS_ORG_FORBIDDEN");
        assertThat(registrationStatus(rgA)).isEqualTo("submitted");
    }

    // M738-F06（2026-10-16 所选选项原文「审批落地职级时沿用已经核过的注册审批范围」）：只凭只勾 B 的角色持有 staff.profile.review、
    // 不持有 access.staff_level.manage 的人，批准一条登记在 B、带职级（种子职级 lvl_m1_lower）的「已提交」注册。
    // 修复前注册审批即按本公司 A 拒（403 CROSS_ORG_FORBIDDEN），整笔回滚（第一处断言即红）。
    @Test
    void approvingRegistrationLandsStaffLevelWithinTheReviewReach() throws Exception {
        String rgB = registration("rgb", entityB);
        jdbcTemplate.update("UPDATE staff_profiles SET staff_level_id = 'lvl_m1_lower' WHERE id = ?", rgB);
        RequestPostProcessor ticked = staffOfA("ticked", tickedBRole("ticked", "staff.profile.review"));

        // 修复前红：403 CROSS_ORG_FORBIDDEN
        postJson(ticked, "/api/v1/staff/registrations/" + rgB + "/review", """
                {"idempotency_key": "%s", "decision": "approve", "initial_password": "m738-test-init"}
                """.formatted(key())).andExpect(status().isOk());
        assertThat(registrationStatus(rgB)).isEqualTo("approved");
        String created = jdbcTemplate.queryForObject("SELECT user_id FROM staff_profiles WHERE id = ?", String.class, rgB);
        assertThat(jdbcTemplate.queryForObject("SELECT organization_id FROM access_users WHERE id = ?", String.class, created))
                .isEqualTo(entityB);
        assertThat(jdbcTemplate.queryForList("SELECT role_id FROM user_roles WHERE user_id = ?", String.class, created))
                .contains("role_staff_self");
        assertThat(jdbcTemplate.queryForObject("SELECT staff_level_id FROM staff_level_assignments WHERE user_id = ?", String.class,
                created)).isEqualTo("lvl_m1_lower");
    }

    // M738-F07（2026-10-17 所选选项原文「域公司审批已通过的注册，职级也能落地」）：域公司 C 的人事（账号与档案归属 C，只经普通角色持有
    // staff.profile.review、不持有 access.staff_level.manage）按所属公司批准一条登记项目 B、所属公司 C、带职级 lvl_m1_lower 的「已提交」商户员工注册。
    // 修复前落地职级按新账号组织 B 另核范围（普通角色只到本人项目 C）→ 403 CROSS_ORG_FORBIDDEN、整笔回滚（第一处断言即红）。
    @Test
    void domainCompanyApprovalLandsStaffLevelOfMerchantRegistrationInAnotherProject() throws Exception {
        RequestPostProcessor hrC = account("hrc", "staff", entityC, null, plainRole("hrc", "staff.profile.review"));
        profile("hrc", entityC, entityC, null);
        String rg = profileId("rgc");
        jdbcTemplate.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, company_id, staff_level_id, registration_status,
                        registration_source, status, created_at, updated_at)
                VALUES (?, ?, ?, NULL, 'M738', ?, 'lvl_m1_lower', 'submitted', 'merchant_submitted', 'active', now(), now())
                """, rg, entityB, merchant("mc", entityC), entityC);

        // 修复前红：403 CROSS_ORG_FORBIDDEN
        postJson(hrC, "/api/v1/staff/registrations/" + rg + "/review", """
                {"idempotency_key": "%s", "decision": "approve", "initial_password": "m738-test-init"}
                """.formatted(key())).andExpect(status().isOk());
        assertThat(registrationStatus(rg)).isEqualTo("approved");
        String created = jdbcTemplate.queryForObject("SELECT user_id FROM staff_profiles WHERE id = ?", String.class, rg);
        assertThat(jdbcTemplate.queryForObject("SELECT organization_id FROM access_users WHERE id = ?", String.class, created))
                .isEqualTo(entityB);
        assertThat(jdbcTemplate.queryForList("SELECT role_id FROM user_roles WHERE user_id = ?", String.class, created))
                .contains("role_staff_self");
        assertThat(jdbcTemplate.queryForObject("SELECT staff_level_id FROM staff_level_assignments WHERE user_id = ?", String.class,
                created)).isEqualTo("lvl_m1_lower");
    }

    // J3（2.3 员工登记）：码 staff.profile.submit，商户 mchB 的经营主体公司是 B。修复前只勾 B 的人登记到 B 被拒（第一处断言即红）。
    @Test
    void registrationMayTargetReachableProjects() throws Exception {
        String code = "staff.profile.submit";
        String merchantB = merchant("b", entityB);
        RequestPostProcessor ticked = staffOfA("ticked", tickedBRole("ticked", code));

        // 修复前红：403 CROSS_ORG_FORBIDDEN（只认本人项目 A）
        String created = JsonPath.read(register(ticked, entityB, merchantB).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        assertThat(jdbcTemplate.queryForMap("SELECT project_id, company_id FROM staff_profiles WHERE id = ?", created))
                .containsEntry("project_id", entityB).containsEntry("company_id", entityB);
        // 没勾的 C：拒绝，不落行
        denied(register(ticked, entityC, merchantB), "CROSS_ORG_FORBIDDEN");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM staff_profiles WHERE project_id = ?", Integer.class, entityC))
                .isZero();
        // 非持有人照旧只能登记到本人项目（守护）
        denied(register(staffOfA("plain", plainRole("plain", code)), entityB, merchantB), "CROSS_ORG_FORBIDDEN");
        // 勾全部：平台公司 P 不在项目集合里
        denied(register(staffOfA("all", allProjectsRole("all", code)), entityP, merchantB), "CROSS_ORG_FORBIDDEN");
    }

    // J4（2.4 列表）：eA、eB、eP、mB 各有一行排班、考勤、待批请假；mB 这几行各表自己的 project_id 是改挂前写入的 A。
    // 修复前只勾 B 的人三个列表拿到的都是本公司 A 的 eA、mB（第一处断言即红）。
    @Test
    void staffListsFollowRegisteredProject() throws Exception {
        String[] codes = {"staff.shift.manage", "staff.attendance.view"};
        String eA = employee("ea", entityA);
        String eB = employee("eb", entityB);
        String eP = employee("ep", entityP);
        movedToB("mb");
        String mB = userId("mb");
        staffRows(eA, entityA);
        staffRows(eB, entityB);
        staffRows(eP, entityP);
        staffRows(mB, entityA);

        // 修复前红：只勾 B 的人拿到 eA、mB
        assertStaffLists(staffOfA("ticked", tickedBRole("ticked", codes)), eB, mB);
        // 只勾 A：改挂到 B 的 mB 不出（修复前有 mB；各表自己的 project_id 不算）
        assertStaffLists(staffOfA("tickeda", tickedARole("tickeda", codes)), eA);
        // 非持有人：照旧按所属公司（守护）
        assertStaffLists(staffOfA("plain", plainRole("plain", codes)), eA, mB);
        // 勾全部：登记在平台公司 P 的 eP 不出
        assertStaffLists(staffOfA("all", allProjectsRole("all", codes)), eA, eB, mB);
    }

    // J6（2.5 指向单个员工的写）：码 staff.payroll.manage、staff.shift.manage。固定工资入口现行在范围判定之后一律 409 PAYROLL_SCHEME_REQUIRED，
    // 范围外先 403。修复前只勾 B 的人给 eB 设固定工资得 403（第一处断言即红）。
    @Test
    void staffWritesFollowTargetProfilesRegisteredProject() throws Exception {
        String eA = employee("ea", entityA);
        String eB = employee("eb", entityB);
        movedToB("mb");
        String leaveB = leave(eB, entityB, "pending");
        RequestPostProcessor ticked = staffOfA("ticked", tickedBRole("ticked", "staff.payroll.manage", "staff.shift.manage"));

        // 修复前红：403 CROSS_ORG_FORBIDDEN
        conflict(setBaseSalary(ticked, eB), "PAYROLL_SCHEME_REQUIRED");
        conflict(setBaseSalary(ticked, userId("mb")), "PAYROLL_SCHEME_REQUIRED");
        denied(setBaseSalary(ticked, eA), "CROSS_ORG_FORBIDDEN");
        postJson(ticked, "/api/v1/staff/leaves/" + leaveB + "/review", """
                {"idempotency_key": "%s", "decision": "approve", "review_note": "M738 同意"}
                """.formatted(key())).andExpect(status().isOk());
        assertThat(leaveStatus(leaveB)).isEqualTo("approved");

        // 勾全部：没有员工档案的平台管理员账号一律碰不到
        String admin = userId("padm");
        account("padm", "platform_admin", "org", null);
        denied(setBaseSalary(staffOfA("all", allProjectsRole("all", "staff.payroll.manage")), admin), "CROSS_PROJECT_FORBIDDEN");
        // 带商户归属的账号挂勾 B 的角色：照旧只到本人项目（守护）
        String merchant = merchant("m", entityA);
        denied(setBaseSalary(account("mch", "staff", entityA, merchant, tickedBRole("mch", "staff.payroll.manage")), eB),
                "CROSS_ORG_FORBIDDEN");
    }

    // J7（2.0 重试先核范围）：A 的员工只经角色「可查看 B、C」持有 staff.payroll.manage，用提交编号 K 给 eB 发起奖惩复核，同一编号立刻重试照旧；
    // 角色的可查看项目减掉 B（减少审批生效后的状态：直接删 role_viewable_projects 那一行）后，同一编号重试被拒、不返回第一次的结果。
    // 修复前第一次提交即 403（第一处断言即红）。
    @Test
    void payrollAdjustmentRetryAfterTargetProjectLeavesReachIsDenied() throws Exception {
        String reduced = role("bc", false, List.of(entityB, entityC), "staff.payroll.manage");
        RequestPostProcessor staff = staffOfA("adj", reduced);
        String eB = employee("eb", entityB);
        String body = """
                {"idempotency_key": "%s", "user_id": "%s", "month": "2098-06", "adj_type": "bonus", "amount_cent": 100, "reason": "M738 奖励"}
                """.formatted("m738-adj-" + suffix, eB);

        // 修复前红：403 CROSS_ORG_FORBIDDEN
        String reviewId = JsonPath.read(postJson(staff, "/api/v1/staff/payroll/adjustments", body).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.review_id");
        // 仍可触及 B：同一编号重试照旧返回同一张复核单，不新增
        postJson(staff, "/api/v1/staff/payroll/adjustments", body).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_id").value(reviewId));
        assertThat(adjustmentRequestCount(eB)).isEqualTo(1);

        // 角色的可查看项目减掉 B 并生效：同一编号重试被拒，不新增
        jdbcTemplate.update("DELETE FROM role_viewable_projects WHERE role_id = ? AND viewable_id = ?", reduced, entityB);
        denied(postJson(staff, "/api/v1/staff/payroll/adjustments", body), "CROSS_ORG_FORBIDDEN");
        assertThat(adjustmentRequestCount(eB)).isEqualTo(1);
    }

    // J8（2.6 主管复核）：三张待审复核单，发起人分别是 A、B、P 的普通账号。码 governance.supervisor_review.view、.approve。
    // 修复前只勾 B 的人待审清单里是本人项目 A 发起的 rvA（第一处断言即红）。
    @Test
    void pendingReviewsFollowInitiatorProjectWithinReach() throws Exception {
        String[] codes = {"governance.supervisor_review.view", "governance.supervisor_review.approve"};
        String rvA = pendingReview("a", entityA);
        String rvB = pendingReview("b", entityB);
        pendingReview("p", entityP);
        RequestPostProcessor ticked = staffOfA("ticked", tickedBRole("ticked", codes));

        // 修复前红：只有 rvA
        assertThat(pendingReviews(ticked)).containsExactly(rvB);
        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + rvB + "/chain").with(ticked)).andExpect(status().isOk());
        denied(mockMvc.perform(get("/api/v1/platform/supervisor-review/" + rvA + "/chain").with(ticked)),
                "SUPERVISOR_REVIEW_SCOPE_FORBIDDEN");
        denied(postJson(ticked, "/api/v1/platform/supervisor-review/" + rvA + "/approve", """
                {"idempotency_key": "%s", "decision": "approved", "comment": "M738 同意"}
                """.formatted(key())), "SUPERVISOR_REVIEW_SCOPE_FORBIDDEN");
        assertThat(reviewStatus(rvA)).isEqualTo("pending_review");

        // 非持有人：照旧只到本人项目（守护）
        assertThat(pendingReviews(staffOfA("plain", plainRole("plain", codes)))).containsExactly(rvA);
        // 勾全部：平台公司 P 的普通账号发起的单不出
        assertThat(pendingReviews(staffOfA("all", allProjectsRole("all", codes)))).containsExactlyInAnyOrder(rvA, rvB);
    }

    // J8 补（M738-F02 验收④）：只勾 B、普通角色、勾全部三种人各一组复核单——发起人分别在其范围内、范围外，
    // 单上只有一个待裁决的角色节点（按角色解析、不指定审批人，所以看不看得到、能不能通过只取决于范围）。码同 J8。
    @Test
    void eachKindOfHolderReadsAndApprovesOnlyReviewsWithinReach() throws Exception {
        String[] codes = {"governance.supervisor_review.view", "governance.supervisor_review.approve"};
        approvesWithinReach("ticked", tickedBRole("ticked", codes), entityB, entityA);
        approvesWithinReach("plain", plainRole("plain", codes), entityA, entityB);
        // 勾全部：平台公司 P 不在项目集合里
        approvesWithinReach("all", allProjectsRole("all", codes), entityB, entityP);
    }

    // 18E-④：只持查看码（只勾 B）按编号读复核单原件——B 发起的 200，本人项目 A 发起的 403（该码没有非可跨项目来源）。修复前读 rvB 即 403（第一处断言即红）。
    @Test
    void reviewOriginalByIdFollowsInitiatorProjectWithinReach() throws Exception {
        String rvA = pendingReview("a", entityA);
        String rvB = pendingReview("b", entityB);
        RequestPostProcessor ticked = staffOfA("ticked", tickedBRole("ticked", "governance.supervisor_review.view"));

        // 修复前红：403 SUPERVISOR_REVIEW_SCOPE_FORBIDDEN
        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + rvB).with(ticked))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.review_id").value(rvB));
        denied(mockMvc.perform(get("/api/v1/platform/supervisor-review/" + rvA).with(ticked)), "SUPERVISOR_REVIEW_SCOPE_FORBIDDEN");
    }

    // 18E-⑤（18A 同根）：酒吧营业日配置的审批原件只借这张审批单的可见性——只勾 B 持查看码的人读 B 发起的配置原件 200。修复前 403（第一处断言即红）。
    @Test
    void barBusinessDayOriginalFollowsReviewVisibilityWithinReach() throws Exception {
        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'mall', 'M738 B', 'active', now(), now())",
                entityB);
        String bar = merchant("bar", entityB);
        String rvB = pendingReview("bar", entityB);
        String config = "bbd_m738_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO bar_business_day_configs (id, bar_id, project_id, version, cutoff_hour, min_cutoff_hour, max_cutoff_hour,
                        close_hour, min_close_hour, max_close_hour, effective_at, status, review_id, reason, created_by, created_at)
                VALUES (?, ?, ?, 'V0.01', 10, 8, 12, 7, 5, 9, now() + interval '1 day', 'pending_review', ?, 'M738', 'usr_m738', now())
                """, config, bar, entityB, rvB);

        // 修复前红：403 SUPERVISOR_REVIEW_SCOPE_FORBIDDEN
        mockMvc.perform(get("/api/v1/bar-business-day-configs/" + config)
                        .with(staffOfA("ticked", tickedBRole("ticked", "governance.supervisor_review.view"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.review_id").value(rvB));
        // 非持有人：照旧只到本人项目（守护）
        denied(mockMvc.perform(get("/api/v1/bar-business-day-configs/" + config)
                .with(staffOfA("plain", plainRole("plain", "governance.supervisor_review.view")))), "SUPERVISOR_REVIEW_SCOPE_FORBIDDEN");
    }

    // J9（2.7 服务单）：履约人 eA、eB，服务单 oA、oB（项目分别 A、B）。码 service.catalog.manage、service.order.create。
    // 修复前只勾 B 的人后台列表里是本人项目 A 的 oA（第一处断言即红）。
    @Test
    void serviceOrdersFollowPerformerProjectWithinReach() throws Exception {
        String[] codes = {"service.catalog.manage", "service.order.create"};
        String eA = employee("ea", entityA);
        String eB = employee("eb", entityB);
        String catalogItem = catalogItem();
        String oA = serviceOrder("a", eA, entityA, catalogItem);
        String oB = serviceOrder("b", eB, entityB, catalogItem);
        RequestPostProcessor ticked = staffOfA("ticked", tickedBRole("ticked", codes));

        // 修复前红：只有 oA
        assertThat(serviceOrders(ticked)).containsExactly(oB);
        cancel(ticked, oB).andExpect(status().isOk());
        assertThat(orderStatus(oB)).isEqualTo("cancelled");
        denied(cancel(ticked, oA), "CROSS_PROJECT_FORBIDDEN");
        assertThat(orderStatus(oA)).isEqualTo("created");
        // 可为 B 的履约人在 B 下单：新单的项目照旧取履约人档案项目
        String created = JsonPath.read(createOrder(ticked, catalogItem, eB).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        assertThat(jdbcTemplate.queryForObject("SELECT project_id FROM service_orders WHERE id = ?", String.class, created))
                .isEqualTo(entityB);

        // 非持有人：照旧只到本人项目（守护）
        assertThat(serviceOrders(staffOfA("plain", plainRole("plain", codes)))).containsExactly(oA);
        assertThat(serviceOrders(staffOfA("all", allProjectsRole("all", codes)))).containsExactlyInAnyOrder(oA, oB);
    }

    // J9 补（M738-F03 验收④）：只勾 B、普通角色、勾全部三种人各一组服务单——履约人分别在其范围内、范围外。码同 J9。
    @Test
    void eachKindOfHolderListsCancelsAndOrdersOnlyWithinReach() throws Exception {
        String[] codes = {"service.catalog.manage", "service.order.create"};
        String eA = employee("ea", entityA);
        String eB = employee("eb", entityB);
        String eP = employee("ep", entityP);
        String catalogItem = catalogItem();

        ordersWithinReach(staffOfA("ticked", tickedBRole("ticked", codes)), "t", catalogItem, eB, entityB, eA, entityA);
        ordersWithinReach(staffOfA("plain", plainRole("plain", codes)), "n", catalogItem, eA, entityA, eB, entityB);
        // 勾全部：登记在平台公司 P 的履约人不在项目集合里
        ordersWithinReach(staffOfA("all", allProjectsRole("all", codes)), "w", catalogItem, eB, entityB, eP, entityP);
    }

    // J10①（2.5 顶班，2026-10-16 所选选项原文二）：只勾 B 的人（码 staff.shift.manage）查看 eB 班次 shB 的顶班记录。修复前 403（第一处断言即红）。
    @Test
    void holderViewsCoversOfReachableOriginalShift() throws Exception {
        String shiftB = shift(employee("eb", entityB), entityB);

        // 修复前红：403 CROSS_ORG_FORBIDDEN
        mockMvc.perform(get("/api/v1/staff/shifts/" + shiftB + "/covers")
                .with(staffOfA("ticked", tickedBRole("ticked", "staff.shift.manage")))).andExpect(status().isOk());
        // 非持有人：原班次员工不在其范围内，照旧 403（守护）
        denied(mockMvc.perform(get("/api/v1/staff/shifts/" + shiftB + "/covers")
                .with(staffOfA("plain", plainRole("plain", "staff.shift.manage")))), "CROSS_ORG_FORBIDDEN");
    }

    // J10②（同上）：只勾 B 的人给 shB 安排顶班，顶班员工是同在 B 的 eB2，起止在原班次内。修复前 403、不落顶班记录（第一处断言即红）。
    @Test
    void holderArrangesCoverForReachableOriginalShift() throws Exception {
        String shiftB = shift(employee("eb", entityB), entityB);
        String eB2 = employee("eb2", entityB);
        String body = """
                {"idempotency_key": "%s", "cover_user_id": "%s", "reason": "M738 临时顶班",
                 "start_at": "2099-03-17T02:00:00Z", "end_at": "2099-03-17T04:00:00Z", "handover_note": "M738 交接"}
                """;

        // 修复前红：403 CROSS_ORG_FORBIDDEN
        postJson(staffOfA("ticked", tickedBRole("ticked", "staff.shift.manage")), "/api/v1/staff/shifts/" + shiftB + "/cover",
                body.formatted(key(), eB2)).andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM staff_shift_covers WHERE shift_id = ?", Integer.class, shiftB))
                .isEqualTo(1);
        // 非持有人：照旧 403，不新增（守护）
        denied(postJson(staffOfA("plain", plainRole("plain", "staff.shift.manage")), "/api/v1/staff/shifts/" + shiftB + "/cover",
                body.formatted(key(), eB2)), "CROSS_ORG_FORBIDDEN");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM staff_shift_covers WHERE shift_id = ?", Integer.class, shiftB))
                .isEqualTo(1);
    }

    // 18E-①（main 后来新增的同类入口）：只勾 B 持 staff.payroll.manage 的人按员工查奖惩申诉清单——B 的员工 200、本公司 A 的员工 403。修复前查 eB 即 403（第一处断言即红）。
    @Test
    void adjustmentAppealsByEmployeeFollowRegisteredProject() throws Exception {
        String eA = employee("ea", entityA);
        String eB = employee("eb", entityB);
        RequestPostProcessor ticked = staffOfA("ticked", tickedBRole("ticked", "staff.payroll.manage"));

        // 修复前红：403 CROSS_ORG_FORBIDDEN
        mockMvc.perform(get("/api/v1/staff/payroll/adjustments/appeals").param("user_id", eB).with(ticked)).andExpect(status().isOk());
        denied(mockMvc.perform(get("/api/v1/staff/payroll/adjustments/appeals").param("user_id", eA).with(ticked)), "CROSS_ORG_FORBIDDEN");
    }

    // 18E-②（main 后来新增的同类入口）：只勾 B 持 staff.shift.manage 的人批准 B 员工的销假申请。修复前 403（第一处断言即红）。
    @Test
    void holderApprovesLeaveRevokeOfReachableEmployee() throws Exception {
        String eB = employee("eb", entityB);
        String eA = employee("ea", entityA);
        String leaveB = leave(eB, entityB, "revoke_requested");
        String leaveA = leave(eA, entityA, "revoke_requested");
        RequestPostProcessor ticked = staffOfA("ticked", tickedBRole("ticked", "staff.shift.manage"));
        String body = """
                {"idempotency_key": "%s", "decision": "approve", "review_note": "M738 同意销假"}
                """;

        // 修复前红：403 CROSS_ORG_FORBIDDEN
        postJson(ticked, "/api/v1/staff/leaves/" + leaveB + "/revoke", body.formatted(key())).andExpect(status().isOk());
        assertThat(leaveStatus(leaveB)).isEqualTo("revoked");
        denied(postJson(ticked, "/api/v1/staff/leaves/" + leaveA + "/revoke", body.formatted(key())), "CROSS_ORG_FORBIDDEN");
        assertThat(leaveStatus(leaveA)).isEqualTo("revoke_requested");
    }

    // 18E-③：操作人自己有员工档案（所属公司 A），只勾 B 持 staff.attendance.view，查看 B 公司员工 eB 的工作手机考勤事件。
    // 修复前凭角色触及 eB 之后又按「本人公司≠员工公司」403 STAFF_COMPANY_SCOPE_MISMATCH（第一处断言即红）；非持有人照旧 403（守护）。
    @Test
    void workPhoneReceiptsOfReachableEmployeeInAnotherCompany() throws Exception {
        String eB = employee("eb", entityB);
        RequestPostProcessor hr = account("hr", "staff", entityA, null, tickedBRole("hr", "staff.attendance.view"));
        profile("hr", entityA, entityA, null);

        // 修复前红：403 STAFF_COMPANY_SCOPE_MISMATCH
        mockMvc.perform(get("/api/v1/staff/attendance/receipt-events").param("user_id", eB).with(hr)).andExpect(status().isOk());
        denied(mockMvc.perform(get("/api/v1/staff/attendance/receipt-events").param("user_id", eB)
                .with(staffOfA("plain", plainRole("plain", "staff.attendance.view")))), "STAFF_SCOPE_MISMATCH");
    }

    // 18E 复核后操作员裁决补充第 16 条（所选选项原文「这三类人事复核单按所处理员工的登记项目判普通查看人能不能看；审批人、发起人本人照旧。」）：
    // A 的员工经只勾 B 的角色持有 staff.payroll.manage、staff.shift.manage、staff.profile.review（另经普通角色持有 staff.shift.manage 与查看码），
    // 替 B 的员工 eB 提交奖惩、排班、状态变更三张复核单，再排一批 eA＋eB 的班。查看码 governance.supervisor_review.view。
    // 修复前四张单都按发起人算 A：A 里只持查看码的普通员工在待复核清单里看得到（第一处断言即红）。
    @Test
    void staffReviewsForAnotherProjectsEmployeeFollowRegisteredProject() throws Exception {
        String view = "governance.supervisor_review.view";
        String eA = employee("ea", entityA);
        String eB = employee("eb", entityB);
        RequestPostProcessor holder = staffOfA("holder",
                tickedBRole("holder", "staff.payroll.manage", "staff.shift.manage", "staff.profile.review"),
                plainRole("holder_p", "staff.shift.manage", view));
        // 状态变更须由该员工直属领导发起（不挂部门线：人事会签不解析，三张单都不生成审批链）
        jdbcTemplate.update("INSERT INTO org_hierarchy (user_id, superior_id, updated_by) VALUES (?, ?, ?)", eB, userId("holder"),
                userId("holder"));
        String adjustment = reviewIdOf(postJson(holder, "/api/v1/staff/payroll/adjustments", """
                {"idempotency_key": "%s", "user_id": "%s", "month": "2098-07", "adj_type": "penalty", "amount_cent": 200, "reason": "M738 扣罚"}
                """.formatted(key(), eB)));
        String shift = reviewIdOf(postJson(holder, "/api/v1/staff/shifts", """
                {"idempotency_key": "%s", "user_id": "%s", "shift_id": null, "work_date": "2099-04-01", "start_at": "2099-04-01T01:00:00Z",
                 "end_at": "2099-04-01T09:00:00Z", "point_space_ids": [], "note": null, "night_duty_manager": false}
                """.formatted(key(), eB)));
        String employment = reviewIdOf(postJson(holder, "/api/v1/staff/profiles/" + profileId("eb") + "/employment-status", """
                {"idempotency_key": "%s", "employment_status": "suspended", "reason": "M738 停岗"}
                """.formatted(key())));
        String mixed = reviewIdOf(postJson(holder, "/api/v1/staff/shifts/batch", """
                {"idempotency_key": "%s", "user_ids": ["%s", "%s"], "start_date": "2099-04-02", "days": 1, "weekdays": [], "start_time": "09:00",
                 "end_time": "17:00", "point_space_ids": [], "note": null, "night_duty_manager": false}
                """.formatted(key(), eA, eB)));
        List<String> forB = List.of(adjustment, shift, employment);
        // 待审清单按到期时刻排序、最多 200 张：夹具单排到前面
        jdbcTemplate.update("UPDATE supervisor_review_decisions SET due_at = TIMESTAMPTZ '2000-01-01 00:00:00+00' WHERE review_id IN (?, ?, ?, ?)",
                adjustment, shift, employment, mixed);
        RequestPostProcessor viewerA = staffOfA("viewa", plainRole("viewa", view));
        RequestPostProcessor viewerB = account("viewb", "staff", entityB, null, plainRole("viewb", view));

        // 修复前红：A 的普通查看人看得到这四张
        assertThat(pendingIds(viewerA)).doesNotContainAnyElementsOf(List.of(adjustment, shift, employment, mixed));
        assertThat(pendingIds(viewerB)).containsAll(forB).doesNotContain(mixed);
        for (String id : forB) {
            denied(mockMvc.perform(get("/api/v1/platform/supervisor-review/" + id).with(viewerA)), "SUPERVISOR_REVIEW_SCOPE_FORBIDDEN");
            mockMvc.perform(get("/api/v1/platform/supervisor-review/" + id).with(viewerB))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.review_id").value(id));
        }
        denied(mockMvc.perform(get("/api/v1/platform/supervisor-review/" + adjustment + "/chain").with(viewerA)),
                "SUPERVISOR_REVIEW_SCOPE_FORBIDDEN");
        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + adjustment + "/chain").with(viewerB)).andExpect(status().isOk());
        // 批次里有一名员工登记在 A：B 的普通查看人也看不到（全部员工的登记项目都在范围内才算）
        denied(mockMvc.perform(get("/api/v1/platform/supervisor-review/" + mixed).with(viewerB)), "SUPERVISOR_REVIEW_SCOPE_FORBIDDEN");

        // 审批人照旧：A 里只持查看码、是奖惩单当前节点审批人的人看得到（守护）
        RequestPostProcessor approverA = staffOfA("apr", plainRole("apr", view));
        jdbcTemplate.update("INSERT INTO supervisor_review_nodes (node_id, review_id, line_code, seq, approver_kind, approver_id, status) "
                + "VALUES (?, ?, 'main', 1, 'user', ?, 'pending')", "nd_" + adjustment, adjustment, userId("apr"));
        assertThat(pendingIds(approverA)).contains(adjustment).doesNotContain(shift, employment, mixed);
        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + adjustment).with(approverA)).andExpect(status().isOk());
        // 发起人本人照旧（守护）
        assertThat(pendingIds(holder)).contains(adjustment, shift, employment, mixed);

        // 审批人照旧（含已裁决的）：holder 在 A 的直属上级 sup 经普通角色持查看码与审批码，是排班发布单（one_level_up）唯一节点的审批人。
        // 通过后已不是当前节点，按编号读原件与审批链照旧 200（审批链页通过后刷新即读这两项）；A 的普通查看人仍 403。修复前通过后读原件即 403。
        RequestPostProcessor superior = staffOfA("sup", plainRole("sup", view, "governance.supervisor_review.approve"));
        jdbcTemplate.update("INSERT INTO org_hierarchy (user_id, superior_id, updated_by) VALUES (?, ?, ?)", userId("holder"), userId("sup"),
                userId("holder"));
        String decided = reviewIdOf(postJson(holder, "/api/v1/staff/shifts", """
                {"idempotency_key": "%s", "user_id": "%s", "shift_id": null, "work_date": "2099-04-03", "start_at": "2099-04-03T01:00:00Z",
                 "end_at": "2099-04-03T09:00:00Z", "point_space_ids": [], "note": null, "night_duty_manager": false}
                """.formatted(key(), eB)));
        approve(superior, decided).andExpect(status().isOk()).andExpect(jsonPath("$.data.review_status").value("approved"));
        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + decided).with(superior))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.review_id").value(decided));
        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + decided + "/chain").with(superior)).andExpect(status().isOk());
        denied(mockMvc.perform(get("/api/v1/platform/supervisor-review/" + decided).with(viewerA)), "SUPERVISOR_REVIEW_SCOPE_FORBIDDEN");
        denied(mockMvc.perform(get("/api/v1/platform/supervisor-review/" + decided + "/chain").with(viewerA)),
                "SUPERVISOR_REVIEW_SCOPE_FORBIDDEN");
    }

    // 第十八批 F 段第 83 条（裁决 17 所选选项原文「对象有所属项目的，普通查看人按对象项目判；对象没有项目的（全平台或全国配置、手填对象、
    // 没带项目的合同）照旧按发起人；审批人、发起人本人、商户账号照旧。只改可见性，不改各发起入口。」）：A 的员工经只勾 B 的角色持有设备管理码与
    // 授权申请码（另经普通角色持查看码与主管复核发起码），替 B 的设备发起报废（scrap）、替 B 的员工 eB 申请临时权限（grant），再在主管复核页手填一份
    // 库里没有的合同发起违约复核（manual，对象没有项目）。修复前三张单都按发起人算 A：A 里只持查看码的普通员工在待复核清单里看得到 scrap、grant（第一处断言即红）。
    @Test
    void otherDomainReviewsForAnotherProjectFollowObjectProject() throws Exception {
        String view = "governance.supervisor_review.view";
        String eB = employee("eb83", entityB);
        RequestPostProcessor holder = staffOfA("h83", tickedBRole("h83", "facility.equipment.manage", "access.grant_request.apply"),
                plainRole("h83_p", view, "governance.supervisor_review.manage"));
        String equipment = "feq_m738_" + suffix;
        jdbcTemplate.update("INSERT INTO facility_equipment (id, name, equipment_type, status, project_id) VALUES (?, 'M738 冷却塔', 'hvac', 'normal', ?)",
                equipment, entityB);
        String scrap = reviewIdOf(postJson(holder, "/api/v1/facility/equipment/" + equipment + "/scrap-requests", """
                {"idempotency_key": "%s", "reason": "M738 压缩机烧毁", "disposal_method": "整机交回收商"}
                """.formatted(key())));
        String grant = reviewIdOf(postJson(holder, "/api/v1/access/grant-requests", """
                {"idempotency_key": "%s", "request_type": "temporary_permission", "target_user_id": "%s",
                 "permission_codes": ["amusement.device.view"], "expires_at": "2099-01-01T00:00:00Z", "single_use": false,
                 "reason": "M738 临时顶班", "operator_note": null}
                """.formatted(key(), eB)));
        String manual = reviewIdOf(postJson(holder, "/api/v1/platform/supervisor-review/evaluate", """
                {"idempotency_key": "%s", "action_type": "contract_violation", "object_type": "contract", "object_id": "free_83_%s",
                 "violation_summary": "M738 手填违约"}
                """.formatted(key(), suffix)));
        List<String> forB = List.of(scrap, grant);
        // 待审清单按到期时刻排序、最多 200 张：夹具单排到前面
        jdbcTemplate.update("UPDATE supervisor_review_decisions SET due_at = TIMESTAMPTZ '2000-01-01 00:00:00+00' WHERE review_id IN (?, ?, ?)",
                scrap, grant, manual);
        RequestPostProcessor viewerA = staffOfA("viewa83", plainRole("viewa83", view));
        RequestPostProcessor viewerB = account("viewb83", "staff", entityB, null, plainRole("viewb83", view));

        // 修复前红：A 的普通查看人看得到 scrap、grant
        assertThat(pendingIds(viewerA)).doesNotContainAnyElementsOf(forB).contains(manual);
        assertThat(pendingIds(viewerB)).containsAll(forB).doesNotContain(manual);
        for (String id : forB) {
            denied(mockMvc.perform(get("/api/v1/platform/supervisor-review/" + id).with(viewerA)), "SUPERVISOR_REVIEW_SCOPE_FORBIDDEN");
            mockMvc.perform(get("/api/v1/platform/supervisor-review/" + id).with(viewerB))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.review_id").value(id));
        }
        denied(mockMvc.perform(get("/api/v1/platform/supervisor-review/" + scrap + "/chain").with(viewerA)),
                "SUPERVISOR_REVIEW_SCOPE_FORBIDDEN");
        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + scrap + "/chain").with(viewerB)).andExpect(status().isOk());
        // 对象没有项目（手填对象）照旧按发起人：A 的普通查看人读得到，B 的读不到（守护）
        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + manual).with(viewerA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.review_id").value(manual));
        denied(mockMvc.perform(get("/api/v1/platform/supervisor-review/" + manual).with(viewerB)), "SUPERVISOR_REVIEW_SCOPE_FORBIDDEN");
        // 发起人本人照旧（守护）
        assertThat(pendingIds(holder)).contains(scrap, grant, manual);
        // 审批人照旧：A 里只持查看码、是报废单当前节点审批人的人看得到（守护）
        RequestPostProcessor approverA = staffOfA("apr83", plainRole("apr83", view));
        jdbcTemplate.update("INSERT INTO supervisor_review_nodes (node_id, review_id, line_code, seq, approver_kind, approver_id, status) "
                + "VALUES (?, ?, 'main', 1, 'user', ?, 'pending')", "nd_" + scrap, scrap, userId("apr83"));
        assertThat(pendingIds(approverA)).contains(scrap, manual).doesNotContain(grant);
        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + scrap).with(approverA)).andExpect(status().isOk());
    }

    // 18F 复核（裁决补充第 17 条「审批人、发起人本人、商户账号照旧」）：指定岗位审批（specific_role）不指定最终审批人时，链尾是按角色解析的节点
    // （approver_id 为空，任一经角色持该事项审批码的人裁决，批准入口照旧按发起人项目判）。A 里经角色持 bar.payroll.approve 的人照旧按发起人 A
    // 看得到、读得到审批链；只持查看码的仍按对象项目 B。修复前第一处断言即红（A 的审批人清单里没有这张单，批准入口却按 A 放行）。
    @Test
    void roleNodeApproversOfOtherDomainReviewsKeepInitiatorProject() throws Exception {
        String view = "governance.supervisor_review.view";
        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now()) "
                + "ON CONFLICT (id) DO NOTHING", entityB, entityB);
        String rule = "bpr_m738_" + suffix;
        jdbcTemplate.update("INSERT INTO bar_payroll_rules (id, rule_kind, project_id, effective_from, version, rule_json, reason, status, "
                + "platform_company_id, created_by) VALUES (?, 'charge', ?, DATE '2099-03-01', 'V1', '{}'::jsonb, 'M738 服务费', 'pending_review', ?, ?)",
                rule, entityB, entityB, userId("h83r"));
        account("h83r", "staff", entityA, null);
        String review = "rv_m738_h83r_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO supervisor_review_decisions (review_id, policy_code, action_type, object_type, object_id, blocked, review_status,
                                                         required_level, evaluated_at, initiated_by, due_at)
                VALUES (?, 'bar_payroll_rule', 'bar.payroll_rule.adjust', 'bar_payroll_rule', ?, TRUE, 'pending_review', 1, now(), ?,
                        TIMESTAMPTZ '2000-01-01 00:00:00+00')
                """, review, rule, userId("h83r"));
        roleNode(review);
        RequestPostProcessor approverA = staffOfA("rna83", plainRole("rna83", view, "bar.payroll.approve"));
        RequestPostProcessor viewerA = staffOfA("rnv83", plainRole("rnv83", view));
        RequestPostProcessor approverB = account("rnb83", "staff", entityB, null, plainRole("rnb83", view, "bar.payroll.approve"));

        assertThat(pendingReviews(approverA)).contains(review);
        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + review + "/chain").with(approverA)).andExpect(status().isOk());
        assertThat(pendingReviews(viewerA)).doesNotContain(review);
        denied(mockMvc.perform(get("/api/v1/platform/supervisor-review/" + review).with(viewerA)), "SUPERVISOR_REVIEW_SCOPE_FORBIDDEN");
        assertThat(pendingReviews(approverB)).contains(review);
    }

    // 82a-R2（第十八批 G 段第 82 条；治理地图第 659 行「所有业务数据默认按项目隔离，普通岗位没有跨项目查看权」「在所分配的权限内可以查看、
    // 处理该角色可查看项目的数据」；2026-09-24 M738-iter1 所选选项原文一；XM-M001-D 答复 3 第 10 条「要把财务、员工排班等列表对商户账号改为
    // 只按商户过滤」）：提成月结拨付清单按本公司（收款主体）、可触及项目（派驻项目）、本商户（酒吧）过滤。夹具直插 2099-03 的四行月结记录：
    // r1（本商户 ma1→A，项目 A）、r2（别家酒吧→P，项目 A）、r3（B 的酒吧→B，项目 B）、r4（B 的酒吧→A，项目 B）。
    // 修复前本公司人事拿到四行（第一处断言即红）。
    @Test
    void commissionSettlementsFollowOwnCompanyReachableProjectAndOwnMerchant() throws Exception {
        String code = "staff.payroll.manage";
        String mA1 = merchant("ma1", entityA);
        String r1 = settlementRow("r1", mA1, entityA, entityA);
        String r2 = settlementRow("r2", "mch_m738_ma2_" + suffix, entityP, entityA);
        String r3 = settlementRow("r3", "mch_m738_mb_" + suffix, entityB, entityB);
        String r4 = settlementRow("r4", "mch_m738_mb_" + suffix, entityA, entityB);

        // 修复前红：本公司人事看到 r1～r4
        assertThat(settlements(staffOfA("hr", plainRole("hr", code)))).containsExactlyInAnyOrder(r1, r4);
        // 只勾 B 的持有人：派驻项目在 B 的
        assertThat(settlements(staffOfA("tb", tickedBRole("tb", code)))).containsExactlyInAnyOrder(r3, r4);
        // 商户账号：只本商户（修复前 r1、r2）
        assertThat(settlements(account("ma", "staff", entityA, mA1, plainRole("ma", code)))).containsExactly(r1);
        // 平台管理员：全部（守护）
        assertThat(settlements(account("pa", "platform_admin", entityA, null, plainRole("pa", code))))
                .containsExactlyInAnyOrder(r1, r2, r3, r4);
    }

    // 82a-R3（docs/decisions/2026-10-18-cg-pending-decisions.md 第 20 条「由持有有效工资管理权限且在授权范围内的人员人工触发月结」；
    // 治理地图第 659 行同上）：提成月结拨付只结本人范围内的（酒吧, 主体）组，与清单同一口径；范围外的组留给有权的人结，同组不重复挂账。
    // 夹具：A 公司员工 ea 派驻到 B 项目的酒吧 mb（组 G1＝mb→A）、B 公司员工 eb 派驻到 B 项目的酒吧 mb2（组 G2＝mb2→B），2099-04 各一笔已结账计提。
    // 请求体按前端（{month}，客户端补 idempotency_key、转蛇形）。修复前 A 公司人事一次结了两组（第一处断言即红）。
    @Test
    void commissionCloseSettlesOnlyGroupsInOwnScope() throws Exception {
        // 第十八批 V 段 159-R8：mb、mb2 是 retail 商户，按所属综合体 entityB 判最终标记；2099-04 在夹具建档之后，先把该月五类标齐。
        markRangeFinal(jdbcTemplate, "project", entityB, LocalDate.of(2099, 4, 1), LocalDate.of(2099, 4, 30));
        String ea = employee("ea", entityA);
        String eb = employee("eb", entityB);
        String mb = merchant("mb", entityB);
        String mb2 = merchant("mb2", entityB);
        dispatchedCommission("g1", ea, entityA, mb, entityB);
        dispatchedCommission("g2", eb, entityB, mb2, entityB);
        RequestPostProcessor hr = staffOfA("hr", plainRole("hr", "staff.payroll.manage"));

        // 修复前红：settlements=2
        closeCommissions(hr).andExpect(status().isOk()).andExpect(jsonPath("$.data.settlements").value(1));
        assertThat(settledGroups(mb, mb2)).containsExactly(mb + "/" + entityA);
        assertThat(transferPayees(mb, mb2)).containsExactly(entityA);
        // 只勾 B 的持有人：G1、G2 都在范围内，G1 已结不重复
        closeCommissions(staffOfA("tb", tickedBRole("tb", "staff.payroll.manage")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.settlements").value(1));
        assertThat(settledGroups(mb, mb2)).containsExactlyInAnyOrder(mb + "/" + entityA, mb2 + "/" + entityB);
        // 再结一次：0 组、行数不变
        closeCommissions(hr).andExpect(status().isOk()).andExpect(jsonPath("$.data.settlements").value(0));
        assertThat(settledGroups(mb, mb2)).hasSize(2);
        assertThat(transferPayees(mb, mb2)).containsExactlyInAnyOrder(entityA, entityB);
    }

    /** 82a-R2：一条 2099-03 的提成月结记录（酒吧、收款主体、派驻项目按用例给定），返回编号。 */
    private String settlementRow(String tag, String merchantId, String companyId, String projectId) {
        String id = "bcs_m738_" + tag + "_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO bar_commission_settlements (id, month, merchant_id, company_id, project_id, amount_cent, calculation_count,
                        staff_count, due_date, created_by, created_at)
                VALUES (?, '2099-03', ?, ?, ?, 1000, 1, 1, DATE '2099-04-05', 'usr_m738', now())
                """, id, merchantId, companyId, projectId);
        return id;
    }

    private List<String> settlements(RequestPostProcessor session) throws Exception {
        return mine(session, "/api/v1/staff/commission-settlements?month=2099-03", "$.data.items[*].id");
    }

    /** 82a-R3：员工（所属主体 companyId）派驻到酒吧（派驻项目 projectId），2099-04-10 在该酒吧一笔已结账、已计提的提成 3000 分。 */
    private void dispatchedCommission(String tag, String userId, String companyId, String merchantId, String projectId) {
        String table = "tbl_m738_" + tag + "_" + suffix;
        String order = "ord_m738_" + tag + "_" + suffix;
        String bill = "bill_m738_" + tag + "_" + suffix;
        jdbcTemplate.update("INSERT INTO bar_tables (id, bar_id, table_code, table_name, table_type, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'M738 卡座', 'booth', 'available', now(), now())", table, merchantId, table);
        jdbcTemplate.update("INSERT INTO bar_orders (id, bar_id, table_id, order_source, status, total_amount_cent, currency,"
                + " created_at, updated_at, order_type) VALUES (?, ?, ?, 'staff', 'settled', 100000, 'CNY',"
                + " TIMESTAMPTZ '2099-04-10 12:00:00+00', now(), 'standard')", order, merchantId, table);
        jdbcTemplate.update("INSERT INTO bar_bills (id, bill_no, order_id, bar_id, table_id, subtotal_amount_cent, payable_amount_cent,"
                + " paid_amount_cent, currency, payment_status, settlement_status, created_at, updated_at, merchant_id, sales_staff_id)"
                + " VALUES (?, ?, ?, ?, ?, 100000, 100000, 100000, 'CNY', 'paid', 'settled', now(), now(), ?, ?)",
                bill, bill, order, merchantId, table, merchantId, userId);
        jdbcTemplate.update("INSERT INTO bar_commission_calculations (id, bill_id, sales_staff_id, sales_role_type, commission_base_cent,"
                + " commission_rate_bp, commission_amount_cent, status, created_at)"
                + " VALUES (?, ?, ?, 'staff_sales', 100000, 300, 3000, 'calculated', TIMESTAMPTZ '2099-04-10 12:00:00+00')",
                "cm_m738_" + tag + "_" + suffix, bill, userId);
        jdbcTemplate.update("INSERT INTO staff_dispatch_records (id, user_id, company_id, merchant_id, project_id, start_date, status, created_at)"
                + " VALUES (?, ?, ?, ?, ?, DATE '2099-04-01', 'active', now())",
                "dsp_m738_" + tag + "_" + suffix, userId, companyId, merchantId, projectId);
    }

    private ResultActions closeCommissions(RequestPostProcessor session) throws Exception {
        return postJson(session, "/api/v1/staff/commission-settlements/close", """
                {"idempotency_key": "%s", "month": "2099-04"}
                """.formatted(key()));
    }

    /** 两家夹具酒吧 2099-04 已落的月结组（酒吧/收款主体）。 */
    private List<String> settledGroups(String bar1, String bar2) {
        return jdbcTemplate.queryForList("SELECT merchant_id || '/' || company_id FROM bar_commission_settlements"
                + " WHERE month = '2099-04' AND merchant_id IN (?, ?)", String.class, bar1, bar2);
    }

    /** 两家夹具酒吧的拨付应付的收款主体。 */
    private List<String> transferPayees(String bar1, String bar2) {
        return jdbcTemplate.queryForList("SELECT counterparty_id FROM ar_ap_items WHERE source_type = 'dispatch_commission_transfer'"
                + " AND merchant_id IN (?, ?)", String.class, bar1, bar2);
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private String userId(String tag) {
        return "usr_m738_" + tag + "_" + suffix;
    }

    private String profileId(String tag) {
        return "stf_m738_" + tag + "_" + suffix;
    }

    private static String key() {
        return "m738-" + UUID.randomUUID();
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

    /** 归属 A、不带商户归属、不建员工档案的员工（本人公司回落到归属 A）。 */
    private RequestPostProcessor staffOfA(String tag, String... roleIds) {
        return account(tag, "staff", entityA, null, roleIds);
    }

    /** 普通角色：两项勾选都没有。 */
    private String plainRole(String tag, String... codes) {
        return role(tag, false, List.of(), codes);
    }

    /** 勾了「可以查看全部项目」的角色。 */
    private String allProjectsRole(String tag, String... codes) {
        return role(tag, true, List.of(), codes);
    }

    /** 只逐个勾选了 B 的角色。 */
    private String tickedBRole(String tag, String... codes) {
        return role(tag, false, List.of(entityB), codes);
    }

    /** 只逐个勾选了 A 的角色。 */
    private String tickedARole(String tag, String... codes) {
        return role(tag, false, List.of(entityA), codes);
    }

    private String role(String tag, boolean viewAll, List<String> ticked, String... codes) {
        String roleId = "role_m738_" + tag + "_" + suffix;
        accessRepository.createRole(new RoleRecord(roleId, "M738 " + tag + " " + suffix, null, "project", "active", Instant.now(),
                List.of(codes), null, viewAll, ticked));
        return roleId;
    }

    /** 公司自有员工（自助注册的样子）：staff 账号归属＝该主体，一行已批准的档案，登记项目＝所属公司＝该主体。返回账号编号。 */
    private String employee(String tag, String entity) {
        account(tag, "staff", entity, null);
        profile(tag, entity, entity, null);
        return userId(tag);
    }

    /** 从 A 改挂到 B 之后的员工（updateUserScope 只改 project_id）：账号归属 B，档案登记项目 B、所属公司仍是 A。返回以其身份登录的会话。 */
    private RequestPostProcessor movedToB(String tag, String... roleIds) {
        RequestPostProcessor session = account(tag, "staff", entityB, null, roleIds);
        profile(tag, entityB, entityA, null);
        return session;
    }

    private void profile(String tag, String projectId, String companyId, String merchantId) {
        jdbcTemplate.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, company_id,
                        registration_status, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'M738', ?, 'approved', 'active', now(), now())
                """, profileId(tag), projectId, merchantId, userId(tag), companyId);
    }

    /** 一条「已提交」的员工注册：还没有账号，登记项目＝所属公司＝该主体，发起人为空。 */
    private String registration(String tag, String entity) {
        jdbcTemplate.update("""
                INSERT INTO staff_profiles (id, project_id, user_id, name, company_id, registration_status, status, created_at, updated_at)
                VALUES (?, ?, NULL, 'M738', ?, 'submitted', 'active', now(), now())
                """, profileId(tag), entity, entity);
        return profileId(tag);
    }

    /** 一个在用商户，经营主体公司＝主项目＝该主体（项目表补同编号的一行，商户主项目有外键）。 */
    private String merchant(String tag, String entity) {
        String id = "mch_m738_" + tag + "_" + suffix;
        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now()) ON CONFLICT (id) DO NOTHING", entity, entity);
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, business_entity_id, project_id, status, created_at, updated_at) "
                + "VALUES (?, ?, 'retail', ?, ?, 'active', now(), now())", id, id, entity, entity);
        return id;
    }

    /** 2099-03-17 的一个班次（各表自己的 project_id 填 rowProject），返回班次编号。 */
    private String shift(String userId, String rowProject) {
        String id = "shf_" + userId;
        jdbcTemplate.update("""
                INSERT INTO staff_shift_schedules (id, user_id, work_date, start_at, end_at, created_by, created_at, updated_at, project_id)
                VALUES (?, ?, DATE '2099-03-17', TIMESTAMPTZ '2099-03-17 09:00:00+08', TIMESTAMPTZ '2099-03-17 17:00:00+08',
                        'usr_m738', now(), now(), ?)
                """, id, userId, rowProject);
        return id;
    }

    // 一条请假（状态按用例给定）。建单时间取很靠后——请假列表按建单时间倒序、最多 500 行，不过滤的人（勾全部）也要看得到夹具行。
    private String leave(String userId, String rowProject, String leaveStatus) {
        String id = "lv_" + userId;
        jdbcTemplate.update("""
                INSERT INTO staff_leave_requests (id, user_id, leave_type, start_date, end_date, reason, status, created_at, updated_at, project_id)
                VALUES (?, ?, 'annual', DATE '2099-03-18', DATE '2099-03-18', 'M738', ?, TIMESTAMPTZ '2099-03-17 00:00:00+00',
                        now(), ?)
                """, id, userId, leaveStatus, rowProject);
        return id;
    }

    /** J4：一个班次、一次打卡（2099-03-17）、一条待批请假，各表自己的 project_id 都填 rowProject。 */
    private void staffRows(String userId, String rowProject) {
        shift(userId, rowProject);
        jdbcTemplate.update("""
                INSERT INTO staff_attendance_records (id, user_id, punch_type, punched_at, method, judgement, created_at, project_id)
                VALUES (?, ?, 'check_in', TIMESTAMPTZ '2099-03-17 12:00:00+08', 'qr', 'normal', now(), ?)
                """, "att_" + userId, userId, rowProject);
        leave(userId, rowProject, "pending");
    }

    // 一张待审复核单，发起人是归属该主体的普通账号（不挂角色）。到期时刻取很早——待审清单按到期时刻排序、最多 200 张，夹具行排在前面。
    private String pendingReview(String tag, String entity) {
        String initiator = "init_" + tag;
        account(initiator, "staff", entity, null);
        String id = "rv_m738_" + tag + "_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO supervisor_review_decisions (review_id, policy_code, action_type, object_type, object_id, blocked, review_status,
                                                         required_level, evaluated_at, initiated_by, due_at)
                VALUES (?, 'm738_cross_project', 'm738.cross_project', 'm738_cross_project', ?, TRUE, 'pending_review', 2, now(), ?,
                        TIMESTAMPTZ '2000-01-01 00:00:00+00')
                """, id, id, userId(initiator));
        return id;
    }

    /** J8 补：给复核单挂一个待裁决的主线角色节点（按角色解析、不指定审批人，同审批链生成的角色节点）。 */
    private void roleNode(String reviewId) {
        jdbcTemplate.update("INSERT INTO supervisor_review_nodes (node_id, review_id, line_code, seq, approver_kind, status) "
                + "VALUES (?, ?, 'main', 1, 'role', 'pending')", "nd_" + reviewId, reviewId);
    }

    /** 一个在用的服务目录项（固定价、无管理费）。 */
    private String catalogItem() {
        String id = "svc_m738_" + suffix;
        jdbcTemplate.update("INSERT INTO service_catalog_items (id, name, price_cent, duration_minutes, active, mgmt_fee_mode, mgmt_fee_value) "
                + "VALUES (?, 'M738 点舞', 10000, 10, TRUE, 'none', 0)", id);
        return id;
    }

    /** 一张新建状态的服务单（项目按用例给定），建单时间取当前。 */
    private String serviceOrder(String tag, String performer, String projectId, String catalogItem) {
        String id = "svo_m738_" + tag + "_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO service_orders (id, catalog_item_id, item_name, price_cent, performer_user_id, status, created_at, updated_at,
                                            project_id)
                VALUES (?, ?, 'M738 点舞', 10000, ?, 'created', now(), now(), ?)
                """, id, catalogItem, performer, projectId);
        return id;
    }

    // ── 取数与请求 ───────────────────────────────────────────────────────────────────────────────

    /** 以给定身份 GET（须 200），取出该 JSON 路径下属于本次夹具的编号（都以随机后缀结尾）；共享库里别的行不看。 */
    private List<String> mine(RequestPostProcessor session, String url, String path) throws Exception {
        List<String> ids = JsonPath.read(mockMvc.perform(get(url).with(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), path);
        return ids.stream().filter(id -> id != null && id.endsWith(suffix)).toList();
    }

    private List<String> profiles(RequestPostProcessor session) throws Exception {
        return mine(session, "/api/v1/staff/profiles", "$.data.items[*].id");
    }

    /** J4：三个人事列表都只出给定员工的行。 */
    private void assertStaffLists(RequestPostProcessor session, String... userIds) throws Exception {
        for (String url : STAFF_LISTS) {
            assertThat(mine(session, url, "$.data.items[*].user_id")).as(url).containsExactlyInAnyOrder(userIds);
        }
    }

    private List<String> pendingReviews(RequestPostProcessor session) throws Exception {
        return mine(session, "/api/v1/platform/supervisor-review/pending", "$.data.items[*].decision.review_id");
    }

    /** 待复核清单里的全部复核单编号（经接口生成的单编号不带随机后缀，用例按编号比对）。 */
    private List<String> pendingIds(RequestPostProcessor session) throws Exception {
        return JsonPath.read(mockMvc.perform(get("/api/v1/platform/supervisor-review/pending").with(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.items[*].decision.review_id");
    }

    /** 提交须 200，取回复核单编号。 */
    private static String reviewIdOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.data.review_id");
    }

    private List<String> serviceOrders(RequestPostProcessor session) throws Exception {
        return mine(session, "/api/v1/services/orders", "$.data.items[*].id");
    }

    private ResultActions postJson(RequestPostProcessor session, String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(session).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** 403，错误码沿用各闸现有的。 */
    private static void denied(ResultActions result, String detailCode) throws Exception {
        result.andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value(detailCode));
    }

    /** 409：范围判定通过后，由入口自身的现行规则拒绝。 */
    private static void conflict(ResultActions result, String detailCode) throws Exception {
        result.andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value(detailCode));
    }

    private ResultActions archive(RequestPostProcessor session, String profileId) throws Exception {
        return postJson(session, "/api/v1/staff/profiles/" + profileId + "/archive", """
                {"reason": "M738 存档"}
                """);
    }

    private ResultActions salesPosition(RequestPostProcessor session, String profileId) throws Exception {
        return postJson(session, "/api/v1/staff/profiles/" + profileId + "/sales-position", """
                {"sales_position": true, "reason": "M738 设为销售岗"}
                """);
    }

    private ResultActions reject(RequestPostProcessor session, String registrationId) throws Exception {
        return postJson(session, "/api/v1/staff/registrations/" + registrationId + "/review", """
                {"idempotency_key": "%s", "decision": "reject", "reject_reason": "M738 驳回"}
                """.formatted(key()));
    }

    private ResultActions register(RequestPostProcessor session, String projectId, String merchantId) throws Exception {
        return postJson(session, "/api/v1/staff/registrations", """
                {"idempotency_key": "%s", "project_id": "%s", "merchant_id": "%s", "name": "M738 新员工"}
                """.formatted(key(), projectId, merchantId));
    }

    private ResultActions setBaseSalary(RequestPostProcessor session, String userId) throws Exception {
        return postJson(session, "/api/v1/staff/payroll/base", """
                {"idempotency_key": "%s", "user_id": "%s", "monthly_salary_cent": 500000}
                """.formatted(key(), userId));
    }

    private ResultActions cancel(RequestPostProcessor session, String orderId) throws Exception {
        return postJson(session, "/api/v1/services/orders/" + orderId + "/cancel", """
                {"idempotency_key": "%s", "reason": "M738 取消"}
                """.formatted(key()));
    }

    private ResultActions createOrder(RequestPostProcessor session, String catalogItem, String performer) throws Exception {
        return postJson(session, "/api/v1/services/orders", """
                {"idempotency_key": "%s", "catalog_item_id": "%s", "performer_user_id": "%s"}
                """.formatted(key(), catalogItem, performer));
    }

    private ResultActions approve(RequestPostProcessor session, String reviewId) throws Exception {
        return postJson(session, "/api/v1/platform/supervisor-review/" + reviewId + "/approve", """
                {"idempotency_key": "%s", "decision": "approved", "comment": "M738 同意"}
                """.formatted(key()));
    }

    /**
     * J8 补：以 tag 建归属 A、挂 roleId 的员工，给一个 M2 职级（level_rank 4 → 主管级别 2，够得上 required_level 2 的角色节点）。
     * 范围内的单：清单里有、读审批链 200、通过后节点与复核单都推进为已通过；范围外的单：清单里没有、读链与通过 403、节点与复核单不变。
     */
    private void approvesWithinReach(String tag, String roleId, String inEntity, String outEntity) throws Exception {
        RequestPostProcessor session = staffOfA(tag, roleId);
        jdbcTemplate.update("INSERT INTO staff_level_assignments (user_id, staff_level_id) VALUES (?, 'lvl_m2_lower')", userId(tag));
        String in = pendingReview(tag + "_in", inEntity);
        String out = pendingReview(tag + "_out", outEntity);
        roleNode(in);
        roleNode(out);

        assertThat(pendingReviews(session)).contains(in).doesNotContain(out);
        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + in + "/chain").with(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].node_id").value("nd_" + in));
        approve(session, in).andExpect(status().isOk()).andExpect(jsonPath("$.data.review_status").value("approved"));
        assertThat(nodeState(in)).isEqualTo("approved/" + userId(tag));
        assertThat(reviewStatus(in)).isEqualTo("approved");

        denied(mockMvc.perform(get("/api/v1/platform/supervisor-review/" + out + "/chain").with(session)),
                "SUPERVISOR_REVIEW_SCOPE_FORBIDDEN");
        denied(approve(session, out), "SUPERVISOR_REVIEW_SCOPE_FORBIDDEN");
        assertThat(nodeState(out)).isEqualTo("pending/");
        assertThat(reviewStatus(out)).isEqualTo("pending_review");
    }

    /**
     * J9 补：以给定身份——列表里有范围内的单、没有范围外的单；取消范围内的成功、库里已取消，取消范围外的 403、不变；
     * 为范围内的履约人下单成功、新单项目＝其档案项目，为范围外的下单 403、没有新单。
     */
    private void ordersWithinReach(RequestPostProcessor session, String tag, String catalogItem, String inPerformer, String inProject,
                                   String outPerformer, String outProject) throws Exception {
        String in = serviceOrder(tag + "_in", inPerformer, inProject, catalogItem);
        String out = serviceOrder(tag + "_out", outPerformer, outProject, catalogItem);

        assertThat(serviceOrders(session)).contains(in).doesNotContain(out);
        cancel(session, in).andExpect(status().isOk());
        assertThat(orderStatus(in)).isEqualTo("cancelled");
        denied(cancel(session, out), "CROSS_PROJECT_FORBIDDEN");
        assertThat(orderStatus(out)).isEqualTo("created");

        String created = JsonPath.read(createOrder(session, catalogItem, inPerformer).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        assertThat(jdbcTemplate.queryForObject("SELECT project_id FROM service_orders WHERE id = ?", String.class, created))
                .isEqualTo(inProject);
        int before = ordersOf(outPerformer);
        denied(createOrder(session, catalogItem, outPerformer), "CROSS_PROJECT_FORBIDDEN");
        assertThat(ordersOf(outPerformer)).isEqualTo(before);
    }

    private java.sql.Timestamp archivedAt(String profileId) {
        return jdbcTemplate.queryForObject("SELECT archived_at FROM staff_profiles WHERE id = ?", java.sql.Timestamp.class, profileId);
    }

    private String registrationStatus(String profileId) {
        return jdbcTemplate.queryForObject("SELECT registration_status FROM staff_profiles WHERE id = ?", String.class, profileId);
    }

    private String leaveStatus(String leaveId) {
        return jdbcTemplate.queryForObject("SELECT status FROM staff_leave_requests WHERE id = ?", String.class, leaveId);
    }

    private int adjustmentRequestCount(String userId) {
        Integer n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM staff_payroll_adjustment_requests WHERE user_id = ?", Integer.class, userId);
        return n == null ? 0 : n;
    }

    private String orderStatus(String orderId) {
        return jdbcTemplate.queryForObject("SELECT status FROM service_orders WHERE id = ?", String.class, orderId);
    }

    private int ordersOf(String performer) {
        Integer n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM service_orders WHERE performer_user_id = ?", Integer.class, performer);
        return n == null ? 0 : n;
    }

    private String reviewStatus(String reviewId) {
        return jdbcTemplate.queryForObject("SELECT review_status FROM supervisor_review_decisions WHERE review_id = ?", String.class, reviewId);
    }

    /** 节点状态／裁决人（未裁决为空）。 */
    private String nodeState(String reviewId) {
        return jdbcTemplate.queryForObject("SELECT status || '/' || coalesce(decided_by, '') FROM supervisor_review_nodes WHERE node_id = ?",
                String.class, "nd_" + reviewId);
    }
}
