package com.complex.platform.facility;

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
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

    public FacilityController(FacilityEquipmentService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
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
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.create(request));
    }

    @PostMapping("/{equipment_id}")
    public ApiResponse<FacilityEquipmentRecord> update(
            @PathVariable("equipment_id") String equipmentId, @RequestBody @jakarta.validation.Valid FacilityEquipmentRequest request) {
        authorizationService.requirePermission(PERM);
        return ApiResponse.ok(service.update(equipmentId, request));
    }

    @PostMapping("/{equipment_id}/status")
    public ApiResponse<FacilityEquipmentRecord> changeStatus(
            @PathVariable("equipment_id") String equipmentId, @RequestBody @jakarta.validation.Valid FacilityEquipmentStatusRequest request) {
        authorizationService.requirePermission(PERM);
        return ApiResponse.ok(service.changeStatus(equipmentId, request));
    }

    @PostMapping("/{equipment_id}/maintenance-done")
    public ApiResponse<FacilityEquipmentRecord> maintenanceDone(
            @PathVariable("equipment_id") String equipmentId,
            @RequestParam(value = "idempotency_key", required = false) String idempotencyKey) {
        authorizationService.requirePermission(PERM);
        return ApiResponse.ok(service.recordMaintenanceDone(equipmentId, idempotencyKey));
    }

    private void requireReadPermission() {
        AccessActor actor = authorizationService.currentActor();
        authorizationService.requirePermission(
                authorizationService.isOutsourcedVendor(actor) ? "facility.equipment.view" : PERM);
    }
}
