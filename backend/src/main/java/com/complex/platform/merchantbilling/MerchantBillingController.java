package com.complex.platform.merchantbilling;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.merchantbilling.dto.MerchantBillConfirmRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillCreateRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillDisputeRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillDunningRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant-billing")
public class MerchantBillingController {

    private final MerchantBillingService service;
    private final AuthorizationService authorizationService;

    public MerchantBillingController(
            MerchantBillingService service,
            AuthorizationService authorizationService
    ) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/bills")
    public ApiResponse<PageResponse<MerchantBillRecord>> listBills() {
        authorizationService.requirePermission("merchant.billing.view");
        return ApiResponse.ok(service.listBills());
    }

    @PostMapping("/bills")
    public ApiResponse<MerchantBillRecord> createBill(@RequestBody MerchantBillCreateRequest request) {
        authorizationService.requirePermission("merchant.billing.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createBill(request));
    }

    @PostMapping("/bills/{id}/confirm")
    public ApiResponse<MerchantBillRecord> confirmBill(
            @PathVariable String id,
            @RequestBody MerchantBillConfirmRequest request
    ) {
        authorizationService.requirePermission("merchant.billing.confirm");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.confirmBill(id, request));
    }

    @PostMapping("/bills/{id}/disputes")
    public ApiResponse<MerchantBillDisputeRecord> submitDispute(
            @PathVariable String id,
            @RequestBody MerchantBillDisputeRequest request
    ) {
        authorizationService.requirePermission("merchant.billing.dispute.submit");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.submitDispute(id, request));
    }

    @PostMapping("/bills/{id}/dunning-records")
    public ApiResponse<MerchantBillDunningRecord> recordDunning(
            @PathVariable String id,
            @RequestBody MerchantBillDunningRequest request
    ) {
        authorizationService.requirePermission("merchant.billing.dunning.record");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.recordDunning(id, request));
    }
}
