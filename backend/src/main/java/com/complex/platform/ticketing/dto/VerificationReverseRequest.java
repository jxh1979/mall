package com.complex.platform.ticketing.dto;

/**
 * R5 T5 核销冲正请求：幂等键必填、冲正原因 reason 必填。
 */
public record VerificationReverseRequest(
        String idempotencyKey,
        String reason
) {
}
