package com.complex.platform.access;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 第十八批 CN 段第 372 条第 3 段 372-S3-R1（第十八批裁决 77「各清单、明细、导出只看得到这段时间发生的记录」）：期间整段 andPeriod、
 * 整月 andMonth 与内存筛选 covers 三个助手。真实接口路径由 AuditDataWindowSettlementJdbcIntegrationTests 覆盖。
 */
class AuditDataWindowTests {

    @AfterEach
    void reset() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void withoutWindowHelpersAddNothing() {
        window(null);
        assertThat(AuditDataWindow.andPeriod("s", "e")).isEmpty();
        assertThat(AuditDataWindow.andMonth("month")).isEmpty();
        assertThat(AuditDataWindow.covers(Instant.parse("1999-01-01T00:00:00Z"))).isTrue();
    }

    @Test
    void periodWholeMonthAndCoversFollowWindowInBeijingDays() {
        window(new AuditDataWindow(LocalDate.parse("2026-01-15"), LocalDate.parse("2026-12-31")));
        assertThat(AuditDataWindow.andPeriod("s", "e")).isEqualTo(" AND s >= DATE '2026-01-15' AND e <= DATE '2026-12-31'");
        assertThat(AuditDataWindow.andMonth("month")).isEqualTo(" AND month BETWEEN '2026-02' AND '2026-12'");
        assertThat(AuditDataWindow.covers(Instant.parse("2026-01-14T16:00:00Z"))).isTrue();
        assertThat(AuditDataWindow.covers(Instant.parse("2026-01-14T15:59:59Z"))).isFalse();
        assertThat(AuditDataWindow.covers(Instant.parse("2026-12-31T15:59:59Z"))).isTrue();
        assertThat(AuditDataWindow.covers(Instant.parse("2026-12-31T16:00:00Z"))).isFalse();
        assertThat(AuditDataWindow.covers(null)).isTrue();
    }

    @Test
    void wholeMonthKeepsMonthStartingOnFirstDayAndDropsMonthCutMidway() {
        window(new AuditDataWindow(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31")));
        assertThat(AuditDataWindow.andMonth("month")).isEqualTo(" AND month BETWEEN '2026-01' AND '2026-12'");
        window(new AuditDataWindow(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-06-15")));
        assertThat(AuditDataWindow.andMonth("month")).isEqualTo(" AND month BETWEEN '2026-01' AND '2026-05'");
    }

    @Test
    void windowWithoutAnyWholeMonthMatchesNoMonth() {
        window(new AuditDataWindow(LocalDate.parse("2026-01-10"), LocalDate.parse("2026-01-20")));
        assertThat(AuditDataWindow.andMonth("month")).isEqualTo(" AND FALSE");
    }

    private static void window(AuditDataWindow window) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (window != null) {
            request.setAttribute(AuditDataWindow.ATTRIBUTE, window);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
