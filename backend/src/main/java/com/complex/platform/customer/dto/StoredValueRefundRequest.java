package com.complex.platform.customer.dto;

/** 储值退卡/退余额请求（后台，退回余额；实际退款打款待支付渠道）。 */
public record StoredValueRefundRequest(
        String idempotencyKey,
        String projectId,
        String merchantId,
        Long amountCent,
        String reason,
        Boolean closeAccount
) {
}
