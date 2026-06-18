package com.complex.platform.floorplan;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.floorplan.dto.FloorPlanCreateRequest;
import com.complex.platform.floorplan.dto.FloorPlanImportCreateRequest;
import com.complex.platform.floorplan.dto.FloorPlanImportPublishRequest;
import com.complex.platform.floorplan.dto.FloorPlanShapeSaveRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class FloorPlanService {

    private static final Set<String> STATUSES = Set.of("active", "inactive", "archived");
    private static final Set<String> SHAPE_TYPES = Set.of("area", "shop", "entrance", "facility", "risk_zone", "route");
    private static final Set<String> REF_TYPES = Set.of("space", "merchant", "project", "amusement_project", "device", "custom");
    private static final Set<String> IMPORT_SOURCE_FORMATS = Set.of("svg", "standard_json");
    private static final Set<String> IMPORT_TASK_STATUSES = Set.of("parsed", "failed", "published", "cancelled");
    private static final Set<String> IMPORT_CANDIDATE_STATUSES = Set.of("pending", "published", "rejected");

    private final FloorPlanRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final FloorPlanImportParser importParser;
    private final AuthorizationService authorizationService;

    public FloorPlanService(
            FloorPlanRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            FloorPlanImportParser importParser,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.importParser = importParser;
        this.authorizationService = authorizationService;
    }

    public PageResponse<FloorPlanRecord> listFloorPlans() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        List<FloorPlanRecord> records = scope.isPresent()
                ? repository.findAllFloorPlansByMerchant(scope.get())
                : repository.findAllFloorPlans();
        auditEventService.recordSuccess(
                "floor_plan.viewed",
                "floor_plan.view",
                "floor_plan",
                "all",
                requestId(),
                null,
                Map.of("count", records.size())
        );
        return new PageResponse<>(records, 1, 50, records.size());
    }

    public PageResponse<FloorPlanShapeRecord> listShapes(String floorPlanId) {
        String id = requireText(floorPlanId, "floor_plan_id");
        ensureFloorPlanExists(id);
        List<FloorPlanShapeRecord> records = repository.findShapesByFloorPlanId(id);
        auditEventService.recordSuccess(
                "floor_plan.shape_viewed",
                "floor_plan.view",
                "floor_plan_shape",
                id,
                requestId(),
                null,
                Map.of("count", records.size())
        );
        return new PageResponse<>(records, 1, 50, records.size());
    }

    public FloorPlanRecord createFloorPlan(FloorPlanCreateRequest request) {
        return executeWrite(
                "createFloorPlan",
                request == null ? null : request.idempotencyKey(),
                request,
                "floor_plan.create_failed",
                "floor_plan.manage",
                "new",
                () -> validateCreateRequest(request),
                () -> createFloorPlanOnce(request)
        );
    }

    public FloorPlanShapeRecord saveShape(String floorPlanId, FloorPlanShapeSaveRequest request) {
        String id = requireText(floorPlanId, "floor_plan_id");
        return executeShapeWrite(
                "saveFloorPlanShape:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                "floor_plan.shape_save_failed",
                "floor_plan.manage",
                id,
                () -> {
                    ensureFloorPlanExists(id);
                    validateShapeRequest(request);
                },
                () -> saveShapeOnce(id, request)
        );
    }

    public PageResponse<FloorPlanImportTaskRecord> listImportTasks(String floorPlanId) {
        String id = requireText(floorPlanId, "floor_plan_id");
        ensureFloorPlanExists(id);
        List<FloorPlanImportTaskRecord> records = repository.findImportTasksByFloorPlanId(id);
        auditEventService.recordSuccess(
                "floor_plan.import_viewed",
                "floor_plan.import.view",
                "floor_plan_import",
                id,
                requestId(),
                null,
                Map.of("count", records.size())
        );
        return new PageResponse<>(records, 1, 50, records.size());
    }

    public FloorPlanImportDetailsResponse getImportDetails(String floorPlanId, String importTaskId) {
        String planId = requireText(floorPlanId, "floor_plan_id");
        String taskId = requireText(importTaskId, "import_task_id");
        FloorPlanImportTaskRecord task = ensureImportTaskBelongsToFloorPlan(planId, taskId);
        List<FloorPlanImportCandidateRecord> candidates = repository.findImportCandidatesByTaskId(task.id());
        List<FloorPlanImportErrorRecord> errors = repository.findImportErrorsByTaskId(task.id());
        auditEventService.recordSuccess(
                "floor_plan.import_candidate_viewed",
                "floor_plan.import.view",
                "floor_plan_import",
                task.id(),
                requestId(),
                null,
                Map.of("candidate_count", candidates.size(), "error_count", errors.size())
        );
        return new FloorPlanImportDetailsResponse(task, candidates, errors);
    }

    public FloorPlanImportTaskRecord createImportTask(String floorPlanId, FloorPlanImportCreateRequest request) {
        String id = requireText(floorPlanId, "floor_plan_id");
        try {
            ensureFloorPlanExists(id);
            validateImportCreateRequest(request);
            return idempotencyService.execute(
                    "createFloorPlanImport:" + id,
                    request.idempotencyKey(),
                    request,
                    () -> createImportTaskOnce(id, request),
                    FloorPlanImportTaskRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "floor_plan.import_create_failed",
                    "floor_plan.import.manage",
                    "floor_plan_import",
                    id,
                    requestId(),
                    request == null ? null : request.idempotencyKey(),
                    errorCode(exception)
            );
            throw exception;
        }
    }

    public FloorPlanImportTaskRecord publishImportTask(
            String floorPlanId,
            String importTaskId,
            FloorPlanImportPublishRequest request
    ) {
        String planId = requireText(floorPlanId, "floor_plan_id");
        String taskId = requireText(importTaskId, "import_task_id");
        try {
            ensureImportTaskBelongsToFloorPlan(planId, taskId);
            validateImportPublishRequest(request);
            return idempotencyService.execute(
                    "publishFloorPlanImport:" + taskId,
                    request.idempotencyKey(),
                    request,
                    () -> publishImportTaskOnce(planId, taskId, request),
                    FloorPlanImportTaskRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "floor_plan.import_publish_failed",
                    "floor_plan.import.manage",
                    "floor_plan_import",
                    taskId,
                    requestId(),
                    request == null ? null : request.idempotencyKey(),
                    errorCode(exception)
            );
            throw exception;
        }
    }

    private FloorPlanRecord createFloorPlanOnce(FloorPlanCreateRequest request) {
        Instant now = Instant.now();
        Optional<String> scope = authorizationService.currentMerchantScope();
        FloorPlanRecord record = repository.saveFloorPlan(new FloorPlanRecord(
                "fp_" + UUID.randomUUID(),
                trimToNull(request.projectId()),
                trimToNull(request.buildingCode()),
                requireText(request.floorNo(), "floor_no"),
                requireText(request.planName(), "plan_name"),
                requireText(request.imageUrl(), "image_url"),
                request.imageWidth(),
                request.imageHeight(),
                requireText(request.versionNo(), "version_no"),
                normalizeAllowed(request.status(), "status", STATUSES),
                now,
                now,
                scope.orElse(null)
        ));
        auditEventService.recordSuccess(
                "floor_plan.created",
                "floor_plan.manage",
                "floor_plan",
                record.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("status", record.status())
        );
        return record;
    }

    private FloorPlanShapeRecord saveShapeOnce(String floorPlanId, FloorPlanShapeSaveRequest request) {
        String requestedShapeId = trimToNull(request.shapeId());
        FloorPlanShapeRecord existing = requestedShapeId == null
                ? null
                : repository.findShapeById(requestedShapeId).orElse(null);
        if (existing != null && !existing.floorPlanId().equals(floorPlanId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "shape does not belong to this floor plan");
        }
        Instant now = Instant.now();
        FloorPlanShapeRecord record = repository.saveShape(new FloorPlanShapeRecord(
                requestedShapeId == null ? "fps_" + slug(request.idempotencyKey()) : requestedShapeId,
                floorPlanId,
                normalizeAllowed(request.shapeType(), "shape_type", SHAPE_TYPES),
                normalizeAllowed(request.refType(), "ref_type", REF_TYPES),
                trimToNull(request.refId()),
                requireText(request.label(), "label"),
                List.copyOf(request.polygonPoints()),
                request.zIndex() == null ? 0 : request.zIndex(),
                normalizeAllowed(request.status(), "status", STATUSES),
                existing == null ? now : existing.createdAt(),
                now
        ));
        auditEventService.recordSuccess(
                "floor_plan.shape_saved",
                "floor_plan.manage",
                "floor_plan_shape",
                record.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("floor_plan_id", floorPlanId, "status", record.status())
        );
        return record;
    }

    private FloorPlanImportTaskRecord createImportTaskOnce(String floorPlanId, FloorPlanImportCreateRequest request) {
        Instant now = Instant.now();
        FloorPlanImportParseResult parseResult = importParser.parse(request.sourceFormat(), request.sourcePayload());
        String taskId = "fpi_" + slug(request.idempotencyKey());
        String status = parseResult.candidates().isEmpty() ? "failed" : "parsed";
        FloorPlanImportTaskRecord task = repository.saveImportTask(new FloorPlanImportTaskRecord(
                taskId,
                floorPlanId,
                normalizeAllowed(request.sourceFormat(), "source_format", IMPORT_SOURCE_FORMATS),
                requireText(request.sourceName(), "source_name"),
                sha256(request.sourcePayload()),
                request.sourcePayload(),
                normalizeAllowed(status, "status", IMPORT_TASK_STATUSES),
                parseResult.candidates().size(),
                parseResult.errors().size(),
                trimToNull(request.operatorNote()),
                now,
                now
        ));

        int candidateNo = 1;
        for (FloorPlanImportParseResult.CandidateDraft candidate : parseResult.candidates()) {
            repository.saveImportCandidate(new FloorPlanImportCandidateRecord(
                    "fpic_" + slug(taskId + "_" + candidateNo),
                    taskId,
                    candidateNo,
                    normalizeAllowed(candidate.shapeType(), "shape_type", SHAPE_TYPES),
                    normalizeAllowed(candidate.refType(), "ref_type", REF_TYPES),
                    trimToNull(candidate.refId()),
                    requireText(candidate.label(), "label"),
                    List.copyOf(candidate.polygonPoints()),
                    candidate.zIndex() == null ? candidateNo : candidate.zIndex(),
                    normalizeAllowed("pending", "status", IMPORT_CANDIDATE_STATUSES),
                    now,
                    now
            ));
            candidateNo++;
        }

        int errorNo = 1;
        for (FloorPlanImportParseResult.ErrorDraft error : parseResult.errors()) {
            repository.saveImportError(new FloorPlanImportErrorRecord(
                    "fpie_" + slug(taskId + "_" + errorNo),
                    taskId,
                    error.lineNo(),
                    trimToNull(error.fieldName()),
                    requireText(error.errorCode(), "error_code"),
                    requireText(error.errorMessage(), "error_message"),
                    now
            ));
            errorNo++;
        }

        auditEventService.recordSuccess(
                "floor_plan.import_created",
                "floor_plan.import.manage",
                "floor_plan_import",
                task.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("status", task.status(), "candidate_count", task.candidateCount(), "error_count", task.errorCount())
        );
        return task;
    }

    private FloorPlanImportTaskRecord publishImportTaskOnce(
            String floorPlanId,
            String importTaskId,
            FloorPlanImportPublishRequest request
    ) {
        FloorPlanImportTaskRecord task = ensureImportTaskBelongsToFloorPlan(floorPlanId, importTaskId);
        if (!"parsed".equals(task.status())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "only parsed import task can be published");
        }

        List<String> candidateIds = request.candidateIds().stream()
                .map(this::trimToNull)
                .filter(value -> value != null)
                .distinct()
                .toList();
        if (candidateIds.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "candidate_ids is required");
        }

        List<FloorPlanImportCandidateRecord> candidates = new ArrayList<>();
        for (String candidateId : candidateIds) {
            FloorPlanImportCandidateRecord candidate = repository.findImportCandidateById(candidateId)
                    .orElseThrow(() -> ApiException.notFound("IMPORT_CANDIDATE_NOT_FOUND", "import candidate not found"));
            if (!candidate.importTaskId().equals(importTaskId)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "candidate does not belong to import task");
            }
            if (!"pending".equals(candidate.status())) {
                throw ApiException.badRequest("VALIDATION_FAILED", "only pending candidate can be published");
            }
            candidates.add(candidate);
        }

        Instant now = Instant.now();
        for (FloorPlanImportCandidateRecord candidate : candidates) {
            repository.saveShape(new FloorPlanShapeRecord(
                    "fps_" + slug(importTaskId + "_" + candidate.id()),
                    floorPlanId,
                    candidate.shapeType(),
                    candidate.refType(),
                    candidate.refId(),
                    candidate.label(),
                    candidate.polygonPoints(),
                    candidate.zIndex(),
                    "active",
                    now,
                    now
            ));
            repository.saveImportCandidate(new FloorPlanImportCandidateRecord(
                    candidate.id(),
                    candidate.importTaskId(),
                    candidate.candidateNo(),
                    candidate.shapeType(),
                    candidate.refType(),
                    candidate.refId(),
                    candidate.label(),
                    candidate.polygonPoints(),
                    candidate.zIndex(),
                    "published",
                    candidate.createdAt(),
                    now
            ));
        }

        FloorPlanImportTaskRecord published = repository.saveImportTask(new FloorPlanImportTaskRecord(
                task.id(),
                task.floorPlanId(),
                task.sourceFormat(),
                task.sourceName(),
                task.sourceHash(),
                task.sourcePayload(),
                "published",
                task.candidateCount(),
                task.errorCount(),
                task.operatorNote(),
                task.createdAt(),
                now
        ));
        auditEventService.recordSuccess(
                "floor_plan.import_published",
                "floor_plan.import.manage",
                "floor_plan_import",
                published.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("published_count", candidates.size())
        );
        return published;
    }

    private FloorPlanRecord executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            String failureEvent,
            String permissionCode,
            String businessId,
            Runnable preValidation,
            Supplier<FloorPlanRecord> action
    ) {
        try {
            preValidation.run();
            return idempotencyService.execute(operationId, idempotencyKey, request, action, FloorPlanRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    "floor_plan",
                    businessId,
                    requestId(),
                    idempotencyKey,
                    errorCode(exception)
            );
            throw exception;
        }
    }

    private FloorPlanShapeRecord executeShapeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            String failureEvent,
            String permissionCode,
            String businessId,
            Runnable preValidation,
            Supplier<FloorPlanShapeRecord> action
    ) {
        try {
            preValidation.run();
            return idempotencyService.execute(operationId, idempotencyKey, request, action, FloorPlanShapeRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    "floor_plan_shape",
                    businessId,
                    requestId(),
                    idempotencyKey,
                    errorCode(exception)
            );
            throw exception;
        }
    }

    private void validateCreateRequest(FloorPlanCreateRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.floorNo(), "floor_no");
        requireText(request.planName(), "plan_name");
        requireText(request.imageUrl(), "image_url");
        requireText(request.versionNo(), "version_no");
        requirePositive(request.imageWidth(), "image_width");
        requirePositive(request.imageHeight(), "image_height");
        normalizeAllowed(request.status(), "status", STATUSES);
    }

    private void validateShapeRequest(FloorPlanShapeSaveRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.label(), "label");
        normalizeAllowed(request.shapeType(), "shape_type", SHAPE_TYPES);
        normalizeAllowed(request.refType(), "ref_type", REF_TYPES);
        normalizeAllowed(request.status(), "status", STATUSES);
        List<FloorPlanPoint> points = request.polygonPoints();
        if (points == null || points.size() < 3) {
            throw ApiException.badRequest("VALIDATION_FAILED", "polygon_points must contain at least 3 points");
        }
        for (FloorPlanPoint point : points) {
            if (point == null || point.x() < 0 || point.x() > 1 || point.y() < 0 || point.y() > 1) {
                throw ApiException.badRequest("VALIDATION_FAILED", "polygon point x/y must be between 0 and 1");
            }
        }
    }

    private void validateImportCreateRequest(FloorPlanImportCreateRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        normalizeAllowed(request.sourceFormat(), "source_format", IMPORT_SOURCE_FORMATS);
        requireText(request.sourceName(), "source_name");
        String payload = requireText(request.sourcePayload(), "source_payload");
        if (payload.length() > 1_000_000) {
            throw ApiException.badRequest("VALIDATION_FAILED", "source_payload exceeds 1000000 characters");
        }
    }

    private void validateImportPublishRequest(FloorPlanImportPublishRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        if (request.candidateIds() == null || request.candidateIds().isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "candidate_ids is required");
        }
    }

    private FloorPlanRecord ensureFloorPlanExists(String id) {
        FloorPlanRecord fp = repository.findFloorPlanById(id)
                .orElseThrow(() -> ApiException.notFound("FLOOR_PLAN_NOT_FOUND", "floor plan not found"));
        checkMerchantOwnership(fp.merchantId());
        return fp;
    }

    private void checkMerchantOwnership(String resourceMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "floor plan belongs to a different merchant");
        }
    }

    private FloorPlanImportTaskRecord ensureImportTaskBelongsToFloorPlan(String floorPlanId, String importTaskId) {
        ensureFloorPlanExists(floorPlanId);
        FloorPlanImportTaskRecord task = repository.findImportTaskById(importTaskId)
                .orElseThrow(() -> ApiException.notFound("IMPORT_TASK_NOT_FOUND", "import task not found"));
        if (!task.floorPlanId().equals(floorPlanId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "import task does not belong to floor plan");
        }
        return task;
    }

    private void requirePositive(Integer value, String field) {
        if (value == null || value <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " must be greater than 0");
        }
    }

    private String normalizeAllowed(String value, String field, Set<String> allowed) {
        String normalized = requireText(value, field).toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is invalid");
        }
        return normalized;
    }

    private String requireText(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String slug(String value) {
        return requireText(value, "idempotency_key").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "_");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(requireText(value, "source_payload").getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("source payload hashing unavailable", exception);
        }
    }

    private String requestId() {
        return UUID.randomUUID().toString();
    }

    private String errorCode(RuntimeException exception) {
        return exception instanceof ApiException apiException ? apiException.code() : "INTERNAL_ERROR";
    }
}
