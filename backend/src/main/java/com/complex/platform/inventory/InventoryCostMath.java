package com.complex.platform.inventory;

import java.time.Instant;

/**
 * V296 移动加权平均成本纯算法（无状态、无依赖，可纯内存单测）。
 *
 * <p>口径：
 * <ul>
 *   <li>方向由流水行自证：仅目标库位=入库；仅源库位=出库；两者都有=调拨（记参考成本、不改状态）。</li>
 *   <li>入库带进货单价 → 建立/滚动成本基准；不带单价 → 有基准按当前均价计入（均价不变），
 *       无基准只滚动数量、成本列 NULL（不编造）。</li>
 *   <li>出库成本按状态行总成本比例扣转（数量清零时总成本精确清零，避免舍入漂移）；无基准 → NULL。</li>
 *   <li>出库数量超过状态行持有量（存量过渡期）只扣转持有部分的总成本，持有数量落 0 不为负。</li>
 * </ul>
 */
final class InventoryCostMath {

    enum Direction { INBOUND, OUTBOUND, TRANSFER }

    /** 一笔流水的成本落账结果：行成本列（可空=无成本基准）+ 更新后的状态行（null=状态不变）。 */
    record Result(Long unitCostCent, Long totalCostCent, InventoryItemCostStateRecord newState) {
    }

    private InventoryCostMath() {
    }

    static Direction direction(String sourceLocationId, String targetLocationId) {
        if (sourceLocationId != null && targetLocationId == null) {
            return Direction.OUTBOUND;
        }
        if (sourceLocationId == null && targetLocationId != null) {
            return Direction.INBOUND;
        }
        // 双库位=调拨；双空为异常数据，按“不改状态”兜底处理。
        return Direction.TRANSFER;
    }

    static Result apply(
            InventoryItemCostStateRecord state,
            Direction direction,
            int quantity,
            Long inboundUnitCostCent,
            Instant now
    ) {
        long qty = quantity;
        switch (direction) {
            case INBOUND -> {
                if (inboundUnitCostCent != null) {
                    // 带进货单价：建立/滚动成本基准。
                    long rowTotal = inboundUnitCostCent * qty;
                    long newQty = state.onHandQuantity() + qty;
                    long newTotal = state.totalCostCent() + rowTotal;
                    return new Result(inboundUnitCostCent, rowTotal, withRolled(
                            state, newQty, newTotal, state.cumulativeInCostCent() + rowTotal,
                            state.cumulativeOutCostCent(), true, now));
                }
                if (state.hasCostBasis()) {
                    // 无价入库（如盘盈）：按当前均价计入，均价不变。
                    long rowTotal = proportional(state.totalCostCent(), state.onHandQuantity(), qty);
                    long newQty = state.onHandQuantity() + qty;
                    long newTotal = state.totalCostCent() + rowTotal;
                    return new Result(perUnit(rowTotal, qty), rowTotal, withRolled(
                            state, newQty, newTotal, state.cumulativeInCostCent() + rowTotal,
                            state.cumulativeOutCostCent(), true, now));
                }
                // 无基准：只滚动数量，成本列 NULL。
                return new Result(null, null, withRolled(
                        state, state.onHandQuantity() + qty, 0L,
                        state.cumulativeInCostCent(), state.cumulativeOutCostCent(), false, now));
            }
            case OUTBOUND -> {
                if (!state.hasCostBasis()) {
                    // 无基准：数量仍滚动（不为负），成本列 NULL，不阻断出库。
                    return new Result(null, null, withRolled(
                            state, Math.max(0L, state.onHandQuantity() - qty), 0L,
                            state.cumulativeInCostCent(), state.cumulativeOutCostCent(), false, now));
                }
                long consume = Math.min(qty, state.onHandQuantity());
                // 全量出清时精确扣转全部总成本，杜绝舍入残值。
                long rowTotal = consume == state.onHandQuantity()
                        ? state.totalCostCent()
                        : proportional(state.totalCostCent(), state.onHandQuantity(), consume);
                long newQty = state.onHandQuantity() - consume;
                long newTotal = state.totalCostCent() - rowTotal;
                return new Result(perUnit(rowTotal, qty), rowTotal, withRolled(
                        state, newQty, newTotal, state.cumulativeInCostCent(),
                        state.cumulativeOutCostCent() + rowTotal, true, now));
            }
            default -> {
                // 调拨：记参考成本（按当前均价折算），不改状态行。
                if (!state.hasCostBasis()) {
                    return new Result(null, null, null);
                }
                long rowTotal = proportional(state.totalCostCent(), state.onHandQuantity(), qty);
                return new Result(perUnit(rowTotal, qty), rowTotal, null);
            }
        }
    }

    /** 按持有总成本比例折算 qty 份的成本；持有量为 0 时按 0（有基准但已出清）。 */
    private static long proportional(long totalCostCent, long onHandQuantity, long qty) {
        if (onHandQuantity <= 0) {
            return 0L;
        }
        return Math.round((double) totalCostCent * qty / onHandQuantity);
    }

    private static Long perUnit(long rowTotal, long qty) {
        return qty <= 0 ? 0L : Math.round((double) rowTotal / qty);
    }

    private static InventoryItemCostStateRecord withRolled(
            InventoryItemCostStateRecord state,
            long onHandQuantity,
            long totalCostCent,
            long cumulativeIn,
            long cumulativeOut,
            boolean hasCostBasis,
            Instant now
    ) {
        long avg = onHandQuantity > 0 ? Math.round((double) totalCostCent / onHandQuantity) : 0L;
        return new InventoryItemCostStateRecord(
                state.itemId(), state.merchantId(), onHandQuantity, totalCostCent, avg,
                cumulativeIn, cumulativeOut, state.costMethod(), hasCostBasis, now);
    }
}
