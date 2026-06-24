package com.complex.platform.customer;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.customer.dto.LoyaltyFreezeRequest;
import com.complex.platform.customer.dto.MembershipPlanRequest;
import com.complex.platform.customer.dto.StoredValueFreezeRequest;
import com.complex.platform.customer.dto.StoredValueRefundRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员忠诚度后台管理（staff-token + 权限闸）：等级配置查询、积分账户冻结/解冻、手动等级评估；
 * 第2部分（V090）：付费会员档位管理、储值账户冻结/解冻/退卡。
 */
@RestController
@RequestMapping("/api/v1/loyalty")
public class LoyaltyController {

    private final LoyaltyService service;
    private final MembershipService membershipService;
    private final StoredValueService storedValueService;
    private final CustomerRepository repository;

    public LoyaltyController(LoyaltyService service, MembershipService membershipService,
                             StoredValueService storedValueService, CustomerRepository repository) {
        this.service = service;
        this.membershipService = membershipService;
        this.storedValueService = storedValueService;
        this.repository = repository;
    }

    @GetMapping("/member-levels")
    public ApiResponse<PageResponse<CustomerMemberLevelRecord>> listMemberLevels(
            @RequestParam(name = "project_id", required = false) String projectId
    ) {
        List<CustomerMemberLevelRecord> levels = repository.listMemberLevels(projectId);
        return ApiResponse.ok(new PageResponse<>(levels, 1, levels.size(), levels.size()));
    }

    @PostMapping("/points-accounts/{customer_id}/freeze")
    public ApiResponse<CustomerPointsAccountRecord> freeze(
            @PathVariable("customer_id") String customerId,
            @RequestBody(required = false) LoyaltyFreezeRequest request
    ) {
        return ApiResponse.ok(service.freeze(customerId, request == null ? null : request.reason()));
    }

    @PostMapping("/points-accounts/{customer_id}/unfreeze")
    public ApiResponse<CustomerPointsAccountRecord> unfreeze(
            @PathVariable("customer_id") String customerId,
            @RequestBody(required = false) LoyaltyFreezeRequest request
    ) {
        return ApiResponse.ok(service.unfreeze(customerId, request == null ? null : request.reason()));
    }

    @PostMapping("/points-accounts/{customer_id}/evaluate-level")
    public ApiResponse<CustomerPointsAccountRecord> evaluateLevel(
            @PathVariable("customer_id") String customerId
    ) {
        return ApiResponse.ok(service.evaluateLevel(customerId, Instant.now()));
    }

    // ---- 第2部分（V090）：付费会员档位管理 ----

    @PostMapping("/membership-plans")
    public ApiResponse<CustomerMembershipPlanRecord> saveMembershipPlan(
            @RequestBody MembershipPlanRequest request
    ) {
        return ApiResponse.ok(membershipService.savePlan(
                null, request.projectId(), request.planCode(), request.name(),
                request.priceCent() == null ? 0L : request.priceCent(),
                request.durationDays() == null ? 0 : request.durationDays(),
                request.benefits(), request.status()));
    }

    @GetMapping("/membership-plans")
    public ApiResponse<PageResponse<CustomerMembershipPlanRecord>> listMembershipPlans(
            @RequestParam(name = "project_id", required = false) String projectId
    ) {
        List<CustomerMembershipPlanRecord> list = repository.listMembershipPlans(projectId);
        return ApiResponse.ok(new PageResponse<>(list, 1, list.size(), list.size()));
    }

    // ---- 第2部分（V090）：储值账户冻结/解冻/退卡 ----

    @PostMapping("/stored-value/{customer_id}/freeze")
    public ApiResponse<CustomerStoredValueAccountRecord> freezeStoredValue(
            @PathVariable("customer_id") String customerId,
            @RequestBody(required = false) StoredValueFreezeRequest request
    ) {
        return ApiResponse.ok(storedValueService.freeze(customerId,
                request == null ? null : request.projectId(),
                request == null ? null : request.reason()));
    }

    @PostMapping("/stored-value/{customer_id}/unfreeze")
    public ApiResponse<CustomerStoredValueAccountRecord> unfreezeStoredValue(
            @PathVariable("customer_id") String customerId,
            @RequestBody(required = false) StoredValueFreezeRequest request
    ) {
        return ApiResponse.ok(storedValueService.unfreeze(customerId,
                request == null ? null : request.projectId(),
                request == null ? null : request.reason()));
    }

    @PostMapping("/stored-value/{customer_id}/refund")
    public ApiResponse<CustomerStoredValueTransactionRecord> refundStoredValue(
            @PathVariable("customer_id") String customerId,
            @RequestBody StoredValueRefundRequest request
    ) {
        return ApiResponse.ok(storedValueService.refund(customerId, request.projectId(),
                request.amountCent() == null ? 0L : request.amountCent(), request.reason(),
                Boolean.TRUE.equals(request.closeAccount()), request.idempotencyKey()));
    }

    // 积分转赠风控配置已随「积分转赠下线」移除（HTML：积分不支持会员间转赠）。
}
