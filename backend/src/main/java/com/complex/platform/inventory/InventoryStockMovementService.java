package com.complex.platform.inventory;

// 文件版本：V0.04

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.inventory.dto.StockAdjustmentRequest;
import com.complex.platform.inventory.dto.StockInRequest;
import com.complex.platform.inventory.dto.StockMovementRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

final class InventoryStockMovementService {


    // P1-4: adjustmentType 白名单，防止任意字符串触发扣减
    private static final Set<String> ALLOWED_ADJUSTMENT_TYPES =
            Set.of("gain", "loss", "damage", "waste", "count_correction");

    // P1-2: movementType 白名单，明确拒绝 "inbound" 绕过入库审计
    // R3 P0-7: 新增 redeem_consume（积分兑换扣减），替代此前误用的 "loss" 调整
    // FS-018 R1: 报损族（damage/loss/waste）移出直接扣减白名单——该族由 Controller 转入 createStockAdjustment
    //（高价值判定 + 照片/视频双证据 + 主管复核批准后才扣库存），服务层不再有先扣库存的直接路径。
    private static final Set<String> ALLOWED_TYPED_MOVEMENT_TYPES =
            // 第十八批 M 段 112-R3：加 gift_claim_consume（礼物领取消耗，营销赠发的店内实物券核销），与积分兑换消耗分开记（BSM:1194）。
            Set.of("outbound", "redeem_consume", "gift_claim_consume");

    static final Set<String> TYPED_DAMAGE_FAMILY = Set.of("damage", "loss", "waste");

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
    private final HighValueDecisionService highValueDecisionService;

