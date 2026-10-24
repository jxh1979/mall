package com.complex.platform.facility;

// 文件版本：V0.02

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.facility.dto.FacilityWorkOrderActionRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderAssignRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderConfirmerRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderPolicyRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderReassignRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderRequest;
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

    public FacilityWorkOrderController(FacilityWorkOrderService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
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
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.create(request));
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
        requirePermission("facility.work_order.process_assigned");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.start(workOrderId, request));
    }

    @PostMapping("/{work_order_id}/complete")
    public ApiResponse<FacilityWorkOrderRecord> complete(
            @PathVariable("work_order_id") String workOrderId, @RequestBody @jakarta.validation.Valid FacilityWorkOrderActionRequest request) {
        requirePermission("facility.work_order.process_assigned");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.complete(workOrderId, request));
    }

    @PostMapping("/{work_order_id}/accept")
    public ApiResponse<FacilityWorkOrderRecord> accept(
            @PathVariable("work_order_id") String workOrderId, @RequestBody @jakarta.validation.Valid FacilityWorkOrderActionRequest request) {
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.accept(workOrderId, request));
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
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.suspend(workOrderId, request));
    }

    @PostMapping("/{work_order_id}/resume")
    public ApiResponse<FacilityWorkOrderRecord> resume(
            @PathVariable("work_order_id") String workOrderId, @RequestBody @jakarta.validation.Valid FacilityWorkOrderActionRequest request) {
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.resume(workOrderId, request));
    }

    @PostMapping("/mark-overdue")
    public ApiResponse<Integer> markOverdue(
            @RequestParam(value = "idempotency_key", required = false) String idempotencyKey) {
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(idempotencyKey);
        return ApiResponse.ok(service.markOverdue(idempotencyKey));
    }

    @PostMapping("/policies")
    public ApiResponse<FacilityWorkOrderPolicyRecord> savePolicy(@RequestBody @jakarta.validation.Valid FacilityWorkOrderPolicyRequest request) {
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.savePolicy(request));
    }

    private void requirePermission(String outsourcedPermission) {
        AccessActor actor = authorizationService.currentActor();
        authorizationService.requirePermission(
                authorizationService.isOutsourcedVendor(actor) ? outsourcedPermission : PERM);
    }
}
