// 文件版本：V0.02
package com.complex.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.contract.ContractRecord;
import com.complex.platform.contract.ContractRepository;
import com.complex.platform.contract.ContractSettlementRecord;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第 25 条商户手机应用（受控十一端之一，ENUM:233、PGM:745）。
 * 25-R1：登录只许商户账号（BSM:1884-1886「商户侧的手机端」）；会话决定终端为 merchant_app，改报别的端也照 merchant_app 判总开关
 * （PGM:719「开关关闭时，端或模块就不能用」、PGM:744「前端隐藏只是辅助，后端接口必须独立校验」）。
 * 25-R2：第十八批裁决 19「第一次登录时系统发一个设备编号，存进 App 的系统安全存储并在后台登记，暗纹和导出密码用它」。
 * 走真实生产登录链与授权链（prod 档 ProductionAuthController → ProductionAuthService → auth_credentials；令牌过滤器 → 设备凭证拦截器 → 终端总开关），
 * 按 App 请求形状：蛇形键、Authorization: Bearer、X-Terminal-Type: merchant_app、不带浏览器 Cookie。
 * 第 91 条 91-R1：商户账号的网页请求（浏览器 Cookie 会话＋X-XSRF-TOKEN，同后台网页登录 {login_name, password}）除设备绑定型外按 merchant_portal 判，
 * 改报 web 或不带终端头不再绕过商户网页门户总开关与商户专区模块开关（PGM:719、PGM:737、PGM:744）。
 */
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "complex.database.credential-separation.enforced=false",
        "refund.tiered_authorization.enabled=true",
        "complex.auth.production.seed-admin.username=p0_merchant_app_admin",
        "complex.auth.production.seed-admin.password=p0_merchant_app_password",
        "ticket.credential.signing.secret=prod-test-ticket-signing-secret-32-bytes-minimum",
        "bar.invitation.signing.secret=prod-test-bar-invitation-signing-secret-32-bytes-minimum",
        "compliance.wecom.webhook.signing.secret=prod-test-wecom-webhook-signing-secret-32-bytes-minimum",
        "keyvault.keys.pii=prod-test-pii-keyvault-secret-32-bytes-minimum",
        "keyvault.keys.pii-index=prod-test-pii-index-blind-secret-32-bytes-minimum"
})
@Transactional
class MerchantAppTerminalJdbcIntegrationTests {

    private static final String APP = "merchant_app";
    private static final String PORTAL = "merchant_portal";
    private static final String DEVICE_COOKIE = "complex_device_no";
    private static final String DEVICE_NO = "WB[A-HJKMNP-Z2-9]{12}";

