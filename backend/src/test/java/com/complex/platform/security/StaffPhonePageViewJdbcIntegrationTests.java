// 文件版本：V0.01
package com.complex.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第 23 条 23-R6 员工手机端在线屏暗纹与敏感屏打开留痕。PGM:687「后台、商户专区和员工手机端的所有在线页面都带有按同一规则生成的暗纹……
 * 打开敏感页这件事本身也要留痕，记下是谁打开的、什么时间打开的、打开的是哪一个页面、这一次看到的是哪些对象」。
 * 按 Android 员工端的请求形状（真实登录拿访问令牌、Bearer、设备签名头、蛇形键、不带浏览器 Cookie）走真实入口；设备＝经设备凭证验签的登记设备号。
 */
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "complex.database.credential-separation.enforced=false",
        "refund.tiered_authorization.enabled=true",
        "complex.auth.production.seed-admin.username=p0_staffphone_admin",
        "complex.auth.production.seed-admin.password=p0_staffphone_password",
        "ticket.credential.signing.secret=prod-test-ticket-signing-secret-32-bytes-minimum",
        "bar.invitation.signing.secret=prod-test-bar-invitation-signing-secret-32-bytes-minimum",
        "compliance.wecom.webhook.signing.secret=prod-test-wecom-webhook-signing-secret-32-bytes-minimum",
        "keyvault.keys.pii=prod-test-pii-keyvault-secret-32-bytes-minimum",
        "keyvault.keys.pii-index=prod-test-pii-index-blind-secret-32-bytes-minimum"
})
@Transactional
class StaffPhonePageViewJdbcIntegrationTests {

    private static final String PATH = "/api/v1/security/page-views";
    private static final String PAGE = "酒吧空位 / 帮客订位";

    @DynamicPropertySource
    static void masterPublicKey(DynamicPropertyRegistry registry) throws Exception {
        String key = Base64.getEncoder().encodeToString(KeyPairGenerator.getInstance("Ed25519").generateKeyPair().getPublic().getEncoded());
        registry.add("complex.auth.master.public-key", () -> key);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String user = "b28sp_staff_" + suffix;
    private final String password = "b28sp_password_" + suffix;
    private final String device = "b28sp-phone-" + suffix;
    private KeyPair keys;

    @BeforeEach
    void seed() throws Exception {
        String project = "b28sp_prj_" + suffix;
        String profile = "b28sp_sp_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','员工手机暗纹回归','active',now(),now())", project);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,NULL,'active',now())",
                user, user, project);
        jdbc.update("""
                INSERT INTO auth_credentials (user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                                              password_changed_at, status, created_at, updated_at)
                VALUES (?, ?, 'bcrypt', 0, NULL, now(), 'active', now(), now())
                """, user, new BCryptPasswordEncoder(10).encode(password));
        jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,registration_status,status,created_at,updated_at) VALUES(?,?,NULL,?,'员工手机暗纹','approved','active',now(),now())",
                profile, project, user);
        jdbc.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, staff_id, status, bound_at, created_at, updated_at)
                VALUES (?, ?, ?, 'staff_work_phone', 'space', ?, 'bound', now(), now(), now())
                """, "td_b28sp_" + suffix, device, "DC-b28sp-" + suffix, profile);
        keys = KeyPairGenerator.getInstance("EC").generateKeyPair();
        jdbc.update("INSERT INTO terminal_device_signing_keys(device_id, public_key, status) VALUES (?, ?, 'active')",
                device, Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()));
    }

    @Test
    void signedWorkPhoneSensitiveScreenViewRecordsRegisteredDeviceAndObjects() throws Exception {
        String token = login();
        String body = body(true, List.of(Map.of("object_type", "bar_reservation", "object_id", "resv-" + suffix)));

        String code = objectMapper.readTree(signed(pageView(token, body), body).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).at("/data/code").asText();

        Map<String, Object> mark = jdbc.queryForMap(
                "SELECT account_id, device_no, page_id, sensitive, object_refs::text AS refs FROM watermark_marks WHERE code = ?", code);
        assertThat(mark.get("device_no")).isEqualTo(device);
        assertThat(mark.get("account_id")).isEqualTo(user);
        assertThat(mark.get("page_id")).isEqualTo(PAGE);
        assertThat(mark.get("sensitive")).isEqualTo(true);
        assertThat(objectMapper.readTree((String) mark.get("refs")))
                .isEqualTo(objectMapper.readTree("[{\"object_type\":\"bar_reservation\",\"object_id\":\"resv-" + suffix + "\"}]"));
        assertThat(jdbc.queryForObject("""
                SELECT metadata->>'device_no' FROM audit_events WHERE event_name = 'security.sensitive_page.viewed' AND business_id = ?
                """, String.class, code)).isEqualTo(device);
    }

    /** 未验签（grace 下放行的无签名写）不能当作这台设备：没有可归属的设备号，不发码。 */
    @Test
    void unsignedWorkPhoneViewHasNoDeviceAndIsRefused() throws Exception {
        String token = login();

        mockMvc.perform(pageView(token, body(false, List.of())).header("X-Device-Id", device))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DEVICE_NOT_REGISTERED"));
    }

    private String login() throws Exception {
        JsonNode login = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Terminal-Type", "staff_work_phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("login_name", user, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        return login.at("/data/access_token").asText();
    }

    private String body(boolean sensitive, List<Map<String, String>> refs) throws Exception {
        return objectMapper.writeValueAsString(Map.of("page_id", PAGE, "sensitive", sensitive, "object_refs", refs,
                "idempotency_key", "b28sp-" + UUID.randomUUID()));
    }

    private MockHttpServletRequestBuilder pageView(String token, String body) {
        return post(PATH)
                .header("Authorization", "Bearer " + token)
                .header("X-Terminal-Type", "staff_work_phone")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    /** 与 Android deviceRequestHeaders 同式：deviceId、毫秒时间戳、nonce、方法、路径、正文 SHA-256 逐行拼接后 ECDSA 签名。 */
    private ResultActions signed(MockHttpServletRequestBuilder request, String body) throws Exception {
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString();
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8)));
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(keys.getPrivate());
        signer.update(String.join("\n", device, Long.toString(timestamp), nonce, "POST", PATH, digest).getBytes(StandardCharsets.UTF_8));
        return mockMvc.perform(request
                .header("X-Device-Id", device)
                .header("X-Device-Timestamp", Long.toString(timestamp))
                .header("X-Device-Nonce", nonce)
                .header("X-Device-Signature", Base64.getEncoder().encodeToString(signer.sign())));
    }
}
