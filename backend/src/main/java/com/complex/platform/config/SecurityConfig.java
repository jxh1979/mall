package com.complex.platform.config;

import com.complex.platform.auth.BearerAuthenticationFilter;
import com.complex.platform.auth.CustomerTokenAuthenticationFilter;
import com.complex.platform.auth.ProductionBearerAuthenticationFilter;
import org.springframework.beans.factory.ObjectProvider;
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

    private final ObjectProvider<BearerAuthenticationFilter> bearerAuthenticationFilter;
    private final ObjectProvider<ProductionBearerAuthenticationFilter> productionBearerAuthenticationFilter;
    private final ObjectProvider<CustomerTokenAuthenticationFilter> customerTokenAuthenticationFilter;
    private final Environment environment;

    public SecurityConfig(
            ObjectProvider<BearerAuthenticationFilter> bearerAuthenticationFilter,
            ObjectProvider<ProductionBearerAuthenticationFilter> productionBearerAuthenticationFilter,
            ObjectProvider<CustomerTokenAuthenticationFilter> customerTokenAuthenticationFilter,
            Environment environment
    ) {
        this.bearerAuthenticationFilter = bearerAuthenticationFilter;
        this.productionBearerAuthenticationFilter = productionBearerAuthenticationFilter;
        this.customerTokenAuthenticationFilter = customerTokenAuthenticationFilter;
        this.environment = environment;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
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
