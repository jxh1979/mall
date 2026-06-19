package com.complex.platform.terminal;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.terminal.dto.CapabilitySwitchRequest;
import com.complex.platform.terminal.dto.DeviceBindRequest;
import com.complex.platform.terminal.dto.DeviceLifecycleRequest;
import com.complex.platform.terminal.dto.DeviceReturnRequest;
import com.complex.platform.terminal.dto.DeviceUnbindRequest;
import com.complex.platform.terminal.dto.KioskHeartbeatRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/terminals")
public class TerminalDeviceController {

    private final TerminalDeviceService service;
    private final AuthorizationService authorizationService;

    public TerminalDeviceController(TerminalDeviceService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/devices/bind")
    public ApiResponse<DeviceBindingResponse> bindDevice(@RequestBody DeviceBindRequest request) {
        authorizationService.requirePermission("terminal.device.bind");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.bindDevice(request));
    }

    @PostMapping("/devices/return")
    public ApiResponse<DeviceBindingResponse> returnDevice(@RequestBody DeviceReturnRequest request) {
        authorizationService.requirePermission("terminal.device.bind");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.returnDevice(request));
    }

    @PostMapping("/devices/{id}/unbind")
    public ApiResponse<DeviceBindingResponse> unbindDevice(
            @PathVariable String id,
            @RequestBody DeviceUnbindRequest request
    ) {
        authorizationService.requirePermission("terminal.device.unbind");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.unbindDevice(id, request));
    }

    // ── P0#9 设备生命周期状态机端点 ───────────────────────────────────────────
    @PostMapping("/devices/{id}/repair")
    public ApiResponse<DeviceBindingResponse> repairDevice(
            @PathVariable String id,
            @RequestBody DeviceLifecycleRequest request
    ) {
        authorizationService.requirePermission("terminal.device.lifecycle");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.repairDevice(id, request));
    }

    @PostMapping("/devices/{id}/lost")
    public ApiResponse<DeviceBindingResponse> markDeviceLost(
            @PathVariable String id,
            @RequestBody DeviceLifecycleRequest request
    ) {
        authorizationService.requirePermission("terminal.device.lifecycle");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.markDeviceLost(id, request));
    }

    @PostMapping("/devices/{id}/found")
    public ApiResponse<DeviceBindingResponse> markDeviceFound(
            @PathVariable String id,
            @RequestBody DeviceLifecycleRequest request
    ) {
        authorizationService.requirePermission("terminal.device.lifecycle");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.markDeviceFound(id, request));
    }

    @PostMapping("/devices/{id}/scrap")
    public ApiResponse<DeviceBindingResponse> scrapDevice(
            @PathVariable String id,
            @RequestBody DeviceLifecycleRequest request
    ) {
        authorizationService.requirePermission("terminal.device.lifecycle");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.scrapDevice(id, request));
    }

    @GetMapping("/capability-switches")
    public ApiResponse<PageResponse<TerminalCapabilitySwitchRecord>> listCapabilitySwitches() {
        authorizationService.requirePermission("terminal.switch.view");
        return ApiResponse.ok(service.listCapabilitySwitches());
    }

    @PutMapping("/capability-switches/{id}")
    public ApiResponse<TerminalCapabilitySwitchRecord> updateCapabilitySwitch(
            @PathVariable String id,
            @RequestBody CapabilitySwitchRequest request
    ) {
        authorizationService.requirePermission("terminal.switch.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.updateCapabilitySwitch(id, request));
    }

    @GetMapping("/kiosks/{device_id}/config")
    public ApiResponse<KioskConfigResponse> getKioskConfig(
            @PathVariable("device_id") String deviceId,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken
    ) {
        return ApiResponse.ok(service.getKioskConfig(deviceId, deviceToken));
    }

    @PostMapping("/kiosks/{device_id}/heartbeat")
    public ApiResponse<KioskHeartbeatRecord> recordKioskHeartbeat(
            @PathVariable("device_id") String deviceId,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @RequestBody KioskHeartbeatRequest request
    ) {
        return ApiResponse.ok(service.recordKioskHeartbeat(deviceId, deviceToken, request));
    }
}
