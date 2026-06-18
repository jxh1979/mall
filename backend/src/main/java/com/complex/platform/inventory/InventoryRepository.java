package com.complex.platform.inventory;

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

    // P0-1: 原子库存余额操作，消除竞态条件
    int atomicDecreaseStockBalance(String itemId, String locationId, int qty);

    void atomicIncreaseStockBalance(String itemId, String locationId, int qty, String newId, String merchantId);

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
}
