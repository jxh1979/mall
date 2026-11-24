package com.complex.platform.access;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
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
 * 第十八批 CO 段第 372 条第 4 段 372-S4-R1～R3（PGM:682「外部审计/监管只能查看划定的范围与时间段」；PGM:686「每一次导出——不论脱敏与否——
 * 都记录导出人、导出时间、导出所用设备、导出内容范围与筛选条件」；第十八批裁决 77「各清单、明细、导出只看得到这段时间发生的记录；没有发生日期的资料照常显示」）：
 * 酒吧订单清单按下单时刻、挂起订单风险看板与其导出按挂起时刻、小费与服务费月结按所查月份整月承载外审数据时段；服务收费规则照常显示。
 * 真库＋真实接口＋真实角色授权链，夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AuditDataWindowBarJdbcIntegrationTests {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final String BEFORE = "2025-12-31 23:30:00+08";
    private static final String INSIDE = "2026-06-01 10:00:00+08";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = String.valueOf(System.nanoTime());
    private final String p1 = "adw4_p1_" + s;
    private final String bar = "adw4_bar_" + s;
    private final String table = "adw4_tbl_" + s;
    private final String auditor = "usr_adw4_auditor_" + s;
    private RequestPostProcessor auditorRequest;
    private RequestPostProcessor projectStaff;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','外审时段综合体','active',now(),now())", p1);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,?,'bar','active',?,now(),now())",
                bar, "外审时段酒吧" + s, p1);
        String space = "adw4_spc_" + s;
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at)"
                + " VALUES(?,?,?,'卡座区','unit','active',now(),now())", space, p1, bar);
        jdbc.update("INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,capacity,allowed_pass_free_guest_count,"
                + "min_consumption_cent,currency,status,created_at,updated_at,space_id) VALUES(?,?,?,?,'卡座','card_seat',4,0,0,'CNY','active',now(),now(),?)",
                table, bar, bar, "K" + s, space);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        auditorRequest = staffs.staff(auditor, "outsourced_vendor", p1, null, "bar.order.view", "audit.operation_log.export", "bar.payroll.view");
        projectStaff = staffs.staff("usr_adw4_staff_" + s, "staff", p1, null, "bar.order.view", "audit.operation_log.export", "bar.payroll.view");
        window("2026-01-01", "2026-12-31");
    }

    // ---------- 372-S4-R1 酒吧订单清单 ----------

    @Test
    void barOrdersListOnlyOrdersPlacedInsideWindowWithTotalOnSameBasis() throws Exception {
        order("o1", "2025-12-31 23:30:00+08");
        String in1 = order("o2", "2026-01-01 00:10:00+08");
        String in2 = order("o3", "2026-12-31 23:59:00+08");
        order("o4", "2027-01-01 00:00:00+08");
        String body = body(auditorRequest, "/api/v1/bar/orders");
        assertThat(JsonPath.<List<String>>read(body, "$.data.items[*].id")).containsExactlyInAnyOrder(in1, in2);
        assertThat(((Number) JsonPath.read(body, "$.data.total")).longValue()).isEqualTo(2L);
        String all = body(projectStaff, "/api/v1/bar/orders");
        assertThat(JsonPath.<List<String>>read(all, "$.data.items[*].id")).hasSize(4);
        assertThat(((Number) JsonPath.read(all, "$.data.total")).longValue()).isEqualTo(4L);
    }

    // ---------- 372-S4-R2 挂起订单风险看板与导出 ----------

    @Test
    void suspendedOrderBoardListsTotalAndUnsettledSummaryOnlyInsideWindow() throws Exception {
        suspended("s1", BEFORE);
        String in = suspended("s2", INSIDE);
        String body = body(auditorRequest, "/api/v1/bar/suspended-orders?size=100");
        assertThat(JsonPath.<List<String>>read(body, "$.data.orders[*].order_id")).containsExactly(in);
        assertThat(((Number) JsonPath.read(body, "$.data.total")).longValue()).isEqualTo(1L);
        assertThat(((Number) JsonPath.read(body, "$.data.summary.unsettled_count")).longValue()).isEqualTo(1L);
        String all = body(projectStaff, "/api/v1/bar/suspended-orders?size=100");
        assertThat(JsonPath.<List<String>>read(all, "$.data.orders[*].order_id")).hasSize(2);
        assertThat(((Number) JsonPath.read(all, "$.data.total")).longValue()).isEqualTo(2L);
        assertThat(((Number) JsonPath.read(all, "$.data.summary.unsettled_count")).longValue()).isEqualTo(2L);
    }

    @Test
    void suspendedOrderExportRecordsWindowAndCountsOnlyOrdersInsideIt() throws Exception {
        suspended("e1", BEFORE);
        suspended("e2", INSIDE);
        String device = "WBADV" + s.substring(s.length() - 10);
        jdbc.update("INSERT INTO browser_devices (device_no, first_user_id) VALUES (?, ?)", device, auditor);
        mockMvc.perform(get("/api/v1/bar/suspended-orders/export").with(auditorRequest).cookie(new Cookie("complex_device_no", device)))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForMap("SELECT filters->>'data_window' AS data_window, row_count FROM export_files"
                + " WHERE exporter_id = ? AND export_type = 'bar_suspended_orders'", auditor))
                .containsEntry("data_window", "2026-01-01～2026-12-31").containsEntry("row_count", 1);
        assertThat(jdbc.queryForObject("SELECT metadata->>'row_count' FROM audit_events WHERE event_name = 'bar.order_suspension.exported' AND actor_id = ?",
                String.class, auditor)).isEqualTo("1");
    }

    // ---------- 372-S4-R3 小费与服务费月结 ----------

    @Test
    void payrollRunsReturnOnlyMonthsWhollyInsideWindowWhilePayrollRulesStayVisible() throws Exception {
        String december = run("2025-12");
        String june = run("2026-06");
        String url = "/api/v1/bar/payroll-runs?project_id=" + p1 + "&merchant_id=" + bar + "&month=";
        assertThat(JsonPath.<List<String>>read(body(auditorRequest, url + "2025-12"), "$.data[*].id")).isEmpty();
        assertThat(JsonPath.<List<String>>read(body(auditorRequest, url + "2026-06"), "$.data[*].id")).contains(june);
        assertThat(JsonPath.<List<String>>read(body(projectStaff, url + "2025-12"), "$.data[*].id")).contains(december);

        window("2026-06-15", "2026-12-31");
        assertThat(JsonPath.<List<String>>read(body(auditorRequest, url + "2026-06"), "$.data[*].id")).isEmpty();

        // 没有发生日期的服务收费规则照常显示
        mockMvc.perform(get("/api/v1/bar/payroll-rules?project_id=" + p1 + "&merchant_id=" + bar).with(auditorRequest))
                .andExpect(status().isOk());
    }

    // ---------- helpers ----------

    private void window(String from, String to) throws Exception {
        mockMvc.perform(put("/api/v1/access/users/" + auditor + "/audit-data-window").with(master())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"adw4-" + UUID.randomUUID() + "\",\"data_from\":\"" + from + "\",\"data_to\":\"" + to + "\"}"))
                .andExpect(status().isOk());
    }

    private String body(RequestPostProcessor who, String url) throws Exception {
        return mockMvc.perform(get(url).with(who)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private String order(String tag, String createdAt) {
        String id = "adw4_ord_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_orders(id,bar_id,merchant_id,table_id,order_source,status,total_amount_cent,currency,created_at,updated_at)"
                + " VALUES(?,?,?,?,'staff','submitted',5000,'CNY',CAST(? AS timestamptz),now())", id, bar, bar, table, createdAt);
        return id;
    }

    /** 一单挂起：订单、账单、挂起记录（已挂起待付款 50 元）。 */
    private String suspended(String tag, String suspendedAt) {
        String order = order(tag, suspendedAt);
        String bill = "adw4_bil_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_bills(id,bill_no,order_id,bar_id,merchant_id,table_id,subtotal_amount_cent,discount_amount_cent,deposit_deduct_amount_cent,"
                + "payable_amount_cent,paid_amount_cent,currency,payment_status,settlement_status,tip_amount_cent,created_at,updated_at)"
                + " VALUES(?,?,?,?,?,?,5000,0,0,5000,0,'CNY','unpaid','unsettled',0,CAST(? AS timestamptz),now())",
                bill, "B" + tag + s, order, bar, bar, table, suspendedAt);
        jdbc.update("INSERT INTO bar_order_suspensions(id,order_id,bill_id,bar_id,merchant_id,project_id,table_id,status,amount_cent,reason,"
                + "suspended_by,suspended_at,updated_at) VALUES(?,?,?,?,?,?,?,'hanging_unpaid',5000,'包厢客人暂离','usr_adw4_cashier',CAST(? AS timestamptz),now())",
                "adw4_sus_" + tag + "_" + s, order, bill, bar, bar, p1, table, suspendedAt);
        return order;
    }

    /** 一行已定格月结（金额全 0）。 */
    private String run(String month) {
        String id = "adw4_run_" + month + "_" + s;
        jdbc.update("INSERT INTO bar_payroll_runs(id,project_id,merchant_id,month,source_fingerprint,source_snapshot,collected_tip_cent,"
                + "collected_service_fee_cent,payable_tip_cent,payable_service_fee_cent,withheld_service_fee_cent,rounding_residual_cent,"
                + "settlement_entity_id,status,created_by) VALUES(?,?,?,?,?,'{}'::jsonb,0,0,0,0,0,0,'adw4_entity','confirmed','usr_adw4_creator')",
                id, p1, bar, month, "fp_" + month + "_" + s);
        return id;
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
