// 文件版本：V0.01
package com.complex.platform.gate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 M 段第 116 条（PGM:747「同一个员工 App，装在公司设备和员工自有手机上能用的功能不一样：能做什么由后台按设备类型统一下发，
 * 员工自有手机只开放低风险功能（公告、我的工单、帮客人订位、查看考勤、请假、查自己薪资等）。」；PGM:744「……前端隐藏只是辅助，后端接口必须独立校验。」；
 * PGM:738「访问被拒绝也要留痕，便于排查是权限问题还是开关问题。」）。
 * 走真实生产登录链与授权链（默认 grace、不签名）：员工登录（{login_name, password}）取访问令牌；按安卓「员工设备凭证」绑定的发法
 * （X-Terminal-Type: staff_private_phone、不带 X-Device-Id、蛇形键）落凭证；之后按私人手机的发法（Bearer、私人手机终端头、X-Device-Id＝指纹）请求。
 */
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "complex.database.credential-separation.enforced=false",
        "refund.tiered_authorization.enabled=true",
        "complex.auth.production.seed-admin.username=p0_private_functions_admin",
        "complex.auth.production.seed-admin.password=p0_private_functions_password",
        "ticket.credential.signing.secret=prod-test-ticket-signing-secret-32-bytes-minimum",
        "bar.invitation.signing.secret=prod-test-bar-invitation-signing-secret-32-bytes-minimum",
        "compliance.wecom.webhook.signing.secret=prod-test-wecom-webhook-signing-secret-32-bytes-minimum",
        "keyvault.keys.pii=prod-test-pii-keyvault-secret-32-bytes-minimum",
        "keyvault.keys.pii-index=prod-test-pii-index-blind-secret-32-bytes-minimum"
})
@Transactional
class StaffPrivatePhoneFunctionsJdbcIntegrationTests {

    private static final String PHONE = "staff_private_phone";
    private static final List<String> PRIVATE_FUNCTIONS = List.of("staff_device", "inbox", "notices", "my_work_orders",
            "bar_reservation", "my_shifts", "my_payroll", "attendance_month", "leave", "my_service_income", "shift_manage", "my_profile");

