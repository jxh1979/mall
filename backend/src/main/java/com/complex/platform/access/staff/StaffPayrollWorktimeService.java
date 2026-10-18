package com.complex.platform.access.staff;

// 文件版本：V0.02

import static com.complex.platform.access.staff.StaffPayrollModels.*;
import com.complex.platform.common.LocalZone;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** 原始领取、归还及核时结果决定实际工时；排班仅用于应出勤和事假交集。 */
@Service
class StaffPayrollWorktimeService {
    private final JdbcTemplate jdbc;
    private final JdbcStaffRepository staff;
    private final StaffWorkPhoneAttendanceService phones;
    StaffPayrollWorktimeService(JdbcTemplate jdbc,JdbcStaffRepository staff,StaffWorkPhoneAttendanceService phones) {
        this.jdbc=jdbc;this.staff=staff;this.phones=phones;
    }
    record Facts(List<WorkInterval> intervals,List<String> attendanceIds,List<String> leaveIds,
            long scheduledSeconds,long leaveSeconds,Map<String,Long> leaveSecondsById,WorkResult result) {}
    Facts collect(Scheme scheme,PeriodFacts facts,CalendarVersion calendar,LocalDate asOf) {
        String user=scheme.userId();
        Instant start=facts.cycleStart().atStartOfDay(LocalZone.ID).toInstant();
        Instant end=facts.cycleEnd().plusDays(1).atStartOfDay(LocalZone.ID).toInstant();
        if("standard".equals(scheme.worktimeTerms().system())) {
            var month=YearMonth.parse(facts.month());
            start=month.atDay(1).with(java.time.temporal.WeekFields.ISO.dayOfWeek(),1).atStartOfDay(LocalZone.ID).toInstant();
            end=month.atEndOfMonth().with(java.time.temporal.WeekFields.ISO.dayOfWeek(),7).plusDays(1).atStartOfDay(LocalZone.ID).toInstant();
        }
        // 待审方案只在 staff_shift_revisions 中；本表是审批发布或已安排顶班后的有效班次原件。
        List<String> ids=jdbc.queryForList("SELECT id FROM staff_shift_schedules WHERE user_id=? AND end_at>? AND start_at<? ORDER BY start_at,id",
            String.class,user,java.sql.Timestamp.from(start),java.sql.Timestamp.from(end));
        List<PendingItem> pending=new ArrayList<>();List<WorkInterval> intervals=new ArrayList<>();List<String> attendance=new ArrayList<>();
        List<WorkInterval> planned=new ArrayList<>();Set<String> usedLeaves=new TreeSet<>();List<WorkInterval> leaveParts=new ArrayList<>();
        // 2026-10-18 裁决：申请销假待主管批准(revoke_requested)的事假仍有效，照常参与扣薪；批准转 revoked 后才按 BSM L1481 冲正。
        var leaves=jdbc.queryForList("SELECT id,start_date,end_date FROM staff_leave_requests WHERE user_id=? AND leave_type='personal' AND status IN ('approved','revoke_requested') ORDER BY start_date,id",user);
        for(String id:ids) {
            var shift=staff.findShiftById(id).orElseThrow();
            String month=YearMonth.from(shift.endAt().atZone(LocalZone.ID)).toString();
            if(month.equals(facts.month())) {
                planned.add(new WorkInterval(id,id,shift.startAt(),shift.endAt(),month));
                for(var leave:leaves) {
                    Instant from=((java.sql.Date)leave.get("start_date")).toLocalDate().atStartOfDay(LocalZone.ID).toInstant();
                    Instant to=((java.sql.Date)leave.get("end_date")).toLocalDate().plusDays(1).atStartOfDay(LocalZone.ID).toInstant();
                    from=from.isAfter(shift.startAt())?from:shift.startAt();to=to.isBefore(shift.endAt())?to:shift.endAt();
                    if(from.isBefore(to)){usedLeaves.add((String)leave.get("id"));leaveParts.add(new WorkInterval((String)leave.get("id"),id,from,to,month));}
                }
            }
            if(phones.hasPendingTimeEvidence(user,shift)) {
                pending.add(new PendingItem("ATTENDANCE_TIME_PENDING",id,"本班领取时间待核实，相关加班等待补结"));continue;
            }
            var punches=jdbc.queryForList("SELECT id,punch_type,punched_at FROM staff_attendance_records WHERE user_id=? AND shift_id=? ORDER BY punched_at,id",user,id);
            Instant from=null,to=null;
            for(var row:punches) {
                Instant at=((java.sql.Timestamp)row.get("punched_at")).toInstant();
                attendance.add((String)row.get("id"));
                if("check_in".equals(row.get("punch_type"))&&from==null)from=at;
                if("check_out".equals(row.get("punch_type")))to=at;
            }
            if(from==null||to==null||!from.isBefore(to)) {
                pending.add(new PendingItem("ATTENDANCE_INTERVAL_PENDING",id,"本班实际领取、归还时段尚未完整核实"));continue;
            }
            if(to.isAfter(Instant.now())) {
                pending.add(new PendingItem("ATTENDANCE_INTERVAL_PENDING",id,"本班尚未实际结束"));continue;
            }
            // 综合周期按实际日期边界分段，跨周期的同一班次不重复计入。
            if("comprehensive".equals(scheme.worktimeTerms().system())) {
                if(from.isBefore(start))from=start;
                if(to.isAfter(end))to=end;
            }
            if(from.isBefore(to))intervals.add(new WorkInterval(id,id,from,to,month));
        }
        if(ids.isEmpty())pending.add(new PendingItem("WORKTIME_ROSTER_PENDING",facts.id(),"核定周期尚无真实已发布班次，工时项目保留待核"));
        Map<LocalDate,String> days=StaffPayrollConfigurationService.calendarMap(calendar.days());
        List<WorkSegment> segments;
        WorkResult result;
        try {
            segments=StaffPayrollCalculator.segments(intervals,days,LocalZone.ID);
            // 不完整的周期不能用部分已知工时判断总超时；法定假日已核时段可以独立先计。
            boolean incomplete=!pending.isEmpty();
            if(incomplete) {
                long holiday=segments.stream().filter(s->s.payrollMonth().equals(facts.month())&&"statutory_holiday".equals(s.kind())).mapToLong(WorkSegment::seconds).sum();
                result=new WorkResult(segments,0,0,holiday,0,0,StaffPayrollCalculator.overtimeMoney(holiday,scheme.worktimeTerms().hourlyBaseCent(),scheme.worktimeTerms().holidayOvertimeBps()),List.copyOf(pending));
            } else {
                var computed=StaffPayrollCalculator.overtime(segments,scheme.worktimeTerms(),facts,asOf,facts.month());
                pending.addAll(computed.pendingItems());
                result=new WorkResult(segments,computed.ordinarySeconds(),computed.restDaySeconds(),computed.holidaySeconds(),
                    computed.ordinaryOvertimeCent(),computed.restDayOvertimeCent(),computed.holidayOvertimeCent(),List.copyOf(pending));
            }
        }catch(com.complex.platform.common.api.ApiException e) {
            if(!"PAYROLL_REVIEW_REQUIRED".equals(e.code()))throw e;
            pending.add(new PendingItem("WORKTIME_BASIS_PENDING",facts.id(),e.getMessage()));
            result=new WorkResult(List.of(),0,0,0,0,0,0,List.copyOf(pending));
        }
        for(var f:facts.componentFacts())if(f.appliedLeaveIds()!=null&&!usedLeaves.containsAll(f.appliedLeaveIds()))
            throw StaffPayrollCalculator.pending("公式内事假扣减必须引用本员工本期批准且实际重合的请假记录");
        // BSM L1481：逐条事假的重合工时存入快照；定格后销假时按原快照扣除该假工时复算，不回改原单。
        Map<String,Long> leaveSecondsById=new TreeMap<>();
        for(String leave:usedLeaves)leaveSecondsById.put(leave,unionSeconds(leaveParts.stream().filter(p->leave.equals(p.sourceId())).toList()));
        return new Facts(List.copyOf(intervals),List.copyOf(attendance),List.copyOf(usedLeaves),unionSeconds(planned),unionSeconds(leaveParts),Collections.unmodifiableMap(leaveSecondsById),result);
    }
    private long unionSeconds(List<WorkInterval> input) {
        long total=0;Instant covered=null;
        for(var i:input.stream().sorted(Comparator.comparing(WorkInterval::from).thenComparing(WorkInterval::to)).toList()) {
            Instant from=covered==null||i.from().isAfter(covered)?i.from():covered;
            if(from.isBefore(i.to()))total=Math.addExact(total,Duration.between(from,i.to()).getSeconds());
            if(covered==null||i.to().isAfter(covered))covered=i.to();
        }
        return total;
    }
}
