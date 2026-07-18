package com.complex.platform.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * V296 移动加权平均成本纯算法单测（无 Spring/无 DB，可随 test-compile 直接跑）。
 * 覆盖：建立基准/滚动均价/出库比例扣转/全量出清清零/无基准 NULL 不编造/调拨不改状态/无价入库按均价。
 */
class InventoryCostMathTests {

    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    private InventoryItemCostStateRecord empty() {
        return new InventoryItemCostStateRecord(
                "item_1", "m_1", 0L, 0L, 0L, 0L, 0L, "moving_average", false, NOW);
    }

    @Test
    void inboundWithPriceEstablishesBasisAndRollsAverage() {
        // 首次入库 10 件 @100 分 → 均价 100。
        InventoryCostMath.Result first = InventoryCostMath.apply(
                empty(), InventoryCostMath.Direction.INBOUND, 10, 100L, NOW);
        assertThat(first.unitCostCent()).isEqualTo(100L);
        assertThat(first.totalCostCent()).isEqualTo(1000L);
        assertThat(first.newState().hasCostBasis()).isTrue();
        assertThat(first.newState().onHandQuantity()).isEqualTo(10L);
        assertThat(first.newState().avgUnitCostCent()).isEqualTo(100L);

        // 再入 10 件 @200 分 → 移动加权均价 (1000+2000)/20 = 150。
        InventoryCostMath.Result second = InventoryCostMath.apply(
                first.newState(), InventoryCostMath.Direction.INBOUND, 10, 200L, NOW);
        assertThat(second.newState().onHandQuantity()).isEqualTo(20L);
        assertThat(second.newState().totalCostCent()).isEqualTo(3000L);
        assertThat(second.newState().avgUnitCostCent()).isEqualTo(150L);
        assertThat(second.newState().cumulativeInCostCent()).isEqualTo(3000L);
    }

    @Test
    void outboundConsumesAtMovingAverageProportionally() {
        InventoryItemCostStateRecord state = InventoryCostMath.apply(
                empty(), InventoryCostMath.Direction.INBOUND, 20, 150L, NOW).newState();
        // 出 5 件 → 成本 5×150=750，剩 15 件 2250。
        InventoryCostMath.Result out = InventoryCostMath.apply(
                state, InventoryCostMath.Direction.OUTBOUND, 5, null, NOW);
        assertThat(out.unitCostCent()).isEqualTo(150L);
        assertThat(out.totalCostCent()).isEqualTo(750L);
        assertThat(out.newState().onHandQuantity()).isEqualTo(15L);
        assertThat(out.newState().totalCostCent()).isEqualTo(2250L);
        assertThat(out.newState().cumulativeOutCostCent()).isEqualTo(750L);
    }

    @Test
    void fullOutboundZeroesTotalExactlyAvoidingRoundingResidue() {
        // 3 件总成本 100 分（均价有舍入）→ 全量出清必须精确清零。
        InventoryItemCostStateRecord state = new InventoryItemCostStateRecord(
                "item_1", "m_1", 3L, 100L, 33L, 100L, 0L, "moving_average", true, NOW);
        InventoryCostMath.Result out = InventoryCostMath.apply(
                state, InventoryCostMath.Direction.OUTBOUND, 3, null, NOW);
        assertThat(out.totalCostCent()).isEqualTo(100L);
        assertThat(out.newState().onHandQuantity()).isZero();
        assertThat(out.newState().totalCostCent()).isZero();
        assertThat(out.newState().avgUnitCostCent()).isZero();
    }

    @Test
    void noBasisOutboundRecordsNullCostAndNeverGoesNegative() {
        // 存量库存无成本基准：出库不阻断、成本 NULL（不编造）、持有数量不为负。
        InventoryCostMath.Result out = InventoryCostMath.apply(
                empty(), InventoryCostMath.Direction.OUTBOUND, 5, null, NOW);
        assertThat(out.unitCostCent()).isNull();
        assertThat(out.totalCostCent()).isNull();
        assertThat(out.newState().onHandQuantity()).isZero();
        assertThat(out.newState().hasCostBasis()).isFalse();
    }

    @Test
    void inboundWithoutPriceOnBasisUsesCurrentAverage() {
        InventoryItemCostStateRecord state = InventoryCostMath.apply(
                empty(), InventoryCostMath.Direction.INBOUND, 10, 100L, NOW).newState();
        // 盘盈 2 件无进货价 → 按当前均价 100 计入，均价不变。
        InventoryCostMath.Result gain = InventoryCostMath.apply(
                state, InventoryCostMath.Direction.INBOUND, 2, null, NOW);
        assertThat(gain.unitCostCent()).isEqualTo(100L);
        assertThat(gain.totalCostCent()).isEqualTo(200L);
        assertThat(gain.newState().onHandQuantity()).isEqualTo(12L);
        assertThat(gain.newState().avgUnitCostCent()).isEqualTo(100L);
    }

    @Test
    void transferStampsReferenceCostWithoutMutatingState() {
        InventoryItemCostStateRecord state = InventoryCostMath.apply(
                empty(), InventoryCostMath.Direction.INBOUND, 10, 100L, NOW).newState();
        InventoryCostMath.Result transfer = InventoryCostMath.apply(
                state, InventoryCostMath.Direction.TRANSFER, 4, null, NOW);
        assertThat(transfer.unitCostCent()).isEqualTo(100L);
        assertThat(transfer.totalCostCent()).isEqualTo(400L);
        assertThat(transfer.newState()).isNull();
    }

    @Test
    void outboundBeyondOnHandConsumesOnlyHeldCost() {
        // 状态行只记 5 件 500 分，但出库 8 件（存量过渡期）：只扣转持有部分总成本，数量落 0。
        InventoryItemCostStateRecord state = new InventoryItemCostStateRecord(
                "item_1", "m_1", 5L, 500L, 100L, 500L, 0L, "moving_average", true, NOW);
        InventoryCostMath.Result out = InventoryCostMath.apply(
                state, InventoryCostMath.Direction.OUTBOUND, 8, null, NOW);
        assertThat(out.totalCostCent()).isEqualTo(500L);
        assertThat(out.newState().onHandQuantity()).isZero();
        assertThat(out.newState().totalCostCent()).isZero();
    }

    @Test
    void directionIsDerivedFromLocations() {
        assertThat(InventoryCostMath.direction(null, "wh_1")).isEqualTo(InventoryCostMath.Direction.INBOUND);
        assertThat(InventoryCostMath.direction("wh_1", null)).isEqualTo(InventoryCostMath.Direction.OUTBOUND);
        assertThat(InventoryCostMath.direction("wh_1", "bar_1")).isEqualTo(InventoryCostMath.Direction.TRANSFER);
    }
}
