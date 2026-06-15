package com.complex.platform.merchantservice;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderActionRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderAssignRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderCloseRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderCreateRequest;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class MerchantServiceService {

    private static final String BUSINESS_TYPE = "merchant_service_work_order";
    private static final Set<String> TYPES = Set.of("repair", "complaint", "rectification", "service");
    private static final Set<String> PRIORITIES = Set.of("low", "medium", "high", "critical");
    private static final Set<String> ACTION_TARGET_STATUSES = Set.of("assigned", "processing", "pending_merchant", "resolved");
    private static final Set<String> TERMINAL_STATUSES = Set.of("closed", "cancelled");

    private final MerchantServiceRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;

    public MerchantServiceService(
            MerchantServiceRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
    }

    public PageResponse<MerchantWorkOrderRecord> listWorkOrders() {
        List<MerchantWorkOrderRecord> records = repository.findAllWorkOrders();
        auditEventService.recordSuccess(
                "merchant_service.work_order.viewed",
                "merchant.service_work_order.view",
                BUSINESS_TYPE,
                "all",
                requestId(),
                null,
                Map.of("count", records.size())
        );
        return new PageResponse<>(records, 1, 50, records.size());
    }

    public MerchantWorkOrderRecord createWorkOrder(MerchantWorkOrderCreateRequest request) {
        return executeWrite(
                "createMerchantWorkOrder",
                request == null ? null : request.idempotencyKey(),
                request,
                "merchant_service.work_order.create_failed",
                "merchant.service_work_order.create",
                "new",
                () -> validateCreateRequest(request),
                () -> createWorkOrderOnce(request)
        );
    }

    public MerchantWorkOrderRecord assignWorkOrder(String id, MerchantWorkOrderAssignRequest request) {
        String workOrderId = requireText(id, "id");
        return executeWrite(
                "assignMerchantWorkOrder:" + workOrderId,
                request == null ? null : request.idempotencyKey(),
                request,
                "merchant_service.work_order.assign_failed",
                "merchant.service_work_order.assign",
                workOrderId,
                () -> {
                    validateAssignRequest(request);
                    ensureWorkOrderCanChange(workOrderId);
                },
                () -> assignWorkOrderOnce(workOrderId, request)
        );
    }

    public MerchantWorkOrderRecord addAction(String id, MerchantWorkOrderActionRequest request) {
        String workOrderId = requireText(id, "id");
        return executeWrite(
                "addMerchantWorkOrderAction:" + workOrderId,
                request == null ? null : request.idempotencyKey(),
                request,
                "merchant_service.work_order.action_failed",
                "merchant.service_work_order.process",
                workOrderId,
                () -> {
                    validateActionRequest(request);
                    ensureWorkOrderCanChange(workOrderId);
                },
                () -> addActionOnce(workOrderId, request)
        );
    }

    public MerchantWorkOrderRecord closeWorkOrder(String id, MerchantWorkOrderCloseRequest request) {
        String workOrderId = requireText(id, "id");
        return executeWrite(
                "closeMerchantWorkOrder:" + workOrderId,
                request == null ? null : request.idempotencyKey(),
                request,
                "merchant_service.work_order.close_failed",
                "merchant.service_work_order.close",
                workOrderId,
                () -> {
                    validateCloseRequest(request);
                    ensureWorkOrderCanChange(workOrderId);
                },
                () -> closeWorkOrderOnce(workOrderId, request)
        );
    }

    private MerchantWorkOrderRecord createWorkOrderOnce(MerchantWorkOrderCreateRequest request) {
        String type = normalizeAllowed(request.workOrderType(), "work_order_type", TYPES);
        String priority = normalizeAllowed(request.priority(), "priority", PRIORITIES);
        Instant now = Instant.now();
        MerchantWorkOrderRecord record = repository.saveWorkOrder(new MerchantWorkOrderRecord(
                "mswo_" + slug(request.idempotencyKey()),
                requireText(request.merchantId(), "merchant_id"),
                trimToNull(request.spaceId()),
                trimToNull(request.contractId()),
                type,
                priority,
                "created",
                requireText(request.title(), "title"),
                requireText(request.description(), "description"),
                requireText(request.reportedByUserId(), "reported_by_user_id"),
                requireText(request.reportedChannel(), "reported_channel"),
                null,
                null,
                request.dueAt(),
                null,
                null,
                null,
                evidenceIds(request.evidenceAttachmentIds()),
                now,
                now
        ));
        repository.saveAction(action(
                record.id(),
                "created",
                null,
                "created",
                record.reportedByUserId(),
                defaultNote(request.operatorNote(), "created"),
                record.evidenceAttachmentIds()
        ));
        auditEventService.recordSuccess(
                "merchant_service.work_order.created",
                "merchant.service_work_order.create",
                BUSINESS_TYPE,
                record.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("status", record.status())
        );
        return record;
    }

    private MerchantWorkOrderRecord assignWorkOrderOnce(String id, MerchantWorkOrderAssignRequest request) {
        MerchantWorkOrderRecord existing = findWorkOrder(id);
        rejectTerminal(existing);
        MerchantWorkOrderRecord assigned = repository.saveWorkOrder(existing.assign(
                requireText(request.ownerDepartment(), "owner_department"),
                requireText(request.ownerUserId(), "owner_user_id"),
                Instant.now()
        ));
        repository.saveAction(action(
                id,
                "assigned",
                existing.status(),
                assigned.status(),
                requireText(request.operatorUserId(), "operator_user_id"),
                requireText(request.operatorNote(), "operator_note"),
                evidenceIds(request.evidenceAttachmentIds())
        ));
        auditEventService.recordSuccess(
                "merchant_service.work_order.assigned",
                "merchant.service_work_order.assign",
                BUSINESS_TYPE,
                assigned.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("status", assigned.status())
        );
        return assigned;
    }

    private MerchantWorkOrderRecord addActionOnce(String id, MerchantWorkOrderActionRequest request) {
        MerchantWorkOrderRecord existing = findWorkOrder(id);
        rejectTerminal(existing);
        String operatorUserId = requireText(request.operatorUserId(), "operator_user_id");
        String operatorNote = requireText(request.operatorNote(), "operator_note");
        String targetStatus = trimToNull(request.targetStatus());
        MerchantWorkOrderRecord saved = existing;
        String actionType = "commented";
        if (targetStatus != null) {
            targetStatus = normalizeAllowed(targetStatus, "target_status", ACTION_TARGET_STATUSES);
            saved = repository.saveWorkOrder(existing.transitionTo(targetStatus, Instant.now()));
            actionType = existing.status().equals(targetStatus) ? "commented" : "status_changed";
        }
        repository.saveAction(action(
                id,
                actionType,
                existing.status(),
                saved.status(),
                operatorUserId,
                operatorNote,
                evidenceIds(request.evidenceAttachmentIds())
        ));
        auditEventService.recordSuccess(
                "merchant_service.work_order.action_added",
                "merchant.service_work_order.process",
                BUSINESS_TYPE,
                saved.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("status", saved.status())
        );
        return saved;
    }

    private MerchantWorkOrderRecord closeWorkOrderOnce(String id, MerchantWorkOrderCloseRequest request) {
        MerchantWorkOrderRecord existing = findWorkOrder(id);
        rejectTerminal(existing);
        String resolution = requireText(request.resolution(), "resolution");
        validateSatisfactionScore(request.satisfactionScore());
        MerchantWorkOrderRecord closed = repository.saveWorkOrder(existing.close(
                resolution,
                request.satisfactionScore(),
                Instant.now()
        ));
        repository.saveAction(action(
                id,
                "closed",
                existing.status(),
                "closed",
                requireText(request.operatorUserId(), "operator_user_id"),
                defaultNote(request.operatorNote(), "closed"),
                evidenceIds(request.evidenceAttachmentIds())
        ));
        auditEventService.recordSuccess(
                "merchant_service.work_order.closed",
                "merchant.service_work_order.close",
                BUSINESS_TYPE,
                closed.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("status", closed.status())
        );
        return closed;
    }

    private MerchantWorkOrderRecord findWorkOrder(String id) {
        return repository.findWorkOrderById(id)
                .orElseThrow(() -> ApiException.notFound("WORK_ORDER_NOT_FOUND", "merchant service work order not found"));
    }

    private void ensureWorkOrderCanChange(String id) {
        rejectTerminal(findWorkOrder(id));
    }

    private void validateCreateRequest(MerchantWorkOrderCreateRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.merchantId(), "merchant_id");
        normalizeAllowed(request.workOrderType(), "work_order_type", TYPES);
        normalizeAllowed(request.priority(), "priority", PRIORITIES);
        requireText(request.title(), "title");
        requireText(request.description(), "description");
        requireText(request.reportedByUserId(), "reported_by_user_id");
        requireText(request.reportedChannel(), "reported_channel");
    }

    private void validateAssignRequest(MerchantWorkOrderAssignRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.ownerDepartment(), "owner_department");
        requireText(request.ownerUserId(), "owner_user_id");
        requireText(request.operatorUserId(), "operator_user_id");
        requireText(request.operatorNote(), "operator_note");
    }

    private void validateActionRequest(MerchantWorkOrderActionRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.operatorUserId(), "operator_user_id");
        requireText(request.operatorNote(), "operator_note");
        String targetStatus = trimToNull(request.targetStatus());
        if (targetStatus != null) {
            normalizeAllowed(targetStatus, "target_status", ACTION_TARGET_STATUSES);
        }
    }

    private void validateCloseRequest(MerchantWorkOrderCloseRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.resolution(), "resolution");
        requireText(request.operatorUserId(), "operator_user_id");
        validateSatisfactionScore(request.satisfactionScore());
    }

    private MerchantWorkOrderActionRecord action(
            String workOrderId,
            String actionType,
            String fromStatus,
            String toStatus,
            String operatorUserId,
            String operatorNote,
            List<String> evidenceAttachmentIds
    ) {
        return new MerchantWorkOrderActionRecord(
                UUID.randomUUID().toString(),
                workOrderId,
                actionType,
                fromStatus,
                toStatus,
                operatorUserId,
                operatorNote,
                evidenceAttachmentIds,
                Instant.now()
        );
    }

    private MerchantWorkOrderRecord executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            String failureEvent,
            String permissionCode,
            String businessId,
            Runnable preValidation,
            Supplier<MerchantWorkOrderRecord> action
    ) {
        try {
            preValidation.run();
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    action,
                    MerchantWorkOrderRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    BUSINESS_TYPE,
                    businessId,
                    requestId(),
                    idempotencyKey,
                    errorCode(exception)
            );
            throw exception;
        }
    }

    private void rejectTerminal(MerchantWorkOrderRecord existing) {
        if (TERMINAL_STATUSES.contains(existing.status())) {
            throw ApiException.conflict("WORK_ORDER_CLOSED", "closed or cancelled work order cannot be changed");
        }
    }

    private void validateSatisfactionScore(Integer score) {
        if (score != null && (score < 1 || score > 5)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "satisfaction_score must be between 1 and 5");
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

    private String defaultNote(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private List<String> evidenceIds(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                cleaned.add(trimmed);
            }
        }
        return List.copyOf(cleaned);
    }

    private String slug(String value) {
        String text = requireText(value, "idempotency_key").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "_");
        return text.length() <= 48 ? text : text.substring(0, 48);
    }

    private String requestId() {
        return UUID.randomUUID().toString();
    }

    private String errorCode(RuntimeException exception) {
        return exception instanceof ApiException apiException ? apiException.code() : "INTERNAL_ERROR";
    }
}
