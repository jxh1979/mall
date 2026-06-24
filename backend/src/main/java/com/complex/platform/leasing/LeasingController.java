package com.complex.platform.leasing;

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.governance.ContractRecord;
import com.complex.platform.leasing.dto.LeasingConvertRequest;
import com.complex.platform.leasing.dto.LeasingLeadRequest;
import com.complex.platform.leasing.dto.LeasingOfferRequest;
import com.complex.platform.leasing.dto.LeasingStatusRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ② 履约闭环 S4 招商/租赁 API：招商线索漏斗 + 租赁报价 + 报价转合同。 */
@RestController
@RequestMapping("/api/v1/leasing")
public class LeasingController {

    private final LeasingService service;
    private final AuthorizationService authorizationService;

    public LeasingController(LeasingService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/leads")
    public ApiResponse<PageResponse<LeasingLeadRecord>> listLeads() {
        authorizationService.requirePermission("leasing.lead.manage");
        return ApiResponse.ok(service.listLeads());
    }

    @PostMapping("/leads")
    public ApiResponse<LeasingLeadRecord> createLead(@RequestBody LeasingLeadRequest request) {
        authorizationService.requirePermission("leasing.lead.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createLead(request));
    }

    @PostMapping("/leads/{lead_id}/status")
    public ApiResponse<LeasingLeadRecord> updateLeadStatus(
            @PathVariable("lead_id") String leadId, @RequestBody LeasingStatusRequest request) {
        authorizationService.requirePermission("leasing.lead.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.updateLeadStatus(leadId, request));
    }

    @GetMapping("/offers")
    public ApiResponse<PageResponse<LeasingOfferRecord>> listOffers() {
        authorizationService.requirePermission("leasing.offer.manage");
        return ApiResponse.ok(service.listOffers());
    }

    @PostMapping("/offers")
    public ApiResponse<LeasingOfferRecord> createOffer(@RequestBody LeasingOfferRequest request) {
        authorizationService.requirePermission("leasing.offer.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createOffer(request));
    }

    @PostMapping("/offers/{offer_id}/status")
    public ApiResponse<LeasingOfferRecord> updateOfferStatus(
            @PathVariable("offer_id") String offerId, @RequestBody LeasingStatusRequest request) {
        authorizationService.requirePermission("leasing.offer.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.updateOfferStatus(offerId, request));
    }

    @PostMapping("/offers/{offer_id}/convert-to-contract")
    public ApiResponse<ContractRecord> convertOfferToContract(
            @PathVariable("offer_id") String offerId, @RequestBody LeasingConvertRequest request) {
        authorizationService.requirePermission("leasing.offer.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.convertOfferToContract(offerId, request));
    }
}
