package com.complex.platform.access.staff;

import java.time.Instant;
import java.time.LocalDate;

/** F批:请假单(主管审;通过区间考勤月视图记「请假」)。 */
public record StaffLeaveRequestRecord(
        String id,
        String userId,
        String leaveType,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        String status,
        String reviewedBy,
        String reviewNote,
        Instant createdAt,
        Instant updatedAt
) {
}
