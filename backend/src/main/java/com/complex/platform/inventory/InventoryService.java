package com.complex.platform.inventory;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.inventory.dto.InventoryItemRequest;
import com.complex.platform.inventory.dto.InventorySupplierRequest;
import com.complex.platform.inventory.dto.InventoryRiskScanRequest;
import com.complex.platform.inventory.dto.InventoryStockBatchRequest;
import com.complex.platform.inventory.dto.PurchaseRequestCreate;
import com.complex.platform.inventory.dto.SavedBottleCancelRequest;
import com.complex.platform.inventory.dto.SavedBottleClaimRequest;
import com.complex.platform.inventory.dto.SavedBottleRequest;
import com.complex.platform.inventory.dto.StockAdjustmentRequest;
import com.complex.platform.inventory.dto.StockInRequest;
import com.complex.platform.inventory.dto.StockMovementRequest;
import com.complex.platform.inventory.dto.SupplierReconciliationRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final Set<String> STOCK_BATCH_STATUSES = Set.of("active", "consumed", "expired", "archived");
    private static final Set<String> SUPPLIER_RECONCILIATION_STATUSES = Set.of("pending", "matched", "difference_found");

    private final InventoryRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final InventoryStockMovementService stockMovementService;

    public InventoryService(
            InventoryRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.stockMovementService = new InventoryStockMovementService(
                repository, idempotencyService, auditEventService, authorizationService);
    }

    public PageResponse<InventoryItemRecord> listInventoryItems() {
        return readPage(
                "inventory.item.viewed",
                "inventory.item.viewed_failed",
                "inventory.stock.view",
                "inventory_item",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listInventoryItemsByMerchant)
                                .orElseGet(repository::listInventoryItems);
                }
        );
    }

    @Transactional
    public InventoryItemRecord createInventoryItem(InventoryItemRequest request) {
        return executeWrite(
                "createInventoryItem",
                request == null ? null : request.idempotencyKey(),
                request,
                InventoryItemRecord.class,
                "inventory.item.created",
                "inventory.item.created_failed",
                "inventory.stock.move",
                "inventory_item",
                () -> {
                    InventoryItemRequest actual = requireRequest(request);
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    Instant now = Instant.now();
                    InventoryItemRecord item = new InventoryItemRecord(
                            "item_" + slug(actual.idempotencyKey()),
                            null,
                            requireText(actual.itemName(), "item_name"),
                            requireText(actual.category(), "category"),
                            requireText(actual.unit(), "unit"),
                            null,
                            false,
                            nonNegativeOrDefault(actual.minQuantity(), 0, "min_quantity"),
                            trimToDefault(actual.status(), "active"),
                            now,
                            now,
                            merchantId
                    );
                    return repository.saveInventoryItem(item);
                }
        );
    }

    public PageResponse<PurchaseRequestRecord> listPurchaseRequests() {
        return readPage(
                "inventory.purchase_request_viewed",
                "inventory.purchase_request_view_failed",
                "inventory.purchase_request.view",
                "inventory_purchase_request",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listPurchaseRequestsByMerchant)
                                .orElseGet(repository::listPurchaseRequests);
                }
        );
    }

    @Transactional
    public PurchaseRequestRecord createPurchaseRequest(PurchaseRequestCreate request) {
        return executeWrite(
                "createPurchaseRequest",
                request == null ? null : request.idempotencyKey(),
                request,
                PurchaseRequestRecord.class,
                "inventory.purchase_request_changed",
                "inventory.purchase_request_change_failed",
                "inventory.purchase_request.manage",
                "inventory_purchase_request",
                () -> {
                    PurchaseRequestCreate actual = requireRequest(request);
                    Map<String, Object> item = firstItem(actual.items(), "items");
                    String itemId = requireItemText(item, "item_id");
                    int quantity = requirePositiveItemQuantity(item, "quantity");
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    Instant now = Instant.now();
                    String slug = slug(actual.idempotencyKey());
                    return repository.savePurchaseRequest(new PurchaseRequestRecord(
                            "purchase_" + slug,
                            "PR" + now.toEpochMilli() + slug.substring(0, Math.min(6, slug.length())).toUpperCase(Locale.ROOT),
                            trimToNull(actual.supplierId()),
                            itemId,
                            quantity,
                            actual.expectedArrivalDate() == null ? LocalDate.now() : actual.expectedArrivalDate(),
                            "created",
                            safeList(actual.evidenceAttachmentIds()),
                            now,
                            now,
                            merchantId
                    ));
                }
        );
    }

    public PageResponse<SavedBottleRecord> listSavedBottles() {
        return readPage(
                "bar.saved_bottle.viewed",
                "bar.saved_bottle.view_failed",
                "bar.saved_bottle.manage",
                "saved_bottle",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listSavedBottlesByMerchant)
                                .orElseGet(repository::listSavedBottles);
                }
        );
    }

    @Transactional
    public SavedBottleRecord createSavedBottle(SavedBottleRequest request) {
        return executeWrite(
                "createSavedBottle",
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleRecord.class,
                "bar.saved_bottle.created",
                "bar.saved_bottle.create_failed",
                "bar.saved_bottle.manage",
                "saved_bottle",
                () -> {
                    SavedBottleRequest actual = requireRequest(request);
                    Map<String, Object> item = firstItem(actual.items(), "items");
                    String itemId = requireItemText(item, "item_id");
                    int quantity = requirePositiveItemQuantity(item, "quantity");
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    Instant now = Instant.now();
                    return repository.saveSavedBottle(new SavedBottleRecord(
                            "saved_" + slug(actual.idempotencyKey()),
                            requireText(actual.customerId(), "customer_id"),
                            requireText(actual.barId(), "bar_id"),
                            itemId,
                            trimToDefault(textFromItem(item, "bottle_name"), itemId),
                            quantity,
                            quantity,
                            trimToDefault(textFromItem(item, "unit"), "bottle"),
                            actual.expireAt(),
                            "stored",
                            safeList(actual.evidenceAttachmentIds()),
                            now,
                            now,
                            merchantId,
                            Boolean.TRUE.equals(actual.isHighValue()),
                            actual.highValueThresholdCents(),
                            trimToNull(actual.bottleLabelValue()),
                            trimToNull(actual.customerNameOrNickname()),
                            trimToNull(actual.customerPhone()),
                            phoneHash(actual.customerPhone())
                    ));
                }
        );
    }

    /** 取酒核身用：手机号 SHA-256 哈希（精确检索，不依赖明文解密）。null/空返回 null。 */
    private static String phoneHash(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(phone.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    /** 重建存酒记录（携带 A1 扩展字段），仅改 status/remaining/updatedAt。 */
    private SavedBottleRecord savedBottleWith(SavedBottleRecord b, String status, int remaining, Instant updatedAt) {
        return new SavedBottleRecord(b.id(), b.customerId(), b.barProjectId(), b.itemId(), b.bottleName(),
                b.initialQuantity(), remaining, b.unit(), b.expireAt(), status, b.evidenceAttachmentIds(),
                b.createdAt(), updatedAt, b.merchantId(), b.isHighValue(), b.highValueThresholdCents(),
                b.bottleLabelValue(), b.customerNameOrNickname(), b.customerPhone(), b.customerPhoneHash());
    }

    @Transactional
    public SavedBottleClaimRecord claimSavedBottle(String savedBottleId, SavedBottleClaimRequest request) {
        String id = requireText(savedBottleId, "saved_bottle_id");
        return executeWrite(
                "claimSavedBottle:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleClaimRecord.class,
                "bar.saved_bottle_picked_up",
                "bar.saved_bottle.pickup_failed",
                "bar.saved_bottle.manage",
                "saved_bottle_claim",
                () -> {
                    SavedBottleClaimRequest actual = requireRequest(request);
                    // P1-3: verificationCode 必须提供，不可绕过
                    requireText(actual.verificationCode(), "verification_code");
                    // P0-2: SELECT FOR UPDATE 防止并发超领
                    SavedBottleRecord savedBottle = repository.findSavedBottleByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "saved bottle not found"));
                    // P0-2: 跨商户越权检查
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    if (scope.isPresent() && !scope.get().equals(savedBottle.merchantId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                "saved bottle does not belong to your merchant");
                    }
                    if (!"stored".equals(savedBottle.status())) {
                        throw ApiException.conflict("SAVED_BOTTLE_NOT_CLAIMABLE", "saved bottle is not claimable");
                    }
                    if (savedBottle.expireAt() != null && savedBottle.expireAt().isBefore(Instant.now())) {
                        throw ApiException.conflict("SAVED_BOTTLE_EXPIRED", "saved bottle is expired");
                    }
                    // REQ-30 取酒核身：登记了瓶底标签值时，verificationCode 必须与之匹配；不一致进入人工复核（不出酒）。
                    Instant nowReview = Instant.now();
                    if (savedBottle.bottleLabelValue() != null
                            && !savedBottle.bottleLabelValue().equals(actual.verificationCode().trim())) {
                        repository.saveSavedBottle(savedBottleWith(
                                savedBottle, "pending_identity_review", savedBottle.remainingQuantity(), nowReview));
                        return repository.saveSavedBottleClaim(new SavedBottleClaimRecord(
                                "claim_" + slug(actual.idempotencyKey()),
                                savedBottle.id(),
                                0,
                                trimToNull(actual.claimByPhone()),
                                "staff_work_phone",
                                null,
                                "pending_identity_review",
                                safeList(actual.evidenceAttachmentIds()),
                                nowReview
                        ));
                    }
                    Map<String, Object> claimItem = firstItem(actual.claimItems(), "claim_items");
                    int quantity = requirePositiveItemQuantity(claimItem, "quantity");
                    if (quantity > savedBottle.remainingQuantity()) {
                        throw ApiException.conflict("SAVED_BOTTLE_INSUFFICIENT", "saved bottle remaining quantity is insufficient");
                    }
                    int remaining = savedBottle.remainingQuantity() - quantity;
                    Instant now = Instant.now();
                    repository.saveSavedBottle(savedBottleWith(
                            savedBottle, remaining == 0 ? "claimed" : "stored", remaining, now));
                    return repository.saveSavedBottleClaim(new SavedBottleClaimRecord(
                            "claim_" + slug(actual.idempotencyKey()),
                            savedBottle.id(),
                            quantity,
                            trimToNull(actual.claimByPhone()),
                            "staff_work_phone",
                            null,
                            "picked_up",
                            safeList(actual.evidenceAttachmentIds()),
                            now
                    ));
                }
        );
    }

    /** A1/REQ-32：存酒作废。高价值存酒必须提供照片+视频双证据（>=2 附件）方可作废（主管复核闭环为后续）。 */
    @Transactional
    public SavedBottleRecord cancelSavedBottle(String savedBottleId, SavedBottleCancelRequest request) {
        String id = requireText(savedBottleId, "saved_bottle_id");
        return executeWrite(
                "cancelSavedBottle:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleRecord.class,
                "bar.saved_bottle.cancelled",
                "bar.saved_bottle.cancel_failed",
                "bar.saved_bottle.manage",
                "saved_bottle",
                () -> {
                    SavedBottleCancelRequest actual = requireRequest(request);
                    SavedBottleRecord savedBottle = repository.findSavedBottleByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "saved bottle not found"));
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    if (scope.isPresent() && !scope.get().equals(savedBottle.merchantId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                "saved bottle does not belong to your merchant");
                    }
                    if (!List.of("stored", "pending_identity_review", "disputed_frozen").contains(savedBottle.status())) {
                        throw ApiException.conflict("SAVED_BOTTLE_NOT_CANCELLABLE", "saved bottle is not cancellable");
                    }
                    if (savedBottle.isHighValue() && safeList(actual.evidenceAttachmentIds()).size() < 2) {
                        throw ApiException.badRequest("HIGH_VALUE_EVIDENCE_REQUIRED",
                                "high value saved bottle void requires photo and video evidence (>=2 attachments) and supervisor review");
                    }
                    return repository.saveSavedBottle(
                            savedBottleWith(savedBottle, "cancelled", savedBottle.remainingQuantity(), Instant.now()));
                }
        );
    }

    @Transactional
    public StockAdjustmentRecord createStockAdjustment(StockAdjustmentRequest request) {
        return stockMovementService.createStockAdjustment(request);
    }

    public PageResponse<StockBalanceRecord> listStockBalances() {
        return readPage(
                "inventory.stock_balance.viewed",
                "inventory.stock_balance.viewed_failed",
                "inventory.stock.view",
                "inventory_stock_balance",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listStockBalancesByMerchant)
                                .orElseGet(repository::listStockBalances);
                }
        );
    }

    public PageResponse<InventoryStockBatchRecord> listStockBatches() {
        return readPage(
                "inventory.stock_batch.viewed",
                "inventory.stock_batch.view_failed",
                "inventory.stock.view",
                "inventory_stock_batch",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listStockBatchesByMerchant)
                                .orElseGet(repository::listStockBatches);
                }
        );
    }

    @Transactional
    public InventoryStockBatchRecord createStockBatch(InventoryStockBatchRequest request) {
        return executeWrite(
                "createInventoryStockBatch",
                request == null ? null : request.idempotencyKey(),
                request,
                InventoryStockBatchRecord.class,
                "inventory.stock_batch.recorded",
                "inventory.stock_batch.record_failed",
                "inventory.stock.batch.manage",
                "inventory_stock_batch",
                () -> createStockBatchOnce(request)
        );
    }

    public PageResponse<InventoryRiskAlertRecord> listRiskAlerts() {
        return readPage(
                "inventory.risk_alert.viewed",
                "inventory.risk_alert.view_failed",
                "inventory.risk_alert.view",
                "inventory_risk_alert",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listRiskAlertsByMerchant)
                                .orElseGet(repository::listRiskAlerts);
                }
        );
    }

    @Transactional
    public InventoryRiskScanResult runInventoryRiskScan(InventoryRiskScanRequest request) {
        return executeWrite(
                "runInventoryRiskScan",
                request == null ? null : request.idempotencyKey(),
                request,
                InventoryRiskScanResult.class,
                "inventory.risk_scan.completed",
                "inventory.risk_scan.failed",
                "inventory.risk.scan",
                "inventory_risk_scan",
                () -> scanInventoryRisks(request)
        );
    }

    public PageResponse<SupplierReconciliationRecord> listSupplierReconciliations() {
        return readPage(
                "inventory.supplier_reconciliation.viewed",
                "inventory.supplier_reconciliation.view_failed",
                "inventory.supplier.reconcile",
                "inventory_supplier_reconciliation",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listSupplierReconciliationsByMerchant)
                                .orElseGet(repository::listSupplierReconciliations);
                }
        );
    }

    @Transactional
    public SupplierReconciliationRecord createSupplierReconciliation(SupplierReconciliationRequest request) {
        return executeWrite(
                "createSupplierReconciliation",
                request == null ? null : request.idempotencyKey(),
                request,
                SupplierReconciliationRecord.class,
                "inventory.supplier_reconciliation.created",
                "inventory.supplier_reconciliation.create_failed",
                "inventory.supplier.reconcile",
                "inventory_supplier_reconciliation",
                () -> createSupplierReconciliationOnce(request)
        );
    }

    @Transactional
    public StockMovementRecord createStockIn(StockInRequest request) {
        return stockMovementService.createStockIn(request);
    }

    public PageResponse<StockMovementRecord> listStockMovements() {
        return stockMovementService.listStockMovements();
    }

    @Transactional
    public StockMovementRecord createStockMovement(StockMovementRequest request) {
        return stockMovementService.createStockMovement(request);
    }

    @Transactional
    public StockMovementRecord transferStockMovement(StockMovementRequest request) {
        return stockMovementService.transferStockMovement(request);
    }

    @Transactional
    public StockMovementRecord createTypedStockMovement(String movementType, StockMovementRequest request) {
        return stockMovementService.createTypedStockMovement(movementType, request);
    }

    public PageResponse<InventorySupplierRecord> listInventorySuppliers() {
        return readPage(
                "inventory.supplier_viewed",
                "inventory.supplier_view_failed",
                "inventory.supplier.view",
                "inventory_supplier",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listInventorySuppliersByMerchant)
                                .orElseGet(repository::listInventorySuppliers);
                }
        );
    }

    @Transactional
    public InventorySupplierRecord createInventorySupplier(InventorySupplierRequest request) {
        return executeWrite(
                "createInventorySupplier",
                request == null ? null : request.idempotencyKey(),
                request,
                InventorySupplierRecord.class,
                "inventory.supplier_changed",
                "inventory.supplier_change_failed",
                "inventory.supplier.manage",
                "inventory_supplier",
                () -> {
                    InventorySupplierRequest actual = requireRequest(request);
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    Instant now = Instant.now();
                    return repository.saveInventorySupplier(new InventorySupplierRecord(
                            "supplier_" + slug(actual.idempotencyKey()),
                            requireText(actual.supplierName(), "supplier_name"),
                            "general",
                            trimToNull(actual.contactName()),
                            trimToNull(actual.contactPhone()),
                            trimToDefault(actual.status(), "active"),
                            now,
                            now,
                            merchantId
                    ));
                }
        );
    }

    @Transactional
    public TraceHandoverRecord scanTraceCodeHandover(VerificationRequest request) {
        return executeWrite(
                "scanTraceCodeHandover",
                request == null ? null : request.idempotencyKey(),
                request,
                TraceHandoverRecord.class,
                "operation_handover.created",
                "operation_handover.failed",
                "inventory.stock.move",
                "inventory_trace_handover",
                () -> {
                    VerificationRequest actual = requireRequest(request);
                    Map<String, Object> context = actual.businessContext() == null ? Map.of() : actual.businessContext();
                    Instant now = Instant.now();
                    return repository.saveTraceHandover(new TraceHandoverRecord(
                            "trace_" + slug(actual.idempotencyKey()),
                            requireText(actual.credentialPayload(), "credential_payload"),
                            trimToNull(textFromMap(context, "item_id", "itemId")),
                            trimToNull(textFromMap(context, "from_staff_id", "fromStaffId")),
                            trimToNull(textFromMap(context, "to_staff_id", "toStaffId")),
                            requireText(actual.deviceId(), "device_id"),
                            "accepted",
                            List.of(),
                            now
                    ));
                }
        );
    }

    private InventoryStockBatchRecord createStockBatchOnce(InventoryStockBatchRequest request) {
        InventoryStockBatchRequest actual = requireRequest(request);
        String itemId = requireText(actual.itemId(), "item_id");
        repository.findInventoryItemById(itemId)
                .orElseThrow(() -> ApiException.notFound("INVENTORY_ITEM_NOT_FOUND", "inventory item not found"));
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        Instant now = Instant.now();
        return repository.saveStockBatch(new InventoryStockBatchRecord(
                "batch_" + slug(actual.idempotencyKey()),
                itemId,
                requireText(actual.locationId(), "location_id"),
                requireText(actual.batchNo(), "batch_no"),
                trimToNull(actual.supplierId()),
                requirePositiveInteger(actual.quantity(), "quantity"),
                actual.expireAt(),
                normalizeAllowed(trimToDefault(actual.status(), "active"), STOCK_BATCH_STATUSES, "status"),
                safeList(actual.evidenceAttachmentIds()),
                now,
                now,
                merchantId
        ));
    }

    private InventoryRiskScanResult scanInventoryRisks(InventoryRiskScanRequest request) {
        InventoryRiskScanRequest actual = requireRequest(request);
        requireText(actual.idempotencyKey(), "idempotency_key");
        // P1-5: operatorUserId 一律取自认证上下文，禁止来自请求体
        String operatorUserId = authorizationService.currentActor().id();
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        int expiryWarningDays = expiryWarningDays(actual.expiryWarningDays());
        Instant now = Instant.now();
        Instant expiryCutoff = now.plusSeconds(expiryWarningDays * 86_400L);

        // 使用租户隔离的列表
        List<InventoryItemRecord> items = merchantId != null
                ? repository.listInventoryItemsByMerchant(merchantId)
                : repository.listInventoryItems();
        List<StockBalanceRecord> balances = merchantId != null
                ? repository.listStockBalancesByMerchant(merchantId)
                : repository.listStockBalances();
        List<InventoryStockBatchRecord> batches = merchantId != null
                ? repository.listStockBatchesByMerchant(merchantId)
                : repository.listStockBatches();

        Map<String, InventoryItemRecord> itemsById = items.stream()
                .collect(java.util.stream.Collectors.toMap(InventoryItemRecord::id, item -> item, (left, right) -> left));
        java.util.ArrayList<InventoryRiskAlertRecord> alerts = new java.util.ArrayList<>();
        int index = 0;

        for (StockBalanceRecord balance : balances) {
            InventoryItemRecord item = itemsById.get(balance.itemId());
            int minQuantity = item == null ? 0 : item.minQuantity();
            if (balance.availableQuantity() <= 0) {
                alerts.add(riskAlert(actual.idempotencyKey(), index++, "out_of_stock", "high", balance.itemId(),
                        balance.locationId(), null, balance.availableQuantity(), minQuantity, null, operatorUserId, merchantId, now));
            } else if (minQuantity > 0 && balance.availableQuantity() < minQuantity) {
                alerts.add(riskAlert(actual.idempotencyKey(), index++, "safety_stock_low", "medium", balance.itemId(),
                        balance.locationId(), null, balance.availableQuantity(), minQuantity, null, operatorUserId, merchantId, now));
            }
        }

        for (InventoryStockBatchRecord batch : batches) {
            if (!"active".equals(batch.status()) || batch.expireAt() == null || batch.quantity() <= 0) {
                continue;
            }
            if (!batch.expireAt().isAfter(expiryCutoff)) {
                alerts.add(riskAlert(actual.idempotencyKey(), index++, "expiry_warning", "high", batch.itemId(),
                        batch.locationId(), batch.id(), batch.quantity(), null, batch.expireAt(), operatorUserId, merchantId, now));
            }
        }

        List<InventoryRiskAlertRecord> saved = alerts.stream()
                .map(repository::saveRiskAlert)
                .toList();
        return new InventoryRiskScanResult(saved.size(), saved);
    }

    private InventoryRiskAlertRecord riskAlert(
            String idempotencyKey,
            int index,
            String alertType,
            String severity,
            String itemId,
            String locationId,
            String batchId,
            Integer availableQuantity,
            Integer thresholdQuantity,
            Instant expireAt,
            String operatorUserId,
            String merchantId,
            Instant now
    ) {
        return new InventoryRiskAlertRecord(
                "risk_" + slug(idempotencyKey + ":" + index),
                alertType,
                severity,
                itemId,
                locationId,
                batchId,
                availableQuantity,
                thresholdQuantity,
                expireAt,
                "open",
                operatorUserId,
                now,
                merchantId
        );
    }

    private SupplierReconciliationRecord createSupplierReconciliationOnce(SupplierReconciliationRequest request) {
        SupplierReconciliationRequest actual = requireRequest(request);
        String supplierId = requireText(actual.supplierId(), "supplier_id");
        boolean supplierExists = repository.listInventorySuppliers().stream()
                .anyMatch(supplier -> supplier.id().equals(supplierId));
        if (!supplierExists) {
            throw ApiException.notFound("SUPPLIER_NOT_FOUND", "inventory supplier not found");
        }
        LocalDate periodStart = requireDate(actual.periodStart(), "period_start");
        LocalDate periodEnd = requireDate(actual.periodEnd(), "period_end");
        if (periodEnd.isBefore(periodStart)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "period_end must be on or after period_start");
        }
        long expectedAmountCent = requireNonNegativeLong(actual.expectedAmountCent(), "expected_amount_cent");
        long actualAmountCent = requireNonNegativeLong(actual.actualAmountCent(), "actual_amount_cent");
        // P1-5: operatorUserId 一律取自认证上下文，禁止来自请求体
        String operatorUserId = authorizationService.currentActor().id();
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        Instant now = Instant.now();
        return repository.saveSupplierReconciliation(new SupplierReconciliationRecord(
                "supplier_recon_" + slug(actual.idempotencyKey()),
                supplierId,
                periodStart,
                periodEnd,
                expectedAmountCent,
                actualAmountCent,
                actualAmountCent - expectedAmountCent,
                normalizeAllowed(trimToDefault(actual.status(), "pending"), SUPPLIER_RECONCILIATION_STATUSES, "status"),
                safeList(actual.evidenceAttachmentIds()),
                operatorUserId,
                now,
                now,
                merchantId
        ));
    }

    private <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<List<T>> supplier
    ) {
        try {
            List<T> items = supplier.get();
            auditEventService.recordSuccess(
                    successEvent,
                    permissionCode,
                    businessType,
                    "all",
                    null,
                    null,
                    Map.of("count", items.size())
            );
            return new PageResponse<>(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    "all",
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<T> action
    ) {
        try {
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    () -> {
                        T response = action.get();
                        auditEventService.recordSuccess(
                                successEvent,
                                permissionCode,
                                businessType,
                                businessId(response),
                                null,
                                idempotencyKey,
                                Map.of("operation_id", operationId)
                        );
                        return response;
                    },
                    responseType
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    "unknown",
                    null,
                    idempotencyKey,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private String businessId(Object response) {
        if (response instanceof InventoryItemRecord record) {
            return record.id();
        }
        if (response instanceof InventorySupplierRecord record) {
            return record.id();
        }
        if (response instanceof PurchaseRequestRecord record) {
            return record.id();
        }
        if (response instanceof SavedBottleRecord record) {
            return record.id();
        }
        if (response instanceof SavedBottleClaimRecord record) {
            return record.id();
        }
        if (response instanceof StockMovementRecord record) {
            return record.id();
        }
        if (response instanceof StockAdjustmentRecord record) {
            return record.id();
        }
        if (response instanceof InventoryStockBatchRecord record) {
            return record.id();
        }
        if (response instanceof InventoryRiskScanResult) {
            return "inventory_risk_scan";
        }
        if (response instanceof InventoryRiskAlertRecord record) {
            return record.id();
        }
        if (response instanceof SupplierReconciliationRecord record) {
            return record.id();
        }
        if (response instanceof TraceHandoverRecord record) {
            return record.id();
        }
        return "unknown";
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private Map<String, Object> firstItem(List<Map<String, Object>> items, String fieldName) {
        return requireItems(items, fieldName).getFirst();
    }

    private List<Map<String, Object>> requireItems(List<Map<String, Object>> items, String fieldName) {
        if (items == null || items.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return items;
    }

    private String requireItemText(Map<String, Object> item, String fieldName) {
        return requireText(textFromItem(item, fieldName), fieldName);
    }

    private String textFromItem(Map<String, Object> item, String fieldName) {
        return textFromMap(item, fieldName, snakeToCamel(fieldName));
    }

    private String textFromMap(Map<String, Object> map, String snakeName, String camelName) {
        Object value = map.get(snakeName);
        if (value == null) {
            value = map.get(camelName);
        }
        return value == null ? null : value.toString();
    }

    private int requirePositiveItemQuantity(Map<String, Object> item, String fieldName) {
        String raw = textFromItem(item, fieldName);
        if (raw == null || !raw.matches("[0-9]+")) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be a positive integer");
        }
        try {
            int quantity = Integer.parseInt(raw);
            if (quantity <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be positive");
            }
            return quantity;
        } catch (NumberFormatException exception) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is out of range");
        }
    }

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private int nonNegativeOrDefault(Integer value, int defaultValue, String fieldName) {
        int actual = value == null ? defaultValue : value;
        if (actual < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
        return actual;
    }

    private int requirePositiveInteger(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be positive");
        }
        return value;
    }

    private long requireNonNegativeLong(Long value, String fieldName) {
        if (value == null || value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
        return value;
    }

    private LocalDate requireDate(LocalDate value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return value;
    }

    private String normalizeAllowed(String value, Set<String> allowed, String fieldName) {
        String normalized = requireText(value, fieldName).toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return normalized;
    }

    private int expiryWarningDays(Integer value) {
        int actual = value == null ? 7 : value;
        if (actual < 1 || actual > 365) {
            throw ApiException.badRequest("VALIDATION_FAILED", "expiry_warning_days must be between 1 and 365");
        }
        return actual;
    }

    private List<String> safeList(List<String> value) {
        return value == null ? List.of() : List.copyOf(value);
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String snakeToCamel(String value) {
        StringBuilder builder = new StringBuilder();
        boolean upperNext = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '_') {
                upperNext = true;
            } else if (upperNext) {
                builder.append(Character.toUpperCase(character));
                upperNext = false;
            } else {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    private String slug(String seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(requireText(seed, "idempotency_key").getBytes(StandardCharsets.UTF_8)))
                    .substring(0, 16);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("slug generation unavailable", exception);
        }
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return "INTERNAL_ERROR";
    }
}
