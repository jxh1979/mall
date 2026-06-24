package com.complex.platform.governance;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.governance.dto.ApprovalRequest;
import com.complex.platform.governance.dto.ContractActivateRequest;
import com.complex.platform.governance.dto.BusinessEntityBindingRequest;
import com.complex.platform.governance.dto.BusinessEntityRequest;
import com.complex.platform.governance.dto.ContractDraftRequest;
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
    public ApiResponse<BusinessEntityRecord> createBusinessEntity(@RequestBody BusinessEntityRequest request) {
        authorizationService.requirePermission("entity.binding.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createBusinessEntity(request));
    }

    @GetMapping("/merchants")
    public ApiResponse<PageResponse<MerchantRecord>> listMerchants() {
        authorizationService.requirePermission("merchant.profile.view");
        return ApiResponse.ok(service.listMerchants());
    }

    @PostMapping("/merchants")
    public ApiResponse<MerchantRecord> createMerchant(@RequestBody MerchantProfileRequest request) {
        authorizationService.requirePermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createMerchant(request));
    }

    @PutMapping("/merchants/{merchant_id}")
    public ApiResponse<MerchantRecord> updateMerchant(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody MerchantProfileRequest request
    ) {
        authorizationService.requirePermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.updateMerchant(merchantId, request));
    }

    @GetMapping("/projects")
    public ApiResponse<PageResponse<ProjectRecord>> listProjects() {
        authorizationService.requirePermission("governance.project.view");
        return ApiResponse.ok(service.listProjects());
    }

    @PostMapping("/projects")
    public ApiResponse<ProjectRecord> createProject(@RequestBody ProjectRequest request) {
        authorizationService.requirePermission("governance.project.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createProject(request));
    }

    @GetMapping("/spaces")
    public ApiResponse<PageResponse<SpaceRecord>> listSpaces() {
        authorizationService.requirePermission("governance.space.view");
        return ApiResponse.ok(service.listSpaces());
    }

    @PostMapping("/spaces")
    public ApiResponse<SpaceRecord> createSpace(@RequestBody SpaceRequest request) {
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
            @RequestBody BusinessEntityBindingRequest request
    ) {
        authorizationService.requirePermission("entity.binding.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createBusinessEntityBinding(request));
    }

    @PostMapping("/business-entity-bindings/{id}/change")
    public ApiResponse<BusinessEntityBindingRecord> changeBusinessEntityBinding(
            @PathVariable String id,
            @RequestBody StatusChangeRequest request
    ) {
        authorizationService.requirePermission("entity.binding.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.changeBusinessEntityBinding(id, request));
    }

    @GetMapping("/contracts")
    public ApiResponse<PageResponse<ContractRecord>> listContracts() {
        authorizationService.requirePermission("contract.view");
        return ApiResponse.ok(service.listContracts());
    }

    @PostMapping("/contracts")
    public ApiResponse<ContractRecord> createContractDraft(@RequestBody ContractDraftRequest request) {
        authorizationService.requirePermission("contract.draft.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createContractDraft(request));
    }

    @PostMapping("/contracts/{contract_id}/approve")
    public ApiResponse<ContractRecord> approveContract(
            @PathVariable("contract_id") String contractId,
            @RequestBody ApprovalRequest request
    ) {
        authorizationService.requirePermission("contract.lifecycle.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.approveContract(contractId, request));
    }

    @PostMapping("/contracts/{contract_id}/activate")
    public ApiResponse<ContractRecord> activateContract(
            @PathVariable("contract_id") String contractId,
            @RequestBody ContractActivateRequest request
    ) {
        authorizationService.requirePermission("contract.lifecycle.activate");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.activateContract(contractId, request));
    }

    @GetMapping("/contracts/{contract_id}/spaces")
    public ApiResponse<PageResponse<ContractSpaceRecord>> listContractSpaces(
            @PathVariable("contract_id") String contractId
    ) {
        authorizationService.requirePermission("contract.view");
        return ApiResponse.ok(service.listContractSpaces(contractId));
    }
}
