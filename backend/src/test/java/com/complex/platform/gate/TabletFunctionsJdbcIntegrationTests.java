// 文件版本：V0.02
package com.complex.platform.gate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
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

    @DynamicPropertySource
    static void barImageDir(DynamicPropertyRegistry registry) throws Exception {
        String dir = Files.createTempDirectory("b36-bar-images").toString();
        registry.add("bar.pos.image.dir", () -> dir);
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

    /**
     * 第十八批 P 段第 137 条 137-R1（BSM:945「平板生成支付二维码给客人手机扫码，或扫描客人手机付款码。」；BSM:1028「支付优先使用线上支付：客人手机扫平板二维码，
     * 或平板扫客人手机付款码。」；BSM:1066「……扫客人付款码的收款记操作员为收款人……」）：照安卓 settleBarBillByScan 的发法——生成账单取应收，
     * 按应收建支付意图（wechat），结账带意图编号 payment_id；结清后收款人是平板登录员工、渠道归并为聚合支付。结账不带 payment_id（安卓原发法）400。
     */
    @Test
    void tabletScanCheckoutSettlesWithPaymentIntentId() throws Exception {
        String token = seed();
        JsonNode bill = posBill(token, "scan");
        String billId = bill.get("id").asText();
        long payable = bill.get("payable_amount_cent").asLong();

        Map<String, Object> intent = new LinkedHashMap<>();
        intent.put("idempotency_key", "b36tf-intent-" + suffix);
        intent.put("operator_note", "Android terminal bar payment intent");
        intent.put("evidence_attachment_ids", List.of());
        intent.put("business_order_type", "bar_bill");
        intent.put("business_order_id", billId);
        intent.put("amount_cent", payable);
        intent.put("currency", "CNY");
        intent.put("channel", "wechat");
        String intentId = data(mockMvc.perform(tablet(post("/api/v1/bar/bills/" + billId + "/pay-intents"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(intent)))
                .andExpect(status().isOk()).andReturn()).get("id").asText();

        Map<String, Object> settle = new LinkedHashMap<>();
        settle.put("idempotency_key", "b36tf-settle-" + suffix);
        settle.put("operator_note", "Android terminal bar settlement");
        settle.put("evidence_attachment_ids", List.of());
        settle.put("payment_id", intentId);
        settle.put("settlement_amount_cent", payable);
        settle.put("payment_channel", "wechat");
        settle.put("cashier_staff_id", user);
        mockMvc.perform(tablet(post("/api/v1/bar/bills/" + billId + "/settle"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(settle)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlement_status").value("settled"));
        assertThat(jdbc.queryForMap("SELECT settle_channel, cashier_staff_id FROM bar_bill_settlements WHERE bill_id = ?", billId))
                .containsEntry("settle_channel", "aggregate_pay").containsEntry("cashier_staff_id", user);

        String other = posBill(token, "nopay").get("id").asText();
        settle.put("idempotency_key", "b36tf-settle-nopay-" + suffix);
        settle.remove("payment_id");
        mockMvc.perform(tablet(post("/api/v1/bar/bills/" + other + "/settle"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(settle)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(rejections()).isZero();
    }

    /**
     * 第十八批 P 段第 137 条 137-R1 复核修正（BSM:1064「……该笔金额留在本台位账上，客人可继续点其他产品。……继续点其他产品时，以该笔留款抵付的部分
     * 不重复收款、不重复确认收入。」）：同一开台场次先有退品留款，平板再点单，账单应收被留款抵到 0；照安卓 settleBarBillByScan 的发法不建支付意图，
     * 结账带本次幂等键作 payment_id、settlement_amount_cent=0，返回 200 并结清。
     */
    @Test
    void tabletSettlesZeroDueBillCoveredByRetainedAmount() throws Exception {
        String token = seed();
        String space = "space_b36tf_" + suffix;
        jdbc.update("INSERT INTO spaces (id, project_id, merchant_id, space_name, space_type, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, '平板留款回归卡座区', 'unit', 'active', now(), now())", space, project, bar);
        jdbc.update("UPDATE bar_tables SET space_id = ? WHERE id = ?", space, table);
        jdbc.update("INSERT INTO bar_table_sessions (id, bar_id, merchant_id, business_date, table_id, status, opened_at, created_at, updated_at)"
                + " VALUES (?, ?, ?, current_date, ?, 'open', now(), now(), now())", "ses_b36tf_" + suffix, bar, bar, table);
        String first = posBill(token, "retain").get("id").asText();
        jdbc.update("""
                INSERT INTO bar_bill_adjustments (id, bill_id, adjustment_type, amount_cent, before_payable_amount_cent, after_payable_amount_cent,
                                                  reason, requested_by_user_id, approval_required, status, idempotency_key, created_at)
                VALUES (?, ?, 'min_spend_retained', 10000, 0, 0, '出品后退品留款', ?, false, 'applied', ?, now())
                """, "bill_adj_b36tf_" + suffix, first, user, "b36tf-retain-" + suffix);

        JsonNode bill = posBill(token, "zero");
        assertThat(bill.get("payable_amount_cent").asLong()).isZero();
        String key = "b36tf-settle-zero-" + suffix;
        Map<String, Object> settle = new LinkedHashMap<>();
        settle.put("idempotency_key", key);
        settle.put("operator_note", "Android terminal bar settlement");
        settle.put("evidence_attachment_ids", List.of());
        settle.put("payment_id", key);
        settle.put("settlement_amount_cent", 0);
        settle.put("payment_channel", "wechat");
        settle.put("cashier_staff_id", user);
        mockMvc.perform(tablet(post("/api/v1/bar/bills/" + bill.get("id").asText() + "/settle"), token)
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

    /**
     * 第十八批 P 段第 136 条 136-R2（BSM:958「进入带商品图片的分类菜单，按酒水、饮品、小食等分类挑选；……」；BSM:960「……图片后台上传保存……」）：
     * 后台网页照「商品管理」页的发法（multipart 的 file 与 product_id 两个部件）上传商品图；平板按 POS 发法列商品拿到 image_url，
     * 再照安卓 catalogImageBytes（只带 Bearer 与 X-Terminal-Type: tablet，不带设备头）回读 GET /api/v1/bar/catalog/images/{name}，取回原图；文件不存在 404。
     */
    @Test
    void tabletReadsBackCatalogImageUploadedOnWeb() throws Exception {
        String token = seed();
        MvcResult web = mockMvc.perform(post("/api/v1/auth/login").header("X-Terminal-Type", "web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("login_name", user, "password", password))))
                .andExpect(status().isOk()).andReturn();
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 1, 2, 3};
        String upload = mockMvc.perform(multipart("/api/v1/bar/catalog/images")
                        .file(new MockMultipartFile("file", "beer.png", "image/png", png))
                        .part(new MockPart("product_id", product.getBytes(StandardCharsets.UTF_8)))
                        .cookie(web.getResponse().getCookies()).header("X-Terminal-Type", "web")
                        .header("X-XSRF-TOKEN", web.getResponse().getCookie("XSRF-TOKEN").getValue()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        String imageUrl = data(mockMvc.perform(tablet(get("/api/v1/bar/catalog/products").param("bar_id", bar), token))
                .andExpect(status().isOk()).andReturn()).get(0).get("image_url").asText();
        assertThat(imageUrl).startsWith("bar-images/");
        assertThat(upload).contains(imageUrl);

        MvcResult image = mockMvc.perform(get("/api/v1/bar/catalog/images/" + imageUrl.substring("bar-images/".length()))
                        .header("Authorization", "Bearer " + token).header("X-Terminal-Type", TABLET))
                .andExpect(status().isOk()).andReturn();
        assertThat(image.getResponse().getContentType()).isEqualTo("image/png");
        assertThat(image.getResponse().getContentAsByteArray()).isEqualTo(png);

        mockMvc.perform(get("/api/v1/bar/catalog/images/missing.png")
                        .header("Authorization", "Bearer " + token).header("X-Terminal-Type", TABLET))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("IMAGE_NOT_FOUND"));
    }

    /** 照平板 POS 发法（购物车 items 的数量是字符串）下单两瓶，再由订单生成账单，返回账单。 */
    private JsonNode posBill(String token, String tag) throws Exception {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("idempotency_key", "b36tf-order-" + tag + "-" + suffix);
        order.put("bar_id", bar);
        order.put("table_id", table);
        order.put("items", List.of(Map.of("product_id", product, "quantity", "2")));
        String orderId = data(mockMvc.perform(tablet(post("/api/v1/bar/orders"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(order)))
                .andExpect(status().isOk()).andReturn()).get("id").asText();
        return data(mockMvc.perform(tablet(post("/api/v1/bar/orders/" + orderId + "/bill"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("idempotency_key", "b36tf-bill-" + tag + "-" + suffix,
                                "operator_note", "Android terminal bar order bill", "evidence_attachment_ids", List.of()))))
                .andExpect(status().isOk()).andReturn());
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
                "finance.payment.register", "verification.execute", "service.order.create", "bar.reservation.view",
                "bar.product.view", "bar.product.manage");
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
