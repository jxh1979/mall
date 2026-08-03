package com.complex.platform.gate;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * AUD-010 ④ 仅对「写 + 带 X-Device-Signature」的请求把 body 缓存进 {@link CachedBodyHttpServletRequest}，
 * 供验签取 body 摘要。其余请求透传、零改动——不做全局 body 缓存。排在最前（早于 Security 与 DispatcherServlet），
 * 确保拦截器/Controller 拿到同一个可重复读的 body。
 *
 * <p>2026-08-03：删去原先的「自报 X-Terminal-Type 属设备绑定型」前置条件。终端类型现由服务端按
 * {@code X-Device-Id} 反查登记表派生（见 {@link DeviceCredentialVerifier}），过滤器跑在拦截器之前拿不到该结果；
 * 若仍按自报头筛，客户端只要不发头就拿不到缓存 body，验签必然摘要不符——签名请求反而被自己挡掉。
 * 「带签名头」本身已是足够窄的条件，且体积上限不变，不扩大预认证攻击面。
 *
 * <p>预认证 DoS 防护：仅在需缓存时才处理，且先按 Content-Length 早拒超 {@link CachedBodyHttpServletRequest#MAX_BODY_BYTES}
 * 的体（413），无 Content-Length 时由 wrapper 的有上限读取兜底（截断→验签失败）。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DeviceCredentialBodyCachingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (needsBodyCache(request)) {
            if (request.getContentLengthLong() > CachedBodyHttpServletRequest.MAX_BODY_BYTES) {
                response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                        "request body too large for a signed device request");
                return;
            }
            chain.doFilter(new CachedBodyHttpServletRequest(request), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    private static boolean needsBodyCache(HttpServletRequest request) {
        String sig = request.getHeader("X-Device-Signature");
        if (sig == null || sig.isBlank()) {
            return false;
        }
        return DeviceCredentialVerifier.isWrite(request.getMethod());
    }
}
