package com.complex.platform.parking;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.finance.gateway.PaymentGatewayResult;
import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
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
 * HP-P01 R4/R5 真库、真接口（客人端请求体蛇形键）：BSM「停车场」章「消费抵免」条——同一客人在本综合体当日累计判断门槛、
 * 每日限一次、达到门槛的多倍不增加次数；抵免成本按组成有效消费金额的比例，由各笔原消费归属经营主体按内部结算价承担；同一次抵免不得重复使用。
 * 第十二批操作员裁决补充第 3 条：门票不计入门槛、也不承担（门槛只算酒吧账单和商户有效消费）；第 4 条：内部结算价＝实际抵掉的计费小时数×每小时内部价，
 * 免费时长内离场、没抵掉计费时长的不算用掉抵免、不产生成本；第 11 条：按实际少收的钱算，一分没少收＝没用掉抵免、不计成本。
 * 决策台账 N02：300 元与 200 元消费承担 20 元时分别 12 元、8 元。
 * 本项目参数：免费 30 分、5 元/时、封顶 30 元、门槛 200 元、抵免 120 分；城市项目公司每小时内部结算价 10 元。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        ParkingFixture.SECRET_PROPERTY
})
@AutoConfigureMockMvc
@Transactional
class ParkingCreditJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CustomerRepository customers;
    @Autowired private BusinessDayResolver businessDays;
    @Autowired private com.complex.platform.settlement.SettlementConfigWriteService catalog;
    @Autowired private ParkingCreditService credits;
    @MockitoBean private PaymentGatewayRouter gateway;

    private ParkingFixture f;
    private String customer;
    private String token;
    private String company;
    private String[] a;
    private String[] b;

    @BeforeEach
    void seed() {
        f = new ParkingFixture(jdbc, "pcr");
        f.policy();
        f.vendor(true);
        company = f.company();
        String[] c = f.customer(customers, "c");
        customer = c[0];
        token = c[1];
        a = f.merchant("a", "bar");
        b = f.merchant("b", "catering");
        when(gateway.initiatePayment(any())).thenAnswer(inv -> new PaymentGatewayResult(PaymentGatewayResult.Status.PENDING, "pk_gateway",
                "ref_" + UUID.randomUUID(), "pending"));
    }

    // ── R4 资格 ─────────────────────────────────────────────

    @Test
    void customerCreditViewShapedLikeCustomerClient() throws Exception {
        Instant at = Instant.now().minus(Duration.ofMinutes(5));
        f.barBill(a[0], customer, null, 15_000, at);
        f.serviceOrder(b[0], customer, 6_000, at);
        credits().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("available"))
                .andExpect(jsonPath("$.data.eligible_amount_cent").value(21_000))
                .andExpect(jsonPath("$.data.threshold_cent").value(20_000))
                .andExpect(jsonPath("$.data.credit_minutes").value(120));
        assertThat(jdbc.queryForList("SELECT merchant_id, bearer_entity_id, amount_cent FROM parking_credit_components c JOIN parking_credits p"
                + " ON p.id=c.credit_id WHERE p.customer_id=? ORDER BY amount_cent", customer))
                .extracting(r -> r.get("bearer_entity_id")).containsExactly(b[1], a[1]);
    }

    @Test
    void multipleOfThresholdStillOneCredit() throws Exception {
        f.barBill(a[0], customer, null, 60_000, Instant.now().minus(Duration.ofMinutes(5)));
        credits().andExpect(jsonPath("$.data.status").value("available")).andExpect(jsonPath("$.data.credit_minutes").value(120));
        credits().andExpect(jsonPath("$.data.status").value("available"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_credits WHERE customer_id=?", Integer.class, customer)).isEqualTo(1);
    }

    /** 收银未识别会员、下单也无客人的酒吧账单不计；收银识别的会员按会员计。 */
    @Test
    void unidentifiedBarBillNotCounted() throws Exception {
        Instant at = Instant.now().minus(Duration.ofMinutes(5));
        f.barBill(a[0], null, null, 30_000, at);
        f.barBill(a[0], null, customer, 10_000, at);
        credits().andExpect(jsonPath("$.data.status").value("not_reached"))
                .andExpect(jsonPath("$.data.eligible_amount_cent").value(10_000));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_credits WHERE customer_id=?", Integer.class, customer)).isZero();
    }

    /** 营业日归集（ENUM「运营营业日」）：营业日起点前下单的消费归前一营业日；前一营业日入场的车按入场营业日的抵免试算。 */
    @Test
    void consumptionBeforeCutoffCountsToPreviousBusinessDay() throws Exception {
        Instant start = businessDays.startInstant(businessDays.businessDate(Instant.now()));
        Instant entered = start.minus(Duration.ofHours(3));
        f.barBill(a[0], customer, null, 30_000, entered.plus(Duration.ofMinutes(1)));
        credits().andExpect(jsonPath("$.data.status").value("not_reached")).andExpect(jsonPath("$.data.eligible_amount_cent").value(0));
        f.session("C4", "粤A40004", entered.toString());
        lookup("粤A40004").andExpect(status().isOk()).andExpect(jsonPath("$.data.credit_status").value("available"))
                .andExpect(jsonPath("$.data.credit_minutes").value(120));
        assertThat(jdbc.queryForObject("SELECT business_date::text FROM parking_credits WHERE customer_id=?", String.class, customer))
                .isEqualTo(businessDays.businessDate(entered).toString());
    }

    /** 第十二批裁决补充第 3 条「门票不计入门槛、也不承担」：当日已付款门票单独就够门槛，加少量酒吧账单也不生成抵免，组成里没有门票。 */
    @Test
    void ticketOrderNeitherCountsNorBears() throws Exception {
        Instant at = Instant.now().minus(Duration.ofMinutes(5));
        String type = "pk_tt_" + f.suffix;
        String order = "pk_to_" + f.suffix;
        jdbc.update("INSERT INTO ticket_types(id,ticket_name,base_calendar_price_mode,status,created_at,updated_at) VALUES(?,'停车回归票','fixed','active',now(),now())",
                type);
        jdbc.update("""
                INSERT INTO ticket_orders(id,order_no,ticket_type_id,quantity,sales_channel,amount_cent,pay_status,issue_status,status,created_at,updated_at,project_id,customer_id)
                VALUES(?,?,?,1,'web',30000,'paid','issued','created',?,?,?,?)""", order, order, type,
                java.sql.Timestamp.from(at), java.sql.Timestamp.from(at), f.project, customer);
        f.barBill(a[0], customer, null, 5_000, at);
        credits().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("not_reached"))
                .andExpect(jsonPath("$.data.eligible_amount_cent").value(5_000));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_credits WHERE customer_id=?", Integer.class, customer)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_credit_components WHERE source_id=?", Integer.class, order)).isZero();
    }

    // ── R5 使用与成本分担 ─────────────────────────────────────

    /** N02：300 元与 200 元两笔消费构成资格，抵掉 h 个计费小时×10 元／时，按 3:2 分担；抵到 0 元记一笔 0 元已缴并下发抵免分钟。 */
    @Test
    void n02ExampleSplits1200And800() throws Exception {
        f.hourPrice(company, 1_000);
        Instant entered = Instant.now().minus(Duration.ofMinutes(140));
        f.barBill(a[0], customer, null, 30_000, entered.plus(Duration.ofMinutes(1)));
        f.barBill(b[0], customer, null, 20_000, entered.plus(Duration.ofMinutes(2)));
        String session = f.session("C5", "粤A50005", entered.toString());
        long hours = offsetHours(entered, 120);
        lookup("粤A50005").andExpect(jsonPath("$.data.due_cent").value(0)).andExpect(jsonPath("$.data.credit_offset_hours").value(hours));
        String payment = id(pay(session, "n02_" + f.suffix).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("paid")).andExpect(jsonPath("$.data.amount_cent").value(0))
                .andExpect(jsonPath("$.data.grace_until").exists()));
        Map<String, Object> credit = jdbc.queryForMap("SELECT * FROM parking_credits WHERE customer_id=?", customer);
        assertThat(credit.get("status")).isEqualTo("used");
        assertThat(credit.get("session_id")).isEqualTo(session);
        assertThat(((Number) credit.get("cost_cent")).longValue()).isEqualTo(hours * 1_000);
        assertThat(shares()).containsExactly(Map.entry(a[0], hours * 600), Map.entry(b[0], hours * 400));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_credit_cost_allocations WHERE merchant_id IS NULL AND credit_id=?",
                Integer.class, credit.get("id"))).isZero();
        mockMvc.perform(f.openGet("/parking/payments")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].payment_id").value(payment)).andExpect(jsonPath("$.data[0].credit_minutes").value(120));
    }

    /** 抵到 0 元的已缴记录没有资金往来，停车系统也回报不了 0 元缴费：出场不进停车缴费对账差异（BSM L1445 收入归属与对账）。 */
    @Test
    void zeroAmountCreditPaymentNotReconciledAtExit() throws Exception {
        f.hourPrice(company, 1_000);
        Instant entered = Instant.now().minus(Duration.ofMinutes(140));
        f.barBill(a[0], customer, null, 30_000, entered.plus(Duration.ofMinutes(1)));
        String session = f.session("C12", "粤A12012", entered.toString());
        String payment = id(pay(session, "zero_" + f.suffix).andExpect(status().isOk()).andExpect(jsonPath("$.data.amount_cent").value(0)));
        mockMvc.perform(f.webhook("vehicle_exited", "zx_" + f.suffix, Map.of("session_ref", "C12", "exited_at", Instant.now().toString(),
                "fee_cent", 0, "paid_cent", 0))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM parking_sessions WHERE id=?", String.class, session)).isEqualTo("exited");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM integration_reconciliation_differences WHERE external_business_id=?",
                Integer.class, payment)).isZero();
    }

    /** 三方均分不尽：各方四舍五入到分，尾差单列一行由停车收款主体城市项目公司承担。 */
    @Test
    void threeWayResidualRecordedAsSeparateLine() throws Exception {
        f.hourPrice(company, 1_000);
        String[] c = f.merchant("c3", "retail");
        Instant entered = Instant.now().minus(Duration.ofMinutes(60));
        for (String m : List.of(a[0], b[0], c[0])) {
            f.barBill(m, customer, null, 10_000, entered.plus(Duration.ofMinutes(1)));
        }
        String session = f.session("C6", "粤A60006", entered.toString());
        pay(session, "res_" + f.suffix).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("paid"));
        long cost = offsetHours(entered, 120) * 1_000;
        long each = Math.round(cost / 3.0);
        Map<String, Object> residual = jdbc.queryForMap("SELECT bearer_entity_id, share_cent FROM parking_credit_cost_allocations a"
                + " JOIN parking_credits p ON p.id=a.credit_id WHERE p.customer_id=? AND a.merchant_id IS NULL", customer);
        assertThat(residual.get("bearer_entity_id")).isEqualTo(company);
        assertThat(((Number) residual.get("share_cent")).longValue()).isEqualTo(cost - 3 * each).isNotZero();
        assertThat(shares().values()).containsOnly(each);
    }

    @Test
    void creditNotUsedWhenStayWithinFreeTime() throws Exception {
        f.hourPrice(company, 1_000);
        Instant entered = Instant.now().minus(Duration.ofMinutes(10));
        f.barBill(a[0], customer, null, 30_000, entered.plus(Duration.ofMinutes(1)));
        String session = f.session("C7", "粤A70007", entered.toString());
        lookup("粤A70007").andExpect(jsonPath("$.data.due_cent").value(0)).andExpect(jsonPath("$.data.credit_offset_hours").value(0));
        pay(session, "free_" + f.suffix).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("PARKING_NO_FEE_DUE"));
        assertThat(jdbc.queryForObject("SELECT status FROM parking_credits WHERE customer_id=?", String.class, customer)).isEqualTo("available");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_credit_cost_allocations a JOIN parking_credits p ON p.id=a.credit_id"
                + " WHERE p.customer_id=?", Integer.class, customer)).isZero();
    }

    /** 同一客人当日两辆车同时缴费：只有先下单的一笔占到抵免，另一笔按全额。 */
    @Test
    void concurrentPaymentsOnlyOneGetsCredit() throws Exception {
        f.hourPrice(company, 1_000);
        Instant entered = Instant.now().minus(Duration.ofMinutes(300));
        f.barBill(a[0], customer, null, 30_000, entered.plus(Duration.ofMinutes(1)));
        String first = f.session("C8", "粤A80008", entered.toString());
        String second = f.session("C9", "粤A90009", entered.toString());
        String p1 = id(pay(first, "cc1_" + f.suffix).andExpect(status().isOk()));
        String p2 = id(pay(second, "cc2_" + f.suffix).andExpect(status().isOk()));
        Map<String, Object> one = jdbc.queryForMap("SELECT credit_id, amount_cent FROM parking_payments WHERE id=?", p1);
        Map<String, Object> two = jdbc.queryForMap("SELECT credit_id, amount_cent FROM parking_payments WHERE id=?", p2);
        assertThat(one.get("credit_id")).isNotNull();
        assertThat(two.get("credit_id")).isNull();
        assertThat(((Number) two.get("amount_cent")).longValue()).isGreaterThan(((Number) one.get("amount_cent")).longValue());
    }

    @Test
    void missingInternalPriceIsConflict() throws Exception {
        Instant entered = Instant.now().minus(Duration.ofMinutes(140));
        f.barBill(a[0], customer, null, 30_000, entered.plus(Duration.ofMinutes(1)));
        String session = f.session("C10", "粤A10010", entered.toString());
        pay(session, "np_" + f.suffix).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PARKING_CREDIT_PRICE_MISSING"));
        assertThat(jdbc.queryForObject("SELECT status FROM parking_credits WHERE customer_id=?", String.class, customer)).isEqualTo("available");
    }

    /**
     * 每小时内部结算价在内部结算价目录维护（BSM「内部结算价目录」：实际提供商品或服务的主体提交价格，提交人与审批人分离）：
     * 城市项目公司作为提供主体落草案不生效，他人复核转正后缴费才按该价固化成本。请求体按前端 adminClient.mapKeys 蛇形键。
     */
    @Test
    void entityProvidedHourPriceDraftAppliesOnlyAfterActivation() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper snake = new com.fasterxml.jackson.databind.ObjectMapper()
                .setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);
        com.complex.platform.settlement.SettlementConfigWriteRequests.InternalPriceItemRequest request = snake.readValue("""
                {"idempotency_key":"ipc-pk-%s","provider_entity_id":"%s","category":"parking_credit","item_name":"停车抵免（每小时）","price_cent":800}
                """.formatted(f.suffix, company), com.complex.platform.settlement.SettlementConfigWriteRequests.InternalPriceItemRequest.class);
        String code = catalog.saveInternalPriceItem(request, "pk_drafter_" + f.suffix);
        Instant entered = Instant.now().minus(Duration.ofMinutes(140));
        f.barBill(a[0], customer, null, 30_000, entered.plus(Duration.ofMinutes(1)));
        String session = f.session("C11", "粤A11011", entered.toString());
        pay(session, "dr_" + f.suffix).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PARKING_CREDIT_PRICE_MISSING"));
        String draft = jdbc.queryForObject("SELECT id FROM internal_price_catalog WHERE item_code=? AND is_draft AND status='active'",
                String.class, code);
        catalog.activateDraft("internal_price_item", draft, "pk_approver_" + f.suffix);
        pay(session, "ac_" + f.suffix).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("paid"));
        assertThat(jdbc.queryForObject("SELECT hour_price_cent FROM parking_credits WHERE customer_id=?", Long.class, customer)).isEqualTo(800L);
    }

    // ── 第十二批裁决补充第 11 条「按实际少收的钱算」──────────────
    // 入场在上一营业日、跨当前营业日起点：抵免前后的差额只在前一营业日一段，当日一段抵免前后相同（与跑测时刻无关）。

    /** 封顶下抵免前后同价：一分没少收＝没用掉抵免——缴费不带抵免、抵免仍可用、不产生成本。 */
    @Test
    void creditNotUsedWhenDailyCapLeavesNoShortfall() throws Exception {
        f.hourPrice(company, 1_000);
        Instant entered = todayStart().minus(Duration.ofHours(8));
        f.barBill(a[0], customer, null, 30_000, entered.plus(Duration.ofMinutes(1)));
        String session = f.session("C13", "粤A13013", entered.toString());
        lookup("粤A13013").andExpect(status().isOk()).andExpect(jsonPath("$.data.credit_offset_hours").value(0))
                .andExpect(jsonPath("$.data.credit_id").doesNotExist());
        String payment = id(pay(session, "cap_" + f.suffix).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending")));
        assertThat(jdbc.queryForObject("SELECT credit_id FROM parking_payments WHERE id=?", String.class, payment)).isNull();
        Map<String, Object> credit = jdbc.queryForMap("SELECT status, pending_payment_id, cost_cent FROM parking_credits WHERE customer_id=?",
                customer);
        assertThat(credit.get("status")).isEqualTo("available");
        assertThat(credit.get("pending_payment_id")).isNull();
        assertThat(credit.get("cost_cent")).isNull();
    }

    /** 7 小时封顶下只少收 1 小时的钱（计费小时差为 2）：抵免照常占用、缴费成功置已使用，成本按 1 小时×每小时内部价。 */
    @Test
    void costCountsOnlyTheHourActuallyUncollected() throws Exception {
        f.hourPrice(company, 1_000);
        Instant entered = todayStart().minus(Duration.ofHours(7));
        f.barBill(a[0], customer, null, 30_000, entered.plus(Duration.ofMinutes(1)));
        String session = f.session("C14", "粤A14014", entered.toString());
        lookup("粤A14014").andExpect(jsonPath("$.data.credit_offset_hours").value(1));
        String payment = id(pay(session, "seven_" + f.suffix).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.credit_offset_hours").value(1)));
        confirm(payment).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("paid"));
        Map<String, Object> credit = jdbc.queryForMap("SELECT status, offset_hours, cost_cent FROM parking_credits WHERE customer_id=?", customer);
        assertThat(credit.get("status")).isEqualTo("used");
        assertThat(((Number) credit.get("offset_hours")).longValue()).isEqualTo(1);
        assertThat(((Number) credit.get("cost_cent")).longValue()).isEqualTo(1_000);
    }

    /** 先缴费、后达标再续缴：按整次停车累计不抵免应缴与累计实缴之差计少收——未达标时已缴到只差 1 小时，续缴抵到 0 元、成本按 1 小时。 */
    @Test
    void paidBeforeReachingThresholdUsesCumulativeShortfall() throws Exception {
        f.hourPrice(company, 1_000);
        Instant entered = todayStart().minus(Duration.ofHours(5));
        String session = f.session("C15", "粤A15015", entered.toString());
        long gross = ((Number) JsonPath.read(body(lookup("粤A15015").andExpect(status().isOk())), "$.data.gross_fee_cent")).longValue();
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO parking_payments(id,project_id,session_id,customer_id,amount_cent,payment_intent_id,status,paid_at,grace_until,
                    receiver_entity_id,idempotency_key) VALUES(?,?,?,?,?,?,'paid',?,?,?,?)""", "ppay_pre_" + f.suffix, f.project, session,
                customer, gross - 500, "pi_pre_" + f.suffix, java.sql.Timestamp.from(now.minus(Duration.ofMinutes(60))),
                java.sql.Timestamp.from(now.minus(Duration.ofMinutes(45))), company, "pre_" + f.suffix);
        // 酒吧账单在首次缴费之后才结清，客人这才达标。
        f.barBill(a[0], customer, null, 30_000, entered.plus(Duration.ofMinutes(1)));
        lookup("粤A15015").andExpect(jsonPath("$.data.due_cent").value(0)).andExpect(jsonPath("$.data.credit_offset_hours").value(1));
        pay(session, "cum_" + f.suffix).andExpect(status().isOk()).andExpect(jsonPath("$.data.amount_cent").value(0))
                .andExpect(jsonPath("$.data.status").value("paid"));
        Map<String, Object> credit = jdbc.queryForMap("SELECT status, offset_hours, cost_cent FROM parking_credits WHERE customer_id=?", customer);
        assertThat(credit.get("status")).isEqualTo("used");
        assertThat(((Number) credit.get("offset_hours")).longValue()).isEqualTo(1);
        assertThat(((Number) credit.get("cost_cent")).longValue()).isEqualTo(1_000);
    }

    /**
     * 带抵免缴费后继续停到每日封顶：续缴按整次停车累计重算——累计少收从 2 小时降到 1 小时，抵免仍为已使用，成本与分担改按 1 小时。
     * 前一营业日入场 7.5 小时、第 3 小时带抵免实缴 5 元（抵掉 2 小时）；整段封顶后不抵免应缴 30 元、抵免后 25 元。
     */
    @Test
    void renewalAfterCapLowersOffsetToCumulativeShortfall() throws Exception {
        f.hourPrice(company, 1_000);
        Instant entered = todayStart().minus(Duration.ofMinutes(450));
        String session = usedOnFirstPayment("C16", "粤A16016", entered);
        lookup("粤A16016").andExpect(jsonPath("$.data.credit_offset_hours").value(1));
        confirm(id(pay(session, "rn1_" + f.suffix).andExpect(status().isOk()))).andExpect(status().isOk());
        Map<String, Object> credit = jdbc.queryForMap("SELECT id, status, offset_hours, cost_cent FROM parking_credits WHERE customer_id=?",
                customer);
        assertThat(credit.get("status")).isEqualTo("used");
        assertThat(((Number) credit.get("offset_hours")).longValue()).isEqualTo(1);
        assertThat(((Number) credit.get("cost_cent")).longValue()).isEqualTo(1_000);
        assertThat(jdbc.queryForObject("SELECT sum(share_cent) FROM parking_credit_cost_allocations WHERE credit_id=?", Long.class,
                credit.get("id"))).isEqualTo(1_000L);
    }

    /**
     * 第 11 条「一分没少收＝没用掉抵免、不计成本」按整次停车累计：带抵免缴费后停到封顶，续缴后累计实缴＝不抵免应缴——
     * 抵免退回可用（不挂本次停车）、不计成本、未入账的分担删除。前一营业日入场 9 小时、第 3 小时带抵免实缴 5 元（抵掉 2 小时）。
     */
    @Test
    void renewalReturnsCreditWhenCumulativelyNothingUncollected() throws Exception {
        f.hourPrice(company, 1_000);
        Instant entered = todayStart().minus(Duration.ofHours(9));
        String session = usedOnFirstPayment("C17", "粤A17017", entered);
        lookup("粤A17017").andExpect(jsonPath("$.data.credit_offset_hours").value(0));
        confirm(id(pay(session, "rn0_" + f.suffix).andExpect(status().isOk()))).andExpect(status().isOk());
        Map<String, Object> credit = jdbc.queryForMap("SELECT id, status, session_id, used_at, cost_cent FROM parking_credits WHERE customer_id=?",
                customer);
        assertThat(credit.get("status")).isEqualTo("available");
        assertThat(credit.get("session_id")).isNull();
        assertThat(credit.get("used_at")).isNull();
        assertThat(credit.get("cost_cent")).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_credit_cost_allocations WHERE credit_id=?", Integer.class,
                credit.get("id"))).isZero();
    }

    // ── 工具 ─────────────────────────────────────────────

    /**
     * 前一营业日 entered 入场、当日消费达标；入场后第 3 小时带抵免缴费成功：免费 30 分后计费 2.5 小时（按 3 小时 15 元），
     * 抵免 120 分后只剩 0.5 小时（按 1 小时 5 元），实缴 5 元、实际抵掉 2 小时——按缴费成功的既有路径（预占→use）置已使用并分担。
     */
    private String usedOnFirstPayment(String ref, String plate, Instant entered) throws Exception {
        f.barBill(a[0], customer, null, 30_000, entered.plus(Duration.ofMinutes(1)));
        String session = f.session(ref, plate, entered.toString());
        lookup(plate).andExpect(jsonPath("$.data.credit_status").value("available"));
        String credit = jdbc.queryForObject("SELECT id FROM parking_credits WHERE customer_id=?", String.class, customer);
        String first = "ppay_first_" + ref + "_" + f.suffix;
        Instant paidAt = entered.plus(Duration.ofHours(3));
        jdbc.update("""
                INSERT INTO parking_payments(id,project_id,session_id,customer_id,amount_cent,payment_intent_id,status,paid_at,grace_until,
                    receiver_entity_id,idempotency_key,credit_id,credit_offset_hours,credit_hour_price_cent,credit_price_entry_id)
                VALUES(?,?,?,?,500,?,'paid',?,?,?,?,?,2,1000,?)""", first, f.project, session, customer, "pi_" + first,
                java.sql.Timestamp.from(paidAt), java.sql.Timestamp.from(paidAt.plus(Duration.ofMinutes(15))), company, first, credit,
                "ipc_pk_" + f.suffix);
        jdbc.update("UPDATE parking_credits SET pending_payment_id=? WHERE id=?", first, credit);
        credits.use(first, paidAt);
        assertThat(jdbc.queryForObject("SELECT cost_cent FROM parking_credits WHERE id=?", Long.class, credit)).isEqualTo(2_000L);
        return session;
    }

    private Instant todayStart() {
        return businessDays.startInstant(businessDays.businessDate(Instant.now()));
    }

    private ResultActions confirm(String paymentId) throws Exception {
        Map<String, Object> intent = jdbc.queryForMap("""
                SELECT i.gateway_ref, i.amount_cent FROM finance_payment_intents i
                  JOIN parking_payments p ON p.payment_intent_id = i.id WHERE p.id=?""", paymentId);
        String ref = (String) intent.get("gateway_ref");
        when(gateway.isGatewayEnabled()).thenReturn(true);
        when(gateway.queryStatus(ref)).thenReturn(new PaymentGatewayResult(PaymentGatewayResult.Status.SUCCEEDED, "pk_gateway", ref, "", null,
                new com.complex.platform.finance.gateway.PaymentGatewayRequest("parking_fee", paymentId,
                        ((Number) intent.get("amount_cent")).longValue(), "CNY", "wechat", ref, null, null)));
        return mockMvc.perform(post("/api/v1/customer/parking/payments/" + paymentId + "/confirm")
                .header("X-Terminal-Type", "web").header("X-Customer-Token", token).contentType(MediaType.APPLICATION_JSON).content("{}"));
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private long offsetHours(Instant entered, int creditMinutes) {
        return ParkingFeeCalculator.quote(entered, Instant.now(), 30, 500, 3_000, creditMinutes,
                t -> businessDays.endInstant(businessDays.businessDate(t))).offsetHours();
    }

    private Map<String, Long> shares() {
        Map<String, Long> out = new java.util.LinkedHashMap<>();
        jdbc.queryForList("""
                SELECT a.merchant_id, a.share_cent FROM parking_credit_cost_allocations a JOIN parking_credits p ON p.id=a.credit_id
                 WHERE p.customer_id=? AND a.merchant_id IS NOT NULL ORDER BY a.share_cent DESC, a.merchant_id""", customer)
                .forEach(r -> out.put((String) r.get("merchant_id"), ((Number) r.get("share_cent")).longValue()));
        return out;
    }

    private ResultActions credits() throws Exception {
        return mockMvc.perform(get("/api/v1/customer/parking/credits").param("project_id", f.project)
                .header("X-Terminal-Type", "web").header("X-Customer-Token", token));
    }

    private ResultActions lookup(String plate) throws Exception {
        return mockMvc.perform(get("/api/v1/customer/parking/sessions").param("project_id", f.project).param("plate_no", plate)
                .header("X-Terminal-Type", "web").header("X-Customer-Token", token));
    }

    private ResultActions pay(String session, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/customer/parking/payments").header("X-Terminal-Type", "web").header("X-Customer-Token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parking_session_id\":\"" + session + "\",\"idempotency_key\":\"" + key + "\"}"));
    }

    private static String id(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }
}