    @DynamicPropertySource
    static void masterPublicKey(DynamicPropertyRegistry registry) throws Exception {
        String key = Base64.getEncoder().encodeToString(KeyPairGenerator.getInstance("Ed25519").generateKeyPair().getPublic().getEncoded());
        registry.add("complex.auth.master.public-key", () -> key);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ContractRepository contracts;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String merchant = "mch_b30ma_" + suffix;
    private final String shop = "usr_b30ma_" + suffix;
    private final String password = "b30-merchant-app-" + suffix;
    private final String ownSettlement = "stl_b30ma_" + suffix;
    private final String otherSettlement = "stl_b30mo_" + suffix;
    private StaffRequestFixture accounts;

    @BeforeEach
    void seed() {
        accounts = new StaffRequestFixture(jdbc);
        account(shop, "staff", merchant, "merchant.portal.settlement.view", "merchant.portal.settlement.dispute");
        Instant now = Instant.now();
        contracts.saveContract(contract("ctr_b30ma_" + suffix, merchant, now));
        contracts.saveContract(contract("ctr_b30mo_" + suffix, "mch_b30mo_" + suffix, now));
        contracts.saveSettlement(settlement(ownSettlement, "ctr_b30ma_" + suffix, now));
        contracts.saveSettlement(settlement(otherSettlement, "ctr_b30mo_" + suffix, now));
    }

    /** 25-R1 (a) ＋ 25-R2：商户账号首次登录——会话终端 merchant_app；系统发编号、后台登记、随登录响应下发；App 不是浏览器，不发 complex_device_no。 */
    @Test
    void firstMerchantAppLoginIssuesRegisteredDeviceNumberWithoutBrowserCookie() throws Exception {
        int before = registered();

        MvcResult login = appLogin(shop, password, null).andExpect(status().isOk()).andReturn();

        assertThat(deviceSetCookie(login)).as("商户手机应用登录不下发 complex_device_no").isNull();
        JsonNode data = body(login).path("data");
        String deviceNo = data.path("device_id").asText();
        assertThat(deviceNo).as("登录响应带本次会话设备编号").matches(DEVICE_NO);
        assertThat(firstUser(deviceNo)).isEqualTo(shop);
        assertThat(registered()).isEqualTo(before + 1);
        Map<String, Object> session = jdbc.queryForMap(
                "SELECT terminal_type, device_id FROM auth_sessions WHERE id = ?", data.path("session_id").asText());
        assertThat(session.get("terminal_type")).isEqualTo(APP);
        assertThat(session.get("device_id")).isEqualTo(deviceNo);
    }

    /** 25-R2：带 App 存的已登记编号再登录沿用、不增行；带没登记过的编号（伪造或后台查不到）发新号并登记。 */
    @Test
    void savedDeviceNumberIsReusedAndUnregisteredOneIsReplaced() throws Exception {
        String deviceNo = deviceOf(appLogin(shop, password, null).andExpect(status().isOk()).andReturn());
        int before = registered();

        assertThat(deviceOf(appLogin(shop, password, deviceNo).andExpect(status().isOk()).andReturn())).isEqualTo(deviceNo);
        assertThat(registered()).isEqualTo(before);

        String replaced = deviceOf(appLogin(shop, password, "WBFORGED23456").andExpect(status().isOk()).andReturn());
        assertThat(replaced).isNotEqualTo("WBFORGED23456");
        assertThat(firstUser(replaced)).isEqualTo(shop);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM browser_devices WHERE device_no = 'WBFORGED23456'", Integer.class)).isZero();
    }

    /** 25-R1 (b)：平台账号（含挂了商户的）与无商户归属员工以商户手机应用登录一律 403 并留失败痕、不建会话；口令错仍先报 401；被拒的都不登记编号。 */
    @Test
    void merchantAppRefusesPlatformAndNonMerchantAccounts() throws Exception {
        String admin = "usr_b30pa_" + suffix;
        String staff = "usr_b30st_" + suffix;
        account(admin, "platform_admin", merchant);
        account(staff, "staff", null);
        int registeredBefore = registered();

        for (String user : List.of(admin, staff)) {
            int failedBefore = failedLogins("MERCHANT_APP_ACCOUNT_REQUIRED");
            appLogin(user, password, null)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value("MERCHANT_APP_ACCOUNT_REQUIRED"));
            assertThat(failedLogins("MERCHANT_APP_ACCOUNT_REQUIRED")).as(user + " 留 auth.login.failed").isEqualTo(failedBefore + 1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_sessions WHERE user_id = ?", Integer.class, user))
                    .as(user + " 不建会话").isZero();
        }
        appLogin(shop, "wrong-" + password, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail_code").value("INVALID_CREDENTIALS"));
        assertThat(registered()).as("被拒与口令错的登录不登记设备编号").isEqualTo(registeredBefore);
    }

    /** 25-R1 (c)：商户手机应用会话读本商户对账、对本商户结算单提异议；写请求按会话派生终端，不落无证明写入留痕；别家结算单仍 403。 */
    @Test
    void merchantAppSessionWorksOnOwnSettlementsWithoutUnprovenWriteTrail() throws Exception {
        String token = accessToken(appLogin(shop, password, null).andExpect(status().isOk()).andReturn());

        JsonNode items = body(mockMvc.perform(app(get("/api/v1/merchant-portal/settlements"), token, APP))
                .andExpect(status().isOk()).andReturn()).at("/data/items");
        List<String> ids = new ArrayList<>();
        items.forEach(item -> ids.add(item.path("id").asText()));
        assertThat(ids).containsExactly(ownSettlement);

        int unprovenBefore = unprovenWrites();
        mockMvc.perform(dispute(ownSettlement, token)).andExpect(status().isOk());
        assertThat(unprovenWrites()).as("会话派生的 merchant_app 写请求不走 grace_allow_unproven_write").isEqualTo(unprovenBefore);

        mockMvc.perform(dispute(otherSettlement, token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
    }

    /** 25-R1 (d)：商户 App 总开关关闭——同一令牌报 merchant_app、改报 merchant_portal 或 web 都 403 TERMINAL_DISABLED，留痕终端都是 merchant_app。 */
    @Test
    void sessionDecidesTerminalSoSelfReportCannotBypassMerchantAppSwitch() throws Exception {
        String token = accessToken(appLogin(shop, password, null).andExpect(status().isOk()).andReturn());
        jdbc.update("UPDATE terminal_capability_switches SET enabled = FALSE WHERE id = 'tcs_term_merchant_app'");
        int before = switchRejections(APP);

        for (String claimed : List.of(APP, "merchant_portal", "web")) {
            mockMvc.perform(app(get("/api/v1/merchant-portal/settlements"), token, claimed))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        }
        assertThat(switchRejections(APP)).isEqualTo(before + 3);
    }

    /** 91-R1 (a)：商户网页门户总开关关闭——商户账号同一网页会话报 merchant_portal、改报 web、不带终端头都 403 TERMINAL_DISABLED，留痕终端都是
     *  merchant_portal；改报 web 的异议也被拒、不落异议留痕。 */
    @Test
    void merchantAccountSelfReportCannotBypassMerchantPortalSwitch() throws Exception {
        Cookie[] session = webSession(shop);
        jdbc.update("UPDATE terminal_capability_switches SET enabled = FALSE WHERE id = 'tcs_term_merchant_portal'");
        int before = switchRejections(PORTAL);

        for (String claimed : Arrays.asList(PORTAL, "web", null)) {
            mockMvc.perform(web(get("/api/v1/merchant-portal/settlements"), session, claimed))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        }
        assertThat(switchRejections(PORTAL)).isEqualTo(before + 3);

        int disputed = disputes();
        mockMvc.perform(webDispute(ownSettlement, session, "web"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        assertThat(disputes()).isEqualTo(disputed);
    }

    /** 91-R1 (b)：商户专区模块开关（V199 tcs_mp_mod_merchant）关闭——商户账号改报 web 并带 X-Module-Code: merchant 也 403 TERMINAL_DISABLED。 */
    @Test
    void merchantAccountSelfReportedWebCannotBypassMerchantModuleSwitch() throws Exception {
        Cookie[] session = webSession(shop);
        jdbc.update("UPDATE terminal_capability_switches SET enabled = FALSE WHERE id = 'tcs_mp_mod_merchant'");

        mockMvc.perform(web(get("/api/v1/merchant-portal/settlements"), session, "web").header("X-Module-Code", "merchant"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
    }

    /** 91-R1 (c)：开关全开时商户账号改报 web 照常使用，只看到本商户结算单。 */
    @Test
    void merchantAccountSelfReportedWebWorksWhenSwitchesOn() throws Exception {
        Cookie[] session = webSession(shop);

        JsonNode items = body(mockMvc.perform(web(get("/api/v1/merchant-portal/settlements"), session, "web"))
                .andExpect(status().isOk()).andReturn()).at("/data/items");
        List<String> ids = new ArrayList<>();
        items.forEach(item -> ids.add(item.path("id").asText()));
        assertThat(ids).containsExactly(ownSettlement);
    }

    /** 91-R1 (d)：无商户归属的员工不按商户网页门户判——该端总开关关闭时，其报 web 的请求不因此被拒。 */
    @Test
    void nonMerchantStaffIsNotJudgedByMerchantPortalSwitch() throws Exception {
        String staff = "usr_b31st_" + suffix;
        account(staff, "staff", null);
        Cookie[] session = webSession(staff);
        jdbc.update("UPDATE terminal_capability_switches SET enabled = FALSE WHERE id = 'tcs_term_merchant_portal'");

        MvcResult result = mockMvc.perform(web(get("/api/v1/merchant-portal/settlements"), session, "web")).andReturn();
        assertThat(body(result).path("detail_code").asText()).isNotEqualTo("TERMINAL_DISABLED");
    }

    /** 25-R2：页面暗纹记 App 的设备编号（裁决 19「暗纹和导出密码用它」）；刷新带同号续期、响应仍是该号，带错号 401。 */
    @Test
    void pageWatermarkUsesAppDeviceNumberAndRefreshKeepsIt() throws Exception {
        MvcResult login = appLogin(shop, password, null).andExpect(status().isOk()).andReturn();
        String token = accessToken(login);

        String code = body(mockMvc.perform(app(post("/api/v1/security/page-views"), token, APP)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("page_id", "ADM-MER-005", "sensitive", false, "object_refs", List.of(),
                                "idempotency_key", "b30ma-pv-" + suffix))))
                .andExpect(status().isOk()).andReturn()).at("/data/code").asText();

        String deviceNo = deviceOf(login);
        Map<String, Object> mark = jdbc.queryForMap("SELECT account_id, device_no FROM watermark_marks WHERE code = ?", code);
        assertThat(mark.get("account_id")).isEqualTo(shop);
        assertThat(mark.get("device_no")).isEqualTo(deviceNo);

        String refresh = body(login).at("/data/refresh_token").asText();
        mockMvc.perform(refresh(token, refresh, "WBWRONG234567"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail_code").value("SESSION_MISMATCH"));
        JsonNode refreshed = body(mockMvc.perform(refresh(token, refresh, deviceNo)).andExpect(status().isOk()).andReturn()).path("data");
        assertThat(refreshed.path("device_id").asText()).isEqualTo(deviceNo);
    }

    private ResultActions appLogin(String loginName, String loginPassword, String deviceId) throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("login_name", loginName);
        request.put("password", loginPassword);
        request.put("terminal_type", APP);
        if (deviceId != null) {
            request.put("device_id", deviceId);
        }
        return mockMvc.perform(post("/api/v1/auth/login")
                .header("X-Terminal-Type", APP)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));
    }

    /** 后台网页登录（AdminApp 登录体只有 login_name、password），取浏览器会话 Cookie 与 CSRF Cookie。 */
    private Cookie[] webSession(String loginName) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Terminal-Type", "web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("login_name", loginName, "password", password))))
                .andExpect(status().isOk()).andReturn();
        return new Cookie[] {login.getResponse().getCookie("admin_access_token"), login.getResponse().getCookie("XSRF-TOKEN")};
    }

    private MockHttpServletRequestBuilder web(MockHttpServletRequestBuilder request, Cookie[] session, String claimed) {
        request.cookie(session).header("X-XSRF-TOKEN", session[1].getValue());
        return claimed == null ? request : request.header("X-Terminal-Type", claimed);
    }

    private MockHttpServletRequestBuilder webDispute(String settlementId, Cookie[] session, String claimed) throws Exception {
        return web(post("/api/v1/merchant-portal/settlements/" + settlementId + "/dispute"), session, claimed)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("idempotency_key", "b31mp-dispute-" + settlementId, "reason", "租金与合同约定不符")));
    }

