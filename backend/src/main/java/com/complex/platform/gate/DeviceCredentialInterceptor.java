package com.complex.platform.gate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * AUD-010 每请求早期跑设备凭证验签({@link DeviceCredentialVerifier}),把服务端派生的可信终端身份存入
 * {@link TerminalIdentityHolder} 供下游终端开关/入场闸/资金闸统一读取。设备绑定型写请求在 enforce/无效签名下
 * 由 verifier 抛 403(DispatcherServlet 内经 GlobalExceptionHandler 统一 JSON),与终端总开关拦截器同范式。
 * 必须排在 {@link TerminalMasterSwitchInterceptor} 之前,使总开关按服务端派生类型判定。
 */
@Component
public class DeviceCredentialInterceptor implements HandlerInterceptor {

    // ObjectProvider:@WebMvcTest 切片不装 gate 依赖时静默跳过(对齐总开关拦截器)。
    private final ObjectProvider<DeviceCredentialVerifier> verifier;

    public DeviceCredentialInterceptor(ObjectProvider<DeviceCredentialVerifier> verifier) {
        this.verifier = verifier;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        DeviceCredentialVerifier v = verifier.getIfAvailable();
        if (v != null) {
            // 设备绑定型写请求缺/坏凭证在 enforce 下抛 DEVICE_CREDENTIAL_INVALID(403);其余存放行态。
            TerminalIdentityHolder.set(request, v.verify(request));
        }
        return true;
    }
}
