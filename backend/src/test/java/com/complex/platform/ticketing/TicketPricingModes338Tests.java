package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
 * 第十八批 AW 段 338-R1、338-R2（BSM:782「会员价、渠道价、动态价都在对应基础价上按打折/减免/加价/固定额派生；动态价按时段、容量等规则配置，可先试算再发布」）：
 * 派生价可按基础价加减金额（负数为减免、正数为加价），金额必填且不为 0；动态价可按固定价（固定额），金额必填，命中即公开价为该固定价、仍受最高价夹值。
 * 后台票务策略页、动态定价页真实请求（蛇形键）与真实授权链，随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TicketPricingModes338Tests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = String.valueOf(System.nanoTime());
    private RequestPostProcessor admin;
    private String ticket;

    @BeforeEach
    void seed() throws Exception {
        admin = new StaffRequestFixture(jdbc).staff("usr_c338_" + s, "platform_admin", "org_1", null, "ticket.product.manage",
                "ticket.product.view", "ticket.price_variant.manage", "ticket.dynamic_pricing.manage", "ticket.dynamic_pricing.simulate");
        String body = send("/api/v1/ticketing/ticket-types", """
                {"idempotency_key": "c338-tt-%s", "ticket_name": "定价口径票%s", "base_calendar_price_mode": "weekday_weekend_holiday",
                 "weekday_price_cent": 9900, "weekend_price_cent": 9900, "holiday_price_cent": 9900, "stock_required": false}
                """.formatted(s, s)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        ticket = JSON.readTree(body).path("data").path("id").asText();
    }

    @Test
    void amountDeltaPriceRuleCreated_missingOrZeroAmount400() throws Exception {
        priceRule("a", "-1000").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.price_mode").value("amount_delta"))
                .andExpect(jsonPath("$.data.adjustment_amount_cent").value(-1000));
        priceRule("b", null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        priceRule("c", "0").andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    @Test
    void dynamicFixedPriceSimulatesToTheFixedPriceWithinMaxPrice() throws Exception {
        dynamicRule("a", 10, "19900", null).andExpect(status().isOk()).andExpect(jsonPath("$.data.adjustment_type").value("fixed_price"));
        simulate().andExpect(status().isOk()).andExpect(jsonPath("$.data.final_price_cent").value(19900));
        dynamicRule("b", 20, "19900", 18000L).andExpect(status().isOk());
        simulate().andExpect(status().isOk()).andExpect(jsonPath("$.data.final_price_cent").value(18000));
        dynamicRule("c", 30, null, null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    @Test
    void simulationExplanationIsChineseWhetherMatchedOrNot() throws Exception {
        // 18AW 收尾复核修正（PGM:287「展示给用户的一切文字，包括……状态、枚举取值……必须全部为中文，不得出现任何英文」）：试算说明在后台结果区上屏，未命中与命中都用中文。
        simulate().andExpect(status().isOk()).andExpect(jsonPath("$.data.explanation").value("未命中动态定价规则"));
        dynamicRule("a", 10, "19900", null).andExpect(status().isOk());
        simulate().andExpect(status().isOk()).andExpect(jsonPath("$.data.explanation").value("命中规则：节假日晚场固定价a"));
    }

    private ResultActions priceRule(String tag, String amount) throws Exception {
        return send("/api/v1/ticketing/price-rules", """
                {"idempotency_key": "c338-pr-%s-%s", "ticket_type_id": "%s", "variant_type": "member", "price_mode": "amount_delta",
                 "adjustment_amount_cent": %s}
                """.formatted(tag, s, ticket, amount));
    }

    private ResultActions dynamicRule(String tag, int priority, String amount, Long maxPrice) throws Exception {
        return send("/api/v1/ticketing/dynamic-pricing-rules", """
                {"idempotency_key": "c338-dr-%s-%s", "ticket_type_id": "%s", "rule_name": "节假日晚场固定价%s", "priority": %d,
                 "effective_from": "%s", "effective_to": "%s", "sales_channel": "admin", "adjustment_type": "fixed_price",
                 "adjustment_amount_cent": %s, "max_price_cent": %s}
                """.formatted(tag, s, ticket, tag, priority, LocalDate.now().minusDays(1), LocalDate.now().plusDays(30), amount, maxPrice));
    }

    private ResultActions simulate() throws Exception {
        return send("/api/v1/ticketing/dynamic-pricing/simulations", """
                {"idempotency_key": "c338-sim-%s", "ticket_type_id": "%s", "sales_channel": "admin"}
                """.formatted(System.nanoTime(), ticket));
    }

    private ResultActions send(String path, String body) throws Exception {
        return mockMvc.perform(post(path).with(admin).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
