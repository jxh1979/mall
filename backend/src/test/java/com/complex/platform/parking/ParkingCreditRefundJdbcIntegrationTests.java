package com.complex.platform.parking;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.finance.RefundPayableSettled;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * HP-P01 R6 真库：BSM「停车场」章「消费抵免」条「对应消费退款成功后，未使用的抵免按剩余有效消费重核，低于门槛的撤销，仍符合资格的按有效组成消费核定承担关系。
 * 已经使用的抵免保留真实停车服务和原确定的费用承担，不从客人的原消费退款中另扣停车费，也不自动冲掉已经发生的停车内部应付」；决策台账 N03。
 * 退款成功＝退款应付经统一回写点实付结清（FinanceService 发布 RefundPayableSettled，来源为酒吧账单退款调整）。门槛 200 元。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        ParkingFixture.SECRET_PROPERTY
})
@AutoConfigureMockMvc
@Transactional
class ParkingCreditRefundJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CustomerRepository customers;
    @Autowired private ApplicationEventPublisher events;

    private ParkingFixture f;
    private String customer;
    private String token;
    private String company;
    private String bill;
    private String session;

    @BeforeEach
    void seed() throws Exception {
        f = new ParkingFixture(jdbc, "pcf");
        f.policy();
        company = f.company();
        String[] c = f.customer(customers, "c");
        customer = c[0];
        token = c[1];
        String[] a = f.merchant("a", "bar");
        // 入场营业日内的消费构成资格；客人查车即按需评估生成当日抵免。
        Instant entered = Instant.now().minus(Duration.ofMinutes(140));
        bill = f.barBill(a[0], customer, null, 25_000, entered.plus(Duration.ofMinutes(1)));
        session = f.session("F1", "粤F10001", entered.toString());
        mockMvc.perform(get("/api/v1/customer/parking/sessions").param("project_id", f.project).param("plate_no", "粤F10001")
                .header("X-Terminal-Type", "web").header("X-Customer-Token", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.credit_status").value("available"));
    }

    @Test
    void unusedCreditRevokedWhenRefundDropsBelowThreshold() {
        refunded(10_000);
        Map<String, Object> credit = credit();
        assertThat(credit.get("status")).isEqualTo("revoked");
        assertThat(((Number) credit.get("eligible_amount_cent")).longValue()).isEqualTo(15_000L);
        assertThat(audits("parking.credit.revoked")).isEqualTo(1);
    }

    @Test
    void unusedCreditKeptWithUpdatedComponentWhenStillEligible() {
        refunded(3_000);
        Map<String, Object> credit = credit();
        assertThat(credit.get("status")).isEqualTo("available");
        assertThat(((Number) credit.get("eligible_amount_cent")).longValue()).isEqualTo(22_000L);
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM parking_credit_components WHERE source_type='bar_bill' AND source_id=?",
                Long.class, bill)).isEqualTo(22_000L);
    }

    /** 已使用：抵免、组成、成本分担原样保留，退款金额不被停车抵扣，只留一条对照审计。 */
    @Test
    void usedCreditUnchangedAndRefundAmountNotReducedByParking() throws Exception {
        f.hourPrice(company, 1_000);
        mockMvc.perform(post("/api/v1/customer/parking/payments").header("X-Terminal-Type", "web").header("X-Customer-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parking_session_id\":\"" + session + "\",\"idempotency_key\":\"rf3_" + f.suffix + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("paid"));
        List<Map<String, Object>> before = allocations();
        String adjustment = refunded(10_000);
        Map<String, Object> credit = credit();
        assertThat(credit.get("status")).isEqualTo("used");
        assertThat(((Number) credit.get("eligible_amount_cent")).longValue()).isEqualTo(25_000L);
        assertThat(allocations()).isEqualTo(before).isNotEmpty();
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM bar_bill_adjustments WHERE id=?", Long.class, adjustment)).isEqualTo(10_000L);
        assertThat(audits("parking.credit.refund_after_use")).isEqualTo(1);
    }

    @Test
    void refundEventReplayIsIdempotent() {
        String adjustment = refunded(10_000);
        events.publishEvent(new RefundPayableSettled("arap_" + adjustment, adjustment));
        assertThat(credit().get("status")).isEqualTo("revoked");
        assertThat(audits("parking.credit.revoked")).isEqualTo(1);
    }

    /** 酒吧账单退款调整生效、应付扣减，退款应付实付结清后发布退款成功事件。 */
    private String refunded(long amountCent) {
        long before = jdbc.queryForObject("SELECT payable_amount_cent FROM bar_bills WHERE id=?", Long.class, bill);
        String id = "adj_pk_" + UUID.randomUUID().toString().substring(0, 12);
        jdbc.update("""
                INSERT INTO bar_bill_adjustments(id,bill_id,adjustment_type,amount_cent,before_payable_amount_cent,after_payable_amount_cent,reason,
                    requested_by_user_id,approval_required,status,idempotency_key,created_at,refunded_at)
                    VALUES(?,?,'refund',?,?,?,'客人退一瓶','pk_admin',TRUE,'applied',?,now(),now())""",
                id, bill, amountCent, before, before - amountCent, "pk-refund-" + id);
        jdbc.update("UPDATE bar_bills SET payable_amount_cent=? WHERE id=?", before - amountCent, bill);
        events.publishEvent(new RefundPayableSettled("arap_" + id, id));
        return id;
    }

    private Map<String, Object> credit() {
        return jdbc.queryForMap("SELECT * FROM parking_credits WHERE customer_id=?", customer);
    }

    private List<Map<String, Object>> allocations() {
        return jdbc.queryForList("""
                SELECT a.id, a.merchant_id, a.bearer_entity_id, a.share_cent, a.component_amount_cent FROM parking_credit_cost_allocations a
                  JOIN parking_credits p ON p.id=a.credit_id WHERE p.customer_id=? ORDER BY a.id""", customer);
    }

    private int audits(String event) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM audit_events WHERE event_name=? AND business_id=(SELECT id FROM parking_credits WHERE customer_id=?)""",
                Integer.class, event, customer);
    }
}
