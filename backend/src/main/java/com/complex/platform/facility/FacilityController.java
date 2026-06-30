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
    public ApiResponse<PageResponse<FacilityEquipmentRecord>> list() {
        requireReadPermission();
        return ApiResponse.ok(service.listEquipment());
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
