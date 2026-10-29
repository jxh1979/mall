// 文件版本：V0.01
package com.complex.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.List;
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
 * 第 23 条 23-R1 浏览器设备编号。第十八批裁决 6：「浏览器第一次登录时系统发一个设备编号存进这个浏览器并在后台登记，
 * 导出密码和页面暗纹用这个编号（清浏览器数据或换浏览器算新设备），不用装软件」。
 * 走真实生产登录链（prod 档 ProductionAuthController → ProductionAuthService → auth_credentials），按前端蛇形键请求。
 */
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "complex.database.credential-separation.enforced=false",
        "refund.tiered_authorization.enabled=true",
        "complex.auth.production.seed-admin.username=p0_browser_device_admin",
        "complex.auth.production.seed-admin.password=p0_browser_device_password",
        "ticket.credential.signing.secret=prod-test-ticket-signing-secret-32-bytes-minimum",
        "bar.invitation.signing.secret=prod-test-bar-invitation-signing-secret-32-bytes-minimum",
        "compliance.wecom.webhook.signing.secret=prod-test-wecom-webhook-signing-secret-32-bytes-minimum",
        "keyvault.keys.pii=prod-test-pii-keyvault-secret-32-bytes-minimum",
        "keyvault.keys.pii-index=prod-test-pii-index-blind-secret-32-bytes-minimum"
})
@Transactional
class BrowserDeviceLoginJdbcIntegrationTests {

    private static final String ADMIN = "p0_browser_device_admin";
    private static final String ADMIN_PASSWORD = "p0_browser_device_password";
    private static final String DEVICE_COOKIE = "complex_device_no";
    // 生产档启动须有破窗验签公钥（同 ProductionAuthJdbcIntegrationTests）；私钥只在测试进程内模拟 U 盘。
    private static final KeyPair MASTER_KEY_PAIR = generateMasterKeyPair();

