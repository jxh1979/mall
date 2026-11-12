package com.complex.platform.ticketing;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
 * 第十八批 AH 段第 215 条：通票最低赠酒杯数。
 *
 * <p>215-R1（ENUM:34「一张酒吧通票或跨域组合通票覆盖的每一家酒吧，赠酒杯数不得低于后台配置的最低赠酒杯数……低于该下限的票种不得发布上架」
 * 「该最低赠酒杯数属于票面强制要求提供的权益，一律记为域主管公司出资」；BSM:1033「只有商户主动申请并书面确认让利的权益才记为商户让利、不结算」）：
 * 发布闸按每家覆盖酒吧核对赠酒杯数与平台出资杯数。
 *
 * <p>215-R2（ENUM:34、BSM:1032「后台配置的最低赠酒杯数（业务参数，按票种可配，出厂默认 1 杯，须同时配置上下限）」）：按票种配最低赠酒杯数，
 * 值与上下限同时填写；已上架通票改高下限后跌破即退回草稿。
 *
 * <p>真实接口与授权链：平台管理员按后台票务策略页同键（蛇形键）建票、建模板、绑定、发布、配最低杯数。不开测试事务，@AfterEach 按票种清理。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class PassBarGiftFloorJdbcIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final long n = Long.parseLong(suffix, 16);
    private final String project = "prj_c215_" + suffix;
    private final String bar = "bar_c215_" + suffix;
    private final String product = "bp_c215_" + suffix;
    private final String catalogCode = "sku_c215_" + suffix;
    private final String admin = "usr_c215_admin_" + suffix;
    private final List<String> ticketTypes = new ArrayList<>();
    private final List<String> templates = new ArrayList<>();

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '赠酒下限综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', 'active', ?, now(), now())", bar, "赠酒下限酒吧" + n, project);
        // 第十八批 AI 段 236-R2（ENUM:54「任何商户在没有获批内部结算价之前，不得为其开通需要该价格结算的权益或券」）：赠酒商品引用已获批生效的目录条目。
        jdbc.update("INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name, price_cent, status, is_draft,"
                + " updated_by, created_at, updated_at) VALUES (?, ?, ?, 'beverage', '赠酒一杯', 2000, 'active', FALSE, 'seed_c215', now(), now())",
                catalogCode, catalogCode, bar);
        jdbc.update("INSERT INTO bar_products (id, bar_id, product_name, product_type, price_cent, currency, inventory_item_id,"
                + " stock_location_id, stock_deduction_required, status, internal_settlement_price_cent, catalog_item_code, created_at, updated_at)"
                + " VALUES (?, ?, '赠酒一杯', 'beverage', 3800, 'CNY', NULL, 'default', FALSE, 'active', 2000, ?, now(), now())",
                product, bar, catalogCode);
        String roleId = "role_" + admin;
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", admin, admin);
        jdbc.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId, "通票赠酒下限配置" + n);
        for (String code : List.of("ticket.product.manage", "ticket.product.view", "ticket.entitlement.manage")) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", admin, roleId);
    }

    @AfterEach
    void cleanup() {
        for (String type : ticketTypes) {
            jdbc.update("DELETE FROM ticket_type_gift_floors WHERE ticket_type_id = ?", type);
            jdbc.update("DELETE FROM ticket_type_entitlements WHERE ticket_type_id = ?", type);
            jdbc.update("DELETE FROM ticket_types WHERE id = ?", type);
        }
        for (String template : templates) {
            jdbc.update("DELETE FROM entitlement_templates WHERE id = ?", template);
        }
        jdbc.update("DELETE FROM bar_products WHERE id = ?", product);
        jdbc.update("DELETE FROM internal_price_catalog WHERE item_code = ?", catalogCode);
        jdbc.update("UPDATE projects SET status = 'suspended' WHERE id = ?", project);
    }

    @Test
    void passPublishGateChecksEveryBarFloorAndPlatformFunding() throws Exception {
        String pass = createPass("k215a");
        String entry = template("入场", "entry", null);
        bind(pass, entry, 1, 1, 5000L, null);

        publish(pass).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PASS_BAR_GIFT_COVERAGE_INSUFFICIENT"));

        String concession = template("让利赠酒", "gift_drink", product);
        bind(pass, concession, 1, 1, 0L, "merchant_concession");
        publish(pass).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PASS_BAR_GIFT_FLOOR_NOT_PLATFORM_FUNDED"));

        String platform = template("平台赠酒", "gift_drink", product);
        bind(pass, platform, 1, 1, 0L, "platform_funded");
        publish(pass).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
    }

    @Test
    void minGiftCupsConfiguredPerTicketTypeWithBoundsAndPublishedPassFallsBackToDraft() throws Exception {
        String pass = createPass("k215b");
        mockMvc.perform(get("/api/v1/ticketing/ticket-types/" + pass + "/min-gift-cups").with(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.min_gift_cups").value(1));

        String entry = template("入场", "entry", null);
        bind(pass, entry, 1, 1, 5000L, null);
        String gift = template("平台赠酒", "gift_drink", product);
        bind(pass, gift, 1, 1, 0L, "platform_funded");
        publish(pass).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));

        configure(pass, "{\"min_gift_cups\": 2, \"min_gift_cups_lower\": 1}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        configure(pass, "{\"min_gift_cups\": 2, \"min_gift_cups_lower\": 3, \"min_gift_cups_upper\": 3}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));

        configure(pass, "{\"min_gift_cups\": 2, \"min_gift_cups_lower\": 1, \"min_gift_cups_upper\": 3}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.min_gift_cups").value(2));
        mockMvc.perform(get("/api/v1/ticketing/ticket-types/" + pass + "/min-gift-cups").with(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.min_gift_cups").value(2))
                .andExpect(jsonPath("$.data.min_gift_cups_lower").value(1))
                .andExpect(jsonPath("$.data.min_gift_cups_upper").value(3));
        assertThat(jdbc.queryForObject("SELECT status FROM ticket_types WHERE id = ?", String.class, pass))
                .as("已上架通票改到 2 杯后只赠 1 杯，退回草稿").isEqualTo("draft");

        publish(pass).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PASS_BAR_GIFT_COVERAGE_INSUFFICIENT"));
    }

    private String createPass(String tag) throws Exception {
        String body = mockMvc.perform(post("/api/v1/ticketing/ticket-types").with(session())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c215-%s-%s", "ticket_name": "赠酒下限通票%d",
                                 "base_calendar_price_mode": "weekday_weekend_holiday", "weekday_price_cent": 5000,
                                 "weekend_price_cent": 5000, "holiday_price_cent": 5000, "stock_required": false, "status": "active"}
                                """.formatted(tag, suffix, n)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JSON.readTree(body).path("data").path("id").asText();
        ticketTypes.add(id);
        return id;
    }

    private String template(String name, String type, String barItemRef) throws Exception {
        String body = mockMvc.perform(post("/api/v1/ticketing/entitlement-templates").with(session())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c215-tpl-%s", "name": "赠酒下限酒吧 · %s%d", "entitlement_type": "%s",
                                 "default_total_times": 1, "default_value_cent": 0, "status": "active", "merchant_id": "%s",
                                 "catalog_item_code": null, "exclusive_merchant_id": null, "bar_item_ref": %s}
                                """.formatted(UUID.randomUUID(), name, n, type, bar,
                                barItemRef == null ? "null" : "\"" + barItemRef + "\"")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JSON.readTree(body).path("data").path("id").asText();
        templates.add(id);
        return id;
    }

    private void bind(String pass, String template, int quantity, int totalTimes, long componentPriceCent, String costBearer)
            throws Exception {
        mockMvc.perform(post("/api/v1/ticketing/ticket-types/" + pass + "/entitlements").with(session())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c215-bind-%s", "entitlement_template_id": "%s", "quantity": %d,
                                 "total_times": %d, "num_people": 1, "component_price_cent": %d, "cost_bearer": %s}
                                """.formatted(UUID.randomUUID(), template, quantity, totalTimes, componentPriceCent,
                                costBearer == null ? "null" : "\"" + costBearer + "\"")))
                .andExpect(status().isOk());
    }

    private ResultActions publish(String pass) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/ticket-types/" + pass + "/publish").with(session())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "c215-pub-%s", "action": "publish", "reason": "通票配好赠酒上架"}
                        """.formatted(UUID.randomUUID())));
    }

    private ResultActions configure(String pass, String values) throws Exception {
        String body = values.substring(0, values.length() - 1)
                + ", \"idempotency_key\": \"c215-cups-" + UUID.randomUUID() + "\"}";
        return mockMvc.perform(put("/api/v1/ticketing/ticket-types/" + pass + "/min-gift-cups").with(session())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    // 后台 Web 终端 + 生产过滤器放入的 AccessActor 主体（与账号表当前行一致）。
    private RequestPostProcessor session() {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(
                    new AccessActor(admin, admin, "platform_admin", "org_1", null, "active"), null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
