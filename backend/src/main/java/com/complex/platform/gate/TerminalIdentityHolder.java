package com.complex.platform.gate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AUD-010 服务端派生终端身份的请求级载体。{@link DeviceCredentialInterceptor} 在每请求早期把经设备凭证验签
 * 派生出的可信终端类型/设备 id 存入请求属性,下游 {@link GateRequestContext}(终端开关/入场闸)、
 * {@link TerminalMasterSwitchInterceptor}(终端总开关)与 ServiceOrderController(资金四路)统一读它,
 * 不再各自信任可伪造的 {@code X-Terminal-Type} 请求头。
 *
 * <p>{@link VerifiedTerminal#verified()} = true 表示该终端类型来自通过验签的设备凭证(服务端真相);
 * false 表示放行态(非设备绑定型终端 web/app,或灰度期未登记设备)——此时 terminalType 取服务端按
 * {@code X-Device-Id} 反查 {@code terminal_devices} 的登记类型,查不到才回落自报头值(残留)。
 */
public final class TerminalIdentityHolder {

    static final String ATTR = "com.complex.platform.gate.verified_terminal";

    private TerminalIdentityHolder() {
    }

    /**
     * @param terminalType 服务端已知的最佳终端类型(验签成功=设备登记类型;放行态=登记类型或自报类型)
     * @param deviceId     设备 id(可空)
     * @param verified     该终端类型是否经设备凭证验签得出
     */
    public record VerifiedTerminal(String terminalType, String deviceId, boolean verified) {
    }

    static void set(HttpServletRequest request, VerifiedTerminal terminal) {
        request.setAttribute(ATTR, terminal);
    }

    /** 指定请求上已派生的可信终端身份;拦截器未运行时为空。拦截器/过滤器手上有 request，走这条不依赖线程上下文。 */
    static Optional<VerifiedTerminal> of(HttpServletRequest request) {
        Object attr = request == null ? null : request.getAttribute(ATTR);
        return attr instanceof VerifiedTerminal vt ? Optional.of(vt) : Optional.empty();
    }

    /** 当前请求已派生的可信终端身份;无请求上下文/拦截器未运行时为空。 */
    public static Optional<VerifiedTerminal> current() {
        return of(currentRequest());
    }

    /** 可信终端类型:有派生结果且非空则取之,否则回退到传入的自报值。 */
    public static String trustedTerminalType(String claimedFallback) {
        return current()
                .map(VerifiedTerminal::terminalType)
                .filter(t -> t != null && !t.isBlank())
                .orElse(claimedFallback);
    }

    /**
     * 第十八批 N 段第 122 条 122-R1（BSM:940、PGM:745）：操作留痕终端列取当前请求的可信终端；拦截器豁免的路径（一体机自助、
     * 员工自助注册）取请求自报的 {@code X-Terminal-Type}；没有请求或没有终端信息时为空。超长截到列宽 64。
     */
    public static String auditTerminal() {
        HttpServletRequest request = currentRequest();
        String terminal = trustedTerminalType(request == null ? null : request.getHeader("X-Terminal-Type"));
        if (terminal == null || terminal.isBlank()) {
            return null;
        }
        terminal = terminal.trim();
        return terminal.length() > 64 ? terminal.substring(0, 64) : terminal;
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
