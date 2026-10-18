package com.complex.platform.dealer.dto;

/** 只带幂等键的状态推进请求（提交审核 / 提交审批 / 置失效）。 */
public record DealerIdempotentRequest(String idempotencyKey) {
}
