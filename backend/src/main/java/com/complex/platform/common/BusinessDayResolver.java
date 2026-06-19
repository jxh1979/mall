package com.complex.platform.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 经营日（business day）切分助手。财务确认口径（2026-06-19）：经营日切时点可后台配置，默认 04:00。
 *
 * <p>一笔发生在 {@code instant} 的流水归属的经营日 = {@code instant.minusHours(cutoffHour).toLocalDate()}（UTC）。
 * 即凌晨 04:00 之前的流水归前一经营日，04:00 及之后归当日。所有 T7 日结六源汇总与 T12 看板 ETL
 * 的"经营日"分桶统一走本助手，替换原先的自然日（UTC date）口径。
 *
 * <p>cutoffHour 通过 {@code finance.business-day-cutoff-hour} 配置（默认 4，合法范围 0–23）。
 */
@Component
public class BusinessDayResolver {

    private final int cutoffHour;

    public BusinessDayResolver(
            @Value("${finance.business-day-cutoff-hour:4}") int cutoffHour
    ) {
        if (cutoffHour < 0 || cutoffHour > 23) {
            throw new IllegalArgumentException(
                    "finance.business-day-cutoff-hour must be between 0 and 23, was " + cutoffHour);
        }
        this.cutoffHour = cutoffHour;
    }

    /** 配置的日切小时（默认 4）。 */
    public int cutoffHour() {
        return cutoffHour;
    }

    /** 把某一时刻归集到其所属经营日。 */
    public LocalDate businessDate(Instant instant) {
        return instant.minusSeconds(cutoffHour * 3600L)
                .atZone(ZoneOffset.UTC)
                .toLocalDate();
    }

    /** 经营日的起始时刻（含），= 经营日 00:00 UTC + cutoff 小时。 */
    public Instant startInstant(LocalDate businessDate) {
        return businessDate.atStartOfDay(ZoneOffset.UTC)
                .plusHours(cutoffHour)
                .toInstant();
    }

    /** 经营日的结束时刻（不含），= 次日 00:00 UTC + cutoff 小时。 */
    public Instant endInstant(LocalDate businessDate) {
        return businessDate.plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .plusHours(cutoffHour)
                .toInstant();
    }
}
