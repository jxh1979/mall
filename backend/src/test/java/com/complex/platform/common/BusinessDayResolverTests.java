package com.complex.platform.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * 经营日归属与窗口。所有断言都写成「北京几点几分的单归哪个经营日」——业务人员就是这么问的。
 *
 * <p>2026-08-02 口径:日切小时按<b>所在地(北京)</b>时间读(LocalZone),此前锚 UTC。下面的时刻特意挑在
 * 两种口径会给出不同答案的那 8 小时里,把 LocalZone.ID 改回 UTC 这些断言必红。
 */
class BusinessDayResolverTests {

    private final BusinessDayResolver resolver = new BusinessDayResolver(4);

    @Test
    void orderBeforeCutoffBelongsToPreviousBusinessDay() {
        // 北京 2026-06-18 03:00(< 04:00 日切)→ 归前一经营日 06-17。
        assertThat(resolver.businessDate(Instant.parse("2026-06-17T19:00:00Z")))
                .isEqualTo(LocalDate.parse("2026-06-17"));
    }

    @Test
    void orderAtOrAfterCutoffBelongsToSameBusinessDay() {
        // 北京 2026-06-18 05:00(>= 04:00 日切)→ 归当日 06-18;锚 UTC 会记成 06-18 之前的那天。
        assertThat(resolver.businessDate(Instant.parse("2026-06-17T21:00:00Z")))
                .isEqualTo(LocalDate.parse("2026-06-18"));
        // 恰好北京 04:00 边界 → 归当日。
        assertThat(resolver.businessDate(Instant.parse("2026-06-17T20:00:00Z")))
                .isEqualTo(LocalDate.parse("2026-06-18"));
    }

    @Test
    void windowBoundsAreCutoffShiftedOnLocalCalendarDay() {
        // 经营日 06-18 = 北京 06-18 04:00(含)~ 06-19 04:00(不含)。
        LocalDate day = LocalDate.parse("2026-06-18");
        assertThat(resolver.startInstant(day)).isEqualTo(Instant.parse("2026-06-17T20:00:00Z"));
        assertThat(resolver.endInstant(day)).isEqualTo(Instant.parse("2026-06-18T20:00:00Z"));
    }

    @Test
    void barSixAmCutoffIsReadInLocalTime() {
        // 酒吧 06:00 日切指北京早六点(晚 6 点到次日早 6 点算一天),不是 UTC 早六点(=北京下午两点)。
        BusinessDayResolver bar = new BusinessDayResolver(6);
        // 北京 6/18 07:00(已过日切)开的单归 6/18 营业日;锚 UTC 会记到 6/17,酒吧当天账少一笔、前一天多一笔。
        assertThat(bar.businessDate(Instant.parse("2026-06-17T23:00:00Z")))
                .isEqualTo(LocalDate.parse("2026-06-18"));
        // 北京 6/18 05:00(未到日切)仍归 6/17 营业日——夜场跨零点不切账,这是 06:00 日切的本意。
        assertThat(bar.businessDate(Instant.parse("2026-06-17T21:00:00Z")))
                .isEqualTo(LocalDate.parse("2026-06-17"));
    }

    @Test
    void windowRoundTripsBackToTheSameBusinessDay() {
        // 守恒:窗口起止两端反查回来必须还是同一个经营日(businessDate 与 startInstant 互为逆运算)。
        LocalDate day = LocalDate.parse("2026-06-18");
        assertThat(resolver.businessDate(resolver.startInstant(day))).isEqualTo(day);
        assertThat(resolver.businessDate(resolver.endInstant(day).minusSeconds(1))).isEqualTo(day);
    }

    @Test
    void cutoffZeroFallsBackToNaturalLocalDay() {
        BusinessDayResolver midnight = new BusinessDayResolver(0);
        // 北京 06-18 11:00 → 自然日 06-18。
        assertThat(midnight.businessDate(Instant.parse("2026-06-18T03:00:00Z")))
                .isEqualTo(LocalDate.parse("2026-06-18"));
        // 自然日起点 = 北京 00:00 = 前一日 16:00Z。
        assertThat(midnight.startInstant(LocalDate.parse("2026-06-18")))
                .isEqualTo(Instant.parse("2026-06-17T16:00:00Z"));
    }

    @Test
    void domainCutoffFallsBackToGlobalWhenConfigUnavailable() {
        // S0:配置表缺席(单测/未迁移库)按域回落全局切分,fail-safe 不阻塞日结。
        BusinessDayResolver resolver = new BusinessDayResolver(4);
        assertThat(resolver.cutoffHourFor("bar")).isEqualTo(4);
        assertThat(resolver.businessDate("bar", Instant.parse("2026-06-18T05:00:00Z")))
                .isEqualTo(resolver.businessDate(Instant.parse("2026-06-18T05:00:00Z")));
        assertThat(resolver.startInstant("bar", LocalDate.parse("2026-06-18")))
                .isEqualTo(resolver.startInstant(LocalDate.parse("2026-06-18")));
    }

    @Test
    void invalidCutoffIsRejected() {
        assertThatThrownBy(() -> new BusinessDayResolver(24))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BusinessDayResolver(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
