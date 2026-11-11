package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AF 段第 198 条 198-R5（票务）：契约删去 TicketPriceRuleRequest.operator_note、evidence_attachment_ids（代码不读、没有客户端发送）后，
 * 票务管理员按票务策略页（TicketingPoliciesPage.createRule）固定价的真实请求形状——idempotency_key、ticket_type_id、variant_type、price_mode、
 * discount_rate（null）、weekday_price_cent、weekend_price_cent、holiday_price_cent——走真实 HTTP、真实授权链建派生价规则。
 * 入口未改，本类只锁现行为，不先红。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TicketPriceRuleCreateWebShape198R5Tests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = String.valueOf(System.nanoTime());

    // 策略页固定价新形状（不带 operator_note、evidence_attachment_ids）：200，规则落库为固定价、三档价格照填、折扣为空。
    @Test
    void createTicketPriceRule_webShapeFixedPriceWithoutNoteAndEvidence_ok() throws Exception {
        String project = "prj_b52tp_" + s;
        String ticketType = "tt_b52tp_" + s;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '派生价形状综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent,"
                + " holiday_price_cent, stock_required, status, validity_days, created_at, updated_at)"
                + " VALUES (?, '派生价形状门票', 'flat', 5000, 5000, 5000, FALSE, 'published', 1, now(), now())", ticketType);
        RequestPostProcessor manager = new StaffRequestFixture(jdbc).staff("usr_b52tp_" + s, "staff", project, null,
                "ticket.price_variant.manage");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "b52tp-rule-" + s);
        body.put("ticket_type_id", ticketType);
        body.put("variant_type", "student");
        body.put("price_mode", "fixed_price");
        body.put("discount_rate", null);
        body.put("weekday_price_cent", 3000);
        body.put("weekend_price_cent", 3000);
        body.put("holiday_price_cent", 3000);
        String id = JsonPath.read(mockMvc.perform(post("/api/v1/ticketing/price-rules").with(manager)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticket_type_id").value(ticketType))
                .andExpect(jsonPath("$.data.price_mode").value("fixed_price"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        assertThat(jdbc.queryForMap("SELECT ticket_type_id, variant_type, price_mode, weekday_price_cent, holiday_price_cent, discount_rate"
                        + " FROM ticket_price_rules WHERE id = ?", id))
                .containsEntry("ticket_type_id", ticketType)
                .containsEntry("variant_type", "student")
                .containsEntry("price_mode", "fixed_price")
                .containsEntry("weekday_price_cent", 3000L)
                .containsEntry("holiday_price_cent", 3000L)
                .containsEntry("discount_rate", null);
    }
}
