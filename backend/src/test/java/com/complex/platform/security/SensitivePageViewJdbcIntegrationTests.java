// 文件版本：V0.01
package com.complex.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * 第 23 条 23-R5 敏感页打开留痕。PGM:687「打开敏感页这件事本身也要留痕，记下是谁打开的、什么时间打开的、打开的是哪一个页面、
 * 这一次看到的是哪些对象（哪个客人、哪个商户、哪些单据）；这条留痕不提供关闭开关」；DEC-R3B-2026-08-04-14「兜底关闭不含留痕的写入」。
 * 走真实生产登录链与真实拦截链，按后台外壳的请求头与蛇形键请求。
 */
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "complex.database.credential-separation.enforced=false",
        "refund.tiered_authorization.enabled=true",
        "complex.auth.production.seed-admin.username=p0_sensitive_admin",
        "complex.auth.production.seed-admin.password=p0_sensitive_password",
        "ticket.credential.signing.secret=prod-test-ticket-signing-secret-32-bytes-minimum",
        "bar.invitation.signing.secret=prod-test-bar-invitation-signing-secret-32-bytes-minimum",
        "compliance.wecom.webhook.signing.secret=prod-test-wecom-webhook-signing-secret-32-bytes-minimum",
        "keyvault.keys.pii=prod-test-pii-keyvault-secret-32-bytes-minimum",
        "keyvault.keys.pii-index=prod-test-pii-index-blind-secret-32-bytes-minimum"
})
@Transactional
class SensitivePageViewJdbcIntegrationTests {

    private static final KeyPair MASTER_KEY_PAIR = generateMasterKeyPair();

    @DynamicPropertySource
    static void masterPublicKey(DynamicPropertyRegistry registry) {
        registry.add("complex.auth.master.public-key",
                () -> Base64.getEncoder().encodeToString(MASTER_KEY_PAIR.getPublic().getEncoded()));
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;

    private final String user = "b28sp_viewer_" + UUID.randomUUID().toString().substring(0, 8);

    @Test
    void sensitivePageViewIsRecordedWithObjectsEvenWithEveryGlobalSwitchOff() throws Exception {
        String project = user + "_prj";
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','敏感页回归项目','active',now(),now())", project);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,NULL,'active',now())",
                user, user, project);
        jdbc.update("""
                INSERT INTO auth_credentials (user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                                              password_changed_at, status, created_at, updated_at)
                VALUES (?, ?, 'bcrypt', 0, NULL, now(), 'active', now(), now())
                """, user, new BCryptPasswordEncoder(10).encode("b28-sensitive-password"));
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("login_name", user, "password", "b28-sensitive-password"))))
                .andExpect(status().isOk())
                .andReturn();
        Cookie csrf = login.getResponse().getCookie("XSRF-TOKEN");
        Cookie device = login.getResponse().getCookie("complex_device_no");

        // 终端总开关、各模块开关与短信域开关（V554 等，均为全局行）全部关掉。
        assertThat(jdbc.update("UPDATE terminal_capability_switches SET enabled = FALSE WHERE scope_type = 'global'")).isPositive();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM terminal_capability_switches WHERE scope_type='global' AND capability_code LIKE 'sms.%' AND enabled",
                Integer.class)).isZero();

        List<Map<String, String>> refs = List.of(Map.of("object_type", "customer_blacklist_request", "object_id", "blr-1"),
                Map.of("object_type", "customer", "object_id", "cus-7"));
        String body = mockMvc.perform(post("/api/v1/security/page-views")
                        .cookie(login.getResponse().getCookie("admin_access_token"), csrf, device)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .header("X-Terminal-Type", "web")
                        .header("X-Module-Code", "governance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("page_id", "ADM-CUS-001", "sensitive", true, "object_refs", refs,
                                "idempotency_key", "b28sp-" + UUID.randomUUID()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String code = objectMapper.readTree(body).at("/data/code").asText();

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT account_id, device_no, page_id, sensitive, object_refs::text AS refs FROM watermark_marks WHERE code=?", code);
        assertThat(row.get("account_id")).isEqualTo(user);
        assertThat(row.get("device_no")).isEqualTo(device.getValue());
        assertThat(row.get("page_id")).isEqualTo("ADM-CUS-001");
        assertThat(row.get("sensitive")).isEqualTo(true);
        assertThat(objectMapper.readTree((String) row.get("refs"))).isEqualTo(objectMapper.valueToTree(refs));
        Map<String, Object> audit = jdbc.queryForMap(
                "SELECT actor_id, metadata->>'page_id' AS page_id, (metadata->'object_refs')::text AS refs FROM audit_events"
                        + " WHERE event_name='security.sensitive_page.viewed' AND business_id=?", code);
        assertThat(audit.get("actor_id")).isEqualTo(user);
        assertThat(audit.get("page_id")).isEqualTo("ADM-CUS-001");
        assertThat(objectMapper.readTree((String) audit.get("refs"))).isEqualTo(objectMapper.valueToTree(refs));
    }

    private static KeyPair generateMasterKeyPair() {
        try {
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
