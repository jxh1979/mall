package com.complex.platform.integration;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

final class IntegrationOperationSupport {

    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;

    IntegrationOperationSupport(IdempotencyService idempotencyService, AuditEventService auditEventService) {
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
    }

    <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<List<T>> action
    ) {
        try {
            List<T> items = action.get();
            recordSuccess(successEvent, permissionCode, businessType, "all", null, Map.of("count", items.size()));
            return new PageResponse<>(items, 1, 50, items.size());
        } catch (RuntimeException exception) {
            recordFailure(failureEvent, permissionCode, businessType, "all", null, errorCode(exception));
            throw exception;
        }
    }

    <T> T executeIdempotent(
            String operationId,
            String idempotencyKey,
            Object request,
            Supplier<T> action,
            Class<T> responseType
    ) {
        return idempotencyService.execute(operationId, idempotencyKey, request, action, responseType);
    }

    <T> T executeWrite(
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
                        recordSuccess(
                                successEvent,
                                permissionCode,
                                businessType,
                                businessId(saved),
                                idempotencyKey,
                                Map.of("operation_id", operationId)
                        );
                        return saved;
                    },
                    responseType
            );
        } catch (RuntimeException exception) {
            recordFailure(failureEvent, permissionCode, businessType, "unknown", idempotencyKey, errorCode(exception));
            throw exception;
        }
    }

    void recordSuccess(
            String eventName,
            String permissionCode,
            String businessType,
            String businessId,
            String idempotencyKey,
            Map<String, Object> metadata
    ) {
        auditEventService.recordSuccess(
                eventName,
                permissionCode,
                businessType,
                businessId,
                requestId(),
                idempotencyKey,
                metadata
        );
    }

    void recordFailure(
            String eventName,
            String permissionCode,
            String businessType,
            String businessId,
            String idempotencyKey,
            String errorCode
    ) {
        auditEventService.recordFailure(
                eventName,
                permissionCode,
                businessType,
                businessId,
                requestId(),
                idempotencyKey,
                errorCode
        );
    }

    String errorCode(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return "INTERNAL_ERROR";
    }

    private String requestId() {
        return "req_" + UUID.randomUUID();
    }

    private String businessId(Object saved) {
        if (saved instanceof IntegrationOrderSyncRecord record) {
            return record.id();
        }
        if (saved instanceof IntegrationTaskRecord record) {
            return record.id();
        }
        if (saved instanceof ReconciliationBatchRecord record) {
            return record.id();
        }
        if (saved instanceof ReconciliationDifferenceRecord record) {
            return record.id();
        }
        if (saved instanceof ProviderAdapterRecord record) {
            return record.id();
        }
        if (saved instanceof AdapterTestRecord record) {
            return record.id();
        }
        if (saved instanceof IntegrationPollingCheckRecord record) {
            return record.id();
        }
        return "unknown";
    }
}