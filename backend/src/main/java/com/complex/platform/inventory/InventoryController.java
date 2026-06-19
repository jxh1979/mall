package com.complex.platform.inventory;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

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
import com.complex.platform.inventory.dto.SupplyContractApproveRequest;
import com.complex.platform.inventory.dto.SupplyContractCreateRequest;
import com.complex.platform.inventory.dto.WarehouseCreateRequest;
import com.complex.platform.inventory.dto.TraceExceptionReviewRequest;
import com.complex.platform.inventory.dto.TraceHandoverCreateRequest;
import com.complex.platform.inventory.dto.TraceRobotReportRequest;
import com.complex.platform.inventory.dto.TraceScanSegmentRequest;
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
    private final SupplyContractService supplyContractService;
    private final WarehouseService warehouseService;
    private final AuthorizationService authorizationService;

    public InventoryController(
            InventoryService service,
            SupplyContractService supplyContractService,
            WarehouseService warehouseService,
            AuthorizationService authorizationService
    ) {
        this.service = service;
        this.supplyContractService = supplyContractService;
        this.warehouseService = warehouseService;
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
        authorizationService.requirePermission("inventory.handover.scan");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.scanTraceCodeHandover(request));
    }

    @PostMapping("/trace-handovers")
    public ApiResponse<TraceHandoverChainView> createTraceHandover(@RequestBody TraceHandoverCreateRequest request) {
        authorizationService.requirePermission("inventory.handover.scan");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createTraceHandover(request));
    }

    @PostMapping("/trace-handovers/{id}/scan-segment")
    public ApiResponse<TraceHandoverChainView> scanTraceSegment(
            @PathVariable String id,
            @RequestBody TraceScanSegmentRequest request
    ) {
        authorizationService.requirePermission("inventory.handover.scan");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.scanTraceSegment(id, request));
    }

    @PostMapping("/trace-handovers/{id}/robot-report")
    public ApiResponse<TraceHandoverChainView> reportRobotSegment(
            @PathVariable String id,
            @RequestBody TraceRobotReportRequest request
    ) {
        authorizationService.requirePermission("inventory.handover.scan");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reportRobotSegment(id, request));
    }

    @PostMapping("/trace-handovers/exceptions/{id}/review")
    public ApiResponse<TraceHandoverExceptionRecord> reviewException(
            @PathVariable String id,
            @RequestBody TraceExceptionReviewRequest request
    ) {
        authorizationService.requirePermission("inventory.handover.review");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reviewException(id, request));
    }

    // ── R5 T6：供应合同三模式 ──────────────────────────────────────────────────
    @GetMapping("/supply-contracts")
    public ApiResponse<PageResponse<SupplyContractRecord>> listSupplyContracts() {
        authorizationService.requirePermission("inventory.supply_contract.manage");
        return ApiResponse.ok(supplyContractService.listSupplyContracts());
    }

    @PostMapping("/supply-contracts")
    public ApiResponse<SupplyContractRecord> createSupplyContract(@RequestBody SupplyContractCreateRequest request) {
        authorizationService.requirePermission("inventory.supply_contract.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(supplyContractService.createSupplyContract(request));
    }

    @PostMapping("/supply-contracts/{id}/approve")
    public ApiResponse<SupplyContractRecord> approveSupplyContract(
            @PathVariable String id,
            @RequestBody SupplyContractApproveRequest request
    ) {
        authorizationService.requirePermission("inventory.purchase.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(supplyContractService.approveSupplyContract(id, request));
    }

    // ── R5 T6：结构化库房 ─────────────────────────────────────────────────────
    @GetMapping("/warehouses")
    public ApiResponse<PageResponse<WarehouseRecord>> listWarehouses() {
        authorizationService.requirePermission("inventory.warehouse.manage");
        return ApiResponse.ok(warehouseService.listWarehouses());
    }

    @PostMapping("/warehouses")
    public ApiResponse<WarehouseRecord> createWarehouse(@RequestBody WarehouseCreateRequest request) {
        authorizationService.requirePermission("inventory.warehouse.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(warehouseService.createWarehouse(request));
    }
}
