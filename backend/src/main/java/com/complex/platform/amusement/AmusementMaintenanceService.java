package com.complex.platform.amusement;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 游乐设施定期检修与主管确认恢复营业（设计 §5.1.1/§6.6.1/§7.14a/§8.7.1）。
 */
final class AmusementMaintenanceService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final Set<String> RESULTS = Set.of("pass", "fail");
    // 来源终端取《统一口径基准》D5 10 端枚举。
    private static final Set<String> SOURCE_TERMINALS = Set.of(
            "app", "miniapp", "web", "tablet", "staff_work_phone",
            "staff_private_phone", "merchant_portal", "merchant_app", "kiosk", "mobile_admin");

    private final AmusementRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    AmusementMaintenanceService(
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

    PageResponse<AmusementMaintenanceRecord> listMaintenanceRecords() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return readPage(
                "amusement.maintenance.viewed",
                "amusement.maintenance.view_failed",
                "amusement.maintenance.read",
                "amusement_maintenance_record",
                scope.isPresent()
                        ? () -> repository.listMaintenanceRecordsByMerchant(scope.get())
                        : repository::listMaintenanceRecords
        );
    }

    AmusementMaintenanceRecord getMaintenanceRecord(String recordId) {
        String id = requireText(recordId, "id");
        try {
            AmusementMaintenanceRecord record = findRecord(id);
            checkProjectOwnership(record.projectId());
            auditEventService.recordSuccess(
                    "amusement.maintenance.viewed",
                    "amusement.maintenance.read",
                    "amusement_maintenance_record",
                    record.id(),
                    null,
                    null,
                    Map.of());
            return record;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "amusement.maintenance.view_failed",
                    "amusement.maintenance.read",
                    "amusement_maintenance_record",
                    id,
                    null,
                    null,
                    codeOf(exception));
            throw exception;
        }
    }

    AmusementMaintenanceRecord createMaintenanceRecord(AmusementMaintenanceRecordRequest request) {
        return executeWrite(
                "createAmusementMaintenanceRecord",
                request == null ? null : request.idempotencyKey(),
                request,
                "amusement.maintenance_recorded",
                "amusement.maintenance_record_failed",
                "amusement.maintenance.submit",
                () -> createMaintenanceRecordInternal(request));
    }

    AmusementMaintenanceRecord confirmMaintenanceResume(String recordId, AmusementMaintenanceResumeRequest request) {
        String id = requireText(recordId, "id");
        return executeWrite(
                "confirmAmusementMaintenanceResume:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                "amusement.project.resumed",
                "amusement.maintenance_resume_failed",
                "amusement.maintenance.confirm",
                () -> confirmMaintenanceResumeInternal(id, request));
    }

    private AmusementMaintenanceRecord createMaintenanceRecordInternal(AmusementMaintenanceRecordRequest request) {
        AmusementMaintenanceRecordRequest actual = requireRequest(request);
        String projectId = requireText(actual.projectId(), "project_id");
        AmusementProjectRecord project = repository.findProjectById(projectId)
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
        checkProjectOwnership(projectId);
        String vendorId = requireText(actual.maintenanceVendorId(), "maintenance_vendor_id");
        String result = enumValue(actual.result(), RESULTS, "result");
        String sourceTerminal = enumValue(actual.sourceTerminal(), SOURCE_TERMINALS, "source_terminal");
        String engineerName = trimToNull(actual.engineerName());
        String exceptionNote = trimToNull(actual.exceptionNote());
        if ("fail".equals(result) && exceptionNote == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "exception_note is required when result is fail");
        }
        // submitted=待确认前置；pass 进入待主管确认，fail 保持已提交（不可进入确认流程）。
        String status = "pass".equals(result) ? "pending_confirm" : "submitted";
        Instant now = Instant.now();
        return repository.saveMaintenanceRecord(new AmusementMaintenanceRecord(
                "maintenance_" + slug(actual.idempotencyKey()),
                projectId,
                trimToNull(actual.deviceId()),
                trimToNull(actual.locationPointId()),
                vendorId,
                trimToNull(actual.engineerId()),
                engineerName,
                actual.maintenanceAt() == null ? now : actual.maintenanceAt(),
                result,
                exceptionNote,
                sourceTerminal,
                status,
                null,
                null,
                null,
                now,
                project.merchantId()));
    }

    private AmusementMaintenanceRecord confirmMaintenanceResumeInternal(
            String recordId,
            AmusementMaintenanceResumeRequest request
    ) {
        AmusementMaintenanceResumeRequest actual = requireRequest(request);
        String supervisorId = requireText(actual.supervisorConfirmedBy(), "supervisor_confirmed_by");
        AmusementMaintenanceRecord existing = findRecord(recordId);
        checkProjectOwnership(existing.projectId());
        if ("confirmed".equals(existing.status())) {
            throw ApiException.conflict("MAINTENANCE_ALREADY_CONFIRMED", "maintenance already confirmed");
        }
        // 硬约束（5.1.1）：result 必须 pass。
        if (!"pass".equals(existing.result())) {
            throw ApiException.conflict("MAINTENANCE_NOT_PASSED", "maintenance result must be pass before resume");
        }
        AmusementProjectRecord project = repository.findProjectById(existing.projectId())
                .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "project not found"));
        // 硬约束（5.1.1）：当前项目必须处于 maintenance，才允许 maintenance → open。
        if (!"maintenance".equals(project.openStatus())) {
            throw ApiException.conflict("PROJECT_NOT_IN_MAINTENANCE", "project is not in maintenance");
        }
        if (!repository.updateProjectOpenStatus(project.id(), "maintenance", "open")) {
            throw ApiException.conflict("PROJECT_NOT_IN_MAINTENANCE", "project is not in maintenance");
        }
        Instant now = Instant.now();
        return repository.saveMaintenanceRecord(new AmusementMaintenanceRecord(
                existing.id(),
                existing.projectId(),
                existing.deviceId(),
                existing.locationPointId(),
                existing.maintenanceVendorId(),
                existing.engineerId(),
                existing.engineerName(),
                existing.maintenanceAt(),
                existing.result(),
                existing.exceptionNote(),
                existing.sourceTerminal(),
                "confirmed",
                supervisorId,
                now,
                now,
                existing.createdAt(),
                existing.merchantId()));
    }

    private AmusementMaintenanceRecord findRecord(String recordId) {
        return repository.findMaintenanceRecordById(recordId)
                .orElseThrow(() -> ApiException.notFound("MAINTENANCE_RECORD_NOT_FOUND", "maintenance record not found"));
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
                    successEvent, permissionCode, businessType, "all", null, null, Map.of("count", items.size()));
            return new PageResponse<>(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent, permissionCode, businessType, "all", null, null, codeOf(exception));
            throw exception;
        }
    }

    private AmusementMaintenanceRecord executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            String successEvent,
            String failureEvent,
            String permissionCode,
            Supplier<AmusementMaintenanceRecord> action
    ) {
        try {
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    () -> {
                        AmusementMaintenanceRecord saved = action.get();
                        auditEventService.recordSuccess(
                                successEvent,
                                permissionCode,
                                "amusement_maintenance_record",
                                saved.id(),
                                null,
                                idempotencyKey,
                                Map.of());
                        return saved;
                    },
                    AmusementMaintenanceRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    "amusement_maintenance_record",
                    "unknown",
                    null,
                    idempotencyKey,
                    codeOf(exception));
            throw exception;
        }
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

    private String enumValue(String value, Set<String> allowed, String fieldName) {
        String actual = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!allowed.contains(actual)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return actual;
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
