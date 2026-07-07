package com.complex.platform.governance;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.governance.dto.BusinessEntityBindingRequest;
import com.complex.platform.governance.dto.BusinessEntityRequest;
import com.complex.platform.governance.dto.MerchantProfileRequest;
import com.complex.platform.governance.dto.ProjectRequest;
import com.complex.platform.governance.dto.SpaceRequest;
import com.complex.platform.governance.dto.StatusChangeRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform")
public class PlatformController {

    private final PlatformService service;
    private final AuthorizationService authorizationService;

    public PlatformController(PlatformService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/business-entities")
    public ApiResponse<PageResponse<BusinessEntityRecord>> listBusinessEntities() {
        authorizationService.requirePermission("entity.binding.view");
        return ApiResponse.ok(service.listBusinessEntities());
    }

    @PostMapping("/business-entities")
    public ApiResponse<BusinessEntityRecord> createBusinessEntity(@RequestBody @jakarta.validation.Valid BusinessEntityRequest request) {
        authorizationService.requirePermission("entity.binding.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createBusinessEntity(request));
    }

    @GetMapping("/merchants")
    public ApiResponse<PageResponse<MerchantRecord>> listMerchants(
            @org.springframework.web.bind.annotation.RequestParam(name = "includeArchived", required = false, defaultValue = "false") boolean includeArchived) {
        authorizationService.requirePermission("merchant.profile.view");
        return ApiResponse.ok(service.listMerchants(includeArchived));
    }

    @PostMapping("/merchants")
    public ApiResponse<MerchantRecord> createMerchant(@RequestBody @jakarta.validation.Valid MerchantProfileRequest request) {
        authorizationService.requirePermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createMerchant(request));
    }

    @PutMapping("/merchants/{merchant_id}")
    public ApiResponse<MerchantRecord> updateMerchant(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody @jakarta.validation.Valid MerchantProfileRequest request
    ) {
        authorizationService.requirePermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.updateMerchant(merchantId, request));
    }

    // ================= V260 商户存档与审批后删除范式 =================

    @PostMapping("/merchants/{merchant_id}/archive")
    public ApiResponse<MerchantRecord> archiveMerchant(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody @jakarta.validation.Valid com.complex.platform.governance.dto.MerchantArchiveRequest request) {
        authorizationService.requirePermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.archiveMerchant(merchantId, request));
    }

    @PostMapping("/merchants/{merchant_id}/unarchive")
    public ApiResponse<MerchantRecord> unarchiveMerchant(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody @jakarta.validation.Valid com.complex.platform.governance.dto.MerchantArchiveRequest request) {
        authorizationService.requirePermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.unarchiveMerchant(merchantId, request));
    }

    @PostMapping("/merchants/{merchant_id}/delete-request")
    public ApiResponse<MerchantRecord> requestDeleteMerchant(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody @jakarta.validation.Valid com.complex.platform.governance.dto.MerchantDeleteRequest request) {
        authorizationService.requirePermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.requestDeleteMerchant(merchantId, request));
    }

    @PostMapping("/merchants/{merchant_id}/delete-approve")
    public ApiResponse<MerchantDeleteDecision> approveDeleteMerchant(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody @jakarta.validation.Valid com.complex.platform.governance.dto.MerchantDeleteApprovalRequest request) {
        authorizationService.requirePermission("merchant.profile.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.approveDeleteMerchant(merchantId, request));
    }

    @GetMapping("/projects")
    public ApiResponse<PageResponse<ProjectRecord>> listProjects() {
        authorizationService.requirePermission("governance.project.view");
        return ApiResponse.ok(service.listProjects());
    }

    @PostMapping("/projects")
    public ApiResponse<ProjectRecord> createProject(@RequestBody @jakarta.validation.Valid ProjectRequest request) {
        authorizationService.requirePermission("governance.project.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createProject(request));
    }

    @GetMapping("/spaces")
    public ApiResponse<PageResponse<SpaceRecord>> listSpaces() {
        authorizationService.requirePermission("governance.space.view");
        return ApiResponse.ok(service.listSpaces());
    }

    @GetMapping("/space-types")
    public ApiResponse<PageResponse<SpaceTypeRecord>> listSpaceTypes() {
        authorizationService.requirePermission("governance.space.view");
        return ApiResponse.ok(service.listSpaceTypes());
    }

    @PostMapping("/space-types")
    public ApiResponse<SpaceTypeRecord> createSpaceType(
            @RequestBody @jakarta.validation.Valid com.complex.platform.governance.dto.SpaceTypeRequest request) {
        authorizationService.requirePermission("governance.space.manage");
        return ApiResponse.ok(service.createSpaceType(request));
    }

    @PostMapping("/spaces")
    public ApiResponse<SpaceRecord> createSpace(@RequestBody @jakarta.validation.Valid SpaceRequest request) {
        authorizationService.requirePermission("governance.space.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createSpace(request));
    }

    @GetMapping("/business-entity-bindings")
    public ApiResponse<PageResponse<BusinessEntityBindingRecord>> listBusinessEntityBindings() {
        authorizationService.requirePermission("entity.binding.view");
        return ApiResponse.ok(service.listBusinessEntityBindings());
    }

    @PostMapping("/business-entity-bindings")
    public ApiResponse<BusinessEntityBindingRecord> createBusinessEntityBinding(
            @RequestBody @jakarta.validation.Valid BusinessEntityBindingRequest request
    ) {
        authorizationService.requirePermission("entity.binding.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createBusinessEntityBinding(request));
    }

    @PostMapping("/business-entity-bindings/{id}/change")
    public ApiResponse<BusinessEntityBindingRecord> changeBusinessEntityBinding(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid StatusChangeRequest request
    ) {
        authorizationService.requirePermission("entity.binding.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.changeBusinessEntityBinding(id, request));
    }



}
