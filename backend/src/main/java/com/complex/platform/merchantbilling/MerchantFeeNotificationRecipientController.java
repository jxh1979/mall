package com.complex.platform.merchantbilling;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.merchantbilling.dto.MerchantFeeNotificationRecipientRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant-billing/fee-notification-recipients")
public class MerchantFeeNotificationRecipientController {

    private static final String PERM = "merchant.billing.manage";

    private final MerchantFeeNotificationRecipientService service;
    private final AuthorizationService authorizationService;

    public MerchantFeeNotificationRecipientController(
            MerchantFeeNotificationRecipientService service,
            AuthorizationService authorizationService
    ) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<MerchantFeeNotificationRecipientRecord>> list(
            @RequestParam("merchant_id") String merchantId
    ) {
        authorizationService.requirePermission(PERM);
        return ApiResponse.ok(service.list(merchantId));
    }

    @GetMapping("/{id}")
    public ApiResponse<MerchantFeeNotificationRecipientRecord> get(@PathVariable String id) {
        authorizationService.requirePermission(PERM);
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    public ApiResponse<MerchantFeeNotificationRecipientRecord> upsert(
            @RequestBody @jakarta.validation.Valid MerchantFeeNotificationRecipientRequest request
    ) {
        authorizationService.requirePermission(PERM);
        return ApiResponse.ok(service.upsert(request));
    }

    @PostMapping("/{id}/deactivate")
    public ApiResponse<MerchantFeeNotificationRecipientRecord> deactivate(@PathVariable String id) {
        authorizationService.requirePermission(PERM);
        return ApiResponse.ok(service.deactivate(id));
    }
}
