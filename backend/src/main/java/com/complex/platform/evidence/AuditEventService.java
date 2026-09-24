package com.complex.platform.evidence;

import com.complex.platform.access.AccessActor;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditEventService {

    private final AuditEventRepository repository;

    public AuditEventService(AuditEventRepository repository) {
        this.repository = repository;
    }

    /** 条目4：跨对象操作留痕查询（供审计只读角色搜索/导出）。 */
    public java.util.List<AuditEvent> searchOperationEvents(String businessType, String businessId,
            String actorId, String eventName, java.time.Instant from, java.time.Instant to, int limit, String merchantScope) {
        return repository.searchOperationEvents(businessType, businessId, actorId, eventName, from, to, limit, merchantScope);
    }

    public AuditEvent recordSuccess(
            String eventName,
            String permissionCode,
            String businessType,
            String businessId,
            String requestId,
            String idempotencyKey,
            Map<String, Object> metadata
    ) {
        return record(eventName, "success", permissionCode, businessType, businessId, requestId, idempotencyKey,
                metadata, null, null);
    }

    /**
     * P0 清单#2 重载：在原 recordSuccess 基础上多 eventCategory（事件类目）/ subjectType（事件主体类型），
     * 写入 audit_events 新列（V101）。供积分入账等事件按类目 / 主体检索。旧重载等价于此处两参传 null。
     */
    public AuditEvent recordSuccess(
            String eventName,
            String permissionCode,
            String businessType,
            String businessId,
            String requestId,
            String idempotencyKey,
            Map<String, Object> metadata,
            String eventCategory,
            String subjectType
    ) {
        return record(eventName, "success", permissionCode, businessType, businessId, requestId, idempotencyKey,
                metadata, eventCategory, subjectType);
    }

    /**
     * 失败事件在独立事务里写入（XM-M031-B）：调用方事务回滚或已因 SQL 错误中止时，失败记录仍须留住、且不得顶掉原异常
     * （治理地图第 893、899、902 行；BSM 第 1583 行）。成功事件仍随调用方事务，业务回滚时一起回滚。
     */
    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public AuditEvent recordFailure(
            String eventName,
            String permissionCode,
            String businessType,
            String businessId,
            String requestId,
            String idempotencyKey,
            String errorCode
    ) {
        return record(eventName, "failure", permissionCode, businessType, businessId, requestId, idempotencyKey, Map.of(
                "error_code", errorCode
        ), null, null);
    }

    private AuditEvent record(
            String eventName,
            String eventResult,
            String permissionCode,
            String businessType,
            String businessId,
            String requestId,
            String idempotencyKey,
            Map<String, Object> metadata,
            String eventCategory,
            String subjectType
    ) {
        String actorId = null;
        String actorType = "staff";
        String authContext = null;
        String merchantId = null;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                Object principal = auth.getPrincipal();
                if (principal instanceof AccessActor actor) {
                    actorId = actor.id();
                    actorType = actor.accountType() != null ? actor.accountType() : "staff";
                    merchantId = actor.merchantId();
                    authContext = actor.username();
                } else {
                    authContext = auth.getName();
                }
                if (authContext != null && authContext.length() > 64) {
                    authContext = authContext.substring(0, 64);
                }
            }
        } catch (RuntimeException ignored) {
            // best-effort: never suppress the audited operation due to auth extraction failure
        }

        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                Instant.now(),
                eventName,
                eventResult,
                actorType,
                actorId,
                authContext,
                "web",
                permissionCode,
                businessType,
                businessId,
                requestId,
                idempotencyKey,
                Map.copyOf(new LinkedHashMap<>(metadata == null ? Map.of() : metadata)),
                merchantId,
                eventCategory,
                subjectType
        );
        return repository.save(event);
    }
}
