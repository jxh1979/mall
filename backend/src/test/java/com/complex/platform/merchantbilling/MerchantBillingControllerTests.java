package com.complex.platform.merchantbilling;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.complex.platform.merchantbilling.dto.MerchantBillConfirmRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillCreateRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillDisputeRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillDunningRequest;
import java.time.Instant;
import java.time.LocalDate;
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

@WebMvcTest(controllers = MerchantBillingController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, MerchantBillingControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class MerchantBillingControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantBillingService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
    }

    @Test
    void unauthenticatedListReturnsUnauthorizedOrForbidden() throws Exception {
        int status = mockMvc.perform(get("/api/v1/merchant-billing/bills"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isIn(401, 403);
    }

    @Test
    void listRequiresViewPermissionAndReturnsPage() throws Exception {
        when(service.listBills()).thenReturn(new PageResponse<>(List.of(bill("issued")), 1, 50, 1));

        mockMvc.perform(get("/api/v1/merchant-billing/bills")
                        .with(user("finance").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("mbill_1"))
                .andExpect(jsonPath("$.data.items[0].lines[0].fee_type").value("rent"))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(authorizationService).requirePermission("merchant.billing.view");
    }

    @Test
    void createWithoutIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/merchant-billing/bills")
                        .with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchant_id": "merchant_1",
                                  "ar_ap_item_id": "arap_merchant_1",
                                  "bill_period_start": "2026-06-01",
                                  "bill_period_end": "2026-06-30",
                                  "due_date": "2026-07-05",
                                  "total_amount_cent": 168800,
                                  "lines": [
                                    {"fee_type": "rent", "description": "June rent", "amount_cent": 168800}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    @Test
    void createRequiresManagePermissionAndReturnsBill() throws Exception {
        when(service.createBill(any(MerchantBillCreateRequest.class))).thenReturn(bill("issued"));

        mockMvc.perform(post("/api/v1/merchant-billing/bills")
                        .with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "bill-create",
                                  "operator_note": "issue bill",
                                  "merchant_id": "merchant_1",
                                  "ar_ap_item_id": "arap_merchant_1",
                                  "bill_period_start": "2026-06-01",
                                  "bill_period_end": "2026-06-30",
                                  "due_date": "2026-07-05",
                                  "total_amount_cent": 168800,
                                  "source_type": "manual",
                                  "source_id": "billing_batch_202606",
                                  "lines": [
                                    {"fee_type": "rent", "description": "June rent", "amount_cent": 128800},
                                    {"fee_type": "cam", "description": "June CAM", "amount_cent": 40000}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("mbill_1"))
                .andExpect(jsonPath("$.data.status").value("issued"));

        verify(authorizationService).requirePermission("merchant.billing.manage");
    }

    @Test
    void createPermissionDeniedReturnsForbidden() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("merchant.billing.manage");

        mockMvc.perform(post("/api/v1/merchant-billing/bills")
                        .with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "bill-denied",
                                  "merchant_id": "merchant_1",
                                  "ar_ap_item_id": "arap_merchant_1",
                                  "bill_period_start": "2026-06-01",
                                  "bill_period_end": "2026-06-30",
                                  "due_date": "2026-07-05",
                                  "total_amount_cent": 168800,
                                  "lines": [
                                    {"fee_type": "rent", "description": "June rent", "amount_cent": 168800}
                                  ]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    @Test
    void confirmRequiresConfirmPermission() throws Exception {
        when(service.confirmBill(eq("mbill_1"), any(MerchantBillConfirmRequest.class))).thenReturn(bill("confirmed"));

        mockMvc.perform(post("/api/v1/merchant-billing/bills/mbill_1/confirm")
                        .with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "bill-confirm",
                                  "operator_note": "merchant confirmed",
                                  "operator_user_id": "staff_finance"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("confirmed"));

        verify(authorizationService).requirePermission("merchant.billing.confirm");
    }

    @Test
    void submitDisputeRequiresPermissionAndReturnsDispute() throws Exception {
        when(service.submitDispute(eq("mbill_1"), any(MerchantBillDisputeRequest.class)))
                .thenReturn(new MerchantBillDisputeRecord(
                        "mbill_dispute_1",
                        "mbill_1",
                        "merchant_1",
                        "energy_amount_mismatch",
                        "Energy reading mismatch",
                        "submitted",
                        List.of("att_meter_photo"),
                        "staff_finance",
                        Instant.parse("2026-06-14T00:00:00Z"),
                        Instant.parse("2026-06-14T00:00:00Z")
                ));

        mockMvc.perform(post("/api/v1/merchant-billing/bills/mbill_1/disputes")
                        .with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "bill-dispute",
                                  "operator_note": "merchant dispute",
                                  "evidence_attachment_ids": ["att_meter_photo"],
                                  "reason_code": "energy_amount_mismatch",
                                  "description": "Energy reading mismatch",
                                  "operator_user_id": "staff_finance"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("submitted"))
                .andExpect(jsonPath("$.data.evidence_attachment_ids[0]").value("att_meter_photo"));

        verify(authorizationService).requirePermission("merchant.billing.dispute.submit");
    }

    @Test
    void recordDunningRequiresPermissionAndReturnsDunningRecord() throws Exception {
        when(service.recordDunning(eq("mbill_1"), any(MerchantBillDunningRequest.class)))
                .thenReturn(new MerchantBillDunningRecord(
                        "mbill_dunning_1",
                        "mbill_1",
                        "merchant_1",
                        "wecom",
                        "rent_overdue_first_notice",
                        "sent",
                        Instant.parse("2026-06-16T02:00:00Z"),
                        "staff_finance",
                        Instant.parse("2026-06-14T00:00:00Z")
                ));

        mockMvc.perform(post("/api/v1/merchant-billing/bills/mbill_1/dunning-records")
                        .with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "bill-dunning",
                                  "operator_note": "first reminder",
                                  "dunning_channel": "wecom",
                                  "message_template": "rent_overdue_first_notice",
                                  "status": "sent",
                                  "sent_at": "2026-06-16T02:00:00Z",
                                  "operator_user_id": "staff_finance"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dunning_channel").value("wecom"))
                .andExpect(jsonPath("$.data.status").value("sent"));

        verify(authorizationService).requirePermission("merchant.billing.dunning.record");
    }

    private MerchantBillRecord bill(String status) {
        return new MerchantBillRecord(
                "mbill_1",
                "merchant_1",
                "arap_merchant_1",
                "MBILL202606140001",
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                LocalDate.parse("2026-07-05"),
                168800L,
                "CNY",
                status,
                0,
                "manual",
                "billing_batch_202606",
                List.of(new MerchantBillLineRecord(
                        "mbill_line_1",
                        "mbill_1",
                        "rent",
                        "June rent",
                        168800L,
                        "CNY",
                        Instant.parse("2026-06-14T00:00:00Z")
                )),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        );
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        MerchantBillingService merchantBillingService() {
            return Mockito.mock(MerchantBillingService.class);
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
