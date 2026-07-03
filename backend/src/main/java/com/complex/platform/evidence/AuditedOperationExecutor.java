package com.complex.platform.evidence;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class AuditedOperationExecutor {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;

    public AuditedOperationExecutor(IdempotencyService idempotencyService, AuditEventService auditEventService) {
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
    }

    public <T> PageResponse<T> readPage(AuditedOperation operation, Supplier<List<T>> action) {
        try {
            List<T> items = action.get();
            auditEventService.recordSuccess(
                    operation.successEvent(),
                    operation.permissionCode(),
                    operation.businessType(),
                    "all",
                    null,
                    null,
                    Map.of("count", items.size())
            );
            return new PageResponse<>(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    operation.failureEvent(),
                    operation.permissionCode(),
                    operation.businessType(),
                    "all",
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    /**
     * 服务端真分页版本：调用方(service)自己带 page/size 查库并构建含真实 total 的 PageResponse,
     * 本方法只负责统一审计留痕(成功记 count=当前页条数,失败记错误码),不改分页语义。
     * 租户隔离谓词由调用方 SQL 保持(与 readPage 同口径),本层不介入。
     */
    public <T> PageResponse<T> readPageResult(AuditedOperation operation, Supplier<PageResponse<T>> action) {
        try {
            PageResponse<T> page = action.get();
            auditEventService.recordSuccess(
                    operation.successEvent(),
                    operation.permissionCode(),
                    operation.businessType(),
                    "all",
                    null,
                    null,
                    Map.of("count", page.items().size(), "total", page.total())
            );
            return page;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    operation.failureEvent(),
                    operation.permissionCode(),
                    operation.businessType(),
                    "all",
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    public <T> T readSingle(AuditedOperation operation, Supplier<T> action, Function<Object, String> businessIdResolver) {
        try {
            T item = action.get();
            auditEventService.recordSuccess(
                    operation.successEvent(),
                    operation.permissionCode(),
                    operation.businessType(),
                    businessIdResolver.apply(item),
                    null,
                    null,
                    Map.of()
            );
            return item;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    operation.failureEvent(),
                    operation.permissionCode(),
                    operation.businessType(),
                    "all",
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    public <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            AuditedOperation operation,
            Supplier<T> action,
            Function<Object, String> businessIdResolver,
            Map<String, Object> successMetadata
    ) {
        try {
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    () -> {
                        T saved = action.get();
                        auditEventService.recordSuccess(
                                operation.successEvent(),
                                operation.permissionCode(),
                                operation.businessType(),
                                businessIdResolver.apply(saved),
                                null,
                                idempotencyKey,
                                successMetadata == null ? Map.of() : successMetadata
                        );
                        return saved;
                    },
                    responseType
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    operation.failureEvent(),
                    operation.permissionCode(),
                    operation.businessType(),
                    null,
                    null,
                    idempotencyKey,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    public String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
