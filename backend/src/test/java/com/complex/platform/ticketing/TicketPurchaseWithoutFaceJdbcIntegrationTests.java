package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest;
import com.complex.platform.ticketing.dto.TicketIssueRequest;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * TK-06-R1（BSM:843 / ENUM:60 / PGM:891「不设需要人脸照的票种；购票和转赠均不要求录入人脸照」；「多人票与单次票不留人脸」）：
 * 游乐票（原 V229 默认「需要人脸照」的票种）在客人端、柜台下单、收款、出票全程不要人脸，也不落任何人脸照；客人端商品不再下发人脸要求。
 * 客人端请求按 H5 真实形状（蛇形键，不带 face_photos / face_consent）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TicketPurchaseWithoutFaceJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private TicketOrderService orderService;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String ticketTypeId = "tt_tk06r1_" + suffix;

    @BeforeEach
    void seed() {
        String projectId = "project_tk06r1_" + suffix;
        String merchantId = "merchant_tk06r1_" + suffix;
        String templateId = "et_tk06r1_" + suffix;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '游乐票测试综合体', 'active', now(), now())", projectId);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '游乐测试商户', 'amusement', 'active', now(), now(), ?)", merchantId, projectId);
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent,"
                + " holiday_price_cent, stock_required, status, validity_days, created_at, updated_at)"
                + " VALUES (?, '游乐票', 'flat', 5000, 5000, 5000, FALSE, 'published', 1, now(), now())", ticketTypeId);
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,"
                + " merchant_id, entitlement_kind, consumption_window, created_at, updated_at)"
                + " VALUES (?, '游乐项目', 'amusement', 1, 5000, 'active', ?, 'virtual', 'per_use', now(), now())", templateId, merchantId);
        jdbc.update("INSERT INTO ticket_type_entitlements (id, ticket_type_id, entitlement_template_id, quantity, total_times,"
                + " num_people, value_amount_cent, component_price_cent, created_at, updated_at)"
                + " VALUES (?, ?, ?, 1, 1, 1, 5000, 5000, now(), now())",
                "bind_tk06r1_" + suffix, ticketTypeId, templateId);
        com.complex.platform.support.TicketSalesContractTestFixtures.approvedPublished(jdbc, json, ticketTypeId, projectId, merchantId);
        // V525 起已无「需要人脸照」开关：模板表不再有该列。
        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_name = 'entitlement_templates'"
                + " AND column_name = 'require_face_photo'", Integer.class)).isZero();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // R1 RED：客人端 H5 按真实形状（无 face_photos / face_consent）下单游乐票 → 成单。
    @Test
    void customerOrderForAmusementTicketSucceedsWithoutPhotos() throws Exception {
        String customerId = "cus_tk06r1_" + suffix;
        String token = "cust_token_tk06r1_" + suffix;
        String digits = String.format("%08d", Math.floorMod(suffix.hashCode(), 100_000_000));
        Instant now = Instant.now();
        customerRepository.saveCustomer(new CustomerRecord(customerId, "136" + digits, "136****" + digits.substring(4),
                "active", null, null, now, now));
        customerRepository.saveSession(new CustomerSessionRecord(
                "sess_tk06r1_" + suffix, customerId, token, "miniapp", now, now.plusSeconds(3600), "active"));

        String body = json.writeValueAsString(Map.of(
                "idempotency_key", "tk06r1-cust-" + suffix,
                "ticket_type_id", ticketTypeId,
                "quantity", 1,
                "visit_date", LocalDate.now().toString(),
                "contact_phone", "136" + digits));
        mockMvc.perform(post("/api/v1/customer/ticket-orders")
                        .header("X-Terminal-Type", "web")
                        .header("X-Customer-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticket_type_id").value(ticketTypeId));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_orders WHERE ticket_type_id = ? AND customer_id = ?",
                Integer.class, ticketTypeId, customerId)).isEqualTo(1);
    }

    // R1 RED：柜台下单 → 收款 → 出票全程不带任何人脸字段 → 成功，且不落人脸照。
    @Test
    void counterOrderPaymentAndIssueSucceedWithoutFaceConsent() {
        TestSecurityActors.asPlatformAdmin();
        TicketOrderRecord order = assertDoesNotThrow(() -> orderService.createOrder(new TicketOrderRequest(
                "tk06r1-order-" + suffix, null, List.of(), ticketTypeId, null, 1, LocalDate.now(), "admin", null, null)));
        TicketPaymentIntentRecord intent = orderService.createPaymentIntent(order.id(), new PaymentIntentRequest(
                "tk06r1-pay-" + suffix, null, List.of(), "ticket_order", order.id(), order.amountCent(), "CNY", "cash"));
        orderService.confirmPayment(order.id(), new ConfirmTicketOrderPaymentRequest("tk06r1-confirm-" + suffix, intent.id(), null));
        IssuedTicketResponse issued = orderService.issueEntitlements(ticketTypeId, new TicketIssueRequest(
                "tk06r1-issue-" + suffix, "issue", List.of(), order.id(), "admin", List.of("dynamic_qr")));

        assertThat(issued.issuedEntitlements()).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_face_photos p JOIN ticket_instances i"
                + " ON i.id = p.ticket_instance_id WHERE i.order_id = ?", Integer.class, order.id())).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_consents WHERE order_id = ?", Integer.class, order.id()))
                .isZero();
    }

    // R1 RED：客人端商品列表不再下发「是否需要人脸照 / 人脸照留存天数」。
    @Test
    void customerTicketProductsExposeNoFaceRequirement() throws Exception {
        String body = mockMvc.perform(get("/api/v1/customer/ticket-products").header("X-Terminal-Type", "web"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains(ticketTypeId).doesNotContain("require_face_photo").doesNotContain("face_photo_retention_days");
    }
}
