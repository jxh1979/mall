package com.complex.platform.integration;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.complex.platform.support.TicketSalesContractTestFixtures;
import com.complex.platform.ticketing.TicketTypeEntitlementRecord;
import com.complex.platform.ticketing.TicketTypeRecord;
import com.complex.platform.ticketing.TicketingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 S 段第 172 条 172-R1（BSM:749「单店酒吧门票由销售该票的酒吧商户承担……经第三方渠道售出也不改变该归属」；
 * ENUM:32「客人可通过……获授权第三方渠道……购票。单一酒吧门票由对应酒吧商户定价、销售」；
 * BSM:1992「美团、大众点评、抖音先接团购购票、订单回流、核销和对账」）：
 * 渠道回流的单店票按票种所属酒吧走原出票链出票，订单、票实例、权益都归该酒吧；同一渠道单号重复回流不重复出票。
 * 渠道门店映射、渠道票种映射与回流登记按后台真实请求形状（蛇形键）走真实入口与真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class ChannelBarTicketReflowJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private TicketingRepository ticketing;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "cbr_prj_" + s;
    private final String bar = "cbr_bar_" + s;
    private final String ticketType = "cbr_tt_" + s;
    private final String provider = "cbr-pos-" + s;

    @Test
    void channelReflowOfSingleBarTicketIssuesUnderTicketTypeBar() throws Exception {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','渠道单店票回流综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'渠道单店票回流酒吧','bar','active',?,now(),now())",
                bar, project);
        Instant now = Instant.now();
        ticketing.saveTicketType(new TicketTypeRecord(
                ticketType, "渠道回流单店票", "fixed", 8800L, 8800L, 8800L,
                false, "published", 2, null, now, now, null, 0, null, false, bar, TicketTypeRecord.NO_COMBO_DISCOUNT));
        jdbc.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times,
                    default_value_cent, status, merchant_id, created_at, updated_at)
                VALUES (?, '入场', 'entry', 1, 0, 'active', ?, now(), now())
                """, "cbr_et_" + s, bar);
        ticketing.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "cbr_tte_" + s, ticketType, "cbr_et_" + s, 1, 1, 8800L, null, 1, 8800L, now, now));
        TicketSalesContractTestFixtures.approvedPublished(jdbc, json, ticketType, project, bar);
        jdbc.update("UPDATE terminal_capability_switches SET enabled = TRUE WHERE capability_code = 'integration.ticket_channel.sync'");
        RequestPostProcessor admin = new StaffRequestFixture(jdbc).staff("usr_cbr_admin_" + s, "platform_admin", project, null,
                "integration.channel_store_map.manage", "integration.order_sync.manage", "ticket.order.create",
                "finance.payment_intent.create", "finance.payment.register", "ticket.ticket.issue");

        send(admin, "/api/v1/integration/channel-store-maps", Map.of("idempotency_key", "cbr-store-" + s,
                "provider_code", provider, "channel", "meituan", "external_store_id", "mt-store-" + s, "merchant_id", bar));
        send(admin, "/api/v1/integration/channel-ticket-type-maps", Map.of("idempotency_key", "cbr-product-" + s,
                "provider_code", provider, "channel", "meituan", "external_product_id", "mt-prod-" + s, "ticket_type_id", ticketType));

        JsonNode first = reflow(admin, "cbr-reflow-" + s);
        assertThat(first.path("status").asText()).isEqualTo("synced");
        String orderId = first.path("provider_payload").path("local_ticket_order_id").asText();
        assertThat(orderId).as("渠道回流的单店票本地出票").isNotBlank();
        assertThat(jdbc.queryForObject("SELECT bar_id FROM ticket_orders WHERE id = ?", String.class, orderId))
                .as("单店票经第三方渠道售出不改变归属（BSM:749）").isEqualTo(bar);
        assertThat(jdbc.queryForList("SELECT bar_id FROM ticket_instances WHERE order_id = ?", String.class, orderId))
                .isNotEmpty().containsOnly(bar);
        assertThat(jdbc.queryForList("SELECT bar_id FROM issued_entitlements WHERE order_id = ?", String.class, orderId))
                .isNotEmpty().containsOnly(bar);

        JsonNode again = reflow(admin, "cbr-reflow-again-" + s);
        assertThat(again.path("provider_payload").path("local_ticket_order_id").asText())
                .as("同一渠道单号重复回流返回同一本地订单").isEqualTo(orderId);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_orders WHERE ticket_type_id = ?", Integer.class, ticketType))
                .as("重复回流不重复出票").isEqualTo(1);
    }

    private JsonNode reflow(RequestPostProcessor who, String idempotencyKey) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", idempotencyKey);
        body.put("provider_code", provider);
        body.put("business_type", "ticket");
        body.put("sync_type", "order");
        body.put("external_business_id", "mt-order-" + s);
        body.put("status", "received");
        body.put("amount_cent", 8800);
        body.put("currency", "CNY");
        body.put("channel", "meituan");
        body.put("external_channel_order_id", "mtord-" + s);
        body.put("provider_payload", Map.of("external_store_id", "mt-store-" + s, "external_product_id", "mt-prod-" + s, "quantity", 1));
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/integration/order-syncs").with(who)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andReturn().getResponse();
        String content = response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(response.getStatus()).as(content).isEqualTo(200);
        return json.readTree(content).path("data");
    }

    private void send(RequestPostProcessor who, String path, Map<String, Object> body) throws Exception {
        mockMvc.perform(post(path).with(who).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk());
    }
}
