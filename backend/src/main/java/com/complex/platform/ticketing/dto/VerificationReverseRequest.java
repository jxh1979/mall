package com.complex.platform.ticketing.dto;

// 文件版本：V0.01

/**
 * R5 T5 核销冲正请求：幂等键必填、冲正原因 reason 必填。TK-14 提起核销异常复核同形（幂等键＋原因）。
 */
public record VerificationReverseRequest(
        String idempotencyKey,
        String reason
) {
}
