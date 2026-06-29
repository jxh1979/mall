package com.complex.platform.facility;

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.facility.dto.FacilityInspectionRecheckConfirmRequest;
import com.complex.platform.facility.dto.FacilityInspectionRecordSubmitRequest;
import com.complex.platform.facility.dto.FacilityInspectionTaskRequest;
import com.complex.platform.facility.dto.FacilityInspectionTemplateRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Building inspection API (S3, V167). */
@RestController
@RequestMapping("/api/v1/facility/inspections")
public class FacilityInspectionController {

    private static final String PERM = "facility.inspection.manage";

    private final FacilityInspectionService service;
    private final AuthorizationService authorizationService;

    public FacilityInspectionController(FacilityInspectionService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/templates")
    public ApiResponse<PageResponse<FacilityInspectionTemplateRecord>> listTemplates() {
        authorizationService.requirePermission(PERM);
        return ApiResponse.ok(service.listTemplates());
    }

    @PostMapping("/templates")
    public ApiResponse<FacilityInspectionTemplateRecord> createTemplate(
            @RequestBody @jakarta.validation.Valid FacilityInspectionTemplateRequest request) {
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createTemplate(request));
    }

    @GetMapping("/tasks")
    public ApiResponse<PageResponse<FacilityInspectionTaskRecord>> listTasks() {
        requirePermission("facility.inspection.view_assigned");
        return ApiResponse.ok(service.listTasks());
    }

    @GetMapping("/tasks/{task_id}")
    public ApiResponse<FacilityInspectionTaskRecord> getTask(@PathVariable("task_id") String taskId) {
        requirePermission("facility.inspection.view_assigned");
        return ApiResponse.ok(service.findTaskById(taskId));
    }

    @PostMapping("/tasks")
    public ApiResponse<FacilityInspectionTaskRecord> createTask(@RequestBody @jakarta.validation.Valid FacilityInspectionTaskRequest request) {
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createTask(request));
    }

    @PostMapping("/tasks/{task_id}/start")
    public ApiResponse<FacilityInspectionTaskRecord> start(
            @PathVariable("task_id") String taskId, @RequestBody @jakarta.validation.Valid FacilityInspectionRecheckConfirmRequest request) {
        requirePermission("facility.inspection.record_assigned");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.start(taskId, request));
    }

    @PostMapping("/tasks/{task_id}/records")
    public ApiResponse<FacilityInspectionRecordRecord> submitRecord(
            @PathVariable("task_id") String taskId, @RequestBody @jakarta.validation.Valid FacilityInspectionRecordSubmitRequest request) {
        requirePermission("facility.inspection.record_assigned");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.submitRecord(taskId, request));
    }

    @GetMapping("/tasks/{task_id}/records")
    public ApiResponse<PageResponse<FacilityInspectionRecordRecord>> listRecords(@PathVariable("task_id") String taskId) {
        requirePermission("facility.inspection.view_assigned");
        return ApiResponse.ok(service.listRecords(taskId));
    }

    @PostMapping("/tasks/{task_id}/recheck")
    public ApiResponse<FacilityInspectionTaskRecord> recheck(
            @PathVariable("task_id") String taskId, @RequestBody @jakarta.validation.Valid FacilityInspectionRecheckConfirmRequest request) {
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.recheck(taskId, request));
    }

    @PostMapping("/tasks/{task_id}/supervisor-confirm")
    public ApiResponse<FacilityInspectionTaskRecord> supervisorConfirm(
            @PathVariable("task_id") String taskId, @RequestBody @jakarta.validation.Valid FacilityInspectionRecheckConfirmRequest request) {
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.supervisorConfirm(taskId, request));
    }

    private void requirePermission(String outsourcedPermission) {
        AccessActor actor = authorizationService.currentActor();
        authorizationService.requirePermission(
                authorizationService.isOutsourcedVendor(actor) ? outsourcedPermission : PERM);
    }
}
