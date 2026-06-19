package com.complex.platform.bar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.TraceabilityRepository;
import com.complex.platform.evidence.TraceabilityService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.ticketing.dto.VerificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BarServiceTests {

    private InMemoryBarRepository barRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private CapturingBarReceivableLedgerService receivableLedgerService;
    private AuthorizationService authorizationService;
    private BarService service;

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
        barRepository = new InMemoryBarRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        receivableLedgerService = new CapturingBarReceivableLedgerService();
        AccessRepository accessRepository = mock(AccessRepository.class);
        when(accessRepository.permissionExists(anyString())).thenReturn(true);
        authorizationService = new AuthorizationService(accessRepository);
        AuditEventService auditEventService = new AuditEventService(auditEventRepository);
        IdempotencyService idempotencyService = new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper());
        AuditedOperationExecutor operations = new AuditedOperationExecutor(idempotencyService, auditEventService);
        BarEntryService entryService = new BarEntryService(barRepository, authorizationService, auditEventService);
        service = new BarService(
                barRepository,
                new BarOperationExecutor(operations),
                new BarBillingService(barRepository, receivableLedgerService, authorizationService, auditEventService),
                new BarShiftService(barRepository, authorizationService, auditEventService),
                new BarTableSessionService(barRepository, authorizationService),
                new BarOrderService(barRepository, authorizationService, auditEventService,
                        new TraceabilityService(mock(TraceabilityRepository.class), authorizationService),
                        receivableLedgerService),
                entryService,
                new BarAgeVerificationService(idempotencyService, auditEventService, entryService),
                authorizationService
        );
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // P0-2: settleBill state guard and amount cap
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void settleBillRejectsAlreadySettledBillToPreventDoubleSettlement() {
        BarBillRecord bill = billForOrder("already-settled", "staff_sales");
        service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-first", "first settlement", List.of(), "pay_1",
                bill.payableAmountCent(), "aggregate_pay"
        ));

        assertThatThrownBy(() -> service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-second", "duplicate settlement", List.of(), "pay_2",
                bill.payableAmountCent(), "aggregate_pay"
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_BILL_ALREADY_SETTLED");
    }

    @Test
    void settleBillRejectsAmountExceedingPayableToPreventOverpaymentManipulation() {
        BarBillRecord bill = billForOrder("overpay-settle", "staff_sales");

        assertThatThrownBy(() -> service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-overpay", "overpay", List.of(), "pay_overpay",
                bill.payableAmountCent() + 100_00L, "aggregate_pay"
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // P1-1: list interface tenant isolation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void listTablesReturnsOnlyCurrentMerchantRecordsWhenScopedToMerchant() {
        TestSecurityActors.asMerchantUser("merchant_a");
        BarTableRecord merchantATable = service.createTable(new BarTableRequest(
                "table-isolation-a", "create", List.of(), "bar_1", "VIP A", "card_seat", 50000L, 2, "active"
        ));

        TestSecurityActors.asMerchantUser("merchant_b");
        service.createTable(new BarTableRequest(
                "table-isolation-b", "create", List.of(), "bar_1", "VIP B", "card_seat", 60000L, 2, "active"
        ));

        TestSecurityActors.asMerchantUser("merchant_a");
        PageResponse<BarTableRecord> result = service.listTables();

        assertThat(result.items())
                .extracting(BarTableRecord::id)
                .containsExactly(merchantATable.id());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 处理清单 #8：bar_tables 空间指派/回填（over_space 依赖 space_id）
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void assignTableSpaceSetsSpaceIdAndIsReadableForBacklog() {
        TestSecurityActors.asMerchantUser("merchant_a");
        BarTableRecord table = service.createTable(new BarTableRequest(
                "table-space-assign", "create", List.of(), "bar_1", "VIP S", "card_seat", 50000L, 2, "active"));
        barRepository.registerSpace("space_a", "merchant_a");

        // 回填前：出现在「未回填空间」清单。
        assertThat(service.listTablesWithoutSpace().items())
                .extracting(BarTableSpaceBacklogRecord::id)
                .contains(table.id());

        BarTableSpaceAssignmentRecord assigned = service.assignTableSpace(table.id(),
                new BarTableSpaceAssignmentRequest("assign-key", "space_a", "运营回填", List.of()));
        assertThat(assigned.spaceId()).isEqualTo("space_a");
        // 回读 bar_tables.space_id 命中目标空间。
        assertThat(barRepository.tableSpaceId(table.id())).isEqualTo("space_a");
        // 回填后：不再出现在「未回填空间」清单。
        assertThat(service.listTablesWithoutSpace().items())
                .extracting(BarTableSpaceBacklogRecord::id)
                .doesNotContain(table.id());
    }

    @Test
    void assignTableSpaceRejectsCrossMerchantSpaceWithForbiddenError() {
        TestSecurityActors.asMerchantUser("merchant_a");
        BarTableRecord table = service.createTable(new BarTableRequest(
                "table-space-cross", "create", List.of(), "bar_1", "VIP X", "card_seat", 50000L, 2, "active"));
        barRepository.registerSpace("space_b", "merchant_b");

        assertThatThrownBy(() -> service.assignTableSpace(table.id(),
                new BarTableSpaceAssignmentRequest("assign-cross", "space_b", null, List.of())))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void assignTableSpaceRejectsMissingSpaceWithNotFoundError() {
        TestSecurityActors.asMerchantUser("merchant_a");
        BarTableRecord table = service.createTable(new BarTableRequest(
                "table-space-missing", "create", List.of(), "bar_1", "VIP M", "card_seat", 50000L, 2, "active"));

        assertThatThrownBy(() -> service.assignTableSpace(table.id(),
                new BarTableSpaceAssignmentRequest("assign-missing", "space_ghost", null, List.of())))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SPACE_NOT_FOUND");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // P1-2: by-ID writes verify resource ownership
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void settleBillRejectsCrossMerchantAccessWithForbiddenError() {
        TestSecurityActors.asMerchantUser("merchant_a");
        BarBillRecord bill = billForOrder("cross-merchant-settle", "staff_sales");

        TestSecurityActors.asMerchantUser("merchant_b");
        assertThatThrownBy(() -> service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-cross", "cross settle", List.of(), "pay_x", bill.payableAmountCent(), "aggregate_pay"
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void deliverOrderRejectsCrossMerchantAccessWithForbiddenError() {
        TestSecurityActors.asMerchantUser("merchant_a");
        BarReservationRecord reservation = createReservation("cross-merchant-deliver", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-cross-deliver", "create", List.of(), "bar_1", "Gin", "spirit", 8800L,
                null, null, "active", null, null, false
        ));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-cross-deliver", "create", List.of(), "bar_1", reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)), null, List.of(), "offline"
        ));
        String itemId = barRepository.listOrderItems(order.id()).getFirst().id();

        TestSecurityActors.asMerchantUser("merchant_b");
        assertThatThrownBy(() -> service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-cross", "cross deliver", List.of(),
                List.of(Map.of("order_item_id", itemId, "quantity", 1)), "staff_1", List.of()
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // P1-3: audit attribution comes from auth token, not request body
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void billAdjustmentRecordsAuthenticatedActorAsRequestedByUserIdIgnoringRequestBody() {
        BarBillRecord bill = billForOrder("audit-actor", "staff_sales");

        BarBillAdjustmentRecord adjustment = service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-audit-actor",
                        "test audit actor",
                        List.of(),
                        "service_fee",
                        5000L,
                        "audit test",
                        "injected_attacker_id"
                )
        );

        assertThat(adjustment.requestedByUserId())
                .as("requestedByUserId must come from the auth token, not the request body")
                .isEqualTo("usr_admin");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Existing tests — preserved intact with updated assertions
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void checkInConsumesOnlyReservationFreeGuestQuotaAndRejectsExhaustion() {
        BarReservationRecord reservation = createReservation("reservation-quota", "bar_1", 1);

        BarTableGuestCheckinRecord first = service.checkInReservationGuest(
                reservation.id(),
                verification("checkin-1", "guest_1", "bar_1")
        );

        assertThat(first.checkedInCountAfter()).isEqualTo(1);
        assertThatThrownBy(() -> service.checkInReservationGuest(
                reservation.id(),
                verification("checkin-2", "guest_2", "bar_1")
        ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("FREE_GUEST_QUOTA_EXHAUSTED");
        assertThat(barRepository.guestCheckins).hasSize(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.table_guest.checked_in", "bar.table_guest.rejected");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // R5 T16 — 订台定金闭环
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void createReservationComputesRatioDepositPendingFromMinConsumption() {
        // 规则 min_consumption_ratio、ratio=30%、桌台低消 1000 元(=100000分) → 定金 300 元(=30000分)、status=pending（非0）。
        service.createDepositRule(new BarDepositRuleRequest(
                "deposit-rule-ratio", "rule", List.of(), "bar_t16",
                "bar", "bar_t16", "min_consumption_ratio", null, 3000, 60));
        BarTableRecord table = service.createTable(new BarTableRequest(
                "table-t16-ratio", "create table", List.of(), "bar_t16",
                "VIP RATIO", "card_seat", 100000L, 2, "active"));
        BarReservationRecord reservation = service.createReservation(new BarReservationRequest(
                "resv-t16-ratio", "create reservation", List.of(), "bar_t16",
                table.id(), Instant.parse("2026-06-20T21:00:00Z"), 2, "staff_sales", null));

        assertThat(reservation.depositAmountCent()).isEqualTo(30000L);
        assertThat(reservation.depositStatus()).isEqualTo("pending");
        assertThat(reservation.depositIntentId()).isNotNull();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.reservation_deposit_required");
    }

    @Test
    void createReservationComputesFixedDepositPending() {
        service.createDepositRule(new BarDepositRuleRequest(
                "deposit-rule-fixed", "rule", List.of(), "bar_t16f",
                "bar", "bar_t16f", "fixed", 20000L, null, 30));
        BarTableRecord table = service.createTable(new BarTableRequest(
                "table-t16-fixed", "create table", List.of(), "bar_t16f",
                "VIP FIXED", "card_seat", 100000L, 2, "active"));
        BarReservationRecord reservation = service.createReservation(new BarReservationRequest(
                "resv-t16-fixed", "create reservation", List.of(), "bar_t16f",
                table.id(), Instant.parse("2026-06-20T21:00:00Z"), 2, "staff_sales", null));

        assertThat(reservation.depositAmountCent()).isEqualTo(20000L);
        assertThat(reservation.depositStatus()).isEqualTo("pending");
    }

    @Test
    void createReservationWithoutRuleKeepsBackwardCompatibleNotRequired() {
        BarReservationRecord reservation = createReservation("resv-t16-none", "bar_norule", 2);
        assertThat(reservation.depositAmountCent()).isEqualTo(0L);
        assertThat(reservation.depositStatus()).isEqualTo("not_required");
        assertThat(reservation.depositIntentId()).isNull();
    }

    @Test
    void payDepositThenCheckInSettlesAsDeducted() {
        BarReservationRecord reservation = reservationWithFixedDeposit("settle", 20000L, 30);

        BarReservationRecord paid = service.payReservationDeposit(reservation.id(),
                new BarReservationDepositActionRequest("pay-settle", "pay", List.of(), "aggregate_pay"));
        assertThat(paid.depositStatus()).isEqualTo("paid");
        assertThat(paid.depositPaidAt()).isNotNull();

        BarReservationRecord checkedIn = service.checkInReservationDeposit(reservation.id(),
                new BarReservationDepositActionRequest("checkin-settle", "checkin", List.of(), null));
        assertThat(checkedIn.depositStatus()).isEqualTo("settled");
        assertThat(checkedIn.depositSettlement()).isEqualTo("deducted");
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName)
                .contains("bar.reservation_deposit_paid", "bar.reservation_deposit_deducted");
    }

    @Test
    void cancelBeforeFreeWindowRefundsDeposit() {
        // 免没收时限 30 分钟，预约在远未来 → 取消属早于时限 → 退定金 refunded。
        BarReservationRecord reservation = reservationWithFixedDeposit("refund", 20000L, 30);
        service.payReservationDeposit(reservation.id(),
                new BarReservationDepositActionRequest("pay-refund", "pay", List.of(), "aggregate_pay"));

        BarReservationRecord cancelled = service.cancelReservation(reservation.id(),
                new BarReservationCancelRequest("cancel-refund", "cancel", List.of(), false, "customer"));
        assertThat(cancelled.status()).isEqualTo("cancelled");
        assertThat(cancelled.depositSettlement()).isEqualTo("refunded");
        assertThat(cancelled.depositStatus()).isEqualTo("settled");
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName)
                .contains("bar.reservation_deposit_refunded");
    }

    @Test
    void noShowForfeitsDeposit() {
        BarReservationRecord reservation = reservationWithFixedDeposit("forfeit", 20000L, 30);
        service.payReservationDeposit(reservation.id(),
                new BarReservationDepositActionRequest("pay-forfeit", "pay", List.of(), "aggregate_pay"));

        BarReservationRecord cancelled = service.cancelReservation(reservation.id(),
                new BarReservationCancelRequest("cancel-forfeit", "no-show", List.of(), true, "no_show"));
        assertThat(cancelled.depositSettlement()).isEqualTo("forfeited");
        assertThat(cancelled.depositStatus()).isEqualTo("settled");
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName)
                .contains("bar.reservation_deposit_forfeited");
    }

    @Test
    void payDepositRejectsWhenNotPending() {
        BarReservationRecord reservation = createReservation("resv-t16-nopay", "bar_norule", 2);
        assertThatThrownBy(() -> service.payReservationDeposit(reservation.id(),
                new BarReservationDepositActionRequest("pay-bad", "pay", List.of(), "aggregate_pay")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_DEPOSIT_NOT_PAYABLE");
    }

    private BarReservationRecord reservationWithFixedDeposit(String key, long fixedAmountCent, int freeMinutes) {
        service.createDepositRule(new BarDepositRuleRequest(
                "deposit-rule-" + key, "rule", List.of(), "bar_" + key,
                "bar", "bar_" + key, "fixed", fixedAmountCent, null, freeMinutes));
        BarTableRecord table = service.createTable(new BarTableRequest(
                "table-" + key, "create table", List.of(), "bar_" + key,
                "VIP " + key, "card_seat", 100000L, 2, "active"));
        return service.createReservation(new BarReservationRequest(
                "resv-" + key, "create reservation", List.of(), "bar_" + key,
                table.id(), Instant.parse("2030-06-20T21:00:00Z"), 2, "staff_sales", null));
    }

    @Test
    void checkInRejectsGuestWhenVerificationBarDiffersFromReservationBar() {
        BarReservationRecord reservation = createReservation("reservation-scope", "bar_1", 2);

        assertThatThrownBy(() -> service.checkInReservationGuest(
                reservation.id(),
                verification("checkin-cross-bar", "guest_1", "bar_2")
        ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_SCOPE_MISMATCH");

        assertThat(barRepository.guestCheckins).isEmpty();
    }

    @Test
    void billUsesOrderCentsAndReservationDepositWithoutTicketOrMinimumSpendOffset() {
        BarReservationRecord reservation = createReservation("reservation-bill", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-bill",
                "create product",
                List.of(),
                "bar_1",
                "Whisky",
                "spirit",
                128800L,
                null,
                null,
                "active",
                null,
                null,
                false
        ));

        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-bill",
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)),
                "staff_sales",
                List.of("staff_service"),
                "offline"
        ));
        BarBillRecord bill = service.createOrderBill(order.id(), new BarOrderBillCreateRequest(
                "bill-create",
                "create bill",
                List.of(),
                "offline",
                true,
                0
        ));

        assertThat(order.totalAmountCent()).isEqualTo(257600L);
        assertThat(bill.subtotalAmountCent()).isEqualTo(257600L);
        assertThat(bill.depositDeductAmountCent()).isZero();
        assertThat(bill.payableAmountCent()).isEqualTo(257600L);
        assertThat(bill.paymentStatus()).isEqualTo("unpaid");
    }

    @Test
    void salesCommissionGeneratesFinancePayableLedger() {
        BarBillRecord bill = billForOrder("commission-payable", "staff_sales");
        service.calculateCommission(bill.id(),
                new CommissionCalculateRequest("commission-payable-1", bill.id(), true, false));
        // A5(REQ-26b 增强)：销售提成贯通 finance 应付台账
        assertThat(receivableLedgerService.commissionPayables).isNotEmpty();
    }

    @Test
    void commissionUsesFifteenPercentForSalesAndTenPercentForNonSalesInBasisPoints() {
        BarBillRecord salesBill = billForOrder("sales-order", "staff_sales");
        BarCommissionCalculationRecord salesCommission = service.calculateCommission(
                salesBill.id(),
                new CommissionCalculateRequest("commission-sales", salesBill.id(), true, false)
        );

        BarBillRecord nonSalesBill = billForOrder("service-order", null);
        BarCommissionCalculationRecord nonSalesCommission = service.calculateCommission(
                nonSalesBill.id(),
                new CommissionCalculateRequest("commission-non-sales", nonSalesBill.id(), true, false)
        );

        assertThat(salesCommission)
                .extracting(
                        BarCommissionCalculationRecord::commissionRateBp,
                        BarCommissionCalculationRecord::commissionAmountCent
                )
                .containsExactly(1500, 19320L);
        assertThat(nonSalesCommission)
                .extracting(
                        BarCommissionCalculationRecord::salesRoleType,
                        BarCommissionCalculationRecord::commissionRateBp,
                        BarCommissionCalculationRecord::commissionAmountCent
                )
                .containsExactly("non_sales", 1000, 12880L);
    }

    @Test
    void deliveryRejectsStockManagedProductWhenInventoryIsInsufficientAndKeepsItemUndelivered() {
        BarReservationRecord reservation = createReservation("reservation-stock", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-stock",
                "create product",
                List.of(),
                "bar_1",
                "Champagne",
                "spirit",
                188800L,
                null,
                null,
                "active",
                "inventory_champagne",
                "bar_1",
                true
        ));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-stock",
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)),
                null,
                List.of(),
                "offline"
        ));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        assertThatThrownBy(() -> service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-stock",
                "deliver",
                List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2)),
                "staff_1",
                List.of()
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("STOCK_INSUFFICIENT");

        assertThat(barRepository.orderItems.values())
                .extracting(BarOrderItemRecord::deliveryStatus)
                .containsExactly("pending");
        assertThat(barRepository.stockMovements).isEmpty();
    }

    @Test
    void deliveryRejectsAlreadyDeliveredOrderItemAndDoesNotDeductStockAgain() {
        BarReservationRecord reservation = createReservation("reservation-stock-repeat", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-stock-repeat",
                "create product",
                List.of(),
                "bar_1",
                "Champagne",
                "spirit",
                188800L,
                null,
                null,
                "active",
                "inventory_champagne_repeat",
                "bar_1",
                true
        ));
        barRepository.saveStockBalance(new StockBalanceSnapshot(
                "balance-repeat",
                "inventory_champagne_repeat",
                "bar_1",
                5,
                0,
                5,
                Instant.now()
        ));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-stock-repeat",
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)),
                null,
                List.of(),
                "offline"
        ));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-stock-repeat-1",
                "deliver once",
                List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2)),
                "staff_1",
                List.of("att_delivery_1")
        ));

        assertThatThrownBy(() -> service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-stock-repeat-2",
                "deliver twice",
                List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2)),
                "staff_1",
                List.of("att_delivery_2")
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_ORDER_ITEM_ALREADY_DELIVERED");

        assertThat(barRepository.findStockBalance("inventory_champagne_repeat", "bar_1"))
                .get()
                .extracting(StockBalanceSnapshot::quantity, StockBalanceSnapshot::availableQuantity)
                .containsExactly(3, 3);
        assertThat(barRepository.stockMovements).hasSize(1);
        assertThat(barRepository.orderItems.get(orderItemId).deliveryStatus()).isEqualTo("delivered");
    }

    @Test
    void deliveryRejectsOrderItemFromAnotherOrderAndKeepsBothOrdersPending() {
        BarReservationRecord firstReservation = createReservation("reservation-scope-1", "bar_1", 2);
        BarReservationRecord secondReservation = createReservation("reservation-scope-2", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-scope",
                "create product",
                List.of(),
                "bar_1",
                "Soda",
                "soft_drink",
                1800L,
                null,
                null,
                "active",
                null,
                null,
                false
        ));
        BarOrderRecord firstOrder = service.createOrder(new BarOrderRequest(
                "order-scope-1",
                "create first order",
                List.of(),
                "bar_1",
                firstReservation.tableId(),
                firstReservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)),
                null,
                List.of(),
                "offline"
        ));
        BarOrderRecord secondOrder = service.createOrder(new BarOrderRequest(
                "order-scope-2",
                "create second order",
                List.of(),
                "bar_1",
                secondReservation.tableId(),
                secondReservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)),
                null,
                List.of(),
                "offline"
        ));
        String secondOrderItemId = barRepository.listOrderItems(secondOrder.id()).getFirst().id();

        assertThatThrownBy(() -> service.deliverOrder(firstOrder.id(), new BarOrderDeliveryRequest(
                "deliver-scope-mismatch",
                "deliver wrong item",
                List.of(),
                List.of(Map.of("order_item_id", secondOrderItemId, "quantity", 1)),
                "staff_1",
                List.of("att_delivery_scope")
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_ORDER_ITEM_SCOPE_MISMATCH");

        assertThat(barRepository.findOrderById(firstOrder.id()))
                .get()
                .extracting(BarOrderRecord::status)
                .isEqualTo("created");
        assertThat(barRepository.findOrderById(secondOrder.id()))
                .get()
                .extracting(BarOrderRecord::status)
                .isEqualTo("created");
        assertThat(barRepository.orderItems.values())
                .extracting(BarOrderItemRecord::deliveryStatus)
                .containsOnly("pending");
    }

    @Test
    void deliveryPersistsEvidenceAndPhotoAttachmentsOnTicketEventAndStockMovement() {
        BarReservationRecord reservation = createReservation("reservation-delivery-evidence", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-delivery-evidence",
                "create product",
                List.of(),
                "bar_1",
                "Champagne",
                "spirit",
                188800L,
                null,
                null,
                "active",
                "inventory_champagne_evidence",
                "bar_1",
                true
        ));
        barRepository.saveStockBalance(new StockBalanceSnapshot(
                "balance-evidence",
                "inventory_champagne_evidence",
                "bar_1",
                3,
                0,
                3,
                Instant.now()
        ));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-delivery-evidence",
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)),
                null,
                List.of(),
                "offline"
        ));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-with-photo-evidence",
                "deliver with photo evidence",
                List.of("att_delivery_doc", "att_shared"),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 1)),
                "staff_1",
                List.of("att_delivery_photo", "att_shared")
        ));

        assertThat(barRepository.stockMovements).hasSize(1);
        assertThat(barRepository.stockMovements.getFirst().evidenceAttachmentIds())
                .containsExactly("att_delivery_doc", "att_shared", "att_delivery_photo");
        assertThat(barRepository.productionTicketEvents)
                .extracting(BarProductionTicketEventRecord::evidenceAttachmentIds)
                .containsExactly(List.of("att_delivery_doc", "att_shared", "att_delivery_photo"));
    }

    @Test
    void deliveryWithExcessPourRecordsLossAndEmitsLossExceptionWhenOverThreshold() {
        BarReservationRecord reservation = createReservation("reservation-pour-loss", "bar_1", 2);
        // 标准 30ml/杯，损耗阈值 10ml；按杯销售配置（REQ-22）
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-pour-loss", "create", List.of(), "bar_1", "Whisky", "spirit", 128800L, null, null,
                "active", null, null, false, 30, "ml", 10));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-pour-loss", "create order", List.of(), "bar_1", reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)), null, List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        // 2 杯标准 60ml，实际 75ml → 损耗 15ml > 阈值 10ml
        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-pour-loss", "deliver", List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2, "actual_pour_volume", 75)),
                "staff_1", List.of()));

        assertThat(barRepository.listOrderItems(order.id()).getFirst().lossVolume()).isEqualTo(15L);
        assertThat(barRepository.listOrderItems(order.id()).getFirst().actualPourVolume()).isEqualTo(75);
        assertThat(auditEventRepository.events.stream()
                .anyMatch(event -> "inventory.loss_exception".equals(event.eventName()))).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // P0#4 按杯 pour cost 强制采集（文档 bar §17.4 / R6）
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void deliveryRejectsByGlassWhenActualPourVolumeMissingInStrictMode() {
        // 默认拒绝模式：按杯产品（配置 standard_pour_volume）出品缺 actual_pour_volume → 拒单。
        BarReservationRecord reservation = createReservation("reservation-pour-required", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-pour-required", "create", List.of(), "bar_1", "Whisky", "spirit", 128800L, null, null,
                "active", null, null, false, 30, "ml", 10));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-pour-required", "create order", List.of(), "bar_1", reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)), null, List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        assertThatThrownBy(() -> service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-pour-required", "deliver", List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2)),
                "staff_1", List.of())))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("POUR_VOLUME_REQUIRED");

        assertThat(barRepository.orderItems.get(orderItemId).deliveryStatus()).isEqualTo("pending");
    }

    @Test
    void deliveryByGlassFallbackModeUsesStandardVolumeAndMarksNeedsReviewWhenActualMissing() {
        // 兜底模式：缺 actual_pour_volume → 按标准量计、loss=0，但落异常事件标记 needs_review（严禁静默）。
        BarOrderService fallbackOrderService = new BarOrderService(
                barRepository, authorizationService,
                new AuditEventService(auditEventRepository),
                new TraceabilityService(mock(TraceabilityRepository.class), authorizationService),
                receivableLedgerService, false);

        BarReservationRecord reservation = createReservation("reservation-pour-fallback", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-pour-fallback", "create", List.of(), "bar_1", "Whisky", "spirit", 128800L, null, null,
                "active", null, null, false, 30, "ml", 10));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-pour-fallback", "create order", List.of(), "bar_1", reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)), null, List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        fallbackOrderService.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-pour-fallback", "deliver", List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2)),
                "staff_1", List.of()));

        // 2 杯标准 30ml → 兜底实际量 60ml，损耗 0。
        assertThat(barRepository.listOrderItems(order.id()).getFirst().actualPourVolume()).isEqualTo(60);
        assertThat(barRepository.listOrderItems(order.id()).getFirst().lossVolume()).isEqualTo(0L);
        assertThat(auditEventRepository.events.stream()
                .anyMatch(event -> "inventory.loss_exception".equals(event.eventName())
                        && "pour_volume_fallback".equals(event.metadata().get("reason"))
                        && Boolean.TRUE.equals(event.metadata().get("needs_review")))).isTrue();
    }

    @Test
    void deliveryNonByGlassProductWithoutPourVolumeIsAllowed() {
        // 非按杯产品（未配置 standard_pour_volume）：缺 actual_pour_volume 不拦截，保持既有行为。
        BarReservationRecord reservation = createReservation("reservation-non-by-glass", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-non-by-glass", "create", List.of(), "bar_1", "Bottle Beer", "beer", 2800L, null, null,
                "active", null, null, false));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-non-by-glass", "create order", List.of(), "bar_1", reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)), null, List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-non-by-glass", "deliver", List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 1)),
                "staff_1", List.of()));

        assertThat(barRepository.orderItems.get(orderItemId).deliveryStatus()).isEqualTo("delivered");
        assertThat(barRepository.listOrderItems(order.id()).getFirst().actualPourVolume()).isNull();
        assertThat(barRepository.listOrderItems(order.id()).getFirst().lossVolume()).isEqualTo(0L);
    }

    @Test
    void deliveryRejectsPartialQuantityAndKeepsOrderInventoryAndTicketPending() {
        BarReservationRecord reservation = createReservation("reservation-partial-delivery", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-partial-delivery",
                "create product",
                List.of(),
                "bar_1",
                "Champagne",
                "spirit",
                188800L,
                null,
                null,
                "active",
                "inventory_champagne_partial",
                "bar_1",
                true
        ));
        barRepository.saveStockBalance(new StockBalanceSnapshot(
                "balance-partial",
                "inventory_champagne_partial",
                "bar_1",
                5,
                0,
                5,
                Instant.now()
        ));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-partial-delivery",
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)),
                null,
                List.of(),
                "offline"
        ));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        assertThatThrownBy(() -> service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-partial-quantity",
                "deliver partial quantity",
                List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 1)),
                "staff_1",
                List.of("att_partial_delivery")
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_ORDER_PARTIAL_DELIVERY_UNSUPPORTED");

        assertThat(barRepository.findOrderById(order.id()))
                .get()
                .extracting(BarOrderRecord::status)
                .isEqualTo("created");
        assertThat(barRepository.orderItems.get(orderItemId).deliveryStatus()).isEqualTo("pending");
        assertThat(barRepository.findStockBalance("inventory_champagne_partial", "bar_1"))
                .get()
                .extracting(StockBalanceSnapshot::quantity, StockBalanceSnapshot::availableQuantity)
                .containsExactly(5, 5);
        assertThat(barRepository.stockMovements).isEmpty();
        assertThat(barRepository.findProductionTicketByOrderId(order.id()))
                .get()
                .extracting(BarProductionTicketRecord::ticketStatus)
                .isEqualTo("created");
    }

    @Test
    void barBillPayIntentReusesFinancePaymentIntentSemantics() {
        BarBillRecord bill = billForOrder("pay-intent-order", "staff_sales");

        PaymentIntentRecord paymentIntent = service.createBillPayIntent(bill.id(), new PaymentIntentRequest(
                "pay-intent",
                "create intent",
                List.of(),
                "bar_bill",
                bill.id(),
                bill.payableAmountCent(),
                "CNY",
                "aggregate_pay"
        ));

        assertThat(paymentIntent)
                .extracting(
                        PaymentIntentRecord::businessOrderType,
                        PaymentIntentRecord::businessOrderId,
                        PaymentIntentRecord::amountCent,
                        PaymentIntentRecord::channel,
                        PaymentIntentRecord::status
                )
                .containsExactly("bar_bill", bill.id(), bill.payableAmountCent(), "aggregate_pay", "created");
    }

    @Test
    void productionTicketTransitionsThroughAcceptedPreparingAndReadyWithEventTrace() {
        BarReservationRecord reservation = createReservation("reservation-production-flow", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-production-flow",
                "create product",
                List.of(),
                "bar_1",
                "Negroni",
                "cocktail",
                8800L,
                null,
                null,
                "active",
                null,
                null,
                false
        ));
        service.createOrder(new BarOrderRequest(
                "production-flow",
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)),
                null,
                List.of(),
                "offline"
        ));

        BarProductionTicketRecord accepted = service.transitionProductionTicket(
                "ticket_production_flow",
                new BarProductionTicketTransitionRequest(
                        "production-accept",
                        "accept order",
                        List.of("att_accept"),
                        "accepted",
                        "bar-counter-1",
                        "staff_bar_1",
                        null
                )
        );
        BarProductionTicketRecord preparing = service.transitionProductionTicket(
                "ticket_production_flow",
                new BarProductionTicketTransitionRequest(
                        "production-prepare",
                        "start mixing",
                        List.of(),
                        "preparing",
                        "bar-counter-1",
                        "staff_bar_1",
                        null
                )
        );
        BarProductionTicketRecord ready = service.transitionProductionTicket(
                "ticket_production_flow",
                new BarProductionTicketTransitionRequest(
                        "production-ready",
                        "ready for waiter",
                        List.of("att_ready"),
                        "ready",
                        "bar-counter-1",
                        "staff_bar_1",
                        null
                )
        );

        assertThat(accepted.ticketStatus()).isEqualTo("accepted");
        assertThat(preparing.ticketStatus()).isEqualTo("preparing");
        assertThat(ready.ticketStatus()).isEqualTo("ready");
        // P1-3 fix: operatorUserId is now from auth token ("usr_admin"), not request body ("staff_bar_1")
        assertThat(service.listProductionTicketEvents("ticket_production_flow").items())
                .extracting(
                        BarProductionTicketEventRecord::actionType,
                        BarProductionTicketEventRecord::fromStatus,
                        BarProductionTicketEventRecord::toStatus,
                        BarProductionTicketEventRecord::stationCode,
                        BarProductionTicketEventRecord::operatorUserId
                )
                .containsExactly(
                        tuple("mark_ready", "preparing", "ready", "bar-counter-1", "usr_admin"),
                        tuple("start_prepare", "accepted", "preparing", "bar-counter-1", "usr_admin"),
                        tuple("accept", "created", "accepted", "bar-counter-1", "usr_admin")
                );
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.production_ticket.transitioned");
    }

    @Test
    void productionTicketExceptionTransitionUsesDbAllowedActionType() {
        BarReservationRecord reservation = createReservation("reservation-production-exception", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-production-exception",
                "create product",
                List.of(),
                "bar_1",
                "Old Fashioned",
                "cocktail",
                9800L,
                null,
                null,
                "active",
                null,
                null,
                false
        ));
        service.createOrder(new BarOrderRequest(
                "production-exception",
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)),
                null,
                List.of(),
                "offline"
        ));

        BarProductionTicketRecord exception = service.transitionProductionTicket(
                "ticket_production_exception",
                new BarProductionTicketTransitionRequest(
                        "production-exception-mark",
                        "spill incident",
                        List.of(),
                        "exception",
                        "bar-counter-1",
                        "staff_bar_1",
                        null
                )
        );

        assertThat(exception.ticketStatus()).isEqualTo("exception");
        // action_type must be one of the values allowed by V029 chk_bar_production_ticket_event_action
        // ('accept','start_prepare','mark_ready','cancel','exception','deliver') — NOT "mark_exception".
        assertThat(service.listProductionTicketEvents("ticket_production_exception").items())
                .extracting(BarProductionTicketEventRecord::actionType, BarProductionTicketEventRecord::toStatus)
                .containsExactly(tuple("exception", "exception"));
    }

    @Test
    void productionTicketRejectsInvalidTransitionAndKeepsStatusUnchanged() {
        BarReservationRecord reservation = createReservation("reservation-production-invalid", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-production-invalid",
                "create product",
                List.of(),
                "bar_1",
                "Mojito",
                "cocktail",
                6800L,
                null,
                null,
                "active",
                null,
                null,
                false
        ));
        service.createOrder(new BarOrderRequest(
                "production-invalid",
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)),
                null,
                List.of(),
                "offline"
        ));

        assertThatThrownBy(() -> service.transitionProductionTicket(
                "ticket_production_invalid",
                new BarProductionTicketTransitionRequest(
                        "production-ready-from-created",
                        "skip states",
                        List.of(),
                        "ready",
                        "bar-counter-1",
                        "staff_bar_1",
                        null
                )
        ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_PRODUCTION_INVALID_TRANSITION");

        assertThat(barRepository.findProductionTicketById("ticket_production_invalid"))
                .get()
                .extracting(BarProductionTicketRecord::ticketStatus)
                .isEqualTo("created");
        assertThat(barRepository.productionTicketEvents).isEmpty();
    }

    @Test
    void deliveryMarksProductionTicketDeliveredWithoutOverwritingCancelledTicket() {
        BarReservationRecord reservation = createReservation("reservation-production-delivery", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-production-delivery",
                "create product",
                List.of(),
                "bar_1",
                "Champagne",
                "spirit",
                188800L,
                null,
                null,
                "active",
                null,
                null,
                false
        ));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "production-delivery",
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)),
                null,
                List.of(),
                "offline"
        ));
        String itemId = barRepository.listOrderItems(order.id()).getFirst().id();

        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "production-deliver",
                "deliver",
                List.of(),
                List.of(Map.of("order_item_id", itemId, "quantity", 1)),
                "staff_1",
                List.of("att_delivery")
        ));

        assertThat(barRepository.findProductionTicketById("ticket_production_delivery"))
                .get()
                .extracting(BarProductionTicketRecord::ticketStatus)
                .isEqualTo("delivered");
        assertThat(barRepository.productionTicketEvents)
                .extracting(BarProductionTicketEventRecord::actionType, BarProductionTicketEventRecord::toStatus)
                .contains(tuple("deliver", "delivered"));
    }

    // A2/A3(REQ-21): 现金结算发 payment.cash_received 事件；channel 用统一词表
    @Test
    void settleWithCashChannelEmitsCashReceivedEvent() {
        BarBillRecord bill = billForOrder("settle-cash", "staff_sales");
        service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-cash-1", "cash settle", List.of(), "pay_cash_1", null, "cash", "cashier_1", null));
        assertThat(auditEventRepository.events.stream()
                .anyMatch(event -> "payment.cash_received".equals(event.eventName()))).isTrue();
    }

    @Test
    void partialSettlementKeepsOriginalPayableAndMarksPartialStatuses() {
        BarBillRecord bill = billForOrder("partial-settle", "staff_sales");

        BarBillRecord settled = service.settleBill(
                bill.id(),
                new BarBillSettleRequest(
                        "partial-settle-once",
                        "customer pays deposit first",
                        List.of("att_partial_settle"),
                        "pay_partial_1",
                        40000L,
                        "aggregate_pay"
                )
        );

        assertThat(settled)
                .extracting(
                        BarBillRecord::payableAmountCent,
                        BarBillRecord::paidAmountCent,
                        BarBillRecord::paymentStatus,
                        BarBillRecord::settlementStatus
                )
                .containsExactly(128800L, 40000L, "partial_paid", "partial_settled");
        assertThat(barRepository.findBillById(bill.id()))
                .get()
                .extracting(BarBillRecord::payableAmountCent, BarBillRecord::paidAmountCent)
                .containsExactly(128800L, 40000L);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.bill.settled");
    }

    @Test
    void serviceFeeAdjustmentIncreasesPayableAndWritesLedger() {
        BarBillRecord bill = billForOrder("service-fee-adjustment", "staff_sales");

        BarBillAdjustmentRecord adjustment = service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-service-fee",
                        "add service fee",
                        List.of("att_service_fee"),
                        "service_fee",
                        12880L,
                        "10 percent service fee",
                        "supervisor_1"
                )
        );

        assertThat(adjustment)
                .extracting(
                        BarBillAdjustmentRecord::adjustmentType,
                        BarBillAdjustmentRecord::amountCent,
                        BarBillAdjustmentRecord::beforePayableAmountCent,
                        BarBillAdjustmentRecord::afterPayableAmountCent,
                        BarBillAdjustmentRecord::status
                )
                .containsExactly("service_fee", 12880L, 128800L, 141680L, "applied");
        assertThat(barRepository.findBillById(bill.id()))
                .get()
                .extracting(BarBillRecord::payableAmountCent, BarBillRecord::paymentStatus, BarBillRecord::settlementStatus)
                .containsExactly(141680L, "unpaid", "unsettled");
        assertThat(barRepository.billAdjustments).hasSize(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.bill.adjusted");
    }

    @Test
    void billAdjustmentIdIsScopedByBillToAvoidCrossBillIdempotencyCollision() {
        BarBillRecord firstBill = billForOrder("adjustment-id-scope-a", "staff_sales");
        BarBillRecord secondBill = billForOrder("adjustment-id-scope-b", "staff_sales");

        BarBillAdjustmentRecord first = service.applyBillAdjustment(
                firstBill.id(),
                new BarBillAdjustmentRequest(
                        "shared-adjustment-key",
                        "add service fee",
                        List.of(),
                        "service_fee",
                        1000L,
                        "same idempotency key on another bill must not collide",
                        "supervisor_1"
                )
        );
        BarBillAdjustmentRecord second = service.applyBillAdjustment(
                secondBill.id(),
                new BarBillAdjustmentRequest(
                        "shared-adjustment-key",
                        "add service fee",
                        List.of(),
                        "service_fee",
                        1000L,
                        "same idempotency key on another bill must not collide",
                        "supervisor_1"
                )
        );

        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(barRepository.billAdjustments)
                .extracting(BarBillAdjustmentRecord::billId)
                .containsExactly(firstBill.id(), secondBill.id());
    }

    @Test
    void minimumSpendTopUpIncreasesPayableWithoutUsingTicketOrEntryRights() {
        BarBillRecord bill = billForOrder("min-spend-top-up", "staff_sales");

        BarBillAdjustmentRecord adjustment = service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-min-spend",
                        "top up minimum spend",
                        List.of(),
                        "min_spend_top_up",
                        20000L,
                        "minimum spend gap cannot be offset by ticket or card-seat entry rights",
                        "supervisor_1"
                )
        );

        assertThat(adjustment)
                .extracting(
                        BarBillAdjustmentRecord::adjustmentType,
                        BarBillAdjustmentRecord::amountCent,
                        BarBillAdjustmentRecord::beforePayableAmountCent,
                        BarBillAdjustmentRecord::afterPayableAmountCent
                )
                .containsExactly("min_spend_top_up", 20000L, 128800L, 148800L);
        assertThat(barRepository.findBillById(bill.id()))
                .get()
                .extracting(BarBillRecord::payableAmountCent)
                .isEqualTo(148800L);
    }

    @Test
    void onAccountMarksBillAsReceivableWithoutChangingPayable() {
        BarBillRecord bill = billForOrder("on-account", "staff_sales");

        BarBillAdjustmentRecord adjustment = service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-on-account",
                        "register on account",
                        List.of("att_on_account"),
                        "on_account",
                        0L,
                        "approved customer credit",
                        "finance_1"
                )
        );

        assertThat(adjustment)
                .extracting(
                        BarBillAdjustmentRecord::adjustmentType,
                        BarBillAdjustmentRecord::amountCent,
                        BarBillAdjustmentRecord::beforePayableAmountCent,
                        BarBillAdjustmentRecord::afterPayableAmountCent,
                        BarBillAdjustmentRecord::arApItemId
                )
                .containsExactly("on_account", 0L, 128800L, 128800L, "arap_for_" + bill.id());
        assertThat(barRepository.findBillById(bill.id()))
                .get()
                .extracting(BarBillRecord::payableAmountCent, BarBillRecord::paymentStatus, BarBillRecord::settlementStatus)
                .containsExactly(128800L, "on_account", "receivable");
        assertThat(receivableLedgerService.requests)
                .extracting(
                        CapturedReceivableRequest::billId,
                        CapturedReceivableRequest::orderId,
                        CapturedReceivableRequest::amountCent,
                        CapturedReceivableRequest::idempotencyKey,
                        CapturedReceivableRequest::operatorNote,
                        CapturedReceivableRequest::evidenceAttachmentIds
                )
                .containsExactly(tuple(
                        bill.id(),
                        bill.orderId(),
                        bill.payableAmountCent(),
                        "bill-adjust-on-account",
                        "approved customer credit",
                        List.of("att_on_account")
                ));
    }

    @Test
    void onAccountRejectsAlreadyReceivableBillToAvoidDuplicateArApItem() {
        BarBillRecord bill = billForOrder("on-account-duplicate", "staff_sales");
        service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-on-account-first",
                        "register on account",
                        List.of(),
                        "on_account",
                        0L,
                        "approved customer credit",
                        "finance_1"
                )
        );

        assertThatThrownBy(() -> service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-on-account-second",
                        "register on account again",
                        List.of(),
                        "on_account",
                        0L,
                        "approved customer credit duplicate",
                        "finance_1"
                )
        ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_BILL_ALREADY_ON_ACCOUNT");

        assertThat(receivableLedgerService.requests).hasSize(1);
        assertThat(barRepository.billAdjustments).hasSize(1);
    }

    @Test
    void billAdjustmentRejectsUnsupportedType() {
        BarBillRecord bill = billForOrder("unsupported-adjustment", "staff_sales");

        assertThatThrownBy(() -> service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-unsupported",
                        "unsupported",
                        List.of(),
                        "free_ticket_offset",
                        1000L,
                        "ticket rights must not offset minimum spend",
                        "supervisor_1"
                )
        ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void waiverReducesPayableBeforeSettlementAndDoesNotRecordRefundTimestamp() {
        BarBillRecord bill = billForOrder("waiver-pre-settle", "staff_sales");

        BarBillAdjustmentRecord adjustment = service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-waiver",
                        "waive part of the bill",
                        List.of("att_waiver"),
                        "waiver",
                        28800L,
                        "goodwill waiver before checkout",
                        "supervisor_1"
                )
        );

        // 免单是结账前减项：after = before - amount；refundedAt 为空（不计入 net 退款冲减）。
        assertThat(adjustment)
                .extracting(
                        BarBillAdjustmentRecord::adjustmentType,
                        BarBillAdjustmentRecord::amountCent,
                        BarBillAdjustmentRecord::beforePayableAmountCent,
                        BarBillAdjustmentRecord::afterPayableAmountCent,
                        BarBillAdjustmentRecord::status,
                        BarBillAdjustmentRecord::refundedAt
                )
                .containsExactly("waiver", 28800L, 128800L, 100000L, "applied", null);
        assertThat(barRepository.findBillById(bill.id()))
                .get()
                .extracting(BarBillRecord::payableAmountCent, BarBillRecord::paymentStatus, BarBillRecord::settlementStatus)
                .containsExactly(100000L, "unpaid", "unsettled");
    }

    @Test
    void waiverRejectsAmountExceedingPayable() {
        BarBillRecord bill = billForOrder("waiver-too-big", "staff_sales");

        assertThatThrownBy(() -> service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-waiver-too-big",
                        "waive more than payable",
                        List.of(),
                        "waiver",
                        128801L,
                        "cannot waive beyond payable",
                        "supervisor_1"
                )
        ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void refundReducesPayableAfterSettlementAndRecordsRefundTimestamp() {
        BarBillRecord bill = billForOrder("refund-post-settle", "staff_sales");
        service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-for-refund", "settle before refund", List.of(),
                "pay_for_refund", null, "aggregate_pay", null, null));

        BarBillAdjustmentRecord adjustment = service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-refund",
                        "refund part of settled bill",
                        List.of("att_refund"),
                        "refund",
                        30000L,
                        "customer complaint refund after checkout",
                        "finance_1"
                )
        );

        // 退款是结账后减项：记录 refundedAt（用于经营日窗口聚合冲减 net）。
        assertThat(adjustment)
                .extracting(
                        BarBillAdjustmentRecord::adjustmentType,
                        BarBillAdjustmentRecord::amountCent,
                        BarBillAdjustmentRecord::beforePayableAmountCent,
                        BarBillAdjustmentRecord::afterPayableAmountCent,
                        BarBillAdjustmentRecord::status
                )
                .containsExactly("refund", 30000L, 128800L, 98800L, "applied");
        assertThat(adjustment.refundedAt()).isNotNull();

        // RC1①：退款过账到 finance ar_ap 台账（direction=payable, source_type=refund），
        // source_id=退款调整记录 id，金额=退款额；调整记录上回填 arApItemId（强一致联动）。
        assertThat(receivableLedgerService.refundPayables).hasSize(1);
        CapturedRefundPayable posted = receivableLedgerService.refundPayables.get(0);
        assertThat(posted.billId()).isEqualTo(bill.id());
        assertThat(posted.refundAmountCent()).isEqualTo(30000L);
        assertThat(posted.adjustmentId()).isEqualTo(adjustment.id());
        assertThat(adjustment.arApItemId()).isEqualTo("arap_refund_" + adjustment.id());
    }

    @Test
    void refundRejectsUnsettledBill() {
        BarBillRecord bill = billForOrder("refund-unsettled", "staff_sales");

        assertThatThrownBy(() -> service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-refund-unsettled",
                        "refund before settle",
                        List.of(),
                        "refund",
                        1000L,
                        "cannot refund an unsettled bill",
                        "finance_1"
                )
        ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_BILL_NOT_REFUNDABLE");
    }

    @Test
    void splitBillCreatesChildBillReducesSourceAndWritesOperation() {
        BarBillRecord bill = billForOrder("split-bill", "staff_sales");

        BarBillOperationRecord operation = service.splitBill(
                bill.id(),
                new BarBillSplitRequest(
                        "split-bill-once",
                        "split bill",
                        List.of("att_split"),
                        40000L,
                        "guest wants separate payment",
                        "cashier_1"
                )
        );

        assertThat(operation)
                .extracting(
                        BarBillOperationRecord::operationType,
                        BarBillOperationRecord::primaryBillId,
                        BarBillOperationRecord::secondaryBillId,
                        BarBillOperationRecord::amountCent,
                        BarBillOperationRecord::status
                )
                .containsExactly("split", bill.id(), operation.generatedBillId(), 40000L, "applied");
        assertThat(barRepository.findBillById(bill.id()))
                .get()
                .extracting(BarBillRecord::subtotalAmountCent, BarBillRecord::payableAmountCent)
                .containsExactly(88800L, 88800L);
        assertThat(barRepository.findBillById(operation.generatedBillId()))
                .get()
                .extracting(BarBillRecord::subtotalAmountCent, BarBillRecord::payableAmountCent, BarBillRecord::paymentStatus)
                .containsExactly(40000L, 40000L, "unpaid");
        assertThat(barRepository.listBillOperations(bill.id()))
                .extracting(BarBillOperationRecord::operationType)
                .containsExactly("split");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.bill.split");
    }

    @Test
    void mergeBillsMovesSecondaryAmountIntoPrimaryAndWritesOperation() {
        BarBillRecord primary = billForOrder("merge-primary-bill", "staff_sales");
        BarBillRecord secondary = billForOrder("merge-secondary-bill", "staff_sales");

        BarBillOperationRecord operation = service.mergeBills(
                primary.id(),
                new BarBillMergeRequest(
                        "merge-bill-once",
                        "merge bill",
                        List.of("att_merge"),
                        secondary.id(),
                        "same party pays together",
                        "cashier_1"
                )
        );

        assertThat(operation)
                .extracting(
                        BarBillOperationRecord::operationType,
                        BarBillOperationRecord::primaryBillId,
                        BarBillOperationRecord::secondaryBillId,
                        BarBillOperationRecord::amountCent,
                        BarBillOperationRecord::status
                )
                .containsExactly("merge", primary.id(), secondary.id(), 128800L, "applied");
        assertThat(barRepository.findBillById(primary.id()))
                .get()
                .extracting(BarBillRecord::subtotalAmountCent, BarBillRecord::payableAmountCent)
                .containsExactly(257600L, 257600L);
        assertThat(barRepository.findBillById(secondary.id()))
                .get()
                .extracting(BarBillRecord::subtotalAmountCent, BarBillRecord::payableAmountCent, BarBillRecord::paymentStatus, BarBillRecord::settlementStatus)
                .containsExactly(0L, 0L, "merged", "merged");
        assertThat(barRepository.listBillOperations(primary.id()))
                .extracting(BarBillOperationRecord::operationType)
                .containsExactly("merge");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.bill.merged");
    }

    @Test
    void splitAndMergeRejectInvalidPaidSettledAndSameBillOperations() {
        BarBillRecord bill = billForOrder("split-merge-reject", "staff_sales");

        assertThatThrownBy(() -> service.splitBill(
                        bill.id(),
                        new BarBillSplitRequest("split-too-much", "split", List.of(), 128800L, "too much", "cashier_1")
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_BILL_SPLIT_AMOUNT_INVALID");

        BarBillRecord paid = barRepository.saveBill(new BarBillRecord(
                bill.id(),
                bill.billNo(),
                bill.orderId(),
                bill.barId(),
                bill.merchantId(),
                bill.tableId(),
                bill.reservationId(),
                bill.subtotalAmountCent(),
                bill.discountAmountCent(),
                bill.depositDeductAmountCent(),
                bill.payableAmountCent(),
                1000L,
                bill.currency(),
                "partial_paid",
                "partial_settled",
                bill.salesStaffId(),
                bill.salesRoleType(),
                bill.createdAt(),
                bill.updatedAt()
        ));

        assertThatThrownBy(() -> service.splitBill(
                        paid.id(),
                        new BarBillSplitRequest("split-paid", "split", List.of(), 1000L, "paid", "cashier_1")
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_BILL_NOT_SPLITTABLE");

        assertThatThrownBy(() -> service.mergeBills(
                        paid.id(),
                        new BarBillMergeRequest("merge-same", "merge", List.of(), paid.id(), "same", "cashier_1")
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_BILL_MERGE_SAME_BILL");

        BarBillRecord primary = billForOrder("merge-cross-bar-primary", "staff_sales");
        BarBillRecord secondary = billForOrder("merge-cross-bar-secondary", "staff_sales");
        BarBillRecord otherBarSecondary = barRepository.saveBill(new BarBillRecord(
                secondary.id(),
                secondary.billNo(),
                secondary.orderId(),
                "bar_2",
                secondary.merchantId(),
                secondary.tableId(),
                secondary.reservationId(),
                secondary.subtotalAmountCent(),
                secondary.discountAmountCent(),
                secondary.depositDeductAmountCent(),
                secondary.payableAmountCent(),
                secondary.paidAmountCent(),
                secondary.currency(),
                secondary.paymentStatus(),
                secondary.settlementStatus(),
                secondary.salesStaffId(),
                secondary.salesRoleType(),
                secondary.createdAt(),
                secondary.updatedAt()
        ));

        assertThatThrownBy(() -> service.mergeBills(
                        primary.id(),
                        new BarBillMergeRequest("merge-cross-bar", "merge", List.of(), otherBarSecondary.id(), "cross bar", "cashier_1")
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_BILL_MERGE_BAR_MISMATCH");
    }

    @Test
    void ageVerificationStoresAdapterBoundaryResultAndEvidence() {
        BarAgeVerificationRecord record = service.createAgeVerification(new VerificationRequest(
                "age-check",
                "provider_result",
                "face-session-1",
                "device_age_1",
                "bar_1",
                Map.of("result", "failed", "review_status", "manual_review", "evidence_attachment_ids", List.of("att_1"))
        ));

        assertThat(record)
                .extracting(
                        BarAgeVerificationRecord::result,
                        BarAgeVerificationRecord::providerCode,
                        BarAgeVerificationRecord::reviewStatus,
                        BarAgeVerificationRecord::evidenceAttachmentIds
                )
                .containsExactly("failed", "placeholder", "manual_review", List.of("att_1"));
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.age_verification.rejected");
    }

    @Test
    void transferTableSessionKeepsSameBarMerchantAndBusinessDateTrace() {
        BarTableRecord sourceTable = createTable("source-transfer", "bar_1", "active", 50000L);
        BarTableRecord targetTable = createTable("target-transfer", "bar_1", "active", 80000L);
        BarTableSessionRecord session = createSession("session-transfer", sourceTable.id(), "bar_1", "merchant_1");

        BarTableSessionAdjustmentRecord adjustment = service.transferTableSession(
                session.id(),
                new BarTableTransferRequest(
                        "transfer-once",
                        "guest wants larger card seat",
                        List.of("att_transfer"),
                        targetTable.id(),
                        "guest wants larger card seat",
                        "supervisor_1",
                        session.sessionVersion()
                )
        );

        assertThat(adjustment)
                .extracting(
                        BarTableSessionAdjustmentRecord::adjustmentType,
                        BarTableSessionAdjustmentRecord::fromTableId,
                        BarTableSessionAdjustmentRecord::toTableId,
                        BarTableSessionAdjustmentRecord::status
                )
                .containsExactly("transfer", sourceTable.id(), targetTable.id(), "applied");
        assertThat(barRepository.findTableSessionById(session.id()))
                .get()
                .extracting(BarTableSessionRecord::tableId, BarTableSessionRecord::sessionVersion, BarTableSessionRecord::status)
                .containsExactly(targetTable.id(), 2L, "open");
        assertThat(barRepository.findTableById(sourceTable.id())).get().extracting(BarTableRecord::status).isEqualTo("pending_clear");
        assertThat(barRepository.findTableById(targetTable.id())).get().extracting(BarTableRecord::status).isEqualTo("opened");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.table.transferred");
    }

    @Test
    void transferTableSessionRejectsCrossBarTargetAndLeavesSessionUnchanged() {
        BarTableRecord sourceTable = createTable("source-cross-bar", "bar_1", "active", 50000L);
        BarTableRecord targetTable = createTable("target-cross-bar", "bar_2", "active", 80000L);
        BarTableSessionRecord session = createSession("session-cross-bar", sourceTable.id(), "bar_1", "merchant_1");

        assertThatThrownBy(() -> service.transferTableSession(
                session.id(),
                new BarTableTransferRequest(
                        "transfer-cross-bar",
                        "cross bar should fail",
                        List.of(),
                        targetTable.id(),
                        "cross bar should fail",
                        "supervisor_1",
                        session.sessionVersion()
                )
        ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_SCOPE_MISMATCH");

        assertThat(barRepository.findTableSessionById(session.id()))
                .get()
                .extracting(BarTableSessionRecord::tableId, BarTableSessionRecord::sessionVersion)
                .containsExactly(sourceTable.id(), 1L);
        assertThat(barRepository.findTableById(targetTable.id())).get().extracting(BarTableRecord::status).isEqualTo("active");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.table.transfer_failed");
    }

    @Test
    void mergeTableSessionsMarksSecondaryMergedAndKeepsPrimaryTraceable() {
        BarTableRecord primaryTable = createTable("primary-merge", "bar_1", "active", 50000L);
        BarTableRecord secondaryTable = createTable("secondary-merge", "bar_1", "active", 60000L);
        BarTableSessionRecord primary = createSession("primary-merge", primaryTable.id(), "bar_1", "merchant_1");
        BarTableSessionRecord secondary = createSession("secondary-merge", secondaryTable.id(), "bar_1", "merchant_1");

        BarTableSessionAdjustmentRecord adjustment = service.mergeTableSessions(
                primary.id(),
                new BarTableMergeRequest(
                        "merge-once",
                        "two groups joined",
                        List.of("att_merge"),
                        secondary.id(),
                        "two groups joined",
                        "supervisor_1",
                        primary.sessionVersion(),
                        secondary.sessionVersion()
                )
        );

        assertThat(adjustment)
                .extracting(
                        BarTableSessionAdjustmentRecord::adjustmentType,
                        BarTableSessionAdjustmentRecord::sourceSessionId,
                        BarTableSessionAdjustmentRecord::targetSessionId,
                        BarTableSessionAdjustmentRecord::status
                )
                .containsExactly("merge", secondary.id(), primary.id(), "applied");
        assertThat(barRepository.findTableSessionById(primary.id()))
                .get()
                .extracting(BarTableSessionRecord::sessionVersion, BarTableSessionRecord::status)
                .containsExactly(2L, "open");
        assertThat(barRepository.findTableSessionById(secondary.id()))
                .get()
                .extracting(BarTableSessionRecord::status, BarTableSessionRecord::sourceSessionId)
                .containsExactly("merged", primary.id());
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.table.merged");
    }

    @Test
    void renewTableSessionAppendsRenewalAdjustmentAndIncrementsVersion() {
        BarTableRecord table = createTable("renew", "bar_1", "active", 50000L);
        BarTableSessionRecord session = createSession("renew", table.id(), "bar_1", "merchant_1");

        BarTableSessionAdjustmentRecord adjustment = service.renewTableSession(
                session.id(),
                new BarTableRenewRequest(
                        "renew-once",
                        "guest extends time",
                        List.of("att_renew"),
                        Instant.parse("2026-06-21T02:00:00Z"),
                        20000L,
                        0L,
                        1000L,
                        "guest extends time",
                        "supervisor_1",
                        session.sessionVersion()
                )
        );

        assertThat(adjustment)
                .extracting(
                        BarTableSessionAdjustmentRecord::adjustmentType,
                        BarTableSessionAdjustmentRecord::renewedUntil,
                        BarTableSessionAdjustmentRecord::additionalMinConsumptionCent,
                        BarTableSessionAdjustmentRecord::additionalServiceFeeCent
                )
                .containsExactly("renew", Instant.parse("2026-06-21T02:00:00Z"), 20000L, 1000L);
        assertThat(barRepository.findTableSessionById(session.id()))
                .get()
                .extracting(BarTableSessionRecord::sessionVersion, BarTableSessionRecord::status)
                .containsExactly(2L, "open");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.table.renewed");
    }

    @Test
    void tableSessionAdjustmentRejectsStaleVersion() {
        BarTableRecord sourceTable = createTable("source-stale", "bar_1", "active", 50000L);
        BarTableRecord targetTable = createTable("target-stale", "bar_1", "active", 80000L);
        BarTableSessionRecord session = createSession("session-stale", sourceTable.id(), "bar_1", "merchant_1");

        assertThatThrownBy(() -> service.transferTableSession(
                session.id(),
                new BarTableTransferRequest(
                        "transfer-stale-version",
                        "stale version",
                        List.of(),
                        targetTable.id(),
                        "stale version",
                        "supervisor_1",
                        0L
                )
        ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_TABLE_SESSION_VERSION_CONFLICT");
    }

    private BarTableRecord createTable(String key, String barId, String status, long minConsumptionCent) {
        return service.createTable(new BarTableRequest(
                "table-" + key,
                "create table",
                List.of(),
                barId,
                "VIP " + key,
                "card_seat",
                minConsumptionCent,
                2,
                status
        ));
    }

    private BarTableSessionRecord createSession(String key, String tableId, String barId, String merchantId) {
        return service.createTableSession(new BarTableSessionRequest(
                "session-" + key,
                "open session",
                List.of("att_session"),
                barId,
                merchantId,
                LocalDate.parse("2026-06-20"),
                tableId,
                null,
                "customer_" + key,
                2,
                "staff_sales",
                "staff_service",
                "supervisor_1"
        ));
    }

    // ========================================================================
    // V091 成熟 POS 深度：退菜退酒 / 反结 / 沽清 / 小费
    // ========================================================================

    @Test
    void voidPendingOrderItemCancelsLineWithoutStockMovement() {
        BarReservationRecord reservation = createReservation("void-pending", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-void-pending", "create product", List.of(), "bar_1", "Beer", "beer",
                3000L, null, null, "active", null, null, false));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-void-pending", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)), null, List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        BarOrderItemVoidRecord voided = service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-pending-1", "wrong item", List.of(), orderItemId, "customer changed mind", false, null));

        assertThat(voided.voidType()).isEqualTo("cancel");
        assertThat(voided.wasDelivered()).isFalse();
        assertThat(voided.returnToStock()).isFalse();
        assertThat(barRepository.orderItems.get(orderItemId).voided()).isTrue();
        assertThat(barRepository.orderItems.get(orderItemId).deliveryStatus()).isEqualTo("voided");
        assertThat(barRepository.stockMovements).isEmpty();
    }

    @Test
    void voidDeliveredOrderItemReturnsStockAndDeductsBill() {
        BarReservationRecord reservation = createReservation("void-return", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-void-return", "create product", List.of(), "bar_1", "Champagne", "spirit",
                100000L, null, null, "active", "inventory_void_return", "bar_1", true));
        barRepository.saveStockBalance(new StockBalanceSnapshot(
                "balance-void-return", "inventory_void_return", "bar_1", 5, 0, 5, Instant.now()));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-void-return", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)), null, List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();
        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-void-return", "deliver", List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2)), "staff_1", List.of("att1")));
        // stock decreased to 3 after delivery
        assertThat(barRepository.findStockBalance("inventory_void_return", "bar_1"))
                .get().extracting(StockBalanceSnapshot::quantity).isEqualTo(3);
        BarBillRecord bill = service.createOrderBill(order.id(), new BarOrderBillCreateRequest(
                "bill-void-return", "create bill", List.of(), "offline", false, 0));
        long payableBefore = bill.payableAmountCent();

        BarOrderItemVoidRecord voided = service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-return-1", "quality issue", List.of(), orderItemId, "spoiled", true, bill.id()));

        assertThat(voided.voidType()).isEqualTo("return");
        assertThat(voided.wasDelivered()).isTrue();
        assertThat(voided.returnToStock()).isTrue();
        // stock restored to 5, inbound movement recorded
        assertThat(barRepository.findStockBalance("inventory_void_return", "bar_1"))
                .get().extracting(StockBalanceSnapshot::quantity).isEqualTo(5);
        assertThat(barRepository.stockMovements.stream().anyMatch(m -> "inbound".equals(m.movementType()))).isTrue();
        // bill payable deducted by the voided line amount
        assertThat(barRepository.bills.get(bill.id()).payableAmountCent())
                .isEqualTo(payableBefore - 200000L);
    }

    @Test
    void voidAlreadyVoidedOrderItemIsRejected() {
        BarReservationRecord reservation = createReservation("void-twice", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-void-twice", "create product", List.of(), "bar_1", "Beer", "beer",
                3000L, null, null, "active", null, null, false));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-void-twice", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)), null, List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();
        service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-twice-1", "x", List.of(), orderItemId, "first", false, null));

        assertThatThrownBy(() -> service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-twice-2", "y", List.of(), orderItemId, "second", false, null)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_ORDER_ITEM_ALREADY_VOIDED");
    }

    @Test
    void voidApprovalDecisionWritesBackApprovedStatusToVoidLedger() {
        BarReservationRecord reservation = createReservation("void-approve", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-void-approve", "create product", List.of(), "bar_1", "Beer", "beer",
                3000L, null, null, "active", null, null, false));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-void-approve", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)), null, List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        BarOrderItemVoidRecord voided = service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-approve-1", "x", List.of(), orderItemId, "guest left", false, null));
        // 高危需审批 → 流水落待审批态。
        assertThat(voided.approvalRequired()).isTrue();
        assertThat(voided.status()).isEqualTo("pending");

        BarOrderItemVoidRecord decided = service.decideOrderItemVoid(voided.id(),
                new BarOrderItemVoidDecisionRequest("void-approve-decide-1", "approved", "supervisor ok"));

        // 审批通过 → status 回写 approved，落审批人/时点/理由。
        assertThat(decided.status()).isEqualTo("approved");
        assertThat(decided.reviewedByUserId()).isNotNull();
        assertThat(decided.reviewedAt()).isNotNull();
        assertThat(decided.reviewReason()).isEqualTo("supervisor ok");
        assertThat(barRepository.findOrderItemVoidById(voided.id()))
                .get().extracting(BarOrderItemVoidRecord::status).isEqualTo("approved");
    }

    @Test
    void voidRejectionDecisionWritesBackRejectedStatusToVoidLedger() {
        BarReservationRecord reservation = createReservation("void-reject", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-void-reject", "create product", List.of(), "bar_1", "Beer", "beer",
                3000L, null, null, "active", null, null, false));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-void-reject", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)), null, List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        BarOrderItemVoidRecord voided = service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-reject-1", "x", List.of(), orderItemId, "mistake", false, null));
        assertThat(voided.status()).isEqualTo("pending");

        BarOrderItemVoidRecord decided = service.decideOrderItemVoid(voided.id(),
                new BarOrderItemVoidDecisionRequest("void-reject-decide-1", "rejected", "not justified"));

        // 审批驳回 → status 回写 rejected。
        assertThat(decided.status()).isEqualTo("rejected");
        assertThat(decided.reviewedByUserId()).isNotNull();
        assertThat(barRepository.findOrderItemVoidById(voided.id()))
                .get().extracting(BarOrderItemVoidRecord::status).isEqualTo("rejected");

        // 已决策（非 pending）再次决策被拒。
        assertThatThrownBy(() -> service.decideOrderItemVoid(voided.id(),
                new BarOrderItemVoidDecisionRequest("void-reject-decide-2", "approved", "late")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_ORDER_ITEM_VOID_NOT_PENDING");
    }

    // ========================================================================
    // 延期项④：退菜退酒审批驳回的资金/库存反向冲销（恢复 void 前状态）。
    // ========================================================================

    @Test
    void voidRejectionReversesStockAndBillBackToPreVoidState() {
        BarReservationRecord reservation = createReservation("void-rej-reverse", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-void-rej-reverse", "create product", List.of(), "bar_1", "Champagne", "spirit",
                100000L, null, null, "active", "inventory_void_rej_reverse", "bar_1", true));
        barRepository.saveStockBalance(new StockBalanceSnapshot(
                "balance-void-rej-reverse", "inventory_void_rej_reverse", "bar_1", 5, 0, 5, Instant.now()));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-void-rej-reverse", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)), null, List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();
        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-void-rej-reverse", "deliver", List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2)), "staff_1", List.of("att1")));
        // 出品后库存 5→3。
        assertThat(barRepository.findStockBalance("inventory_void_rej_reverse", "bar_1"))
                .get().extracting(StockBalanceSnapshot::quantity).isEqualTo(3);
        BarBillRecord bill = service.createOrderBill(order.id(), new BarOrderBillCreateRequest(
                "bill-void-rej-reverse", "create bill", List.of(), "offline", false, 0));
        long payableBeforeVoid = bill.payableAmountCent();

        // void：回补库存 3→5、账单减 200000。
        BarOrderItemVoidRecord voided = service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-rej-reverse-1", "quality issue", List.of(), orderItemId, "spoiled", true, bill.id()));
        assertThat(barRepository.findStockBalance("inventory_void_rej_reverse", "bar_1"))
                .get().extracting(StockBalanceSnapshot::quantity).isEqualTo(5);
        assertThat(barRepository.bills.get(bill.id()).payableAmountCent()).isEqualTo(payableBeforeVoid - 200000L);

        // 驳回：对称冲销 → 库存回到 void 前 3、账单回到 void 前 payable、明细恢复 delivered/未撤单。
        BarOrderItemVoidRecord decided = service.decideOrderItemVoid(voided.id(),
                new BarOrderItemVoidDecisionRequest("void-rej-reverse-decide", "rejected", "not justified"));
        assertThat(decided.status()).isEqualTo("rejected");
        assertThat(barRepository.findStockBalance("inventory_void_rej_reverse", "bar_1"))
                .get().extracting(StockBalanceSnapshot::quantity).isEqualTo(3);
        assertThat(barRepository.bills.get(bill.id()).payableAmountCent()).isEqualTo(payableBeforeVoid);
        // outbound 冲销流水落库。
        assertThat(barRepository.stockMovements.stream()
                .anyMatch(m -> "outbound".equals(m.movementType())
                        && "bar_order_item_void_reject".equals(m.businessType()))).isTrue();
        // 反向账单调整落 waiver_reversal（未结账单）。
        assertThat(barRepository.billAdjustments.stream()
                .anyMatch(a -> "waiver_reversal".equals(a.adjustmentType()))).isTrue();
        // 明细恢复未撤单 + delivered。
        BarOrderItemRecord restored = barRepository.orderItems.get(orderItemId);
        assertThat(restored.voided()).isFalse();
        assertThat(restored.deliveryStatus()).isEqualTo("delivered");
    }

    @Test
    void voidApprovalKeepsAppliedSideEffectsWithoutReversal() {
        BarReservationRecord reservation = createReservation("void-appr-keep", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-void-appr-keep", "create product", List.of(), "bar_1", "Champagne", "spirit",
                100000L, null, null, "active", "inventory_void_appr_keep", "bar_1", true));
        barRepository.saveStockBalance(new StockBalanceSnapshot(
                "balance-void-appr-keep", "inventory_void_appr_keep", "bar_1", 5, 0, 5, Instant.now()));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-void-appr-keep", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)), null, List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();
        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-void-appr-keep", "deliver", List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2)), "staff_1", List.of("att1")));
        BarBillRecord bill = service.createOrderBill(order.id(), new BarOrderBillCreateRequest(
                "bill-void-appr-keep", "create bill", List.of(), "offline", false, 0));
        long payableBeforeVoid = bill.payableAmountCent();

        BarOrderItemVoidRecord voided = service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-appr-keep-1", "quality issue", List.of(), orderItemId, "spoiled", true, bill.id()));
        // void 副作用：库存回补 5、账单减项。
        long payableAfterVoid = barRepository.bills.get(bill.id()).payableAmountCent();
        assertThat(payableAfterVoid).isEqualTo(payableBeforeVoid - 200000L);

        // 通过：保持已施加副作用，不冲销。
        service.decideOrderItemVoid(voided.id(),
                new BarOrderItemVoidDecisionRequest("void-appr-keep-decide", "approved", "supervisor ok"));
        assertThat(barRepository.findStockBalance("inventory_void_appr_keep", "bar_1"))
                .get().extracting(StockBalanceSnapshot::quantity).isEqualTo(5);
        assertThat(barRepository.bills.get(bill.id()).payableAmountCent()).isEqualTo(payableAfterVoid);
        // 无冲销流水/反向调整。
        assertThat(barRepository.stockMovements.stream()
                .noneMatch(m -> "bar_order_item_void_reject".equals(m.businessType()))).isTrue();
        assertThat(barRepository.billAdjustments.stream()
                .noneMatch(a -> a.adjustmentType().endsWith("_reversal"))).isTrue();
        assertThat(barRepository.orderItems.get(orderItemId).voided()).isTrue();
    }

    @Test
    void voidRejectionReversalIsIdempotentBecauseSecondDecisionIsBlocked() {
        BarReservationRecord reservation = createReservation("void-rej-idem", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-void-rej-idem", "create product", List.of(), "bar_1", "Champagne", "spirit",
                100000L, null, null, "active", "inventory_void_rej_idem", "bar_1", true));
        barRepository.saveStockBalance(new StockBalanceSnapshot(
                "balance-void-rej-idem", "inventory_void_rej_idem", "bar_1", 5, 0, 5, Instant.now()));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-void-rej-idem", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)), null, List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();
        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-void-rej-idem", "deliver", List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2)), "staff_1", List.of("att1")));
        BarBillRecord bill = service.createOrderBill(order.id(), new BarOrderBillCreateRequest(
                "bill-void-rej-idem", "create bill", List.of(), "offline", false, 0));
        BarOrderItemVoidRecord voided = service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-rej-idem-1", "quality issue", List.of(), orderItemId, "spoiled", true, bill.id()));

        service.decideOrderItemVoid(voided.id(),
                new BarOrderItemVoidDecisionRequest("void-rej-idem-decide-1", "rejected", "not justified"));
        int stockAfterFirst = barRepository.findStockBalance("inventory_void_rej_idem", "bar_1")
                .map(StockBalanceSnapshot::quantity).orElseThrow();
        long reversalAdjustmentsAfterFirst = barRepository.billAdjustments.stream()
                .filter(a -> a.adjustmentType().endsWith("_reversal")).count();

        // 二次决策被 pending 守卫拦截 → 不会重复冲销（幂等）。
        assertThatThrownBy(() -> service.decideOrderItemVoid(voided.id(),
                new BarOrderItemVoidDecisionRequest("void-rej-idem-decide-2", "rejected", "again")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_ORDER_ITEM_VOID_NOT_PENDING");
        assertThat(barRepository.findStockBalance("inventory_void_rej_idem", "bar_1")
                .map(StockBalanceSnapshot::quantity).orElseThrow()).isEqualTo(stockAfterFirst);
        assertThat(barRepository.billAdjustments.stream()
                .filter(a -> a.adjustmentType().endsWith("_reversal")).count())
                .isEqualTo(reversalAdjustmentsAfterFirst);
    }

    @Test
    void reopenSettledBillTransitionsToReopenedAndAllowsResettle() {
        BarBillRecord bill = billForOrder("reopen-flow", null);
        service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-reopen-flow", "settle", List.of(), "pay_reopen_flow", null, "cash"));
        assertThat(barRepository.bills.get(bill.id()).settlementStatus()).isEqualTo("settled");

        BarBillOperationRecord op = service.reopenBill(bill.id(), new BarBillReopenRequest(
                "reopen-flow-1", "reopen", List.of(), "wrong payment method"));

        assertThat(op.operationType()).isEqualTo("reopen");
        BarBillRecord reopened = barRepository.bills.get(bill.id());
        assertThat(reopened.reopened()).isTrue();
        assertThat(reopened.settlementStatus()).isEqualTo("unsettled");
        assertThat(reopened.reopenCount()).isEqualTo(1);

        // re-settle after reopen succeeds
        BarBillRecord resettled = service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-reopen-flow-2", "resettle", List.of(), "pay_reopen_flow_2", null, "pos"));
        assertThat(resettled.settlementStatus()).isEqualTo("settled");
    }

    @Test
    void reopenUnsettledBillIsRejected() {
        BarBillRecord bill = billForOrder("reopen-unsettled", null);
        assertThatThrownBy(() -> service.reopenBill(bill.id(), new BarBillReopenRequest(
                "reopen-unsettled-1", "reopen", List.of(), "no")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_BILL_NOT_REOPENABLE");
    }

    @Test
    void reopenSettledBillOnLockedBusinessDayRequiresBackfill() {
        BarBillRecord bill = billForOrder("reopen-locked", null);
        service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-reopen-locked", "settle", List.of(), "pay_reopen_locked", null, "cash"));
        receivableLedgerService.businessDayLocked = true;

        assertThatThrownBy(() -> service.reopenBill(bill.id(), new BarBillReopenRequest(
                "reopen-locked-1", "reopen", List.of(), "late fix")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("DAY_CLOSE_LOCKED");
    }

    @Test
    void soldOutProductIsRejectedOnOrderThenRestored() {
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-soldout", "create product", List.of(), "bar_1", "Limited Whisky", "spirit",
                500000L, null, null, "active", null, null, false));
        BarReservationRecord reservation = createReservation("soldout", "bar_1", 2);

        BarProductRecord soldOut = service.markProductSoldOut(product.id(), new BarProductSoldOutRequest(
                "soldout-1", "out of stock", List.of(), true, "kegs empty"));
        assertThat(soldOut.soldOut()).isTrue();

        assertThatThrownBy(() -> service.createOrder(new BarOrderRequest(
                "order-soldout", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)), null, List.of(), "offline")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("PRODUCT_SOLD_OUT");

        BarProductRecord restored = service.markProductSoldOut(product.id(), new BarProductSoldOutRequest(
                "soldout-2", "back in stock", List.of(), false, "refilled"));
        assertThat(restored.soldOut()).isFalse();

        // now ordering succeeds
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-soldout-ok", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)), null, List.of(), "offline"));
        assertThat(order.totalAmountCent()).isEqualTo(500000L);
    }

    @Test
    void addTipIncreasesBillAndSettlementIncludesTip() {
        BarBillRecord bill = billForOrder("tip-flow", null);
        long payable = bill.payableAmountCent();

        BarBillRecord tipped = service.addBillTip(bill.id(), new BarBillTipRequest(
                "tip-flow-1", "tip", List.of(), 5000L, null));
        assertThat(tipped.tipAmountCent()).isEqualTo(5000L);

        // settling for less than payable+tip is partial; full amount settles
        BarBillRecord settled = service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-tip-flow", "settle", List.of(), "pay_tip_flow", payable + 5000L, "cash"));
        assertThat(settled.settlementStatus()).isEqualTo("settled");
        assertThat(settled.paidAmountCent()).isEqualTo(payable + 5000L);
    }

    @Test
    void settlementExceedingPayablePlusTipIsRejected() {
        BarBillRecord bill = billForOrder("tip-exceed", null);
        long payable = bill.payableAmountCent();
        service.addBillTip(bill.id(), new BarBillTipRequest("tip-exceed-1", "tip", List.of(), 1000L, null));

        assertThatThrownBy(() -> service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-tip-exceed", "settle", List.of(), "pay_tip_exceed", payable + 1000L + 1L, "cash")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    private BarReservationRecord createReservation(String key, String barId, int allowedFreeGuests) {
        BarTableRecord table = service.createTable(new BarTableRequest(
                "table-" + key,
                "create table",
                List.of(),
                barId,
                "VIP " + key,
                "card_seat",
                50000L,
                allowedFreeGuests,
                "active"
        ));
        return service.createReservation(new BarReservationRequest(
                key,
                "create reservation",
                List.of(),
                barId,
                table.id(),
                Instant.parse("2026-06-20T21:00:00Z"),
                allowedFreeGuests,
                "staff_sales",
                null
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // V095: 交班 / 班结对账（PRD §10.1）
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void closeShiftAggregatesSettlementsByChannelRefundAndTipIntoSystemReceivable() {
        BarShiftRecord shift = service.openShift(new BarShiftOpenRequest(
                "open-1", "bar_1", "cashier_1", null, 50_000L, 0L, null));

        // 三笔不同支付方式结算 + 一笔小费账单，均归属 cashier_1。
        BarBillRecord cashBill = billForOrder("agg-cash", "staff_sales");
        service.settleBill(cashBill.id(), new BarBillSettleRequest(
                "s-cash", null, List.of(), "pay_cash", cashBill.payableAmountCent(),
                "cash", "cashier_1", null));
        BarBillRecord posBill = billForOrder("agg-pos", "staff_sales");
        service.settleBill(posBill.id(), new BarBillSettleRequest(
                "s-pos", null, List.of(), "pay_pos", posBill.payableAmountCent(),
                "pos", "cashier_1", "POS123"));
        BarBillRecord onlineBill = billForOrder("agg-online", "staff_sales");
        service.settleBill(onlineBill.id(), new BarBillSettleRequest(
                "s-online", null, List.of(), "pay_online", onlineBill.payableAmountCent(),
                "aggregate_pay", "cashier_1", null));

        // 小费账单：加 1000 分小费后用现金结算（结算含小费），系统现金应收含 tip，tip 另行单列。
        BarBillRecord tipBill = billForOrder("agg-tip", "staff_sales");
        service.addBillTip(tipBill.id(), new BarBillTipRequest("tip-1", null, List.of(), 1_000L, null));
        BarBillRecord tippedReloaded = barRepository.findBillById(tipBill.id()).orElseThrow();
        service.settleBill(tipBill.id(), new BarBillSettleRequest(
                "s-tip", null, List.of(), "pay_tip",
                tippedReloaded.payableAmountCent() + tippedReloaded.tipAmountCent(),
                "cash", "cashier_1", null));

        BarShiftRecord closed = service.closeShift(shift.id(), new BarShiftCloseRequest(
                "close-1", 50_000L + cashBill.payableAmountCent()
                        + tippedReloaded.payableAmountCent() + tippedReloaded.tipAmountCent(),
                "cashier_2", "end of night"));

        assertThat(closed.status()).isEqualTo("closed");
        assertThat(closed.systemCashCent())
                .isEqualTo(cashBill.payableAmountCent() + tippedReloaded.payableAmountCent() + 1_000L);
        assertThat(closed.systemPosCent()).isEqualTo(posBill.payableAmountCent());
        assertThat(closed.systemOnlineCent()).isEqualTo(onlineBill.payableAmountCent());
        assertThat(closed.systemTipCent()).isEqualTo(1_000L);
        assertThat(closed.systemRefundCent()).isZero();
        assertThat(closed.handoverToUserId()).isEqualTo("cashier_2");
    }

    @Test
    void closeShiftComputesCashDiffAgainstDeclaredCashAndOpeningFloat() {
        BarShiftRecord shift = service.openShift(new BarShiftOpenRequest(
                "open-diff", "bar_1", "cashier_diff", null, 20_000L, 0L, null));
        BarBillRecord cashBill = billForOrder("diff-cash", "staff_sales");
        service.settleBill(cashBill.id(), new BarBillSettleRequest(
                "s-diff", null, List.of(), "pay_diff", cashBill.payableAmountCent(),
                "cash", "cashier_diff", null));

        // 实点现金 = 备用金 + 系统现金 + 300 长款。
        long declared = 20_000L + cashBill.payableAmountCent() + 300L;
        BarShiftRecord closed = service.closeShift(shift.id(), new BarShiftCloseRequest(
                "close-diff", declared, null, null));

        assertThat(closed.systemCashCent()).isEqualTo(cashBill.payableAmountCent());
        assertThat(closed.cashDiffCent()).isEqualTo(300L);
        assertThat(closed.needsReview()).isTrue();
    }

    @Test
    void closeShiftBalancesWhenDeclaredCashMatchesSystemAndDoesNotNeedReview() {
        BarShiftRecord shift = service.openShift(new BarShiftOpenRequest(
                "open-bal", "bar_1", "cashier_bal", null, 10_000L, 500L, null));
        BarBillRecord cashBill = billForOrder("bal-cash", "staff_sales");
        service.settleBill(cashBill.id(), new BarBillSettleRequest(
                "s-bal", null, List.of(), "pay_bal", cashBill.payableAmountCent(),
                "cash", "cashier_bal", null));

        BarShiftRecord closed = service.closeShift(shift.id(), new BarShiftCloseRequest(
                "close-bal", 10_000L + cashBill.payableAmountCent(), null, null));

        assertThat(closed.cashDiffCent()).isZero();
        assertThat(closed.needsReview()).isFalse();
    }

    @Test
    void closeShiftAggregatesRefundWithinWindow() {
        BarShiftRecord shift = service.openShift(new BarShiftOpenRequest(
                "open-refund", "bar_1", "cashier_refund", null, 0L, 0L, null));
        BarBillRecord bill = billForOrder("refund-bill", "staff_sales");
        service.settleBill(bill.id(), new BarBillSettleRequest(
                "s-refund", null, List.of(), "pay_refund", bill.payableAmountCent(),
                "cash", "cashier_refund", null));
        service.applyBillAdjustment(bill.id(), new BarBillAdjustmentRequest(
                "refund-1", null, List.of(), "refund", 5_000L, "guest complaint", null));

        BarShiftRecord closed = service.closeShift(shift.id(), new BarShiftCloseRequest(
                "close-refund", bill.payableAmountCent(), null, null));

        assertThat(closed.systemRefundCent()).isEqualTo(5_000L);
    }

    @Test
    void openShiftRejectsDuplicateOpenShiftForSameCashierAndBar() {
        service.openShift(new BarShiftOpenRequest("open-dup-1", "bar_1", "cashier_dup", null, 0L, 0L, null));
        assertThatThrownBy(() -> service.openShift(
                new BarShiftOpenRequest("open-dup-2", "bar_1", "cashier_dup", null, 0L, 0L, null)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_SHIFT_ALREADY_OPEN");
    }

    @Test
    void closeShiftRejectsAlreadyClosedShift() {
        BarShiftRecord shift = service.openShift(new BarShiftOpenRequest(
                "open-closed", "bar_1", "cashier_closed", null, 0L, 0L, null));
        service.closeShift(shift.id(), new BarShiftCloseRequest("close-once", 0L, null, null));
        assertThatThrownBy(() -> service.closeShift(shift.id(),
                new BarShiftCloseRequest("close-twice", 0L, null, null)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_SHIFT_NOT_OPEN");
    }

    @Test
    void closeShiftRejectsCrossMerchantAccessWithForbiddenError() {
        TestSecurityActors.asMerchantUser("merchant_a");
        BarShiftRecord shift = barRepository.saveShift(new BarShiftRecord(
                "bar_shift_other", null, "merchant_b", "bar_1", "cashier_x", "open",
                Instant.now(), null, 0L, null, 0L, 0L, 0L, 0L, 0L, null, 0L, false,
                null, null, "opener", null, Instant.now(), Instant.now()));
        assertThatThrownBy(() -> service.closeShift(shift.id(),
                new BarShiftCloseRequest("close-x", 0L, null, null)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void listShiftsReturnsOnlyCurrentMerchantRecordsWhenScopedToMerchant() {
        barRepository.saveShift(new BarShiftRecord(
                "bar_shift_a", null, "merchant_a", "bar_1", "c1", "open",
                Instant.now(), null, 0L, null, 0L, 0L, 0L, 0L, 0L, null, 0L, false,
                null, null, "opener", null, Instant.now(), Instant.now()));
        barRepository.saveShift(new BarShiftRecord(
                "bar_shift_b", null, "merchant_b", "bar_1", "c2", "open",
                Instant.now(), null, 0L, null, 0L, 0L, 0L, 0L, 0L, null, 0L, false,
                null, null, "opener", null, Instant.now(), Instant.now()));

        TestSecurityActors.asMerchantUser("merchant_a");
        assertThat(service.listShifts().items())
                .extracting(BarShiftRecord::id)
                .containsExactly("bar_shift_a");
    }

    private BarBillRecord billForOrder(String key, String salesStaffId) {
        BarReservationRecord reservation = createReservation("reservation-" + key, "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-" + key,
                "create product",
                List.of(),
                "bar_1",
                "Whisky " + key,
                "spirit",
                128800L,
                null,
                null,
                "active",
                null,
                null,
                false
        ));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-" + key,
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)),
                salesStaffId,
                List.of(),
                "offline"
        ));
        return service.createOrderBill(order.id(), new BarOrderBillCreateRequest(
                "bill-" + key,
                "create bill",
                List.of(),
                "offline",
                false,
                0
        ));
    }

    private VerificationRequest verification(String idempotencyKey, String guestIdentifier, String barId) {
        return new VerificationRequest(
                idempotencyKey,
                "manual",
                guestIdentifier,
                "device_1",
                barId,
                Map.of("bar_id", barId)
        );
    }

    private static final class InMemoryBarRepository implements BarRepository {
        private final Map<String, BarTableRecord> tables = new LinkedHashMap<>();
        private final Map<String, BarReservationRecord> reservations = new LinkedHashMap<>();
        private final Map<String, BarDepositRuleRecord> depositRules = new LinkedHashMap<>();
        private final Map<String, BarProductRecord> products = new LinkedHashMap<>();
        private final Map<String, BarOrderRecord> orders = new LinkedHashMap<>();
        private final Map<String, BarOrderItemRecord> orderItems = new LinkedHashMap<>();
        private final List<BarOrderItemVoidRecord> orderItemVoids = new ArrayList<>();
        private final Map<String, BarBillRecord> bills = new LinkedHashMap<>();
        private final List<BarBillSettlementRecord> billSettlements = new ArrayList<>();
        private final Map<String, BarShiftRecord> shifts = new LinkedHashMap<>();
        private final List<BarBillAdjustmentRecord> billAdjustments = new ArrayList<>();
        private final List<BarBillOperationRecord> billOperations = new ArrayList<>();
        private final Map<String, BarTableSessionRecord> tableSessions = new LinkedHashMap<>();
        private final List<BarTableSessionAdjustmentRecord> tableSessionAdjustments = new ArrayList<>();
        private final List<BarTableGuestCheckinRecord> guestCheckins = new ArrayList<>();
        private final List<BarProductionTicketRecord> productionTickets = new ArrayList<>();
        private final List<BarProductionTicketEventRecord> productionTicketEvents = new ArrayList<>();
        private final List<BarCommissionCalculationRecord> commissions = new ArrayList<>();
        private final Map<String, StockBalanceSnapshot> stockBalances = new LinkedHashMap<>();
        private final List<StockMovementSnapshot> stockMovements = new ArrayList<>();

        @Override
        public BarTableRecord saveTable(BarTableRecord table) {
            tables.put(table.id(), table);
            return table;
        }

        @Override
        public List<BarTableRecord> listTables(Optional<String> merchantScope) {
            List<BarTableRecord> all = new ArrayList<>(tables.values());
            if (merchantScope.isEmpty()) {
                return all;
            }
            return all.stream().filter(r -> merchantScope.get().equals(r.merchantId())).toList();
        }

        @Override
        public Optional<BarTableRecord> findTableById(String id) {
            return Optional.ofNullable(tables.get(id));
        }

        @Override
        public BarReservationRecord saveReservation(BarReservationRecord reservation) {
            reservations.put(reservation.id(), reservation);
            return reservation;
        }

        @Override
        public List<BarReservationRecord> listReservations(Optional<String> merchantScope) {
            List<BarReservationRecord> all = new ArrayList<>(reservations.values());
            if (merchantScope.isEmpty()) {
                return all;
            }
            return all.stream().filter(r -> merchantScope.get().equals(r.merchantId())).toList();
        }

        @Override
        public Optional<BarReservationRecord> findReservationById(String id) {
            return Optional.ofNullable(reservations.get(id));
        }

        @Override
        public BarDepositRuleRecord saveDepositRule(BarDepositRuleRecord rule) {
            depositRules.put(rule.id(), rule);
            return rule;
        }

        @Override
        public List<BarDepositRuleRecord> listDepositRules(String barId) {
            return depositRules.values().stream()
                    .filter(r -> r.enabled() && barId.equals(r.barId()))
                    .sorted((a, b) -> b.scopeType().compareTo(a.scopeType()))
                    .toList();
        }

        @Override
        public int countGuestCheckins(String reservationId) {
            return (int) guestCheckins.stream()
                    .filter(checkin -> reservationId.equals(checkin.reservationId()))
                    .count();
        }

        @Override
        public BarTableGuestCheckinRecord saveGuestCheckin(BarTableGuestCheckinRecord checkin) {
            guestCheckins.add(checkin);
            return checkin;
        }

        @Override
        public BarAgeVerificationRecord saveAgeVerification(BarAgeVerificationRecord verification) {
            return verification;
        }

        @Override
        public BarProductRecord saveProduct(BarProductRecord product) {
            products.put(product.id(), product);
            return product;
        }

        @Override
        public List<BarProductRecord> listProducts(Optional<String> merchantScope) {
            List<BarProductRecord> all = new ArrayList<>(products.values());
            if (merchantScope.isEmpty()) {
                return all;
            }
            return all.stream().filter(r -> merchantScope.get().equals(r.merchantId())).toList();
        }

        @Override
        public Optional<BarProductRecord> findProductById(String id) {
            return Optional.ofNullable(products.get(id));
        }

        @Override
        public BarOrderRecord saveOrder(BarOrderRecord order) {
            orders.put(order.id(), order);
            return order;
        }

        @Override
        public List<BarOrderRecord> listOrders(Optional<String> merchantScope) {
            List<BarOrderRecord> all = new ArrayList<>(orders.values());
            if (merchantScope.isEmpty()) {
                return all;
            }
            return all.stream().filter(r -> merchantScope.get().equals(r.merchantId())).toList();
        }

        @Override
        public Optional<BarOrderRecord> findOrderById(String id) {
            return Optional.ofNullable(orders.get(id));
        }

        @Override
        public BarOrderItemRecord saveOrderItem(BarOrderItemRecord item) {
            orderItems.put(item.id(), item);
            return item;
        }

        @Override
        public List<BarOrderItemRecord> listOrderItems(String orderId) {
            return orderItems.values().stream()
                    .filter(item -> orderId.equals(item.orderId()))
                    .toList();
        }

        @Override
        public Optional<BarOrderItemRecord> findOrderItemById(String id) {
            return Optional.ofNullable(orderItems.get(id));
        }

        @Override
        public BarOrderItemVoidRecord saveOrderItemVoid(BarOrderItemVoidRecord voidRecord) {
            orderItemVoids.removeIf(existing -> existing.id().equals(voidRecord.id()));
            orderItemVoids.add(voidRecord);
            return voidRecord;
        }

        @Override
        public List<BarOrderItemVoidRecord> listOrderItemVoids(String orderId) {
            return orderItemVoids.stream().filter(v -> orderId.equals(v.orderId())).toList();
        }

        @Override
        public java.util.Optional<BarOrderItemVoidRecord> findOrderItemVoidById(String id) {
            return orderItemVoids.stream().filter(v -> v.id().equals(id)).findFirst();
        }

        @Override
        public BarProductionTicketRecord saveProductionTicket(BarProductionTicketRecord ticket) {
            productionTickets.removeIf(existing -> existing.id().equals(ticket.id()));
            productionTickets.add(ticket);
            return ticket;
        }

        @Override
        public List<BarProductionTicketRecord> listProductionTickets(Optional<String> merchantScope) {
            List<BarProductionTicketRecord> all = List.copyOf(productionTickets);
            if (merchantScope.isEmpty()) {
                return all;
            }
            return all.stream().filter(r -> merchantScope.get().equals(r.merchantId())).toList();
        }

        @Override
        public Optional<BarProductionTicketRecord> findProductionTicketById(String id) {
            return productionTickets.stream()
                    .filter(ticket -> id.equals(ticket.id()))
                    .findFirst();
        }

        @Override
        public Optional<BarProductionTicketRecord> findProductionTicketByOrderId(String orderId) {
            return productionTickets.stream()
                    .filter(ticket -> orderId.equals(ticket.orderId()))
                    .findFirst();
        }

        @Override
        public BarProductionTicketEventRecord saveProductionTicketEvent(BarProductionTicketEventRecord event) {
            productionTicketEvents.add(event);
            return event;
        }

        @Override
        public List<BarProductionTicketEventRecord> listProductionTicketEvents(String ticketId) {
            return productionTicketEvents.stream()
                    .filter(event -> ticketId.equals(event.ticketId()))
                    .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                    .toList();
        }

        @Override
        public BarBillRecord saveBill(BarBillRecord bill) {
            bills.put(bill.id(), bill);
            return bill;
        }

        @Override
        public Optional<BarBillRecord> findBillById(String id) {
            return Optional.ofNullable(bills.get(id));
        }

        @Override
        public BarBillAdjustmentRecord saveBillAdjustment(BarBillAdjustmentRecord adjustment) {
            billAdjustments.add(adjustment);
            return adjustment;
        }

        @Override
        public List<BarBillAdjustmentRecord> listBillAdjustments(String billId) {
            return billAdjustments.stream()
                    .filter(adjustment -> billId.equals(adjustment.billId()))
                    .toList();
        }

        @Override
        public BarBillOperationRecord saveBillOperation(BarBillOperationRecord operation) {
            billOperations.removeIf(existing ->
                    existing.primaryBillId().equals(operation.primaryBillId())
                            && existing.idempotencyKey().equals(operation.idempotencyKey()));
            billOperations.add(operation);
            return operation;
        }

        @Override
        public List<BarBillOperationRecord> listBillOperations(String billId) {
            return billOperations.stream()
                    .filter(operation -> billId.equals(operation.primaryBillId())
                            || billId.equals(operation.secondaryBillId())
                            || billId.equals(operation.generatedBillId()))
                    .toList();
        }

        @Override
        public BarBillSettlementRecord saveBillSettlement(BarBillSettlementRecord settlement) {
            billSettlements.removeIf(existing -> existing.id().equals(settlement.id()));
            billSettlements.add(settlement);
            return settlement;
        }

        @Override
        public List<BarBillSettlementRecord> listBillSettlements(String billId) {
            return billSettlements.stream().filter(s -> billId.equals(s.billId())).toList();
        }

        @Override
        public BarShiftRecord saveShift(BarShiftRecord shift) {
            shifts.put(shift.id(), shift);
            return shift;
        }

        @Override
        public Optional<BarShiftRecord> findShiftById(String id) {
            return Optional.ofNullable(shifts.get(id));
        }

        @Override
        public Optional<BarShiftRecord> findOpenShift(String barId, String cashierUserId) {
            return shifts.values().stream()
                    .filter(s -> "open".equals(s.status())
                            && barId.equals(s.barId())
                            && cashierUserId.equals(s.cashierUserId()))
                    .findFirst();
        }

        @Override
        public List<BarShiftRecord> listShifts(Optional<String> merchantScope) {
            List<BarShiftRecord> all = new ArrayList<>(shifts.values());
            if (merchantScope.isEmpty()) {
                return all;
            }
            return all.stream().filter(r -> merchantScope.get().equals(r.merchantId())).toList();
        }

        @Override
        public BarShiftReconciliationSnapshot aggregateShiftReconciliation(
                String barId,
                String cashierUserId,
                Instant from,
                Instant to,
                Optional<String> merchantScope
        ) {
            long cash = 0L;
            long pos = 0L;
            long online = 0L;
            long tip = 0L;
            java.util.Set<String> tippedBills = new java.util.LinkedHashSet<>();
            for (BarBillSettlementRecord s : billSettlements) {
                BarBillRecord bill = bills.get(s.billId());
                if (bill == null || !barId.equals(bill.barId())) {
                    continue;
                }
                if (!cashierUserId.equals(s.cashierStaffId())) {
                    continue;
                }
                if (merchantScope.isPresent() && !merchantScope.get().equals(bill.merchantId())) {
                    continue;
                }
                Instant at = s.settledAt();
                if (at == null || at.isBefore(from) || !at.isBefore(to)) {
                    continue;
                }
                switch (s.settleChannel()) {
                    case "cash" -> cash += s.settlementAmountCent();
                    case "pos" -> pos += s.settlementAmountCent();
                    default -> online += s.settlementAmountCent();
                }
                if (bill.tipAmountCent() > 0) {
                    tippedBills.add(bill.id());
                }
            }
            for (String billId : tippedBills) {
                tip += bills.get(billId).tipAmountCent();
            }
            long refund = 0L;
            for (BarBillAdjustmentRecord a : billAdjustments) {
                boolean isRefund = "refund".equals(a.adjustmentType());
                boolean isRefundReversal = "refund_reversal".equals(a.adjustmentType());
                if ((!isRefund && !isRefundReversal) || a.refundedAt() == null) {
                    continue;
                }
                BarBillRecord bill = bills.get(a.billId());
                if (bill == null || !barId.equals(bill.barId())) {
                    continue;
                }
                if (merchantScope.isPresent() && !merchantScope.get().equals(bill.merchantId())) {
                    continue;
                }
                Instant at = a.refundedAt();
                if (at.isBefore(from) || !at.isBefore(to)) {
                    continue;
                }
                // 延期项④：refund 计入退款净额，refund_reversal（驳回冲销）抵减，net 归零。
                refund += isRefundReversal ? -a.amountCent() : a.amountCent();
            }
            return new BarShiftReconciliationSnapshot(cash, pos, online, refund, tip);
        }

        @Override
        public BarCommissionCalculationRecord saveCommissionCalculation(BarCommissionCalculationRecord calculation) {
            commissions.add(calculation);
            return calculation;
        }

        @Override
        public BarTableSessionRecord saveTableSession(BarTableSessionRecord session) {
            tableSessions.put(session.id(), session);
            return session;
        }

        @Override
        public List<BarTableSessionRecord> listTableSessions(Optional<String> merchantScope) {
            List<BarTableSessionRecord> all = new ArrayList<>(tableSessions.values());
            if (merchantScope.isEmpty()) {
                return all;
            }
            return all.stream().filter(r -> merchantScope.get().equals(r.merchantId())).toList();
        }

        @Override
        public Optional<BarTableSessionRecord> findTableSessionById(String id) {
            return Optional.ofNullable(tableSessions.get(id));
        }

        @Override
        public BarTableSessionAdjustmentRecord saveTableSessionAdjustment(BarTableSessionAdjustmentRecord adjustment) {
            tableSessionAdjustments.add(adjustment);
            return adjustment;
        }

        @Override
        public List<BarTableSessionAdjustmentRecord> listTableSessionAdjustments(String sessionId) {
            return tableSessionAdjustments.stream()
                    .filter(adjustment -> sessionId.equals(adjustment.sessionId()))
                    .toList();
        }

        @Override
        public Optional<StockBalanceSnapshot> findStockBalance(String itemId, String locationId) {
            return Optional.ofNullable(stockBalances.get(itemId + ":" + locationId));
        }

        @Override
        public boolean atomicDecreaseStock(String itemId, String locationId, int quantity) {
            String key = itemId + ":" + locationId;
            StockBalanceSnapshot current = stockBalances.get(key);
            if (current == null || current.availableQuantity() < quantity) {
                return false;
            }
            stockBalances.put(key, new StockBalanceSnapshot(
                    current.id(),
                    current.itemId(),
                    current.locationId(),
                    current.quantity() - quantity,
                    current.reservedQuantity(),
                    current.availableQuantity() - quantity,
                    Instant.now()
            ));
            return true;
        }

        @Override
        public boolean atomicIncreaseStock(String itemId, String locationId, int quantity) {
            String key = itemId + ":" + locationId;
            StockBalanceSnapshot current = stockBalances.get(key);
            if (current == null) {
                return false;
            }
            stockBalances.put(key, new StockBalanceSnapshot(
                    current.id(),
                    current.itemId(),
                    current.locationId(),
                    current.quantity() + quantity,
                    current.reservedQuantity(),
                    current.availableQuantity() + quantity,
                    Instant.now()
            ));
            return true;
        }

        @Override
        public StockBalanceSnapshot saveStockBalance(StockBalanceSnapshot balance) {
            stockBalances.put(balance.itemId() + ":" + balance.locationId(), balance);
            return balance;
        }

        @Override
        public StockMovementSnapshot saveStockMovement(StockMovementSnapshot movement) {
            stockMovements.add(movement);
            return movement;
        }

        private final Map<String, BarGiftRedemptionRecord> giftRedemptions = new LinkedHashMap<>();

        @Override
        public Optional<BarTableRecord> findFirstTableByBar(String barId) {
            return tables.values().stream().filter(t -> barId.equals(t.barId())).findFirst();
        }

        // 处理清单 #8：内存版空间映射（spaceId -> merchantId；空串表示无商户/共享空间）。
        private final Map<String, String> spaceMerchants = new LinkedHashMap<>();
        private final Map<String, String> tableSpaceIds = new LinkedHashMap<>();

        void registerSpace(String spaceId, String merchantId) {
            spaceMerchants.put(spaceId, merchantId == null ? "" : merchantId);
        }

        String tableSpaceId(String tableId) {
            return tableSpaceIds.get(tableId);
        }

        @Override
        public int assignTableSpace(String tableId, String spaceId) {
            if (!tables.containsKey(tableId)) {
                return 0;
            }
            tableSpaceIds.put(tableId, spaceId);
            return 1;
        }

        @Override
        public Optional<String> findSpaceMerchant(String spaceId) {
            return Optional.ofNullable(spaceMerchants.get(spaceId));
        }

        @Override
        public List<BarTableSpaceBacklogRecord> listTablesWithoutSpace(Optional<String> merchantScope) {
            return tables.values().stream()
                    .filter(t -> tableSpaceIds.get(t.id()) == null)
                    .filter(t -> merchantScope.isEmpty() || merchantScope.get().equals(t.merchantId()))
                    .map(t -> new BarTableSpaceBacklogRecord(
                            t.id(), t.barId(), t.merchantId(), t.tableCode(), t.tableName(), t.status()))
                    .toList();
        }

        @Override
        public boolean insertGiftRedemptionIfAbsent(BarGiftRedemptionRecord redemption) {
            if (giftRedemptions.containsKey(redemption.verificationId())) {
                return false;
            }
            giftRedemptions.put(redemption.verificationId(), redemption);
            return true;
        }

        @Override
        public BarGiftRedemptionRecord saveGiftRedemption(BarGiftRedemptionRecord redemption) {
            giftRedemptions.put(redemption.verificationId(), redemption);
            return redemption;
        }

        @Override
        public Optional<BarGiftRedemptionRecord> findGiftRedemptionByVerification(String verificationId) {
            return Optional.ofNullable(giftRedemptions.get(verificationId));
        }

        @Override
        public List<BarGiftRedemptionRecord> listGiftRedemptions(Optional<String> merchantScope) {
            List<BarGiftRedemptionRecord> all = new ArrayList<>(giftRedemptions.values());
            if (merchantScope.isEmpty()) {
                return all;
            }
            return all.stream().filter(r -> merchantScope.get().equals(r.merchantId())).toList();
        }
    }

    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String key = record.operationId() + ":" + record.idempotencyKey();
            if (records.containsKey(key)) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(record.operationId() + ":" + record.idempotencyKey(), record);
            return record;
        }
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }

    private static final class CapturingBarReceivableLedgerService implements BarReceivableLedgerService {
        private final List<CapturedReceivableRequest> requests = new ArrayList<>();
        private boolean businessDayLocked = false;

        @Override
        public boolean isBusinessDayLocked(java.time.LocalDate businessDate) {
            return businessDayLocked;
        }

        @Override
        public String createReceivableForOnAccountBill(
                BarBillRecord bill,
                BarOrderRecord order,
                String idempotencyKey,
                String operatorNote,
                List<String> evidenceAttachmentIds
        ) {
            requests.add(new CapturedReceivableRequest(
                    bill.id(),
                    order.id(),
                    bill.payableAmountCent(),
                    idempotencyKey,
                    operatorNote,
                    List.copyOf(evidenceAttachmentIds)
            ));
            return "arap_for_" + bill.id();
        }

        private final List<String> commissionPayables = new ArrayList<>();

        @Override
        public String createPayableForCommission(BarCommissionCalculationRecord commission) {
            commissionPayables.add(commission.id());
            return "arap_commission_" + commission.id();
        }

        private final List<CapturedRefundPayable> refundPayables = new ArrayList<>();

        @Override
        public String createRefundPayable(
                BarBillRecord bill,
                BarOrderRecord order,
                String adjustmentId,
                long refundAmountCent,
                String idempotencyKey,
                String reason,
                List<String> evidenceAttachmentIds
        ) {
            refundPayables.add(new CapturedRefundPayable(
                    bill.id(),
                    order.id(),
                    adjustmentId,
                    refundAmountCent,
                    idempotencyKey,
                    reason,
                    List.copyOf(evidenceAttachmentIds)
            ));
            return "arap_refund_" + adjustmentId;
        }

        private final List<CapturedRefundPayable> refundReversals = new ArrayList<>();

        @Override
        public String reverseRefundPayable(
                BarBillRecord bill,
                BarOrderRecord order,
                String reversalAdjustmentId,
                long refundAmountCent,
                String idempotencyKey,
                String reason,
                List<String> evidenceAttachmentIds
        ) {
            refundReversals.add(new CapturedRefundPayable(
                    bill.id(),
                    order.id(),
                    reversalAdjustmentId,
                    refundAmountCent,
                    idempotencyKey,
                    reason,
                    List.copyOf(evidenceAttachmentIds)
            ));
            return "arap_refund_reversal_" + reversalAdjustmentId;
        }
    }

    private record CapturedRefundPayable(
            String billId,
            String orderId,
            String adjustmentId,
            long refundAmountCent,
            String idempotencyKey,
            String reason,
            List<String> evidenceAttachmentIds
    ) {
    }

    private record CapturedReceivableRequest(
            String billId,
            String orderId,
            long amountCent,
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds
    ) {
    }
}
