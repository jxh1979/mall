package com.complex.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * 3.3 免登录 cookie：CustomerSessionCookieFilter 单测。
 * 契约：httpOnly cookie 桥接为 X-Customer-Token 头（19 处 @RequestHeader 读取零改动）；
 * cookie 鉴权的非安全方法必须带 X-Requested-With 自定义头（CSRF 防御，跨站表单发不出自定义头），
 * 否则 403 fail-closed；显式头 token 优先、cookie 不覆盖。
 */
class CustomerSessionCookieFilterTests {

    private final CustomerSessionCookieFilter filter = new CustomerSessionCookieFilter();

    @Test
    void bridgesCookieToCustomerTokenHeaderOnGet() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/customer/orders");
        request.setCookies(new Cookie(CustomerSessionCookieFilter.COOKIE_NAME, "cust_abc"));
        AtomicReference<String> seenToken = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain(new jakarta.servlet.http.HttpServlet() {
            @Override
            protected void service(jakarta.servlet.http.HttpServletRequest req, jakarta.servlet.http.HttpServletResponse res) {
                seenToken.set(req.getHeader("X-Customer-Token"));
            }
        });
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertThat(seenToken.get()).isEqualTo("cust_abc");
    }

    @Test
    void unsafeMethodWithCookieRequiresCsrfHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/customer/ticket-orders");
        request.setCookies(new Cookie(CustomerSessionCookieFilter.COOKIE_NAME, "cust_abc"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("CSRF_HEADER_REQUIRED");
    }

    @Test
    void unsafeMethodWithCookieAndCsrfHeaderPasses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/customer/ticket-orders");
        request.setCookies(new Cookie(CustomerSessionCookieFilter.COOKIE_NAME, "cust_abc"));
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        AtomicReference<String> seenToken = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain(new jakarta.servlet.http.HttpServlet() {
            @Override
            protected void service(jakarta.servlet.http.HttpServletRequest req, jakarta.servlet.http.HttpServletResponse res) {
                seenToken.set(req.getHeader("X-Customer-Token"));
            }
        });
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertThat(seenToken.get()).isEqualTo("cust_abc");
    }

    @Test
    void explicitHeaderTokenWinsOverCookieAndSkipsCsrfGate() throws Exception {
        // 头 token 调用方（小程序/App）不受 cookie CSRF 闸影响，cookie 不覆盖显式头。
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/customer/ticket-orders");
        request.setCookies(new Cookie(CustomerSessionCookieFilter.COOKIE_NAME, "cust_cookie"));
        request.addHeader("X-Customer-Token", "cust_header");
        AtomicReference<String> seenToken = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain(new jakarta.servlet.http.HttpServlet() {
            @Override
            protected void service(jakarta.servlet.http.HttpServletRequest req, jakarta.servlet.http.HttpServletResponse res) {
                seenToken.set(req.getHeader("X-Customer-Token"));
            }
        });
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertThat(seenToken.get()).isEqualTo("cust_header");
    }

    @Test
    void nonCustomerPathUntouched() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/ticketing/verifications");
        request.setCookies(new Cookie(CustomerSessionCookieFilter.COOKIE_NAME, "cust_abc"));
        AtomicReference<String> seenToken = new AtomicReference<>("unset");
        MockFilterChain chain = new MockFilterChain(new jakarta.servlet.http.HttpServlet() {
            @Override
            protected void service(jakarta.servlet.http.HttpServletRequest req, jakarta.servlet.http.HttpServletResponse res) {
                seenToken.set(req.getHeader("X-Customer-Token"));
            }
        });
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(seenToken.get()).isNull();
    }
}
