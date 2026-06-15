package com.complex.platform.merchantservice;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderActionRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderAssignRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderCloseRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderCreateRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant-service")
public class MerchantServiceController {

    private final MerchantServiceService service;
    private final AuthorizationService authorizationService;

    public MerchantServiceController(
            MerchantServiceService service,
            AuthorizationService authorizationService
    ) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/work-orders")
    public ApiResponse<PageResponse<MerchantWorkOrderRecord>> listWorkOrders() {
        authorizationService.requirePermission("merchant.service_work_order.view");
        return ApiResponse.ok(service.listWorkOrders());
    }

    @PostMapping("/work-orders")
    public ApiResponse<MerchantWorkOrderRecord> createWorkOrder(@RequestBody MerchantWorkOrderCreateRequest request) {
        authorizationService.requirePermission("merchant.service_work_order.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createWorkOrder(request));
    }

    @PostMapping("/work-orders/{id}/assign")
    public ApiResponse<MerchantWorkOrderRecord> assignWorkOrder(
            @PathVariable String id,
            @RequestBody MerchantWorkOrderAssignRequest request
    ) {
        authorizationService.requirePermission("merchant.service_work_order.assign");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.assignWorkOrder(id, request));
    }

    @PostMapping("/work-orders/{id}/actions")
    public ApiResponse<MerchantWorkOrderRecord> addAction(
            @PathVariable String id,
            @RequestBody MerchantWorkOrderActionRequest request
    ) {
        authorizationService.requirePermission("merchant.service_work_order.process");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.addAction(id, request));
    }

    @PostMapping("/work-orders/{id}/close")
    public ApiResponse<MerchantWorkOrderRecord> closeWorkOrder(
            @PathVariable String id,
            @RequestBody MerchantWorkOrderCloseRequest request
    ) {
        authorizationService.requirePermission("merchant.service_work_order.close");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.closeWorkOrder(id, request));
    }

    private void requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "idempotency_key is required");
        }
    }
}
