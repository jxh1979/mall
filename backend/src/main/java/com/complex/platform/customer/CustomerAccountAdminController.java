package com.complex.platform.customer;

// 文件版本：V0.01

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台管理客人账号状态（V194；ROOT-03-R23 按 BSM 第 819 行、ENUM 第 68 行对齐）：
 * 置入受限（截止按项目受限时长）、提前解除受限需 customer.account.manage；列入或解除黑名单只能提交申请，经夜班值班经理与娱乐平台负责人两级批准后执行
 * （见 {@link CustomerMembershipSettingsController}），不再有直接拉黑入口。客人自助注销走 /api/v1/customer/deregister。
 */
@RestController
@RequestMapping("/api/v1/customer-accounts")
public class CustomerAccountAdminController {

    private static final String MANAGE = "customer.account.manage";

    private final CustomerService service;
    private final CustomerAccountStatusService statusService;
    private final AuthorizationService authorizationService;

    public CustomerAccountAdminController(CustomerService service, CustomerAccountStatusService statusService,
            AuthorizationService authorizationService) {
        this.service = service;
        this.statusService = statusService;
        this.authorizationService = authorizationService;
    }

    /**
     * 按手机号精确查客人账号（盲索引查询，返回安全投影；查不到返回空列表）。
     * AUD-020：手机号属 PII，改用 POST body 传参，避免明文手机号进入 URL query string 落入 access log / 代理日志。
     */
    @PostMapping("/lookup")
    @com.complex.platform.access.DoesNotConsumeSingleUseGrant // M723：按手机号查客人账号是查询（只留查看痕迹）；查询不扣单次授权（操作员 2026-10-03）
    public ApiResponse<java.util.List<CustomerAccountView>> lookup(
            @RequestBody @jakarta.validation.Valid CustomerAccountLookupRequest request) {
        authorizationService.requirePermission(MANAGE);
        String phone = request == null ? null : request.phone();
        return ApiResponse.ok(service.findAccountByPhone(phone).map(CustomerAccountView::of)
                .map(java.util.List::of).orElse(java.util.List.of()));
    }

    /** ROOT-03-R23 置入受限：请求体 {project_id, reason}；截止＝此刻＋该项目受限时长（出厂 90 天）。 */
    @PostMapping("/{customer_id}/suspend")
    public ApiResponse<CustomerAccountView> suspend(
            @PathVariable("customer_id") String customerId,
            @RequestBody @jakarta.validation.Valid CustomerSuspendRequest request) {
        return ApiResponse.ok(CustomerAccountView.of(statusService.restrict(customerId,
                request == null ? null : request.projectId(), request == null ? null : request.reason())));
    }

    /** ROOT-03-R23 提前解除受限（原因必填）；已关闭账号只能走两级批准的解除申请。 */
    @PostMapping("/{customer_id}/reinstate")
    public ApiResponse<CustomerAccountView> reinstate(
            @PathVariable("customer_id") String customerId,
            @RequestBody(required = false) CustomerAccountReasonRequest request) {
        authorizationService.requirePermission(MANAGE);
        return ApiResponse.ok(CustomerAccountView.of(service.reinstateCustomer(customerId, request == null ? null : request.reason())));
    }

    /** ROOT-03-R23 申请列入（list）或解除（release）黑名单：保安团队提交，待两级批准。 */
    @PostMapping("/{customer_id}/blacklist-requests")
    public ApiResponse<CustomerBlacklistRequestRecord> requestBlacklistChange(
            @PathVariable("customer_id") String customerId,
            @RequestBody CustomerBlacklistRequestCommand request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(statusService.requestBlacklistChange(customerId, request));
    }
}
