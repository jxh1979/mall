package com.complex.platform.config;

import com.complex.platform.auth.BearerAuthenticationFilter;
import com.complex.platform.auth.CustomerTokenAuthenticationFilter;
import com.complex.platform.auth.DoubleSubmitCsrfFilter;
import com.complex.platform.auth.ProductionBearerAuthenticationFilter;
import com.complex.platform.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    // 入口点自建 mapper（不依赖容器 ObjectMapper，测试切片未必装配 Jackson）：与全局 Jackson 配置对齐
    // （SNAKE_CASE + ISO 时间），保证 401 信封字段名与业务响应一致（detail_code/request_id/...）。
    private static final ObjectMapper UNAUTHORIZED_ENVELOPE_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private final ObjectProvider<BearerAuthenticationFilter> bearerAuthenticationFilter;
    private final ObjectProvider<ProductionBearerAuthenticationFilter> productionBearerAuthenticationFilter;
    private final ObjectProvider<CustomerTokenAuthenticationFilter> customerTokenAuthenticationFilter;
    private final ObjectProvider<DoubleSubmitCsrfFilter> doubleSubmitCsrfFilter;
    private final Environment environment;

    public SecurityConfig(
            ObjectProvider<BearerAuthenticationFilter> bearerAuthenticationFilter,
            ObjectProvider<ProductionBearerAuthenticationFilter> productionBearerAuthenticationFilter,
            ObjectProvider<CustomerTokenAuthenticationFilter> customerTokenAuthenticationFilter,
            ObjectProvider<DoubleSubmitCsrfFilter> doubleSubmitCsrfFilter,
            Environment environment
    ) {
        this.bearerAuthenticationFilter = bearerAuthenticationFilter;
        this.productionBearerAuthenticationFilter = productionBearerAuthenticationFilter;
        this.customerTokenAuthenticationFilter = customerTokenAuthenticationFilter;
        this.doubleSubmitCsrfFilter = doubleSubmitCsrfFilter;
        this.environment = environment;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Spring 内置 CSRF 关闭：本系统鉴权是自建 Bearer/Cookie 令牌（非 Spring Session），
                // 改由 DoubleSubmitCsrfFilter 精确按「是否后台 Web Cookie 会话」做双提交校验（设备端 Bearer 免疫、零影响）。
                .csrf(csrf -> csrf.disable())
                // 会话失效统一 401：无 token / token 伪造 / token 过期一律 UNAUTHORIZED（此前无入口点时
                // Spring 默认回 403 空 body，前端误译为"当前账号无权进行此操作"且无法触发重新登录）。
                // 真权限不足（已认证账号越权）仍由应用层 ApiException.forbidden 返回 403 FORBIDDEN，不受影响。
                .exceptionHandling(handling -> handling.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    UNAUTHORIZED_ENVELOPE_MAPPER.writeValue(response.getWriter(), ApiResponse.error(
                            "UNAUTHORIZED",
                            "AUTH_SESSION_INVALID",
                            "authentication required: token is missing, invalid or expired"));
                }))
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(publicApiPaths()).permitAll();
                    if (!environment.acceptsProfiles(Profiles.of("prod"))) {
                        authorize.requestMatchers(apiDocumentationPaths()).permitAll();
                    }
                    authorize.anyRequest().authenticated();
                })
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                // 安全响应头（纵深防御）：CSP 收紧脚本源、frame-ancestors none 防点击劫持、nosniff 防 MIME 嗅探。
                // SPA 文档由前端静态托管方另发 CSP meta；此处覆盖后端直返的任何内容。
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(withDefaults -> {})
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
                                        + "img-src 'self' data:; font-src 'self' data:; connect-src 'self'; "
                                        + "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'")));

        // CSRF 双提交校验：置于鉴权过滤器之前，非法写请求早退（403），不进业务链。
        doubleSubmitCsrfFilter.ifAvailable(filter ->
                http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class));

        bearerAuthenticationFilter.ifAvailable(filter ->
                http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class));
        productionBearerAuthenticationFilter.ifAvailable(filter ->
                http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class));
        // #14 钱包双路径：客人 token 过滤器在员工 Bearer 过滤器之后注册——仅当员工未认证时才写入客人主体，
        // 保证"员工代客操作"时保留员工主体走 @GuardedAction 提成归属。
        customerTokenAuthenticationFilter.ifAvailable(filter ->
                http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class));

        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }

    private String[] publicApiPaths() {
        return new String[]{
                "/api/v1/auth/login",
                "/api/v1/auth/refresh",
                "/api/v1/auth/master/**",
                "/api/v1/customer/**",
                "/api/open/**",
                "/api/v1/terminals/kiosks/*/config",
                "/api/v1/terminals/kiosks/*/heartbeat",
                "/api/v1/system/health",
                "/api/v1/system/version",
                "/actuator/health",
                "/actuator/info"
        };
    }

    private String[] apiDocumentationPaths() {
        return new String[]{
                "/docs",
                "/docs/**",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs",
                "/v3/api-docs/**"
        };
    }
}
