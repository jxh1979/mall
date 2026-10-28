package com.complex.platform.customer;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.customer.dto.BirthdayRuleConfigRequest;
import com.complex.platform.customer.dto.CheckInTierConfigRequest;
import com.complex.platform.customer.dto.MemberLevelConfigRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员忠诚度运营配置后台端点（会员忠诚度 PRD §3/§6）：等级档位/门槛/保级/倍率、连续签到阶梯、生日加倍规则。
 * 查看需 customer.loyalty.view，维护需 customer.loyalty.manage。POST 兼作新增/更新（请求体 id 空=新增、非空=更新）。
 * 与既有 {@code /api/v1/loyalty/member-levels}（LoyaltyController 只读 GET）分路径，本处为带权限闸的配置读写通道。
 * 第十八批第 12 条：会员等级与生日加倍规则全平台一套，不带 project_id；签到阶梯仍按综合体。
 */
@RestController
@RequestMapping("/api/v1/loyalty/config")
public class LoyaltyConfigAdminController {

    private final LoyaltyConfigAdminService service;

    public LoyaltyConfigAdminController(LoyaltyConfigAdminService service) {
        this.service = service;
    }

    @GetMapping("/member-levels")
    public ApiResponse<PageResponse<CustomerMemberLevelRecord>> listMemberLevels() {
        return ApiResponse.ok(service.listMemberLevels());
    }

    @PostMapping("/member-levels")
    public ApiResponse<CustomerMemberLevelRecord> saveMemberLevel(
            @RequestBody @jakarta.validation.Valid MemberLevelConfigRequest request) {
        return ApiResponse.ok(service.saveMemberLevel(request));
    }

    @GetMapping("/check-in-tiers")
    public ApiResponse<PageResponse<CustomerCheckInTierRecord>> listCheckInTiers(
            @RequestParam(name = "project_id", required = false) String projectId) {
        return ApiResponse.ok(service.listCheckInTiers(projectId));
    }

    @PostMapping("/check-in-tiers")
    public ApiResponse<CustomerCheckInTierRecord> saveCheckInTier(
            @RequestBody @jakarta.validation.Valid CheckInTierConfigRequest request) {
        return ApiResponse.ok(service.saveCheckInTier(request));
    }

    @GetMapping("/birthday-rules")
    public ApiResponse<PageResponse<CustomerBirthdayRuleRecord>> listBirthdayRules() {
        return ApiResponse.ok(service.listBirthdayRules());
    }

    @PostMapping("/birthday-rules")
    public ApiResponse<CustomerBirthdayRuleRecord> saveBirthdayRule(
            @RequestBody @jakarta.validation.Valid BirthdayRuleConfigRequest request) {
        return ApiResponse.ok(service.saveBirthdayRule(request));
    }
}
