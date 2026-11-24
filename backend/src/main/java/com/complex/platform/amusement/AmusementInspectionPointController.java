package com.complex.platform.amusement;

// 文件版本：V0.02

import com.complex.platform.common.api.ApiRequests;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.evidence.ManualBackfillService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第十八批 BS 段第 373 条 373-R1、373-R5：游乐项目巡检点位清单（读、整份替换）与按序扫码走点，见 {@link AmusementInspectionPointService}；
 * 第十八批 CF 段第 404 条 404-R1：点位缺扫码人工补录（通用补录主链，见 {@link AmusementInspectionPointBackfillTarget}）。
 */
@RestController
@RequestMapping("/api/v1/amusement/projects/{id}/inspection-points")
public class AmusementInspectionPointController {

    private final AmusementInspectionPointService service;
    private final ManualBackfillService backfills;

    public AmusementInspectionPointController(AmusementInspectionPointService service, ManualBackfillService backfills) {
        this.service = service;
        this.backfills = backfills;
    }

    @GetMapping
    public ApiResponse<AmusementInspectionPointService.View> view(@PathVariable("id") String projectId) {
        return ApiResponse.ok(service.view(projectId));
    }

    @PutMapping
    public ApiResponse<AmusementInspectionPointService.View> replace(@PathVariable("id") String projectId,
            @RequestBody(required = false) AmusementInspectionPointService.ReplaceRequest request) {
        ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.replacePoints(projectId, request));
    }

    @PostMapping("/{seq}/check")
    public ApiResponse<AmusementInspectionPointService.View> check(@PathVariable("id") String projectId, @PathVariable("seq") int seq,
            @RequestBody(required = false) AmusementInspectionPointService.CheckRequest request) {
        ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.checkPoint(projectId, seq, request));
    }

    /** 404-R1（BSM:1300「确需补录的一律须附证据（照片／录音／录像等）并经主管复核后方生效」）：点位缺扫码补录，进主管复核，通过后才落走点行。 */
    @PostMapping("/{seq}/backfill")
    public ApiResponse<ManualBackfillService.Backfill> backfill(@PathVariable("id") String projectId, @PathVariable("seq") int seq,
            @RequestBody(required = false) ManualBackfillService.Request request) {
        ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(backfills.submit(AmusementInspectionPointBackfillTarget.TYPE, projectId, String.valueOf(seq), request));
    }
}
