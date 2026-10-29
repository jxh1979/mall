package com.complex.platform.bar;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BarPourLossTests {

    @Test
    void lossIsExcessOverStandardTimesQuantity() {
        // 标准 30ml/杯 × 2 杯 = 60ml；实际 75ml → 损耗 15ml
        assertThat(BarPourLoss.lossVolume(30, 2, 75)).isEqualTo(15L);
    }

    @Test
    void noLossWhenActualWithinStandard() {
        assertThat(BarPourLoss.lossVolume(30, 2, 60)).isEqualTo(0L);
        assertThat(BarPourLoss.lossVolume(30, 2, 50)).isEqualTo(0L);
    }

    @Test
    void exceedsThresholdOnlyWhenLossStrictlyAboveThreshold() {
        assertThat(BarPourLoss.exceedsThreshold(15L, 10)).isTrue();
        assertThat(BarPourLoss.exceedsThreshold(10L, 10)).isFalse();
        assertThat(BarPourLoss.exceedsThreshold(15L, null)).isFalse(); // 未配置阈值不触发
    }

    @Test
    void bottlesToOpenUsesOpenedVolumeFirstThenOpensWholeBottles() {
        // 第十八批 I 段第 76 条：700ml 瓶装。余量够（余 645 要 55）、正好用完（余 55 要 55）都不开瓶；没余量开一瓶；跨两瓶（余 20 要 800）开两瓶。
        assertThat(BarPourLoss.bottlesToOpen(55, 645, 700)).isZero();
        assertThat(BarPourLoss.bottlesToOpen(55, 55, 700)).isZero();
        assertThat(BarPourLoss.bottlesToOpen(55, 0, 700)).isEqualTo(1);
        assertThat(BarPourLoss.bottlesToOpen(800, 20, 700)).isEqualTo(2);
    }
}
