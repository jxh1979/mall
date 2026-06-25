package com.complex.platform.facility;

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.facility.dto.FacilityEquipmentRequest;
import com.complex.platform.facility.dto.FacilityInspectionCompleteRequest;
import com.complex.platform.facility.dto.FacilityInspectionTemplateRequest;
import com.complex.platform.facility.dto.FacilityStatusRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderAssignRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderStatusRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ③ 楼宇运营 S1 楼宇设施设备台账 API。 */
@RestController
@RequestMapping("/api/v1/facility")
public class FacilityController {

    private final FacilityService service;
    private final AuthorizationService authorizationService;

    public FacilityController(FacilityService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/equipment")
    public ApiResponse<PageResponse<FacilityEquipmentRecord>> listEquipment() {
        authorizationService.requirePermission("facility.equipment.manage");
        return ApiResponse.ok(service.listEquipment());
    }

    @PostMapping("/equipment")
    public ApiResponse<FacilityEquipmentRecord> createEquipment(@RequestBody FacilityEquipmentRequest request) {
        authorizationService.requirePermission("facility.equipment.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createEquipment(request));
    }

    @PostMapping("/equipment/{equipment_id}/status")
    public ApiResponse<FacilityEquipmentRecord> updateEquipmentStatus(
            @PathVariable("equipment_id") String equipmentId, @RequestBody FacilityStatusRequest request) {
        authorizationService.requirePermission("facility.equipment.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.updateEquipmentStatus(equipmentId, request));
    }

    @GetMapping("/work-orders")
    public ApiResponse<PageResponse<FacilityWorkOrderRecord>> listWorkOrders() {
        authorizationService.requirePermission("facility.work_order.manage");
        return ApiResponse.ok(service.listWorkOrders());
    }

    @PostMapping("/work-orders")
    public ApiResponse<FacilityWorkOrderRecord> createWorkOrder(@RequestBody FacilityWorkOrderRequest request) {
        authorizationService.requirePermission("facility.work_order.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createWorkOrder(request));
    }

    @PostMapping("/work-orders/{work_order_id}/assign")
    public ApiResponse<FacilityWorkOrderRecord> assignWorkOrder(
            @PathVariable("work_order_id") String workOrderId, @RequestBody FacilityWorkOrderAssignRequest request) {
        authorizationService.requirePermission("facility.work_order.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.assignWorkOrder(workOrderId, request));
    }

    @PostMapping("/work-orders/{work_order_id}/status")
    public ApiResponse<FacilityWorkOrderRecord> updateWorkOrderStatus(
            @PathVariable("work_order_id") String workOrderId, @RequestBody FacilityWorkOrderStatusRequest request) {
        authorizationService.requirePermission("facility.work_order.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.updateWorkOrderStatus(workOrderId, request));
    }

    @GetMapping("/inspection-templates")
    public ApiResponse<PageResponse<FacilityInspectionTemplateRecord>> listInspectionTemplates() {
        authorizationService.requirePermission("facility.inspection.manage");
        return ApiResponse.ok(service.listInspectionTemplates());
    }

    @PostMapping("/inspection-templates")
    public ApiResponse<FacilityInspectionTemplateRecord> createInspectionTemplate(
            @RequestBody FacilityInspectionTemplateRequest request) {
        authorizationService.requirePermission("facility.inspection.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createInspectionTemplate(request));
    }

    @GetMapping("/inspection-tasks")
    public ApiResponse<PageResponse<FacilityInspectionTaskRecord>> listInspectionTasks() {
        authorizationService.requirePermission("facility.inspection.manage");
        return ApiResponse.ok(service.listInspectionTasks());
    }

    @PostMapping("/inspection-tasks/generate")
    public ApiResponse<Map<String, Integer>> generateInspectionTasks() {
        authorizationService.requirePermission("facility.inspection.manage");
        return ApiResponse.ok(Map.of("upserted", service.generateDueInspectionTasksManually()));
    }

    @PostMapping("/inspection-tasks/{task_id}/complete")
    public ApiResponse<FacilityInspectionRecordRecord> completeInspectionTask(
            @PathVariable("task_id") String taskId, @RequestBody FacilityInspectionCompleteRequest request) {
        authorizationService.requirePermission("facility.inspection.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.completeInspectionTask(taskId, request));
    }
}
