package com.complex.platform.inventory;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.inventory.dto.StockAdjustmentRequest;
import com.complex.platform.inventory.dto.StockInRequest;
import com.complex.platform.inventory.dto.StockMovementRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

final class InventoryStockMovementService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;

    // P1-4: adjustmentType 白名单，防止任意字符串触发扣减
    private static final Set<String> ALLOWED_ADJUSTMENT_TYPES =
            Set.of("gain", "loss", "damage", "waste", "count_correction");

    // P1-2: movementType 白名单，明确拒绝 "inbound" 绕过入库审计
    // R3 P0-7: 新增 redeem_consume（积分兑换扣减），替代此前误用的 "loss" 调整
    private static final Set<String> ALLOWED_TYPED_MOVEMENT_TYPES =
            Set.of("outbound", "damage", "waste", "loss", "redeem_consume");

    // R5 T6-4：报损/盘点差异高价值判定——调整类型到复核 subject_type 的映射。
    // 报损族（loss/damage/waste）→ damage_report；盘点差异（count_correction）→ stock_variance；
    // gain（报溢/增加库存）不视为高价值（不存在价值损失，维持即时调整）。
    private static final Map<String, String> HIGH_VALUE_SUBJECT_BY_TYPE = Map.of(
            "loss", InventoryHighValueReviewService.SUBJECT_DAMAGE_REPORT,
            "damage", InventoryHighValueReviewService.SUBJECT_DAMAGE_REPORT,
            "waste", InventoryHighValueReviewService.SUBJECT_DAMAGE_REPORT,
            "count_correction", InventoryHighValueReviewService.SUBJECT_STOCK_VARIANCE);

    private final InventoryRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final InventoryHighValueReviewService highValueReviewService;

    InventoryStockMovementService(
            InventoryRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            InventoryHighValueReviewService highValueReviewService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.highValueReviewService = highValueReviewService;
    }

    StockAdjustmentRecord createStockAdjustment(StockAdjustmentRequest request) {
        return executeWrite(
                "createStockAdjustment",
                request == null ? null : request.idempotencyKey(),
                request,
                StockAdjustmentRecord.class,
                "inventory.counted",
                "inventory.adjustment_failed",
                "inventory.stock.adjust",
                "inventory_stock_adjustment",
                () -> {
                    StockAdjustmentRequest actual = requireRequest(request);
                    Map<String, Object> item = firstItem(actual.items(), "items");
                    String itemId = requireItemText(item, "item_id");
                    String locationId = requireItemText(item, "location_id");
                    int quantity = requirePositiveItemQuantity(item, "quantity");
                    String adjustmentType = requireText(actual.adjustmentType(), "adjustment_type");
                    // P1-4: 白名单校验
                    if (!ALLOWED_ADJUSTMENT_TYPES.contains(adjustmentType)) {
                        throw ApiException.badRequest("INVALID_ADJUSTMENT_TYPE",
                                "adjustment_type '" + adjustmentType + "' is not allowed");
                    }
                    String reason = requireText(actual.reason(), "reason");
                    Long unitCostCents = nonNegativeOrNull(actual.unitCostCents(), "unit_cost_cents");
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    Instant now = Instant.now();
                    String adjustmentId = "adjust_" + slug(actual.idempotencyKey());

                    // R5 T6-4：报损(damage_report)/盘点差异(stock_variance)高价值入口接入域内复核。
                    // 命中高价值（quantity × unitCostCents ≥ 物料 high_value_threshold_cents，且二者均非空）→
                    // 不直接扣减：建 pending 复核单（要求照片+视频双证据各非空）+ 调整置 pending_review（库存不变）。
                    // 主管 approve 后由 applyDecisionEffect 真正扣减并置 completed；reject 置 rejected（不扣）。
                    String subjectType = HIGH_VALUE_SUBJECT_BY_TYPE.get(adjustmentType);
                    if (subjectType != null && isHighValue(itemId, quantity, unitCostCents)) {
                        StockAdjustmentRecord pending = repository.saveStockAdjustment(new StockAdjustmentRecord(
                                adjustmentId, itemId, locationId, adjustmentType, quantity, reason,
                                safeList(actual.evidenceAttachmentIds()), "pending_review", now, merchantId,
                                unitCostCents));
                        highValueReviewService.createPending(
                                actual.idempotencyKey(),
                                subjectType,
                                pending.id(),
                                actual.photoAttachmentIds(),
                                actual.videoAttachmentIds());
                        return pending;
                    }

                    // 非高价值：维持即时调整（向后兼容）。
                    if ("gain".equals(adjustmentType)) {
                        increaseBalance(itemId, locationId, quantity);
                    } else {
                        decreaseBalance(itemId, locationId, quantity);
                    }
                    return repository.saveStockAdjustment(new StockAdjustmentRecord(
                            adjustmentId,
                            itemId,
                            locationId,
                            adjustmentType,
                            quantity,
                            reason,
                            safeList(actual.evidenceAttachmentIds()),
                            "completed",
                            now,
                            merchantId,
                            unitCostCents
                    ));
                }
        );
    }

    StockMovementRecord createStockIn(StockInRequest request) {
        return executeWrite(
                "createStockIn",
                request == null ? null : request.idempotencyKey(),
                request,
                StockMovementRecord.class,
                "inventory.inbound_confirmed",
                "inventory.inbound_failed",
                "inventory.stock.move",
                "inventory_stock_movement",
                () -> {
                    StockInRequest actual = requireRequest(request);
                    String warehouseId = requireText(actual.warehouseId(), "warehouse_id");
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    StockMovementRecord last = null;
                    for (Map<String, Object> item : requireItems(actual.items(), "items")) {
                        String itemId = requireItemText(item, "item_id");
                        int quantity = requirePositiveItemQuantity(item, "quantity");
                        increaseBalance(itemId, warehouseId, quantity);
                        last = saveMovement(
                                "movement_" + slug(actual.idempotencyKey() + ":" + itemId + ":in"),
                                "inbound",
                                itemId,
                                null,
                                warehouseId,
                                quantity,
                                requireText(actual.sourceType(), "source_type"),
                                trimToNull(actual.sourceId()),
                                null,
                                safeList(actual.evidenceAttachmentIds()),
                                merchantId
                        );
                    }
                    return last;
                }
        );
    }

    /**
     * R5 T6 采购入库：采购单 received 时生成 inbound 入库流水。
     *
     * <p>原子增加余额（落 location 与可空 warehouse_id），并写一条 movement_type=inbound 的流水，
     * business_type=purchase_request、business_id=采购单 id 关联采购单。由调用方在事务内调用。
     *
     * @return 生成的入库流水记录
     */
    StockMovementRecord createPurchaseInbound(
            String idempotencySeed,
            String purchaseRequestId,
            String itemId,
            String locationId,
            String warehouseId,
            int quantity,
            List<String> evidenceAttachmentIds
    ) {
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        increaseBalance(itemId, locationId, quantity, warehouseId);
        return saveMovement(
                "movement_" + slug(idempotencySeed + ":" + itemId + ":purchase_in"),
                "inbound",
                itemId,
                null,
                locationId,
                quantity,
                "purchase_request",
                purchaseRequestId,
                null,
                safeList(evidenceAttachmentIds),
                merchantId
        );
    }

    PageResponse<StockMovementRecord> listStockMovements() {
        return readPage(
                "inventory.stock_movement.viewed",
                "inventory.stock_movement.view_failed",
                "inventory.stock.view",
                "inventory_stock_movement",
                () -> {
                    java.util.Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listStockMovementsByMerchant)
                                .orElseGet(repository::listStockMovements);
                }
        );
    }

    StockMovementRecord createStockMovement(StockMovementRequest request) {
        return executeWrite(
                "createStockMovement",
                request == null ? null : request.idempotencyKey(),
                request,
                StockMovementRecord.class,
                "inventory.outbound_verified",
                "inventory.stock_movement_failed",
                "inventory.stock.move",
                "inventory_stock_movement",
                () -> createMovement(request, "outbound", false, true)
        );
    }

    StockMovementRecord transferStockMovement(StockMovementRequest request) {
        return executeWrite(
                "transferStockMovement",
                request == null ? null : request.idempotencyKey(),
                request,
                StockMovementRecord.class,
                "inventory.stock_transfer.created",
                "inventory.stock_transfer.created_failed",
                "inventory.stock.move",
                "inventory_stock_movement",
                () -> createMovement(request, "transfer", true, true)
        );
    }

    StockMovementRecord createTypedStockMovement(String movementType, StockMovementRequest request) {
        String type = requireText(movementType, "movement_type");
        // P1-2: 白名单校验，明确拒绝 "inbound" 绕过入库审计
        if (!ALLOWED_TYPED_MOVEMENT_TYPES.contains(type)) {
            throw ApiException.badRequest("INVALID_MOVEMENT_TYPE",
                    "movement_type '" + type + "' is not allowed via this endpoint");
        }
        return executeWrite(
                "createTypedStockMovement:" + type,
                request == null ? null : request.idempotencyKey(),
                request,
                StockMovementRecord.class,
                "inventory.stock_movement.created",
                "inventory.stock_movement.created_failed",
                "inventory.stock.move",
                "inventory_stock_movement",
                () -> createMovement(request, type, false, true)
        );
    }

    private StockMovementRecord createMovement(
            StockMovementRequest request,
            String movementType,
            boolean increaseTarget,
            boolean decreaseSource
    ) {
        StockMovementRequest actual = requireRequest(request);
        String sourceLocationId = trimToNull(actual.fromLocationId());
        String targetLocationId = trimToNull(actual.toLocationId());
        if (decreaseSource) {
            requireText(sourceLocationId, "from_location_id");
        }
        if (increaseTarget) {
            requireText(targetLocationId, "to_location_id");
        }
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        StockMovementRecord last = null;
        for (Map<String, Object> item : requireItems(actual.items(), "items")) {
            String itemId = requireItemText(item, "item_id");
            int quantity = requirePositiveItemQuantity(item, "quantity");
            if (decreaseSource) {
                decreaseBalance(itemId, sourceLocationId, quantity);
            }
            if (increaseTarget) {
                increaseBalance(itemId, targetLocationId, quantity);
            }
            last = saveMovement(
                    "movement_" + slug(actual.idempotencyKey() + ":" + itemId + ":" + movementType),
                    movementType,
                    itemId,
                    sourceLocationId,
                    targetLocationId,
                    quantity,
                    "manual",
                    null,
                    trimToNull(actual.traceCode()),
                    safeList(actual.evidenceAttachmentIds()),
                    merchantId
            );
        }
        return last;
    }

    private StockMovementRecord saveMovement(
            String id,
            String movementType,
            String itemId,
            String sourceLocationId,
            String targetLocationId,
            int quantity,
            String businessType,
            String businessId,
            String traceCode,
            List<String> evidenceAttachmentIds,
            String merchantId
    ) {
        return repository.saveStockMovement(new StockMovementRecord(
                id,
                movementType,
                itemId,
                sourceLocationId,
                targetLocationId,
                quantity,
                businessType,
                businessId,
                traceCode,
                null,
                evidenceAttachmentIds,
                "completed",
                Instant.now(),
                merchantId
        ));
    }

    // P0-1: 原子增加余额，消除 read-modify-write 竞态
    private void increaseBalance(String itemId, String locationId, int quantity) {
        increaseBalance(itemId, locationId, quantity, null);
    }

    // R5 T6：可携带结构化库房 warehouse_id（入库确认时挂库房；其余场景传 null）。
    private void increaseBalance(String itemId, String locationId, int quantity, String warehouseId) {
        String balanceId = "balance_" + slug(itemId + ":" + locationId);
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        repository.atomicIncreaseStockBalance(itemId, locationId, quantity, balanceId, merchantId, warehouseId);
    }

    // P0-1: 原子减少余额，消除 read-modify-write 竞态
    private void decreaseBalance(String itemId, String locationId, int quantity) {
        int affected = repository.atomicDecreaseStockBalance(itemId, locationId, quantity);
        if (affected == 0) {
            throw ApiException.conflict("STOCK_INSUFFICIENT", "available stock is insufficient");
        }
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
        if (response instanceof StockMovementRecord record) {
            return record.id();
        }
        if (response instanceof StockAdjustmentRecord record) {
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

    /** 可空非负长整：null 透传（如调整单价 null=不视高价值），非空须 >= 0。 */
    private Long nonNegativeOrNull(Long value, String fieldName) {
        if (value != null && value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
        return value;
    }

    /**
     * R5 T6-4 高价值判定口径：调整金额（数量 × 单价）≥ 物料阈值 high_value_threshold_cents。
     *
     * <p>阈值或单价任一为空 → 不视为高价值（向后兼容，维持即时调整）。物料缺失亦不视高价值。
     */
    private boolean isHighValue(String itemId, int quantity, Long unitCostCents) {
        if (unitCostCents == null) {
            return false;
        }
        Long threshold = repository.findInventoryItemById(itemId)
                .map(InventoryItemRecord::highValueThresholdCents)
                .orElse(null);
        if (threshold == null) {
            return false;
        }
        return (long) quantity * unitCostCents >= threshold;
    }

    private List<String> safeList(List<String> value) {
        return value == null ? List.of() : List.copyOf(value);
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
