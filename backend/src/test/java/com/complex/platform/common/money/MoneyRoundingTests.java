package com.complex.platform.common.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * R-7 金额舍入公共入口自检(纯单元,无 DB):四舍五入字面、符号对称、不溢出、尾差守恒。
 */
class MoneyRoundingTests {

    // ---- ① 四舍五入(HALF_UP)而非整除截断 ----

    @Test
    void bpsOfRoundsHalfUpInsteadOfTruncating() {
        // 100 分 × 12.34% = 12.34 分 → 截断 12,四舍五入 12。
        assertThat(MoneyRounding.bpsOf(100L, 1234L)).isEqualTo(12L);
        // 100 分 × 12.55% = 12.55 分 → 截断 12(旧),四舍五入 13(新)。
        assertThat(MoneyRounding.bpsOf(100L, 1255L)).isEqualTo(13L);
        // 恰好 .5 → 进位(HALF_UP,不是 HALF_EVEN)。
        assertThat(MoneyRounding.bpsOf(1L, 5000L)).isEqualTo(1L);
        assertThat(MoneyRounding.bpsOf(3L, 5000L)).isEqualTo(2L);   // 1.5 → 2
        // 旧整除截断在这三例上分别给 12 / 12 / 0 / 1,证伪"整除≡四舍五入"。
        assertThat(100L * 1255L / 10000L).isEqualTo(12L);
        assertThat(1L * 5000L / 10000L).isZero();
        assertThat(3L * 5000L / 10000L).isEqualTo(1L);
    }

    @Test
    void prorateRoundsHalfUp() {
        assertThat(MoneyRounding.prorate(1000L, 1, 3)).isEqualTo(333L);   // 333.33…
        assertThat(MoneyRounding.prorate(1000L, 2, 3)).isEqualTo(667L);   // 666.66… → 667
        assertThat(MoneyRounding.prorate(1L, 1, 2)).isEqualTo(1L);        // 0.5 → 1
        assertThat(MoneyRounding.prorate(7L, 1, 2)).isEqualTo(4L);        // 3.5 → 4
    }

    // ---- ② 符号对称 f(-x) = -f(x)(冲正/红冲逐分对消的前提) ----

    @Test
    void roundingIsSignSymmetricSoReversalsNetToZero() {
        long[] bases = {0L, 1L, 3L, 7L, 99L, 12345L, 999_999L};
        int[] rates = {0, 1, 250, 3333, 5000, 8888, 10000};
        for (long base : bases) {
            for (int bps : rates) {
                assertThat(MoneyRounding.bpsOf(-base, bps))
                        .as("f(-%d, %d) 必须 = -f(%d, %d)", base, bps, base, bps)
                        .isEqualTo(-MoneyRounding.bpsOf(base, bps));
                // 核销 + 冲正 = 0(全生命周期守恒)
                assertThat(MoneyRounding.bpsOf(base, bps) + MoneyRounding.bpsOf(-base, bps)).isZero();
            }
        }
    }

    // ---- ③ 不溢出(旧 base*bps 先乘后除会溢 long) ----

    @Test
    void noOverflowOnHugeBase() {
        long huge = Long.MAX_VALUE / 3;                       // × 10000 必溢 long
        assertThat(huge * 10000L / 10000L).isNotEqualTo(huge); // 证伪旧写法:先乘即溢出、结果失真
        assertThat(MoneyRounding.bpsOf(huge, 10000L)).isEqualTo(huge); // 新写法 100% = 原值
    }

    @Test
    void zeroDenominatorIsRejectedLoudly() {
        assertThatThrownBy(() -> MoneyRounding.prorate(100L, 1L, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- ④ 尾差守恒:Σ各方份额 + 尾差 ≡ 应分总额 ----

    @Test
    void residualMakesPartsSumBackToTotalExactly() {
        // 池 1000 分,三家各 33.33%(合计 99.99%)——各自 HALF_UP 后合计与应分总额差 1 分。
        long pool = 1000L;
        int[] shares = {3333, 3333, 3333};
        List<Long> rounded = new ArrayList<>();
        BigDecimal exactTotal = BigDecimal.ZERO;
        for (int bps : shares) {
            rounded.add(MoneyRounding.bpsOf(pool, bps));
            exactTotal = exactTotal.add(MoneyRounding.exactBps(pool, bps));
        }
        long residual = MoneyRounding.residual(exactTotal, rounded);
        long sum = rounded.stream().mapToLong(Long::longValue).sum();
        assertThat(sum + residual).isEqualTo(MoneyRounding.toCent(exactTotal)); // 守恒
        assertThat(rounded).containsExactly(333L, 333L, 333L);
        assertThat(MoneyRounding.toCent(exactTotal)).isEqualTo(1000L);          // 999.9 → 1000
        assertThat(residual).isEqualTo(1L);                                     // 尾差 1 分归平台
    }

    @Test
    void residualIsZeroWhenNoRoundingHappens() {
        long pool = 10_000L;
        List<Long> rounded = List.of(MoneyRounding.bpsOf(pool, 5000), MoneyRounding.bpsOf(pool, 5000));
        BigDecimal exactTotal = MoneyRounding.exactBps(pool, 5000).add(MoneyRounding.exactBps(pool, 5000));
        assertThat(MoneyRounding.residual(exactTotal, rounded)).isZero();
        assertThat(rounded).containsExactly(5000L, 5000L);
    }

    @Test
    void residualCanBeNegativeAndStillConserves() {
        // 池 100 分,两家各 50.5%——各自 HALF_UP 进位(51+51=102)超过应分总额 101 → 尾差 -1(平台追回)。
        long pool = 100L;
        List<Long> rounded = List.of(MoneyRounding.bpsOf(pool, 5050), MoneyRounding.bpsOf(pool, 5050));
        BigDecimal exactTotal = MoneyRounding.exactBps(pool, 5050).add(MoneyRounding.exactBps(pool, 5050));
        long residual = MoneyRounding.residual(exactTotal, rounded);
        assertThat(rounded).containsExactly(51L, 51L);
        assertThat(MoneyRounding.toCent(exactTotal)).isEqualTo(101L);
        assertThat(residual).isEqualTo(-1L);
        assertThat(rounded.stream().mapToLong(Long::longValue).sum() + residual).isEqualTo(101L);
    }

    // ---- ⑤ 多段累加只舍入一次(阶梯提成/多档) ----

    @Test
    void multiSegmentAccumulatesExactlyThenRoundsOnce() {
        // 三段各 0.4 分:逐段 HALF_UP 各得 0 → 合计 0(丢 1.2 分);先累精确再收一次 = 1 分。
        BigDecimal exact = MoneyRounding.exactBps(10L, 400)
                .add(MoneyRounding.exactBps(10L, 400))
                .add(MoneyRounding.exactBps(10L, 400));
        long perSegment = MoneyRounding.bpsOf(10L, 400) * 3;
        assertThat(perSegment).isZero();
        assertThat(MoneyRounding.toCent(exact)).isEqualTo(1L);
    }
}
