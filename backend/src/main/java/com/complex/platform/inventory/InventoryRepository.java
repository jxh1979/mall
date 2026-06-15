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
}
