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
 * 第十八批 O 段第 126 条 126-R2（BSM:990「允许现场现金和 POS 机支付，但必须由授权员工记录到订单、账单、日结和对账里。」；
 * BSM:1029「现场必须允许现金和 POS 机支付，但要进入账单、日结、员工责任链和财务对账。」；
 * BSM:1081「收银员开班记备用金，班结按本班次汇总各支付方式应收（现金/扫码/POS/退款/小费），录入实点现金算长短款差异。」）：
 * 授权员工在一体机上登记的现金/POS 订台定金记登记员工，并按渠道计入该员工本班次的现金/POS 应收。
 * 开班、一体机订台、登记定金、交班都按真实请求形状（蛇形键）走真实入口与真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarDepositShiftCloseJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;

    private CustomerTicketPaymentFixture f;
    private String s;
    private String bar;
    private String device;
    private String cashierId;
    private RequestPostProcessor cashier;
    private RequestPostProcessor otherStaff;

    @BeforeEach
    void seed() throws Exception {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        s = f.suffix;
        bar = "bar_bds_" + s;
        device = "bds-kiosk-" + s;
        String space = "space_bds_" + s;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '定金班结酒吧', 'bar', 'active', now(), now(), ?)", bar, f.projectId);
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at)"
                + " VALUES (?, ?, '酒吧前台一体机', 'shop', 'active', now(), now())", space, f.projectId);
        // 定金规则：本酒吧固定 200 元。
        jdbc.update("INSERT INTO bar_deposit_rules (id, bar_id, scope_type, scope_id, mode, fixed_amount_cent, cancel_free_before_minutes,"
                + " enabled, created_at, updated_at) VALUES (?, ?, 'bar', ?, 'fixed', 20000, 0, TRUE, now(), now())",
                "bar_dep_bds_" + s, bar, bar);
        // 交班过合同准入闸：本酒吧有一份含酒吧业务的生效合同。
        jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,"
                + "lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,"
                + "'effective','[\"bar\"]'::jsonb,now(),now())", "bds_ct_" + s, bar, bar, bar);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        RequestPostProcessor admin = staffs.staff("usr_bds_admin_" + s, "platform_admin", f.projectId, null, "terminal.device.bind");
        cashierId = "usr_bds_cashier_" + s;
        cashier = staffs.staff(cashierId, "staff", f.projectId, bar,
                "bar.reservation.create", "bar.reservation.manage", "bar.reservation.view", "bar.shift.manage", "bar.shift.close");
        otherStaff = staffs.staff("usr_bds_other_" + s, "staff", f.projectId, bar,
                "bar.reservation.create", "bar.reservation.manage", "bar.reservation.view");
        mockMvc.perform(post("/api/v1/terminals/devices/bind").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bds-bind-" + s, "device_id", device,
                                "terminal_type", "kiosk", "binding_scope", "space", "bound_space_id", space, "kiosk_mode", "service"))))
                .andExpect(status().isOk());
    }

    /** 本班次收银员在一体机上登记一笔现金、一笔 POS 定金：交班现金、POS 应收各计 200 元，实点 200 元现金平账不进复核。 */
    @Test
    void cashAndPosDepositsCollectedByCashierCountInHisShiftClose() throws Exception {
        String shift = openShift();
        String cash = kioskReservation("cash", table("a"), phone(1));
        payDeposit(cashier, cash, "cash").andExpect(status().isOk());
        String pos = kioskReservation("pos", table("b"), phone(2));
        payDeposit(cashier, pos, "pos").andExpect(status().isOk());

        closeShift(shift, 20_000).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.system_cash_cent").value(20000))
                .andExpect(jsonPath("$.data.system_pos_cent").value(20000))
                .andExpect(jsonPath("$.data.cash_diff_cent").value(0))
                .andExpect(jsonPath("$.data.needs_review").value(false));
        assertThat(collector(cash)).isEqualTo(cashierId);
        assertThat(collector(pos)).isEqualTo(cashierId);
    }

    /** 开班前登记的、别的员工登记的定金不进本班；抢台失败被取消的那笔现金定金钱已进钱箱，仍计入。 */
    @Test
    void shiftCountsOnlyThisCashiersDepositsWithinShiftIncludingConflictCancelled() throws Exception {
        String before = kioskReservation("before", table("c"), phone(3));
        payDeposit(cashier, before, "cash").andExpect(status().isOk());
        String shift = openShift();
        String sharedTable = table("d");
        String won = kioskReservation("won", sharedTable, phone(4));
        String lost = kioskReservation("lost", sharedTable, phone(5));
        payDeposit(cashier, won, "cash").andExpect(status().isOk());
        payDeposit(cashier, lost, "cash").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("cancelled"));
        String others = kioskReservation("others", table("e"), phone(6));
        payDeposit(otherStaff, others, "cash").andExpect(status().isOk());

        closeShift(shift, 40_000).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.system_cash_cent").value(40000))
                .andExpect(jsonPath("$.data.system_pos_cent").value(0))
                .andExpect(jsonPath("$.data.cash_diff_cent").value(0));
        assertThat(collector(lost)).isEqualTo(cashierId);
        assertThat(collector(others)).isEqualTo("usr_bds_other_" + s);
    }

    // ---------- helpers ----------

    /** 收银员按后台真实请求开班（备用金 0）。 */
    private String openShift() throws Exception {
        return CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/bar/shifts/open").with(cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bds-shift-" + s, "bar_id", bar,
                                "cashier_user_id", cashierId, "opening_cash_cent", 0))))
                .andExpect(status().isOk()), "$.data.id");
    }

    private ResultActions closeShift(String shiftId, long declaredCashCent) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/shifts/" + shiftId + "/close").with(cashier)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "bds-shiftclose-" + s,
                        "declared_cash_cent", declaredCashCent))));
    }

    /** 一体机订台屏的真实请求形状（定金待付、未占台）。 */
    private String kioskReservation(String tag, String tableId, String customerPhone) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bds-" + tag + "-" + s);
        body.put("bar_id", bar);
        body.put("table_id", tableId);
        body.put("reservation_time", Instant.now().plusSeconds(3600).toString());
        body.put("latest_arrival_at", Instant.now().plusSeconds(7200).toString());
        body.put("guest_count", 2);
        body.put("customer_phone", customerPhone);
        return CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/bar/reservations").with(kiosk(cashier))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk()), "$.data.id");
    }

    /** 一体机收款步骤：授权员工登记定金（cash|pos）。 */
    private ResultActions payDeposit(RequestPostProcessor who, String reservationId, String channel) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/reservations/" + reservationId + "/pay-deposit").with(kiosk(who))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "bds-pay-" + reservationId, "channel", channel))));
    }

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

    private String collector(String reservationId) {
        return jdbc.queryForObject("SELECT deposit_collector_staff_id FROM bar_reservations WHERE id = ?", String.class, reservationId);
    }

    private String table(String tag) {
        String id = "table_bds_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, '卡座', 'card_seat', 6, 2, 50000, 'CNY', 'active', now(), now())",
                id, bar, bar, "S-" + tag.toUpperCase() + "-" + s);
        return id;
    }

    private String phone(int n) {
        return "137" + String.format("%08d", Math.floorMod(s.hashCode() + n, 100_000_000));
    }
}
