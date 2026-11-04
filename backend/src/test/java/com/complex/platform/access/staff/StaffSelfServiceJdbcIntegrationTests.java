// 文件版本：V0.01
package com.complex.platform.access.staff;

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
 * 第十八批 P 段第 136 条 136-R1（BSM:1458「每位审批通过的员工都自动获得一份“员工自助”权限：可查看本人（且仅本人）的工资条（含提成明细）、
 * 基础资料、待办通知与排班/考勤/请假，看不到其他同事的这些信息。」；PGM:747 员工自有手机开放「查自己薪资」等低风险功能）。
 * 契约补 getMyStaffProfile、listMyStaffPayrollCommissions 后的真实流程：审批通过员工（role_staff_self）真实登录、按私人手机声明绑定凭证，
 * 再按安卓的发法（Bearer、X-Terminal-Type: staff_private_phone、X-Device-Id＝指纹）读本人档案与本人提成明细。
 * 136-R3：本人档案只读，自改姓名写口已删。
 */
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "complex.database.credential-separation.enforced=false",
        "refund.tiered_authorization.enabled=true",
        "complex.auth.production.seed-admin.username=p0_staff_self_service_admin",
        "complex.auth.production.seed-admin.password=p0_staff_self_service_password",
        "ticket.credential.signing.secret=prod-test-ticket-signing-secret-32-bytes-minimum",
        "bar.invitation.signing.secret=prod-test-bar-invitation-signing-secret-32-bytes-minimum",
        "compliance.wecom.webhook.signing.secret=prod-test-wecom-webhook-signing-secret-32-bytes-minimum",
        "keyvault.keys.pii=prod-test-pii-keyvault-secret-32-bytes-minimum",
        "keyvault.keys.pii-index=prod-test-pii-index-blind-secret-32-bytes-minimum"
})
@Transactional
class StaffSelfServiceJdbcIntegrationTests {

    private static final String PHONE = "staff_private_phone";

    @DynamicPropertySource
    static void masterPublicKey(DynamicPropertyRegistry registry) throws Exception {
        String key = Base64.getEncoder().encodeToString(KeyPairGenerator.getInstance("Ed25519").generateKeyPair().getPublic().getEncoded());
        registry.add("complex.auth.master.public-key", () -> key);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String password = "b36-staff-self-" + suffix;
    private final String project = "prj_b36ss_" + suffix;
    private final String user = "usr_b36ss_" + suffix;
    private final String profile = "sp_b36ss_" + suffix;
    private final String colleague = "usr_b36ss_mate_" + suffix;

    /** 「我的资料」：只返本人档案（不是同项目同事的），手机号脱敏、证件号显示 ******，蛇形键。 */
    @Test
    void myProfileReturnsOwnMaskedProfileOnly() throws Exception {
        selfServiceStaff();
        profile("sp_b36ss_mate_" + suffix, colleague, "同事档案", "13900005678");
        String token = login(user);
        String fingerprint = bind(token);

        JsonNode data = data(mockMvc.perform(phone(get("/api/v1/staff/profiles/mine"), token, fingerprint))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(data.get("id").asText()).isEqualTo(profile);
        assertThat(data.get("user_id").asText()).isEqualTo(user);
        assertThat(data.get("name").asText()).isEqualTo("本人档案");
        assertThat(data.get("registration_status").asText()).isEqualTo("approved");
        assertThat(data.get("phone").asText()).isEqualTo("138****1234");
        assertThat(data.get("id_doc").asText()).isEqualTo("******");
    }

    /** 「我的薪资」提成明细：只含本人该月提成行（同事同月一行不出现）；月份不是 YYYY-MM 时 400 VALIDATION_FAILED。 */
    @Test
    void myCommissionsListOwnRowsOnly() throws Exception {
        selfServiceStaff();
        commission(user, "src_b36ss_mine_" + suffix, 1200);
        commission(colleague, "src_b36ss_mate_" + suffix, 3400);
        String token = login(user);
        String fingerprint = bind(token);

        JsonNode data = data(mockMvc.perform(phone(get("/api/v1/staff/payroll/mine/commissions").param("month", "2026-11"), token, fingerprint))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(data.get("items")).hasSize(1);
        JsonNode line = data.get("items").get(0);
        assertThat(line.get("source_type").asText()).isEqualTo("service_order");
        assertThat(line.get("source_id").asText()).isEqualTo("src_b36ss_mine_" + suffix);
        assertThat(line.get("amount_cent").asLong()).isEqualTo(1200);

        mockMvc.perform(phone(get("/api/v1/staff/payroll/mine/commissions").param("month", "2026-1"), token, fingerprint))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    /**
     * 136-R3：员工自助对基础资料只写「可查看」（BSM:1458），自助写入口只列注册、请假、申诉（BSM:1464），不再有自改姓名写口——
     * 工作手机身份（不受功能清单约束）按安卓原发法 POST 本人档案 405，库里姓名仍为原值。
     */
    @Test
    void myProfileHasNoSelfEditWriteEntry() throws Exception {
        selfServiceStaff();
        String token = login(user);

        mockMvc.perform(post("/api/v1/staff/profiles/mine")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Terminal-Type", "staff_work_phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idempotency_key", "b36ss-rename-" + suffix, "name", "新名字"))))
                .andExpect(status().isMethodNotAllowed());
        assertThat(jdbc.queryForObject("SELECT name FROM staff_profiles WHERE id = ?", String.class, profile)).isEqualTo("本人档案");
    }

    /** 审批通过员工：账号只挂「员工自助」角色 role_staff_self（StaffService 审批通过即分配），有登录口令与本人档案；另建同项目一名同事账号。 */
    private void selfServiceStaff() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                + "VALUES (?, 'complex', '员工自助回归综合体', 'active', now(), now())", project);
        StaffRequestFixture fixture = new StaffRequestFixture(jdbc);
        fixture.staff(user, "staff", project, null);
        fixture.staff(colleague, "staff", project, null);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, 'role_staff_self')", user);
        jdbc.update("""
                INSERT INTO auth_credentials (user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                                              password_changed_at, status, created_at, updated_at)
                VALUES (?, ?, 'bcrypt', 0, NULL, now(), 'active', now(), now())
                """, user, new BCryptPasswordEncoder(10).encode(password));
        profile(profile, user, "本人档案", "13812341234");
    }

    private void profile(String id, String userId, String name, String phone) {
        jdbc.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, phone, id_doc, company_id, registration_status, status,
                                            created_at, updated_at)
                VALUES (?, ?, NULL, ?, ?, ?, '110101199001011234', ?, 'approved', 'active', now(), now())
                """, id, project, userId, name, phone, project);
    }

    private void commission(String userId, String sourceId, long amountCent) {
        jdbc.update("INSERT INTO staff_commission_ledger (id, user_id, month, source_type, source_id, amount_cent, created_at, project_id) "
                + "VALUES (?, ?, '2026-11', 'service_order', ?, ?, now(), ?)", "scl_" + sourceId, userId, sourceId, amountCent, project);
    }

    /** 安卓「员工设备凭证」绑定：按私人手机声明、不带设备号，蛇形键；返回本机指纹。 */
    private String bind(String token) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        PublicKey key = generator.generateKeyPair().getPublic();
        String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getEncoded()));
        mockMvc.perform(post("/api/v1/staff/device-credentials")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Terminal-Type", PHONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idempotency_key", "b36ss-bind-" + suffix, "staff_profile_id", profile,
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

    private JsonNode data(String body) throws Exception {
        return objectMapper.readTree(body).get("data");
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
