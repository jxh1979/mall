package com.complex.platform.amusement;

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.amusement.dto.CompensationBatchConfirmRequest;
import com.complex.platform.amusement.dto.CompensationBatchInitiateRequest;
import com.complex.platform.amusement.dto.CompensationPolicyRequest;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** CG-08b 全园故障补偿：有效期参数、补偿批次发起/第二岗位确认发放、批次与补偿记录查询。 */
@RestController
@RequestMapping("/api/v1/amusement")
public class AmusementFaultCompensationController {

    private final AmusementFaultCompensationService service;

    public AmusementFaultCompensationController(AmusementFaultCompensationService service) {
        this.service = service;
    }

    @GetMapping("/compensation-policies/{project_id}")
    public ApiResponse<AmusementCompensationPolicyRecord> getPolicy(@PathVariable("project_id") String projectId) {
        return ApiResponse.ok(service.getPolicy(projectId));
    }

    @PostMapping("/compensation-policies/{project_id}")
    public ApiResponse<AmusementCompensationPolicyRecord> configurePolicy(@PathVariable("project_id") String projectId,
            @RequestBody CompensationPolicyRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.configurePolicy(projectId, request));
    }

    @GetMapping("/park-wide-faults/{fault_id}/compensation-batches")
    public ApiResponse<PageResponse<AmusementFaultCompensationBatchRecord>> listBatches(@PathVariable("fault_id") String faultId) {
        return ApiResponse.ok(service.listBatches(faultId));
    }

    @PostMapping("/park-wide-faults/{fault_id}/compensation-batches")
    public ApiResponse<AmusementFaultCompensationBatchRecord> initiateBatch(@PathVariable("fault_id") String faultId,
            @RequestBody CompensationBatchInitiateRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.initiateBatch(faultId, request));
    }

    /** TK-18：按事件类型列出补偿批次（场次提前结束批次 event_type=session_early_end，无全园故障可查）。 */
    @GetMapping("/compensation-batches")
    public ApiResponse<PageResponse<AmusementFaultCompensationBatchRecord>> listBatchesByEvent(
            @RequestParam(name = "event_type", required = false) String eventType,
            @RequestParam(name = "project_id", required = false) String projectId) {
        return ApiResponse.ok(service.listBatchesByEvent(eventType, projectId));
    }

    @PostMapping("/compensation-batches/{batch_id}/confirm")
    public ApiResponse<AmusementFaultCompensationBatchRecord> confirmBatch(@PathVariable("batch_id") String batchId,
            @RequestBody CompensationBatchConfirmRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.confirmBatch(batchId, request));
    }

    @GetMapping("/compensation-batches/{batch_id}/items")
    public ApiResponse<PageResponse<AmusementFaultCompensationItemRecord>> listItems(@PathVariable("batch_id") String batchId) {
        return ApiResponse.ok(service.listItems(batchId));
    }
}
