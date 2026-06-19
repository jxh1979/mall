package com.complex.platform.customer.dto;

/** 储值充值请求（客人端，绑支付意图）。amountCent=充值金额（分）；channel=支付渠道。 */
public record StoredValueTopupRequest(
        String idempotencyKey,
        String projectId,
        Long amountCent,
        String channel
) {
}