    InventoryStockMovementService(
            InventoryRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            InventoryHighValueReviewService highValueReviewService,
            HighValueDecisionService highValueDecisionService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.highValueReviewService = highValueReviewService;
        this.highValueDecisionService = highValueDecisionService;
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
                    // C1/C4：入口显式校验物料存在（404）+ 归属当前商户（跨商户 403），杜绝越权改库存/靠 DB FK 兜底成 500。
                    requireItemOwnership(itemId);
                    String locationId = requireItemText(item, "location_id");
                    // 盘点期冻结：盘点进行中（counting/reviewing）不得经调整单偷改库存基线（与 stock-in/movement 一致）。
                    requireNoActiveStocktake(locationId);
                    int quantity = requirePositiveItemQuantity(item, "quantity");
                    String adjustmentType = requireText(actual.adjustmentType(), "adjustment_type");
                    // P1-4: 白名单校验
                    if (!ALLOWED_ADJUSTMENT_TYPES.contains(adjustmentType)) {
                        throw ApiException.badRequest("INVALID_ADJUSTMENT_TYPE",
                                "调整类型「" + adjustmentType + "」不允许");
                    }
                    String reason = requireText(actual.reason(), "reason");
                    Long unitCostCents = nonNegativeOrNull(actual.unitCostCents(), "unit_cost_cents");
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    Instant now = Instant.now();
                    String adjustmentId = "adjust_" + slug(actual.idempotencyKey());

                    // 报损(damage_report)/盘点差异(stock_variance)高价值入口接入域内复核。
                    // V350 归一：命中判定（物品进货价 > 后台单一阈值 / 手工标记 / 阈值未配置 / 进货价未知）→
                    // 不直接扣减：建 pending 复核单（要求照片+视频双证据各非空）+ 调整置 pending_review（库存不变）。
                    // 主管 approve 后由 applyDecisionEffect 真正扣减并置 completed；reject 置 rejected（不扣）。
                    // OBS-13-02（BSM L1072「转库存之后的报废……必须走审批并保留操作记录」；第十四批第 4 条门店主管与财务审批）：
                    // 存酒库房里的报损族（报废）不论高低价值一律先审批；证据沿用既有报损口径——高价值才强制照片+视频。
                    String subjectType = HIGH_VALUE_SUBJECT_BY_TYPE.get(adjustmentType);
                    boolean savedBottleDisposal = TYPED_DAMAGE_FAMILY.contains(adjustmentType)
                            && repository.isSavedBottleWarehouse(locationId);
                    boolean highValue = subjectType != null && isHighValue(itemId, merchantId);
                    if (highValue || savedBottleDisposal) {
                        StockAdjustmentRecord pending = repository.saveStockAdjustment(new StockAdjustmentRecord(
                                adjustmentId, itemId, locationId, adjustmentType, quantity, reason,
                                safeList(actual.evidenceAttachmentIds()), "pending_review", now, merchantId,
                                unitCostCents));
                        highValueReviewService.createPending(
                                actual.idempotencyKey(),
                                savedBottleDisposal ? InventoryHighValueReviewService.SUBJECT_SAVED_BOTTLE_STOCK_DISPOSAL : subjectType,
                                pending.id(),
                                actual.photoAttachmentIds(),
                                actual.videoAttachmentIds(),
                                highValue);
                        return pending;
                    }

                    // 非高价值：维持即时调整（向后兼容）。
                    if ("gain".equals(adjustmentType)) {
                        increaseBalance(itemId, locationId, quantity);
                    } else {
                        decreaseBalance(itemId, locationId, quantity);
                        consumeBatchesFefo(itemId, locationId, quantity);
                    }
                    // 裁决#7 守恒账本：即时调整改了余额，必须在同事务补写一条 inventory_stock_movements，
                    // 否则"按流水重建库存"对不平、内部人经调整单报损/盘溢无流水痕迹。
                    // movement_type = adjustmentType（gain/loss/damage/waste，DB 无 CHECK 约束，取值落在合理集合）；
                    // gain（增加）→ 目标库位入；loss/damage/waste（减少）→ 源库位出。
                    boolean adjustmentIncrease = "gain".equals(adjustmentType);
                    saveMovement(
                            "movement_" + slug(actual.idempotencyKey() + ":" + itemId + ":adjust"),
                            adjustmentType,
                            itemId,
                            adjustmentIncrease ? null : locationId,
                            adjustmentIncrease ? locationId : null,
                            quantity,
                            // V296：报溢(gain)=入库，携带申报单价滚动均价；减少类出库成本按均价自动计，不用申报价。
                            adjustmentIncrease ? unitCostCents : null,
                            "inventory_stock_adjustment",
                            adjustmentId,
                            null,
                            safeList(actual.evidenceAttachmentIds()),
                            merchantId
                    );
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
                    requireNoActiveStocktake(warehouseId);
                    // 2e 进货查验 C9：堵采购旁路——采购类入库必须走 PurchaseRequestService.receive（经供应商资质卡控），
                    // 不可经通用 stock-in 以 source_type=purchase 绕过资质/查验直接入库不合格品/无证货。
                    String inboundSourceType = requireText(actual.sourceType(), "source_type");
                    if (java.util.Set.of("purchase", "procurement", "purchase_request", "purchase_inbound")
                            .contains(inboundSourceType.toLowerCase(java.util.Locale.ROOT))) {
                        throw ApiException.badRequest("STOCK_IN_PURCHASE_BYPASS_FORBIDDEN",
                                "采购入库必须走采购申请收货流程（供应商资质闸），" + "直接入库不能使用采购来源类型");
                    }
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    StockMovementRecord last = null;
                    for (Map<String, Object> item : requireItems(actual.items(), "items")) {
                        String itemId = requireItemText(item, "item_id");
                        // C1/C4：入库前校验物料存在 + 归属当前商户。
                        requireItemOwnership(itemId);
                        int quantity = requirePositiveItemQuantity(item, "quantity");
                        increaseBalance(itemId, warehouseId, quantity);
                        createInboundBatch(actual.idempotencyKey(), itemId, warehouseId, quantity, merchantId,
                                parseOptionalExpireAt(item));
                        last = saveMovement(
                                "movement_" + slug(actual.idempotencyKey() + ":" + itemId + ":in"),
                                "inbound",
                                itemId,
                                null,
                                warehouseId,
                                quantity,
                                // V296：入库行可携带进货单价 item.unit_cost_cent（可空，兼容存量调用）。
                                parseOptionalUnitCostCent(item),
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
            // V296：采购入库进货单价（整数分，可空）——入库确认时录入，滚动移动加权均价。
            Long unitCostCent,
            List<String> evidenceAttachmentIds
    ) {
        // 盘点期冻结：盘点进行中（counting/reviewing）的库房/库位不得经采购到货入库污染盘点基线。
        requireNoActiveStocktake(warehouseId);
        requireNoActiveStocktake(locationId);
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        increaseBalance(itemId, locationId, quantity, warehouseId);
        // 采购到货批次：expire_at 暂由 PurchaseRequestService 侧不带（后续可由采购单行携带增强）。
        createInboundBatch(idempotencySeed, itemId, locationId, quantity, merchantId, null);
        return saveMovement(
                "movement_" + slug(idempotencySeed + ":" + itemId + ":purchase_in"),
                "inbound",
                itemId,
                null,
                locationId,
                quantity,
                unitCostCent,
                "purchase_request",
                purchaseRequestId,
                null,
                safeList(evidenceAttachmentIds),
                merchantId
        );
    }

    PageResponse<StockMovementRecord> listStockMovements(int page, int size) {
        return readPage(
                "inventory.stock_movement.viewed",
                "inventory.stock_movement.view_failed",
                "inventory.stock.view",
                "inventory_stock_movement",
                () -> {
                    // C2: merchant staff -> merchant; project staff (no merchant) -> project; platform admin -> all.
                    // AUD-013: 真分页在 SQL 层, 租户谓词经 nullable merchantId/projectId 下推。
                    java.util.Optional<String> merchantScope = authorizationService.currentMerchantScope();
                    String merchantId = merchantScope.orElse(null);
                    String projectId = merchantId != null ? null
                            : authorizationService.currentProjectScopeQuietly().orElse(null);
                    return repository.listStockMovementsPaged(merchantId, projectId, page, size);
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
                    "变动类型「" + type + "」不允许通过此接口操作");
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
            requireNoActiveStocktake(sourceLocationId);
        }
        if (increaseTarget) {
            requireText(targetLocationId, "to_location_id");
            requireNoActiveStocktake(targetLocationId);
        }
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        // OBS-13-02（BSM L1072「转库存之后的报废、赠送、内部使用等处置必须走审批并保留操作记录」；第十四批第 4 条门店主管与财务审批）：
        // 存酒库房的存货只来自到期/放弃存酒转入，从这里出库（赠送、内部使用等）不直接扣减；调拨不是处置，照常。
        boolean savedBottleDisposal = decreaseSource && !increaseTarget && repository.isSavedBottleWarehouse(sourceLocationId);
        StockMovementRecord last = null;
        for (Map<String, Object> item : requireItems(actual.items(), "items")) {
            String itemId = requireItemText(item, "item_id");
            // C1/C4：出库/移库前校验物料存在 + 归属当前商户（跨商户扣减 403，不存在 404）。
            requireItemOwnership(itemId);
            int quantity = requirePositiveItemQuantity(item, "quantity");
            if (savedBottleDisposal) {
                last = requestSavedBottleStockDisposal(actual, movementType, itemId, sourceLocationId, quantity, merchantId);
                continue;
            }
            if (decreaseSource) {
                decreaseBalance(itemId, sourceLocationId, quantity);
                consumeBatchesFefo(itemId, sourceLocationId, quantity);
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
                    // V296：出库/调拨不携带单价，成本由移动加权均价自动定稿。
                    null,
                    "manual",
                    null,
                    trimToNull(actual.traceCode()),
                    safeList(actual.evidenceAttachmentIds()),
                    merchantId
            );
        }
        return last;
    }

    /**
     * 第十八批 M 段第 117 条（BSM:1380「用料必须从指定库房出库并按当前平均进货价计成本」）：工单登记用料即从所选库位出库一次，流水挂工单
     * （business_type/business_id），成本由仓储层按移动加权均价定稿；幂等与审计由工单侧做。调用方已限平台口径（无商户归属），出库的是平台库房，
     * 物料与余额不按商户范围过滤（商户范围的 requireItemOwnership/decreaseBalance 对无商户归属的项目员工会 403）。
     */
    StockMovementRecord issueForWorkOrder(String businessType, String workOrderId, String locationId, String itemId,
            Integer quantity, String idempotencyKey) {
        requireText(locationId, "location_id");
        requireNoActiveStocktake(locationId);
        repository.findInventoryItemById(requireText(itemId, "item_id"))
                .orElseThrow(() -> ApiException.notFound("INVENTORY_ITEM_NOT_FOUND", "库存商品不存在或已被删除"));
        if (quantity == null || quantity <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "quantity 必须为正整数");
        }
        if (repository.atomicDecreaseStockBalance(itemId, locationId, quantity, null) == 0) {
            throw ApiException.conflict("STOCK_INSUFFICIENT", "可用库存不足");
        }
        consumeBatchesFefo(itemId, locationId, quantity);
        return saveMovement("movement_" + slug(idempotencyKey + ":" + itemId + ":" + businessType), "outbound", itemId, locationId,
                null, quantity, null, businessType, workOrderId, null, List.of(), null);
    }

    /**
     * 存酒库房出库待审：落 pending_review 调整单（类型取本次出库类型、原因取 operator_note 必填）＋存酒库房处置复核单，库存不动；
     * 门店主管与财务都批后由复核生效回调扣减并写同类型流水，驳回不扣。返回的流水记录状态为 pending_review（尚未入账，仅回显）。
     */
    private StockMovementRecord requestSavedBottleStockDisposal(
            StockMovementRequest actual, String movementType, String itemId, String locationId, int quantity, String merchantId) {
        StockAdjustmentRecord pending = repository.saveStockAdjustment(new StockAdjustmentRecord(
                "adjust_" + slug(actual.idempotencyKey() + ":" + itemId + ":" + movementType), itemId, locationId, movementType,
                quantity, requireText(actual.operatorNote(), "operator_note"), safeList(actual.evidenceAttachmentIds()),
                "pending_review", Instant.now(), merchantId, null));
        highValueReviewService.createPending(actual.idempotencyKey(),
                InventoryHighValueReviewService.SUBJECT_SAVED_BOTTLE_STOCK_DISPOSAL, pending.id(), List.of(), List.of(), false);
        return new StockMovementRecord("movement_" + slug(pending.id() + ":hv_approve"), movementType, itemId, locationId, null,
                quantity, null, null, "inventory_stock_adjustment", pending.id(), trimToNull(actual.traceCode()), null,
                pending.evidenceAttachmentIds(), "pending_review", pending.createdAt(), merchantId);
    }

    private StockMovementRecord saveMovement(
            String id,
            String movementType,
            String itemId,
            String sourceLocationId,
            String targetLocationId,
            int quantity,
            // V296：入库进货单价（整数分，可空）。出库/调拨恒传 null，成本由仓储层按移动加权自动定稿。
            Long unitCostCent,
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
                unitCostCent,
                null,
                businessType,
                businessId,
                traceCode,
                null,
                evidenceAttachmentIds,
                "completed",
                Instant.now(),
                merchantId
        ), authorizationService.currentProjectScopeQuietly().orElse(null));
    }

    // P0-1: 原子增加余额，消除 read-modify-write 竞态
    private void increaseBalance(String itemId, String locationId, int quantity) {
        increaseBalance(itemId, locationId, quantity, null);
    }

    // R5 T6：可携带结构化库房 warehouse_id（入库确认时挂库房；其余场景传 null）。
    private void increaseBalance(String itemId, String locationId, int quantity, String warehouseId) {
        String balanceId = "balance_" + slug(itemId + ":" + locationId);
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        // C2：余额行落归属 project_id（写入者项目 scope），供项目级读隔离。
        String projectId = authorizationService.currentProjectScopeQuietly().orElse(null);
        repository.atomicIncreaseStockBalance(itemId, locationId, quantity, balanceId, merchantId, warehouseId, projectId);
    }

    // P0-1: 原子减少余额，消除 read-modify-write 竞态
    private void decreaseBalance(String itemId, String locationId, int quantity) {
        String merchantScope = authorizationService.currentMerchantScope().orElse(null);
        int affected = repository.atomicDecreaseStockBalance(itemId, locationId, quantity, merchantScope);
        if (affected == 0) {
            throw ApiException.conflict("STOCK_INSUFFICIENT", "可用库存不足");
        }
    }

    // 2e：入库创建批次行（FEFO 扣减的数据基础，供过期/召回/先进先出追溯）。
    // expireAt 由入库请求携带（item.expire_at）→ 落 inventory_stock_batches.expire_at，使 FEFO（expire_at ASC NULLS LAST）真正生效；
    // 缺省 null 时退化按 created_at（FIFO）。supplier 仍缺省 null（后续增强）。
    private void createInboundBatch(String idSeed, String itemId, String locationId, int quantity,
            String merchantId, Instant expireAt) {
        Instant now = Instant.now();
        repository.saveStockBatch(new InventoryStockBatchRecord(
                "batch_" + slug(idSeed + ":" + itemId + ":" + locationId),
                itemId,
                locationId,
                "B" + now.toEpochMilli() + "-" + itemId,
                null,
                quantity,
                expireAt,
                "active",
                List.of(),
                now,
                now,
                merchantId), authorizationService.currentProjectScopeQuietly().orElse(null));
    }

    // V296：从入库 item map 读取可选进货单价 unit_cost_cent（整数分）；缺省 → null（无成本基准，不编造）；
    // 非法/负数 → 400（钱不允许静默吞坏值）。
    private Long parseOptionalUnitCostCent(Map<String, Object> item) {
        String raw = textFromItem(item, "unit_cost_cent");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            long value = Long.parseLong(raw.trim());
            if (value < 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "单位成本不能为负数");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw ApiException.badRequest("VALIDATION_FAILED", "单位成本必须为整数（单位：分）");
        }
    }

    // 从入库 item map 读取可选 expire_at（ISO-8601 instant，如 2026-12-31T00:00:00Z）；缺省/非法 → null（退化 FIFO）。
    private Instant parseOptionalExpireAt(Map<String, Object> item) {
        Object raw = item == null ? null : item.get("expire_at");
        if (raw == null || raw.toString().isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw.toString().trim());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    // 2e：出库 FEFO 逐批扣减批次 quantity（先到期先出，涵盖 FIFO）。balance 已权威原子扣减，批次为追溯层；
    // 历史库存无对应批次（过渡期）时 remaining>0 不阻断出库（balance 是权威，批次缺口仅影响追溯完整性）。
    private void consumeBatchesFefo(String itemId, String locationId, int quantity) {
        int remaining = quantity;
        // 有界重试：每轮按 FEFO 重取活跃批次逐批 CAS 扣减；某批 CAS 失败（并发被改）则下一轮重取最新值再扣，
        // 避免并发下"扣一遍就放弃"导致批次少扣、与 balance 漂移。本轮无任何成功扣减（无可消费批次）即停。
        for (int attempt = 0; attempt < 5 && remaining > 0; attempt++) {
            boolean progressed = false;
            for (InventoryStockBatchRecord batch : repository.listActiveBatchesForConsumption(itemId, locationId)) {
                if (remaining <= 0) {
                    break;
                }
                int take = Math.min(remaining, batch.quantity());
                if (take > 0 && repository.atomicDecreaseBatchQuantity(batch.id(), take) > 0) {
                    remaining -= take;
                    progressed = true;
                }
            }
            if (!progressed) {
                break;
            }
        }
        // remaining>0 仅当无足够 active 批次（历史库存过渡期/批次缺口）：balance 已权威原子扣减，
        // 批次为追溯辅助层，不阻断出库（既有裁定：balance 权威）。
    }

    // 2e 盘点冻结：库房存在未关闭盘点单（counting/reviewing）时该库房出入库冻结，避免盘点期间库存变动致差异基线失真。
    // 参数为出入库 location/warehouse 标识（库房级库存 location 即 warehouse_id；子库位/消费点不命中库房盘点，不误冻结）。
    private void requireNoActiveStocktake(String warehouseOrLocationId) {
        if (warehouseOrLocationId != null && repository.hasActiveStocktakeForWarehouse(warehouseOrLocationId)) {
            throw ApiException.conflict("INVENTORY_WAREHOUSE_STOCKTAKE_LOCKED",
                    "仓库正在盘点中，盘点结束前库存变动已冻结");
        }
    }

    private <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<PageResponse<T>> supplier
    ) {
        try {
            PageResponse<T> page = supplier.get();
            auditEventService.recordSuccess(
                    successEvent,
                    permissionCode,
                    businessType,
                    "all",
                    null,
                    null,
                    Map.of("count", page.total())
            );
            return page;
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
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
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
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 必须为正整数");
        }
        try {
            int quantity = Integer.parseInt(raw);
            if (quantity <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 必须大于 0");
            }
            return quantity;
        } catch (NumberFormatException exception) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 超出允许范围");
        }
    }

    /** 可空非负长整：null 透传（如调整单价 null=不视高价值），非空须 >= 0。 */
    private Long nonNegativeOrNull(Long value, String fieldName) {
        if (value != null && value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " 不能为负数");
        }
        return value;
    }

    /**
     * V350 归一：报损/盘点差异的高价值判定改走全平台唯一入口
     * {@link HighValueDecisionService#decide}（物品进货价 vs 后台单一阈值，或手工标记）。
     *
     * <p>改造前是「数量 × 申报单价 ≥ 物料自带阈值」，且**物料没录阈值就直接放行**——
     * 等于「不给物料录阈值」就能绕开主管复核。现在阈值未配置/进货价未知一律按高价值拦下。
     */
    private boolean isHighValue(String itemId, String merchantId) {
        return highValueDecisionService.decide(itemId, merchantId).highValue();
    }

    /**
     * C1/C4 入口归属校验：物料必须存在（否则 404 INVENTORY_ITEM_NOT_FOUND，替代靠 DB FK 兜底的 500），
     * 且在有商户 scope 时必须归属当前商户（否则 403 CROSS_MERCHANT_FORBIDDEN，杜绝跨商户改他人库存）。
     * 平台/项目管理员（空 scope）不做归属限制（放行全量），与既有 loadXxxScoped 口径一致。
     */
    private void requireItemOwnership(String itemId) {
        InventoryItemRecord item = repository.findInventoryItemById(itemId)
                .orElseThrow(() -> ApiException.notFound(
                        "INVENTORY_ITEM_NOT_FOUND", "库存商品不存在或已被删除"));
        authorizationService.currentMerchantScope().ifPresent(scope -> {
            if (!scope.equals(item.merchantId())) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                        "该库存商品不属于当前商户");
            }
        });
    }


    private List<String> safeList(List<String> value) {
        return value == null ? List.of() : List.copyOf(value);
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
        return com.complex.platform.common.Hashing.idempotencyCode(requireText(seed, "idempotency_key"));
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return "INTERNAL_ERROR";
    }
}
