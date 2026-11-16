package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 AO 段第 249 条 249-R4（真库、真实入口、真实授权链）：BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」——
 * 最高权限本人对已存档的价格规则申请删除即生效，申请与批准各留一条痕；价格规则仍被门票订单引用时 409 TICKET_PRICE_RULE_IN_USE，
 * 整笔回滚，仍为已存档、未申请删除（BSM:572「只要还有占用就拦住不让删」）。不开测试事务，才看得到回滚；夹具编号后缀纯数字。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class TicketCatalogMasterDeleteJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    private final String ticketType = "tt_r249_" + s;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at)"
                + " VALUES (?, '最高权限删除回归票种', 'fixed', 'active', now(), now())", ticketType);
    }

    @Test
    void masterDeleteRequestOnArchivedPriceRuleTakesEffectAtOnce() throws Exception {
        String rule = archivedRule("direct");

        deleteRequest(rule, "direct")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.delete_status").value("approved"));

        assertThat(jdbc.queryForObject("SELECT delete_status FROM ticket_price_rules WHERE id = ?", String.class, rule)).isEqualTo("approved");
        assertThat(masterEvents(rule)).containsExactlyInAnyOrder(
                "ticket_price_variant.delete_requested|success", "ticket_price_variant.delete_approved|success");
    }

    @Test
    void masterDeleteRequestOnPriceRuleStillOnOrdersIsRejectedAndRolledBack() throws Exception {
        String rule = archivedRule("ordered");
        String order = "to_r249_" + s;
        jdbc.update("INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, pay_status, issue_status, status,"
                + " created_at, updated_at, price_rule_id) VALUES (?, ?, ?, 1, 'web', 9000, 'paid', 'issued', 'created', now(), now(), ?)",
                order, "R249-" + s, ticketType, rule);
        long failed = masterFailures();

        deleteRequest(rule, "ordered")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_PRICE_RULE_IN_USE"));

        Map<String, Object> row = jdbc.queryForMap("SELECT archived_at, delete_status FROM ticket_price_rules WHERE id = ?", rule);
        assertThat(row.get("archived_at")).isNotNull();
        assertThat(row.get("delete_status")).isEqualTo("none");
        assertThat(masterEvents(rule)).isEmpty();
        assertThat(masterFailures()).isEqualTo(failed + 1);
    }

    private String archivedRule(String tag) {
        String id = "tpr_r249_" + tag + "_" + s;
        jdbc.update("INSERT INTO ticket_price_rules (id, ticket_type_id, variant_type, price_mode, weekday_price_cent, status, created_at, updated_at,"
                + " archived_at, archived_by) VALUES (?, ?, 'member', 'fixed_price', 9000, 'active', now(), now(), now(), ?)",
                id, ticketType, "usr_r249_pricing_" + s);
        return id;
    }

    private ResultActions deleteRequest(String rule, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/price-rules/" + rule + "/delete-request").with(master())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "r249-tpr-" + tag + "-" + s, "reason", "停用的会员价"))));
    }

    private List<String> masterEvents(String businessId) {
        return jdbc.queryForList("SELECT event_name || '|' || event_result FROM audit_events"
                + " WHERE actor_id = ? AND business_id = ? AND event_name LIKE 'ticket_price_variant.delete_%'",
                String.class, SessionIssuer.MASTER_USER_ID, businessId);
    }

    private long masterFailures() {
        Long count = jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'ticket_price_variant.delete_request_failed'"
                + " AND actor_id = ? AND metadata->>'error_code' = 'TICKET_PRICE_RULE_IN_USE'", Long.class, SessionIssuer.MASTER_USER_ID);
        return count == null ? 0L : count;
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
