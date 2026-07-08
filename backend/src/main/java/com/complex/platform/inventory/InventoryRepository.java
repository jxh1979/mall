package com.complex.platform.inventory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository {
    InventoryItemRecord saveInventoryItem(InventoryItemRecord item);

    List<InventoryItemRecord> listInventoryItems();

    Optional<InventoryItemRecord> findInventoryItemById(String id);

    InventorySupplierRecord saveInventorySupplier(InventorySupplierRecord supplier);

    List<InventorySupplierRecord> listInventorySuppliers();

    // ── V277 跨主体流转:default 以不破坏测试替身 ──
    default SupplyEntityRecord saveSupplyEntity(SupplyEntityRecord entity) { throw new UnsupportedOperationException(); }
    default List<SupplyEntityRecord> listSupplyEntities() { return java.util.List.of(); }
    default java.util.Optional<SupplyEntityRecord> findSupplyEntityById(String id) { return java.util.Optional.empty(); }
    default InventoryTransferOrderRecord saveTransferOrder(InventoryTransferOrderRecord order) { throw new UnsupportedOperationException(); }
    default void saveTransferLine(InventoryTransferLineRecord line) { }
    default java.util.Optional<InventoryTransferOrderRecord> findTransferOrderById(String id) { return java.util.Optional.empty(); }
    default List<InventoryTransferOrderRecord> listTransferOrders() { return java.util.List.of(); }
    default List<InventoryTransferLineRecord> listTransferLinesByOrder(String orderId) { return java.util.List.of(); }
    default void saveTraceOwnershipHistory(InventoryTraceOwnershipHistoryRecord history) { }
    default int updateTraceCodeOwnerIfExists(String traceCode, String fromOwnerId, String ownerType, String ownerId, java.time.Instant now) { return 0; }

    PurchaseRequestRecord savePurchaseRequest(PurchaseRequestRecord request);

    List<PurchaseRequestRecord> listPurchaseRequests();

    // R5 T6 采购周期状态机：行锁读取 + 流转留痕事件。
    Optional<PurchaseRequestRecord> findPurchaseRequestByIdForUpdate(String id);

    PurchaseRequestEventRecord savePurchaseRequestEvent(PurchaseRequestEventRecord event);

    List<PurchaseRequestEventRecord> listPurchaseRequestEvents(String requestId);

    Optional<StockBalanceRecord> findStockBalance(String itemId, String locationId);

    // 2e 盘点冻结：系统账面合计（item+warehouse），盘点 book_qty 取此而非客户端传入，防伪造。
    int sumStockBalanceByItemAndWarehouse(String itemId, String warehouseId);

    StockBalanceRecord saveStockBalance(StockBalanceRecord balance);

    List<StockBalanceRecord> listStockBalances();

    InventoryStockBatchRecord saveStockBatch(InventoryStockBatchRecord batch, String projectId);

    // 2e 批次扣减：FEFO 排序的可消费 active 批次（先到期先出）。
    List<InventoryStockBatchRecord> listActiveBatchesForConsumption(String itemId, String locationId);

    // 2e 批次扣减：原子扣减批次 quantity（CAS，扣至 0 → consumed）。返回受影响行数。
    int atomicDecreaseBatchQuantity(String batchId, int qty);

    List<InventoryStockBatchRecord> listStockBatches();

    InventoryRiskAlertRecord saveRiskAlert(InventoryRiskAlertRecord alert);

    List<InventoryRiskAlertRecord> listRiskAlerts();

    SupplierReconciliationRecord saveSupplierReconciliation(SupplierReconciliationRecord reconciliation);

    List<SupplierReconciliationRecord> listSupplierReconciliations();

    StockMovementRecord saveStockMovement(StockMovementRecord movement, String projectId);

    List<StockMovementRecord> listStockMovements();

    StockAdjustmentRecord saveStockAdjustment(StockAdjustmentRecord adjustment);

    /** R5 T6-4：高价值复核 approve/reject 时按 id 行锁读取报损/盘点差异调整单（生效/收口）。 */
    Optional<StockAdjustmentRecord> findStockAdjustmentByIdForUpdate(String id);

    SavedBottleRecord saveSavedBottle(SavedBottleRecord savedBottle);

    Optional<SavedBottleRecord> findSavedBottleById(String id);

    List<SavedBottleRecord> listSavedBottles();

    default List<SavedBottleRecord> listStoredSavedBottlesExpiring(Instant now, Instant windowEnd) {
        return listSavedBottles().stream()
                .filter(bottle -> "stored".equals(bottle.status()))
                .filter(bottle -> bottle.expireAt() != null)
                .filter(bottle -> bottle.expireAt().isAfter(now) && !bottle.expireAt().isAfter(windowEnd))
                .toList();
    }

    default List<SavedBottleRecord> listStoredSavedBottlesExpired(Instant now) {
        return listSavedBottles().stream()
                .filter(bottle -> "stored".equals(bottle.status()))
                .filter(bottle -> bottle.expireAt() != null)
                .filter(bottle -> bottle.expireAt().isBefore(now))
                .toList();
    }

    SavedBottleClaimRecord saveSavedBottleClaim(SavedBottleClaimRecord claim);

    // P0#4 存酒转赠留痕。
    SavedBottleTransferRecord saveSavedBottleTransfer(SavedBottleTransferRecord transfer);

    TraceHandoverRecord saveTraceHandover(TraceHandoverRecord handover);

    // R5 T17：交接全链状态机——表头查询、分段步骤、异常复核、机器人段被动留痕（T14 关联）。
    Optional<TraceHandoverRecord> findTraceHandoverByIdForUpdate(String id);

    List<TraceHandoverStepRecord> listTraceHandoverSteps(String handoverId);

    Optional<TraceHandoverStepRecord> findTraceHandoverStep(String handoverId, String segment);

    TraceHandoverStepRecord saveTraceHandoverStep(TraceHandoverStepRecord step);

    TraceHandoverExceptionRecord saveTraceHandoverException(TraceHandoverExceptionRecord exception);

    Optional<TraceHandoverExceptionRecord> findTraceHandoverExceptionByIdForUpdate(String id);

    /** T14 关联：机器人段被动留痕写入 integration_robot_task_events(handover_id)。 */
    void saveRobotTaskEvent(String id, String robotTaskRef, String eventType, String handoverId, String payloadJson, Instant reportedAt);

    // P0-1: 原子库存余额操作，消除竞态条件
    // C1: merchantScope 谓词（null=平台/项目管理员放行全量），数据层防跨商户扣减。
    int atomicDecreaseStockBalance(String itemId, String locationId, int qty, String merchantScope);

    // C2: projectId 归属列，供项目级读隔离。
    void atomicIncreaseStockBalance(String itemId, String locationId, int qty, String newId, String merchantId, String warehouseId, String projectId);

    // P0-2: 行锁读取，消除 claimSavedBottle 竞态
    Optional<SavedBottleRecord> findSavedBottleByIdForUpdate(String id);

    // P1: 按商户过滤的列表，实现数据级租户隔离
    List<InventoryItemRecord> listInventoryItemsByMerchant(String merchantId);

    List<InventorySupplierRecord> listInventorySuppliersByMerchant(String merchantId);

    List<PurchaseRequestRecord> listPurchaseRequestsByMerchant(String merchantId);

    List<StockBalanceRecord> listStockBalancesByMerchant(String merchantId);

    // C2: 项目级读隔离通道（项目员工无商户 scope 时按项目过滤）。
    List<StockBalanceRecord> listStockBalancesByProject(String projectId);

    List<StockMovementRecord> listStockMovementsByMerchant(String merchantId);

    // C2 read-side: project-scoped movement list (project staff, no merchant scope).
    List<StockMovementRecord> listStockMovementsByProject(String projectId);

    List<InventoryStockBatchRecord> listStockBatchesByMerchant(String merchantId);

    // C2 read-side: project-scoped batch list (project staff, no merchant scope).
    List<InventoryStockBatchRecord> listStockBatchesByProject(String projectId);

    List<InventoryRiskAlertRecord> listRiskAlertsByMerchant(String merchantId);

    List<SupplierReconciliationRecord> listSupplierReconciliationsByMerchant(String merchantId);

    List<SavedBottleRecord> listSavedBottlesByMerchant(String merchantId);

    // R5 T6：供应合同三模式 + 结构化库房。
    SupplyContractRecord saveSupplyContract(SupplyContractRecord contract);

    Optional<SupplyContractRecord> findSupplyContractByIdForUpdate(String id);

    List<SupplyContractRecord> listSupplyContracts();

    List<SupplyContractRecord> listSupplyContractsByMerchant(String merchantId);

    WarehouseRecord saveWarehouse(WarehouseRecord warehouse);

    List<WarehouseRecord> listWarehouses();

    List<WarehouseRecord> listWarehousesByMerchant(String merchantId);

    // R5 T6：库存域内高价值复核（inventory_high_value_reviews）。
    InventoryHighValueReviewRecord saveHighValueReview(InventoryHighValueReviewRecord review);

    Optional<InventoryHighValueReviewRecord> findHighValueReviewByIdForUpdate(String id);

    List<InventoryHighValueReviewRecord> listHighValueReviews();

    // P1#18：供应商资质（进货查验）+ 结算多档/账期子表。
    SupplierQualificationRecord saveSupplierQualification(SupplierQualificationRecord qualification);

    List<SupplierQualificationRecord> listSupplierQualificationsBySupplier(String supplierId);

    List<SupplierQualificationRecord> listSupplierQualificationsByMerchant(String merchantId);

    List<SupplierQualificationRecord> listSupplierQualifications();

    SupplierSettlementMethodRecord saveSupplierSettlementMethod(SupplierSettlementMethodRecord method);

    List<SupplierSettlementMethodRecord> listSupplierSettlementMethodsBySupplier(String supplierId);

    List<SupplierSettlementMethodRecord> listSupplierSettlementMethodsByMerchant(String merchantId);

    List<SupplierSettlementMethodRecord> listSupplierSettlementMethods();

    // ── V108/V126：盘点单（stocktake）+ 库存差异明细（inventory_variance）写入侧。 ──
    StocktakeRecord saveStocktake(StocktakeRecord stocktake);

    Optional<StocktakeRecord> findStocktakeById(String id);

    List<StocktakeRecord> listStocktakes();

    List<StocktakeRecord> listStocktakesByMerchant(String merchantId);

    // 2e 盘点冻结：该库房是否存在未关闭（counting/reviewing）盘点单——出入库据此冻结。
    boolean hasActiveStocktakeForWarehouse(String warehouseId);

    InventoryVarianceRecord saveInventoryVariance(InventoryVarianceRecord variance);

    Optional<InventoryVarianceRecord> findInventoryVarianceByIdForUpdate(String id);

    List<InventoryVarianceRecord> listInventoryVariancesByStocktake(String stocktakeId);

    /**
     * V108 差异复核：条件 UPDATE（WHERE review_status='pending'）置 approved/rejected，受影响行数==1 才成功，
     * 防并发重复复核。返回受影响行数。
     */
    int reviewInventoryVarianceIfPending(
            String varianceId, String decision, String reviewedBy, Instant reviewedAt);

    // ── V128 / REQ-39：溯源码主数据（inventory_trace_codes）。 ──
    InventoryTraceCodeRecord saveTraceCode(InventoryTraceCodeRecord traceCode);

    Optional<InventoryTraceCodeRecord> findTraceCodeById(String id);

    Optional<InventoryTraceCodeRecord> findTraceCodeByCode(String traceCode);

    List<InventoryTraceCodeRecord> listTraceCodes();

    List<InventoryTraceCodeRecord> listTraceCodesByMerchant(String merchantId);

    /**
     * V128 出库打印：条件 UPDATE（WHERE status='registered'）置 printed + 回填 printed_at，受影响行数==1
     * 才成功，防重复打印。返回受影响行数。
     */
    int printTraceCodeIfRegistered(String id, Instant printedAt, String boundRef);
}
