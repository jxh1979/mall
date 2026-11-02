// 文件版本：V0.02
package com.complex.platform.gate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.Base64;
import java.util.HexFormat;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第 100 条 100-R3（PGM:744「前端隐藏只是辅助，后端接口必须独立校验」；PGM:747「同一个员工 App，装在公司设备和员工自有手机上能用的功能不一样……」）：
 * 私人手机有自己的验签开关键 {@code access.device_credential.enforcement.staff_private_phone}，单独翻 enforce 生效。
 * 走真实生产登录链与授权链：员工登录（{login_name, password}）取访问令牌，按安卓私人手机提交请假的请求形状
 * （Authorization: Bearer、X-Terminal-Type: staff_private_phone、X-Device-Id＝凭证指纹、蛇形键请求体）调 POST /api/v1/staff/leaves。
 * 登录按安卓私人手机的实际发法带私人手机终端头、不签名（第十八批 M 段第 115 条：设备绑定型的登录在 enforce 下无设备证明也放行）。
 */
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "complex.database.credential-separation.enforced=false",
        "refund.tiered_authorization.enabled=true",
        "complex.auth.production.seed-admin.username=p0_private_phone_admin",
        "complex.auth.production.seed-admin.password=p0_private_phone_password",
        "ticket.credential.signing.secret=prod-test-ticket-signing-secret-32-bytes-minimum",
        "bar.invitation.signing.secret=prod-test-bar-invitation-signing-secret-32-bytes-minimum",
        "compliance.wecom.webhook.signing.secret=prod-test-wecom-webhook-signing-secret-32-bytes-minimum",
        "keyvault.keys.pii=prod-test-pii-keyvault-secret-32-bytes-minimum",
        "keyvault.keys.pii-index=prod-test-pii-index-blind-secret-32-bytes-minimum",
        "access.device_credential.enforcement.staff_private_phone=enforce"
})
@Transactional
class StaffPrivatePhoneEnforcementJdbcIntegrationTests {

    private static final String PHONE = "staff_private_phone";
    private static final String LEAVES = "/api/v1/staff/leaves";

    @DynamicPropertySource
    static void masterPublicKey(DynamicPropertyRegistry registry) throws Exception {
        String key = Base64.getEncoder().encodeToString(KeyPairGenerator.getInstance("Ed25519").generateKeyPair().getPublic().getEncoded());
        registry.add("complex.auth.master.public-key", () -> key);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String user = "usr_b32pp_" + suffix;
    private final String password = "b32-private-phone-" + suffix;

    /** 私人手机写请求不签名 → 403 DEVICE_CREDENTIAL_INVALID、不落请假；用该手机凭证私钥签名 → 200、落一条请假。 */
    @Test
    void privatePhoneWriteNeedsSignatureWhenItsKeyIsEnforced() throws Exception {
        new StaffRequestFixture(jdbc).staff(user, "staff", null, null, "staff.leave.submit");
        jdbc.update("""
                INSERT INTO auth_credentials (user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                                              password_changed_at, status, created_at, updated_at)
                VALUES (?, ?, 'bcrypt', 0, NULL, now(), 'active', now(), now())
                """, user, new BCryptPasswordEncoder(10).encode(password));
        String profile = "sp_b32pp_" + suffix;
        jdbc.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, registration_status, status, created_at, updated_at)
                VALUES (?, 'seed-org', NULL, ?, '私人手机请假', 'approved', 'active', now(), now())
                """, profile, user);
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        KeyPair keys = generator.generateKeyPair();
        byte[] der = keys.getPublic().getEncoded();
        String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(der));
        jdbc.update("""
                INSERT INTO staff_device_credentials (id, staff_profile_id, device_public_key, device_fingerprint_hash, bind_status,
                                                      integrity_status, bound_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'active', 'trusted', now(), now(), now())
                """, "scred_b32pp_" + suffix, profile, Base64.getEncoder().encodeToString(der), fingerprint);
        String token = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Terminal-Type", PHONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("login_name", user, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).at("/data/access_token").asText();

        byte[] unsigned = leaveBody("b32pp-leave-a-" + suffix);
        mockMvc.perform(phone(token, fingerprint, unsigned))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("DEVICE_CREDENTIAL_INVALID"));
        assertThat(leaves()).isZero();

        byte[] body = leaveBody("b32pp-leave-b-" + suffix);
        long ts = System.currentTimeMillis();
        String nonce = "n-" + suffix;
        String canonical = fingerprint + "\n" + ts + "\n" + nonce + "\nPOST\n" + LEAVES + "\n"
                + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(keys.getPrivate());
        signer.update(canonical.getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(phone(token, fingerprint, body)
                        .header("X-Device-Timestamp", Long.toString(ts))
                        .header("X-Device-Nonce", nonce)
                        .header("X-Device-Signature", Base64.getEncoder().encodeToString(signer.sign())))
                .andExpect(status().isOk());
        assertThat(leaves()).isEqualTo(1);
    }

    private MockHttpServletRequestBuilder phone(String token, String fingerprint, byte[] body) {
        return post(LEAVES)
                .header("Authorization", "Bearer " + token)
                .header("X-Terminal-Type", PHONE)
                .header("X-Device-Id", fingerprint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    /** 安卓 LeaveSubmitRequest 的蛇形键。 */
    private byte[] leaveBody(String idempotencyKey) throws Exception {
        return json(Map.of("idempotency_key", idempotencyKey, "leave_type", "personal",
                "start_date", "2099-03-10", "end_date", "2099-03-11", "reason", "家中有事")).getBytes(StandardCharsets.UTF_8);
    }

    private int leaves() {
        return jdbc.queryForObject("SELECT count(*) FROM staff_leave_requests WHERE user_id = ?", Integer.class, user);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
