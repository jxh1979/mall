package com.complex.platform.customer.dto;

/** 付费会员开通请求（客人端，绑支付意图）。 */
public record MembershipSubscribeRequest(
        String idempotencyKey,
        String planId,
        String channel
) {
}
