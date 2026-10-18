package com.complex.platform.amusement;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/amusement/sessions")
public class AmusementSessionLifecycleController {
    private final AmusementSessionLifecycleService service;
    private final com.complex.platform.evidence.AuditEventService audit;
    public AmusementSessionLifecycleController(AmusementSessionLifecycleService service,
            com.complex.platform.evidence.AuditEventService audit) { this.service=service; this.audit=audit; }

    @PostMapping("/{id}/end")
    public ApiResponse<AmusementSessionLifecycleService.Ending> end(@PathVariable String id,
            @RequestBody(required=false) AmusementSessionLifecycleService.EndRequest request) {
        try {
            return ApiResponse.ok(service.endEarly(id,request));
        } catch(RuntimeException failure) {
            // 包含业务方法执行前的终端能力拒绝；失败审计由独立事务保留，成功审计随清场事务提交。
            audit.recordFailure("amusement.session.end_failed",AmusementSessionLifecycleService.END,
                    "amusement_session",id,null,request==null?null:request.idempotencyKey(),
                    failure instanceof com.complex.platform.common.api.ApiException api?api.code():"INTERNAL_ERROR");
            throw failure;
        }
    }

    @GetMapping("/{id}/endings")
    public ApiResponse<List<AmusementSessionLifecycleService.Ending>> endings(@PathVariable String id) {
        return ApiResponse.ok(service.endings(id));
    }

    @GetMapping("/{id}/followups")
    public ApiResponse<List<Map<String,Object>>> followups(@PathVariable String id) {
        return ApiResponse.ok(service.followups(id));
    }
}
