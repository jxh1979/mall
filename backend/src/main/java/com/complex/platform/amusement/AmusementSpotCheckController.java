package com.complex.platform.amusement;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiRequests;
import com.complex.platform.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 第十八批 BS 段第 373 条 373-R2：营业中抽检间隔（按综合体项目）与当班未抽检记录，见 {@link AmusementSpotCheckService}。 */
@RestController
@RequestMapping("/api/v1/amusement")
public class AmusementSpotCheckController {

    private final AmusementSpotCheckService service;

    public AmusementSpotCheckController(AmusementSpotCheckService service) {
        this.service = service;
    }

    @GetMapping("/spot-check-policies/{project_id}")
    public ApiResponse<AmusementSpotCheckService.Policy> getPolicy(@PathVariable("project_id") String projectId) {
        return ApiResponse.ok(service.getPolicy(projectId));
    }

    @PutMapping("/spot-check-policies/{project_id}")
    public ApiResponse<AmusementSpotCheckService.Policy> savePolicy(@PathVariable("project_id") String projectId,
            @RequestBody(required = false) AmusementSpotCheckService.PolicyRequest request) {
        ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.savePolicy(projectId, request));
    }

    @GetMapping("/spot-check-misses")
    public ApiResponse<List<AmusementSpotCheckService.Miss>> listMisses() {
        return ApiResponse.ok(service.listMisses());
    }
}
