package com.complex.platform.access.staff;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.LocalZone;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AB 段第 191 条 191-R2：派驻酒吧可给派驻员工排班、顶班、核时并看其排班与考勤（真库、真接口、请求体按前端蛇形键）。
 * 依据：BSM 第 1143 行「其他公司的人员派驻到某家酒吧工作期间，由该酒吧负责排班、现场考勤和履约调度」；第 1474 行「主管排班：排到几点几分，
 * 可指定当天工作点位……由领班直接安排其他工作人员顶班……」；决策台账第 328 行「派驻期间由服务经营主体负责排班、现场考勤和履约调度」。
 * 派驻是否覆盖某日只看起止日期；此刻是否在期按本酒吧营业日、考勤列表按打卡时点所属营业日（第十八批裁决 46）；工资单等仍不交派驻酒吧。夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class DispatchedStaffShiftJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BusinessDayResolver businessDays;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "prj_dss_" + suffix;
    private final String merchant = "mch_dss_" + suffix;
    private final String barCompany = "ent_dss_bar_" + suffix;
    private final String staffCompany = "ent_dss_staff_" + suffix;
    private final String employee = "usr_dss_e_" + suffix;
    private final String idle = "usr_dss_i_" + suffix;
    private final String own = "usr_dss_o_" + suffix;
    private final AtomicInteger seq = new AtomicInteger();
    private LocalDate today;
    private RequestPostProcessor manager;
    private RequestPostProcessor operator;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "派驻排班项目 " + suffix);
        for (String company : List.of(barCompany, staffCompany)) {
            jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) "
                    + "VALUES(?,'派驻排班主体','management_company','active',now(),now())", company);
        }
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", barCompany, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,"
                + "created_at,updated_at) VALUES(?,'派驻排班酒吧','bar',?,?,?,?,'active',now(),now())", merchant, barCompany, barCompany, barCompany, project);
        // 第十八批裁决 46：派驻在期按本酒吧营业日判，「今天」取此刻所属本酒吧营业日（北京日历日零点到起点前仍属前一营业日）。
        today = businessDays.merchantBusinessDate(null, merchant, Instant.now());
        // 公司自有员工（派驻的与未派驻的）：档案登记项目＝所属公司、无商户；账号项目＝所属公司、无商户号。本店员工：档案挂本项目本商户。
        for (String user : List.of(employee, idle)) {
            user(user, staffCompany, null);
            profile(user, staffCompany, null, staffCompany);
        }
        user(own, project, merchant);
        profile(own, project, merchant, barCompany);
        manager = account("usr_dss_m_" + suffix, "staff", project, merchant,
                role("排班", "'staff.shift.manage','staff.attendance.view','staff.payroll.manage'"));
        operator = account("usr_dss_p_" + suffix, "platform_admin", "org_1", null, role("派驻登记", "'staff.profile.review'"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 派驻今天起、结束日期后天：酒吧商户账号给派驻员工排派驻期内的班（单排、按周批量）进审批；未派驻的其他公司员工、派驻结束日之后的班照旧 403。 */
    @Test
    void barSchedulesDispatchedStaffWithinTheDispatchOnly() throws Exception {
        endDispatch(dispatch(today), today.plusDays(2));

        // 修复前红：403 CROSS_ORG_FORBIDDEN
        shift(employee, today.plusDays(1)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_id").value(employee))
                .andExpect(jsonPath("$.data.publication_status").value("pending_review"));
        denied(shift(idle, today.plusDays(1)), "CROSS_ORG_FORBIDDEN");
        denied(shift(employee, today.plusDays(3)), "CROSS_ORG_FORBIDDEN");

        batch(today.plusDays(1), 2, employee, own).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.created_count").value(4))
                .andExpect(jsonPath("$.data.publication_status").value("pending_review"));
        denied(batch(today.plusDays(1), 3, employee), "CROSS_ORG_FORBIDDEN");
    }

    /** 派驻期内：领班安排派驻员工给本店员工顶班；派驻员工自己的班次可看排班历史与顶班记录；未派驻的其他公司员工照旧不能顶班。 */
    @Test
    void barArrangesDispatchedStaffAsCoverAndReadsTheirShift() throws Exception {
        dispatch(today);
        LocalDate day = today.plusDays(1);
        String source = publishedShift(own, project, day);

        // 修复前红：403 CROSS_ORG_FORBIDDEN
        cover(source, employee, day).andExpect(status().isOk()).andExpect(jsonPath("$.data.cover_user_id").value(employee));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_shift_covers WHERE shift_id=?", Integer.class, source)).isEqualTo(1);
        denied(cover(source, idle, day), "CROSS_ORG_FORBIDDEN");

        String theirs = publishedShift(employee, staffCompany, day.plusDays(1));
        mockMvc.perform(get("/api/v1/staff/shifts/" + theirs + "/history").with(manager)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/staff/shifts/" + theirs + "/covers").with(manager)).andExpect(status().isOk());
    }

    /** 派驻今天起、结束日期后天：排班与考勤列表含派驻员工派驻期内的班次与打卡，派驻结束日之后的不含；未派驻员工不含；工资单列表不含派驻员工。 */
    @Test
    void barListsDispatchedStaffShiftsAndAttendanceWithinTheDispatchOnly() throws Exception {
        endDispatch(dispatch(today), today.plusDays(2));
        String inside = publishedShift(employee, staffCompany, today.plusDays(1));
        String after = publishedShift(employee, staffCompany, today.plusDays(3));
        String idleShift = publishedShift(idle, staffCompany, today.plusDays(1));
        String ownShift = publishedShift(own, project, today.plusDays(1));
        String punch = punch(employee, today);
        String latePunch = punch(employee, today.plusDays(3));
        String statement = "pst_dss_" + suffix;
        jdbc.update("INSERT INTO staff_payroll_statements (id, user_id, month, base_cent, total_cent, status, project_id, payer_company_id, merchant_id) "
                + "VALUES (?, ?, ?, 500000, 500000, 'closed', ?, ?, NULL)", statement, employee, YearMonth.now(LocalZone.ID).toString(),
                staffCompany, staffCompany);

        // 修复前红：只出档案挂本商户的员工
        assertThat(ids("/api/v1/staff/shifts?date=" + today.plusDays(1))).contains(inside, ownShift).doesNotContain(idleShift);
        assertThat(ids("/api/v1/staff/shifts?date=" + today.plusDays(3))).doesNotContain(after);
        assertThat(ids("/api/v1/staff/attendance?date=" + today)).contains(punch);
        assertThat(ids("/api/v1/staff/attendance?date=" + today.plusDays(3))).doesNotContain(latePunch);
        assertThat(ids("/api/v1/staff/payroll?month=" + YearMonth.now(LocalZone.ID))).doesNotContain(statement);
    }

    /** 派驻今天起：酒吧商户账号看派驻员工的工作手机领取事件（现场考勤）；未派驻的其他公司员工照旧 403。 */
    @Test
    void barViewsDispatchedStaffWorkPhoneReceiptsToday() throws Exception {
        dispatch(today);

        // 修复前红：403 STAFF_SCOPE_MISMATCH
        mockMvc.perform(get("/api/v1/staff/attendance/receipt-events").param("user_id", employee).with(manager)).andExpect(status().isOk());
        denied(mockMvc.perform(get("/api/v1/staff/attendance/receipt-events").param("user_id", idle).with(manager)), "STAFF_SCOPE_MISMATCH");
    }

    /** 第十八批裁决 46「结束日当天的夜班过零点后、到营业日结束前仍算派驻期内」：结束日＝此刻所属营业日（北京日历日已是次日），酒吧商户账号照常看派驻员工的领取事件；结束日早于此刻所属营业日的 403。 */
    @Test
    void endDateNightAfterMidnightStillViewsReceipts() throws Exception {
        crossMidnight();
        String dispatchId = dispatch(today);
        endDispatch(dispatchId, today);

        // 修复前红：按北京日历日判，403 STAFF_SCOPE_MISMATCH
        mockMvc.perform(get("/api/v1/staff/attendance/receipt-events").param("user_id", employee).with(manager)).andExpect(status().isOk());

        jdbc.update("UPDATE staff_dispatch_records SET start_date=?, end_date=? WHERE id=?", today.minusDays(1), today.minusDays(1), dispatchId);
        denied(mockMvc.perform(get("/api/v1/staff/attendance/receipt-events").param("user_id", employee).with(manager)), "STAFF_SCOPE_MISMATCH");
    }

    /** 同裁决（ENUM:187 考勤日期按营业日）：考勤列表按打卡时点所属营业日判派驻——结束日次日零点后（仍属结束日营业日）的打卡列入，次日营业日起点后的不列。 */
    @Test
    void attendanceListCountsAfterMidnightPunchesToTheEndDate() throws Exception {
        endDispatch(dispatch(today.minusDays(2)), today.minusDays(1));
        Instant night = ts(today, 0).toInstant().plusSeconds(1800);
        Instant noon = ts(today, 12).toInstant();
        assertThat(businessDays.merchantBusinessDate(null, merchant, night)).isEqualTo(today.minusDays(1));
        assertThat(businessDays.merchantBusinessDate(null, merchant, noon)).isEqualTo(today);
        String afterMidnight = punch(employee, night);
        String nextDay = punch(employee, noon);

        // 修复前红：按查询日（北京日历日）判，零点后的打卡不列
        assertThat(ids("/api/v1/staff/attendance?date=" + today)).contains(afterMidnight).doesNotContain(nextDay);
    }

    /**
     * 第十八批 AB 段复核意见（第 191 条；BSM:1143「其他公司的人员派驻到某家酒吧工作期间，由该酒吧负责排班、现场考勤和履约调度。」；PGM:659「数据的项目与商户隔离
     * 不提供关闭开关」）：派驻放行只给看、只给办本酒吧设备上的领取事件——派驻员工在别家酒吧设备上的事件，本酒吧主管查列表看不到、核时与看审批历史 403；
     * 本酒吧设备上的照常。
     */
    @Test
    void receiptsOnAnotherBarsDeviceStayWithThatBar() throws Exception {
        dispatch(today);
        String here = receiptEvent(merchant);
        String other = "mch_dss_x_" + suffix;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,"
                + "created_at,updated_at) VALUES(?,'派驻排班别家酒吧','bar',?,?,?,?,'active',now(),now())", other, barCompany, barCompany, barCompany, project);
        String elsewhere = receiptEvent(other);

        // 修复前红：列表含别家酒吧设备上的事件
        List<String> listed = JsonPath.read(mockMvc.perform(get("/api/v1/staff/attendance/receipt-events").param("user_id", employee).with(manager))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data[*].id");
        assertThat(listed).contains(here).doesNotContain(elsewhere);
        denied(review(elsewhere), "STAFF_SCOPE_MISMATCH");
        denied(mockMvc.perform(get("/api/v1/staff/attendance/receipt-events/" + elsewhere + "/reviews").with(manager)), "STAFF_SCOPE_MISMATCH");
        mockMvc.perform(get("/api/v1/staff/attendance/receipt-events/" + here + "/reviews").with(manager)).andExpect(status().isOk());
        // 本酒吧设备上的过了范围闸，到「须填写核实的实际领取时间」校验。
        review(here).andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    /** 平台侧获授权人员按后台派驻登记请求（蛇形键）把派驻员工登记到本酒吧，返回派驻记录编号。 */
    private String dispatch(LocalDate startDate) throws Exception {
        String body = "{\"idempotency_key\":\"dss-d-" + seq.incrementAndGet() + "-" + suffix + "\",\"user_id\":\"" + employee
                + "\",\"merchant_id\":\"" + merchant + "\",\"start_date\":\"" + startDate + "\",\"note\":\"派驻到酒吧\"}";
        return JsonPath.read(postJson(operator, "/api/v1/staff/dispatches", body).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    private void endDispatch(String dispatchId, LocalDate endDate) throws Exception {
        String body = "{\"idempotency_key\":\"dss-end-" + seq.incrementAndGet() + "-" + suffix + "\",\"end_date\":\"" + endDate
                + "\",\"reason\":\"派驻期满\"}";
        postJson(operator, "/api/v1/staff/dispatches/" + dispatchId + "/end", body).andExpect(status().isOk());
    }

    /** 后台排班页单排一班（蛇形键）：该日 20:00～23:00。 */
    private ResultActions shift(String userId, LocalDate day) throws Exception {
        String body = "{\"idempotency_key\":\"dss-s-" + seq.incrementAndGet() + "-" + suffix + "\",\"user_id\":\"" + userId
                + "\",\"shift_id\":null,\"work_date\":\"" + day + "\",\"start_at\":\"" + at(day, 20) + "\",\"end_at\":\"" + at(day, 23)
                + "\",\"point_space_ids\":[],\"note\":null,\"night_duty_manager\":false}";
        return postJson(manager, "/api/v1/staff/shifts", body);
    }

    /** 后台按周批量排班（蛇形键）：起始日起 days 天、每天 10:00～12:00。 */
    private ResultActions batch(LocalDate startDate, int days, String... userIds) throws Exception {
        String users = String.join("\",\"", userIds);
        String body = "{\"idempotency_key\":\"dss-b-" + seq.incrementAndGet() + "-" + suffix + "\",\"user_ids\":[\"" + users
                + "\"],\"start_date\":\"" + startDate + "\",\"days\":" + days + ",\"weekdays\":[],\"start_time\":\"10:00\",\"end_time\":\"12:00\","
                + "\"point_space_ids\":[],\"note\":null,\"night_duty_manager\":false}";
        return postJson(manager, "/api/v1/staff/shifts/batch", body);
    }

    /** 领班安排临时顶班（蛇形键）：原班次内 15:00～17:00。 */
    private ResultActions cover(String shiftId, String coverUserId, LocalDate day) throws Exception {
        String body = "{\"idempotency_key\":\"dss-c-" + seq.incrementAndGet() + "-" + suffix + "\",\"cover_user_id\":\"" + coverUserId
                + "\",\"reason\":\"派驻员工顶班\",\"start_at\":\"" + at(day, 15) + "\",\"end_at\":\"" + at(day, 17)
                + "\",\"handover_note\":\"现场交接\"}";
        return postJson(manager, "/api/v1/staff/shifts/" + shiftId + "/cover", body);
    }

    /** 已生效班次：该日 14:00～22:00。 */
    private String publishedShift(String userId, String rowProject, LocalDate day) {
        String id = "shf_dss_" + suffix + "_" + seq.incrementAndGet();
        jdbc.update("INSERT INTO staff_shift_schedules (id, user_id, work_date, start_at, end_at, created_by, created_at, updated_at, project_id) "
                + "VALUES (?, ?, ?, ?, ?, 'usr_dss', now(), now(), ?)", id, userId, day, ts(day, 14), ts(day, 22), rowProject);
        return id;
    }

    /** 该日 12:00 一次签到。 */
    private String punch(String userId, LocalDate day) {
        return punch(userId, ts(day, 12).toInstant());
    }

    /** 该时刻一次签到。 */
    private String punch(String userId, Instant at) {
        String id = "att_dss_" + suffix + "_" + seq.incrementAndGet();
        jdbc.update("INSERT INTO staff_attendance_records (id, user_id, punch_type, punched_at, method, judgement, created_at, project_id) "
                + "VALUES (?, ?, 'check_in', ?, 'manual', 'normal', now(), ?)", id, userId, Timestamp.from(at), staffCompany);
        return id;
    }

    /**
     * 第十八批裁决 46 跨零点夹具：本综合体已批准营业日版本的所在地时区取 UTC-6（BusinessDayResolver 按综合体版本取酒吧时区），本酒吧起点仍为出厂 10:00，
     * 此刻所属本酒吧营业日恒为北京日历日的前一天，相当于「结束日当天的夜班过零点后、到营业日结束前」；today 随之改取此刻所属营业日。
     */
    private void crossMidnight() {
        Instant effective = Instant.parse("2026-01-01T00:00:00Z");
        jdbc.update("INSERT INTO amusement_business_day_configs(id,project_id,version,zone_id,cutoff_hour,min_cutoff_hour,max_cutoff_hour,"
                + "close_hour,min_close_hour,max_close_hour,effective_at,status,reason,created_by,created_at,decided_by,decided_at)"
                + " VALUES(?,?,'V0.01','Etc/GMT+6',10,0,23,7,0,23,?,'approved','跨零点测试版本','usr_dss',?,'usr_master',?)",
                "abd_dss_" + suffix, project, Timestamp.from(effective), Timestamp.from(effective.minusSeconds(7200)),
                Timestamp.from(effective.minusSeconds(3600)));
        businessDays.invalidate();
        today = businessDays.merchantBusinessDate(null, merchant, Instant.now());
        assertThat(today).isEqualTo(LocalDate.now(LocalZone.ID).minusDays(1));
    }

    /** 派驻员工在某酒吧工作手机上的一条待核实领取事件（设备挂该酒吧商户）。 */
    private String receiptEvent(String merchantId) {
        String n = suffix + "_" + seq.incrementAndGet();
        String space = "spc_dss_" + n;
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) "
                + "VALUES(?,?,?,'卡座区','unit','active',now(),now())", space, project, merchantId);
        String device = "dss-phone-" + n;
        jdbc.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, bound_space_id,"
                + " location_point_id, status, bound_at, created_at, updated_at, merchant_id)"
                + " VALUES (?, ?, ?, 'staff_work_phone', 'space', ?, ?, 'bound', now(), now(), now(), ?)",
                "td_dss_" + n, device, device, space, space, merchantId);
        String id = "wpe_dss_" + n;
        jdbc.update("INSERT INTO staff_work_phone_events (id, submitted_by, device_id, method, payload, user_id, status, received_at)"
                + " VALUES (?, ?, ?, 'nfc', '{}'::jsonb, ?, 'pending_review', now())", id, employee, device, employee);
        return id;
    }

    /** 主管对领取事件发起核时（蛇形键），只带原因。 */
    private ResultActions review(String eventId) throws Exception {
        return postJson(manager, "/api/v1/staff/attendance/receipt-events/" + eventId + "/review",
                "{\"idempotency_key\":\"dss-rv-" + seq.incrementAndGet() + "-" + suffix + "\",\"reason\":\"离线领取核时\"}");
    }

    private List<String> ids(String url) throws Exception {
        return JsonPath.read(mockMvc.perform(get(url).with(manager)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.items[*].id");
    }

    private static void denied(ResultActions result, String code) throws Exception {
        result.andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value(code));
    }

    private ResultActions postJson(RequestPostProcessor session, String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(session).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String at(LocalDate day, int hour) {
        return day.atTime(hour, 0).atZone(LocalZone.ID).toInstant().toString();
    }

    private static Timestamp ts(LocalDate day, int hour) {
        return Timestamp.from(day.atTime(hour, 0).atZone(LocalZone.ID).toInstant());
    }

    private void user(String userId, String organization, String merchantId) {
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, organization, merchantId);
    }

    private void profile(String userId, String projectId, String merchantId, String companyId) {
        jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,company_id,registration_status,status,employment_status,"
                + "is_sales_position,created_at,updated_at) VALUES(?,?,?,?,'派驻排班员工',?,'approved','active','employed',false,now(),now())",
                "pro_" + userId, projectId, merchantId, userId, companyId);
    }

    private String role(String name, String permissions) {
        String role = "role_dss_" + seq.incrementAndGet() + "_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'派驻排班回归','project','active',now())",
                role, "派驻排班" + name + System.nanoTime());
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN("
                + permissions + ")", role);
        return role;
    }

    private RequestPostProcessor account(String userId, String type, String organization, String merchantId, String role) {
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,?,?,?,'active',now())",
                userId, userId, type, organization, merchantId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        if ("staff".equals(type)) {
            jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, organization);
        }
        AccessActor actor = new AccessActor(userId, userId, type, organization, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
