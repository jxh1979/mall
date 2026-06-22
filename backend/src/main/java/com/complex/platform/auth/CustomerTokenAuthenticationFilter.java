package com.complex.platform.auth;

import com.complex.platform.access.AccessActor;
import com.complex.platform.customer.CustomerService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * #14 钱包双路径：把携带有效 {@code X-Customer-Token} 的客人请求解析为 SecurityContext 中的"客人 principal"
 * （{@link AccessActor} accountType=customer），使 {@code /api/v1/customer/**} 下的 {@code @GuardedAction}
 * 自助动作不再因无认证主体而抛 UNAUTHENTICATED。
 *
 * <p>安全约束：
 * <ul>
 *   <li>仅对 {@code /api/v1/customer/**} 路径生效——客人 principal 不会泄漏到员工端点（避免凭客人 token
 *       通过 {@code anyRequest().authenticated()} 访问仅靠认证保护的员工接口）。</li>
 *   <li>仅当 SecurityContext 尚无认证时才写入——若员工 Bearer 过滤器已认证为员工（代客操作路径），
 *       则保留员工主体，由 {@code @GuardedAction} 正常校验员工权限并归属提成。</li>
 *   <li>无效/过期 token 静默跳过，由后续链路按未认证处理。</li>
 * </ul>
 * 客人自助权限的实际放行由 {@code AuthorizationService.requirePermission} 的客人自助白名单桥完成。
 */
@Component
public class CustomerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String CUSTOMER_PATH_PREFIX = "/api/v1/customer/";

    // 用 ObjectProvider 懒解析：@WebMvcTest 等切片上下文可能无 CustomerService bean，
    // 此时过滤器仍可被实例化、运行时静默跳过（与 SecurityConfig 对可选过滤器的处理一致）。
    private final ObjectProvider<CustomerService> customerService;

    public CustomerTokenAuthenticationFilter(ObjectProvider<CustomerService> customerService) {
        this.customerService = customerService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String uri = request.getRequestURI();
        boolean customerPath = uri != null && uri.startsWith(CUSTOMER_PATH_PREFIX);
        boolean alreadyAuthenticated = SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated();
        CustomerService resolver = customerService.getIfAvailable();
        if (customerPath && !alreadyAuthenticated && resolver != null) {
            String customerToken = request.getHeader("X-Customer-Token");
            resolver.resolveCustomerIdQuietly(customerToken).ifPresent(customerId -> {
                AccessActor actor = new AccessActor(
                        customerId, customerId, "customer", null, null, "active");
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                actor, "customer-token", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
            });
        }
        filterChain.doFilter(request, response);
    }
}
