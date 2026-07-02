package com.complex.platform.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 3.3 免登录 cookie：客人 web 端登录后 token 存 httpOnly cookie（脚本不可读，防 XSS 窃取），
 * 本过滤器把 cookie 桥接为 {@code X-Customer-Token} 头——下游
 * {@link CustomerTokenAuthenticationFilter} 与 19 处控制器 {@code @RequestHeader} 读取零改动。
 *
 * <p>CSRF 防御（cookie 属浏览器自动携带的环境凭证）：cookie 种为 SameSite=Strict；
 * 纵深再加一道——cookie 鉴权的非安全方法（POST/PUT/PATCH/DELETE）必须带
 * {@code X-Requested-With} 自定义头（跨站表单/顶层导航发不出自定义头），缺失 403 fail-closed。
 * 显式头 token 调用方（小程序/App/Android）完全不受影响：有头则 cookie 不生效、CSRF 闸不触发。
 *
 * <p>@Order(HIGHEST_PRECEDENCE)：在 Spring Security 链之前包装请求，保证安全链与 MVC 层都看到桥接后的头。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CustomerSessionCookieFilter extends org.springframework.web.filter.OncePerRequestFilter {

    public static final String COOKIE_NAME = "customer_session";
    static final String CUSTOMER_PATH_PREFIX = "/api/v1/customer";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        boolean customerPath = uri != null && uri.startsWith(CUSTOMER_PATH_PREFIX);
        boolean hasExplicitToken = request.getHeader("X-Customer-Token") != null
                || request.getHeader("Authorization") != null;
        String cookieToken = customerPath && !hasExplicitToken ? readCookie(request) : null;
        if (cookieToken == null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!SAFE_METHODS.contains(request.getMethod()) && request.getHeader("X-Requested-With") == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"code\":\"FORBIDDEN\",\"detail_code\":\"CSRF_HEADER_REQUIRED\","
                            + "\"message\":\"cookie-authenticated write requires X-Requested-With header\"}");
            return;
        }
        filterChain.doFilter(new CookieTokenRequest(request, cookieToken), response);
    }

    private String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /** 把 cookie 值暴露为 X-Customer-Token 头（仅该头，其他请求属性原样透传）。 */
    private static final class CookieTokenRequest extends HttpServletRequestWrapper {
        private final String token;

        private CookieTokenRequest(HttpServletRequest request, String token) {
            super(request);
            this.token = token;
        }

        @Override
        public String getHeader(String name) {
            if ("X-Customer-Token".equalsIgnoreCase(name)) {
                return token;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if ("X-Customer-Token".equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(token));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new java.util.LinkedHashSet<>();
            Enumeration<String> original = super.getHeaderNames();
            while (original != null && original.hasMoreElements()) {
                names.add(original.nextElement());
            }
            names.add("X-Customer-Token");
            return Collections.enumeration(names);
        }
    }
}
