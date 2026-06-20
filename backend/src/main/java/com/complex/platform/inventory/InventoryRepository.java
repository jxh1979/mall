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

    PurchaseRequestRecord savePurchaseRequest(PurchaseRequestRecord request);

    List<PurchaseRequestRecord> listPurchaseRequests();

    // R5 T6 采购周期状态机：行锁读取 + 流转留痕事件。
    Optional<PurchaseRequestRecord> findPurchaseRequestByIdForUpdate(String id);

    PurchaseRequestEventRecord savePurchaseRequestEvent(PurchaseRequestEventRecord event);

    List<PurchaseRequestEventRecord> listPurchaseRequestEvents(String requestId);

    Optional<StockBalanceRecord> findStockBalance(String itemId, String locationId);

    StockBalanceRecord saveStockBalance(StockBalanceRecord balance);

    List<StockBalanceRecord> listStockBalances();

    InventoryStockBatchRecord saveStockBatch(InventoryStockBatchRecord batch);

    List<InventoryStockBatchRecord> listStockBatches();

    InventoryRiskAlertRecord saveRiskAlert(InventoryRiskAlertRecord alert);

    List<InventoryRiskAlertRecord> listRiskAlerts();

    SupplierReconciliationRecord saveSupplierReconciliation(SupplierReconciliationRecord reconciliation);

    List<SupplierReconciliationRecord> listSupplierReconciliations();

    StockMovementRecord saveStockMovement(StockMovementRecord movement);

    List<StockMovementRecord> listStockMovements();

    StockAdjustmentRecord saveStockAdjustment(StockAdjustmentRecord adjustment);

    /** R5 T6-4：高价值复核 approve/reject 时按 id 行锁读取报损/盘点差异调整单（生效/收口）。 */
    Optional<StockAdjustmentRecord> findStockAdjustmentByIdForUpdate(String id);

    SavedBottleRecord saveSavedBottle(SavedBottleRecord savedBottle);

    Optional<SavedBottleRecord> findSavedBottleById(String id);

    List<SavedBottleRecord> listSavedBottles();

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
    int atomicDecreaseStockBalance(String itemId, String locationId, int qty);

    void atomicIncreaseStockBalance(String itemId, String locationId, int qty, String newId, String merchantId, String warehouseId);

    // P0-2: 行锁读取，消除 claimSavedBottle 竞态
    Optional<SavedBottleRecord> findSavedBottleByIdForUpdate(String id);

    // P1: 按商户过滤的列表，实现数据级租户隔离
    List<InventoryItemRecord> listInventoryItemsByMerchant(String merchantId);

    List<InventorySupplierRecord> listInventorySuppliersByMerchant(String merchantId);

    List<PurchaseRequestRecord> listPurchaseRequestsByMerchant(String merchantId);

    List<StockBalanceRecord> listStockBalancesByMerchant(String merchantId);

    List<StockMovementRecord> listStockMovementsByMerchant(String merchantId);

    List<InventoryStockBatchRecord> listStockBatchesByMerchant(String merchantId);

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

    InventoryVarianceRecord saveInventoryVariance(InventoryVarianceRecord variance);

    Optional<InventoryVarianceRecord> findInventoryVarianceByIdForUpdate(String id);

    List<InventoryVarianceRecord> listInventoryVariancesByStocktake(String stocktakeId);

    /**
     * V108 差异复核：条件 UPDATE（WHERE review_status='pending'）置 approved/rejected，受影响行数==1 才成功，
     * 防并发重复复核。返回受影响行数。
     */
    int reviewInventoryVarianceIfPending(
            String varianceId, String decision, String reviewedBy, Instant reviewedAt);
}
