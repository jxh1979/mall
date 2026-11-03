package com.complex.platform.facility;

// 文件版本：V0.02

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.facility.dto.FacilityEquipmentRequest;
import com.complex.platform.facility.dto.FacilityEquipmentStatusRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 楼宇机电设备台账 API（③楼宇运营 S1，V165）。 */
@RestController
@RequestMapping("/api/v1/facility/equipment")
public class FacilityController {

    private static final String PERM = "facility.equipment.manage";

    private final FacilityEquipmentService service;
    private final AuthorizationService authorizationService;
    private final AuditedOperationExecutor operations;

    // 第十八批 N 段第 124 条 124-R3（PGM L738）：写口任一步失败（权限、校验、状态冲突）经 AuditedOperationExecutor.recordingFailure
    // 落 OpenAPI 声明的 x-audit-event-failure 再原样抛出；成功事件照旧由服务层写。
    public FacilityController(FacilityEquipmentService service, AuthorizationService authorizationService,
            AuditedOperationExecutor operations) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.operations = operations;
    }

    @GetMapping
    public ApiResponse<PageResponse<FacilityEquipmentRecord>> list(
            @RequestParam(value = "include_archived", required = false, defaultValue = "false") boolean includeArchived) {
        requireReadPermission();
        return ApiResponse.ok(service.listEquipment(includeArchived));
    }

    // ── V264 设施设备存档与审批后删除 ──────────────────────────────────
    @PostMapping("/{equipment_id}/archive")
    public ApiResponse<FacilityEquipmentRecord> archive(@PathVariable("equipment_id") String equipmentId,
            @org.springframework.web.bind.annotation.RequestBody(required = false) com.complex.platform.facility.dto.FacilityEquipmentArchiveRequest request) {
        authorizationService.requirePermission(PERM);
        return ApiResponse.ok(service.archive(equipmentId, true, request == null ? null : request.reason()));
    }

    @PostMapping("/{equipment_id}/unarchive")
    public ApiResponse<FacilityEquipmentRecord> unarchive(@PathVariable("equipment_id") String equipmentId) {
        authorizationService.requirePermission(PERM);
        return ApiResponse.ok(service.archive(equipmentId, false, null));
    }

    @PostMapping("/{equipment_id}/delete-request")
    public ApiResponse<FacilityEquipmentRecord> requestDelete(
            @PathVariable("equipment_id") String equipmentId,
            @RequestBody(required = false) com.complex.platform.facility.dto.FacilityEquipmentDeleteRequest request) {
        authorizationService.requirePermission(PERM);
        return ApiResponse.ok(service.requestDelete(equipmentId, request));
    }

    @PostMapping("/{equipment_id}/delete-approve")
    public ApiResponse<com.complex.platform.facility.dto.FacilityEquipmentDeleteDecision> approveDelete(
            @PathVariable("equipment_id") String equipmentId,
            @RequestBody(required = false) com.complex.platform.facility.dto.FacilityEquipmentDeleteApprovalRequest request) {
        authorizationService.requirePermission("facility.equipment.approve");
        return ApiResponse.ok(service.approveDelete(equipmentId, request));
    }

    @GetMapping("/maintenance-due")
    public ApiResponse<PageResponse<FacilityEquipmentRecord>> maintenanceDue() {
        requireReadPermission();
        return ApiResponse.ok(service.listMaintenanceDue());
    }

    @PostMapping
    public ApiResponse<FacilityEquipmentRecord> create(@RequestBody @jakarta.validation.Valid FacilityEquipmentRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.equipment_create_failed", "facility.equipment.manage",
                "facility_equipment", "new", key, () -> {
                    authorizationService.requirePermission(PERM);
                    requireIdempotencyKey(key);
                    return service.create(request);
                }));
    }

    @PostMapping("/{equipment_id}")
    public ApiResponse<FacilityEquipmentRecord> update(
            @PathVariable("equipment_id") String equipmentId, @RequestBody @jakarta.validation.Valid FacilityEquipmentRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.equipment_update_failed", "facility.equipment.manage",
                "facility_equipment", equipmentId, key, () -> {
                    authorizationService.requirePermission(PERM);
                    return service.update(equipmentId, request);
                }));
    }

    @PostMapping("/{equipment_id}/status")
    public ApiResponse<FacilityEquipmentRecord> changeStatus(
            @PathVariable("equipment_id") String equipmentId, @RequestBody @jakarta.validation.Valid FacilityEquipmentStatusRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.equipment_status_change_failed", "facility.equipment.manage",
                "facility_equipment", equipmentId, key, () -> {
                    authorizationService.requirePermission(PERM);
                    return service.changeStatus(equipmentId, request);
                }));
    }

    @PostMapping("/{equipment_id}/maintenance-done")
    public ApiResponse<FacilityEquipmentRecord> maintenanceDone(
            @PathVariable("equipment_id") String equipmentId,
            @RequestParam(value = "idempotency_key", required = false) String idempotencyKey,
            @RequestParam(value = "maintenance_type", required = false) String maintenanceType) {
        return ApiResponse.ok(operations.recordingFailure("facility.equipment_maintain_failed", "facility.equipment.manage",
                "facility_equipment", equipmentId, idempotencyKey, () -> {
                    authorizationService.requirePermission(PERM);
                    return service.recordMaintenanceDone(equipmentId, maintenanceType, idempotencyKey);
                }));
    }

    private void requireReadPermission() {
        AccessActor actor = authorizationService.currentActor();
        authorizationService.requirePermission(
                authorizationService.isOutsourcedVendor(actor) ? "facility.equipment.view" : PERM);
    }
}
