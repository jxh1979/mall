package com.complex.platform.ops.productionreadiness;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.ops.productionreadiness.dto.ProductionReadinessCheckRequest;
import com.complex.platform.ops.productionreadiness.dto.ProductionReadinessCheckResponse;
import com.complex.platform.ops.productionreadiness.dto.ProductionReadinessItemResponse;
import com.complex.platform.ops.productionreadiness.dto.ProductionReadinessItemUpdateRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops/production-readiness")
public class ProductionReadinessController {

    private final ProductionReadinessService service;
    private final AuthorizationService authorizationService;

    public ProductionReadinessController(
            ProductionReadinessService service,
            AuthorizationService authorizationService
    ) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/items")
    public ApiResponse<PageResponse<ProductionReadinessItemResponse>> listItems() {
        authorizationService.requirePermission("ops.production_readiness.view");
        return ApiResponse.ok(service.listItems());
    }

    @PutMapping("/items/{id}")
    public ApiResponse<ProductionReadinessItemResponse> updateItem(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid ProductionReadinessItemUpdateRequest request
    ) {
        authorizationService.requirePermission("ops.production_readiness.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.updateItem(id, request));
    }

    @PostMapping("/checks")
    public ApiResponse<ProductionReadinessCheckResponse> createCheck(
            @RequestBody @jakarta.validation.Valid ProductionReadinessCheckRequest request
    ) {
        authorizationService.requirePermission("ops.production_readiness.check");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createCheck(request));
    }
}
