package com.complex.platform.bar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.ticketing.GiftReversedEvent;
import com.complex.platform.ticketing.GiftVerifiedEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * R5 T15 赠饮兑换消费者单元测试：覆盖 核销事件→0元单→扣减→inventory_movement_id 回填 全链、
 * 幂等（重复事件只一单一次扣减）、冲正回补、以及 AUD-015 fail-closed（缺 SKU 映射/缺台位/库存不足/
 * 商户吧缺内部结算价一律抛错不标记 delivered，保票权益可用）的落地形态。
 */
class BarGiftConsumerTests {

    private static final String BAR_ID = "bar_1";
    private static final String ITEM_REF = "sku_beer";
    private static final String LOCATION = "default";

    private CapturingBarRepository repository;
    private BarGiftConsumer consumer;

    @BeforeEach
    void setUp() {
        repository = new CapturingBarRepository();
        // 吧台落 FK 用台位
        repository.saveTable(table("table_1", BAR_ID));
        // SKU 映射存在时的库存余额
        repository.saveStockBalance(new StockBalanceSnapshot(
                "balance_1", ITEM_REF, LOCATION, 10, 0, 10, Instant.now()));
        consumer = new BarGiftConsumer(repository);
    }

    @Test
    void giftVerifiedWithItemRefCreatesZeroAmountOrderDeductsStockAndBackfillsMovementId() {
        consumer.onGiftVerified(new GiftVerifiedEvent(
                "ent_1", "ver_1", ITEM_REF, BAR_ID, null, "merchant_1"));

        // 0 元赠饮单（order_type=gift, amount=0）已落、状态 delivered。
        List<BarOrderRecord> orders = repository.listOrders(Optional.empty());
        assertThat(orders).hasSize(1);
        BarOrderRecord order = orders.getFirst();
        assertThat(order.orderType()).isEqualTo("gift");
        assertThat(order.totalAmountCent()).isZero();
        assertThat(order.status()).isEqualTo("delivered");

        // 库存按 redeem_consume 扣减 1。
        assertThat(repository.findStockBalance(ITEM_REF, LOCATION).orElseThrow().availableQuantity()).isEqualTo(9);
        assertThat(repository.stockMovements).hasSize(1);
        assertThat(repository.stockMovements.getFirst().movementType()).isEqualTo("redeem_consume");
        String movementId = repository.stockMovements.getFirst().id();

        // 出品 deliver 事件回填 inventory_movement_id。
        BarProductionTicketEventRecord event = repository.productionTicketEvents.stream()
                .filter(e -> "deliver".equals(e.actionType())).findFirst().orElseThrow();
        assertThat(event.inventoryMovementId()).isEqualTo(movementId);

        // 桥表 delivered + 回填 movement id。
        BarGiftRedemptionRecord redemption = repository.findGiftRedemptionByVerification("ver_1").orElseThrow();
        assertThat(redemption.status()).isEqualTo("delivered");
        assertThat(redemption.barOrderId()).isEqualTo(order.id());
        assertThat(redemption.inventoryMovementId()).isEqualTo(movementId);
    }

    @Test
    void giftVerifiedAndReversedSettlesAndReversesPlatformBarPayable() {
        CapturingLedger ledger = new CapturingLedger();
        BarGiftConsumer settlingConsumer = new BarGiftConsumer(repository, ledger);
        // 该免费酒水对应吧台商品（内部结算价 4200），经 itemRef（库存物料）桥接。
        repository.giftDrinkProduct = new BarProductRecord(
                "prod_beer", BAR_ID, "merchant_1", "House Beer", "beer", 3800L, "CNY",
                ITEM_REF, LOCATION, false, "active", Instant.now(), Instant.now(),
                null, null, null, false, null, null, 4200L);

        settlingConsumer.onGiftVerified(new GiftVerifiedEvent("ent_s", "ver_s", ITEM_REF, BAR_ID, null, "merchant_1"));
        // 平台→酒吧结算应付按内部价 4200 生成。
        assertThat(ledger.settledVerificationId).isEqualTo("ver_s");
        assertThat(ledger.settledMerchantId).isEqualTo("merchant_1");
        assertThat(ledger.settledAmount).isEqualTo(4200L);

        settlingConsumer.onGiftReversed(new GiftReversedEvent("ver_s"));
        // 赠饮冲正 → 冲销结算应付。
        assertThat(ledger.reversedVerificationId).isEqualTo("ver_s");
    }

