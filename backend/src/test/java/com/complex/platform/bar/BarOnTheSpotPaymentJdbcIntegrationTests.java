package com.complex.platform.bar;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 T 段第 157 条 157-R1（真库）：第十八批裁决 37「散台、卡座下单必须当场付款、不能挂起」；PGM:931「正式挂起订单是已下单、已出品、
 * 已扣库存的未付款订单」；BSM:1025「酒水必须先完成付款才能出品上台」；BSM:1064 与 ENUM:117「最低消费是下单的前置条件：客人在该台位下单时，
 * 本次下单后的累计消费金额必须达到该台位适用的最低消费，未达到的不允许提交订单。」（控制席补：对所有台位生效）。
 * 员工请求按后台订单页与平板真实形状（蛇形键、X-Terminal-Type）经控制器与真实授权链提交；客人扫码点单按小程序真实形状匿名提交。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "bar.scan_order.enabled=true"
})
@AutoConfigureMockMvc
@Transactional
class BarOnTheSpotPaymentJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private BarRepository bars;
    @MockitoBean private PaymentGatewayRouter gateway;

    private CustomerTicketPaymentFixture f;
    private String s;
    private String bar;
    private String item;
    private String location;
    private String beer;
    private String snack;
    private RequestPostProcessor staff;

    @BeforeEach
    void seed() throws Exception {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        f.stubGateway(gateway);
        s = f.suffix;
        String project = "prj_spot_" + s;
        String company = "ent_spot_" + s;
        bar = "bar_spot_" + s;
        item = "item_spot_" + s;
        location = "loc_spot_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','当场付款综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())", company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'当场付款酒吧','bar',?,?,?,?,'active',now(),now())", bar, company, company, company, project);
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) VALUES(?,?,?,'散台区','unit','active',now(),now())",
                "space_spot_" + s, project, bar);
        jdbc.update("INSERT INTO warehouses (id, project_id, merchant_id, warehouse_type, name) VALUES (?, ?, ?, 'bar_counter', '一号吧台')",
                "wh_spot_" + s, project, bar);
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, '精酿啤酒', '酒类', '瓶', 'active', ?, now(), now())", item, bar);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id)"
                + " VALUES (?, ?, ?, 10, 0, 10, now(), ?, ?)", "bal_spot_" + s, item, location, bar, "wh_spot_" + s);
        Instant now = Instant.now();
        beer = "product_spot_beer_" + s;
        snack = "product_spot_snack_" + s;
        bars.saveProduct(new BarProductRecord(beer, bar, bar, "精酿啤酒", "beer", 500L, "CNY", item, location, true, "active",
                now, now, null, null, null, false, null, null, null));
        bars.saveProduct(new BarProductRecord(snack, bar, bar, "果盘", "snack", 300L, "CNY", null, null, false, "active",
                now, now, null, null, null, false, null, null, null));
        // 卡座、包厢开账单要有该营业日已批准的收费规则（服务费率取 0，账单金额即消费额）。
        jdbc.update("""
                INSERT INTO bar_payroll_rules(id, rule_kind, project_id, merchant_id, effective_from, version, rule_json, reason, status,
                    platform_company_id, created_by)
                VALUES (?, 'charge', ?, NULL, ?, 'V0.01', ?::jsonb, '服务费 0', 'approved', ?, 'usr_spot')
                """, "bpr_spot_" + s, project, java.sql.Date.valueOf(LocalDate.now().minusDays(1)),
                json.writeValueAsString(new BarPayrollRequests.RuleValues(0, 0, 2000, null, null, null, List.of())), company);
        // 吧台员工：下单、生成账单、出品（bar.order.create）与登记收款（finance.payment.register）。
        staff = new StaffRequestFixture(jdbc).staff("usr_spot_staff_" + s, "staff", project, bar, "bar.order.create", "finance.payment.register");
    }

    /** 卡座未付清不得出品：生成账单未结账即 409，不扣库存、不落出库流水；结清后出品照常扣库存。 */
    @Test
    void cardSeatDeliverRequiresSettledBill() throws Exception {
        String table = table("card", "card_seat", 0L);
        openSession("card", table, 0L);
        String order = orderId(order(table, beer, 1).andExpect(status().isOk()));
        String bill = data(mockMvc.perform(post("/api/v1/bar/orders/" + order + "/bill").with(staff)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("idempotency_key", "spot-bill-" + s))))
                .andExpect(status().isOk()), "$.data.id");

        deliver(order, "first").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_ORDER_PAYMENT_REQUIRED"));
        assertThat(stock()).isEqualTo(10);
        assertThat(movements(order)).isZero();

        settle(bill, 500L).andExpect(status().isOk()).andExpect(jsonPath("$.data.settlement_status").value("settled"));
        deliver(order, "second").andExpect(status().isOk());
        assertThat(stock()).isEqualTo(9);
        assertThat(movements(order)).isEqualTo(1);
    }

    /** 散台同口径：没生成账单直接出品 409；只付了一部分（partial_settled）仍 409。 */
    @Test
    void looseTableDeliverRejectedWithoutBillOrWhenPartiallyPaid() throws Exception {
        String table = table("loose", "loose_table", 0L);
        openSession("loose", table, 0L);
        String order = orderId(order(table, beer, 2).andExpect(status().isOk()));
        deliver(order, "nobill").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_ORDER_PAYMENT_REQUIRED"));
        String bill = data(mockMvc.perform(post("/api/v1/bar/orders/" + order + "/bill").with(staff)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("idempotency_key", "spot-bill-" + s))))
                .andExpect(status().isOk()), "$.data.id");
        settle(bill, 400L).andExpect(status().isOk()).andExpect(jsonPath("$.data.settlement_status").value("partial_settled"));
        deliver(order, "partial").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_ORDER_PAYMENT_REQUIRED"));
        assertThat(stock()).isEqualTo(10);
    }

    /** 包厢（vip）与吧台位不加出品闸：未付也能出品（包厢由服务人员在客人离开前收款；吧台位照现口径）。 */
    @Test
    void vipAndBarCounterDeliverWithoutPayment() throws Exception {
        String vip = table("vip", "vip", 0L);
        openSession("vip", vip, 0L);
        deliver(orderId(order(vip, beer, 1).andExpect(status().isOk())), "vip").andExpect(status().isOk());
        String counter = table("counter", "bar_counter", 0L);
        deliver(orderId(order(counter, beer, 1).andExpect(status().isOk())), "counter").andExpect(status().isOk());
        assertThat(stock()).isEqualTo(8);
    }

    /** 最低消费是下单前置：卡座低消 2000 分，首单 500 不许提交、不落单；首单 2000 可下，之后再加 300 照常。 */
    @Test
    void cardSeatMinSpendCheckedWhenOrdering() throws Exception {
        String table = table("min", "card_seat", 2000L);
        openSession("min", table, 2000L);
        order(table, beer, 1).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_MIN_CONSUMPTION_NOT_MET"));
        assertThat(orders(table)).isZero();
        order(table, beer, 4).andExpect(status().isOk());
        order(table, snack, 1).andExpect(status().isOk());
        assertThat(orders(table)).isEqualTo(2);
    }

    /** 控制席补（ENUM:117、BSM:1064 对任何台位）：包厢设了低消同样在下单时判。 */
    @Test
    void vipMinSpendAlsoCheckedWhenOrdering() throws Exception {
        String table = table("vipmin", "vip", 2000L);
        openSession("vipmin", table, 2000L);
        order(table, beer, 1).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_MIN_CONSUMPTION_NOT_MET"));
        assertThat(orders(table)).isZero();
    }

    /** 客人扫码点单：散台低消 1000 分，本次 500 收款前即拒、不建待付单与支付单；1000 照常发起付款。 */
    @Test
    void customerScanOrderBelowMinSpendRejectedBeforePayment() throws Exception {
        String table = table("scan", "loose_table", 1000L);
        openSession("scan", table, 1000L);
        int intentsBefore = intents();
        scanOrder(table, 1).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_MIN_CONSUMPTION_NOT_MET"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_scan_order_payments WHERE table_id = ?", Integer.class, table)).isZero();
        assertThat(intents()).isEqualTo(intentsBefore);
        scanOrder(table, 2).andExpect(status().isOk()).andExpect(jsonPath("$.data.amount_cent").value(1000));
    }

    /**
     * 第十八批 AA 段第 203 条 203-R1（BSM:1025「扫码点单只适用于散台，卡座不开放扫码点单。」；PGM:744「前端隐藏只是辅助，后端接口必须独立校验。」）：
     * 员工扫码下单接口同样只限散台——卡座、包厢、吧台位开台后扫码下单 409 BAR_SCAN_ORDER_TABLE_NOT_ALLOWED、不落单；散台照常成单、来源 app。
     */
    @Test
    void staffScanOrderOnlyOnLooseTable() throws Exception {
        for (String type : List.of("card_seat", "vip", "bar_counter")) {
            String tag = "ss" + type.charAt(0) + type.length();
            String table = table(tag, type, 0L);
            openSession(tag, table, 0L);
            staffScanOrder("session_spot_" + tag + "_" + s).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail_code").value("BAR_SCAN_ORDER_TABLE_NOT_ALLOWED"));
            assertThat(orders(table)).isZero();
        }
        String loose = table("ssloose", "loose_table", 0L);
        openSession("ssloose", loose, 0L);
        staffScanOrder("session_spot_ssloose_" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order_source").value("app"));
        assertThat(orders(loose)).isEqualTo(1);
    }

    /**
     * 第十八批 AK 段第 222 条 222-R1（PGM:303「同一次操作重复提交时，返回第一次的结果并提示已完成。」；PGM:528「⑤同一笔操作重复提交不重复生效」）：
     * 同一订单已开出未作废账单后，后台「生成账单」成功后再点（换新请求编号）——返回已开出的那张并提示已完成，不再开一张全额账单，
     * 服务费与留款抵用也不再算一遍。
     */
    @Test
    void secondBillRequestWithNewKeyReturnsTheIssuedBill() throws Exception {
        // 本营业日收费规则：服务费率 10%。
        jdbc.update("""
                INSERT INTO bar_payroll_rules(id, rule_kind, project_id, merchant_id, effective_from, version, rule_json, reason, status,
                    platform_company_id, created_by)
                VALUES (?, 'charge', ?, NULL, ?, 'V0.02', ?::jsonb, '服务费 10%', 'approved', ?, 'usr_spot')
                """, "bpr_spot_fee_" + s, "prj_spot_" + s, java.sql.Date.valueOf(LocalDate.now()),
                json.writeValueAsString(new BarPayrollRequests.RuleValues(1000, 0, 2000, null, null, null, List.of())), "ent_spot_" + s);
        String table = table("dup", "card_seat", 0L);
        openSession("dup", table, 0L);
        // 同一开台会话里先前一张账单留有低消留款 200，待后续账单抵用。
        String priorBill = bill(orderId(order(table, snack, 1).andExpect(status().isOk())), "prior").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        bars.saveBillAdjustment(new BarBillAdjustmentRecord("adj_spot_retained_" + s, json.readTree(priorBill).at("/data/id").asText(),
                "min_spend_retained", 200L, 330L, 330L, "退款留款（低消）", "usr_spot_staff_" + s, true, "applied", "spot-retained-" + s,
                List.of(), null, "{}", Instant.now(), null));

        String order = orderId(order(table, beer, 1).andExpect(status().isOk()));
        String first = data(bill(order, "k1").andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").doesNotExist())
                .andExpect(jsonPath("$.data.payable_amount_cent").value(350)), "$.data.id");

        bill(order, "k2").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(first))
                .andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.message").value(com.complex.platform.common.api.ApiResponse.REPLAYED_MESSAGE));
        assertThat(jdbc.queryForList("SELECT id FROM bar_bills WHERE order_id = ? AND settlement_status <> 'void'", String.class, order))
                .containsExactly(first);
        assertThat(adjustments(order, "service_fee")).isEqualTo(1);
        assertThat(adjustments(order, "min_spend_retained_applied")).isEqualTo(1);
    }

    /**
     * 222-R1：客人扫码已付单在成单时就随单记了一张已结清账单；后台再点「生成账单」返回这张已结清账单、不新增未付账单，
     * 散台出品不被 BAR_ORDER_PAYMENT_REQUIRED 拦。
     */
    @Test
    void staffBillRequestOnPaidScanOrderReturnsItsSettledBill() throws Exception {
        String table = table("paid", "loose_table", 0L);
        openSession("paid", table, 0L);
        ResultActions created = scanOrder(table, 1).andExpect(status().isOk());
        f.markPaid(data(created, "$.data.payment_intent_id"));
        String confirmKey = "spot-scan-confirm-" + UUID.randomUUID();
        String order = data(mockMvc.perform(post("/api/v1/customer/bar-tables/" + table + "/scan-orders/"
                        + data(created, "$.data.scan_order_id") + "/confirm")
                .header("X-Terminal-Type", "miniapp").header("Idempotency-Key", confirmKey)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("idempotency_key", confirmKey))))
                .andExpect(status().isOk()), "$.data.bar_order_id");
        String settled = jdbc.queryForObject("SELECT id FROM bar_bills WHERE order_id = ?", String.class, order);

        bill(order, "paid").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(settled))
                .andExpect(jsonPath("$.data.settlement_status").value("settled"))
                .andExpect(jsonPath("$.replayed").value(true));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_bills WHERE order_id = ?", Integer.class, order)).isEqualTo(1);
        deliver(order, "paid").andExpect(status().isOk());
        assertThat(stock()).isEqualTo(9);
    }

    /** 222-R1：已作废账单不算已开——账单经主管「作废并冲正」后再生成账单，开出新账单（不是重复提交，不提示已完成）。 */
    @Test
    void billCanBeIssuedAgainAfterTheFirstIsVoidedAndReversed() throws Exception {
        String table = table("void", "loose_table", 0L);
        openSession("void", table, 0L);
        String order = orderId(order(table, beer, 1).andExpect(status().isOk()));
        String first = data(bill(order, "v1").andExpect(status().isOk()), "$.data.id");
        RequestPostProcessor supervisor = new StaffRequestFixture(jdbc).staff("usr_spot_sup_" + s, "staff", "prj_spot_" + s, bar,
                "bar.bill.adjust", "bar.bill.anomaly.resolve");
        mockMvc.perform(post("/api/v1/bar/bills/" + first + "/anomaly").with(supervisor).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "spot-raise-" + s, "reason", "客人称整单记错台位"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/bar/bills/" + first + "/anomaly/resolve").with(supervisor).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "spot-void-" + s, "resolution", "voided_reversed",
                        "reason", "核实整单记错，作废并冲正"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.settlement_status").value("void"));

        String second = data(bill(order, "v2").andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").doesNotExist())
                .andExpect(jsonPath("$.data.settlement_status").value("unsettled")), "$.data.id");
        assertThat(second).isNotEqualTo(first);
        assertThat(jdbc.queryForList("SELECT id FROM bar_bills WHERE order_id = ? AND settlement_status <> 'void'", String.class, order))
                .containsExactly(second);
    }

    // ---------- helpers ----------

    /** 后台订单页「生成账单」：idempotency_key、operator_note。 */
    private ResultActions bill(String order, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/bill").with(staff).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "spot-bill-" + tag + "-" + s, "operator_note", "生成账单"))));
    }

    private int adjustments(String order, String type) {
        return jdbc.queryForObject("SELECT count(*) FROM bar_bill_adjustments a JOIN bar_bills b ON b.id = a.bill_id"
                + " WHERE b.order_id = ? AND a.adjustment_type = ?", Integer.class, order, type);
    }

    private String table(String tag, String type, long minConsumptionCent) {
        String id = "table_spot_" + tag + "_" + s;
        Instant now = Instant.now();
        bars.saveTable(new BarTableRecord(id, bar, bar, "T-" + tag + "-" + s, tag, type, 4, 0, minConsumptionCent, "CNY", "opened", now, now));
        bars.assignTableSpace(id, "space_spot_" + s);
        return id;
    }

    private void openSession(String tag, String table, long minConsumptionCent) {
        Instant now = Instant.now();
        bars.saveTableSession(new BarTableSessionRecord("session_spot_" + tag + "_" + s, bar, bar, LocalDate.now(), table, null, null, 2,
                minConsumptionCent, 0, null, null, "walk_in", "open", now, null, null, 1L, now, now));
    }

    /** 后台订单页与平板「下单」请求：bar_id、table_id、items[{product_id, quantity}]、idempotency_key。 */
    private ResultActions order(String table, String product, int quantity) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "spot-order-" + UUID.randomUUID());
        body.put("bar_id", bar);
        body.put("table_id", table);
        body.put("items", List.of(Map.of("product_id", product, "quantity", quantity)));
        return mockMvc.perform(post("/api/v1/bar/orders").with(staff).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 后台订单页「标记送达」：整单明细一次送达。 */
    private ResultActions deliver(String order, String tag) throws Exception {
        List<Map<String, Object>> lines = jdbc.queryForList("SELECT id, quantity FROM bar_order_items WHERE order_id = ?", order).stream()
                .map(row -> Map.<String, Object>of("order_item_id", row.get("id"), "quantity", row.get("quantity"))).toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "spot-deliver-" + tag + "-" + s);
        body.put("operator_note", "已送达");
        body.put("delivery_photo_attachment_ids", List.of());
        body.put("delivered_items", lines);
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/deliver").with(staff)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 平板与后台结账：现金登记，payment_id 取本次登记的幂等键。 */
    private ResultActions settle(String bill, long amountCent) throws Exception {
        String key = "spot-settle-" + UUID.randomUUID();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("payment_id", key);
        body.put("settlement_amount_cent", amountCent);
        body.put("payment_channel", "cash");
        return mockMvc.perform(post("/api/v1/bar/bills/" + bill + "/settle").with(staff)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 小程序扫码点单：Idempotency-Key 头，体 items=[{product_id, quantity}]，匿名。 */
    private ResultActions scanOrder(String table, int quantity) throws Exception {
        String key = "spot-scan-" + UUID.randomUUID();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("items", List.of(Map.of("product_id", beer, "quantity", quantity)));
        return mockMvc.perform(post("/api/v1/customer/bar-tables/" + table + "/scan-orders").header("X-Terminal-Type", "miniapp")
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 员工扫码下单 POST /api/v1/bar/table-sessions/{session_id}/scan-orders：体 idempotency_key、items=[{product_id, quantity}]。 */
    private ResultActions staffScanOrder(String session) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "spot-staff-scan-" + UUID.randomUUID());
        body.put("items", List.of(Map.of("product_id", beer, "quantity", 1)));
        return mockMvc.perform(post("/api/v1/bar/table-sessions/" + session + "/scan-orders").with(staff)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private String orderId(ResultActions result) throws Exception {
        return data(result, "$.data.id");
    }

    private static String data(ResultActions result, String path) throws Exception {
        return CustomerTicketPaymentFixture.read(result, path);
    }

    private int stock() {
        return jdbc.queryForObject("SELECT available_quantity FROM inventory_stock_balances WHERE item_id = ? AND location_id = ?",
                Integer.class, item, location);
    }

    private int movements(String order) {
        return jdbc.queryForObject("SELECT count(*) FROM inventory_stock_movements WHERE business_id = ?", Integer.class, order);
    }

    private int orders(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM bar_orders WHERE table_id = ?", Integer.class, table);
    }

    private int intents() {
        return jdbc.queryForObject("SELECT count(*) FROM finance_payment_intents WHERE business_order_type = 'bar_scan_order'", Integer.class);
    }
}
