package com.complex.platform.partner;

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.partner.dto.ExternalPartnerOrgRequest;
import com.complex.platform.partner.dto.PartnerSubordinateRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 条目2 外部协作机构与自助下属门户。机构 CRUD=平台管理员；下属管理=机构管理员自助。 */
@RestController
@RequestMapping("/api/v1/partner")
public class PartnerController {

    private final PartnerService service;
    private final AuthorizationService authorizationService;
    private final PartnerSubordinateCompletionService completionService;

    public PartnerController(PartnerService service, AuthorizationService authorizationService,
            PartnerSubordinateCompletionService completionService) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.completionService = completionService;
    }

    @GetMapping("/orgs")
    public ApiResponse<PageResponse<ExternalPartnerOrgRecord>> listOrgs() {
        authorizationService.requirePermission("partner.org.view");
        return ApiResponse.ok(service.listPartnerOrgs());
    }

    @PostMapping("/orgs")
    public ApiResponse<ExternalPartnerOrgRecord> createOrg(
            @RequestBody @jakarta.validation.Valid ExternalPartnerOrgRequest request) {
        authorizationService.requirePermission("partner.org.manage");
        return ApiResponse.ok(service.createPartnerOrg(request));
    }

    /** AUD-033：平台把既有后台账号绑到外部协作机构（外包模块按机构过滤的前置）。 */
    @PostMapping("/orgs/{org_id}/bind-user")
    public ApiResponse<Void> bindUserToOrg(
            @PathVariable("org_id") String orgId,
            @RequestBody @jakarta.validation.Valid com.complex.platform.partner.dto.PartnerOrgUserBindRequest request) {
        authorizationService.requirePermission("partner.org.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        service.bindUserToOrg(orgId, request.userId());
        return ApiResponse.ok(null);
    }

    @GetMapping("/subordinates")
    public ApiResponse<PageResponse<PartnerSubordinateRecord>> listSubordinates() {
        authorizationService.requirePermission("partner.subordinate.view");
        return ApiResponse.ok(service.listMySubordinates());
    }

    /** 机构查看本机构下属工作完成情况（治理 PRD §4，用户 2026-07 拍板要建）。 */
    @GetMapping("/subordinates/completion")
    public ApiResponse<PageResponse<PartnerSubordinateCompletionRecord>> listSubordinateCompletion() {
        authorizationService.requirePermission("partner.subordinate.view");
        return ApiResponse.ok(completionService.listMySubordinateCompletion());
    }

    @PostMapping("/subordinates")
    public ApiResponse<PartnerSubordinateRecord> createSubordinate(
            @RequestBody @jakarta.validation.Valid PartnerSubordinateRequest request) {
        authorizationService.requirePermission("partner.subordinate.manage");
        return ApiResponse.ok(service.createSubordinate(request));
    }

    @PostMapping("/subordinates/{user_id}/disable")
    public ApiResponse<PartnerSubordinateRecord> disableSubordinate(@PathVariable("user_id") String userId) {
        authorizationService.requirePermission("partner.subordinate.manage");
        return ApiResponse.ok(service.disableSubordinate(userId));
    }
}
