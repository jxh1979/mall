package com.complex.platform.bar;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BarService {

    private final BarRepository repository;
    private final BarOperationExecutor operations;
    private final BarBillingService billingService;
    private final BarShiftService shiftService;
    private final BarTableSessionService tableSessionService;
    private final BarOrderService orderService;
    private final BarEntryService entryService;
    private final BarAgeVerificationService ageVerificationService;
    private final AuthorizationService authorizationService;

    public BarService(
            BarRepository repository,
            BarOperationExecutor operations,
            BarBillingService billingService,
            BarShiftService shiftService,
            BarTableSessionService tableSessionService,
            BarOrderService orderService,
            BarEntryService entryService,
            BarAgeVerificationService ageVerificationService,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.operations = operations;
        this.billingService = billingService;
        this.shiftService = shiftService;
        this.tableSessionService = tableSessionService;
        this.orderService = orderService;
        this.entryService = entryService;
        this.ageVerificationService = ageVerificationService;
        this.authorizationService = authorizationService;
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.entry.age_verify")
    @Transactional
    public BarAgeVerificationRecord createAgeVerification(VerificationRequest request) {
        return ageVerificationService.createAgeVerification(request);
    }

    public PageResponse<BarOrderRecord> listOrders() {
        return operations.readPage(
                BarReadOperation.LIST_ORDERS,
                () -> repository.listOrders(authorizationService.currentMerchantScope())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.order.create")
    @Transactional
    public BarOrderRecord createOrder(BarOrderRequest request) {
        return operations.executeWrite(
                "createBarOrder",
                request == null ? null : request.idempotencyKey(),
                request,
                BarOrderRecord.class,
                BarWriteOperation.CREATE_ORDER,
                () -> orderService.createOrder(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.order.create")
    @Transactional
    public BarBillRecord createOrderBill(String orderId, BarOrderBillCreateRequest request) {
        String id = operations.requireText(orderId, "order_id");
        return operations.executeWrite(
                "createBarOrderBill:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillRecord.class,
                BarWriteOperation.CREATE_ORDER_BILL,
                () -> billingService.createBill(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.order.create")
    @Transactional
    public BarOrderRecord deliverOrder(String orderId, BarOrderDeliveryRequest request) {
        String id = operations.requireText(orderId, "order_id");
        return operations.executeWrite(
                "deliverBarOrder:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarOrderRecord.class,
                BarWriteOperation.DELIVER_ORDER,
                () -> orderService.deliverOrder(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.order.create")
    @Transactional
    public BarOrderItemRecord addOrderItem(String orderId, BarOrderRequest request) {
        String id = operations.requireText(orderId, "order_id");
        return operations.executeWrite(
                "addBarOrderItem:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarOrderItemRecord.class,
                BarWriteOperation.ADD_ORDER_ITEM,
                () -> orderService.addOrderItem(id, request)
        );
    }

    public PageResponse<BarProductionTicketRecord> listProductionTickets() {
        return operations.readPage(
                BarReadOperation.LIST_PRODUCTION_TICKETS,
                () -> repository.listProductionTickets(authorizationService.currentMerchantScope())
        );
    }

    public PageResponse<BarProductionTicketEventRecord> listProductionTicketEvents(String ticketId) {
        String id = operations.requireText(ticketId, "ticket_id");
        return operations.readPage(
                BarReadOperation.LIST_PRODUCTION_TICKET_EVENTS,
                () -> repository.listProductionTicketEvents(id)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.production_ticket.manage")
    @Transactional
    public BarProductionTicketRecord transitionProductionTicket(String ticketId, BarProductionTicketTransitionRequest request) {
        String id = operations.requireText(ticketId, "ticket_id");
        return operations.executeWrite(
                "transitionBarProductionTicket:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarProductionTicketRecord.class,
                BarWriteOperation.TRANSITION_PRODUCTION_TICKET,
                () -> orderService.transitionProductionTicket(id, request)
        );
    }

    public PageResponse<BarProductRecord> listProducts() {
        return operations.readPage(
                BarReadOperation.LIST_PRODUCTS,
                () -> repository.listProducts(authorizationService.currentMerchantScope())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.order.create")
    @Transactional
    public BarProductRecord createProduct(BarProductRequest request) {
        return operations.executeWrite(
                "createBarProduct",
                request == null ? null : request.idempotencyKey(),
                request,
                BarProductRecord.class,
                BarWriteOperation.CREATE_PRODUCT,
                () -> orderService.createProduct(request)
        );
    }

    public PageResponse<BarReservationRecord> listReservations() {
        return operations.readPage(
                BarReadOperation.LIST_RESERVATIONS,
                () -> repository.listReservations(authorizationService.currentMerchantScope())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.reservation.create")
    @Transactional
    public BarReservationRecord createReservation(BarReservationRequest request) {
        return operations.executeWrite(
                "createBarReservation",
                request == null ? null : request.idempotencyKey(),
                request,
                BarReservationRecord.class,
                BarWriteOperation.CREATE_RESERVATION,
                () -> entryService.createReservation(request, null, "staff", "web")
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.reservation.manage")
    @Transactional
    public BarReservationRecord payReservationDeposit(String reservationId, BarReservationDepositActionRequest request) {
        String id = operations.requireText(reservationId, "reservation_id");
        return operations.executeWrite(
                "payBarReservationDeposit:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarReservationRecord.class,
                BarWriteOperation.PAY_RESERVATION_DEPOSIT,
                () -> entryService.payDeposit(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.reservation.manage")
    @Transactional
    public BarReservationRecord checkInReservationDeposit(String reservationId, BarReservationDepositActionRequest request) {
        String id = operations.requireText(reservationId, "reservation_id");
        return operations.executeWrite(
                "checkInBarReservationDeposit:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarReservationRecord.class,
                BarWriteOperation.CHECK_IN_RESERVATION_DEPOSIT,
                () -> entryService.checkInDeposit(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.reservation.manage")
    @Transactional
    public BarReservationRecord cancelReservation(String reservationId, BarReservationCancelRequest request) {
        String id = operations.requireText(reservationId, "reservation_id");
        return operations.executeWrite(
                "cancelBarReservation:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarReservationRecord.class,
                BarWriteOperation.CANCEL_RESERVATION,
                () -> entryService.cancelReservation(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.deposit_rule.manage")
    @Transactional
    public BarDepositRuleRecord createDepositRule(BarDepositRuleRequest request) {
        return operations.executeWrite(
                "createBarDepositRule",
                request == null ? null : request.idempotencyKey(),
                request,
                BarDepositRuleRecord.class,
                BarWriteOperation.CREATE_DEPOSIT_RULE,
                () -> entryService.createDepositRule(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table.guest_checkin")
    @Transactional
    public BarReservationEntryAllowanceVerificationRecord verifyReservationEntryAllowance(String reservationId, VerificationRequest request) {
        String id = operations.requireText(reservationId, "reservation_id");
        return operations.executeWrite(
                "verifyBarReservationEntryAllowance:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarReservationEntryAllowanceVerificationRecord.class,
                BarWriteOperation.VERIFY_RESERVATION_ENTRY_ALLOWANCE,
                () -> entryService.verifyGuestEntryAllowance(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table.guest_checkin")
    @Transactional
    public BarTableGuestCheckinRecord checkInReservationGuest(String reservationId, VerificationRequest request) {
        String id = operations.requireText(reservationId, "reservation_id");
        return operations.executeWrite(
                "checkInBarReservationGuest:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableGuestCheckinRecord.class,
                BarWriteOperation.CHECK_IN_RESERVATION_GUEST,
                () -> entryService.checkInGuest(id, request)
        );
    }

    public PageResponse<BarTableRecord> listTables() {
        return operations.readPage(
                BarReadOperation.LIST_TABLES,
                () -> repository.listTables(authorizationService.currentMerchantScope())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table.manage")
    @Transactional
    public BarTableRecord createTable(BarTableRequest request) {
        return operations.executeWrite(
                "createBarTable",
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableRecord.class,
                BarWriteOperation.CREATE_TABLE,
                () -> tableSessionService.createTable(request)
        );
    }

    public PageResponse<BarTableSpaceBacklogRecord> listTablesWithoutSpace() {
        return operations.readPage(
                BarReadOperation.LIST_TABLES_WITHOUT_SPACE,
                tableSessionService::listTablesWithoutSpace
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table.manage")
    @Transactional
    public BarTableSpaceAssignmentRecord assignTableSpace(String tableId, BarTableSpaceAssignmentRequest request) {
        String id = operations.requireText(tableId, "table_id");
        return operations.executeWrite(
                "assignBarTableSpace:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableSpaceAssignmentRecord.class,
                BarWriteOperation.ASSIGN_TABLE_SPACE,
                () -> tableSessionService.assignTableSpace(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table.open_close")
    @Transactional
    public BarTableRecord openTable(String tableId, OpenBarTableRequest request) {
        String id = operations.requireText(tableId, "table_id");
        return operations.executeWrite(
                "openBarTable:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableRecord.class,
                BarWriteOperation.OPEN_TABLE,
                () -> tableSessionService.openTable(id, request)
        );
    }

    public PageResponse<BarTableSessionRecord> listTableSessions() {
        return operations.readPage(
                BarReadOperation.LIST_TABLE_SESSIONS,
                () -> repository.listTableSessions(authorizationService.currentMerchantScope())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table_session.create")
    @Transactional
    public BarTableSessionRecord createTableSession(BarTableSessionRequest request) {
        return operations.executeWrite(
                "createBarTableSession",
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableSessionRecord.class,
                BarWriteOperation.CREATE_TABLE_SESSION,
                () -> tableSessionService.createTableSession(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table_session.transfer")
    @Transactional
    public BarTableSessionAdjustmentRecord transferTableSession(String sessionId, BarTableTransferRequest request) {
        String id = operations.requireText(sessionId, "session_id");
        return operations.executeWrite(
                "transferBarTableSession:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableSessionAdjustmentRecord.class,
                BarWriteOperation.TRANSFER_TABLE_SESSION,
                () -> tableSessionService.transferTableSession(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table_session.merge")
    @Transactional
    public BarTableSessionAdjustmentRecord mergeTableSessions(String sessionId, BarTableMergeRequest request) {
        String id = operations.requireText(sessionId, "session_id");
        return operations.executeWrite(
                "mergeBarTableSession:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableSessionAdjustmentRecord.class,
                BarWriteOperation.MERGE_TABLE_SESSIONS,
                () -> tableSessionService.mergeTableSessions(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table_session.renew")
    @Transactional
    public BarTableSessionAdjustmentRecord renewTableSession(String sessionId, BarTableRenewRequest request) {
        String id = operations.requireText(sessionId, "session_id");
        return operations.executeWrite(
                "renewBarTableSession:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableSessionAdjustmentRecord.class,
                BarWriteOperation.RENEW_TABLE_SESSION,
                () -> tableSessionService.renewTableSession(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.payment_intent.create")
    @Transactional
    public PaymentIntentRecord createBillPayIntent(String billId, PaymentIntentRequest request) {
        String id = operations.requireText(billId, "bill_id");
        return operations.executeWrite(
                "createBarBillPayIntent:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                PaymentIntentRecord.class,
                BarWriteOperation.CREATE_BILL_PAY_INTENT,
                () -> billingService.createBillPayIntent(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.payment.register")
    @Transactional
    public BarBillRecord settleBill(String billId, BarBillSettleRequest request) {
        String id = operations.requireText(billId, "bill_id");
        return operations.executeWrite(
                "settleBarBill:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillRecord.class,
                BarWriteOperation.SETTLE_BILL,
                () -> billingService.settleBill(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.bill.adjust", admissionAction = "bar.bill_waiver", objectType = "bar_bill")
    @Transactional
    public BarBillAdjustmentRecord applyBillAdjustment(String billId, BarBillAdjustmentRequest request) {
        String id = operations.requireText(billId, "bill_id");
        return operations.executeWrite(
                "adjustBarBill:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillAdjustmentRecord.class,
                BarWriteOperation.APPLY_BILL_ADJUSTMENT,
                () -> billingService.applyBillAdjustment(id, request)
        );
    }

    public PageResponse<BarBillOperationRecord> listBillOperations(String billId) {
        String id = operations.requireText(billId, "bill_id");
        return operations.readPage(
                BarReadOperation.LIST_BILL_OPERATIONS,
                () -> repository.listBillOperations(id)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.bill.split_merge")
    @Transactional
    public BarBillOperationRecord splitBill(String billId, BarBillSplitRequest request) {
        String id = operations.requireText(billId, "bill_id");
        return operations.executeWrite(
                "splitBarBill:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillOperationRecord.class,
                BarWriteOperation.SPLIT_BILL,
                () -> billingService.splitBill(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.bill.split_merge")
    @Transactional
    public BarBillOperationRecord mergeBills(String billId, BarBillMergeRequest request) {
        String id = operations.requireText(billId, "bill_id");
        return operations.executeWrite(
                "mergeBarBill:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillOperationRecord.class,
                BarWriteOperation.MERGE_BILLS,
                () -> billingService.mergeBills(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.order_item.void", admissionAction = "bar.order_item_void", objectType = "bar_order_item")
    @Transactional
    public BarOrderItemVoidRecord voidOrderItem(String orderId, BarOrderItemVoidRequest request) {
        String id = operations.requireText(orderId, "order_id");
        return operations.executeWrite(
                "voidBarOrderItem:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarOrderItemVoidRecord.class,
                BarWriteOperation.VOID_ORDER_ITEM,
                () -> orderService.voidOrderItem(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(permission = "bar.order_item.void_decide")
    @Transactional
    public BarOrderItemVoidRecord decideOrderItemVoid(String voidId, BarOrderItemVoidDecisionRequest request) {
        String id = operations.requireText(voidId, "void_id");
        return operations.executeWrite(
                "decideBarOrderItemVoid:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarOrderItemVoidRecord.class,
                BarWriteOperation.DECIDE_ORDER_ITEM_VOID,
                () -> orderService.decideOrderItemVoid(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.bill.reopen", admissionAction = "bar.bill_reopen", objectType = "bar_bill")
    @Transactional
    public BarBillOperationRecord reopenBill(String billId, BarBillReopenRequest request) {
        String id = operations.requireText(billId, "bill_id");
        return operations.executeWrite(
                "reopenBarBill:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillOperationRecord.class,
                BarWriteOperation.REOPEN_BILL,
                () -> billingService.reopenBill(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.product.sold_out")
    @Transactional
    public BarProductRecord markProductSoldOut(String productId, BarProductSoldOutRequest request) {
        String id = operations.requireText(productId, "product_id");
        return operations.executeWrite(
                "markBarProductSoldOut:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarProductRecord.class,
                BarWriteOperation.MARK_PRODUCT_SOLD_OUT,
                () -> orderService.markSoldOut(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.bill.tip")
    @Transactional
    public BarBillRecord addBillTip(String billId, BarBillTipRequest request) {
        String id = operations.requireText(billId, "bill_id");
        return operations.executeWrite(
                "addBarBillTip:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillRecord.class,
                BarWriteOperation.ADD_BILL_TIP,
                () -> billingService.addTip(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table_commission.calculate")
    @Transactional
    public BarCommissionCalculationRecord calculateCommission(String billId, CommissionCalculateRequest request) {
        String id = operations.requireText(billId, "bill_id");
        return operations.executeWrite(
                "calculateBarTableCommission:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarCommissionCalculationRecord.class,
                BarWriteOperation.CALCULATE_COMMISSION,
                () -> billingService.calculateCommission(id, request)
        );
    }

    // ── V095 交班 / 班结对账 ──────────────────────────────────────────────────
    public PageResponse<BarShiftRecord> listShifts() {
        return operations.readPage(
                BarReadOperation.LIST_SHIFTS,
                () -> repository.listShifts(authorizationService.currentMerchantScope())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.shift.manage")
    @Transactional
    public BarShiftRecord openShift(BarShiftOpenRequest request) {
        return operations.executeWrite(
                "openBarShift",
                request == null ? null : request.idempotencyKey(),
                request,
                BarShiftRecord.class,
                BarWriteOperation.OPEN_SHIFT,
                () -> shiftService.openShift(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.shift.close", admissionAction = "bar.shift_close", objectType = "bar_shift")
    @Transactional
    public BarShiftRecord closeShift(String shiftId, BarShiftCloseRequest request) {
        String id = operations.requireText(shiftId, "shift_id");
        return operations.executeWrite(
                "closeBarShift:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarShiftRecord.class,
                BarWriteOperation.CLOSE_SHIFT,
                () -> shiftService.closeShift(id, request)
        );
    }
}
