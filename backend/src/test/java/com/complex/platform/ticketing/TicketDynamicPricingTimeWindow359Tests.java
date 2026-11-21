package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BP 段第 359 条 359-R1（BSM:782「动态价按时段、容量等规则配置，可先试算再发布。」；BSM:756「先按日期、时段、场次和容量算动态公开价」；
 * BSM:780「时区一律取项目所在地时间，禁止使用服务器默认时区」；PGM:744「前端隐藏只是辅助，后端接口必须独立校验」）：动态价按每日时段配置，
 * 服务端按下单时刻（所在地时间）判是否落在时段内，后台、客人端都不传上下文；试算可指定试算时刻；条件只认规定的键，配错 400 且不落库。
 *
 * <p>真实接口与授权链（蛇形键）：可售票种与客人会话用 CustomerTicketPaymentFixture（已发布、每张 5000 分）；员工按后台动态定价页建规则、按后台下单。
 * 300-R1 起已上架未挂酒吧的票发布动态价只存进规则草稿版（审批流程由 DomainPassRuleVersion300Tests 覆盖），本类规则走接口建、置已发布直接改库。随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TicketDynamicPricingTimeWindow359Tests {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @MockitoBean private PaymentGatewayRouter gateway;

    private final String s = String.valueOf(System.nanoTime());
    private RequestPostProcessor staff;

    @BeforeEach
    void seed() {
        staff = new StaffRequestFixture(jdbc).staff("usr_c359_" + s, "platform_admin", "org_1", null,
                "ticket.dynamic_pricing.manage", "ticket.dynamic_pricing.simulate", "ticket.order.create");
    }

    /** a)、c)：时段含此刻、加价 10% 的已发布规则——后台下单与客人自助下单都不带上下文即命中（5500 分），客人端票种列表「我的价格」同为 5500 分。 */
    @Test
    void ruleWhoseTimeWindowCoversNowAppliesToStaffAndCustomerOrdersWithoutContext() throws Exception {
        CustomerTicketPaymentFixture f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        LocalTime now = LocalTime.now(LocalZone.ID);
        String rule = publish(rule(f.ticketTypeId, "此刻时段加价", Map.of("time_from", now.minusHours(1).format(HH_MM),
                "time_to", now.plusHours(1).format(HH_MM))).andExpect(status().isOk()));

        String staffOrder = body(staffOrder(f.ticketTypeId).andExpect(status().isOk()));
        assertThat(JsonPath.<String>read(staffOrder, "$.data.dynamic_pricing_rule_id")).isEqualTo(rule);
        assertThat(JsonPath.<Integer>read(staffOrder, "$.data.amount_cent")).isEqualTo(5500);

        String customerOrder = body(f.createOrder(1, Map.of()).andExpect(status().isOk()));
        assertThat(JsonPath.<String>read(customerOrder, "$.data.dynamic_pricing_rule_id")).isEqualTo(rule);
        String products = body(mockMvc.perform(get("/api/v1/customer/ticket-products").header("X-Terminal-Type", "miniapp")
                .header("X-Customer-Token", f.token)).andExpect(status().isOk()));
        assertThat(JsonPath.<List<Integer>>read(products, "$.data.items[?(@.ticket_type_id == '" + f.ticketTypeId + "')].my_price_cent"))
                .containsExactly(5500);
    }

    /** b)：时段不含此刻（此刻后两小时起一小时）的已发布规则不命中，后台下单按基础价 5000 分。 */
    @Test
    void ruleWhoseTimeWindowExcludesNowDoesNotApply() throws Exception {
        CustomerTicketPaymentFixture f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        LocalTime now = LocalTime.now(LocalZone.ID);
        publish(rule(f.ticketTypeId, "稍后时段加价", Map.of("time_from", now.plusHours(2).format(HH_MM),
                "time_to", now.plusHours(3).format(HH_MM))).andExpect(status().isOk()));

        String order = body(staffOrder(f.ticketTypeId).andExpect(status().isOk()));
        assertThat(JsonPath.<Integer>read(order, "$.data.amount_cent")).isEqualTo(5000);
        assertThat(JsonPath.<String>read(order, "$.data.dynamic_pricing_rule_id")).isNull();
    }

    /** d)：跨零点时段 22:00～02:00 的草稿规则，试算时刻 01:30 命中、12:00 不命中且说明为中文。 */
    @Test
    void simulationUsesTheGivenTimeAgainstAnOvernightWindow() throws Exception {
        CustomerTicketPaymentFixture f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        String rule = id(rule(f.ticketTypeId, "夜场加价", Map.of("time_from", "22:00", "time_to", "02:00")).andExpect(status().isOk()));

        simulate(f.ticketTypeId, "01:30").andExpect(status().isOk()).andExpect(jsonPath("$.data.matched_rule_id").value(rule));
        simulate(f.ticketTypeId, "12:00").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matched_rule_id").doesNotExist())
                .andExpect(jsonPath("$.data.explanation").value("未命中动态定价规则"));
    }

    /** e)：只填一端、时刻写错、带不认得的键、两端相同——一律 400 VALIDATION_FAILED，规则不落库。 */
    @Test
    void malformedConditionsAreRejectedAndNothingIsStored() throws Exception {
        CustomerTicketPaymentFixture f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        for (Map<String, Object> condition : List.<Map<String, Object>>of(
                Map.of("time_from", "20:00"),
                Map.of("time_from", "25:00", "time_to", "02:00"),
                Map.of("inventory_level", "low"),
                Map.of("time_from", "20:00", "time_to", "20:00"))) {
            rule(f.ticketTypeId, "配错的条件", condition).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_dynamic_pricing_rules WHERE ticket_type_id = ?", Integer.class,
                f.ticketTypeId)).isZero();
    }

    /** 后台动态定价页建规则的请求形状：所在地今天前后各一天生效、全部渠道、按比例加价 10%。 */
    private ResultActions rule(String ticketTypeId, String name, Map<String, Object> condition) throws Exception {
        LocalDate today = LocalDate.now(LocalZone.ID);
        return mockMvc.perform(post("/api/v1/ticketing/dynamic-pricing-rules").with(staff).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "c359-dr-" + UUID.randomUUID(), "ticket_type_id", ticketTypeId,
                        "rule_name", name, "priority", 10, "effective_from", today.minusDays(1).toString(),
                        "effective_to", today.plusDays(1).toString(), "adjustment_type", "percent_delta", "adjustment_rate", 10,
                        "condition_rule", condition))));
    }

    private String publish(ResultActions created) throws Exception {
        String rule = id(created);
        jdbc.update("UPDATE ticket_dynamic_pricing_rules SET status = 'published' WHERE id = ?", rule);
        return rule;
    }

    private ResultActions staffOrder(String ticketTypeId) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/orders").with(staff).contentType(MediaType.APPLICATION_JSON).content("""
                {"idempotency_key": "c359-order-%s", "ticket_type_id": "%s", "quantity": 1, "sales_channel": "admin"}
                """.formatted(UUID.randomUUID(), ticketTypeId)));
    }

    private ResultActions simulate(String ticketTypeId, String atTime) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/dynamic-pricing/simulations").with(staff).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"idempotency_key": "c359-sim-%s", "ticket_type_id": "%s", "sales_channel": "admin", "context": {"at_time": "%s"}}
                        """.formatted(UUID.randomUUID(), ticketTypeId, atTime)));
    }

    private static String id(ResultActions result) throws Exception {
        return JsonPath.read(body(result), "$.data.id");
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
