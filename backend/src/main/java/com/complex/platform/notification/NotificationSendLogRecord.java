package com.complex.platform.notification;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record NotificationSendLogRecord(
        String id,
        String requestId,
        String channel,
        String domain,
        String eventKey,
        String recipient,
        String templateCode,
        Map<String, Object> params,
        String status,
        String reason,
        String providerRef,
        Instant createdAt,
        Instant updatedAt
) {
    public NotificationSendLogRecord {
        params = params == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }
}
