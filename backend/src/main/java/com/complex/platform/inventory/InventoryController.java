package com.complex.platform.inventory;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.inventory.dto.CreateStocktakeRequest;
import com.complex.platform.inventory.dto.InventoryItemRequest;
import com.complex.platform.inventory.dto.InventorySupplierRequest;
import com.complex.platform.inventory.dto.RecordVarianceRequest;
import com.complex.platform.inventory.dto.ReviewVarianceRequest;
import com.complex.platform.inventory.dto.InventoryRiskScanRequest;
import com.complex.platform.inventory.dto.InventoryStockBatchRequest;
import com.complex.platform.inventory.dto.PrintTraceCodeRequest;
import com.complex.platform.inventory.dto.PurchaseRequestApproveRequest;
import com.complex.platform.inventory.dto.PurchaseRequestCreate;
import com.complex.platform.inventory.dto.RegisterTraceCodeRequest;
import com.complex.platform.inventory.dto.PurchaseRequestReceiveRequest;
import com.complex.platform.inventory.dto.HighValueReviewDecisionRequest;
import com.complex.platform.inventory.dto.PurchaseRequestTransitionRequest;
import com.complex.platform.inventory.dto.SavedBottleCancelRequest;
import com.complex.platform.inventory.dto.SavedBottleClaimRequest;
import com.complex.platform.inventory.dto.SavedBottleTransferRequest;
import com.complex.platform.inventory.dto.SavedBottleRequest;
import com.complex.platform.inventory.dto.StockAdjustmentRequest;
import com.complex.platform.inventory.dto.StockInRequest;
import com.complex.platform.inventory.dto.StockMovementRequest;
import com.complex.platform.inventory.dto.SupplierQualificationRequest;
import com.complex.platform.inventory.dto.SupplierReconciliationRequest;
import com.complex.platform.inventory.dto.SupplierSettlementMethodRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService service;
    private final SupplyContractService supplyContractService;
    private final PurchaseRequestService purchaseRequestService;
    private final WarehouseService warehouseService;
    private final SupplierQualificationService supplierQualificationService;
    private final AuthorizationService authorizationService;

    public InventoryController(
            InventoryService service,
            SupplyContractService supplyContractService,
            PurchaseRequestService purchaseRequestService,
            WarehouseService warehouseService,
            SupplierQualificationService supplierQualificationService,
            AuthorizationService authorizationService
    ) {
        this.service = service;
        this.supplyContractService = supplyContractService;
        this.purchaseRequestService = purchaseRequestService;
        this.warehouseService = warehouseService;
        this.supplierQualificationService = supplierQualificationService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/items")
    public ApiResponse<PageResponse<InventoryItemRecord>> listInventoryItems() {
        authorizationService.requirePermission("inventory.stock.view");
        return ApiResponse.ok(service.listInventoryItems());
    }

    @PostMapping("/items")
    public ApiResponse<InventoryItemRecord> createInventoryItem(@RequestBody @jakarta.validation.Valid InventoryItemRequest request) {
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
    public ApiResponse<PurchaseRequestRecord> createPurchaseRequest(@RequestBody @jakarta.validation.Valid PurchaseRequestCreate request) {
        authorizationService.requirePermission("inventory.purchase_request.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createPurchaseRequest(request));
    }

    // ── R5 T6：采购周期状态机（created/submitted → approved/rejected → ordered → expected → arrived → received）──
    @PostMapping("/purchase-requests/{id}/approve")
    public ApiResponse<PurchaseRequestRecord> approvePurchaseRequest(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid PurchaseRequestApproveRequest request
    ) {
        authorizationService.requirePermission("inventory.purchase.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(purchaseRequestService.approve(id, request));
    }

    @PostMapping("/purchase-requests/{id}/order")
    public ApiResponse<PurchaseRequestRecord> orderPurchaseRequest(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid PurchaseRequestTransitionRequest request
    ) {
        authorizationService.requirePermission("inventory.purchase.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(purchaseRequestService.order(id, request));
    }

    @PostMapping("/purchase-requests/{id}/arrive")
    public ApiResponse<PurchaseRequestRecord> arrivePurchaseRequest(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid PurchaseRequestTransitionRequest request
    ) {
        authorizationService.requirePermission("inventory.purchase.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(purchaseRequestService.arrive(id, request));
    }

    @PostMapping("/purchase-requests/{id}/receive")
    public ApiResponse<PurchaseRequestRecord> receivePurchaseRequest(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid PurchaseRequestReceiveRequest request
    ) {
        authorizationService.requirePermission("inventory.purchase.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(purchaseRequestService.receive(id, request));
    }

    @GetMapping("/saved-bottles")
    public ApiResponse<PageResponse<SavedBottleRecord>> listSavedBottles() {
        authorizationService.requirePermission("bar.saved_bottle.manage");
        return ApiResponse.ok(service.listSavedBottles());
    }

    @PostMapping("/saved-bottles")
    public ApiResponse<SavedBottleRecord> createSavedBottle(@RequestBody @jakarta.validation.Valid SavedBottleRequest request) {
        authorizationService.requirePermission("bar.saved_bottle.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createSavedBottle(request));
    }

    @PostMapping("/saved-bottles/{id}/transfer")
    public ApiResponse<SavedBottleTransferRecord> transferSavedBottle(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid SavedBottleTransferRequest request
    ) {
        authorizationService.requirePermission("bar.saved_bottle.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.transferSavedBottle(id, request));
    }

    @PostMapping("/saved-bottles/{id}/claim")
    public ApiResponse<SavedBottleClaimRecord> claimSavedBottle(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid SavedBottleClaimRequest request
    ) {
        authorizationService.requirePermission("bar.saved_bottle.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.claimSavedBottle(id, request));
    }

    @PostMapping("/saved-bottles/{id}/cancel")
    public ApiResponse<SavedBottleRecord> cancelSavedBottle(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid SavedBottleCancelRequest request
    ) {
        authorizationService.requirePermission("bar.saved_bottle.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.cancelSavedBottle(id, request));
    }

    // P0#4：存酒争议受理（disputed 入状态机）。
    @PostMapping("/saved-bottles/{id}/dispute")
    public ApiResponse<SavedBottleRecord> disputeSavedBottle(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid SavedBottleCancelRequest request
    ) {
        authorizationService.requirePermission("bar.saved_bottle.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.disputeSavedBottle(id, request));
    }

    // ── R5 T6：库存域内高价值复核（saved_bottle_void/damage_report/stock_variance）──
    @GetMapping("/high-value-reviews")
    public ApiResponse<PageResponse<InventoryHighValueReviewRecord>> listHighValueReviews() {
        authorizationService.requirePermission("inventory.high_value.review");
        return ApiResponse.ok(service.listHighValueReviews());
    }

    @PostMapping("/high-value-reviews/{id}/review")
    public ApiResponse<InventoryHighValueReviewRecord> reviewHighValue(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid HighValueReviewDecisionRequest request
    ) {
        authorizationService.requirePermission("inventory.high_value.review");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reviewHighValue(id, request));
    }

    // ── V108/V126：盘点录入闭环（建盘点单 → 录差异 → 差异复核(approved/rejected) → 查询）。 ──
    @GetMapping("/stocktakes")
    public ApiResponse<PageResponse<StocktakeRecord>> listStocktakes() {
        authorizationService.requirePermission("inventory.stocktake.view");
        return ApiResponse.ok(service.listStocktakes());
    }

    @GetMapping("/stocktakes/{id}")
    public ApiResponse<StocktakeDetailRecord> getStocktake(@PathVariable String id) {
        authorizationService.requirePermission("inventory.stocktake.view");
        return ApiResponse.ok(service.getStocktake(id));
    }

    @PostMapping("/stocktakes")
    public ApiResponse<StocktakeRecord> createStocktake(@RequestBody @jakarta.validation.Valid CreateStocktakeRequest request) {
        authorizationService.requirePermission("inventory.stocktake.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createStocktake(request));
    }

    @PostMapping("/stocktakes/{id}/variances")
    public ApiResponse<InventoryVarianceRecord> recordVariance(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid RecordVarianceRequest request
    ) {
        authorizationService.requirePermission("inventory.stocktake.variance_record");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.recordVariance(id, request));
    }

    @PostMapping("/stocktakes/{id}/variances/{vid}/review")
    public ApiResponse<InventoryVarianceRecord> reviewVariance(
            @PathVariable String id,
            @PathVariable String vid,
            @RequestBody @jakarta.validation.Valid ReviewVarianceRequest request
    ) {
        authorizationService.requirePermission("inventory.stocktake.variance_review");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reviewVariance(id, vid, request));
    }

    @PostMapping("/stock-adjustments")
    public ApiResponse<StockAdjustmentRecord> createStockAdjustment(@RequestBody @jakarta.validation.Valid StockAdjustmentRequest request) {
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
    public ApiResponse<InventoryStockBatchRecord> createStockBatch(@RequestBody @jakarta.validation.Valid InventoryStockBatchRequest request) {
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
    public ApiResponse<InventoryRiskScanResult> runInventoryRiskScan(@RequestBody @jakarta.validation.Valid InventoryRiskScanRequest request) {
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
            @RequestBody @jakarta.validation.Valid SupplierReconciliationRequest request
    ) {
        authorizationService.requirePermission("inventory.supplier.reconcile");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createSupplierReconciliation(request));
    }

    @PostMapping("/stock-in")
    public ApiResponse<StockMovementRecord> createStockIn(@RequestBody @jakarta.validation.Valid StockInRequest request) {
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
    public ApiResponse<StockMovementRecord> createStockMovement(@RequestBody @jakarta.validation.Valid StockMovementRequest request) {
        authorizationService.requirePermission("inventory.stock.move");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createStockMovement(request));
    }

    @PostMapping("/stock-movements/transfer")
    public ApiResponse<StockMovementRecord> transferStockMovement(@RequestBody @jakarta.validation.Valid StockMovementRequest request) {
        authorizationService.requirePermission("inventory.stock.move");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.transferStockMovement(request));
    }

    @PostMapping("/stock-movements/{movement_type}")
    public ApiResponse<StockMovementRecord> createTypedStockMovement(
            @PathVariable("movement_type") String movementType,
            @RequestBody @jakarta.validation.Valid StockMovementRequest request
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
    public ApiResponse<InventorySupplierRecord> createInventorySupplier(@RequestBody @jakarta.validation.Valid InventorySupplierRequest request) {
        authorizationService.requirePermission("inventory.supplier.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createInventorySupplier(request));
    }

    // ── V128 / REQ-39：溯源码主数据 CRUD（贴码登记 → 出库打印 → 责任链查询/列表）。 ──
    @GetMapping("/trace-codes")
    public ApiResponse<PageResponse<InventoryTraceCodeRecord>> listTraceCodes() {
        authorizationService.requirePermission("inventory.trace_code.view");
        return ApiResponse.ok(service.listTraceCodes());
    }

    @GetMapping("/trace-codes/{id}")
    public ApiResponse<InventoryTraceCodeRecord> getTraceCode(@PathVariable String id) {
        authorizationService.requirePermission("inventory.trace_code.view");
        return ApiResponse.ok(service.getTraceCode(id));
    }

    @PostMapping("/trace-codes")
    public ApiResponse<InventoryTraceCodeRecord> registerTraceCode(@RequestBody @jakarta.validation.Valid RegisterTraceCodeRequest request) {
        authorizationService.requirePermission("inventory.trace_code.register");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.registerTraceCode(request));
    }

    @PostMapping("/trace-codes/{id}/print")
    public ApiResponse<InventoryTraceCodeRecord> printTraceCode(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid PrintTraceCodeRequest request
    ) {
        authorizationService.requirePermission("inventory.trace_code.print");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.printTraceCode(id, request));
    }

    @PostMapping("/trace-codes/scan-handover")
    public ApiResponse<TraceHandoverRecord> scanTraceCodeHandover(@RequestBody @jakarta.validation.Valid VerificationRequest request) {
        authorizationService.requirePermission("inventory.handover.scan");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.scanTraceCodeHandover(request));
    }

    @PostMapping("/trace-handovers")
    public ApiResponse<TraceHandoverChainView> createTraceHandover(@RequestBody @jakarta.validation.Valid TraceHandoverCreateRequest request) {
        authorizationService.requirePermission("inventory.handover.scan");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createTraceHandover(request));
    }

    @PostMapping("/trace-handovers/{id}/scan-segment")
    public ApiResponse<TraceHandoverChainView> scanTraceSegment(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid TraceScanSegmentRequest request
    ) {
        authorizationService.requirePermission("inventory.handover.scan");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.scanTraceSegment(id, request));
    }

    @PostMapping("/trace-handovers/{id}/robot-report")
    public ApiResponse<TraceHandoverChainView> reportRobotSegment(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid TraceRobotReportRequest request
    ) {
        authorizationService.requirePermission("inventory.handover.scan");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reportRobotSegment(id, request));
    }

    @PostMapping("/trace-handovers/exceptions/{id}/review")
    public ApiResponse<TraceHandoverExceptionRecord> reviewException(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid TraceExceptionReviewRequest request
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
    public ApiResponse<SupplyContractRecord> createSupplyContract(@RequestBody @jakarta.validation.Valid SupplyContractCreateRequest request) {
        authorizationService.requirePermission("inventory.supply_contract.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(supplyContractService.createSupplyContract(request));
    }

    @PostMapping("/supply-contracts/{id}/approve")
    public ApiResponse<SupplyContractRecord> approveSupplyContract(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid SupplyContractApproveRequest request
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
    public ApiResponse<WarehouseRecord> createWarehouse(@RequestBody @jakarta.validation.Valid WarehouseCreateRequest request) {
        authorizationService.requirePermission("inventory.warehouse.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(warehouseService.createWarehouse(request));
    }

    // ── P1#18：供应商资质（进货查验） ─────────────────────────────────────────────
    @GetMapping("/supplier-qualifications")
    public ApiResponse<PageResponse<SupplierQualificationRecord>> listSupplierQualifications(
            @RequestParam(name = "supplier_id", required = false) String supplierId
    ) {
        authorizationService.requirePermission("inventory.supplier.qualification.manage");
        return ApiResponse.ok(supplierQualificationService.listQualifications(supplierId));
    }

    @PostMapping("/supplier-qualifications")
    public ApiResponse<SupplierQualificationRecord> createSupplierQualification(
            @RequestBody @jakarta.validation.Valid SupplierQualificationRequest request
    ) {
        authorizationService.requirePermission("inventory.supplier.qualification.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(supplierQualificationService.createQualification(request));
    }

    // ── P1#18：供应商结算多档/账期 ────────────────────────────────────────────────
    @GetMapping("/supplier-settlement-methods")
    public ApiResponse<PageResponse<SupplierSettlementMethodRecord>> listSupplierSettlementMethods(
            @RequestParam(name = "supplier_id", required = false) String supplierId
    ) {
        authorizationService.requirePermission("inventory.supplier.settlement.manage");
        return ApiResponse.ok(supplierQualificationService.listSettlementMethods(supplierId));
    }

    @PostMapping("/supplier-settlement-methods")
    public ApiResponse<SupplierSettlementMethodRecord> createSupplierSettlementMethod(
            @RequestBody @jakarta.validation.Valid SupplierSettlementMethodRequest request
    ) {
        authorizationService.requirePermission("inventory.supplier.settlement.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(supplierQualificationService.createSettlementMethod(request));
    }
}
