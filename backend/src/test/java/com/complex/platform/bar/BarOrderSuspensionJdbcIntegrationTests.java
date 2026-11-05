package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 T 段第 157 条 157-R2（真库）：第十八批裁决 37「散台、卡座下单必须当场付款、不能挂起」，追问题面「只有包厢（系统里叫「贵宾包座」）
 * 可以挂起」；BSM:1105「状态为已挂起待付款、已结清……禁止无日志删除。」；BSM:1106「收银员可以直接挂起，不设单笔金额和当班累计金额上限；
 * 必须填写原因并记录金额、桌台、责任人、操作时间和完整日志。未结清订单阻止交班和日结。」；BSM:1107「已出品挂起订单取消须经理审批」；
 * BSM:1109「不可回库的已出品商品不得走普通取消」；PGM:699「全部动作强制留痕」。
 * 请求按后台订单页真实形状（蛇形键、X-Terminal-Type: web）经控制器与真实授权链提交。
 * 失败的请求会把测试事务标成只回滚，之后班结的准入闸（保存点）即按失败关闭——要班结成功的用例先班结、再发预期失败的请求。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarOrderSuspensionJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private BarRepository bars;

    private String s;
    private String project;
    private String bar;
    private String wine;
    private String champagne;
    private String cashierId;
    private String memberId;
    private String waiterId;
    private RequestPostProcessor cashier;
    private RequestPostProcessor member;
    private RequestPostProcessor supervisor;
    private RequestPostProcessor noHang;
    private RequestPostProcessor otherBarCashier;

    @BeforeEach
    void seed() throws Exception {
        s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        project = "prj_hang_" + s;
        String company = "ent_hang_" + s;
        bar = "bar_hang_" + s;
        String otherBar = "bar_hang_other_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','挂起订单综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())", company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'挂起订单酒吧','bar',?,?,?,?,'active',now(),now()),(?,'另一酒吧','bar',?,?,?,?,'active',now(),now())",
                bar, company, company, company, project, otherBar, company, company, company, project);
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) VALUES(?,?,?,'包厢区','unit','active',now(),now())",
                "space_hang_" + s, project, bar);
        // 班结过合同准入闸：本酒吧有一份含酒吧业务的生效合同。
        jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,"
                + "lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,"
                + "'effective','[\"bar\"]'::jsonb,now(),now())", "hang_ct_" + s, bar, bar, bar);
        // 卡座、包厢开账单要有该营业日已批准的收费规则（服务费率取 0，账单金额即消费额）。
        jdbc.update("""
                INSERT INTO bar_payroll_rules(id, rule_kind, project_id, merchant_id, effective_from, version, rule_json, reason, status,
                    platform_company_id, created_by)
                VALUES (?, 'charge', ?, NULL, ?, 'V0.01', ?::jsonb, '服务费 0', 'approved', ?, 'usr_hang')
                """, "bpr_hang_" + s, project, java.sql.Date.valueOf(LocalDate.now().minusDays(1)),
                json.writeValueAsString(new BarPayrollRequests.RuleValues(0, 0, 2000, null, null, null, List.of())), company);
        Instant now = Instant.now();
        wine = "product_hang_wine_" + s;
        champagne = "product_hang_champagne_" + s;
        bars.saveProduct(new BarProductRecord(wine, bar, bar, "干红", "wine", 30000L, "CNY", null, null, false, "active",
                now, now, null, null, null, false, null, null, null));
        bars.saveProduct(new BarProductRecord(champagne, bar, bar, "黑桃A香槟", "wine", 1_000_000L, "CNY", null, null, false, "active",
                now, now, null, null, null, false, null, null, null));
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        cashierId = "usr_hang_cashier_" + s;
        memberId = "usr_hang_member_" + s;
        waiterId = "usr_hang_waiter_" + s;
        cashier = staffs.staff(cashierId, "staff", project, bar, "bar.order.create", "finance.payment.register", "bar.bill.hang",
                "bar.shift.manage", "bar.shift.close", "bar.bill.adjust", "bar.bill.split_merge");
        member = staffs.staff(memberId, "staff", project, bar, "bar.order.create", "bar.bill.hang", "bar.shift.manage");
        staffs.staff(waiterId, "staff", project, bar, "bar.order.view");
        supervisor = staffs.staff("usr_hang_sup_" + s, "staff", project, bar, "bar.bill.anomaly.resolve");
        noHang = staffs.staff("usr_hang_nohang_" + s, "staff", project, bar, "bar.order.create");
        otherBarCashier = staffs.staff("usr_hang_other_" + s, "staff", project, otherBar, "bar.bill.hang");
    }

    /**
     * 收银员挂起包厢未付款单：记金额、桌台、主责服务员、挂起人与所在班次，审计带原因；同键重放同结果。未挂起的包厢未付账单不挡交班；
     * 部分收款仍是已挂起待付款，收齐即已结清并留痕，本班次随即可交班。之后换键再挂 409。
     */
    @Test
    void cashierSuspendsVipOrderAndSettlementReleasesShift() throws Exception {
        String shift = openShift(cashier, cashierId, "main");
        String table = table("vip", "vip");
        String session = openSession("vip", table);
        responsible(session, table, waiterId);
        String order = orderId(order(table, wine, 2));
        String bill = bill(order, "b1");
        deliver(order, "d1").andExpect(status().isOk());
        long due = jdbc.queryForObject("SELECT payable_amount_cent + tip_amount_cent - paid_amount_cent FROM bar_bills WHERE id = ?",
                Long.class, bill);
        assertThat(due).isEqualTo(60000L);

        String key = "hang-s1-" + s;
        String id = data(suspend(cashier, order, "客人稍后付款", key).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("hanging_unpaid"))
                .andExpect(jsonPath("$.data.amount_cent").value(60000))
                .andExpect(jsonPath("$.data.table_id").value(table))
                .andExpect(jsonPath("$.data.bill_id").value(bill))
                .andExpect(jsonPath("$.data.responsible_user_id").value(waiterId))
                .andExpect(jsonPath("$.data.suspended_by").value(cashierId))
                .andExpect(jsonPath("$.data.shift_id").value(shift))
                .andExpect(jsonPath("$.data.reason").value("客人稍后付款"))
                .andExpect(jsonPath("$.data.suspended_at").isNotEmpty()), "$.data.id");
        Map<String, Object> event = jdbc.queryForMap("SELECT metadata->>'reason' AS reason, metadata->>'amount_cent' AS amount,"
                + " metadata->>'table_id' AS table_id, metadata->>'responsible_user_id' AS responsible FROM audit_events"
                + " WHERE event_name = 'bar.order_suspended' AND business_id = ?", id);
        assertThat(event).containsEntry("reason", "客人稍后付款").containsEntry("amount", "60000")
                .containsEntry("table_id", table).containsEntry("responsible", waiterId);
        suspend(cashier, order, "客人稍后付款", key).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(id));

        bill(orderId(order(table, wine, 1)), "unsuspended");
        settle(bill, 20000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.settlement_status").value("partial_settled"));
        assertThat(suspensionStatus(order)).isEqualTo("hanging_unpaid");
        settle(bill, 40000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.settlement_status").value("settled"));
        assertThat(suspensionStatus(order)).isEqualTo("settled");
        assertThat(jdbc.queryForObject("SELECT settled_at IS NOT NULL FROM bar_order_suspensions WHERE id = ?", Boolean.class, id)).isTrue();
        assertThat(events("bar.order_suspension_settled", id)).isEqualTo(1);
        close(cashier, shift, "main").andExpect(status().isOk());

        suspend(cashier, order, "再次挂起", "hang-s2-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_ORDER_ALREADY_SUSPENDED"));
    }

    /** 加入同一钱箱的收银员挂起的单记在该班次；部分收款后仍未结清，交班 409 并列出账单号。 */
    @Test
    void memberSuspensionStillUnsettledBlocksShiftClose() throws Exception {
        String shift = openShift(cashier, cashierId, "m");
        mockMvc.perform(post("/api/v1/bar/shifts/" + shift + "/join").with(member).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "hang-join-" + s)))).andExpect(status().isOk());
        String table = table("vipm", "vip");
        openSession("vipm", table);
        String order = orderId(order(table, wine, 1));
        String bill = bill(order, "m");
        suspend(member, order, "客人去取钱", "hang-m-" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shift_id").value(shift))
                .andExpect(jsonPath("$.data.responsible_user_id").value(org.hamcrest.Matchers.nullValue()));
        settle(bill, 10000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.settlement_status").value("partial_settled"));
        String billNo = jdbc.queryForObject("SELECT bill_no FROM bar_bills WHERE id = ?", String.class, bill);
        close(cashier, shift, "m").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_SHIFT_SUSPENDED_ORDER_UNSETTLED"))
                .andExpect(jsonPath("$.message").value(containsString(billNo)));
    }

    /** 整单赠送（应付 0）结账关单不收款，同样把挂起置已结清。 */
    @Test
    void zeroPayableSettlementSettlesSuspension() throws Exception {
        String table = table("vipz", "vip");
        openSession("vipz", table);
        Map<String, Object> gift = new LinkedHashMap<>();
        gift.put("idempotency_key", "hang-gift-" + s);
        gift.put("bar_id", bar);
        gift.put("table_id", table);
        gift.put("items", List.of(Map.of("product_id", wine, "quantity", 1, "is_gift", true)));
        String order = orderId(mockMvc.perform(post("/api/v1/bar/orders").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(gift))));
        String bill = bill(order, "z");
        suspend(cashier, order, "客人投诉待处理", "hang-z-" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount_cent").value(0));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "hang-zero-" + s);
        body.put("payment_id", "hang-zero-" + s);
        body.put("payment_channel", "cash");
        mockMvc.perform(post("/api/v1/bar/bills/" + bill + "/settle").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlement_status").value("settled"));
        assertThat(suspensionStatus(order)).isEqualTo("settled");
    }

    /** 合单：副单所属订单的挂起改指主单，主单结清时该挂起置已结清。 */
    @Test
    void mergedBillSettlementSettlesSuspension() throws Exception {
        String table = table("vipg", "vip");
        openSession("vipg", table);
        String first = orderId(order(table, wine, 1));
        String primary = bill(first, "g1");
        String second = orderId(order(table, wine, 1));
        String secondary = bill(second, "g2");
        suspend(cashier, second, "客人稍后付款", "hang-g-" + s).andExpect(status().isOk());
        Map<String, Object> merge = new LinkedHashMap<>();
        merge.put("idempotency_key", "hang-merge-" + s);
        merge.put("merged_bill_id", secondary);
        merge.put("reason", "同包厢合并结账");
        mockMvc.perform(post("/api/v1/bar/bills/" + primary + "/merge").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(merge))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT bill_id FROM bar_order_suspensions WHERE order_id = ?", String.class, second))
                .isEqualTo(primary);
        settle(primary, 60000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.settlement_status").value("settled"));
        assertThat(suspensionStatus(second)).isEqualTo("settled");
    }

    /** 挂起中的订单账单不能经异常处置「作废并冲正」（须走经理审批取消或确认坏账）；「更正后恢复」不受影响。 */
    @Test
    void voidReversalRejectedWhileSuspended() throws Exception {
        String table = table("vipa", "vip");
        openSession("vipa", table);
        String order = orderId(order(table, wine, 1));
        String bill = bill(order, "a");
        suspend(cashier, order, "客人稍后付款", "hang-a-" + s).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/bar/bills/" + bill + "/anomaly").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "hang-raise-" + s, "reason", "客人称少点一瓶"))))
                .andExpect(status().isOk());
        resolve(bill, "voided_reversed", "v").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_ORDER_SUSPENDED_USE_CANCEL"));
        assertThat(jdbc.queryForObject("SELECT payment_status FROM bar_bills WHERE id = ?", String.class, bill)).isEqualTo("abnormal");
        resolve(bill, "restored", "r").andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("unpaid"));
        assertThat(suspensionStatus(order)).isEqualTo("hanging_unpaid");
    }

    /** 只有包厢能挂起；原因必填；无挂起权限 403；跨商户 403 且不落幂等；不设金额上限。 */
    @Test
    void onlyVipWithReasonPermissionAndScope() throws Exception {
        for (String type : List.of("card_seat", "loose_table")) {
            String table = table(type, type);
            openSession(type, table);
            suspend(cashier, orderId(order(table, wine, 1)), "客人稍后付款", "hang-" + type + "-" + s).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail_code").value("BAR_ORDER_SUSPEND_VIP_ONLY"));
        }
        String counter = table("counter", "bar_counter");
        suspend(cashier, orderId(order(counter, wine, 1)), "客人稍后付款", "hang-counter-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_ORDER_SUSPEND_VIP_ONLY"));

        String vip = table("vipr", "vip");
        openSession("vipr", vip);
        String order = orderId(order(vip, champagne, 1));
        bill(order, "r");
        suspend(cashier, order, "", "hang-blank-" + s).andExpect(status().isBadRequest());
        suspend(noHang, order, "客人稍后付款", "hang-noperm-" + s).andExpect(status().isForbidden());
        String crossKey = "hang-cross-" + s;
        suspend(otherBarCashier, order, "客人稍后付款", crossKey).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM idempotency_records WHERE idempotency_key = ?", Integer.class, crossKey))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_order_suspensions WHERE order_id = ?", Integer.class, order)).isZero();
        suspend(cashier, order, "客人稍后付款", "hang-big-" + s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount_cent").value(1_000_000));
    }

    // ---------- helpers ----------

    private String table(String tag, String type) {
        String id = "table_hang_" + tag + "_" + s;
        Instant now = Instant.now();
        bars.saveTable(new BarTableRecord(id, bar, bar, "T-" + tag + "-" + s, tag, type, 4, 0, 0L, "CNY", "opened", now, now));
        bars.assignTableSpace(id, "space_hang_" + s);
        return id;
    }

    private String openSession(String tag, String table) {
        Instant now = Instant.now();
        String id = "session_hang_" + tag + "_" + s;
        bars.saveTableSession(new BarTableSessionRecord(id, bar, bar, LocalDate.now(), table, null, null, 2,
                0L, 0, null, null, "walk_in", "open", now, null, null, 1L, now, now));
        return id;
    }

    /** 开台会话此刻的主责服务员区间。 */
    private void responsible(String session, String table, String userId) {
        jdbc.update("INSERT INTO bar_service_responsibilities(id,session_id,table_id,project_id,merchant_id,user_id,payer_company_id,"
                + "started_at,source_id,recorded_by,reason) VALUES(?,?,?,?,?,?,?,now() - interval '1 hour',?,?,'开台确认责任人')",
                "bsr_hang_" + s, session, table, project, bar, userId, "ent_hang_" + s, "open:" + session, cashierId);
    }

    private ResultActions order(String table, String product, int quantity) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "hang-order-" + UUID.randomUUID());
        body.put("bar_id", bar);
        body.put("table_id", table);
        body.put("items", List.of(Map.of("product_id", product, "quantity", quantity)));
        return mockMvc.perform(post("/api/v1/bar/orders").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    private String bill(String order, String tag) throws Exception {
        return data(mockMvc.perform(post("/api/v1/bar/orders/" + order + "/bill").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "hang-bill-" + tag + "-" + s)))).andExpect(status().isOk()),
                "$.data.id");
    }

    private ResultActions deliver(String order, String tag) throws Exception {
        List<Map<String, Object>> lines = jdbc.queryForList("SELECT id, quantity FROM bar_order_items WHERE order_id = ?", order).stream()
                .map(row -> Map.<String, Object>of("order_item_id", row.get("id"), "quantity", row.get("quantity"))).toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "hang-deliver-" + tag + "-" + s);
        body.put("operator_note", "已送达");
        body.put("delivery_photo_attachment_ids", List.of());
        body.put("delivered_items", lines);
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/deliver").with(cashier)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 后台订单页「挂起」：体 reason、idempotency_key。 */
    private ResultActions suspend(RequestPostProcessor who, String order, String reason, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/orders/" + order + "/suspend").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", key, "reason", reason))));
    }

    private ResultActions settle(String bill, long amountCent) throws Exception {
        String key = "hang-settle-" + UUID.randomUUID();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("payment_id", key);
        body.put("settlement_amount_cent", amountCent);
        body.put("payment_channel", "cash");
        return mockMvc.perform(post("/api/v1/bar/bills/" + bill + "/settle").with(cashier)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions resolve(String bill, String resolution, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/bills/" + bill + "/anomaly/resolve").with(supervisor).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "hang-resolve-" + tag + "-" + s, "resolution", resolution,
                        "reason", "核实后处置"))));
    }

    private String openShift(RequestPostProcessor who, String cashierUserId, String tag) throws Exception {
        return data(mockMvc.perform(post("/api/v1/bar/shifts/open").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "hang-open-" + tag + "-" + s, "bar_id", bar,
                        "cashier_user_id", cashierUserId, "opening_cash_cent", 0)))).andExpect(status().isOk()), "$.data.id");
    }

    private ResultActions close(RequestPostProcessor who, String shift, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/bar/shifts/" + shift + "/close").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "hang-close-" + tag + "-" + s, "declared_cash_cent", 60000))));
    }

    private String orderId(ResultActions result) throws Exception {
        return data(result.andExpect(status().isOk()), "$.data.id");
    }

    private static String data(ResultActions result, String path) throws Exception {
        return CustomerTicketPaymentFixture.read(result, path);
    }

    private String suspensionStatus(String order) {
        return jdbc.queryForObject("SELECT status FROM bar_order_suspensions WHERE order_id = ?", String.class, order);
    }

    private int events(String event, String businessId) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = ? AND business_id = ?", Integer.class,
                event, businessId);
    }
}
