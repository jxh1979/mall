package com.complex.platform.inventory;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.inventory.dto.InventoryItemRequest;
import com.complex.platform.inventory.dto.InventorySupplierRequest;
import com.complex.platform.inventory.dto.InventoryRiskScanRequest;
import com.complex.platform.inventory.dto.InventoryStockBatchRequest;
import com.complex.platform.inventory.dto.PurchaseRequestCreate;
import com.complex.platform.inventory.dto.SavedBottleClaimRequest;
import com.complex.platform.inventory.dto.SavedBottleRequest;
import com.complex.platform.inventory.dto.StockAdjustmentRequest;
import com.complex.platform.inventory.dto.StockInRequest;
import com.complex.platform.inventory.dto.StockMovementRequest;
import com.complex.platform.inventory.dto.SupplierReconciliationRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService service;
    private final AuthorizationService authorizationService;

    public InventoryController(InventoryService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/items")
    public ApiResponse<PageResponse<InventoryItemRecord>> listInventoryItems() {
        authorizationService.requirePermission("inventory.stock.view");
        return ApiResponse.ok(service.listInventoryItems());
    }

    @PostMapping("/items")
    public ApiResponse<InventoryItemRecord> createInventoryItem(@RequestBody InventoryItemRequest request) {
        authorizationService.requirePermission("inventory.stock.move");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createInventoryItem(request));
    }

    @GetMapping("/purchase-requests")
    public ApiResponse<PageResponse<PurchaseRequestRecord>> listPurchaseRequests() {
        authorizationService.requirePermission("inventory.purchase_request.view");
        return ApiResponse.ok(service.listPurchaseRequests());
    }

    @PostMapping("/purchase-requests")
    public ApiResponse<PurchaseRequestRecord> createPurchaseRequest(@RequestBody PurchaseRequestCreate request) {
        authorizationService.requirePermission("inventory.purchase_request.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createPurchaseRequest(request));
    }

    @GetMapping("/saved-bottles")
    public ApiResponse<PageResponse<SavedBottleRecord>> listSavedBottles() {
        authorizationService.requirePermission("bar.saved_bottle.manage");
        return ApiResponse.ok(service.listSavedBottles());
    }

    @PostMapping("/saved-bottles")
    public ApiResponse<SavedBottleRecord> createSavedBottle(@RequestBody SavedBottleRequest request) {
        authorizationService.requirePermission("bar.saved_bottle.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createSavedBottle(request));
    }

    @PostMapping("/saved-bottles/{id}/claim")
    public ApiResponse<SavedBottleClaimRecord> claimSavedBottle(
            @PathVariable String id,
            @RequestBody SavedBottleClaimRequest request
    ) {
        authorizationService.requirePermission("bar.saved_bottle.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.claimSavedBottle(id, request));
    }

    @PostMapping("/stock-adjustments")
    public ApiResponse<StockAdjustmentRecord> createStockAdjustment(@RequestBody StockAdjustmentRequest request) {
        authorizationService.requirePermission("inventory.stock.adjust");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createStockAdjustment(request));
    }

    @GetMapping("/stock-balances")
    public ApiResponse<PageResponse<StockBalanceRecord>> listStockBalances() {
        authorizationService.requirePermission("inventory.stock.view");
        return ApiResponse.ok(service.listStockBalances());
    }

    @GetMapping("/stock-batches")
    public ApiResponse<PageResponse<InventoryStockBatchRecord>> listStockBatches() {
        authorizationService.requirePermission("inventory.stock.view");
        return ApiResponse.ok(service.listStockBatches());
    }

    @PostMapping("/stock-batches")
    public ApiResponse<InventoryStockBatchRecord> createStockBatch(@RequestBody InventoryStockBatchRequest request) {
        authorizationService.requirePermission("inventory.stock.batch.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createStockBatch(request));
    }

    @GetMapping("/risk-alerts")
    public ApiResponse<PageResponse<InventoryRiskAlertRecord>> listRiskAlerts() {
        authorizationService.requirePermission("inventory.risk_alert.view");
        return ApiResponse.ok(service.listRiskAlerts());
    }

    @PostMapping("/risk-alerts/scan")
    public ApiResponse<InventoryRiskScanResult> runInventoryRiskScan(@RequestBody InventoryRiskScanRequest request) {
        authorizationService.requirePermission("inventory.risk.scan");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.runInventoryRiskScan(request));
    }

    @GetMapping("/supplier-reconciliations")
    public ApiResponse<PageResponse<SupplierReconciliationRecord>> listSupplierReconciliations() {
        authorizationService.requirePermission("inventory.supplier.reconcile");
        return ApiResponse.ok(service.listSupplierReconciliations());
    }

    @PostMapping("/supplier-reconciliations")
    public ApiResponse<SupplierReconciliationRecord> createSupplierReconciliation(
            @RequestBody SupplierReconciliationRequest request
    ) {
        authorizationService.requirePermission("inventory.supplier.reconcile");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createSupplierReconciliation(request));
    }

    @PostMapping("/stock-in")
    public ApiResponse<StockMovementRecord> createStockIn(@RequestBody StockInRequest request) {
        authorizationService.requirePermission("inventory.stock.move");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createStockIn(request));
    }

    @GetMapping("/stock-movements")
    public ApiResponse<PageResponse<StockMovementRecord>> listStockMovements() {
        authorizationService.requirePermission("inventory.stock.view");
        return ApiResponse.ok(service.listStockMovements());
    }

    @PostMapping("/stock-movements")
    public ApiResponse<StockMovementRecord> createStockMovement(@RequestBody StockMovementRequest request) {
        authorizationService.requirePermission("inventory.stock.move");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createStockMovement(request));
    }

    @PostMapping("/stock-movements/transfer")
    public ApiResponse<StockMovementRecord> transferStockMovement(@RequestBody StockMovementRequest request) {
        authorizationService.requirePermission("inventory.stock.move");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.transferStockMovement(request));
    }

    @PostMapping("/stock-movements/{movement_type}")
    public ApiResponse<StockMovementRecord> createTypedStockMovement(
            @PathVariable("movement_type") String movementType,
            @RequestBody StockMovementRequest request
    ) {
        authorizationService.requirePermission("inventory.stock.move");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createTypedStockMovement(movementType, request));
    }

    @GetMapping("/suppliers")
    public ApiResponse<PageResponse<InventorySupplierRecord>> listInventorySuppliers() {
        authorizationService.requirePermission("inventory.supplier.view");
        return ApiResponse.ok(service.listInventorySuppliers());
    }

    @PostMapping("/suppliers")
    public ApiResponse<InventorySupplierRecord> createInventorySupplier(@RequestBody InventorySupplierRequest request) {
        authorizationService.requirePermission("inventory.supplier.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createInventorySupplier(request));
    }

    @PostMapping("/trace-codes/scan-handover")
    public ApiResponse<TraceHandoverRecord> scanTraceCodeHandover(@RequestBody VerificationRequest request) {
        authorizationService.requirePermission("inventory.stock.move");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.scanTraceCodeHandover(request));
    }

    private void requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "idempotency_key is required");
        }
    }
}
