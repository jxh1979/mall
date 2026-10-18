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
 * 「高价值」判定全平台归一的口径本体测试。
 *
 * <p>业务口径（BSM「高价值物品」、ENUM「高价值判定」）：以该物品<b>最近一次实际到货价</b>（不使用滚动变化的
 * 当前平均进货价）与后台配置的<b>固定金额阈值</b>比较，该到货价<b>大于或等于</b>阈值即为高价值、低于阈值即为低价值；
 * 或被<b>手工标记</b>为高价值；<b>阈值未配置时一律按高价值处理、拦下走审批，不得静默放行</b>。
 * 退酒 / 报损 / 盘点差异 / 存酒作废四处共用本判定。
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

    /**
     * @param latestArrivalCent 最近一次采购入库流水的实际到货单价；null = 无采购入库流水 / 该笔未录单价
     * @param avgUnitCostCent   移动加权成本台账当前均价；null = 无成本基准（判定不得使用它，仅用于证明不被误用）
     */
    private void item(String itemId, boolean manualHighValue, Long latestArrivalCent, Long avgUnitCostCent) {
        when(repository.findInventoryItemById(itemId)).thenReturn(Optional.of(new InventoryItemRecord(
                itemId, null, "酒", "spirit", "bottle", null, false, 0, "active",
                Instant.now(), Instant.now(), null, manualHighValue)));
        when(repository.findLatestPurchaseArrivalUnitCostCent(itemId))
                .thenReturn(Optional.ofNullable(latestArrivalCent));
        when(repository.findCostState(itemId)).thenReturn(avgUnitCostCent == null
                ? Optional.empty()
                : Optional.of(new InventoryItemCostStateRecord(
                        itemId, null, 0L, 0L, avgUnitCostCent, 0L, 0L, "moving_average", true, Instant.now())));
    }

    @Test
    void latestArrivalPriceAboveThresholdIsHighValue() {
        item("item_hi", false, 600_000L, 600_000L); // 到货价 6000 元 ≥ 5000 元
        HighValueDecisionService.HighValueDecision decision = service.decide("item_hi", "m_1");
        assertThat(decision.highValue()).isTrue();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_ABOVE_THRESHOLD);
        assertThat(decision.purchaseUnitCostCent()).isEqualTo(600_000L);
        assertThat(decision.thresholdCent()).isEqualTo(500_000L);
    }

    @Test
    void latestArrivalPriceBelowThresholdIsLowValue() {
        item("item_lo", false, 400_000L, 400_000L); // 到货价 4000 元 < 5000 元
        HighValueDecisionService.HighValueDecision decision = service.decide("item_lo", "m_1");
        assertThat(decision.highValue()).isFalse();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_BELOW_THRESHOLD);
    }

    @Test
    void latestArrivalPriceEqualToThresholdIsHighValue() {
        // BSM「高价值物品」：该到货价大于或等于阈值即为高价值——等于算高价值。
        item("item_eq", false, 500_000L, 500_000L);
        HighValueDecisionService.HighValueDecision decision = service.decide("item_eq", "m_1");
        assertThat(decision.highValue()).isTrue();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_ABOVE_THRESHOLD);
        assertThat(decision.purchaseUnitCostCent()).isEqualTo(500_000L);
    }

    @Test
    void latestArrivalPriceWinsOverLowerMovingAverage() {
        // 最近一次到货价 6000 元 ≥ 阈值，而滚动均价 4181 元 < 阈值：
        // BSM 明文「不使用滚动变化的当前平均进货价」，必须判高价值。
        item("item_recent", false, 600_000L, 418_181L);
        HighValueDecisionService.HighValueDecision decision = service.decide("item_recent", "m_1");
        assertThat(decision.highValue()).isTrue();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_ABOVE_THRESHOLD);
        assertThat(decision.purchaseUnitCostCent()).isEqualTo(600_000L);
    }

    @Test
    void latestArrivalPriceBelowThresholdIsLowValueEvenIfMovingAverageIsAbove() {
        // 对称面：最近一次到货价 4000 元 < 阈值，均价 6000 元 ≥ 阈值——仍以到货价为准 ⇒ 低价值。
        item("item_recent_lo", false, 400_000L, 600_000L);
        HighValueDecisionService.HighValueDecision decision = service.decide("item_recent_lo", "m_1");
        assertThat(decision.highValue()).isFalse();
        assertThat(decision.purchaseUnitCostCent()).isEqualTo(400_000L);
    }

    @Test
    void movingAverageWithoutPurchaseArrivalIsCostUnknown() {
        // 只有成本台账均价、没有任何采购入库流水 ⇒ 最近一次实际到货价未知 ⇒ 不得拿均价顶替，按高价值拦下。
        item("item_avg_only", false, null, 600_000L);
        HighValueDecisionService.HighValueDecision decision = service.decide("item_avg_only", "m_1");
        assertThat(decision.highValue()).isTrue();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_COST_UNKNOWN);
        assertThat(decision.purchaseUnitCostCent()).isNull();
    }

    @Test
    void manualMarkWinsOverLowArrivalPrice() {
        item("item_mark", true, 1_000L, 1_000L); // 到货价仅 10 元，但被手工标记
        HighValueDecisionService.HighValueDecision decision = service.decide("item_mark", "m_1");
        assertThat(decision.highValue()).isTrue();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_MANUAL_MARK);
    }

    @Test
    void thresholdUnsetIsHighValueNotSilentPass() {
        thresholdCent = null;
        item("item_any", false, 1_000L, 1_000L); // 到货价极低，仍必须被拦下
        HighValueDecisionService.HighValueDecision decision = service.decide("item_any", "m_1");
        assertThat(decision.highValue()).isTrue();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_THRESHOLD_UNSET);
        assertThat(decision.thresholdCent()).isNull();
    }

    @Test
    void thresholdPortMissingIsHighValueNotSilentPass() {
        service.thresholdPort = null; // 端口未装配 = 阈值未配置
        item("item_any2", false, 1_000L, 1_000L);
        assertThat(service.decide("item_any2", "m_1").highValue()).isTrue();
    }

    @Test
    void unknownArrivalPriceIsHighValue() {
        // 没有到货价就不能证明它低价值——否则「不录到货价」立刻成为绕过主管复核的后门。
        item("item_nocost", false, null, null);
        HighValueDecisionService.HighValueDecision decision = service.decide("item_nocost", "m_1");
        assertThat(decision.highValue()).isTrue();
        assertThat(decision.basis()).isEqualTo(HighValueDecisionService.BASIS_COST_UNKNOWN);
    }

    @Test
    void nullItemIsHighValue() {
        // 商品没关联库存物料（如现调鸡尾酒）⇒ 到货价无从谈起 ⇒ 拦下走审批。
        when(repository.findLatestPurchaseArrivalUnitCostCent(anyString())).thenReturn(Optional.empty());
        assertThat(service.decide(null, "m_1").highValue()).isTrue();
    }
}
