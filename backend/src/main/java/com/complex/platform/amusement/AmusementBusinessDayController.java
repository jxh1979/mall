package com.complex.platform.amusement;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/amusement/business-day-configs")
public class AmusementBusinessDayController {
    private final AmusementBusinessDayService service;
    private final AuditedOperationExecutor operations;
    public AmusementBusinessDayController(AmusementBusinessDayService service,AuditedOperationExecutor operations){this.service=service;this.operations=operations;}

    @GetMapping
    public ApiResponse<AmusementBusinessDayService.Catalog> list(@RequestParam(name="project_id",required=false)String project){
        return ApiResponse.ok(operations.readSingle(AuditedOperation.of("amusement.business_day.viewed","amusement.business_day.view_failed",AmusementBusinessDayService.VIEW,AmusementBusinessDayService.OBJECT),
                ()->service.list(project),result->project==null?"catalog":project));
    }
    @GetMapping("/{id}")
    public ApiResponse<AmusementBusinessDayService.Config> original(@PathVariable String id){
        return ApiResponse.ok(operations.readSingle(AuditedOperation.of("amusement.business_day.original_viewed","amusement.business_day.original_view_failed","governance.supervisor_review.view",AmusementBusinessDayService.OBJECT),
                ()->service.original(id),result->((AmusementBusinessDayService.Config)result).id()));
    }
    @PostMapping
    public ApiResponse<AmusementBusinessDayService.Config> submit(@RequestBody AmusementBusinessDayService.Request request){
        return ApiResponse.ok(operations.readSingle(AuditedOperation.of("amusement.business_day.submit_completed","amusement.business_day.submit_failed",AmusementBusinessDayService.MANAGE,AmusementBusinessDayService.OBJECT),
                ()->service.submit(request),result->((AmusementBusinessDayService.Config)result).id()));
    }
}
