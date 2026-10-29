package com.complex.platform.bar;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.complex.platform.support.CustomerTicketPaymentFixture;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-03 clients-R7 扫码点单服务端＋backend-R24 客人不能自助下赠品（真库）。依据：
 * BSM 第 1025 行「扫码点单只适用于散台，卡座不开放扫码点单。散台也须服务员先开台才能扫码点单，开台后客人扫描桌台二维码即可自助点单，
 * 下单时即时付款、付款成功订单才成立，不挂账；酒水必须先完成付款才能出品上台。付款成功后所点酒水计入该桌位当前账单，账单上逐项列出物品名称与价格，
 * 并分别标注已上菜与未上菜……此后新客人看到的是一张空账单，看不到上一位客人的消费」；ENUM 第 129 行「非会员可以浏览、点单和付款」；
 * ENUM 第 111 行「沽清商品在客人扫码点单菜单上置灰展示为已售完，不隐藏」；BSM 第 1069 行「凡免单、打折与赠送，一律记录原因、责任人、批准人」。
 * 客人请求按前端真实形状（蛇形键、X-Terminal-Type），匿名不带客人凭证。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "bar.scan_order.enabled=true"
})
@AutoConfigureMockMvc
@Transactional
class BarScanOrderCustomerJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private BarRepository bars;
    @Autowired private BarBillingService billing;
    @Autowired private com.complex.platform.finance.CustomerOnlinePaymentSettlementJob settlementJob;
    @MockitoBean private PaymentGatewayRouter gateway;

    private CustomerTicketPaymentFixture f;
    private String merchant;
    private String looseTable;
    private String cardSeat;
    private String idleLooseTable;
    private String sessionId;
    private final String beer = "product_scan_beer_" + UUID.randomUUID().toString().substring(0, 8);
    private final String soldOut = "product_scan_out_" + UUID.randomUUID().toString().substring(0, 8);

    @BeforeEach
    void seed() {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        f.stubGateway(gateway);
        String s = f.suffix;
        String project = "prj_scan_" + s;
        String company = "ent_scan_" + s;
        merchant = "bar_scan_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','扫码综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())", company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'扫码酒吧','bar',?,?,?,?,'active',now(),now())", merchant, company, company, company, project);
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) VALUES(?,?,?,'散台区','unit','active',now(),now())",
                "space_scan_" + s, project, merchant);
        Instant now = Instant.now();
        looseTable = table("loose", "loose_table", "opened");
        cardSeat = table("card", "card_seat", "opened");
        idleLooseTable = table("idle", "loose_table", "active");
        sessionId = openSession("first", looseTable);
        openSession("card", cardSeat);
        bars.saveProduct(new BarProductRecord(beer, merchant, merchant, "精酿啤酒", "beer", 3800L, "CNY", null, null, false, "active",
                now, now, null, null, null, false, null, null, null));
        bars.saveProduct(new BarProductRecord(soldOut, merchant, merchant, "黑啤", "beer", 4200L, "CNY", null, null, false, "active",
                now, now, null, null, null, true, now, "当日售完", null));
    }

    @Test
    void cardSeatTableRejected() throws Exception {
        anonymousGet("/api/v1/customer/bar-tables/" + cardSeat + "/menu")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SCAN_ORDER_TABLE_NOT_ALLOWED"));
    }

    @Test
    void unopenedLooseTableRejected() throws Exception {
        anonymousGet("/api/v1/customer/bar-tables/" + idleLooseTable + "/menu")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_NOT_OPENED"));
    }

    @Test
    void anonymousCanBrowseMenuWithSoldOutGreyed() throws Exception {
        anonymousGet("/api/v1/customer/bar-tables/" + looseTable + "/menu")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.product_id == '" + beer + "')].price_cent").value(3800))
                .andExpect(jsonPath("$.data.items[?(@.product_id == '" + soldOut + "')].sold_out").value(true));
    }

    @Test
    void customerScanOrderWithGiftFlagRejected() throws Exception {
        scanOrder(null, List.of(Map.of("product_id", beer, "quantity", 2, "is_gift", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("BAR_SCAN_ORDER_GIFT_NOT_ALLOWED"));
        assertThat(barOrders(looseTable)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_scan_order_payments WHERE table_id = ?", Integer.class, looseTable)).isZero();
    }

    @Test
    void anonymousOrderPendingUntilPaidThenLandsOnCurrentBillOnce() throws Exception {
        ResultActions created = scanOrder(null, List.of(Map.of("product_id", beer, "quantity", 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount_cent").value(7600))
                .andExpect(jsonPath("$.data.gateway_status").value("PENDING"))
                .andExpect(jsonPath("$.data.launch.kind").value("miniapp_wechat"));
        String scanOrderId = CustomerTicketPaymentFixture.read(created, "$.data.scan_order_id");
        String intentId = CustomerTicketPaymentFixture.read(created, "$.data.payment_intent_id");
        // 付款成功前不成单、不出品（未付时确认见 confirmBeforePaidKeepsOrderPending：本类整例一个事务，失败请求会把事务标为只回滚，不在同一例里接成单）。
        assertThat(barOrders(looseTable)).isZero();

        f.markPaid(intentId);
        String orderId = CustomerTicketPaymentFixture.read(confirm(scanOrderId).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("paid")), "$.data.bar_order_id");
        confirm(scanOrderId).andExpect(status().isOk()).andExpect(jsonPath("$.data.bar_order_id").value(orderId));
        assertThat(barOrders(looseTable)).isEqualTo(1);
        // 不挂账：该笔账单随付款结清；进出品流转。
        assertThat(jdbc.queryForObject("SELECT settlement_status FROM bar_bills WHERE order_id = ?", String.class, orderId)).isEqualTo("settled");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_production_tickets WHERE order_id = ?", Integer.class, orderId)).isEqualTo(1);
        anonymousGet("/api/v1/customer/bar-tables/" + looseTable + "/current-bill")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].product_name").value("精酿啤酒"))
                .andExpect(jsonPath("$.data.items[0].unit_price_cent").value(3800))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].served").value(false))
                .andExpect(jsonPath("$.data.total_cent").value(7600));
    }

    @Test
    void confirmBeforePaidKeepsOrderPending() throws Exception {
        ResultActions created = scanOrder(null, List.of(Map.of("product_id", beer, "quantity", 1))).andExpect(status().isOk());
        String scanOrderId = CustomerTicketPaymentFixture.read(created, "$.data.scan_order_id");
        confirm(scanOrderId).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("PAYMENT_NOT_SUCCEEDED"));
        assertThat(barOrders(looseTable)).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM bar_scan_order_payments WHERE id = ?", String.class, scanOrderId))
                .isEqualTo("pending");
    }

    /** 客人付完款没回前台：服务端结算作业向渠道查单后同样成单（不丢单）。 */
    @Test
    void settlementJobPlacesPaidScanOrderWhenClientNeverReturns() throws Exception {
        ResultActions created = scanOrder(null, List.of(Map.of("product_id", beer, "quantity", 1))).andExpect(status().isOk());
        String scanOrderId = CustomerTicketPaymentFixture.read(created, "$.data.scan_order_id");
        String intentId = CustomerTicketPaymentFixture.read(created, "$.data.payment_intent_id");
        f.markPaid(intentId);
        jdbc.update("UPDATE finance_payment_intents SET created_at = now() - interval '5 minutes' WHERE id = ?", intentId);
        settlementJob.settlePending();
        assertThat(jdbc.queryForObject("SELECT status FROM bar_scan_order_payments WHERE id = ?", String.class, scanOrderId))
                .isEqualTo("paid");
        assertThat(barOrders(looseTable)).isEqualTo(1);
    }

    @Test
    void memberScanOrderRecordsCustomer() throws Exception {
        String scanOrderId = CustomerTicketPaymentFixture.read(scanOrder(f.token, List.of(Map.of("product_id", beer, "quantity", 1)))
                .andExpect(status().isOk()), "$.data.scan_order_id");
        assertThat(jdbc.queryForObject("SELECT customer_id FROM bar_scan_order_payments WHERE id = ?", String.class, scanOrderId))
                .isEqualTo(f.customerId);
    }

    @Test
    void confirmAfterSessionClosedRefunds() throws Exception {
        ResultActions created = scanOrder(null, List.of(Map.of("product_id", beer, "quantity", 1))).andExpect(status().isOk());
        String scanOrderId = CustomerTicketPaymentFixture.read(created, "$.data.scan_order_id");
        f.markPaid(CustomerTicketPaymentFixture.read(created, "$.data.payment_intent_id"));
        jdbc.update("UPDATE bar_table_sessions SET status = 'closed', closed_at = now() WHERE id = ?", sessionId);
        confirm(scanOrderId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SCAN_ORDER_SESSION_CLOSED_REFUNDING"));
        assertThat(barOrders(looseTable)).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM bar_scan_order_payments WHERE id = ?", String.class, scanOrderId))
                .isEqualTo("refunding");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM finance_refunds WHERE source_type = 'bar_scan_order_rejection' AND target_id = ?",
                Integer.class, scanOrderId)).isEqualTo(1);
    }

    @Test
    void paidButSoldOutBeforeConfirmRefundsInsteadOfRetrying() throws Exception {
        ResultActions created = scanOrder(null, List.of(Map.of("product_id", beer, "quantity", 1))).andExpect(status().isOk());
        String scanOrderId = CustomerTicketPaymentFixture.read(created, "$.data.scan_order_id");
        String intentId = CustomerTicketPaymentFixture.read(created, "$.data.payment_intent_id");
        f.markPaid(intentId);
        // 下单后、到账前员工把这款酒沽清（营业中常见）：渠道已扣款却成不了单——登记原路待退，不整笔回滚让作业每轮重试。
        jdbc.update("UPDATE bar_products SET sold_out = TRUE, sold_out_at = now(), sold_out_reason = '当日售完' WHERE id = ?", beer);
        confirm(scanOrderId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SCAN_ORDER_REJECTED_REFUNDING"));
        assertThat(barOrders(looseTable)).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM bar_scan_order_payments WHERE id = ?", String.class, scanOrderId))
                .isEqualTo("refunding");
        assertThat(jdbc.queryForObject("SELECT status FROM finance_payment_intents WHERE id = ?", String.class, intentId))
                .isEqualTo("succeeded");
        assertThat(jdbc.queryForMap("SELECT refund_amount_cent, status FROM finance_refunds"
                + " WHERE source_type = 'bar_scan_order_rejection' AND target_id = ?", scanOrderId))
                .containsEntry("refund_amount_cent", 3800L).containsEntry("status", "requested");
        confirm(scanOrderId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SCAN_ORDER_REJECTED_REFUNDING"));
    }

    @Test
    void priceChangedBeforeConfirmRefundsSoBillMatchesPayment() throws Exception {
        ResultActions created = scanOrder(null, List.of(Map.of("product_id", beer, "quantity", 2))).andExpect(status().isOk());
        String scanOrderId = CustomerTicketPaymentFixture.read(created, "$.data.scan_order_id");
        f.markPaid(CustomerTicketPaymentFixture.read(created, "$.data.payment_intent_id"));
        jdbc.update("UPDATE bar_products SET price_cent = 4000 WHERE id = ?", beer);
        confirm(scanOrderId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SCAN_ORDER_REJECTED_REFUNDING"));
        assertThat(barOrders(looseTable)).isZero();
        assertThat(jdbc.queryForObject("SELECT refund_amount_cent FROM finance_refunds"
                + " WHERE source_type = 'bar_scan_order_rejection' AND target_id = ?", Long.class, scanOrderId)).isEqualTo(7600L);
    }

    @Test
    void currentBillShowsOnlyCurrentSession() throws Exception {
        ResultActions created = scanOrder(null, List.of(Map.of("product_id", beer, "quantity", 1))).andExpect(status().isOk());
        f.markPaid(CustomerTicketPaymentFixture.read(created, "$.data.payment_intent_id"));
        confirm(CustomerTicketPaymentFixture.read(created, "$.data.scan_order_id")).andExpect(status().isOk());
        anonymousGet("/api/v1/customer/bar-tables/" + looseTable + "/current-bill")
                .andExpect(jsonPath("$.data.items.length()").value(1));
        // 关台清台后新客开台：新客人看到的是一张空账单。
        jdbc.update("UPDATE bar_table_sessions SET status = 'closed', closed_at = now() WHERE id = ?", sessionId);
        openSession("second", looseTable);
        anonymousGet("/api/v1/customer/bar-tables/" + looseTable + "/current-bill")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.total_cent").value(0));
    }

    /**
     * 第十八批 I 段 86-R1（BSM 第 1070 行「一张账单的提成只计一次、只归帮该客人预订的那个人」；第 1468 行「业绩记名人与销售提成归属人始终是同一人」）：
     * 员工代订开台的散台（会话销售为代订员工），客人扫码付款成单——已结账单的销售取开台会话的销售、岗位档按其员工档案定，可计提归该员工。
     */
    @Test
    void paidScanOrderBillCarriesTheSessionSalesForCommission() throws Exception {
        String sales = "usr_scan_sales_" + f.suffix;
        jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,registration_status,status,employment_status,is_sales_position,"
                + "created_at,updated_at) VALUES(?,?,?,?,'订台销售','approved','active','employed',TRUE,now(),now())",
                "pro_scan_sales_" + f.suffix, "prj_scan_" + f.suffix, merchant, sales);
        jdbc.update("UPDATE bar_table_sessions SET sales_staff_id = ? WHERE id = ?", sales, sessionId);
        ResultActions created = scanOrder(null, List.of(Map.of("product_id", beer, "quantity", 1))).andExpect(status().isOk());
        f.markPaid(CustomerTicketPaymentFixture.read(created, "$.data.payment_intent_id"));
        String orderId = CustomerTicketPaymentFixture.read(confirm(CustomerTicketPaymentFixture.read(created, "$.data.scan_order_id"))
                .andExpect(status().isOk()), "$.data.bar_order_id");
        assertThat(jdbc.queryForMap("SELECT sales_staff_id, sales_role_type FROM bar_bills WHERE order_id = ?", orderId))
                .containsEntry("sales_staff_id", sales).containsEntry("sales_role_type", "sales");
        String bill = jdbc.queryForObject("SELECT id FROM bar_bills WHERE order_id = ?", String.class, orderId);
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        try {
            BarCommissionCalculationRecord commission = billing.calculateCommission(bill,
                    new CommissionCalculateRequest("scan-cm-" + f.suffix, bill, true, false));
            assertThat(commission.salesStaffId()).isEqualTo(sales);
            assertThat(commission.salesRoleType()).isEqualTo("sales");
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }

    // ---------- helpers ----------

    private String table(String tag, String type, String tableStatus) {
        String id = "table_scan_" + tag + "_" + f.suffix;
        Instant now = Instant.now();
        bars.saveTable(new BarTableRecord(id, merchant, merchant, "T-" + tag + "-" + f.suffix, tag, type, 4, 0, 0L, "CNY",
                tableStatus, now, now));
        bars.assignTableSpace(id, "space_scan_" + f.suffix);
        return id;
    }

    private String openSession(String tag, String table) {
        Instant now = Instant.now();
        return bars.saveTableSession(new BarTableSessionRecord("session_scan_" + tag + "_" + f.suffix, merchant, merchant,
                LocalDate.now(), table, null, null, 2, 0L, 0, null, null, "open", now, null, null, 1L, now, now)).id();
    }

    private ResultActions anonymousGet(String path) throws Exception {
        return mockMvc.perform(get(path).header("X-Terminal-Type", "miniapp"));
    }

    private ResultActions scanOrder(String customerToken, List<Map<String, Object>> items) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "scan-" + UUID.randomUUID());
        body.put("items", items);
        var request = post("/api/v1/customer/bar-tables/" + looseTable + "/scan-orders").header("X-Terminal-Type", "miniapp")
                .header("Idempotency-Key", body.get("idempotency_key").toString())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        if (customerToken != null) {
            request = request.header("X-Customer-Token", customerToken);
        }
        return mockMvc.perform(request);
    }

    private ResultActions confirm(String scanOrderId) throws Exception {
        String key = "scan-confirm-" + UUID.randomUUID();
        return mockMvc.perform(post("/api/v1/customer/bar-tables/" + looseTable + "/scan-orders/" + scanOrderId + "/confirm")
                .header("X-Terminal-Type", "miniapp").header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("idempotency_key", key))));
    }

    private int barOrders(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM bar_orders WHERE table_id = ?", Integer.class, table);
    }
}
