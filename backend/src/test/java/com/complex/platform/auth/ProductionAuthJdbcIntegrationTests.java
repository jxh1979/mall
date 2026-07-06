package com.complex.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
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
        "spring.datasource.password=complex_dev_pwd",
        "complex.database.credential-separation.enforced=false",
        "complex.auth.production.seed-admin.username=p0_prod_test_admin",
        "complex.auth.production.seed-admin.password=p0_prod_test_password",
        "complex.auth.failed-login.max-attempts=3",
        "complex.auth.failed-login.lock-duration=PT15M",
        "ticket.credential.signing.secret=prod-test-ticket-signing-secret-32-bytes-minimum",
        "bar.invitation.signing.secret=prod-test-bar-invitation-signing-secret-32-bytes-minimum"
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
