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

    SavedBottleRecord saveSavedBottle(SavedBottleRecord savedBottle);

    Optional<SavedBottleRecord> findSavedBottleById(String id);

    List<SavedBottleRecord> listSavedBottles();

    SavedBottleClaimRecord saveSavedBottleClaim(SavedBottleClaimRecord claim);

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
}
