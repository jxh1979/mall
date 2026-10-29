// 文件版本：V0.01
package com.complex.platform.gate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 第 84 条 84-R1 工作手机签名 multipart 上传证据（BSM 第 1201 行「扫码缺失的可事后人工补录，但一律须附证据（照片、录音、录像等）」；
 * BSM 第 1891 行工作手机「补录、巡检、库房和酒水交接扫码」）。起真 Tomcat、用 java.net.http 发真实 multipart 正文——MockMvc 的模拟
 * multipart 没有正文，复现不了 1MB 早拒和包装器读空输入流。按 Android 员工端请求形状：真实登录取令牌、Bearer、设备签名头。
 * JSON 签名体 1MB 上限那条用 MockMvc（真 Tomcat 会把过滤器的 413 转去错误页）。
 * 不加 @Transactional（服务端线程看不到测试事务里的夹具），夹具用唯一后缀。
 */
@ActiveProfiles("prod")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "complex.database.credential-separation.enforced=false",
        "refund.tiered_authorization.enabled=true",
        "complex.auth.production.seed-admin.username=p0_b29mp_admin",
        "complex.auth.production.seed-admin.password=p0_b29mp_password",
        "ticket.credential.signing.secret=prod-test-ticket-signing-secret-32-bytes-minimum",
        "bar.invitation.signing.secret=prod-test-bar-invitation-signing-secret-32-bytes-minimum",
        "compliance.wecom.webhook.signing.secret=prod-test-wecom-webhook-signing-secret-32-bytes-minimum",
        "keyvault.keys.pii=prod-test-pii-keyvault-secret-32-bytes-minimum",
        "keyvault.keys.pii-index=prod-test-pii-index-blind-secret-32-bytes-minimum"
})
class DeviceCredentialSignedMultipartJdbcIntegrationTests {

    @DynamicPropertySource
    static void masterPublicKey(DynamicPropertyRegistry registry) throws Exception {
        String key = Base64.getEncoder().encodeToString(KeyPairGenerator.getInstance("Ed25519").generateKeyPair().getPublic().getEncoded());
        registry.add("complex.auth.master.public-key", () -> key);
    }

    private static final String UPLOAD = "/api/v1/evidence/attachments/upload";
    private static final HttpClient HTTP = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;

    private final ObjectMapper json = new ObjectMapper();
    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String user = "b29mp_staff_" + suffix;
    private final String password = "b29mp_password_" + suffix;
    private final String device = "b29mp-phone-" + suffix;
    private KeyPair keys;
    private String token;

