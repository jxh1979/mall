package com.complex.platform.customer;

// 文件版本：V0.01

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.common.api.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台：会员受限时长与注册注销频次预警参数（值与上下限同配）、黑名单申请的查看与两级批准（ROOT-03-R22、R23）。
 */
@RestController
@RequestMapping("/api/v1")
public class CustomerMembershipSettingsController {

    private final CustomerChurnAlertService churnAlerts;
    private final CustomerAccountStatusService statusService;

    public CustomerMembershipSettingsController(CustomerChurnAlertService churnAlerts, CustomerAccountStatusService statusService) {
        this.churnAlerts = churnAlerts;
        this.statusService = statusService;
    }

    @GetMapping("/customer-churn-alert-setting")
    public ApiResponse<CustomerChurnAlertService.Setting> getChurnAlertSetting() {
        return ApiResponse.ok(churnAlerts.getSetting());
    }

    @PutMapping("/customer-churn-alert-setting")
    public ApiResponse<CustomerChurnAlertService.Setting> configureChurnAlertSetting(
            @RequestBody CustomerChurnAlertService.SettingRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(churnAlerts.configure(request));
    }

    @GetMapping("/customer-restriction-settings/{project_id}")
    public ApiResponse<CustomerAccountStatusService.RestrictionSetting> getRestrictionSetting(
            @PathVariable("project_id") String projectId) {
        return ApiResponse.ok(statusService.getRestrictionSetting(projectId));
    }

    @PutMapping("/customer-restriction-settings/{project_id}")
    public ApiResponse<CustomerAccountStatusService.RestrictionSetting> configureRestrictionSetting(
            @PathVariable("project_id") String projectId,
            @RequestBody CustomerAccountStatusService.RestrictionSettingRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(statusService.configureRestrictionSetting(projectId, request));
    }

    @GetMapping("/customer-blacklist-requests")
    public ApiResponse<List<CustomerBlacklistRequestRecord>> listBlacklistRequests(
            @RequestParam(name = "status", required = false) String status) {
        return ApiResponse.ok(statusService.listBlacklistRequests(status));
    }

    @PostMapping("/customer-blacklist-requests/{request_id}/approve")
    public ApiResponse<CustomerBlacklistRequestRecord> approveBlacklistRequest(
            @PathVariable("request_id") String requestId, @RequestBody Map<String, String> request) {
        String key = request == null ? null : request.get("idempotency_key");
        requireIdempotencyKey(key);
        return ApiResponse.ok(statusService.approveBlacklistRequest(requestId, key));
    }

    @PostMapping("/customer-blacklist-requests/{request_id}/reject")
    public ApiResponse<CustomerBlacklistRequestRecord> rejectBlacklistRequest(
            @PathVariable("request_id") String requestId, @RequestBody Map<String, String> request) {
        String key = request == null ? null : request.get("idempotency_key");
        requireIdempotencyKey(key);
        return ApiResponse.ok(statusService.rejectBlacklistRequest(requestId, key));
    }
}
