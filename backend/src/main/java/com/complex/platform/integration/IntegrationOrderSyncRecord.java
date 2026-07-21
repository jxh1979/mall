package com.complex.platform.integration;

import java.time.Instant;
import java.util.Map;

public record IntegrationOrderSyncRecord(
        String id,
        String providerCode,
        String businessType,
        String syncType,
        String externalBusinessId,
        String status,
        long amountCent,
        String currency,
        String channel,
        String externalChannelOrderId,
        // 落商户归属：NULL = 未归属（第三方给不到门店 / 未维护映射 / 存量历史行），下游仅平台级，不静默塞给某商户。
        String merchantId,
        Map<String, Object> providerPayload,
        Instant createdAt,
        Instant updatedAt
) {
}
