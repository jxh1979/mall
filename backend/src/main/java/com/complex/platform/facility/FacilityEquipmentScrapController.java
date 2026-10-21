package com.complex.platform.facility;

// 文件版本：V0.01

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.facility.dto.FacilityEquipmentScrapRequests;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** HP-F03 楼宇设备报废：申请（审批）、实际处置登记、财务残值/损失登记与报废处置清单（BSM L1408）。 */
@RestController
public class FacilityEquipmentScrapController {

    private static final String PERM = "facility.equipment.manage";

    private final FacilityEquipmentScrapService service;
    private final AuthorizationService authorizationService;

    public FacilityEquipmentScrapController(FacilityEquipmentScrapService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/api/v1/facility/equipment/{equipment_id}/scrap-requests")
    public ApiResponse<FacilityEquipmentScrapService.Scrap> request(@PathVariable("equipment_id") String equipmentId,
            @RequestBody FacilityEquipmentScrapRequests.Create request) {
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.request(equipmentId, request));
    }

    @GetMapping("/api/v1/facility/equipment-scraps")
    public ApiResponse<PageResponse<FacilityEquipmentScrapService.Scrap>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping("/api/v1/facility/equipment-scraps/{scrap_id}/disposal")
    public ApiResponse<FacilityEquipmentScrapService.Scrap> disposal(@PathVariable("scrap_id") String scrapId,
            @RequestBody FacilityEquipmentScrapRequests.Disposal request) {
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.completeDisposal(scrapId, request));
    }

    @PostMapping("/api/v1/facility/equipment-scraps/{scrap_id}/finance")
    public ApiResponse<FacilityEquipmentScrapService.Scrap> finance(@PathVariable("scrap_id") String scrapId,
            @RequestBody FacilityEquipmentScrapRequests.Finance request) {
        authorizationService.requirePermission("finance.ar_ap.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.recordFinance(scrapId, request));
    }
}
