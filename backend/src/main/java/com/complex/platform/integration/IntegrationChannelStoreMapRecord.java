package com.complex.platform.integration;

import java.time.Instant;

/** 渠道门店号 → 商户映射（源A：渠道回流 payload.external_store_id 反查落商户）。 */
public record IntegrationChannelStoreMapRecord(
        String id,
        String providerCode,
        String channel,
        String externalStoreId,
        String merchantId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
