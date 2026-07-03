package com.complex.platform.access.staff;

import java.time.Instant;

/** F批(2026-07-02 拍板):月结定格薪资单——total = base + commission + bonus - penalty。 */
public record StaffPayrollStatementRecord(
        String id,
        String userId,
        String month,
        long baseCent,
        long commissionCent,
        long bonusCent,
        long penaltyCent,
        long taxCent,
        long totalCent,
        String closedBy,
        Instant closedAt
) {
}
