package com.complex.platform.amusement;

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.amusement.dto.ParkWideFaultConfirmRequest;
import com.complex.platform.amusement.dto.ParkWideFaultInitiateRequest;
import com.complex.platform.amusement.dto.ParkWideFaultLiftRequest;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** CG-08a 全园故障状态：发起 / 第二岗位确认生效 / 发起解除 / 第二岗位确认解除 / 查询 / 留痕。 */
@RestController
@RequestMapping("/api/v1/amusement/park-wide-faults")
public class AmusementParkWideFaultController {

    private final AmusementParkWideFaultService service;

    public AmusementParkWideFaultController(AmusementParkWideFaultService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<AmusementParkWideFaultRecord>> list(
            @RequestParam(name = "project_id", required = false) String projectId,
            @RequestParam(name = "status", required = false) String status) {
        return ApiResponse.ok(service.list(projectId, status));
    }

    @PostMapping
    public ApiResponse<AmusementParkWideFaultRecord> initiate(@RequestBody ParkWideFaultInitiateRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.initiate(request));
    }

    @PostMapping("/{fault_id}/confirm-activation")
    public ApiResponse<AmusementParkWideFaultRecord> confirmActivation(@PathVariable("fault_id") String faultId,
            @RequestBody ParkWideFaultConfirmRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.confirmActivation(faultId, request));
    }

    @PostMapping("/{fault_id}/initiate-lift")
    public ApiResponse<AmusementParkWideFaultRecord> initiateLift(@PathVariable("fault_id") String faultId,
            @RequestBody ParkWideFaultLiftRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.initiateLift(faultId, request));
    }

    @PostMapping("/{fault_id}/confirm-lift")
    public ApiResponse<AmusementParkWideFaultRecord> confirmLift(@PathVariable("fault_id") String faultId,
            @RequestBody ParkWideFaultConfirmRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.confirmLift(faultId, request));
    }

    @GetMapping("/{fault_id}/actions")
    public ApiResponse<PageResponse<AmusementParkWideFaultActionRecord>> listActions(@PathVariable("fault_id") String faultId) {
        return ApiResponse.ok(service.listActions(faultId));
    }
}
