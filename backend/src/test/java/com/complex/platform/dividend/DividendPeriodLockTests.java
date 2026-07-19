package com.complex.platform.dividend;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.dividend.DividendRecords.DividendStatementRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 纯函数关账判定单测：月单/季单覆盖 3 月/preview 不锁/非终态不锁/voided 解锁/边界月。 */
class DividendPeriodLockTests {

    private static boolean locks(String kind, String status, String start, String end, String month) {
        return DividendPeriodLock.locks(kind, status,
                LocalDate.parse(start), LocalDate.parse(end), YearMonth.parse(month));
    }

    @Test
    void monthlyApprovedLocksItsOwnMonthOnly() {
        assertThat(locks("official", "approved", "2026-07-01", "2026-07-31", "2026-07")).isTrue();
        assertThat(locks("official", "approved", "2026-07-01", "2026-07-31", "2026-06")).isFalse();
        assertThat(locks("official", "approved", "2026-07-01", "2026-07-31", "2026-08")).isFalse();
    }

    @Test
    void quarterlyPaidLocksAllThreeMonthsIncludingBoundaries() {
        // 2026-Q3 = 07/08/09。
        assertThat(locks("official", "paid", "2026-07-01", "2026-09-30", "2026-07")).isTrue();
        assertThat(locks("official", "paid", "2026-07-01", "2026-09-30", "2026-08")).isTrue();
        assertThat(locks("official", "paid", "2026-07-01", "2026-09-30", "2026-09")).isTrue();
        // 相邻季度的边界月不锁。
        assertThat(locks("official", "paid", "2026-07-01", "2026-09-30", "2026-06")).isFalse();
        assertThat(locks("official", "paid", "2026-07-01", "2026-09-30", "2026-10")).isFalse();
    }

    @Test
    void previewNeverLocks() {
        assertThat(locks("preview", "approved", "2026-07-01", "2026-07-31", "2026-07")).isFalse();
        assertThat(locks("preview", "paid", "2026-07-01", "2026-09-30", "2026-08")).isFalse();
    }

    @Test
    void nonTerminalStatusesDoNotLock() {
        for (String status : List.of("draft", "pending_review", "rejected", "voided")) {
            assertThat(locks("official", status, "2026-07-01", "2026-09-30", "2026-08"))
                    .as("status %s must not lock", status).isFalse();
        }
    }

    @Test
    void voidedApprovedStatementUnlocks() {
        // 曾 approved 的单被作废后（status=voided）天然解锁——判定式不含 voided。
        assertThat(locks("official", "voided", "2026-07-01", "2026-09-30", "2026-08")).isFalse();
    }

    @Test
    void isLockedScansStatementCollection() {
        DividendStatementRecord approvedQuarter = stmt("official", "approved", "2026-07-01", "2026-09-30");
        DividendStatementRecord draftMonth = stmt("official", "draft", "2026-10-01", "2026-10-31");
        List<DividendStatementRecord> statements = List.of(draftMonth, approvedQuarter);
        assertThat(DividendPeriodLock.isLocked(statements, YearMonth.parse("2026-08"))).isTrue();
        assertThat(DividendPeriodLock.isLocked(statements, YearMonth.parse("2026-10"))).isFalse();
        assertThat(DividendPeriodLock.isLocked(List.of(), YearMonth.parse("2026-08"))).isFalse();
    }

    private static DividendStatementRecord stmt(String kind, String status, String start, String end) {
        Instant now = Instant.now();
        return new DividendStatementRecord("s", "m", "p", kind, "lbl",
                LocalDate.parse(start), LocalDate.parse(end), "u", 8000, 2000,
                List.of("bar"), true, List.of("labor"),
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, status,
                null, null, null, null, null, null, null, null, null, now, now);
    }
}
