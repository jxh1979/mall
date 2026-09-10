package com.complex.platform.access.staff;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** U05-006：柜机单端上报格口事件——与 kiosk config/heartbeat 同一套 X-Device-Token 设备认证，不带员工会话。 */
@RestController
@RequestMapping("/api/v1/terminals/kiosks")
public class StaffLockerKioskController {
    private final StaffLockerService lockers;

    public StaffLockerKioskController(StaffLockerService lockers) {
        this.lockers = lockers;
    }

    @PostMapping("/{device_id}/locker-events")
    public ApiResponse<StaffLockerService.Receipt> lockerEvent(@PathVariable("device_id") String deviceId,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @RequestBody StaffLockerService.ScanEvent event) {
        return ApiResponse.ok(lockers.record(deviceId, deviceToken, event));
    }
}
