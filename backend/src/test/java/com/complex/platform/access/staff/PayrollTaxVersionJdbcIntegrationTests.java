package com.complex.platform.access.staff;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.staff.dto.PayrollTaxBracketRequest;
import com.complex.platform.access.staff.dto.StaffPayrollCloseRequest;
import com.complex.platform.common.api.ApiException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.complex.platform.evidence.EvidenceEventService;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/** HP-H11：维护权限不能代替每次税费审批，未核定税目不能凭服务名直接算税。 */
@SpringBootTest(properties = {
    "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
    "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
    "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class PayrollTaxVersionJdbcIntegrationTests {
    @Autowired StaffService staff;
    @Autowired JdbcStaffRepository staffRepository;
    @Autowired JdbcTemplate jdbc;
    @Autowired AccessRepository access;
    @Autowired PayrollTaxService taxes;
    @Autowired StaffPayrollService payroll;
    @Autowired com.complex.platform.finance.FinancePayrollService bankPayroll;
    private PayrollApprovedFactsFixture.Source payrollFacts;
    @Autowired SupervisorReviewService reviews;
    @Autowired EvidenceEventService evidence;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    private String project;
    private String employee;
    private String actor;
    private String finance;
    private String taxReviewer;
    private String legal;

    @BeforeEach void setup() {
        String suffix=UUID.randomUUID().toString().substring(0,8);
        project="tax_project_"+suffix; employee="tax_employee_"+suffix; actor="tax_actor_"+suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','税档回归','active',now(),now())",project);
        StaffTestActors.grant(jdbc,access,actor,project);
        StaffTestActors.grant(jdbc,access,employee,project);
        jdbc.update("UPDATE roles SET view_all_projects=true WHERE id='role_m010_fixture'");
        jdbc.update("INSERT INTO staff_profiles(id,project_id,user_id,name,company_id,registration_status,status,employment_status,created_at,updated_at) VALUES(?,?,?,'税档员工',?,'approved','active','employed',now(),now())", "profile_"+suffix,project,employee,project);
        StaffTestActors.actor(actor,"staff",project,null);
        StaffTestActors.alignRowToSession(jdbc);
        finance="tax_finance_"+suffix;taxReviewer="tax_review_"+suffix;legal="tax_legal_"+suffix;
        for(String reviewer:List.of(finance,taxReviewer,legal))StaffTestActors.grant(jdbc,access,reviewer,project);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT 'role_m010_fixture',permission_code FROM permissions WHERE permission_code IN ('evidence.attachment.create','evidence.timeline.view','governance.supervisor_review.view') ON CONFLICT DO NOTHING");
        for(var line:lines())jdbc.update("INSERT INTO approval_lines(line_code,line_name,min_start_rank,entry_user_id,status) VALUES(?,?,1,?,'active') ON CONFLICT(line_code) DO UPDATE SET entry_user_id=EXCLUDED.entry_user_id,status='active'",line.lineCode(),line.lineCode(),line.finalApproverId());
    }
    @AfterEach void cleanup() {SecurityContextHolder.clearContext();}

    @Test void singleBracketWriteCannotBypassCompleteVersionApproval() {
        String id="tax_direct_"+UUID.randomUUID();
        assertThatThrownBy(()->staff.upsertTaxBracket(new PayrollTaxBracketRequest(id,"test_tax_"+actor,1,
                0L,null,10000,0L,1000,0L,LocalDate.of(2099,1,1),UUID.randomUUID().toString())))
            .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("TAX_VERSION_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payroll_tax_brackets WHERE id=?",Integer.class,id)).isZero();
    }

    @Test void serviceIncomeWithoutProfessionalTaxAssessmentCannotCloseAtAssumedTaxCategory() {
        income("first",1000000);
        assertThatThrownBy(()->staff.closePayroll(new StaffPayrollCloseRequest(UUID.randomUUID().toString(),"2099-07")))
            .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PAYROLL_CONFIRMED_FACTS_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_payroll_statements WHERE user_id=? AND month='2099-07'",Integer.class,employee)).isZero();
    }

    @Test void completeVersionNeedsThreeDepartmentReviewsThenHighestConfirmation() {
        var draft=draftVersion(LocalDate.of(2099,1,1),1000);
        assertThat(draft.status()).isEqualTo("draft");
        assertThatThrownBy(()->taxes.effective(category(),LocalDate.of(2099,7,1))).isInstanceOf(ApiException.class);
        var pending=taxes.submitVersion(draft.id(),submit(draft.id(),PayrollTaxService.VERSION));
        assertThat(pending.status()).isEqualTo("pending_review");
        StaffTestActors.master();
        assertThatThrownBy(()->reviews.approve(pending.reviewId(),decision("approved"))).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("DEPARTMENT_REVIEWS_PENDING"));
        for(String user:List.of(finance,taxReviewer,legal))approve(pending.reviewId(),user);
        assertThat(taxes.version(draft.id(),false).status()).isEqualTo("pending_review");
        StaffTestActors.master();reviews.approve(pending.reviewId(),decision("approved"));
        act(actor);
        assertThat(taxes.effective(category(),LocalDate.of(2099,7,1)).id()).isEqualTo(draft.id());
    }

    @Test void missingDepartmentOrSelfApprovalCannotPublish() {
        var draft=draftVersion(LocalDate.of(2099,1,1),1000);
        var request=submit(draft.id(),PayrollTaxService.VERSION);
        assertThatThrownBy(()->taxes.submitVersion(draft.id(),new PayrollTaxVersion.SubmitRequest(key(),request.evidenceAttachmentIds(),List.of(lines().getFirst()))))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("APPROVAL_LINE_FINAL_APPROVER_REQUIRED"));
        var row=taxes.submitVersion(draft.id(),request);
        assertThatThrownBy(()->reviews.approve(row.reviewId(),decision("approved"))).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        assertThat(taxes.version(row.id(),false).status()).isEqualTo("pending_review");
    }

    @Test void unuploadedOrOtherDraftEvidenceCannotSubmit() {
        var draft=draftVersion(LocalDate.of(2099,1,1),1000);
        assertThatThrownBy(()->taxes.submitVersion(draft.id(),new PayrollTaxVersion.SubmitRequest(key(),List.of("not-uploaded"),lines()))).isInstanceOf(ApiException.class);
        var other=draftVersion(LocalDate.of(2099,2,1),1500);
        var otherFile=upload(PayrollTaxService.VERSION,other.id());
        assertThatThrownBy(()->taxes.submitVersion(draft.id(),new PayrollTaxVersion.SubmitRequest(key(),List.of(otherFile),lines()))).isInstanceOf(ApiException.class);
        assertThat(taxes.version(draft.id(),false).status()).isEqualTo("draft");
    }

    @Test void rejectionRetainsDraftAndDoesNotSelectIt() {
        var draft=draftVersion(LocalDate.of(2099,1,1),1000);
        var pending=taxes.submitVersion(draft.id(),submit(draft.id(),PayrollTaxService.VERSION));
        act(finance);reviews.approve(pending.reviewId(),decision("rejected"));act(actor);
        assertThat(taxes.version(pending.id(),false).status()).isEqualTo("rejected");
        assertThatThrownBy(()->taxes.effective(category(),LocalDate.of(2099,7,1))).isInstanceOf(ApiException.class);
    }

    @Test void approvedFutureTableIsNotUsedBeforeItsEffectiveDate() {
        var old=approvedVersion(LocalDate.of(2099,1,1),1000);
        var next=approvedVersion(LocalDate.of(2099,8,1),2000);
        assertThat(taxes.effective(category(),LocalDate.of(2099,7,31)).id()).isEqualTo(old.id());
        assertThat(taxes.effective(category(),LocalDate.of(2099,8,1)).id()).isEqualTo(next.id());
    }

    @Test void approvedAssessmentClosesAndLocksTaxVersionAgainstLaterApprovedVersion() {
        var old=approvedVersion(LocalDate.of(2099,1,1),1000);income("first",1000000);
        var calculation=fullCalculation();var assessment=completeAssessment(calculation,100000,true);
        var original=closeComplete(calculation,assessment);
        approvedVersion(LocalDate.of(2099,7,1),2000);
        assertThatThrownBy(()->closeComplete(calculation,assessment)).isInstanceOf(ApiException.class);
        var statement=payroll.detail(original.statement().id(),false).statement();
        assertThat(statement.taxVersionId()).isEqualTo(old.id());
        assertThat(statement.taxAssessmentId()).isEqualTo(assessment.id());assertThat(statement.taxCent()).isEqualTo(100000);
    }

    @Test void pendingAssessmentCannotCloseAndApprovedIncomeChangesRequireNewAssessment() {
        approvedVersion(LocalDate.of(2099,1,1),1000);income("first",1000000);
        var calculation=fullCalculation();var pending=completeAssessment(calculation,100000,false);
        assertThatThrownBy(()->closeComplete(calculation,pending)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PAYROLL_TAX_ASSESSMENT_REQUIRED"));
        approveAll(pending.reviewId());income("second",100000);
        assertThatThrownBy(()->closeComplete(calculation,taxes.assessment(pending.id(),false))).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PAYROLL_SOURCE_CHANGED"));
    }

    @Test void changedIncomeCannotReplacePayrollLockedAssessmentEvenWithSameTaxVersion() {
        var original=approvedVersion(LocalDate.of(2099,1,1),1000);income("first",1000000);
        var calculation=fullCalculation();var assessment=completeAssessment(calculation,100000,true);closeComplete(calculation,assessment);
        approvedVersion(LocalDate.of(2099,7,1),2000);income("second",100000);
        assertThatThrownBy(this::draftAssessment).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PAYROLL_TAX_ASSESSMENT_LOCKED"));
        assertThatThrownBy(()->closeComplete(calculation,assessment)).isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("SELECT tax_cent FROM staff_payroll_statements WHERE user_id=? AND month='2099-07'",Long.class,employee)).isEqualTo(100000);
        assertThat(jdbc.queryForObject("SELECT tax_version_id FROM staff_payroll_statements WHERE user_id=? AND month='2099-07'",String.class,employee)).isEqualTo(original.id());
    }

    @Test void historicalPayrollPaymentDoesNotRequireTaxBackfill() {
        jdbc.update("INSERT INTO staff_payroll_statements(id,user_id,month,base_cent,commission_cent,bonus_cent,penalty_cent,leave_deduction_cent,tax_cent,total_cent,status,closed_at,project_id,payer_company_id) VALUES(?,?,'2099-07',0,1000000,0,0,0,100000,900000,'closed',now(),?,?)","legacy_"+actor,employee,project,project);
        income("historical",1000000);
        assertThatThrownBy(()->staff.markPayrollPaid(new com.complex.platform.access.staff.dto.StaffPayrollPayRequest(key(),"2099-07",employee,"bank_transfer","legacy_fixture")))
            .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PAYROLL_FINANCE_RESULT_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT status FROM staff_payroll_statements WHERE user_id=? AND month='2099-07'",String.class,employee)).isEqualTo("closed");
        assertThat(jdbc.queryForObject("SELECT tax_assessment_id FROM staff_payroll_statements WHERE user_id=? AND month='2099-07'",String.class,employee)).isNull();
    }

    @Test void databaseDoesNotAllowReplacingLockedAssessment() {
        approvedVersion(LocalDate.of(2099,1,1),1000);income("first",1000000);
        var calculation=fullCalculation();var first=completeAssessment(calculation,100000,true);var second=completeAssessment(calculation,100000,true);
        closeComplete(calculation,second);
        assertThat(jdbc.queryForObject("SELECT tax_assessment_id FROM staff_payroll_statements WHERE user_id=? AND month='2099-07'",String.class,employee)).isEqualTo(second.id());
        assertThatThrownBy(()->jdbc.update("UPDATE staff_payroll_statements SET tax_assessment_id=? WHERE user_id=? AND month='2099-07'",first.id(),employee)).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test void reassessmentCannotRewriteAlreadyPaidPayroll() {
        approvedVersion(LocalDate.of(2099,1,1),1000);income("first",1000000);
        var calculation=fullCalculation();var assessment=completeAssessment(calculation,100000,true);
        var statement=closeComplete(calculation,assessment);payActually(statement.statement().id());
        assertThat(jdbc.queryForObject("SELECT status FROM staff_payroll_statements WHERE id=?",String.class,statement.statement().id())).isEqualTo("paid");
        assertThatThrownBy(this::draftAssessment).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PAYROLL_ALREADY_PAID"));
    }

    @Test void draftCannotChangeAfterCreationAtDatabaseLayer() {
        var row=draftVersion(LocalDate.of(2099,1,1),1000);
        assertThatThrownBy(()->jdbc.update("UPDATE payroll_tax_versions SET category='changed' WHERE id=?",row.id())).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test void allTableRowsMustBeContinuousAndExplicit() {
        assertThatThrownBy(()->taxes.draft(new PayrollTaxVersion.DraftRequest(key(),category(),LocalDate.of(2099,1,1),"测试身份依据","测试分档依据",PayrollTaxService.ROUNDING,
                List.of(new PayrollTaxVersion.Bracket(1,0L,100L,10000,0L,1000,0L),new PayrollTaxVersion.Bracket(2,102L,null,10000,0L,1000,0L)),"缺口测试")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(()->taxes.draft(new PayrollTaxVersion.DraftRequest(key(),category(),LocalDate.of(2099,1,1),"测试身份依据","测试分档依据",PayrollTaxService.ROUNDING,
                List.of(new PayrollTaxVersion.Bracket(1,0L,null,10000,0L,null,0L)),"缺费率测试")))
                .isInstanceOf(ApiException.class);
    }

    @Test void arbitraryThreePeopleCannotReplaceConfiguredDepartmentRoutes() {
        var draft=draftVersion(LocalDate.of(2099,1,1),1000);var request=submit(draft.id(),PayrollTaxService.VERSION);
        jdbc.update("UPDATE approval_lines SET entry_user_id=NULL WHERE line_code='tax_review'");
        assertThatThrownBy(()->taxes.submitVersion(draft.id(),request)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("TAX_DEPARTMENT_ROUTING_REQUIRED"));
        jdbc.update("UPDATE approval_lines SET entry_user_id=? WHERE line_code='tax_review'",taxReviewer);
        var wrong=List.of(new SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance),new SupervisorReviewEvaluateRequest.ParallelLine("tax_review",legal),new SupervisorReviewEvaluateRequest.ParallelLine("legal_review",legal));
        assertThatThrownBy(()->taxes.submitVersion(draft.id(),new PayrollTaxVersion.SubmitRequest(key(),request.evidenceAttachmentIds(),wrong)))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("FINAL_APPROVER_NOT_IN_CHAIN"));
        assertThat(taxes.version(draft.id(),false).status()).isEqualTo("draft");
    }

    @Test void callbackReplayIsHarmlessAndForgedPendingApprovalDoesNotPublish() {
        var draft=draftVersion(LocalDate.of(2099,1,1),1000);var p=taxes.submitVersion(draft.id(),submit(draft.id(),PayrollTaxService.VERSION));
        var pending=reviews.findReview(p.reviewId()).orElseThrow();
        var forged=new com.complex.platform.governance.review.SupervisorReviewDecisionRecord(pending.reviewId(),pending.policyCode(),pending.actionType(),pending.objectType(),pending.objectId(),false,"approved",pending.requiredLevel(),null,"approved",pending.reason(),pending.evidenceAttachmentIds(),pending.evaluatedAt(),pending.initiatedBy(),pending.approvalLevel(),pending.requiredApproverRank());
        taxes.decided(forged);assertThat(taxes.version(p.id(),false).status()).isEqualTo("pending_review");
        approveAll(p.reviewId());var before=taxes.version(p.id(),false);taxes.decided(reviews.findReview(p.reviewId()).orElseThrow());
        assertThat(taxes.version(p.id(),false)).isEqualTo(before);
    }

    @Test void realHttpVersionAndAssessmentRoutesReturnDraftsAndRespectPermissions() throws Exception {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        var body=new PayrollTaxVersion.DraftRequest(key(),category(),LocalDate.of(2099,1,1),"测试身份","测试公式",PayrollTaxService.ROUNDING,
                List.of(new PayrollTaxVersion.Bracket(1,0L,null,10000,0L,1000,0L)),"HTTP草案");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/staff/payroll/tax-versions")
                .header("X-Terminal-Type","web").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth))
                .contentType("application/json").content(json.copy().setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE).writeValueAsString(body)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.status").value("draft"));
        act(actor);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/staff/payroll/tax-assessments").param("user_id",employee).param("month","2099-07")
                .header("X-Terminal-Type","web").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        jdbc.update("DELETE FROM user_roles WHERE user_id=?",actor);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/staff/payroll/tax-versions")
                .header("X-Terminal-Type","web").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth))
                .contentType("application/json").content(json.copy().setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE).writeValueAsString(body)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }

    @Test void businessAndEvidencePermissionsMustCoverTheSameProject() {
        var version=approvedVersion(LocalDate.of(2099,1,1),1000);income("first",1000000);
        var assessment=draftAssessment();
        String other="tax_other_"+UUID.randomUUID().toString().substring(0,8);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','另一项目','active',now(),now())",other);
        jdbc.update("DELETE FROM role_permissions WHERE role_id='role_m010_fixture' AND permission_code IN ('evidence.attachment.create','evidence.timeline.view')");
        String role="tax_evidence_"+actor;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'本项目证据权限','project','active',now())",role,role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN ('evidence.attachment.create','evidence.timeline.view')",role);
        access.assignUserRole(actor,role);
        StaffTestActors.actor(actor,"staff",other,null);StaffTestActors.alignRowToSession(jdbc);
        assertThat(taxes.assessments(employee,"2099-07")).hasSize(1);
        assertThatThrownBy(()->upload(PayrollTaxService.ASSESSMENT,assessment.id())).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("TAX_EVIDENCE_SCOPE_MISMATCH"));
        assertThatThrownBy(()->taxes.requireEvidenceScope(PayrollTaxService.VERSION,version.id(),"evidence.timeline.view")).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("TAX_EVIDENCE_SCOPE_MISMATCH"));
        act(actor);
        assertThat(upload(PayrollTaxService.ASSESSMENT,assessment.id())).isNotBlank();
    }

    @Test void employeeTransferCannotCombineNewProjectBusinessWithOldProjectEvidenceScope() {
        approvedVersion(LocalDate.of(2099,1,1),1000);income("first",1000000);var assessment=draftAssessment();
        String other="tax_new_"+UUID.randomUUID().toString().substring(0,8);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','员工新项目','active',now(),now())",other);
        jdbc.update("UPDATE staff_profiles SET project_id=?,company_id=? WHERE user_id=?",other,other,employee);
        jdbc.update("UPDATE access_users SET organization_id=? WHERE id=?",other,employee);
        jdbc.update("UPDATE roles SET view_all_projects=false WHERE id='role_m010_fixture'");
        jdbc.update("DELETE FROM role_permissions WHERE role_id='role_m010_fixture' AND permission_code='evidence.timeline.view'");
        String role="tax_old_evidence_"+actor;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'原项目证据范围','project','active',now())",role,role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'evidence.timeline.view')",role);
        jdbc.update("INSERT INTO role_viewable_projects(role_id,viewable_id) VALUES(?,?)",role,project);
        access.assignUserRole(actor,role);
        StaffTestActors.actor(actor,"staff",other,null);StaffTestActors.alignRowToSession(jdbc);
        assertThat(taxes.assessments(employee,"2099-07")).isEmpty();
        assertThatThrownBy(()->taxes.requireEvidenceScope(PayrollTaxService.ASSESSMENT,assessment.id(),"evidence.timeline.view"))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("STAFF_SCOPE_MISMATCH"));
    }

    @Test void broaderTaxReadGrantIsNotShadowedByNarrowPayrollGrant() {
        approvedVersion(LocalDate.of(2099,1,1),1000);income("first",1000000);
        var assessment=draftAssessment();
        String other="tax_reader_project_"+UUID.randomUUID().toString().substring(0,8);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','读权限本项目','active',now(),now())",other);
        jdbc.update("DELETE FROM role_permissions WHERE role_id='role_m010_fixture' AND permission_code IN ('staff.payroll.manage','staff.tax_bracket.manage')");
        String payrollRole="tax_narrow_payroll_"+actor;
        String taxRole="tax_broad_reader_"+actor;
        for(String role:List.of(payrollRole,taxRole)) {
            jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'税档读取范围测试','project','active',now())",role,role);
            access.assignUserRole(actor,role);
        }
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'staff.payroll.manage'),(?,'staff.tax_bracket.manage')",payrollRole,taxRole);
        jdbc.update("INSERT INTO role_viewable_projects(role_id,viewable_id) VALUES(?,?)",taxRole,project);
        StaffTestActors.actor(actor,"staff",other,null);StaffTestActors.alignRowToSession(jdbc);
        assertThat(taxes.assessments(employee,"2099-07")).extracting(PayrollTaxAssessment::id).containsExactly(assessment.id());
        assertThat(taxes.requireEvidenceScope(PayrollTaxService.ASSESSMENT,assessment.id(),"evidence.timeline.view")).isNull();
        // 员工转到本项目后，不能拿薪资权限的员工范围拼税档权限的原认定范围。
        jdbc.update("UPDATE staff_profiles SET project_id=?,company_id=? WHERE user_id=?",other,other,employee);
        jdbc.update("UPDATE access_users SET organization_id=? WHERE id=?",other,employee);
        assertThat(taxes.assessments(employee,"2099-07")).isEmpty();
        assertThatThrownBy(()->taxes.requireEvidenceScope(PayrollTaxService.ASSESSMENT,assessment.id(),"evidence.timeline.view"))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("STAFF_SCOPE_MISMATCH"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void unrelatedSameProjectEvidenceReaderCannotDownloadPendingTaxAssessmentProof(boolean hasGeneralApproval) throws Exception {
        approvedVersion(LocalDate.of(2099,1,1),1000);income("first",1000000);
        var draft=draftAssessment();
        var submitted=taxes.submitAssessment(draft.id(),submit(draft.id(),PayrollTaxService.ASSESSMENT));
        String outsider="tax_proof_outsider_"+UUID.randomUUID().toString().substring(0,8);
        StaffTestActors.grant(jdbc,access,outsider,project);
        jdbc.update("DELETE FROM user_roles WHERE user_id=?",outsider);
        String role="tax_proof_reader_"+outsider;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,'税务证据读取测试','同项目无税档或工资授权','project','active',now())",role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'evidence.timeline.view')",role);
        if(hasGeneralApproval)jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'governance.supervisor_review.approve'),(?,'governance.supervisor_review.view')",role,role);
        access.assignUserRole(outsider,role);
        act(outsider);
        assertThat(com.complex.platform.governance.chain.ApprovalChainService.currentNodes(reviews.chain(submitted.reviewId())))
                .noneMatch(node->outsider.equals(node.approverId())||outsider.equals(node.forwardedTo()));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/evidence/attachments/"+submitted.evidenceAttachmentIds().getFirst()+"/file")
                        .header("X-Terminal-Type","web")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(SecurityContextHolder.getContext().getAuthentication())))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }

    @Test void currentTaxAssessmentReviewerReadsProofOnlyWhileApprovalPermissionRemainsEffective() throws Exception {
        approvedVersion(LocalDate.of(2099,1,1),1000);income("first",1000000);
        var draft=draftAssessment();
        var submitted=taxes.submitAssessment(draft.id(),submit(draft.id(),PayrollTaxService.ASSESSMENT));
        jdbc.update("DELETE FROM role_permissions WHERE role_id='role_m010_fixture' AND permission_code IN ('staff.payroll.manage','staff.tax_bracket.manage')");
        act(finance);
        assertThat(com.complex.platform.governance.chain.ApprovalChainService.currentNodes(reviews.chain(submitted.reviewId())))
                .anyMatch(node->finance.equals(node.approverId()));
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/evidence/attachments/"+submitted.evidenceAttachmentIds().getFirst()+"/file")
                        .header("X-Terminal-Type","web")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(authentication)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/png"));
        jdbc.update("DELETE FROM role_permissions WHERE role_id='role_m010_fixture' AND permission_code='governance.supervisor_review.approve'");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/evidence/attachments/"+submitted.evidenceAttachmentIds().getFirst()+"/file")
                        .header("X-Terminal-Type","web")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(authentication)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }

    @Test void actualTaxReviewerReadsOriginalAndUploadedProofWithoutTaxMaintenanceGrant() throws Exception {
        var draft=draftVersion(LocalDate.of(2099,1,1),1000);
        var submitted=taxes.submitVersion(draft.id(),submit(draft.id(),PayrollTaxService.VERSION));
        jdbc.update("DELETE FROM role_permissions WHERE role_id='role_m010_fixture' AND permission_code IN ('staff.payroll.manage','staff.tax_bracket.manage')");
        act(finance);
        var pending=reviews.listPending(200).items().stream().filter(item->item.decision().reviewId().equals(submitted.reviewId())).findFirst().orElseThrow();
        assertThat(pending.decision().reason()).contains(draft.calculationBasis(),draft.category(),draft.version());
        assertThat(pending.decision().evidenceAttachmentIds()).isEqualTo(submitted.evidenceAttachmentIds());
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/platform/supervisor-review/"+submitted.reviewId())
                        .header("X-Terminal-Type","web")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(authentication)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.reason").value(pending.decision().reason()));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/evidence/attachments/"+submitted.evidenceAttachmentIds().getFirst()+"/file")
                        .header("X-Terminal-Type","web")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(authentication)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/png"));
    }

    @Test void masterReadsOneCrossProjectTaxOriginalAndCompletesFinalConfirmation() {
        var draft=draftVersion(LocalDate.of(2099,1,1),1000);
        var pending=taxes.submitVersion(draft.id(),submit(draft.id(),PayrollTaxService.VERSION));
        for(String user:List.of(finance,taxReviewer,legal))approve(pending.reviewId(),user);
        StaffTestActors.master();
        assertThat(reviews.listPending(200).items()).noneMatch(item->pending.reviewId().equals(item.decision().reviewId()));
        var original=reviews.readReview(pending.reviewId());
        assertThat(original.reviewStatus()).isEqualTo("pending_review");
        assertThat(original.reason()).contains(draft.category(),draft.calculationBasis());
        assertThat(original.evidenceAttachmentIds()).isEqualTo(pending.evidenceAttachmentIds());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='governance.supervisor_review.viewed_cross_scope' AND business_id=?",Integer.class,pending.reviewId())).isPositive();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='governance.supervisor_review.original_viewed' AND business_id=?",Integer.class,pending.reviewId())).isPositive();
        reviews.approve(pending.reviewId(),decision("approved"));
        assertThat(taxes.version(draft.id(),false).status()).isEqualTo("approved");
    }

    @Test void unrelatedCrossProjectReviewerCannotReadOriginalById() throws Exception {
        var draft=draftVersion(LocalDate.of(2099,1,1),1000);
        var pending=taxes.submitVersion(draft.id(),submit(draft.id(),PayrollTaxService.VERSION));
        String other="tax_outside_"+UUID.randomUUID().toString().substring(0,8), outsider="reader_"+other;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','非经办项目','active',now(),now())",other);
        StaffTestActors.grant(jdbc,access,outsider,other);
        StaffTestActors.actor(outsider,"staff",other,null);StaffTestActors.alignRowToSession(jdbc);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/platform/supervisor-review/"+pending.reviewId())
                        .header("X-Terminal-Type","web")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(SecurityContextHolder.getContext().getAuthentication())))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.detail_code").value("SUPERVISOR_REVIEW_SCOPE_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE actor_id=? AND event_name='governance.supervisor_review.original_view_failed' AND business_id=?",Integer.class,outsider,pending.reviewId())).isEqualTo(1);
    }

    @Test void legacyHistoricalTaxReadEmitsActualSuccessAndFailureAudit() {
        staff.listTaxBrackets(category());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE actor_id=? AND event_name='staff.payroll.tax_bracket_viewed' AND event_result='success'",Integer.class,actor)).isEqualTo(1);
        jdbc.update("DELETE FROM user_roles WHERE user_id=?",actor);
        assertThatThrownBy(()->staff.listTaxBrackets(category())).isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE actor_id=? AND event_name='staff.payroll.tax_bracket_view_failed' AND event_result='failure'",Integer.class,actor)).isEqualTo(1);
    }

    @Test void monthCloseKeepsApprovedOriginalPayerAfterEmployeeTransfer() {
        approvedVersion(LocalDate.of(2099,1,1),1000);income("first",1000000);
        var calculation=fullCalculation();var assessment=completeAssessment(calculation,100000,true);
        String other=newProject();jdbc.update("UPDATE staff_profiles SET company_id=? WHERE user_id=?",other,employee);
        assertOriginalPayrollSubject(closeComplete(calculation,assessment),assessment,other);
    }

    @Test void monthCloseKeepsApprovedOriginalProjectAfterEmployeeTransfer() {
        approvedVersion(LocalDate.of(2099,1,1),1000);income("first",1000000);
        var calculation=fullCalculation();var assessment=completeAssessment(calculation,100000,true);
        String other=newProject();jdbc.update("UPDATE staff_profiles SET project_id=? WHERE user_id=?",other,employee);
        jdbc.update("UPDATE access_users SET organization_id=? WHERE id=?",other,employee);
        assertOriginalPayrollSubject(closeComplete(calculation,assessment),assessment,other);
    }

    private void assertOriginalPayrollSubject(StaffPayrollModels.StatementDetail detail,PayrollTaxAssessment assessment,String other) {
        // 调岗不把已批准原工资及税务原件迁给新主体；也不能用原税额给新主体定格。
        var statement=detail.statement();
        assertThat(statement.payerCompanyId()).isEqualTo(project).isNotEqualTo(other);
        assertThat(jdbc.queryForObject("SELECT project_id FROM staff_payroll_statements WHERE id=?",String.class,statement.id())).isEqualTo(project);
        assertThat(statement.taxAssessmentId()).isEqualTo(assessment.id());
        assertThat(statement.taxVersionId()).isEqualTo(assessment.taxVersionId());
        assertThat(statement.commissionCent()).isEqualTo(1000000);
        assertThat(statement.taxCent()).isEqualTo(100000);
        assertThat(statement.totalCent()).isEqualTo(900000);
        assertThat(detail.schemeVersionId()).isEqualTo(payrollFacts.scheme());
        assertThat(detail.factsId()).isEqualTo(payrollFacts.facts());
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ar_ap_items WHERE id=?",Long.class,detail.arApItemId())).isEqualTo(900000);
    }

    private String newProject() {
        String id="tax_reassigned_"+UUID.randomUUID().toString().substring(0,8);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','发薪归属变更回归','active',now(),now())",id);
        return id;
    }

    private StaffPayrollModels.CalculationDraft fullCalculation() {
        if(payrollFacts==null)payrollFacts=PayrollApprovedFactsFixture.create(jdbc,json,employee,project,actor,"2099-07",0,0);
        return payroll.calculate(new StaffPayrollModels.SettleRequest(key(),employee,"2099-07","monthly",payrollFacts.facts(),null));
    }
    private PayrollTaxAssessment completeAssessment(StaffPayrollModels.CalculationDraft c,long amount,boolean approve) {
        var d=taxes.draftAssessment(new PayrollTaxAssessment.DraftRequest(key(),employee,"2099-07",category(),LocalDate.of(2099,7,31),
            "测试员工身份与主体","本期完整工资来源","测试完整专业计算书","完整工资认定",c.id(),amount,"完整专业原件:"+c.id()));
        var pending=taxes.submitAssessment(d.id(),submit(d.id(),PayrollTaxService.ASSESSMENT));
        if(approve)approveAll(pending.reviewId());return taxes.assessment(d.id(),false);
    }
    private StaffPayrollModels.StatementDetail closeComplete(StaffPayrollModels.CalculationDraft c,PayrollTaxAssessment tax) {
        return payroll.settle(new StaffPayrollModels.SettleRequest(key(),employee,"2099-07","monthly",payrollFacts.facts(),null,c.id(),tax.id()));
    }
    private void payActually(String statement) {
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT 'role_m010_fixture',permission_code FROM permissions WHERE permission_code LIKE 'finance.payroll.%' ON CONFLICT DO NOTHING");
        long total=jdbc.queryForObject("SELECT total_cent FROM staff_payroll_statements WHERE id=?",Long.class,statement);
        String id=(String)bankPayroll.prepare(statement,new com.complex.platform.finance.FinancePayrollService.PrepareRequest(key(),total)).get("id");
        String proof=evidence.uploadAttachment(new MockMultipartFile("file","bank.png","image/png",java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jCYQAAAAASUVORK5CYII=")),"staff_payroll_payment",id,"bank_proof").id();
        var at=java.time.Instant.now().minusSeconds(60);
        bankPayroll.submit(id,new com.complex.platform.finance.FinancePayrollService.SubmitRequest(key(),"request_"+id,at,employee,List.of(proof)));
        bankPayroll.result(id,new com.complex.platform.finance.FinancePayrollService.ResultRequest(key(),"succeeded",at.plusSeconds(1),"flow_"+id,"真实成功测试流水",List.of(proof)));
    }
    private PayrollTaxVersion draftVersion(LocalDate date,int rate) {
        act(actor);return taxes.draft(new PayrollTaxVersion.DraftRequest(key(),category(),date,"仅回归测试人员，非法律税率","测试使用既有分档公式并逐步截断到分",PayrollTaxService.ROUNDING,
                List.of(new PayrollTaxVersion.Bracket(1,0L,null,10000,0L,rate,0L)),"测试完整税表"));
    }
    private PayrollTaxVersion approvedVersion(LocalDate date,int rate){var d=draftVersion(date,rate);var p=taxes.submitVersion(d.id(),submit(d.id(),PayrollTaxService.VERSION));approveAll(p.reviewId());return taxes.version(d.id(),false);}
    private PayrollTaxAssessment draftAssessment(){act(actor);return taxes.draftAssessment(new PayrollTaxAssessment.DraftRequest(key(),employee,"2099-07",category(),LocalDate.of(2099,7,31),"测试人员身份确认","测试真实台账合同性质","测试财税专业文件，不作申报","本月服务收入认定"));}
    private PayrollTaxAssessment approvedAssessment(){var d=draftAssessment();var p=taxes.submitAssessment(d.id(),submit(d.id(),PayrollTaxService.ASSESSMENT));approveAll(p.reviewId());return taxes.assessment(d.id(),false);}
    private PayrollTaxVersion.SubmitRequest submit(String id,String type){return new PayrollTaxVersion.SubmitRequest(key(),List.of(upload(type,id)),lines());}
    private String upload(String type,String id){return evidence.uploadAttachment(new MockMultipartFile("file","professional-basis.png","image/png",java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jCYQAAAAASUVORK5CYII=")),type,id,"professional_basis").id();}
    private List<SupervisorReviewEvaluateRequest.ParallelLine> lines(){return List.of(new SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance),new SupervisorReviewEvaluateRequest.ParallelLine("tax_review",taxReviewer),new SupervisorReviewEvaluateRequest.ParallelLine("legal_review",legal));}
    private void approveAll(String review){for(String user:List.of(finance,taxReviewer,legal))approve(review,user);StaffTestActors.master();reviews.approve(review,decision("approved"));act(actor);}
    private void approve(String review,String user){act(user);reviews.approve(review,decision("approved"));}
    private SupervisorReviewApprovalRequest decision(String value){return new SupervisorReviewApprovalRequest(key(),"测试审核意见",List.of(),null,value,"已核对本测试专业依据与完整数据");}
    private void income(String source,long amount){jdbc.update("INSERT INTO staff_commission_ledger(id,user_id,month,source_type,source_id,amount_cent,created_at,project_id) VALUES(?,?,'2099-07','service_order',?,?,now(),?)","tax_income_"+source+actor,employee,"service_"+source+actor,amount,project);}
    private void act(String user){StaffTestActors.actor(user,"staff",project,null);StaffTestActors.alignRowToSession(jdbc);}
    private String category(){return "test_category_"+actor;}
    private String key(){return UUID.randomUUID().toString();}
}
