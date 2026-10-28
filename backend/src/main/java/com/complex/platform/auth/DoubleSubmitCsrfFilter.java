package com.complex.platform.auth;

import com.complex.platform.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CSRF 双提交校验（2026-07-19 用户拍板：后台令牌迁 HttpOnly Cookie 后开启 CSRF 防护）。
 *
 * <p>规则：仅当「非安全方法（POST/PUT/PATCH/DELETE）」且「携带 {@code admin_access_token} Cookie
 * （即后台 Web Cookie 会话）」时，才要求请求头 {@code X-XSRF-TOKEN} 与 {@code XSRF-TOKEN} Cookie 一致，否则 403。
 *
 * <p>为何这样划界（设备端零影响）：
 * <ul>
 *   <li>设备端（安卓/一体机）用 {@code Authorization: Bearer} 头认证，不带后台 Cookie → 不触发本校验。
 *       Bearer 天然免疫 CSRF：跨站页面无法设置自定义 Authorization 头。此处再显式短路一次。</li>
 *   <li>客人端用 {@code X-Customer-Token}/{@code Bearer} 头，不带后台 Cookie → 不触发。</li>
 *   <li>登录接口一律不触发：登录前尚无有效会话，可能残留的旧令牌 Cookie（HttpOnly，JS 清不掉）不能把登录拦成 403（#549）；GET/HEAD 等安全方法 → 不触发。</li>
 * </ul>
 * 之所以自实现而非 Spring 内置 CSRF：本系统鉴权是自建 Bearer/Cookie 令牌（非 Spring Session），
 * 双提交仅需 ~一屏代码、无状态、可精确按「是否 Cookie 会话」划界，避开 Spring 6 CSRF 延迟加载与会话假设的坑。
 * ponytail: 双提交为无状态方案；若日后要令牌绑定会话/一次性，再升级为服务端存储 CSRF 令牌。
 */
@Component
public class DoubleSubmitCsrfFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final String LOGIN_PATH = "/api/v1/auth/login";

    // 与 SecurityConfig 的 401 入口点一致：SNAKE_CASE + ISO 时间，保证 403 信封字段名与业务响应一致。
    private static final ObjectMapper ENVELOPE_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (requiresCsrf(request) && !csrfValid(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            ENVELOPE_MAPPER.writeValue(response.getWriter(), ApiResponse.error(
                    "FORBIDDEN", "CSRF_TOKEN_INVALID", "安全校验未通过，请刷新页面后重试"));
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean requiresCsrf(HttpServletRequest request) {
        if (SAFE_METHODS.contains(request.getMethod()) || LOGIN_PATH.equals(request.getRequestURI())) {
            return false;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return false; // 设备端 Bearer 会话天然免疫 CSRF
        }
        return AuthCookies.readAccessToken(request) != null; // 仅后台 Web Cookie 会话需要 CSRF
    }

    private boolean csrfValid(HttpServletRequest request) {
        String cookie = AuthCookies.readCsrf(request);
        String header = request.getHeader(AuthCookies.CSRF_HEADER);
        if (cookie == null || header == null || cookie.isBlank() || header.isBlank()) {
            return false;
        }
        // 常量时间比较，防定时侧信道。
        return MessageDigest.isEqual(cookie.getBytes(StandardCharsets.UTF_8), header.getBytes(StandardCharsets.UTF_8));
    }
}
