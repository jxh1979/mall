package com.complex.platform.access.staff;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** E1(2026-07-02 拍板):班次到分钟+指定点位(主管排,双端)。 */
public record StaffShiftRecord(
        String id,
        String userId,
        LocalDate workDate,
        Instant startAt,
        Instant endAt,
        List<String> pointSpaceIds,
        String note,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
