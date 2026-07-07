package com.complex.platform.customer;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.customer.dto.CustomerPointsRuleRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台积分规则引擎配置端点（REQ-12, P0-6）。查看需 customer.points_rule.view，维护需 customer.points_rule.manage。
 * 租户隔离：项目级管理员只见/只写本项目（服务层按 token scope 过滤/覆盖）。更新沿用后台“POST /{id}”写法。
 */
@RestController
@RequestMapping("/api/v1/customer/points-rules")
public class CustomerPointsRuleAdminController {

    private final CustomerPointsRuleAdminService service;

    public CustomerPointsRuleAdminController(CustomerPointsRuleAdminService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<CustomerPointsRuleRecord>> list() {
        return ApiResponse.ok(service.list());
    }

    @GetMapping("/{rule_id}")
    public ApiResponse<CustomerPointsRuleRecord> get(@PathVariable("rule_id") String ruleId) {
        return ApiResponse.ok(service.get(ruleId));
    }

    @PostMapping
    public ApiResponse<CustomerPointsRuleRecord> create(
            @RequestBody @jakarta.validation.Valid CustomerPointsRuleRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PostMapping("/{rule_id}")
    public ApiResponse<CustomerPointsRuleRecord> update(
            @PathVariable("rule_id") String ruleId,
            @RequestBody @jakarta.validation.Valid CustomerPointsRuleRequest request) {
        return ApiResponse.ok(service.update(ruleId, request));
    }
}
