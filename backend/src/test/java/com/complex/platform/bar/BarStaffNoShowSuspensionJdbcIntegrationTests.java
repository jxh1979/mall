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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 C 段第 15 条（真库）：BSM 第 1468 行「客人超过最晚到店时间未到店的，系统自动释放该台位，并记一笔爽约到经手员工名下……
 * 经手员工的爽约率超过后台配置的上限时（业务参数，按酒吧可配，须同时配置上下限），自动停用其代订权限，由店长复核后恢复」「业绩记名人……
 * 都是帮该客人预订的那个人」；BSM 第 819 行「客人自助订台超时未到店的……不记客人爽约」；BSM 第 1062 行「按爽约发生当日所属的营业日归属账期」；
 * 第十八批裁决 3「按自然月：每个自然月单独统计」。代订按后台新建订台表单的真实请求形状（蛇形键，不填销售员＝本人代订）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarStaffNoShowSuspensionJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private BarReservationTimeoutJob timeoutJob;
    @Autowired private BusinessDayResolver businessDays;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "nsp_prj_" + s;
    private final String bar = "nsp_bar_" + s;
    private final String otherBar = "nsp_bar2_" + s;
    private final String staffA = "nsp_staff_" + s;
    private RequestPostProcessor staff;
    private RequestPostProcessor manager;
    private RequestPostProcessor otherManager;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','爽约综合体','active',now(),now())", project);
        for (String merchant : List.of(bar, otherBar)) {
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'爽约酒吧','bar','active',?,now(),now())",
                    merchant, project);
        }
        staff = actor(staffA, "platform_admin", null, List.of("bar.reservation.create", "bar.reservation.manage", "bar.reservation.view"));
        manager = actor("nsp_mgr_" + s, "merchant_staff", bar, List.of("bar.staff_booking.restore"));
        otherManager = actor("nsp_mgr2_" + s, "merchant_staff", otherBar, List.of("bar.staff_booking.restore"));
    }

    @Test
    void staffNoShowOverLimitSuspendsBookingUntilStoreManagerRestores() throws Exception {
        limit(50, 0, 100);
        String first = id(reserve("ns1", monthPast()).andExpect(status().isOk()));
        timeoutJob.releaseOverdue();

        // 本自然月 1 笔代订 1 笔爽约（100%）超过上限 50%：自动停用该经手员工的代订权限，整笔不落库。
        reserve("ns2", Instant.now().plusSeconds(7200))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("BAR_STAFF_BOOKING_SUSPENDED"));
        Map<String, Object> row = jdbc.queryForMap("SELECT sales_staff_id, no_show_at FROM bar_reservations WHERE id = ?", first);
        assertThat(row.get("sales_staff_id")).isEqualTo(staffA);
        assertThat(row.get("no_show_at")).isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_reservations WHERE id = ?", Integer.class, "reservation_nsp_ns2_" + s)).isZero();
        String suspension = jdbc.queryForObject("SELECT id FROM bar_staff_booking_suspensions WHERE merchant_id = ? AND staff_user_id = ?"
                + " AND status = 'suspended'", String.class, bar, staffA);

        // 店长在停用列表看到该员工；他酒吧的店长不能恢复。
        mockMvc.perform(get("/api/v1/bar/staff-booking-suspensions").with(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].staff_user_id").value(staffA))
                .andExpect(jsonPath("$.data.items[0].no_show_count").value(1))
                .andExpect(jsonPath("$.data.items[0].booking_count").value(1));
        restore(otherManager, suspension)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        restore(manager, suspension)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("restored"));
        // 恢复的成功留痕记到这条停用记录上（OpenAPI x-audit-event-success: bar.staff_booking_restored）。
        assertThat(jdbc.queryForObject("SELECT business_id FROM audit_events WHERE event_name = 'bar.staff_booking_restored'"
                + " AND event_result = 'success' AND actor_id = ?", String.class, "nsp_mgr_" + s)).isEqualTo(suspension);
        reserve("ns3", Instant.now().plusSeconds(7200)).andExpect(status().isOk());
    }

    @Test
    void previousMonthNoShowsNotCountedInThisMonthRate() throws Exception {
        limit(60, 0, 100);
        Instant thisMonthStart = monthStart();
        // 上一自然月 3 笔爽约（按营业日归月）；本月 1 笔已到店。
        for (int i = 1; i <= 3; i++) {
            seedStaffBooking("prev" + i, thisMonthStart.minus(2, ChronoUnit.DAYS), "cancelled", "released", true);
        }
        seedStaffBooking("arrived", monthPast(), "reserved", "released", false);
        reserve("cur-ns", monthPast()).andExpect(status().isOk());
        timeoutJob.releaseOverdue();

        // 本月 1/2＝50% 不超 60%，不停用；误计上月则 4/5＝80% 会停用。
        reserve("cur-next", Instant.now().plusSeconds(7200)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_staff_booking_suspensions WHERE staff_user_id = ?", Integer.class, staffA)).isZero();
    }

    @Test
    void unconfiguredLimitNeverSuspends() throws Exception {
        reserve("noconf", monthPast()).andExpect(status().isOk());
        timeoutJob.releaseOverdue();
        reserve("noconf-next", Instant.now().plusSeconds(7200)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_staff_booking_suspensions WHERE staff_user_id = ?", Integer.class, staffA)).isZero();
    }

    @Test
    void customerSelfReservationTimeoutNotRecordedAsNoShow() {
        String table = table("self");
        String id = "res_nsp_self_" + s;
        jdbc.update("INSERT INTO bar_reservations (id, bar_id, merchant_id, table_id, customer_id, reserved_at, guest_count, deposit_status,"
                + " order_source, status, created_at, updated_at, latest_arrival_at) VALUES (?,?,?,?,?,now(),2,'pending','miniapp','reserved',now(),now(),?)",
                id, bar, bar, table, "cus_nsp_" + s, Timestamp.from(Instant.now().minusSeconds(60)));
        timeoutJob.releaseOverdue();
        Map<String, Object> row = jdbc.queryForMap("SELECT status, no_show_at FROM bar_reservations WHERE id = ?", id);
        assertThat(row.get("status")).isEqualTo("cancelled");
        assertThat(row.get("no_show_at")).isNull();
    }

    // ---------- helpers ----------

    /** 后台新建订台表单（BarTableSessionsPage）真实请求形状：不填销售员＝本人代订。 */
    private ResultActions reserve(String tag, Instant latestArrival) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "nsp-" + tag + "-" + s);
        body.put("bar_id", bar);
        body.put("table_id", table(tag));
        body.put("guest_count", 2);
        body.put("reservation_time", latestArrival.minusSeconds(600).toString());
        body.put("latest_arrival_at", latestArrival.toString());
        body.put("customer_phone", "13900000008");
        return mockMvc.perform(post("/api/v1/bar/reservations").with(staff)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions restore(RequestPostProcessor who, String suspensionId) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/staff-booking-suspensions/" + suspensionId + "/restore").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"nsp-restore-" + UUID.randomUUID() + "\"}"));
    }

    private String id(ResultActions result) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.id");
    }

    private void limit(int value, int min, int max) {
        jdbc.update("UPDATE merchants SET staff_booking_no_show_limit_pct = ?, staff_booking_no_show_limit_min_pct = ?,"
                + " staff_booking_no_show_limit_max_pct = ? WHERE id = ?", value, min, max, bar);
    }

    private Instant monthStart() {
        return businessDays.start(null, bar, YearMonth.from(businessDays.businessDate(null, bar, Instant.now())).atDay(1));
    }

    /** 本营业月内、已过的最晚到店时间。 */
    private Instant monthPast() {
        Instant t = Instant.now().minusSeconds(60);
        Instant start = monthStart();
        return t.isAfter(start) ? t : start.plusSeconds(1);
    }

    private void seedStaffBooking(String tag, Instant latestArrival, String status, String hold, boolean noShow) {
        jdbc.update("INSERT INTO bar_reservations (id, bar_id, merchant_id, table_id, reserved_at, guest_count, deposit_status, order_source,"
                + " sales_staff_id, status, created_at, updated_at, latest_arrival_at, hold_status, no_show_at)"
                + " VALUES (?,?,?,?,?,2,'staff_waived','staff',?,?,now(),now(),?,?,?)",
                "res_nsp_" + tag + "_" + s, bar, bar, table(tag), Timestamp.from(latestArrival), staffA, status,
                Timestamp.from(latestArrival), hold, noShow ? Timestamp.from(latestArrival) : null);
    }

    private String table(String tag) {
        String id = "table_nsp_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, '卡座', 'card_seat', 6, 2, 50000, 'CNY', 'active', now(), now())",
                id, bar, bar, "NS-" + tag.toUpperCase() + "-" + s);
        return id;
    }

    /** 真实授权链：access_users＋角色→权限码。merchantId 非空＝该酒吧商户的员工（商户范围）。 */
    private RequestPostProcessor actor(String userId, String accountType, String merchantId, List<String> permissions) {
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, ?, NULL, ?, 'active', now())", userId, userId, accountType, merchantId);
        String role = "role_" + userId;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '爽约回归', ?, 'active', now())",
                role, role, merchantId == null ? "platform" : "merchant");
        for (String code : permissions) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, code);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        AccessActor actor = new AccessActor(userId, userId, accountType, null, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of())).postProcessRequest(request);
        };
    }
}