    @DynamicPropertySource
    static void masterPublicKey(DynamicPropertyRegistry registry) {
        registry.add("complex.auth.master.public-key",
                () -> Base64.getEncoder().encodeToString(MASTER_KEY_PAIR.getPublic().getEncoded()));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void firstWebLoginIssuesHttpOnlyDeviceNumberAndRegistersIt() throws Exception {
        MvcResult login = webLogin(ADMIN, ADMIN_PASSWORD);

        String header = deviceSetCookie(login);
        assertThat(header).as("首次网页登录须下发 complex_device_no").isNotNull();
        assertThat(header).contains("HttpOnly").contains("SameSite=Strict").contains("Path=/").contains("Max-Age=34560000");
        String deviceNo = login.getResponse().getCookie(DEVICE_COOKIE).getValue();
        assertThat(deviceNo).matches("WB[A-HJKMNP-Z2-9]{12}");
        assertThat(firstUser(deviceNo)).isEqualTo(userId(login));
    }

    @Test
    void registeredDeviceNumberIsReusedAcrossLoginsAndAccounts() throws Exception {
        MvcResult first = webLogin(ADMIN, ADMIN_PASSWORD);
        String deviceNo = issuedDeviceNo(first);
        insertStaffAccount("usr_b28_device_other", "b28_device_other", "b28-device-other-password");
        int before = registered();

        MvcResult again = webLogin("b28_device_other", "b28-device-other-password", new Cookie(DEVICE_COOKIE, deviceNo));

        assertThat(issuedDeviceNo(again)).as("已登记的编号沿用、每次登录续期").isEqualTo(deviceNo);
        assertThat(registered()).isEqualTo(before);
        assertThat(firstUser(deviceNo)).isEqualTo(userId(first));
    }

    @Test
    void unregisteredDeviceNumberIsReplacedWithNewOne() throws Exception {
        MvcResult login = webLogin(ADMIN, ADMIN_PASSWORD, new Cookie(DEVICE_COOKIE, "WBFORGED23456"));

        String deviceNo = issuedDeviceNo(login);
        assertThat(deviceNo).isNotEqualTo("WBFORGED23456");
        assertThat(firstUser(deviceNo)).isEqualTo(userId(login));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM browser_devices WHERE device_no = 'WBFORGED23456'", Integer.class)).isZero();
    }

    /**
     * 复核：员工手机、平板、一体机登录按 Android 真实形状——请求头 X-Terminal-Type 报设备绑定型终端、请求体只有 login_name 与 password、
     * 不带浏览器 Cookie（TerminalApiClient.kt LoginRequest）——不发浏览器设备编号、不在 browser_devices 登记（核对稿 23-R1 ③「Android 端……
     * 不发浏览器编号」；这些设备的暗纹与导出用验签后的登记设备号）。每天登录多次也不增行。
     */
    @Test
    void staffPhoneLoginGetsNoBrowserDeviceNumberAndRegistersNothing() throws Exception {
        int before = registered();
        for (String terminal : List.of("staff_work_phone", "staff_work_phone", "staff_private_phone", "tablet", "kiosk")) {
            MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Terminal-Type", terminal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(java.util.Map.of("login_name", ADMIN, "password", ADMIN_PASSWORD))))
                    .andExpect(status().isOk())
                    .andReturn();
            assertThat(deviceSetCookie(login)).as(terminal).isNull();
        }
        assertThat(registered()).isEqualTo(before);
    }

    @Test
    void cookieOnlyWebRefreshStillSucceeds() throws Exception {
        MvcResult login = webLogin(ADMIN, ADMIN_PASSWORD);
        Cookie device = new Cookie(DEVICE_COOKIE, issuedDeviceNo(login));
        Cookie access = login.getResponse().getCookie("admin_access_token");
        Cookie refresh = login.getResponse().getCookie("admin_refresh_token");
        Cookie csrf = login.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(access, refresh, csrf, device)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void logoutKeepsDeviceNumberAndNextLoginGetsTheSameOne() throws Exception {
        MvcResult login = webLogin(ADMIN, ADMIN_PASSWORD);
        String deviceNo = issuedDeviceNo(login);
        Cookie device = new Cookie(DEVICE_COOKIE, deviceNo);
        Cookie access = login.getResponse().getCookie("admin_access_token");
        Cookie csrf = login.getResponse().getCookie("XSRF-TOKEN");

        MvcResult logout = mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(access, csrf, device)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"b28-device-logout\",\"session_id\":\""
                                + body(login).at("/data/session_id").asText() + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(logout.getResponse().getHeaders("Set-Cookie")).noneMatch(h -> h.startsWith(DEVICE_COOKIE + "="));
        assertThat(issuedDeviceNo(webLogin(ADMIN, ADMIN_PASSWORD, device))).isEqualTo(deviceNo);
    }

    @Test
    void breakGlassWebLoginAlsoGetsBrowserDeviceNumber() throws Exception {
        MvcResult challenge = mockMvc.perform(post("/api/v1/auth/master/challenge"))
                .andExpect(status().isOk())
                .andReturn();
        String text = body(challenge).at("/data/challenge").asText();
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(MASTER_KEY_PAIR.getPrivate());
        signer.update(text.getBytes(StandardCharsets.UTF_8));
        MvcResult login = mockMvc.perform(post("/api/v1/auth/master/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "challenge", text, "signature", Base64.getEncoder().encodeToString(signer.sign())))))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(firstUser(issuedDeviceNo(login))).isEqualTo("usr_master_authority");
    }

    private static KeyPair generateMasterKeyPair() {
        try {
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private MvcResult webLogin(String loginName, String password, Cookie... cookies) throws Exception {
        var request = post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("login_name", loginName, "password", password)));
        if (cookies.length > 0) {
            request.cookie(cookies);
        }
        return mockMvc.perform(request).andExpect(status().isOk()).andReturn();
    }

    private String deviceSetCookie(MvcResult result) {
        return result.getResponse().getHeaders("Set-Cookie").stream()
                .filter(h -> h.startsWith(DEVICE_COOKIE + "="))
                .findFirst()
                .orElse(null);
    }

    private String issuedDeviceNo(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie(DEVICE_COOKIE);
        assertThat(cookie).as("网页登录须下发 complex_device_no").isNotNull();
        return cookie.getValue();
    }

    private String firstUser(String deviceNo) {
        return jdbcTemplate.queryForObject(
                "SELECT first_user_id FROM browser_devices WHERE device_no = ?", String.class, deviceNo);
    }

    private int registered() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM browser_devices", Integer.class);
    }

    private String userId(MvcResult result) throws Exception {
        return body(result).at("/data/user/id").asText();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void insertStaffAccount(String id, String username, String password) {
        jdbcTemplate.update("""
                INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)
                VALUES (?, ?, 'staff', 'org-x', NULL, 'active', now())
                """, id, username);
        jdbcTemplate.update("""
                INSERT INTO auth_credentials (user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                                              password_changed_at, status, created_at, updated_at)
                VALUES (?, ?, 'bcrypt', 0, NULL, now(), 'active', now(), now())
                """, id, new BCryptPasswordEncoder(10).encode(password));
    }
}
