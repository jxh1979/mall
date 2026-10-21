package com.complex.platform.facility;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
import com.complex.platform.facility.dto.FacilityWorkOrderConfirmerRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderReassignRequest;
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
        // 第十二批裁决第 9 条：转派由物业主管（持 facility.work_order.manage）操作。
        assertThatThrownBy(() -> controller.reassign("wo_1",
                new FacilityWorkOrderReassignRequest("usr_vendor_b", "换人", "k_reassign")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
    }

    /**
     * HP-F02：指定提单确认人是物业侧动作，外包供应商只持处理权限，被 facility.work_order.manage 拒。
     * 提单确认与退回返工的权限闸在服务层（持 manage，或本人即设备告警自动工单的提单确认承接人），控制器不先按 manage 拦。
     */
    @Test
    void designateConfirmerRequiresManageButReviewAndReworkGateInService() {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied: facility.work_order.manage"))
                .when(authorizationService).requirePermission("facility.work_order.manage");

        assertThatThrownBy(() -> controller.designateConfirmer("wo_1",
                new FacilityWorkOrderConfirmerRequest("usr_b", "责任人即维修人", "k_des")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
        FacilityWorkOrderActionRequest rework = new FacilityWorkOrderActionRequest(null, null, "仍漏水", "k_rw");
        FacilityWorkOrderActionRequest review = new FacilityWorkOrderActionRequest(null, 5, null, "k_rv");
        assertThatCode(() -> controller.rework("wo_1", rework)).doesNotThrowAnyException();
        assertThatCode(() -> controller.review("wo_1", review)).doesNotThrowAnyException();
        verify(service).rework("wo_1", rework);
        verify(service).review("wo_1", review);
        verify(authorizationService, times(1)).requirePermission("facility.work_order.manage");
    }
}
