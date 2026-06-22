package com.complex.platform.amusement;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

final class AmusementInspectionService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final Set<String> OVERALL_RESULTS = Set.of("pass", "fail", "pending_rectification");

    private final AmusementRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    AmusementInspectionService(
            AmusementRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
    }

    PageResponse<AmusementSafetyInspectionRecord> listSafetyInspections() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return readPage(
                "amusement.safety_inspection.viewed",
                "amusement.safety_inspection.view_failed",
                "amusement.safety_inspection.view",
                "amusement_safety_inspection",
                scope.isPresent() ? () -> repository.listSafetyInspectionsByMerchant(scope.get()) : repository::listSafetyInspections
        );
    }

    AmusementSafetyInspectionRecord createSafetyInspection(AmusementSafetyInspectionRequest request) {
        return executeWrite(
                "createAmusementSafetyInspection",
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementSafetyInspectionRecord.class,
                "amusement.safety_inspection.created",
                "amusement.safety_inspection.create_failed",
                "amusement.safety_inspection.create",
                "amusement_safety_inspection",
                () -> createSafetyInspectionInternal(request)
        );
    }

    AmusementSafetyInspectionRecord submitSafetyInspection(String inspectionId, AmusementSafetyActionRequest request) {
        String id = requireText(inspectionId, "inspection_id");
        String idempotencyKey = request == null ? null : request.idempotencyKey();
        try {
            return idempotencyService.execute(
                    "submitAmusementSafetyInspection:" + id,
                    idempotencyKey,
                    request,
                    () -> {
                        AmusementSafetyInspectionRecord submitted = submitSafetyInspectionInternal(id, request);
                        if ("pass".equals(submitted.overallResult())) {
                            auditEventService.recordSuccess(
                                    "amusement.safety_inspection.submitted",
                                    "amusement.safety_inspection.submit",
                                    "amusement_safety_inspection",
                                    submitted.id(),
                                    null,
                                    idempotencyKey,
                                    Map.of()
                            );
                        } else {
                            auditEventService.recordFailure(
                                    "amusement.safety_inspection.blocked",
                                    "amusement.safety_inspection.submit",
                                    "amusement_safety_inspection",
                                    submitted.id(),
                                    null,
                                    idempotencyKey,
                                    "SAFETY_REOPEN_PENDING"
                            );
                        }
                        return submitted;
                    },
                    AmusementSafetyInspectionRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "amusement.safety_inspection.blocked",
                    "amusement.safety_inspection.submit",
                    "amusement_safety_inspection",
                    id,
                    null,
                    idempotencyKey,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    AmusementSafetyInspectionRecord rectifySafetyInspection(String inspectionId, AmusementSafetyActionRequest request) {
        String id = requireText(inspectionId, "inspection_id");
        return executeWrite(
                "rectifyAmusementSafetyInspection:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementSafetyInspectionRecord.class,
                "amusement.safety_inspection.rectification_submitted",
                "amusement.safety_inspection.rectification_failed",
                "amusement.safety_inspection.rectify",
                "amusement_safety_inspection",
                () -> rectifySafetyInspectionInternal(id, request)
        );
    }

    AmusementSafetyInspectionRecord recheckSafetyInspection(String inspectionId, AmusementSafetyActionRequest request) {
        String id = requireText(inspectionId, "inspection_id");
        return executeWrite(
                "recheckAmusementSafetyInspection:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementSafetyInspectionRecord.class,
                "amusement.safety_inspection.rechecked",
                "amusement.safety_inspection.recheck_failed",
                "amusement.safety_inspection.recheck",
                "amusement_safety_inspection",
                () -> recheckSafetyInspectionInternal(id, request)
        );
    }

    AmusementSafetyInspectionRecord approveReopen(String inspectionId, StatusChangeRequest request) {
        String id = requireText(inspectionId, "inspection_id");
        return executeWrite(
                "approveAmusementReopen:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                AmusementSafetyInspectionRecord.class,
                "amusement.safety_inspection.reopened",
                "amusement.safety_inspection.reopen_rejected",
                "amusement.safety_inspection.approve_reopen",
                "amusement_safety_inspection",
                () -> approveReopenInternal(id, request)
        );
    }

    private AmusementSafetyInspectionRecord createSafetyInspectionInternal(AmusementSafetyInspectionRequest request) {
        AmusementSafetyInspectionRequest actual = requireRequest(request);
        String projectId = requireText(actual.projectId(), "project_id");
        repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
        // P1 越权修复：创建巡检此前唯独漏 checkProjectOwnership（submit/rectify/recheck 均有），
        // 可对他商户项目造巡检并经 evaluateOpenEligibility 干预其开放状态。
        checkProjectOwnership(projectId);
        String templateId = trimToNull(actual.templateId());
        if (templateId != null) {
            repository.findSafetyTemplateById(templateId)
                    .orElseThrow(() -> ApiException.notFound("SAFETY_TEMPLATE_NOT_FOUND", "safety template not found"));
        }
        if (actual.items() == null || actual.items().isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "items is required");
        }
        String overall = enumOrDefault(actual.overallResult(), "pending_rectification", OVERALL_RESULTS, "overall_result");
        // 2b 防伪造 pass：overall=pass 不可由请求方凭空指定开放高风险设备——
        // (1) 任一检查项 result=fail 时与项级结论矛盾，不得判 pass；(2) pass 必须有证据（必拍）。
        if ("pass".equals(overall)) {
            boolean anyItemFailed = actual.items().stream()
                    .anyMatch(item -> item != null && "fail".equals(String.valueOf(item.get("result"))));
            if (anyItemFailed) {
                throw ApiException.conflict("SAFETY_INSPECTION_PASS_CONFLICTS_ITEMS",
                        "overall_result cannot be pass when any inspection item result is fail");
            }
            if (actual.evidenceAttachmentIds() == null || actual.evidenceAttachmentIds().isEmpty()) {
                throw ApiException.badRequest("SAFETY_INSPECTION_EVIDENCE_REQUIRED",
                        "pass inspection requires evidence attachments");
            }
        }
        Instant now = Instant.now();
        // V114 结构化补强：巡检人结构化落库；inspection_result 直接派生自 overall_result（同枚举域，便于看板/审计读结构化字段）；
        // 异常标记：overall 非 pass 即异常，abnormal_note 取请求填报。
        boolean abnormal = !"pass".equals(overall);
        return repository.saveSafetyInspection(new AmusementSafetyInspectionRecord(
                "inspection_" + slug(actual.idempotencyKey()),
                projectId,
                trimToNull(actual.deviceId()),
                templateId,
                List.copyOf(actual.items()),
                overall,
                "created",
                "fail".equals(overall) || "pending_rectification".equals(overall) ? "pending" : "not_required",
                "fail".equals(overall) || "pending_rectification".equals(overall) ? "pending" : "not_required",
                actual.inspectedAt() == null ? now : actual.inspectedAt(),
                null,
                null,
                null,
                trimToNull(actual.operatorNote()),
                evidence(actual.evidenceAttachmentIds()),
                now,
                now,
                trimToNull(actual.inspectorId()),
                overall,
                abnormal,
                trimToNull(actual.abnormalNote())
        ));
    }

    private AmusementSafetyInspectionRecord submitSafetyInspectionInternal(String inspectionId, AmusementSafetyActionRequest request) {
        AmusementSafetyActionRequest actual = requireRequest(request);
        requireText(actual.comment(), "comment");
        AmusementSafetyInspectionRecord existing = findInspection(inspectionId);
        checkProjectOwnership(existing.projectId());
        if (!"created".equals(existing.inspectionStatus())) {
            throw ApiException.conflict("INSPECTION_ALREADY_SUBMITTED", "inspection already submitted");
        }
        Instant now = Instant.now();
        repository.saveSafetyAction(new AmusementSafetyActionRecord(
                "safety_action_" + slug(actual.idempotencyKey()),
                existing.id(),
                "submit",
                existing.overallResult(),
                trimToNull(actual.comment()),
                evidence(actual.attachmentIds()),
                now
        ));
        boolean blocked = "fail".equals(existing.overallResult()) || "pending_rectification".equals(existing.overallResult());
        AmusementSafetyInspectionRecord saved = repository.saveSafetyInspection(new AmusementSafetyInspectionRecord(
                existing.id(),
                existing.projectId(),
                existing.deviceId(),
                existing.templateId(),
                existing.items(),
                existing.overallResult(),
                "submitted",
                blocked ? "pending" : "not_required",
                blocked ? "pending" : "not_required",
                existing.inspectedAt(),
                now,
                existing.recheckedAt(),
                existing.approvedAt(),
                trimToDefault(actual.comment(), existing.operatorNote()),
                evidenceOrExisting(actual.evidenceAttachmentIds(), existing.evidenceAttachmentIds()),
                existing.createdAt(),
                now,
                existing.inspectorId(),
                existing.inspectionResult(),
                existing.abnormalFound(),
                existing.abnormalNote()
        ));
        // F-OPS-04：巡检 PASS 提交即生效，刷新设备最近 PASS 巡检时间，供核销有效期校验（过期则阻断核销）。
        if ("pass".equals(existing.overallResult())) {
            refreshDeviceLastPass(existing.projectId(), existing.deviceId(), existing.inspectedAt());
        }
        return saved;
    }

    /**
     * F-OPS-04：巡检 PASS 生效时刷新设备最近 PASS 巡检时间。设备级巡检（device_id 非空）刷新单设备，
     * 项目级巡检（device_id 空）覆盖刷新该项目全部设备；inspectedAt 为空回退当前时刻。
     */
    private void refreshDeviceLastPass(String projectId, String deviceId, Instant inspectedAt) {
        Instant at = inspectedAt == null ? Instant.now() : inspectedAt;
        if (deviceId != null && !deviceId.isBlank()) {
            repository.updateDeviceLastPassInspection(projectId, deviceId, at);
        } else {
            repository.updateProjectDevicesLastPass(projectId, at);
        }
    }

    private AmusementSafetyInspectionRecord rectifySafetyInspectionInternal(String inspectionId, AmusementSafetyActionRequest request) {
        AmusementSafetyActionRequest actual = requireRequest(request);
        requireText(actual.comment(), "comment");
        AmusementSafetyInspectionRecord existing = findInspection(inspectionId);
        checkProjectOwnership(existing.projectId());
        if (!"submitted".equals(existing.inspectionStatus()) || "not_required".equals(existing.rectificationStatus())) {
            throw ApiException.conflict("RECTIFICATION_NOT_REQUIRED", "rectification is not required");
        }
        Instant now = Instant.now();
        repository.saveSafetyAction(new AmusementSafetyActionRecord(
                "safety_action_" + slug(actual.idempotencyKey()),
                existing.id(),
                "rectify",
                trimToDefault(actual.actionResult(), "submitted"),
                trimToNull(actual.comment()),
                evidence(actual.attachmentIds()),
                now
        ));
        return repository.saveSafetyInspection(new AmusementSafetyInspectionRecord(
                existing.id(),
                existing.projectId(),
                existing.deviceId(),
                existing.templateId(),
                existing.items(),
                existing.overallResult(),
                existing.inspectionStatus(),
                "submitted",
                "pending",
                existing.inspectedAt(),
                existing.submittedAt(),
                existing.recheckedAt(),
                existing.approvedAt(),
                trimToDefault(actual.comment(), existing.operatorNote()),
                evidenceOrExisting(actual.evidenceAttachmentIds(), existing.evidenceAttachmentIds()),
                existing.createdAt(),
                now,
                existing.inspectorId(),
                existing.inspectionResult(),
                existing.abnormalFound(),
                existing.abnormalNote()
        ));
    }

    private AmusementSafetyInspectionRecord recheckSafetyInspectionInternal(String inspectionId, AmusementSafetyActionRequest request) {
        AmusementSafetyActionRequest actual = requireRequest(request);
        String result = requireText(actual.actionResult(), "action_result").toLowerCase(java.util.Locale.ROOT);
        if (!Set.of("pass", "fail").contains(result)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "action_result must be pass or fail");
        }
        AmusementSafetyInspectionRecord existing = findInspection(inspectionId);
        checkProjectOwnership(existing.projectId());
        if (!"submitted".equals(existing.rectificationStatus())) {
            throw ApiException.conflict("RECTIFICATION_NOT_SUBMITTED", "rectification must be submitted before recheck");
        }
        // 2b 防伪造 pass（与 submit 路径一致）：复检判 pass 翻转 overallResult、解锁恢复营业，必须有影像证据留痕，
        // 不得凭空空证据复检通过开放高风险设备。证据可经动作附件或巡检证据任一提供。
        if ("pass".equals(result)
                && (actual.attachmentIds() == null || actual.attachmentIds().isEmpty())
                && (actual.evidenceAttachmentIds() == null || actual.evidenceAttachmentIds().isEmpty())) {
            throw ApiException.badRequest("SAFETY_INSPECTION_EVIDENCE_REQUIRED",
                    "pass recheck requires evidence attachments");
        }
        Instant now = Instant.now();
        repository.saveSafetyAction(new AmusementSafetyActionRecord(
                "safety_action_" + slug(actual.idempotencyKey()),
                existing.id(),
                "recheck",
                result,
                trimToNull(actual.comment()),
                evidence(actual.attachmentIds()),
                now
        ));
        return repository.saveSafetyInspection(new AmusementSafetyInspectionRecord(
                existing.id(),
                existing.projectId(),
                existing.deviceId(),
                existing.templateId(),
                existing.items(),
                result,
                existing.inspectionStatus(),
                "rechecked",
                "pass".equals(result) ? "pending_approval" : "pending",
                existing.inspectedAt(),
                existing.submittedAt(),
                now,
                existing.approvedAt(),
                trimToDefault(actual.comment(), existing.operatorNote()),
                evidenceOrExisting(actual.evidenceAttachmentIds(), existing.evidenceAttachmentIds()),
                existing.createdAt(),
                now,
                existing.inspectorId(),
                // 复检结论刷新结构化 inspection_result；pass 视为异常已消除。
                result,
                !"pass".equals(result),
                existing.abnormalNote()
        ));
    }

    private AmusementSafetyInspectionRecord approveReopenInternal(String inspectionId, StatusChangeRequest request) {
        StatusChangeRequest actual = requireRequest(request);
        requireText(actual.reason(), "reason");
        AmusementSafetyInspectionRecord existing = findInspection(inspectionId);
        checkProjectOwnership(existing.projectId());
        if (!"pending_approval".equals(existing.reopenStatus()) || !"pass".equals(existing.overallResult())) {
            throw ApiException.conflict("RECHECK_PASS_REQUIRED", "recheck pass is required before reopen approval");
        }
        Instant now = Instant.now();
        // 断链修复（清单#18）：原"批准恢复营业"仅置 reopen_status=approved，不联动 amusement_projects.open_status，
        // 导致"批准恢复营业却不恢复营业态"。此处补联动——批准后若项目仍处停业态（maintenance/closed），
        // 用与检修线 confirmMaintenanceResume 一致的乐观锁 updateProjectOpenStatus 翻转为 open；
        // 取舍：安全巡检审批是独立恢复路径（无需依赖独立检修单也能恢复），故对 maintenance 与 closed 均尝试恢复；
        // 项目已处运营态（open/paused）则不强翻——避免与暂停线/检修线冲突（paused 为人工暂停，不由巡检审批顶替）。
        reopenProjectIfBlocked(existing.projectId());
        AmusementSafetyInspectionRecord saved = repository.saveSafetyInspection(new AmusementSafetyInspectionRecord(
                existing.id(),
                existing.projectId(),
                existing.deviceId(),
                existing.templateId(),
                existing.items(),
                existing.overallResult(),
                existing.inspectionStatus(),
                existing.rectificationStatus(),
                "approved",
                existing.inspectedAt(),
                existing.submittedAt(),
                existing.recheckedAt(),
                now,
                trimToDefault(actual.reason(), existing.operatorNote()),
                evidenceOrExisting(actual.evidenceAttachmentIds(), existing.evidenceAttachmentIds()),
                existing.createdAt(),
                now,
                existing.inspectorId(),
                existing.inspectionResult(),
                existing.abnormalFound(),
                existing.abnormalNote()
        ));
        // F-OPS-04：批准恢复营业即巡检 PASS 生效（前置已校验 overall=pass），刷新设备最近 PASS 巡检时间。
        refreshDeviceLastPass(existing.projectId(), existing.deviceId(), existing.inspectedAt());
        return saved;
    }

    /**
     * 批准恢复营业时联动项目营业态：仅当项目当前处于停业态（maintenance/closed）时翻转为 open，
     * 与检修线 confirmMaintenanceResume 同口径（乐观锁 updateProjectOpenStatus(expected→new)）。
     * 已处运营态（open/paused）不动；两线不冲突：检修线只翻 maintenance→open，巡检线在无独立检修单时亦可从 closed 恢复。
     */
    private void reopenProjectIfBlocked(String projectId) {
        AmusementProjectRecord project = repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
        String current = project.openStatus();
        if ("maintenance".equals(current) || "closed".equals(current)) {
            repository.updateProjectOpenStatus(projectId, current, "open");
        }
    }

    private AmusementSafetyInspectionRecord findInspection(String inspectionId) {
        return repository.findSafetyInspectionById(inspectionId)
                .orElseThrow(() -> ApiException.notFound("SAFETY_INSPECTION_NOT_FOUND", "safety inspection not found"));
    }

    private void checkProjectOwnership(String projectId) {
        authorizationService.currentMerchantScope().ifPresent(merchantId -> {
            AmusementProjectRecord project = repository.findProjectById(projectId)
                    .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
            if (!merchantId.equals(project.merchantId())) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "project does not belong to your merchant");
            }
        });
    }

    private <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<List<T>> action
    ) {
        try {
            List<T> items = action.get();
            auditEventService.recordSuccess(
                    successEvent,
                    permissionCode,
                    businessType,
                    "all",
                    null,
                    null,
                    Map.of("count", items.size())
            );
            return new PageResponse<>(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    "all",
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<T> action
    ) {
        try {
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    () -> {
                        T saved = action.get();
                        auditEventService.recordSuccess(
                                successEvent,
                                permissionCode,
                                businessType,
                                businessId(saved),
                                null,
                                idempotencyKey,
                                Map.of()
                        );
                        return saved;
                    },
                    responseType
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    "unknown",
                    null,
                    idempotencyKey,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private String businessId(Object response) {
        if (response instanceof AmusementSafetyInspectionRecord record) {
            return record.id();
        }
        return "unknown";
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireText(String value, String fieldName) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String enumOrDefault(String value, String defaultValue, Set<String> allowed, String fieldName) {
        String actual = trimToDefault(value, defaultValue);
        if (actual == null || !allowed.contains(actual)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return actual;
    }

    private List<String> evidence(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private List<String> evidenceOrExisting(List<String> values, List<String> existing) {
        List<String> evidence = evidence(values);
        return evidence.isEmpty() ? List.copyOf(existing == null ? List.of() : existing) : evidence;
    }

    private String slug(String value) {
        String source = requireText(value, "idempotency_key");
        UUID uuid = UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(asBytes(uuid)).substring(0, 24);
    }

    private byte[] asBytes(UUID uuid) {
        byte[] bytes = new byte[16];
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        for (int index = 0; index < 8; index++) {
            bytes[index] = (byte) (most >>> (8 * (7 - index)));
            bytes[index + 8] = (byte) (least >>> (8 * (7 - index)));
        }
        return bytes;
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
