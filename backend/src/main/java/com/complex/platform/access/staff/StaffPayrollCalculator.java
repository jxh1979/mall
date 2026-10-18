package com.complex.platform.access.staff;

// 文件版本：V0.01

import static com.complex.platform.access.staff.StaffPayrollModels.*;
import com.complex.platform.common.api.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.WeekFields;
import java.util.*;

/** 只计算批准输入；不读取当前配置，不把缺失专业依据当零元。 */
final class StaffPayrollCalculator {
    private StaffPayrollCalculator() {}
    record Earnings(List<Line> lines,long baseCent,long allowanceCent,long performanceCent,long bonusCent,
            long leaveDeductionCent,long penaltyRequestedCent,long penaltyCapCent,long penaltyCent,long beforeTaxCent) {}

    static Earnings calculate(List<Component> components,List<ComponentFact> facts,BigDecimal points,
            PerformanceRules performance,String grade,long leaveSeconds,long scheduledSeconds,
            long requestedPenalty,int capBps) {
        if(capBps<0 || capBps>10000 || requestedPenalty<0 || leaveSeconds<0 || scheduledSeconds<0 || leaveSeconds>scheduledSeconds)
            throw invalid("工资比例或扣款依据不合法");
        Map<String,ComponentFact> byCode=new HashMap<>();
        for(var fact:facts)if(byCode.put(fact.componentCode(),fact)!=null)throw invalid("同一薪资项目不能重复输入");
        List<Line> lines=new ArrayList<>();
        long base=0,allowance=0,perf=0,bonus=0,leave=0,total=0;
        Set<String> codes=new HashSet<>();
        for(var component:components) {
            if(!codes.add(component.code()) || component.amountCent()<0)throw invalid("薪资项目重复或金额为负");
            var fact=byCode.get(component.code());
            BigDecimal quantity=fact==null?null:fact.quantity();
            BigDecimal denominator=fact==null?null:fact.denominator();
            long embedded=fact==null?0:fact.embeddedLeaveCent();
            if(embedded<0)throw invalid("公式内事假扣减不能为负");
            long formula=switch(component.method()) {
                case "fixed_monthly" -> component.amountCent();
                case "per_unit" -> {
                    if(quantity==null || quantity.signum()<0)throw pending("项目缺少已核实计发数量："+component.label());
                    yield money(BigDecimal.valueOf(component.amountCent()).multiply(quantity));
                }
                case "attendance_ratio" -> {
                    if(quantity==null || denominator==null || quantity.signum()<0 || denominator.signum()<=0 || quantity.compareTo(denominator)>0)
                        throw pending("项目缺少已核实出勤日期和约定折算分母："+component.label());
                    yield money(BigDecimal.valueOf(component.amountCent()).multiply(quantity).divide(denominator,12,RoundingMode.HALF_UP));
                }
                case "performance" -> {
                    if(performance==null || points==null || performance.basePoints().signum()<=0 || grade==null
                            || !performance.gradeCoefficients().containsKey(grade))throw pending("绩效分数、档次或批准系数尚未确认");
                    BigDecimal actual=points.max(BigDecimal.ZERO);
                    yield money(BigDecimal.valueOf(component.amountCent()).multiply(actual)
                        .divide(performance.basePoints(),12,RoundingMode.HALF_UP).multiply(performance.gradeCoefficients().get(grade)));
                }
                default -> throw invalid("不支持的薪资计算方式");
            };
            if(embedded>0 && !Set.of("attendance_ratio","per_unit").contains(component.method()))
                throw invalid("只有已按实际数量折算的项目可登记公式内事假扣减");
            if(embedded>0 && (fact.appliedLeaveIds()==null || fact.appliedLeaveIds().isEmpty()))
                throw pending("公式内事假扣减必须关联真实请假记录");
            long gross=Math.addExact(formula,embedded);
            long due=switch(component.leaveMode()) {
                case "exempt" -> {
                    if(embedded!=0)throw invalid("不扣事假的项目不能登记公式内扣减");
                    yield 0L;
                }
                case "proportional" -> scheduledSeconds==0?0:ratio(gross,leaveSeconds,scheduledSeconds);
                default -> throw invalid("不支持的事假处理方式");
            };
            if(embedded>due)throw invalid("公式内事假扣减超过本项目同次事假应扣金额");
            long additional=Math.min(formula,Math.max(0,due-embedded));
            long amount=formula-additional;
            leave=Math.addExact(leave,Math.addExact(embedded,additional));
            total=Math.addExact(total,amount);
            switch(component.type()) {
                case "base" -> base=Math.addExact(base,gross);
                case "performance" -> perf=Math.addExact(perf,formula);
                case "bonus" -> bonus=Math.addExact(bonus,formula);
                case "allowance","full_attendance" -> allowance=Math.addExact(allowance,formula);
                default -> throw invalid("不支持的薪资项目类别");
            }
            Map<String,Object> snapshot=new LinkedHashMap<>();
            snapshot.put("method",component.method());snapshot.put("configured_amount_cent",component.amountCent());
            snapshot.put("leave_mode",component.leaveMode());snapshot.put("leave_seconds",leaveSeconds);
            snapshot.put("scheduled_seconds",scheduledSeconds);
            if(fact!=null)snapshot.put("fact",fact);
            if("performance".equals(component.method())) {
                snapshot.put("points",points);snapshot.put("grade",grade);snapshot.put("rules",performance);
            }
            lines.add(new Line(null,component.code(),component.type(),component.label(),gross,embedded,additional,amount,
                "salary_component",component.code(),snapshot));
        }
        if(components.stream().noneMatch(c->"base".equals(c.type())))throw pending("薪资方案须明确基础工资项目，不能缺项当零");
        long cap=ratio(base,capBps,10000);
        long applied=Math.min(requestedPenalty,cap);
        return new Earnings(List.copyOf(lines),base,allowance,perf,bonus,leave,requestedPenalty,cap,applied,Math.max(0,total-applied));
    }

