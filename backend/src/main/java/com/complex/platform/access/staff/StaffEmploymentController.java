package com.complex.platform.access.staff;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff/profiles")
public class StaffEmploymentController {
    private final StaffHandoverService handovers;
    public StaffEmploymentController(StaffHandoverService handovers){this.handovers=handovers;}

    @GetMapping("/{id}/handover")
    public ApiResponse<StaffHandoverService.Overview> overview(@PathVariable String id){
        return ApiResponse.ok(handovers.overview(id));
    }

    @PostMapping("/{id}/handover")
    public ApiResponse<StaffHandoverRepository.Transfer> assign(@PathVariable String id,@RequestBody StaffHandoverService.Request request){
        return ApiResponse.ok(handovers.assign(id,request));
    }
}
