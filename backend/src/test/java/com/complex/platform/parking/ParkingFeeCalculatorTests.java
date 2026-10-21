package com.complex.platform.parking;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

/** HP-P01 R3：BSM L1434「免费时长、超出后按小时计费（不足一小时按一小时）、每日封顶」；营业日切段（ENUM「按营业日归集」）。 */
class ParkingFeeCalculatorTests {

    // 营业日在 UTC 20:00（北京 04:00）切换。
    private static final UnaryOperator<Instant> DAY_END = t -> {
        Instant cut = t.truncatedTo(java.time.temporal.ChronoUnit.DAYS).plus(Duration.ofHours(20));
        return t.isBefore(cut) ? cut : cut.plus(Duration.ofDays(1));
    };
    private static final Instant ENTER = Instant.parse("2026-10-21T02:00:00Z");

    @Test
    void freeMinutesThenCeilHourly() {
        assertThat(fee(31, 0)).isEqualTo(500);
        assertThat(fee(91, 0)).isEqualTo(1000);
        assertThat(fee(30, 0)).isZero();
    }

    @Test
    void dailyCapAppliesPerBusinessDay() {
        // 02:30 起计费到次日 23:00：当日段 17.5 小时→封顶 3000；次日段 03:00 小时→1500。
        Instant until = Instant.parse("2026-10-21T23:00:00Z");
        long cent = ParkingFeeCalculator.quote(ENTER, until, 30, 500, 3000, 0, DAY_END).grossFeeCent();
        assertThat(cent).isEqualTo(3000 + 1500);
    }

    @Test
    void creditMinutesOffsetBeforeBillingAndReportOffsetHours() {
        // 停 4 小时、免费 30 分、抵免 120 分：不抵 4 小时（3.5→4）2000；抵后 1.5→2 小时 1000，实际抵掉 2 小时。
        ParkingFeeCalculator.Quote q = ParkingFeeCalculator.quote(ENTER, ENTER.plus(Duration.ofHours(4)), 30, 500, 100000, 120, DAY_END);
        assertThat(q.grossFeeCent()).isEqualTo(2000);
        assertThat(q.netFeeCent()).isEqualTo(1000);
        assertThat(q.offsetHours()).isEqualTo(2);
        // 免费时长内离场：没有可抵的计费时长。
        assertThat(ParkingFeeCalculator.quote(ENTER, ENTER.plus(Duration.ofMinutes(20)), 30, 500, 100000, 120, DAY_END).offsetHours())
                .isZero();
    }

    /** 第十二批裁决补充第 11 条「按实际少收的钱算」：封顶下抵免前后同价＝一分没少收，不算抵掉；7 小时只少收 1 小时的钱按 1 小时。 */
    @Test
    void offsetHoursFollowActualShortfallUnderDailyCap() {
        // 停 10 小时、免费 30 分、抵免 120 分、封顶 30 元：不抵 9.5→10 小时、抵后 7.5→8 小时都封顶 3000，一分没少收。
        ParkingFeeCalculator.Quote same = ParkingFeeCalculator.quote(ENTER, ENTER.plus(Duration.ofHours(10)), 30, 500, 3000, 120, DAY_END);
        assertThat(same.grossFeeCent()).isEqualTo(3000);
        assertThat(same.netFeeCent()).isEqualTo(3000);
        assertThat(same.offsetHours()).isZero();
        // 停 7 小时：不抵 6.5→7 小时封顶 3000、抵后 4.5→5 小时 2500，计费小时差 2 但只少收 500＝1 小时。
        ParkingFeeCalculator.Quote seven = ParkingFeeCalculator.quote(ENTER, ENTER.plus(Duration.ofHours(7)), 30, 500, 3000, 120, DAY_END);
        assertThat(seven.grossFeeCent() - seven.netFeeCent()).isEqualTo(500);
        assertThat(seven.offsetHours()).isEqualTo(1);
        // 少收金额不足一小时费率按一小时；不超过抵免覆盖的计费小时差；没少收为 0。
        assertThat(ParkingFeeCalculator.offsetHours(300, 500, 2)).isEqualTo(1);
        assertThat(ParkingFeeCalculator.offsetHours(5000, 500, 2)).isEqualTo(2);
        assertThat(ParkingFeeCalculator.offsetHours(0, 500, 2)).isZero();
    }

    private static long fee(int stayMinutes, int creditMinutes) {
        return ParkingFeeCalculator.quote(ENTER, ENTER.plus(Duration.ofMinutes(stayMinutes)), 30, 500, 100000, creditMinutes, DAY_END)
                .grossFeeCent();
    }
}
