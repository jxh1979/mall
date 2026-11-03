// 文件版本：V0.01
package com.complex.platform.gate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 O 段第 130 条 130-R2（BSM:1901「只做酒吧点酒下单，绑定酒吧和卡座，校验最低消费，支持扫码支付。」；PGM:634「平板只负责酒吧点酒下单。」；
 * PGM:747「……能做什么由后台按设备类型统一下发……」；PGM:744「……前端隐藏只是辅助，后端接口必须独立校验。」；PGM:738「访问被拒绝也要留痕……」）。
 * 走真实生产登录链与授权链（默认 grace、不签名）：酒吧员工按平板的发法登录取访问令牌，之后按平板 POS 的发法
 * （Bearer、X-Terminal-Type: tablet、X-Device-Id＝登记的平板设备号、蛇形键）请求。
 */
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "complex.database.credential-separation.enforced=false",
        "refund.tiered_authorization.enabled=true",
        "complex.auth.production.seed-admin.username=p0_tablet_functions_admin",
        "complex.auth.production.seed-admin.password=p0_tablet_functions_password",
        "ticket.credential.signing.secret=prod-test-ticket-signing-secret-32-bytes-minimum",
        "bar.invitation.signing.secret=prod-test-bar-invitation-signing-secret-32-bytes-minimum",
        "compliance.wecom.webhook.signing.secret=prod-test-wecom-webhook-signing-secret-32-bytes-minimum",
        "keyvault.keys.pii=prod-test-pii-keyvault-secret-32-bytes-minimum",
        "keyvault.keys.pii-index=prod-test-pii-index-blind-secret-32-bytes-minimum"
})
@Transactional
class TabletFunctionsJdbcIntegrationTests {

    private static final String TABLET = "tablet";

