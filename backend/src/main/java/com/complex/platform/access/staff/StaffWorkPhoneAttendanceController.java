package com.complex.platform.access.staff;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.gate.GuardedAction;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/staff/attendance")
public class StaffWorkPhoneAttendanceController {
    private final StaffWorkPhoneAttendanceService service;
    public StaffWorkPhoneAttendanceController(StaffWorkPhoneAttendanceService service) { this.service=service; }

    @PostMapping("/time-anchors")
    @GuardedAction(permission="staff.attendance.punch")
    public ApiResponse<StaffWorkPhoneAttendanceService.AnchorResponse> anchor(
            @RequestBody StaffWorkPhoneAttendanceService.AnchorRequest request) {
        return ApiResponse.ok(service.anchor(request));
    }

    @PostMapping("/receipt-events")
    @GuardedAction(permission="staff.attendance.punch")
    public ApiResponse<StaffWorkPhoneAttendanceService.Event> receipt(
            @RequestBody StaffWorkPhoneAttendanceService.ReceiptRequest request) {
        return ApiResponse.ok(service.receipt(request));
    }

    @GetMapping("/work-phone-context")
    public ApiResponse<StaffWorkPhoneAttendanceService.Context> context() {
        return ApiResponse.ok(service.currentContext());
    }

    @GetMapping("/work-phone-contexts")
    public ApiResponse<List<StaffWorkPhoneAttendanceService.Context>> contexts(@RequestParam(name="user_id") String userId) {
        return ApiResponse.ok(service.historicalContexts(userId));
    }

    @PostMapping("/receipt-events/recover")
    @GuardedAction(permission="staff.shift.manage")
    public ApiResponse<StaffWorkPhoneAttendanceService.Event> recover(
            @RequestBody StaffWorkPhoneAttendanceService.RecoveryRequest request) {
        return ApiResponse.ok(service.recover(request));
    }

    @GetMapping("/receipt-events")
    public ApiResponse<List<StaffWorkPhoneAttendanceService.Event>> events(
            @RequestParam(name="user_id",required=false) String userId) {
        return ApiResponse.ok(service.events(userId));
    }
}

