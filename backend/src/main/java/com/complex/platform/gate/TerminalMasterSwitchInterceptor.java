package com.complex.platform.gate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 3.19 二期 · 终端总开关逐请求后端强制（业务 HTML「终端开关」节 + PRD §6.3 后端拒绝铁律）：
 * 凡声明了 {@code X-Terminal-Type} 的请求，一律先过 {@link TerminalSwitchGuard} 的
 * {@code terminal.enabled} 闸——矩阵页关掉某端/某模块后，该端的业务接口被后端硬拒
 * （403 TERMINAL_DISABLED + terminal.access_rejected 留痕），不再只是前端不可见。
 * scope 维度（module/device/merchant/project…）由 guard 按请求头/令牌解析，最具体行胜出；
 * 开关读取异常 fail-closed 拒绝。
 *
 * <p>未声明终端头的调用（第三方 webhook、测试、历史脚本）不进此闸——终端开关是对
 * 受管一方端的运营停用开关，不是鉴权边界（权限校验仍独立生效）。
 *
 * <p>自救豁免：登录与开关管理路径不受总开关限制，否则关掉 web 端后运营自己也进不去
 * 矩阵页重新打开（死锁）。
 */
@Component
public class TerminalMasterSwitchInterceptor implements HandlerInterceptor {

    static final String MASTER_CAPABILITY = TerminalSwitchGuard.MASTER_CAPABILITY;

    /** 自救豁免前缀：登录/破窗 + 开关查看与管理（含 effective 只读）。 */
    private static final List<String> EXEMPT_PREFIXES = List.of(
            "/api/v1/auth/",
            "/api/v1/terminals/capability-switches"
    );

    // ObjectProvider：@WebMvcTest 切片自动纳入 HandlerInterceptor 但不装 gate 依赖，guard 缺席时跳过闸；
    // 完整应用上下文中 TerminalSwitchGuard 恒存在（@Component），生产不会走缺席分支。
    private final org.springframework.beans.factory.ObjectProvider<TerminalSwitchGuard> terminalSwitchGuard;

    public TerminalMasterSwitchInterceptor(
            org.springframework.beans.factory.ObjectProvider<TerminalSwitchGuard> terminalSwitchGuard) {
        this.terminalSwitchGuard = terminalSwitchGuard;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String terminalType = request.getHeader("X-Terminal-Type");
        if (terminalType == null || terminalType.isBlank()) {
            return true;
        }
        String uri = request.getRequestURI();
        for (String prefix : EXEMPT_PREFIXES) {
            if (uri != null && uri.startsWith(prefix)) {
                return true;
            }
        }
        TerminalSwitchGuard guard = terminalSwitchGuard.getIfAvailable();
        if (guard != null) {
            // 关/读取失败 → guard 抛 TERMINAL_DISABLED（DispatcherServlet 内，GlobalExceptionHandler 统一 403 JSON）。
            // 总开关 AND 语义:global 行关闭即硬拒,不被模块/设备行覆盖(商户专区活证据揭出的缺陷)。
            guard.checkTerminalEnabled();
        }
        return true;
    }
}
