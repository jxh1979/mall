package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.LocalZone;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BP 段第 359 条 359-R3（BSM:782「动态价按时段、容量等规则配置，可先试算再发布。」；BSM:756「先按日期、时段、场次和容量算动态公开价」；
 * 第十八批裁决 75「按当天已售张数」：规则里填「到访当天已售满多少张起」，不要求票种设限量）：已售张数按该单到访日统计该票种已售张数（与票种限量的已售数
 * 同一口径：未取消订单的张数减已退的票，不含本单）；试算可填当天已售张数。
 *
 * <p>真实接口与授权链（蛇形键）：可售票种用 CustomerTicketPaymentFixture（已发布、不限量、每张 5000 分）；员工按后台动态定价页建规则、按后台下单。
 * 规则置已发布直接改库（同 TicketDynamicPricingTimeWindow359Tests）。随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TicketDynamicPricingDaySold359Tests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @MockitoBean private PaymentGatewayRouter gateway;

    private final String s = String.valueOf(System.nanoTime());
    private final LocalDate today = LocalDate.now(LocalZone.ID);
    private RequestPostProcessor staff;
    private CustomerTicketPaymentFixture f;

    @BeforeEach
    void seed() {
        staff = new StaffRequestFixture(jdbc).staff("usr_c359s_" + s, "platform_admin", "org_1", null,
                "ticket.dynamic_pricing.manage", "ticket.dynamic_pricing.simulate", "ticket.order.create", "ticket.ticket.issue");
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
    }

    /**
     * 「到访当天已售满 5 张起」加价 10%：明天到访的 5 张不算今天的；今天第一单 5 张（下单前今天已售 0）按基础价；今天再买 1 张（已售 5）命中、5500 分。
     * 票种不限量也能配。
     */
    @Test
    void ordersAfterTheVisitDaySoldThresholdTakeTheDynamicPrice() throws Exception {
        String rule = id(rule(5).andExpect(status().isOk()));
        jdbc.update("UPDATE ticket_dynamic_pricing_rules SET status = 'published' WHERE id = ?", rule);

        String tomorrow = body(order(5, today.plusDays(1)).andExpect(status().isOk()));
        assertThat(JsonPath.<String>read(tomorrow, "$.data.dynamic_pricing_rule_id")).isNull();
        String first = body(order(5, today).andExpect(status().isOk()));
        assertThat(JsonPath.<String>read(first, "$.data.dynamic_pricing_rule_id")).isNull();
        assertThat(JsonPath.<Integer>read(first, "$.data.amount_cent")).isEqualTo(25000);

        String second = body(order(1, today).andExpect(status().isOk()));
        assertThat(JsonPath.<String>read(second, "$.data.dynamic_pricing_rule_id")).isEqualTo(rule);
        assertThat(JsonPath.<Integer>read(second, "$.data.amount_cent")).isEqualTo(5500);
    }

    /** 已取消的订单不计（同票种限量：取消即回补）：今天到访先下 5 张、经「取消未支付订单」入口取消；再下 1 张时当天已售 0，按基础价、不命中。 */
    @Test
    void cancelledOrdersAreNotCountedInTheDaySold() throws Exception {
        String rule = id(rule(5).andExpect(status().isOk()));
        jdbc.update("UPDATE ticket_dynamic_pricing_rules SET status = 'published' WHERE id = ?", rule);

        String cancelled = id(order(5, today).andExpect(status().isOk()));
        mockMvc.perform(post("/api/v1/ticketing/orders/" + cancelled + "/cancel").with(staff).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "c359s-cancel-%s", "reason": "客人没有付款"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("cancelled"));

        String next = body(order(1, today).andExpect(status().isOk()));
        assertThat(JsonPath.<String>read(next, "$.data.dynamic_pricing_rule_id")).isNull();
        assertThat(JsonPath.<Integer>read(next, "$.data.amount_cent")).isEqualTo(5000);
    }

    /**
     * 已退的票扣回（同票种限量：退一张放一张）：今天到访 5 张出票后退掉其中 1 张（该票实例置已退）；再下 1 张时当天已售 4，按基础价、不命中；
     * 再下 1 张时已售 5，命中、5500 分。
     */
    @Test
    void refundedTicketsAreTakenOutOfTheDaySold() throws Exception {
        String rule = id(rule(5).andExpect(status().isOk()));
        jdbc.update("UPDATE ticket_dynamic_pricing_rules SET status = 'published' WHERE id = ?", rule);

        String issued = id(order(5, today).andExpect(status().isOk()));
        mockMvc.perform(post("/api/v1/ticketing/orders/" + issued + "/issue").with(staff).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "c359s-issue-%s", "reason": "客人在前台购票"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_instances WHERE order_id = ?", Integer.class, issued)).isEqualTo(5);
        jdbc.update("UPDATE ticket_instances SET status = 'refunded' WHERE id = (SELECT min(id) FROM ticket_instances WHERE order_id = ?)", issued);

        String below = body(order(1, today).andExpect(status().isOk()));
        assertThat(JsonPath.<String>read(below, "$.data.dynamic_pricing_rule_id")).isNull();
        assertThat(JsonPath.<Integer>read(below, "$.data.amount_cent")).isEqualTo(5000);
        String reached = body(order(1, today).andExpect(status().isOk()));
        assertThat(JsonPath.<String>read(reached, "$.data.dynamic_pricing_rule_id")).isEqualTo(rule);
        assertThat(JsonPath.<Integer>read(reached, "$.data.amount_cent")).isEqualTo(5500);
    }

    /** 试算填当天已售 8 张命中、2 张不命中；门槛不是不小于 1 的整数 400 VALIDATION_FAILED。 */
    @Test
    void simulationTakesTheGivenDaySoldAndThresholdMustBeAPositiveInteger() throws Exception {
        String rule = id(rule(5).andExpect(status().isOk()));
        simulate(8).andExpect(status().isOk()).andExpect(jsonPath("$.data.matched_rule_id").value(rule));
        simulate(2).andExpect(status().isOk()).andExpect(jsonPath("$.data.matched_rule_id").doesNotExist());

        rule(0).andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    private ResultActions rule(int daySoldMin) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "c359s-dr-" + UUID.randomUUID());
        body.put("ticket_type_id", f.ticketTypeId);
        body.put("rule_name", "当天卖多加价");
        body.put("priority", 10);
        body.put("effective_from", today.minusDays(1).toString());
        body.put("effective_to", today.plusDays(1).toString());
        body.put("adjustment_type", "percent_delta");
        body.put("adjustment_rate", 10);
        body.put("condition_rule", Map.of("day_sold_min", daySoldMin));
        return mockMvc.perform(post("/api/v1/ticketing/dynamic-pricing-rules").with(staff).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    private ResultActions order(int quantity, LocalDate visitDate) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/orders").with(staff).contentType(MediaType.APPLICATION_JSON).content("""
                {"idempotency_key": "c359s-order-%s", "ticket_type_id": "%s", "quantity": %d, "visit_date": "%s", "sales_channel": "admin"}
                """.formatted(UUID.randomUUID(), f.ticketTypeId, quantity, visitDate)));
    }

    private ResultActions simulate(int daySold) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/dynamic-pricing/simulations").with(staff).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"idempotency_key": "c359s-sim-%s", "ticket_type_id": "%s", "visit_date": "%s", "sales_channel": "admin",
                         "context": {"day_sold": %d}}
                        """.formatted(UUID.randomUUID(), f.ticketTypeId, today, daySold)));
    }

    private static String id(ResultActions result) throws Exception {
        return JsonPath.read(body(result), "$.data.id");
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
