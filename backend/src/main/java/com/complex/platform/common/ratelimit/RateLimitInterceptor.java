package com.complex.platform.common.ratelimit;

import com.complex.platform.common.api.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 敏感端点应用层限速（按来源 IP）：登录/刷新/手机登录、支付、退款——同一 IP 超过阈值即
 * 429 {@code RATE_LIMITED} + {@code Retry-After}（信封与头由 {@link com.complex.platform.common.api.GlobalExceptionHandler} 统一）。
 *
 * <p>与既有「账号锁定」互补而不冲突：账号锁定（{@code ProductionAuthService}，DB 持久）按同一凭证连续失败次数封号，
 * 此处按请求频率即时收敛，专防单 IP 高频刷（含撞库/枚举）；两者错误码不同（{@code ACCOUNT_LOCKED} 401 vs
 * {@code RATE_LIMITED} 429），互不触发。账号/手机号维度在对应服务内单独判定（OTP 撞码见 {@code CustomerService}）。
 *
 * <p>ObjectProvider：@WebMvcTest 等切片自动纳入 HandlerInterceptor 但不装限速依赖（RateLimiter/属性 @Component），
 * 依赖缺席时逐请求放行（对齐 {@code TerminalMasterSwitchInterceptor} 范式）；完整上下文中依赖恒存在。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private enum Category { AUTH, PAYMENT, REFUND }

    private record Rule(String pattern, Category category) {}

    // 覆盖清单：鉴权 + money-movement（支付/退款）端点；其余高危写按需追加即可。路径用 Ant 风格（* 匹配单段）。
    private static final List<Rule> RULES = List.of(
            new Rule("/api/v1/auth/login", Category.AUTH),
            new Rule("/api/v1/auth/refresh", Category.AUTH),
            // U07-004：公开自助注册入口按认证类频控（PGM L579 设备绑定契约落地前后均须防刷）。
            new Rule("/api/v1/staff/self-registrations", Category.AUTH),
            new Rule("/api/v1/customer/auth/phone-login", Category.AUTH),
            new Rule("/api/v1/finance/payment-intents", Category.PAYMENT),
            new Rule("/api/v1/finance/payments/*/confirm-offline", Category.PAYMENT),
            new Rule("/api/v1/finance/ar-ap-items/*/payments", Category.PAYMENT),
            new Rule("/api/v1/customer/stored-value/topup", Category.PAYMENT),
            new Rule("/api/v1/services/orders/*/mark-paid", Category.PAYMENT),
            new Rule("/api/v1/services/payment-code", Category.PAYMENT),
            new Rule("/api/v1/ticketing/orders/*/pay-intents", Category.PAYMENT),
            new Rule("/api/v1/ticketing/orders/*/confirm-payment", Category.PAYMENT),
            new Rule("/api/v1/finance/refunds", Category.REFUND),
            new Rule("/api/v1/ticketing/refunds", Category.REFUND),
            new Rule("/api/v1/ticketing/refunds/*/approve", Category.REFUND));

    private final ObjectProvider<RateLimiter> rateLimiter;
    private final ObjectProvider<RateLimitProperties> properties;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public RateLimitInterceptor(ObjectProvider<RateLimiter> rateLimiter, ObjectProvider<RateLimitProperties> properties) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        RateLimiter limiter = rateLimiter.getIfAvailable();
        RateLimitProperties config = properties.getIfAvailable();
        if (limiter == null || config == null || !config.isEnabled()) {
            return true;
        }
        String method = request.getMethod();
        // 只限状态变更请求；只读不进限速（这些端点均 POST，GET/HEAD/OPTIONS 直接放行）。
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            return true;
        }
        Category category = categoryOf(request.getRequestURI());
        if (category == null) {
            return true;
        }
        RateLimitProperties.Rule rule = ruleFor(config, category);
        String key = "ip:" + category + ":" + clientIp(request);
        RateLimiter.Decision decision = limiter.tryAcquire(key, rule.getLimit(), rule.getWindow());
        if (!decision.allowed()) {
            throw ApiException.tooManyRequests("RATE_LIMITED",
                    "请求过于频繁，请稍后再试", decision.retryAfterSeconds());
        }
        return true;
    }

    private Category categoryOf(String uri) {
        if (uri == null) {
            return null;
        }
        for (Rule r : RULES) {
            if (matcher.match(r.pattern(), uri)) {
                return r.category();
            }
        }
        return null;
    }

    private RateLimitProperties.Rule ruleFor(RateLimitProperties config, Category category) {
        return switch (category) {
            case AUTH -> config.getAuthIp();
            case PAYMENT -> config.getPaymentIp();
            case REFUND -> config.getRefundIp();
        };
    }

    /**
     * 取真实客户端 IP：nginx 反代注入 {@code X-Real-IP}=直连客户端、{@code X-Forwarded-For} 追加链；
     * 优先可信头，回落 {@code remoteAddr}。
     * ponytail: 信任本仓 nginx 注入的头；后端仅经 nginx 暴露，直连绕过属部署边界问题，另管。
     */
    static String clientIp(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int comma = forwardedFor.indexOf(',');
            return (comma > 0 ? forwardedFor.substring(0, comma) : forwardedFor).trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "unknown" : remote;
    }
}