    private MockHttpServletRequestBuilder app(MockHttpServletRequestBuilder request, String token, String claimed) {
        return request.header("Authorization", "Bearer " + token).header("X-Terminal-Type", claimed);
    }

    private MockHttpServletRequestBuilder dispute(String settlementId, String token) throws Exception {
        return app(post("/api/v1/merchant-portal/settlements/" + settlementId + "/dispute"), token, APP)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("idempotency_key", "b30ma-dispute-" + settlementId, "reason", "租金与合同约定不符")));
    }

    private MockHttpServletRequestBuilder refresh(String token, String refreshToken, String deviceId) throws Exception {
        return app(post("/api/v1/auth/refresh"), token, APP)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("refresh_token", refreshToken, "device_id", deviceId)));
    }

    private void account(String userId, String accountType, String merchantId, String... permissions) {
        accounts.staff(userId, accountType, null, merchantId, permissions);
        jdbc.update("""
                INSERT INTO auth_credentials (user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                                              password_changed_at, status, created_at, updated_at)
                VALUES (?, ?, 'bcrypt', 0, NULL, now(), 'active', now(), now())
                """, userId, new BCryptPasswordEncoder(10).encode(password));
    }

    private static ContractRecord contract(String id, String merchantId, Instant at) {
        return new ContractRecord(id, merchantId, "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of(),
                "fixed_ratio", "by_contract", 0L, Map.of(), List.of(), null, null, List.of(),
                "effective", "proj_1", null, null, null, at, at);
    }

    private static ContractSettlementRecord settlement(String id, String contractId, Instant at) {
        return new ContractSettlementRecord(id, contractId, "move_out", "pending_settlement", List.of(),
                null, null, null, "proj_1", at, at);
    }

    private String deviceOf(MvcResult login) throws Exception {
        String deviceNo = body(login).at("/data/device_id").asText();
        assertThat(deviceNo).as("登录响应 device_id").matches(DEVICE_NO);
        return deviceNo;
    }

    private String accessToken(MvcResult login) throws Exception {
        return body(login).at("/data/access_token").asText();
    }

    private String deviceSetCookie(MvcResult result) {
        return result.getResponse().getHeaders("Set-Cookie").stream()
                .filter(h -> h.startsWith(DEVICE_COOKIE + "="))
                .findFirst()
                .orElse(null);
    }

    private String firstUser(String deviceNo) {
        return jdbc.queryForObject("SELECT first_user_id FROM browser_devices WHERE device_no = ?", String.class, deviceNo);
    }

    private int registered() {
        return jdbc.queryForObject("SELECT count(*) FROM browser_devices", Integer.class);
    }

    private int failedLogins(String code) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM audit_events WHERE event_name = 'auth.login.failed' AND metadata->>'error_code' = ?
                """, Integer.class, code);
    }

    private int unprovenWrites() {
        return jdbc.queryForObject("""
                SELECT count(*) FROM audit_events WHERE event_name = 'terminal.device_credential'
                   AND metadata->>'reason' = 'grace_allow_unproven_write' AND metadata->>'terminal_type' = 'merchant_app'
                """, Integer.class);
    }

    private int switchRejections(String terminal) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM audit_events WHERE event_name = 'terminal.access_rejected'
                   AND metadata->>'terminal_type' = ? AND metadata->>'capability' = 'terminal.enabled'
                """, Integer.class, terminal);
    }

    private int disputes() {
        return jdbc.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_name = 'merchant_portal.settlement.disputed'", Integer.class);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
