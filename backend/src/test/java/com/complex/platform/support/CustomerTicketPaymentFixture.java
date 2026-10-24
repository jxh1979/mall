package com.complex.platform.support;

// 文件版本：V0.01

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.complex.platform.customer.CustomerStoredValueAccountRecord;
import com.complex.platform.finance.gateway.PaymentClientLaunch;
import com.complex.platform.finance.gateway.PaymentGatewayRequest;
import com.complex.platform.finance.gateway.PaymentGatewayResult;
import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * ROOT-03 B 段支付组（R17/R18/R20/R4/R27）共用造数：一张已发布、可出票的游乐票（每张 5000 分）、一位持小程序会话的客人、
 * 一张本项目储值卡；客人端请求一律按前端真实形状（蛇形键、X-Terminal-Type、X-Customer-Token）。
 * 支付渠道用替身：发起即返回渠道单号与小程序拉起参数，{@link #markPaid} 后查单返回与发起一致的成功回执。
 */
public final class CustomerTicketPaymentFixture {

    public static final String GATEWAY = "test_gateway";

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    public final String projectId = "project_b14pay_" + suffix;
    public final String merchantId = "merchant_b14pay_" + suffix;
    public final String ticketTypeId = "tt_b14pay_" + suffix;
    public final String customerId = "cus_b14pay_" + suffix;
    public final String token = "cust_token_b14pay_" + suffix;
    public final String accountId = "svacc_b14pay_" + suffix;
    public final String phone;
    private final Map<String, PaymentGatewayRequest> initiated = new ConcurrentHashMap<>();
    private final Set<String> paidRefs = ConcurrentHashMap.newKeySet();

    public CustomerTicketPaymentFixture(MockMvc mockMvc, JdbcTemplate jdbc, ObjectMapper json, CustomerRepository customers,
            long cardBalanceCent) {
        this.mockMvc = mockMvc;
        this.jdbc = jdbc;
        this.json = json;
        this.phone = "137" + String.format("%08d", Math.floorMod(suffix.hashCode(), 100_000_000));
        String templateId = "et_b14pay_" + suffix;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '支付组测试综合体', 'active', now(), now())", projectId);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '支付组游乐商户', 'amusement', 'active', now(), now(), ?)", merchantId, projectId);
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent,"
                + " holiday_price_cent, stock_required, status, validity_days, created_at, updated_at)"
                + " VALUES (?, '支付组游乐票', 'flat', 5000, 5000, 5000, FALSE, 'published', 1, now(), now())", ticketTypeId);
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,"
                + " merchant_id, entitlement_kind, consumption_window, created_at, updated_at)"
                + " VALUES (?, '游乐项目', 'amusement', 1, 5000, 'active', ?, 'virtual', 'per_use', now(), now())", templateId, merchantId);
        jdbc.update("INSERT INTO ticket_type_entitlements (id, ticket_type_id, entitlement_template_id, quantity, total_times,"
                + " num_people, value_amount_cent, component_price_cent, created_at, updated_at)"
                + " VALUES (?, ?, ?, 1, 1, 1, 5000, 5000, now(), now())", "bind_b14pay_" + suffix, ticketTypeId, templateId);
        TicketSalesContractTestFixtures.approvedPublished(jdbc, json, ticketTypeId, projectId, merchantId);
        Instant now = Instant.now();
        customers.saveCustomer(new CustomerRecord(customerId, phone, "137****" + phone.substring(7), "active", null, null, now, now));
        customers.saveSession(new CustomerSessionRecord("sess_b14pay_" + suffix, customerId, token, "miniapp", now,
                now.plusSeconds(3600), "active"));
        customers.saveStoredValueAccount(new CustomerStoredValueAccountRecord(accountId, customerId, projectId, null,
                cardBalanceCent, "active", now, now, null, cardBalanceCent, 0L));
    }

    /** 渠道替身：已开启；发起返回待支付＋小程序拉起参数；被 {@link #markPaid} 的渠道单查单返回成功回执。 */
    public void stubGateway(PaymentGatewayRouter gateway) {
        when(gateway.isGatewayEnabled()).thenReturn(true);
        when(gateway.initiatePayment(any())).thenAnswer(invocation -> {
            PaymentGatewayRequest request = invocation.getArgument(0);
            String ref = "ref_" + UUID.randomUUID().toString().replace("-", "");
            initiated.put(ref, request);
            return new PaymentGatewayResult(PaymentGatewayResult.Status.PENDING, GATEWAY, ref, "pending", null, null,
                    new PaymentClientLaunch("miniapp_wechat", Map.of("timeStamp", "1700000000", "package", "prepay_id=" + ref)));
        });
        when(gateway.queryStatus(anyString())).thenAnswer(invocation -> {
            String ref = invocation.getArgument(0);
            PaymentGatewayRequest request = initiated.get(ref);
            if (request == null || !paidRefs.contains(ref)) {
                return new PaymentGatewayResult(PaymentGatewayResult.Status.PENDING, GATEWAY, ref, "pending");
            }
            return new PaymentGatewayResult(PaymentGatewayResult.Status.SUCCEEDED, GATEWAY, ref, "", null,
                    new PaymentGatewayRequest(request.businessOrderType(), request.businessOrderId(), request.amountCent(),
                            request.currency(), request.channel(), ref, request.payeeMerchantId(), request.fundMode()));
        });
    }

    /** 最近一次发起的渠道请求（核对付款端等入参）。 */
    public PaymentGatewayRequest initiatedFor(String paymentIntentId) {
        return initiated.get(gatewayRef(paymentIntentId));
    }

    public void markPaid(String paymentIntentId) {
        paidRefs.add(gatewayRef(paymentIntentId));
    }

    public String gatewayRef(String paymentIntentId) {
        return jdbc.queryForObject("SELECT gateway_ref FROM finance_payment_intents WHERE id = ?", String.class, paymentIntentId);
    }

    public ResultActions createOrder(int quantity, Map<String, Object> extra) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "b14pay-order-" + UUID.randomUUID());
        body.put("ticket_type_id", ticketTypeId);
        body.put("quantity", quantity);
        body.put("visit_date", LocalDate.now().toString());
        body.put("contact_phone", phone);
        body.putAll(extra);
        return customerPost("/api/v1/customer/ticket-orders", body);
    }

    public String createOrderId(int quantity) throws Exception {
        return read(createOrder(quantity, Map.of()), "$.data.id");
    }

    /** 储值付款（amountCent 为空＝付清剩余应付）。 */
    public ResultActions payWithStoredValue(String orderId, Long amountCent) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "b14pay-sv-" + UUID.randomUUID());
        body.put("account_id", accountId);
        body.put("phone", phone);
        body.put("sms_code", "000000");
        if (amountCent != null) {
            body.put("amount_cent", amountCent);
        }
        return customerPost("/api/v1/customer/orders/" + orderId + "/stored-value/pay", body);
    }

    /** 在线付款发起：请求体里的金额应被服务端忽略。 */
    public ResultActions startOnlinePayment(String orderId, String channel) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "b14pay-online-" + UUID.randomUUID());
        body.put("channel", channel);
        body.put("amount_cent", 1);
        return customerPost("/api/v1/customer/orders/" + orderId + "/payments", body);
    }

    public ResultActions confirmOnlinePayment(String orderId, String paymentIntentId) throws Exception {
        return customerPost("/api/v1/customer/orders/" + orderId + "/payments/" + paymentIntentId + "/confirm",
                Map.of("idempotency_key", "b14pay-confirm-" + UUID.randomUUID()));
    }

    public ResultActions listOrders() throws Exception {
        return mockMvc.perform(get("/api/v1/customer/orders").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token));
    }

    public ResultActions customerPost(String path, Map<String, Object> body) throws Exception {
        Object key = body.get("idempotency_key");
        return mockMvc.perform(post(path).header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token)
                .header("Idempotency-Key", key == null ? "b14pay-" + UUID.randomUUID() : key.toString())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    public long cardBalance() {
        return jdbc.queryForObject("SELECT principal_cent + bonus_cent FROM customer_stored_value_accounts WHERE id = ?",
                Long.class, accountId);
    }

    public Map<String, Object> order(String orderId) {
        return jdbc.queryForMap("SELECT pay_status, issue_status, status, amount_cent FROM ticket_orders WHERE id = ?", orderId);
    }

    public int entitlements(String orderId) {
        return jdbc.queryForObject("SELECT count(*) FROM issued_entitlements WHERE order_id = ?", Integer.class, orderId);
    }

    public static String read(ResultActions result, String path) throws Exception {
        Object value = JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), path);
        return value == null ? null : value.toString();
    }
}
