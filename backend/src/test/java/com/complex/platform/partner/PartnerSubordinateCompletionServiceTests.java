package com.complex.platform.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.facility.FacilityInspectionRepository;
import com.complex.platform.facility.FacilityWorkOrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 外包机构查看下属完成情况：按下属聚合被派工单/巡检 完成数/总数；未绑机构拒绝。纯单测。 */
class PartnerSubordinateCompletionServiceTests {

    private final PartnerRepository repository = mock(PartnerRepository.class);
    private final AuthorizationService authz = mock(AuthorizationService.class);
    private final FacilityWorkOrderRepository workOrders = mock(FacilityWorkOrderRepository.class);
    private final FacilityInspectionRepository inspections = mock(FacilityInspectionRepository.class);
    private final PartnerSubordinateCompletionService service =
            new PartnerSubordinateCompletionService(repository, authz, workOrders, inspections);

    private static final String ADMIN = "vendor-admin-1";
    private static final String ORG = "epo1";
    private static final String PROJ = "proj-1";

    @Test
    void aggregatesCompletionPerSubordinate() {
        when(authz.currentActor()).thenReturn(new AccessActor(ADMIN, "va", "outsourced_vendor", null, null, "active"));
        when(repository.findPartnerOrgIdForUser(ADMIN)).thenReturn(Optional.of(ORG));
        when(repository.findOrgById(ORG)).thenReturn(Optional.of(new ExternalPartnerOrgRecord(
                ORG, PROJ, "维保甲", "facility_maintenance", null, 5, null, "active",
                Instant.parse("2026-06-30T00:00:00Z"), Instant.parse("2026-06-30T00:00:00Z"))));
        PartnerSubordinateRecord sub = new PartnerSubordinateRecord(
                "sub-1", "worker1", ORG, "outsourced_vendor", "active", Instant.parse("2026-06-30T00:00:00Z"));
        when(repository.listSubordinates(ORG)).thenReturn(List.of(sub));
        // 2 工单（1 完成），1 巡检（0 完成）。
        when(workOrders.countCompletionByAssignees(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(PROJ)))
                .thenReturn(java.util.Map.of("sub-1", new int[] {2, 1}));
        when(inspections.countCompletionByAssignees(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(PROJ)))
                .thenReturn(java.util.Map.of("sub-1", new int[] {1, 0}));

        var page = service.listMySubordinateCompletion();
        assertThat(page.items()).hasSize(1);
        PartnerSubordinateCompletionRecord row = page.items().get(0);
        assertThat(row.subordinateId()).isEqualTo("sub-1");
        assertThat(row.workOrderTotal()).isEqualTo(2);
        assertThat(row.workOrderCompleted()).isEqualTo(1);
        assertThat(row.inspectionTotal()).isEqualTo(1);
        assertThat(row.inspectionCompleted()).isEqualTo(0);
    }

    @Test
    void rejectsWhenNotBoundToOrg() {
        when(authz.currentActor()).thenReturn(new AccessActor(ADMIN, "va", "outsourced_vendor", null, null, "active"));
        when(repository.findPartnerOrgIdForUser(ADMIN)).thenReturn(Optional.empty());
        assertThatThrownBy(service::listMySubordinateCompletion)
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PARTNER_ORG_NOT_BOUND"));
    }

}
