package com.complex.platform.bar;

import com.complex.platform.finance.PaymentIntentRecord;
import java.util.List;
import java.util.Optional;

public interface BarRepository {
    BarTableRecord saveTable(BarTableRecord table);

    List<BarTableRecord> listTables(Optional<String> merchantScope);

    Optional<BarTableRecord> findTableById(String id);

    BarReservationRecord saveReservation(BarReservationRecord reservation);

    List<BarReservationRecord> listReservations(Optional<String> merchantScope);

    Optional<BarReservationRecord> findReservationById(String id);

    int countGuestCheckins(String reservationId);

    BarTableGuestCheckinRecord saveGuestCheckin(BarTableGuestCheckinRecord checkin);

    BarAgeVerificationRecord saveAgeVerification(BarAgeVerificationRecord verification);

    BarProductRecord saveProduct(BarProductRecord product);

    List<BarProductRecord> listProducts(Optional<String> merchantScope);

    Optional<BarProductRecord> findProductById(String id);

    BarOrderRecord saveOrder(BarOrderRecord order);

    List<BarOrderRecord> listOrders(Optional<String> merchantScope);

    Optional<BarOrderRecord> findOrderById(String id);

    BarOrderItemRecord saveOrderItem(BarOrderItemRecord item);

    List<BarOrderItemRecord> listOrderItems(String orderId);

    Optional<BarOrderItemRecord> findOrderItemById(String id);

    BarProductionTicketRecord saveProductionTicket(BarProductionTicketRecord ticket);

    List<BarProductionTicketRecord> listProductionTickets(Optional<String> merchantScope);

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

    List<BarTableSessionRecord> listTableSessions(Optional<String> merchantScope);

    Optional<BarTableSessionRecord> findTableSessionById(String id);

    BarTableSessionAdjustmentRecord saveTableSessionAdjustment(BarTableSessionAdjustmentRecord adjustment);

    List<BarTableSessionAdjustmentRecord> listTableSessionAdjustments(String sessionId);

    Optional<StockBalanceSnapshot> findStockBalance(String itemId, String locationId);

    boolean atomicDecreaseStock(String itemId, String locationId, int quantity);

    StockBalanceSnapshot saveStockBalance(StockBalanceSnapshot balance);

    StockMovementSnapshot saveStockMovement(StockMovementSnapshot movement);

    default PaymentIntentRecord savePaymentIntent(PaymentIntentRecord paymentIntent) {
        return paymentIntent;
    }

    /** A2/A3：账单结算流水（现金/POS 收款留痕 + channel）。默认 no-op/空，JdbcBarRepository 覆盖。 */
    default BarBillSettlementRecord saveBillSettlement(BarBillSettlementRecord settlement) {
        return settlement;
    }

    default List<BarBillSettlementRecord> listBillSettlements(String billId) {
        return List.of();
    }
}
