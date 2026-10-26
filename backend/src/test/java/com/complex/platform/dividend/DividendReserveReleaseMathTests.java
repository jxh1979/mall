package com.complex.platform.dividend;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * 分红发放保护线留存·释放纯算法单测（无 DB / 无 Spring）：
 * 释放额（全额 vs 比例 / 截断 / 负值钳零 / 非法比例）+ 到期判定（各释放模式 / 未到期不释放）。
 */
class DividendReserveReleaseMathTests {

    // ── 释放额：全额 vs 比例 ──────────────────────────────────

    @Test
    void releasableFullEqualsReserve() {
        // 默认口径：全额释放（=原留存额，不多不少）。
        assertThat(DividendReserveReleaseMath.releasableCent(16_000L, DividendReserveReleaseMath.FULL_RATIO_BPS))
                .isEqualTo(16_000L);
        assertThat(DividendReserveReleaseMath.releasableCent(0L, DividendReserveReleaseMath.FULL_RATIO_BPS))
                .isZero();
    }

    @Test
    void releasableByRatioTruncatesTowardZero() {
        // 比例释放（整除链截断，与薪资/分红同口径）。
        assertThat(DividendReserveReleaseMath.releasableCent(16_000L, 5000)).isEqualTo(8_000L);
        assertThat(DividendReserveReleaseMath.releasableCent(999L, 3333)).isEqualTo(332L); // 999*3333/10000=332.9→332
        assertThat(DividendReserveReleaseMath.releasableCent(16_000L, 0)).isZero();
    }

    @Test
    void releasableClampsNegativeReserveToZero() {
        assertThat(DividendReserveReleaseMath.releasableCent(-100L, DividendReserveReleaseMath.FULL_RATIO_BPS))
                .isZero();
    }

    @Test
    void releasableRejectsOutOfRangeRatio() {
        assertThatThrownBy(() -> DividendReserveReleaseMath.releasableCent(100L, 10001))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DividendReserveReleaseMath.releasableCent(100L, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── 到期判定：各模式 / 未到期不释放 ───────────────────────

    @Test
    void manualModeNeverAutoReleases() {
        LocalDate periodEnd = LocalDate.parse("2026-03-31");
        assertThat(DividendReserveReleaseMath.isDue("manual", periodEnd, 1, "quarterly",
                LocalDate.parse("2030-01-01"))).isFalse();
    }

    @Test
    void nextPeriodDueAfterPeriodEnd() {
        LocalDate periodEnd = LocalDate.parse("2026-03-31");
        // 期末当日尚未到期（进入下一期=期末之后）。
        assertThat(DividendReserveReleaseMath.isDue("next_period", periodEnd, 1, "quarterly", periodEnd)).isFalse();
        assertThat(DividendReserveReleaseMath.isDue("next_period", periodEnd, 1, "quarterly",
                LocalDate.parse("2026-04-01"))).isTrue();
    }

    @Test
    void fixedDelayDueAfterNPeriods() {
        LocalDate periodEnd = LocalDate.parse("2026-03-31"); // 季度方案：每期 3 月
        // 延后 2 期 = 6 月 → 到期日 2026-09-30 之后才释放。
        assertThat(DividendReserveReleaseMath.isDue("fixed_delay", periodEnd, 2, "quarterly",
                LocalDate.parse("2026-09-30"))).isFalse();
        assertThat(DividendReserveReleaseMath.isDue("fixed_delay", periodEnd, 2, "quarterly",
                LocalDate.parse("2026-10-01"))).isTrue();
        // 月度方案：每期 1 月，延后 2 期 = 2 月。
        LocalDate monthEnd = LocalDate.parse("2026-03-31");
        assertThat(DividendReserveReleaseMath.isDue("fixed_delay", monthEnd, 2, "monthly",
                LocalDate.parse("2026-05-31"))).isFalse();
        assertThat(DividendReserveReleaseMath.isDue("fixed_delay", monthEnd, 2, "monthly",
                LocalDate.parse("2026-06-01"))).isTrue();
    }

    /** BSM L1777 分红周期取半年、年：固定延后 N 期按每期 6 / 12 个月换算，到期日落在对应期末。 */
    @Test
    void fixedDelayUsesSemiannualAndAnnualPeriodLengths() {
        LocalDate halfEnd = LocalDate.parse("2026-06-30");
        assertThat(DividendReserveReleaseMath.isDue("fixed_delay", halfEnd, 1, "semiannual",
                LocalDate.parse("2026-12-31"))).isFalse();
        assertThat(DividendReserveReleaseMath.isDue("fixed_delay", halfEnd, 1, "semiannual",
                LocalDate.parse("2027-01-01"))).isTrue();
        LocalDate yearEnd = LocalDate.parse("2026-12-31");
        assertThat(DividendReserveReleaseMath.isDue("fixed_delay", yearEnd, 1, "annual",
                LocalDate.parse("2027-12-31"))).isFalse();
        assertThat(DividendReserveReleaseMath.isDue("fixed_delay", yearEnd, 1, "annual",
                LocalDate.parse("2028-01-01"))).isTrue();
    }

    @Test
    void annualDueAfterYearEnd() {
        LocalDate periodEnd = LocalDate.parse("2026-06-30");
        assertThat(DividendReserveReleaseMath.isDue("annual", periodEnd, 1, "quarterly",
                LocalDate.parse("2026-12-31"))).isFalse();
        assertThat(DividendReserveReleaseMath.isDue("annual", periodEnd, 1, "quarterly",
                LocalDate.parse("2027-01-01"))).isTrue();
    }

    @Test
    void nullOrUnknownInputsAreNotDue() {
        assertThat(DividendReserveReleaseMath.isDue("next_period", null, 1, "quarterly",
                LocalDate.parse("2027-01-01"))).isFalse();
        assertThat(DividendReserveReleaseMath.isDue("unknown_mode", LocalDate.parse("2026-03-31"), 1, "quarterly",
                LocalDate.parse("2030-01-01"))).isFalse();
    }
}
