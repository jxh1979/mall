package com.complex.platform.settlement;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.GlobalExceptionHandler;
import com.complex.platform.config.SecurityConfig;
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
 * 结算配置中心批1(通票分账):过账网关端点 ② {@code POST /api/v1/settlement/bills/{id}/post} 鉴权 + 契约闭合。
 *
 * <p>集成测试(批2-4/2-5)已直调 {@link SettlementPostingService#postBill} 覆盖真实过账副作用
 * (ar_ap 落地 + bill→settled + 幂等);端点仅是「鉴权 + 平台域 + 转发 postBill 返回笔数」的薄委托,
 * 本 @WebMvcTest 补齐该薄层:高敏权限 settlement.bill.post + 平台域门、返回笔数透传、缺权限/非平台域一律 403、
 * postBill 抛业务冲突(超池 draft 单)→ 409 SETTLEMENT_BILL_NOT_CONFIRMED 透传前端(与既有 config controller 鉴权测试同范式)。
 */
@WebMvcTest(controllers = SettlementController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, SettlementControllerPostBillTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class SettlementControllerPostBillTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private SettlementPostingService postingService;

    @BeforeEach
    void reset() {
        Mockito.reset(authorizationService, postingService);
    }

    /** 过账:校 settlement.bill.post + 平台域,透传 postBill 返回的过账笔数。 */
    @Test
    void postBillRequiresPostPermissionAndPlatformScopeAndReturnsCount() throws Exception {
        when(postingService.postBill("bill_1")).thenReturn(3);

        mockMvc.perform(post("/api/v1/settlement/bills/bill_1/post").with(user("admin").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));

        verify(authorizationService).requirePermission("settlement.bill.post");
        verify(authorizationService).requirePlatformScope();
        verify(postingService).postBill("bill_1");
    }

    /** 缺 settlement.bill.post → 403(高敏动作 fail-closed),且不触达过账。 */
    @Test
    void postBillForbiddenWithoutPostPermission() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "missing settlement.bill.post"))
                .when(authorizationService).requirePermission("settlement.bill.post");

        mockMvc.perform(post("/api/v1/settlement/bills/bill_1/post").with(user("admin").roles("P0_SMOKE")))
                .andExpect(status().isForbidden());

        verify(authorizationService).requirePermission("settlement.bill.post");
        verifyNoInteractions(postingService); // 鉴权先于过账,fail-closed 不落账
    }

    /** 非平台主体(商户/外包)→ 403 PLATFORM_SCOPE_REQUIRED,不触达过账。 */
    @Test
    void postBillForbiddenForNonPlatformScope() throws Exception {
        doThrow(ApiException.forbidden("PLATFORM_SCOPE_REQUIRED", "platform-level action is not accessible"))
                .when(authorizationService).requirePlatformScope();

        mockMvc.perform(post("/api/v1/settlement/bills/bill_1/post").with(user("admin").roles("P0_SMOKE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PLATFORM_SCOPE_REQUIRED"));

        verifyNoInteractions(postingService);
    }

    /**
     * v7 审计 R-2(V357):超池特批走<b>专属权限点</b> settlement.bill.special_approve(非过账权限),
     * 平台域;回执带到期日(S9 短期激励期限)。
     */
    @Test
    void specialApproveRequiresDedicatedPermissionAndReturnsValidUntil() throws Exception {
        java.time.LocalDate validUntil = java.time.LocalDate.now().plusMonths(3);
        when(postingService.specialApprove("bill_1", "SA-1"))
                .thenReturn(new SettlementPostingService.SpecialApprovalResult("SA-1", validUntil));

        mockMvc.perform(post("/api/v1/settlement/bills/bill_1/special-approval")
                        .with(user("admin").roles("P0_SMOKE"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        // 请求体同样 SNAKE_CASE：special_approval_ref。
                        .content("{\"special_approval_ref\":\"SA-1\"}"))
                .andExpect(status().isOk())
                // 全局 SNAKE_CASE 序列化：specialApprovalRef ↔ special_approval_ref。
                .andExpect(jsonPath("$.data.special_approval_ref").value("SA-1"))
                .andExpect(jsonPath("$.data.valid_until").value(validUntil.toString()));

        verify(authorizationService).requirePermission("settlement.bill.special_approve");
        verify(authorizationService).requirePlatformScope();
    }

    /** 只有过账权限、没有特批专属权限 → 403,不触达特批(分权:过账可下放,超池特批只归平台负责人)。 */
    @Test
    void specialApproveForbiddenWithoutDedicatedPermission() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "missing settlement.bill.special_approve"))
                .when(authorizationService).requirePermission("settlement.bill.special_approve");

        mockMvc.perform(post("/api/v1/settlement/bills/bill_1/special-approval")
                        .with(user("admin").roles("P0_SMOKE"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        // 请求体同样 SNAKE_CASE：special_approval_ref。
                        .content("{\"special_approval_ref\":\"SA-1\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(postingService);
    }

    /** 超池 draft 单调过账 → postBill 抛冲突 → 409 SETTLEMENT_BILL_NOT_CONFIRMED 透传。 */
    @Test
    void postBillConflictWhenNotConfirmed() throws Exception {
        when(postingService.postBill("draft_bill")).thenThrow(ApiException.conflict(
                "SETTLEMENT_BILL_NOT_CONFIRMED", "只有已确认的账单才能过账；当前状态：draft"));

        mockMvc.perform(post("/api/v1/settlement/bills/draft_bill/post").with(user("admin").roles("P0_SMOKE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("SETTLEMENT_BILL_NOT_CONFIRMED"));
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        AuthorizationService authorizationService() {
            return Mockito.mock(AuthorizationService.class);
        }

        @Bean
        SettlementQueryService settlementQueryService() {
            return Mockito.mock(SettlementQueryService.class);
        }

        @Bean
        SettlementPostingService settlementPostingService() {
            return Mockito.mock(SettlementPostingService.class);
        }

        @Bean
        AuthService authService() {
            return new AuthService("", "");
        }
    }
}
