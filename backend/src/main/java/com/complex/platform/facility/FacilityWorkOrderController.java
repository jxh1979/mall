package com.complex.platform.facility;

// 文件版本：V0.04

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.facility.dto.FacilityWorkOrderActionRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderAssignRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderConfirmerRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderPolicyRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderReassignRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderRequest;
import com.complex.platform.facility.dto.WorkOrderMaterialRequests;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 楼宇维保/物业工单 API（③楼宇运营 S2，V166）。 */
@RestController
@RequestMapping("/api/v1/facility/work-orders")
public class FacilityWorkOrderController {

    private static final String PERM = "facility.work_order.manage";

    private final FacilityWorkOrderService service;
    private final AuthorizationService authorizationService;
    private final AuditedOperationExecutor operations;

    // 第十八批 N 段第 124 条 124-R3（PGM L738）：写口任一步失败（权限、校验、状态冲突）经 AuditedOperationExecutor.recordingFailure
    // 落 OpenAPI 声明的 x-audit-event-failure 再原样抛出；成功事件照旧由服务层写。
    public FacilityWorkOrderController(FacilityWorkOrderService service, AuthorizationService authorizationService,
            AuditedOperationExecutor operations) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.operations = operations;
    }

    @GetMapping
    public ApiResponse<PageResponse<FacilityWorkOrderRecord>> list() {
        requirePermission("facility.work_order.view_assigned");
        return ApiResponse.ok(service.list());
    }

    @GetMapping("/assigned-to-me")
    public ApiResponse<PageResponse<FacilityWorkOrderRecord>> listAssignedToMe() {
        return ApiResponse.ok(service.listAssignedToMe());
    }

    @GetMapping("/overdue")
    public ApiResponse<PageResponse<FacilityWorkOrderRecord>> overdue() {
        authorizationService.requirePermission(PERM);
        return ApiResponse.ok(service.listOverdue());
    }

    @GetMapping("/{work_order_id}")
    public ApiResponse<FacilityWorkOrderRecord> get(@PathVariable("work_order_id") String workOrderId) {
        requirePermission("facility.work_order.view_assigned");
        return ApiResponse.ok(service.findById(workOrderId));
    }

    // ROOT-03 clients-R8：客人投诉附件（照片、视频、语音）按工单查看权限受控查看。字段注入，不改既有两参构造（单测直接 new）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CustomerComplaintService complaints;

    @GetMapping("/{work_order_id}/customer-media")
    public ApiResponse<java.util.List<CustomerComplaintService.MediaView>> listCustomerMedia(
            @PathVariable("work_order_id") String workOrderId) {
        authorizationService.requirePermission(PERM);
        return ApiResponse.ok(complaints.listMedia(workOrderId));
    }

    @GetMapping("/{work_order_id}/customer-media/{media_id}/file")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> readCustomerMedia(
            @PathVariable("work_order_id") String workOrderId, @PathVariable("media_id") String mediaId) {
        authorizationService.requirePermission(PERM);
        java.util.Map.Entry<java.nio.file.Path, String> file = complaints.readMedia(workOrderId, mediaId);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(file.getValue()))
                .body(new org.springframework.core.io.FileSystemResource(file.getKey()));
    }

    @PostMapping
    public ApiResponse<FacilityWorkOrderRecord> create(@RequestBody @jakarta.validation.Valid FacilityWorkOrderRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.work_order_create_failed", "facility.work_order.manage",
                "facility_work_order", "new", key, () -> {
                    authorizationService.requirePermission(PERM);
                    requireIdempotencyKey(key);
                    return service.create(request);
                }));
    }

    @PostMapping("/{work_order_id}/assign")
    public ApiResponse<FacilityWorkOrderRecord> assign(
            @PathVariable("work_order_id") String workOrderId, @RequestBody @jakarta.validation.Valid FacilityWorkOrderAssignRequest request) {
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.assign(workOrderId, request));
    }

    /** 第十二批裁决第 9 条「补转派」：已派单或处理中的工单由物业主管改派给他人，处理时限不重算，留痕原处理人、新处理人和原因。 */
    @PostMapping("/{work_order_id}/reassign")
    public ApiResponse<FacilityWorkOrderRecord> reassign(
            @PathVariable("work_order_id") String workOrderId, @RequestBody @jakarta.validation.Valid FacilityWorkOrderReassignRequest request) {
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reassign(workOrderId, request));
    }

    @PostMapping("/{work_order_id}/start")
    public ApiResponse<FacilityWorkOrderRecord> start(
            @PathVariable("work_order_id") String workOrderId, @RequestBody @jakarta.validation.Valid FacilityWorkOrderActionRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.work_order_start_failed", "facility.work_order.manage",
                "facility_work_order", workOrderId, key, () -> {
                    requirePermission("facility.work_order.process_assigned");
                    requireIdempotencyKey(key);
                    return service.start(workOrderId, request);
                }));
    }

    @PostMapping("/{work_order_id}/complete")
    public ApiResponse<FacilityWorkOrderRecord> complete(
            @PathVariable("work_order_id") String workOrderId, @RequestBody @jakarta.validation.Valid FacilityWorkOrderActionRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.work_order_complete_failed", "facility.work_order.manage",
                "facility_work_order", workOrderId, key, () -> {
                    requirePermission("facility.work_order.process_assigned");
                    requireIdempotencyKey(key);
                    return service.complete(workOrderId, request);
                }));
    }

    @PostMapping("/{work_order_id}/accept")
    public ApiResponse<FacilityWorkOrderRecord> accept(
            @PathVariable("work_order_id") String workOrderId, @RequestBody @jakarta.validation.Valid FacilityWorkOrderActionRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.work_order_accept_failed", "facility.work_order.manage",
                "facility_work_order", workOrderId, key, () -> {
                    authorizationService.requirePermission(PERM);
                    requireIdempotencyKey(key);
                    return service.accept(workOrderId, request);
                }));
    }

    @PostMapping("/{work_order_id}/review")
    public ApiResponse<FacilityWorkOrderRecord> review(
            @PathVariable("work_order_id") String workOrderId, @RequestBody @jakarta.validation.Valid FacilityWorkOrderActionRequest request) {
        // 权限在服务层核：持 facility.work_order.manage，或本人即设备告警自动工单的提单确认承接人（BSM L1385）。
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.review(workOrderId, request));
    }

    /** 第十二批裁决第 2 条：验收或提单确认不通过，填原因退回原维修人返工（回「处理中」，另一方已做的确认作废）。 */
    @PostMapping("/{work_order_id}/rework")
    public ApiResponse<FacilityWorkOrderRecord> rework(
            @PathVariable("work_order_id") String workOrderId, @RequestBody @jakarta.validation.Valid FacilityWorkOrderActionRequest request) {
        // 权限在服务层核：同 review（主管验收不通过由持 manage 者退回，提单确认不通过由承接人本人退回）。
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.rework(workOrderId, request));
    }

    /** HP-F02：自动生成的大型维修工单，设备责任人不在岗或本人维修时由物业主管指定提单确认人。 */
    @PostMapping("/{work_order_id}/confirmer")
    public ApiResponse<FacilityWorkOrderRecord> designateConfirmer(
            @PathVariable("work_order_id") String workOrderId, @RequestBody @jakarta.validation.Valid FacilityWorkOrderConfirmerRequest request) {
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.designateConfirmer(workOrderId, request));
    }

    @PostMapping("/{work_order_id}/suspend")
    public ApiResponse<FacilityWorkOrderRecord> suspend(
            @PathVariable("work_order_id") String workOrderId, @RequestBody @jakarta.validation.Valid FacilityWorkOrderActionRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.work_order_suspend_failed", "facility.work_order.manage",
                "facility_work_order", workOrderId, key, () -> {
                    authorizationService.requirePermission(PERM);
                    requireIdempotencyKey(key);
                    return service.suspend(workOrderId, request);
                }));
    }

    @PostMapping("/{work_order_id}/resume")
    public ApiResponse<FacilityWorkOrderRecord> resume(
            @PathVariable("work_order_id") String workOrderId, @RequestBody @jakarta.validation.Valid FacilityWorkOrderActionRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.work_order_resume_failed", "facility.work_order.manage",
                "facility_work_order", workOrderId, key, () -> {
                    authorizationService.requirePermission(PERM);
                    requireIdempotencyKey(key);
                    return service.resume(workOrderId, request);
                }));
    }

    /** 第十八批 M 段第 117 条（BSM:1380）：工单用料面板——用料明细与合计、费用承担方、承担方争议记录、本项目平台库房余额行与经营商户下拉。 */
    @GetMapping("/{work_order_id}/materials")
    public ApiResponse<WorkOrderMaterialService.Materials> materials(@PathVariable("work_order_id") String workOrderId) {
        authorizationService.requirePermission(PERM);
        return ApiResponse.ok(service.materials(workOrderId));
    }

    /** 第十八批 M 段第 117 条：登记用料——从所选库位出库一次，按当前平均进货价计成本、挂本工单（只限处理中）。 */
    @PostMapping("/{work_order_id}/materials")
    public ApiResponse<WorkOrderMaterialService.Materials> issueMaterial(
            @PathVariable("work_order_id") String workOrderId, @RequestBody WorkOrderMaterialRequests.Issue request) {
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.issueMaterial(workOrderId, request));
    }

    /** 第十八批 M 段第 117 条：标明费用承担方（物业承担／商户承担＋商户）。 */
    @PostMapping("/{work_order_id}/cost-bearer")
    public ApiResponse<WorkOrderMaterialService.Materials> markCostBearer(
            @PathVariable("work_order_id") String workOrderId, @RequestBody WorkOrderMaterialRequests.Bearer request) {
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.markCostBearer(workOrderId, request));
    }

    /** 第十八批 M 段第 117 条 117-R2：承担方有争议——工单挂起，转上级在审批链判定（意见即判定依据）。 */
    @PostMapping("/{work_order_id}/bearer-dispute")
    public ApiResponse<FacilityWorkOrderRecord> disputeBearer(
            @PathVariable("work_order_id") String workOrderId, @RequestBody WorkOrderMaterialRequests.Dispute request) {
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.disputeBearer(workOrderId, request));
    }

    @PostMapping("/mark-overdue")
    public ApiResponse<Integer> markOverdue(
            @RequestParam(value = "idempotency_key", required = false) String idempotencyKey) {
        return ApiResponse.ok(operations.recordingFailure("facility.work_order_overdue_mark_failed", "facility.work_order.manage",
                "facility_work_order", "all", idempotencyKey, () -> {
                    authorizationService.requirePermission(PERM);
                    requireIdempotencyKey(idempotencyKey);
                    return service.markOverdue(idempotencyKey);
                }));
    }

    @PostMapping("/policies")
    public ApiResponse<FacilityWorkOrderPolicyRecord> savePolicy(@RequestBody @jakarta.validation.Valid FacilityWorkOrderPolicyRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("facility.work_order_policy_save_failed", "facility.work_order.manage",
                "facility_work_order_policy", "new", key, () -> {
                    authorizationService.requirePermission(PERM);
                    requireIdempotencyKey(key);
                    return service.savePolicy(request);
                }));
    }

    private void requirePermission(String outsourcedPermission) {
        AccessActor actor = authorizationService.currentActor();
        authorizationService.requirePermission(
                authorizationService.isOutsourcedVendor(actor) ? outsourcedPermission : PERM);
    }
}
