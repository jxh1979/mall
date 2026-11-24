package com.complex.platform.inventory;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.inventory.dto.CloseStocktakeRequest;
import java.time.Instant;
import java.util.Map;

/**
 * 第十八批 BK 段修复项 F01：结束盘点。
 *
 * <p>建盘点单后该库房的入库、采购到货、要货收货等库存变动被盘点冻结闸拦下（409 INVENTORY_WAREHOUSE_STOCKTAKE_LOCKED
 * 「仓库正在盘点中，盘点结束前库存变动已冻结」），此前没有任何把盘点单改离盘点中的入口，库房永久冻结。BSM:1164「记录入库、出库、调拨、
 * 盘点、报损、报溢和库存调整。」——入库、出库、调拨是日常动作。本类把盘点单置已关闭（closed），冻结闸随之放行。
 *
 * <p>规则：按盘点单范围闸核归属（第十八批 BK 段第 384 条 384-R1，{@link InventoryProjectScope#requireStocktake}；在幂等之前，同号重放也先过）；已关闭的不能再结束；还有待复核的差异时不许结束；成功与失败都留痕。
 * 由 {@link InventoryService#closeStocktake} 委托调用（能力闸与事务在那一层）。
 */
final class StocktakeCloseService {

    private static final String PERMISSION = "inventory.stocktake.create";

    private final InventoryRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final InventoryProjectScope scope;

    StocktakeCloseService(
            InventoryRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            InventoryProjectScope scope
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.scope = scope;
    }

    StocktakeRecord close(String stocktakeId, CloseStocktakeRequest request) {
        String id = requireText(stocktakeId, "stocktake_id");
        String idempotencyKey = request == null ? null : request.idempotencyKey();
        String operationId = "closeStocktake:" + id;
        try {
            // 384-R1：商户账号只限本商户的，不挂商户的账号按建单码可触及盘点库房所在综合体。
            StocktakeRecord stocktake = scope.requireStocktake(id, PERMISSION);
            return idempotencyService.execute(operationId, idempotencyKey, request, () -> {
                if ("closed".equals(stocktake.status())) {
                    throw ApiException.conflict("STOCKTAKE_NOT_OPEN", "该盘点单已经结束");
                }
                if (repository.listInventoryVariancesByStocktake(id).stream()
                        .anyMatch(variance -> "pending".equals(variance.reviewStatus()))) {
                    throw ApiException.conflict("STOCKTAKE_HAS_PENDING_VARIANCE", "该盘点单还有待复核的差异，复核完才能结束盘点");
                }
                StocktakeRecord closed = repository.saveStocktake(new StocktakeRecord(
                        stocktake.id(), stocktake.stocktakeNo(), stocktake.warehouseId(), stocktake.merchantId(),
                        stocktake.businessDate(), "closed", stocktake.remark(), stocktake.createdAt(), Instant.now()));
                auditEventService.recordSuccess("inventory.stocktake.closed", PERMISSION, "stocktake", id, null, idempotencyKey,
                        Map.of("operation_id", operationId, "warehouse_id", stocktake.warehouseId()));
                return closed;
            }, StocktakeRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("inventory.stocktake.close_failed", PERMISSION, "stocktake", id, null, idempotencyKey,
                    exception instanceof ApiException apiException ? apiException.code() : "INTERNAL_ERROR");
            throw exception;
        }
    }
}
