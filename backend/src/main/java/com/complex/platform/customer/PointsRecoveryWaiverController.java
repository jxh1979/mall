package com.complex.platform.customer;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance/points-recoveries")
public class PointsRecoveryWaiverController {
    private final PointsRecoveryWaiverService service;
    private final AuditedOperationExecutor operations;
    public PointsRecoveryWaiverController(PointsRecoveryWaiverService service,AuditedOperationExecutor operations) {
        this.service=service;this.operations=operations;
    }
    @GetMapping("/{customerId}")
    public ApiResponse<PointsRecoveryWaiverService.Detail> get(@PathVariable String customerId) {
        return ApiResponse.ok(operations.readSingle(AuditedOperation.of("points.recovery.viewed","points.recovery.view_failed",
                PointsRecoveryWaiverService.VIEW,"customer_points_recovery"),()->service.get(customerId),result->customerId));
    }
    @PostMapping("/{customerId}/waive")
    public ApiResponse<PointsRecoveryWaiverService.Waiver> waive(@PathVariable String customerId,
            @RequestBody PointsRecoveryWaiverService.Request request) {
        return ApiResponse.ok(operations.readSingle(AuditedOperation.of("points.recovery.waive_completed","points.recovery.waive_failed",
                PointsRecoveryWaiverService.WAIVE,"customer_points_recovery"),()->service.waive(customerId,request),result->((PointsRecoveryWaiverService.Waiver)result).id()));
    }
}
