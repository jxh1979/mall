package com.complex.platform.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BusinessDayResolverTests {

    private final BusinessDayResolver resolver = new BusinessDayResolver(4);

    @Test
    void orderBeforeCutoffBelongsToPreviousBusinessDay() {
        // 凌晨 03:00 UTC（< 04:00 日切）→ 归前一经营日。
        Instant at3am = Instant.parse("2026-06-18T03:00:00Z");
        assertThat(resolver.businessDate(at3am)).isEqualTo(LocalDate.parse("2026-06-17"));
    }

    @Test
    void orderAtOrAfterCutoffBelongsToSameBusinessDay() {
        // 05:00 UTC（>= 04:00 日切）→ 归当日经营日。
        Instant at5am = Instant.parse("2026-06-18T05:00:00Z");
        assertThat(resolver.businessDate(at5am)).isEqualTo(LocalDate.parse("2026-06-18"));
        // 恰好 04:00 边界 → 归当日。
        Instant at4am = Instant.parse("2026-06-18T04:00:00Z");
        assertThat(resolver.businessDate(at4am)).isEqualTo(LocalDate.parse("2026-06-18"));
    }

    @Test
    void windowBoundsAreCutoffShifted() {
        LocalDate day = LocalDate.parse("2026-06-18");
        assertThat(resolver.startInstant(day)).isEqualTo(Instant.parse("2026-06-18T04:00:00Z"));
        assertThat(resolver.endInstant(day)).isEqualTo(Instant.parse("2026-06-19T04:00:00Z"));
    }

    @Test
    void cutoffZeroFallsBackToNaturalUtcDay() {
        BusinessDayResolver midnight = new BusinessDayResolver(0);
        assertThat(midnight.businessDate(Instant.parse("2026-06-18T03:00:00Z")))
                .isEqualTo(LocalDate.parse("2026-06-18"));
        assertThat(midnight.startInstant(LocalDate.parse("2026-06-18")))
                .isEqualTo(Instant.parse("2026-06-18T00:00:00Z"));
    }

    @Test
    void invalidCutoffIsRejected() {
        assertThatThrownBy(() -> new BusinessDayResolver(24))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BusinessDayResolver(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