    @DynamicPropertySource
    static void masterPublicKey(DynamicPropertyRegistry registry) throws Exception {
        String key = Base64.getEncoder().encodeToString(KeyPairGenerator.getInstance("Ed25519").generateKeyPair().getPublic().getEncoded());
        registry.add("complex.auth.master.public-key", () -> key);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String password = "b33-private-functions-" + suffix;
    private final String project = "prj_b33pf_" + suffix;
    /** 商户员工（带商户归属、有员工档案）。 */
    private final String user = "usr_b33pf_" + suffix;

    /** (a)～(f)：商户员工经真实绑定接口落凭证后，私人手机按清单放行或拒绝；不带终端头（按商户网页门户判）不受清单约束。 */
    @Test
    void privatePhoneOnlyReachesLowRiskFunctions() throws Exception {
        String profile = "sp_b33pf_" + suffix;
        account(user, "mch_b33pf_" + suffix, "staff.device.bind", "staff.leave.submit", "bar.order.view", "service.order.perform");
        profile(profile, user, "mch_b33pf_" + suffix, "私人手机功能清单");
        String token = login(user);
        String fingerprint = bind(token, profile);
        assertThat(jdbc.queryForObject("SELECT bind_status FROM staff_device_credentials WHERE device_fingerprint_hash = ?",
                String.class, fingerprint)).isEqualTo("active");

        // (a) 功能清单按可信终端下发。
        JsonNode phone = data(mockMvc.perform(phone(get("/api/v1/terminals/staff-app-functions"), token, fingerprint))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(phone.get("terminal_type").asText()).isEqualTo(PHONE);
        assertThat(strings(phone.get("functions"))).isEqualTo(PRIVATE_FUNCTIONS)
                .doesNotContain("ticket_verify", "attendance_punch", "my_alarms", "service_order_fulfil", "bar_order",
                        "merchant_staff_registration");
        JsonNode portal = data(mockMvc.perform(get("/api/v1/terminals/staff-app-functions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(strings(portal.get("functions"))).isEqualTo(StaffAppFunctions.ALL);

        // (b) 请假照常。
        mockMvc.perform(phone(post("/api/v1/staff/leaves"), token, fingerprint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idempotency_key", "b33pf-leave-" + suffix, "leave_type", "personal",
                                "start_date", "2099-03-10", "end_date", "2099-03-11", "reason", "家中有事"))))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_leave_requests WHERE user_id = ?", Integer.class, user)).isEqualTo(1);

        // (c) 清单外的读：拒并留痕。
        int before = functionRejections();
        mockMvc.perform(phone(get("/api/v1/bar/orders"), token, fingerprint))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_NOT_ALLOWED"));
        assertThat(functionRejections()).isEqualTo(before + 1);

        // (d) 接单是现场操作：拒，服务单不变。
        String order = "so_b33pf_" + suffix;
        jdbc.update("""
                INSERT INTO service_orders (id, catalog_item_id, item_name, price_cent, performer_user_id, status, created_at, updated_at, project_id)
                VALUES (?, 'fixture_catalog', '私人手机接单回归', 1000, ?, 'pending_accept', now(), now(), ?)
                """, order, user, project);
        mockMvc.perform(phone(post("/api/v1/services/orders/" + order + "/accept"), token, fingerprint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idempotency_key", "b33pf-accept-" + suffix))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_NOT_ALLOWED"));
        assertThat(jdbc.queryForObject("SELECT status FROM service_orders WHERE id = ?", String.class, order)).isEqualTo("pending_accept");

        // (e) 我的接警不开放给私人手机。
        mockMvc.perform(phone(get("/api/v1/facility/alarms/mine"), token, fingerprint))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_NOT_ALLOWED"));

        // (f) 同一账号不带终端头按商户网页门户判，私人手机清单不管它。
        mockMvc.perform(get("/api/v1/bar/orders").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    /** (g) 公司员工（无商户归属）在还没声明身份的手机上按私人手机声明绑定凭证：200，落一条生效凭证。 */
    @Test
    void companyEmployeeBindsCredentialDeclaredAsPrivatePhone() throws Exception {
        String employee = "usr_b33pf_emp_" + suffix;
        String profile = "sp_b33pf_emp_" + suffix;
        account(employee, null, "staff.device.bind");
        profile(profile, employee, null, "私人手机绑定");
        String fingerprint = bind(login(employee), profile);
        assertThat(jdbc.queryForObject("SELECT bind_status FROM staff_device_credentials WHERE staff_profile_id = ? AND device_fingerprint_hash = ?",
                String.class, profile, fingerprint)).isEqualTo("active");
    }

    /**
     * 第 111 条 111-R1（BSM:1466「我的工单：只看派给自己的工单和处理进度。」；PGM:747 私人手机开放「我的工单」）：公司员工在私人手机
     * 「我的工单」拉派给本人的商户服务工单，按真实登录、绑定与私人手机请求头放行。
     */
    @Test
    void privatePhoneListsMerchantWorkOrdersAssignedToMe() throws Exception {
        String employee = "usr_b33pf_mwo_" + suffix;
        String profile = "sp_b33pf_mwo_" + suffix;
        account(employee, null, "staff.device.bind", "merchant.service_work_order.process");
        profile(profile, employee, null, "私人手机商户工单");
        String merchant = "mch_b33pf_mwo_" + suffix;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                + "VALUES (?, ?, 'retail', 'active', now(), now(), ?)", merchant, "私人手机商户工单回归商户" + suffix, project);
        String order = "mswo_b33pf_" + suffix;
        jdbc.update("""
                INSERT INTO merchant_service_work_orders (id, merchant_id, work_order_type, priority, status, title, description,
                                                          reported_by_user_id, reported_channel, owner_department, owner_user_id,
                                                          created_at, updated_at)
                VALUES (?, ?, 'repair', 'medium', 'assigned', '门锁坏了', '店门门锁坏了', 'usr_b33pf_mch', 'merchant_portal', 'property', ?,
                        now(), now())
                """, order, merchant, employee);
        String token = login(employee);
        String fingerprint = bind(token, profile);
        mockMvc.perform(phone(get("/api/v1/merchant-service/work-orders/assigned-to-me"), token, fingerprint))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(order))
                .andExpect(jsonPath("$.data.items[0].work_order_type").value("repair"));
    }

    /** 安卓「员工设备凭证」绑定：按私人手机声明、不带设备号，蛇形键；返回本机指纹。 */
    private String bind(String token, String profile) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        PublicKey key = generator.generateKeyPair().getPublic();
        String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getEncoded()));
        mockMvc.perform(post("/api/v1/staff/device-credentials")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Terminal-Type", PHONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idempotency_key", "b33pf-bind-" + profile, "staff_profile_id", profile,
                                "device_public_key", Base64.getEncoder().encodeToString(key.getEncoded()),
                                "device_fingerprint_hash", fingerprint))))
                .andExpect(status().isOk());
        return fingerprint;
    }

    private MockHttpServletRequestBuilder phone(MockHttpServletRequestBuilder request, String token, String fingerprint) {
        return request.header("Authorization", "Bearer " + token).header("X-Terminal-Type", PHONE).header("X-Device-Id", fingerprint);
    }

    private String login(String loginName) throws Exception {
        return data(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("login_name", loginName, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("access_token").asText();
    }

    private void account(String userId, String merchantId, String... permissions) {
        if (jdbc.queryForObject("SELECT count(*) FROM projects WHERE id = ?", Integer.class, project) == 0) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', '私人手机功能清单回归综合体', 'active', now(), now())", project);
        }
        new StaffRequestFixture(jdbc).staff(userId, "staff", project, merchantId, permissions);
        jdbc.update("""
                INSERT INTO auth_credentials (user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                                              password_changed_at, status, created_at, updated_at)
                VALUES (?, ?, 'bcrypt', 0, NULL, now(), 'active', now(), now())
                """, userId, new BCryptPasswordEncoder(10).encode(password));
    }

    private void profile(String id, String userId, String merchantId, String name) {
        jdbc.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, company_id, registration_status, status,
                                            created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 'approved', 'active', now(), now())
                """, id, project, merchantId, userId, name, project);
    }

    private int functionRejections() {
        return jdbc.queryForObject("""
                SELECT count(*) FROM audit_events WHERE event_name = 'terminal.access_rejected'
                   AND metadata->>'terminal_type' = 'staff_private_phone' AND metadata->>'reason' = 'private_phone_function_not_allowed'
                """, Integer.class);
    }

    private JsonNode data(String body) throws Exception {
        return objectMapper.readTree(body).get("data");
    }

    private static List<String> strings(JsonNode array) {
        List<String> out = new ArrayList<>();
        array.forEach(node -> out.add(node.asText()));
        return out;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
