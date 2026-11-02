package com.complex.platform.gate;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UrlPathHelper;

/**
 * 3.19 二期 · 终端总开关逐请求后端强制（业务 HTML「终端开关」节 + PRD §6.3 后端拒绝铁律）：
 * 业务请求先过 {@link TerminalSwitchGuard} 的 {@code terminal.enabled} 闸——矩阵页关掉某端/某模块后，
 * 该端的业务接口被后端硬拒（403 TERMINAL_DISABLED + terminal.access_rejected 留痕），不再只是前端不可见。
 * scope 维度（module/device/merchant/project…）由 guard 解析，取最严；开关读取异常 fail-closed 拒绝。
 * Web 前端与商户网页门户的 module 维度由服务端按所调接口定（第 100 条 100-R1，{@link ServerModuleRoutes}），不看自报 X-Module-Code。
 *
 * <p><b>2026-08-03 收口（治理地图「关闭后菜单不可见或不可点，接口也要拒绝业务动作」「开关拉取失败时按
 * 兜底关闭处理，写操作一律拒绝；前端隐藏只是辅助，后端接口必须独立校验」）</b>：终端类型改取
 * {@link DeviceCredentialInterceptor} 派生并存入 {@link TerminalIdentityHolder} 的服务端可信身份，
 * 不再读客户端自报的 {@code X-Terminal-Type} 头。旧实现「无自报头即放行」等于任何客户端删掉一个请求头
 * 就跳过整条 terminal.enabled 闸（含 fail-closed 兜底）——{@code checkTerminalEnabled()} 全仓仅此一个
 * 生产调用点，跳过拦截器即整条闸不跑。
 *
 * <p><b>写请求 fail-closed</b>：解析不出终端身份的非 GET/HEAD/OPTIONS 请求一律 403 TERMINAL_NOT_ALLOWED。
 * 读请求维持放行——终端开关是对受管一方端的运营停用开关（不是鉴权边界，权限校验仍独立生效），
 * 前端隐藏为辅助，读放行不产生业务副作用。
 *
 * <p>豁免前缀两类：①<b>自救</b>——登录与开关管理路径不受总开关限制，否则关掉 web 端后运营自己也进不去
 * 矩阵页重新打开（死锁）；②<b>非终端通道</b>——第三方开放平台回调、一体机自助端点不是「受管一方端」，
 * 各有自己的签名 / X-Device-Token 认证，且一体机路径同样豁免于 {@link DeviceCredentialInterceptor}
 * （拿不到可信身份），不进本闸，否则第三方回调与一体机心跳会被写请求 fail-closed 误杀。
 *
 * <p>第十八批 M 段第 116 条（PGM:747「……员工自有手机只开放低风险功能……」；PGM:744；PGM:738「访问被拒绝也要留痕……」）：可信终端是员工私人手机时，
 * 只放行 {@link StaffAppFunctions} 私人手机清单内的接口，读写都判；清单外的先留痕 {@code private_phone_function_not_allowed} 再 403 TERMINAL_NOT_ALLOWED。
 * 豁免前缀照旧先放行。
 */
@Component
public class TerminalMasterSwitchInterceptor implements HandlerInterceptor {

    static final String MASTER_CAPABILITY = TerminalSwitchGuard.MASTER_CAPABILITY;

    /** 自救豁免前缀：登录/破窗 + 开关查看与管理（含 effective 只读）；非终端通道见下方注释。 */
    private static final List<String> EXEMPT_PREFIXES = List.of(
            "/api/v1/auth/",
            "/api/v1/terminals/capability-switches",
            // 非终端通道（2026-08-03 随写请求 fail-closed 一并登记，否则被新增的 403 误杀）：
            "/api/open/",               // 第三方开放平台回调，自带签名验证，不是受管终端
            "/api/v1/terminals/kiosks/", // 一体机自助端点，X-Device-Token 认证，亦豁免于设备凭证拦截器
            // U05-003/U07-004：公开自助注册入口——私人手机此时尚未成为受管终端，身份证明是请求体里的设备公钥/指纹（服务端校验 + 频控）。
            "/api/v1/staff/self-registrations",
            "/api/v1/staff/registration-companies",
            // 第 23 条 23-R2：打开页面的暗纹与留痕（PGM:687「这条留痕不提供关闭开关」；DEC-R3B-2026-08-04-14「兜底关闭不含留痕的写入」）。
            // 本拦截器对读请求放行，开关关掉或读取失败兜底关闭时页面照样出数据；page-views 是写请求，被拒就成了「页面照常显示但没有暗纹、没有打开留痕」。
            "/api/v1/security/page-views"
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
        TerminalSwitchGuard guard = terminalSwitchGuard.getIfAvailable();
        if (guard == null) {
            // gate 组件未装（@WebMvcTest 等轻量切片）：没有 DeviceCredentialInterceptor 派生身份、也没有开关仓储，
            // 整闸不生效（含下面的写请求 fail-closed）。生产恒有 @Component，不会走本分支。
            return true;
        }
        String uri = request.getRequestURI();
        for (String prefix : EXEMPT_PREFIXES) {
            if (uri != null && uri.startsWith(prefix)) {
                return true;
            }
        }
        String terminalType = TerminalIdentityHolder.of(request)
                .map(TerminalIdentityHolder.VerifiedTerminal::terminalType)
                .filter(type -> type != null && !type.isBlank())
                .orElse(null);
        if (terminalType == null) {
            if (DeviceCredentialVerifier.isWrite(request.getMethod())) {
                throw ApiException.forbidden("TERMINAL_NOT_ALLOWED",
                        "无法识别终端身份，业务写操作已被拒绝（终端开关兜底关闭）");
            }
            return true;
        }
        if (DeviceCredentialVerifier.STAFF_PRIVATE_PHONE.equals(terminalType) && !StaffAppFunctions.privatePhoneAllows(
                request.getMethod(), UrlPathHelper.defaultInstance.getPathWithinApplication(request))) {
            guard.auditRejection(terminalType, null, "private_phone_function_not_allowed", null, null);
            throw ApiException.forbidden("TERMINAL_NOT_ALLOWED", "员工自有手机只开放低风险功能，此操作请在公司工作手机上办理");
        }
        // 关/读取失败 → guard 抛 TERMINAL_DISABLED（DispatcherServlet 内，GlobalExceptionHandler 统一 403 JSON）。
        // 总开关 AND 语义:global 行关闭即硬拒,不被模块/设备行覆盖(商户专区活证据揭出的缺陷)。
        // guard 内部经 GateRequestContext 同样优先读 TerminalIdentityHolder,与上面的判定同源。
        guard.checkTerminalEnabled();
        return true;
    }
}
