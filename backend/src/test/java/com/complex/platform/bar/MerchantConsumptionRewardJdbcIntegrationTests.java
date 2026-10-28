package com.complex.platform.bar;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.MerchantConsumptionRewardService;
import com.complex.platform.customer.TicketRewardConfig;
import com.complex.platform.serviceorder.ServiceOrderRecord;
import com.complex.platform.serviceorder.ServiceOrderService;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十一批 TK-04-a（R2 会员识别、R3 整笔履约完成快照与待确认积分、成长值入账）。
 * 依据：BSM L822「积分与成长值获取」（录入会员号走客人查询专门通道并留痕；积分规则与成长值规则分别配置）；BSM L823「冻结与有效消费」
 * （整笔全部实际履约完成时点统一起算；仅完成支付不算履约完成；券抵扣不属有效消费）；BSM L831 生日按支付成功时点锁定；
 * BSM L1064 留款抵用部分计、未抵用不计；台账 §31 Q38A；2026-10-21 操作员裁决补充第 4 条（成长值按业态独立配置，未配置不记）。
 * 本类 @Transactional 回滚；规则版本用远期随机时点或当下时点，避免与库内其他已提交版本相撞。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class MerchantConsumptionRewardJdbcIntegrationTests {

    @Autowired private BarBillingService billingService;
    @Autowired private BarOrderService orderService;
    @Autowired private JdbcBarRepository barRepository;
    @Autowired private MerchantConsumptionRewardService rewards;
    @Autowired private ServiceOrderService serviceOrders;
    @Autowired private CustomerRepository customers;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    /** 与 MVC 反序列化同一命名策略：前端 adminClient.mapKeys 把请求键转蛇形。 */
    private final ObjectMapper snake = new ObjectMapper().findAndRegisterModules()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    private final String s = Long.toString(System.nanoTime(), 36);
    private final String merchant = "merchant_mcr_" + s;
    private final Instant base = Instant.parse("2036-03-01T12:00:00Z").plus(Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 500_000L), ChronoUnit.MINUTES);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor("usr_admin", "usr_admin", "platform_admin", "seed-org", null, "active"), "token", List.of()));
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','奖励测试综合体','active',now(),now())", "p_" + s);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'奖励测试酒吧','bar','active',?,now(),now())", merchant, "p_" + s);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── R2 结账识别会员 ─────────────────────────────────────────────

    @Test
    void settleWithMemberPhoneBindsMemberAndAuditsLookup() throws Exception {
        rules(Instant.now().minusSeconds(5), 1_000L, 100L);
        String phone = "139" + digits();
        String member = customer(phone, null);
        String bill = bill(null, 30_000L, false, null, Instant.now().minusSeconds(60));
        // 前端真实请求形状（蛇形键）。
        BarBillSettleRequest request = snake.readValue("""
                {"idempotency_key":"mcr-phone-%s","payment_id":"pay-%s","payment_channel":"aggregate_pay","member_phone":"%s"}
                """.formatted(s, s, phone), BarBillSettleRequest.class);
        billingService.settleBill(bill, request);

        assertThat(jdbc.queryForList("SELECT customer_id FROM merchant_consumption_members WHERE source_type='bar_bill' AND source_id=?",
                String.class, bill)).containsExactly(member);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_events WHERE event_name='customer.account.viewed' AND business_id=?
                   AND metadata->>'purpose'='bar_bill_member' AND metadata->>'source_id'=? AND metadata->>'phone_hash' IS NOT NULL
                   AND actor_id='usr_admin'""", Integer.class, member, bill)).isEqualTo(1);
        Map<String, Object> reward = reward(bill);
        assertThat(reward.get("customer_id")).isEqualTo(member);
        assertThat(reward.get("status")).isEqualTo("pending");
    }

    @Test
    void settleWithUnknownMemberPhoneRejected() {
        rules(Instant.now().minusSeconds(5), 1_000L, 100L);
        String bill = bill(null, 30_000L, false, null, Instant.now().minusSeconds(60));
        assertThatThrownBy(() -> billingService.settleBill(bill, new BarBillSettleRequest("mcr-unknown-" + s, null, List.of(),
                "pay-" + s, null, "aggregate_pay", null, null, null, null, null, null, "137" + digits())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MEMBER_NOT_FOUND"));
        assertThat(barRepository.findBillById(bill).orElseThrow().settlementStatus()).isEqualTo("unsettled");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM merchant_consumption_members WHERE source_id=?", Integer.class, bill)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customers WHERE id LIKE 'c_mcr_%' AND created_at > now() - interval '1 minute'", Integer.class)).isZero();
    }

    // ── R3 整笔履约完成快照 ─────────────────────────────────────────

    @Test
    void paidOnlyNotFulfilledCreatesNothingAndLastDeliveryCreatesReward() {
        rules(Instant.now().minusSeconds(5), 1_000L, 100L);
        String member = customer("138" + digits(), null);
        String bill = bill(member, 30_000L, false, null, Instant.now().minusSeconds(600), (Instant) null);
        billingService.settleBill(bill, new BarBillSettleRequest("mcr-partial-" + s, null, List.of(), "pay-" + s, null, "aggregate_pay"));
        assertThat(rewardCount(bill)).isZero();

        orderService.deliverOrder("order_mcr_" + s, new BarOrderDeliveryRequest("mcr-deliver-" + s, null, List.of(),
                List.of(Map.of("order_item_id", "item_mcr_b_" + s, "quantity", 1)), null, List.of()));

        Map<String, Object> reward = reward(bill);
        Instant lastDelivered = jdbc.queryForObject("SELECT delivered_at FROM bar_order_items WHERE id=?", Timestamp.class, "item_mcr_b_" + s).toInstant();
        assertThat(((Timestamp) reward.get("completed_at")).toInstant()).isEqualTo(lastDelivered);
        assertThat(((Timestamp) reward.get("available_at")).toInstant()).isEqualTo(lastDelivered.plusSeconds(24 * 3600L));
        assertThat(((Number) reward.get("points")).longValue()).isEqualTo(30L);
        assertThat(((Number) reward.get("growth")).longValue()).isEqualTo(300L);
        // 成长值完成即记、积分待确认：余额不动，保级进度加成长值，积分流水挂账单可反查（ENUM L77）。
        assertThat(jdbc.queryForObject("SELECT balance FROM customer_points_accounts WHERE customer_id=?", Long.class, member)).isZero();
        assertThat(jdbc.queryForObject("SELECT keep_threshold_progress FROM customer_points_accounts WHERE customer_id=?", Long.class, member)).isEqualTo(300L);
        assertThat(jdbc.queryForObject("SELECT points_delta FROM customer_points_ledger WHERE source_doc_type='bar_bill' AND source_doc_id=? AND change_type='earn' AND status='pending'",
                Long.class, bill)).isEqualTo(30L);
        assertThat(jdbc.queryForObject("SELECT growth_delta FROM customer_growth_ledger WHERE merchant_reward_id=? AND change_type='earn'",
                Long.class, reward.get("id"))).isEqualTo(300L);
    }

    @Test
    void lastDeliveryStartsWaitingClock() {
        rules(base.minus(1, ChronoUnit.DAYS), 1_000L, 100L);
        String member = customer("136" + digits(), null);
        Instant a = base, b = base.plus(2, ChronoUnit.HOURS), paid = base.plus(1, ChronoUnit.HOURS);
        String bill = bill(member, 30_000L, true, paid, a, b);
        rewards.tryCreateForBarBill(bill);
        Map<String, Object> reward = reward(bill);
        assertThat(((Timestamp) reward.get("completed_at")).toInstant()).isEqualTo(b);
        assertThat(((Timestamp) reward.get("available_at")).toInstant()).isEqualTo(b.plus(24, ChronoUnit.HOURS));
        rewards.tryCreateForBarBill(bill);
        assertThat(rewardCount(bill)).isEqualTo(1);
    }

    @Test
    void voucherAndTipExcludedFromEffectiveConsumption() {
        rules(base.minus(1, ChronoUnit.DAYS), 1_000L, 100L);
        String member = customer("135" + digits(), null);
        String bill = bill(member, 30_000L, true, base, base);
        // 券抵扣 5000 进折扣、应付 25000；小费 2000 单列。
        jdbc.update("UPDATE bar_bills SET discount_amount_cent=5000,payable_amount_cent=25000,tip_amount_cent=2000,paid_amount_cent=27000 WHERE id=?", bill);
        rewards.tryCreateForBarBill(bill);
        Map<String, Object> reward = reward(bill);
        assertThat(((Number) reward.get("effective_amount_cent")).longValue()).isEqualTo(25_000L);
        assertThat(((Number) reward.get("points")).longValue()).isEqualTo(25L);
        assertThat(((Number) reward.get("growth")).longValue()).isEqualTo(250L);
    }

    @Test
    void minSpendRetainedNotRewardedAppliedRetentionRewarded() {
        rules(base.minus(1, ChronoUnit.DAYS), 1_000L, 100L);
        String member = customer("134" + digits(), null);
        String retainedBill = bill(member, 30_000L, true, base, base);
        adjustment(retainedBill, "min_spend_retained", 3_000L);
        rewards.tryCreateForBarBill(retainedBill);
        assertThat(((Number) reward(retainedBill).get("effective_amount_cent")).longValue()).isEqualTo(27_000L);

        String appliedBill = bill(member, 20_000L, true, base, base, "x");
        jdbc.update("UPDATE bar_bills SET payable_amount_cent=17000,paid_amount_cent=17000 WHERE id=?", appliedBill);
        adjustment(appliedBill, "min_spend_retained_applied", 3_000L);
        rewards.tryCreateForBarBill(appliedBill);
        assertThat(((Number) reward(appliedBill).get("effective_amount_cent")).longValue()).isEqualTo(20_000L);
    }

    @Test
    void birthdayLockedAtPaymentTimeAndLevelMultiplierApplied() {
        rules(base.minus(1, ChronoUnit.DAYS), 1_000L, 100L);
        LocalDate birthday = LocalDate.ofInstant(base, com.complex.platform.common.LocalZone.ID);
        jdbc.update("INSERT INTO customer_birthday_rules(id,rule_name,window_type,multiplier_basis_points,status) VALUES(?,?,'birthday_day',20000,'active')",
                "cbr_" + s, "生日加倍_" + s);
        String level = "lvl_mcr_" + s;
        jdbc.update("INSERT INTO customer_member_levels(id,level_code,level_name,level_rank,point_multiplier_basis_points,status) VALUES(?,?,'测试等级',900,15000,'active')",
                level, level);
        String member = customer("133" + digits(), birthday.withYear(1990));
        jdbc.update("INSERT INTO customer_points_accounts(id,customer_id,balance,level,recent_summary,frozen_points,account_status,keep_threshold_progress,check_in_streak,created_at,updated_at) VALUES(?,?,0,?,'{}',0,'active',0,0,now(),now())",
                "points_" + member, member, level);
        Instant birthdayEvening = birthday.atTime(23, 30).atZone(com.complex.platform.common.LocalZone.ID).toInstant();
        String onBirthday = bill(member, 30_000L, true, birthdayEvening, birthdayEvening.plus(1, ChronoUnit.HOURS));
        String dayBefore = bill(member, 30_000L, true, birthdayEvening.minus(1, ChronoUnit.DAYS), birthdayEvening.minus(23, ChronoUnit.HOURS), "y");
        rewards.tryCreateForBarBill(onBirthday);
        rewards.tryCreateForBarBill(dayBefore);

        Map<String, Object> hit = reward(onBirthday);
        long birthdayBp = ((Number) hit.get("birthday_multiplier_bp")).longValue();
        assertThat(birthdayBp).isGreaterThanOrEqualTo(20_000L);
        assertThat(((Number) hit.get("level_multiplier_bp")).longValue()).isEqualTo(15_000L);
        assertThat(((Number) hit.get("points")).longValue()).isEqualTo(30L * 15_000 / 10_000 * birthdayBp / 10_000);
        Map<String, Object> miss = reward(dayBefore);
        assertThat(((Number) miss.get("birthday_multiplier_bp")).longValue()).isEqualTo(10_000L);
        assertThat(((Number) miss.get("points")).longValue()).isEqualTo(45L);
    }

    @Test
    void missingGrowthRuleDoesNotFallBackToPointsRule() {
        rules(base.minus(1, ChronoUnit.DAYS), 1_000L, null);
        String member = customer("132" + digits(), null);
        String bill = bill(member, 30_000L, true, base, base);
        rewards.tryCreateForBarBill(bill);
        Map<String, Object> reward = reward(bill);
        assertThat(((Number) reward.get("points")).longValue()).isEqualTo(30L);
        assertThat(((Number) reward.get("growth")).longValue()).isZero();
        assertThat(reward.get("growth_rule_id")).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_growth_ledger WHERE merchant_reward_id=?", Integer.class, reward.get("id"))).isZero();
    }

    @Test
    void missingPointsRuleCreditsGrowthOnlyAndNoRulesCreatesNothing() {
        rules(base.minus(1, ChronoUnit.DAYS), null, 100L);
        String member = customer("131" + digits(), null);
        String bill = bill(member, 30_000L, true, base, base);
        rewards.tryCreateForBarBill(bill);
        Map<String, Object> reward = reward(bill);
        assertThat(((Number) reward.get("points")).longValue()).isZero();
        assertThat(reward.get("status")).isEqualTo("confirmed");
        assertThat(((Number) reward.get("growth")).longValue()).isEqualTo(300L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_points_ledger WHERE source_doc_type='bar_bill' AND source_doc_id=?", Integer.class, bill)).isZero();

        jdbc.update("UPDATE merchants SET merchant_type='retail' WHERE id=?", merchant);
        String retail = bill(member, 30_000L, true, base, base, "r");
        rewards.tryCreateForBarBill(retail);
        assertThat(rewardCount(retail)).isZero();
    }

    // ── R2/R3 专项服务单 ───────────────────────────────────────────

    @Test
    void serviceOrderMarkPaidWithMemberPhoneThenCompletionCreatesPendingRewardIdempotently() throws Exception {
        rules(Instant.now().minusSeconds(5), 1_000L, 100L);
        String phone = "130" + digits();
        String member = customer(phone, null);
        var item = serviceOrders.upsertCatalogItem(new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                "mcr-cat-" + s, null, "点舞_" + s, 30_000L, 15, true, "percent", 1000L));
        serviceOrders.setPayConfig(new com.complex.platform.serviceorder.dto.ServicePayConfigRequest("mcr-pay-" + s, "https://qr.example/company"));
        ServiceOrderRecord order = serviceOrders.createOrder(new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                "mcr-so-" + s, item.id(), "usr_admin", merchant, null, null));
        // 前端真实请求形状（终端蛇形键）。
        var paid = snake.readValue("""
                {"idempotency_key":"mcr-paid-%s","member_phone":"%s"}
                """.formatted(s, phone), com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest.class);
        serviceOrders.markPaid(order.id(), paid);
        assertThat(jdbc.queryForList("SELECT customer_id FROM merchant_consumption_members WHERE source_type='service_order' AND source_id=?",
                String.class, order.id())).containsExactly(member);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM merchant_consumption_rewards WHERE source_type='service_order' AND source_id=?",
                Integer.class, order.id())).isZero();

        serviceOrders.complete(order.id(), new com.complex.platform.serviceorder.dto.ServiceOrderActionRequest("mcr-done-" + s));
        rewards.tryCreateForServiceOrder(order.id());

        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM merchant_consumption_rewards WHERE source_type='service_order' AND source_id=?", order.id());
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().get("status")).isEqualTo("pending");
        assertThat(((Number) rows.getFirst().get("points")).longValue()).isEqualTo(30L);
        assertThat(rows.getFirst().get("customer_id")).isEqualTo(member);
    }

    // ── 造数 ──────────────────────────────────────────────────────

    private void rules(Instant effective, Long centsPerPoint, Long centsPerGrowth) {
        if (centsPerPoint != null) config("merchant_points", "bar", rate(centsPerPoint), effective, "V0.01");
        if (centsPerGrowth != null) config("merchant_growth", "bar", rate(centsPerGrowth), effective, "V0.01");
        String entity = "be_mcr_" + s;
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'积分结算中心','platform','active',now(),now())", entity);
        config("national", null, new TicketRewardConfig.Values(null, null, null, 24, 1, 720, new BigDecimal("0.01"), new BigDecimal("0.001"),
                new BigDecimal("100"), entity, null, null, null, List.of(), 24, 1, 720), effective,
                "V" + Math.floorMod(s.hashCode(), 900_000) + ".07");
    }

    private static TicketRewardConfig.Values rate(long cents) {
        return new TicketRewardConfig.Values(cents, 1L, 1_000_000L, null, null, null, null, null, null, null, null, null, null, List.of());
    }

    private void config(String kind, String format, TicketRewardConfig.Values values, Instant effective, String version) {
        try {
            jdbc.update("INSERT INTO ticket_reward_config_versions(id,kind,business_format,version,payload,effective_at,published_at,status,created_by,created_at,approved_at) VALUES(?,?,?,?,?::jsonb,?,?,'approved','test-reviewer',?,?)",
                    "trc_" + UUID.randomUUID().toString().replace("-", ""), kind, format, version, json.writeValueAsString(values),
                    Timestamp.from(effective), Timestamp.from(effective.minusSeconds(60)), Timestamp.from(effective.minusSeconds(120)),
                    Timestamp.from(effective.minusSeconds(60)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String customer(String phone, LocalDate birthDate) {
        String id = "c_mcr_" + UUID.randomUUID().toString().replace("-", "");
        Instant now = Instant.now();
        customers.saveCustomer(new CustomerRecord(id, phone, "***", "active", birthDate, now, now, now));
        return id;
    }

    private static String digits() {
        return String.format("%08d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000L));
    }

    /** 已送达明细 A（deliveredA）＋待送达明细 B（deliveredB 为空即未送达）。 */
    private String bill(String orderCustomer, long amountCent, boolean settled, Instant paidAt, Instant deliveredA, Instant deliveredB) {
        return bill(orderCustomer, amountCent, settled, paidAt, deliveredA, deliveredB, "");
    }

    private String bill(String orderCustomer, long amountCent, boolean settled, Instant paidAt, Instant delivered) {
        return bill(orderCustomer, amountCent, settled, paidAt, delivered, delivered, "");
    }

    private String bill(String orderCustomer, long amountCent, boolean settled, Instant paidAt, Instant delivered, String tag) {
        return bill(orderCustomer, amountCent, settled, paidAt, delivered, delivered, tag);
    }

    private String bill(String orderCustomer, long amountCent, boolean settled, Instant paidAt, Instant deliveredA, Instant deliveredB, String tag) {
        Instant now = Instant.now();
        String k = s + tag;
        String table = "table_mcr_" + k, order = "order_mcr_" + k, bill = "bill_mcr_" + k, product = "product_mcr_" + k;
        barRepository.saveTable(new BarTableRecord(table, "bar_mcr", merchant, "T-MCR" + k, "奖励测试桌", "card_seat", 4, 1, 0L, "CNY", "active", now, now));
        barRepository.saveProduct(new BarProductRecord(product, "bar_mcr", merchant, "测试酒", "spirit", amountCent / 2, "CNY", null, null, false,
                "active", now, now, null, null, null, false, null, null, null));
        barRepository.saveOrder(new BarOrderRecord(order, "bar_mcr", merchant, table, null, orderCustomer, "staff", "created", amountCent, "CNY",
                "web", null, null, now, now, "standard", paidAt));
        barRepository.saveOrderItem(new BarOrderItemRecord("item_mcr_a_" + k, order, product, "测试酒", 1, amountCent / 2, amountCent / 2,
                "delivered", now, now, null, 0L, false, null, null, null, false, false, deliveredA));
        barRepository.saveOrderItem(new BarOrderItemRecord("item_mcr_b_" + k, order, product, "测试酒", 1, amountCent - amountCent / 2,
                amountCent - amountCent / 2, deliveredB == null ? "pending" : "delivered", now, now, null, 0L, false, null, null, null, false, false, deliveredB));
        barRepository.saveBill(new BarBillRecord(bill, "BILL-MCR-" + k, order, "bar_mcr", merchant, table, null, amountCent, 0L, 0L, amountCent,
                settled ? amountCent : 0L, "CNY", settled ? "paid" : "unpaid", settled ? "settled" : "unsettled", null, null, now, now));
        return bill;
    }

    private void adjustment(String bill, String type, long amountCent) {
        barRepository.saveBillAdjustment(new BarBillAdjustmentRecord("adj_mcr_" + UUID.randomUUID().toString().replace("-", ""), bill, type, amountCent, 0L, 0L,
                "留款测试", "usr_admin", false, "applied", "mcr-adj-" + UUID.randomUUID(), List.of(), null, "{}", Instant.now(), null));
    }

    private Map<String, Object> reward(String bill) {
        assertThat(rewardCount(bill)).as("该账单应生成一条商户消费奖励").isEqualTo(1);
        return jdbc.queryForMap("SELECT * FROM merchant_consumption_rewards WHERE source_type='bar_bill' AND source_id=?", bill);
    }

    private int rewardCount(String bill) {
        return jdbc.queryForObject("SELECT count(*) FROM merchant_consumption_rewards WHERE source_type='bar_bill' AND source_id=?", Integer.class, bill);
    }
}
