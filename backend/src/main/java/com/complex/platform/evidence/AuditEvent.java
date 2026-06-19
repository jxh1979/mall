package com.complex.platform.evidence;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 审计事件。P0 清单#2 扩展（V101）：追加 eventCategory（事件类目）/ subjectType（事件主体类型）两个可空字段，
 * 用于积分入账等事件的类目与主体检索。向后兼容：保留原 15 参构造器，新字段默认 null。
 */
public record AuditEvent(
        UUID id,
        Instant occurredAt,
        String eventName,
        String eventResult,
        String actorType,
        String actorId,
        String authContext,
        String terminal,
        String permissionCode,
        String businessType,
        String businessId,
        String requestId,
        String idempotencyKey,
        Map<String, Object> metadata,
        String merchantId,
        String eventCategory,
        String subjectType
) {

    /** 向后兼容构造器（原 15 字段）：eventCategory / subjectType 默认 null。 */
    public AuditEvent(
            UUID id,
            Instant occurredAt,
            String eventName,
            String eventResult,
            String actorType,
            String actorId,
            String authContext,
            String terminal,
            String permissionCode,
            String businessType,
            String businessId,
            String requestId,
            String idempotencyKey,
            Map<String, Object> metadata,
            String merchantId
    ) {
        this(id, occurredAt, eventName, eventResult, actorType, actorId, authContext, terminal,
                permissionCode, businessType, businessId, requestId, idempotencyKey, metadata, merchantId,
                null, null);
    }
}
