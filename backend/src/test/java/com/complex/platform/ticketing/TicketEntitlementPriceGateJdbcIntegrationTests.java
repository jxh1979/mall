package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
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
 * 第十八批 AI 段第 236 条 236-R2（第十八批裁决 52 ①内部结算价审批版本；ENUM:54「任何商户在没有获批内部结算价之前，不得为其开通需要该价格结算的
 * 权益或券。」；BSM:1754「每项权益都必须绑定实际提供方和审批版本，避免悬空账。」）：
 * 票种发布、给已上架票种加权益时，须提供方兑付、核销时按内部结算价结算的权益，其提供方在内部结算价目录里必须已有获批生效价（草案须他人复核转正）；
 * 商户让利的专属直付权益不结算、入场等非提供方兑付权益不受影响。
 *
 * <p>真实接口与授权链：平台管理员按后台票务策略页同键（蛇形键）建票、建模板、绑定、发布；内部结算价由提交人落草案、另一人复核转正。
 * 不开测试事务，@AfterEach 按票种清理。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class TicketEntitlementPriceGateJdbcIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final long n = Long.parseLong(suffix, 16);
    private final String project = "prj_c236_" + suffix;
    private final String restaurant = "rest_c236_" + suffix;
    private final String bar = "bar_c236_" + suffix;
    private final String product = "bp_c236_" + suffix;
    private final String catalogCode = "sku_c236_" + suffix;
    private final String admin = "usr_c236_admin_" + suffix;
    private final String pricer = "usr_c236_price_" + suffix;
    private final String reviewer = "usr_c236_review_" + suffix;
    private final List<String> ticketTypes = new ArrayList<>();
    private final List<String> templates = new ArrayList<>();

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '内部结算价发布闸综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, 'catering', 'active', ?, now(), now())", restaurant, "发布闸餐厅" + n, project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', 'active', ?, now(), now())", bar, "发布闸酒吧" + n, project);
        // 酒吧赠酒商品没有引用内部结算价目录条目＝没有获批内部结算价。
        jdbc.update("INSERT INTO bar_products (id, bar_id, product_name, product_type, price_cent, currency, inventory_item_id,"
                + " stock_location_id, stock_deduction_required, status, created_at, updated_at)"
                + " VALUES (?, ?, '赠酒一杯', 'beverage', 3800, 'CNY', NULL, 'default', FALSE, 'active', now(), now())",
                product, bar);
        user(admin, "票种发布闸配置" + n, List.of("ticket.product.manage", "ticket.product.view", "ticket.entitlement.manage"));
        user(pricer, "内部结算价提交" + n, List.of("settlement.config.manage"));
        user(reviewer, "内部结算价复核" + n, List.of("settlement.config.manage"));
    }

    @AfterEach
    void cleanup() {
        for (String type : ticketTypes) {
            jdbc.update("DELETE FROM ticket_type_entitlements WHERE ticket_type_id = ?", type);
            jdbc.update("DELETE FROM ticket_types WHERE id = ?", type);
        }
        for (String template : templates) {
            jdbc.update("DELETE FROM entitlement_templates WHERE id = ?", template);
        }
        jdbc.update("DELETE FROM internal_price_catalog WHERE item_code = ?", catalogCode);
        jdbc.update("DELETE FROM bar_products WHERE id = ?", product);
        jdbc.update("UPDATE projects SET status = 'suspended' WHERE id = ?", project);
    }

    @Test
    void publishBlockedUntilProviderInternalPriceApprovedByAnotherPerson() throws Exception {
        // 提供方提交餐饮套餐内部结算价，目录只有草案、还没复核转正。
        mockMvc.perform(post("/api/v1/settlement/config/internal-price-item").with(session(pricer))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c236-price-%s", "item_code": "%s", "merchant_id": "%s", "category": "dining",
                                 "item_name": "套餐", "price_cent": 3000}
                                """.formatted(suffix, catalogCode, restaurant)))
                .andExpect(status().isOk());
        String pass = createPass("a");
        bind(pass, template("入场", "entry", null, null), 5000L, null).andExpect(status().isOk());
        String dining = template("餐饮（平台出资）", "dining", catalogCode, restaurant);
        bind(pass, dining, 0L, "platform_funded").andExpect(status().isOk());

        publish(pass).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_ENTITLEMENT_PRICE_NOT_APPROVED"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("餐饮（平台出资）")));
        assertThat(ticketStatus(pass)).isEqualTo("draft");

        String draft = jdbc.queryForObject("SELECT id FROM internal_price_catalog WHERE item_code = ? AND is_draft = TRUE",
                String.class, catalogCode);
        mockMvc.perform(post("/api/v1/settlement/config/internal_price_item/" + draft + "/activate").with(session(reviewer)))
                .andExpect(status().isOk());

        publish(pass).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
    }

    @Test
    void bindingUnapprovedGiftDrinkToPublishedTicketRejectedAndNotStored() throws Exception {
        String pass = createPass("b");
        // 入场等非提供方兑付权益不查内部结算价，照常发布。
        bind(pass, template("入场", "entry", null, null), 5000L, null).andExpect(status().isOk());
        publish(pass).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));

        String gift = template("平台赠酒", "gift_drink", null, null, bar, product);
        bind(pass, gift, 0L, "platform_funded").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_ENTITLEMENT_PRICE_NOT_APPROVED"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("不能加到已上架票种")));
        assertThat(ticketStatus(pass)).isEqualTo("published");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_type_entitlements WHERE ticket_type_id = ? AND entitlement_template_id = ?",
                Integer.class, pass, gift)).isZero();
    }

    @Test
    void merchantConcessionExclusiveDiningWithoutCatalogPricePublishes() throws Exception {
        String pass = createPass("c");
        bind(pass, template("入场", "entry", null, null), 5000L, null).andExpect(status().isOk());
        bind(pass, template("餐饮（商户让利）", "dining", null, restaurant), 0L, "merchant_concession").andExpect(status().isOk());

        publish(pass).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
    }

    private String createPass(String tag) throws Exception {
        String body = mockMvc.perform(post("/api/v1/ticketing/ticket-types").with(session(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c236-%s-%s", "ticket_name": "发布闸通票%d",
                                 "base_calendar_price_mode": "weekday_weekend_holiday", "weekday_price_cent": 5000,
                                 "weekend_price_cent": 5000, "holiday_price_cent": 5000, "stock_required": false, "status": "active"}
                                """.formatted(tag, suffix, n)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JSON.readTree(body).path("data").path("id").asText();
        ticketTypes.add(id);
        return id;
    }

    /** 餐厅提供的模板（入场、餐饮）；exclusiveMerchant 非空＝专属直付。 */
    private String template(String name, String type, String catalog, String exclusiveMerchant) throws Exception {
        return template(name, type, catalog, exclusiveMerchant, restaurant, null);
    }

    private String template(String name, String type, String catalog, String exclusiveMerchant, String merchant, String barItemRef)
            throws Exception {
        String body = mockMvc.perform(post("/api/v1/ticketing/entitlement-templates").with(session(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c236-tpl-%s", "name": "发布闸 · %s%d", "entitlement_type": "%s",
                                 "default_total_times": 1, "default_value_cent": 0, "status": "active", "merchant_id": "%s",
                                 "catalog_item_code": %s, "exclusive_merchant_id": %s, "bar_item_ref": %s}
                                """.formatted(UUID.randomUUID(), name, n, type, merchant, quoted(catalog), quoted(exclusiveMerchant),
                                quoted(barItemRef))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JSON.readTree(body).path("data").path("id").asText();
        templates.add(id);
        return id;
    }

    private ResultActions bind(String pass, String template, long componentPriceCent, String costBearer) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/ticket-types/" + pass + "/entitlements").with(session(admin))
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "c236-bind-%s", "entitlement_template_id": "%s", "quantity": 1,
                         "total_times": 1, "num_people": 1, "component_price_cent": %d, "cost_bearer": %s}
                        """.formatted(UUID.randomUUID(), template, componentPriceCent, quoted(costBearer))));
    }

    private ResultActions publish(String pass) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/ticket-types/" + pass + "/publish").with(session(admin))
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "c236-pub-%s", "action": "publish", "reason": "通票配好权益上架"}
                        """.formatted(UUID.randomUUID())));
    }

    private String ticketStatus(String pass) {
        return jdbc.queryForObject("SELECT status FROM ticket_types WHERE id = ?", String.class, pass);
    }

    private static String quoted(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private void user(String userId, String roleName, List<String> permissions) {
        String roleId = "role_" + userId;
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", userId, userId);
        jdbc.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId, roleName);
        for (String code : permissions) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
    }

    // 后台 Web 终端 + 生产过滤器放入的 AccessActor 主体（与账号表当前行一致）。
    private RequestPostProcessor session(String userId) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(
                    new AccessActor(userId, userId, "platform_admin", "org_1", null, "active"), null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
