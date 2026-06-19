package com.complex.platform.terminal;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.terminal.dto.MdmAppWhitelistRequest;
import com.complex.platform.terminal.dto.MdmDeviceEnrollRequest;
import com.complex.platform.terminal.dto.MdmPolicyViolationRequest;
import com.complex.platform.terminal.dto.MdmSiteWhitelistRequest;
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
        return ApiResponse.ok(service.listAppWhitelist());
    }

    @PutMapping("/app-whitelist/{id}")
    public ApiResponse<MdmAppWhitelistRecord> upsertAppWhitelist(
            @PathVariable String id,
            @RequestBody MdmAppWhitelistRequest request
    ) {
        authorizationService.requirePermission("mdm.policy.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.upsertAppWhitelist(id, request));
    }

    @GetMapping("/site-whitelist")
    public ApiResponse<PageResponse<MdmSiteWhitelistRecord>> listSiteWhitelist() {
        authorizationService.requirePermission("mdm.policy.manage");
        return ApiResponse.ok(service.listSiteWhitelist());
    }

    @PutMapping("/site-whitelist/{id}")
    public ApiResponse<MdmSiteWhitelistRecord> upsertSiteWhitelist(
            @PathVariable String id,
            @RequestBody MdmSiteWhitelistRequest request
    ) {
        authorizationService.requirePermission("mdm.policy.manage");
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

    @GetMapping("/violations")
    public ApiResponse<PageResponse<MdmPolicyViolationRecord>> listViolations(
            @RequestParam(value = "device_id", required = false) String deviceId
    ) {
        authorizationService.requirePermission("mdm.violation.view");
        return ApiResponse.ok(service.listViolations(deviceId));
    }
}
