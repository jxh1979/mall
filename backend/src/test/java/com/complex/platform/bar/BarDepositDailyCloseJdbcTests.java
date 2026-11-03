package com.complex.platform.bar;

// 文件版本：V0.01

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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
 * 第十八批 O 段第 126 条 126-R1（BSM:990「允许现场现金和 POS 机支付，但必须由授权员工记录到订单、账单、日结和对账里。」；
 * BSM:1003「连接支付和财务，一体机产生的订单必须进入日结、对账和经营看板。」）：订台定金进日结订台定金源（bar_deposit）——
 * 当日到账的定金（各渠道）计金额与笔数，当日实际退回成功的定金计退款；员工代订免收不计；其他商户的定金不进本商户日结；定金不计入酒吧源。
 * 一体机订台、授权员工登记定金、平台管理员锁日结与查汇总都按真实请求形状（蛇形键）走真实入口与真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarDepositDailyCloseJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private BusinessDayResolver days;

    private CustomerTicketPaymentFixture f;
    private String s;
    private String bar;
    private String device;
    private RequestPostProcessor staff;
    private RequestPostProcessor admin;

    @BeforeEach
    void seed() throws Exception {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        s = f.suffix;
        bar = "bar_bdc_" + s;
        device = "bdc-kiosk-" + s;
        String space = "space_bdc_" + s;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '定金日结酒吧', 'bar', 'active', now(), now(), ?)", bar, f.projectId);
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at)"
                + " VALUES (?, ?, '酒吧前台一体机', 'shop', 'active', now(), now())", space, f.projectId);
        // 定金规则：本酒吧固定 200 元。
        jdbc.update("INSERT INTO bar_deposit_rules (id, bar_id, scope_type, scope_id, mode, fixed_amount_cent, cancel_free_before_minutes,"
                + " enabled, created_at, updated_at) VALUES (?, ?, 'bar', ?, 'fixed', 20000, 0, TRUE, now(), now())",
                "bar_dep_bdc_" + s, bar, bar);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        admin = staffs.staff("usr_bdc_admin_" + s, "platform_admin", f.projectId, null,
                "terminal.device.bind", "finance.daily_close.lock", "finance.daily_close.manage");
        staff = staffs.staff("usr_bdc_staff_" + s, "staff", f.projectId, bar,
                "bar.reservation.create", "bar.reservation.manage", "bar.reservation.view");
        mockMvc.perform(post("/api/v1/terminals/devices/bind").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bdc-bind-" + s, "device_id", device,
                                "terminal_type", "kiosk", "binding_scope", "space", "bound_space_id", space, "kiosk_mode", "service"))))
                .andExpect(status().isOk());
    }

    /** 一体机订台、授权员工登记现金定金：当日日结订台定金源计 200 元 1 笔，酒吧源不计。 */
    @Test
    void kioskCashDepositEntersBarDepositSourceNotBarSource() throws Exception {
        String booking = kioskPaidDeposit("cash", table("a"), phone(1));
        String close = close(businessDayOf(booking));
        summaries(close).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.source_stream == 'bar_deposit')].amount_cent").value(20000))
                .andExpect(jsonPath("$.data.items[?(@.source_stream == 'bar_deposit')].order_count").value(1))
                .andExpect(jsonPath("$.data.items[?(@.source_stream == 'bar_deposit')].refund_cent").value(0))
                .andExpect(jsonPath("$.data.items[?(@.source_stream == 'bar')].amount_cent").value(0));
    }

    /**
     * 各渠道当日到账都计（一体机现金＋线上微信）；退款只计当日实际退回成功的（已退还、达标已原路退回），没收不计；
     * 员工代订免收不计；其他商户当日到账的定金不进本商户日结。
     */
    @Test
    void depositSourceCountsEveryChannelPaidThatDayAndRefundsSettledThatDay() throws Exception {
        String booking = kioskPaidDeposit("mix", table("b"), phone(2));
        Instant paidAt = jdbc.queryForObject("SELECT deposit_paid_at FROM bar_reservations WHERE id = ?", Timestamp.class, booking)
                .toInstant();
        Instant earlier = paidAt.minus(Duration.ofDays(2));
        deposit("online", bar, table("c"), 20_000L, "paid", "wechat", null, paidAt, null);
        deposit("refunded", bar, table("d"), 15_000L, "settled", "cash", "refunded", earlier, paidAt);
        deposit("threshold", bar, table("e"), 5_000L, "settled", "pos", "refunded_on_threshold", earlier, paidAt);
        deposit("forfeited", bar, table("f"), 7_000L, "settled", "cash", "forfeited", earlier, paidAt);
        // 员工代订：免收定金，没有到账时点。
        reserve(staff, "waived", table("g"), phone(3)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deposit_status").value("staff_waived"));
        String other = "bar_bdc_other_" + s;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '定金日结另一酒吧', 'bar', 'active', now(), now(), ?)", other, f.projectId);
        deposit("other", other, table(other, "h"), 50_000L, "paid", "cash", null, paidAt, null);

        String close = close(businessDayOf(booking));
        summaries(close).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.source_stream == 'bar_deposit')].amount_cent").value(40000))
                .andExpect(jsonPath("$.data.items[?(@.source_stream == 'bar_deposit')].order_count").value(2))
                .andExpect(jsonPath("$.data.items[?(@.source_stream == 'bar_deposit')].refund_cent").value(20000));
    }

    // ---------- helpers ----------

    /** 一体机订台后，授权员工在一体机上登记定金（真实请求形状）。 */
    private String kioskPaidDeposit(String tag, String tableId, String customerPhone) throws Exception {
        String booking = CustomerTicketPaymentFixture.read(reserve(kiosk(staff), tag, tableId, customerPhone)
                .andExpect(status().isOk()), "$.data.id");
        mockMvc.perform(post("/api/v1/bar/reservations/" + booking + "/pay-deposit").with(kiosk(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bdc-pay-" + tag + "-" + s, "channel", "cash"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deposit_status").value("paid"));
        return booking;
    }

    /** 该笔定金到账时点所属的酒吧营业日。 */
    private LocalDate businessDayOf(String reservationId) {
        Timestamp paidAt = jdbc.queryForObject("SELECT deposit_paid_at FROM bar_reservations WHERE id = ?", Timestamp.class, reservationId);
        return days.businessDate(null, bar, paidAt.toInstant());
    }

    /** 平台管理员按后台真实请求锁本酒吧商户该营业日日结，返回日结 ID。 */
    private String close(LocalDate businessDate) throws Exception {
        return CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/finance/daily-closes").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bdc-close-" + s, "operator_note", "日结",
                                "business_date", businessDate.toString(), "scope_type", "merchant", "scope_id", bar,
                                "force_close", false))))
                .andExpect(status().isOk()), "$.data.id");
    }

    private ResultActions summaries(String dailyCloseId) throws Exception {
        return mockMvc.perform(get("/api/v1/finance/daily-closes/" + dailyCloseId + "/summaries").with(admin));
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

    /** 订台屏的真实请求形状。 */
    private ResultActions reserve(RequestPostProcessor who, String tag, String tableId, String customerPhone) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bdc-" + tag + "-" + s);
        body.put("bar_id", bar);
        body.put("table_id", tableId);
        body.put("reservation_time", Instant.now().plusSeconds(3600).toString());
        body.put("latest_arrival_at", Instant.now().plusSeconds(7200).toString());
        body.put("guest_count", 2);
        body.put("customer_phone", customerPhone);
        return mockMvc.perform(post("/api/v1/bar/reservations").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 前置态：一笔已处于给定定金状态的订台（线上到账、已退回、已没收、他店）。 */
    private void deposit(String tag, String barId, String tableId, long amountCent, String depositStatus, String channel,
            String settlement, Instant paidAt, Instant settledAt) {
        jdbc.update("INSERT INTO bar_reservations (id, bar_id, merchant_id, table_id, reserved_at, guest_count, deposit_amount_cent,"
                + " deposit_status, deposit_channel, deposit_settlement, deposit_paid_at, deposit_settled_at, order_source, status,"
                + " created_at, updated_at) VALUES (?,?,?,?,?,2,?,?,?,?,?,?,'customer','reserved',now(),now())",
                "res_bdc_" + tag + "_" + s, barId, barId, tableId, Timestamp.from(paidAt), amountCent, depositStatus, channel,
                settlement, Timestamp.from(paidAt), settledAt == null ? null : Timestamp.from(settledAt));
    }

    private String table(String tag) {
        return table(bar, tag);
    }

    private String table(String barId, String tag) {
        String id = "table_bdc_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, '卡座', 'card_seat', 6, 2, 50000, 'CNY', 'active', now(), now())",
                id, barId, barId, "D-" + tag.toUpperCase() + "-" + s);
        return id;
    }

    private String phone(int n) {
        return "136" + String.format("%08d", Math.floorMod(s.hashCode() + n, 100_000_000));
    }
}
