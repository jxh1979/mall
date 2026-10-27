// 文件版本：V0.02
package com.complex.platform.settlement;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.GlobalExceptionHandler;
import com.complex.platform.config.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 结算配置中心 controller 鉴权闭合(批0)。
 *
 * <p>集成测试直调仓储绕过了 controller 鉴权层,本 @WebMvcTest 补齐:概览须 settlement.config.view + 平台域;
 * 转正须 settlement.config.manage + 平台域;缺权限 / 非平台主体一律 403(与复用的 AuthorizationService 契约对齐)。
 */
@WebMvcTest(controllers = SettlementConfigController.class)
@Import({com.complex.platform.support.AuthAccessTestConfiguration.class, GlobalExceptionHandler.class, SecurityConfig.class, SettlementConfigControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class SettlementConfigControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private SettlementConfigService settlementConfigService;

    @BeforeEach
    void reset() {
        Mockito.reset(authorizationService, settlementConfigService);
    }

    /** 概览:校 settlement.config.view + 平台域,返回总览。 */
    @Test
    void overviewRequiresViewPermissionAndPlatformScope() throws Exception {
        when(settlementConfigService.overview()).thenReturn(
                new SettlementConfigService.SettlementConfigOverview(List.of(), 0, List.of()));

        mockMvc.perform(get("/api/v1/settlement/config/overview").with(user("admin").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pending_draft_count").value(0));

        verify(authorizationService).requirePermission("settlement.config.view");
        verify(authorizationService).requirePlatformScope();
    }

    /** 非平台主体(商户/外包)访问概览 → 403 PLATFORM_SCOPE_REQUIRED。 */
    @Test
    void overviewForbiddenForNonPlatformScope() throws Exception {
        doThrow(ApiException.forbidden("PLATFORM_SCOPE_REQUIRED", "platform-level data is not accessible"))
                .when(authorizationService).requirePlatformScope();

        mockMvc.perform(get("/api/v1/settlement/config/overview").with(user("admin").roles("P0_SMOKE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PLATFORM_SCOPE_REQUIRED"));
    }

    /** 转正:缺 settlement.config.manage → 403(高敏动作 fail-closed)。 */
    @Test
    void activateForbiddenWithoutManagePermission() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "missing settlement.config.manage"))
                .when(authorizationService).requirePermission("settlement.config.manage");

        mockMvc.perform(post("/api/v1/settlement/config/attribution-rules/mrar_1/activate")
                        .with(user("admin").roles("P0_SMOKE")))
                .andExpect(status().isForbidden());

        verify(authorizationService).requirePermission("settlement.config.manage");
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        AuthorizationService authorizationService() {
            return Mockito.mock(AuthorizationService.class);
        }

        @Bean
        SettlementConfigService settlementConfigService() {
            return Mockito.mock(SettlementConfigService.class);
        }

        @Bean
        SettlementConfigWriteService settlementConfigWriteService() {
            return Mockito.mock(SettlementConfigWriteService.class);
        }

        @Bean
        DistributionSchemeService distributionSchemeService() {
            return Mockito.mock(DistributionSchemeService.class);
        }

        @Bean
        com.complex.platform.evidence.AuditEventService auditEventService() {
            return Mockito.mock(com.complex.platform.evidence.AuditEventService.class);
        }

        @Bean
        AuthService authService() {
            return new AuthService("", "");
        }
    }
}