    static List<WorkSegment> segments(List<WorkInterval> intervals,Map<LocalDate,String> calendar,ZoneId zone) {
        var sorted=intervals.stream().sorted(Comparator.comparing(WorkInterval::from).thenComparing(WorkInterval::to)).toList();
        List<WorkSegment> result=new ArrayList<>();
        Instant covered=null;
        for(var interval:sorted) {
            if(!interval.from().isBefore(interval.to()))throw pending("实际上下班时段尚未核实");
            Instant cursor=covered==null || interval.from().isAfter(covered)?interval.from():covered;
            if(!cursor.isBefore(interval.to()))continue;
            while(cursor.isBefore(interval.to())) {
                var date=cursor.atZone(zone).toLocalDate();
                String kind=calendar.get(date);
                if(kind==null)throw pending("缺少已确认日期性质："+date);
                Instant next=date.plusDays(1).atStartOfDay(zone).toInstant();
                Instant end=next.isBefore(interval.to())?next:interval.to();
                result.add(new WorkSegment(interval.shiftId(),date,kind,Duration.between(cursor,end).getSeconds(),interval.payrollMonth()));
                cursor=end;
            }
            if(covered==null || interval.to().isAfter(covered))covered=interval.to();
        }
        return List.copyOf(result);
    }

    static WorkResult overtime(List<WorkSegment> segments,WorkTimeTerms terms,PeriodFacts facts,
            LocalDate asOf,String payrollMonth) {
        if(terms==null || terms.hourlyBaseCent()<0 || ("standard".equals(terms.system()) && (terms.standardDailyMinutes()<=0 || terms.standardWeeklyMinutes()<=0)))
            throw pending("适用工时制度及计薪基数尚未核实");
        long holiday=segments.stream().filter(s->s.payrollMonth().equals(payrollMonth)&&"statutory_holiday".equals(s.kind())).mapToLong(WorkSegment::seconds).sum();
        long ordinary=0,rest=0;
        List<PendingItem> pending=new ArrayList<>();
        if("comprehensive".equals(terms.system())) {
            if(asOf.isBefore(facts.cycleEnd())) {
                pending.add(new PendingItem("WORKTIME_CYCLE_PENDING",facts.id(),"综合工时周期尚未结束；本月已确认工资和法定节假日工时正常计发"));
            } else {
                long worked=segments.stream().filter(s->!"statutory_holiday".equals(s.kind())).mapToLong(WorkSegment::seconds).sum();
                ordinary=Math.max(0,worked-Math.multiplyExact(facts.cycleStandardMinutes(),60));
            }
        } else if("standard".equals(terms.system())) {
            Map<LocalDate,List<WorkSegment>> days=new TreeMap<>();
            for(var segment:segments) {
                if("statutory_holiday".equals(segment.kind()))continue;
                if("rest_day".equals(segment.kind())) {
                    if(segment.payrollMonth().equals(payrollMonth))rest=Math.addExact(rest,segment.seconds());
                } else days.computeIfAbsent(segment.date(),ignored->new ArrayList<>()).add(segment);
            }
            for(var entry:facts.restCompensatedMinutes().entrySet()) {
                if(entry.getValue()<0)throw invalid("调休抵扣工时不能为负");
                long actual=segments.stream().filter(s->s.date().equals(entry.getKey())&&"rest_day".equals(s.kind())&&s.payrollMonth().equals(payrollMonth)).mapToLong(WorkSegment::seconds).sum();
                long compensated=Math.multiplyExact(entry.getValue(),60);
                if(compensated>actual)throw invalid("已批准调休工时超过对应休息日实际工作工时");
                rest-=compensated;
            }
            Map<LocalDate,Long> weeks=new TreeMap<>();
            for(var entry:days.entrySet()) {
                long dailyQuota=Math.multiplyExact(terms.standardDailyMinutes(),60),dayUsed=0;
                LocalDate week=entry.getKey().with(WeekFields.ISO.dayOfWeek(),1);
                for(var segment:entry.getValue()) {
                    long normal=Math.min(segment.seconds(),Math.max(0,dailyQuota-dayUsed));
                    long dailyExcess=segment.seconds()-normal;
                    long before=weeks.getOrDefault(week,0L),weeklyQuota=Math.multiplyExact(terms.standardWeeklyMinutes(),60);
                    long weeklyExcess=Math.max(0,Math.addExact(before,normal)-weeklyQuota)-Math.max(0,before-weeklyQuota);
                    if(segment.payrollMonth().equals(payrollMonth))ordinary=Math.addExact(ordinary,Math.addExact(dailyExcess,weeklyExcess));
                    weeks.put(week,Math.addExact(before,normal));dayUsed=Math.addExact(dayUsed,segment.seconds());
                }
            }
        } else if(!"flexible".equals(terms.system()))throw invalid("不支持的工时制度");
        return new WorkResult(segments,ordinary,rest,holiday,
            overtimeMoney(ordinary,terms.hourlyBaseCent(),terms.ordinaryOvertimeBps()),
            overtimeMoney(rest,terms.hourlyBaseCent(),terms.restDayOvertimeBps()),
            overtimeMoney(holiday,terms.hourlyBaseCent(),terms.holidayOvertimeBps()),List.copyOf(pending));
    }

