package com.complex.platform.finance;

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.finance.dto.RefundAuthorityLimitRequest;
import com.complex.platform.finance.dto.RefundAutoRuleRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 退款分级授权配置 API（Stage C）：平台管理员维护员工自助退款额度 + 自动放行规则。
 * view 权限看配置、manage 权限改配置（敏感：决定谁能退多少），仿 IntegrationController 范式。
 */
@RestController
@RequestMapping("/api/v1/finance/refund-authority")
public class RefundAuthorizationController {

    private static final String VIEW_PERMISSION = "refund.authority.view";
    private static final String MANAGE_PERMISSION = "refund.authority.manage";

    private final RefundAuthorizationConfigService service;
    private final AuthorizationService authorizationService;
    private final com.complex.platform.evidence.AuditedOperationExecutor operations;

    // 第十八批 N 段第 124 条 124-R4（PGM L738）：额度与规则写口任一步失败（权限、幂等键、校验）经 AuditedOperationExecutor.recordingFailure
    // 落 OpenAPI 声明的 refund_authority.change_failed 再原样抛出；成功事件照旧由服务层写。
    public RefundAuthorizationController(
            RefundAuthorizationConfigService service, AuthorizationService authorizationService,
            com.complex.platform.evidence.AuditedOperationExecutor operations) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.operations = operations;
    }

    @GetMapping("/limits")
    public ApiResponse<PageResponse<RefundAuthorityLimitRecord>> listLimits(
            @RequestParam(name = "project_id", required = false) String projectId) {
        authorizationService.requirePermission(VIEW_PERMISSION);
        // B1d② 可按综合体过滤：传 project_id → 该综合体 + 全局；不传 → 全部（平台口径）。
        return ApiResponse.ok(service.listLimits(projectId));
    }

    @PostMapping("/limits")
    public ApiResponse<RefundAuthorityLimitRecord> upsertLimit(@RequestBody @jakarta.validation.Valid RefundAuthorityLimitRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("refund_authority.change_failed", MANAGE_PERMISSION, "refund_authority_limit", null,
                key, () -> {
                    authorizationService.requirePermission(MANAGE_PERMISSION);
                    requireIdempotencyKey(key);
                    return service.upsertLimit(request);
                }));
    }

    @GetMapping("/rules")
    public ApiResponse<PageResponse<RefundAutoRuleRecord>> listRules(
            @RequestParam(name = "project_id", required = false) String projectId) {
        authorizationService.requirePermission(VIEW_PERMISSION);
        return ApiResponse.ok(service.listRules(projectId));
    }

    @PostMapping("/rules")
    public ApiResponse<RefundAutoRuleRecord> upsertRule(@RequestBody @jakarta.validation.Valid RefundAutoRuleRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("refund_authority.change_failed", MANAGE_PERMISSION, "refund_auto_rule", null,
                key, () -> {
                    authorizationService.requirePermission(MANAGE_PERMISSION);
                    requireIdempotencyKey(key);
                    return service.upsertRule(request);
                }));
    }
}
