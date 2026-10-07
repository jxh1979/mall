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
    @GetMapping("/leads")
    // M723-F06：明文联系电话看一次扣一次（操作员 2026-10-03）；业务读权限照旧不扣
    @com.complex.platform.access.ConsumesSingleUseGrant(PhoneMask.VIEW_PLAINTEXT_PERMISSION)
    public ApiResponse<PageResponse<LeasingLeadRecord>> listLeads(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "500") int size) {
        authorizationService.requirePermission("leasing.lead.manage");
        return ApiResponse.ok(maskLeadPhonesForUnauthorized(service.listLeads(page, size)));
    }

    @GetMapping("/leads/follow-up-due")
    // M723-F06：明文联系电话看一次扣一次（操作员 2026-10-03）；业务读权限照旧不扣
    @com.complex.platform.access.ConsumesSingleUseGrant(PhoneMask.VIEW_PLAINTEXT_PERMISSION)
    public ApiResponse<PageResponse<LeasingLeadRecord>> listLeadsNeedingFollowUp(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "500") int size) {
        authorizationService.requirePermission("leasing.lead.manage");
        return ApiResponse.ok(maskLeadPhonesForUnauthorized(service.listLeadsNeedingFollowUp(page, size)));
    }

    @PostMapping("/leads")
    public ApiResponse<LeasingLeadRecord> createLead(@RequestBody @jakarta.validation.Valid LeasingLeadRequest request) {
        authorizationService.requirePermission("leasing.lead.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskLeadPhoneForUnauthorized(service.createLead(request)));
    }

    @PostMapping("/leads/{lead_id}/assign")
    public ApiResponse<LeasingLeadRecord> assignLead(
            @PathVariable("lead_id") String leadId, @RequestBody @jakarta.validation.Valid LeasingLeadAssignRequest request) {
        authorizationService.requirePermission("leasing.lead.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskLeadPhoneForUnauthorized(service.assignLead(leadId, request)));
    }

    @PostMapping("/leads/{lead_id}/status")
    public ApiResponse<LeasingLeadRecord> updateLeadStatus(
            @PathVariable("lead_id") String leadId, @RequestBody @jakarta.validation.Valid LeasingLeadStatusRequest request) {
        authorizationService.requirePermission("leasing.lead.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(maskLeadPhoneForUnauthorized(service.updateLeadStatus(leadId, request)));
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

    // ── P0-A/GAP-07 招商线索联系人手机号展示脱敏（V339）。无 pii.contact_phone.view 的读端看掩码；
    // 脱敏放在 controller（service 外），与 V279 内部结算价同款范式。 ──
    private LeasingLeadRecord maskLeadPhoneForUnauthorized(LeasingLeadRecord record) {
        if (record == null || viewPhonePlaintext(java.util.stream.Stream.of(record.contactPhone()))) {
            return record;
        }
        return record.withMaskedPhone();
    }

    private PageResponse<LeasingLeadRecord> maskLeadPhonesForUnauthorized(PageResponse<LeasingLeadRecord> page) {
        if (viewPhonePlaintext(page.items().stream().map(LeasingLeadRecord::contactPhone))) {
            return page;
        }
        return new PageResponse<>(page.items().stream().map(LeasingLeadRecord::withMaskedPhone).toList(),
                page.page(), page.pageSize(), page.total());
    }

    // M723-F06（操作员 2026-10-03：敏感明文「算用掉，看一次扣一次」）：持有明文码、且这次响应里确有非空电话，才核验明文码并返回明文
    // ——单次来源在这里消耗，长期来源不产生消耗；否则返回掩码、不碰明文码。一次响应只核一次（列表按整页判断）。
    private boolean viewPhonePlaintext(java.util.stream.Stream<String> phones) {
        if (phones.noneMatch(phone -> phone != null && !phone.isBlank())
                || !authorizationService.currentActorHasPermission(PhoneMask.VIEW_PLAINTEXT_PERMISSION)) {
            return false;
        }
        authorizationService.requirePermission(PhoneMask.VIEW_PLAINTEXT_PERMISSION);
        // 本请求只是返回第一次的结果、新用掉的单次来源会被回滚（不算用掉）时不给明文
        return !com.complex.platform.common.idempotency.IdempotentReplayContext.replayDiscardsConsumption();
    }
}
