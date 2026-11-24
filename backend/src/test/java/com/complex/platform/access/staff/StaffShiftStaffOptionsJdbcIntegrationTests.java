package com.complex.platform.access.staff;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.LocalZone;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
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
 * 第十八批 CJ 段第 416 条 416-R1（BSM:1474「主管排班：排到几点几分……主管在手机上也能排。」「由领班直接安排其他工作人员顶班并在系统中记录顶班人员」；
 * BSM:1143「其他公司的人员派驻到某家酒吧工作期间，由该酒吧负责排班、现场考勤和履约调度。」；第十八批裁决 46「凡判断是否在派驻期内都按酒吧营业日」；
 * 第十八批裁决 71）：排班可选员工清单与提交排班同一范围判定——所属公司的主管按本公司员工（M738 本人那一份）、酒吧商户账号含本商户员工与派驻到本店
 * 且结束日不早于本店今日营业日的员工、最高权限看全部；不列停岗、离职、已存档与未通过登记（没有账号）的。
 * 真实 HTTP、蛇形键、真实授权链；夹具随测试事务回滚，编号带每次运行的随机后缀。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class StaffShiftStaffOptionsJdbcIntegrationTests {

    private static final String URL = "/api/v1/staff/shift-staff-options";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BusinessDayResolver businessDays;

    private final String s = UUID.randomUUID().toString().substring(0, 8);
    private final String entityA = "be_b108so_a_" + s;
    private final String entityB = "be_b108so_b_" + s;
    private int seq;

    @BeforeEach
    void entities() {
        for (String entity : List.of(entityA, entityB)) {
            jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) "
                    + "VALUES (?, ?, 'project_company', 'active', now(), now())", entity, "排班可选员工公司" + entity);
        }
    }

    // ①所属公司 A 的主管：A 的在职与待入职员工，带档案姓名；B 的、停岗、离职、已存档、未通过登记的不列。修复前接口不存在（状态断言失败）。
    @Test
    void companySupervisorListsOwnCompanySchedulableStaffOnly() throws Exception {
        String employed = employee("ea", entityA, null, entityA, "employed", "张三" + s);
        String pending = employee("ep", entityA, null, entityA, "pending_entry", "李四" + s);
        employee("eb", entityB, null, entityB, "employed", "王五" + s);
        employee("es", entityA, null, entityA, "suspended", "停岗员工" + s);
        employee("ed", entityA, null, entityA, "departed", "离职员工" + s);
        String archived = employee("ex", entityA, null, entityA, "employed", "存档员工" + s);
        jdbc.update("UPDATE staff_profiles SET archived_at = now(), archived_by = 'usr_b108so' WHERE user_id = ?", archived);
        jdbc.update("INSERT INTO staff_profiles (id, project_id, user_id, name, company_id, registration_status, status, created_at, updated_at) "
                + "VALUES (?, ?, NULL, ?, ?, 'submitted', 'active', now(), now())", "stf_b108so_reg_" + s, entityA, "未登记员工" + s, entityA);

        String body = options(supervisorOf(entityA));
        assertThat(JsonPath.<List<String>>read(body, "$.data.items[*].user_id")).containsExactlyInAnyOrder(employed, pending);
        assertThat(JsonPath.<List<String>>read(body, "$.data.items[*].name")).containsExactlyInAnyOrder("张三" + s, "李四" + s);
    }

    // ②酒吧商户账号：本商户员工与派驻到本店、结束日为空或不早于今日营业日的他公司员工（含结束日即今日营业日的、下周才开始的）；他商户员工、派驻已于前天结束的、派驻在期但已离职的不列。
    @Test
    void barAccountListsOwnStaffAndDispatchedStaffWithinDispatch() throws Exception {
        String project = "prj_b108so_" + s;
        String barCompany = "ent_b108so_bar_" + s;
        String staffCompany = "ent_b108so_staff_" + s;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "排班可选员工综合体" + s);
        for (String company : List.of(barCompany, staffCompany)) {
            jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) "
                    + "VALUES(?,'排班可选员工主体','management_company','active',now(),now())", company);
        }
        String bar = bar("a", project, barCompany);
        String otherBar = bar("b", project, barCompany);
        LocalDate today = businessDays.merchantBusinessDate(null, bar, Instant.now());
        String own = employee("own", project, bar, barCompany, "employed", "本店员工" + s);
        employee("ob", project, otherBar, barCompany, "employed", "他店员工" + s);
        String dispatched = employee("dp", staffCompany, null, staffCompany, "employed", "派驻员工" + s);
        dispatch(dispatched, staffCompany, bar, project, today, null);
        String ended = employee("de", staffCompany, null, staffCompany, "employed", "派驻期满员工" + s);
        dispatch(ended, staffCompany, bar, project, today.minusDays(5), today.minusDays(2));
        String departed = employee("dd", staffCompany, null, staffCompany, "departed", "派驻离职员工" + s);
        dispatch(departed, staffCompany, bar, project, today, null);
        String endsToday = employee("dt", staffCompany, null, staffCompany, "employed", "今日派驻期满员工" + s);
        dispatch(endsToday, staffCompany, bar, project, today.minusDays(5), today);
        String upcoming = employee("du", staffCompany, null, staffCompany, "employed", "下周派驻员工" + s);
        dispatch(upcoming, staffCompany, bar, project, today.plusDays(7), null);

        String body = options(new StaffRequestFixture(jdbc).staff("usr_b108so_bm_" + s, "staff", project, bar, "staff.shift.manage"));
        assertThat(JsonPath.<List<String>>read(body, "$.data.items[*].user_id")).containsExactlyInAnyOrder(own, dispatched, endsToday, upcoming);
        assertThat(JsonPath.<List<String>>read(body, "$.data.items[*].name")).containsExactlyInAnyOrder("本店员工" + s, "派驻员工" + s,
                "今日派驻期满员工" + s, "下周派驻员工" + s);
    }

    // ③最高权限（第十八批裁决 71）：A、B 两家公司的员工都在。
    @Test
    void masterAuthorityListsStaffOfEveryCompany() throws Exception {
        String a = employee("ma", entityA, null, entityA, "employed", "甲公司员工" + s);
        String b = employee("mb", entityB, null, entityB, "employed", "乙公司员工" + s);

        assertThat(JsonPath.<List<String>>read(options(master()), "$.data.items[*].user_id")).contains(a, b);
    }

    // ④不持 staff.shift.manage：403。
    @Test
    void callerWithoutShiftManageIsForbidden() throws Exception {
        RequestPostProcessor viewer = new StaffRequestFixture(jdbc).staff("usr_b108so_v_" + s, "staff", entityA, null, "staff.attendance.view");
        mockMvc.perform(get(URL).with(viewer)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    // ⑤清单与提交同口径：①列出的每名员工都能提交排班（200），被剔除的 B 公司员工提交 403。
    @Test
    void listedStaffCanBeScheduledAndExcludedStaffCannot() throws Exception {
        employee("fa", entityA, null, entityA, "employed", "可排员工甲" + s);
        employee("fp", entityA, null, entityA, "pending_entry", "可排员工乙" + s);
        String other = employee("fb", entityB, null, entityB, "employed", "他公司员工" + s);
        RequestPostProcessor supervisor = supervisorOf(entityA);

        List<String> listed = JsonPath.read(options(supervisor), "$.data.items[*].user_id");
        assertThat(listed).hasSize(2).doesNotContain(other);
        int day = 1;
        for (String userId : listed) {
            shift(supervisor, userId, day++).andExpect(status().isOk());
        }
        shift(supervisor, other, day).andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_ORG_FORBIDDEN"));
    }

    // ⑥裁决 46 按酒吧营业日：本店营业日起点配在此刻之后（此刻仍属前一营业日，与北京自然日不同），结束日为本店今日营业日（即昨天）的派驻员工照列。
    @Test
    void dispatchEndingOnTheBarsBusinessDayIsListedWhileItDiffersFromTheCalendarDay() throws Exception {
        ZonedDateTime local = Instant.now().atZone(LocalZone.ID);
        assumeTrue(local.getHour() < 23 && local.getMinute() < 58, "北京 23 点后没有更晚的整点起点可配，整点前两分钟内不跑");
        String project = "prj_b108so_n_" + s;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "营业日派驻综合体" + s);
        String bar = bar("n", project, entityA);
        Instant effective = Instant.now().minus(Duration.ofDays(3));
        jdbc.update("INSERT INTO bar_business_day_configs(id,bar_id,project_id,version,cutoff_hour,min_cutoff_hour,max_cutoff_hour,close_hour,"
                + "min_close_hour,max_close_hour,effective_at,status,reason,created_by,created_at,decided_by,decided_at) "
                + "VALUES(?,?,?,'V0.01',?,0,23,7,0,23,?,'approved','测试已批准版本','usr_b108so',?,'usr_master',?)",
                "bbd_b108so_" + s, bar, project, local.getHour() + 1, Timestamp.from(effective),
                Timestamp.from(effective.minusSeconds(7200)), Timestamp.from(effective.minusSeconds(3600)));
        LocalDate today = businessDays.merchantBusinessDate(null, bar, Instant.now());
        assertThat(today).isEqualTo(local.toLocalDate().minusDays(1));
        String lastDay = employee("dn", entityB, null, entityB, "employed", "营业日派驻员工" + s);
        dispatch(lastDay, entityB, bar, project, today.minusDays(5), today);

        String body = options(new StaffRequestFixture(jdbc).staff("usr_b108so_bn_" + s, "staff", project, bar, "staff.shift.manage"));
        assertThat(JsonPath.<List<String>>read(body, "$.data.items[*].user_id")).containsExactly(lastDay);
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    /** 所属公司为该主体、不带商户归属、不建员工档案的主管（普通角色持 staff.shift.manage；本人公司回落到账号归属）。 */
    private RequestPostProcessor supervisorOf(String entity) {
        return new StaffRequestFixture(jdbc).staff("usr_b108so_sup_" + (++seq) + "_" + s, "staff", entity, null, "staff.shift.manage");
    }

    /** 一名员工：staff 账号＋一行已通过登记的档案，登记项目、商户、所属公司、任职状态与姓名按给定。返回账号编号。 */
    private String employee(String tag, String projectId, String merchantId, String companyId, String employmentStatus, String name) {
        String userId = "usr_b108so_" + tag + "_" + s;
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, projectId, merchantId);
        jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,company_id,registration_status,status,employment_status,"
                + "is_sales_position,created_at,updated_at) VALUES(?,?,?,?,?,?,'approved','active',?,false,now(),now())",
                "stf_b108so_" + tag + "_" + s, projectId, merchantId, userId, name, companyId, employmentStatus);
        return userId;
    }

    private String bar(String tag, String project, String company) {
        String id = "mch_b108so_" + tag + "_" + s;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,"
                + "created_at,updated_at) VALUES(?,?,'bar',?,?,?,?,'active',now(),now())", id, "排班可选员工酒吧" + tag, company, company, company, project);
        return id;
    }

    private void dispatch(String userId, String company, String merchant, String project, LocalDate start, LocalDate end) {
        jdbc.update("INSERT INTO staff_dispatch_records(id,user_id,company_id,merchant_id,project_id,start_date,end_date,status,note,created_by,created_at) "
                + "VALUES(?,?,?,?,?,?,?,?,'派驻到酒吧','usr_b108so',now())", "dsp_b108so_" + (++seq) + "_" + s, userId, company, merchant, project,
                start, end, end == null ? "active" : "ended");
    }

    private String options(RequestPostProcessor who) throws Exception {
        return mockMvc.perform(get(URL).with(who)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** 后台排班页单排一班（蛇形键）：2099-06-day 北京 09:00～17:00。 */
    private ResultActions shift(RequestPostProcessor who, String userId, int day) throws Exception {
        String date = "2099-06-0" + day;
        String body = "{\"idempotency_key\":\"b108so-s-" + (++seq) + "-" + s + "\",\"user_id\":\"" + userId + "\",\"shift_id\":null,\"work_date\":\""
                + date + "\",\"start_at\":\"" + date + "T01:00:00Z\",\"end_at\":\"" + date + "T09:00:00Z\",\"point_space_ids\":[],\"note\":null,"
                + "\"night_duty_manager\":false}";
        return mockMvc.perform(post("/api/v1/staff/shifts").with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
