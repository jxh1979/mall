// 文件版本：V0.01
package com.complex.platform.gate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 第 100 条 100-R1：Web 前端与商户网页门户的模块按接口定（按段边界取最长前缀；共用接口不判），自报 X-Module-Code 不再起作用。 */
class ServerModuleRoutesTests {

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void webRoutesResolveByLongestSegmentPrefix() {
        assertThat(ServerModuleRoutes.moduleOf("web", "/api/v1/bar/void-reason-codes")).isEqualTo("bar");
        assertThat(ServerModuleRoutes.moduleOf("web", "/api/v1/bar/catalog")).as("酒吧与票务页共用").isNull();
        assertThat(ServerModuleRoutes.moduleOf("web", "/api/v1/bar-business-day-configs"))
                .as("不被 /api/v1/bar 前缀吃掉").isEqualTo("amusement");
        assertThat(ServerModuleRoutes.moduleOf("web", "/api/v1/merchant-penalty-bounds")).isEqualTo("governance");
        assertThat(ServerModuleRoutes.moduleOf("web", "/api/v1/analytics/risk-alerts/x/resolve")).isEqualTo("risk");
        assertThat(ServerModuleRoutes.moduleOf("web", "/api/v1/access/effective-permissions")).as("外壳取权限").isNull();
    }

    @Test
    void merchantPortalRoutesResolveOnlyMerchantZone() {
        assertThat(ServerModuleRoutes.moduleOf("merchant_portal", "/api/v1/merchant-portal/unread-bills")).isEqualTo("merchant");
        assertThat(ServerModuleRoutes.moduleOf("merchant_portal", "/api/v1/merchant-portal/card-exit-notices")).isNull();
        assertThat(ServerModuleRoutes.moduleOf("merchant_portal", "/api/v1/bar/orders")).isNull();
    }

    @Test
    void webIgnoresModuleHeaderWhileCustomerAppStillReadsIt() {
        bind("web", "/api/v1/platform/projects", "bar");
        assertThat(GateRequestContext.moduleCode()).as("web 端忽略自报模块头").isNull();
        assertThat(GateRequestContext.orderedScopes().get("module")).isNull();

        bind("app", "/api/v1/customer/points/summary", "points");
        assertThat(GateRequestContext.moduleCode()).as("客人端仍按头").isEqualTo("points");
    }

    private static void bind(String terminal, String uri, String moduleHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.addHeader("X-Module-Code", moduleHeader);
        TerminalIdentityHolder.set(request, new TerminalIdentityHolder.VerifiedTerminal(terminal, null, false));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
