package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
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
 * 第十八批 I 段第 86 条酒吧销售与服务归属真库、真接口回归（请求体按前端蛇形键）。依据：BSM 第 1021 行「由有销售归属的订台开台的，自动记营销带客并带出该销售人员；
 * 以上都不命中的直接到店开台默认记自来客——此时现场可人工补选营销带客并选定销售人员，不补选即维持自来客」；第 1070 行「一张账单的提成只计一次、只归帮该客人
 * 预订的那个人；自来客不产生销售提成；协助服务、加单、送达等其他经手人只留责任痕迹、不产生销售提成」；第 1077 行「主责服务提供人按当日该场次排班指派到该卡座，
 * 同一卡座在不同场次可以更换」；第 1468 行「业绩记名人与销售提成归属人始终是同一人——都是帮该客人预订的那个人」。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarSalesAttributionJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BarRepository bars;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "prj_bsa_" + suffix;
    private final String merchant = "mch_bsa_" + suffix;
    private final String company = "ent_bsa_" + suffix;
    private final String product = "prd_bsa_" + suffix;
    private final AtomicInteger seq = new AtomicInteger();
    private RequestPostProcessor salesA;
    private RequestPostProcessor clerkB;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "销售归属项目 " + suffix);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())",
                company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,"
                + "created_at,updated_at) VALUES(?,'销售归属酒吧','bar',?,?,?,?,'active',now(),now())", merchant, company, company, company, project);
        String staffRole = role("'bar.reservation.create','bar.table_session.create','bar.table_session.view','bar.order.create',"
                + "'bar.table_commission.calculate','bar.payroll.view'");
        salesA = account("a", staffRole);
        clerkB = account("b", staffRole);
        for (String worker : List.of("s", "t", "u")) {
            account(worker, staffRole);
        }
        profile("a", true);
        for (String worker : List.of("b", "s", "t", "u")) {
            profile(worker, false);
        }
        Instant now = Instant.now();
        bars.saveProduct(new BarProductRecord(product, merchant, merchant, "精酿啤酒", "beer", 3800L, "CNY", null, null, false, "active",
                now, now, null, null, null, false, null, null, null));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // ── 86-R2：由订台开台带出订台销售，现场补选只限直接到店 ─────────────────────────────────────────

    /** 员工 A 代订（不填销售员＝A 本人），员工 B 开台时即使填了自己为销售，也按订台带出 A。 */
    @Test
    void bookedSessionCarriesTheReservationSalesNotTheOnSitePick() throws Exception {
        String table = table("card_seat");
        String reservation = reserve(salesA, table);
        open(clerkB, table, reservation, user("b"), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sales_staff_id").value(user("a")));
        assertThat(sessionOf(table, "sales_staff_id")).isEqualTo(user("a"));
    }

    /** 直接到店开台：现场补选营销带客并选定销售人员 B，按补选记 B。 */
    @Test
    void walkInSessionTakesTheOnSitePick() throws Exception {
        String table = table("card_seat");
        open(clerkB, table, null, user("b"), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sales_staff_id").value(user("b")));
        assertThat(sessionOf(table, "sales_staff_id")).isEqualTo(user("b"));
    }

    /** 客人自助订台（订台上没有销售）开台：不按现场填写的销售记，会话销售为空。 */
    @Test
    void selfBookedReservationOpensWithoutSalesEvenIfOneIsFilled() throws Exception {
        String table = table("card_seat");
        String reservation = "rsv_bsa_" + suffix + "_" + seq.incrementAndGet();
        jdbc.update("INSERT INTO bar_reservations(id,bar_id,merchant_id,table_id,reserved_at,guest_count,deposit_status,order_source,sales_staff_id,"
                + "status,created_at,updated_at) VALUES(?,?,?,?,now(),2,'paid','customer',NULL,'reserved',now(),now())",
                reservation, merchant, merchant, table);
        open(clerkB, table, reservation, user("b"), null).andExpect(status().isOk());
        assertThat(sessionOf(table, "sales_staff_id")).isNull();
    }

    // ── 86-R1：订单销售取开台会话销售，不再取下单人 ─────────────────────────────────────────────────

    /** A 代订、B 开台并点单：订单、账单与提成都归 A（A 在员工档案标了销售岗→销售档）。 */
    @Test
    void orderBillAndCommissionFollowTheBookingStaffNotTheOrderTaker() throws Exception {
        String table = table("loose_table");
        open(clerkB, table, reserve(salesA, table), null, null).andExpect(status().isOk());
        String order = order(clerkB, table).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sales_staff_id").value(user("a"))).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String orderId = JsonPath.read(order, "$.data.id");
        assertThat(jdbc.queryForObject("SELECT sales_staff_id FROM bar_orders WHERE id=?", String.class, orderId)).isEqualTo(user("a"));
        String bill = JsonPath.read(bill(clerkB, orderId).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sales_staff_id").value(user("a")))
                .andExpect(jsonPath("$.data.sales_role_type").value("sales"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        commission(clerkB, bill).andExpect(status().isOk());
        assertThat(jdbc.queryForMap("SELECT sales_staff_id, sales_role_type FROM bar_commission_calculations WHERE bill_id=?", bill))
                .containsEntry("sales_staff_id", user("a")).containsEntry("sales_role_type", "sales");
    }

    // ── 86-R4：无销售归属的账单不计销售提成 ─────────────────────────────────────────────────────────

    /** 直接到店不补选（自来客）开台后点单：订单与账单都没有销售，计提 409、不落提成行。 */
    @Test
    void walkInWithoutPickHasNoOrderSalesAndNoCommission() throws Exception {
        String table = table("loose_table");
        open(clerkB, table, null, null, null).andExpect(status().isOk());
        String orderId = JsonPath.read(order(clerkB, table).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        assertThat(jdbc.queryForObject("SELECT sales_staff_id FROM bar_orders WHERE id=?", String.class, orderId)).isNull();
        String bill = JsonPath.read(bill(clerkB, orderId).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        commission(clerkB, bill).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_COMMISSION_NO_SALES"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_commission_calculations WHERE bill_id=?", Integer.class, bill)).isZero();
    }

    // ── 86-R3：卡座开台主责服务人按当日该场次排班指派 ─────────────────────────────────────────────

    /** 卡座点位当前只有 S 一人排班：不填服务员即指派 S，并自开台起开一条 S 的责任区间。 */
    @Test
    void cardSeatAssignsTheOnlyScheduledStaff() throws Exception {
        String table = table("card_seat");
        shift("s", spaceOf(table));
        String body = open(clerkB, table, null, null, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service_staff_id").value(user("s")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String session = JsonPath.read(body, "$.data.id");
        mockMvc.perform(get("/api/v1/bar/table-sessions/" + session + "/service-responsibilities").with(clerkB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].user_id").value(user("s")))
                .andExpect(jsonPath("$.data[0].ended_at").doesNotExist());
    }

    /** 填了不在该卡座点位当前排班里的人：409，台位不动、不建会话。 */
    @Test
    void cardSeatRejectsStaffNotOnTheSchedule() throws Exception {
        String table = table("card_seat");
        shift("s", spaceOf(table));
        open(clerkB, table, null, null, user("t")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SERVICE_STAFF_NOT_SCHEDULED"));
        assertThat(jdbc.queryForObject("SELECT status FROM bar_tables WHERE id=?", String.class, table)).isEqualTo("active");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_table_sessions WHERE table_id=?", Integer.class, table)).isZero();
    }

    /** 同一点位 S、T 都当班：不填即留空、不开区间（不拦开台）；填 T 即记 T。 */
    @Test
    void cardSeatWithSeveralOnDutyNeedsAPick() throws Exception {
        String space = "spc_bsa_shared_" + suffix;
        space(space);
        shift("s", space);
        shift("t", space);
        String first = table("card_seat", space);
        String body = open(clerkB, first, null, null, null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(sessionOf(first, "service_staff_id")).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_service_responsibilities WHERE session_id=?", Integer.class,
                (String) JsonPath.read(body, "$.data.id"))).isZero();
        String second = table("card_seat", space);
        open(clerkB, second, null, null, user("t")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service_staff_id").value(user("t")));
    }

    /** S 的班次此刻由 U 真实顶班（顶班本身发布为 U 的班次）：不填即指派 U。 */
    @Test
    void coveredShiftAssignsTheCoveringStaff() throws Exception {
        String table = table("card_seat");
        String original = shift("s", spaceOf(table));
        String cover = shift("u", spaceOf(table));
        jdbc.update("INSERT INTO staff_shift_covers(id,shift_id,original_user_id,cover_user_id,arranged_by,reason,start_at,end_at,handover_note,"
                + "cover_shift_id,source_updated_at) VALUES(?,?,?,?,?,'病假顶班',?,?,'交接卡座',?,now())", "cov_bsa_" + suffix, original,
                user("s"), user("u"), user("b"), Timestamp.from(Instant.now().minus(Duration.ofHours(1))),
                Timestamp.from(Instant.now().plus(Duration.ofHours(2))), cover);
        open(clerkB, table, null, null, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service_staff_id").value(user("u")));
    }

    /** 散台不按排班指派：照旧取开台填写的服务员。 */
    @Test
    void looseTableKeepsTheRequestedServiceStaff() throws Exception {
        String table = table("loose_table");
        open(clerkB, table, null, null, user("t")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service_staff_id").value(user("t")));
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private String reserve(RequestPostProcessor who, String table) throws Exception {
        int n = seq.incrementAndGet();
        String phone = "137" + String.format("%08d", Math.floorMod((suffix + n).hashCode(), 100_000_000));
        String body = "{\"idempotency_key\":\"bsa-r-" + n + "-" + suffix + "\",\"bar_id\":\"" + merchant + "\",\"table_id\":\"" + table
                + "\",\"reservation_time\":\"" + Instant.now().plus(Duration.ofMinutes(30)) + "\",\"guest_count\":2,\"latest_arrival_at\":\""
                + Instant.now().plus(Duration.ofHours(2)) + "\",\"customer_phone\":\"" + phone + "\"}";
        String result = mockMvc.perform(post("/api/v1/bar/reservations").with(who).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(result, "$.data.id");
    }

    private ResultActions open(RequestPostProcessor who, String table, String reservation, String sales, String service) throws Exception {
        String body = "{\"idempotency_key\":\"bsa-o-" + seq.incrementAndGet() + "-" + suffix + "\",\"bar_id\":\"" + merchant + "\",\"table_id\":\""
                + table + "\"" + (reservation == null ? "" : ",\"reservation_id\":\"" + reservation + "\"") + ",\"guest_count\":2"
                + (sales == null ? "" : ",\"sales_staff_id\":\"" + sales + "\"")
                + (service == null ? "" : ",\"service_staff_id\":\"" + service + "\"") + "}";
        return mockMvc.perform(post("/api/v1/bar/table-sessions").with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions order(RequestPostProcessor who, String table) throws Exception {
        String body = "{\"idempotency_key\":\"bsa-d-" + seq.incrementAndGet() + "-" + suffix + "\",\"bar_id\":\"" + merchant + "\",\"table_id\":\""
                + table + "\",\"items\":[{\"product_id\":\"" + product + "\",\"quantity\":2}]}";
        return mockMvc.perform(post("/api/v1/bar/orders").with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions bill(RequestPostProcessor who, String order) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/bill").with(who).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"bsa-b-" + seq.incrementAndGet() + "-" + suffix + "\"}"));
    }

    private ResultActions commission(RequestPostProcessor who, String bill) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/bills/" + bill + "/commissions/calculate").with(who).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"bsa-c-" + seq.incrementAndGet() + "-" + suffix + "\",\"include_only_table_sales\":true}"));
    }

    private String sessionOf(String table, String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM bar_table_sessions WHERE table_id=?", String.class, table);
    }

    private void space(String id) {
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) "
                + "VALUES(?,?,?,'卡座区','unit','active',now(),now())", id, project, merchant);
    }

    private String table(String type) {
        String space = "spc_bsa_" + suffix + "_" + seq.incrementAndGet();
        space(space);
        return table(type, space);
    }

    /** 台位挂本项目本商户铺位（服务责任与扫码来源按铺位定综合体）；低消 0。 */
    private String table(String type, String space) {
        String id = "tbl_bsa_" + suffix + "_" + seq.incrementAndGet();
        jdbc.update("INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,capacity,allowed_pass_free_guest_count,"
                + "min_consumption_cent,currency,status,created_at,updated_at,space_id) VALUES(?,?,?,?,'台位',?,6,0,0,'CNY','active',now(),now(),?)",
                id, merchant, merchant, "K" + id, type, space);
        return id;
    }

    private String spaceOf(String table) {
        return jdbc.queryForObject("SELECT space_id FROM bar_tables WHERE id=?", String.class, table);
    }

    /** 已发布班次：覆盖此刻，工作点位为该铺位。 */
    private String shift(String tag, String space) {
        String id = "sft_bsa_" + tag + "_" + suffix + "_" + seq.incrementAndGet();
        Instant now = Instant.now();
        jdbc.update("INSERT INTO staff_shift_schedules(id,user_id,work_date,start_at,end_at,point_space_ids,created_by,project_id) "
                + "VALUES(?,?,?,?,?,jsonb_build_array(?::text),?,?)", id, user(tag), LocalDate.now(), Timestamp.from(now.minus(Duration.ofHours(2))),
                Timestamp.from(now.plus(Duration.ofHours(4))), space, user("b"), project);
        return id;
    }

    private String role(String permissions) {
        String role = "role_bsa_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'销售归属回归','project','active',now())", role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN("
                + permissions + ")", role);
        return role;
    }

    private RequestPostProcessor account(String tag, String role) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, project, merchant);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        AccessActor actor = new AccessActor(userId, userId, "staff", project, merchant, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private void profile(String tag, boolean salesPosition) {
        jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,company_id,registration_status,status,employment_status,"
                + "is_sales_position,created_at,updated_at) VALUES(?,?,?,?,'在岗员工',?,'approved','active','employed',?,now(),now())",
                "pro_bsa_" + tag + "_" + suffix, project, merchant, user(tag), company, salesPosition);
    }

    private String user(String tag) {
        return "usr_bsa_" + tag + "_" + suffix;
    }
}
