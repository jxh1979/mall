package com.complex.platform.floorplan;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.floorplan.dto.FloorPlanCreateRequest;
import com.complex.platform.floorplan.dto.FloorPlanImportCreateRequest;
import com.complex.platform.floorplan.dto.FloorPlanImportPublishRequest;
import com.complex.platform.floorplan.dto.FloorPlanShapeSaveRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/floor-plans")
public class FloorPlanController {

    private final FloorPlanService service;
    private final AuthorizationService authorizationService;

    public FloorPlanController(FloorPlanService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<FloorPlanRecord>> listFloorPlans() {
        authorizationService.requirePermission("floor_plan.view");
        return ApiResponse.ok(service.listFloorPlans());
    }

    @PostMapping
    public ApiResponse<FloorPlanRecord> createFloorPlan(@RequestBody FloorPlanCreateRequest request) {
        authorizationService.requirePermission("floor_plan.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createFloorPlan(request));
    }

    @GetMapping("/{id}/shapes")
    public ApiResponse<PageResponse<FloorPlanShapeRecord>> listShapes(@PathVariable String id) {
        authorizationService.requirePermission("floor_plan.view");
        return ApiResponse.ok(service.listShapes(id));
    }

    @PostMapping("/{id}/shapes")
    public ApiResponse<FloorPlanShapeRecord> saveShape(
            @PathVariable String id,
            @RequestBody FloorPlanShapeSaveRequest request
    ) {
        authorizationService.requirePermission("floor_plan.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.saveShape(id, request));
    }

    @GetMapping("/{id}/imports")
    public ApiResponse<PageResponse<FloorPlanImportTaskRecord>> listImports(@PathVariable String id) {
        authorizationService.requirePermission("floor_plan.import.view");
        return ApiResponse.ok(service.listImportTasks(id));
    }

    @PostMapping("/{id}/imports")
    public ApiResponse<FloorPlanImportTaskRecord> createImport(
            @PathVariable String id,
            @RequestBody FloorPlanImportCreateRequest request
    ) {
        authorizationService.requirePermission("floor_plan.import.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createImportTask(id, request));
    }

    @GetMapping("/{id}/imports/{importId}/candidates")
    public ApiResponse<FloorPlanImportDetailsResponse> listImportCandidates(
            @PathVariable String id,
            @PathVariable String importId
    ) {
        authorizationService.requirePermission("floor_plan.import.view");
        return ApiResponse.ok(service.getImportDetails(id, importId));
    }

    @PostMapping("/{id}/imports/{importId}/publish")
    public ApiResponse<FloorPlanImportTaskRecord> publishImport(
            @PathVariable String id,
            @PathVariable String importId,
            @RequestBody FloorPlanImportPublishRequest request
    ) {
        authorizationService.requirePermission("floor_plan.import.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.publishImportTask(id, importId, request));
    }

    private void requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "idempotency_key is required");
        }
    }
}
