// 文件版本：V0.03
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
@Import({com.complex.platform.support.AuthAccessTestConfiguration.class, GlobalExceptionHandler.class, SecurityConfig.class, TicketingControllerTests.TestBeans.class})
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


    /**
     * TK-05：后台按前端真实请求形状（adminClient.mapKeys 转蛇形键）提交退款理由闭集与通票安全事件，控制器原样绑定、按权限码校验。
     */
    @Test
    void refundReasonAndSafetyEventRoutesBindFrontendSnakeCaseShape() throws Exception {
        TicketSafetyEventRecord event = new TicketSafetyEventRecord("tse_1", "prj_1", "ticket_order_1", "tkt_1", 2, null, null,
                null, "客人突发情绪失控", "场内冲突", "be_amusement", "refund", "pending", "ops", Instant.parse("2026-10-20T14:00:00Z"),
                null, null, null, null, null);
        when(service.requestTicketRefund(any(TicketRefundRequest.class))).thenReturn(new TicketRefundRecord("refund_1",
                "ticket_order_1", 9900L, "活动取消", "original", "requested", "ops", null, null,
                Instant.parse("2026-10-20T14:00:00Z"), Instant.parse("2026-10-20T14:00:00Z"), null, "event_cancelled", "be_amusement", false));
        when(service.requestTicketSafetyEvent(any(com.complex.platform.ticketing.dto.TicketSafetyEventRequest.class))).thenReturn(event);
        when(service.approveTicketSafetyEvent(any(String.class), any(StatusChangeRequest.class))).thenReturn(event);
        when(service.listTicketSafetyEvents()).thenReturn(new PageResponse<>(List.of(event), 1, 50, 1));

        mockMvc.perform(post("/api/v1/ticketing/refunds")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "refund-ec", "target_id": "ticket_order_1", "reason": "活动取消",
                                 "reason_code": "event_cancelled", "responsible_entity_id": "be_amusement",
                                 "ticket_instance_ids": ["tkt_1"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reason_code").value("event_cancelled"))
                .andExpect(jsonPath("$.data.responsible_entity_id").value("be_amusement"));
        mockMvc.perform(post("/api/v1/ticketing/safety-events")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "tse-req", "ticket_instance_id": "tkt_1", "slot_index": 2,
                                 "reason": "客人突发情绪失控", "situation": "场内冲突", "responsible_entity_id": "be_amusement"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("refund"));
        mockMvc.perform(post("/api/v1/ticketing/safety-events/tse_1/approve")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "tse-approve", "action": "approve", "reason": "核实属实"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/ticketing/safety-events").with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].slot_index").value(2));

        org.mockito.ArgumentCaptor<TicketRefundRequest> refund = org.mockito.ArgumentCaptor.forClass(TicketRefundRequest.class);
        verify(service).requestTicketRefund(refund.capture());
        org.assertj.core.api.Assertions.assertThat(refund.getValue().reasonCode()).isEqualTo("event_cancelled");
        org.assertj.core.api.Assertions.assertThat(refund.getValue().responsibleEntityId()).isEqualTo("be_amusement");
        org.assertj.core.api.Assertions.assertThat(refund.getValue().ticketInstanceIds()).containsExactly("tkt_1");
        org.mockito.ArgumentCaptor<com.complex.platform.ticketing.dto.TicketSafetyEventRequest> safety =
                org.mockito.ArgumentCaptor.forClass(com.complex.platform.ticketing.dto.TicketSafetyEventRequest.class);
        verify(service).requestTicketSafetyEvent(safety.capture());
        org.assertj.core.api.Assertions.assertThat(safety.getValue().ticketInstanceId()).isEqualTo("tkt_1");
        org.assertj.core.api.Assertions.assertThat(safety.getValue().slotIndex()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(safety.getValue().situation()).isEqualTo("场内冲突");
        org.assertj.core.api.Assertions.assertThat(safety.getValue().responsibleEntityId()).isEqualTo("be_amusement");
        verify(service).approveTicketSafetyEvent(org.mockito.ArgumentMatchers.eq("tse_1"), any(StatusChangeRequest.class));
        verify(authorizationService, times(2)).requirePermission("ticket.refund.request");
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
        when(service.listTicketOrders(null)).thenReturn(new PageResponse<>(List.of(sampleTicketOrder()), 1, 50, 1));

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
        AuthService authService() {
            return new AuthService("", "");
        }
    }
}
