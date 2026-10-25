package com.complex.platform.bar;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiRequests;
import com.complex.platform.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 第十四批 OBS-13-03-R3 最晚到店时间前提醒：后台按项目配提前分钟数（值与上下限同配，见 {@link BarArrivalReminderService}）。 */
@RestController
public class BarArrivalReminderController {

    private final BarArrivalReminderService service;

    public BarArrivalReminderController(BarArrivalReminderService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/bar/arrival-reminder-policies/{project_id}")
    public ApiResponse<BarArrivalReminderService.Policy> getPolicy(@PathVariable("project_id") String projectId) {
        return ApiResponse.ok(service.getPolicy(projectId));
    }

    @PutMapping("/api/v1/bar/arrival-reminder-policies/{project_id}")
    public ApiResponse<BarArrivalReminderService.Policy> savePolicy(@PathVariable("project_id") String projectId,
            @RequestBody BarArrivalReminderService.PolicyRequest request) {
        ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.savePolicy(projectId, request));
    }
}
