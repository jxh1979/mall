package com.complex.platform.access.staff.dto;

/** CG-07 登记派驻：员工、派驻酒吧（商户）、起始日期（YYYY-MM-DD）；所属主体取自员工档案。 */
public record StaffDispatchCreateRequest(
        String idempotencyKey,
        String userId,
        String merchantId,
        String startDate,
        String note
) {
}
