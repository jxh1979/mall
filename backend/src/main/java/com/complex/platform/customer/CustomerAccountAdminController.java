package com.complex.platform.customer;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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


    /** 按手机号精确查客人账号（盲索引查询，返回安全投影；查不到返回空列表）。 */
    @GetMapping
    public ApiResponse<java.util.List<CustomerAccountView>> lookup(@RequestParam("phone") String phone) {
        authorizationService.requirePermission(MANAGE);
        return ApiResponse.ok(service.findAccountByPhone(phone).map(CustomerAccountView::of)
                .map(java.util.List::of).orElse(java.util.List.of()));
    }

    @PostMapping("/{customer_id}/suspend")
    public ApiResponse<CustomerAccountView> suspend(
            @PathVariable("customer_id") String customerId,
            @RequestBody @jakarta.validation.Valid CustomerSuspendRequest request) {
        authorizationService.requirePermission(MANAGE);
        Instant until = request == null || request.suspendedUntil() == null || request.suspendedUntil().isBlank()
                ? null : Instant.parse(request.suspendedUntil());
        return ApiResponse.ok(CustomerAccountView.of(service.suspendCustomer(customerId, until, request == null ? null : request.reason())));
    }

    @PostMapping("/{customer_id}/blacklist")
    public ApiResponse<CustomerAccountView> blacklist(
            @PathVariable("customer_id") String customerId,
            @RequestBody(required = false) CustomerAccountReasonRequest request) {
        authorizationService.requirePermission(MANAGE);
        return ApiResponse.ok(CustomerAccountView.of(service.blacklistCustomer(customerId, request == null ? null : request.reason())));
    }

    @PostMapping("/{customer_id}/reinstate")
    public ApiResponse<CustomerAccountView> reinstate(
            @PathVariable("customer_id") String customerId,
            @RequestBody(required = false) CustomerAccountReasonRequest request) {
        authorizationService.requirePermission(MANAGE);
        return ApiResponse.ok(CustomerAccountView.of(service.reinstateCustomer(customerId, request == null ? null : request.reason())));
    }
}
