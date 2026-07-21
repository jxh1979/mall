package com.complex.platform.gate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AUD-010 服务端派生终端身份的请求级载体。{@link DeviceCredentialInterceptor} 在每请求早期把经设备凭证验签
 * 派生出的可信终端类型/设备 id 存入请求属性,下游 {@link GateRequestContext}(终端开关/入场闸)与
 * ServiceOrderController(资金四路)统一读它,不再各自信任可伪造的 {@code X-Terminal-Type} 请求头。
 *
 * <p>{@link VerifiedTerminal#verified()} = true 表示该终端类型来自通过验签的设备凭证(服务端真相);
 * false 表示放行态(非设备绑定型终端 web/app,或灰度期未登记设备)——此时 terminalType 仍取自报头值(残留)。
 */
public final class TerminalIdentityHolder {

    static final String ATTR = "com.complex.platform.gate.verified_terminal";

    private TerminalIdentityHolder() {
    }

    /**
     * @param terminalType 服务端已知的最佳终端类型(验签成功=设备登记类型;放行态=自报类型)
     * @param deviceId     设备 id(可空)
     * @param verified     该终端类型是否经设备凭证验签得出
     */
    public record VerifiedTerminal(String terminalType, String deviceId, boolean verified) {
    }

    static void set(HttpServletRequest request, VerifiedTerminal terminal) {
        request.setAttribute(ATTR, terminal);
    }

    /** 当前请求已派生的可信终端身份;无请求上下文/拦截器未运行时为空。 */
    public static Optional<VerifiedTerminal> current() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return Optional.empty();
        }
        Object attr = request.getAttribute(ATTR);
        return attr instanceof VerifiedTerminal vt ? Optional.of(vt) : Optional.empty();
    }

    /** 可信终端类型:有派生结果且非空则取之,否则回退到传入的自报值。 */
    public static String trustedTerminalType(String claimedFallback) {
        return current()
                .map(VerifiedTerminal::terminalType)
                .filter(t -> t != null && !t.isBlank())
                .orElse(claimedFallback);
    }

    private static HttpServletRequest currentRequest() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                return attrs.getRequest();
            }
        } catch (RuntimeException ignored) {
            // no request context (async/test) → null
        }
        return null;
    }
}
