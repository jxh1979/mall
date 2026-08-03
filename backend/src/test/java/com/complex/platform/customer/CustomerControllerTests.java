package com.complex.platform.customer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.auth.AuthService;
import com.complex.platform.bar.BarReservationRecord;
import com.complex.platform.common.api.GlobalExceptionHandler;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.config.SecurityConfig;
import com.complex.platform.customer.dto.CustomerBarReservationRequest;
import com.complex.platform.customer.dto.CustomerConsentRequest;
import com.complex.platform.customer.dto.CustomerPhoneLoginRequest;
import com.complex.platform.customer.dto.CustomerPointsRedemptionRequest;
import com.complex.platform.customer.dto.CustomerTicketOrderRequest;
import com.complex.platform.ticketing.TicketCredentialResponse;
import com.complex.platform.ticketing.TicketOrderRecord;
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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CustomerController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, CustomerControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class CustomerControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
    }

    @Test
    void phoneLoginIsPublicAndReturnsCustomerToken() throws Exception {
        when(service.phoneLogin(any(CustomerPhoneLoginRequest.class))).thenReturn(new CustomerLoginResponse(
                "customer_1",
                "138****8000",
                "cust_token_1",
                Instant.parse("2026-06-15T00:00:00Z")
        ));

        mockMvc.perform(post("/api/v1/customer/auth/phone-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "13800138000",
                                  "sms_code": "000000",
                                  "scene": "miniapp"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customer_token").value("cust_token_1"))
                .andExpect(jsonPath("$.data.masked_phone").value("138****8000"));

        verify(authorizationService, never()).requirePermission(any(String.class));
    }

    @Test
    void ticketProductsArePublicAndDoNotRequireStaffPermission() throws Exception {
        when(service.listTicketProducts(any())).thenReturn(new PageResponse<>(List.of(new CustomerTicketProductResponse(
                "ticket_type_1",
                "P0 Pass",
                9900L,
                11900L,
                12900L,
                "CNY",
                "published"
        )), 1, 50, 1));

        mockMvc.perform(get("/api/v1/customer/ticket-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].ticket_type_id").value("ticket_type_1"));

        verify(authorizationService, never()).requirePermission(any(String.class));
    }

    @Test
    void customerOwnedEndpointsUseCustomerTokenHeader() throws Exception {
        when(service.createTicketOrder(any(String.class), any(String.class), any(CustomerTicketOrderRequest.class))).thenReturn(new TicketOrderRecord(
                "ticket_order_1",
                "TO20260614000001",
                "customer_1",
                "ticket_type_1",
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "miniapp",
                "13800138000",
                9900L, null, 9900L, 0L,
                "CNY",
                "unpaid",
                "not_issued",
                "created",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        , null, null, null));
        when(service.listOrders("cust_token_1")).thenReturn(new PageResponse<>(List.of(new TicketOrderRecord(
                "ticket_order_1",
                "TO20260614000001",
                "customer_1",
                "ticket_type_1",
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "miniapp",
                "13800138000",
                9900L, null, 9900L, 0L,
                "CNY",
                "unpaid",
                "not_issued",
                "created",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        , null, null, null)), 1, 50, 1));
        when(service.getTicketQr("cust_token_1", "ent_1")).thenReturn(new TicketCredentialResponse(
                "credential_1",
                "ent_1",
                "ticket_order_1",
                "cred_token_1",
                "dynamic_qr",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:05:00Z"),
                1
        ));
        when(service.recordConsent(any(String.class), any(String.class), any(CustomerConsentRequest.class))).thenReturn(new CustomerConsentRecord(
                "consent_1",
                "customer_1",
                "privacy",
                true,
                "v1",
                "miniapp",
                Instant.parse("2026-06-14T00:00:00Z"),
                null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        when(service.getPoints("cust_token_1")).thenReturn(new CustomerPointsResponse(
                "customer_1",
                0L,
                "base",
                Map.of()
        ));

        mockMvc.perform(post("/api/v1/customer/ticket-orders")
                        .header("X-Customer-Token", "cust_token_1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "customer-order",
                                  "ticket_type_id": "ticket_type_1",
                                  "quantity": 1,
                                  "visit_date": "2026-06-20",
                                  "contact_phone": "13800138000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customer_id").value("customer_1"));
        mockMvc.perform(get("/api/v1/customer/orders")
                        .header("X-Customer-Token", "cust_token_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].customer_id").value("customer_1"));
        mockMvc.perform(get("/api/v1/customer/tickets/ent_1/qr")
                        .header("X-Customer-Token", "cust_token_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.credential_token").value("cred_token_1"));
        mockMvc.perform(post("/api/v1/customer/consents")
                        .header("X-Customer-Token", "cust_token_1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "consent",
                                  "consent_type": "privacy",
                                  "accepted": true,
                                  "version": "v1",
                                  "source_terminal": "miniapp"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true));
        mockMvc.perform(get("/api/v1/customer/points")
                        .header("X-Customer-Token", "cust_token_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.level").value("base"));

        verify(authorizationService, never()).requirePermission(any(String.class));
    }

    @Test
    void customerPrivateEndpointWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/customer/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_UNAUTHENTICATED"));
    }

    @Test
    void customerSessionProbeWithoutTokenReturnsNoContent() throws Exception {
        // #7：匿名探测「是否已登录」返回 204 空会话，不再 401 污染控制台。
        mockMvc.perform(get("/api/v1/customer/session"))
                .andExpect(status().isNoContent());
        // 不应查询会话（匿名短路，不触达 service）。
        verify(service, never()).sessionInfo(any(String.class));
    }

    @Test
    void customerSessionProbeWithTokenReturnsIdentity() throws Exception {
        when(service.sessionInfo("cust_token_1")).thenReturn(
                new CustomerSessionInfoResponse("cust_1", "138****5678", Instant.parse("2026-08-01T00:00:00Z")));
        mockMvc.perform(get("/api/v1/customer/session")
                        .header("X-Customer-Token", "cust_token_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customer_id").value("cust_1"))
                .andExpect(jsonPath("$.data.masked_phone").value("138****5678"));
    }

    @Test
    void customerBarReservationRoutesUseCustomerTokenAndDoNotRequireStaffPermission() throws Exception {
        when(service.listBarReservationOptions("cust_token_1")).thenReturn(new PageResponse<>(List.of(new CustomerBarReservationOptionResponse(
                "bar_1",
                "table_1",
                "card_seat",
                "VIP A1",
                6,
                2,
                50000L,
                "CNY",
                "active"
        )), 1, 50, 1));
        when(service.createBarReservation(any(String.class), any(String.class), any(CustomerBarReservationRequest.class)))
                .thenReturn(new BarReservationRecord(
                        "reservation_customer",
                        "bar_1",
                        null,
                        "table_1",
                        "customer_1",
                        Instant.parse("2026-06-20T21:00:00Z"),
                        2,
                        2,
                        0L,
                        "not_required",
                        "miniapp",
                        null,
                        "reserved",
                        "miniapp",
                        null,
                        List.of(),
                        Instant.parse("2026-06-14T00:00:00Z"),
                        Instant.parse("2026-06-14T00:00:00Z"),
                        null
                ));

        mockMvc.perform(get("/api/v1/customer/bar-reservation-options")
                        .header("X-Customer-Token", "cust_token_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].allowed_pass_free_guest_count").value(2));
        mockMvc.perform(post("/api/v1/customer/bar-reservations")
                        .header("X-Customer-Token", "cust_token_1")
                        .header("Idempotency-Key", "bar-reservation-once")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bar_id": "bar_1",
                                  "table_id": "table_1",
                                  "table_type": "card_seat",
                                  "reservation_time": "2026-06-20T21:00:00Z",
                                  "guest_count": 2,
                                  "contact_phone": "13800138000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customer_id").value("customer_1"))
                .andExpect(jsonPath("$.data.allowed_pass_free_guest_count").value(2));

        verify(authorizationService, never()).requirePermission(any(String.class));
    }

    @Test
    void customerPointsRedemptionRoutesUseCustomerTokenAndDoNotRequireStaffPermission() throws Exception {
        when(service.listPointsLedger("cust_token_1")).thenReturn(new PageResponse<>(List.of(new CustomerPointsLedgerRecord(
                "ledger_1",
                "customer_1",
                -120L,
                380L,
                "points_redemption",
                "redemption_1",
                "Redeemed Plush Gift",
                null,
                null,
                false,
                Instant.parse("2026-06-14T00:00:00Z")
        )), 1, 50, 1));
        when(service.listRedemptionItems("cust_token_1")).thenReturn(new PageResponse<>(List.of(new CustomerRedemptionCatalogItemRecord(
                "gift_1",
                "Plush Gift",
                "gift",
                120L,
                "item_gift",
                "gift_shelf",
                1,
                "active",
                null,
                null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        )), 1, 50, 1));
        when(service.redeemPoints(any(String.class), any(String.class), any(CustomerPointsRedemptionRequest.class)))
                .thenReturn(new CustomerPointsRedemptionRecord(
                        "redemption_1",
                        "customer_1",
                        "gift_1",
                        "Plush Gift",
                        1,
                        120L,
                        120L,
                        "item_gift",
                        "gift_shelf",
                        1,
                        "miniapp",
                        "pending_pickup",
                        Instant.parse("2026-06-14T00:00:00Z"),
                        Instant.parse("2026-06-14T00:00:00Z")
                ));
        when(service.listRedemptions("cust_token_1")).thenReturn(new PageResponse<>(List.of(new CustomerPointsRedemptionRecord(
                "redemption_1",
                "customer_1",
                "gift_1",
                "Plush Gift",
                1,
                120L,
                120L,
                "item_gift",
                "gift_shelf",
                1,
                "miniapp",
                "pending_pickup",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        )), 1, 50, 1));

        mockMvc.perform(get("/api/v1/customer/points/ledger")
                        .header("X-Customer-Token", "cust_token_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].points_delta").value(-120));
        mockMvc.perform(get("/api/v1/customer/points/redemption-items")
                        .header("X-Customer-Token", "cust_token_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].points_cost").value(120));
        mockMvc.perform(post("/api/v1/customer/points/redemptions")
                        .header("X-Customer-Token", "cust_token_1")
                        .header("Idempotency-Key", "redeem-once")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalog_item_id": "gift_1",
                                  "quantity": 1,
                                  "source_terminal": "miniapp"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_pickup"));
        mockMvc.perform(get("/api/v1/customer/points/redemptions")
                        .header("X-Customer-Token", "cust_token_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].catalog_item_id").value("gift_1"));

        verify(authorizationService, never()).requirePermission(any(String.class));
    }

    @Test
    void customerBarReservationWithoutIdempotencyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/customer/bar-reservations")
                        .header("X-Customer-Token", "cust_token_1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bar_id": "bar_1",
                                  "table_id": "table_1",
                                  "table_type": "card_seat",
                                  "reservation_time": "2026-06-20T21:00:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("请勿重复提交，请刷新后再试"));
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        CustomerService customerService() {
            return Mockito.mock(CustomerService.class);
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
