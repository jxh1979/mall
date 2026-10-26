package com.complex.platform.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * 营业日归属与窗口（无库＝出厂口径）。ENUM:187「营业日从当日上午 10:00 起算，至次日上午 10:00 止；其中次日凌晨 07:00 至上午 10:00 为
 * 非营业时段（已闭店），该时段内发生的记录仍归属正在进行的这一个营业日」；DEC-BIZDAY-001（原「建议默认 06:00」作废）。
 * 断言都写成「北京几点几分的单归哪个营业日」。
 */
// 文件版本：V0.02
class BusinessDayResolverTests {

    private final BusinessDayResolver resolver = new BusinessDayResolver();

    @Test
    void factoryBusinessDayStartsAtTenAmBeijing() {
        // 北京 11-02 09:30（未到 10:00）→ 前一营业日 11-01；09:59 同；10:00 起归当日。
        assertThat(resolver.businessDate(null, null, Instant.parse("2026-11-02T01:30:00Z"))).isEqualTo(LocalDate.parse("2026-11-01"));
        assertThat(resolver.businessDate("p", "b", Instant.parse("2026-11-02T01:59:00Z"))).isEqualTo(LocalDate.parse("2026-11-01"));
        assertThat(resolver.businessDate(null, null, Instant.parse("2026-11-02T02:00:00Z"))).isEqualTo(LocalDate.parse("2026-11-02"));
    }

    @Test
    void recordsAfterSevenAmCloseStillBelongToTheRunningBusinessDay() {
        // 北京 11-03 07:30（已闭店、未到次日起点）→ 仍是 11-02 营业日。
        assertThat(resolver.businessDate(null, null, Instant.parse("2026-11-02T23:30:00Z"))).isEqualTo(LocalDate.parse("2026-11-02"));
        // 月末最后一个营业日：北京 12-01 08:00 仍属 11-30，归 11 月。
        assertThat(resolver.businessDate(null, null, Instant.parse("2026-12-01T00:00:00Z"))).isEqualTo(LocalDate.parse("2026-11-30"));
    }

    @Test
    void factoryWindowRunsFromTenAmToNextTenAmAndRoundTrips() {
        LocalDate day = LocalDate.parse("2026-11-02");
        assertThat(resolver.start(null, null, day)).isEqualTo(Instant.parse("2026-11-02T02:00:00Z"));
        assertThat(resolver.end(null, null, day)).isEqualTo(Instant.parse("2026-11-03T02:00:00Z"));
        assertThat(resolver.businessDate(null, null, resolver.start(null, null, day))).isEqualTo(day);
        assertThat(resolver.businessDate(null, null, resolver.end(null, null, day).minusSeconds(1))).isEqualTo(day);
        assertThat(resolver.end(null, null, day)).isEqualTo(resolver.start(null, null, day.plusDays(1)));
    }

    @Test
    void withoutDatabaseObjectsHaveNoVersionsOrOwners() {
        assertThat(resolver.barOfMerchant("m")).isNull();
        assertThat(resolver.projectOfMerchant("m")).isNull();
        assertThat(resolver.barWindows(LocalDate.parse("2026-11-02"))).isEmpty();
    }
}
