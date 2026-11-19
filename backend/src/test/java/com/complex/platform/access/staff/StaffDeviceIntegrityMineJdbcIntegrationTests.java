// 文件版本：V0.01
package com.complex.platform.access.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BB 段第 266 条 266-R1（PGM:586「检测到手机被破解、篡改或处于异常环境时，私人手机 App 失效并复核。」；
 * BSM:938「……检测到手机被破解、篡改或处于异常环境时自动失效并进入复核。」）：私人手机 App 拿不到本人凭证编号，
 * 按登录人上报本机完整性 POST /api/v1/staff/device-credentials/report-integrity。走真实登录链、授权链与私人手机验签（enforce），
 * 类注解与签名夹具照抄 StaffPrivatePhoneEnforcementJdbcIntegrationTests。
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
class StaffDeviceIntegrityMineJdbcIntegrationTests {

    private static final String PHONE = "staff_private_phone";
    private static final String REPORT = "/api/v1/staff/device-credentials/report-integrity";
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
    private final String user = "usr_b74pi_" + suffix;
    private final String password = "b74-private-phone-" + suffix;
    private final String profile = "sp_b74pi_" + suffix;
    private final String credential = "scred_b74pi_" + suffix;

    /** 本人私人手机报 root_detected → 本人凭证吊销、发通知、留痕记本人；之后同一台手机的签名写请求按凭证失效拒绝（App 失效）。 */
    @Test
    void rootedPhoneReportRevokesOwnCredentialAndThePhoneStopsWorking() throws Exception {
        Phone phone = enrolledPhone();

        signed(phone, REPORT, json(Map.of("idempotency_key", "b74pi-root-" + suffix, "integrity_status", "root_detected")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(credential))
                .andExpect(jsonPath("$.data.bind_status").value("revoked"))
                .andExpect(jsonPath("$.data.integrity_status").value("root_detected"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM employee_notices WHERE staff_profile_id = ?", Integer.class, profile))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'staff_device.integrity_failed'"
                + " AND business_id = ? AND actor_id = ?", Integer.class, credential, user)).isEqualTo(1);

        signed(phone, LEAVES, json(Map.of("idempotency_key", "b74pi-leave-" + suffix, "leave_type", "personal",
                "start_date", "2099-03-10", "end_date", "2099-03-11", "reason", "家中有事")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("DEVICE_CREDENTIAL_INVALID"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_leave_requests WHERE user_id = ?", Integer.class, user)).isZero();
    }

    /** 报 trusted 只刷新完整性标记，凭证仍生效。 */
    @Test
    void trustedReportKeepsTheCredentialActive() throws Exception {
        Phone phone = enrolledPhone();

        signed(phone, REPORT, json(Map.of("idempotency_key", "b74pi-trusted-" + suffix, "integrity_status", "trusted")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bind_status").value("active"))
                .andExpect(jsonPath("$.data.integrity_status").value("trusted"));
    }

    /** 没有生效中凭证的员工（网页会话；私人手机签名请求此时先被终端闸拒，到不了服务层）→ 404 CREDENTIAL_NOT_FOUND，并落失败留痕。 */
    @Test
    void employeeWithoutActiveCredentialGetsNotFoundAndTheFailureIsRecorded() throws Exception {
        employee();
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("login_name", user, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        Cookie csrf = login.getResponse().getCookie("XSRF-TOKEN");
        String key = "b74pi-none-" + suffix;

        mockMvc.perform(post(REPORT)
                        .cookie(login.getResponse().getCookie("admin_access_token"), csrf,
                                login.getResponse().getCookie("complex_device_no"))
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .header("X-Terminal-Type", "web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idempotency_key", key, "integrity_status", "abnormal"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("CREDENTIAL_NOT_FOUND"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'staff_device.integrity_report_failed'"
                + " AND idempotency_key = ? AND actor_id = ?", Integer.class, key, user)).isEqualTo(1);
    }

    private record Phone(KeyPair keys, String fingerprint, String token) {
    }

    private void employee() {
        new StaffRequestFixture(jdbc).staff(user, "staff", null, null, "staff.leave.submit");
        jdbc.update("""
                INSERT INTO auth_credentials (user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                                              password_changed_at, status, created_at, updated_at)
                VALUES (?, ?, 'bcrypt', 0, NULL, now(), 'active', now(), now())
                """, user, new BCryptPasswordEncoder(10).encode(password));
        jdbc.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, company_id, user_id, name, registration_status, status,
                                            created_at, updated_at)
                VALUES (?, 'seed-org', NULL, 'seed-org', ?, '私人手机完整性', 'approved', 'active', now(), now())
                """, profile, user);
    }

    /** 员工、生效中的私人手机凭证（EC P-256，指纹＝公钥 DER 的 SHA-256），按安卓私人手机实际发法登录（带终端头、不签名）。 */
    private Phone enrolledPhone() throws Exception {
        employee();
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        KeyPair keys = generator.generateKeyPair();
        byte[] der = keys.getPublic().getEncoded();
        String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(der));
        jdbc.update("""
                INSERT INTO staff_device_credentials (id, staff_profile_id, device_public_key, device_fingerprint_hash, bind_status,
                                                      integrity_status, bound_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'active', 'trusted', now(), now(), now())
                """, credential, profile, Base64.getEncoder().encodeToString(der), fingerprint);
        String token = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Terminal-Type", PHONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("login_name", user, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).at("/data/access_token").asText();
        return new Phone(keys, fingerprint, token);
    }

    private ResultActions signed(Phone phone, String path, String json) throws Exception {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        long ts = System.currentTimeMillis();
        String nonce = "n-" + UUID.randomUUID();
        String canonical = phone.fingerprint() + "\n" + ts + "\n" + nonce + "\nPOST\n" + path + "\n"
                + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(phone.keys().getPrivate());
        signer.update(canonical.getBytes(StandardCharsets.UTF_8));
        return mockMvc.perform(post(path)
                .header("Authorization", "Bearer " + phone.token())
                .header("X-Terminal-Type", PHONE)
                .header("X-Device-Id", phone.fingerprint())
                .header("X-Device-Timestamp", Long.toString(ts))
                .header("X-Device-Nonce", nonce)
                .header("X-Device-Signature", Base64.getEncoder().encodeToString(signer.sign()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
