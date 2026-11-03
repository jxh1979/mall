package com.complex.platform.evidence;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import com.complex.platform.common.Strings;

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

    /**
     * 第十八批 N 段第 124 条 124-R3（PGM L738「访问被拒绝也要留痕，便于排查是权限问题还是开关问题。」；先例 18I 第 90 条
     * LeasingController.failureAudited）：控制器写口把权限、幂等键与服务调用一起放进 action，任一步失败（权限、范围、校验、状态冲突）
     * 落 OpenAPI 声明的 x-audit-event-failure 再原样抛出；成功事件照旧由服务层写。失败事件是独立事务（recordFailure，XM-M031-B），
     * 业务回滚也保留；写失败事件本身出错挂在原异常上、不顶掉原异常。放在本类、不放进 AuditEventService：自调用会绕过 REQUIRES_NEW。
     * 请求体反序列化失败与 @Valid 注解校验在进入控制器方法之前就被框架拦下，本包装接不到。
     */
    public <T> T recordingFailure(String failureEvent, String permission, String businessType, String businessId,
            String idempotencyKey, Supplier<T> action) {
        try {
            return action.get();
        } catch (RuntimeException error) {
            try {
                auditEventService.recordFailure(failureEvent, permission, businessType, businessId, null, idempotencyKey,
                        codeOf(error));
            } catch (RuntimeException auditError) {
                error.addSuppressed(auditError);
            }
            throw error;
        }
    }

    /**
     * 追加一条不可改的操作明细留痕(who/when 由审计上下文自动填充),供对象级处置时间线回溯。
     * 复用 audit_events(只增不改),不另建历史表;由调用方在业务事务内调用,与业务写入同事务原子落库。
     */
    public AuditEvent recordDetail(String eventName, String permissionCode, String businessType,
            String businessId, Map<String, Object> metadata) {
        return auditEventService.recordSuccess(
                eventName, permissionCode, businessType, businessId, null, null, metadata);
    }

    /**
     * 读某业务对象某事件的操作明细时间线,按发生时间升序返回(searchOperationEvents 为 occurred_at DESC,此处反转),
     * 供处置闭环回溯。platform 作用域(merchantScope=null)不加商户谓词。
     */
    public List<AuditEvent> readObjectTimeline(String businessType, String businessId, String eventName) {
        List<AuditEvent> events = new java.util.ArrayList<>(auditEventService.searchOperationEvents(
                businessType, businessId, null, eventName, null, null, 200, null));
        java.util.Collections.reverse(events);
        return events;
    }

    public String requireText(String value, String fieldName) {
        return Strings.requireText(value, fieldName);
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
