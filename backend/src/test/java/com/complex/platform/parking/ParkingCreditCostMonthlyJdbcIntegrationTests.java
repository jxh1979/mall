package com.complex.platform.parking;

// 文件版本：V0.04

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.LocalZone;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.settlement.MerchantSettlementStatementNoticeJob;
import com.complex.platform.support.InternalPayDayFixtures;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Propagation;
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
    @Autowired private MerchantSettlementStatementNoticeJob notice;
    @Autowired private PiiCipher cipher;

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
    }

    @Test
    void monthlyJobReplayIsIdempotent() {
        job.run(Instant.now());
        job.run(Instant.now());
        assertThat(receivables()).hasSize(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_credit_cost_months WHERE project_id=?", Integer.class, f.project))
                .isEqualTo(2);
    }

    /**
     * 第 101 条（BSM:1597「异议期自该账单在商户专区可查看之日的次日起算」、BSM:1673「月结定格 → 账单出账 → 异议期届满 → 付账日」、DEC-R8-55）：
     * 停车抵免成本到出账日（internal_payable_schedules.visible_at）才列入商户专区综合账单与未读月份，与门票分账、异议截止日同一时刻；没列出不记已读。
     */
    @Test
    void merchantPortalListsCostFromStatementDayOnly() throws Exception {
        job.run(Instant.now());
        String item = jdbc.queryForObject("SELECT id FROM ar_ap_items WHERE source_type='parking_credit_cost' AND project_id=? AND counterparty_id=?",
                String.class, f.project, a[0]);
        String month = YearMonth.now(LocalZone.ID).minusMonths(1).toString();
        RequestPostProcessor shop = new StaffRequestFixture(jdbc).staff("usr_pcm_" + f.suffix, "staff", null, a[0],
                "merchant.billing.view", "merchant.portal.settlement.view");

        // 出账日未到（夹具出账日为 1 日，改配置造未来出账日会随当天日期漂移，故直接改 visible_at）。
        jdbc.update("UPDATE internal_payable_schedules SET visible_at = now() + interval '1 day' WHERE ar_ap_item_id=?", item);
        JsonNode before = portal(get("/api/v1/merchant-portal/fee-statement").param("month", month), shop);
        assertThat(cost(before)).as("出账日未到，综合账单不列").isZero();
        assertThat(before.path("bill").path("lines").findValuesAsText("fee_name")).doesNotContain("停车抵免成本");
        assertThat(portal(get("/api/v1/merchant-portal/ticket-settlements"), shop).findValuesAsText("ar_ap_item_id")).doesNotContain(item);
        assertThat(billMonths(shop)).doesNotContainKey(month);
        assertThat(reads(item)).as("没列出不记已读").isZero();

        // 出账日到。
        jdbc.update("UPDATE internal_payable_schedules SET visible_at = now() - interval '1 minute' WHERE ar_ap_item_id=?", item);
        assertThat(billMonths(shop)).containsExactly(entry(month, 1L));
        JsonNode after = portal(get("/api/v1/merchant-portal/fee-statement").param("month", month), shop);
        long aShare = share(a[0]);
        assertThat(cost(after)).isEqualTo(aShare);
        assertThat(after.path("bill").path("total_amount_cent").asLong()).isEqualTo(aShare);
        JsonNode listed = portal(get("/api/v1/merchant-portal/ticket-settlements"), shop);
        assertThat(listed.findValuesAsText("ar_ap_item_id")).containsExactly(item);
        assertThat(listed.get(0).hasNonNull("objection_deadline")).isTrue();
        assertThat(billMonths(shop)).isEmpty();
        assertThat(reads(item)).isEqualTo(1);
    }

    /**
     * 第 110 条（BSM:1597「结算金额出账与调整……通知中写明金额、原因与异议截止日期」「异议期自该账单在商户专区可查看之日的次日起算」、
     * BSM:1673「月结定格 → 账单出账 → 异议期届满 → 付账日」、DEC-R8-55）：停车抵免成本入账不发短信，到出账日（visible_at）随结算金额出账
     * 短信通知承担商户，写明金额、原因与异议截止日（与商户专区门票分账列表同一截止日），重跑不重发。通知在提交后才发，本例不回滚。
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void costStatementSmsSentOnStatementDayWithReasonAndDeadline() throws Exception {
        String phone = "136" + String.format("%08d", Math.floorMod(f.suffix.hashCode(), 100_000_000));
        jdbc.update("UPDATE merchants SET contact_phone=? WHERE id=?", cipher.encrypt(phone), a[0]);
        job.run(Instant.now());
        String item = jdbc.queryForObject("SELECT id FROM ar_ap_items WHERE source_type='parking_credit_cost' AND project_id=? AND counterparty_id=?",
                String.class, f.project, a[0]);
        String monthId = jdbc.queryForObject("SELECT id FROM parking_credit_cost_months WHERE ar_ap_item_id=?", String.class, item);
        String[] requests = {"parking-credit-cost:" + monthId, "mamt:settlement_statement:" + item};
        assertThat(sms(requests)).as("入账不发").isEmpty();

        jdbc.update("UPDATE internal_payable_schedules SET visible_at = now() + interval '1 day' WHERE ar_ap_item_id=?", item);
        notice.notifyVisibleStatements(Instant.now());
        assertThat(sms(requests)).as("出账日未到不发").isEmpty();

        jdbc.update("UPDATE internal_payable_schedules SET visible_at = now() - interval '1 minute' WHERE ar_ap_item_id=?", item);
        notice.notifyVisibleStatements(Instant.now());
        notice.notifyVisibleStatements(Instant.now());
        String deadline = jdbc.queryForObject("SELECT objection_deadline::text FROM internal_payable_schedules WHERE ar_ap_item_id=?",
                String.class, item);
        String user = "usr_pcs_" + f.suffix;
        RequestPostProcessor shop = new StaffRequestFixture(jdbc).staff(user, "staff", null, a[0], "merchant.portal.settlement.view");
        jdbc.update("UPDATE roles SET role_name=? WHERE id=?", "停车抵免出账短信回归" + Long.parseLong(f.suffix, 16), "role_" + user);
        JsonNode listed = portal(get("/api/v1/merchant-portal/ticket-settlements"), shop);
        assertThat(listed.findValuesAsText("ar_ap_item_id")).containsExactly(item);
        assertThat(listed.get(0).path("objection_deadline").asText()).isEqualTo(deadline);
        long aShare = share(a[0]);
        assertThat(sms(requests)).singleElement().satisfies(r -> {
            assertThat(r.get("request_id")).isEqualTo(requests[1]);
            assertThat(r.get("domain")).isEqualTo("merchant_amount");
            assertThat(r.get("event_key")).isEqualTo("settlement_statement");
            assertThat(r.get("template_code")).isEqualTo("sms.merchant_amount.settlement_statement");
            assertThat(cipher.decrypt((String) r.get("recipient"))).isEqualTo(phone);
            assertThat(r.get("amount")).isEqualTo(String.valueOf(aShare));
            assertThat((String) r.get("reason")).contains("停车抵免成本出账：商户应付");
            assertThat(r.get("deadline")).isEqualTo(deadline);
        });
    }

    private List<Map<String, Object>> sms(String[] requests) {
        return jdbc.queryForList("""
                SELECT request_id, domain, event_key, template_code, recipient, params->>'amount_cent' AS amount, params->>'reason' AS reason,
                       params->>'objection_deadline' AS deadline FROM notification_send_log WHERE request_id IN (?,?)""", requests[0], requests[1]);
    }

    private JsonNode portal(MockHttpServletRequestBuilder request, RequestPostProcessor who) throws Exception {
        String body = mockMvc.perform(request.with(who)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return new ObjectMapper().readTree(body).path("data");
    }

    private Map<String, Long> billMonths(RequestPostProcessor who) throws Exception {
        Map<String, Long> months = new LinkedHashMap<>();
        portal(get("/api/v1/merchant-portal/unread-bills"), who).path("bill_months")
                .forEach(n -> months.put(n.path("month").asText(), n.path("unread_count").asLong()));
        return months;
    }

    private static long cost(JsonNode statement) {
        for (JsonNode fee : statement.path("fee_items")) {
            if ("停车抵免成本".equals(fee.path("fee_name").asText())) {
                return fee.path("amount_cent").asLong();
            }
        }
        throw new AssertionError("费用总表缺停车抵免成本");
    }

    private int reads(String item) {
        return jdbc.queryForObject("SELECT count(*) FROM merchant_bill_reads WHERE merchant_id=? AND bill_id=?", Integer.class, a[0], item);
    }

    private void used(String tag, List<Map.Entry<String, Long>> bills, String plate) throws Exception {
        String[] c = f.customer(customers, tag);
        Instant entered = Instant.now().minus(Duration.ofMinutes(140));
        for (Map.Entry<String, Long> bill : bills) {
            f.barBill(bill.getKey(), c[0], null, bill.getValue(), entered.plus(Duration.ofMinutes(1)));
        }
        String session = f.session("M" + tag + "_" + f.suffix, plate, entered.toString());
        mockMvc.perform(post("/api/v1/customer/parking/payments").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", c[1])
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
