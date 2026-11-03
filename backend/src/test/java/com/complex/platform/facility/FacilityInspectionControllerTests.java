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
import com.complex.platform.facility.dto.FacilityInspectionExceptionReportRequest;
import com.complex.platform.facility.dto.FacilityInspectionRecheckConfirmRequest;
import com.complex.platform.facility.dto.FacilityInspectionRecordSubmitRequest;
import com.complex.platform.facility.dto.FacilityInspectionTaskRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FacilityInspectionControllerTests {

    private FacilityInspectionService service;
    private AuthorizationService authorizationService;
    private FacilityInspectionController controller;
    private AccessActor outsourced;

    @BeforeEach
    void setUp() {
        service = mock(FacilityInspectionService.class);
        authorizationService = mock(AuthorizationService.class);
        controller = new FacilityInspectionController(service, authorizationService, new com.complex.platform.evidence.AuditedOperationExecutor(null, mock(com.complex.platform.evidence.AuditEventService.class)));
        outsourced = new AccessActor("usr_vendor_a", "vendor-a", "outsourced_vendor", "org_1", null, "active");
        when(authorizationService.currentActor()).thenReturn(outsourced);
        when(authorizationService.isOutsourcedVendor(outsourced)).thenReturn(true);
    }

    @Test
    void outsourcedTaskListUsesAssignedViewPermission() {
        when(service.listTasks()).thenReturn(new PageResponse<>(List.of(), 1, 500, 0));

        controller.listTasks();

        verify(authorizationService).requirePermission("facility.inspection.view_assigned");
    }

    @Test
    void outsourcedCanReadTemplatesWithTemplateViewPermission() {
        when(service.listTemplates()).thenReturn(new PageResponse<>(List.of(), 1, 500, 0));

        controller.listTemplates();

        verify(authorizationService).requirePermission("facility.inspection.template.view");
    }

    @Test
    void outsourcedDetailAndRecordsUseAssignedViewPermission() {
        controller.getTask("task_1");
        controller.listRecords("task_1");

        verify(authorizationService, times(2)).requirePermission("facility.inspection.view_assigned");
    }

    @Test
    void outsourcedRecordFlowUsesRecordAssignedPermission() {
        controller.start("task_1", new FacilityInspectionRecheckConfirmRequest("start", "k_start"));
        controller.submitRecord("task_1", new FacilityInspectionRecordSubmitRequest(
                "normal", List.of("https://cdn.example/a.jpg"), "qr_1",
                BigDecimal.ONE, BigDecimal.TEN, List.of(Map.of("item", "door", "result", "normal")),
                "note", null, "usr_vendor_a", "k_record"));

        verify(authorizationService, times(2)).requirePermission("facility.inspection.record_assigned");
    }

    @Test
    void outsourcedReportExceptionUsesExceptionPermission() {
        controller.reportException("task_1", new FacilityInspectionExceptionReportRequest(
                List.of("https://cdn.example/abnormal.jpg"), "qr_1",
                BigDecimal.ONE, BigDecimal.TEN, List.of(Map.of("item", "door", "result", "abnormal")),
                "note", "door broken", "usr_vendor_a", "k_exception"));

        verify(authorizationService).requirePermission("facility.inspection.report_exception_assigned");
    }

    @Test
    void outsourcedCreateTaskStillRequiresManagePermission() {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied: facility.inspection.manage"))
                .when(authorizationService).requirePermission("facility.inspection.manage");

        assertThatThrownBy(() -> controller.createTask(new FacilityInspectionTaskRequest(
                null, "security", "task", "manual", List.of(), "usr_vendor_a",
                LocalDate.parse("2026-06-26"), LocalDate.parse("2026-06-26"), "proj_1", "sp_1", "k_task")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
        assertThatThrownBy(() -> controller.recheck("task_1", new FacilityInspectionRecheckConfirmRequest("recheck", "k_recheck")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
    }
}
