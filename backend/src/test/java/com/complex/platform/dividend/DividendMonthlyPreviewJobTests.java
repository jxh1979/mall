package com.complex.platform.dividend;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** 定时作业薄包装单测：开关关不跑；开关开跑上月预览。不触数据库（service 为 mock）。 */
class DividendMonthlyPreviewJobTests {

    // 固定时钟：2026-08-15 → 上月 = 2026-07。
    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void disabledFlagSkipsRun() {
        DividendService service = mock(DividendService.class);
        new DividendMonthlyPreviewJob(service, FIXED, false).scheduledPreview();
        verify(service, never()).runScheduledMonthlyPreview(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void enabledFlagRunsPreviousMonthPreview() {
        DividendService service = mock(DividendService.class);
        new DividendMonthlyPreviewJob(service, FIXED, true).scheduledPreview();
        verify(service).runScheduledMonthlyPreview(YearMonth.of(2026, 7));
    }
}