    @BeforeEach
    void seed() throws Exception {
        String project = "b29mp_prj_" + suffix;
        String profile = "b29mp_sp_" + suffix;
        String role = "b29mp_role_" + suffix;
        String merchant = "b29mp_mch_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','签名上传回归','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                + "VALUES (?, ?, 'bar', 'active', now(), now(), ?)", merchant, "签名上传商户 " + suffix, project);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                user, user, project, merchant);
        jdbc.update("""
                INSERT INTO auth_credentials (user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                                              password_changed_at, status, created_at, updated_at)
                VALUES (?, ?, 'bcrypt', 0, NULL, now(), 'active', now(), now())
                """, user, new BCryptPasswordEncoder(10).encode(password));
        jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,registration_status,status,created_at,updated_at) VALUES(?,?,?,?,'签名上传','approved','active',now(),now())",
                profile, project, merchant, user);
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'签名上传回归','project','active',now())", role, "签名上传回归角色-" + Long.parseLong(suffix, 16));
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'evidence.attachment.create')", role);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user, role);
        jdbc.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, staff_id, status, bound_at, created_at, updated_at)
                VALUES (?, ?, ?, 'staff_work_phone', 'space', ?, 'bound', now(), now(), now())
                """, "td_b29mp_" + suffix, device, "DC-b29mp-" + suffix, profile);
        keys = KeyPairGenerator.getInstance("EC").generateKeyPair();
        jdbc.update("INSERT INTO terminal_device_signing_keys(device_id, public_key, status) VALUES (?, ?, 'active')",
                device, Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()));
        token = login();
    }

    /** 2MB 照片签名上传：不再被 1MB 早拒，落一行证据附件。 */
    @Test
    void signedTwoMegabyteEvidenceUploadIsStored() throws Exception {
        byte[] photo = new byte[2 * 1024 * 1024];
        Arrays.fill(photo, (byte) 7);

        HttpResponse<String> response = upload("manual_backfill", "manual_backfill", photo);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        JsonNode data = json.readTree(response.body()).path("data");
        assertThat(data.path("file_size").asLong()).isEqualTo(2097152L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM evidence_attachments WHERE id = ? AND business_type = 'manual_backfill' AND file_size = 2097152",
                Integer.class, data.path("id").asText())).isEqualTo(1);
    }

    /** 300KB 的签名上传：包装器不再读空原输入流，控制器拿得到文件部件。 */
    @Test
    void signedSmallEvidenceUploadReachesTheController() throws Exception {
        byte[] photo = new byte[300 * 1024];
        Arrays.fill(photo, (byte) 3);

        HttpResponse<String> response = upload("manual_backfill", "manual_backfill", photo);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(json.readTree(response.body()).at("/data/file_size").asLong()).isEqualTo(300L * 1024);
    }

    /** 签的是 manual_backfill、发出去换成别的值：部件规范串不符，验签拒绝并留痕 bad_signature。 */
    @Test
    void changedPartAfterSigningIsRejectedAsBadSignature() throws Exception {
        byte[] photo = new byte[300 * 1024];

        HttpResponse<String> response = upload("manual_backfill", "inventory_quarantine", photo);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(403);
        assertThat(json.readTree(response.body()).path("detail_code").asText()).isEqualTo("DEVICE_CREDENTIAL_INVALID");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_events WHERE event_name = 'terminal.device_credential' AND business_id = ?
                   AND metadata->>'reason' = 'bad_signature'
                """, Integer.class, device)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM evidence_attachments WHERE business_type = 'inventory_quarantine' AND file_name = 'evidence_1.jpg' AND file_size = ?",
                Integer.class, (long) photo.length)).isZero();
    }

    /** JSON 签名体的 1MB 上限不变：过滤器仍按 Content-Length 早拒 413（MockMvc 不走 Tomcat 错误页转发，直接看到过滤器给的状态）。 */
    @Test
    void signedJsonBodyOverOneMegabyteIsStillRejected() throws Exception {
        String path = "/api/v1/security/page-views";
        byte[] bytes = json.writeValueAsString(Map.of("page_id", "x".repeat(1024 * 1024 + 16), "sensitive", false))
                .getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequestBuilder request = post(path).contentType(MediaType.APPLICATION_JSON).content(bytes);
        signedHeaders(path, sha256Hex(bytes)).forEach((name, value) -> request.header(name, value));

        mockMvc.perform(request).andExpect(status().isPayloadTooLarge());
    }

    private String login() throws Exception {
        HttpResponse<String> response = HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .header("X-Terminal-Type", "staff_work_phone")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("login_name", user, "password", password))))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return json.readTree(response.body()).at("/data/access_token").asText();
    }

    /** 签名用 signedType 算部件规范串，正文里实际发 sentType。规范串与 Android multipartBodyDigest 同式（独立在此重算）。 */
    private HttpResponse<String> upload(String signedType, String sentType, byte[] file) throws Exception {
        String boundary = "b29mp" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"business_type\"\r\n\r\n" + sentType + "\r\n")
                .getBytes(StandardCharsets.UTF_8));
        body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"evidence_1.jpg\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.writeBytes(file);
        body.writeBytes(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        String canonicalParts = "p:business_type=" + signedType + "\nf:file:evidence_1.jpg:" + sha256Hex(file);

        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + UPLOAD))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()));
        signedHeaders(UPLOAD, sha256Hex(canonicalParts.getBytes(StandardCharsets.UTF_8))).forEach(request::header);
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    /** 与 Android deviceRequestHeaders 同式：deviceId、毫秒时间戳、nonce、方法、路径、正文摘要逐行拼接后 ECDSA 签名。 */
    private Map<String, String> signedHeaders(String path, String bodyDigest) throws Exception {
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString();
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(keys.getPrivate());
        signer.update(String.join("\n", device, Long.toString(timestamp), nonce, "POST", path, bodyDigest).getBytes(StandardCharsets.UTF_8));
        return Map.of("Authorization", "Bearer " + token,
                "X-Terminal-Type", "staff_work_phone",
                "X-Device-Id", device,
                "X-Device-Timestamp", Long.toString(timestamp),
                "X-Device-Nonce", nonce,
                "X-Device-Signature", Base64.getEncoder().encodeToString(signer.sign()));
    }

    private static String sha256Hex(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }
}
