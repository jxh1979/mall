package com.complex.platform.integration;

/** 批6 渠道票种映射登记/改绑（招商/运营后台录入）。自然键(provider_code,channel,external_product_id)幂等 upsert。 */
public record IntegrationChannelTicketTypeMapRequest(
        String idempotencyKey,
        String providerCode,
        String channel,
        String externalProductId,
        String ticketTypeId,
        // 可空：渠道团购价对应的本地价格规则；空=按票种门市价出票。
        String priceRuleId
) {
}
