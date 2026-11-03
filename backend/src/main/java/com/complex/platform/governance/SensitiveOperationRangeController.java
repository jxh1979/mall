package com.complex.platform.governance;

// 文件版本：V0.01

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.governance.dto.SensitiveOperationRangeRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 第 123 条 123-R1：项目敏感操作授权范围（BSM:1630「授权范围按项目在后台配置」）；权限与项目范围在服务层校验。 */
@RestController
@RequestMapping("/api/v1/platform/projects/{project_id}/sensitive-operation-range")
public class SensitiveOperationRangeController {

    private final SensitiveOperationRangeService service;

    public SensitiveOperationRangeController(SensitiveOperationRangeService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<SensitiveOperationRangeRecord> get(@PathVariable("project_id") String projectId) {
        return ApiResponse.ok(service.get(projectId));
    }

    @PutMapping
    public ApiResponse<SensitiveOperationRangeRecord> save(@PathVariable("project_id") String projectId,
            @RequestBody SensitiveOperationRangeRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.save(projectId, request));
    }
}
