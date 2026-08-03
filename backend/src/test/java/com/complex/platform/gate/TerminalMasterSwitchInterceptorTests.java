package com.complex.platform.gate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.complex.platform.common.api.ApiException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 3.19 二期 + 2026-08-03 收口：终端总开关逐请求强制，按<b>服务端可信终端身份</b>（设备凭证拦截器派生）判定。
 * 治理地图「关闭后…接口也要拒绝业务动作」「开关拉取失败时按兜底关闭处理，写操作一律拒绝；
 * 前端隐藏只是辅助，后端接口必须独立校验」：解析不出终端身份的写请求一律 403，不再"没头就跳过闸"。
 */
class TerminalMasterSwitchInterceptorTests {

    private final TerminalSwitchGuard guard = mock(TerminalSwitchGuard.class);
    private final TerminalMasterSwitchInterceptor interceptor = new TerminalMasterSwitchInterceptor(
            new org.springframework.beans.factory.support.StaticListableBeanFactory(
                    java.util.Map.of("terminalSwitchGuard", guard))
                    .getBeanProvider(TerminalSwitchGuard.class));

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static MockHttpServletRequest trusted(String method, String uri, String terminalType) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        TerminalIdentityHolder.set(request,
                new TerminalIdentityHolder.VerifiedTerminal(terminalType, "dev1", true));
        return request;
    }

    @Test
    void writeWithoutTrustedTerminalIdentityIsRejected() {
        // 旧行为：POST 省略 X-Terminal-Type → 整条 terminal.enabled 闸（含 fail-closed 兜底）都不跑。
        // 新行为：解析不出可信终端身份的写请求直接 403，治理地图「写操作一律拒绝」。
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/bar/orders");
        request.addHeader("X-Terminal-Type", "web"); // 自报头不再有任何效力
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TERMINAL_NOT_ALLOWED"));
        verify(guard, never()).checkTerminalEnabled();
    }

    @Test
    void readWithoutTrustedTerminalIdentityStillSkipsGate() {
        // 读请求维持放行：终端开关是端停用开关，不是鉴权边界；前端隐藏为辅助，读不产生业务副作用。
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bar/orders");
        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        verify(guard, never()).checkTerminalEnabled();
    }

    @Test
    void nonTerminalChannelWritesAreExempt() {
        // 自救路径 + 非终端通道（第三方回调、一体机自助端点）不受写请求 fail-closed 影响。
        for (String uri : new String[]{
                "/api/v1/auth/login",
                "/api/open/d/ep1/webhooks/wechat/pay",
                "/api/v1/terminals/kiosks/dev1/heartbeat"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
            assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        }
        verify(guard, never()).checkTerminalEnabled();
    }

    @Test
    void selfRescuePathsAreExemptEvenWithTrustedTerminal() {
        for (String uri : new String[]{
                "/api/v1/auth/login",
                "/api/v1/terminals/capability-switches",
                "/api/v1/terminals/capability-switches/effective",
                "/api/v1/terminals/capability-switches/tcs_term_web"}) {
            assertThat(interceptor.preHandle(trusted("GET", uri, "web"),
                    new MockHttpServletResponse(), new Object())).isTrue();
        }
        verify(guard, never()).checkTerminalEnabled();
    }

    @Test
    void trustedTerminalGoesThroughMasterGate() {
        MockHttpServletRequest request = trusted("POST", "/api/v1/ticketing/verifications", "staff_work_phone");
        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        verify(guard).checkTerminalEnabled();
    }

    @Test
    void verifiedDeviceIdentityBeatsSelfReportedHeaderAtTheGate() {
        // 设备凭证已验签为 kiosk，客户端却自报 web 想绕开 kiosk 的关停开关 → 闸按 kiosk 判定。
        MockHttpServletRequest request = trusted("POST", "/api/v1/bar/orders", "kiosk");
        request.addHeader("X-Terminal-Type", "web");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        List<String> judgedAs = new ArrayList<>();
        doAnswer(invocation -> judgedAs.add(GateRequestContext.terminalType()))
                .when(guard).checkTerminalEnabled();

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(judgedAs).containsExactly("kiosk");
    }

    @Test
    void disabledTerminalRejectionPropagates() {
        doThrow(ApiException.forbidden("TERMINAL_DISABLED", "该终端能力已停用：terminal.enabled"))
                .when(guard).checkTerminalEnabled();
        MockHttpServletRequest request =
                trusted("GET", "/api/v1/finance/reconciliation/differences", "web");
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
    }
}
