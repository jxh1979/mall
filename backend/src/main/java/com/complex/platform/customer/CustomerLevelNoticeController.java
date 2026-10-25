package com.complex.platform.customer;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.evidence.AuditEventService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OBS-13-01 会员等级通知（BSM L820 降级预警、「已保级」、降级补发）：官方 App／小程序启动与回前台拉取本人未读通知并弹出，
 * 点「知道了」回写已读。只许本人（按客人令牌取客人）；BSM L1596 客人短信清单不含等级通知，不发短信。
 */
@RestController
@RequestMapping("/api/v1/customer/level-notices")
public class CustomerLevelNoticeController {

    private final LoyaltyService loyalty;
    private final CustomerService customers;
    private final AuditEventService audit;

    public CustomerLevelNoticeController(LoyaltyService loyalty, CustomerService customers, AuditEventService audit) {
        this.loyalty = loyalty;
        this.customers = customers;
        this.audit = audit;
    }

    @GetMapping
    public ApiResponse<List<LoyaltyService.LevelNotice>> list(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(loyalty.listLevelNotices(customers.resolveCustomerId(token(customerToken, authorization))));
    }

    @PostMapping("/{notice_id}/read")
    public ApiResponse<Void> read(@PathVariable("notice_id") String noticeId,
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        try {
            loyalty.markLevelNoticeRead(customers.resolveCustomerId(token(customerToken, authorization)), noticeId);
            audit.recordSuccess("member.level_notice.read", "not_applicable", "customer_level_notice", noticeId, null, null, Map.of());
            return ApiResponse.ok(null);
        } catch (ApiException exception) {
            audit.recordFailure("member.level_notice.read_failed", "not_applicable", "customer_level_notice", noticeId, null, null, exception.code());
            throw exception;
        }
    }

    private static String token(String customerToken, String authorization) {
        return customerToken != null && !customerToken.isBlank() ? customerToken.trim()
                : authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7).trim() : null;
    }
}
