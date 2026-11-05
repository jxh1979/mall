package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.settlement.TicketDistributionSettlementService;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 S 段 156-R1（BSM:751「商户让利的零元免费权益不结算；域主管公司出资的免费权益按锁定内部结算价核销后结算，未核销不结算。」；
 * BSM:1689「商户让利的零元权益不结算，域主管公司出资的零元权益按锁定内部结算价核销后结算。」）：
 * 组合通票餐饮行成本承担方为「商户让利型」时，核销结算价 0、结算事实 0、月结该餐厅专属直付不含这笔；
 * 「平台出资型」照旧按目录价直付；商户让利行不挂目录条目也能核销（不报 ENTITLEMENT_PRICE_UNRESOLVED）。
 * 真实接口与授权链：按 TicketingPoliciesPage.createAndBind 前端蛇形键建模板、绑票种，餐厅员工在本店设备核销。
 * 不开测试事务（结算事实在核销事务提交后写入）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class MerchantConcessionDiningSettlementJdbcIntegrationTests {

    private static final long CATALOG_PRICE_CENT = 3000L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TicketingRepository ticketingRepository;

    @Autowired
    private TicketDistributionSettlementService settlementService;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "prj_c156_" + suffix;
    private final String restaurant = "rest_c156_" + suffix;
    private final String admin = "usr_c156_admin_" + suffix;
    private final String staff = "usr_c156_staff_" + suffix;
    private final String device = "dev_c156_" + suffix;
    private final String typeId = "tt_c156_" + suffix;
    private final String catalogCode = "sku_c156_" + suffix;
    private final List<String> templateIds = new ArrayList<>();
    private final List<String> codes = new ArrayList<>();

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '让利结算综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, 'catering', 'active', ?, now(), now())", restaurant, "让利餐厅" + suffix, project);
        jdbc.update("""
                INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name,
                    price_cent, status, is_draft, updated_by, created_at, updated_at)
                VALUES (?, ?, ?, 'dining', '套餐', ?, 'active', FALSE, 'seed_c156', now(), now())
                """, catalogCode, catalogCode, restaurant, CATALOG_PRICE_CENT);
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)"
                + " VALUES (?, '组合通票', 'fixed', FALSE, 'draft', now(), now())", typeId);
        jdbc.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', ?, now(), now())", device, device, device, restaurant);
        user(admin, "platform_admin", "org_1", null, "票务策略配置" + Long.parseLong(suffix, 16),
                List.of("ticket.entitlement.manage", "ticket.product.manage"));
        user(staff, "staff", project, restaurant, "餐厅核销员" + Long.parseLong(suffix, 16), List.of("verification.execute"));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        jdbc.update("DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)", project);
        jdbc.update("DELETE FROM settlement_bills WHERE project_id = ?", project);
        for (String code : codes) {
            com.complex.platform.support.AppendOnlyTestBypass.update(
                    jdbc, "DELETE FROM settlement_transactions WHERE entitlement_id = ?", "ent_" + code);
            jdbc.update("DELETE FROM ticket_verifications WHERE entitlement_id = ?", "ent_" + code);
            jdbc.update("DELETE FROM issued_entitlements WHERE id = ?", "ent_" + code);
            jdbc.update("DELETE FROM ticket_orders WHERE id = ?", "order_" + code);
        }
        jdbc.update("DELETE FROM ticket_type_entitlements WHERE ticket_type_id = ?", typeId);
        for (String id : templateIds) {
            jdbc.update("DELETE FROM entitlement_templates WHERE id = ?", id);
        }
        jdbc.update("DELETE FROM internal_price_catalog WHERE item_code = ?", catalogCode);
        jdbc.update("DELETE FROM ticket_types WHERE id = ?", typeId);
        jdbc.update("DELETE FROM terminal_devices WHERE id = ?", device);
    }

    @Test
    void merchantConcessionDiningSettlesZeroWhilePlatformFundedPaysCatalogPrice() throws Exception {
        String concession = issue(createAndBind("让利餐厅 · 餐饮（商户让利）", catalogCode, "merchant_concession"), "mc");
        String funded = issue(createAndBind("让利餐厅 · 餐饮（平台出资）", catalogCode, "platform_funded"), "pf");

        verify(concession);
        verify(funded);

        assertThat(settlementPrice(concession)).isEqualTo(0L);
        assertThat(settledAmount(concession)).isEqualTo(0L);
        assertThat(settlementPrice(funded)).isEqualTo(CATALOG_PRICE_CENT);
        assertThat(settledAmount(funded)).isEqualTo(CATALOG_PRICE_CENT);

        TestSecurityActors.asPlatformAdmin();
        settlementService.settleMonth(project, YearMonth.now(ZoneOffset.UTC));
        // 该餐厅专属直付只含平台出资那一笔；商户让利那笔不产生应付。
        assertThat(jdbc.queryForObject("""
                SELECT si.final_amount_cent FROM settlement_items si JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.merchant_id = ? AND si.distribution_method = 'exclusive_full'
                """, Long.class, project, restaurant)).isEqualTo(CATALOG_PRICE_CENT);
    }

    @Test
    void merchantConcessionDiningWithoutCatalogItemStillVerifiesAndSettlesZero() throws Exception {
        String concession = issue(createAndBind("让利餐厅 · 餐饮（商户让利、未配目录）", null, "merchant_concession"), "nc");

        verify(concession);

        assertThat(settlementPrice(concession)).isEqualTo(0L);
        assertThat(settledAmount(concession)).isEqualTo(0L);
    }

    /** 按 TicketingPoliciesPage.createAndBind 餐饮行的请求形状建模板并绑到通票（纯赠送，票面原价 0）。 */
    private String createAndBind(String name, String catalog, String costBearer) throws Exception {
        String body = mockMvc.perform(post("/api/v1/ticketing/entitlement-templates").with(session(admin, "platform_admin", "org_1", null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c156-tpl-%s-%s", "name": "%s", "entitlement_type": "dining",
                                 "default_total_times": 1, "default_value_cent": 0, "status": "active", "merchant_id": "%s",
                                 "catalog_item_code": %s, "exclusive_merchant_id": "%s", "bar_item_ref": null}
                                """.formatted(costBearer, UUID.randomUUID(), name, restaurant,
                                catalog == null ? "null" : "\"" + catalog + "\"", restaurant)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String templateId = new ObjectMapper().readTree(body).path("data").path("id").asText();
        templateIds.add(templateId);
        mockMvc.perform(post("/api/v1/ticketing/ticket-types/" + typeId + "/entitlements").with(session(admin, "platform_admin", "org_1", null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c156-bind-%s", "entitlement_template_id": "%s", "quantity": 1,
                                 "total_times": 1, "num_people": 1, "component_price_cent": 0, "cost_bearer": "%s"}
                                """.formatted(UUID.randomUUID(), templateId, costBearer)))
                .andExpect(status().isOk());
        return templateId;
    }

    /** 售出该通票后发到客人手上的餐饮权益（专属该餐厅、纯赠送）。 */
    private String issue(String templateId, String tag) {
        String code = "c156-" + tag + "-" + suffix;
        codes.add(code);
        Instant now = Instant.now();
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_" + code, "ON-" + code, "cust_" + code, typeId, null, 1,
                LocalDate.now(), "web", null, 0L, null, 0L, 0L, "CNY", "paid", "issued", "created", now, now, null, null, null));
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, typeId, "order_" + code, templateId, "dining",
                restaurant, null, "point_" + suffix, 1, 0, 0L, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, null,
                "virtual", null, null, restaurant, null, 0, null, null));
        return code;
    }

    /** 餐厅员工在本店核销设备上核销。 */
    private void verify(String code) throws Exception {
        mockMvc.perform(post("/api/v1/ticketing/verifications").with(session(staff, "staff", project, restaurant))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c156-verify-%s", "credential_type": "qr", "credential_payload": "%s",
                                 "device_id": "%s", "point_id": "point_%s", "business_context": {}}
                                """.formatted(UUID.randomUUID(), code, device, suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("success"));
    }

    private Long settlementPrice(String code) {
        return jdbc.queryForObject("SELECT settlement_price_cent FROM ticket_verifications WHERE entitlement_id = ? AND status = 'success'",
                Long.class, "ent_" + code);
    }

    private Long settledAmount(String code) {
        return jdbc.queryForObject("SELECT verified_amount_cent FROM settlement_transactions WHERE entitlement_id = ?",
                Long.class, "ent_" + code);
    }

    private void user(String userId, String accountType, String organizationId, String merchantId, String roleName,
                      List<String> permissions) {
        String roleId = "role_" + userId;
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, ?, ?, ?, 'active', now())", userId, userId, accountType, organizationId, merchantId);
        jdbc.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId, roleName);
        for (String code : permissions) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
    }

    // 后台 Web 终端 + 生产过滤器放入的 AccessActor 主体（与账号表当前行一致）。
    private RequestPostProcessor session(String userId, String accountType, String organizationId, String merchantId) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(
                    new AccessActor(userId, userId, accountType, organizationId, merchantId, "active"), null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
