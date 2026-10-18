package com.complex.platform.finance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.GlobalExceptionHandler;
import com.complex.platform.config.SecurityConfig;
import com.complex.platform.customer.StoredValueService;
import com.complex.platform.finance.dto.RefundResultConfirmRequest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FinanceController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, FinanceRefundLifecycleControllerTests.TestBeans.class})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class})
class FinanceRefundLifecycleControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean FinanceService service;
    @MockitoBean AuthorizationService authorization;
    @MockitoBean StoredValueService storedValueService;

    private static final String OPERATION = """
            {"idempotency_key":"query-one","operator_note":"核查退款"}
            """;
    private static final String CONFIRMATION = """
            {"idempotency_key":"confirm-one","operator_note":"核实原流水",
             "evidence_attachment_ids":["evidence-one"],"status":"success","amount_cent":3000,
             "payment_reference":"original-refund-flow","payment_operator_id":"actual-operator"}
            """;

    @Test
    void unauthenticatedRefundReadDoesNotReachBusinessService() throws Exception {
        mvc.perform(get("/api/v1/finance/refunds")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void refundListAndOriginalPaymentsRequireLedgerViewPermission() throws Exception {
        mvc.perform(get("/api/v1/finance/refunds").with(user("finance").roles("P0_SMOKE")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/finance/ar-ap-items/item-one/payments").with(user("finance").roles("P0_SMOKE")))
                .andExpect(status().isOk());
        verify(authorization, times(2)).requirePermission("finance.ar_ap.view");
        verify(service).listFinanceRefunds();
        verify(service).listArApPayments("item-one");
    }

    @Test
    void queryUsesReadPermissionAndNeverDispatchesRetry() throws Exception {
        mvc.perform(post("/api/v1/finance/refunds/refund-one/query").with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON).content(OPERATION))
                .andExpect(status().isOk());
        verify(authorization).requirePermission("finance.ar_ap.view");
        verify(authorization, never()).requirePermission("finance.refund.create");
        verify(service).queryFinanceRefund(eq("refund-one"), any());
        verify(service, never()).retryFinanceRefund(anyString(), any());
    }

    @Test
    void retryWithoutRefundPermissionIsRejectedBeforeService() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "无退款权限"))
                .when(authorization).requirePermission("finance.refund.create");
        mvc.perform(post("/api/v1/finance/refunds/refund-one/retry").with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON).content(OPERATION))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        verifyNoInteractions(service);
    }

    @Test
    void executionWithoutEvidenceIsRejectedBeforeService() throws Exception {
        mvc.perform(post("/api/v1/finance/refunds/refund-one/execution").with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                            {"idempotency_key":"execute-one","operator_note":"本人执行",
                             "evidence_attachment_ids":[],"payment_reference":"refund-flow"}
                            """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        verifyNoInteractions(service);
    }

    @Test
    void confirmationRequiresIndependentPaymentRegistrationPermission() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "无款项核实权限"))
                .when(authorization).requirePermission("finance.payment.register");
        mvc.perform(post("/api/v1/finance/refunds/refund-one/confirm").with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON).content(CONFIRMATION))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        verify(authorization).requirePermission("finance.refund.create");
        verify(authorization).requirePermission("finance.payment.register");
        verifyNoInteractions(service);
    }

    @Test
    void confirmationCarriesExactAmountReferenceAndRecordedOperatorToService() throws Exception {
        when(service.confirmFinanceRefundResult(eq("refund-one"), any())).thenReturn(
                new FinanceRefundRecord("refund-one", "payment", 3000, "原路退款", "wechat", "original", "success", Instant.now()));
        mvc.perform(post("/api/v1/finance/refunds/refund-one/confirm").with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON).content(CONFIRMATION))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
        ArgumentCaptor<RefundResultConfirmRequest> request = ArgumentCaptor.forClass(RefundResultConfirmRequest.class);
        verify(service).confirmFinanceRefundResult(eq("refund-one"), request.capture());
        verify(authorization).requirePermission("finance.refund.create");
        verify(authorization).requirePermission("finance.payment.register");
        assertThat(request.getValue().amountCent()).isEqualTo(3000);
        assertThat(request.getValue().paymentOperatorId()).isEqualTo("actual-operator");
        assertThat(request.getValue().paymentReference()).isEqualTo("original-refund-flow");
        assertThat(request.getValue().evidenceAttachmentIds()).containsExactly("evidence-one");
    }

    @Test
    void confirmationRejectsNonPositiveAmountBeforeService() throws Exception {
        mvc.perform(post("/api/v1/finance/refunds/refund-one/confirm").with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON).content(CONFIRMATION.replace("3000", "0")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        verifyNoInteractions(service);
    }

    @TestConfiguration
    static class TestBeans {
        @Bean AuthService authService() { return new AuthService("", ""); }
    }
}
