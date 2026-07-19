package com.complex.platform.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

/**
 * 后台 Web 会话 Cookie（2026-07-19 用户拍板：令牌从 localStorage 迁到 HttpOnly Cookie + CSRF 双提交）。
 * 目的：JS 读不到访问/刷新令牌 → XSS 无法窃取管理员令牌。
 *
 * <p>三个 Cookie：
 * <ul>
 *   <li>{@code admin_access_token}——访问令牌，HttpOnly（JS 不可读），Path=/（全站 API 携带）。</li>
 *   <li>{@code admin_refresh_token}——刷新令牌，HttpOnly，Path=/api/v1/auth（仅认证端点携带，缩小暴露面）。</li>
 *   <li>{@code XSRF-TOKEN}——CSRF 双提交令牌，非 HttpOnly（JS 读出后回填 {@code X-XSRF-TOKEN} 头）。</li>
 * </ul>
 *
 * <p>会话 Cookie（不设 Max-Age）：F5 保留（满足「刷新不掉登录」），关闭浏览器即失效（比 localStorage 落盘更安全）。
 * {@code SameSite=Strict}：跨站请求一律不带 Cookie，结构性防 CSRF；{@code Secure} 按请求是否 https 决定（dev http 也能用）。
 *
 * <p>设备端（安卓/一体机）零影响：它们走 {@code Authorization: Bearer} 头 + 响应体令牌，不使用这些 Cookie。
 */
public final class AuthCookies {

    public static final String ACCESS_COOKIE = "admin_access_token";
    public static final String REFRESH_COOKIE = "admin_refresh_token";
    public static final String CSRF_COOKIE = "XSRF-TOKEN";
    public static final String CSRF_HEADER = "X-XSRF-TOKEN";

    private static final String AUTH_PATH = "/api/v1/auth";
    private static final SecureRandom RANDOM = new SecureRandom();

    private AuthCookies() {
    }

    /** 双提交 CSRF 令牌（无需服务端存储：只校验 header 与 cookie 相等即证明同源 JS 读出后回填）。 */
    public static String newCsrfToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 登录/刷新成功后写会话 Cookie。{@code csrfToken} 传 null 时不覆写 XSRF-TOKEN（刷新时保留登录下发的同一个）。
     */
    public static void writeSession(HttpServletRequest request, HttpServletResponse response,
                                    String accessToken, String refreshToken, String csrfToken) {
        boolean secure = request.isSecure();
        addCookie(response, ResponseCookie.from(ACCESS_COOKIE, accessToken)
                .httpOnly(true).secure(secure).sameSite("Strict").path("/").build());
        if (refreshToken != null && !refreshToken.isBlank()) {
            addCookie(response, ResponseCookie.from(REFRESH_COOKIE, refreshToken)
                    .httpOnly(true).secure(secure).sameSite("Strict").path(AUTH_PATH).build());
        }
        if (csrfToken != null && !csrfToken.isBlank()) {
            addCookie(response, ResponseCookie.from(CSRF_COOKIE, csrfToken)
                    .httpOnly(false).secure(secure).sameSite("Strict").path("/").build());
        }
    }

    /** 登出：清空三个 Cookie（Max-Age=0）。设备端无这些 Cookie，清空无副作用。 */
    public static void clearSession(HttpServletRequest request, HttpServletResponse response) {
        boolean secure = request.isSecure();
        expire(response, ACCESS_COOKIE, "/", secure, true);
        expire(response, REFRESH_COOKIE, AUTH_PATH, secure, true);
        expire(response, CSRF_COOKIE, "/", secure, false);
    }

    public static String readAccessToken(HttpServletRequest request) {
        return readCookie(request, ACCESS_COOKIE);
    }

    public static String readRefreshToken(HttpServletRequest request) {
        return readCookie(request, REFRESH_COOKIE);
    }

    public static String readCsrf(HttpServletRequest request) {
        return readCookie(request, CSRF_COOKIE);
    }

    private static void addCookie(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private static void expire(HttpServletResponse response, String name, String path, boolean secure, boolean httpOnly) {
        addCookie(response, ResponseCookie.from(name, "")
                .httpOnly(httpOnly).secure(secure).sameSite("Strict").path(path).maxAge(0).build());
    }

    private static String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                String value = cookie.getValue();
                return value == null || value.isBlank() ? null : value;
            }
        }
        return null;
    }
}
