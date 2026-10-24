package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.crypto.BlindIndex;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-03-R22 真库：ENUM 第 68 行「同一手机号在一年内注销与注册合计超过后台配置的次数上限的，触发异常预警交风控查看处置（业务参数，
 * 全平台统一可配，出厂默认一年内合计 3 次，须同时配置上下限）；预警只作提示，不自动阻断注册。为完成上述计数……系统仅留存注销事件与该手机号的
 * 对应记录至一年统计窗口届满」。注销后同号释放（BSM「账户注销」）由 R21 落地，本类用历史注销记录行模拟此前的注册—注销。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class CustomerChurnAlertJdbcIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BlindIndex blindIndex;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private CustomerDeregistrationMarkPurgeJob purgeJob;

    private final String phone = "13" + String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000));
    private final Instant now = Instant.now();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void fourthEventDeregistrationRaisesAlert() throws Exception {
        mark(now.minus(Duration.ofDays(100)), now.minus(Duration.ofDays(90)));   // 第 1、2 个事件
        String token = register().andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String customerToken = JSON.readTree(token).at("/data/customer_token").asText();
        String customerId = JSON.readTree(token).at("/data/customer_id").asText();
        assertThat(alerts(customerId)).isZero();                                    // 第 3 个事件：未超过 3 次

        mockMvc.perform(post("/api/v1/customer/deregister").header("X-Terminal-Type", "miniapp")
                        .header("X-Customer-Token", customerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"waive_balance\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("deregistered"));      // 预警只作提示，注销照常
        assertThat(alerts(customerId)).isEqualTo(1);                               // 第 4 个事件：超过 3 次
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_deregistration_marks WHERE phone_hash = ?",
                Integer.class, blindIndex.hash(phone))).isEqualTo(2);
        String summary = jdbc.queryForObject("SELECT summary FROM risk_alerts WHERE source_ref = ? AND alert_type = 'customer.churn'",
                String.class, customerId);
        assertThat(summary).contains("4").doesNotContain(phone);
    }

    @Test
    void fifthEventRegistrationAlsoAlertsButSucceeds() throws Exception {
        mark(now.minus(Duration.ofDays(200)), now.minus(Duration.ofDays(150)));
        mark(now.minus(Duration.ofDays(100)), now.minus(Duration.ofDays(30)));
        String body = register().andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String customerId = JSON.readTree(body).at("/data/customer_id").asText();
        assertThat(alerts(customerId)).isEqualTo(1);
        assertThat(customerRepository.findCustomerByPhone(phone)).get().extracting(CustomerRecord::status).isEqualTo("active");
    }

    @Test
    void eventsOlderThanOneYearNotCounted() throws Exception {
        mark(now.minus(Duration.ofDays(500)), now.minus(Duration.ofDays(450)));
        mark(now.minus(Duration.ofDays(420)), now.minus(Duration.ofDays(380)));
        String body = register().andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(alerts(JSON.readTree(body).at("/data/customer_id").asText())).isZero();
    }

    @Test
    void settingRequiresBoundsTogetherAndRejectsOutOfBounds() throws Exception {
        RequestPostProcessor admin = platformStaff("usr_r22_" + UUID.randomUUID().toString().substring(0, 8));
        mockMvc.perform(get("/api/v1/customer-churn-alert-setting").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.threshold").value(3));
        save(admin, Map.of("threshold", 5)).andExpect(status().isBadRequest());
        save(admin, Map.of("threshold", 5, "min_threshold", 1, "max_threshold", 4)).andExpect(status().isBadRequest());
        save(admin, Map.of("threshold", 1, "min_threshold", 1, "max_threshold", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.threshold").value(1))
                .andExpect(jsonPath("$.data.max_threshold").value(10));
        // 新上限立即生效：历史 1 条注销记录（2 个事件）＋本次注册 = 3 次 > 1。
        mark(now.minus(Duration.ofDays(100)), now.minus(Duration.ofDays(90)));
        SecurityContextHolder.clearContext();
        String body = register().andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(alerts(JSON.readTree(body).at("/data/customer_id").asText())).isEqualTo(1);
    }

    @Test
    void markPurgedAfterOneYearWindow() {
        String expired = mark(now.minus(Duration.ofDays(500)), now.minus(Duration.ofDays(370)));
        String kept = mark(now.minus(Duration.ofDays(100)), now.minus(Duration.ofDays(90)));
        purgeJob.purge();
        List<String> left = jdbc.queryForList("SELECT id FROM customer_deregistration_marks WHERE id IN (?, ?)", String.class, expired, kept);
        assertThat(left).containsExactly(kept);
    }

    // ---------- helpers ----------

    private String mark(Instant registeredAt, Instant deregisteredAt) {
        String id = "cdm_t_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        jdbc.update("INSERT INTO customer_deregistration_marks (id, phone_hash, registered_at, deregistered_at, purge_after) VALUES (?, ?, ?, ?, ?)",
                id, blindIndex.hash(phone), Timestamp.from(registeredAt), Timestamp.from(deregisteredAt),
                Timestamp.from(deregisteredAt.plus(Duration.ofDays(365))));
        return id;
    }

    private int alerts(String customerId) {
        return jdbc.queryForObject("SELECT count(*) FROM risk_alerts WHERE source_ref = ? AND alert_type = 'customer.churn'",
                Integer.class, customerId);
    }

    private ResultActions register() throws Exception {
        JsonNode terms = JSON.readTree(mockMvc.perform(get("/api/v1/customer/terms").header("X-Terminal-Type", "miniapp"))
                .andReturn().getResponse().getContentAsString()).path("data");
        return mockMvc.perform(post("/api/v1/customer/auth/register").header("X-Terminal-Type", "miniapp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("phone", phone, "sms_code", "000000", "birth_date", "1990-05-01",
                        "member_terms_version", terms.path("member_terms").path("version").asText(),
                        "privacy_terms_version", terms.path("privacy_policy").path("version").asText()))));
    }

    private ResultActions save(RequestPostProcessor who, Map<String, Object> body) throws Exception {
        Map<String, Object> actual = new java.util.LinkedHashMap<>(body);
        actual.put("idempotency_key", "r22_" + UUID.randomUUID());
        return mockMvc.perform(put("/api/v1/customer-churn-alert-setting").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(actual)));
    }

    /** 平台员工账号（无项目、无商户；按真实授权链持 customer.account.manage）。 */
    private RequestPostProcessor platformStaff(String userId) {
        String role = "role_" + userId;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '预警参数回归', 'platform', 'active', now())",
                role, role);
        jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'customer.account.manage')", role);
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) VALUES (?, ?, 'staff', NULL, NULL, 'active', now())",
                userId, userId);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        AccessActor actor = new AccessActor(userId, userId, "staff", null, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
