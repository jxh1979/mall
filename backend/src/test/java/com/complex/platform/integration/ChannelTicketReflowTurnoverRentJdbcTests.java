package com.complex.platform.integration;

// 文件版本：V0.03

import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.support.StaffRequestFixture;
import com.complex.platform.support.TicketSalesContractTestFixtures;
import com.complex.platform.ticketing.TicketTypeEntitlementRecord;
import com.complex.platform.ticketing.TicketTypeRecord;
import com.complex.platform.ticketing.TicketingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
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
 * 第十八批 Q 段第 138 条 138-R2（BSM:1347「凡已计收过门票综合服务费的部分——包括通票或券核销划归本商户的部分，以及单店门票的票款——
 * 一律不再计入抽成基数，同一笔钱不两头收费」；BSM:1330「连接第三方接入，餐饮和零售商户的 POS 结果回流到平台」；BSM:1548「回流订单按该映射认定商户」）：
 * 第三方渠道（映射到该酒吧的渠道门店）售出的门票回流按渠道门店映射落到该酒吧商户、本地出票后，这笔回流不进该酒吧的营业额抽成基数。
 * 渠道门店映射、渠道票种映射与回流登记按后台真实请求形状（蛇形键）走真实入口与真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class ChannelTicketReflowTurnoverRentJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private TicketingRepository ticketing;
    @Autowired private FinanceService finance;
    @Autowired private BusinessDayResolver days;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "ctr_prj_" + s;
    private final String bar = "ctr_bar_" + s;
    private final String ticketType = "ctr_tt_" + s;
    private final String provider = "ctr-pos-" + s;
    private final String contract = "ctr_contract_" + s;

    /** 映射到该酒吧的美团门店售出门票 500 元 → 本地出票、回流行 synced 且归该酒吧 → 该酒吧当月营业额抽成（10%、无保底）为 0。 */
    @Test
    void channelSoldTicketReflowStaysOutOfBarTurnoverRentBase() throws Exception {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','渠道门票抽成综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'渠道门票抽成酒吧','bar','active',?,now(),now())",
                bar, project);
        Instant now = Instant.now();
        ticketing.saveTicketType(new TicketTypeRecord(
                ticketType, "渠道门票抽成回归通票", "fixed", 50000L, 50000L, 50000L,
                false, "published", 2, null, now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
        jdbc.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times,
                    default_value_cent, status, merchant_id, created_at, updated_at)
                VALUES (?, '入场', 'entry', 1, 0, 'active', ?, now(), now())
                """, "ctr_et_" + s, bar);
        ticketing.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "ctr_tte_" + s, ticketType, "ctr_et_" + s, 1, 1, 50000L, null, 1, 50000L, now, now));
        TicketSalesContractTestFixtures.approvedPublished(jdbc, json, ticketType, project, bar);
        jdbc.update("INSERT INTO channel_fee_configs (id, project_id, channel, fee_bps, is_draft) VALUES (?, ?, 'meituan', 1000, FALSE)",
                "ctr_cfc_" + s, project);
        jdbc.update("UPDATE terminal_capability_switches SET enabled = TRUE WHERE capability_code = 'integration.ticket_channel.sync'");
        jdbc.update("""
                INSERT INTO turnover_rent_rules
                    (id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent, effective_from, effective_to, status, is_draft)
                VALUES (?, ?, ?, '渠道门票回流抽成回归规则', 1000, 0, DATE '2020-01-01', NULL, 'active', FALSE)
                """, "ctr_rule_" + s, contract, bar);
        RequestPostProcessor admin = new StaffRequestFixture(jdbc).staff("usr_ctr_admin_" + s, "platform_admin", project, null,
                "integration.channel_store_map.manage", "integration.order_sync.manage", "ticket.order.create",
                "finance.payment_intent.create", "finance.payment.register", "ticket.ticket.issue");

        send(admin, "/api/v1/integration/channel-store-maps", Map.of("idempotency_key", "ctr-store-" + s,
                "provider_code", provider, "channel", "meituan", "external_store_id", "mt-store-" + s, "merchant_id", bar));
        send(admin, "/api/v1/integration/channel-ticket-type-maps", Map.of("idempotency_key", "ctr-product-" + s,
                "provider_code", provider, "channel", "meituan", "external_product_id", "mt-prod-" + s, "ticket_type_id", ticketType));
        Map<String, Object> reflow = new LinkedHashMap<>();
        reflow.put("idempotency_key", "ctr-reflow-" + s);
        reflow.put("provider_code", provider);
        reflow.put("business_type", "ticket");
        reflow.put("sync_type", "order");
        reflow.put("external_business_id", "mt-order-" + s);
        reflow.put("status", "received");
        reflow.put("amount_cent", 50000);
        reflow.put("currency", "CNY");
        reflow.put("channel", "meituan");
        reflow.put("external_channel_order_id", "mtord-" + s);
        reflow.put("provider_payload", Map.of("external_store_id", "mt-store-" + s, "external_product_id", "mt-prod-" + s, "quantity", 1));
        mockMvc.perform(post("/api/v1/integration/order-syncs").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(reflow)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("synced"))
                .andExpect(jsonPath("$.data.merchant_id").value(bar))
                .andExpect(jsonPath("$.data.provider_payload.local_ticket_order_id").isNotEmpty());
        assertThat(jdbc.queryForList("SELECT bar_id FROM ticket_orders WHERE ticket_type_id = ?", String.class, ticketType))
                .as("172-R1 回归：通票回流照旧不挂酒吧").containsExactly((String) null);

        YearMonth month = YearMonth.from(days.businessDate(null, bar, Instant.now()));
        // 第十八批 V 段 159-R4：抽成出账要求账期内营业日的订单与核销、退款与冲正已标记为最终（本用例不测该闸，整段标齐）。
        markRangeFinal(jdbc, "bar", bar, month.atDay(1), month.atEndOfMonth());
        assertThat(finance.computePeriodTurnoverRent(bar, contract, month.toString()))
                .as("渠道售出的门票回流已本地出票、计入门票，不进该酒吧抽成基数（BSM:1347）").isZero();
    }

    private void send(RequestPostProcessor who, String path, Map<String, Object> body) throws Exception {
        mockMvc.perform(post(path).with(who).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk());
    }
}
