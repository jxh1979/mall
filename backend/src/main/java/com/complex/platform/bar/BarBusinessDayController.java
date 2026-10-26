package com.complex.platform.bar;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bar-business-day-configs")
public class BarBusinessDayController {
    private final BarBusinessDayService service;
    private final AuditedOperationExecutor operations;
    public BarBusinessDayController(BarBusinessDayService service,AuditedOperationExecutor operations){this.service=service;this.operations=operations;}

    @GetMapping
    public ApiResponse<BarBusinessDayService.Catalog> list(@RequestParam(name="bar_id",required=false)String bar){
        return ApiResponse.ok(operations.readSingle(AuditedOperation.of("bar.business_day.viewed","bar.business_day.view_failed",BarBusinessDayService.VIEW,BarBusinessDayService.OBJECT),
                ()->service.list(bar),result->bar==null?"catalog":bar));
    }
    @GetMapping("/{id}")
    public ApiResponse<BarBusinessDayService.Config> original(@PathVariable String id){
        return ApiResponse.ok(operations.readSingle(AuditedOperation.of("bar.business_day.original_viewed","bar.business_day.original_view_failed","governance.supervisor_review.view",BarBusinessDayService.OBJECT),
                ()->service.original(id),result->((BarBusinessDayService.Config)result).id()));
    }
    @PostMapping
    public ApiResponse<BarBusinessDayService.Config> submit(@RequestBody BarBusinessDayService.Request request){
        return ApiResponse.ok(operations.readSingle(AuditedOperation.of("bar.business_day.submit_completed","bar.business_day.submit_failed",BarBusinessDayService.MANAGE,BarBusinessDayService.OBJECT),
                ()->service.submit(request),result->((BarBusinessDayService.Config)result).id()));
    }
}
