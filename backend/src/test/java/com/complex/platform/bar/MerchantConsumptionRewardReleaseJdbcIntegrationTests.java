package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.access.AccessActor;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.complex.platform.customer.MerchantConsumptionRewardService;
import com.complex.platform.customer.TicketPointsBillingService;
import com.complex.platform.customer.TicketRewardConfig;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.dto.ArApPaymentRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十一批 TK-04-b（R4 等待期届满释放、退款门槛与冲回、商户积分发行费并入月账单；R5 客人端与账单端双向查看）。
 * 依据：BSM L823「冻结与有效消费」（等待期已满且无待处理的对应退款申请才转为可用；部分退款只取消对应尚待确认积分、剩余仍按原完成时点；
 * 退款待审、已批待退、处理中仍待确认且不产生商户积分发行费；已可用积分后来退款按原追回及待追回积分规则处理；只有转为可用才产生商户积分发行费）；
 * ENUM L72（成本由发生消费的商户承担）、L75（发行费＝转为可用的积分数×全国统一积分发行单价）、L77（从消费单据可反查积分与成长值记录）、
 * L84（各自全额挂账）；BSM L877（客人端只展示结果）；台账 §31 Q38B。
 * 本类 @Transactional 回滚；时间取真实当前时点前后（定时释放只按当前时点判断），规则版本与发行单价均为本例新建。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class MerchantConsumptionRewardReleaseJdbcIntegrationTests {

    @Autowired private BarBillingService billingService;
    @Autowired private BarOrderService orderService;
    @Autowired private JdbcBarRepository barRepository;
    @Autowired private MerchantConsumptionRewardService rewards;
    @Autowired private TicketPointsBillingService pointsBilling;
    @Autowired private FinanceService finance;
    @Autowired private CustomerRepository customers;
    @Autowired private com.complex.platform.access.AccessRepository accessRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private MockMvc mockMvc;

    /** 与 MVC 反序列化同一命名策略：前端 adminClient.mapKeys 把请求键转蛇形。 */
    private final ObjectMapper snake = new ObjectMapper().findAndRegisterModules()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    private final String s = Long.toString(System.nanoTime(), 36);
    private final String merchant = "merchant_mcr2_" + s;
    private final String project = "p_mcr2_" + s;
    private final String contractEntity = "be_mcr2_contract_" + s;
    private final String receiver = "be_mcr2_center_" + s;
    private final Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    private String laterPrice;
    private String firstNational;

    private final String admin = "usr_mcr2_admin_" + s;

    @BeforeEach
    void setUp() {
        accessRepository.createUser(new com.complex.platform.access.AccessUser(admin, admin, "platform_admin", "org_1", null, "active", Instant.now()));
        accessRepository.saveUserPermissionOverrides(admin, List.of("bar.bill.refund", "finance.payment.register", "ticket.points.billing.view"),
                com.complex.platform.access.AccessRepository.Effect.GRANT, "商户消费奖励退款测试授权", admin);
        asAdmin();
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','奖励释放测试综合体','active',now(),now())", project);
        for (String entity : List.of(contractEntity, receiver, "be_mcr2_old_" + s)) {
            jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,?,'platform','active',now(),now())", entity, "测试主体" + entity);
        }
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,contract_entity_id,created_at,updated_at) VALUES(?,'奖励释放测试酒吧','bar','active',?,?,now(),now())",
                merchant, project, contractEntity);
        jdbc.update("INSERT INTO refund_authority_limits (id, subject_type, subject_id, refund_domain, limit_amount_cent) VALUES (?, 'staff', ?, 'bar', 100000000)",
                "ral_mcr2_" + s, admin);
        Instant ruleAt = now.minus(40, ChronoUnit.HOURS);
        config("merchant_points", "bar", rate(1_000L), ruleAt, "V0.01");
        config("merchant_growth", "bar", rate(100L), ruleAt, "V0.01");
        firstNational = config("national", null, national("be_mcr2_old_" + s, "0.5"), ruleAt, version("1"));
        // 释放时生效的全国发行单价（后公布的新版本）：发行费取释放时版本，等待期仍取生成时锁定版本。
        laterPrice = config("national", null, national(receiver, "2"), now.minusSeconds(10), version("2"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── R4 等待期与退款门槛 ───────────────────────────────────────

    @Test
    void refundPendingBlocksReleaseAfterWaitAndPaidRefundCancelsOnlyItsShare() throws Exception {
        String member = customer();
        String bill = bill(member, 30_000L, false, null, now.minus(30, ChronoUnit.HOURS), now.minus(30, ChronoUnit.HOURS), "");
        billingService.settleBill(bill, new BarBillSettleRequest("mcr2-settle-" + s, null, List.of(), "pay-" + s, null, "aggregate_pay"));
        Map<String, Object> reward = reward(bill);
        assertThat(reward.get("status")).isEqualTo("pending");
        Instant available = ((Timestamp) reward.get("available_at")).toInstant();

        // 前端真实请求形状（蛇形键）：已结账单退款 150 元，现金类渠道挂应付、尚未实际退出。
        BarBillAdjustmentRequest refund = snake.readValue("""
                {"idempotency_key":"mcr2-refund-%s","adjustment_type":"refund","amount_cent":15000,"reason":"客人退一瓶"}
                """.formatted(s), BarBillAdjustmentRequest.class);
        BarBillAdjustmentRecord adjustment = billingService.applyBillAdjustment(bill, refund);
        assertThat(adjustment.arApItemId()).isNotNull();

        assertThat(rewards.release((String) reward.get("id"))).as("等待期已满但退款处理中，继续待确认").isFalse();
        assertThat(reward(bill).get("status")).isEqualTo("pending");
        assertThat(feeCount((String) reward.get("id"))).isZero();

        pay(adjustment.arApItemId(), 15_000L);

        Map<String, Object> afterRefund = reward(bill);
        assertThat(((Number) afterRefund.get("refunded_amount_cent")).longValue()).isEqualTo(15_000L);
        assertThat(((Number) afterRefund.get("reversed_points")).longValue()).isEqualTo(15L);
        assertThat(((Number) afterRefund.get("reversed_growth")).longValue()).isEqualTo(150L);
        assertThat(((Timestamp) afterRefund.get("available_at")).toInstant()).as("退款成功不重新起算等待期").isEqualTo(available);
        assertThat(afterRefund.get("status")).isEqualTo("pending");

        assertThat(rewards.release((String) reward.get("id"))).isTrue();
        assertThat(balance(member)).isEqualTo(15L);
        assertThat(jdbc.queryForObject("SELECT points FROM merchant_points_issuance_fees WHERE reward_id=?", Long.class, reward.get("id"))).isEqualTo(15L);
    }

    @Test
    void q38bPartialRefundKeepsOriginalCompletionTime() {
        String member = customer();
        // A 先送达、B 后送达：整笔完成时点取 B；B 部分退款成功后剩余积分仍按 B 起算，不改到 A，也不从退款时点重等。
        Instant deliveredA = now.minus(30, ChronoUnit.HOURS), deliveredB = now.minus(27, ChronoUnit.HOURS);
        String bill = bill(member, 30_000L, true, now.minus(31, ChronoUnit.HOURS), deliveredA, deliveredB, "");
        rewards.tryCreateForBarBill(bill);
        String id = (String) reward(bill).get("id");
        succeededRefund(bill, 30_000L, 15_000L, null);

        rewards.reconcileBarBill(bill);

        Map<String, Object> r = reward(bill);
        assertThat(((Timestamp) r.get("available_at")).toInstant()).isEqualTo(deliveredB.plus(24, ChronoUnit.HOURS));
        assertThat(((Number) r.get("reversed_points")).longValue()).isEqualTo(15L);
        assertThat(jdbc.queryForObject("SELECT keep_threshold_progress FROM customer_points_accounts WHERE customer_id=?", Long.class, member)).isEqualTo(150L);
        assertThat(jdbc.queryForObject("SELECT sum(growth_delta) FROM customer_growth_ledger WHERE merchant_reward_id=?", Long.class, id)).isEqualTo(150L);
        rewards.reconcileBarBill(bill);
        assertThat(((Number) reward(bill).get("reversed_points")).longValue()).as("重复对账不重复取消").isEqualTo(15L);

        assertThat(rewards.release(id)).isTrue();
        assertThat(balance(member)).isEqualTo(15L);
        assertThat(((Number) reward(bill).get("confirmed_points")).longValue()).isEqualTo(15L);
    }

    @Test
    void rejectedVoidRefundReleasesAtOriginalTimeWithFullPoints() {
        String member = customer();
        String bill = bill(member, 30_000L, true, now.minus(31, ChronoUnit.HOURS), now.minus(30, ChronoUnit.HOURS), now.minus(30, ChronoUnit.HOURS), "");
        rewards.tryCreateForBarBill(bill);
        String id = (String) reward(bill).get("id");
        String adjustment = succeededRefund(bill, 30_000L, 15_000L, null);
        String voidId = "void_mcr2_" + s;
        jdbc.update("""
                INSERT INTO bar_order_item_voids(id,order_item_id,order_id,bar_id,merchant_id,void_type,quantity,amount_cent,was_delivered,
                  bill_id,bill_adjustment_id,reason,requested_by_user_id,status,idempotency_key,created_at,effects_applied_at)
                VALUES(?,?,?,'bar_mcr2',?,'return',1,15000,true,?,?,'客人退品','usr_admin','pending',?,now(),now())
                """, voidId, "item_mcr2_b_" + s, "order_mcr2_" + s, merchant, bill, adjustment, "mcr2-void-" + s);

        assertThat(rewards.release(id)).as("退品退款待审核，继续待确认").isFalse();
        assertThat(((Number) reward(bill).get("reversed_points")).longValue()).isZero();

        jdbc.update("UPDATE bar_order_item_voids SET status='rejected',reviewed_by_user_id='usr_reviewer',reviewed_at=now(),review_reason='不予退品' WHERE id=?", voidId);
        barRepository.saveBillAdjustment(new BarBillAdjustmentRecord("adj_mcr2_rev_" + s, bill, "refund_reversal", 15_000L, 15_000L, 30_000L,
                "退品驳回", "usr_reviewer", true, "applied", "mcr2-rev-" + s, List.of(), null, "{}", Instant.now(), Instant.now()));
        jdbc.update("UPDATE bar_bills SET payable_amount_cent=30000 WHERE id=?", bill);

        assertThat(rewards.release(id)).as("驳回且确定不退：按原完成时点与原等待期发放").isTrue();
        assertThat(balance(member)).isEqualTo(30L);
        assertThat(((Number) reward(bill).get("reversed_points")).longValue()).isZero();
    }

    @Test
    void waitNotElapsedOrFrozenAccountKeepsPending() {
        String member = customer();
        String fresh = bill(member, 30_000L, true, now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS), "f");
        rewards.tryCreateForBarBill(fresh);
        assertThat(rewards.release((String) reward(fresh).get("id"))).isFalse();

        String due = bill(member, 30_000L, true, now.minus(31, ChronoUnit.HOURS), now.minus(30, ChronoUnit.HOURS), now.minus(30, ChronoUnit.HOURS), "d");
        rewards.tryCreateForBarBill(due);
        assertThat(rewards.due(now)).contains((String) reward(due).get("id")).doesNotContain((String) reward(fresh).get("id"));
        jdbc.update("UPDATE customer_points_accounts SET account_status='frozen' WHERE customer_id=?", member);
        assertThat(rewards.release((String) reward(due).get("id"))).isFalse();
        assertThat(reward(due).get("status")).isEqualTo("pending");
    }

    // ── R4 商户积分发行费 ─────────────────────────────────────────

    @Test
    void releaseWritesMerchantIssuanceFeeChargedToConsumingMerchant() {
        String member = customer();
        String bill = bill(member, 30_000L, true, now.minus(31, ChronoUnit.HOURS), now.minus(30, ChronoUnit.HOURS), now.minus(30, ChronoUnit.HOURS), "");
        rewards.tryCreateForBarBill(bill);
        String id = (String) reward(bill).get("id");
        assertThat(reward(bill).get("wait_rule_id")).isEqualTo(firstNational);

        assertThat(rewards.release(id)).isTrue();

        Map<String, Object> fee = jdbc.queryForMap("SELECT * FROM merchant_points_issuance_fees WHERE reward_id=?", id);
        assertThat(fee.get("seller_merchant_id")).isEqualTo(merchant);
        assertThat(fee.get("seller_entity_id")).isEqualTo(contractEntity);
        assertThat(fee.get("receiver_entity_id")).isEqualTo(receiver);
        assertThat(fee.get("price_version_id")).isEqualTo(laterPrice);
        assertThat(fee.get("project_id")).isEqualTo(project);
        assertThat(((Number) fee.get("points")).longValue()).isEqualTo(30L);
        assertThat(((BigDecimal) fee.get("unit_price_cent"))).isEqualByComparingTo("2");
        assertThat(((Number) fee.get("amount_cent")).longValue()).isEqualTo(60L);
        assertThat(balance(member)).isEqualTo(30L);
        assertThat(reward(bill).get("status")).isEqualTo("confirmed");
        assertThat(jdbc.queryForObject("SELECT status FROM customer_points_ledger WHERE id=?", String.class, "ledger_earn_" + id)).isEqualTo("confirmed");
        assertThat(jdbc.queryForObject("SELECT points_delta FROM customer_points_ledger WHERE source_doc_type='bar_bill' AND source_doc_id=? AND change_type='confirm'",
                Long.class, bill)).isEqualTo(30L);
        assertThat(rewards.release(id)).isFalse();
        assertThat(feeCount(id)).isEqualTo(1);
    }

    @Test
    void refundAfterConfirmedDeductsAndCreatesRecoveryDebtRepaidByLaterPoints() throws Exception {
        String member = customer();
        String bill = bill(member, 30_000L, false, null, now.minus(30, ChronoUnit.HOURS), now.minus(30, ChronoUnit.HOURS), "");
        billingService.settleBill(bill, new BarBillSettleRequest("mcr2-settle-" + s, null, List.of(), "pay-" + s, null, "aggregate_pay"));
        String id = (String) reward(bill).get("id");
        assertThat(rewards.release(id)).isTrue();
        jdbc.update("UPDATE customer_points_accounts SET balance=10 WHERE customer_id=?", member);

        BarBillAdjustmentRecord adjustment = billingService.applyBillAdjustment(bill, new BarBillAdjustmentRequest(
                "mcr2-refund-" + s, null, List.of(), "refund", 15_000L, "客人退一瓶", null));
        pay(adjustment.arApItemId(), 15_000L);

        Map<String, Object> r = reward(bill);
        assertThat(r.get("status")).isEqualTo("confirmed");
        assertThat(((Number) r.get("reversed_points")).longValue()).isEqualTo(15L);
        assertThat(balance(member)).isZero();
        Map<String, Object> recovery = jdbc.queryForMap("SELECT * FROM customer_points_recoveries WHERE merchant_reward_id=?", id);
        assertThat(((Number) recovery.get("original_points")).longValue()).isEqualTo(5L);
        assertThat(((Number) recovery.get("remaining_points")).longValue()).isEqualTo(5L);
        assertThat(recovery.get("status")).isEqualTo("pending");
        assertThat(recovery.get("reward_id")).isNull();
        assertThat(jdbc.queryForObject("SELECT keep_threshold_progress FROM customer_points_accounts WHERE customer_id=?", Long.class, member)).isEqualTo(150L);
        assertThat(feeCount(id)).as("已确认发行费不因后来退款改写").isEqualTo(1);

        String later = bill(member, 30_000L, true, now.minus(31, ChronoUnit.HOURS), now.minus(30, ChronoUnit.HOURS), now.minus(30, ChronoUnit.HOURS), "l");
        rewards.tryCreateForBarBill(later);
        String laterId = (String) reward(later).get("id");
        assertThat(rewards.release(laterId)).isTrue();
        assertThat(balance(member)).isEqualTo(25L);
        assertThat(jdbc.queryForObject("SELECT status FROM customer_points_recoveries WHERE merchant_reward_id=?", String.class, id)).isEqualTo("recovered");
        assertThat(jdbc.queryForObject("SELECT points FROM merchant_points_issuance_fees WHERE reward_id=?", Long.class, laterId)).isEqualTo(25L);
    }

    @Test
    void monthlyBillCarriesMerchantIssuanceFeeSeparatelyFromTicketFees() {
        String member = customer();
        String bill = bill(member, 30_000L, true, now.minus(31, ChronoUnit.HOURS), now.minus(30, ChronoUnit.HOURS), now.minus(30, ChronoUnit.HOURS), "");
        rewards.tryCreateForBarBill(bill);
        String id = (String) reward(bill).get("id");
        assertThat(rewards.release(id)).isTrue();
        LocalDate month = Instant.now().atZone(com.complex.platform.common.LocalZone.ID).toLocalDate().withDayOfMonth(1);
        jdbc.update("INSERT INTO ticket_points_billing_rules(id,version,effective_month,statement_day,objection_working_days,minimum_days,maximum_days,pay_days,status,created_by,created_at,approved_at) VALUES(?,?,?,1,10,1,30,'{5,15,25}','approved','real-config-review',now(),now()) ON CONFLICT DO NOTHING",
                "billrule_mcr2_" + s, "VMCR2" + s, month);

        pointsBilling.closeReadyMonths(month.plusMonths(1).atTime(8, 0).atZone(com.complex.platform.common.LocalZone.ID).toInstant());

        Map<String, Object> monthly = jdbc.queryForMap("SELECT * FROM ticket_points_monthly_bills WHERE seller_merchant_id=? AND billing_month=?", merchant, month);
        assertThat(monthly.get("fee_kind")).isEqualTo("merchant");
        assertThat(monthly.get("seller_entity_id")).isEqualTo(contractEntity);
        assertThat(((Number) monthly.get("amount_cent")).longValue()).isEqualTo(60L);
        assertThat(jdbc.queryForObject("SELECT bill_id FROM merchant_points_monthly_bill_items WHERE reward_id=?", String.class, id)).isEqualTo(monthly.get("id"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_points_monthly_bill_items WHERE bill_id=?", Integer.class, monthly.get("id"))).isZero();
        var lines = pointsBilling.items((String) monthly.get("id"));
        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst().sourceType()).isEqualTo("bar_bill");
        assertThat(lines.getFirst().sourceId()).isEqualTo(bill);
        assertThat(lines.getFirst().points()).isEqualTo(30L);
    }

    // ── R5 双向查看 ───────────────────────────────────────────────

    @Test
    void customerSeesOnlyOwnMerchantRewardsWithSourceBillNo() throws Exception {
        String member = customer(), other = customer();
        String own = bill(member, 30_000L, true, now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS), "o");
        String theirs = bill(other, 20_000L, true, now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS), "t");
        rewards.tryCreateForBarBill(own);
        rewards.tryCreateForBarBill(theirs);
        String token = "token_mcr2_" + UUID.randomUUID();
        customers.saveSession(new CustomerSessionRecord("session_mcr2_" + UUID.randomUUID(), member, token, "web", Instant.now(), Instant.now().plusSeconds(3600), "active"));
        SecurityContextHolder.clearContext();

        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/customer/merchant-rewards")
                        .header("X-Customer-Token", token).header("X-Terminal-Type", "web"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode rows = json.readTree(body).path("data");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).path("source_type").asText()).isEqualTo("bar_bill");
        assertThat(rows.get(0).path("source_no").asText()).isEqualTo("BILL-MCR2-" + s + "o");
        assertThat(rows.get(0).path("merchant_name").asText()).isEqualTo("奖励释放测试酒吧");
        assertThat(rows.get(0).path("points").asLong()).isEqualTo(30L);
        assertThat(rows.get(0).path("growth").asLong()).isEqualTo(300L);
        assertThat(rows.get(0).path("status").asText()).isEqualTo("pending");
        assertThat(rows.get(0).has("customer_id")).isFalse();
    }

    @Test
    void barBillRewardsRequireMerchantScope() throws Exception {
        String member = customer();
        String bill = bill(member, 30_000L, true, now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS), "");
        rewards.tryCreateForBarBill(bill);
        String ownUser = merchantUser(merchant), otherUser = merchantUser("merchant_mcr2_other_" + s);
        SecurityContextHolder.clearContext();

        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/bar/bills/" + bill + "/rewards")
                        .header("X-Terminal-Type", "web").with(staff(ownUser, merchant)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode items = json.readTree(body).path("data").path("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).path("points").asLong()).isEqualTo(30L);
        assertThat(items.get(0).path("source_id").asText()).isEqualTo(bill);
        assertThat(items.get(0).has("customer_id")).isFalse();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/bar/bills/" + bill + "/rewards")
                        .header("X-Terminal-Type", "web").with(staff(otherUser, "merchant_mcr2_other_" + s)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }

    // ── 造数 ──────────────────────────────────────────────────────

    private void asAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(admin, admin, "platform_admin", "org_1", null, "active"), "token", List.of()));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor staff(String user, String merchantId) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(new AccessActor(user, user, "staff", "org_1", merchantId, "active"), null,
                        List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF"))));
    }

    private String merchantUser(String merchantId) {
        String user = "usr_mcr2_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        accessRepository.createUser(new com.complex.platform.access.AccessUser(user, user, "staff", "org_1", merchantId, "active", Instant.now()));
        accessRepository.saveUserPermissionOverrides(user, List.of("bar.order.view"),
                com.complex.platform.access.AccessRepository.Effect.GRANT, "账单奖励查看测试授权", user);
        return user;
    }

    private void pay(String arApItemId, long amountCent) throws Exception {
        // 前端真实请求形状（蛇形键）：财务登记实际退款付出。
        ArApPaymentRequest request = snake.readValue("""
                {"idempotency_key":"mcr2-pay-%s","amount_cent":%d,"payment_method":"bank_transfer","bank_flow_ref":"退款回单-%s"}
                """.formatted(UUID.randomUUID(), amountCent, s), ArApPaymentRequest.class);
        finance.registerArApPayment(arApItemId, request);
    }

    /** 已退出（不经应付）的退款：账单应付减去退款额，退款调整生效。 */
    private String succeededRefund(String bill, long before, long after, String arApItemId) {
        String id = "adj_mcr2_refund_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        barRepository.saveBillAdjustment(new BarBillAdjustmentRecord(id, bill, "refund", before - after, before, after,
                "客人退一瓶", "usr_admin", true, "applied", "mcr2-refund-" + UUID.randomUUID(), List.of(), arApItemId, "{}", Instant.now(), Instant.now()));
        jdbc.update("UPDATE bar_bills SET payable_amount_cent=? WHERE id=?", after, bill);
        return id;
    }

    private static String version(String tag) {
        return "V" + Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 900_000L) + "." + (tag.length() == 1 ? "0" + tag : tag);
    }

    private static TicketRewardConfig.Values rate(long cents) {
        return new TicketRewardConfig.Values(cents, 1L, 1_000_000L, null, null, null, null, null, null, null, null, null, null, List.of());
    }

    private static TicketRewardConfig.Values national(String receiverEntity, String price) {
        return new TicketRewardConfig.Values(null, null, null, 24, 1, 720, new BigDecimal(price), new BigDecimal("0.001"),
                new BigDecimal("100"), receiverEntity, null, null, null, List.of(), 24, 1, 720);
    }

    private String config(String kind, String format, TicketRewardConfig.Values values, Instant effective, String version) {
        String id = "trc_" + UUID.randomUUID().toString().replace("-", "");
        try {
            jdbc.update("INSERT INTO ticket_reward_config_versions(id,kind,business_format,version,payload,effective_at,published_at,status,created_by,created_at,approved_at) VALUES(?,?,?,?,?::jsonb,?,?,'approved','test-reviewer',?,?)",
                    id, kind, format, version, json.writeValueAsString(values), Timestamp.from(effective), Timestamp.from(effective.minusSeconds(60)),
                    Timestamp.from(effective.minusSeconds(120)), Timestamp.from(effective.minusSeconds(60)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return id;
    }

    private String customer() {
        String id = "c_mcr2_" + UUID.randomUUID().toString().replace("-", "");
        String phone = "137" + String.format("%08d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000L));
        Instant at = Instant.now();
        customers.saveCustomer(new CustomerRecord(id, phone, "***", "active", null, at, at, at));
        return id;
    }

    private String bill(String orderCustomer, long amountCent, boolean settled, Instant paidAt, Instant deliveredA, Instant deliveredB, String tag) {
        Instant at = Instant.now();
        String k = s + tag;
        String table = "table_mcr2_" + k, order = "order_mcr2_" + k, bill = "bill_mcr2_" + k, product = "product_mcr2_" + k;
        barRepository.saveTable(new BarTableRecord(table, "bar_mcr2", merchant, "T-MCR2" + k, "奖励释放测试桌", "card_seat", 4, 1, 0L, "CNY", "active", at, at));
        barRepository.saveProduct(new BarProductRecord(product, "bar_mcr2", merchant, "测试酒", "spirit", amountCent / 2, "CNY", null, null, false,
                "active", at, at, null, null, null, false, null, null, null));
        barRepository.saveOrder(new BarOrderRecord(order, "bar_mcr2", merchant, table, null, orderCustomer, "staff", "created", amountCent, "CNY",
                "web", null, null, at, at, "standard", paidAt));
        barRepository.saveOrderItem(new BarOrderItemRecord("item_mcr2_a_" + k, order, product, "测试酒", 1, amountCent / 2, amountCent / 2,
                "delivered", at, at, null, 0L, false, null, null, null, false, false, deliveredA));
        barRepository.saveOrderItem(new BarOrderItemRecord("item_mcr2_b_" + k, order, product, "测试酒", 1, amountCent - amountCent / 2,
                amountCent - amountCent / 2, "delivered", at, at, null, 0L, false, null, null, null, false, false, deliveredB));
        barRepository.saveBill(new BarBillRecord(bill, "BILL-MCR2-" + k, order, "bar_mcr2", merchant, table, null, amountCent, 0L, 0L, amountCent,
                settled ? amountCent : 0L, "CNY", settled ? "paid" : "unpaid", settled ? "settled" : "unsettled", null, null, at, at));
        return bill;
    }

    private Map<String, Object> reward(String bill) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM merchant_consumption_rewards WHERE source_type='bar_bill' AND source_id=?", Integer.class, bill))
                .as("该账单应生成一条商户消费奖励").isEqualTo(1);
        return jdbc.queryForMap("SELECT * FROM merchant_consumption_rewards WHERE source_type='bar_bill' AND source_id=?", bill);
    }

    private long balance(String member) {
        return jdbc.queryForObject("SELECT balance FROM customer_points_accounts WHERE customer_id=?", Long.class, member);
    }

    private int feeCount(String rewardId) {
        return jdbc.queryForObject("SELECT count(*) FROM merchant_points_issuance_fees WHERE reward_id=?", Integer.class, rewardId);
    }
}
