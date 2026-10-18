package com.complex.platform.access.staff;

// 文件版本：V0.01

import java.time.Instant;
import java.util.Comparator;

/** 复用原在线打卡规则：前后12小时候选，覆盖优先，否则取距起止最近的班。 */
final class StaffAttendanceShiftMatcher {
    private static final long WINDOW_SECONDS = 12 * 3600L;
    private StaffAttendanceShiftMatcher() {}

    static StaffShiftRecord match(JdbcStaffRepository repository,String userId,String punchType,Instant at) {
        var candidates=repository.listShiftsForUserOverlapping(userId,at.minusSeconds(WINDOW_SECONDS),at.plusSeconds(WINDOW_SECONDS));
        var covering=candidates.stream().filter(s->!at.isBefore(s.startAt()) && !at.isAfter(s.endAt())).toList();
        return (covering.isEmpty()?candidates:covering).stream().min(Comparator.comparingLong(s->Math.abs(
                ("check_in".equals(punchType)?s.startAt():s.endAt()).toEpochMilli()-at.toEpochMilli()))).orElse(null);
    }
}