    static LocalDate dueDate(LocalDate agreed,LocalDate statutory,Map<LocalDate,String> calendar) {
        LocalDate due=statutory!=null && statutory.isBefore(agreed)?statutory:agreed;
        for(int count=0;count<370;count++,due=due.minusDays(1)) {
            String kind=calendar.get(due);
            if(kind==null)throw pending("工资提前支付日期缺少已确认日历："+due);
            if(Set.of("workday","adjusted_workday").contains(kind))return due;
        }
        throw pending("工资支付日历没有可确定的最近工作日");
    }
    static long actualCorrection(long originalPenalty,long originalCap,long remainingPenalty,long alreadyRestored) {
        if(originalPenalty<0 || originalCap<0 || remainingPenalty<0 || alreadyRestored<0)throw invalid("原扣款复算依据不合法");
        return Math.max(0,originalPenalty-Math.min(remainingPenalty,originalCap)-alreadyRestored);
    }
    static long overtimeMoney(long seconds,long hourlyCent,int bps) {
        if(bps<0)throw invalid("加班计薪比例不能为负");
        return money(BigDecimal.valueOf(seconds).multiply(BigDecimal.valueOf(hourlyCent)).multiply(BigDecimal.valueOf(bps))
            .divide(BigDecimal.valueOf(3600L*10000),12,RoundingMode.HALF_UP));
    }
    static long ratio(long amount,long numerator,long denominator) {
        return money(BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(numerator)).divide(BigDecimal.valueOf(denominator),12,RoundingMode.HALF_UP));
    }
    static long money(BigDecimal value) {return value.setScale(0,RoundingMode.HALF_UP).longValueExact();}
    static ApiException invalid(String message){return ApiException.badRequest("PAYROLL_INPUT_INVALID",message);}
    static ApiException pending(String message){return ApiException.conflict("PAYROLL_REVIEW_REQUIRED",message);}
}
