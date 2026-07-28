package com.complex.platform.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AuthorizationService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * V350「高价值」判定全平台归一的口径本体测试。
 *
 * <p>业务口径（业务HTML 2026-07-28）：以该物品的<b>进货价</b>与后台配置的<b>一个固定金额阈值</b>比较，
 * 高于即高价值、低于即低价值；或被<b>手工标记</b>为高价值；<b>阈值未配置时一律按高价值处理、
 * 拦下走审批，不得静默放行</b>。退酒 / 报损 / 盘点差异 / 存酒作废四处共用本判定。
 */
class HighValueDecisionServiceTests {

    private InventoryRepository repository;
    private HighValueDecisionService service;
    private Long thresholdCent;

    @BeforeEach
    void setUp() {
        repository = mock(InventoryRepository.class);
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        when(authorizationService.currentProjectScopeQuietly()).thenReturn(Optional.empty());
        service = new HighValueDecisionService(repository, authorizationService);
        service.thresholdPort = (projectId, merchantId) -> thresholdCent;
        thresholdCent = 500_000L; // 出厂默认 5000 元
    }

    private void itemWithCost(String itemId, boolean manualHighValue, Long avgUnitCostCent) {
        when(repository.findInventoryItemById(itemId)).thenReturn(Optional.of(new InventoryItemRecord(
                itemId, null, "酒", "spirit", "bottle", null, false, 0, "active",
                Instant.now(), Instant.now(), null, manualHighValue)));
        when(repository.findCostState(itemId)).thenReturn(avgUnitCostCent == null
                ? Optional.empty()
                : Optional.of(new InventoryItemCostStateRecord(
                        itemId, null, 0L, 0L, avgUnitCostCent, 0L, 0L, "moving_average", true, Instant.now())));
    }

    @Test
    void purchasePriceAboveThresholdIsHighValue() {
        itemWithCost("item_hi", false, 600_000L); // 进货价 6000 元 > 5000 元
        HighValueDecisionService.HighValueDecision decision = service.decide("item_hi", "m_1");
        assertThat(decision.highValue()).isTrue();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_ABOVE_THRESHOLD);
        assertThat(decision.purchaseUnitCostCent()).isEqualTo(600_000L);
        assertThat(decision.thresholdCent()).isEqualTo(500_000L);
    }

    @Test
    void purchasePriceBelowThresholdIsLowValue() {
        itemWithCost("item_lo", false, 400_000L); // 进货价 4000 元 < 5000 元
        assertThat(service.decide("item_lo", "m_1").highValue()).isFalse();
    }

    @Test
    void purchasePriceEqualToThresholdIsLowValue() {
        // 口径是「高于阈值即高价值」，等于不算高于。
        itemWithCost("item_eq", false, 500_000L);
        assertThat(service.decide("item_eq", "m_1").highValue()).isFalse();
    }

    @Test
    void manualMarkWinsOverLowPurchasePrice() {
        itemWithCost("item_mark", true, 1_000L); // 进货价仅 10 元，但被手工标记
        HighValueDecisionService.HighValueDecision decision = service.decide("item_mark", "m_1");
        assertThat(decision.highValue()).isTrue();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_MANUAL_MARK);
    }

    @Test
    void thresholdUnsetIsHighValueNotSilentPass() {
        thresholdCent = null;
        itemWithCost("item_any", false, 1_000L); // 进货价极低，仍必须被拦下
        HighValueDecisionService.HighValueDecision decision = service.decide("item_any", "m_1");
        assertThat(decision.highValue()).isTrue();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_THRESHOLD_UNSET);
        assertThat(decision.thresholdCent()).isNull();
    }

    @Test
    void thresholdPortMissingIsHighValueNotSilentPass() {
        service.thresholdPort = null; // 端口未装配 = 阈值未配置
        itemWithCost("item_any2", false, 1_000L);
        assertThat(service.decide("item_any2", "m_1").highValue()).isTrue();
    }

    @Test
    void unknownPurchasePriceIsHighValue() {
        // 没录进货价就不能证明它低价值——否则「不录进货价」立刻成为绕过主管复核的后门。
        itemWithCost("item_nocost", false, null);
        HighValueDecisionService.HighValueDecision decision = service.decide("item_nocost", "m_1");
        assertThat(decision.highValue()).isTrue();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_COST_UNKNOWN);
    }

    @Test
    void costStateWithoutBasisCountsAsUnknownPurchasePrice() {
        when(repository.findInventoryItemById("item_nobasis")).thenReturn(Optional.of(new InventoryItemRecord(
                "item_nobasis", null, "酒", "spirit", "bottle", null, false, 0, "active",
                Instant.now(), Instant.now(), null, false)));
        when(repository.findCostState("item_nobasis")).thenReturn(Optional.of(new InventoryItemCostStateRecord(
                "item_nobasis", null, 0L, 0L, 0L, 0L, 0L, "moving_average", false, Instant.now())));
        assertThat(service.decide("item_nobasis", "m_1").basis())
                .isEqualTo(HighValueDecisionService.BASIS_COST_UNKNOWN);
    }

    @Test
    void nullItemIsHighValue() {
        // 商品没关联库存物料（如现调鸡尾酒）⇒ 进货价无从谈起 ⇒ 拦下走审批。
        when(repository.findCostState(anyString())).thenReturn(Optional.empty());
        assertThat(service.decide(null, "m_1").highValue()).isTrue();
    }
}
