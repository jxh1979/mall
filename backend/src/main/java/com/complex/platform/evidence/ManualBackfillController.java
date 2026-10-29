package com.complex.platform.evidence;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiRequests;
import com.complex.platform.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 第十八批 F 段第 14 条 14-R1：人工补录参数（按项目）与某对象的补录记录查询；提交入口由各对象模块提供（见 {@link ManualBackfillService}）。 */
@RestController
public class ManualBackfillController {

    private final ManualBackfillService service;

    public ManualBackfillController(ManualBackfillService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/evidence/manual-backfill-policies/{project_id}")
    public ApiResponse<ManualBackfillService.Policy> getPolicy(@PathVariable("project_id") String projectId) {
        return ApiResponse.ok(service.getPolicy(projectId));
    }

    @PutMapping("/api/v1/evidence/manual-backfill-policies/{project_id}")
    public ApiResponse<ManualBackfillService.Policy> savePolicy(@PathVariable("project_id") String projectId,
            @RequestBody ManualBackfillService.PolicyRequest request) {
        ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.savePolicy(projectId, request));
    }

    @GetMapping("/api/v1/evidence/manual-backfills")
    public ApiResponse<List<ManualBackfillService.Backfill>> list(@RequestParam("target_type") String targetType,
            @RequestParam("target_id") String targetId) {
        return ApiResponse.ok(service.list(targetType, targetId));
    }
}
