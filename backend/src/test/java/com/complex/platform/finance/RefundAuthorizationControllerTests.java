// 文件版本：V0.01
package com.complex.platform.finance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.GlobalExceptionHandler;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.config.SecurityConfig;
import com.complex.platform.finance.dto.RefundAuthorityLimitRequest;
import com.complex.platform.finance.dto.RefundAutoRuleRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RefundAuthorizationController.class)
@Import({com.complex.platform.support.AuthAccessTestConfiguration.class, GlobalExceptionHandler.class, SecurityConfig.class, RefundAuthorizationControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class RefundAuthorizationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RefundAuthorizationConfigService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
    }

    @Test
    void listLimitsUsesViewPermissionAndPageShape() throws Exception {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        when(service.listLimits(org.mockito.ArgumentMatchers.any())).thenReturn(new PageResponse<>(
                List.of(new RefundAuthorityLimitRecord(
                        "refund_limit_staff_op1_ticket", "staff", "op1", "ticket", 50000L, null, "active", now, now)),
                1, 50, 1));

        mockMvc.perform(get("/api/v1/finance/refund-authority/limits")
                        .with(user("admin").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].subject_id").value("op1"))
                .andExpect(jsonPath("$.data.items[0].limit_amount_cent").value(50000))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(authorizationService).requirePermission("refund.authority.view");
    }

    @Test
    void upsertLimitUsesManagePermission() throws Exception {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        when(service.upsertLimit(any(RefundAuthorityLimitRequest.class))).thenReturn(new RefundAuthorityLimitRecord(
                "refund_limit_staff_op1_ticket", "staff", "op1", "ticket", 50000L, null, "active", now, now));

        mockMvc.perform(post("/api/v1/finance/refund-authority/limits")
                        .with(user("admin").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "limit-1",
                                  "subject_type": "staff",
                                  "subject_id": "op1",
                                  "refund_domain": "ticket",
                                  "limit_amount_cent": 50000,
                                  "status": "active"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("refund_limit_staff_op1_ticket"))
                .andExpect(jsonPath("$.data.limit_amount_cent").value(50000));

        verify(authorizationService).requirePermission("refund.authority.manage");
    }

    @Test
    void upsertLimitMissingIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/finance/refund-authority/limits")
                        .with(user("admin").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subject_type": "staff",
                                  "subject_id": "op1",
                                  "refund_domain": "ticket",
                                  "limit_amount_cent": 50000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("请勿重复提交，请刷新后再试"));
    }

    @Test
    void listRulesUsesViewPermission() throws Exception {
        when(service.listRules(org.mockito.ArgumentMatchers.any())).thenReturn(new PageResponse<>(List.of(), 1, 50, 0));

        mockMvc.perform(get("/api/v1/finance/refund-authority/rules")
                        .with(user("admin").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        verify(authorizationService).requirePermission("refund.authority.view");
    }

    @Test
    void upsertRuleUsesManagePermission() throws Exception {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        when(service.upsertRule(any(RefundAutoRuleRequest.class))).thenReturn(new RefundAutoRuleRecord(
                "refund_rule_ticket_expired_unused", "ticket", "expired_unused", true, null, "active", now, now));

        mockMvc.perform(post("/api/v1/finance/refund-authority/rules")
                        .with(user("admin").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "rule-1",
                                  "refund_domain": "ticket",
                                  "rule_type": "expired_unused",
                                  "enabled": true,
                                  "status": "active"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("refund_rule_ticket_expired_unused"))
                .andExpect(jsonPath("$.data.enabled").value(true));

        verify(authorizationService).requirePermission("refund.authority.manage");
    }

    @Test
    void upsertRuleMissingIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/finance/refund-authority/rules")
                        .with(user("admin").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refund_domain": "ticket",
                                  "rule_type": "expired_unused",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("请勿重复提交，请刷新后再试"));
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        RefundAuthorizationConfigService refundAuthorizationConfigService() {
            return Mockito.mock(RefundAuthorizationConfigService.class);
        }

        @Bean
        AuthorizationService authorizationService() {
            return Mockito.mock(AuthorizationService.class);
        }

        @Bean
        AuthService authService() {
            return new AuthService("", "");
        }
    }
}
