package com.complex.platform.bar;

// 文件版本：V0.02

import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
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
 * 第十八批 P 段第 131 条 131-R1（ENUM:103「两种情形均按爽约发生当日所属营业日归属账期。因客人爽约或超时取消而没收的订台定金，
 * 全额归该卡座所属的酒吧商户，作为空置补偿计入该商户营业额与营业额抽成基数」「到店未达最低消费、爽约或超时取消的，按上述规则没收」；
 * BSM:1062「因爽约、超时取消或到店未达最低消费而没收的定金，按爽约发生当日所属的营业日归属账期」；BSM:1682「酒吧仍按每家酒吧配置」）：
 * 三种没收（店员按爽约取消、超时释放、关台未达低消）的定金计入该酒吧商户营业额抽成基数，按没收时点所属的该酒吧营业日归账期；
 * 已退还、达标退回、仍为已付的定金与他店的没收定金不计。一体机订台、授权员工登记定金、店员取消、关台与平台管理员出抽成账单
 * 都按真实请求形状（蛇形键）走真实入口与真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarForfeitedDepositTurnoverRentJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private BusinessDayResolver days;
    @Autowired private FinanceService finance;
    @Autowired private BarReservationTimeoutJob timeoutJob;

    private CustomerTicketPaymentFixture f;
    private String s;
    private String bar;
    private String device;
    private String contract;
    private String rule;
    private RequestPostProcessor staff;
    private RequestPostProcessor admin;

    @BeforeEach
    void seed() throws Exception {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        s = f.suffix;
        bar = bar("main", null);
        device = "ffd-kiosk-" + s;
        contract = "ffd_contract_" + s;
        rule = "ffd_rule_" + s;
        String space = "space_ffd_" + s;
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at)"
                + " VALUES (?, ?, '酒吧前台一体机', 'shop', 'active', now(), now())", space, f.projectId);
        // 定金规则：本酒吧固定 200 元，不设免费取消时限。
        jdbc.update("INSERT INTO bar_deposit_rules (id, bar_id, scope_type, scope_id, mode, fixed_amount_cent, cancel_free_before_minutes,"
                + " enabled, created_at, updated_at) VALUES (?, ?, 'bar', ?, 'fixed', 20000, 0, TRUE, now(), now())",
                "bar_dep_ffd_" + s, bar, bar);
        // 抽成规则：本酒吧 10%、无保底，已复核生效。
        jdbc.update("""
                INSERT INTO turnover_rent_rules
                    (id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent, effective_from, effective_to, status, is_draft)
                VALUES (?, ?, ?, '没收定金抽成回归规则', 1000, 0, DATE '2020-01-01', NULL, 'active', FALSE)
                """, rule, contract, bar);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        admin = staffs.staff("usr_ffd_admin_" + s, "platform_admin", f.projectId, null,
                "terminal.device.bind", "finance.turnover_rent.bill");
        staff = staffs.staff("usr_ffd_staff_" + s, "staff", f.projectId, bar,
                "bar.reservation.create", "bar.reservation.manage", "bar.reservation.view",
                "bar.table_session.create", "bar.table.open_close");
        mockMvc.perform(post("/api/v1/terminals/devices/bind").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "ffd-bind-" + s, "device_id", device,
                                "terminal_type", "kiosk", "binding_scope", "space", "bound_space_id", space, "kiosk_mode", "service"))))
                .andExpect(status().isOk());
    }

    /** 店员按爽约取消已付定金的订台 → 没收；平台管理员出没收营业日所在月的抽成账单：200 元 × 10% = 20 元。 */
    @Test
    void noShowForfeitedDepositIsBilledAsTurnoverRent() throws Exception {
        String booking = kioskPaidDeposit("noshow", table("a"));
        mockMvc.perform(post("/api/v1/bar/reservations/" + booking + "/cancel").with(staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "ffd-cancel-" + s, "no_show", true))))
                .andExpect(status().isOk());
        assertThat(settlement(booking)).isEqualTo("forfeited");

        YearMonth month = forfeitMonth(booking);
        // 第十八批 V 段 159-R4：抽成出账要求账期内营业日的订单与核销、退款与冲正已标记为最终（本用例不测该闸，整段标齐）。
        markRangeFinal(jdbc, "bar", bar, month.atDay(1), month.atEndOfMonth());
        mockMvc.perform(post("/api/v1/finance/turnover-rent/bills").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "ffd-bill-" + s, "rule_id", rule,
                                "period_start", month.atDay(1).toString(), "period_end", month.atEndOfMonth().toString(),
                                "due_date", month.atEndOfMonth().plusDays(10).toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount_cent").value(2000));
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ar_ap_items WHERE source_type = 'turnover_rent' AND source_id = ?",
                Long.class, rule)).isEqualTo(2000L);
    }

    /** 酒吧营业日起点批准为 12:00：北京 2026-08-01 11:00 没收的定金归 7 月账期（出厂 10:00 会归 8 月）。 */
    @Test
    void forfeitureFollowsTheBarsApprovedBusinessDay() {
        String noon = bar("noon", 12);
        String noonRule = "ffd_rule_noon_" + s;
        jdbc.update("""
                INSERT INTO turnover_rent_rules
                    (id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent, effective_from, effective_to, status, is_draft)
                VALUES (?, ?, ?, '没收定金营业日回归规则', 1000, 0, DATE '2020-01-01', NULL, 'active', FALSE)
                """, noonRule, contract + "_noon", noon);
        deposit("noon", noon, 30_000L, "settled", "forfeited", Instant.parse("2026-08-01T03:00:00Z"));

        assertThat(finance.computePeriodTurnoverRent(noon, contract + "_noon", "2026-07")).isEqualTo(3_000L);
        assertThat(finance.computePeriodTurnoverRent(noon, contract + "_noon", "2026-08")).isZero();
    }

    /** 只计本酒吧没收的定金：已退还、达标退回、仍为已付的与他店没收的都不计；回流 1000 元＋没收 200 元 → 120 元。 */
    @Test
    void onlyThisBarsForfeitedDepositsAddToReflowTurnover() {
        Instant mid = Instant.parse("2026-08-15T06:00:00Z");
        deposit("forfeited", bar, 20_000L, "settled", "forfeited", mid);
        deposit("refunded", bar, 15_000L, "settled", "refunded", mid);
        deposit("threshold", bar, 5_000L, "settled", "refunded_on_threshold", mid);
        deposit("paid", bar, 7_000L, "paid", null, null);
        deposit("other", bar("other", null), 50_000L, "settled", "forfeited", mid);
        Timestamp landed = Timestamp.from(mid);
        jdbc.update("""
                INSERT INTO integration_order_syncs (id, provider_code, business_type, sync_type, external_business_id,
                    status, amount_cent, currency, merchant_id, created_at, updated_at)
                VALUES (?, ?, 'catering', 'order', ?, 'synced', 100000, 'CNY', ?, ?, ?)
                """, "ios_ffd_" + s, "pos_ffd_" + s, "ext_ffd_" + s, bar, landed, landed);

        assertThat(finance.computePeriodTurnoverRent(bar, contract, "2026-08")).isEqualTo(12_000L);
    }

    /** 已付定金超过最晚到店时间、超时作业释放 → 没收并计入抽成基数。 */
    @Test
    void timeoutForfeitedDepositCounts() throws Exception {
        String booking = kioskPaidDeposit("timeout", table("b"));
        jdbc.update("UPDATE bar_reservations SET latest_arrival_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minusSeconds(600)), booking);

        timeoutJob.releaseOverdue();

        assertThat(settlement(booking)).isEqualTo("forfeited");
        YearMonth month = forfeitMonth(booking);
        // 第十八批 V 段 159-R4：抽成出账要求账期内营业日的订单与核销、退款与冲正已标记为最终（本用例不测该闸，整段标齐）。
        markRangeFinal(jdbc, "bar", bar, month.atDay(1), month.atEndOfMonth());
        assertThat(finance.computePeriodTurnoverRent(bar, contract, month.toString())).isEqualTo(2_000L);
    }

    /** 订台到店开台、未点单即关台（未达最低消费）→ 定金没收并计入抽成基数。 */
    @Test
    void closingBelowMinimumSpendForfeitedDepositCounts() throws Exception {
        String table = table("c");
        // 关台登记服务收入要求台位关联本综合体铺位。
        jdbc.update("UPDATE bar_tables SET space_id = ? WHERE id = ?", "space_ffd_" + s, table);
        String booking = kioskPaidDeposit("close", table);
        Map<String, Object> open = new LinkedHashMap<>();
        open.put("idempotency_key", "ffd-open-" + s);
        open.put("bar_id", bar);
        open.put("table_id", table);
        open.put("business_date", days.businessDate(null, bar, Instant.now()).toString());
        open.put("guest_count", 2);
        open.put("reservation_id", booking);
        String session = CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/bar/table-sessions").with(staff)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(open)))
                .andExpect(status().isOk()), "$.data.id");
        long version = jdbc.queryForObject("SELECT session_version FROM bar_table_sessions WHERE id = ?", Long.class, session);
        mockMvc.perform(post("/api/v1/bar/table-sessions/" + session + "/service-close").with(staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "ffd-close-" + s, "session_version", version,
                                "reason", "客人离店"))))
                .andExpect(status().isOk());

        assertThat(settlement(booking)).isEqualTo("forfeited");
        YearMonth month = forfeitMonth(booking);
        // 第十八批 V 段 159-R4：抽成出账要求账期内营业日的订单与核销、退款与冲正已标记为最终（本用例不测该闸，整段标齐）。
        markRangeFinal(jdbc, "bar", bar, month.atDay(1), month.atEndOfMonth());
        assertThat(finance.computePeriodTurnoverRent(bar, contract, month.toString())).isEqualTo(2_000L);
    }

    // ---------- helpers ----------

    /** 一体机订台后，授权员工在一体机上登记现金定金（真实请求形状）。 */
    private String kioskPaidDeposit(String tag, String tableId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "ffd-" + tag + "-" + s);
        body.put("bar_id", bar);
        body.put("table_id", tableId);
        body.put("reservation_time", Instant.now().plusSeconds(3600).toString());
        body.put("latest_arrival_at", Instant.now().plusSeconds(7200).toString());
        body.put("guest_count", 2);
        body.put("customer_phone", "136" + String.format("%08d", Math.floorMod((s + tag).hashCode(), 100_000_000)));
        String booking = CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/bar/reservations").with(kiosk(staff))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk()), "$.data.id");
        mockMvc.perform(post("/api/v1/bar/reservations/" + booking + "/pay-deposit").with(kiosk(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "ffd-pay-" + tag + "-" + s, "channel", "cash"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deposit_status").value("paid"));
        return booking;
    }

    private String settlement(String reservationId) {
        return jdbc.queryForObject("SELECT deposit_settlement FROM bar_reservations WHERE id = ?", String.class, reservationId);
    }

    /** 没收时点所属的本酒吧营业日所在月。 */
    private YearMonth forfeitMonth(String reservationId) {
        Timestamp settledAt = jdbc.queryForObject("SELECT deposit_settled_at FROM bar_reservations WHERE id = ?",
                Timestamp.class, reservationId);
        return YearMonth.from(days.businessDate(null, bar, settledAt.toInstant()));
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

    /** 酒吧（业态为酒吧的商户，酒吧 id 即商户 id）；cutoffHour 非空时落一条已批准、已生效的营业日版本。 */
    private String bar(String tag, Integer cutoffHour) {
        String id = "bar_ffd_" + tag + "_" + s;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, ?, 'bar', 'active', now(), now(), ?)", id, "没收定金酒吧" + tag, f.projectId);
        if (cutoffHour != null) {
            Instant effective = Instant.parse("2015-01-01T00:00:00Z");
            jdbc.update("""
                    INSERT INTO bar_business_day_configs(id,bar_id,project_id,version,cutoff_hour,min_cutoff_hour,max_cutoff_hour,
                      close_hour,min_close_hour,max_close_hour,effective_at,status,reason,created_by,created_at,decided_by,decided_at)
                    VALUES(?,?,?,'V0.01',?,0,23,7,0,23,?,'approved','测试已批准版本','ffd_requester',?,'usr_master',?)
                    """, "bbd_ffd_" + tag + "_" + s, id, f.projectId, cutoffHour, Timestamp.from(effective),
                    Timestamp.from(effective.minusSeconds(7200)), Timestamp.from(effective.minusSeconds(3600)));
            days.invalidate();
        }
        return id;
    }

    private String table(String tag) {
        return table(bar, tag);
    }

    private String table(String barId, String tag) {
        String id = "table_ffd_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, '卡座', 'card_seat', 6, 2, 50000, 'CNY', 'active', now(), now())",
                id, barId, barId, "F-" + tag.toUpperCase() + "-" + s);
        return id;
    }

    /** 前置态：一笔已处于给定定金状态的订台（没收、已退还、达标退回、仍已付、他店）。 */
    private void deposit(String tag, String barId, long amountCent, String depositStatus, String settlement, Instant settledAt) {
        Instant paidAt = Instant.parse("2026-07-20T06:00:00Z");
        jdbc.update("INSERT INTO bar_reservations (id, bar_id, merchant_id, table_id, reserved_at, guest_count, deposit_amount_cent,"
                + " deposit_status, deposit_channel, deposit_settlement, deposit_paid_at, deposit_settled_at, order_source, status,"
                + " created_at, updated_at) VALUES (?,?,?,?,?,2,?,?,'cash',?,?,?,'customer','cancelled',now(),now())",
                "res_ffd_" + tag + "_" + s, barId, barId, table(barId, "dep_" + tag), Timestamp.from(paidAt), amountCent,
                depositStatus, settlement, Timestamp.from(paidAt), settledAt == null ? null : Timestamp.from(settledAt));
    }
}