    @Test
    void giftMerchantBarWithoutInternalPriceThrowsFailClosed() {
        // AUD-015：商户吧赠饮但吧台商品无内部结算价 → 平台无从挂应付（商户白送/零补偿=资金漏），必须 fail-closed 抛错。
        CapturingLedger ledger = new CapturingLedger();
        BarGiftConsumer settlingConsumer = new BarGiftConsumer(repository, ledger);
        repository.giftDrinkProduct = new BarProductRecord(
                "prod_beer", BAR_ID, "merchant_1", "House Beer", "beer", 3800L, "CNY",
                ITEM_REF, LOCATION, false, "active", Instant.now(), Instant.now(),
                null, null, null, false, null, null, null);
        assertThatThrownBy(() -> settlingConsumer.onGiftVerified(
                new GiftVerifiedEvent("ent_n", "ver_n", ITEM_REF, BAR_ID, null, "merchant_1")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("内部结算价");
        assertThat(ledger.settledVerificationId).isNull();
    }

    @Test
    void giftPlatformBarSkipsSettlementWithoutThrow() {
        // 平台自营吧（merchantId null）→ 无商户结算，正常交付、跳过结算，不抛错。
        CapturingLedger ledger = new CapturingLedger();
        BarGiftConsumer settlingConsumer = new BarGiftConsumer(repository, ledger);
        repository.giftDrinkProduct = new BarProductRecord(
                "prod_beer2", BAR_ID, null, "House Beer", "beer", 3800L, "CNY",
                ITEM_REF, LOCATION, false, "active", Instant.now(), Instant.now(),
                null, null, null, false, null, null, 4200L);
        settlingConsumer.onGiftVerified(new GiftVerifiedEvent("ent_p", "ver_p", ITEM_REF, BAR_ID, null, null));
        assertThat(ledger.settledVerificationId).isNull();
        assertThat(repository.findGiftRedemptionByVerification("ver_p").orElseThrow().status())
                .isEqualTo("delivered");
    }

    @Test
    void giftVerifiedIsIdempotentAcrossRepeatedEvents() {
        GiftVerifiedEvent event = new GiftVerifiedEvent("ent_1", "ver_1", ITEM_REF, BAR_ID, null, "merchant_1");
        consumer.onGiftVerified(event);
        consumer.onGiftVerified(event); // 重复核销事件

        assertThat(repository.listOrders(Optional.empty())).hasSize(1); // 幂等：只一单
        assertThat(repository.stockMovements).hasSize(1);               // 只扣一次
        assertThat(repository.findStockBalance(ITEM_REF, LOCATION).orElseThrow().availableQuantity()).isEqualTo(9);
    }

    @Test
    void giftReversedRestoresStockAndMarksRedemptionReversed() {
        consumer.onGiftVerified(new GiftVerifiedEvent("ent_1", "ver_1", ITEM_REF, BAR_ID, null, "merchant_1"));
        assertThat(repository.findStockBalance(ITEM_REF, LOCATION).orElseThrow().availableQuantity()).isEqualTo(9);

        consumer.onGiftReversed(new GiftReversedEvent("ver_1"));

        BarGiftRedemptionRecord redemption = repository.findGiftRedemptionByVerification("ver_1").orElseThrow();
        assertThat(redemption.status()).isEqualTo("reversed");
        // 库存回补到 10。
        assertThat(repository.findStockBalance(ITEM_REF, LOCATION).orElseThrow().availableQuantity()).isEqualTo(10);
    }

    @Test
    void giftReversedIsIdempotent() {
        consumer.onGiftVerified(new GiftVerifiedEvent("ent_1", "ver_1", ITEM_REF, BAR_ID, null, "merchant_1"));
        consumer.onGiftReversed(new GiftReversedEvent("ver_1"));
        consumer.onGiftReversed(new GiftReversedEvent("ver_1")); // 重复冲正

        assertThat(repository.findStockBalance(ITEM_REF, LOCATION).orElseThrow().availableQuantity()).isEqualTo(10);
    }

    @Test
    void giftVerifiedWithoutItemRefThrowsAndDoesNotDeliver() {
        // AUD-015：无 SKU 映射(bar_item_ref) → fail-closed 抛错，绝不落单/标记 delivered（生产同事务回滚保权益可用）。
        assertThatThrownBy(() -> consumer.onGiftVerified(
                new GiftVerifiedEvent("ent_1", "ver_1", null, BAR_ID, null, "merchant_1")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("bar_item_ref");
        assertThat(repository.listOrders(Optional.empty())).isEmpty();
        assertThat(repository.stockMovements).isEmpty();
        assertThat(repository.findGiftRedemptionByVerification("ver_1")).isEmpty();
    }

    @Test
    void giftVerifiedWithInsufficientStockThrowsAndDoesNotDeliver() {
        // AUD-015：库存不足（含并发抢到最后一件的失败方）→ fail-closed 抛错，绝不标记 delivered。
        repository.saveStockBalance(new StockBalanceSnapshot(
                "balance_1", ITEM_REF, LOCATION, 0, 0, 0, Instant.now()));
        assertThatThrownBy(() -> consumer.onGiftVerified(
                new GiftVerifiedEvent("ent_1", "ver_1", ITEM_REF, BAR_ID, null, "merchant_1")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("库存不足");
        assertThat(repository.findGiftRedemptionByVerification("ver_1")).isEmpty();
    }

    @Test
    void giftVerifiedSucceedsAfterStockReplenished() {
        // AUD-015 人工修复闭环：补足库存后，新的核销事件可正常交付（fail-closed 不留坏状态）。
        repository.saveStockBalance(new StockBalanceSnapshot(
                "balance_1", ITEM_REF, LOCATION, 0, 0, 0, Instant.now()));
        assertThatThrownBy(() -> consumer.onGiftVerified(
                new GiftVerifiedEvent("ent_1", "ver_1", ITEM_REF, BAR_ID, null, "merchant_1")))
                .isInstanceOf(ApiException.class);
        repository.saveStockBalance(new StockBalanceSnapshot(
                "balance_1", ITEM_REF, LOCATION, 5, 0, 5, Instant.now()));
        consumer.onGiftVerified(new GiftVerifiedEvent("ent_1", "ver_2", ITEM_REF, BAR_ID, null, "merchant_1"));
        assertThat(repository.findGiftRedemptionByVerification("ver_2").orElseThrow().status())
                .isEqualTo("delivered");
    }

    @Test
    void giftVerifiedWithoutResolvableTableThrowsAndDoesNotDeliver() {
        // AUD-015：吧台无可用台位 → fail-closed 抛错，不落单。
        assertThatThrownBy(() -> consumer.onGiftVerified(
                new GiftVerifiedEvent("ent_1", "ver_1", ITEM_REF, "bar_unknown", null, "merchant_1")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("台位");
        assertThat(repository.listOrders(Optional.empty())).isEmpty();
        assertThat(repository.findGiftRedemptionByVerification("ver_1")).isEmpty();
    }

    private static BarTableRecord table(String id, String barId) {
        Instant now = Instant.now();
        return new BarTableRecord(id, barId, "merchant_1", "T1", "Table 1", "standard", 4,
                0, 0L, "CNY", "active", now, now);
    }

    /** F-TXN-03 piece③b：捕获平台↔酒吧结算调用的内存 BarReceivableLedgerService（其余方法 stub）。 */
    private static final class CapturingLedger implements BarReceivableLedgerService {
        String settledVerificationId;
        String settledMerchantId;
        long settledAmount;
        String reversedVerificationId;

        @Override
        public boolean isBusinessDayLocked(java.time.LocalDate businessDate) {
            return false;
        }

        @Override
        public String createReceivableForOnAccountBill(BarBillRecord bill, BarOrderRecord order,
                String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds) {
            return null;
        }

        @Override
        public String createPayableForCommission(BarCommissionCalculationRecord commission) {
            return null;
        }

        @Override
        public String createRefundPayable(BarBillRecord bill, BarOrderRecord order, String adjustmentId,
                long refundAmountCent, String idempotencyKey, String reason, List<String> evidenceAttachmentIds) {
            return null;
        }

        @Override
        public String reverseRefundPayable(BarBillRecord bill, BarOrderRecord order, String reversalAdjustmentId,
                long refundAmountCent, String idempotencyKey, String reason, List<String> evidenceAttachmentIds) {
            return null;
        }

        @Override
        public String createPayableForGiftSettlement(String verificationId, String merchantId, long internalPriceCent) {
            settledVerificationId = verificationId;
            settledMerchantId = merchantId;
            settledAmount = internalPriceCent;
            return "arap_" + verificationId;
        }

        @Override
        public String reverseGiftSettlementPayable(String verificationId, String merchantId, long internalPriceCent) {
            reversedVerificationId = verificationId;
            return "arap_rev_" + verificationId;
        }
    }

    /** 仅实现消费者所需 + 断言所需方法的内存 BarRepository。 */
    private static final class CapturingBarRepository implements BarRepository {
        @Override public void setReservationArchiveReason(String id, String reason) { }
        @Override
        public java.util.Set<String> listActiveReservedTableIds(java.time.Instant since) {
            return java.util.Set.of();
        }

        private final Map<String, BarTableRecord> tables = new LinkedHashMap<>();
        private final Map<String, BarOrderRecord> orders = new LinkedHashMap<>();
        private final List<BarProductionTicketRecord> productionTickets = new ArrayList<>();
        final List<BarProductionTicketEventRecord> productionTicketEvents = new ArrayList<>();
        private final Map<String, StockBalanceSnapshot> stockBalances = new LinkedHashMap<>();
        final List<StockMovementSnapshot> stockMovements = new ArrayList<>();
        private final Map<String, BarGiftRedemptionRecord> giftRedemptions = new LinkedHashMap<>();
        // F-TXN-03 piece③b：通票赠饮 itemRef→bar_product 桥接的商品（含内部结算价）。
        BarProductRecord giftDrinkProduct;

        @Override
        public BarTableRecord saveTable(BarTableRecord table) {
            tables.put(table.id(), table);
            return table;
        }

        @Override
        public Optional<BarProductRecord> findProductByInventoryItem(String barId, String inventoryItemId) {
            if (giftDrinkProduct != null && barId.equals(giftDrinkProduct.barId())
                    && inventoryItemId.equals(giftDrinkProduct.inventoryItemId())) {
                return Optional.of(giftDrinkProduct);
            }
            return Optional.empty();
        }

        @Override
        public Optional<BarTableRecord> findFirstTableByBar(String barId) {
            return tables.values().stream().filter(t -> barId.equals(t.barId())).findFirst();
        }

        @Override
        public int assignTableSpace(String tableId, String spaceId) {
            return tables.containsKey(tableId) ? 1 : 0;
        }

        @Override
        public Optional<String> findSpaceMerchant(String spaceId) {
            return Optional.empty();
        }

        @Override
        public List<BarTableSpaceBacklogRecord> listTablesWithoutSpace(Optional<String> merchantScope) {
            return List.of();
        }

        @Override
        public BarOrderRecord saveOrder(BarOrderRecord order) {
            orders.put(order.id(), order);
            return order;
        }

        @Override
        public List<BarOrderRecord> listOrders(Optional<String> merchantScope) {
            return new ArrayList<>(orders.values());
        }

        @Override
        public BarProductionTicketRecord saveProductionTicket(BarProductionTicketRecord ticket) {
            productionTickets.removeIf(existing -> existing.id().equals(ticket.id()));
            productionTickets.add(ticket);
            return ticket;
        }

        @Override
        public BarProductionTicketEventRecord saveProductionTicketEvent(BarProductionTicketEventRecord event) {
            productionTicketEvents.add(event);
            return event;
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
                    current.id(), current.itemId(), current.locationId(),
                    current.quantity() - quantity, current.reservedQuantity(),
                    current.availableQuantity() - quantity, Instant.now()));
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
            return new ArrayList<>(giftRedemptions.values());
        }

        // ── 未使用的接口方法（默认空实现）──────────────────────────────
        @Override public List<BarTableRecord> listTables(Optional<String> merchantScope) { return List.of(); }
        @Override public Optional<BarTableRecord> findTableById(String id) { return Optional.ofNullable(tables.get(id)); }
        @Override public BarReservationRecord saveReservation(BarReservationRecord reservation) { return reservation; }
        @Override public List<BarReservationRecord> listReservations(Optional<String> merchantScope) { return List.of(); }
        @Override public Optional<BarReservationRecord> findReservationById(String id) { return Optional.empty(); }
        @Override public int countGuestCheckins(String reservationId) { return 0; }
        @Override public BarTableGuestCheckinRecord saveGuestCheckin(BarTableGuestCheckinRecord checkin) { return checkin; }
        @Override public BarProductRecord saveProduct(BarProductRecord product) { return product; }
        @Override public List<BarProductRecord> listProducts(Optional<String> merchantScope) { return List.of(); }
        @Override public Optional<BarProductRecord> findProductById(String id) { return Optional.empty(); }
        @Override public Optional<BarOrderRecord> findOrderById(String id) { return Optional.ofNullable(orders.get(id)); }
        @Override public BarOrderItemRecord saveOrderItem(BarOrderItemRecord item) { return item; }
        @Override public List<BarOrderItemRecord> listOrderItems(String orderId) { return List.of(); }
        @Override public Optional<BarOrderItemRecord> findOrderItemById(String id) { return Optional.empty(); }
        @Override public List<BarProductionTicketRecord> listProductionTickets(Optional<String> merchantScope) { return List.copyOf(productionTickets); }
        @Override public Optional<BarProductionTicketRecord> findProductionTicketById(String id) { return Optional.empty(); }
        @Override public Optional<BarProductionTicketRecord> findProductionTicketByOrderId(String orderId) { return Optional.empty(); }
        @Override public List<BarProductionTicketEventRecord> listProductionTicketEvents(String ticketId) { return List.copyOf(productionTicketEvents); }
        @Override public BarBillRecord saveBill(BarBillRecord bill) { return bill; }
        @Override public Optional<BarBillRecord> findBillById(String id) { return Optional.empty(); }
        @Override public List<BarBillRecord> listBillsByOrderIds(List<String> orderIds) { return List.of(); }
        @Override public Optional<BarBillRecord> findBillByIdForUpdate(String id) { return Optional.empty(); }
        @Override public Optional<BarCommissionCalculationRecord> findCommissionByBillId(String billId) { return Optional.empty(); }
        @Override public int markBillSettlementsReversed(String billId) { return 0; }
        @Override public BarBillAdjustmentRecord saveBillAdjustment(BarBillAdjustmentRecord adjustment) { return adjustment; }
        @Override public List<BarBillAdjustmentRecord> listBillAdjustments(String billId) { return List.of(); }
        @Override public BarBillOperationRecord saveBillOperation(BarBillOperationRecord operation) { return operation; }
        @Override public List<BarBillOperationRecord> listBillOperations(String billId) { return List.of(); }
        @Override public BarCommissionCalculationRecord saveCommissionCalculation(BarCommissionCalculationRecord calculation) { return calculation; }
        @Override public BarTableSessionRecord saveTableSession(BarTableSessionRecord session) { return session; }
        @Override public boolean updateTableSessionIfVersionMatches(BarTableSessionRecord session, long expectedVersion) { return true; }
        @Override public List<BarTableSessionRecord> listTableSessions(Optional<String> merchantScope) { return List.of(); }
        @Override public Optional<BarTableSessionRecord> findTableSessionById(String id) { return Optional.empty(); }
        @Override public BarTableSessionAdjustmentRecord saveTableSessionAdjustment(BarTableSessionAdjustmentRecord adjustment) { return adjustment; }
        @Override public List<BarTableSessionAdjustmentRecord> listTableSessionAdjustments(String sessionId) { return List.of(); }
        @Override public PaymentIntentRecord savePaymentIntent(PaymentIntentRecord paymentIntent) { return paymentIntent; }
    }
}
