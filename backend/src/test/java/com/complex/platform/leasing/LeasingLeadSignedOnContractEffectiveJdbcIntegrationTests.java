package com.complex.platform.leasing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.contract.ContractRecord;
import com.complex.platform.contract.ContractService;
import com.complex.platform.contract.dto.ContractActivateRequest;
import com.complex.platform.contract.dto.ContractApproveRequest;
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
 * （audit_events 只增不删）。全链：signIntent 起草 → submit → 四槽审批 → activate，逐步追 {@code leasing_leads.status} 列。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class LeasingLeadSignedOnContractEffectiveJdbcIntegrationTests {

    private static final List<String> APPROVER_CODES = List.of("contract.approve.business", "contract.approve.finance",
            "contract.approve.legal", "contract.approve.gm");

    @Autowired private LeasingService leasingService;
    @Autowired private ContractService contractService;
    @Autowired private JdbcLeasingRepository leasingRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String suffix;
    private String projectId;
    private String subject;
    private String spaceId;
    private String leadId;
    private String intentId;

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
        contractService.submit(contract.id(), "submit_" + suffix);
        assertThat(leadStatusColumn()).isEqualTo("negotiating");

        for (String code : APPROVER_CODES) {
            contractService.approve(contract.id(), new ContractApproveRequest("appr_" + code + suffix, code, "approve", "同意"));
        }
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
        contractService.submit(contract.id(), "submit_" + suffix);
        ContractRecord back = contractService.approve(contract.id(),
                new ContractApproveRequest("reject_" + suffix, "contract.approve.finance", "reject", "条款不合规"));

        assertThat(back.lifecycleStatus()).isEqualTo("drafting");
        assertThat(leadStatusColumn()).isEqualTo("negotiating");
    }

    /** 已签约后不得回退至未签约状态：生效后改线索为任一其他状态 → 409，列不变。 */
    @Test
    void signedLeadRejectsAnyRollback() {
        ContractRecord contract = sign();
        contractService.submit(contract.id(), "submit_" + suffix);
        for (String code : APPROVER_CODES) {
            contractService.approve(contract.id(), new ContractApproveRequest("appr_" + code + suffix, code, "approve", "同意"));
        }
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

    // ---- helpers ----

    private ContractRecord sign() {
        return leasingService.signIntent(intentId, new LeasingSignRequest(
                null, subject, null, null,
                LocalDate.parse("2026-08-01"), LocalDate.parse("2027-07-31"), "sign_" + suffix));
    }

    private String leadStatusColumn() {
        return jdbcTemplate.queryForObject("SELECT status FROM leasing_leads WHERE id = ?", String.class, leadId);
    }

    /**
     * 审批槽与生效都走 requirePermission：contract.approve.gm / contract.lifecycle.activate 为高危码，
     * role_platform_admin 的播种条目不算逐项授权，须给操作人一个显式角色来源（真实 access_users / roles / role_permissions 行）。
     */
    private void authenticateAsPlatformAdminHoldingContractPermissions() {
        String userId = "usr_cg01_" + suffix;
        String roleId = "role_cg01_" + suffix;
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", userId, "u_cg01_" + suffix);
        jdbcTemplate.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId, "cg01-" + suffix);
        for (String code : APPROVER_CODES) {
            jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'contract.lifecycle.activate')", roleId);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        AccessActor actor = new AccessActor(userId, "u_cg01_" + suffix, "platform_admin", "org_1", null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }
}
