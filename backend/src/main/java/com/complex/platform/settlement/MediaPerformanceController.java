package com.complex.platform.settlement;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.settlement.MediaPerformanceRecords.FulfillmentRecord;
import com.complex.platform.settlement.MediaPerformanceRecords.SettlementRecord;
import com.complex.platform.settlement.MediaPerformanceRecords.ShowDetail;
import com.complex.platform.settlement.MediaPerformanceRecords.ShowRecord;
import com.complex.platform.settlement.MediaPerformanceRequests.ActionRequest;
import com.complex.platform.settlement.MediaPerformanceRequests.AdjustRequest;
import com.complex.platform.settlement.MediaPerformanceRequests.DisputeRequest;
import com.complex.platform.settlement.MediaPerformanceRequests.FulfillmentRequest;
import com.complex.platform.settlement.MediaPerformanceRequests.ResolveRequest;
import com.complex.platform.settlement.MediaPerformanceRequests.ShowRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TK-29 传媒逐场演出合同记录 / 实际履约确认 / 三种结算入口（BSM L1144–L1145；操作员裁决 2026-10-20）。
 * 权限码与范围校验在 {@link MediaPerformanceService} / {@link MediaPerformanceSettlementService}。
 */
@RestController
@RequestMapping("/api/v1/settlement/media-shows")
public class MediaPerformanceController {

    private final MediaPerformanceService service;
    private final MediaPerformanceSettlementService settlementService;

    public MediaPerformanceController(MediaPerformanceService service, MediaPerformanceSettlementService settlementService) {
        this.service = service;
        this.settlementService = settlementService;
    }

    @GetMapping
    public ApiResponse<List<ShowRecord>> list(@RequestParam(value = "project_id", required = false) String projectId,
                                              @RequestParam(value = "merchant_id", required = false) String merchantId) {
        return ApiResponse.ok(service.listShows(projectId, merchantId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ShowDetail> detail(@PathVariable("id") String id) {
        return ApiResponse.ok(service.getDetail(id));
    }

    @PostMapping
    public ApiResponse<ShowRecord> create(@RequestBody ShowRequest request) {
        return ApiResponse.ok(service.createShow(request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ShowRecord> update(@PathVariable("id") String id, @RequestBody ShowRequest request) {
        return ApiResponse.ok(service.updateShow(id, request));
    }

    @PostMapping("/{id}/lock")
    public ApiResponse<ShowRecord> lock(@PathVariable("id") String id, @RequestBody ActionRequest request) {
        return ApiResponse.ok(service.lockShow(id, request));
    }

    @PostMapping("/{id}/versions")
    public ApiResponse<ShowRecord> createVersion(@PathVariable("id") String id, @RequestBody ShowRequest request) {
        return ApiResponse.ok(service.createVersion(id, request));
    }

    @PostMapping("/{id}/fulfillment")
    public ApiResponse<FulfillmentRecord> record(@PathVariable("id") String id, @RequestBody FulfillmentRequest request) {
        return ApiResponse.ok(service.recordFulfillment(id, request));
    }

    @PostMapping("/{id}/fulfillment/confirm")
    public ApiResponse<FulfillmentRecord> confirm(@PathVariable("id") String id, @RequestBody ActionRequest request) {
        return ApiResponse.ok(service.confirmFulfillment(id, request));
    }

    @PostMapping("/{id}/fulfillment/dispute")
    public ApiResponse<FulfillmentRecord> dispute(@PathVariable("id") String id, @RequestBody DisputeRequest request) {
        return ApiResponse.ok(service.disputeFulfillment(id, request));
    }

    @PostMapping("/{id}/fulfillment/dispute/resolve")
    public ApiResponse<FulfillmentRecord> resolve(@PathVariable("id") String id, @RequestBody ResolveRequest request) {
        return ApiResponse.ok(service.resolveDispute(id, request));
    }

    @PostMapping("/{id}/settle")
    public ApiResponse<List<SettlementRecord>> settle(@PathVariable("id") String id, @RequestBody ActionRequest request) {
        return ApiResponse.ok(settlementService.settleShow(id, request == null ? null : request.idempotencyKey()));
    }

    @PostMapping("/{id}/settlements/{settlementId}/adjust")
    public ApiResponse<SettlementRecord> adjust(@PathVariable("id") String id, @PathVariable("settlementId") String settlementId,
                                                @RequestBody AdjustRequest request) {
        return ApiResponse.ok(settlementService.adjust(id, settlementId, request));
    }
}
