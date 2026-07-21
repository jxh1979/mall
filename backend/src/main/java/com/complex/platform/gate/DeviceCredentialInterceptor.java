package com.complex.platform.gate;

import com.complex.platform.common.api.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * AUD-010 每请求早期跑设备凭证验签（{@link DeviceCredentialVerifier}），把服务端派生的可信终端身份存入
 * {@link TerminalIdentityHolder}；验签通过（verified）后消费 nonce（{@link NonceStore}）防重放（③）。
 * 设备绑定型写请求在 enforce/无效签名下由 verifier 抛 403；重放抛 DEVICE_CREDENTIAL_REPLAY。必须排在
 * {@link TerminalMasterSwitchInterceptor} 之前，使总开关按服务端派生类型判定。
 *
 * <p>豁免前缀：kiosk 自助端点（config/heartbeat）有自己的 {@code X-Device-Token} 认证，不走本闸（否则 enforce
 * 下无签名的心跳会被 403 误伤）。
 */
@Component
public class DeviceCredentialInterceptor implements HandlerInterceptor {

    private static final List<String> EXEMPT_PREFIXES = List.of("/api/v1/terminals/kiosks/");

    private final ObjectProvider<DeviceCredentialVerifier> verifier;
    private final ObjectProvider<NonceStore> nonceStore;

    public DeviceCredentialInterceptor(ObjectProvider<DeviceCredentialVerifier> verifier,
            ObjectProvider<NonceStore> nonceStore) {
        this.verifier = verifier;
        this.nonceStore = nonceStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        for (String prefix : EXEMPT_PREFIXES) {
            if (uri != null && uri.startsWith(prefix)) {
                return true;
            }
        }
        DeviceCredentialVerifier v = verifier.getIfAvailable();
        if (v == null) {
            return true;
        }
        TerminalIdentityHolder.VerifiedTerminal vt = v.verify(request); // 抛 403 on enforce/invalid
        if (vt.verified()) {
            consumeNonceOrReject(request);
        }
        TerminalIdentityHolder.set(request, vt);
        return true;
    }

    private void consumeNonceOrReject(HttpServletRequest request) {
        NonceStore store = nonceStore.getIfAvailable();
        if (store == null) {
            return;
        }
        String deviceId = trim(request.getHeader("X-Device-Id"));
        String nonce = trim(request.getHeader("X-Device-Nonce"));
        if (deviceId != null && nonce != null && !store.tryConsume(deviceId, nonce)) {
            throw ApiException.forbidden("DEVICE_CREDENTIAL_REPLAY", "device request nonce already used");
        }
    }

    private static String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
