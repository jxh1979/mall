package com.complex.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "complex.database.credential-separation.enforced=false",
        "refund.tiered_authorization.enabled=true",
        "complex.auth.production.seed-admin.username=p0_prod_test_admin",
        "complex.auth.production.seed-admin.password=p0_prod_test_password",
        "complex.auth.failed-login.max-attempts=3",
        "complex.auth.failed-login.lock-duration=PT15M",
        "ticket.credential.signing.secret=prod-test-ticket-signing-secret-32-bytes-minimum",
        "bar.invitation.signing.secret=prod-test-bar-invitation-signing-secret-32-bytes-minimum",
        "compliance.wecom.webhook.signing.secret=prod-test-wecom-webhook-signing-secret-32-bytes-minimum"
})
@Transactional
class ProductionAuthJdbcIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProductionAuthService productionAuthService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    @Test
    void prodProfileLoginRefreshAndLogoutUseJdbcSessionsWithoutPlaintextTokens() throws Exception {
        JsonNode login = login("p0_prod_test_admin", "p0_prod_test_password");
        String accessToken = login.at("/data/access_token").asText();
        String refreshToken = login.at("/data/refresh_token").asText();
        String sessionId = login.at("/data/session_id").asText();

        assertThat(accessToken).startsWith("p0_prod_access_");
        assertThat(refreshToken).startsWith("p0_prod_refresh_");
        assertThat(sessionId).startsWith("sess_");
        assertThat(login.at("/data/user/account_type").asText()).isEqualTo("platform_admin");

        jdbcTemplate.queryForObject("""
                SELECT access_token_hash, refresh_token_hash
                  FROM auth_sessions
                 WHERE id = ?
                """, (rs, rowNum) -> {
            assertThat(rs.getString("access_token_hash")).hasSize(64).isNotEqualTo(accessToken);
            assertThat(rs.getString("refresh_token_hash")).hasSize(64).isNotEqualTo(refreshToken);
            return true;
        }, sessionId);

        mockMvc.perform(get("/api/v1/ops/production-readiness/items")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refresh_token": "%s",
                                  "device_id": "prod-web-1"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.session_id").value(sessionId))
                .andReturn();

        JsonNode refreshed = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String rotatedAccessToken = refreshed.at("/data/access_token").asText();
        String rotatedRefreshToken = refreshed.at("/data/refresh_token").asText();
        assertThat(rotatedAccessToken).isNotEqualTo(accessToken);
        assertThat(rotatedRefreshToken).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer " + rotatedAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refresh_token": "%s",
                                  "device_id": "prod-web-1"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail_code").value("REFRESH_TOKEN_INVALID"));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + rotatedAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "prod-logout-once",
                                  "session_id": "%s",
                                  "device_id": "prod-web-1",
                                  "operator_note": "integration test logout"
                                }
                                """.formatted(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("logged_out"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM auth_sessions WHERE id = ?",
                String.class,
                sessionId
        )).isEqualTo("logged_out");

        int afterLogoutStatus = mockMvc.perform(get("/api/v1/ops/production-readiness/items")
                        .header("Authorization", "Bearer " + rotatedAccessToken))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(afterLogoutStatus).isIn(401, 403);
    }

    @Test
    void seedAdminDoesNotOverwriteExistingCredentialWhenConfiguredAgain() throws Exception {
        String userId = jdbcTemplate.queryForObject(
                "SELECT id FROM access_users WHERE username = ?",
                String.class,
                "p0_prod_test_admin"
        );
        jdbcTemplate.update("""
                UPDATE auth_credentials
                   SET password_hash = ?,
                       failed_attempt_count = 0,
                       locked_until = NULL,
                       status = 'active'
                 WHERE user_id = ?
                """, passwordEncoder.encode("operator-rotated-password"), userId);

        productionAuthService.seedAdminWhenConfigured();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login_name": "p0_prod_test_admin",
                                  "password": "p0_prod_test_password"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        login("p0_prod_test_admin", "operator-rotated-password");
    }

    @Test
    void prodProfileRepeatedInvalidPasswordLocksSeededAccount() throws Exception {
        for (int index = 0; index < 3; index++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "login_name": "p0_prod_test_admin",
                                      "password": "wrong-password"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login_name": "p0_prod_test_admin",
                                  "password": "p0_prod_test_password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail_code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void lockingCredentialImmediatelyRevokesAlreadyIssuedAccessToken() throws Exception {
        JsonNode login = login("p0_prod_test_admin", "p0_prod_test_password");
        String accessToken = login.at("/data/access_token").asText();

        // Token is valid before the credential is locked
        mockMvc.perform(get("/api/v1/ops/production-readiness/items")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Admin/lockout disables the credential (lock window in the future)
        String userId = jdbcTemplate.queryForObject(
                "SELECT id FROM access_users WHERE username = ?", String.class, "p0_prod_test_admin");
        jdbcTemplate.update("""
                UPDATE auth_credentials
                   SET status = 'locked',
                       locked_until = now() + interval '15 minutes'
                 WHERE user_id = ?
                """, userId);

        // The already-issued access token must now be rejected, not honoured until expiry
        int statusAfterLock = mockMvc.perform(get("/api/v1/ops/production-readiness/items")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn().getResponse().getStatus();
        assertThat(statusAfterLock).isIn(401, 403);
    }

    @Test
    void prodProfileProtectsOpenApiAndSwaggerEndpointsFromAnonymousAccess() throws Exception {
        expectAnonymousAccessProtected("/v3/api-docs");
        expectAnonymousAccessProtected("/swagger-ui/index.html");
        expectAnonymousAccessProtected("/docs/openapi");
    }

    private static final java.security.KeyPair MASTER_KEY_PAIR = generateMasterKeyPair();

    /** 破窗验签公钥（R-004：服务器只留公钥；私钥只在测试进程内模拟 U 盘）。 */
    @org.springframework.test.context.DynamicPropertySource
    static void masterPublicKey(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("complex.auth.master.public-key",
                () -> java.util.Base64.getEncoder().encodeToString(MASTER_KEY_PAIR.getPublic().getEncoded()));
    }

    private static java.security.KeyPair generateMasterKeyPair() {
        try {
            return java.security.KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    // M008-F-06 / F-04（PGM-1.15.1「万一管理员账号全部无法登录，走破窗应急通道恢复进入系统」「破窗一旦使用即全程留痕」；R-004 服务器只留验签公钥）：
    // prod 真实链路：挑战 → U 盘私钥签名 → 签发保留超管会话，后台 Web 与日常登录同一 Cookie 通道（HttpOnly + CSRF）；
    // 保留超管落库、会话只存哈希；挑战一次性；保留超管不能凭口令走 /login。
    @Test
    void breakGlassIssuesMasterSessionWithCookiesAndPasswordLoginStaysImpossible() throws Exception {
        MvcResult challengeResult = mockMvc.perform(post("/api/v1/auth/master/challenge"))
                .andExpect(status().isOk())
                .andReturn();
        String challenge = objectMapper.readTree(challengeResult.getResponse().getContentAsString()).at("/data/challenge").asText();
        java.security.Signature signer = java.security.Signature.getInstance("Ed25519");
        signer.initSign(MASTER_KEY_PAIR.getPrivate());
        signer.update(challenge.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String signature = java.util.Base64.getEncoder().encodeToString(signer.sign());
        String body = objectMapper.writeValueAsString(java.util.Map.of("challenge", challenge, "signature", signature));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/master/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("master-authority"))
                .andExpect(jsonPath("$.data.user.account_type").value("master_authority"))
                .andReturn();
        java.util.List<String> cookies = loginResult.getResponse().getHeaders("Set-Cookie");
        assertThat(cookies).anyMatch(cookie -> cookie.startsWith("admin_access_token=") && cookie.contains("HttpOnly"));
        assertThat(cookies).anyMatch(cookie -> cookie.startsWith("XSRF-TOKEN="));
        JsonNode login = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = login.at("/data/access_token").asText();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM access_users WHERE id = 'usr_master_authority' AND username = 'master-authority' AND account_type = 'master_authority'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList(
                "SELECT device_id || '/' || terminal_type FROM auth_sessions WHERE user_id = 'usr_master_authority' AND status = 'active'",
                String.class)).containsExactly("usb-master:127.0.0.1/recovery");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM auth_sessions WHERE user_id = 'usr_master_authority' AND access_token_hash = ?",
                Integer.class, accessToken)).isZero(); // 会话只存哈希，不存明文令牌

        mockMvc.perform(get("/api/v1/ops/production-readiness/items")
                        .cookie(new jakarta.servlet.http.Cookie("admin_access_token", accessToken)))
                .andExpect(status().isOk()); // Cookie 会话可进受保护后台读接口（与日常登录同通道）

        String refreshToken = login.at("/data/refresh_token").asText();
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refresh_token": "%s",
                                  "device_id": "usb-master:127.0.0.1"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail_code").value("MASTER_SESSION_REFRESH_FORBIDDEN"));

        productionAuthService.revokeOutstandingMasterSessions();
        int statusAfterRevoke = mockMvc.perform(get("/api/v1/ops/production-readiness/items")
                        .cookie(new jakarta.servlet.http.Cookie("admin_access_token", accessToken)))
                .andReturn().getResponse().getStatus();
        assertThat(statusAfterRevoke).isIn(401, 403);

        mockMvc.perform(post("/api/v1/auth/master/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized()); // 挑战一次性：重放拒绝

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login_name": "master-authority",
                                  "password": "any-password-at-all-1234"
                                }
                                """))
                .andExpect(status().isUnauthorized()); // 保留超管只能凭 U 盘，不能凭口令
    }

    // M008-A1-03（BSM-1.26.1「上线生产必须换掉开发默认的弱口令…必须换成强密码，这是上线生产的必做一步」）：
    // 生产种子管理员口令拒绝开发默认与弱值，启动即 fail-closed；测试用强口令通过。
    @Test
    void weakOrDefaultSeedAdminPasswordIsRejectedInProduction() {
        for (String weak : java.util.List.of("admin-pass", "admin", "password", "short-1", "prod-change-me-secret-key")) {
            assertThatThrownBy(() -> ProductionAuthService.requireStrongSeedPassword(weak))
                    .isInstanceOf(IllegalStateException.class);
        }
        ProductionAuthService.requireStrongSeedPassword("p0_prod_test_password");
    }

    // M008-G2-02（BSM-1.26.1「持有授权 U 盘密钥的人才是最高权限；最高权限全平台只设一人」；PGM-1.15.1 破窗恢复进入系统；
    // R-001 通配只认 usr_master_authority + master-authority）：他人预占保留用户名时，破窗仍落到保留 id，
    // 预占账号被改名留痕、不被提权，其口令不能以 master-authority 登录。
    @Test
    void breakGlassLandsOnTheReservedIdentityEvenWhenTheUsernameWasPreRegistered() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)
                VALUES ('usr_intruder', 'master-authority', 'staff', 'org-x', NULL, 'active', now())
                """);
        jdbcTemplate.update("""
                INSERT INTO auth_credentials (user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                                              password_changed_at, status, created_at, updated_at)
                VALUES ('usr_intruder', ?, 'bcrypt', 0, NULL, now(), 'active', now(), now())
                """, passwordEncoder.encode("intruder-password-1234"));

        JsonNode breakGlass = breakGlassLogin();
        assertThat(breakGlass.at("/data/user/id").asText()).isEqualTo("usr_master_authority");
        assertThat(breakGlass.at("/data/user/username").asText()).isEqualTo("master-authority");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT username FROM access_users WHERE id = 'usr_master_authority'", String.class)).isEqualTo("master-authority");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT username FROM access_users WHERE id = 'usr_intruder'", String.class))
                .isNotEqualTo("master-authority").startsWith("master-authority-displaced-");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT account_type FROM access_users WHERE id = 'usr_intruder'", String.class)).isEqualTo("staff");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login_name": "master-authority",
                                  "password": "intruder-password-1234"
                                }
                                """))
                .andExpect(status().isUnauthorized()); // 预占者的口令不再能以保留用户名登录
    }

    private JsonNode breakGlassLogin() throws Exception {
        MvcResult challengeResult = mockMvc.perform(post("/api/v1/auth/master/challenge"))
                .andExpect(status().isOk())
                .andReturn();
        String challenge = objectMapper.readTree(challengeResult.getResponse().getContentAsString()).at("/data/challenge").asText();
        java.security.Signature signer = java.security.Signature.getInstance("Ed25519");
        signer.initSign(MASTER_KEY_PAIR.getPrivate());
        signer.update(challenge.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String signature = java.util.Base64.getEncoder().encodeToString(signer.sign());
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/master/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("challenge", challenge, "signature", signature))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loginResult.getResponse().getContentAsString());
    }

    private JsonNode login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login_name": "%s",
                                  "password": "%s",
                                  "device_id": "prod-web-1",
                                  "terminal_type": "web"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access_token").isString())
                .andExpect(jsonPath("$.data.refresh_token").isString())
                .andExpect(jsonPath("$.data.session_id").isString())
                .andExpect(jsonPath("$.data.token_type").value("Bearer"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void expectAnonymousAccessProtected(String path) throws Exception {
        int status = mockMvc.perform(get(path))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(status).isIn(401, 403);
    }
}
