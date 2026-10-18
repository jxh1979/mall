package com.complex.platform.access.staff.dto;

/** CG-07 提成月结内部结算：月份（YYYY-MM）。 */
public record StaffCommissionSettlementCloseRequest(
        String idempotencyKey,
        String month
) {
}
