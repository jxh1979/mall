package com.complex.platform.bar;

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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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
 * 第十八批 AB 段第 191 条 191-R1：派驻期间本人按派驻酒吧取商户范围（真库、真接口、请求体按前端蛇形键）。依据：BSM 第 1143 行「派驻仅由平台侧获授权人员登记，
 * 保留员工、目标商户及项目组织范围」「其他公司的人员派驻到某家酒吧工作期间……该人员在该酒吧促成订单产生的提成」；第 931 行「员工私人手机装的是同一个工作 App……
 * 帮客人订位……在自己手机上就能办」；第 1468 行「帮客人订位：查酒吧空位，替客人预订卡座/散台……业绩记名人与销售提成归属人始终是同一人——都是帮该客人预订的那个人」。
 * 派驻是否在期按本酒吧营业日、只看起止日期（第十八批裁决 46），不看状态。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarDispatchedStaffBookingJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BusinessDayResolver businessDays;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "prj_bdb_" + suffix;
    private final String merchant = "mch_bdb_" + suffix;
    private final String barCompany = "ent_bdb_bar_" + suffix;
    private final String staffCompany = "ent_bdb_staff_" + suffix;
    private final String employee = "usr_bdb_e_" + suffix;
    private final AtomicInteger seq = new AtomicInteger();
    private LocalDate today;
    private RequestPostProcessor dispatched;
    private RequestPostProcessor clerk;
    private RequestPostProcessor operator;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "派驻订位项目 " + suffix);
        for (String company : List.of(barCompany, staffCompany)) {
            jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) "
                    + "VALUES(?,'派驻订位主体','management_company','active',now(),now())", company);
        }
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", barCompany, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,"
                + "created_at,updated_at) VALUES(?,'派驻订位酒吧','bar',?,?,?,?,'active',now(),now())", merchant, barCompany, barCompany, barCompany, project);
        // 第十八批裁决 46：派驻在期按本酒吧营业日判，「今天」取此刻所属本酒吧营业日（北京日历日零点到起点前仍属前一营业日）。
        today = businessDays.merchantBusinessDate(null, merchant, Instant.now());
        // 公司自有员工：档案登记项目＝所属公司、无商户；账号项目＝所属公司、无商户号（StaffRegistrationService 自助注册、StaffService 审批开通同口径）。
        dispatched = account(employee, "staff", staffCompany, null, role("订位", "'bar.reservation.create'"));
        jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,company_id,registration_status,status,employment_status,"
                + "is_sales_position,created_at,updated_at) VALUES(?,?,NULL,?,'派驻营销',?,'approved','active','employed',true,now(),now())",
                "pro_bdb_e_" + suffix, staffCompany, employee, staffCompany);
        clerk = account("usr_bdb_c_" + suffix, "staff", project, merchant,
                role("开台", "'bar.reservation.create','bar.table_session.create','bar.table_session.view','bar.order.create'"));
        operator = account("usr_bdb_p_" + suffix, "platform_admin", "org_1", null, role("派驻登记", "'staff.profile.review'"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 派驻今天起：本人查空位只见派驻酒吧的台位；本人代订成功、记本人为销售、免收定金；酒吧店员带该订台开台记营销带客、销售为本人。 */
    @Test
    void dispatchedCompanyStaffBooksAtTheDispatchedBar() throws Exception {
        String table = table();
        dispatch(today);
        String availability = mockMvc.perform(get("/api/v1/bar/tables/availability").with(dispatched))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> bars = JsonPath.read(availability, "$.data.items[*].bar_id");
        List<String> tables = JsonPath.read(availability, "$.data.items[*].id");
        assertThat(bars).isNotEmpty().containsOnly(merchant);
        assertThat(tables).contains(table);

        String reservation = JsonPath.read(reserve(table).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        assertThat(jdbc.queryForMap("SELECT sales_staff_id, deposit_status, merchant_id FROM bar_reservations WHERE id=?", reservation))
                .containsEntry("sales_staff_id", employee).containsEntry("deposit_status", "staff_waived")
                .containsEntry("merchant_id", merchant);

        String body = "{\"idempotency_key\":\"bdb-o-" + seq.incrementAndGet() + "-" + suffix + "\",\"bar_id\":\"" + merchant
                + "\",\"table_id\":\"" + table + "\",\"reservation_id\":\"" + reservation + "\",\"guest_count\":2}";
        mockMvc.perform(post("/api/v1/bar/table-sessions").with(clerk).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForMap("SELECT sales_staff_id, guest_source FROM bar_table_sessions WHERE table_id=?", table))
                .containsEntry("sales_staff_id", employee).containsEntry("guest_source", "marketing");
    }

    /** 结束派驻、结束日期选明天：状态已为 ended，今天仍在派驻期内，本人代订照样成功。 */
    @Test
    void endedDispatchKeepsTheBarUntilItsEndDate() throws Exception {
        String table = table();
        String dispatchId = dispatch(today);
        String end = "{\"idempotency_key\":\"bdb-end-" + suffix + "\",\"end_date\":\"" + today.plusDays(1) + "\",\"reason\":\"派驻期满\"}";
        mockMvc.perform(post("/api/v1/staff/dispatches/" + dispatchId + "/end").with(operator).contentType(MediaType.APPLICATION_JSON).content(end))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ended"));
        reserve(table).andExpect(status().isOk());
    }

    /** 派驻明天才起：今天本人查空位与代订都 403 MERCHANT_SCOPE_REQUIRED。 */
    @Test
    void dispatchStartingTomorrowGivesNoBarToday() throws Exception {
        String table = table();
        dispatch(today.plusDays(1));
        mockMvc.perform(get("/api/v1/bar/tables/availability").with(dispatched))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("MERCHANT_SCOPE_REQUIRED"));
        reserve(table).andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("MERCHANT_SCOPE_REQUIRED"));
    }

    /** 未派驻的公司员工照旧 403 MERCHANT_SCOPE_REQUIRED。 */
    @Test
    void undispatchedCompanyStaffIsStillRefused() throws Exception {
        String table = table();
        mockMvc.perform(get("/api/v1/bar/tables/availability").with(dispatched))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("MERCHANT_SCOPE_REQUIRED"));
        reserve(table).andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("MERCHANT_SCOPE_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_reservations WHERE table_id=?", Integer.class, table)).isZero();
    }

    /** 第十八批裁决 46「结束日当天的夜班过零点后、到营业日结束前仍算派驻期内」：结束日＝此刻所属营业日（北京日历日已是次日），本人照常查空位、代订；结束日早于此刻所属营业日的 403。 */
    @Test
    void endDateNightAfterMidnightKeepsTheBar() throws Exception {
        crossMidnight();
        String table = table();
        String dispatchId = dispatch(today);
        String end = "{\"idempotency_key\":\"bdb-end-" + suffix + "\",\"end_date\":\"" + today + "\",\"reason\":\"派驻期满\"}";
        mockMvc.perform(post("/api/v1/staff/dispatches/" + dispatchId + "/end").with(operator).contentType(MediaType.APPLICATION_JSON).content(end))
                .andExpect(status().isOk());

        // 修复前红：按北京日历日判，403 MERCHANT_SCOPE_REQUIRED
        mockMvc.perform(get("/api/v1/bar/tables/availability").with(dispatched)).andExpect(status().isOk());
        reserve(table).andExpect(status().isOk());

        jdbc.update("UPDATE staff_dispatch_records SET start_date=?, end_date=? WHERE id=?", today.minusDays(1), today.minusDays(1), dispatchId);
        mockMvc.perform(get("/api/v1/bar/tables/availability").with(dispatched))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("MERCHANT_SCOPE_REQUIRED"));
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    /** 平台侧获授权人员按后台派驻登记请求（蛇形键）登记派驻到本酒吧，返回派驻记录编号。 */
    private String dispatch(LocalDate startDate) throws Exception {
        String body = "{\"idempotency_key\":\"bdb-d-" + seq.incrementAndGet() + "-" + suffix + "\",\"user_id\":\"" + employee
                + "\",\"merchant_id\":\"" + merchant + "\",\"start_date\":\"" + startDate + "\",\"note\":\"派驻到酒吧\"}";
        return JsonPath.read(mockMvc.perform(post("/api/v1/staff/dispatches").with(operator).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    /**
     * 第十八批裁决 46 跨零点夹具：本综合体已批准营业日版本的所在地时区取 UTC-6（BusinessDayResolver 按综合体版本取酒吧时区），本酒吧起点仍为出厂 10:00，
     * 此刻所属本酒吧营业日恒为北京日历日的前一天，相当于「结束日当天的夜班过零点后、到营业日结束前」；today 随之改取此刻所属营业日。
     */
    private void crossMidnight() {
        Instant effective = Instant.parse("2026-01-01T00:00:00Z");
        jdbc.update("INSERT INTO amusement_business_day_configs(id,project_id,version,zone_id,cutoff_hour,min_cutoff_hour,max_cutoff_hour,"
                + "close_hour,min_close_hour,max_close_hour,effective_at,status,reason,created_by,created_at,decided_by,decided_at)"
                + " VALUES(?,?,'V0.01','Etc/GMT+6',10,0,23,7,0,23,?,'approved','跨零点测试版本','usr_bdb',?,'usr_master',?)",
                "abd_bdb_" + suffix, project, Timestamp.from(effective), Timestamp.from(effective.minusSeconds(7200)),
                Timestamp.from(effective.minusSeconds(3600)));
        businessDays.invalidate();
        today = businessDays.merchantBusinessDate(null, merchant, Instant.now());
        assertThat(today).isEqualTo(LocalDate.now(LocalZone.ID).minusDays(1));
    }

    /** 派驻员工本人按员工端订位请求（蛇形键）代订，不填销售员。 */
    private ResultActions reserve(String table) throws Exception {
        int n = seq.incrementAndGet();
        String phone = "137" + String.format("%08d", Math.floorMod((suffix + n).hashCode(), 100_000_000));
        String body = "{\"idempotency_key\":\"bdb-r-" + n + "-" + suffix + "\",\"bar_id\":\"" + merchant + "\",\"table_id\":\"" + table
                + "\",\"customer_phone\":\"" + phone + "\",\"reservation_time\":\"" + Instant.now().plus(Duration.ofMinutes(30))
                + "\",\"latest_arrival_at\":\"" + Instant.now().plus(Duration.ofHours(2)) + "\",\"guest_count\":2}";
        return mockMvc.perform(post("/api/v1/bar/reservations").with(dispatched).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** 本酒吧散台，挂本项目本商户铺位，低消 0。 */
    private String table() {
        String space = "spc_bdb_" + suffix + "_" + seq.incrementAndGet();
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) "
                + "VALUES(?,?,?,'散台区','unit','active',now(),now())", space, project, merchant);
        String id = "tbl_bdb_" + suffix + "_" + seq.incrementAndGet();
        jdbc.update("INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,capacity,allowed_pass_free_guest_count,"
                + "min_consumption_cent,currency,status,created_at,updated_at,space_id) VALUES(?,?,?,?,'台位','loose_table',6,0,0,'CNY','active',now(),now(),?)",
                id, merchant, merchant, "K" + id, space);
        return id;
    }

    private String role(String name, String permissions) {
        String role = "role_bdb_" + seq.incrementAndGet() + "_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'派驻订位回归','project','active',now())",
                role, "派驻订位" + name + System.nanoTime());
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
