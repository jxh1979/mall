// 文件版本：V0.01
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
import java.util.LinkedHashMap;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 M 段第 108 条（108-R2、108-R3）与第 115 条（115-R1）：安卓员工端的四条启动请求走真实生产登录链与授权链。
 * 工作手机、平板、私人手机三类都翻 enforce；请求形状按安卓客户端（{login_name, password} 登录取令牌，Bearer、蛇形键请求体）。
 * <ul>
 *   <li>108-R2（PGM:719「开关打开后，也还要继续看员工权限、商户范围和设备状态。」；PGM:906「员工注册审批、设备绑定、换机解绑重绑。」）：
 *   无商户归属的设备管理员在未绑定的手机上绑定工作手机、平板并登记签名公钥，请求按所绑类型声明终端头；不带终端头照旧被总开关拒。</li>
 *   <li>108-R3（PGM:585「员工换手机必须先解绑旧手机，再绑定新手机。」）：公司自有员工在新私人手机上申请换机，凭证绑定请求声明私人手机。</li>
 *   <li>115-R1（BSM:940「同一账号允许在多台设备、多个端同时登录。」；PGM:744「前端隐藏只是辅助，后端接口必须独立校验。」）：
 *   设备绑定型的登录、设备绑定、登记签名公钥、私人手机凭证绑定在设备证明建立之前，enforce 下无证明也放行；之后的业务写请求照旧必须验签。</li>
 * </ul>
 */
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "complex.database.credential-separation.enforced=false",
        "refund.tiered_authorization.enabled=true",
        "complex.auth.production.seed-admin.username=p0_android_bootstrap_admin",
        "complex.auth.production.seed-admin.password=p0_android_bootstrap_password",
        "ticket.credential.signing.secret=prod-test-ticket-signing-secret-32-bytes-minimum",
        "bar.invitation.signing.secret=prod-test-bar-invitation-signing-secret-32-bytes-minimum",
        "compliance.wecom.webhook.signing.secret=prod-test-wecom-webhook-signing-secret-32-bytes-minimum",
        "keyvault.keys.pii=prod-test-pii-keyvault-secret-32-bytes-minimum",
        "keyvault.keys.pii-index=prod-test-pii-index-blind-secret-32-bytes-minimum",
        "access.device_credential.enforcement.staff_work_phone=enforce",
        "access.device_credential.enforcement.tablet=enforce",
        "access.device_credential.enforcement.staff_private_phone=enforce"
})
@Transactional
class AndroidDeviceBootstrapJdbcIntegrationTests {

    private static final String WORK_PHONE = "staff_work_phone";
    private static final String TABLET = "tablet";
    private static final String PRIVATE_PHONE = "staff_private_phone";
    private static final String BIND = "/api/v1/terminals/devices/bind";
    private static final String CREDENTIALS = "/api/v1/staff/device-credentials";
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
    private final String password = "b33-android-boot-" + suffix;
    private final String project = "prj_b33ab_" + suffix;
    private final String space = "spc_b33ab_" + suffix;
    /** 设备管理员：无商户归属的项目员工，有 terminal.device.bind。 */
    private final String admin = "usr_b33ab_adm_" + suffix;
    /** 工作手机持有人：有员工档案与请假权限。 */
    private final String holder = "usr_b33ab_hld_" + suffix;
    private final String holderProfile = "sp_b33ab_hld_" + suffix;
    private final String phone = "WP-B33AB-" + suffix;
    private final String tablet = "POS-B33AB-" + suffix;
    private KeyPair phoneKeys;

