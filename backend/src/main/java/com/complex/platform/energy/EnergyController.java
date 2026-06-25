package com.complex.platform.energy;

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.energy.dto.EnergyMeterReadingRequest;
import com.complex.platform.energy.dto.EnergyMeterRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ③ 楼宇运营 S4 能耗抄表/计量 API。 */
@RestController
@RequestMapping("/api/v1/facility/energy")
public class EnergyController {

    private final EnergyService service;
    private final AuthorizationService authorizationService;

    public EnergyController(EnergyService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/meters")
    public ApiResponse<PageResponse<EnergyMeterRecord>> listMeters() {
        authorizationService.requirePermission("facility.energy.manage");
        return ApiResponse.ok(service.listMeters());
    }

    @PostMapping("/meters")
    public ApiResponse<EnergyMeterRecord> createMeter(@RequestBody EnergyMeterRequest request) {
        authorizationService.requirePermission("facility.energy.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createMeter(request));
    }

    @PostMapping("/meters/{meter_id}/readings")
    public ApiResponse<EnergyMeterReadingRecord> recordReading(
            @PathVariable("meter_id") String meterId, @RequestBody EnergyMeterReadingRequest request) {
        authorizationService.requirePermission("facility.energy.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.recordReading(meterId, request));
    }

    @GetMapping("/readings")
    public ApiResponse<PageResponse<EnergyMeterReadingRecord>> listReadings() {
        authorizationService.requirePermission("facility.energy.manage");
        return ApiResponse.ok(service.listReadings());
    }
}
