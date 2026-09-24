package com.complex.platform.ticketing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
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
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import com.complex.platform.ticketing.dto.TicketPriceRuleRequest;
import com.complex.platform.ticketing.dto.TicketRefundRequest;
import com.complex.platform.ticketing.dto.TicketTypeRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

@WebMvcTest(controllers = TicketingController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, TicketingControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class TicketingControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketingService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
    }

    @Test
    void unauthenticatedRequestReturns401UnauthorizedEnvelope() throws Exception {
        // 会话失效全局回登录页：无 token / token 过期必须 401（此前 Spring 默认 403 空 body，
        // 前端误译"当前账号无权进行此操作"且无法触发重新登录）。真权限不足仍 403（下方 PERMISSION_DENIED 测试）。
        mockMvc.perform(get("/api/v1/ticketing/ticket-types"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.detail_code").value("AUTH_SESSION_INVALID"));
    }

    // R-005：柜台散客人脸照同意落账端点与出票同权限；无 ticket.ticket.issue → 403。
    @Test
    void recordingFacePhotoConsentWithoutIssuePermissionReturnsPermissionDenied() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("ticket.ticket.issue");
        mockMvc.perform(post("/api/v1/ticketing/orders/to_1/face-photo-consent")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "consent-denied",
                                  "accepted": true,
                                  "source_terminal": "counter"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    @Test
    void recordingFacePhotoConsentReturnsConsentIdEnvelope() throws Exception {
        when(service.recordFacePhotoConsent(any(String.class),
                any(com.complex.platform.ticketing.dto.FacePhotoConsentRequest.class)))
                .thenReturn(new TicketFacePhotoService.FaceConsentView("consent_walkin_1", "to_1",
                        "face_photo_notice_v1@retention_30d", java.time.Instant.parse("2026-09-07T12:00:00Z")));
        mockMvc.perform(post("/api/v1/ticketing/orders/to_1/face-photo-consent")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "consent-ok",
                                  "accepted": true,
                                  "source_terminal": "counter"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.consent_id").value("consent_walkin_1"))
                .andExpect(jsonPath("$.data.version").value("face_photo_notice_v1@retention_30d"));
        verify(authorizationService).requirePermission("ticket.ticket.issue");
    }

    @Test
    void creatingTicketTypeWithoutManagePermissionReturnsPermissionDenied() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("ticket.product.manage");

        mockMvc.perform(post("/api/v1/ticketing/ticket-types")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "ticket-type-denied",
                                  "ticket_name": "P0 Pass",
                                  "base_calendar_price_mode": "weekday_weekend_holiday",
                                  "weekday_price_cent": 9900,
                                  "status": "active"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    @Test
    void creatingTicketTypeWithoutIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/ticketing/ticket-types")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticket_name": "P0 Pass",
                                  "base_calendar_price_mode": "weekday_weekend_holiday",
                                  "weekday_price_cent": 9900,
                                  "status": "active"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("请勿重复提交，请刷新后再试"));
    }

    @Test
    void creatingVerificationWithoutIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/ticketing/verifications")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "credential_type": "qr",
                                  "credential_payload": "P0-ENT-1",
                                  "device_id": "dev_1",
                                  "point_id": "gate_1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("请勿重复提交，请刷新后再试"));
    }

    // R5 T5：核销冲正端点权限 ticket.verify.reverse + 幂等键必填 + 成功回包。
    @Test
    void reverseVerificationWithoutReversePermissionReturnsPermissionDenied() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("ticket.verify.reverse");

        mockMvc.perform(post("/api/v1/ticketing/verifications/ver_1/reverse")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "reverse-denied",
                                  "reason": "wrong scan"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    @Test
    void reverseVerificationWithoutIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/ticketing/verifications/ver_1/reverse")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "wrong scan"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("请勿重复提交，请刷新后再试"));
    }

    @Test
    void reverseVerificationRouteUsesReversePermission() throws Exception {
        when(authorizationService.currentActor()).thenReturn(new com.complex.platform.access.AccessActor(
                "supervisor_1", "supervisor", "staff", null, null, "active"));
        when(service.reverseVerification(any(String.class),
                any(com.complex.platform.ticketing.dto.VerificationReverseRequest.class), any()))
                .thenReturn(new VerificationResponse("ver_rev_1", "V20260618000001", "reversed", "误核销"));

        mockMvc.perform(post("/api/v1/ticketing/verifications/ver_1/reverse")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "reverse-ok",
                                  "reason": "误核销"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("reversed"));

        verify(authorizationService).requirePermission("ticket.verify.reverse");
    }

    @Test
    void creatingTicketTypeUsesOpenApiPermissionAndSnakeCaseResponse() throws Exception {
        when(service.createTicketType(any(TicketTypeRequest.class))).thenReturn(new TicketTypeRecord(
                "ticket_type_1",
                "P0 Pass",
                "weekday_weekend_holiday",
                9900L,
                11900L,
                12900L,
                false,
                "active",
                null,
                null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        , null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));

        mockMvc.perform(post("/api/v1/ticketing/ticket-types")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "ticket-type-create",
                                  "ticket_name": "P0 Pass",
                                  "base_calendar_price_mode": "weekday_weekend_holiday",
                                  "weekday_price_cent": 9900,
                                  "weekend_price_cent": 11900,
                                  "holiday_price_cent": 12900,
                                  "stock_required": false,
                                  "status": "active"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("ticket_type_1"))
                .andExpect(jsonPath("$.data.ticket_name").value("P0 Pass"))
                .andExpect(jsonPath("$.data.base_calendar_price_mode").value("weekday_weekend_holiday"));

        verify(authorizationService).requirePermission("ticket.product.manage");
    }

    @Test
    void creatingTicketOrderUsesOpenApiPermissionAndRequiresIdempotencyKey() throws Exception {
        when(service.createTicketOrder(any(TicketOrderRequest.class))).thenReturn(new TicketOrderRecord(
                "ticket_order_1",
                "TO20260614000001",
                "customer_1",
                "ticket_type_1",
                null,
                2,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                19800L, null, 19800L, 0L,
                "CNY",
                "unpaid",
                "not_issued",
                "created",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        , null, null, null));

        mockMvc.perform(post("/api/v1/ticketing/orders")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "ticket-order-create",
                                  "ticket_type_id": "ticket_type_1",
                                  "quantity": 2,
                                  "visit_date": "2026-06-20",
                                  "sales_channel": "web",
                                  "buyer_phone": "13800138000",
                                  "customer_id": "customer_1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("ticket_order_1"))
                .andExpect(jsonPath("$.data.amount_cent").value(19800))
                .andExpect(jsonPath("$.data.currency").value("CNY"));

        verify(authorizationService).requirePermission("ticket.order.create");
    }

    @Test
    void ticketPayIntentRouteUsesFinancePaymentIntentPermission() throws Exception {
        when(service.createTicketOrderPayIntent(any(String.class), any(PaymentIntentRequest.class))).thenReturn(new TicketPaymentIntentRecord(
                "pay_intent_1",
                "ticket_order",
                "ticket_order_1",
                19800L,
                "CNY",
                "aggregate_pay",
                "created",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));

        mockMvc.perform(post("/api/v1/ticketing/orders/ticket_order_1/pay-intents")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "pay-ticket-order",
                                  "business_order_type": "ticket_order",
                                  "business_order_id": "ticket_order_1",
                                  "amount_cent": 19800,
                                  "currency": "CNY",
                                  "channel": "aggregate_pay"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("created"));

        verify(authorizationService).requirePermission("finance.payment_intent.create");
    }

    @Test
    void priceRuleCredentialAndRefundRoutesUseExactPermissions() throws Exception {
        when(service.createTicketPriceRule(any(TicketPriceRuleRequest.class))).thenReturn(new TicketPriceRuleRecord(
                "price_rule_1",
                "ticket_type_1",
                "student",
                "fixed_price",
                6900L,
                7900L,
                8900L,
                null,
                "web",
                "CNY",
                "active",
                Map.of(),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        when(service.getTicketCredential("ent_1")).thenReturn(new TicketCredentialResponse(
                "credential_1",
                "ent_1",
                "ticket_order_1",
                "cred_token",
                "dynamic_qr",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:05:00Z"),
                1
        ));
        when(service.requestTicketRefund(any(TicketRefundRequest.class))).thenReturn(new TicketRefundRecord(
                "refund_1",
                "ticket_order_1",
                19800L,
                "customer request",
                "original",
                "requested",
                null,
                null,
                null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z"),
                "merchant_1"
        ));
        when(service.approveTicketRefund(any(String.class), any(StatusChangeRequest.class))).thenReturn(new TicketRefundRecord(
                "refund_1",
                "ticket_order_1",
                19800L,
                "customer request",
                "original",
                "approved",
                "op_req",
                "ops",
                Instant.parse("2026-06-14T00:01:00Z"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:01:00Z"),
                "merchant_1"
        ));
        when(service.listTicketRefundLines("refund_1")).thenReturn(new PageResponse<>(List.of(new TicketRefundLineRecord(
                "refund_line_1",
                "refund_1",
                "ticket_order_1",
                "ent_1",
                "entry",
                "entitlement",
                9900L,
                1,
                0,
                "requested",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        )), 1, 50, 1));

        mockMvc.perform(post("/api/v1/ticketing/price-rules")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "price-rule",
                                  "ticket_type_id": "ticket_type_1",
                                  "variant_type": "student",
                                  "price_mode": "fixed_price",
                                  "weekday_price_cent": 6900
                                }
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/ticketing/tickets/ent_1/credential")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.credential_token").value("cred_token"));
        mockMvc.perform(post("/api/v1/ticketing/refunds")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "refund-request",
                                  "target_id": "ticket_order_1",
                                  "refund_amount_cent": 19800,
                                  "reason": "customer request"
                                }
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/ticketing/refunds/refund_1/approve")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "refund-approve",
                                  "action": "approve",
                                  "reason": "approved"
                                }
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/ticketing/refunds/refund_1/lines")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].refund_amount_cent").value(9900));

        verify(authorizationService).requirePermission("ticket.price_variant.manage");
        verify(authorizationService).requirePermission("ticket.credential.view");
        verify(authorizationService).requirePermission("ticket.refund.request");
        verify(authorizationService).requirePermission("ticket.refund.approve");
        verify(authorizationService).requirePermission("ticket.refund.view");
    }


    @Test
    void dynamicPricingRoutesUseExactPermissionsAndRequireIdempotency() throws Exception {
        TicketDynamicPricingRuleRecord rule = new TicketDynamicPricingRuleRecord(
                "dyn_rule_1",
                "ticket_type_1",
                "Summer uplift",
                100,
                java.time.LocalDate.parse("2026-06-01"),
                java.time.LocalDate.parse("2026-06-30"),
                "web",
                "percent_delta",
                new java.math.BigDecimal("20.00"),
                null,
                9900L,
                15000L,
                "draft",
                Map.of("inventory_level", "low"),
                "ops",
                List.of("att_dyn"),
                Instant.parse("2026-06-15T00:00:00Z"),
                Instant.parse("2026-06-15T00:00:00Z")
        );
        when(service.listDynamicPricingRules()).thenReturn(new PageResponse<>(List.of(rule), 1, 50, 1));
        when(service.createDynamicPricingRule(any(TicketDynamicPricingRuleRequest.class))).thenReturn(rule);
        when(service.publishDynamicPricingRule(any(String.class), any(StatusChangeRequest.class)))
                .thenReturn(new TicketDynamicPricingRuleRecord(
                        rule.id(), rule.ticketTypeId(), rule.ruleName(), rule.priority(), rule.effectiveFrom(),
                        rule.effectiveTo(), rule.salesChannel(), rule.adjustmentType(), rule.adjustmentRate(),
                        rule.adjustmentAmountCent(), rule.minPriceCent(), rule.maxPriceCent(), "published",
                        rule.conditionRule(), "publish", List.of("att_publish"), rule.createdAt(), rule.updatedAt()
                ));
        when(service.simulateDynamicPricing(any(TicketDynamicPricingSimulationRequest.class)))
                .thenReturn(new TicketDynamicPricingSimulationResponse(
                        "ticket_type_1",
                        java.time.LocalDate.parse("2026-06-20"),
                        "web",
                        11900L,
                        2380L,
                        14280L,
                        "dyn_rule_1",
                        "matched dynamic pricing rule"
                ));

        mockMvc.perform(get("/api/v1/ticketing/dynamic-pricing-rules")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("dyn_rule_1"));
        mockMvc.perform(post("/api/v1/ticketing/dynamic-pricing-rules")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "dyn-create",
                                  "ticket_type_id": "ticket_type_1",
                                  "rule_name": "Summer uplift",
                                  "priority": 100,
                                  "effective_from": "2026-06-01",
                                  "effective_to": "2026-06-30",
                                  "sales_channel": "web",
                                  "adjustment_type": "percent_delta",
                                  "adjustment_rate": 20.00,
                                  "min_price_cent": 9900,
                                  "max_price_cent": 15000,
                                  "condition_rule": {"inventory_level": "low"}
                                }
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/ticketing/dynamic-pricing-rules/dyn_rule_1/publish")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "dyn-publish",
                                  "action": "publish",
                                  "reason": "approved"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("published"));
        mockMvc.perform(post("/api/v1/ticketing/dynamic-pricing/simulations")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "dyn-sim",
                                  "ticket_type_id": "ticket_type_1",
                                  "visit_date": "2026-06-20",
                                  "sales_channel": "web",
                                  "context": {"inventory_level": "low"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.final_price_cent").value(14280));
        mockMvc.perform(post("/api/v1/ticketing/dynamic-pricing/simulations")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticket_type_id": "ticket_type_1",
                                  "visit_date": "2026-06-20",
                                  "sales_channel": "web"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(authorizationService).requirePermission("ticket.dynamic_pricing.view");
        verify(authorizationService).requirePermission("ticket.dynamic_pricing.manage");
        verify(authorizationService).requirePermission("ticket.dynamic_pricing.publish");
        verify(authorizationService, times(2)).requirePermission("ticket.dynamic_pricing.simulate");
    }

    // ---------- XM-009（PGM-1.15.2.c2 / c3）：员工端订单列表与下单响应只出脱敏购票人手机号，不见明文 ----------
    @Test
    void listingTicketOrdersReturnsMaskedBuyerPhone() throws Exception {
        when(service.listTicketOrders()).thenReturn(new PageResponse<>(List.of(sampleTicketOrder()), 1, 50, 1));

        mockMvc.perform(get("/api/v1/ticketing/orders").with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("ticket_order_1"))
                .andExpect(jsonPath("$.data.items[0].buyer_phone").value("138****8000"));
    }

    @Test
    void creatingTicketOrderReturnsMaskedBuyerPhone() throws Exception {
        when(service.createTicketOrder(any(TicketOrderRequest.class))).thenReturn(sampleTicketOrder());

        mockMvc.perform(post("/api/v1/ticketing/orders")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "ticket-order-mask",
                                  "ticket_type_id": "ticket_type_1",
                                  "quantity": 2,
                                  "visit_date": "2026-06-20",
                                  "sales_channel": "web",
                                  "buyer_phone": "13800138000",
                                  "customer_id": "customer_1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("ticket_order_1"))
                .andExpect(jsonPath("$.data.buyer_phone").value("138****8000"));
    }

    private static TicketOrderRecord sampleTicketOrder() {
        return new TicketOrderRecord("ticket_order_1", "TO20260614000001", "customer_1", "ticket_type_1", null, 2,
                LocalDate.parse("2026-06-20"), "web", "13800138000", 19800L, null, 19800L, 0L, "CNY", "unpaid", "not_issued",
                "created", Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z"), null, null, null);
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        TicketingService ticketingService() {
            return Mockito.mock(TicketingService.class);
        }

        @Bean
        AuthorizationService authorizationService() {
            return Mockito.mock(AuthorizationService.class);
        }

        @Bean
        TicketFacePhotoService ticketFacePhotoService() {
            // 修既有基线断裂：控制器构造器新增 facePhotoService 后本切片未同步，12 条测试全数上下文加载失败。
            return Mockito.mock(TicketFacePhotoService.class);
        }

        @Bean
        AuthService authService() {
            return new AuthService("", "");
        }
    }
}
