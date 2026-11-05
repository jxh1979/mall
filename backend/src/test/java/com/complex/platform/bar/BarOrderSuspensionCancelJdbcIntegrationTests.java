package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 T 段第 157 条 157-R6（真库）：BSM:1107「已出品挂起订单取消须经理审批。能回库的商品继续执行既有退菜、退酒和高价值退品证据审批链，
 * 并同步记录退菜、库存回补和审计流水；经理审批不能替代既有高价值审批链。」；BSM:1108「取消审批生效与付款成功只能有一项先完成，先完成者生效；
 * 取消生效时作废未完成付款；取消后迟到到账进入待退款，不得恢复订单或继续出品。」；BSM:1109「不可回库的已出品商品不得走普通取消」；PGM:699「经理审批取消」。
 * 请求按后台订单页真实形状（蛇形键、X-Terminal-Type: web）经控制器与真实授权链提交；预期失败的请求放在各用例最后（失败会把测试事务标成只回滚）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarOrderSuspensionCancelJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private BarRepository bars;
    @Autowired private BarRevenueSourceService revenues;
    @Autowired private BusinessDayResolver days;

    private String s;
    private String project;
    private String bar;
    private String wine;
    private String beer;
    private String highWine;
    private String item;
    private String highItem;
    private String location;
    private String cashierId;
    private String managerId;
    private RequestPostProcessor cashier;
    private RequestPostProcessor manager;
    private RequestPostProcessor voidDecider;
    private RequestPostProcessor admin;

    @BeforeEach
    void seed() throws Exception {
        s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        project = "prj_sc_" + s;
        String company = "ent_sc_" + s;
        bar = "bar_sc_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','挂起取消综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())", company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'挂起取消酒吧','bar',?,?,?,?,'active',now(),now())", bar, company, company, company, project);
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) VALUES(?,?,?,'包厢区','unit','active',now(),now())",
                "space_sc_" + s, project, bar);
        jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,"
                + "lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,"
                + "'effective','[\"bar\"]'::jsonb,now(),now())", "sc_ct_" + s, bar, bar, bar);
        // 包厢开账单要有该营业日已批准的收费规则（服务费率取 0，账单金额即消费额）。
        jdbc.update("""
                INSERT INTO bar_payroll_rules(id, rule_kind, project_id, merchant_id, effective_from, version, rule_json, reason, status,
                    platform_company_id, created_by)
                VALUES (?, 'charge', ?, NULL, ?, 'V0.01', ?::jsonb, '服务费 0', 'approved', ?, 'usr_sc')
                """, "bpr_sc_" + s, project, java.sql.Date.valueOf(LocalDate.now().minusDays(1)),
                json.writeValueAsString(new BarPayrollRequests.RuleValues(0, 0, 2000, null, null, null, List.of())), company);
        // 整瓶出品扣库存：干红到货价 80 元（低价值）吧台 10 瓶；陈年威士忌到货价 6000 元（高价值，全平台阈值出厂 5000 元）吧台 5 瓶。
        item = "item_sc_" + s;
        location = "loc_sc_" + s;
        String counter = "wh_sc_" + s;
        jdbc.update("INSERT INTO warehouses (id, project_id, merchant_id, warehouse_type, name) VALUES (?, ?, ?, 'bar_counter', '包厢吧台')", counter, project, bar);
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, '干红 750 毫升', '酒类', '瓶', 'active', ?, now(), now())", item, bar);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id, project_id)"
                + " VALUES (?, ?, ?, 10, 0, 10, now(), ?, ?, ?)", "bal_sc_" + s, item, location, bar, counter, project);
        jdbc.update("INSERT INTO inventory_item_cost_state(item_id,merchant_id,on_hand_quantity,total_cost_cent,avg_unit_cost_cent,has_cost_basis)"
                + " VALUES(?,?,10,80000,8000,TRUE)", item, bar);
        arrival("arr_sc_" + s, item, 8000L);
        highItem = "item_sc_high_" + s;
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, '陈年威士忌 700 毫升', '酒类', '瓶', 'active', ?, now(), now())", highItem, bar);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id, project_id)"
                + " VALUES (?, ?, ?, 5, 0, 5, now(), ?, ?, ?)", "bal_sc_high_" + s, highItem, location, bar, counter, project);
        jdbc.update("INSERT INTO inventory_item_cost_state(item_id,merchant_id,on_hand_quantity,total_cost_cent,avg_unit_cost_cent,has_cost_basis)"
                + " VALUES(?,?,5,3000000,600000,TRUE)", highItem, bar);
        arrival("arr_sc_high_" + s, highItem, 600000L);
        Instant now = Instant.now();
        wine = "product_sc_wine_" + s;
        bars.saveProduct(new BarProductRecord(wine, bar, bar, "干红", "wine", 30000L, "CNY", item, location, true, "active",
                now, now, null, null, null, false, null, null, null));
        beer = "product_sc_beer_" + s;
        bars.saveProduct(new BarProductRecord(beer, bar, bar, "精酿啤酒", "beer", 5000L, "CNY", null, null, false, "active",
                now, now, null, null, null, false, null, null, null));
        highWine = "product_sc_high_" + s;
        bars.saveProduct(new BarProductRecord(highWine, bar, bar, "陈年威士忌", "wine", 900000L, "CNY", highItem, location, true, "active",
                now, now, null, null, null, false, null, null, null));
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        cashierId = "usr_sc_cashier_" + s;
        managerId = "usr_sc_manager_" + s;
        // 收银员兼持经理码，用来验证「审批人不得是取消发起人」。
        cashier = staffs.staff(cashierId, "staff", project, bar, "bar.order.create", "finance.payment.register", "bar.bill.hang",
                "bar.bill.hang_cancel", "bar.order_item.void", "evidence.attachment.create", "bar.table.open_close");
        manager = staffs.staff(managerId, "staff", project, bar, "bar.bill.hang_cancel");
        voidDecider = staffs.staff("usr_sc_decider_" + s, "staff", project, bar, "bar.order_item.void", "bar.order_item.void_decide");
        admin = staffs.staff("usr_sc_admin_" + s, "platform_admin", project, null, "finance.daily_close.lock", "finance.daily_close.manage");
    }

    /**
     * 挂起后收银员发起取消（勾选可回库的已出品干红）→ 另一经理批准：库存回补且有回库流水、退菜单已批准且审批人为该经理，本单账单作废、
     * 账单待支付的支付意向作废，挂起已取消并留痕；关台与日结不再被此账单拦；之后不得加菜、出品。
     */
    @Test
    void managerApprovesCancelReturnsStockVoidsBillAndPendingPayment() throws Exception {
        String table = table("a");
        String session = openSession("a", table);
        String order = orderId(order(table, Map.of(wine, 1)));
        String bill = bill(order, "a");
        deliver(order).andExpect(status().isOk());
        assertThat(stock(item)).isEqualTo(9);
        String itemId = itemOf(order, wine);
        suspend(order).andExpect(status().isOk());
        String intent = "pi_sc_" + s;
        jdbc.update("INSERT INTO finance_payment_intents(id,business_order_type,business_order_id,amount_cent,currency,channel,status,created_at,updated_at)"
                + " VALUES(?,'bar_bill',?,30000,'CNY','wechat_pay','created',now(),now())", intent, bill);

        requestCancel(order, List.of(itemId), "客人整瓶未开，退回", null, List.of()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("cancel_pending_approval"))
                .andExpect(jsonPath("$.data.cancel_item_ids[0]").value(itemId))
                .andExpect(jsonPath("$.data.cancel_reason").value("客人整瓶未开，退回"))
                .andExpect(jsonPath("$.data.cancel_requested_by").value(cashierId));
        String id = data(decide(manager, order, "approve").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("cancelled"))
                .andExpect(jsonPath("$.data.cancel_decided_by").value(managerId)), "$.data.id");

        assertThat(stock(item)).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM inventory_stock_movements WHERE movement_type = 'inbound'"
                + " AND business_type = 'bar_order_item_void' AND business_id = ?", Integer.class, itemId)).isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT status, approval_mode, requested_by_user_id, reviewed_by_user_id FROM bar_order_item_voids"
                + " WHERE order_item_id = ?", itemId)).containsEntry("status", "approved").containsEntry("approval_mode", "immediate")
                .containsEntry("requested_by_user_id", cashierId).containsEntry("reviewed_by_user_id", managerId);
        assertThat(jdbc.queryForMap("SELECT payment_status, settlement_status FROM bar_bills WHERE id = ?", bill))
                .containsEntry("payment_status", "void").containsEntry("settlement_status", "void");
        assertThat(jdbc.queryForObject("SELECT status FROM finance_payment_intents WHERE id = ?", String.class, intent)).isEqualTo("cancelled");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'bar.order_suspension_cancelled' AND business_id = ?",
                Integer.class, id)).isEqualTo(1);

        serviceClose(session).andExpect(status().isOk());
        LocalDate day = days.businessDate(null, bar, Instant.now());
        String close = data(mockMvc.perform(post("/api/v1/finance/daily-closes").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "sc-close-" + s, "operator_note", "日结",
                        "business_date", day.toString(), "scope_type", "merchant", "scope_id", bar)))).andExpect(status().isOk()), "$.data.id");
        mockMvc.perform(post("/api/v1/finance/daily-closes/" + close + "/confirm").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "sc-confirm-" + s))))
                .andExpect(jsonPath("$.detail_code").value(not("DAY_CLOSE_UNSETTLED_BILL")));

        Map<String, Object> add = new LinkedHashMap<>();
        add.put("idempotency_key", "sc-add-" + s);
        add.put("items", List.of(Map.of("product_id", beer, "quantity", 1)));
        mockMvc.perform(post("/api/v1/bar/orders/" + order + "/items").with(cashier).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(add)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_ORDER_SUSPENSION_CANCELLED"));
        deliver(order).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_ORDER_SUSPENSION_CANCELLED"));
    }

    /** 经理驳回 → 回到已挂起待付款、库存与账单不动，可重新发起；发起人自己审批 409 BAR_SUSPENSION_CANCEL_SELF_APPROVAL。 */
    @Test
    void rejectReturnsToHangingAndRequesterCannotApprove() throws Exception {
        String table = table("r");
        openSession("r", table);
        String order = orderId(order(table, Map.of(wine, 1)));
        String bill = bill(order, "r");
        deliver(order).andExpect(status().isOk());
        String itemId = itemOf(order, wine);
        suspend(order).andExpect(status().isOk());
        requestCancel(order, List.of(itemId), "客人要求退", null, List.of()).andExpect(status().isOk());

        decide(manager, order, "reject").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("hanging_unpaid"))
                .andExpect(jsonPath("$.data.cancel_decided_by").value(managerId));
        assertThat(stock(item)).isEqualTo(9);
        assertThat(jdbc.queryForObject("SELECT payable_amount_cent FROM bar_bills WHERE id = ?", Long.class, bill)).isEqualTo(30000L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_order_item_voids WHERE order_item_id = ?", Integer.class, itemId)).isZero();

        requestCancel(order, List.of(itemId), "客人再次要求退", null, List.of()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("cancel_pending_approval"));
        decide(cashier, order, "approve").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SUSPENSION_CANCEL_SELF_APPROVAL"));
    }

    /** 取消审批中先付款结清 → 挂起已结清（付款先完成者生效），经理再批准 409 BAR_SUSPENSION_NOT_CANCEL_PENDING。 */
    @Test
    void paymentFirstWinsOverPendingCancel() throws Exception {
        String table = table("p");
        openSession("p", table);
        String order = orderId(order(table, Map.of(wine, 1)));
        String bill = bill(order, "p");
        deliver(order).andExpect(status().isOk());
        String itemId = itemOf(order, wine);
        suspend(order).andExpect(status().isOk());
        requestCancel(order, List.of(itemId), "客人要求退", null, List.of()).andExpect(status().isOk());

        settle(bill, "cash", 30000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.settlement_status").value("settled"));
        assertThat(jdbc.queryForObject("SELECT status FROM bar_order_suspensions WHERE order_id = ?", String.class, order)).isEqualTo("settled");
        decide(manager, order, "approve").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SUSPENSION_NOT_CANCEL_PENDING"));
    }

    /** 取消生效后又登记 POS 到账 → 账单仍作废、该笔流水冲正、挂起待退款并挂原路退款应付，营业额与分红的酒吧收入不含该单。 */
    @Test
    void latePaymentAfterCancelGoesToPendingRefund() throws Exception {
        String table = table("l");
        openSession("l", table);
        String order = orderId(order(table, Map.of(wine, 1)));
        String bill = bill(order, "l");
        deliver(order).andExpect(status().isOk());
        suspend(order).andExpect(status().isOk());
        requestCancel(order, List.of(itemOf(order, wine)), "客人整瓶未开，退回", null, List.of()).andExpect(status().isOk());
        decide(manager, order, "approve").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("cancelled"));

        String key = "sc-late-" + s;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("payment_id", key);
        body.put("payment_channel", "pos");
        body.put("pos_transaction_no", "POS-" + s);
        body.put("settlement_amount_cent", 30000);
        mockMvc.perform(post("/api/v1/bar/bills/" + bill + "/settle").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlement_status").value("void"));
        Map<String, Object> settlement = jdbc.queryForMap("SELECT id, reversed, settle_channel, pos_transaction_no FROM bar_bill_settlements"
                + " WHERE bill_id = ? AND settlement_amount_cent = 30000 AND pos_transaction_no = ?", bill, "POS-" + s);
        assertThat(settlement).containsEntry("reversed", true).containsEntry("settle_channel", "pos");
        Map<String, Object> row = jdbc.queryForMap("SELECT id, status, refund_ar_ap_item_id FROM bar_order_suspensions WHERE order_id = ?", order);
        assertThat(row).containsEntry("status", "pending_refund");
        assertThat(jdbc.queryForMap("SELECT direction, source_type, source_id, amount_cent FROM ar_ap_items WHERE id = ?", row.get("refund_ar_ap_item_id")))
                .containsEntry("direction", "payable").containsEntry("source_type", "refund")
                .containsEntry("source_id", settlement.get("id")).containsEntry("amount_cent", 30000L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'bar.order_suspension_late_payment' AND business_id = ?",
                Integer.class, row.get("id"))).isEqualTo(1);
        assertThat(revenues.completedCollections(bar, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS)))
                .noneMatch(revenue -> revenue.billId().equals(bill));
    }

    /**
     * 高价值洋酒：带原因码与照片视频发起取消、经理批准后仍为取消审批中、库存不动（经理审批不能替代既有高价值审批链）；退菜退酒审批批准后才回库、转已取消。
     * 缺原因码 400 BAR_VOID_EVIDENCE_REQUIRED。
     */
    @Test
    void highValueCancelWaitsForExistingVoidApproval() throws Exception {
        String table = table("h");
        openSession("h", table);
        String order = orderId(order(table, Map.of(highWine, 1)));
        bill(order, "h");
        deliver(order).andExpect(status().isOk());
        String itemId = itemOf(order, highWine);
        suspend(order).andExpect(status().isOk());
        String photo = upload("现场照片.jpg", "image/jpeg");
        String video = upload("现场视频.mp4", "video/mp4");
        requestCancel(order, List.of(itemId), "客人整瓶未开，退回", "customer_change", List.of(photo, video)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cancel_reason_code").value("customer_change"));

        decide(manager, order, "approve").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("cancel_pending_approval"));
        assertThat(stock(highItem)).isEqualTo(4);
        Map<String, Object> pending = jdbc.queryForMap("SELECT id, status, approval_mode, requested_by_user_id FROM bar_order_item_voids WHERE order_item_id = ?", itemId);
        assertThat(pending).containsEntry("status", "pending").containsEntry("approval_mode", "approval_first")
                .containsEntry("requested_by_user_id", cashierId);

        decideVoid((String) pending.get("id"), "approved").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM bar_order_suspensions WHERE order_id = ?", String.class, order)).isEqualTo("cancelled");
        assertThat(stock(highItem)).isEqualTo(5);

        String other = orderId(order(table, Map.of(highWine, 1)));
        bill(other, "h2");
        deliver(other).andExpect(status().isOk());
        suspend(other).andExpect(status().isOk());
        requestCancel(other, List.of(itemOf(other, highWine)), "客人要求退", null, List.of(photo, video)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("BAR_VOID_EVIDENCE_REQUIRED"));
    }

    /** 高价值退品审批驳回 → 挂起回到已挂起待付款，该项仍在账单（应付不变）、库存不动。 */
    @Test
    void highValueVoidRejectedReturnsToHanging() throws Exception {
        String table = table("j");
        openSession("j", table);
        String order = orderId(order(table, Map.of(highWine, 1)));
        String bill = bill(order, "j");
        deliver(order).andExpect(status().isOk());
        String itemId = itemOf(order, highWine);
        suspend(order).andExpect(status().isOk());
        requestCancel(order, List.of(itemId), "客人要求退", "customer_change",
                List.of(upload("现场照片.jpg", "image/jpeg"), upload("现场视频.mp4", "video/mp4"))).andExpect(status().isOk());
        decide(manager, order, "approve").andExpect(status().isOk());
        String voidId = jdbc.queryForObject("SELECT id FROM bar_order_item_voids WHERE order_item_id = ?", String.class, itemId);

        decideVoid(voidId, "rejected").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM bar_order_suspensions WHERE order_id = ?", String.class, order)).isEqualTo("hanging_unpaid");
        assertThat(jdbc.queryForObject("SELECT voided FROM bar_order_items WHERE id = ?", Boolean.class, itemId)).isFalse();
        assertThat(jdbc.queryForObject("SELECT payable_amount_cent FROM bar_bills WHERE id = ?", Long.class, bill)).isEqualTo(900000L);
        assertThat(stock(highItem)).isEqualTo(4);
    }

    /**
     * 只取消部分明细 → 批准后回到已挂起待付款、应付减少；已收一半的整单取消 → 已取消，已收部分挂原路退款应付、收款流水冲正；
     * 挂起中经退菜退酒直接退已出品商品 409 BAR_ORDER_SUSPENDED_USE_CANCEL（逐项退掉即等于未经经理审批取消）。
     */
    @Test
    void partialCancelAndPartiallyPaidCancel() throws Exception {
        String table = table("m");
        openSession("m", table);
        String order = orderId(order(table, Map.of(wine, 1, beer, 1)));
        String bill = bill(order, "m");
        deliver(order).andExpect(status().isOk());
        suspend(order).andExpect(status().isOk());
        requestCancel(order, List.of(itemOf(order, wine)), "干红未开，退回", null, List.of()).andExpect(status().isOk());
        decide(manager, order, "approve").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("hanging_unpaid"));
        assertThat(jdbc.queryForObject("SELECT payable_amount_cent FROM bar_bills WHERE id = ?", Long.class, bill)).isEqualTo(5000L);

        String paid = orderId(order(table, Map.of(wine, 2)));
        String paidBill = bill(paid, "m2");
        settle(paidBill, "cash", 30000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.settlement_status").value("partial_settled"));
        deliver(paid).andExpect(status().isOk());
        suspend(paid).andExpect(status().isOk());
        requestCancel(paid, List.of(itemOf(paid, wine)), "两瓶都未开，退回", null, List.of()).andExpect(status().isOk());
        decide(manager, paid, "approve").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("cancelled"));
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ar_ap_items WHERE direction = 'payable' AND source_type = 'refund' AND source_id = ?",
                Long.class, paidBill)).isEqualTo(30000L);
        assertThat(jdbc.queryForObject("SELECT bool_and(reversed) FROM bar_bill_settlements WHERE bill_id = ?", Boolean.class, paidBill)).isTrue();

        Map<String, Object> voidBody = new LinkedHashMap<>();
        voidBody.put("idempotency_key", "sc-void-" + s);
        voidBody.put("order_item_id", itemOf(order, beer));
        voidBody.put("reason", "客人要求退");
        voidBody.put("return_to_stock", true);
        voidBody.put("bill_id", bill);
        mockMvc.perform(post("/api/v1/bar/orders/" + order + "/void-item").with(cashier).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(voidBody)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_ORDER_SUSPENDED_USE_CANCEL"));
    }

    /**
     * 18T 复核修复①（BSM:1107「能回库的商品继续执行既有退菜、退酒和高价值退品证据审批链，并同步记录退菜、库存回补和审计流水」；BSM:1109「不可回库的已出品商品
     * 不得走普通取消。经理确认最终无法收款后转为坏账损耗处理中」；BSM:1111「财务确认未收售价、库存成本和赔偿记录均完成后，订单转为已核销结案」）：
     * 包厢一单出品干红 1 瓶与餐酒 2 瓶（同一库存物料，每瓶成本 8000）→ 挂起 → 只取消干红、经理批准（干红回库，挂起回到已挂起待付款）→ 经理对餐酒确认无法收款：
     * 固化的库存成本等于赔偿登记各行成本之和（只有餐酒 16000），不含已回库干红的 8000。
     */
    @Test
    void badDebtAfterPartialCancelFreezesOnlyUnreturnedCost() throws Exception {
        Instant now = Instant.now();
        String house = "product_sc_house_" + s;
        bars.saveProduct(new BarProductRecord(house, bar, bar, "餐酒", "wine", 20000L, "CNY", item, location, true, "active",
                now, now, null, null, null, false, null, null, null));
        String table = table("d");
        openSession("d", table);
        String order = orderId(order(table, Map.of(wine, 1, house, 2)));
        bill(order, "d");
        deliver(order).andExpect(status().isOk());
        suspend(order).andExpect(status().isOk());
        requestCancel(order, List.of(itemOf(order, wine)), "干红未开，退回", null, List.of()).andExpect(status().isOk());
        decide(manager, order, "approve").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("hanging_unpaid"));
        assertThat(stock(item)).isEqualTo(8);

        mockMvc.perform(post("/api/v1/bar/orders/" + order + "/suspension/bad-debt").with(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "sc-bd-" + s, "reason", "餐酒已开，客人失联"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("bad_debt_processing"))
                .andExpect(jsonPath("$.data.compensation_lines.length()").value(1))
                .andExpect(jsonPath("$.data.compensation_lines[0].order_item_id").value(itemOf(order, house)))
                .andExpect(jsonPath("$.data.compensation_lines[0].cost_cent").value(16000))
                .andExpect(jsonPath("$.data.inventory_cost_cent").value(16000));
    }

    // ---------- helpers ----------

    private String table(String tag) {
        String id = "table_sc_" + tag + "_" + s;
        Instant now = Instant.now();
        bars.saveTable(new BarTableRecord(id, bar, bar, "T-" + tag + "-" + s, tag, "vip", 4, 0, 0L, "CNY", "opened", now, now));
        bars.assignTableSpace(id, "space_sc_" + s);
        return id;
    }

    private String openSession(String tag, String table) {
        Instant now = Instant.now();
        String id = "session_sc_" + tag + "_" + s;
        bars.saveTableSession(new BarTableSessionRecord(id, bar, bar, LocalDate.now(), table, null, null, 2,
                0L, 0, null, null, "walk_in", "open", now, null, null, 1L, now, now));
        return id;
    }

    private ResultActions order(String table, Map<String, Integer> lines) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "sc-order-" + UUID.randomUUID());
        body.put("bar_id", bar);
        body.put("table_id", table);
        body.put("items", lines.entrySet().stream().map(e -> Map.of("product_id", e.getKey(), "quantity", e.getValue())).toList());
        return mockMvc.perform(post("/api/v1/bar/orders").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    private String bill(String order, String tag) throws Exception {
        return data(mockMvc.perform(post("/api/v1/bar/orders/" + order + "/bill").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "sc-bill-" + tag + "-" + s)))).andExpect(status().isOk()),
                "$.data.id");
    }

    private String itemOf(String order, String product) {
        return jdbc.queryForObject("SELECT id FROM bar_order_items WHERE order_id = ? AND product_id = ?", String.class, order, product);
    }

    private ResultActions deliver(String order) throws Exception {
        List<Map<String, Object>> lines = jdbc.queryForList("SELECT id, quantity FROM bar_order_items WHERE order_id = ? AND NOT voided", order).stream()
                .map(row -> Map.<String, Object>of("order_item_id", row.get("id"), "quantity", row.get("quantity"))).toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "sc-deliver-" + UUID.randomUUID());
        body.put("operator_note", "已送达");
        body.put("delivery_photo_attachment_ids", List.of());
        body.put("delivered_items", lines.isEmpty() ? List.of(Map.of("order_item_id", "none", "quantity", 1)) : lines);
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/deliver").with(cashier)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions serviceClose(String session) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "sc-svc-close-" + session);
        body.put("session_version", jdbc.queryForObject("SELECT session_version FROM bar_table_sessions WHERE id = ?", Long.class, session));
        body.put("reason", "客人离开");
        return mockMvc.perform(post("/api/v1/bar/table-sessions/" + session + "/service-close").with(cashier)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions suspend(String order) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/suspend").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "sc-hang-" + order, "reason", "客人稍后付款"))));
    }

    /** 后台订单页「发起取消」：体 order_item_ids、reason、reason_code（选填）、evidence_attachment_ids、idempotency_key。 */
    private ResultActions requestCancel(String order, List<String> items, String reason, String reasonCode, List<String> evidence) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "sc-cancel-" + UUID.randomUUID());
        body.put("order_item_ids", items);
        body.put("reason", reason);
        if (reasonCode != null) {
            body.put("reason_code", reasonCode);
        }
        body.put("evidence_attachment_ids", evidence);
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/suspension/cancel").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    /** 后台订单页「审批取消」：体 decision（approve/reject）、reason、idempotency_key。 */
    private ResultActions decide(RequestPostProcessor who, String order, String decision) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/suspension/cancel-decision").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "sc-decide-" + UUID.randomUUID(), "decision", decision,
                        "reason", "核对过现场"))));
    }

    /** 后台订单页退菜退酒待审队列「批准」「驳回」：体 decision（approved/rejected）、reason、idempotency_key。 */
    private ResultActions decideVoid(String voidId, String decision) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/order-item-voids/" + voidId + "/decision").with(voidDecider).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "sc-void-decide-" + UUID.randomUUID(), "decision", decision,
                        "reason", "核对照片与视频"))));
    }

    private String upload(String name, String contentType) throws Exception {
        return data(mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                .file(new MockMultipartFile("file", name, contentType, "现场证据".getBytes(StandardCharsets.UTF_8)))
                .param("business_type", "bar_order_item_void").param("attachment_role", contentType.startsWith("video") ? "video" : "photo")
                .with(cashier)).andExpect(status().isOk()), "$.data.id");
    }

    private ResultActions settle(String bill, String channel, long amountCent) throws Exception {
        String key = "sc-settle-" + UUID.randomUUID();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("payment_id", key);
        body.put("settlement_amount_cent", amountCent);
        body.put("payment_channel", channel);
        return mockMvc.perform(post("/api/v1/bar/bills/" + bill + "/settle").with(cashier)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private int stock(String inventoryItem) {
        return jdbc.queryForObject("SELECT quantity FROM inventory_stock_balances WHERE item_id = ? AND location_id = ?", Integer.class,
                inventoryItem, location);
    }

    /** 采购到货流水（高价值判定只看最近一次实际到货价）。 */
    private void arrival(String id, String itemId, long unitCostCent) {
        jdbc.update("INSERT INTO inventory_stock_movements (id, movement_type, item_id, target_location_id, quantity, business_type, status,"
                + " created_at, merchant_id, project_id, unit_cost_cent, total_cost_cent) VALUES (?, 'inbound', ?, ?, 1, 'purchase_request', 'completed',"
                + " now() - interval '1 day', ?, ?, ?, ?)", id, itemId, location, bar, project, unitCostCent, unitCostCent);
    }

    private String orderId(ResultActions result) throws Exception {
        return data(result.andExpect(status().isOk()), "$.data.id");
    }

    private static String data(ResultActions result, String path) throws Exception {
        return CustomerTicketPaymentFixture.read(result, path);
    }
}
