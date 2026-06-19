package com.complex.platform.bar;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

@WebMvcTest(controllers = BarController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, BarControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class BarControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BarService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
        when(service.createAgeVerification(any(VerificationRequest.class))).thenReturn(ageVerification());
        when(service.calculateCommission(eq("bill_1"), any(CommissionCalculateRequest.class))).thenReturn(commission(1500, 19320L));
        when(service.createBillPayIntent(eq("bill_1"), any(PaymentIntentRequest.class))).thenReturn(paymentIntent());
        when(service.settleBill(eq("bill_1"), any(BarBillSettleRequest.class))).thenReturn(settledBill());
        when(service.applyBillAdjustment(eq("bill_1"), any(BarBillAdjustmentRequest.class))).thenReturn(billAdjustment());
        when(service.splitBill(eq("bill_1"), any(BarBillSplitRequest.class))).thenReturn(billOperation("split"));
        when(service.mergeBills(eq("bill_1"), any(BarBillMergeRequest.class))).thenReturn(billOperation("merge"));
        when(service.listBillOperations(eq("bill_1"))).thenReturn(new PageResponse<>(List.of(billOperation("split")), 1, 50, 1));
        when(service.listOrders()).thenReturn(new PageResponse<>(List.of(order()), 1, 50, 1));
        when(service.createOrder(any(BarOrderRequest.class))).thenReturn(order());
        when(service.createOrderBill(eq("order_1"), any(BarOrderBillCreateRequest.class))).thenReturn(bill());
        when(service.deliverOrder(eq("order_1"), any(BarOrderDeliveryRequest.class))).thenReturn(orderDelivered());
        when(service.addOrderItem(eq("order_1"), any(BarOrderRequest.class))).thenReturn(orderItem());
        when(service.listProductionTickets()).thenReturn(new PageResponse<>(List.of(ticket()), 1, 50, 1));
        when(service.listProductionTicketEvents(eq("ticket_1"))).thenReturn(new PageResponse<>(List.of(ticketEvent()), 1, 50, 1));
        when(service.transitionProductionTicket(eq("ticket_1"), any(BarProductionTicketTransitionRequest.class))).thenReturn(ticketReady());
        when(service.listProducts()).thenReturn(new PageResponse<>(List.of(product(false)), 1, 50, 1));
        when(service.createProduct(any(BarProductRequest.class))).thenReturn(product(false));
        when(service.listReservations()).thenReturn(new PageResponse<>(List.of(reservation()), 1, 50, 1));
        when(service.createReservation(any(BarReservationRequest.class))).thenReturn(reservation());
        when(service.checkInReservationGuest(eq("reservation_1"), any(VerificationRequest.class))).thenReturn(checkin());
        when(service.listTables()).thenReturn(new PageResponse<>(List.of(table()), 1, 50, 1));
        when(service.createTable(any(BarTableRequest.class))).thenReturn(table());
        when(service.openTable(eq("table_1"), any(OpenBarTableRequest.class))).thenReturn(openedTable());
        when(service.listTableSessions()).thenReturn(new PageResponse<>(List.of(tableSession()), 1, 50, 1));
        when(service.createTableSession(any(BarTableSessionRequest.class))).thenReturn(tableSession());
        when(service.transferTableSession(eq("session_1"), any(BarTableTransferRequest.class))).thenReturn(adjustment("transfer"));
        when(service.mergeTableSessions(eq("session_1"), any(BarTableMergeRequest.class))).thenReturn(adjustment("merge"));
        when(service.renewTableSession(eq("session_1"), any(BarTableRenewRequest.class))).thenReturn(adjustment("renew"));
    }

    @ParameterizedTest
    @CsvSource({
            "POST,/api/v1/bar/age-verifications,bar.entry.age_verify",
            "POST,/api/v1/bar/bills/bill_1/commissions/calculate,bar.table_commission.calculate",
            "POST,/api/v1/bar/bills/bill_1/pay-intents,finance.payment_intent.create",
            "POST,/api/v1/bar/bills/bill_1/settle,finance.payment.register",
            "POST,/api/v1/bar/bills/bill_1/adjustments,bar.bill.adjust",
            "POST,/api/v1/bar/bills/bill_1/split,bar.bill.split_merge",
            "POST,/api/v1/bar/bills/bill_1/merge,bar.bill.split_merge",
            "GET,/api/v1/bar/bills/bill_1/operations,bar.bill.split_merge",
            "GET,/api/v1/bar/orders,bar.order.view",
            "POST,/api/v1/bar/orders,bar.order.create",
            "POST,/api/v1/bar/orders/order_1/bill,bar.order.create",
            "POST,/api/v1/bar/orders/order_1/deliver,bar.order.create",
            "POST,/api/v1/bar/orders/order_1/items,bar.order.create",
            "GET,/api/v1/bar/production-tickets,bar.production_ticket.view",
            "GET,/api/v1/bar/production-tickets/ticket_1/events,bar.production_ticket.view",
            "POST,/api/v1/bar/production-tickets/ticket_1/transition,bar.production_ticket.manage",
            "GET,/api/v1/bar/products,bar.order.create",
            "POST,/api/v1/bar/products,bar.order.create",
            "GET,/api/v1/bar/reservations,bar.reservation.view",
            "POST,/api/v1/bar/reservations,bar.reservation.create",
            "POST,/api/v1/bar/reservations/reservation_1/check-in-guest,bar.table.guest_checkin",
            "GET,/api/v1/bar/tables,bar.table.view",
            "POST,/api/v1/bar/tables,bar.table.manage",
            "POST,/api/v1/bar/tables/table_1/open,bar.table.open_close",
            "GET,/api/v1/bar/table-sessions,bar.table_session.view",
            "POST,/api/v1/bar/table-sessions,bar.table_session.create",
            "POST,/api/v1/bar/table-sessions/session_1/transfer,bar.table_session.transfer",
            "POST,/api/v1/bar/table-sessions/session_1/merge,bar.table_session.merge",
            "POST,/api/v1/bar/table-sessions/session_1/renew,bar.table_session.renew"
    })
    void barRoutesUseExactOpenApiPermission(String method, String path, String permission) throws Exception {
        if ("GET".equals(method)) {
            mockMvc.perform(get(path).with(user("bar").roles("P0_SMOKE")))
                    .andExpect(status().isOk());
        } else {
            mockMvc.perform(post(path)
                            .with(user("bar").roles("P0_SMOKE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyFor(path, true)))
                    .andExpect(status().isOk());
        }

        verify(authorizationService).requirePermission(permission);
    }

    @ParameterizedTest
    @CsvSource({
            "/api/v1/bar/age-verifications",
            "/api/v1/bar/bills/bill_1/commissions/calculate",
            "/api/v1/bar/bills/bill_1/pay-intents",
            "/api/v1/bar/bills/bill_1/settle",
            "/api/v1/bar/bills/bill_1/adjustments",
            "/api/v1/bar/bills/bill_1/split",
            "/api/v1/bar/bills/bill_1/merge",
            "/api/v1/bar/orders",
            "/api/v1/bar/orders/order_1/bill",
            "/api/v1/bar/orders/order_1/deliver",
            "/api/v1/bar/orders/order_1/items",
            "/api/v1/bar/production-tickets/ticket_1/transition",
            "/api/v1/bar/products",
            "/api/v1/bar/reservations",
            "/api/v1/bar/reservations/reservation_1/check-in-guest",
            "/api/v1/bar/tables",
            "/api/v1/bar/tables/table_1/open",
            "/api/v1/bar/table-sessions",
            "/api/v1/bar/table-sessions/session_1/transfer",
            "/api/v1/bar/table-sessions/session_1/merge",
            "/api/v1/bar/table-sessions/session_1/renew"
    })
    void writeRoutesRejectMissingIdempotencyKey(String path) throws Exception {
        mockMvc.perform(post(path)
                        .with(user("bar").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyFor(path, false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("idempotency_key is required"));
    }

    private String bodyFor(String path, boolean withIdempotencyKey) {
        String idempotency = withIdempotencyKey ? "\"idempotency_key\":\"bar-once\"," : "";
        if (path.contains("table-sessions") && path.endsWith("/transfer")) {
            return "{" + idempotency + """
                    "target_table_id":"table_2","reason":"guest wants larger card seat","requested_by_user_id":"supervisor_1","session_version":1}
                    """;
        }
        if (path.contains("table-sessions") && path.endsWith("/merge")) {
            return "{" + idempotency + """
                    "merged_session_id":"session_2","reason":"two groups joined","requested_by_user_id":"supervisor_1","primary_session_version":1,"merged_session_version":1}
                    """;
        }
        if (path.contains("table-sessions") && path.endsWith("/renew")) {
            return "{" + idempotency + """
                    "renewed_until":"2026-06-21T02:00:00Z","additional_min_consumption_cent":20000,"additional_deposit_amount_cent":0,"additional_service_fee_cent":1000,"reason":"guest extends time","requested_by_user_id":"supervisor_1","session_version":1}
                    """;
        }
        if (path.endsWith("/table-sessions")) {
            return "{" + idempotency + """
                    "bar_id":"bar_1","merchant_id":"merchant_1","business_date":"2026-06-20","table_id":"table_1","customer_id":"customer_1","guest_count":2,"sales_staff_id":"staff_sales","service_staff_id":"staff_service","opened_by_user_id":"supervisor_1"}
                    """;
        }
        if (path.contains("age-verifications") || path.contains("check-in-guest")) {
            return "{" + idempotency + """
                    "credential_type":"manual","credential_payload":"guest_1","device_id":"dev_1","point_id":"bar_1","business_context":{"bar_id":"bar_1"}}
                    """;
        }
        if (path.contains("commissions")) {
            return "{" + idempotency + "\"bill_id\":\"bill_1\",\"include_only_table_sales\":true}";
        }
        if (path.contains("pay-intents")) {
            return "{" + idempotency + """
                    "business_order_type":"bar_bill","business_order_id":"bill_1","amount_cent":118800,"currency":"CNY","channel":"aggregate_pay"}
                    """;
        }
        if (path.contains("settle")) {
            return "{" + idempotency + "\"payment_id\":\"payment_1\",\"settlement_amount_cent\":118800,\"payment_channel\":\"cash\"}";
        }
        if (path.endsWith("/adjustments")) {
            return "{" + idempotency + """
                    "adjustment_type":"service_fee","amount_cent":12880,"reason":"10 percent service fee","requested_by_user_id":"supervisor_1","evidence_attachment_ids":["att_1"]}
                    """;
        }
        if (path.endsWith("/split")) {
            return "{" + idempotency + """
                    "split_amount_cent":40000,"reason":"guest wants separate payment","requested_by_user_id":"cashier_1","evidence_attachment_ids":["att_split"]}
                    """;
        }
        if (path.endsWith("/merge")) {
            return "{" + idempotency + """
                    "merged_bill_id":"bill_2","reason":"same party pays together","requested_by_user_id":"cashier_1","evidence_attachment_ids":["att_merge"]}
                    """;
        }
        if (path.endsWith("/bill")) {
            return "{" + idempotency + "\"payment_mode\":\"offline\",\"min_spend_fill_required\":false}";
        }
        if (path.contains("deliver")) {
            return "{" + idempotency + "\"delivered_items\":[{\"order_item_id\":\"item_1\",\"quantity\":1}],\"delivered_by_user_id\":\"staff_1\"}";
        }
        if (path.contains("production-tickets") && path.endsWith("/transition")) {
            return "{" + idempotency + """
                    "target_status":"ready","station_code":"bar-counter-1","operator_user_id":"staff_bar_1","operator_note":"ready","evidence_attachment_ids":["att_ready"]}
                    """;
        }
        if (path.contains("products")) {
            return "{" + idempotency + "\"bar_id\":\"bar_1\",\"product_name\":\"Whisky\",\"category\":\"spirit\",\"price_cent\":128800,\"status\":\"active\"}";
        }
        if (path.contains("reservations")) {
            return "{" + idempotency + """
                    "bar_id":"bar_1","table_id":"table_1","reservation_time":"2026-06-20T21:00:00Z","guest_count":2,"salesperson_user_id":"staff_sales"}
                    """;
        }
        if (path.contains("tables") && path.endsWith("/open")) {
            return "{" + idempotency + "\"reservation_id\":\"reservation_1\",\"opened_by_user_id\":\"staff_1\",\"guest_count\":2}";
        }
        if (path.contains("tables")) {
            return "{" + idempotency + """
                    "bar_id":"bar_1","table_name":"A1","table_type":"card_seat","minimum_spend_cent":50000,"allowed_free_entry_count":2,"status":"active"}
                    """;
        }
        return "{" + idempotency + """
                "bar_id":"bar_1","table_id":"table_1","reservation_id":"reservation_1","items":[{"product_id":"product_1","quantity":1}],"salesperson_user_id":"staff_sales","payment_mode":"offline"}
                """;
    }

    private BarTableRecord table() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new BarTableRecord("table_1", "bar_1", null, "T-bar_once", "A1", "card_seat", 2, 2, 50000L, "CNY", "active", now, now);
    }

    private BarTableRecord openedTable() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new BarTableRecord("table_1", "bar_1", null, "T-bar_once", "A1", "card_seat", 2, 2, 50000L, "CNY", "opened", now, now);
    }

    private BarReservationRecord reservation() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new BarReservationRecord("reservation_1", "bar_1", null, "table_1", "customer_1", now, 2, 2, 0L, "not_required", "staff", "staff_sales", "reserved", "web", null, List.of(), now, now, null);
    }

    private BarTableGuestCheckinRecord checkin() {
        return new BarTableGuestCheckinRecord("checkin_1", "reservation_1", "bar_1", "table_1", "guest_1", "accepted", 1, "dev_1", null, List.of(), Instant.parse("2026-06-14T00:00:00Z"));
    }

    private BarAgeVerificationRecord ageVerification() {
        return new BarAgeVerificationRecord("age_1", "bar_1", "guest_1", "dev_1", null, "passed", "placeholder", "accepted", List.of(), Instant.parse("2026-06-14T00:00:00Z"));
    }

    private BarProductRecord product(boolean stockManaged) {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new BarProductRecord("product_1", "bar_1", null, "Whisky", "spirit", 128800L, "CNY", stockManaged ? "inventory_1" : null, stockManaged ? "bar_1" : null, stockManaged, "active", now, now, null, null, null);
    }

    private BarOrderRecord order() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new BarOrderRecord("order_1", "bar_1", null, "table_1", "reservation_1", null, "staff", "created", 128800L, "CNY", "web", null, "staff_sales", now, now);
    }

    private BarOrderRecord orderDelivered() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new BarOrderRecord("order_1", "bar_1", null, "table_1", "reservation_1", null, "staff", "delivered", 128800L, "CNY", "web", null, "staff_sales", now, now);
    }

    private BarOrderItemRecord orderItem() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new BarOrderItemRecord("item_1", "order_1", "product_1", "Whisky", 1, 128800L, 128800L, "pending", now, now, null, 0L);
    }

    private BarProductionTicketRecord ticket() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new BarProductionTicketRecord("ticket_1", "order_1", "bar_1", null, "created", now, now);
    }

    private BarProductionTicketRecord ticketReady() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new BarProductionTicketRecord("ticket_1", "order_1", "bar_1", null, "ready", now, now);
    }

    private BarProductionTicketEventRecord ticketEvent() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new BarProductionTicketEventRecord(
                "event_1",
                "ticket_1",
                "order_1",
                "bar_1",
                "mark_ready",
                "preparing",
                "ready",
                "bar-counter-1",
                "staff_bar_1",
                "ready",
                "bar-once",
                List.of("att_ready"),
                now
        );
    }

    private BarBillRecord bill() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new BarBillRecord("bill_1", "BILL202606140001", "order_1", "bar_1", null, "table_1", "reservation_1", 128800L, 0L, 10000L, 118800L, 0L, "CNY", "unpaid", "unsettled", "staff_sales", "sales", now, now);
    }

    private BarBillRecord settledBill() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new BarBillRecord("bill_1", "BILL202606140001", "order_1", "bar_1", null, "table_1", "reservation_1", 128800L, 0L, 10000L, 118800L, 118800L, "CNY", "paid", "settled", "staff_sales", "sales", now, now);
    }

    private BarCommissionCalculationRecord commission(int rateBp, long amountCent) {
        return new BarCommissionCalculationRecord("commission_1", "bill_1", "staff_sales", "sales", 128800L, rateBp, amountCent, "calculated", Instant.parse("2026-06-14T00:00:00Z"));
    }

    private BarBillAdjustmentRecord billAdjustment() {
        return new BarBillAdjustmentRecord(
                "bill_adjustment_1",
                "bill_1",
                "service_fee",
                12880L,
                118800L,
                131680L,
                "10 percent service fee",
                "supervisor_1",
                true,
                "applied",
                "bar-once",
                List.of("att_1"),
                "arap_1",
                "{}",
                Instant.parse("2026-06-14T00:00:00Z"),
                null
        );
    }

    private BarBillOperationRecord billOperation(String type) {
        return new BarBillOperationRecord(
                "bill_operation_" + type,
                type,
                "bill_1",
                "merge".equals(type) ? "bill_2" : "bill_split_1",
                "split".equals(type) ? "bill_split_1" : null,
                40000L,
                type + " bill",
                "cashier_1",
                "applied",
                "bar-once",
                List.of("att_" + type),
                "{}",
                Instant.parse("2026-06-14T00:00:00Z")
        );
    }

    private PaymentIntentRecord paymentIntent() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new PaymentIntentRecord("pay_intent_1", "bar_bill", "bill_1", 118800L, "CNY", "aggregate_pay", "created", now, now);
    }

    private BarTableSessionRecord tableSession() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new BarTableSessionRecord(
                "session_1",
                "bar_1",
                "merchant_1",
                LocalDate.parse("2026-06-20"),
                "table_1",
                "reservation_1",
                "customer_1",
                2,
                50000L,
                2,
                "staff_sales",
                "staff_service",
                "open",
                now,
                null,
                null,
                1L,
                now,
                now
        );
    }

    private BarTableSessionAdjustmentRecord adjustment(String type) {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new BarTableSessionAdjustmentRecord(
                "adjustment_" + type,
                "session_1",
                type,
                "merge".equals(type) ? "session_2" : null,
                "merge".equals(type) ? "session_1" : null,
                "table_1",
                "transfer".equals(type) ? "table_2" : "table_1",
                "bar_1",
                "merchant_1",
                LocalDate.parse("2026-06-20"),
                50000L,
                "transfer".equals(type) ? 80000L : 50000L,
                "renew".equals(type) ? Instant.parse("2026-06-21T02:00:00Z") : null,
                "renew".equals(type) ? 20000L : 0L,
                0L,
                "renew".equals(type) ? 1000L : 0L,
                "reason",
                "supervisor_1",
                false,
                "applied",
                "bar-once",
                List.of(),
                "{}",
                now
        );
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        BarService barService() {
            return Mockito.mock(BarService.class);
        }

        @Bean
        AuthorizationService authorizationService() {
            return Mockito.mock(AuthorizationService.class);
        }

        @Bean
        AuthService authService() {
            return Mockito.mock(AuthService.class);
        }
    }
}
