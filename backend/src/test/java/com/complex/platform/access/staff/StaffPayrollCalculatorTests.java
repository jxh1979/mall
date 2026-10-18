package com.complex.platform.access.staff;

// 文件版本：V0.01

import static com.complex.platform.access.staff.StaffPayrollModels.*;
import static org.assertj.core.api.Assertions.*;
import com.complex.platform.common.api.ApiException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class StaffPayrollCalculatorTests {
    Component component(String code,String type,String method,long amount,String leave) {
        return new Component(code,code,type,method,amount,"month",leave,"已批准计薪依据");
    }
    @Test void performanceReductionDoesNotConsumeCashPenaltyAllowance() {
        var rules=new PerformanceRules(new BigDecimal("100"),BigDecimal.ZERO,new BigDecimal("200"),
            Map.of("A",new BigDecimal("1.2"),"B",BigDecimal.ONE,"C",new BigDecimal("0.8")),"达标",2,1,Map.of(),null);
        var r=StaffPayrollCalculator.calculate(List.of(component("base","base","fixed_monthly",500000,"proportional"),
            component("performance","performance","performance",100000,"proportional")),List.of(),new BigDecimal("80"),rules,"B",0,3600,120000,2000);
        assertThat(r.performanceCent()).isEqualTo(80000);
        assertThat(r.penaltyCent()).isEqualTo(100000);
        assertThat(r.beforeTaxCent()).isEqualTo(480000);
    }
    @Test void sameLeaveAlreadyInAttendanceFormulaIsOnlyShownOnce() {
        var r=StaffPayrollCalculator.calculate(List.of(component("base","base","attendance_ratio",600000,"proportional")),
            List.of(new ComponentFact("base",new BigDecimal("9"),new BigDecimal("10"),60000,List.of("leave1"),"9个核实出勤日")),
            null,null,null,3600,36000,0,2000);
        assertThat(r.leaveDeductionCent()).isEqualTo(60000);
        assertThat(r.lines().getFirst().additionalLeaveCent()).isZero();
        assertThat(r.beforeTaxCent()).isEqualTo(540000);
    }
    @Test void anotherComponentNotAlreadyDeductedStillUsesItsOwnLeaveRatio() {
        var r=StaffPayrollCalculator.calculate(List.of(component("base","base","attendance_ratio",600000,"proportional"),
            component("travel","allowance","fixed_monthly",100000,"proportional")),
            List.of(new ComponentFact("base",new BigDecimal("9"),new BigDecimal("10"),60000,List.of("leave1"),"出勤")),
            null,null,null,3600,36000,0,2000);
        assertThat(r.lines().get(1).additionalLeaveCent()).isEqualTo(10000);
        assertThat(r.beforeTaxCent()).isEqualTo(630000);
    }
    @Test void allowancesAndBonusDoNotIncreaseCashCap() {
        var r=StaffPayrollCalculator.calculate(List.of(component("base","base","fixed_monthly",500000,"proportional"),
            component("bonus","bonus","fixed_monthly",100000,"proportional"),
            component("food","allowance","fixed_monthly",100000,"proportional")),List.of(),null,null,null,0,3600,180000,2000);
        assertThat(r.penaltyCapCent()).isEqualTo(100000);
        assertThat(r.beforeTaxCent()).isEqualTo(600000);
    }
    @Test void fullLeaveCannotMakePayNegative() {
        var r=StaffPayrollCalculator.calculate(List.of(component("base","base","fixed_monthly",500000,"proportional")),
            List.of(),null,null,null,3600,3600,200000,2000);
        assertThat(r.beforeTaxCent()).isZero();
    }
    @Test void missingQuantityCannotBeTreatedAsZero() {
        assertThatThrownBy(()->StaffPayrollCalculator.calculate(List.of(component("base","base","attendance_ratio",500000,"proportional")),
            List.of(),null,null,null,0,3600,0,2000)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PAYROLL_REVIEW_REQUIRED"));
    }
    @Test void leaveClaimNeedsRealLeaveReference() {
        assertThatThrownBy(()->StaffPayrollCalculator.calculate(List.of(component("base","base","attendance_ratio",600000,"proportional")),
            List.of(new ComponentFact("base",new BigDecimal("9"),new BigDecimal("10"),60000,List.of(),"出勤")),
            null,null,null,3600,36000,0,2000)).isInstanceOf(ApiException.class);
    }
    @Test void crossingMidnightUsesActualDatesButRetainsShiftEndMonth() {
        var start=Instant.parse("2026-01-31T14:00:00Z");
        var end=Instant.parse("2026-01-31T22:00:00Z");
        var segments=StaffPayrollCalculator.segments(List.of(new WorkInterval("a","shift",start,end,"2026-02")),
            Map.of(LocalDate.of(2026,1,31),"statutory_holiday",LocalDate.of(2026,2,1),"workday"),ZoneId.of("Asia/Shanghai"));
        assertThat(segments).extracting(WorkSegment::seconds).containsExactly(7200L,21600L);
        assertThat(segments).extracting(WorkSegment::kind).containsExactly("statutory_holiday","workday");
        assertThat(segments).allMatch(s->"2026-02".equals(s.payrollMonth()));
    }
    @Test void overlappingCoverAndPhoneIntervalsCannotDoubleCount() {
        var from=Instant.parse("2026-01-31T01:00:00Z");
        var result=StaffPayrollCalculator.segments(List.of(new WorkInterval("a","a",from,from.plusSeconds(3600),"2026-01"),
            new WorkInterval("b","b",from.plusSeconds(1800),from.plusSeconds(5400),"2026-01")),
            Map.of(LocalDate.of(2026,1,31),"workday"),ZoneId.of("Asia/Shanghai"));
        assertThat(result.stream().mapToLong(WorkSegment::seconds).sum()).isEqualTo(5400);
    }
    @Test void missingCalendarIsPendingRatherThanNormalWorkingDay() {
        assertThatThrownBy(()->StaffPayrollCalculator.segments(List.of(new WorkInterval("a","a",Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T01:00:00Z"),"2026-01")),Map.of(),ZoneId.of("Asia/Shanghai"))).isInstanceOf(ApiException.class);
    }
    @Test void dueDateMovesBackAndHonorsAdjustedWorkingWeekend() {
        var due=LocalDate.of(2026,1,5);
        assertThat(StaffPayrollCalculator.dueDate(due,null,Map.of(due,"statutory_holiday",due.minusDays(1),"adjusted_workday"))).isEqualTo(due.minusDays(1));
    }
    @Test void earlierLawfulDeadlineWinsOverOrdinaryAgreement() {
        var agreed=LocalDate.of(2026,1,10);var legal=LocalDate.of(2026,1,8);
        assertThat(StaffPayrollCalculator.dueDate(agreed,legal,Map.of(legal,"workday"))).isEqualTo(legal);
    }
    @Test void nominalRevocationDoesNotOverpayWhenOtherPenaltyStillHitsCap() {
        assertThat(StaffPayrollCalculator.actualCorrection(20000,20000,40000,0)).isZero();
        assertThat(StaffPayrollCalculator.actualCorrection(20000,20000,0,0)).isEqualTo(20000);
        assertThat(StaffPayrollCalculator.actualCorrection(20000,20000,0,20000)).isZero();
    }

    private PeriodFacts workFacts(String month,LocalDate start,LocalDate end,long standard) {
        return new PeriodFacts("facts","user",month,"project",null,"payer","scheme","calendar",List.of(),null,
            start,end,standard,Map.of(),end.plusDays(5),null,"已批准实际周期","approved","review",List.of(),"actor",Instant.EPOCH);
    }
    @Test void dailyOvertimeBelongsOnlyToTheActualLaterShiftAcrossPayrollMonths() {
        var terms=new WorkTimeTerms("standard",480,2400,1000,15000,20000,30000,"已核制度","已核计薪基数");
        var date=LocalDate.of(2026,1,31);
        var segments=List.of(new WorkSegment("early",date,"workday",8*3600,"2026-01"),
            new WorkSegment("late",date,"workday",4*3600,"2026-02"),
            new WorkSegment("late",date.plusDays(1),"workday",4*3600,"2026-02"));
        var facts=workFacts("2026-01",date.withDayOfMonth(1),date,0);
        assertThat(StaffPayrollCalculator.overtime(segments,terms,facts,date.plusDays(1),"2026-01").ordinarySeconds()).isZero();
        assertThat(StaffPayrollCalculator.overtime(segments,terms,facts,date.plusDays(1),"2026-02").ordinarySeconds()).isEqualTo(4*3600);
    }
    @Test void weeklyExcessIsAllocatedByActualSegmentsAndNotDuplicatedAcrossMonths() {
        var terms=new WorkTimeTerms("standard",480,2400,1000,15000,20000,30000,"已核制度","已核计薪基数");
        var start=LocalDate.of(2026,1,26);List<WorkSegment> segments=new ArrayList<>();
        for(int i=0;i<5;i++)segments.add(new WorkSegment("normal"+i,start.plusDays(i),"workday",8*3600,"2026-01"));
        segments.add(new WorkSegment("early",start.plusDays(5),"adjusted_workday",4*3600,"2026-01"));
        segments.add(new WorkSegment("late",start.plusDays(5),"adjusted_workday",2*3600,"2026-02"));
        var facts=workFacts("2026-01",start,start.plusDays(6),0);
        assertThat(StaffPayrollCalculator.overtime(segments,terms,facts,start.plusDays(6),"2026-01").ordinarySeconds()).isEqualTo(4*3600);
        assertThat(StaffPayrollCalculator.overtime(segments,terms,facts,start.plusDays(6),"2026-02").ordinarySeconds()).isEqualTo(2*3600);
    }
    @Test void incompleteComprehensiveCycleKeepsHolidayPayAndMarksOrdinaryHoursPending() {
        var terms=new WorkTimeTerms("comprehensive",0,0,1000,15000,20000,30000,"真实批准的综合工时","真实计薪依据");
        var start=LocalDate.of(2026,1,1);var end=LocalDate.of(2026,3,31);
        var facts=workFacts("2026-01",start,end,24000);
        var result=StaffPayrollCalculator.overtime(List.of(new WorkSegment("holiday",start,"statutory_holiday",7200,"2026-01"),
            new WorkSegment("ordinary",start.plusDays(1),"workday",36000,"2026-01")),terms,facts,start.plusDays(30),"2026-01");
        assertThat(result.holidayOvertimeCent()).isEqualTo(6000);
        assertThat(result.ordinaryOvertimeCent()).isZero();
        assertThat(result.pendingItems()).anyMatch(p->"WORKTIME_CYCLE_PENDING".equals(p.code()));
    }
}
