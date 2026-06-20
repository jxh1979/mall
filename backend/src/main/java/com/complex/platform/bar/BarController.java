package com.complex.platform.bar;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bar")
public class BarController {

    private final BarService service;
    private final AuthorizationService authorizationService;

    public BarController(BarService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/age-verifications")
    public ApiResponse<BarAgeVerificationRecord> createAgeVerification(
            @RequestBody(required = false) VerificationRequest request
    ) {
        authorizationService.requirePermission("bar.entry.age_verify");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createAgeVerification(request));
    }

    @PostMapping("/bills/{bill_id}/commissions/calculate")
    public ApiResponse<BarCommissionCalculationRecord> calculateCommission(
            @PathVariable("bill_id") String billId,
            @RequestBody(required = false) CommissionCalculateRequest request
    ) {
        authorizationService.requirePermission("bar.table_commission.calculate");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.calculateCommission(billId, request));
    }

    @PostMapping("/bills/{bill_id}/pay-intents")
    public ApiResponse<PaymentIntentRecord> createBillPayIntent(
            @PathVariable("bill_id") String billId,
            @RequestBody(required = false) PaymentIntentRequest request
    ) {
        authorizationService.requirePermission("finance.payment_intent.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createBillPayIntent(billId, request));
    }

    @PostMapping("/bills/{bill_id}/settle")
    public ApiResponse<BarBillRecord> settleBill(
            @PathVariable("bill_id") String billId,
            @RequestBody(required = false) BarBillSettleRequest request
    ) {
        authorizationService.requirePermission("finance.payment.register");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.settleBill(billId, request));
    }

    @PostMapping("/bills/{bill_id}/adjustments")
    public ApiResponse<BarBillAdjustmentRecord> applyBillAdjustment(
            @PathVariable("bill_id") String billId,
            @RequestBody(required = false) BarBillAdjustmentRequest request
    ) {
        authorizationService.requirePermission("bar.bill.adjust");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.applyBillAdjustment(billId, request));
    }

    @GetMapping("/bills/{bill_id}/operations")
    public ApiResponse<PageResponse<BarBillOperationRecord>> listBillOperations(@PathVariable("bill_id") String billId) {
        authorizationService.requirePermission("bar.bill.split_merge");
        return ApiResponse.ok(service.listBillOperations(billId));
    }

    @PostMapping("/bills/{bill_id}/split")
    public ApiResponse<BarBillOperationRecord> splitBill(
            @PathVariable("bill_id") String billId,
            @RequestBody(required = false) BarBillSplitRequest request
    ) {
        authorizationService.requirePermission("bar.bill.split_merge");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.splitBill(billId, request));
    }

    @PostMapping("/bills/{bill_id}/merge")
    public ApiResponse<BarBillOperationRecord> mergeBills(
            @PathVariable("bill_id") String billId,
            @RequestBody(required = false) BarBillMergeRequest request
    ) {
        authorizationService.requirePermission("bar.bill.split_merge");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.mergeBills(billId, request));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResponse<BarOrderRecord>> listOrders() {
        authorizationService.requirePermission("bar.order.view");
        return ApiResponse.ok(service.listOrders());
    }

    @PostMapping("/orders")
    public ApiResponse<BarOrderRecord> createOrder(@RequestBody(required = false) BarOrderRequest request) {
        authorizationService.requirePermission("bar.order.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createOrder(request));
    }

    @PostMapping("/orders/{order_id}/bill")
    public ApiResponse<BarBillRecord> createOrderBill(
            @PathVariable("order_id") String orderId,
            @RequestBody(required = false) BarOrderBillCreateRequest request
    ) {
        authorizationService.requirePermission("bar.order.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createOrderBill(orderId, request));
    }

    @PostMapping("/orders/{order_id}/deliver")
    public ApiResponse<BarOrderRecord> deliverOrder(
            @PathVariable("order_id") String orderId,
            @RequestBody(required = false) BarOrderDeliveryRequest request
    ) {
        authorizationService.requirePermission("bar.order.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.deliverOrder(orderId, request));
    }

    @PostMapping("/orders/{order_id}/void-item")
    public ApiResponse<BarOrderItemVoidRecord> voidOrderItem(
            @PathVariable("order_id") String orderId,
            @RequestBody(required = false) BarOrderItemVoidRequest request
    ) {
        authorizationService.requirePermission("bar.order_item.void");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.voidOrderItem(orderId, request));
    }

    @PostMapping("/order-item-voids/{void_id}/decision")
    public ApiResponse<BarOrderItemVoidRecord> decideOrderItemVoid(
            @PathVariable("void_id") String voidId,
            @RequestBody(required = false) BarOrderItemVoidDecisionRequest request
    ) {
        // V125 P1-2：审批分离——决策端点用独立审批权限点，与申请权限 bar.order_item.void 区分，杜绝自审批。
        authorizationService.requirePermission("bar.order_item.void_decide");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.decideOrderItemVoid(voidId, request));
    }

    @PostMapping("/bills/{bill_id}/reopen")
    public ApiResponse<BarBillOperationRecord> reopenBill(
            @PathVariable("bill_id") String billId,
            @RequestBody(required = false) BarBillReopenRequest request
    ) {
        authorizationService.requirePermission("bar.bill.reopen");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reopenBill(billId, request));
    }

    @PostMapping("/bills/{bill_id}/tip")
    public ApiResponse<BarBillRecord> addBillTip(
            @PathVariable("bill_id") String billId,
            @RequestBody(required = false) BarBillTipRequest request
    ) {
        authorizationService.requirePermission("bar.bill.tip");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.addBillTip(billId, request));
    }

    @GetMapping("/shifts")
    public ApiResponse<PageResponse<BarShiftRecord>> listShifts() {
        authorizationService.requirePermission("bar.shift.manage");
        return ApiResponse.ok(service.listShifts());
    }

    @PostMapping("/shifts/open")
    public ApiResponse<BarShiftRecord> openShift(@RequestBody(required = false) BarShiftOpenRequest request) {
        authorizationService.requirePermission("bar.shift.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.openShift(request));
    }

    @PostMapping("/shifts/{shift_id}/close")
    public ApiResponse<BarShiftRecord> closeShift(
            @PathVariable("shift_id") String shiftId,
            @RequestBody(required = false) BarShiftCloseRequest request
    ) {
        authorizationService.requirePermission("bar.shift.close");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.closeShift(shiftId, request));
    }

    @PostMapping("/products/{product_id}/sold-out")
    public ApiResponse<BarProductRecord> markProductSoldOut(
            @PathVariable("product_id") String productId,
            @RequestBody(required = false) BarProductSoldOutRequest request
    ) {
        authorizationService.requirePermission("bar.product.sold_out");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.markProductSoldOut(productId, request));
    }

    @PostMapping("/orders/{order_id}/items")
    public ApiResponse<BarOrderItemRecord> addOrderItem(
            @PathVariable("order_id") String orderId,
            @RequestBody(required = false) BarOrderRequest request
    ) {
        authorizationService.requirePermission("bar.order.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.addOrderItem(orderId, request));
    }

    @GetMapping("/production-tickets")
    public ApiResponse<PageResponse<BarProductionTicketRecord>> listProductionTickets() {
        authorizationService.requirePermission("bar.production_ticket.view");
        return ApiResponse.ok(service.listProductionTickets());
    }

    @GetMapping("/production-tickets/{ticket_id}/events")
    public ApiResponse<PageResponse<BarProductionTicketEventRecord>> listProductionTicketEvents(
            @PathVariable("ticket_id") String ticketId
    ) {
        authorizationService.requirePermission("bar.production_ticket.view");
        return ApiResponse.ok(service.listProductionTicketEvents(ticketId));
    }

    @PostMapping("/production-tickets/{ticket_id}/transition")
    public ApiResponse<BarProductionTicketRecord> transitionProductionTicket(
            @PathVariable("ticket_id") String ticketId,
            @RequestBody(required = false) BarProductionTicketTransitionRequest request
    ) {
        authorizationService.requirePermission("bar.production_ticket.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.transitionProductionTicket(ticketId, request));
    }

    @GetMapping("/products")
    public ApiResponse<PageResponse<BarProductRecord>> listProducts() {
        authorizationService.requirePermission("bar.order.create");
        return ApiResponse.ok(service.listProducts());
    }

    @PostMapping("/products")
    public ApiResponse<BarProductRecord> createProduct(@RequestBody(required = false) BarProductRequest request) {
        authorizationService.requirePermission("bar.order.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createProduct(request));
    }

    @GetMapping("/reservations")
    public ApiResponse<PageResponse<BarReservationRecord>> listReservations() {
        authorizationService.requirePermission("bar.reservation.view");
        return ApiResponse.ok(service.listReservations());
    }

    @PostMapping("/reservations")
    public ApiResponse<BarReservationRecord> createReservation(@RequestBody(required = false) BarReservationRequest request) {
        authorizationService.requirePermission("bar.reservation.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createReservation(request));
    }

    @PostMapping("/reservations/{id}/pay-deposit")
    public ApiResponse<BarReservationRecord> payReservationDeposit(
            @PathVariable String id,
            @RequestBody(required = false) BarReservationDepositActionRequest request
    ) {
        authorizationService.requirePermission("bar.reservation.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.payReservationDeposit(id, request));
    }

    @PostMapping("/reservations/{id}/checkin")
    public ApiResponse<BarReservationRecord> checkInReservationDeposit(
            @PathVariable String id,
            @RequestBody(required = false) BarReservationDepositActionRequest request
    ) {
        authorizationService.requirePermission("bar.reservation.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.checkInReservationDeposit(id, request));
    }

    @PostMapping("/reservations/{id}/cancel")
    public ApiResponse<BarReservationRecord> cancelReservation(
            @PathVariable String id,
            @RequestBody(required = false) BarReservationCancelRequest request
    ) {
        authorizationService.requirePermission("bar.reservation.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.cancelReservation(id, request));
    }

    @PostMapping("/deposit-rules")
    public ApiResponse<BarDepositRuleRecord> createDepositRule(
            @RequestBody(required = false) BarDepositRuleRequest request
    ) {
        authorizationService.requirePermission("bar.deposit_rule.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createDepositRule(request));
    }

    @PostMapping("/reservations/{id}/check-in-guest")
    public ApiResponse<BarTableGuestCheckinRecord> checkInReservationGuest(
            @PathVariable String id,
            @RequestBody(required = false) VerificationRequest request
    ) {
        authorizationService.requirePermission("bar.table.guest_checkin");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.checkInReservationGuest(id, request));
    }

    @GetMapping("/tables")
    public ApiResponse<PageResponse<BarTableRecord>> listTables() {
        authorizationService.requirePermission("bar.table.view");
        return ApiResponse.ok(service.listTables());
    }

    @PostMapping("/tables")
    public ApiResponse<BarTableRecord> createTable(@RequestBody(required = false) BarTableRequest request) {
        authorizationService.requirePermission("bar.table.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createTable(request));
    }

    @GetMapping("/tables/without-space")
    public ApiResponse<PageResponse<BarTableSpaceBacklogRecord>> listTablesWithoutSpace() {
        authorizationService.requirePermission("bar.table.manage");
        return ApiResponse.ok(service.listTablesWithoutSpace());
    }

    @PutMapping("/tables/{table_id}/space")
    public ApiResponse<BarTableSpaceAssignmentRecord> assignTableSpace(
            @PathVariable("table_id") String tableId,
            @RequestBody(required = false) BarTableSpaceAssignmentRequest request
    ) {
        authorizationService.requirePermission("bar.table.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.assignTableSpace(tableId, request));
    }

    @PostMapping("/tables/{table_id}/open")
    public ApiResponse<BarTableRecord> openTable(
            @PathVariable("table_id") String tableId,
            @RequestBody(required = false) OpenBarTableRequest request
    ) {
        authorizationService.requirePermission("bar.table.open_close");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.openTable(tableId, request));
    }

    @GetMapping("/table-sessions")
    public ApiResponse<PageResponse<BarTableSessionRecord>> listTableSessions() {
        authorizationService.requirePermission("bar.table_session.view");
        return ApiResponse.ok(service.listTableSessions());
    }

    @PostMapping("/table-sessions")
    public ApiResponse<BarTableSessionRecord> createTableSession(@RequestBody(required = false) BarTableSessionRequest request) {
        authorizationService.requirePermission("bar.table_session.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createTableSession(request));
    }

    @PostMapping("/table-sessions/{session_id}/transfer")
    public ApiResponse<BarTableSessionAdjustmentRecord> transferTableSession(
            @PathVariable("session_id") String sessionId,
            @RequestBody(required = false) BarTableTransferRequest request
    ) {
        authorizationService.requirePermission("bar.table_session.transfer");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.transferTableSession(sessionId, request));
    }

    @PostMapping("/table-sessions/{session_id}/merge")
    public ApiResponse<BarTableSessionAdjustmentRecord> mergeTableSessions(
            @PathVariable("session_id") String sessionId,
            @RequestBody(required = false) BarTableMergeRequest request
    ) {
        authorizationService.requirePermission("bar.table_session.merge");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.mergeTableSessions(sessionId, request));
    }

    @PostMapping("/table-sessions/{session_id}/renew")
    public ApiResponse<BarTableSessionAdjustmentRecord> renewTableSession(
            @PathVariable("session_id") String sessionId,
            @RequestBody(required = false) BarTableRenewRequest request
    ) {
        authorizationService.requirePermission("bar.table_session.renew");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.renewTableSession(sessionId, request));
    }
}
