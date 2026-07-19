package com.complex.platform.bar;

import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.inventory.InventoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
class JdbcBarRepository implements BarRepository {

    private final JdbcBarCatalogRepository catalog;
    private final JdbcBarOrderRepository orderRepository;
    private final JdbcBarBillingRepository billing;
    private final JdbcBarSessionRepository sessionRepository;
    private final JdbcBarInventoryPaymentRepository inventoryPayment;

    // C-1(V296)：注入库存仓储 + 事务管理器——酒吧域流水写入改道库存域成本管道（见
    // JdbcBarInventoryPaymentRepository.saveStockMovement），不再裸 INSERT inventory_stock_movements。
    JdbcBarRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            InventoryRepository inventoryRepository,
            PlatformTransactionManager transactionManager
    ) {
        var jdbcObjectMapper = objectMapper.copy().findAndRegisterModules();
        var rowMapper = new JdbcBarRowMapper(jdbcObjectMapper);
        var parameterSource = new JdbcBarParameterSource(jdbcObjectMapper);
        this.catalog = new JdbcBarCatalogRepository(jdbcTemplate, rowMapper, parameterSource);
        this.orderRepository = new JdbcBarOrderRepository(jdbcTemplate, rowMapper, parameterSource);
        this.billing = new JdbcBarBillingRepository(jdbcTemplate, rowMapper, parameterSource);
        this.sessionRepository = new JdbcBarSessionRepository(jdbcTemplate, rowMapper, parameterSource);
        this.inventoryPayment = new JdbcBarInventoryPaymentRepository(
                jdbcTemplate, rowMapper, parameterSource, inventoryRepository,
                new TransactionTemplate(transactionManager));
    }
    @Override
    public BarTableRecord saveTable(BarTableRecord table) {
        return catalog.saveTable(table);
    }

    @Override
    public java.util.Set<String> listActiveReservedTableIds(java.time.Instant since) {
        return catalog.listActiveReservedTableIds(since);
    }

    @Override
    public List<BarTableRecord> listTables(Optional<String> merchantScope) {
        return catalog.listTables(merchantScope);
    }

    @Override
    public Optional<BarTableRecord> findFirstTableByBar(String barId) {
        return catalog.findFirstTableByBar(barId);
    }

    @Override
    public Optional<BarTableRecord> findTableById(String id) {
        return catalog.findTableById(id);
    }

    @Override
    public int assignTableSpace(String tableId, String spaceId) {
        return catalog.assignTableSpace(tableId, spaceId);
    }

    @Override
    public Optional<String> findSpaceMerchant(String spaceId) {
        return catalog.findSpaceMerchant(spaceId);
    }

    @Override
    public List<BarTableSpaceBacklogRecord> listTablesWithoutSpace(Optional<String> merchantScope) {
        return catalog.listTablesWithoutSpace(merchantScope);
    }

    @Override
    public BarReservationRecord saveReservation(BarReservationRecord reservation) {
        return catalog.saveReservation(reservation);
    }

    @Override
    public List<BarReservationRecord> listReservations(Optional<String> merchantScope) {
        return catalog.listReservations(merchantScope);
    }

    @Override
    public List<BarReservationRecord> listReservations(Optional<String> merchantScope, boolean includeArchived) {
        return catalog.listReservations(merchantScope, includeArchived);
    }

    @Override
    public long countReservationReferences(String id) {
        return catalog.countReservationReferences(id);
    }

    @Override
    public void deleteReservation(String id) {
        catalog.deleteReservation(id);
    }

    @Override
    public Optional<BarReservationRecord> findReservationById(String id) {
        return catalog.findReservationById(id);
    }

    @Override
    public Optional<BarReservationRecord> findReservationByIdForUpdate(String id) {
        return catalog.findReservationByIdForUpdate(id);
    }

    @Override
    public BarDepositRuleRecord saveDepositRule(BarDepositRuleRecord rule) {
        return catalog.saveDepositRule(rule);
    }

    @Override
    public List<BarDepositRuleRecord> listDepositRules(String barId) {
        return catalog.listDepositRules(barId);
    }

    @Override
    public int countGuestCheckins(String reservationId) {
        return catalog.countGuestCheckins(reservationId);
    }

    @Override
    public BarTableGuestCheckinRecord saveGuestCheckin(BarTableGuestCheckinRecord checkin) {
        return catalog.saveGuestCheckin(checkin);
    }

    @Override
    public Optional<BarReservationRecord> findReservationByInvitationToken(String token) {
        return catalog.findReservationByInvitationToken(token);
    }

    @Override
    public BarReservationInvitationMemberRecord saveInvitationMember(BarReservationInvitationMemberRecord member) {
        return catalog.saveInvitationMember(member);
    }

    @Override
    public List<BarReservationInvitationMemberRecord> listInvitationMembers(String reservationId) {
        return catalog.listInvitationMembers(reservationId);
    }

    @Override
    public BarProductRecord saveProduct(BarProductRecord product) {
        return catalog.saveProduct(product);
    }

    @Override
    public List<BarProductRecord> listProducts(Optional<String> merchantScope) {
        return catalog.listProducts(merchantScope);
    }

    @Override
    public Optional<BarProductRecord> findProductById(String id) {
        return catalog.findProductById(id);
    }

    @Override
    public Optional<BarProductRecord> findProductByInventoryItem(String barId, String inventoryItemId) {
        return catalog.findProductByInventoryItem(barId, inventoryItemId);
    }

    @Override
    public BarOrderRecord saveOrder(BarOrderRecord order) {
        return orderRepository.saveOrder(order);
    }

    @Override
    public List<BarOrderRecord> listOrders(Optional<String> merchantScope) {
        return orderRepository.listOrders(merchantScope);
    }

    @Override
    public Optional<BarOrderRecord> findOrderById(String id) {
        return orderRepository.findOrderById(id);
    }

    @Override
    public BarOrderItemRecord saveOrderItem(BarOrderItemRecord item) {
        return orderRepository.saveOrderItem(item);
    }

    @Override
    public List<BarOrderItemRecord> listOrderItems(String orderId) {
        return orderRepository.listOrderItems(orderId);
    }

    @Override
    public Optional<BarOrderItemRecord> findOrderItemById(String id) {
        return orderRepository.findOrderItemById(id);
    }

    @Override
    public BarOrderItemVoidRecord saveOrderItemVoid(BarOrderItemVoidRecord voidRecord) {
        return orderRepository.saveOrderItemVoid(voidRecord);
    }

    @Override
    public List<BarOrderItemVoidRecord> listOrderItemVoids(String orderId) {
        return orderRepository.listOrderItemVoids(orderId);
    }

    // 修复预存接线缺口：facade 此前未委派，导致生产环境 BarRepository 默认返回 Optional.empty()，
    // 审批决策(decideOrderItemVoid)实际查不到 void 流水。改为委派 JDBC 实现，并让 ForUpdate 行锁真正生效。
    @Override
    public Optional<BarOrderItemVoidRecord> findOrderItemVoidById(String id) {
        return orderRepository.findOrderItemVoidById(id);
    }

    @Override
    public Optional<BarOrderItemVoidRecord> findOrderItemVoidByIdForUpdate(String id) {
        return orderRepository.findOrderItemVoidByIdForUpdate(id);
    }

    @Override
    public BarProductionTicketRecord saveProductionTicket(BarProductionTicketRecord ticket) {
        return orderRepository.saveProductionTicket(ticket);
    }

    @Override
    public List<BarProductionTicketRecord> listProductionTickets(Optional<String> merchantScope) {
        return orderRepository.listProductionTickets(merchantScope);
    }

    @Override
    public Optional<BarProductionTicketRecord> findProductionTicketById(String id) {
        return orderRepository.findProductionTicketById(id);
    }

    @Override
    public Optional<BarProductionTicketRecord> findProductionTicketByOrderId(String orderId) {
        return orderRepository.findProductionTicketByOrderId(orderId);
    }

    @Override
    public BarProductionTicketEventRecord saveProductionTicketEvent(BarProductionTicketEventRecord event) {
        return orderRepository.saveProductionTicketEvent(event);
    }

    @Override
    public List<BarProductionTicketEventRecord> listProductionTicketEvents(String ticketId) {
        return orderRepository.listProductionTicketEvents(ticketId);
    }

    @Override
    public boolean insertGiftRedemptionIfAbsent(BarGiftRedemptionRecord redemption) {
        return orderRepository.insertGiftRedemptionIfAbsent(redemption);
    }

    @Override
    public BarGiftRedemptionRecord saveGiftRedemption(BarGiftRedemptionRecord redemption) {
        return orderRepository.saveGiftRedemption(redemption);
    }

    @Override
    public Optional<BarGiftRedemptionRecord> findGiftRedemptionByVerification(String verificationId) {
        return orderRepository.findGiftRedemptionByVerification(verificationId);
    }

    @Override
    public List<BarGiftRedemptionRecord> listGiftRedemptions(Optional<String> merchantScope) {
        return orderRepository.listGiftRedemptions(merchantScope);
    }

    @Override
    public BarBillRecord saveBill(BarBillRecord bill) {
        return billing.saveBill(bill);
    }

    @Override
    public BarBillSettlementRecord saveBillSettlement(BarBillSettlementRecord settlement) {
        return billing.saveBillSettlement(settlement);
    }

    @Override
    public java.util.List<BarBillSettlementRecord> listBillSettlements(String billId) {
        return billing.listBillSettlements(billId);
    }

    @Override
    public BarShiftRecord saveShift(BarShiftRecord shift) {
        return billing.saveShift(shift);
    }

    @Override
    public Optional<BarShiftRecord> findShiftById(String id) {
        return billing.findShiftById(id);
    }

    @Override
    public Optional<BarShiftRecord> findOpenShift(String barId, String cashierUserId) {
        return billing.findOpenShift(barId, cashierUserId);
    }

    @Override
    public List<BarShiftRecord> listShifts(Optional<String> merchantScope) {
        return billing.listShifts(merchantScope);
    }

    // P2 多综合体 project_id scoped 读通道：委派至 billing，按真实 project_id 列行级隔离（AC-ISO-1，§4）。
    public List<BarShiftRecord> listShiftsByProject(String projectId) {
        return billing.listShiftsByProject(projectId);
    }

    @Override
    public BarShiftReconciliationSnapshot aggregateShiftReconciliation(
            String barId,
            String cashierUserId,
            java.time.Instant from,
            java.time.Instant to,
            Optional<String> merchantScope
    ) {
        return billing.aggregateShiftReconciliation(barId, cashierUserId, from, to, merchantScope);
    }

    @Override
    public List<BarBillRecord> listBillsByOrderIds(List<String> orderIds) {
        return billing.listBillsByOrderIds(orderIds);
    }

    @Override
    public Optional<BarBillRecord> findBillById(String id) {
        return billing.findBillById(id);
    }

    @Override
    public Optional<BarBillRecord> findBillByIdForUpdate(String id) {
        return billing.findBillByIdForUpdate(id);
    }

    @Override
    public Optional<BarCommissionCalculationRecord> findCommissionByBillId(String billId) {
        return billing.findCommissionByBillId(billId);
    }

    @Override
    public int markBillSettlementsReversed(String billId) {
        return billing.markBillSettlementsReversed(billId);
    }

    @Override
    public BarBillAdjustmentRecord saveBillAdjustment(BarBillAdjustmentRecord adjustment) {
        return billing.saveBillAdjustment(adjustment);
    }

    @Override
    public List<BarBillAdjustmentRecord> listBillAdjustments(String billId) {
        return billing.listBillAdjustments(billId);
    }

    @Override
    public BarBillOperationRecord saveBillOperation(BarBillOperationRecord operation) {
        return billing.saveBillOperation(operation);
    }

    @Override
    public List<BarBillOperationRecord> listBillOperations(String billId) {
        return billing.listBillOperations(billId);
    }

    @Override
    public BarCommissionCalculationRecord saveCommissionCalculation(BarCommissionCalculationRecord calculation) {
        return billing.saveCommissionCalculation(calculation);
    }

    @Override
    public BarTableSessionRecord saveTableSession(BarTableSessionRecord session) {
        return sessionRepository.saveTableSession(session);
    }

    @Override
    public boolean updateTableSessionIfVersionMatches(BarTableSessionRecord session, long expectedVersion) {
        return sessionRepository.updateTableSessionIfVersionMatches(session, expectedVersion);
    }

    @Override
    public List<BarTableSessionRecord> listTableSessions(Optional<String> merchantScope) {
        return sessionRepository.listTableSessions(merchantScope);
    }

    @Override
    public Optional<BarTableSessionRecord> findTableSessionById(String id) {
        return sessionRepository.findTableSessionById(id);
    }

    @Override
    public BarTableSessionAdjustmentRecord saveTableSessionAdjustment(BarTableSessionAdjustmentRecord adjustment) {
        return sessionRepository.saveTableSessionAdjustment(adjustment);
    }

    @Override
    public List<BarTableSessionAdjustmentRecord> listTableSessionAdjustments(String sessionId) {
        return sessionRepository.listTableSessionAdjustments(sessionId);
    }

    @Override
    public boolean atomicDecreaseStock(String itemId, String locationId, int quantity) {
        return inventoryPayment.atomicDecreaseStock(itemId, locationId, quantity);
    }

    @Override
    public boolean atomicIncreaseStock(String itemId, String locationId, int quantity) {
        return inventoryPayment.atomicIncreaseStock(itemId, locationId, quantity);
    }

    @Override
    public Optional<StockBalanceSnapshot> findStockBalance(String itemId, String locationId) {
        return inventoryPayment.findStockBalance(itemId, locationId);
    }

    @Override
    public StockBalanceSnapshot saveStockBalance(StockBalanceSnapshot balance) {
        return inventoryPayment.saveStockBalance(balance);
    }

    @Override
    public StockMovementSnapshot saveStockMovement(StockMovementSnapshot movement) {
        return inventoryPayment.saveStockMovement(movement);
    }

    @Override
    public PaymentIntentRecord savePaymentIntent(PaymentIntentRecord paymentIntent) {
        return inventoryPayment.savePaymentIntent(paymentIntent);
    }

    @Override
    public void setReservationArchiveReason(String id, String reason) {
        catalog.setReservationArchiveReason(id, reason);
    }
}
