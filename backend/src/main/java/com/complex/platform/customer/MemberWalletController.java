package com.complex.platform.customer;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.customer.dto.MembershipSubscribeRequest;
import com.complex.platform.customer.dto.StoredValueConsumeRequest;
import com.complex.platform.customer.dto.StoredValueSettleRequest;
import com.complex.platform.customer.dto.StoredValueTopupRequest;
import com.complex.platform.finance.PaymentIntentRecord;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员钱包（客人端 customer-token）：储值充值/入账/消费/查询、付费会员开通/查询。
 * 充值/开通绑 finance 支付意图：先返回支付意图，支付成功后再调入账/生效接口；保证不凭空加余额/开通。
 */
@RestController
@RequestMapping("/api/v1/customer")
public class MemberWalletController {

    private final CustomerService customerService;
    private final StoredValueService storedValueService;
    private final MembershipService membershipService;
    private final LoyaltyService loyaltyService;
    private final CustomerRepository repository;

    public MemberWalletController(CustomerService customerService, StoredValueService storedValueService,
                                  MembershipService membershipService, LoyaltyService loyaltyService,
                                  CustomerRepository repository) {
        this.customerService = customerService;
        this.storedValueService = storedValueService;
        this.membershipService = membershipService;
        this.loyaltyService = loyaltyService;
        this.repository = repository;
    }

    // ---- 储值 ----

    @PostMapping("/stored-value/topup")
    public ApiResponse<PaymentIntentRecord> topup(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody StoredValueTopupRequest request
    ) {
        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
        return ApiResponse.ok(storedValueService.topup(customerId, request.projectId(),
                request.amountCent() == null ? 0L : request.amountCent(), request.channel(), request.idempotencyKey()));
    }

    @PostMapping("/stored-value/settle")
    public ApiResponse<CustomerStoredValueTransactionRecord> settle(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody StoredValueSettleRequest request
    ) {
        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
        return ApiResponse.ok(storedValueService.settleTopup(customerId, request.projectId(), request.paymentIntentId()));
    }

    @PostMapping("/stored-value/consume")
    public ApiResponse<CustomerStoredValueTransactionRecord> consume(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody StoredValueConsumeRequest request
    ) {
        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
        return ApiResponse.ok(storedValueService.consume(customerId, request.projectId(),
                request.amountCent() == null ? 0L : request.amountCent(), request.reason(), request.idempotencyKey()));
    }

    @GetMapping("/stored-value/account")
    public ApiResponse<CustomerStoredValueAccountRecord> getAccount(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(name = "project_id", required = false) String projectId
    ) {
        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
        return ApiResponse.ok(repository.findStoredValueAccount(customerId, projectId).orElse(null));
    }

    // ---- 付费会员 ----

    @PostMapping("/memberships/subscribe")
    public ApiResponse<PaymentIntentRecord> subscribe(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody MembershipSubscribeRequest request
    ) {
        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
        return ApiResponse.ok(membershipService.subscribe(customerId, request.planId(),
                request.channel(), request.idempotencyKey()));
    }

    @GetMapping("/memberships")
    public ApiResponse<PageResponse<CustomerMembershipRecord>> listMemberships(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
        List<CustomerMembershipRecord> list = repository.listMembershipsByCustomerId(customerId);
        return ApiResponse.ok(new PageResponse<>(list, 1, list.size(), list.size()));
    }

    @GetMapping("/membership-plans")
    public ApiResponse<PageResponse<CustomerMembershipPlanRecord>> listPlans(
            @RequestParam(name = "project_id", required = false) String projectId
    ) {
        List<CustomerMembershipPlanRecord> list = repository.listMembershipPlans(projectId);
        return ApiResponse.ok(new PageResponse<>(list, 1, list.size(), list.size()));
    }

    @GetMapping("/membership-benefits")
    public ApiResponse<Map<String, Object>> benefits(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
        return ApiResponse.ok(membershipService.activeBenefits(customerId));
    }

    // 积分转赠（会员间转移）已下线：HTML 明确「积分不支持会员间转赠」，端点/表/记录类已随 V172 清理移除。

    private String token(String customerToken, String authorization) {
        if (customerToken != null && !customerToken.isBlank()) {
            return customerToken.trim();
        }
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length()).trim();
        }
        return customerToken;
    }
}
