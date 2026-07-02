package com.complex.platform.gate;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 3.19 二期：把终端总开关拦截器挂到全部 API 路径（豁免与免闸条件在拦截器内判定）。
 * ObjectProvider：@WebMvcTest 等轻量切片不装 gate 组件时静默跳过（对齐 SecurityConfig 过滤器范式）。
 */
@Configuration
public class GateWebMvcConfig implements WebMvcConfigurer {

    private final ObjectProvider<TerminalMasterSwitchInterceptor> terminalMasterSwitchInterceptor;

    public GateWebMvcConfig(ObjectProvider<TerminalMasterSwitchInterceptor> terminalMasterSwitchInterceptor) {
        this.terminalMasterSwitchInterceptor = terminalMasterSwitchInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        terminalMasterSwitchInterceptor.ifAvailable(interceptor ->
                registry.addInterceptor(interceptor).addPathPatterns("/api/**"));
    }
}
