package com.complex.platform.floorplan;
// 文件版本：V0.03

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
import static com.complex.platform.common.Strings.trimToNull;
import com.complex.platform.common.Strings;
import static com.complex.platform.common.Strings.requireText;

@Service
public class FloorPlanService {

    private static final Set<String> STATUSES = Set.of("active", "inactive", "archived");
    // M016-002（M015-013 同批）：与 V423 CHECK 同闭集——治理地图候选区域「铺位、摊位、公共区」= shop / booth / public_area。
    private static final Set<String> SHAPE_TYPES = Set.of("area", "shop", "entrance", "facility", "risk_zone", "route", "booth", "public_area");
    private static final Set<String> REF_TYPES = Set.of("space", "merchant", "project", "amusement_project", "device", "custom");
    private static final Set<String> IMPORT_SOURCE_FORMATS = Set.of("svg", "standard_json");
    private static final Set<String> IMPORT_TASK_STATUSES = Set.of("parsed", "failed", "published", "cancelled");
    private static final Set<String> IMPORT_CANDIDATE_STATUSES = Set.of("pending", "published", "rejected");

    // F-CROSS-10 导入边界（DoS/资源耗尽防护）：除既有 source_payload 字符上限（1_000_000，见
    // validateImportCreateRequest）外，再限解析出的候选形状（节点）数与单形状顶点数。
    // 一个 <1MB 的 payload 仍可塞入海量小多边形或单个超多顶点多边形撑爆解析/落库，故 parse 后强制上限。
    private static final int MAX_IMPORT_CANDIDATES = 2_000;
    private static final int MAX_IMPORT_POLYGON_POINTS = 1_000;
    // M015-008：发布一次最多 200 个候选（与 OpenAPI FloorPlanImportPublishRequest.candidate_ids maxItems 一致）。
    private static final int MAX_PUBLISH_CANDIDATES = 200;
    // M015-005：本模块校验文案中文整句，不经 FieldLabels 通用键翻译（candidate_ids/plan_name 在通用表中另有含义）。
    private static final Map<String, String> FIELD_LABELS = Map.of(
            "status", "状态", "shape_type", "图形类型", "ref_type", "关联类型", "source_format", "导入格式");

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
        Optional<String> merchantScope = authorizationService.currentMerchantIdQuietly();
        // M015-003：商户 scope 优先（用永不抛的 currentMerchantIdQuietly，项目员工才能走到项目 scope）；否则按项目 scope（BEQR-152）；平台管理员全量。
        List<FloorPlanRecord> records = merchantScope.isPresent()
                ? repository.findAllFloorPlansByMerchant(merchantScope.get())
                : authorizationService.currentProjectScopeQuietly()
                        .map(repository::findAllFloorPlansByProject)
                        .orElseGet(repository::findAllFloorPlans);
        auditEventService.recordSuccess(
                "floor_plan.viewed",
                "floor_plan.view",
                "floor_plan",
                "all",
                requestId(),
                null,
                Map.of("count", records.size())
        );
        return new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size());
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
        return new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size());
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
        return new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size());
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
        Optional<String> scope = authorizationService.currentMerchantIdQuietly();
        FloorPlanRecord record = repository.saveFloorPlan(new FloorPlanRecord(
                "fp_" + UUID.randomUUID(),
                requireText(request.projectId(), "project_id"),
                trimToNull(request.buildingCode()),
                requireText(request.floorNo(), "floor_no"),
                requireField(request.planName(), "平面图名称"),
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
            throw ApiException.badRequest("VALIDATION_FAILED", "图形不属于该平面图");
        }
        Instant now = Instant.now();
        FloorPlanShapeRecord record = repository.saveShape(new FloorPlanShapeRecord(
                requestedShapeId == null ? derivedId("fps_", floorPlanId + "_" + request.idempotencyKey()) : requestedShapeId,
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
        enforceImportLimits(parseResult);
        String taskId = derivedId("fpi_", floorPlanId + "_" + request.idempotencyKey());
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
            throw ApiException.badRequest("VALIDATION_FAILED", "只有解析完成的导入任务才能发布");
        }

        List<String> candidateIds = request.candidateIds().stream()
                .map(Strings::trimToNull)
                .filter(value -> value != null)
                .distinct()
                .toList();
        if (candidateIds.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "候选项列表不能为空");
        }

        List<FloorPlanImportCandidateRecord> candidates = new ArrayList<>();
        for (String candidateId : candidateIds) {
            FloorPlanImportCandidateRecord candidate = repository.findImportCandidateById(candidateId)
                    .orElseThrow(() -> ApiException.notFound("IMPORT_CANDIDATE_NOT_FOUND", "导入候选项不存在或已被删除"));
            if (!candidate.importTaskId().equals(importTaskId)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "候选项不属于该导入任务");
            }
            if (!"pending".equals(candidate.status())) {
                throw ApiException.badRequest("VALIDATION_FAILED", "只有待处理的候选项才能发布");
            }
            candidates.add(candidate);
        }

        Instant now = Instant.now();
        for (FloorPlanImportCandidateRecord candidate : candidates) {
            repository.saveShape(new FloorPlanShapeRecord(
                    derivedId("fps_", importTaskId + "_" + candidate.id()),
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
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        // M015-002/003：平面图必须归属已存在的综合体项目（PGM-017），且项目员工只能在本项目下创建（BEQR-152）。
        String projectId = requireText(request.projectId(), "project_id");
        if (!repository.projectExists(projectId)) {
            throw ApiException.notFound("PROJECT_NOT_FOUND", "综合体项目不存在或已被删除");
        }
        if (authorizationService.currentMerchantIdQuietly().isEmpty()) {
            authorizationService.currentProjectScopeQuietly().ifPresent(scope -> {
                if (!scope.equals(projectId)) {
                    throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "只能在当前综合体项目下创建平面图");
                }
            });
        }
        requireText(request.floorNo(), "floor_no");
        requireField(request.planName(), "平面图名称");
        requireText(request.imageUrl(), "image_url");
        requireText(request.versionNo(), "version_no");
        requirePositive(request.imageWidth(), "image_width");
        requirePositive(request.imageHeight(), "image_height");
        normalizeAllowed(request.status(), "status", STATUSES);
    }

    private void validateShapeRequest(FloorPlanShapeSaveRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.label(), "label");
        normalizeAllowed(request.shapeType(), "shape_type", SHAPE_TYPES);
        normalizeAllowed(request.refType(), "ref_type", REF_TYPES);
        normalizeAllowed(request.status(), "status", STATUSES);
        List<FloorPlanPoint> points = request.polygonPoints();
        if (points == null || points.size() < 3) {
            throw ApiException.badRequest("VALIDATION_FAILED", "多边形至少需要 3 个顶点");
        }
        for (FloorPlanPoint point : points) {
            if (point == null || point.x() < 0 || point.x() > 1 || point.y() < 0 || point.y() > 1) {
                throw ApiException.badRequest("VALIDATION_FAILED", "多边形顶点坐标 x/y 必须在 0 到 1 之间");
            }
        }
    }

    private void validateImportCreateRequest(FloorPlanImportCreateRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        normalizeAllowed(request.sourceFormat(), "source_format", IMPORT_SOURCE_FORMATS);
        requireText(request.sourceName(), "source_name");
        String payload = requireText(request.sourcePayload(), "source_payload");
        if (payload.length() > 1_000_000) {
            throw ApiException.badRequest("VALIDATION_FAILED", "导入内容超过 100 万字符上限");
        }
    }

    /**
     * F-CROSS-10 导入边界：解析完成后强制节点数 / 单形状顶点数上限，超限抛 badRequest（不落库）。
     * 防止 &lt;1MB 的 payload 经海量小多边形或单个超多顶点多边形撑爆解析结果与落库。
     */
    private void enforceImportLimits(FloorPlanImportParseResult parseResult) {
        List<FloorPlanImportParseResult.CandidateDraft> candidates = parseResult.candidates();
        if (candidates.size() > MAX_IMPORT_CANDIDATES) {
            throw ApiException.badRequest("FLOOR_PLAN_IMPORT_TOO_MANY_NODES",
                    "导入图形数量超过上限 " + MAX_IMPORT_CANDIDATES + " 个");
        }
        for (FloorPlanImportParseResult.CandidateDraft candidate : candidates) {
            int pointCount = candidate.polygonPoints() == null ? 0 : candidate.polygonPoints().size();
            if (pointCount > MAX_IMPORT_POLYGON_POINTS) {
                throw ApiException.badRequest("FLOOR_PLAN_IMPORT_TOO_MANY_POINTS",
                        "存在图形的顶点数超过上限 " + MAX_IMPORT_POLYGON_POINTS + " 个");
            }
        }
    }

    private void validateImportPublishRequest(FloorPlanImportPublishRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        if (request.candidateIds() == null || request.candidateIds().isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "候选项列表不能为空");
        }
        if (request.candidateIds().size() > MAX_PUBLISH_CANDIDATES) {
            throw ApiException.badRequest("VALIDATION_FAILED", "一次发布的候选项不能超过 " + MAX_PUBLISH_CANDIDATES + " 个");
        }
    }

    private FloorPlanRecord ensureFloorPlanExists(String id) {
        FloorPlanRecord fp = repository.findFloorPlanById(id)
                .orElseThrow(() -> ApiException.notFound("FLOOR_PLAN_NOT_FOUND", "平面图不存在或已被删除"));
        checkMerchantOwnership(fp.merchantId());
        checkProjectOwnership(fp.projectId());
        return fp;
    }

    // M015-003：项目员工只能访问本项目平面图（BEQR-152）；商户账号走商户闸、平台管理员 scope 为空放行。
    private void checkProjectOwnership(String resourceProjectId) {
        if (authorizationService.currentMerchantIdQuietly().isPresent()) {
            return;
        }
        Optional<String> scope = authorizationService.currentProjectScopeQuietly();
        if (scope.isPresent() && !scope.get().equals(resourceProjectId)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "平面图属于其他综合体项目");
        }
    }

    private void checkMerchantOwnership(String resourceMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantIdQuietly();
        if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "平面图属于其他商户");
        }
    }

    private FloorPlanImportTaskRecord ensureImportTaskBelongsToFloorPlan(String floorPlanId, String importTaskId) {
        ensureFloorPlanExists(floorPlanId);
        FloorPlanImportTaskRecord task = repository.findImportTaskById(importTaskId)
                .orElseThrow(() -> ApiException.notFound("IMPORT_TASK_NOT_FOUND", "导入任务不存在或已被删除"));
        if (!task.floorPlanId().equals(floorPlanId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "导入任务不属于该平面图");
        }
        return task;
    }

    private void requirePositive(Integer value, String field) {
        if (value == null || value <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 必须大于 0");
        }
    }

    private String normalizeAllowed(String value, String field, Set<String> allowed) {
        String normalized = requireText(value, field).toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw ApiException.badRequest("VALIDATION_FAILED", FIELD_LABELS.getOrDefault(field, field) + "取值无效");
        }
        return normalized;
    }

    private String requireField(String value, String label) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", label + "不能为空");
        }
        return trimmed;
    }

    // M015-001（r2）：派生主键＝前缀 + SHA-256 前 32 位十六进制（恒 36 字符），确定性、含平面图域且恒落入 id VARCHAR(64)；
    // slug 直接拼接会随平面图 UUID 与客户端键长度溢出（候选 id 68、UUID 键图形 id 80）。
    private String derivedId(String prefix, String seed) {
        return prefix + sha256(seed).substring(0, 32);
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
