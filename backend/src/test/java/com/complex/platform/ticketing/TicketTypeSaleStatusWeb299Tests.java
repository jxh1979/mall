package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.support.TicketSalesContractTestFixtures;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AW 段 299-R1（ENUM:30「票务策略状态为：草稿（规则未发布）、待启用（规则已配置但未到生效时间）、售卖中（当前可售卖）、暂停售卖（临时停止售卖）、
 * 已结束（售卖周期结束）、归档（历史策略）。」；R14 裁决 20「不设独立状态机，复用票种 4 态（草稿/上架/暂停/归档）；「待启用」「已结束」按生效时间派生、不存储」）：
 * 票种售卖设置（暂停售卖、开售与停售时间）一个动作整体替换；列表与响应带派生的 sale_status；售卖闸按派生态拦新下单，客人公开列表只列售卖中。
 * 票种为前置事实直写；售卖设置、票种列表、下单、客人列表都走后台与客人端真实请求（蛇形键）与真实授权链，随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TicketTypeSaleStatusWeb299Tests {

    private static final String TYPES = "/api/v1/ticketing/ticket-types";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TicketingRepository ticketing;

    private final String s = String.valueOf(System.nanoTime());
    /** 票务运营：管票种、下单。 */
    private final String operator = "usr_c299_op_" + s;
    /** 票种权益的提供方商户（定票种所属综合体）。 */
    private final String merchant = "mch_c299_" + s;
    private final String project = "prj_c299_" + s;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '售卖设置综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '售卖设置餐厅', 'catering', 'active', now(), now(), ?)", merchant, project);
        String roleId = "role_c299_" + s;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, view_all_projects, created_at)"
                + " VALUES (?, ?, '票种售卖设置回归', 'platform', 'active', TRUE, now())", roleId, "票种售卖设置运营" + s);
        for (String code : List.of("ticket.product.manage", "ticket.product.view", "ticket.order.create")) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", operator, operator);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", operator, roleId);
    }

    /** ①②③：已上架为售卖中；暂停后拦新下单、客人列表不露出、库内 status 不变、留痕、同键重放同一结果；取消暂停后回到售卖中可下单。 */
    @Test
    void pauseBlocksNewOrdersAndHidesFromCustomersThenResumes() throws Exception {
        String t = ticket("a", "published");
        mockMvc.perform(get(TYPES).with(operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + t + "')].sale_status").value(contains("on_sale")));

        String body = "{\"idempotency_key\": \"c299-a1-" + s + "\", \"sale_paused\": true, \"reason\": \"临时停售\"}";
        String paused = settings(t, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sale_status").value("paused"))
                .andExpect(jsonPath("$.data.sale_paused_at").value(not(emptyOrNullString())))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(jdbc.queryForObject("SELECT status FROM ticket_types WHERE id = ?", String.class, t)).isEqualTo("published");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'ticket_product.sale_settings_changed'"
                + " AND event_result = 'success' AND business_id = ? AND metadata->>'reason' = '临时停售'", Integer.class, t)).isEqualTo(1);
        settings(t, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sale_status").value("paused"))
                .andExpect(jsonPath("$.data.sale_paused_at").value((String) JsonPath.read(paused, "$.data.sale_paused_at")));

        order(t, "a1").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_TYPE_NOT_SELLABLE"))
                .andExpect(jsonPath("$.message").value(containsString("暂停售卖")));
        mockMvc.perform(get("/api/v1/customer/ticket-products").header("X-Terminal-Type", "miniapp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.ticket_type_id == '" + t + "')]").isEmpty());

        settings(t, "{\"idempotency_key\": \"c299-a2-" + s + "\", \"sale_paused\": false}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sale_status").value("on_sale"));
        order(t, "a2").andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/customer/ticket-products").header("X-Terminal-Type", "miniapp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].ticket_type_id").value(hasItem(t)));
    }

    /** ④：开售时间未到为待启用、停售时间已过为已结束，各拦新下单；停售不晚于开售 400；两端清空回到售卖中。 */
    @Test
    void saleWindowDerivesPendingActivationAndEnded() throws Exception {
        String t = ticket("b", "published");
        Instant now = Instant.now();
        settings(t, window("b1", now.plus(1, ChronoUnit.DAYS), null)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sale_status").value("pending_activation"));
        order(t, "b1").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_TYPE_NOT_SELLABLE"))
                .andExpect(jsonPath("$.message").value(containsString("尚未开售")));

        settings(t, window("b2", null, now.minus(1, ChronoUnit.HOURS))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sale_status").value("ended"));
        order(t, "b2").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_TYPE_NOT_SELLABLE"))
                .andExpect(jsonPath("$.message").value(containsString("售卖已结束")));

        settings(t, window("b3", now.plus(2, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));

        settings(t, window("b4", null, null)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sale_status").value("on_sale"));
    }

    /** ⑤：草稿票能设、仍为草稿；已存档 409；不存在 404；缺 sale_paused 400。 */
    @Test
    void draftArchivedUnknownAndMissingFlag() throws Exception {
        String draft = ticket("c", "draft");
        settings(draft, "{\"idempotency_key\": \"c299-c1-" + s + "\", \"sale_paused\": true}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sale_status").value("draft"));

        String archived = ticket("d", "published");
        ticketing.saveTicketType(ticketing.findTicketTypeById(archived).orElseThrow().withArchive(Instant.now(), operator, Instant.now()));
        settings(archived, "{\"idempotency_key\": \"c299-d1-" + s + "\", \"sale_paused\": true}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_TYPE_NOT_CONFIGURABLE"));

        settings("tt_c299_missing_" + s, "{\"idempotency_key\": \"c299-e1-" + s + "\", \"sale_paused\": true}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("TICKET_TYPE_NOT_FOUND"));

        String t = ticket("f", "published");
        settings(t, "{\"idempotency_key\": \"c299-f1-" + s + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    private String ticket(String tag, String status) {
        String id = "tt_c299_" + tag + "_" + s;
        Instant now = Instant.now();
        ticketing.saveTicketType(new TicketTypeRecord(id, "售卖设置回归票" + tag, "flat", 8800L, 8800L, 8800L, false, status, 1, null,
                now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
        String template = "tpl_c299_" + tag + "_" + s;
        ticketing.saveEntitlementTemplate(new EntitlementTemplateRecord(template, "餐饮券" + tag, "catering_voucher", 1, 8800L, "active",
                merchant, null, null, null, now, now, "virtual", null, "per_use", null));
        ticketing.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord("bind_" + template, id, template, 1, 1, 8800L, "default", 1,
                8800L, now, now));
        TicketSalesContractTestFixtures.approvedPublished(jdbc, new ObjectMapper(), id, project, merchant);
        return id;
    }

    private String window(String tag, Instant startsAt, Instant endsAt) {
        return "{\"idempotency_key\": \"c299-" + tag + "-" + s + "\", \"sale_paused\": false, \"sale_starts_at\": "
                + quoted(startsAt) + ", \"sale_ends_at\": " + quoted(endsAt) + "}";
    }

    private static String quoted(Instant instant) {
        return instant == null ? "null" : "\"" + instant + "\"";
    }

    private ResultActions settings(String ticketType, String body) throws Exception {
        return mockMvc.perform(put(TYPES + "/" + ticketType + "/sale-settings").with(operator())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions order(String ticketType, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/orders").with(operator())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "c299-order-%s-%s", "ticket_type_id": "%s", "quantity": 1, "sales_channel": "admin"}
                        """.formatted(tag, s, ticketType)));
    }

    // 后台 Web 终端 + 生产过滤器放入的 AccessActor 主体（与账号表当前行一致）。
    private RequestPostProcessor operator() {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(
                    new AccessActor(operator, operator, "platform_admin", "org_1", null, "active"), null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
