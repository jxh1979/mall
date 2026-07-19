package com.complex.platform.dividend;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** 留存自动释放定时作业薄包装单测：开关关不跑；开关开跑当日到期释放。不触数据库（service 为 mock）。 */
class DividendReserveReleaseJobTests {

    // 固定时钟：2026-04-15。
    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-04-15T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void disabledFlagSkipsRun() {
        DividendService service = mock(DividendService.class);
        new DividendReserveReleaseJob(service, FIXED, false).scheduledRelease();
        verify(service, never()).runScheduledReserveRelease(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void enabledFlagRunsReleaseForToday() {
        DividendService service = mock(DividendService.class);
        new DividendReserveReleaseJob(service, FIXED, true).scheduledRelease();
        verify(service).runScheduledReserveRelease(LocalDate.of(2026, 4, 15));
    }
}
