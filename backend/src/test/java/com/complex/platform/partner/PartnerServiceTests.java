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
        when(repository.findPartnerOrgIdForUser(ADMIN)).thenReturn(Optional.of(ORG));
    }

    @Test
    void listPartnerOrgsRejectsNonPlatformScopedCaller() {
        // 棘轮 F1 external_partner_orgs：机构列表读=平台管理员口径，须过 platform-scope 闸拒绝商户/外包越权读。
        org.mockito.Mockito.doThrow(ApiException.forbidden("PLATFORM_SCOPE_REQUIRED", "denied"))
                .when(authz).requirePlatformScope();
        assertThatThrownBy(service::listPartnerOrgs).isInstanceOf(ApiException.class);
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
