package com.complex.platform.access.staff;

// 文件版本：V0.01

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** F批:考勤月视图日行——出勤/缺勤/迟到/早退/未打下班卡/请假，另有实际时间待核pending_review（不直接认缺勤）；无排班=休，同日多问题并列。 */
public record StaffAttendanceDayRecord(
        LocalDate date,
        List<String> dayStatus,
        Instant shiftStartAt,
        Instant shiftEndAt
) {
}
