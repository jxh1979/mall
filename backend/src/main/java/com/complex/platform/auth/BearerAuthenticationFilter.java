package com.complex.platform.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile({"local", "dev", "test"})
public class BearerAuthenticationFilter extends OncePerRequestFilter {

    private final AuthService authService;

    public BearerAuthenticationFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // 双通道令牌来源：设备端（安卓/一体机）走 Authorization: Bearer 头；后台 Web 走 HttpOnly Cookie。
        // 头优先——保证「员工代客」等设备端语义不变；Cookie 仅在无 Bearer 头时兜底。
        String token = null;
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring("Bearer ".length()).trim();
        } else {
            token = AuthCookies.readAccessToken(request);
        }
        if (token != null && !token.isBlank()) {
            authService.authenticateToken(token).ifPresent(user -> {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_P0_SMOKE"))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
            authService.attachSession(request, token);
        }
        filterChain.doFilter(request, response);
    }
}
