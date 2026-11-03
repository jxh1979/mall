package com.complex.platform.ticketing;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.complex.platform.integration.ChannelTicketIssuePort;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-03-R29 真库、真接口（客人端与员工端均按前端真实请求形状，蛇形键）。BSM 第 862 行「查看我的订单、我的票、我的权益和使用记录」；第 880 行
 * 「核销后客人能看到使用记录」；第 892 行「第三方团购（美团/大众点评/抖音）购买的票按渠道来源在“我的订单/我的票”统一展示，渠道未接入前相关入口置灰、
 * 不展示伪造数据」；第 986 行「一体机……客人用手机号短信验证绑定，在官方 App/小程序查看门票」。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class CustomerTicketChannelLinkJdbcIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private ChannelTicketIssuePort channelIssue;

    private CustomerTicketPaymentFixture f;
    private final Set<String> granted = new HashSet<>();

    @BeforeEach
    void seed() {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 20_000L);
    }

    @AfterEach
    void clear() {
        TestSecurityActors.clear();
    }

    @Test
    void ownTicketUsagesListed() throws Exception {
        String ticketId = paidTicket();
        verification(ticketId, "success", Instant.now().minusSeconds(60));
        mockMvc.perform(get("/api/v1/customer/tickets/" + ticketId + "/usages").header("X-Terminal-Type", "miniapp")
                        .header("X-Customer-Token", f.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].result_label").value("成功"))
                .andExpect(jsonPath("$.data.items[0].merchant_name").value("支付组游乐商户"))
                .andExpect(jsonPath("$.data.items[0].verified_at").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].staff_id").doesNotExist());
    }

    @Test
    void otherCustomersTicket403() throws Exception {
        String ticketId = paidTicket();
        String other = "cus_r29_other_" + f.suffix;
        Instant now = Instant.now();
        customers.saveCustomer(new CustomerRecord(other, "13512345678", "135****5678", "active", null, null, now, now));
        customers.saveSession(new CustomerSessionRecord("sess_" + other, other, "tok_" + other, "miniapp", now, now.plusSeconds(3600), "active"));
        mockMvc.perform(get("/api/v1/customer/tickets/" + ticketId + "/usages").header("X-Terminal-Type", "miniapp")
                        .header("X-Customer-Token", "tok_" + other))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_ORDER_FORBIDDEN"));
    }

    @Test
    void channelOrderWithMemberPhoneAppearsInMyOrders() throws Exception {
        String orderId = channelOrder(f.phone);
        assertThat(jdbc.queryForObject("SELECT customer_id FROM ticket_orders WHERE id = ?", String.class, orderId)).isEqualTo(f.customerId);
        f.listOrders().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + orderId + "')].sales_channel").value("meituan"));
        mockMvc.perform(get("/api/v1/customer/my-tickets").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", f.token))
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    @Test
    void channelOrderBeforeRegistrationLinkedOnRegister() throws Exception {
        String phone = "139" + String.format("%08d", Math.floorMod(f.suffix.hashCode() + 7, 100_000_000));
        String orderId = channelOrder(phone);
        assertThat(jdbc.queryForObject("SELECT customer_id FROM ticket_orders WHERE id = ?", String.class, orderId)).isNull();
        String token = register(phone);
        String customerId = customers.findCustomerByPhone(phone).orElseThrow().id();
        assertThat(jdbc.queryForObject("SELECT customer_id FROM ticket_orders WHERE id = ?", String.class, orderId)).isEqualTo(customerId);
        mockMvc.perform(get("/api/v1/customer/my-tickets").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    @Test
    void kioskOrderWithoutSmsCodeRejected() throws Exception {
        RequestPostProcessor cashier = atTicketingKiosk(staff("usr_r29_kiosk_" + f.suffix, "ticket.order.create"));
        kioskOrder(cashier, f.phone, null).andExpect(status().isBadRequest());
        kioskOrder(cashier, f.phone, "999999").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_OTP_INVALID"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_orders WHERE ticket_type_id = ? AND sales_channel = 'kiosk'",
                Integer.class, f.ticketTypeId)).isZero();
    }

    @Test
    void kioskOrderBindsAfterVerification() throws Exception {
        RequestPostProcessor cashier = atTicketingKiosk(staff("usr_r29_kiosk_" + f.suffix, "ticket.order.create"));
        mockMvc.perform(post("/api/v1/ticketing/kiosk-bind-codes").with(cashier).contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("phone", f.phone))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone_masked").isNotEmpty());
        String orderId = CustomerTicketPaymentFixture.read(kioskOrder(cashier, f.phone, "000000").andExpect(status().isOk()), "$.data.id");
        assertThat(jdbc.queryForObject("SELECT customer_id FROM ticket_orders WHERE id = ?", String.class, orderId)).isEqualTo(f.customerId);
    }

    @Test
    void ticketChannelsReportDisabledWhenSwitchOff() throws Exception {
        jdbc.update("UPDATE terminal_capability_switches SET enabled = FALSE WHERE capability_code = 'integration.ticket_channel.sync'"
                + " AND scope_type = 'global'");
        mockMvc.perform(get("/api/v1/customer/ticket-channels").header("X-Terminal-Type", "miniapp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[?(@.channel == 'meituan')].name").value("美团"))
                .andExpect(jsonPath("$.data[?(@.enabled == true)]").isEmpty());
        jdbc.update("UPDATE terminal_capability_switches SET enabled = TRUE WHERE capability_code = 'integration.ticket_channel.sync'"
                + " AND scope_type = 'global'");
        mockMvc.perform(get("/api/v1/customer/ticket-channels").header("X-Terminal-Type", "app"))
                .andExpect(jsonPath("$.data[?(@.enabled == false)]").isEmpty());
    }

    // ---------- helpers ----------

    /** 客人储值付清一张票（付款即出票），返回该票（权益）编号。 */
    private String paidTicket() throws Exception {
        String orderId = f.createOrderId(1);
        f.payWithStoredValue(orderId, null).andExpect(status().isOk());
        return jdbc.queryForObject("SELECT id FROM issued_entitlements WHERE order_id = ?", String.class, orderId);
    }

    private void verification(String entitlementId, String status, Instant at) {
        jdbc.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', now(), now()) ON CONFLICT DO NOTHING",
                "dev_r29_" + f.suffix, "dev_r29_" + f.suffix, "dev_r29_" + f.suffix);
        jdbc.update("""
                INSERT INTO ticket_verifications (id, verification_no, entitlement_id, entitlement_code, credential_type, credential_payload,
                    device_id, point_id, status, staff_id, merchant_id, created_at)
                SELECT ?, ?, id, entitlement_code, 'dynamic_qr', 'payload', ?, 'point_r29', ?, 'usr_gate', ?, ?
                  FROM issued_entitlements WHERE id = ?
                """, "ver_r29_" + UUID.randomUUID(), "V" + UUID.randomUUID().toString().substring(0, 12), "dev_r29_" + f.suffix,
                status, f.merchantId,
                java.sql.Timestamp.from(at), entitlementId);
    }

    /** 第三方渠道回流出票（与美团等回流同一出票主链）。 */
    private String channelOrder(String phone) {
        TestSecurityActors.asPlatformAdmin();
        try {
            return channelIssue.issue(new ChannelTicketIssuePort.IssueCommand("r29_channel_" + UUID.randomUUID(), f.ticketTypeId, null,
                    1, LocalDate.now(), "meituan", phone, "美团回流")).ticketOrderId();
        } finally {
            TestSecurityActors.clear();
        }
    }

    private ResultActions kioskOrder(RequestPostProcessor cashier, String phone, String smsCode) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "r29_kiosk_" + UUID.randomUUID());
        body.put("ticket_type_id", f.ticketTypeId);
        body.put("quantity", 1);
        body.put("visit_date", LocalDate.now().toString());
        body.put("sales_channel", "kiosk");
        body.put("buyer_phone", phone);
        body.put("operator_note", "客人到店补票");
        if (smsCode != null) {
            body.put("sms_code", smsCode);
        }
        return mockMvc.perform(post("/api/v1/ticketing/orders").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(body)));
    }

    /** 第十八批 N 段 120-R2：一体机售票须在本机启用售票的售票型一体机上（一体机终端头＋登记设备号）。 */
    private RequestPostProcessor atTicketingKiosk(RequestPostProcessor who) {
        String device = "dev_r29_kiosk_" + f.suffix;
        jdbc.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, kiosk_mode, status, created_at,"
                + " updated_at) VALUES (?, ?, ?, 'kiosk', 'venue', 'ticketing', 'bound', now(), now()) ON CONFLICT DO NOTHING", device, device, device);
        jdbc.update("INSERT INTO terminal_capability_switches (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason,"
                + " created_at, updated_at) VALUES (?, 'kiosk', 'ticketing', 'device', ?, TRUE, '开启本机售票', now(), now()) ON CONFLICT DO NOTHING",
                "tcs_kiosk_ticketing_" + device, device);
        return request -> {
            MockHttpServletRequest r = who.postProcessRequest(request);
            r.removeHeader("X-Terminal-Type");
            r.addHeader("X-Terminal-Type", "kiosk");
            r.addHeader("X-Device-Id", device);
            return r;
        };
    }

    private String register(String phone) throws Exception {
        String terms = mockMvc.perform(get("/api/v1/customer/terms").header("X-Terminal-Type", "miniapp"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        Map<String, Object> body = Map.of("phone", phone, "sms_code", "000000", "birth_date", "1995-03-01",
                "member_terms_version", com.jayway.jsonpath.JsonPath.read(terms, "$.data.member_terms.version"),
                "privacy_terms_version", com.jayway.jsonpath.JsonPath.read(terms, "$.data.privacy_policy.version"));
        return CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/customer/auth/register").header("X-Terminal-Type", "miniapp")
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body))).andExpect(status().isOk()),
                "$.data.customer_token");
    }

    private RequestPostProcessor staff(String userId, String... permissions) {
        if (granted.add(userId)) {
            String role = "role_" + userId;
            jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '渠道归集回归', 'project', 'active', now())",
                    role, role);
            for (String code : permissions) {
                jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, code);
            }
            jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) VALUES (?, ?, 'staff', ?, NULL, 'active', now())",
                    userId, userId, f.projectId);
            jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        }
        AccessActor actor = new AccessActor(userId, userId, "staff", f.projectId, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
