package com.complex.platform.access.staff;

// 文件版本：V0.01

import static com.complex.platform.access.staff.StaffPayrollModels.*;
import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

/** 已批准输入夹具，只用于原算法/查询回归；真实审批及核税证据链由 Workflow/TaxVersion 测试单独覆盖。 */
final class PayrollApprovedCloseFixture {
    private PayrollApprovedCloseFixture() {}
    static String key(){return UUID.randomUUID().toString().replace("-","");}
    static String actor(){
        Object p=SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(p instanceof AccessActor a)return a.id();
        if(p instanceof AuthService.UserRecord u)return u.id();
        throw new IllegalStateException("测试须提供真实员工会话");
    }
    static void prepare(JdbcTemplate jdbc,ObjectMapper json,String user,String month,long base) {
        var rows=jdbc.queryForList("SELECT project_id,merchant_id,company_id FROM staff_profiles WHERE user_id=? ORDER BY created_at DESC",user);
        if(rows.isEmpty())throw new IllegalStateException("测试须明确员工档案及发薪主体："+user);
        var p=rows.getFirst();
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now()) ON CONFLICT(id) DO NOTHING",user,user,p.get("project_id"),p.get("merchant_id"));
        PayrollApprovedFactsFixture.createForSubject(jdbc,json,user,(String)p.get("project_id"),(String)p.get("merchant_id"),(String)p.get("company_id"),actor(),month,base,0,null);
    }
    static StatementDetail closeWithApprovedZeroTax(JdbcTemplate jdbc,ObjectMapper json,StaffPayrollService payroll,String user,String month,String kind) {
        String facts=jdbc.queryForObject("SELECT id FROM staff_payroll_period_facts WHERE user_id=? AND month=? AND status='approved' ORDER BY created_at DESC,id LIMIT 1",String.class,user,month);
        var calc=payroll.calculate(new SettleRequest(key(),user,month,kind,facts,null));
        String tax=approvedZeroTax(jdbc,json,calc);
        return payroll.settle(new SettleRequest(key(),user,month,kind,facts,null,calc.id(),tax));
    }
    static String approvedZeroTax(JdbcTemplate jdbc,ObjectMapper json,CalculationDraft calc) {
        String version="v_"+key(),category="fixture_"+key(),id="tax_"+key();
        LocalDate date=YearMonth.parse(calc.month()).atEndOfMonth();
        var bracket=new PayrollTaxVersion.Bracket(1,0L,null,10000,0L,0,0L);
        var calculation=new PayrollTaxAssessment.Calculation(version,"V0.01",category,date,"professional_confirmed","本用例明确批准税额0分",null,calc.beforeTaxCent(),null,null,null,0);
        jdbc.update("INSERT INTO payroll_tax_versions(id,category,version,effective_from,applicability_basis,calculation_basis,rounding_rule,brackets,reason,status,created_by,created_at,decided_at) VALUES(?,?,'V0.01',?,'测试已核身份','本用例专业税额原件','truncate_cent_each_step',?::jsonb,'已批准输入夹具','approved',?,now(),now())",version,category,date,encode(json,List.of(bracket)),actor());
        jdbc.update("INSERT INTO payroll_tax_assessments(id,user_id,month,project_id,merchant_id,payer_company_id,category,tax_date,identity_basis,transaction_basis,professional_basis,income_cent,source_lines,source_fingerprint,tax_version_id,tax_cent,calculation_snapshot,reason,status,created_by,created_at,decided_at,payroll_calculation_id,professional_calculation_reference) VALUES(?,?,?,?,?,?,?,?,'测试已核身份','本次完整工资','本用例明确批准0分税额',?,?::jsonb,?,?,0,?::jsonb,'已批准输入夹具','approved',?,now(),now(),?,'测试专业原件')",
            id,calc.userId(),calc.month(),calc.projectId(),calc.merchantId(),calc.payerCompanyId(),category,date,calc.beforeTaxCent(),
            encode(json,List.of(new PayrollTaxAssessment.SourceLine(calc.id(),calc.id(),calc.beforeTaxCent()))),calc.sourceFingerprint(),version,encode(json,calculation),actor(),calc.id());
        return id;
    }
    static String encode(ObjectMapper json,Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);}}
}
