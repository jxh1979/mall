package com.complex.platform.dividend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * 纯算法单测：净利劈分/税额取整（整除链截断，与薪资税表 V217 同口径）/恒等式。
 * 恒等式与 V299 dividend_statements 的 DB CHECK 一一对应。
 */
class DividendMathTests {

    @Test
    void typicalCaseDefaultRatioAndTax() {
        // 净利 10,000 元 → 平台 80% = 8,000 元；主理人 2,000 元；代扣 20% = 400 元；实发 1,600 元。
        DividendMath.Split split = DividendMath.split(1_000_000L, 8000, 2000);
        assertThat(split.distributableCent()).isEqualTo(1_000_000L);
        assertThat(split.platformShareCent()).isEqualTo(800_000L);
        assertThat(split.principalGrossCent()).isEqualTo(200_000L);
        assertThat(split.taxCent()).isEqualTo(40_000L);
        assertThat(split.principalNetCent()).isEqualTo(160_000L);
    }

    @Test
    void oddCentsTruncateAndRemainderGoesToPrincipal() {
        // 999 分 × 80% = 799.2 → 整除链截断 799；余数归主理人：999 − 799 = 200。
        DividendMath.Split split = DividendMath.split(999L, 8000, 2000);
        assertThat(split.platformShareCent()).isEqualTo(799L);
        assertThat(split.principalGrossCent()).isEqualTo(200L);
        // 税 200 × 20% = 40（整除无余）。
        assertThat(split.taxCent()).isEqualTo(40L);
        assertThat(split.principalNetCent()).isEqualTo(160L);
    }

    @Test
    void taxTruncatesNotRounds() {
        // 主理人应得 999 分 × 20% = 199.8 → 截断 199（不四舍五入到 200）。
        DividendMath.Split split = DividendMath.split(999L, 0, 2000);
        assertThat(split.principalGrossCent()).isEqualTo(999L);
        assertThat(split.taxCent()).isEqualTo(199L);
        assertThat(split.principalNetCent()).isEqualTo(800L);
    }

    @Test
    void splitIdentityHoldsForArbitraryAmounts() {
        // 恒等式：platform + gross = distributable；net = gross − tax（抽样多组奇异值）。
        long[] samples = {1L, 3L, 7L, 99L, 101L, 12_345L, 999_999L, 1_000_001L, 987_654_321L};
        int[] shares = {1, 3333, 5000, 8000, 9999};
        int[] taxes = {0, 1, 2000, 4567, 9999};
        for (long net : samples) {
            for (int share : shares) {
                for (int tax : taxes) {
                    DividendMath.Split split = DividendMath.split(net, share, tax);
                    assertThat(split.platformShareCent() + split.principalGrossCent())
                            .as("split identity net=%d share=%d", net, share)
                            .isEqualTo(split.distributableCent());
                    assertThat(split.principalNetCent())
                            .as("net pay identity net=%d tax=%d", net, tax)
                            .isEqualTo(split.principalGrossCent() - split.taxCent());
                    assertThat(split.taxCent()).isBetween(0L, split.principalGrossCent());
                }
            }
        }
    }

    @Test
    void negativeNetProfitYieldsZeroDistribution() {
        // 亏损期可分配额为 0，不倒扣主理人现金。
        DividendMath.Split split = DividendMath.split(-500_000L, 8000, 2000);
        assertThat(split.distributableCent()).isZero();
        assertThat(split.platformShareCent()).isZero();
        assertThat(split.principalGrossCent()).isZero();
        assertThat(split.taxCent()).isZero();
        assertThat(split.principalNetCent()).isZero();
    }

    @Test
    void zeroNetProfitYieldsZeroDistribution() {
        DividendMath.Split split = DividendMath.split(0L, 8000, 2000);
        assertThat(split.distributableCent()).isZero();
        assertThat(split.principalNetCent()).isZero();
    }

    @Test
    void oneCentAllGoesToPrincipalUntaxed() {
        // 1 分 × 80% 截断 = 0 → 全归主理人；1 分 × 20% 截断 = 0 → 免税实发 1 分（不丢分）。
        DividendMath.Split split = DividendMath.split(1L, 8000, 2000);
        assertThat(split.platformShareCent()).isZero();
        assertThat(split.principalGrossCent()).isEqualTo(1L);
        assertThat(split.taxCent()).isZero();
        assertThat(split.principalNetCent()).isEqualTo(1L);
    }