    @DynamicPropertySource
    static void masterPublicKey(DynamicPropertyRegistry registry) throws Exception {
        String key = Base64.getEncoder().encodeToString(KeyPairGenerator.getInstance("Ed25519").generateKeyPair().getPublic().getEncoded());
        registry.add("complex.auth.master.public-key", () -> key);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String password = "b35-tablet-functions-" + suffix;
    private final String project = "prj_b35tf_" + suffix;
    private final String bar = "bar_b35tf_" + suffix;
    private final String table = "table_b35tf_" + suffix;
    private final String product = "bp_b35tf_" + suffix;
    private final String device = "POS-B35TF-" + suffix;
    /** 酒吧员工（带商户归属）。 */
    private final String user = "usr_b35tf_" + suffix;

    /** ①② 平板只拿到酒吧点酒下单与平板点单 POS 两项；按平板 POS 真实形状下单、生成账单、发起支付、结账都放行。 */
    @Test
    void tabletGetsBarOrderingFunctionsAndCompletesPosCheckout() throws Exception {
        String token = seed();

        JsonNode functions = data(mockMvc.perform(tablet(get("/api/v1/terminals/staff-app-functions"), token))
                .andExpect(status().isOk()).andReturn());
        assertThat(functions.get("terminal_type").asText()).isEqualTo(TABLET);
        assertThat(strings(functions.get("functions"))).containsExactly("bar_order", "tablet_pos");

        Map<String, Object> order = new LinkedHashMap<>();
        order.put("idempotency_key", "b35tf-order-" + suffix);
        order.put("bar_id", bar);
        order.put("table_id", table);
        order.put("items", List.of(Map.of("product_id", product, "quantity", 2)));
        String orderId = data(mockMvc.perform(tablet(post("/api/v1/bar/orders"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(order)))
                .andExpect(status().isOk()).andReturn()).get("id").asText();

        String billId = data(mockMvc.perform(tablet(post("/api/v1/bar/orders/" + orderId + "/bill"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("idempotency_key", "b35tf-bill-" + suffix))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payable_amount_cent").value(10_000)).andReturn()).get("id").asText();

        Map<String, Object> intent = new LinkedHashMap<>();
        intent.put("idempotency_key", "b35tf-intent-" + suffix);
        intent.put("business_order_type", "bar_bill");
        intent.put("business_order_id", billId);
        intent.put("amount_cent", 10_000);
        intent.put("currency", "CNY");
        intent.put("channel", "aggregate_pay");
        mockMvc.perform(tablet(post("/api/v1/bar/bills/" + billId + "/pay-intents"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(intent)))
                .andExpect(status().isOk());

        String key = "b35tf-settle-" + suffix;
        Map<String, Object> settle = new LinkedHashMap<>();
        settle.put("idempotency_key", key);
        settle.put("payment_id", key);
        settle.put("settlement_amount_cent", 10_000);
        settle.put("payment_channel", "cash");
        settle.put("cashier_staff_id", user);
        mockMvc.perform(tablet(post("/api/v1/bar/bills/" + billId + "/settle"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(settle)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlement_status").value("settled"));
        assertThat(rejections()).isZero();
    }

    /** ③ 清单外的读写都拒并留痕；④ 同一员工换后台网页会话办核销预检不受平板清单约束。 */
    @Test
    void tabletIsRejectedOutsideBarOrderingWhileWebSessionIsNot() throws Exception {
        String token = seed();

        mockMvc.perform(tablet(post("/api/v1/ticketing/verifications"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idempotency_key", "b35tf-verify-" + suffix, "entitlement_code", "ENT-B35TF-" + suffix))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_NOT_ALLOWED"));
        assertThat(rejections()).isEqualTo(1);

        mockMvc.perform(tablet(post("/api/v1/services/orders"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("idempotency_key", "b35tf-service-" + suffix))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_NOT_ALLOWED"));
        mockMvc.perform(tablet(get("/api/v1/bar/reservations"), token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_NOT_ALLOWED"));
        assertThat(rejections()).isEqualTo(3);

        MvcResult web = mockMvc.perform(post("/api/v1/auth/login").header("X-Terminal-Type", "web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("login_name", user, "password", password))))
                .andExpect(status().isOk()).andReturn();
        Cookie[] cookies = web.getResponse().getCookies();
        String body = mockMvc.perform(post("/api/v1/ticketing/verifications/preview").cookie(cookies)
                        .header("X-Terminal-Type", "web")
                        .header("X-XSRF-TOKEN", web.getResponse().getCookie("XSRF-TOKEN").getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idempotency_key", "b35tf-preview-" + suffix, "entitlement_code", "ENT-B35TF-" + suffix))))
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(body).path("detail_code").asText()).isNotEqualTo("TERMINAL_NOT_ALLOWED");
        assertThat(rejections()).isEqualTo(3);
    }

    /** 造酒吧、吧台位、商品、一台已领用的平板与酒吧员工；按平板发法登录，返回访问令牌。 */
    private String seed() throws Exception {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                + "VALUES (?, 'complex', '平板功能清单回归综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '平板功能清单回归酒吧', 'bar', 'active', now(), now(), ?)", bar, project);
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, '吧台位', 'bar_counter', 4, 0, 0, 'CNY', 'active', now(), now())", table, bar, bar, "BC-TF-" + suffix);
        jdbc.update("INSERT INTO bar_products (id, bar_id, merchant_id, product_name, product_type, price_cent, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, '某某啤酒（整瓶）', 'beer', 5000, 'active', now(), now())", product, bar, bar);
        jdbc.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, merchant_id, status, created_at, updated_at)
                VALUES (?, ?, ?, 'tablet', 'space', ?, 'bound', now(), now())
                """, "td_b35tf_" + suffix, device, device, bar);
        new StaffRequestFixture(jdbc).staff(user, "staff", project, bar, "bar.order.create", "finance.payment_intent.create",
                "finance.payment.register", "verification.execute", "service.order.create", "bar.reservation.view");
        jdbc.update("""
                INSERT INTO auth_credentials (user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                                              password_changed_at, status, created_at, updated_at)
                VALUES (?, ?, 'bcrypt', 0, NULL, now(), 'active', now(), now())
                """, user, new BCryptPasswordEncoder(10).encode(password));
        return data(mockMvc.perform(post("/api/v1/auth/login").header("X-Terminal-Type", TABLET).header("X-Device-Id", device)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("login_name", user, "password", password))))
                .andExpect(status().isOk()).andReturn()).get("access_token").asText();
    }

    private MockHttpServletRequestBuilder tablet(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token).header("X-Terminal-Type", TABLET).header("X-Device-Id", device);
    }

    private int rejections() {
        return jdbc.queryForObject("""
                SELECT count(*) FROM audit_events WHERE event_name = 'terminal.access_rejected'
                   AND metadata->>'terminal_type' = 'tablet' AND metadata->>'reason' = 'tablet_function_not_allowed'
                   AND metadata->>'device' = ?
                """, Integer.class, device);
    }

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private static List<String> strings(JsonNode array) {
        List<String> out = new ArrayList<>();
        array.forEach(node -> out.add(node.asText()));
        return out;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
