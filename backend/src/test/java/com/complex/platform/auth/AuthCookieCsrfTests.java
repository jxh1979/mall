package com.complex.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.GlobalExceptionHandler;
import com.complex.platform.config.SecurityConfig;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台令牌迁 HttpOnly Cookie + CSRF 双提交（2026-07-19 用户拍板）的鉴权切片测试。
 * 覆盖：登录种 HttpOnly Cookie / 带 Cookie 通过 / 无凭证拒 / 写请求缺 CSRF 头被拒 / CSRF 正确通过 /
 * 设备端 Bearer 头写请求免 CSRF（双通道兼容）。
 *
 * <p>@WebMvcTest 切片（不触发 Flyway），可独立运行。
 */
@WebMvcTest(controllers = {AuthController.class, AuthProbeController.class})
@ActiveProfiles("test")
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        BearerAuthenticationFilter.class,
        DoubleSubmitCsrfFilter.class,
        AuthCookieCsrfTests.TestBeans.class
})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class AuthCookieCsrfTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loginIssuesHttpOnlyTokenCookiesAndReadableCsrfCookie() throws Exception {
        MvcResult result = loginResult();

        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        String accessCookie = setCookies.stream().filter(h -> h.startsWith("admin_access_token=")).findFirst().orElseThrow();
        String refreshCookie = setCookies.stream().filter(h -> h.startsWith("admin_refresh_token=")).findFirst().orElseThrow();
        String csrfCookie = setCookies.stream().filter(h -> h.startsWith("XSRF-TOKEN=")).findFirst().orElseThrow();

        // 令牌 Cookie 必须 HttpOnly（JS 读不到）+ SameSite=Strict（跨站不带）。
        assertThat(accessCookie).contains("HttpOnly").contains("SameSite=Strict").contains("Path=/");
        assertThat(refreshCookie).contains("HttpOnly").contains("SameSite=Strict").contains("Path=/api/v1/auth");
        // CSRF Cookie 必须可被 JS 读取（非 HttpOnly），才能回填 X-XSRF-TOKEN 头。
        assertThat(csrfCookie).doesNotContain("HttpOnly").contains("SameSite=Strict");
    }

    @Test
    void cookieAuthenticatesProtectedRead() throws Exception {
        MvcResult login = loginResult();
        Cookie access = login.getResponse().getCookie(AuthCookies.ACCESS_COOKIE);

        mockMvc.perform(get("/api/v1/test/probe").cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("ok"));
    }

    @Test
    void missingCredentialsRejectedOnProtectedRead() throws Exception {
        mockMvc.perform(get("/api/v1/test/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail_code").value("AUTH_SESSION_INVALID"));
    }

    @Test
    void cookieWriteWithoutCsrfHeaderRejected() throws Exception {
        MvcResult login = loginResult();
        Cookie access = login.getResponse().getCookie(AuthCookies.ACCESS_COOKIE);
        Cookie csrf = login.getResponse().getCookie(AuthCookies.CSRF_COOKIE);

        // 带 Cookie 会话但缺 X-XSRF-TOKEN 头 → 403 CSRF。
        mockMvc.perform(post("/api/v1/test/write").cookie(access, csrf)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    void cookieWriteWithMismatchedCsrfHeaderRejected() throws Exception {
        MvcResult login = loginResult();
        Cookie access = login.getResponse().getCookie(AuthCookies.ACCESS_COOKIE);
        Cookie csrf = login.getResponse().getCookie(AuthCookies.CSRF_COOKIE);

        mockMvc.perform(post("/api/v1/test/write").cookie(access, csrf)
                        .header(AuthCookies.CSRF_HEADER, "not-the-cookie-value")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    void cookieWriteWithMatchingCsrfHeaderAccepted() throws Exception {
        MvcResult login = loginResult();
        Cookie access = login.getResponse().getCookie(AuthCookies.ACCESS_COOKIE);
        Cookie csrf = login.getResponse().getCookie(AuthCookies.CSRF_COOKIE);

        mockMvc.perform(post("/api/v1/test/write").cookie(access, csrf)
                        .header(AuthCookies.CSRF_HEADER, csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("written"));
    }

    @Test
    void deviceBearerWriteBypassesCsrf() throws Exception {
        // 设备端（安卓/一体机）：Authorization: Bearer 头 + 无 Cookie + 无 CSRF 头，写请求照常通过（双通道兼容）。
        JsonNode login = objectMapper.readTree(loginResult().getResponse().getContentAsString());
        String accessToken = login.at("/data/access_token").asText();

        mockMvc.perform(post("/api/v1/test/write")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("written"));
    }

    @Test
    void logoutClearsSessionCookies() throws Exception {
        MvcResult login = loginResult();
        Cookie access = login.getResponse().getCookie(AuthCookies.ACCESS_COOKIE);
        Cookie csrf = login.getResponse().getCookie(AuthCookies.CSRF_COOKIE);
        String sessionId = objectMapper.readTree(login.getResponse().getContentAsString()).at("/data/session_id").asText();

        MvcResult logout = mockMvc.perform(post("/api/v1/auth/logout").cookie(access, csrf)
                        .header(AuthCookies.CSRF_HEADER, csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"logout-1\",\"session_id\":\"" + sessionId + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // 三个 Cookie 均被 Max-Age=0 清除。
        List<String> cleared = logout.getResponse().getHeaders("Set-Cookie");
        assertThat(cleared).anyMatch(h -> h.startsWith("admin_access_token=") && h.contains("Max-Age=0"));
        assertThat(cleared).anyMatch(h -> h.startsWith("admin_refresh_token=") && h.contains("Max-Age=0"));
        assertThat(cleared).anyMatch(h -> h.startsWith("XSRF-TOKEN=") && h.contains("Max-Age=0"));
    }

    private MvcResult loginResult() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
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

/** 受保护探针端点（/api/v1/test/**，需鉴权），验证 Cookie/Bearer 鉴权与 CSRF 划界；顶层类保证 @WebMvcTest 切片能映射其路由。 */
@RestController
class AuthProbeController {

    @GetMapping("/api/v1/test/probe")
    ApiResponse<String> probe() {
        return ApiResponse.ok("ok");
    }

    @PostMapping("/api/v1/test/write")
    ApiResponse<String> write() {
        return ApiResponse.ok("written");
    }
}
