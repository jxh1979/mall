package com.complex.platform.merchantbilling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.config.SecurityConfig;
import com.complex.platform.merchantbilling.dto.MerchantFeeNotificationRecipientRequest;
import java.time.Instant;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MerchantFeeNotificationRecipientController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, MerchantFeeNotificationRecipientControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class MerchantFeeNotificationRecipientControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantFeeNotificationRecipientService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
    }

    @Test
    void listRequiresManagePermissionAndReturnsPage() throws Exception {
        when(service.list("merchant_1")).thenReturn(new PageResponse<>(List.of(recipient("active")), 1, 200, 1));

        mockMvc.perform(get("/api/v1/merchant-billing/fee-notification-recipients")
                        .queryParam("merchant_id", "merchant_1")
                        .with(user("finance").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("mfnr_1"))
                .andExpect(jsonPath("$.data.items[0].recipient_phone").value("15577000001"));

        verify(authorizationService).requirePermission("merchant.billing.manage");
    }

    @Test
    void getRequiresManagePermissionAndReturnsRecipient() throws Exception {
        when(service.get("mfnr_1")).thenReturn(recipient("active"));

        mockMvc.perform(get("/api/v1/merchant-billing/fee-notification-recipients/mfnr_1")
                        .with(user("finance").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fee_event").value("rent"));

        verify(authorizationService).requirePermission("merchant.billing.manage");
    }

    @Test
    void upsertRequiresManagePermissionAndReturnsRecipient() throws Exception {
        when(service.upsert(any(MerchantFeeNotificationRecipientRequest.class))).thenReturn(recipient("active"));

        mockMvc.perform(post("/api/v1/merchant-billing/fee-notification-recipients")
                        .with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchant_id": "merchant_1",
                                  "fee_event": "rent",
                                  "recipient_name": "Finance",
                                  "recipient_phone": "15577000001",
                                  "operator_user_id": "staff_finance"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("active"));

        verify(authorizationService).requirePermission("merchant.billing.manage");
    }

    @Test
    void deactivateRequiresManagePermissionAndReturnsInactiveRecipient() throws Exception {
        when(service.deactivate("mfnr_1")).thenReturn(recipient("inactive"));

        mockMvc.perform(post("/api/v1/merchant-billing/fee-notification-recipients/mfnr_1/deactivate")
                        .with(user("finance").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("inactive"));

        verify(authorizationService).requirePermission("merchant.billing.manage");
    }

    @Test
    void permissionDeniedReturnsForbidden() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("merchant.billing.manage");

        mockMvc.perform(post("/api/v1/merchant-billing/fee-notification-recipients")
                        .with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchant_id": "merchant_1",
                                  "fee_event": "rent",
                                  "recipient_name": "Finance",
                                  "recipient_phone": "15577000001",
                                  "operator_user_id": "staff_finance"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    private MerchantFeeNotificationRecipientRecord recipient(String status) {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new MerchantFeeNotificationRecipientRecord(
                "mfnr_1",
                "merchant_1",
                "rent",
                "Finance",
                "15577000001",
                status,
                "staff_finance",
                now,
                now,
                "inactive".equals(status) ? now : null
        );
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        MerchantFeeNotificationRecipientService merchantFeeNotificationRecipientService() {
            return Mockito.mock(MerchantFeeNotificationRecipientService.class);
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
