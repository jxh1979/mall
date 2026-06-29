package com.complex.platform.energy;

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.energy.dto.EnergyMeterRequest;
import com.complex.platform.energy.dto.EnergyReadingRecordRequest;
import com.complex.platform.energy.dto.EnergyReadingReviewRequest;
import com.complex.platform.energy.dto.EnergySettleRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 楼宇运营 S4 能耗抄表 API。 */
@RestController
@RequestMapping("/api/v1/energy")
public class EnergyController {

    private static final String METER_PERM = "energy.meter.manage";
    private static final String READING_PERM = "energy.reading.manage";

    private final EnergyService service;
    private final AuthorizationService authorizationService;

    public EnergyController(EnergyService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/meters")
    public ApiResponse<PageResponse<EnergyMeterRecord>> listMeters() {
        authorizationService.requirePermission(METER_PERM);
        return ApiResponse.ok(service.listMeters());
    }

    @PostMapping("/meters")
    public ApiResponse<EnergyMeterRecord> createMeter(@RequestBody @jakarta.validation.Valid EnergyMeterRequest request) {
        authorizationService.requirePermission(METER_PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createMeter(request));
    }

    @PostMapping("/meters/{meter_id}")
    public ApiResponse<EnergyMeterRecord> updateMeter(
            @PathVariable("meter_id") String meterId, @RequestBody @jakarta.validation.Valid EnergyMeterRequest request) {
        authorizationService.requirePermission(METER_PERM);
        return ApiResponse.ok(service.updateMeter(meterId, request));
    }

    @GetMapping("/readings")
    public ApiResponse<PageResponse<EnergyMeterReadingRecord>> listReadings(
            @RequestParam(value = "period", required = false) String period) {
        authorizationService.requirePermission(READING_PERM);
        return ApiResponse.ok(service.listReadings(period));
    }

    @PostMapping("/readings")
    public ApiResponse<EnergyMeterReadingRecord> recordReading(@RequestBody @jakarta.validation.Valid EnergyReadingRecordRequest request) {
        authorizationService.requirePermission(READING_PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.recordReading(request));
    }

    @PostMapping("/readings/{reading_id}/review")
    public ApiResponse<EnergyMeterReadingRecord> reviewReading(
            @PathVariable("reading_id") String readingId, @RequestBody @jakarta.validation.Valid EnergyReadingReviewRequest request) {
        authorizationService.requirePermission(READING_PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reviewReading(readingId, request));
    }

    @GetMapping("/meters/{meter_id}/difference")
    public ApiResponse<EnergyMeterDifferenceRecord> meterDifference(
            @PathVariable("meter_id") String meterId, @RequestParam("period") String period) {
        authorizationService.requirePermission(READING_PERM);
        return ApiResponse.ok(service.meterDifference(meterId, period));
    }

    @PostMapping({"/settlements/energy-fee", "/settle-energy-fee"})
    public ApiResponse<List<EnergyFeeSettlementRecord>> settleEnergyFee(@RequestBody @jakarta.validation.Valid EnergySettleRequest request) {
        authorizationService.requirePermission(READING_PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.settleEnergyFee(request.period(), request.idempotencyKey()));
    }
}
