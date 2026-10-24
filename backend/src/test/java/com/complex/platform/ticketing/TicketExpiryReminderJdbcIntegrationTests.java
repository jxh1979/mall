package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-03 clients-R10 未核销票到期提醒（BSM 第 1596 行「未核销的票在到期前通过官方 App 或小程序弹出的到期提醒（业务参数，按项目可配，出厂默认到期前
 * 二十四小时，须同时配置上下限）」；决策台账 DEC-R4-2026-08-04-84「未核销票到期前二十四小时经 App／小程序弹通知」）。请求按前端真实形状（蛇形键）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TicketExpiryReminderJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTicketingRepository repository;
    @Autowired private CustomerRepository customers;
    @Autowired private JdbcTemplate jdbc;

    private final Instant now = Instant.now();
    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String projectId = "project_ter_" + suffix;
    private StaffRequestFixture staffs;
    private String me;
    private String myToken;

    @BeforeEach
    void seed() {
        staffs = new StaffRequestFixture(jdbc);
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '到期提醒测试综合体', 'active', now(), now())", projectId);
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at) "
                + "VALUES ('tt_ter','到期提醒测试票','fixed','active', now(), now()) ON CONFLICT (id) DO NOTHING");
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status, created_at, updated_at) "
                + "VALUES ('et_ter','到期提醒测试权益','ticket',1,100,'active', now(), now()) ON CONFLICT (id) DO NOTHING");
        me = customer("me");
        myToken = "tok_ter_me_" + suffix;
        customers.saveSession(new CustomerSessionRecord("sess_ter_me_" + suffix, me, myToken, "app", now, now.plusSeconds(3600), "active"));
    }

    @Test
    void onlyOwnUnredeemedTicketsWithinProjectWindow() throws Exception {
        String soon = ticket(me, 10, 0);
        String later = ticket(me, 30, 0);
        ticket(me, 5, 1);
        ticket(customer("other"), 5, 0);
        reminders().andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].ticket_id").value(containsInAnyOrder(soon)))
                .andExpect(jsonPath("$.data[0].ticket_name").value("到期提醒测试票"));
        RequestPostProcessor operator = staffs.staff("usr_ter_" + suffix, "staff", projectId, null, "ticket.product.manage",
                "ticket.product.view");
        savePolicy(operator, projectId, 48, 12, 72).andExpect(status().isOk());
        reminders().andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].ticket_id").value(containsInAnyOrder(soon, later)));
    }

    @Test
    void policyBoundsEnforcedOnUpdate() throws Exception {
        RequestPostProcessor operator = staffs.staff("usr_ter_b_" + suffix, "staff", projectId, null, "ticket.product.manage",
                "ticket.product.view");
        mockMvc.perform(get("/api/v1/ticketing/expiry-reminder-policies/" + projectId).with(operator)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.remind_hours").value(24))
                .andExpect(jsonPath("$.data.min_hours").value(org.hamcrest.Matchers.nullValue()));
        savePolicy(operator, projectId, 48, null, null).andExpect(status().isBadRequest());
        savePolicy(operator, projectId, 100, 12, 72).andExpect(status().isBadRequest());
        savePolicy(operator, projectId, 48, 12, 72).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.remind_hours").value(48))
                .andExpect(jsonPath("$.data.min_hours").value(12))
                .andExpect(jsonPath("$.data.max_hours").value(72));
        savePolicy(operator, "project_other_" + suffix, 48, 12, 72).andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

    private ResultActions reminders() throws Exception {
        return mockMvc.perform(get("/api/v1/customer/ticket-expiry-reminders").header("X-Terminal-Type", "app")
                .header("X-Customer-Token", myToken));
    }

    private ResultActions savePolicy(RequestPostProcessor who, String project, Integer hours, Integer min, Integer max) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "ter-" + UUID.randomUUID());
        body.put("remind_hours", hours);
        body.put("min_hours", min);
        body.put("max_hours", max);
        return mockMvc.perform(put("/api/v1/ticketing/expiry-reminder-policies/" + project).with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private String customer(String tag) {
        String id = "cus_ter_" + tag + "_" + suffix;
        String phone = "135" + String.format("%08d", Math.floorMod((tag + suffix).hashCode(), 100_000_000));
        customers.saveCustomer(new CustomerRecord(id, phone, "135****" + phone.substring(7), "active", null, null, now, now));
        return id;
    }

    /** 一张单人票（一条权益），持有人 holder，hoursLeft 小时后到期，已核销 used 次。返回权益编号（我的票的票号）。 */
    private String ticket(String holder, int hoursLeft, int used) {
        String parent = "tkt_ter_" + UUID.randomUUID().toString().replace("-", "");
        String orderId = "ord_" + parent;
        jdbc.update("INSERT INTO ticket_orders (id, order_no, customer_id, ticket_type_id, quantity, sales_channel, amount_cent, "
                + "pay_status, issue_status, status, created_at, updated_at, project_id) "
                + "VALUES (?, ?, ?, 'tt_ter', 1, 'app', 0, 'paid', 'issued', 'created', now(), now(), ?)",
                orderId, "ONO_" + parent, holder, projectId);
        repository.saveTicketInstance(new TicketInstanceRecord(parent, "TNO_" + parent, orderId, "tt_ter", 1, 1, 1,
                holder, holder, "active", null, null, now, now));
        repository.upsertSlotHolder(parent, 1, holder, now);
        String eid = "ent_" + UUID.randomUUID().toString().replace("-", "");
        repository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                eid, "code_" + eid, "tt_ter", orderId, "et_ter", "ticket", "m1", null, null,
                1, 0, 100L, "active", now.minusSeconds(60), now.plusSeconds(3600L * hoursLeft), now, now,
                1, null, null, null, null, null, parent, 1, null, null));
        if (used > 0) {
            jdbc.update("UPDATE issued_entitlements SET used_times = ? WHERE id = ?", used, eid);
        }
        jdbc.update("UPDATE issued_entitlements SET valid_to = ? WHERE id = ?", Timestamp.from(now.plusSeconds(3600L * hoursLeft)), eid);
        return eid;
    }
}
