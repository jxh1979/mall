package com.complex.platform.access.staff;

// 文件版本：V0.02

import static com.complex.platform.access.staff.StaffPayrollModels.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;

/** 仅测试计算/银行边界的已批准原件夹具；真实方案审批在 PayrollWorkflowJdbcIntegrationTests 单独覆盖。 */
final class PayrollApprovedFactsFixture {
    record Source(String scheme,String calendar,String facts) {}
    static Source create(JdbcTemplate jdbc,ObjectMapper json,String user,String project,String actor,String month,long base,long performance) {
        return create(jdbc,json,user,project,actor,month,base,performance,new PerformanceFact(false,true,"测试目标达成事实"));
    }
    static Source create(JdbcTemplate jdbc,ObjectMapper json,String user,String project,String actor,String month,long base,long performance,PerformanceFact performanceFact) {
        return createForSubject(jdbc,json,user,project,null,project,actor,month,base,performance,performanceFact);
    }
    /** 已批方案内含已批分值表条目（rule_code→扣分）；分值表自身的总经理审批不在该夹具范围。 */
    static Source create(JdbcTemplate jdbc,ObjectMapper json,String user,String project,String actor,String month,long base,long performance,Map<String,BigDecimal> deductions) {
        return createForSubject(jdbc,json,user,project,null,project,actor,month,base,performance,new PerformanceFact(false,true,"测试目标达成事实"),deductions);
    }
    static Source createForSubject(JdbcTemplate jdbc,ObjectMapper json,String user,String project,String merchant,String payer,String actor,String month,long base,long performance,PerformanceFact performanceFact) {
        return createForSubject(jdbc,json,user,project,merchant,payer,actor,month,base,performance,performanceFact,Map.of());
    }
    static Source createForSubject(JdbcTemplate jdbc,ObjectMapper json,String user,String project,String merchant,String payer,String actor,String month,long base,long performance,PerformanceFact performanceFact,Map<String,BigDecimal> deductions) {
        String suffix=UUID.randomUUID().toString().replace("-","");
        String scheme="ps_"+suffix,calendar="pc_"+suffix,facts="pf_"+suffix;
        var ym=YearMonth.parse(month);
        var components=new ArrayList<Component>();
        components.add(new Component("base","基础工资","base","fixed_monthly",base,"month","proportional","测试批准月薪依据"));
        if(performance>0)components.add(new Component("performance","绩效工资","performance","performance",performance,"month","proportional","测试批准绩效公式"));
        var rule=performance>0?new PerformanceRules(BigDecimal.valueOf(100),BigDecimal.ZERO,BigDecimal.valueOf(200),
            Map.of("A",BigDecimal.valueOf(1.2),"B",BigDecimal.ONE,"C",BigDecimal.valueOf(.8)),"测试条件",2,1,deductions,null):null;
        var terms=new WorkTimeTerms("standard",480,2400,1000,15000,20000,30000,"测试依法核定制度，不作法律默认值","测试计薪依据");
        String version=StaffPayrollConfigurationService.version(jdbc.queryForObject("SELECT count(*)+1 FROM staff_payroll_scheme_versions WHERE user_id=?",Integer.class,user));
        jdbc.update("INSERT INTO staff_payroll_scheme_versions(id,user_id,project_id,merchant_id,payer_company_id,version,effective_from,position_code,components,performance_rules,worktime_terms,payment_basis,reason,status,created_by,approved_at) VALUES(?,?,?,?,?,?,?,'test_position',?::jsonb,?::jsonb,?::jsonb,'测试支付约定','测试已批原件','approved',?,now())",
            scheme,user,project,merchant,payer,version,ym.atDay(1),encode(json,components),encode(json,rule),encode(json,terms),actor);
        var days=new ArrayList<CalendarDay>();
        for(LocalDate d=ym.atDay(1).minusDays(7);!d.isAfter(ym.plusMonths(1).atDay(10));d=d.plusDays(1))days.add(new CalendarDay(d,"workday"));
        jdbc.update("INSERT INTO staff_payroll_calendar_versions(id,project_id,version,start_date,end_date,days,source_basis,reason,status,created_by,approved_at) VALUES(?,?,'V0.01',?,?,?::jsonb,'测试日历事实','测试已批日历','approved',?,now())",
            calendar,project,days.getFirst().date(),days.getLast().date(),encode(json,days),actor);
        jdbc.update("INSERT INTO staff_payroll_period_facts(id,user_id,month,project_id,merchant_id,payer_company_id,scheme_version_id,calendar_version_id,component_facts,performance_facts,cycle_start,cycle_end,cycle_standard_minutes,agreed_due_date,reason,status,created_by,approved_at) VALUES(?,?,?,?,?,?,?,?,'[]',?::jsonb,?,?,0,?,'测试已核项目依据','approved',?,now())",
            facts,user,month,project,merchant,payer,scheme,calendar,encode(json,performanceFact),ym.atDay(1),ym.atEndOfMonth(),ym.plusMonths(1).atDay(5),actor);
        return new Source(scheme,calendar,facts);
    }
    static String encode(ObjectMapper json,Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);}}
}
