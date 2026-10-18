package com.complex.platform.finance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.*;
import com.complex.platform.access.*;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.EvidenceEventService;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/** 仅此工资银行桥的真实 JDBC/附件测试；计薪审批完整链另由工资集成测试覆盖。 */
@SpringBootTest(properties={
    "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
    "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
    "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"})
@Transactional
class FinancePayrollJdbcIntegrationTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired AccessRepository access;
    @Autowired FinancePayrollService payroll;
    @Autowired FinanceService finance;
    @Autowired EvidenceEventService evidence;
    @Autowired com.complex.platform.analytics.AnalyticsRepository analytics;
    String project,user,actor,role,scheme,facts,calendar,wage;
    final Instant requested=Instant.parse("2026-01-01T10:00:00Z"), resultAt=Instant.parse("2026-01-01T10:01:00Z");
    String key(){return UUID.randomUUID().toString().replace("-","");}

    @BeforeEach void setup() {
        String suffix=key().substring(0,10);
        project="pay_prj_"+suffix;user="pay_user_"+suffix;actor="pay_fin_"+suffix;role="pay_role_"+suffix;
        scheme="pay_scheme_"+suffix;facts="pay_facts_"+suffix;calendar="pay_calendar_"+suffix;wage="pay_wage_"+suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','工资银行测试','active',now(),now())",project);
        access.createUser(new AccessUser(actor,actor,"staff",project,null,"active",Instant.now()));
        access.createUser(new AccessUser(user,user,"staff",project,null,"active",Instant.now()));
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,view_all_projects,created_at) VALUES(?,?,'工资银行测试明确授权','platform','active',true,now())",role,role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code LIKE 'finance.%' OR permission_code IN ('evidence.attachment.create','evidence.timeline.view')",role);
        access.assignUserRole(actor,role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AccessActor(actor,actor,"staff",project,null,"active"),"test",List.of()));
        jdbc.update("""
            INSERT INTO staff_profiles(id,project_id,user_id,name,company_id,registration_status,status,employment_status,created_at,updated_at)
            VALUES(?,?,?,'工资员工',?,'approved','active','employed',now(),now())
            ""","profile_"+suffix,project,user,project);
        jdbc.update("""
            INSERT INTO staff_payroll_scheme_versions(id,user_id,project_id,payer_company_id,version,effective_from,position_code,
             components,worktime_terms,payment_basis,reason,status,created_by,approved_at)
            VALUES(?,?,?,?,'V0.01','2026-01-01','test_position','[]','{}','已核工资支付日期','测试已批准来源','approved',?,now())
            """,scheme,user,project,project,actor);
        jdbc.update("""
            INSERT INTO staff_payroll_calendar_versions(id,project_id,version,start_date,end_date,days,source_basis,reason,status,created_by,approved_at)
            VALUES(?,?,'V0.01','2026-01-01','2026-12-31','[]','测试日历','已批准来源','approved',?,now())
            """,calendar,project,actor);
        jdbc.update("""
            INSERT INTO staff_payroll_period_facts(id,user_id,month,project_id,payer_company_id,scheme_version_id,calendar_version_id,
             component_facts,cycle_start,cycle_end,cycle_standard_minutes,agreed_due_date,reason,status,created_by,approved_at)
            VALUES(?,?,'2026-01',?,?,?,?,'[]','2026-01-01','2026-01-31',0,'2026-02-05','测试已批准期间','approved',?,now())
            """,facts,user,project,project,scheme,calendar,actor);
        statement(wage,"2026-01","monthly");
        payroll.ensurePayrollPayable(wage);
    }
    @AfterEach void cleanup(){SecurityContextHolder.clearContext();}

    void statement(String id,String month,String kind) {
        jdbc.update("""
            INSERT INTO staff_payroll_statements(id,user_id,month,base_cent,performance_cent,penalty_cent,total_cent,status,
              project_id,payer_company_id,scheme_version_id,facts_id,calculation_snapshot,payment_due_date,payment_due_basis,statement_kind)
            VALUES(?,?,?,500000,80000,100000,480000,'closed',?,?,?,?,?::jsonb,'2026-02-05',?::jsonb,?)
            """,id,user,month,project,project,scheme,facts,"{\"gross_cent\":480000}","{\"approved_facts\":\""+facts+"\"}",kind);
    }
    String prepared(long amount){return (String)payroll.prepare(wage,new FinancePayrollService.PrepareRequest(key(),amount)).get("id");}
    String upload(String id){return evidence.uploadAttachment(new MockMultipartFile("file","bank.png","image/png",
        Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jCYQAAAAASUVORK5CYII=")),"staff_payroll_payment",id,"bank_proof").id();}
    void submit(String id){payroll.submit(id,new FinancePayrollService.SubmitRequest(key(),"request_"+id,requested,user,List.of(upload(id))));}
    FinancePayrollService.ResultRequest result(String status,String flow,List<String> files){return new FinancePayrollService.ResultRequest(key(),status,resultAt,flow,"已核银行实际结果",files);}
    long paid(){return jdbc.queryForObject("SELECT paid_amount_cent FROM ar_ap_items WHERE source_type='staff_payroll' AND source_id=?",Long.class,wage);}
    void rejected(Runnable action,String code){assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo(code));}

    @Test void unknownBankResultDoesNotPayAndPreventsAnotherPayment() {
        String id=prepared(480000);submit(id);
        payroll.result(id,result("unknown",null,List.of(upload(id))));
        assertThat(paid()).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM staff_payroll_statements WHERE id=?",String.class,wage)).isEqualTo("closed");
        rejected(()->prepared(480000),"PAYROLL_PAYMENT_INVALID");
        rejected(()->payroll.result(id,result("cancelled",null,List.of())),"PAYROLL_PAYMENT_INVALID");
        assertThat(payroll.attempts(wage).attempts()).hasSize(1);
    }

    @Test void successfulPartialPaymentIsCountedOnceAndUpdatesOriginalWage() {
        String id=prepared(200000);submit(id);
        var request=result("succeeded","success_"+id,List.of(upload(id)));
        payroll.result(id,request);payroll.result(id,request);
        assertThat(paid()).isEqualTo(200000);
        assertThat(jdbc.queryForObject("SELECT paid_amount_cent FROM staff_payroll_statements WHERE id=?",Long.class,wage)).isEqualTo(200000);
        assertThat(jdbc.queryForObject("SELECT status FROM staff_payroll_statements WHERE id=?",String.class,wage)).isEqualTo("partial_paid");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_payments WHERE ar_ap_item_id=(SELECT ar_ap_item_id FROM staff_payroll_statements WHERE id=?)",Integer.class,wage)).isEqualTo(1);
        String second=prepared(280000);submit(second);
        rejected(()->payroll.result(second,result("succeeded","success_"+id,List.of(upload(second)))),"PAYROLL_PAYMENT_INVALID");
        payroll.result(second,result("succeeded","success_"+second,List.of(upload(second))));
        assertThat(paid()).isEqualTo(480000);
        assertThat(jdbc.queryForObject("SELECT status FROM staff_payroll_statements WHERE id=?",String.class,wage)).isEqualTo("paid");
    }

    @Test void definiteFailureAllowsNewAttemptButCannotBeRewrittenToSuccess() {
        String id=prepared(480000);submit(id);payroll.result(id,result("failed",null,List.of(upload(id))));
        assertThat(paid()).isZero();
        rejected(()->payroll.result(id,result("succeeded","late-flow",List.of(upload(id)))),"PAYROLL_PAYMENT_INVALID");
        assertThat(prepared(480000)).isNotEqualTo(id);
    }

    @Test void wrongEmployeeAndOtherAttemptAttachmentCannotRegisterBankSubmission() {
        String id=prepared(480000);
        rejected(()->payroll.submit(id,new FinancePayrollService.SubmitRequest(key(),"bad-recipient",requested,"other-user",List.of())),"PAYROLL_PAYMENT_INVALID");
        rejected(()->payroll.submit(id,new FinancePayrollService.SubmitRequest(key(),"no-proof",requested,user,List.of("not-uploaded"))),"PAYROLL_PAYMENT_INVALID");
        String file=upload(id);
        payroll.result(id,result("cancelled",null,List.of()));
        String other=prepared(480000);
        rejected(()->payroll.submit(other,new FinancePayrollService.SubmitRequest(key(),"wrong-proof",requested,user,List.of(file))),"PAYROLL_PAYMENT_INVALID");
        assertThat(paid()).isZero();
    }

    @Test void ordinaryFinancePaymentCannotBypassWageBankEvidence() {
        String ap=jdbc.queryForObject("SELECT ar_ap_item_id FROM staff_payroll_statements WHERE id=?",String.class,wage);
        rejected(()->finance.registerArApPayment(ap,new com.complex.platform.finance.dto.ArApPaymentRequest(key(),"人工核销",List.of(),480000L,"bank_transfer",resultAt,"fake-flow")),"PAYROLL_DEDICATED_WORKFLOW_REQUIRED");
        assertThat(paid()).isZero();
    }

    @Test void lockedDailySnapshotDoesNotDelaySalaryAndStaysUnchanged() {
        String close="close_"+key();
        jdbc.update("INSERT INTO finance_daily_closes(id,business_date,scope_type,scope_id,status,locked_at,created_at) VALUES(?,'2026-02-05','project',?,'locked',now(),now())",close,project);
        jdbc.update("INSERT INTO finance_daily_close_summaries(id,daily_close_id,source_stream,amount_cent,refund_cent,order_count) VALUES(?,?,'bar',10000,0,1)","summary_"+key(),close);
        jdbc.update("UPDATE ar_ap_items SET locked_day_close_id=? WHERE source_type='staff_payroll' AND source_id=?",close,wage);
        String id=prepared(480000);submit(id);payroll.result(id,result("succeeded","post-close_"+id,List.of(upload(id))));
        assertThat(paid()).isEqualTo(480000);
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM finance_daily_close_summaries WHERE daily_close_id=?",Long.class,close)).isEqualTo(10000);
        assertThat(jdbc.queryForObject("SELECT locked_day_close_id FROM ar_ap_items WHERE source_type='staff_payroll' AND source_id=?",String.class,wage)).isEqualTo(close);
    }

    @Test void financeGeneralReadOrExportDoesNotGrantWageReadOrExport() {
        var visible = analytics.apOpenPayableDetail(LocalDate.of(2026,3,1), 100, 0, Set.of(project), payroll.visiblePayableIds(FinancePayrollService.VIEW));
        assertThat(visible.totalRows()).isEqualTo(1);
        assertThat(visible.summaryValue()).isEqualByComparingTo("480000");
        jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_code LIKE 'finance.payroll.%'",role);
        var hidden = analytics.apOpenPayableDetail(LocalDate.of(2026,3,1), 100, 0, Set.of(project), payroll.visiblePayableIds(FinancePayrollService.VIEW));
        assertThat(hidden.rows()).isEmpty();
        assertThat(hidden.totalRows()).isZero();
        assertThat(hidden.summaryValue()).isZero();
        assertThat(finance.listArApItems(null,1,100).items()).noneMatch(row->"staff_payroll".equals(row.sourceType()));
        assertThat(payroll.visiblePayableIds(FinancePayrollService.EXPORT)).isEmpty();
        assertThatThrownBy(()->payroll.attempts(wage)).isInstanceOf(ApiException.class);
    }

    @Test void unrelatedWageSubjectCannotBePaidEvenWithFinancePermission() {
        String attemptId=prepared(480000);
        String other="other_"+key().substring(0,10);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','其他项目','active',now(),now())",other);
        jdbc.update("UPDATE roles SET view_all_projects=false WHERE id=?",role);
        jdbc.update("UPDATE access_users SET organization_id=? WHERE id=?",other,actor);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AccessActor(actor,actor,"staff",other,null,"active"),"test",List.of()));
        String prepareKey=key(),submitKey=key(),resultKey=key();
        failureAuditedOnce(wage,prepareKey,"PAYROLL_FINANCE_SCOPE_MISMATCH",()->payroll.prepare(wage,new FinancePayrollService.PrepareRequest(prepareKey,480000)));
        failureAuditedOnce(attemptId,submitKey,"PAYROLL_FINANCE_SCOPE_MISMATCH",()->payroll.submit(attemptId,new FinancePayrollService.SubmitRequest(submitKey,"private-request",requested,user,List.of())));
        failureAuditedOnce(attemptId,resultKey,"PAYROLL_FINANCE_SCOPE_MISMATCH",()->payroll.result(attemptId,new FinancePayrollService.ResultRequest(resultKey,"unknown",resultAt,null,"private-reason",List.of())));
        assertThat(paid()).isZero();
    }
    void failureAuditedOnce(String objectId,String requestKey,String code,Runnable action) {
        rejected(action,code);
        var rows=jdbc.queryForList("""
                SELECT metadata->>'error_code' code,metadata::text metadata
                  FROM audit_events WHERE event_name='finance.payroll.payment_failed'
                   AND event_result='failure' AND actor_id=? AND business_id=?
                   AND idempotency_key IS NOT DISTINCT FROM ?
                """,actor,objectId,requestKey);
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().get("code")).isEqualTo(code);
        assertThat(rows.getFirst().get("metadata")).isEqualTo("{\"error_code\": \""+code+"\"}");
        assertThat(paid()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_payroll_payouts WHERE statement_id=?",Integer.class,wage)).isZero();
    }

    @Test void entryPermissionRejectionsAreAuditedBeforeIdempotentReplay() {
        String prepareKey=key();
        var prepareRequest=new FinancePayrollService.PrepareRequest(prepareKey,480000);
        String id=(String)payroll.prepare(wage,prepareRequest).get("id");
        jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_code=?",role,FinancePayrollService.PAY);
        failureAuditedOnce(wage,prepareKey,"PERMISSION_DENIED",()->payroll.prepare(wage,prepareRequest));
        String submitKey=key(),resultKey=key();
        failureAuditedOnce(id,submitKey,"PERMISSION_DENIED",()->payroll.submit(id,new FinancePayrollService.SubmitRequest(submitKey,"private-request",requested,user,List.of())));
        failureAuditedOnce(id,resultKey,"PERMISSION_DENIED",()->payroll.result(id,new FinancePayrollService.ResultRequest(resultKey,"unknown",resultAt,null,"private-reason",List.of())));
        assertThat(payroll.attempts(wage).attempts()).hasSize(1);
    }

    @Test void missingOriginalsAreAuditedWithoutCreatingPaymentFacts() {
        String missing="missing_"+key(),prepareKey=key(),submitKey=key(),resultKey=key();
        failureAuditedOnce(missing,prepareKey,"PAYROLL_NOT_FOUND",()->payroll.prepare(missing,new FinancePayrollService.PrepareRequest(prepareKey,480000)));
        failureAuditedOnce(missing,submitKey,"PAYROLL_PAYMENT_NOT_FOUND",()->payroll.submit(missing,new FinancePayrollService.SubmitRequest(submitKey,"private-request",requested,user,List.of())));
        failureAuditedOnce(missing,resultKey,"PAYROLL_PAYMENT_NOT_FOUND",()->payroll.result(missing,new FinancePayrollService.ResultRequest(resultKey,"unknown",resultAt,null,"private-reason",List.of())));
        assertThat(payroll.attempts(wage).attempts()).isEmpty();
    }

    @Test void invalidEntryRequestsAreAuditedWithoutChangingPreparedAttempt() {
        String prepareKey=key(),resultKey=key();
        failureAuditedOnce(wage,prepareKey,"PAYROLL_PAYMENT_INVALID",()->payroll.prepare(wage,new FinancePayrollService.PrepareRequest(prepareKey,0)));
        String id=prepared(480000);
        failureAuditedOnce(id,null,"PAYROLL_PAYMENT_INVALID",()->payroll.submit(id,null));
        failureAuditedOnce(id,resultKey,"PAYROLL_PAYMENT_INVALID",()->payroll.result(id,new FinancePayrollService.ResultRequest(resultKey,"invalid",resultAt,null,"private-reason",List.of())));
        assertThat(payroll.attempts(wage).attempts()).singleElement().satisfies(row->assertThat(row.get("status")).isEqualTo("prepared"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM finance_payroll_payment_events WHERE attempt_id=?",Integer.class,id)).isZero();
    }

    @Test void failureInsideWriteIsNotAuditedTwice() {
        String id=prepared(480000),submitKey=key();
        failureAuditedOnce(id,submitKey,"PAYROLL_PAYMENT_INVALID",()->payroll.submit(id,new FinancePayrollService.SubmitRequest(submitKey,"private-request",requested,"wrong-recipient",List.of())));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM finance_payroll_payment_events WHERE attempt_id=?",Integer.class,id)).isZero();
    }

}
