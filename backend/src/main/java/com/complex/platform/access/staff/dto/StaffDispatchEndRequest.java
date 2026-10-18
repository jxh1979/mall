package com.complex.platform.access.staff.dto;

/** CG-07 结束派驻：结束日期（YYYY-MM-DD，空＝当日）与事由。 */
public record StaffDispatchEndRequest(
        String idempotencyKey,
        String endDate,
        String reason
) {
}
