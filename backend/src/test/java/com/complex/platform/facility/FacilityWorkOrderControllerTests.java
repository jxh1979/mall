package com.complex.platform.facility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.facility.dto.FacilityWorkOrderActionRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderAssignRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FacilityWorkOrderControllerTests {

    private FacilityWorkOrderService service;
    private AuthorizationService authorizationService;
    private FacilityWorkOrderController controller;
    private AccessActor outsourced;

    @BeforeEach
    void setUp() {
        service = mock(FacilityWorkOrderService.class);
        authorizationService = mock(AuthorizationService.class);
        controller = new FacilityWorkOrderController(service, authorizationService);
        outsourced = new AccessActor("usr_vendor_a", "vendor-a", "outsourced_vendor", "org_1", null, "active");
        when(authorizationService.currentActor()).thenReturn(outsourced);
        when(authorizationService.isOutsourcedVendor(outsourced)).thenReturn(true);
    }

    @Test
    void outsourcedListUsesAssignedViewPermission() {
        when(service.list()).thenReturn(new PageResponse<>(List.of(), 1, 500, 0));

        controller.list();

        verify(authorizationService).requirePermission("facility.work_order.view_assigned");
    }

    @Test
    void outsourcedDetailUsesAssignedViewPermission() {
        controller.get("wo_1");

        verify(authorizationService).requirePermission("facility.work_order.view_assigned");
    }

    @Test
    void outsourcedProcessUsesAssignedPermissionButAcceptRequiresPropertyPermission() {
        FacilityWorkOrderActionRequest process = new FacilityWorkOrderActionRequest("done", null, "note", "k_process");
        FacilityWorkOrderActionRequest accept = new FacilityWorkOrderActionRequest(null, 5, "ok", "k_accept");
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied: facility.work_order.manage"))
                .when(authorizationService).requirePermission("facility.work_order.manage");

        controller.start("wo_1", process);
        controller.complete("wo_1", process);

        verify(authorizationService, times(2)).requirePermission("facility.work_order.process_assigned");
        assertThatThrownBy(() -> controller.accept("wo_1", accept))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
        verify(authorizationService).requirePermission("facility.work_order.manage");
    }

    @Test
    void propertySideCanAcceptWithManagePermission() {
        AccessActor staff = new AccessActor("usr_ops", "ops", "staff", "org_1", null, "active");
        when(authorizationService.currentActor()).thenReturn(staff);
        when(authorizationService.isOutsourcedVendor(staff)).thenReturn(false);

        controller.accept("wo_1", new FacilityWorkOrderActionRequest(null, 5, "ok", "k_accept"));

        verify(authorizationService).requirePermission("facility.work_order.manage");
    }

    @Test
    void outsourcedCreateAndAssignStillRequireManagePermission() {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied: facility.work_order.manage"))
                .when(authorizationService).requirePermission("facility.work_order.manage");

        assertThatThrownBy(() -> controller.create(new FacilityWorkOrderRequest(
                "repair", "high", "title", "desc", "platform", "sp_1", null,
                "usr_reporter", "staff", "proj_1", null, "k_create")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
        assertThatThrownBy(() -> controller.assign("wo_1", new FacilityWorkOrderAssignRequest("usr_vendor_a", "k_assign")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
        assertThatThrownBy(() -> controller.suspend("wo_1",
                new FacilityWorkOrderActionRequest(null, null, "hold", "k_suspend")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
    }
}
