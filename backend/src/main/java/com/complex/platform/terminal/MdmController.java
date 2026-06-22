package com.complex.platform.terminal;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.terminal.dto.MdmAppWhitelistRequest;
import com.complex.platform.terminal.dto.MdmDeviceEnrollRequest;
import com.complex.platform.terminal.dto.MdmPolicyViolationRequest;
import com.complex.platform.terminal.dto.MdmSiteWhitelistRequest;
import com.complex.platform.terminal.dto.MdmViolationDetectRequest;
import com.complex.platform.terminal.dto.MdmViolationHandleRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** R5 T9 MDM 自有侧接口：白名单管理 / 设备纳管 / 违规查询。 */
@RestController
@RequestMapping("/api/v1/terminals/mdm")
public class MdmController {

    private final MdmService service;
    private final AuthorizationService authorizationService;

    public MdmController(MdmService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/app-whitelist")
    public ApiResponse<PageResponse<MdmAppWhitelistRecord>> listAppWhitelist() {
        authorizationService.requirePermission("mdm.policy.manage");
        // F-CROSS-02 最小收口：MDM 白名单是平台级清单（mdm_app_whitelist 仅 global/project/device_model scope，
        // 无 merchant_id 列；权限 mdm.policy.manage domain=terminal 仅授 platform_admin）。fail-closed 拒绝
        // merchant-scoped 主体读取，防"权限被下放给商户角色即可读全平台白名单"。
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(service.listAppWhitelist());
    }

    @PutMapping("/app-whitelist/{id}")
    public ApiResponse<MdmAppWhitelistRecord> upsertAppWhitelist(
            @PathVariable String id,
            @RequestBody MdmAppWhitelistRequest request
    ) {
        authorizationService.requirePermission("mdm.policy.manage");
        // F-CROSS-02 最小收口：白名单平台级，fail-closed 拒绝 merchant-scoped 主体写入，防跨商户篡改/覆盖。
        authorizationService.requirePlatformScope();
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.upsertAppWhitelist(id, request));
    }

    @GetMapping("/site-whitelist")
    public ApiResponse<PageResponse<MdmSiteWhitelistRecord>> listSiteWhitelist() {
        authorizationService.requirePermission("mdm.policy.manage");
        // F-CROSS-02 最小收口：白名单平台级，fail-closed 拒绝 merchant-scoped 主体读取。
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(service.listSiteWhitelist());
    }

    @PutMapping("/site-whitelist/{id}")
    public ApiResponse<MdmSiteWhitelistRecord> upsertSiteWhitelist(
            @PathVariable String id,
            @RequestBody MdmSiteWhitelistRequest request
    ) {
        authorizationService.requirePermission("mdm.policy.manage");
        // F-CROSS-02 最小收口：白名单平台级，fail-closed 拒绝 merchant-scoped 主体写入，防跨商户篡改/覆盖。
        authorizationService.requirePlatformScope();
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.upsertSiteWhitelist(id, request));
    }

    @PostMapping("/devices/enroll")
    public ApiResponse<MdmDeviceManagementRecord> enrollDevice(@RequestBody MdmDeviceEnrollRequest request) {
        authorizationService.requirePermission("mdm.device.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.enrollDevice(request));
    }

    @PostMapping("/violations")
    public ApiResponse<MdmPolicyViolationRecord> reportViolation(@RequestBody MdmPolicyViolationRequest request) {
        authorizationService.requirePermission("mdm.device.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reportPolicyViolation(request));
    }

    /** P0#9 服务端违规检测：上报 app/site，由服务端比对白名单判定并产生 violation。 */
    @PostMapping("/violations/detect")
    public ApiResponse<MdmViolationDetectionResult> detectViolations(@RequestBody MdmViolationDetectRequest request) {
        authorizationService.requirePermission("mdm.device.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.detectViolations(
                request == null ? null : request.idempotencyKey(),
                request == null ? null : request.deviceId(),
                request == null ? null : request.appPackages(),
                request == null ? null : request.siteDomains()));
    }

    /** P0#9 违规处置闭环：置 handled + 处置类型（device_blocked 联动封禁/撤销凭证）。 */
    @PostMapping("/violations/{id}/handle")
    public ApiResponse<MdmPolicyViolationRecord> handleViolation(
            @PathVariable String id,
            @RequestBody MdmViolationHandleRequest request
    ) {
        authorizationService.requirePermission("mdm.violation.handle");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.handleViolation(
                id,
                request == null ? null : request.disposition(),
                request == null ? null : request.handledNote(),
                request == null ? null : request.idempotencyKey()));
    }

    @GetMapping("/violations")
    public ApiResponse<PageResponse<MdmPolicyViolationRecord>> listViolations(
            @RequestParam(value = "device_id", required = false) String deviceId
    ) {
        authorizationService.requirePermission("mdm.violation.view");
        return ApiResponse.ok(service.listViolations(deviceId));
    }
}
