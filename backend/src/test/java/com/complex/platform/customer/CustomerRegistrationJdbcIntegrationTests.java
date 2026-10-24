package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-03-R2 / R3 真库、真接口，请求体按客人端真实形状（蛇形键）。
 * <ul>
 *   <li>R3 BSM 第 886 行「创建会员身份必须完成手机号短信验证、填写生日、满足平台年龄要求，并同意当前会员条款和隐私条款。条件全部满足即自动
 *       创建有效会员，否则注册失败且不创建身份；已关闭状态的手机号拒绝再次注册」；操作员裁决（第十三批第 3 条）「满 18 周岁」。</li>
 *   <li>R2 PGM 第 745 行「客人手机应用、客人小程序、Web 前端（业务人员，客人无网页端）」；第 744 行「前端隐藏只是辅助，后端接口必须独立校验」；
 *       BSM 第 1611 行「订单从哪里来，是官方 App/小程序……」。本类把 app、miniapp 翻成 enforce（与 application-prod.yml 一致）。</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "access.device_credential.enforcement.app=enforce",
        "access.device_credential.enforcement.miniapp=enforce"
})
@AutoConfigureMockMvc
@Transactional
class CustomerRegistrationJdbcIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ObjectMapper springJson;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // ---------- R3 注册建号 ----------

    @Test
    void unknownPhoneLoginReturnsRegistrationRequiredAndCreatesNothing() throws Exception {
        String phone = newPhone();
        login("miniapp", phone)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_REGISTRATION_REQUIRED"));
        assertThat(customerRepository.findCustomerByPhone(phone)).isEmpty();
    }

    @Test
    void registerCreatesActiveMemberWithBirthDateAndTwoConsents() throws Exception {
        String phone = newPhone();
        Map<String, String> terms = currentTerms();
        String token = text(register("app", body(phone, "1990-05-01", terms))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customer_token").isNotEmpty()), "/data/customer_token");

        CustomerRecord customer = customerRepository.findCustomerByPhone(phone).orElseThrow();
        assertThat(customer.status()).isEqualTo("active");
        assertThat(customer.birthDate()).isEqualTo(LocalDate.parse("1990-05-01"));
        List<Map<String, Object>> consents = jdbc.queryForList(
                "SELECT consent_type, accepted, version, source_terminal FROM customer_consents WHERE customer_id = ? ORDER BY consent_type",
                customer.id());
        assertThat(consents).extracting(row -> row.get("consent_type")).containsExactly("member_terms", "privacy_policy");
        assertThat(consents).extracting(row -> row.get("version"))
                .containsExactly(terms.get("member_terms"), terms.get("privacy_policy"));
        assertThat(consents).allSatisfy(row -> {
            assertThat(row.get("accepted")).isEqualTo(true);
            assertThat(row.get("source_terminal")).isEqualTo("app");
        });
        assertThat(customerRepository.findSessionByToken(token)).get()
                .extracting(CustomerSessionRecord::scene).isEqualTo("app");
        // 注册后同号登录照常（已有账号不再走注册）。
        login("app", phone).andExpect(status().isOk());
    }

    @Test
    void registerWithoutBirthDateCreatesNothing() throws Exception {
        String phone = newPhone();
        Map<String, Object> body = body(phone, null, currentTerms());
        register("miniapp", body).andExpect(status().isBadRequest());
        assertThat(customerRepository.findCustomerByPhone(phone)).isEmpty();
    }

    @Test
    void registerBelowAgeThresholdCreatesNothing() throws Exception {
        String phone = newPhone();
        LocalDate today = LocalDate.now(com.complex.platform.common.LocalZone.ID);
        register("miniapp", body(phone, today.minusYears(18).plusDays(1).toString(), currentTerms()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_UNDER_AGE"));
        assertThat(customerRepository.findCustomerByPhone(phone)).isEmpty();
        // 当天满 18 周岁即可注册。
        register("miniapp", body(phone, today.minusYears(18).toString(), currentTerms())).andExpect(status().isOk());
    }

    @Test
    void registerWithOutdatedTermsVersionCreatesNothing() throws Exception {
        String phone = newPhone();
        Map<String, String> terms = new LinkedHashMap<>(currentTerms());
        terms.put("privacy_policy", "0000000000000000");
        register("miniapp", body(phone, "1990-05-01", terms))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_TERMS_OUTDATED"));
        assertThat(customerRepository.findCustomerByPhone(phone)).isEmpty();
    }

    @Test
    void closedPhoneCannotRegister() throws Exception {
        String phone = newPhone();
        Instant now = Instant.now();
        customerRepository.saveCustomer(new CustomerRecord("cust_closed_" + suffix, phone, "1**", "blacklisted",
                null, null, now, now));
        register("miniapp", body(phone, "1990-05-01", currentTerms()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_PHONE_CLOSED"));
    }

    @Test
    void existingPhoneRegisterRejectedAsAlreadyRegistered() throws Exception {
        String phone = newPhone();
        register("miniapp", body(phone, "1990-05-01", currentTerms())).andExpect(status().isOk());
        register("miniapp", body(phone, "1990-05-01", currentTerms()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_ALREADY_REGISTERED"));
    }

    @Test
    void termsEndpointReturnsBothDocumentsWithVersions() throws Exception {
        mockMvc.perform(get("/api/v1/customer/terms").header("X-Terminal-Type", "miniapp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.member_terms.version").isNotEmpty())
                .andExpect(jsonPath("$.data.member_terms.content").isNotEmpty())
                .andExpect(jsonPath("$.data.privacy_policy.version").isNotEmpty())
                .andExpect(jsonPath("$.data.privacy_policy.content").isNotEmpty());
    }

    // ---------- R2 终端证明与来源 ----------

    @Test
    void phoneLoginFromWebTerminalRejected() throws Exception {
        String phone = newPhone();
        register("miniapp", body(phone, "1990-05-01", currentTerms())).andExpect(status().isOk());
        login("web", phone)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_TERMINAL_NOT_ALLOWED"));
    }

    @Test
    void sessionSceneLockedToLoginTerminal() throws Exception {
        String token = registeredToken("app");
        mockMvc.perform(get("/api/v1/customer/orders").header("X-Terminal-Type", "app").header("X-Customer-Token", token))
                .andExpect(status().isOk());
        // 小程序会话的凭证拿到 App 端（或反之）一律拒绝，任何模式都拒。
        mockMvc.perform(get("/api/v1/customer/orders").header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("DEVICE_CREDENTIAL_INVALID"));
    }

    @Test
    void appWriteWithMatchingCustomerSessionAllowedUnderEnforce() throws Exception {
        String token = registeredToken("app");
        consent("app", token).andExpect(status().isOk());
    }

    @Test
    void appWriteWithoutSessionRejectedInEnforce() throws Exception {
        consent("app", null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("DEVICE_CREDENTIAL_INVALID"));
    }

    @Test
    void consentSourceFollowsVerifiedTerminalNotRequestBody() throws Exception {
        String token = registeredToken("app");
        String id = text(consent("app", token).andExpect(status().isOk()), "/data/id");
        assertThat(jdbc.queryForObject("SELECT source_terminal FROM customer_consents WHERE id = ?", String.class, id))
                .isEqualTo("app");
    }

    @Test
    void appMasterSwitchOffRejectsCustomerWriteEvenWithValidSession() throws Exception {
        String token = registeredToken("app");
        jdbc.update("UPDATE terminal_capability_switches SET enabled = FALSE WHERE terminal_type = 'app'"
                + " AND capability_code = 'terminal.enabled' AND scope_type = 'global'");
        consent("app", token)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
    }

    @Test
    void appModuleSwitchOffRejectsThatModuleOnly() throws Exception {
        String token = registeredToken("app");
        jdbc.update("""
                INSERT INTO terminal_capability_switches (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
                VALUES (?, 'app', 'terminal.enabled', 'module', 'customer_consent', FALSE, '停用同意模块', now(), now())
                ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO UPDATE SET enabled = FALSE""", "tcs_r2_mod_" + suffix);
        mockMvc.perform(post("/api/v1/customer/consents").header("X-Terminal-Type", "app").header("X-Customer-Token", token)
                        .header("X-Module-Code", "customer_consent").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("idempotency_key", "r2_mod_" + suffix, "consent_type", "marketing",
                                "accepted", true, "version", "v1"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        consent("app", token).andExpect(status().isOk());
    }

    @Test
    void ticketOrderSalesChannelFollowsVerifiedTerminal() throws Exception {
        String ticketTypeId = publishedTicketType();
        String token = registeredToken("app");
        mockMvc.perform(post("/api/v1/customer/ticket-orders").header("X-Terminal-Type", "app").header("X-Customer-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of(
                                "idempotency_key", "r2_order_" + suffix,
                                "ticket_type_id", ticketTypeId,
                                "quantity", 1,
                                "visit_date", LocalDate.now().toString(),
                                "contact_phone", newPhone()))))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT sales_channel FROM ticket_orders WHERE ticket_type_id = ?", String.class, ticketTypeId))
                .isEqualTo("app");
    }

    @Test
    void effectiveSwitchesReadableAnonymouslyForApp() throws Exception {
        String id = "tcs_r2_" + suffix;
        jdbc.update("""
                INSERT INTO terminal_capability_switches (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
                VALUES (?, 'app', 'terminal.enabled', 'module', ?, FALSE, '停用积分模块', now(), now())""", id, "points_" + suffix);
        mockMvc.perform(get("/api/v1/customer/terminal-switches/effective").header("X-Terminal-Type", "app"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + id + "')].scope_id").value("points_" + suffix));
        mockMvc.perform(get("/api/v1/customer/terminal-switches/effective").header("X-Terminal-Type", "web"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_TERMINAL_NOT_ALLOWED"));
    }

    // ---------- helpers ----------

    /** 已发布、已审批的单人游乐票（与 TicketPurchaseWithoutFaceJdbcIntegrationTests 同一套最小夹具）。 */
    private String publishedTicketType() {
        String ticketTypeId = "tt_r2_" + suffix;
        String projectId = "project_r2_" + suffix;
        String merchantId = "merchant_r2_" + suffix;
        String templateId = "et_r2_" + suffix;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '客人端来源测试综合体', 'active', now(), now())", projectId);
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
                + " VALUES (?, ?, ?, 1, 1, 1, 5000, 5000, now(), now())", "bind_r2_" + suffix, ticketTypeId, templateId);
        com.complex.platform.support.TicketSalesContractTestFixtures.approvedPublished(jdbc, springJson, ticketTypeId, projectId, merchantId);
        return ticketTypeId;
    }

    private String registeredToken(String terminal) throws Exception {
        return text(register(terminal, body(newPhone(), "1990-05-01", currentTerms())).andExpect(status().isOk()),
                "/data/customer_token");
    }

    private ResultActions consent(String terminal, String token) throws Exception {
        var request = post("/api/v1/customer/consents").header("X-Terminal-Type", terminal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of(
                        "idempotency_key", "r2_consent_" + UUID.randomUUID(),
                        "consent_type", "marketing",
                        "accepted", true,
                        "version", "v1",
                        "source_terminal", "miniapp")));
        if (token != null) {
            request.header("X-Customer-Token", token);
        }
        return mockMvc.perform(request);
    }

    private ResultActions login(String terminal, String phone) throws Exception {
        return mockMvc.perform(post("/api/v1/customer/auth/phone-login").header("X-Terminal-Type", terminal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("phone", phone, "sms_code", "000000"))));
    }

    private ResultActions register(String terminal, Map<String, ?> body) throws Exception {
        return mockMvc.perform(post("/api/v1/customer/auth/register").header("X-Terminal-Type", terminal)
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)));
    }

    private Map<String, String> currentTerms() throws Exception {
        JsonNode data = JSON.readTree(mockMvc.perform(get("/api/v1/customer/terms").header("X-Terminal-Type", "miniapp"))
                .andReturn().getResponse().getContentAsString()).path("data");
        return Map.of("member_terms", data.path("member_terms").path("version").asText(),
                "privacy_policy", data.path("privacy_policy").path("version").asText());
    }

    private static Map<String, Object> body(String phone, String birthDate, Map<String, String> terms) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("phone", phone);
        body.put("sms_code", "000000");
        if (birthDate != null) {
            body.put("birth_date", birthDate);
        }
        body.put("member_terms_version", terms.get("member_terms"));
        body.put("privacy_terms_version", terms.get("privacy_policy"));
        return body;
    }

    private static String text(ResultActions actions, String pointer) throws Exception {
        return JSON.readTree(actions.andReturn().getResponse().getContentAsString()).at(pointer).asText();
    }

    private static String newPhone() {
        return "13" + String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000));
    }
}
