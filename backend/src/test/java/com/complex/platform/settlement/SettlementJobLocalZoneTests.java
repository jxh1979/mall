package com.complex.platform.settlement;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 2026-08-02 所在地时区口径:分账归属月与对账日按<b>北京日历</b>判定。
 *
 * <p>全 mock、不触库、过账开关关——本用例只验"哪个月/哪一日",分账瀑布、守恒校验、金额精度、
 * 配置中心规则一概不进入(真账逻辑的验收在 SettlementMonthlyJobPostingTests / 各 S 系列用例)。
 */
class SettlementJobLocalZoneTests {

    /** 北京 2026-08-01 04:00 = UTC 2026-07-31 20:00:两种口径分属不同月/不同日的那 8 小时。 */
    private static final Clock BEIJING_AUG_FIRST_EARLY =
            Clock.fixed(Instant.parse("2026-07-31T20:00:00Z"), ZoneOffset.UTC);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void monthlyJobAttributesToPreviousLocalMonth() {
        TicketDistributionSettlementService service = mock(TicketDistributionSettlementService.class);
        JdbcSettlementBillRepository repository = mock(JdbcSettlementBillRepository.class);
        SettlementPostingService posting = mock(SettlementPostingService.class);
        // 作业开、过账关。
        SettlementMonthlyJob job = new SettlementMonthlyJob(
                service, repository, posting, BEIJING_AUG_FIRST_EARLY, true, false);

        job.scheduledSettle();

        // 北京 8/1 凌晨跑 → 归属月(上月)= 2026-07。锚 UTC 时这一刻还在 7/31,"上月"会算成 2026-06:
        // 7 月整月不进回看窗口的最新一格,商户当月分账单迟一轮才出。
        verify(service).settleMonth(null, YearMonth.of(2026, 7));
        // 过账闸关着:一分钱都不该落台账。
        verifyNoInteractions(posting);
    }

    @Test
    void reconciliationDayAndWindowAreLocalCalendarDay() {
        JdbcSettlementTransactionRepository repository = mock(JdbcSettlementTransactionRepository.class);

        new SettlementReconciliationJob(repository, BEIJING_AUG_FIRST_EARLY).scheduledReconcile();

        // 北京 8/1 凌晨跑 → 对账"前一日" = 北京 7/31,窗口 = 北京 7/31 00:00(含)~ 8/1 00:00(不含)。
        // 锚 UTC 时既对错了日子(7/30),窗口也与标的日期错开 8 小时,财务照单核对不上。
        verify(repository).sumOldTicketAllocatedCent(
                Instant.parse("2026-07-30T16:00:00Z"), Instant.parse("2026-07-31T16:00:00Z"));
        verify(repository).sumNewSettlementCent(
                Instant.parse("2026-07-30T16:00:00Z"), Instant.parse("2026-07-31T16:00:00Z"));
        verify(repository).upsertReconciliationStatus(
                eq(LocalDate.of(2026, 7, 31)), anyLong(), anyLong(), anyLong());
    }
}
