package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 Q 段第 138 条 138-R1、第 140 条 140-R1（BSM:1347「不论收款走平台还是走商户自有渠道均计入」「减去当期已成功的退款金额」；
 * BSM:1070「商户营业额与营业额抽成基数按打折后的实收金额计」「券抵扣的金额不计入该商户营业额与营业额抽成基数」；
 * BSM:1076「月结定格的扣留部分，扣留金额计入该商户营业额、抽成基数」；BSM:1077「小费……不计入商户营业额与营业额抽成基数」；
 * BSM:1111「挂起订单在结清前不计入……营业额抽成基数……结清后按实际结清金额计入，归属结清所在营业日」；
 * ENUM:257「营业额抽成账单在该账期的薪资月结完成、该账期月结定格时确定的服务费应发数与扣留数定数之后出账，账单一次出准，不使用预估数」；
 * BSM:1682「酒吧仍按每家酒吧配置」）：酒吧商户的平台内酒吧收入（整单已确认商品实收＋月结扣留−已退成商品分项）进营业额抽成基数，
 * 按该酒吧营业日窗取数；服务费未月结时出账 409 BAR_REVENUE_PENDING。后台网页收银开班、下单、出账、结账与平台管理员出抽成账单
 * 按真实请求形状（蛇形键）走真实入口与真实授权链。138-R4：酒吧商户已完成的服务点单（点舞）全额进营业额抽成基数，按下单时点归期，
 * 点单员下单、确认收款、完成、取消走服务点单真实入口。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarPlatformTurnoverRentJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private FinanceService finance;
    @Autowired private BusinessDayResolver days;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "bpt_prj_" + s;
    private final String bar = "bpt_bar_" + s;
    private final String space = "bpt_space_" + s;
    private final String contract = "bpt_contract_" + s;
    private final String rule = "bpt_rule_" + s;

    private String table;
    private String cashierId;
    private RequestPostProcessor cashier;
    private RequestPostProcessor admin;
    private String product;

    @BeforeEach
    void seed() throws Exception {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','抽成酒吧综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'抽成酒吧','bar','active',?,now(),now())",
                bar, project);
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at)"
                + " VALUES (?, ?, '抽成酒吧吧台区', 'shop', 'active', now(), now())", space, project);
        table = table(bar, "counter", "bar_counter");
        String item = "bpt_item_" + s;
        String location = "bpt_loc_" + s;
        String counter = "bpt_counter_" + s;
        jdbc.update("INSERT INTO warehouses (id, project_id, merchant_id, warehouse_type, name) VALUES (?, ?, ?, 'bar_counter', '一号吧台')", counter, project, bar);
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, merchant_id, spec_volume_ml, created_at, updated_at)"
                + " VALUES (?, '某某啤酒 330 毫升瓶装', '酒类', '瓶', 'active', ?, 330, now(), now())", item, bar);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id)"
                + " VALUES (?, ?, ?, 10, 0, 10, now(), ?, ?)", "bpt_bal_" + s, item, location, bar, counter);
        // 抽成规则：本酒吧 10%、无保底，已复核生效。
        jdbc.update("""
                INSERT INTO turnover_rent_rules
                    (id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent, effective_from, effective_to, status, is_draft)
                VALUES (?, ?, ?, '酒吧平台内营业额抽成规则', 1000, 0, DATE '2020-01-01', NULL, 'active', FALSE)
                """, rule, contract, bar);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        admin = staffs.staff("usr_bpt_admin_" + s, "platform_admin", project, null, "finance.turnover_rent.bill");
        // 后台网页收银员：开班、建商品、下单出账、结账。
        cashierId = "usr_bpt_cashier_" + s;
        cashier = staffs.staff(cashierId, "staff", project, bar,
                "bar.shift.manage", "bar.product.manage", "bar.order.create", "finance.payment.register");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bpt-product-" + s);
        body.put("bar_id", bar);
        body.put("product_name", "某某啤酒（整瓶）");
        body.put("category", "beer");
        body.put("price_cent", 5000);
        body.put("status", "active");
        body.put("stock_deduction_required", true);
        body.put("inventory_item_id", item);
        body.put("stock_location_id", location);
        product = data(mockMvc.perform(post("/api/v1/bar/products").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isOk())).path("id").asText();
    }

    /** ① 网页收银开班、吧台位下单 2 瓶（10000）、出账、现金结清 → 平台管理员出该营业日所在月抽成账单：10000 × 10% = 1000。 */
    @Test
    void webCashierSettledBarBillIsBilledAsTurnoverRent() throws Exception {
        mockMvc.perform(post("/api/v1/bar/shifts/open").with(cashier).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bpt-shift-" + s, "bar_id", bar,
                                "cashier_user_id", cashierId, "opening_cash_cent", 0))))
                .andExpect(status().isOk());
        settle(bill("full"), "full", 10_000);

        YearMonth month = barMonth(Instant.now());
        mockMvc.perform(post("/api/v1/finance/turnover-rent/bills").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bpt-bill-" + s, "rule_id", rule,
                                "period_start", month.atDay(1).toString(), "period_end", month.atEndOfMonth().toString(),
                                "due_date", month.atEndOfMonth().plusDays(10).toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount_cent").value(1000));
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ar_ap_items WHERE source_type = 'turnover_rent' AND source_id = ?",
                Long.class, rule)).isEqualTo(1000L);
    }

    /**
     * ② 口径：结清的 10000 计入；用券抵扣 2000、小费 500 的账单只计折后实收减小费 8000；部分付款（4000/10000，待付清）不计；
     * 他店结清的账单不计 → (10000 + 8000) × 10% = 1800。
     */
    @Test
    void onlyThisBarsConfirmedGoodsRevenueCounts() throws Exception {
        settle(bill("full"), "full", 10_000);
        settle(bill("partial"), "partial", 4_000);
        Instant now = Instant.now();
        String tipped = seededBill("tipped", bar, table, 10_000, 2_000, 500, 8_000, 8_500, "settled", now);
        jdbc.update("INSERT INTO bar_payroll_bill_components(id,source_bill_id,session_id,source_table_id,project_id,merchant_id,business_date,"
                + "component_type,amount_cent,original_base_cent,source_event_id,source_snapshot,collection_bill_id)"
                + " VALUES(?,?,?,?,?,?,?,'tip',500,500,?,'[]'::jsonb,?)", "bpt_tip_" + s, tipped, session(bar, table), table, project, bar,
                java.sql.Date.valueOf(barMonth(now).atDay(1)), "bpt_tip_event_" + s, tipped);
        String other = "bpt_bar_other_" + s;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'他店酒吧','bar','active',?,now(),now())",
                other, project);
        seededBill("other", other, table(other, "other", "bar_counter"), 50_000, 0, 0, 50_000, 50_000, "settled", now);

        assertThat(finance.computePeriodTurnoverRent(bar, contract, barMonth(now).toString())).isEqualTo(1_800L);
    }

    /** ③ 卡座服务费尚未月结（月结定格前扣留数未定）：出账 409 BAR_REVENUE_PENDING，不生成应收（ENUM:257）。 */
    @Test
    void serviceFeeAwaitingMonthCloseBlocksTurnoverRentBill() throws Exception {
        String seat = table(bar, "seat", "card_seat");
        String fee = seededBill("fee", bar, seat, 20_000, 0, 0, 22_000, 0, "unsettled", null);
        jdbc.update("INSERT INTO bar_payroll_bill_components(id,source_bill_id,session_id,source_table_id,project_id,merchant_id,business_date,"
                + "component_type,amount_cent,original_base_cent,source_event_id,source_snapshot,collection_bill_id)"
                + " VALUES(?,?,?,?,?,?,DATE '2026-07-15','service_fee',2000,20000,?,'[]'::jsonb,?)", "bpt_fee_" + s, fee, session(bar, seat),
                seat, project, bar, "bpt_fee_event_" + s, fee);

        mockMvc.perform(post("/api/v1/finance/turnover-rent/bills").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bpt-pending-" + s, "rule_id", rule,
                                "period_start", "2026-07-01", "period_end", "2026-07-31", "due_date", "2026-08-10"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_REVENUE_PENDING"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_type = 'turnover_rent' AND source_id = ?",
                Integer.class, rule)).isZero();
    }

    /** ④ 7 月结清 10000 的账单在 8 月退成商品 4000：7 月仍计 10000 → 1000；8 月结清 10000 减退成 4000 → 600。 */
    @Test
    void completedGoodsRefundIsDeductedInTheRefundMonth() {
        Instant july = Instant.parse("2026-07-10T06:00:00Z");
        Instant august = Instant.parse("2026-08-12T06:00:00Z");
        String refunded = seededBill("july", bar, table, 10_000, 0, 0, 6_000, 10_000, "settled", july);
        seededBill("august", bar, table, 10_000, 0, 0, 10_000, 10_000, "settled", Instant.parse("2026-08-10T06:00:00Z"));
        String payable = "bpt_ap_" + s;
        String refund = "bpt_rf_" + s;
        jdbc.update("INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,unpaid_amount_cent,due_date,"
                + "status,created_at,updated_at) VALUES(?,?,'payable',?,'refund',?,4000,4000,'2026-08-12','open',now(),now())",
                payable, payable, bar, refund);
        jdbc.update("INSERT INTO bar_bill_adjustments(id,bill_id,adjustment_type,amount_cent,before_payable_amount_cent,after_payable_amount_cent,reason,"
                + "requested_by_user_id,approval_required,status,idempotency_key,created_at,refunded_at,ar_ap_item_id)"
                + " VALUES(?,?,'refund',4000,10000,6000,'客人退两瓶中的部分',?,TRUE,'applied',?,?,?,?)",
                refund, refunded, cashierId, "bpt-refund-" + s, Timestamp.from(august), Timestamp.from(august), payable);
        jdbc.update("INSERT INTO bar_payroll_refund_lines(id,adjustment_id,collection_bill_id,source_bill_id,component_type,amount_cent,ar_ap_item_id)"
                + " VALUES(?,?,?,?,'goods',4000,?)", "bprf_bpt_" + s, refund, refunded, refunded, payable);
        jdbc.update("INSERT INTO ar_ap_payments(id,ar_ap_item_id,amount_cent,payment_method,payment_time,created_at)"
                + " VALUES(?,?,4000,'bank_transfer',?,now())", "bpt_pay_" + s, payable, Timestamp.from(august));

        assertThat(finance.computePeriodTurnoverRent(bar, contract, "2026-07")).isEqualTo(1_000L);
        assertThat(finance.computePeriodTurnoverRent(bar, contract, "2026-08")).isEqualTo(600L);
    }

    /** ⑤（140-R1）本酒吧营业日起点批准为 12:00：网页收银真实结清后，结清时点为北京 2026-08-01 11:00 的这笔归 7 月账期（出厂 10:00 会归 8 月）。 */
    @Test
    void barRevenueFollowsTheBarsApprovedBusinessDay() throws Exception {
        String billId = bill("noon");
        settle(billId, "noon", 10_000);
        jdbc.update("UPDATE bar_bill_settlements SET settled_at = ? WHERE bill_id = ?",
                Timestamp.from(Instant.parse("2026-08-01T03:00:00Z")), billId);
        Instant effective = Instant.parse("2015-01-01T00:00:00Z");
        jdbc.update("""
                INSERT INTO bar_business_day_configs(id,bar_id,project_id,version,cutoff_hour,min_cutoff_hour,max_cutoff_hour,
                  close_hour,min_close_hour,max_close_hour,effective_at,status,reason,created_by,created_at,decided_by,decided_at)
                VALUES(?,?,?,'V0.01',12,0,23,7,0,23,?,'approved','测试已批准版本','bpt_requester',?,'usr_master',?)
                """, "bbd_bpt_" + s, bar, project, Timestamp.from(effective),
                Timestamp.from(effective.minusSeconds(7200)), Timestamp.from(effective.minusSeconds(3600)));
        days.invalidate();

        assertThat(finance.computePeriodTurnoverRent(bar, contract, "2026-07")).isEqualTo(1_000L);
        assertThat(finance.computePeriodTurnoverRent(bar, contract, "2026-08")).isZero();
    }

    /**
     * ⑥（138-R4，BSM:1122「所有服务费由公司统一收款……全额进公司账——不存在钱不进账的情况；分给服务人员的部分，是入账之后按薪资/提成/成本名录列支的支出」；
     * BSM:1714「点舞等专项服务的收款主体为该服务发生地所属的酒吧商户」；决策台账 §17「客人仍向酒吧全额付款，酒吧确认对客收入」；ENUM:290 服务完成后不可退）：
     * 点单员在公司工作手机上给本酒吧代客下单点舞 30000、确认收款、完成履约（管理费 10%）→ 全额 30000 计入；已收款后取消的、只收款未完成的、
     * 他店已完成的单不计 → 30000 × 10% = 3000。
     */
    @Test
    void completedServiceOrderCountsInFullForTheBar() throws Exception {
        String performer = "usr_bpt_dancer_" + s;
        jdbc.update("INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, registration_status, status, created_at, updated_at)"
                + " VALUES (?, ?, NULL, ?, '抽成舞者', 'approved', 'active', now(), now())", "bpt_sprof_" + s, project, performer);
        String dance = "bpt_dance_" + s;
        jdbc.update("INSERT INTO service_catalog_items (id, name, price_cent, duration_minutes, active, mgmt_fee_mode, mgmt_fee_value, updated_at)"
                + " VALUES (?, '点舞', 30000, 15, TRUE, 'percent', 1000, now())", dance);
        // 工作手机按空间绑定本酒吧吧台区（登记设备号即请求头 X-Device-Id）。
        String device = "bpt-phone-" + s;
        jdbc.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, bound_space_id,"
                + " location_point_id, status, bound_at, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'staff_work_phone', 'space', ?, ?, 'bound', now(), now(), now())",
                "td_bpt_" + s, device, device, space, space);
        RequestPostProcessor web = new StaffRequestFixture(jdbc).staff("usr_bpt_waiter_" + s, "staff", project, bar,
                "service.order.create");
        RequestPostProcessor waiter = request -> {
            MockHttpServletRequest r = web.postProcessRequest(request);
            r.removeHeader("X-Terminal-Type");
            r.addHeader("X-Terminal-Type", "staff_work_phone");
            r.addHeader("X-Device-Id", device);
            return r;
        };
        String other = "bpt_bar_svc_other_" + s;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'他店点舞酒吧','bar','active',?,now(),now())",
                other, project);

        String completed = serviceOrder(waiter, "done", dance, performer, bar, table);
        servicePost(waiter, completed, "mark-paid", "done", Map.of());
        servicePost(waiter, completed, "complete", "done", Map.of());
        String cancelled = serviceOrder(waiter, "cancel", dance, performer, bar, table);
        servicePost(waiter, cancelled, "mark-paid", "cancel", Map.of());
        servicePost(waiter, cancelled, "cancel", "cancel", Map.of("reason", "客人临时取消"));
        String paidOnly = serviceOrder(waiter, "paid", dance, performer, bar, table);
        servicePost(waiter, paidOnly, "mark-paid", "paid", Map.of());
        String elsewhere = serviceOrder(waiter, "other", dance, performer, other, null);
        servicePost(waiter, elsewhere, "mark-paid", "other", Map.of());
        servicePost(waiter, elsewhere, "complete", "other", Map.of());

        assertThat(finance.computePeriodTurnoverRent(bar, contract, barMonth(Instant.now()).toString())).isEqualTo(3_000L);
    }

    // ---------- helpers ----------

    /** 网页收银吧台位按商品与数量下单（2 瓶，10000）后生成待支付账单，返回账单 id。 */
    private String bill(String tag) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bpt-order-" + tag + "-" + s);
        body.put("bar_id", bar);
        body.put("table_id", table);
        body.put("items", List.of(Map.of("product_id", product, "quantity", "2")));
        String order = data(mockMvc.perform(post("/api/v1/bar/orders").with(cashier)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isOk())).path("id").asText();
        return data(mockMvc.perform(post("/api/v1/bar/orders/" + order + "/bill").with(cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "bpt-bill-" + tag + "-" + s))))
                .andExpect(status().isOk())).path("id").asText();
    }

    /** 网页收银现金结账（同后台真实请求形状）。 */
    private void settle(String billId, String tag, long amountCent) throws Exception {
        String key = "bpt-settle-" + tag + "-" + s;
        mockMvc.perform(post("/api/v1/bar/bills/" + billId + "/settle").with(cashier).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", key, "payment_id", key,
                                "settlement_amount_cent", amountCent, "payment_channel", "cash"))))
                .andExpect(status().isOk());
    }

    /** 点单员代客下单（同工作手机 ServiceOrderCreateReq 的蛇形键，空字段不发），返回服务单 id。 */
    private String serviceOrder(RequestPostProcessor waiter, String tag, String item, String performer, String barId, String tableId)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bpt-svc-" + tag + "-" + s);
        body.put("catalog_item_id", item);
        body.put("performer_user_id", performer);
        body.put("bar_id", barId);
        if (tableId != null) {
            body.put("table_id", tableId);
        }
        return data(mockMvc.perform(post("/api/v1/services/orders").with(waiter).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isOk())).path("id").asText();
    }

    /** 服务单确认收款（mark-paid）、完成履约（complete）、取消（cancel，带 reason）。 */
    private void servicePost(RequestPostProcessor waiter, String orderId, String action, String tag, Map<String, Object> extra)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>(extra);
        body.put("idempotency_key", "bpt-svc-" + action + "-" + tag + "-" + s);
        mockMvc.perform(post("/api/v1/services/orders/" + orderId + "/" + action).with(waiter).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isOk());
    }

    private YearMonth barMonth(Instant at) {
        return YearMonth.from(days.businessDate(null, bar, at));
    }

    private String table(String barId, String tag, String type) {
        String id = "bpt_table_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at, space_id)"
                + " VALUES (?, ?, ?, ?, '抽成台位', ?, 4, 0, 0, 'CNY', 'active', now(), now(), ?)",
                id, barId, barId, "BPT-" + tag.toUpperCase() + "-" + s, type, space);
        return id;
    }

    /** 前置态：一张账单（结清时带一笔未冲正收款流水）。 */
    private String seededBill(String tag, String barId, String tableId, long subtotal, long discount, long tip, long payable,
            long paid, String settlementStatus, Instant settledAt) {
        String order = "bpt_order_" + tag + "_" + s;
        String id = "bpt_billx_" + tag + "_" + s;
        Timestamp at = Timestamp.from(settledAt == null ? Instant.now() : settledAt);
        jdbc.update("INSERT INTO bar_orders(id,bar_id,merchant_id,table_id,order_source,status,total_amount_cent,currency,created_at,updated_at)"
                + " VALUES(?,?,?,?,'staff','created',?,'CNY',?,?)", order, barId, barId, tableId, subtotal, at, at);
        jdbc.update("INSERT INTO bar_bills(id,bill_no,order_id,bar_id,merchant_id,table_id,subtotal_amount_cent,discount_amount_cent,"
                + "deposit_deduct_amount_cent,payable_amount_cent,paid_amount_cent,currency,payment_status,settlement_status,tip_amount_cent,created_at,updated_at)"
                + " VALUES(?,?,?,?,?,?,?,?,0,?,?,'CNY',?,?,?,?,?)", id, "BPT" + tag + s, order, barId, barId, tableId, subtotal, discount,
                payable, paid, paid > 0 ? "paid" : "unpaid", settlementStatus, tip, at, at);
        if (paid > 0) {
            jdbc.update("INSERT INTO bar_bill_settlements(id,bill_id,settle_channel,settlement_amount_cent,merchant_id,settled_at)"
                    + " VALUES(?,?,'aggregate_pay',?,?,?)", "bpt_pay_" + tag + "_" + s, id, paid, barId, at);
        }
        return id;
    }

    private String session(String barId, String tableId) {
        String id = "bpt_session_" + tableId.hashCode() + "_" + s;
        Timestamp at = Timestamp.from(Instant.now());
        jdbc.update("INSERT INTO bar_table_sessions(id,bar_id,merchant_id,business_date,table_id,service_staff_id,status,opened_at,closed_at,created_at,updated_at)"
                + " VALUES(?,?,?,current_date,?,?,'closed',?,?,?,?)", id, barId, barId, tableId, cashierId, at, at, at, at);
        return id;
    }

    private JsonNode data(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
    }
}
