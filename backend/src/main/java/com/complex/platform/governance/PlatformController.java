package com.complex.platform.governance;
// 文件版本：V0.09
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.PhoneMask;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotentReplayContext;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.governance.dto.BusinessEntityBindingRequest;
import com.complex.platform.governance.dto.BusinessEntityRequest;
import com.complex.platform.governance.dto.MerchantProfileRequest;
import com.complex.platform.governance.dto.ProjectRequest;
import com.complex.platform.governance.dto.ProjectAssociationRequest;
import com.complex.platform.governance.dto.SpaceRequest;
import com.complex.platform.governance.dto.StatusChangeRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final AuditedOperationExecutor operations;

    // 第十八批 N 段第 124 条 124-R5（PGM L738「访问被拒绝也要留痕，便于排查是权限问题还是开关问题。」）：写口任一步失败（权限、范围、校验、
    // 状态冲突）经 AuditedOperationExecutor.recordingFailure 落 OpenAPI 声明的 x-audit-event-failure 再原样抛出。
    public PlatformController(PlatformService service, AuthorizationService authorizationService,
                              VenueBusinessHoursService venueBusinessHoursService, AuditedOperationExecutor operations) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.venueBusinessHoursService = venueBusinessHoursService;
        this.operations = operations;
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
    // M723：看商户列表（脱敏）不扣单次授权；明文手机号查看看一次扣一次（M009-058；操作员 2026-10-03）
    @com.complex.platform.access.ConsumesSingleUseGrant(PhoneMask.VIEW_PLAINTEXT_PERMISSION)
    public ApiResponse<PageResponse<MerchantRecord>> listMerchants(
            @org.springframework.web.bind.annotation.RequestParam(name = "includeArchived", required = false, defaultValue = "false") boolean includeArchived,
            // V348+V384 按业态查询：merchantType=bar|catering|retail|amusement|media|other，缺省＝全部业态。
            @org.springframework.web.bind.annotation.RequestParam(name = "merchantType", required = false) String merchantType,
            @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
            @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
        String plaintextCode = requireMerchantPermission("merchant.profile.view");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(maskPhonesForUnauthorized(service.listMerchants(includeArchived, merchantType, page, size), plaintextCode));
    }

    @PostMapping("/merchants")
    public ApiResponse<MerchantRecord> createMerchant(@RequestBody @jakarta.validation.Valid MerchantProfileRequest request) {
        String plaintextCode = requireMerchantPermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskPhonesForUnauthorized(service.createMerchant(request), plaintextCode));
    }

    @PutMapping("/merchants/{merchant_id}")
    public ApiResponse<MerchantRecord> updateMerchant(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody @jakarta.validation.Valid MerchantProfileRequest request
    ) {
        String plaintextCode = requireMerchantPermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskPhonesForUnauthorized(service.updateMerchant(merchantId, request), plaintextCode));
    }

    @PutMapping("/merchants/{merchant_id}/project")
    public ApiResponse<MerchantRecord> assignMerchantProject(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody @jakarta.validation.Valid ProjectAssociationRequest request) {
        String plaintextCode = requireMerchantPermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskPhonesForUnauthorized(service.assignMerchantProject(merchantId, request), plaintextCode));
    }

    /** 第十批 TK-27（BSM L1058）：酒吧（商户）待清台提醒时限；未配置按出厂 15 分钟。 */
    @GetMapping("/merchants/{merchant_id}/pending-clear-alert")
    public ApiResponse<MerchantPendingClearAlertRecord> getPendingClearAlert(@PathVariable("merchant_id") String merchantId) {
        authorizationService.requirePermission("merchant.profile.view");
        return ApiResponse.ok(service.getPendingClearAlert(merchantId));
    }

    /** 第十批 TK-27：配置待清台提醒时限（值、下限、上限同填且值在闭区间；权限同商户资料写口 merchant.profile.manage）。 */
    @PutMapping("/merchants/{merchant_id}/pending-clear-alert")
    public ApiResponse<MerchantPendingClearAlertRecord> configurePendingClearAlert(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody com.complex.platform.governance.dto.MerchantPendingClearAlertRequest request) {
        authorizationService.requirePermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.configurePendingClearAlert(merchantId, request));
    }

    /** 第十八批 AB 段第 190 条 190-R1（BSM:1079）：酒吧（商户）转台次数上限；未配置按出厂 10 次。 */
    @GetMapping("/merchants/{merchant_id}/table-transfer-limit")
    public ApiResponse<MerchantTableTransferLimitRecord> getTableTransferLimit(@PathVariable("merchant_id") String merchantId) {
        authorizationService.requirePermission("merchant.profile.view");
        return ApiResponse.ok(service.getTableTransferLimit(merchantId));
    }

    /** 第十八批 AB 段第 190 条 190-R1：配置转台次数上限（值、下限、上限同填且值在闭区间；权限同商户资料写口 merchant.profile.manage）。 */
    @PutMapping("/merchants/{merchant_id}/table-transfer-limit")
    public ApiResponse<MerchantTableTransferLimitRecord> configureTableTransferLimit(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody com.complex.platform.governance.dto.MerchantTableTransferLimitRequest request) {
        authorizationService.requirePermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.configureTableTransferLimit(merchantId, request));
    }

    /** 第十八批 C 段第 15 条（BSM:1468）：酒吧员工代订爽约率上限；未配置不自动停用代订权限。 */
    @GetMapping("/merchants/{merchant_id}/staff-booking-no-show-limit")
    public ApiResponse<MerchantPercentParamRecord> getStaffBookingNoShowLimit(@PathVariable("merchant_id") String merchantId) {
        authorizationService.requirePermission("merchant.profile.view");
        return ApiResponse.ok(service.getPercentParam(merchantId, MerchantPercentParamRecord.Param.STAFF_BOOKING_NO_SHOW_LIMIT));
    }

    /** 第十八批 C 段第 15 条：配置员工代订爽约率上限（值、下限、上限同填，整数百分比；权限同商户资料写口）。 */
    @PutMapping("/merchants/{merchant_id}/staff-booking-no-show-limit")
    public ApiResponse<MerchantPercentParamRecord> configureStaffBookingNoShowLimit(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody com.complex.platform.governance.dto.MerchantPercentParamRequest request) {
        authorizationService.requirePermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.configurePercentParam(merchantId, MerchantPercentParamRecord.Param.STAFF_BOOKING_NO_SHOW_LIMIT, request));
    }

    /** 第十八批 C 段第 17 条（BSM:2015；第十八批裁决 4）：酒吧损耗率阈值；未配置按出厂 5%。 */
    @GetMapping("/merchants/{merchant_id}/pour-loss-rate")
    public ApiResponse<MerchantPercentParamRecord> getPourLossRate(@PathVariable("merchant_id") String merchantId) {
        authorizationService.requirePermission("merchant.profile.view");
        return ApiResponse.ok(service.getPercentParam(merchantId, MerchantPercentParamRecord.Param.POUR_LOSS_RATE));
    }

    /** 第十八批 C 段第 17 条：配置酒吧损耗率阈值（值、下限、上限同填，下限不小于 1；权限同商户资料写口）。 */
    @PutMapping("/merchants/{merchant_id}/pour-loss-rate")
    public ApiResponse<MerchantPercentParamRecord> configurePourLossRate(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody com.complex.platform.governance.dto.MerchantPercentParamRequest request) {
        authorizationService.requirePermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.configurePercentParam(merchantId, MerchantPercentParamRecord.Param.POUR_LOSS_RATE, request));
    }

    @PostMapping("/business-entities/{entity_id}/projects")
    public ApiResponse<BusinessEntityRecord> associateBusinessEntityProject(
            @PathVariable("entity_id") String entityId,
            @RequestBody @jakarta.validation.Valid ProjectAssociationRequest request) {
        authorizationService.requirePermission("entity.binding.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.associateBusinessEntityProject(entityId, request));
    }

    // ================= V260 商户存档与审批后删除范式 =================

    @PostMapping("/merchants/{merchant_id}/archive")
    public ApiResponse<MerchantRecord> archiveMerchant(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody @jakarta.validation.Valid com.complex.platform.governance.dto.MerchantArchiveRequest request) {
        String plaintextCode = requireMerchantPermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskPhonesForUnauthorized(service.archiveMerchant(merchantId, request), plaintextCode));
    }

    @PostMapping("/merchants/{merchant_id}/unarchive")
    public ApiResponse<MerchantRecord> unarchiveMerchant(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody @jakarta.validation.Valid com.complex.platform.governance.dto.MerchantArchiveRequest request) {
        String plaintextCode = requireMerchantPermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskPhonesForUnauthorized(service.unarchiveMerchant(merchantId, request), plaintextCode));
    }

    @PostMapping("/merchants/{merchant_id}/delete-request")
    public ApiResponse<MerchantRecord> requestDeleteMerchant(
            @PathVariable("merchant_id") String merchantId,
            @RequestBody @jakarta.validation.Valid com.complex.platform.governance.dto.MerchantDeleteRequest request) {
        String plaintextCode = requireMerchantPermission("merchant.profile.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskPhonesForUnauthorized(service.requestDeleteMerchant(merchantId, request), plaintextCode));
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

    @DeleteMapping("/space-types/{id}")
    public ApiResponse<Void> deleteSpaceType(@org.springframework.web.bind.annotation.PathVariable("id") String id) {
        authorizationService.requirePermission("governance.space.manage");
        service.deleteSpaceType(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/space-types/{id}/archive")
    public ApiResponse<Void> archiveSpaceType(
            @org.springframework.web.bind.annotation.PathVariable("id") String id,
            @RequestBody java.util.Map<String, String> body) {
        authorizationService.requirePermission("governance.space.manage");
        service.archiveSpaceType(id, body == null ? null : body.get("reason"));
        return ApiResponse.ok(null);
    }

    @PostMapping("/space-types/{id}/unarchive")
    public ApiResponse<Void> unarchiveSpaceType(@org.springframework.web.bind.annotation.PathVariable("id") String id) {
        authorizationService.requirePermission("governance.space.manage");
        service.unarchiveSpaceType(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/space-types/{id}/delete-request")
    public ApiResponse<Void> requestDeleteSpaceType(
            @org.springframework.web.bind.annotation.PathVariable("id") String id,
            @RequestBody java.util.Map<String, String> body) {
        authorizationService.requirePermission("governance.space.manage");
        service.requestDeleteSpaceType(id, body == null ? null : body.get("reason"));
        return ApiResponse.ok(null);
    }

    @PostMapping("/space-types/{id}/delete-approve")
    public ApiResponse<Void> approveDeleteSpaceType(
            @org.springframework.web.bind.annotation.PathVariable("id") String id,
            @RequestBody java.util.Map<String, String> body) {
        service.approveDeleteSpaceType(id, body == null ? null : body.get("decision"));
        return ApiResponse.ok(null);
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
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("venue_business_hours.update_failed", "governance.space.manage",
                "venue_business_hours", "new", key, () -> {
                    authorizationService.requirePermission("governance.space.manage");
                    authorizationService.requirePlatformScope();
                    requireIdempotencyKey(key);
                    return venueBusinessHoursService.save(request);
                }));
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

    // 持明文码时把入口码一并核验（单次来源在这里消耗，一次请求只消耗一次），返回入口码供逐行判；不持明文码返回 null。
    private String requireMerchantPermission(String permissionCode) {
        boolean viewPhonePlaintext = authorizationService.currentActorHasPermission(PhoneMask.VIEW_PLAINTEXT_PERMISSION);
        if (viewPhonePlaintext) {
            authorizationService.requirePermissions(permissionCode, PhoneMask.VIEW_PLAINTEXT_PERMISSION);
        } else {
            authorizationService.requirePermission(permissionCode);
        }
        return viewPhonePlaintext ? permissionCode : null;
    }

    // M723-F06（操作员 2026-10-03：敏感明文「算用掉，看一次扣一次」）：本请求只是返回第一次的结果、新用掉的单次来源会被回滚（不算用掉）时，
    // 即使核验过明文码也按掩码返回——不扣就不给明文。
    // 18E（第 19 条 19-R3）（治理地图第 877 行「可以在其获准访问的商户档案列表和档案操作结果中查看完整号码」「平台管理员身份本身不自动取得该查看权限」
    // 「也不免除项目范围检查」；第 659 行「在所分配的权限内」）：逐行判——入口码与明文码可触及范围的交集覆盖该商户所属项目才给明文
    // （本请求刚用掉的单次来源仍计入），其余行掩码；不持明文码一律掩码（含平台管理员）；最高权限本人持明文码即整份明文、不按项目判。
    private java.util.function.Predicate<MerchantRecord> plaintextRows(String plaintextCode) {
        if (plaintextCode == null || IdempotentReplayContext.replayDiscardsConsumption()) {
            return merchant -> false;
        }
        var actor = authorizationService.currentActor();
        if (authorizationService.isMasterAuthority(actor)) {
            return merchant -> true;
        }
        var reach = authorizationService.projectReach(actor, plaintextCode, PhoneMask.VIEW_PLAINTEXT_PERMISSION);
        // 18E 组间遗漏（第 20 条连带；治理地图第 659 行「各经营项目的员工和主项目的员工一样」）：行项目＝该商户的经营项目
        // （主项目加已批准追加的项目，与列表取数同口径），任一被覆盖即明文。ponytail: 主项目不覆盖的行逐行查一次经营项目，一页至多 size 次。
        return merchant -> reach.covers(merchant.projectId())
                || service.merchantOperatingProjectIds(merchant.id()).stream().anyMatch(reach::covers);
    }

    private MerchantRecord maskPhonesForUnauthorized(MerchantRecord merchant, String plaintextCode) {
        return merchant == null || plaintextRows(plaintextCode).test(merchant) ? merchant : merchant.withMaskedPhones();
    }

    private PageResponse<MerchantRecord> maskPhonesForUnauthorized(PageResponse<MerchantRecord> page, String plaintextCode) {
        var plaintext = plaintextRows(plaintextCode);
        return new PageResponse<>(page.items().stream()
                .map(merchant -> plaintext.test(merchant) ? merchant : merchant.withMaskedPhones()).toList(),
                page.page(), page.pageSize(), page.total());
    }
}
