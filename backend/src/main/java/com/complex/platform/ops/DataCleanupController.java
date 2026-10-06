package com.complex.platform.ops;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 集中数据清理工具接口（Tier-D）。
 * 权限：发起+预览 data.cleanup.request（high）；审批+执行 data.cleanup.approve（critical，职责分离）。
 */
@RestController
@RequestMapping("/api/v1/ops/data-cleanup")
public class DataCleanupController {

    private final DataCleanupService service;
    private final AuthorizationService authorizationService;

    public DataCleanupController(DataCleanupService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/jobs")
    public ApiResponse<List<DataCleanupJobRecord>> list(
            @RequestParam(value = "status", required = false) String status) {
        authorizationService.requirePermission("data.cleanup.request");
        return ApiResponse.ok(service.list(status));
    }

    @PostMapping("/preview")
    @com.complex.platform.access.DoesNotConsumeSingleUseGrant // M723：清理预览只统计与取样、不删数据（落一条试运行台账），提交申请才扣；预览不扣单次授权（操作员 2026-10-03）
    public ApiResponse<DataCleanupJobRecord> preview(
            @RequestBody(required = false) DataCleanupPreviewRequest request) {
        authorizationService.requirePermission("data.cleanup.request");
        return ApiResponse.ok(service.preview(request));
    }

    @PostMapping("/jobs/{id}/submit")
    public ApiResponse<DataCleanupJobRecord> submit(
            @PathVariable("id") String id, @RequestBody(required = false) DataCleanupSubmitRequest request) {
        authorizationService.requirePermission("data.cleanup.request");
        return ApiResponse.ok(service.submitForApproval(id, request));
    }

    @PostMapping("/jobs/{id}/approve")
    public ApiResponse<DataCleanupJobRecord> approve(
            @PathVariable("id") String id, @RequestBody(required = false) DataCleanupApprovalRequest request) {
        authorizationService.requirePermission("data.cleanup.approve");
        return ApiResponse.ok(service.approveAndExecute(id, request));
    }
}
