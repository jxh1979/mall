package com.complex.platform.merchantportal.dto;

/** 商户专区「我的对账」结算异议留言。 */
public record MerchantSettlementDisputeRequest(
        String idempotencyKey,
        String operatorNote,
        String reason
) {
}
