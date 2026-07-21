package com.complex.platform.integration;

/** 渠道门店映射登记/改绑（招商签约录入 / 运营后台补录）。自然键(provider_code,channel,external_store_id)幂等 upsert。 */
public record IntegrationChannelStoreMapRequest(
        String idempotencyKey,
        String providerCode,
        String channel,
        String externalStoreId,
        String merchantId
) {
}
