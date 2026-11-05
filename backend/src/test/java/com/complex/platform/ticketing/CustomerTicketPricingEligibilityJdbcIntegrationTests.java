package com.complex.platform.ticketing;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-03-R27（BSM 第 733 行「符合后台规则时系统自动判断可用，不符合规则时不能使用」；第 756 行与 ENUM 第 58 行「再计算会员、新人、学生、团体和渠道等
 * 全部符合条件的价格；多种资格价不叠加，选择最终价格最低的一项。订单保存基础价、动态价、全部命中资格价、最终选中价和规则版本」；第 781 行新人判定；
 * 第 821 行各等级均有「票务会员价购买资格」；第 1301 行团体需主管审批）：客人自助购票由系统判资格取最低，不得自选价格规则；团体、渠道价不对客人自助开放。
 * 票价 5000 分；会员价 8 折、新人价 7 折、学生价 5 折、团体 4 折、渠道 3 折。客人端请求按真实形状（蛇形键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class CustomerTicketPricingEligibilityJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private JdbcTicketingRepository repository;
    @MockitoBean private PaymentGatewayRouter gateway;

    private CustomerTicketPaymentFixture f;

    @BeforeEach
    void seed() {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
    }

    @Test
    void clientSuppliedPriceRuleRejected() throws Exception {
        String student = rule("student", 50);
        f.createOrder(1, Map.of("price_rule_id", student)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_orders WHERE ticket_type_id = ?", Integer.class, f.ticketTypeId))
                .isZero();
    }

    @Test
    void memberPriceAutoSelectedWhenLowest() throws Exception {
        String member = rule("member", 80);
        String orderId = f.createOrderId(1);
        assertThat(order(orderId)).containsEntry("amount_cent", 4000L).containsEntry("price_rule_id", member);
    }

    @Test
    void groupAndChannelRulesNeverApplyToCustomer() throws Exception {
        String member = rule("member", 80);
        rule("group", 40);
        rule("channel", 30);
        rule("student", 50);
        String orderId = f.createOrderId(2);
        assertThat(order(orderId)).containsEntry("amount_cent", 8000L).containsEntry("price_rule_id", member);
    }

    @Test
    void newCustomerPriceOnlyBeforeAnyPaidTicketOrder() throws Exception {
        rule("member", 80);
        String fresh = rule("new_customer", 70);
        String first = f.createOrderId(1);
        assertThat(order(first)).containsEntry("amount_cent", 3500L).containsEntry("price_rule_id", fresh);
        jdbc.update("UPDATE ticket_orders SET pay_status = 'paid', paid_at = now() WHERE id = ?", first);
        String second = f.createOrderId(1);
        assertThat(order(second)).containsEntry("amount_cent", 4000L);
    }

    @Test
    void pendingNewCustomerOrderCountsAsBoughtUntilCancelled() throws Exception {
        rule("member", 80);
        String fresh = rule("new_customer", 70);
        String first = f.createOrderId(1);
        assertThat(order(first)).containsEntry("price_rule_id", fresh);
        // BSM 第 781 行「同一手机号没买过才算新人」：已按新人价下了一笔待付单，第二笔不能再按新人价成交。
        String second = f.createOrderId(1);
        assertThat(order(second)).containsEntry("amount_cent", 4000L);
        // 第一笔取消了（没买成），仍是新人。
        jdbc.update("UPDATE ticket_orders SET status = 'cancelled' WHERE id IN (?, ?)", first, second);
        assertThat(order(f.createOrderId(1))).containsEntry("price_rule_id", fresh);
    }

    @Test
    void reRegisteredWithinOneYearNotNewCustomer() throws Exception {
        rule("member", 80);
        rule("new_customer", 70);
        deregisteredDaysBeforeRegistration(100);
        assertThat(order(f.createOrderId(1))).containsEntry("amount_cent", 4000L);
    }

    @Test
    void reRegisteredAfterOneYearIsNewCustomerAgain() throws Exception {
        rule("member", 80);
        String fresh = rule("new_customer", 70);
        deregisteredDaysBeforeRegistration(400);
        assertThat(order(f.createOrderId(1))).containsEntry("amount_cent", 3500L).containsEntry("price_rule_id", fresh);
    }

    @Test
    void orderStoresAllHitPrices() throws Exception {
        String member = rule("member", 80);
        String fresh = rule("new_customer", 70);
        rule("group", 40);
        f.createOrderId(1);
        String body = f.listOrders().andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Map<String, Object>> hits = JsonPath.read(body, "$.data.items[0].price_hits");
        assertThat(hits).extracting(h -> h.get("rule_id")).containsExactlyInAnyOrder(member, fresh);
        assertThat(hits).extracting(h -> h.get("unit_price_cent")).containsExactlyInAnyOrder(4000, 3500);
        assertThat(hits).allSatisfy(h -> assertThat(h.get("rule_updated_at")).isNotNull());
    }

    @Test
    void ticketProductsShowMyPriceWhenLoggedIn() throws Exception {
        rule("member", 80);
        rule("new_customer", 70);
        String body = mockMvc.perform(get("/api/v1/customer/ticket-products").header("X-Terminal-Type", "miniapp")
                        .header("X-Customer-Token", f.token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Integer> prices = JsonPath.read(body, "$.data.items[?(@.ticket_type_id == '" + f.ticketTypeId + "')].my_price_cent");
        List<String> basis = JsonPath.read(body, "$.data.items[?(@.ticket_type_id == '" + f.ticketTypeId + "')].my_price_basis");
        assertThat(prices).containsExactly(3500);
        assertThat(basis).containsExactly("新人价");
    }

    @Test
    void adminCreatesMemberPriceRuleThroughEntry() throws Exception {
        // 170-R2（契约 TicketPriceRuleRequest.variant_type 补 member）：后台按 TicketingPoliciesPage.createRule 的蛇形键建会员价规则。
        RequestPostProcessor manager = new StaffRequestFixture(jdbc).staff("usr_b14_rule_" + f.suffix, "staff", f.projectId, null,
                "ticket.price_variant.manage");
        String id = CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/ticketing/price-rules").with(manager)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "b14-rule-member-" + f.suffix, "ticket_type_id", f.ticketTypeId,
                                "variant_type", "member", "price_mode", "derive_from_calendar_price", "discount_rate", 80))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.variant_type").value("member")), "$.data.id");
        assertThat(jdbc.queryForObject("SELECT variant_type FROM ticket_price_rules WHERE id = ?", String.class, id)).isEqualTo("member");
    }

    private String rule(String variant, int discount) {
        String id = "price_rule_b14_" + variant + "_" + f.suffix;
        Instant now = Instant.now();
        repository.savePriceRule(new TicketPriceRuleRecord(id, f.ticketTypeId, variant, "derive_from_calendar_price",
                null, null, null, BigDecimal.valueOf(discount), null, "CNY", "active", Map.of(), now, now));
        return id;
    }

    private void deregisteredDaysBeforeRegistration(int days) {
        jdbc.update("""
                INSERT INTO customer_deregistration_marks (id, phone_hash, registered_at, deregistered_at, purge_after)
                SELECT ?, phone_hash, created_at - INTERVAL '800 days', created_at - make_interval(days => ?), now() + INTERVAL '30 days'
                  FROM customers WHERE id = ?
                """, "cdm_b14_" + f.suffix, days, f.customerId);
    }

    private Map<String, Object> order(String orderId) {
        return jdbc.queryForMap("SELECT amount_cent, price_rule_id FROM ticket_orders WHERE id = ?", orderId);
    }
}
