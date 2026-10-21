package com.complex.platform.ticketing.dto;

// 文件版本：V0.01

/**
 * TK-14 核销异常处置请求（ENUM「异常状态的统一出口」）：resolution 取 restored（更正后恢复）/ voided_reversed（作废并冲正），
 * reason 必填留痕，幂等键必填。
 */
public record VerificationAnomalyResolveRequest(
        String idempotencyKey,
        String resolution,
        String reason
) {
}
