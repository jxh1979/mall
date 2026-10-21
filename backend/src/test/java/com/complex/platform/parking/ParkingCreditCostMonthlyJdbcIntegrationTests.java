package com.complex.platform.parking;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.support.InternalPayDayFixtures;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * HP-P01 R5 按月：BSM「停车场」章「消费抵免」条「按内部结算价与城市项目公司走平台内部各主体之间的普通内部结算应收应付，按月结算」；
 * BSM「结算配置中心」三个付账日与钱路规则；BSM「商户应收应付变化一律短信」。上月已使用抵免的成本分担，按项目、承担商户合并为一笔
 * 城市项目公司对承担方的应收（钱路 parking_credit_cost），到期日按该钱路付账日规则推算并固化；重跑不重复入账。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        ParkingFixture.SECRET_PROPERTY
})
@AutoConfigureMockMvc
@Transactional
class ParkingCreditCostMonthlyJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CustomerRepository customers;
    @Autowired private ParkingCreditCostMonthlyJob job;

    private ParkingFixture f;
    private String[] a;
    private String[] b;
    private String company;

    @BeforeEach
    void seed() throws Exception {
        InternalPayDayFixtures.seed(jdbc);
        f = new ParkingFixture(jdbc, "pcm");
        f.policy();
        company = f.company();
        f.hourPrice(company, 1_000);
        a = f.merchant("a", "bar");
        b = f.merchant("b", "catering");
        // 两位客人各用一次抵免：A 商户承担两笔、B 商户承担一笔。
        used("c1", List.of(Map.entry(a[0], 30_000L), Map.entry(b[0], 20_000L)), "粤M10001");
        used("c2", List.of(Map.entry(a[0], 25_000L)), "粤M10002");
        // 成本记在上月（月度入账只处理已结束的月份）。
        LocalDate lastMonth = LocalDate.now(com.complex.platform.common.LocalZone.ID).withDayOfMonth(1).minusMonths(1);
        jdbc.update("UPDATE parking_credit_cost_allocations SET cost_month=? WHERE project_id=?", lastMonth, f.project);
    }

    @Test
    void monthlyJobPostsOneReceivablePerBearerWithPayDaySchedule() {
        job.run(Instant.now());
        List<Map<String, Object>> items = receivables();
        assertThat(items).extracting(r -> r.get("counterparty_id")).containsExactly(a[0], b[0]);
        long aShare = share(a[0]);
        assertThat(((Number) items.get(0).get("amount_cent")).longValue()).isEqualTo(aShare);
        assertThat(items).allSatisfy(r -> {
            assertThat(r.get("direction")).isEqualTo("receivable");
            assertThat(r.get("project_id")).isEqualTo(f.project);
            assertThat(r.get("due_date")).isNotNull();
        });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM internal_payable_schedules s JOIN ar_ap_items i ON i.id=s.ar_ap_item_id"
                + " WHERE i.source_type='parking_credit_cost' AND i.project_id=? AND s.route='parking_credit_cost'", Integer.class, f.project))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_credit_cost_allocations WHERE project_id=? AND merchant_id IS NOT NULL"
                + " AND monthly_id IS NULL", Integer.class, f.project)).isZero();
        // 商户应收应付变化一律短信：未配置通知联系人时留失败审计，不影响入账。
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='parking.credit_cost.notification.not_sent'"
                + " AND business_id IN (SELECT id FROM parking_credit_cost_months WHERE project_id=?)", Integer.class, f.project)).isEqualTo(2);
    }

    @Test
    void monthlyJobReplayIsIdempotent() {
        job.run(Instant.now());
        job.run(Instant.now());
        assertThat(receivables()).hasSize(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_credit_cost_months WHERE project_id=?", Integer.class, f.project))
                .isEqualTo(2);
    }

    private void used(String tag, List<Map.Entry<String, Long>> bills, String plate) throws Exception {
        String[] c = f.customer(customers, tag);
        Instant entered = Instant.now().minus(Duration.ofMinutes(140));
        for (Map.Entry<String, Long> bill : bills) {
            f.barBill(bill.getKey(), c[0], null, bill.getValue(), entered.plus(Duration.ofMinutes(1)));
        }
        String session = f.session("M" + tag, plate, entered.toString());
        mockMvc.perform(post("/api/v1/customer/parking/payments").header("X-Terminal-Type", "web").header("X-Customer-Token", c[1])
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parking_session_id\":\"" + session + "\",\"idempotency_key\":\"pcm_" + tag + "_" + f.suffix + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("paid"));
    }

    private long share(String merchant) {
        return jdbc.queryForObject("SELECT sum(share_cent) FROM parking_credit_cost_allocations WHERE project_id=? AND merchant_id=?",
                Long.class, f.project, merchant);
    }

    private List<Map<String, Object>> receivables() {
        return jdbc.queryForList("""
                SELECT id, direction, counterparty_id, amount_cent, project_id, due_date FROM ar_ap_items
                 WHERE source_type='parking_credit_cost' AND project_id=? ORDER BY counterparty_id""", f.project);
    }
}
