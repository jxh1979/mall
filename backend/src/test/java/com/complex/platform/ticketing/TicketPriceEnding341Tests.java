package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.hamcrest.Matchers;
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
 * 第十八批 AW 段 341-R1（BSM:754「运营可在此基础上填一个近似售价用于凑整（使最终售价为整数，且尾数落在后台配置的允许尾数集合内——业务参数，全平台统一可配，
 * 出厂默认 8、9、0）」）：系统算出 174.25 元时，改平日价填 175 元（尾数 5）当场拒绝；定价核算回带「尾数合不合规」与允许尾数，供页面实时提示。
 * 后台票务策略页真实请求（蛇形键）与真实授权链，随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TicketPriceEnding341Tests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = String.valueOf(System.nanoTime());
    private final String restaurant = "rest_c341_" + s;
    private RequestPostProcessor admin;
    private String ticket;

    @BeforeEach
    void seed() throws Exception {
        String project = "prj_c341_" + s;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '凑整尾数综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, 'catering', 'active', ?, now(), now())", restaurant, "凑整尾数餐厅" + s, project);
        admin = new StaffRequestFixture(jdbc).staff("usr_c341_" + s, "platform_admin", "org_1", null, "ticket.product.manage",
                "ticket.product.view", "ticket.entitlement.manage");
        // 各业态价格 205 元、八五折：系统算出 174.25 元；先填 178 元（整元、尾数 8）。
        ticket = id(send(post("/api/v1/ticketing/ticket-types"), """
                {"idempotency_key": "c341-tt-%s", "ticket_name": "凑整尾数票%s", "base_calendar_price_mode": "weekday_weekend_holiday",
                 "weekday_price_cent": 17800, "weekend_price_cent": 19800, "holiday_price_cent": 19800, "stock_required": false,
                 "combo_discount_rate": 85}
                """.formatted(s, s)));
        String template = id(send(post("/api/v1/ticketing/entitlement-templates"), """
                {"idempotency_key": "c341-tpl-%s", "name": "凑整尾数入场%s", "entitlement_type": "entry", "default_total_times": 1,
                 "default_value_cent": 0, "status": "active", "merchant_id": "%s"}
                """.formatted(s, s, restaurant)));
        send(post("/api/v1/ticketing/ticket-types/" + ticket + "/entitlements"), """
                {"idempotency_key": "c341-bind-%s", "entitlement_template_id": "%s", "quantity": 1, "total_times": 1, "num_people": 1,
                 "component_price_cent": 20500}
                """.formatted(s, template)).andExpect(status().isOk());
    }

    @Test
    void roundedWeekdayPriceEndingOutsideAllowedSet_400_andPriceCheckShowsEndingVerdict() throws Exception {
        pricing("a", 17500L).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("TICKET_PRICE_ENDING_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("175.00")));
        pricing("b", 17450L).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("TICKET_PRICE_ENDING_NOT_ALLOWED"));

        mockMvc.perform(get("/api/v1/ticketing/ticket-types/" + ticket + "/price-check").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed_price_cent").value(17425))
                .andExpect(jsonPath("$.data.ending_allowed").value(true))
                .andExpect(jsonPath("$.data.rounding_allowed_endings").isArray());
        pricing("c", 17900L).andExpect(status().isOk()).andExpect(jsonPath("$.data.weekday_price_cent").value(17900));
    }

    private ResultActions pricing(String tag, long weekday) throws Exception {
        return send(put("/api/v1/ticketing/ticket-types/" + ticket + "/pricing"), """
                {"idempotency_key": "c341-p-%s-%s", "reason": "凑整", "combo_discount_rate": 85, "weekday_price_cent": %d,
                 "weekend_price_cent": 19800, "holiday_price_cent": 19800}
                """.formatted(tag, s, weekday));
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String body)
            throws Exception {
        return mockMvc.perform(request.with(admin).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String id(ResultActions result) throws Exception {
        return JSON.readTree(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("id").asText();
    }
}
