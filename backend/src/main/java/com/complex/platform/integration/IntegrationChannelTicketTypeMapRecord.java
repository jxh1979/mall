package com.complex.platform.integration;

import java.time.Instant;

/**
 * 批6 渠道票种映射：第三方商品 id → 本地票种（美团/抖音团购套餐落成哪一种通票）。
 *
 * <p>priceRuleId 可空：空=按票种门市价出票；非空=按该价格规则（渠道团购价）出票，
 * 本地订单金额与渠道实收对齐，自动对账（channel-auto）才对得上。
 */
public record IntegrationChannelTicketTypeMapRecord(
        String id,
        String providerCode,
        String channel,
        String externalProductId,
        String ticketTypeId,
        String priceRuleId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
