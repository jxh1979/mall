package com.complex.platform.access.staff.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** E1 排班请求(同员工同开始时刻幂等覆盖)。 */
public record StaffShiftRequest(
        String idempotencyKey,
        String userId,
        LocalDate workDate,
        Instant startAt,
        Instant endAt,
        List<String> pointSpaceIds,
        String note
) {
}
