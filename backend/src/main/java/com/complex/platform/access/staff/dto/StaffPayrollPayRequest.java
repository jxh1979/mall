package com.complex.platform.access.staff.dto;

/** 资金安全①:发放某员工某月已定格薪资单。 */
public record StaffPayrollPayRequest(
        String idempotencyKey,
        String month,
        String userId,
        String payMethod,
        String payRef
) {
}
