package com.complex.platform.serviceorder;

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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AB 段第 191 条 191-R3：点单员可指定派驻到本酒吧的服务提供人（真库、真接口、请求体按前端蛇形键）。
 * 依据：BSM 第 1143 行「其他公司的人员派驻到某家酒吧工作期间，由该酒吧负责排班、现场考勤和履约调度」；第 1128 行「点单员帮客人下单，
 * 下单时就指定由哪位服务提供人履约。」派驻是否在期按本酒吧营业日、只看起止日期（第十八批裁决 46）。夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class DispatchedPerformerServiceOrderJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BusinessDayResolver businessDays;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "prj_dpo_" + suffix;
    private final String merchant = "mch_dpo_" + suffix;
    private final String barCompany = "ent_dpo_bar_" + suffix;
    private final String staffCompany = "ent_dpo_staff_" + suffix;
    private final String employee = "usr_dpo_e_" + suffix;
    private final String idle = "usr_dpo_i_" + suffix;
    private final String catalogItem = "svc_dpo_" + suffix;
    private final AtomicInteger seq = new AtomicInteger();
    private LocalDate today;
    private String table;
    private RequestPostProcessor clerk;
    private RequestPostProcessor operator;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "派驻点单项目 " + suffix);
        for (String company : List.of(barCompany, staffCompany)) {
            jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) "
                    + "VALUES(?,'派驻点单主体','management_company','active',now(),now())", company);
        }
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", barCompany, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,"
                + "created_at,updated_at) VALUES(?,'派驻点单酒吧','bar',?,?,?,?,'active',now(),now())", merchant, barCompany, barCompany, barCompany, project);
        // 第十八批裁决 46：派驻在期按本酒吧营业日判，「今天」取此刻所属本酒吧营业日（北京日历日零点到起点前仍属前一营业日）。
        today = businessDays.merchantBusinessDate(null, merchant, Instant.now());
        String space = "spc_dpo_" + suffix;
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) "
                + "VALUES(?,?,?,'卡座区','unit','active',now(),now())", space, project, merchant);
        table = "tbl_dpo_" + suffix;
        jdbc.update("INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,capacity,allowed_pass_free_guest_count,"
                + "min_consumption_cent,currency,status,created_at,updated_at,space_id) VALUES(?,?,?,?,'卡座','card_seat',6,0,0,'CNY','active',now(),now(),?)",
                table, merchant, merchant, "K" + table, space);
        jdbc.update("INSERT INTO service_catalog_items (id, name, price_cent, duration_minutes, active, mgmt_fee_mode, mgmt_fee_value) "
                + "VALUES (?, '派驻点舞', 10000, 10, TRUE, 'none', 0)", catalogItem);
        // 公司自有员工（派驻的与未派驻的）：档案登记项目＝所属公司、无商户；账号项目＝所属公司、无商户号。
        for (String user : List.of(employee, idle)) {
            jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) "
                    + "VALUES(?,?,'staff',?,NULL,'active',now())", user, user, staffCompany);
            jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,company_id,registration_status,status,employment_status,"
                    + "is_sales_position,created_at,updated_at) VALUES(?,?,NULL,?,'派驻服务提供人',?,'approved','active','employed',false,now(),now())",
                    "pro_" + user, staffCompany, user, staffCompany);
        }
        // 点单员在卡座现场用本店工作手机下单（商户账号经网页即商户专区，下单只开放公司终端）。
        String device = "dpo-phone-" + suffix;
        jdbc.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, bound_space_id,"
                + " location_point_id, status, bound_at, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'staff_work_phone', 'space', ?, ?, 'bound', now(), now(), now())",
                "td_dpo_" + suffix, device, device, space, space);
        RequestPostProcessor web = account("usr_dpo_c_" + suffix, "staff", project, merchant, role("点单", "'service.order.create'"));
        clerk = request -> {
            MockHttpServletRequest r = web.postProcessRequest(request);
            r.removeHeader("X-Terminal-Type");
            r.addHeader("X-Terminal-Type", "staff_work_phone");
            r.addHeader("X-Device-Id", device);
            return r;
        };
        operator = account("usr_dpo_p_" + suffix, "platform_admin", "org_1", null, role("派驻登记", "'staff.profile.review'"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 派驻今天起：酒吧点单员下单指定派驻员工为服务提供人成功、落库履约人＝该员工。 */
    @Test
    void barClerkAssignsDispatchedPerformer() throws Exception {
        dispatch(today);

        // 修复前红：403 CROSS_PROJECT_FORBIDDEN
        String order = JsonPath.read(order(employee).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.performer_user_id").value(employee))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        assertThat(jdbc.queryForObject("SELECT performer_user_id FROM service_orders WHERE id=?", String.class, order)).isEqualTo(employee);
    }

    /** 未派驻的其他公司员工照旧 403 CROSS_PROJECT_FORBIDDEN、不建单。 */
    @Test
    void undispatchedOtherCompanyStaffIsStillRefused() throws Exception {
        dispatch(today);
        denied(order(idle));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM service_orders WHERE performer_user_id=?", Integer.class, idle)).isZero();
    }

    /** 派驻结束日早于今天：同样 403 CROSS_PROJECT_FORBIDDEN。 */
    @Test
    void dispatchEndedBeforeTodayIsRefused() throws Exception {
        String dispatchId = dispatch(today.minusDays(3));
        String end = "{\"idempotency_key\":\"dpo-end-" + suffix + "\",\"end_date\":\"" + today.minusDays(1) + "\",\"reason\":\"派驻期满\"}";
        mockMvc.perform(post("/api/v1/staff/dispatches/" + dispatchId + "/end").with(operator).contentType(MediaType.APPLICATION_JSON).content(end))
                .andExpect(status().isOk());
        denied(order(employee));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM service_orders WHERE performer_user_id=?", Integer.class, employee)).isZero();
    }

    /**
     * 第十八批裁决 46「凡判断是否在派驻期内都按酒吧营业日：结束日当天的夜班过零点后、到营业日结束前仍算派驻期内」：结束日＝此刻所属营业日（北京日历日已是次日）
     * 照常下单；结束日早于此刻所属营业日的照旧 403。
     */
    @Test
    void endDateNightAfterMidnightStillOrders() throws Exception {
        crossMidnight();
        String dispatchId = dispatch(today);
        end(dispatchId, today);

        // 修复前红：按北京日历日判，403 CROSS_PROJECT_FORBIDDEN
        order(employee).andExpect(status().isOk());

        jdbc.update("UPDATE staff_dispatch_records SET start_date=?, end_date=? WHERE id=?", today.minusDays(1), today.minusDays(1), dispatchId);
        denied(order(employee));
    }

    /**
     * 同裁决「已在派驻期内下的服务单，之后的收款、完成、取消按下单时所属营业日判」：派驻结束日营业日内下的单，次一营业日照常确认收款、完成
     * （该单落本项目，同根补修后本项目点单员按单据所属项目即放行）。
     */
    @Test
    void orderPlacedWithinTheDispatchIsCollectedNextBusinessDay() throws Exception {
        String dispatchId = dispatch(today.minusDays(3));
        String placed = orderId(employee);
        end(dispatchId, today.minusDays(1));
        // 回填下单时点到结束日营业日内：本营业日起点前一小时。
        Instant before = businessDays.start(null, merchant, today).minusSeconds(3600);
        assertThat(businessDays.merchantBusinessDate(null, merchant, before)).isEqualTo(today.minusDays(1));
        jdbc.update("UPDATE service_orders SET created_at=? WHERE id=?", Timestamp.from(before), placed);

        // 修复前红：按今天判，403 CROSS_PROJECT_FORBIDDEN
        act(placed, "mark-paid").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("paid"));
        act(placed, "complete").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("completed"));
    }

    /** 第十八批 AB 段复核意见（第 191 条；PGM:659「所有业务数据默认按项目隔离」）：派驻履约人的服务单落派驻目标项目，本项目运营「服务点单」列表看得到，人员所属公司账号看不到。 */
    @Test
    void dispatchedPerformerOrderBelongsToTheBarProject() throws Exception {
        dispatch(today);
        String order = orderId(employee);

        // 修复前红：project_id 落人员所属公司
        assertThat(jdbc.queryForObject("SELECT project_id FROM service_orders WHERE id=?", String.class, order)).isEqualTo(project);
        RequestPostProcessor ops = account("usr_dpo_ops_" + suffix, "staff", project, null, role("项目运营", "'service.catalog.manage'"));
        RequestPostProcessor company = account("usr_dpo_co_" + suffix, "staff", staffCompany, null, role("公司运营", "'service.catalog.manage'"));
        assertThat(listed(ops)).contains(order);
        assertThat(listed(company)).doesNotContain(order);
    }

    /**
     * 同根补修（第 191 条复核意见：B 所在项目的运营「列不出这些单，也没法从该页「后台取消」」；PGM:659）：派驻服务单落本项目，
     * 本项目运营（无商户号、持 service.order.create）可后台取消；别的项目运营照旧 403 CROSS_PROJECT_FORBIDDEN。
     */
    @Test
    void barProjectOperatorCancelsDispatchedPerformerOrder() throws Exception {
        dispatch(today);
        String order = orderId(employee);
        String other = "prj_dpo_x_" + suffix;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                other, "派驻点单别的项目 " + suffix);
        RequestPostProcessor ops = account("usr_dpo_oc_" + suffix, "staff", project, null, role("项目运营取消", "'service.order.create'"));
        RequestPostProcessor stranger = account("usr_dpo_ox_" + suffix, "staff", other, null, role("别的项目运营取消", "'service.order.create'"));

        denied(cancel(order, stranger));
        // 修复前红：按履约人档案项目（所属公司）判，403 CROSS_PROJECT_FORBIDDEN
        cancel(order, ops).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("cancelled"));
        assertThat(jdbc.queryForObject("SELECT status FROM service_orders WHERE id=?", String.class, order)).isEqualTo("cancelled");
    }

    /**
     * 同根补修②：未选酒吧的派驻服务单，费率草案生效营业日按派驻目标项目取。本项目所在地时区取 UTC-12、起点 14 点，本项目营业日恒为
     * 人员所属公司（无营业日版本、按出厂口径）营业日的前一天；草案生效日＝所属公司营业日 → 按本项目尚未生效，快照仍为现行版本。
     */
    @Test
    void dispatchedOrderWithoutBarTakesFeeDraftDayFromTheBarProject() throws Exception {
        Instant effective = Instant.parse("2026-01-01T00:00:00Z");
        jdbc.update("INSERT INTO amusement_business_day_configs(id,project_id,version,zone_id,cutoff_hour,min_cutoff_hour,max_cutoff_hour,"
                + "close_hour,min_close_hour,max_close_hour,effective_at,status,reason,created_by,created_at,decided_by,decided_at)"
                + " VALUES(?,?,'V0.01','Etc/GMT+12',14,0,23,7,0,23,?,'approved','本项目营业日早所属公司一天','usr_dpo',?,'usr_master',?)",
                "abd_dpo_" + suffix, project, Timestamp.from(effective), Timestamp.from(effective.minusSeconds(7200)),
                Timestamp.from(effective.minusSeconds(3600)));
        businessDays.invalidate();
        Instant now = Instant.now();
        LocalDate projectDay = businessDays.businessDate(project, null, now);
        LocalDate companyDay = businessDays.businessDate(staffCompany, null, now);
        assertThat(companyDay).isEqualTo(projectDay.plusDays(1));
        dispatch(businessDays.merchantBusinessDate(null, merchant, now));
        jdbc.update("UPDATE service_catalog_items SET next_mgmt_fee_mode='none', next_mgmt_fee_value=0, next_effective_from=?,"
                + " next_requested_by='usr_dpo', next_requested_at=now(), next_approved_by='usr_master' WHERE id=?", companyDay, catalogItem);

        String body = "{\"idempotency_key\":\"dpo-o-" + seq.incrementAndGet() + "-" + suffix + "\",\"catalog_item_id\":\"" + catalogItem
                + "\",\"performer_user_id\":\"" + employee + "\"}";
        String order = JsonPath.read(mockMvc.perform(post("/api/v1/services/orders").with(clerk).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");

        assertThat(jdbc.queryForObject("SELECT project_id FROM service_orders WHERE id=?", String.class, order)).isEqualTo(project);
        // 修复前红：按人员所属公司营业日判，草案已生效、快照为新版本 2
        assertThat(jdbc.queryForObject("SELECT snap_fee_version FROM service_orders WHERE id=?", Integer.class, order)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT next_effective_from FROM service_catalog_items WHERE id=?", LocalDate.class, catalogItem))
                .isEqualTo(companyDay);
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    /** 平台侧获授权人员按后台派驻登记请求（蛇形键）把派驻员工登记到本酒吧，返回派驻记录编号。 */
    private String dispatch(LocalDate startDate) throws Exception {
        String body = "{\"idempotency_key\":\"dpo-d-" + seq.incrementAndGet() + "-" + suffix + "\",\"user_id\":\"" + employee
                + "\",\"merchant_id\":\"" + merchant + "\",\"start_date\":\"" + startDate + "\",\"note\":\"派驻到酒吧\"}";
        return JsonPath.read(mockMvc.perform(post("/api/v1/staff/dispatches").with(operator).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    /** 点单员代客下单（蛇形键）：本酒吧本卡座、指定服务提供人。 */
    private ResultActions order(String performer) throws Exception {
        String body = "{\"idempotency_key\":\"dpo-o-" + seq.incrementAndGet() + "-" + suffix + "\",\"catalog_item_id\":\"" + catalogItem
                + "\",\"performer_user_id\":\"" + performer + "\",\"bar_id\":\"" + merchant + "\",\"table_id\":\"" + table + "\"}";
        return mockMvc.perform(post("/api/v1/services/orders").with(clerk).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static void denied(ResultActions result) throws Exception {
        result.andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
    }

    /**
     * 第十八批裁决 46 跨零点夹具：本综合体已批准营业日版本的所在地时区取 UTC-6（BusinessDayResolver 按综合体版本取酒吧时区），本酒吧起点仍为出厂 10:00，
     * 此刻所属本酒吧营业日恒为北京日历日的前一天，相当于「结束日当天的夜班过零点后、到营业日结束前」；today 随之改取此刻所属营业日。
     */
    private void crossMidnight() {
        Instant effective = Instant.parse("2026-01-01T00:00:00Z");
        jdbc.update("INSERT INTO amusement_business_day_configs(id,project_id,version,zone_id,cutoff_hour,min_cutoff_hour,max_cutoff_hour,"
                + "close_hour,min_close_hour,max_close_hour,effective_at,status,reason,created_by,created_at,decided_by,decided_at)"
                + " VALUES(?,?,'V0.01','Etc/GMT+6',10,0,23,7,0,23,?,'approved','跨零点测试版本','usr_dpo',?,'usr_master',?)",
                "abd_dpo_" + suffix, project, Timestamp.from(effective), Timestamp.from(effective.minusSeconds(7200)),
                Timestamp.from(effective.minusSeconds(3600)));
        businessDays.invalidate();
        today = businessDays.merchantBusinessDate(null, merchant, Instant.now());
        assertThat(today).isEqualTo(LocalDate.now(LocalZone.ID).minusDays(1));
    }

    private void end(String dispatchId, LocalDate endDate) throws Exception {
        String body = "{\"idempotency_key\":\"dpo-end-" + seq.incrementAndGet() + "-" + suffix + "\",\"end_date\":\"" + endDate
                + "\",\"reason\":\"派驻期满\"}";
        mockMvc.perform(post("/api/v1/staff/dispatches/" + dispatchId + "/end").with(operator).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private String orderId(String performer) throws Exception {
        return JsonPath.read(order(performer).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    /** 点单员对服务单确认收款或完成履约（蛇形键）。 */
    private ResultActions act(String orderId, String action) throws Exception {
        String body = "{\"idempotency_key\":\"dpo-a-" + seq.incrementAndGet() + "-" + suffix + "\"}";
        return mockMvc.perform(post("/api/v1/services/orders/" + orderId + "/" + action).with(clerk).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** 后台「服务点单」页取消（蛇形键）。 */
    private ResultActions cancel(String orderId, RequestPostProcessor viewer) throws Exception {
        String body = "{\"idempotency_key\":\"dpo-x-" + seq.incrementAndGet() + "-" + suffix + "\",\"reason\":\"后台取消\"}";
        return mockMvc.perform(post("/api/v1/services/orders/" + orderId + "/cancel").with(viewer).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** 后台「服务点单」页按账号查服务单。 */
    private List<String> listed(RequestPostProcessor viewer) throws Exception {
        return JsonPath.read(mockMvc.perform(get("/api/v1/services/orders").with(viewer)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.items[*].id");
    }

    private String role(String name, String permissions) {
        String role = "role_dpo_" + seq.incrementAndGet() + "_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'派驻点单回归','project','active',now())",
                role, "派驻点单" + name + System.nanoTime());
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
