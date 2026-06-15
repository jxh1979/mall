package com.complex.platform.config;

import com.complex.platform.auth.BearerAuthenticationFilter;
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
    private final Environment environment;

    public SecurityConfig(
            ObjectProvider<BearerAuthenticationFilter> bearerAuthenticationFilter,
            ObjectProvider<ProductionBearerAuthenticationFilter> productionBearerAuthenticationFilter,
            Environment environment
    ) {
        this.bearerAuthenticationFilter = bearerAuthenticationFilter;
        this.productionBearerAuthenticationFilter = productionBearerAuthenticationFilter;
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
                .formLogin(AbstractHttpConfigurer::disable);

        bearerAuthenticationFilter.ifAvailable(filter ->
                http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class));
        productionBearerAuthenticationFilter.ifAvailable(filter ->
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
