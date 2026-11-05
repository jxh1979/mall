// 文件版本：V0.07
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
import org.junit.jupiter.api.Test;
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
@Import({com.complex.platform.support.AuthAccessTestConfiguration.class, GlobalExceptionHandler.class, SecurityConfig.class, BarControllerTests.TestBeans.class})
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
    private BarEntitlementRedemptionService redemptionService;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, redemptionService, authorizationService);
        when(service.calculateCommission(eq("bill_1"), any(CommissionCalculateRequest.class))).thenReturn(commission(1500, 19320L));
        when(service.createBillPayIntent(eq("bill_1"), any(PaymentIntentRequest.class))).thenReturn(paymentIntent());
        when(service.settleBill(eq("bill_1"), any(BarBillSettleRequest.class))).thenReturn(settledBill());
        when(service.applyBillAdjustment(eq("bill_1"), any(BarBillAdjustmentRequest.class))).thenReturn(billAdjustment());
        when(service.splitBill(eq("bill_1"), any(BarBillSplitRequest.class))).thenReturn(billOperation("split"));
        when(service.mergeBills(eq("bill_1"), any(BarBillMergeRequest.class))).thenReturn(billOperation("merge"));
        when(service.listBillOperations(eq("bill_1"))).thenReturn(new PageResponse<>(List.of(billOperation("split")), 1, 50, 1));
        when(service.listOrders()).thenReturn(new PageResponse<>(List.of(BarOrderView.of(order(), List.of(), List.of())), 1, 50, 1));
        when(service.createOrder(any(BarOrderRequest.class))).thenReturn(order());
        when(service.createOrderBill(eq("order_1"), any(BarOrderBillCreateRequest.class))).thenReturn(bill());
        when(service.deliverOrder(eq("order_1"), any(BarOrderDeliveryRequest.class))).thenReturn(orderDelivered());
        when(service.addOrderItem(eq("order_1"), any(BarOrderRequest.class))).thenReturn(orderItem());
        when(service.listProductionTickets()).thenReturn(new PageResponse<>(List.of(ticket()), 1, 50, 1));
        when(service.listProductionTicketEvents(eq("ticket_1"))).thenReturn(new PageResponse<>(List.of(ticketEvent()), 1, 50, 1));
        when(service.transitionProductionTicket(eq("ticket_1"), any(BarProductionTicketTransitionRequest.class))).thenReturn(ticketReady());
        when(service.listProducts(false)).thenReturn(new PageResponse<>(List.of(product(false)), 1, 50, 1));
        when(service.createProduct(any(BarProductRequest.class))).thenReturn(product(false));
        when(service.listReservations(false)).thenReturn(new PageResponse<>(List.of(reservation()), 1, 50, 1));
        when(service.createReservation(any(BarReservationRequest.class))).thenReturn(reservation());
        when(service.listTables()).thenReturn(new PageResponse<>(List.of(table()), 1, 50, 1));
        when(service.createTable(any(BarTableRequest.class))).thenReturn(table());
        when(service.openTable(eq("table_1"), any(OpenBarTableRequest.class))).thenReturn(openedTable());
        when(service.clearTable(eq("table_1"), any(BarTableClearRequest.class))).thenReturn(table());
        when(service.listTableSessions()).thenReturn(new PageResponse<>(List.of(tableSession()), 1, 50, 1));
        when(service.createTableSession(any(BarTableSessionRequest.class))).thenReturn(tableSession());
        when(service.transferTableSession(eq("session_1"), any(BarTableTransferRequest.class))).thenReturn(adjustment("transfer"));
        when(service.mergeTableSessions(eq("session_1"), any(BarTableMergeRequest.class))).thenReturn(adjustment("merge"));
        when(service.splitTableSessions(eq("session_1"), any(BarTableSplitRequest.class))).thenReturn(adjustment("split"));
        when(redemptionService.redeemDrink(any(BarEntitlementDrinkRedemptionRequest.class))).thenReturn(giftRedemption());
        when(redemptionService.listRedemptions()).thenReturn(new PageResponse<>(List.of(giftRedemption()), 1, 50, 1));
    }

    @ParameterizedTest
    @CsvSource({
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
            "GET,/api/v1/bar/products,bar.product.view",
            "POST,/api/v1/bar/products,bar.product.manage",
            "GET,/api/v1/bar/reservations,bar.reservation.view",
            "POST,/api/v1/bar/reservations,bar.reservation.create",
            "POST,/api/v1/bar/reservations/reservation_1/invitation,bar.reservation.invitation.manage",
            "GET,/api/v1/bar/reservations/reservation_1/invitation,bar.reservation.view",
            "POST,/api/v1/bar/reservations/reservation_1/invitation/revoke,bar.reservation.invitation.manage",
            "POST,/api/v1/bar/reservations/reservation_1/invitation/entry-verify,bar.table.guest_checkin",
            "GET,/api/v1/bar/tables,bar.table.view",
            "POST,/api/v1/bar/tables,bar.table.manage",
            "POST,/api/v1/bar/tables/table_1/open,bar.table.open_close",
            "POST,/api/v1/bar/tables/table_1/clear,bar.table.open_close",
            "GET,/api/v1/bar/table-sessions,bar.table_session.view",
            "POST,/api/v1/bar/table-sessions,bar.table_session.create",
            "POST,/api/v1/bar/table-sessions/session_1/transfer,bar.table_session.transfer",
            "POST,/api/v1/bar/table-sessions/session_1/merge,bar.table_session.merge",
            "POST,/api/v1/bar/table-sessions/session_1/split,bar.table_session.split",
            "POST,/api/v1/bar/entitlement-redemptions/drink,bar.entitlement.redeem",
            "GET,/api/v1/bar/entitlement-redemptions,bar.entitlement.read"
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
            "/api/v1/bar/reservations/reservation_1/invitation",
            "/api/v1/bar/reservations/reservation_1/invitation/revoke",
            "/api/v1/bar/reservations/reservation_1/invitation/entry-verify",
            "/api/v1/bar/tables",
            "/api/v1/bar/tables/table_1/open",
            "/api/v1/bar/tables/table_1/clear",
            "/api/v1/bar/table-sessions",
            "/api/v1/bar/table-sessions/session_1/transfer",
            "/api/v1/bar/table-sessions/session_1/merge",
            "/api/v1/bar/table-sessions/session_1/split",
            "/api/v1/bar/entitlement-redemptions/drink"
    })
    void writeRoutesRejectMissingIdempotencyKey(String path) throws Exception {
        mockMvc.perform(post(path)
                        .with(user("bar").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyFor(path, false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("请勿重复提交，请刷新后再试"));
    }



    @Test
    void createOrderAcceptsAdminWebSnakeCaseBodyAndKeepsReservationBinding() throws Exception {
        // 第十批 TK-24-R3：后台 adminClient.mapKeys 蛇形体（table_id / reservation_id / items[].product_id）落单，
        // 服务层随后按订台定金达标判退（BarServiceTests.depositRefundsOnThresholdOnceAndKeepsResultAcrossMerge）。
        mockMvc.perform(post("/api/v1/bar/orders")
                        .with(user("bar").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key":"order-deposit-web","operator_note":"web order","evidence_attachment_ids":[],"bar_id":"bar_1","table_id":"table_1","reservation_id":"reservation_1","items":[{"product_id":"product_1","quantity":2}],"service_user_ids":[],"payment_mode":"offline"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("order_1"));

        org.mockito.ArgumentCaptor<BarOrderRequest> captor = org.mockito.ArgumentCaptor.forClass(BarOrderRequest.class);
        verify(service).createOrder(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().tableId()).isEqualTo("table_1");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().reservationId()).isEqualTo("reservation_1");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().items().getFirst())
                .containsEntry("product_id", "product_1").containsEntry("quantity", 2);
    }

    @Test
    void deliverAcceptsSnakeCaseDeliveredItemsFromAdminWeb() throws Exception {
        // 第十批 TK-26-R2：后台 adminClient.mapKeys 蛇形体 delivered_items[].order_item_id（按明细勾选送达）；
        // 部分明细送达时服务层不置整单已送达（BarServiceTests.partialDeliveryKeepsOrderAndTicketUndelivered），接口原样返回在途状态。
        when(service.deliverOrder(eq("order_1"), any(BarOrderDeliveryRequest.class))).thenReturn(order());
        mockMvc.perform(post("/api/v1/bar/orders/order_1/deliver")
                        .with(user("bar").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key":"deliver-web-partial","operator_note":"完整后台验收出品","delivery_photo_attachment_ids":["att-admin-delivery-1"],"delivered_items":[{"order_item_id":"item_1","quantity":2}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("created"));

        org.mockito.ArgumentCaptor<BarOrderDeliveryRequest> captor = org.mockito.ArgumentCaptor.forClass(BarOrderDeliveryRequest.class);
        verify(service).deliverOrder(eq("order_1"), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().deliveredItems()).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().deliveredItems().getFirst())
                .containsEntry("order_item_id", "item_1").containsEntry("quantity", 2);
    }

    // 第十批 TK-28：结账弹窗按前端真实请求形状（adminClient.mapKeys 蛇形）带 voucher_code，服务端收到折扣券码。
    @Test
    void settleBillAcceptsSnakeCaseVoucherCodeFromWeb() throws Exception {
        mockMvc.perform(post("/api/v1/bar/bills/bill_1/settle")
                        .with(user("bar").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key":"settle-web-voucher","payment_id":"payment_1","payment_channel":"aggregate_pay","voucher_code":"VCAAA","operator_note":"按本次真实收款凭据登记，多渠道累计足额后完成账单"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlement_status").value("settled"));

        org.mockito.ArgumentCaptor<BarBillSettleRequest> captor = org.mockito.ArgumentCaptor.forClass(BarBillSettleRequest.class);
        verify(service).settleBill(eq("bill_1"), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().voucherCode()).isEqualTo("VCAAA");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().voucherInstanceId()).isNull();
        org.assertj.core.api.Assertions.assertThat(captor.getValue().settlementAmountCent()).isNull();
    }

    @Test
    void listEntitlementRedemptionsReturnsGiftLedger() throws Exception {
        mockMvc.perform(get("/api/v1/bar/entitlement-redemptions")
                        .with(user("bar").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].verification_id").value("ver_1"));

        verify(authorizationService).requirePermission("bar.entitlement.read");
        verify(redemptionService).listRedemptions();
    }

    @Test
    void redeemDrinkReturnsExistingGiftRedemptionRecord() throws Exception {
        mockMvc.perform(post("/api/v1/bar/entitlement-redemptions/drink")
                        .with(user("bar").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key":"bar-gift-once","verification_id":"ver_1","operator_note":"guest claims gift drink","evidence_attachment_ids":["att_1"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verification_id").value("ver_1"))
                .andExpect(jsonPath("$.data.status").value("delivered"));

        verify(authorizationService).requirePermission("bar.entitlement.redeem");
        verify(redemptionService).redeemDrink(any(BarEntitlementDrinkRedemptionRequest.class));
    }

    @Test
    void createProductRejectsCatalogRefWithoutInternalPricePermission() throws Exception {
        // AUD-043 写侧门控（批10/E 收编后语义不变）：持 bar.product.manage 但无内部价专用权限者，
        // 挂内部结算 SKU 目录引用（=建立成本价基）被拒。
        when(authorizationService.currentActorHasPermission("bar.internal_settlement_price.view")).thenReturn(false);
        mockMvc.perform(post("/api/v1/bar/products")
                        .with(user("bar").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"bar-ip-deny\",\"bar_id\":\"bar_1\",\"product_name\":\"Whisky\",\"category\":\"spirit\",\"price_cent\":128800,\"catalog_item_code\":\"sku_x\",\"status\":\"active\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("BAR_INTERNAL_PRICE_FORBIDDEN"));
        verify(service, org.mockito.Mockito.never()).createProduct(any(BarProductRequest.class));
    }

    @Test
    void createProductAllowsCatalogRefWithInternalPricePermission() throws Exception {
        // 持内部价专用权限者可挂目录引用（内部价本体在目录里走草案复核，商品侧只建立引用）。
        when(authorizationService.currentActorHasPermission("bar.internal_settlement_price.view")).thenReturn(true);
        mockMvc.perform(post("/api/v1/bar/products")
                        .with(user("bar").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"bar-ip-ok\",\"bar_id\":\"bar_1\",\"product_name\":\"Whisky\",\"category\":\"spirit\",\"price_cent\":128800,\"catalog_item_code\":\"sku_x\",\"status\":\"active\"}"))
                .andExpect(status().isOk());
        verify(service).createProduct(any(BarProductRequest.class));
    }

    private String bodyFor(String path, boolean withIdempotencyKey) {
        String idempotency = withIdempotencyKey ? "\"idempotency_key\":\"bar-once\"," : "";
        if (path.contains("entitlement-redemptions")) {
            return "{" + idempotency + "\"verification_id\":\"ver_1\",\"operator_note\":\"guest claims gift drink\",\"evidence_attachment_ids\":[\"att_1\"]}";
        }
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
        if (path.endsWith("/table-sessions")) {
            return "{" + idempotency + """
                    "bar_id":"bar_1","merchant_id":"merchant_1","business_date":"2026-06-20","table_id":"table_1","customer_id":"customer_1","guest_count":2,"sales_staff_id":"staff_sales","service_staff_id":"staff_service","opened_by_user_id":"supervisor_1"}
                    """;
        }
        if (path.contains("age-verifications")) {
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
        if (path.contains("table-sessions") && path.endsWith("/split")) {
            return "{" + idempotency + """
                    "mode":"last_merge","order_assignments":[{"order_id":"order_9","session_id":"session_2"}],"reason":"guests part ways","requested_by_user_id":"supervisor_1","primary_session_version":2}
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
        if (path.contains("tables") && path.endsWith("/clear")) {
            return "{" + idempotency + "\"operator_note\":\"清理完成，登记恢复空闲\"}";
        }
        if (path.contains("tables") && path.endsWith("/open")) {
            return "{" + idempotency + "\"reservation_id\":\"reservation_1\",\"opened_by_user_id\":\"staff_1\",\"guest_count\":2}";
        }
        if (path.contains("tables")) {
            return "{" + idempotency + """
                    "bar_id":"bar_1","table_name":"A1","table_type":"card_seat","minimum_spend_cent":50000,"capacity":2,"status":"active"}
                    """;
        }
        return "{" + idempotency + """
                "bar_id":"bar_1","table_id":"table_1","reservation_id":"reservation_1","items":[{"product_id":"product_1","quantity":1}],"payment_mode":"offline"}
                """;
    }


    private BarGiftRedemptionRecord giftRedemption() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new BarGiftRedemptionRecord(
                "gift_1",
                "ent_1",
                "ver_1",
                "order_gift_1",
                "bar_1",
                "merchant_1",
                "sku_beer",
                "movement_1",
                "delivered",
                "gift redeemed; stock decreased via gift_claim_consume",
                now,
                now
        );
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


    private BarProductRecord product(boolean stockManaged) {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new BarProductRecord("product_1", "bar_1", null, "Whisky", "spirit", 128800L, "CNY", stockManaged ? "inventory_1" : null, stockManaged ? "bar_1" : null, stockManaged, "active", now, now, null, null, null, false, null, null, null);
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
                "marketing",
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
                null,
                0L,
                0L,
                0L,
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
        // 第十八批 N 段第 124 条 124-R5：退酒原因码写口经 AuditedOperationExecutor.recordingFailure 落失败事件、成功后落成功事件，切片里给桩
        @Bean
        com.complex.platform.evidence.AuditEventService auditEventService() {
            return Mockito.mock(com.complex.platform.evidence.AuditEventService.class);
        }

        @Bean
        com.complex.platform.evidence.AuditedOperationExecutor auditedOperationExecutor(com.complex.platform.evidence.AuditEventService audit) {
            return new com.complex.platform.evidence.AuditedOperationExecutor(null, audit);
        }
        @Bean
        BarService barService() {
            return Mockito.mock(BarService.class);
        }

        @Bean
        BarEntitlementRedemptionService barEntitlementRedemptionService() {
            return Mockito.mock(BarEntitlementRedemptionService.class);
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
