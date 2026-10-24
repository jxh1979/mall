package com.complex.platform.customer;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiRequests;
import com.complex.platform.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** ROOT-03-R31 商户级储值卡退出公示：平台配公示天数、商户门户发公告、客人端读公示中的公告（见 {@link MerchantCardExitNoticeService}）。 */
@RestController
public class MerchantCardExitNoticeController {

    private final MerchantCardExitNoticeService service;
    private final CustomerService customerService;

    public MerchantCardExitNoticeController(MerchantCardExitNoticeService service, CustomerService customerService) {
        this.service = service;
        this.customerService = customerService;
    }

    @GetMapping("/api/v1/loyalty/stored-value/merchant-card-exit-notice-policy")
    public ApiResponse<MerchantCardExitNoticeService.Policy> getPolicy() {
        return ApiResponse.ok(service.getPolicy());
    }

    @PostMapping("/api/v1/loyalty/stored-value/merchant-card-exit-notice-policy")
    public ApiResponse<MerchantCardExitNoticeService.Policy> savePolicy(@RequestBody MerchantCardExitNoticeService.PolicyRequest request) {
        ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.savePolicy(request));
    }

    @PostMapping("/api/v1/merchant-portal/card-exit-notices")
    public ApiResponse<MerchantCardExitNoticeService.Notice> publish(@RequestBody MerchantCardExitNoticeService.PublishRequest request) {
        ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.publish(request));
    }

    @GetMapping("/api/v1/merchant-portal/card-exit-notices")
    public ApiResponse<List<MerchantCardExitNoticeService.Notice>> listMine() {
        return ApiResponse.ok(service.listMine());
    }

    @GetMapping("/api/v1/customer/merchant-card-exit-notices")
    public ApiResponse<List<MerchantCardExitNoticeService.Notice>> listForCustomer(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(service.listForCustomer(customerService.resolveCustomerId(token(customerToken, authorization))));
    }

    private static String token(String customerToken, String authorization) {
        if (customerToken != null && !customerToken.isBlank()) {
            return customerToken.trim();
        }
        return authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7).trim() : null;
    }
}
