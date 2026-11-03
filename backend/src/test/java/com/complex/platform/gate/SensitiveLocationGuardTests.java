package com.complex.platform.gate;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/** 第十八批 N 段第 123 条 123-R2：定位闸的球面距离（haversine，地球半径 6371008.8 米）。 */
class SensitiveLocationGuardTests {

    @Test
    void haversineMatchesKnownDistances() {
        // 沿经线或赤道 1 度＝2πR/360＝111195.08 米。
        assertThat(SensitiveLocationGuard.distanceMeters(0, 0, 1, 0)).isCloseTo(111195.08, within(1.0));
        assertThat(SensitiveLocationGuard.distanceMeters(0, 0, 0, 1)).isCloseTo(111195.08, within(1.0));
        // 测试用圆心以北 0.018 度约 2001.5 米。
        assertThat(SensitiveLocationGuard.distanceMeters(31.2304, 121.4737, 31.2484, 121.4737)).isCloseTo(2001.51, within(1.0));
    }
}
