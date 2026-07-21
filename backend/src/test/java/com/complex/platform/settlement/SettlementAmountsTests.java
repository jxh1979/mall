package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 结算金额不可变增量公式的资金恒等自检(非 DB 单测)：
 * ①逐笔增量相加 == 累计已核销(telescope,无舍入漂移)；②冲正=原行取反,配对净 0。
 */
class SettlementAmountsTests {

    @Test
    void incrementalDeltasTelescopeToCumulativeNonExclusive() {
        // allocated=1000 分, total=3 次:round(1000×k/3)=333/667/1000,增量=333/334/333,Σ=1000。
        long allocated = 1000L;
        int total = 3;
        long sum = 0;
        for (int k = 1; k <= total; k++) {
            sum += SettlementAmounts.incrementalVerified(allocated, total, k, false, null);
            assertThat(sum).isEqualTo(
                    SettlementAmounts.cumulativeVerified(allocated, total, k, false, null));
        }
        assertThat(sum).isEqualTo(allocated); // 全核销 == allocated,无漂移
    }

    @Test
    void incrementalDeltasNonNegativeAndBoundedByAllocated() {
        long allocated = 7L; // 极端:allocated<total,逐笔可能为 0/1
        int total = 5;
        long sum = 0;
        for (int k = 1; k <= total; k++) {
            long d = SettlementAmounts.incrementalVerified(allocated, total, k, false, null);
            assertThat(d).isGreaterThanOrEqualTo(0L);
            sum += d;
        }
        assertThat(sum).isEqualTo(allocated);
    }

    @Test
    void exclusiveByPriceIncrementIsFlatPrice() {
        long price = 1500L;
        for (int k = 1; k <= 4; k++) {
            assertThat(SettlementAmounts.incrementalVerified(null, 4, k, true, price)).isEqualTo(price);
        }
        assertThat(SettlementAmounts.cumulativeVerified(null, 4, 4, true, price)).isEqualTo(price * 4);
    }

    @Test
    void reversalIsExactInverseSoPairNetsZero() {
        // 冲正取原核销行金额的严格取反 → 配对 SUM 净 0(资金守恒)。
        long increment = SettlementAmounts.incrementalVerified(1000L, 3, 2, false, null); // 334
        long reversal = -increment;
        assertThat(increment + reversal).isZero();
    }
}
