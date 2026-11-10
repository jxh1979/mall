package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.LocalZone;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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
 * 第十八批 AB 段第 191 条 191-R4：派驻人员可任该酒吧的主责服务人（真库、真接口、请求体按前端蛇形键）。依据：BSM 第 1143 行「其他公司的人员派驻到某家酒吧
 * 工作期间，由该酒吧负责排班、现场考勤和履约调度」「其中应付给本人的提成、服务费等报酬，由该酒吧按月拨付给人员所属主体」；第 1077 行「主责服务提供人按当日该场次
 * 排班指派到该卡座」；第 1474 行「顶班人员按顶班记录取得该班次的小费与服务费份额」；决策台账第 328 行「派驻期间由服务经营主体负责排班、现场考勤和履约调度」。
 * 派驻是否在期按本酒吧营业日、只看起止日期（第十八批裁决 46）；服务责任的付款主体仍取员工档案所属主体。夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarDispatchedServiceStaffJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BusinessDayResolver businessDays;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "prj_bds_" + suffix;
    private final String merchant = "mch_bds_" + suffix;
    private final String barCompany = "ent_bds_bar_" + suffix;
    private final String staffCompany = "ent_bds_staff_" + suffix;
    private final String employee = "usr_bds_e_" + suffix;
    private final String own = "usr_bds_o_" + suffix;
    private final AtomicInteger seq = new AtomicInteger();
    private LocalDate today;
    private RequestPostProcessor clerk;
    private RequestPostProcessor leader;
    private RequestPostProcessor operator;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "派驻服务项目 " + suffix);
        for (String company : List.of(barCompany, staffCompany)) {
            jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) "
                    + "VALUES(?,'派驻服务主体','management_company','active',now(),now())", company);
        }
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", barCompany, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,"
                + "created_at,updated_at) VALUES(?,'派驻服务酒吧','bar',?,?,?,?,'active',now(),now())", merchant, barCompany, barCompany, barCompany, project);
        // 第十八批裁决 46：派驻在期按本酒吧营业日判，「今天」取此刻所属本酒吧营业日（北京日历日零点到起点前仍属前一营业日）。
        today = businessDays.merchantBusinessDate(null, merchant, Instant.now());
        // 公司自有员工：档案登记项目＝所属公司、无商户；账号项目＝所属公司、无商户号，本人持点单权限。本店员工：档案挂本项目本商户。
        account(employee, "staff", staffCompany, null, role("服务", "'bar.order.create'"));
        profile(employee, staffCompany, null, staffCompany);
        account(own, "staff", project, merchant, role("本店服务", "'bar.order.create'"));
        profile(own, project, merchant, barCompany);
        clerk = account("usr_bds_c_" + suffix, "staff", project, merchant,
                role("开台", "'bar.table_session.create','bar.table_session.view','bar.order.create'"));
        leader = account("usr_bds_l_" + suffix, "staff", project, merchant, role("领班", "'staff.shift.manage'"));
        operator = account("usr_bds_p_" + suffix, "platform_admin", "org_1", null, role("派驻登记", "'staff.profile.review'"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 派驻今天起：领班安排派驻员工顶本店员工此刻在卡座点位的班；店员开台指派该派驻员工为主责服务人成功，服务责任付款主体为其所属主体。 */
    @Test
    void dispatchedStaffServesTheCardSeatOnTheirCoverShift() throws Exception {
        String table = cardSeat();
        dispatch();
        String source = shiftNow(own, project, spaceOf(table));
        cover(source).andExpect(status().isOk()).andExpect(jsonPath("$.data.cover_user_id").value(employee));

        // 修复前红：409 BAR_SERVICE_WORKER_INVALID
        String session = JsonPath.read(open(table).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service_staff_id").value(employee))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT user_id, payer_company_id, merchant_id FROM bar_service_responsibilities WHERE session_id=?", session);
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()).containsEntry("user_id", employee).containsEntry("payer_company_id", staffCompany)
                .containsEntry("merchant_id", merchant);
    }

    /** 派驻结束日早于今天、班次仍在：开台指派该员工为主责服务人照旧 409 BAR_SERVICE_WORKER_INVALID。 */
    @Test
    void endedDispatchCannotServeEvenWithAShiftLeft() throws Exception {
        String table = cardSeat();
        String dispatchId = dispatch();
        jdbc.update("UPDATE staff_dispatch_records SET start_date=?, end_date=?, status='ended' WHERE id=?",
                today.minusDays(3), today.minusDays(1), dispatchId);
        shiftNow(employee, staffCompany, spaceOf(table));

        open(table).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_SERVICE_WORKER_INVALID"));
    }

    /** 第十八批裁决 46「结束日当天的夜班过零点后、到营业日结束前仍算派驻期内」：结束日＝此刻所属营业日（北京日历日已是次日），派驻员工顶班后照常被开台指派为主责服务人。 */
    @Test
    void endDateNightAfterMidnightStillServes() throws Exception {
        crossMidnight();
        String table = cardSeat();
        end(dispatch(), today);
        String source = shiftNow(own, project, spaceOf(table));
        cover(source).andExpect(status().isOk());

        // 修复前红：按北京日历日判，409 BAR_SERVICE_WORKER_INVALID
        open(table).andExpect(status().isOk()).andExpect(jsonPath("$.data.service_staff_id").value(employee));
    }

    /** 同裁决：结束日营业日结束后（结束日早于此刻所属营业日），此刻班次仍在也照旧 409 BAR_SERVICE_WORKER_INVALID。 */
    @Test
    void afterTheEndDateBusinessDayCannotServe() throws Exception {
        crossMidnight();
        String table = cardSeat();
        String dispatchId = dispatch();
        jdbc.update("UPDATE staff_dispatch_records SET start_date=?, end_date=?, status='ended' WHERE id=?",
                today.minusDays(1), today.minusDays(1), dispatchId);
        shiftNow(employee, staffCompany, spaceOf(table));

        open(table).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_SERVICE_WORKER_INVALID"));
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    /** 平台侧获授权人员按后台派驻登记请求（蛇形键）把派驻员工登记到本酒吧、今天起，返回派驻记录编号。 */
    private String dispatch() throws Exception {
        String body = "{\"idempotency_key\":\"bds-d-" + seq.incrementAndGet() + "-" + suffix + "\",\"user_id\":\"" + employee
                + "\",\"merchant_id\":\"" + merchant + "\",\"start_date\":\"" + today + "\",\"note\":\"派驻到酒吧\"}";
        return JsonPath.read(postJson(operator, "/api/v1/staff/dispatches", body).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    /**
     * 第十八批裁决 46 跨零点夹具：本综合体已批准营业日版本的所在地时区取 UTC-6（BusinessDayResolver 按综合体版本取酒吧时区），本酒吧起点仍为出厂 10:00，
     * 此刻所属本酒吧营业日恒为北京日历日的前一天，相当于「结束日当天的夜班过零点后、到营业日结束前」；today 随之改取此刻所属营业日。
     */
    private void crossMidnight() {
        Instant effective = Instant.parse("2026-01-01T00:00:00Z");
        jdbc.update("INSERT INTO amusement_business_day_configs(id,project_id,version,zone_id,cutoff_hour,min_cutoff_hour,max_cutoff_hour,"
                + "close_hour,min_close_hour,max_close_hour,effective_at,status,reason,created_by,created_at,decided_by,decided_at)"
                + " VALUES(?,?,'V0.01','Etc/GMT+6',10,0,23,7,0,23,?,'approved','跨零点测试版本','usr_bds',?,'usr_master',?)",
                "abd_bds_" + suffix, project, Timestamp.from(effective), Timestamp.from(effective.minusSeconds(7200)),
                Timestamp.from(effective.minusSeconds(3600)));
        businessDays.invalidate();
        today = businessDays.merchantBusinessDate(null, merchant, Instant.now());
        assertThat(today).isEqualTo(LocalDate.now(LocalZone.ID).minusDays(1));
    }

    private void end(String dispatchId, LocalDate endDate) throws Exception {
        String body = "{\"idempotency_key\":\"bds-end-" + seq.incrementAndGet() + "-" + suffix + "\",\"end_date\":\"" + endDate
                + "\",\"reason\":\"派驻期满\"}";
        postJson(operator, "/api/v1/staff/dispatches/" + dispatchId + "/end", body).andExpect(status().isOk());
    }

    /** 领班安排派驻员工顶整个原班次（蛇形键）。 */
    private ResultActions cover(String shiftId) throws Exception {
        Map<String, Object> shift = jdbc.queryForMap("SELECT start_at, end_at FROM staff_shift_schedules WHERE id=?", shiftId);
        String body = "{\"idempotency_key\":\"bds-c-" + seq.incrementAndGet() + "-" + suffix + "\",\"cover_user_id\":\"" + employee
                + "\",\"reason\":\"派驻员工顶班\",\"start_at\":\"" + ((Timestamp) shift.get("start_at")).toInstant() + "\",\"end_at\":\""
                + ((Timestamp) shift.get("end_at")).toInstant() + "\",\"handover_note\":\"现场交接\"}";
        return postJson(leader, "/api/v1/staff/shifts/" + shiftId + "/cover", body);
    }

    /** 店员开台（蛇形键），指派派驻员工为主责服务人。 */
    private ResultActions open(String table) throws Exception {
        String body = "{\"idempotency_key\":\"bds-o-" + seq.incrementAndGet() + "-" + suffix + "\",\"bar_id\":\"" + merchant + "\",\"table_id\":\""
                + table + "\",\"guest_count\":2,\"service_staff_id\":\"" + employee + "\"}";
        return postJson(clerk, "/api/v1/bar/table-sessions", body);
    }

    /** 已生效班次：覆盖此刻，工作点位为该铺位，工作日为今天。 */
    private String shiftNow(String userId, String rowProject, String space) {
        String id = "shf_bds_" + suffix + "_" + seq.incrementAndGet();
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        jdbc.update("INSERT INTO staff_shift_schedules(id,user_id,work_date,start_at,end_at,point_space_ids,created_by,created_at,updated_at,project_id) "
                + "VALUES(?,?,?,?,?,jsonb_build_array(?::text),'usr_bds',now(),now(),?)", id, userId, today,
                Timestamp.from(now.minus(Duration.ofHours(2))), Timestamp.from(now.plus(Duration.ofHours(4))), space, rowProject);
        return id;
    }

    /** 本酒吧卡座，挂本项目本商户铺位，低消 0。 */
    private String cardSeat() {
        String space = "spc_bds_" + suffix + "_" + seq.incrementAndGet();
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) "
                + "VALUES(?,?,?,'卡座区','unit','active',now(),now())", space, project, merchant);
        String id = "tbl_bds_" + suffix + "_" + seq.incrementAndGet();
        jdbc.update("INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,capacity,allowed_pass_free_guest_count,"
                + "min_consumption_cent,currency,status,created_at,updated_at,space_id) VALUES(?,?,?,?,'台位','card_seat',6,0,0,'CNY','active',now(),now(),?)",
                id, merchant, merchant, "K" + id, space);
        return id;
    }

    private String spaceOf(String table) {
        return jdbc.queryForObject("SELECT space_id FROM bar_tables WHERE id=?", String.class, table);
    }

    private ResultActions postJson(RequestPostProcessor session, String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(session).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void profile(String userId, String projectId, String merchantId, String companyId) {
        jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,company_id,registration_status,status,employment_status,"
                + "is_sales_position,created_at,updated_at) VALUES(?,?,?,?,'派驻服务员工',?,'approved','active','employed',false,now(),now())",
                "pro_" + userId, projectId, merchantId, userId, companyId);
    }

    private String role(String name, String permissions) {
        String role = "role_bds_" + seq.incrementAndGet() + "_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'派驻服务回归','project','active',now())",
                role, "派驻服务" + name + System.nanoTime());
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
