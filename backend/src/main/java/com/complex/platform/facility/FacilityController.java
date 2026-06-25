package com.complex.platform.facility;

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.facility.dto.FacilityEquipmentRequest;
import com.complex.platform.facility.dto.FacilityStatusRequest;
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
}
