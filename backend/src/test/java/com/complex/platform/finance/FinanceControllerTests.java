// 文件版本：V0.03
package com.complex.platform.finance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
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
import com.complex.platform.finance.dto.ArApItemRequest;
import com.complex.platform.finance.dto.ArApPaymentRequest;
import com.complex.platform.finance.dto.DailyCloseRequest;
import com.complex.platform.finance.dto.OfflinePaymentConfirmRequest;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.finance.dto.RefundRequest;
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

@WebMvcTest(controllers = FinanceController.class)
@Import({com.complex.platform.support.AuthAccessTestConfiguration.class, GlobalExceptionHandler.class, SecurityConfig.class, FinanceControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class FinanceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    // StoredValueService 有必需的 @Autowired 字段；@Bean 交出的 mock 仍会被字段注入，故用 @MockitoBean。
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.complex.platform.customer.StoredValueService storedValueService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.complex.platform.dealer.JdbcDealerOrderRepository dealerOrderRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private FinanceService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
        // Default: platform admin scope (FinanceService mock doesn't call this, but
        // stub it so any unexpected real call doesn't blow up with an unstubbed mock error)
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
    }

    @Test
    void createArApItemMissingIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/finance/ar-ap-items")
                        .with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "direction": "receivable",
                                  "counterparty_id": "merchant_1",
                                  "source_type": "order",
                                  "source_id": "order_1",
                                  "amount_cent": 128800,
                                  "due_date": "2026-06-30",
                                  "accounting_period": "2026-06"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("请勿重复提交，请刷新后再试"));
    }

    @Test
    void createArApItemRejectsNonPositiveAmount() throws Exception {
        // @Valid 全域广覆盖：要紧单据（应收应付）金额走 @NotNull @Positive 在控制器层拦下负数（不进 service）。
        mockMvc.perform(post("/api/v1/finance/ar-ap-items")
                        .with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "arap-neg",
                                  "direction": "receivable",
                                  "counterparty_id": "merchant_1",
                                  "source_type": "order",
                                  "source_id": "order_1",
                                  "amount_cent": -100,
                                  "due_date": "2026-06-30",
                                  "accounting_period": "2026-06"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    @Test
    void createRefundRejectsNonPositiveAmount() throws Exception {
        // T1.3 边界防御纵深：@Valid + @Positive 在控制器层拦下负数退款金额（不进 service；服务层 requirePositive 仍是主闸）。
        mockMvc.perform(post("/api/v1/finance/refunds")
                        .with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "refund-neg",
                                  "target_id": "payment_1",
                                  "refund_amount_cent": -100,
                                  "reason": "x",
                                  "refund_channel": "original_channel"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    @Test
    void createPaymentIntentRejectsNonPositiveAmount() throws Exception {
        // T1.3 边界防御纵深：@Valid + @Positive 在控制器层拦下零/负支付意图金额（不进 service）。
        mockMvc.perform(post("/api/v1/finance/payment-intents")
                        .with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "pi-zero",
                                  "business_order_type": "ticket_order",
                                  "business_order_id": "order_1",
                                  "amount_cent": 0,
                                  "currency": "CNY",
                                  "channel": "aggregate_pay"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    @Test
    void listArApItemsUsesOpenApiViewPermissionAndPageShape() throws Exception {
        when(service.listArApItems(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(new PageResponse<>(List.of(arApItem()), 1, 50, 1));

        mockMvc.perform(get("/api/v1/finance/ar-ap-items")
                        .with(user("finance").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].item_no").value("ARAP202606140001"))
                .andExpect(jsonPath("$.data.items[0].unpaid_amount_cent").value(128800))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(authorizationService).requirePermission("finance.ar_ap.view");
    }

    @Test
    void exportArApItemsUsesDedicatedExportPermissionAndReturnsPasswordProtectedPdf() throws Exception {
        // 第 23 条 23-R3：导出只交出服务端生成的带密码 PDF（PGM:687），不再返回数据行与 CSV 文本。
        when(service.exportArApItems()).thenReturn(new com.complex.platform.security.ExportFileService.ExportFile(
                "exp_1", "应收应付台账.pdf", new byte[] {'%', 'P', 'D', 'F'}));

        mockMvc.perform(get("/api/v1/finance/ar-ap-items/export")
                        .with(user("finance").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("application/pdf"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("filename*=UTF-8''")));

        // 查≠导：导出须独立权限点（审计角色不直授，走临时授权审批）。
        verify(authorizationService).requirePermission("finance.ar_ap.export");
        verify(service).exportArApItems();
    }

    @Test
    void paymentRegistrationUsesPaymentRegisterPermission() throws Exception {
        when(service.registerArApPayment(anyString(), any(ArApPaymentRequest.class)))
                .thenReturn(new ArApPaymentResponse(
                        new ArApPaymentRecord(
                                "arap_pay_1",
                                "arap_1",
                                28800L,
                                "bank_transfer",
                                Instant.parse("2026-06-14T01:00:00Z"),
                                "bank_flow_1",
                                Instant.parse("2026-06-14T01:00:01Z")
                        ),
                        new ArApItemRecord(
                                "arap_1",
                                "ARAP202606140001",
                                "receivable",
                                "merchant_1",
                                "order",
                                "order_1",
                                128800L,
                                28800L,
                                100000L,
                                "CNY",
                                LocalDate.parse("2026-06-30"),
                                "2026-06",
                                "partial_paid",
                                Instant.parse("2026-06-14T00:00:00Z"),
                                Instant.parse("2026-06-14T01:00:01Z"),
                                null
                        )
                ));

        mockMvc.perform(post("/api/v1/finance/ar-ap-items/arap_1/payments")
                        .with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "pay-arap-1",
                                  "amount_cent": 28800,
                                  "payment_method": "bank_transfer",
                                  "payment_time": "2026-06-14T01:00:00Z",
                                  "bank_flow_ref": "bank_flow_1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.item.status").value("partial_paid"))
                .andExpect(jsonPath("$.data.item.unpaid_amount_cent").value(100000));

        verify(authorizationService).requirePermission("finance.payment.register");
    }

    @Test
    void dailyCloseUsesDailyCloseLockPermission() throws Exception {
        when(service.createDailyClose(any(DailyCloseRequest.class))).thenReturn(new DailyCloseRecord(
                "daily_close_1",
                LocalDate.parse("2026-06-14"),
                "project",
                "project_1",
                false,
                "locked",
                Instant.parse("2026-06-14T23:59:00Z"),
                Instant.parse("2026-06-14T23:59:00Z")
        ));

        mockMvc.perform(post("/api/v1/finance/daily-closes")
                        .with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "daily-close-1",
                                  "business_date": "2026-06-14",
                                  "scope_type": "project",
                                  "scope_id": "project_1",
                                  "force_close": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("locked"));

        verify(authorizationService).requirePermission("finance.daily_close.lock");
    }

    @Test
    void paymentIntentUsesPaymentIntentCreatePermission() throws Exception {
        when(service.createPaymentIntent(any(PaymentIntentRequest.class))).thenReturn(new PaymentIntentRecord(
                "pay_intent_1",
                "ticket_order",
                "order_1",
                128800L,
                "CNY",
                "mock",
                "created",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));

        mockMvc.perform(post("/api/v1/finance/payment-intents")
                        .with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "intent-1",
                                  "business_order_type": "ticket_order",
                                  "business_order_id": "order_1",
                                  "amount_cent": 128800,
                                  "currency": "CNY",
                                  "channel": "mock"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("created"));

        verify(authorizationService).requirePermission("finance.payment_intent.create");
    }

    @Test
    void offlineConfirmationUsesPaymentRegisterPermission() throws Exception {
        when(service.confirmOfflinePayment(anyString(), any(OfflinePaymentConfirmRequest.class)))
                .thenReturn(new OfflinePaymentConfirmationRecord(
                        "offline_confirm_1",
                        "payment_1",
                        128800L,
                        "cash",
                        Instant.parse("2026-06-14T00:05:00Z"),
                        List.of("attachment_1"),
                        "confirmed",
                        Instant.parse("2026-06-14T00:06:00Z")
                ));

        mockMvc.perform(post("/api/v1/finance/payments/payment_1/confirm-offline")
                        .with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "offline-confirm-1",
                                  "payment_id": "payment_1",
                                  "amount_cent": 128800,
                                  "payment_method": "cash",
                                  "confirmed_at": "2026-06-14T00:05:00Z",
                                  "evidence_attachment_ids": ["attachment_1"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("confirmed"));

        verify(authorizationService).requirePermission("finance.payment.register");
    }

    @Test
    void reconciliationEndpointsUseOpenApiPermissions() throws Exception {
        when(service.listReconciliationBatches()).thenReturn(new PageResponse<>(List.of(), 1, 50, 0));
        when(service.listReconciliationDifferences()).thenReturn(new PageResponse<>(List.of(), 1, 50, 0));

        mockMvc.perform(get("/api/v1/finance/reconciliation/batches")
                        .with(user("finance").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/api/v1/finance/reconciliation/differences")
                        .with(user("finance").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        // 读路径统一 finance.reconciliation.view(batches+differences 各一次); execute 保留给执行/补偿动作。
        verify(authorizationService, org.mockito.Mockito.times(2)).requirePermission("finance.reconciliation.view");
    }

    @Test
    void refundUsesRefundCreatePermission() throws Exception {
        when(service.createRefund(any(RefundRequest.class))).thenReturn(new FinanceRefundRecord(
                "refund_1",
                "payment_1",
                18800L,
                "guest request",
                "original_channel",
                "original",
                "requested",
                Instant.parse("2026-06-14T00:00:00Z")
        ));

        mockMvc.perform(post("/api/v1/finance/refunds")
                        .with(user("finance").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "refund-1",
                                  "target_id": "payment_1",
                                  "refund_amount_cent": 18800,
                                  "reason": "guest request",
                                  "refund_channel": "original_channel"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("requested"));

        verify(authorizationService).requirePermission("finance.refund.create");
    }

    private ArApItemRecord arApItem() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new ArApItemRecord(
                "arap_1",
                "ARAP202606140001",
                "receivable",
                "merchant_1",
                "order",
                "order_1",
                128800L,
                0L,
                128800L,
                "CNY",
                LocalDate.parse("2026-06-30"),
                "2026-06",
                "pending_collection",
                now,
                now,
                null
        );
    }

    @TestConfiguration
    static class TestBeans {
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
