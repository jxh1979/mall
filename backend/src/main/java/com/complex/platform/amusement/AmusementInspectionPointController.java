package com.complex.platform.amusement;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiRequests;
import com.complex.platform.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 第十八批 BS 段第 373 条 373-R1、373-R5：游乐项目巡检点位清单（读、整份替换）与按序扫码走点，见 {@link AmusementInspectionPointService}。 */
@RestController
@RequestMapping("/api/v1/amusement/projects/{id}/inspection-points")
public class AmusementInspectionPointController {

    private final AmusementInspectionPointService service;

    public AmusementInspectionPointController(AmusementInspectionPointService service) {
        this.service = service;
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
}
