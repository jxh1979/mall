package com.complex.platform.leasing;

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.governance.ContractRecord;
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
    public ApiResponse<PageResponse<LeasingLeadRecord>> listLeads() {
        authorizationService.requirePermission("leasing.lead.manage");
        return ApiResponse.ok(service.listLeads());
    }

    @GetMapping("/leads/follow-up-due")
    public ApiResponse<PageResponse<LeasingLeadRecord>> listLeadsNeedingFollowUp() {
        authorizationService.requirePermission("leasing.lead.manage");
        return ApiResponse.ok(service.listLeadsNeedingFollowUp());
    }

    @PostMapping("/leads")
    public ApiResponse<LeasingLeadRecord> createLead(@RequestBody LeasingLeadRequest request) {
        authorizationService.requirePermission("leasing.lead.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createLead(request));
    }

    @PostMapping("/leads/{lead_id}/assign")
    public ApiResponse<LeasingLeadRecord> assignLead(
            @PathVariable("lead_id") String leadId, @RequestBody LeasingLeadAssignRequest request) {
        authorizationService.requirePermission("leasing.lead.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.assignLead(leadId, request));
    }

    @PostMapping("/leads/{lead_id}/status")
    public ApiResponse<LeasingLeadRecord> updateLeadStatus(
            @PathVariable("lead_id") String leadId, @RequestBody LeasingLeadStatusRequest request) {
        authorizationService.requirePermission("leasing.lead.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.updateLeadStatus(leadId, request));
    }

    // ---- 意向 ----
    @GetMapping("/intents")
    public ApiResponse<PageResponse<LeasingIntentRecord>> listIntents() {
        authorizationService.requirePermission("leasing.intent.manage");
        return ApiResponse.ok(service.listIntents());
    }

    @PostMapping("/intents")
    public ApiResponse<LeasingIntentRecord> createIntent(@RequestBody LeasingIntentRequest request) {
        authorizationService.requirePermission("leasing.intent.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createIntent(request));
    }

    @PostMapping("/intents/{intent_id}/advance")
    public ApiResponse<LeasingIntentRecord> advanceIntent(
            @PathVariable("intent_id") String intentId, @RequestBody LeasingIntentAdvanceRequest request) {
        authorizationService.requirePermission("leasing.intent.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.advanceIntent(intentId, request));
    }

    @PostMapping("/intents/{intent_id}/sign")
    public ApiResponse<ContractRecord> signIntent(
            @PathVariable("intent_id") String intentId, @RequestBody LeasingSignRequest request) {
        authorizationService.requirePermission("leasing.convert");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.signIntent(intentId, request));
    }

    // ---- 品牌库 ----
    @GetMapping("/brands")
    public ApiResponse<PageResponse<LeasingBrandRecord>> listBrands() {
        authorizationService.requirePermission("leasing.brand.manage");
        return ApiResponse.ok(service.listBrands());
    }

    @PostMapping("/brands")
    public ApiResponse<LeasingBrandRecord> createBrand(@RequestBody LeasingBrandRequest request) {
        authorizationService.requirePermission("leasing.brand.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createBrand(request));
    }

    @PostMapping("/brands/{brand_id}/blacklist")
    public ApiResponse<LeasingBrandRecord> blacklistBrand(
            @PathVariable("brand_id") String brandId, @RequestBody LeasingBrandBlacklistRequest request) {
        authorizationService.requirePermission("leasing.brand.blacklist");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.blacklistBrand(brandId, request));
    }

    // ---- 跟进 ----
    @GetMapping("/follow-ups")
    public ApiResponse<PageResponse<LeasingFollowUpRecord>> listFollowUps(
            @RequestParam("ref_type") String refType, @RequestParam("ref_id") String refId) {
        authorizationService.requirePermission("leasing.lead.manage");
        return ApiResponse.ok(service.listFollowUps(refType, refId));
    }

    @PostMapping("/follow-ups")
    public ApiResponse<LeasingFollowUpRecord> createFollowUp(@RequestBody LeasingFollowUpRequest request) {
        authorizationService.requirePermission("leasing.lead.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createFollowUp(request));
    }
}
