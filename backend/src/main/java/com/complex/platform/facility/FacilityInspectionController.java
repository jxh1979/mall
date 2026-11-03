package com.complex.platform.facility;

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.facility.dto.FacilityInspectionExceptionReportRequest;
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
    private final AuditedOperationExecutor operations;

    // 第十八批 N 段第 124 条 124-R3（PGM L738）：写口任一步失败（权限、校验、状态冲突）经 AuditedOperationExecutor.recordingFailure
    // 落 OpenAPI 声明的 x-audit-event-failure 再原样抛出；成功事件照旧由服务层写。
    public FacilityInspectionController(FacilityInspectionService service, AuthorizationService authorizationService,
            AuditedOperationExecutor operations) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.operations = operations;
    }

    @GetMapping("/templates")
    public ApiResponse<PageResponse<FacilityInspectionTemplateRecord>> listTemplates() {
        requirePermission("facility.inspection.template.view");
        return ApiResponse.ok(service.listTemplates());
    }

    @PostMapping("/templates")
    public ApiResponse<FacilityInspectionTemplateRecord> createTemplate(
            @RequestBody @jakarta.validation.Valid FacilityInspectionTemplateRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.inspection_template_create_failed", "facility.inspection.manage",
                "facility_inspection_template", "new", key, () -> {
                    authorizationService.requirePermission(PERM);
                    requireIdempotencyKey(key);
                    return service.createTemplate(request);
                }));
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
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.inspection_task_create_failed", "facility.inspection.manage",
                "facility_inspection_task", "new", key, () -> {
                    authorizationService.requirePermission(PERM);
                    requireIdempotencyKey(key);
                    return service.createTask(request);
                }));
    }

    @PostMapping("/tasks/{task_id}/start")
    public ApiResponse<FacilityInspectionTaskRecord> start(
            @PathVariable("task_id") String taskId, @RequestBody @jakarta.validation.Valid FacilityInspectionRecheckConfirmRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.inspection_task_start_failed", "facility.inspection.manage",
                "facility_inspection_task", taskId, key, () -> {
                    requirePermission("facility.inspection.record_assigned");
                    requireIdempotencyKey(key);
                    return service.start(taskId, request);
                }));
    }

    @PostMapping("/tasks/{task_id}/records")
    public ApiResponse<FacilityInspectionRecordRecord> submitRecord(
            @PathVariable("task_id") String taskId, @RequestBody @jakarta.validation.Valid FacilityInspectionRecordSubmitRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.inspection_record_submit_failed", "facility.inspection.manage",
                "facility_inspection_task", taskId, key, () -> {
                    requirePermission("facility.inspection.record_assigned");
                    requireIdempotencyKey(key);
                    return service.submitRecord(taskId, request);
                }));
    }

    @PostMapping("/tasks/{task_id}/report-exception")
    public ApiResponse<FacilityInspectionRecordRecord> reportException(
            @PathVariable("task_id") String taskId, @RequestBody @jakarta.validation.Valid FacilityInspectionExceptionReportRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.inspection_exception_report_failed", "facility.inspection.report_exception_assigned",
                "facility_inspection_task", taskId, key, () -> {
                    requirePermission("facility.inspection.report_exception_assigned");
                    requireIdempotencyKey(key);
                    return service.reportException(taskId, request);
                }));
    }

    @GetMapping("/tasks/{task_id}/records")
    public ApiResponse<PageResponse<FacilityInspectionRecordRecord>> listRecords(@PathVariable("task_id") String taskId) {
        requirePermission("facility.inspection.view_assigned");
        return ApiResponse.ok(service.listRecords(taskId));
    }

    @PostMapping("/tasks/{task_id}/recheck")
    public ApiResponse<FacilityInspectionTaskRecord> recheck(
            @PathVariable("task_id") String taskId, @RequestBody @jakarta.validation.Valid FacilityInspectionRecheckConfirmRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.inspection_task_recheck_failed", "facility.inspection.manage",
                "facility_inspection_task", taskId, key, () -> {
                    authorizationService.requirePermission(PERM);
                    requireIdempotencyKey(key);
                    return service.recheck(taskId, request);
                }));
    }

    @PostMapping("/tasks/{task_id}/supervisor-confirm")
    public ApiResponse<FacilityInspectionTaskRecord> supervisorConfirm(
            @PathVariable("task_id") String taskId, @RequestBody @jakarta.validation.Valid FacilityInspectionRecheckConfirmRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.inspection_task_close_failed", "facility.inspection.manage",
                "facility_inspection_task", taskId, key, () -> {
                    authorizationService.requirePermission(PERM);
                    requireIdempotencyKey(key);
                    return service.supervisorConfirm(taskId, request);
                }));
    }

    private void requirePermission(String outsourcedPermission) {
        AccessActor actor = authorizationService.currentActor();
        authorizationService.requirePermission(
                authorizationService.isOutsourcedVendor(actor) ? outsourcedPermission : PERM);
    }
}
