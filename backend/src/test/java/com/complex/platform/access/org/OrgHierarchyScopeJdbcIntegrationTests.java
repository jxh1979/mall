package com.complex.platform.access.org;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.RoleRecord;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.governance.review.SupervisorReviewRepository;
import java.time.Instant;
import java.util.List;
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

@SpringBootTest(properties = {
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class OrgHierarchyScopeJdbcIntegrationTests {
    @Autowired OrgHierarchyService service;
    @Autowired AccessRepository access;
    @Autowired AuthorizationService authorization;
    @Autowired SupervisorReviewRepository reviews;
    @Autowired JdbcTemplate jdbc;
    String suffix, projectA, projectB, manager, employeeA, employeeB, hrA, hrB, leaderA, leaderB, deptA, deptB;

    @BeforeEach void seed() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        projectA = "scope_a_" + suffix;
        projectB = "scope_b_" + suffix;
        for (String p : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES (?,'complex',?,'active',now(),now())", p, p);
        }
        manager = user("manager", projectA);
        employeeA = user("employee_a", projectA); employeeB = user("employee_b", projectB);
        hrA = user("hr_a", projectA); hrB = user("hr_b", projectB);
        leaderA = user("leader_a", projectA); leaderB = user("leader_b", projectB);
        role("manage", manager, List.of("access.role.assign"), List.of());
        role("hr_a", hrA, List.of("staff.hr.review"), List.of());
        role("hr_b", hrB, List.of("staff.hr.review"), List.of());
        deptA = "dept_a_" + suffix; deptB = "dept_b_" + suffix;
        reviews.saveApprovalLine(new SupervisorReviewRepository.ApprovalLine(deptA, "甲部门", 1, leaderA, "active", hrA), manager, Instant.now());
        reviews.saveApprovalLine(new SupervisorReviewRepository.ApprovalLine(deptB, "乙部门", 1, leaderB, "active", hrB), manager, Instant.now());
        reviews.saveDirectSuperior(employeeA, leaderA, deptA, manager, Instant.now());
        reviews.saveDirectSuperior(employeeB, leaderB, deptB, manager, Instant.now());
        act(manager, projectA);
        authorization.requirePermission("access.role.assign");
        assertThat(authorization.canReachUser(authorization.currentActor(), access.findUserById(employeeB).orElseThrow(), "access.role.assign")).isFalse();
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void ownProjectManagerCannotClearOrReplaceOtherDepartmentsHr() {
        var original = reviews.findApprovalLine(deptB).orElseThrow();
        for (String replacement : new String[] {null, hrA}) {
            assertScopeDenied(() -> service.upsertApprovalLine(deptB, request(replacement, null)));
            assertThat(reviews.findApprovalLine(deptB)).contains(original);
        }
    }

    @Test void everyBoundEmployeeIsCheckedEvenWhenOldHrAndEntryAreInReach() {
        reviews.saveApprovalLine(new SupervisorReviewRepository.ApprovalLine(deptB, "乙部门", 1, leaderA, "active", hrA), manager, Instant.now());
        var original = reviews.findApprovalLine(deptB).orElseThrow();
        assertScopeDenied(() -> service.upsertApprovalLine(deptB, request(null, null)));
        assertThat(reviews.findApprovalLine(deptB)).contains(original);
        assertThat(service.listApprovalLines()).extracting(SupervisorReviewRepository.ApprovalLine::lineCode).doesNotContain(deptB);
    }

    @Test void oldHrAndEntryStillProtectAnUnboundDepartmentFromTakeover() {
        jdbc.update("DELETE FROM org_hierarchy WHERE user_id=?", employeeB);
        var original = reviews.findApprovalLine(deptB).orElseThrow();
        assertScopeDenied(() -> service.upsertApprovalLine(deptB, request(hrA, leaderA)));
        assertThat(reviews.findApprovalLine(deptB)).contains(original);
    }

    @Test void newEntryMustAlsoBeWithinTheAuthorizedScope() {
        assertScopeDenied(() -> service.upsertApprovalLine(deptA, request(hrA, leaderB)));
        assertThat(reviews.findApprovalLine(deptA).orElseThrow().entryUserId()).isEqualTo(leaderA);
    }

    @Test void sameProjectManagerCanClearAndRestoreDepartmentHr() {
        assertThat(service.upsertApprovalLine(deptA, request(null, leaderA)).hrFinalApproverId()).isNull();
        assertThat(service.upsertApprovalLine(deptA, request(hrA, leaderA)).hrFinalApproverId()).isEqualTo(hrA);
    }

    @Test void explicitlyAuthorizedCrossProjectRoleCanManageCoveredDepartment() {
        role("cross", manager, List.of("access.role.assign"), List.of(projectB));
        assertThat(authorization.canReachUser(authorization.currentActor(), access.findUserById(employeeB).orElseThrow(), "access.role.assign")).isTrue();
        assertThat(service.upsertApprovalLine(deptB, request(hrB, leaderB)).hrFinalApproverId()).isEqualTo(hrB);
        assertThat(service.setDirectSuperior(employeeB, new OrgSuperiorRequest(key(), "合法跨项目维护", leaderB, deptB)).lineCode()).isEqualTo(deptB);
    }

    @Test void cannotMoveOtherProjectsEmployeeToOwnDepartment() {
        assertScopeDenied(() -> service.setDirectSuperior(employeeB, new OrgSuperiorRequest(key(), "改部门", leaderA, deptA)));
        assertThat(jdbc.queryForObject("SELECT line_code FROM org_hierarchy WHERE user_id=?", String.class, employeeB)).isEqualTo(deptB);
        assertThat(reviews.findDirectSuperior(employeeB)).contains(leaderB);
    }

    @Test void scopedListsDoNotExposeOtherProjectsConfiguration() {
        assertThat(service.listApprovalLines()).extracting(SupervisorReviewRepository.ApprovalLine::lineCode).contains(deptA).doesNotContain(deptB);
        assertThat(service.listHierarchy(2000)).extracting(SupervisorReviewRepository.OrgSuperior::userId).contains(employeeA).doesNotContain(employeeB);
    }

    @Test void serviceEntryRequiresTheManagementPermissionEvenForOwnProject() {
        act(employeeA, projectA);
        assertThatThrownBy(() -> service.upsertApprovalLine(deptA, request(null, leaderA)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
        assertThatThrownBy(service::listApprovalLines)
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
        assertThatThrownBy(() -> service.setDirectSuperior(employeeA, new OrgSuperiorRequest(key(), "无授权", leaderA, deptA)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
        assertThatThrownBy(() -> service.listHierarchy(10))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
    }

    @Test void cannotAttachOwnEmployeeToOtherProjectsDepartment() {
        assertScopeDenied(() -> service.setDirectSuperior(employeeA, new OrgSuperiorRequest(key(), "改审批归属", leaderA, deptB)));
        assertThat(jdbc.queryForObject("SELECT line_code FROM org_hierarchy WHERE user_id=?", String.class, employeeA)).isEqualTo(deptA);
    }

    @Test void emptyExistingDepartmentCannotBeClaimedBySupplyingOwnHr() {
        String empty = "unowned_" + suffix;
        reviews.saveApprovalLine(new SupervisorReviewRepository.ApprovalLine(empty, "未归属线", 1, null, "active", null), manager, Instant.now());
        assertScopeDenied(() -> service.upsertApprovalLine(empty, request(hrA, leaderA)));
        assertThat(reviews.findApprovalLine(empty).orElseThrow().hrFinalApproverId()).isNull();
        assertThat(service.listApprovalLines()).extracting(SupervisorReviewRepository.ApprovalLine::lineCode).doesNotContain(empty);
    }

    @Test void allProjectsRoleCanManageAnUnboundGlobalDepartment() {
        String empty = "global_" + suffix;
        reviews.saveApprovalLine(new SupervisorReviewRepository.ApprovalLine(empty, "全局线", 1, null, "active", null), manager, Instant.now());
        String role = "role_all_" + suffix;
        access.createRole(new RoleRecord(role, role, null, "project", "active", Instant.now(),
                List.of("access.role.assign"), null, true, List.of()));
        access.assignUserRole(manager, role);
        assertThat(authorization.projectReach(authorization.currentActor(), "access.role.assign").allProjects()).isTrue();
        assertThat(service.upsertApprovalLine(empty, request(hrA, leaderA)).hrFinalApproverId()).isEqualTo(hrA);
    }

    @Test void newDepartmentRequiresAnInScopeAnchorOrAllProjectsRole() {
        String own = "new_own_" + suffix;
        assertThat(service.upsertApprovalLine(own, request(hrA, leaderA)).hrFinalApproverId()).isEqualTo(hrA);
        String empty = "new_unowned_" + suffix;
        assertScopeDenied(() -> service.upsertApprovalLine(empty, request(null, null)));
        assertThat(reviews.findApprovalLine(empty)).isEmpty();
    }

    @Test void newHrMustBeWithinScopeEvenWhenDepartmentIsOwned() {
        var original = reviews.findApprovalLine(deptA).orElseThrow();
        assertScopeDenied(() -> service.upsertApprovalLine(deptA, request(hrB, leaderA)));
        assertThat(reviews.findApprovalLine(deptA)).contains(original);
    }

    @Test void oldHrAndOldEntryIndependentlyAnchorDepartmentScope() {
        jdbc.update("DELETE FROM org_hierarchy WHERE user_id=?", employeeB);
        for (var original : List.of(
                new SupervisorReviewRepository.ApprovalLine(deptB, "乙部门", 1, leaderA, "active", hrB),
                new SupervisorReviewRepository.ApprovalLine(deptB, "乙部门", 1, leaderB, "active", hrA))) {
            reviews.saveApprovalLine(original, manager, Instant.now());
            assertScopeDenied(() -> service.upsertApprovalLine(deptB, request(hrA, leaderA)));
            assertThat(reviews.findApprovalLine(deptB)).contains(original);
        }
    }

    @Test void replayRechecksCurrentDepartmentScopeBeforeReturningSavedResult() {
        var change = request(hrA, leaderA);
        service.upsertApprovalLine(deptA, change);
        jdbc.update("UPDATE access_users SET organization_id=? WHERE id=?", projectB, employeeA);
        assertScopeDenied(() -> service.upsertApprovalLine(deptA, change));
    }

    private ApprovalLineRequest request(String hr, String entry) {
        return new ApprovalLineRequest(key(), "范围复核", "更新部门", 1, entry, "active", hr);
    }
    private void assertScopeDenied(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN"));
    }
    private String user(String tag, String project) {
        String id = tag + "_" + suffix;
        access.createUser(new AccessUser(id, id, "staff", project, null, "active", Instant.now()));
        return id;
    }
    private void role(String tag, String user, List<String> permissions, List<String> projects) {
        String id = "role_scope_" + tag + "_" + suffix;
        access.createRole(new RoleRecord(id, id, null, "project", "active", Instant.now(), permissions, null, false, projects));
        access.assignUserRole(user, id);
    }
    private void act(String user, String project) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(user, user, "staff", project, null, "active"), "test", List.of()));
    }
    private String key() { return UUID.randomUUID().toString(); }
}
