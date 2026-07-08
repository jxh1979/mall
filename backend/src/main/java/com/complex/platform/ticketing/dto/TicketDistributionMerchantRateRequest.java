package com.complex.platform.ticketing.dto;

/**
 * 配置票务分销给商户的费率请求。merchantId 必填;perGuestCent/fixedAmountCent 至少配一项;
 * status 缺省 active;isDraft 草稿态(缺省 false)。同(项目,商户)幂等 upsert。
 */
public record TicketDistributionMerchantRateRequest(
        String idempotencyKey,
        String projectId,
        String merchantId,
        Long perGuestCent,
        Long fixedAmountCent,
        String status,
        Boolean isDraft
) {
}
