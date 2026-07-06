package com.complex.platform.access.staff;

import java.time.Instant;

/** F批(2026-07-02 拍板):月结定格薪资单——
 *  total = base + commission + bonus - penalty - leaveDeduction - tax。
 *  leaveDeduction(2026-07-05 拍板):事假(无薪假)按日工资×天数扣;病假/年假/调休不扣。 */
public record StaffPayrollStatementRecord(
        String id,
        String userId,
        String month,
        long baseCent,
        long commissionCent,
        long bonusCent,
        long penaltyCent,
        long leaveDeductionCent,
        long taxCent,
        long totalCent,
        String closedBy,
        Instant closedAt
) {
}
