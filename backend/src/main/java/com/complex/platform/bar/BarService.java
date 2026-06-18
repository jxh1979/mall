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
    private final BarTableSessionService tableSessionService;
    private final BarOrderService orderService;
    private final BarEntryService entryService;
    private final BarAgeVerificationService ageVerificationService;
    private final AuthorizationService authorizationService;

    public BarService(
            BarRepository repository,
            BarOperationExecutor operations,
            BarBillingService billingService,
            BarTableSessionService tableSessionService,
            BarOrderService orderService,
            BarEntryService entryService,
            BarAgeVerificationService ageVerificationService,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.operations = operations;
        this.billingService = billingService;
        this.tableSessionService = tableSessionService;
        this.orderService = orderService;
        this.entryService = entryService;
        this.ageVerificationService = ageVerificationService;
        this.authorizationService = authorizationService;
    }

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
}
