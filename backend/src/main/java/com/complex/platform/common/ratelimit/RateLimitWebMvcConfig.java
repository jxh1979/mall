package com.complex.platform.common.ratelimit;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 把限速拦截器挂到全部 API 路径（具体匹配与放行在拦截器内判定）。
 * ObjectProvider：@WebMvcTest 等轻量切片不装限速组件时静默跳过（对齐 {@code GateWebMvcConfig} / SecurityConfig 范式）。
 */
@Configuration
public class RateLimitWebMvcConfig implements WebMvcConfigurer {

    private final ObjectProvider<RateLimitInterceptor> rateLimitInterceptor;

    public RateLimitWebMvcConfig(ObjectProvider<RateLimitInterceptor> rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        rateLimitInterceptor.ifAvailable(interceptor ->
                registry.addInterceptor(interceptor).addPathPatterns("/api/**"));
    }
}
