package com.complex.platform.access.staff;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.gate.GuardedAction;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/staff/attendance/receipt-events")
public class StaffAttendanceReceiptReviewController {
    private final StaffAttendanceReceiptReviewService service;
    public StaffAttendanceReceiptReviewController(StaffAttendanceReceiptReviewService service){this.service=service;}

    @PostMapping("/{id}/review")
    @GuardedAction(permission="staff.shift.manage")
    public ApiResponse<StaffAttendanceReceiptReviewService.Review> submit(@PathVariable String id,
            @RequestBody StaffAttendanceReceiptReviewService.Request request){return ApiResponse.ok(service.submit(id,request));}

    @GetMapping("/{id}/reviews")
    public ApiResponse<List<StaffAttendanceReceiptReviewService.Review>> history(@PathVariable String id){
        return ApiResponse.ok(service.history(id));
    }
}
