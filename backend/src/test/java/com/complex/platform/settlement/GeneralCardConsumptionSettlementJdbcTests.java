package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.LocalZone;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.StoredValueService;
import com.complex.platform.support.AppendOnlyTestBypass;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.InternalPayDayFixtures;
import com.complex.platform.support.StaffRequestFixture;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
 * 第十八批 AZ 段第 350 条 350-R1：通用卡购票按营业月合并挂长沙项目公司对票款收款主体的应付，跨月退款另挂追收应收。依据：BSM:1704「长沙项目公司就通用卡余额的
 * 实际消费向各业态的经营主体直接付款结算，按三个付账日结清」；ENUM:86「购买通票按通票规则与娱乐平台公司结算，购买游乐与游乐公司结算，购买演出与传媒公司结算」
 * 「充值赠送金额与赠送权益的成本一律由长沙项目公司承担」「相应消费结算的通路费调整关联原消费批次及原费率依据」；BSM:749「只扣一次」；BSM:1673「月结定格」；
 * BSM:755「收支两条线，不得轧差」；第十八批裁决 69「付款方是平台主体的应收，登记收款前也须付款确认」。
 *
 * <p>夹具随测试事务回滚：每张通用卡先充一批 20000 分本金（通路费率 1%），客人经真实客人端接口（蛇形键）用通用卡买一张或两张游乐票（每张 5000 分），
 * 付款即出票、按该笔批次分摊固化通路费（每 5000 分 50 分）。月度入账作业以付款营业月之后第 2 个月 2 日北京时间 10:00 为当前时刻直调；
 * 台账经平台财务真实接口查看与登记收款。修复前作业不入账，各用例第一条断言即红。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class GeneralCardConsumptionSettlementJdbcTests {

    private static final String SOURCE = "general_card_consume";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private StoredValueService storedValue;
    @Autowired private BusinessDayResolver businessDays;
    @Autowired private GeneralCardConsumptionSettlementJob job;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private RequestPostProcessor finance;

    @BeforeEach
    void seed() {
        InternalPayDayFixtures.seed(jdbc);
        finance = new StaffRequestFixture(jdbc).staff("usr_gcc_fin_" + s, "platform_admin", null, null,
                "finance.ar_ap.view", "finance.ar_ap.manage", "finance.payment.register");
    }

    @AfterEach
    void clear() {
        TestSecurityActors.clear();
    }

    /**
     * ①②③⑦：纯游乐票的通用卡消费挂长沙项目公司对游乐公司的应付；本单有价权益在酒吧的按通票口径挂对娱乐平台公司的应付；金额＝付额减该笔批次通路费，
     * 账期＝付款营业月，项目＝订单项目，付款时点按钱路 general_card_consume 固化。只付了一部分、未出票的消费与商户卡消费不入账；再跑一轮不重复挂账。
     */
    @Test
    void generalCardTicketPaymentsPostMonthlyPayableToTheCollectingEntity() throws Exception {
        routeRule();
        CustomerTicketPaymentFixture amusement = card();
        String amusementTxn = pay(amusement, amusement.createOrderId(1));
        String partialOrder = amusement.createOrderId(1);
        amusement.payWithStoredValue(partialOrder, 1_000L).andExpect(status().isOk());
        String partialTxn = txnOf(partialOrder);
        CustomerTicketPaymentFixture pass = card();
        String passTxn = pay(pass, pass.createOrderId(1));
        // 本单有价权益的商户业态改为酒吧：不再是纯游乐票，按通票规则与娱乐平台公司结算（ENUM:86、ENUM:32）。
        jdbc.update("UPDATE merchants SET merchant_type = 'bar' WHERE id = ?", pass.merchantId);
        CustomerTicketPaymentFixture merchantCard = card();
        String merchantTxn = pay(merchantCard, merchantCard.createOrderId(1));
        jdbc.update("UPDATE customer_stored_value_accounts SET merchant_id = ? WHERE id = ?", merchantCard.merchantId, merchantCard.accountId);
        assertThat(jdbc.queryForObject("SELECT fee_cent FROM ticket_payment_route_fees WHERE payment_intent_id = ?", Long.class, amusementTxn))
                .isEqualTo(50L);

        assertThat(job.run(runAt(amusement))).isGreaterThanOrEqualTo(2);

        String amusementItem = itemOf(amusementTxn);
        String passItem = itemOf(passTxn);
        ledger().andExpect(status().isOk())
                .andExpect(jsonPath(row(amusementItem, "source_type"), contains(SOURCE)))
                .andExpect(jsonPath(row(amusementItem, "direction"), contains("payable")))
                .andExpect(jsonPath(row(amusementItem, "counterparty_id"), contains("be_amusement")))
                .andExpect(jsonPath(row(amusementItem, "amount_cent"), contains(4_950)))
                .andExpect(jsonPath(row(amusementItem, "accounting_period"), contains(month(amusement).toString())))
                .andExpect(jsonPath(row(passItem, "direction"), contains("payable")))
                .andExpect(jsonPath(row(passItem, "counterparty_id"), contains("be_ent_platform")))
                .andExpect(jsonPath(row(passItem, "amount_cent"), contains(4_950)));
        assertThat(jdbc.queryForObject("SELECT project_id FROM ar_ap_items WHERE id = ?", String.class, amusementItem))
                .isEqualTo(amusement.projectId);
        assertThat(jdbc.queryForObject("SELECT route FROM internal_payable_schedules WHERE ar_ap_item_id = ?", String.class, amusementItem))
                .isEqualTo(SOURCE);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM general_card_consume_lines WHERE txn_id IN (?, ?)", Integer.class,
                partialTxn, merchantTxn)).isZero();
        assertThat(arApCount(amusement.projectId) + arApCount(merchantCard.projectId)).isEqualTo(1);
        // 18AZ 收尾复核（BSM:522「都必须可点击、可追溯到来源单据与操作记录」；照停车抵免成本 parking.credit_cost.posted）：入账记一条操作留痕。
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'settlement.general_card_consume.posted' "
                + "AND business_type = 'ar_ap_item' AND business_id = ?", Integer.class, amusementItem)).isEqualTo(1);

        job.run(runAt(amusement));
        assertThat(arApCount(amusement.projectId)).isEqualTo(1);
        assertThat(arApCount(pass.projectId)).isEqualTo(1);
    }

    /**
     * ④⑤：同月部分退款并进当月应付（冲减额＝退额减本次按原批次原费率冲减的通路费），不挂应收；跨月整单退款不与当月轧差——原消费营业月挂全额净额应付，
     * 退款营业月另挂一笔对同一收款主体的追收应收，登记收款前须付款确认（409 PAYMENT_CONFIRMATION_REQUIRED）。
     */
    @Test
    void sameMonthRefundNetsIntoPayableAndCrossMonthRefundIsSeparateRecovery() throws Exception {
        routeRule();
        CustomerTicketPaymentFixture same = card();
        String sameTxn = pay(same, same.createOrderId(2));
        CustomerTicketPaymentFixture cross = card();
        String crossTxn = pay(cross, cross.createOrderId(1));
        // 储值流水只追加不改（V140）：测试旁路临时摘掉触发器，把这笔消费挪回上一营业月（随测试事务回滚）。
        AppendOnlyTestBypass.updateOnTable(jdbc, "customer_stored_value_transactions", "trg_append_only_ud",
                "UPDATE customer_stored_value_transactions SET created_at = created_at - interval '1 month' WHERE id = ?", crossTxn);
        String sameReversal;
        String crossReversal;
        TestSecurityActors.asPlatformAdmin("usr_gcc_rf_" + s);
        try {
            sameReversal = storedValue.refundConsumption(sameTxn, 5_000L, "退一张票", "gcc-rf-same-" + s).id();
            crossReversal = storedValue.refundConsumption(crossTxn, 5_000L, "跨月整单退款", "gcc-rf-cross-" + s).id();
        } finally {
            TestSecurityActors.clear();
        }

        assertThat(job.run(runAt(same))).isGreaterThanOrEqualTo(3);

        String sameItem = itemOf(sameTxn);
        assertThat(itemOf(sameReversal)).isEqualTo(sameItem);
        String crossItem = itemOf(crossTxn);
        String recoveryItem = itemOf(crossReversal);
        ledger().andExpect(status().isOk())
                .andExpect(jsonPath(row(sameItem, "direction"), contains("payable")))
                .andExpect(jsonPath(row(sameItem, "amount_cent"), contains(4_950)))
                .andExpect(jsonPath(row(crossItem, "direction"), contains("payable")))
                .andExpect(jsonPath(row(crossItem, "amount_cent"), contains(4_950)))
                .andExpect(jsonPath(row(crossItem, "accounting_period"), contains(month(cross).minusMonths(1).toString())))
                .andExpect(jsonPath(row(recoveryItem, "direction"), contains("receivable")))
                .andExpect(jsonPath(row(recoveryItem, "counterparty_id"), contains("be_amusement")))
                .andExpect(jsonPath(row(recoveryItem, "amount_cent"), contains(4_950)))
                .andExpect(jsonPath(row(recoveryItem, "accounting_period"), contains(month(cross).toString())));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ar_ap_items WHERE source_type = ? AND project_id = ? AND direction = 'receivable'",
                Integer.class, SOURCE, same.projectId)).isZero();

        mockMvc.perform(post("/api/v1/finance/ar-ap-items/" + recoveryItem + "/payments").with(finance).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"gcc-collect-" + s + "\",\"amount_cent\":4950,\"payment_method\":\"bank_transfer\","
                                + "\"bank_flow_ref\":\"flow_gcc_" + s + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PAYMENT_CONFIRMATION_REQUIRED"));
    }

    /** ⑥：财务还没给 general_card_consume 定付账日规则时，客人付款照常、卡已扣款，月度入账只是暂不入账；定好规则后下一轮入账。 */
    @Test
    void withoutPayDayRuleTheCardIsChargedButNothingPostsUntilTheRuleExists() throws Exception {
        jdbc.update("UPDATE internal_pay_day_rules SET status = 'rejected' WHERE kind = 'route' AND route = ?", SOURCE);
        CustomerTicketPaymentFixture f = card();
        String txn = pay(f, f.createOrderId(1));
        assertThat(f.cardBalance()).isEqualTo(15_000L);

        job.run(runAt(f));
        assertThat(arApCount(f.projectId)).isZero();

        routeRule();
        job.run(runAt(f));
        assertThat(arApCount(f.projectId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM general_card_consume_lines WHERE txn_id = ?", Integer.class, txn)).isEqualTo(1);
    }

    // ---------- 夹具 ----------

    /** 一张空通用卡，再充一批 20000 分本金（通路费率 1%）。 */
    private CustomerTicketPaymentFixture card() {
        CustomerTicketPaymentFixture f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        String topup = "svtxn_gcc_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.update("UPDATE customer_stored_value_accounts SET principal_cent = 20000, balance_cent = 20000 WHERE id = ?", f.accountId);
        jdbc.update("INSERT INTO customer_stored_value_transactions (id, account_id, customer_id, txn_type, amount_cent, balance_after_cent,"
                + " reason, idempotency_key, created_at, principal_cent, bonus_cent) VALUES (?, ?, ?, 'topup', 20000, 20000, '通用卡充值',"
                + " ?, now(), 20000, 0)", topup, f.accountId, f.customerId, "gcc-topup-" + topup);
        jdbc.update("INSERT INTO stored_value_topup_batches (id, account_id, customer_id, project_id, payment_channel, fee_bps, principal_cent,"
                + " remaining_principal_cent, paid_at) VALUES (?, ?, ?, ?, 'wechat', 100, 20000, 20000, now())", topup, f.accountId, f.customerId,
                f.projectId);
        return f;
    }

    /** 通用卡付清该单，返回储值消费流水号（即该笔储值付款意向号）。 */
    private String pay(CustomerTicketPaymentFixture f, String orderId) throws Exception {
        f.payWithStoredValue(orderId, null).andExpect(status().isOk()).andExpect(jsonPath("$.data.issue_status").value("issued"));
        return txnOf(orderId);
    }

    private String txnOf(String orderId) {
        return jdbc.queryForObject("SELECT id FROM ticket_payment_intents WHERE business_order_id = ? AND channel = 'stored_value'",
                String.class, orderId);
    }

    /** 付款营业月 M 之后第 2 个月 2 日北京时间 10:00（M 已结束）。 */
    private Instant runAt(CustomerTicketPaymentFixture f) {
        return month(f).plusMonths(2).atDay(2).atTime(10, 0).atZone(LocalZone.ID).toInstant();
    }

    private YearMonth month(CustomerTicketPaymentFixture f) {
        return YearMonth.from(businessDays.businessDate(f.projectId, null, Instant.now()));
    }

    private String itemOf(String txnId) {
        return jdbc.queryForObject("SELECT ar_ap_item_id FROM general_card_consume_lines WHERE txn_id = ?", String.class, txnId);
    }

    private int arApCount(String projectId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM ar_ap_items WHERE source_type = ? AND project_id = ?", Integer.class, SOURCE, projectId);
    }

    /** 财务给钱路 general_card_consume 定一条早已生效的已批规则（次月 1 日出账、指定第 1 个付账日）。 */
    private void routeRule() {
        jdbc.update("""
                INSERT INTO internal_pay_day_rules (id, kind, route, statement_day, pay_slot, effective_from, status, reason,
                    submitted_by, decided_by, decided_at)
                VALUES (?, 'route', ?, 1, 1, DATE '2000-01-01', 'approved', '测试夹具', 'fixture-finance', 'fixture-approver', now())
                """, "ipdr_gcc_" + s, SOURCE);
    }

    private ResultActions ledger() throws Exception {
        return mockMvc.perform(get("/api/v1/finance/ar-ap-items").param("keyword", SOURCE).param("size", "200").with(finance));
    }

    private static String row(String itemId, String field) {
        return "$.data.items[?(@.id == '" + itemId + "')]." + field;
    }
}
