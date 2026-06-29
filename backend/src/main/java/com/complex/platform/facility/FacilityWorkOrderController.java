package com.complex.platform.facility;

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.facility.dto.FacilityWorkOrderActionRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderAssignRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderPolicyRequest;
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
        requirePermission("facility.work_order.accept_assigned");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.accept(workOrderId, request));
    }

    @PostMapping("/{work_order_id}/review")
    public ApiResponse<FacilityWorkOrderRecord> review(
            @PathVariable("work_order_id") String workOrderId, @RequestBody @jakarta.validation.Valid FacilityWorkOrderActionRequest request) {
        authorizationService.requirePermission(PERM);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.review(workOrderId, request));
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
