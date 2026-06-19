package com.complex.platform.customer.dto;

/** 储值消费请求（余额扣减，不足拒）。 */
public record StoredValueConsumeRequest(
        String idempotencyKey,
        String projectId,
        Long amountCent,
        String reason
) {
}
