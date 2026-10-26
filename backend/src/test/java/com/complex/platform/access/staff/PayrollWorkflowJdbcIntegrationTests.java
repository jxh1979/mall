package com.complex.platform.access.staff;

// 文件版本：V0.06

import static org.assertj.core.api.Assertions.*;
import static com.complex.platform.access.staff.StaffPayrollModels.*;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.EvidenceEventService;
import com.complex.platform.governance.review.*;
import com.complex.platform.governance.chain.ApprovalChainNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties={
 "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
 "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
 "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"})
@Transactional
class PayrollWorkflowJdbcIntegrationTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired AccessRepository access;
    @Autowired StaffPayrollService payroll;
    @Autowired StaffPayrollConfigurationService config;
    @Autowired PayrollTaxService taxes;
    @Autowired EvidenceEventService evidence;
    @Autowired SupervisorReviewService reviews;
    @Autowired StaffService staff;
    @Autowired com.complex.platform.bar.BarService bars;
    String project,user,actor,hr,finance,tax,legal;List<String> leaders;
    PayrollApprovedFactsFixture.Source source;
    final String month="2025-07";
    String key(){return UUID.randomUUID().toString().replace("-","");}
    @BeforeEach void setup(){
        String s=key().substring(0,8);project="pw_project_"+s;user="pw_employee_"+s;actor="pw_actor_"+s;
        hr="pw_hr_"+s;finance="pw_fin_"+s;tax="pw_tax_"+s;legal="pw_legal_"+s;
        leaders=List.of("pw_l1_"+s,"pw_l2_"+s,"pw_l3_"+s);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','工资实算回归','active',now(),now())",project);
        for(String u:List.of(user,actor,hr,finance,tax,legal))StaffTestActors.grant(jdbc,access,u,project);
        for(String u:leaders)StaffTestActors.grant(jdbc,access,u,project);
        jdbc.update("UPDATE roles SET view_all_projects=true WHERE id='role_m010_fixture'");
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT 'role_m010_fixture',permission_code FROM permissions WHERE permission_code IN ('evidence.attachment.create','evidence.timeline.view','governance.supervisor_review.view') ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO staff_profiles(id,project_id,user_id,name,company_id,registration_status,status,employment_status,created_at,updated_at) VALUES(?,?,?,'实算员工',?,'approved','active','employed',now(),now())","profile_"+s,project,user,project);
        String below=user;for(String leader:leaders){jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id,line_code,updated_by) VALUES(?,?,'main',?)",below,leader,actor);below=leader;}
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id,line_code,updated_by) VALUES(?,?,'main',?)",actor,leaders.getFirst(),actor);
        jdbc.update("INSERT INTO approval_lines(line_code,line_name,min_start_rank,status,hr_final_approver_id) VALUES(?,?,1,'active',?)","pw_dept_"+s,"测试部门",hr);
        jdbc.update("UPDATE org_hierarchy SET line_code=? WHERE user_id=?","pw_dept_"+s,user);
        for(var line:taxLines())jdbc.update("INSERT INTO approval_lines(line_code,line_name,min_start_rank,status,entry_user_id) VALUES(?,?,1,'active',?) ON CONFLICT(line_code) DO UPDATE SET status='active',entry_user_id=EXCLUDED.entry_user_id",line.lineCode(),line.lineCode(),line.finalApproverId());
        act(actor);
    }
    @AfterEach void cleanup(){SecurityContextHolder.clearContext();}
    void act(String who){StaffTestActors.actor(who,"staff",project,null);StaffTestActors.alignRowToSession(jdbc);}
    void prepare(long base,long performance){source=PayrollApprovedFactsFixture.create(jdbc,json,user,project,actor,month,base,performance);}
    SettleRequest request(String kind){return new SettleRequest(key(),user,month,kind,source.facts(),null);}
    PayrollTaxAssessment tax(CalculationDraft calc,long amount) {
        String category="pw_category_"+user;
        if(jdbc.queryForObject("SELECT count(*) FROM payroll_tax_versions WHERE category=? AND status='approved'",Integer.class,category)==0) {
            var d=taxes.draft(new PayrollTaxVersion.DraftRequest(key(),category,LocalDate.of(2025,1,1),"测试专业身份依据","测试税务计征规则版本",PayrollTaxService.ROUNDING,
                List.of(new PayrollTaxVersion.Bracket(1,0L,null,10000,0L,1000,0L)),"测试税表"));
            var p=taxes.submitVersion(d.id(),taxSubmit("payroll_tax_version",d.id()));approveTax(p.reviewId());
        }
        var d=taxes.draftAssessment(new PayrollTaxAssessment.DraftRequest(key(),user,month,category,LocalDate.of(2025,7,31),
            "员工与发薪主体已核","本期全部收入性质已核","专业税额复核计算书","完整工资税额认定",calc.id(),amount,"测试专业计算书:"+calc.id()));
        var p=taxes.submitAssessment(d.id(),taxSubmit("payroll_tax_assessment",d.id()));approveTax(p.reviewId());return taxes.assessment(d.id(),false);
    }
    PayrollTaxVersion.SubmitRequest taxSubmit(String type,String id){return new PayrollTaxVersion.SubmitRequest(key(),List.of(upload(type,id)),taxLines());}
    List<SupervisorReviewEvaluateRequest.ParallelLine> taxLines(){return List.of(new SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance),new SupervisorReviewEvaluateRequest.ParallelLine("tax_review",tax),new SupervisorReviewEvaluateRequest.ParallelLine("legal_review",legal));}
    void approveTax(String review){
        var nodes=reviews.chain(review);
        assertThat(nodes.stream().filter(n->ApprovalChainNode.MAIN_LINE.equals(n.lineCode())).map(ApprovalChainNode::approverId))
            .startsWith(leaders.toArray(String[]::new));
        assertThat(nodes.stream().map(ApprovalChainNode::lineCode)).contains("finance_review","tax_review","legal_review");
        for(String who:List.of(finance,tax,legal)){act(who);reviews.approve(review,decision());}
        for(String who:leaders){act(who);reviews.approve(review,decision());}
        StaffTestActors.master();reviews.approve(review,decision());act(actor);
    }
    SupervisorReviewApprovalRequest decision(){return new SupervisorReviewApprovalRequest(key(),"测试已核实原件",List.of(),null,"approved","核实完整测试依据");}
    String upload(String type,String id){return evidence.uploadAttachment(new MockMultipartFile("file","basis.png","image/png",Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jCYQAAAAASUVORK5CYII=")),type,id,"professional_basis").id();}
    StatementDetail settle(SettleRequest req,CalculationDraft calc,PayrollTaxAssessment tax){return payroll.settle(new SettleRequest(key(),user,month,req.statementKind(),source.facts(),req.originEventId(),calc.id(),tax.id()));}

    @Test void h01AFullPayrollLocks4800AndCreatesActualPayableWithoutPretendingPaid() {
        prepare(500000,100000);
        jdbc.update("UPDATE staff_policy_settings SET numeric_value=2000 WHERE policy_key='payroll_penalty_cap_bps' AND scope_id='global'");
        jdbc.update("INSERT INTO staff_payroll_adjustments(id,user_id,month,adj_type,amount_cent,reason,created_by,project_id) VALUES(?,?,?,'penalty',120000,'已批现金处罚',?,?)",key(),user,month,actor,project);
        // 本测试以已批准20分为输入，扣分审批另有真实三级复核测试。
        jdbc.update("INSERT INTO staff_performance_score_adjustments(id,user_id,month,project_id,points,rule_code,scheme_version_id,reason,review_id,status,created_by,approved_at) VALUES(?,?,?,?,20,'test_points',?,'已核扣分',?,'approved',?,now())",key(),user,month,project,source.scheme(),key(),actor);
        var r=request("monthly");var calc=payroll.calculate(r);
        assertThat(calc.beforeTaxCent()).isEqualTo(480000);
        var detail=settle(r,calc,tax(calc,0));
        assertThat(detail.statement().totalCent()).isEqualTo(480000);
        assertThat(detail.statement().penaltyCent()).isEqualTo(100000);
        assertThat(detail.lines()).anyMatch(l->"performance".equals(l.componentType())&&l.amountCent()==80000);
        assertThat(detail.status()).isEqualTo("closed");assertThat(detail.paidAmountCent()).isZero();
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ar_ap_items WHERE id=?",Long.class,detail.arApItemId())).isEqualTo(480000);
        assertThat(detail.pendingItems()).anyMatch(p->"WORKTIME_ROSTER_PENDING".equals(p.code()));
    }
    @Test void completeIncomeTaxIsOneApprovedAssessmentAndNeverDoubleDeductsServiceTax() {
        prepare(500000,0);
        jdbc.update("INSERT INTO staff_commission_ledger(id,user_id,month,source_type,source_id,amount_cent,created_at,project_id) VALUES(?,?,?,'service_order',?,100000,now(),?)",key(),user,month,key(),project);
        var r=request("monthly");var calc=payroll.calculate(r);assertThat(calc.beforeTaxCent()).isEqualTo(600000);
        var assessment=tax(calc,12345);var detail=settle(r,calc,assessment);
        assertThat(detail.statement().taxCent()).isEqualTo(12345);assertThat(detail.statement().totalCent()).isEqualTo(587655);
        assertThat(assessment.payrollCalculationId()).isEqualTo(calc.id());
    }
    @Test void professionalAmountWithoutApprovedEvidenceCannotSettle() {
        prepare(500000,0);var r=request("monthly");var calc=payroll.calculate(r);
        assertThatThrownBy(()->payroll.settle(new SettleRequest(key(),user,month,"monthly",source.facts(),null,calc.id(),"not-approved")))
            .isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_payroll_statements WHERE user_id=?",Integer.class,user)).isZero();
    }
    @Test void sourceChangeAfterTaxApprovalRequiresRecalculationAndLeavesOriginalProof() {
        prepare(500000,0);var r=request("monthly");var calc=payroll.calculate(r);var assessment=tax(calc,0);
        jdbc.update("INSERT INTO staff_payroll_adjustments(id,user_id,month,adj_type,amount_cent,reason,created_by,project_id) VALUES(?,?,?,'bonus',10000,'后到已批奖金',?,?)",key(),user,month,actor,project);
        assertThatThrownBy(()->settle(r,calc,assessment)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PAYROLL_SOURCE_CHANGED"));
        assertThat(taxes.assessment(assessment.id(),false).status()).isEqualTo("approved");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_payroll_statements WHERE user_id=?",Integer.class,user)).isZero();
    }
    @Test void sameEmployeeCanReceiveSeparateSupplementWithoutReissuingBase() {
        prepare(500000,0);var r=request("monthly");var calc=payroll.calculate(r);var original=settle(r,calc,tax(calc,0));
        jdbc.update("INSERT INTO staff_commission_ledger(id,user_id,month,source_type,source_id,amount_cent,created_at,project_id) VALUES(?,?,?,'manual',?,10000,now(),?)",key(),user,month,key(),project);
        var next=request("supplemental");var additional=payroll.calculate(next);
        assertThat(additional.beforeTaxCent()).isEqualTo(10000);var result=settle(next,additional,tax(additional,0));
        assertThat(result.statement().baseCent()).isZero();assertThat(result.statement().totalCent()).isEqualTo(10000);
        assertThat(payroll.preparePendingCorrections(original.statement().id())).contains(result.statement().id());
        assertThat(jdbc.queryForObject("SELECT total_cent FROM staff_payroll_statements WHERE id=?",Long.class,original.statement().id())).isEqualTo(500000);
    }
    @Test void revokedPenaltyRestoresActualLockedDifferenceAndNextPaymentIncludesItOnce() {
        prepare(500000,0);
        String first=key(),second=key();
        for(var item:Map.of(first,120000L,second,50000L).entrySet())jdbc.update("INSERT INTO staff_payroll_adjustments(id,user_id,month,adj_type,amount_cent,reason,created_by,project_id) VALUES(?,?,?,'penalty',?,'已批扣款',?,?)",
            item.getKey(),user,month,item.getValue(),actor,project);
        var r=request("monthly");var calc=payroll.calculate(r);var original=settle(r,calc,tax(calc,0));
        jdbc.update("INSERT INTO staff_payroll_adjustments(id,user_id,month,adj_type,amount_cent,reason,created_by,project_id,reversal_of) VALUES(?,?,?,'penalty_reversal',50000,'已批撤罚',?,?,?)",key(),user,month,actor,project,second);
        payroll.correctionRequested(new StaffPayrollService.CorrectionRequested(user,month,key(),second));
        assertThat(jdbc.queryForObject("SELECT sum(amount_cent) FROM staff_payroll_corrections WHERE original_statement_id=?",Long.class,original.statement().id())).isZero();
        jdbc.update("INSERT INTO staff_payroll_adjustments(id,user_id,month,adj_type,amount_cent,reason,created_by,project_id,reversal_of) VALUES(?,?,?,'penalty_reversal',120000,'已批撤罚',?,?,?)",key(),user,month,actor,project,first);
        var event=new StaffPayrollService.CorrectionRequested(user,month,key(),first);payroll.correctionRequested(event);payroll.correctionRequested(event);
        assertThat(jdbc.queryForObject("SELECT sum(amount_cent) FROM staff_payroll_corrections WHERE original_statement_id=?",Long.class,original.statement().id())).isEqualTo(100000);
        var extra=request("supplemental");var extraCalc=payroll.calculate(extra);assertThat(extraCalc.beforeTaxCent()).isEqualTo(100000);
        var additional=settle(extra,extraCalc,tax(extraCalc,0));
        assertThat(additional.lines()).anyMatch(l->"payroll_correction".equals(l.sourceType())&&l.amountCent()==100000);
        assertThat(jdbc.queryForObject("SELECT total_cent FROM staff_payroll_statements WHERE id=?",Long.class,original.statement().id())).isEqualTo(400000);
        assertThat(payroll.calculate(request("supplemental")).beforeTaxCent()).isZero();
    }
    @Test void revokedPersonalLeaveAfterLockRestoresDeductionInNextStatementWithSource() {
        // BSM L1481：销假或撤销发生在已月结定格月份之后的，在当期薪资单冲正并注明来源，不回改已定格月份。
        prepare(500000,0);var zone=com.complex.platform.common.LocalZone.ID;String leave=key();
        // 两个已发布班次各 8 小时，已批事假覆盖其中一天：事假工时占应出勤 50%，基础工资与提成各按比例扣。
        for(var date:List.of(LocalDate.of(2025,7,15),LocalDate.of(2025,7,16))) {
            Instant start=date.atTime(9,0).atZone(zone).toInstant();
            jdbc.update("INSERT INTO staff_shift_schedules(id,user_id,work_date,start_at,end_at,point_space_ids,created_by,project_id) VALUES(?,?,?,?,?,'[]'::jsonb,?,?)",
                key(),user,date,java.sql.Timestamp.from(start),java.sql.Timestamp.from(start.plusSeconds(8*3600)),actor,project);
        }
        jdbc.update("INSERT INTO staff_leave_requests(id,user_id,leave_type,start_date,end_date,reason,status,reviewed_by,project_id) VALUES(?,?,'personal',?,?,'已批事假','approved',?,?)",
            leave,user,LocalDate.of(2025,7,16),LocalDate.of(2025,7,16),actor,project);
        jdbc.update("INSERT INTO staff_commission_ledger(id,user_id,month,source_type,source_id,amount_cent,created_at,project_id) VALUES(?,?,?,'manual',?,100000,now(),?)",key(),user,month,key(),project);
        // 2026-10-18 操作员裁决：销假=本人申请＋主管批准。员工先申请（待销假审批）——主管批准前假仍有效，月结照扣事假。
        act(user);
        assertThat(staff.requestLeaveRevoke(leave,new com.complex.platform.access.staff.dto.StaffLeaveRevokeRequest(key(),"已到岗")).status()).isEqualTo("revoke_requested");
        act(actor);
        var r=request("monthly");var calc=payroll.calculate(r);
        assertThat(calc.beforeTaxCent()).isEqualTo(300000);
        var original=settle(r,calc,tax(calc,0));
        assertThat(original.statement().leaveDeductionCent()).isEqualTo(300000);
        // 定格后主管批准销假：按 BSM L1481 冲正；原审批留痕与本人申请留痕都保留。
        var revoked=staff.revokeLeave(leave,new com.complex.platform.access.staff.dto.StaffLeaveReviewRequest(key(),"approve","定格后批准销假"));
        assertThat(revoked.status()).isEqualTo("revoked");
        assertThat(revoked.reviewedBy()).isEqualTo(actor);assertThat(revoked.revokeRequestedBy()).isEqualTo(user);assertThat(revoked.revokedBy()).isEqualTo(actor);
        // 定格月份不回改：按锁定快照把该假工时扣除后复算出实差，补差注明来源。
        assertThat(jdbc.queryForList("SELECT amount_cent,status,original_statement_id,calculation_snapshot->>'source' AS source FROM staff_payroll_corrections WHERE source_adjustment_id=?",leave))
            .singleElement().satisfies(row->{
                assertThat(row.get("amount_cent")).isEqualTo(300000L);assertThat(row.get("status")).isEqualTo("pending");
                assertThat(row.get("original_statement_id")).isEqualTo(original.statement().id());assertThat(row.get("source")).isEqualTo("leave_revoked");
            });
        var extra=request("supplemental");var extraCalc=payroll.calculate(extra);
        assertThat(extraCalc.beforeTaxCent()).isEqualTo(300000);
        var additional=settle(extra,extraCalc,tax(extraCalc,0));
        assertThat(additional.lines()).anyMatch(l->"payroll_correction".equals(l.sourceType())&&l.amountCent()==300000);
        assertThat(jdbc.queryForObject("SELECT total_cent FROM staff_payroll_statements WHERE id=?",Long.class,original.statement().id())).isEqualTo(300000);
        assertThat(payroll.calculate(request("supplemental")).beforeTaxCent()).isZero();
    }
    @Test void currentApprovedSalaryOnlyAndNoFutureOrDifferentAmount() {
        assertThat(config.approvedPositionSalary(user,"test_position",null)).isNull();
        assertThatThrownBy(()->config.approvedPositionSalary(user,"test_position",1L)).isInstanceOf(ApiException.class);
        prepare(500000,0);
        assertThat(config.approvedPositionSalary(user,"test_position",null)).isEqualTo(500000);
        assertThat(config.approvedPositionSalary(user,"test_position",500000L)).isEqualTo(500000);
        assertThatThrownBy(()->config.approvedPositionSalary(user,"test_position",600000L)).isInstanceOf(ApiException.class);
        PayrollApprovedFactsFixture.create(jdbc,json,user,project,actor,"2099-01",900000,0);
        assertThat(config.approvedPositionSalary(user,"test_position",null)).isEqualTo(500000);
    }
    @Test void schemeReallyRequiresThreeSuperiorsHrAndFinanceWithUploadedOriginal() {
        var draft=config.draftScheme(new SchemeDraftRequest(key(),user,LocalDate.of(2025,7,1),null,"test_position",
            List.of(new Component("base","基础工资","base","fixed_monthly",500000,"month","proportional","测试合同月薪")),
            null,new WorkTimeTerms("standard",480,2400,1000,15000,20000,30000,"测试核定工时制度","测试计薪基数"),
            "约定每月5日发薪","本次真实薪资方案审批"));
        assertThatThrownBy(()->config.submitScheme(draft.id(),new SubmitRequest(key(),List.of("not-uploaded"),List.of(new SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance)))))
            .isInstanceOf(ApiException.class);
        var submitted=config.submitScheme(draft.id(),new SubmitRequest(key(),List.of(upload("staff_payroll_scheme",draft.id())),List.of(new SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance))));
        var nodes=reviews.chain(submitted.reviewId());
        assertThat(nodes.stream().filter(n->ApprovalChainNode.MAIN_LINE.equals(n.lineCode())).map(ApprovalChainNode::approverId)).containsExactlyElementsOf(leaders);
        assertThat(nodes.stream().map(ApprovalChainNode::lineCode)).contains("hr_review","finance_review");
        for(String who:leaders){act(who);reviews.approve(submitted.reviewId(),decision());}
        act(hr);reviews.approve(submitted.reviewId(),decision());assertThat(config.scheme(draft.id()).status()).isEqualTo("pending_review");
        act(finance);reviews.approve(submitted.reviewId(),decision());assertThat(config.scheme(draft.id()).status()).isEqualTo("approved");
    }

    @Test void pendingPerformanceDoesNotHoldBaseAndItsLaterSupplementIsPaidOnlyOnce() {
        source=PayrollApprovedFactsFixture.create(jdbc,json,user,project,actor,month,500000,100000,new PerformanceFact(false,false,"当前未达标，上一评定周期尚缺原件"));
        String deduction=key();
        jdbc.update("INSERT INTO staff_performance_score_adjustments(id,user_id,month,project_id,points,rule_code,scheme_version_id,reason,review_id,status,created_by,approved_at) VALUES(?,?,?,?,20,'late_points',?,'测试已批准扣分原件',?,'approved',?,now())",deduction,user,month,project,source.scheme(),key(),actor);
        var request=request("monthly");var calc=payroll.calculate(request);
        assertThat(calc.beforeTaxCent()).isEqualTo(500000);
        var original=settle(request,calc,tax(calc,0));
        assertThat(original.pendingItems()).anyMatch(p->"PERFORMANCE_BASIS_PENDING".equals(p.code()));
        String revised=key();
        jdbc.update("INSERT INTO staff_payroll_period_facts(id,user_id,month,project_id,merchant_id,payer_company_id,scheme_version_id,calendar_version_id,component_facts,performance_facts,cycle_start,cycle_end,cycle_standard_minutes,rest_compensated_minutes,agreed_due_date,reason,status,created_by,approved_at) SELECT ?,user_id,month,project_id,merchant_id,payer_company_id,scheme_version_id,calendar_version_id,component_facts,?::jsonb,cycle_start,cycle_end,cycle_standard_minutes,rest_compensated_minutes,agreed_due_date,'测试新批准绩效原件','approved',created_by,now() FROM staff_payroll_period_facts WHERE id=?",
            revised,config.encode(new PerformanceFact(false,true,"已另行确认达标原件")),source.facts());
        source=new PayrollApprovedFactsFixture.Source(source.scheme(),source.calendar(),revised);
        var extra=request("supplemental");var extraCalc=payroll.calculate(extra);
        assertThat(extraCalc.beforeTaxCent()).isEqualTo(80000);
        var latePerformance=settle(extra,extraCalc,tax(extraCalc,0));
        assertThat(payroll.calculate(request("supplemental")).beforeTaxCent()).isZero();
        act(user);
        var appeal=config.appealScore(deduction,new com.complex.platform.access.staff.dto.StaffAdjustmentAppealRequest(key(),"撤销原20分扣分"));
        for(String who:leaders){act(who);reviews.approve(appeal.reviewId(),decision());}
        act(hr);reviews.approve(appeal.reviewId(),decision());act(actor);
        assertThat(config.score(appeal.id()).status()).isEqualTo("approved");
        assertThat(jdbc.queryForObject("SELECT sum(amount_cent) FROM staff_payroll_corrections WHERE original_statement_id=?",Long.class,original.statement().id())).isEqualTo(20000);
        var compensation=request("supplemental");var compensationCalc=payroll.calculate(compensation);
        assertThat(compensationCalc.beforeTaxCent()).isEqualTo(20000);
        settle(compensation,compensationCalc,tax(compensationCalc,0));
        assertThat(payroll.calculate(request("supplemental")).beforeTaxCent()).isZero();
        assertThat(payroll.detail(latePerformance.statement().id(),false).statement().totalCent()).isEqualTo(80000);
        var old=payroll.detail(original.statement().id(),false);
        assertThat(old.statement().totalCent()).isEqualTo(500000);
        assertThat(old.pendingItems()).noneMatch(p->"PERFORMANCE_BASIS_PENDING".equals(p.code()));
        assertThat(old.pendingItems()).anyMatch(p->"WORKTIME_ROSTER_PENDING".equals(p.code()));
    }
    @Test void nextPayrollExplicitlyCarriesOriginalUnresolvedMonthAndStatement() {
        prepare(500000,0);var first=request("monthly");var calc=payroll.calculate(first);var original=settle(first,calc,tax(calc,0));
        var next=PayrollApprovedFactsFixture.create(jdbc,json,user,project,actor,"2025-08",500000,0);
        var nextCalc=payroll.calculate(new SettleRequest(key(),user,"2025-08","monthly",next.facts(),null));
        var snap=json.convertValue(nextCalc.calculationSnapshot(),StaffPayrollService.Snapshot.class);
        assertThat(snap.beforeTaxCent()).isEqualTo(500000);
        assertThat(snap.pending()).anyMatch(p->"HISTORICAL_PAYROLL_PENDING".equals(p.code())&&p.message().contains(month)&&p.message().contains(original.statement().id()));
    }
    @Test void taxOnlyReaderSeesCompleteCalculationWithinScopeAndCannotCalculate() {
        prepare(500000,0);var calc=payroll.calculate(request("monthly"));String reader="tax_reader_"+key(),role="tax_role_"+key();
        access.createUser(new com.complex.platform.access.AccessUser(reader,reader,"staff",project,null,"active",Instant.now()));
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'仅核税读取','project','active',now())",role,role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'staff.tax_bracket.manage')",role);
        access.assignUserRole(reader,role);act(reader);
        assertThat(payroll.calculations(user,month)).extracting(CalculationDraft::id).containsExactly(calc.id());
        assertThatThrownBy(()->payroll.calculate(request("supplemental"))).isInstanceOf(ApiException.class);
        String other="tax_other_"+key();
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','其他发放主体','active',now(),now())",other);
        String outside=key();
        // 仅验证查询范围的独立原件夹具；完整工资主体一致性由正式计算/定格用例验证。
        jdbc.update("INSERT INTO staff_payroll_calculation_drafts(id,user_id,month,statement_kind,scheme_version_id,facts_id,project_id,merchant_id,payer_company_id,before_tax_cent,source_fingerprint,calculation_snapshot,created_by) SELECT ?,user_id,month,statement_kind,scheme_version_id,facts_id,project_id,merchant_id,?,before_tax_cent,source_fingerprint,calculation_snapshot,created_by FROM staff_payroll_calculation_drafts WHERE id=?",outside,other,calc.id());
        assertThat(payroll.calculations(user,month)).extracting(CalculationDraft::id).containsExactly(calc.id());
        String merchant="tax_merchant_"+key();
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,status,created_at,updated_at) VALUES(?,'范围测试商户','bar',?,'active',now(),now())",merchant,project);
        StaffTestActors.actor(reader,"staff",project,merchant);StaffTestActors.alignRowToSession(jdbc);
        assertThat(payroll.calculations(user,month)).isEmpty();
    }
    @Test void publishedScheduleUsesActualAttendanceAndIgnoresUnappliedRevision() {
        prepare(500000,0);
        String shift=key(),in=key(),out=key(),draft=key();
        var date=LocalDate.of(2025,7,15);var zone=com.complex.platform.common.LocalZone.ID;
        Instant start=date.atTime(9,0).atZone(zone).toInstant();
        // 已发布排班和已核实打卡原件夹具；审批发布链由 StaffJdbc 的真实审批用例覆盖。
        jdbc.update("INSERT INTO staff_shift_schedules(id,user_id,work_date,start_at,end_at,point_space_ids,created_by,project_id) VALUES(?,?,?,?,?,'[]'::jsonb,?,?)",
            shift,user,date,java.sql.Timestamp.from(start),java.sql.Timestamp.from(start.plusSeconds(8*3600)),actor,project);
        jdbc.update("INSERT INTO staff_attendance_records(id,user_id,punch_type,punched_at,method,shift_id,judgement,project_id) VALUES(?,?,'check_in',?,'manual',?,'normal',?),(?,?,'check_out',?,'manual',?,'normal',?)",
            in,user,java.sql.Timestamp.from(start),shift,project,out,user,java.sql.Timestamp.from(start.plusSeconds(10*3600)),shift,project);
        var unapproved=new StaffShiftRecord(draft,user,date.plusDays(1),start.plusSeconds(86400),start.plusSeconds(86400+10*3600),List.of(),"待审草案",actor,start,start,key(),"pending_review");
        jdbc.update("INSERT INTO staff_shift_revisions(id,shift_id,review_id,after_value,initiated_by) VALUES(?,?,?,?::jsonb,?)",key(),draft,unapproved.reviewId(),PayrollApprovedFactsFixture.encode(json,unapproved),actor);
        var calc=payroll.calculate(request("monthly"));
        assertThat(calc.beforeTaxCent()).isEqualTo(503000);
        var snap=json.convertValue(calc.calculationSnapshot(),StaffPayrollService.Snapshot.class);
        assertThat(snap.worktime().scheduledSeconds()).isEqualTo(8*3600);
        assertThat(snap.worktime().result().ordinarySeconds()).isEqualTo(2*3600);
        assertThat(snap.worktime().attendanceIds()).containsExactly(in,out);
        assertThat(snap.worktime().result().pendingItems()).isEmpty();
        assertThat(snap.worktime().intervals()).extracting(WorkInterval::shiftId).containsExactly(shift);
    }
    @Test void dispatchedCrossProjectBarIncomeUsesOriginalPayerAndOtherPayerRemainsPending() {
        prepare(500000,0);String merchant="pa_merchant_"+key(),other="pa_company_"+key(),serviceProject="pa_service_"+key(),run=key(),matched=key(),unmatched=key();
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','派驻实际服务项目','active',now(),now())",serviceProject);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'原收入发薪主体','management_company','active',now(),now())",other);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,status,created_at,updated_at) VALUES(?,'原服务酒吧','bar',?,'active',now(),now())",merchant,other);
        // 此处只测工资消费已确认分配原件；分配的收款/责任区间/规则链由 BarPayroll 集成用例验证。
        jdbc.update("INSERT INTO bar_payroll_runs(id,project_id,merchant_id,month,source_fingerprint,source_snapshot,collected_tip_cent,collected_service_fee_cent,payable_tip_cent,payable_service_fee_cent,withheld_service_fee_cent,rounding_residual_cent,settlement_entity_id,status,created_by) VALUES(?,?,?,?,?,'{}'::jsonb,7000,0,7000,0,0,0,?,'confirmed',?)",run,serviceProject,merchant,month,key(),other,actor);
        for(var row:Map.of(matched,project,unmatched,other).entrySet())
            jdbc.update("INSERT INTO bar_payroll_allocations(id,run_id,user_id,month,component_type,amount_cent,merchant_id,project_id,payer_company_id,source_bill_ids,rule_version_id,status) VALUES(?,?,?,?,'tip',?,?,?,?, '[]'::jsonb,?,'confirmed')",
                row.getKey(),run,user,month,row.getKey().equals(matched)?2000:5000,merchant,serviceProject,row.getValue(),key());
        var r=request("monthly");var calc=payroll.calculate(r);
        assertThat(calc.beforeTaxCent()).isEqualTo(502000);
        var detail=settle(r,calc,tax(calc,0));
        assertThat(detail.lines()).anyMatch(l->"bar_payroll_allocation".equals(l.sourceType())&&matched.equals(l.sourceId())&&l.amountCent()==2000);
        assertThat(detail.lines()).noneMatch(l->unmatched.equals(l.sourceId()));
        assertThat(detail.statement().payerCompanyId()).isEqualTo(project);
        assertThat(jdbc.queryForObject("SELECT project_id FROM staff_payroll_statements WHERE id=?",String.class,detail.statement().id())).isEqualTo(project);
        assertThat(detail.pendingItems()).anyMatch(p->"BAR_ALLOCATION_PAYER_PENDING".equals(p.code())&&unmatched.equals(p.sourceId())&&p.message().contains(other));
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ar_ap_items WHERE id=?",Long.class,detail.arApItemId())).isEqualTo(502000);
    }

    /**
     * 第十七批（操作员裁决补充第 1 条「作废时系统自动……冲回已发积分、成长值和提成（工资单不再挂待定）」）：酒吧账单作废并冲正后，
     * 该单提成不再以「酒吧账单尚未确认结算」挂待定，原工资单记下的这条待定作废当时即结清（原单不再显示、下月不再按历史待定展开）；
     * 已计入定格工资的提成在原期补发中以负额冲回，只冲一次；本期没有其他可付金额时冲回转「收入冲正超出本期可付」待核，下次可付时随冲回行结清。
     */
    @Test void voidedBarBillCommissionStopsPendingAndPaidCommissionReversedInSupplement() {
        prepare(500000,0);
        String settled=barBill("paid","settled"),open=barBill("unpaid","unsettled");
        String paid=barCommission(settled,4500),pending=barCommission(open,1200);
        var r=request("monthly");var calc=payroll.calculate(r);var original=settle(r,calc,tax(calc,0));
        assertThat(original.lines()).anyMatch(l->"bar_commission".equals(l.sourceType())&&paid.equals(l.sourceId())&&l.amountCent()==4500);
        assertThat(original.pendingItems()).anyMatch(p->"BAR_COMMISSION_PENDING".equals(p.code())&&pending.equals(p.sourceId()));

        // 主管在本综合体内处置：作废把已收 30000 原路待退挂退款应付，按操作者所在综合体落账。
        StaffTestActors.actor("pw_bar_supervisor","merchant_staff",project,"pw_merchant");
        for(String bill:List.of(settled,open)) {
            bars.raiseBillAnomaly(bill,new com.complex.platform.bar.BarBillAnomalyRequest(key(),"客人对整单有争议"));
            bars.resolveBillAnomaly(bill,new com.complex.platform.bar.BarBillAnomalyResolveRequest(key(),"voided_reversed","核实整单录错，作废并冲正"));
        }
        act(actor);
        assertThat(payroll.detail(original.statement().id(),false).pendingItems()).noneMatch(p->"BAR_COMMISSION_PENDING".equals(p.code()));
        var only=request("supplemental");var onlyCalc=payroll.calculate(only);
        var onlySnap=json.convertValue(onlyCalc.calculationSnapshot(),StaffPayrollService.Snapshot.class);
        assertThat(onlyCalc.beforeTaxCent()).isZero();
        assertThat(onlySnap.lines()).noneMatch(l->"bar_commission_reversal".equals(l.sourceType()));
        assertThat(onlySnap.pending()).anyMatch(p->"BAR_INCOME_REVERSAL_PENDING".equals(p.code())&&paid.equals(p.sourceId()));
        settle(only,onlyCalc,tax(onlyCalc,0));
        jdbc.update("INSERT INTO staff_payroll_adjustments(id,user_id,month,adj_type,amount_cent,reason,created_by,project_id) VALUES(?,?,?,'bonus',10000,'后到已批奖金',?,?)",key(),user,month,actor,project);
        var extra=request("supplemental");var extraCalc=payroll.calculate(extra);
        var snap=json.convertValue(extraCalc.calculationSnapshot(),StaffPayrollService.Snapshot.class);
        assertThat(snap.pending()).noneMatch(p->"BAR_COMMISSION_PENDING".equals(p.code()));
        assertThat(snap.lines()).anyMatch(l->"bar_commission_reversal".equals(l.sourceType())&&paid.equals(l.sourceId())&&l.amountCent()==-4500);
        assertThat(extraCalc.beforeTaxCent()).isEqualTo(5500);
        settle(extra,extraCalc,tax(extraCalc,0));
        assertThat(jdbc.queryForObject("SELECT status FROM staff_payroll_pending_sources WHERE user_id=? AND source_type='BAR_INCOME_REVERSAL_PENDING' AND source_id=?",String.class,user,paid))
            .isEqualTo("resolved");
        assertThat(payroll.calculate(request("supplemental")).beforeTaxCent()).isZero();
    }
    /** 本员工为销售归属人的酒吧账单（无收款流水：本用例只看工资侧）。 */
    String barBill(String payment,String settlement) {
        String id="pw_bill_"+key(),table="pw_tbl_"+key(),order="pw_ord_"+key();
        jdbc.update("INSERT INTO bar_tables(id,bar_id,table_code,table_name,table_type,status,created_at,updated_at) VALUES(?,'pw_bar',?,'提成回归卡座','card_seat','active',now(),now())",table,table);
        jdbc.update("INSERT INTO bar_orders(id,bar_id,table_id,order_source,status,total_amount_cent,created_at,updated_at) VALUES(?,'pw_bar',?,'staff','billed',30000,now(),now())",order,table);
        jdbc.update("INSERT INTO bar_bills(id,bill_no,order_id,bar_id,merchant_id,table_id,subtotal_amount_cent,payable_amount_cent,paid_amount_cent,payment_status,settlement_status,sales_staff_id,created_at,updated_at) VALUES(?,?,?,'pw_bar','pw_merchant',?,30000,30000,?,?,?,?,now(),now())",
            id,id,order,table,"paid".equals(payment)?30000:0,payment,settlement,user);
        return id;
    }
    String barCommission(String bill,long amount) {
        String id="pw_cm_"+key();
        jdbc.update("INSERT INTO bar_commission_calculations(id,bill_id,sales_staff_id,sales_role_type,commission_base_cent,commission_rate_bp,commission_amount_cent,status,created_at) VALUES(?,?,?,'sales',30000,1500,?,'calculated',?::timestamptz)",
            id,bill,user,amount,month+"-15T12:00:00+08:00");
        return id;
    }

    @Test void completePayrollTaxEvidenceStaysWithOriginalSubjectAfterEmployeeTransfer() {
        prepare(500000,0);var calc=payroll.calculate(request("monthly"));tax(calc,0);
        var draft=taxes.draftAssessment(new PayrollTaxAssessment.DraftRequest(key(),user,month,"pw_category_"+user,LocalDate.of(2025,7,31),
            "原员工身份","原完整工资事实","原发放主体专业核定","跨主体后仍依原工资办理",calc.id(),0L,"原件:"+calc.id()));
        String other="moved_project_"+key(),reader="original_tax_"+key(),role="original_tax_role_"+key();
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','员工后来所属项目','active',now(),now())",other);
        jdbc.update("UPDATE staff_profiles SET project_id=?,company_id=? WHERE user_id=?",other,other,user);
        jdbc.update("UPDATE access_users SET organization_id=? WHERE id=?",other,user);
        access.createUser(new com.complex.platform.access.AccessUser(reader,reader,"staff",project,null,"active",Instant.now()));
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'原主体核税及原件','project','active',now())",role,role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN ('staff.tax_bracket.manage','evidence.attachment.create','evidence.timeline.view')",role);
        access.assignUserRole(reader,role);act(reader);
        assertThat(upload(PayrollTaxService.ASSESSMENT,draft.id())).isNotBlank();
        assertThat(taxes.assessments(user,month)).extracting(PayrollTaxAssessment::id).contains(draft.id());
        assertThat(taxes.requireEvidenceScope(PayrollTaxService.ASSESSMENT,draft.id(),"evidence.timeline.view")).isNull();
        StaffTestActors.actor(reader,"staff",other,null);StaffTestActors.alignRowToSession(jdbc);
        assertThat(taxes.assessments(user,month)).isEmpty();
        assertThatThrownBy(()->upload(PayrollTaxService.ASSESSMENT,draft.id())).isInstanceOfSatisfying(ApiException.class,
            e->assertThat(e.code()).isEqualTo("STAFF_SCOPE_MISMATCH"));
    }

    @Test void rejectedPerformanceAppealCanBeSubmittedAgainButPendingOrApprovedCannotDuplicate() {
        prepare(500000,100000);String original=key();
        jdbc.update("INSERT INTO staff_performance_score_adjustments(id,user_id,month,project_id,points,rule_code,scheme_version_id,reason,review_id,status,created_by,approved_at) VALUES(?,?,?,?,20,'test_points',?,'已批准扣分原件',?,'approved',?,now())",
            original,user,month,project,source.scheme(),key(),actor);
        act(user);var first=config.appealScore(original,new com.complex.platform.access.staff.dto.StaffAdjustmentAppealRequest(key(),"首次申诉"));
        assertThatThrownBy(()->config.appealScore(original,new com.complex.platform.access.staff.dto.StaffAdjustmentAppealRequest(key(),"重复待审")))
            .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("APPEAL_ALREADY_EXISTS"));
        act(leaders.getFirst());reviews.approve(first.reviewId(),new SupervisorReviewApprovalRequest(key(),"首次依据不足",List.of(),null,"rejected","维持原扣分"));
        assertThat(config.score(first.id()).status()).isEqualTo("rejected");
        act(user);var second=config.appealScore(original,new com.complex.platform.access.staff.dto.StaffAdjustmentAppealRequest(key(),"补充依据重新申诉"));
        assertThat(second.id()).isNotEqualTo(first.id());assertThat(second.status()).isEqualTo("pending_review");
        for(String who:leaders){act(who);reviews.approve(second.reviewId(),decision());}
        act(hr);reviews.approve(second.reviewId(),decision());act(user);
        assertThat(config.score(second.id()).status()).isEqualTo("approved");
        assertThat(config.score(first.id()).status()).isEqualTo("rejected");
        assertThatThrownBy(()->config.appealScore(original,new com.complex.platform.access.staff.dto.StaffAdjustmentAppealRequest(key(),"已撤销再次申诉")))
            .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("APPEAL_ALREADY_EXISTS"));
    }

    @Test void scoreDeductionTakesEffectOnlyAfterThreeSuperiorsAndHr() {
        // BSM L1489：绩效扣分与其他涉及员工的人力操作一样，须经 HR 平行会签及以该员工直属领导为第一级的向上三个组织层级复核后生效。
        // 已批方案夹具内含已批分值表 late_points=20 分（基础分 100、B 档系数 1.0）。
        source=PayrollApprovedFactsFixture.create(jdbc,json,user,project,actor,month,500000,100000,Map.of("late_points",BigDecimal.valueOf(20)));
        var deduction=config.deductScore(new ScoreRequest(key(),user,month,"late_points","测试违规扣分"));
        assertThat(deduction.status()).isEqualTo("pending_review");assertThat(deduction.points()).isEqualByComparingTo("20");
        var nodes=reviews.chain(deduction.reviewId());
        assertThat(nodes.stream().filter(n->ApprovalChainNode.MAIN_LINE.equals(n.lineCode())).map(ApprovalChainNode::approverId)).containsExactlyElementsOf(leaders);
        assertThat(nodes.stream().map(ApprovalChainNode::lineCode)).contains("hr_review");
        // 未全批前不扣分：基础 500000 + 绩效 100000。
        assertThat(payroll.calculate(request("monthly")).beforeTaxCent()).isEqualTo(600000);
        for(String who:leaders){act(who);reviews.approve(deduction.reviewId(),decision());}
        act(actor);assertThat(config.score(deduction.id()).status()).isEqualTo("pending_review");
        assertThat(payroll.calculate(request("monthly")).beforeTaxCent()).isEqualTo(600000);
        act(hr);reviews.approve(deduction.reviewId(),decision());act(actor);
        assertThat(config.score(deduction.id()).status()).isEqualTo("approved");
        var calc=payroll.calculate(request("monthly"));
        var snap=json.convertValue(calc.calculationSnapshot(),StaffPayrollService.Snapshot.class);
        // (100−20)÷100 × B 档系数 1.0 × 绩效金额 100000 = 80000。
        assertThat(snap.points()).isEqualByComparingTo("80");assertThat(snap.earnings().performanceCent()).isEqualTo(80000);
        assertThat(calc.beforeTaxCent()).isEqualTo(580000);
    }

    @Test void originalPeriodAndScoreRemainAvailableOnlyInLockedSubjectAfterEmployeeTransfer() {
        prepare(500000,100000);String score=key();
        jdbc.update("INSERT INTO staff_performance_score_adjustments(id,user_id,month,project_id,points,rule_code,scheme_version_id,reason,review_id,status,created_by,approved_at) VALUES(?,?,?,?,20,'test_points',?,'原主体已批准扣分',?,'approved',?,now())",
            score,user,month,project,source.scheme(),key(),actor);
        String other="history_other_"+key(),reader="history_reader_"+key(),role="history_role_"+key();
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','员工调入主体','active',now(),now())",other);
        jdbc.update("UPDATE staff_profiles SET project_id=?,company_id=? WHERE user_id=?",other,other,user);
        jdbc.update("UPDATE access_users SET organization_id=? WHERE id=?",other,user);
        access.createUser(new com.complex.platform.access.AccessUser(reader,reader,"staff",project,null,"active",Instant.now()));
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'原主体工资权限','project','active',now())",role,role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'staff.payroll.manage')",role);
        access.assignUserRole(reader,role);act(reader);
        assertThat(config.periods(user,month)).extracting(PeriodFacts::id).containsExactly(source.facts());
        assertThat(config.scores(user,month)).extracting(ScoreAdjustment::id).containsExactly(score);
        var old=config.period(source.facts());
        var request=new PeriodDraftRequest(key(),user,month,source.scheme(),source.calendar(),old.componentFacts(),old.performanceFacts(),
            old.cycleStart(),old.cycleEnd(),old.cycleStandardMinutes(),old.restCompensatedMinutes(),old.agreedDueDate(),old.statutoryDueDate(),"补齐原主体计薪事实");
        var draft=config.draftPeriod(request);
        assertThat(draft.projectId()).isEqualTo(project);assertThat(draft.payerCompanyId()).isEqualTo(project);
        // 旧主体的历史办理权不扩大为当前员工新任岗或新扣罚权。
        assertThatThrownBy(()->config.deductScore(new ScoreRequest(key(),user,month,"test_points","不可代新主体扣分"))).isInstanceOf(ApiException.class);
        StaffTestActors.actor(reader,"staff",other,null);StaffTestActors.alignRowToSession(jdbc);
        assertThat(config.periods(user,month)).isEmpty();assertThat(config.scores(user,month)).isEmpty();
        assertThatThrownBy(()->config.draftPeriod(request)).isInstanceOf(ApiException.class);
    }

    @Test void currentReviewerMustAlsoHaveEvidencePermissionForTheOriginalSubject() {
        var draft=config.draftScheme(new SchemeDraftRequest(key(),user,LocalDate.of(2025,7,1),null,"test_position",
            List.of(new Component("base","基础工资","base","fixed_monthly",500000,"month","proportional","合同月薪")),
            null,new WorkTimeTerms("standard",480,2400,1000,15000,20000,30000,"已核制度","已核计薪"),
            "每月5日发薪","验证真实当前审批人的附件权限交集"));
        var pending=config.submitScheme(draft.id(),new SubmitRequest(key(),List.of(upload("staff_payroll_scheme",draft.id())),
            List.of(new SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance))));
        String reviewer=leaders.getFirst(),other="evidence_other_"+key(),approveRole="review_role_"+key(),evidenceRole="evidence_role_"+key();
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','证据权限所属项目','active',now(),now())",other);
        jdbc.update("DELETE FROM user_roles WHERE user_id=?",reviewer);
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,view_all_projects,created_at) VALUES(?,?,'跨项目审批','project','active',true,now()),(?,?,'仅本项目证据','project','active',false,now())",
            approveRole,approveRole,evidenceRole,evidenceRole);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'governance.supervisor_review.approve'),(?,'evidence.timeline.view')",approveRole,evidenceRole);
        access.assignUserRole(reviewer,approveRole);access.assignUserRole(reviewer,evidenceRole);
        StaffTestActors.actor(reviewer,"staff",other,null);StaffTestActors.alignRowToSession(jdbc);
        assertThat(config.currentReviewer("staff_payroll_scheme",draft.id(),pending.reviewId())).isTrue();
        // 工资配置服务的范围校验统一抛 PAYROLL_SCOPE_MISMATCH（项目、商户或发薪主体），证据权限不覆盖原主体同样走这一码。
        assertThatThrownBy(()->config.requireEvidenceScope("staff_payroll_scheme",draft.id(),"evidence.timeline.view"))
            .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PAYROLL_SCOPE_MISMATCH"));
        jdbc.update("UPDATE roles SET view_all_projects=true WHERE id=?",evidenceRole);
        assertThat(config.requireEvidenceScope("staff_payroll_scheme",draft.id(),"evidence.timeline.view")).isNull();
    }

    @Test void terminationPayrollSettlesAloneOnStatutoryDateAndBlocksSecondMonthly() {
        // BSM L1673「离职工资依法单独结清，不等待普通月结或交接结束」「工资按合法约定及适用劳动规定的日期支付，遇节假日或休息日提前至最近工作日」；L1500 离职结清按工资专用规则执行。
        prepare(500000,0);String review="pw_departed_"+key();LocalDate statutory=LocalDate.of(2025,7,20);
        // 已生效离职审批原件夹具：after_status='departed' 且 applied_at 非空；审批链本身由人事三级审批用例覆盖。
        jdbc.update("INSERT INTO supervisor_review_decisions(review_id,policy_code,action_type,object_type,object_id,blocked,review_status,required_level,evaluated_at,initiated_by) VALUES(?,'fixture','fixture','fixture',?,false,'approved',1,now(),?)",review,review,actor);
        jdbc.update("INSERT INTO staff_employment_status_revisions(review_id,profile_id,user_id,before_status,after_status,reason,initiated_by,effective_at,applied_at) SELECT ?,id,user_id,'employed','departed','已批准离职',?,now(),now() FROM staff_profiles WHERE user_id=?",review,actor,user);
        // 法定结清日 7/20 早于约定发薪日 8/5，且日历把 7/20 定为休息日：应提前到最近工作日 7/19。已批原件不可改写，另立已批日历与计薪依据。
        String calendar="pc_term_"+key(),facts="pf_term_"+key();
        var days=config.calendar(source.calendar()).days().stream().map(d->d.date().equals(statutory)?new CalendarDay(d.date(),"rest_day"):d).toList();
        jdbc.update("INSERT INTO staff_payroll_calendar_versions(id,project_id,version,start_date,end_date,days,source_basis,reason,status,created_by,approved_at) SELECT ?,project_id,'V0.02',start_date,end_date,?::jsonb,source_basis,'测试离职结清日休息日','approved',created_by,now() FROM staff_payroll_calendar_versions WHERE id=?",calendar,config.encode(days),source.calendar());
        jdbc.update("INSERT INTO staff_payroll_period_facts(id,user_id,month,project_id,merchant_id,payer_company_id,scheme_version_id,calendar_version_id,component_facts,performance_facts,cycle_start,cycle_end,cycle_standard_minutes,rest_compensated_minutes,agreed_due_date,statutory_due_date,reason,status,created_by,approved_at) SELECT ?,user_id,month,project_id,merchant_id,payer_company_id,scheme_version_id,?,component_facts,performance_facts,cycle_start,cycle_end,cycle_standard_minutes,rest_compensated_minutes,agreed_due_date,?,'测试依法核定离职结清日','approved',created_by,now() FROM staff_payroll_period_facts WHERE id=?",facts,calendar,statutory,source.facts());
        source=new PayrollApprovedFactsFixture.Source(source.scheme(),calendar,facts);
        assertThatThrownBy(()->payroll.calculate(new SettleRequest(key(),user,month,"termination",source.facts(),null)))
            .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(()->payroll.calculate(new SettleRequest(key(),user,month,"termination",source.facts(),"not-applied-"+key())))
            .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PAYROLL_INPUT_INVALID"));
        var r=new SettleRequest(key(),user,month,"termination",source.facts(),review);var calc=payroll.calculate(r);
        var detail=settle(r,calc,tax(calc,0));
        assertThat(detail.statementKind()).isEqualTo("termination");
        assertThat(detail.paymentDueDate()).isEqualTo(statutory.minusDays(1));
        assertThat(detail.paymentDueBasis()).containsEntry("statutory_due_date",statutory.toString()).containsEntry("resolved_due_date",statutory.minusDays(1).toString());
        assertThat(jdbc.queryForObject("SELECT due_date FROM ar_ap_items WHERE id=?",LocalDate.class,detail.arApItemId())).isEqualTo(statutory.minusDays(1));
        // 离职工资已单独结清，本月不再有普通月结。
        assertThatThrownBy(()->payroll.calculate(request("monthly")))
            .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PAYROLL_ALREADY_LOCKED"));
    }
    @Test void actualDisabledCapabilityPreventsPayrollCalculationAndConfigurationWrites() {
        prepare(500000,0);
        jdbc.update("INSERT INTO terminal_capability_switches(id,terminal_type,capability_code,scope_type,scope_id,enabled,reason,created_at,updated_at) VALUES(?,'*','staff.payroll.manage','global','global',false,'测试关闭',now(),now()) ON CONFLICT(terminal_type,capability_code,scope_type,scope_id) DO UPDATE SET enabled=false",key());
        assertThatThrownBy(()->payroll.calculate(request("monthly"))).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
        assertThatThrownBy(()->config.draftScheme(null)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_payroll_calculation_drafts WHERE user_id=?",Integer.class,user)).isZero();
    }
}
