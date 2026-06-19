package com.complex.platform.bar;

import com.complex.platform.finance.PaymentIntentRecord;
import java.util.List;
import java.util.Optional;

public interface BarRepository {
    BarTableRecord saveTable(BarTableRecord table);

    List<BarTableRecord> listTables(Optional<String> merchantScope);

    Optional<BarTableRecord> findTableById(String id);

    /** R5 T15：按吧台解析一张台位用于赠饮 0 元单落 FK（无可靠台位来源时的兜底；无台位则跳过落单）。 */
    Optional<BarTableRecord> findFirstTableByBar(String barId);

    /** 处理清单 #8：设置 bar_tables.space_id（空间指派/回填），返回受影响行数。 */
    int assignTableSpace(String tableId, String spaceId);

    /** 处理清单 #8：解析空间所属商户（spaces.merchant_id），用于跨商户校验；空间不存在返回 empty。 */
    Optional<String> findSpaceMerchant(String spaceId);

    /** 处理清单 #8：列出 space_id 为空、待运营回填的卡座/桌台（按商户范围过滤）。 */
    List<BarTableSpaceBacklogRecord> listTablesWithoutSpace(Optional<String> merchantScope);

    BarReservationRecord saveReservation(BarReservationRecord reservation);

    List<BarReservationRecord> listReservations(Optional<String> merchantScope);

    Optional<BarReservationRecord> findReservationById(String id);

    /** R5 T16：保存订台定金规则。 */
    default BarDepositRuleRecord saveDepositRule(BarDepositRuleRecord rule) {
        return rule;
    }

    /** R5 T16：按吧台列启用中的定金规则（scope_type 倒序，table_type/time_slot 优先于 bar）。 */
    default List<BarDepositRuleRecord> listDepositRules(String barId) {
        return List.of();
    }

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

    /** V091 退菜退酒流水（区分 cancel/return + 库存回补/资金扣减留痕）。 */
    default BarOrderItemVoidRecord saveOrderItemVoid(BarOrderItemVoidRecord voidRecord) {
        return voidRecord;
    }

    default List<BarOrderItemVoidRecord> listOrderItemVoids(String orderId) {
        return List.of();
    }

    BarProductionTicketRecord saveProductionTicket(BarProductionTicketRecord ticket);

    List<BarProductionTicketRecord> listProductionTickets(Optional<String> merchantScope);

    Optional<BarProductionTicketRecord> findProductionTicketById(String id);

    Optional<BarProductionTicketRecord> findProductionTicketByOrderId(String orderId);

    BarProductionTicketEventRecord saveProductionTicketEvent(BarProductionTicketEventRecord event);

    List<BarProductionTicketEventRecord> listProductionTicketEvents(String ticketId);

    /** R5 T15：赠饮兑换桥 upsert（幂等键 verification_id；首次插入返回 true，已存在返回 false）。 */
    boolean insertGiftRedemptionIfAbsent(BarGiftRedemptionRecord redemption);

    BarGiftRedemptionRecord saveGiftRedemption(BarGiftRedemptionRecord redemption);

    Optional<BarGiftRedemptionRecord> findGiftRedemptionByVerification(String verificationId);

    List<BarGiftRedemptionRecord> listGiftRedemptions(Optional<String> merchantScope);

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

    /** V091 退酒回补库存（原子加回）。 */
    default boolean atomicIncreaseStock(String itemId, String locationId, int quantity) {
        return false;
    }

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
