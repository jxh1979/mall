package com.complex.platform.inventory;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.PhoneMask;
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
        return ApiResponse.ok(maskCostForUnauthorized(service.createStockIn(request)));
    }

    @GetMapping("/stock-movements")
    public ApiResponse<PageResponse<StockMovementRecord>> listStockMovements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        authorizationService.requirePermission("inventory.stock.view");
        return ApiResponse.ok(maskCostsForUnauthorized(service.listStockMovements(page, size)));
    }

    @PostMapping("/stock-movements")
    public ApiResponse<StockMovementRecord> createStockMovement(@RequestBody @jakarta.validation.Valid StockMovementRequest request) {
        authorizationService.requirePermission("inventory.stock.move");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskCostForUnauthorized(service.createStockMovement(request)));
    }

    @PostMapping("/stock-movements/transfer")
    public ApiResponse<StockMovementRecord> transferStockMovement(@RequestBody @jakarta.validation.Valid StockMovementRequest request) {
        authorizationService.requirePermission("inventory.stock.move");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskCostForUnauthorized(service.transferStockMovement(request)));
    }

    // ── V296 成本台账：成本流水分页 + 期间出库成本汇总（分红引擎成本侧取数点）。 ──
    // I-2：成本=内部口径（2026-07-08 内部结算价拍板范式），两端点要求独立高危权限 inventory.cost.view。
    @GetMapping("/cost-ledger")
    public ApiResponse<PageResponse<StockMovementRecord>> listInventoryCostLedger(
            @RequestParam(value = "item_id", required = false) String itemId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        authorizationService.requirePermission("inventory.cost.view");
        return ApiResponse.ok(service.listCostLedger(itemId, page, size));
    }

    @GetMapping("/cost-summary")
    public ApiResponse<InventoryCostSummaryRecord> getInventoryCostSummary(
            @RequestParam("period_start") String periodStart,
            @RequestParam("period_end") String periodEnd,
            @RequestParam(value = "merchant_id", required = false) String merchantId) {
        authorizationService.requirePermission("inventory.cost.view");
        return ApiResponse.ok(service.getCostSummary(merchantId, periodStart, periodEnd));
    }

    // I-2(V296)：无 inventory.cost.view 权限的调用方，库存流水响应成本列脱敏置 null（店员端/终端不可见）。
    // ── P0-A/GAP-07 供应商联系人手机号展示脱敏（V339）。同上 cost 脱敏范式，放 controller 层。 ──
    private InventorySupplierRecord maskSupplierPhoneForUnauthorized(InventorySupplierRecord record) {
        if (record == null || authorizationService.currentActorHasPermission(PhoneMask.VIEW_PLAINTEXT_PERMISSION)) {
            return record;
        }
        return record.withMaskedPhone();
    }

    private PageResponse<InventorySupplierRecord> maskSupplierPhonesForUnauthorized(
            PageResponse<InventorySupplierRecord> page) {
        if (authorizationService.currentActorHasPermission(PhoneMask.VIEW_PLAINTEXT_PERMISSION)) {
            return page;
        }
        return new PageResponse<>(page.items().stream().map(InventorySupplierRecord::withMaskedPhone).toList(),
                page.page(), page.pageSize(), page.total());
    }

    private StockMovementRecord maskCostForUnauthorized(StockMovementRecord record) {
        if (record == null || authorizationService.currentActorHasPermission("inventory.cost.view")) {
            return record;
        }
        return record.withoutCosts();
    }

    private PageResponse<StockMovementRecord> maskCostsForUnauthorized(PageResponse<StockMovementRecord> page) {
        if (authorizationService.currentActorHasPermission("inventory.cost.view")) {
            return page;
        }
        return new PageResponse<>(
                page.items().stream().map(StockMovementRecord::withoutCosts).toList(),
                page.page(), page.pageSize(), page.total());
    }

    @PostMapping("/stock-movements/{movement_type}")
    public ApiResponse<StockMovementRecord> createTypedStockMovement(
            @PathVariable("movement_type") String movementType,
            @RequestBody @jakarta.validation.Valid StockMovementRequest request
    ) {
        authorizationService.requirePermission("inventory.stock.move");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskCostForUnauthorized(service.createTypedStockMovement(movementType, request)));
    }

    @GetMapping("/suppliers")
    public ApiResponse<PageResponse<InventorySupplierRecord>> listInventorySuppliers() {
        authorizationService.requirePermission("inventory.supplier.view");
        return ApiResponse.ok(maskSupplierPhonesForUnauthorized(service.listInventorySuppliers()));
    }

    @PostMapping("/suppliers")
    public ApiResponse<InventorySupplierRecord> createInventorySupplier(@RequestBody @jakarta.validation.Valid InventorySupplierRequest request) {
        authorizationService.requirePermission("inventory.supplier.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskSupplierPhoneForUnauthorized(service.createInventorySupplier(request)));
    }

    // ── V277 跨主体流转:供货主体 + 流转单 ──
    @GetMapping("/supply-entities")
    public ApiResponse<PageResponse<SupplyEntityRecord>> listSupplyEntities() {
        return ApiResponse.ok(service.listSupplyEntities());
    }

    @PostMapping("/supply-entities")
    public ApiResponse<SupplyEntityRecord> createSupplyEntity(@RequestBody @jakarta.validation.Valid com.complex.platform.inventory.dto.SupplyEntityRequest request) {
        authorizationService.requirePermission("inventory.supply_entity.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createSupplyEntity(request));
    }

    @GetMapping("/transfer-orders")
    public ApiResponse<PageResponse<InventoryTransferOrderRecord>> listTransferOrders() {
        return ApiResponse.ok(service.listTransferOrders());
    }

    @GetMapping("/transfer-orders/{order_id}")
    public ApiResponse<InventoryTransferOrderRecord> getTransferOrder(@PathVariable("order_id") String orderId) {
        return ApiResponse.ok(service.getTransferOrder(orderId));
    }

    @PostMapping("/transfer-orders")
    public ApiResponse<InventoryTransferOrderRecord> createTransferOrder(@RequestBody @jakarta.validation.Valid com.complex.platform.inventory.dto.CreateTransferOrderRequest request) {
        authorizationService.requirePermission("inventory.transfer_order.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createTransferOrder(request));
    }

    @PostMapping("/transfer-orders/{order_id}/ship")
    public ApiResponse<InventoryTransferOrderRecord> shipTransferOrder(@PathVariable("order_id") String orderId) {
        authorizationService.requirePermission("inventory.transfer_order.manage");
        return ApiResponse.ok(service.shipTransferOrder(orderId));
    }

    @PostMapping("/transfer-orders/{order_id}/receive")
    public ApiResponse<InventoryTransferOrderRecord> receiveTransferOrder(
            @PathVariable("order_id") String orderId,
            @RequestBody(required = false) com.complex.platform.inventory.dto.ReceiveTransferRequest request) {
        authorizationService.requirePermission("inventory.transfer_order.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.receiveTransferOrder(orderId, request));
    }

    // ── V277 借调归还 / 取消(AUD-002/003)：借用型收货后可归还翻回归属；created/shipped 可取消并冲销已挂应付。 ──
    @PostMapping("/transfer-orders/{order_id}/return")
    public ApiResponse<InventoryTransferOrderRecord> returnTransferOrder(
            @PathVariable("order_id") String orderId,
            @RequestBody(required = false) com.complex.platform.inventory.dto.ReceiveTransferRequest request) {
        authorizationService.requirePermission("inventory.transfer_order.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.returnTransferOrder(orderId, request));
    }

    @PostMapping("/transfer-orders/{order_id}/cancel")
    public ApiResponse<InventoryTransferOrderRecord> cancelTransferOrder(
            @PathVariable("order_id") String orderId,
            @RequestBody(required = false) com.complex.platform.inventory.dto.ReceiveTransferRequest request) {
        authorizationService.requirePermission("inventory.transfer_order.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.cancelTransferOrder(orderId, request));
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
