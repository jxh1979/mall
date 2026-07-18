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
}
