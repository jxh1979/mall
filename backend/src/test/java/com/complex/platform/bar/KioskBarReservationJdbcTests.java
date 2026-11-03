package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 N 段第 120 条 120-R3（BSM:987「支持订台下单：选择酒吧、日期、时段、卡座/桌台/吧台位、人数和订台备注；订台需要支付定金。」；BSM:1023「订台订单必须记录
 * 定金、订台来源、销售人员或设备来源、到店和取消结果。」；BSM:1058「……客人自助订台支付定金成功、或员工代订提交成功时，该台位即进入已预留。……冲突时先支付定金者得……」；
 * BSM:819「……受限：暂停该客人的自助订台资格与员工为其代订的资格……」）：一体机上订台按一体机来源收定金——定金待付、设备来源、没有销售人员、付定金才占台，订台备注落库；
 * 授权员工登记现金定金即占台，后付者取消并登记原路待退。管理员按后台真实请求绑定一体机，订台请求按一体机真实形状（一体机终端头＋登记设备号、蛇形键）走真实入口与真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class KioskBarReservationJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private BarReservationTimeoutJob timeoutJob;

    private CustomerTicketPaymentFixture f;
    private String s;
    private String bar;
    private String table;
    private String device;
    private RequestPostProcessor staff;
    private String staffId;

    @BeforeEach
    void seed() throws Exception {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        s = f.suffix;
        bar = "bar_kbr_" + s;
        device = "kbr-kiosk-" + s;
        String space = "space_kbr_" + s;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '一体机订台酒吧', 'bar', 'active', now(), now(), ?)", bar, f.projectId);
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at)"
                + " VALUES (?, ?, '酒吧前台一体机', 'shop', 'active', now(), now())", space, f.projectId);
        table = table("a");
        // 定金规则：本酒吧固定 200 元。
        jdbc.update("INSERT INTO bar_deposit_rules (id, bar_id, scope_type, scope_id, mode, fixed_amount_cent, cancel_free_before_minutes,"
                + " enabled, created_at, updated_at) VALUES (?, ?, 'bar', ?, 'fixed', 20000, 0, TRUE, now(), now())",
                "bar_dep_kbr_" + s, bar, bar);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        RequestPostProcessor admin = staffs.staff("usr_kbr_admin_" + s, "platform_admin", f.projectId, null, "terminal.device.bind");
        staffId = "usr_kbr_staff_" + s;
        staff = staffs.staff(staffId, "staff", f.projectId, bar,
                "bar.reservation.create", "bar.reservation.manage", "bar.reservation.view");
        // 管理员按后台真实请求把设备绑定为服务型一体机（按空间绑点位）。
        mockMvc.perform(post("/api/v1/terminals/devices/bind").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "kbr-bind-" + s, "device_id", device,
                                "terminal_type", "kiosk", "binding_scope", "space", "bound_space_id", space, "kiosk_mode", "service"))))
                .andExpect(status().isOk());
    }

    @Test
    void kioskReservationTakesDepositFromDeviceSourceAndHoldsOnlyWhenDepositPaid() throws Exception {
        String first = CustomerTicketPaymentFixture.read(reserve(kiosk(staff), "first", table, phone(1), "靠舞台，生日布置")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.remark").value("靠舞台，生日布置"))
                .andExpect(jsonPath("$.data.deposit_status").value("pending"))
                .andExpect(jsonPath("$.data.deposit_amount_cent").value(20000)), "$.data.id");
        assertThat(row(first)).containsEntry("order_source", "kiosk").containsEntry("created_terminal", "kiosk")
                .containsEntry("device_id", device).containsEntry("sales_staff_id", null)
                .containsEntry("deposit_status", "pending").containsEntry("deposit_amount_cent", 20_000L)
                .containsEntry("hold_status", null).containsEntry("remark", "靠舞台，生日布置");
        // 未付定金不占台：同一台位第二笔一体机订台照样可下。
        String second = CustomerTicketPaymentFixture.read(reserve(kiosk(staff), "second", table, phone(2), null)
                .andExpect(status().isOk()), "$.data.id");

        // 授权员工登记现金定金：先付者占台。
        payDeposit(first, "first").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deposit_status").value("paid"))
                .andExpect(jsonPath("$.data.hold_status").value("held"));
        assertThat(row(first)).containsEntry("deposit_status", "paid").containsEntry("deposit_channel", "cash")
                .containsEntry("hold_status", "held");
        // 后付者：订台取消、不占台，已收定金登记原路待退并留痕。
        payDeposit(second, "second").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("cancelled"));
        assertThat(row(second)).containsEntry("status", "cancelled").containsEntry("deposit_status", "paid")
                .containsEntry("hold_status", null);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'bar.reservation_deposit_refund_on_conflict'"
                + " AND business_id = ?", Integer.class, second)).isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT refund_amount_cent, refund_channel, status FROM finance_refunds"
                + " WHERE source_type = 'bar_deposit_rejection' AND target_id = ?", second))
                .containsEntry("refund_amount_cent", 20_000L).containsEntry("refund_channel", "cash").containsEntry("status", "requested");
    }

    @Test
    void kioskReservationRejectsRestrictedMemberAndBarWithoutDepositRule() throws Exception {
        jdbc.update("UPDATE customers SET status = 'suspended', suspended_until = ? WHERE id = ?",
                Timestamp.from(Instant.now().plusSeconds(86_400)), f.customerId);
        reserve(kiosk(staff), "restricted", table, f.phone, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_RESTRICTED"));
        jdbc.update("DELETE FROM bar_deposit_rules WHERE bar_id = ?", bar);
        reserve(kiosk(staff), "no-rule", table, phone(3), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SELF_RESERVATION_UNAVAILABLE"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_reservations WHERE table_id = ?", Integer.class, table)).isZero();
    }

    @Test
    void staffBookingOffKioskStaysWaivedSoldBySelfAndHeldOnSubmit() throws Exception {
        String booking = CustomerTicketPaymentFixture.read(reserve(staff, "staff", table, phone(4), "员工代订备注")
                .andExpect(status().isOk()), "$.data.id");
        assertThat(row(booking)).containsEntry("order_source", "staff").containsEntry("created_terminal", "web")
                .containsEntry("sales_staff_id", staffId).containsEntry("deposit_status", "staff_waived")
                .containsEntry("hold_status", "held").containsEntry("remark", "员工代订备注");
    }

    @Test
    void paidKioskReservationOverdueForfeitsDepositWithoutNoShow() throws Exception {
        String booking = CustomerTicketPaymentFixture.read(reserve(kiosk(staff), "late", table, phone(5), null)
                .andExpect(status().isOk()), "$.data.id");
        payDeposit(booking, "late").andExpect(status().isOk());
        jdbc.update("UPDATE bar_reservations SET latest_arrival_at = now() - interval '1 minute' WHERE id = ?", booking);
        timeoutJob.releaseOverdue();
        assertThat(jdbc.queryForMap("SELECT status, deposit_status, deposit_settlement, no_show_at FROM bar_reservations WHERE id = ?", booking))
                .containsEntry("status", "cancelled").containsEntry("deposit_status", "settled")
                .containsEntry("deposit_settlement", "forfeited").containsEntry("no_show_at", null);
    }

    // ---------- helpers ----------

    /** 一体机真实形状：一体机终端头＋登记设备号（不签名，默认 grace）。 */
    private RequestPostProcessor kiosk(RequestPostProcessor who) {
        return request -> {
            MockHttpServletRequest r = who.postProcessRequest(request);
            r.removeHeader("X-Terminal-Type");
            r.addHeader("X-Terminal-Type", "kiosk");
            r.addHeader("X-Device-Id", device);
            return r;
        };
    }

    /** 一体机订台屏的真实请求形状。 */
    private ResultActions reserve(RequestPostProcessor who, String tag, String tableId, String customerPhone, String remark)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "kbr-" + tag + "-" + s);
        body.put("bar_id", bar);
        body.put("table_id", tableId);
        body.put("reservation_time", Instant.now().plusSeconds(3600).toString());
        body.put("latest_arrival_at", Instant.now().plusSeconds(7200).toString());
        body.put("guest_count", 2);
        body.put("customer_phone", customerPhone);
        if (remark != null) {
            body.put("remark", remark);
        }
        return mockMvc.perform(post("/api/v1/bar/reservations").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 一体机收款步骤：授权员工登记现金定金。 */
    private ResultActions payDeposit(String reservationId, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/reservations/" + reservationId + "/pay-deposit").with(kiosk(staff))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "kbr-pay-" + tag + "-" + s, "channel", "cash"))));
    }

    private String table(String tag) {
        String id = "table_kbr_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, '卡座', 'card_seat', 6, 2, 50000, 'CNY', 'active', now(), now())",
                id, bar, bar, "K-" + tag.toUpperCase() + "-" + s);
        return id;
    }

    private String phone(int n) {
        return "135" + String.format("%08d", Math.floorMod(s.hashCode() + n, 100_000_000));
    }

    private Map<String, Object> row(String reservationId) {
        return jdbc.queryForMap("SELECT order_source, created_terminal, device_id, sales_staff_id, status, deposit_status, deposit_amount_cent,"
                + " deposit_channel, hold_status, remark FROM bar_reservations WHERE id = ?", reservationId);
    }
}
