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
        Instant closedAt,
        // CG-07：工资单发放主体＝人员所属主体（BSM「派驻人员的成本与报酬」：列入该人员的工资单由人员所属主体统一发放并代扣个人所得税）。
        String payerCompanyId
) {
    /** 兼容构造（12 参，无发放主体）。 */
    public StaffPayrollStatementRecord(String id, String userId, String month, long baseCent, long commissionCent,
            long bonusCent, long penaltyCent, long leaveDeductionCent, long taxCent, long totalCent,
            String closedBy, Instant closedAt) {
        this(id, userId, month, baseCent, commissionCent, bonusCent, penaltyCent, leaveDeductionCent, taxCent, totalCent,
                closedBy, closedAt, null);
    }
}
