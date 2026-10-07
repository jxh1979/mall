package com.complex.platform.leasing;

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.PhoneMask;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.contract.ContractRecord;
import com.complex.platform.leasing.dto.LeasingBrandBlacklistRequest;
import com.complex.platform.leasing.dto.LeasingBrandRequest;
import com.complex.platform.leasing.dto.LeasingFollowUpRequest;
import com.complex.platform.leasing.dto.LeasingIntentAdvanceRequest;
import com.complex.platform.leasing.dto.LeasingIntentRequest;
import com.complex.platform.leasing.dto.LeasingLeadAssignRequest;
import com.complex.platform.leasing.dto.LeasingLeadRequest;
import com.complex.platform.leasing.dto.LeasingLeadStatusRequest;
import com.complex.platform.leasing.dto.LeasingSignRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 招商获客 API（V161 重建）：线索漏斗 + 意向 5 阶段 + 品牌库 + 跟进 + 签约转合同。 */
@RestController
@RequestMapping("/api/v1/leasing")
public class LeasingController {

    private final LeasingService service;
    private final AuthorizationService authorizationService;

    public LeasingController(LeasingService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    // ---- 线索 ----
    // M724（治理地图第 871 行，操作员 2026-10-07 确认：「招商线索联系人的手机号另按本句处理：线索列表和操作结果一律只显示脱敏号码；
    // 经单独授权且查看权限有效的人员，可以在某一条线索上点“查看完整号码”，只显示这一条的完整号码，每点一次算查看一次并留痕，
    // 单次授权看一次即用掉。」）：两个列表与三个写接口返回的线索一律脱敏，不核验、不消耗明文码；完整号码只走下面的 viewLeadContactPhone。
    @GetMapping("/leads")
    public ApiResponse<PageResponse<LeasingLeadRecord>> listLeads(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "500") int size) {
        authorizationService.requirePermission("leasing.lead.manage");
        return ApiResponse.ok(maskLeadPhones(service.listLeads(page, size)));
    }

    @GetMapping("/leads/follow-up-due")
    public ApiResponse<PageResponse<LeasingLeadRecord>> listLeadsNeedingFollowUp(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "500") int size) {
        authorizationService.requirePermission("leasing.lead.manage");
        return ApiResponse.ok(maskLeadPhones(service.listLeadsNeedingFollowUp(page, size)));
    }

    // M724：查看某一条线索的完整联系人手机号。先核线索业务权限（不扣）；明文码在服务层核验——排在范围与线索存在的判断之后，
    // 看一次扣一次（查看请求默认不消耗单次授权，标了本注解才对明文码消耗）。
    @GetMapping("/leads/{lead_id}/contact-phone")
    @com.complex.platform.access.ConsumesSingleUseGrant(PhoneMask.VIEW_PLAINTEXT_PERMISSION)
    public ApiResponse<LeasingLeadContactPhoneRecord> viewLeadContactPhone(@PathVariable("lead_id") String leadId) {
        authorizationService.requirePermission("leasing.lead.manage");
        return ApiResponse.ok(service.viewLeadContactPhone(leadId));
    }

    @PostMapping("/leads")
    public ApiResponse<LeasingLeadRecord> createLead(@RequestBody @jakarta.validation.Valid LeasingLeadRequest request) {
        authorizationService.requirePermission("leasing.lead.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createLead(request).withMaskedPhone());
    }

    @PostMapping("/leads/{lead_id}/assign")
    public ApiResponse<LeasingLeadRecord> assignLead(
            @PathVariable("lead_id") String leadId, @RequestBody @jakarta.validation.Valid LeasingLeadAssignRequest request) {
        authorizationService.requirePermission("leasing.lead.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.assignLead(leadId, request).withMaskedPhone());
    }

    @PostMapping("/leads/{lead_id}/status")
    public ApiResponse<LeasingLeadRecord> updateLeadStatus(
            @PathVariable("lead_id") String leadId, @RequestBody @jakarta.validation.Valid LeasingLeadStatusRequest request) {
        authorizationService.requirePermission("leasing.lead.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.updateLeadStatus(leadId, request).withMaskedPhone());
    }

    // ---- 意向 ----
    @GetMapping("/intents")
    public ApiResponse<PageResponse<LeasingIntentRecord>> listIntents(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "500") int size) {
        authorizationService.requirePermission("leasing.intent.manage");
        return ApiResponse.ok(service.listIntents(page, size));
    }

    @PostMapping("/intents")
    public ApiResponse<LeasingIntentRecord> createIntent(@RequestBody @jakarta.validation.Valid LeasingIntentRequest request) {
        authorizationService.requirePermission("leasing.intent.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createIntent(request));
    }

    @PostMapping("/intents/{intent_id}/advance")
    public ApiResponse<LeasingIntentRecord> advanceIntent(
            @PathVariable("intent_id") String intentId, @RequestBody @jakarta.validation.Valid LeasingIntentAdvanceRequest request) {
        authorizationService.requirePermission("leasing.intent.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.advanceIntent(intentId, request));
    }

    @PostMapping("/intents/{intent_id}/sign")
    public ApiResponse<ContractRecord> signIntent(
            @PathVariable("intent_id") String intentId, @RequestBody @jakarta.validation.Valid LeasingSignRequest request) {
        authorizationService.requirePermission("leasing.convert");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.signIntent(intentId, request));
    }

    // ---- 品牌库 ----
    @GetMapping("/brands")
    public ApiResponse<PageResponse<LeasingBrandRecord>> listBrands(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "500") int size) {
        authorizationService.requirePermission("leasing.brand.manage");
        return ApiResponse.ok(service.listBrands(page, size));
    }

    @PostMapping("/brands")
    public ApiResponse<LeasingBrandRecord> createBrand(@RequestBody @jakarta.validation.Valid LeasingBrandRequest request) {
        authorizationService.requirePermission("leasing.brand.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createBrand(request));
    }

    @PostMapping("/brands/{brand_id}/blacklist")
    public ApiResponse<LeasingBrandRecord> blacklistBrand(
            @PathVariable("brand_id") String brandId, @RequestBody @jakarta.validation.Valid LeasingBrandBlacklistRequest request) {
        authorizationService.requirePermission("leasing.brand.blacklist");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.blacklistBrand(brandId, request));
    }

    // ---- 跟进 ----
    @GetMapping("/follow-ups")
    public ApiResponse<PageResponse<LeasingFollowUpRecord>> listFollowUps(
            @RequestParam(value = "ref_type", required = false) String refType,
            @RequestParam(value = "ref_id", required = false) String refId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "500") int size) {
        authorizationService.requirePermission("leasing.lead.manage");
        return ApiResponse.ok(service.listFollowUps(refType, refId, page, size));
    }

    @PostMapping("/follow-ups")
    public ApiResponse<LeasingFollowUpRecord> createFollowUp(@RequestBody @jakarta.validation.Valid LeasingFollowUpRequest request) {
        authorizationService.requirePermission("leasing.lead.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createFollowUp(request));
    }

    // ── P0-A/GAP-07 招商线索联系人手机号展示脱敏（V339）；M724 起列表一律脱敏（见「线索」段注释），不再按 pii.contact_phone.view 分支。
    // 脱敏放在 controller（service 外），与 V279 内部结算价同款范式。 ──
    private PageResponse<LeasingLeadRecord> maskLeadPhones(PageResponse<LeasingLeadRecord> page) {
        return new PageResponse<>(page.items().stream().map(LeasingLeadRecord::withMaskedPhone).toList(),
                page.page(), page.pageSize(), page.total());
    }
}
