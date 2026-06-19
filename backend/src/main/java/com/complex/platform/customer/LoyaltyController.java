package com.complex.platform.customer;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.customer.dto.LoyaltyFreezeRequest;
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
 * 会员忠诚度后台管理（staff-token + 权限闸）：等级配置查询、积分账户冻结/解冻、手动等级评估。
 * 权限点 customer.loyalty.view / customer.points.freeze / customer.loyalty.evaluate（V089 seed）。
 */
@RestController
@RequestMapping("/api/v1/loyalty")
public class LoyaltyController {

    private final LoyaltyService service;
    private final CustomerRepository repository;

    public LoyaltyController(LoyaltyService service, CustomerRepository repository) {
        this.service = service;
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
}
