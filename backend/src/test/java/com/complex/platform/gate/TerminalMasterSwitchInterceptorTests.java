package com.complex.platform.gate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.complex.platform.common.api.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * 3.19 二期：终端总开关逐请求强制。声明 X-Terminal-Type 的请求过 terminal.enabled 闸；
 * 未声明/自救路径（登录、开关管理）不进闸；开关关/读取失败由 guard 抛 TERMINAL_DISABLED。
 */
class TerminalMasterSwitchInterceptorTests {

    private final TerminalSwitchGuard guard = mock(TerminalSwitchGuard.class);
    private final TerminalMasterSwitchInterceptor interceptor = new TerminalMasterSwitchInterceptor(
            new org.springframework.beans.factory.support.StaticListableBeanFactory(
                    java.util.Map.of("terminalSwitchGuard", guard))
                    .getBeanProvider(TerminalSwitchGuard.class));

    @Test
    void requestWithoutTerminalHeaderSkipsGate() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/bar/orders");
        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        verify(guard, never()).check(TerminalMasterSwitchInterceptor.MASTER_CAPABILITY);
    }

    @Test
    void selfRescuePathsAreExemptEvenWithTerminalHeader() {
        for (String uri : new String[]{
                "/api/v1/auth/login",
                "/api/v1/terminals/capability-switches",
                "/api/v1/terminals/capability-switches/effective",
                "/api/v1/terminals/capability-switches/tcs_term_web"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
            request.addHeader("X-Terminal-Type", "web");
            assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        }
        verify(guard, never()).check(TerminalMasterSwitchInterceptor.MASTER_CAPABILITY);
    }

    @Test
    void declaredTerminalGoesThroughMasterGate() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/ticketing/verifications");
        request.addHeader("X-Terminal-Type", "staff_work_phone");
        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        verify(guard).check(TerminalMasterSwitchInterceptor.MASTER_CAPABILITY);
    }

    @Test
    void disabledTerminalRejectionPropagates() {
        doThrow(ApiException.forbidden("TERMINAL_DISABLED", "terminal capability is disabled: terminal.enabled"))
                .when(guard).check(TerminalMasterSwitchInterceptor.MASTER_CAPABILITY);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/finance/reconciliation/differences");
        request.addHeader("X-Terminal-Type", "web");
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
    }
}
