package com.complex.platform.parking;

// 文件版本：V0.01

import java.time.Duration;
import java.time.Instant;
import java.util.function.UnaryOperator;

/**
 * HP-P01 停车费试算（BSM L1434「免费时长、超出后按小时计费（不足一小时按一小时）、每日封顶」；ENUM「全系统的钱账统一按营业日归集」）：
 * 入场起先扣免费时长（与可用的消费抵免时长），剩余计费时长按营业日切段，每段不足一小时按一小时×小时费率、再与每日封顶取小，逐段相加。
 * 与下发给停车系统的是同一套规则；道闸出场计费以停车系统为准，差额进对账。
 */
public final class ParkingFeeCalculator {

    private ParkingFeeCalculator() {
    }

    /**
     * grossFeeCent 不扣抵免；netFeeCent 扣抵免后；offsetHours＝抵免实际抵掉的计费小时数：按实际少收的钱折回小时
     * （第十二批操作员裁决补充第 11 条，见 {@link #offsetHours}），免费时长内离场、封顶下抵免前后同价均为 0。
     */
    public record Quote(long grossFeeCent, long netFeeCent, long offsetHours) {}

    /** dayEnd：给定时刻所属营业日的结束时刻（不含）。 */
    public static Quote quote(Instant enteredAt, Instant until, int freeMinutes, long hourlyRateCent, long dailyCapCent,
            int creditMinutes, UnaryOperator<Instant> dayEnd) {
        Instant billFrom = enteredAt.plus(Duration.ofMinutes(freeMinutes));
        long[] gross = fee(billFrom, until, hourlyRateCent, dailyCapCent, dayEnd);
        long[] net = fee(billFrom.plus(Duration.ofMinutes(creditMinutes)), until, hourlyRateCent, dailyCapCent, dayEnd);
        return new Quote(gross[0], net[0], offsetHours(gross[0] - net[0], hourlyRateCent, gross[1] - net[1]));
    }

    /**
     * 第十二批操作员裁决补充第 11 条「按实际少收的钱算」：一分没少收为 0；少收了按少收金额折回的小时数（不足一小时按一小时），
     * 不超过抵免覆盖的计费小时数 maxHours。
     */
    public static long offsetHours(long shortfallCent, long hourlyRateCent, long maxHours) {
        return shortfallCent <= 0 ? 0 : Math.min((shortfallCent + hourlyRateCent - 1) / hourlyRateCent, maxHours);
    }

    /** 返回 {金额, 计费小时数}。 */
    private static long[] fee(Instant from, Instant until, long hourlyRateCent, long dailyCapCent, UnaryOperator<Instant> dayEnd) {
        long cent = 0;
        long hours = 0;
        for (Instant start = from; start.isBefore(until); ) {
            Instant end = dayEnd.apply(start);
            Instant segmentEnd = end.isBefore(until) ? end : until;
            long seconds = Duration.between(start, segmentEnd).toSeconds();
            long segmentHours = (seconds + 3599) / 3600;
            hours += segmentHours;
            cent += Math.min(segmentHours * hourlyRateCent, dailyCapCent);
            start = segmentEnd;
        }
        return new long[] {cent, hours};
    }
}
