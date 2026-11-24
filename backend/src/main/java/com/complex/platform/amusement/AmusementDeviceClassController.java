package com.complex.platform.amusement;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiRequests;
import com.complex.platform.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第十八批 CF 段第 407 条：游乐设备分级申请与历史（407-R1，见 {@link AmusementDeviceClassService}）、停用设备与辅助设备停用后能否继续运行的判定
 * （407-R2，见 {@link AmusementDeviceContinueJudgementService}）。设备建档与列表仍在 AmusementController。
 */
@RestController
@RequestMapping("/api/v1/amusement/projects/{id}/devices")
public class AmusementDeviceClassController {

    private final AmusementDeviceClassService classes;
    private final AmusementDeviceContinueJudgementService judgements;

    public AmusementDeviceClassController(AmusementDeviceClassService classes, AmusementDeviceContinueJudgementService judgements) {
        this.classes = classes;
        this.judgements = judgements;
    }

    @GetMapping("/classification-changes")
    public ApiResponse<List<AmusementDeviceClassService.Change>> history(@PathVariable("id") String projectId) {
        return ApiResponse.ok(classes.history(projectId));
    }

    @PostMapping("/{deviceId}/classification-changes")
    public ApiResponse<AmusementDeviceClassService.Change> requestChange(@PathVariable("id") String projectId,
            @PathVariable("deviceId") String deviceId, @RequestBody(required = false) AmusementDeviceClassService.Request request) {
        ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(classes.request(projectId, deviceId, request));
    }

    @GetMapping("/stops")
    public ApiResponse<List<AmusementDeviceStopScope.Stop>> stops(@PathVariable("id") String projectId) {
        return ApiResponse.ok(judgements.stops(projectId));
    }

    @PostMapping("/{deviceId}/continue-judgements")
    public ApiResponse<AmusementDeviceStopScope.Judgement> judge(@PathVariable("id") String projectId,
            @PathVariable("deviceId") String deviceId, @RequestBody(required = false) AmusementDeviceContinueJudgementService.Request request) {
        ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(judgements.judge(projectId, deviceId, request));
    }
}
