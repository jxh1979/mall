package com.complex.platform.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.partner.dto.PartnerSubordinateRequest;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PartnerServiceTests {

    private final PartnerRepository repository = mock(PartnerRepository.class);
    private final AuthorizationService authz = mock(AuthorizationService.class);
    private final AuditEventService audit = mock(AuditEventService.class);
    private final PartnerService service = new PartnerService(repository, authz, audit);

    private static final String ADMIN = "vendor-admin-1";
    private static final String ORG = "epo1";

    @BeforeEach
    void setUp() {
        when(authz.currentActor()).thenReturn(new AccessActor(ADMIN, "va", "outsourced_vendor", null, null, "active"));
        when(authz.currentProjectScopeQuietly()).thenReturn(Optional.empty());
        when(repository.findPartnerOrgIdForUser(ADMIN)).thenReturn(Optional.of(ORG));
    }

    @Test
    void listPartnerOrgsPassesProjectScopeToRepository() {
        // 3.2 跨项目读隔离：项目员工读机构列表时把本项目 scope 下沉仓储谓词。
        when(authz.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_a"));
        when(repository.listOrgs("prj_a")).thenReturn(java.util.List.of());
        service.listPartnerOrgs();
        verify(repository).listOrgs("prj_a");
    }

    @Test
    void listPartnerOrgsRejectsNonPlatformScopedCaller() {
        // 棘轮 F1 external_partner_orgs：机构列表读=平台管理员口径，须过 platform-scope 闸拒绝商户/外包越权读。
        org.mockito.Mockito.doThrow(ApiException.forbidden("PLATFORM_SCOPE_REQUIRED", "denied"))
                .when(authz).requirePlatformScope();
        assertThatThrownBy(service::listPartnerOrgs).isInstanceOf(ApiException.class);
    }

    @Test
    void bindUserToOrgValidatesOrgActiveAndOutsourcedUserThenUpdates() {
        when(repository.findOrgById(ORG)).thenReturn(Optional.of(org(3, "active", null)));
        when(repository.findAccessUserAccountType("u1")).thenReturn(Optional.of("outsourced_vendor"));
        service.bindUserToOrg(ORG, " u1 ");
        verify(repository).updateUserPartnerOrg("u1", ORG);
    }

    @Test
    void bindUserToOrgRejectsNonOutsourcedAccountTypes() {
        // 复核实害链:误绑平台账号→外部机构管理员可停用之/到期任务静默禁用——非外包类账号一律拒。
        when(repository.findOrgById(ORG)).thenReturn(Optional.of(org(3, "active", null)));
        when(repository.findAccessUserAccountType("admin1")).thenReturn(Optional.of("platform_admin"));
        assertThatThrownBy(() -> service.bindUserToOrg(ORG, "admin1"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("ACCESS_USER_NOT_OUTSOURCED"));
        verify(repository, org.mockito.Mockito.never()).updateUserPartnerOrg(any(), any());
    }

    @Test
    void bindUserToOrgRejectsMissingOrInactiveOrgAndMissingUser() {
        when(repository.findOrgById("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.bindUserToOrg("ghost", "u1"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PARTNER_ORG_NOT_FOUND"));
        when(repository.findOrgById(ORG)).thenReturn(Optional.of(org(3, "suspended", null)));
        assertThatThrownBy(() -> service.bindUserToOrg(ORG, "u1"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PARTNER_ORG_NOT_ACTIVE"));
        when(repository.findOrgById(ORG)).thenReturn(Optional.of(org(3, "active", null)));
        when(repository.findAccessUserAccountType("ghost_user")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.bindUserToOrg(ORG, "ghost_user"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ACCESS_USER_NOT_FOUND"));
        verify(repository, org.mockito.Mockito.never()).updateUserPartnerOrg(any(), any());
    }

    @Test
    void unbindUserFromOrgClearsBindingWhenUserBoundToThisOrg() {
        // 2026-07-18 收尾:解绑=bind 逆操作;机构非 active 也允许解绑(停用/过期机构要能清挂靠)。
        when(repository.findOrgById(ORG)).thenReturn(Optional.of(org(3, "suspended", null)));
        when(repository.findAccessUserAccountType("u1")).thenReturn(Optional.of("outsourced_vendor"));
        when(repository.findPartnerOrgIdForUser("u1")).thenReturn(Optional.of(ORG));
        service.unbindUserFromOrg(ORG, " u1 ");
        verify(repository).updateUserPartnerOrg("u1", null);
    }

    @Test
    void unbindUserFromOrgRejectsWhenUserBoundElsewhereOrUnbound() {
        // 校验该账号当前绑的就是这个机构:绑在别处/本就未绑 → 409,不写库。
        when(repository.findOrgById(ORG)).thenReturn(Optional.of(org(3, "active", null)));
        when(repository.findAccessUserAccountType("u1")).thenReturn(Optional.of("outsourced_vendor"));
        when(repository.findPartnerOrgIdForUser("u1")).thenReturn(Optional.of("epo-other"));
        assertThatThrownBy(() -> service.unbindUserFromOrg(ORG, "u1"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PARTNER_ORG_USER_NOT_BOUND"));
        when(repository.findPartnerOrgIdForUser("u1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.unbindUserFromOrg(ORG, "u1"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PARTNER_ORG_USER_NOT_BOUND"));
        verify(repository, org.mockito.Mockito.never()).updateUserPartnerOrg(any(), any());
    }

    @Test
    void unbindUserFromOrgRejectsMissingOrgOrUser() {
        when(repository.findOrgById("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.unbindUserFromOrg("ghost", "u1"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PARTNER_ORG_NOT_FOUND"));
        when(repository.findOrgById(ORG)).thenReturn(Optional.of(org(3, "active", null)));
        when(repository.findAccessUserAccountType("ghost_user")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.unbindUserFromOrg(ORG, "ghost_user"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ACCESS_USER_NOT_FOUND"));
        verify(repository, org.mockito.Mockito.never()).updateUserPartnerOrg(any(), any());
    }

    private ExternalPartnerOrgRecord org(int max, String status, Instant validUntil) {
        Instant now = Instant.parse("2026-06-30T00:00:00Z");
        return new ExternalPartnerOrgRecord(ORG, null, "维保甲", "facility_maintenance", null, max, validUntil, status, now, now);
    }

    @Test
    void createSubordinateEnforcesCap() {
        when(repository.findOrgById(ORG)).thenReturn(Optional.of(org(2, "active", null)));
        when(repository.countActiveSubordinates(ORG)).thenReturn(2L);
        assertThatThrownBy(() -> service.createSubordinate(new PartnerSubordinateRequest("k", "u1")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PARTNER_SUBORDINATE_LIMIT_REACHED"));
    }

    @Test
    void createSubordinateUnderCapAssignsCategoryRole() {
        when(repository.findOrgById(ORG)).thenReturn(Optional.of(org(3, "active", null)));
        when(repository.countActiveSubordinates(ORG)).thenReturn(1L);
        when(repository.createSubordinate(any(), eq("role_outsourced_facility"))).thenAnswer(i -> i.getArgument(0));
        PartnerSubordinateRecord saved = service.createSubordinate(new PartnerSubordinateRequest("k", "u1"));
        assertThat(saved.partnerOrgId()).isEqualTo(ORG);
        verify(repository).createSubordinate(any(), eq("role_outsourced_facility"));
    }

    @Test
    void createSubordinateRejectsExpiredOrg() {
        when(repository.findOrgById(ORG)).thenReturn(Optional.of(org(5, "active", Instant.parse("2020-01-01T00:00:00Z"))));
        assertThatThrownBy(() -> service.createSubordinate(new PartnerSubordinateRequest("k", "u1")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PARTNER_ORG_EXPIRED"));
    }

    @Test
    void notBoundToOrgRejected() {
        when(repository.findPartnerOrgIdForUser(ADMIN)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.listMySubordinates())
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PARTNER_ORG_NOT_BOUND"));
    }

    @Test
    void disableSubordinateRejectsOtherOrg() {
        when(repository.findSubordinate("other-user")).thenReturn(Optional.of(
                new PartnerSubordinateRecord("other-user", "x", "epo-OTHER", "outsourced_vendor", "active", Instant.parse("2026-06-30T00:00:00Z"))));
        assertThatThrownBy(() -> service.disableSubordinate("other-user"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PARTNER_SUBORDINATE_FORBIDDEN"));
    }
}
