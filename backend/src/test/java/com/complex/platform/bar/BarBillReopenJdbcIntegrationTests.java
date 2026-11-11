package com.complex.platform.bar;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerStoredValueAccountRecord;
import com.complex.platform.finance.FinanceRepository;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
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
 * 第十八批 AA 段第 202 条 202-R1（真库、真接口、真实授权链，请求按后台订单页蛇形键）：BSM:1069「反结（重开已结账单）、沽清（卖完、临时下架）
 * 均需主管审批留痕」；ENUM:111「高危动作（需审批/双证据）」行「反结、沽清、免单、账单调整、存酒作废（照片+视频）。」；
 * 第十八批裁决 39「反结生效时储值部分当场回原卡；现金、POS 等收款记录作废，收银按更正后的账单重新收款」；
 * 第十八批裁决 45「已登记的 500 退款照常由财务付出；反结只把没退的 2500 收款作废，再按更正后的账单重收。与第十七批「作废并冲正」遇到已退款时的处理一致，账和钱都不重复。」
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarBillReopenJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private BarRepository bars;
    @Autowired private CustomerRepository customers;
    @Autowired private FinanceRepository financeRepository;
    @Autowired private BusinessDayResolver businessDays;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String bar = "bar_rb_" + s;
    private final String table = "table_rb_" + s;
    private final String snack = "product_rb_" + s;
    private final String cashierId = "usr_rb_cash_" + s;
    private final String card = "svacc_rb_" + s;
    private String phone;
    private RequestPostProcessor cashier;
    private RequestPostProcessor supervisor;
    private RequestPostProcessor viewer;
    private RequestPostProcessor refunder;
    private RequestPostProcessor resolver;

    @BeforeEach
    void seed() throws Exception {
        String project = "prj_rb_" + s;
        String company = "ent_rb_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','反结综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())", company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'反结酒吧','bar',?,?,?,?,'active',now(),now())", bar, company, company, company, project);
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) VALUES(?,?,?,'散台区','unit','active',now(),now())",
                "space_rb_" + s, project, bar);
        Instant now = Instant.now();
        bars.saveProduct(new BarProductRecord(snack, bar, bar, "果盘", "snack", 300L, "CNY", null, null, false, "active",
                now, now, null, null, null, false, null, null, null));
        bars.saveTable(new BarTableRecord(table, bar, bar, "T-rb-" + s, "散台", "loose_table", 4, 0, 0L, "CNY", "opened", now, now));
        bars.assignTableSpace(table, "space_rb_" + s);
        bars.saveTableSession(new BarTableSessionRecord("session_rb_" + s, bar, bar, LocalDate.now(), table, null, null, 2,
                0L, 0, null, null, "walk_in", "open", now, null, null, 1L, now, now));
        jdbc.update("""
                INSERT INTO bar_payroll_rules(id, rule_kind, project_id, merchant_id, effective_from, version, rule_json, reason, status,
                    platform_company_id, created_by)
                VALUES (?, 'charge', ?, NULL, ?, 'V0.01', ?::jsonb, '服务费 0', 'approved', ?, 'usr_rb')
                """, "bpr_rb_" + s, project, java.sql.Date.valueOf(LocalDate.now().minusDays(1)),
                json.writeValueAsString(new BarPayrollRequests.RuleValues(0, 0, 2000, null, null, null, List.of())), company);
        // 客人在本酒吧的储值卡 8000 分。
        phone = "139" + String.format("%08d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000L));
        customers.saveCustomer(new CustomerRecord("cust_rb_" + s, phone, "139****" + phone.substring(7), "active", null, null, now, now));
        customers.saveStoredValueAccount(new CustomerStoredValueAccountRecord(card, "cust_rb_" + s, null, bar, 8_000L,
                "active", now, now, "merchant_prepaid", 8_000L, 0L));
        // 收银：下单、生成账单、登记收款、反结；也持主管复核审批码，用来验证不能批自己发起的反结。本酒吧主管为收银的直接上级。
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        cashier = staffs.staff(cashierId, "staff", project, bar, "bar.order.create", "finance.payment.register", "bar.bill.reopen",
                "governance.supervisor_review.approve");
        String supervisorId = "usr_rb_sup_" + s;
        supervisor = staffs.staff(supervisorId, "staff", project, bar,
                "governance.supervisor_review.view", "governance.supervisor_review.approve", "governance.supervisor_review.manage");
        viewer = staffs.staff("usr_rb_view_" + s, "staff", project, bar, "bar.bill.split_merge");
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id) VALUES (?, ?)", cashierId, supervisorId);
        // 第十八批裁决 45 用例：持 bar.bill.refund 的员工登记退款（直接上级同为本酒吧主管，本域自助退款额度 AUD-007），主管处置异常账单。
        String refunderId = "usr_rb_ref_" + s;
        refunder = staffs.staff(refunderId, "staff", project, bar, "bar.bill.adjust", "bar.bill.refund");
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id) VALUES (?, ?)", refunderId, supervisorId);
        jdbc.update("INSERT INTO refund_authority_limits (id, subject_type, subject_id, refund_domain, limit_amount_cent)"
                + " VALUES (?, 'staff', ?, 'bar', 100000000)", "ral_rb_" + s, refunderId);
        resolver = staffs.staff("usr_rb_res_" + s, "staff", project, bar, "bar.bill.anomaly.resolve");
    }

    /**
     * 储值 1000＋现金 2000 结清的账单：首次反结 409 ADMISSION_BLOCKED 建主管复核单、账单与卡都不动；发起人不能批自己的单；直属上级批准后原样再提交，
     * 账单回到未付、原收款全部冲正，储值 1000 当场回原卡、现金不挂退款应付；同一反结重放不重复退卡；批准只用一次；重新结账选储值只扣一次。
     */
    @Test
    void reopenNeedsSupervisorApprovalAndReturnsStoredValueToCard() throws Exception {
        String order = read(order(10).andExpect(status().isOk()), "$.data.id");
        String bill = read(call(cashier, "/api/v1/bar/orders/" + order + "/bill", Map.of("idempotency_key", "rb-bill-" + s))
                .andExpect(status().isOk()), "$.data.id");
        settle(bill, "stored_value", 1_000L).andExpect(status().isOk());
        settle(bill, "cash", 2_000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.settlement_status").value("settled"));
        assertThat(cardBalance()).isEqualTo(7_000L);

        String blocked = reopen(bill, "rb-reopen-1-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED")).andReturn().getResponse().getContentAsString();
        List<String> pending = jdbc.queryForList("SELECT review_id FROM supervisor_review_decisions WHERE action_type = 'bar.bill_reopen'"
                + " AND object_id = ? AND review_status = 'pending_review'", String.class, bill);
        assertThat(pending).hasSize(1);
        assertThat(blocked).contains(pending.get(0));
        assertThat(billRow(bill)).containsEntry("settlement_status", "settled").containsEntry("reopen_count", 0);
        assertThat(operations(bill)).isZero();
        assertThat(cardBalance()).isEqualTo(7_000L);

        approve(cashier, pending.get(0)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        mockMvc.perform(post("/api/v1/platform/supervisor-review/" + pending.get(0) + "/chain/regenerate").with(supervisor))
                .andExpect(status().isOk());
        approve(supervisor, pending.get(0)).andExpect(status().isOk());

        String key = "rb-reopen-2-" + s;
        reopen(bill, key).andExpect(status().isOk()).andExpect(jsonPath("$.data.operation_type").value("reopen"));
        assertThat(jdbc.queryForMap("SELECT operation_type, reason, requested_by_user_id FROM bar_bill_operations WHERE primary_bill_id = ?", bill))
                .containsEntry("operation_type", "reopen").containsEntry("reason", "客人追加消费，重开改正后再结")
                .containsEntry("requested_by_user_id", cashierId);
        // 第十八批 AG 段 232-R1 ③：反结原因同时存进账单的反结原因。
        assertThat(jdbc.queryForObject("SELECT reopen_reason FROM bar_bills WHERE id = ?", String.class, bill))
                .isEqualTo("客人追加消费，重开改正后再结");
        assertThat(billRow(bill)).containsEntry("reopened", true).containsEntry("reopen_count", 1)
                .containsEntry("payment_status", "unpaid").containsEntry("settlement_status", "unsettled").containsEntry("paid_amount_cent", 0L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_bill_settlements WHERE bill_id = ? AND NOT reversed", Integer.class, bill)).isZero();
        // 裁决 39：储值部分当场回原卡；现金收款记录作废、不挂退款应付。
        assertThat(cardBalance()).isEqualTo(8_000L);
        assertThat(cardReversals()).containsExactly(1_000L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE direction = 'payable' AND source_id = ?", Integer.class, bill))
                .isZero();
        mockMvc.perform(get("/api/v1/bar/bills/" + bill + "/operations").with(viewer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].operation_type").value(hasItem("reopen")));

        reopen(bill, key).andExpect(status().isOk());
        assertThat(cardBalance()).isEqualTo(8_000L);
        assertThat(cardReversals()).containsExactly(1_000L);
        reopen(bill, "rb-reopen-3-" + s).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED"));
        assertThat(billRow(bill)).containsEntry("reopen_count", 1);

        settle(bill, "stored_value", 3_000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.settlement_status").value("settled"));
        assertThat(cardBalance()).isEqualTo(5_000L);
    }

    /**
     * 第十八批裁决 45：现金 3000 结清、员工登记退款 500（财务未实付，账单仍为已付）后经主管批准反结，再按更正后的账单收现金 2500。
     * 反结只作废没退的 2500，被已退 500 占用的收款留存不冲正：日结＝2500，未冲正收款＝留存 500＋新收 2500，退款应付仍只有原来那 500。
     */
    @Test
    void reopenAfterRegisteredCashRefundReversesOnlyUnrefundedCollection() throws Exception {
        String bill = cashRefundedReopenedAndResettled();
        assertThat(dayNet()).isEqualTo(2_500L);
        assertThat(collections(bill)).containsExactly(500L, 2_500L);
        assertThat(refundPayables(bill)).containsExactly(500L);
        assertThat(billRow(bill)).containsEntry("payment_status", "paid").containsEntry("paid_amount_cent", 2_500L);
    }

    /** 接上一条：重结后「作废并冲正」，新挂原路待退＝新收的 2500（原 500 退款照常由财务付出），作废后该账单合计为 0、日结不重复冲减。 */
    @Test
    void voidAfterReopenAndResettleRefundsNewCollectionOnly() throws Exception {
        String bill = cashRefundedReopenedAndResettled();
        call(refunder, "/api/v1/bar/bills/" + bill + "/anomaly", Map.of("reason", "客人称整单不是本桌消费", "idempotency_key", "rb-anomaly-" + s))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("abnormal"));
        call(resolver, "/api/v1/bar/bills/" + bill + "/anomaly/resolve", Map.of("resolution", "voided_reversed",
                "reason", "核实整单记错，作废并退款", "idempotency_key", "rb-void-" + s))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("void"));
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ar_ap_items WHERE direction = 'payable' AND source_type = 'refund' AND source_id = ?",
                Long.class, bill)).isEqualTo(2_500L);
        assertThat(refundPayables(bill)).containsExactlyInAnyOrder(500L, 2_500L);
        assertThat(collections(bill)).containsExactly(500L);
        assertThat(dayNet()).isZero();
    }

    /**
     * 第十八批裁决 45 与裁决 39：储值 1000＋现金 2000 结清、已登记现金退款 500（后收的现金先退）后反结——储值退回原卡的是储值结算里实际被冲正的部分
     * （已退 500 占在现金上，储值 1000 整笔冲正、整笔回卡），现金留存 500；重结选储值只扣一次，卡余额、日结与客人净付一致。
     * 重结后再退新收的 2500：全额原路回卡、不另挂应付；原 500 应付财务未付，账单为部分退款，日结不重复冲减。
     */
    @Test
    void reopenAfterCashRefundReturnsOnlyReversedStoredValue() throws Exception {
        String order = read(order(10).andExpect(status().isOk()), "$.data.id");
        String bill = read(call(cashier, "/api/v1/bar/orders/" + order + "/bill", Map.of("idempotency_key", "rb-bill-" + s))
                .andExpect(status().isOk()), "$.data.id");
        settle(bill, "stored_value", 1_000L).andExpect(status().isOk());
        settle(bill, "cash", 2_000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.settlement_status").value("settled"));
        refund(bill, 500L);
        assertThat(cardBalance()).isEqualTo(7_000L);
        assertThat(refundPayables(bill)).containsExactly(500L);

        reopenApproved(bill);
        assertThat(cardReversals()).containsExactly(1_000L);
        assertThat(cardBalance()).isEqualTo(8_000L);
        assertThat(dayNet()).isZero();
        assertThat(collections(bill)).containsExactly(500L);

        settle(bill, "stored_value", 2_500L).andExpect(status().isOk()).andExpect(jsonPath("$.data.settlement_status").value("settled"));
        assertThat(cardBalance()).isEqualTo(5_500L);
        long cashKept = jdbc.queryForObject("SELECT COALESCE(sum(settlement_amount_cent), 0) FROM bar_bill_settlements"
                + " WHERE bill_id = ? AND settle_channel = 'cash' AND NOT reversed", Long.class, bill);
        // 客人净付＝储值净扣＋现金净收（原 2000 反结时当场退回没退的 1500）－已登记的现金退款 500。
        long customerNet = (8_000L - cardBalance()) + cashKept - 500L;
        assertThat(customerNet).isEqualTo(2_500L);
        assertThat(dayNet()).isEqualTo(customerNet);

        refund(bill, 2_500L);
        assertThat(cardBalance()).isEqualTo(8_000L);
        assertThat(refundPayables(bill)).containsExactly(500L);
        assertThat(billRow(bill)).containsEntry("payment_status", "partial_refunded");
        assertThat(dayNet()).isZero();
    }

    // ---------- helpers ----------

    /** 后台订单页「下单」：bar_id、table_id、items[{product_id, quantity}]、idempotency_key。 */
    private ResultActions order(int quantity) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "rb-order-" + UUID.randomUUID());
        body.put("bar_id", bar);
        body.put("table_id", table);
        body.put("items", List.of(Map.of("product_id", snack, "quantity", quantity)));
        return call(cashier, "/api/v1/bar/orders", body);
    }

    /** 后台订单页结账：储值须带客人手机号与短信验证码（测试环境固定验证码）。 */
    private ResultActions settle(String bill, String channel, long amountCent) throws Exception {
        String key = "rb-settle-" + UUID.randomUUID();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        body.put("payment_id", key);
        body.put("settlement_amount_cent", amountCent);
        body.put("payment_channel", channel);
        if ("stored_value".equals(channel)) {
            body.put("phone", phone);
            body.put("sms_code", "000000");
        }
        return call(cashier, "/api/v1/bar/bills/" + bill + "/settle", body);
    }

    private ResultActions reopen(String bill, String key) throws Exception {
        return call(cashier, "/api/v1/bar/bills/" + bill + "/reopen", Map.of("idempotency_key", key, "reason", "客人追加消费，重开改正后再结"));
    }

    private ResultActions approve(RequestPostProcessor who, String reviewId) throws Exception {
        return call(who, "/api/v1/platform/supervisor-review/" + reviewId + "/approve", Map.of(
                "idempotency_key", "rb-approve-" + UUID.randomUUID(), "decision", "approved", "comment", "核实需重开"));
    }

    private ResultActions call(RequestPostProcessor who, String path, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post(path).with(who).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private static String read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }

    private Map<String, Object> billRow(String bill) {
        return jdbc.queryForMap("SELECT settlement_status, payment_status, paid_amount_cent, reopened, reopen_count FROM bar_bills WHERE id = ?", bill);
    }

    private int operations(String bill) {
        return jdbc.queryForObject("SELECT count(*) FROM bar_bill_operations WHERE primary_bill_id = ?", Integer.class, bill);
    }

    private long cardBalance() {
        return jdbc.queryForObject("SELECT balance_cent FROM customer_stored_value_accounts WHERE id = ?", Long.class, card);
    }

    private List<Long> cardReversals() {
        return jdbc.queryForList("SELECT amount_cent FROM customer_stored_value_transactions WHERE account_id = ? AND txn_type = 'consume_reversal'",
                Long.class, card);
    }

    /** 现金 3000 结清 → 持 bar.bill.refund 的员工登记退款 500（财务未实付，账单仍为已付）→ 主管批准后反结 → 按更正后的账单收现金 2500。 */
    private String cashRefundedReopenedAndResettled() throws Exception {
        String order = read(order(10).andExpect(status().isOk()), "$.data.id");
        String bill = read(call(cashier, "/api/v1/bar/orders/" + order + "/bill", Map.of("idempotency_key", "rb-bill-" + s))
                .andExpect(status().isOk()), "$.data.id");
        settle(bill, "cash", 3_000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.settlement_status").value("settled"));
        refund(bill, 500L);
        assertThat(refundPayables(bill)).containsExactly(500L);
        assertThat(billRow(bill)).containsEntry("payment_status", "paid");
        reopenApproved(bill);
        assertThat(billRow(bill)).containsEntry("payment_status", "unpaid").containsEntry("paid_amount_cent", 0L);
        settle(bill, "cash", 2_500L).andExpect(status().isOk()).andExpect(jsonPath("$.data.settlement_status").value("settled"));
        return bill;
    }

    /** 反结：首次提交 409 建主管复核单，直属上级批准后原样再提交生效。 */
    private void reopenApproved(String bill) throws Exception {
        String key = "rb-reopen-" + UUID.randomUUID();
        reopen(bill, key).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED"));
        approvePending("bar.bill_reopen", bill);
        reopen(bill, key).andExpect(status().isOk());
    }

    /** 后台订单页「退款」（按商品明细）：持 bar.bill.refund 的员工首次提交 409 建主管复核单，直属上级批准后原样再提交。 */
    private void refund(String bill, long amountCent) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "rb-refund-" + UUID.randomUUID());
        body.put("adjustment_type", "refund");
        body.put("amount_cent", amountCent);
        body.put("reason", "客人退回果盘");
        body.put("evidence_attachment_ids", List.of("att_rb_refund"));
        body.put("refund_lines", List.of(Map.of("source_bill_id", bill, "component_type", "goods", "amount_cent", amountCent)));
        call(refunder, "/api/v1/bar/bills/" + bill + "/adjustments", body).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED"));
        approvePending("bar.bill_waiver", bill);
        String adjustment = read(call(refunder, "/api/v1/bar/bills/" + bill + "/adjustments", body).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adjustment_type").value("refund")), "$.data.id");
        // 第十八批 AG 段 232-R3 ③：证据附件编号随本次调整记录保存。
        assertThat(jdbc.queryForList("SELECT jsonb_array_elements_text(evidence_attachment_ids) FROM bar_bill_adjustments WHERE id = ?",
                String.class, adjustment)).containsExactly("att_rb_refund");
    }

    private void approvePending(String action, String bill) throws Exception {
        String review = jdbc.queryForObject("SELECT review_id FROM supervisor_review_decisions WHERE action_type = ? AND object_id = ?"
                + " AND review_status = 'pending_review'", String.class, action, bill);
        mockMvc.perform(post("/api/v1/platform/supervisor-review/" + review + "/chain/regenerate").with(supervisor))
                .andExpect(status().isOk());
        approve(supervisor, review).andExpect(status().isOk());
    }

    /** 本单未冲正的收款流水金额（按收款时点）。 */
    private List<Long> collections(String bill) {
        return jdbc.queryForList("SELECT settlement_amount_cent FROM bar_bill_settlements WHERE bill_id = ? AND NOT reversed ORDER BY settled_at, id",
                Long.class, bill);
    }

    /** 本单挂出的退款应付：各笔退款调整的应付与作废原路待退（source_id＝账单）。 */
    private List<Long> refundPayables(String bill) {
        return jdbc.queryForList("SELECT amount_cent FROM ar_ap_items WHERE direction = 'payable' AND source_type = 'refund'"
                + " AND (source_id = ? OR id IN (SELECT ar_ap_item_id FROM bar_bill_adjustments WHERE bill_id = ?)) ORDER BY created_at, id",
                Long.class, bill, bill);
    }

    /** 该营业日酒吧日结口径（商户口径，同日结取数 FinanceRepository.aggregateBar）：未冲正收款合计减按退款时点计的退款。 */
    private long dayNet() {
        LocalDate day = businessDays.businessDate(null, bar, Instant.now());
        FinanceRepository.DailyCloseSourceTotals totals = financeRepository.aggregateBar(day, businessDays.start(null, bar, day),
                businessDays.end(null, bar, day), "merchant", bar);
        return totals.amountCent() - totals.refundCent();
    }
}
