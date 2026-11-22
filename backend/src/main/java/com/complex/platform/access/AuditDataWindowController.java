package com.complex.platform.access;

// 文件版本：V0.01

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.dto.AuditDataWindowRequest;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditedOperationExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第十八批 BX 段第 372 条 372-R1（PGM:682「外部审计/监管只能查看划定的范围与时间段」；第十八批裁决 77「开外审权限必须划定数据起止日期」）：
 * 外审数据时段的划定（setAuditDataWindow）与查看（listAuditDataWindows）。写口任一步失败落 access.audit_data_window.set_failed 再原样抛出。
 */
@RestController
@RequestMapping("/api/v1/access")
public class AuditDataWindowController {

    private final AuditDataWindowService service;
    private final AuthorizationService authorizationService;
    private final AuditedOperationExecutor operations;

    public AuditDataWindowController(AuditDataWindowService service, AuthorizationService authorizationService,
            AuditedOperationExecutor operations) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.operations = operations;
    }

    @PutMapping("/users/{user_id}/audit-data-window")
    public ApiResponse<AuditDataWindowRecord> setAuditDataWindow(@PathVariable("user_id") String userId,
            @RequestBody AuditDataWindowRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("access.audit_data_window.set_failed", "access.user.manage", "access_user", userId,
                key, () -> {
                    authorizationService.requirePermission("access.user.manage");
                    requireIdempotencyKey(key);
                    return service.set(userId, request);
                }));
    }

    @GetMapping("/audit-data-windows")
    public ApiResponse<PageResponse<AuditDataWindowRecord>> listAuditDataWindows() {
        authorizationService.requirePermission("access.user.view");
        return ApiResponse.ok(service.list());
    }
}
