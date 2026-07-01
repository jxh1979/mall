package com.complex.platform.customer;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import java.time.Instant;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台管理客人账号生命周期（V194）：封禁(临时)/拉黑(永久)/恢复。需权限 customer.account.manage。
 * 客人自助注销走 /api/v1/customer/deregister（凭本人 token，非本控制器）。
 */
@RestController
@RequestMapping("/api/v1/customer-accounts")
public class CustomerAccountAdminController {

    private static final String MANAGE = "customer.account.manage";

    private final CustomerService service;
    private final AuthorizationService authorizationService;

    public CustomerAccountAdminController(CustomerService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/{customer_id}/suspend")
    public ApiResponse<CustomerRecord> suspend(
            @PathVariable("customer_id") String customerId,
            @RequestBody @jakarta.validation.Valid CustomerSuspendRequest request) {
        authorizationService.requirePermission(MANAGE);
        Instant until = request == null || request.suspendedUntil() == null || request.suspendedUntil().isBlank()
                ? null : Instant.parse(request.suspendedUntil());
        return ApiResponse.ok(service.suspendCustomer(customerId, until, request == null ? null : request.reason()));
    }

    @PostMapping("/{customer_id}/blacklist")
    public ApiResponse<CustomerRecord> blacklist(
            @PathVariable("customer_id") String customerId,
            @RequestBody(required = false) CustomerAccountReasonRequest request) {
        authorizationService.requirePermission(MANAGE);
        return ApiResponse.ok(service.blacklistCustomer(customerId, request == null ? null : request.reason()));
    }

    @PostMapping("/{customer_id}/reinstate")
    public ApiResponse<CustomerRecord> reinstate(
            @PathVariable("customer_id") String customerId,
            @RequestBody(required = false) CustomerAccountReasonRequest request) {
        authorizationService.requirePermission(MANAGE);
        return ApiResponse.ok(service.reinstateCustomer(customerId, request == null ? null : request.reason()));
    }
}
