package com.complex.platform.gate;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * AUD-010 ④ 仅对「设备绑定型 + 写 + 带 X-Device-Signature」的请求把 body 缓存进 {@link CachedBodyHttpServletRequest}，
 * 供验签取 body 摘要。其余请求透传、零改动——不做全局 body 缓存，避免影响上传/流式/大 body。
 * 排在最前（早于 Security 与 DispatcherServlet），确保拦截器/Controller 拿到同一个可重复读的 body。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DeviceCredentialBodyCachingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (needsBodyCache(request)) {
            chain.doFilter(new CachedBodyHttpServletRequest(request), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    private static boolean needsBodyCache(HttpServletRequest request) {
        String type = request.getHeader("X-Terminal-Type");
        if (type == null || !DeviceCredentialVerifier.DEVICE_BOUND_TYPES.contains(type.trim())) {
            return false;
        }
        String sig = request.getHeader("X-Device-Signature");
        if (sig == null || sig.isBlank()) {
            return false;
        }
        String m = request.getMethod() == null ? "" : request.getMethod().toUpperCase(Locale.ROOT);
        return !("GET".equals(m) || "HEAD".equals(m) || "OPTIONS".equals(m));
    }
}
