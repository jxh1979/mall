package com.complex.platform.governance;

// 文件版本：V0.01

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.governance.dto.MerchantOperatingProjectRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 第十八批 E 段第 20 条 20-R1：商户档案里的经营项目（治理地图第 659 行「在商户档案里加经营项目，要经过审批才生效」）。 */
@RestController
@RequestMapping("/api/v1/platform/merchants/{merchant_id}/operating-projects")
public class MerchantOperatingProjectController {

    private final MerchantOperatingProjectService service;
    private final AuthorizationService authorizationService;

    public MerchantOperatingProjectController(MerchantOperatingProjectService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ApiResponse<MerchantOperatingProjectService.OperatingProjects> view(@PathVariable("merchant_id") String merchantId) {
        authorizationService.requirePlatformScope();
        authorizationService.requirePermission(MerchantOperatingProjectService.VIEW);
        return ApiResponse.ok(service.view(merchantId));
    }

    @PostMapping
    public ApiResponse<MerchantOperatingProjectService.ProjectRequestRow> request(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody @jakarta.validation.Valid MerchantOperatingProjectRequest request) {
        authorizationService.requirePlatformScope();
        authorizationService.requirePermission(MerchantOperatingProjectService.MANAGE);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.request(merchantId, request));
    }
}
