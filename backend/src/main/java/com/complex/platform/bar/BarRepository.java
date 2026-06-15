package com.complex.platform.bar;

import com.complex.platform.finance.PaymentIntentRecord;
import java.util.List;
import java.util.Optional;

public interface BarRepository {
    BarTableRecord saveTable(BarTableRecord table);

    List<BarTableRecord> listTables();

    Optional<BarTableRecord> findTableById(String id);

    BarReservationRecord saveReservation(BarReservationRecord reservation);

    List<BarReservationRecord> listReservations();

    Optional<BarReservationRecord> findReservationById(String id);

    int countGuestCheckins(String reservationId);

    BarTableGuestCheckinRecord saveGuestCheckin(BarTableGuestCheckinRecord checkin);

    BarAgeVerificationRecord saveAgeVerification(BarAgeVerificationRecord verification);

    BarProductRecord saveProduct(BarProductRecord product);

    List<BarProductRecord> listProducts();

    Optional<BarProductRecord> findProductById(String id);

    BarOrderRecord saveOrder(BarOrderRecord order);

    List<BarOrderRecord> listOrders();

    Optional<BarOrderRecord> findOrderById(String id);

    BarOrderItemRecord saveOrderItem(BarOrderItemRecord item);

    List<BarOrderItemRecord> listOrderItems(String orderId);

    Optional<BarOrderItemRecord> findOrderItemById(String id);

    BarProductionTicketRecord saveProductionTicket(BarProductionTicketRecord ticket);

    List<BarProductionTicketRecord> listProductionTickets();

    Optional<BarProductionTicketRecord> findProductionTicketById(String id);

    Optional<BarProductionTicketRecord> findProductionTicketByOrderId(String orderId);

    BarProductionTicketEventRecord saveProductionTicketEvent(BarProductionTicketEventRecord event);

    List<BarProductionTicketEventRecord> listProductionTicketEvents(String ticketId);

    BarBillRecord saveBill(BarBillRecord bill);

    Optional<BarBillRecord> findBillById(String id);

    BarBillAdjustmentRecord saveBillAdjustment(BarBillAdjustmentRecord adjustment);

    List<BarBillAdjustmentRecord> listBillAdjustments(String billId);

    BarBillOperationRecord saveBillOperation(BarBillOperationRecord operation);

    List<BarBillOperationRecord> listBillOperations(String billId);

    BarCommissionCalculationRecord saveCommissionCalculation(BarCommissionCalculationRecord calculation);

    BarTableSessionRecord saveTableSession(BarTableSessionRecord session);

    List<BarTableSessionRecord> listTableSessions();

    Optional<BarTableSessionRecord> findTableSessionById(String id);

    BarTableSessionAdjustmentRecord saveTableSessionAdjustment(BarTableSessionAdjustmentRecord adjustment);

    List<BarTableSessionAdjustmentRecord> listTableSessionAdjustments(String sessionId);

    Optional<StockBalanceSnapshot> findStockBalance(String itemId, String locationId);

    StockBalanceSnapshot saveStockBalance(StockBalanceSnapshot balance);

    StockMovementSnapshot saveStockMovement(StockMovementSnapshot movement);

    default PaymentIntentRecord savePaymentIntent(PaymentIntentRecord paymentIntent) {
        return paymentIntent;
    }
}
