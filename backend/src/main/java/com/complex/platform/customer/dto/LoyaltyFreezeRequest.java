package com.complex.platform.customer.dto;

/** 会员积分账户冻结/解冻请求（后台风控动作）。 */
public record LoyaltyFreezeRequest(
        String idempotencyKey,
        String reason
) {
}