    @Test
    void boundaryBpsZeroAndFull() {
        // 平台 0%：全归主理人；平台 100%：主理人 0、税 0。
        DividendMath.Split allPrincipal = DividendMath.split(10_000L, 0, 2000);
        assertThat(allPrincipal.platformShareCent()).isZero();
        assertThat(allPrincipal.principalGrossCent()).isEqualTo(10_000L);
        DividendMath.Split allPlatform = DividendMath.split(10_000L, 10000, 2000);
        assertThat(allPlatform.platformShareCent()).isEqualTo(10_000L);
        assertThat(allPlatform.principalGrossCent()).isZero();
        assertThat(allPlatform.taxCent()).isZero();
    }

    @Test
    void zeroTaxRateMeansNetEqualsGross() {
        DividendMath.Split split = DividendMath.split(123_456L, 8000, 0);
        assertThat(split.taxCent()).isZero();
        assertThat(split.principalNetCent()).isEqualTo(split.principalGrossCent());
    }

    @Test
    void invalidBpsRejected() {
        assertThatThrownBy(() -> DividendMath.split(100L, -1, 2000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DividendMath.split(100L, 10001, 2000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DividendMath.split(100L, 8000, 10001))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── V303 扣除项三开关 ──────────────────────────────────────────

    /** compute 全关且无往期累计（carryIn=0）逐分等于 split（回归保证：默认口径不变）。 */
    @Test
    void computeAllTogglesOffEqualsLegacySplit() {
        long[] samples = {-500_000L, 0L, 1L, 999L, 1_000_000L, 987_654_321L};
        int[] shares = {0, 3333, 8000, 10000};
        int[] taxes = {0, 2000, 9999};
        for (long net : samples) {
            for (int share : shares) {
                for (int tax : taxes) {
                    DividendMath.Split legacy = DividendMath.split(net, share, tax);
                    DividendMath.Distribution d = DividendMath.compute(net, share, tax,
                            false, 0L, false, 999_999L, false, 5000, 999_999L);
                    assertThat(d.distributableCent()).as("dist net=%d", net).isEqualTo(legacy.distributableCent());
                    assertThat(d.platformShareCent()).isEqualTo(legacy.platformShareCent());
                    assertThat(d.principalGrossCent()).isEqualTo(legacy.principalGrossCent());
                    assertThat(d.taxCent()).isEqualTo(legacy.taxCent());
                    assertThat(d.principalNetCent()).isEqualTo(legacy.principalNetCent());
                    // 关闭亏损结转：不扣减（applied=0）；欠款抵扣/留存关=0；实发 = 税后应得。
                    assertThat(d.carryInCent()).isZero();
                    assertThat(d.carryAppliedCent()).isZero();
                    assertThat(d.arrearsOffsetCent()).isZero();
                    assertThat(d.reserveWithheldCent()).isZero();
                    assertThat(d.payoutCent()).isEqualTo(legacy.principalNetCent());
                }
            }
        }
    }

    @Test
    void lossCarryforwardFirstLossAccumulatesAndDistributesZero() {
        // 首个亏损期：期初累计 0，净利 −500 → 可分配 0、弥补 0、结转至下期 500。
        DividendMath.Distribution d = DividendMath.compute(-500L, 8000, 2000, true, 0L, false, 0L, false, 0, 0L);
        assertThat(d.distributableCent()).isZero();
        assertThat(d.carryAppliedCent()).isZero();
        assertThat(d.carryAfterCent()).isEqualTo(500L);
        assertThat(d.payoutCent()).isZero();
    }

    @Test
    void lossCarryforwardProfitPartiallyRecoversPriorLoss() {
        // 净利 300 < 期初累计 1000 → 全额弥补 300、可分配 0、结转 700。
        DividendMath.Distribution d = DividendMath.compute(300L, 8000, 2000, true, 1000L, false, 0L, false, 0, 0L);
        assertThat(d.distributableCent()).isZero();
        assertThat(d.carryAppliedCent()).isEqualTo(300L);
        assertThat(d.carryAfterCent()).isEqualTo(700L);
    }

    @Test
    void lossCarryforwardProfitExceedsPriorLoss() {
        // 净利 1500 > 期初累计 1000 → 弥补 1000、可分配 500、结转 0。
        DividendMath.Distribution d = DividendMath.compute(1500L, 10000, 0, true, 1000L, false, 0L, false, 0, 0L);
        assertThat(d.carryAppliedCent()).isEqualTo(1000L);
        assertThat(d.carryAfterCent()).isZero();
        assertThat(d.distributableCent()).isEqualTo(500L);
        assertThat(d.platformShareCent()).isEqualTo(500L);
    }

    @Test
    void lossCarryforwardDisabledCarriesButDoesNotDeduct() {
        // 用户 2026-07-19 拍板：关闭期不扣减可分配额，但累计不清零、恒结转。
        // 关闭 + 盈利 1500 + 期初累计 1000：不弥补（applied=0）、可分配 1500（不扣）、累计 1000 原样带下去。
        DividendMath.Distribution d = DividendMath.compute(1500L, 0, 0, false, 1000L, false, 0L, false, 0, 0L);
        assertThat(d.carryInCent()).isEqualTo(1000L);
        assertThat(d.carryAppliedCent()).isZero();
        assertThat(d.distributableCent()).isEqualTo(1500L);
        assertThat(d.carryAfterCent()).isEqualTo(1000L);
        // 关闭 + 亏损 500 + 期初累计 1000：累计继续叠加到 1500（关闭期照样累计新亏损）。
        DividendMath.Distribution loss = DividendMath.compute(-500L, 8000, 2000, false, 1000L, false, 0L, false, 0, 0L);
        assertThat(loss.carryAppliedCent()).isZero();
        assertThat(loss.distributableCent()).isZero();
        assertThat(loss.carryAfterCent()).isEqualTo(1500L);
    }

    @Test
    void arrearsOffsetPartialReducesPayout() {
        // 税后应得 1600（net 10000, 平台 80%, 税 20%）；欠款 1000 < 应得 → 抵扣 1000，实发 600。
        DividendMath.Distribution d = DividendMath.compute(1_000_000L, 8000, 2000, false, 0L, true, 1000L, false, 0, 0L);
        assertThat(d.principalNetCent()).isEqualTo(160_000L);
        assertThat(d.arrearsOffsetCent()).isEqualTo(1000L);
        assertThat(d.payoutCent()).isEqualTo(159_000L);
    }

    @Test
    void arrearsOffsetClampsToPrincipalNetAndPayoutZero() {
        // 欠款 999999 > 税后应得 160000 → 抵扣钳到 160000，实发 0（不倒欠主理人）。
        DividendMath.Distribution d = DividendMath.compute(1_000_000L, 8000, 2000, false, 0L, true, 999_999L, false, 0, 0L);
        assertThat(d.arrearsOffsetCent()).isEqualTo(160_000L);
        assertThat(d.payoutCent()).isZero();
    }

    @Test
    void arrearsOffsetDisabledIgnored() {
        DividendMath.Distribution d = DividendMath.compute(1_000_000L, 8000, 2000, false, 0L, false, 999_999L, false, 0, 0L);
        assertThat(d.arrearsOffsetCent()).isZero();
        assertThat(d.payoutCent()).isEqualTo(160_000L);
    }

    @Test
    void payoutFloorByPercentageWithholdsReserve() {
        // 税后应得 160000，留 10%（1000 万分比）→ 留存 16000，实发 144000。
        DividendMath.Distribution d = DividendMath.compute(1_000_000L, 8000, 2000, false, 0L, false, 0L, true, 1000, 0L);
        assertThat(d.reserveWithheldCent()).isEqualTo(16_000L);
        assertThat(d.payoutCent()).isEqualTo(144_000L);
    }

    @Test
    void payoutFloorTakesMaxOfPctAndFixedThenCapsAtAvailable() {
        // 应得 160000；比例 10%=16000，固定 20000 → 取大 20000，实发 140000。
        DividendMath.Distribution d1 = DividendMath.compute(1_000_000L, 8000, 2000, false, 0L, false, 0L, true, 1000, 20_000L);
        assertThat(d1.reserveWithheldCent()).isEqualTo(20_000L);
        assertThat(d1.payoutCent()).isEqualTo(140_000L);
        // 固定额 999999 > 可发额 → 留存钳到 160000，实发 0（不为负）。
        DividendMath.Distribution d2 = DividendMath.compute(1_000_000L, 8000, 2000, false, 0L, false, 0L, true, 0, 999_999L);
        assertThat(d2.reserveWithheldCent()).isEqualTo(160_000L);
        assertThat(d2.payoutCent()).isZero();
    }

    @Test
    void allThreeDeductionsComposeInOrderLossThenArrearsThenReserve() {
        // 净利 1000000，期初亏损累计 200000 → 可分配 800000；平台 80% → 应得毛 160000，税 20% → 税后 128000；
        // 欠款抵扣 28000 → 余 100000；留存 10% → 10000；实发 90000。
        DividendMath.Distribution d = DividendMath.compute(
                1_000_000L, 8000, 2000, true, 200_000L, true, 28_000L, true, 1000, 0L);
        assertThat(d.distributableCent()).isEqualTo(800_000L);
        assertThat(d.carryAppliedCent()).isEqualTo(200_000L);
        assertThat(d.carryAfterCent()).isZero();
        assertThat(d.principalNetCent()).isEqualTo(128_000L);
        assertThat(d.arrearsOffsetCent()).isEqualTo(28_000L);
        assertThat(d.reserveWithheldCent()).isEqualTo(10_000L);
        assertThat(d.payoutCent()).isEqualTo(90_000L);
        // 发放恒等：税后应得 − 抵扣 − 留存 = 实发。
        assertThat(d.payoutCent())
                .isEqualTo(d.principalNetCent() - d.arrearsOffsetCent() - d.reserveWithheldCent());
    }

    @Test
    void reserveBpsValidated() {
        assertThatThrownBy(() -> DividendMath.compute(100L, 8000, 2000, false, 0L, false, 0L, true, 10001, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── V309 自定义扣除项（#35）纯算式 ─────────────────────────────────

    @Test
    void clampDeductionsSequentiallyCapsAtBudgetNeverNegative() {
        // 预算 100；三项 40/50/30 → 40（余 60）、50（余 10）、10（余 0，第三项原 30 只扣 10）。
        long[] applied = DividendMath.clampDeductions(100L, new long[] {40L, 50L, 30L});
        assertThat(applied).containsExactly(40L, 50L, 10L);
        // 合计不超预算；负预算/负原始额按 0。
        assertThat(DividendMath.clampDeductions(-5L, new long[] {10L})).containsExactly(0L);
        assertThat(DividendMath.clampDeductions(100L, new long[] {-10L, 20L})).containsExactly(0L, 20L);
        assertThat(DividendMath.clampDeductions(100L, null)).isEmpty();
    }

    @Test
    void emptyCustomDeductionsEqualsLegacyDistribution() {
        // 无自定义扣除时，11 参 compute 逐分等于 10 参（V303）现状。
        DividendMath.Distribution legacy = DividendMath.compute(
                1_000_000L, 8000, 2000, false, 0L, true, 1000L, true, 1000, 0L);
        DividendMath.Distribution withEmpty = DividendMath.compute(
                1_000_000L, 8000, 2000, false, 0L, true, 1000L, new long[0], true, 1000, 0L);
        assertThat(withEmpty).isEqualTo(legacy);
        assertThat(legacy.customDeductionCent()).isZero();
    }

    @Test
    void customDeductionsReducePayoutAfterArrearsBeforeReserve() {
        // 净利 100 万 → 毛 20 万、税 4 万 → 税后 16 万；无欠款；自定义扣 3 万 + 10 万 = 13 万 → 余 3 万；
        // 留存 10% 作用于自定义扣后余额 3 万 = 3000，实发 27000。
        DividendMath.Distribution d = DividendMath.compute(
                1_000_000L, 8000, 2000, false, 0L, false, 0L,
                new long[] {30_000L, 100_000L}, true, 1000, 0L);
        assertThat(d.principalNetCent()).isEqualTo(160_000L);
        assertThat(d.customDeductionCent()).isEqualTo(130_000L);
        assertThat(d.reserveWithheldCent()).isEqualTo(3_000L);   // 30000×10%
        assertThat(d.payoutCent()).isEqualTo(27_000L);
        // 发放额恒等：payout = principal_net − arrears − custom − reserve。
        assertThat(d.payoutCent()).isEqualTo(
                d.principalNetCent() - d.arrearsOffsetCent() - d.customDeductionCent() - d.reserveWithheldCent());
    }

    @Test
    void customDeductionsClampToAfterArrearsBudget() {
        // 税后 16 万；欠款抵扣 6 万 → 余 10 万；自定义扣 8 万 + 5 万，合计封顶 10 万 → 实发 0。
        DividendMath.Distribution d = DividendMath.compute(
                1_000_000L, 8000, 2000, false, 0L, true, 60_000L,
                new long[] {80_000L, 50_000L}, false, 0, 0L);
        assertThat(d.arrearsOffsetCent()).isEqualTo(60_000L);
        assertThat(d.customDeductionCent()).isEqualTo(100_000L);   // 封顶到 (160000−60000)
        assertThat(d.payoutCent()).isZero();
    }
}
