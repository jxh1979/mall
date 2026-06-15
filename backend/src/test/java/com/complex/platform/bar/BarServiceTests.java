package com.complex.platform.bar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BarServiceTests {

    private InMemoryBarRepository barRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private CapturingBarReceivableLedgerService receivableLedgerService;
    private BarService service;

    @BeforeEach
    void setUp() {
        barRepository = new InMemoryBarRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        receivableLedgerService = new CapturingBarReceivableLedgerService();
        service = new BarService(
                barRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                receivableLedgerService
        );
    }

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
        assertThat(service.listProductionTicketEvents("ticket_production_flow").items())
                .extracting(
                        BarProductionTicketEventRecord::actionType,
                        BarProductionTicketEventRecord::fromStatus,
                        BarProductionTicketEventRecord::toStatus,
                        BarProductionTicketEventRecord::stationCode,
                        BarProductionTicketEventRecord::operatorUserId
                )
                .containsExactly(
                        tuple("mark_ready", "preparing", "ready", "bar-counter-1", "staff_bar_1"),
                        tuple("start_prepare", "accepted", "preparing", "bar-counter-1", "staff_bar_1"),
                        tuple("accept", "created", "accepted", "bar-counter-1", "staff_bar_1")
                );
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.production_ticket.transitioned");
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
        private final Map<String, BarProductRecord> products = new LinkedHashMap<>();
        private final Map<String, BarOrderRecord> orders = new LinkedHashMap<>();
        private final Map<String, BarOrderItemRecord> orderItems = new LinkedHashMap<>();
        private final Map<String, BarBillRecord> bills = new LinkedHashMap<>();
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
        public List<BarTableRecord> listTables() {
            return new ArrayList<>(tables.values());
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
        public List<BarReservationRecord> listReservations() {
            return new ArrayList<>(reservations.values());
        }

        @Override
        public Optional<BarReservationRecord> findReservationById(String id) {
            return Optional.ofNullable(reservations.get(id));
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
        public List<BarProductRecord> listProducts() {
            return new ArrayList<>(products.values());
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
        public List<BarOrderRecord> listOrders() {
            return new ArrayList<>(orders.values());
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
        public BarProductionTicketRecord saveProductionTicket(BarProductionTicketRecord ticket) {
            productionTickets.removeIf(existing -> existing.id().equals(ticket.id()));
            productionTickets.add(ticket);
            return ticket;
        }

        @Override
        public List<BarProductionTicketRecord> listProductionTickets() {
            return List.copyOf(productionTickets);
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
        public List<BarTableSessionRecord> listTableSessions() {
            return new ArrayList<>(tableSessions.values());
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
        public StockBalanceSnapshot saveStockBalance(StockBalanceSnapshot balance) {
            stockBalances.put(balance.itemId() + ":" + balance.locationId(), balance);
            return balance;
        }

        @Override
        public StockMovementSnapshot saveStockMovement(StockMovementSnapshot movement) {
            stockMovements.add(movement);
            return movement;
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
