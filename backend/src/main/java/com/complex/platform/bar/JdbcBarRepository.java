package com.complex.platform.bar;

import com.complex.platform.finance.PaymentIntentRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcBarRepository implements BarRepository {

    private final JdbcBarCatalogRepository catalog;
    private final JdbcBarOrderRepository orderRepository;
    private final JdbcBarBillingRepository billing;
    private final JdbcBarSessionRepository sessionRepository;
    private final JdbcBarInventoryPaymentRepository inventoryPayment;

    JdbcBarRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        var jdbcObjectMapper = objectMapper.copy().findAndRegisterModules();
        var rowMapper = new JdbcBarRowMapper(jdbcObjectMapper);
        var parameterSource = new JdbcBarParameterSource(jdbcObjectMapper);
        this.catalog = new JdbcBarCatalogRepository(jdbcTemplate, rowMapper, parameterSource);
        this.orderRepository = new JdbcBarOrderRepository(jdbcTemplate, rowMapper, parameterSource);
        this.billing = new JdbcBarBillingRepository(jdbcTemplate, rowMapper, parameterSource);
        this.sessionRepository = new JdbcBarSessionRepository(jdbcTemplate, rowMapper, parameterSource);
        this.inventoryPayment = new JdbcBarInventoryPaymentRepository(jdbcTemplate, rowMapper, parameterSource);
    }
    @Override
    public BarTableRecord saveTable(BarTableRecord table) {
        return catalog.saveTable(table);
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
    public BarReservationRecord saveReservation(BarReservationRecord reservation) {
        return catalog.saveReservation(reservation);
    }

    @Override
    public List<BarReservationRecord> listReservations(Optional<String> merchantScope) {
        return catalog.listReservations(merchantScope);
    }

    @Override
    public Optional<BarReservationRecord> findReservationById(String id) {
        return catalog.findReservationById(id);
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
    public BarAgeVerificationRecord saveAgeVerification(BarAgeVerificationRecord verification) {
        return catalog.saveAgeVerification(verification);
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
    public Optional<BarBillRecord> findBillById(String id) {
        return billing.findBillById(id);
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
}
