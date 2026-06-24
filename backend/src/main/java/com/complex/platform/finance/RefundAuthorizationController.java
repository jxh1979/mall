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

    public RefundAuthorizationController(
            RefundAuthorizationConfigService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/limits")
    public ApiResponse<PageResponse<RefundAuthorityLimitRecord>> listLimits() {
        authorizationService.requirePermission(VIEW_PERMISSION);
        return ApiResponse.ok(service.listLimits());
    }

    @PostMapping("/limits")
    public ApiResponse<RefundAuthorityLimitRecord> upsertLimit(@RequestBody RefundAuthorityLimitRequest request) {
        authorizationService.requirePermission(MANAGE_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.upsertLimit(request));
    }

    @GetMapping("/rules")
    public ApiResponse<PageResponse<RefundAutoRuleRecord>> listRules() {
        authorizationService.requirePermission(VIEW_PERMISSION);
        return ApiResponse.ok(service.listRules());
    }

    @PostMapping("/rules")
    public ApiResponse<RefundAutoRuleRecord> upsertRule(@RequestBody RefundAutoRuleRequest request) {
        authorizationService.requirePermission(MANAGE_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.upsertRule(request));
    }
}