    @BeforeEach
    void seed() throws Exception {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                + "VALUES (?, 'complex', '安卓启动回归综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at) "
                + "VALUES (?, ?, '安卓启动回归吧台', 'shop', 'active', now(), now())", space, project);
        StaffRequestFixture fixture = new StaffRequestFixture(jdbc);
        fixture.staff(admin, "staff", project, null, "terminal.device.bind");
        fixture.staff(holder, "staff", project, null, "staff.leave.submit");
        password(admin);
        password(holder);
        profile(holderProfile, holder, "工作手机持有人");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        phoneKeys = generator.generateKeyPair();
    }

    /**
     * 工作手机：设备管理员在未绑定的手机上登录（不带终端头），按工作手机声明绑定并登记签名公钥；持有人按已绑定工作手机的发法登录（不签名），
     * 业务写请求不签名被拒、用登记公钥对应私钥签名才落请假。
     */
    @Test
    void workPhoneBootstrapRequestsPassAndBusinessWritesStillNeedSignature() throws Exception {
        String adminToken = login(admin, null);
        Map<String, Object> bind = new LinkedHashMap<>();
        bind.put("idempotency_key", "b33ab-bind-" + suffix);
        bind.put("device_id", phone);
        bind.put("terminal_type", WORK_PHONE);
        bind.put("binding_scope", "space");
        bind.put("bound_space_id", space);
        bind.put("staff_id", holderProfile);
        bind.put("device_model", "Pixel 8");
        // 对照：现状安卓的发法（未绑定不带终端头）被终端总开关兜底拒绝。
        mockMvc.perform(post(BIND).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json(bind)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_NOT_ALLOWED"));

        mockMvc.perform(post(BIND).header("Authorization", "Bearer " + adminToken).header("X-Terminal-Type", WORK_PHONE)
                        .contentType(MediaType.APPLICATION_JSON).content(json(bind)))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForMap("SELECT status, staff_id, terminal_type FROM terminal_devices WHERE device_id = ?", phone))
                .containsEntry("status", "bound").containsEntry("staff_id", holderProfile).containsEntry("terminal_type", WORK_PHONE);

        mockMvc.perform(post("/api/v1/terminals/devices/" + phone + "/signing-key")
                        .header("Authorization", "Bearer " + adminToken).header("X-Terminal-Type", WORK_PHONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("public_key", Base64.getEncoder().encodeToString(phoneKeys.getPublic().getEncoded()),
                                "idempotency_key", "b33ab-key-" + suffix))))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM terminal_device_signing_keys WHERE device_id = ? AND status = 'active'",
                Integer.class, phone)).isEqualTo(1);

        // 持有人在已绑定的工作手机上登录：带工作手机终端头、不签名。
        String holderToken = login(holder, WORK_PHONE);
        mockMvc.perform(workPhoneLeave(holderToken, leaveBody("b33ab-leave-a-" + suffix)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("DEVICE_CREDENTIAL_INVALID"));
        assertThat(leaves()).isZero();

        byte[] body = leaveBody("b33ab-leave-b-" + suffix);
        long ts = System.currentTimeMillis();
        String nonce = "n-" + suffix;
        String canonical = phone + "\n" + ts + "\n" + nonce + "\nPOST\n" + LEAVES + "\n"
                + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(phoneKeys.getPrivate());
        signer.update(canonical.getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(workPhoneLeave(holderToken, body)
                        .header("X-Device-Timestamp", Long.toString(ts))
                        .header("X-Device-Nonce", nonce)
                        .header("X-Device-Signature", Base64.getEncoder().encodeToString(signer.sign())))
                .andExpect(status().isOk());
        assertThat(leaves()).isEqualTo(1);
    }

    /** 平板：设备管理员按平板声明绑定酒吧点位；在平板上登录带平板终端头、不签名。 */
    @Test
    void tabletBindAndLoginPassWithDeclaredType() throws Exception {
        String adminToken = login(admin, null);
        Map<String, Object> bind = new LinkedHashMap<>();
        bind.put("idempotency_key", "b33ab-pos-" + suffix);
        bind.put("device_id", tablet);
        bind.put("terminal_type", TABLET);
        bind.put("binding_scope", "space");
        bind.put("bound_space_id", space);
        bind.put("device_model", "Galaxy Tab");
        mockMvc.perform(post(BIND).header("Authorization", "Bearer " + adminToken).header("X-Terminal-Type", TABLET)
                        .contentType(MediaType.APPLICATION_JSON).content(json(bind)))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT terminal_type FROM terminal_devices WHERE device_id = ?", String.class, tablet))
                .isEqualTo(TABLET);

        assertThat(login(admin, TABLET)).isNotBlank();
    }

    /** 私人手机换机：公司自有员工在新手机上（本机还没有私人手机记录、不带终端头）登录，按私人手机声明提交新凭证 → 待复核、指向旧凭证。 */
    @Test
    void privatePhoneRebindRequestPassesWithDeclaredType() throws Exception {
        String employee = "usr_b33ab_emp_" + suffix;
        String employeeProfile = "sp_b33ab_emp_" + suffix;
        String oldCredential = "scred_b33ab_old_" + suffix;
        new StaffRequestFixture(jdbc).staff(employee, "staff", project, null);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, 'role_staff_self')", employee);
        password(employee);
        profile(employeeProfile, employee, "换机员工");
        jdbc.update("""
                INSERT INTO staff_device_credentials (id, staff_profile_id, device_public_key, device_fingerprint_hash, bind_status,
                                                      integrity_status, bound_at, created_at, updated_at)
                VALUES (?, ?, 'OLD_PUBLIC_KEY', 'fp_old', 'active', 'trusted', now(), now(), now())
                """, oldCredential, employeeProfile);
        String token = login(employee, null);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("idempotency_key", "b33ab-cred-" + suffix);
        request.put("operator_note", "Android terminal device credential bind");
        request.put("staff_profile_id", employeeProfile);
        request.put("device_public_key", Base64.getEncoder().encodeToString(phoneKeys.getPublic().getEncoded()));
        request.put("device_fingerprint_hash", "fp_new");
        // 对照：现状安卓的发法（新手机不带终端头）被终端总开关兜底拒绝。
        mockMvc.perform(post(CREDENTIALS).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_NOT_ALLOWED"));

        mockMvc.perform(post(CREDENTIALS).header("Authorization", "Bearer " + token).header("X-Terminal-Type", PRIVATE_PHONE)
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForMap("SELECT bind_status, supersedes_credential_id FROM staff_device_credentials "
                        + "WHERE staff_profile_id = ? AND device_fingerprint_hash = 'fp_new'", employeeProfile))
                .containsEntry("bind_status", "pending_review").containsEntry("supersedes_credential_id", oldCredential);
    }

    private String login(String user, String terminalType) throws Exception {
        MockHttpServletRequestBuilder login = post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("login_name", user, "password", password)));
        if (terminalType != null) {
            login.header("X-Terminal-Type", terminalType);
        }
        return objectMapper.readTree(mockMvc.perform(login).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).at("/data/access_token").asText();
    }

    private void password(String user) {
        jdbc.update("""
                INSERT INTO auth_credentials (user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                                              password_changed_at, status, created_at, updated_at)
                VALUES (?, ?, 'bcrypt', 0, NULL, now(), 'active', now(), now())
                """, user, new BCryptPasswordEncoder(10).encode(password));
    }

    private void profile(String id, String user, String name) {
        jdbc.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, company_id, registration_status, status,
                                            created_at, updated_at)
                VALUES (?, ?, NULL, ?, ?, ?, 'approved', 'active', now(), now())
                """, id, project, user, name, project);
    }

    private MockHttpServletRequestBuilder workPhoneLeave(String token, byte[] body) {
        return post(LEAVES)
                .header("Authorization", "Bearer " + token)
                .header("X-Terminal-Type", WORK_PHONE)
                .header("X-Device-Id", phone)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    /** 安卓 LeaveSubmitRequest 的蛇形键。 */
    private byte[] leaveBody(String idempotencyKey) throws Exception {
        return json(Map.of("idempotency_key", idempotencyKey, "leave_type", "personal",
                "start_date", "2099-03-10", "end_date", "2099-03-11", "reason", "家中有事")).getBytes(StandardCharsets.UTF_8);
    }

    private int leaves() {
        return jdbc.queryForObject("SELECT count(*) FROM staff_leave_requests WHERE user_id = ?", Integer.class, holder);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
