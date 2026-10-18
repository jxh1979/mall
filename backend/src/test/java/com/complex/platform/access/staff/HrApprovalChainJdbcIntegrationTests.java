package com.complex.platform.access.staff;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.staff.dto.StaffAdjustmentAppealRequest;
import com.complex.platform.access.staff.dto.StaffEmploymentStatusRequest;
import com.complex.platform.access.staff.dto.StaffPayrollAdjustmentRequest;
import com.complex.platform.access.staff.dto.StaffPayrollCloseRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.governance.chain.ApprovalChainNode;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * CG-12（BSM:1478 / BSM:1487 / ENUM:283 / PRD:757、779「辞退与其他涉及员工的人力操作一样，须经 HR 与该员工直属领导、向上三级复核后生效，
 * 生效即转「离职」」「绩效扣分……员工可对扣分提出申诉，申诉同样报至 HR 与直属领导向上三级裁定，过程与结论均留痕」；
 * PGM-1.7.1「申请人与直接受益人不得审批与自己相关的申请」）。
 *
 * <p>真实 PostgreSQL（Flyway 全量迁移），@Transactional 回滚。组织层级：员工 E → S1 → S2 → S3 → S4；HR 持 staff.hr.review；
 * 发起人 I 不在该链上。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class HrApprovalChainJdbcIntegrationTests {

    @Autowired private StaffService service;
    @Autowired private StaffPayrollService payroll;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired private SupervisorReviewService reviews;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AccessRepository access;
    @Autowired private com.complex.platform.auth.AuthService auth;
    @Autowired private StaffEmploymentStatusJob employmentJob;
    @Autowired private com.complex.platform.access.AuthorizationService authorization;

    private String suffix;
    private String employee;
    private String profileId;
    private String initiator;
    private String hr;
    private List<String> superiors;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        employee = "usr_cg12_e_" + suffix;
        initiator = "usr_cg12_i_" + suffix;
        hr = "usr_cg12_hr_" + suffix;
        superiors = List.of("usr_cg12_s1_" + suffix, "usr_cg12_s2_" + suffix, "usr_cg12_s3_" + suffix, "usr_cg12_s4_" + suffix);
        for (String user : List.of(employee, initiator, hr)) {
            StaffTestActors.grant(jdbc, access, user, "project_main");
        }
        for (String user : superiors) {
            StaffTestActors.grant(jdbc, access, user, "project_main");
        }
        String below = employee;
        for (String superior : superiors) {
            jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by) VALUES (?, ?, 'main', ?)",
                    below, superior, initiator);
            below = superior;
        }
        jdbc.update("INSERT INTO approval_lines(line_code,line_name,min_start_rank,status,hr_final_approver_id) VALUES (?,?,1,'active',?)",
                "dept_" + suffix, "测试部门", hr);
        jdbc.update("UPDATE org_hierarchy SET line_code=? WHERE user_id=?", "dept_" + suffix, employee);
        profileId = "stf_cg12_" + suffix;
        jdbc.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, company_id,
                        registration_status, status, employment_status, created_at, updated_at)
                VALUES (?, 'project_main', NULL, ?, '链测员工', 'project_main', 'approved', 'active', 'employed', now(), now())
                """, profileId, employee);
        // 普通现金扣除上限只以基础工资计算；原基础表用于兼容其他既有岗位用例。
        jdbc.update("INSERT INTO staff_base_salaries (user_id, monthly_salary_cent, updated_at, project_id) VALUES (?, 1000000, now(), 'project_main')", employee);
        actAs(initiator);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** 链生成：HR 节点 + 直属上级向上三级（S1、S2、S3，不含 S4）；全部通过前不写入，全部通过后 departed 生效即「离职」。 */
    @Test
    void employmentStatusChangeNeedsHrPlusThreeSuperiorsAndAppliesOnlyAfterAllApprove() {
        actAs(superiors.get(0));
        SupervisorReviewDecisionRecord review = service.setEmploymentStatus(profileId,
                new StaffEmploymentStatusRequest(key(), "departed", "连续两次旷工"));
        assertThat(review.reviewStatus()).isEqualTo("pending_review");
        assertThat(employmentStatus()).isEqualTo("employed");

        List<ApprovalChainNode> nodes = reviews.chain(review.reviewId());
        assertThat(nodes.stream().filter(n -> ApprovalChainNode.MAIN_LINE.equals(n.lineCode())).map(ApprovalChainNode::approverId).toList())
                .containsExactly(superiors.get(1), superiors.get(2));
        assertThat(nodes.stream().filter(n -> "hr_review".equals(n.lineCode())).map(ApprovalChainNode::approverId).toList())
                .containsExactly(hr);
        assertThat(nodes).extracting(ApprovalChainNode::approverId).doesNotContain(initiator, employee, superiors.get(3));

        for (String approver : List.of(superiors.get(1), superiors.get(2))) {
            approve(review.reviewId(), approver);
            assertThat(employmentStatus()).as("未全部通过不得写入").isEqualTo("employed");
        }
        approve(review.reviewId(), hr);
        assertThat(employmentStatus()).isEqualTo("departed");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name = 'staff.profile.employment_status_changed'"
                + " AND business_id = ? AND metadata->>'after' = 'departed'", Integer.class, profileId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT applied_at IS NOT NULL FROM staff_employment_status_revisions WHERE review_id = ?",
                Boolean.class, review.reviewId())).isTrue();
    }

    /** 任一目标状态都走链（含 suspended），且任一节点驳回即整单退回、不写入。 */
    @Test
    void anyEmploymentStatusTargetGoesThroughChainAndRejectionLeavesStatusUntouched() {
        actAs(superiors.get(0));
        SupervisorReviewDecisionRecord review = service.setEmploymentStatus(profileId,
                new StaffEmploymentStatusRequest(key(), "suspended", "停岗调查"));
        assertThat(employmentStatus()).isEqualTo("employed");
        actAs(superiors.get(1));
        reviews.approve(review.reviewId(), new SupervisorReviewApprovalRequest(key(), "驳回", List.of(), null, "rejected", "事由不足"));
        assertThat(employmentStatus()).isEqualTo("employed");
        actAs(initiator);
        assertThat(reviews.findReview(review.reviewId()).orElseThrow().reviewStatus()).isEqualTo("rejected");
    }

    @Test
    void missingDepartmentHrLeavesPendingAndCanRecoverAfterConfigurationIsCompleted() {
        jdbc.update("UPDATE approval_lines SET hr_final_approver_id=NULL WHERE line_code=?", "dept_" + suffix);
        actAs(superiors.get(0));
        var review = service.setEmploymentStatus(profileId, new StaffEmploymentStatusRequest(key(), "suspended", "待核实"));
        assertThat(review.reviewStatus()).isEqualTo("pending_review");
        assertThat(reviews.chain(review.reviewId())).isEmpty();
        assertThat(employmentStatus()).isEqualTo("employed");
        jdbc.update("UPDATE approval_lines SET hr_final_approver_id=? WHERE line_code=?", hr, "dept_" + suffix);
        assertThat(reviews.regenerateChain(review.reviewId())).extracting(ApprovalChainNode::approverId)
                .containsExactly(superiors.get(1), superiors.get(2), hr);
    }

    @Test
    void genericReviewCannotReplaceDepartmentHrWithCallerSuppliedApprover() {
        var review = reviews.evaluate(new com.complex.platform.governance.review.SupervisorReviewEvaluateRequest(key(),
                "奖惩复核", List.of(), SupervisorReviewService.STAFF_ADJUSTMENT_ACTION, "staff_payroll_adjustment",
                "manual_" + suffix, 100L, "测试", null,
                List.of(new com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine("hr_review", superiors.get(3))),
                employee));
        assertThat(reviews.chain(review.reviewId()).stream().filter(n -> "hr_review".equals(n.lineCode())))
                .extracting(ApprovalChainNode::approverId).containsExactly(hr);
    }

    @Test
    void invalidDepartmentHrCannotProduceACompletableChain() {
        for (String invalid : List.of(employee, initiator)) {
            jdbc.update("UPDATE approval_lines SET hr_final_approver_id=? WHERE line_code=?", invalid, "dept_" + suffix);
            var review = service.addPayrollAdjustment(new StaffPayrollAdjustmentRequest(key(), employee, "2099-08", "penalty", 100L, "待复核"));
            assertThat(reviews.chain(review.reviewId())).isEmpty();
        }
        jdbc.update("UPDATE approval_lines SET hr_final_approver_id=? WHERE line_code=?", hr, "dept_" + suffix);
        jdbc.update("UPDATE access_users SET organization_id='outside_project' WHERE id=?", hr);
        var review = service.addPayrollAdjustment(new StaffPayrollAdjustmentRequest(key(), employee, "2099-08", "penalty", 100L, "待复核"));
        assertThat(reviews.chain(review.reviewId())).isEmpty();
    }

    @Test
    void hrPermissionRevocationAfterSubmissionBlocksApproval() {
        var review = service.addPayrollAdjustment(new StaffPayrollAdjustmentRequest(key(), employee, "2099-08", "penalty", 100L, "待复核"));
        jdbc.update("DELETE FROM role_permissions WHERE role_id='role_m010_fixture' AND permission_code='staff.hr.review'");
        actAs(hr);
        assertThatThrownBy(() -> reviews.approve(review.reviewId(), approval("approved")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("HR_APPROVER_SCOPE_INVALID"));
        assertThat(adjustmentCount("penalty")).isZero();
    }

    @Test
    void repeatedHrEscalationCannotAddFourthSupervisor() {
        actAs(superiors.get(0));
        var review = service.setEmploymentStatus(profileId, new StaffEmploymentStatusRequest(key(), "suspended", "待复核"));
        var later = java.time.Instant.now().plus(java.time.Duration.ofDays(30));
        reviews.escalateOverdue(later);
        reviews.escalateOverdue(later.plus(java.time.Duration.ofDays(30)));
        assertThat(reviews.chain(review.reviewId())).extracting(ApprovalChainNode::approverId)
                .containsOnly(superiors.get(2), hr);
        assertThat(employmentStatus()).isEqualTo("employed");
    }

    @Test
    void hrForwardingRequiresTargetToHoldHrRoleAndEmployeeScope() {
        var outsider = "usr_out_" + suffix;
        StaffTestActors.grant(jdbc, access, outsider, "outside_project");
        var review = service.addPayrollAdjustment(new StaffPayrollAdjustmentRequest(key(), employee, "2099-08", "penalty", 100L, "待复核"));
        actAs(hr);
        assertThatThrownBy(() -> reviews.forward(review.reviewId(),
                new com.complex.platform.governance.review.SupervisorReviewForwardRequest(key(), "转交", outsider, "代办")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("HR_APPROVER_SCOPE_INVALID"));
        assertThat(reviews.chain(review.reviewId()).stream().filter(n -> "hr_review".equals(n.lineCode())))
                .allMatch(n -> n.forwardedTo() == null);
    }

    /** 奖惩：bonus / penalty 都走链，未通过不入账，全部通过后入账。 */
    @Test
    void bonusAndPenaltyAreBookedOnlyAfterChainApproval() {
        SupervisorReviewDecisionRecord penalty = service.addPayrollAdjustment(
                new StaffPayrollAdjustmentRequest(key(), employee, "2099-08", "penalty", 3000L, "迟到"));
        assertThat(adjustmentCount("penalty")).isZero();
        actAs(superiors.get(1));
        assertThatThrownBy(() -> reviews.approve(penalty.reviewId(), approval("approved")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("APPROVAL_NODE_NOT_REACHED"));
        actAs(superiors.get(0));
        reviews.approve(penalty.reviewId(), approval("rejected"));
        assertThat(adjustmentCount("penalty")).as("驳回不入账").isZero();

        actAs(initiator);
        SupervisorReviewDecisionRecord bonus = service.addPayrollAdjustment(
                new StaffPayrollAdjustmentRequest(key(), employee, "2099-08", "bonus", 5000L, "月度之星"));
        assertThat(adjustmentCount("bonus")).isZero();
        approveAll(bonus.reviewId());
        assertThat(adjustmentCount("bonus")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM staff_payroll_adjustments WHERE id = ?", Long.class, bonus.objectId()))
                .isEqualTo(5000L);
    }

    /** 申诉：员工对已生效扣罚提出申诉 → 同链裁定；撤销 = 冲回原调整（原记录保留），工资单净额回正；维持不冲回。 */
    @Test
    void appealOnPenaltyIsDecidedBySameChainAndRevocationReversesWithoutDeletingOriginal() {
        SupervisorReviewDecisionRecord penalty = service.addPayrollAdjustment(
                new StaffPayrollAdjustmentRequest(key(), employee, "2099-09", "penalty", 4000L, "绩效扣分"));
        approveAll(penalty.reviewId());
        String adjustmentId = penalty.objectId();
        actAs(initiator);
        PayrollApprovedCloseFixture.prepare(jdbc,json,employee,"2099-09",1000000);
        var original=PayrollApprovedCloseFixture.closeWithApprovedZeroTax(jdbc,json,payroll,employee,"2099-09","monthly");
        assertThat(statementPenalty("2099-09")).isEqualTo(4000L);

        actAs(employee);
        StaffAdjustmentAppealRecord appeal = service.submitAdjustmentAppeal(adjustmentId,
                new StaffAdjustmentAppealRequest(key(), "当日已报备"));
        assertThat(appeal.status()).isEqualTo("pending");
        List<ApprovalChainNode> nodes = reviews.chain(appeal.reviewId());
        assertThat(nodes.stream().filter(n -> ApprovalChainNode.MAIN_LINE.equals(n.lineCode())).map(ApprovalChainNode::approverId).toList())
                .containsExactly(superiors.get(0), superiors.get(1), superiors.get(2));
        assertThat(nodes).extracting(ApprovalChainNode::approverId).contains(hr).doesNotContain(employee);

        approveAll(appeal.reviewId());
        StaffAdjustmentAppealRecord decided = service.listAdjustmentAppeals(employee).items().stream()
                .filter(a -> a.id().equals(appeal.id())).findFirst().orElseThrow();
        assertThat(decided.status()).isEqualTo("revoked");
        assertThat(decided.reversalAdjustmentId()).isNotNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_payroll_adjustments WHERE id = ?", Integer.class, adjustmentId))
                .as("原记录保留").isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT adj_type, amount_cent, reversal_of FROM staff_payroll_adjustments WHERE id = ?",
                decided.reversalAdjustmentId()))
                .containsEntry("adj_type", "penalty_reversal").containsEntry("amount_cent", 4000L).containsEntry("reversal_of", adjustmentId);
        actAs(initiator);
        var supplementary=PayrollApprovedCloseFixture.closeWithApprovedZeroTax(jdbc,json,payroll,employee,"2099-09","supplemental");
        assertThat(supplementary.statement().totalCent()).isEqualTo(4000L);
        assertThat(jdbc.queryForObject("SELECT penalty_cent FROM staff_payroll_statements WHERE id=?",Long.class,original.statement().id())).as("原扣罚工资保留，补差单独发放").isEqualTo(4000L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name = 'staff.payroll.adjustment_appeal_decided'"
                + " AND business_id = ? AND metadata->>'conclusion' = 'revoked'", Integer.class, appeal.id())).isEqualTo(1);

        // 维持：再申诉一笔已生效扣罚并驳回 → 结论 upheld，无冲回。
        actAs(initiator);
        SupervisorReviewDecisionRecord second = service.addPayrollAdjustment(
                new StaffPayrollAdjustmentRequest(key(), employee, "2099-10", "penalty", 1000L, "违规"));
        approveAll(second.reviewId());
        actAs(employee);
        StaffAdjustmentAppealRecord upheld = service.submitAdjustmentAppeal(second.objectId(),
                new StaffAdjustmentAppealRequest(key(), "不服"));
        actAs(superiors.get(0));
        reviews.approve(upheld.reviewId(), approval("rejected"));
        assertThat(service.listAdjustmentAppeals(employee).items().stream().filter(a -> a.id().equals(upheld.id())).findFirst()
                .orElseThrow().status()).isEqualTo("upheld");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_payroll_adjustments WHERE reversal_of = ?", Integer.class, second.objectId()))
                .isZero();
    }

    /** 申诉只能由被扣罚员工本人提出，且只针对已生效扣罚。 */
    @Test
    void appealIsOnlyForTheEmployeeAndOnlyOnPenalty() {
        SupervisorReviewDecisionRecord bonus = service.addPayrollAdjustment(
                new StaffPayrollAdjustmentRequest(key(), employee, "2099-11", "bonus", 1000L, "奖"));
        approveAll(bonus.reviewId());
        actAs(employee);
        assertThatThrownBy(() -> service.submitAdjustmentAppeal(bonus.objectId(), new StaffAdjustmentAppealRequest(key(), "不服")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("APPEAL_ONLY_FOR_PENALTY"));
        actAs(initiator);
        SupervisorReviewDecisionRecord penalty = service.addPayrollAdjustment(
                new StaffPayrollAdjustmentRequest(key(), employee, "2099-11", "penalty", 1000L, "罚"));
        approveAll(penalty.reviewId());
        actAs(superiors.get(0));
        assertThatThrownBy(() -> service.submitAdjustmentAppeal(penalty.objectId(), new StaffAdjustmentAppealRequest(key(), "代诉")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("APPEAL_SELF_ONLY"));
        actAs(employee);
        assertThatThrownBy(() -> service.submitAdjustmentAppeal(penalty.objectId(), new StaffAdjustmentAppealRequest(key(), " ")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void independentStatusChangeRejectsAnUnrelatedManagerEvenWithPermission() {
        assertThatThrownBy(() -> service.setEmploymentStatus(profileId,
                new StaffEmploymentStatusRequest(key(), "suspended", "停岗调查")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DIRECT_SUPERVISOR_REQUIRED"));
        assertThat(employmentStatus()).isEqualTo("employed");
    }

    @Test
    void employmentStopRevokesGrantedRolesEvenWithoutAHandOverRecipient() {
        actAs(superiors.get(0));
        var review = service.setEmploymentStatus(profileId,
                new StaffEmploymentStatusRequest(key(), "departed", "离职生效"));
        assertThat(access.findUserRoleIds(employee)).isNotEmpty();
        for (String approver : List.of(superiors.get(1), superiors.get(2), hr)) approve(review.reviewId(), approver);
        assertThat(employmentStatus()).isEqualTo("departed");
        assertThat(access.findUserRoleIds(employee)).as("没有接手人也必须撤销原员工全部业务角色").isEmpty();
    }

    @Test
    void employmentStopInvalidatesExistingBusinessSessions() {
        auth.provisionUser(employee, employee, "staff-test-password", "staff", "project_main", null, "active");
        var session = auth.login(new com.complex.platform.auth.AuthService.LoginRequest(employee, "staff-test-password", null, null, "web"));
        assertThat(auth.authenticateToken(session.accessToken())).isPresent();
        actAs(superiors.get(0));
        var review = service.setEmploymentStatus(profileId,
                new StaffEmploymentStatusRequest(key(), "suspended", "停岗生效"));
        for (String approver : List.of(superiors.get(1), superiors.get(2), hr)) approve(review.reviewId(), approver);
        assertThat(auth.authenticateToken(session.accessToken())).as("停岗后旧会话立即失效").isEmpty();
        assertThatThrownBy(() -> auth.refresh(
                new com.complex.platform.auth.AuthService.RefreshTokenRequest(session.refreshToken(), null), session.accessToken()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void employmentStopRejectsPasswordLoginAfterApproval() {
        auth.provisionUser(employee, employee, "staff-test-password", "staff", "project_main", null, "active");
        actAs(superiors.get(0));
        var review = service.setEmploymentStatus(profileId,
                new StaffEmploymentStatusRequest(key(), "departed", "离职生效"));
        for (String approver : List.of(superiors.get(1), superiors.get(2), hr)) approve(review.reviewId(), approver);
        assertThatThrownBy(() -> auth.login(
                new com.complex.platform.auth.AuthService.LoginRequest(employee, "staff-test-password", null, null, "web")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void futureApprovedStopBlocksAtTheDeadlineEvenBeforeTheJobMaterializesRevocation() {
        auth.provisionUser(employee, employee, "staff-test-password", "staff", "project_main", null, "active");
        var session = auth.login(new com.complex.platform.auth.AuthService.LoginRequest(employee, "staff-test-password", null, null, "web"));
        actAs(superiors.get(0));
        var review = service.setEmploymentStatus(profileId, new StaffEmploymentStatusRequest(key(), "suspended", "未来生效",
                java.time.Instant.now().plusSeconds(3600)));
        for (String approver : List.of(superiors.get(1), superiors.get(2), hr)) approve(review.reviewId(), approver);
        assertThat(employmentStatus()).isEqualTo("employed");
        assertThat(auth.authenticateToken(session.accessToken())).isPresent();
        assertThat(access.findUserRoleIds(employee)).isNotEmpty();
        jdbc.update("UPDATE staff_employment_status_revisions SET effective_at=now()-interval '1 second' WHERE review_id=?",review.reviewId());
        assertThat(employmentStatus()).as("模拟调度尚未落地").isEqualTo("employed");
        assertThat(auth.authenticateToken(session.accessToken())).isEmpty();
        assertThat(authorization.holdsPermissionViaRole(employee,"staff.profile.review")).isFalse();
        assertThatThrownBy(()->auth.login(new com.complex.platform.auth.AuthService.LoginRequest(employee,"staff-test-password",null,null,"web"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->access.assignUserRole(employee,"role_m010_fixture")).isInstanceOf(ApiException.class);
        actAs(hr);
        employmentJob.runOnce(java.time.Instant.now());
        assertThat(employmentStatus()).isEqualTo("suspended");
        assertThat(access.findUserRoleIds(employee)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT applied_at IS NOT NULL FROM staff_employment_status_revisions WHERE review_id=?",Boolean.class,review.reviewId())).isTrue();
    }

    @Test
    void stopRevokesFutureTemporaryAndTemplateAndIndividualGrantsWithoutRevivingThemOnReturn() {
        var now=java.time.Instant.now();
        String temporary=UUID.randomUUID().toString(),template="tpl_stop_"+suffix;
        jdbc.update("INSERT INTO permission_templates(id,template_name,template_code,updated_at) VALUES(?,'停权测试模板',?,now())",template,template);
        jdbc.update("INSERT INTO permission_template_items(template_id,permission_code,effect) VALUES(?,'staff.profile.view','grant')",template);
        var applied=access.applyTemplate(template,List.of(employee),now.plusSeconds(7200),now.plusSeconds(10800),false,initiator);
        access.createTemporaryGrant(new com.complex.platform.access.TemporaryGrant(temporary,employee,List.of("staff.profile.view"),
                now.plusSeconds(7200),now.plusSeconds(10800),false,false,"active","待生效",initiator,now,null,null));
        access.saveUserPermissionOverrides(employee,List.of("staff.profile.view"),AccessRepository.Effect.GRANT,"单独授予",initiator);
        access.saveUserPermissionOverrides(employee,List.of("staff.profile.submit"),AccessRepository.Effect.DENY,"明确拒绝保留",initiator);
        actAs(superiors.get(0));
        var review=service.setEmploymentStatus(profileId,new StaffEmploymentStatusRequest(key(),"suspended","停岗"));
        for(String approver:List.of(superiors.get(1),superiors.get(2),hr)) approve(review.reviewId(),approver);
        assertThat(jdbc.queryForObject("SELECT status FROM temporary_permission_grants WHERE grant_group_id=?",String.class,temporary)).isEqualTo("revoked");
        assertThat(jdbc.queryForObject("SELECT status FROM permission_template_applications WHERE id=?",String.class,applied.get(0).id())).isEqualTo("revoked");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_permission_overrides WHERE user_id=? AND effect='grant'",Integer.class,employee)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_permission_overrides WHERE user_id=? AND effect='deny'",Integer.class,employee)).isEqualTo(1);
        actAs(superiors.get(0));
        var returned=service.setEmploymentStatus(profileId,new StaffEmploymentStatusRequest(key(),"employed","恢复在职"));
        for(String approver:List.of(superiors.get(1),superiors.get(2),hr)) approve(returned.reviewId(),approver);
        assertThat(employmentStatus()).isEqualTo("employed");
        assertThat(access.findUserRoleIds(employee)).isEmpty();
        assertThat(access.findActiveTemplateApplicationIds(employee)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT status FROM temporary_permission_grants WHERE grant_group_id=?",String.class,temporary)).isEqualTo("revoked");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_permission_overrides WHERE user_id=? AND effect='grant'",Integer.class,employee)).isZero();
    }

    // ── helpers ──

    private void approveAll(String reviewId) {
        for (String approver : List.of(superiors.get(0), superiors.get(1), superiors.get(2), hr)) {
            approve(reviewId, approver);
        }
    }

    private void approve(String reviewId, String approver) {
        actAs(approver);
        reviews.approve(reviewId, approval("approved"));
    }

    private static SupervisorReviewApprovalRequest approval(String decision) {
        return new SupervisorReviewApprovalRequest(key(), "复核", List.of(), null, decision, "同意".equals(decision) ? "同意" : "已核");
    }

    private void actAs(String userId) {
        StaffTestActors.actor(userId, "staff", "project_main", null);
        StaffTestActors.alignRowToSession(jdbc);
    }

    private String employmentStatus() {
        return jdbc.queryForObject("SELECT employment_status FROM staff_profiles WHERE id = ?", String.class, profileId);
    }

    private int adjustmentCount(String type) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM staff_payroll_adjustments WHERE user_id = ? AND adj_type = ?",
                Integer.class, employee, type);
    }

    private Long statementPenalty(String month) {
        return jdbc.queryForObject("SELECT penalty_cent FROM staff_payroll_statements WHERE user_id = ? AND month = ?",
                Long.class, employee, month);
    }

    private static String key() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
