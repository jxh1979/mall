package com.complex.platform.governance;
// 文件版本：V0.01
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.PhoneMask;
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
    // D1：场地营业时间配置（V226 建表后一直无界面，场次划分全靠它）。
    private final VenueBusinessHoursService venueBusinessHoursService;

    public PlatformController(PlatformService service, AuthorizationService authorizationService,
                              VenueBusinessHoursService venueBusinessHoursService) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.venueBusinessHoursService = venueBusinessHoursService;
    }

    @GetMapping("/business-entities")
    public ApiResponse<PageResponse<BusinessEntityRecord>> listBusinessEntities(
            @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
            @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
        authorizationService.requirePermission("entity.binding.view");
        // M009-015：治理读端与写端同法 fail-closed（BSM-072/BSM-074/PGM-027：商户不能看平台全局和其他商户数据）。
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(service.listBusinessEntities(page, size));
    }

    @PostMapping("/business-entities")
    public ApiResponse<BusinessEntityRecord> createBusinessEntity(@RequestBody @jakarta.validation.Valid BusinessEntityRequest request) {
        authorizationService.requirePermission("entity.binding.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createBusinessEntity(request));
    }

    @GetMapping("/merchants")
    public ApiResponse<PageResponse<MerchantRecord>> listMerchants(
            @org.springframework.web.bind.annotation.RequestParam(name = "includeArchived", required = false, defaultValue = "false") boolean includeArchived,
            // V348+V384 按业态查询：merchantType=bar|catering|retail|amusement|media|other，缺省＝全部业态。
            @org.springframework.web.bind.annotation.RequestParam(name = "merchantType", required = false) String merchantType,
            @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
            @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
        authorizationService.requirePermission("merchant.profile.view");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(maskPhonesForUnauthorized(service.listMerchants(includeArchived, merchantType, page, size)));
    }

    @PostMapping("/merchants")
    public ApiResponse<MerchantRecord> createMerchant(@RequestBody @jakarta.validation.Valid MerchantProfileRequest request) {
        authorizationService.requirePermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskPhonesForUnauthorized(service.createMerchant(request)));
    }

    @PutMapping("/merchants/{merchant_id}")
    public ApiResponse<MerchantRecord> updateMerchant(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody @jakarta.validation.Valid MerchantProfileRequest request
    ) {
        authorizationService.requirePermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskPhonesForUnauthorized(service.updateMerchant(merchantId, request)));
    }

    // ================= V260 商户存档与审批后删除范式 =================

    @PostMapping("/merchants/{merchant_id}/archive")
    public ApiResponse<MerchantRecord> archiveMerchant(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody @jakarta.validation.Valid com.complex.platform.governance.dto.MerchantArchiveRequest request) {
        authorizationService.requirePermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskPhonesForUnauthorized(service.archiveMerchant(merchantId, request)));
    }

    @PostMapping("/merchants/{merchant_id}/unarchive")
    public ApiResponse<MerchantRecord> unarchiveMerchant(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody @jakarta.validation.Valid com.complex.platform.governance.dto.MerchantArchiveRequest request) {
        authorizationService.requirePermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskPhonesForUnauthorized(service.unarchiveMerchant(merchantId, request)));
    }

    @PostMapping("/merchants/{merchant_id}/delete-request")
    public ApiResponse<MerchantRecord> requestDeleteMerchant(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody @jakarta.validation.Valid com.complex.platform.governance.dto.MerchantDeleteRequest request) {
        authorizationService.requirePermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskPhonesForUnauthorized(service.requestDeleteMerchant(merchantId, request)));
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
    public ApiResponse<PageResponse<ProjectRecord>> listProjects(
            @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
            @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
        authorizationService.requirePermission("governance.project.view");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(service.listProjects(page, size));
    }

    @PostMapping("/projects")
    public ApiResponse<ProjectRecord> createProject(@RequestBody @jakarta.validation.Valid ProjectRequest request) {
        authorizationService.requirePermission("governance.project.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createProject(request));
    }

    @GetMapping("/spaces")
    public ApiResponse<PageResponse<SpaceRecord>> listSpaces(
            @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
            @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
        authorizationService.requirePermission("governance.space.view");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(service.listSpaces(page, size));
    }

    @GetMapping("/space-types")
    public ApiResponse<PageResponse<SpaceTypeRecord>> listSpaceTypes() {
        authorizationService.requirePermission("governance.space.view");
        authorizationService.requirePlatformScope();
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

    // D1 场地营业时间：V226 的 venue_business_hours 此前只有核销侧读、没有任何配置入口（只能改库）。
    //   入场资格/当天无限权益的场次全按这张表划，配错=整个场地当天核销被拒，必须有后台配置面。
    @GetMapping("/venue-business-hours")
    public ApiResponse<PageResponse<VenueBusinessHoursRecord>> listVenueBusinessHours() {
        authorizationService.requirePermission("governance.space.view");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(PageResponse.of(venueBusinessHoursService.list(), 1, 500));
    }

    @PostMapping("/venue-business-hours")
    public ApiResponse<VenueBusinessHoursRecord> saveVenueBusinessHours(
            @RequestBody @jakarta.validation.Valid com.complex.platform.governance.dto.VenueBusinessHoursRequest request) {
        authorizationService.requirePermission("governance.space.manage");
        authorizationService.requirePlatformScope();
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(venueBusinessHoursService.save(request));
    }

    @GetMapping("/business-entity-bindings")
    public ApiResponse<PageResponse<BusinessEntityBindingRecord>> listBusinessEntityBindings(
            @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
            @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
        authorizationService.requirePermission("entity.binding.view");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(service.listBusinessEntityBindings(page, size));
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

    // ── P0-A/GAP-07 联系人手机号展示脱敏（V339）──────────────────────────────
    // 业务 HTML「员工日常界面只显示脱敏号码，不见明文」：无 pii.contact_phone.view 的读端一律看掩码。
    // 脱敏放在 controller（service 外），与 V279 内部结算价同款范式——避免脱敏值污染幂等缓存后
    // 被有权限者重放读到掩码。

    private boolean canViewPhonePlaintext() {
        return authorizationService.currentActorHasPermission(PhoneMask.VIEW_PLAINTEXT_PERMISSION);
    }

    private MerchantRecord maskPhonesForUnauthorized(MerchantRecord merchant) {
        return canViewPhonePlaintext() || merchant == null ? merchant : merchant.withMaskedPhones();
    }

    private PageResponse<MerchantRecord> maskPhonesForUnauthorized(PageResponse<MerchantRecord> page) {
        if (canViewPhonePlaintext()) {
            return page;
        }
        return new PageResponse<>(page.items().stream().map(MerchantRecord::withMaskedPhones).toList(),
                page.page(), page.pageSize(), page.total());
    }
}
