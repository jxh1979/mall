package com.complex.platform.leasing;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.contract.ContractRecord;
import com.complex.platform.contract.ContractRepository;
import com.complex.platform.contract.ContractService;
import com.complex.platform.contract.dto.ContractActivateRequest;
import com.complex.platform.contract.dto.ContractDraftRentTermsRequest;
import com.complex.platform.contract.dto.ContractSubmitRequest;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.leasing.dto.LeasingLeadStatusRequest;
import com.complex.platform.leasing.dto.LeasingSignRequest;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * CG-01 招商线索「已签约」改到合同生效时（PGM「招商运营细则」：「招商线索在已签约前，可从待跟进、跟进中、已转意向或洽谈中转为
 * 已流失或已搁置；转换时必须填写原因，合同审批全部通过、生效后转为已签约，已签约后不得回退至未签约状态」）。
 *
 * <p>走真实 Flyway 迁移库（V442 leasing_intents.contract_id），{@code @Transactional} 每用例回滚；审计事件 REQUIRES_NEW 独立提交
 * （audit_events 只增不删）。全链：signIntent 起草 → submit（HP-C01：开统一审批复核单）→ 主线+三条平行线节点逐个裁决 → activate，
 * 逐步追 {@code leasing_leads.status} 列。第四轮裁决第 4 条「签约起草后在草稿上补录租金条款再提交」：意向条款无月租，提交前先补录月租。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class LeasingLeadSignedOnContractEffectiveJdbcIntegrationTests {

    /** 平台默认「新建」模板的三条平行线（V494）。 */
    private static final List<String> LINE_CODES = List.of("business_review", "finance_review", "legal_review");

    @Autowired private LeasingService leasingService;
    @Autowired private ContractService contractService;
    @Autowired private ContractRepository contractRepository;
    @Autowired private SupervisorReviewService supervisorReviewService;
    @Autowired private JdbcLeasingRepository leasingRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String suffix;
    private String projectId;
    private String subject;
    private String spaceId;
    private String leadId;
    private String intentId;
    /** 起草人的直接上级：主线与三条平行线的最终审批人（org_hierarchy）。 */
    private String superiorId;

    @BeforeEach
    void seed() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        projectId = "cg01_project_" + suffix;
        subject = "be_cg01_" + suffix;
        spaceId = "sp_cg01_" + suffix;
        leadId = "lead_cg01_" + suffix;
        intentId = "intent_cg01_" + suffix;
        jdbcTemplate.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) "
                + "VALUES (?, 'complex', '签约项目', 'active', now(), now())", projectId);
        jdbcTemplate.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) "
                + "VALUES (?, ?, 'management_company', 'active', now(), now())", subject, "签约测试合同主体");
        jdbcTemplate.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES (?, ?)", subject, projectId);
        jdbcTemplate.update("INSERT INTO spaces (id, project_id, merchant_id, parent_space_id, space_name, "
                + "space_type, floor_no, area_square_meter, status, created_at, updated_at) "
                + "VALUES (?, ?, NULL, NULL, ?, 'shop', '1F', 88.0, 'active', now(), now())",
                spaceId, projectId, "签约测试铺位");
        Instant now = Instant.now();
        leasingRepository.saveLead(new LeasingLeadRecord(leadId, "签约测试联系人", "13800002222", "referral",
                "retail", spaceId, null, null, "usr_sales", "negotiating", null, null, projectId, now, now));
        leasingRepository.saveIntent(new LeasingIntentRecord(intentId, leadId, "pending_signing", spaceId,
                null, Map.of(), now, "usr_supervisor", projectId, now, now));
        authenticateAsPlatformAdminHoldingContractPermissions();
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    /** 签约只起草合同草稿：线索保持洽谈中；合同绑到来源意向（V442 列）。 */
    @Test
    void signIntentDraftsContractAndLeavesLeadStatusUntouched() {
        ContractRecord contract = sign();

        assertThat(contract.lifecycleStatus()).isEqualTo("drafting");
        assertThat(leadStatusColumn()).isEqualTo("negotiating");
        assertThat(jdbcTemplate.queryForObject("SELECT contract_id FROM leasing_intents WHERE id = ?",
                String.class, intentId)).isEqualTo(contract.id());
    }

    /** 全链：提交、审批全部通过仍非已签约；合同生效那一刻线索列变 signed，并留痕 leasing.lead.signed。 */
    @Test
    void leadBecomesSignedOnlyWhenContractActivates() {
        ContractRecord contract = sign();
        ContractRecord submitted = submitForReview(contract);
        assertThat(leadStatusColumn()).isEqualTo("negotiating");
        assertThat(jdbcTemplate.queryForObject("SELECT review_id FROM contracts WHERE id = ?", String.class, contract.id()))
                .isEqualTo(submitted.reviewId());

        decideAllNodes(submitted.reviewId(), "approved");
        assertThat(jdbcTemplate.queryForObject("SELECT lifecycle_status FROM contracts WHERE id = ?",
                String.class, contract.id())).isEqualTo("pending_effective");
        // 审批全部通过但尚未生效：仍不是已签约。
        assertThat(leadStatusColumn()).isEqualTo("negotiating");

        ContractRecord effective = contractService.activate(contract.id(),
                new ContractActivateRequest("activate_" + suffix, List.of(spaceId)));
        assertThat(effective.lifecycleStatus()).isEqualTo("effective");
        assertThat(leadStatusColumn()).isEqualTo("signed");
        Integer signedEvents = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_name = 'leasing.lead.signed' AND business_id = ?",
                Integer.class, leadId);
        assertThat(signedEvents).isEqualTo(1);
    }

    /** 审批驳回退回起草：线索不得变已签约。 */
    @Test
    void rejectedApprovalKeepsLeadUnsigned() {
        ContractRecord contract = sign();
        decideAllNodes(submitForReview(contract).reviewId(), "rejected");   // 任一节点驳回 → 整单退回起草

        assertThat(jdbcTemplate.queryForObject("SELECT lifecycle_status FROM contracts WHERE id = ?",
                String.class, contract.id())).isEqualTo("drafting");
        assertThat(leadStatusColumn()).isEqualTo("negotiating");
    }

    /** 已签约后不得回退至未签约状态：生效后改线索为任一其他状态 → 409，列不变。 */
    @Test
    void signedLeadRejectsAnyRollback() {
        ContractRecord contract = sign();
        decideAllNodes(submitForReview(contract).reviewId(), "approved");
        contractService.activate(contract.id(), new ContractActivateRequest("activate_" + suffix, List.of(spaceId)));
        assertThat(leadStatusColumn()).isEqualTo("signed");

        assertThatThrownBy(() -> leasingService.createIntent(new com.complex.platform.leasing.dto.LeasingIntentRequest(
                leadId, spaceId, new java.math.BigDecimal("88"), Map.of(), "intent-again-" + suffix)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("LEASING_LEAD_ALREADY_SIGNED"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM leasing_intents WHERE lead_id=?", Integer.class, leadId)).isEqualTo(1);

        for (String target : List.of("pending_follow_up", "following_up", "converted_to_intent", "negotiating",
                "lost", "on_hold")) {
            assertThatThrownBy(() -> leasingService.updateLeadStatus(leadId,
                    new LeasingLeadStatusRequest(target, "试图回退", "rb_" + target + suffix)))
                    .isInstanceOfSatisfying(ApiException.class,
                            e -> assertThat(e.code()).isEqualTo("LEASING_LEAD_ALREADY_SIGNED"));
            assertThat(leadStatusColumn()).isEqualTo("signed");
        }
    }

    @Test
    void manualSignedStatusCannotReplaceEffectiveContract() {
        assertThatThrownBy(() -> leasingService.updateLeadStatus(leadId,
                new LeasingLeadStatusRequest("signed", "人工标记", "manual-" + suffix)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("LEASING_SIGNED_REQUIRES_EFFECTIVE_CONTRACT"));
        assertThat(leadStatusColumn()).isEqualTo("negotiating");
    }

    @Test
    void otherProjectCannotMutateLeadOrCreateItsFollowUp() {
        String otherProject = "other_" + suffix, user = "other_user_" + suffix, role = "other_role_" + suffix;
        jdbcTemplate.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','其他项目','active',now(),now())", otherProject);
        jdbcTemplate.update("INSERT INTO access_users(id,username,account_type,organization_id,status,created_at) VALUES(?,?,'staff',?,'active',now())", user,user,otherProject);
        jdbcTemplate.update("INSERT INTO roles(id,role_name,created_at) VALUES(?,?,now())",role,role);
        for (String permission : List.of("leasing.lead.manage","leasing.intent.manage")) {
            jdbcTemplate.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)",role,permission);
        }
        jdbcTemplate.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)",user,role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(user,user,"staff",otherProject,null,"active"),"token",List.of()));
        List<Runnable> writes = List.of(
                () -> leasingService.assignLead(leadId,new com.complex.platform.leasing.dto.LeasingLeadAssignRequest(user,"assign-other-"+suffix)),
                () -> leasingService.updateLeadStatus(leadId,new LeasingLeadStatusRequest("lost","停止跟进","status-other-"+suffix)),
                () -> leasingService.createIntent(new com.complex.platform.leasing.dto.LeasingIntentRequest(leadId,spaceId,null,Map.of(),"intent-other-"+suffix)),
                () -> leasingService.createFollowUp(new com.complex.platform.leasing.dto.LeasingFollowUpRequest("lead",leadId,"phone","其他项目跟进",null,"follow-other-"+suffix)));
        for (Runnable write : writes) assertThatThrownBy(write::run)
                .isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        assertThat(leadStatusColumn()).isEqualTo("negotiating");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM leasing_intents WHERE lead_id=?",Integer.class,leadId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM leasing_follow_ups WHERE ref_id=?",Integer.class,leadId)).isZero();
    }

    /**
     * 第四轮裁决第 4 条「招商签约合同没有月租」→「合同草稿可编辑补录」「非纯提成合同月租为 0 的不得提交审批」：签约起草带意向条款（无月租）→
     * 提交 400「请先在草稿上补录月租」、合同仍草拟中；按 web 真实请求形状（adminClient.mapKeys 把 rent_terms 嵌套键蛇形化）补录月租 →
     * 库内 rent_terms 归一为驼峰、押金＝收取月数×月租（BSM L1347）→ 再提交进待确认。
     */
    @Test
    void signedDraftWithoutRentIsRejectedUntilRentFilledOnDraftThenSubmits() {
        ContractRecord contract = sign();
        assertThatThrownBy(() -> submitOnly(contract))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("CONTRACT_RENT_REQUIRED");
                    assertThat(e.getMessage()).isEqualTo("请先在草稿上补录月租");
                });
        assertThat(jdbcTemplate.queryForObject("SELECT lifecycle_status FROM contracts WHERE id = ?",
                String.class, contract.id())).isEqualTo("drafting");

        ContractRecord edited = contractService.editDraftRentTerms(contract.id(), new ContractDraftRentTermsRequest(
                "rent_" + suffix, Map.of("rent_mode", "monthly_fixed", "rent_billing_mode", "fixed_only", "base_rent_cent", 150000),
                null));
        long expectedDeposit = contractRepository.depositMonthsSetting().value() * 150000L;
        assertThat(edited.depositCent()).isEqualTo(expectedDeposit);
        assertThat(jdbcTemplate.queryForObject("SELECT deposit_cent FROM contracts WHERE id = ?", Long.class, contract.id()))
                .isEqualTo(expectedDeposit);
        assertThat(jdbcTemplate.queryForObject("SELECT rent_terms->>'baseRentCent' FROM contracts WHERE id = ?",
                String.class, contract.id())).isEqualTo("150000");
        assertThat(jdbcTemplate.queryForObject("SELECT jsonb_exists(rent_terms, 'base_rent_cent') FROM contracts WHERE id = ?",
                Boolean.class, contract.id())).isFalse();

        assertThat(submitOnly(contract).lifecycleStatus()).isEqualTo("pending_confirmation");
    }

    /** 草稿编辑沿用起草合同的权限码 contract.draft.manage 与项目范围：持该码的别的综合体项目员工 → 403 CROSS_PROJECT_FORBIDDEN，条款不变。 */
    @Test
    void otherProjectStaffCannotEditSignedDraftRentTerms() {
        ContractRecord contract = sign();
        String otherProject = "other_" + suffix, user = "other_user_" + suffix, role = "other_role_" + suffix;
        jdbcTemplate.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','其他项目','active',now(),now())", otherProject);
        jdbcTemplate.update("INSERT INTO access_users(id,username,account_type,organization_id,status,created_at) VALUES(?,?,'staff',?,'active',now())", user, user, otherProject);
        jdbcTemplate.update("INSERT INTO roles(id,role_name,created_at) VALUES(?,?,now())", role, role);
        jdbcTemplate.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'contract.draft.manage')", role);
        jdbcTemplate.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user, role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(user, user, "staff", otherProject, null, "active"), "token", List.of()));

        assertThatThrownBy(() -> contractService.editDraftRentTerms(contract.id(), new ContractDraftRentTermsRequest(
                "rent_other_" + suffix, Map.of("base_rent_cent", 150000), null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbcTemplate.queryForObject("SELECT rent_terms->>'baseRentCent' FROM contracts WHERE id = ?",
                String.class, contract.id())).isNull();
    }

    // ---- helpers ----

    private ContractRecord sign() {
        return leasingService.signIntent(intentId, new LeasingSignRequest(
                null, subject, null, null,
                LocalDate.parse("2026-08-01"), LocalDate.parse("2027-07-31"), "sign_" + suffix));
    }

    /** 第四轮裁决第 4 条：签约草稿先补录月租（web 蛇形键），再按 HP-C01 提交。 */
    private ContractRecord submitForReview(ContractRecord contract) {
        contractService.editDraftRentTerms(contract.id(), new ContractDraftRentTermsRequest("rent_" + suffix,
                Map.of("rent_mode", "monthly_fixed", "rent_billing_mode", "fixed_only", "base_rent_cent", 100000), null));
        return submitOnly(contract);
    }

    /** HP-C01：提交即按「新建」模板开复核单——主线最终审批人与三条平行线最终审批人都指定为起草人的直接上级。 */
    private ContractRecord submitOnly(ContractRecord contract) {
        return contractService.submit(contract.id(), new ContractSubmitRequest("submit_" + suffix, superiorId,
                LINE_CODES.stream().map(code -> new ParallelLine(code, superiorId)).toList(), "招商签约提交"));
    }

    /**
     * 以直接上级身份在统一审批入口逐节点裁决（主线 1 节点 + 三条平行线各 1 节点）：approved 要四个节点全过复核单才 approved
     * （BSM L1349「主线与各条平行线都通过才生效」）；rejected 第一个节点驳回即整单 rejected。裁决完切回起草人会话。
     */
    private void decideAllNodes(String reviewId, String decision) {
        assertThat(reviewId).isNotNull();
        var drafter = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(superiorId, "u_cg01_sup_" + suffix, "platform_admin", "org_1", null, "active"), "token", List.of()));
        try {
            int nodes = "approved".equals(decision) ? 1 + LINE_CODES.size() : 1;
            for (int i = 0; i < nodes; i++) {
                supervisorReviewService.approve(reviewId, new SupervisorReviewApprovalRequest("decide_" + i + "_" + suffix,
                        null, List.of(), null, decision, "approved".equals(decision) ? "同意" : "条款不合规"));
            }
        } finally {
            SecurityContextHolder.getContext().setAuthentication(drafter);
        }
        assertThat(jdbcTemplate.queryForObject("SELECT review_status FROM supervisor_review_decisions WHERE review_id = ?",
                String.class, reviewId)).isEqualTo(decision);
    }

    private String leadStatusColumn() {
        return jdbcTemplate.queryForObject("SELECT status FROM leasing_leads WHERE id = ?", String.class, leadId);
    }

    /**
     * 生效走 requirePermission：contract.lifecycle.activate 为高危码，role_platform_admin 的播种条目不算逐项授权，须给操作人一个
     * 显式角色来源（真实 access_users / roles / role_permissions 行）。HP-C01：另种起草人的直接上级（org_hierarchy），作主线与三条
     * 平行线的最终审批人。
     */
    private void authenticateAsPlatformAdminHoldingContractPermissions() {
        String userId = "usr_cg01_" + suffix;
        String roleId = "role_cg01_" + suffix;
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", userId, "u_cg01_" + suffix);
        jdbcTemplate.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId, "cg01-" + suffix);
        jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'contract.lifecycle.activate')", roleId);
        jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'contract.draft.manage')", roleId);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        superiorId = "usr_cg01_sup_" + suffix;
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", superiorId, "u_cg01_sup_" + suffix);
        jdbcTemplate.update("INSERT INTO org_hierarchy (user_id, superior_id) VALUES (?, ?)", userId, superiorId);
        AccessActor actor = new AccessActor(userId, "u_cg01_" + suffix, "platform_admin", "org_1", null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }
}
