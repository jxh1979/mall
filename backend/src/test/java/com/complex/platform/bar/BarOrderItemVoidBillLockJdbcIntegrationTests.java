package com.complex.platform.bar;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 第十八批 AK 段第 261 条 261-B12-R1（真库、双线程）：PGM:303「两个操作同时发生冲突时，一律以“先处理成功”的那个为准……不会因为重试就重复扣款、
 * 重复入账」；BSM:1068「出品后退品按是否回库处理；正常退品应予退款」。结账由收银端发起、退菜退酒由后台订单页发起，两端写同一张账单：
 * 退菜退酒发起与驳回改账单先锁账单行，与结账同时发生时按结账后的账单处理（已结即走退款冲正），不把已结账单写回未结、已付 0。
 * 先到者（结账）在自己的事务里做完动作后延迟 1.5 秒提交，后到者在其未提交时开始；结果与「先结账后退酒／后驳回」顺序执行一致。
 * 请求按后台订单页与收银端真实形状（蛇形键、X-Terminal-Type）经控制器与真实授权链提交。
 *
 * <p>本类不开测试事务（两个线程各自提交）；夹具按随机后缀隔离、留在库里。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class BarOrderItemVoidBillLockJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private BarRepository bars;
    @Autowired private PlatformTransactionManager transactionManager;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String bar = "bvl_bar_" + s;
    private final String table = "bvl_table_" + s;
    private final String beer = "bvl_beer_" + s;
    private RequestPostProcessor cashier;
    private RequestPostProcessor decider;

    @BeforeEach
    void seed() {
        String project = "bvl_prj_" + s;
        String item = "bvl_item_" + s;
        String location = "bvl_loc_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','退酒锁账单综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at)"
                + " VALUES(?,'退酒锁账单酒吧','bar','active',?,now(),now())", bar, project);
        // 退菜退酒过合同准入闸：本酒吧有一份含酒吧业务的生效合同。
        jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,"
                + "lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,"
                + "'effective','[\"bar\"]'::jsonb,now(),now())", "bvl_ct_" + s, bar, bar, bar);
        jdbc.update("INSERT INTO warehouses (id, project_id, merchant_id, warehouse_type, name) VALUES (?, ?, ?, 'bar_counter', '一号吧台')",
                "bvl_wh_" + s, project, bar);
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, '精酿啤酒', '酒类', '瓶', 'active', ?, now(), now())", item, bar);
        jdbc.update("INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, merchant_id, warehouse_id)"
                + " VALUES (?, ?, ?, 20, 0, 20, now(), ?, ?)", "bvl_bal_" + s, item, location, bar, "bvl_wh_" + s);
        // 已出品退酒按物料最近到货价判高价值（V350）：到货价低于平台阈值＝低价值，退品即时生效。
        jdbc.update("INSERT INTO inventory_stock_movements (id, movement_type, item_id, target_location_id, quantity, business_type, status,"
                + " created_at, merchant_id, project_id, unit_cost_cent, total_cost_cent) VALUES (?, 'inbound', ?, ?, 20, 'purchase_request', 'completed',"
                + " now() - interval '1 day', ?, ?, 300, 6000)", "bvl_arrival_" + s, item, location, bar, project);
        Instant now = Instant.now();
        bars.saveProduct(new BarProductRecord(beer, bar, bar, "精酿啤酒", "beer", 500L, "CNY", item, location, true, "active",
                now, now, null, null, null, false, null, null, null));
        bars.saveTable(new BarTableRecord(table, bar, bar, "B-" + s, "吧台位", "bar_counter", 2, 0, 0L, "CNY", "active", now, now));
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        cashier = staffs.staff("bvl_cashier_" + s, "staff", project, bar, "bar.order.create", "finance.payment.register",
                "bar.order_item.void", "bar.bill.refund", "bar.bill.tip", "bar.bill.split_merge", "bar.order.view");
        decider = staffs.staff("bvl_sup_" + s, "staff", project, bar, "bar.order_item.void_decide");
    }

    /**
     * 先到者现金结清账单（持锁未提交），后到者对该账单一条已出品低价值明细发起退酒：结账提交后退酒按已结账走退款——账单仍已结、已付不回退、
     * 退款减项与退款应付各 1 条，与「先结账后退酒」顺序执行一致；再退同一明细 409，账单不变。
     */
    @Test
    void voidWaitingOnSettlementRefundsAgainstTheSettledBill() throws Exception {
        Bill sequential = deliveredBill("seq");
        perform(settle(sequential, "seq", 1500L)).andExpect(status().isOk());
        perform(voidItem(sequential, "seq")).andExpect(status().isOk());

        Bill concurrent = deliveredBill("con");
        firstHoldsThenSecond(settle(concurrent, "con", 1500L), voidItem(concurrent, "con")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approval_mode").value("immediate"));

        assertThat(billState(concurrent.id())).isEqualTo(billState(sequential.id()));
        assertThat(jdbc.queryForMap("SELECT settlement_status, paid_amount_cent, payable_amount_cent FROM bar_bills WHERE id = ?", concurrent.id()))
                .containsEntry("settlement_status", "settled").containsEntry("paid_amount_cent", 1500L).containsEntry("payable_amount_cent", 1000L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_bill_adjustments a JOIN ar_ap_items x ON x.id = a.ar_ap_item_id"
                + " WHERE a.bill_id = ? AND a.adjustment_type = 'refund' AND x.direction = 'payable' AND x.source_type = 'refund'",
                Integer.class, concurrent.id())).isEqualTo(1);

        String before = billState(concurrent.id());
        perform(voidItem(concurrent, "con-again")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_ORDER_ITEM_ALREADY_VOIDED"));
        assertThat(billState(concurrent.id())).isEqualTo(before);
    }

    /**
     * 先到者结账持锁，后到者驳回该账单上一条结账前已生效的即时退酒：账单仍已结、已付不回退，与「先结账后驳回」顺序执行一致；
     * 再驳回 409，账单不变。
     */
    @Test
    void rejectionWaitingOnSettlementKeepsTheBillSettled() throws Exception {
        Bill sequential = deliveredBill("rseq");
        String sequentialVoid = data(perform(voidItem(sequential, "rseq")).andExpect(status().isOk()), "/data/id");
        perform(settle(sequential, "rseq", 1000L)).andExpect(status().isOk());
        perform(reject(sequentialVoid, "rseq")).andExpect(status().isOk());

        Bill concurrent = deliveredBill("rcon");
        String concurrentVoid = data(perform(voidItem(concurrent, "rcon")).andExpect(status().isOk()), "/data/id");
        firstHoldsThenSecond(settle(concurrent, "rcon", 1000L), reject(concurrentVoid, "rcon")).andExpect(status().isOk());

        assertThat(billState(concurrent.id())).isEqualTo(billState(sequential.id()));
        assertThat(jdbc.queryForMap("SELECT settlement_status, paid_amount_cent FROM bar_bills WHERE id = ?", concurrent.id()))
                .containsEntry("settlement_status", "settled").containsEntry("paid_amount_cent", 1000L);

        String before = billState(concurrent.id());
        perform(reject(concurrentVoid, "rcon-again")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_ORDER_ITEM_VOID_NOT_PENDING"));
        assertThat(billState(concurrent.id())).isEqualTo(before);
    }

    /**
     * 第十八批 AU 段第 282 条 282-R1（PGM:303「两个操作同时发生冲突时，一律以“先处理成功”的那个为准」；BSM:1077 小费「结账可录」）：先到者现金结清账单
     * （持锁未提交），后到者给该账单加小费：先锁账单行，结账提交后按已结账单处理回 409，账单仍已结、已付等于应付、小费未变。
     */
    @Test
    void tipWaitingOnSettlementSeesTheSettledBill() throws Exception {
        Bill bill = withServiceSession(deliveredBill("tip"));
        firstHoldsThenSecond(settle(bill, "tip", 1500L), staffPost("/api/v1/bar/bills/" + bill.id() + "/tip", cashier,
                Map.of("idempotency_key", "bvl-tip-" + s, "tip_amount_cent", 5000)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_BILL_NOT_TIPPABLE"));
        assertSettledUntouched(bill);
    }

    /** 282-R1（BSM:1079「按金额拆出子账单供客人分别付款」只对未支付、未结算账单）：后到者对正被结账的账单拆单，结账提交后 409，不拆出账单。 */
    @Test
    void splitWaitingOnSettlementSeesTheSettledBill() throws Exception {
        Bill bill = deliveredBill("split");
        firstHoldsThenSecond(settle(bill, "split", 1500L), staffPost("/api/v1/bar/bills/" + bill.id() + "/split", cashier,
                Map.of("idempotency_key", "bvl-split-" + s, "split_amount_cent", 500, "reason", "客人分开付")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_BILL_NOT_SPLITTABLE"));
        assertSettledUntouched(bill);
    }

    /** 282-R1（BSM:1079「将同一酒吧内另一张未支付、未结算账单合入主账单」）：后到者把正被结账的账单并入另一张未付账单，结账提交后 409，副单不置已并单。 */
    @Test
    void mergeWaitingOnSettlementSeesTheSettledBill() throws Exception {
        Bill primary = deliveredBill("mrg-main");
        Bill bill = deliveredBill("mrg");
        String primaryBefore = billState(primary.id());
        firstHoldsThenSecond(settle(bill, "mrg", 1500L), staffPost("/api/v1/bar/bills/" + primary.id() + "/merge", cashier,
                Map.of("idempotency_key", "bvl-merge-" + s, "merged_bill_id", bill.id(), "reason", "客人一起付")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_BILL_NOT_MERGEABLE"));
        assertSettledUntouched(bill);
        assertThat(billState(primary.id())).isEqualTo(primaryBefore);
    }

    /**
     * 第十八批 AY 段第 348 条 348-R1（BSM:1079「账单合并称为“合单”，不等同于台位“并台”。」；PGM:287）：已并入主账单的副单再加小费 409 BAR_BILL_NOT_TIPPABLE、
     * 再拆单 409 BAR_BILL_NOT_SPLITTABLE，提示都用状态字典的同一叫法「已合并」，不出「并单」。修复前为「已结账或已并单的账单不能再加小费」。
     */
    @Test
    void mergedSecondaryBillMessagesSayMerged() throws Exception {
        Bill primary = deliveredBill("tipm-main");
        Bill secondary = deliveredBill("tipm");
        perform(staffPost("/api/v1/bar/bills/" + primary.id() + "/merge", cashier,
                Map.of("idempotency_key", "bvl-tipm-merge-" + s, "merged_bill_id", secondary.id(), "reason", "客人一起付")))
                .andExpect(status().isOk());

        String tip = data(perform(staffPost("/api/v1/bar/bills/" + secondary.id() + "/tip", cashier,
                Map.of("idempotency_key", "bvl-tipm-tip-" + s, "tip_amount_cent", 500)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_BILL_NOT_TIPPABLE")), "/message");
        assertThat(tip).contains("已合并").doesNotContain("并单");
        String split = data(perform(staffPost("/api/v1/bar/bills/" + secondary.id() + "/split", cashier,
                Map.of("idempotency_key", "bvl-tipm-split-" + s, "split_amount_cent", 500, "reason", "客人分开付")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_BILL_NOT_SPLITTABLE")), "/message");
        assertThat(split).contains("已合并").doesNotContain("并单");
    }

    /** 282-R1：订单列表账单摘要带应付、已付、小费金额（分），供后台订单页按所选账单显示与办理。 */
    @Test
    void orderListBillSummaryCarriesPayablePaidAndTip() throws Exception {
        Bill bill = withServiceSession(deliveredBill("sum"));
        perform(staffPost("/api/v1/bar/bills/" + bill.id() + "/tip", cashier,
                Map.of("idempotency_key", "bvl-sum-tip-" + s, "tip_amount_cent", 300))).andExpect(status().isOk());
        perform(settle(bill, "sum", 1000L)).andExpect(status().isOk());
        var orders = json.readTree(mockMvc.perform(get("/api/v1/bar/orders").with(cashier)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).at("/data/items");
        var summary = java.util.stream.StreamSupport.stream(orders.spliterator(), false)
                .filter(order -> bill.order().equals(order.path("id").asText())).findFirst().orElseThrow().at("/bills/0");
        assertThat(summary.path("id").asText()).isEqualTo(bill.id());
        assertThat(summary.path("payable_amount_cent").isNumber()).isTrue();
        assertThat(summary.path("payable_amount_cent").asLong()).isEqualTo(1500L);
        assertThat(summary.path("paid_amount_cent").asLong()).isEqualTo(1000L);
        assertThat(summary.path("tip_amount_cent").asLong()).isEqualTo(300L);
    }

    // ---------- helpers ----------

    /** 小费按落单时锁定的服务场次归属（BSM:1077「归属该桌台的主责服务提供人」）：给本单补一条已关台的服务场次与订单归属。 */
    private Bill withServiceSession(Bill bill) {
        String session = "bvl_sess_" + bill.order();
        jdbc.update("INSERT INTO bar_table_sessions(id,bar_id,merchant_id,business_date,table_id,service_staff_id,status,opened_at,closed_at,created_at,updated_at)"
                + " VALUES(?,?,?,current_date,?,?,'closed',now(),now(),now(),now())", session, bar, bar, table, "bvl_cashier_" + s);
        jdbc.update("INSERT INTO bar_order_service_sources(order_id,session_id,table_id,project_id,merchant_id,business_date) VALUES(?,?,?,?,?,current_date)",
                bill.order(), session, table, "bvl_prj_" + s, bar);
        return bill;
    }

    /** 被结账单仍已结、已付等于应付（1500）、小费 0，本单没有拆出的账单。 */
    private void assertSettledUntouched(Bill bill) {
        assertThat(jdbc.queryForMap("SELECT settlement_status, payment_status, paid_amount_cent, payable_amount_cent, tip_amount_cent FROM bar_bills WHERE id = ?",
                bill.id())).containsEntry("settlement_status", "settled").containsEntry("payment_status", "paid")
                .containsEntry("paid_amount_cent", 1500L).containsEntry("payable_amount_cent", 1500L).containsEntry("tip_amount_cent", 0L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_bills WHERE order_id = ?", Integer.class, bill.order())).isEqualTo(1);
    }

    private record Bill(String id, String order, String smallItem) {
    }

    /** 吧台位下单两行（1 瓶 500、2 瓶 1000），生成账单（应付 1500）并整单送达；smallItem 为 1 瓶那一行。 */
    private Bill deliveredBill(String tag) throws Exception {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("idempotency_key", "bvl-order-" + tag + "-" + s);
        order.put("bar_id", bar);
        order.put("table_id", table);
        order.put("items", List.of(Map.of("product_id", beer, "quantity", 1), Map.of("product_id", beer, "quantity", 2)));
        String orderId = data(perform(staffPost("/api/v1/bar/orders", cashier, order)).andExpect(status().isOk()), "/data/id");
        String bill = data(perform(staffPost("/api/v1/bar/orders/" + orderId + "/bill", cashier,
                Map.of("idempotency_key", "bvl-bill-" + tag + "-" + s))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payable_amount_cent").value(1500)), "/data/id");
        List<Map<String, Object>> lines = jdbc.queryForList("SELECT id, quantity FROM bar_order_items WHERE order_id = ? ORDER BY quantity", orderId);
        Map<String, Object> deliver = new LinkedHashMap<>();
        deliver.put("idempotency_key", "bvl-deliver-" + tag + "-" + s);
        deliver.put("operator_note", "已送达");
        deliver.put("delivery_photo_attachment_ids", List.of());
        deliver.put("delivered_items", lines.stream()
                .map(row -> Map.<String, Object>of("order_item_id", row.get("id"), "quantity", row.get("quantity"))).toList());
        perform(staffPost("/api/v1/bar/orders/" + orderId + "/deliver", cashier, deliver)).andExpect(status().isOk());
        return new Bill(bill, orderId, (String) lines.getFirst().get("id"));
    }

    /** 收银端结账：现金登记，payment_id 取本次登记的幂等键。 */
    private MockHttpServletRequestBuilder settle(Bill bill, String tag, long amountCent) throws Exception {
        String key = "bvl-settle-" + tag + "-" + s;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("payment_id", key);
        body.put("settlement_amount_cent", amountCent);
        body.put("payment_channel", "cash");
        return staffPost("/api/v1/bar/bills/" + bill.id() + "/settle", cashier, body);
    }

    /** 后台订单页「退菜退酒」：已出品明细不回库，资金回退指到本单账单。 */
    private MockHttpServletRequestBuilder voidItem(Bill bill, String tag) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bvl-void-" + tag + "-" + s);
        body.put("order_item_id", bill.smallItem());
        body.put("reason", "客人不要了");
        body.put("evidence_attachment_ids", List.of());
        body.put("return_to_stock", false);
        body.put("bill_id", bill.id());
        body.put("operator_note", "退菜退酒");
        return staffPost("/api/v1/bar/orders/" + bill.order() + "/void-item", cashier, body);
    }

    /** 后台订单页退酒审批：驳回。 */
    private MockHttpServletRequestBuilder reject(String voidId, String tag) throws Exception {
        return staffPost("/api/v1/bar/order-item-voids/" + voidId + "/decision", decider,
                Map.of("idempotency_key", "bvl-reject-" + tag + "-" + s, "decision", "rejected"));
    }

    private MockHttpServletRequestBuilder staffPost(String path, RequestPostProcessor who, Map<String, Object> body) throws Exception {
        return post(path).with(who).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request);
    }

    /** 先到者在自己的事务里做完动作（须 200）后延迟 1.5 秒提交；后到者在其未提交时开始、走自己的事务。返回后到者的响应。 */
    private ResultActions firstHoldsThenSecond(MockHttpServletRequestBuilder first, MockHttpServletRequestBuilder second) throws Exception {
        CountDownLatch firstDone = new CountDownLatch(1);
        AtomicReference<Throwable> firstError = new AtomicReference<>();
        Thread holder = new Thread(() -> {
            try {
                new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                    try {
                        mockMvc.perform(first).andExpect(status().isOk());
                        firstDone.countDown();
                        Thread.sleep(1500);
                    } catch (Exception error) {
                        throw new IllegalStateException(error);
                    }
                });
            } catch (Throwable error) {
                firstError.set(error);
            } finally {
                firstDone.countDown();
            }
        });
        holder.start();
        assertThat(firstDone.await(30, TimeUnit.SECONDS)).isTrue();
        ResultActions result = mockMvc.perform(second);
        holder.join(30_000);
        assertThat(firstError.get()).isNull();
        return result;
    }

    /** 账单状态、金额与各类调整（类型、金额、状态；退款与冲销带不带台账项）汇成一行，供并发与顺序两种执行比对。 */
    private String billState(String bill) {
        return jdbc.queryForObject("""
                SELECT b.payment_status || '|' || b.settlement_status || '|' || b.payable_amount_cent || '|' || b.paid_amount_cent || '|'
                       || coalesce((SELECT string_agg(a.adjustment_type || ':' || a.amount_cent || ':' || a.status || ':'
                                              || (a.ar_ap_item_id IS NOT NULL), ',' ORDER BY a.adjustment_type, a.amount_cent)
                                      FROM bar_bill_adjustments a WHERE a.bill_id = b.id), '')
                  FROM bar_bills b WHERE b.id = ?""", String.class, bill);
    }

    private String data(ResultActions result, String pointer) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString()).at(pointer).asText();
    }
}
