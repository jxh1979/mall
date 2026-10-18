package com.complex.platform.access.org;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewRepository;
import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

// M087-A-01 / A-03（PGM-1.7.1 组织层级「直接上级」与平行审批线「最低起点层级」）：维护入口规则。
// 文件版本：V0.01
class OrgHierarchyServiceTests {

    @Test
    void departmentHrConfigurationRequiresActiveRoleAndAuthorizedScope() {
        var access = Mockito.mock(AccessRepository.class);
        var auth = Mockito.mock(AuthorizationService.class);
        var actor = new com.complex.platform.access.AccessActor("manager", "manager", "staff", "project", null, "active");
        var hr = new AccessUser("hr", "hr", "staff", "project", null, "active", Instant.EPOCH);
        Mockito.when(auth.currentActor()).thenReturn(actor);
        var reach = AuthorizationService.ProjectReach.ownOnly("project");
        Mockito.when(auth.projectReach(actor, OrgHierarchyService.MANAGE_PERMISSION)).thenReturn(reach);
        Mockito.when(access.findUserById("hr")).thenReturn(Optional.of(hr));
        var target = new OrgHierarchyService(repository, access, auth,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()), new AuditEventService(audits));
        assertThatThrownBy(() -> target.upsertApprovalLine("department",
                new ApprovalLineRequest("hr-1", "分配人力负责人", "部门", 1, null, "active", "hr")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("HR_APPROVER_LACKS_ROLE"));
        Mockito.when(auth.holdsPermissionViaRole("hr", "staff.hr.review")).thenReturn(true);
        assertThatThrownBy(() -> target.upsertApprovalLine("department",
                new ApprovalLineRequest("hr-2", "分配人力负责人", "部门", 1, null, "active", "hr")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_ORG_FORBIDDEN"));
        Mockito.when(auth.canReachUser(reach, hr)).thenReturn(true);
        assertThat(target.upsertApprovalLine("department",
                new ApprovalLineRequest("hr-3", "分配人力负责人", "部门", 1, null, "active", "hr")).hrFinalApproverId()).isEqualTo("hr");
    }


    private final HierarchyRepository repository = new HierarchyRepository();
    private final InMemoryAuditEventRepository audits = new InMemoryAuditEventRepository();
    private OrgHierarchyService service;

    @BeforeEach
    void setUp() {
        AccessRepository accessRepository = Mockito.mock(AccessRepository.class);
        Set<String> known = Set.of("usr_a", "usr_b", "usr_c", "usr_entry");
        Mockito.when(accessRepository.findUserById(ArgumentMatchers.anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return known.contains(id)
                    ? Optional.of(new AccessUser(id, id, "staff", "org_1", null, "active", Instant.parse("2026-06-14T00:00:00Z")))
                    : Optional.empty();
        });
        var auth = Mockito.mock(AuthorizationService.class);
        var actor = new com.complex.platform.access.AccessActor("manager", "manager", "staff", "org_1", null, "active");
        var reach = AuthorizationService.ProjectReach.ownOnly("org_1");
        Mockito.when(auth.currentActor()).thenReturn(actor);
        Mockito.when(auth.projectReach(actor, OrgHierarchyService.MANAGE_PERMISSION)).thenReturn(reach);
        Mockito.when(auth.canReachUser(ArgumentMatchers.eq(reach), ArgumentMatchers.any(AccessUser.class)))
                .thenAnswer(invocation -> "org_1".equals(((AccessUser) invocation.getArgument(1)).organizationId()));
        service = new OrgHierarchyService(repository, accessRepository, auth,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()), new AuditEventService(audits));
        TestSecurityActors.asSupervisorLevel(3);
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    @Test
    void setsAndListsDirectSuperiorsWithAudit() {
        var saved = service.setDirectSuperior("usr_a", new OrgSuperiorRequest("h-1", null, "usr_b", null));
        assertThat(saved.superiorId()).isEqualTo("usr_b");
        assertThat(repository.superiors).containsEntry("usr_a", "usr_b");
        assertThat(service.listHierarchy(10)).extracting(SupervisorReviewRepository.OrgSuperior::userId).containsExactly("usr_a");
        assertThat(audits.events).extracting(AuditEvent::eventName).contains("access.org_hierarchy.superior_set");

        service.setDirectSuperior("usr_a", new OrgSuperiorRequest("h-2", null, null, null)); // 清除
        assertThat(repository.superiors.get("usr_a")).isNull();
    }

    @Test
    void rejectsSelfUnknownSuperiorAndCycles() {
        assertThatThrownBy(() -> service.setDirectSuperior("usr_a", new OrgSuperiorRequest("h-self", null, "usr_a", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ORG_HIERARCHY_SELF"));
        assertThatThrownBy(() -> service.setDirectSuperior("usr_a", new OrgSuperiorRequest("h-unknown", null, "usr_ghost", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("USER_NOT_FOUND"));
        service.setDirectSuperior("usr_a", new OrgSuperiorRequest("h-ab", null, "usr_b", null));
        service.setDirectSuperior("usr_b", new OrgSuperiorRequest("h-bc", null, "usr_c", null));
        assertThatThrownBy(() -> service.setDirectSuperior("usr_c", new OrgSuperiorRequest("h-cycle", null, "usr_a", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ORG_HIERARCHY_CYCLE"));
        assertThat(repository.superiors.get("usr_c")).isNull();
    }

    @Test
    void approvalLineRequiresRankWithinBoundsAndKnownEntryPerson() {
        var line = service.upsertApprovalLine("finance_review", new ApprovalLineRequest("l-1", null, "财务审核线", 6, "usr_entry", "active"));
        assertThat(line.minStartRank()).isEqualTo(6);
        assertThat(service.listApprovalLines()).extracting(SupervisorReviewRepository.ApprovalLine::lineCode).containsExactly("finance_review");
        assertThatThrownBy(() -> service.upsertApprovalLine("legal_review", new ApprovalLineRequest("l-2", null, "法务线", 31, null, "active")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> service.upsertApprovalLine("legal_review", new ApprovalLineRequest("l-3", null, "法务线", 1, "usr_ghost", "active")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("USER_NOT_FOUND"));
        assertThatThrownBy(() -> service.setDirectSuperior("usr_a", new OrgSuperiorRequest("h-line", null, "usr_b", "no_such_line")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("APPROVAL_LINE_NOT_CONFIGURED"));
        assertThat(audits.events).extracting(AuditEvent::eventName).contains(
                "access.approval_line.saved", "access.approval_line.listed", "access.approval_line.save_failed");
    }

    @Test
    void approvalLineReadFailureIsAuditedAndPreservesOriginalFailure() {
        var failingRepository = Mockito.mock(SupervisorReviewRepository.class);
        var failure = new IllegalStateException("storage unavailable");
        Mockito.when(failingRepository.listApprovalLines()).thenThrow(failure);
        var target = new OrgHierarchyService(failingRepository, Mockito.mock(AccessRepository.class),
                Mockito.mock(AuthorizationService.class), Mockito.mock(IdempotencyService.class), new AuditEventService(audits));
        assertThatThrownBy(target::listApprovalLines).isSameAs(failure);
        assertThat(audits.events).extracting(AuditEvent::eventName).contains("access.approval_line.list_failed");
    }

    static final class HierarchyRepository implements SupervisorReviewRepository {
        final Map<String, String> superiors = new LinkedHashMap<>();
        final Map<String, ApprovalLine> lines = new LinkedHashMap<>();

        @Override
        public SupervisorReviewDecisionRecord saveSupervisorReview(SupervisorReviewDecisionRecord record) {
            return record;
        }

        @Override
        public Optional<SupervisorReviewDecisionRecord> findPendingSupervisorReviewById(String reviewId) {
            return Optional.empty();
        }

        @Override
        public Optional<String> findDirectSuperior(String userId) {
            return Optional.ofNullable(superiors.get(userId));
        }

        @Override
        public Optional<ApprovalLine> findApprovalLine(String lineCode) {
            return Optional.ofNullable(lines.get(lineCode));
        }

        @Override
        public void saveDirectSuperior(String userId, String superiorId, String lineCode, String updatedBy, Instant updatedAt) {
            superiors.put(userId, superiorId);
        }

        @Override
        public List<OrgSuperior> listOrgHierarchy(int limit) {
            List<OrgSuperior> out = new ArrayList<>();
            superiors.forEach((user, superior) -> out.add(new OrgSuperior(user, superior, null, "x", Instant.EPOCH)));
            return out.stream().limit(limit).toList();
        }

        @Override
        public void saveApprovalLine(ApprovalLine line, String updatedBy, Instant updatedAt) {
            lines.put(line.lineCode(), line);
        }

        @Override
        public List<ApprovalLine> listApprovalLines() {
            return new ArrayList<>(lines.values());
        }
    }
}
