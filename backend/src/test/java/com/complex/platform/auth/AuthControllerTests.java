package com.complex.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.api.GlobalExceptionHandler;
import com.complex.platform.config.SecurityConfig;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = AuthController.class)
@ActiveProfiles("test")
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        BearerAuthenticationFilter.class,
        AuthControllerTests.TestBeans.class
})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loginAcceptsLoginNamePayloadAndReturnsSessionRefreshTokenShape() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login_name": "p0_admin",
                                  "password": "secret",
                                  "device_id": "web-1",
                                  "terminal_type": "web"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access_token").isString())
                .andExpect(jsonPath("$.data.refresh_token").isString())
                .andExpect(jsonPath("$.data.session_id").isString())
                .andExpect(jsonPath("$.data.token_type").value("Bearer"))
                .andExpect(jsonPath("$.data.user.username").value("p0_admin"));
    }

    @Test
    void usernamePayloadStillAuthenticatesForBackwardCompatibility() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "p0_admin",
                                  "password": "secret"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access_token").isString())
                .andExpect(jsonPath("$.data.refresh_token").isString())
                .andExpect(jsonPath("$.data.session_id").isString());
    }

    @Test
    void emptyLoginPayloadReturnsValidationFailureInsteadOfInternalError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.data.access_token").doesNotExist());
    }

    @Test
    void emptyLogoutPayloadWithBearerReturnsValidationFailureInsteadOfInternalError() throws Exception {
        JsonNode login = login();
        String accessToken = login.at("/data/access_token").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void refreshEndpointIsReachableWithoutAccessTokenSimulatingExpiredTokenScenario() throws Exception {
        // Login to get a valid refresh token
        JsonNode login = login();
        String refreshToken = login.at("/data/refresh_token").asText();

        // Simulate the prod expired-access-token scenario: send a valid refresh_token but omit
        // the Authorization header (as happens when the access token expires and the bearer filter
        // does not populate the security context).
        // BEFORE FIX: Spring Security returns 401 because /api/v1/auth/refresh was not in
        //             publicApiPaths() — the refresh token is never even read.
        // AFTER FIX:  The request reaches AuthService.refresh(), which finds the session, then
        //             rejects with 400 VALIDATION_FAILED (access_token is required) — proving
        //             Spring Security no longer blocks the endpoint.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refresh_token": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void refreshAndLogoutRequireBearerTokenInsteadOfPermitAll() throws Exception {
        int refreshStatus = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refresh_token": "p0_refresh_missing"
                                }
                                """))
                .andReturn()
                .getResponse()
                .getStatus();
        int logoutStatus = mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "logout-once",
                                  "session_id": "sess_missing"
                                }
                                """))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(refreshStatus).isIn(401, 403);
        assertThat(logoutStatus).isIn(401, 403);
    }

    @Test
    void refreshRotatesRefreshTokenAndRejectsReusedTokenWith4xxResponse() throws Exception {
        JsonNode login = login();
        String accessToken = login.at("/data/access_token").asText();
        String refreshToken = login.at("/data/refresh_token").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refresh_token": "%s",
                                  "device_id": "web-1"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access_token").isString())
                .andExpect(jsonPath("$.data.refresh_token").isString())
                .andExpect(jsonPath("$.data.session_id").value(login.at("/data/session_id").asText()))
                .andReturn();

        JsonNode refresh = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        assertThat(refresh.at("/data/access_token").asText()).isNotEqualTo(accessToken);
        assertThat(refresh.at("/data/refresh_token").asText()).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer " + refresh.at("/data/access_token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refresh_token": "%s",
                                  "device_id": "web-1"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));
    }

    @Test
    void logoutRevokesAccessTokenAndRefreshToken() throws Exception {
        JsonNode login = login();
        String accessToken = login.at("/data/access_token").asText();
        String refreshToken = login.at("/data/refresh_token").asText();
        String sessionId = login.at("/data/session_id").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "logout-once",
                                  "session_id": "%s",
                                  "device_id": "web-1",
                                  "operator_note": "normal logout"
                                }
                                """.formatted(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.session_id").value(sessionId))
                .andExpect(jsonPath("$.data.status").value("logged_out"));

        int authenticatedStatus = mockMvc.perform(get("/api/v1/access/permissions")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(authenticatedStatus).isIn(401, 403);

        // After adding /api/v1/auth/refresh to publicApiPaths (auth-001 fix), the request
        // now reaches the controller; the service layer rejects the revoked refresh token.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refresh_token": "%s",
                                  "device_id": "web-1"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));
    }

    private JsonNode login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login_name": "p0_admin",
                                  "password": "secret",
                                  "device_id": "web-1",
                                  "terminal_type": "web"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        AuthService authService(AuditEventService auditEventService) {
            return new AuthService("p0_admin", "secret", auditEventService);
        }

        @Bean
        AuditEventService auditEventService(AuditEventRepository auditEventRepository) {
            return new AuditEventService(auditEventRepository);
        }

        @Bean
        AuditEventRepository auditEventRepository() {
            return new InMemoryAuditEventRepository();
        }
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }

}
