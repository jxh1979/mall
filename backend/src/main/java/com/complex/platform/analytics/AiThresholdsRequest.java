package com.complex.platform.analytics;

import java.math.BigDecimal;

/** V218 AI/对标阈值配置请求(经营参数页):AI建议采纳最低置信度 + 对标偏离告警线,均∈[0,1]。 */
public record AiThresholdsRequest(
        String idempotencyKey,
        BigDecimal localLlmMinConfidence,
        BigDecimal benchmarkDeviation
) {
}
